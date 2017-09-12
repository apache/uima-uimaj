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
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Vector;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.LinearTypeOrder;
import org.apache.uima.cas.admin.LinearTypeOrderBuilder;
import org.apache.uima.cas.impl.FSIndexFlat.FSIteratorFlat;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.internal.util.ComparableIntPointerIterator;
import org.apache.uima.internal.util.Int2IntArrayMapFixedSize;
import org.apache.uima.internal.util.IntComparator;
import org.apache.uima.internal.util.IntPointerIterator;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.PositiveIntSet;
import org.apache.uima.internal.util.PositiveIntSet_impl;
import org.apache.uima.util.Misc;

/**
 * There is one instance of this class per CAS View.
 * 
 * Some parts of the data here are shared between all views of a CAS.
 * 
 * Many things refer to specific types, and their associated Java Cover classes.
 *    There are 2 kinds of cover classes:
 *        If JCas is not being used, or if there is no JCas cover class defined for a type, then
 *        the Java class FeatureStructureImplC is used as the cover class.
 *        
 *        If the JCas is being used, then the JCas cover type (a subtype of TOP) is used as the cover class.
 *        
 *        Both of these classes inherit from FeatureStructureImpl (an abstract class)
 *        
 *    Both of these classes implement the common interface FeatureStructure.
 *
 * Generic typing: 
 *   User facing APIs can make use of the (JCas) Java cover types, for indexes and iterators over them
 *   The general generic type used is typically written here as T extends FeatureStructure, where
 *   FeatureStructure is the super interface of all cover types (JCas and non-JCas).  
 *
 *   APIs having no reference to Java cover types (i.e., low level iterators) are not generic, unless they
 *   are needed to be to pass along the associated type to other APIs. 
 */
public class FSIndexRepositoryImpl implements FSIndexRepositoryMgr, LowLevelIndexRepository {

  private final static boolean DEBUG = false;
  /**
   * The default size of an index.
   */
  public static final int DEFAULT_INDEX_SIZE = 16;


  /**
   * Define this JVM property to allow adding the same identical FS to Set and Sorted indexes more than once.  
   */

  public static final String ALLOW_DUP_ADD_TO_INDEXES = "uima.allow_duplicate_add_to_indexes";
  
  // accessed by FSIntArrayIndex and tests
  public static final boolean IS_ALLOW_DUP_ADD_2_INDEXES  =  Misc.getNoValueSystemProperty(ALLOW_DUP_ADD_TO_INDEXES);
  
  public static final String DISABLE_ENHANCED_WRONG_INDEX = "uima.disable_enhanced_check_wrong_add_to_index";
 
  private static final boolean IS_DISABLE_ENHANCED_WRONG_INDEX_CHECK = Misc.getNoValueSystemProperty(DISABLE_ENHANCED_WRONG_INDEX);

  /**
   * Kinds of extra functions for iterators
   */
  public enum IteratorExtraFunction {
    SNAPSHOT,  // snapshot iterators
    UNORDERED, // unordered iterators - means unordered among subtypes, but each subtype may have an order
  }
    
  private static final FSIterator emptyFSIterator = new FSIteratorImplBase<FeatureStructure>() {

    @Override
    public boolean isValid() {return false;}

    @Override
    public FeatureStructure get() throws NoSuchElementException { throw new NoSuchElementException(); }

    @Override
    public void moveToNext() {}

    @Override
    public void moveToPrevious() {}

    @Override
    public void moveToFirst() {}

    @Override
    public void moveToLast() {}

    @Override
    public void moveTo(FeatureStructure fs) {}

    @Override
    public FSIterator<FeatureStructure> copy() {
      return this;
    }
    @Override
    <TT extends AnnotationFS> void moveTo(int begin, int end) {}
  };

  private static final LowLevelIterator emptyLlIterator = new FSIntIteratorImplBase<FeatureStructure>(null, null) {

    @Override
    public boolean isValid() { return false; }

    @Override
    public int get() { throw new NoSuchElementException(); }

    @Override
    public void moveTo(int i) {}

    @Override
    public void moveToFirst() {}

    @Override
    public void moveToLast() {}

    @Override
    public Object copy() { return this; }

    @Override
    public void moveToNext() {}

    @Override
    public void moveToPrevious() {}

    @Override
    public int ll_indexSize() { return 0; }

  };
  // Implementation note: the use of equals() here is pretty hairy and
  // should probably be fixed. We rely on the fact that when two
  // FSIndexComparators are compared, the type of the comparators is
  // ignored! A fix for this would be to split the FSIndexComparator
  // class into two classes, one for the key-comparator pairs, and one
  // for the combination of the two. Note also that we compare two
  // IndexIteratorCachePairs by comparing their
  // index.getComparator()s.

  /**
   * IndexIteratorCachePair (iicp)
   * 
   * A pair of an leaf index and an iterator cache. An iterator cache is the set of all leaf-indexes necessary
   * to create an iterator for the type of the index.
   * 
   *  The cache includes the index for the type of this index, as well as all subtypes.
   *  
   * compareTo() is based on types and the comparator of the index.
   * 
   * T is the Java cover class of the top type (root) in the index set
   * 
   * Also includes a lazily initialized reference to a corresponding FSIndexFlat instance.
   * 
   * This class is package private to share with FSIndexFlat
   * For Internal Use
   */
  class IndexIteratorCachePair<T extends FeatureStructure> 
               implements Comparable<IndexIteratorCachePair<? extends FeatureStructure>> {

    /**
     *  The "root" index, i.e., index of the type of the iterator.
     *  default visibility to make it accessable by FSIndexFlat
     */
    final private FSLeafIndexImpl<T> fsLeafIndex;
    
    FSLeafIndexImpl<T> getFsLeafIndex() {
      return fsLeafIndex;
    }

    /**
     * A list of indexes (the sub-indexes that we need for an iterator). 
     * I.e., one index for each type that's subsumed by the iterator's type; 
     * includes the iterator's type leaf index too.
     */
    private ArrayList<FSLeafIndexImpl<? extends T>> cachedSubFsLeafIndexes = null;
    
    // VOLATILE to permit double-checked locking technique
    private volatile boolean isIteratorCacheSetup = false;
    
    /**
     * Link to associated flattened information, set up lazily, only if this level has an iterator
     */
    private FSIndexFlat<T> flatIndex = null;
    
    /**
     * The type codes corresponding to the cachedSubFsLeafIndexes, set up lazily
     */
    int[] sortedTypeCodes;
    
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("IndexIteratorCachePair, index=");
      sb.append(fsLeafIndex).append('\n');
      if (!isIteratorCacheSetup) {
        sb.append(" cache not set up yet");
      } else {  
        int len = Math.min(3,  cachedSubFsLeafIndexes.size());
        for (int i = 0; i < len; i++) {
          FSLeafIndexImpl<? extends T> lii = cachedSubFsLeafIndexes.get(i); 
          sb.append("  cache ").append(i++);
          sb.append("  ").append(lii).append('\n');
        }
        if (cachedSubFsLeafIndexes.size() > 3) {
          sb.append(" ... and " + (cachedSubFsLeafIndexes.size() - 3) + " more\n");
        }
      }
      return sb.toString();
    }

    private IndexIteratorCachePair(FSLeafIndexImpl<T> fsLeafIndex) {
      this.fsLeafIndex = fsLeafIndex;
//      setAndTestMask = FSIndexRepositoryImpl.this.cas.getTypeSystemImpl().getSetAndTestMasks(fsLeafIndex.getTypeCode());
    }

    // Two IICPs are equal iff their index comparators are equal AND their
    // indexing strategy is the same.
    // Equal is used when creating the index iterator cache to select
    //   from the set of all IndexIteratorCachePairs for a particular type,
    //   the one that goes with the same index definition
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof IndexIteratorCachePair)) {
        return false;
      }
      @SuppressWarnings("rawtypes")
      final IndexIteratorCachePair iicp = (IndexIteratorCachePair) o;
      return this.fsLeafIndex.getComparator().equals(iicp.fsLeafIndex.getComparator())
          && (this.fsLeafIndex.getIndexingStrategy() == iicp.fsLeafIndex.getIndexingStrategy());
    }

