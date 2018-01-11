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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.impl.FSClassRegistry.JCasClassInfo;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.UIMAClassLoader;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

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

public abstract class FSClassRegistry { // abstract to prevent instantiating; this class only has static methods
  
  /* ========================================================= */
  /*    This class has only static methods and fields          */
  /*    To allow multi-threaded use, some fields are           */
  /*      kept in a thread local                               */
  /* ========================================================= */
  
  /* ==========================++++++=== */
  /*   Static final, not in thread local */
  /*     not sync-ed                     */
  /* ==========================++++++=== */

  // Used for the built-ins.
  private static final Lookup defaultLookup = MethodHandles.lookup();

  private static final MethodType findConstructorJCasCoverType      = methodType(void.class, TypeImpl.class, CASImpl.class);
  //private static final MethodType findConstructorJCasCoverTypeArray = methodType(void.class, TypeImpl.class, CASImpl.class, int.class);
  /**
   * The callsite has the return type, followed by capture arguments
   */
  private static final MethodType callsiteFsGenerator      = methodType(FsGenerator3.class);
  //private static final MethodType callsiteFsGeneratorArray = methodType(FsGeneratorArray.class);  // NO LONGER USED
  
  private static final MethodType fsGeneratorType      = methodType(TOP.class, TypeImpl.class, CASImpl.class);
  //private static final MethodType fsGeneratorArrayType = methodType(TOP.class, TypeImpl.class, CASImpl.class, int.class); // NO LONGER USED

  /**
   * precomputed generators for built-in types
   * These instances are shared for all type systems
   * Key = index = typecode
   */
  private static final JCasClassInfo[] jcasClassesInfoForBuiltins;

  /* =================================== */
  /*    not in thread local, sync-ed     */
  /* =================================== */

  /** a cache for constant int method handles */
  private static final List<MethodHandle> methodHandlesForInt = Collections.synchronizedList(new ArrayList<>());
  
  /**
   * Map from class loaders used to load JCas Classes, both PEAR and non-Pear cases, to JCasClassInfo for that loaded JCas class instance.
   *   value is a map:
   *     Key is JCas fully qualified name (not UIMA type name).
   *       Is String, since different type systems may use the same JCas classes.
   *     value is the JCasClassInfo for that class
   * Cache of FsGenerator[]s kept in TypeSystemImpl instance, since it depends on type codes.
   * Current FsGenerator[] kept in CASImpl shared view data, switched as needed for PEARs. 
   */
  private static final Map<ClassLoader, Map<String, JCasClassInfo>> cl_to_type2JCas = Collections.synchronizedMap(new IdentityHashMap<>());
  
  private static final Map<ClassLoader, Map<String, JCasClassInfo>> cl_4pears_to_type2JCas = Collections.synchronizedMap(new IdentityHashMap<>());

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
 
//  /**
//   * Map (per class loader) from JCas Classes, to all callSites in that JCas class 
//   */
//  public static final Map<Class<? extends TOP>, ArrayList<Entry<String, MutableCallSite>>> callSites_all_JCasClasses = new HashMap<>();

  /**
   * One instance per UIMA Type per class loader
   *   - per class loader, because different JCas class definitions for the same name are possible, per class loader
   *   
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
  public static class JCasClassInfo {
    
    final FsGenerator3 generator;
   
    /**
     * The corresponding loaded JCas Class for this UIMA type, may be a JCas class associated with a UIMA supertype
     *   if no JCas class is found for this type.
     */
    final Class<? extends TOP> jcasClass;
    
    /**
     * NOT the TypeCode, but instead, the unique int assigned to the JCas class 
     * when it gets loaded
     * Might be -1 if the JCasClassInfo instance is for a non-JCas instantiable type
     */
    final int jcasType;
    
    final JCasClassFeatureInfo[] features;
    
    boolean isAlreadyCheckedBuiltIn;
        
    JCasClassInfo(Class<? extends TOP> jcasClass, FsGenerator3 generator, int jcasType) {
      this.generator = generator;
      this.jcasClass = jcasClass;
      this.jcasType = jcasType;    // typeId for jcas class, **NOT Typecode**
      this.features = getJCasClassFeatureInfo(jcasClass);
      
//      System.out.println("debug create jcci, class = " + jcasClass.getName() + ", typeint = " + jcasType);
    }
    
    boolean isCopydown(TypeImpl ti) {
      return isCopydown(Misc.typeName2ClassName(ti.getName()));
    }
  
    boolean isCopydown(String jcasClassName) {
      return !jcasClass.getCanonicalName().equals(jcasClassName);
    }
    
