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

package org.apache.uima.jcas.impl;

// * todo:
// *
// * Compatibility removes at some point: TypeSystemInit and it's caller

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.AbstractCas_ImplBase;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.CasOwner;
import org.apache.uima.cas.ConstraintFactory;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeaturePath;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FeatureValuePath;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.SofaID;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSClassRegistry;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.impl.LowLevelException;
import org.apache.uima.cas.impl.LowLevelIndexRepository;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.tcas.Annotation;

// *********************************
// * Implementation of JCas *
// *********************************

/*
 * 
 * Overview --------
 * 
 * This design uses classes for types, not Interfaces. JCas CAS types are represented in a running
 * server by a collection of classes A set of 2 classes for each loaded equivalent-to-CAS type Foo
 * (Class) Foo_Type (Class)
 * 
 * JCas and Foo_Type style classes have one instantiation per CAS View
 * 
 * Integrated with Framework Design -------------------------------- Assume: Framework collects for
 * each CAS creation all the types that will be defined for this CAS. Note: can create multiple
 * instances of CASs (and JCases) each TAE/App.
 * 
 * Assume: Each JCas instance (associated with a CAS) is single-threaded. if not - must synchronize
 * refs to tables/arrays associated with this
 * 
 * Assume: Each CAS instance only has one associated set of types, specified by a String[] of
 * fully-qual-type-names Implication: if CAS type system changes, create new JCas instance. This
 * allows many things to be "final".
 * 
 * Integrate with UIMA Framework Resource Manager for use of class loaders.
 * 
 * Framework will call getJCas once after the CAS types are completely specified, or when a
 * deserialization has re-initialized a CAS.
 * 
 * Initialize: To get to the type object from Foo, use jcas.getType(Foo.type) To get to the JCas
 * type object from instance of Foo, use this.jcasType.
 * 
 * If the CAS has its contents reset, call jcas.clearData() to reset the corresponding JCas content.
 * 
 * Implementation Notes: At loadtime, Foo and Foo_Type classes are assigned static integer indexes,
 * using the JCasRegistry. These indexes are used in arrays in the jcas instance to go from Foo
 * class (not instances) to Foo_Type instances (one per CAS). Things which require "types" at
 * runtime reference the Foo_Type instances.
 * 
 * Maps: Note: per CAS means per shared JCas instance assoc w/ CAS
 * 
 * (universal) (static field in class) go from Foo class to index unique ID used with next map to
 * locate Foo_Type instance associated with this class If universal - becomes an index for all
 * FooStyle classes loaded (per CAS)
 * 
 * (ArrayList) map index from Foo (static) to Foo_Type instance used in creating new instances.
 * Needs to be one per CAS because a particular "xyz" feature in CAS1 != "xyz" feature in CAS2 If
 * above universal, then can become large array with mostly unused slots. Possibility: reuse
 * no-longer-used slots. Identify no-longer-used slots at CAS Type unload event?
 */

/**
 * implements the supporting infrastructure for JCas model linked with a Cas. There is one logical
 * instance of this instantiated per Cas or CasView. If you hold a reference to a CAS, to get a
 * reference to the corresponding JCas, use the method getJCas(). Likewise, if you hold a reference
 * to this object, you can get a reference to the corresponding CAS object using the method
 * getCas().
 * <p>
 * There are variables here that hold references to constant, frequently needed Cas values, such as
 * 0-length FSArrays, 0-length Strings, etc. These can be used to improve efficiency, since the same
 * object can be shared and used for all instances because it is unchangeable. These objects are
 * reset when the CAS is reset, and are initialized lazily, on first use.
 */

public class JCasImpl extends AbstractCas_ImplBase implements AbstractCas, JCas {
  // * FSIndexRepository - the get method returns the java type *
  // * so FSIndexRepository can't be in the implements clause *

  private static class LoadedJCasType {
    final String typeName;

    final int index;

    final boolean isSubtypeOfAnnotationBase;

    final Constructor constructorFor_Type;

    final Constructor constructorForType;

    LoadedJCasType(String typeName, Class _TypeClass, ClassLoader cl) {
      this.typeName = typeName;
      int tempindex = 0;
      boolean tempisSubtypeOfAnnotationBase = false;
      Constructor tempconstructorFor_Type = null;
      Constructor tempconstructorForType = null;

      String name = _TypeClass.getName();
      try {
        Class typeClass = Class.forName(name.substring(0, name.length() - 5), true, cl); // drop
        // _Type

        Field typeIndexField = null;
        try {
          typeIndexField = _TypeClass.getDeclaredField("typeIndexID");
        } catch (NoSuchFieldException e) {
          try {
            // old version has the index in the base type
            typeIndexField = typeClass.getDeclaredField("typeIndexID");
          } catch (NoSuchFieldException e2) {
            logAndThrow(e2);
          }
        }
        tempindex = typeIndexField.getInt(null); // null - static instance var
        tempconstructorFor_Type = _TypeClass.getDeclaredConstructor(jcasBaseAndType);
        tempconstructorForType = typeClass.getDeclaredConstructor(intAnd_Type);
        tempisSubtypeOfAnnotationBase = AnnotationBase.class.isAssignableFrom(typeClass);
      } catch (SecurityException e) {
        logAndThrow(e);
      } catch (NoSuchMethodException e) {
        logAndThrow(e);
      } catch (ClassNotFoundException e) {
        logAndThrow(e);
      } catch (IllegalArgumentException e) {
        logAndThrow(e);
      } catch (IllegalAccessException e) {
        logAndThrow(e);
      } finally {
        index = tempindex;
        constructorFor_Type = tempconstructorFor_Type;
        constructorForType = tempconstructorForType;
        isSubtypeOfAnnotationBase = tempisSubtypeOfAnnotationBase;
      }
    }

    private void logAndThrow(Exception e) {
      CASRuntimeException casEx = new CASRuntimeException(CASRuntimeException.JCAS_CAS_MISMATCH);
      casEx.initCause(e);
      throw casEx;
    }
  }

  // *************************************************
  // * Static Data shared with all instances of JCas *
  // *************************************************
  private final static int INITIAL_HASHMAP_SIZE = 256;

  // key = typeSystemImpl instance, value = Maps:
  //   key = class loader instance, value = Maps:
  //     key = string = fully qualified java type names of _Type classes, 
  //     value = instances of LoadedJCasType:
  //       fully-qualified-name-of-CAS-type
  //       the _Type class for this (specific to class loader)
  //       the class loader instance
  

