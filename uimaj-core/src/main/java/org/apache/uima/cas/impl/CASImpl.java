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
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.AbstractCas_ImplBase;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.CasOwner;
import org.apache.uima.cas.CommonArrayFS;
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
import org.apache.uima.cas.Marker;
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
import org.apache.uima.cas.impl.FSsTobeAddedback.FSsTobeAddedbackSingle;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.cas.text.Language;
import org.apache.uima.internal.util.IntListIterator;
import org.apache.uima.internal.util.PositiveIntSet;
import org.apache.uima.internal.util.PositiveIntSet_impl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.CommonArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.EmptyFloatList;
import org.apache.uima.jcas.cas.EmptyIntegerList;
import org.apache.uima.jcas.cas.EmptyList;
import org.apache.uima.jcas.cas.EmptyStringList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.JavaObjectArray;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.impl.JCasImpl;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.Level;
import org.apache.uima.util.Misc;

/**
 * Implements the CAS interfaces. This class must be public because we need to
 * be able to create instance of it from outside the package. Use at your own
 * risk. May change without notice.
 * 
 */
public class CASImpl extends AbstractCas_ImplBase implements CAS, CASMgr, LowLevelCAS {
  
  private static final boolean trace = false;
  
  // debug
  private static final AtomicInteger casIdProvider = new AtomicInteger(0);

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

  /**
   * The UIMA framework detects (unless disabled, for high performance) updates to indexed FS which update
   * key values used as keys in indexes.  Normally the framework will protect against index corruption by 
   * temporarily removing the FS from the indexes, then do the update to the feature value, and then addback
   * the changed FS.
   * <p>
   * Users can use the protectIndexes() methods to explicitly control this remove - add back cycle, for instance
   * to "batch" together several updates to multiple features in a FS.
   * <p>
   * Some build processes may want to FAIL if any unprotected updates of this kind occur, instead of having the
   * framework silently recover them.  This is enabled by having the framework throw an exception;  this is controlled
   * by this global JVM property, which, if defined, causes the framework to throw an exception rather than recover.
   * 
   */
  public static final String THROW_EXCEPTION_FS_UPDATES_CORRUPTS = "uima.exception_when_fs_update_corrupts_index";
  
  // public for test case use
  public static final boolean IS_THROW_EXCEPTION_CORRUPT_INDEX = Misc.getNoValueSystemProperty(THROW_EXCEPTION_FS_UPDATES_CORRUPTS);
  
  /**
   * Define this JVM property to enable checking for invalid updates to features which are used as 
   * keys by any index.
   * <ul>
   *   <li>The following are the same:  -Duima.check_invalid_fs_updates    and -Duima.check_invalid_fs_updates=true</li>
   * </ul> 
   */
  public static final String REPORT_FS_UPDATES_CORRUPTS = "uima.report_fs_update_corrupts_index";

  private static final boolean IS_REPORT_FS_UPDATE_CORRUPTS_INDEX = 
      IS_THROW_EXCEPTION_CORRUPT_INDEX || Misc.getNoValueSystemProperty(REPORT_FS_UPDATES_CORRUPTS);


  /**
   * Set this JVM property to false for high performance, (no checking);
   * insure you don't have the report flag (above) turned on - otherwise it will force this to "true".
   */
  public static final String DISABLE_PROTECT_INDEXES = "uima.disable_auto_protect_indexes";
  
  /**
   * the protect indexes flag is on by default, but may be turned of via setting the property.
   * 
   * This is overridden if a report is requested or the exception detection is on.
   */
  private static final boolean IS_DISABLED_PROTECT_INDEXES = 
      Misc.getNoValueSystemProperty(DISABLE_PROTECT_INDEXES) &&
      !IS_REPORT_FS_UPDATE_CORRUPTS_INDEX &&
      !IS_THROW_EXCEPTION_CORRUPT_INDEX;
    

  // this next seemingly non-sensical static block
  // is to force the classes needed by Eclipse debugging to load
  // otherwise, you get a com.sun.jdi.ClassNotLoadedException when
  // the class is used as part of formatting debugging messages
  static {
    new DebugNameValuePair(null, null);
    new DebugFSLogicalStructure();
  }

  // Static classes representing shared instance data
  // - shared data is computed once for all views
  
  /**
   * Journaling changes for computing delta cas.
   * Each instance represents one or more changes for one feature structure
   * A particular Feature Structure may have multiple FsChange instances
   *   but we attempt to minimize this
   */
  public static class FsChange {
    /** ref to the FS being modified */
    final TOP fs;
    /**
     * which feature (by offset) is modified
     */
    final BitSet featuresModified;
    
    final PositiveIntSet arrayUpdates; 

    FsChange(TOP fs) {
      this.fs = fs;
      TypeImpl ti = fs._typeImpl;
      featuresModified = (ti.highestOffset == -1) ? null : new BitSet(ti.highestOffset + 1); 
      arrayUpdates = (ti.isArray()) ? new PositiveIntSet_impl() : null;
    }
    
    void addFeatData(int v) {
      featuresModified.set(v);
    }
        
    void addArrayData(int v, int nbrOfConsecutive) {
      for (int i = 0; i < nbrOfConsecutive; i++) {
        arrayUpdates.add(v++);
      }
    }

    @Override
    public int hashCode() {
      return 31 + ((fs == null) ? 0 : fs._id);
    }

    @Override
    public boolean equals(Object obj) {
      return ((FsChange)obj).fs._id == fs._id;
    }
  }
    
  // fields shared among all CASes belong to views of a common base CAS
  static class SharedViewData {
    /**
     * map from FS ids to FSs.  
     */
    final private Id2FS id2fs = new Id2FS();

    // private SymbolTable stringTable;
    // private ArrayList stringList;
//    final private StringHeap stringHeap = new StringHeap();

//    final private ByteHeap byteHeap = new ByteHeap(); // for storing 8 bit values

//    final private ShortHeap shortHeap = new ShortHeap(); // for storing 16 bit values

//    final private LongHeap longHeap = new LongHeap(); // for storing 64 bit values
    
    // Base CAS for all views
    final private CASImpl baseCAS;

    private FeatureStructure cache_not_in_index = null; // a one item cache of a FS guaranteed to not be in any index
    
    /**
     * These fields are here, not in TypeSystemImpl, because different CASes may have different indexes but share the same type system
     * They hold the same data (constant per CAS) but are accessed with different indexes
     */
    private final BitSet featureCodesInIndexKeys = new BitSet(1024); // 128 bytes
    private final BitSet featureJiInIndexKeys = new BitSet(1024);  // indexed by JCas Feature Index, not feature code.
    
    // A map from Sofas to IndexRepositories.
    private Map<Integer, FSIndexRepository> sofa2indexMap;


    /**
     * A map from Sofa numbers to CAS views.
     * number 0 - not used
     * number 1 - used for view named "_InitialView"
     * number 2-n used for other views
     * 
     * Note: this is not reset with "Cas Reset" because views (really, their associated index repos) 
     * take a lot of setup for the indexes.
     * However, the maximum view count is reset; so creation of new views "reuses" these pre-setup indexRepos 
     * associated with these views.
     */
    private ArrayList<CASImpl> sofaNbr2ViewMap;

    /**
     * a set of instantiated sofaNames
     */
    private Set<String> sofaNameSet;

    // Flag that initial Sofa has been created
    private boolean initialSofaCreated = false;

    // Count of Views created in this cas
    // equals count of sofas except if initial view has no sofa.
    int viewCount;

    // The ClassLoader that should be used by the JCas to load the generated
    // FS cover classes for this CAS. Defaults to the ClassLoader used
    // to load the CASImpl class.
    private ClassLoader jcasClassLoader = this.getClass().getClassLoader();

    // If this CAS can be flushed (reset) or not.
    // often, the framework disables this before calling users code
    private boolean flushEnabled = true;

    // not final because set with reinit deserialization
    private TypeSystemImpl tsi;

    private ComponentInfo componentInfo;
    
    /**
     * This tracks the changes for delta cas
     * May also in the future support Journaling by component,
     * allowing determination of which component in a flow 
     * created/updated a FeatureStructure (not implmented)
     * 
     * TrackingMarkers are held on to by things outside of the 
     * Cas, to support switching from one tracking marker to 
     * another (currently not used, but designed to support
     * Component Journaling).
     * 
     * We track changes on a granularity of features
     *   and for features which are arrays, which element of the array
     *   (This last to enable efficient delta serializations of 
     *      giant arrays of things, where you've only updated a few items)
     *   
     * The FsChange doesn't store the changed data, only stores the 
     *   ref info needed to get to what was changed.  
     */
    private MarkerImpl trackingMark;
   
    
    private Map<TOP, FsChange> modifiedPreexistingFSs;
      
    /**
     * This list currently only contains at most 1 element.
     * If Journaling is implemented, it may contain an
     * element per component being journaled.
     */
    private List<MarkerImpl> trackingMarkList;
   
    /**
     * This stack corresponds to nested protectIndexes contexts. Normally should be very shallow.
     */
    private final ArrayList<FSsTobeAddedback> fssTobeAddedback = new ArrayList<FSsTobeAddedback>();
    
    /**
     * This version is for single fs use, by binary deserializers and by automatic mode
     * Only one user at a time is allowed.
     */
    private final FSsTobeAddedbackSingle fsTobeAddedbackSingle = (FSsTobeAddedbackSingle) FSsTobeAddedback.createSingle();
    /**
     * Set to true while this is in use.
     */
    private boolean fsTobeAddedbackSingleInUse = false;
    
    // used to generate FSIDs, increments by 1 for each use.  First id == 1
    private AtomicInteger fsIdGenerator = new AtomicInteger(0);

    // mostly for debug - counts # times cas is reset
    private final AtomicInteger casResets = new AtomicInteger(0);
    
    // unique ID for a created CAS view, not updated if CAS is reset and reused
    private final int casId = casIdProvider.incrementAndGet();
    
    // shared singltons, created at type system commit
    
    private EmptyFSList emptyFSList;
    private EmptyFloatList emptyFloatList;
    private EmptyIntegerList emptyIntegerList;
    private EmptyStringList emptyStringList;

    private final BinaryCasSerDes bcsd;
    
    private SharedViewData(CASImpl baseCAS, TypeSystemImpl tsi) {
      this.baseCAS = baseCAS;
      this.tsi = tsi;
      bcsd = new BinaryCasSerDes(baseCAS);
    }
    
    void clearCasReset() {
      // fss
      fsIdGenerator.set(0);
      id2fs.clear();
      cache_not_in_index = null;

      // index corruption avoidance
      fssTobeAddedback.clear();
      fsTobeAddedbackSingle.clear();
      fsTobeAddedbackSingleInUse = false; 

      // misc
      flushEnabled = true;      
      componentInfo = null;
    }
    
    void clearSofaInfo() {
      sofaNameSet.clear();
      initialSofaCreated = false;      
    }
    
    void clear() {
      clearCasReset();
      
      // type system + index spec
      tsi = null;
      featureCodesInIndexKeys.clear();
      featureJiInIndexKeys.clear();
      emptyFloatList = null;
      emptyFSList = null;
      emptyIntegerList = null;
      emptyStringList = null;
      
      clearSofaInfo();
      sofaNbr2ViewMap.clear();
      viewCount = 0;
    }
  }
  
  // -----------------------------------------------------
  // Non-shared instance data for base CAS and each view
  // -----------------------------------------------------

  // package protected to let other things share this info
  final SharedViewData svd; // shared view data
  
  
  // ----------------------------------------
  //   accessors for data in SharedViewData
  // ----------------------------------------
  
  void addSofaViewName(String id) {
    svd.sofaNameSet.add(id);
  }
  
  void setViewCount(int n) {
    svd.viewCount = n;
  }
  
  void addbackSingle(TOP fs) {
    svd.fsTobeAddedbackSingle.addback(fs);
    svd.fsTobeAddedbackSingleInUse = false;
  }
  
  void resetAddbackSingleInUse() {
    svd.fsTobeAddedbackSingleInUse = false;
  }
  
  FSsTobeAddedbackSingle getAddbackSingle() {
    if (svd.fsTobeAddedbackSingleInUse) {
      throw new RuntimeException(); // internal error
    }
    svd.fsTobeAddedbackSingleInUse = true;
    return svd.fsTobeAddedbackSingle;
  }
  
  void featureCodes_inIndexKeysAdd(int featCode, int registryIndex) {
    svd.featureCodesInIndexKeys.set(featCode);
    // skip adding if no JCas registry entry for this feature
    if (registryIndex >= 0) {
      svd.featureJiInIndexKeys.set(registryIndex);
    }
  }
  
  void maybeClearCacheNotInIndex(FeatureStructure fs) {
    if (svd.cache_not_in_index == fs) {
      svd.cache_not_in_index = null;
    }
  }
  
  /**
   * Called by feature setters which know the FS is not in any index
   * to bypass any index corruption checking, e.g., CasCopier
   * 
   * Internal use only
   * @param fsAddr the address of the feature structure
   */
  public void setCacheNotInIndex(FeatureStructure fs) {
    svd.cache_not_in_index = fs;
  }

  // The index repository. Referenced by XmiCasSerializer
  FSIndexRepositoryImpl indexRepository;

  // the sofaFS this view is based on
  // SofaFS mySofa;
  /**
   * The Feature Structure for the sofa FS for this view, or
   * null
   * //-1 if the sofa FS is for the initial view, or
   * // 0 if there is no sofa FS - for instance, in the "base cas"
   */
  private Sofa mySofaRef = null;

  JCas jcas = null;

  /**
   * Copies of frequently accessed data pulled up for 
   * locality of reference - only an optimization
   *   - each value needs to be reset appropriately 
   *   - getters check for null, and if null, do the get.
   */
  
  private TypeSystemImpl tsi;
  
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.admin.CASMgr#setCAS(org.apache.uima.cas.CAS)
   *      Internal use Never called Kept because it's in the interface.
   */
  @Override
  public void setCAS(CAS cas) {
  }

  // CASImpl(TypeSystemImpl typeSystem) {
  // this(typeSystem, DEFAULT_INITIAL_HEAP_SIZE);
  // }

  // // Reference existing CAS
  // // For use when creating views of the CAS
  // CASImpl(CAS cas) {
  // this.setCAS(cas);
  // this.useFSCache = false;
  // initTypeVariables();
  // }

  /*
   * Configure a new (base view) CASImpl, **not a new view** typeSystem can be
   * null, in which case a new instance of TypeSystemImpl is set up, but not
   * committed. If typeSystem is not null, it is committed (locked). ** Note: it
   * is assumed that the caller of this will always set up the initial view **
   * by calling
   */

  public CASImpl(TypeSystemImpl typeSystem) {
    super();
    TypeSystemImpl ts;
    final boolean externalTypeSystem = (typeSystem != null);

    if (externalTypeSystem) {
      ts = typeSystem;
    } else {
      ts = new TypeSystemImpl(); // creates also new CASMetadata and
      // FSClassRegistry instances
    }

    this.svd = new SharedViewData(this, ts);
//    this.svd.baseCAS = this;

//    this.svd.heap = new Heap(initialHeapSize);

    if (externalTypeSystem) {
      commitTypeSystem();
    }
   
    this.svd.sofa2indexMap = new HashMap<Integer, FSIndexRepository>();
    this.svd.sofaNbr2ViewMap = new ArrayList<>();
    this.svd.sofaNameSet = new HashSet<String>();
    this.svd.initialSofaCreated = false;
    this.svd.viewCount = 0;
    
    clearTrackingMarks();
  }

  public CASImpl() {
    this((TypeSystemImpl) null);
  }

  // In May 2007, appears to have 1 caller, createCASMgr in Serialization class,
  // could have out-side the framework callers because it is public.
  public CASImpl(CASMgrSerializer ser) {
    this(ser.getTypeSystem());
    checkInternalCodes(ser);
    // assert(ts != null);
    // assert(getTypeSystem() != null);
    this.indexRepository = ser.getIndexRepository(this);
  }

  // Use this when creating a CAS view
  CASImpl(CASImpl cas, SofaFS aSofa) {
    
    // these next fields are final and must be set in the constructor
    this.svd = cas.svd;

    this.mySofaRef = (Sofa) aSofa;

    // get the indexRepository for this Sofa
    this.indexRepository = (this.mySofaRef == null) ? 
        (FSIndexRepositoryImpl) cas.getSofaIndexRepository(1) : 
        (FSIndexRepositoryImpl) cas.getSofaIndexRepository(aSofa);
    if (null == this.indexRepository) {
      // create the indexRepository for this CAS
      // use the baseIR to create a lightweight IR copy
      FSIndexRepositoryImpl baseIndexRepo = (FSIndexRepositoryImpl) cas.getBaseIndexRepository();
      this.indexRepository = new FSIndexRepositoryImpl(this, baseIndexRepo);
      // the index creation depends on "indexRepository" already being set
      baseIndexRepo.name2indexMap.keySet().stream().forEach(key -> this.indexRepository.createIndex(baseIndexRepo, key));
      this.indexRepository.commit();
      // save new sofa index
      if (this.mySofaRef == null) {
        cas.setSofaIndexRepository(1, this.indexRepository);
      } else {
        cas.setSofaIndexRepository(aSofa, this.indexRepository);
      }
    }
  }