    boolean isPearOverride(ClassLoader cl) {
      return jcasClass.getClassLoader().equals(cl);
    }
  }
  
  /**
   * Information about all features this JCas class defines
   * Used to expand the type system when the JCas defines more features
   * than the type system declares.
   */
  private static class JCasClassFeatureInfo {
    final String shortName;
    // rangename is byte.class, etc
    // or x.y.z.JCasClassName
    final String uimaRangeName;       
    
    JCasClassFeatureInfo(String shortName, String uimaRangeName) {
      this.shortName = shortName;
      this.uimaRangeName = uimaRangeName;
    }
  }
    

  static {
    TypeSystemImpl tsi = TypeSystemImpl.staticTsi;
    jcasClassesInfoForBuiltins = new JCasClassInfo[tsi.getTypeArraySize()]; 
//    lookup = defaultLookup;
        
    // walk in subsumption order, supertype before subtype
    // Class loader used for builtins is the UIMA framework's class loader
    ArrayList<MutableCallSite> callSites_toSync = new ArrayList<>();
    ClassLoader cl = tsi.getClass().getClassLoader();
    loadBuiltins(tsi.topType, cl, cl_to_type2JCas.computeIfAbsent(cl, x -> new HashMap<>()), callSites_toSync);
    
    MutableCallSite[] sync = callSites_toSync.toArray(new MutableCallSite[callSites_toSync.size()]);
    MutableCallSite.syncAll(sync);

    reportErrors();
  }
    
  private static void loadBuiltins(TypeImpl ti, ClassLoader cl, Map<String, JCasClassInfo> type2jcci, ArrayList<MutableCallSite> callSites_toSync) {
    String typeName = ti.getName();
    
    if (BuiltinTypeKinds.creatableBuiltinJCas.contains(typeName) || typeName.equals(CAS.TYPE_NAME_SOFA)) {
      JCasClassInfo jcasClassInfo = getOrCreateJCasClassInfo(ti, cl, type2jcci, callSites_toSync, defaultLookup);
      assert jcasClassInfo != null;
      jcasClassesInfoForBuiltins[ti.getCode()] = jcasClassInfo;
      
//      Class<?> builtinClass = maybeLoadJCas(ti, cl);
//      assert (builtinClass != null);  // builtin types must be present
//      // copy down to subtypes, if needed, done later
//      int jcasType = Misc.getStaticIntFieldNoInherit(builtinClass, "typeIndexID");
//      JCasClassInfo jcasClassInfo = createJCasClassInfo(builtinClass, ti, jcasType); 
//      jcasClassesInfoForBuiltins[ti.getCode()] = jcasClassInfo; 
    }
    
    for (TypeImpl subType : ti.getDirectSubtypes()) {
      TypeSystemImpl.typeBeingLoadedThreadLocal.set(subType);
      loadBuiltins(subType, cl, type2jcci, callSites_toSync);
    }
  }
    
  /**
   * Load JCas types for some combination of class loader and type system
   * Some of these classes may have already been loaded for this type system
   * Some of these classes may have already been loaded (perhaps for another type system)
   * @param ts the type system
   * @param isDoUserJCasLoading always true, left in for experimentation in the future with dynamic generation of JCas classes
   * @param cl the class loader. For Pears, is the pear class loader
   */
  private static synchronized void loadJCasForTSandClassLoader(
      TypeSystemImpl ts, 
      boolean isDoUserJCasLoading, 
      ClassLoader cl, 
      Map<String, JCasClassInfo> type2jcci) { 

//    boolean alreadyPartiallyLoaded = false;  // true if any JCas types for this class loader have previously been loaded

//      synchronized (cl2t2j) {
//      type2jcci = cl2t2j.get(cl);
//    
//      if (null == type2jcci) {    
//        type2jcci = new HashMap<>();
//        cl2t2j.put(cl, type2jcci);
//      } else {
//        alreadyPartiallyLoaded = true;
//      }
//    }
    
    /**
     * copy in built-ins
     *   update t2jcci (if not already loaded) with load info for type
     *   update type system's map from unique JCasID to the type in this type system
     */
     
    /* ============================== */
    /*            BUILT-INS           */
    /* ============================== */
    for (int typecode = 1; typecode < jcasClassesInfoForBuiltins.length; typecode++) {
  
      JCasClassInfo jcci = jcasClassesInfoForBuiltins[typecode];
      if (jcci != null) {
        Class<? extends TOP> jcasClass = jcci.jcasClass;  
        type2jcci.putIfAbsent(jcasClass.getCanonicalName(), jcci);
        setTypeFromJCasIDforBuiltIns(jcci, ts, typecode);
        // update call sites not called, was called when
        // jcci was created.
//        updateAllCallSitesForJCasClass(jcasClass, ts.getTypeForCode(typecode));
      }
    }  

    /* ========================================================= */
    /*   Add all user-defined JCas Types, in subsumption order   */
    /* ========================================================= */

    
    if (isDoUserJCasLoading) {
      /**
       * Two passes are needed loading is needed.  
       *   - The first one loads the JCas Cover Classes initializes everything
       *      -- some of the classes might already be loaded (including the builtins which are loaded once per class loader)
       *   - The second pass performs the conformance checks between the loaded JCas cover classes, and the current type system.
       *     This depends on having the TypeImpl's javaClass field be accurate (reflect any loaded JCas types)
       */
      
      // this is this here rather than in a static initializer, because 
      // we want to use the "cl" parameter to get a version of the 
      // getMethodHandlesLookup that will have the right (maybe more local) permissions checking.
      // This is done by having the UIMA Class loader notice that the class being loaded is 
      //   MHLC, and then dynamically loading in that class loader a copy of the byte code 
      //   for that class.
      Lookup lookup = getLookup(cl);

      ArrayList<MutableCallSite> callSites_toSync = new ArrayList<>();
      maybeLoadJCasAndSubtypes(ts, ts.topType, type2jcci.get(TOP.class.getCanonicalName()), cl, type2jcci, callSites_toSync, lookup);
      
      MutableCallSite[] sync = callSites_toSync.toArray(new MutableCallSite[callSites_toSync.size()]);
      MutableCallSite.syncAll(sync);

      checkConformance(ts, ts.topType, type2jcci);
    }
        
    reportErrors();
  }

  private static void setTypeFromJCasIDforBuiltIns(JCasClassInfo jcci, TypeSystemImpl tsi, int typeCode) {
    int v = jcci.jcasType;
    // v is negative if not found, which is the case for types like FloatList (these can't be instantiated)
    if (v >= 0) {
      tsi.setJCasRegisteredType(v, tsi.getTypeForCode(typeCode));
    }
  }

  /**
   * Called for all the types, including the built-ins, but the built-ins have already been set up by the caller.
   * Saves the results in two places
   *   type system independent spot: JCasClassInfo instance indexed by JCasClassName
   *   type system spot: the JCasIndexID -> type table in the type system
   * @param ts the type system
   * @param ti the type to process
   * @param copyDownDefault_jcasClassInfo
   * @param cl the loader used to load, and to save the results under the key of the class loader the results
   * @param type2JCas map holding the results of loading JCas classes
   */
  private static void maybeLoadJCasAndSubtypes(
      TypeSystemImpl ts, 
      TypeImpl ti, 
      JCasClassInfo copyDownDefault_jcasClassInfo,
      ClassLoader cl,
      Map<String, JCasClassInfo> type2jcci,
      ArrayList<MutableCallSite> callSites_toSync,
      Lookup lookup) {
        
    String t2jcciKey = Misc.typeName2ClassName(ti.getName());
    JCasClassInfo jcci = type2jcci.get(t2jcciKey);
    boolean isCopyDown;

    if (jcci == null) {
      
      jcci = getOrCreateJCasClassInfo(ti, cl, type2jcci, callSites_toSync, lookup);
      isCopyDown = false;
      if (null == jcci) {
        jcci = copyDownDefault_jcasClassInfo;
        isCopyDown = true;
      }

//      // not yet recorded as loaded under this class loader.
//    
//      Class<?> clazz = maybeLoadJCas(ti, cl);
//      if (null != clazz && TOP.class.isAssignableFrom(clazz)) {
//        
//        int jcasType = -1;
//        if (!Modifier.isAbstract(clazz.getModifiers())) { // skip next for abstract classes
//          jcasType = Misc.getStaticIntFieldNoInherit(clazz, "typeIndexID");
//          // if jcasType is negative, this means there's no value for this field
//          assert(jcasType >= 0);
//        }         
//        jcci = createJCasClassInfo(clazz, ti, jcasType);
//        isCopyDown = false;
//        // don't do this call, caller will call conformance which has a weaker
//        // test - passes if there is a shared
//        // https://issues.apache.org/jira/browse/UIMA-5660
////        if (clazz != TOP.class) {  // TOP has no super class  
////          validateSuperClass(jcci, ti);
////        }
//      } else {
//        jcci = copyDownDefault_jcasClassInfo;
//      }
      
      type2jcci.put(t2jcciKey, jcci);

    } else {
      if (ti.getTypeSystem().isCommitted()) {
        updateAllCallSitesForJCasClass(jcci.jcasClass, ti, callSites_toSync);
      }
      
      
      // this UIMA type was set up (maybe loaded, maybe defaulted to a copy-down) previously
      isCopyDown = jcci.isCopydown(t2jcciKey);

      if (isCopyDown) {
        // the "stored" version might have the wrong super class for this type system
        type2jcci.put(t2jcciKey, jcci = copyDownDefault_jcasClassInfo);
        
      } else if (!ti.isTopType()) {
        // strong test for non-copy-down case: supertype must match, with 2 exceptions
        // removed https://issues.apache.org/jira/browse/UIMA-5660
//        validateSuperClass(jcci, ti);
      }
    }
       
    // this is done even after the class is first loaded, in case the type system changed.
    // don't set anything if copy down - otherwise was setting the copyed-down typeId ref to the 
    //   new ti
//    System.out.println("debug set jcas regisered type " + jcci.jcasType + ",  type = " + ti.getName());

    if (jcci.jcasType >= 0 && ! isCopyDown) {
      ts.setJCasRegisteredType(jcci.jcasType, ti); 
    }
    
    if (!ti.isPrimitive()) {  // bypass this for primitives because the jcasClassInfo is the "inherited one" of TOP
      /**
       * Note: this value sets into the shared TypeImpl (maybe shared among many JCas impls) the "latest" jcasClass
       * It is "read" by the conformance testing, while still under the type system lock.
       * Other uses of this may get an arbitrary (the latest) version of the class
       * Currently the only other use is in backwards compatibility with low level type system "switching" an existing type.
       */
      ti.setJavaClass(jcci.jcasClass);
    }
    
    
    for (TypeImpl subtype : ti.getDirectSubtypes()) {
      TypeSystemImpl.typeBeingLoadedThreadLocal.set(subtype);  // not used, but left in for backwards compat.
      maybeLoadJCasAndSubtypes(ts, subtype, jcci, cl, type2jcci, callSites_toSync, lookup);
    }
  }

  /** 
   * only called for non-Pear callers
   * @param ti -
   * @param cl -
   * @return -
   */
  public static JCasClassInfo getOrCreateJCasClassInfo(
      TypeImpl ti, 
      ClassLoader cl, 
      Map<String, JCasClassInfo> type2jcci, 
      ArrayList<MutableCallSite> callSites_toSync,
      Lookup lookup) {
    final String t2jcciKey = Misc.typeName2ClassName(ti.getName());
    
    JCasClassInfo jcci = type2jcci.get(t2jcciKey);

    if (jcci == null) {
      // for this class loader, no entry for this type name
      jcci = createJCasClassInfo(ti, cl, callSites_toSync, lookup);
      type2jcci.put(t2jcciKey, jcci);
    } else if (ti.getTypeSystem().isCommitted()) {
//      try { 
        updateAllCallSitesForJCasClass(jcci.jcasClass, ti, callSites_toSync);
//      } finally {
//        TypeSystemImpl.typeBeingLoadedThreadLocal.set(null);
//      }
    }
      
    return jcci;
  }
  
  
  public static JCasClassInfo createJCasClassInfo(
      TypeImpl ti, 
      ClassLoader cl, 
      ArrayList<MutableCallSite> callSites_toSync, 
      Lookup lookup) {
    Class<? extends TOP> clazz = maybeLoadJCas(ti, cl);
    
    if (null == clazz || ! TOP.class.isAssignableFrom(clazz)) {
      return null;
    }
    
    int jcasType = -1;
    if (!Modifier.isAbstract(clazz.getModifiers())) { // skip next for abstract classes
      jcasType = Misc.getStaticIntFieldNoInherit(clazz, "typeIndexID");
      // if jcasType is negative, this means there's no value for this field
      if (jcasType == -1) {
        add2errors(errorSet, 
                   /** The Class "{0}" matches a UIMA Type, and is a subtype of uima.cas.TOP, but is missing the JCas typeIndexId.*/
                   new CASRuntimeException(CASRuntimeException.JCAS_MISSING_TYPEINDEX, clazz.getName()),
                   false);  // not a fatal error
        return null;
      }
    }         
    if (ti.getTypeSystem().isCommitted()) {
      try { 
        updateAllCallSitesForJCasClass(clazz, ti, callSites_toSync);
      } finally {
        TypeSystemImpl.typeBeingLoadedThreadLocal.set(null);
      }
    }
    return createJCasClassInfo(clazz, ti, jcasType, lookup);
  }
  
  static void augmentFeaturesFromJCas(
      TypeImpl type, 
      ClassLoader cl, 
      TypeSystemImpl tsi, 
      Map<String, 
      JCasClassInfo> type2jcci,
      Lookup lookup) {
    
    if (type.isBuiltIn) {
      return;
    }
    
    /**************************************************************************************
     *    N O T E :                                                                       *
     *    fixup the ordering of staticMergedFeatures:                                     *
     *      - supers, then features introduced by this type.                              *
     *      - order may be "bad" if later feature merge introduced an additional feature  *
     **************************************************************************************/
    type.getFeatureImpls(); // done to reorder the features if needed, see above comment block
    
    if ( ! type.isTopType()) {
      // skip for top level; no features there, but no super type either
      type.getFeatureImpls(); // done for side effect of computingcomputeStaticMergedFeaturesList();
      TypeSystemImpl.typeBeingLoadedThreadLocal.set(type);
      JCasClassInfo jcci = getOrCreateJCasClassInfo(type, cl, type2jcci, null, lookup);  // no call site sync
      if (jcci != null) {
        for (JCasClassFeatureInfo f : jcci.features) {
          FeatureImpl fi = type.getFeatureByBaseName(f.shortName);
          if (fi == null) {
            // feature is missing in the type, add it
            // Range is either one of the uima primitives, or
            // a fs reference.  FS References could be to "unknown" types in this type system.
            // If so, use TOP
            TypeImpl rangeType = tsi.getType(f.uimaRangeName);
            if (rangeType == null) {
              rangeType = tsi.topType;
            }            
            tsi.addFeature(f.shortName, type, rangeType);
          }
        }
      }
    }
     
    
    for (TypeImpl subti : type.getDirectSubtypes()) {
      augmentFeaturesFromJCas(subti, cl, tsi, type2jcci, lookup);
    }
  }

  
  
