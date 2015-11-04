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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.LinearTypeOrder;
import org.apache.uima.cas.admin.LinearTypeOrderBuilder;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.ObjHashSet;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.Misc;

/**
 * There is one instance of this class per CAS View.
 * 
 * Some parts of the data here are shared between all views of a CAS.
 * 
 * Many things refer to specific types, and their associated Java Cover classes.
 *    Java impl classes are always used for each type; 
 *        If there is no JCas cover class defined for a type, then
 *        the most specific superclass which has a JCas defined class is used;
 *        this is the class TOP or one of its subclasses.
 *        
 *
 * Generic typing: 
 *   User facing APIs can make use of the (JCas) Java cover types, for indexes and iterators over them
 *   The general generic type used is typically written here as T extends FeatureStructure, where
 *   FeatureStructure is the super interface of all JCas types.  
 *
 *   APIs having no reference to Java cover types (i.e., low level iterators) are not generic, unless they
 *   are needed to be to pass along the associated type to other APIs. 
 */
public class FSIndexRepositoryImpl implements FSIndexRepositoryMgr, LowLevelIndexRepository {

  private final static boolean DEBUG = false;
  
  public final static boolean ITEM_ADDED_TO_INDEX = true;
  public final static boolean ITEM_REMOVED_FROM_INDEX = false;
  /**
   * The default size of an index.
   */
  public static final int DEFAULT_INDEX_SIZE = 16;

  /**
   * flag used when removing FSs due to corruption avoidance
   */
  public static final boolean SKIP_BAG_INDEXES = true;
  public static final boolean INCLUDE_BAG_INDEXES = false;
  
  /**
   * Define this JVM property to allow adding the same identical FS to Set and Sorted indexes more than once.  
   */
  public static final String ALLOW_DUP_ADD_TO_INDEXES = "uima.allow_duplicate_add_to_indexes";
  static { 
    if (Misc.getNoValueSystemProperty(ALLOW_DUP_ADD_TO_INDEXES)) {
      throw new CASAdminException(CASAdminException.INDEX_DUPLICATES_NOT_SUPPORTED);
    }
  }
  
  public static final String DISABLE_ENHANCED_WRONG_INDEX = "uima.disable_enhanced_check_wrong_add_to_index";
 
  private static final boolean IS_DISABLE_ENHANCED_WRONG_INDEX_CHECK = Misc.getNoValueSystemProperty(DISABLE_ENHANCED_WRONG_INDEX);
      
  // Implementation note: the use of equals() here is pretty hairy and
  // should probably be fixed. We rely on the fact that when two
  // FSIndexComparators are compared, the type of the comparators is
  // ignored! A fix for this would be to split the FSIndexComparator
  // class into two classes, one for the key-comparator pairs, and one
  // for the combination of the two. Note also that we compare two
  // FsIndex_iicps by comparing their
  // index.getComparator()s.


  /**
   * The index repository holds instances of defined and built-in indexes.
   * 
   * Indexes implement Java's NavigableSet ( *** do this later ??? may be a bit of work to make work over subtypes )
   * 
   * There are various kinds of iterators that can be obtained:
   *   Iterator - plain Java iterator - goes forward only
   *   FSIterator - an extension that can go in either direction, and can locate a position via an FS
   * 
   *   Low-level iterators return internal "ints" representing FS ids; these are only for backwards compatibility   
   *   IntPointerIterator - for backwards compatibility - a wrapper around FSIterator, plus inc() dec()
   *     - may be dropped if only internally used
   *   LowLevelIterator - for backwards compatibility - a wrapper around FSIterator, plus ll_get, ll_indexSize, ll_getIndex
   * 
   * To obtain normal iterators, use the FSIndex methods 
   *   iterator
   *   iterator(FeatureStructure) - to initially position to the FS)
   *   index.withSnapshotIterators() - to get snapshot versions of the indexes to iterate over   
   *    
   * To get the low level iterators, 
   *   get a low-level indexrepository (CAS -> getLowLevelCAS -> get low-level index repository), and from there
   *     get a low-level index
   *       get a low-level iterator
   */
  
  /**
   * General FSIterator creation
   * 
   *   There are some alternatives
   *     If the index has subtypes, then 
   *       - the iterator may be ordered/unordered.  
   *         - Ordered ones select among the subtypes as needed.
   *         - Unordered ones return all elements from each subtype and then switch to the next subtype.
   *           This is for efficiency.
   *       - (currently disabled) flattened indexes may be created and used    
   *     If the index says to use snapshots, then each call makes a snapshot of the index, and iterates over that.
   *       - these don't throw ConcurrentModificationExceptions.
   */  
   
  /*************************************************************
   * Information about indexes that is shared across all views *
   *************************************************************/
  private static class SharedIndexInfo {
    
    private LinearTypeOrderBuilder defaultOrderBuilder = null;

    private LinearTypeOrder defaultTypeOrder = null;
    
    // A reference to the type system.
    private final TypeSystemImpl tsi;
    
    /**
     * lazily created comparator using the built-in annotation index
     */
    private Comparator<AnnotationFS> annotationFsComparator = null;
    
    /**
     * optimization only - bypasses some shared (among views) initialization if already done
     */
    private boolean isSetUpFromBaseCAS = false;
    
    SharedIndexInfo(TypeSystemImpl typeSystem) {
      this.tsi = typeSystem;
    }
  }
  
  /**
   * For processing index updates in batch mode when deserializing from a remote service;
   * lists of FSs that were added, removed, or reindexed
   * 
   *  only used when processing updates in batch mode
   */
  private static class ProcessedIndexInfo {
    final private ObjHashSet<FeatureStructureImplC> fsAddedToIndex     = new ObjHashSet<>(FeatureStructureImplC.class);
    final private ObjHashSet<FeatureStructureImplC> fsDeletedFromIndex = new ObjHashSet<>(FeatureStructureImplC.class); 
    final private ObjHashSet<FeatureStructureImplC> fsReindexed        = new ObjHashSet<>(FeatureStructureImplC.class);
  }
  
  /**
   * Information about all the indexes for a single type.  This is kept in an 
   * List, with the key being the type code.
   */
  static class IndexesForType {
    /**
     * true if one or more of the indexes is a set index
     */
    boolean hasSetIndex; 
    /**
     * index of any sorted index or -1 if no sorted index
     */
    int aSortedIndex = -1;   // -1 or the position of an arbitrary sorted index
    int aBagIndex = -1;      // -1 or the position of an arbitrary bag index
    ArrayList<FsIndex_iicp<FeatureStructure>> indexesForType = new ArrayList<>(1); 
    
    FsIndex_iicp<FeatureStructure> getNonSetIndex() {
      if (aSortedIndex < 0 && aBagIndex < 0) { // index is empty!
        return null;
      }
      return indexesForType.get((aBagIndex >= 0) ? aBagIndex : aSortedIndex);
    }
    