// if this throws, then the Eclipse debugger fails to show the object saying 
// com.sun.jdi.InvocationException occurred invoking method. 
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + this.fsLeafIndex.getComparator().hashCode();
      result = prime * result + this.fsLeafIndex.getIndexingStrategy();
      return result;
    }

    // Populate the cache.
    // For read-only CASes, this may be called on multiple threads, so do some synchronization
        
    @SuppressWarnings("unchecked")
    private void createIndexIteratorCache() {
      // using double-checked sync - see http://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
      if (isIteratorCacheSetup) {
        return;
      }
      synchronized (this) {
        if (isIteratorCacheSetup) {
          return;
        }
        final Type rootType = this.fsLeafIndex.getComparator().getType();
        final int indexKind = this.fsLeafIndex.getIndexingStrategy();
        ArrayList<Type> allTypes = null;
        if (indexKind == FSIndex.DEFAULT_BAG_INDEX) {
          allTypes = new ArrayList<Type>();
          allTypes.add(rootType);
        } else {
          // includes the original type as element 0
          allTypes = getAllSubsumedTypes(rootType, FSIndexRepositoryImpl.this.sii.tsi);
        }
        
        final ArrayList<FSLeafIndexImpl<? extends T>> tempSubIndexCache = new ArrayList<FSLeafIndexImpl<? extends T>>();
        final int len = allTypes.size();
        if (indexKind == FSIndex.SORTED_INDEX) {
          sortedTypeCodes = new int[len];
        }
        
        for (int i = 0; i < len; i++) {
          final int typeCode = ((TypeImpl) allTypes.get(i)).getCode();
          final ArrayList<IndexIteratorCachePair<?>> indexList = FSIndexRepositoryImpl.this.indexArray[typeCode];
          final int indexPos = indexList.indexOf(this);
          FSLeafIndexImpl<? extends T> leafIndex = (FSLeafIndexImpl<? extends T>) indexList.get(indexPos).fsLeafIndex;
          if (indexPos >= 0) {  // is always true???
            tempSubIndexCache.add(leafIndex);            
          } else {
            throw new RuntimeException("never happen");
          }
          if (indexKind == FSIndex.SORTED_INDEX) {
            sortedTypeCodes[i] = leafIndex.getTypeCode();
          }
        }
        this.cachedSubFsLeafIndexes = tempSubIndexCache; 
        if (this.fsLeafIndex.getIndexingStrategy() == FSIndex.SORTED_INDEX) {
          Arrays.sort(sortedTypeCodes);
          this.flatIndex = FSIndexFlat.enabled ? new FSIndexFlat<>(this) : null; // must follow cachedSubFsLeafIndexes setup
        }
        // assign to "volatile" at end, after all initialization is complete
        this.isIteratorCacheSetup = true;
      }  // end of synchronized block
    }

    
    /**
     * Maybe not used 3/2015
     * 
     * Compares first using the type code of the main types
     *   If those are equal,
     *   Compares using the comparator objects
     * @see java.lang.Comparable#compareTo(Object)
     * 
     */
    public int compareTo(IndexIteratorCachePair<? extends FeatureStructure> cp) {
      final int typeCode1 = ((TypeImpl) this.fsLeafIndex.getType()).getCode();
      final int typeCode2 = ((TypeImpl) cp.fsLeafIndex.getType()).getCode();
      if (typeCode1 < typeCode2) {
        return -1;
      } else if (typeCode1 > typeCode2) {
        return 1;
      } else { // types are equal
        return this.fsLeafIndex.getComparator().compareTo(cp.fsLeafIndex.getComparator());
      }
    }

    /**
     * 
     * @return the sum of the sizes of the indexes of the type + all subtypes
     */
    int size() {
      int size = 0;
      createIndexIteratorCache();  // does nothing if already created
      final ArrayList<FSLeafIndexImpl<? extends T>> localIc = this.cachedSubFsLeafIndexes;
      final int len = localIc.size();
      for (int i = 0; i < len; i++) {
        size += localIc.get(i).size();
      }
      return size;
    }
    
    boolean has1OrMoreEntries() {
      createIndexIteratorCache();  // does nothing if already created
      final ArrayList<FSLeafIndexImpl<? extends T>> localIc = this.cachedSubFsLeafIndexes;
      final int len = localIc.size();
      for (int i = 0; i < len; i++) {
        if (localIc.get(i).size() > 0) {
          return true;
        };
      }
      return false;
    }
    
    /**
     * A faster version of size() when there are lots of subtypes
     * The cache must be already set up
     * 
     * Guess by adding the sizes of up to the first 3 type/subtypes, 
     * then add 1 more for each subtype in addition.
     * 
     * @return a guess at the size, done quickly
     */
    int guessedSize() {
      final ArrayList<FSLeafIndexImpl<? extends T>> localIc = this.cachedSubFsLeafIndexes;
      final int len = localIc.size();
      final int lim = Math.min(3, len);
      int size = 0;
      for (int i = 0; i < lim; i++) {
        size += localIc.get(i).size();
      }
      size += len - lim;
      return size;
    }
       
    /**
     * Flat array filled, ordered
     * @param flatArray the array to fill
     */
    public void fillFlatArray(FeatureStructure[] flatArray) {
      LowLevelIterator it = (LowLevelIterator) createPointerIterator(this);
      int i = 0;
      while (it.isValid()) {
        if (i >= flatArray.length) {
          throw new ConcurrentModificationException();
        }
        flatArray[i++] = cas.createFS(it.ll_get());
        if (DEBUG) {
          int tc1 = fsLeafIndex.getTypeCode();
          int tc2 = ((TypeImpl)(flatArray[i-1].getType())).getCode();
          if (!subsumes(tc1, tc2)) {
            throw new RuntimeException(String.format("FillFlatArray for element %,d produced a non-subtype, tc1 = %d, tc2 = %d%n"
                + "iicp = %s%nfs = %s%n",
                tc1,  tc2,
                i-1,
                this,
                flatArray[i-1]));
          }
        }
        it.moveToNext();
      }
      if (i != flatArray.length) {
//        System.out.println("Debug - got iterator invalid before all items filled, i = " + i + ", size = " + flatArray.length);
        throw new ConcurrentModificationException();
      }
    }
    
    Int2IntArrayMapFixedSize createIndexUpdateCountsAtReset() {
      Int2IntArrayMapFixedSize m = new Int2IntArrayMapFixedSize(sortedTypeCodes.length);
      captureIndexUpdateCounts(m);
      return m;
    }
    
    void captureIndexUpdateCounts() {
      captureIndexUpdateCounts(this.flatIndex.indexUpdateCountsResetValues);
    }
    
    private void captureIndexUpdateCounts(Int2IntArrayMapFixedSize m) {
      final int[] localSortedTypeCodes = sortedTypeCodes;
      for (int i = 0; i < localSortedTypeCodes.length; i++) {
        m.putAtIndex(i, detectIllegalIndexUpdates[localSortedTypeCodes[i]]);
      } 
    }
    
    boolean isUpdateFreeSinceLastCounterReset() {
      final Int2IntArrayMapFixedSize typeCode2updateCount = this.flatIndex.indexUpdateCountsResetValues;
      final int[] localSortedTypeCodes = sortedTypeCodes;
      for (int i = 0; i < localSortedTypeCodes.length; i++) {
        if (typeCode2updateCount.getAtIndex(i) != detectIllegalIndexUpdates[localSortedTypeCodes[i]]) {
          return false;
        }
      }
      return true;
    }
        
    boolean isUpdateFreeSinceLastCounterReset(final int typeCode) {
      return this.flatIndex.indexUpdateCountsResetValues.get(typeCode, sortedTypeCodes) == 
          detectIllegalIndexUpdates[typeCode];
    }
      
    boolean subsumes(int superType, int subType) {
      return cas.getTypeSystemImpl().subsumes(superType,  subType);
    }
    
    // debug
    CASImpl getCASImpl() {
      return cas;
    }
    
    void addToIteratedSortedIndexes() {
      iteratedSortedIndexes.add(this);
    }
    
    // flatIndex is null except for sorted indexes
    private boolean hasFlatIndex() {
      if (! FSIndexFlat.enabled) {
        return false;
      } else {
        return isIteratorCacheSetup && (flatIndex != null) && flatIndex.hasFlatIndex();
      }
    }
    
  }  // end of class definition for IndexIteratorCachePair

  
  /* ============================================================================================================*/
  /*           Pointer Iterators                                                                                
  /* ============================================================================================================*/
  /**
   * Create an "ordered" (e.g. one that maintains iicp sort order for sorted index) pointer iterator over an iicp
   * @param iicp - the index plus its subtype list of indexes
   * @return an int iterator
   */
  IntPointerIterator createPointerIterator(IndexIteratorCachePair<? extends FeatureStructure> iicp) {
    return createPointerIterator(iicp, false);
  }


  /**
   * Create an ordered or iicp-unordered pointer iterator over an iicp
   * 
   *  Note that flattened index iterators are not int style; and they're created higher up...
   *  
   * @param iicp - the index plus its subtype list of indexes
   * @param is_unordered true if ordering among subtypes not needed
   * @return an int iterator
   */
  <T extends FeatureStructure> IntPointerIterator createPointerIterator(IndexIteratorCachePair<? extends FeatureStructure> iicp, boolean is_unordered) {
    iicp.createIndexIteratorCache();
    if (iicp.cachedSubFsLeafIndexes.size() > 1) {
      final int strat = iicp.fsLeafIndex.getIndexingStrategy();
      if (strat == FSIndex.BAG_INDEX ||           
          strat == FSIndex.SET_INDEX ||  // because set indexes do not enforce ordering
          is_unordered) {
        return new PointerIteratorUnordered(iicp);
      } else {
        return new PointerIterator(iicp);
      }
    }
    return createLeafPointerIterator(iicp);
  }

  <T extends FeatureStructure> IntPointerIterator createPointerIterator(IndexIteratorCachePair<? extends FeatureStructure> iicp, int fs) {
    return createPointerIterator(iicp, false, fs);
  }
  
  <T extends FeatureStructure> IntPointerIterator createPointerIterator(IndexIteratorCachePair<? extends FeatureStructure> iicp, boolean is_unordered, int fs) {
    IntPointerIterator it = createPointerIterator(iicp, is_unordered);
    it.moveTo(fs);
    return it;
  }
  
  private <T extends FeatureStructure> IntPointerIterator createLeafPointerIterator(IndexIteratorCachePair<? extends FeatureStructure> iicp) {
    FSLeafIndexImpl<T> leafIndex = (FSLeafIndexImpl<T>) iicp.fsLeafIndex;
    return leafIndex.pointerIterator(leafIndex, this.detectIllegalIndexUpdates, leafIndex.getTypeCode());
  }

  /**
   * The next 3 classes (PointerIterator, PointerIteratorUnordered and LeafPointerIterator) 
   * implement iterators for particular indexes.
   * 
   * PointerIteratorUnordered is used for bag and things like all indexed fs where order is not important.
   * It uses the same impl as PointerIterator, except it works by sequentially iterating over each of the 
   * iterator pieces.
   * 
   * This class handles the concepts involved with iterating over a type and
   * all of its subtypes, keeping the ordering among the subtypes. 
   * 
   * The LeafPointerIterator handles just iterating over a particular type or subtype
   * (the one that this class picks).
   * 
   * The iterator implementation for indexes. Tricky because the iterator needs to be able to move
   * backwards as well as forwards.
   */
  private class PointerIterator implements IntPointerIterator, LowLevelIterator {

    /**
     * The number of elements to keep in order before the binary heap starts. This section helps the
     * performance in cases where a couple of types dominate the index.
     */
    static final int SORTED_SECTION = 3;

    // The IICP
    final private IndexIteratorCachePair<? extends FeatureStructure> iicp;

    protected IndexIteratorCachePair<? extends FeatureStructure> getIicp() {
      return iicp;
    }

    // An array of ComparableIntPointerIterators, one for each subtype.
    //   Each instance of these has a Class.this kind of ref to a particular variety of FSLeafIndex (bag, set, sorted) corresponding to 1 type
    //   This array has the indexes for all the subtypes that were non-empty at the time of iterator creation
    protected ComparableIntPointerIterator[] iterators;

    int lastValidIndex;

    // snapshot to detectIllegalIndexUpdates
    // need to move this to ComparableIntPointerIterator so it can be tested

    // currentIndex is always 0

    // The iterator works in two modes:
    // Forward and backward processing. This flag tells which mode we're in.
    // The iterator heap needs to be reconstructed when we switch direction.
    protected boolean wentForward;

    // Comparator that is used to compare FS addresses for the purposes of
    // iteration.
    final private IntComparator iteratorComparator;

    // skip including iterators for empty indexes
    //   The concurrent modification exception notification doesn't occur when subsequent "adds" are done, but
    //   that is the same as current: 
    //   where the move to first would mark the iterator "invalid" (because it was initially empty) and it would be
    //     subsequently ignored - same effect
    private ComparableIntPointerIterator[] initPointerIterator() {
      // Note to maintainers: Make sure the iterator cache exists on all paths calling this
      final ArrayList<?> cachedSubIndexes = iicp.cachedSubFsLeafIndexes;
      final int length = cachedSubIndexes.size();
      
      final ArrayList<ComparableIntPointerIterator> pia = new ArrayList<ComparableIntPointerIterator>(cachedSubIndexes.size());

      // put all non-empty leaf iterators into the iteration, and if all are empty, put the last one in
      //   (to avoid handling 0 as a special case)
      for (int i = 0; i < length; i++) {
        final FSLeafIndexImpl<?> leafIndex = (FSLeafIndexImpl<?>) cachedSubIndexes.get(i);
        if ((leafIndex.size() > 0) || 
            ((i == length -1) &&        // this logic puts in the last one if all are empty
             (0 == pia.size()))) {
          pia.add(leafIndex.pointerIterator(
              this.iteratorComparator,
              FSIndexRepositoryImpl.this.detectIllegalIndexUpdates,
              ((TypeImpl) leafIndex.getType()).getCode()));
        }
      }
      
      ComparableIntPointerIterator[] piaa = new ComparableIntPointerIterator[pia.size()];
      return pia.toArray(piaa);
    }

    private PointerIterator(final IndexIteratorCachePair<? extends FeatureStructure> iicp) {
      // next 3 are final so aren't done in the common init
      this.iicp = iicp;
      this.iteratorComparator = iicp.cachedSubFsLeafIndexes.get(0);
      this.iterators = initPointerIterator();
      moveToFirst();
    }

    private PointerIterator(final IndexIteratorCachePair<? extends FeatureStructure> iicp, int fs) {
      // next 3 are final so aren't done in the common init
      this.iicp = iicp;
      this.iteratorComparator = iicp.cachedSubFsLeafIndexes.get(0);
      this.iterators = initPointerIterator();
      moveTo(fs);
    }

    public boolean isValid() {
      // We're valid as long as at least one index is.
      return (this.lastValidIndex >= 0 );
    }

    protected ComparableIntPointerIterator<?> checkConcurrentModification(int i) {
      final FSIntIteratorImplBase<?> cipi = (FSIntIteratorImplBase<?>) this.iterators[i];
      cipi.checkConcurrentModification();  // throws if concurrentModification
      return cipi;
    }

    /**
     * Test the order with which the two iterators should be used. Introduces arbitrary ordering for
     * equivalent FSs. Only called with valid iterators.
     * 
     * @param l
     * @param r
     * @param dir
     *          Direction of movement, 1 for forward, -1 for backward
     * @return true if the left iterator needs to be used before the right one.
     */
    private boolean is_before(ComparableIntPointerIterator l, ComparableIntPointerIterator r,
        int dir) {
      final int il = l.get();
      final int ir = r.get();
      int d = this.iteratorComparator.compare(il, ir);

      // If two FSs are identical wrt the comparator of the index,
      // we still need to be able to distinguish them to be able to have a
      // well-defined sequence. In that case, we arbitrarily order FSs by
      // their
      // addresses. We need to do this in order to be able to ensure that a
      // reverse iterator produces the reverse order of the forward iterator.
      if (d == 0) {
        d = il - ir;
      }

      return d * dir < 0;
    }

    /**
     * Move the idx'th element up in the heap until it finds its proper position.
     * 
     * @param it
     *          indexes[idx]
     * @param idx
     *          Element to move
     * @param dir
     *          Direction of iterator movement, 1 for forward, -1 for backward
     */
    private void heapify_up(ComparableIntPointerIterator it, int idx, int dir) {
      FSIndexFlat<? extends FeatureStructure> flatIndexInfo = iicp.flatIndex;
      if (null != flatIndexInfo) {
        flatIndexInfo.incrementReorderingCount();
      }
      int nidx;

      while (idx > SORTED_SECTION) {
        nidx = (idx + SORTED_SECTION - 1) >> 1;
        if (!is_before(it, this.iterators[nidx], dir)) {
          this.iterators[idx] = it;
          return;
        }
        this.iterators[idx] = this.iterators[nidx];
        idx = nidx;
      }

      while (idx > 0) {
        nidx = idx - 1;
        if (!is_before(it, this.iterators[nidx], dir)) {
          this.iterators[idx] = it;
          return;
        }
        this.iterators[idx] = this.iterators[nidx];
        idx = nidx;
      }

      this.iterators[idx] = it;
    }

    /**
     * Move the top element down in the heap until it finds its proper position.
     * 
     * @param it the current lowest index, that was just incremented or decremented
     *          indexes[0]
     * @param dir
     *          Direction of iterator movement, 1 for forward, -1 for backward
     */
    private void heapify_down(ComparableIntPointerIterator it, int dir) {
      FSIndexFlat<? extends FeatureStructure> flatIndexInfo = iicp.flatIndex;
      if (null != flatIndexInfo) {
        flatIndexInfo.incrementReorderingCount();
      }

      // if the iterator passed in just became invalid
      //   move it to the last valid iterator spot (and reduce lastValidIndex by 1 afterwards)
      if (!it.isValid()) {
        final ComparableIntPointerIterator itl = checkConcurrentModification(this.lastValidIndex);
        this.iterators[this.lastValidIndex] = it;
        this.iterators[0] = itl;
        --this.lastValidIndex;
        it = itl;
      }

      // return if 0th iterator is lowest (or equal) one
      final int num = this.lastValidIndex;
      if ((num < 1) ||  // no iterator is valid OR
                        // 0th is before or == to 1 in position
          !is_before(checkConcurrentModification(1), it, dir)) {
        return;
      }

      // 0th is after iterator[1]
      int idx = 1;
      this.iterators[0] = this.iterators[1];
      final int end = Math.min(num, SORTED_SECTION);
      int nidx = idx + 1;

      // make sure we don't leave the iterator in a completely invalid state
      // (i.e. one it can't recover from using moveTo/moveToFirst/moveToLast)
      // in case of a concurrent modification
      try {
        while (nidx <= end) {
          if (!is_before(checkConcurrentModification(nidx), it, dir)) {
            return; // passes through finally
          }

          this.iterators[idx] = this.iterators[nidx];
          idx = nidx;
          nidx = idx + 1;
        }

        nidx = SORTED_SECTION + 1;
        while (nidx <= num) {
          if ((nidx < num)
              && is_before(checkConcurrentModification(nidx + 1),
                  checkConcurrentModification(nidx), dir)) {
            ++nidx;
          }

          if (!is_before(this.iterators[nidx], it, dir)) {
            return;
          }

          this.iterators[idx] = this.iterators[nidx];
          idx = nidx;
          nidx = (nidx << 1) - (SORTED_SECTION - 1);
        }
      } finally {
        this.iterators[idx] = it;
      }
    }

    public void moveToFirst() {
      int lvi = this.iterators.length - 1;
      // Need to consider all iterators.
      // Set all iterators to insertion point.
      int i = 0;
      while (i <= lvi) {
        final FSIntIteratorImplBase<?> it = (FSIntIteratorImplBase<?>) this.iterators[i];
        it.resetConcurrentModification();
        it.moveToFirst();
        if (it.isValid()) {
          heapify_up(it, i, 1);
          ++i;
        } else {
          // swap this iterator with the last possibly valid one
          // lvi might be equal to i, this will not be a problem
          this.iterators[i] = this.iterators[lvi];
          this.iterators[lvi] = it;
          --lvi;
        }
      }
      // configured to continue with forward iterations
      this.wentForward = true;
      this.lastValidIndex = lvi;
    }

    public void moveToLast() {
      int lvi = this.iterators.length - 1;
      // Need to consider all iterators.
      // Set all iterators to insertion point.
      int i = 0;
      while (i <= lvi) {
        final FSIntIteratorImplBase<?> it = (FSIntIteratorImplBase<?>) this.iterators[i];
        it.resetConcurrentModification();
        it.moveToLast();
        if (it.isValid()) {
          heapify_up(it, i, -1);
          ++i;
        } else {
          // swap this iterator with the last possibly valid one
          // lvi might be equal to i, this will not be a problem
          this.iterators[i] = this.iterators[lvi];
          this.iterators[lvi] = it;
          --lvi;
        }
      }
      // configured to continue with backward iterations
      this.wentForward = false;
      this.lastValidIndex = lvi;
    }

    public void moveToNext() {
      if (!isValid()) {
        return;
      }

      final ComparableIntPointerIterator it0 = checkConcurrentModification(0);

      if (this.wentForward) {
        it0.inc();
        heapify_down(it0, 1);
      } else {
        // We need to increment everything.
        int lvi = this.iterators.length - 1;
        int i = 1;
        while (i <= lvi) {
          // Any iterator other than the current one needs to be
          // incremented until it's pointing at something that's
          // greater than the current element.
          final ComparableIntPointerIterator it = checkConcurrentModification(i);
          // If the iterator we're considering is not valid, we
          // set it to the first element. This should be it for this iterator...
          if (!it.isValid()) {
            it.moveToFirst();
          }
          // Increment the iterator while it is valid and pointing
          // at something smaller than the current element.
          while (it.isValid() && is_before(it, it0, 1)) {
            it.inc();
          }

          // find placement
          if (it.isValid()) {
            heapify_up(it, i, 1);
            ++i;
          } else {
            // swap this iterator with the last possibly valid one
            // lvi might be equal to i, this will not be a problem
            this.iterators[i] = this.iterators[lvi];
            this.iterators[lvi] = it;
            --lvi;
          }
        }

        this.lastValidIndex = lvi;
        this.wentForward = true;

        it0.inc();
        heapify_down(it0, 1);
      }
    }

    public void moveToPrevious() {
      if (!isValid()) {
        return;
      }

      final ComparableIntPointerIterator it0 = checkConcurrentModification(0);
      if (!this.wentForward) {
        it0.dec();
        // this also takes care of invalid iterators
        heapify_down(it0, -1);
      } else {
        // We need to decrement everything.
        int lvi = this.iterators.length - 1;
        int i = 1;
        while (i <= lvi) {
          // Any iterator other than the current one needs to be
          // decremented until it's pointing at something that's
          // smaller than the current element.
          final ComparableIntPointerIterator it = checkConcurrentModification(i);
          // If the iterator we're considering is not valid, we
          // set it to the last element. This should be it for this iterator...
          if (!it.isValid()) {
            it.moveToLast();
          }
          // Decrement the iterator while it is valid and pointing
          // at something greater than the current element.
          while (it.isValid() && is_before(it, it0, -1)) {
            it.dec();
          }

          // find placement
          if (it.isValid()) {
            heapify_up(it, i, -1);
            ++i;
          } else {
            // swap this iterator with the last possibly valid one
            // lvi might be equal to i, this will not be a problem
            this.iterators[i] = this.iterators[lvi];
            this.iterators[lvi] = it;
            --lvi;
          }
        }

        this.lastValidIndex = lvi;
        this.wentForward = false;

        it0.dec();
        heapify_down(it0, -1);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.cas.impl.LowLevelIterator#ll_get()
     */
    public int get() throws NoSuchElementException {
      return ll_get();
    }

    public int ll_get() {
      if (!isValid()) {
        throw new NoSuchElementException();
      }
      return checkConcurrentModification(0).get();
    }

    public Object copy() {
      // If this.isValid(), return a copy pointing to the same element.
      if (this.isValid()) {
        PointerIterator it = new PointerIterator(this.iicp);
        moveTo(this.get());
        return it;
      }
      // Else, create a copy that is also not valid.
      final PointerIterator pi = new PointerIterator(this.iicp);
      pi.moveToFirst();
      pi.moveToPrevious();
      return pi;
    }
  
    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#moveTo(int)
     */
    public void moveTo(int fs) {
      moveTo(fs, false);
    }
    /**
     * @param fs the FS to move to
     * @param isExact if true, move to this exact one (must be present),
     *                if false, move to the left-most element that is equal to fs
     *                using the comparator for the index or if none is equal,
     *                move to the next element that is greater than this fs
     *                or invalid position of all are less than this fs
     */
   void moveTo(int fs, boolean isExact) {
      int lvi = this.iterators.length - 1;
      // Need to consider all iterators.
      // Set all iterators to insertion point.
      int i = 0;
      while (i <= lvi) {
        final FSIntIteratorImplBase<?> it = (FSIntIteratorImplBase<?>) this.iterators[i];
        it.moveTo(fs, isExact);
        if (it.isValid()) {
          heapify_up(it, i, 1);
          ++i;
        } else {
          // swap this iterator with the last possibly valid one
          // lvi might be equal to i, this will not be a problem
          this.iterators[i] = this.iterators[lvi];
          this.iterators[lvi] = it;
          --lvi;
        }
      }
      // configured to continue with forward iterations
      this.wentForward = true;
      this.lastValidIndex = lvi;
     
      // moved to leaf iterator
//      if (!isValid()) {
//        // this means the moveTo found the insert point at the end of the index
//        // so just return invalid, since there's no way to return an insert point for a position
//        // that satisfies the FS at that position is greater than fs  
//        return;
//      }
//      // Go back until we find a FS that is really smaller
//      while (true) {
//        moveToPrevious();
//        if (isValid()) {
//          int prev = get();
//          if (this.iicp.index.compare(prev, fs) != 0) {
//            moveToNext();
//            break;
//          }
//        } else {
//          moveToFirst();
//          break;
//        }
//      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.cas.impl.LowLevelIterator#moveToNext()
     */
    public void inc() {
      moveToNext();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.cas.impl.LowLevelIterator#moveToPrevious()
     */
    public void dec() {
      moveToPrevious();
    }

    public int ll_indexSize() {
      return this.iicp.size();
    }

    public LowLevelIndex ll_getIndex() {
      return (LowLevelIndex) this.iicp.fsLeafIndex;
    }
    
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder(this.getClass().getSimpleName() + " [iicp=" + iicp + ", indexes=\n");
      int i = 0;
      for (ComparableIntPointerIterator item : iterators) {
        sb.append("  ").append(i++).append("  ").append(item).append('\n');
        if (i > 4) {
          break;
        }
      }
      if (i < iterators.length) {
        sb.append("  and ").append(iterators.length - i).append(" more.\n");
      }
      sb.append("  lastValidIndex="
          + lastValidIndex + ", wentForward=" + wentForward + ", iteratorComparator=" + iteratorComparator + "]");
      return sb.toString();
    }

  }  // end of class PointerIterator
  
  /**
   * Version of pointer iterator for unordered uses (bags and getAllIndexedFSs
   * Since bags have no order, simplify the iteration by just going thru sequentially
   * all the subtypes
   *
   */
  private class PointerIteratorUnordered extends PointerIterator {
    
    private PointerIteratorUnordered(final IndexIteratorCachePair<? extends FeatureStructure> iicp) {
      super(iicp);
    }
    
    private PointerIteratorUnordered(final IndexIteratorCachePair<? extends FeatureStructure> iicp, int fs) {
      super(iicp); 
      moveTo(fs);
    }

    /* (non-Javadoc)
     * @see org.apache.uima.cas.impl.FSIndexRepositoryImpl.PointerIterator#isValid()
     */
    @Override
    public boolean isValid() {
      return (lastValidIndex >= 0) && (lastValidIndex < iterators.length);
    }
    
    

    /* (non-Javadoc)
     * @see org.apache.uima.cas.impl.FSIndexRepositoryImpl.PointerIterator#ll_get()
     */
    @Override
    public int ll_get() {
      if (!isValid()) {
        throw new NoSuchElementException();
      }
      return checkConcurrentModification(lastValidIndex).get();
    }

    /* (non-Javadoc)
     * @see org.apache.uima.cas.impl.FSIndexRepositoryImpl.PointerIterator#moveToFirst()
     */
    @Override
    public void moveToFirst() {
      for (int i = 0; i < iterators.length; i++) {
        FSIntIteratorImplBase<?> it = (FSIntIteratorImplBase<?>) iterators[i];
        it.moveToFirst();
        it.resetConcurrentModification();
        if (it.isValid()) {
          lastValidIndex = i;
          return;
        }
      }
      lastValidIndex = -1; // no valid index
    }

    /* (non-Javadoc)
     * @see org.apache.uima.cas.impl.FSIndexRepositoryImpl.PointerIterator#moveToLast()
     */
    @Override
    public void moveToLast() {
      for (int i = iterators.length -1; i >= 0; i--) {
        FSIntIteratorImplBase<?> it = (FSIntIteratorImplBase<?>) iterators[i];
        it.moveToLast();
        it.resetConcurrentModification();
        if (it.isValid()) {
          lastValidIndex = i;
          return;
        }
      }
      lastValidIndex = -1;
    }

    /* (non-Javadoc)
     * @see org.apache.uima.cas.impl.FSIndexRepositoryImpl.PointerIterator#moveToNext()
     */
    @Override
    public void moveToNext() {
      if (!isValid()) {
        return;
      }
      
      ComparableIntPointerIterator it = checkConcurrentModification(lastValidIndex);

      it.inc();
      
      while (!it.isValid()) {
        // loop until find valid index, or end
        lastValidIndex ++;
        if (lastValidIndex == iterators.length) {
          return; // all subsequent indices are invalid
        }
        it = iterators[lastValidIndex];
        it.moveToFirst();
      }
    }

    /* (non-Javadoc)
     * @see org.apache.uima.cas.impl.FSIndexRepositoryImpl.PointerIterator#moveToPrevious()
     */
    @Override
    public void moveToPrevious() {
      if (!isValid()) {
        return;
      }
      
      ComparableIntPointerIterator it = checkConcurrentModification(lastValidIndex);

      it.dec();
      
      while (!it.isValid()) {
        // loop until find valid index or end
        lastValidIndex --;
        if (lastValidIndex < 0) {
          return; 
        }
        it = iterators[lastValidIndex];
        it.moveToLast();
      }
    }

    /* (non-Javadoc)
     * @see org.apache.uima.cas.impl.FSIndexRepositoryImpl.PointerIterator#copy()
     */
    @Override
    public Object copy() {
      // If this.isValid(), return a copy pointing to the same element.
      if (this.isValid()) {
        return new PointerIteratorUnordered(getIicp(), get());
      }
      // Else, create a copy that is also not valid.
      final PointerIteratorUnordered pi = new PointerIteratorUnordered(getIicp());
      pi.moveToFirst();
      pi.moveToPrevious();
      return pi;
    }

    /* (non-Javadoc)
     * @see org.apache.uima.cas.impl.FSIndexRepositoryImpl.PointerIterator#moveTo(int)
     * 
     * moveTo is used internally in the contains and find methods of FSIndexes
     *   The unordered version is only called on for bags and sets having subtypes
     *   There, the logic needs to iterate over all the iterators, looking for an equal (for set) or eq one (for bags)
     *   If not found, its more useful to make the iterator not-valid
     *   
     * Should not be called if sorted, but do something reasonable:
     *   stop on first one found equal.  This will be the left-most
     *   equal one in cas there are multiples of this particular type
     *   if none found equal on one subType, go to next subType
     *   if none found equal on any subType, mark iterator invalid 
     *     (NOTE: not really the contract for moveTo, but 
     *      as stated in the beginning, probably not called for unordered)
     */
    @Override
    public void moveTo(int fs) {
      moveTo(fs, false);
    }
    
    void moveTo(int fs, boolean isExact) {
      IndexIteratorCachePair<? extends FeatureStructure> iicp = getIicp();
      int kind = iicp.fsLeafIndex.getIndexingStrategy();
      for (int i = 0; i < iterators.length; i++) {
        if (kind == FSIndex.SORTED_INDEX) {
          // case: sorted index being used in unordered mode, eg. for getAllIndexedFSs
          FSIntArrayIndex<? extends FeatureStructure> sortedIndex = 
              (FSIntArrayIndex<? extends FeatureStructure>) ((FSIntIteratorImplBase) iterators[i]).getFSLeafIndexImpl(); 
          if ((isExact ? sortedIndex.findEq(fs) :sortedIndex.findLeftmost(fs)) < 0) {
            continue;  // fs not found in the index of this subtype  
          }
        }
        // if sorted index, fs is in this leaf index
        // if it is not sorted, we're in some type/subtype of the index
        FSIntIteratorImplBase<?> li = (FSIntIteratorImplBase<?>) iterators[i];
        li.moveTo(fs); 
        if (li.isValid() && (0 == iicp.fsLeafIndex.compare(fs, li.get()))) {
          lastValidIndex = i;
          li.resetConcurrentModification();
          return;
        }
        // if get here, iterate to the next subtype
      }
      // if get here, nothing found that was equal or eq for bag
      // make iterator invalid
      moveToFirst();
      moveToPrevious();
    }
  }

  /**
   * This class and the previous ones (PointerIterator, PointerIteratorUnordered, and LeafPointerIterator) 
   * implement iterators for particular indexes.
   * 
   * PointerIterator and PointerIteratorUnordered handles the concepts involved with iterating over a type and
   * all of its subtypes
   * 
   * This class handles just iterating over a particular type or subtype
   * (the one that the previous class picks).

   * The iterator implementation for indexes. Tricky because the iterator needs to be able to move
   * backwards as well as forwards.
   */
  
  // removed 4/2015 - was an extra wrapper, needed maintenance, and the wrapper provided no purpose.
//  private class LeafPointerIterator implements IntPointerIterator, LowLevelIterator {
//
//    // The IICP
//    final private IndexIteratorCachePair<? extends FeatureStructure> iicp;
//
//    // The underlying iterator
//    final private ComparableIntPointerIterator iter;
//
//    
//    @Override
//    public String toString() {
//      return "LeafPointerIterator [iicp=" + iicp + ", index=" + iter + "]";
//    }
//
//    private LeafPointerIterator(final IndexIteratorCachePair<? extends FeatureStructure> iicp) {
//      this.iicp = iicp;
//      // Make sure the iterator cache exists.
//      final ArrayList<?> iteratorCache = iicp.cachedSubFsLeafIndexes;
//      final FSLeafIndexImpl<?> leafIndex = (FSLeafIndexImpl<?>) iteratorCache.get(0);
//      this.iter = leafIndex.pointerIterator(leafIndex,
//          FSIndexRepositoryImpl.this.detectIllegalIndexUpdates,
//          ((TypeImpl) leafIndex.getType()).getCode());
//    }
//
//    private LeafPointerIterator(IndexIteratorCachePair<? extends FeatureStructure> iicp, int fs) {
//      this(iicp);
//      moveTo(fs);
//    }
//
//    private ComparableIntPointerIterator checkConcurrentModification() {
//      if (this.iter.isConcurrentModification()) {
//        throw new ConcurrentModificationException();
//      }
//      return this.iter;
//    }
//
//    public boolean isValid() {
//      return this.iter.isValid();
//    }
//
//    public void moveToLast() {
//      this.iter.resetConcurrentModification();
//      this.iter.moveToLast();
//    }
//
//    public void moveToFirst() {
//      this.iter.resetConcurrentModification();
//      this.iter.moveToFirst();
//    }
//
//    public void moveToNext() {
//      checkConcurrentModification().inc();
//    }
//
//    public void moveToPrevious() {
//      checkConcurrentModification().dec();
//    }
//
//    public int get() throws NoSuchElementException {
//      return ll_get();
//    }
//
//    public int ll_get() {
//      if (!isValid()) {
//        throw new NoSuchElementException();
//      }
//      return checkConcurrentModification().get();
//    }
//
//    public Object copy() {
//      // If this.isValid(), return a copy pointing to the same element.
//      if (this.isValid()) {
//        return new LeafPointerIterator(this.iicp, this.get());
//      }
//      // Else, create a copy that is also not valid.
//      final LeafPointerIterator pi = new LeafPointerIterator(this.iicp);
//      pi.moveToFirst();
//      pi.moveToPrevious();
//      return pi;
//    }
//
//    /**
//     * @see org.apache.uima.internal.util.IntPointerIterator#moveTo(int)
//     */
//    public void moveTo(int fs) {
//      this.iter.resetConcurrentModification();
//      this.iter.moveTo(fs);
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.apache.uima.cas.impl.LowLevelIterator#moveToNext()
//     */
//    public void inc() {
//      checkConcurrentModification().inc();
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.apache.uima.cas.impl.LowLevelIterator#moveToPrevious()
//     */
//    public void dec() {
//      checkConcurrentModification().dec();
//    }
//
//    public int ll_indexSize() {
//      return this.iicp.size();
//    }
//
//    public LowLevelIndex ll_getIndex() {
//      return (LowLevelIndex) this.iicp.fsLeafIndex;
//    }
//
//  }  // end of LeafPointerIterator

  /**
   * This implementation creates a pseudo index that is
   * flattened and 
   * copied (so it is a snapshot), and
   * returns an iterator over that 
   * 
   */
  private class SnapshotPointerIterator<T extends FeatureStructure> implements IntPointerIterator, LowLevelIterator {

    final private FSIntArrayIndex<T> sortedLeafIndex;
    final private int[] snapshot;
    final private int size;   // can't get from snapshot.length - that might have extra space  https://issues.apache.org/jira/browse/UIMA-4248
    private int pos = 0;
      
    @Override
    public String toString() {
      return "SnapshotPointerIterator[size: " + snapshot.length + ", position: " + pos + "]";
    }
    
    private SnapshotPointerIterator(IndexIteratorCachePair<T> iicp0) {
      this(iicp0, false);
    }
    
    private SnapshotPointerIterator(IndexIteratorCachePair<T> iicp0, boolean isRootOnly) {
      FSLeafIndexImpl<T> leafIndex = iicp0.fsLeafIndex;
      FSIndexComparator comp = leafIndex.getComparator();
      
      final int size = iicp0.size();  // adds up all the sizes of the indexes
      sortedLeafIndex = (FSIntArrayIndex<T>) FSIndexRepositoryImpl.this.<T>addNewIndexCore(comp, size, FSIndex.SORTED_INDEX);
      snapshot = sortedLeafIndex.getVector().getArray();
      this.size = size;
      flattenCopy(iicp0, isRootOnly);
      sortedLeafIndex.getVector().setSize(size);
      moveToFirst();      
    }

    private SnapshotPointerIterator(IndexIteratorCachePair<T> iicp0, int fs) {
      this(iicp0);
      moveTo(fs);
    }
    
    private void flattenCopy(IndexIteratorCachePair<T> iicp0, boolean isRootOnly) {
    
      
//      if (iicp0.iteratorCache.size() > 1) {
//        if (indexKind == FSIndex.BAG_INDEX) {
//          // have a set of bag indexes, just copy them into the snapshot in bulk
//        } else { 
//          // for sorted or set, extract the elements 
//          
//        }
      LowLevelIterator it = (LowLevelIterator) 
          (isRootOnly ?
              createLeafPointerIterator(iicp0) :
              createPointerIterator(iicp0));
      int i = 0;
      while (it.isValid()) {
        snapshot[i++] = it.ll_get();
        it.moveToNext();
      }
    }
       
    public boolean isValid() {
      return (0 <= pos) && (pos < size);
    }
    
    public void moveToLast() {
      pos = size - 1;
    }

    public void moveToFirst() {
      pos = 0;
    }

    public void moveToNext() {
      pos ++;
    }

    public void moveToPrevious() {
      pos --;
    }

    public int get() throws NoSuchElementException {
      return ll_get();
    }

    public int ll_get() {
      if (!isValid()) {
        throw new NoSuchElementException();
      }
      return snapshot[pos];  // no concurrent mod test
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#moveTo(int)
     */
    public void moveTo(int fs) {
      moveTo(fs, false);
    }
    
    void moveTo(int fs, boolean isExact) {
      if (sortedLeafIndex.getComparator().getNumberOfKeys() == 0) {
        // use identity, search from beginning to get "left-most"
        int i = 0;
        for (; i < size; i++) {
          if (fs == snapshot[i]) {
            break;
          }
        }
        pos = i;
      } else {
        int position = isExact ? sortedLeafIndex.findEq(fs) : sortedLeafIndex.findLeftmost(fs);
        if (position >= 0) {
          pos = position;
        } else {
          if (isExact) {
            throw new UIMARuntimeException(); // Internal error
          }
          pos = -(position + 1);
        }
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.cas.impl.LowLevelIterator#moveToNext()
     */
    public void inc() {
      pos ++;  // no concurrent mod check
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.cas.impl.LowLevelIterator#moveToPrevious()
     */
    public void dec() {
      pos --;  // no concurrent mod check
    }
    
    public int ll_indexSize() {
      return size;
    }
    
    public Object copy() {
      // If this.isValid(), return a copy pointing to the same element.
      IndexIteratorCachePair<T> iicp = new IndexIteratorCachePair<T>(sortedLeafIndex);
      if (this.isValid()) {
        SnapshotPointerIterator<T> it = new SnapshotPointerIterator<T>(iicp);
        it.moveTo(this.get(), true); // move to exact match
        return it;
      }
      // Else, create a copy that is also not valid.
      SnapshotPointerIterator<T> pi = new SnapshotPointerIterator<T>(iicp);
      pi.pos = -1;
      return pi;
    }

    public LowLevelIndex ll_getIndex() {
      return (LowLevelIndex) sortedLeafIndex;
    }

  }  // end of SnapshotPointerIterator

  /**
   * Implementation of a particular index for a particular Type (and its subtypes)
   *
   * @param <T> - the particular type (and it subtypes) this particular index is associated with
   */
  // needs default visibility
  class IndexImpl<T extends FeatureStructure> implements FSIndex<T>, FSIndexImpl {

    private final IndexIteratorCachePair<T> iicp;
    
    private final boolean is_with_snapshot_iterators;

    private final boolean is_unordered; //Set for getAllIndexedFSs

    private IndexImpl(IndexIteratorCachePair<T> iicp) {
      this.iicp = iicp;
      is_with_snapshot_iterators = false;
      is_unordered = false;
    }
    
    private IndexImpl(IndexIteratorCachePair<T> iicp, IteratorExtraFunction extraFn) {
      this.iicp = iicp;
      is_with_snapshot_iterators = (extraFn == IteratorExtraFunction.SNAPSHOT);
      is_unordered = (extraFn == IteratorExtraFunction.UNORDERED);
    }    

    public int ll_compare(int ref1, int ref2) {
      return this.iicp.fsLeafIndex.ll_compare(ref1, ref2);
    }

    /**
     * @see org.apache.uima.cas.FSIndex#getIndexingStrategy()
     */
    @Override
    public int getIndexingStrategy() {
      return this.iicp.fsLeafIndex.getIndexingStrategy();
    }

    /**
     * @see org.apache.uima.cas.FSIndexImpl#getComparator()
     */
    @Override
    public FSIndexComparator getComparator() {
      return this.iicp.fsLeafIndex.getComparator();
    }
    
    // never used 3/2015
//    protected IntComparator getIntComparator() {
//      return this.iicp.fsLeafIndex.getIntComparator();
//    }

    // probably never called 3/15/2015
    public void flush() {
      this.iicp.fsLeafIndex.flush();
    }

    /**
     * @see org.apache.uima.cas.FSIndex#compare(FeatureStructure, FeatureStructure)
     */
    @Override
    public int compare(FeatureStructure fs1, FeatureStructure fs2) {
      return this.iicp.fsLeafIndex.compare(fs1, fs2);
    }
        
    /**
     * @see org.apache.uima.cas.FSIndex#contains(FeatureStructure)
     */
    @Override
    public boolean contains(FeatureStructure fs) {
      FeatureStructureImpl fsi = (FeatureStructureImpl) fs;
      IntPointerIterator it = createPointerIterator(this.iicp); 
      it.moveTo(fsi.getAddress());
      return it.isValid() && (0 == this.iicp.fsLeafIndex.ll_compare(fsi.getAddress(), it.get()));
    }

    /**
     * @see org.apache.uima.cas.FSIndex#find(FeatureStructure)
     */
    @Override
    public FeatureStructure find(FeatureStructure fs) {
      FeatureStructureImpl fsi = (FeatureStructureImpl) fs;
      IntPointerIterator it = createPointerIterator(this.iicp); 
      it.moveTo(fsi.getAddress());
      if (it.isValid()) {
        int v = it.get();
        return (0 == this.iicp.fsLeafIndex.ll_compare(fsi.getAddress(), v)) ? 
            this.iicp.getCASImpl().createFS(v) :
            null;
      }
      return null;
    }

    /**
     * @see org.apache.uima.cas.FSIndex#getType()
     */
    @Override
    public Type getType() {
      return this.iicp.fsLeafIndex.getType();
    }

    /**
     * @see org.apache.uima.cas.FSIndex#iterator()
     */
    @Override
    public FSIterator<T> iterator() {
      return iterator(null);
    }

    /**
     * @see org.apache.uima.cas.FSIndex#iterator(FeatureStructure)
     */
   @Override
   public FSIterator<T> iterator(FeatureStructure fs) {
      if (this.iicp.flatIndex != null) {
        FSIteratorFlat<T> flatIterator = this.iicp.flatIndex.iterator(fs);
        if (flatIterator != null) {
          if (DEBUG) {
            // this stuff - the flat iterator will have been created by other means, and could be ordered, 
            // so don't force unordering
            return new FSIteratorWrapperDoubleCheck<T>(nonFlatIterator(fs, false), flatIterator);
          }
          return flatIterator;
        }
      }
      return nonFlatIterator(fs, true);
    }
 
    private FSIterator<T> nonFlatIterator(FeatureStructure fs, boolean respectUnordered) {
      if (null != fs) {
        final int fsAddr = ((FeatureStructureImpl) fs).getAddress();
        return new FSIteratorWrapper<T>(
            is_with_snapshot_iterators ?
                new SnapshotPointerIterator<T>(iicp, fsAddr) :
                createPointerIterator(this.iicp, fsAddr),
            FSIndexRepositoryImpl.this.cas);
      } else {
        return new FSIteratorWrapper<T>(
            is_with_snapshot_iterators ?
                new SnapshotPointerIterator<T>(iicp) :
                createPointerIterator(this.iicp, respectUnordered && is_unordered),
            FSIndexRepositoryImpl.this.cas);
      }
    }
        
    // probably never called 3/15/2015
    public IntPointerIterator getIntIterator() {
      return createPointerIterator(this.iicp, is_unordered);
    }

    /**
     * @see org.apache.uima.cas.FSIndex#size()
     */
    @Override
    public int size() {
      return this.iicp.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.cas.impl.LowLevelIndex#ll_iterator()
     */
    public LowLevelIterator ll_iterator() {
      return (LowLevelIterator) 
          (is_with_snapshot_iterators ?
              new SnapshotPointerIterator<T>(iicp) :
              createPointerIterator(this.iicp, is_unordered));
    }

    public LowLevelIterator ll_rootIterator() {
      this.iicp.createIndexIteratorCache();
      return (LowLevelIterator) (is_with_snapshot_iterators ?
          new SnapshotPointerIterator<T>(iicp, true) : 
          createLeafPointerIterator(this.iicp));
    }

    public LowLevelIterator ll_iterator(boolean ambiguous) {
      if (ambiguous) {
        return this.ll_iterator();
      }
      return new LLUnambiguousIteratorImpl(this.ll_iterator(), this.iicp.fsLeafIndex.lowLevelCAS);
    }

    /**
     * @see org.apache.uima.cas.FSIndex#withSnapshotIterators()
     */
    @Override
    public FSIndex<T> withSnapshotIterators() {
      return new IndexImpl<T>(this.iicp, IteratorExtraFunction.SNAPSHOT);
    }

    FSIndexRepositoryImpl getFsRepositoryImpl() {
      return iicp.getCASImpl().indexRepository;
    }
  }  // end of class IndexImpl
  
   
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
     * lazily created comparator using the built-in annotation index, but for ints
     */
    private IntComparator annotationComparator = null;
    
    /**
     * optimization only - bypasses some shared (among views) initialization if already done
     */
    private boolean isSetUpFromBaseCAS = false;
    
    SharedIndexInfo(TypeSystemImpl typeSystem) {
      this.tsi = typeSystem;
    }
  }
  
  private static class ProcessedIndexInfo {
    final private PositiveIntSet fsAddedToIndex = new PositiveIntSet_impl(); // only used when processing updates in batch mode

    final private PositiveIntSet fsDeletedFromIndex = new PositiveIntSet_impl(); // only used when processing updates in batch mode

    final private PositiveIntSet fsReindexed = new PositiveIntSet_impl(); // only used when processing updates in batch mode
  }
  
  /*****  I N S T A N C E   V A R I A B L E S  *****/
  /*****           Replicated per view         *****/                 

  // A reference to the CAS View.
  private final CASImpl cas;

  // Is the index repository locked?
  private boolean locked = false;

  /** 
   * An array of ArrayLists, one for each type in the type hierarchy. 
   * The ArrayLists are unordered lists of IndexIteratorCachePairs for 
   * that type, corresponding to the different index definitions over that type.
   * This list includes iicps for subtypes of defined and default indexes over some type
   */
  final private ArrayList<IndexIteratorCachePair<? extends FeatureStructure>>[] indexArray;
  
  <T extends FeatureStructure> ArrayList<IndexIteratorCachePair<T>> getIndexesForType(int typeCode) {
    return (ArrayList<IndexIteratorCachePair<T>>) (Object) indexArray[typeCode];
  }

  /** 
   * an array of ints, one for each type in the type hierarchy. 
   * Used to enable iterators to detect modifications (adds / removes) 
   * to indexes they're iterating over while they're iterating over them.
   * Not private so it can be seen by FSLeafIndexImpl
   */
  final int[] detectIllegalIndexUpdates;
  
  /**
   * A map from names to IndexIteratorCachePairs, which represent the index at the
   * top-most type declared in the index specification.    
   * Different names may map to the same iicp.
   * The keys are the same across all views, but the values are different, per view
   */
  final private HashMap<String, IndexIteratorCachePair<? extends FeatureStructure>> name2indexMap;
  
  // the next are for journaling updates to indexes
  final private IntVector indexUpdates;

  final private BitSet indexUpdateOperation;

  private boolean logProcessed;


  // Monitor indexes used to optimize getIndexedFS and flush
  // only used for faster access to next set bit
  final private IntVector usedIndexes;

  // one bit per typeCode, indexed by typeCode
  final private boolean[] isUsed;
  
  // Monitor which indexes are iterated over, to allow resetting flatIndexes
  final private List<IndexIteratorCachePair<? extends FeatureStructure>> iteratedSortedIndexes = 
      Collections.synchronizedList(new ArrayList<IndexIteratorCachePair<? extends FeatureStructure>>());
  
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
    this.name2indexMap = new HashMap<String, IndexIteratorCachePair<? extends FeatureStructure>>();
    this.indexUpdates = new IntVector();
    this.indexUpdateOperation = new BitSet();
    this.logProcessed = false;
    this.indexArray = new ArrayList[this.sii.tsi.getNumberOfTypes() + 1];
    this.usedIndexes = new IntVector();
    this.isUsed = new boolean[numTypes];
    init();
  }

  /**
   * Constructor for views.
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
    
    this.name2indexMap = new HashMap<String, IndexIteratorCachePair<? extends FeatureStructure>>();
    this.indexUpdates = new IntVector();
    this.indexUpdateOperation = new BitSet();
    this.logProcessed = false;
    this.indexArray = new ArrayList[numTypes];
    this.usedIndexes = new IntVector();
    this.isUsed = new boolean[numTypes];

    init();
    final Set<String> keys = baseIndexRepo.name2indexMap.keySet();
    if (!keys.isEmpty()) {
      final Iterator<String> keysIter = keys.iterator();
      while (keysIter.hasNext()) {
        final String key = keysIter.next();
        final IndexIteratorCachePair<? extends FeatureStructure> iicp = baseIndexRepo.name2indexMap.get(key);
        createIndexNoQuestionsAsked(iicp.fsLeafIndex.getComparator(), key,
            
            iicp.fsLeafIndex.getIndexingStrategy());
      }
    }
  }

  /**
   * Initialize data. Called from the constructor.
   */
  private void init() {
    final TypeSystemImpl ts = this.sii.tsi;
    // Type counting starts at 1.
    final int numTypes = ts.getNumberOfTypes() + 1;
    // Can't instantiate arrays of generic types.
    for (int i = 1; i < numTypes; i++) {
      this.indexArray[i] = new ArrayList<IndexIteratorCachePair<? extends FeatureStructure>>();
    }
    resetDetectIllegalIndexUpdates();
    mPii = new ProcessedIndexInfo();
//    this.fsAddedToIndex = new PositiveIntSet_impl();
//    this.fsDeletedFromIndex = new PositiveIntSet_impl();
//    this.fsReindexed = new PositiveIntSet_impl();
  }
  
  private void resetDetectIllegalIndexUpdates() {
    for (int i = 0; i < detectIllegalIndexUpdates.length; i++) {
      detectIllegalIndexUpdates[i] = Integer.MIN_VALUE;
    }
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

    for (int i = 0; i < this.usedIndexes.size(); i++) {
      this.isUsed[this.usedIndexes.get(i)] = false;
      ArrayList<IndexIteratorCachePair<? extends FeatureStructure>> v = 
          this.indexArray[this.usedIndexes.get(i)];
      int max = v.size();
      for (int j = 0; j < max; j++) {
        IndexIteratorCachePair<? extends FeatureStructure> iicp = v.get(j);
        iicp.fsLeafIndex.flush();
      }
    }

    clearIteratedSortedIndexes();
    
    // reset the index update trackers
//    resetDetectIllegalIndexUpdates();
    
    this.indexUpdates.removeAllElements();
    this.indexUpdateOperation.clear();
    mPii = new ProcessedIndexInfo();
//    this.fsAddedToIndex = new IntSet();
//    this.fsDeletedFromIndex = new IntSet();
//    this.fsReindexed = new PositiveIntSet_impl();
    this.logProcessed = false;
    this.usedIndexes.removeAllElements();
  }
  
  private void clearIteratedSortedIndexes() {
    int sz = iteratedSortedIndexes.size();
    if (DEBUG) {
      System.out.println("Index Flush flatIndex, size = " + sz);
    }

    for (IndexIteratorCachePair<? extends FeatureStructure> iicp : iteratedSortedIndexes) {
      iicp.flatIndex.flush();
    }
    if (iteratedSortedIndexes.size() != sz) {
      throw new RuntimeException(
          "Index Flush flatIndex, size not the same, before = " + 
          sz + ", after = " + iteratedSortedIndexes.size());
    }
    iteratedSortedIndexes.clear();
  }

  public void addFS(int fsRef) {
    ll_addFS(fsRef);
  }

  private IndexIteratorCachePair<? extends FeatureStructure> addNewIndex(FSIndexComparator comparator, int indexType) {
    return addNewIndex(comparator, DEFAULT_INDEX_SIZE, indexType);
  }

  /**
   * This is where the actual index gets created.
   */
  private <T extends FeatureStructure> IndexIteratorCachePair<T> addNewIndex(final FSIndexComparator comparator, int initialSize,
      int indexType) {
    
    FSLeafIndexImpl<T> fsLeafIndex = addNewIndexCore(comparator, initialSize, indexType);
    IndexIteratorCachePair<T> iicp = new IndexIteratorCachePair<T>(fsLeafIndex); 
//    iicp.fsLeafIndex =  addNewIndexCore(comparator, initialSize, indexType);
    final Type type = comparator.getType();
    final int typeCode = ((TypeImpl) type).getCode();
    // add indexes so that sorted ones are first, to benefit getAllIndexedFSs
    if (indexType == FSIndex.SORTED_INDEX) {
      this.indexArray[typeCode].add(0, iicp);
    } else {
      this.indexArray[typeCode].add(iicp);
    }
    return iicp;
  }
  
  private <T extends FeatureStructure> FSLeafIndexImpl<T> addNewIndexCore(
      final FSIndexComparator comparator, 
      int initialSize,
      int indexType) {
    final Type type = comparator.getType();
    // final int vecLen = indexVector.size();
    FSLeafIndexImpl<T> ind;
    switch (indexType) {
    case FSIndex.SET_INDEX: {
      ind = new FSRBTSetIndex<T>(this.cas, type, indexType);
      break;
    }
    case FSIndex.BAG_INDEX:
    case FSIndex.DEFAULT_BAG_INDEX: {
      ind = new FSBagIndex<T>(this.cas, type, initialSize, indexType);
      break;
    }
    default: {
      // SORTED_INDEX is the default. We don't throw any errors, if the
      // code is unknown, we just create a sorted index (with duplicates).
      // ind = new FSRBTIndex(this.cas, type, FSIndex.SORTED_INDEX);
       
      ind = new FSIntArrayIndex<T>(this.cas, type, initialSize, FSIndex.SORTED_INDEX, isAnnotationIndex(type, comparator));
      break;
    }
    }
    // ind = new FSRBTIndex(this.cas, type);
    // ind = new FSVectorIndex(this.cas, initialSize);
    ind.init(comparator);
    return ind;
  }
  
  private boolean isAnnotationIndex(Type type, FSIndexComparator comp) {
    TypeSystemImpl tsi = cas.getTypeSystemImpl();
    return 
        (type == tsi.annotType) &&
        (comp.getNumberOfKeys() == 3) &&
        (comp.getKeyType(0) == FSIndexComparator.FEATURE_KEY) &&
        (comp.getKeyType(1) == FSIndexComparator.FEATURE_KEY) &&
        (comp.getKeyType(2) == FSIndexComparator.TYPE_ORDER_KEY) &&
        (comp.getKeyComparator(0) == FSIndexComparator.STANDARD_COMPARE) &&
        (comp.getKeyComparator(1) == FSIndexComparator.REVERSE_STANDARD_COMPARE) &&       
        (comp.getKeyComparator(2) == FSIndexComparator.STANDARD_COMPARE) &&
        (comp.getKeyFeature(0) == tsi.startFeat) &&
        (comp.getKeyFeature(1) == tsi.endFeat); 
  }

  /*
   * private IndexIteratorCachePair addIndex( FSIndexComparator comparator, int initialSize) { final
   * Type type = comparator.getType(); final int typeCode = ((TypeImpl) type).getCode(); final
   * Vector indexVector = this.indexArray[typeCode]; final int vecLen = indexVector.size();
   * FSLeafIndexImpl ind;
   * 
   * for (int i = 0; i < vecLen; i++) { ind = ((IndexIteratorCachePair) indexVector.get(i)).index;
   * if (comparator.equals(ind.getComparator())) { return null; } }
   * 
   * ind = new FSRBTIndex(this.cas, type); // ind = new FSVectorIndex(this.cas, initialSize);
   * ind.init(comparator); IndexIteratorCachePair iicp = new IndexIteratorCachePair(); iicp.index =
   * ind; indexVector.add(iicp); return iicp; }
   */
  // private IndexIteratorCachePair addIndexRecursive(FSIndexComparator
  // comparator) {
  // final FSIndexComparatorImpl compCopy =
  // ((FSIndexComparatorImpl) comparator).copy();
  // return addIndexRec(compCopy);
  // }
  
  /**
   * Top level call to add the indexes for a particular index definition
   * @param comparator
   * @param indexType
   * @return the iicp for the top new index
   */
  private IndexIteratorCachePair<? extends FeatureStructure> addNewIndexRecursive(FSIndexComparator comparator, int indexType) {
    final FSIndexComparatorImpl compCopy = ((FSIndexComparatorImpl) comparator).copy();
    return addNewIndexRec(compCopy, indexType);
  }

  /**
   * Finds an index among iicp's for all defined indexes of a type, such that
   *   the type of the index (SET, BAG, SORTED) is the same and 
   *   the comparator (the keys) are the same
   * @param indexes
   * @param comp
   * @param indexType
   * @return the index in the set of iicps for this type for the matching index
   */
  private static final <T extends FeatureStructure> int findIndex(ArrayList<IndexIteratorCachePair<T>> indexes,
      FSIndexComparator comp,
      int indexType) {
    FSIndexComparator indexComp;
    final int max = indexes.size();
    for (int i = 0; i < max; i++) {
      FSLeafIndexImpl<? extends FeatureStructure> index = indexes.get(i).fsLeafIndex;
      if (index.getIndexingStrategy() != indexType) {
        continue;
      }
      indexComp = index.getComparator();
      if (comp.equals(indexComp)) {
        return i;
      }
    }
    return -1;
  }

  /*
   * // Will modify comparator, so call with copy. private IndexIteratorCachePair
   * addIndexRec(FSIndexComparator comp) { FSIndexComparator compCopy; IndexIteratorCachePair cp =
   * this.addIndex(comp); if (cp == null) { return null; // The index already exists. } final Type
   * superType = comp.getType(); final Vector types =
   * this.typeSystem.getDirectlySubsumedTypes(superType); final int max = types.size(); for (int i =
   * 0; i < max; i++) { compCopy = ((FSIndexComparatorImpl)comp).copy(); compCopy.setType((Type)
   * types.get(i)); addIndexRec(compCopy); } return cp; }
   */
  // Will modify comparator, so call with copy.
  
  /**
   * Add an index for a type, and then (unless it's a
   * DEFAULT_BAG_INDEX), call yourself recursively to add the indexes for all the directly subsumed subtypes.
   * @param comparator
   * @param indexType
   * @return the new iicp for the new index
   */
  private IndexIteratorCachePair<? extends FeatureStructure> addNewIndexRec(FSIndexComparator comparator, int indexType) {
    final IndexIteratorCachePair<? extends FeatureStructure> iicp = this.addNewIndex(comparator, indexType);
    if (indexType == FSIndex.DEFAULT_BAG_INDEX) {
      // In this special case, we do not add indexes for subtypes.
      return iicp;
    }
    final Type superType = comparator.getType();
    final Vector<Type> types = this.sii.tsi.getDirectlySubsumedTypes(superType);
    final int max = types.size();
    FSIndexComparator compCopy;
    for (int i = 0; i < max; i++) {
      compCopy = ((FSIndexComparatorImpl) comparator).copy();
      compCopy.setType(types.get(i));
      addNewIndexRec(compCopy, indexType);
    }
    return iicp;
  }

  // includes the original type as element 0  
  private static final ArrayList<Type> getAllSubsumedTypes(Type t, TypeSystem ts) {
    final ArrayList<Type> v = new ArrayList<Type>();
    addAllSubsumedTypes(t, ts, v);
    return v;
  }

  // includes the original type as element 0
  private static final void addAllSubsumedTypes(Type t, TypeSystem ts, ArrayList<Type> v) {
    v.add(t);
    Iterator<Type> it = ((TypeSystemImpl)ts).getDirectSubtypesIterator(t);
    while(it.hasNext()) {
      addAllSubsumedTypes(it.next(), ts, v);
    }
    
//    final List<Type> sub = ts.getDirectSubtypes(t);
//    final int len = sub.size();
//    for (int i = 0; i < len; i++) {
//      addAllSubsumedTypes(sub.get(i), ts, v);
//    }
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
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#createIndex(FSIndexComparator, String)
   */
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
   * @param comp -
   * @param label -
   * @param indexType -
   * @return -
   */
  public boolean createIndexNoQuestionsAsked(final FSIndexComparator comp, String label, int indexType) {
    IndexIteratorCachePair<? extends FeatureStructure> cp = this.name2indexMap.get(label);
    // Now check if the index already exists.
    if (cp == null) {
      // The name is new.
      cp = this.addNewIndexRecursive(comp, indexType);
      
      // create a set of feature codes that are in one or more index definitions
      if (!sii.isSetUpFromBaseCAS) {
        final int nKeys = comp.getNumberOfKeys();
        for (int i = 0; i < nKeys; i++) {
          if (comp.getKeyType(i) == FSIndexComparator.FEATURE_KEY) {
            final int featCode = ((FeatureImpl)comp.getKeyFeature(i)).getCode();
            cas.featureCodesInIndexKeysAdd(featCode);
          }
        }
      }
      
      this.name2indexMap.put(label, cp);
      return true;
    }
    // For now, just return false if the label already exists.
    return false;
    // // An index has previously been registered for this name. We need to
    // // compare the types to see if the new addition is compatible with
    // the
    // // pre-existing one. There are three cases: the new type can be a
    // sub-type
    // // of the old one, in which case we don't need to do anything; or,
    // the
    // // new type is a super-type of the old one, in which case we add the
    // new
    // // index while keeping the old one; or, there is no subsumption
    // relation,
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
  public Iterator<String> getLabels(FSIndexComparator comp) {
    final ArrayList<String> labels = new ArrayList<String>();
    final Iterator<String> it = this.getLabels();
    String label;
    while (it.hasNext()) {
      label = it.next();
      if (this.name2indexMap.get(label).fsLeafIndex.getComparator().equals(comp)) {
        labels.add(label);
      }
    }
    return labels.iterator();
  }

  /**
   * @see org.apache.uima.cas.FSIndexRepository#getIndex(String, Type)
   */

  @SuppressWarnings("unchecked")
  public <T extends FeatureStructure> FSIndex<T> getIndex(String label, Type type) {
    // iicp is for the type the index was defined for
    final IndexIteratorCachePair<? extends FeatureStructure> iicp = this.name2indexMap.get(label);
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
    final Type indexType = iicp.fsLeafIndex.getType();
    if (!this.sii.tsi.subsumes(indexType, type)) {
      final CASRuntimeException cre = new CASRuntimeException(
          CASRuntimeException.TYPE_NOT_IN_INDEX, new String[] { label, type.getName(),
              indexType.getName() });
      throw cre;
    }
    final int typeCode = ((TypeImpl) type).getCode();

    final ArrayList<IndexIteratorCachePair<T>> inds = this.getIndexesForType(typeCode);
    // Since we found an index for the correct type, find() must return a
    // valid result -- unless this is a special auto-index.
    final int indexCode = findIndex(inds, iicp.fsLeafIndex.getComparator(), iicp.fsLeafIndex.getIndexingStrategy());
    if (indexCode < 0) {
      return null;
    }
    // assert((indexCode >= 0) && (indexCode < inds.size()));
    return new IndexImpl<T>((IndexIteratorCachePair<T>) inds.get(indexCode));
    // return ((IndexIteratorCachePair)inds.get(indexCode)).index;
  }
  

  /**
   * @see org.apache.uima.cas.FSIndexRepository#getIndex(String)
   */
  @SuppressWarnings("unchecked")
  public <T extends FeatureStructure> FSIndex<T> getIndex(String label) {
    final IndexIteratorCachePair<? extends FeatureStructure> iicp = this.name2indexMap.get(label);
    if (iicp == null) {
      return null;
    }
    return new IndexImpl<T>((IndexIteratorCachePair<T>) iicp);
    // return ((IndexIteratorCachePair)name2indexMap.get(label)).index;
  }

  public IntPointerIterator getIntIteratorForIndex(String label) {
    final IndexImpl<FeatureStructure> index = (IndexImpl<FeatureStructure>) getIndex(label);
    if (index == null) {
      return null;
    }
    return createPointerIterator(index.iicp);
  }

  public IntPointerIterator getIntIteratorForIndex(String label, Type type) {
    final IndexImpl<FeatureStructure> index = (IndexImpl<FeatureStructure>) getIndex(label, type);
    if (index == null) {
      return null;
    }
    return createPointerIterator(index.iicp);
  }

  public int getIndexSize(Type type) {
    final int typeCode = ((TypeImpl) type).getCode();
    final ArrayList<IndexIteratorCachePair<? extends FeatureStructure>> indexVector = this.indexArray[typeCode];
    if (indexVector.size() == 0) {
      // No index for this type exists.
      return 0;
    }
    int numFSs = indexVector.get(0).fsLeafIndex.size();
    final Vector<Type> typeVector = this.sii.tsi.getDirectlySubsumedTypes(type);
    final int max = typeVector.size();
    for (int i = 0; i < max; i++) {
      numFSs += getIndexSize(typeVector.get(i));
    }
    return numFSs;
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
    final ArrayList<IndexIteratorCachePair<? extends FeatureStructure>> allIndexesForType = this.indexArray[typeCode];
    for (IndexIteratorCachePair<? extends FeatureStructure> iicp : allIndexesForType) {
      iicp.fsLeafIndex.flush();
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
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#createIndexComparator()
   */
  // public FSIndexComparator createIndexComparator() {
  // return new FSIndexComparatorImpl(this.cas);
  // }
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
   * return an array containing all FSs in any defined index, in this view. 
   * This is intended to be used for serialization.
   * 
   * Note that duplicate entries are removed.
   * It used to be that the items were sorted by fs addr, but that only happens if the dedup code runs
   * 
   * The order in which FSs occur in the array does not reflect the order in which they
   * were added to the repository. This means that set indexes deserialized from this list may
   * contain different but equal elements than the original index.
   * @return an array containing all FSs in any defined index, in this view.
   */
  public int[] getIndexedFSs() {
    final IntVector v = new IntVector();  // accumulates fsAddrs from various indexes
    IndexIteratorCachePair<? extends FeatureStructure> iicp;
    IntPointerIterator it;
    ArrayList<IndexIteratorCachePair<? extends FeatureStructure>> iv;
    // We may need to profile this. If this is a bottleneck, use a different
    // implementation.
    IntVector indexedFSs = new IntVector();
    int jMax, indStrat;
    // Iterate over index by type, with something in there
    for (int i = 0; i < this.usedIndexes.size(); i++) {
      iv = this.indexArray[this.usedIndexes.get(i)];
      // Iterate over the indexes for the type.
      jMax = iv.size();
      // https://issues.apache.org/jira/browse/UIMA-4111
      
      // as of v 2.7.0, we can guarantee there's a bag or sorted index for all types
      //   which have had something added to indexes.
      
      // Create a vector of IICPs. 
      // If there is at least one sorted or bag
      // index, pick one arbitrarily and add its FSs (since it contains all
      // FSs that all other indexes for the same type contain). 
      
      IndexIteratorCachePair<? extends FeatureStructure> anIndex = null;
      for (int j = 0; j < jMax; j++) {
        iicp = iv.get(j);
        indStrat = iicp.fsLeafIndex.getIndexingStrategy();
        if (indStrat != FSIndex.SET_INDEX) {  // don't use SET indexes because 
                                              // they miss some items which have been added to the indexes
          anIndex = iicp;
          break;
        }
      }
      assert (anIndex != null);

      // Note: This next loop removes duplicates (and also sorts
      // the fs addrs associated with one type)
      // Duplicates arise from having an index having the same identical FS added
      // multiple times.
      if (IS_ALLOW_DUP_ADD_2_INDEXES) {
        indexedFSs.removeAllElements();
        // get an iterator over just the leaf index for this type itself, excluding subtypes
        it = anIndex.fsLeafIndex.refIterator();
        while (it.isValid()) {
          indexedFSs.add(it.get());
          it.inc();
        }
        // sort and remove duplicates
        indexedFSs.sortDedup();
        // add to previously collected types
        v.add(indexedFSs.getArray(), 0, indexedFSs.size());  // bulk add of all elements
      } else {
        anIndex.fsLeafIndex.bulkAddTo(v);
      }
    }  // loop to accumulate in v all for all types
    return v.toArray();
  }

  /**
   * @see org.apache.uima.cas.FSIndexRepository#addFS(org.apache.uima.cas.FeatureStructure)
   */
  public void addFS(FeatureStructure fs) {
    addFS(((FeatureStructureImpl) fs).getAddress());
  }

  private void incrementIllegalIndexUpdateDetector(int typeCode) {
    this.detectIllegalIndexUpdates[typeCode] ++;
//    indexUpdated(typeCode);
  }

  /**
   * @see org.apache.uima.cas.FSIndexRepository#removeFS(org.apache.uima.cas.FeatureStructure)
   */
  public void removeFS(FeatureStructure fs) {
    ll_removeFS(this.cas.ll_getFSRef(fs));

    // final int typeCode =
    // this.cas.ll_getFSRefType(this.cas.ll_getFSRef(fs));
    // // final TypeImpl type = (TypeImpl) fs.getType();
    // ArrayList idxList = this.indexArray[typeCode];
    // final int max = idxList.size();
    // incrementIllegalIndexUpdateDetector(typeCode);
    // for (int i = 0; i < max; i++) {
    // ((IndexIteratorCachePair) idxList.get(i)).index.deleteFS(fs);
    // }
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

  // private static final void bubbleSort(Object[] array, int end)
  // {
  // int comp;
  // Object tmp;
  // for (int i = (end - 1); i >= 0; i--)
  // {
  // for (int j = 1; j <= i; j++)
  // {
  // comp = ((Comparable) array[j - 1]).compareTo(array[j]);
  // if (comp > 0)
  // {
  // tmp = array[j - 1];
  // array[j - 1] = array[j];
  // array[j] = tmp;
  // }
  // }
  // }
  // }

  public LowLevelIndex ll_getIndex(String indexName) {
    return (LowLevelIndex) getIndex(indexName);
  }

  public LowLevelIndex ll_getIndex(String indexName, int typeCode) {
    if (!this.sii.tsi.isType(typeCode) || !this.cas.ll_isRefType(typeCode)) {
      final LowLevelException e = new LowLevelException(LowLevelException.INVALID_INDEX_TYPE);
      e.addArgument(Integer.toString(typeCode));
      throw e;
    }
    return (LowLevelIndex) getIndex(indexName, this.sii.tsi.ll_getTypeForCode(typeCode));
  }

  public final void ll_addFS(int fsRef, boolean doChecks) {
    if (doChecks) {
      this.cas.checkFsRef(fsRef);
      this.cas.ll_isRefType(this.cas.ll_getFSRefType(fsRef));
    }
    ll_addFS(fsRef);
  }

  public void ll_addFS(int fsRef) {
    ll_addFS_common(fsRef, false, 1);
  }
  
  public void ll_addback(int fsRef, int count) {
    ll_addFS_common(fsRef, true, count);
  }
  
  private void ll_addFS_common(int fsRef, boolean isAddback, int count) {
    cas.maybeClearCacheNotInIndex(fsRef);
    // Determine type of FS.
    final int typeCode = this.cas.getTypeCode(fsRef);

    // https://issues.apache.org/jira/browse/UIMA-4099
    // skip test for wrong view if addback, etc.
    if (!isAddback && (!IS_DISABLE_ENHANCED_WRONG_INDEX_CHECK) && sii.tsi.isAnnotationBaseOrSubtype(typeCode)) {
      final int sofaAddr = cas.getSofaFeat(fsRef);
      if (sofaAddr == 0) {
        throw new CASRuntimeException(
            CASRuntimeException.SOFAREF_NOT_SET, new String[] {
                ((FeatureStructureImpl)(cas.ll_getFSForRef(fsRef))).toString()});            
      }
      if (!cas.isSofaView(sofaAddr)) {
        AnnotationBaseImpl fs_abi = new AnnotationBaseImpl(fsRef, cas);
        SofaFS annotSofaFS = cas.getSofa(sofaAddr);
        SofaFS viewSofaFS  = cas.getSofa(cas.getSofaRef());
        
        CASRuntimeException e = new CASRuntimeException(
            CASRuntimeException.ANNOTATION_IN_WRONG_INDEX, new String[] { 
                fs_abi.toString(),
                annotSofaFS.getSofaID(), 
                viewSofaFS.getSofaID()});
        throw e;
      }
    }
   
    // indicate this type's indexes are being modified
    // in case an iterator is simultaneously active over this type
    incrementIllegalIndexUpdateDetector(typeCode);
    // Get the indexes for the type.
    final ArrayList<IndexIteratorCachePair<? extends FeatureStructure>> indexes = this.indexArray[typeCode];
    // Add fsRef to all indexes.
    boolean noIndexOrOnlySetindexes = true;
    for (IndexIteratorCachePair<? extends FeatureStructure> iicp : indexes) {
      final int indexingStrategy = iicp.fsLeafIndex.getIndexingStrategy(); 
      if (isAddback) {
        if (indexingStrategy == FSIndex.BAG_INDEX) {
        continue;  // skip adding back to bags - because removes are skipped for bags
        }
        iicp.fsLeafIndex.insert(fsRef, count);
      } else {
        iicp.fsLeafIndex.insert(fsRef);  // if not addback, only insert 1
      }
      if (noIndexOrOnlySetindexes) {
        noIndexOrOnlySetindexes = indexingStrategy == FSIndex.SET_INDEX;
      }
    }
    // log even if added back, because remove logs remove, and might want to know it was "reindexed"
    if (this.cas.getCurrentMark() != null) {
      logIndexOperation(fsRef, true);
    }
    
    if (isAddback) { return; }
    
    // https://issues.apache.org/jira/browse/UIMA-4111
    if (noIndexOrOnlySetindexes) {
      // lazily create a default bag index for this type
      final Type type = this.sii.tsi.ll_getTypeForCode(typeCode);
      final String defIndexName = getAutoIndexNameForType(type);
      final FSIndexComparator comparator = createComparator();
      comparator.setType(type);
      createIndexNoQuestionsAsked(comparator, defIndexName, FSIndex.DEFAULT_BAG_INDEX);

      // add the FS to the bag index
      // which is the last one added
      indexes.get(indexes.size() - 1).fsLeafIndex.insert(fsRef);
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

  boolean ll_removeFS_ret(int fsRef) {
    final int typeCode = this.cas.ll_getFSRefType(fsRef);
    incrementIllegalIndexUpdateDetector(typeCode);
    final ArrayList<IndexIteratorCachePair<? extends FeatureStructure>> idxList = this.indexArray[typeCode];
    final int max = idxList.size();
    boolean atLeastOneRemoved = false;
    for (int i = 0; i < max; i++) {
      atLeastOneRemoved |= idxList.get(i).fsLeafIndex.remove(fsRef);
    }
    if (atLeastOneRemoved) {
      if (this.cas.getCurrentMark() != null) {
        logIndexOperation(fsRef, false);
      }
    }
    return atLeastOneRemoved;    
  }

  /**
   * Remove potentially multiple times from all defined indexes for this view
   * @param fsRef the item to remove
   * @return the number of times the FS was added to the index, may be 0
   */
  int ll_removeFS_all_ret(int fsRef) {
    int countOfRemoved = 0;
    boolean wasRemoved;
    // loop until remove fails
    do {
      wasRemoved = ll_removeFS_ret(fsRef);
      if (wasRemoved) {
        countOfRemoved++;
      }
    } while(wasRemoved);
    return countOfRemoved;
  }
  
  @Override
  public void ll_removeFS(int fsRef) {
    ll_removeFS_ret(fsRef);
  }
  
  public LowLevelIterator ll_getAllIndexedFS(Type type) {
    final List<LowLevelIterator> iteratorList = new ArrayList<LowLevelIterator>();
    ll_getAllIndexedFS(type, iteratorList);
    return
        (iteratorList.size() == 0) ? emptyLlIterator :
          (iteratorList.size() == 1) ? iteratorList.get(0) : 
            new LowLevelIteratorAggregate(iteratorList);
  }
  
  private final void ll_getAllIndexedFS(Type type, List<LowLevelIterator> iteratorList) {
    // get all indexes for this type
    ArrayList<IndexIteratorCachePair<FeatureStructure>> iicps =  this.getIndexesForType(((TypeImpl)type).getCode());

    IndexIteratorCachePair<FeatureStructure> iicpSorted = null;
    IndexIteratorCachePair<FeatureStructure> iicpBag = null;
    IndexIteratorCachePair<FeatureStructure> iicpDefaultBag = null;
    
    for (IndexIteratorCachePair<FeatureStructure> iicp : iicps) {
      int indexKind = iicp.fsLeafIndex.getIndexingStrategy();
      
      // Try to do a flattened index
      if (indexKind == FSIndex.SORTED_INDEX) {
        if (iicp.hasFlatIndex()) {
          FSIteratorFlat<FeatureStructure> flatIterator = (FSIteratorFlat<FeatureStructure>) iicp.flatIndex.iterator();
          if (flatIterator != null) {
            if (FSIndexFlat.trace) {
              System.out.format("FSIndexFlattened getAllIndexedFS use: %s%n", flatIterator.toString());
            }
            iteratorList.add(flatIterator);
            return;
          }
        }
        iicpSorted = iicp;  // remember in case want to use later
        continue;
      }
      
      // past all sorted indexes in the list, non of which had a flattened version (but there are more items in the list)
      if (null != iicpSorted) {
        break;  // will use this one 
      }
      
      // past the spot in the list where sorted indexes are, no sorted indexes found (but there are more items in the list)
      
      if (indexKind == FSIndex.BAG_INDEX) {
        iicpBag = iicp; 
        break;
      }
      
      if (indexKind == FSIndex.DEFAULT_BAG_INDEX) {
        iicpDefaultBag = iicp;
        break;  // no need to keep scanning, if have default bag, there is no real bag index
     }
    } // end of all indexes for this type scan

    if (null != iicpSorted) {
      if (iicpSorted.has1OrMoreEntries()) {
        iteratorList.add(new IndexImpl<FeatureStructure>(iicpSorted, IteratorExtraFunction.UNORDERED).ll_iterator());
      }
      return;
    }

    if (null != iicpBag) {
      if (iicpBag.has1OrMoreEntries()) {
        iteratorList.add(new IndexImpl<FeatureStructure>(iicpBag).ll_iterator());
      }
      return;
    }

    // If get here, there are only Set or default bag indexes for this type 
    // default bag index is guaranteed to exist if any FS of Type type were added to the indexes
    //   https://issues.apache.org/jira/browse/UIMA-4111
    if (iicpDefaultBag != null) {      // There must be a default bag index defined if any FSs of this type were added to indexes
      iteratorList.add(new IndexImpl<FeatureStructure>(iicpDefaultBag).ll_iterator());
      // We found one of the special auto-indexes which don't inherit down the tree. So, we
      // manually need to traverse the inheritance tree to look for more indexes. Note that
      // this is not necessary when we have a regular index
      ll_addDirectSubtypes(type, iteratorList);      
      return;
    }
    
    // No index for this type was found at all. 
    // Example:  You ask for an iterator over "TOP", but no instances of TOP are created,
    //   and no index over TOP was ever created.
    // Since the auto-indexes are created on demand for
    //   each type, there may be gaps in the inheritance chain. So keep descending the inheritance
    //   tree looking for relevant indexes.
    ll_addDirectSubtypes(type, iteratorList);          
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndexRepository#getAllIndexedFS(org.apache.uima.cas.Type)
   */
  public <T extends FeatureStructure> FSIterator<T> getAllIndexedFS(Type type) {
    final List<FSIteratorImplBase<T>> iteratorList = new ArrayList<>();
    getAllIndexedFS(type, iteratorList);
    return 
        (iteratorList.size() == 0) ? emptyFSIterator :  
          (iteratorList.size() == 1) ? iteratorList.get(0) : new FSIteratorAggregate<T>(iteratorList);
  }

  private final <T extends FeatureStructure> void getAllIndexedFS(Type type, List<FSIteratorImplBase<T>> iteratorList) {
    // Strategy:  go through the list of all indexes for this type.
    //   The list is intentially ordered when created to have "SORTED" indexes come first.
    //   
    //   Check all of the Sorted indexes to see if any have a flatten iterator, and if found use that.
    //
    //   If no sorted, flattened indexes exist, use any sorted index, but run as unordered to avoid rattling iterators
    //    
    //   If no sorted index exists, use Bag or Default-bag index.  If default-bag, call recursively to get sub-indexes.
    //   
    //   If no sorted or non-default bag index exists (must be a SET or DEFAULT_BAG) 
    //     if default-bag exists, use that plus call this recursively on direct subtypes
    //     if no default-bag index, call this recursively on direct subtypes
    //   Note that a default bag index is guaranteed to exist if any FS of Type type were added to the indexes
    //     and only a SET index was defined, see https://issues.apache.org/jira/browse/UIMA-4111

    // get all indexes for this type
    ArrayList<IndexIteratorCachePair<T>> iicps =  this.getIndexesForType(((TypeImpl)type).getCode());
    
    IndexIteratorCachePair<T> iicpSorted = null;
    IndexIteratorCachePair<T> iicpBag = null;
    IndexIteratorCachePair<T> iicpDefaultBag = null;
    
    for (IndexIteratorCachePair<T> iicp : iicps) {
      int indexKind = iicp.fsLeafIndex.getIndexingStrategy();
      
      // Try to do a flattened index
      if (indexKind == FSIndex.SORTED_INDEX) {
        if (iicp.hasFlatIndex()) {
          FSIterator<T> flatIterator = (FSIterator<T>) iicp.flatIndex.iterator();
          if (flatIterator != null) {
            if (FSIndexFlat.trace) {
              System.out.format("FSIndexFlattened getAllIndexedFS use: %s%n", flatIterator.toString());
            }
            iteratorList.add((FSIteratorImplBase<T>) flatIterator);
            return;
          }
        }
        iicpSorted = iicp;  // remember in case want to use later
        continue;
      }
      
      // past all sorted indexes in the list, non of which had a flattened version (but there are more items in the list)
      
      if (null != iicpSorted) {
        break;  // will use this one
      }
      
      // past the spot in the list where sorted indexes are, no sorted indexes found (but there are more items in the list)
      
      if (indexKind == FSIndex.BAG_INDEX) {
        iicpBag = iicp;
        break;
      }
      
      if (indexKind == FSIndex.DEFAULT_BAG_INDEX) {
        iicpDefaultBag = iicp;
        break;  // no need to keep scanning, if have default bag, there is no real bag index
      }
    }
        
    if (null != iicpSorted) {
      if (iicpSorted.has1OrMoreEntries()) {
        iteratorList.add((FSIteratorImplBase<T>) new IndexImpl<T>((IndexIteratorCachePair<T>) iicpSorted, IteratorExtraFunction.UNORDERED).iterator());
        // even though this is not a sorted index (because of UNORDERED) and therefore won't normally increment the
        // count used to figure out when to create a flattened index, increment this count to 
        // get a small speedup by caching the Java cover classes.
        
        FSIndexFlat<? extends FeatureStructure> flatindex = iicpSorted.flatIndex;
        if (flatindex != null) { // means we don't have a flat version yet, but are counting toward making one
          final int iicpsize = iicpSorted.guessedSize();  
          flatindex.incrementReorderingCount(iicpsize);
          if (DEBUG) {
            System.out.println(String.format("GetAllIndexes over type %s incrementing reordering count by %,d",
                type.getName(), iicpsize));
          }
        }
      }
      return;
    }
    
    if (null != iicpBag) {
      if (iicpBag.has1OrMoreEntries()) {
        iteratorList.add((FSIteratorImplBase<T>) new IndexImpl<T>(iicpBag).iterator());
      }
      return;
    }

    // If get here, there are only Set or default bag indexes for this type 
    // default bag index is guaranteed to exist if any FS of Type type were added to the indexes
    //   https://issues.apache.org/jira/browse/UIMA-4111
    if (iicpDefaultBag != null) {      // There must be a default bag index defined if any FSs of this type were added to indexes
      iteratorList.add((FSIteratorImplBase<T>) new IndexImpl<T>(iicpDefaultBag).iterator());
      // We found one of the special auto-indexes which don't inherit down the tree. So, we
      // manually need to traverse the inheritance tree to look for more indexes. Note that
      // this is not necessary when we have a regular index
      addDirectSubtypes(type, iteratorList);      
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
  
//  boolean isFsInAnyIndex(int fsAddr) {
//    final int typeCode = cas.getTypeCode(fsAddr);
//    if (cas.getTypeSystemImpl().subsumes(superType, type))
//  }

  private <T extends FeatureStructure> void addDirectSubtypes(Type type, List<FSIteratorImplBase<T>> iteratorList) {
    Iterator<Type> typeIterator = this.sii.tsi.getDirectSubtypesIterator(type);
    while(typeIterator.hasNext()) {
      getAllIndexedFS(typeIterator.next(), iteratorList);
    }   
  }
  
  private void ll_addDirectSubtypes(Type type, List<LowLevelIterator> iteratorList) {
    Iterator<Type> typeIterator = this.sii.tsi.getDirectSubtypesIterator(type);
    while(typeIterator.hasNext()) {
      Type nextType = typeIterator.next();
      ll_getAllIndexedFS(nextType, iteratorList);
    }   
  }
    
  /**
   * This is used to see if a FS which has a key feature being modified
   * could corrupt an index in this view.  It returns true if found 
   * (sometimes it returns true, even if strictly speaking, there is 
   * no chance of corruption - see below)
   * 
   * It does this by seeing if this FS is indexed by one or more Set or Sorted
   * indexes.
   * 
   * If found in the first Sorted index encountered, return true
   * Else, if found in any Set index, return true, otherwise false 
   * 
   *   To speed up the 2nd case, when there are more than one Set indexes, 
   *   we do an approximation if there are any bag indexes: we check if found in 
   *   the first bag index encountered -- return true if found
   *   
   *      This is an approximation in that it could be that none of the Set 
   *      indexes contain that FS, but it is in the bag index.
   *      So, this method can sometimes return true incorrectly. 
   *      
   * If type is subtype of annotation, can just test the built-in
   * Annotation index.
   * 
   * @param fsAddr the FS to see if it is in some index that could be corrupted by a key feature value change
   * @return true if this fs is found in a Set or Sorted index.  
   */
  public boolean isInSetOrSortedIndexInThisView(int fsAddr) {
    final int typeCode = cas.getTypeCode(fsAddr);
    
    // if subtype of Annotation, use that index to see if the fs is indexed
    if (sii.tsi.isAnnotationOrSubtype(typeCode)) {
      return (getAnnotationIndexNoSubtypes(typeCode).ll_containsEq(fsAddr));
    }

    // otherwise, check all the indexes for this type. 
    final ArrayList<IndexIteratorCachePair<? extends FeatureStructure>> indexesForType = indexArray[typeCode];
    FSBagIndex<? extends FeatureStructure> index_bag = null;
    boolean found_in_bag = false;
    ArrayList<FSRBTSetIndex<? extends FeatureStructure>> setindexes = null;

    for (IndexIteratorCachePair<? extends FeatureStructure> iicp : indexesForType) {
      FSLeafIndexImpl<?> index_for_this_typeCode = iicp.fsLeafIndex;
      final int kind = index_for_this_typeCode.getIndexingStrategy(); // SORTED_INDEX, BAG_, or SET_
      if (kind == FSIndex.SORTED_INDEX) {
        return ((FSIntArrayIndex<?>)index_for_this_typeCode).ll_containsEq(fsAddr);
      }
      if (kind == FSIndex.BAG_INDEX && !found_in_bag) {
        if (FSBagIndex.USE_POSITIVE_INT_SET) {
          found_in_bag = ((FSBagIndex<?>)index_for_this_typeCode).ll_contains(fsAddr);
          if (!found_in_bag) {
            return false; 
          }
          continue; // may still return false if no set or sorted indexes
        } 
        index_bag = (FSBagIndex<?>) index_for_this_typeCode;
      } else {  // is Set case
        if (setindexes == null) {
          setindexes = new ArrayList<FSRBTSetIndex<?>>();
        }
        setindexes.add((FSRBTSetIndex<?>) index_for_this_typeCode);
      } 
    }
    // if get here, there's no Sorted index
    if (setindexes == null) {
      return false;  // is not in any set or sorted index for this type, so return false
    }
    
    // there is one or more Set indexes
    if (setindexes.size() == 1) { 
      return setindexes.get(0).ll_contains(fsAddr);
    }
    // there is more than 1 set indexes, try to substitute a bag test
    if (found_in_bag) {
      return true;
    }
    
    if (index_bag != null) {
      return (index_bag).ll_contains(fsAddr);
    }
    
    // there are no bag indexes.  Need to check each Set index, and return true if any of them contain this FS
    
    for (FSRBTSetIndex<?> index_set : setindexes) {
      if (index_set.ll_contains(fsAddr)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * This is used when deserializing a FS using delta CAS which could be
   * modifying an existing one (below the line).  In order to update 
   * if the FS is in any index (in any view) and
   *    it has new key values for some key used in the indexes,
   *    it has to be removed, updated, and then readded back to the indexes
   * The current implementation does not try to determine if any keys are being updated,
   *    it just assumes one or more are.   
   * 
   * Optimization: If type is subtype of annotation, can just test the built-in
   * Annotation index.
   * 
   * If the view has nothing other than bag indexes for this type, return false without doing any remove
   * 
   * @param fsRef - the FS to see if it is in some index that could be corrupted by a key feature value change
   * @return count of number of instances of this fs that were removed in this view, if the view had one or more Set or Sorted indexes,
   *         otherwise 0  
   */
  int removeIfInCorrputableIndexInThisView(int fsAddr) {
    final int typeCode = cas.getTypeCode(fsAddr);
    
    // if subtype of Annotation is corruptable, so do remove.
    if (sii.tsi.isAnnotationOrSubtype(typeCode)) {
      return ll_removeFS_all(fsAddr);
    }

    // otherwise, check all the indexes for this type to see if there is a Set or Sorted one. 

    for (IndexIteratorCachePair<? extends FeatureStructure> iicp : indexArray[typeCode]) {
      FSLeafIndexImpl<?> index_for_this_typeCode = iicp.fsLeafIndex;
      final int kind = index_for_this_typeCode.getIndexingStrategy(); // SORTED_INDEX, BAG_, or SET_
      if (kind == FSIndex.SORTED_INDEX || kind == FSIndex.SET_INDEX) {
        // next call removes from all defined indexes for this type
        return ll_removeFS_all(fsAddr);
      }
    }
    return 0;
  }
  /**
   * Remove this fsAddr from all defined indexes in this view for this type
   * @param fsAddr - the item to remove all instances of
   * @return the number of times this FS was added to the indexes, may be 0 if not in any index
   */
  int ll_removeFS_all(int fsAddr) {
    return (IS_ALLOW_DUP_ADD_2_INDEXES) ? 
        ll_removeFS_all_ret(fsAddr) : 
        ((ll_removeFS_ret(fsAddr)) ? 1 : 0);
  }
  
//  /**
//   * reset the flat index is valid for this type
//   */
//  private void indexUpdated(int typeCode) {
//    flattenedIndexValid.clear(typeCode);       
//  }


  /**
   * returns the annotation index for a type which is Annotation or a subtype of it.
   * @param typeCode
   * @return the index just for that type
   */
  private <T extends FeatureStructure> FSIntArrayIndex<T> getAnnotationIndexNoSubtypes(int typeCode) {
    final IndexIteratorCachePair<? extends FeatureStructure> annotation_iicp = this.name2indexMap.get(CAS.STD_ANNOTATION_INDEX);
    final ArrayList<IndexIteratorCachePair<T>> iicps_for_type = getIndexesForType(typeCode);
    final FSLeafIndexImpl<?> ri = annotation_iicp.fsLeafIndex;
    // search all defined indexes for this type, to find an annotation one
    final int ii = findIndex(iicps_for_type, ri.getComparator(), FSIndex.SORTED_INDEX);
    return (FSIntArrayIndex<T>) iicps_for_type.get(ii).fsLeafIndex; // cast ok because annotation index is sorted
  }
  
  private void logIndexOperation(int fsRef, boolean added) {
    this.indexUpdates.add(fsRef);
    if (added) {
      this.indexUpdateOperation.set(this.indexUpdates.size() - 1, added);
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
      final int fsRef = this.indexUpdates.get(i);
      final boolean added = this.indexUpdateOperation.get(i);
      if (added) {
        boolean wasRemoved = pii.fsDeletedFromIndex.remove(fsRef);
        if (wasRemoved) {
          pii.fsReindexed.add(fsRef);  
        } else if (pii.fsReindexed.contains(fsRef)) {
          continue;  // add on top of reindex is ignored
        } else {  // wasn't in deleted, wasn't in reindexed
          pii.fsAddedToIndex.add(fsRef);
        }
      } else {
        // operation was remove-from-indexes
        boolean wasRemoved = pii.fsAddedToIndex.remove(fsRef);
        if (!wasRemoved) {
          pii.fsReindexed.remove(fsRef);
          pii.fsDeletedFromIndex.add(fsRef);
        }
      }
    }
    this.logProcessed = true;
    this.indexUpdates.removeAllElements();
    this.indexUpdateOperation.clear();
  }
  
  public int[] getUpdatedFSs(PositiveIntSet items) {
    if (!this.logProcessed) {
      processIndexUpdates();
    }
    return items.toIntArray();    
  }
  
  public int[] getAddedFSs() {
    return getUpdatedFSs(mPii.fsAddedToIndex);
  }

  public int[] getDeletedFSs() {
    return getUpdatedFSs(mPii.fsDeletedFromIndex);
  }

  public int[] getReindexedFSs() {
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
    return "FSIndexRepositoryImpl [" + cas + "]";
  }
  
//  public Comparator<AnnotationFS> getAnnotationComparator() {
//    if (null == this.sii.annotationComparator) {
//      @SuppressWarnings("unchecked")
//      final IndexIteratorCachePair<AnnotationFS> iicp = 
//          (IndexIteratorCachePair<AnnotationFS>) this.name2indexMap.get(CAS.STD_ANNOTATION_INDEX);
//      this.sii.annotationComparator = (FSIntArrayIndex<AnnotationFS>)(iicp.fsLeafIndex);
//    }
//    return this.sii.annotationComparator;
//  }
  
  Comparator<AnnotationFS> getAnnotationFsComparator() {
    Comparator<AnnotationFS> r = this.sii.annotationFsComparator;
    if (null == r) {

      final CASImpl ci = cas;
      TypeSystemImpl tsi = ci.getTypeSystemImpl();
      final int beginOffset = ci.getFeatureOffset(TypeSystemImpl.startFeatCode);
      final int endOffset = ci.getFeatureOffset(TypeSystemImpl.endFeatCode);
      final LinearTypeOrder typeOrder = getDefaultTypeOrder();
      
      return this.sii.annotationFsComparator = new Comparator<AnnotationFS>() {

        @Override
        public int compare(AnnotationFS o1, AnnotationFS o2) {
          
          final int fs1 = ((FeatureStructureImpl)o1).getAddress();
          final int fs2 = ((FeatureStructureImpl)o2).getAddress();
          if (fs1 == fs2) return 0;
          
          final int b1 = ci.getHeapValue(fs1 + beginOffset);
          final int b2 = ci.getHeapValue(fs2 + beginOffset);
          if (b1 < b2) return -1;
          if (b1 > b2) return 1;
          
          final int e1 = ci.getHeapValue(fs1 + endOffset);
          final int e2 = ci.getHeapValue(fs2 + endOffset);
          if (e1 > e2) return -1;  // reverse
          if (e1 < e2) return 1;
          
          final int tc1 = ci.getTypeCode(fs1);
          final int tc2 = ci.getTypeCode(fs2);
          
          return (tc1 == tc2) ? 0 : 
            ((typeOrder.lessThan(ci.getTypeCode(fs1), ci.getTypeCode(fs2))) ? -1 : 1);
        }
      };
    }
    return r;
  }
 
  IntComparator getAnnotationIntComparator() {
    IntComparator r = this.sii.annotationComparator;
    if (null == r) {

      final CASImpl ci = cas;
      final int beginOffset = ci.getFeatureOffset(TypeSystemImpl.startFeatCode);
      final int endOffset = ci.getFeatureOffset(TypeSystemImpl.endFeatCode);
      final LinearTypeOrder typeOrder = getDefaultTypeOrder();
      
      return this.sii.annotationComparator = new IntComparator() {

        @Override
        public int compare(int fs1, int fs2) {
          
          if (fs1 == fs2) return 0;
          
          final int b1 = ci.getHeapValue(fs1 + beginOffset);
          final int b2 = ci.getHeapValue(fs2 + beginOffset);
          if (b1 < b2) return -1;
          if (b1 > b2) return 1;
          
          final int e1 = ci.getHeapValue(fs1 + endOffset);
          final int e2 = ci.getHeapValue(fs2 + endOffset);
          if (e1 > e2) return -1;  // reverse
          if (e1 < e2) return 1;
          
          final int tc1 = ci.getTypeCode(fs1);
          final int tc2 = ci.getTypeCode(fs2);
          if (tc1 == tc2) {
            return 0;
          }
          return (typeOrder.lessThan(tc1, tc2)) ? -1 : 1;
        }
      };
    }
    return r;
  }

  
}
