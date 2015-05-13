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
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * Implements a filtered iterator.
 */
class FilteredIterator<T extends FeatureStructure> extends FSIteratorImplBase<T> {

  // The base iterator.
  private FSIterator<T> it;

  // The filter constraint.
  private FSMatchConstraint cons;

  // Private...
  private FilteredIterator() {
    super();
  }

  /**
   * Create a filtered iterator from a base iterator and a constraint.
   */
  FilteredIterator(FSIterator<T> it, FSMatchConstraint cons) {
    this();
    this.it = it;
    this.cons = cons;
    moveToFirst();
  }

  public boolean isValid() {
    // We always make sure that the underlying iterator is either pointing
    // at an FS
    // that matches the constraint, or is not valid. Thus, for isValid(), we
    // can simply refer to the underlying iterator.
    return this.it.isValid();
  }
  
  private void adjustForConstraintForward() {
    // If the iterator is valid, but doesn't match the constraint, advance.
    while (this.it.isValid() && !this.cons.match(this.it.get())) {
      this.it.moveToNext();
    }    
  }
  
  private void adjustForConstraintBackward() {
    // If the iterator is valid, but doesn't match the constraint, advance.
    while (this.it.isValid() && !this.cons.match(this.it.get())) {
      this.it.moveToPrevious();
    }    
  }
  

  public void moveToFirst() {
    this.it.moveToFirst();
    adjustForConstraintForward();
  }

  public void moveToLast() {
    this.it.moveToLast();
    adjustForConstraintBackward();
  }

  public void moveToNext() {
    this.it.moveToNext();
    adjustForConstraintForward();
  }

  public void moveToPrevious() {
    this.it.moveToPrevious();
    adjustForConstraintBackward();
  }

  public T get() throws NoSuchElementException {
    // This may throw an exception.
    return this.it.get();
  }

  /**
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  public FSIterator<T> copy() {
    return new FilteredIterator<T>(this.it.copy(), this.cons);
  }

  /**
   * @see org.apache.uima.cas.FSIterator#moveTo(FeatureStructure)
   */
  public void moveTo(FeatureStructure fs) {
    this.it.moveTo(fs);
    adjustForConstraintForward();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.FSIteratorImplBase#moveTo(java.util.Comparator)
   */
  @Override
  <TT extends AnnotationFS> void moveTo(int begin, int end) {
    ((FSIteratorImplBase<T>)(this.it)).moveTo(begin, end);
    adjustForConstraintForward();
  }
}