//  private static String superTypeJCasName(TypeImpl ti) {
//    return Misc.typeName2ClassName(ti.getSuperType().getName());
//  }
//  
//  /**
//   * Removed https://issues.apache.org/jira/browse/UIMA-5660
//   * verify that the supertype class chain matches the type
//   * @param clazz -
//   * @param ti -
//   */
//  private static void validateSuperClass(JCasClassInfo jcci, TypeImpl ti) {
//    final Class<?> clazz = jcci.jcasClass; 
//    if (! clazz.getSuperclass().getCanonicalName().equals(superTypeJCasName(ti))) {
//      /** Special case exceptions */
//      TypeImpl superti = ti.getSuperType();
//      TypeSystemImpl tsi = ti.getTypeSystem();
//      if (superti == tsi.arrayBaseType ||
//          superti == tsi.listBaseType) return;
//      /** The JCas class: "{0}" has supertype: "{1}" which doesn''t  match the UIMA type "{2}"''s supertype "{3}". */
//      throw new CASRuntimeException(CASRuntimeException.JCAS_MISMATCH_SUPERTYPE,
//        clazz.getCanonicalName(), 
//        clazz.getSuperclass().getCanonicalName(),
//        ti.getName(),
//        ti.getSuperType().getName());
//    }
//
//  }
  
  /**
   * Called to load (if possible) a corresponding JCas class for a UIMA type.
   * Called at Class Init time for built-in types
   * Called at TypeSystemCommit for non-built-in types
   *   Runs the static initializers in the loaded JCas classes - doing resolve
   *   
   * Synchronization: done outside this class
   *   
   * @param typeName -
   * @param cl the class loader to use
   * @return the loaded / resolved class
   */
  private static Class<? extends TOP> maybeLoadJCas(TypeImpl ti, ClassLoader cl) {
    Class<? extends TOP> clazz = null;
    String className = Misc.typeName2ClassName(ti.getName());
    
    try { 
      clazz = (Class<? extends TOP>) Class.forName(className, true, cl);
    } catch (ClassNotFoundException e) {
      // Class not found is normal, if there is no JCas for this class
      return clazz;
    } catch (Throwable e) {
      e.printStackTrace(System.err);
    }
    return clazz;
  }
      
  static synchronized MethodHandle getConstantIntMethodHandle(int i) {
    MethodHandle mh = Misc.getWithExpand(methodHandlesForInt, i);
    if (mh == null) {
      methodHandlesForInt.set(i, mh = MethodHandles.constant(int.class, i));
    }
    return mh;    
  }
  
  /**
   * Return a Functional Interface for a generator for creating instances of a type.
   *   Function takes a casImpl arg, and returning an instance of the JCas type.
   * @param jcasClass the class of the JCas type to construct
   * @param typeImpl the UIMA type
   * @return a Functional Interface whose createFS method takes a casImpl 
   *         and when subsequently invoked, returns a new instance of the class
   */
  private static FsGenerator3 createGenerator(Class<?> jcasClass, Lookup lookup) {
    try {
      
      MethodHandle mh = lookup.findConstructor(jcasClass, findConstructorJCasCoverType);
      MethodType mtThisGenerator = methodType(jcasClass, TypeImpl.class, CASImpl.class);
 
      CallSite callSite = LambdaMetafactory.metafactory(
          lookup, // lookup context for the constructor 
          "createFS", // name of the method in the Function Interface 
          callsiteFsGenerator, // signature of callsite, return type is functional interface, args are captured args if any
          fsGeneratorType,  // samMethodType signature and return type of method impl by function object 
          mh,  // method handle to constructor 
          mtThisGenerator);
      return (FsGenerator3) callSite.getTarget().invokeExact();
    } catch (Throwable e) {
      if (e instanceof NoSuchMethodException) {
        String classname = jcasClass.getName();
        add2errors(errorSet, new CASRuntimeException(e, CASRuntimeException.JCAS_CAS_NOT_V3, 
            classname,
            jcasClass.getClassLoader().getResource(classname.replace('.', '/') + ".class").toString()
            ));
        return null;
      }
      /** An internal error occurred, please report to the Apache UIMA project; nested exception if present: {0} */
      throw new UIMARuntimeException(e, UIMARuntimeException.INTERNAL_ERROR);
    }
  }
  