  // Use this when creating a CAS view
  void refreshView(CAS cas, SofaFS aSofa) {

    if (aSofa != null) {
      // save address of SofaFS
      this.mySofaRef = (Sofa) aSofa;
    } else {
      // this is the InitialView
      this.mySofaRef = null;
    }

    // toss the JCas, if it exists
    this.jcas = null;

    // create the indexRepository for this Sofa
    final FSIndexRepositoryImpl baseIndexRepo = (FSIndexRepositoryImpl) ((CASImpl) cas).getBaseIndexRepository();
    this.indexRepository = new FSIndexRepositoryImpl(this,baseIndexRepo);
    // the index creation depends on "indexRepository" already being set
    baseIndexRepo.name2indexMap.keySet().stream().forEach(key -> this.indexRepository.createIndex(baseIndexRepo, key));

    this.indexRepository.commit();
    // save new sofa index
    if (this.mySofaRef == null) {
      ((CASImpl) cas).setSofaIndexRepository(1, this.indexRepository);
    } else {
      ((CASImpl) cas).setSofaIndexRepository(aSofa, this.indexRepository);
    }
  }

  private void checkInternalCodes(CASMgrSerializer ser) throws CASAdminException {
    if ((ser.topTypeCode > 0)
        && (ser.topTypeCode != ((TypeImpl) getTypeSystemImpl().getTopType()).getCode())) {
      throw new CASAdminException(CASAdminException.DESERIALIZATION_ERROR);
    }
    if (ser.featureOffsets == null) {
      return;
    }
//    if (ser.featureOffsets.length != this.svd.casMetadata.featureOffset.length) {
//      throw new CASAdminException(CASAdminException.DESERIALIZATION_ERROR);
//    }
    TypeSystemImpl tsi = getTypeSystemImpl();
    for (int i = 1; i < ser.featureOffsets.length; i++) {
      FeatureImpl fi = tsi.getFeatureForCode_checked(i);
      int adjOffset = fi.isInInt ? 0 : fi.getRangeImpl().nbrOfUsedIntDataSlots;
      if (ser.featureOffsets[i] != (fi.getOffset() + adjOffset)) {
        throw new CASAdminException(CASAdminException.DESERIALIZATION_ERROR);
      }
    }
  }

  @Override
  public void enableReset(boolean flag) {
    this.svd.flushEnabled = flag;
  }

  @Override
  public TypeSystem getTypeSystem() {
    return getTypeSystemImpl();
  }

  public TypeSystemImpl getTypeSystemImpl() {
    if (tsi == null) {
      tsi = this.svd.tsi;
    }
    return this.tsi;
  }
  
  void installTypeSystem(TypeSystemImpl ts) {
    this.svd.tsi = ts;
    for (CASImpl view : this.svd.sofaNbr2ViewMap) {
      view.tsi = null; // reset previous cache
    }
    this.getBaseCAS().tsi = null;  // reset base cas Cache too
  }
  
  @Override
  public ConstraintFactory getConstraintFactory() {
    return ConstraintFactory.instance();
  }

  /**
   * Create the appropriate Feature Structure Java instance
   *   - from whatever the generator for this type specifies.
   *   
   * @param type the type to create
   * @return a Java object representing the FeatureStructure impl in Java.
   */
  @Override
  public <T extends TOP> T createFS(Type type) {
    final TypeImpl ti = (TypeImpl) type;
    if (!ti.isCreatableAndNotBuiltinArray()) {
      throw new CASRuntimeException(CASRuntimeException.NON_CREATABLE_TYPE, type.getName(), "CAS.createFS()");
    }
    return createFSAnnotCheck(ti);
  }
  
  private <T extends FeatureStructureImplC> T createFSAnnotCheck(TypeImpl ti) {
    if (ti.isAnnotationBaseType()) {
      if (this.isBaseCas()) {    
        throw new CASRuntimeException(CASRuntimeException.DISALLOW_CREATE_ANNOTATION_IN_BASE_CAS, ti.getName());
      }
      getSofaRef();  // create sofa in _InitialView if needed
    }
  
    T fs = (T) (((FsGenerator)getFsGenerator(ti.getCode())).createFS(ti, this));
    svd.cache_not_in_index = fs;
    return fs;
  } 
  
  public int ll_createFSAnnotCheck(int typeCode) {
    return createFSAnnotCheck(getTypeSystemImpl().getTypeForCode(typeCode))._id;
  }
  
  public TOP createArray(TypeImpl type, int arrayLength) {
    if (((TypeImpl_array)type).getComponentType().isPrimitive()) {
      return (((FsGeneratorArray)getFsGenerator(type.getCode())).createFS(type, this, arrayLength));
    }
    return (TOP) createArrayFS(arrayLength);
  }

  @Override
  public ArrayFS createArrayFS(int length) {
    checkArrayPreconditions(length);
    return (ArrayFS)           (((FsGeneratorArray)getFsGenerator(TypeSystemImpl.fsArrayTypeCode))
        .createFS(getTypeSystemImpl().fsArrayType, this, length));
  }

  @Override
  public IntArrayFS createIntArrayFS(int length) {
    checkArrayPreconditions(length);
    return (IntArrayFS)        (((FsGeneratorArray)getFsGenerator(TypeSystemImpl.intArrayTypeCode))
        .createFS(getTypeSystemImpl().intArrayType, this, length));
  }

  @Override
  public FloatArrayFS createFloatArrayFS(int length) {
    checkArrayPreconditions(length);
    return (FloatArrayFS)      (((FsGeneratorArray)getFsGenerator(TypeSystemImpl.floatArrayTypeCode))
        .createFS(getTypeSystemImpl().floatArrayType, this, length));
  }

  @Override
  public StringArrayFS createStringArrayFS(int length) {
    checkArrayPreconditions(length);
    return (StringArrayFS)     (((FsGeneratorArray)getFsGenerator(TypeSystemImpl.stringArrayTypeCode))
        .createFS(getTypeSystemImpl().stringArrayType, this, length));
  }
  
  public JavaObjectArray createJavaObjectArrayFS(int length) {
    checkArrayPreconditions(length);
    return (JavaObjectArray)   (((FsGeneratorArray)getFsGenerator(TypeSystemImpl.stringArrayTypeCode))
        .createFS(getTypeSystemImpl().javaObjectArrayType, this, length));
  }


  // return true if only one sofa and it is the default text sofa
  public boolean isBackwardCompatibleCas() {
    // check that there is exactly one sofa
    if (this.svd.viewCount != 1) {
      return false;
    }

    if (!this.svd.initialSofaCreated) {
      return false;
    }
    
    Sofa sofa = (Sofa) this.getInitialView().getSofa();

    // check for mime type exactly equal to "text"
    String sofaMime = sofa.getMimeType();
    if (!"text".equals(sofaMime)) {
      return false;
    }
    // check that sofaURI and sofaArray are not set
    String sofaUri = sofa.getSofaURI();
    if (sofaUri != null) {
      return false;
    }
    TOP sofaArray = sofa.getSofaArray();
    if (sofaArray != null) {
      return false;
    }
    // check that name is NAME_DEFAULT_SOFA
    String sofaname = sofa.getSofaID();
    return NAME_DEFAULT_SOFA.equals(sofaname);
  }

  int getBaseSofaCount() {
    return this.svd.viewCount;
  }

  FSIndexRepository getSofaIndexRepository(SofaFS aSofa) {
    return getSofaIndexRepository(aSofa.getSofaRef());
  }

  FSIndexRepository getSofaIndexRepository(int aSofaRef) {
    return this.svd.sofa2indexMap.get(aSofaRef);
  }

  void setSofaIndexRepository(SofaFS aSofa, FSIndexRepository indxRepos) {
    setSofaIndexRepository(aSofa.getSofaRef(), indxRepos);
  }

  void setSofaIndexRepository(int aSofaRef, FSIndexRepository indxRepos) {
    this.svd.sofa2indexMap.put(Integer.valueOf(aSofaRef), indxRepos);
  }

  /**
   * @deprecated
   */
  @Override
  @Deprecated
  public SofaFS createSofa(SofaID sofaID, String mimeType) {
    // extract absolute SofaName string from the ID
    SofaFS aSofa = createSofa(sofaID.getSofaID(), mimeType);
    getView(aSofa); // will create the view, needed to make the
    // resetNoQuestions and other things that
    // iterate over views work.
    return aSofa;
  }

  Sofa createSofa(String sofaName, String mimeType) {
    return createSofa(++this.svd.viewCount, sofaName, mimeType);
  }
  
  Sofa createSofa(int sofaNum, String sofaName, String mimeType) {  
    if (this.svd.sofaNameSet.contains(sofaName)) {
      throw new CASRuntimeException(CASRuntimeException.SOFANAME_ALREADY_EXISTS, sofaName);
    }

    Sofa sofa = new Sofa(
        getTypeSystemImpl().sofaType, 
        this, 
        sofaNum,
        sofaName,
        mimeType);
    
    this.getBaseIndexRepository().addFS(sofa);
    this.svd.sofaNameSet.add(sofaName);
    return sofa;
  }

  Sofa createInitialSofa(String mimeType) { 
    Sofa sofa = createSofa(1, CAS.NAME_DEFAULT_SOFA, mimeType);

    registerInitialSofa();
    this.mySofaRef = sofa;
    return sofa;
  }

  void registerInitialSofa() {
    this.svd.initialSofaCreated = true;
  }

  boolean isInitialSofaCreated() {
    return this.svd.initialSofaCreated;
  }

  /**
   * @deprecated
   */
  @Override
  @Deprecated
  public SofaFS getSofa(SofaID sofaID) {
    // extract absolute SofaName string from the ID
    return getSofa(sofaID.getSofaID());
  }

  private SofaFS getSofa(String sofaName) {
    FSIterator<Sofa> iterator = this.svd.baseCAS.getSofaIterator();
    while (iterator.hasNext()) {
      SofaFS sofa = iterator.next();
      if (sofaName.equals(sofa.getSofaID())) {
        return sofa;
      }
    }
    throw new CASRuntimeException(CASRuntimeException.SOFANAME_NOT_FOUND, sofaName);
  }

