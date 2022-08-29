/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.uima.cas.impl;

import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.UIMAClassLoader;
import org.apache.uima.internal.util.WeakIdentityMap;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.spi.JCasClassProvider;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

// @formatter:off
/**
 * There is one **class** instance of this per UIMA core class loader.
 *   The class loader is the loader for the UIMA core classes, not any UIMA extension class loader
 *   - **Builtin** JCas Types are loaded and shared among all type systems, once, when this class is loaded.
 * 
 * There are no instances of this class.  
 *   - The type system impl instances at commit time initialize parts of their Impl from data in this class
 *   - Some of the data kept in this class in static values, is constructed when the type system is committed
 * 
 * The class instance is shared
 *   - by multiple type systems 
 *   - by multiple CASes (in a CAS pool, for instance, when these CASes are sharing the same type system).
 *   - by all views of those CASes.
 *   - by multiple different pipelines, built using the same merged type system instance
 *   - by non-built-in JCas classes, loaded under possibly different extension class loaders
 * 
 * PEAR support
 *   Multiple PEAR contexts can be used.
 *   - hierarchy (each is parent of kind below
 *     -- UIMA core class loader (built-ins, not redefinable by user JCas classes) 
 *         --- a new limitation of UIMA V3 to allow sharing of built-in JCas classes, which also
 *             have custom impl, and don't fit the model used for PEAR Trampolines
 *     -- outer (non Pear) class loader (optional, known as base extension class loader)
 *         --- possible multiple, for different AE pipelines
 *     -- Within PEAR class loader
 *   - when running within a PEAR, operations which return Feature Structures potentially return
 *     JCas instances of classes loaded from the Pear's class loader. 
 *       - These instances share the same int[] and Object[] and _typeImpl and _casView refs with the outer class loader's FS
 * 
 * Timing / life cycle
 *   Built-in classes loaded and initialized at first type system commit time.
 *   non-pear classes loaded and initialized at type system commit time (if not already loaded)
 *     - special checks for conformability if some types loaded later, due to requirements for computing feature offsets at load time
 *   pear classes loaded and initialized at first entry to Pear, for a given type system and class loader.        
 *
 * 
 *   At typeSystemCommit time, this class is created and initialized: 
 *     - The built-in JCas types are loaded
 * 
 *     - The user-defined non-PEAR JCas classes are loaded (not lazy, but eager), provided the type system is a new one.
 *       (If the type system is "equal" to an existing committed one, that one is used instead).
 * 
 *       -- User classes defined with the name of UIMA types, but which are not JCas definitions, are not used as 
 *          JCas types.  This permits uses cases where users define a class which (perhaps at a later integration time)
 *          has the same name as a UIMA type, but is not a JCas class.
 *       -- These classes, once loaded, remain loaded because of Java's design, unless the ClassLoader
 *          used to load them is Garbage Collected.
 *          --- The ClassLoader used is the CAS's JCasClassLoader, set from the UIMA Extension class loader if specified.
 * 
 *   Assigning slots for features:
 *     - each type being loaded runs static final initializers to set for (a subset of) all features the offset
 *       in the int or ref storage arrays for those values. 
 *     - These call a static method in JCasRegistry: register[Int/Ref]Feature, which assigns the next available slot
 *       via accessing/updating a thread local instance of TypeSystemImpl.SlotAllocate.
 */
// @formatter:on
public abstract class FSClassRegistry { // abstract to prevent instantiating; this class only has
                                        // static methods

  static final String RECORD_JCAS_CLASSLOADERS = "uima.record_jcas_classloaders";
  static final boolean IS_RECORD_JCAS_CLASSLOADERS = Misc
          .getNoValueSystemProperty(RECORD_JCAS_CLASSLOADERS);

  static final String LOG_JCAS_CLASSLOADERS_ON_SHUTDOWN = "uima.log_jcas_classloaders_on_shutdown";
  static final boolean IS_LOG_JCAS_CLASSLOADERS_ON_SHUTDOWN = Misc
          .getNoValueSystemProperty(LOG_JCAS_CLASSLOADERS_ON_SHUTDOWN);

  // private static final boolean IS_TRACE_AUGMENT_TS = false;
  // private static final boolean IS_TIME_AUGMENT_FEATURES = false;
  /* ========================================================= */
  /* This class has only static methods and fields */
  /* To allow multi-threaded use, some fields are */
  /* kept in a thread local */
  /* ========================================================= */

  /* ==========================++++++=== */
  /* Static final, not in thread local */
  /* not sync-ed */
  /* ==========================++++++=== */

  // Used for the built-ins.
  private static final Lookup defaultLookup = MethodHandles.lookup();

  private static final MethodType findConstructorJCasCoverType = methodType(void.class,
          TypeImpl.class, CASImpl.class);
  // private static final MethodType findConstructorJCasCoverTypeArray = methodType(void.class,
  // TypeImpl.class, CASImpl.class, int.class);
  /**
   * The callsite has the return type, followed by capture arguments
   */
  private static final MethodType callsiteFsGenerator = methodType(FsGenerator3.class);
  // private static final MethodType callsiteFsGeneratorArray = methodType(FsGeneratorArray.class);
  // // NO LONGER USED

  private static final MethodType fsGeneratorType = methodType(TOP.class, TypeImpl.class,
          CASImpl.class);
  // private static final MethodType fsGeneratorArrayType = methodType(TOP.class, TypeImpl.class,
  // CASImpl.class, int.class); // NO LONGER USED

  // @formatter:off
  /**
   * precomputed generators for built-in types
   * These instances are shared for all type systems
   * Key = index = typecode
   */
  // @formatter:on
  private static final JCasClassInfo[] jcasClassesInfoForBuiltins;

  /* =================================== */
  /* not in thread local, sync-ed */
  /* =================================== */

  /** a cache for constant int method handles */
  private static final List<MethodHandle> methodHandlesForInt = new ArrayList<>();

  // @formatter:off
  /**
   * Map from class loaders used to load JCas Classes, both PEAR and non-Pear cases, to JCasClassInfo for that loaded JCas class instance.
   *   key is the class loader
   *   value is a plain HashMapmap from string form of typenames to JCasClassInfo corresponding to the JCas class covering that type
   *     (which may be a supertype of the type name).
   * 
   *     Key is JCas fully qualified name (not UIMA type name).
   *       Is a String, since different type systems may use the same JCas classes.
   *     value is the JCasClassInfo for that class
   *       - this may be for that actual JCas class, if one exists for that UIMA type name
   *       - or it is null, signalling that there is no JCas for this type, and a supertype should be used
   * 
   * Cache of FsGenerator[]s kept in TypeSystemImpl instance, since it depends on type codes.
   * Current FsGenerator[] kept in CASImpl shared view data, switched as needed for PEARs.
   * <p>
   * <b>NOTE:</b> Access this map in a thread-safe way only via {@link #get_className_to_jcci} which
   * synchronizes on the map object.
   */
  // @formatter:on
  private static final WeakIdentityMap<ClassLoader, Map<String, JCasClassInfo>> cl_to_type2JCas = WeakIdentityMap
          .newHashMap(); // identity: key is classloader
  private static final WeakIdentityMap<ClassLoader, StackTraceElement[]> cl_to_type2JCasStacks;
  private static final WeakIdentityMap<ClassLoader, Map<String, Class<? extends TOP>>> cl_to_spiJCas = WeakIdentityMap
          .newHashMap();
  private static final WeakIdentityMap<ClassLoader, UIMAClassLoader> cl_to_uimaCl = WeakIdentityMap
          .newHashMap();

  // private static final Map<ClassLoader, Map<String, JCasClassInfo>> cl_4pears_to_type2JCas =
  // Collections.synchronizedMap(new IdentityHashMap<>()); // identity: key is classloader

  static private class ErrorReport {
    final Exception e;
    final boolean doThrow;

    ErrorReport(Exception e, boolean doThrow) {
      this.e = e;
      this.doThrow = doThrow;
    }
  }

  // must precede first (static) use
  static private ThreadLocal<List<ErrorReport>> errorSet = new ThreadLocal<>();

  // /**
  // * Map (per class loader) from JCas Classes, to all callSites in that JCas class
  // */
  // public static final Map<Class<? extends TOP>, ArrayList<Entry<String, MutableCallSite>>>
  // callSites_all_JCasClasses = new HashMap<>();

  // @formatter:off
  /**
   * One instance per JCas class defined for it, per class loader
   *   - per class loader, because different JCas class definitions for the same name are possible, per class loader
   * 
   * Kept in maps, per class loader.  
   * Created when potentially loading JCas classes.
   * 
   * Entries kept in potentially multiple global static hashmaps, with key = the string form of the typename
   *   - string form of key allows sharing the same named JCas definition among different type system type-impls. 
   *   - one hashmap per classloader
   *   Entries reused potentially by multiple type systems.
   * 
   * Info used for 
   *   - identifying the target of a "new" operator - could be generator for superclass.
   *   - remembering the results of getting all the features this JCas class defines
   * One entry per defined JCas class per classloader; no instance if no JCas class is defined.
   */
  // @formatter:on
  public static class JCasClassInfo {

