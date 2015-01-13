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

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Aggregate several LowLevelIterators.  Simply iterates over one after the other, no sorting or merging
 * of any kind occurs.  Intended for use in 
 * {@link FSIndexRepositoryImpl#ll_getAllIndexedFS(org.apache.uima.cas.Type)}.
 * 
 * <p>Note: this class does not support moveTo(FS), as it is not sorted.
 */
class LowLevelIteratorAggregate implements LowLevelIterator {

  // A list of iterators, unordered.
  private final LowLevelIterator[] iterators;
  
  // The offset of the current index.
  private int iteratorIndex = 0;
 
  public LowLevelIteratorAggregate(List<LowLevelIterator> c) {
    iterators = new LowLevelIterator[c.size()];
    for (int i = 0; i < c.size(); i++) {
      iterators[i] = c.get(i);
    }
    moveToFirst();
  }
  
  private LowLevelIteratorAggregate(LowLevelIterator[] c) {
    iterators = c;
    moveToFirst();
  }


  @Override
  public void moveToFirst() {
    // Go through the iterators, starting with the first one
    this.iteratorIndex = 0;
    while (this.iteratorIndex < this.iterators.length) {
      LowLevelIterator it = this.iterators[this.iteratorIndex];
      // Reset iterator to first position
      it.moveToFirst();
      // If the iterator is valid (i.e., non-empty), return...
      if (it.isValid()) {
        return;
      }
      // ...else try the next one
      ++this.iteratorIndex;
    }
    // If we get here, all iterators are empty.   
  }

  @Override
  public void moveToLast() {
    // See comments on moveToFirst()
    this.iteratorIndex = this.iterators.length - 1;
    while (this.iteratorIndex >= 0) {
      LowLevelIterator it = this.iterators[iteratorIndex];
      it.moveToLast();
      if (it.isValid()) {
        return;
      }
      --this.iteratorIndex;
    }
  }

  @Override
  public boolean isValid() {
    return (this.iteratorIndex < this.iterators.length);
  }

  @Override
  public int ll_get() throws NoSuchElementException {
    if (!isValid()) {
      throw new NoSuchElementException();
    }
    return this.iterators[this.iteratorIndex].ll_get();
  }

  @Override
  public void moveToNext() {
    // No point in going anywhere if iterator is not valid.
    if (!isValid()) {
      return;
    }
    // Grab current iterator and inc.
    LowLevelIterator current = this.iterators[iteratorIndex];
    current.moveToNext();
    // If we're ok with the current iterator, return.
    if (current.isValid()) {
      return;
    }
    ++this.iteratorIndex;
    while (this.iteratorIndex < this.iterators.length) {
      current = this.iterators[this.iteratorIndex];
      current.moveToFirst();
      if (current.isValid()) {
        return;
      }
      ++this.iteratorIndex;
    }
    // If we get here, the iterator is no longer valid, there are no more elements.
  }

  @Override
  public void moveToPrevious() {
    // No point in going anywhere if iterator is not valid.
    if (!isValid()) {
      return;
    }
    // Grab current iterator and dec.
    LowLevelIterator current = this.iterators[iteratorIndex];
    current.moveToPrevious();
    // If we're ok with the current iterator, return.
    if (current.isValid()) {
      return;
    }
    --this.iteratorIndex;
    while (this.iteratorIndex >= 0) {
      current = this.iterators[iteratorIndex];
      current.moveToLast();
      if (current.isValid()) {
        return;
      }
      --this.iteratorIndex;
    }
    // If we get here, the iterator is no longer valid, there are no more elements.  Set internal
    // counter to the invalid position.
    this.iteratorIndex = this.iterators.length;
  }

  @Override
  public void moveTo(int fsRef) {
    throw new UnsupportedOperationException("This operation is not supported on an aggregate ll_iterator.");
  }

  @Override
  public Object copy() {
   LowLevelIterator[] itCopies = new LowLevelIterator[this.iterators.length];
    for (int i = 0; i < this.iterators.length; i++) {
      itCopies[i] = (LowLevelIterator) iterators[i].copy();
    }
    LowLevelIteratorAggregate copy = new LowLevelIteratorAggregate(itCopies);
    copy.iteratorIndex = this.iteratorIndex;
    return copy;
  }

  @Override
  public int ll_indexSize() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public LowLevelIndex ll_getIndex() {
    // TODO Auto-generated method stub
    return null;
  }
  

}