    void add(FsIndex_iicp<FeatureStructure> iicp) {
      final int kind = iicp.fsIndex_singletype.getIndexingStrategy();
      int i = indexesForType.size();
      switch (kind) {
      case FSIndex.BAG_INDEX:
        aBagIndex = i;
        break;
      case FSIndex.DEFAULT_BAG_INDEX:
        if (aBagIndex == -1) {  // real bag indexes have priority
          aBagIndex = i;
        }
        break;
      case FSIndex.SORTED_INDEX:
        aSortedIndex = i;
        break;
      }
      indexesForType.add(iicp);
    }
    
    
  }
  /*****  I N S T A N C E   V A R I A B L E S  *****/
  /*****           Replicated per view         *****/                 

  // A reference to the CAS View.
  private final CASImpl cas;

  // Is the index repository locked?
  private boolean locked = false;

  /** 
   * An array of information about defined indexes, one for each type in the type hierarchy. 
   *   - includes for each type, an unordered list of FsIndex_iicps for 
   *     that type, corresponding to the different index definitions over that type.
   * 
   * The key is the typecode of the type.
   */
  
  final IndexesForType[] indexArray;
  
  IndexesForType getIndexesForType(int typeCode) {
    return  indexArray[typeCode];
  }

  /** 
   * an array of ints, one for each type in the type hierarchy. 
   * Used to enable iterators to detect modifications (adds / removes) 
   * to indexes they're iterating over while they're iterating over them.
   * Not private so it can be seen by FSLeafIndexImpl
   */
  final int[] detectIllegalIndexUpdates;
  
  /**
   * A map from names to FsIndex_iicps, which represent the index at the
   * top-most type declared in the index specification.    
   * Different names may map to the same iicp.
   * The keys are the same across all views, but the values are different, per view
   */
  final HashMap<String, FsIndex_iicp<FeatureStructure>> name2indexMap;
 
  /**
   * speedup for annotation index accessing by type, lazily initialized
   */
  final private Map<TypeImpl, FsIndex_annotation<Annotation>> annotationIndexes = 
      new IdentityHashMap<TypeImpl, FsIndex_annotation<Annotation>>();

  // the next are for journaling updates to indexes
  final private List<FeatureStructure> indexUpdates;

  final private BitSet indexUpdateOperation;

  private boolean logProcessed;


  // Monitor indexes used to optimize getIndexedFS and flush
  // only used for faster access to next set bit
  final private IntVector usedIndexes;

  // one bit per typeCode, indexed by typeCode
  final private boolean[] isUsed;
  
  // Monitor which indexes are iterated over, to allow resetting flatIndexes
  final private List<FsIndex_iicp<? extends FeatureStructure>> iteratedSortedIndexes = 
      Collections.synchronizedList(new ArrayList<FsIndex_iicp<? extends FeatureStructure>>());
  
  private final SharedIndexInfo sii;

  private ProcessedIndexInfo mPii;
  
  /** ----------------------- Support for flattened indexes -----------------*/

  // this approach doesn't work, because an iterator over a subtype could do an invalid->valid transition
  //   while a flattened iterator over a supertype was in existence.
  //   Subsequent creations of new iterators over the supertype would not notice that the flattened iterator was invalid.
//  /** 
//   * FlattenedIndexValid 
//   *  
//   * <p>a BitSet, one per view, indexed by typeCode 
//   * A bit[i] being on means that a time window has begun (from the moment it is turned on) 
//   * where a flattened version if it exists) is valid, for index[i] and all its subtypes.
//   *      
//   * <p>Used at iterator creation time to see if a flattened multi-type sorted index needs to be discarded.</p>
//   * 
//   * <p>Bits are initially off.</p>
//   * 
//   * <p>Bit is turned on when a flattened index is successfully created for any index 
//   * which starts at that type; the flag is only set for the type the flattened index is created for (not its subtypes)</p> 
//   * 
//   * Bit is turned off for add/remove to/from index operation, for a type and all its super types.
//   * This is facilitated by having a bit set for each type of all its supertypes.    
//   * This insures that an upper level flattened index is invalidated, even if the lower level
//   * gets a new flattened index (and has its is valid bit is set).
//   * The reason for this is that any update to a subtype of a type having
//   * a flattened index causes that flattened index to become invalid.</p>
//   * 
//   * Multi-threading: Because BitSet is not safe for multithread use, all reading / writing done
//   * using itself as a synch lock.
//   */
//  final ConcurrentBits flattenedIndexValid;

//  boolean syncGetFlattenedIndexValid(int i) {
//    synchronized (flattenedIndexValid) {
//      return flattenedIndexValid.get(i);
//    }
//  }
  
  @SuppressWarnings("unused")
  private FSIndexRepositoryImpl() {
    this.cas = null;  // because it's final
    this.sii = null;
    this.name2indexMap = null;
    this.indexArray = null;
    this.detectIllegalIndexUpdates = null;
//    this.flattenedIndexValid = null;
    this.indexUpdates = null;
    this.indexUpdateOperation = null;
    this.usedIndexes = null;
    this.isUsed = null;
  }

  /**
   * Constructor.
   * Assumption: called first before next constructor call, with the base CAS view
   * 
   * @param cas
   */
  FSIndexRepositoryImpl(CASImpl cas) {
    this.cas = cas;
    this.sii = new SharedIndexInfo(cas.getTypeSystemImpl());

    final TypeSystemImpl ts = this.sii.tsi;
    // Type counting starts at 1.
    final int numTypes = ts.getNumberOfTypes() + 1;
    this.detectIllegalIndexUpdates = new int[numTypes];
//    this.flattenedIndexValid = new ConcurrentBits(numTypes);
    this.name2indexMap = new HashMap<String, FsIndex_iicp<FeatureStructure>>();
    this.indexUpdates = new ArrayList<>();
    this.indexUpdateOperation = new BitSet();
    this.logProcessed = false;
    this.indexArray = new IndexesForType[this.sii.tsi.getNumberOfTypes() + 1];
    this.usedIndexes = new IntVector();
    this.isUsed = new boolean[numTypes];
    init();
  }

  /**
   * Constructor for additional views.
   * 
   * @param cas
   * @param baseIndexRepository
   */
  FSIndexRepositoryImpl(CASImpl cas, FSIndexRepositoryImpl baseIndexRepo) {

    this.cas = cas;
    this.sii = baseIndexRepo.sii;
    sii.isSetUpFromBaseCAS = true;  // bypasses initialization already done

    final TypeSystemImpl ts = this.sii.tsi;
    // Type counting starts at 1.
    final int numTypes = ts.getNumberOfTypes() + 1;
    this.detectIllegalIndexUpdates = new int[numTypes];
//    this.flattenedIndexValid = new ConcurrentBits(numTypes);
    
    this.name2indexMap = new HashMap<String, FsIndex_iicp<FeatureStructure>>();
    this.indexUpdates = new ArrayList<>();
    this.indexUpdateOperation = new BitSet();
    this.logProcessed = false;
    this.indexArray = new IndexesForType[numTypes];
    this.usedIndexes = new IntVector();
    this.isUsed = new boolean[numTypes];

    init();
    // cant do this here because need to have the CAS's ref to this instance set before this is done.
//    baseIndexRepo.name2indexMap.keySet().stream().forEach(key -> createIndex(baseIndexRepo, key));
  }

