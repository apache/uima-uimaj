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

import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;

/**
 * Class comment for FSListIteratorImpl.java goes here.
 * 
 * 
 */
class FSListIteratorImpl<T extends FeatureStructure> implements ListIterator<T> {

  // Keep two pointers: one for the next element, and one for the previous
  // one. The two pointers are always moved in lockstep. Watch for border
  // conditions where only one of the pointers is valid.
  private FSIterator<T> forward;

  private FSIterator<T> back;

  public FSListIteratorImpl(FSIterator<T> it) {
    super();
    this.forward = it;
    this.back = it.copy();
    it.moveToPrevious();
  }

  /**
   * @see ListIterator#hasNext()
   */
  public boolean hasNext() {
    return this.forward.isValid();
  }

  /**
   * @see ListIterator#hasPrevious()
   */
  public boolean hasPrevious() {
    return this.back.isValid();
  }

  /**
   * Move the iterator so that the next call to {@link #previous() previous()} will return the first
   * element in the index (for a non-empty index).
   */
  public void moveToEnd() {
    // Move the back pointer to point at the last element.
    this.back.moveToLast();
    // Move the forward pointer past the end.
    this.forward.moveToLast();
    this.forward.moveToNext();
  }

  /**
   * Move the iterator so that the next call to {@link #next() next()} will return the first element
   * in the index (for a non-empty index).
   */
  public void moveToStart() {
    // Move the forward pointer to the start.
    this.forward.moveToFirst();
    // Move the back pointer past the start.
    this.back.moveToFirst();
    this.back.moveToPrevious();
  }

  /**
   * @see ListIterator#next()
   */
  public T next() throws NoSuchElementException {
    // Throw exception if forward pointer is invalid.
    if (!this.forward.isValid()) {
      throw new NoSuchElementException();
    }
    // Move the forward pointer.
    this.forward.moveToNext();
    // Move the backward pointer. Need to check if it's valid.
    if (this.back.isValid()) {
      this.back.moveToNext();
    } else {
      // This is guaranteed to yield a valid pointer, since otherwise, the
      // forward pointer couldn't have been valid.
      this.back.moveToFirst();
    }
    // The back pointer is now pointing at the current forward element.
    return this.back.get();
  }

  /**
   * @see ListIterator#previous()
   */
  public T previous() throws NoSuchElementException {
    // See comments for next().
    if (!this.back.isValid()) {
      throw new NoSuchElementException();
    }
    this.back.moveToPrevious();
    if (this.forward.isValid()) {
      this.forward.moveToPrevious();
    } else {
      this.forward.moveToLast();
    }
    return this.forward.get();
  }

  /**
   * @see java.util.ListIterator#add(Object)
   */
  public void add(T o) {
    throw new UnsupportedOperationException();
  }

  /**
   * @see java.util.ListIterator#nextIndex()
   */
  public int nextIndex() {
    throw new UnsupportedOperationException();
  }

  /**
   * @see java.util.ListIterator#previousIndex()
   */
  public int previousIndex() {
    throw new UnsupportedOperationException();
  }

  /**
   * @see java.util.Iterator#remove()
   */
  public void remove() {
    throw new UnsupportedOperationException();
  }

  /**
   * @see java.util.ListIterator#set(T)
   */
  public void set(T o) {
    throw new UnsupportedOperationException();
  }

}
