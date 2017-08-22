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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.SofaID;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.CommonSerDes.Header;
import org.apache.uima.cas.impl.CommonSerDes.Reading;
import org.apache.uima.cas.impl.FSsTobeAddedback.FSsTobeAddedbackSingle;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.cas.text.Language;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.MiscImpl;
import org.apache.uima.internal.util.PositiveIntSet_impl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.impl.JCasImpl;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasLoadMode;
import org.apache.uima.util.Level;
import org.apache.uima.util.Misc;

/**
 * Implements the CAS interfaces. This class must be public because we need to
 * be able to create instance of it from outside the package. Use at your own
 * risk. May change without notice.
 * 
 */
public class CASImpl extends AbstractCas_ImplBase implements CAS, CASMgr, LowLevelCAS {
  
  private static final String TRACE_FSS = "uima.trace_fs_creation_and_updating";
  private static final boolean trace = false;
  public static final boolean traceFSs = // false;  // debug - trace FS creation and update
      Misc.getNoValueSystemProperty(TRACE_FSS);

  private static final String traceFile = "traceFSs.log.txt";
  private static final PrintStream traceOut;
  static {
    try {
      if (traceFSs) {
        System.out.println("Creating traceFSs file in directory " + System.getProperty("user.dir"));
        traceOut = traceFSs ? new PrintStream(new BufferedOutputStream(new FileOutputStream(traceFile, false))) : null;
      } else {
        traceOut = null;
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
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

  private static final int[] INT0 = new int[0];
  
  public static final int DEFAULT_INITIAL_HEAP_SIZE = 500000;

  public static final int DEFAULT_RESET_HEAP_SIZE = 5000000;
//  no longer used 3/2015
//  private static final int resetHeapSize = DEFAULT_RESET_HEAP_SIZE;


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

  private static final boolean DEFAULT_USE_FS_CACHE = false;

  // this next seemingly non-sensical static block
  // is to force the classes needed by Eclipse debugging to load
  // otherwise, you get a com.sun.jdi.ClassNotLoadedException when
  // the class is used as part of formatting debugging messages
  static {
    new DebugNameValuePair(null, null);
    new DebugFSLogicalStructure();
  }

  private static enum ModifiedHeap { FSHEAP, BYTEHEAP, SHORTHEAP, LONGHEAP };
  // Static classes representing shared instance data
  // - shared data is computed once for all views

  // fields shared among all CASes belong to views of a common base CAS
  private static class SharedViewData {

    final private Heap heap;

    // private SymbolTable stringTable;
    // private ArrayList stringList;
    final private StringHeap stringHeap = new StringHeap();

    final private ByteHeap byteHeap = new ByteHeap(); // for storing 8 bit values

    final private ShortHeap shortHeap = new ShortHeap(); // for storing 16 bit values

    final private LongHeap longHeap = new LongHeap(); // for storing 64 bit values

    // for efficiency in accessing the begin and end offsets of annotations
    private int annotFeatOffset_begin;
    private int annotFeatOffset_end;
    
    // Base CAS for all views
    final private CASImpl baseCAS;

    private int cache_not_in_index = 0; // a one item cache of a FS not in the index
    
    private final PositiveIntSet_impl featureCodesInIndexKeys = new PositiveIntSet_impl(); 
    
    // A map from Sofas to IndexRepositories.
    private Map<Integer, FSIndexRepository> sofa2indexMap;

    // A map from Sofa numbers to CAS views.
    // number 0 - not used
    // number 1 - used for view named "_InitialView"
    // number 2-n used for other views
//    private Map<Integer, CAS> sofaNbr2ViewMap;
    private ArrayList<CAS> sofaNbr2ViewMap;

    // set of instantiated sofaNames
    private Set<String> sofaNameSet;

    // Flag that initial Sofa has been created
    private boolean initialSofaCreated = false;

    // Count of Views created in this cas
    // equals count of sofas except if initial view has no sofa.
    private int viewCount;

    // The ClassLoader that should be used by the JCas to load the generated
    // FS cover classes for this CAS. Defaults to the ClassLoader used
    // to load the CASImpl class.
    private ClassLoader jcasClassLoader = this.getClass().getClassLoader();

    private ClassLoader previousJCasClassLoader = this.jcasClassLoader;

    // If this CAS can be flushed (reset) or not.
    // often, the framework disables this before calling users code
    private boolean flushEnabled = true;

    // controls whether Java cover objects for CAS objects,
    // including JCas objects,
    // are cached and reused.
    // If set true, don't also cache the JCas ones - this will
    // duplicate the space with no benefit
    private final boolean useFSCache;

    // this is the actual cache. It is simply an array of the same size
    // as the heap, with the CAS object's addr slot filled with
    // a (strong) ref to the Java object.
    // This is a trade off verses using hash tables

    // The actual cache.
    // TODO implement the resizing algorithm used for the main heap, here too.
    private FeatureStructure[] fsArray;

    // not final because set with reinit deserialization
    private CASMetadata casMetadata;

    private ComponentInfo componentInfo;

    private FSGenerator<? extends FeatureStructure>[] localFsGenerators;
    
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
     */
    private MarkerImpl trackingMark;
    
    private IntVector modifiedPreexistingFSs;
    
    private IntVector modifiedFSHeapCells;
    
    private IntVector modifiedByteHeapCells;
    
    private IntVector modifiedShortHeapCells;
    
    private IntVector modifiedLongHeapCells;
    
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
    
    private final AtomicInteger casResets = new AtomicInteger(0);
    
    private final int casId;
    
    private final  StringBuilder traceFScreationSb = traceFSs ? new StringBuilder() : null;
    private int traceFSid = 0; 
    private boolean traceFSisCreate;    
    
    private SharedViewData(boolean useFSCache, Heap heap, CASImpl baseCAS, CASMetadata casMetadata) {
      this.useFSCache = useFSCache;
      this.heap = heap;
      this.baseCAS = baseCAS;
      this.casMetadata = casMetadata;
      casId = casIdProvider.incrementAndGet();
    }
  }
  
  // -----------------------------------------------------
  // Non-shared instance data for base CAS and each view
  // -----------------------------------------------------

  // package protected to let other things share this info
  final SharedViewData svd; // shared view data
  
  void addbackSingle(int fsAddr) {
    svd.fsTobeAddedbackSingle.addback(fsAddr);
    svd.fsTobeAddedbackSingleInUse = false;
//    System.out.format("debug return (addbackSingle) add back single for cas %s%n", this.getViewName());    
  }
  
  void resetAddbackSingleInUse() {
//    System.out.format("debug return (reset) add back single for cas %s%n", this.getViewName());
    svd.fsTobeAddedbackSingleInUse = false;
  }
  
  FSsTobeAddedbackSingle getAddbackSingle() {
    if (svd.fsTobeAddedbackSingleInUse) {
      throw new RuntimeException(); // internal error
    }
//    System.out.format("debug checkout add back single for cas %s%n", this.getViewName());
    svd.fsTobeAddedbackSingleInUse = true;
    return svd.fsTobeAddedbackSingle;
  }
  
  void featureCodesInIndexKeysAdd(int featCode) {
    svd.featureCodesInIndexKeys.add(featCode);
  }
  
  void maybeClearCacheNotInIndex(int fsAddr) {
    if (svd.cache_not_in_index == fsAddr) {
      svd.cache_not_in_index = 0;
    }
  }
  
  /**
   * Called by feature setters which know the FS is not in any index
   * to bypass any index corruption checking, e.g., CasCopier
   * 
   * Internal use only
   * @param fsAddr the address of the feature structure
   */
  public void setCacheNotInIndex(int fsAddr) {
    svd.cache_not_in_index = fsAddr;
  }

  // The index repository. Referenced by XmiCasSerializer
  FSIndexRepositoryImpl indexRepository;

  // the sofaFS this view is based on
  // SofaFS mySofa;
  /**
   * The heap address of the sofa FS for this view, or
   * -1 if the sofa FS is for the initial view, or
   * 0 if there is no sofa FS - for instance, in the "base cas"
   */
  private int mySofaRef = 0;

  private JCasImpl jcas = null;
  
  private final boolean isUsedJcasCache;

  private final ArrayList<String> getStringList() {
    ArrayList<String> stringList = new ArrayList<String>();
    stringList.add(null);
    int pos = this.getStringHeap().getLeastStringCode();
    final int end = this.getStringHeap().getLargestStringCode();
    while (pos <= end) {
      stringList.add(this.getStringHeap().getStringForCode(pos));
      ++pos;
    }
    return stringList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.admin.CASMgr#setCAS(org.apache.uima.cas.CAS)
   *      Internal use Never called Kept because it's in the interface.
   */
  @Override
public void setCAS(CAS cas) {

    // this.indexRepository = ((CASImpl)cas).indexRepository; // only for test
    // case, others override later
    // this.svd.casMetadata.fsClassRegistry = casImpl.fsClassReg;

    // initTypeCodeVars();
    // this.jcas = in.jcas;
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

  public CASImpl(TypeSystemImpl typeSystem, int initialHeapSize, boolean useJcasCache) {
    this(typeSystem, initialHeapSize, DEFAULT_USE_FS_CACHE, useJcasCache);
  }

  /*
   * Configure a new (base view) CASImpl, **not a new view** typeSystem can be
   * null, in which case a new instance of TypeSystemImpl is set up, but not
   * committed. If typeSystem is not null, it is committed (locked). ** Note: it
   * is assumed that the caller of this will always set up the initial view **
   * by calling
   */

  CASImpl(TypeSystemImpl typeSystem, int initialHeapSize, boolean useFSCache, boolean useJcasCache) {
    super();
    this.isUsedJcasCache = useJcasCache;
    TypeSystemImpl ts;
    final boolean externalTypeSystem = (typeSystem != null);

    if (externalTypeSystem) {
      ts = typeSystem;
    } else {
      ts = new TypeSystemImpl(); // creates also new CASMetadata and
      // FSClassRegistry instances
    }

    this.svd = new SharedViewData(useFSCache, new Heap(initialHeapSize), this, ts.casMetadata);
//    this.svd.baseCAS = this;

//    this.svd.heap = new Heap(initialHeapSize);

    if (externalTypeSystem) {
      commitTypeSystem();
    }

    this.svd.sofa2indexMap = new HashMap<Integer, FSIndexRepository>();
    this.svd.sofaNbr2ViewMap = new ArrayList<CAS>();
    this.svd.sofaNameSet = new HashSet<String>();
    this.svd.initialSofaCreated = false;
    this.svd.viewCount = 0;
    
    clearTrackingMarks();
  }

  /**
   * Constructor. Use only if you want to use the low-level APIs.
   */
  public CASImpl() {
    this(DEFAULT_INITIAL_HEAP_SIZE, CASFactory.USE_JCAS_CACHE_DEFAULT);
  }

  public CASImpl(int initialHeapSize, boolean useJcasCache) {
    this((TypeSystemImpl) null, initialHeapSize, useJcasCache);
  }

  // In May 2007, appears to have 1 caller, createCASMgr in Serialization class,
  // could have
  // out-side the framework callers because it is public.
  public CASImpl(CASMgrSerializer ser) {
    this(ser.getTypeSystem(), DEFAULT_INITIAL_HEAP_SIZE, CASFactory.USE_JCAS_CACHE_DEFAULT);
    checkInternalCodes(ser);
    // assert(ts != null);
    // assert(getTypeSystem() != null);
    this.indexRepository = ser.getIndexRepository(this);
  }

  // Use this when creating a CAS view
  CASImpl(CASImpl cas, SofaFS aSofa, boolean useJcasCache) {
    this.isUsedJcasCache = useJcasCache;
    
    // these next fields are final and must be set in the constructor
    this.svd = cas.svd;

    // this.mySofa = aSofa;
    if (aSofa != null) {
      // save address of SofaFS
      this.mySofaRef = ((FeatureStructureImpl) aSofa).getAddress();
    } else {
      // this is the InitialView
      this.mySofaRef = -1;
    }

    // get the indexRepository for this Sofa
    this.indexRepository = (this.mySofaRef == -1) ? 
        (FSIndexRepositoryImpl) cas.getSofaIndexRepository(1) : 
        (FSIndexRepositoryImpl) cas.getSofaIndexRepository(aSofa);
    if (null == this.indexRepository) {
      // create the indexRepository for this CAS
      // use the baseIR to create a lightweight IR copy
      this.indexRepository = new FSIndexRepositoryImpl(
                                    this, 
                                    (FSIndexRepositoryImpl) cas.getBaseIndexRepository());
      this.indexRepository.commit();
      // save new sofa index
      if (this.mySofaRef == -1) {
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
      this.mySofaRef = ((FeatureStructureImpl) aSofa).getAddress();
    } else {
      // this is the InitialView
      this.mySofaRef = -1;
    }

    // toss the JCas, if it exists
    this.jcas = null;

    // create the indexRepository for this Sofa
    this.indexRepository = new FSIndexRepositoryImpl(this, (FSIndexRepositoryImpl) ((CASImpl) cas)
        .getBaseIndexRepository());
    this.indexRepository.commit();
    // save new sofa index
    if (this.mySofaRef == -1) {
      ((CASImpl) cas).setSofaIndexRepository(1, this.indexRepository);
    } else {
      ((CASImpl) cas).setSofaIndexRepository(aSofa, this.indexRepository);
    }
  }

  private void checkInternalCodes(CASMgrSerializer ser) throws CASAdminException {
    if ((ser.topTypeCode > 0)
        && (ser.topTypeCode != ((TypeImpl) this.svd.casMetadata.ts.getTopType()).getCode())) {
      throw new CASAdminException(CASAdminException.DESERIALIZATION_ERROR);
    }
    if (ser.featureOffsets == null) {
      return;
    }
    if (ser.featureOffsets.length != this.svd.casMetadata.featureOffset.length) {
      throw new CASAdminException(CASAdminException.DESERIALIZATION_ERROR);
    }
    for (int i = 1; i < ser.featureOffsets.length; i++) {
      if (ser.featureOffsets[i] != this.svd.casMetadata.featureOffset[i]) {
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
    final TypeSystemImpl ts = this.svd.casMetadata.ts;
    if (ts.isCommitted()) {
      return ts;
    }
    throw new CASRuntimeException(CASRuntimeException.TYPESYSTEM_NOT_LOCKED);
  }

  @Override
public ConstraintFactory getConstraintFactory() {
    return ConstraintFactory.instance();
  }

  @Override
public <T extends FeatureStructure> T  createFS(Type type) {
    final int typeCode = ((TypeImpl) type).getCode();
    if (!isCreatableType(typeCode)) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.NON_CREATABLE_TYPE,
          new String[] { type.getName(), "CAS.createFS()" });
      throw e;
    }
    return ll_getFSForRef(ll_createFSAnnotCheck(typeCode));
  }
  
  public int ll_createFSAnnotCheck(int typeCode) {
    final int addr = ll_createFS(typeCode);
    final TypeSystemImpl ts = this.svd.casMetadata.ts;
    final boolean isAnnot = ts.subsumes(TypeSystemImpl.annotBaseTypeCode, typeCode);
    if (isAnnot && (this == this.getBaseCAS())) {
      CASRuntimeException e = new CASRuntimeException(
          CASRuntimeException.DISALLOW_CREATE_ANNOTATION_IN_BASE_CAS,
          new String[] { ts.ll_getTypeForCode(typeCode).getName() });
      throw e;
    }
    if (isAnnot) {
      this.setFeatureValueNotJournaled(addr,  TypeSystemImpl.annotSofaFeatCode, this.getSofaRef());
    }
    return addr;
  }

  // public FeatureStructure createPermFS(Type type) {
  // final int addr = createPermFS(((TypeImpl) type).getCode());
  // return getFSClassRegistry().createFS(addr, this);
  // }

  @Override
public ArrayFS createArrayFS(int length) {
    checkArrayPreconditions(length);
    final int addr = createTempArray(TypeSystemImpl.fsArrayTypeCode, length);
    return (ArrayFS) createFS(addr);
  }

  @Override
public IntArrayFS createIntArrayFS(int length) {
    checkArrayPreconditions(length);
    final int addr = createTempArray(TypeSystemImpl.intArrayTypeCode, length);
    return (IntArrayFS) createFS(addr);
  }

  @Override
public FloatArrayFS createFloatArrayFS(int length) {
    checkArrayPreconditions(length);
    final int addr = createTempArray(TypeSystemImpl.floatArrayTypeCode, length);
    return (FloatArrayFS) createFS(addr);
  }

  @Override
public StringArrayFS createStringArrayFS(int length) {
    checkArrayPreconditions(length);
    final int addr = createTempArray(TypeSystemImpl.stringArrayTypeCode, length);
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
    if (this.svd.viewCount != 1) {
      return false;
    }

    if (!this.svd.initialSofaCreated) {
      return false;
    }
    final int llsofa = this.getInitialView().getLowLevelCAS().ll_getSofa();

    // check for mime type exactly equal to "text"
    String sofaMime = ll_getStringValue(llsofa, TypeSystemImpl.sofaMimeFeatCode);
    if (!"text".equals(sofaMime)) {
      return false;
    }
    // check that sofaURI and sofaArray are not set
    String sofaUri = ll_getStringValue(llsofa, TypeSystemImpl.sofaUriFeatCode);
    if (sofaUri != null) {
      return false;
    }
    int sofaArray = ll_getRefValue(llsofa, TypeSystemImpl.sofaArrayFeatCode);
    if (sofaArray != CASImpl.NULL) {
      return false;
    }
    // check that name is NAME_DEFAULT_SOFA
    String sofaname = ll_getStringValue(llsofa, TypeSystemImpl.sofaIdFeatCode);
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

  SofaFS createSofa(String sofaName, String mimeType) {
    final int addr = ll_createFS(TypeSystemImpl.sofaTypeCode);
    final FeatureStructure sofa = ll_getFSForRef(addr);
    addSofa(sofa, sofaName, mimeType);
    return (SofaFS) sofa;
  }

  SofaFS createInitialSofa(String mimeType) {
    final int addr = ll_createFS(TypeSystemImpl.sofaTypeCode);
    final FeatureStructure sofa = ll_getFSForRef(addr);
    // final int llsofa = ll_getFSRef(sofa);
    ll_setIntValue(/* llsofa */addr, TypeSystemImpl.sofaNumFeatCode, 1);
    addSofa(sofa, CAS.NAME_DEFAULT_SOFA, mimeType);
    registerInitialSofa();
    this.mySofaRef = /* ((FeatureStructureImpl)sofa).getAddress() */addr;
    return (SofaFS) sofa;
  }

  void registerInitialSofa() {
    this.svd.initialSofaCreated = true;
  }

  boolean isInitialSofaCreated() {
    return this.svd.initialSofaCreated;
  }

  // Internal use only
  public void addSofa(FeatureStructure sofa, String sofaName, String mimeType) {
    if (this.svd.sofaNameSet.contains(sofaName)) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFANAME_ALREADY_EXISTS,
          new String[] { sofaName });
      throw e;
    }
    final int llsofa = ll_getFSRef(sofa);
    if (0 == ll_getIntValue(llsofa, TypeSystemImpl.sofaNumFeatCode)) {
      ll_setIntValue(llsofa, TypeSystemImpl.sofaNumFeatCode, ++this.svd.viewCount);
    }
    ll_setStringValue(llsofa, TypeSystemImpl.sofaIdFeatCode, sofaName);
    ll_setStringValue(llsofa, TypeSystemImpl.sofaMimeFeatCode, mimeType);
    this.getBaseIndexRepository().addFS(sofa);
    this.svd.sofaNameSet.add(sofaName);
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
    FSIterator<SofaFS> iterator = this.svd.baseCAS.getSofaIterator();
    while (iterator.isValid()) {
      SofaFS sofa = iterator.get();
      if (sofaName.equals(getStringValue(((FeatureStructureImpl) sofa).getAddress(),
          TypeSystemImpl.sofaIdFeatCode))) {
        return sofa;
      }
      iterator.moveToNext();
    }
    CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFANAME_NOT_FOUND,
        new String[] { sofaName });
    throw e;
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
@SuppressWarnings("unchecked")
  public FSIterator<SofaFS> getSofaIterator() {
    FSIndex<SofaFS> sofaIndex =  (FSIndex<SofaFS>) ( FSIndex<?>) this.svd.baseCAS.indexRepository.getIndex(CAS.SOFA_INDEX_NAME);
    return sofaIndex.iterator();
  }

  // For internal use only
  public void setSofaFeat(int addr, int sofa) {
    // never an index key
    setFeatureValueNoIndexCorruptionCheck(addr, TypeSystemImpl.annotSofaFeatCode, sofa);
  }

  // For internal use only
  public int getSofaFeat(int addr) {
    return getFeatureValue(addr, TypeSystemImpl.annotSofaFeatCode);
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
        int arraySize = this.ll_getArraySize(fs.getAddress());
        ByteBuffer buf = null;
        Type type = fs.getType();
        if (type.getName().equals(CAS.TYPE_NAME_STRING_ARRAY)) {
          StringBuffer sb = new StringBuffer();
          for (int i=0; i<((StringArrayFS)fs).size(); i++) {
            if (i==0) {
              sb.append( ((StringArrayFS)fs).get(i) );
            } else {
              sb.append( "\n" + ((StringArrayFS)fs).get(i) );
            }
          }
          ByteArrayInputStream bis = new ByteArrayInputStream( sb.toString().getBytes("UTF-8") );
          return bis;
        } else if (type.getName().equals(CAS.TYPE_NAME_INTEGER_ARRAY)) {
          arrayStart = getArrayStartAddress(fs.getAddress());
          buf = ByteBuffer.allocate(arraySize * 4);
          IntBuffer intbuf = buf.asIntBuffer();
          intbuf.put(this.getHeap().heap, arrayStart, arraySize);
          ByteArrayInputStream bis = new ByteArrayInputStream(buf.array());
          return bis;
        } else if (type.getName().equals(CAS.TYPE_NAME_FLOAT_ARRAY)) {
          arrayStart = getArrayStartAddress(fs.getAddress());
          buf = ByteBuffer.allocate(arraySize * 4);
          FloatBuffer floatbuf = buf.asFloatBuffer();
          float[] floatArray = new float[arraySize];
          for (int i = arrayStart; i < arrayStart + arraySize; i++) {
            floatArray[i - arrayStart] = Float.intBitsToFloat(this.getHeap().heap[i]);
          }
          floatbuf.put(floatArray);
          ByteArrayInputStream bis = new ByteArrayInputStream(buf.array());
          return bis;
        } else if (type.getName().equals(CAS.TYPE_NAME_BOOLEAN_ARRAY)
            || type.getName().equals(CAS.TYPE_NAME_BYTE_ARRAY)) {
          arrayStart = this.getHeap().heap[getArrayStartAddress(fs.getAddress())];
          buf = ByteBuffer.allocate(arraySize);
          buf.put(this.getByteHeap().heap, arrayStart, arraySize);
          ByteArrayInputStream bis = new ByteArrayInputStream(buf.array());
          return bis;
        } else if (type.getName().equals(CAS.TYPE_NAME_SHORT_ARRAY)) {
          arrayStart = this.getHeap().heap[getArrayStartAddress(fs.getAddress())];
          buf = ByteBuffer.allocate(arraySize * 2);
          ShortBuffer shortbuf = buf.asShortBuffer();
          shortbuf.put(this.getShortHeap().heap, arrayStart, arraySize);

          ByteArrayInputStream bis = new ByteArrayInputStream(buf.array());
          return bis;
        } else if (type.getName().equals(CAS.TYPE_NAME_LONG_ARRAY)) {
          arrayStart = this.getHeap().heap[getArrayStartAddress(fs.getAddress())];
          buf = ByteBuffer.allocate(arraySize * 8);
          LongBuffer longbuf = buf.asLongBuffer();
          longbuf.put(this.getLongHeap().heap, arrayStart, arraySize);
          ByteArrayInputStream bis = new ByteArrayInputStream(buf.array());
          return bis;
        } else if (type.getName().equals(CAS.TYPE_NAME_DOUBLE_ARRAY)) {
          arrayStart = this.getHeap().heap[getArrayStartAddress(fs.getAddress())];
          buf = ByteBuffer.allocate(arraySize * 8);
          DoubleBuffer doublebuf = buf.asDoubleBuffer();
          double[] doubleArray = new double[arraySize];
          for (int i = arrayStart; i < arrayStart + arraySize; i++) {
            doubleArray[i - arrayStart] = Double.longBitsToDouble(this.getLongHeap().heap[i]);
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
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFADATASTREAM_ERROR,
          new String[] { exc.getMessage() });
      throw e;
    } catch (CASRuntimeException exc) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFADATASTREAM_ERROR,
          new String[] { exc.getMessage() });
      throw e;
    } catch (IOException exc) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFADATASTREAM_ERROR,
          new String[] { exc.getMessage() });
      throw e;
    }
    return null;
  }

  @Override
  public<T extends FeatureStructure> FSIterator<T> createFilteredIterator(FSIterator<T> it, FSMatchConstraint cons) {
    return new FilteredIterator<T>(it, cons);
  }

  public void commitTypeSystem() {
    final TypeSystemImpl ts = this.svd.casMetadata.ts;
    // For CAS pools, the type system could have already been committed
    // Skip the initFSClassReg if so, because it may have been updated to a JCas
    // version by another CAS processing in the pool
    // @see org.apache.uima.cas.impl.FSClassRegistry

    // avoid race: two instances of a CAS from a pool attempting to commit the
    // ts
    // at the same time
    synchronized (ts) {
      if (!ts.isCommitted()) {
        ts.commit();
        initFSClassRegistry();  // add generators
        FSClassRegistry fscr = getFSClassRegistry();
        // save for the case of non=jcas pipeline with a jcas pear in the middle
        // - this
        // allows subsequent downstream annotators to run without jcas
        fscr.saveGeneratorsForClassLoader(this.svd.previousJCasClassLoader, fscr
            .getBaseGenerators());
      }
    }
    setLocalFsGenerators(this.svd.casMetadata.fsClassRegistry.getBaseGenerators());
    // After the type system has been committed, we can create the
    // index repository.
    createIndexRepository();
    svd.annotFeatOffset_begin = getFeatureOffset(TypeSystemImpl.startFeatCode);
    svd.annotFeatOffset_end   = getFeatureOffset(TypeSystemImpl.endFeatCode);
  }

  // internal use, public for cross class ref
  public void setLocalFsGenerators(FSGenerator<? extends FeatureStructure>[] fsGenerators) {
    this.svd.localFsGenerators = fsGenerators;
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
    return this.svd.casMetadata.ts;
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
    if (trace) {
      System.out.println("CAS Reset in thread " + Thread.currentThread().getName() +
          " for CasId = " + getCasId() + ", new reset count = " + svd.casResets.get());
    }
    int numViews = this.getBaseSofaCount();
    // Flush indexRepository for all Sofa
    for (int view = 1; view <= numViews; view++) {
      CAS tcas = (view == 1) ? getInitialView() : getView(view);
      if (tcas != null) {
        ((CASImpl) tcas).resetView();

        // mySofaRef = -1 is a flag in initial view that sofa has not been set.
        // For the initial view, it is possible to not have a sofa - it is set
        // "lazily" upon the first need.
        // all other views always have a sofa set. The sofaRef is set to 0,
        // but will be set to the actual sofa addr in the cas when the view is
        // initialized.
        ((CASImpl) tcas).mySofaRef = (1 == view) ? -1 : 0;
      }
    }
    this.getHeap().reset(/*this.getHeap().getHeapSize() > CASImpl.resetHeapSize*/);

    resetStringTable();

    this.getByteHeap().reset();
    this.getShortHeap().reset();
    this.getLongHeap().reset();

    this.indexRepository.flush();  // for base view, other views flushed above
    this.svd.sofaNameSet = new HashSet<String>();
    this.svd.initialSofaCreated = false;
    // always an Initial View now!!!
    this.svd.viewCount = 1;

    if (null != this.svd.casMetadata.fsClassRegistry) {
      // needed only if caching non-JCas Java cover objects
      // NOTE: This code doesn't work - impl commented outhas not been maintained
      this.svd.casMetadata.fsClassRegistry.flush();
    }
    if (this.jcas != null) {
      JCasImpl.clearData(this);
    }
    clearTrackingMarks();
    this.svd.cache_not_in_index = 0;
    this.svd.fssTobeAddedback.clear();
    this.svd.fssTobeAddedback.trimToSize();
    
    this.svd.traceFSid = 0;
    if (traceFSs) { 
      this.svd.traceFScreationSb.setLength(0);
    }
    this.svd.componentInfo = null; // https://issues.apache.org/jira/browse/UIMA-5097
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

  void addSofaFsToIndex(SofaFS sofa) {
    this.svd.baseCAS.getBaseIndexRepository().addFS(sofa);
  }

  void registerView(SofaFS aSofa) {
    this.mySofaRef = ((FeatureStructureImpl) aSofa).getAddress();
  }

  public void reinit(CASSerializer ser) {
    if (this != this.svd.baseCAS) {
      this.svd.baseCAS.reinit(ser);
      return;
    }
    this.resetNoQuestions();
    reinit(ser.getHeapMetadata(), ser.getHeapArray(), ser.getStringTable(), ser.getFSIndex(), ser
        .getByteArray(), ser.getShortArray(), ser.getLongArray());
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

  void resetStringTable() {
    this.getStringHeap().reset();
  }

  // public void setFSClassRegistry(FSClassRegistry fsClassReg) {
  // this.svd.casMetadata.fsClassRegistry = fsClassReg;
  // }

  private void initFSClassRegistry() {
    final TypeSystemImpl ts = this.svd.casMetadata.ts;
    // System.out.println("Initializing FSClassRegistry");
    this.svd.casMetadata.fsClassRegistry.initGeneratorArray();
    this.svd.casMetadata.fsClassRegistry.addClassForType(ts.fsArrayType, new ArrayFSGenerator());
    this.svd.casMetadata.fsClassRegistry.addClassForType(ts.intArrayType, IntArrayFSImpl
        .generator());
    this.svd.casMetadata.fsClassRegistry.addClassForType(ts.floatArrayType, FloatArrayFSImpl
        .generator());
    this.svd.casMetadata.fsClassRegistry.addClassForType(ts.stringArrayType, StringArrayFSImpl
        .generator());
    this.svd.casMetadata.fsClassRegistry.addClassForType(ts.sofaType, SofaFSImpl
        .getSofaFSGenerator());
    this.svd.casMetadata.fsClassRegistry.addClassForType(ts.annotBaseType, AnnotationBaseImpl
        .getAnnotationGenerator());
    this.svd.casMetadata.fsClassRegistry.addClassForType(ts.annotType, AnnotationImpl
        .getAnnotationGenerator());
    this.svd.casMetadata.fsClassRegistry.addClassForType(ts.byteArrayType, ByteArrayFSImpl
        .generator());
    this.svd.casMetadata.fsClassRegistry.addClassForType(ts.booleanArrayType, BooleanArrayFSImpl
        .generator());
    this.svd.casMetadata.fsClassRegistry.addClassForType(ts.shortArrayType, ShortArrayFSImpl
        .generator());
    this.svd.casMetadata.fsClassRegistry.addClassForType(ts.longArrayType, LongArrayFSImpl
        .generator());
    this.svd.casMetadata.fsClassRegistry.addClassForType(ts.doubleArrayType, DoubleArrayFSImpl
        .generator());

    // assert(fsClassReg != null);
  }

  // JCasGen'd cover classes use this to add their generators to the class
  // registry
  // Note that this now (June 2007) a no-op for JCasGen'd generators
  // Also used in JCas initialization to copy-down super generators to subtypes
  // as needed
  public FSClassRegistry getFSClassRegistry() // for JCas integration
  {
    return this.svd.casMetadata.fsClassRegistry;
  }

  public CASImpl setupCasFromCasMgrSerializer(CASMgrSerializer casMgrSerializer) {
    CASImpl cas = this.getBaseCAS();

    if (null != casMgrSerializer) {
  
      TypeSystemImpl ts = casMgrSerializer.getTypeSystem();
      cas.svd.casMetadata = ts.casMetadata;
      cas.commitTypeSystem();
  
      // reset index repositories -- wipes out Sofa index
      cas.indexRepository = casMgrSerializer.getIndexRepository(cas);
      cas.indexRepository.commit();
  
      // get handle to existing initial View
      CAS initialView = cas.getInitialView();
  
      // throw away all other View information as the CAS definition may have
      // changed
      cas.svd.sofa2indexMap.clear();
      cas.svd.sofaNbr2ViewMap.clear();
      cas.svd.viewCount = 0;
  
      // freshen the initial view
      ((CASImpl) initialView).refreshView(cas.svd.baseCAS, null);
      setViewForSofaNbr(1, initialView);
      cas.svd.viewCount = 1;
    }
    return cas;
  }
  
  public void reinit(CASCompleteSerializer casCompSer) {
    if (this != this.svd.baseCAS) {
      this.svd.baseCAS.reinit(casCompSer);
      return;
    }
    setupCasFromCasMgrSerializer(casCompSer.getCASMgrSerializer());

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
    // this.sofa2jcasMap.clear();
    
    clearTrackingMarks();
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
    this.svd.modifiedPreexistingFSs = null;
    this.svd.modifiedFSHeapCells = null;
    this.svd.modifiedByteHeapCells = null;
    this.svd.modifiedShortHeapCells = null;
    this.svd.modifiedLongHeapCells = null;
    this.svd.trackingMarkList = null;     
  }

  void reinit(int[] heapMetadata, int[] heapArray, String[] stringTable, int[] fsIndex,
      byte[] byteHeapArray, short[] shortHeapArray, long[] longHeapArray) {
    createStringTableFromArray(stringTable);
    this.getHeap().reinit(heapMetadata, heapArray);
    if (byteHeapArray != null) {
      this.getByteHeap().reinit(byteHeapArray);
    }
    if (shortHeapArray != null) {
      this.getShortHeap().reinit(shortHeapArray);
    }
    if (longHeapArray != null) {
      this.getLongHeap().reinit(longHeapArray);
    }

    reinitIndexedFSs(fsIndex);
  }
  
  
  /**
   * Binary Deserializaion Support
   * An instance of this class is made for every reinit operation 
   *
   */
  private class BinDeserSupport {
    
    private int fsStartAddr;
    private int fsEndAddr;
    /**
     * An array of all the starting indexes of the FSs on the old/prev heap
     * (below the mark, for delta CAS, plus one last one (one beyond the end)
     */
    private int[] fssAddrArray;
    private int fssIndex;
    private int lastRemovedFsAddr;
    // feature codes - there are exactly the same number as their are features
    private int[] featCodes;
    private FSsTobeAddedback tobeAddedback = FSsTobeAddedback.createSingle();
  }
    
  /**
   * --------------------------------------------------------------------- 
   * see Blob Format in CASSerializer
   * 
   * This reads in and deserializes CAS data from a stream. Byte swapping may be
   * needed if the blob is from C++ -- C++ blob serialization writes data in
   * native byte order.
   * 
   * @param istream -
   * @return -
   * @throws CASRuntimeException wraps IOException
   */
  public SerialFormat reinit(InputStream istream) throws CASRuntimeException {
    if (this != this.svd.baseCAS) {
      return this.svd.baseCAS.reinit(istream);
    }
   
    final DataInputStream dis = CommonSerDes.maybeWrapToDataInputStream(istream);

    try {
      Header h = CommonSerDes.readHeader(dis);
      return reinit(h, istream, null, CasLoadMode.DEFAULT, null, AllowPreexistingFS.allow, null);
    } catch (IOException e) {
      String msg = e.getMessage();
      if (msg == null) {
        msg = e.toString();
      }
      CASRuntimeException exception = new CASRuntimeException(
          CASRuntimeException.BLOB_DESERIALIZATION, new String[] { msg });
      throw exception;
    }
  }
  
  /**
   * --------------------------------------------------------------------- 
   * Deserialize a binary input stream, after reading the header, 
   * and optionally an externally provided type system and index spec 
   * used in compressed form 6 serialization previously
   * 
   * This reads in and deserializes CAS data from a stream. Byte swapping may be
   * needed if the blob is from C++ -- C++ blob serialization writes data in
   * native byte order.
   * 
   * The corresponding serialization code is in org.apache.uima.cas.impl.Serialization,
   * also see CasIOUtils
   * 
   * @param h -
   * @param istream -
   * @param casMgrSerializer null or the Java object representing the externally supplied type 
   *                         and maybe indexes definition (TSI)
   * @param casLoadMode DEFAULT or REINIT. REINIT required with compressed form 6 to
   *                          reinitialize the cas's type system and index definition, for form 6.  
   * @param f6 only used for form 6 where an instance of BinaryCasSerDes6 has been initialized
   * @param allowPreexistingFS only used for form 6 delta deserialization
   * @param ts -    
   * @return -
   * @throws CASRuntimeException wraps IOException
   */
  public SerialFormat reinit(Header h, 
                             InputStream istream, 
                             CASMgrSerializer casMgrSerializer,
                             CasLoadMode casLoadMode,
                             BinaryCasSerDes6 f6,
                             AllowPreexistingFS allowPreexistingFS,
                             TypeSystemImpl ts) throws CASRuntimeException {
    if (this != this.svd.baseCAS) {
      return this.svd.baseCAS.reinit(h, istream, casMgrSerializer, casLoadMode, f6, allowPreexistingFS, ts);
    }
   
    final DataInputStream dis = CommonSerDes.maybeWrapToDataInputStream(istream);

    final BinDeserSupport bds = new BinDeserSupport();
    
    CASMgrSerializer embeddedCasMgrSerializer = maybeReadEmbeddedTSI(h, dis);
    
    if (!h.isForm6() || casLoadMode == CasLoadMode.REINIT)  {
      setupCasFromCasMgrSerializer(
          (null != embeddedCasMgrSerializer && embeddedCasMgrSerializer.hasIndexRepository()) 
            ? embeddedCasMgrSerializer
            : casMgrSerializer);
    }
      
    if (!h.isForm6() && casLoadMode == CasLoadMode.LENIENT) {
      /**Lenient deserialization not support for input of type {0}.*/
      throw new CASRuntimeException(CASRuntimeException.LENIENT_NOT_SUPPORTED, new Object[] {h.toString()});
    }
    
    try {
      final boolean delta = h.isDelta;
      
      if (!delta) {
        this.resetNoQuestions();
      }

      if (h.form4) {
        (new BinaryCasSerDes4(this.getTypeSystemImpl(), false)).deserialize(this, dis, delta);
        return h.typeSystemIndexDefIncluded ? SerialFormat.COMPRESSED_TSI : SerialFormat.COMPRESSED;
      }
      
      if (h.form6) {
        CASMgrSerializer cms = (embeddedCasMgrSerializer != null) ? embeddedCasMgrSerializer : casMgrSerializer; 
        TypeSystemImpl tsRead = (cms != null) ? cms.getTypeSystem() : null;
        if (null != tsRead) {
          tsRead.commit();  // no generators set up
        }
          
        
        TypeSystemImpl ts_for_decoding =
            (tsRead != null && embeddedCasMgrSerializer != null) 
              ? tsRead                      // first choice: embedded - it's always correct
              : (ts != null)                // 2nd choice is passed in ts arg, either ts or f6.getTgtTs() 
                  ? ts
                  : (f6 != null && f6.getTgtTs() != null)
                      ? f6.getTgtTs()       // this is the ts passed in via BinaryCasSerDes6 constructor
                      : tsRead;             // last choice: the ts read from 2nd input to load() in CasIOUtils
            
            
//            (f6 != null)                    
//              ? ((f6.getTgtTs() != null)    // first choice: embedded, because it's always correct 
//                   ? f6.getTgtTs()
//                   : (ts != null)           // 2nd choice: ts passed in
//                       ? ts
//                       : tsRead)            // 3rd choice: tsRead
//              : (ts != null)            // no embedded
//                  ? ts
//                  : tsRead;
                   
//        if (f6 != null) {
//          ts_for_decoding = f6.getTgtTs(); // use embedded one if available
//          if (null == ts_for_decoding) {
//            ts_for_decoding = ts;          // use argument ts
//          }
//          if (null == ts_for_decoding) {
//            ts_for_decoding = tsRead;
//          }
//        } else {
//          ts_for_decoding = (ts != null) 
//                              ? ts
//                              : tsRead;
//        }
          
//        TypeSystemImpl ts_for_decoding = (ts != null)
//                                           ? ts 
//                                           : (f6 == null) 
//                                               ? tsRead
//                                               : (f6.getTgtTs() == null)
//                                                   ? tsRead
//                                                   : f6.getTgtTs(); 
                                                   
        try {
          BinaryCasSerDes6 bcsd = (f6 != null) 
                                    ? new BinaryCasSerDes6(f6, ts_for_decoding)
                                    : new BinaryCasSerDes6(this, ts_for_decoding);          
         
          bcsd.deserializeAfterVersion(dis, delta, AllowPreexistingFS.allow);
          return h.typeSystemIndexDefIncluded 
                   ? SerialFormat.COMPRESSED_FILTERED_TSI
                   : h.typeSystemIncluded 
                       ? SerialFormat.COMPRESSED_FILTERED_TS
                       : SerialFormat.COMPRESSED_FILTERED;
        } catch (ResourceInitializationException e) {
          throw new CASRuntimeException(CASRuntimeException.DESERIALIZING_COMPRESSED_BINARY_UNSUPPORTED, null, e);
        }
      }      
            
      final Reading r = h.reading;
      
      // main fsheap
      final int fsheapsz = r.readInt();
      
      int startPos = 0;
      if (!delta) {
        this.getHeap().reinitSizeOnly(fsheapsz);
      } else {
    	startPos = this.getHeap().getNextId();
    	this.getHeap().grow(fsheapsz);
      }
            
      // add new heap slots
      for (int i = startPos; i < fsheapsz+startPos; i++) {
        this.getHeap().heap[i] = r.readInt();
      }
      
      // string heap
      int stringheapsz = r.readInt();

      final StringHeapDeserializationHelper shdh = new StringHeapDeserializationHelper();
      
      shdh.charHeap = new char[stringheapsz];
      for (int i = 0; i < stringheapsz; i++) {
        shdh.charHeap[i] = (char) r.readShort();
      }
      shdh.charHeapPos = stringheapsz;

      // word alignment
      if (stringheapsz % 2 != 0) {
        dis.readChar();
      }

      // string ref heap
      int refheapsz = r.readInt();

      refheapsz--;
      refheapsz = refheapsz / 2;
      refheapsz = refheapsz * 3;

      // read back into references consisting to three ints
      // --stringheap offset,length, stringlist offset
      shdh.refHeap = new int[StringHeapDeserializationHelper.FIRST_CELL_REF + refheapsz];

      dis.readInt(); // 0
      for (int i = shdh.refHeapPos; i < shdh.refHeap.length; i += StringHeapDeserializationHelper.REF_HEAP_CELL_SIZE) {
        shdh.refHeap[i + StringHeapDeserializationHelper.CHAR_HEAP_POINTER_OFFSET] = r.readInt();
        shdh.refHeap[i + StringHeapDeserializationHelper.CHAR_HEAP_STRLEN_OFFSET] = r.readInt();
        shdh.refHeap[i + StringHeapDeserializationHelper.STRING_LIST_ADDR_OFFSET] = 0;
      }
      shdh.refHeapPos = refheapsz + StringHeapDeserializationHelper.FIRST_CELL_REF;
      
      this.getStringHeap().reinit(shdh, delta);
      
      //if delta, handle modified fs heap cells
      if (delta) {
        
        final int heapsize = this.getHeap().getNextId();
        
        // compute table of ints which correspond to FSs in the existing heap
        // we need this because the list of modifications is just arbitrary single words on the heap
        //   at arbitrary boundaries
        IntVector fss = new IntVector(Math.max(128, heapsize >> 6));
        int fsAddr;
        for (fsAddr = 1; fsAddr < heapsize; fsAddr = getNextFsHeapAddr(fsAddr)) {          
          fss.add(fsAddr);
        }
        fss.add(fsAddr);  // add trailing value
        bds.fssAddrArray = fss.toArray();
        
        int fsmodssz = r.readInt();
        bds.fsStartAddr = -1;        
        
        // loop over all heap modifications to existing FSs
        
        // first disable auto addbacks for index corruption - this routine is handling that
        svd.fsTobeAddedbackSingleInUse = true;  // sorry, a bad hack...
//        System.out.format("debug checkout (fake) add back single for cas %s%n", this.getViewName());
        try {
          for (int i = 0; i < fsmodssz; i++) {
            final int heapAddrBeingModified = r.readInt();
            maybeAddBackAndRemoveFs(heapAddrBeingModified, bds);       
            this.getHeap().heap[heapAddrBeingModified] = r.readInt();
          }
          bds.tobeAddedback.addback(bds.lastRemovedFsAddr);
          bds.fssAddrArray = null;  // free storage
        } finally {
//          System.out.format("debug return (fake) add back single for cas %s%n", this.getViewName());
          svd.fsTobeAddedbackSingleInUse = false;
        }
      }

      // indexed FSs
      int fsindexsz = r.readInt();
      int[] fsindexes = new int[fsindexsz];
      for (int i = 0; i < fsindexsz; i++) {
        fsindexes[i] = r.readInt();
      }

      // build the index
      if (delta) {
    	reinitDeltaIndexedFSs(fsindexes);  
      } else {
        reinitIndexedFSs(fsindexes);
      }
      
      // byte heap
      int heapsz = r.readInt();

      if (!delta) {
        this.getByteHeap().heap = new byte[Math.max(16, heapsz)]; // must be > 0
        dis.readFully(this.getByteHeap().heap, 0, heapsz);
        this.getByteHeap().heapPos = heapsz;
      }  else {
        for (int i=0; i < heapsz; i++) {
      	  this.getByteHeap().addByte(dis.readByte());
        }
      }
      // word alignment
      int align = (4 - (heapsz % 4)) % 4;
      BinaryCasSerDes6.skipBytes(dis, align);

      // short heap
      heapsz = r.readInt();
      
      if (!delta) {
        this.getShortHeap().heap = new short[Math.max(16, heapsz)]; // must be > 0
        for (int i = 0; i < heapsz; i++) {
          this.getShortHeap().heap[i] = r.readShort();
        }
        this.getShortHeap().heapPos = heapsz;
      } else {
      	for (int i = 0; i < heapsz; i++) {
      	  this.getShortHeap().addShort(r.readShort());
        }
      }
      // word alignment
      if (heapsz % 2 != 0) {
        dis.readShort();
      }

      // long heap
      heapsz = r.readInt();
      
      if (!delta) {
        this.getLongHeap().heap = new long[Math.max(16, heapsz)]; // must be > 0
        for (int i = 0; i < heapsz; i++) {
          this.getLongHeap().heap[i] = r.readLong();
        }
        this.getLongHeap().heapPos = heapsz;
      } else {
      	for (int i = 0; i < heapsz; i++) {
      	  this.getLongHeap().addLong(r.readLong());
        }
      }
      
      if (delta)  {
          //modified Byte Heap
        heapsz = r.readInt();
      	if (heapsz > 0) {
      	  int[] heapAddrs = new int[heapsz];
      	  for (int i = 0; i < heapsz; i++) {
      	    heapAddrs[i] = r.readInt();
      	  }
      	  for (int i = 0; i < heapsz; i++) {
      	    this.getByteHeap().heap[heapAddrs[i]] = dis.readByte();
      	  }
      	}
      	// word alignment
        align = (4 - (heapsz % 4)) % 4;
        BinaryCasSerDes6.skipBytes(dis, align);
        
        //modified Short Heap
        heapsz = r.readInt();
        if (heapsz > 0) {
          int[] heapAddrs = new int[heapsz];
      	  for (int i = 0; i < heapsz; i++) {
            heapAddrs[i] = r.readInt();
          }
          for (int i = 0; i < heapsz; i++) {
            this.getShortHeap().heap[heapAddrs[i]] = r.readShort();
       	  }
      	}
      	
        // word alignment
        if (heapsz % 2 != 0) {
          dis.readShort();
        }
      
        //modified Long Heap
        heapsz = r.readInt();
        if (heapsz > 0) {
          int[] heapAddrs = new int[heapsz];
          for (int i = 0; i < heapsz; i++) {
            heapAddrs[i] = r.readInt();
          }
          for (int i = 0; i < heapsz; i++) {
            this.getLongHeap().heap[heapAddrs[i]] = r.readLong();
          }
        }
      } // of delta - modified processing
    } catch (IOException e) {
      String msg = e.getMessage();
      if (msg == null) {
        msg = e.toString();
      }
      CASRuntimeException exception = new CASRuntimeException(
          CASRuntimeException.BLOB_DESERIALIZATION, new String[] { msg });
      throw exception;
    }
    return h.typeSystemIndexDefIncluded ? SerialFormat.BINARY_TSI : SerialFormat.BINARY;
  }
    
  
  static CASMgrSerializer maybeReadEmbeddedTSI(Header h, DataInputStream dis) {  
    if (h.isTypeSystemIncluded() || h.isTypeSystemIndexDefIncluded()) { // Load TS from CAS stream
      try {
        ObjectInputStream ois = new ObjectInputStream(dis);
        return (CASMgrSerializer) ois.readObject();
      } catch (ClassNotFoundException | IOException e) {
        /**Unrecognized serialized CAS format*/
        throw new CASRuntimeException(CASRuntimeException.UNRECOGNIZED_SERIALIZED_CAS_FORMAT, null, e);
      }
    }
    return null;
  }
  
  /**
   * for Deserialization of Delta, when updating existing FSs,
   * If the heap addr is for the next FS, re-add the previous one to those indexes where it was removed,
   * and then maybe remove the new one (and remember which views to re-add to).
   * @param heapAddr
   */
  private void maybeAddBackAndRemoveFs(int heapAddr, final BinDeserSupport bds) {
    if (bds.fsStartAddr == -1) {
      bds.fssIndex = -1;
      bds.lastRemovedFsAddr = -1;
      bds.tobeAddedback.clear();
    }
    findCorrespondingFs(heapAddr, bds); // sets fsStartAddr, end addr
    if (bds.lastRemovedFsAddr != bds.fsStartAddr) {
      bds.tobeAddedback.addback(bds.lastRemovedFsAddr);
      if (bds.featCodes.length == 0) {
        // is array
        final int typeCode = getTypeCode(bds.fsStartAddr);
        assert(getTypeSystemImpl().ll_isArrayType(typeCode));
      } else {
        int featCode = bds.featCodes[heapAddr - (bds.fsStartAddr + 1)];
        removeFromCorruptableIndexAnyView(bds.lastRemovedFsAddr = bds.fsStartAddr, bds.tobeAddedback, featCode);
      }
    }
  }
    
  private void findCorrespondingFs(int heapAddr, final BinDeserSupport bds) {
    if (bds.fsStartAddr < heapAddr && heapAddr < bds.fsEndAddr) {
      return;
    }
    
    // search forward by 1 before doing binary search
    bds.fssIndex ++;  // incrementing dense index into fssAddrArray for start addrs
    bds.fsStartAddr = bds.fssAddrArray[bds.fssIndex];  // must exist
    if (bds.fssIndex + 1 < bds.fssAddrArray.length) { // handle edge case where prev was at the end
      bds.fsEndAddr = bds.fssAddrArray[bds.fssIndex + 1];  // must exist
      if (bds.fsStartAddr < heapAddr && heapAddr < bds.fsEndAddr) {
        bds.featCodes = getTypeSystemImpl().ll_getAppropriateFeatures(getTypeCode(bds.fsStartAddr));
        return;
      }
    }
    
    int result;
    if (heapAddr > bds.fsEndAddr) {
      // item is higher
      result = Arrays.binarySearch(bds.fssAddrArray, bds.fssIndex + 1, bds.fssAddrArray.length, heapAddr);
    } else {
      result = Arrays.binarySearch(bds.fssAddrArray,  0, bds.fssIndex - 1, heapAddr);
    }
    
    // result must be negative - should never modify a type code slot
    assert (result < 0);
    bds.fssIndex = (-result) - 2;
    bds.fsStartAddr = bds.fssAddrArray[bds.fssIndex];
    bds.fsEndAddr = bds.fssAddrArray[bds.fssIndex + 1];
    bds.featCodes = getTypeSystemImpl().ll_getAppropriateFeatures(getTypeCode(bds.fsStartAddr));  
    assert(bds.fsStartAddr < heapAddr && heapAddr < bds.fsEndAddr);
  }
  
  private int getNextFsHeapAddr(int fsAddr) {
    final TypeSystemImpl tsi = getTypeSystemImpl();
    final int typeCode = getTypeCode(fsAddr);
    final Type type = tsi.ll_getTypeForCode(typeCode);
    //debug
//    if (tsi.ll_getTypeForCode(typeCode) == null) {
//      System.out.println("debug, typeCode = "+ typeCode);
//    }
    final boolean isHeapStoredArray = (typeCode == TypeSystemImpl.intArrayTypeCode) || (typeCode == TypeSystemImpl.floatArrayTypeCode)
        || (typeCode == TypeSystemImpl.fsArrayTypeCode) || (typeCode == TypeSystemImpl.stringArrayTypeCode)
        || (TypeSystemImpl.isArrayTypeNameButNotBuiltIn(type.getName()));
    if (isHeapStoredArray) {
      return fsAddr + 2 + getHeapValue(fsAddr + 1);
    } else if (type.isArray()) {
      return fsAddr + 3;  // for the aux ref and the length
    } else {
      return fsAddr + this.svd.casMetadata.fsSpaceReq[typeCode];
    }    
  }
  
//  private long swap8(DataInputStream dis, byte[] buf) throws IOException {
//
//    buf[7] = dis.readByte();
//    buf[6] = dis.readByte();
//    buf[5] = dis.readByte();
//    buf[4] = dis.readByte();
//    buf[3] = dis.readByte();
//    buf[2] = dis.readByte();
//    buf[1] = dis.readByte();
//    buf[0] = dis.readByte();
//    ByteBuffer bb = ByteBuffer.wrap(buf);
//    return bb.getLong();
//  }
//
//  private int swap4(DataInputStream dis, byte[] buf) throws IOException {
//    buf[3] = dis.readByte();
//    buf[2] = dis.readByte();
//    buf[1] = dis.readByte();
//    buf[0] = dis.readByte();
//    ByteBuffer bb = ByteBuffer.wrap(buf);
//    return bb.getInt();
//  }
//
//  private char swap2(DataInputStream dis, byte[] buf) throws IOException {
//    buf[1] = dis.readByte();
//    buf[0] = dis.readByte();
//    ByteBuffer bb = ByteBuffer.wrap(buf, 0, 2);
//    return bb.getChar();
//  }

  // assumes:
  // indexes are empty on entry
  //   
  void reinitIndexedFSs(int[] fsIndex) {
    // Add FSs to index repository for base CAS
    int numViews = fsIndex[0];
    int loopLen = fsIndex[1]; // number of sofas, not necessarily the same as
    // number of views
    // because the initial view may not have a sofa
    for (int i = 2; i < loopLen + 2; i++) { // iterate over all the sofas,
      this.indexRepository.addFS(fsIndex[i]); // add to base index
    }
    int loopStart = loopLen + 2;

    FSIterator<SofaFS> iterator = this.svd.baseCAS.getSofaIterator();
    final Feature idFeat = getTypeSystem().getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFAID);
    // Add FSs to index repository for each View
    while (iterator.isValid()) {
      SofaFS sofa = iterator.get();
      String id = ll_getStringValue(((FeatureStructureImpl) sofa).getAddress(),
          ((FeatureImpl) idFeat).getCode());
      if (CAS.NAME_DEFAULT_SOFA.equals(id)) {
        this.registerInitialSofa();
        this.svd.sofaNameSet.add(id);
      }
      // next line the getView as a side effect
      // checks for dupl sofa name, and if not,
      // adds the name to the sofaNameSet
      ((CASImpl) this.getView(sofa)).registerView(sofa);

      iterator.moveToNext();
    }
    getInitialView();  // done for side effect of creating the initial view.
    // must be done before the next line, because it sets the
    // viewCount to 1.
    this.svd.viewCount = numViews; // total number of views
    
    for (int viewNbr = 1; viewNbr <= numViews; viewNbr++) {
      CAS view = (viewNbr == 1) ? getInitialView() : getView(viewNbr);
      if (view != null) {
        FSIndexRepositoryImpl loopIndexRep = (FSIndexRepositoryImpl) getSofaIndexRepository(viewNbr);
        loopLen = fsIndex[loopStart];
        for (int i = loopStart + 1; i < loopStart + 1 + loopLen; i++) {
          loopIndexRep.addFS(fsIndex[i]);
        }
        loopStart += loopLen + 1;
          ((CASImpl) view).updateDocumentAnnotation();
      } else {
        loopStart += 1;
      }
    }
  }
  
  /**
   * Adds the SofaFSs to the base view
   * Assumes "cas" refers to the base cas
   *
   * Processes "adds", "removes" and "reindexes" for all views
   *
   * @param fsIndex - array of fsRefs and counts, for sofas, and all views
   */
  void reinitDeltaIndexedFSs(int[] fsIndex) {
    assert(this.svd.baseCAS == this);  
    // Add Sofa FSs to index repository for base CAS
    int numViews = fsIndex[0]; // total number of views
    int loopLen = fsIndex[1]; // number of sofas, not necessarily the same as number of views (initial view could be missing a Sofa)
    // add Sofa FSs to base view number of views. Should only contain new Sofas.
    for (int i = 2; i < loopLen + 2; i++) { // iterate over all the sofas,
      this.indexRepository.addFS(fsIndex[i]); // add to base index
    }
    int loopStart = loopLen + 2;

    FSIterator<SofaFS> iterator = this.getSofaIterator();
    final int idFeatCode = ((FeatureImpl)getTypeSystem().getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFAID)).getCode();
    
    // Register all Sofas
    while (iterator.isValid()) {
      SofaFS sofa = iterator.get();
      String id = ll_getStringValue(((FeatureStructureImpl) sofa).getAddress(), idFeatCode);
      if (CAS.NAME_DEFAULT_SOFA.equals(id)) {
        this.registerInitialSofa();
        this.svd.sofaNameSet.add(id);
      }
      // next line the getView as a side effect
      // checks for dupl sofa name, and if not,
      // adds the name to the sofaNameSet
      ((CASImpl) this.getView(sofa)).registerView(sofa);

      iterator.moveToNext();
    }
    
    this.svd.viewCount = numViews; // total number of views

    for (int viewNbr = 1; viewNbr <= numViews; viewNbr++) {
      CAS view = (viewNbr == 1) ? getInitialView() : getView(viewNbr);
      if (view != null) {
        
        // for all views
        
        FSIndexRepositoryImpl loopIndexRep = (FSIndexRepositoryImpl) getSofaIndexRepository(viewNbr);
        loopLen = fsIndex[loopStart];
        
        // add FSs to index
        
        for (int i = loopStart + 1; i < loopStart + 1 + loopLen; i++) {
          loopIndexRep.addFS(fsIndex[i]);
        }
        
        // remove FSs from indexes
        
        loopStart += loopLen + 1;
        loopLen = fsIndex[loopStart];
        for (int i = loopStart + 1; i < loopStart + 1 + loopLen; i++) {
          loopIndexRep.removeFS(fsIndex[i]);
        }
        
        // skip the reindex - this isn't done here https://issues.apache.org/jira/browse/UIMA-4100
        // but we need to run the loop to read over the items in the input stream
        loopStart += loopLen + 1;
        loopLen = fsIndex[loopStart];
//        for (int i = loopStart + 1; i < loopStart + 1 + loopLen; i++) {
//          loopIndexRep.removeFS(fsIndex[i]);
//          loopIndexRep.addFS(fsIndex[i]);
//        }
        loopStart += loopLen + 1;
        ((CASImpl) view).updateDocumentAnnotation();
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

    int numViews = getBaseSofaCount();
    v.add(numViews);

    // Get indexes for base CAS
    fsLoopIndex = this.svd.baseCAS.indexRepository.getIndexedFSs();
    v.add(fsLoopIndex.length);
    v.add(fsLoopIndex, 0, fsLoopIndex.length);
//    for (int k = 0; k < fsLoopIndex.length; k++) {
//      v.add(fsLoopIndex[k]);
//    }

    // Get indexes for each SofaFS in the CAS
    for (int sofaNum = 1; sofaNum <= numViews; sofaNum++) {
      FSIndexRepositoryImpl loopIndexRep = (FSIndexRepositoryImpl) this.svd.baseCAS
          .getSofaIndexRepository(sofaNum);
      if (loopIndexRep != null) {
        fsLoopIndex = loopIndexRep.getIndexedFSs();
      } else {
        fsLoopIndex = INT0;
      }
      v.add(fsLoopIndex.length);
      for (int k = 0; k < fsLoopIndex.length; k++) {
        v.add(fsLoopIndex[k]);
      }
    }
    return v.toArray();
  }
  
 
  
  //Delta IndexedFSs format:
  // number of views
  // number of sofas - new
  // [sofa-1 ... sofa-n]
  // number of new FS add in View1
  // [FS-1 ... FS-n]
  // number of  FS removed from View1
  // [FS-1 ... FS-n]
  //number of  FS reindexed in View1
  // [FS-1 ... FS-n]
  // etc.
  int[] getDeltaIndexedFSs(MarkerImpl mark) {
    IntVector v = new IntVector();
    int[] fsLoopIndex;
    int[] fsDeletedFromIndex;
    int[] fsReindexed;

    int numViews = getBaseSofaCount();
    v.add(numViews);

    // Get indexes for base CAS
    fsLoopIndex = this.svd.baseCAS.indexRepository.getIndexedFSs();
    // Get the new Sofa FS
    IntVector newSofas = new IntVector();
    for (int k = 0; k < fsLoopIndex.length; k++) {
      if ( mark.isNew(fsLoopIndex[k]) ) {
        newSofas.add(fsLoopIndex[k]);
      }
    }
    
    v.add(newSofas.size());
    v.add(newSofas.getArray(), 0, newSofas.size());
//    for (int k = 0; k < newSofas.size(); k++) {
//      v.add(newSofas.get(k));
//    }

    // Get indexes for each view in the CAS
    for (int sofaNum = 1; sofaNum <= numViews; sofaNum++) {
      FSIndexRepositoryImpl loopIndexRep = (FSIndexRepositoryImpl) this.svd.baseCAS
          .getSofaIndexRepository(sofaNum);
      if (loopIndexRep != null) {
        fsLoopIndex = loopIndexRep.getAddedFSs();
        fsDeletedFromIndex = loopIndexRep.getDeletedFSs();
        fsReindexed = loopIndexRep.getReindexedFSs();
      } else {
        fsLoopIndex = INT0;
        fsDeletedFromIndex = INT0;
        fsReindexed = INT0;
      }
      v.add(fsLoopIndex.length);
      v.add(fsLoopIndex, 0, fsLoopIndex.length);
//      for (int k = 0; k < fsLoopIndex.length; k++) {
//        v.add(fsLoopIndex[k]);
//      }
      v.add(fsDeletedFromIndex.length);
      v.add(fsDeletedFromIndex, 0, fsDeletedFromIndex.length);
//      for (int k = 0; k < fsDeletedFromIndex.length; k++) {
//        v.add(fsDeletedFromIndex[k]);
//      }
      v.add(fsReindexed.length);
      v.add(fsReindexed, 0, fsReindexed.length);
//      for (int k = 0; k < fsReindexed.length; k++) {
//        v.add(fsReindexed[k]);
//      }
    }
    return v.toArray();
  }

  void createStringTableFromArray(String[] stringTable) {
    // why a new heap instead of reseting the old one???
    // this.stringHeap = new StringHeap();
    this.getStringHeap().reset();
    for (int i = 1; i < stringTable.length; i++) {
      this.getStringHeap().addString(stringTable[i]);
    }
  }

  static String mapName(String name, HashMap<String, String> map) {
    String out = map.get(name);
    if (out != null) {
      return out;
    }
    return name;
  }

  /**
   * This is your link from the low-level API to the high-level API. Use this
   * method to create a FeatureStructure object from an address. Not that the
   * reverse is not supported by public APIs (i.e., there is currently no way to
   * get at the address of a FeatureStructure. Maybe we will need to change
   * that.
   * 
   * @param addr
   *                The address of the feature structure to be created.
   * @param <T> The Java class associated with this feature structure
   * @return A FeatureStructure object. Note that no checking whatsoever is done
   *         on the input address. There is really no way of finding out which
   *         addresses in the valid address space actually represent feature
   *         structures, and which don't.
   */
  public <T extends FeatureStructure> T createFS(int addr) {
    return ll_getFSForRef(addr);
  }

  @Override
public int ll_getArraySize(int arrayFsRef) {
    return this.getHeap().heap[arrayFsRef + arrayLengthFeatOffset];
  }

  /**
   * Get the heap address of the first cell of this array.
   * 
   * @param addr
   *                The address of the array.
   * @return The address where the first cell of the array is located.
   */
  public final int getArrayStartAddress(int addr) {
    return addr + arrayContentOffset;
  }

  /**
   * Get a specific value out of an array.
   * 
   * @param addr
   *                The address of the array.
   * @param index
   *                The index of the value we're interested in.
   * @return The value at <code>index</code>.
   * @exception ArrayIndexOutOfBoundsException -
   */
  public int getArrayValue(int addr, int index) {
    checkArrayBounds(addr, index);
    return this.getHeap().heap[addr + arrayContentOffset + index];
  }

  /**
   * Set an array value.
   * 
   * @param addr
   *                The address of the array.
   * @param index
   *                The index we want to set.
   * @param value
   *                The value we want to set.
   * @exception ArrayIndexOutOfBoundsException
   */
  void setArrayValue(final int addr, final int index, final int value)
      throws ArrayIndexOutOfBoundsException {
    // Get the length of this array.
    final int arraySize = this.getHeap().heap[addr + arrayLengthFeatOffset];
    // Check for boundary violation.
    if ((index < 0) || (index >= arraySize)) {
      throw new ArrayIndexOutOfBoundsException();
    }
    this.getHeap().heap[addr + arrayContentOffset + index] = value;
    if (this.svd.trackingMark != null) {
    	this.logFSUpdate(addr, addr+arrayContentOffset+index, ModifiedHeap.FSHEAP, 1);
    }
  }

  void setArrayValueFromString(final int addr, final int index, final String value) {
    int arrayType = this.getHeap().heap[addr];

    if (arrayType == TypeSystemImpl.intArrayTypeCode) {
      setArrayValue(addr, index, Integer.parseInt(value));
    } else if (arrayType == TypeSystemImpl.floatArrayTypeCode) {
      setArrayValue(addr, index, CASImpl.float2int(Float.parseFloat(value)));
    } else if (arrayType == TypeSystemImpl.stringArrayTypeCode) {
      setArrayValue(addr, index, addString(value));
    } else if (arrayType == TypeSystemImpl.booleanArrayTypeCode) {
      ll_setBooleanArrayValue(addr, index, Boolean.valueOf(value).booleanValue());
    } else if (arrayType == TypeSystemImpl.byteArrayTypeCode) {
      ll_setByteArrayValue(addr, index, Byte.parseByte(value));
    } else if (arrayType == TypeSystemImpl.shortArrayTypeCode) {
      ll_setShortArrayValue(addr, index, Short.parseShort(value));
    } else if (arrayType == TypeSystemImpl.longArrayTypeCode) {
      ll_setLongArrayValue(addr, index, Long.parseLong(value));
    } else if (arrayType == TypeSystemImpl.doubleArrayTypeCode) {
      ll_setDoubleArrayValue(addr, index, Double.parseDouble(value));
    } else if (arrayType == TypeSystemImpl.fsArrayTypeCode) {
      setArrayValue(addr, index, Integer.parseInt(value));
    }
  }

  /**
   * Copy the contents of an array to an externally provided array.
   * 
   * @param addr
   *                The address of the source array.
   * @param sourceOffset
   *                The offset we want to start copying at.
   * @param dest
   *                The destination array.
   * @param destOffset
   *                An offset into the destination array.
   * @param length
   *                The number of items to copy.
   */
  void copyToArray(int addr, int sourceOffset, int[] dest, int destOffset, int length) {
    // Get the length of this array.
    final int arraySize = this.getHeap().heap[addr + arrayLengthFeatOffset];
    // Check boundary conditions for source array. We can rely on Java to
    // complain about boundary violations for the destination array.
    if ((sourceOffset < 0) || ((length + sourceOffset) > arraySize)) {
      throw new ArrayIndexOutOfBoundsException();
    }
    // Compute the offset into the heap where the array starts.
    final int offset = addr + arrayContentOffset;
    System.arraycopy(this.getHeap().heap, offset + sourceOffset, dest, destOffset, length);
  }

  /**
   * Copy the contents of an input array into a CAS array.
   * 
   * @param src
   *                The array to copy from.
   * @param srcOffset
   *                An offset into the source from where to start copying.
   * @param addr
   *                The address of the array we're copying to.
   * @param destOffset
   *                Where to start copying into the destination array.
   * @param length
   *                How many elements to copy.
   */
  void copyFromArray(int[] src, int srcOffset, int addr, int destOffset, int length) {
    // Get the length of this array.
    final int arraySize = this.getHeap().heap[addr + arrayLengthFeatOffset];
    // Check boundary conditions for destination array. We can rely on Java
    // to
    // complain about boundary violations for the source array.
    if ((destOffset < 0) || ((length + destOffset) > arraySize)) {
      throw new ArrayIndexOutOfBoundsException();
    }
    // Compute the offset into the heap where the array starts.
    final int offset = addr + arrayContentOffset;
    System.arraycopy(src, srcOffset, this.getHeap().heap, offset + destOffset, length);
    if (this.svd.trackingMark != null) {
    	this.logFSUpdate(addr, offset + destOffset, ModifiedHeap.FSHEAP, length);
    }
  }

  public int getTypeCode(int fsAddr) {
    return getHeapValue(fsAddr);
  }
  
  // not journaled, this is for a new FS only
  void copyFeatures(int trgAddr, int srcAddr) throws CASRuntimeException {
    int typeCode = getHeapValue(trgAddr);
    if (typeCode != getHeapValue(srcAddr)) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_TYPE);
      // What's that supposed to mean? Internationalized, my foot.
      // TODO: fix exception argument.
      // e.addArgument("Type of source and target feature structures do not
      // match");
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
      int val = getHeapValue(srcAddr + this.svd.casMetadata.featureOffset[featCode]);
      // if this is a string, create a new reference in the string
      // reference heap
      // and point to the same string as the string feature in src fs.
      setFeatureValueNotJournaled(trgAddr,  featCode, isStringType(rangeType) ? 
            this.getStringHeap().cloneStringReference(val)  : 
            getHeapValue(srcAddr + this.svd.casMetadata.featureOffset[featCode]));
    }
  }

  /**
   * Get the value of an address on the heap.
   * 
   * @param addr
   *                The target address.
   * @return The value at the address.
   */
  public int getHeapValue(int addr) {
    return this.getHeap().heap[addr];
  }

  /**
   * This is the common point where all sets of values in the heap come through
   * It implements the check for invalid feature setting and potentially the addback.
   * 
   * Set the value of a feature of a FS.
   * 
   * @param addr
   *                The address of the FS.
   * @param feat
   *                The code of the feature.
   * @param val
   *                The new value for the feature.
   * @exception ArrayIndexOutOfBoundsException
   *                    If the feature is not a legal feature, or it is not
   *                    appropriate for the type at the address.
   */
  public void setFeatureValue(int addr, int feat, int val) {
    boolean wasRemoved = checkForInvalidFeatureSetting(addr, feat);
    setFeatureValueNotJournaled(addr, feat, val);
    if (wasRemoved) {
      maybeAddback(addr);
    }
    if (this.svd.trackingMark != null) {
    	this.logFSUpdate(addr, addr + this.svd.casMetadata.featureOffset[feat], 
    			ModifiedHeap.FSHEAP, 1);
    }
  }
  
  /**
   * Set the value of a feature of a FS without checking for index corruption
   * (typically because the feature isn't one that can be used as a key, or
   * the context is one where the FS is being created, and is guaranteed not to be in any index (yet))
   * 
   * @param addr    The address of the FS.
   * @param feat    The code of the feature.
   * @param val     The new value for the feature.
   * @exception ArrayIndexOutOfBoundsException
   *                    If the feature is not a legal feature, or it is not
   *                    appropriate for the type at the address.
   */
  void setFeatureValueNoIndexCorruptionCheck(int addr, int feat, int val) {
    setFeatureValueNotJournaled(addr, feat, val);
    if (this.svd.trackingMark != null) {
      this.logFSUpdate(addr, addr+this.svd.casMetadata.featureOffset[feat], 
          ModifiedHeap.FSHEAP, 1);
    }    
  }
  /**
   * Set the value of a feature in the FS without journaling
   *   (because it's for a new FS above the mark)
   * @param addr    The address of the FS.
   * @param feat    The code of the feature.
   * @param val     The new value for the feature.
   * @exception ArrayIndexOutOfBoundsException
   *                    If the feature is not a legal feature, or it is not
   *                    appropriate for the type at the address.
   */
  void setFeatureValueNotJournaled(int addr, int feat, int val) {
    this.getHeap().heap[(addr + this.svd.casMetadata.featureOffset[feat])] = val;
    if (traceFSs) {
      traceFSfeat(ll_getFSForRef(addr), (FeatureImpl) getTypeSystemImpl().ll_getFeatureForCode(feat), val);
    }
  }
  
  public void setStringValue(int addr, int feat, String s) {
    final int stringCode = ((s == null) ? NULL : this.getStringHeap().addString(s));
    setFeatureValue(addr, feat, stringCode);
  }

  public void setFloatValue(int addr, int feat, float f) {
    final int floatCode = Float.floatToIntBits(f);
    setFeatureValue(addr, feat, floatCode);
  }

  // doesn't do proper index corruption checking
  // because don't have FS addr
  // never called 2014
//  public void setFloatValue(int addr, float f) {
//    final int floatCode = Float.floatToIntBits(f);
//    this.getHeap().heap[addr] = floatCode;
//  }

  public int getFeatureValue(int addr, int feat) {
    return this.getHeap().heap[(addr + this.svd.casMetadata.featureOffset[feat])];
  }

  public String getStringValue(int addr, int feat) {
    return ll_getStringValue(addr, feat);
  }

  public float getFloatValue(int addr, int feat) {
    return Float.intBitsToFloat(getFeatureValue(addr, feat));
  }

  public float getFloatValue(int addr) {
    return Float.intBitsToFloat(this.getHeap().heap[addr]);
  }

  // byte
  public void setFeatureValue(int addr, int feat, byte v) {
    setFeatureValue(addr, feat, (int) v);
  }

  public byte getByteValue(int addr, int feat) {
    return ll_getByteValue(addr, feat);
  }

  // boolean
  public void setFeatureValue(int addr, int feat, boolean v) {
    setFeatureValue(addr, feat, v ? CASImpl.TRUE : CASImpl.FALSE);
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

  public void setFeatureValue(int addr, int feat, float f) {
    this.ll_setFloatValue(addr,  feat, f);
  }
  
//  public void setFeatureValueNoIndexCorruptionCheck(int addr, int feat, float f) {
//    this.ll_setFloatValueNoIndexCorruptionCheck(addr,  feat,  f);
//  }
  
  // double
  public void setFeatureValue(int addr, int feat, double s) {
    this.ll_setDoubleValue(addr, feat, s);
  }

  public double getDoubleValue(int addr, int feat) {
    return ll_getDoubleValue(addr, feat);
  }

  public String getFeatureValueAsString(int addr, int feat) {
    final TypeSystemImpl ts = this.svd.casMetadata.ts;
    int typeCode = ts.range(feat);
    if (typeCode == TypeSystemImpl.intTypeCode) {
      return Integer.toString(this.ll_getIntValue(addr, feat));
    } else if (typeCode == TypeSystemImpl.floatTypeCode) {
      return Float.toString(this.ll_getFloatValue(addr, feat));
    } else if (ts.subsumes(TypeSystemImpl.stringTypeCode, typeCode)) {
      return this.getStringValue(addr, feat);
    } else if (typeCode == TypeSystemImpl.booleanTypeCode) {
      return Boolean.toString(this.getBooleanValue(addr, feat));
    } else if (typeCode == TypeSystemImpl.byteTypeCode) {
      return Byte.toString(this.getByteValue(addr, feat));
    } else if (typeCode == TypeSystemImpl.shortTypeCode) {
      return Short.toString(this.getShortValue(addr, feat));
    } else if (typeCode == TypeSystemImpl.longTypeCode) {
      return Long.toString(this.getLongValue(addr, feat));
    } else if (typeCode == TypeSystemImpl.doubleTypeCode) {
      return Double.toString(this.getDoubleValue(addr, feat));
    } else {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_RANGE,
          new String[] { ts.ll_getFeatureForCode(feat).getName(),
              ts.ll_getTypeForCode(typeCode).getName() });
      throw e;
    }

  }

  public void setFeatureValueFromString(int fsref, int feat, String value) {
    final TypeSystemImpl ts = this.svd.casMetadata.ts;
    int typeCode = (ts.range(feat));
    if (typeCode == TypeSystemImpl.intTypeCode) {
      this.ll_setIntValue(fsref, feat, Integer.parseInt(value));
    } else if (typeCode == TypeSystemImpl.floatTypeCode) {
      this.setFloatValue(fsref, feat, Float.parseFloat(value));
    } else if (ts.subsumes(TypeSystemImpl.stringTypeCode, typeCode)) {
      this.setStringValue(fsref, feat, value);
    } else if (typeCode == TypeSystemImpl.booleanTypeCode) {
      this.setFeatureValue(fsref, feat, Boolean.valueOf(value).booleanValue());
    } else if (typeCode == TypeSystemImpl.byteTypeCode) {
      this.setFeatureValue(fsref, feat, Byte.parseByte(value));
    } else if (typeCode == TypeSystemImpl.shortTypeCode) {
      this.setFeatureValue(fsref, feat, Short.parseShort(value));
    } else if (typeCode == TypeSystemImpl.longTypeCode) {
      this.setFeatureValue(fsref, feat, Long.parseLong(value));
    } else if (typeCode == TypeSystemImpl.doubleTypeCode) {
      this.setFeatureValue(fsref, feat, Double.parseDouble(value));
    } else {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_TYPE,
          new String[] { ts.ll_getFeatureForCode(feat).getName(),
              ts.ll_getTypeForCode(typeCode).getName() });
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
    return this.svd.casMetadata.ts;
  }

  public String getStringForCode(int stringCode) {
    return this.getStringHeap().getStringForCode(stringCode);
  }

  /**
   * Check if this is a regular type (i.e., not an array or a basic type).
   * 
   * @param typeCode
   *                The code to check.
   * @return <code>true</code> iff <code>typeCode</code> is a type for which
   *         a regular FS can be generated.
   * @exception NullPointerException
   *                    If <code>typeCode</code> is not a type code.
   */
  final boolean isCreatableType(int typeCode) {
    return this.svd.casMetadata.creatableType[typeCode];
  }

  // **** Never called
  // boolean isBuiltinType(Type type) {
  // // had to hack this because it wasn't considering List types as built in
  // // -AL
  // return (type.getName().startsWith("uima") || type == getAnnotationType());
  // /*
  // * final int typeCode = ((TypeImpl) type).getCode(); return (type ==
  // ts.getTopType()) ||
  // * isArrayType(typeCode) || isAbstractArrayType(typeCode) ||
  // isStringType(typeCode) ||
  // * isFloatType(typeCode) || isIntType(typeCode);
  // */
  // }

  int addString(String s) {
    return this.getStringHeap().addString(s);
  }

  // Type access methods.
  public boolean isStringType(Type type) {
    final TypeSystemImpl ts = this.svd.casMetadata.ts;
    return ts.subsumes(ts.stringType, type);
  }

  public boolean isAbstractArrayType(Type type) {
    final TypeSystemImpl ts = this.svd.casMetadata.ts;
    return ts.subsumes(ts.arrayBaseType, type);
  }

  public boolean isArrayType(Type type) {
    final TypeSystemImpl ts = this.svd.casMetadata.ts;
    return ((type == ts.fsArrayType) || (type == ts.intArrayType) || (type == ts.floatArrayType)
        || (type == ts.stringArrayType) || (type == ts.booleanArrayType)
        || (type == ts.byteArrayType) || (type == ts.shortArrayType)
        || (type == ts.doubleArrayType) || (type == ts.longArrayType));
  }

  public boolean isIntArrayType(Type type) {
    return (type == this.svd.casMetadata.ts.intArrayType);
  }

  public boolean isFloatArrayType(Type type) {
    return (type == this.svd.casMetadata.ts.floatArrayType);
  }

  public boolean isStringArrayType(Type type) {
    return (type == this.svd.casMetadata.ts.stringArrayType);
  }

  public boolean isBooleanArrayType(Type type) {
    return (type == this.svd.casMetadata.ts.booleanArrayType);
  }

  public boolean isByteArrayType(Type type) {
    return (type == this.svd.casMetadata.ts.byteArrayType);
  }

  public boolean isShortArrayType(Type type) {
    return (type == this.svd.casMetadata.ts.shortArrayType);
  }

  public boolean isLongArrayType(Type type) {
    return (type == this.svd.casMetadata.ts.longArrayType);
  }

  public boolean isDoubleArrayType(Type type) {
    return (type == this.svd.casMetadata.ts.doubleArrayType);
  }

  public boolean isFSArrayType(Type type) {
    return (type == this.svd.casMetadata.ts.fsArrayType);
  }

  public boolean isIntType(Type type) {
    return (type == this.svd.casMetadata.ts.intType);
  }

  public boolean isFloatType(Type type) {
    return (type == this.svd.casMetadata.ts.floatType);
  }

  public boolean isStringType(int type) {
    final TypeSystemImpl ts = this.svd.casMetadata.ts;
    return ts.subsumes(TypeSystemImpl.stringTypeCode, type);
  }

  public boolean isByteType(Type type) {
    return (type == this.svd.casMetadata.ts.byteType);
  }

  public boolean isBooleanType(Type type) {
    return (type == this.svd.casMetadata.ts.booleanType);
  }

  public boolean isShortType(Type type) {
    return (type == this.svd.casMetadata.ts.shortType);
  }

  public boolean isLongType(Type type) {
    return (type == this.svd.casMetadata.ts.longType);
  }

  public boolean isDoubleType(Type type) {
    return (type == this.svd.casMetadata.ts.doubleType);
  }

  public boolean isAbstractArrayType(int type) {
    return this.svd.casMetadata.ts.subsumes(TypeSystemImpl.arrayBaseTypeCode, type);
  }

  public boolean isArrayType(int type) {
    final TypeSystemImpl ts = this.svd.casMetadata.ts;
    return ts.ll_isArrayType(type);
  }

  public boolean isIntArrayType(int type) {
    return (type == TypeSystemImpl.intArrayTypeCode);
  }

  public boolean isFloatArrayType(int type) {
    return (type == TypeSystemImpl.floatArrayTypeCode);
  }

  public boolean isStringArrayType(int type) {
    return (type == TypeSystemImpl.stringArrayTypeCode);
  }

  public boolean isByteArrayType(int type) {
    return (type == TypeSystemImpl.byteArrayTypeCode);
  }

  public boolean isBooleanArrayType(int type) {
    return (type == TypeSystemImpl.booleanArrayTypeCode);
  }

  public boolean isShortArrayType(int type) {
    return (type == TypeSystemImpl.shortArrayTypeCode);
  }

  public boolean isLongArrayType(int type) {
    return (type == TypeSystemImpl.longArrayTypeCode);
  }

  public boolean isDoubleArrayType(int type) {
    return (type == TypeSystemImpl.doubleArrayTypeCode);
  }

  public boolean isFSArrayType(int type) {
    return (type == TypeSystemImpl.fsArrayTypeCode);
  }

  public boolean isIntType(int type) {
    return (type == TypeSystemImpl.intTypeCode);
  }

  public boolean isFloatType(int type) {
    return (type == TypeSystemImpl.floatTypeCode);
  }

  public boolean isByteType(int type) {
    return (type == TypeSystemImpl.byteTypeCode);
  }

  public boolean isBooleanType(int type) {
    return (type == TypeSystemImpl.booleanTypeCode);
  }

  public boolean isShortType(int type) {
    return (type == TypeSystemImpl.shortTypeCode);
  }

  public boolean isLongType(int type) {
    return (type == TypeSystemImpl.longTypeCode);
  }

  public boolean isDoubleType(int type) {
    return (type == TypeSystemImpl.doubleTypeCode);
  }

  public Heap getHeap() {
    return this.svd.heap;
  }

  ByteHeap getByteHeap() {
    return this.svd.byteHeap;
  }

  ShortHeap getShortHeap() {
    return this.svd.shortHeap;
  }

  LongHeap getLongHeap() {
    return this.svd.longHeap;
  }

  StringHeap getStringHeap() {
    return this.svd.stringHeap;
  }

  public int getFeatureOffset(int feat) {
    if ((feat < 1) || (feat >= this.svd.casMetadata.featureOffset.length)) {
      return -1;
    }
    return this.svd.casMetadata.featureOffset[feat];
  }

  public static void setupTSDefault(TypeSystemImpl ts) {
    // because historically this method was public, protect
    // against user code calling multiple times
    if (ts.areBuiltInTypesSetup) {
      return;
    }
    synchronized (ts) {
      if (ts.areBuiltInTypesSetup) {
        return;
      }
  
      // W A R N I N G (July 2007)
  
      // C++ code has "hard-wired" the type code numbers for the
      // built-in types, so you cannot change the order of the types
      // Also, the complete serialization depends on the type-code numbers
      // for the client and the server for the built-in types being
      // the same.
  
      // The initialization code for types cannot depend on the type system
      // having already been set up, because, obviously, it isn't set up (yet).
  
      // It is important to add types in a particular order and to set
      // ts.xxx<type-name> and ts.xxx<type-name>code values so they are
      // set before they're used. Some of the add-type logic is written
      // to depend on the built-in types already having these values set.
      // For example: addType ( ARRAY_BASE ) calls, eventually,
      // ts.ll_isPrimitiveType -> ll_isRefType ->ll_getTypeClass -> ll_subsumes
      // and along the way these are testing / comparing against type codes
      // for built-in types which need to have been set.
  
      // Create top type.
      Type top = ts.addTopType(CAS.TYPE_NAME_TOP);
      // Add basic data types.
      Type intT = ts.addType(CAS.TYPE_NAME_INTEGER, top);
      ts.intType = (TypeImpl) intT;
      if (TypeSystemImpl.intTypeCode != ts.intType.getCode()) throw new RuntimeException();
      
      Type floatT = ts.addType(CAS.TYPE_NAME_FLOAT, top);
      ts.floatType = (TypeImpl) floatT;
      if (TypeSystemImpl.floatTypeCode != ts.floatType.getCode()) throw new RuntimeException();
      
      Type stringT = ts.addType(CAS.TYPE_NAME_STRING, top);
      ts.stringType = (TypeImpl) stringT;
      if (TypeSystemImpl.stringTypeCode != ts.stringType.getCode()) throw new RuntimeException();
      
      
      // Add arrays.
      Type array = ts.addType(CAS.TYPE_NAME_ARRAY_BASE, top);
      ts.arrayBaseType = (TypeImpl) array; // do here - used in next
      if (TypeSystemImpl.arrayBaseTypeCode != ts.arrayBaseType.getCode()) throw new RuntimeException();
      
      TypeImpl fsArray = ts.fsArrayType = (TypeImpl) ts.addType(CAS.TYPE_NAME_FS_ARRAY, array);
      if (TypeSystemImpl.fsArrayTypeCode != fsArray.getCode()) throw new RuntimeException();
      
      TypeImpl floatArray = ts.floatArrayType = (TypeImpl) ts.addType(CAS.TYPE_NAME_FLOAT_ARRAY, array);
      if (TypeSystemImpl.floatArrayTypeCode != floatArray.getCode()) throw new RuntimeException();
      
      TypeImpl intArray = ts.intArrayType = (TypeImpl) ts.addType(CAS.TYPE_NAME_INTEGER_ARRAY, array);
      if (TypeSystemImpl.intArrayTypeCode != intArray.getCode()) throw new RuntimeException();
      
      TypeImpl stringArray = ts.stringArrayType = (TypeImpl) ts.addType(CAS.TYPE_NAME_STRING_ARRAY, array);
      if (TypeSystemImpl.stringArrayTypeCode != stringArray.getCode()) throw new RuntimeException();
      
      // Add lists.
      Type list = ts.addType(CAS.TYPE_NAME_LIST_BASE, top);
      
      // FS lists.
      Type fsList = ts.addType(CAS.TYPE_NAME_FS_LIST, list);
      Type fsEList = ts.addType(CAS.TYPE_NAME_EMPTY_FS_LIST, fsList);
      Type fsNeList = ts.addType(CAS.TYPE_NAME_NON_EMPTY_FS_LIST, fsList);
      ts.addFeature(CAS.FEATURE_BASE_NAME_HEAD, fsNeList, top, true);
      ts.addFeature(CAS.FEATURE_BASE_NAME_TAIL, fsNeList, fsList, true);
      
      // Float lists.
      Type floatList = ts.addType(CAS.TYPE_NAME_FLOAT_LIST, list);
      Type floatEList = ts.addType(CAS.TYPE_NAME_EMPTY_FLOAT_LIST, floatList);
      Type floatNeList = ts.addType(CAS.TYPE_NAME_NON_EMPTY_FLOAT_LIST, floatList);
      ts.addFeature(CAS.FEATURE_BASE_NAME_HEAD, floatNeList, floatT, false);
      ts.addFeature(CAS.FEATURE_BASE_NAME_TAIL, floatNeList, floatList, true);
      
      // Integer lists.
      Type intList = ts.addType(CAS.TYPE_NAME_INTEGER_LIST, list);
      Type intEList = ts.addType(CAS.TYPE_NAME_EMPTY_INTEGER_LIST, intList);
      Type intNeList = ts.addType(CAS.TYPE_NAME_NON_EMPTY_INTEGER_LIST, intList);
      ts.addFeature(CAS.FEATURE_BASE_NAME_HEAD, intNeList, intT, false);
      ts.addFeature(CAS.FEATURE_BASE_NAME_TAIL, intNeList, intList, true);
      
      // String lists.
      Type stringList = ts.addType(CAS.TYPE_NAME_STRING_LIST, list);
      Type stringEList = ts.addType(CAS.TYPE_NAME_EMPTY_STRING_LIST, stringList);
      Type stringNeList = ts.addType(CAS.TYPE_NAME_NON_EMPTY_STRING_LIST, stringList);
      ts.addFeature(CAS.FEATURE_BASE_NAME_HEAD, stringNeList, stringT, false);
      ts.addFeature(CAS.FEATURE_BASE_NAME_TAIL, stringNeList, stringList, true);
  
      Type booleanT = ts.addType(CAS.TYPE_NAME_BOOLEAN, top);
      ts.booleanType = (TypeImpl) booleanT;
      if (TypeSystemImpl.booleanTypeCode != ts.booleanType.getCode()) throw new RuntimeException();
      
      Type byteT = ts.addType(CAS.TYPE_NAME_BYTE, top);
      ts.byteType = (TypeImpl) byteT;
      if (TypeSystemImpl.byteTypeCode != ts.byteType.getCode()) throw new RuntimeException();
      
      Type shortT = ts.addType(CAS.TYPE_NAME_SHORT, top);
      ts.shortType = (TypeImpl) shortT;
      if (TypeSystemImpl.shortTypeCode != ts.shortType.getCode()) throw new RuntimeException();
      
      Type longT = ts.addType(CAS.TYPE_NAME_LONG, top);
      ts.longType = (TypeImpl) longT;
      if (TypeSystemImpl.longTypeCode != ts.longType.getCode()) throw new RuntimeException();
      
      Type doubleT = ts.addType(CAS.TYPE_NAME_DOUBLE, top);
      ts.doubleType = (TypeImpl) doubleT;
      if (TypeSystemImpl.doubleTypeCode != ts.doubleType.getCode()) throw new RuntimeException();
  
      // array type initialization must follow the component type it's based on
      TypeImpl booleanArray = ts.booleanArrayType = (TypeImpl) ts.addType(CAS.TYPE_NAME_BOOLEAN_ARRAY, array);
      if (TypeSystemImpl.booleanArrayTypeCode != booleanArray.getCode()) throw new RuntimeException();
      
      TypeImpl byteArray = ts.byteArrayType = (TypeImpl) ts.addType(CAS.TYPE_NAME_BYTE_ARRAY, array);
      if (TypeSystemImpl.byteArrayTypeCode != byteArray.getCode()) throw new RuntimeException();
      
      TypeImpl shortArray = ts.shortArrayType = (TypeImpl) ts.addType(CAS.TYPE_NAME_SHORT_ARRAY, array);
      if (TypeSystemImpl.shortArrayTypeCode != shortArray.getCode()) throw new RuntimeException();
      
      TypeImpl longArray = ts.longArrayType = (TypeImpl) ts.addType(CAS.TYPE_NAME_LONG_ARRAY, array);
      if (TypeSystemImpl.longArrayTypeCode != longArray.getCode()) throw new RuntimeException();
      
      TypeImpl doubleArray = ts.doubleArrayType = (TypeImpl) ts.addType(CAS.TYPE_NAME_DOUBLE_ARRAY, array);
      if (TypeSystemImpl.doubleArrayTypeCode != doubleArray.getCode()) throw new RuntimeException();
  
      // Sofa Stuff
      Type sofa = ts.addType(CAS.TYPE_NAME_SOFA, top);
      ts.sofaType = (TypeImpl) sofa;
      if (TypeSystemImpl.sofaTypeCode != ts.sofaType.getCode()) throw new RuntimeException();
      
      ts.addFeature(CAS.FEATURE_BASE_NAME_SOFANUM, sofa, intT, false);
      ts.addFeature(CAS.FEATURE_BASE_NAME_SOFAID, sofa, stringT, false);
      ts.addFeature(CAS.FEATURE_BASE_NAME_SOFAMIME, sofa, stringT, false);
      // Type localSofa = ts.addType(CAS.TYPE_NAME_LOCALSOFA, sofa);
      ts.addFeature(CAS.FEATURE_BASE_NAME_SOFAARRAY, sofa, top, true);
      ts.addFeature(CAS.FEATURE_BASE_NAME_SOFASTRING, sofa, stringT, false);
      // Type remoteSofa = ts.addType(CAS.TYPE_NAME_REMOTESOFA, sofa);
      ts.addFeature(CAS.FEATURE_BASE_NAME_SOFAURI, sofa, stringT, false);
  
      // Annotations
      Type annotBaseType = ts.addType(CAS.TYPE_NAME_ANNOTATION_BASE, top);
      ts.annotBaseType = (TypeImpl) annotBaseType;
      if (TypeSystemImpl.annotBaseTypeCode != ts.annotBaseType.getCode()) throw new RuntimeException();
      
      ts.addFeature(CAS.FEATURE_BASE_NAME_SOFA, annotBaseType, sofa, false);
      
      Type annotType = ts.addType(CAS.TYPE_NAME_ANNOTATION, annotBaseType);
      ts.annotType = (TypeImpl) annotType;
      if (TypeSystemImpl.annotTypeCode != ts.annotType.getCode()) throw new RuntimeException();
      
      ts.addFeature(CAS.FEATURE_BASE_NAME_BEGIN, annotType, intT, false);
      ts.addFeature(CAS.FEATURE_BASE_NAME_END, annotType, intT, false);
      
      Type docType = ts.addType(CAS.TYPE_NAME_DOCUMENT_ANNOTATION, annotType);
      ts.docType = (TypeImpl) docType;
      
      ts.addFeature(CAS.FEATURE_BASE_NAME_LANGUAGE, docType, stringT, false);
  
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
      // because this is volatile, there's a memory barrier that insures
      // the things before happen before other reads 
      // see http://stackoverflow.com/questions/13688697/is-a-write-to-a-volatile-a-memory-barrier-in-java
      ts.areBuiltInTypesSetup = true;
    } // end of sync block
  }

  private static void setTypeFinal(Type type) {
    TypeImpl t = (TypeImpl) type;
    t.setFeatureFinal();
    t.setInheritanceFinal();
  }

  /*
   * Only called on base CAS
   */
  /**
   * @see org.apache.uima.cas.admin.CASMgr#initCASIndexes()
   */
  @Override
public void initCASIndexes() throws CASException {
    final TypeSystemImpl ts = this.svd.casMetadata.ts;
    if (!ts.isCommitted()) {
      throw new CASException(CASException.MUST_COMMIT_TYPE_SYSTEM, null);
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

  ArrayList<String> getStringTable() {
    // return this.stringList;
    return this.svd.baseCAS.getStringList();
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
public JCas getJCas() throws CASException {
    if (this.jcas == null) {
      this.jcas = (JCasImpl) JCasImpl.getJCas(this);
    } else {
      this.jcas.maybeInitializeForClassLoader(svd.jcasClassLoader);
    }
    return this.jcas;
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
  
  private CAS getViewFromSofaNbr(int nbr) {
    final ArrayList<CAS> sn2v = this.svd.sofaNbr2ViewMap;
    if (nbr < sn2v.size()) {
      return sn2v.get(nbr);
    }
    return null;
  }
  
  private void setViewForSofaNbr(int nbr, CAS view) {
    ArrayList<CAS> sn2v = this.svd.sofaNbr2ViewMap;
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
    CAS aView = new CASImpl(this.svd.baseCAS, null, this.isUsedJcasCache);
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
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFANAME_ALREADY_EXISTS,
          new String[] { aSofaID });
      throw e;
    }
    SofaFS newSofa = createSofa(absoluteSofaName, null);
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
    final int sofaNbr = aSofa.getSofaRef();
//    final Integer sofaNbrInteger = Integer.valueOf(sofaNbr);

    CASImpl aView = (CASImpl) getViewFromSofaNbr(sofaNbr);
    if (null == aView) {
      // This is the deserializer case, or the case where an older API created a
      // sofa,
      // which is now creating the associated view

      // create a new CAS view
      aView = new CASImpl(this.svd.baseCAS, aSofa, this.isUsedJcasCache);
      setViewForSofaNbr(sofaNbr, aView);
      verifySofaNameUniqueIfDeserializedViewAdded(sofaNbr, aSofa);
      return aView;
    }

    // for deserialization - might be reusing a view, and need to tie new Sofa
    // to old View
    if (0 == aView.mySofaRef) {
      aView.mySofaRef = ((FeatureStructureImpl) aSofa).getAddress();
    }

    verifySofaNameUniqueIfDeserializedViewAdded(sofaNbr, aSofa);
    return aView;
  }
  
  boolean isSofaView(int sofaAddr) {
    if (mySofaRef == -1) {
      // don't create initial sofa
      return false;
    }
    return mySofaRef == sofaAddr;
  }
  
  

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
      if (this.svd.sofaNameSet.contains(id)) {
        CASRuntimeException e = new CASRuntimeException(
            CASRuntimeException.SOFANAME_ALREADY_EXISTS, new String[] { id });
        throw e;
      }
      this.svd.sofaNameSet.add(id);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getTypeSystem()
   */
  @Override
public LowLevelTypeSystem ll_getTypeSystem() {
    return this.svd.casMetadata.ts.getLowLevelTypeSystem();
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
    final TypeSystemImpl ts = this.svd.casMetadata.ts;
    if (!ts.isApprop(domTypeCode, featCode)) {
      LowLevelException e = new LowLevelException(LowLevelException.FEAT_DOM_ERROR);
      e.addArgument(Integer.toString(domTypeCode));
      e.addArgument(ts.ll_getTypeForCode(domTypeCode).getName());
      e.addArgument(Integer.toString(featCode));
      e.addArgument(ts.ll_getFeatureForCode(featCode).getName());
      throw e;
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
    checkTypingConditions(((TypeImpl) domType).getCode(), ((TypeImpl) ranType).getCode(),
        ((FeatureImpl) feat).getCode());
  }

  // Assumes that parameters are valid type system codes, so check that first.
  private final void checkTypingConditions(int domTypeCode, int ranTypeCode, int featCode) {
    checkDomTypeConditions(domTypeCode, featCode);
    final TypeSystemImpl ts = this.svd.casMetadata.ts;
    if (!ts.subsumes(ts.range(featCode), ranTypeCode)) {
      LowLevelException e = new LowLevelException(LowLevelException.FEAT_RAN_ERROR);
      e.addArgument(Integer.toString(featCode));
      e.addArgument(ts.ll_getFeatureForCode(featCode).getName());
      e.addArgument(Integer.toString(ranTypeCode));
      e.addArgument(ts.ll_getTypeForCode(ranTypeCode).getName());
      throw e;
    }
  }

  private final void checkFsRan(int featCode) throws LowLevelException {
    final TypeSystemImpl ts = this.svd.casMetadata.ts;
    final int rangeTypeCode = ts.range(featCode);
    if (!ll_isRefType(rangeTypeCode)) {
      LowLevelException e = new LowLevelException(LowLevelException.FS_RAN_TYPE_ERROR);
      e.addArgument(Integer.toString(featCode));
      e.addArgument(ts.ll_getFeatureForCode(featCode).getName());
      e.addArgument(ts.ll_getTypeForCode(rangeTypeCode).getName());
      throw e;
    }
  }

  private final void checkFeature(int featureCode) {
    if (!this.svd.casMetadata.ts.isFeature(featureCode)) {
      LowLevelException e = new LowLevelException(LowLevelException.INVALID_FEATURE_CODE);
      e.addArgument(Integer.toString(featureCode));
      throw e;
    }
  }

  private final void checkTypeAt(int typeCode, int fsRef) {
    if (!this.svd.casMetadata.ts.isType(typeCode)) {
      LowLevelException e = new LowLevelException(LowLevelException.VALUE_NOT_A_TYPE);
      e.addArgument(Integer.toString(typeCode));
      e.addArgument(Integer.toString(fsRef));
      throw e;
    }
  }

  final void checkFsRef(int fsRef) {
    if ((fsRef <= NULL_FS_REF) || (fsRef >= this.getHeap().heap.length)) {
      LowLevelException e = new LowLevelException(LowLevelException.INVALID_FS_REF);
      e.addArgument(Integer.toString(fsRef));
      throw e;
    }
  }

  @Override
public final boolean ll_isRefType(int typeCode) {
    if ((typeCode == TypeSystemImpl.intTypeCode) || (typeCode == TypeSystemImpl.floatTypeCode)
        || (typeCode == TypeSystemImpl.stringTypeCode) || (typeCode == TypeSystemImpl.byteTypeCode)
        || (typeCode == TypeSystemImpl.booleanTypeCode) || (typeCode == TypeSystemImpl.shortTypeCode)
        || (typeCode == TypeSystemImpl.longTypeCode) || (typeCode == TypeSystemImpl.doubleTypeCode)) {
      return false;
    }
    if (ll_getTypeSystem().ll_isStringSubtype(typeCode)) {
      return false;
    }
    return true;
  }

  @Override
public final int ll_getTypeClass(int typeCode) {
    final TypeSystemImpl ts = this.svd.casMetadata.ts;
    if (typeCode == TypeSystemImpl.intTypeCode) {
      return TYPE_CLASS_INT;
    }
    if (typeCode == TypeSystemImpl.floatTypeCode) {
      return TYPE_CLASS_FLOAT;
    }
    if (ts.subsumes(TypeSystemImpl.stringTypeCode, typeCode)) {
      return TYPE_CLASS_STRING;
    }
    if (typeCode == TypeSystemImpl.intArrayTypeCode) {
      return TYPE_CLASS_INTARRAY;
    }
    if (typeCode == TypeSystemImpl.floatArrayTypeCode) {
      return TYPE_CLASS_FLOATARRAY;
    }
    if (typeCode == TypeSystemImpl.stringArrayTypeCode) {
      return TYPE_CLASS_STRINGARRAY;
    }
    if (typeCode == TypeSystemImpl.fsArrayTypeCode) {
      return TYPE_CLASS_FSARRAY;
    }
    if (typeCode == TypeSystemImpl.booleanTypeCode) {
      return TYPE_CLASS_BOOLEAN;
    }
    if (typeCode == TypeSystemImpl.byteTypeCode) {
      return TYPE_CLASS_BYTE;
    }
    if (typeCode == TypeSystemImpl.shortTypeCode) {
      return TYPE_CLASS_SHORT;
    }
    if (typeCode == TypeSystemImpl.longTypeCode) {
      return TYPE_CLASS_LONG;
    }
    if (typeCode == TypeSystemImpl.doubleTypeCode) {
      return TYPE_CLASS_DOUBLE;
    }
    if (typeCode == TypeSystemImpl.booleanArrayTypeCode) {
      return TYPE_CLASS_BOOLEANARRAY;
    }
    if (typeCode == TypeSystemImpl.byteArrayTypeCode) {
      return TYPE_CLASS_BYTEARRAY;
    }
    if (typeCode == TypeSystemImpl.shortArrayTypeCode) {
      return TYPE_CLASS_SHORTARRAY;
    }
    if (typeCode == TypeSystemImpl.longArrayTypeCode) {
      return TYPE_CLASS_LONGARRAY;
    }
    if (typeCode == TypeSystemImpl.doubleArrayTypeCode) {
      return TYPE_CLASS_DOUBLEARRAY;
    }
    if (isArrayType(typeCode)) {
      return TYPE_CLASS_FSARRAY;
    }

    return TYPE_CLASS_FS;
  }

  @Override
public final int ll_createFS(int typeCode) {
    final int fsAddr = this.getHeap().add(this.svd.casMetadata.fsSpaceReq[typeCode], typeCode);
    svd.cache_not_in_index = fsAddr;
    if (traceFSs) {
      traceFSCreate((FeatureStructureImpl) ll_getFSForRef(fsAddr));
    }
    return fsAddr;
  }

  @Override
public final int ll_createFS(int typeCode, boolean doCheck) {
    if (doCheck) {
      if (!this.svd.casMetadata.ts.isType(typeCode) || !isCreatableType(typeCode)) {
        LowLevelException e = new LowLevelException(LowLevelException.CREATE_FS_OF_TYPE_ERROR);
        e.addArgument(Integer.toString(typeCode));
        throw e;
      }
    }
    return ll_createFS(typeCode);
  }

  // never called, not used May 2007
  // /**
  // * Create an instance of a subtype of AnnotationBase.
  // *
  // * @param typeCode
  // * @return An annotation?
  // */
  // public final int ll_createAnnotationBaseFS(int typeCode) {
  // int addr = ll_createFS(typeCode);
  // setSofaFeat(addr, this.mySofaRef);
  // return addr;
  // }

  // never called, not used May 2007
  // public final int ll_createAnnotationBaseFS(int typeCode, boolean doCheck) {
  // if (doCheck) {
  // final TypeSystemImpl ts = this.svd.casMetadata.ts;
  // if (!ts.isType(typeCode) || !isCreatableType(typeCode)
  // || ts.ll_subsumes(ts.annotBaseTypeCode, typeCode)) {
  // LowLevelException e = new
  // LowLevelException(LowLevelException.CREATE_FS_OF_TYPE_ERROR);
  // e.addArgument(Integer.toString(typeCode));
  // throw e;
  // }
  // }
  // return ll_createFS(typeCode);
  // }

  /**
   * Create a temporary (i.e., per document) array FS on the heap.
   * 
   * @param type
   *                The type code of the array to be created.
   * @param len
   *                The length of the array to be created.
   * @return -               
   * @exception ArrayIndexOutOfBoundsException
   *                    If <code>type</code> is not a type.
   */
  public int createTempArray(int type, int len) {
    return ll_createArray(type, len);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_createArray(int, int)
   */
  @Override
public int ll_createArray(int typeCode, int arrayLength) {
    final int addr = this.getHeap().add(arrayContentOffset + arrayLength, typeCode);
    this.getHeap().heap[(addr + arrayLengthFeatOffset)] = arrayLength;
    svd.cache_not_in_index = addr;
    if (traceFSs) {
      traceFSCreate((FeatureStructureImpl) ll_getFSForRef(addr));
    }
    return addr;
  }

  public int ll_createAuxArray(int typeCode, int arrayLength) {
    final int addr = this.getHeap().add(arrayContentOffset + 1, typeCode);
    this.getHeap().heap[(addr + arrayLengthFeatOffset)] = arrayLength;
    svd.cache_not_in_index = addr;
    if (traceFSs) {
      traceFSCreate((FeatureStructureImpl) ll_getFSForRef(addr));
    }
    return addr;
  }

  @Override
public int ll_createByteArray(int arrayLength) {
    final int addr = ll_createAuxArray(TypeSystemImpl.byteArrayTypeCode, arrayLength);
    this.getHeap().heap[addr + arrayContentOffset] = this.getByteHeap().reserve(arrayLength);
    return addr;
  }

  @Override
public int ll_createBooleanArray(int arrayLength) {
    final int addr = ll_createAuxArray(TypeSystemImpl.booleanArrayTypeCode, arrayLength);
    this.getHeap().heap[addr + arrayContentOffset] = this.getByteHeap().reserve(arrayLength);
    return addr;
  }

  @Override
public int ll_createShortArray(int arrayLength) {
    final int addr = ll_createAuxArray(TypeSystemImpl.shortArrayTypeCode, arrayLength);
    this.getHeap().heap[addr + arrayContentOffset] = this.getShortHeap().reserve(arrayLength);
    return addr;
  }

  @Override
public int ll_createLongArray(int arrayLength) {
    final int addr = ll_createAuxArray(TypeSystemImpl.longArrayTypeCode, arrayLength);
    this.getHeap().heap[addr + arrayContentOffset] = this.getLongHeap().reserve(arrayLength);
    return addr;
  }

  @Override
public int ll_createDoubleArray(int arrayLength) {
    final int addr = ll_createAuxArray(TypeSystemImpl.doubleArrayTypeCode, arrayLength);
    this.getHeap().heap[addr + arrayContentOffset] = this.getLongHeap().reserve(arrayLength);
    return addr;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_createArray(int, int, boolean)
   */
  @Override
public int ll_createArray(int typeCode, int arrayLength, boolean doChecks) {
    if (doChecks) {
      final TypeSystemImpl ts = this.svd.casMetadata.ts;
      // Check typeCode, arrayLength
      if (!ts.isType(typeCode)) {
        LowLevelException e = new LowLevelException(LowLevelException.INVALID_TYPE_ARGUMENT);
        e.addArgument(Integer.toString(typeCode));
        throw e;
      }
      if (!isCreatableArrayType(typeCode)) {
        LowLevelException e = new LowLevelException(LowLevelException.CREATE_ARRAY_OF_TYPE_ERROR);
        e.addArgument(Integer.toString(typeCode));
        e.addArgument(ts.ll_getTypeForCode(typeCode).getName());
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
    return ((tc == TYPE_CLASS_INTARRAY) || (tc == TYPE_CLASS_FLOATARRAY)
        || (tc == TYPE_CLASS_STRINGARRAY) || (tc == TYPE_CLASS_FSARRAY)
        || (tc == TYPE_CLASS_BOOLEANARRAY) || (tc == TYPE_CLASS_BYTEARRAY)
        || (tc == TYPE_CLASS_SHORTARRAY) || (tc == TYPE_CLASS_LONGARRAY) || (tc == TYPE_CLASS_DOUBLEARRAY));
  }

  @Override
public final int ll_getFSRef(FeatureStructure fsImpl) {
    if (null == fsImpl) {
      return NULL;
    }
    final FeatureStructureImpl fsi = (FeatureStructureImpl) fsImpl;
    if (this != fsi.getCASImpl()) {
      if (this.getBaseCAS() != fsi.getCASImpl().getBaseCAS()) {  
        // https://issues.apache.org/jira/browse/UIMA-3429
        throw new CASRuntimeException(CASRuntimeException.DEREF_FS_OTHER_CAS, 
            new Object[] {fsi.toString(), this.toString() } );
      }
    }
    return fsi.getAddress();
  }

  @Override
@SuppressWarnings("unchecked")
  public <T extends FeatureStructure> T ll_getFSForRef(int fsRef) {
    // return this.svd.casMetadata.fsClassRegistry.createFS(fsRef, this);
    if (fsRef == 0) {
      return null;
    }
    if (this.svd.useFSCache) {
      // FS object cache code.
      // ***** NOTE: This code has not been maintained and may not work ******
      T fs = null;
      try {
        fs = (T) this.svd.fsArray[fsRef];
      } catch (ArrayIndexOutOfBoundsException e) {
        // Do nothing. Code below will expand array as needed.
      }
      if (fs == null) {
        fs = (T) this.svd.localFsGenerators[getHeap().heap[fsRef]].createFS(fsRef, this);
        // fs =
        // this.svd.casMetadata.fsClassRegistry.createFSusingGenerator(fsRef,
        // this);
        if (fsRef >= this.svd.fsArray.length) {
          int newLen = this.svd.fsArray.length * 2;
          while (newLen <= fsRef) {
            newLen *= 2;
          }
          FeatureStructure[] newArray = new FeatureStructure[newLen];
          System.arraycopy(this.svd.fsArray, 0, newArray, 0, this.svd.fsArray.length);
          this.svd.fsArray = newArray;
        }
        this.svd.fsArray[fsRef] = fs;
      }
      return fs;
    }

    return (T) this.svd.localFsGenerators[getHeap().heap[fsRef]].createFS(fsRef, this);
    // return this.svd.casMetadata.fsClassRegistry.createFSusingGenerator(fsRef,
    // this);
  }

  @Override
public final int ll_getIntValue(int fsRef, int featureCode) {
    return this.getHeap().heap[(fsRef + this.svd.casMetadata.featureOffset[featureCode])];
  }

  public final int ll_getIntValueFeatOffset(int fsRef, int featureOffset) {
    return this.getHeap().heap[fsRef + featureOffset];
  }

  @Override
public final float ll_getFloatValue(int fsRef, int featureCode) {
    return int2float(ll_getIntValue(fsRef, featureCode));
  }

  @Override
public final String ll_getStringValue(int fsRef, int featureCode) {
    return this.getStringHeap().getStringForCode(ll_getIntValue(fsRef, featureCode));
  }
  
  public final String ll_getStringValueFeatOffset(int fsRef, int featureOffset) {
    return this.getStringHeap().getStringForCode(ll_getIntValueFeatOffset(fsRef, featureOffset));
  }

  @Override
public final int ll_getRefValue(int fsRef, int featureCode) {
    return ll_getIntValue(fsRef, featureCode);
  }

  public final int ll_getRefValueFeatOffset(int fsRef, int featureOffset) {
    return ll_getIntValueFeatOffset(fsRef, featureOffset);
  }

  @Override
public final int ll_getIntValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, TypeSystemImpl.intTypeCode, featureCode);
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
      checkNonArrayConditions(fsRef, TypeSystemImpl.floatTypeCode, featureCode);
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
      checkNonArrayConditions(fsRef, TypeSystemImpl.stringTypeCode, featureCode);
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
    return ll_getIntValue(fsRef, featureCode);
  }
  
  public int ll_getAnnotBegin(int fsRef) {
    return ll_getIntValueFeatOffset(fsRef, svd.annotFeatOffset_begin);
  }
  
  public int ll_getAnnotEnd(int fsRef) {
    return ll_getIntValueFeatOffset(fsRef, svd.annotFeatOffset_end);
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
   * @param fsRef - the FS to test if it is in the indexes
   * @param featureCode - the feature being tested
   * @return true if something may need to be added back
   */
  private boolean checkForInvalidFeatureSetting(int fsRef, int featureCode) {
    if (fsRef == svd.cache_not_in_index) {
      return false;
    }
    final int ssz = svd.fssTobeAddedback.size();
    // skip if protection is disabled, an no explicit protection block
    if (IS_DISABLED_PROTECT_INDEXES && ssz == 0) {
      return false;
    }
       
    // next method skips if the fsRef is not in the index (cache)
    final boolean wasRemoved = removeFromCorruptableIndexAnyView(
        fsRef, 
        (ssz > 0) ? svd.fssTobeAddedback.get(ssz - 1) : 
                    svd.fsTobeAddedbackSingle, 
        featureCode);            
    
    
    // skip message if wasn't removed
    // skip message if protected in explicit block
    if (wasRemoved && IS_REPORT_FS_UPDATE_CORRUPTS_INDEX && ssz == 0) {
      featModWhileInIndexReport(fsRef, featureCode);
    }  
    return wasRemoved;
  }
  
  private void featModWhileInIndexReport(int fsRef, int featureCode) {
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
        this.getTypeSystemImpl().ll_getFeatureForCode(featureCode).getName(),
        new FeatureStructureImplC(this, fsRef).toString(),  
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
  private void maybeAddback(int fsRef) {
    if (!svd.fsTobeAddedbackSingleInUse && (!IS_DISABLED_PROTECT_INDEXES) && svd.fssTobeAddedback.size() == 0) {
      svd.fsTobeAddedbackSingle.addback(fsRef);
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
   * @param fsRef the fs
   * @param toBeAdded the place to record removal actions
   * @param featCode the feature code to test if it's used as a key in some index
   * @return true if the fs was removed
   */
  boolean removeFromCorruptableIndexAnyView(final int fsRef, FSsTobeAddedback toBeAdded, int featCode) {
    if (fsRef != svd.cache_not_in_index && svd.featureCodesInIndexKeys.contains(featCode)) {
      boolean wasRemoved = removeFromCorruptableIndexAnyView(fsRef, toBeAdded);
      svd.cache_not_in_index = fsRef; // because will remove it if its in the index.
      return wasRemoved;
    }
    return false;
  }
  
  /**
   * Remove the fsRef from any corruptable index in any view, and remember
   * per view whether it was actually in the index.
   * @param fsRef -
   * @param toBeAdded -
   * @return true if it was removed (one or more times)
   */
  boolean removeFromCorruptableIndexAnyView(final int fsRef, FSsTobeAddedback toBeAdded) {
    final int typeCode = getTypeCode(fsRef);
    final TypeSystemImpl tsi = getTypeSystemImpl();
    if (tsi.isAnnotationBaseOrSubtype(typeCode)) {
      // only need to check one view
      // get that view carefully, in case things are not yet properly initialized
      final int addrOfSofaFS = this.getSofaFeat(fsRef);
      if (addrOfSofaFS == 0) {
        return false;  // uninitialized sofa ref- can't be indexed 
      }
      CASImpl view;
      if (addrOfSofaFS == this.getSofaRef()) {
        view = this;
      } else {
        int sofaNum = ll_getSofaNum(addrOfSofaFS);
        view = (CASImpl) getViewFromSofaNbr(sofaNum);
        if (null == view) {
          return false;
        }
      }
      return removeAndRecord(fsRef, view.indexRepository, toBeAdded);
    }
    
    // not a subtype of AnnotationBase, need to check all views (except base)
    // sofas indexed in the base view are not corruptable.
    final Iterator<CAS> viewIterator = getViewIterator();
    boolean wasRemoved = false;
    while (viewIterator.hasNext()) {
      wasRemoved |= removeAndRecord(fsRef, (FSIndexRepositoryImpl) viewIterator.next().getIndexRepository(), toBeAdded);
    }
    return wasRemoved;
  }
  
  boolean removeFromCorruptableIndexAnyViewSetCache(final int fsRef, FSsTobeAddedback toBeAdded) {
    if (fsRef != svd.cache_not_in_index) {
      svd.cache_not_in_index = fsRef;
      return removeFromCorruptableIndexAnyView(fsRef, toBeAdded);
    }
    return false;
  }
  
  /**
   * remove a FS from corruptable indexes in this view
   * @param fsRef the fs to be removed
   * @param ir the view
   * @param toBeAdded the place to record how many times it was in the index, per view
   * @return true if it was removed, false if it wasn't in any corruptable index.
   */
  private boolean removeAndRecord(int fsRef, FSIndexRepositoryImpl ir, FSsTobeAddedback toBeAdded) {
    int nbrRemoved = ir.removeIfInCorrputableIndexInThisView(fsRef);
    if (0 < nbrRemoved) {
      toBeAdded.recordRemove(fsRef, ir, nbrRemoved);
    }
    return (0 < nbrRemoved);
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
    if (null != value) {
      final TypeSystemImpl ts = this.svd.casMetadata.ts;
      String[] stringSet = ts.ll_getStringSet(ts.ll_getRangeType(featureCode));
      if (stringSet != null) {
        final int rc = Arrays.binarySearch(stringSet, value);
        if (rc < 0) {
          // Not a legal value.
          CASRuntimeException e = new CASRuntimeException(CASRuntimeException.ILLEGAL_STRING_VALUE,
              new String[] { value, ts.ll_getTypeForCode(ts.ll_getRangeType(featureCode)).getName() });
          throw e;
        }
      }
    }
    final int stringAddr = (value == null) ? NULL : this.getStringHeap().addString(value);
    setFeatureValue(fsRef, featureCode, stringAddr);
  }

  @Override
public final void ll_setRefValue(int fsRef, int featureCode, int value) {
    // no index check because refs can't be keys
    setFeatureValueNoIndexCorruptionCheck(fsRef, featureCode, value);
  }
  
  @Override
public final void ll_setIntValue(int fsRef, int featureCode, int value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, TypeSystemImpl.intTypeCode, featureCode);
    }
    ll_setIntValue(fsRef, featureCode, value);
  }

  @Override
public final void ll_setFloatValue(int fsRef, int featureCode, float value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, TypeSystemImpl.floatTypeCode, featureCode);
    }
    ll_setFloatValue(fsRef, featureCode, value);
  }

  @Override
public final void ll_setStringValue(int fsRef, int featureCode, String value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, TypeSystemImpl.stringTypeCode, featureCode);
    }
    ll_setStringValue(fsRef, featureCode, value);
  }

  @Override
public final void ll_setCharBufferValue(int fsRef, int featureCode, char[] buffer, int start,
      int length, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, TypeSystemImpl.stringTypeCode, featureCode);
    }
    ll_setCharBufferValue(fsRef, featureCode, buffer, start, length);
  }

  @Override
public final void ll_setCharBufferValue(int fsRef, int featureCode, char[] buffer, int start,
      int length) {
    // don't do any index check here, done by inner call
    final int stringCode = this.getStringHeap().addCharBuffer(buffer, start, length);
    ll_setIntValue(fsRef, featureCode, stringCode);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_copyCharBufferValue(int, int,
   *      char, int)
   */
  @Override
public int ll_copyCharBufferValue(int fsRef, int featureCode, char[] buffer, int start) {
    final int stringCode = ll_getIntValue(fsRef, featureCode);
    if (stringCode == NULL) {
      return -1;
    }
    return this.getStringHeap().copyCharsToBuffer(stringCode, buffer, start);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getCharBufferValueSize(int,
   *      int)
   */
  @Override
public int ll_getCharBufferValueSize(int fsRef, int featureCode) {
    final int stringCode = ll_getIntValue(fsRef, featureCode);
    if (stringCode == NULL) {
      return -1;
    }
    return this.getStringHeap().getCharArrayLength(stringCode);
  }

  @Override
public final void ll_setRefValue(int fsRef, int featureCode, int value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkFsRefConditions(fsRef, featureCode);
      checkFsRef(value);
    }
    ll_setRefValue(fsRef, featureCode, value);
  }

  @Override
public final int ll_getIntArrayValue(int fsRef, int position) {
    final int pos = getArrayStartAddress(fsRef) + position;
    return this.getHeap().heap[pos];
  }

  @Override
public final float ll_getFloatArrayValue(int fsRef, int position) {
    final int pos = getArrayStartAddress(fsRef) + position;
    return int2float(this.getHeap().heap[pos]);
  }

  @Override
public final String ll_getStringArrayValue(int fsRef, int position) {
    final int pos = getArrayStartAddress(fsRef) + position;
    return getStringForCode(this.getHeap().heap[pos]);
  }

  @Override
public final int ll_getRefArrayValue(int fsRef, int position) {
    final int pos = getArrayStartAddress(fsRef) + position;
    return this.getHeap().heap[pos];
  }

  // private final void checkTypeSubsumptionAt(int fsRef, int typeCode) {
  // if (!this.svd.casMetadata.ts.subsumes(typeCode, ll_getFSRefType(fsRef))) {
  // throwAccessTypeError(fsRef, typeCode);
  // }
  // }

  private void throwAccessTypeError(int fsRef, int typeCode) {
    LowLevelException e = new LowLevelException(LowLevelException.ACCESS_TYPE_ERROR);
    e.addArgument(Integer.toString(fsRef));
    e.addArgument(Integer.toString(typeCode));
    e.addArgument(this.svd.casMetadata.ts.ll_getTypeForCode(typeCode).getName());
    e.addArgument(this.svd.casMetadata.ts.ll_getTypeForCode(ll_getFSRefType(fsRef)).getName());
    throw e;
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

  public final void checkArrayBounds(int fsRef, int pos, int length) {
    final int arrayLength = ll_getArraySize(fsRef);
    if ((pos < 0) || (length < 0) || ((pos + length) > arrayLength)) {
      LowLevelException e = new LowLevelException(LowLevelException.ARRAY_INDEX_LENGTH_OUT_OF_RANGE);
      e.addArgument(Integer.toString(pos));
      e.addArgument(Integer.toString(length));
      throw e;
    }
  }

  private final void checkNonArrayConditions(int fsRef, int typeCode, int featureCode) {
    checkFsRef(fsRef);
    // It is now safe to do this.
    final int domTypeCode = this.getHeap().heap[fsRef];
    checkLowLevelParams(fsRef, domTypeCode, typeCode, featureCode);
    checkFsRef(fsRef + this.svd.casMetadata.featureOffset[featureCode]);
  }

  private final void checkFsRefConditions(int fsRef, int featureCode) {
    checkFsRef(fsRef);
    final int domTypeCode = this.getHeap().heap[fsRef];
    checkLowLevelParams(fsRef, domTypeCode, featureCode);
    checkFsRan(featureCode);
    checkFsRef(fsRef + this.svd.casMetadata.featureOffset[featureCode]);
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
      checkFsRef(value);
    }
    ll_setRefArrayValue(fsRef, position, value);
  }

  @Override
public void ll_setIntArrayValue(int fsRef, int position, int value) {
    final int pos = getArrayStartAddress(fsRef) + position;
    this.getHeap().heap[pos] = value;
    if (this.svd.trackingMark != null) {
    	this.logFSUpdate(fsRef, pos, ModifiedHeap.FSHEAP, 1);
    }
  }

  @Override
public void ll_setFloatArrayValue(int fsRef, int position, float value) {
    final int pos = getArrayStartAddress(fsRef) + position;
    this.getHeap().heap[pos] = float2int(value);
    if (this.svd.trackingMark != null) {
    	this.logFSUpdate(fsRef, pos,ModifiedHeap.FSHEAP, 1);
    }
  }

  @Override
public void ll_setStringArrayValue(int fsRef, int position, String value) {
    final int pos = getArrayStartAddress(fsRef) + position;
    final int stringCode = (value == null) ? NULL : addString(value);
    this.getHeap().heap[pos] = stringCode;
    if (this.svd.trackingMark != null) {
    	this.logFSUpdate(fsRef, pos, ModifiedHeap.FSHEAP, 1);
    }
  }

  @Override
public void ll_setRefArrayValue(int fsRef, int position, int value) {
    final int pos = getArrayStartAddress(fsRef) + position;
    this.getHeap().heap[pos] = value;
    if (this.svd.trackingMark != null) {
    	this.logFSUpdate(fsRef, pos, ModifiedHeap.FSHEAP, 1);
    }
  }

  @Override
public int ll_getFSRefType(int fsRef) {
    return this.getHeap().heap[fsRef];
  }

  @Override
public int ll_getFSRefType(int fsRef, boolean doChecks) {
    if (doChecks) {
      checkFsRef(fsRef);
      checkTypeAt(ll_getFSRefType(fsRef), fsRef);
    }
    return ll_getFSRefType(fsRef);
  }

  @Override
public LowLevelCAS getLowLevelCAS() {
    return this;
  }

  @Override
public int size() {
    return this.getHeap().heap.length * 6;
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
    this.svd.previousJCasClassLoader = classLoader;
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

    switchClassLoader(newClassLoader);
  }

  // switches ClassLoader but does not lock CAS
  public void switchClassLoader(ClassLoader newClassLoader) {
    if (null == newClassLoader) { // is null if no cl set
      return;
    }
    if (newClassLoader != this.svd.jcasClassLoader) {
      if (this.svd.jcasClassLoader != this.svd.previousJCasClassLoader) {
        /** Multiply nested classloaders not supported.  Original base loader: {0}, current nested loader: {1}, trying to switch to loader: {2}.*/
        throw new CASRuntimeException(CASRuntimeException.SWITCH_CLASS_LOADER_NESTED, 
                                      new Object[] {this.svd.previousJCasClassLoader, this.svd.jcasClassLoader, newClassLoader});
      }
      // System.out.println("Switching to new class loader");
      this.svd.jcasClassLoader = newClassLoader;
      if (null != this.jcas) {
        this.jcas.switchClassLoader(newClassLoader);
      }
    }
  }

  // internal use, public for cross-package ref
  public boolean usingBaseClassLoader() {
    return (this.svd.jcasClassLoader == this.svd.previousJCasClassLoader);
  }

  public void restoreClassLoaderUnlockCas() {
    // unlock CAS functions
    enableReset(true);
    // this might be called without the switch ever being called
    if (null == this.svd.previousJCasClassLoader) {
      return;
    }
    if (this.svd.previousJCasClassLoader != this.svd.jcasClassLoader) {
      // System.out.println("Switching back to previous class loader");
      this.svd.jcasClassLoader = this.svd.previousJCasClassLoader;
      if (null != this.jcas) {
        this.jcas.switchClassLoader(this.svd.previousJCasClassLoader);
      }
    }

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

  @Override
public ByteArrayFS createByteArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return (ByteArrayFS) createFS(ll_createByteArray(length));
  }

  @Override
public BooleanArrayFS createBooleanArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return (BooleanArrayFS) createFS(ll_createBooleanArray(length));
  }

  @Override
public ShortArrayFS createShortArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return (ShortArrayFS) createFS(ll_createShortArray(length));
  }

  @Override
public LongArrayFS createLongArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return (LongArrayFS) createFS(ll_createLongArray(length));
  }