  /**
   * Initialize data. Common initialization called from the constructors.
   */
  private void init() {
    final TypeSystemImpl ts = this.sii.tsi;
    
    /* **********************************************
     * for each type in the TypeSystem, 
     *   create a list of iicp's 
     *     each one corresponding to a defined index
     * **********************************************/
    final int numTypes = ts.getNumberOfTypes() + 1; // Type counting starts at 1.
    // Can't instantiate arrays of generic types, but this is ok for ArrayList.
    for (int i = 1; i < numTypes; i++) {
      this.indexArray[i] = new IndexesForType();
    }
    
    Arrays.fill(detectIllegalIndexUpdates, Integer.MIN_VALUE);
    mPii = new ProcessedIndexInfo();
  }

  /* ***************
   * Create indexes 
   * ***************/
   
  <T extends FeatureStructure> 
      void createIndex(FSIndexRepositoryImpl baseIndexRepo, String key) {
    final FsIndex_singletype<FeatureStructure> fsIndex = baseIndexRepo.name2indexMap.get(key).fsIndex_singletype;
    createIndexNoQuestionsAsked(fsIndex.getComparatorForIndexSpecs(), key, fsIndex.getIndexingStrategy());
  }

  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#createIndex(FSIndexComparator, String)
   */
  @Override
  public boolean createIndex(FSIndexComparator comp, String label, int indexType) throws CASAdminException {
    if (this.locked) {
      throw new CASAdminException(CASAdminException.REPOSITORY_LOCKED);
    }
    return createIndexNoQuestionsAsked(comp, label, indexType);
  }

  /**
   * This is public only until the xml specifier format supports specifying index kinds (set, bag
   * etc.).
   * 
   * @param comp -
   * @param label -
   * @param indexType -
   * @return -
   */
  public <T extends FeatureStructure> 
      boolean createIndexNoQuestionsAsked(final FSIndexComparator comp, String label, int indexType) {
    
    FsIndex_iicp<FeatureStructure> cp = this.name2indexMap.get(label);
   

    if (cp == null) {
      // Create new index
      cp = this.addNewIndexRecursive(comp, indexType);
      
      // create a set of feature codes that are in one or more index definitions,
      // only once for all cas views
      if (!sii.isSetUpFromBaseCAS) {        
        for (int i = 0, nKeys = comp.getNumberOfKeys(); i < nKeys; i++) {
          if (comp.getKeyType(i) == FSIndexComparator.FEATURE_KEY) {
            FeatureImpl fi = comp.getKeyFeature(i);
            cas.featureCodes_inIndexKeysAdd(fi.getCode(), fi.registryIndex);
          }
        }
      }
      
      this.name2indexMap.put(label, cp);
      return true;
    }
    
    // For now, just return false if the label already exists.
    return false;
    // // An index has previously been registered for this name. We need to
    // // compare the types to see if the new addition is compatible with the
    // // pre-existing one. There are three cases: the new type can be a sub-type
    // // of the old one, in which case we don't need to do anything; or, the
    // // new type is a super-type of the old one, in which case we add the new
    // // index while keeping the old one; or, there is no subsumption relation,
    // // in which case we can't add the index.
    // Type oldType = cp.index.getType(); // Get old type from the index.
    // Type newType = comp.getType(); // Get new type from comparator.
    // if (this.sii.typeSystem.subsumes(oldType, newType)) {
    // // We don't need to do anything.
    // return true;
    // } else if (this.sii.typeSystem.subsumes(newType, oldType)) {
    // // Add the index, subsuming the old one.
    // cp = this.addIndexRecursive(comp);
    // // Replace the old index with the new one in the map.
    // this.name2indexMap.put(label, cp);
    // return true;
    // } else {
    // // Can't add index under that name.
    // return false;
    // }
    // }
  }

  /**
   * Reset all indexes, in one view.
   */
  public void flush() {
    if (!this.locked) {
      return;
    }

//    if (DEBUG) {
//      System.out.println("Index Flush Top");
//    }
    // Do nothing really fast!
    if (this.usedIndexes.size() == 0) {
      return;
    }
    
    annotationIndexes.clear();
    
    for (int i = 0; i < usedIndexes.size(); i++) {
      int used = this.usedIndexes.get(i);
      isUsed[used] = false;
      indexArray[used].indexesForType.stream().forEach(iicp -> iicp.fsIndex_singletype.flush());
    }

    clearIteratedSortedIndexes();
    
    // reset the index update trackers
//    resetDetectIllegalIndexUpdates();
    
    this.indexUpdates.clear();
    this.indexUpdateOperation.clear();
    mPii = new ProcessedIndexInfo();
//    this.fsAddedToIndex = new IntSet();
//    this.fsDeletedFromIndex = new IntSet();
//    this.fsReindexed = new PositiveIntSet_impl();
    this.logProcessed = false;
    this.usedIndexes.removeAllElements();
  }
  
  // for now, with flattened index optimization disabled, this should be a no-op
  private void clearIteratedSortedIndexes() {
    int sz = iteratedSortedIndexes.size();
    if (DEBUG) {
      System.out.println("Index Flush flatIndex, size = " + sz);
    }
    
//    iteratedSortedIndexes.stream().forEach(iicp -> iicp.flatIndex.flush());

    if (DEBUG) {
      if (iteratedSortedIndexes.size() != sz) {
        throw new RuntimeException(
            "Index Flush flatIndex, size not the same, before = " + 
            sz + ", after = " + iteratedSortedIndexes.size());
      }
    }
    iteratedSortedIndexes.clear();
  }

  /* *****************************************
   * Adding indexes to the index repository
   *******************************************/
  
  private FsIndex_iicp<FeatureStructure> addNewIndex(FSIndexComparator comparator, int indexType) {
    return addNewIndex(comparator, DEFAULT_INDEX_SIZE, indexType);
  }

  /**
   * This is where the actual index gets created.
   */
  private <T extends FeatureStructure> FsIndex_iicp<T> addNewIndex(final FSIndexComparator comparator, int initialSize, int indexType) {
    FsIndex_iicp<T> iicp = null;
    if (isAnnotationIndex(comparator, indexType)) {
      FsIndex_singletype<Annotation> index = addNewIndexCore(comparator, initialSize, indexType);
      iicp = (FsIndex_iicp<T>) new FsIndex_annotation<Annotation>(index); 
    } else {
      FsIndex_singletype<FeatureStructure> index = addNewIndexCore(comparator, initialSize, indexType);
      iicp = (FsIndex_iicp<T>) new FsIndex_iicp<FeatureStructure>(index); 
    }
    final Type type = comparator.getType();
    final int typeCode = ((TypeImpl) type).getCode();

//    add indexes so that sorted ones are first, to benefit getAllIndexedFSs
//    if (indexType == FSIndex.SORTED_INDEX) {
//      this.indexArray[typeCode].add(0, iicp);  // shifts rest down
//    } else 
    this.indexArray[typeCode].add((FsIndex_iicp<FeatureStructure>) iicp);
//    }
    return iicp;
  }
  