//  /**
//   * Return a Functional Interface for a getter for getting the value of a feature, 
//   * called by APIs using the non-JCas style of access via features, 
//   * but accessing the values via the JCas cover class getter methods.
//   * 
//   * The caller of these methods is the FeatureStructureImplC methods.  
//   * 
//   * There are these return values:
//   *   boolean, byte, short, int, long, float, double, String, FeatureStructure
//   *   
//   */
//  // static for setting up builtin values
//  private static Object createGetterOrSetter(Class<?> jcasClass, FeatureImpl fi, boolean isGetter, boolean ncnj) {
//    
//    TypeImpl range = fi.getRangeImpl();
//    String name = ncnj ? ("_" + fi.getGetterSetterName(isGetter) + "NcNj")
//                       :        fi.getGetterSetterName(isGetter); 
//    
//    try {
//      /* get an early-bound getter    
//      /* Instead of findSpecial, we use findVirtual, in case the method is overridden by a subtype loaded later */
//      MethodHandle mh = lookup.findVirtual(
//          jcasClass,  // class having the method code for the getter 
//          name,       // the name of the method for the getter or setter 
//          isGetter ? methodType(range.javaClass)
//                   : methodType(void.class, range.javaClass) // return value, e.g. int.class, xyz.class, FeatureStructureImplC.class
//        );
//      
//      // getter methodtype is return_type, FeatureStructure.class
//      //   return_type is int, byte, etc. primitive (except string/substring), or
//      //   object (to correspond with erasure)
//      // setter methodtype is void.class, FeatureStructure.class, javaclass
//      MethodType mhMt = isGetter ? methodType(range.getJavaPrimitiveClassOrObject(), FeatureStructureImplC.class)
//                                 : methodType(void.class, FeatureStructureImplC.class, range.getJavaPrimitiveClassOrObject());
//      MethodType iMt =  isGetter ? methodType(range.javaClass, jcasClass)
//                                 : methodType(void.class, jcasClass, range.javaClass);
//      
////      System.out.format("mh method type for %s method %s is %s%n", 
////          jcasClass.getSimpleName(), 
////          fi.getGetterSetterName(isGetter),
////          mhMt);
//          
//      CallSite callSite = LambdaMetafactory.metafactory(
//          lookup,     // lookup context for the getter
//          isGetter ? "get" : "set", // name of the method in the Function Interface                 
//          methodType(isGetter ? range.getter_funct_intfc_class : range.setter_funct_intfc_class),  // callsite signature = just the functional interface return value
//          mhMt,                      // samMethodType signature and return type of method impl by function object 
//          mh,  // method handle to constructor 
//          iMt);
//    
//      if (range.getJavaClass() == boolean.class) {
//        return isGetter ? (JCas_getter_boolean) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_boolean) callSite.getTarget().invokeExact();
//      } else if (range.getJavaClass() == byte.class) {
//        return isGetter ? (JCas_getter_byte) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_byte) callSite.getTarget().invokeExact();
//      } else if (range.getJavaClass() == short.class) {
//        return isGetter ? (JCas_getter_short) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_short) callSite.getTarget().invokeExact();
//      } else if (range.getJavaClass() == int.class) {
//        return isGetter ? (JCas_getter_int) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_int) callSite.getTarget().invokeExact();
//      } else if (range.getJavaClass() == long.class) {
//        return isGetter ? (JCas_getter_long) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_long) callSite.getTarget().invokeExact();
//      } else if (range.getJavaClass() == float.class) {
//        return isGetter ? (JCas_getter_float) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_float) callSite.getTarget().invokeExact();
//      } else if (range.getJavaClass() == double.class) {
//        return isGetter ? (JCas_getter_double) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_double) callSite.getTarget().invokeExact();
//      } else {
//        return isGetter ? (JCas_getter_generic<?>) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_generic<?>) callSite.getTarget().invokeExact();
//      }
//    } catch (NoSuchMethodException e) {
//      if ((jcasClass == Sofa.class && !isGetter) ||
//          (jcasClass == AnnotationBase.class && !isGetter)) {
//        return null;
//      }  
//      // report missing setter or getter
//      /* Unable to find required {0} method for JCAS type {1} with {2} type of {3}. */
//      CASException casEx = new CASException(CASException.JCAS_GETTER_SETTER_MISSING, 
//          name,
//          jcasClass.getName(),
//          isGetter ? "return" : "argument",
//          range.javaClass.getName()     
//          );
//      ArrayList<Exception> es = errorSet.get();
//      if (es == null) {
//        es = new ArrayList<Exception>();
//        errorSet.set(es);
//      }
//      es.add(casEx);
//      return null;
//    } catch (Throwable e) {
//      throw new UIMARuntimeException(e, UIMARuntimeException.INTERNAL_ERROR);
//    }
//  }
   
