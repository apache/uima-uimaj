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

import java.util.Comparator;
import java.util.NoSuchElementException;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.cas.TOP;

/**
 * Performs an ordered iteration among a set of iterators, each one corresponding to
 *   the type or subtype of the uppermost type.
 *   
 * The set of iterators is maintained in an array, with the 0th element being the current valid iterator.
 * 
 * This class doesn't do concurrent mod checking - that's done by the individual iterators.
 *
 * @param <T> result type
 */
public class FsIterator_subtypes_ordered<T extends FeatureStructure> 
                    extends FsIterator_subtypes_list<T> {
 
  /**
   * The number of elements to keep in order before the binary heap starts. This section helps the
   * performance in cases where a couple of types dominate the index.
   * 
   * The sorted section is searched sequentially.
   * Above the sorted section, the search is done using binary search
   */
  private static final int SORTED_SECTION = 3;
  
  private boolean wentForward = true;
  
  final private Comparator<TOP> comparator; 

  public FsIterator_subtypes_ordered(FsIndex_iicp<T> iicp) {
    super(iicp);
    this.comparator = (iicp.fsIndex_singletype instanceof FsIndex_set_sorted) 
                        ? ((FsIndex_set_sorted)iicp.fsIndex_singletype).getComparatorWithId()
                        : (Comparator) iicp.fsIndex_singletype;
    moveToStart();
  } 
  
  /**
   * Move operators have to move a group of iterators for this type and all its subtypes
   */
  
  @Override
  public void moveToFirst() {
    if (firstChangedEmptyIterator() >= 0) {
      this.nonEmptyIterators = initIterators();
    }
    // no need to call isIndexesHaveBeenUpdated because
    // there's no state in this iterator that needs updating.
    moveToStart();
  }
  
  private void moveToStart() {
    
    int lvi = this.nonEmptyIterators.length - 1;
    // Need to consider all iterators.
    // Set all iterators to insertion point.
    int i = 0;
    while (i <= lvi) {
      final FsIterator_singletype<T> it = this.nonEmptyIterators[i];
      it.moveToFirst();
      if (it.isValid()) {
        heapify_up(it, i, 1);
        ++i;
      } else {
        // swap this iterator with the last possibly valid one
        // lvi might be equal to i, this will not be a problem
        this.nonEmptyIterators[i] = this.nonEmptyIterators[lvi];
        this.nonEmptyIterators[lvi] = it;
        --lvi;
      }
    }
    // configured to continue with forward iterations
    this.wentForward = true;
    this.lastValidIndex = lvi;
  }
  
  @Override
  public void moveToLast() {
    if (firstChangedEmptyIterator() >= 0) {
      this.nonEmptyIterators = initIterators();
    }
    // no need to call isIndexesHaveBeenUpdated because
    // there's no state in this iterator that needs updating.
    
    int lvi = this.nonEmptyIterators.length - 1;
    // Need to consider all iterators.
    // Set all iterators to insertion point.
    int i = 0;
    while (i <= lvi) {
      final FsIterator_singletype<T> it = this.nonEmptyIterators[i];
      it.resetConcurrentModification();
      it.moveToLast();
      if (it.isValid()) {
        heapify_up(it, i, -1);
        ++i;
      } else {
        // swap this iterator with the last possibly valid one
        // lvi might be equal to i, this will not be a problem
        this.nonEmptyIterators[i] = this.nonEmptyIterators[lvi];
        this.nonEmptyIterators[lvi] = it;
        --lvi;
      }
    }
    // configured to continue with backward iterations
    this.wentForward = false;
    this.lastValidIndex = lvi;
  }

  @Override
  public void moveToNext() {
    if (!isValid()) {
      return;
    }

    final FsIterator_singletype<T> it0 = nonEmptyIterators[0]/*.checkConcurrentModification()*/;

    if (this.wentForward) {
      it0.moveToNextNvc();
      heapify_down(it0, 1);
    } else {
      moveToNextCmn(it0);
    }
  }

  @Override
  public void moveToNextNvc() {
    final FsIterator_singletype<T> it0 = nonEmptyIterators[0]/*.checkConcurrentModification()*/;

    if (this.wentForward) {
      it0.moveToNextNvc();
      heapify_down(it0, 1);
    } else {
      moveToNextCmn(it0);
    }
  }

  /**
   * 
   * @param it0 guaranteed to be a valid iterator by callers
   */
  private void moveToNextCmn(final FsIterator_singletype<T> it0) {
    // We need to increment everything.
    int lvi = this.nonEmptyIterators.length - 1;
    int i = 1;
    while (i <= lvi) {
      // Any iterator other than the current one needs to be
      // incremented until it's pointing at something that's
      // greater than the current element.
      final FsIterator_singletype<T> it = nonEmptyIterators[i]/*.checkConcurrentModification()*/;
      // If the iterator we're considering is not valid, we
      // set it to the first element. This should be it for this iterator...
      if (!it.isValid()) {
        it.moveToFirst();
      }
      // Increment the iterator while it is valid and pointing
      // at something smaller than the current element.
      while (it.isValid() && is_before(it, it0, 1)) {
        it.moveToNext();
      }

      // find placement
      if (it.isValid()) {
        heapify_up(it, i, 1);
        ++i;
      } else {
        // swap this iterator with the last possibly valid one
        // lvi might be equal to i, this will not be a problem
        this.nonEmptyIterators[i] = this.nonEmptyIterators[lvi];
        this.nonEmptyIterators[lvi] = it;
        --lvi;
      }
    }

    this.lastValidIndex = lvi;
    this.wentForward = true;

    it0.moveToNext();
    heapify_down(it0, 1);

  }
  
  @Override
  public void moveToPrevious() {
    if (!isValid()) {
      return;
    }

    moveToPreviousNvc();
  }
  
  @Override
  public void moveToPreviousNvc() {
    final FsIterator_singletype<T> it0 = nonEmptyIterators[0]/*.checkConcurrentModification()*/;
    if (!this.wentForward) {
      it0.moveToPreviousNvc();
      // this also takes care of invalid iterators
      heapify_down(it0, -1);
    } else {
      // We need to decrement everything.
      int lvi = this.nonEmptyIterators.length - 1;
      int i = 1;
      while (i <= lvi) {
        // Any iterator other than the current one needs to be
        // decremented until it's pointing at something that's
        // smaller than the current element.
        final FsIterator_singletype<T> it = nonEmptyIterators[i]/*.checkConcurrentModification()*/;
        // If the iterator we're considering is not valid, we
        // set it to the last element. This should be it for this iterator...
        if (!it.isValid()) {
          it.moveToLast();
        }
        // Decrement the iterator while it is valid and pointing
        // at something greater than the current element.
        while (it.isValid() && is_before(it, it0, -1)) {
          it.moveToPrevious();
        }

        // find placement
        if (it.isValid()) {
          heapify_up(it, i, -1);
          ++i;
        } else {
          // swap this iterator with the last possibly valid one
          // lvi might be equal to i, this will not be a problem
          this.nonEmptyIterators[i] = this.nonEmptyIterators[lvi];
          this.nonEmptyIterators[lvi] = it;
          --lvi;
        }
      }

      this.lastValidIndex = lvi;
      this.wentForward = false;

      it0.moveToPrevious();
      heapify_down(it0, -1);
    }
  }


  /**
   * Test the order with which the two iterators should be used. Introduces arbitrary ordering for
   * equivalent FSs. Only called with valid iterators.
   * 
   * @param l - guaranteed to ba a valid iterator by callers
   * @param r - guaranteed to be a valid iterator by callers
   * @param dir 
   *          Direction of movement, 1 for forward, -1 for backward
   * @return true if the left iterator needs to be used before the right one.
   */
  private boolean is_before(FsIterator_singletype<T> l, FsIterator_singletype<T> r,
      int dir) {

//    // debug
//    if (!l.isValid()) {
//      throw new RuntimeException("1st arg invalid");
//    }
//
//    if (!r.isValid()) {
//      throw new RuntimeException("2nd arg invalid");
//    }

    final TOP fsLeft = (TOP) l.getNvc();
    final TOP fsRight = (TOP) r.getNvc();
    
    int d = comparator.compare(fsLeft, fsRight);

    // If two FSs are identical wrt the comparator of the index,
    // we still need to be able to distinguish them to be able to have a
    // well-defined sequence. In that case, we arbitrarily order FSs by
    // their
    // addresses. We need to do this in order to be able to ensure that a
    // reverse iterator produces the reverse order of the forward iterator.
    if (d == 0) {
      d = fsLeft._id() - fsRight._id();
    }
    return d * dir < 0;
  }

  /**
   * Move the idx'th element up in the heap until it finds its proper position.
   * 
   * @param it
   *          indexes[idx], guaranteed to be "valid"
   * @param idx
   *          Element to move
   * @param dir
   *          Direction of iterator movement, 1 for forward, -1 for backward
   */
  private void heapify_up(FsIterator_singletype<T> it, int idx, int dir) {
//    FSIndexFlat<? extends FeatureStructure> flatIndexInfo = iicp.flatIndex;
//    if (null != flatIndexInfo) {
//      flatIndexInfo.incrementReorderingCount();
//    }
    int nidx;

    while (idx > SORTED_SECTION) {
      nidx = (idx + SORTED_SECTION - 1) >> 1;
      if (!is_before(it, this.nonEmptyIterators[nidx], dir)) {
        this.nonEmptyIterators[idx] = it;
        return;
      }
      this.nonEmptyIterators[idx] = this.nonEmptyIterators[nidx];
      idx = nidx;
    }

    while (idx > 0) {
      nidx = idx - 1;
      if (!is_before(it, this.nonEmptyIterators[nidx], dir)) {
        this.nonEmptyIterators[idx] = it;
        return;
      }
      this.nonEmptyIterators[idx] = this.nonEmptyIterators[nidx];
      idx = nidx;
    }

    this.nonEmptyIterators[idx] = it;
  }

  /**
   * Move the top element down in the heap until it finds its proper position.
   * 
   * @param it
   *          indexes[0], may be invalid
   * @param dir
   *          Direction of iterator movement, 1 for forward, -1 for backward
   */
  private void heapify_down(FsIterator_singletype<T> it, int dir) {
//    FSIndexFlat<? extends FeatureStructure> flatIndexInfo = iicp.flatIndex;
//    if (null != flatIndexInfo) {
//      flatIndexInfo.incrementReorderingCount();
//    }

    if (!it.isValid()) {
      // if at the end of the iteration, the lastValidIndex is this one (e.g., is 0)
      // and this operation is a noop, but sets the lastValidIndex to -1, indicating the iterator is invalid
      final FsIterator_singletype<T> itl = this.nonEmptyIterators[this.lastValidIndex]/*.checkConcurrentModification()*/;
      this.nonEmptyIterators[this.lastValidIndex] = it;
      this.nonEmptyIterators[0] = itl;
      --this.lastValidIndex;
      it = itl;
    }

    final int num = this.lastValidIndex;
    if ((num < 1) || !is_before(this.nonEmptyIterators[1]/*.checkConcurrentModification()*/, it, dir)) {
      return;
    }

    int idx = 1;
    this.nonEmptyIterators[0] = this.nonEmptyIterators[1];
    final int end = Math.min(num, SORTED_SECTION);
    int nidx = idx + 1;

    // make sure we don't leave the iterator in a completely invalid state
    // (i.e. one it can't recover from using moveTo/moveToFirst/moveToLast)
    // in case of a concurrent modification
    try {
      while (nidx <= end) {
        if (!is_before(this.nonEmptyIterators[nidx]/*.checkConcurrentModification()*/, it, dir)) {
          return; // passes through finally
        }

        this.nonEmptyIterators[idx] = this.nonEmptyIterators[nidx];
        idx = nidx;
        nidx = idx + 1;
      }

      nidx = SORTED_SECTION + 1;
      while (nidx <= num) {
        if ((nidx < num)
            && is_before(this.nonEmptyIterators[nidx+1]/*.checkConcurrentModification()*/,
                this.nonEmptyIterators[nidx]/*.checkConcurrentModification()*/, dir)) {
          ++nidx;
        }

        if (!is_before(this.nonEmptyIterators[nidx], it, dir)) {
          return;
        }

        this.nonEmptyIterators[idx] = this.nonEmptyIterators[nidx];
        idx = nidx;
        nidx = (nidx << 1) - (SORTED_SECTION - 1);
      }
    } finally {
      this.nonEmptyIterators[idx] = it;
    }
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#isValid()
   */
  @Override
  public boolean isValid() {
    return lastValidIndex >= 0;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#get()
   */
  @Override
  public T get() throws NoSuchElementException {
    if (!isValid()) {
      throw new NoSuchElementException();
    }
    return getNvc();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#getNvc()
   */
  @Override
  public T getNvc() throws NoSuchElementException {
    return nonEmptyIterators[0]/*.checkConcurrentModification()*/.get();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveTo(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public void moveTo(FeatureStructure fs) {
    if (firstChangedEmptyIterator() >= 0) {
      this.nonEmptyIterators = initIterators();
    }
    // no need to call isIndexesHaveBeenUpdated because
    // there's no state in this iterator that needs updating.

    int lvi = this.nonEmptyIterators.length - 1;
    // Need to consider all iterators.
    // Set all iterators to insertion point.
    int i = 0;
    while (i <= lvi) {
      final FsIterator_singletype<T> it = this.nonEmptyIterators[i];
      it.moveTo(fs);  
      if (it.isValid()) {
        heapify_up(it, i, 1);
        ++i;
      } else {
        // swap this iterator with the last possibly valid one
        // lvi might be equal to i, this will not be a problem
        this.nonEmptyIterators[i] = this.nonEmptyIterators[lvi];
        this.nonEmptyIterators[lvi] = it;
        --lvi;
      }
    }
    // configured to continue with forward iterations
    this.wentForward = true;
    this.lastValidIndex = lvi;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  @Override
  public FSIterator<T> copy() {
    FsIterator_subtypes_ordered<T> it = new FsIterator_subtypes_ordered<T>(iicp);
    if (!isValid()) {
      it.moveToPrevious();  // mark new one also invalid
    } else {
      T posFs = get();
      it.moveTo(posFs);  // moves to left-most position
      while(it.get() != posFs) {
        it.moveToNext();
      }
    }
    return it;
  }

  

  
}
