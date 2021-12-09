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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.AbstractCas_ImplBase;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.CasOwner;
import org.apache.uima.cas.ConstraintFactory;
import org.apache.uima.cas.FSIndex;
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
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.impl.LowLevelIndexRepository;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
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

/**
 * 
 * Overview 
 * ========
 * This design uses classes for types, not Interfaces. JCas CAS types are represented in a running
 * server by a collection of classes, one for each loaded equivalent-to-CAS type Foo.
 * 
 * In v2, JCas was optional;
 * In v3, JCas is still optional, but the JCas classes are used in all cases for the feature-final built-ins
 *   - which are those classes that are skipped when JCasgen is run
 * 
 * Built-in JCas classes have one definition.
 * Custom JCas classes have one definition per classloader
 *   - Running a pipeline with a custom extension classloader
 *   - PEAR Wrappers support contexts with the PEAR where there's potentially different JCas implementations.
 *   - Running with different JCas classes is possible using user (or other framework (e.g. servlet)) class loader isolation  
 * 
 * Hierarchy: The JCas class hierarchy (super class structure) follows the UIMA type hierarchy
 *   - with some additional shared-code style classes
 *   - with some additional "marker" interfaces (e.g the built-in UIMA list - marking empty and non-empty nodes)
 *   - TOP extends FeatureStructureImplC 
 *      -- which has the non-JCas support for representing Feature Structures as Java Objects
 *      
 * I N S T A N C E S   of these classes
 *   - belong to a CAS, and record the particular CAS view used when creating the instance 
 *     -- specifies the CAS to which the Feature Structure belongs
 *     -- is the view in which they were created
 *        --- used for instance.addToIndexes
 *        --- used for checking - e.g. can't create an Annotation in the "base" CAS view
 * 
 * The CAS must be updated on a single thread.
 *   A read-only CAS may be accessed on multiple threads.
 * 
 * At classloader load time, JCas classes are assigned an incrementing static integer index.
 * This index is used with a table kept per Type System (possibly shared among multiple CASes)
 * to locate the corresponding TypeImpl
 *   - this TypeImpl is set in a local field in every FS instance when the instance is created
 *   - multiple JCas cover classes (loaded under different classloaders) may end up having the same TypeImpl
 *     -- e.g. inside a PEAR
 *  
 * _______________________________________________________________________    
 * T r e a t m e n t   o f   e m b e d d e d  classloading context (PEARS)
 * 
 * In v2, different definitions of JCas cover classes were possible within a PEAR, and the 
 *   implementation switched among these.
 *   
 * In v3, we copy this implementation.  For those types which have new JCas definitions in the PEAR's
 * classpath, special versions of Feature Structure instances of those JCas classes are constructed,
 * called "trampoline" FSs.  These have an internal flag set indicating they're trampolines, and their
 * refs to the int[] and Object[] values are "shared" with the non-PEAR FSs.
 * 
 * When creating new instances, if the PEAR context defines a different JCas class for this type, two FSs
 * are created: a "base" FS and the trampoline FS.
 * 
 * When iterating and retrieving a FS, if in a PEAR context and the type has a different JCas class from the base,
 * return a (possibly new) trampoline for that FS.
 *   - the trampolines are kept in a JCasHashMap, indexed by class loader (in case there are multiple PEARs in one pipeline)
 *   - Once created, the same trampoline is reused when called for
 *   
 * UIMA structures storing Feature Structures (e.g. indexes) always store the base (non-trampoline) version.
 *   - Methods like add-to-indexes convert a trampoline to its corresponding base  
 * 
 * (Possible future generalization for any internals-hiding AE component - not supported)
 *   - support non-input/output Type isolation for internals-hiding components
 *     -- types not specified as input/output are particularized to the internals-hiding component
 *       --- removed from indexes upon exit (because they're internal use only)    
 */

