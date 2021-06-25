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
 * Wraps FSIterator<T>, limits results to n gets. Moving the iterator around does not count towards
 * the limit.
 */
class FsIterator_limited<T extends FeatureStructure> implements LowLevelIterator<T> {

  final private LowLevelIterator<T> iterator; // not just for single-type iterators
  final private int limit;

  private int count = 0;
  private boolean limitReached = false;

  FsIterator_limited(FSIterator<T> iterator, int limit) {
    this.iterator = (LowLevelIterator<T>) iterator;
    this.limit = limit;
    this.limitReached = limit <= count;
  }

  private void maybeMakeInvalid() {
    if (count < 0 || count == limit) {
      limitReached = true;
    }
  }

  @Override
  public T getNvc() {
    maybeMakeInvalid();
    if (limitReached) {
      throw new NoSuchElementException();
    }
    T r = iterator.get(); // not getNvc because of above maybeMakeInvalid
    return r;
  }

  @Override
  public void moveToNextNvc() {
    maybeMakeInvalid();
    if (limitReached) {
      return;
    }
    iterator.moveToNext(); // not getNvc because of above maybeMakeInvalid
    count++;
  }

  @Override
  public void moveToPreviousNvc() {
    count--;
    maybeMakeInvalid();
    if (limitReached) {
      return;
    }
    iterator.moveToPrevious(); // not getNvc because of above maybeMakeInvalid
  }

  @Override
  public void moveToFirstNoReinit() {
    iterator.moveToFirstNoReinit();
    count = 0;
    this.limitReached = limit <= count;
  }

  @Override
  public void moveToLastNoReinit() {
    if (count >= 0 && limitReached && iterator.isValid()) {
      iterator.moveToPrevious();
      count--;
    } else {
      moveToFirstNoReinit();
    }

    while (count < limit - 1 && iterator.isValid()) {
      iterator.moveToNextNvc();
      if (!iterator.isValid()) {
        iterator.moveToLastNoReinit();
        break;
      } else {
        count++;
      }
    }
    this.limitReached = limit <= count;
  }

  @Override
  public void moveToNoReinit(FeatureStructure fs) {
    // If the iterator is not valid, then we make it valid by moving to the start
    if (!isValid()) {
      moveToFirstNoReinit();
    }

    // Now we seek... we cannot make proper use of the binary search here because we do not know
    // now many elements the binary search skips and whether we stay inside the limit. Hopefully,
    // the limit is not too big so that it might actually be faster to seek instead of doing the
    // binary search.
    Comparator<TOP> cmp = iterator.getComparator();
    boolean skippedMatch = false;
    boolean movedForward = false;
    while (isValid()) {
      T current = get();

      int c = cmp.compare((TOP) current, (TOP) fs);

      // We seek to the first match in index order. Because we move backwards, we need to skip over
      // matches in order to do so and then potentially back-up at the end of the move process.
      if (c == 0) {
        if (movedForward) {
          // We reached the first match by seeking forward, so we can stop here
          break;
        }

        moveToPreviousNvc();
        skippedMatch = true;
        continue;
      }

      if (skippedMatch) {
        break;
      }

      if (c < 0) {
        moveToNextNvc();
        movedForward = true;
        continue;
      }

      if (c > 0) {
        moveToPreviousNvc();
      }
    }

    if (skippedMatch) {
      if (!isValid()) {
        moveToFirstNoReinit();
      } else {
        moveToNextNvc();
      }
    }
  }

  // @Override
  // public void moveToExactNoReinit(FeatureStructure fs) {
  // iterator.moveToExactNoReinit(fs);
  // maybeMakeInvalid();
  // }

  @Override
  public FSIterator<T> copy() {
    FsIterator_limited<T> copy = new FsIterator_limited<>(iterator.copy(), limit);
    copy.count = count;
    copy.limitReached = limitReached;
    return copy;
  }

  @Override
  public boolean isValid() {
    maybeMakeInvalid();
    return !limitReached && iterator.isValid();
  }

  @Override
  public int ll_indexSizeMaybeNotCurrent() {
    return iterator.ll_indexSizeMaybeNotCurrent();
  }

  @Override
  public int ll_maxAnnotSpan() {
    return iterator.ll_maxAnnotSpan();
  }

  @Override
  public LowLevelIndex<T> ll_getIndex() {
    return iterator.ll_getIndex();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelIterator#isIndexesHaveBeenUpdated()
   */
  @Override
  public boolean isIndexesHaveBeenUpdated() {
    return iterator.isIndexesHaveBeenUpdated();
  }

  @Override
  public boolean maybeReinitIterator() {
    return iterator.maybeReinitIterator();
  }

  @Override
  public Comparator<TOP> getComparator() {
    return iterator.getComparator();
  }

  @Override
  public int size() {
    return Math.min(limit, iterator.size());
  }
}
