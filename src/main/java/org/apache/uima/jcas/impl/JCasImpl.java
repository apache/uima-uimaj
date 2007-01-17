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
// * check iterators returning over mixed types
// * both old style jfs returning mixed new/ole
// * and new style fsiterators returning mixed?
// * reinit - have it discard and reget jcas instance
// * call to getJCas from CASImpl - fix to simplify
// * getStringArray0 etc - check if OK with new impl in old code
// *
// * Compatibility removes at some point: TypeSystemInit and it's caller
// * done:

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

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
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.impl.LowLevelException;
import org.apache.uima.cas.impl.LowLevelIndexRepository;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.cas.TOP_Type;

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
 * JCas and Foo_Type style classes have one instantiation per CAS
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
 * Implementation Notes: --------------------- At loadtime, Foo and Foo_Type classes assigned static
 * integer indexes These indexes are used in arrays in the jcas instance to go from Foo class (not
 * instances) to Foo_Type instances (one per CAS) Things which require "types" at runtime reference
 * the Foo_Type instances.
 * 
 * Maps: Note: per CAS means per shared JCas instance assoc w/ CAS
 * 
 * (universal) (static field in class) go from Foo class to index unique ID used with next map to
 * locate Foo_Type instance associated with this class If universal - becomes an index for all
 * FooStyle classes loaded (per CAS) (ArrayList) map index from Foo (static) to Foo_Type instance
 * used in creating new instances. Needs to be one per CAS because a particular "xyz" feature in
 * CAS1 != "xyz" feature in CAS2 If above universal, then can become large array with mostly unused
 * slots. Possibility: reuse no-longer-used slots. Identify no-longer-used slots at CAS Type unload
 * event?
 */

/**
 * implements the supporting infrastructure for JCas model linked with a Cas. There is one logical
 * instance of this instantiated per Cas. If you hold a reference to a CAS, to get a reference to
 * the corresponding JCas, use the method getJCas(). Likewise, if you hold a reference to this
 * object, you can get a reference to the corresponding CAS object using the method getCas().
 * <p>
 * There are variables here that hold references to constant, frequently needed Cas values, such as
 * 0-length FSArrays, 0-length Strings, etc. These can be used to improve efficiency, since the same
 * object can be shared and used for all instances because it is unchangeable. These objects are
 * reset when the CAS is reset, and are initialized lazily, on first use.
 */

public class JCasImpl extends AbstractCas_ImplBase implements AbstractCas, JCas {
  // * FSIndexRepository - the get method returns the java type *
  // * so FSIndexRepository can't be in the implements clause *

  // *************************************************
  // * Static Data shared with all instances of JCas *
  // *************************************************
  private final static int INITIAL_HASHMAP_SIZE = 200;


  // **********************************************
  // * Data shared among views of a single CAS *
  // * We keep one copy per view set *
  // **********************************************/
  private static class JCasSharedView {
    // ********************************************************
    // * Data shared among all views in a (J)CAS View group *
    // * Access to this data is assumed to be single threaded *
    // ********************************************************

    /* convenience holders of CAS constants that may be useful */
    /* initialization done lazily - on first call to getter */

    public StringArray stringArray0L = null;

    public IntegerArray integerArray0L = null;

    public FloatArray floatArray0L = null;

    public FSArray fsArray0L = null;

    // * collection of errors that occur during initialization
    public Collection errorSet = new ArrayList();
  }

  // *******************
  // * Data per (J)CAS *
  // * There may be multiples of these for one base CAS - one per "view"
  // * Access to this data is assumed to be single threaded
  // *******************

  /** key = CAS addr, value = corresponding Java instance 
   * The reason there are multiple cover objects per Cas object
   * is that the JCas cover object distinguishes the CAS View
   * the object belongs to, in order to support the
   * methods:  aJCasCoverObjectInstance.addToIndexes()
   * which needs to know which view to use
   * This "convenience" of not needing to specify which CAS
   * view to use here, is traded off with space: the same object
   * indexed in multiple views ends up not "sharing" the Java
   * cover object (because the objects are not identical - they
   * refer to different views).
   */

  public Map cAddr2Jfs;

  private int prevCaddr2JfsSize = INITIAL_HASHMAP_SIZE;

  private final JCasSharedView sharedView;

  private final CASImpl casImpl; // not public to protect it from accidents