/**
 * implements the supporting infrastructure for JCas model linked with a Cas. There is one logical
 * instance of this instantiated per CasView. If you hold a reference to a CAS, to get a reference
 * to the corresponding JCas, use the method getJCas(). Likewise, if you hold a reference to this
 * object, you can get a reference to the corresponding CAS object using the method getCas().
 */

public class JCasImpl extends AbstractCas_ImplBase implements AbstractCas, JCas {

  // **********************************************
  // * Data shared among views of a single CAS *
  // * We keep one copy per CAS view set *
  // **********************************************/

  // *******************
  // * Data per (J)CAS *
  // * There may be multiples of these for one base CAS - one per "view"
  // * Access to this data is assumed to be single threaded
  // *******************

  // not public to protect it from accidents
  private final CASImpl casImpl;

  private final LowLevelIndexRepository ll_IndexRepository;

  private final JFSIndexRepository jfsIndexRepository;

  // *********************************
  // * Getters for read-only objects *
  // *********************************
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getFSIndexRepository()
   */
  @Override
  public FSIndexRepository getFSIndexRepository() {
    return casImpl.getIndexRepository();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getLowLevelIndexRepository()
   */
  @Override
  public LowLevelIndexRepository getLowLevelIndexRepository() {
    return ll_IndexRepository;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getCas()
   */
  @Override
  public CAS getCas() {
    return casImpl;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getCasImpl()
   */
  @Override
  public CASImpl getCasImpl() {
    return casImpl;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getLowLevelCas()
   */
  @Override
  public LowLevelCAS getLowLevelCas() {
    return casImpl;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getTypeSystem()
   */
  @Override
  public TypeSystem getTypeSystem() {
    return casImpl.getTypeSystem();
  }

  TypeSystemImpl getTypeSystemImpl() {
    return casImpl.getTypeSystemImpl();
  }

  /*
   * @see org.apache.uima.jcas.JCas#getType(int)
   */
  @Override
  public TOP_Type getType(final int i) {
    throw new UnsupportedOperationException("UIMA V2 operation not supported in V3");
    // if (i >= typeArray.length || null == typeArray[i]) {
    // getTypeInit(i);
    // }
    // return typeArray[i];
  }

  /*
   * @see org.apache.uima.jcas.JCas#getType(org.apache.uima.jcas.cas.TOP)
   */
  @Override
  public TOP_Type getType(TOP instance) {
    return getType(instance.getTypeIndexID());
  }

  /*
   * Given Foo.type, return the corresponding CAS Type object. This is useful in the methods which
   * require a CAS Type, for instance iterator creation. (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getCasType(int)
   * 
   */
  @Override
  public Type getCasType(int i) {
    return getTypeSystemImpl().getJCasRegisteredType(i);
  }

  // /** throws (an unchecked) CASRuntimeException */
  // private static void logAndThrow(Exception e) {
  // CASRuntimeException casEx = new CASRuntimeException(CASRuntimeException.JCAS_CAS_MISMATCH);
  // casEx.initCause(e);
  // throw casEx;
  // }

  // never called, but have to set values to null because they're final
  private JCasImpl() {
    casImpl = null;
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
  private JCasImpl(CASImpl cas) {

    // * A new instance of JCas exists for each CAS
    // * At this point, some but not necessarily all of the Types have been
    // loaded

    // * the typeArray needs to be big enough to hold all the types
    // * that will be loaded.

    this.casImpl = cas;

    this.ll_IndexRepository = casImpl.ll_getIndexRepository();
    this.jfsIndexRepository = new JFSIndexRepositoryImpl(this, cas.getIndexRepository());
  }

  public TOP createFS(Type type) {
    return casImpl.createFS(type);
  }

  /**
   * creates a new JCas instance that corresponds to a CAS instance. Will be called once by the UIMA
   * framework when it creates the CAS.
   * 
   * @param cas
   *          a CAS instance
   * @return newly created and initialized JCas // * @throws CASException -
   */
  public static JCasImpl getJCas(CASImpl cas) {
    return getJCasImpl(cas);
  }

  /**
   * creates a new JCas instance that corresponds to a CAS instance. Will be called once by the UIMA
   * framework when it creates the CAS.
   * 
   * @param cas
   *          a CAS instance
   * @return newly created and initialized JCas
   */
  private static JCasImpl getJCasImpl(CASImpl cas) {
    return new JCasImpl(cas);
  }

  // This generator class is used in place of the generator that is in each of the
  // older JCasGen'd classes

  // It makes use of the passed-in CAS view so one generator works for all Cas Views.
  // It references the xxx_Type instance using the getType call, which will
  // (lazily) instantiate the xxx_Type object if needed (due to switching class loaders:
  // see comment under getType(int))

  // static private class JCasFsGenerator<T extends TOP> implements FSGenerator<T> {
  // // multiple reader threads in same CAS
  // static final ThreadLocal<Object[]> initArgsThreadLocal = new ThreadLocal<Object[]>() {
  // protected Object[] initialValue() { return new Object[2]; } };
  //
  // private final int type;
  //
  // private final Constructor<T> c;
  //
  // private final boolean isSubtypeOfAnnotationBase;
  //
  // private final int sofaNbrFeatCode;
  //
  // private final int annotSofaFeatCode;
  //
  //
  // JCasFsGenerator(int type, Constructor<T> c, boolean isSubtypeOfAnnotationBase,
  // int sofaNbrFeatCode, int annotSofaFeatCode) {
  // this.type = type;
  // this.c = c;
  // this.isSubtypeOfAnnotationBase = isSubtypeOfAnnotationBase;
  // this.sofaNbrFeatCode = sofaNbrFeatCode;
  // this.annotSofaFeatCode = annotSofaFeatCode;
  // }

  /*
   * Called from the CAS's this.svd.localFsGenerators
   * 
   * Those are set up with either JCas style generators, or the the shared common instances of
   * FeatureStructureImplC for non-JCas classes.
   * 
   */
  // Called in 2 cases
  // 1) a non-JCas call to create a new JCas style FS
  // 2) a low-level iterator
  // public T createFS(int addr, CASImpl casView) {
  // try {
  // JCasImpl jcasView = (JCasImpl) casView.getJCas();
  // T fs = jcasView.<T>getJfsFromCaddr(addr);
  // if (null != fs) {
  // fs.jcasType = jcasView.getType(type);
  // return fs;
  // }
  // return doCreateFS(addr, casView);
  // } catch (CASException e1) {
  // logAndThrow(e1, null);
  // return null; // to avoid compile warning
  // }
  // }
  //
  // private T doCreateFS(int addr, CASImpl casView) {
  // // this funny logic is because although the annotationView should always be set if
  // // a type is a subtype of annotation, it isn't always set if an application uses low-level
  // // api's. Rather than blow up, we limp along.
  // CASImpl maybeAnnotationView = null;
  // if (isSubtypeOfAnnotationBase) {
  // final int sofaNbr = getSofaNbr(addr, casView);
  // if (sofaNbr > 0) {
  // maybeAnnotationView = (CASImpl) casView.getView(sofaNbr);
  // }
  // }
  // final CASImpl view = (null != maybeAnnotationView) ? maybeAnnotationView : casView;
  //
  // try {
  // JCasImpl jcasView = (JCasImpl) view.getJCas();
  // final Object[] initargs = initArgsThreadLocal.get();
  // initargs[0] = Integer.valueOf(addr);
  // initargs[1] = jcasView.getType(type);
  // T fs = null;
  // try {
  // fs = (T) c.newInstance(initargs);
  // } catch (IllegalArgumentException e) {
  // logAndThrow(e, jcasView);
  // } catch (InstantiationException e) {
  // logAndThrow(e, jcasView);
  // } catch (IllegalAccessException e) {
  // logAndThrow(e, jcasView);
  // } catch (InvocationTargetException e) {
  // logAndThrow(e, jcasView);
  // }
  // jcasView.putJfsFromCaddr(addr, fs);
  // return fs;
  // } catch (CASException e1) {
  // logAndThrow(e1, null);
  // return null;
  // }
  // }

  // private void logAndThrow(Exception e, JCasImpl jcasView) {
  // CASRuntimeException casEx = new CASRuntimeException(
  // CASRuntimeException.JCAS_CAS_MISMATCH,
  // new String[] { (null == jcasView) ? "-- ignore outer msg, error is can''t get value of jcas
  // from cas"
  // : (jcasView.getType(type).casType.getName() + "; exception= "
  // + e.getClass().getName() + "; msg= " + e.getLocalizedMessage()) });
  // casEx.initCause(e);
  // throw casEx;
  // }
  //
  // private int getSofaNbr(final int addr, final CASImpl casView) {
  // final int sofa = casView.ll_getIntValue(addr, annotSofaFeatCode, false);
  // return (sofa == 0) ? 0 : casView.ll_getIntValue(sofa, sofaNbrFeatCode);
  // }
  // }

  // per JCas instance - so don't need to synch.
  // private final Object[] constructorArgsFor_Type = new Object[2];

  // /**
  // * Make the instance of the JCas xxx_Type class for this CAS. Note: not all types will have
  // * xxx_Type. Instance creation does the typeSystemInit kind of function, as well.
  // *
  // * @param jcasTypeInfo -
  // * @param alreadyLoaded -
  // * @param fsGenerators updated by side effect with new instances of the _Type class
  // * @return true if a new instance of a _Type class was created
  // */
  // private <T extends TOP> boolean makeInstanceOf_Type(LoadedJCasType<T> jcasTypeInfo, boolean
  // alreadyLoaded,
  // FSGenerator<?>[] fsGenerators) {
  //
  // // return without doing anything if the _Type instance is already existing
  // // this happens when a JCas has some _Type instances made (e.g, the
  // // built-in ones) but the class loader was switched. Some of the
  // // _Type instances for the new class loader can share previously
  // // instantiated _Type instances, but others may be different
  // // (due to different impls of the _Type class loaded by the different
  // // class loader).
  // // This can also happen in the case where
  // // JCasImpl.getType is called for a non-existing class
  // // What happens in this case is that the getType code has to assume that
  // // perhaps none of the _Type instances were made for this JCas (yet), because
  // // these are created lazily - so it calls instantiateJCas_Types to make them.
  // // If they were already made, this next test short circuits this.
  // int typeIndex = jcasTypeInfo.index;
  // if (typeArray[typeIndex] != null) {
  // return false;
  // }
  //
  // Constructor<?> c_Type = jcasTypeInfo.constructorFor_Type;
  // Constructor<T> cType = jcasTypeInfo.constructorForType;
  // TypeImpl casType = (TypeImpl) casImpl.getTypeSystem().getType(jcasTypeInfo.typeName);
  //
  // try {
  // constructorArgsFor_Type[0] = this;
  // constructorArgsFor_Type[1] = casType;
  // TOP_Type x_Type_instance = (TOP_Type) c_Type.newInstance(constructorArgsFor_Type);
  // typeArray[typeIndex] = x_Type_instance;
  // // install the standard generator
  // // this is sharable by all views, since the CAS is passed to the generator
  // // Also sharable by all in a CasPool, except for "swapping" due to PEARs/Classloaders.
  // if (!alreadyLoaded) {
  // final TypeSystemImpl ts = casImpl.getTypeSystemImpl();
  // fsGenerators[casType.getCode()] = new JCasFsGenerator<T>(typeIndex, cType,
  // jcasTypeInfo.isSubtypeOfAnnotationBase, TypeSystemImpl.sofaNumFeatCode,
  // TypeSystemImpl.annotSofaFeatCode);
  // // this.casImpl.getFSClassRegistry().loadJCasGeneratorForType(typeIndex, cType, casType,
  // // jcasTypeInfo.isSubtypeOfAnnotationBase);
  // }
  // } catch (SecurityException e) {
  // logAndThrow(e);
  // } catch (InstantiationException e) {
  // logAndThrow(e);
  // } catch (IllegalAccessException e) {
  // logAndThrow(e);
  // } catch (InvocationTargetException e) {
  // logAndThrow(e);
  // } catch (ArrayIndexOutOfBoundsException e) {
  // logAndThrow(e);
  // }
  // return true;
  // }

  // /**
  // * Make the instance of the JCas xxx_Type class for this CAS. Note: not all types will have
  // * xxx_Type. Instance creation does the typeSystemInit kind of function, as well.
  // */
  // /*
  // * private void makeInstanceOf_Type(Type casType, Class clas, CASImpl cas) { Constructor c;
  // Field
  // * typeIndexField = null; int typeIndex; try { c = clas.getDeclaredConstructor(jcasBaseAndType);
  // * try {
  // *
  // * typeIndexField = clas.getDeclaredField("typeIndexID"); } catch (NoSuchFieldException e) { try
  // { //
  // * old version has the index in the base type String name = clas.getName(); Class clas2 =
  // * Class.forName(name.substring(0, name.length() - 5), true, cas .getJCasClassLoader()); // drop
  // * _Type typeIndexField = clas2.getDeclaredField("typeIndexID"); } catch (NoSuchFieldException
  // e2) {
  // * logAndThrow(e2); } catch (ClassNotFoundException e3) { logAndThrow(e3); } } typeIndex =
  // * typeIndexField.getInt(null); // null - static instance var TOP_Type x_Type_instance =
  // * (TOP_Type) c.newInstance(new Object[] { this, casType }); typeArray[typeIndex] =
  // * x_Type_instance; } catch (SecurityException e) { logAndThrow(e); } catch
  // (NoSuchMethodException
  // * e) { logAndThrow(e); } catch (InstantiationException e) { logAndThrow(e); } catch
  // * (IllegalAccessException e) { logAndThrow(e); } catch (InvocationTargetException e) {
  // * logAndThrow(e); } catch (ArrayIndexOutOfBoundsException e) { logAndThrow(e); } }
  // */

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getRequiredType(java.lang.String)
   */
  @Override
  public Type getRequiredType(String s) throws CASException {
    Type t = getTypeSystem().getType(s);
    if (null == t) {
      throw new CASException(CASException.JCAS_TYPENOTFOUND_ERROR, s);
    }
    return t;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getRequiredFeature(org.apache.uima.cas.Type, java.lang.String)
   */
  @Override
  public Feature getRequiredFeature(Type t, String s) throws CASException {
    Feature f = t.getFeatureByBaseName(s);
    if (null == f) {
      throw new CASException(CASException.JCAS_FEATURENOTFOUND_ERROR, t.getName(), s);
    }
    return f;
  }

  // /*
  // * (non-Javadoc)
  // *
  // * @see org.apache.uima.jcas.JCas#getRequiredFeatureDE(org.apache.uima.cas.Type,
  // java.lang.String,
  // * java.lang.String, boolean)
  // */
  //
  // public Feature getRequiredFeatureDE(Type t, String s, String rangeName, boolean featOkTst) {
  // Feature f = t.getFeatureByBaseName(s);
  // Type rangeType = this.getTypeSystem().getType(rangeName);
  // if (null == f && !featOkTst) {
  // CASException casEx = new CASException(CASException.JCAS_FEATURENOTFOUND_ERROR, t.getName(), s);
  // sharedView.errorSet.add(casEx);
  // }
  // if (null != f)
  // try {
  // casImpl.checkTypingConditions(t, rangeType, f);
  // } catch (LowLevelException e) {
  // CASException casEx = new CASException(CASException.JCAS_FEATURE_WRONG_TYPE, t.getName(), s,
  // rangeName, f.getRange());
  // sharedView.errorSet.add(casEx);
  // }
  // return f;
  // }

  // /**
  // * Internal - throw missing feature exception at runtime
  // *
  // * @param feat -
  // * @param type -
  // */
  // public void throwFeatMissing(String feat, String type) {
  // CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_FEAT,
  // new String[] { feat, type });
  // throw e;
  // }

  // /*
  // * (non-Javadoc)
  // *
  // * @see org.apache.uima.jcas.JCas#putJfsFromCaddr(int, org.apache.uima.cas.FeatureStructure)
  // */
  // public void putJfsFromCaddr(int casAddr, FeatureStructure fs) {
  // sharedView.cAddr2Jfs.put((FeatureStructureImpl) fs);
  // }

  // /*
  // * (non-Javadoc)
  // *
  // * Generics: extends FeatureStructure, not TOP, because
  // * when the JCas is being used, but a particular type instance doesn't have a JCas cover class,
  // * this holds instances of FeatureStructureC - the shared Class for non-JCas Java cover objects.
  // *
  // * @see org.apache.uima.jcas.JCas#getJfsFromCaddr(int)
  // */
  // @SuppressWarnings("unchecked")
  // public <T extends TOP> T getJfsFromCaddr(int casAddr) {
  // return (T) sharedView.cAddr2Jfs.getReserve(casAddr);
  // }

  // public void showJfsFromCaddrHistogram() {
  // sharedView.cAddr2Jfs.showHistogram();
  // }

  // * Implementation of part of the Cas interface as part of JCas*

  /*
   * (Internal Use only) called by the CAS reset function - clears the hashtable holding the
   * associations.
   */
  public static void clearData(CAS cas) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#reset()
   */
  @Override
  public void reset() {
    casImpl.reset();
  }

  // /*
  // * (non-Javadoc)
  // *
  // * @see org.apache.uima.jcas.JCas#checkArrayBounds(int, int)
  // */
  // public final void checkArrayBounds(int fsRef, int pos) {
  // if (NULL == fsRef) {
  // // note - need to add this to ll_runtimeException
  // throw new LowLevelException(LowLevelException.NULL_ARRAY_ACCESS, pos);
  // }
  // final int arrayLength = casImpl.ll_getArraySize(fsRef);
  // if (pos < 0 || pos >= arrayLength) {
  // throw new LowLevelException(LowLevelException.ARRAY_INDEX_OUT_OF_RANGE, pos);
  // }
  // }

  // *****************
  // * Sofa support *
  // *****************

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getSofa(org.apache.uima.cas.SofaID)
   */
  @Override
  public Sofa getSofa(SofaID sofaID) {
    return (Sofa) casImpl.getSofa(sofaID);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getSofa()
   */
  @Override
  public Sofa getSofa() {
    return (Sofa) casImpl.getSofa();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#createView(java.lang.String)
   */
  @Override
  public JCas createView(String sofaID) throws CASException {
    return casImpl.createView(sofaID).getJCas();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getJCas(org.apache.uima.jcas.cas.Sofa)
   */
  @Override
  public JCas getJCas(Sofa sofa) throws CASException {
    return casImpl.getView(sofa).getJCas();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getSofaIterator()
   */
  @Override
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
  @Override
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
  @Override
  public TOP getDocumentAnnotationFs() {
    return (TOP) casImpl.getDocumentAnnotation();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getDocumentText()
   */
  @Override
  public String getDocumentText() {
    return casImpl.getDocumentText();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getSofaDataString()
   */
  @Override
  public String getSofaDataString() {
    return casImpl.getSofaDataString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getSofaDataArray()
   */
  @Override
  public FeatureStructure getSofaDataArray() {
    return casImpl.getSofaDataArray();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getSofaDataURI()
   */
  @Override
  public String getSofaDataURI() {
    return casImpl.getSofaDataURI();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getSofaMimeType()
   */
  @Override
  public String getSofaMimeType() {
    return casImpl.getSofaMimeType();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#setDocumentText(java.lang.String)
   */
  @Override
  public void setDocumentText(String text) throws CASRuntimeException {
    casImpl.setDocumentText(text);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#setSofaDataString(java.lang.String, java.lang.String)
   */
  @Override
  public void setSofaDataString(String text, String mime) throws CASRuntimeException {
    casImpl.setSofaDataString(text, mime);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#setSofaDataArray(org.apache.uima.jcas.cas.TOP, java.lang.String)
   */
  @Override
  public void setSofaDataArray(FeatureStructure array, String mime) throws CASRuntimeException {
    casImpl.setSofaDataArray(array, mime);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#setSofaDataURI(java.lang.String, java.lang.String)
   */
  @Override
  public void setSofaDataURI(String uri, String mime) throws CASRuntimeException {
    casImpl.setSofaDataURI(uri, mime);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getDocumentLanguage()
   */
  @Override
  public String getDocumentLanguage() {
    return casImpl.getDocumentLanguage();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#setDocumentLanguage(java.lang.String)
   */
  @Override
  public void setDocumentLanguage(String language) throws CASRuntimeException {
    casImpl.setDocumentLanguage(language);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getSofaDataStream()
   */
  @Override
  public InputStream getSofaDataStream() {
    return casImpl.getSofaDataStream();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getConstraintFactory()
   */
  @Override
  public ConstraintFactory getConstraintFactory() {
    return casImpl.getConstraintFactory();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#createFeaturePath()
   */
  @Override
  public FeaturePath createFeaturePath() {
    return casImpl.createFeaturePath();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#createFilteredIterator(org.apache.uima.cas.FSIterator,
   * org.apache.uima.cas.FSMatchConstraint)
   */
  @Override
  public <T extends FeatureStructure> FSIterator<T> createFilteredIterator(FSIterator<T> it,
          FSMatchConstraint constraint) {
    return casImpl.createFilteredIterator(it, constraint);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getStringArray0L()
   * 
   * @deprecated use emptyXXXArray() instead
   */
  @Override
  @Deprecated
  public StringArray getStringArray0L() {
    return this.getCas().emptyStringArray();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getIntegerArray0L()
   * 
   * @deprecated use emptyXXXArray() instead
   */
  @Override
  @Deprecated
  public IntegerArray getIntegerArray0L() {
    return this.getCas().emptyIntegerArray();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getFloatArray0L()
   * 
   * @deprecated use emptyXXXArray() instead
   */
  @Override
  @Deprecated
  public FloatArray getFloatArray0L() {
    return this.getCas().emptyFloatArray();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getFSArray0L()
   * 
   * @deprecated use emptyXXXArray() instead
   */
  @Override
  @Deprecated
  public FSArray getFSArray0L() {
    return this.getCas().emptyFSArray();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#processInit()
   */
  @Override
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
  @Override
  public void setOwner(CasOwner aCasOwner) {
    casImpl.setOwner(aCasOwner);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#release()
   */
  @Override
  public void release() {
    casImpl.release();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getView(java.lang.String)
   */
  @Override
  public JCas getView(String localViewName) throws CASException {
    // try { // defer for release 3.0.2 because this change breaks test cases that test for specific
    // exceptions being thrown; revisit when 2nd digit bumps
    return casImpl.getView(localViewName).getJCas();
    // } catch (CASRuntimeException e) {
    // throw new CASException(e); // https://issues.apache.org/jira/browse/UIMA-5869
    // }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getView(org.apache.uima.cas.SofaFS)
   */
  @Override
  public JCas getView(SofaFS aSofa) throws CASException {
    return casImpl.getView(aSofa).getJCas();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#addFsToIndexes(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public void addFsToIndexes(FeatureStructure instance) {
    casImpl.addFsToIndexes(instance);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#removeFsFromIndexes(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public void removeFsFromIndexes(FeatureStructure instance) {
    casImpl.removeFsFromIndexes(instance);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#removeAllIncludingSubtypes(int)
   */
  @Override
  public void removeAllIncludingSubtypes(int i) {
    getFSIndexRepository().removeAllIncludingSubtypes(getCasType(i));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#removeAllExcludingSubtypes(int)
   */
  @Override
  public void removeAllExcludingSubtypes(int i) {
    getFSIndexRepository().removeAllExcludingSubtypes(getCasType(i));
  }

  /**
   * @see org.apache.uima.cas.CAS#fs2listIterator(FSIterator)
   */
  @Override
  public <T extends FeatureStructure> ListIterator<T> fs2listIterator(FSIterator<T> it) {
    return casImpl.fs2listIterator(it);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.BaseCas#createFeatureValuePath(java.lang.String)
   */
  @Override
  public FeatureValuePath createFeatureValuePath(String featureValuePath)
          throws CASRuntimeException {
    return casImpl.createFeatureValuePath(featureValuePath);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.BaseCas#createSofa(org.apache.uima.cas.SofaID, java.lang.String)
   * 
   * @deprecated
   */
  @Override
  public SofaFS createSofa(SofaID sofaID, String mimeType) {
    // extract absolute SofaName string from the ID
    return casImpl.createSofa(sofaID, mimeType);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.BaseCas#getIndexRepository()
   */
  @Override
  public FSIndexRepository getIndexRepository() {
    return casImpl.getIndexRepository();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.BaseCas#getViewName()
   */
  @Override
  public String getViewName() {
    return casImpl.getViewName();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.BaseCas#size()
   */
  @Override
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
  @Override
  public AnnotationIndex<Annotation> getAnnotationIndex() {
    return casImpl.<Annotation> getAnnotationIndex();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getAnnotationIndex(org.apache.uima.cas.Type)
   */
  @Override
  public <T extends Annotation> AnnotationIndex<T> getAnnotationIndex(Type type)
          throws CASRuntimeException {
    return (AnnotationIndex<T>) casImpl.<T> getAnnotationIndex(type);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getAnnotationIndex(int)
   */
  @Override
  public <T extends Annotation> AnnotationIndex<T> getAnnotationIndex(int type)
          throws CASRuntimeException {
    return (AnnotationIndex<T>) casImpl.<T> getAnnotationIndex(this.getCasType(type));
  }

  @Override
  public <T extends Annotation> AnnotationIndex<T> getAnnotationIndex(Class<T> clazz) {
    return getAnnotationIndex(getCasType(clazz));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#getViewIterator()
   */
  @Override
  public Iterator<JCas> getViewIterator() throws CASException {
    List<JCas> viewList = new ArrayList<>();
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
  @Override
  public Iterator<JCas> getViewIterator(String localViewNamePrefix) throws CASException {
    List<JCas> viewList = new ArrayList<>();
    Iterator<CAS> casViewIter = casImpl.getViewIterator(localViewNamePrefix);
    while (casViewIter.hasNext()) {
      viewList.add((casViewIter.next()).getJCas());
    }
    return viewList.iterator();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#protectIndexes()
   */
  @Override
  public AutoCloseable protectIndexes() {
    return casImpl.protectIndexes();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JCas#protectIndexes(java.lang.Runnable)
   */
  @Override
  public void protectIndexes(Runnable runnable) {
    casImpl.protectIndexes(runnable);
  }

  /**
   * Static method to get the corresponding Type for a JCas class object
   */
  private static int getTypeRegistryIndex(Class<? extends FeatureStructure> clazz) {
    try {
      return clazz.getField("type").getInt(clazz);
    } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
            | SecurityException e) {
      throw new RuntimeException(e); // should never happen
    }
  }

  /**
   * Return the UIMA Type object corresponding to this JCas's JCas cover class (Note: different
   * JCas's, with different type systems, may share the same cover class impl)
   * 
   * @param clazz
   *          a JCas cover class
   * @return the corresponding UIMA Type object
   */
  @Override
  public Type getCasType(Class<? extends FeatureStructure> clazz) {
    return getCasType(getTypeRegistryIndex(clazz));
  }

  @Override
  public <T extends TOP> FSIterator<T> getAllIndexedFS(Class<T> clazz) {
    return getFSIndexRepository().getAllIndexedFS(getCasType(clazz));
  }

  @Override
  public <T extends TOP> FSIndex<T> getIndex(String label, Class<T> clazz) {
    return getFSIndexRepository().getIndex(label, getCasType(clazz));
  }
}