  @Override
public DoubleArrayFS createDoubleArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return (DoubleArrayFS) createFS(ll_createDoubleArray(length));
  }

  @Override
public byte ll_getByteValue(int fsRef, int featureCode) {
    return (byte) ll_getIntValue(fsRef, featureCode);
  }

  @Override
public byte ll_getByteValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, TypeSystemImpl.byteTypeCode, featureCode);
    }
    return ll_getByteValue(fsRef, featureCode);
  }

  @Override
public boolean ll_getBooleanValue(int fsRef, int featureCode) {
    return CASImpl.TRUE == ll_getIntValue(fsRef, featureCode);
  }

  @Override
public boolean ll_getBooleanValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, TypeSystemImpl.booleanTypeCode, featureCode);
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
      checkNonArrayConditions(fsRef, TypeSystemImpl.shortTypeCode, featureCode);
    }
    return ll_getShortValue(fsRef, featureCode);
  }

  public long ll_getLongValue(int offset) {
    return this.getLongHeap().getHeapValue(offset);
  }

  @Override
public long ll_getLongValue(int fsRef, int featureCode) {
    final int offset = this.getHeap().heap[fsRef + this.svd.casMetadata.featureOffset[featureCode]];
    long val = this.getLongHeap().getHeapValue(offset);
    return (val);
  }

  public long ll_getLongValueFeatOffset(int fsRef, int featureOffset) {
    final int offset = this.getHeap().heap[fsRef + featureOffset];
    long val = this.getLongHeap().getHeapValue(offset);
    return (val);
  }

  @Override
