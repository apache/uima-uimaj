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
import org.apache.uima.internal.util.SortedIntSet;

public class FSIndexRepositoryImpl implements FSIndexRepositoryMgr, LowLevelIndexRepository {

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
   * to create an iterator for the type of the index. compareTo() is based on types and the
   * comparator of the index.
   */
  private class IndexIteratorCachePair implements Comparable<IndexIteratorCachePair> {

    // The "root" index, i.e., index of the type of the iterator.
    private FSLeafIndexImpl<?> index = null;

    // A list of indexes (the sub-indexes that we need for an
    // iterator). I.e., one index for each type that's subsumed by the
    // iterator
    // type.
    private ArrayList<FSLeafIndexImpl<?>> iteratorCache = null;

    private IndexIteratorCachePair() {
      super();
    }

    // Two IICPs are equal iff their index comparators are equal AND their
    // indexing strategy is the same.
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof IndexIteratorCachePair)) {
        return false;
      }
      final IndexIteratorCachePair iicp = (IndexIteratorCachePair) o;
      return this.index.getComparator().equals(iicp.index.getComparator())
          && (this.index.getIndexingStrategy() == iicp.index.getIndexingStrategy());
    }

    @Override
    public int hashCode() {
      throw new UnsupportedOperationException();
    }

    // Populate the cache.
    private void createIndexIteratorCache() {
      if (this.iteratorCache != null) {
        return;
      }
      this.iteratorCache = new ArrayList<FSLeafIndexImpl<?>>();
      final Type rootType = this.index.getComparator().getType();
      ArrayList<Type> allTypes = null;
      if (this.index.getIndexingStrategy() == FSIndex.DEFAULT_BAG_INDEX) {
        allTypes = new ArrayList<Type>();
        allTypes.add(rootType);
      } else {
        allTypes = getAllSubsumedTypes(rootType, FSIndexRepositoryImpl.this.typeSystem);
      }
      final int len = allTypes.size();
      int typeCode, indexPos;
      ArrayList<IndexIteratorCachePair> indexList;
      for (int i = 0; i < len; i++) {
        typeCode = ((TypeImpl) allTypes.get(i)).getCode();
        indexList = FSIndexRepositoryImpl.this.indexArray[typeCode];
        indexPos = indexList.indexOf(this);
        if (indexPos >= 0) {
          this.iteratorCache.add(indexList.get(indexPos).index);
        }
      }
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
      for (int i = 0; i < this.iteratorCache.size(); i++) {
        size += this.iteratorCache.get(i).size();
      }
      return size;
    }

  }

  IntPointerIterator createPointerIterator(IndexIteratorCachePair iicp) {
    iicp.createIndexIteratorCache();
    if (iicp.iteratorCache.size() > 1) {
      return new PointerIterator(iicp);
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
    private IndexIteratorCachePair iicp;

    // An array of integer arrays, one for each subtype.
    private ComparableIntPointerIterator[] indexes;

    int lastValidIndex;

    // snapshot to detectIllegalIndexUpdates
    // need to move this to ComparableIntPointerIterator so it can be tested

    // currentIndex is always 0

    // The iterator works in two modes:
    // Forward and backward processing. This flag tells which mode we're in.
    // The iterator heap needs to be reconstructed when we switch direction.
    private boolean wentForward;

    // Comparator that is used to compare FS addresses for the purposes of
    // iteration.
    private IntComparator iteratorComparator;

    // The next element in the iterator. When next < 0, there is no
    // next.
    // private int next;

    private PointerIterator() {
      super();
    }

    private void initPointerIterator(IndexIteratorCachePair iicp0) {
      this.iicp = iicp0;
      // Make sure the iterator cache exists.
      final ArrayList<FSLeafIndexImpl<?>> iteratorCache = iicp0.iteratorCache;
      this.indexes = new ComparableIntPointerIterator[iteratorCache.size()];
      this.iteratorComparator = iteratorCache.get(0);
      ComparableIntPointerIterator it;
      for (int i = 0; i < this.indexes.length; i++) {
        final FSLeafIndexImpl<?> leafIndex = iteratorCache.get(i);
        it = leafIndex.pointerIterator(this.iteratorComparator,
            FSIndexRepositoryImpl.this.detectIllegalIndexUpdates,
            ((TypeImpl) leafIndex.getType()).getCode());
        this.indexes[i] = it;
      }
    }

    private PointerIterator(IndexIteratorCachePair iicp) {
      super();
      initPointerIterator(iicp);
      moveToFirst();
    }

    private PointerIterator(IndexIteratorCachePair iicp, int fs) {
      super();
      initPointerIterator(iicp);
      moveTo(fs);
    }

    public boolean isValid() {
      // We're valid as long as at least one index is.
      return (this.lastValidIndex >= 0);
    }

    private ComparableIntPointerIterator checkConcurrentModification(int i) {
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
      // Go back until we find a FS that is really smaller
      while (true) {
        moveToPrevious();
        if (isValid()) {
          int prev = get();
          if (this.iicp.index.compare(prev, fs) != 0) {
            moveToNext();
            break;
          }
        } else {
          moveToFirst();
          break;
        }
      }
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

  }

  /**
   * The iterator implementation for indexes. Tricky because the iterator needs to be able to move
   * backwards as well as forwards.
   */
  private class LeafPointerIterator implements IntPointerIterator, LowLevelIterator {

    // The IICP
    private IndexIteratorCachePair iicp;

    // An array of integer arrays, one for each subtype.
    private ComparableIntPointerIterator index;

    private LeafPointerIterator() {
      super();
    }

    private void initPointerIterator(IndexIteratorCachePair iicp0) {
      this.iicp = iicp0;
      // Make sure the iterator cache exists.
      final ArrayList<FSLeafIndexImpl<?>> iteratorCache = iicp0.iteratorCache;
      final FSLeafIndexImpl<?> leafIndex = iteratorCache.get(0);
      this.index = leafIndex.pointerIterator(leafIndex,
          FSIndexRepositoryImpl.this.detectIllegalIndexUpdates,
          ((TypeImpl) leafIndex.getType()).getCode());
    }

    private LeafPointerIterator(IndexIteratorCachePair iicp) {
      super();
      initPointerIterator(iicp);
      moveToFirst();
    }

    private LeafPointerIterator(IndexIteratorCachePair iicp, int fs) {
      super();
      initPointerIterator(iicp);
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

  }

  private class IndexImpl<T extends FeatureStructure> implements FSIndex<T>, FSIndexImpl {

    private final IndexIteratorCachePair iicp;

    private IndexImpl(IndexIteratorCachePair iicp) {
      super();
      this.iicp = iicp;
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
      return new FSIteratorWrapper<T>(createPointerIterator(this.iicp),
          FSIndexRepositoryImpl.this.cas);
    }

    /**
     * @see org.apache.uima.cas.FSIndex#iterator(FeatureStructure)
     */
    public FSIterator<T> iterator(FeatureStructure fs) {
      return new FSIteratorWrapper<T>(createPointerIterator(this.iicp,
          ((FeatureStructureImpl) fs).getAddress()), FSIndexRepositoryImpl.this.cas);
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
      return (LowLevelIterator) createPointerIterator(this.iicp);
    }

    public LowLevelIterator ll_rootIterator() {
      this.iicp.createIndexIteratorCache();
      return new LeafPointerIterator(this.iicp);
    }

    public LowLevelIterator ll_iterator(boolean ambiguous) {
      if (ambiguous) {
        return this.ll_iterator();
      }
      return new LLUnambiguousIteratorImpl(this.ll_iterator(), this.iicp.index.lowLevelCAS);
    }

  }

  // private class AnnotIndexImpl
  // extends IndexImpl
  // implements AnnotationIndex, FSIndexImpl {
  //
  // private AnnotIndexImpl(IndexIteratorCachePair iicp) {
  // super(iicp);
  // }
  //
  // public FSIterator subiterator(AnnotationFS annot) {
  // return new Subiterator(
  // this.getIntIterator(),
  // annot,
  // (CASImpl) FSIndexRepositoryImpl.this.cas,
  // this.getIntComparator());
  // }
  //
  // public FSIterator subiterator(AnnotationFS annot, boolean ambiguous) {
  // if (ambiguous) {
  // return subiterator(annot);
  // } else {
  // return new UnambiguousIterator(subiterator(annot), this);
  // }
  // }
  //
  // public FSIterator unambigousIterator() {
  // return new UnambiguousIterator(iterator(), this);
  // }
  //
  // }

  /**
   * The default size of an index.
   */
  public static final int DEFAULT_INDEX_SIZE = 100;

  // A reference to the CAS.
  private CASImpl cas;

  // A reference to the type system.
  private TypeSystemImpl typeSystem;

  // Is the index repository locked?
  private boolean locked = false;

  // An array of ArrayLists, one for each type in the type hierarchy.
  // The ArrayLists are unordered lists of IndexIteratorCachePairs for
  // that type.
  private ArrayList<IndexIteratorCachePair>[] indexArray;

  // an array of ints, one for each type in the type hierarchy.
  // Used to enable iterators to detect modifications (adds / removes)
  // to indexes they're iterating over while they're iterating over them.
  // not private so it can be seen by FSLeafIndexImpl
  int[] detectIllegalIndexUpdates;

  // A map from names to IndexIteratorCachePairs. Different names may map to
  // the same index.
  private HashMap<String, IndexIteratorCachePair> name2indexMap;

  private LinearTypeOrderBuilder defaultOrderBuilder = null;

  private LinearTypeOrder defaultTypeOrder = null;

  private IntVector indexUpdates;

  private BitSet indexUpdateOperation;

  private boolean logProcessed;

  private IntSet fsAddedToIndex;

  private IntSet fsDeletedFromIndex;

  private IntSet fsReindexed;

  // Monitor indexes used to optimize getIndexedFS and flush
  private IntVector usedIndexes;

  private boolean[] isUsed;

  @SuppressWarnings("unused")
  private FSIndexRepositoryImpl() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param cas
   */
  FSIndexRepositoryImpl(CASImpl cas) {
    super();
    this.cas = cas;
    this.typeSystem = cas.getTypeSystemImpl();
    this.name2indexMap = new HashMap<String, IndexIteratorCachePair>();
    this.indexUpdates = new IntVector();
    this.indexUpdateOperation = new BitSet();
    this.fsAddedToIndex = new IntSet();
    this.fsDeletedFromIndex = new IntSet();
    this.fsReindexed = new IntSet();
    this.logProcessed = false;
    init();
  }

  /**
   * Constructor for views.
   * 
   * @param cas
   * @param baseIndexRepository
   */
  FSIndexRepositoryImpl(CASImpl cas, FSIndexRepositoryImpl baseIndexRepo) {
    super();
    this.cas = cas;
    this.typeSystem = cas.getTypeSystemImpl();
    this.name2indexMap = new HashMap<String, IndexIteratorCachePair>();
    this.indexUpdates = new IntVector();
    this.indexUpdateOperation = new BitSet();
    this.fsAddedToIndex = new IntSet();
    this.fsDeletedFromIndex = new IntSet();
    this.fsReindexed = new IntSet();
    this.logProcessed = false;
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
    this.defaultOrderBuilder = baseIndexRepo.defaultOrderBuilder;
    this.defaultTypeOrder = baseIndexRepo.defaultTypeOrder;
  }

  /**
   * Initialize data. Called from the constructor.
   */
  @SuppressWarnings("unchecked")
  private void init() {
    final TypeSystemImpl ts = this.typeSystem;
    // Type counting starts at 1.
    final int numTypes = ts.getNumberOfTypes() + 1;
    // Can't instantiate arrays of generic types.
    this.indexArray = new ArrayList[numTypes];
    for (int i = 1; i < numTypes; i++) {
      this.indexArray[i] = new ArrayList<IndexIteratorCachePair>();
    }
    this.detectIllegalIndexUpdates = new int[numTypes];
    for (int i = 0; i < this.detectIllegalIndexUpdates.length; i++) {
      this.detectIllegalIndexUpdates[i] = Integer.MIN_VALUE;
    }
    this.usedIndexes = new IntVector();
    this.isUsed = new boolean[numTypes];
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
  private IndexIteratorCachePair addNewIndex(FSIndexComparator comparator, int initialSize,
      int indexType) {
    final Type type = comparator.getType();
    final int typeCode = ((TypeImpl) type).getCode();
    if (typeCode >= this.indexArray.length) {
      // assert(false);
    }
    final ArrayList<IndexIteratorCachePair> indexVector = this.indexArray[typeCode];
    // final int vecLen = indexVector.size();
    FSLeafIndexImpl<?> ind;
    switch (indexType) {
    case FSIndex.SET_INDEX: {
      ind = new FSRBTSetIndex(this.cas, type, indexType);
      break;
    }
    case FSIndex.BAG_INDEX: {
      ind = new FSBagIndex(this.cas, type, initialSize, indexType);
      break;
    }
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
    final IndexIteratorCachePair iicp = new IndexIteratorCachePair();
    iicp.index = ind;
    indexVector.add(iicp);
    return iicp;
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

  private static final int findIndex(ArrayList<IndexIteratorCachePair> indexes,
      FSIndexComparator comp) {
    FSIndexComparator indexComp;
    final int max = indexes.size();
    for (int i = 0; i < max; i++) {
      indexComp = indexes.get(i).index.getComparator();
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
      // In this special case, we do not add indeces for subtypes.
      return iicp;
    }
    final Type superType = comparator.getType();
    final Vector<Type> types = this.typeSystem.getDirectlySubsumedTypes(superType);
    final int max = types.size();
    FSIndexComparator compCopy;
    for (int i = 0; i < max; i++) {
      compCopy = ((FSIndexComparatorImpl) comparator).copy();
      compCopy.setType(types.get(i));
      addNewIndexRec(compCopy, indexType);
    }
    return iicp;
  }

  private static final ArrayList<Type> getAllSubsumedTypes(Type t, TypeSystem ts) {
    final ArrayList<Type> v = new ArrayList<Type>();
    addAllSubsumedTypes(t, ts, v);
    return v;
  }

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
    if (this.defaultTypeOrder == null) {
      if (this.defaultOrderBuilder == null) {
        this.defaultOrderBuilder = new LinearTypeOrderBuilderImpl(this.typeSystem);
      }
      try {
        this.defaultTypeOrder = this.defaultOrderBuilder.getOrder();
      } catch (final CASException e) {
        // Since we're doing this on an existing type names, we can't
        // get here.
      }
    }
    return this.defaultTypeOrder;
  }

  public LinearTypeOrderBuilder getDefaultOrderBuilder() {
    if (this.defaultOrderBuilder == null) {
      this.defaultOrderBuilder = new LinearTypeOrderBuilderImpl(this.typeSystem);
    }
    return this.defaultOrderBuilder;
  }

  void setDefaultTypeOrder(LinearTypeOrder order) {
    this.defaultTypeOrder = order;
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
   * @param comp
   * @param label
   * @param indexType
   * @return boolean
   */
  public boolean createIndexNoQuestionsAsked(FSIndexComparator comp, String label, int indexType) {
    IndexIteratorCachePair cp = this.name2indexMap.get(label);
    // Now check if the index already exists.
    if (cp == null) {
      // The name is new.
      cp = this.addNewIndexRecursive(comp, indexType);
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
    // if (this.typeSystem.subsumes(oldType, newType)) {
    // // We don't need to do anything.
    // return true;
    // } else if (this.typeSystem.subsumes(newType, oldType)) {
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
    if (!this.typeSystem.subsumes(indexType, type)) {
      final CASRuntimeException cre = new CASRuntimeException(
          CASRuntimeException.TYPE_NOT_IN_INDEX, new String[] { label, type.getName(),
              indexType.getName() });
      throw cre;
    }
    final int typeCode = ((TypeImpl) type).getCode();
    final ArrayList<IndexIteratorCachePair> inds = this.indexArray[typeCode];
    // Since we found an index for the correct type, find() must return a
    // valid result -- unless this is a special auto-index.
    final int indexCode = findIndex(inds, iicp.index.getComparator());
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
    final Vector<Type> typeVector = this.typeSystem.getDirectlySubsumedTypes(type);
    final int max = typeVector.size();
    for (int i = 0; i < max; i++) {
      numFSs += getIndexSize(typeVector.get(i));
    }
    return numFSs;
  }
  
  /**
   * Remove all instances of a particular type (but not its subtypes) from all indexes
   * @param type
   */
  public void removeAllExcludingSubtypes(Type type) {
    final int typeCode = ((TypeImpl) type).getCode();
    // get a list of all indexes defined over this type
    // Includes indexes defined on supertypes of this type
    final ArrayList<IndexIteratorCachePair> allIndexesForType = this.indexArray[typeCode];
    for (IndexIteratorCachePair iicp : allIndexesForType) {
      iicp.index.flush();
//      boolean fff = iicp.iteratorCache.get(0) == iicp.index;
    }
  }
  
  /**
   * Remove all instances of a particular type (including its subtypes) from all indexes
   * @param type
   */
  public void removeAllIncludingSubtypes(Type type) {
    removeAllExcludingSubtypes(type);
    List<Type> subtypes = this.typeSystem.getDirectSubtypes(type);
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
   * Return an array containing all FSs in any index. This is intended to be used for serialization.
   * Note that duplicate entries in indexes will appear in the array as many times as they occur in
   * an index. The order in which FSs occur in the array does not reflect the order in which they
   * were added to the repository. This means that set indexes deserialized from this list may
   * contain different but equal elements than the original index.
   */
  public int[] getIndexedFSs() {
    final IntVector v = new IntVector();
    IndexIteratorCachePair iicp;
    IntPointerIterator it;
    ArrayList<IndexIteratorCachePair> iv, cv;
    // We may need to profile this. If this is a bottleneck, use a different
    // implementation.
    SortedIntSet set;
    int jMax, indStrat;
    // Iterate over indexes with something in there
    for (int i = 0; i < this.usedIndexes.size(); i++) {
      iv = this.indexArray[this.usedIndexes.get(i)];
      // Iterate over the indexes for the type.
      jMax = iv.size();
      // Create a vector of IICPs. If there is at least one sorted or bag
      // index, pick one arbitrarily and add its FSs (since it contains all
      // FSs that all other indexes for the same type contain). If there are
      // only set indexes, create a set of the FSs in those indexes, since they
      // may all contain different elements (different FSs that have the same "key"
      //   are duplicates for one index, but may not be duplicates for a different one).
      cv = new ArrayList<IndexIteratorCachePair>();
      for (int j = 0; j < jMax; j++) {
        iicp = iv.get(j);
        indStrat = iicp.index.getIndexingStrategy();
        if (indStrat == FSIndex.SET_INDEX) {
          cv.add(iicp);
        } else {
          cv.clear();  // only need to save this one
          cv.add(iicp);
          break;
        }
      }
      if (cv.size() > 0) {
        // Note: This next loop removes duplicates (and also sorts
        // the fs addrs associated with one type)
        // Duplicates arise from having mulitple sets combined, and
        // also if a non-set index had the same identical FS added
        // multiple times.
        set = new SortedIntSet();
        for (int k = 0; k < cv.size(); k++) {
          it = cv.get(k).index.refIterator();
          while (it.isValid()) {
            set.add(it.get());
            it.inc();
          }
        }
        v.add(set.getArray(), 0, set.size());  // bulk add of all elements
//        for (int k = 0; k < set.size(); k++) {
//          v.add(set.get(k));
//        }
      }
    }
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
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr#createTypeSortOrder()
   */
  public LinearTypeOrderBuilder createTypeSortOrder() {
    final LinearTypeOrderBuilder orderBuilder = new LinearTypeOrderBuilderImpl(this.typeSystem);
    if (this.defaultOrderBuilder == null) {
      this.defaultOrderBuilder = orderBuilder;
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
    if (!this.typeSystem.isType(typeCode) || !this.cas.ll_isRefType(typeCode)) {
      final LowLevelException e = new LowLevelException(LowLevelException.INVALID_INDEX_TYPE);
      e.addArgument(Integer.toString(typeCode));
      throw e;
    }
    return (LowLevelIndex) getIndex(indexName, this.typeSystem.ll_getTypeForCode(typeCode));
  }

  public final void ll_addFS(int fsRef, boolean doChecks) {
    if (doChecks) {
      this.cas.checkFsRef(fsRef);
      this.cas.ll_isRefType(this.cas.ll_getFSRefType(fsRef));
    }
    ll_addFS(fsRef);
  }

  public void ll_addFS(int fsRef) {
    // Determine type of FS.
    final int typeCode = this.cas.getHeapValue(fsRef);
    // indicate this type's indexes are being modified
    // in case an iterator is simultaneously active over this type
    incrementIllegalIndexUpdateDetector(typeCode);
    // Get the indexes for the type.
    final ArrayList<IndexIteratorCachePair> indexes = this.indexArray[typeCode];
    // Add fsRef to all indexes.
    final int size = indexes.size();
    for (int i = 0; i < size; i++) {
      indexes.get(i).index.insert(fsRef);
    }
    if (size == 0) {
      // lazily create a default bag index for this type
      final Type type = this.typeSystem.ll_getTypeForCode(typeCode);
      final String defIndexName = getAutoIndexNameForType(type);
      final FSIndexComparator comparator = createComparator();
      comparator.setType(type);
      createIndexNoQuestionsAsked(comparator, defIndexName, FSIndex.DEFAULT_BAG_INDEX);
      assert this.indexArray[typeCode].size() == 1;
      // add the FS to the bag index
      this.indexArray[typeCode].get(0).index.insert(fsRef);
    }
    if (this.cas.getCurrentMark() != null) {
      logIndexOperation(fsRef, true);
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

  public void ll_removeFS(int fsRef) {
    final int typeCode = this.cas.ll_getFSRefType(fsRef);
    incrementIllegalIndexUpdateDetector(typeCode);
    final ArrayList<IndexIteratorCachePair> idxList = this.indexArray[typeCode];
    final int max = idxList.size();
    for (int i = 0; i < max; i++) {
      idxList.get(i).index.remove(fsRef);
    }
    if (this.cas.getCurrentMark() != null) {
      logIndexOperation(fsRef, false);
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
    return new FSIteratorAggregate<FeatureStructure>(iteratorList);
  }

  @SuppressWarnings("unchecked")
  private final void getAllIndexedFS(Type type, List<FSIterator<FeatureStructure>> iteratorList) {
    // Start by looking for an auto-index. If one exists, no other index exists.
    final FSIndex<FeatureStructure> autoIndex = getIndex(getAutoIndexNameForType(type));
    if (autoIndex != null) {
      iteratorList.add(autoIndex.iterator());
      // We found one of the special auto-indexes which don't inherit down the tree. So, we
      // manually need to traverse the inheritance tree to look for more indexes. Note that
      // this is not necessary when we have a regular index
      final List<Type> subtypes = this.typeSystem.getDirectSubtypes(type);
      for (int i = 0; i < subtypes.size(); i++) {
        getAllIndexedFS(subtypes.get(i), iteratorList);
      }
      return;
    }
    // Attempt to find a non-set index first.
    // If none found, then use the an arbitrary set index if any.
    FSIndex setIndex = null;
    final Iterator<String> iter = getLabels();
    while (iter.hasNext()) {
      final String label = iter.next();
      final FSIndex index = getIndex(label);
      // Ignore auto-indexes at this stage, they're handled above.
      if (index.getIndexingStrategy() == FSIndex.DEFAULT_BAG_INDEX) {
        continue;
      }
      if (this.typeSystem.subsumes(index.getType(), type)) {
        if (index.getIndexingStrategy() != FSIndex.SET_INDEX) {
          iteratorList.add(getIndex(label, type).iterator());
          // Done, found non-set index.
          return;
        }
        setIndex = getIndex(label, type);
      }
    }
    // No sorted or bag index found for this type. If there was a set index,
    // return an iterator for it.
    if (setIndex != null) {
      iteratorList.add(setIndex.iterator());
      return;
    }
    // No index for this type was found at all. Since the auto-indexes are created on demand for
    // each type, there may be gaps in the inheritance chain. So keep descending the inheritance
    // tree looking for relevant indexes.
    final List subtypes = this.typeSystem.getDirectSubtypes(type);
    for (int i = 0; i < subtypes.size(); i++) {
      getAllIndexedFS((Type) subtypes.get(i), iteratorList);
    }
  }

  private void logIndexOperation(int fsRef, boolean added) {
    this.indexUpdates.add(fsRef);
    if (added) {
      this.indexUpdateOperation.set(this.indexUpdates.size() - 1, added);
    }
    this.logProcessed = false;
  }

  // Delta Serialization support
  private void processIndexUpdates() {
    for (int i = 0; i < this.indexUpdates.size(); i++) {
      final int fsRef = this.indexUpdates.get(i);
      final boolean added = this.indexUpdateOperation.get(i);
      if (added) {
        if (this.fsDeletedFromIndex.contains(fsRef)) {
          this.fsDeletedFromIndex.remove(this.fsDeletedFromIndex.indexOf(fsRef));
          this.fsReindexed.add(fsRef);
        } else {
          this.fsAddedToIndex.add(fsRef);
        }
      } else {
        if (this.fsAddedToIndex.contains(fsRef)) {
          this.fsAddedToIndex.remove(this.fsAddedToIndex.indexOf(fsRef));
        } else if (this.fsReindexed.contains(fsRef)) {
          this.fsReindexed.remove(fsRef);
        } else {
          this.fsDeletedFromIndex.add(fsRef);
        }
      }
    }
    this.logProcessed = true;
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
}
