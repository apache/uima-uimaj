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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * Aggregate several FS iterators.  Simply iterates over one after the other, no sorting or merging
 * of any kind occurs.  Intended for use in 
 * {@link FSIndexRepositoryImpl#getAllIndexedFS(org.apache.uima.cas.Type)}.
 * 
 * <p>Note: this class does not support moveTo(FS), as it is not sorted.
 */
class FSIteratorAggregate<T extends FeatureStructure> extends FSIteratorImplBase<T> {
  
  // Internal contract for this class is that isValid() iff the current iterator isValid().

  // A list of iterators, unordered.
  private final List<FSIteratorImplBase<T>> iterators;
  
  // The offset of the current index.
  private int iteratorIndex = 0;
  
  /**
   * The one and only constructor.
   * @param c Collection of input iterators.
   */
  public FSIteratorAggregate(Collection<FSIteratorImplBase<T>> c) {
    super();
    this.iterators = new ArrayList<FSIteratorImplBase<T>>();
    this.iterators.addAll(c);
    // The unwritten contract of FSIterators is that they point to the first element when they are
    // constructed.  This is also one way of checking that the new iterator is valid (non-empty).
    moveToFirst();
  }

  public FSIterator<T> copy() {
    ArrayList<FSIteratorImplBase<T>> itCopies = new ArrayList<FSIteratorImplBase<T>>(this.iterators.size());
    for (int i = 0; i < this.iterators.size(); i++) {
      itCopies.add((FSIteratorImplBase<T>) this.iterators.get(i).copy());
    }
    FSIteratorAggregate<T> copy = new FSIteratorAggregate<T>(itCopies);
    copy.iteratorIndex = this.iteratorIndex;
    return copy;
  }

  public T get() throws NoSuchElementException {
    if (!isValid()) {
      throw new NoSuchElementException();
    }
    return this.iterators.get(this.iteratorIndex).get();
  }

  public boolean isValid() {
    return (this.iteratorIndex < this.iterators.size());
  }

  public void moveTo(FeatureStructure fs) {
    throw new UnsupportedOperationException("This operation is not supported on an aggregate iterator.");
  }

  public void moveToFirst() {
    // Go through the iterators, starting with the first one
    this.iteratorIndex = 0;
    while (this.iteratorIndex < this.iterators.size()) {
      FSIterator<T> it = this.iterators.get(this.iteratorIndex);
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

  public void moveToLast() {
    // See comments on moveToFirst()
    this.iteratorIndex = this.iterators.size() - 1;
    while (this.iteratorIndex >= 0) {
      FSIterator<T> it = this.iterators.get(this.iteratorIndex);
      it.moveToLast();
      if (it.isValid()) {
        return;
      }
      --this.iteratorIndex;
    }
  }

  public void moveToNext() {
    // No point in going anywhere if iterator is not valid.
    if (!isValid()) {
      return;
    }
    // Grab current iterator and inc.
    FSIterator<T> current = this.iterators.get(this.iteratorIndex);
    current.moveToNext();
    // If we're ok with the current iterator, return.
    if (current.isValid()) {
      return;
    }
    ++this.iteratorIndex;
    while (this.iteratorIndex < this.iterators.size()) {
      current = this.iterators.get(this.iteratorIndex);
      current.moveToFirst();
      if (current.isValid()) {
        return;
      }
      ++this.iteratorIndex;
    }
    // If we get here, the iterator is no longer valid, there are no more elements.
  }

  public void moveToPrevious() {
    // No point in going anywhere if iterator is not valid.
    if (!isValid()) {
      return;
    }
    // Grab current iterator and dec.
    FSIterator<T> current = this.iterators.get(this.iteratorIndex);
    current.moveToPrevious();
    // If we're ok with the current iterator, return.
    if (current.isValid()) {
      return;
    }
    --this.iteratorIndex;
    while (this.iteratorIndex >= 0) {
      current = this.iterators.get(this.iteratorIndex);
      current.moveToLast();
      if (current.isValid()) {
        return;
      }
      --this.iteratorIndex;
    }
    // If we get here, the iterator is no longer valid, there are no more elements.  Set internal
    // counter to the invalid position.
    this.iteratorIndex = this.iterators.size();
  }

  @Override
  int getBegin() {
    return this.iterators.get(this.iteratorIndex).getBegin();
  }

  @Override
  int getEnd() {
    // TODO Auto-generated method stub
    return super.getEnd();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.FSIteratorImplBase#moveTo(java.util.Comparator)
   */
  @Override
  <TT extends AnnotationFS> void moveTo(int begin, int end) {
    throw new UnsupportedOperationException();
  }

  
}