public long ll_getLongValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, TypeSystemImpl.longTypeCode, featureCode);
    }
    return ll_getLongValue(fsRef, featureCode);
  }

  @Override
public double ll_getDoubleValue(int fsRef, int featureCode) {
    final int offset = this.getHeap().heap[fsRef + this.svd.casMetadata.featureOffset[featureCode]];
    long val = this.getLongHeap().getHeapValue(offset);
    return Double.longBitsToDouble(val);
  }
  
  public double ll_getDoubleValueFeatOffset(int fsRef, int featureOffset) {
    final int offset = this.getHeap().heap[fsRef + featureOffset];
    long val = this.getLongHeap().getHeapValue(offset);
    return Double.longBitsToDouble(val);
  }


  @Override
public double ll_getDoubleValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, TypeSystemImpl.doubleTypeCode, featureCode);
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
      checkNonArrayConditions(fsRef, TypeSystemImpl.booleanTypeCode, featureCode);
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
      checkNonArrayConditions(fsRef, TypeSystemImpl.byteTypeCode, featureCode);
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
      checkNonArrayConditions(fsRef, TypeSystemImpl.booleanTypeCode, featureCode);
    }
    ll_setShortValue(fsRef, featureCode, value);
  }

  @Override
public void ll_setLongValue(int fsRef, int featureCode, long value) {
    final int offset = this.getLongHeap().addLong(value);
    setFeatureValue(fsRef, featureCode, offset);
  }

  @Override
