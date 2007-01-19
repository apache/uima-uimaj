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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import org.apache.uima.cas.AbstractCas_ImplBase;
import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.CasOwner;
import org.apache.uima.cas.ComponentInfo;
import org.apache.uima.cas.ConstraintFactory;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeaturePath;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FeatureValuePath;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.SofaID;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.cas.text.Language;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.impl.JCasImpl;

/**
 * Implements the CAS interfaces. This class must be public because we need to be able to create
 * instance of it from outside the package. Use at your own risk. May change without notice.
 * 
 */
public class CASImpl extends AbstractCas_ImplBase implements CAS, CASMgr, LowLevelCAS {

  // Notes on the implementation
  // ---------------------------

  // Floats are handled by casting them to ints when they are stored
  // in the heap. Conveniently, 0 casts to 0.0f, which is the default
  // value.

  public static final int NULL = 0;

  // Boolean scalar values are stored as ints in the fs heap.
  // TRUE is 1 and false is 0.
  public static final int TRUE = 1;

  public static final int FALSE = 0;

  public static final int DEFAULT_INITIAL_HEAP_SIZE = 500000;

  public static final int DEFAULT_RESET_HEAP_SIZE = 5000000;

  // this next seemingly non-sensical static block
  // is to force the classes needed by Eclipse debugging to load
  // otherwise, you get a com.sun.jdi.ClassNotLoadedException when
  // the class is used as part of formatting debugging messages
  static {
    new DebugNameValuePair(null, null);
    new DebugFSLogicalStructure();
  }

  private final int resetHeapSize = DEFAULT_RESET_HEAP_SIZE;

  protected TypeSystemImpl ts;

  // The offset for the array length cell. An array consists of length+2
  // number
  // of cells, where the first cell contains the type, the second one the
  // length,
  // and the rest the actual content of the array.
  private static final int arrayLengthFeatOffset = 1;

  // The number of cells we need to skip to get to the array contents. That
  // is,
  // if we have an array starting at addr, the first cell is at
  // addr+arrayContentOffset.
  private static final int arrayContentOffset = 2;

  protected Heap heap;

  // private SymbolTable stringTable;
  // private ArrayList stringList;
  protected StringHeap stringHeap;

  protected ByteHeap byteHeap; // for storing 8 bit values

  protected ShortHeap shortHeap; // for storing 16 bit values

  protected LongHeap longHeap; // for storing 64 bit values

  // The document text.
  private String documentText;

  // The index repository.
  protected FSIndexRepositoryImpl indexRepository;

  // A map from Sofas to IndexRepositories.
  private HashMap sofa2indexMap;

  // A map from Sofas to CAS views.
  private HashMap sofa2tcasMap;

  // A map from Sofas to JCas views.
  private HashMap sofa2jcasMap;

  // Count of Sofa created in this cas
  protected HashSet sofaNameSet;

  // Flag that initial Sofa has been created
  private boolean initialSofaCreated = false;

  // set of instantiated sofaNames
  private int sofaCount;

  // Base CAS for all views
  protected CASImpl baseCAS;

  // the sofaFS this view is based on
  // SofaFS mySofa;
  protected int mySofaRef = 0;

  // FS registry
  protected FSClassRegistry fsClassReg = null;

  private final boolean useFSCache;

  private static final boolean DEFAULT_USE_FS_CACHE = false;

  // The ClassLoader that should be used by the JCas to load the generated
  // FS cover classes for this CAS. Defaults to the ClassLoader used
  // to load the CASImpl class.
  private ClassLoader jcasClassLoader = this.getClass().getClassLoader();

  // ///////////////////////////////////////////////////////
  // Data structures for type checking and feature encoding

  // For each feature, what the offset from the start of the FS is.
  // That is, this will always be a number > 0. If you have the
  // address a of a structure of type t, then you can get the value of
  // feature f by getting (the value of) a+featureOffset[f] from the
  // heap. If f is not appropriate for t, anything can happen
  // (including an ArrayIndexOutOfBoundsException).
  private int[] featureOffset;

  // For each type, how large structures of that type are. This will
  // also be > 0 for each type (since you need to store the type at a
  // minimum.
  private int[] fsSpaceReq;

  // For each type, remember if it's a regular type that can be created
  // with CAS.createFS() or not. Exceptions are built-in types float, int and
  // string, as well as arrays.
  private boolean[] creatableType;

  // ///////////////////////////////////////////////////////
  // Properties of types.

  // Those types can not be created with CAS.createFS().
  private static String[] nonCreatableTypes = { CAS.TYPE_NAME_INTEGER, CAS.TYPE_NAME_FLOAT,
      CAS.TYPE_NAME_STRING, CAS.TYPE_NAME_ARRAY_BASE, CAS.TYPE_NAME_FS_ARRAY,
      CAS.TYPE_NAME_INTEGER_ARRAY, CAS.TYPE_NAME_FLOAT_ARRAY, CAS.TYPE_NAME_STRING_ARRAY,
      CAS.TYPE_NAME_SOFA, CAS.TYPE_NAME_BYTE, CAS.TYPE_NAME_BYTE_ARRAY, CAS.TYPE_NAME_BOOLEAN,
      CAS.TYPE_NAME_BOOLEAN_ARRAY, CAS.TYPE_NAME_SHORT, CAS.TYPE_NAME_SHORT_ARRAY,
      CAS.TYPE_NAME_LONG, CAS.TYPE_NAME_LONG_ARRAY, CAS.TYPE_NAME_DOUBLE,
      CAS.TYPE_NAME_DOUBLE_ARRAY };

  // References to built-in types.
  private TypeImpl topType;

  private TypeImpl intType;

  private TypeImpl stringType;

  private TypeImpl floatType;

  private TypeImpl arrayBaseType;

  private TypeImpl intArrayType;

  private TypeImpl floatArrayType;

  private TypeImpl stringArrayType;

  private TypeImpl fsArrayType;

  private TypeImpl sofaType;

  private TypeImpl annotType;

  private TypeImpl annotBaseType;

  private TypeImpl docType;

  private FeatureImpl startFeat;

  private FeatureImpl endFeat;

  private FeatureImpl langFeat;

  private TypeImpl byteType;

  private TypeImpl byteArrayType;

  private TypeImpl booleanType;

  private TypeImpl booleanArrayType;

  private TypeImpl shortType;

  private TypeImpl shortArrayType;

  private TypeImpl longType;

  private TypeImpl longArrayType;

  private TypeImpl doubleType;

  private TypeImpl doubleArrayType;

  // private int topTypeCode;
  private int intTypeCode;

  private int stringTypeCode;

  private int floatTypeCode;

  // private int arrayBaseTypeCode;
  private int intArrayTypeCode;

  private int floatArrayTypeCode;

  private int stringArrayTypeCode;

  private int fsArrayTypeCode;

  private int sofaTypeCode;

  protected int annotTypeCode;

  private int annotBaseTypeCode;

  private int byteTypeCode;

  private int booleanTypeCode;

  private int shortTypeCode;

  private int longTypeCode;

  private int doubleTypeCode;

  private int byteArrayTypeCode;

  private int booleanArrayTypeCode;

  private int shortArrayTypeCode;

  private int longArrayTypeCode;

  private int doubleArrayTypeCode;

  private int sofaNumFeatCode;

  private int sofaIdFeatCode;

  private int sofaMimeFeatCode;

  private int sofaUriFeatCode;

  private int sofaArrayFeatCode;

  int annotSofaFeatCode;

  int startFeatCode;

  int endFeatCode;

  protected int langFeatCode;

  // If this CAS can be flushed or not.
  private boolean flushEnabled = true;

  private boolean annotIndexInitialized = false;

  protected JCas jcas = null;

  private ComponentInfo componentInfo;

