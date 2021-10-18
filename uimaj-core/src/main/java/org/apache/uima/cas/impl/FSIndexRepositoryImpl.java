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

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSComparators;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.LinearTypeOrder;
import org.apache.uima.cas.admin.LinearTypeOrderBuilder;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.ObjHashSet;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

// @formatter:off
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
 * APIs having no reference to Java cover types (i.e., low level iterators) are not generic, unless
 * they are needed to be to pass along the associated type to other APIs.
 */
//@formatter:on
public class FSIndexRepositoryImpl implements FSIndexRepositoryMgr, LowLevelIndexRepository {

  // private final static boolean DEBUG = false;

  public final static boolean ITEM_ADDED_TO_INDEX = true;
  public final static boolean ITEM_REMOVED_FROM_INDEX = false;
  /** set next to true to debug issues with different treatment of no type priorities in v3 */
  public final static boolean V2_ANNOTATION_COMPARE_TYPE_ORDER = false;
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
   * Define this JVM property to allow adding the same identical FS to Set and Sorted indexes more
   * than once.
   */
  public static final String ALLOW_DUP_ADD_TO_INDEXES = "uima.allow_duplicate_add_to_indexes";
  static {
    if (Misc.getNoValueSystemProperty(ALLOW_DUP_ADD_TO_INDEXES)) {
      throw new CASAdminException(CASAdminException.INDEX_DUPLICATES_NOT_SUPPORTED);
    }
  }

  public static final String DISABLE_ENHANCED_WRONG_INDEX = "uima.disable_enhanced_check_wrong_add_to_index";

  private static final boolean IS_DISABLE_ENHANCED_WRONG_INDEX_CHECK = // true || // debug
          Misc.getNoValueSystemProperty(DISABLE_ENHANCED_WRONG_INDEX);

  // Implementation note: the use of equals() here is pretty hairy and
  // should probably be fixed. We rely on the fact that when two
  // FSIndexComparators are compared, the type of the comparators is
  // ignored! A fix for this would be to split the FSIndexComparator
  // class into two classes, one for the key-comparator pairs, and one
  // for the combination of the two. Note also that we compare two
  // FsIndex_iicps by comparing their
  // index.getComparator()s.

//@formatter:off
  /**
   * The index repository holds instances of defined and built-in indexes.
   * 
   * Indexes implement Java's NavigableSet ( *** do this later ??? may be a bit of work to make work
   * over subtypes )
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
//@formatter:on

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
    private Comparator<TOP> annotationFsComparatorWithoutId = null;

    private Comparator<TOP> annotationFsComparatorWithId = null;

    private Comparator<TOP> annotationFsComparatorNoTypeWithoutId = null;

    private Comparator<TOP> annotationFsComparatorNoTypeWithId = null;

    /**
     * optimization only - bypasses some shared (among views) initialization if already done
     */
    private boolean isSetUpFromBaseCAS = false;