public void ll_setLongValue(int fsRef, int featureCode, long value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, TypeSystemImpl.longTypeCode, featureCode);
    }
    ll_setLongValue(fsRef, featureCode, value);
  }

  @Override
public void ll_setDoubleValue(int fsRef, int featureCode, double value) {
    long val = Double.doubleToLongBits(value);
    final int offset = this.getLongHeap().addLong(val);
    setFeatureValue(fsRef, featureCode, offset);
  }

  @Override
public void ll_setDoubleValue(int fsRef, int featureCode, double value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, TypeSystemImpl.doubleTypeCode, featureCode);
    }
    ll_setDoubleValue(fsRef, featureCode, value);
  }

  @Override
public byte ll_getByteArrayValue(int fsRef, int position) {
    final int pos = this.getHeap().heap[getArrayStartAddress(fsRef)];
    return this.getByteHeap().getHeapValue(pos + position);
  }

  @Override
public byte ll_getByteArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, TypeSystemImpl.byteArrayTypeCode, position);
    }
    return ll_getByteArrayValue(fsRef, position);
  }

  @Override
public boolean ll_getBooleanArrayValue(int fsRef, int position) {
    final int pos = this.getHeap().heap[getArrayStartAddress(fsRef)];
    return CASImpl.TRUE == this.getByteHeap().getHeapValue(pos + position);
  }

  @Override
