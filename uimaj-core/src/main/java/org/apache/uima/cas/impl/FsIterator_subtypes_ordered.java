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
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Performs an ordered iteration among a set of iterators, each one corresponding to the type or
 * subtype of the uppermost type.
 * 
 * The set of iterators is maintained in an array, with the 0th element being the current valid
 * iterator.
 *
 * @param <T>
 *          result type
 */
public class FsIterator_subtypes_ordered<T extends FeatureStructure>
        extends FsIterator_multiple_indexes<T> {

  private static final boolean DEBUG = false;

  /**
   * The number of elements to keep in order before the binary heap starts. This section helps the
   * performance in cases where a couple of types dominate the index.
   * 
   * The sorted section is searched sequentially. Above the sorted section, the search is done using
   * binary search
   */
  private static final int SORTED_SECTION = 3;

  /** index into nonEmptyIterators, shows last valid one */
  protected int lastValidIteratorIndex = -1;

  private boolean wentForward = true;

  // The IICP
  private final FsIndex_iicp<T> iicp;

  // /** true if sorted index, with typepriority as a key, but ignoring it because
  // * either there are no type priorities defined, or
  // * using a select-API-created iterator configured without typePriority
  // *
  // * Not final for the use case where there's a type-order key, type priorities are specified,
  // * but a select-API-created iterator wants to ignore type priorities.
  // */
  // final private boolean isSortedAndIgnoringTypeOrderKey;

  // call used by select framework to specify ignoring type priority
  public FsIterator_subtypes_ordered(FsIndex_iicp<T> iicp,
          Comparator<TOP> comparatorMaybeNoTypeWithoutId) {
    super(iicp, iicp.getIterators(), comparatorMaybeNoTypeWithoutId);
    this.iicp = iicp;
    FsIndex_set_sorted<T> idx2 = (FsIndex_set_sorted<T>) iicp.fsIndex_singletype;
    moveToFirstNoReinit();
  }

  /**
   * Move operators have to move a group of iterators for this type and all its subtypes
   */

  @Override
  public void moveToFirstNoReinit() {
    if (DEBUG) {
      dumpIteratorInfo("moveToFirstNoReinit - start");
    }

    int lvi = nonEmptyIterators.length - 1;
    // Need to consider all iterators.
    // Set all iterators to insertion point.
    int i = 0;
    while (i <= lvi) {
      final LowLevelIterator<T> it = nonEmptyIterators[i];
      it.moveToFirstNoReinit();
      if (it.isValid()) {
        heapify_up(it, i, 1);
        ++i;
      } else {
        // swap this iterator with the last possibly valid one
        // lvi might be equal to i, this will not be a problem
        nonEmptyIterators[i] = nonEmptyIterators[lvi];
        nonEmptyIterators[lvi] = it;
        --lvi;
      }
    }
    // configured to continue with forward iterations
    wentForward = true;
    lastValidIteratorIndex = lvi;

    if (DEBUG) {
      dumpIteratorInfo("moveToFirstNoReinit - end");
    }
  }

  @Override
  public void moveToLastNoReinit() {
    if (DEBUG) {
      dumpIteratorInfo("moveToLastNoReinit - start");
    }

    // no need to call isIndexesHaveBeenUpdated because
    // there's no state in this iterator that needs updating.

    int lvi = nonEmptyIterators.length - 1;
    // Need to consider all iterators.
    // Set all iterators to insertion point.
    int i = 0;
    while (i <= lvi) {
      final LowLevelIterator<T> it = nonEmptyIterators[i];
      // it.resetConcurrentModification();
      it.moveToLastNoReinit();
      if (it.isValid()) {
        heapify_up(it, i, -1);
        ++i;
      } else {
        // swap this iterator with the last possibly valid one
        // lvi might be equal to i, this will not be a problem
        nonEmptyIterators[i] = nonEmptyIterators[lvi];
        nonEmptyIterators[lvi] = it;
        --lvi;
      }
    }
    // configured to continue with backward iterations
    wentForward = false;
    lastValidIteratorIndex = lvi;

    if (DEBUG) {
      dumpIteratorInfo("moveToLastNoReinit - end");
    }
  }

  @Override
  public void moveToNextNvc() {
    if (DEBUG) {
      dumpIteratorInfo("moveToNextNvc - start");
    }

    final LowLevelIterator<T> it0 = nonEmptyIterators[0]/* .checkConcurrentModification() */;

    if (wentForward) {
      it0.moveToNextNvc();
      heapify_down(it0, 1);
    } else {
      moveToNextCmn(it0);
    }

    if (DEBUG) {
      dumpIteratorInfo("moveToNextNvc - end");
    }
  }

  /**
   * 
   * @param it0
   *          guaranteed to be a valid iterator by callers
   */
  private void moveToNextCmn(final LowLevelIterator<T> it0) {
    // We need to increment everything.
    int lvi = nonEmptyIterators.length - 1;
    int i = 1;
    while (i <= lvi) {
      // Any iterator other than the current one needs to be
      // incremented until it's pointing at something that's
      // greater than the current element.
      final LowLevelIterator<T> it = nonEmptyIterators[i]/* .checkConcurrentModification() */;
      // If the iterator we're considering is not valid, we
      // set it to the first element. This should be it for this iterator...
      if (!it.isValid()) {
        it.moveToFirstNoReinit();
      }
      // Increment the iterator while it is valid and pointing
      // at something smaller than the current element.
      while (it.isValid() && is_before(it, it0, 1)) {
        it.moveToNextNvc();
      }

      // find placement
      if (it.isValid()) {
        heapify_up(it, i, 1);
        ++i;
      } else {
        // swap this iterator with the last possibly valid one
        // lvi might be equal to i, this will not be a problem
        nonEmptyIterators[i] = nonEmptyIterators[lvi];
        nonEmptyIterators[lvi] = it;
        --lvi;
      }
    }

    lastValidIteratorIndex = lvi;
    wentForward = true;

    it0.moveToNext();
    heapify_down(it0, 1);

  }

  @Override
  public void moveToPreviousNvc() {
    if (DEBUG) {
      dumpIteratorInfo("moveToPreviousNvc - start");
    }

    final LowLevelIterator<T> it0 = nonEmptyIterators[0]/* .checkConcurrentModification() */;
    if (!wentForward) {
      it0.moveToPreviousNvc();
      // this also takes care of invalid iterators
      heapify_down(it0, -1);
    } else {
      // We need to decrement everything.
      int lvi = nonEmptyIterators.length - 1;
      int i = 1;
      while (i <= lvi) {
        // Any iterator other than the current one needs to be
        // decremented until it's pointing at something that's
        // smaller than the current element.
        final LowLevelIterator<T> it = nonEmptyIterators[i]/* .checkConcurrentModification() */;
        // If the iterator we're considering is not valid, we
        // set it to the last element. This should be it for this iterator...
        if (!it.isValid()) {
          it.moveToLastNoReinit();
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
          nonEmptyIterators[i] = nonEmptyIterators[lvi];
          nonEmptyIterators[lvi] = it;
          --lvi;
        }
      }

      lastValidIteratorIndex = lvi;
      wentForward = false;

      it0.moveToPrevious();
      heapify_down(it0, -1);

    }

    if (DEBUG) {
      dumpIteratorInfo("moveToPreviousNvc - end");
    }
  }

  /**
   * Test the order with which the two iterators should be used. Introduces arbitrary ordering for
   * equivalent FSs. Only called with valid iterators.
   * 
   * @param l
   *          - guaranteed to ba a valid iterator by callers
   * @param r
   *          - guaranteed to be a valid iterator by callers
   * @param dir
   *          Direction of movement, 1 for forward, -1 for backward
   * @return true if the left iterator needs to be used before the right one.
   */
  private boolean is_before(LowLevelIterator<T> l, LowLevelIterator<T> r, int dir) {

    // // debug
    // if (!l.isValid()) {
    // throw new RuntimeException("1st arg invalid");
    // }
    //
    // if (!r.isValid()) {
    // throw new RuntimeException("2nd arg invalid");
    // }

    int d = compare(l.getNvc(), r.getNvc());
    return d * dir < 0;
  }

  // @formatter:off
  /**
   * Only used to compare two iterator's with different types position
   * @param fsLeft the left iterator's element
   * @param fsRight the right iterator's element
   * @return  1 if left > right,   (compare maybe ignores type)
   *         -1 if left < right,   (compare maybe ignores type)
   *          1 if left == right and left.id > right.id
   *         -1 if left == right and left.id < right.id
   */
  // @formatter:on
  private int compare(FeatureStructure fsLeft, FeatureStructure fsRight) {
    int d = comparatorMaybeNoTypeWithoutId.compare((TOP) fsLeft, (TOP) fsRight);

    // If two FSs are identical wrt the comparator of the index,
    // we still need to be able to distinguish them to be able to have a
    // well-defined sequence. In that case, we arbitrarily order FSs by
    // their ids. We need to do this in order to be able to ensure that a
    // reverse iterator produces the reverse order of the forward iterator.
    if (d == 0) {
      d = fsLeft._id() - fsRight._id();
    }
    return d;
  }

  /**
   * Move the idx'th iterator element up in the heap until it finds its proper position. Up means
   * previous iterators are before it
   * 
   * @param it
   *          indexes[idx], guaranteed to be "valid"
   * @param idx
   *          Element to move, nonEmptyIterators[i] == it
   * @param dir
   *          Direction of iterator movement, 1 for forward, -1 for backward
   */
  private void heapify_up(LowLevelIterator<T> it, int idx, int dir) {
    // FSIndexFlat<? extends FeatureStructure> flatIndexInfo = iicp.flatIndex;
    // if (null != flatIndexInfo) {
    // flatIndexInfo.incrementReorderingCount();
    // }
    int nidx;

    while (idx > SORTED_SECTION) {
      nidx = (idx + SORTED_SECTION - 1) >> 1;
      if (!is_before(it, nonEmptyIterators[nidx], dir)) {
        nonEmptyIterators[idx] = it;
        return;
      }
      nonEmptyIterators[idx] = nonEmptyIterators[nidx];
      idx = nidx;
    }

    while (idx > 0) {
      nidx = idx - 1;
      if (!is_before(it, nonEmptyIterators[nidx], dir)) {
        nonEmptyIterators[idx] = it;
        return;
      }
      nonEmptyIterators[idx] = nonEmptyIterators[nidx];
      idx = nidx;
    }

    nonEmptyIterators[idx] = it;
  }

  /**
   * Move the top element down in the heap until it finds its proper position.
   * 
   * @param it
   *          indexes[0], may be invalid
   * @param dir
   *          Direction of iterator movement, 1 for forward, -1 for backward
   */
  private void heapify_down(LowLevelIterator<T> it, int dir) {
    // FSIndexFlat<? extends FeatureStructure> flatIndexInfo = iicp.flatIndex;
    // if (null != flatIndexInfo) {
    // flatIndexInfo.incrementReorderingCount();
    // }

    if (!it.isValid()) {
      // if at the end of the iteration, the lastValidIteratorIndex is this one (e.g., is 0)
      // and this operation is a noop, but sets the lastValidIteratorIndex to -1, indicating the
      // iterator is invalid
      final LowLevelIterator<T> itl = nonEmptyIterators[lastValidIteratorIndex]/*
                                                                                * .checkConcurrentModification
                                                                                * ()
                                                                                */;
      nonEmptyIterators[lastValidIteratorIndex] = it;
      nonEmptyIterators[0] = itl;
      --lastValidIteratorIndex;
      it = itl;
    }

    final int num = lastValidIteratorIndex;
    if ((num < 1)
            || !is_before(nonEmptyIterators[1]/* .checkConcurrentModification() */, it, dir)) {
      return;
    }

    int idx = 1;
    nonEmptyIterators[0] = nonEmptyIterators[1];
    final int end = Math.min(num, SORTED_SECTION);
    int nidx = idx + 1;

    // make sure we don't leave the iterator in a completely invalid state
    // (i.e. one it can't recover from using moveTo/moveToFirst/moveToLast)
    // in case of a concurrent modification
    try {
      while (nidx <= end) {
        if (!is_before(nonEmptyIterators[nidx]/* .checkConcurrentModification() */, it, dir)) {
          return; // passes through finally
        }

        nonEmptyIterators[idx] = nonEmptyIterators[nidx];
        idx = nidx;
        nidx = idx + 1;
      }

      nidx = SORTED_SECTION + 1;
      while (nidx <= num) {
        if ((nidx < num)
                && is_before(nonEmptyIterators[nidx + 1]/* .checkConcurrentModification() */,
                        nonEmptyIterators[nidx]/* .checkConcurrentModification() */, dir)) {
          ++nidx;
        }

        if (!is_before(nonEmptyIterators[nidx], it, dir)) {
          return;
        }

        nonEmptyIterators[idx] = nonEmptyIterators[nidx];
        idx = nidx;
        nidx = (nidx << 1) - (SORTED_SECTION - 1);
      }
    } finally {
      nonEmptyIterators[idx] = it;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#isValid()
   */
  @Override
  public boolean isValid() {
    return lastValidIteratorIndex >= 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#getNvc()
   */
  @Override
  public T getNvc() throws NoSuchElementException {
    return nonEmptyIterators[0].get();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveTo(org.apache.uima.cas.FeatureStructure)
   * 
   * set all iterators to insertion point
   * 
   * While rattling the iterators for sort order: if ignoreType_moveTo use compare without type
   */
  @Override
  public void moveToNoReinit(FeatureStructure fs) {
    if (DEBUG) {
      dumpIteratorInfo("moveToNoReinit - start");
    }

    // no need to call isIndexesHaveBeenUpdated because
    // there's no state in this iterator that needs updating.

    // set null unless need special extra type compare
    // Type typeCompare = (isSortedAndIgnoringTypeOrderKey) ? fs.getType() : null;

    int lastValidIterator_local = nonEmptyIterators.length - 1;
    // Need to consider all iterators.
    // Set all iterators to insertion point.
    int i = 0;
    while (i <= lastValidIterator_local) {
      final LowLevelIterator<T> it = nonEmptyIterators[i];
      it.moveToNoReinit(fs);

      // If the moveTo operation moved the iterator beyond the end (made it invalid), we need to
      // check if this was really justified according to our comparator. If our comparator says that
      // there are elements in the iterator which are equivalent according to the comparator
      // (can happen in particular if the comparator ignores type priorities), then we need to
      // back up the iterator and restore its validity.
      if (!it.isValid()) {
        it.moveToLastNoReinit();

        boolean wentBack = false;
        while (it.isValid()
                && comparatorMaybeNoTypeWithoutId.compare((TOP) it.get(), (TOP) fs) == 0) {
          it.moveToPreviousNvc();
          wentBack = true;
        }
        if (!it.isValid()) {
          if (wentBack) {
            // Go back to the last one matching according to the comparator
            it.moveToFirst();
          }
        } else {
          // Go back to the last one matching according to the comparator (or simply to where we
          // came from)
          it.moveToNext();
        }
      }
      // The moveTo operation may have moved the iterator beyond the last potentially matching FS
      // because the target FS is of a different type then the index. We need to check that and it
      // necessary back up
      else {
        it.moveToPrevious();
        while (it.isValid()
                && comparatorMaybeNoTypeWithoutId.compare((TOP) it.get(), (TOP) fs) == 0) {
          it.moveToPreviousNvc();
        }

        if (!it.isValid()) {
          // Go back to the last one matching according to the comparator
          it.moveToFirst();
        } else {
          // Go back to the last one matching according to the comparator (or simply to where we
          // came from)
          it.moveToNext();
        }
      }

      if (it.isValid()) {
        heapify_up(it, i, 1);
        ++i;
      } else {
        // swap this iterator with the last possibly valid one
        // lvi might be equal to i, this will not be a problem
        nonEmptyIterators[i] = nonEmptyIterators[lastValidIterator_local];
        nonEmptyIterators[lastValidIterator_local] = it;
        --lastValidIterator_local;
      }
    }

    // configured to continue with forward iterations
    wentForward = true;
    lastValidIteratorIndex = lastValidIterator_local;

    if (DEBUG) {
      dumpIteratorInfo("moveToNoReinit - end");
    }
  }

  // /* (non-Javadoc)
  // * @see org.apache.uima.cas.FSIterator#moveTo(org.apache.uima.cas.FeatureStructure)
  // */
  // @Override
  // public void moveToExactNoReinit(FeatureStructure fs) {
  //
  // int lvi = this.nonEmptyIterators.length - 1;
  // // Need to consider all iterators.
  // // Set all iterators to insertion point.
  // int i = 0;
  // while (i <= lvi) {
  // final LowLevelIterator<T> it = this.nonEmptyIterators[i];
  // it.moveToExactNoReinit(fs);
  // if (it.isValid()) {
  // heapify_up(it, i, 1);
  // ++i;
  // } else {
  // // swap this iterator with the last possibly valid one
  // // lvi might be equal to i, this will not be a problem
  // this.nonEmptyIterators[i] = this.nonEmptyIterators[lvi];
  // this.nonEmptyIterators[lvi] = it;
  // --lvi;
  // }
  // }
  // // configured to continue with forward iterations
  // this.wentForward = true;
  // this.lastValidIteratorIndex = lvi;
  // }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  @Override
  public FSIterator<T> copy() {
    FsIterator_subtypes_ordered<T> it = new FsIterator_subtypes_ordered<>(iicp,
            comparatorMaybeNoTypeWithoutId);
    if (!isValid()) {
      it.moveToPrevious(); // mark new one also invalid
    } else {
      T posFs = getNvc();
      it.moveToNoReinit(posFs); // moves to left-most position
      while (it.get() != posFs) {
        it.moveToNext();
      }
    }
    return it;
  }

  @Override
  public Comparator<TOP> getComparator() {
    return comparatorMaybeNoTypeWithoutId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelIterator#moveToSupported()
   */
  @Override
  public boolean isMoveToSupported() {
    return true;
  }

  private void dumpIteratorInfo(String context) {
    StringBuilder sb = new StringBuilder();
    for (LowLevelIterator it : nonEmptyIterators) {
      FsIterator_set_sorted2 it2 = (FsIterator_set_sorted2) it;
      sb.append(String.format("  [%s@%d] %s %b pos:%d ", it.getClass().getSimpleName(),
              System.identityHashCode(it), it.getType(), it.isValid(), it2.pos));
      if (it.isValid()) {
        Annotation a = (Annotation) it.get();
        sb.append(
                String.format("%s@[%d-%d]", a.getType().getShortName(), a.getBegin(), a.getEnd()));
      } else {
        sb.append("<invalid>");
      }
      sb.append("\n");
    }
    System.out.printf("[%s@%d] %s (lvi: %d  wentForward:%b)%n%s", getClass().getSimpleName(),
            System.identityHashCode(this), context, lastValidIteratorIndex, wentForward, sb);
  }
}