  private boolean isAnnotationIndex(FSIndexComparator c, int indexKind) {
    TypeSystemImpl tsi = getTypeSystemImpl();
    return indexKind == FSIndex.SORTED_INDEX &&
           getTypeSystemImpl().annotType.subsumes((TypeImpl) c.getType()) &&
           c.getNumberOfKeys() == 3 &&
           
           c.getKeyType(0) == FSIndexComparator.FEATURE_KEY &&
           c.getKeyComparator(0) == FSIndexComparator.STANDARD_COMPARE &&
           c.getKeyFeature(0) == tsi.startFeat &&
           
           c.getKeyType(1) == FSIndexComparator.FEATURE_KEY &&
           c.getKeyComparator(1) == FSIndexComparator.REVERSE_STANDARD_COMPARE &&
           c.getKeyFeature(1) == tsi.endFeat &&
           
           c.getKeyType(2) == FSIndexComparator.TYPE_ORDER_KEY;           
  }
  
  /**
   * @param comparatorForIndexSpecs -
   * @param initialSize -
   * @param indexType -
   * @return -
   */
  <T extends FeatureStructure> FsIndex_singletype<T> addNewIndexCore(
      final FSIndexComparator comparatorForIndexSpecs, 
      int initialSize,
      int indexType) {
    final Type type = comparatorForIndexSpecs.getType();
    // final int vecLen = indexVector.size();
    FsIndex_singletype<T> ind;
    switch (indexType) {
    
    case FSIndex.SET_INDEX: 
      ind = new FsIndex_set_sorted<T>(this.cas, type, indexType, false); // false = is set
      break;
    
//    case FSIndex.FLAT_INDEX: 
//      // this index is only created from another existing index
//      throw new UIMARuntimeException(UIMARuntimeException.INTERNAL_ERROR);
    
    case FSIndex.BAG_INDEX:
    case FSIndex.DEFAULT_BAG_INDEX: 
      ind = new FsIndex_bag<T>(this.cas, type, initialSize, indexType);
      break;
    
    default: 
      // SORTED_INDEX is the default. We don't throw any errors, if the code is unknown, we just create a sorted index.
      ind = new FsIndex_set_sorted<T>(this.cas, type, FSIndex.SORTED_INDEX, true); // true = is sorted
      break;
 
    }
    ind.init(comparatorForIndexSpecs);
    return ind;
  }
  
  private boolean isAnnotationIndex(Type type, FSIndexComparator compForIndexSpecs) {
    TypeSystemImpl tsi = this.sii.tsi;
    return 
        (type == tsi.annotType) &&
        (compForIndexSpecs.getNumberOfKeys() == 3) &&
        (compForIndexSpecs.getKeyType(0) == FSIndexComparator.FEATURE_KEY) &&
        (compForIndexSpecs.getKeyType(1) == FSIndexComparator.FEATURE_KEY) &&
        (compForIndexSpecs.getKeyType(2) == FSIndexComparator.TYPE_ORDER_KEY) &&
        (compForIndexSpecs.getKeyComparator(0) == FSIndexComparator.STANDARD_COMPARE) &&
        (compForIndexSpecs.getKeyComparator(1) == FSIndexComparator.REVERSE_STANDARD_COMPARE) &&       
        (compForIndexSpecs.getKeyComparator(2) == FSIndexComparator.STANDARD_COMPARE) &&
        (compForIndexSpecs.getKeyFeature(0) == tsi.startFeat) &&
        (compForIndexSpecs.getKeyFeature(1) == tsi.endFeat); 
  }
 
  /**
   * Top level call to add the indexes for a particular index definition
   * @param compForIndexSpecs
   * @param indexType
   * @return the iicp for the top new index
   */
  private FsIndex_iicp<FeatureStructure> addNewIndexRecursive(FSIndexComparator compForIndexSpecs, int indexType) {
    final FSIndexComparatorImpl compCopy = ((FSIndexComparatorImpl)compForIndexSpecs).copy();
    return addNewIndexRec(compCopy, indexType);
  }


  // Will modify comparator, so call with copy.
  
  /**
   * Add an index for a type, and then (unless it's a
   * DEFAULT_BAG_INDEX), call yourself recursively to add the indexes for all the directly subsumed subtypes.
   * @param comp4indexSpecs
   * @param indexType
   * @return the new iicp for the new index
   */
  private FsIndex_iicp<FeatureStructure> addNewIndexRec(FSIndexComparator comp4indexSpecs, int indexType) {
    final FsIndex_iicp<FeatureStructure> iicp = this.addNewIndex(comp4indexSpecs, indexType);
    
    /**
     * Maybe add this index for all subtypes (exception: default bag index)
     */
    if (indexType == FSIndex.DEFAULT_BAG_INDEX) {
      // In this special case, we do not add indexes for subtypes.
      return iicp;
    }
    final Type superType = comp4indexSpecs.getType();
    final Vector<Type> types = this.sii.tsi.getDirectlySubsumedTypes(superType);
    final int max = types.size();
    FSIndexComparator compCopy;
    for (int i = 0; i < max; i++) {
      compCopy = ((FSIndexComparatorImpl) comp4indexSpecs).copy();
      compCopy.setType(types.get(i));
      addNewIndexRec(compCopy, indexType);
    }
    return iicp;
  }

  /**
   * Finds an index among iicp's for all defined indexes of a type, such that
   *   the type of the index (SET, BAG, SORTED) is the same and 
   *   the comparatorForIndexSpecs (the keys) are the same
   * @param indexes
   * @param comp
   * @param indexType
   * @return the index in the set of iicps for this type for the matching index
   */
  private static final int findIndex(
      ArrayList<FsIndex_iicp<FeatureStructure>> indexes,
      FSIndexComparator comp,
      int indexingStrategy) {
    
    int i = 0;
    
    for (FsIndex_iicp<FeatureStructure> iicp : indexes) {
      final FsIndex_singletype<FeatureStructure> iicpIndex = iicp.fsIndex_singletype;
      if (iicpIndex.getIndexingStrategy() == indexingStrategy &&
          iicpIndex.getComparatorForIndexSpecs().equals(comp)) {
        return i;
      }
      i++;
    }    
    return -1;
  }

  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#commit()
   */
  public void commit() {
    // Will create the default type order if it doesn't exist at this point.
    getDefaultTypeOrder();
    this.locked = true;
  }

  public LinearTypeOrder getDefaultTypeOrder() {
    if (this.sii.defaultTypeOrder == null) {
      if (this.sii.defaultOrderBuilder == null) {
        this.sii.defaultOrderBuilder = new LinearTypeOrderBuilderImpl(this.sii.tsi);
      }
      try {
        this.sii.defaultTypeOrder = this.sii.defaultOrderBuilder.getOrder();
      } catch (final CASException e) {
        // Since we're doing this on an existing type names, we can't
        // get here.
        throw new UIMARuntimeException(UIMARuntimeException.INTERNAL_ERROR, new Object[0], e);
      }
    }
    return this.sii.defaultTypeOrder;
  }

  public LinearTypeOrderBuilder getDefaultOrderBuilder() {
    if (this.sii.defaultOrderBuilder == null) {
      this.sii.defaultOrderBuilder = new LinearTypeOrderBuilderImpl(this.sii.tsi);
    }
    return this.sii.defaultOrderBuilder;
  }