public boolean ll_getBooleanArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, TypeSystemImpl.booleanArrayTypeCode, position);
    }
    return ll_getBooleanArrayValue(fsRef, position);
  }

  @Override
public short ll_getShortArrayValue(int fsRef, int position) {
    final int pos = this.getHeap().heap[getArrayStartAddress(fsRef)];
    return this.getShortHeap().getHeapValue(pos + position);
  }

  @Override
public short ll_getShortArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, TypeSystemImpl.shortArrayTypeCode, position);
    }
    return ll_getShortArrayValue(fsRef, position);
  }

  @Override
public long ll_getLongArrayValue(int fsRef, int position) {
    final int pos = this.getHeap().heap[getArrayStartAddress(fsRef)];
    return this.getLongHeap().getHeapValue(pos + position);
  }

  @Override
public long ll_getLongArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, TypeSystemImpl.longArrayTypeCode, position);
    }
    return ll_getLongArrayValue(fsRef, position);
  }

  @Override
public double ll_getDoubleArrayValue(int fsRef, int position) {
    final int pos = this.getHeap().heap[getArrayStartAddress(fsRef)];
    long val = this.getLongHeap().getHeapValue(pos + position);
    return Double.longBitsToDouble(val);
  }

  @Override
public double ll_getDoubleArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, TypeSystemImpl.doubleArrayTypeCode, position);
    }
    return ll_getDoubleArrayValue(fsRef, position);
  }

  @Override