  SofaFS getSofa(int sofaRef) {
    SofaFS aSofa = (SofaFS) this.ll_getFSForRef(sofaRef);
    if (aSofa == null) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFAREF_NOT_FOUND);
      throw e;
    }
    return aSofa;
  }
  
  
  public int ll_getSofaNum(int sofaRef) {
    return ll_getIntValue(sofaRef, TypeSystemImpl.sofaNumFeatCode);
  }
  public String ll_getSofaID(int sofaRef) {
    return ll_getStringValue(sofaRef, TypeSystemImpl.sofaIdFeatCode);
  }
  
  public String ll_getSofaDataString(int sofaAddr) {
    return ll_getStringValue(sofaAddr, TypeSystemImpl.sofaStringFeatCode);
  }

  public CASImpl getBaseCAS() {
    return this.svd.baseCAS;
  }

  @Override
  public <T extends SofaFS> FSIterator<T> getSofaIterator() {
    FSIndex<T> sofaIndex = this.svd.baseCAS.indexRepository.<T>getIndex(CAS.SOFA_INDEX_NAME);
    return sofaIndex.iterator();
  }

  // For internal use only
  public Sofa getSofaRef() {
    if (this.mySofaRef == null) {
      // create the SofaFS for _InitialView ...
      // ... and reset mySofaRef to point to it
      this.mySofaRef = this.createInitialSofa(null);
    }
    return this.mySofaRef;
  }
  
  

  // For internal use only
  public InputStream getSofaDataStream(SofaFS aSofa) {
        
    Sofa sofa = (Sofa) aSofa;    
    String sd = sofa.getLocalStringData();

    if (null != sd) {
      ByteArrayInputStream bis;
      try {
        bis = new ByteArrayInputStream(sd.getBytes("UTF-8"));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);  // never happen 
      }
      return bis;
      
    } else if (null != aSofa.getLocalFSData()) {
      TOP fs = (TOP) sofa.getLocalFSData();
      ByteBuffer buf = null;
      switch(fs._getTypeCode()) {
      
      case TypeSystemImpl.stringArrayTypeCode: {
        StringBuilder sb = new StringBuilder();
        final String[] theArray = ((StringArray) fs)._getTheArray();
        
        for (int i = 0; i < theArray.length; i++) {
          if (i != 0) {
            sb.append('\n');
          }
          sb.append(theArray[i]);
        }
        try {
          return new ByteArrayInputStream(sb.toString().getBytes("UTF-8") );
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);  // never happen 
        }
      }
      case TypeSystemImpl.intArrayTypeCode: {
        final int[] theArray = ((IntegerArray) fs)._getTheArray();
        (buf = ByteBuffer.allocate(theArray.length * 4)).asIntBuffer().put(theArray, 0, theArray.length);
        break;
      }
        
      case TypeSystemImpl.floatArrayTypeCode: {
        final float[] theArray = ((FloatArray) fs)._getTheArray();
        (buf = ByteBuffer.allocate(theArray.length * 4)).asFloatBuffer().put(theArray, 0, theArray.length);
        break;
      }
        
      case TypeSystemImpl.byteArrayTypeCode: {
        final byte[] theArray = ((ByteArray) fs)._getTheArray();
        buf = ByteBuffer.wrap(theArray);
        break;
      }

      case TypeSystemImpl.shortArrayTypeCode: {
        final short[] theArray = ((ShortArray) fs)._getTheArray();
        (buf = ByteBuffer.allocate(theArray.length * 2)).asShortBuffer().put(theArray, 0, theArray.length);
        break;
      }

      case TypeSystemImpl.longArrayTypeCode: {
        final long[] theArray = ((LongArray) fs)._getTheArray();
        (buf = ByteBuffer.allocate(theArray.length * 8)).asLongBuffer().put(theArray, 0, theArray.length);
         break;
      }
         
      case TypeSystemImpl.doubleArrayTypeCode: {
        final double[] theArray = ((DoubleArray) fs)._getTheArray();
        (buf = ByteBuffer.allocate(theArray.length * 8)).asDoubleBuffer().put(theArray, 0, theArray.length);
        break;
      }

      default:
        assert(false);
      }
      
      ByteArrayInputStream bis = new ByteArrayInputStream(buf.array());
      return bis;

    } else if (null != aSofa.getSofaURI()) {
      URL url;
      try {
        url = new URL(aSofa.getSofaURI());
        return url.openStream();
      } catch (IOException exc) {
        throw new CASRuntimeException(CASRuntimeException.SOFADATASTREAM_ERROR, exc.getMessage());
      }
    } 
    return null;
  }

  @Override
  public<T extends FeatureStructure> FSIterator<T> createFilteredIterator(FSIterator<T> it, FSMatchConstraint cons) {
    return new FilteredIterator<T>(it, cons);
  }

  public TypeSystemImpl commitTypeSystem() {
    TypeSystemImpl ts = getTypeSystemImpl();
    // For CAS pools, the type system could have already been committed
    // Skip the initFSClassReg if so, because it may have been updated to a JCas
    // version by another CAS processing in the pool
    // @see org.apache.uima.cas.impl.FSClassRegistry

    // avoid race: two instances of a CAS from a pool attempting to commit the
    // ts
    // at the same time
    synchronized (ts) {
      if (!ts.isCommitted()) {
        TypeSystemImpl tsc = ts.commit();
        if (tsc != ts) {
          installTypeSystem(tsc);
          ts = tsc;
        }
      }
    }       
    createIndexRepository();
    return ts;
  }

  private void createIndexRepository() {
    if (!this.getTypeSystemMgr().isCommitted()) {
      throw new CASAdminException(CASAdminException.MUST_COMMIT_TYPE_SYSTEM);
    }
    if (this.indexRepository == null) {
      this.indexRepository = new FSIndexRepositoryImpl(this);
    }
  }

  @Override
  public FSIndexRepositoryMgr getIndexRepositoryMgr() {
    // assert(this.cas.getIndexRepository() != null);
    return this.indexRepository;
  }

  /**
   * @deprecated
   * @param fs -
   */
  @Deprecated
  public void commitFS(FeatureStructure fs) {
    getIndexRepository().addFS(fs);
  }

  @Override
  public FeaturePath createFeaturePath() {
    return new FeaturePathImpl();
  }

  // Implement the ConstraintFactory interface.

  /**
   * @see org.apache.uima.cas.admin.CASMgr#getTypeSystemMgr()
   */
  @Override
  public TypeSystemMgr getTypeSystemMgr() {
    return getTypeSystemImpl();
  }

  @Override
  public void reset() {
    if (!this.svd.flushEnabled) {
      throw new CASAdminException(CASAdminException.FLUSH_DISABLED);
    }
    if (this == this.svd.baseCAS) {
      resetNoQuestions();
      return;
    }
    // called from a CAS view.
    // clear CAS ...
    this.svd.baseCAS.resetNoQuestions();
  }

  /*
   * iterated reset - once per view of a CAS except for the base CAS
   */
  private void resetView() {
    this.indexRepository.flush();
    // if (this.mySofaRef > 0 && this.getSofa().getSofaRef() == 1) {
    // // indicate no Sofa exists for the initial view
    // this.mySofaRef = -1;
    // } else {
    // this.mySofaRef = 0;
    // }
    // if (this.jcas != null) {
    // try {
    // JCasImpl.clearData(this);
    // } catch (CASException e) {
    // CASAdminException cae = new
    // CASAdminException(CASAdminException.JCAS_ERROR);
    // cae.addArgument(e.getMessage());
    // throw cae;
    // }
    // }
  }

  public void resetNoQuestions() {
    svd.casResets.incrementAndGet();
    svd.clearCasReset();
     if (trace) {
       System.out.println("CAS Reset in thread " + Thread.currentThread().getName() +
          " for CasId = " + getCasId() + ", new reset count = " + svd.casResets.get());
    }
    int numViews = this.getBaseSofaCount();
    // Flush indexRepository for all views
    for (int view = 1; view <= numViews; view++) {
      CASImpl tcas = (CASImpl) ((view == 1) ? getInitialView() : getView(view));
      if (tcas != null) {
        tcas.indexRepository.flush();
        
        // mySofaRef = -1 is a flag in initial view that sofa has not been set.
        // For the initial view, it is possible to not have a sofa - it is set
        // "lazily" upon the first need.
        // all other views always have a sofa set. The sofaRef is set to 0,
        // but will be set to the actual sofa addr in the cas when the view is
        // initialized.
        
        tcas.mySofaRef = null;  // was in v2: (1 == view) ? -1 : 0;
      }
    }
    clearTrackingMarks();
    

    this.indexRepository.flush();  // for base view, other views flushed above
    this.svd.clearSofaInfo();  // but keep initial view, and other views
                               // because setting up the index infrastructure is expensive
    this.svd.viewCount = 1;  // initial view
  }

  /**
   * @deprecated Use {@link #reset reset()}instead.
   */
  @Override
  @Deprecated
  public void flush() {
    reset();
  }
  
  @Override
  public FSIndexRepository getIndexRepository() {
    if (this == this.svd.baseCAS) {
      // BaseCas has no indexes for users
      return null;
    }
    if (this.indexRepository.isCommitted()) {
      return this.indexRepository;
    }
    return null;
  }

  FSIndexRepository getBaseIndexRepository() {
    if (this.svd.baseCAS.indexRepository.isCommitted()) {
      return this.svd.baseCAS.indexRepository;
    }
    return null;
  }
  
  FSIndexRepositoryImpl getBaseIndexRepositoryImpl() {
    return this.svd.baseCAS.indexRepository;
  }
  
  void addSofaFsToIndex(SofaFS sofa) {
    this.svd.baseCAS.getBaseIndexRepository().addFS(sofa);
  }

  void registerView(Sofa aSofa) {
    this.mySofaRef = aSofa;
  }


  /**
   * @see org.apache.uima.cas.CAS#fs2listIterator(FSIterator)
   */
  @Override
  public <T extends FeatureStructure> ListIterator<T> fs2listIterator(FSIterator<T> it) {
    return new FSListIteratorImpl<T>(it);
  }

  /**
   * @see org.apache.uima.cas.admin.CASMgr#getCAS()
   */
  @Override
  public CAS getCAS() {
    if (this.indexRepository.isCommitted()) {
      return this;
    }
    throw new CASAdminException(CASAdminException.MUST_COMMIT_INDEX_REPOSITORY);
  }

  // public void setFSClassRegistry(FSClassRegistry fsClassReg) {
  // this.svd.casMetadata.fsClassRegistry = fsClassReg;
  // }

  
  // JCasGen'd cover classes use this to add their generators to the class
  // registry
  // Note that this now (June 2007) a no-op for JCasGen'd generators
  // Also used in JCas initialization to copy-down super generators to subtypes
  // as needed
  public FSClassRegistry getFSClassRegistry() {
    return getTypeSystemImpl().getFSClassRegistry();
  }

  
  private void clearTrackingMarks() {
    // resets all markers that might be held by things outside the Cas
    // Currently (2009) this list has a max of 1 element
    // Future impl may have one element per component for component Journaling
    if (this.svd.trackingMarkList != null) {
      for (int i=0; i < this.svd.trackingMarkList.size(); i++) {
        this.svd.trackingMarkList.get(i).isValid = false;
      }
    }

    this.svd.trackingMark = null;
    if (null != this.svd.modifiedPreexistingFSs) this.svd.modifiedPreexistingFSs.clear();
  }

  /**
   * 
   * @param fs the Feature Structure being updated
   * @param fi the Feature of fs being updated, or null if fs is an array 
   * @param arrayIndexStart
   * @param nbrOfConsecutive
   */
  private void logFSUpdate(TOP fs, FeatureImpl fi, int arrayIndexStart, int nbrOfConsecutive) {
   
    //log the FS
    
    final Map<TOP, FsChange> changes = this.svd.modifiedPreexistingFSs;
    
    //create or use last FsChange element
    
    FsChange change = changes.computeIfAbsent(fs, key -> new FsChange(key));
          
    if (fi == null) {
      if (arrayIndexStart < 0) {
        throw new UIMARuntimeException(UIMARuntimeException.INTERNAL_ERROR);
      }
      change.addArrayData(arrayIndexStart, nbrOfConsecutive);
    } else {
      change.addFeatData(fi.getOffset());  
    }
  }
    
  private void logFSUpdate(TOP fs, FeatureImpl fi) {
    logFSUpdate(fs, fi, -1, -1); // indicate non-array call
  }

  
  /**
   * This is your link from the low-level API to the high-level API. Use this
   * method to create a FeatureStructure object from an address. Note that the
   * reverse is not supported by public APIs (i.e., there is currently no way to
   * get at the address of a FeatureStructure. Maybe we will need to change
   * that.
   * 
   * The "create" in "createFS" is a misnomer - the FS must already be created.
   * 
   * @param id The id of the feature structure to be created.
   * @param <T> The Java class associated with this feature structure
   * @return A FeatureStructure object.
   */
  public <T extends TOP> T createFS(int id) {
    return getFsFromId_checked(id);
  }
  
  public int getArraySize(CommonArrayFS fs) {
    return fs.size();
  }
  
  @Override
  public int ll_getArraySize(int id) {
    return getArraySize(getFsFromId_checked(id));
  }
      
  /*
   * Support code for JCas setters
   * Only called from JCasGen'd code
   * 
   */
  public void setWithCheckAndJournalJFRI(TOP fs, int jcasFieldRegistryIndex, Runnable setter) {
    boolean wasRemoved = checkForInvalidFeatureSettingJFRI(fs, jcasFieldRegistryIndex);
    setter.run();
    if (wasRemoved) {
      maybeAddback(fs);
    }
    maybeLogUpdateJFRI(fs, jcasFieldRegistryIndex);
  }
  
  public void setWithCheckAndJournal(TOP fs, int featCode, Runnable setter) {
    boolean wasRemoved = checkForInvalidFeatureSetting(fs, featCode);
    setter.run();
    if (wasRemoved) {
      maybeAddback(fs);
    }
    maybeLogUpdate(fs, featCode);
  }

    
//  public void setWithCheck(FeatureStructureImplC fs, FeatureImpl feat, Runnable setter) {
//    boolean wasRemoved = checkForInvalidFeatureSetting(fs, feat);
//    setter.run();
//    if (wasRemoved) {
//      maybeAddback(fs);
//    }
//  }
  
  /**
   * This method called by setters in JCas gen'd classes when 
   * the setter must check for journaling
   * @param fs
   * @param fi
   * @param setter
   */
  public void setWithJournal(FeatureStructureImplC fs, FeatureImpl fi, Runnable setter) {
    setter.run();
    maybeLogUpdate(fs, fi);
  }

  public void setWithJournalJFRI(FeatureStructureImplC fs, int jcasFieldRegistryIndex, Runnable setter) {
    setter.run();
    maybeLogUpdateJFRI(fs, jcasFieldRegistryIndex);
  }

  
  
  /**
   * 
   * @param fs the Feature Structure being updated
   * @param feat the feature of fs being updated, or null if fs is a primitive array
   * @param i the index being updated
   */
  public void maybeLogArrayUpdate(FeatureStructureImplC fs, FeatureImpl feat, int i) {
    if (this.svd.trackingMark != null && !this.svd.trackingMark.isNew(fs.id())) {
      this.logFSUpdate((TOP) fs, feat, i, 1);
    }    
  }
  
  public void maybeLogUpdate(FeatureStructureImplC fs, FeatureImpl feat) {
    if (this.svd.trackingMark != null && !this.svd.trackingMark.isNew(fs.id())) {
      this.logFSUpdate((TOP) fs, feat);
    }
  }
  
  public void maybeLogUpdateJFRI(FeatureStructureImplC fs, int jcasFieldRegistryIndex) {
    if (this.svd.trackingMark != null && !this.svd.trackingMark.isNew(fs.id())) {
      this.logFSUpdate((TOP) fs, getFeatFromJCasFieldRegistryIndex(jcasFieldRegistryIndex));
    }
  }


  public void maybeLogUpdate(FeatureStructureImplC fs, int featCode) {
    if (this.svd.trackingMark != null && !this.svd.trackingMark.isNew(fs.id())) {
      this.logFSUpdate((TOP)fs, getTypeSystemImpl().getFeatureForCode(featCode));
    }
  }
    
  /**
   * Common setter code for features in Feature Structures
   * 
   * These come in two styles:  one with int values, one with Object values
   */
  
  /**
   * low level setter 
   *  
   * @param fs      the feature structure
   * @param feat    the feature to set
   * @param value -
   */
  
  public void setFeatureValue(FeatureStructureImplC fs, FeatureImpl feat, int value) {
    fs.setIntValue(feat, value);
//    boolean wasRemoved = checkForInvalidFeatureSetting(fs, feat.getCode());
//    fs._intData[feat.getAdjustedOffset()] = value;
//    if (wasRemoved) {
//      maybeAddback(fs);
//    }
//    maybeLogUpdate(fs, feat);
  }

  /**
   * version for longs, uses two slots
   * Only called from FeatureStructureImplC after determining 
   *   there is no local field to use
   * Is here because of of 3 calls to things in this class
   * @param fs      the feature structure
   * @param feat    the feature to set
   * @param value -
   */
  public void setFeatureValue(FeatureStructureImplC fsIn, FeatureImpl feat, int v1, int v2) {
    TOP fs = (TOP) fsIn;
    boolean wasRemoved = checkForInvalidFeatureSetting(fs, feat.getCode());
    int offset = feat.getAdjustedOffset();
    fs._intData[offset] = v1;
    fs._intData[offset + 1] = v2;
    if (wasRemoved) {
      maybeAddback(fs);
    }
    maybeLogUpdate(fs, feat);
  }
  
  /**
   * This is the common point where all low-level operations to set features come through
   * It implements the check for invalid feature setting and potentially the addback.
   *   (String objects may be in keys) 
   * @param fs      the feature structure
   * @param feat    the feature to set
   * @param value -
   */
  
  public void setFeatureValue(FeatureStructureImplC fs, FeatureImpl feat, Object value) {
    fs.setRefValueCJ(feat, value);
//    boolean wasRemoved = checkForInvalidFeatureSetting(fs, feat.getCode());
//    fs._refData[feat.getAdjustedOffset()] = value;
//    if (wasRemoved) {
//      maybeAddback(fs);
//    }
//    maybeLogUpdate(fs, feat);
  }

//  /**
//   * Set the value of a feature of a FS without checking for index corruption
//   * (typically because the feature isn't one that can be used as a key, or
//   * the context is one where the FS is being created, and is guaranteed not to be in any index (yet))
//   * 
//   * @param fs      The FS.
//   * @param feat    The feature.
//   * @param value     The new value for the feature.
//   */
//  void setFeatureValueNoIndexCorruptionCheck(FeatureStructureImplC fs, FeatureImpl feat, int value) {
//    fs._intData[feat.getAdjustedOffset()] = value;   
//    maybeLogUpdate(fs, feat);
//  }

  /**
   * Set the value of a feature of a FS without checking for index corruption
   * (typically because the feature isn't one that can be used as a key, or
   * the context is one where the FS is being created, and is guaranteed not to be in any index (yet))
   * 
   * @param fs      The FS.
   * @param feat    The feature.
   * @param value     The new value for the feature.
   */
  void setFeatureValueNoIndexCorruptionCheck(FeatureStructureImplC fs, FeatureImpl feat, Object value) {
    fs.setFeatureValueNoIndexCorruptionCheck(feat, value);
//    fs._refData[feat.getAdjustedOffset()] = value;   
//    maybeLogUpdate(fs, feat);
  }  

//  /**
//   * Set the value of a feature in the FS without journaling
//   *   (because it's for a new FS above the mark)
//   * @param fs      The Feature Structure.
//   * @param featOffset The offset
//   * @param value     The new value for the feature.
//   */
//  void setFeatureValueNotJournaled(FeatureStructureImplC fs, int featOffset, int value) {
//    fs._intData[featOffset] = value;
//  }