    final FsGenerator3 generator;

    /**
     * The corresponding loaded JCas Class for this UIMA type, may be a JCas class associated with a
     * UIMA supertype if no JCas class is found for this type.
     */
    final Class<? extends TOP> jcasClass;

    /**
     * NOT the TypeCode, but instead, the unique int assigned to the JCas class when it gets loaded
     * Might be -1 if the JCasClassInfo instance is for a non-JCas instantiable type
     */
    final int jcasType;

    final JCasClassFeatureInfo[] features;

    JCasClassInfo(Class<? extends TOP> jcasClass, FsGenerator3 generator, int jcasType) {
      this.generator = generator;
      this.jcasClass = jcasClass;
      this.jcasType = jcasType; // typeId for jcas class, **NOT Typecode**
      this.features = getJCasClassFeatureInfo(jcasClass);

      // System.out.println("debug create jcci, class = " + jcasClass.getName() + ", typeint = " +
      // jcasType);
    }

    boolean isCopydown(TypeImpl ti) {
      return isCopydown(ti.getJCasClassName());
    }

    boolean isCopydown(String jcasClassName) {
      return !jcasClass.getCanonicalName().equals(jcasClassName);
    }

    boolean isPearOverride(TypeSystemImpl tsi) {
      JCasClassInfo baseJcci = tsi.getJcci(jcasClass.getName());
      return baseJcci == null
              || !jcasClass.getClassLoader().equals(baseJcci.jcasClass.getClassLoader());
    }

    TypeImpl getUimaType(TypeSystemImpl tsi) {
      return tsi.getType(Misc.javaClassName2UimaTypeName(jcasClass.getName()));
    }
  }

  /**
   * Information about all features this JCas class defines Used to expand the type system when the
   * JCas defines more features than the type system declares.
   */
  static class JCasClassFeatureInfo {
    final String shortName;
    // rangename is byte.class, etc
    // or x.y.z.JCasClassName
    final String uimaRangeName;

    JCasClassFeatureInfo(String shortName, String uimaRangeName) {
      this.shortName = shortName;
      this.uimaRangeName = uimaRangeName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return String.format("JCasClassFeatureInfo feature: %s, range: %s",
              (shortName == null) ? "<null>" : shortName,
              (uimaRangeName == null) ? "<null>" : uimaRangeName);
    }

  }

