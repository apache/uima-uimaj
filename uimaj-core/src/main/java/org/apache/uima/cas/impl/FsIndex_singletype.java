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

import static java.lang.String.format;
import static java.lang.System.identityHashCode;

import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FSComparators;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.LinearTypeOrder;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.util.Level;

/**
 * The common (among all index kinds - set, sorted, bag) info for an index over
 * 1 type (excluding subtypes)
 * 
 * SubClasses FsIndex_bag, FsIndex_flat, FsIndex_set_sorted, define the actual
 * index repository for each kind.
 * 
 * @param <T>
 *          the Java cover class type for this index, passed along to (wrapped)
 *          iterators producing Java cover classes
 */
public abstract class FsIndex_singletype<T extends FeatureStructure> 
    extends AbstractCollection<T>
    implements Comparator<FeatureStructure>, LowLevelIndex<T> {

  private final static String[] indexTypes = new String[] {
      "Sorted", "Set", "Bag", "DefaultBag" };

  /**
   * shares equal FSIndexComparatorImpl comparatorForIndexSpecs objects updates
   * and accesses are synchronized
   */
  private final static WeakHashMap<FSIndexComparatorImpl, WeakReference<FSIndexComparatorImpl>> comparatorCache = new WeakHashMap<>();

  private final int indexType; // Sorted,
                               // Set,
                               // Bag,
                               // Default-bag,
                               // etc.

  // A reference to the low-level CAS.
  final protected CASImpl casImpl;
  
  // comparators are over TOP since it's allowed to compare
  //   items with types in the superchain of T (up to TOP)
  /**
   * comparator for an index, passed in as an argument to the constructor
   */
  final protected FSIndexComparatorImpl comparatorForIndexSpecs;

  final protected Comparator<TOP> comparatorWithID;
  final protected Comparator<TOP> comparatorWithoutID;

  /**
   * comparator (with id) (ignoring typeorder) - used within one type
   */
  final protected Comparator<TOP> comparatorNoTypeWithID;

  /**
   * comparator (without id) (ignoring typeorder) - used within one type - used
   * for iterator operations where the type is requested to be ignored
   */
  final protected Comparator<TOP> comparatorNoTypeWithoutID;

  public final boolean isAnnotIdx;

  /***********
   * Info about Index Comparator (not used for bag *********** Index into these
   * arrays is the key number (indexes can have multiple keys)
   **********************************************************************/
  // For each key, the int code of the type of that key.
  final private Object[] keys; // either a FeatImpl or a LinearTypeOrder;

  final private int[] keyTypeCodes;
  // For each key, the comparison to use. 
  final private boolean[] isReverse; // true = reverse, false = standard

  // /** true if one of the keys is the linear type order key */
  // private boolean hasLinearTypeOrderKey = false;
  // /** true if there is an linear type order key, but no type priorities are
  // specified */
  // private boolean hasEmptyLinearTypeOrderKey = false;

  protected final TypeImpl type; // The type of this
  final private int typeCode;

  /**
   * common copy on write instance or null; starts out as null Iterator creation
   * initializes (if null). A subsequent Modification to index, if this is not
   * null: call cow.makeCopy(); set wr_cow = null do the modification index
   * clear/flush - set to null;
   * 
   * Weak ref so that after iterator is GC'd, and no ref's exist, this becomes
   * null, so that future mods no longer need to do extra work.
   */
  protected WeakReference<CopyOnWriteIndexPart<T>> wr_cow = null;

  @Override
  public String toString() {
    String kind = (indexType >= 0 && indexType < 4) ? indexTypes[indexType] : "Invalid";
    return this.getClass().getSimpleName() + "(" + kind + ")[" + type.getShortName() + "]";
  }

  // // never called
  // // declared private to block external calls
  // @SuppressWarnings("unused")
  // private FsIndex_singletype() {
  // this.indexType = 0; // must do because it's final
  // this.casImpl = null;
  // this.type = null;
  // this.typeCode = 0;
  // comparatorForIndexSpecs = null;
  // keys = null;
  // keyTypeCodes = null;
  // isReverse = null;
  // }

  /**
   * Constructor for FsIndex_singletype.
   * 
   * @param cas
   *          -
   * @param type
   *          -
   * @param indexType
   *          -
   * @param comparatorForIndexSpecs
   *          -
   */
  protected FsIndex_singletype(CASImpl cas, Type type, int indexType,
      FSIndexComparator comparatorForIndexSpecs) {
    super();
    this.indexType = indexType;
    this.casImpl = cas;
    this.type = (TypeImpl) type;
    this.typeCode = ((TypeImpl) type).getCode();
    FSIndexComparatorImpl compForIndexSpecs = (FSIndexComparatorImpl) comparatorForIndexSpecs;
    this.comparatorForIndexSpecs = Misc.shareExisting(compForIndexSpecs, comparatorCache);
    // this.comparatorForIndexSpecs = compForIndexSpecs/*.copy()*/;

    // Initialize the comparator info.
    final int nKeys = this.comparatorForIndexSpecs.getNumberOfKeys();
    this.keys = new Object[nKeys];
    this.keyTypeCodes = new int[nKeys];
    this.isReverse = new boolean[nKeys];

    if (!this.comparatorForIndexSpecs.isValid()) {
      isAnnotIdx = false;
      comparatorWithID = null;
      comparatorWithoutID = null;
      comparatorNoTypeWithID = null;
      comparatorNoTypeWithoutID = null;
      return;
    }

    for (int i = 0; i < nKeys; i++) {
      int keyType = comparatorForIndexSpecs.getKeyType(i);
      final Object k = (keyType == FSIndexComparator.FEATURE_KEY)
          ? (FeatureImpl) this.comparatorForIndexSpecs.getKeyFeature(i)
          : this.comparatorForIndexSpecs.getKeyTypeOrder(i);
      keys[i] = k;
      if (k instanceof FeatureImpl) {
        keyTypeCodes[i] = ((TypeImpl) ((FeatureImpl) k).getRange()).getCode();
      } else {
        // key is linear type order
        // hasLinearTypeOrderKey = true;
        // hasEmptyLinearTypeOrderKey = ((LinearTypeOrder)k).isEmptyTypeOrder();
      }
      isReverse[i] = this.comparatorForIndexSpecs
          .getKeyComparator(i) == FSIndexComparator.REVERSE_STANDARD_COMPARE;
    }

    FSIndexRepositoryImpl ir = this.casImpl.indexRepository;

    if (ir.isAnnotationIndex(comparatorForIndexSpecs, indexType)) {
      comparatorWithID = ir.getAnnotationFsComparator(FSComparators.WITH_ID,
          FSComparators.WITH_TYPE_ORDER);
      comparatorWithoutID = ir.getAnnotationFsComparator(FSComparators.WITHOUT_ID,
          FSComparators.WITH_TYPE_ORDER);
      comparatorNoTypeWithID = ir.getAnnotationFsComparator(FSComparators.WITH_ID,
          FSComparators.WITHOUT_TYPE_ORDER);
      comparatorNoTypeWithoutID = ir.getAnnotationFsComparator(FSComparators.WITHOUT_ID,
          FSComparators.WITHOUT_TYPE_ORDER);
      isAnnotIdx = true;
    } else {
      // NOT ANNOTATION INDEX      
      isAnnotIdx = false;

      if (indexType == BAG_INDEX) {
        comparatorNoTypeWithID = comparatorNoTypeWithoutID = comparatorWithID = comparatorWithoutID = 
            (o1, o2) -> ((FsIndex_bag)this).compare(o1, o2);
      } else {
        comparatorWithoutID = (o1, o2) -> compare(o1, o2, IS_TYPE_ORDER);
  
        comparatorWithID = (indexType == FSIndex.SORTED_INDEX) ? (o1, o2) -> {
  
          final int c = compare(o1, o2, IS_TYPE_ORDER);
          // augment normal comparator with one that compares IDs if everything
          // else equal
          return (c == 0) ? (Integer.compare(o1._id(), o2._id())) : c;
        }
  
            : comparatorWithoutID;
  
        comparatorNoTypeWithoutID = (o1, o2) -> compare(o1, o2, !IS_TYPE_ORDER);
  
        comparatorNoTypeWithID = (indexType == FSIndex.SORTED_INDEX) ? (o1, o2) -> {
  
          final int c = compare(o1, o2, !IS_TYPE_ORDER);
          // augment normal comparator with one that compares IDs if everything
          // else equal
          return (c == 0) ? (Integer.compare(o1._id(), o2._id())) : c;
        }
  
            : comparatorWithID;
      }
    }

  }

  /**
   * Adding FS to an index. not in upper interfaces because it's internal use
   * only - called via addToIndexes etc.
   * 
   * @param fs
   *          the fs to be added
   */
  abstract void insert(T fs); // not in upper interfaces because it's internal
                              // use only

  // /**
  // * @param fs - the Feature Structure to be removed.
  // * Only this exact Feature Structure is removed (this is a stronger test
  // than, for example,
  // * what moveTo(fs) does, where the fs in that case is used as a template).
  // * It is not an error if this exact Feature Structure is not in an index.
  // * @return true if something was removed, false if not found
  // */
  // boolean remove(int fs) {
  // return deleteFS((T) getCasImpl().getFsFromId_checked(fs));
  // }

  /**
   * @param fs
   *          - the Feature Structure to be removed. Only this exact Feature
   *          Structure is removed (this is a stronger test than, for example,
   *          what moveTo(fs) does, where the fs in that case is used as a
   *          template). It is not an error if this exact Feature Structure is
   *          not in an index.
   * @return true if something was removed, false if not found
   */
  abstract boolean deleteFS(T fs);

  @Override
  public LowLevelIterator<T> iterator(FeatureStructure initialPositionFs) {
    LowLevelIterator<T> fsIt = iterator();
    fsIt.moveTo(initialPositionFs);
    return fsIt;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelIndex#getComparator()
   */
  @Override
  public Comparator<TOP> getComparator() {
    return comparatorWithoutID;
  }

  @Override
  public FSIndexComparator getComparatorForIndexSpecs() {
    return this.comparatorForIndexSpecs;
  }

  public FSIndexComparatorImpl getComparatorImplForIndexSpecs() {
    return this.comparatorForIndexSpecs;
  }

  @Override
  public int getIndexingStrategy() {
    return this.indexType;
  }

  /**
   * @param fs1
   *          -
   * @param fs2
   *          -
   * @return 0 if equal, &lt; 0 if fs1 &lt; fs2, &gt; 0 if fs1 &gt; fs2
   */
  @Override
  public int ll_compare(int fs1, int fs2) {
    return this.compare(fs1, fs2);
  }

  /**
   * @param fs1
   *          -
   * @param fs2
   *          -
   * @return 0 if equal, &lt; 0 if fs1 &lt; fs2, &gt; 0 if fs1 &gt; fs2
   */
  public int compare(int fs1, int fs2) {
    return compare(casImpl.getFsFromId_checked(fs1), casImpl.getFsFromId_checked(fs2));
  }

  // /**
  // * @see org.apache.uima.cas.FSIndex#compare(FeatureStructure,
  // FeatureStructure)
  // *
  // * Note: this is the "general" compare, based on
  // * runtime interpreting the index's definition of its Keys, and the type
  // ordering.
  // *
  // * Annotation Index instances should use the custom comparators
  // */
  // @Override
  // public int compare(FeatureStructure afs1, FeatureStructure afs2) {
  // return compare(afs1, afs2, false); // don't ignore type
  // }

  int compare(FeatureStructure afs1, FeatureStructure afs2, boolean ignoreType) {

    if (afs1 == afs2) {
      return 0;
    }

    FeatureStructureImplC fs1 = (FeatureStructureImplC) afs1;
    FeatureStructureImplC fs2 = (FeatureStructureImplC) afs2;

    /**
     * for each key defined by this index: if Feature: Switch by type: float,
     * get the value: fs1.getXXX, compare
     */
    int i = -1;
    for (Object key : keys) {
      int result = 0;
      i++;
      if (key instanceof FeatureImpl) {
        FeatureImpl fi = (FeatureImpl) key;
        if (fi.getRange().isStringOrStringSubtype()) { // string and string
                                                       // subtypes
          result = Misc.compareStrings(fs1._getStringValueNc(fi), fs2._getStringValueNc(fi));
        } else {
          switch (keyTypeCodes[i]) {
          case TypeSystemConstants.booleanTypeCode:
            result = Integer.compare(fs1._getBooleanValueNc(fi) ? 1 : 0,
                fs2._getBooleanValueNc(fi) ? 1 : 0);
            break;
          case TypeSystemConstants.byteTypeCode:
            result = Integer.compare(fs1._getByteValueNc(fi), fs2._getByteValueNc(fi));
            break;
          case TypeSystemConstants.shortTypeCode:
            result = Integer.compare(fs1._getShortValueNc(fi), fs2._getShortValueNc(fi));
            break;
          case TypeSystemConstants.intTypeCode:
            result = Integer.compare(fs1._getIntValueNc(fi), fs2._getIntValueNc(fi));
            break;
          case TypeSystemConstants.longTypeCode:
            result = Long.compare(fs1._getLongValueNc(fi), fs2._getLongValueNc(fi));
            break;
          case TypeSystemConstants.floatTypeCode:
            result = Float.compare(fs1._getFloatValueNc(fi), fs2._getFloatValueNc(fi));
            break;
          case TypeSystemConstants.doubleTypeCode:
            result = Double.compare(fs1._getDoubleValueNc(fi), fs2._getDoubleValueNc(fi));
            break;
          // next is compared above before the switch
          // case TypeSystemConstants.stringTypeCode:
          // result = Misc.compareStrings(fs1.getStringValueNc(fi),
          // fs2.getStringValueNc(fi));
          // break;
          } // end of switch
        }
      } else { // is type order compare
        if (ignoreType) {
          result = 0;
        } else {
          result = ((LinearTypeOrder) key).compare(fs1, fs2);
        }
      }

      if (result == 0) {
        continue;
      }

      return (isReverse[i]) ? ((result < 0) ? 1 : -1) : ((result > 0) ? 1 : -1);
    } // of for loop iterating over all compare keys
    return 0; // all keys compare equal
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((comparatorForIndexSpecs == null) ? 0 : comparatorForIndexSpecs.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    FsIndex_singletype<?> other = (FsIndex_singletype<?>) obj;
    if (comparatorForIndexSpecs == null) {
      if (other.comparatorForIndexSpecs != null)
        return false;
    } else if (!comparatorForIndexSpecs.equals(other.comparatorForIndexSpecs))
      return false;
    return true;
  }

  /**
   * @see org.apache.uima.cas.FSIndex#getType()
   * @return The type of feature structures in this index.
   */
  @Override
  public Type getType() {
    return this.type;
  }

  public TypeImpl getTypeImpl() {
    return this.type;
  }

  int getTypeCode() {
    return this.typeCode;
  }

  // /** true if there is a type order key, and no type priorities are defined
  // */
  // public boolean hasEmptyLinearTypeOrderKey() {
  // return hasEmptyLinearTypeOrderKey;
  // }

  /**
   * For serialization: get all the items in this index and bulk add to an
   * List&lt;T&gt;
   * 
   * @param v
   *          the set of items to add
   */
  protected abstract void bulkAddTo(List<T> v);

  @Override
  public LowLevelIterator<T> ll_iterator(boolean ambiguous) {
    if (ambiguous) {
      return this.ll_iterator();
    }

    return null;
  }

  @Override
  public CASImpl getCasImpl() {
    return this.casImpl;
  }

  @Override
  public FSIndex<T> withSnapshotIterators() {
    // Is a no-op because this is a single type index.
    // should never be called
    // this is an artifact of the fact that FsIndex_singletype implements
    // FSIndex interface
    return this;
  }

  boolean isSetOrSorted() {
    return indexType == FSIndex.SET_INDEX || indexType == FSIndex.SORTED_INDEX;
  }

  public boolean isSorted() {
    return indexType == FSIndex.SORTED_INDEX;
  }

  /**
   * Differs from flush in that it manipulates flags in the FSs to indicate
   * removed.
   * This can only be done if we can guarantee the FS is not indexed **in any view**.
   * We do that by only resetting if it is a subtype of annotation base, which is guaranteed
   *   to be indexed only in 1 view.
   * 
   */
  void removeAll() {
    FSIterator<T> it = iterator();
    if (type instanceof TypeImpl_annotBase) {
      // only indexed in one index if at all
      while (it.hasNext()) {
        ((TOP) it.nextNvc())._resetInSetSortedIndex();
      }
    }
    flush();
  }

  protected CopyOnWriteIndexPart<T> getNonNullCow() {
    CopyOnWriteIndexPart<T> n = getCopyOnWriteIndexPart();
    if (n != null) {
      if (CASImpl.traceCow) {
        this.casImpl.traceCowReinit("reuse", this);
      }
      return n;
    }

    if (CASImpl.traceCow) {
      this.casImpl.traceCowReinit("getNew", this);
    }

    // null means index updated since iterator was created, need to make new cow
    // and use it
    n = createCopyOnWriteIndexPart(); // new CopyOnWriteObjHashSet<TOP>(index);
    wr_cow = new WeakReference<>(n);
    return n;
  }

  /**
   * @return the copy-on-write wrapper for an index part if it exists for this
   *         index, or null
   */
  public CopyOnWriteIndexPart<T> getCopyOnWriteIndexPart() {
    return (wr_cow == null) ? null : wr_cow.get();
  }

  protected abstract CopyOnWriteIndexPart<T> createCopyOnWriteIndexPart();

  /**
   * Called just before modifying an index if wr_cow has a value, tell that
   * value to create a preserving copy of the index part, and set wr_cow to null
   */
  protected void maybeCopy() {
    if (wr_cow != null) {
      CopyOnWriteIndexPart v = wr_cow.get();
      if (v != null) {
        v.makeReadOnlyCopy();
      }
      wr_cow = null;
    }
  }

  @Override
  public void flush() {
//   maybeCopy(); // https://issues.apache.org/jira/browse/UIMA-5687
    wr_cow = null;
    // casImpl.indexRepository.isUsedChanged = true;
  }

  /* (non-Javadoc)
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  /**
   * This is required to avoid compilation error (but not in Eclipse) due to 
   * ambiguous interface inheritance from both FeatureStructure and Comparator
   */
  @Override
  public abstract int compare(FeatureStructure o1, FeatureStructure o2);
  
  private static final AtomicInteger strictTypeSourceCheckMessageCount = new AtomicInteger(0);
  
  protected final void assertFsTypeMatchesIndexType(FeatureStructure fs, String operation) {
    TypeImpl fsType = ((TOP)fs)._getTypeImpl();
    if (fsType != this.type) {
      String message = String.format(
              "%s operation using a feature structure of type [%s](%d) from type system [%s] on index using "
              + "different type system [%s] is not supported.", operation,
              fsType.getName(), fsType.getCode(), format("<%,d>", identityHashCode(fsType.getTypeSystem())), 
              format("<%,d>", identityHashCode(this.type.getTypeSystem())));
    
      if (TypeSystemImpl.IS_ENABLE_STRICT_TYPE_SOURCE_CHECK) {
        throw new IllegalArgumentException(message);
      }
      else {
        Misc.decreasingWithTrace(strictTypeSourceCheckMessageCount, message, UIMAFramework.getLogger());
      }
    }
  }

  /// **
  // * Common part of iterator creation
  // */
  // protected CopyOnWriteIndexPart setupIteratorCopyOnWrite() {
  // CopyOnWriteIndexPart cow_index_part = getCopyOnWriteIndexPart();
  // if (null == wr_cow || null == wr_cow.get()) {
  // cow_index_part = createCopyOnWriteIndexPart();
  // wr_cow = new WeakReference<>(cow_index_part);
  // }
  // return cow_index_part;
  // }

}