//  /**
//   * Set the value of a feature in the FS without journaling
//   *   (because it's for a new FS above the mark)
//   * @param fs      The Feature Structure.
//   * @param featOffset The offset
//   * @param value     The new value for the feature.
//   */
//  void setFeatureValueNotJournaled(FeatureStructureImplC fs, int featOffset, Object value) {
//    fs._refData[featOffset] = value;
//  }

  public void setFeatureValue(int fsRef, int featureCode, int value) {
    setFeatureValue(getFsFromId_checked(fsRef), getTypeSystemImpl().getFeatureForCode(featureCode), value);
  }
  
  public void setFeatureValue(int fsRef, int featureCode, Object value) {
    setFeatureValue(getFsFromId_checked(fsRef), getTypeSystemImpl().getFeatureForCode(featureCode), value);
  }
  
  void setFeatureValueNoIndexCorruptionCheck(int fsRef, int featureCode, Object value) {
    setFeatureValueNoIndexCorruptionCheck(getFsFromId_checked(fsRef), 
                                         getTypeSystemImpl().getFeatureForCode(featureCode), 
                                         value);
  }

  
  
  
  public String getFeatureValueAsString(FeatureStructureImplC fs, FeatureImpl feat) {
    TypeImpl range = feat.getRangeImpl();
    if (feat.isInInt) {
      switch (range.getCode()) {
      case TypeSystemImpl.floatTypeCode :
        return Float.toString(fs.getFloatValue(feat));
      case TypeSystemImpl.booleanTypeCode :
        return Boolean.toString(fs.getBooleanValue(feat));
      case TypeSystemImpl.longTypeCode :
        return Long.toString(fs.getLongValue(feat));
      case TypeSystemImpl.doubleTypeCode :
        return Double.toString(fs.getDoubleValue(feat));
      default: 
        return Integer.toString(fs.getIntValue(feat));
      }
    }
    
    if (range instanceof TypeImpl_string) {
      return fs.getStringValue(feat);
    }
    
    if (range.getCode() == TypeSystemImpl.javaObjectTypeCode) {
      return serializeJavaObject(fs.getJavaObjectValue(feat));
    }
    
    if (range.isRefType) {
      TOP ref = fs.getFeatureValue(feat);
      return (ref == null) ? null : ref.toString();
    }
    
    throw new CASRuntimeException(UIMARuntimeException.INTERNAL_ERROR);
  }

  /**
   * Supports setting slots to "0" for null values
   * @param fs The feature structure to update
   * @param feat the feature to update-
   * @param s the string representation of the value, could be null
   */
  public void setFeatureValueFromString(FeatureStructureImplC fs, FeatureImpl feat, String s) {
    final TypeImpl range = feat.getRangeImpl();
    if (fs instanceof Sofa) {
      // sofa has special setters
      Sofa sofa = (Sofa) fs;
      switch (feat.getCode()) {
      case TypeSystemImpl.sofaMimeFeatCode : sofa.setMimeType(s); break;
      case TypeSystemImpl.sofaStringFeatCode: sofa.setLocalSofaData(s); break;
      case TypeSystemImpl.sofaUriFeatCode: sofa.setRemoteSofaURI(s); break;
      default: // left empty - ignore trying to set final fields
      }
      return;
    }
    
    if (feat.isInInt) {
      switch (range.getCode()) {
      case TypeSystemImpl.floatTypeCode :   fs.setFloatValue(feat, (s == null) ? 0F : Float.parseFloat(s)); break;
      case TypeSystemImpl.booleanTypeCode : fs.setBooleanValue(feat, (s == null) ? false : Boolean.parseBoolean(s)); break;
      case TypeSystemImpl.longTypeCode :    fs.setLongValue(feat, (s == null) ? 0L : Long.parseLong(s)); break;
      case TypeSystemImpl.doubleTypeCode :  fs.setDoubleValue(feat, (s == null) ? 0D : Double.parseDouble(s)); break;
      case TypeSystemImpl.byteTypeCode :  fs.setByteValue(feat, (s == null) ? 0 : Byte.parseByte(s)); break;
      case TypeSystemImpl.shortTypeCode :  fs.setShortValue(feat, (s == null) ? 0 : Short.parseShort(s)); break;
      case TypeSystemImpl.intTypeCode :  fs.setIntValue(feat, (s == null) ? 0 : Integer.parseInt(s)); break;
      default:                              fs.setIntValue(feat, (s == null) ? 0 : Integer.parseInt(s));
      }
    } else if (range.isRefType) {
      if (s == null) {
        fs.setFeatureValue(feat,  null);
      } else {
        // Setting a reference value "{0}" from a string is not supported. 
        throw new CASRuntimeException(CASRuntimeException.SET_REF_FROM_STRING_NOT_SUPPORTED, feat.getName());
      }
    } else if (range instanceof TypeImpl_string) {  // includes TypeImplSubString
      // is String or Substring
      fs.setStringValue(feat, (s == null) ? null : s);
    } else if (range == tsi.javaObjectType) {
      fs.setJavaObjectValue(feat, (s == null) ? null : deserializeJavaObject(s));
    } else {
      throw new CASRuntimeException(UIMARuntimeException.INTERNAL_ERROR);
    }
  }

  private Object deserializeJavaObject(String s) {
    throw new UnsupportedOperationException("Deserializing JavaObjects not yet implemented");
  }

  private String serializeJavaObject(Object s) {
    throw new UnsupportedOperationException("Serializing JavaObjects not yet implemented");
  }

  /*
   * This should be the only place where the encoding of floats and doubles in terms of ints is specified
   * Someday we may want to preserve NAN things using "raw" versions
   */
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

  // Type access methods.
  public boolean isStringType(Type type) {
    return type instanceof TypeImpl_string;
  }

  public boolean isArrayOfFsType(Type type) {
    return ((TypeImpl) type).isArray();
  }

  public boolean isPrimitiveArrayType(Type type) {
    return (type instanceof TypeImpl_array) && ! type.getComponentType().isPrimitive();
  }

  public boolean isIntArrayType(Type type) {
    return (type == getTypeSystemImpl().intArrayType);
  }

  public boolean isFloatArrayType(Type type) {
    return ((TypeImpl)type).getCode() == TypeSystemImpl.floatArrayTypeCode;
  }

  public boolean isStringArrayType(Type type) {
    return ((TypeImpl)type).getCode() == TypeSystemImpl.stringArrayTypeCode;
  }

  public boolean isBooleanArrayType(Type type) {
    return ((TypeImpl)type).getCode() == TypeSystemImpl.booleanArrayTypeCode;
  }

  public boolean isByteArrayType(Type type) {
    return ((TypeImpl)type).getCode() == TypeSystemImpl.byteArrayTypeCode;
  }

  public boolean isShortArrayType(Type type) {
    return ((TypeImpl)type).getCode() == TypeSystemImpl.byteArrayTypeCode;
  }

  public boolean isLongArrayType(Type type) {
    return ((TypeImpl)type).getCode() == TypeSystemImpl.longArrayTypeCode;
  }

  public boolean isDoubleArrayType(Type type) {
    return ((TypeImpl)type).getCode() == TypeSystemImpl.doubleArrayTypeCode;
  }

  public boolean isFSArrayType(Type type) {
    return ((TypeImpl)type).getCode() == TypeSystemImpl.fsArrayTypeCode;
  }

  public boolean isIntType(Type type) {
    return ((TypeImpl)type).getCode() == TypeSystemImpl.intTypeCode;
  }

  public boolean isFloatType(Type type) {
    return ((TypeImpl)type).getCode() == TypeSystemImpl.floatTypeCode;
  }

  public boolean isByteType(Type type) {
    return ((TypeImpl)type).getCode() == TypeSystemImpl.byteTypeCode;
  }

  public boolean isBooleanType(Type type) {
    return ((TypeImpl)type).getCode() == TypeSystemImpl.floatTypeCode;
  }

  public boolean isShortType(Type type) {
    return ((TypeImpl)type).getCode() == TypeSystemImpl.shortTypeCode;
  }

  public boolean isLongType(Type type) {
    return ((TypeImpl)type).getCode() == TypeSystemImpl.longTypeCode;
  }

  public boolean isDoubleType(Type type) {
    return ((TypeImpl)type).getCode() == TypeSystemImpl.doubleTypeCode;
  }

  /*
   * Only called on base CAS
   */
  /**
   * @see org.apache.uima.cas.admin.CASMgr#initCASIndexes()
   */
  @Override
  public void initCASIndexes() throws CASException {
    final TypeSystemImpl ts = getTypeSystemImpl();
    if (!ts.isCommitted()) {
      throw new CASException(CASException.MUST_COMMIT_TYPE_SYSTEM);
    }

    FSIndexComparator comp = this.indexRepository.createComparator();
    comp.setType(ts.sofaType);
    comp.addKey(ts.sofaNum, FSIndexComparator.STANDARD_COMPARE);
    this.indexRepository.createIndex(comp, CAS.SOFA_INDEX_NAME, FSIndex.BAG_INDEX);

    comp = this.indexRepository.createComparator();
    comp.setType(ts.annotType);
    comp.addKey(ts.startFeat, FSIndexComparator.STANDARD_COMPARE);
    comp.addKey(ts.endFeat, FSIndexComparator.REVERSE_STANDARD_COMPARE);
    comp.addKey(this.indexRepository.getDefaultTypeOrder(), FSIndexComparator.STANDARD_COMPARE);
    this.indexRepository.createIndex(comp, CAS.STD_ANNOTATION_INDEX);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // CAS support ... create CAS view of aSofa

  // For internal use only
  public CAS getView(int sofaNum) {
    return getViewFromSofaNbr(sofaNum);
  }

  
  @Override
  public CAS getCurrentView() {
    return getView(CAS.NAME_DEFAULT_SOFA);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // JCas support

  @Override
  public JCas getJCas() {
    if (this.jcas == null) {
      this.jcas = JCasImpl.getJCas(this);
    }
    return this.jcas;
  }
  
  public JCasImpl getJCasImpl() {
    if (this.jcas == null) {
      this.jcas = JCasImpl.getJCas(this);
    }
    return (JCasImpl) this.jcas;
  }


  /**
   * Internal use only
   * 
   * @return corresponding JCas, assuming it exists
   */
  public JCas getExistingJCas() {
    return this.jcas;
  }

  // Create JCas view of aSofa
  @Override
  public JCas getJCas(SofaFS aSofa) throws CASException {
    // Create base JCas, if needed
    this.svd.baseCAS.getJCas();

    return getView(aSofa).getJCas();
    /*
     * // If a JCas already exists for this Sofa, return it JCas aJCas = (JCas)
     * this.svd.baseCAS.sofa2jcasMap.get(Integer.valueOf(aSofa.getSofaRef())); if
     * (null != aJCas) { return aJCas; } // Get view of aSofa CASImpl view =
     * (CASImpl) getView(aSofa); // wrap in JCas aJCas = view.getJCas();
     * this.sofa2jcasMap.put(Integer.valueOf(aSofa.getSofaRef()), aJCas); return
     * aJCas;
     */
  }

  /**
   * @deprecated
   */
  @Override
  @Deprecated
  public JCas getJCas(SofaID aSofaID) throws CASException {
    SofaFS sofa = getSofa(aSofaID);
    // sofa guaranteed to be non-null by above method.
    return getJCas(sofa);
  }
  
  private CASImpl getViewFromSofaNbr(int nbr) {
    final ArrayList<CASImpl> sn2v = this.svd.sofaNbr2ViewMap;
    if (nbr < sn2v.size()) {
      return sn2v.get(nbr);
    }
    return null;
  }
  
  void setViewForSofaNbr(int nbr, CASImpl view) {
    ArrayList<CASImpl> sn2v = this.svd.sofaNbr2ViewMap;
    // cant use ensure capacity here
    while (sn2v.size() <= nbr) {
      sn2v.add(null);
    }
    sn2v.set(nbr, view);
  }

  // For internal platform use only
  CAS getInitialView() {
    CAS couldBeThis = getViewFromSofaNbr(1);
    if (couldBeThis != null) {
      return couldBeThis;
    }
    // create the initial view, without a Sofa
    CASImpl aView = new CASImpl(this.svd.baseCAS, (SofaFS) null);
    setViewForSofaNbr(1, aView);
    assert (this.svd.viewCount <= 1);
    this.svd.viewCount = 1;
    return aView;
  }

  @Override
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
      throw new CASRuntimeException(CASRuntimeException.SOFANAME_ALREADY_EXISTS, aSofaID);
    }
    Sofa newSofa = createSofa(absoluteSofaName, null);
    CAS newView = getView(newSofa);
    ((CASImpl) newView).registerView(newSofa);
    return newView;
  }

  @Override
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
    // sofa guaranteed to be non-null by above method
    // unless sofa doesn't exist, which will cause a throw.
    return getView(sofa);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.CAS#getView(org.apache.uima.cas.SofaFS)
   * 
   * Callers of this can have created Sofas in the CAS without views: using the
   * old deprecated createSofa apis (this is being fixed so these will create
   * the views) via deserialization, which will put the sofaFSs into the CAS
   * without creating the views, and then call this to create the views. - for
   * deserialization: there are 2 kinds: 1 is xmi the other is binary. - for
   * xmi: there is 1.4.x compatible and 2.1 compatible. The older format can
   * have sofaNbrs in the order 2, 3, 4, 1 (initial sofa), 5, 6, 7 The newer
   * format has them in order. For deserialized sofas, we insure here that there
   * are no duplicates. This is not done in the deserializers - they use either
   * heap dumping (binary) or generic fs creators (xmi).
   * 
   * Goal is to detect case where check is needed (sofa exists, but view not yet
   * created). This is done by looking for cases where sofaNbr &gt; curViewCount.
   * This only works if the sofaNbrs go up by 1 (except for the initial sofa) in
   * the input sequence of calls.
   */
  @Override
  public CAS getView(SofaFS aSofa) {
    Sofa sofa = (Sofa) aSofa;
    final int sofaNbr = sofa.getSofaRef();
//    final Integer sofaNbrInteger = Integer.valueOf(sofaNbr);

    CASImpl aView = (CASImpl) getViewFromSofaNbr(sofaNbr);
    if (null == aView) {
      // This is the deserializer case, or the case where an older API created a
      // sofa,
      // which is now creating the associated view

      // create a new CAS view
      aView = new CASImpl(this.svd.baseCAS, sofa);
      setViewForSofaNbr(sofaNbr, aView);
      verifySofaNameUniqueIfDeserializedViewAdded(sofaNbr, sofa);
      return aView;
    }

    // for deserialization - might be reusing a view, and need to tie new Sofa
    // to old View
    if (null == aView.mySofaRef) {
      aView.mySofaRef = sofa;
    }

    verifySofaNameUniqueIfDeserializedViewAdded(sofaNbr, aSofa);
    return aView;
  }
  
