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
import java.util.BitSet;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Vector;

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
import org.apache.uima.internal.util.ComparableIntPointerIterator;
import org.apache.uima.internal.util.IntComparator;
import org.apache.uima.internal.util.IntPointerIterator;
import org.apache.uima.internal.util.IntSet;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.util.Misc;

public class FSIndexRepositoryImpl implements FSIndexRepositoryMgr, LowLevelIndexRepository {

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
    UNORDERED, // unordered iterators
  }
    

  
  // Implementation note: the use of equals() here is pretty hairy and
  // should probably be fixed. We rely on the fact that when two
  // FSIndexComparators are compared, the type of the comparators is
  // ignored! A fix for this would be to split the FSIndexComparator
  // class into two classes, one for the key-comparator pairs, and one
  // for the combination of the two. Note also that we compare two
  // IndexIteratorCachePairs by comparing their
  // index.getComparator()s.

  /**
   * A pair of an index and an iterator cache. An iterator cache is the set of all indexes necessary
   * to create an iterator for the type of the index.
   * 
   *  This includes the index for the type of this index, as well as all subtypes.
   *  
   * compareTo() is based on types and the
   * comparator of the index.
   */
  private class IndexIteratorCachePair implements Comparable<IndexIteratorCachePair> {

    // The "root" index, i.e., index of the type of the iterator.
    private FSLeafIndexImpl<?> index = null;

    // A list of indexes (the sub-indexes that we need for an
    // iterator). I.e., one index for each type that's subsumed by the
    // iterator's type.
    private ArrayList<FSLeafIndexImpl<?>> iteratorCache = null;
    
    // VOLATILE to permit double-checked locking technique
    private volatile boolean isIteratorCacheSetup = false;

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("IndexIteratorCachePair, index=");
      sb.append(index).append('\n');
      int i = 0;
      if (!isIteratorCacheSetup) {
        sb.append(" cache not set up yet");
      } else {  
        for (FSLeafIndexImpl lii : iteratorCache) {
          sb.append("  cache ").append(i++);
          sb.append("  ").append(lii).append('\n');
        }
      }
      return sb.toString();
    }

    private IndexIteratorCachePair() {}

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
      final IndexIteratorCachePair iicp = (IndexIteratorCachePair) o;
      return this.index.getComparator().equals(iicp.index.getComparator())
          && (this.index.getIndexingStrategy() == iicp.index.getIndexingStrategy());
    }