  // Note: cannot use treemap here because keys (class loaders) are not comparable (have no sort
  // order)
  // To use: first look up with key = typeSystemImpl instance (uses object identity ("==") test)
  // Then use value and look up using class loader (uses object identity ("==") test)
  
  // The first two maps (with keys typeSystemImpl and classLoader) are 
  // Weak maps so that they don't hold onto their keys (class loader or type system) if
  // these are no longer in use
  //
  // This is a static map - effectively indexed by all loaded type systems and all classloaders

  // Access to this must be synch'd
  private static Map<TypeSystemImpl, Map<ClassLoader, Map<String, LoadedJCasType>>> typeSystemToLoadedJCasTypesByClassLoader = 
          new WeakHashMap<TypeSystemImpl, Map<ClassLoader, Map<String, LoadedJCasType>>>(4);

  // **********************************************
  // * Data shared among views of a single CAS *
  // * We keep one copy per view set *
  // **********************************************/
  private static class JCasSharedView {
    // ********************************************************
    // * Data shared among all views in a (J)CAS View group *
    // * Access to this data is assumed to be single threaded *
    // ********************************************************

    /**
     * key = CAS addr, value = corresponding Java instance This impl was changed in May 2007 to a
     * design of one cover object per CAS object, when dealing with multiple views. This implements
     * better semantics for co-located components sharing these objects - they all get to share the
     * same object, independent of which view(s) the object may be indexed in. For cases where the
     * user has augmented the definition of the JCas object to have native Java fields, this design
     * is closer to the user's expectation.
     * 
     * The only use for multiple objects, previously, was to implement the 0-argument version of
     * addToIndexes and removeFromIndexes. The new implementation will come close (but not be
     * perfectly the same) as the old implementation, by doing the following:
     * 
     * Changing the "new" and get/next from iterator methods to set the _Type field of the retrieved
     * cover class object (or new object) to the correct view.
     * 
     * Deref objects follow many paths. They all have a default view they create the object with
     * respect to. A simple deref uses the same cas ref the original object had.
     * 
     */
    private int prevCaddr2JfsSize = INITIAL_HASHMAP_SIZE;

    private JCasHashMap cAddr2Jfs;

    private final Map<ClassLoader, JCasHashMap> cAddr2JfsByClassLoader = new HashMap<ClassLoader, JCasHashMap>();

    /* convenience holders of CAS constants that may be useful */
    /* initialization done lazily - on first call to getter */

    public StringArray stringArray0L = null;

    public IntegerArray integerArray0L = null;

    public FloatArray floatArray0L = null;

    public FSArray fsArray0L = null;

    // * collection of errors that occur during initialization
    public Collection<Exception> errorSet = new ArrayList<Exception>();

    public ClassLoader currentClassLoader = null;

    private JCasSharedView(CASImpl aCAS, boolean useJcasCache) {
      this.cAddr2Jfs = new JCasHashMap(INITIAL_HASHMAP_SIZE, useJcasCache);
      cAddr2JfsByClassLoader.put(aCAS.getJCasClassLoader(), cAddr2Jfs);
      currentClassLoader = aCAS.getJCasClassLoader();
    }
    
  }

  // *******************
  // * Data per (J)CAS *
  // * There may be multiples of these for one base CAS - one per "view"
  // * Access to this data is assumed to be single threaded
  // *******************

  private final JCasSharedView sharedView;

  // not public to protect it from accidents
  private final CASImpl casImpl;

  private final LowLevelIndexRepository ll_IndexRepository;

  private final JFSIndexRepository jfsIndexRepository;
  
  private final boolean isUsedCache;

  /*
   * typeArray is one per CAS because holds pointers to instances of _Type objects, per CAS Not in
   * shared view, because the typeArray points to instances that go with the view. This is used when
   * making instances - it allows each instance to be associated with a view, for purposes of making
   * addtoIndexes() and removeFromIndexes() work. Not final, because it may need to be "extended" if
   * alternate versions of types are loaded from different class loaders, at some point in the
   * execution. The alternate versions are given their own slots in this array.
   */
  private TOP_Type[] typeArray = new TOP_Type[0]; // contents are subtypes of TOP_Type