//  boolean isSofaView(int sofaAddr) {
//    if (mySofaRef == null) {
//      // don't create initial sofa
//      return false;
//    }
//    return mySofaRef == sofaAddr;
//  }
  
  

  /*
   * for Sofas being added (determined by sofaNbr &gt; curViewCount): verify sofa
   * name is not already present, and record it for future tests
   * 
   * Only should do the name test & update in the case of deserialized new sofas
   * coming in. These will come in, in order. Exception is "_InitialView" which
   * could come in the middle. If it comes in the middle, no test will be done
   * for duplicates, and it won't be added to set of known names. This is ok
   * because the createVIew special cases this test. Users could corrupt an xmi
   * input, which would make this logic fail.
   */
  private void verifySofaNameUniqueIfDeserializedViewAdded(int sofaNbr, SofaFS aSofa) {
    final int curViewCount = this.svd.viewCount;
    if (curViewCount < sofaNbr) {
      // Only true for deserialized sofas with new views being either created,
      // or
      // hooked-up from CASes that were freshly reset, which have multiple
      // views.
      // Assume sofa numbers are incrementing by 1
      assert (sofaNbr == curViewCount + 1);
      this.svd.viewCount = sofaNbr;
      String id = aSofa.getSofaID();
      // final Feature idFeat =
      // getTypeSystem().getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFAID);
      // String id =
      // ll_getStringValue(((FeatureStructureImpl)aSofa).getAddress(),
      // ((FeatureImpl) idFeat).getCode());
      if (!this.svd.sofaNameSet.contains(id)) {
        throw new CASRuntimeException(UIMARuntimeException.INTERNAL_ERROR);
//            CASRuntimeException.SOFANAME_ALREADY_EXISTS, id);
      }
//      this.svd.sofaNameSet.add(id);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getTypeSystem()
   */
  @Override
  public LowLevelTypeSystem ll_getTypeSystem() {
    return getTypeSystemImpl().getLowLevelTypeSystem();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getIndexRepository()
   */
  @Override
  public LowLevelIndexRepository ll_getIndexRepository() {
    return this.indexRepository;
  }

  /**
   * 
   * @param fs
   * @param domType
   * @param featCode
   */
  private final void checkLowLevelParams(TOP fs, TypeImpl domType, int featCode) {
 
    checkFeature(featCode);
    checkTypeHasFeature(domType, featCode);
  }

  /**
   * Check that the featCode is a feature of the domain type
   * @param domTypeCode
   * @param featCode
   */
  private final void checkTypeHasFeature(TypeImpl domainType, int featureCode) {
    checkTypeHasFeature(domainType, getTypeSystemImpl().getFeatureForCode_checked(featureCode));
  }

  private final void checkTypeHasFeature(TypeImpl domainType, FeatureImpl feature) {
    if (!domainType.isAppropriateFeature(feature)) {
      throw new LowLevelException(LowLevelException.FEAT_DOM_ERROR, 
          domainType.getCode(), 
          domainType.getName(),
          feature.getCode(),
          feature.getName());
    }
  }
  /**
   * Check the range is appropriate for this type/feature. Throws
   * LowLevelException if it isn't.
   * 
   * @param domType
   *                domain type
   * @param ranType
   *                range type
   * @param feat
   *                feature
   */
  public final void checkTypingConditions(Type domType, Type ranType, Feature feat) {
    TypeImpl domainTi = (TypeImpl) domType;
    FeatureImpl fi = (FeatureImpl) feat;
    checkTypeHasFeature(domainTi, fi);
    if (!((TypeImpl) fi.getRange()).subsumes((TypeImpl) ranType)) {
      throw new LowLevelException(LowLevelException.FEAT_RAN_ERROR,
          fi.getCode(),
          feat.getName(),
          ((TypeImpl)ranType).getCode(),
          ranType.getName());
    }
  }

  /**
   * Validate a feature's range is a ref to a feature structure
   * @param featCode
   * @throws LowLevelException
   */

  private final void checkFsRan(FeatureImpl fi) throws LowLevelException {
    if (!fi.getRangeImpl().isRefType) {
      throw new LowLevelException(LowLevelException.FS_RAN_TYPE_ERROR,
          fi.getCode(),
          fi.getName(),
          fi.getRange().getName());
    }
  }

  private final void checkFeature(int featureCode) {
    if (!getTypeSystemImpl().isFeature(featureCode)) {
      throw new LowLevelException(LowLevelException.INVALID_FEATURE_CODE, featureCode);
    }
  }

  final <T extends FeatureStructure> T getFsFromId_checked(int fsRef) {
    T r = getFsFromId(fsRef);
    if (r == null) {
      if (fsRef == 0) {
        return null;
      }
      throw new LowLevelException(LowLevelException.INVALID_FS_REF, fsRef);
    }
    return r;
  }

  @Override
  public final boolean ll_isRefType(int typeCode) {
    return getTypeSystemImpl().getTypeForCode(typeCode).isRefType;
  }

  @Override
  public final int ll_getTypeClass(int typeCode) {
    return TypeSystemImpl.getTypeClass(getTypeSystemImpl().getTypeForCode(typeCode));
  }

  // backwards compatibility only
  @Override
  public final int ll_createFS(int typeCode) {
    Type ti = getTypeSystemImpl().ll_getTypeForCode(typeCode);
    TOP fs = createFS(ti);
    svd.cache_not_in_index = fs;
    return fs.id();
  }

  @Override
  public final int ll_createFS(int typeCode, boolean doCheck) {
    TypeImpl ti = (TypeImpl) getTypeSystemImpl().ll_getTypeForCode(typeCode);
    if (doCheck) {
      if (ti == null || !ti.isCreatableAndNotBuiltinArray()) {
        throw new LowLevelException(LowLevelException.CREATE_FS_OF_TYPE_ERROR, typeCode);
      }
    }
    return createFS(ti).getAddress();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_createArray(int, int)
   */
  @Override
  public int ll_createArray(int typeCode, int arrayLength) {
    return createArray(getTypeSystemImpl().getTypeForCode_checked(typeCode), arrayLength).id();      
  }

  @Override
  public int ll_createByteArray(int arrayLength) {
    return ll_createArray(TypeSystemImpl.byteArrayTypeCode, arrayLength);
  }

  @Override
  public int ll_createBooleanArray(int arrayLength) {
    return ll_createArray(TypeSystemImpl.booleanArrayTypeCode, arrayLength);
  }

  @Override
  public int ll_createShortArray(int arrayLength) {
    return ll_createArray(TypeSystemImpl.shortArrayTypeCode, arrayLength);
  }

  @Override
  public int ll_createLongArray(int arrayLength) {
    return ll_createArray(TypeSystemImpl.longArrayTypeCode, arrayLength);
  }

  @Override
  public int ll_createDoubleArray(int arrayLength) {
    return ll_createArray(TypeSystemImpl.doubleArrayTypeCode, arrayLength);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_createArray(int, int, boolean)
   */
  @Override
  public int ll_createArray(int typeCode, int arrayLength, boolean doChecks) {
    if (doChecks) {
      final TypeSystemImpl ts = getTypeSystemImpl();
      // Check typeCode, arrayLength
      if (!ts.isType(typeCode)) {
        throw new LowLevelException(LowLevelException.INVALID_TYPE_ARGUMENT, typeCode);
      }
      if (!isCreatableArrayType(typeCode)) {
        throw new LowLevelException(LowLevelException.CREATE_ARRAY_OF_TYPE_ERROR, typeCode, ts.ll_getTypeForCode(typeCode).getName());

      }
      if (arrayLength < 0) {
        throw new LowLevelException(LowLevelException.ILLEGAL_ARRAY_LENGTH, arrayLength);
      }
    }
    return ll_createArray(typeCode, arrayLength);
  }
  
  public void validateArraySize(int length) {
    if (length < 0) {
      /** Array size must be &gt;= 0. */
      throw new CASRuntimeException(CASRuntimeException.ILLEGAL_ARRAY_SIZE);
    }
  }

  private final boolean isCreatableArrayType(int typeCode) {
    final int tc = ll_getTypeClass(typeCode);
    return ((tc == TYPE_CLASS_INTARRAY) || (tc == TYPE_CLASS_FLOATARRAY)
        || (tc == TYPE_CLASS_STRINGARRAY) || (tc == TYPE_CLASS_FSARRAY)
        || (tc == TYPE_CLASS_BOOLEANARRAY) || (tc == TYPE_CLASS_BYTEARRAY)
        || (tc == TYPE_CLASS_SHORTARRAY) || (tc == TYPE_CLASS_LONGARRAY) || (tc == TYPE_CLASS_DOUBLEARRAY));
  }

  @Override
  public final int ll_getFSRef(FeatureStructure fs) {
    if (null == fs) {
      return NULL;
    }
    return ((FeatureStructureImplC)fs).id();
  }

  @Override
  public <T extends TOP> T ll_getFSForRef(int id) {
    return getFsFromId_checked(id);
  }

  @Override
  public final int ll_getIntValue(int fsRef, int featureCode) {
    return getFsFromId_checked(fsRef).getIntValue(getTypeSystemImpl().getFeatureForCode_checked(featureCode));
  }

//  public final int ll_getIntValueFeatOffset(int fsRef, int featureOffset) {
//    return ll_getFSForRef(fsRef)._intData[featureOffset];
//  }

  @Override
  public final float ll_getFloatValue(int fsRef, int featureCode) {
    return getFsFromId_checked(fsRef).getFloatValue(getTypeSystemImpl().getFeatureForCode_checked(featureCode));
  }

  @Override
  public final String ll_getStringValue(int fsRef, int featureCode) {
    return getFsFromId_checked(fsRef).getStringValue(getTypeSystemImpl().getFeatureForCode_checked(featureCode));
  }
  
//  public final String ll_getStringValueFeatOffset(int fsRef, int featureOffset) {
//    return (String) getFsFromId_checked(fsRef)._refData[featureOffset];
//  }

  @Override
  public final int ll_getRefValue(int fsRef, int featureCode) {
    return getFsFromId_checked(fsRef).getFeatureValue(getTypeSystemImpl().getFeatureForCode_checked(featureCode)).id();
  }

//  public final int ll_getRefValueFeatOffset(int fsRef, int featureOffset) {
//    return ((FeatureStructureImplC)getFsFromId_checked(fsRef)._refData[featureOffset]).id();
//  }

  @Override
  public final int ll_getIntValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    return ll_getIntValue(fsRef, featureCode);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getFloatValue(int, int,
   *      boolean)
   */
  @Override
  public final float ll_getFloatValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    return ll_getFloatValue(fsRef, featureCode);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getStringValue(int, int,
   *      boolean)
   */
  @Override
  public final String ll_getStringValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    return ll_getStringValue(fsRef, featureCode);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getRefValue(int, int, boolean)
   */
  @Override
  public final int ll_getRefValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkFsRefConditions(fsRef, featureCode);
    }
    return getFsFromId_checked(fsRef).getFeatureValue(getTypeSystemImpl().getFeatureForCode_checked(featureCode)).id();
  }
  
  /**
   * This is the method all normal FS feature "setters" call before doing the set operation.
   * <p style="margin-left:2em">
   *   The binary deserializers bypass these setters, and directly update the heap values, so they have
   *   a different impl to avoid index corruption.
   * <p>
   * It may do nothing (for performance, it needs to be enabled by a JVM property).
   * <p>
   * If enabled, it will check if the update may corrupt any index in any view.  The check tests
   * whether the feature is being used as a key in one or more indexes and if the FS is in one or more 
   * corruptable view indexes. 
   * <p>
   * If true, then:
   * <ul>
   *   <li>it may remove and remember (for later adding-back) the FS from all corruptable indexes
   *   (bag indexes are not corruptable via updating, so these are skipped). 
   *   The addback occurs later either via an explicit call to do so, or the end of a protectIndex block, or.
   *   (if autoIndexProtect is enabled) after the individual feature update is completed.</li>
   *   <li>it may give a WARN level message to the log. This enables users to 
   *   implement their own optimized handling of this for "high performance"
   *   applications which do not want the overhead of runtime checking.  </li></ul>
   * <p>  
   *   
   * @param fs - the FS to test if it is in the indexes
   * @param featCode - the feature being tested
   * @return true if something may need to be added back
   */  
  private boolean checkForInvalidFeatureSetting(TOP fs, int featCode) {
    return checkForInvalidFeatureSetting(fs, featCode, Integer.MIN_VALUE);
  }
    
  private boolean checkForInvalidFeatureSetting(TOP fs, int featCode, int jcasFieldRegistryIndex) {
    if (fs == svd.cache_not_in_index) {
      return false;
    }
    
    final int ssz = svd.fssTobeAddedback.size();
    // skip if protection is disabled, and no explicit protection block
    if (IS_DISABLED_PROTECT_INDEXES && ssz == 0) {
      return false;
    }
    
    if (jcasFieldRegistryIndex == Integer.MIN_VALUE) {
      if (!svd.featureCodesInIndexKeys.get(featCode)) {
        return false;
      }
    } else {
      if (!svd.featureJiInIndexKeys.get(jcasFieldRegistryIndex)) {
        return false;
      }
    }

    // next method skips if the fsRef is not in the index (cache)
    final boolean wasRemoved = removeFromCorruptableIndexAnyView(
        fs, 
        (ssz > 0) ? svd.fssTobeAddedback.get(ssz - 1) : 
                    svd.fsTobeAddedbackSingle 
        );            
 
    if (wasRemoved) {
      // skip message if wasn't removed
      // skip message if protected in explicit block
      if (IS_REPORT_FS_UPDATE_CORRUPTS_INDEX && ssz == 0) {
        if (jcasFieldRegistryIndex == Integer.MIN_VALUE) {
          featModWhileInIndexReport(fs, featCode);
        } else {
          featModWhileInIndexReport(fs, getFeatFromJCasFieldRegistryIndex(jcasFieldRegistryIndex));
        }
      }
      svd.cache_not_in_index = fs; // speed up adds before add back
    }
    
    return wasRemoved;
  }

  // version of above, but using jcasFieldRegistryIndex
  private boolean checkForInvalidFeatureSettingJFRI(TOP fs, int jcasFieldRegistryIndex) {
    return checkForInvalidFeatureSetting(fs, -1, jcasFieldRegistryIndex);
  }
  
  private FeatureImpl getFeatFromJCasFieldRegistryIndex(int jcasFieldRegistryIndex) {
    return getFSClassRegistry().featuresFromJFRI[jcasFieldRegistryIndex];
  }

  private void featModWhileInIndexReport(FeatureStructure fs, int featCode) {
    featModWhileInIndexReport(fs, getTypeSystemImpl().getFeatureForCode(featCode));
  }
  
  private void featModWhileInIndexReport(FeatureStructure fs, FeatureImpl fi) {
    // prepare a message which includes the feature which is a key, the fs, and
    // the call stack.
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    new Throwable().printStackTrace(pw);
    pw.close();
    String msg = String.format(
        "While FS was in the index, the feature \"%s\""
        + ", which is used as a key in one or more indexes, "
        + "was modified\n FS = \"%s\"\n%s%n",
        fi.getName(),
        fs.toString(),  
        sw.toString());        
    UIMAFramework.getLogger().log(Level.WARNING, msg);
    
    if (IS_THROW_EXCEPTION_CORRUPT_INDEX) {
      throw new UIMARuntimeException(UIMARuntimeException.ILLEGAL_FS_FEAT_UPDATE, new Object[]{});
    }        
  }
    
  /**
   * Do the individual feat update addback if
   *   a) not in a block mode, and 
   *   b) running with auto protect indexes
   *   c) not in block-single mode
   *   
   * if running in block mode, the add back is delayed until the end of the block
   *   
   * @param fsRef the fs to add back
   */
  public void maybeAddback(TOP fs) {
    if (!svd.fsTobeAddedbackSingleInUse && (!IS_DISABLED_PROTECT_INDEXES) && svd.fssTobeAddedback.size() == 0) {
      svd.fsTobeAddedbackSingle.addback(fs);
    }
  }
  
  // next two methods dropped - rather than seeing if something is in the index, and then 
  // later removing it (two lookups), we just conditionally remove it
  
//  /**
//   * test if this feature is in a key and the FS is in the index
//   *   side effect - if the fsRef is determined not to be in the index, this is remembered.
//   * @param fsRef - a feature structure which may be in the index
//   * @param featCode the feature to test if it is in any key
//   * @return true if the fsRef is in the index, and the featureCode is used as a key
//   */
//  boolean isFeatureAKeyInIndexedFS(int fsRef, int featCode) {
//    if (svd.cache_not_in_index == fsRef) {  // keep, needed for bin compr deser calls
//      return false;
//    }
//    if (svd.featureCodesInIndexKeys.contains(featCode)) {
//      if (isFsInCorruptableIndexAnyView(fsRef)) {
//        return true;
//      }
//      svd.cache_not_in_index = fsRef;
//      return false;
//    }
//    return false;
//  }
//  
//  private boolean isFsInCorruptableIndexAnyView(final int fsRef) {
//    final int typeCode = getTypeCode(fsRef);
//    final TypeSystemImpl tsi = getTypeSystemImpl();
//    if (tsi.isAnnotationBaseOrSubtype(typeCode)) {
//      // only need to check one view
//      return ll_getSofaCasView(fsRef).indexRepository.isInSetOrSortedIndexInThisView(fsRef); 
//    }
//    // not a subtype of AnnotationBase, need to check all views (except base)
//    final Iterator<CAS> viewIterator = getViewIterator();
//    while (viewIterator.hasNext()) {
//      final CAS view =  viewIterator.next();
//      if (((FSIndexRepositoryImpl)view.getIndexRepository()).isInSetOrSortedIndexInThisView(fsRef)) {
//        return true;
//      }
//    }
//    return false;  // not in any view's indexes
//  }
  /**
   * A conditional remove, depends on the featCode being used as a key
   * Skip tests if the FS is known not to be in the indexes in any view
   *
   * @param fs the fs
   * @param toBeAdded the place to record removal actions
   * @param featCode the feature to test if it's used as a key in some index
   * @return true if the fs was removed
   */
  
  boolean removeFromCorruptableIndexAnyView(final TOP fs, FSsTobeAddedback toBeAdded, int featCode) {
    if (fs != svd.cache_not_in_index && svd.featureCodesInIndexKeys.get(featCode)) {
      boolean wasRemoved = removeFromCorruptableIndexAnyView(fs, toBeAdded);
      svd.cache_not_in_index = fs; // because will remove it if its in the index.
      return wasRemoved;
    }
    return false;
  }
    
  boolean removeFromCorruptableIndexAnyView(final TOP fs, FSsTobeAddedback toBeAdded) {
    final TypeImpl ti = ((FeatureStructureImplC)fs)._typeImpl;
    if (ti.isAnnotationBaseType()) {
      final AnnotationBase ab = (AnnotationBase) fs;
      
      // in v3, the fs points to the view
      
//      // only need to check one view
//      // get that view carefully, in case things are not yet properly initialized
//      Sofa sofa = ab.getSofa();
//      if (null == sofa) {
//        return false;
//      }
//      CAS view = (sofa == this.getSofa()) ? this : getViewFromSofaNbr(sofa.getSofaNum());
//      if (null == view) {
//        return false;
//      }
      return removeAndRecord(fs, (FSIndexRepositoryImpl) fs._casView.getIndexRepository(), toBeAdded);
    }
    
    // not a subtype of AnnotationBase, need to check all views (except base)
    // sofas indexed in the base view are not corruptable.
    
    
    final Iterator<CAS> viewIterator = getViewIterator();
    boolean wasRemoved = false;
    while (viewIterator.hasNext()) {
      wasRemoved |= removeAndRecord(fs, (FSIndexRepositoryImpl) viewIterator.next().getIndexRepository(), toBeAdded);
    }
    return wasRemoved;
  }

  /**
   * Called from XmiCasDeserialization for delta updating below the line
   * @param fsRef
   * @param toBeAdded
   * @return
   */
  boolean removeFromCorruptableIndexAnyViewSetCache(final TOP fs, FSsTobeAddedback toBeAdded) {
    if (fs != svd.cache_not_in_index) {
      svd.cache_not_in_index = fs;
      return removeFromCorruptableIndexAnyView(fs, toBeAdded);
    }
    return false;
  }

  /**
   * remove a FS from corruptable indexes in this view
   * @param fs the fs to be removed
   * @param ir the view
   * @param toBeAdded the place to record how many times it was in the index, per view
   * @return true if it was removed, false if it wasn't in any corruptable index.
   */
  private boolean removeAndRecord(TOP fs, FSIndexRepositoryImpl ir, FSsTobeAddedback toBeAdded) {
    boolean wasRemoved = ir.removeIfInCorrputableIndexInThisView(fs);
    if (wasRemoved) {
      toBeAdded.recordRemove(fs, ir, 1);
    }
    return wasRemoved;
  }  

  
  @Override
  public final void ll_setIntValue(int fsRef, int featureCode, int value) {
    setFeatureValue(fsRef, featureCode, value);
  }

  @Override
  public final void ll_setFloatValue(int fsRef, int featureCode, float value) {
    setFeatureValue(fsRef, featureCode, float2int(value));
  }
  
//  public final void ll_setFloatValueNoIndexCorruptionCheck(int fsRef, int featureCode, float value) {
//    setFeatureValueNoIndexCorruptionCheck(fsRef, featureCode, float2int(value));
//  }

  @Override
  public final void ll_setStringValue(int fsRef, int featureCode, String value) {
    TOP fs = getFsFromId_checked(fsRef);
    FeatureImpl feat = getTypeSystemImpl().getFeatureForCode(featureCode);
    fs.setStringValue(feat, value);
  }

  @Override
  public final void ll_setRefValue(int fsRef, int featureCode, int value) {
    // no index check because refs can't be keys
    setFeatureValueNoIndexCorruptionCheck(fsRef, featureCode, getFsFromId_checked(value));
  }
  
  @Override
  public final void ll_setIntValue(int fsRef, int featureCode, int value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    ll_setIntValue(fsRef, featureCode, value);
  }

  @Override
  public final void ll_setFloatValue(int fsRef, int featureCode, float value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    ll_setFloatValue(fsRef, featureCode, value);
  }

  @Override
  public final void ll_setStringValue(int fsRef, int featureCode, String value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    ll_setStringValue(fsRef, featureCode, value);
  }

  @Override
  public final void ll_setCharBufferValue(int fsRef, int featureCode, char[] buffer, int start,
      int length, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    ll_setCharBufferValue(fsRef, featureCode, buffer, start, length);
  }

  @Override
  public final void ll_setCharBufferValue(int fsRef, int featureCode, char[] buffer, int start,
      int length) {
    ll_setStringValue(fsRef, featureCode, new String(buffer, start, length));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_copyCharBufferValue(int, int,
   *      char, int)
   */
  @Override
  public int ll_copyCharBufferValue(int fsRef, int featureCode, char[] buffer, int start) {
    String str = ll_getStringValue(fsRef, featureCode);
    if (str == null) {
      return -1;
    }
    
    final int len = str.length();
    final int requestedMax = start + len;
    // Check that the buffer is long enough to copy the whole string. If it isn't long enough, we
    // copy up to buffer.length - start characters.
    final int max = (buffer.length < requestedMax) ? (buffer.length - start) : len;
    for (int i = 0; i < max; i++) {
      buffer[start + i] = str.charAt(i);
    }
    return len;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getCharBufferValueSize(int,
   *      int)
   */
  @Override
  public int ll_getCharBufferValueSize(int fsRef, int featureCode) {
    String str = ll_getStringValue(fsRef, featureCode);
    return str.length();
  }

  @Override
  public final void ll_setRefValue(int fsRef, int featureCode, int value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkFsRefConditions(fsRef, featureCode);
    }
    ll_setRefValue(fsRef, featureCode, value);
  }

  public final int getIntArrayValue(IntegerArray array, int i) {
    return array.get(i);
  }
  
  public final float getFloatArrayValue(FloatArray array, int i) {
    return array.get(i);
  }
  
  public final String getStringArrayValue(StringArray array, int i) {
    return array.get(i);
  }
  
  public final FeatureStructure getRefArrayValue(FSArray array, int i) {
    return array.get(i);
  }
  
  @Override
  public final int ll_getIntArrayValue(int fsRef, int position) {
    return getIntArrayValue(((IntegerArray)getFsFromId_checked(fsRef)), position);
  }

  @Override
  public final float ll_getFloatArrayValue(int fsRef, int position) {
    return getFloatArrayValue(((FloatArray)getFsFromId_checked(fsRef)), position);
  }
  
  @Override
  public final String ll_getStringArrayValue(int fsRef, int position) {
    return getStringArrayValue(((StringArray)getFsFromId_checked(fsRef)), position);
  }

  @Override
  public final int ll_getRefArrayValue(int fsRef, int position) {
    return ((TOP)getRefArrayValue(((FSArray)getFsFromId_checked(fsRef)), position)).id();
  }

  private void throwAccessTypeError(int fsRef, int typeCode) {
    throw new LowLevelException(LowLevelException.ACCESS_TYPE_ERROR,
                 fsRef, 
                 typeCode, 
                 getTypeSystemImpl().ll_getTypeForCode(typeCode).getName(),
                 getTypeSystemImpl().ll_getTypeForCode(ll_getFSRefType(fsRef)).getName());
  }

  public final void checkArrayBounds(int fsRef, int pos) {
    final int arrayLength = ll_getArraySize(fsRef);
    if ((pos < 0) || (pos >= arrayLength)) {
      throw new ArrayIndexOutOfBoundsException(pos);
      // LowLevelException e = new LowLevelException(
      // LowLevelException.ARRAY_INDEX_OUT_OF_RANGE);
      // e.addArgument(Integer.toString(pos));
      // throw e;
    }
  }

  public final void checkArrayBounds(int arrayLength, int pos, int length) {
    if ((pos < 0) || (length < 0) || ((pos + length) > arrayLength)) {
      throw new LowLevelException(LowLevelException.ARRAY_INDEX_LENGTH_OUT_OF_RANGE, Integer.toString(pos), Integer.toString(length));
    }
  }

  /**
   * Check that the fsRef is valid.
   * Check that the fs is featureCode belongs to the fs 
   * Check that the featureCode is one of the features of the domain type of the fsRef
   * feat could be primitive, string, ref to another feature
   * 
   * @param fsRef
   * @param typeCode
   * @param featureCode
   */
  private final void checkNonArrayConditions(int fsRef, int featureCode) {
    TOP fs = getFsFromId_checked(fsRef);

    final TypeImpl domainType = (TypeImpl) fs.getType();
    
//    checkTypeAt(domType, fs);  // since the type is from the FS, it's always OK
    checkFeature(featureCode);  // checks that the featureCode is in the range of all feature codes
    
    TypeSystemImpl tsi = getTypeSystemImpl();
    FeatureImpl fi = tsi.getFeatureForCode_checked(featureCode);
    checkTypeHasFeature(domainType, fi); // checks that the feature code is one of the features of the type
    
//    checkFsRan(fi);
  }

  private final void checkFsRefConditions(int fsRef, int featureCode) {
    TOP fs = getFsFromId_checked(fsRef);
    checkLowLevelParams(fs, fs._typeImpl, featureCode);  // checks type has feature

    TypeSystemImpl tsi = getTypeSystemImpl();
    FeatureImpl fi = tsi.getFeatureForCode_checked(featureCode);
    checkFsRan(fi);

    // next not needed because checkFsRan already validates this
//    checkFsRef(fsRef + this.svd.casMetadata.featureOffset[featureCode]);
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
    if (typeCode != ll_getFSRefType(fsRef)) {
      throwAccessTypeError(fsRef, typeCode);
    }
    checkArrayBounds(fsRef, position);
  }

  @Override
  public final int ll_getIntArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, TypeSystemImpl.intArrayTypeCode, position);
    }
    return ll_getIntArrayValue(fsRef, position);
  }

  @Override
  public float ll_getFloatArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, TypeSystemImpl.floatArrayTypeCode, position);
    }
    return ll_getFloatArrayValue(fsRef, position);
  }

  @Override
  public String ll_getStringArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, TypeSystemImpl.stringArrayTypeCode, position);
    }
    return ll_getStringArrayValue(fsRef, position);
  }

  @Override
  public int ll_getRefArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, TypeSystemImpl.fsArrayTypeCode, position);
    }
    return ll_getRefArrayValue(fsRef, position);
  }

  @Override
  public void ll_setIntArrayValue(int fsRef, int position, int value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, TypeSystemImpl.intArrayTypeCode, position);
    }
    ll_setIntArrayValue(fsRef, position, value);
  }

  @Override
  public void ll_setFloatArrayValue(int fsRef, int position, float value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, TypeSystemImpl.floatArrayTypeCode, position);
    }
    ll_setFloatArrayValue(fsRef, position, value);
  }

  @Override
  public void ll_setStringArrayValue(int fsRef, int position, String value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, TypeSystemImpl.stringArrayTypeCode, position);
    }
    ll_setStringArrayValue(fsRef, position, value);
  }

  @Override
  public void ll_setRefArrayValue(int fsRef, int position, int value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, TypeSystemImpl.fsArrayTypeCode, position);
    }
    ll_setRefArrayValue(fsRef, position, value);
  }

  /* ************************
   * Low Level Array Setters
   * ************************/
  
  @Override
  public void ll_setIntArrayValue(int fsRef, int position, int value) {
    IntegerArray array = getFsFromId_checked(fsRef);
    array.set(position,  value);  // that set operation does required journaling
  }

  @Override
  public void ll_setFloatArrayValue(int fsRef, int position, float value) {
    FloatArray array = getFsFromId_checked(fsRef);
    array.set(position,  value);  // that set operation does required journaling
  }

  @Override
  public void ll_setStringArrayValue(int fsRef, int position, String value) {
    StringArray array = getFsFromId_checked(fsRef);
    array.set(position,  value);  // that set operation does required journaling
  }

  @Override
  public void ll_setRefArrayValue(int fsRef, int position, int value) {
    FSArray array = getFsFromId_checked(fsRef);
    array.set(position,  getFsFromId_checked(value));  // that set operation does required journaling
  }

  /**
   * @param fsRef an id for a FS
   * @return the type code for this FS
   */
  @Override
  public int ll_getFSRefType(int fsRef) {
    return getFsFromId_checked(fsRef)._getTypeCode();
  }

  @Override
  public int ll_getFSRefType(int fsRef, boolean doChecks) {
    // type code is always valid
    return ll_getFSRefType(fsRef);
  }

  @Override
  public LowLevelCAS getLowLevelCAS() {
    return this;
  }

  @Override
  public int size() {
    throw new UIMARuntimeException(UIMARuntimeException.INTERNAL_ERROR);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.admin.CASMgr#getJCasClassLoader()
   */
  @Override
  public ClassLoader getJCasClassLoader() {
    return this.svd.jcasClassLoader;
  }

  /*
   * Called to set the overall jcas class loader to use.
   * 
   * @see org.apache.uima.cas.admin.CASMgr#setJCasClassLoader(java.lang.ClassLoader)
   */
  @Override
  public void setJCasClassLoader(ClassLoader classLoader) {
    this.svd.jcasClassLoader = classLoader;
  }

  // Internal use only, public for cross package use
  // Assumes: The JCasClassLoader for a CAS is set up initially when the CAS is
  // created
  // and not switched (other than by this code) once it is set.

  // Callers of this method always code the "restoreClassLoaderUnlockCAS" in
  // pairs,
  // protected as needed with try - finally blocks.
  //   
  // Special handling is needed for CAS Mulipliers - they can modify a cas up to
  // the point they no longer "own" it. 
  // So the try / finally approach doesn't fit

  public void switchClassLoaderLockCas(Object userCode) {
    switchClassLoaderLockCasCL(userCode.getClass().getClassLoader());
  }

  public void switchClassLoaderLockCasCL(ClassLoader newClassLoader) {
    // lock out CAS functions to which annotator should not have access
    enableReset(false);

//    switchClassLoader(newClassLoader);
  }

//  // switches ClassLoader but does not lock CAS
//  public void switchClassLoader(ClassLoader newClassLoader) {
//    if (null == newClassLoader) { // is null if no cl set
//      return;
//    }
//    if (newClassLoader != this.svd.jcasClassLoader) {
//      // System.out.println("Switching to new class loader");
//      this.svd.jcasClassLoader = newClassLoader;
//      if (null != this.jcas) {
//        ((JCasImpl) this.jcas).switchClassLoader(newClassLoader);
//      }
//    }
//  }

//  // internal use, public for cross-package ref
//  public boolean usingBaseClassLoader() {
//    return (this.svd.jcasClassLoader == this.svd.previousJCasClassLoader);
//  }

  public void restoreClassLoaderUnlockCas() {
    // unlock CAS functions
    enableReset(true);
    // this might be called without the switch ever being called
//    if (null == this.svd.previousJCasClassLoader) {
//      return;
//    }
//    if (this.svd.previousJCasClassLoader != this.svd.jcasClassLoader) {
//      // System.out.println("Switching back to previous class loader");
//      this.svd.jcasClassLoader = this.svd.previousJCasClassLoader;
//      if (null != this.jcas) {
//        ((JCasImpl) this.jcas).switchClassLoader(this.svd.previousJCasClassLoader);
//      }
//    }

  }
  
  @Override
  public FeatureValuePath createFeatureValuePath(String featureValuePath)
      throws CASRuntimeException {
    return FeatureValuePathImpl.getFeaturePath(featureValuePath);
  }

  @Override
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
  @Override
  public void release() {
    CASImpl baseCas = getBaseCAS();
    if (baseCas != this) {
      baseCas.release();
    } else {
      super.release();
    }
  }

  
  /* **********************************
   *    A R R A Y   C R E A T I O N
   ************************************/
  
  @Override
  public ByteArrayFS createByteArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return new ByteArray(this.getJCas(), length);
  }

  @Override
  public BooleanArrayFS createBooleanArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return new BooleanArray(this.getJCas(), length);
  }

  @Override
  public ShortArrayFS createShortArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return new ShortArray(this.getJCas(), length);
  }

  @Override
  public LongArrayFS createLongArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return new LongArray(this.getJCas(), length);
  }

  @Override
  public DoubleArrayFS createDoubleArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return new DoubleArray(this.getJCas(), length);
  }

  @Override
  public byte ll_getByteValue(int fsRef, int featureCode) {
    return (byte) ll_getIntValue(fsRef, featureCode);
  }

  @Override
  public byte ll_getByteValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    return ll_getByteValue(fsRef, featureCode);
  }

  @Override
  public boolean ll_getBooleanValue(int fsRef, int featureCode) {
    TOP fs = getFsFromId_checked(fsRef);
    return CASImpl.TRUE == fs.getIntValue(getTypeSystemImpl().getFeatureForCode_checked(featureCode));
  }

  @Override
  public boolean ll_getBooleanValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    return ll_getBooleanValue(fsRef, featureCode);
  }

  @Override
  public short ll_getShortValue(int fsRef, int featureCode) {
    return (short) (ll_getIntValue(fsRef, featureCode));
  }

  @Override
  public short ll_getShortValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    return ll_getShortValue(fsRef, featureCode);
  }

  // impossible to implement in v3; change callers