//  GetterSetter getGetterSetter(int typecode, String featShortName) {
//    return jcasClassesInfo[typecode].gettersAndSetters.get(featShortName);
//  }
   
  // static for setting up static builtin values
  /**
   * Called after succeeding at loading, once per load for an exact matching JCas Class 
   *   - class was already checked to insure is of proper type for JCas
   *   - skips creating-generator-for-Sofa - since "new Sofa(...)" is not a valid way to create a sofa   
   * 
   * @param jcasClass the JCas class that corresponds to the type
   * @param ti the type
   * @return the info for this JCas that is shared across all type systems under this class loader
   */
  private static JCasClassInfo createJCasClassInfo(
      Class<? extends TOP> jcasClass, 
      TypeImpl ti, 
      int jcasType, 
      Lookup lookup) {
    boolean noGenerator = ti.getCode() == TypeSystemConstants.sofaTypeCode ||
                          Modifier.isAbstract(jcasClass.getModifiers()) ||
                          ti.isArray(); 
    FsGenerator3 generator = noGenerator ? null : createGenerator(jcasClass, lookup);
    JCasClassInfo jcasClassInfo = new JCasClassInfo(jcasClass, generator, jcasType);
//    System.out.println("debug creating jcci, classname = " + jcasClass.getName() + ", jcasTypeNumber: " + jcasType);
    return jcasClassInfo;
  }
  
  private static JCasClassFeatureInfo[] getJCasClassFeatureInfo(Class<?> jcasClass) {
    ArrayList<JCasClassFeatureInfo> features = new ArrayList<>();
    
    try {
      for (Field f : jcasClass.getDeclaredFields()) {
        String fname = f.getName();
        if (fname.length() <= 5 || !fname.startsWith("_FC_")) continue;
        String featName = fname.substring(4);
        
        // compute range by looking at get method
        
        String getterName = "get" + Character.toUpperCase(featName.charAt(0)) + featName.substring(1);
        Method m;
        try {
          m = jcasClass.getDeclaredMethod(getterName); // get the getter with no args
        } catch (NoSuchMethodException e) {
          /** Cas class {0} with feature {1} but is mssing a 0 argument getter.  This feature will not be used to maybe expand the type's feature set.*/
          UIMAFramework.getLogger(FSClassRegistry.class).log(Level.WARNING, CASRuntimeException.JCAS_MISSING_GETTER, 
              new Object[] {jcasClass.getName(), featName});
          continue;  // skip this one
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
  
//  static boolean isFieldInClass(Feature feat, Class<?> clazz) {
//    try {
//      return null != clazz.getDeclaredField("_FC_" + feat.getShortName());
//    } catch (NoSuchFieldException e) {
//      return false;
//    }    
//  }
  
  static void checkConformance(ClassLoader cl, TypeSystemImpl ts) {
    Map<String, JCasClassInfo> type2jcci = get_className_to_jcci(cl, false);
    checkConformance(ts, ts.topType, type2jcci);
  }
  
  private static void checkConformance(TypeSystemImpl ts, TypeImpl ti, Map<String, JCasClassInfo> type2jcci) {
    if (ti.isPrimitive()) return;
    JCasClassInfo jcci = type2jcci.get(ti.getName());
    
//    if (null == jcci) {
//    if (!skipCheck && ti.isBuiltIn && jcci.isAlreadyCheckedBuiltIn) {
//      skipCheck = true;
//    }
    
    if (null != jcci && // skip if the UIMA class has an abstract (non-creatable) JCas class)
        !(ti.isBuiltIn && jcci.isAlreadyCheckedBuiltIn)) { // skip if builtin and already checked
      checkConformance(jcci.jcasClass, ts, ti, type2jcci);
    }
    
    for (TypeImpl subtype : ti.getDirectSubtypes()) {
      checkConformance(ts, subtype, type2jcci);
    }
  }
  
  /**
   * Checks that a JCas class definition conforms to the current type in the current type system.
   * Checks that the superclass chain contains some match to the super type chain.
   * Checks that the return value for the getters for features matches the feature's range.
   * Checks that static _FC_xxx values from the JCas class == the adjusted feature offsets in the type system
   * 
   * @param clazz - the JCas class to check
   * @param tsi -
   * @param ti -
   */
  private static void checkConformance(Class<?> clazz, TypeSystemImpl tsi, TypeImpl ti, Map<String, JCasClassInfo> type2jcci) {

    // skip the test if the jcasClassInfo is being inherited
    //   because that has already been checked
    if (!clazz.getName().equals(Misc.typeName2ClassName(ti.getName()))) {
      return;
    }
    
    // check supertype
    // one of the supertypes must match a superclass of the class
    //       (both of these should be OK)
    
    //   class:   X  ->  XS -> Annotation -> AnnotationBase -> TOP -> FeatureStructureImplC
    //   type:    X   -------> Annotation -> AnnotationBase -> TOP
    //      (if XS getters/setters used, have runtime error; if never used, OK)
    //
    //   class:   X  --------> Annotation -> AnnotationBase -> TOP -> FeatureStructureImplC
    //   type:    X  ->  XS -> Annotation -> AnnotationBase -> TOP
    boolean isOk = false;
    List<Class<?>> superClasses = new ArrayList<>();
    boolean isCheckImmediateSuper = true;
    Class<?> superClass = clazz.getSuperclass();
    
   outer:
    for (TypeImpl uimaSuperType : ti.getAllSuperTypes()) {        // iterate uimaSuperTypes
      JCasClassInfo jci = type2jcci.get(uimaSuperType.getName());
      if (jci != null) {
        if (isCheckImmediateSuper) {
          if (jci.jcasClass != superClass) {
            /** The JCas class: "{0}" has supertype: "{1}" which doesn''t  match the UIMA type "{2}"''s supertype "{3}". */
            add2errors(errorSet, 
                new CASRuntimeException(CASRuntimeException.JCAS_MISMATCH_SUPERTYPE, 
                    clazz.getCanonicalName(), 
                    clazz.getSuperclass().getCanonicalName(),
                    ti.getName(),
                    ti.getSuperType().getName()),
                false);  // not a throwable error, just a warning   
          }
        }

        superClasses.add(superClass);
        while (superClass != FeatureStructureImplC.class && superClass != Object.class) {
          if (jci.jcasClass == superClass) {
            isOk = true;
            break outer;
          }
          superClass = superClass.getSuperclass();
          superClasses.add(superClass);
        }
      } 
      
      isCheckImmediateSuper = false;
    }
    
    // This error only happens if the JCas type chain doesn't go thru "TOP" - so it isn't really a JCas class!
    
    if (!isOk && superClasses.size() > 0) {
      /** JCas Class's supertypes for "{0}", "{1}" and the corresponding UIMA Supertypes for "{2}", "{3}" don't have an intersection. */
      add2errors(errorSet, 
                 new CASRuntimeException(CASRuntimeException.JCAS_CAS_MISMATCH_SUPERTYPE, 
                     clazz.getName(), Misc.ppList(superClasses), ti.getName(), Misc.ppList(Arrays.asList(ti.getAllSuperTypes()))),
                 true);  // throwable error
    }

    // the range of all the features must match the getters

    for (Method m : clazz.getDeclaredMethods()) {
      
      String mname = m.getName(); 
      if (mname.length() <= 3 || !mname.startsWith("get")) continue;
      String suffix = (mname.length() == 4) ? "" : mname.substring(4);  // one char past 1st letter of feature
      String fname = Character.toLowerCase(mname.charAt(3)) + suffix;   // entire name, with first letter lower cased 
      FeatureImpl fi = ti.getFeatureByBaseName(fname);
      if (fi == null) {                            
        fname = mname.charAt(3) + suffix;      // no feature, but look for one with captialized first letter
        fi = ti.getFeatureByBaseName(fname);
        if (fi == null) continue;
      }
      
      // some users are writing getFeat(some other args) as additional signatures - skip checking these
      // https://issues.apache.org/jira/projects/UIMA/issues/UIMA-5557
      Parameter[] p = m.getParameters();
      TypeImpl range = fi.getRangeImpl();

      if (p.length > 1) continue;  // not a getter, which has either 0 or 1 arg(the index int for arrays)
      if (p.length == 1 &&
          ( ! range.isArray() ||
            p[0].getType() != int.class)) {
        continue;  // has 1 arg, but is not an array or the arg is not an int
      }
      
      // have the feature, check the range
      Class<?> returnClass = m.getReturnType(); // for primitive, is int.class, etc.
      Class<?> rangeClass = range.getJavaClass();
      if (range.isArray()) {
        if (p.length == 1 && p[0].getType() == int.class) {
          rangeClass = range.getComponentType().getJavaClass();
        }
      }
      if (!rangeClass.isAssignableFrom(returnClass)) {   // can return subclass of TOP, OK if range is TOP
        if (rangeClass.getName().equals("org.apache.uima.jcas.cas.Sofa") &&       // exception: for backwards compat reasons, sofaRef returns SofaFS, not Sofa.
            returnClass.getName().equals("org.apache.uima.cas.SofaFS")) {
          // empty
        } else {
          
          /** CAS type system type "{0}" defines field "{1}" with range "{2}", but JCas getter method is returning "{3}" which is not a subtype of the declared range.*/
          add2errors(errorSet, 
                     new CASRuntimeException(CASRuntimeException.JCAS_TYPE_RANGE_MISMATCH, 
                         ti.getName(), fi.getShortName(), rangeClass, returnClass),
                     false);  // should throw, but some code breaks!
        }
      }
    } // end of checking methods
    
    try {
      for (Field f : clazz.getDeclaredFields()) {
        String fname = f.getName();
        if (fname.length() <= 5 || !fname.startsWith("_FC_")) continue;
        String featName = fname.substring(4);
        FeatureImpl fi = ti.getFeatureByBaseName(featName);
        if (fi == null) {
          add2errors(errorSet, 
                     new CASRuntimeException(CASRuntimeException.JCAS_FIELD_MISSING_IN_TYPE_SYSTEM, clazz.getName(), featName), 
                     false);  // don't throw on this error, field is set to -1 and will throw if trying to use it   
        } else {
          Field mhf = clazz.getDeclaredField("_FH_" + featName);
          mhf.setAccessible(true);
          MethodHandle mh = (MethodHandle) mhf.get(null);
          int staticOffsetInClass = (int) mh.invokeExact();
          if (fi.getAdjustedOffset() != staticOffsetInClass) {
             /** In JCAS class "{0}", UIMA field "{1}" was set up when this class was previously loaded and initialized, to have
             * an adjusted offset of "{2}" but now the feature has a different adjusted offset of "{3}"; this may be due to 
             * something else other than type system commit actions loading and initializing the JCas class, or to
             * having a different non-compatible type system for this class, trying to use a common JCas cover class, which is not supported. */
            add2errors(errorSet, 
                       new CASRuntimeException(CASRuntimeException.JCAS_FIELD_ADJ_OFFSET_CHANGED,
                          clazz.getName(), 
                          fi.getName(), 
                          Integer.valueOf(staticOffsetInClass), 
                          Integer.valueOf(fi.getAdjustedOffset())),
                       staticOffsetInClass != -1);  // throw unless static offset is -1, in that case, a runtime error will occur if it is usedd
          }  // end of offset changed
        }  // end of feature check
      } // end of for loop
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
  
  private static void add2errors(ThreadLocal<List<ErrorReport>> errors, Exception e) {
    add2errors(errors, e, true);
  }
  
  private static void add2errors(ThreadLocal<List<ErrorReport>> errors, Exception e, boolean doThrow) {
    List<ErrorReport> es = errors.get();
    if (es == null) {
      es = new ArrayList<ErrorReport>();
      errors.set(es);
    }
    es.add(new ErrorReport(e, doThrow));
  }
  
  private static void reportErrors() {
    boolean throwWhenDone = false;
    List<ErrorReport> es = errorSet.get();
    if (es != null) {
      StringBuilder msg = new StringBuilder(100);
//      msg.append('\n');  // makes a break in the message at the beginning, unneeded
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
  
  /**
   * called infrequently to set up cache
   * Only called when a type system has not had generators for a particular class loader.
   * 
   * For PEAR generators: 
   *   Populates only for those classes the PEAR has overriding implementations
   *     - other entries are null; this serves as a boolean indicator that no pear override exists for that type
   *       and therefore no trampoline is needed
   * 
   * @param cl identifies which set of jcas cover classes
   * @param isPear true for pear case
   * @param tsi the type system being used
   * @return the generators for that set, as an array indexed by type code
   */
  static FsGenerator3[] getGeneratorsForClassLoader(ClassLoader cl, boolean isPear, TypeSystemImpl tsi) {
    Map<String, JCasClassInfo> type2jcci = get_className_to_jcci(cl, isPear);
//    final Map<ClassLoader, Map<String, JCasClassInfo>> cl2t2j = isPear ? cl_4pears_to_type2JCas : cl_to_type2JCas;
//    synchronized(cl2t2j) {
//      //debug
//      System.out.format("debug loading JCas for type System %s ClassLoader %s, isPear: %b%n", 
//                         tsi.hashCode(), cl, isPear);
      // This is the first time this class loader is being used - load the classes for this type system, or
      // This is the first time this class loader is being used with this particular type system

    loadJCasForTSandClassLoader(tsi, true, cl, type2jcci);

    FsGenerator3[] r = new FsGenerator3[tsi.getTypeArraySize()];
                          
//      Map<String, JCasClassInfo> t2jcci = cl2t2j.get(cl);
      // can't use values alone because many types have the same value (due to copy-down)
    for (Entry<String, JCasClassInfo> e : type2jcci.entrySet()) {
      TypeImpl ti = tsi.getType(Misc.javaClassName2UimaTypeName(e.getKey()));
      if (null == ti) {
        continue;  // JCas loaded some type in the past, but it's not in this type system
      }
      JCasClassInfo jcci = e.getValue();
      
      // skip entering a generator in the result if
      //    in a pear setup, and this cl is not the cl that loaded the JCas class.
      //    See method comment for why.
      if (!isPear || jcci.isPearOverride(cl)) {
        r[ti.getCode()] = (FsGenerator3) jcci.generator;
      }      
    }
    return r;
  }
  
  private static boolean isAllNull(FsGenerator3[] r) {
    for (FsGenerator3 v : r) {
      if (v != null)
        return false;
    }
    return true;
  }

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
   * @param clazz -
   * @param type -
   */
  private static void updateAllCallSitesForJCasClass(Class<? extends TOP> clazz, TypeImpl type, ArrayList<MutableCallSite> callSites_toSync) {
    try {
      Field[] fields = clazz.getDeclaredFields();
     
      for (Field field : fields) {
        String fieldName = field.getName();
        if (fieldName.startsWith("_FC_")) {
  
//          //debug
//          System.out.println("debug " + fieldName);
          String featureName = fieldName.substring("_FC_".length());
          final int index = TypeSystemImpl.getAdjustedFeatureOffset(type, featureName);
          if (index == -1) {
            continue;  // a feature defined in the JCas class doesn't exist in the currently loaded type
          }             // skip setting it.  If code uses this, a runtime error will happen.
          
          MutableCallSite c;
          field.setAccessible(true);
          c = (MutableCallSite) field.get(null);
          
          if (c == null) { // happens when first load of TypeSystemImpl is from JCas class ref
            continue;  // will be set later when type system is committed.
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
            throw new UIMA_IllegalStateException(UIMA_IllegalStateException.JCAS_INCOMPATIBLE_TYPE_SYSTEMS,
                new Object[] {type.getName(), featureName});
          }
        }
      }
    } catch (Throwable e) {
      Misc.internalError(e); // never happen
    }
  }
  
  static Map<String, JCasClassInfo> get_className_to_jcci(ClassLoader cl, boolean is_pear) {
    final Map<ClassLoader, Map<String, JCasClassInfo>> cl2t2j = is_pear ? cl_4pears_to_type2JCas : cl_to_type2JCas;
    return cl2t2j.computeIfAbsent(cl, x -> new HashMap<>());
  }
  
  static Lookup getLookup(ClassLoader cl) {
    Lookup lookup = null;
    try {
      Class<?> clazz = Class.forName(UIMAClassLoader.MHLC, true, cl);
      Method m = clazz.getMethod("getMethodHandlesLookup");
      lookup = (Lookup) m.invoke(null);  
    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new UIMARuntimeException(e, UIMARuntimeException.INTERNAL_ERROR);
    }
    return lookup;
  }
}
  