  private final LowLevelIndexRepository ll_IndexRepository;

  private final JFSIndexRepository jfsIndexRepository;

  /*
   * typeArray is one per CAS because holds pointers to instances of _Type objects, per CAS
   */
  private final TOP_Type[] typeArray; // contents are subtypes of TOP_Type

  // *********************************
  // * Getters for read-only objects *
  // *********************************
  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getFSIndexRepository()
   */
  public FSIndexRepository getFSIndexRepository() {
    return casImpl.getIndexRepository();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getLowLevelIndexRepository()
   */
  public LowLevelIndexRepository getLowLevelIndexRepository() {
    return ll_IndexRepository;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getCas()
   */
  public CAS getCas() {
    return casImpl;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getCasImpl()
   */
  public CASImpl getCasImpl() {
    return casImpl;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getLowLevelCas()
   */
  public LowLevelCAS getLowLevelCas() {
    return casImpl;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getTypeSystem()
   */
  public TypeSystem getTypeSystem() {
    return casImpl.getTypeSystem();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getType(int)
   */
  public TOP_Type getType(int i) {
    if (i >= typeArray.length || null == typeArray[i]) {

      // unknown ID. Attempt to get offending class.
      Class cls = JCasRegistry.getClassForIndex(i);
      if (cls != null) {
        String typeName = cls.getName();
        // is type in type system
        if (this.casImpl.getTypeSystem().getType(typeName) == null) {
          // no - report error that JCAS type was not defined in XML descriptor
          CASRuntimeException casEx = new CASRuntimeException(
                  CASRuntimeException.JCAS_TYPE_NOT_IN_CAS);
          casEx.addArgument(typeName);
          throw casEx;
        } else {
          // yes - there was some problem loading the _Type object
          CASRuntimeException casEx = new CASRuntimeException(
                  CASRuntimeException.JCAS_MISSING_COVERCLASS);
          casEx.addArgument(typeName + "_Type");
          throw casEx;
        }

      } else {
        throw new CASRuntimeException(CASRuntimeException.JCAS_UNKNOWN_TYPE_NOT_IN_CAS);
      }
    }
    return typeArray[i];
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getCasType(int)
   */
  public Type getCasType(int i) {
    return getType(i).casType;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getType(org.apache.uima.jcas.cas.TOP)
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

  private static class LoadedJCasType {
    Type casType;

    Class clas;

    LoadedJCasType(Type casType, Class clas) {
      this.casType = casType;
      this.clas = clas;
    }
  }

  // never called, but have to set values to null because they're final
  private JCasImpl() {
    sharedView = null;
    casImpl = null;
    typeArray = null;
    ll_IndexRepository = null;
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
    // * At this point, some but not necessarily all of the Types have been loaded

    // * the typeArray needs to be big enough to hold all the types
    // * that will be loaded.

    this.casImpl = cas;
    if (casImpl != casImpl.getBaseCAS()) {
      sharedView = ((JCasImpl)casImpl.getBaseCAS().getJCas()).sharedView;
      sharedView.errorSet.clear();
    } else {
      sharedView = new JCasSharedView();
    }

    cAddr2Jfs = new HashMap(INITIAL_HASHMAP_SIZE);

    this.ll_IndexRepository = casImpl.ll_getIndexRepository();
    this.jfsIndexRepository = new JFSIndexRepositoryImpl(this, cas.getIndexRepository());

    // * acquire the lock for this thread that is the same lock
    // * used in the getNextIndexIncr operation. This will block
    // * any other (meaning on another thread)
    // * loading of the JCas Type classes until this thread's loading
    // * completes.

    synchronized (JCasImpl.class) {

      final TypeSystem ts = casImpl.getTypeSystem();
      Iterator typeIt = ts.getTypeIterator();
      Type t;
      String casName;
      Map jcasTypes = new HashMap();

      // * note that many of these may have already been loaded
      // * load all the others. Actually, we ask to load all the types
      // * and the ones already loaded - we just get their loaded versions returned.
      // * Loading will run the static init functions.

      while (typeIt.hasNext()) {
        t = (Type) typeIt.next();
        casName = t.getName();
        String name_Type;
        if (builtInsWithNoJCas.contains(casName))
          continue;
        if (builtInsWithAltNames.contains(casName))
          // * convert uima.cas.Xxx -> org.apache.uima.jcas.cas.Xxx
          // * convert uima.tcas.Annotator -> org.apache.uima.jcas.tcas.Annotator
          try {
            String nameBase = "org.apache.uima.jcas." + casName.substring(5);
            name_Type = nameBase + "_Type";
            jcasTypes.put(nameBase, new LoadedJCasType(t, Class.forName(name_Type, true, cas
                    .getJCasClassLoader())));
          } catch (ClassNotFoundException e1) {
            // OK for DocumentAnnotation, which may not have a cover class.  Otherwise, not OK.
            if (!CAS.TYPE_NAME_DOCUMENT_ANNOTATION.equals(casName)) {
              assert false : "never get here because built-ins have java cover types";
              e1.printStackTrace();
            }
          }

        // this is done unconditionally to pick up old style cover functions if any
        // as well as other JCas model types
        try {
          name_Type = casName + "_Type";
          jcasTypes.put(casName, new LoadedJCasType(t, Class.forName(name_Type, true, cas
                  .getJCasClassLoader())));
          // also force the load the plain name without _Type for old-style - that's where
          // the index is incremented
          Class.forName(casName, true, cas.getJCasClassLoader());
        } catch (ClassNotFoundException e1) {
          // many classes may not have JCas models, so this is not an error
        }
      }

      typeArray = new TOP_Type[JCasRegistry.getNumberOfRegisteredClasses()];

      Iterator jcasTypeIt = jcasTypes.entrySet().iterator();
      while (jcasTypeIt.hasNext()) {
        LoadedJCasType lt = (LoadedJCasType) ((Map.Entry) jcasTypeIt.next()).getValue();
        makeInstanceOf_Type(lt.casType, lt.clas, cas);
      }
      if (true) {

        typeIt = ts.getTypeIterator(); // reset iterator to start
        Type topType = ts.getTopType();
        Type superType = null;
        while (typeIt.hasNext()) {
          t = (Type) typeIt.next();
          if (builtInsWithNoJCas.contains(t.getName()))
            continue;
          //comment here
          if (CAS.TYPE_NAME_DOCUMENT_ANNOTATION.equals(t.getName())) {
            if (jcasTypes.get("org.apache.uima.jcas.tcas.DocumentAnnotation") != null) 
              continue;                    
          }
          else if (builtInsWithAltNames.contains(t.getName()))
            continue; // because jcasTypes has no entry for these
          if (null != jcasTypes.get(t.getName()))
            continue;
          // we believe that at this point, t is not "top", because top is always loaded
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
          cas.getFSClassRegistry().copyGeneratorForType((TypeImpl) t, (TypeImpl) superType);
        }
      }
    } // end of synchronized block
  }

  /**
   * creates a new JCas instance that corresponds to a CAS instance. Will be called once by the UIMA
   * framework when it creates the CAS.
   * 
   * @param cas
   *          a CAS instance
   * @return newly created and initialized JCas
   */
  public static JCas getJCas(CASImpl cas) throws CASException {
    JCasImpl jcas = new JCasImpl(cas);
    if (jcas.sharedView.errorSet.size() > 0) {
      StringBuffer msg = new StringBuffer(100);
      CASException e = new CASException(CASException.JCAS_INIT_ERROR);
      Iterator iter = jcas.sharedView.errorSet.iterator();
      while (iter.hasNext()) {
        Exception f = (Exception) iter.next();
        msg.append(f.getMessage());
        msg.append("\n");
      }
      e.addArgument(msg.toString());
      throw e;
    }
    return jcas;
  }

  /**
   * built-in types which have alternate names It really isn't necessary to skip these - they're
   * never instantiated. But it is a very slight performance boost, and it may be safer given
   * possible future changes to these types' implementations.
   */
  private static Collection builtInsWithNoJCas = new ArrayList();
  {
    if (builtInsWithNoJCas.size() == 0) {
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
  }

  private static Collection builtInsWithAltNames = new ArrayList();
  { // initialization code
    if (builtInsWithAltNames.size() == 0) {
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
  }

  /**
   * Make the instance of the JCas xxx_Type class for this CAS. Note: not all types will have
   * xxx_Type. Instance creation does the typeSystemInit kind of function, as well.
   */

  private void makeInstanceOf_Type(Type casType, Class clas, CASImpl cas) {
    Constructor c;
    Field typeIndexField = null;
    int typeIndex;
    try {
      c = clas.getDeclaredConstructor(jcasBaseAndType);
      try {

        typeIndexField = clas.getDeclaredField("typeIndexID");
      } catch (NoSuchFieldException e) {
        try {
          // old version has the index in the base type
          String name = clas.getName();
          Class clas2 = Class.forName(name.substring(0, name.length() - 5), true, cas
                  .getJCasClassLoader()); // drop _Type
          typeIndexField = clas2.getDeclaredField("typeIndexID");
        } catch (NoSuchFieldException e2) {
          logAndThrow(e2);
        } catch (ClassNotFoundException e3) {
          logAndThrow(e3);
        }
      }
      typeIndex = typeIndexField.getInt(null); // null - static instance var
      TOP_Type x_Type_instance = (TOP_Type) c.newInstance(new Object[] { this, casType });
      typeArray[typeIndex] = x_Type_instance;
    } catch (SecurityException e) {
      logAndThrow(e);
    } catch (NoSuchMethodException e) {
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
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getRequiredType(java.lang.String)
   */
  public Type getRequiredType(String s) throws CASException {
    Type t = getTypeSystem().getType(s);
    if (null == t) {
      CASException casEx = new CASException(CASException.JCAS_TYPENOTFOUND_ERROR);
      casEx.addArgument(s);
      throw casEx;
    }
    return t;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getRequiredFeature(org.apache.uima.cas.Type, java.lang.String)
   */
  public Feature getRequiredFeature(Type t, String s) throws CASException {
    Feature f = t.getFeatureByBaseName(s);
    if (null == f) {
      CASException casEx = new CASException(CASException.JCAS_FEATURENOTFOUND_ERROR);
      casEx.addArgument(t.getName());
      casEx.addArgument(s);
      throw casEx;
    }
    return f;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getRequiredFeatureDE(org.apache.uima.cas.Type, java.lang.String, java.lang.String, boolean)
   */

  public Feature getRequiredFeatureDE(Type t, String s, String rangeName, boolean featOkTst) {
    Feature f = t.getFeatureByBaseName(s);
    Type rangeType = this.getTypeSystem().getType(rangeName);
    if (null == f && !featOkTst) {
      CASException casEx = new CASException(CASException.JCAS_FEATURENOTFOUND_ERROR);
      casEx.addArgument(t.getName());
      casEx.addArgument(s);
      sharedView.errorSet.add(casEx);
    }
    if (null != f)
      try {
        casImpl.checkTypingConditions(t, rangeType, f);
      } catch (LowLevelException e) {
        CASException casEx = new CASException(CASException.JCAS_FEATURE_WRONG_TYPE);
        casEx.addArgument(t.getName());
        casEx.addArgument(s);
        casEx.addArgument(rangeName);
        casEx.addArgument(((FeatureImpl) f).getRange().toString());
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
    CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_FEAT);
    e.addArgument(feat);
    e.addArgument(type);
    throw e;
  }

  // constant used in the following function
  /** internal use - constant used in getting constructors */
  final static private Class[] jcasBaseAndType = new Class[] { JCas.class, Type.class };

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#putJfsFromCaddr(int, org.apache.uima.cas.FeatureStructure)
   */
  public void putJfsFromCaddr(int casAddr, FeatureStructure fs) {
    cAddr2Jfs.put(new Integer(casAddr), fs);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getJfsFromCaddr(int)
   */
  public TOP getJfsFromCaddr(int casAddr) {
    return (TOP) cAddr2Jfs.get(new Integer(casAddr));
  }

  // * Implementation of part of the Cas interface as part of JCas*

  /**
   * (Internal Use only) called by the CAS reset function - clears the hashtable holding the
   * associations.
   */
  public static void clearData(CAS cas) throws CASException {
    JCasImpl jcas = (JCasImpl) cas.getJCas();
    int hashSize = Math.max(jcas.cAddr2Jfs.size(), 32); // not worth dropping below 32
    // System.out.println("\n***JCas Resizing Hashtable: size is: " + hashSize + ", curmax = " +
    // jcas.prevCaddr2JfsSize);
    if (hashSize <= (jcas.prevCaddr2JfsSize >> 1)) {
      // System.out.println("JCas Shrinking Hashtable from " + jcas.prevCaddr2JfsSize);
      jcas.prevCaddr2JfsSize = hashSize;
      jcas.cAddr2Jfs = new HashMap(hashSize);
    } else {
      jcas.prevCaddr2JfsSize = Math.max(hashSize, jcas.prevCaddr2JfsSize);
      // System.out.println("JCas clearing - keeping same size, new max prev size = " +
      // jcas.prevCaddr2JfsSize);
      jcas.cAddr2Jfs.clear();
    }
    jcas.sharedView.stringArray0L = null;
    jcas.sharedView.floatArray0L = null;
    jcas.sharedView.fsArray0L = null;
    jcas.sharedView.integerArray0L = null;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#reset()
   */
  public void reset() {
    casImpl.reset();
  }

  private final static int NULL = 0;

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#checkArrayBounds(int, int)
   */
  public final void checkArrayBounds(int fsRef, int pos) {
    if (NULL == fsRef) {
      LowLevelException e = new LowLevelException(LowLevelException.NULL_ARRAY_ACCESS);
      // note - need to add this to ll_runtimeException
      e.addArgument(Integer.toString(pos));
      throw e;
    }
    final int arrayLength = casImpl.getArraySize(fsRef);
    if (pos < 0 || pos >= arrayLength) {
      LowLevelException e = new LowLevelException(LowLevelException.ARRAY_INDEX_OUT_OF_RANGE);
      e.addArgument(Integer.toString(pos));
      throw e;
    }
  }

  // *****************
  // * Sofa support *
  // *****************

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getSofa(org.apache.uima.cas.SofaID)
   */
  public Sofa getSofa(SofaID sofaID) {
    return (Sofa) casImpl.getSofa(sofaID);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getSofa()
   */
  public Sofa getSofa() {
      return (Sofa) casImpl.getSofa();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#createView(java.lang.String)
   */
  public JCas createView(String sofaID) throws CASException {
    return casImpl.createView(sofaID).getJCas();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getJCas(org.apache.uima.jcas.cas.Sofa)
   */
  public JCas getJCas(Sofa sofa) throws CASException {
    return casImpl.getView(sofa).getJCas();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getSofaIterator()
   */
  public FSIterator getSofaIterator() {
    return casImpl.getSofaIterator();
  }

  // *****************
  // * Index support *
  // *****************

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getJFSIndexRepository()
   */
  public JFSIndexRepository getJFSIndexRepository() {
    return jfsIndexRepository;
  }

  // ****************
  // * TCas support *
  // ****************

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getDocumentAnnotationFs()
   */
  public TOP getDocumentAnnotationFs() {
    return (TOP) casImpl.getDocumentAnnotation();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getDocumentText()
   */
  public String getDocumentText() {
    return casImpl.getDocumentText();
 }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getSofaDataString()
   */
  public String getSofaDataString() {
    return casImpl.getSofaDataString();
 }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getSofaDataArray()
   */
  public FeatureStructure getSofaDataArray() {
    return casImpl.getSofaDataArray();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getSofaDataURI()
   */
  public String getSofaDataURI() {
    return casImpl.getSofaDataURI();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#setDocumentText(java.lang.String)
   */
  public void setDocumentText(String text) throws CASRuntimeException {
    casImpl.setDocumentText(text);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#setSofaDataString(java.lang.String, java.lang.String)
   */
  public void setSofaDataString(String text, String mime) throws CASRuntimeException {
    casImpl.setSofaDataString(text, mime);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#setSofaDataArray(org.apache.uima.jcas.cas.TOP, java.lang.String)
   */
  public void setSofaDataArray(FeatureStructure array, String mime) throws CASRuntimeException {
    casImpl.setSofaDataArray(array, mime);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#setSofaDataURI(java.lang.String, java.lang.String)
   */
  public void setSofaDataURI(String uri, String mime) throws CASRuntimeException {
    casImpl.setSofaDataURI(uri, mime);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getDocumentLanguage()
   */
  public String getDocumentLanguage() {
    return casImpl.getDocumentLanguage();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#setDocumentLanguage(java.lang.String)
   */
  public void setDocumentLanguage(String language) throws CASRuntimeException {
    casImpl.setDocumentLanguage(language);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getSofaDataStream()
   */
  public InputStream getSofaDataStream() {
    return casImpl.getSofaDataStream();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getConstraintFactory()
   */
  public ConstraintFactory getConstraintFactory() {
    return casImpl.getConstraintFactory();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#createFeaturePath()
   */
  public FeaturePath createFeaturePath() {
    return casImpl.createFeaturePath();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#createFilteredIterator(org.apache.uima.cas.FSIterator, org.apache.uima.cas.FSMatchConstraint)
   */
  public FSIterator createFilteredIterator(FSIterator it, FSMatchConstraint constraint) {
    return casImpl.createFilteredIterator(it, constraint);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getStringArray0L()
   */

  public StringArray getStringArray0L() {
    if (null == sharedView.stringArray0L)
      sharedView.stringArray0L = new StringArray(this, 0);
    return sharedView.stringArray0L;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getIntegerArray0L()
   */
  public IntegerArray getIntegerArray0L() {
    if (null == sharedView.integerArray0L)
      sharedView.integerArray0L = new IntegerArray(this, 0);
    return sharedView.integerArray0L;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getFloatArray0L()
   */
  public FloatArray getFloatArray0L() {
    if (null == sharedView.floatArray0L)
      sharedView.floatArray0L = new FloatArray(this, 0);
    return sharedView.floatArray0L;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#getFSArray0L()
   */
  public FSArray getFSArray0L() {
    if (null == sharedView.fsArray0L)
      sharedView.fsArray0L = new FSArray(this, 0);
    return sharedView.fsArray0L;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#processInit()
   */
  public void processInit() {
    // unused
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.AbstractCas_ImplBase#setOwn
   */
  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#setOwner(org.apache.uima.cas.CasOwner)
   */
  public void setOwner(CasOwner aCasOwner) {
    casImpl.setOwner(aCasOwner);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#release()
   */
  public void release() {
    casImpl.release();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.JCas#getView(java.lang.String)
   */
  public JCas getView(String localViewName) throws CASException {
    return casImpl.getView(localViewName).getJCas();
  }
  /* (non-Javadoc)
   * @see org.apache.uima.jcas.JCas#getView(org.apache.uima.cas.SofaFS)
   */
  public JCas getView(SofaFS aSofa) throws CASException {
    return casImpl.getView(aSofa).getJCas();
  }
  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#addFsToIndexes(org.apache.uima.cas.FeatureStructure)
   */
  public void addFsToIndexes(FeatureStructure instance) {
    casImpl.addFsToIndexes(instance);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.impl.IJCas#removeFsFromIndexes(org.apache.uima.cas.FeatureStructure)
   */
  public void removeFsFromIndexes(FeatureStructure instance) {
    casImpl.removeFsFromIndexes(instance);
  }

  /**
   * @see org.apache.uima.cas.CAS#fs2listIterator(FSIterator)
   */
  public ListIterator fs2listIterator(FSIterator it) {
    return casImpl.fs2listIterator(it);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.BaseCas#createFeatureValuePath(java.lang.String)
   */
  public FeatureValuePath createFeatureValuePath(String featureValuePath)
          throws CASRuntimeException {
    return casImpl.createFeatureValuePath(featureValuePath);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.BaseCas#createSofa(org.apache.uima.cas.SofaID, java.lang.String)
   * @deprecated
   */
  public SofaFS createSofa(SofaID sofaID, String mimeType) {
    // extract absolute SofaName string from the ID
    return casImpl.createSofa(sofaID, mimeType);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.BaseCas#getIndexRepository()
   */
  public FSIndexRepository getIndexRepository() {
    return casImpl.getIndexRepository();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.BaseCas#getViewName()
   */
  public String getViewName() {
    return casImpl.getViewName();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.BaseCas#size()
   */
  public int size() {
    //TODO improve this to account for JCas
    //  structure sizes
    return casImpl.size();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.JCas#getAnnotationIndex()
   */
  public AnnotationIndex getAnnotationIndex() {
    return casImpl.getAnnotationIndex();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.JCas#getAnnotationIndex(org.apache.uima.cas.Type)
   */
  public AnnotationIndex getAnnotationIndex(Type type) throws CASRuntimeException {
    return casImpl.getAnnotationIndex(type);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.JCas#getAnnotationIndex(int)
   */
  public AnnotationIndex getAnnotationIndex(int type) throws CASRuntimeException {
    return casImpl.getAnnotationIndex(this.getCasType(type));
  }

}