  static {
    TypeSystemImpl tsi = TypeSystemImpl.staticTsi;
    jcasClassesInfoForBuiltins = new JCasClassInfo[tsi.getTypeArraySize()];
    // lookup = defaultLookup;

    // walk in subsumption order, supertype before subtype
    // Class loader used for builtins is the UIMA framework's class loader
    ArrayList<MutableCallSite> callSites_toSync = new ArrayList<>();
    ClassLoader cl = tsi.getClass().getClassLoader();
    loadBuiltins(tsi.topType, cl, get_className_to_jcci(cl, false), callSites_toSync);

    MutableCallSite[] sync = callSites_toSync.toArray(new MutableCallSite[callSites_toSync.size()]);
    MutableCallSite.syncAll(sync);

    reportErrors();

    if (IS_LOG_JCAS_CLASSLOADERS_ON_SHUTDOWN || IS_RECORD_JCAS_CLASSLOADERS) {
      cl_to_type2JCasStacks = WeakIdentityMap.newHashMap();
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          log_registered_classloaders(Level.WARN);
        }
      });
    } else {
      cl_to_type2JCasStacks = null;
    }
  }

  static int clToType2JCasSize() {
    return cl_to_type2JCas.size();
  }

  private static void loadBuiltins(TypeImpl ti, ClassLoader cl,
          Map<String, JCasClassInfo> type2jcci, ArrayList<MutableCallSite> callSites_toSync) {
    String typeName = ti.getName();

    if (BuiltinTypeKinds.creatableBuiltinJCas.contains(typeName)
            || typeName.equals(CAS.TYPE_NAME_SOFA)) {
      JCasClassInfo jcci = getOrCreateJCasClassInfo(ti, cl, type2jcci, defaultLookup);
      assert jcci != null;
      // done while beginning to commit the staticTsi (before committed flag is set), for builtins
      updateOrValidateAllCallSitesForJCasClass(jcci.jcasClass, ti, callSites_toSync);
      jcasClassesInfoForBuiltins[ti.getCode()] = jcci;

      // Class<?> builtinClass = maybeLoadJCas(ti, cl);
      // assert (builtinClass != null); // builtin types must be present
      // // copy down to subtypes, if needed, done later
      // int jcasType = Misc.getStaticIntFieldNoInherit(builtinClass, "typeIndexID");
      // JCasClassInfo jcasClassInfo = createJCasClassInfo(builtinClass, ti, jcasType);
      // jcasClassesInfoForBuiltins[ti.getCode()] = jcasClassInfo;
    }

    for (TypeImpl subType : ti.getDirectSubtypes()) {
      loadBuiltins(subType, cl, type2jcci, callSites_toSync);
    }
  }

  /**
   * Load JCas types for some combination of class loader and type system Some of these classes may
   * have already been loaded for this type system Some of these classes may have already been
   * loaded (perhaps for another type system)
   * 
   * @param ts
   *          the type system
   * @param isDoUserJCasLoading
   *          always true, left in for experimentation in the future with dynamic generation of JCas
   *          classes
   * @param cl
   *          the class loader. For Pears, is the pear class loader
   */
  private static synchronized void loadJCasForTSandClassLoader(TypeSystemImpl ts,
          boolean isDoUserJCasLoading, ClassLoader cl, Map<String, JCasClassInfo> type2jcci) {

    // boolean alreadyPartiallyLoaded = false; // true if any JCas types for this class loader have
    // previously been loaded

    // synchronized (cl2t2j) {
    // type2jcci = cl2t2j.get(cl);
    //
    // if (null == type2jcci) {
    // type2jcci = new HashMap<>();
    // cl2t2j.put(cl, type2jcci);
    // } else {
    // alreadyPartiallyLoaded = true;
    // }
    // }

    // @formatter:off
    /**
     * copy in built-ins
     *   update t2jcci (if not already loaded) with load info for type
     *   update type system's map from unique JCasID to the type in this type system
     */
    // @formatter:on

    /* ============================== */
    /* BUILT-INS */
    /* ============================== */
    for (int typecode = 1; typecode < jcasClassesInfoForBuiltins.length; typecode++) {

      JCasClassInfo jcci = jcasClassesInfoForBuiltins[typecode];
      if (jcci != null) {
        Class<? extends TOP> jcasClass = jcci.jcasClass;
        type2jcci.putIfAbsent(jcasClass.getCanonicalName(), jcci);
        setTypeFromJCasIDforBuiltIns(jcci, ts, typecode);
        // update call sites not called, was called when
        // jcci was created.
        // updateAllCallSitesForJCasClass(jcasClass, ts.getTypeForCode(typecode));
      }
    }

    /* ========================================================= */
    /* Add all user-defined JCas Types, in subsumption order */
    /* ========================================================= */

    if (isDoUserJCasLoading) {
      // @formatter:off
      /**
       * Two passes are needed loading is needed.  
       *   - The first one loads the JCas Cover Classes initializes everything
       *      -- some of the classes might already be loaded (including the builtins which are loaded once per class loader)
       *   - The second pass performs the conformance checks between the loaded JCas cover classes, and the current type system.
       *     This depends on having the TypeImpl's javaClass field be accurate (reflect any loaded JCas types)
       */
      // @formatter:on

      // this is this here rather than in a static initializer, because
      // we want to use the "cl" parameter to get a version of the
      // getMethodHandlesLookup that will have the right (maybe more local) permissions checking.
      // This is done by having the UIMA Class loader notice that the class being loaded is
      // MHLC, and then dynamically loading in that class loader a copy of the byte code
      // for that class.
      Lookup lookup = getLookup(cl);

      ArrayList<MutableCallSite> callSites_toSync = new ArrayList<>();
      maybeLoadJCasAndSubtypes(ts, ts.topType, type2jcci.get(TOP.class.getCanonicalName()), cl,
              type2jcci, callSites_toSync, lookup);

      MutableCallSite[] sync = callSites_toSync
              .toArray(new MutableCallSite[callSites_toSync.size()]);
      MutableCallSite.syncAll(sync);

      checkConformance(ts, ts.topType, type2jcci);
    }

    reportErrors();
  }

  private static void setTypeFromJCasIDforBuiltIns(JCasClassInfo jcci, TypeSystemImpl tsi,
          int typeCode) {
    int v = jcci.jcasType;
    // v is negative if not found, which is the case for types like FloatList (these can't be
    // instantiated)
    if (v >= 0) {
      tsi.setJCasRegisteredType(v, tsi.getTypeForCode(typeCode));
    }
  }

  // @formatter:off
  /**
   * Called for all the types, including the built-ins, but the built-ins have already been set up by the caller.
   * Saves the results in two places
   *   type system independent spot: JCasClassInfo instance indexed by JCasClassName
   *   type system spot: the JCasIndexID -> type table in the type system
   * 
   * Looks up by classname to see if there is an associated JCas class for this type.
   *   - all types of that name (perhaps from different loaded type systems) will share that one JCas class
   *   - copyDowns are excluded from this requirement - because there are no JCas class definitions
   *     for this type (in that case).
   * 
   * @param tsi
   *          the type system
   * @param ti
   *          the type to process
   * @param copyDownDefault_jcasClassInfo
   * @param cl
   *          the loader used to load, and to save the results under the key of the class loader the
   *          results
   * @param type2JCas
   *          map holding the results of loading JCas classes
   */
  // @formatter:on
  private static void maybeLoadJCasAndSubtypes(TypeSystemImpl tsi, TypeImpl ti,
          JCasClassInfo copyDownDefault_jcasClassInfo, ClassLoader cl,
          Map<String, JCasClassInfo> type2jcci, ArrayList<MutableCallSite> callSites_toSync,
          Lookup lookup) {

    JCasClassInfo jcci = getOrCreateJCasClassInfo(ti, cl, type2jcci, lookup);

    if (null != jcci && tsi.isCommitted()) {
      updateOrValidateAllCallSitesForJCasClass(jcci.jcasClass, ti, callSites_toSync);
    }

    // String t2jcciKey = Misc.typeName2ClassName(ti.getName());
    // JCasClassInfo jcci = type2jcci.get(t2jcciKey);
    //
    // if (jcci == null) {
    //
    // // first time encountering this typename. Attempt to load a jcas class for this
    // // - if none, the next call returns null.
    // jcci = createJCasClassInfo(ti, cl, callSites_toSync, lookup); // does update of callsites if
    // was able find JCas class
    //
    // if (null != jcci) {
    // validateSuperClass(jcci, ti);
    // type2jcci.put(t2jcciKey, jcci);
    // tsi.setJCasRegisteredType(jcci.jcasType, ti);
    // }
    //
    //// // not yet recorded as loaded under this class loader.
    ////
    //// Class<?> clazz = maybeLoadJCas(ti, cl);
    //// if (null != clazz && TOP.class.isAssignableFrom(clazz)) {
    ////
    //// int jcasType = -1;
    //// if (!Modifier.isAbstract(clazz.getModifiers())) { // skip next for abstract classes
    //// jcasType = Misc.getStaticIntFieldNoInherit(clazz, "typeIndexID");
    //// // if jcasType is negative, this means there's no value for this field
    //// assert(jcasType >= 0);
    //// }
    //// jcci = createJCasClassInfo(clazz, ti, jcasType);
    //// isCopyDown = false;
    //// // don't do this call, caller will call conformance which has a weaker
    //// // test - passes if there is a shared
    //// // https://issues.apache.org/jira/browse/UIMA-5660
    ////// if (clazz != TOP.class) { // TOP has no super class
    ////// validateSuperClass(jcci, ti);
    ////// }
    //// } else {
    //// jcci = copyDownDefault_jcasClassInfo;
    //// }
    //
    //// type2jcci.put(t2jcciKey, jcci);
    //
    // } else {
    // // have previously set up a jcci for this type name
    // // maybe for a different type instance (of the same name)
    // // next may be redundant?
    // if (ti.getTypeSystem().isCommitted()) {
    // updateOrValidateAllCallSitesForJCasClass(jcci.jcasClass, ti, callSites_toSync);
    // }
    //
    // }

    // // this UIMA type was set up (maybe loaded, maybe defaulted to a copy-down) previously
    // isCopyDown = jcci.isCopydown(t2jcciKey);
    //
    // if (isCopyDown) {
    // // the "stored" version might have the wrong super class for this type system
    // type2jcci.put(t2jcciKey, jcci = copyDownDefault_jcasClassInfo);
    //
    // } else if (!ti.isTopType()) {
    // // strong test for non-copy-down case: supertype must match, with 2 exceptions
    // // removed https://issues.apache.org/jira/browse/UIMA-5660
    //// validateSuperClass(jcci, ti);
    // }
    // }

    // this is done even after the class is first loaded, in case the type system changed.
    // don't set anything if copy down - otherwise was setting the copyed-down typeId ref to the
    // new ti
    // System.out.println("debug set jcas regisered type " + jcci.jcasType + ", type = " +
    // ti.getName());

    JCasClassInfo jcci_or_copyDown = (jcci == null) ? copyDownDefault_jcasClassInfo : jcci;

    if (!ti.isPrimitive()) { // bypass this for primitives because the jcasClassInfo is the
                             // "inherited one" of TOP
      /**
       * Note: this value sets into the shared TypeImpl (maybe shared among many JCas impls) the
       * "latest" jcasClass It is "read" by the conformance testing, while still under the type
       * system lock. Other uses of this may get an arbitrary (the latest) version of the class
       * Currently the only other use is in backwards compatibility with low level type system
       * "switching" an existing type.
       */
      ti.setJavaClass(jcci_or_copyDown.jcasClass);
    }

    for (TypeImpl subtype : ti.getDirectSubtypes()) {
      maybeLoadJCasAndSubtypes(tsi, subtype, jcci_or_copyDown, cl, type2jcci, callSites_toSync,
              lookup);
    }
  }

  // @formatter:off
  /**
   * For a particular type name, get the JCasClassInfo
   *   - by fetching the cached value
   *   - by loading the class
   *   - return null if no JCas class for this name 
   * only called for non-Pear callers
   * @param ti -
   * @param cl -
   * @param type2jcci -
   * @param lookup -
   * @return - jcci or null, if no JCas class for this type was able to be loaded
   */
  // @formatter:on
  public static JCasClassInfo getOrCreateJCasClassInfo(TypeImpl ti, ClassLoader cl,
          Map<String, JCasClassInfo> type2jcci, Lookup lookup) {

    JCasClassInfo jcci = type2jcci.get(ti.getJCasClassName());

    if (jcci == null) {
      jcci = maybeCreateJCasClassInfo(ti, cl, type2jcci, lookup);
    }

    // do this setup for new type systems using previously loaded jcci, as well as
    // for new jccis
    if (jcci != null && jcci.jcasType >= 0) {
      ti.getTypeSystem().setJCasRegisteredType(jcci.jcasType, ti);
    }
    return jcci;
  }

  static JCasClassInfo maybeCreateJCasClassInfo(TypeImpl ti, ClassLoader cl,
          Map<String, JCasClassInfo> type2jcci, Lookup lookup) {
    JCasClassInfo jcci = createJCasClassInfo(ti, cl, lookup); // does update of callsites if was
                                                              // able find JCas class

    if (null != jcci) {
      type2jcci.put(ti.getJCasClassName(), jcci);
      // non-creatable JCas types (e.g. FSList) do not have a valid jcasType
    }
    return jcci;
  }

  public static JCasClassInfo createJCasClassInfo(TypeImpl ti, ClassLoader cl, Lookup lookup) {
    Lookup actualLookup = lookup;

    // First we try the local classloader - this is necessary because it might be a PEAR situation
    Class<? extends TOP> clazz = maybeLoadLocalJCas(ti, cl);

    // If the local classloader does not have the JCas wrapper, we try the SPI
    if (clazz == null) {
      Map<String, Class<? extends TOP>> spiJCasClasses = loadJCasClassesFromSPI(cl);
      clazz = spiJCasClasses.get(ti.getJCasClassName());
      if (clazz != null) {
        actualLookup = getLookup(clazz.getClassLoader());
      }
    }

    if (null == clazz || !TOP.class.isAssignableFrom(clazz)) {
      return null;
    }

    int jcasType = -1;
    if (!Modifier.isAbstract(clazz.getModifiers())) { // skip next for abstract classes
      jcasType = Misc.getStaticIntFieldNoInherit(clazz, "typeIndexID");
      // if jcasType is negative, this means there's no value for this field
      if (jcasType == -1) {
        add2errors(errorSet,
                /**
                 * The Class "{0}" matches a UIMA Type, and is a subtype of uima.cas.TOP, but is
                 * missing the JCas typeIndexId.
                 */
                new CASRuntimeException(CASRuntimeException.JCAS_MISSING_TYPEINDEX,
                        clazz.getName()),
                false); // not a fatal error
        return null;
      }
    }

    return createJCasClassInfo(clazz, ti, jcasType, actualLookup);
  }

  // static AtomicLong time = IS_TIME_AUGMENT_FEATURES ? new AtomicLong(0) : null;
  //
  // static {
  // if (IS_TIME_AUGMENT_FEATURES) {
  // Runtime.getRuntime().addShutdownHook(new Thread(null, () -> {
  // System.out.format("Augment features from JCas time: %,d ms%n",
  // time.get() / 1000000L);
  // }, "show augment feat from jcas time"));
  // }
  // }
  //
  // static void augmentFeaturesFromJCas(
  // TypeImpl type,
  // ClassLoader cl,
  // TypeSystemImpl tsi,
  // Map<String, JCasClassInfo> type2jcci,
  // Lookup lookup) {
  //
  // long startTime = 0;
  // if (type.isTopType()) {
  // if (IS_TIME_AUGMENT_FEATURES) {
  // startTime = System.nanoTime();
  // }
  // } else {
  // /**************************************************************************************
  // * N O T E : *
  // * fixup the ordering of staticMergedFeatures: *
  // * - supers, then features introduced by this type. *
  // * - order may be "bad" if later feature merge introduced an additional feature *
  // **************************************************************************************/
  // // skip for top level; no features there, but no super type either
  // type.getFeatureImpls(); // done for side effect of computingcomputeStaticMergedFeaturesList();
  // }
  //
  // if ( //false && // debugging
  // ! type.isBuiltIn) {
  //
  // if (IS_TRACE_AUGMENT_TS) System.out.println("trace Augment TS from JCas, for type " +
  // type.getName());
  //
  //
  // TypeSystemImpl.typeBeingLoadedThreadLocal.set(type); // only for supporting previous version of
  // v3 jcas
  //
  // JCasClassInfo jcci = getOrCreateJCasClassInfo(type, cl, type2jcci, lookup); // no call site
  // sync
  // if (jcci != null) {
  //
  // if (IS_TRACE_AUGMENT_TS) System.out.println(" trace Augment TS from JCas, adding features: " +
  // Misc.ppList(Arrays.asList(jcci.features)));
  //
  // type.jcci = jcci;
  // // also recurse for supertypes to load jcci's (in case some don't have uima type)
  // // recursion stops when have jcci already
  // jcci = type2jcci.get(jcci.jcasClass.getSuperclass());
  // if (null == jcci) {
  //
  // }
  //
  //// for (JCasClassFeatureInfo f : jcci.features) {
  //// FeatureImpl fi = type.getFeatureByBaseName(f.shortName);
  //// if (fi == null) {
  ////
  //// /* *********************************************************************************
  //// * feature is missing in the type, a pseudo feature for it *
  //// * *********************************************************************************/
  ////
  //// /* Range is either one of the uima primitives, or *
  //// * a fs reference. FS References could be to "unknown" types in this type system. *
  //// * If so, use TOP */
  //// TypeImpl rangeType = tsi.getType(f.uimaRangeName);
  //// if (rangeType == null) {
  //// rangeType = tsi.topType;
  //// }
  ////
  //// /** Can't add feature to type "{0}" since it is feature final. */
  //// if (type.isFeatureFinal()) {
  //// throw new CASAdminException(CASAdminException.TYPE_IS_FEATURE_FINAL, type.getName());
  //// }
  ////
  //// if (IS_TRACE_AUGMENT_TS) System.out.println(" trace Augment TS from JCas, for feature: " +
  // f.shortName );
  ////
  //// if (tsi.isInInt(rangeType)) {
  //// type.jcas_added_int_slots.add(new FeatureImpl_jcas_only(f.shortName, rangeType));
  //// } else {
  //// type.jcas_added_ref_slots.add(new FeatureImpl_jcas_only(f.shortName, rangeType));
  //// }
  //// }
  //// }
  // }
  // }
  //
  // if (IS_TRACE_AUGMENT_TS) System.out.println("trace Augment TS from JCas, for subtypes of type "
  // + type.getName() + ", " + Misc.ppList(type.getDirectSubtypes()));
  // for (TypeImpl subti : type.getDirectSubtypes()) {
  // augmentFeaturesFromJCas(subti, cl, tsi, type2jcci, lookup);
  // }
  //
  // if (IS_TIME_AUGMENT_FEATURES && type.isTopType()) {
  // time.addAndGet(System.nanoTime() - startTime);
  // }
  // }

  // private void setTypeJcci(TypeImpl type, ClassLoader cl, Lookup lookup, Map<String,
  // JCasClassInfo> type2jcci) {
  // if (IS_TRACE_AUGMENT_TS) System.out.println("trace Augment TS from JCas, for type " +
  // type.getName());
  //
  // TypeSystemImpl.typeBeingLoadedThreadLocal.set(type); // only for supporting previous version of
  // v3 jcas
  //
  // JCasClassInfo jcci = getOrCreateJCasClassInfo(type, cl, type2jcci, lookup); // no call site
  // sync
  // if (jcci != null) {
  //
  // if (IS_TRACE_AUGMENT_TS) System.out.println(" trace Augment TS from JCas, adding features: " +
  // Misc.ppList(Arrays.asList(jcci.features)));
  //
  // type.jcci = jcci;
  // // also recurse for supertypes to load jcci's (in case some don't have uima type)
  // // recursion stops when have jcci already
  // Class<?> superClass = jcci.jcasClass.getSuperclass();
  // String superClassName = superClass.getName();
  // jcci = type2jcci.get(superClassName);
  //
  // if (null == jcci) {
  // TypeSystemImpl tsi = type.getTypeSystem();
  // TypeImpl ti = tsi.getType(Misc.javaClassName2UimaTypeName(superClassName));
  //
  //
  // setTypeJcci()
  // }
  //
  // }

  // private static String superTypeJCasName(TypeImpl ti) {
  // return Misc.typeName2ClassName(ti.getSuperType().getName());
  // }
  //

  private static boolean compare_C_T(Class<?> clazz, TypeImpl ti) {
    return ti.getJCasClassName().equals(clazz.getName());
  }

  /**
   * Changed https://issues.apache.org/jira/browse/UIMA-5660 to allow insertions of extra types/
   * classes into the superchain. verify that the supertype class chain matches the type
   * 
   * @param clazz
   *          The JCas class, always below TOP
   * @param ti
   *          -
   */
  private static void validateSuperClass(JCasClassInfo jcci, TypeImpl ti) {

    final Class<?> superClass = jcci.jcasClass.getSuperclass();

    final TypeImpl superType = ti.getSuperType();

    if (compare_C_T(superClass, superType)) {
      return;
    }

    for (TypeImpl st : ti.getAllSuperTypes()) {
      if (compare_C_T(superClass, st)) {
        return;
      }
    }

    for (Class<?> sc = superClass.getSuperclass(); sc != Object.class
            && sc != FeatureStructureImplC.class; sc = sc.getSuperclass()) {
      if (compare_C_T(sc, superType)) {
        return;
      }
    }

    /**
     * The JCas class: "{0}" has supertypes: "{1}" which do not match the UIMA type "{2}"''s
     * supertypes "{3}".
     */
    throw new CASRuntimeException(CASRuntimeException.JCAS_MISMATCH_SUPERTYPE,
            jcci.jcasClass.getName(), getAllSuperclassNames(jcci.jcasClass), ti.getName(),
            getAllSuperTypeNames(ti));
  }

  private static String getAllSuperclassNames(Class<?> clazz) {
    StringBuilder sb = new StringBuilder();

    for (Class<?> sc = clazz.getSuperclass(); sc != null
            && sc != FeatureStructureImplC.class; sc = sc.getSuperclass()) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(sc.getName());
    }
    return sb.toString();
  }

  private static String getAllSuperTypeNames(TypeImpl ti) {
    StringBuilder sb = new StringBuilder();

    for (TypeImpl st = ti.getSuperType(); st.getCode() != TypeSystemConstants.topTypeCode; st = st
            .getSuperType()) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(st.getName());
    }
    if (sb.length() > 0) {
      sb.append(", ");
    }
    sb.append("uima.cas.TOP");
    return sb.toString();
  }

  //
  //
  // if (! clazz.getSuperclass().getCanonicalName().equals(superTypeJCasName(ti))) {
  // /** Special case exceptions */
  // TypeImpl superti = ti.getSuperType();
  // TypeSystemImpl tsi = ti.getTypeSystem();
  // if (superti == tsi.arrayBaseType ||
  // superti == tsi.listBaseType) return;
  // /** The JCas class: "{0}" has supertype: "{1}" which doesn''t match the UIMA type "{2}"''s
  // supertype "{3}". */
  // throw new CASRuntimeException(CASRuntimeException.JCAS_MISMATCH_SUPERTYPE,
  // clazz.getCanonicalName(),
  // clazz.getSuperclass().getCanonicalName(),
  // ti.getName(),
  // ti.getSuperType().getName());
  // }
  //
  // }

  // @formatter:off
  /**
   * Called to load (if possible) a corresponding JCas class for a UIMA type.
   * Called at Class Init time for built-in types
   * Called at TypeSystemCommit for non-built-in types
   *   Runs the static initializers in the loaded JCas classes - doing resolve
   * 
   * Synchronization: done outside this class
   * 
   * @param cl
   *          the class loader to use
   * @return the loaded / resolved class
   */
  // @formatter:on
  @SuppressWarnings("unchecked")
  private static Class<? extends TOP> maybeLoadLocalJCas(TypeImpl ti, ClassLoader cl) {
    String className = ti.getJCasClassName();
    try {
      return (Class<? extends TOP>) Class.forName(className, true, cl);
    } catch (ClassNotFoundException e) {
      // Class not found is normal, if there is no JCas for this class
      return null;
    } catch (ExceptionInInitializerError e) {
      throw new RuntimeException("Exception while loading " + className, e);
    }
  }

  static Map<String, Class<? extends TOP>> loadJCasClassesFromSPI(ClassLoader cl) {
    synchronized (cl_to_spiJCas) {
      Map<String, Class<? extends TOP>> spiJCas = cl_to_spiJCas.get(cl);
      if (spiJCas != null) {
        return spiJCas;
      }

      Map<String, Class<? extends TOP>> spiJCasClasses = new LinkedHashMap<>();
      ServiceLoader<JCasClassProvider> loader = ServiceLoader.load(JCasClassProvider.class, cl);
      loader.forEach(provider -> {
        List<Class<? extends TOP>> list = provider.listJCasClasses();
        if (list != null) {
          list.forEach(item -> spiJCasClasses.put(item.getName(), item));
        }
      });
      cl_to_spiJCas.put(cl, spiJCasClasses);

      return spiJCasClasses;
    }
  }

  // SYNCHRONIZED

  static synchronized MethodHandle getConstantIntMethodHandle(int i) {
    MethodHandle mh = Misc.getWithExpand(methodHandlesForInt, i);
    if (mh == null) {
      methodHandlesForInt.set(i, mh = MethodHandles.constant(int.class, i));
    }
    return mh;
  }

  /**
   * Return a Functional Interface for a generator for creating instances of a type. Function takes
   * a casImpl arg, and returning an instance of the JCas type.
   * 
   * @param jcasClass
   *          the class of the JCas type to construct
   * @param typeImpl
   *          the UIMA type
   * @return a Functional Interface whose createFS method takes a casImpl and when subsequently
   *         invoked, returns a new instance of the class
   */
  private static FsGenerator3 createGenerator(Class<?> jcasClass, Lookup lookup) {
    try {

      MethodHandle mh = lookup.findConstructor(jcasClass, findConstructorJCasCoverType);
      MethodType mtThisGenerator = methodType(jcasClass, TypeImpl.class, CASImpl.class);

      CallSite callSite = LambdaMetafactory.metafactory(lookup, // lookup context for the
                                                                // constructor
              "createFS", // name of the method in the Function Interface
              callsiteFsGenerator, // signature of callsite, return type is functional interface,
                                   // args are captured args if any
              fsGeneratorType, // samMethodType signature and return type of method impl by function
                               // object
              mh, // method handle to constructor
              mtThisGenerator);
      return (FsGenerator3) callSite.getTarget().invokeExact();
    } catch (Throwable e) {
      if (e instanceof NoSuchMethodException) {
        String classname = jcasClass.getName();
        add2errors(errorSet,
                new CASRuntimeException(e, CASRuntimeException.JCAS_CAS_NOT_V3, classname,
                        jcasClass.getClassLoader()
                                .getResource(classname.replace('.', '/') + ".class").toString()));
        return null;
      }
      /**
       * An internal error occurred, please report to the Apache UIMA project; nested exception if
       * present: {0}
       */
      throw new UIMARuntimeException(e, UIMARuntimeException.INTERNAL_ERROR);
    }
  }

  // /**
  // * Return a Functional Interface for a getter for getting the value of a feature,
  // * called by APIs using the non-JCas style of access via features,
  // * but accessing the values via the JCas cover class getter methods.
  // *
  // * The caller of these methods is the FeatureStructureImplC methods.
  // *
  // * There are these return values:
  // * boolean, byte, short, int, long, float, double, String, FeatureStructure
  // *
  // */
  // // static for setting up builtin values
  // private static Object createGetterOrSetter(Class<?> jcasClass, FeatureImpl fi, boolean
  // isGetter, boolean ncnj) {
  //
  // TypeImpl range = fi.getRangeImpl();
  // String name = ncnj ? ("_" + fi.getGetterSetterName(isGetter) + "NcNj")
  // : fi.getGetterSetterName(isGetter);
  //
  // try {
  // /* get an early-bound getter
  // /* Instead of findSpecial, we use findVirtual, in case the method is overridden by a subtype
  // loaded later */
  // MethodHandle mh = lookup.findVirtual(
  // jcasClass, // class having the method code for the getter
  // name, // the name of the method for the getter or setter
  // isGetter ? methodType(range.javaClass)
  // : methodType(void.class, range.javaClass) // return value, e.g. int.class, xyz.class,
  // FeatureStructureImplC.class
  // );
  //
  // // getter methodtype is return_type, FeatureStructure.class
  // // return_type is int, byte, etc. primitive (except string/substring), or
  // // object (to correspond with erasure)
  // // setter methodtype is void.class, FeatureStructure.class, javaclass
  // MethodType mhMt = isGetter ? methodType(range.getJavaPrimitiveClassOrObject(),
  // FeatureStructureImplC.class)
  // : methodType(void.class, FeatureStructureImplC.class, range.getJavaPrimitiveClassOrObject());
  // MethodType iMt = isGetter ? methodType(range.javaClass, jcasClass)
  // : methodType(void.class, jcasClass, range.javaClass);
  //
  //// System.out.format("mh method type for %s method %s is %s%n",
  //// jcasClass.getSimpleName(),
  //// fi.getGetterSetterName(isGetter),
  //// mhMt);
  //
  // CallSite callSite = LambdaMetafactory.metafactory(
  // lookup, // lookup context for the getter
  // isGetter ? "get" : "set", // name of the method in the Function Interface
  // methodType(isGetter ? range.getter_funct_intfc_class : range.setter_funct_intfc_class), //
  // callsite signature = just the functional interface return value
  // mhMt, // samMethodType signature and return type of method impl by function object
  // mh, // method handle to constructor
  // iMt);
  //
  // if (range.getJavaClass() == boolean.class) {
  // return isGetter ? (JCas_getter_boolean) callSite.getTarget().invokeExact()
  // : (JCas_setter_boolean) callSite.getTarget().invokeExact();
  // } else if (range.getJavaClass() == byte.class) {
  // return isGetter ? (JCas_getter_byte) callSite.getTarget().invokeExact()
  // : (JCas_setter_byte) callSite.getTarget().invokeExact();
  // } else if (range.getJavaClass() == short.class) {
  // return isGetter ? (JCas_getter_short) callSite.getTarget().invokeExact()
  // : (JCas_setter_short) callSite.getTarget().invokeExact();
  // } else if (range.getJavaClass() == int.class) {
  // return isGetter ? (JCas_getter_int) callSite.getTarget().invokeExact()
  // : (JCas_setter_int) callSite.getTarget().invokeExact();
  // } else if (range.getJavaClass() == long.class) {
  // return isGetter ? (JCas_getter_long) callSite.getTarget().invokeExact()
  // : (JCas_setter_long) callSite.getTarget().invokeExact();
  // } else if (range.getJavaClass() == float.class) {
  // return isGetter ? (JCas_getter_float) callSite.getTarget().invokeExact()
  // : (JCas_setter_float) callSite.getTarget().invokeExact();
  // } else if (range.getJavaClass() == double.class) {
  // return isGetter ? (JCas_getter_double) callSite.getTarget().invokeExact()
  // : (JCas_setter_double) callSite.getTarget().invokeExact();
  // } else {
  // return isGetter ? (JCas_getter_generic<?>) callSite.getTarget().invokeExact()
  // : (JCas_setter_generic<?>) callSite.getTarget().invokeExact();
  // }
  // } catch (NoSuchMethodException e) {
  // if ((jcasClass == Sofa.class && !isGetter) ||
  // (jcasClass == AnnotationBase.class && !isGetter)) {
  // return null;
  // }
  // // report missing setter or getter
  // /* Unable to find required {0} method for JCAS type {1} with {2} type of {3}. */
  // CASException casEx = new CASException(CASException.JCAS_GETTER_SETTER_MISSING,
  // name,
  // jcasClass.getName(),
  // isGetter ? "return" : "argument",
  // range.javaClass.getName()
  // );
  // ArrayList<Exception> es = errorSet.get();
  // if (es == null) {
  // es = new ArrayList<Exception>();
  // errorSet.set(es);
  // }
  // es.add(casEx);
  // return null;
  // } catch (Throwable e) {
  // throw new UIMARuntimeException(e, UIMARuntimeException.INTERNAL_ERROR);
  // }
  // }

  // GetterSetter getGetterSetter(int typecode, String featShortName) {
  // return jcasClassesInfo[typecode].gettersAndSetters.get(featShortName);
  // }

  // static for setting up static builtin values
  // @formatter:off
  /**
   * Called after succeeding at loading, once per load for an exact matching JCas Class 
   *   - class was already checked to insure is of proper type for JCas
   *   - skips creating-generator-for-Sofa - since "new Sofa(...)" is not a valid way to create a sofa   
   * 
   * @param jcasClass
   *          the JCas class that corresponds to the type
   * @param ti
   *          the type
   * @return the info for this JCas that is shared across all type systems under this class loader
   */
  // @formatter:on
  private static JCasClassInfo createJCasClassInfo(Class<? extends TOP> jcasClass, TypeImpl ti,
          int jcasType, Lookup lookup) {
    boolean noGenerator = ti.getCode() == TypeSystemConstants.sofaTypeCode
            || Modifier.isAbstract(jcasClass.getModifiers()) || ti.isArray();
    FsGenerator3 generator = noGenerator ? null : createGenerator(jcasClass, lookup);
    JCasClassInfo jcasClassInfo = new JCasClassInfo(jcasClass, generator, jcasType);
    // System.out.println("debug creating jcci, classname = " + jcasClass.getName() + ",
    // jcasTypeNumber: " + jcasType);
    return jcasClassInfo;
  }

  private static JCasClassFeatureInfo[] getJCasClassFeatureInfo(Class<?> jcasClass) {
    ArrayList<JCasClassFeatureInfo> features = new ArrayList<>();

    try {
      for (Field f : jcasClass.getDeclaredFields()) {
        String fname = f.getName();
        if (fname.length() <= 5 || !fname.startsWith("_FC_")) {
          continue;
        }
        String featName = fname.substring(4);

        // compute range by looking at get method

        String getterName = "get" + Character.toUpperCase(featName.charAt(0))
                + featName.substring(1);
        Method m;
        try {
          m = jcasClass.getDeclaredMethod(getterName); // get the getter with no args
        } catch (NoSuchMethodException e) {
          /**
           * Cas class {0} with feature {1} but is mssing a 0 argument getter. This feature will not
           * be used to maybe expand the type's feature set.
           */
          Logger logger = UIMAFramework.getLogger(FSClassRegistry.class);
          logger.warn(() -> logger.rb_ue(CASRuntimeException.JCAS_MISSING_GETTER,
                  jcasClass.getName(), featName));
          continue; // skip this one
        }

        String rangeClassName = m.getReturnType().getName();
        String uimaRangeName = Misc.javaClassName2UimaTypeName(rangeClassName);
        features.add(new JCasClassFeatureInfo(featName, uimaRangeName));
      } // end of for loop
      JCasClassFeatureInfo[] r = new JCasClassFeatureInfo[features.size()];
      return features.toArray(r);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  // static boolean isFieldInClass(Feature feat, Class<?> clazz) {
  // try {
  // return null != clazz.getDeclaredField("_FC_" + feat.getShortName());
  // } catch (NoSuchFieldException e) {
  // return false;
  // }
  // }

  static void checkConformance(ClassLoader cl, TypeSystemImpl ts) {
    Map<String, JCasClassInfo> type2jcci = get_className_to_jcci(cl, false);
    checkConformance(ts, ts.topType, type2jcci);
  }

  private static void checkConformance(TypeSystemImpl ts, TypeImpl ti,
          Map<String, JCasClassInfo> type2jcci) {
    if (ti.isPrimitive()) {
      return;
    }
    JCasClassInfo jcci = type2jcci.get(ti.getJCasClassName());

    // if (null == jcci) {
    // if (!skipCheck && ti.isBuiltIn && jcci.isAlreadyCheckedBuiltIn) {
    // skipCheck = true;
    // }

    if (null != jcci && // skip if the UIMA class has an abstract (non-creatable) JCas class)
            !(ti.isBuiltIn)) { // skip if builtin
      checkConformance(jcci.jcasClass, ts, ti, type2jcci);
    }

    for (TypeImpl subtype : ti.getDirectSubtypes()) {
      checkConformance(ts, subtype, type2jcci);
    }
  }

  // @formatter:off
  /**
   * Inner check
   * 
   * Never called for "built-ins", or for uima types not having a JCas loaded class
   * 
   * Checks that a JCas class definition conforms to the current type in the current type system.
   * Checks that the superclass chain contains some match to the super type chain.
   * Checks that the return value for the getters for features matches the feature's range.
   * Checks that static _FC_xxx values from the JCas class == the adjusted feature offsets in the type system
   * 
   * @param clazz
   *          - the JCas class to check
   * @param tsi
   *          -
   * @param ti
   *          -
   */
  // @formatter:on
  private static void checkConformance(Class<?> clazz, TypeSystemImpl tsi, TypeImpl ti,
          Map<String, JCasClassInfo> type2jcci) {

    // // skip the test if the jcasClassInfo is being inherited
    // // because that has already been checked
    // if (!clazz.getName().equals(ti.getJCasClassName())) {
    // System.out.println("debug should never print");
    // return;
    // }

    // check supertype

    validateSuperClass(type2jcci.get(ti.getJCasClassName()), ti);

    // // This is done by validateSuperClass, when JCasClass is loaded or looked up for a particular
    // type system
    // // one of the supertypes must match a superclass of the class
    // // (both of these should be OK)
    //
    // // class: X -> XS -> Annotation -> AnnotationBase -> TOP -> FeatureStructureImplC
    // // type: X -------> Annotation -> AnnotationBase -> TOP
    // // (if XS getters/setters used, have runtime error; if never used, OK)
    // //
    // // class: X --------> Annotation -> AnnotationBase -> TOP -> FeatureStructureImplC
    // // type: X -> XS -> Annotation -> AnnotationBase -> TOP
    // boolean isOk = false;
    // List<Class<?>> superClasses = new ArrayList<>();
    // boolean isCheckImmediateSuper = true;
    // Class<?> superClass = clazz.getSuperclass();
    //
    // outer:
    // for (TypeImpl uimaSuperType : ti.getAllSuperTypes()) { // iterate uimaSuperTypes
    // JCasClassInfo jcci = type2jcci.get(uimaSuperType.getJCasClassName());
    // if (jcci != null) {
    // if (isCheckImmediateSuper) {
    // if (jcci.jcasClass != superClass) {
    // /** The JCas class: "{0}" has supertype: "{1}" which doesn''t match the UIMA type "{2}"''s
    // supertype "{3}". */
    // add2errors(errorSet,
    // new CASRuntimeException(CASRuntimeException.JCAS_MISMATCH_SUPERTYPE,
    // clazz.getCanonicalName(),
    // clazz.getSuperclass().getCanonicalName(),
    // ti.getName(),
    // ti.getSuperType().getName()),
    // false); // not a throwable error, just a warning
    // }
    // }
    //
    // superClasses.add(superClass);
    // while (superClass != FeatureStructureImplC.class && superClass != Object.class) {
    // if (jcci.jcasClass == superClass) {
    // isOk = true;
    // break outer;
    // }
    // superClass = superClass.getSuperclass();
    // superClasses.add(superClass);
    // }
    // }
    //
    // isCheckImmediateSuper = false;
    // }
    //
    // // This error only happens if the JCas type chain doesn't go thru "TOP" - so it isn't really
    // a JCas class!
    //
    // if (!isOk && superClasses.size() > 0) {
    // /** JCas Class's supertypes for "{0}", "{1}" and the corresponding UIMA Supertypes for "{2}",
    // "{3}" don't have an intersection. */
    // add2errors(errorSet,
    // new CASRuntimeException(CASRuntimeException.JCAS_CAS_MISMATCH_SUPERTYPE,
    // clazz.getName(), Misc.ppList(superClasses), ti.getName(),
    // Misc.ppList(Arrays.asList(ti.getAllSuperTypes()))),
    // true); // throwable error
    // }

    // the range of all the features must match the getters

    for (Method m : clazz.getDeclaredMethods()) {

      String mname = m.getName();
      if (mname.length() <= 3 || !mname.startsWith("get")) {
        continue;
      }
      String suffix = (mname.length() == 4) ? "" : mname.substring(4); // one char past 1st letter
                                                                       // of feature
      String fname = Character.toLowerCase(mname.charAt(3)) + suffix; // entire name, with first
                                                                      // letter lower cased
      FeatureImpl fi = ti.getFeatureByBaseName(fname);
      if (fi == null) {
        fname = mname.charAt(3) + suffix; // no feature, but look for one with captialized first
                                          // letter
        fi = ti.getFeatureByBaseName(fname);
        if (fi == null) {
          continue;
        }
      }

      // some users are writing getFeat(some other args) as additional signatures - skip checking
      // these
      // https://issues.apache.org/jira/projects/UIMA/issues/UIMA-5557
      Parameter[] p = m.getParameters();
      TypeImpl range = fi.getRangeImpl();

      if (p.length > 1) {
        continue; // not a getter, which has either 0 or 1 arg(the index int for arrays)
      }
      if (p.length == 1 && (!range.isArray() || p[0].getType() != int.class)) {
        continue; // has 1 arg, but is not an array or the arg is not an int
      }

      // have the feature, check the range
      Class<?> returnClass = m.getReturnType(); // for primitive, is int.class, etc.
      Class<?> rangeClass = range.getJavaClass();
      if (range.isArray()) {
        if (p.length == 1 && p[0].getType() == int.class) {
          rangeClass = range.getComponentType().getJavaClass();
        }
      }
      if (!rangeClass.isAssignableFrom(returnClass)) { // can return subclass of TOP, OK if range is
                                                       // TOP
        if (rangeClass.getName().equals("org.apache.uima.jcas.cas.Sofa") && // exception: for
                                                                            // backwards compat
                                                                            // reasons, sofaRef
                                                                            // returns SofaFS, not
                                                                            // Sofa.
                returnClass.getName().equals("org.apache.uima.cas.SofaFS")) {
          // empty
        } else {

          /**
           * CAS type system type "{0}" defines field "{1}" with range "{2}", but JCas getter method
           * is returning "{3}" which is not a subtype of the declared range.
           */
          add2errors(errorSet, new CASRuntimeException(CASRuntimeException.JCAS_TYPE_RANGE_MISMATCH,
                  ti.getName(), fi.getShortName(), rangeClass, returnClass), false); // should
                                                                                     // throw, but
                                                                                     // some code
                                                                                     // breaks!
        }
      }
    } // end of checking methods

    try {
      for (Field f : clazz.getDeclaredFields()) {
        String fname = f.getName();
        if (fname.length() <= 5 || !fname.startsWith("_FC_")) {
          continue;
        }
        String featName = fname.substring(4);
        FeatureImpl fi = ti.getFeatureByBaseName(featName);
        if (fi == null) {
          add2errors(errorSet,
                  /**
                   * JCAS class "{0}" defines a UIMA field "{1}" but the UIMA type doesn''t define
                   * that field.
                   */
                  new CASRuntimeException(CASRuntimeException.JCAS_FIELD_MISSING_IN_TYPE_SYSTEM,
                          clazz.getName(), featName),
                  false); // don't throw on this error, field is still set up
          // //debug
          // System.out.format("debug JCAS field not in ts: type: %s, field: %s %n%s%n",
          // clazz.getName(), featName, Misc.getCallers(1, 30));
        } else {
          Field mhf = clazz.getDeclaredField("_FH_" + featName);
          mhf.setAccessible(true);
          MethodHandle mh = (MethodHandle) mhf.get(null);
          int staticOffsetInClass = (int) mh.invokeExact();
          if (fi.getAdjustedOffset() != staticOffsetInClass) {
            /**
             * In JCAS class "{0}", UIMA field "{1}" was set up when this class was previously
             * loaded and initialized, to have an adjusted offset of "{2}" but now the feature has a
             * different adjusted offset of "{3}"; this may be due to something else other than type
             * system commit actions loading and initializing the JCas class, or to having a
             * different non-compatible type system for this class, trying to use a common JCas
             * cover class, which is not supported.
             */
            add2errors(errorSet,
                    new CASRuntimeException(CASRuntimeException.JCAS_FIELD_ADJ_OFFSET_CHANGED,
                            clazz.getName(), fi.getName(), staticOffsetInClass,
                            fi.getAdjustedOffset()),
                    staticOffsetInClass != -1); // throw unless static offset is -1, in that case, a
                                                // runtime error will occur if it is used
          } // end of offset changed
        } // end of feature check
      } // end of for loop
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private static void add2errors(ThreadLocal<List<ErrorReport>> errors, Exception e) {
    add2errors(errors, e, true);
  }

  private static void add2errors(ThreadLocal<List<ErrorReport>> errors, Exception e,
          boolean doThrow) {
    List<ErrorReport> es = errors.get();
    if (es == null) {
      es = new ArrayList<>();
      errors.set(es);
    }
    es.add(new ErrorReport(e, doThrow));
  }

  private static void reportErrors() {
    boolean throwWhenDone = false;
    List<ErrorReport> es = errorSet.get();
    if (es != null) {
      StringBuilder msg = new StringBuilder(100);
      // msg.append('\n'); // makes a break in the message at the beginning, unneeded
      for (ErrorReport f : es) {
        msg.append(f.e.getMessage());
        throwWhenDone = throwWhenDone || f.doThrow;
        msg.append('\n');
      }
      errorSet.set(null); // reset after reporting
      if (throwWhenDone) {
        throw new CASRuntimeException(CASException.JCAS_INIT_ERROR, "\n" + msg);
      } else {
        Logger logger = UIMAFramework.getLogger();
        if (null == logger) {
          throw new CASRuntimeException(CASException.JCAS_INIT_ERROR, "\n" + msg);
        } else {
          logger.log(Level.WARNING, msg.toString());
        }
      }
    }
  }

  // @formatter:off
  /**
   * called infrequently to set up cache
   * Only called when a type system has not had generators for a particular class loader.
   * 
   * For PEAR generators: 
   *   Populates only for those classes the PEAR has overriding implementations
   *     - other entries are null; this serves as a boolean indicator that no pear override exists for that type
   *       and therefore no trampoline is needed
   * 
   * @param cl
   *          identifies which set of jcas cover classes
   * @param isPear
   *          true for pear case
   * @param tsi
   *          the type system being used
   * @return the generators for that set, as an array indexed by type code
   */
  // @formatter:on
  static FsGenerator3[] getGeneratorsForClassLoader(ClassLoader cl, boolean isPear,
          TypeSystemImpl tsi) {
    Map<String, JCasClassInfo> type2jcci = get_className_to_jcci(cl, isPear);
    // final Map<ClassLoader, Map<String, JCasClassInfo>> cl2t2j = isPear ? cl_4pears_to_type2JCas :
    // cl_to_type2JCas;
    // synchronized(cl2t2j) {
    // //debug
    // System.out.format("debug loading JCas for type System %s ClassLoader %s, isPear: %b%n",
    // tsi.hashCode(), cl, isPear);
    // This is the first time this class loader is being used - load the classes for this type
    // system, or
    // This is the first time this class loader is being used with this particular type system

    loadJCasForTSandClassLoader(tsi, true, cl, type2jcci);

    FsGenerator3[] r = new FsGenerator3[tsi.getTypeArraySize()];

    // Map<String, JCasClassInfo> t2jcci = cl2t2j.get(cl);
    // can't use values alone because many types have the same value (due to copy-down)

    // cannot iterate over type2jcci - that map only has types with found JCas classes

    getGeneratorsForTypeAndSubtypes(tsi.topType, type2jcci, isPear, cl, r, tsi);

    // for (Entry<String, JCasClassInfo> e : type2jcci.entrySet()) {
    // TypeImpl ti = tsi.getType(Misc.javaClassName2UimaTypeName(e.getKey()));
    // if (null == ti) {
    // continue; // JCas loaded some type in the past, but it's not in this type system
    // }
    // JCasClassInfo jcci = e.getValue();
    //
    // // skip entering a generator in the result if
    // // in a pear setup, and this cl is not the cl that loaded the JCas class.
    // // See method comment for why.
    // if (!isPear || jcci.isPearOverride(cl)) {
    // r[ti.getCode()] = (FsGenerator3) jcci.generator;
    // }
    // }
    return r;
  }

  private static void getGeneratorsForTypeAndSubtypes(TypeImpl ti,
          Map<String, JCasClassInfo> t2jcci, boolean isPear, ClassLoader cl, FsGenerator3[] r,
          TypeSystemImpl tsi) {

    TypeImpl jti = ti;
    JCasClassInfo jcci = t2jcci.get(jti.getJCasClassName());
    while (jcci == null) {
      jti = jti.getSuperType();
      jcci = t2jcci.get(jti.getJCasClassName());
    }

    // skip entering a generator in the result if
    // in a pear setup, and this cl is not the cl that loaded the JCas class.
    // See method comment getGeneratorsForClassLoader(...) in for why.
    if (!isPear || jcci.isPearOverride(tsi)) {
      r[ti.getCode()] = (FsGenerator3) jcci.generator;
    }

    for (TypeImpl subtype : ti.getDirectSubtypes()) {
      getGeneratorsForTypeAndSubtypes(subtype, t2jcci, isPear, cl, r, tsi);
    }

  }

  private static boolean isAllNull(FsGenerator3[] r) {
    for (FsGenerator3 v : r) {
      if (v != null) {
        return false;
      }
    }
    return true;
  }

  // @formatter:off
  /**
   * Called once when the JCasClassInfo is created.
   * Once set, the offsets are never changed (although they could be...)
   * 
   * New type systems are checked for conformance to existing settings in the JCas class.
   * Type System types are augmented by features defined in the JCas
   *   but missing in the type, before this routine is called.
   * 
   * Iterate over all fields named _FC_  followed by a feature name.  
   *   If that feature doesn't exist in this type system - skip init, will cause runtime error if used
   *   Else, set the callSite's method Handle to one that returns the int constant for type system's offset of that feature.
   *     If already set, check that the offset didn't change.
   * 
   * 
   * @param clazz
   *          -
   * @param type
   *          -
   */
  // @formatter:on
  private static void updateOrValidateAllCallSitesForJCasClass(Class<? extends TOP> clazz,
          TypeImpl type, ArrayList<MutableCallSite> callSites_toSync) {
    try {
      Field[] fields = clazz.getDeclaredFields();

      for (Field field : fields) {
        String fieldName = field.getName();
        if (fieldName.startsWith("_FC_")) {

          // //debug
          // System.out.println("debug " + fieldName);
          String featureName = fieldName.substring("_FC_".length());
          final int index = TypeSystemImpl.getAdjustedFeatureOffset(type, featureName);
          // //debug
          // if (type.getShortName().equals("Split") && featureName.equals("splits")
          // ) {
          // System.out.println("debug attempting to set offset for splits in Splits to " + index);
          // System.out.println(type.toString(2));
          // System.out.println(Misc.getCallers(1, 32));
          // }
          if (index == -1) {
            continue; // a feature defined in the JCas class doesn't exist in the currently loaded
                      // type
          } // skip setting it. If code uses this, a runtime error will happen.
            // only happens for pear-loaded lazyily JCas classes
            // "Normal" loaded JCas classes (at start of type system commit)
            // have any extra features added to the type system
            // https://issues.apache.org/jira/browse/UIMA-5698

          MutableCallSite c;
          field.setAccessible(true);
          c = (MutableCallSite) field.get(null);

          if (c == null) { // happens when first load of TypeSystemImpl is from JCas class ref
            continue; // will be set later when type system is committed.
          }

          int prev = (int) c.getTarget().invokeExact();
          if (prev == -1) { // the static method in JCas classes, TypeSystemImpl.createCallSite,
                            // initializes the call site with a method handle that returns -1
            MethodHandle mh_constant = getConstantIntMethodHandle(index);
            c.setTarget(mh_constant);
            callSites_toSync.add(c);
          } else if (prev != index) {
            // This is one of two errors.
            // It could also be caused by the range type switching from ref array to the int array
            checkConformance(clazz.getClassLoader(), type.getTypeSystem());
            reportErrors();
            // //debug
            // System.err.format(
            // "Debug incompat offset jcas, class = %s, type= %s, classIndex = %d, type index:
            // %d%n",
            // clazz.getName(), type.getName(), prev, index);
            // System.err.flush();
            throw new UIMA_IllegalStateException(
                    UIMA_IllegalStateException.JCAS_INCOMPATIBLE_TYPE_SYSTEMS,
                    new Object[] { type.getName(), featureName });
          }
        }
      }
    } catch (Throwable e) {
      Misc.internalError(e); // never happen
    }
  }

  /**
   * For internal use only!
   */
  public static void unregister_jcci_classloader(ClassLoader cl) {
    synchronized (cl_to_type2JCas) {
      cl_to_type2JCas.remove(cl);
      if (cl_to_type2JCasStacks != null) {
        cl_to_type2JCasStacks.remove(cl);
      }
    }
  }

  /**
   * For internal use only!
   */
  public static void log_registered_classloaders(Level aLogLevel) {
    Logger log = UIMAFramework.getLogger(lookup().lookupClass());
    if (cl_to_type2JCasStacks == null) {
      log.warn(
              "log_registered_classloaders called but classLoader registration stack logging "
                      + "is not turned on. Define the system property [{}] to enable it.",
              LOG_JCAS_CLASSLOADERS_ON_SHUTDOWN);
      return;
    }

    Map<ClassLoader, StackTraceElement[]> clToLog = new LinkedHashMap<>();
    synchronized (cl_to_type2JCas) {
      Iterator<ClassLoader> i = cl_to_type2JCas.keyIterator();
      while (i.hasNext()) {
        ClassLoader cl = i.next();
        if (cl == TypeSystemImpl.staticTsi.getClass().getClassLoader()) {
          // This is usually the default/system classloader and is registered when the
          // TypeSystemImpl class is statically intialized. It is not interesting for leak
          // detection.
          continue;
        }
        StackTraceElement[] stack = cl_to_type2JCasStacks.get(cl);
        if (stack == null) {
          continue;
        }
        clToLog.put(cl, stack);
      }
    }

    if (clToLog.isEmpty()) {
      log.log(aLogLevel, "No classloaders except the system classloader registered.");
      return;
    }

    StringBuilder buf = new StringBuilder();

    if (aLogLevel.isGreaterOrEqual(Level.WARN)) {
      buf.append("On shutdown, there were still " + clToLog.size()
              + " classloaders registered in the FSClassRegistry. Not destroying "
              + "ResourceManagers after usage can cause memory leaks.");
    } else {
      buf.append(
              "There are " + clToLog.size() + " classloaders registered in the FSClassRegistry:");
    }

    int i = 1;
    for (Entry<ClassLoader, StackTraceElement[]> e : clToLog.entrySet()) {
      buf.append("[" + i + "] " + e.getKey() + " registered through:\n");
      for (StackTraceElement s : e.getValue()) {
        buf.append("    " + s + "\n");
      }
      i++;
    }

    log.log(aLogLevel, buf.toString());
  }

  static Map<String, JCasClassInfo> get_className_to_jcci(ClassLoader cl, boolean is_pear) {
    synchronized (cl_to_type2JCas) {
      // This was used before switching from the normal synchronized map to the weak map
      // and is part of a whole bunch of commented out code with special handling for the PEAR
      // case. Not sure if this commented out code was kept for debugging or for historical
      // reasons - so let's keep this for the moment as a comment here.
      // REC - 2020-10-17
      // final Map<ClassLoader, Map<String, JCasClassInfo>> cl2t2j = cl_to_type2JCas; /*is_pear ?
      // cl_4pears_to_type2JCas :*/
      Map<String, JCasClassInfo> cl_to_jcci = cl_to_type2JCas.get(cl);
      if (cl_to_jcci == null) {
        cl_to_jcci = new HashMap<>();
        cl_to_type2JCas.put(cl, cl_to_jcci);
        if (cl_to_type2JCasStacks != null) {
          cl_to_type2JCasStacks.put(cl, new RuntimeException().getStackTrace());
        }
      }
      return cl_to_jcci;
    }
  }

  private static final URL[] NO_URLS = new URL[0];

  static Lookup getLookup(ClassLoader cl) {
    Lookup lookup = null;
    try {
      // The UIMAClassLoader has special handling for the MHLC, so we must make sure that the CL
      // we are using is actually a UIMAClassLoader, otherwise the MHCL will look up in the wrong
      // CL. This is in particular an issue for classes loaded through the SPI mechanism.
      UIMAClassLoader ucl;
      if (!(cl instanceof UIMAClassLoader)) {
        synchronized (cl_to_uimaCl) {
          ucl = cl_to_uimaCl.get(cl);
          if (ucl == null) {
            ucl = new UIMAClassLoader(NO_URLS, cl);
            cl_to_uimaCl.put(cl, ucl);
          }
        }
      } else {
        ucl = (UIMAClassLoader) cl;
      }

      Class<?> clazz = Class.forName(UIMAClassLoader.MHLC, true, ucl);
      Method m = clazz.getMethod("getMethodHandlesLookup");
      lookup = (Lookup) m.invoke(null);
    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException
            | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new UIMARuntimeException(e, UIMARuntimeException.INTERNAL_ERROR);
    }
    return lookup;
  }
}