  private final ArrayList getStringList() {
    ArrayList stringList = new ArrayList();
    stringList.add(null);
    int pos = this.stringHeap.getLeastStringCode();
    final int end = this.stringHeap.getLargestStringCode();
    while (pos <= end) {
      stringList.add(this.stringHeap.getStringForCode(pos));
      ++pos;
    }
    return stringList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.admin.CASMgr#setCAS(org.apache.uima.cas.CAS)
   */
  public void setCAS(CAS cas) {
    CASImpl in = (CASImpl) cas;
    this.baseCAS = in;
    this.ts = in.ts;
    this.heap = in.heap;
    this.stringHeap = in.stringHeap;
    this.byteHeap = in.byteHeap;
    this.shortHeap = in.shortHeap;
    this.longHeap = in.longHeap;

    this.indexRepository = in.indexRepository;
    this.sofa2indexMap = in.sofa2indexMap;
    this.sofa2tcasMap = in.sofa2tcasMap;
    this.sofa2jcasMap = in.sofa2jcasMap;
    this.sofaNameSet = in.sofaNameSet;
    this.fsClassReg = in.fsClassReg;
    this.featureOffset = in.featureOffset;
    this.fsSpaceReq = in.fsSpaceReq;
    this.creatableType = in.creatableType;
    this.topType = in.topType;
    this.intType = in.intType;
    this.stringType = in.stringType;
    this.floatType = in.floatType;
    this.arrayBaseType = in.arrayBaseType;
    this.intArrayType = in.intArrayType;
    this.floatArrayType = in.floatArrayType;
    this.stringArrayType = in.stringArrayType;
    this.fsArrayType = in.fsArrayType;
    this.annotType = in.annotType;
    this.annotBaseType = in.annotBaseType;
    this.sofaType = in.sofaType;
    this.docType = in.docType;

    this.byteType = in.byteType;
    this.byteArrayType = in.byteArrayType;
    this.booleanType = in.booleanType;
    this.booleanArrayType = in.booleanArrayType;
    this.shortType = in.shortType;
    this.shortArrayType = in.shortArrayType;
    this.longType = in.longType;
    this.longArrayType = in.longArrayType;
    this.doubleType = in.doubleType;
    this.doubleArrayType = in.doubleArrayType;

    this.flushEnabled = in.flushEnabled;
    initTypeCodeVars();
    // this.jcas = in.jcas;
  }

  // CASImpl(TypeSystemImpl typeSystem) {
  // this(typeSystem, DEFAULT_INITIAL_HEAP_SIZE);
  // }

//  // Reference existing CAS
//  // For use when creating views of the CAS
//  CASImpl(CAS cas) {
//    this.setCAS(cas);
//    this.useFSCache = false;
//    initTypeVariables();
//  }

  public CASImpl(TypeSystemImpl typeSystem, int initialHeapSize) {
    this(typeSystem, initialHeapSize, DEFAULT_USE_FS_CACHE);
  }

  CASImpl(TypeSystemImpl typeSystem, int initialHeapSize, boolean useFSCache) {
    super();
    this.useFSCache = useFSCache;
    final boolean externalTypeSystem = (typeSystem != null);
    if (externalTypeSystem) {
      this.ts = typeSystem;
      redoTypeSystemSetup();
      this.ts.setCommitted(true);
    } else {
      this.ts = new TypeSystemImpl();
      setupTSDefault();
    }
    this.initTypeVariables();
    this.heap = new Heap(initialHeapSize);
    initStringTable();

    // initial size 16
    this.byteHeap = new ByteHeap();
    this.shortHeap = new ShortHeap();
    this.longHeap = new LongHeap();

    if (externalTypeSystem) {
      commitTypeSystem();
    }
    this.sofa2indexMap = new HashMap();
    this.sofa2tcasMap = new HashMap();
    this.sofa2jcasMap = new HashMap();
    this.sofaNameSet = new HashSet();
    this.initialSofaCreated = false;
    this.sofaCount = 0;
    this.baseCAS = this;
  }

  /**
   * Constructor. Use only if you want to use the low-level APIs.
   */
  public CASImpl() {
    this(DEFAULT_INITIAL_HEAP_SIZE);
  }

  public CASImpl(int initialHeapSize) {
    this((TypeSystemImpl) null, initialHeapSize);
  }

  public CASImpl(CASMgrSerializer ser) {
    this();
    this.ts = ser.getTypeSystem();
    commitTypeSystem();
    this.initTypeVariables();
    redoTypeSystemSetup();
    checkInternalCodes(ser);
    // assert(ts != null);
    // assert(getTypeSystem() != null);
    this.setIndexRepository(ser.getIndexRepository(this));
  }

  // Use this when creating a CAS view
  CASImpl(CAS cas, SofaFS aSofa) {

    this.setCAS(cas);
    this.useFSCache = false;
    initTypeVariables();
    
    // this.mySofa = aSofa;
    if (aSofa != null) {
      // save address of SofaFS
      this.mySofaRef = aSofa.hashCode();
    } else {
      // this is the InitialView
      this.mySofaRef = -1;
    }

    initFSClassRegistry();

    // get the indexRepository for this Sofa
    this.indexRepository = (this.mySofaRef == -1) ? (FSIndexRepositoryImpl) ((CASImpl) cas)
            .getSofaIndexRepository(1) : (FSIndexRepositoryImpl) ((CASImpl) cas)
            .getSofaIndexRepository(aSofa);
    if (null == this.indexRepository) {
      // create the indexRepository for this CAS
      // use the baseIR to create a lightweight IR copy
      this.indexRepository = new FSIndexRepositoryImpl(this,
              (FSIndexRepositoryImpl) ((CASImpl) cas).getBaseIndexRepository());
      this.indexRepository.commit();
      // save new sofa index
      if (this.mySofaRef == -1) {
        ((CASImpl) cas).setSofaIndexRepository(1, this.indexRepository);
      } else {
        ((CASImpl) cas).setSofaIndexRepository(aSofa, this.indexRepository);
      }
    }
  }

  // Use this when creating a CAS view
  protected void refreshView(CAS cas, SofaFS aSofa) {
    this.setCAS(cas);
    if (aSofa != null) {
      // save address of SofaFS
      this.mySofaRef = aSofa.hashCode();
    } else {
      // this is the InitialView
      this.mySofaRef = -1;
    }

    // toss the JCas, if it exists
    this.jcas = null;

    // create the indexRepository for this Sofa
    this.indexRepository = new FSIndexRepositoryImpl(this,
            (FSIndexRepositoryImpl) ((CASImpl) cas).getBaseIndexRepository());
    this.indexRepository.commit();
    // save new sofa index
    if (this.mySofaRef == -1) {
      ((CASImpl) cas).setSofaIndexRepository(1, this.indexRepository);
    } else {
      ((CASImpl) cas).setSofaIndexRepository(aSofa, this.indexRepository);
    }
  }

  private final void initTypeVariables() {
    // Type objects.
    this.topType = (TypeImpl) this.ts.getTopType();
    this.intType = (TypeImpl) this.ts.getType(TYPE_NAME_INTEGER);
    this.stringType = (TypeImpl) this.ts.getType(TYPE_NAME_STRING);
    this.floatType = (TypeImpl) this.ts.getType(TYPE_NAME_FLOAT);
    this.arrayBaseType = (TypeImpl) this.ts.getType(TYPE_NAME_ARRAY_BASE);
    this.intArrayType = (TypeImpl) this.ts.getType(TYPE_NAME_INTEGER_ARRAY);
    this.floatArrayType = (TypeImpl) this.ts.getType(TYPE_NAME_FLOAT_ARRAY);
    this.stringArrayType = (TypeImpl) this.ts.getType(TYPE_NAME_STRING_ARRAY);
    this.fsArrayType = (TypeImpl) this.ts.getType(TYPE_NAME_FS_ARRAY);
    this.sofaType = (TypeImpl) this.ts.getType(TYPE_NAME_SOFA);
    this.annotType = (TypeImpl) this.ts.getType(CAS.TYPE_NAME_ANNOTATION);
    this.annotBaseType = (TypeImpl) this.ts.getType(CAS.TYPE_NAME_ANNOTATION_BASE);
    this.startFeat = (FeatureImpl) this.ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_BEGIN);
    this.endFeat = (FeatureImpl) this.ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_END);
    this.langFeat = (FeatureImpl) this.ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_LANGUAGE);
    this.docType = (TypeImpl) this.ts.getType(CAS.TYPE_NAME_DOCUMENT_ANNOTATION);

    this.byteType = (TypeImpl) this.ts.getType(TYPE_NAME_BYTE);
    this.byteArrayType = (TypeImpl) this.ts.getType(TYPE_NAME_BYTE_ARRAY);
    this.booleanType = (TypeImpl) this.ts.getType(TYPE_NAME_BOOLEAN);
    this.booleanArrayType = (TypeImpl) this.ts.getType(TYPE_NAME_BOOLEAN_ARRAY);
    this.shortType = (TypeImpl) this.ts.getType(TYPE_NAME_SHORT);
    this.shortArrayType = (TypeImpl) this.ts.getType(TYPE_NAME_SHORT_ARRAY);
    this.longType = (TypeImpl) this.ts.getType(TYPE_NAME_LONG);
    this.longArrayType = (TypeImpl) this.ts.getType(TYPE_NAME_LONG_ARRAY);
    this.doubleType = (TypeImpl) this.ts.getType(TYPE_NAME_DOUBLE);
    this.doubleArrayType = (TypeImpl) this.ts.getType(TYPE_NAME_DOUBLE_ARRAY);

    // Type codes.
    initTypeCodeVars();
  }

  private final void initTypeCodeVars() {
    this.intTypeCode = this.intType.getCode();
    this.stringTypeCode = this.stringType.getCode();
    this.floatTypeCode = this.floatType.getCode();
    // this.arrayBaseTypeCode = arrayBaseType.getCode();
    this.intArrayTypeCode = this.intArrayType.getCode();
    this.floatArrayTypeCode = this.floatArrayType.getCode();
    this.stringArrayTypeCode = this.stringArrayType.getCode();
    this.fsArrayTypeCode = this.fsArrayType.getCode();
    this.sofaTypeCode = this.sofaType.getCode();
    this.annotTypeCode = this.annotType.getCode();
    this.annotBaseTypeCode = this.annotBaseType.getCode();

    this.byteArrayTypeCode = this.byteArrayType.getCode();
    this.byteTypeCode = this.byteType.getCode();
    this.booleanTypeCode = this.booleanType.getCode();
    this.booleanArrayTypeCode = this.booleanArrayType.getCode();
    this.shortTypeCode = this.shortType.getCode();
    this.shortArrayTypeCode = this.shortArrayType.getCode();
    this.longTypeCode = this.longType.getCode();
    this.longArrayTypeCode = this.longArrayType.getCode();
    this.doubleTypeCode = this.doubleType.getCode();
    this.doubleArrayTypeCode = this.doubleArrayType.getCode();

    this.sofaNumFeatCode = ll_getTypeSystem().ll_getCodeForFeature(
            this.sofaType.getFeatureByBaseName(FEATURE_BASE_NAME_SOFANUM));
    this.sofaIdFeatCode = ll_getTypeSystem().ll_getCodeForFeature(
            this.sofaType.getFeatureByBaseName(FEATURE_BASE_NAME_SOFAID));
    this.sofaMimeFeatCode = ll_getTypeSystem().ll_getCodeForFeature(
            this.sofaType.getFeatureByBaseName(FEATURE_BASE_NAME_SOFAMIME));
    this.sofaUriFeatCode = ll_getTypeSystem().ll_getCodeForFeature(
            this.sofaType.getFeatureByBaseName(FEATURE_BASE_NAME_SOFAURI));
    this.sofaArrayFeatCode = ll_getTypeSystem().ll_getCodeForFeature(
            this.sofaType.getFeatureByBaseName(FEATURE_BASE_NAME_SOFAARRAY));
    this.annotSofaFeatCode = ll_getTypeSystem().ll_getCodeForFeature(
            this.annotBaseType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_SOFA));
    this.startFeatCode = ll_getTypeSystem().ll_getCodeForFeature(
            this.annotType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_BEGIN));
    this.endFeatCode = ll_getTypeSystem().ll_getCodeForFeature(
            this.annotType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_END));
    this.langFeatCode = ll_getTypeSystem().ll_getCodeForFeature(
            this.docType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_LANGUAGE));
  }

  private void checkInternalCodes(CASMgrSerializer ser) throws CASAdminException {
    if ((ser.topTypeCode > 0) && (ser.topTypeCode != ((TypeImpl) this.ts.getTopType()).getCode())) {
      throw new CASAdminException(CASAdminException.DESERIALIZATION_ERROR);
    }
    if (ser.featureOffsets == null) {
      return;
    }
    if (ser.featureOffsets.length != this.featureOffset.length) {
      throw new CASAdminException(CASAdminException.DESERIALIZATION_ERROR);
    }
    for (int i = 1; i < ser.featureOffsets.length; i++) {
      if (ser.featureOffsets[i] != this.featureOffset[i]) {
        throw new CASAdminException(CASAdminException.DESERIALIZATION_ERROR);
      }
    }
  }

  public void enableReset(boolean flag) {
    this.flushEnabled = flag;
  }

  public TypeSystem getTypeSystem() {
    if (this.ts.isCommitted()) {
      return this.ts;
    }
    throw new CASRuntimeException(CASRuntimeException.TYPESYSTEM_NOT_LOCKED);
  }

  public ConstraintFactory getConstraintFactory() {
    return ConstraintFactory.instance();
  }

  public FeatureStructure createFS(Type type) {
    final int typeCode = ((TypeImpl) type).getCode();
    if (!isCreatableType(typeCode)) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.NON_CREATABLE_TYPE);
      e.addArgument(type.getName());
      e.addArgument("CAS.createFS()");
      throw e;
    }
    final int addr = createTempFS(typeCode);
    final boolean isAnnot = this.ts.subsumes(this.annotBaseTypeCode, typeCode);
    if (isAnnot && this == this.getBaseCAS()) {
      // Can't create annotation on base CAS
      // TODO add new runtime exception for this problem
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.NON_CREATABLE_TYPE);
      e.addArgument(type.getName());
      e.addArgument("CAS.createFS()");
      throw e;
    }
    final FeatureStructure newFS = this.fsClassReg.createFS(addr, this);
    if (isAnnot) {
      final int llsofa = getLowLevelCAS().ll_getFSRef(newFS);
      getLowLevelCAS().ll_setIntValue(llsofa, this.annotSofaFeatCode, this.getSofaRef());
    }
    return newFS;
  }

  // public FeatureStructure createPermFS(Type type) {
  // final int addr = createPermFS(((TypeImpl) type).getCode());
  // return getFSClassRegistry().createFS(addr, this);
  // }

  public ArrayFS createArrayFS(int length) {
    checkArrayPreconditions(length);
    final int addr = createTempArray(this.fsArrayType.getCode(), length);
    return (ArrayFS) createFS(addr);
  }

  public IntArrayFS createIntArrayFS(int length) {
    checkArrayPreconditions(length);
    final int addr = createTempArray(this.intArrayType.getCode(), length);
    return (IntArrayFS) createFS(addr);
  }

  public FloatArrayFS createFloatArrayFS(int length) {
    checkArrayPreconditions(length);
    final int addr = createTempArray(this.floatArrayType.getCode(), length);
    return (FloatArrayFS) createFS(addr);
  }

  public StringArrayFS createStringArrayFS(int length) {
    checkArrayPreconditions(length);
    final int addr = createTempArray(this.stringArrayType.getCode(), length);
    return (StringArrayFS) createFS(addr);
  }

  public final void checkArrayPreconditions(int len) throws CASRuntimeException {
    // Check array size.
    if (len < 0) {
      throw new CASRuntimeException(CASRuntimeException.ILLEGAL_ARRAY_SIZE);
    }
  }

  // return true if only one sofa and it is the default text sofa
  public boolean isBackwardCompatibleCas() {
    // check that there is exactly one sofa
    if (this.baseCAS.sofaCount != 1) {
      return false;
    }

    if (!this.baseCAS.initialSofaCreated) {
      return false;
    }
    final int llsofa = getLowLevelCAS().ll_getFSRef(this.getInitialView().getSofa());

    // check for mime type exactly equal to "text"
    String sofaMime = getLowLevelCAS().ll_getStringValue(llsofa, this.sofaMimeFeatCode);
    if (!"text".equals(sofaMime))
      return false;
    // check that sofaURI and sofaArray are not set
    String sofaUri = getLowLevelCAS().ll_getStringValue(llsofa, this.sofaUriFeatCode);
    if (sofaUri != null)
      return false;
    int sofaArray = getLowLevelCAS().ll_getRefValue(llsofa, this.sofaArrayFeatCode);
    if (sofaArray != CASImpl.NULL)
      return false;
    // check that name is NAME_DEFAULT_SOFA
    String sofaname = getLowLevelCAS().ll_getStringValue(llsofa, this.sofaIdFeatCode);
    return NAME_DEFAULT_SOFA.equals(sofaname);
  }

  protected int getBaseSofaCount() {
    return this.baseCAS.sofaCount;
  }

  protected FSIndexRepository getSofaIndexRepository(SofaFS aSofa) {
    return getSofaIndexRepository(aSofa.getSofaRef());
  }

  protected FSIndexRepository getSofaIndexRepository(int aSofaRef) {
    return (FSIndexRepositoryImpl) this.sofa2indexMap.get(new Integer(aSofaRef));
  }

  protected void setSofaIndexRepository(SofaFS aSofa, FSIndexRepository indxRepos) {
    setSofaIndexRepository(aSofa.getSofaRef(), indxRepos);
  }

  protected void setSofaIndexRepository(int aSofaRef, FSIndexRepository indxRepos) {
    this.sofa2indexMap.put(new Integer(aSofaRef), indxRepos);
  }

  /**
   * @deprecated
   */
  public SofaFS createSofa(SofaID sofaID, String mimeType) {
    // extract absolute SofaName string from the ID
    return createSofa(sofaID.getSofaID(), mimeType);
  }

  protected SofaFS createSofa(String sofaName, String mimeType) {
    final int addr = createTempFS(this.sofaTypeCode);
    final FeatureStructure sofa = this.fsClassReg.createFS(addr, this);
    addSofa(sofa, sofaName, mimeType);
    return (SofaFS) sofa;
  }

  protected SofaFS createInitialSofa(String mimeType) {
    final int addr = createTempFS(this.sofaTypeCode);
    final FeatureStructure sofa = this.fsClassReg.createFS(addr, this);
    final int llsofa = getLowLevelCAS().ll_getFSRef(sofa);
    getLowLevelCAS().ll_setIntValue(llsofa, this.sofaNumFeatCode, 1);
    addSofa(sofa, CAS.NAME_DEFAULT_SOFA, mimeType);
    registerInitialSofa();
    this.mySofaRef = sofa.hashCode();
    return (SofaFS) sofa;
  }

  protected void registerInitialSofa() {
    this.baseCAS.initialSofaCreated = true;
  }

  protected boolean isInitialSofaCreated() {
    return this.baseCAS.initialSofaCreated;
  }

  // Internal use only
  public void addSofa(FeatureStructure sofa, String sofaName, String mimeType) {
    if (this.baseCAS.sofaNameSet.contains(sofaName)) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFANAME_ALREADY_EXISTS);
      e.addArgument(sofaName);
      throw e;
    }
    final int llsofa = getLowLevelCAS().ll_getFSRef(sofa);
    if (0 == getLowLevelCAS().ll_getIntValue(llsofa, this.sofaNumFeatCode)) {
      getLowLevelCAS().ll_setIntValue(llsofa, this.sofaNumFeatCode, ++this.baseCAS.sofaCount);
    }
    getLowLevelCAS().ll_setStringValue(llsofa, this.sofaIdFeatCode, sofaName);
    getLowLevelCAS().ll_setStringValue(llsofa, this.sofaMimeFeatCode, mimeType);
    this.getBaseIndexRepository().addFS(sofa);
    this.baseCAS.sofaNameSet.add(sofaName);
  }

  /**
   * @deprecated
   */
  public SofaFS getSofa(SofaID sofaID) {
    // extract absolute SofaName string from the ID
    return getSofa(sofaID.getSofaID());
  }

  private SofaFS getSofa(String sofaName) {
    FSIterator iterator = this.baseCAS.getSofaIterator();
    while (iterator.isValid()) {
      SofaFS sofa = (SofaFS) iterator.get();
      if (sofaName.equals(getStringValue(sofa.hashCode(), this.sofaIdFeatCode))) {
        return sofa;
      }
      iterator.moveToNext();
    }
    CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFANAME_NOT_FOUND);
    e.addArgument(sofaName);
    throw e;
  }

  protected SofaFS getSofa(int sofaRef) {
    SofaFS aSofa = (SofaFS) this.ll_getFSForRef(sofaRef);
    if (aSofa == null) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFAREF_NOT_FOUND);
      throw e;
    }
    return aSofa;
  }

  public CASImpl getBaseCAS() {
    return this.baseCAS;
  }

  public FSIterator getSofaIterator() {
    FSIndex sofaIndex = this.baseCAS.indexRepository.getIndex(CAS.SOFA_INDEX_NAME);
    return sofaIndex.iterator();
  }

  // For internal use only
  public void setSofaFeat(int addr, int sofa) {
    setFeatureValue(addr, this.annotSofaFeatCode, sofa);
  }

  // For internal use only
  public int getSofaFeat(int addr) {
    return getFeatureValue(addr, this.annotSofaFeatCode);
  }

  // For internal use only
  public int getSofaRef() {
    if (this.mySofaRef == -1) {
      // create the SofaFS for _InitialView ...
      // ... and reset mySofaRef to point to it
      this.mySofaRef = this.createInitialSofa(null).hashCode();
    }
    return this.mySofaRef;
  }

  // For internal use only
  public InputStream getSofaDataStream(SofaFS aSofa) {
    try {

      if (null != aSofa.getLocalStringData()) {
        ByteArrayInputStream bis = new ByteArrayInputStream(aSofa.getLocalStringData().getBytes(
                "UTF-8"));
        return bis;
      } else if (null != aSofa.getLocalFSData()) {
        FeatureStructureImpl fs = (FeatureStructureImpl) aSofa.getLocalFSData();

        int arrayStart = 0;
        int arraySize = this.getArraySize(fs.getAddress());
        ByteBuffer buf = null;
        Type type = fs.getType();
        if (type.getName().equals(CAS.TYPE_NAME_INTEGER_ARRAY)) {
          arrayStart = getArrayStartAddress(fs.getAddress());
          buf = ByteBuffer.allocate(arraySize * 4);
          IntBuffer intbuf = buf.asIntBuffer();
          intbuf.put(this.heap.heap, arrayStart, arraySize);
          ByteArrayInputStream bis = new ByteArrayInputStream(buf.array());
          return bis;
        } else if (type.getName().equals(CAS.TYPE_NAME_FLOAT_ARRAY)) {
          arrayStart = getArrayStartAddress(fs.getAddress());
          buf = ByteBuffer.allocate(arraySize * 4);
          FloatBuffer floatbuf = buf.asFloatBuffer();
          float[] floatArray = new float[arraySize];
          for (int i = arrayStart; i < arrayStart + arraySize; i++) {
            floatArray[i - arrayStart] = Float.intBitsToFloat(this.heap.heap[i]);
          }
          floatbuf.put(floatArray);
          ByteArrayInputStream bis = new ByteArrayInputStream(buf.array());
          return bis;
        } else if (type.getName().equals(CAS.TYPE_NAME_BOOLEAN_ARRAY)
                || type.getName().equals(CAS.TYPE_NAME_BYTE_ARRAY)) {
          arrayStart = this.heap.heap[getArrayStartAddress(fs.getAddress())];
          buf = ByteBuffer.allocate(arraySize);
          buf.put(this.byteHeap.heap, arrayStart, arraySize);
          ByteArrayInputStream bis = new ByteArrayInputStream(buf.array());
          return bis;
        } else if (type.getName().equals(CAS.TYPE_NAME_SHORT_ARRAY)) {
          arrayStart = this.heap.heap[getArrayStartAddress(fs.getAddress())];
          buf = ByteBuffer.allocate(arraySize * 2);
          ShortBuffer shortbuf = buf.asShortBuffer();
          shortbuf.put(this.shortHeap.heap, arrayStart, arraySize);

          ByteArrayInputStream bis = new ByteArrayInputStream(buf.array());
          return bis;
        } else if (type.getName().equals(CAS.TYPE_NAME_LONG_ARRAY)) {
          arrayStart = this.heap.heap[getArrayStartAddress(fs.getAddress())];
          buf = ByteBuffer.allocate(arraySize * 8);
          LongBuffer longbuf = buf.asLongBuffer();
          longbuf.put(this.longHeap.heap, arrayStart, arraySize);
          ByteArrayInputStream bis = new ByteArrayInputStream(buf.array());
          return bis;
        } else if (type.getName().equals(CAS.TYPE_NAME_DOUBLE_ARRAY)) {
          arrayStart = this.heap.heap[getArrayStartAddress(fs.getAddress())];
          buf = ByteBuffer.allocate(arraySize * 8);
          DoubleBuffer doublebuf = buf.asDoubleBuffer();
          double[] doubleArray = new double[arraySize];
          for (int i = arrayStart; i < arrayStart + arraySize; i++) {
            doubleArray[i - arrayStart] = Double.longBitsToDouble(this.longHeap.heap[i]);
          }
          doublebuf.put(doubleArray);
          ByteArrayInputStream bis = new ByteArrayInputStream(buf.array());
          return bis;
        }

      } else if (null != aSofa.getSofaURI()) {
        URL url = new URL(aSofa.getSofaURI());
        return url.openStream();
      } else {
        return null;
      }
    } catch (MalformedURLException exc) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFADATASTREAM_ERROR);
      e.addArgument(exc.getMessage());
      throw e;
    } catch (CASRuntimeException exc) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFADATASTREAM_ERROR);
      e.addArgument(exc.getMessage());
      throw e;
    } catch (IOException exc) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFADATASTREAM_ERROR);
      e.addArgument(exc.getMessage());
      throw e;
    }
    return null;
  }

  public FSIterator createFilteredIterator(FSIterator it, FSMatchConstraint cons) {
    return new FilteredIterator(it, cons);
  }

  public void commitTypeSystem() {
    this.ts.commit();
    redoTypeSystemSetup();
    initFSClassRegistry();
    // After the type system has been committed, we can create the
    // index repository.
    createIndexRepository();
  }

  private void createIndexRepository() {
    if (!this.getTypeSystemMgr().isCommitted()) {
      throw new CASAdminException(CASAdminException.MUST_COMMIT_TYPE_SYSTEM);
    }
    if (this.indexRepository == null) {
      this.indexRepository = new FSIndexRepositoryImpl(this);
    }
  }

  public FSIndexRepositoryMgr getIndexRepositoryMgr() {
    // assert(this.cas.getIndexRepository() != null);
    return this.indexRepository;
  }

  /**
   * @deprecated
   */
  public void commitFS(FeatureStructure fs) {
    getIndexRepository().addFS(fs);
  }

  public FeaturePath createFeaturePath() {
    return new FeaturePathImpl();
  }

  // Implement the ConstraintFactory interface.

  /**
   * @see org.apache.uima.cas.admin.CASMgr#getTypeSystemMgr()
   */
  public TypeSystemMgr getTypeSystemMgr() {
    return this.ts;
  }

  public void reset() {
    if (!this.flushEnabled) {
      throw new CASAdminException(CASAdminException.FLUSH_DISABLED);
    }
    if (this == this.baseCAS) {
      resetNoQuestions();
      return;
    }
    // called from a CAS view.
    // clear CAS ...
    this.baseCAS.resetNoQuestions();
  }

  private void resetView() {
    this.indexRepository.flush();
    this.documentText = null;
    if (this.mySofaRef > 0 && this.getSofa().getSofaRef() == 1) {
      // indicate no Sofa exists for the initial view
      this.mySofaRef = -1;
    } else {
      this.mySofaRef = 0;
    }
    if (this.jcas != null) {
      try {
        JCasImpl.clearData(this);
      } catch (CASException e) {
        CASAdminException cae = new CASAdminException(CASAdminException.JCAS_ERROR);
        cae.addArgument(e.getMessage());
        throw cae;
      }
    }
  }
  
  public void resetNoQuestions() {
    int numViews = this.getBaseSofaCount();
    // Flush indexRepository for all Sofa
    for (int view = 1; view <= numViews; view++) {
      CAS tcas = (view == 1) ? getInitialView() : getView(view);
      if (tcas != null) {
        ((CASImpl) tcas).resetView();
      }
    }
    if (this.heap.getCurrentTempSize() > this.resetHeapSize) {
      this.heap.resetTempHeap(true);
      resetStringTable(true);
    } else {
      this.heap.resetTempHeap(false);
      resetStringTable(false);
    }

    this.byteHeap.reset();
    this.shortHeap.reset();
    this.longHeap.reset();

    this.indexRepository.flush();
    this.baseCAS.sofaNameSet.clear();
    this.initialSofaCreated = false;
    // always an Initial View now!!!
    this.sofaCount = 1;

    if (null != this.fsClassReg) {
      // TODO figure out why this is needed
      this.fsClassReg.flush();
    }
    if (this.jcas != null) {
      try {
        JCasImpl.clearData(this);
      } catch (CASException e) {
        CASAdminException cae = new CASAdminException(CASAdminException.JCAS_ERROR);
        cae.addArgument(e.getMessage());
        throw cae;
      }
    }
  }

  /**
   * @deprecated Use {@link #reset reset()}instead.
   */
  public void flush() {
    reset();
  }

  /**
   * 
   */
  public FSIndexRepository getIndexRepository() {
    if (this == this.baseCAS) {
      // BaseCas has no indexes for users
      return null;
    }
    if (this.indexRepository.isCommitted()) {
      return this.indexRepository;
    }
    return null;
  }

  protected FSIndexRepository getBaseIndexRepository() {
    if (this.baseCAS.indexRepository.isCommitted()) {
      return this.baseCAS.indexRepository;
    }
    return null;
  }

  protected void addSofaFsToIndex(SofaFS sofa) {
    this.baseCAS.getBaseIndexRepository().addFS(sofa);
  }

  protected void registerView(SofaFS aSofa) {
    this.mySofaRef = aSofa.hashCode();
  }

  public void reinit(CASSerializer ser) {
    if (this != this.baseCAS) {
      this.baseCAS.reinit(ser);
      return;
    }
    this.resetNoQuestions();
    reinit(ser.getHeapMetadata(), ser.getHeapArray(), ser.getStringTable(), ser.getFSIndex(), ser
            .getByteArray(), ser.getShortArray(), ser.getLongArray());
  }

  /**
   * @see org.apache.uima.cas.CAS#fs2listIterator(FSIterator)
   */
  public ListIterator fs2listIterator(FSIterator it) {
    return new FSListIteratorImpl(it);
  }

  /**
   * @see org.apache.uima.cas.admin.CASMgr#getCAS()
   */
  public CAS getCAS() {
    if (this.indexRepository.isCommitted()) {
      return this;
    }
    throw new CASAdminException(CASAdminException.MUST_COMMIT_INDEX_REPOSITORY);
  }

  void resetStringTable() {
    this.resetStringTable(false);
  }

  void resetStringTable(boolean doFullReset) {
    this.stringHeap.reset(doFullReset);
  }

  private void initStringTable() {
    this.stringHeap = new StringHeap();
  }

  public void setFSClassRegistry(FSClassRegistry fsClassReg) {
    this.fsClassReg = fsClassReg;
  }

  void initFSClassRegistry() {
    // System.out.println("Initializing FSClassRegistry");
    this.fsClassReg = new FSClassRegistry(this.ts, this.useFSCache);
    this.fsClassReg.addClassForType(this.fsArrayType, new ArrayFSGenerator());
    this.fsClassReg.addClassForType(this.intArrayType, IntArrayFSImpl.generator());
    this.fsClassReg.addClassForType(this.floatArrayType, FloatArrayFSImpl.generator());
    this.fsClassReg.addClassForType(this.stringArrayType, StringArrayFSImpl.generator());
    this.fsClassReg.addClassForType(this.sofaType, SofaFSImpl.getSofaFSGenerator());
    this.fsClassReg
            .addClassForType(this.annotBaseType, AnnotationBaseImpl.getAnnotationGenerator());
    this.fsClassReg.addClassForType(this.annotType, AnnotationImpl.getAnnotationGenerator());
    this.fsClassReg.addClassForType(getTypeSystem().getType(CAS.TYPE_NAME_BYTE_ARRAY),
            ByteArrayFSImpl.generator());
    this.fsClassReg.addClassForType(getTypeSystem().getType(CAS.TYPE_NAME_BOOLEAN_ARRAY),
            BooleanArrayFSImpl.generator());
    this.fsClassReg.addClassForType(getTypeSystem().getType(CAS.TYPE_NAME_SHORT_ARRAY),
            ShortArrayFSImpl.generator());
    this.fsClassReg.addClassForType(getTypeSystem().getType(CAS.TYPE_NAME_LONG_ARRAY),
            LongArrayFSImpl.generator());
    this.fsClassReg.addClassForType(getTypeSystem().getType(CAS.TYPE_NAME_DOUBLE_ARRAY),
            DoubleArrayFSImpl.generator());

    // assert(fsClassReg != null);
  }

  public FSClassRegistry getFSClassRegistry() // for JCas integration
  {
    return this.fsClassReg;
  }

  private void initCreatableTypeTable() {
    this.creatableType = new boolean[this.ts.getTypeArraySize()];
    Arrays.fill(this.creatableType, true);
    int typeCode;
    for (int i = 0; i < nonCreatableTypes.length; i++) {
      typeCode = ((TypeImpl) this.ts.getType(nonCreatableTypes[i])).getCode();
      for (int subType = this.ts.getSmallestType(); subType < this.creatableType.length; subType++) {
        if (this.ts.subsumes(typeCode, subType)) {
          this.creatableType[subType] = false;
        }
      }
    }
  }

  void setIndexRepository(FSIndexRepositoryImpl ir) {
    this.indexRepository = ir;
  }

  public void reinit(CASCompleteSerializer casCompSer) {
    if (this != this.baseCAS) {
      this.baseCAS.reinit(casCompSer);
      return;
    }
    this.ts = casCompSer.getCASMgrSerializer().getTypeSystem();
    commitTypeSystem();
    this.initTypeVariables();
    this.redoTypeSystemSetup();

    // reset index repositories -- wipes out Sofa index
    this.indexRepository = casCompSer.getCASMgrSerializer().getIndexRepository(this);
    this.indexRepository.commit();

    // get handle to existing initial View
    CAS initialView = (CAS) this.getInitialView();

    // throw away all other View information as the CAS definition may have changed
    this.sofa2indexMap.clear();
    this.sofa2tcasMap.clear();
    this.sofaCount = 0;

    // freshen the initial view
    ((CASImpl) initialView).refreshView(this.baseCAS, null);
    this.sofa2tcasMap.put(new Integer(1), initialView);
    this.baseCAS.sofaCount = 1;

    // deserialize heap
    CASSerializer casSer = casCompSer.getCASSerializer();
    reinit(casSer.getHeapMetadata(), casSer.getHeapArray(), casSer.getStringTable(), casSer
            .getFSIndex(), casSer.getByteArray(), casSer.getShortArray(), casSer.getLongArray());

    // we also need to throw away the JCAS. A new JCAS will be created on
    // the next
    // call to getJCas(). As with the CAS, we are counting on the fact that
    // this happens only in a service, where JCAS handles are not held on
    // to.
    this.jcas = null;
    this.sofa2jcasMap.clear();
  }

  void reinit(int[] heapMetadata, int[] heapArray, String[] stringTable, int[] fsIndex,
          byte[] byteHeapArray, short[] shortHeapArray, long[] longHeapArray) {
    createStringTableFromArray(stringTable);
    this.heap.reinit(heapMetadata, heapArray);
    if (byteHeapArray != null)
      this.byteHeap.reinit(byteHeapArray);
    if (shortHeapArray != null)
      this.shortHeap.reinit(shortHeapArray);
    if (longHeapArray != null)
      this.longHeap.reinit(longHeapArray);

    reinitIndexedFSs(fsIndex);
  }

  /**
   * --------------------------------------------------------------------- Blob Format
   * 
   * Element Size Number of Description (bytes) Elements ------------ ---------
   * -------------------------------- 4 1 Blob key = "UIMA" in utf-8 4 1 Version (currently = 1) 4 1
   * size of 32-bit FS Heap array = s32H 4 s32H 32-bit FS heap array 4 1 size of 16-bit string Heap
   * array = sSH 2 sSH 16-bit string heap array 4 1 size of string Ref Heap array = sSRH 4 2*sSRH
   * string ref offsets and lengths 4 1 size of FS index array = sFSI 4 sFSI FS index array
   * 
   * 4 1 size of 8-bit Heap array = s8H 1 s8H 8-bit Heap array 4 1 size of 16-bit Heap array = s16H
   * 2 s16H 16-bit Heap array 4 1 size of 64-bit Heap array = s64H 8 s64H 64-bit Heap array
   * ---------------------------------------------------------------------
   * 
   * This reads in and deserializes CAS data from a stream. Byte swapping may be needed if the blob
   * is from TAF -- TAF blob serialization writes data in native byte order.
   * 
   * @param istream
   * @throws CASRuntimeException
   */
  public void reinit(InputStream istream) throws CASRuntimeException {
    if (this != this.baseCAS) {
      this.baseCAS.reinit(istream);
      return;
    }
    this.resetNoQuestions();
    DataInputStream dis = new DataInputStream(istream);

    try {
      // key
      // deteremine if byte swap if needed based on key
      byte[] bytebuf = new byte[4];
      bytebuf[0] = dis.readByte();
      bytebuf[1] = dis.readByte();
      bytebuf[2] = dis.readByte();
      bytebuf[3] = dis.readByte();

      boolean swap = false;
      // check if first byte is ascii char U
      if (bytebuf[0] != 65) {
        swap = true;
      }

      // version
      // NOTE: even though nothing ever uses the version (yet), we MUST
      // read it
      // from the stream or else subsequent reads will not work. So that's
      // why
      // we are reading here and not assigning to any variable.
      if (swap) {
        swap4(dis, bytebuf);
      } else {
        dis.readInt();
      }

      // main fsheap
      int fsheapsz = 0;
      if (swap) {
        fsheapsz = swap4(dis, bytebuf);
      } else {
        fsheapsz = dis.readInt();
      }

      this.heap.reinitSizeOnly(fsheapsz);
      for (int i = 0; i < fsheapsz; i++) {
        if (swap) {
          this.heap.heap[i] = swap4(dis, bytebuf);
        } else {
          this.heap.heap[i] = dis.readInt();
        }
      }

      // string heap
      int stringheapsz = 0;
      if (swap) {
        stringheapsz = swap4(dis, bytebuf);
      } else {
        stringheapsz = dis.readInt();
      }

      this.stringHeap.stringHeap = new char[stringheapsz];
      for (int i = 0; i < stringheapsz; i++) {
        if (swap) {
          this.stringHeap.stringHeap[i] = swap2(dis, bytebuf);
        } else {
          this.stringHeap.stringHeap[i] = dis.readChar();
        }
      }
      this.stringHeap.charHeapPos = stringheapsz;

      // word alignment
      if (stringheapsz % 2 != 0) {
        dis.readChar();
      }

      // string ref heap
      int refheapsz = 0;
      if (swap) {
        refheapsz = swap4(dis, bytebuf);
      } else {
        refheapsz = dis.readInt();
      }

      refheapsz--;
      refheapsz = refheapsz / 2;
      refheapsz = refheapsz * 3;

      // read back into references consisting to three ints
      // --stringheap offset,length, stringlist offset
      this.stringHeap.refHeap = new int[StringHeap.FIRST_CELL_REF + refheapsz];

      dis.readInt(); // 0
      for (int i = this.stringHeap.refHeapPos; i < this.stringHeap.refHeap.length; i += StringHeap.REF_HEAP_CELL_SIZE) {
        if (swap) {
          this.stringHeap.refHeap[i + StringHeap.CHAR_HEAP_POINTER_OFFSET] = swap4(dis, bytebuf);
          this.stringHeap.refHeap[i + StringHeap.CHAR_HEAP_STRLEN_OFFSET] = swap4(dis, bytebuf);
        } else {
          this.stringHeap.refHeap[i + StringHeap.CHAR_HEAP_POINTER_OFFSET] = dis.readInt();
          this.stringHeap.refHeap[i + StringHeap.CHAR_HEAP_STRLEN_OFFSET] = dis.readInt();
        }
        this.stringHeap.refHeap[i + StringHeap.STRING_LIST_ADDR_OFFSET] = 0;
      }
      this.stringHeap.refHeapPos = refheapsz;

      // indexed FSs
      int fsindexsz = 0;
      if (swap) {
        fsindexsz = swap4(dis, bytebuf);
      } else {
        fsindexsz = dis.readInt();
      }
      int[] fsindexes = new int[fsindexsz];
      for (int i = 0; i < fsindexsz; i++) {
        if (swap) {
          fsindexes[i] = swap4(dis, bytebuf);
        } else {
          fsindexes[i] = dis.readInt();
        }
      }

      // build the index
      reinitIndexedFSs(fsindexes);

      // byte heap
      int byteheapsz = 0;
      if (swap) {
        byteheapsz = swap4(dis, bytebuf);
      } else {
        byteheapsz = dis.readInt();
      }

      this.byteHeap.heap = new byte[Math.max(16, byteheapsz)]; // must
      // be >
      // 0
      for (int i = 0; i < byteheapsz; i++) {
        this.byteHeap.heap[i] = dis.readByte();
      }
      this.byteHeap.heapPos = byteheapsz;

      // word alignment
      int align = byteheapsz % 4;
      for (int i = 0; i < align; i++) {
        dis.readByte();
      }

      // short heap
      int shortheapsz = 0;
      if (swap) {
        shortheapsz = swap4(dis, bytebuf);
      } else {
        shortheapsz = dis.readInt();
      }
      this.shortHeap.heap = new short[Math.max(16, shortheapsz)]; // must
      // be >
      // 0
      for (int i = 0; i < shortheapsz; i++) {
        if (swap) {
          this.shortHeap.heap[i] = (short) swap2(dis, bytebuf);
        } else {
          this.shortHeap.heap[i] = dis.readShort();
        }
      }
      this.shortHeap.heapPos = shortheapsz;

      // word alignment
      if (shortheapsz % 2 != 0) {
        dis.readChar();
      }

      // long heap
      int longheapsz = 0;
      if (swap) {
        longheapsz = swap4(dis, bytebuf);
        bytebuf = new byte[8];
      } else {
        longheapsz = dis.readInt();
      }
      this.longHeap.heap = new long[Math.max(16, longheapsz)]; // must
      // be >
      // 0
      for (int i = 0; i < longheapsz; i++) {
        if (swap) {
          this.longHeap.heap[i] = swap8(dis, bytebuf);
        } else {
          this.longHeap.heap[i] = dis.readLong();
        }
      }
      this.longHeap.heapPos = longheapsz;

    } catch (IOException e) {
      CASRuntimeException exception = new CASRuntimeException(
              CASRuntimeException.BLOB_DESERIALIZATION);
      exception.addArgument(e.getMessage());
      throw exception;
    }
  }

  private long swap8(DataInputStream dis, byte[] buf) throws IOException {

    buf[7] = dis.readByte();
    buf[6] = dis.readByte();
    buf[5] = dis.readByte();
    buf[4] = dis.readByte();
    buf[3] = dis.readByte();
    buf[2] = dis.readByte();
    buf[1] = dis.readByte();
    buf[0] = dis.readByte();
    ByteBuffer bb = ByteBuffer.wrap(buf);
    return bb.getLong();
  }

  private int swap4(DataInputStream dis, byte[] buf) throws IOException {
    buf[3] = dis.readByte();
    buf[2] = dis.readByte();
    buf[1] = dis.readByte();
    buf[0] = dis.readByte();
    ByteBuffer bb = ByteBuffer.wrap(buf);
    return bb.getInt();
  }

  private char swap2(DataInputStream dis, byte[] buf) throws IOException {
    buf[1] = dis.readByte();
    buf[0] = dis.readByte();
    ByteBuffer bb = ByteBuffer.wrap(buf, 0, 2);
    return bb.getChar();
  }

  private void reinitIndexedFSs(int[] fsIndex) {
    // Add FSs to index repository for base CAS
    int numViews = fsIndex[0];
    int loopLen = fsIndex[1];
    for (int i = 2; i < loopLen + 2; i++) {
      this.indexRepository.addFS(fsIndex[i]);
    }
    int loopStart = loopLen + 2;

    FSIterator iterator = this.baseCAS.getSofaIterator();
    final Feature idFeat = getTypeSystem().getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFAID);
    // Add FSs to index repository for each View
    while (iterator.isValid()) {
      SofaFS sofa = (SofaFS) iterator.get();
      String id = getLowLevelCAS().ll_getStringValue(sofa.hashCode(),
              ((FeatureImpl) idFeat).getCode());
      this.sofaNameSet.add(id);
      if (CAS.NAME_DEFAULT_SOFA.equals(id)) {
        this.registerInitialSofa();
      } else {
        // only bump count if not the initial view
        this.baseCAS.sofaCount++;
      }
      ((CASImpl) this.getView(sofa)).registerView(sofa);
      iterator.moveToNext();
    }
    for (int view = 1; view <= numViews; view++) {
      CAS tcas = (view == 1) ? getInitialView() : getView(view);
      if (tcas != null) {
        FSIndexRepositoryImpl loopIndexRep = (FSIndexRepositoryImpl) getSofaIndexRepository(view);
        loopLen = fsIndex[loopStart];
        for (int i = loopStart + 1; i < loopStart + 1 + loopLen; i++) {
          loopIndexRep.addFS(fsIndex[i]);
        }
        loopStart += loopLen + 1;
        ((CASImpl) tcas).updateDocumentAnnotation();
      } else {
        loopStart += 1;
      }
    }
  }

  // IndexedFSs format:
  // number of views
  // number of sofas
  // [sofa-1 ... sofa-n]
  // number of FS indexed in View1
  // [FS-1 ... FS-n]
  // etc.
  int[] getIndexedFSs() {
    IntVector v = new IntVector();
    int[] fsLoopIndex;

    int numViews = this.getBaseSofaCount();
    v.add(numViews);

    // Get indexes for base CAS
    fsLoopIndex = this.baseCAS.indexRepository.getIndexedFSs();
    v.add(fsLoopIndex.length);
    for (int k = 0; k < fsLoopIndex.length; k++) {
      v.add(fsLoopIndex[k]);
    }

    // Get indexes for each SofaFS in the CAS
    for (int sofaNum = 1; sofaNum <= numViews; sofaNum++) {
      FSIndexRepositoryImpl loopIndexRep = (FSIndexRepositoryImpl) this.baseCAS
              .getSofaIndexRepository(sofaNum);
      if (loopIndexRep != null)
        fsLoopIndex = loopIndexRep.getIndexedFSs();
      else
        fsLoopIndex = (new IntVector()).toArray();
      v.add(fsLoopIndex.length);
      for (int k = 0; k < fsLoopIndex.length; k++) {
        v.add(fsLoopIndex[k]);
      }
    }
    return v.toArray();
  }

  void createStringTableFromArray(String[] stringTable) {
    // why a new heap instead of reseting the old one???
    // this.stringHeap = new StringHeap();
    this.stringHeap.reset();
    for (int i = 1; i < stringTable.length; i++) {
      this.stringHeap.addString(stringTable[i]);
    }
  }

  static String mapName(String name, HashMap map) {
    String out = (String) map.get(name);
    if (out != null) {
      return out;
    }
    return name;
  }

  /**
   * This is your link from the low-level API to the high-level API. Use this method to create a
   * FeatureStructure object from an address. Not that the reverse is not supported by public APIs
   * (i.e., there is currently no way to get at the address of a FeatureStructure. Maybe we will
   * need to change that.
   * 
   * @param addr
   *          The address of the feature structure to be created.
   * @return A FeatureStructure object. Note that no checking whatsoever is done on the input
   *         address. There is really no way of finding out which addresses in the valid address
   *         space actually represent feature structures, and which don't.
   */
  public FeatureStructure createFS(int addr) {
    return this.fsClassReg.createFS(addr, this);
  }

  /**
   * Create a temporary (i.e., per document) String array FS on the heap.
   * 
   * @param type
   *          The type code of the array to be created.
   * @param len
   *          The length of the array to be created.
   * @exception ArrayIndexOutOfBoundsException
   *              If <code>type</code> is not a type.
   */
  public int createTempStringArray(int type, int len) {
    // String arrays are different, since for compatibility with TAF, we
    // need
    // to allocate space for the size of the strings.
    final int addr = this.heap.addToTempHeap(arrayContentOffset + len, type);
    this.heap.heap[(addr + arrayLengthFeatOffset)] = len;
    return addr;
  }

  // /**
  // * Create a permanent FS on the heap.
  // *
  // * @param type
  // * The type of the FS.
  // * @return The address of the new FS. This is an int <code>&gt;=0</code>.
  // * If it is <code>0</code>, something went wrong; <code>0</code>
  // * is not a valid address.
  // */
  // public int createPermFS(int type) {
  // return this.heap.addToHeap(this.fsSpaceReq[type], type);
  // }

  /**
   * Get the size of an array.
   * 
   * @param addr
   *          The address of the array.
   * @return The length of the array.
   */
  public int getArraySize(int addr) {
    return this.ll_getArraySize(addr);
  }

  public int ll_getArraySize(int arrayFsRef) {
    return this.heap.heap[arrayFsRef + arrayLengthFeatOffset];
  }

  /**
   * Get the heap address of the first cell of this array.
   * 
   * @param addr
   *          The address of the array.
   * @return The address where the first cell of the array is located.
   */
  public final int getArrayStartAddress(int addr) {
    return addr + arrayContentOffset;
  }

  /**
   * Get a specific value out of an array.
   * 
   * @param addr
   *          The address of the array.
   * @param index
   *          The index of the value we're interested in.
   * @return The value at <code>index</code>.
   * @exception ArrayIndexOutOfBoundsException
   */
  public int getArrayValue(int addr, int index) {
    checkArrayBounds(addr, index);
    return this.heap.heap[addr + arrayContentOffset + index];
  }

  /**
   * Set an array value.
   * 
   * @param addr
   *          The address of the array.
   * @param index
   *          The index we want to set.
   * @param value
   *          The value we want to set.
   * @exception ArrayIndexOutOfBoundsException
   */
  void setArrayValue(final int addr, final int index, final int value)
          throws ArrayIndexOutOfBoundsException {
    // Get the length of this array.
    final int arraySize = this.heap.heap[addr + arrayLengthFeatOffset];
    // Check for boundary violation.
    if ((index < 0) || (index >= arraySize)) {
      throw new ArrayIndexOutOfBoundsException();
    }
    this.heap.heap[addr + arrayContentOffset + index] = value;
  }

  void setArrayValueFromString(final int addr, final int index, final String value) {
    int arrayType = this.heap.heap[addr];

    if (arrayType == this.intArrayTypeCode) {
      setArrayValue(addr, index, Integer.parseInt(value));
    } else if (arrayType == this.floatArrayTypeCode) {
      setArrayValue(addr, index, CASImpl.float2int(Float.parseFloat(value)));
    } else if (arrayType == this.stringArrayTypeCode) {
      setArrayValue(addr, index, addString(value));
    } else if (arrayType == this.booleanArrayTypeCode) {
      getLowLevelCAS().ll_setBooleanArrayValue(addr, index, Boolean.valueOf(value).booleanValue());
    } else if (arrayType == this.byteArrayTypeCode) {
      getLowLevelCAS().ll_setByteArrayValue(addr, index, Byte.parseByte(value));
    } else if (arrayType == this.shortArrayTypeCode) {
      getLowLevelCAS().ll_setShortArrayValue(addr, index, Short.parseShort(value));
    } else if (arrayType == this.longArrayTypeCode) {
      getLowLevelCAS().ll_setLongArrayValue(addr, index, Long.parseLong(value));
    } else if (arrayType == this.doubleArrayTypeCode) {
      getLowLevelCAS().ll_setDoubleArrayValue(addr, index, Double.parseDouble(value));
    } else if (arrayType == this.fsArrayTypeCode) {
      setArrayValue(addr, index, Integer.parseInt(value));
    }
  }

  /**
   * Copy the contents of an array to an externally provided array.
   * 
   * @param addr
   *          The address of the source array.
   * @param sourceOffset
   *          The offset we want to start copying at.
   * @param dest
   *          The destination array.
   * @param destOffset
   *          An offset into the destination array.
   * @param length
   *          The number of items to copy.
   */
  void copyToArray(int addr, int sourceOffset, int[] dest, int destOffset, int length) {
    // Get the length of this array.
    final int arraySize = this.heap.heap[addr + arrayLengthFeatOffset];
    // Check boundary conditions for source array. We can rely on Java to
    // complain about boundary violations for the destination array.
    if ((sourceOffset < 0) || ((length + sourceOffset) > arraySize)) {
      throw new ArrayIndexOutOfBoundsException();
    }
    // Compute the offset into the heap where the array starts.
    final int offset = addr + arrayContentOffset;
    System.arraycopy(this.heap.heap, offset + sourceOffset, dest, destOffset, length);
  }

  /**
   * Copy the contents of an input array into a CAS array.
   * 
   * @param src
   *          The array to copy from.
   * @param srcOffset
   *          An offset into the source from where to start copying.
   * @param addr
   *          The address of the array we're copying to.
   * @param destOffset
   *          Where to start copying into the destination array.
   * @param length
   *          How many elements to copy.
   */
  void copyFromArray(int[] src, int srcOffset, int addr, int destOffset, int length) {
    // Get the length of this array.
    final int arraySize = this.heap.heap[addr + arrayLengthFeatOffset];
    // Check boundary conditions for destination array. We can rely on Java
    // to
    // complain about boundary violations for the source array.
    if ((destOffset < 0) || ((length + destOffset) > arraySize)) {
      throw new ArrayIndexOutOfBoundsException();
    }
    // Compute the offset into the heap where the array starts.
    final int offset = addr + arrayContentOffset;
    System.arraycopy(src, srcOffset, this.heap.heap, offset + destOffset, length);
  }

  void copyFeatures(int trgAddr, int srcAddr) throws CASRuntimeException {
    int typeCode = getHeapValue(trgAddr);
    if (typeCode != getHeapValue(srcAddr)) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_TYPE);
      e.addArgument("Type of source and target feature structures do not match");
      throw (e);
    }
    // get features to copy
    int[] featcodes = getTypeSystem().getLowLevelTypeSystem().ll_getAppropriateFeatures(typeCode);
    for (int i = 0; i < featcodes.length; i++) {

      // get range type of this feature
      Feature feature = getTypeSystem().getLowLevelTypeSystem().ll_getFeatureForCode(featcodes[i]);
      Type rangeType = feature.getRange();
      // get feature code
      int featCode = ((FeatureImpl) feature).getCode();
      // get the value for this feature offset in src fs
      int val = getHeapValue(srcAddr + this.featureOffset[featCode]);
      // if this is a string, create a new reference in the string
      // reference heap
      // and point to the same string as the string feature in src fs.
      if (isStringType(rangeType)) {
        int newRef = this.stringHeap.cloneStringReference(val);
        // this.heap.heap[trgAddr+1+i] = newRef;
        this.heap.heap[trgAddr + this.featureOffset[featCode]] = newRef;
      } else { // scalar values copied / other FS
        this.heap.heap[trgAddr + this.featureOffset[featCode]] = getHeapValue(srcAddr
                + this.featureOffset[featCode]);
      }
    }
  }

  /**
   * Create a permanent array on the heap.
   * 
   * @param type
   *          The type of the array.
   * @param len
   *          The length of the array.
   * @return The address of the new FS. This is an int <code>&gt;=0</code>. If it is
   *         <code>0</code>, something went wrong; <code>0</code> is not a valid address.
   */
  public int createPermArray(int type, int len) {
    final int addr = this.heap.addToHeap(this.fsSpaceReq[type] + len, type);
    this.heap.heap[(addr + arrayLengthFeatOffset)] = len;
    return addr;
  }

  /**
   * Get the value of an address on the heap.
   * 
   * @param addr
   *          The target address.
   * @return The value at the address.
   */
  public int getHeapValue(int addr) {
    return this.heap.heap[addr];
  }

  /**
   * Set the value of a feature of a FS.
   * 
   * @param addr
   *          The address of the FS.
   * @param feat
   *          The code of the feature.
   * @param val
   *          The new value for the feature.
   * @exception ArrayIndexOutOfBoundsException
   *              If the feature is not a legal feature, or it is not appropriate for the type at
   *              the address.
   */
  public void setFeatureValue(int addr, int feat, int val) {
    this.heap.heap[(addr + this.featureOffset[feat])] = val;
  }

  public void setStringValue(int addr, int feat, String s) {
    final int stringCode = ((s == null) ? NULL : this.stringHeap.addString(s));
    setFeatureValue(addr, feat, stringCode);
  }

  public void setFloatValue(int addr, int feat, float f) {
    final int floatCode = Float.floatToIntBits(f);
    setFeatureValue(addr, feat, floatCode);
  }

  public void setFloatValue(int addr, float f) {
    final int floatCode = Float.floatToIntBits(f);
    this.heap.heap[addr] = floatCode;
  }

  public int getFeatureValue(int addr, int feat) {
    return this.heap.heap[(addr + this.featureOffset[feat])];
  }

  public String getStringValue(int addr, int feat) {
    return this.stringHeap.getStringForCode(this.heap.heap[addr + this.featureOffset[feat]]);
  }

  public float getFloatValue(int addr, int feat) {
    return Float.intBitsToFloat(getFeatureValue(addr, feat));
  }

  public float getFloatValue(int addr) {
    return Float.intBitsToFloat(this.heap.heap[addr]);
  }

  // byte
  public void setFeatureValue(int addr, int feat, byte v) {
    Byte bytevalue = new Byte(v);
    setFeatureValue(addr, feat, bytevalue.intValue());
  }

  public byte getByteValue(int addr, int feat) {
    return ll_getByteValue(addr, feat);
  }

  // boolean
  public void setFeatureValue(int addr, int feat, boolean v) {
    if (v) {
      setFeatureValue(addr, feat, CASImpl.TRUE);
    } else {
      setFeatureValue(addr, feat, CASImpl.FALSE);
    }
  }

  public boolean getBooleanValue(int addr, int feat) {
    return ll_getBooleanValue(addr, feat);
  }

  // short
  public void setFeatureValue(int addr, int feat, short s) {
    setFeatureValue(addr, feat, (int) s);
  }

  public short getShortValue(int addr, int feat) {
    return this.ll_getShortValue(addr, feat);
  }

  // long
  public void setFeatureValue(int addr, int feat, long s) {
    this.ll_setLongValue(addr, feat, s);
  }

  public long getLongValue(int addr, int feat) {
    return this.ll_getLongValue(addr, feat);
  }

  // double
  public void setFeatureValue(int addr, int feat, double s) {
    this.ll_setDoubleValue(addr, feat, s);
  }

  public double getDoubleValue(int addr, int feat) {
    return ll_getDoubleValue(addr, feat);
  }

  public String getFeatureValueAsString(int addr, int feat) {
    int typeCode = (this.ts.range(feat));
    if (typeCode == this.intTypeCode) {
      return Integer.toString(this.ll_getIntValue(addr, feat));
    } else if (typeCode == this.floatTypeCode) {
      return Float.toString(this.ll_getFloatValue(addr, feat));
    } else if (this.ts.subsumes(this.stringTypeCode, typeCode)) {
      return this.getStringValue(addr, feat);
    } else if (typeCode == this.booleanTypeCode) {
      return Boolean.toString(this.getBooleanValue(addr, feat));
    } else if (typeCode == this.byteTypeCode) {
      return Byte.toString(this.getByteValue(addr, feat));
    } else if (typeCode == this.shortTypeCode) {
      return Short.toString(this.getShortValue(addr, feat));
    } else if (typeCode == this.longTypeCode) {
      return Long.toString(this.getLongValue(addr, feat));
    } else if (typeCode == this.doubleTypeCode) {
      return Double.toString(this.getDoubleValue(addr, feat));
    } else {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_RANGE);
      e.addArgument(this.ts.getFeatureName(feat));
      e.addArgument(this.ts.getTypeName(typeCode));
      throw e;
    }

  }

  public void setFeatureValueFromString(int fsref, int feat, String value) {
    int typeCode = (this.ts.range(feat));
    if (typeCode == this.intTypeCode) {
      this.ll_setIntValue(fsref, feat, Integer.parseInt(value));
    } else if (typeCode == this.floatTypeCode) {
      this.setFloatValue(fsref, feat, Float.parseFloat(value));
    } else if (this.ts.subsumes(this.stringTypeCode, typeCode)) {
      this.setStringValue(fsref, feat, value);
    } else if (typeCode == this.booleanTypeCode) {
      this.setFeatureValue(fsref, feat, Boolean.valueOf(value).booleanValue());
    } else if (typeCode == this.byteTypeCode) {
      this.setFeatureValue(fsref, feat, Byte.parseByte(value));
    } else if (typeCode == this.shortTypeCode) {
      this.setFeatureValue(fsref, feat, Short.parseShort(value));
    } else if (typeCode == this.longTypeCode) {
      this.setFeatureValue(fsref, feat, Long.parseLong(value));
    } else if (typeCode == this.doubleTypeCode) {
      this.setFeatureValue(fsref, feat, Double.parseDouble(value));
    } else {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_TYPE);
      e.addArgument(this.ts.getFeatureName(feat));
      e.addArgument(this.ts.getTypeName(typeCode));
      throw e;
    }
  }

  public static final float int2float(int i) {
    return Float.intBitsToFloat(i);
  }

  public static final int float2int(float f) {
    return Float.floatToIntBits(f);
  }

  public static final double long2double(long l) {
    return Double.longBitsToDouble(l);
  }

  public static final long double2long(double d) {
    return Double.doubleToLongBits(d);
  }

  public TypeSystemImpl getTypeSystemImpl() {
    return this.ts;
  }

  public String getStringForCode(int stringCode) {
    return this.stringHeap.getStringForCode(stringCode);
  }

  /**
   * Check if this is a regular type (i.e., not an array or a basic type).
   * 
   * @param typeCode
   *          The code to check.
   * @return <code>true</code> iff <code>typeCode</code> is a type for which a regular FS can be
   *         generated.
   * @exception NullPointerException
   *              If <code>typeCode</code> is not a type code.
   */
  final boolean isCreatableType(int typeCode) {
    return this.creatableType[typeCode];
  }

  boolean isBuiltinType(Type type) {
    // had to hack this because it wasn't considering List types as built in
    // -AL
    return (type.getName().startsWith("uima") || type == getAnnotationType());
    /*
     * final int typeCode = ((TypeImpl) type).getCode(); return (type == ts.getTopType()) ||
     * isArrayType(typeCode) || isAbstractArrayType(typeCode) || isStringType(typeCode) ||
     * isFloatType(typeCode) || isIntType(typeCode);
     */
  }

  int addString(String s) {
    return this.stringHeap.addString(s);
  }

  // Type access methods.
  public boolean isStringType(Type type) {
    return this.ts.subsumes(this.stringType, type);
  }

  public boolean isAbstractArrayType(Type type) {
    return this.ts.subsumes(this.arrayBaseType, type);
  }

  public boolean isArrayType(Type type) {
    return ((type == this.fsArrayType) || (type == this.intArrayType)
            || (type == this.floatArrayType) || (type == this.stringArrayType)
            || (type == this.booleanArrayType) || (type == this.byteArrayType)
            || (type == this.shortArrayType) || (type == this.doubleArrayType) || (type == this.longArrayType));

  }

  public boolean isIntArrayType(Type type) {
    return (type == this.intArrayType);
  }

  public boolean isFloatArrayType(Type type) {
    return (type == this.floatArrayType);
  }

  public boolean isStringArrayType(Type type) {
    return (type == this.stringArrayType);
  }

  public boolean isBooleanArrayType(Type type) {
    return (type == this.booleanArrayType);
  }

  public boolean isByteArrayType(Type type) {
    return (type == this.byteArrayType);
  }

  public boolean isShortArrayType(Type type) {
    return (type == this.shortArrayType);
  }

  public boolean isLongArrayType(Type type) {
    return (type == this.longArrayType);
  }

  public boolean isDoubleArrayType(Type type) {
    return (type == this.doubleArrayType);
  }

  public boolean isFSArrayType(Type type) {
    return (type == this.fsArrayType);
  }

  public boolean isIntType(Type type) {
    return (type == this.intType);
  }

  public boolean isFloatType(Type type) {
    return (type == this.floatType);
  }

  public boolean isStringType(int type) {
    return this.ts.subsumes(this.stringType.getCode(), type);
  }

  public boolean isByteType(Type type) {
    return (type == this.byteType);
  }

  public boolean isBooleanType(Type type) {
    return (type == this.booleanType);
  }

  public boolean isShortType(Type type) {
    return (type == this.shortType);
  }

  public boolean isLongType(Type type) {
    return (type == this.longType);
  }

  public boolean isDoubleType(Type type) {
    return (type == this.doubleType);
  }

  public boolean isAbstractArrayType(int type) {
    return this.ts.subsumes(this.arrayBaseType.getCode(), type);
  }

  public boolean isArrayType(int type) {
    return ((type == this.fsArrayType.getCode()) || (type == this.intArrayType.getCode())
            || (type == this.floatArrayType.getCode()) || (type == this.stringArrayType.getCode())
            || (type == this.booleanArrayType.getCode()) || (type == this.byteArrayType.getCode())
            || (type == this.shortArrayType.getCode()) || (type == this.longArrayType.getCode()) || (type == this.doubleArrayType
            .getCode()));
  }

  public boolean isIntArrayType(int type) {
    return (type == this.intArrayType.getCode());
  }

  public boolean isFloatArrayType(int type) {
    return (type == this.floatArrayType.getCode());
  }

  public boolean isStringArrayType(int type) {
    return (type == this.stringArrayType.getCode());
  }

  public boolean isByteArrayType(int type) {
    return (type == this.byteArrayType.getCode());
  }

  public boolean isBooleanArrayType(int type) {
    return (type == this.booleanArrayType.getCode());
  }

  public boolean isShortArrayType(int type) {
    return (type == this.shortArrayType.getCode());
  }

  public boolean isLongArrayType(int type) {
    return (type == this.longArrayType.getCode());
  }

  public boolean isDoubleArrayType(int type) {
    return (type == this.doubleArrayType.getCode());
  }

  public boolean isFSArrayType(int type) {
    return (type == this.fsArrayType.getCode());
  }

  public boolean isIntType(int type) {
    return (type == this.intType.getCode());
  }

  public boolean isFloatType(int type) {
    return (type == this.floatType.getCode());
  }

  public boolean isByteType(int type) {
    return (type == this.byteType.getCode());
  }

  public boolean isBooleanType(int type) {
    return (type == this.booleanType.getCode());
  }

  public boolean isShortType(int type) {
    return (type == this.shortType.getCode());
  }

  public boolean isLongType(int type) {
    return (type == this.longType.getCode());
  }

  public boolean isDoubleType(int type) {
    return (type == this.doubleType.getCode());
  }

  public Heap getHeap() {
    return this.heap;
  }

  public int getFeatureOffset(int feat) {
    if (feat < 1 || feat >= this.featureOffset.length) {
      return -1;
    }
    return this.featureOffset[feat];
  }

  public void redoTypeSystemSetup() {
    // Compute feature offsets.
    computeFeatureOffsets();
    // Compute FS space requirements.
    final int numTypes = this.ts.getNumberOfTypes();
    this.fsSpaceReq = new int[numTypes + 1];
    for (int i = 1; i <= numTypes; i++) {
      this.fsSpaceReq[i] = this.ts.getAppropriateFeatures(i).length + 1;
    }
    // Initialize the non-creatable types info.
    initCreatableTypeTable();
  }

  // Compute the feature offsets
  private final void computeFeatureOffsets() {
    final int numFeats = this.ts.getNumberOfFeatures();
    this.featureOffset = new int[numFeats + 1];
    Type startType = this.ts.getTopType();
    // Recursively compute the offsets, starting at the top. Initial offset
    // is 0.
    computeFeatureOffsets(startType, 0);
  }

  // Compute the offsets for features of a type. The offset parameter
  // specifies
  // how many offset values have already been used.
  private final void computeFeatureOffsets(Type t, int offset) {
    // Find all features for which the input type is the the domain type.
    List allFeats = t.getFeatures();
    ArrayList introFeats = new ArrayList();
    final int numAllFeats = allFeats.size();
    Feature feat;
    for (int i = 0; i < numAllFeats; i++) {
      feat = (Feature) allFeats.get(i);
      if (feat.getDomain() == t) {
        introFeats.add(feat);
      }
    }
    // For each feature for which the input type is the domain, assign an
    // offset
    // arbitrarily, starting with the input offset + 1.
    int featCode;
    final int numFeats = introFeats.size();
    for (int i = 0; i < numFeats; i++) {
      featCode = ((FeatureImpl) introFeats.get(i)).getCode();
      this.featureOffset[featCode] = offset + 1 + i;
    }
    // Call routine recursively for all subtypes. Increment input offset by
    // number of features introduced on this type.
    Vector types = this.ts.getDirectlySubsumedTypes(t);
    final int numTypes = types.size();
    for (int i = 0; i < numTypes; i++) {
      computeFeatureOffsets((Type) types.get(i), offset + numFeats);
    }
  }

  private void setupTSDefault() {
    setupTSDefault(this.ts);
    // Create internal tables.
    redoTypeSystemSetup();
  }

  public static void setupTSDefault(TypeSystemImpl ts) {
    // Create top type.
    Type top = ts.addTopType(CAS.TYPE_NAME_TOP);
    // Add basic data types.
    Type intT = ts.addType(CAS.TYPE_NAME_INTEGER, top);
    Type floatT = ts.addType(CAS.TYPE_NAME_FLOAT, top);
    Type stringT = ts.addType(CAS.TYPE_NAME_STRING, top);
    // Add arrays.
    Type array = ts.addType(CAS.TYPE_NAME_ARRAY_BASE, top);
    TypeImpl fsArray = (TypeImpl) ts.addType(CAS.TYPE_NAME_FS_ARRAY, array);
    TypeImpl floatArray = (TypeImpl) ts.addType(CAS.TYPE_NAME_FLOAT_ARRAY, array);
    TypeImpl intArray = (TypeImpl) ts.addType(CAS.TYPE_NAME_INTEGER_ARRAY, array);
    TypeImpl stringArray = (TypeImpl) ts.addType(CAS.TYPE_NAME_STRING_ARRAY, array);
    // Add lists.
    Type list = ts.addType(CAS.TYPE_NAME_LIST_BASE, top);
    // FS lists.
    Type fsList = ts.addType(CAS.TYPE_NAME_FS_LIST, list);
    Type fsEList = ts.addType(CAS.TYPE_NAME_EMPTY_FS_LIST, fsList);
    Type fsNeList = ts.addType(CAS.TYPE_NAME_NON_EMPTY_FS_LIST, fsList);
    ts.addFeature(CAS.FEATURE_BASE_NAME_HEAD, fsNeList, top);
    ts.addFeature(CAS.FEATURE_BASE_NAME_TAIL, fsNeList, fsList);
    // Float lists.
    Type floatList = ts.addType(CAS.TYPE_NAME_FLOAT_LIST, list);
    Type floatEList = ts.addType(CAS.TYPE_NAME_EMPTY_FLOAT_LIST, floatList);
    Type floatNeList = ts.addType(CAS.TYPE_NAME_NON_EMPTY_FLOAT_LIST, floatList);
    ts.addFeature(CAS.FEATURE_BASE_NAME_HEAD, floatNeList, floatT);
    ts.addFeature(CAS.FEATURE_BASE_NAME_TAIL, floatNeList, floatList);
    // Integer lists.
    Type intList = ts.addType(CAS.TYPE_NAME_INTEGER_LIST, list);
    Type intEList = ts.addType(CAS.TYPE_NAME_EMPTY_INTEGER_LIST, intList);
    Type intNeList = ts.addType(CAS.TYPE_NAME_NON_EMPTY_INTEGER_LIST, intList);
    ts.addFeature(CAS.FEATURE_BASE_NAME_HEAD, intNeList, intT);
    ts.addFeature(CAS.FEATURE_BASE_NAME_TAIL, intNeList, intList);
    // String lists.
    Type stringList = ts.addType(CAS.TYPE_NAME_STRING_LIST, list);
    Type stringEList = ts.addType(CAS.TYPE_NAME_EMPTY_STRING_LIST, stringList);
    Type stringNeList = ts.addType(CAS.TYPE_NAME_NON_EMPTY_STRING_LIST, stringList);
    ts.addFeature(CAS.FEATURE_BASE_NAME_HEAD, stringNeList, stringT);
    ts.addFeature(CAS.FEATURE_BASE_NAME_TAIL, stringNeList, stringList);

    Type booleanT = ts.addType(CAS.TYPE_NAME_BOOLEAN, top);
    Type byteT = ts.addType(CAS.TYPE_NAME_BYTE, top);
    Type shortT = ts.addType(CAS.TYPE_NAME_SHORT, top);
    Type longT = ts.addType(CAS.TYPE_NAME_LONG, top);
    Type doubleT = ts.addType(CAS.TYPE_NAME_DOUBLE, top);

    TypeImpl booleanArray = (TypeImpl) ts.addType(CAS.TYPE_NAME_BOOLEAN_ARRAY, array);
    TypeImpl byteArray = (TypeImpl) ts.addType(CAS.TYPE_NAME_BYTE_ARRAY, array);
    TypeImpl shortArray = (TypeImpl) ts.addType(CAS.TYPE_NAME_SHORT_ARRAY, array);
    TypeImpl longArray = (TypeImpl) ts.addType(CAS.TYPE_NAME_LONG_ARRAY, array);
    TypeImpl doubleArray = (TypeImpl) ts.addType(CAS.TYPE_NAME_DOUBLE_ARRAY, array);

    // Sofa Stuff
    Type sofa = ts.addType(CAS.TYPE_NAME_SOFA, top);
    ts.addFeature(CAS.FEATURE_BASE_NAME_SOFANUM, sofa, intT);
    ts.addFeature(CAS.FEATURE_BASE_NAME_SOFAID, sofa, stringT);
    ts.addFeature(CAS.FEATURE_BASE_NAME_SOFAMIME, sofa, stringT);
    // Type localSofa = ts.addType(CAS.TYPE_NAME_LOCALSOFA, sofa);
    ts.addFeature(CAS.FEATURE_BASE_NAME_SOFAARRAY, sofa, top);
    ts.addFeature(CAS.FEATURE_BASE_NAME_SOFASTRING, sofa, stringT);
    // Type remoteSofa = ts.addType(CAS.TYPE_NAME_REMOTESOFA, sofa);
    ts.addFeature(CAS.FEATURE_BASE_NAME_SOFAURI, sofa, stringT);

    // Annotations
    Type annotBaseType = ts.addType(CAS.TYPE_NAME_ANNOTATION_BASE, top);
    ts.addFeature(CAS.FEATURE_BASE_NAME_SOFA, annotBaseType, sofa);
    Type annotType = ts.addType(CAS.TYPE_NAME_ANNOTATION, annotBaseType);
    ts.addFeature(CAS.FEATURE_BASE_NAME_BEGIN, annotType, intT);
    ts.addFeature(CAS.FEATURE_BASE_NAME_END, annotType, intT);
    Type docType = ts.addType(CAS.TYPE_NAME_DOCUMENT_ANNOTATION, annotType);
    ts.addFeature(CAS.FEATURE_BASE_NAME_LANGUAGE, docType, stringT);

    // Lock individual types.
    setTypeFinal(intT);
    setTypeFinal(floatT);
    setTypeFinal(stringT);
    ((TypeImpl) top).setFeatureFinal();
    setTypeFinal(array);
    setTypeFinal(fsArray);
    setTypeFinal(intArray);
    setTypeFinal(floatArray);
    setTypeFinal(stringArray);
    setTypeFinal(sofa);

    setTypeFinal(byteT);
    setTypeFinal(booleanT);
    setTypeFinal(shortT);
    setTypeFinal(longT);
    setTypeFinal(doubleT);
    setTypeFinal(booleanArray);
    setTypeFinal(byteArray);
    setTypeFinal(shortArray);
    setTypeFinal(longArray);
    setTypeFinal(doubleArray);

    ((TypeImpl) list).setFeatureFinal();
    ((TypeImpl) fsList).setFeatureFinal();
    ((TypeImpl) fsEList).setFeatureFinal();
    ((TypeImpl) fsNeList).setFeatureFinal();
    ((TypeImpl) floatList).setFeatureFinal();
    ((TypeImpl) floatEList).setFeatureFinal();
    ((TypeImpl) floatNeList).setFeatureFinal();
    ((TypeImpl) intList).setFeatureFinal();
    ((TypeImpl) intEList).setFeatureFinal();
    ((TypeImpl) intNeList).setFeatureFinal();
    ((TypeImpl) stringList).setFeatureFinal();
    ((TypeImpl) stringEList).setFeatureFinal();
    ((TypeImpl) stringNeList).setFeatureFinal();
    ((TypeImpl) annotType).setFeatureFinal();
    ((TypeImpl) annotBaseType).setFeatureFinal();
  }

  private static void setTypeFinal(Type type) {
    TypeImpl t = (TypeImpl) type;
    t.setFeatureFinal();
    t.setInheritanceFinal();
  }

  /**
   * @see org.apache.uima.cas.admin.CASMgr#initCASIndexes()
   */
  public void initCASIndexes() throws CASException {
    if (null == this.ts.getType(CAS.TYPE_NAME_SOFA)) {
      throw new CASException(CASException.MUST_COMMIT_TYPE_SYSTEM, null);
    }
    if (this.annotIndexInitialized) {
      return;
    }
    FSIndexComparator comp = this.indexRepository.createComparator();
    comp.setType(this.ts.getType(CAS.TYPE_NAME_SOFA));
    comp.addKey(this.ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFANUM),
            FSIndexComparator.STANDARD_COMPARE);
    this.indexRepository.createIndex(comp, CAS.SOFA_INDEX_NAME, FSIndex.SET_INDEX);

    comp = this.indexRepository.createComparator();
    comp.setType(this.ts.getType(CAS.TYPE_NAME_ANNOTATION));
    comp.addKey(this.ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_BEGIN),
            FSIndexComparator.STANDARD_COMPARE);
    comp.addKey(this.ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_END),
            FSIndexComparator.REVERSE_STANDARD_COMPARE);
    comp.addKey(this.indexRepository.getDefaultTypeOrder(), FSIndexComparator.STANDARD_COMPARE);
    this.indexRepository.createIndex(comp, CAS.STD_ANNOTATION_INDEX);
  }

  ArrayList getStringTable() {
    // return this.stringList;
    return this.baseCAS.getStringList();
  }

  // ///////////////////////////////////////////////////////////////////////////
  // CAS support ... create CAS view of aSofa


  // For internal use only
  public CAS getView(int sofaNum) {
    return (CAS) this.sofa2tcasMap.get(new Integer(sofaNum));
  }

  /**
   * 
   */
  public CAS getCurrentView() {
    return getView(CAS.NAME_DEFAULT_SOFA);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // JCas support

  public JCas getJCas() throws CASException {
    if (this.jcas == null) {
      this.jcas = JCasImpl.getJCas(this);
    }
    return this.jcas;
  }

  // Create JCas view of aSofa
  public JCas getJCas(SofaFS aSofa) throws CASException {
    // Create base JCas, if needed
    if (this.baseCAS.jcas == null) {
      this.baseCAS.jcas = JCasImpl.getJCas(this.baseCAS);
    }

    // If a JCas already exists for this Sofa, return it
    JCas aJCas = (JCas) this.baseCAS.sofa2jcasMap.get(new Integer(aSofa.getSofaRef()));
    if (null != aJCas) {
      return aJCas;
    }

    // Get view of aSofa
    CASImpl view = (CASImpl) getView(aSofa);
    // wrap in JCas
    aJCas = view.getJCas();
    this.sofa2jcasMap.put(new Integer(aSofa.getSofaRef()), aJCas);
    return aJCas;
  }

  /**
   * @deprecated
   */
  public JCas getJCas(SofaID aSofaID) throws CASException {
    SofaFS sofa = getSofa(aSofaID);
    if (sofa == null) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFANAME_NOT_FOUND);
      e.addArgument(aSofaID.getSofaID());
      throw e;
    }
    return getJCas(sofa);
  }

  // For infernal platform use only
  protected CAS getInitialView() {
    CAS couldBeThis = (CAS) this.sofa2tcasMap.get(new Integer(1));
    if (couldBeThis != null) {
      return couldBeThis;
    }
    // create the initial view, without a Sofa
    CAS aTcas = new CASImpl(this.baseCAS, null);
    this.sofa2tcasMap.put(new Integer(1), aTcas);
    this.baseCAS.sofaCount = 1;
    return aTcas;
  }

  public CAS createView(String aSofaID) {
    // do sofa mapping for current component
    String absoluteSofaName = null;
    if (getCurrentComponentInfo() != null) {
      absoluteSofaName = getCurrentComponentInfo().mapToSofaID(aSofaID);
    }
    if (absoluteSofaName == null) {
      absoluteSofaName = aSofaID;
    }

    // Can't use name of Initial View
    if (CAS.NAME_DEFAULT_SOFA.equals(absoluteSofaName)) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFANAME_ALREADY_EXISTS);
      e.addArgument(aSofaID);
      throw e;
    }
    SofaFS newSofa = createSofa(absoluteSofaName, null);
    CAS newView = getView(newSofa);
    ((CASImpl) newView).registerView(newSofa);
    return newView;
  }

  public CAS getView(String aSofaID) {
    // do sofa mapping for current component
    String absoluteSofaName = null;
    if (getCurrentComponentInfo() != null) {
      absoluteSofaName = getCurrentComponentInfo().mapToSofaID(aSofaID);
    }
    if (absoluteSofaName == null) {
      absoluteSofaName = aSofaID;
    }

    // if this resolves to the Initial View, return view(1)...
    // ... as the Sofa for this view may not exist yet
    if (CAS.NAME_DEFAULT_SOFA.equals(absoluteSofaName)) {
      return getInitialView();
    }
    // get Sofa and switch to view
    SofaFS sofa = getSofa(absoluteSofaName);
    if (sofa == null) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFANAME_NOT_FOUND);
      e.addArgument(absoluteSofaName);
      throw e;
    }
    return getView(sofa);
  }

  public CAS getView(SofaFS aSofa) {
    CASImpl aTcas = (CASImpl) this.sofa2tcasMap.get(new Integer(aSofa.getSofaRef()));
    if (null == aTcas) {
      // create a new CAS view
      aTcas = new CASImpl(this.baseCAS, aSofa);
      this.sofa2tcasMap.put(new Integer(aSofa.getSofaRef()), aTcas);
      if (this.baseCAS.sofaCount < aSofa.getSofaRef()) {
        // for binary deserialization
        this.baseCAS.sofaCount = aSofa.getSofaRef();
        final Feature idFeat = getTypeSystem().getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFAID);
        String id = getLowLevelCAS().ll_getStringValue(aSofa.hashCode(),
                ((FeatureImpl) idFeat).getCode());
        if (this.baseCAS.sofaNameSet.contains(id)) {
          CASRuntimeException e = new CASRuntimeException(
                  CASRuntimeException.SOFANAME_ALREADY_EXISTS);
          e.addArgument(id);
          throw e;
        }
        this.baseCAS.sofaNameSet.add(id);
      }
    }
    if (this.baseCAS.sofaCount < aSofa.getSofaRef()) {
      // for xcas deserialization
      this.baseCAS.sofaCount = aSofa.getSofaRef();
      final Feature idFeat = getTypeSystem().getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFAID);
      String id = getLowLevelCAS().ll_getStringValue(aSofa.hashCode(),
              ((FeatureImpl) idFeat).getCode());
      if (this.baseCAS.sofaNameSet.contains(id)) {
        CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFANAME_ALREADY_EXISTS);
        e.addArgument(id);
        throw e;
      }
      this.baseCAS.sofaNameSet.add(id);
    }
    return aTcas;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getTypeSystem()
   */
  public LowLevelTypeSystem ll_getTypeSystem() {
    return this.ts.getLowLevelTypeSystem();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getIndexRepository()
   */
  public LowLevelIndexRepository ll_getIndexRepository() {
    return this.indexRepository;
  }

  private final void checkLowLevelParams(int fsRef, int domType, int ranType, int feat) {
    checkTypeAt(domType, fsRef);
    checkFeature(feat);
    checkTypingConditions(domType, ranType, feat);
  }

  private final void checkLowLevelParams(int fsRef, int domType, int feat) {
    checkTypeAt(domType, fsRef);
    checkFeature(feat);
    checkDomTypeConditions(domType, feat);
  }

  private final void checkDomTypeConditions(int domTypeCode, int featCode) {
    if (!this.ts.isApprop(domTypeCode, featCode)) {
      LowLevelException e = new LowLevelException(LowLevelException.FEAT_DOM_ERROR);
      e.addArgument(Integer.toString(domTypeCode));
      e.addArgument(this.ts.ll_getTypeForCode(domTypeCode).getName());
      e.addArgument(Integer.toString(featCode));
      e.addArgument(this.ts.ll_getFeatureForCode(featCode).getName());
      throw e;
    }
  }

  /**
   * Check the range is appropriate for this type/feature. Throws LowLevelException if it isn't.
   * 
   * @param domType
   *          domain type
   * @param ranType
   *          range type
   * @param feat
   *          feature
   */
  public final void checkTypingConditions(Type domType, Type ranType, Feature feat) {
    checkTypingConditions(((TypeImpl) domType).getCode(), ((TypeImpl) ranType).getCode(),
            ((FeatureImpl) feat).getCode());
  }

  // Assumes that parameters are valid type system codes, so check that first.
  private final void checkTypingConditions(int domTypeCode, int ranTypeCode, int featCode) {
    checkDomTypeConditions(domTypeCode, featCode);
    if (!this.ts.subsumes(this.ts.range(featCode), ranTypeCode)) {
      LowLevelException e = new LowLevelException(LowLevelException.FEAT_RAN_ERROR);
      e.addArgument(Integer.toString(featCode));
      e.addArgument(this.ts.ll_getFeatureForCode(featCode).getName());
      e.addArgument(Integer.toString(ranTypeCode));
      e.addArgument(this.ts.ll_getTypeForCode(ranTypeCode).getName());
      throw e;
    }
  }

  private final void checkFsRan(int featCode) throws LowLevelException {
    final int rangeTypeCode = this.ts.range(featCode);
    if (!ll_isRefType(rangeTypeCode)) {
      LowLevelException e = new LowLevelException(LowLevelException.FS_RAN_TYPE_ERROR);
      e.addArgument(Integer.toString(featCode));
      e.addArgument(this.ts.ll_getFeatureForCode(featCode).getName());
      e.addArgument(this.ts.ll_getTypeForCode(rangeTypeCode).getName());
      throw e;
    }
  }

  private final void checkFeature(int featureCode) {
    if (!this.ts.isFeature(featureCode)) {
      LowLevelException e = new LowLevelException(LowLevelException.INVALID_FEATURE_CODE);
      e.addArgument(Integer.toString(featureCode));
      throw e;
    }
  }

  private final void checkTypeAt(int typeCode, int fsRef) {
    if (!this.ts.isType(typeCode)) {
      LowLevelException e = new LowLevelException(LowLevelException.VALUE_NOT_A_TYPE);
      e.addArgument(Integer.toString(typeCode));
      e.addArgument(Integer.toString(fsRef));
      throw e;
    }
  }

  final void checkFsRef(int fsRef) {
    if (fsRef <= NULL_FS_REF || fsRef >= this.heap.heap.length) {
      LowLevelException e = new LowLevelException(LowLevelException.INVALID_FS_REF);
      e.addArgument(Integer.toString(fsRef));
      throw e;
    }
  }

  final boolean isFSRefType(int typeCode) {
    return ll_isRefType(typeCode);
  }

  public final boolean ll_isRefType(int typeCode) {
    if (typeCode == this.intTypeCode || typeCode == this.floatTypeCode
            || typeCode == this.stringTypeCode || typeCode == this.byteTypeCode
            || typeCode == this.booleanTypeCode || typeCode == this.shortTypeCode
            || typeCode == this.longTypeCode || typeCode == this.doubleTypeCode) {
      return false;
    }
    if (ll_getTypeSystem().ll_isStringSubtype(typeCode)) {
      return false;
    }
    return true;
  }

  public final int ll_getTypeClass(int typeCode) {
    if (typeCode == this.intTypeCode) {
      return TYPE_CLASS_INT;
    }
    if (typeCode == this.floatTypeCode) {
      return TYPE_CLASS_FLOAT;
    }
    if (this.ts.subsumes(this.stringTypeCode, typeCode)) {
      return TYPE_CLASS_STRING;
    }
    if (typeCode == this.intArrayTypeCode) {
      return TYPE_CLASS_INTARRAY;
    }
    if (typeCode == this.floatArrayTypeCode) {
      return TYPE_CLASS_FLOATARRAY;
    }
    if (typeCode == this.stringArrayTypeCode) {
      return TYPE_CLASS_STRINGARRAY;
    }
    if (typeCode == this.fsArrayTypeCode) {
      return TYPE_CLASS_FSARRAY;
    }
    if (typeCode == this.booleanTypeCode) {
      return TYPE_CLASS_BOOLEAN;
    }
    if (typeCode == this.byteTypeCode) {
      return TYPE_CLASS_BYTE;
    }
    if (typeCode == this.shortTypeCode) {
      return TYPE_CLASS_SHORT;
    }
    if (typeCode == this.longTypeCode) {
      return TYPE_CLASS_LONG;
    }
    if (typeCode == this.doubleTypeCode) {
      return TYPE_CLASS_DOUBLE;
    }
    if (typeCode == this.booleanArrayTypeCode) {
      return TYPE_CLASS_BOOLEANARRAY;
    }
    if (typeCode == this.byteArrayTypeCode) {
      return TYPE_CLASS_BYTEARRAY;
    }
    if (typeCode == this.shortArrayTypeCode) {
      return TYPE_CLASS_SHORTARRAY;
    }
    if (typeCode == this.longArrayTypeCode) {
      return TYPE_CLASS_LONGARRAY;
    }
    if (typeCode == this.doubleArrayTypeCode) {
      return TYPE_CLASS_DOUBLEARRAY;
    }

    return TYPE_CLASS_FS;
  }

  /**
   * Create a temporary (i.e., per document) FS on the heap.
   * 
   * @param type
   *          The type code of the structure to be created.
   * @exception ArrayIndexOutOfBoundsException
   *              If <code>type</code> is not a type.
   */
  public int createTempFS(int type) {
    return ll_createFS(type);
  }

  public final int ll_createFS(int typeCode) {
    return this.heap.addToTempHeap(this.fsSpaceReq[typeCode], typeCode);
  }

  public final int ll_createFS(int typeCode, boolean doCheck) {
    if (doCheck) {
      if (!this.ts.isType(typeCode) || !isCreatableType(typeCode)) {
        LowLevelException e = new LowLevelException(LowLevelException.CREATE_FS_OF_TYPE_ERROR);
        e.addArgument(Integer.toString(typeCode));
        throw e;
      }
    }
    return ll_createFS(typeCode);
  }

  /**
   * Create an instance of a subtype of AnnotationBase.
   * 
   * @param typeCode
   * @return An annotation?
   */
  public final int ll_createAnnotationBaseFS(int typeCode) {
    int addr = ll_createFS(typeCode);
    setSofaFeat(addr, this.mySofaRef);
    return addr;
  }

  public final int ll_createAnnotationBaseFS(int typeCode, boolean doCheck) {
    if (doCheck) {
      if (!this.ts.isType(typeCode) || !isCreatableType(typeCode)
              || this.ts.ll_subsumes(this.annotBaseTypeCode, typeCode)) {
        LowLevelException e = new LowLevelException(LowLevelException.CREATE_FS_OF_TYPE_ERROR);
        e.addArgument(Integer.toString(typeCode));
        throw e;
      }
    }
    return ll_createFS(typeCode);
  }

  /**
   * Create a temporary (i.e., per document) array FS on the heap.
   * 
   * @param type
   *          The type code of the array to be created.
   * @param len
   *          The length of the array to be created.
   * @exception ArrayIndexOutOfBoundsException
   *              If <code>type</code> is not a type.
   */
  public int createTempArray(int type, int len) {
    final int addr = this.heap.addToTempHeap(arrayContentOffset + len, type);
    this.heap.heap[(addr + arrayLengthFeatOffset)] = len;
    return addr;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_createArray(int, int)
   */
  public int ll_createArray(int typeCode, int arrayLength) {
    final int addr = this.heap.addToTempHeap(arrayContentOffset + arrayLength, typeCode);
    this.heap.heap[(addr + arrayLengthFeatOffset)] = arrayLength;
    return addr;
  }

  public int ll_createAuxArray(int typeCode, int arrayLength) {
    final int addr = this.heap.addToTempHeap(arrayContentOffset + 1, typeCode);
    this.heap.heap[(addr + arrayLengthFeatOffset)] = arrayLength;
    return addr;
  }

  public int ll_createByteArray(int arrayLength) {
    final int addr = ll_createAuxArray(this.byteArrayTypeCode, arrayLength);
    this.heap.heap[addr + arrayContentOffset] = this.byteHeap.reserve(arrayLength);
    return addr;
  }

  public int ll_createBooleanArray(int arrayLength) {
    final int addr = ll_createAuxArray(this.booleanArrayTypeCode, arrayLength);
    this.heap.heap[addr + arrayContentOffset] = this.byteHeap.reserve(arrayLength);
    return addr;
  }

  public int ll_createShortArray(int arrayLength) {
    final int addr = ll_createAuxArray(this.shortArrayTypeCode, arrayLength);
    this.heap.heap[addr + arrayContentOffset] = this.shortHeap.reserve(arrayLength);
    return addr;
  }

  public int ll_createLongArray(int arrayLength) {
    final int addr = ll_createAuxArray(this.longArrayTypeCode, arrayLength);
    this.heap.heap[addr + arrayContentOffset] = this.longHeap.reserve(arrayLength);
    return addr;
  }

  public int ll_createDoubleArray(int arrayLength) {
    final int addr = ll_createAuxArray(this.doubleArrayTypeCode, arrayLength);
    this.heap.heap[addr + arrayContentOffset] = this.longHeap.reserve(arrayLength);
    return addr;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_createArray(int, int, boolean)
   */
  public int ll_createArray(int typeCode, int arrayLength, boolean doChecks) {
    if (doChecks) {
      // Check typeCode, arrayLength
      if (!this.ts.isType(typeCode)) {
        LowLevelException e = new LowLevelException(LowLevelException.INVALID_TYPE_ARGUMENT);
        e.addArgument(Integer.toString(typeCode));
        throw e;
      }
      if (!isCreatableArrayType(typeCode)) {
        LowLevelException e = new LowLevelException(LowLevelException.CREATE_ARRAY_OF_TYPE_ERROR);
        e.addArgument(Integer.toString(typeCode));
        e.addArgument(this.ts.ll_getTypeForCode(typeCode).getName());
        throw e;
      }
      if (arrayLength < 0) {
        LowLevelException e = new LowLevelException(LowLevelException.ILLEGAL_ARRAY_LENGTH);
        e.addArgument(Integer.toString(arrayLength));
        throw e;
      }
    }
    return ll_createArray(typeCode, arrayLength);
  }

  private final boolean isCreatableArrayType(int typeCode) {
    final int tc = ll_getTypeClass(typeCode);
    return (tc == TYPE_CLASS_INTARRAY || tc == TYPE_CLASS_FLOATARRAY
            || tc == TYPE_CLASS_STRINGARRAY || tc == TYPE_CLASS_FSARRAY
            || tc == TYPE_CLASS_BOOLEANARRAY || tc == TYPE_CLASS_BYTEARRAY
            || tc == TYPE_CLASS_SHORTARRAY || tc == TYPE_CLASS_LONGARRAY || tc == TYPE_CLASS_DOUBLEARRAY);
  }

  public final int ll_getFSRef(FeatureStructure fsImpl) {
    if (null == fsImpl)
      return NULL;
    return ((FeatureStructureImpl) fsImpl).getAddress();
  }

  public FeatureStructure ll_getFSForRef(int fsRef) {
    return this.fsClassReg.createFS(fsRef, this);
  }

  public final int ll_getIntValue(int fsRef, int featureCode) {
    return this.heap.heap[(fsRef + this.featureOffset[featureCode])];
  }

  public final float ll_getFloatValue(int fsRef, int featureCode) {
    return int2float(ll_getIntValue(fsRef, featureCode));
  }

  public final String ll_getStringValue(int fsRef, int featureCode) {
    return this.stringHeap.getStringForCode(ll_getIntValue(fsRef, featureCode));
  }

  public final int ll_getRefValue(int fsRef, int featureCode) {
    return ll_getIntValue(fsRef, featureCode);
  }

  public final int ll_getIntValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, this.intTypeCode, featureCode);
    }
    return ll_getIntValue(fsRef, featureCode);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getFloatValue(int, int, boolean)
   */
  public final float ll_getFloatValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, this.floatTypeCode, featureCode);
    }
    return ll_getFloatValue(fsRef, featureCode);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getStringValue(int, int, boolean)
   */
  public final String ll_getStringValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, this.stringTypeCode, featureCode);
    }
    return ll_getStringValue(fsRef, featureCode);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getRefValue(int, int, boolean)
   */
  public final int ll_getRefValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkFsRefConditions(fsRef, featureCode);
    }
    return ll_getIntValue(fsRef, featureCode);
  }

  public final void ll_setIntValue(int fsRef, int featureCode, int value) {
    this.heap.heap[fsRef + this.featureOffset[featureCode]] = value;
  }

  public final void ll_setFloatValue(int fsRef, int featureCode, float value) {
    this.heap.heap[fsRef + this.featureOffset[featureCode]] = float2int(value);
  }

  public final void ll_setStringValue(int fsRef, int featureCode, String value) {
    String[] stringSet = this.ts.ll_getStringSet(this.ts.ll_getRangeType(featureCode));
    if (stringSet != null) {
      final int rc = Arrays.binarySearch(stringSet, value);
      if (rc < 0) {
        // Not a legal value.
        CASRuntimeException e = new CASRuntimeException(CASRuntimeException.ILLEGAL_STRING_VALUE);
        e.addArgument(value);
        e.addArgument(this.ts.ll_getTypeForCode(this.ts.ll_getRangeType(featureCode)).getName());
        throw e;
      }
    }
    final int stringAddr = (value == null) ? NULL : this.stringHeap.addString(value);
    final int valueAddr = fsRef + this.featureOffset[featureCode]; 
    this.heap.heap[valueAddr] = stringAddr;
  }

  public final void ll_setRefValue(int fsRef, int featureCode, int value) {
    this.heap.heap[fsRef + this.featureOffset[featureCode]] = value;
  }

  public final void ll_setIntValue(int fsRef, int featureCode, int value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, this.intTypeCode, featureCode);
    }
    ll_setIntValue(fsRef, featureCode, value);
  }

  public final void ll_setFloatValue(int fsRef, int featureCode, float value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, this.floatTypeCode, featureCode);
    }
    ll_setFloatValue(fsRef, featureCode, value);
  }

  public final void ll_setStringValue(int fsRef, int featureCode, String value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, this.stringTypeCode, featureCode);
    }
    ll_setStringValue(fsRef, featureCode, value);
  }

  public final void ll_setCharBufferValue(int fsRef, int featureCode, char[] buffer, int start,
          int length, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, this.stringTypeCode, featureCode);
    }
    ll_setCharBufferValue(fsRef, featureCode, buffer, start, length);
  }

  public final void ll_setCharBufferValue(int fsRef, int featureCode, char[] buffer, int start,
          int length) {
    final int stringCode = this.stringHeap.addCharBuffer(buffer, start, length);
    ll_setIntValue(fsRef, featureCode, stringCode);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_copyCharBufferValue(int, int, char, int)
   */
  public int ll_copyCharBufferValue(int fsRef, int featureCode, char[] buffer, int start) {
    final int stringCode = ll_getIntValue(fsRef, featureCode);
    if (stringCode == NULL) {
      return -1;
    }
    return this.stringHeap.copyCharsToBuffer(stringCode, buffer, start);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getCharBufferValueSize(int, int)
   */
  public int ll_getCharBufferValueSize(int fsRef, int featureCode) {
    final int stringCode = ll_getIntValue(fsRef, featureCode);
    if (stringCode == NULL) {
      return -1;
    }
    return this.stringHeap.getCharArrayLength(stringCode);
  }

  public final void ll_setRefValue(int fsRef, int featureCode, int value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkFsRefConditions(fsRef, featureCode);
      checkFsRef(value);
    }
    ll_setRefValue(fsRef, featureCode, value);
  }

  public final int ll_getIntArrayValue(int fsRef, int position) {
    final int pos = getArrayStartAddress(fsRef) + position;
    return this.heap.heap[pos];
  }

  public final float ll_getFloatArrayValue(int fsRef, int position) {
    final int pos = getArrayStartAddress(fsRef) + position;
    return int2float(this.heap.heap[pos]);
  }

  public final String ll_getStringArrayValue(int fsRef, int position) {
    final int pos = getArrayStartAddress(fsRef) + position;
    return getStringForCode(this.heap.heap[pos]);
  }

  public final int ll_getRefArrayValue(int fsRef, int position) {
    final int pos = getArrayStartAddress(fsRef) + position;
    return this.heap.heap[pos];
  }

  // private final void checkTypeSubsumptionAt(int fsRef, int typeCode) {
  // if (!this.ts.subsumes(typeCode, ll_getFSRefType(fsRef))) {
  // throwAccessTypeError(fsRef, typeCode);
  // }
  // }

  private void throwAccessTypeError(int fsRef, int typeCode) {
    LowLevelException e = new LowLevelException(LowLevelException.ACCESS_TYPE_ERROR);
    e.addArgument(Integer.toString(fsRef));
    e.addArgument(Integer.toString(typeCode));
    e.addArgument(this.ts.ll_getTypeForCode(typeCode).getName());
    e.addArgument(this.ts.ll_getTypeForCode(ll_getFSRefType(fsRef)).getName());
    throw e;
  }

  public final void checkArrayBounds(int fsRef, int pos) {
    final int arrayLength = getArraySize(fsRef);
    if (pos < 0 || pos >= arrayLength) {
      throw new ArrayIndexOutOfBoundsException(pos);
      // LowLevelException e = new LowLevelException(
      // LowLevelException.ARRAY_INDEX_OUT_OF_RANGE);
      // e.addArgument(Integer.toString(pos));
      // throw e;
    }
  }

  public final void checkArrayBounds(int fsRef, int pos, int length) {
    final int arrayLength = getArraySize(fsRef);
    if (pos < 0 || length < 0 || (pos + length) > arrayLength) {
      LowLevelException e = new LowLevelException(LowLevelException.ARRAY_INDEX_LENGTH_OUT_OF_RANGE);
      e.addArgument(Integer.toString(pos));
      e.addArgument(Integer.toString(length));
      throw e;
    }
  }

  private final void checkNonArrayConditions(int fsRef, int typeCode, int featureCode) {
    checkFsRef(fsRef);
    // It is now safe to do this.
    final int domTypeCode = this.heap.heap[fsRef];
    checkLowLevelParams(fsRef, domTypeCode, typeCode, featureCode);
    checkFsRef(fsRef + this.featureOffset[featureCode]);
  }

  private final void checkFsRefConditions(int fsRef, int featureCode) {
    checkFsRef(fsRef);
    final int domTypeCode = this.heap.heap[fsRef];
    checkLowLevelParams(fsRef, domTypeCode, featureCode);
    checkFsRan(featureCode);
    checkFsRef(fsRef + this.featureOffset[featureCode]);
  }

  // private final void checkArrayConditions(int fsRef, int typeCode,
  // int position) {
  // checkTypeSubsumptionAt(fsRef, typeCode);
  // // skip this next test because
  // // a) it's done implicitly in the bounds check and
  // // b) it fails for arrays stored outside of the main heap (e.g.,
  // byteArrays, etc.)
  // // checkFsRef(getArrayStartAddress(fsRef) + position);
  // checkArrayBounds(fsRef, position);
  // }

  private final void checkPrimitiveArrayConditions(int fsRef, int typeCode, int position) {
    if (typeCode != ll_getFSRefType(fsRef))
      throwAccessTypeError(fsRef, typeCode);
    checkArrayBounds(fsRef, position);
  }

  public final int ll_getIntArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, this.intArrayTypeCode, position);
    }
    return ll_getIntArrayValue(fsRef, position);
  }

  public float ll_getFloatArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, this.floatArrayTypeCode, position);
    }
    return ll_getFloatArrayValue(fsRef, position);
  }

  public String ll_getStringArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, this.stringArrayTypeCode, position);
    }
    return ll_getStringArrayValue(fsRef, position);
  }

  public int ll_getRefArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, this.fsArrayTypeCode, position);
    }
    return ll_getRefArrayValue(fsRef, position);
  }

  public void ll_setIntArrayValue(int fsRef, int position, int value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, this.intArrayTypeCode, position);
    }
    ll_setIntArrayValue(fsRef, position, value);
  }

  public void ll_setFloatArrayValue(int fsRef, int position, float value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, this.floatArrayTypeCode, position);
    }
    ll_setFloatArrayValue(fsRef, position, value);
  }

  public void ll_setStringArrayValue(int fsRef, int position, String value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, this.stringArrayTypeCode, position);
    }
    ll_setStringArrayValue(fsRef, position, value);
  }

  public void ll_setRefArrayValue(int fsRef, int position, int value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, this.fsArrayTypeCode, position);
      checkFsRef(value);
    }
    ll_setRefArrayValue(fsRef, position, value);
  }

  public void ll_setIntArrayValue(int fsRef, int position, int value) {
    final int pos = getArrayStartAddress(fsRef) + position;
    this.heap.heap[pos] = value;
  }

  public void ll_setFloatArrayValue(int fsRef, int position, float value) {
    final int pos = getArrayStartAddress(fsRef) + position;
    this.heap.heap[pos] = float2int(value);
  }

  public void ll_setStringArrayValue(int fsRef, int position, String value) {
    final int pos = getArrayStartAddress(fsRef) + position;
    final int stringCode = (value == null) ? NULL : addString(value);
    this.heap.heap[pos] = stringCode;
  }

  public void ll_setRefArrayValue(int fsRef, int position, int value) {
    final int pos = getArrayStartAddress(fsRef) + position;
    this.heap.heap[pos] = value;
  }

  public int ll_getFSRefType(int fsRef) {
    return this.heap.heap[fsRef];
  }

  public int ll_getFSRefType(int fsRef, boolean doChecks) {
    if (doChecks) {
      checkFsRef(fsRef);
      checkTypeAt(ll_getFSRefType(fsRef), fsRef);
    }
    return ll_getFSRefType(fsRef);
  }

  public LowLevelCAS getLowLevelCAS() {
    return this;
  }

  public int size() {
    return this.heap.heap.length * 6;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.admin.CASMgr#getJCasClassLoader()
   */
  public ClassLoader getJCasClassLoader() {
    if (this != this.baseCAS) {
      return this.baseCAS.getJCasClassLoader();
    }
    return this.jcasClassLoader;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.admin.CASMgr#setJCasClassLoader(java.lang.ClassLoader)
   */
  public void setJCasClassLoader(ClassLoader classLoader) {
    this.jcasClassLoader = classLoader;
  }

  public FeatureValuePath createFeatureValuePath(String featureValuePath)
          throws CASRuntimeException {
    return FeatureValuePathImpl.getFeaturePath(featureValuePath);
  }

  public void setOwner(CasOwner aCasOwner) {
    CASImpl baseCas = getBaseCAS();
    if (baseCas != this) {
      baseCas.setOwner(aCasOwner);
    } else {
      super.setOwner(aCasOwner);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.AbstractCas_ImplBase#release()
   */
  public void release() {
    CASImpl baseCas = getBaseCAS();
    if (baseCas != this) {
      baseCas.release();
    } else {
      super.release();
    }
  }

  public ByteArrayFS createByteArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return (ByteArrayFS) createFS(ll_createByteArray(length));
  }

  public BooleanArrayFS createBooleanArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return (BooleanArrayFS) createFS(ll_createBooleanArray(length));
  }

  public ShortArrayFS createShortArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return (ShortArrayFS) createFS(ll_createShortArray(length));
  }

  public LongArrayFS createLongArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return (LongArrayFS) createFS(ll_createLongArray(length));
  }

  public DoubleArrayFS createDoubleArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return (DoubleArrayFS) createFS(ll_createDoubleArray(length));
  }

  public byte ll_getByteValue(int fsRef, int featureCode) {
    return (byte) ll_getIntValue(fsRef, featureCode);
  }

  public byte ll_getByteValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, this.byteTypeCode, featureCode);
    }
    return ll_getByteValue(fsRef, featureCode);
  }

  public boolean ll_getBooleanValue(int fsRef, int featureCode) {
    return CASImpl.TRUE == ll_getIntValue(fsRef, featureCode);
  }

  public boolean ll_getBooleanValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, this.booleanTypeCode, featureCode);
    }
    return ll_getBooleanValue(fsRef, featureCode);
  }

  public short ll_getShortValue(int fsRef, int featureCode) {
    return (short) (ll_getIntValue(fsRef, featureCode));
  }

  public short ll_getShortValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, this.shortTypeCode, featureCode);
    }
    return ll_getShortValue(fsRef, featureCode);
  }

  public long ll_getLongValue(int offset) {
    return this.longHeap.getHeapValue(offset);
  }

  public long ll_getLongValue(int fsRef, int featureCode) {
    final int offset = this.heap.heap[fsRef + this.featureOffset[featureCode]];
    long val = this.longHeap.getHeapValue(offset);
    return (val);
  }

  public long ll_getLongValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, this.longTypeCode, featureCode);
    }
    return ll_getLongValue(fsRef, featureCode);
  }

  public double ll_getDoubleValue(int fsRef, int featureCode) {
    final int offset = this.heap.heap[fsRef + this.featureOffset[featureCode]];
    long val = this.longHeap.getHeapValue(offset);
    return Double.longBitsToDouble(val);
  }

  public double ll_getDoubleValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, this.doubleTypeCode, featureCode);
    }
    return ll_getDoubleValue(fsRef, featureCode);
  }

  public void ll_setBooleanValue(int fsRef, int featureCode, boolean value) {
    this.heap.heap[fsRef + this.featureOffset[featureCode]] = value ? CASImpl.TRUE : CASImpl.FALSE;
  }

  public void ll_setBooleanValue(int fsRef, int featureCode, boolean value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, this.booleanTypeCode, featureCode);
    }
    ll_setBooleanValue(fsRef, featureCode, value);
  }

  public final void ll_setByteValue(int fsRef, int featureCode, byte value) {
    this.heap.heap[fsRef + this.featureOffset[featureCode]] = value;
  }

  public void ll_setByteValue(int fsRef, int featureCode, byte value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, this.byteTypeCode, featureCode);
    }
    ll_setByteValue(fsRef, featureCode, value);
  }

  public final void ll_setShortValue(int fsRef, int featureCode, short value) {
    this.heap.heap[fsRef + this.featureOffset[featureCode]] = value;
  }

  public void ll_setShortValue(int fsRef, int featureCode, short value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, this.booleanTypeCode, featureCode);
    }
    ll_setShortValue(fsRef, featureCode, value);
  }

  public void ll_setLongValue(int fsRef, int featureCode, long value) {
    final int offset = this.longHeap.addLong(value);
    setFeatureValue(fsRef, featureCode, offset);
  }

  public void ll_setLongValue(int fsRef, int featureCode, long value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, this.longTypeCode, featureCode);
    }
    ll_setLongValue(fsRef, featureCode, value);
  }

  public void ll_setDoubleValue(int fsRef, int featureCode, double value) {
    long val = Double.doubleToLongBits(value);
    final int offset = this.longHeap.addLong(val);
    setFeatureValue(fsRef, featureCode, offset);
  }

  public void ll_setDoubleValue(int fsRef, int featureCode, double value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, this.doubleTypeCode, featureCode);
    }
    ll_setDoubleValue(fsRef, featureCode, value);
  }

  public byte ll_getByteArrayValue(int fsRef, int position) {
    final int pos = this.heap.heap[getArrayStartAddress(fsRef)];
    return this.byteHeap.getHeapValue(pos + position);
  }

  public byte ll_getByteArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, this.byteArrayTypeCode, position);
    }
    return ll_getByteArrayValue(fsRef, position);
  }

  public boolean ll_getBooleanArrayValue(int fsRef, int position) {
    final int pos = this.heap.heap[getArrayStartAddress(fsRef)];
    return CASImpl.TRUE == this.byteHeap.getHeapValue(pos + position);
  }

  public boolean ll_getBooleanArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, this.booleanArrayTypeCode, position);
    }
    return ll_getBooleanArrayValue(fsRef, position);
  }

  public short ll_getShortArrayValue(int fsRef, int position) {
    final int pos = this.heap.heap[getArrayStartAddress(fsRef)];
    return this.shortHeap.getHeapValue(pos + position);
  }

  public short ll_getShortArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, this.shortArrayTypeCode, position);
    }
    return ll_getShortArrayValue(fsRef, position);
  }

  public long ll_getLongArrayValue(int fsRef, int position) {
    final int pos = this.heap.heap[getArrayStartAddress(fsRef)];
    return this.longHeap.getHeapValue(pos + position);
  }

  public long ll_getLongArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, this.longArrayTypeCode, position);
    }
    return ll_getLongArrayValue(fsRef, position);
  }

  public double ll_getDoubleArrayValue(int fsRef, int position) {
    final int pos = this.heap.heap[getArrayStartAddress(fsRef)];
    long val = this.longHeap.getHeapValue(pos + position);
    return Double.longBitsToDouble(val);
  }

  public double ll_getDoubleArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, this.doubleArrayTypeCode, position);
    }
    return ll_getDoubleArrayValue(fsRef, position);
  }

  public void ll_setByteArrayValue(int fsRef, int position, byte value) {
    final int offset = this.heap.heap[getArrayStartAddress(fsRef)];
    this.byteHeap.setHeapValue(value, offset + position);
  }

  public void ll_setByteArrayValue(int fsRef, int position, byte value, boolean doTypeChecks) {

    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, this.byteArrayTypeCode, position);
    }
    ll_setByteArrayValue(fsRef, position, value);
  }

  public void ll_setBooleanArrayValue(int fsRef, int position, boolean b) {
    byte value = (byte) (b ? CASImpl.TRUE : CASImpl.FALSE);
    final int offset = this.heap.heap[getArrayStartAddress(fsRef)];
    this.byteHeap.setHeapValue(value, offset + position);
  }

  public void ll_setBooleanArrayValue(int fsRef, int position, boolean value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, this.booleanArrayTypeCode, position);
    }
    ll_setBooleanArrayValue(fsRef, position, value);
  }

  public void ll_setShortArrayValue(int fsRef, int position, short value) {
    final int offset = this.heap.heap[getArrayStartAddress(fsRef)];
    this.shortHeap.setHeapValue(value, offset + position);
  }

  public void ll_setShortArrayValue(int fsRef, int position, short value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, this.shortArrayTypeCode, position);
    }
    ll_setShortArrayValue(fsRef, position, value);
  }

  public void ll_setLongArrayValue(int fsRef, int position, long value) {
    final int offset = this.heap.heap[getArrayStartAddress(fsRef)];
    this.longHeap.setHeapValue(value, offset + position);
  }

  public void ll_setLongArrayValue(int fsRef, int position, long value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, this.longArrayTypeCode, position);
    }
    ll_setLongArrayValue(fsRef, position, value);
  }

  public void ll_setDoubleArrayValue(int fsRef, int position, double d) {
    final int offset = this.heap.heap[getArrayStartAddress(fsRef)];
    long value = Double.doubleToLongBits(d);
    this.longHeap.setHeapValue(value, offset + position);
  }

  public void ll_setDoubleArrayValue(int fsRef, int position, double value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, this.doubleArrayTypeCode, position);
    }
    ll_setDoubleArrayValue(fsRef, position, value);
  }

  public boolean isAnnotationType(Type t) {
    return getTypeSystem().subsumes(getAnnotationType(), t);
  }

  public boolean isAnnotationType(int t) {
    return this.ts.subsumes(this.annotTypeCode, t);
  }

  public AnnotationFS createAnnotation(Type type, int begin, int end) {
    if (this == this.baseCAS) {
      // Can't create annotation on base CAS
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INVALID_BASE_CAS_METHOD);
      e.addArgument("createAnnotation");
      throw e;
    }
    FeatureStructure fs = createFS(type);
    final int addr = ll_getFSRef(fs);
    // setSofaFeat(addr, this.mySofaRef); // already done by createFS
    setFeatureValue(addr, this.startFeatCode, begin);
    // setStartFeat(addr, begin);
    setFeatureValue(addr, this.endFeatCode, end);
    // setEndFeat(addr, end);
    return (AnnotationFS) fs;
  }

  public AnnotationIndex getAnnotationIndex() {
    return new AnnotationIndexImpl(getIndexRepository().getIndex(CAS.STD_ANNOTATION_INDEX));
  }

  public AnnotationIndex getAnnotationIndex(Type type) {
    return new AnnotationIndexImpl(getIndexRepository().getIndex(CAS.STD_ANNOTATION_INDEX, type));
  }

  /**
   * @see org.apache.uima.cas.text.CAS#getAnnotationType()
   */
  public Type getAnnotationType() {
    return this.annotType;
  }

  /**
   * @see org.apache.uima.cas.text.CAS#getEndFeature()
   */
  public Feature getEndFeature() {
    return this.endFeat;
  }

  /**
   * @see org.apache.uima.cas.text.CAS#getBeginFeature()
   */
  public Feature getBeginFeature() {
    return this.startFeat;
  }

  private AnnotationFS createDocumentAnnotation(int length) {
    // Remove any existing document annotations.
    FSIterator it = getAnnotationIndex(this.docType).iterator();
    ArrayList list = new ArrayList();
    while (it.isValid()) {
      list.add(it.get());
      it.moveToNext();
    }
    for (int i = 0; i < list.size(); i++) {
      getIndexRepository().removeFS((FeatureStructure) list.get(i));
    }
    // Create a new document annotation.
    AnnotationFS doc = createAnnotation(this.docType, 0, length);
    getIndexRepository().addFS(doc);
    // Set the language feature to the default value.
    doc.setStringValue(this.langFeat, CAS.DEFAULT_LANGUAGE_NAME);
    return doc;
  }

  // Update the document Text and document Annotation if required
  public void updateDocumentAnnotation() {
    if (!mySofaIsValid()) {
      return;
    }
    final Type SofaType = this.ts.getType(CAS.TYPE_NAME_SOFA);
    final Feature sofaString = SofaType.getFeatureByBaseName(FEATURE_BASE_NAME_SOFASTRING);
    String newDoc = getSofa(this.mySofaRef).getStringValue(sofaString);
    this.documentText = newDoc;
    if (null != newDoc)
      getDocumentAnnotation().setIntValue(getEndFeature(), newDoc.length());
  }

  public AnnotationFS getDocumentAnnotation() {
    if (this == this.baseCAS) {
      // base CAS has no document
      return null;
    }
    FSIterator it = getAnnotationIndex(this.docType).iterator();
    if (it.isValid()) {
      return (AnnotationFS) it.get();
    }
    return createDocumentAnnotation(0);
  }

  public String getDocumentLanguage() {
    if (this == this.baseCAS) {
      // base CAS has no document
      return null;
    }
    LowLevelCAS llc = this;
    final int docAnnotAddr = llc.ll_getFSRef(getDocumentAnnotation());
    return llc.ll_getStringValue(docAnnotAddr, this.langFeatCode);
  }

  public String getDocumentText() {
    return this.getSofaDataString();
  }

  public String getSofaDataString() {
    if (this == this.baseCAS) {
      // base CAS has no document
      return null;
    }
    if (mySofaIsValid()) {
      return this.getSofa(this.mySofaRef).getLocalStringData();
    }
    return null;
  }

  public FeatureStructure getSofaDataArray() {
    if (this == this.baseCAS) {
      // base CAS has no Sofa
      return null;
    }
    if (mySofaIsValid()) {
      return this.getSofa(this.mySofaRef).getLocalFSData();
    }
    return null;
  }

  public String getSofaDataURI() {
    if (this == this.baseCAS) {
      // base CAS has no Sofa
      return null;
    }
    if (mySofaIsValid()) {
      return this.getSofa(this.mySofaRef).getSofaURI();
    }
    return null;
  }

  public InputStream getSofaDataStream() {
    if (this == this.baseCAS) {
      // base CAS has no Sofa nothin
      return null;
    }
    return this.getSofaDataStream(this.getSofa());
  }

  public String getSofaMimeType() {
    if (this == this.baseCAS) {
      // base CAS has no Sofa
      return null;
    }
    if (mySofaIsValid()) {
      return this.getSofa(this.mySofaRef).getSofaMime();
    }
    return null;
  }
  
  public SofaFS getSofa() {
    if (this.mySofaRef > 0) {
      return getSofa(this.mySofaRef);
    }
    return null;
  }

  public String getViewName() {
    if (this == this.sofa2tcasMap.get(new Integer(1))) {
      return CAS.NAME_DEFAULT_SOFA;
    } else if (this.mySofaRef > 0) {
      return this.getSofa().getSofaID();
    } else {
      return null;
    }
  }

  private boolean mySofaIsValid() {
    return this.mySofaRef > 0;
  }

  protected void setDocTextFromDeserializtion(String text) {
    if (mySofaIsValid()) {
      final int SofaStringCode = ll_getTypeSystem().ll_getCodeForFeature(
              this.getTypeSystem().getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFASTRING));
      final int llsofa = getLowLevelCAS().ll_getFSRef(this.getSofa());
      getLowLevelCAS().ll_setStringValue(llsofa, SofaStringCode, text);
      this.documentText = text;
    }
  }

  public void setDocumentLanguage(String languageCode) {
    if (this == this.baseCAS) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INVALID_BASE_CAS_METHOD);
      e.addArgument("setDocumentLanguage");
      throw e;
    }
    // LowLevelCAS llc = getLowLevelCAS();
    LowLevelCAS llc = this;
    final int docAnnotAddr = llc.ll_getFSRef(getDocumentAnnotation());
    languageCode = Language.normalize(languageCode);
    llc.ll_setStringValue(docAnnotAddr, this.langFeatCode, languageCode);
  }

  public void setDocumentText(String text) {
    if (this == this.baseCAS) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INVALID_BASE_CAS_METHOD);
      e.addArgument("setDocumentText");
      throw e;
    }
    setSofaDataString(text, "text");
  }

  public void setSofaDataString(String text, String mime) throws CASRuntimeException {
    if (this == this.baseCAS) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INVALID_BASE_CAS_METHOD);
      e.addArgument("setDocumentText");
      throw e;
    }

    if (!mySofaIsValid()) {
      this.createInitialSofa(null);
    }
    // try to put the document into the SofaString ...
    // ... will fail if previously set
    getSofa(this.mySofaRef).setLocalSofaData(text);
    getLowLevelCAS().ll_setStringValue(mySofaRef, this.sofaMimeFeatCode, mime);    
  }

  public void setSofaDataArray(FeatureStructure array, String mime) throws CASRuntimeException {
    if (this == this.baseCAS) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INVALID_BASE_CAS_METHOD);
      e.addArgument("setSofaDataArray");
      throw e;
    }
    if (!mySofaIsValid()) {
      this.baseCAS.createInitialSofa(null);
    }
    // try to put the document into the SofaString ...
    // ... will fail if previously set
    getSofa(this.mySofaRef).setLocalSofaData(array);
    getLowLevelCAS().ll_setStringValue(mySofaRef, this.sofaMimeFeatCode, mime);    
  }

  public void setSofaDataURI(String uri, String mime) throws CASRuntimeException {
    if (this == this.baseCAS) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INVALID_BASE_CAS_METHOD);
      e.addArgument("setSofaDataURI");
      throw e;
    }
    if (!mySofaIsValid()) {
      this.baseCAS.createInitialSofa(null);
    }
    // try to put the document into the SofaString ...
    // ... will fail if previously set
    getSofa(this.mySofaRef).setRemoteSofaURI(uri);
    getLowLevelCAS().ll_setStringValue(mySofaRef, this.sofaMimeFeatCode, mime);    
  }

  public void setCurrentComponentInfo(ComponentInfo info) {
    // always store component info in base CAS
    this.baseCAS.componentInfo = info;
  }

  protected ComponentInfo getCurrentComponentInfo() {
    // component info in always stored in base CAS
    return this.baseCAS.componentInfo;
  }

  /**
   * @see org.apache.uima.cas.CAS#addFsToIndexes(FeatureStructure fs)
   */
  public void addFsToIndexes(FeatureStructure fs) {
    if (fs instanceof AnnotationBaseFS) {
      final CAS sofaView = ((AnnotationBaseFS) fs).getView();
      if (sofaView != this) {
        CASRuntimeException e = new CASRuntimeException(
                CASRuntimeException.ANNOTATION_IN_WRONG_INDEX);
        e.addArgument(fs.toString());
        e.addArgument(sofaView.getSofa().getSofaID());
        e.addArgument(this.getSofa().getSofaID());
        throw e;
      }
    }
    this.indexRepository.addFS(fs);
  }

  /**
   * @see org.apache.uima.cas.CAS#removeFsFromIndexes(FeatureStructure fs)
   */
  public void removeFsFromIndexes(FeatureStructure fs) {
    this.indexRepository.removeFS(fs);
  }

  public CASImpl ll_getSofaCasView(int addr) {
    // if the sofa feature for this annotation is not 0
    int sofaId = this.getSofaFeat(addr);
    if (sofaId > 0) {
      // check if same as that of current CAS view
//      if (!(this instanceof CASImpl) || (sofaId != ((CASImpl) this).getSofaRef())) {
      if (sofaId != this.getSofaRef()) {
        // Does not match. get CAS for the sofaRef feature found in
        // annotation
        return (CASImpl) this.getView(getSofa(sofaId).getSofaRef());
      }
    } else {// annotation created from low-level APIs, without setting sofa
      // feature
      // Ignore it for backwards compatibility
    }
    return this;
  }
}