  void setDefaultTypeOrder(LinearTypeOrder order) {
    this.sii.defaultTypeOrder = order;
  }

  
  /**
   * Managing effective notification that a flat index is no longer valid (at least for new iterators)
   * 
   * Each time an iterator is about to be created, where a flattened index exists, it may be 
   * invalid because an index update occurred for one or more of its contents.  This update may
   * be at any of the subtypes.
   * 
   * When an update occurs, that type plus all of its supertypes need to record that any 
   * already existing flattened index covering these is no longer valid.
   * 
   * This is done in two ways - a slow way and a fast way.  The fast way requires an extra bit
   * of data, a reset BitSet, to be created. This is created the first time a reset like this is
   * needed.  This is because in many applications, there may be lots of types that are never
   * instantiated or used. 
   * 
   * The slow way is to walk up the iicp chain and collect the positions of the bits in the shared
   * flattenedIndexValid, and reset those, and as a side effect, construct the fast reset bitset.
   * During this walk up, if we find a fast reset bitset, stop the walk there.
   * 
   * To make this work, the iicp has a parent pointer, and a position int set at creation time.
   *  
   * 
   * 
   * @return an array of BitSets
   *   [0] is the flattenedIndexValid bitset, all initialized to false (0)
   *   [1 - n] depth-first order of getDirectlySubsumedTypes, the "reset" 
   */
  /**
   * Computing the reset bitset lazily
   * This is only needed when an index update operation for that type occurs.
   * 
   */
//  private BitSet[] createflattenedIndexValid()

  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#getIndexes()
   */
  public Iterator<FSIndex<FeatureStructure>> getIndexes() {
    final ArrayList<FSIndex<FeatureStructure>> indexList = new ArrayList<>();
    final Iterator<String> it = this.getLabels();
    String label;
    while (it.hasNext()) {
      label = it.next();
      indexList.add(getIndex(label));
    }
    return indexList.iterator();
  }
  
  public Iterator<LowLevelIndex> ll_getIndexes() {
    ArrayList<LowLevelIndex> indexList = new ArrayList<LowLevelIndex>();
    final Iterator<String> it = this.getLabels();
    String label;
    while (it.hasNext()) {
      label = it.next();
      indexList.add(ll_getIndex(label));
    }
    return indexList.iterator();
  }

  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#getLabels()
   */
  public Iterator<String> getLabels() {
    return this.name2indexMap.keySet().iterator();
  }

  /**
   * Get the labels for a specific comparator.
   * 
   * @param comp
   *          The comparator.
   * @return An iterator over the labels.
   */
  public <T extends FeatureStructure> Iterator<String> getLabels(FSIndexComparator comp) {
    final ArrayList<String> labels = new ArrayList<String>();
    final Iterator<String> it = this.getLabels();
    String label;
    while (it.hasNext()) {
      label = it.next();
      if (this.name2indexMap.get(label).fsIndex_singletype.getComparatorForIndexSpecs().equals(comp)) {
        labels.add(label);
      }
    }
    return labels.iterator();
  }

  /**
   * @see org.apache.uima.cas.FSIndexRepository#getIndex(String, Type)
   * Find iicp by label (for type it was defined for)
   *   - if not found, return null
   * 
   * Also return null if is an array type of some non-primitive, not TOP (???)
   *   
   * Throw exception if type not subsumed by the top level type
   * 
   * Search all iicps for the type to find the one with same indexing strategy and keys as the iicp for the label  
   */

  public <T extends FeatureStructure> FSIndex<T> getIndex(String label, Type type) {
    
    // iicp is for the type the index was defined for
    final FsIndex_iicp<FeatureStructure> iicp = (FsIndex_iicp<FeatureStructure>) this.name2indexMap.get(label);
    if (iicp == null) {
      return null;
    }
    // Why is this necessary?
    // probably because we don't support indexes over FSArray<some-particular-type>
    if (type.isArray()) {
      final Type componentType = type.getComponentType();
      if ((componentType != null) && !componentType.isPrimitive()
          && !componentType.getName().equals(CAS.TYPE_NAME_TOP)) {
        return null;
      }
    }
    
    final TypeImpl indexType = iicp.fsIndex_singletype.getTypeImpl();
    if (!getTypeSystemImpl().subsumes(indexType, type)) {
      throw new CASRuntimeException(CASRuntimeException.TYPE_NOT_IN_INDEX, label, type.getName(), indexType.getName());
    }

    final ArrayList<FsIndex_iicp<FeatureStructure>> inds = this.getIndexesForType(((TypeImpl) type).getCode()).indexesForType;
    // Since we found an index for the correct type, and 
    // named indexes at creation time create all their subtype iicps, find() must return a
    // valid result
    final int indexCode = findIndex(inds, iicp.fsIndex_singletype.getComparatorForIndexSpecs(), iicp.fsIndex_singletype.getIndexingStrategy());
    return (FSIndex<T>) inds.get(indexCode);
  }
  

  /**
   * @see org.apache.uima.cas.FSIndexRepository#getIndex(String)
   */
  @SuppressWarnings("unchecked")
  public <T extends FeatureStructure> FSIndex<T> getIndex(String label) {
    return (FSIndex<T>) this.name2indexMap.get(label);
  }
  
  /**
   * Remove all instances of a particular type (but not its subtypes) from all indexes
   * @param type -
   */
  public void removeAllExcludingSubtypes(Type type) {
    final int typeCode = ((TypeImpl) type).getCode();
    incrementIllegalIndexUpdateDetector(typeCode);
    // get a list of all indexes defined over this type
    // Includes indexes defined on supertypes of this type
    final ArrayList<FsIndex_iicp<FeatureStructure>> allIndexesForType = this.indexArray[typeCode].indexesForType;
    for (FsIndex_iicp<? extends FeatureStructure> iicp : allIndexesForType) {
      iicp.fsIndex_singletype.flush();
    }
  }
  