  // *********************************
  // * Getters for read-only objects *
  // *********************************
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getFSIndexRepository()
   */
  public FSIndexRepository getFSIndexRepository() {
    return casImpl.getIndexRepository();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getLowLevelIndexRepository()
   */
  public LowLevelIndexRepository getLowLevelIndexRepository() {
    return ll_IndexRepository;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getCas()
   */
  public CAS getCas() {
    return casImpl;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getCasImpl()
   */
  public CASImpl getCasImpl() {
    return casImpl;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getLowLevelCas()
   */
  public LowLevelCAS getLowLevelCas() {
    return casImpl;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getTypeSystem()
   */
  public TypeSystem getTypeSystem() {
    return casImpl.getTypeSystem();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getType(int)
   */
  public TOP_Type getType(int i) {
    if (i >= typeArray.length || null == typeArray[i]) {
      // unknown ID. This may be due to a need to update the typeArray
      // due to switching class loaders. This updating is done
      // partially lazily - when needed, beyond the particular CAS instance and
      // view that was being passed to a process method when the
      // class loader switch was done.

      // In order for this to work, all access to the typeArray must be
      // via this getter.

      // Try instantiating the entries in typeArray for this class loader
      instantiateJCas_Types(this.sharedView.currentClassLoader);
      if (i >= typeArray.length || null == typeArray[i]) {

        // unknown ID. Attempt to get offending class.
        Class cls = JCasRegistry.getClassForIndex(i);
        if (cls != null) {
          String typeName = cls.getName();
          // is type in type system
          if (this.casImpl.getTypeSystem().getType(typeName) == null) {
            // no - report error that JCAS type was not defined in XML
            // descriptor
            CASRuntimeException casEx = new CASRuntimeException(
                CASRuntimeException.JCAS_TYPE_NOT_IN_CAS, new String[] { typeName });
            throw casEx;
          } else {
            // yes - there was some problem loading the _Type object
            CASRuntimeException casEx = new CASRuntimeException(
                CASRuntimeException.JCAS_MISSING_COVERCLASS, new String[] { typeName + "_Type" });
            throw casEx;
          }

        } else {
          throw new CASRuntimeException(CASRuntimeException.JCAS_UNKNOWN_TYPE_NOT_IN_CAS);
        }
      }
    }
    return typeArray[i];
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getCasType(int)
   */
  public Type getCasType(int i) {
    return getType(i).casType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getType(org.apache.uima.jcas.cas.TOP)
   */
  public TOP_Type getType(TOP instance) {
    return getType(instance.getTypeIndexID());
  }

  /** throws (an unchecked) CASRuntimeException */
  private void logAndThrow(Exception e) {
    CASRuntimeException casEx = new CASRuntimeException(CASRuntimeException.JCAS_CAS_MISMATCH);
    casEx.initCause(e);
    throw casEx;
  }

  // never called, but have to set values to null because they're final
  private JCasImpl() {
    sharedView = null;
    casImpl = null;
    ll_IndexRepository = null;
    isUsedCache = false;
    throw new RuntimeException("JCas constructor with no args called, should never be called.");
  }

  /*
   * Private constructor, called when new instance (either for new cas, or for old cas but new
   * Annotator/Application class, needed
   * 
   * Called by JCas.getJCas(cas)
   * 
   * The CAS must be initialized when this is called.
   * 
   */
  private JCasImpl(CASImpl cas) throws CASException {

    // * A new instance of JCas exists for each CAS
    // * At this point, some but not necessarily all of the Types have been
    // loaded

    // * the typeArray needs to be big enough to hold all the types
    // * that will be loaded.

    this.casImpl = cas;
    this.isUsedCache = cas.doUseJcasCache();
    if (casImpl != casImpl.getBaseCAS()) {
      sharedView = ((JCasImpl) casImpl.getBaseCAS().getJCas()).sharedView;
      sharedView.errorSet.clear();
    } else {
      sharedView = new JCasSharedView(cas, this.isUsedCache);
    }

    this.ll_IndexRepository = casImpl.ll_getIndexRepository();
    this.jfsIndexRepository = new JFSIndexRepositoryImpl(this, cas.getIndexRepository());

    // * acquire the lock for this thread that is the same lock
    // * used in the getNextIndexIncr operation. This will block
    // * any other (meaning on another thread)
    // * loading of the JCas Type classes until this thread's loading
    // * completes.

    synchronized (JCasImpl.class) {
      ClassLoader cl = cas.getJCasClassLoader();
      instantiateJCas_Types(cl);
    } // end of synchronized block
  }

  /**
   * This is only called when the JCas is already set up
   * 
   * @param cl
   *                class loader to switch to
   * @throws CASException
   */
  public void switchClassLoader(ClassLoader cl) {

    // first try a fast switch - works if we've switched before to this class loader
    if (!casImpl.getFSClassRegistry().swapInGeneratorsForClassLoader(cl, casImpl)) {
      instantiateJCas_Types(cl);
    }
    final JCasSharedView sv = this.sharedView;
    sv.cAddr2Jfs = (JCasHashMap) sv.cAddr2JfsByClassLoader.get(cl);
    if (null == sv.cAddr2Jfs) {
      sv.cAddr2Jfs = new JCasHashMap(INITIAL_HASHMAP_SIZE, this.isUsedCache);
      sv.cAddr2JfsByClassLoader.put(cl, sv.cAddr2Jfs);
    }
    sv.currentClassLoader = cl;
  }

  /**
   * There may be several type systems with different defined types loaded and operating at the same
   * time (in different CASes) There is an instance of the loaded JCas Classes for each Type System
   * for each class loader.
   * 
   * @param cl
   * @return
   */
  private synchronized Map<String, LoadedJCasType> loadJCasClasses(ClassLoader cl) {
    final TypeSystem ts = casImpl.getTypeSystem();
    Iterator<Type> typeIt = ts.getTypeIterator();
    TypeImpl t;
    String casName;
    Map<String, LoadedJCasType> jcasTypes = new HashMap<String, LoadedJCasType>();

    // * note that many of these may have already been loaded
    // * load all the others. Actually, we ask to load all the types
    // * and the ones already loaded - we just get their loaded versions
    // returned.
    // * Loading will run the static init functions.

    while (typeIt.hasNext()) {
      t = (TypeImpl) typeIt.next();
      casName = t.getName();
      String name_Type;
      if (builtInsWithNoJCas.contains(casName))
        continue;
      if (builtInsWithAltNames.contains(casName))
        // * convert uima.cas.Xxx -> org.apache.uima.jcas.cas.Xxx
        // * convert uima.tcas.Annotator ->
        // org.apache.uima.jcas.tcas.Annotator
        try {
          String nameBase = "org.apache.uima.jcas." + casName.substring(5);
          name_Type = nameBase + "_Type";
          jcasTypes.put(nameBase, new LoadedJCasType(t.getName(), Class
              .forName(name_Type, true, cl), cl));
        } catch (ClassNotFoundException e1) {
          // OK for DocumentAnnotation, which may not have a cover class.
          // Otherwise, not OK.
          if (!CAS.TYPE_NAME_DOCUMENT_ANNOTATION.equals(casName)) {
            assert false : "never get here because built-ins have java cover types";
            e1.printStackTrace();
          }
        }

      // this is done unconditionally to pick up old style cover functions if
      // any
      // as well as other JCas model types
      try {
        name_Type = casName + "_Type";
        jcasTypes.put(casName, new LoadedJCasType(t.getName(), Class.forName(name_Type, true, cl),
            cl));
        // also force the load the plain name without _Type for
        // old-style - that's where
        // the index is incremented
        Class.forName(casName, true, cl);
      } catch (ClassNotFoundException e1) {
        // many classes may not have JCas models, so this is not an
        // error
      }
    }

    // note: this entire method is synchronized
    Map<ClassLoader, Map<String, LoadedJCasType>> classLoaderToLoadedJCasTypes = typeSystemToLoadedJCasTypesByClassLoader.get(casImpl
        .getTypeSystemImpl());
    if (null == classLoaderToLoadedJCasTypes) {
      classLoaderToLoadedJCasTypes = new WeakHashMap<ClassLoader, Map<String, LoadedJCasType>>(4);
      typeSystemToLoadedJCasTypesByClassLoader.put(casImpl.getTypeSystemImpl(),
          classLoaderToLoadedJCasTypes);
    }
    classLoaderToLoadedJCasTypes.put(cl, jcasTypes);

    expandTypeArrayIfNeeded();
    return jcasTypes;
  }

  // note: callers are not synchronized because the typeArray has a
  // separate instance per CAS (actually per CAS view)
  private void expandTypeArrayIfNeeded() {
    if (typeArray.length < JCasRegistry.getNumberOfRegisteredClasses()) {
      TOP_Type[] newTypeArray = new TOP_Type[JCasRegistry.getNumberOfRegisteredClasses()];
      System.arraycopy(typeArray, 0, newTypeArray, 0, typeArray.length);
      typeArray = newTypeArray;
    }
  }

  public void instantiateJCas_Types(ClassLoader cl) {
    Map<String, LoadedJCasType> loadedJCasTypes = null;
    FSClassRegistry fscr = casImpl.getFSClassRegistry();
    boolean alreadyLoaded;  // means the "classes" have been loaded, but doesn't mean
                            // the _Type instances of those classes have been created.
    boolean anyNewInstances = false;  // true if any new instances of _Type are generated
    FSGenerator[] newFSGeneratorSet;
    synchronized (JCasImpl.class) {
      Map<ClassLoader, Map<String, LoadedJCasType>> classLoaderToLoadedJCasTypes = typeSystemToLoadedJCasTypesByClassLoader.get(casImpl
          .getTypeSystemImpl());
      if (null != classLoaderToLoadedJCasTypes) {
        loadedJCasTypes = classLoaderToLoadedJCasTypes.get(cl);
      }
      alreadyLoaded = (null != loadedJCasTypes);
      if (!alreadyLoaded) {
        loadedJCasTypes = loadJCasClasses(cl);
      }

      expandTypeArrayIfNeeded();
      // if already loaded, can skip making new generators - 
      //   in this case newFSGeneratorSet is never referenced
      //   Set it to null for "safety"
      newFSGeneratorSet = alreadyLoaded ? null : fscr.getNewFSGeneratorSet();
      for (Iterator<Map.Entry<String, LoadedJCasType>> it = loadedJCasTypes.entrySet().iterator(); it.hasNext();) {
        
        // Explanation for this logic:
        //   Instances of _Types are kept per class loader, per Cas (e.g., in the cas pool)
        //   When switching class loaders (e.g. a pear in the pipeline), some of the 
        //     _Type instances (e.g. the built-ins) might be already instantiated, but others are not
        //   
        boolean madeNewInstance = makeInstanceOf_Type(it.next().getValue(), alreadyLoaded,
            newFSGeneratorSet); 
        anyNewInstances = madeNewInstance || anyNewInstances;
      }
      
      // speed up - skip rest if nothing to do
      if (!anyNewInstances) {
        return;
      }
      if (!alreadyLoaded) {
        copyDownSuperGenerators(loadedJCasTypes, newFSGeneratorSet);
        if (casImpl.usingBaseClassLoader()) {
          fscr.setBaseGenerators(newFSGeneratorSet); // should be under sync lock
        }
        fscr.saveGeneratorsForClassLoader(cl, newFSGeneratorSet);
      }
    }
    if (alreadyLoaded) {
      fscr.swapInGeneratorsForClassLoader(cl, casImpl);
    } else {
      casImpl.setLocalFsGenerators(newFSGeneratorSet);
    }
  }

  // note all callers are synchronized
  private void copyDownSuperGenerators(Map<String, LoadedJCasType> jcasTypes, FSGenerator[] fsGenerators) {
    final TypeSystem ts = casImpl.getTypeSystem();
    Iterator<Type> typeIt = ts.getTypeIterator(); // reset iterator to start
    Type topType = ts.getTopType();
    Type superType = null;
    while (typeIt.hasNext()) {
      Type t = (Type) typeIt.next();
      if (builtInsWithNoJCas.contains(t.getName()))
        continue;
      // comment here
      if (CAS.TYPE_NAME_DOCUMENT_ANNOTATION.equals(t.getName())) {
        if (jcasTypes.get("org.apache.uima.jcas.tcas.DocumentAnnotation") != null)
          continue;
      } else if (builtInsWithAltNames.contains(t.getName()))
        continue; // because jcasTypes has no entry for these
      if (null != jcasTypes.get(t.getName()))
        continue;
      // we believe that at this point, t is not "top", because top is
      // always loaded
      // find closest supertype that has a loaded cover class
      superType = t;
      String superTypeName;
      do {
        superType = ts.getParent(superType);
        superTypeName = superType.getName();
        if (builtInsWithAltNames.contains(superTypeName)) {
          superTypeName = "org.apache.uima.jcas." + superTypeName.substring(5);
        }
      } while ((null == jcasTypes.get(superTypeName) && !superType.equals(topType)));
      // copy down its generator
      fsGenerators[((TypeImpl) t).getCode()] = fsGenerators[((TypeImpl) superType).getCode()];
    }
  }

  /**
   * creates a new JCas instance that corresponds to a CAS instance. Will be called once by the UIMA
   * framework when it creates the CAS.
   * 
   * @param cas
   *                a CAS instance
   * @return newly created and initialized JCas
   */
  public static JCas getJCas(CASImpl cas) throws CASException {
    JCasImpl jcas = new JCasImpl(cas);
    JCasSharedView sv = jcas.sharedView;
    if (sv.errorSet.size() > 0) {
      StringBuffer msg = new StringBuffer(100);
      for (Exception f : sv.errorSet) {
        msg.append(f.getMessage());
        msg.append("\n");
      }
      CASException e = new CASException(CASException.JCAS_INIT_ERROR,
          new String[] { msg.toString() });
      throw e;
    }
    return jcas;
  }

  /**
   * built-in types which have alternate names It really isn't necessary to skip these - they're
   * never instantiated. But it is a very slight performance boost, and it may be safer given
   * possible future changes to these types' implementations.
   */
  private static final Collection<String> builtInsWithNoJCas = new ArrayList<String>();
  static {
    builtInsWithNoJCas.add(CAS.TYPE_NAME_BOOLEAN);
    builtInsWithNoJCas.add(CAS.TYPE_NAME_BYTE);
    builtInsWithNoJCas.add(CAS.TYPE_NAME_SHORT);
    builtInsWithNoJCas.add(CAS.TYPE_NAME_INTEGER);
    builtInsWithNoJCas.add(CAS.TYPE_NAME_LONG);
    builtInsWithNoJCas.add(CAS.TYPE_NAME_FLOAT);
    builtInsWithNoJCas.add(CAS.TYPE_NAME_DOUBLE);
    builtInsWithNoJCas.add(CAS.TYPE_NAME_STRING);
    builtInsWithNoJCas.add(CAS.TYPE_NAME_ARRAY_BASE);
    builtInsWithNoJCas.add(CAS.TYPE_NAME_LIST_BASE);
  }

  private static final Collection<String> builtInsWithAltNames = new ArrayList<String>();
  static { // initialization code
    builtInsWithAltNames.add(CAS.TYPE_NAME_TOP);
    builtInsWithAltNames.add(CAS.TYPE_NAME_STRING_ARRAY);
    builtInsWithAltNames.add(CAS.TYPE_NAME_BOOLEAN_ARRAY);
    builtInsWithAltNames.add(CAS.TYPE_NAME_BYTE_ARRAY);
    builtInsWithAltNames.add(CAS.TYPE_NAME_SHORT_ARRAY);
    builtInsWithAltNames.add(CAS.TYPE_NAME_INTEGER_ARRAY);
    builtInsWithAltNames.add(CAS.TYPE_NAME_LONG_ARRAY);
    builtInsWithAltNames.add(CAS.TYPE_NAME_FS_ARRAY);
    builtInsWithAltNames.add(CAS.TYPE_NAME_FLOAT_ARRAY);
    builtInsWithAltNames.add(CAS.TYPE_NAME_DOUBLE_ARRAY);
    builtInsWithAltNames.add(CAS.TYPE_NAME_EMPTY_FLOAT_LIST);
    builtInsWithAltNames.add(CAS.TYPE_NAME_EMPTY_FS_LIST);
    builtInsWithAltNames.add(CAS.TYPE_NAME_EMPTY_INTEGER_LIST);
    builtInsWithAltNames.add(CAS.TYPE_NAME_EMPTY_STRING_LIST);
    builtInsWithAltNames.add(CAS.TYPE_NAME_FLOAT_LIST);
    builtInsWithAltNames.add(CAS.TYPE_NAME_FS_LIST);
    builtInsWithAltNames.add(CAS.TYPE_NAME_INTEGER_LIST);
    builtInsWithAltNames.add(CAS.TYPE_NAME_STRING_LIST);
    builtInsWithAltNames.add(CAS.TYPE_NAME_NON_EMPTY_FLOAT_LIST);
    builtInsWithAltNames.add(CAS.TYPE_NAME_NON_EMPTY_FS_LIST);
    builtInsWithAltNames.add(CAS.TYPE_NAME_NON_EMPTY_INTEGER_LIST);
    builtInsWithAltNames.add(CAS.TYPE_NAME_NON_EMPTY_STRING_LIST);
    builtInsWithAltNames.add(CAS.TYPE_NAME_SOFA);
    builtInsWithAltNames.add(CAS.TYPE_NAME_ANNOTATION_BASE);
    builtInsWithAltNames.add(CAS.TYPE_NAME_ANNOTATION);
    builtInsWithAltNames.add(CAS.TYPE_NAME_DOCUMENT_ANNOTATION);
  }

  // This generator class is used in place of the generator that is in each of the
  // older JCasGen'd classes

  // It makes use of the passed-in CAS view so one generator works for all Cas Views.
  // It references the xxx_Type instance using the getType call, which will
  // (lazily) instantiate the xxx_Type object if needed (due to switching class loaders:
  // see comment under getType(int))

  static private class JCasFsGenerator implements FSGenerator {
    private final int type;

    private final Constructor c;

    private final boolean isSubtypeOfAnnotationBase;

    private final int sofaNbrFeatCode;

    private final int annotSofaFeatCode;

    JCasFsGenerator(int type, Constructor c, boolean isSubtypeOfAnnotationBase,
        int sofaNbrFeatCode, int annotSofaFeatCode) {
      this.type = type;
      this.c = c;
      this.isSubtypeOfAnnotationBase = isSubtypeOfAnnotationBase;
      this.sofaNbrFeatCode = sofaNbrFeatCode;
      this.annotSofaFeatCode = annotSofaFeatCode;
    }

    // Called in 3 cases
    // 1) a non-JCas call to create a new FS
    // 2) a dereference of an existing FS
    // 3) an iterator
    public FeatureStructure createFS(int addr, CASImpl casView) {
      Object[] initargs = new Object[2];
      JCasImpl jcasView = null;
      // this funny logic is because although the annotationView should always be set if
      // a type is a subtype of annotation, it isn't always set if an application uses low-level
      // api's. Rather than blow up, we limp along.
      CASImpl maybeAnnotationView = null;
      if (isSubtypeOfAnnotationBase) {
        final int sofaNbr = getSofaNbr(addr, casView);
        if (sofaNbr > 0) {
          maybeAnnotationView = (CASImpl) casView.getView(sofaNbr);
        }
      }
      final CASImpl view = (null != maybeAnnotationView) ? maybeAnnotationView : casView;

      try {
        jcasView = (JCasImpl) view.getJCas();
      } catch (CASException e1) {
        logAndThrow(e1, jcasView);
      }

      // Return eq fs instance if already created
      TOP fs = jcasView.getJfsFromCaddr(addr);
      if (null != fs) {
        fs.jcasType = jcasView.getType(type);
      } else {
        initargs[0] = Integer.valueOf(addr);
        initargs[1] = jcasView.getType(type);
        try {
          fs = (TOP) c.newInstance(initargs);
        } catch (IllegalArgumentException e) {
          logAndThrow(e, jcasView);
        } catch (InstantiationException e) {
          logAndThrow(e, jcasView);
        } catch (IllegalAccessException e) {
          logAndThrow(e, jcasView);
        } catch (InvocationTargetException e) {
          logAndThrow(e, jcasView);
        }
        jcasView.putJfsFromCaddr(addr, fs);
      }
      return fs;
    }

    private void logAndThrow(Exception e, JCasImpl jcasView) {
      CASRuntimeException casEx = new CASRuntimeException(
          CASRuntimeException.JCAS_CAS_MISMATCH,
          new String[] { (null == jcasView) ? "-- ignore outer msg, error is can''t get value of jcas from cas"
              : (jcasView.getType(type).casType.getName() + "; exception= "
                  + e.getClass().getName() + "; msg= " + e.getLocalizedMessage()) });
      casEx.initCause(e);
      throw casEx;
    }

    private int getSofaNbr(int addr, CASImpl casView) {
      final LowLevelCAS llCas = casView.getLowLevelCAS();
      final int sofa = llCas.ll_getIntValue(addr, annotSofaFeatCode, false);
      return (sofa == 0) ? 0 : llCas.ll_getIntValue(sofa, sofaNbrFeatCode);
    }
  }

  // per JCas instance - so don't need to synch.
  private final Object[] constructorArgsFor_Type = new Object[2];

  /**
   * Make the instance of the JCas xxx_Type class for this CAS. Note: not all types will have
   * xxx_Type. Instance creation does the typeSystemInit kind of function, as well.
   * 
   * returns true if a new instance of a _Type class was created
   */

  private boolean makeInstanceOf_Type(LoadedJCasType jcasTypeInfo, boolean alreadyLoaded,
      FSGenerator[] fsGenerators) {
    
    // return without doing anything if the _Type instance is already existing
    //   this happens when a JCas has some _Type instances made (e.g, the
    //     built-in ones) but the class loader was switched.  Some of the
    //     _Type instances for the new class loader can share previously 
    //     instantiated _Type instances, but others may be different
    //     (due to different impls of the _Type class loaded by the different
    //     class loader).
    //   This can also happen in the case where
    //   JCasImpl.getType is called for a non-existing class
    //     What happens in this case is that the getType code has to assume that
    //     perhaps none of the _Type instances were made for this JCas (yet), because
    //     these are created lazily - so it calls instantiateJCas_Types to make them.
    //     If they were already made, this next test short circuits this.
    int typeIndex = jcasTypeInfo.index;
    if (typeArray[typeIndex] != null) {
      return false;
    }
    
    Constructor c_Type = jcasTypeInfo.constructorFor_Type;
    Constructor cType = jcasTypeInfo.constructorForType;
    TypeImpl casType = (TypeImpl) casImpl.getTypeSystem().getType(jcasTypeInfo.typeName);

    try {
      constructorArgsFor_Type[0] = this;
      constructorArgsFor_Type[1] = casType;
      TOP_Type x_Type_instance = (TOP_Type) c_Type.newInstance(constructorArgsFor_Type);
      typeArray[typeIndex] = x_Type_instance;
      // install the standard generator
      // this is sharable by all views, since the CAS is passed to the generator
      // Also sharable by all in a CasPool, except for "swapping" due to PEARs/Classloaders.
      if (!alreadyLoaded) {
        final TypeSystemImpl ts = casImpl.getTypeSystemImpl();
        fsGenerators[casType.getCode()] = new JCasFsGenerator(typeIndex, cType,
            jcasTypeInfo.isSubtypeOfAnnotationBase, ts.sofaNumFeatCode, ts.annotSofaFeatCode);
        // this.casImpl.getFSClassRegistry().loadJCasGeneratorForType(typeIndex, cType, casType,
        // jcasTypeInfo.isSubtypeOfAnnotationBase);
      }
    } catch (SecurityException e) {
      logAndThrow(e);
    } catch (InstantiationException e) {
      logAndThrow(e);
    } catch (IllegalAccessException e) {
      logAndThrow(e);
    } catch (InvocationTargetException e) {
      logAndThrow(e);
    } catch (ArrayIndexOutOfBoundsException e) {
      logAndThrow(e);
    }
    return true;
  }

  /**
   * Make the instance of the JCas xxx_Type class for this CAS. Note: not all types will have
   * xxx_Type. Instance creation does the typeSystemInit kind of function, as well.
   */
  /*
   * private void makeInstanceOf_Type(Type casType, Class clas, CASImpl cas) { Constructor c; Field
   * typeIndexField = null; int typeIndex; try { c = clas.getDeclaredConstructor(jcasBaseAndType);
   * try {
   * 
   * typeIndexField = clas.getDeclaredField("typeIndexID"); } catch (NoSuchFieldException e) { try { //
   * old version has the index in the base type String name = clas.getName(); Class clas2 =
   * Class.forName(name.substring(0, name.length() - 5), true, cas .getJCasClassLoader()); // drop
   * _Type typeIndexField = clas2.getDeclaredField("typeIndexID"); } catch (NoSuchFieldException e2) {
   * logAndThrow(e2); } catch (ClassNotFoundException e3) { logAndThrow(e3); } } typeIndex =
   * typeIndexField.getInt(null); // null - static instance var TOP_Type x_Type_instance =
   * (TOP_Type) c.newInstance(new Object[] { this, casType }); typeArray[typeIndex] =
   * x_Type_instance; } catch (SecurityException e) { logAndThrow(e); } catch (NoSuchMethodException
   * e) { logAndThrow(e); } catch (InstantiationException e) { logAndThrow(e); } catch
   * (IllegalAccessException e) { logAndThrow(e); } catch (InvocationTargetException e) {
   * logAndThrow(e); } catch (ArrayIndexOutOfBoundsException e) { logAndThrow(e); } }
   */

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getRequiredType(java.lang.String)
   */
  public Type getRequiredType(String s) throws CASException {
    Type t = getTypeSystem().getType(s);
    if (null == t) {
      CASException casEx = new CASException(CASException.JCAS_TYPENOTFOUND_ERROR,
          new String[] { s });
      throw casEx;
    }
    return t;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getRequiredFeature(org.apache.uima.cas.Type, java.lang.String)
   */
  public Feature getRequiredFeature(Type t, String s) throws CASException {
    Feature f = t.getFeatureByBaseName(s);
    if (null == f) {
      CASException casEx = new CASException(CASException.JCAS_FEATURENOTFOUND_ERROR, new String[] {
          t.getName(), s });
      throw casEx;
    }
    return f;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getRequiredFeatureDE(org.apache.uima.cas.Type, java.lang.String,
   *      java.lang.String, boolean)
   */

  public Feature getRequiredFeatureDE(Type t, String s, String rangeName, boolean featOkTst) {
    Feature f = t.getFeatureByBaseName(s);
    Type rangeType = this.getTypeSystem().getType(rangeName);
    if (null == f && !featOkTst) {
      CASException casEx = new CASException(CASException.JCAS_FEATURENOTFOUND_ERROR, new String[] {
          t.getName(), s });
      sharedView.errorSet.add(casEx);
    }
    if (null != f)
      try {
        casImpl.checkTypingConditions(t, rangeType, f);
      } catch (LowLevelException e) {
        CASException casEx = new CASException(CASException.JCAS_FEATURE_WRONG_TYPE, new String[] {
            t.getName(), s, rangeName, f.getRange().toString() });
        sharedView.errorSet.add(casEx);
      }
    return f;
  }

  /**
   * Internal - throw missing feature exception at runtime
   * 
   * @param feat
   * @param type
   */
  public void throwFeatMissing(String feat, String type) {
    CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_FEAT,
        new String[] { feat, type });
    throw e;
  }

  // constant used in the following function
  /** internal use - constant used in getting constructors */
  final static private Class[] jcasBaseAndType = new Class[] { JCas.class, Type.class };

  final static private Class[] intAnd_Type = new Class[] { int.class, TOP_Type.class };

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#putJfsFromCaddr(int, org.apache.uima.cas.FeatureStructure)
   */
  public void putJfsFromCaddr(int casAddr, FeatureStructure fs) {
    sharedView.cAddr2Jfs.put((FeatureStructureImpl) fs);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getJfsFromCaddr(int)
   */
  public TOP getJfsFromCaddr(int casAddr) {
    return (TOP) sharedView.cAddr2Jfs.get(casAddr);
  }

  public void showJfsFromCaddrHistogram() {
    sharedView.cAddr2Jfs.showHistogram();
  }

  // * Implementation of part of the Cas interface as part of JCas*

  /**
   * (Internal Use only) called by the CAS reset function - clears the hashtable holding the
   * associations.
   */
  public static void clearData(CAS cas) {
    JCasImpl jcas = (JCasImpl) ((CASImpl) cas).getExistingJCas();
    final JCasSharedView sv = jcas.sharedView;
    for (Iterator<Map.Entry<ClassLoader, JCasHashMap>> it = sv.cAddr2JfsByClassLoader.entrySet().iterator(); it.hasNext();) {
      Map.Entry<ClassLoader, JCasHashMap> e = it.next();
      sv.cAddr2Jfs = e.getValue();
      int hashSize = Math.max(sv.cAddr2Jfs.size(), 32); // not worth dropping
      // below 32
      // System.out.println("\n***JCas Resizing Hashtable: size is: " +
      // hashSize + ", curmax = " +
      // jcas.prevCaddr2JfsSize);
      if (hashSize <= (sv.prevCaddr2JfsSize >> 1)) {
        // System.out.println("JCas Shrinking Hashtable from " +
        // jcas.prevCaddr2JfsSize);
        sv.prevCaddr2JfsSize = hashSize;
        sv.cAddr2Jfs = new JCasHashMap(hashSize, jcas.isUsedCache);
        sv.cAddr2JfsByClassLoader.put(e.getKey(), sv.cAddr2Jfs);
      } else {
        sv.prevCaddr2JfsSize = Math.max(hashSize, sv.prevCaddr2JfsSize);
        // System.out.println("JCas clearing - keeping same size, new max prev
        // size = " +
        // jcas.prevCaddr2JfsSize);
        sv.cAddr2Jfs.clear();
      }
      sv.stringArray0L = null;
      sv.floatArray0L = null;
      sv.fsArray0L = null;
      sv.integerArray0L = null;
    }
    sv.cAddr2Jfs = (JCasHashMap) sv.cAddr2JfsByClassLoader
        .get(((CASImpl) cas).getJCasClassLoader());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#reset()
   */
  public void reset() {
    casImpl.reset();
  }

  private final static int NULL = 0;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#checkArrayBounds(int, int)
   */
  public final void checkArrayBounds(int fsRef, int pos) {
    if (NULL == fsRef) {
      LowLevelException e = new LowLevelException(LowLevelException.NULL_ARRAY_ACCESS);
      // note - need to add this to ll_runtimeException
      e.addArgument(Integer.toString(pos));
      throw e;
    }
    final int arrayLength = casImpl.ll_getArraySize(fsRef);
    if (pos < 0 || pos >= arrayLength) {
      LowLevelException e = new LowLevelException(LowLevelException.ARRAY_INDEX_OUT_OF_RANGE);
      e.addArgument(Integer.toString(pos));
      throw e;
    }
  }

  // *****************
  // * Sofa support *
  // *****************

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getSofa(org.apache.uima.cas.SofaID)
   */
  public Sofa getSofa(SofaID sofaID) {
    return (Sofa) casImpl.getSofa(sofaID);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getSofa()
   */
  public Sofa getSofa() {
    return (Sofa) casImpl.getSofa();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#createView(java.lang.String)
   */
  public JCas createView(String sofaID) throws CASException {
    return casImpl.createView(sofaID).getJCas();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getJCas(org.apache.uima.jcas.cas.Sofa)
   */
  public JCas getJCas(Sofa sofa) throws CASException {
    return casImpl.getView(sofa).getJCas();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getSofaIterator()
   */
  public FSIterator<SofaFS> getSofaIterator() {
    return casImpl.getSofaIterator();
  }

  // *****************
  // * Index support *
  // *****************

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getJFSIndexRepository()
   */
  public JFSIndexRepository getJFSIndexRepository() {
    return jfsIndexRepository;
  }

  // ****************
  // * TCas support *
  // ****************

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getDocumentAnnotationFs()
   */
  public TOP getDocumentAnnotationFs() {
    return (TOP) casImpl.getDocumentAnnotation();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getDocumentText()
   */
  public String getDocumentText() {
    return casImpl.getDocumentText();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getSofaDataString()
   */
  public String getSofaDataString() {
    return casImpl.getSofaDataString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getSofaDataArray()
   */
  public FeatureStructure getSofaDataArray() {
    return casImpl.getSofaDataArray();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getSofaDataURI()
   */
  public String getSofaDataURI() {
    return casImpl.getSofaDataURI();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getSofaMimeType()
   */
  public String getSofaMimeType() {
    return casImpl.getSofaMimeType();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#setDocumentText(java.lang.String)
   */
  public void setDocumentText(String text) throws CASRuntimeException {
    casImpl.setDocumentText(text);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#setSofaDataString(java.lang.String, java.lang.String)
   */
  public void setSofaDataString(String text, String mime) throws CASRuntimeException {
    casImpl.setSofaDataString(text, mime);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#setSofaDataArray(org.apache.uima.jcas.cas.TOP, java.lang.String)
   */
  public void setSofaDataArray(FeatureStructure array, String mime) throws CASRuntimeException {
    casImpl.setSofaDataArray(array, mime);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#setSofaDataURI(java.lang.String, java.lang.String)
   */
  public void setSofaDataURI(String uri, String mime) throws CASRuntimeException {
    casImpl.setSofaDataURI(uri, mime);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getDocumentLanguage()
   */
  public String getDocumentLanguage() {
    return casImpl.getDocumentLanguage();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#setDocumentLanguage(java.lang.String)
   */
  public void setDocumentLanguage(String language) throws CASRuntimeException {
    casImpl.setDocumentLanguage(language);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getSofaDataStream()
   */
  public InputStream getSofaDataStream() {
    return casImpl.getSofaDataStream();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getConstraintFactory()
   */
  public ConstraintFactory getConstraintFactory() {
    return casImpl.getConstraintFactory();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#createFeaturePath()
   */
  public FeaturePath createFeaturePath() {
    return casImpl.createFeaturePath();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#createFilteredIterator(org.apache.uima.cas.FSIterator,
   *      org.apache.uima.cas.FSMatchConstraint)
   */
  public <T extends FeatureStructure> FSIterator<T> createFilteredIterator(FSIterator<T> it, FSMatchConstraint constraint) {
    return casImpl.createFilteredIterator(it, constraint);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getStringArray0L()
   */

  public StringArray getStringArray0L() {
    if (null == sharedView.stringArray0L)
      sharedView.stringArray0L = new StringArray(this, 0);
    return sharedView.stringArray0L;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getIntegerArray0L()
   */
  public IntegerArray getIntegerArray0L() {
    if (null == sharedView.integerArray0L)
      sharedView.integerArray0L = new IntegerArray(this, 0);
    return sharedView.integerArray0L;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getFloatArray0L()
   */
  public FloatArray getFloatArray0L() {
    if (null == sharedView.floatArray0L)
      sharedView.floatArray0L = new FloatArray(this, 0);
    return sharedView.floatArray0L;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getFSArray0L()
   */
  public FSArray getFSArray0L() {
    if (null == sharedView.fsArray0L)
      sharedView.fsArray0L = new FSArray(this, 0);
    return sharedView.fsArray0L;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#processInit()
   */
  public void processInit() {
    // unused
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.AbstractCas_ImplBase#setOwn
   */
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#setOwner(org.apache.uima.cas.CasOwner)
   */
  public void setOwner(CasOwner aCasOwner) {
    casImpl.setOwner(aCasOwner);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#release()
   */
  public void release() {
    casImpl.release();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getView(java.lang.String)
   */
  public JCas getView(String localViewName) throws CASException {
    return casImpl.getView(localViewName).getJCas();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getView(org.apache.uima.cas.SofaFS)
   */
  public JCas getView(SofaFS aSofa) throws CASException {
    return casImpl.getView(aSofa).getJCas();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#addFsToIndexes(org.apache.uima.cas.FeatureStructure)
   */
  public void addFsToIndexes(FeatureStructure instance) {
    casImpl.addFsToIndexes(instance);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#removeFsFromIndexes(org.apache.uima.cas.FeatureStructure)
   */
  public void removeFsFromIndexes(FeatureStructure instance) {
    casImpl.removeFsFromIndexes(instance);
  }

  
  /* (non-Javadoc)
   * @see org.apache.uima.jcas.JCas#removeAllIncludingSubtypes(int)
   */
  public void removeAllIncludingSubtypes(int i) {
    getFSIndexRepository().removeAllIncludingSubtypes(getCasType(i));    
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.JCas#removeAllExcludingSubtypes(int)
   */
  public void removeAllExcludingSubtypes(int i) {
    getFSIndexRepository().removeAllExcludingSubtypes(getCasType(i));    
  }

  /**
   * @see org.apache.uima.cas.CAS#fs2listIterator(FSIterator)
   */
  public <T extends FeatureStructure> ListIterator<T> fs2listIterator(FSIterator<T> it) {
    return casImpl.fs2listIterator(it);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.BaseCas#createFeatureValuePath(java.lang.String)
   */
  public FeatureValuePath createFeatureValuePath(String featureValuePath)
      throws CASRuntimeException {
    return casImpl.createFeatureValuePath(featureValuePath);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.BaseCas#createSofa(org.apache.uima.cas.SofaID, java.lang.String)
   * @deprecated
   */
  public SofaFS createSofa(SofaID sofaID, String mimeType) {
    // extract absolute SofaName string from the ID
    return casImpl.createSofa(sofaID, mimeType);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.BaseCas#getIndexRepository()
   */
  public FSIndexRepository getIndexRepository() {
    return casImpl.getIndexRepository();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.BaseCas#getViewName()
   */
  public String getViewName() {
    return casImpl.getViewName();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.BaseCas#size()
   */
  public int size() {
    // TODO improve this to account for JCas
    // structure sizes
    return casImpl.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getAnnotationIndex()
   */
  @SuppressWarnings("unchecked")
  public AnnotationIndex<Annotation> getAnnotationIndex() {
    return (AnnotationIndex<Annotation>) (AnnotationIndex<?>) 
            casImpl.getAnnotationIndex();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getAnnotationIndex(org.apache.uima.cas.Type)
   */
  @SuppressWarnings("unchecked")
  public AnnotationIndex<Annotation> getAnnotationIndex(Type type) throws CASRuntimeException {
    return (AnnotationIndex<Annotation>) (AnnotationIndex<?>)  
            casImpl.getAnnotationIndex(type);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getAnnotationIndex(int)
   */
  @SuppressWarnings("unchecked")
  public AnnotationIndex<Annotation> getAnnotationIndex(int type) throws CASRuntimeException {
    return (AnnotationIndex<Annotation>) (AnnotationIndex<?>) 
            casImpl.getAnnotationIndex(this.getCasType(type));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getViewIterator()
   */
  public Iterator<JCas> getViewIterator() throws CASException {
    List<JCas> viewList = new ArrayList<JCas>();
    Iterator<CAS> casViewIter = casImpl.getViewIterator();
    while (casViewIter.hasNext()) {
      viewList.add((casViewIter.next()).getJCas());
    }
    return viewList.iterator();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getViewIterator(java.lang.String)
   */
  public Iterator<JCas> getViewIterator(String localViewNamePrefix) throws CASException {
    List<JCas> viewList = new ArrayList<JCas>();
    Iterator<CAS> casViewIter = casImpl.getViewIterator(localViewNamePrefix);
    while (casViewIter.hasNext()) {
      viewList.add((casViewIter.next()).getJCas());
    }
    return viewList.iterator();
  }
}