public void ll_setByteArrayValue(int fsRef, int position, byte value) {
    final int offset = this.getHeap().heap[getArrayStartAddress(fsRef)];
    this.getByteHeap().setHeapValue(value, offset + position);
    if (this.svd.trackingMark != null) {
    	this.logFSUpdate(fsRef, offset+position, ModifiedHeap.BYTEHEAP, 1);
    }
  }

  @Override
public void ll_setByteArrayValue(int fsRef, int position, byte value, boolean doTypeChecks) {

    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, TypeSystemImpl.byteArrayTypeCode, position);
    }
    ll_setByteArrayValue(fsRef, position, value);
  }

  @Override
public void ll_setBooleanArrayValue(int fsRef, int position, boolean b) {
    byte value = (byte) (b ? CASImpl.TRUE : CASImpl.FALSE);
    final int offset = this.getHeap().heap[getArrayStartAddress(fsRef)];
    this.getByteHeap().setHeapValue(value, offset + position);
    if (this.svd.trackingMark != null) {
      this.logFSUpdate(fsRef, offset+position, ModifiedHeap.BYTEHEAP, 1);
    }
  }

  @Override
public void ll_setBooleanArrayValue(int fsRef, int position, boolean value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, TypeSystemImpl.booleanArrayTypeCode, position);
    }
    ll_setBooleanArrayValue(fsRef, position, value);
  }

  @Override