  /**
   * Remove all instances of a particular type (including its subtypes) from all indexes
   * @param type -
   */
  public void removeAllIncludingSubtypes(Type type) {
    removeAllExcludingSubtypes(type);
    List<Type> subtypes = this.sii.tsi.getDirectSubtypes(type);
    for (Type subtype : subtypes) {
      removeAllIncludingSubtypes(subtype);
    }
  }
  

  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#createComparator()
   */
  public FSIndexComparator createComparator() {
    return new FSIndexComparatorImpl(this.cas);
  }

  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#isCommitted()
   */
  public boolean isCommitted() {
    return this.locked;
  }

  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#createIndex(org.apache.uima.cas.admin.FSIndexComparator,
   *      java.lang.String)
   */
  public boolean createIndex(FSIndexComparator comp, String label) throws CASAdminException {
    return createIndex(comp, label, FSIndex.SORTED_INDEX);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Serialization support

  /**
   * For one particular view (the one associated with this instance of FsIndexRepositoryImpl),
   * return an array containing the ids of all FSs in any defined index, in this view. 
   * This is intended to be used for serialization.
   * 
   * The order in which FSs occur in the array does not reflect the order in which they
   * were added to the repository. 
   * 
   * @return a stream all FSs in any defined index, in this view.
   */
  public int[] getIndexedFSs() {
    
    final IntVector v = new IntVector();  // accumulates fsAddrs from various indexes
    
    /* Iterate over index by type, with something in there
     * and dump all the fss found for that type (excluding subtypes) into v
     *   bag preferred over sorted; 
     */
    for (int i = 0; i < this.usedIndexes.size(); i++) {
      indexArray[this.usedIndexes.get(i)].getNonSetIndex().fsIndex_singletype.bulkAddTo(v); 
    }  
    return v.toArray();
  } 

  /* *****************************************
   *    Adding/removing FS to/from the index
   * *****************************************/
  public void addFS(int fsRef) {
    ll_addFS(fsRef);
  }
  
  public void ll_addFS(int fsRef) {
    addFS_common(cas.getFsFromId_checked(fsRef), false);  // false === is not an addback call
  }

  public void ll_removeFS(int fsRef) {
    removeFS(cas.getFsFromId_checked(fsRef));
  }
  /**
   * @see org.apache.uima.cas.FSIndexRepository#addFS(org.apache.uima.cas.FeatureStructure)
   */
  public <T extends FeatureStructure> void addFS(T fs) {
    addFS_common(fs, false);
  }

  private void incrementIllegalIndexUpdateDetector(int typeCode) {
    this.detectIllegalIndexUpdates[typeCode] ++;
  }

  /**
   * @see org.apache.uima.cas.FSIndexRepository#removeFS(org.apache.uima.cas.FeatureStructure)
   */
  public void removeFS(FeatureStructure fs) {
    removeFS_ret((FeatureStructureImplC) fs, INCLUDE_BAG_INDEXES);
  }

  public void removeFS(int fsRef) {
    ll_removeFS(fsRef);
  }

  /*
   * Only used by test cases
   * Others call getDefaultOrderBuilder
   * 
   * This method always returns the newly created object which may be different
   * (not identical == ) to the this.defaultOrderBuilder.  
   * Not sure if that's important or a small bug... Oct 2014 schor
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#createTypeSortOrder()
   */
  public LinearTypeOrderBuilder createTypeSortOrder() {
    final LinearTypeOrderBuilder orderBuilder = new LinearTypeOrderBuilderImpl(this.sii.tsi);
    if (this.sii.defaultOrderBuilder == null) {
      this.sii.defaultOrderBuilder = orderBuilder;
    }
    return orderBuilder;
  }

  public <T extends FeatureStructure> LowLevelIndex<T> ll_getIndex(String indexName) {
    return (LowLevelIndex<T>) getIndex(indexName);
  }

  public <T extends FeatureStructure> LowLevelIndex<T> ll_getIndex(String indexName, int typeCode) {
    final TypeSystemImpl tsi = this.sii.tsi;
    if (!tsi.isType(typeCode) || !this.cas.ll_isRefType(typeCode)) {
      throw new LowLevelException(LowLevelException.INVALID_INDEX_TYPE, Integer.toString(typeCode));
    }
    return (LowLevelIndex<T>) getIndex(indexName, tsi.ll_getTypeForCode(typeCode));
  }

  public final void ll_addFS(int fsRef, boolean doChecks) {
    ll_addFS(fsRef);
  }
  
  


  
  public <T extends FeatureStructure> void addback(T fs) {
    addFS_common(fs, true);
  }
  
  private <T extends FeatureStructure> void addFS_common(T fs, boolean isAddback) {
    cas.maybeClearCacheNotInIndex(fs);
    TypeImpl ti = ((FeatureStructureImplC)fs)._typeImpl;
    final int typeCode = ti.getCode();    

    // https://issues.apache.org/jira/browse/UIMA-4099
    // skip test for wrong view if addback, etc.
 
    if (!isAddback && (!IS_DISABLE_ENHANCED_WRONG_INDEX_CHECK) && ti.isAnnotationBaseType()) {
      
      // Check that the annotationBase FS is being added to the proper Cas View
      AnnotationBase fsAnnotBase = (AnnotationBase) fs;
      final Sofa sofa = fsAnnotBase.getSofa();   
      if (cas.getSofa() != sofa) {        
        throw new CASRuntimeException(CASRuntimeException.ANNOTATION_IN_WRONG_INDEX,  
                fsAnnotBase.toString(), sofa.getSofaID(), cas.getSofa().getSofaID());
      }
    }
   
    // indicate this type's indexes are being modified
    // in case an iterator is simultaneously active over this type
    incrementIllegalIndexUpdateDetector(typeCode);
    
    // Get the indexes for the type.
    final ArrayList<FsIndex_iicp<FeatureStructure>> indexes = this.indexArray[typeCode].indexesForType;
    
    // Add fsRef to all indexes.
    boolean noIndexOrOnlySetindexes = true;
    for (FsIndex_iicp<FeatureStructure> iicp : indexes) {
      
      // the indexes for the type are over the type and its subtypes.
      final int indexingStrategy = iicp.fsIndex_singletype.getIndexingStrategy(); 
      if (isAddback) {
        if (indexingStrategy == FSIndex.BAG_INDEX) {
          continue;  // skip adding back to bags - because removes are skipped for bags
        }
      }
      iicp.fsIndex_singletype.insert(fs);
      
      // remember if we get any index other than set by turning this false;
      if (noIndexOrOnlySetindexes) {
        noIndexOrOnlySetindexes = indexingStrategy == FSIndex.SET_INDEX;
      }
    }
    
    // log even if added back, because remove logs remove, and might want to know it was "reindexed"
    if (this.cas.getCurrentMark() != null) {
      logIndexOperation(fs, true);
    }
    
    if (isAddback) { return; }
    
    // https://issues.apache.org/jira/browse/UIMA-4111
    if (noIndexOrOnlySetindexes) {
      // lazily create a default bag index for this type
      final Type type = this.sii.tsi.ll_getTypeForCode(typeCode);
      final String defIndexName = getAutoIndexNameForType(type);
      final FSIndexComparator comparator = createComparator();  // empty comparator
      comparator.setType(type);
      createIndexNoQuestionsAsked(comparator, defIndexName, FSIndex.DEFAULT_BAG_INDEX);

      // add the FS to the bag index
      // which is the last one added
      ((FsIndex_singletype<T>)(indexes.get(indexes.size() - 1)).fsIndex_singletype).insert(fs);
    }

    if (!this.isUsed[typeCode]) {
      // mark this index as used
      this.isUsed[typeCode] = true;
      this.usedIndexes.add(typeCode);
    }
  }

  private static final String getAutoIndexNameForType(Type type) {
    return "_" + type.getName() + "_GeneratedIndex";
  }

  boolean removeFS_ret(FeatureStructureImplC fs, boolean skipBagIndexes) {
    final int typeCode = fs._typeImpl.getCode();
    incrementIllegalIndexUpdateDetector(typeCode);
    final ArrayList<FsIndex_iicp<FeatureStructure>> idxList = this.indexArray[typeCode].indexesForType;

    int nbrRemoved = 0;
    
    for (FsIndex_iicp<FeatureStructure> iicp : idxList) {
      FsIndex_singletype<FeatureStructure> st = iicp.fsIndex_singletype;
      if (skipBagIndexes && !st.isSetOrSorted()) {
        continue;
      }
      nbrRemoved = nbrRemoved + (st.deleteFS(fs) ? 1 : 0);
    }
//    
//    int nbrRemoved = idxList.stream().map(iicp -> iicp.fsIndex_singletype)
//           .filter(st -> (!skipBagIndexes) || st.isSetOrSorted())
//           .mapToInt(st -> st.deleteFS(fs) ? 1 : 0).sum();
    
    if (nbrRemoved > 0) {
      if (this.cas.getCurrentMark() != null) {
        logIndexOperation(fs, ITEM_REMOVED_FROM_INDEX);
      }
    }
    return nbrRemoved > 0;    
  }
   
  public <T extends FeatureStructure> LowLevelIterator<T> ll_getAllIndexedFS(Type type) {
    return (LowLevelIterator<T>) getAllIndexedFS(type);
  }
    
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndexRepository#getAllIndexedFS(org.apache.uima.cas.Type)
   */
  public <T extends FeatureStructure> FSIterator<T> getAllIndexedFS(Type type) {
    final ArrayList<FSIterator<T>> iteratorList = new ArrayList<>();
    getAllIndexedFS(type, iteratorList);
    final int iteratorListSize = iteratorList.size();
    if (iteratorListSize == 0) {
      return FsIterator_subtypes.FS_ITERATOR_EMPTY;
    }
    if (iteratorListSize == 1) {
      return iteratorList.get(0);
    }
    
    FSIterator<T>[] ia = new FSIterator[iteratorListSize];
    return new FsIterator_aggregation_common<T>(iteratorList.toArray(ia), null);
  }

  private final <T extends FeatureStructure> void getAllIndexedFS(Type type, List<FSIterator<T>> iteratorList) {
    // Strategy:  go through the list of all indexes for this type.
    //   The list is intentionally ordered when created to have "SORTED" indexes come first.
    //   
    //   Check all of the Sorted indexes to see if any have a flatten iterator, and if found use that.
    //
    //   If no sorted, flattened indexes exist, use any sorted index, but run as unordered to avoid rattling iterators
    //    
    //   If no sorted index exists, use Bag or Default-bag index.  If default-bag, call recursively to get sub-indexes.
    //   
    //   Note that a default bag index is guaranteed to exist if any FS of Type type were added to the indexes
    //     and only a SET index was defined, see https://issues.apache.org/jira/browse/UIMA-4111

    // get all indexes for this type    
    FsIndex_iicp<FeatureStructure> iicp = getIndexesForType(((TypeImpl)type).getCode()).getNonSetIndex();  
    
    if (null != iicp) {
      iteratorList.add((FSIterator<T>) iicp.iterator());
      if (iicp.isDefaultBagIndex()) {
        // We found one of the special auto-indexes which don't inherit down the tree. So, we
        // manually need to traverse the inheritance tree to look for more indexes. Note that
        // this is not necessary when we have a regular index
        addDirectSubtypes(type, iteratorList);
      }
      return;
    }
        
    // No index for this type was found at all. 
    // Example:  You ask for an iterator over "TOP", but no instances of TOP are created,
    //   and no index over TOP was ever created.
    // Since the auto-indexes are created on demand for
    //   each type, there may be gaps in the inheritance chain. So keep descending the inheritance
    //   tree looking for relevant indexes.
    addDirectSubtypes(type, iteratorList);
  }
  
  private <T extends FeatureStructure> void addDirectSubtypes(Type type, List<FSIterator<T>> iteratorList) {
    ((TypeImpl)type).getDirectSubtypes().stream().forEach(subType -> getAllIndexedFS(subType, iteratorList));
  }
    
  // next method dropped - rather than seeing if something is in the index, and then 
  // later removing it (two lookups), we just conditionally remove it

//  /**
//   * This is used to see if a FS which has a key feature being modified
//   * could corrupt an index in this view.  It returns true if found 
//   * (sometimes it returns true, even if strictly speaking, there is 
//   * no chance of corruption - see below)
//   * 
//   * It does this by seeing if this FS is indexed by one or more Set or Sorted
//   * indexes. No need to check bag indexes - they have no keys, so can't be corrupted by key value changes.
//   * 
//   * Any sorted index indexes all FSs, so if we find a sorted index, return true if it contains the fs, false otherwise.
//   * 
//   * If there are no sorted indexes, if there are one or more set indexes, 
//   *   return true if any of the set indexes have the fs, false if none have it.
//   *   
//   * To speed this up, we keep the set of indexes defined for a type with two additional pieces of meta information:
//   *   1) the first sorted index (or -1 if none are sorted).
//   *   2) a boolean - if there exist any set indexes for this type.  
//   * 
//   * 
//   * @param fs the FS to see if it is in some index that could be corrupted by a key feature value change
//   * @return true if this fs is found in a Set or Sorted index.  
//   */
//  public boolean isInSetOrSortedIndexInThisView(FeatureStructureImplC fs) {
//    final TypeImpl ti = fs._typeImpl;
//    
//    final IndexesForType i4t = indexArray[ti.getCode()];
//    
//    int si = i4t.aSortedIndex;
//    if (si >= 0) { // have sorted index
//      return i4t.indexesForType.get(si).fsIndex_singletype.contains(fs);
//    }
//    
//    int bi = i4t.aBagIndex;
//    if (bi >= 0) {  // have a bag index
//      if (i4t.indexesForType.get(si).fsIndex_singletype.contains(fs) == false) {
//        // not in the index, return false
//        return false;
//      }
//      // is in bag index, there are no sort indexes
//      if (i4t.hasSetIndex) {
//        return true; // approximation - maybe it isn't in the set index.
//      }
//      // no sorted, no set indexes
//      return false; 
//    }
//    
//    // no sort, no bag index
//    // because bag is present if any item indexed, this means nothing in the index
//    return false;
//  }
 
  /**
   * This is called when it has been determined that:
   *   - the fs might be in some indexes
   *   - a change is being made to 1 or more feature values, and those features are being used as keys in one or more indexes
   * 
   * This happens 
   *   - in normal operation, when setting feature values.
   *   - when deserializing a FS using delta CAS which could be modifying an existing one (below the line).
   * 
   * Although one could check each index's keys against the value which was changing, this isn't done because
   *   - it would slow things down
   *   - this "automatic" removal should be the exception.  Users are advised to manually remove the FSs from indexes themselves
   *     before updating features which might be used as keys.
   *     
   * The removal skips removing FSs from bag or default-bag indexes, because these have no keys.
   * The add-back also refrains from adding the FSs back to bag indexes.
   * 
   * The current implementation does not try to determine if any keys are updated with different values,
   *    it just assumes one or more are.   
   * 
   * If the view has nothing other than bag indexes for this type, return false without doing any remove
   * 
   * @param afs - the FS to see if it is in some index that could be corrupted by a key feature value change
   * @return true if this fs was in the indexes and will need to be added back.
   */
  boolean removeIfInCorrputableIndexInThisView(FeatureStructure afs) {
    FeatureStructureImplC fs = (FeatureStructureImplC) afs;
    TypeImpl ti = fs._typeImpl;
    final IndexesForType i4t = indexArray[ti.getCode()];
 
    int si = i4t.aSortedIndex;  
    if (si >= 0) { // then we have a sorted index
      return removeFS_ret(fs, SKIP_BAG_INDEXES);
    }
    
    int bi = i4t.aBagIndex;
    if (bi >= 0) { // have one or more bag indexes including default bag index, for this type
      if (!i4t.indexesForType.get(bi).fsIndex_singletype.contains(fs)) {
        return false;
      }
      if (i4t.hasSetIndex) {
        return removeFS_ret(fs, SKIP_BAG_INDEXES);
      }
    }
    
    // have no bag index, no sort index (implies index is empty) 
    return false;
  }
  
//  /**
//   * reset the flat index is valid for this type
//   */
//  private void indexUpdated(int typeCode) {
//    flattenedIndexValid.clear(typeCode);       
//  }
  
  /**
   * returns the annotation index for a type which is Annotation or a subtype of it.
   * remembers answer in hashmap annotationIndexes, key = TypeImpl
   * @param typeCode
   * @return the index for that type
   */
  <T extends AnnotationFS> FsIndex_annotation<T> getAnnotationIndex(TypeImpl ti) {
//    assert(ti.isAnnotationType());
    FsIndex_annotation<Annotation> r = annotationIndexes.get(ti);
    if (r != null) { 
      return (FsIndex_annotation<T>) r;
    }
    
    final FsIndex_iicp<? extends FeatureStructure> annotation_iicp = this.name2indexMap.get(CAS.STD_ANNOTATION_INDEX);
    final FSIndexComparator comp = annotation_iicp.fsIndex_singletype.getComparatorForIndexSpecs();

    final ArrayList<FsIndex_iicp<FeatureStructure>> iicps_for_type = getIndexesForType(ti.getCode()).indexesForType;
    
    // search all defined indexes for this type, to find an annotation one
    final int ii = findIndex(iicps_for_type, comp, FSIndex.SORTED_INDEX);
    if (ii < 0) {
      /**
       * Error accessing index "{0}" for type "{1}". Index "{0}" is over type "{2}", which is not a
       * supertype of "{1}".
       */
      throw new CASRuntimeException(CASRuntimeException.TYPE_NOT_IN_INDEX, 
          CAS.STD_ANNOTATION_INDEX, ti.getName(), comp.getType().getName());
    }
    annotationIndexes.put(ti, r = (FsIndex_annotation<Annotation>) (Object) iicps_for_type.get(ii)); 
    return (FsIndex_annotation<T>) r;
  }
  
  private <T extends FeatureStructure> void logIndexOperation(T fs, boolean added) {
    this.indexUpdates.add(fs);
    if (added) {
      this.indexUpdateOperation.set(this.indexUpdates.size() - 1, added);  // operation was "add"
    }
    this.logProcessed = false;
  }

  // Delta Serialization support
  /**
   * Go through the journal, and use those entries to update
   *   added, deleted, and reindexed lists
   * in such a way as to guarantee:
   *   a FS is in only one of these lists, (or in none)
   *   
   * For a journal "add-to-indexes" event:
   *   fs in "deleted":  remove from "deleted", add to "reindexed"
   *   fs in "reindexed": do nothing
   *   fs in "added": do nothing
   *   fs not in any of these: add to "added"
   *   
   * For a journal "remove-from-indexes" event:
   *   fs in "added": remove from "added" (don't add to "deleted")
   *   fs in "reindexed": remove from "reindexed" and add to "deleted")
   *   fs in "deleted": do nothing
   *   fs not in any of these: add to "deleted"
   *   
   * The journal is cleared after processing.
   */
  private void processIndexUpdates() {
    
    final ProcessedIndexInfo pii = mPii;
       
    final int len = this.indexUpdates.size();
    for (int i = 0; i < len; i++) {
      final FeatureStructureImplC fs = (FeatureStructureImplC) this.indexUpdates.get(i);
      final boolean added = this.indexUpdateOperation.get(i);
      if (added) {
        boolean wasRemoved = pii.fsDeletedFromIndex.remove(fs);
        if (wasRemoved) {
          pii.fsReindexed.add(fs);  
        } else if (pii.fsReindexed.contains(fs)) {
          continue;  // add on top of reindex is ignored
        } else {  // wasn't in deleted, wasn't in reindexed
          pii.fsAddedToIndex.add(fs);
        }
      } else {
        // operation was remove-from-indexes
        boolean wasRemoved = pii.fsAddedToIndex.remove(fs);
        if (!wasRemoved) {
          pii.fsReindexed.remove(fs);
          pii.fsDeletedFromIndex.add(fs);
        }
      }
    }
    this.logProcessed = true;
    this.indexUpdates.clear();
    this.indexUpdateOperation.clear();
  }
  
  public ObjHashSet<FeatureStructureImplC> getUpdatedFSs(ObjHashSet<FeatureStructureImplC> items) {
    if (!this.logProcessed) {
      processIndexUpdates();
    }
    return items;    
  }
  
  public ObjHashSet<FeatureStructureImplC> getAddedFSs() {
    return getUpdatedFSs(mPii.fsAddedToIndex);
  }

  public ObjHashSet<FeatureStructureImplC> getDeletedFSs() {
    return getUpdatedFSs(mPii.fsDeletedFromIndex);
  }

  public ObjHashSet<FeatureStructureImplC> getReindexedFSs() {
    return getUpdatedFSs(mPii.fsReindexed);
  }

  public boolean isModified() {
    if (!this.logProcessed) {
      processIndexUpdates();
    }
    final ProcessedIndexInfo pii = mPii;
    return ((pii.fsAddedToIndex.size() > 0) || (pii.fsDeletedFromIndex.size() > 0) || (pii.fsReindexed
        .size() > 0));
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " [" + cas + "]";
  }
  
//  public Comparator<AnnotationFS> getAnnotationComparator() {
//    if (null == this.sii.annotationComparator) {
//      @SuppressWarnings("unchecked")
//      final FsIndex_iicp<AnnotationFS> iicp = 
//          (FsIndex_iicp<AnnotationFS>) this.name2indexMap.get(CAS.STD_ANNOTATION_INDEX);
//      this.sii.annotationComparator = (FSIntArrayIndex<AnnotationFS>)(iicp.fsIndex_singletype);
//    }
//    return this.sii.annotationComparator;
//  }
  
  Comparator<AnnotationFS> getAnnotationFsComparator() {
    Comparator<AnnotationFS> r = this.sii.annotationFsComparator;
    // lazy creation
    if (null != r) {
      return r;
    }
    
    return createAnnotationFsComparator();
  }
  
  
  private Comparator<AnnotationFS> createAnnotationFsComparator() {

    final LinearTypeOrder typeOrder = getDefaultTypeOrder();
    
    return this.sii.annotationFsComparator = new Comparator<AnnotationFS>() {

      @Override
      public int compare(AnnotationFS fsx1, AnnotationFS fsx2) {
        Annotation fs1 = (Annotation) fsx1;
        Annotation fs2 = (Annotation) fsx2;
        
        if (fs1 == fs2) return 0;

        int result =  Integer.compare(fs1.getBegin(), fs2.getBegin());
        if (result != 0) return result;

        result = Integer.compare(fs1.getEnd(), fs2.getEnd());
        if (result != 0) return -result;  // reverse compare
        
        return typeOrder.compare(fs1, fs2);          
      }
    };
  }
  
  public TypeSystemImpl getTypeSystemImpl() {
    return sii.tsi;
  }
}
