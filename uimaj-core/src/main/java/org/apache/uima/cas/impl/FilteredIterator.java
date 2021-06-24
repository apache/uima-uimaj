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

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.cas.TOP;

/**
 * Implements a filtered iterator.
 */
class FilteredIterator<T extends FeatureStructure> implements LowLevelIterator<T> {

  // The base iterator.
  private LowLevelIterator<T> it;

  // The filter constraint.
  private FSMatchConstraint cons;

  // Private...
  private FilteredIterator() {
  }

  /**
   * Create a filtered iterator from a base iterator and a constraint.
   */
  FilteredIterator(FSIterator<T> it, FSMatchConstraint cons) {
    this();
    this.it = (LowLevelIterator<T>) it;
    this.cons = cons;
    moveToFirst();
  }

  @Override
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

  @Override
  public void moveToFirstNoReinit() {
    this.it.moveToFirstNoReinit();
    adjustForConstraintForward();
  }

  @Override
  public void moveToLastNoReinit() {
    this.it.moveToLast();
    adjustForConstraintBackward();
  }

  @Override
  public void moveToNextNvc() {
    this.it.moveToNextNvc();
    adjustForConstraintForward();
  }

  @Override
  public void moveToPreviousNvc() {
    this.it.moveToPreviousNvc();
    adjustForConstraintBackward();
  }

  @Override
  public T getNvc() {
    return this.it.getNvc();
  }

  /**
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  @Override
  public FilteredIterator<T> copy() {
    return new FilteredIterator<>(this.it.copy(), this.cons);
  }

  /**
   * @see org.apache.uima.cas.FSIterator#moveTo(FeatureStructure)
   */
  @Override
  public void moveToNoReinit(FeatureStructure fs) {
    this.it.moveToNoReinit(fs);
    adjustForConstraintForward();
  }

  // @Override
  // public void moveToExactNoReinit(FeatureStructure fs) {
  // this.it.moveToExactNoReinit(fs);
  // adjustForConstraintForward();
  // }

  @Override
  public int ll_indexSizeMaybeNotCurrent() {
    return it.ll_indexSizeMaybeNotCurrent();
  }

  @Override
  public LowLevelIndex<T> ll_getIndex() {
    return it.ll_getIndex();
  }

  @Override
  public int ll_maxAnnotSpan() {
    return it.ll_maxAnnotSpan();
  }

  @Override
  public boolean isIndexesHaveBeenUpdated() {
    return it.isIndexesHaveBeenUpdated();
  }

  @Override
  public boolean maybeReinitIterator() {
    return this.it.maybeReinitIterator();
  }

  @Override
  public Comparator<TOP> getComparator() {
    return it.getComparator();
  }

  // /* (non-Javadoc)
  // * @see org.apache.uima.cas.impl.FSIteratorImplBase#moveTo(java.util.Comparator)
  // */
  // @Override
  // <TT extends AnnotationFS> void moveTo(int begin, int end) {
  // ((FSIterator_concurrentmod<T>)(this.it)).moveTo(begin, end);
  // adjustForConstraintForward();
  // }
}