// if this throws, then the Eclipse debugger fails to show the object saying 
// com.sun.jdi.InvocationException occurred invoking method. 
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + this.index.getComparator().hashCode();
      result = prime * result + this.index.getIndexingStrategy();
      return result;
    }



    // Populate the cache.
    // For read-only CASes, this may be called on multiple threads, so do some synchronization
        
    private void createIndexIteratorCache() {
      // using double-checked sync - see http://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
      if (isIteratorCacheSetup) {
        return;
      }
      synchronized (this) {
        if (isIteratorCacheSetup) {
          return;
        }
        final ArrayList<FSLeafIndexImpl<?>> tempIteratorCache = new ArrayList<FSLeafIndexImpl<?>>();
        final Type rootType = this.index.getComparator().getType();
        ArrayList<Type> allTypes = null;
        if (this.index.getIndexingStrategy() == FSIndex.DEFAULT_BAG_INDEX) {
          allTypes = new ArrayList<Type>();
          allTypes.add(rootType);
        } else {
          // includes the original type as element 0
          allTypes = getAllSubsumedTypes(rootType, FSIndexRepositoryImpl.this.sii.tsi);
        }
        final int len = allTypes.size();
        int typeCode, indexPos;
        ArrayList<IndexIteratorCachePair> indexList;
        for (int i = 0; i < len; i++) {
          typeCode = ((TypeImpl) allTypes.get(i)).getCode();
          indexList = FSIndexRepositoryImpl.this.indexArray[typeCode];
          indexPos = indexList.indexOf(this);
          if (indexPos >= 0) {
            tempIteratorCache.add(indexList.get(indexPos).index);
          }
        }
        this.iteratorCache = tempIteratorCache; 
        // assign to "volatile" at end, after all initialization is complete
        this.isIteratorCacheSetup = true;
      }  // end of synchronized block
    }


    /**
     * @see java.lang.Comparable#compareTo(Object)
     */
    public int compareTo(IndexIteratorCachePair o) {
      final IndexIteratorCachePair cp = o;
      final int typeCode1 = ((TypeImpl) this.index.getType()).getCode();
      final int typeCode2 = ((TypeImpl) cp.index.getType()).getCode();
      if (typeCode1 < typeCode2) {
        return -1;
      } else if (typeCode1 > typeCode2) {
        return 1;
      } else { // types are equal
        return this.index.getComparator().compareTo(cp.index.getComparator());
      }
    }

    int size() {
      int size = 0;
      createIndexIteratorCache();  // does nothing if already created
      final ArrayList<FSLeafIndexImpl<?>> localIc = this.iteratorCache;
      final int len = localIc.size();
      for (int i = 0; i < len; i++) {
        size += localIc.get(i).size();
      }
      return size;
    }

  }  // end of class definition for IndexIteratorCachePair

  IntPointerIterator createPointerIterator(IndexIteratorCachePair iicp) {
    return createPointerIterator(iicp, false);
  }

  IntPointerIterator createPointerIterator(IndexIteratorCachePair iicp, boolean is_unordered) {
    iicp.createIndexIteratorCache();
    if (iicp.iteratorCache.size() > 1) {
      if (iicp.index.getIndexingStrategy() == FSIndex.BAG_INDEX || is_unordered) {
        return new PointerIteratorUnordered(iicp);
      } else {
        return new PointerIterator(iicp);
      }
    }
    return new LeafPointerIterator(iicp);
  }

  IntPointerIterator createPointerIterator(IndexIteratorCachePair iicp, int fs) {
    iicp.createIndexIteratorCache();
    if (iicp.iteratorCache.size() > 1) {
      return new PointerIterator(iicp, fs);
    }
    return new LeafPointerIterator(iicp, fs);
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
   * all of its subtypes
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
    final private IndexIteratorCachePair iicp;

    // An array of ComparableIntPointerIterators, one for each subtype.
    //   Each instance of these has a Class.this kind of ref to a particular variety of FSLeafIndex (bag, set, sorted) corresponding to 1 type
    //   This array has the indexes for all the subtypes
    protected ComparableIntPointerIterator[] indexes;

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

    protected IndexIteratorCachePair getIicp() {
      return iicp;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder(this.getClass().getSimpleName() + " [iicp=" + iicp + ", indexes=\n");
      int i = 0;
      for (ComparableIntPointerIterator item : indexes) {
        sb.append("  ").append(i++).append("  ").append(item).append('\n');
      }
      sb.append("  lastValidIndex="
          + lastValidIndex + ", wentForward=" + wentForward + ", iteratorComparator=" + iteratorComparator + "]");
      return sb.toString();
    }

    private ComparableIntPointerIterator[] initPointerIterator() {
      // Make sure the iterator cache exists.
      final ArrayList<FSLeafIndexImpl<?>> iteratorCache = iicp.iteratorCache;
      
      final ComparableIntPointerIterator[] pia = new ComparableIntPointerIterator[iteratorCache.size()];
           
      for (int i = 0; i < pia.length; i++) {
        final FSLeafIndexImpl<?> leafIndex = iteratorCache.get(i);
        pia[i] = leafIndex.pointerIterator(
            this.iteratorComparator,
            FSIndexRepositoryImpl.this.detectIllegalIndexUpdates,
            ((TypeImpl) leafIndex.getType()).getCode());
      }
      return pia;
    }

    private PointerIterator(final IndexIteratorCachePair iicp) {
      // next 3 are final so aren't done in the common init
      this.iicp = iicp;
      this.iteratorComparator = iicp.iteratorCache.get(0);
      this.indexes = initPointerIterator();
      moveToFirst();
    }

    private PointerIterator(final IndexIteratorCachePair iicp, int fs) {
      // next 3 are final so aren't done in the common init
      this.iicp = iicp;
      this.iteratorComparator = iicp.iteratorCache.get(0);
      this.indexes = initPointerIterator();
      moveTo(fs);
    }

    public boolean isValid() {
      // We're valid as long as at least one index is.
      return (this.lastValidIndex >= 0 );
    }

    protected ComparableIntPointerIterator checkConcurrentModification(int i) {
      final ComparableIntPointerIterator cipi = this.indexes[i];
      if (cipi.isConcurrentModification()) {
        throw new ConcurrentModificationException();
      }
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
      int nidx;

      while (idx > SORTED_SECTION) {
        nidx = (idx + SORTED_SECTION - 1) >> 1;
        if (!is_before(it, this.indexes[nidx], dir)) {
          this.indexes[idx] = it;
          return;
        }
        this.indexes[idx] = this.indexes[nidx];
        idx = nidx;
      }

      while (idx > 0) {
        nidx = idx - 1;
        if (!is_before(it, this.indexes[nidx], dir)) {
          this.indexes[idx] = it;
          return;
        }
        this.indexes[idx] = this.indexes[nidx];
        idx = nidx;
      }

      this.indexes[idx] = it;
    }

    /**
     * Move the top element down in the heap until it finds its proper position.
     * 
     * @param it
     *          indexes[0]
     * @param dir
     *          Direction of iterator movement, 1 for forward, -1 for backward
     */
    private void heapify_down(ComparableIntPointerIterator it, int dir) {
      if (!it.isValid()) {
        final ComparableIntPointerIterator itl = checkConcurrentModification(this.lastValidIndex);
        this.indexes[this.lastValidIndex] = it;
        this.indexes[0] = itl;
        --this.lastValidIndex;
        it = itl;
      }

      final int num = this.lastValidIndex;
      if ((num < 1) || !is_before(checkConcurrentModification(1), it, dir)) {
        return;
      }

      int idx = 1;
      this.indexes[0] = this.indexes[1];
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

          this.indexes[idx] = this.indexes[nidx];
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

          if (!is_before(this.indexes[nidx], it, dir)) {
            return;
          }

          this.indexes[idx] = this.indexes[nidx];
          idx = nidx;
          nidx = (nidx << 1) - (SORTED_SECTION - 1);
        }
      } finally {
        this.indexes[idx] = it;
      }
    }

    public void moveToFirst() {
      int lvi = this.indexes.length - 1;
      // Need to consider all iterators.
      // Set all iterators to insertion point.
      int i = 0;
      while (i <= lvi) {
        final ComparableIntPointerIterator it = this.indexes[i];
        it.resetConcurrentModification();
        it.moveToFirst();
        if (it.isValid()) {
          heapify_up(it, i, 1);
          ++i;
        } else {
          // swap this iterator with the last possibly valid one
          // lvi might be equal to i, this will not be a problem
          this.indexes[i] = this.indexes[lvi];
          this.indexes[lvi] = it;
          --lvi;
        }
      }
      // configured to continue with forward iterations
      this.wentForward = true;
      this.lastValidIndex = lvi;
    }

    public void moveToLast() {
      int lvi = this.indexes.length - 1;
      // Need to consider all iterators.
      // Set all iterators to insertion point.
      int i = 0;
      while (i <= lvi) {
        final ComparableIntPointerIterator it = this.indexes[i];
        it.resetConcurrentModification();
        it.moveToLast();
        if (it.isValid()) {
          heapify_up(it, i, -1);
          ++i;
        } else {
          // swap this iterator with the last possibly valid one
          // lvi might be equal to i, this will not be a problem
          this.indexes[i] = this.indexes[lvi];
          this.indexes[lvi] = it;
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
        int lvi = this.indexes.length - 1;
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
            this.indexes[i] = this.indexes[lvi];
            this.indexes[lvi] = it;
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
        int lvi = this.indexes.length - 1;
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
            this.indexes[i] = this.indexes[lvi];
            this.indexes[lvi] = it;
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
        return new PointerIterator(this.iicp, this.get());
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
      int lvi = this.indexes.length - 1;
      // Need to consider all iterators.
      // Set all iterators to insertion point.
      int i = 0;
      while (i <= lvi) {
        final ComparableIntPointerIterator it = this.indexes[i];
        it.resetConcurrentModification();
        it.moveTo(fs);
        if (it.isValid()) {
          heapify_up(it, i, 1);
          ++i;
        } else {
          // swap this iterator with the last possibly valid one
          // lvi might be equal to i, this will not be a problem
          this.indexes[i] = this.indexes[lvi];
          this.indexes[lvi] = it;
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
      return this.iicp.index;
    }

  }  // end of class PointerIterator
  
  /**
   * Version of pointer iterator for unordered uses (bags and getAllIndexedFSs
   * Since bags have no order, simplify the iteration by just going thru sequentially
   * all the subtypes
   *
   */
  private class PointerIteratorUnordered extends PointerIterator {
    
    private PointerIteratorUnordered(final IndexIteratorCachePair iicp) {
      super(iicp);
    }
    
    private PointerIteratorUnordered(final IndexIteratorCachePair iicp, int fs) {
      super(iicp); 
      moveTo(fs);
    }

    /* (non-Javadoc)
     * @see org.apache.uima.cas.impl.FSIndexRepositoryImpl.PointerIterator#isValid()
     */
    @Override
    public boolean isValid() {
      return (lastValidIndex >= 0) && (lastValidIndex < indexes.length);
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
      for (int i = 0; i < indexes.length; i++) {
        ComparableIntPointerIterator it = indexes[i];
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
      for (int i = indexes.length -1; i >= 0; i--) {
        ComparableIntPointerIterator it = indexes[i];
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
        if (lastValidIndex == indexes.length) {
          return; // all subsequent indices are invalid
        }
        it = indexes[lastValidIndex];
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
        it = indexes[lastValidIndex];
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
     */
    @Override
    public void moveTo(int fs) {
      IndexIteratorCachePair iicp = getIicp();
      int kind = iicp.index.getIndexingStrategy();
      for (int i = 0; i < indexes.length; i++) {
        if (kind == FSIndex.SORTED_INDEX) {
          FSIntArrayIndex sortedIndex = (FSIntArrayIndex) iicp.iteratorCache.get(i);
          if (sortedIndex.findEq(fs) < 0) {
            continue;  // 
          }
        }
        // if sorted index, fs is in this leaf index
        ComparableIntPointerIterator li = indexes[i];
        li.moveTo(fs); 
        if (li.isValid()) {
          lastValidIndex = i;
          li.resetConcurrentModification();
          return;
        }
      }      
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
  private class LeafPointerIterator implements IntPointerIterator, LowLevelIterator {

    // The IICP
    final private IndexIteratorCachePair iicp;

    // The underlying iterator
    final private ComparableIntPointerIterator index;

    
    @Override
    public String toString() {
      return "LeafPointerIterator [iicp=" + iicp + ", index=" + index + "]";
    }

    private LeafPointerIterator(final IndexIteratorCachePair iicp) {
      this.iicp = iicp;
      // Make sure the iterator cache exists.
      final ArrayList<FSLeafIndexImpl<?>> iteratorCache = iicp.iteratorCache;
      final FSLeafIndexImpl<?> leafIndex = iteratorCache.get(0);
      this.index = leafIndex.pointerIterator(leafIndex,
          FSIndexRepositoryImpl.this.detectIllegalIndexUpdates,
          ((TypeImpl) leafIndex.getType()).getCode());

      moveToFirst();
    }

    private LeafPointerIterator(IndexIteratorCachePair iicp, int fs) {
      this(iicp);
      moveTo(fs);
    }

    private ComparableIntPointerIterator checkConcurrentModification() {
      if (this.index.isConcurrentModification()) {
        throw new ConcurrentModificationException();
      }
      return this.index;
    }

    public boolean isValid() {
      return this.index.isValid();
    }

    public void moveToLast() {
      this.index.resetConcurrentModification();
      this.index.moveToLast();
    }

    public void moveToFirst() {
      this.index.resetConcurrentModification();
      this.index.moveToFirst();
    }

    public void moveToNext() {
      checkConcurrentModification().inc();
    }

    public void moveToPrevious() {
      checkConcurrentModification().dec();
    }

    public int get() throws NoSuchElementException {
      return ll_get();
    }

    public int ll_get() {
      if (!isValid()) {
        throw new NoSuchElementException();
      }
      return checkConcurrentModification().get();
    }

    public Object copy() {
      // If this.isValid(), return a copy pointing to the same element.
      if (this.isValid()) {
        return new LeafPointerIterator(this.iicp, this.get());
      }
      // Else, create a copy that is also not valid.
      final LeafPointerIterator pi = new LeafPointerIterator(this.iicp);
      pi.moveToFirst();
      pi.moveToPrevious();
      return pi;
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#moveTo(int)
     */
    public void moveTo(int fs) {
      this.index.resetConcurrentModification();
      this.index.moveTo(fs);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.cas.impl.LowLevelIterator#moveToNext()
     */
    public void inc() {
      checkConcurrentModification().inc();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.cas.impl.LowLevelIterator#moveToPrevious()
     */
    public void dec() {
      checkConcurrentModification().dec();
    }

    public int ll_indexSize() {
      return this.iicp.size();
    }

    public LowLevelIndex ll_getIndex() {
      return this.iicp.index;
    }

  }  // end of LeafPointerIterator

  /**
   * This implementation creates a pseudo index that is
   * flattened and 
   * copied (so it is a snapshot), and
   * returns an iterator over that 
   * 
   */
  private class SnapshotPointerIterator implements IntPointerIterator, LowLevelIterator {

    final private FSIntArrayIndex sortedLeafIndex;
    final private int[] snapshot;
    private int pos = 0;
      
    @Override
    public String toString() {
      return "SnapshotPointerIterator[size: " + snapshot.length + ", position: " + pos + "]";
    }
    
    private SnapshotPointerIterator(IndexIteratorCachePair iicp0) {
      this(iicp0, false);
    }
    
    private SnapshotPointerIterator(IndexIteratorCachePair iicp0, boolean isRootOnly) {
      FSLeafIndexImpl leafIndex = iicp0.index;
      FSIndexComparator comp = leafIndex.getComparator();
      
      final int size = iicp0.size();
      sortedLeafIndex = (FSIntArrayIndex) addNewIndexCore(comp, size, FSIndex.SORTED_INDEX);
      snapshot = sortedLeafIndex.getVector().getArray();
      flattenCopy(iicp0, isRootOnly);
      sortedLeafIndex.getVector().setSize(size);
      moveToFirst();      
    }

    private SnapshotPointerIterator(IndexIteratorCachePair iicp0, int fs) {
      this(iicp0);
      moveTo(fs);
    }
    
    private void flattenCopy(IndexIteratorCachePair iicp0, boolean isRootOnly) {
    
      
//      if (iicp0.iteratorCache.size() > 1) {
//        if (indexKind == FSIndex.BAG_INDEX) {
//          // have a set of bag indexes, just copy them into the snapshot in bulk
//        } else { 
//          // for sorted or set, extract the elements 
//          
//        }
      LowLevelIterator it = (LowLevelIterator) 
          (isRootOnly ?
              new LeafPointerIterator(iicp0) :
              createPointerIterator(iicp0));
      int i = 0;
      while (it.isValid()) {
        snapshot[i++] = it.ll_get();
        it.moveToNext();
      }
    }
       
    public boolean isValid() {
      return (0 <= pos) && (pos < snapshot.length);
    }
    
    public void moveToLast() {
      pos = snapshot.length - 1;
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
      if (sortedLeafIndex.getComparator().getNumberOfKeys() == 0) {
        // use identity, search from beginning to get "left-most"
        int i = 0;
        for (; i < snapshot.length; i++) {
          if (fs == snapshot[i]) {
            break;
          }
        }
        pos = i;
      } else {
        int position = sortedLeafIndex.findLeftmost(fs);
        if (position >= 0) {
          pos = position;
        } else {
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
      return snapshot.length;
    }
    
    public Object copy() {
      // If this.isValid(), return a copy pointing to the same element.
      IndexIteratorCachePair iicp = new IndexIteratorCachePair();
      iicp.index = sortedLeafIndex;
      if (this.isValid()) {
        return new SnapshotPointerIterator(iicp, this.get());
      }
      // Else, create a copy that is also not valid.
      SnapshotPointerIterator pi = new SnapshotPointerIterator(iicp);
      pi.pos = -1;
      return pi;
    }

    public LowLevelIndex ll_getIndex() {
      return sortedLeafIndex;
    }

  }  // end of SnapshotPointerIterator

  /**
   * Implementation of a particular index for a particular Type (and its subtypes)
   *
   * @param <T> - the particular type (and it subtypes) this particular index is associated with
   */
  private class IndexImpl<T extends FeatureStructure> implements FSIndex<T>, FSIndexImpl {

    private final IndexIteratorCachePair iicp;
    
    private final boolean is_with_snapshot_iterators;

    private final boolean is_unordered; //Set for getAllIndexedFSs

    private IndexImpl(IndexIteratorCachePair iicp) {
      this.iicp = iicp;
      is_with_snapshot_iterators = false;
      is_unordered = false;
    }
    
    private IndexImpl(IndexIteratorCachePair iicp, IteratorExtraFunction extraFn) {
      this.iicp = iicp;
      is_with_snapshot_iterators = (extraFn == IteratorExtraFunction.SNAPSHOT);
      is_unordered = (extraFn == IteratorExtraFunction.UNORDERED);
    }    

    public int ll_compare(int ref1, int ref2) {
      return this.iicp.index.ll_compare(ref1, ref2);
    }

    public int getIndexingStrategy() {
      return this.iicp.index.getIndexingStrategy();
    }

    public FSIndexComparator getComparator() {
      return this.iicp.index.getComparator();
    }

    protected IntComparator getIntComparator() {
      return this.iicp.index.getIntComparator();
    }

    public void flush() {
      this.iicp.index.flush();
    }

    /**
     * @see org.apache.uima.cas.FSIndex#compare(FeatureStructure, FeatureStructure)
     */
    public int compare(FeatureStructure fs1, FeatureStructure fs2) {
      return this.iicp.index.compare(fs1, fs2);
    }

    /**
     * @see org.apache.uima.cas.FSIndex#contains(FeatureStructure)
     */
    public boolean contains(FeatureStructure fs) {
      return this.iicp.index.contains(fs);
    }

    public FeatureStructure find(FeatureStructure fs) {
      return this.iicp.index.find(fs);
    }

    /**
     * @see org.apache.uima.cas.FSIndex#getType()
     */
    public Type getType() {
      return this.iicp.index.getType();
    }

    /**
     * @see org.apache.uima.cas.FSIndex#iterator()
     */
    public FSIterator<T> iterator() {
      return new FSIteratorWrapper<T>(
          is_with_snapshot_iterators ?
             new SnapshotPointerIterator(iicp) :               
             createPointerIterator(this.iicp, is_unordered), 
          FSIndexRepositoryImpl.this.cas);
    }

    /**
     * @see org.apache.uima.cas.FSIndex#iterator(FeatureStructure)
     */
    public FSIterator<T> iterator(FeatureStructure fs) {
      final int fsAddr = ((FeatureStructureImpl) fs).getAddress();
      return new FSIteratorWrapper<T>(
          is_with_snapshot_iterators ?
              new SnapshotPointerIterator(iicp, fsAddr) :
              createPointerIterator(this.iicp, fsAddr),
          FSIndexRepositoryImpl.this.cas);
    }

    public IntPointerIterator getIntIterator() {
      return createPointerIterator(this.iicp);
    }

    /**
     * @see org.apache.uima.cas.FSIndex#size()
     */
    public int size() {
      this.iicp.createIndexIteratorCache();
      // int size = this.iicp.index.size();
      int size = 0;
      final ArrayList<FSLeafIndexImpl<?>> subIndex = this.iicp.iteratorCache;
      final int max = subIndex.size();
      for (int i = 0; i < max; i++) {
        size += subIndex.get(i).size();
      }
      return size;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.cas.impl.LowLevelIndex#ll_iterator()
     */
    public LowLevelIterator ll_iterator() {
      return (LowLevelIterator) 
          (is_with_snapshot_iterators ?
              new SnapshotPointerIterator(iicp) :
              createPointerIterator(this.iicp));
    }

    public LowLevelIterator ll_rootIterator() {
      this.iicp.createIndexIteratorCache();
      return is_with_snapshot_iterators ?
          new SnapshotPointerIterator(iicp, true) : 
          new LeafPointerIterator(this.iicp);
    }

    public LowLevelIterator ll_iterator(boolean ambiguous) {
      if (ambiguous) {
        return this.ll_iterator();
      }
      return new LLUnambiguousIteratorImpl(this.ll_iterator(), this.iicp.index.lowLevelCAS);
    }

    @Override
    public FSIndex withSnapshotIterators() {
      return new IndexImpl(this.iicp, IteratorExtraFunction.SNAPSHOT);
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
     * optimization only - bypasses some shared (among views) initialization if already done
     */
    private boolean isSetUpFromBaseCAS = false;
    
    SharedIndexInfo(TypeSystemImpl typeSystem) {
      this.tsi = typeSystem;
    }
  }
  
  /*****  I N S T A N C E   V A R I A B L E S  *****/
  /*****           Replicated per view         *****/                 

  // A reference to the CAS View.
  private final CASImpl cas;

  // Is the index repository locked?
  private boolean locked = false;

  // An array of ArrayLists, one for each type in the type hierarchy.
  // The ArrayLists are unordered lists of IndexIteratorCachePairs for
  // that type, corresponding to the different index definitions over that type
  final private ArrayList<IndexIteratorCachePair>[] indexArray;

  // an array of ints, one for each type in the type hierarchy.
  // Used to enable iterators to detect modifications (adds / removes)
  // to indexes they're iterating over while they're iterating over them.
  // not private so it can be seen by FSLeafIndexImpl
  final int[] detectIllegalIndexUpdates;

  // A map from names to IndexIteratorCachePairs. Different names may map to
  // the same index.  
  // The keys are the same across all views, but the values are different, per view
  final private HashMap<String, IndexIteratorCachePair> name2indexMap;
  
  // the next are for journaling updates to indexes
  final private IntVector indexUpdates;

  final private BitSet indexUpdateOperation;

  private boolean logProcessed;

  private IntSet fsAddedToIndex;

  private IntSet fsDeletedFromIndex;

  private IntSet fsReindexed;

  // Monitor indexes used to optimize getIndexedFS and flush
  // only used for faster access to next set bit
  final private IntVector usedIndexes;

  // one bit per typeCode, indexed by typeCode
  final private boolean[] isUsed;
  
  private final SharedIndexInfo sii;
  
  @SuppressWarnings("unused")
  private FSIndexRepositoryImpl() {
    this.cas = null;  // because it's final
    this.sii = null;
    this.name2indexMap = null;
    this.indexArray = null;
    this.detectIllegalIndexUpdates = null;
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
  @SuppressWarnings("unchecked")
  FSIndexRepositoryImpl(CASImpl cas) {
    this.cas = cas;
    this.sii = new SharedIndexInfo(cas.getTypeSystemImpl());

    final TypeSystemImpl ts = this.sii.tsi;
    // Type counting starts at 1.
    final int numTypes = ts.getNumberOfTypes() + 1;
    this.detectIllegalIndexUpdates = new int[numTypes];
    
    this.name2indexMap = new HashMap<String, IndexIteratorCachePair>();
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
  @SuppressWarnings("unchecked")
  FSIndexRepositoryImpl(CASImpl cas, FSIndexRepositoryImpl baseIndexRepo) {

    this.cas = cas;
    this.sii = baseIndexRepo.sii;
    sii.isSetUpFromBaseCAS = true;  // bypasses initialization already done

    final TypeSystemImpl ts = this.sii.tsi;
    // Type counting starts at 1.
    final int numTypes = ts.getNumberOfTypes() + 1;
    this.detectIllegalIndexUpdates = new int[numTypes];
    
    this.name2indexMap = new HashMap<String, IndexIteratorCachePair>();
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
        final IndexIteratorCachePair iicp = baseIndexRepo.name2indexMap.get(key);
        createIndexNoQuestionsAsked(iicp.index.getComparator(), key,
            iicp.index.getIndexingStrategy());
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
      this.indexArray[i] = new ArrayList<IndexIteratorCachePair>();
    }
    for (int i = 0; i < this.detectIllegalIndexUpdates.length; i++) {
      this.detectIllegalIndexUpdates[i] = Integer.MIN_VALUE;
    }
    this.fsAddedToIndex = new IntSet();
    this.fsDeletedFromIndex = new IntSet();
    this.fsReindexed = new IntSet();
  }

  /**
   * Reset all indexes.
   */
  public void flush() {
    if (!this.locked) {
      return;
    }
    int max;
    ArrayList<IndexIteratorCachePair> v;

    // Do nothing really fast!
    if (this.usedIndexes.size() == 0) {
      return;
    }

    for (int i = 0; i < this.usedIndexes.size(); i++) {
      this.isUsed[this.usedIndexes.get(i)] = false;
      v = this.indexArray[this.usedIndexes.get(i)];
      max = v.size();
      for (int j = 0; j < max; j++) {
        v.get(j).index.flush();
      }
    }
    this.indexUpdates.removeAllElements();
    this.indexUpdateOperation.clear();
    this.fsAddedToIndex = new IntSet();
    this.fsDeletedFromIndex = new IntSet();
    this.fsReindexed = new IntSet();
    this.logProcessed = false;
    this.usedIndexes.removeAllElements();
  }

  public void addFS(int fsRef) {
    ll_addFS(fsRef);
  }

  private IndexIteratorCachePair addNewIndex(FSIndexComparator comparator, int indexType) {
    return addNewIndex(comparator, DEFAULT_INDEX_SIZE, indexType);
  }

  /**
   * This is where the actual index gets created.
   */
  private IndexIteratorCachePair addNewIndex(final FSIndexComparator comparator, int initialSize,
      int indexType) {
    
    IndexIteratorCachePair iicp = new IndexIteratorCachePair(); 
    iicp.index = addNewIndexCore(comparator, initialSize, indexType);
    final Type type = comparator.getType();
    final int typeCode = ((TypeImpl) type).getCode();
    this.indexArray[typeCode].add(iicp);
    return iicp;
  }
  
  private FSLeafIndexImpl<?> addNewIndexCore(final FSIndexComparator comparator, int initialSize,
      int indexType) {
    final Type type = comparator.getType();
    // final int vecLen = indexVector.size();
    FSLeafIndexImpl<?> ind;
    switch (indexType) {
    case FSIndex.SET_INDEX: {
      ind = new FSRBTSetIndex(this.cas, type, indexType);
      break;
    }
    case FSIndex.BAG_INDEX:
    case FSIndex.DEFAULT_BAG_INDEX: {
      ind = new FSBagIndex(this.cas, type, initialSize, indexType);
      break;
    }
    default: {
      // SORTED_INDEX is the default. We don't throw any errors, if the
      // code
      // is unknown, we just create a sorted index (with duplicates).
      // ind = new FSRBTIndex(this.cas, type, FSIndex.SORTED_INDEX);
      ind = new FSIntArrayIndex(this.cas, type, initialSize, FSIndex.SORTED_INDEX);
      break;
    }
    }
    // ind = new FSRBTIndex(this.cas, type);
    // ind = new FSVectorIndex(this.cas, initialSize);
    ind.init(comparator);
    return ind;
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
  private IndexIteratorCachePair addNewIndexRecursive(FSIndexComparator comparator, int indexType) {
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
  private static final int findIndex(ArrayList<IndexIteratorCachePair> indexes,
      FSIndexComparator comp,
      int indexType) {
    FSIndexComparator indexComp;
    final int max = indexes.size();
    for (int i = 0; i < max; i++) {
      FSLeafIndexImpl index = indexes.get(i).index;
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
  private IndexIteratorCachePair addNewIndexRec(FSIndexComparator comparator, int indexType) {
    final IndexIteratorCachePair iicp = this.addNewIndex(comparator, indexType);
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
    final List<Type> sub = ts.getDirectSubtypes(t);
    final int len = sub.size();
    for (int i = 0; i < len; i++) {
      addAllSubsumedTypes(sub.get(i), ts, v);
    }
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
    IndexIteratorCachePair cp = this.name2indexMap.get(label);
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
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#getIndexes()
   */
  public Iterator<FSIndex<FeatureStructure>> getIndexes() {
    final ArrayList<FSIndex<FeatureStructure>> indexList = new ArrayList<FSIndex<FeatureStructure>>();
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
      if (this.name2indexMap.get(label).index.getComparator().equals(comp)) {
        labels.add(label);
      }
    }
    return labels.iterator();
  }

  /**
   * @see org.apache.uima.cas.FSIndexRepository#getIndex(String, Type)
   */
  public FSIndex<FeatureStructure> getIndex(String label, Type type) {
    final IndexIteratorCachePair iicp = this.name2indexMap.get(label);
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
    final Type indexType = iicp.index.getType();
    if (!this.sii.tsi.subsumes(indexType, type)) {
      final CASRuntimeException cre = new CASRuntimeException(
          CASRuntimeException.TYPE_NOT_IN_INDEX, new String[] { label, type.getName(),
              indexType.getName() });
      throw cre;
    }
    final int typeCode = ((TypeImpl) type).getCode();
    final ArrayList<IndexIteratorCachePair> inds = this.indexArray[typeCode];
    // Since we found an index for the correct type, find() must return a
    // valid result -- unless this is a special auto-index.
    final int indexCode = findIndex(inds, iicp.index.getComparator(), iicp.index.getIndexingStrategy());
    if (indexCode < 0) {
      return null;
    }
    // assert((indexCode >= 0) && (indexCode < inds.size()));
    return new IndexImpl<FeatureStructure>(inds.get(indexCode));
    // return ((IndexIteratorCachePair)inds.get(indexCode)).index;
  }
  

  /**
   * @see org.apache.uima.cas.FSIndexRepository#getIndex(String)
   */
  public FSIndex<FeatureStructure> getIndex(String label) {
    final IndexIteratorCachePair iicp = this.name2indexMap.get(label);
    if (iicp == null) {
      return null;
    }
    return new IndexImpl<FeatureStructure>(iicp);
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
    final ArrayList<IndexIteratorCachePair> indexVector = this.indexArray[typeCode];
    if (indexVector.size() == 0) {
      // No index for this type exists.
      return 0;
    }
    int numFSs = indexVector.get(0).index.size();
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
    final ArrayList<IndexIteratorCachePair> allIndexesForType = this.indexArray[typeCode];
    for (IndexIteratorCachePair iicp : allIndexesForType) {
      iicp.index.flush();
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
   * Note that duplicate entries are removed, and the results are sorted by FS address.
   * 
   * The order in which FSs occur in the array does not reflect the order in which they
   * were added to the repository. This means that set indexes deserialized from this list may
   * contain different but equal elements than the original index.
   * @return an array containing all FSs in any defined index, in this view.
   */
  public int[] getIndexedFSs() {
    final IntVector v = new IntVector();
    IndexIteratorCachePair iicp;
    IntPointerIterator it;
    ArrayList<IndexIteratorCachePair> iv;
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
      
      IndexIteratorCachePair anIndex = null;
      for (int j = 0; j < jMax; j++) {
        iicp = iv.get(j);
        indStrat = iicp.index.getIndexingStrategy();
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
        it = anIndex.index.refIterator();
        while (it.isValid()) {
          indexedFSs.add(it.get());
          it.inc();
        }
        // sort and remove duplicates
        indexedFSs.sortDedup();
        // add to previously collected types
        v.add(indexedFSs.getArray(), 0, indexedFSs.size());  // bulk add of all elements
      } else {
        anIndex.index.bulkAddTo(v);
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
    this.detectIllegalIndexUpdates[typeCode]++;
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
    final ArrayList<IndexIteratorCachePair> indexes = this.indexArray[typeCode];
    // Add fsRef to all indexes.
    boolean noIndexOrOnlySetindexes = true;
    for (IndexIteratorCachePair iicp : indexes) {
      final int indexingStrategy = iicp.index.getIndexingStrategy(); 
      if (isAddback) {
        if (indexingStrategy == FSIndex.BAG_INDEX) {
        continue;  // skip adding back to bags - because removes are skipped for bags
        }
        iicp.index.insert(fsRef, count);
      } else {
        iicp.index.insert(fsRef);  // if not addback, only insert 1
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
      indexes.get(indexes.size() - 1).index.insert(fsRef);
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
    final ArrayList<IndexIteratorCachePair> idxList = this.indexArray[typeCode];
    final int max = idxList.size();
    boolean atLeastOneRemoved = false;
    for (int i = 0; i < max; i++) {
      atLeastOneRemoved |= idxList.get(i).index.remove(fsRef);
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
    return (iteratorList.size() == 1) ? iteratorList.get(0) : 
        new LowLevelIteratorAggregate(iteratorList);
  }
  
  private final void ll_getAllIndexedFS(Type type, List<LowLevelIterator> iteratorList) {
    // Start by looking for an auto-index, because its existence implies no set or bag index for that type 
    final LowLevelIndex autoIndex = ll_getIndex(getAutoIndexNameForType(type));
    if (autoIndex != null) {
      iteratorList.add(autoIndex.ll_iterator());
      // We found one of the special auto-indexes which don't inherit down the tree. So, we
      // manually need to traverse the inheritance tree to look for more indexes. Note that
      // this is not necessary when we have a regular index
      final List<Type> subtypes = this.sii.tsi.getDirectSubtypes(type);
      for (int i = 0; i < subtypes.size(); i++) {
        ll_getAllIndexedFS(subtypes.get(i), iteratorList);
      }
      return;
    }
    // use the first non-set index found; this is
    // guaranteed to exist https://issues.apache.org/jira/browse/UIMA-4111
    
    // iterate over all defined indexes for this type
    
    ArrayList<IndexIteratorCachePair> iicps = this.indexArray[((TypeImpl)type).getCode()];
    for (IndexIteratorCachePair iicp : iicps) {
      if (iicp.index.getIndexingStrategy() != FSIndex.SET_INDEX) {
        // return an iterator that covers this type and all its subtypes
        iteratorList.add(new IndexImpl<FeatureStructure>(iicp, IteratorExtraFunction.UNORDERED).ll_iterator());
        return;
      }
    }
    
    // No index for this type was found at all. 
    // Example:  You ask for an iterator over "TOP", but no instances of TOP are created,
    //   and no index over TOP was ever created.
    // Since the auto-indexes are created on demand for
    //   each type, there may be gaps in the inheritance chain. So keep descending the inheritance
    //   tree looking for relevant indexes.
    final List<Type> subtypes = this.sii.tsi.getDirectSubtypes(type);
    for (Type t : subtypes) {
      ll_getAllIndexedFS(t, iteratorList);
    }
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndexRepository#getAllIndexedFS(org.apache.uima.cas.Type)
   */
  public FSIterator<FeatureStructure> getAllIndexedFS(Type type) {
    final List<FSIterator<FeatureStructure>> iteratorList = new ArrayList<FSIterator<FeatureStructure>>();
    getAllIndexedFS(type, iteratorList);
    return (iteratorList.size() == 1) ? iteratorList.get(0) : 
        new FSIteratorAggregate<FeatureStructure>(iteratorList);
  }

  private final void getAllIndexedFS(Type type, List<FSIterator<FeatureStructure>> iteratorList) {
    // Start by looking for an auto-index. 
    final FSIndex<FeatureStructure> autoIndex = getIndex(getAutoIndexNameForType(type));
    if (autoIndex != null) {
      iteratorList.add(autoIndex.iterator());
      // We found one of the special auto-indexes which don't inherit down the tree. So, we
      // manually need to traverse the inheritance tree to look for more indexes. Note that
      // this is not necessary when we have a regular index
      final List<Type> subtypes = this.sii.tsi.getDirectSubtypes(type);
      for (int i = 0; i < subtypes.size(); i++) {
        getAllIndexedFS(subtypes.get(i), iteratorList);
      }
      return;
    }
    // use the first non-set index found; this is
    // guaranteed to exist https://issues.apache.org/jira/browse/UIMA-4111
    
    // iterate over all defined indexes for this type
    
    ArrayList<IndexIteratorCachePair> iicps = this.indexArray[((TypeImpl)type).getCode()];
    for (IndexIteratorCachePair iicp : iicps) {
      if (iicp.index.getIndexingStrategy() != FSIndex.SET_INDEX) {
        // return an iterator that covers this type and all its subtypes
        iteratorList.add(new IndexImpl<FeatureStructure>(iicp, IteratorExtraFunction.UNORDERED).iterator());
        return;
      }
    }
    
    // No index for this type was found at all. 
    // Example:  You ask for an iterator over "TOP", but no instances of TOP are created,
    //   and no index over TOP was ever created.
    // Since the auto-indexes are created on demand for
    //   each type, there may be gaps in the inheritance chain. So keep descending the inheritance
    //   tree looking for relevant indexes.
    final List<Type> subtypes = this.sii.tsi.getDirectSubtypes(type);
    for (Type t : subtypes) {
      getAllIndexedFS(t, iteratorList);
    }
    
//    
//    
//    final Iterator<String> iter = getLabels();
//    while (iter.hasNext()) {
//      final String label = iter.next();
//      final FSIndex index = getIndex(label);
//      if (this.sii.tsi.subsumes(index.getType(), type)) {
//        if (index.getIndexingStrategy() != FSIndex.SET_INDEX) {
//          iteratorList.add(getIndex(label, type).iterator());
//          // Done, found non-set index.
//          return;
//        }
//        setIndex = getIndex(label, type);
//      }
//    }
//    // No sorted or bag index found for this type. If there was a set index,
//    // return an iterator for it.
//    if (setIndex != null) {
//      iteratorList.add(setIndex.iterator());
//      return;
//    }
//    // No index for this type was found at all. Since the auto-indexes are created on demand for
//    // each type, there may be gaps in the inheritance chain. So keep descending the inheritance
//    // tree looking for relevant indexes.
//    final List<Type> subtypes = this.sii.tsi.getDirectSubtypes(type);
//    for (Type t : subtypes) {
//      getAllIndexedFS(t, iteratorList);
//    }
  }
  
//  boolean isFsInAnyIndex(int fsAddr) {
//    final int typeCode = cas.getTypeCode(fsAddr);
//    if (cas.getTypeSystemImpl().subsumes(superType, type))
//  }
  
  /**
   * This is used to see if a FS which has a key feature being modified
   * could corrupt an index in this view.  It returns true if found 
   * (sometimes it returns true, even if strictly speaking, there is 
   * no chance of corruption - seel below)
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
    final ArrayList<IndexIteratorCachePair> indexesForType = indexArray[typeCode];
    FSBagIndex index_bag = null;
    boolean found_in_bag = false;
    ArrayList<FSRBTSetIndex> setindexes = null;

    for (IndexIteratorCachePair iicp : indexesForType) {
      FSLeafIndexImpl<?> index_for_this_typeCode = iicp.index;
      final int kind = index_for_this_typeCode.getIndexingStrategy(); // SORTED_INDEX, BAG_, or SET_
      if (kind == FSIndex.SORTED_INDEX) {
        return ((FSIntArrayIndex<?>)index_for_this_typeCode).ll_containsEq(fsAddr);
      }
      if (kind == FSIndex.BAG_INDEX && !found_in_bag) {
        if (FSBagIndex.USE_POSITIVE_INT_SET) {
          found_in_bag = ((FSBagIndex)index_for_this_typeCode).ll_contains(fsAddr);
          if (!found_in_bag) {
            return false; 
          }
          continue; // may still return false if no set or sorted indexes
        } 
        index_bag = (FSBagIndex) index_for_this_typeCode;
      } else {  // is Set case
        if (setindexes == null) {
          setindexes = new ArrayList<FSRBTSetIndex>();
        }
        setindexes.add((FSRBTSetIndex) index_for_this_typeCode);
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
    
    for (FSRBTSetIndex index_set : setindexes) {
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

    for (IndexIteratorCachePair iicp : indexArray[typeCode]) {
      FSLeafIndexImpl<?> index_for_this_typeCode = iicp.index;
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

  /**
   * returns the annotation index for a type which is Annotation or a subtype of it.
   * @param typeCode
   * @return the index just for that type
   */
  private FSIntArrayIndex<?> getAnnotationIndexNoSubtypes(int typeCode) {
    final IndexIteratorCachePair annotation_iicp = this.name2indexMap.get(CAS.STD_ANNOTATION_INDEX);
    final ArrayList<IndexIteratorCachePair> iicps_for_type = indexArray[typeCode];
    final FSLeafIndexImpl<?> ri = annotation_iicp.index;
    // search all defined indexes for this type, to find an annotation one
    final int ii = findIndex(iicps_for_type, ri.getComparator(), FSIndex.SORTED_INDEX);
    return ((FSIntArrayIndex<?>)iicps_for_type.get(ii).index);
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
    for (int i = 0; i < this.indexUpdates.size(); i++) {
      final int fsRef = this.indexUpdates.get(i);
      final boolean added = this.indexUpdateOperation.get(i);
      if (added) {
        final int indexOfDeletedItem = this.fsDeletedFromIndex.indexOf(fsRef);
        if (indexOfDeletedItem >= 0) {
          this.fsDeletedFromIndex.removeElementAt(indexOfDeletedItem);
          this.fsReindexed.add(fsRef);
        } else if (this.fsReindexed.contains(fsRef)) {
          continue;  // skip adding this to anything
        } else {
          this.fsAddedToIndex.add(fsRef);  // this is a set, so dups not added
        }
      } else {
        final int indexOfaddedItem = this.fsAddedToIndex.indexOf(fsRef);
        if (indexOfaddedItem >= 0) {
          this.fsAddedToIndex.removeElementAt(indexOfaddedItem);
        } else {
          final int indexOfReindexedItem = this.fsReindexed.indexOf(fsRef);
          if (indexOfReindexedItem >= 0) {
            this.fsReindexed.removeElementAt(indexOfReindexedItem);
            this.fsDeletedFromIndex.add(fsRef);
          }
          else {
            this.fsDeletedFromIndex.add(fsRef);
          }
        }
      }
    }
    this.logProcessed = true;
    this.indexUpdates.removeAllElements();
    this.indexUpdateOperation.clear();
  }

  public int[] getAddedFSs() {
    if (!this.logProcessed) {
      processIndexUpdates();
    }
    final int[] fslist = new int[this.fsAddedToIndex.size()];
    for (int i = 0; i < fslist.length; i++) {
      fslist[i] = this.fsAddedToIndex.get(i);
    }
    return fslist;
  }

  public int[] getDeletedFSs() {
    if (!this.logProcessed) {
      processIndexUpdates();
    }
    final int[] fslist = new int[this.fsDeletedFromIndex.size()];
    for (int i = 0; i < fslist.length; i++) {
      fslist[i] = this.fsDeletedFromIndex.get(i);
    }
    return fslist;
  }

  public int[] getReindexedFSs() {
    if (!this.logProcessed) {
      processIndexUpdates();
    }
    final int[] fslist = new int[this.fsReindexed.size()];
    for (int i = 0; i < fslist.length; i++) {
      fslist[i] = this.fsReindexed.get(i);
    }
    return fslist;
  }

  public boolean isModified() {
    if (!this.logProcessed) {
      processIndexUpdates();
    }
    return ((this.fsAddedToIndex.size() > 0) || (this.fsDeletedFromIndex.size() > 0) || (this.fsReindexed
        .size() > 0));
  }

  @Override
  public String toString() {
    return "FSIndexRepositoryImpl [" + cas + "]";
  }
}