public void ll_setShortArrayValue(int fsRef, int position, short value) {
    final int offset = this.getHeap().heap[getArrayStartAddress(fsRef)];
    this.getShortHeap().setHeapValue(value, offset + position);
    if (this.svd.trackingMark != null) {
      this.logFSUpdate(fsRef, offset+position, ModifiedHeap.SHORTHEAP, 1);
    }
  }

  @Override
public void ll_setShortArrayValue(int fsRef, int position, short value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, TypeSystemImpl.shortArrayTypeCode, position);
    }
    ll_setShortArrayValue(fsRef, position, value);
  }

  @Override
public void ll_setLongArrayValue(int fsRef, int position, long value) {
    final int offset = this.getHeap().heap[getArrayStartAddress(fsRef)];
    this.getLongHeap().setHeapValue(value, offset + position);
    if (this.svd.trackingMark != null) {
      this.logFSUpdate(fsRef, offset+position, ModifiedHeap.LONGHEAP, 1);
    }
  }

  @Override
public void ll_setLongArrayValue(int fsRef, int position, long value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, TypeSystemImpl.longArrayTypeCode, position);
    }
    ll_setLongArrayValue(fsRef, position, value);
  }

  @Override
public void ll_setDoubleArrayValue(int fsRef, int position, double d) {
    final int offset = this.getHeap().heap[getArrayStartAddress(fsRef)];
    long value = Double.doubleToLongBits(d);
    this.getLongHeap().setHeapValue(value, offset + position);
    if (this.svd.trackingMark != null) {
      this.logFSUpdate(fsRef, offset+position, ModifiedHeap.LONGHEAP, 1);
    }

  }

  @Override
public void ll_setDoubleArrayValue(int fsRef, int position, double value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, TypeSystemImpl.doubleArrayTypeCode, position);
    }
    ll_setDoubleArrayValue(fsRef, position, value);
  }

  public boolean isAnnotationType(Type t) {
    return getTypeSystem().subsumes(getAnnotationType(), t);
  }

  /**
   * @param t the type code to test
   * @return true if that type is subsumed by AnnotationBase type
   */
  public boolean isSubtypeOfAnnotationBaseType(int t) {
    return this.svd.casMetadata.ts.subsumes(TypeSystemImpl.annotBaseTypeCode, t);
  }

  @Override