    SharedIndexInfo(TypeSystemImpl typeSystem) {
      this.tsi = typeSystem;
    }
  }

  /**
   * For processing index updates in batch mode when deserializing from a remote service; lists of
   * FSs that were added, removed, or reindexed
   * 
   * only used when processing updates in batch mode
   */
  private static class ProcessedIndexInfo {
    final private Set<TOP> fsAddedToIndex = new ObjHashSet<>(TOP.class, TOP._singleton);
    final private Set<TOP> fsDeletedFromIndex = new ObjHashSet<>(TOP.class, TOP._singleton);
    final private Set<TOP> fsReindexed = new ObjHashSet<>(TOP.class, TOP._singleton);
  }

  /**
   * Information about all the indexes for a single type. This is kept in an List, with the key
   * being the type code.
   */
  static class IndexesForType {
    /**
     * true if one or more of the indexes is a set index
     */
    boolean hasSetIndex;
    final String typename;
    /**
     * index of any sorted index or -1 if no sorted index
     */
    int aSortedIndex = -1; // -1 or the position of an arbitrary sorted index
    int aBagIndex = -1; // -1 or the position of an arbitrary bag index
    final ArrayList<FsIndex_iicp<TOP>> indexesForType = new ArrayList<>(0);

    IndexesForType(TypeImpl ti) {
      this.typename = ti.getName();
    }

    <T extends TOP> FsIndex_iicp<T> getNonSetIndex() {
      if (aSortedIndex < 0 && aBagIndex < 0) { // index is empty!
        return null;
      }
      return (FsIndex_iicp<T>) indexesForType.get((aBagIndex >= 0) ? aBagIndex : aSortedIndex);
    }

    void add(FsIndex_iicp<TOP> iicp) {
      assert typename.equals(iicp.fsIndex_singletype.getType().getName());
      final int kind = iicp.fsIndex_singletype.getIndexingStrategy();
      int i = indexesForType.size();
      switch (kind) {
        case FSIndex.BAG_INDEX:
          aBagIndex = i;
          break;
        case FSIndex.DEFAULT_BAG_INDEX:
          if (aBagIndex == -1) { // real bag indexes have priority
            aBagIndex = i;
          }
          break;
        case FSIndex.SORTED_INDEX:
          aSortedIndex = i;
          break;
        case FSIndex.SET_INDEX:
          hasSetIndex = true;
          break;
        default:
          Misc.internalError();
      }
      indexesForType.add(iicp);
    }

    <T extends FeatureStructure> FsIndex_iicp<T> getIndexExcludingType(int indexingStrategy,
            FSIndexComparatorImpl comparatorForIndexSpecs) {
      for (FsIndex_iicp<TOP> index : indexesForType) {
        FsIndex_singletype<TOP> singleTypeIndex = index.fsIndex_singletype;

        if (singleTypeIndex.getIndexingStrategy() == indexingStrategy) {
          FSIndexComparatorImpl indexComp = singleTypeIndex.getComparatorImplForIndexSpecs();
          if (indexComp.equalsWithoutType(comparatorForIndexSpecs)) {
            return (FsIndex_iicp<T>) index;
          }
        }
      }
      return null;
    }

    private void removeIndexExcludingType(int indexingStrategy,
            FSIndexComparatorImpl comparatorForIndexSpecs) {
      Iterator<FsIndex_iicp<TOP>> it = indexesForType.iterator();
      while (it.hasNext()) {
        FsIndex_singletype<TOP> singleTypeIndex = it.next().fsIndex_singletype;
        if (singleTypeIndex.getIndexingStrategy() == indexingStrategy) {
          FSIndexComparatorImpl indexComp = singleTypeIndex.getComparatorImplForIndexSpecs();
          if (indexComp.equalsWithoutType(comparatorForIndexSpecs)) {
            it.remove();
            break;
          }
        }
      }
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("IndexesForType ").append(typename).append(" [hasSetIndex=")
              .append(hasSetIndex).append(", aSortedIndex=").append(aSortedIndex)
              .append(", aBagIndex=").append(aBagIndex).append(", indexesForType=")
              .append(indexesForType).append("]");
      return builder.toString();
    }

  }

  /***** I N S T A N C E V A R I A B L E S *****/
  /***** Replicated per view *****/

  // A reference to the CAS View.
  private final CASImpl cas;

  // Is the index repository locked?
  private boolean locked = false;

  /**
   * An array of information about defined indexes, one for each type in the type hierarchy. -
   * includes for each type, an unordered list of FsIndex_iicps for that type, corresponding to the
   * different index definitions over that type.
   * 
   * The key is the typecode of the type.
   */

  final IndexesForType[] indexArray;

  IndexesForType getIndexesForType(int typeCode) {
    return indexArray[typeCode];
  }

  IndexesForType getIndexesForUsedType(int i) {
    return indexArray[this.usedIndexes.get(i)];
  }

  // moved from here into individual indexes over each type, for better locality of reference
  // /**
  // * an array of ints, one for each type in the type hierarchy.
  // * Used to enable iterators to detect modifications (adds / removes)
  // * to indexes they're iterating over while they're iterating over them.
  // * Not private so it can be seen by FSLeafIndexImpl
  // */
  // final int[] detectIllegalIndexUpdates;

  /**
   * A map from names to FsIndex_iicps, which represent the index at the top-most type declared in
   * the index specification. Different names may map to the same iicp. The keys are the same across
   * all views, but the values are different, per view
   */
  final HashMap<String, FsIndex_iicp<TOP>> name2indexMap;

  /**
   * speedup for annotation index accessing by type, lazily initialized
   */
  final private Map<TypeImpl, FsIndex_annotation<Annotation>> annotationIndexes = new IdentityHashMap<>();

  // the next are for journaling updates to indexes
  final private List<TOP> indexUpdates;

  final private BitSet indexUpdateOperation;

  private boolean logProcessed;

  // Monitor indexes used to optimize getIndexedFS and flush
  // set of typecodes corresponding to indexes that are used
  final private IntVector usedIndexes;

  // one bit per typeCode, indexed by typeCode
  // This is only to speed up the test to skip adding an index to the set of "used" ones if it
  // already is used.
  final private BitSet isUsed;

  // /**
  // * Used for maintaining collection of all used iicp's for indexes
  // * package scope for setting in index impl flush
  // */
  // boolean isUsedChanged = true;
  //
  // /**
  // * iicps for all FSs
  // */
  // private List<FsIndex_iicp<?>> iicps4allFSs = null;

  // Monitor which indexes are iterated over, to allow resetting flatIndexes
  // final private List<FsIndex_iicp<? extends FeatureStructure>> iteratedSortedIndexes =
  // Collections.synchronizedList(new ArrayList<FsIndex_iicp<? extends FeatureStructure>>());

  private final SharedIndexInfo sii;

  private ProcessedIndexInfo mPii;

  /** ----------------------- Support for flattened indexes ----------------- */

  // this approach doesn't work, because an iterator over a subtype could do an invalid->valid
  // transition
  // while a flattened iterator over a supertype was in existence.
  // Subsequent creations of new iterators over the supertype would not notice that the flattened
  // iterator was invalid.
  // /**
  // * FlattenedIndexValid
  // *
  // * <p>a BitSet, one per view, indexed by typeCode
  // * A bit[i] being on means that a time window has begun (from the moment it is turned on)
  // * where a flattened version if it exists) is valid, for index[i] and all its subtypes.
  // *
  // * <p>Used at iterator creation time to see if a flattened multi-type sorted index needs to be
  // discarded.</p>
  // *
  // * <p>Bits are initially off.</p>
  // *
  // * <p>Bit is turned on when a flattened index is successfully created for any index
  // * which starts at that type; the flag is only set for the type the flattened index is created
  // for (not its subtypes)</p>
  // *
  // * Bit is turned off for add/remove to/from index operation, for a type and all its super types.
  // * This is facilitated by having a bit set for each type of all its supertypes.
  // * This insures that an upper level flattened index is invalidated, even if the lower level
  // * gets a new flattened index (and has its is valid bit is set).
  // * The reason for this is that any update to a subtype of a type having
  // * a flattened index causes that flattened index to become invalid.</p>
  // *
  // * Multi-threading: Because BitSet is not safe for multithread use, all reading / writing done
  // * using itself as a synch lock.
  // */
  // final ConcurrentBits flattenedIndexValid;

  // boolean syncGetFlattenedIndexValid(int i) {
  // synchronized (flattenedIndexValid) {
  // return flattenedIndexValid.get(i);
  // }
  // }

  @SuppressWarnings("unused")
  private FSIndexRepositoryImpl() {
    this.cas = null; // because it's final
    this.sii = null;
    this.name2indexMap = null;
    this.indexArray = null;
    // this.detectIllegalIndexUpdates = null;
    // this.flattenedIndexValid = null;
    this.indexUpdates = null;
    this.indexUpdateOperation = null;
    this.usedIndexes = null;
    this.isUsed = null;
    // this.isUsedChanged = true;
    // this.iicps4allFSs = null;
  }

  /**
   * Constructor. Assumption: called first before next constructor call, with the base CAS view
   * 
   * @param cas
   */
  FSIndexRepositoryImpl(CASImpl cas) {
    this.cas = cas;
    this.sii = new SharedIndexInfo(cas.getTypeSystemImpl());

    final TypeSystemImpl ts = this.sii.tsi;
    // Type counting starts at 1.
    final int numTypes = ts.getNumberOfTypes() + 1;
    // this.detectIllegalIndexUpdates = new int[numTypes];
    // this.flattenedIndexValid = new ConcurrentBits(numTypes);
    this.name2indexMap = new HashMap<>();
    this.indexUpdates = new ArrayList<>();
    this.indexUpdateOperation = new BitSet();
    this.logProcessed = false;
    this.indexArray = new IndexesForType[this.sii.tsi.getNumberOfTypes() + 1];
    this.usedIndexes = new IntVector();
    this.isUsed = new BitSet(numTypes);
    // this.isUsedChanged = true;
    // this.iicps4allFSs = new ArrayList<>();
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
    sii.isSetUpFromBaseCAS = true; // bypasses initialization already done

    final TypeSystemImpl ts = this.sii.tsi;
    // Type counting starts at 1.
    final int numTypes = ts.getNumberOfTypes() + 1;
    // this.detectIllegalIndexUpdates = new int[numTypes];
    // this.flattenedIndexValid = new ConcurrentBits(numTypes);

    this.name2indexMap = new HashMap<>();
    this.indexUpdates = new ArrayList<>();
    this.indexUpdateOperation = new BitSet();
    this.logProcessed = false;
    this.indexArray = new IndexesForType[numTypes];
    this.usedIndexes = new IntVector();
    this.isUsed = new BitSet(numTypes);
    // this.isUsedChanged = true;
    // this.iicps4allFSs = new ArrayList<>();
    init();
    // cant do this here because need to have the CAS's ref to this instance set before this is
    // done.
    // baseIndexRepo.name2indexMap.keySet().stream().forEach(key -> createIndex(baseIndexRepo,
    // key));
  }

  /**
   * Initialize data. Common initialization called from the constructors.
   */
  private void init() {
    final TypeSystemImpl ts = this.sii.tsi;

    // **********************************************
    // for each type in the TypeSystem,
    // create a list of iicp's
    // each one corresponding to a defined index
    // **********************************************
    final int numTypes = ts.getNumberOfTypes() + 1; // Type counting starts at 1.
    // Can't instantiate arrays of generic types, but this is ok for ArrayList.
    for (int i = 1; i < numTypes; i++) {
      this.indexArray[i] = new IndexesForType(ts.types.get(i));
    }

    // Arrays.fill(detectIllegalIndexUpdates, Integer.MIN_VALUE);
    mPii = new ProcessedIndexInfo();
  }

  // ***************
  // Create indexes
  // ***************

  /**
   * create indexes in a view, by copying the baseCas's index repository's definitions
   * 
   * Called when creating or refreshing (after deserializing) a view
   * 
   * @param baseIndexRepo
   *          - where the base definitions are
   * @param key
   *          - the name of the index.
   */
  <T extends FeatureStructure> void createIndex(FSIndexRepositoryImpl baseIndexRepo, String key) {
    final FsIndex_singletype<TOP> fsIndex = baseIndexRepo.name2indexMap.get(key).fsIndex_singletype;
    createIndexNoQuestionsAsked(fsIndex.getComparatorImplForIndexSpecs(), key,
            fsIndex.getIndexingStrategy());
  }

  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#createIndex(FSIndexComparator, String)
   */
  @Override
  public boolean createIndex(FSIndexComparator comp, String label, int indexType)
          throws CASAdminException {
    if (this.locked) {
      throw new CASAdminException(CASAdminException.REPOSITORY_LOCKED);
    }
    return createIndexNoQuestionsAsked(comp, label, indexType);
  }

  /**
   * This is public only until the xml specifier format supports specifying index kinds (set, bag
   * etc.).
   * 
   * @param comp
   *          -
   * @param label
   *          -
   * @param indexType
   *          -
   * @param <T>
   *          -
   * @return -
   */
  public <T extends FeatureStructure> boolean createIndexNoQuestionsAsked(
          final FSIndexComparator comp, String label, int indexType) {

    FsIndex_iicp<TOP> cp = this.name2indexMap.get(label);

    if (cp == null) {
      // Create new index
      cp = this.addNewIndexRecursive(comp, indexType);

      // create a set of feature codes that are in one or more index definitions,
      // only once for all cas views
      if (!sii.isSetUpFromBaseCAS) {
        for (int i = 0, nKeys = comp.getNumberOfKeys(); i < nKeys; i++) {
          if (comp.getKeyType(i) == FSIndexComparator.FEATURE_KEY) {
            FeatureImpl fi = (FeatureImpl) comp.getKeyFeature(i);
            cas.featureCodes_inIndexKeysAdd(fi.getCode()/* , fi.registryIndex */);
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

//@formatter:off
  /**
   * just for testing purposes
   * removes the named index
   * Also removes indexes for all subtypes.  
   *   - NOTE this might remove another index definition's index, if it matches
   * Only valid if no add-to-index operations have happened.
   * 
   * @param label
   *          the name of the index to remove
   */
//@formatter:on
  public void removeIndex(String label) {
    FsIndex_iicp<TOP> cp = this.name2indexMap.get(label);
    if (cp == null) {
      return;
    }
    int indexingStrategy = cp.getIndexingStrategy();
    FSIndexComparatorImpl comp = cp.getComparatorImplForIndexSpecs();
    removeIndexBySpec(cp.getTypeCode(), indexingStrategy, comp);

    if (indexingStrategy != FSIndex.DEFAULT_BAG_INDEX) {
      final TypeImpl type = (TypeImpl) cp.getType();
      type.getAllSubtypes().forEachOrdered(subType -> {
        FSIndexComparatorImpl compSub = comp.copy();
        compSub.setType(type);
        removeIndexBySpec(subType.getCode(), indexingStrategy, compSub);
      });
    }
  }

  /**
   * Reset all indexes, in one view.
   */
  public void flush() {
    if (!this.locked) {
      return;
    }

    // if (DEBUG) {
    // System.out.println("Index Flush Top");
    // }
    // Do nothing really fast!
    if (this.usedIndexes.size() == 0) {
      return;
    }

    annotationIndexes.clear();
    isUsed.clear();
    // isUsedChanged = true;
    // iicps4allFSs.clear();
    for (int i = 0; i < usedIndexes.size(); i++) {
      int used = this.usedIndexes.get(i);
      for (FsIndex_iicp<?> iicp : indexArray[used].indexesForType) {
        iicp.fsIndex_singletype.flush();
      }
    }

    // clearIteratedSortedIndexes();

    // reset the index update trackers
    // resetDetectIllegalIndexUpdates();

    this.indexUpdates.clear();
    this.indexUpdateOperation.clear();
    mPii = new ProcessedIndexInfo();
    // this.fsAddedToIndex = new IntSet();
    // this.fsDeletedFromIndex = new IntSet();
    // this.fsReindexed = new PositiveIntSet_impl();
    this.logProcessed = false;
    this.usedIndexes.removeAllElements();
  }

  // // for now, with flattened index optimization disabled, this should be a no-op
  // private void clearIteratedSortedIndexes() {
  // int sz = iteratedSortedIndexes.size();
  // if (DEBUG) {
  // System.out.println("Index Flush flatIndex, size = " + sz);
  // }
  //
  //// iteratedSortedIndexes.stream().forEach(iicp -> iicp.flatIndex.flush());
  //
  // if (DEBUG) {
  // if (iteratedSortedIndexes.size() != sz) {
  // throw new RuntimeException(
  // "Index Flush flatIndex, size not the same, before = " +
  // sz + ", after = " + iteratedSortedIndexes.size());
  // }
  // }
  // iteratedSortedIndexes.clear();
  // }

  // *****************************************
  // Adding indexes to the index repository
  // *****************************************

  private FsIndex_iicp<TOP> addNewIndex(FSIndexComparatorImpl comparator, int indexType) {
    return addNewIndex(comparator, DEFAULT_INDEX_SIZE, indexType);
  }

  /**
   * This is where the actual index gets created.
   */
  private <T extends TOP> FsIndex_iicp<T> addNewIndex(final FSIndexComparatorImpl comparator,
          int initialSize, int indexType) {
    FsIndex_iicp<T> iicp = null;
    if (isAnnotationIndex(comparator, indexType)) {
      FsIndex_singletype<Annotation> index = addNewIndexCore(comparator, initialSize, indexType);
      iicp = (FsIndex_iicp<T>) new FsIndex_annotation<>(index);
    } else {
      FsIndex_singletype<TOP> index = addNewIndexCore(comparator, initialSize, indexType);
      iicp = (FsIndex_iicp<T>) new FsIndex_iicp<>(index);
    }
    final Type type = comparator.getType();
    final int typeCode = ((TypeImpl) type).getCode();

    // add indexes so that sorted ones are first, to benefit getAllIndexedFSs
    // if (indexType == FSIndex.SORTED_INDEX) {
    // this.indexArray[typeCode].add(0, iicp); // shifts rest down
    // } else
    getIndexesForType(typeCode).add((FsIndex_iicp<TOP>) iicp);
    // }
    return iicp;
  }

  boolean isAnnotationIndex(FSIndexComparator c, int indexKind) {
    TypeSystemImpl tsi = getTypeSystemImpl();
    return indexKind == FSIndex.SORTED_INDEX
            && getTypeSystemImpl().annotType.subsumes((TypeImpl) c.getType())
            && c.getNumberOfKeys() == 3 &&

            c.getKeyType(0) == FSIndexComparator.FEATURE_KEY
            && c.getKeyComparator(0) == FSIndexComparator.STANDARD_COMPARE
            && c.getKeyFeature(0) == tsi.startFeat &&

            c.getKeyType(1) == FSIndexComparator.FEATURE_KEY
            && c.getKeyComparator(1) == FSIndexComparator.REVERSE_STANDARD_COMPARE
            && c.getKeyFeature(1) == tsi.endFeat &&

            c.getKeyType(2) == FSIndexComparator.TYPE_ORDER_KEY;
  }

  /**
   * The routine which actually creates a new index, for a single type.
   * 
   * @param comparatorForIndexSpecs
   *          -
   * @param initialSize
   *          -
   * @param indexingStrategy
   *          -
   * @return -
   */
  <T extends TOP> FsIndex_singletype<T> addNewIndexCore(
          final FSIndexComparatorImpl comparatorForIndexSpecs, int initialSize,
          int indexingStrategy) {
    final TypeImpl type = (TypeImpl) comparatorForIndexSpecs.getType();

    FsIndex_singletype<T> ind;
    switch (indexingStrategy) {

      case FSIndex.SET_INDEX:
        ind = new FsIndex_set_sorted<>(this.cas, type, indexingStrategy, comparatorForIndexSpecs); // false
                                                                                                   // =
                                                                                                   // is
                                                                                                   // set
        break;

      // case FSIndex.FLAT_INDEX:
      // // this index is only created from another existing index
      // throw new UIMARuntimeException(UIMARuntimeException.INTERNAL_ERROR);

      case FSIndex.BAG_INDEX:
      case FSIndex.DEFAULT_BAG_INDEX:
        ind = new FsIndex_bag<>(this.cas, type, initialSize, indexingStrategy,
                comparatorForIndexSpecs);
        break;

      default:
        // SORTED_INDEX is the default. We don't throw any errors, if the code is unknown, we just
        // create a sorted index.
        ind = new FsIndex_set_sorted<>(this.cas, type, FSIndex.SORTED_INDEX,
                comparatorForIndexSpecs); // true = is sorted
        break;

    }
    return ind;
  }

  /**
   * Top level call to add the indexes for a particular index definition
   * 
   * @param compForIndexSpecs
   * @param indexType
   * @return the iicp for the top new index
   */
  private FsIndex_iicp<TOP> addNewIndexRecursive(FSIndexComparator compForIndexSpecs,
          int indexType) {
    final FSIndexComparatorImpl compCopy = ((FSIndexComparatorImpl) compForIndexSpecs).copy();
    return addNewIndexRec(compCopy, indexType);
  }

  // Will modify comparator, so call with copy.

  /**
   * Add an index for a type, and then (unless it's a DEFAULT_BAG_INDEX), call yourself recursively
   * to add the indexes for all the directly subsumed subtypes.
   * 
   * @param comp4indexSpecs
   * @param indexType
   * @return the new iicp for the new index
   */
  private FsIndex_iicp<TOP> addNewIndexRec(FSIndexComparatorImpl comp4indexSpecs, int indexType) {

    // if this index is already in existence, just reuse it.
    FsIndex_iicp<TOP> existing = getIndexBySpec(comp4indexSpecs.getTypeCode(), indexType,
            comp4indexSpecs);
    if (null != existing) {
      return existing;
    }

    final FsIndex_iicp<TOP> iicp = this.addNewIndex(comp4indexSpecs, indexType);

    /**
     * Maybe add this index for all subtypes (exception: default bag index)
     */
    if (indexType == FSIndex.DEFAULT_BAG_INDEX) {
      // In this special case, we do not add indexes for subtypes.
      return iicp;
    }
    final TypeImpl superType = (TypeImpl) comp4indexSpecs.getType();

    for (Type subType : superType.getDirectSubtypes()) {
      FSIndexComparatorImpl compCopy = comp4indexSpecs.copy();
      compCopy.setType(subType);
      addNewIndexRec(compCopy, indexType);
    }
    return iicp;
  }

  // /**
  // * Finds an index among iicp's for all defined indexes of a type, such that
  // * the type of the index (SET, BAG, SORTED) is the same and
  // * the comparatorForIndexSpecs (the keys) are the same
  // *
  // * Note: the callers of this require that the comp "type" not be part of the comparison -
  // * because it will always miscompare, because we're using the
  // * "top" level type defined by the index spec but looking for a subtype with the same comparator
  // * @param indexes
  // * @param comp
  // * @param indexType
  // * @return the index in the set of iicps for this type for the matching index
  // */
  // private static final int findIndex(
  // ArrayList<FsIndex_iicp<FeatureStructure>> indexes,
  // FSIndexComparatorImpl comp,
  // int indexingStrategy) {
  //
  // int i = 0;
  //
  // for (FsIndex_iicp<FeatureStructure> iicp : indexes) {
  // final FsIndex_singletype<FeatureStructure> iicpIndex = iicp.fsIndex_singletype;
  // if (iicpIndex.getIndexingStrategy() == indexingStrategy &&
  // ((FSIndexComparatorImpl)(iicpIndex.getComparatorForIndexSpecs())).equalsWithoutType(comp)) {
  // return i;
  // }
  // i++;
  // }
  // return -1;
  // }

  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#commit()
   */
  @Override
  public void commit() {
    // Will create the default type order if it doesn't exist at this point.
    getDefaultTypeOrder();
    this.locked = true;
  }

  @Override
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

  @Override
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
   * Managing effective notification that a flat index is no longer valid (at least for new
   * iterators)
   * 
   * Each time an iterator is about to be created, where a flattened index exists, it may be invalid
   * because an index update occurred for one or more of its contents. This update may be at any of
   * the subtypes.
   * 
   * When an update occurs, that type plus all of its supertypes need to record that any already
   * existing flattened index covering these is no longer valid.
   * 
   * This is done in two ways - a slow way and a fast way. The fast way requires an extra bit of
   * data, a reset BitSet, to be created. This is created the first time a reset like this is
   * needed. This is because in many applications, there may be lots of types that are never
   * instantiated or used.
   * 
   * The slow way is to walk up the iicp chain and collect the positions of the bits in the shared
   * flattenedIndexValid, and reset those, and as a side effect, construct the fast reset bitset.
   * During this walk up, if we find a fast reset bitset, stop the walk there.
   * 
   * To make this work, the iicp has a parent pointer, and a position int set at creation time.
   * 
   * @return an array of BitSets [0] is the flattenedIndexValid bitset, all initialized to false (0)
   *         [1 - n] depth-first order of getDirectlySubsumedTypes, the "reset"
   */
  /**
   * Computing the reset bitset lazily This is only needed when an index update operation for that
   * type occurs.
   * 
   */
  // private BitSet[] createflattenedIndexValid()

  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#getIndexes()
   */
  @Override
  public Iterator<FSIndex<TOP>> getIndexes() {
    final ArrayList<FSIndex<TOP>> indexList = new ArrayList<>();
    final Iterator<String> it = this.getLabels();
    String label;
    while (it.hasNext()) {
      label = it.next();
      indexList.add(getIndex(label));
    }
    return indexList.iterator();
  }

  @Override
  public Iterator<LowLevelIndex> ll_getIndexes() {
    ArrayList<LowLevelIndex> indexList = new ArrayList<>();
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
  @Override
  public Iterator<String> getLabels() {
    return this.name2indexMap.keySet().iterator();
  }

  /**
   * Get the labels for a specific comparator.
   * 
   * @param comp
   *          The comparator.
   * @param <T>
   *          type of Feature Structure
   * @return An iterator over the labels.
   */
  public <T extends FeatureStructure> Iterator<String> getLabels(FSIndexComparator comp) {
    final ArrayList<String> labels = new ArrayList<>();
    final Iterator<String> it = this.getLabels();
    String label;
    while (it.hasNext()) {
      label = it.next();
      if (this.name2indexMap.get(label).fsIndex_singletype.getComparatorImplForIndexSpecs()
              .equals(comp)) {
        labels.add(label);
      }
    }
    return labels.iterator();
  }

  /**
   * @see org.apache.uima.cas.FSIndexRepository#getIndex(String, Type) Find iicp by label (for type
   *      it was defined for) - if not found, return null
   * 
   *      Also return null if is an array type of some non-primitive, not TOP (???)
   * 
   *      Throw exception if type not subsumed by the top level type
   * 
   *      Search all iicps for the type to find the one with same indexing strategy and keys as the
   *      iicp for the label
   */

  @Override
  public <T extends FeatureStructure> FSIndex<T> getIndex(String label, Type type) {

    // iicp is for the type the index was defined for
    final FsIndex_iicp<TOP> iicp = this.name2indexMap.get(label);
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
    final TypeImpl ti = (TypeImpl) type;
    if (!indexType.subsumes(ti)) {
      throw new CASRuntimeException(CASRuntimeException.TYPE_NOT_IN_INDEX, label, type.getName(),
              indexType.getName());
    }

    // Since we found an index for the correct type, and
    // named indexes at creation time create all their subtype iicps, find() must return a
    // valid result
    return (FSIndex<T>) this.getIndexBySpec(ti.getCode(), iicp.getIndexingStrategy(),
            iicp.getComparatorImplForIndexSpecs());
  }

  /**
   * @see org.apache.uima.cas.FSIndexRepository#getIndex(String)
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T extends FeatureStructure> LowLevelIndex<T> getIndex(String label) {
    return (LowLevelIndex<T>) this.name2indexMap.get(label);
  }

  /**
   * Remove all instances of a particular type (but not its subtypes) from all indexes
   * 
   * @param type
   *          -
   */
  @Override
  public void removeAllExcludingSubtypes(Type type) {
    final int typeCode = ((TypeImpl) type).getCode();
    // incrementIllegalIndexUpdateDetector(typeCode);
    // get a list of all indexes defined over this type
    // Includes indexes defined on supertypes of this type
    final ArrayList<FsIndex_iicp<TOP>> allIndexesForType = getIndexesForType(
            typeCode).indexesForType;
    for (FsIndex_iicp<? extends FeatureStructure> iicp : allIndexesForType) {
      iicp.fsIndex_singletype.removeAll();
    }
  }

  /**
   * Remove all instances of a particular type (including its subtypes) from all indexes
   * 
   * @param type
   *          Type to remove (including all its subtypes) from this particular view.
   */
  @Override
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
  @Override
  public FSIndexComparator createComparator() {
    return new FSIndexComparatorImpl();
  }

  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#isCommitted()
   */
  @Override
  public boolean isCommitted() {
    return this.locked;
  }

  /**
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#createIndex(org.apache.uima.cas.admin.FSIndexComparator,
   *      java.lang.String)
   */
  @Override
  public boolean createIndex(FSIndexComparator comp, String label) throws CASAdminException {
    return createIndex(comp, label, FSIndex.SORTED_INDEX);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Serialization support

  // /**
  // * For one particular view (the one associated with this instance of FsIndexRepositoryImpl),
  // * return an array containing all FSs in any defined index, in this view.
  // * This is intended to be used for serialization.
  // *
  // * The order in which FSs occur in the array does not reflect the order in which they
  // * were added to the repository.
  // *
  // * @param <T> type of Feature Structure
  // * @return a List of all FSs in any defined index, in this view.
  // */
  // public <T extends FeatureStructure> List<T> getIndexedFSs4Serializers() {
  //
  // final ArrayList<TOP> v = new ArrayList<>(); // accumulates fsAddrs from various indexes
  //
  // /* Iterate over index by type, with something in there
  // * and dump all the fss found for that type (excluding subtypes) into v
  // * bag preferred over sorted;
  // */
  // for (int i = 0; i < this.usedIndexes.size(); i++) {
  //// // debug
  //// int vs1 = v.size();
  // getNonSetSingleIndexForUsedType(i).bulkAddTo(v);
  //// for (int di = vs1; di < v.size(); di ++) { // debug
  //// assert v.get(di) != null; // debug verify not null
  //// }
  // }
  //
  // return (List<T>) v;
  // }

  /**
   * For this view, walk the indexed FSs in arbitrary order.
   * 
   * @param action
   *          the action to do on each FS
   */
  public void walkIndexedFSs(Consumer<TOP> action) {
    for (int i = 0; i < this.usedIndexes.size(); i++) {
      for (TOP fs : getNonSetSingleIndexForUsedType(i)) {
        action.accept(fs);
      }
    }
  }

  /**
   * For this view, walk the indexed FSs, sorted by id (e.g. creation time)
   * 
   * @param action
   *          -
   */
  public void walkSortedIndexedFSs(Consumer<TOP> action) {
    List<TOP> fss = new ArrayList<>();
    for (int i = 0; i < this.usedIndexes.size(); i++) {
      for (TOP fs : getNonSetSingleIndexForUsedType(i)) {
        fss.add(fs);
      }
    }
    fss.sort((fs1, fs2) -> Integer.compare(fs1._id, fs2._id));
    for (TOP fs : fss) {
      action.accept(fs);
    }
  }

  public FsIndex_singletype<TOP> getNonSetSingleIndexForType(int typecode) {
    return getIndexesForType(typecode).getNonSetIndex().fsIndex_singletype;
  }

  public FsIndex_singletype<TOP> getNonSetSingleIndexForUsedType(int i) {
    return getIndexesForUsedType(i).getNonSetIndex().fsIndex_singletype;
  }

  // /**
  // * walk routine for all views for all reachable fss
  // * Variations: split apart above/below line ?
  // * do type filtering
  // *
  // * because java doesn't support tail recursion use a stack
  // * @param action
  // */
  // public <T extends TOP> void walkReachableFSs(Consumer<T> action) {
  // PositiveIntSet alreadySeen = new PositiveIntSet_impl();
  // Deque<TOP> toWalk = new ArrayDeque<>();
  // walkReachableFSs(action, alreadySeen, toWalk);
  // }
  //
  // private <T extends TOP> void walkReachableFSs(
  // Consumer<T> action,
  // PositiveIntSet alreadySeen,
  // Deque<TOP> toWalk) {
  // cas.getBaseCAS().indexRepository.<T>walkIndexedFSs(fs -> {
  // alreadySeen.add(fs._id);
  // action.accept(fs);
  // }); // walk the sofas
  // cas.forAllIndexRepos(ir -> ir.walkReachableFSsOneIndexRepo(action, alreadySeen, toWalk));
  // }
  //
  // public <T extends TOP> void walkReachableFSsOneIndexRepo(
  // Consumer<T> action,
  // PositiveIntSet alreadySeen,
  // Deque<TOP> toWalk) {
  //
  // for (int i = 0; i < this.usedIndexes.size(); i++) {
  // FsIndex_singletype<T> index =
  // indexArray[this.usedIndexes.get(i)].<T>getNonSetIndex().fsIndex_singletype;
  // for (T fs : index) {
  // action.accept(fs);
  // }
  // }
  // }

  // *****************************************
  // Adding/removing FS to/from the index
  // *****************************************
  public void addFS(int fsRef) {
    ll_addFS(fsRef);
  }

  @Override
  public void ll_addFS(int fsRef) {
    addFS_common(cas.getFsFromId_checked(fsRef), false); // false === is not an addback call
  }

  @Override
  public void ll_removeFS(int fsRef) {
    removeFS(cas.getFsFromId_checked(fsRef));
  }

  /**
   * @see org.apache.uima.cas.FSIndexRepository#addFS(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public <T extends FeatureStructure> void addFS(T fs) {
    addFS_common((TOP) fs, false);
  }

  // private void incrementIllegalIndexUpdateDetector(int typeCode) {
  // this.detectIllegalIndexUpdates[typeCode] ++;
  // }

  /**
   * @see org.apache.uima.cas.FSIndexRepository#removeFS(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public void removeFS(FeatureStructure fs) {
    removeFS_ret((TOP) fs, INCLUDE_BAG_INDEXES);
    if (fs instanceof AnnotationBase) {
      // fs can only be in 1 view, and has been removed from *all* indexes in that view
      ((FeatureStructureImplC) fs)._resetInSetSortedIndex();
    }
  }

  public void removeFS(int fsRef) {
    removeFS(cas.getFsFromId_checked(fsRef));
  }

//@formatter:off
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
//@formatter:on
  @Override
  public LinearTypeOrderBuilder createTypeSortOrder() {
    final LinearTypeOrderBuilder orderBuilder = new LinearTypeOrderBuilderImpl(this.sii.tsi);
    if (this.sii.defaultOrderBuilder == null) {
      this.sii.defaultOrderBuilder = orderBuilder;
    }
    return orderBuilder;
  }

  @Override
  public <T extends FeatureStructure> LowLevelIndex<T> ll_getIndex(String indexName) {
    return (LowLevelIndex<T>) getIndex(indexName);
  }

  @Override
  public <T extends FeatureStructure> LowLevelIndex<T> ll_getIndex(String indexName, int typeCode) {
    final TypeSystemImpl tsi = this.sii.tsi;
    if (!tsi.isType(typeCode) || !this.cas.ll_isRefType(typeCode)) {
      throw new LowLevelException(LowLevelException.INVALID_INDEX_TYPE, Integer.toString(typeCode));
    }
    return (LowLevelIndex<T>) getIndex(indexName, tsi.ll_getTypeForCode(typeCode));
  }

  @Override
  public final void ll_addFS(int fsRef, boolean doChecks) {
    ll_addFS(fsRef);
  }

  public <T extends TOP> void addback(T fs) {
    addFS_common(fs, true);
  }

  private <T extends TOP> void addFS_common(T fs, boolean isAddback) {
    if (fs._isPearTrampoline()) {
      fs = fs._casView.getBaseFsFromTrampoline(fs);
    }
    TypeImpl ti = ((FeatureStructureImplC) fs)._getTypeImpl();
    final int typeCode = ti.getCode();

    if (typeCode != TypeSystemConstants.sofaTypeCode && cas.isBaseCas()) {
      throw new CASRuntimeException(CASRuntimeException.ILLEGAL_ADD_TO_INDEX_IN_BASE_CAS, fs, cas);
    }
    // https://issues.apache.org/jira/browse/UIMA-4099
    // skip test for wrong view if addback, etc.

    if (CASImpl.traceCow) {
      fs._casView.traceIndexMod(true, fs, isAddback);
    }

    if (!isAddback && (!IS_DISABLE_ENHANCED_WRONG_INDEX_CHECK) && ti.isAnnotationBaseType()) {
      Sofa sofa = (Sofa) ((AnnotationBase) fs).getSofa();
      if (sofa == null) {
        throw new CASRuntimeException(CASRuntimeException.SOFAREF_NOT_SET, fs.toString(3));
      }

      // Check that the annotationBase FS is being added to the proper Cas View
      CASImpl indexView = fs._getView();
      if (indexView.getIndexRepository() != this) {
        /*
         * Error - the Annotation "{0}" is over view "{1}" and cannot be added to indexes associated
         * with the different view "{2}"
         */
        throw new CASRuntimeException(CASRuntimeException.ANNOTATION_IN_WRONG_INDEX, fs.toString(),
                indexView.getViewName(), cas.getViewName());
      }
    }

    // indicate this type's indexes are being modified
    // in case an iterator is simultaneously active over this type
    // incrementIllegalIndexUpdateDetector(typeCode);

    // Get the indexes for the type.
    final ArrayList<FsIndex_iicp<TOP>> indexes = getIndexesForType(typeCode).indexesForType;

    // Add fsRef to all indexes.
    boolean noIndexOrOnlySetindexes = true;
    boolean setOrSorted = false; // set to true if at least one set or sorted index found
    for (FsIndex_iicp<TOP> iicp : indexes) {

      // the indexes for the type are over the type and its subtypes.
      final int indexingStrategy = iicp.fsIndex_singletype.getIndexingStrategy();
      if (isAddback) {
        if (indexingStrategy == FSIndex.BAG_INDEX) {
          continue; // skip adding back to bags - because removes are skipped for bags
        }
      }
      iicp.fsIndex_singletype.insert(fs);

      // remember if we get any index other than set by turning this false;
      if (noIndexOrOnlySetindexes) {
        noIndexOrOnlySetindexes = indexingStrategy == FSIndex.SET_INDEX;
      }

      // remember if we get any set or sorted index by turning this true
      if (setOrSorted == false && indexingStrategy != FSIndex.BAG_INDEX) {
        setOrSorted = true;
      }
    }

    // log even if added back, because remove logs remove, and might want to know it was "reindexed"
    if (this.cas.getCurrentMark() != null) {
      logIndexOperation(fs, true);
    }

    if (setOrSorted) { // only set this bit if this fs is in 1 or more set or sorted indexes
      fs._setInSetSortedIndexed();
    }

    if (isAddback) {
      return;
    }

    // https://issues.apache.org/jira/browse/UIMA-4111
    if (noIndexOrOnlySetindexes) {
      // lazily create a default bag index for this type
      final Type type = this.sii.tsi.ll_getTypeForCode(typeCode);
      final String defIndexName = getAutoIndexNameForType(type);
      final FSIndexComparator comparator = createComparator(); // empty comparator
      comparator.setType(type);
      createIndexNoQuestionsAsked(comparator, defIndexName, FSIndex.DEFAULT_BAG_INDEX);

      // add the FS to the bag index
      // which is the last one added
      ((FsIndex_singletype<T>) (indexes.get(indexes.size() - 1)).fsIndex_singletype).insert(fs);
    }

    if (!this.isUsed.get(typeCode)) {
      // mark this type as being in some indexes
      this.isUsed.set(typeCode);
      // this.isUsedChanged = true;
      this.usedIndexes.add(typeCode);
    }
  }

  private static final String getAutoIndexNameForType(Type type) {
    return "_" + type.getName() + "_DefaultBagGeneratedIndex";
  }

  /**
   * Common remove FS code; all remove operations call this, except bulk remove (flush and
   * removeall...) Removes FS from all indexes in this view (except bag if skipBagIndexes is true)
   * 
   * @param fs
   *          the fs to remove
   * @param skipBagIndexes
   *          set true by protect-indexes style of temporary removal
   * @return true if it was removed
   */
  boolean removeFS_ret(TOP fs, boolean skipBagIndexes) {
    if (skipBagIndexes && !fs._inSetSortedIndex()) {
      return false;
    }
    final int typeCode = fs._getTypeImpl().getCode();
    final IndexesForType i4t = getIndexesForType(typeCode);
    final ArrayList<FsIndex_iicp<TOP>> indexes4type = i4t.indexesForType;

    boolean wasRemoved = false;

  //@formatter:off
    /**
     * some optimization speedup 
     * - skip remove if 
     * 
     *   -- there is no sorted index AND
     *     -- there is no bag index and no set index (no index at all) OR
     *     -- this is a bag index but it doesn't have this fs
     */
  //@formatter:on
    if (i4t.aSortedIndex < 0) {
      int bi = i4t.aBagIndex; // >= 0 if there is a bag index
      if (bi < 0 && !i4t.hasSetIndex) {
        return false; // no indexes defined for this type
      }
      if (bi >= 0 && !i4t.indexesForType.get(bi).fsIndex_singletype.contains(fs)) {
        return false; // not in defined bag index
      }
    }

    /**
     * Actual remove loop over all indexes for this type in this view
     */
    for (FsIndex_iicp<TOP> iicp : indexes4type) {
      FsIndex_singletype<TOP> st = iicp.fsIndex_singletype;
      if (skipBagIndexes && !st.isSetOrSorted()) {
        continue;
      }
      if (st.deleteFS(fs)) {
        wasRemoved = true;
      }
    }
    //
    // int nbrRemoved = idxList.stream().map(iicp -> iicp.fsIndex_singletype)
    // .filter(st -> (!skipBagIndexes) || st.isSetOrSorted())
    // .mapToInt(st -> st.deleteFS(fs) ? 1 : 0).sum();

    if (wasRemoved) {
      // incrementIllegalIndexUpdateDetector(typeCode);
      if (this.cas.getCurrentMark() != null) {
        logIndexOperation(fs, ITEM_REMOVED_FROM_INDEX);
      }

      // oops, might still be indexed in other views if not instance of AnnotationBase
      // reset in caller (removeFromIndexAnyView)
      // if (skipBagIndexes || // means called for remove only from all corruptable indexes
      // fs instanceof AnnotationBase) { // means only indexed in this view
      // fs._resetInSetSortedIndex();
      // }
    }
    if (CASImpl.traceCow) {
      this.cas.traceIndexMod(false, fs, skipBagIndexes);
    }
    return wasRemoved;
  }

  public <T extends FeatureStructure> LowLevelIterator<T> ll_getAllIndexedFS(Type type) {
    return (LowLevelIterator<T>) getAllIndexedFS(type);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndexRepository#getAllIndexedFS(org.apache.uima.cas.Type)
   */
  @Override
  public <T extends FeatureStructure> LowLevelIterator<T> getAllIndexedFS(Type type) {
    final ArrayList<LowLevelIterator<T>> iteratorList = new ArrayList<>();

    getAllIndexedFS(type, iteratorList);

    final int iteratorListSize = iteratorList.size();
    if (iteratorListSize == 0) {
      return (LowLevelIterator<T>) LowLevelIterator.FS_ITERATOR_LOW_LEVEL_EMPTY;
    }
    if (iteratorListSize == 1) {
      return iteratorList.get(0);
    }

    LowLevelIterator<T>[] ia = new LowLevelIterator[iteratorListSize];
    return new FsIterator_aggregation_common<>(iteratorList.toArray(ia), null, null);
  }

  private final <T extends FeatureStructure> void getAllIndexedFS(Type type,
          List<LowLevelIterator<T>> iteratorList) {
    // Strategy: go through the list of all indexes for this type.
    // The list is intentionally ordered when created to have "SORTED" indexes come first.
    //
    // Check all of the Sorted indexes to see if any have a flatten iterator, and if found use that.
    //
    // If no sorted, flattened indexes exist, use any sorted index, but run as unordered to avoid
    // rattling iterators
    //
    // If no sorted index exists, use Bag or Default-bag index. If default-bag, call recursively to
    // get sub-indexes.
    //
    // Note that a default bag index is guaranteed to exist if any FS of Type type were added to the
    // indexes
    // and only a SET index was defined, see https://issues.apache.org/jira/browse/UIMA-4111

    // get all indexes for this type and compute iicps4allFSs

    TypeImpl ti = (TypeImpl) type;
    if (isUsed.get(ti.getCode())) {
      FsIndex_iicp<T> iicp = (FsIndex_iicp<T>) getIndexesForType(ti.getCode()).getNonSetIndex();

      // iicps4allFSs.add(iicp);
      if (null != iicp && !iicp.isEmpty()) {
        LowLevelIterator<T> it = (iicp.getIndexingStrategy() == FSIndex.SORTED_INDEX)
                ? (LowLevelIterator<T>) iicp.iterator(true, true) // order not needed, ignore type
                : (LowLevelIterator<T>) iicp.iterator();
        iteratorList.add(it);
        if (iicp.isDefaultBagIndex()) {
          // We found one of the special auto-indexes which don't inherit down the tree. So, we
          // manually need to traverse the inheritance tree to look for more indexes. Note that
          // this is not necessary when we have a regular index
          addDirectSubtypes(ti, iteratorList);
        }
        return;
      }
    }
    // No index for this type was found at all.
    // Example: You ask for an iterator over "TOP", but no instances of TOP are created,
    // and no index over TOP was ever created.
    // Since the auto-indexes are created on demand for
    // each type, there may be gaps in the inheritance chain. So keep descending the inheritance
    // tree looking for relevant indexes.
    addDirectSubtypes(ti, iteratorList);
  }

  private <T extends FeatureStructure> void addDirectSubtypes(TypeImpl type,
          List<LowLevelIterator<T>> iteratorList) {
    for (TypeImpl subType : type.getDirectSubtypes()) {
      getAllIndexedFS(subType, iteratorList);
    }
    // ((TypeImpl)type).getDirectSubtypes().stream().forEach(subType -> getAllIndexedFS(subType,
    // iteratorList));
  }

  // do this in index creation order
  // needed for backwards compatibility
  // https://issues.apache.org/jira/browse/UIMA-5603 see comment toward end

  @Override
  public Collection<TOP> getIndexedFSs() {
    final ArrayList<CopyOnWriteIndexPart<TOP>> indexes = new ArrayList<>();
    for (int i = 0; i < this.usedIndexes.size(); i++) {
      FsIndex_singletype<TOP> idx = getNonSetSingleIndexForUsedType(i);
      if (idx.size() > 0) {
        indexes.add(idx.getNonNullCow());
      }
    }
    return getCollectionFromCows(indexes);
  }

  @Override
  public <T extends TOP> Collection<T> getIndexedFSs(Class<T> clazz) {
    return getIndexedFSs(cas.getCasType(clazz));
  }

  /**
   * @param type
   *          the type of Feature Structures to include (including subtypes)
   * @return an unmodifiable, unordered set of all indexed (in this view) Feature Structures of the
   *         specified type (including subtypes)
   */
  @Override
  public <T extends TOP> Collection<T> getIndexedFSs(Type type) {
    // collect CopyOnWriteIndexPart s for all index parts for type and its subtypes
    final ArrayList<CopyOnWriteIndexPart<T>> indexes = new ArrayList<>();
    TypeImpl ti = (TypeImpl) type;

    collectCowIndexParts(ti, indexes);
    return getCollectionFromCows(indexes);
  }

  private <T extends TOP> Collection<T> getCollectionFromCows(
          ArrayList<CopyOnWriteIndexPart<T>> indexes) {

    if (indexes.size() == 0) {
      return Collections.emptySet();
    }

    return new AbstractCollection<T>() {

      @Override
      public Iterator<T> iterator() {
        return new Iterator<T>() {
          final int indexesSize = indexes.size();
          int indexesIndex = 0;
          Iterator<T> it = indexes.get(0).iterator();

          @Override
          public boolean hasNext() {
            return indexesIndex < indexesSize;
          }

          @Override
          public T next() {
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            T v = it.next();

            if (!it.hasNext()) {
              indexesIndex++;
              if (indexesIndex == indexesSize) {
                return v;
              }
              it = indexes.get(indexesIndex).iterator();
            }
            return v;
          }

        };
      }

      @Override
      public int size() {
        int r = 0;
        for (CopyOnWriteIndexPart<T> cow : indexes) {
          r += cow.size();
        }
        return r;
      }

      @Override
      public TOP[] toArray() {
        TOP[] r = new TOP[size()];

        int i = 0;
        for (CopyOnWriteIndexPart<T> idx : indexes) {
          i = idx.copyToArray((T[]) r, i);
        }
        return r;
      }

      /*
       * (non-Javadoc)
       * 
       * @see java.util.AbstractCollection#toArray(java.lang.Object[])
       */
      @Override
      public <U> U[] toArray(U[] r) {

        int i = 0;
        for (CopyOnWriteIndexPart<T> idx : indexes) {
          i = idx.copyToArray((T[]) r, i);
        }
        return r;
      }

      /*
       * (non-Javadoc)
       * 
       * @see java.util.AbstractCollection#isEmpty()
       */
      @Override
      public boolean isEmpty() {
        return indexes.isEmpty();
      }

    };
  }

  private <T extends TOP> void collectCowIndexParts(TypeImpl ti,
          ArrayList<CopyOnWriteIndexPart<T>> indexes) {
    FsIndex_iicp<T> iicp;

    if (!isUsed.get(ti.getCode())
            || (iicp = getIndexesForType(ti.getCode()).getNonSetIndex()) == null
            || iicp.isEmpty()) { // could be used, but now empty
      // No index for this type was found at all.
      // Example: You ask for an iterator over "TOP", but no instances of TOP are created,
      // and no index over TOP was ever created.
      // Since the auto-indexes are created on demand for
      // each type, there may be gaps in the inheritance chain. So keep descending the inheritance
      // tree looking for relevant indexes.
      ti.getDirectSubtypes().forEach(type -> collectCowIndexParts(type, indexes));
      return;
    }

    if (iicp.isDefaultBagIndex()) {
      if (iicp.getFsIndex_singleType().size() > 0) {
        indexes.add(iicp.getFsIndex_singleType().getNonNullCow());
      }
      ti.getDirectSubtypes().forEach(type -> collectCowIndexParts(type, indexes));
    } else {
      iicp.collectCowIndexParts(indexes);
    }
  }

  /**
   * Stream instances of all of the non-empty indexes themselves
   * 
   * @param type
   *          - the type to filter the indexes with
   * @return all of the non-empty indexes, one for each sorted or default bag per type
   */
  public Stream<FsIndex_singletype<TOP>> streamNonEmptyIndexes(Type type) {
    TypeImpl ti = (TypeImpl) type;
    if (!isUsed.get(ti.getCode())) {
      return streamNonEmptyDirectSubtypes(ti);
    }
    FsIndex_iicp<TOP> iicp = getIndexesForType(ti.getCode()).getNonSetIndex();
    if (null == iicp || iicp.isEmpty()) {
      return Stream.empty();
    }
    Stream<FsIndex_singletype<TOP>> iicpIndexesStream = iicp.streamNonEmptyIndexes();
    return iicp.isDefaultBagIndex()
            ? Stream.concat(iicpIndexesStream, streamNonEmptyDirectSubtypes(ti))
            : iicpIndexesStream;
  }

  public Stream<FsIndex_singletype<TOP>> streamNonEmptyIndexes(Class<? extends TOP> clazz) {
    return streamNonEmptyIndexes(getCasImpl().getCasType(clazz));
  }

  private Stream<FsIndex_singletype<TOP>> streamNonEmptyDirectSubtypes(TypeImpl ti) {
    Stream<FsIndex_singletype<TOP>> r = null;
    for (TypeImpl subType : ti.getDirectSubtypes()) {
      r = (r == null) ? streamNonEmptyIndexes(subType)
              : Stream.concat(r, streamNonEmptyIndexes(subType));
    }
    return (r == null) ? Stream.empty() : r;
  }

  // next method dropped - rather than seeing if something is in the index, and then
  // later removing it (two lookups), we just conditionally remove it

  // /**
  // * This is used to see if a FS which has a key feature being modified
  // * could corrupt an index in this view. It returns true if found
  // * (sometimes it returns true, even if strictly speaking, there is
  // * no chance of corruption - see below)
  // *
  // * It does this by seeing if this FS is indexed by one or more Set or Sorted
  // * indexes. No need to check bag indexes - they have no keys, so can't be corrupted by key value
  // changes.
  // *
  // * Any sorted index indexes all FSs, so if we find a sorted index, return true if it contains
  // the fs, false otherwise.
  // *
  // * If there are no sorted indexes, if there are one or more set indexes,
  // * return true if any of the set indexes have the fs, false if none have it.
  // *
  // * To speed this up, we keep the set of indexes defined for a type with two additional pieces of
  // meta information:
  // * 1) the first sorted index (or -1 if none are sorted).
  // * 2) a boolean - if there exist any set indexes for this type.
  // *
  // *
  // * @param fs the FS to see if it is in some index that could be corrupted by a key feature value
  // change
  // * @return true if this fs is found in a Set or Sorted index.
  // */
  // public boolean isInSetOrSortedIndexInThisView(FeatureStructureImplC fs) {
  // final TypeImpl ti = fs._getTypeImpl();
  //
  // final IndexesForType i4t = indexArray[ti.getCode()];
  //
  // int si = i4t.aSortedIndex;
  // if (si >= 0) { // have sorted index
  // return i4t.indexesForType.get(si).fsIndex_singletype.contains(fs);
  // }
  //
  // int bi = i4t.aBagIndex;
  // if (bi >= 0) { // have a bag index
  // if (i4t.indexesForType.get(si).fsIndex_singletype.contains(fs) == false) {
  // // not in the index, return false
  // return false;
  // }
  // // is in bag index, there are no sort indexes
  // if (i4t.hasSetIndex) {
  // return true; // approximation - maybe it isn't in the set index.
  // }
  // // no sorted, no set indexes
  // return false;
  // }
  //
  // // no sort, no bag index
  // // because bag is present if any item indexed, this means nothing in the index
  // return false;
  // }

  // see instead removeAndRecord in CASImpl
  // /**
  // * This is called when it has been determined that:
  // * - the fs might be in some indexes
  // * - a change is being made to 1 or more feature values, and those features are being used as
  // keys in one or more indexes
  // *
  // * This happens
  // * - in normal operation, when setting feature values.
  // * - when deserializing a FS using delta CAS which could be modifying an existing one (below the
  // line).
  // *
  // * Although one could check each index's keys against the value which was changing, this isn't
  // done because
  // * - it would slow things down
  // * - this "automatic" removal should be the exception. Users are advised to manually remove the
  // FSs from indexes themselves
  // * before updating features which might be used as keys.
  // *
  // * The removal skips removing FSs from bag or default-bag indexes, because these have no keys.
  // * The add-back also refrains from adding the FSs back to bag indexes.
  // *
  // * The current implementation does not try to determine if any keys are updated with different
  // values,
  // * it just assumes one or more are.
  // *
  // * If the view has nothing other than bag indexes for this type, return false without doing any
  // remove
  // *
  // * @param afs - the FS to see if it is in some index that could be corrupted by a key feature
  // value change
  // * @return true if this fs was in the indexes and will need to be added back.
  // */
  // boolean removeIfInCorrputableIndexInThisView(FeatureStructure afs) {
  // return removeFS_ret((TOP) afs, SKIP_BAG_INDEXES);
  //// TOP fs = (TOP) afs;
  //// TypeImpl ti = fs._getTypeImpl();
  //// final IndexesForType i4t = getIndexesForType(ti.getCode());
  ////
  //// int si = i4t.aSortedIndex;
  //// if (si >= 0) { // then we have a sorted index
  //// return removeFS_ret(fs, SKIP_BAG_INDEXES);
  //// }
  ////
  //// int bi = i4t.aBagIndex;
  //// if (bi >= 0) { // have one or more bag indexes including default bag index, for this type
  //// // use the bag index to stop if it doesn't contain the FS, because bag contains testing is
  // fast..
  //// if (!i4t.indexesForType.get(bi).fsIndex_singletype.contains(fs)) {
  //// return false;
  //// }
  //// if (i4t.hasSetIndex) {
  //// return removeFS_ret(fs, SKIP_BAG_INDEXES);
  //// }
  //// }
  ////
  //// // have no bag index, no sort index (implies index is empty)
  //// return false;
  // }

  // /**
  // * reset the flat index is valid for this type
  // */
  // private void indexUpdated(int typeCode) {
  // flattenedIndexValid.clear(typeCode);
  // }

  /**
   * returns the annotation index for a type which is Annotation or a subtype of it. remembers
   * answer in hashmap annotationIndexes, key = TypeImpl
   * 
   * @param typeCode
   * @return the index for that type
   */
  <T extends AnnotationFS> FsIndex_annotation<T> getAnnotationIndex(TypeImpl ti) {
    // assert(ti.isAnnotationType());
    FsIndex_annotation<Annotation> r = annotationIndexes.get(ti);
    if (r != null) {
      return (FsIndex_annotation<T>) r;
    }

    FsIndex_annotation r1 = (FsIndex_annotation) getIndex(CAS.STD_ANNOTATION_INDEX, ti);
    r = r1;

    annotationIndexes.put(ti, r);
    return (FsIndex_annotation<T>) r;
  }

  private <T extends TOP> void logIndexOperation(T fs, boolean added) {
    this.indexUpdates.add(fs);
    if (added) {
      this.indexUpdateOperation.set(this.indexUpdates.size() - 1, added); // operation was "add"
    }
    this.logProcessed = false;
  }

  // Delta Serialization support
//@formatter:off
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
//@formatter:on
  private void processIndexUpdates() {

    final ProcessedIndexInfo pii = mPii;

    final int len = this.indexUpdates.size();
    for (int i = 0; i < len; i++) {
      final TOP fs = this.indexUpdates.get(i);
      final boolean added = this.indexUpdateOperation.get(i);
      if (added) {
        boolean wasRemoved = pii.fsDeletedFromIndex.remove(fs);
        if (wasRemoved) {
          pii.fsReindexed.add(fs);
        } else if (pii.fsReindexed.contains(fs)) {
          continue; // add on top of reindex is ignored
        } else { // wasn't in deleted, wasn't in reindexed
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

  public Set<TOP> getUpdatedFSs(Set<TOP> items) {
    if (!this.logProcessed) {
      processIndexUpdates();
    }
    return items;
  }

  public Set<TOP> getAddedFSs() {
    return getUpdatedFSs(mPii.fsAddedToIndex);
  }

  public Set<TOP> getDeletedFSs() {
    return getUpdatedFSs(mPii.fsDeletedFromIndex);
  }

  public Set<TOP> getReindexedFSs() {
    return getUpdatedFSs(mPii.fsReindexed);
  }

  public boolean isModified() {
    if (!this.logProcessed) {
      processIndexUpdates();
    }
    final ProcessedIndexInfo pii = mPii;
    return ((pii.fsAddedToIndex.size() > 0) || (pii.fsDeletedFromIndex.size() > 0)
            || (pii.fsReindexed.size() > 0));
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " [" + cas + "]";
  }

  // public Comparator<AnnotationFS> getAnnotationComparator() {
  // if (null == this.sii.annotationComparator) {
  // @SuppressWarnings("unchecked")
  // final FsIndex_iicp<AnnotationFS> iicp =
  // (FsIndex_iicp<AnnotationFS>) this.name2indexMap.get(CAS.STD_ANNOTATION_INDEX);
  // this.sii.annotationComparator = (FSIntArrayIndex<AnnotationFS>)(iicp.fsIndex_singletype);
  // }
  // return this.sii.annotationComparator;
  // }

  public Comparator<TOP> getAnnotationFsComparator(FSComparators withId,
          FSComparators withTypeOrder) {
    Comparator<TOP> r = getCachedComparator(withId, withTypeOrder);
    if (r == null) {
      r = createAnnotationFsComparator(withId, withTypeOrder);
      setCachedComparator(withId, withTypeOrder, r);
    }
    return r;
  }

  private Comparator<TOP> createAnnotationFsComparator(FSComparators withId,
          FSComparators withTypeOrder) {
    LinearTypeOrder lto = (withTypeOrder == FSComparators.WITH_TYPE_ORDER) ? getDefaultTypeOrder()
            : null;
    if (withId == FSComparators.WITH_ID) {
      if (withTypeOrder == FSComparators.WITH_TYPE_ORDER) {
        return (fs1, fs2) -> (fs1 == fs2) ? 0
                : ((Annotation) fs1).compareAnnotationWithId((Annotation) fs2, lto);
      } else {
        return (fs1, fs2) -> (fs1 == fs2) ? 0
                : ((Annotation) fs1).compareAnnotationWithId((Annotation) fs2);
      }
    } else {
      if (withTypeOrder == FSComparators.WITH_TYPE_ORDER) {
        return (fs1, fs2) -> (fs1 == fs2) ? 0
                : ((Annotation) fs1).compareAnnotation((Annotation) fs2, lto);
      } else {
        return (fs1, fs2) -> (fs1 == fs2) ? 0
                : ((Annotation) fs1).compareAnnotation((Annotation) fs2);
      }
    }
  }

  public Comparator<TOP> getAnnotationFsComparatorWithoutId() {
    Comparator<TOP> r = this.sii.annotationFsComparatorWithoutId;
    // lazy creation
    if (null != r) {
      return r;
    }
    return createAnnotationFsComparator();
  }

  Comparator<TOP> getAnnotationFsComparatorWithId() {
    Comparator<TOP> r = this.sii.annotationFsComparatorWithId;
    // lazy creation
    if (null != r) {
      return r;
    }
    return createAnnotationFsComparatorWithId();
  }

  private Comparator<TOP> createAnnotationFsComparator() {
    final LinearTypeOrder lto = getDefaultTypeOrder(); // used as constant in comparator

    if (!V2_ANNOTATION_COMPARE_TYPE_ORDER && lto.isEmptyTypeOrder()) {
      return this.sii.annotationFsComparatorWithoutId = (fsx1, fsx2) -> {
        if (fsx1 == fsx2)
          return 0;
        Annotation fs1 = (Annotation) fsx1;
        Annotation fs2 = (Annotation) fsx2;
        return fs1.compareAnnotation(fs2);
      };

    } else {
      return this.sii.annotationFsComparatorWithoutId = (fsx1, fsx2) -> {
        if (fsx1 == fsx2)
          return 0;
        Annotation fs1 = (Annotation) fsx1;
        Annotation fs2 = (Annotation) fsx2;

        return fs1.compareAnnotation(fs2, lto);
      };
    }
  }

  // public boolean isAnnotationComparator_usesTypeOrder() {
  // final LinearTypeOrder lto = getDefaultTypeOrder();
  // return V2_ANNOTATION_COMPARE_TYPE_ORDER || !lto.isEmptyTypeOrder();
  // }

  // unrolled because of high frequency use
  private Comparator<TOP> createAnnotationFsComparatorWithId() {
    final LinearTypeOrder lto = getDefaultTypeOrder(); // used as constant in comparator

    if (!V2_ANNOTATION_COMPARE_TYPE_ORDER && lto.isEmptyTypeOrder()) {
      this.sii.annotationFsComparatorWithId = (fsx1, fsx2) -> {
        if (fsx1 == fsx2)
          return 0;
        final Annotation fs1 = (Annotation) fsx1;
        final Annotation fs2 = (Annotation) fsx2;
        return fs1.compareAnnotationWithId(fs2);
      };

    } else {
      this.sii.annotationFsComparatorWithId = (fsx1, fsx2) -> {
        if (fsx1 == fsx2)
          return 0;
        final Annotation fs1 = (Annotation) fsx1;
        final Annotation fs2 = (Annotation) fsx2;
        return fs1.compareAnnotationWithId(fs2, lto);
      };
    }
    return this.sii.annotationFsComparatorWithId;
  }

  /**
   * Get the FsIndex_iicp for a given typeCode, indexingStrategy, and comparator (type ignored)
   * 
   * @param typeCode
   *          -
   * @param indexingStrategy
   *          -
   * @param comp
   *          -
   * @param <T>
   *          type of Feature Structure
   * @return -
   */
  public <T extends FeatureStructure> FsIndex_iicp<T> getIndexBySpec(int typeCode,
          int indexingStrategy, FSIndexComparatorImpl comp) {
    return getIndexesForType(typeCode).getIndexExcludingType(indexingStrategy, comp);
  }

  private void removeIndexBySpec(int typeCode, int indexingStrategy, FSIndexComparatorImpl comp) {
    getIndexesForType(typeCode).removeIndexExcludingType(indexingStrategy, comp);
  }

  public TypeSystemImpl getTypeSystemImpl() {
    return sii.tsi;
  }

  public CASImpl getCasImpl() {
    return cas;
  }

  private Comparator<TOP> getCachedComparator(FSComparators withId, FSComparators withTypeOrder) {
    if (withId == FSComparators.WITH_ID) {
      if (withTypeOrder == FSComparators.WITH_TYPE_ORDER) {
        return this.sii.annotationFsComparatorWithId;
      } else {
        return this.sii.annotationFsComparatorNoTypeWithId;
      }
    } else {
      if (withTypeOrder == FSComparators.WITH_TYPE_ORDER) {
        return this.sii.annotationFsComparatorWithoutId;
      } else {
        return this.sii.annotationFsComparatorNoTypeWithoutId;
      }
    }
  }

  private void setCachedComparator(FSComparators withId, FSComparators withTypeOrder,
          Comparator<TOP> c) {
    if (withId == FSComparators.WITH_ID) {
      if (withTypeOrder == FSComparators.WITH_TYPE_ORDER) {
        this.sii.annotationFsComparatorWithId = c;
      } else {
        this.sii.annotationFsComparatorNoTypeWithId = c;
      }
    } else {
      if (withTypeOrder == FSComparators.WITH_TYPE_ORDER) {
        this.sii.annotationFsComparatorWithoutId = c;
      } else {
        this.sii.annotationFsComparatorNoTypeWithoutId = c;
      }
    }
  }

}