//  public long ll_getLongValue(int offset) {
//    return this.getLongHeap().getHeapValue(offset);
//  }

  @Override
  public long ll_getLongValue(int fsRef, int featureCode) {
    TOP fs = getFsFromId_checked(fsRef);
    return fs.getLongValue(getTypeSystemImpl().getFeatureForCode_checked(featureCode));
  }

  public long ll_getLongValueFeatOffset(int fsRef, int offset) {
    TOP fs = getFsFromId_checked(fsRef);
    return fs.getLongValueOffset(offset);
  }
  
  @Override
  public long ll_getLongValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    return ll_getLongValue(fsRef, featureCode);
  }

  @Override
  public double ll_getDoubleValue(int fsRef, int featureCode) {
    TOP fs = getFsFromId_checked(fsRef);
    return fs.getDoubleValue(getTypeSystemImpl().getFeatureForCode_checked(featureCode));
  }
  
  public double ll_getDoubleValueFeatOffset(int fsRef, int offset) {
    TOP fs = getFsFromId_checked(fsRef);
    return fs.getDoubleValueOffset(offset);
  }


  @Override
  public double ll_getDoubleValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    return ll_getDoubleValue(fsRef, featureCode);
  }

  @Override
  public void ll_setBooleanValue(int fsRef, int featureCode, boolean value) {
    setFeatureValue(fsRef, featureCode, value ? CASImpl.TRUE : CASImpl.FALSE);
  }

  @Override
  public void ll_setBooleanValue(int fsRef, int featureCode, boolean value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    ll_setBooleanValue(fsRef, featureCode, value);
  }

  @Override
  public final void ll_setByteValue(int fsRef, int featureCode, byte value) {
    setFeatureValue(fsRef, featureCode, value);
  }

  @Override
  public void ll_setByteValue(int fsRef, int featureCode, byte value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    ll_setByteValue(fsRef, featureCode, value);
  }

  @Override
  public final void ll_setShortValue(int fsRef, int featureCode, short value) {
    setFeatureValue(fsRef, featureCode, value);
  }

  @Override
  public void ll_setShortValue(int fsRef, int featureCode, short value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    ll_setShortValue(fsRef, featureCode, value);
  }

  @Override
  public void ll_setLongValue(int fsRef, int featureCode, long value) {
    TOP fs = getFsFromId_checked(fsRef);
    fs.setLongValue(getTypeSystemImpl().getFeatureForCode_checked(featureCode), value);
   
  }

  @Override
  public void ll_setLongValue(int fsRef, int featureCode, long value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    ll_setLongValue(fsRef, featureCode, value);
  }

  @Override
  public void ll_setDoubleValue(int fsRef, int featureCode, double value) {
    ll_setLongValue(fsRef, featureCode, Double.doubleToLongBits(value));
  }

  @Override
  public void ll_setDoubleValue(int fsRef, int featureCode, double value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    ll_setDoubleValue(fsRef, featureCode, value);
  }

  @Override
  public byte ll_getByteArrayValue(int fsRef, int position) {
    return ((ByteArray) getFsFromId_checked(fsRef)).get(position);
  }

  @Override
  public byte ll_getByteArrayValue(int fsRef, int position, boolean doTypeChecks) {
    return ll_getByteArrayValue(fsRef, position);
  }

  @Override
  public boolean ll_getBooleanArrayValue(int fsRef, int position) {
    return ((BooleanArray) getFsFromId_checked(fsRef)).get(position);
  }

  @Override
  public boolean ll_getBooleanArrayValue(int fsRef, int position, boolean doTypeChecks) {
    return ll_getBooleanArrayValue(fsRef, position);
  }

  @Override
  public short ll_getShortArrayValue(int fsRef, int position) {
    return ((ShortArray) getFsFromId_checked(fsRef)).get(position);
  }

  @Override
  public short ll_getShortArrayValue(int fsRef, int position, boolean doTypeChecks) {
    return ll_getShortArrayValue(fsRef, position);
  }

  @Override
  public long ll_getLongArrayValue(int fsRef, int position) {
    return ((LongArray) getFsFromId_checked(fsRef)).get(position);
  }

  @Override
  public long ll_getLongArrayValue(int fsRef, int position, boolean doTypeChecks) {
    return ll_getLongArrayValue(fsRef, position);
  }

  @Override
  public double ll_getDoubleArrayValue(int fsRef, int position) {
    return ((DoubleArray) getFsFromId_checked(fsRef)).get(position);
  }

  @Override
  public double ll_getDoubleArrayValue(int fsRef, int position, boolean doTypeChecks) {
    return ll_getDoubleArrayValue(fsRef, position);
  }

  @Override
  public void ll_setByteArrayValue(int fsRef, int position, byte value) {
    ((ByteArray) getFsFromId_checked(fsRef)).set(position, value);
  }

  @Override
  public void ll_setByteArrayValue(int fsRef, int position, byte value, boolean doTypeChecks) {
    ll_setByteArrayValue(fsRef, position, value);}

  @Override
  public void ll_setBooleanArrayValue(int fsRef, int position, boolean b) {
    ((BooleanArray) getFsFromId_checked(fsRef)).set(position, b);
  }

  @Override
  public void ll_setBooleanArrayValue(int fsRef, int position, boolean value, boolean doTypeChecks) {
    ll_setBooleanArrayValue(fsRef, position, value);
  }

  @Override
  public void ll_setShortArrayValue(int fsRef, int position, short value) {
    ((ShortArray) getFsFromId_checked(fsRef)).set(position, value);
  }

  @Override
  public void ll_setShortArrayValue(int fsRef, int position, short value, boolean doTypeChecks) {
    ll_setShortArrayValue(fsRef, position, value);
  }

  @Override
  public void ll_setLongArrayValue(int fsRef, int position, long value) {
    ((LongArray) getFsFromId_checked(fsRef)).set(position, value);
  }

  @Override
  public void ll_setLongArrayValue(int fsRef, int position, long value, boolean doTypeChecks) {
    ll_setLongArrayValue(fsRef, position, value);
  }

  @Override
  public void ll_setDoubleArrayValue(int fsRef, int position, double d) {
    ((DoubleArray) getFsFromId_checked(fsRef)).set(position, d);
  }

  @Override
  public void ll_setDoubleArrayValue(int fsRef, int position, double value, boolean doTypeChecks) {
    ll_setDoubleArrayValue(fsRef, position, value);
  }

  public boolean isAnnotationType(Type t) {
    return ((TypeImpl)t).isAnnotationType();
  }

  /**
   * @param t the type code to test
   * @return true if that type is subsumed by AnnotationBase type
   */
  public boolean isSubtypeOfAnnotationBaseType(int t) {
    TypeImpl ti = getTypeSystemImpl().getTypeForCode(t);
    if (ti != null) {
      return ti.isAnnotationBaseType();
    }
    return false;
  }
  
  public boolean isBaseCas() {
    return this == getBaseCAS();
  }

  @Override
  public Annotation createAnnotation(Type type, int begin, int end) {
    // duplicates a later check
//    if (this.isBaseCas()) {
//      // Can't create annotation on base CAS
//      throw new CASRuntimeException(CASRuntimeException.INVALID_BASE_CAS_METHOD, "createAnnotation(Type, int, int)");
//    }
    Annotation fs = createFS(type);
    fs.setBegin(begin);
    fs.setEnd(end);
    return fs;
  }
  
  public int ll_createAnnotation(int typeCode, int begin, int end) {
    return createAnnotation(getTypeSystemImpl().getTypeForCode(typeCode), begin, end).id();
  }
  
  /**
   * The generic spec T extends AnnotationFS (rather than AnnotationFS) allows the method
   * JCasImpl getAnnotationIndex to return Annotation instead of AnnotationFS
   * @param <T> the Java class associated with the annotation index
   * @return the annotation index
   */
  @Override
  public <T extends AnnotationFS> AnnotationIndex<T> getAnnotationIndex() {
    return (AnnotationIndex<T>) indexRepository.getAnnotationIndex(getTypeSystemImpl().annotType);
  }
  
  

  /* (non-Javadoc)
   * @see org.apache.uima.cas.CAS#getAnnotationIndex(org.apache.uima.cas.Type)
   */
  @Override
  public <T extends AnnotationFS> AnnotationIndex<T> getAnnotationIndex(Type type)
      throws CASRuntimeException {
    return (AnnotationIndex<T>) indexRepository.getAnnotationIndex((TypeImpl) type);
  }

  /**
   * @see org.apache.uima.cas.CAS#getAnnotationType()
   */
  @Override
  public Type getAnnotationType() {
    return getTypeSystemImpl().annotType;
  }

  /**
   * @see org.apache.uima.cas.CAS#getEndFeature()
   */
  @Override
  public Feature getEndFeature() {
    return getTypeSystemImpl().endFeat;
  }

  /**
   * @see org.apache.uima.cas.CAS#getBeginFeature()
   */
  @Override
  public Feature getBeginFeature() {
    return getTypeSystemImpl().startFeat;
  }

  private <T extends AnnotationFS> T createDocumentAnnotation(int length) {
    final TypeSystemImpl ts = getTypeSystemImpl();
    // Remove any existing document annotations.
    FSIterator<T> it = this.<T>getAnnotationIndex(ts.docType).iterator();
    List<T> list = new ArrayList<T>();
    while (it.isValid()) {
      list.add(it.get());
      it.moveToNext();
    }
    for (int i = 0; i < list.size(); i++) {
      getIndexRepository().removeFS(list.get(i));
    }
    
    AnnotationFS docAnnot = createAnnotation(ts.docType, 0, length);
    docAnnot.setStringValue(ts.langFeat, CAS.DEFAULT_LANGUAGE_NAME);
    addFsToIndexes(docAnnot);
    return (T) docAnnot;
  }
  
  private <T extends AnnotationFS> T createDocumentAnnotationNoRemove(int length) {
    final TypeSystemImpl ts = getTypeSystemImpl();
    AnnotationFS docAnnot = createAnnotation(ts.docType, 0, length);
    docAnnot.setStringValue(ts.langFeat, CAS.DEFAULT_LANGUAGE_NAME);
    addFsToIndexes(docAnnot);
    return (T) docAnnot;    
  }
  
  public <T extends Annotation> T createDocumentAnnotationNoRemoveNoIndex(int length) {
    final TypeSystemImpl ts = getTypeSystemImpl();
    AnnotationFS docAnnot = createAnnotation(ts.docType, 0, length);
    docAnnot.setStringValue(ts.langFeat, CAS.DEFAULT_LANGUAGE_NAME);
    return (T) docAnnot;    
  }
  
  
  public int ll_createDocumentAnnotation(int length) {
    final int fsRef = ll_createDocumentAnnotationNoIndex(0, length);
    ll_getIndexRepository().ll_addFS(fsRef);
    return fsRef;
  }
  
  public int ll_createDocumentAnnotationNoIndex(int begin, int end) {
    final TypeSystemImpl ts = getTypeSystemImpl();
    int fsRef = ll_createAnnotation(ts.docType.getCode(), begin, end);
    ll_setStringValue(fsRef, ts.langFeat.getCode(), CAS.DEFAULT_LANGUAGE_NAME);
    return fsRef;
  }

  // For the "built-in" instance of Document Annotation, set the
  // "end" feature to be the length of the sofa string
  /**
   * updates the document annotation setting the end feature to be the length of the sofa string, if any.
   * creates the document annotation if not present unless null = local string data
   * only works if not in the base cas
   * @return the document annotation
   */
  public void updateDocumentAnnotation() {
    if (!mySofaIsValid() || this == this.svd.baseCAS) {
      return;
    }
    String newDoc = this.mySofaRef.getLocalStringData();
    if (null != newDoc) {
      Annotation docAnnot = getDocumentAnnotationNoCreate();
      if (docAnnot != null) {
        boolean wasRemoved = this.indexRepository.removeIfInCorrputableIndexInThisView(docAnnot);
        docAnnot.setIntValue(getTypeSystemImpl().endFeat, newDoc.length());
        if (wasRemoved) {
          ((FSIndexRepositoryImpl)ll_getIndexRepository()).addback(docAnnot);
        }
      } else {
        // not in the index (yet)
        createDocumentAnnotation(newDoc.length());
      }
    }
    return;
  }
  
  /**
   * Generic issue:  The returned document annotation could be either an instance of 
   *   DocumentAnnotation or an instance of Annotation - the Java cover class used for 
   *   annotations when JCas is not being used.
   */
  @Override
  public <T extends AnnotationFS> T getDocumentAnnotation() {
    T docAnnot = (T) getDocumentAnnotationNoCreate();
    if (null == docAnnot) {
      return (T) createDocumentAnnotationNoRemove(0);
    } else {
      return docAnnot;
    }
  }
  
  public <T extends AnnotationFS> T getDocumentAnnotationNoCreate() {
    if (this == this.svd.baseCAS) {
      // base CAS has no document
      return null;
    }
    FSIterator<T> it = this.<T>getAnnotationIndex(getTypeSystemImpl().docType).iterator();
    if (it.isValid()) {
      return it.get();
    }
    return null;
  }
  
  /**
   * 
   * @return the fs addr of the document annotation found via the index, or 0 if not there
   */
  public int ll_getDocumentAnnotation() {
    if (this == this.svd.baseCAS) {
      // base CAS has no document
      return 0;
    }
    
    FSIterator<FeatureStructure> it = getIndexRepository().getIndex(CAS.STD_ANNOTATION_INDEX, getTypeSystemImpl().docType).iterator();
    if (it.isValid()) {
      return it.get().id();
    }
    return 0;
  }
  
  @Override
  public String getDocumentLanguage() {
    if (this == this.svd.baseCAS) {
      // base CAS has no document
      return null;
    }
    final int docAnnotAddr = ll_getFSRef(getDocumentAnnotation());
    return ll_getStringValue(docAnnotAddr, TypeSystemImpl.langFeatCode);
  }

  @Override
  public String getDocumentText() {
    return this.getSofaDataString();
  }

  @Override
  public String getSofaDataString() {
    if (this == this.svd.baseCAS) {
      // base CAS has no document
      return null;
    }
    return mySofaIsValid() ? mySofaRef.getLocalStringData() : null;
  }

  @Override
  public FeatureStructure getSofaDataArray() {
    if (this == this.svd.baseCAS) {
      // base CAS has no Sofa
      return null;
    }
    return mySofaIsValid() ? mySofaRef.getLocalFSData() : null;
  }

  @Override
  public String getSofaDataURI() {
    if (this == this.svd.baseCAS) {
      // base CAS has no Sofa
      return null;
    }
    return mySofaIsValid() ? mySofaRef.getSofaURI() : null;
  }

  @Override
  public InputStream getSofaDataStream() {
    if (this == this.svd.baseCAS) {
      // base CAS has no Sofa nothin
      return null;
    }
//  return mySofaRef.getSofaDataStream();  // this just goes to the next method
    return mySofaIsValid() ? this.getSofaDataStream(mySofaRef) : null; 
 
  }

  @Override
  public String getSofaMimeType() {
    if (this == this.svd.baseCAS) {
      // base CAS has no Sofa
      return null;
    }
    return mySofaIsValid() ? mySofaRef.getSofaMime() : null;
  }

  @Override
  public Sofa getSofa() {
    return mySofaRef;
  }
  
  /**
   * @return the addr of the sofaFS associated with this view, or 0
   */
  @Override
  public int ll_getSofa() {
    return mySofaIsValid() ? mySofaRef.id() : 0;
  }

  @Override
  public String getViewName() {
    return (this == getViewFromSofaNbr(1)) ? CAS.NAME_DEFAULT_SOFA :
           mySofaIsValid() ? mySofaRef.getSofaID() : 
           null; 
  }

  private boolean mySofaIsValid() {
    return this.mySofaRef != null;
  }

  void setDocTextFromDeserializtion(String text) {
    if (mySofaIsValid()) {
      SofaFS sofa = getSofaRef();  // creates sofa if doesn't already exist
      sofa.setLocalSofaData(text);
    }
  }

  @Override
  public void setDocumentLanguage(String languageCode) {
    if (this == this.svd.baseCAS) {
      throw new CASRuntimeException(CASRuntimeException.INVALID_BASE_CAS_METHOD, "setDocumentLanguage(String)");
    }
    AnnotationFS docAnnot = getDocumentAnnotation();
    languageCode = Language.normalize(languageCode);
    docAnnot.setStringValue(getTypeSystemImpl().langFeat, languageCode);
  }

  private void setSofaThingsMime(Consumer<Sofa> c, String msg) {
    if (this == this.svd.baseCAS) {
      throw new CASRuntimeException(CASRuntimeException.INVALID_BASE_CAS_METHOD, msg);
    }   
    Sofa sofa = getSofaRef();
    c.accept(sofa);
  }

  @Override
  public void setDocumentText(String text) {
    setSofaDataString(text, "text");
  }

  @Override
  public void setSofaDataString(String text, String mime) throws CASRuntimeException {
    setSofaThingsMime(sofa -> sofa.setLocalSofaData(text, mime), "setSofaDataString(text, mime)");
  }

  @Override
  public void setSofaDataArray(FeatureStructure array, String mime) {
    setSofaThingsMime(sofa -> sofa.setLocalSofaData(array, mime), "setSofaDataArray(FeatureStructure, mime)");
  }

  @Override
  public void setSofaDataURI(String uri, String mime) throws CASRuntimeException {
    setSofaThingsMime(sofa -> sofa.setRemoteSofaURI(uri, mime), "setSofaDataURI(String, String)");
  }
  
  @Override
  public void setCurrentComponentInfo(ComponentInfo info) {
    // always store component info in base CAS
    this.svd.componentInfo = info;
  }

  ComponentInfo getCurrentComponentInfo() {
    return this.svd.componentInfo;
  }

  /**
   * @see org.apache.uima.cas.CAS#addFsToIndexes(FeatureStructure fs)
   */
  @Override
  public void addFsToIndexes(FeatureStructure fs) {
//    if (fs instanceof AnnotationBaseFS) {
//      final CAS sofaView = ((AnnotationBaseFS) fs).getView();
//      if (sofaView != this) {
//        CASRuntimeException e = new CASRuntimeException(
//            CASRuntimeException.ANNOTATION_IN_WRONG_INDEX, new String[] { fs.toString(),
//                sofaView.getSofa().getSofaID(), this.getSofa().getSofaID() });
//        throw e;
//      }
//    }
    this.indexRepository.addFS(fs);
  }

  /**
   * @see org.apache.uima.cas.CAS#removeFsFromIndexes(FeatureStructure fs)
   */
  @Override
  public void removeFsFromIndexes(FeatureStructure fs) {
    this.indexRepository.removeFS(fs);
  }

  /**
   * @param fs the AnnotationBase instance
   * @return the view associated with this FS where it could be indexed
   */
  public CASImpl getSofaCasView(AnnotationBase fs) {
    return fs._casView;
//    Sofa sofa = fs.getSofa();
//    
//    if (null != sofa && sofa != this.getSofa()) {
//      return (CASImpl) this.getView(sofa.getSofaNum());
//    }
//    
//    /* Note: sofa == null means annotation created from low-level APIs, without setting sofa feature
//     * Ignore this for backwards compatibility */
//    return this;
  }

  @Override
  public CASImpl ll_getSofaCasView(int id) {
    return getSofaCasView(getFsFromId_checked(id));
  }