public AnnotationFS createAnnotation(Type type, int begin, int end) {
    if (this == this.svd.baseCAS) {
      // Can't create annotation on base CAS
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INVALID_BASE_CAS_METHOD,
          new String[] { "createAnnotation(Type, int, int)" });
      throw e;
    }
    FeatureStructure fs = createFS(type);
    final int addr = ll_getFSRef(fs);
    // setSofaFeat(addr, this.mySofaRef); // already done by createFS
    setFeatureValueNotJournaled(addr, TypeSystemImpl.startFeatCode, begin); // because it's a create - not in index
    // setStartFeat(addr, begin);
    setFeatureValueNotJournaled(addr, TypeSystemImpl.endFeatCode, end); // because it's a create - not in index
    // setEndFeat(addr, end);
    return (AnnotationFS) fs;
  }
  
  public int ll_createAnnotation(int typeCode, int begin, int end) {
    int addr = ll_createFSAnnotCheck(typeCode);
    setFeatureValueNotJournaled(addr, TypeSystemImpl.startFeatCode, begin); // because it's a create - not in index
    // setStartFeat(addr, begin);
    setFeatureValueNotJournaled(addr, TypeSystemImpl.endFeatCode, end); // because it's a create - not in index
    return addr;
  }
  
  /**
   * The generic spec T extends AnnotationFS (rather than AnnotationFS) allows the method
   * JCasImpl getAnnotationIndex to return Annotation instead of AnnotationFS
   * @param <T> the Java class associated with the annotation index
   * @return the annotation index
   */
  @Override
public <T extends AnnotationFS> AnnotationIndex<T> getAnnotationIndex() {
    return new AnnotationIndexImpl<T>(
            getIndexRepository().<T>getIndex(
             CAS.STD_ANNOTATION_INDEX));
  }

  /**
   * @see org.apache.uima.cas.CAS#getAnnotationIndex(Type)
   */
  @Override
public <T extends AnnotationFS> AnnotationIndex<T> getAnnotationIndex(Type type) {
    return new AnnotationIndexImpl<T>(
             getIndexRepository().<T>getIndex(
            CAS.STD_ANNOTATION_INDEX, type));
  }

  /**
   * @see org.apache.uima.cas.CAS#getAnnotationType()
   */
  @Override
public Type getAnnotationType() {
    return this.svd.casMetadata.ts.annotType;
  }

  /**
   * @see org.apache.uima.cas.CAS#getEndFeature()
   */
  @Override
public Feature getEndFeature() {
    return this.svd.casMetadata.ts.endFeat;
  }

  /**
   * @see org.apache.uima.cas.CAS#getBeginFeature()
   */
  @Override
public Feature getBeginFeature() {
    return this.svd.casMetadata.ts.startFeat;
  }

  private <T extends AnnotationFS> T createDocumentAnnotation(int length) {
    final TypeSystemImpl ts = this.svd.casMetadata.ts;
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
    return this.<T>ll_getFSForRef(ll_createDocumentAnnotation(length));
  }
  
  public int ll_createDocumentAnnotation(int length) {
    final int fsRef = ll_createDocumentAnnotationNoIndex(0, length);
    ll_getIndexRepository().ll_addFS(fsRef);
    return fsRef;
  }
  
  public int ll_createDocumentAnnotationNoIndex(int begin, int end) {
    final TypeSystemImpl ts = this.svd.casMetadata.ts;
    int fsRef = ll_createAnnotation(ts.docType.getCode(), begin, end);
    ll_setStringValue(fsRef, ts.langFeat.getCode(), CAS.DEFAULT_LANGUAGE_NAME);
    return fsRef;
  }

  // For the "built-in" instance of Document Annotation, set the
  // "end" feature to be the length of the sofa string
  public void updateDocumentAnnotation() {
    if (!mySofaIsValid() || this == this.svd.baseCAS) {
      return;
    }
    String newDoc = ll_getSofaDataString(this.mySofaRef);
    if (null != newDoc) {
      final int docAnnot = ll_getDocumentAnnotation();
      if (docAnnot != 0) {
        boolean wasRemoved = this.removeFromCorruptableIndexAnyViewSetCache(docAnnot, this.getAddbackSingle());
        setFeatureValueNoIndexCorruptionCheck(docAnnot, TypeSystemImpl.endFeatCode, newDoc.length());
        if (wasRemoved) {
          this.addbackSingle(docAnnot);
        } else {
          this.svd.fsTobeAddedbackSingleInUse = false;
        }
      } else {
        // not in the index (yet)
        createDocumentAnnotation(newDoc.length());
      }
    }
  }
  
  /**
   * Generic issue:  The returned document annotation could be either an instance of 
   *   DocumentAnnotation or an instance of AnnotationImpl - the Java cover class used for 
   *   annotations when JCas is not being used.
   */
  @Override
public <T extends AnnotationFS> T getDocumentAnnotation() {
    if (this == this.svd.baseCAS) {
      // base CAS has no document
      return null;
    }
    FSIterator<T> it = this.<T>getAnnotationIndex(this.svd.casMetadata.ts.docType).iterator();
    if (it.isValid()) {
      return it.get();
    }
    return createDocumentAnnotation(0);
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
    LowLevelIterator it = ll_getIndexRepository().
                            ll_getIndex(CAS.STD_ANNOTATION_INDEX, this.svd.casMetadata.ts.docType.getCode()).
                              ll_iterator();
    if (it.isValid()) {
      return it.ll_get();
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
    if (mySofaIsValid()) {
      return ll_getSofaDataString(this.mySofaRef);
    }
    return null;
  }

  @Override
public FeatureStructure getSofaDataArray() {
    if (this == this.svd.baseCAS) {
      // base CAS has no Sofa
      return null;
    }
    if (mySofaIsValid()) {
      return ll_getFSForRef(ll_getRefValue(mySofaRef, TypeSystemImpl.sofaArrayFeatCode));
//      return this.getSofa(this.mySofaRef).getLocalFSData();
    }
    return null;
  }

  @Override
public String getSofaDataURI() {
    if (this == this.svd.baseCAS) {
      // base CAS has no Sofa
      return null;
    }
    if (mySofaIsValid()) {
      return this.getSofa(this.mySofaRef).getSofaURI();
    }
    return null;
  }

  @Override
public InputStream getSofaDataStream() {
    if (this == this.svd.baseCAS) {
      // base CAS has no Sofa nothin
      return null;
    }
    return this.getSofaDataStream(this.getSofa());
  }

  @Override
public String getSofaMimeType() {
    if (this == this.svd.baseCAS) {
      // base CAS has no Sofa
      return null;
    }
    if (mySofaIsValid()) {
      return this.getSofa(this.mySofaRef).getSofaMime();
    }
    return null;
  }

  @Override
public SofaFS getSofa() {
    if (this.mySofaRef > 0) {
      return getSofa(this.mySofaRef);
    }
    return null;
  }
  
  /**
   * @return the addr of the sofaFS associated with this view, or 0
   */
  @Override
public int ll_getSofa() {
    return (mySofaRef > 0) ? mySofaRef : 0;
  }

  @Override
public String getViewName() {
    if (this == getViewFromSofaNbr(1)) {
      return CAS.NAME_DEFAULT_SOFA;
    } else if (this.mySofaRef > 0) {
      return ll_getSofaID(mySofaRef);
//      return this.getSofa().getSofaID();
    } else {
      return null;
    }
  }

  private boolean mySofaIsValid() {
    return this.mySofaRef > 0;
  }

  void setDocTextFromDeserializtion(String text) {
    if (mySofaIsValid()) {
      final int SofaStringCode = ll_getTypeSystem().ll_getCodeForFeature(
          this.getTypeSystem().getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFASTRING));
      ll_setStringValue(this.getSofaRef(), SofaStringCode, text);
    }
  }

  @Override
public void setDocumentLanguage(String languageCode) {
    if (this == this.svd.baseCAS) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INVALID_BASE_CAS_METHOD,
          new String[] { "setDocumentLanguage(String)" });
      throw e;
    }
    // LowLevelCAS llc = getLowLevelCAS();
    LowLevelCAS llc = this;
    final int docAnnotAddr = llc.ll_getFSRef(getDocumentAnnotation());
    languageCode = Language.normalize(languageCode);
    llc.ll_setStringValue(docAnnotAddr, TypeSystemImpl.langFeatCode, languageCode);
  }

  @Override
public void setDocumentText(String text) {
    if (this == this.svd.baseCAS) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INVALID_BASE_CAS_METHOD,
          new String[] { "setDocumentText(String)" });
      throw e;
    }
    setSofaDataString(text, "text");
  }

  @Override
public void setSofaDataString(String text, String mime) throws CASRuntimeException {
    if (this == this.svd.baseCAS) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INVALID_BASE_CAS_METHOD,
          new String[] { "setDocumentText(String)" });
      throw e;
    }

    if (!mySofaIsValid()) {
      this.createInitialSofa(null);
    }
    // try to put the document into the SofaString ...
    // ... will fail if previously set
    getSofa(this.mySofaRef).setLocalSofaData(text);
    ll_setStringValue(this.mySofaRef, TypeSystemImpl.sofaMimeFeatCode,
        mime);
  }

  @Override
public void setSofaDataArray(FeatureStructure array, String mime) throws CASRuntimeException {
    if (this == this.svd.baseCAS) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INVALID_BASE_CAS_METHOD,
          new String[] { "setSofaDataArray(FeatureStructure, mime)" });
      throw e;
    }
    if (!mySofaIsValid()) {
      this.createInitialSofa(null);
    }
    // try to put the document into the SofaString ...
    // ... will fail if previously set
    getSofa(this.mySofaRef).setLocalSofaData(array);
    ll_setStringValue(this.mySofaRef, TypeSystemImpl.sofaMimeFeatCode,
        mime);
  }

  @Override
public void setSofaDataURI(String uri, String mime) throws CASRuntimeException {
    if (this == this.svd.baseCAS) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INVALID_BASE_CAS_METHOD,
          new String[] { "setSofaDataURI(String, String)" });
      throw e;
    }
    if (!mySofaIsValid()) {
      this.createInitialSofa(null);
    }
    // try to put the document into the SofaString ...
    // ... will fail if previously set
    getSofa(this.mySofaRef).setRemoteSofaURI(uri);
    ll_setStringValue(this.mySofaRef, TypeSystemImpl.sofaMimeFeatCode,
        mime);
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
   * @param addr the heap address of a feature structure which is a subtype of AnnotationBase
   * @return the view associated with this FS where it could be indexed
   */
  @Override
public CASImpl ll_getSofaCasView(int addr) {
    // if the sofa feature for this annotation is not 0
    int addrOfSofaFS = this.getSofaFeat(addr);
    if (addrOfSofaFS > 0) {
      // check if same as that of current CAS view
      // if (!(this instanceof CASImpl) || (sofaId != ((CASImpl)
      // this).getSofaRef())) {
      if (addrOfSofaFS != this.getSofaRef()) {
        // Does not match. get CAS for the sofaRef feature found in
        // annotation
            // getSofaRef gets the feature which is the dense sofa number
        return (CASImpl) this.getView(ll_getSofaNum(addrOfSofaFS));
      }
    } else {// annotation created from low-level APIs, without setting sofa
      // feature
      // Ignore it for backwards compatibility
    }
    return this;
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
  
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.CAS#getViewIterator()
   */
  @Override
public Iterator<CAS> getViewIterator() {
    return new Iterator<CAS>() {
      
      final CASImpl initialView = (CASImpl) getInitialView();  // creates one if not existing, w/o sofa  

      boolean testInitView = !initialView.mySofaIsValid(); // true if has no Sofa in initial view
                                                           //     but is reset to false once iterater moves
                                                           //     off of initial view.
      
                                                           // if initial view has a sofa, we just use the 
                                                           // sofa iterator instead.

      final FSIterator<SofaFS> sofaIter = getSofaIterator(); 

      @Override
      public boolean hasNext() {
        if (testInitView) {
          return true;
        }
        return sofaIter.hasNext(); 
      }

      @Override
      public CAS next() {
        if (testInitView) {
          testInitView = false;
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
    FSIterator<SofaFS> sofaIter = getSofaIterator();
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
  
  public final boolean doUseJcasCache() {
    return this.isUsedJcasCache;
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
          FSsTobeAddedback toAddBack = s.remove(i);
          toAddBack.addback();
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
  	this.svd.trackingMark = new MarkerImpl(this.getHeap().getNextId(), 
  			this.getStringHeap().getSize(),
  			this.getByteHeap().getSize(),
  			this.getShortHeap().getSize(),
  			this.getLongHeap().getSize(),
  			this);
  	if (this.svd.modifiedPreexistingFSs == null) {
  	  this.svd.modifiedPreexistingFSs = new IntVector();
  	} else {errorMultipleMarkers();}
  	if (this.svd.modifiedFSHeapCells == null) {
  	  this.svd.modifiedFSHeapCells = new IntVector();
  	} else {errorMultipleMarkers();}
  	if (this.svd.modifiedByteHeapCells == null) {
        this.svd.modifiedByteHeapCells = new IntVector();
  	} else {errorMultipleMarkers();}
  	if (this.svd.modifiedShortHeapCells == null) { 
        this.svd.modifiedShortHeapCells = new IntVector();
  	} else {errorMultipleMarkers();}
  	if (this.svd.modifiedLongHeapCells == null) {
        this.svd.modifiedLongHeapCells = new IntVector();
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
  
  private void logFSUpdate(int fsaddr, int position, ModifiedHeap whichheap, int howmany) {
  	if (this.svd.trackingMark != null && !this.svd.trackingMark.isNew(fsaddr)) {
  	  //log the FS
  	  int lastModifiedFS = -1;	
  	  if (this.svd.modifiedPreexistingFSs.size() > 0) {
  	    lastModifiedFS =  this.svd.modifiedPreexistingFSs.get(this.svd.modifiedPreexistingFSs.size()-1);
  	  }
  	  //only log if the last one logged is not the same fs.s
  	  if (lastModifiedFS != fsaddr) {
  		this.svd.modifiedPreexistingFSs.add(fsaddr);
  	  }	
  	  //log cells that were updated
  	  switch (whichheap) {  
    		case FSHEAP:
    		  for (int i=0; i < howmany;i++) {
    		    this.svd.modifiedFSHeapCells.add(position+i);
    		  }
    		break;
    		case BYTEHEAP:
    		  for (int i=0; i < howmany;i++) {
    		    this.svd.modifiedByteHeapCells.add(position+i);
    		  }
    		break;
    		case SHORTHEAP:
    		  for (int i=0; i < howmany;i++) {
    		    this.svd.modifiedShortHeapCells.add(position+i);
    		  }
    	    break;
    		case LONGHEAP:
    		  for (int i=0; i < howmany;i++) {
    		    this.svd.modifiedLongHeapCells.add(position+i);
    		  }
    	    break;
    	}
  	}
  }
  
  // made public https://issues.apache.org/jira/browse/UIMA-2478
  public MarkerImpl getCurrentMark() {
	  return this.svd.trackingMark;
  }
  
  IntVector getModifiedFSList() {
	return this.svd.modifiedPreexistingFSs;  
  }
  
  IntVector getModifiedFSHeapAddrs() {
	return this.svd.modifiedFSHeapCells;  
  }
  
  IntVector getModifiedByteHeapAddrs() {
		return this.svd.modifiedByteHeapCells;  
  }
  
  IntVector getModifiedShortHeapAddrs() {
		return this.svd.modifiedShortHeapCells;  
  }
  
  IntVector getModifiedLongHeapAddrs() {
		return this.svd.modifiedLongHeapCells;  
  }

  @Override
  public String toString() {
    String sofa =  (mySofaRef == -1) ? "_InitialView" :
            (mySofaRef == 0) ? "no Sofa" :
                               getSofa(this.mySofaRef).getSofaID();
    return "CASImpl [view: " + sofa + "]";
  }
    
  int getCasResets() {
    return svd.casResets.get();
  }
  
  int getCasId() {
    return svd.casId;
  }
  
  /******************************************
   * DEBUGGING and TRACING
   *
   ******************************************/
  
  void traceFSCreate(FeatureStructureImpl fs) {
    StringBuilder b = svd.traceFScreationSb;
    if (b.length() > 0) {
      traceFSflush();
    }
    traceFSfs(fs);
    svd.traceFSisCreate = true;
    if (fs.getType().isArray()) {
      int arraySize = ll_getArraySize(fs.getAddress());
      b.append(" l:").append(arraySize);
    }
    if (fs.getAddress() > 190000 && fs.getAddress() < 200000 && fs.getType().getShortName().equals("Passage")) {
      traceOut.println("Passage callback: " + MiscImpl.getCallers(3, 10));
    }
  }
  
  void traceFSfs(FeatureStructureImpl fs) {
    StringBuilder b = svd.traceFScreationSb;
    svd.traceFSid = fs.getAddress();
    b.append("c:").append(String.format("%-3d", getCasId()));
    String viewName = fs.getCAS().getViewName();
    if (null == viewName) {
      viewName = "base";
    }
    b.append(" v:").append(MiscImpl.elide(viewName, 8));
    b.append(" i:").append(String.format("%-5s", fs.getAddress()));
    b.append(" t:").append(MiscImpl.elide(fs.getType().getShortName(), 10));    
  }
  
  void traceFSfeat(FeatureStructure fs, FeatureImpl fi, int v) {
    StringBuilder b = svd.traceFScreationSb;
//    assert (b.length() > 0);  // may be 0 if fs created via deserialization
    FeatureStructureImpl fsi = (FeatureStructureImpl) fs;
    if (fsi.getAddress() != svd.traceFSid) {
      traceFSfeatUpdate(fsi);
    }
    String fn = fi.getShortName();
    String fv = getTraceRepOfObj(fi, v);
    int i_v = Math.max(0, 10 - fn.length());
    int i_n = Math.max(0,  10 - fv.length());
    
    fn = MiscImpl.elide(fn, 10 + i_n, false);
    fv = MiscImpl.elide(fv, 10 + i_v, false);
    
    // debug
//    if (!svd.traceFSisCreate && fn.equals("uninf.dWord") && fv.equals("XsgTokens")) {
//      traceOut.println("debug uninf.dWord:XsgTokens: updated by " + MiscImpl.getCallers(3, 10));
//    }
    b.append(' ').append(MiscImpl.elide(fn + ':' + fv, 21));
  }
  
  private String getTraceRepOfObj( FeatureImpl fi, int v) {
    TypeImpl range = (TypeImpl) fi.getRange();
    int rangeCode = range.getCode();
    if (ll_isRefType(rangeCode)) {
      if (v == 0) return "null";
//      FeatureStructureImpl fs = ll_getFSForRef(v);
      Type type = getTypeSystemImpl().ll_getTypeForCode(getTypeCode(v));
      return ((type == null) ? "unknwn" 
                             : MiscImpl.elide(type.getShortName(), 5, false))
              + ':' + v;
    } else if (isStringType(rangeCode)){
      String s = this.getStringHeap().getStringForCode(v);
      s = MiscImpl.elide(s, 50, false);
      return MiscImpl.replaceWhiteSpace(s, "_");
    } else if (isFloatType(rangeCode)) {
      return Float.toString(int2float(v));
    } else if (isBooleanType(rangeCode)) {
      return (v == 1) ? "true" : "false";
    } else if (isLongType(rangeCode)) {
      return Long.toString(this.getLongHeap().getHeapValue(v));
    } else if (isDoubleType(rangeCode)) {
      return Double.toString(long2double(this.getLongHeap().getHeapValue(v)));
    } else {
      return Integer.toString(v);
    }
  }
  
  void traceFSfeatUpdate(FeatureStructureImpl fs) {
    traceFSflush();
    traceFSfs(fs);
    svd.traceFSisCreate = false; 
  }
  
  StringBuilder traceFSflush() {
    StringBuilder b = svd.traceFScreationSb;
//    assert (b.length() > 0);  // may be 0 if fs created via deserialization
    if (b.length() > 0) {
      traceOut.println((svd.traceFSisCreate ? "cr: " :
                                              "up: ") + b);
      b.setLength(0);
    }
    return b;
  }
  
  static {

    // this is definitely needed
    if (traceFSs) {
      Runtime.getRuntime().addShutdownHook(new Thread(null, new Runnable() {
          @Override
          public void run() {
            System.out.println("closing traceOut");
            traceOut.close();
          }
        }, "close trace output"));
    }
  }
  
  
}