//  public Iterator<CAS> getViewIterator() {
//    List<CAS> viewList = new ArrayList<CAS>();
//    // add initial view if it has no sofa
//    if (!((CASImpl) getInitialView()).mySofaIsValid()) {
//      viewList.add(getInitialView());
//    }
//    // add views with Sofas
//    FSIterator<SofaFS> sofaIter = getSofaIterator();
//    while (sofaIter.hasNext()) {
//      viewList.add(getView(sofaIter.next()));
//    }
//    return viewList.iterator();
//  }
  
  /**
   * Creates the initial view (without a sofa) if not present
   * @return the number of views, excluding the base view, including the initial view (even if not initially present or no sofa)
   */
  public int getNumberOfViews() {
    CASImpl initialView = (CASImpl) getInitialView();  // creates one if not existing, w/o sofa
    int nbrSofas = this.svd.baseCAS.indexRepository.getIndex(CAS.SOFA_INDEX_NAME).size();
    return initialView.mySofaIsValid() ? nbrSofas : 1 + nbrSofas; 
  }
  
  public int getNumberOfSofas() {
    return this.svd.baseCAS.indexRepository.getIndex(CAS.SOFA_INDEX_NAME).size();
  }
   
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.CAS#getViewIterator()
   */
  @Override
  public Iterator<CAS> getViewIterator() {
    return new Iterator<CAS>() {
      
      final CASImpl initialView = (CASImpl) getInitialView();  // creates one if not existing, w/o sofa  

      boolean isInitialView_but_noSofa = !initialView.mySofaIsValid(); // true if has no Sofa in initial view
                                                           //     but is reset to false once iterator moves
                                                           //     off of initial view.
      
                                                           // if initial view has a sofa, we just use the 
                                                           // sofa iterator instead.

      final FSIterator<Sofa> sofaIter = getSofaIterator(); 

      @Override
      public boolean hasNext() {
        if (isInitialView_but_noSofa) {
          return true;
        }
        return sofaIter.hasNext(); 
      }

      @Override
      public CAS next() {
        if (isInitialView_but_noSofa) {
          isInitialView_but_noSofa = false;  // no incr of sofa iterator because it was missing initial view
          return initialView;
        }
        return getView(sofaIter.next());
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
      
    };
  }

  /**
   * 
   * 
   * @return iterator over all views except the base view
   */
  public Iterator<CASImpl> getViewImplIterator() {
    return new Iterator<CASImpl>() {
      
      final CASImpl initialView = (CASImpl) getInitialView();  // creates one if not existing, w/o sofa  

      boolean isInitialView_but_noSofa = !initialView.mySofaIsValid(); // true if has no Sofa in initial view
                                                           //     but is reset to false once iterator moves
                                                           //     off of initial view.
      
                                                           // if initial view has a sofa, we just use the 
                                                           // sofa iterator instead.

      final FSIterator<Sofa> sofaIter = getSofaIterator(); 

      @Override
      public boolean hasNext() {
        if (isInitialView_but_noSofa) { // set to false once iterator moves off of first value
          return true;
        }
        return sofaIter.hasNext(); 
      }

      @Override
      public CASImpl next() {
        if (isInitialView_but_noSofa) {
          isInitialView_but_noSofa = false;  // no incr of sofa iterator because it was missing initial view
          return initialView;
        }
        return (CASImpl) getView(sofaIter.next());
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
      
    };
  }
  
  void forAllViews(Consumer<CASImpl> processViews) {
    Iterator<CASImpl> it = getViewImplIterator();
    while (it.hasNext()) {
      processViews.accept(it.next());
    }
  }

  void forAllSofas(Consumer<Sofa> processSofa) {
    FSIterator<Sofa> it = getSofaIterator();
    while (it.hasNext()) {
      processSofa.accept(it.next());
    }
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.CAS#getViewIterator(java.lang.String)
   */
  @Override
  public Iterator<CAS> getViewIterator(String localViewNamePrefix) {
    // do sofa mapping for current component
    String absolutePrefix = null;
    if (getCurrentComponentInfo() != null) {
      absolutePrefix = getCurrentComponentInfo().mapToSofaID(localViewNamePrefix);
    }
    if (absolutePrefix == null) {
      absolutePrefix = localViewNamePrefix;
    }

    // find Sofas with this prefix
    List<CAS> viewList = new ArrayList<CAS>();
    FSIterator<Sofa> sofaIter = getSofaIterator();
    while (sofaIter.hasNext()) {
      SofaFS sofa = sofaIter.next();
      String sofaId = sofa.getSofaID();
      if (sofaId.startsWith(absolutePrefix)) {
        if ((sofaId.length() == absolutePrefix.length())
            || (sofaId.charAt(absolutePrefix.length()) == '.')) {
          viewList.add(getView(sofa));
        }
      }
    }
    return viewList.iterator();
  }
    
  /**
   * protectIndexes
   * 
   * Within the scope of protectIndexes, 
   *   feature updates are checked, and if found to be a key, and the FS is in a corruptable index,
   *     then the FS is removed from the indexes (in all necessary views) (perhaps multiple times
   *     if the FS was added to the indexes multiple times), and this removal is recorded on
   *     an new instance of FSsTobeReindexed appended to fssTobeAddedback.
   *     
   *   Later, when the protectIndexes is closed, the tobe items are added back to the indies.
   */
  @Override
  public AutoCloseable protectIndexes() {
    FSsTobeAddedback r = FSsTobeAddedback.createMultiple(this);
    svd.fssTobeAddedback.add(r);
    return r;
  }
  
  void dropProtectIndexesLevel () {
    svd.fssTobeAddedback.remove(svd.fssTobeAddedback.size() -1);
  }
  
  /**
   * This design is to support normal operations where the
   *   addbacks could be nested
   * It also handles cases where nested ones were inadvertently left open
   * Three cases:
   *    1) the addbacks are the last element in the stack
   *         - remove it from the stack
   *    2) the addbacks are (no longer) in the list
   *         - leave stack alone
   *    3) the addbacks are in the list but not at the end
   *         - remove it and all later ones     
   *  
   * If the "withProtectedindexes" approach is used, it guarantees proper 
   * nesting, but the Runnable can't throw checked exceptions.
   * 
   * You can do your own try-finally blocks (or use the try with resources
   * form in Java 8 to do a similar thing with no restrictions on what the
   * body can contain.
   * 
   * @param addbacks
   */
  void addbackModifiedFSs (FSsTobeAddedback addbacks) {
    final List<FSsTobeAddedback> s =  svd.fssTobeAddedback;
    if (s.get(s.size() - 1) == addbacks) {
      s.remove(s.size());
    } else {
      int pos = s.indexOf(addbacks);
      if (pos >= 0) {
        for (int i = s.size() - 1; i > pos; i--) {
          s.remove(i);
          s.get(i).addback();
        }
      }      
    }
    addbacks.addback();
  }
  
  /**
   * 
   * @param r an inner block of code to be run with 
   */
  @Override
  public void protectIndexes(Runnable r) {
    AutoCloseable addbacks = protectIndexes();
    try {
      r.run();
    } finally {
      addbackModifiedFSs((FSsTobeAddedback) addbacks);
    }
  }
  

  /**
   * The current implementation only supports 1 marker call per 
   * CAS.  Subsequent calls will throw an error.
   * 
   * The design is intended to support (at some future point)
   * multiple markers; for this to work, the intent is to 
   * extend the MarkerImpl to keep track of indexes into
   * these IntVectors specifying where that marker starts/ends.
   */
  @Override
  public Marker createMarker() {
    if (!this.svd.flushEnabled) {
	  throw new CASAdminException(CASAdminException.FLUSH_DISABLED);
  	}
  	this.svd.trackingMark = new MarkerImpl(this.getLastUsedFsId() + 1, 
  			this);
  	if (this.svd.modifiedPreexistingFSs == null) {
  	  this.svd.modifiedPreexistingFSs = new IdentityHashMap<>();
  	} else {errorMultipleMarkers();}

  	if (this.svd.trackingMarkList == null) {
  	  this.svd.trackingMarkList = new ArrayList<MarkerImpl>();
  	} else {errorMultipleMarkers();}
  	this.svd.trackingMarkList.add(this.svd.trackingMark);
  	return this.svd.trackingMark;
  }
    
  private void errorMultipleMarkers() {
    throw new CASRuntimeException(CASRuntimeException.MULTIPLE_CREATE_MARKER);
  }
  
  
  // made public https://issues.apache.org/jira/browse/UIMA-2478
  public MarkerImpl getCurrentMark() {
	  return this.svd.trackingMark;
  }
  
  Collection<FsChange> getModifiedFSList() {
	  return this.svd.modifiedPreexistingFSs.values();
  }
  
  boolean isInModifiedPreexisting(TOP fs) {
    return this.svd.modifiedPreexistingFSs.containsKey(fs);
  }
    
  @Override
  public String toString() {
    String sofa =  (mySofaRef == null) ? (isBaseCas() ? "Base CAS" : "_InitialView or no Sofa") :
//                 (mySofaRef == 0) ? "no Sofa" :
                   mySofaRef.getSofaID();
    return this.getClass().getSimpleName() + ":" + getCasId() + "[view: " + sofa + "]";
  }
    
  int getCasResets() {
    return svd.casResets.get();
  }
  
  int getCasId() {
    return svd.casId;
  }
  
  public int setId2fs(FeatureStructureImplC fs) {
    svd.id2fs.add(fs);
//    if (svd.id2fs.size() != (2 + svd.fsIdGenerator.get())) {
//      System.out.println("debug out of sync id generator and id2fs size");
//    }
    assert(svd.id2fs.size() == (2 + svd.fsIdGenerator.get()));
    return getNextFsId();
  }
  
  private int getNextFsId() {
    return svd.fsIdGenerator.incrementAndGet();
  }
  
  public int getLastUsedFsId() {
    return svd.fsIdGenerator.get();
  }
    
  public <T extends FeatureStructure> T getFsFromId(int id) {
    return this.svd.id2fs.getWithMissingIsNull(id);
  }
  
//  /**
//   * Get the Java class corresponding to a particular type
//   * Only valid after type system commit
//   * 
//   * @param type
//   * @return
//   */
//  public <T extends FeatureStructure> Class<T> getClass4Type(Type type) {
//    TypeSystemImpl tsi = getTypeSystemImpl();
//    if (!tsi.isCommitted()) {
//      throw new CASRuntimeException(CASRuntimeException.GET_CLASS_FOR_TYPE_BEFORE_TS_COMMIT);
//    }
//    
//  }
  
  public static boolean isSameCAS(CAS c1, CAS c2) {
    CASImpl ci1 = (CASImpl) c1.getLowLevelCAS();
    CASImpl ci2 = (CASImpl) c2.getLowLevelCAS();
    return ci1.getBaseCAS() == ci2.getBaseCAS();
  }
  
  public boolean isInCAS(FeatureStructure fs) {
    return ((TOP)fs)._casView.getBaseCAS() == this.getBaseCAS();
  }
  
  private Object getFsGenerator(int typecode) {
    return getTypeSystemImpl().getFSClassRegistry().getGenerator(typecode);
  }
  
  public final void checkArrayPreconditions(int len) throws CASRuntimeException {
    // Check array size.
    if (len < 0) {
      throw new CASRuntimeException(CASRuntimeException.ILLEGAL_ARRAY_SIZE);
    }
  }

  public EmptyFSList getEmptyFSList() {
    if (null == svd.emptyFSList) {
      svd.emptyFSList = new EmptyFSList(getTypeSystemImpl().fsEListType, this);
    }
    return svd.emptyFSList;
  }

  public EmptyFloatList getEmptyFloatList() {
    if (null == svd.emptyFloatList) {
      svd.emptyFloatList = new EmptyFloatList(getTypeSystemImpl().floatEListType, this);
    }
    return svd.emptyFloatList;
  }
  
  public EmptyIntegerList getEmptyIntegerList() {
    if (null == svd.emptyIntegerList) {
      svd.emptyIntegerList = new EmptyIntegerList(getTypeSystemImpl().intEListType, this);
    }
    return svd.emptyIntegerList;
  }
  
  public EmptyStringList getEmptyStringList() {
    if (null == svd.emptyStringList) {
      svd.emptyStringList = new EmptyStringList(getTypeSystemImpl().stringEListType, this);
    }
    return svd.emptyStringList;
  }
  
  public EmptyList getEmptyList(int rangeCode) {
    return (rangeCode == CasSerializerSupport.TYPE_CLASS_INTLIST) ? getEmptyIntegerList() :
           (rangeCode == CasSerializerSupport.TYPE_CLASS_FLOATLIST) ? getEmptyFloatList() :
           (rangeCode == CasSerializerSupport.TYPE_CLASS_STRINGLIST) ? getEmptyStringList() :
                                                                       getEmptyFSList();
  }
  
  /**
   * Get an empty list from the type code of a list
   * @param rangeCode
   * @return
   */
  public EmptyList getEmptyListFromTypeCode(int rangeCode) {
    switch (rangeCode) {
    case TypeSystemImpl.fsListTypeCode:
    case TypeSystemImpl.fsEListTypeCode:
    case TypeSystemImpl.fsNeListTypeCode: return getEmptyFSList();
    case TypeSystemImpl.floatListTypeCode:
    case TypeSystemImpl.floatEListTypeCode:
    case TypeSystemImpl.floatNeListTypeCode: return getEmptyFloatList();
    case TypeSystemImpl.intListTypeCode:
    case TypeSystemImpl.intEListTypeCode:
    case TypeSystemImpl.intNeListTypeCode: return getEmptyIntegerList();
    case TypeSystemImpl.stringListTypeCode:
    case TypeSystemImpl.stringEListTypeCode:
    case TypeSystemImpl.stringNeListTypeCode: return getEmptyStringList();
    default: throw new IllegalArgumentException();
    }
  }

  public TypeImpl getTypeImplFromJCasTypeIndex(int typeIndexID) {
    return getTypeSystemImpl().getJCasRegisteredType(typeIndexID);
  }
  
  /**
   * Copies a feature, from one fs to another
   *   FSs may belong to different CASes, but must have the same type system
   *   Features must have compatible ranges
   * @param fsSrc source FS
   * @param fi Feature to copy
   * @param fsTgt target FS
   */
  public static void copyFeature(TOP fsSrc, FeatureImpl fi, TOP fsTgt) {
    if (!copyFeatureExceptFsRef(fsSrc, fi, fsTgt, fi)) {
      if (!fi.isAnnotBaseSofaRef) {
        fsTgt.setFeatureValue(fi, fsSrc.getFeatureValue(fi));
      }
    }
  }
  
  /**
   * Copies a feature from one fs to another
   * FSs may be in different type systems 
   *   Doesn't copy a feature ref, but instead returns false.  
   *     This is because feature refs can't cross CASes
   * @param fsSrc source FS
   * @param fiSrc feature in source to copy
   * @param fsTgt target FS
   * @param fiTgt feature in target to set
   * @return false if feature is an fsRef
   */
  public static boolean copyFeatureExceptFsRef(TOP fsSrc, FeatureImpl fiSrc, TOP fsTgt, FeatureImpl fiTgt) {
    switch (fiSrc.getRangeImpl().getCode()) {
    case TypeSystemImpl.booleanTypeCode    : fsTgt.setBooleanValue(   fiTgt, fsSrc.getBooleanValue(   fiSrc)); break;
    case TypeSystemImpl.byteTypeCode       : fsTgt.setByteValue(      fiTgt, fsSrc.getByteValue(      fiSrc)); break;
    case TypeSystemImpl.shortTypeCode      : fsTgt.setShortValue(     fiTgt, fsSrc.getShortValue(     fiSrc)); break;
    case TypeSystemImpl.intTypeCode        : fsTgt.setIntValue(       fiTgt, fsSrc.getIntValue(       fiSrc)); break;
    case TypeSystemImpl.longTypeCode       : fsTgt.setLongValue(      fiTgt, fsSrc.getLongValue(      fiSrc)); break;
    case TypeSystemImpl.floatTypeCode      : fsTgt.setFloatValue(     fiTgt, fsSrc.getFloatValue(     fiSrc)); break;
    case TypeSystemImpl.doubleTypeCode     : fsTgt.setDoubleValue(    fiTgt, fsSrc.getDoubleValue(    fiSrc)); break;
    case TypeSystemImpl.stringTypeCode     : fsTgt.setStringValue(    fiTgt, fsSrc.getStringValue(    fiSrc)); break;
    case TypeSystemImpl.javaObjectTypeCode : fsTgt.setJavaObjectValue(fiTgt, fsSrc.getJavaObjectValue(fiSrc)); break;
               // skip setting sofaRef - it's final and can't be set
    default: return false;
    } // end of switch
    return true;
  }

  public static CommonArray copyArray(TOP srcArray) {
    CommonArray srcCA = (CommonArray) srcArray;
    CommonArray copy = (CommonArray) srcArray._casView.createArray(srcArray._typeImpl, srcCA.size());
    copy.copyValuesFrom(srcCA); 
    return copy;
  }
    
  public BinaryCasSerDes getBinaryCasSerDes() {
    return svd.bcsd;
  }
  

}
