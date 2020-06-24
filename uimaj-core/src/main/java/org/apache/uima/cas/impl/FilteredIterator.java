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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.cas.TOP;

/**
 * Implements a filtered iterator.
 */
class FilteredIterator<T extends FeatureStructure> implements LowLevelIterator<T> {
//IC see: https://issues.apache.org/jira/browse/UIMA-5250

  // The base iterator.
  private LowLevelIterator<T> it;

  // The filter constraint.
  private FSMatchConstraint cons;

  // Private...
  private FilteredIterator() {
    super();
  }

  /**
   * Create a filtered iterator from a base iterator and a constraint.
   */
//IC see: https://issues.apache.org/jira/browse/UIMA-1444
  FilteredIterator(FSIterator<T> it, FSMatchConstraint cons) {
    this();
//IC see: https://issues.apache.org/jira/browse/UIMA-5250
    this.it = (LowLevelIterator<T>) it;
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
//IC see: https://issues.apache.org/jira/browse/UIMA-4392
//IC see: https://issues.apache.org/jira/browse/UIMA-4391
//IC see: https://issues.apache.org/jira/browse/UIMA-4393
    while (this.it.isValid() && !this.cons.match(this.it.get())) {
      this.it.moveToPrevious();
    }    
  }
  

  public void moveToFirstNoReinit() {
//IC see: https://issues.apache.org/jira/browse/UIMA-5250
    this.it.moveToFirstNoReinit();
    adjustForConstraintForward();
  }

  public void moveToLastNoReinit() {
    this.it.moveToLast();
    adjustForConstraintBackward();
  }

  public void moveToNextNvc() {
//IC see: https://issues.apache.org/jira/browse/UIMA-4674
    this.it.moveToNextNvc();
    adjustForConstraintForward();
  }

  public void moveToPreviousNvc() {
//IC see: https://issues.apache.org/jira/browse/UIMA-4674
    this.it.moveToPreviousNvc();
    adjustForConstraintBackward();
  }

  public T getNvc() {
//IC see: https://issues.apache.org/jira/browse/UIMA-4674
    return this.it.getNvc();
  }
  
  /**
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  public FilteredIterator<T> copy() {
//IC see: https://issues.apache.org/jira/browse/UIMA-5921
    return new FilteredIterator<>(this.it.copy(), this.cons);
  }

  /**
   * @see org.apache.uima.cas.FSIterator#moveTo(FeatureStructure)
   */
  public void moveToNoReinit(FeatureStructure fs) {
//IC see: https://issues.apache.org/jira/browse/UIMA-5250
    this.it.moveToNoReinit(fs);
    adjustForConstraintForward();
  }
  
//  @Override
//  public void moveToExactNoReinit(FeatureStructure fs) {
//    this.it.moveToExactNoReinit(fs);
//    adjustForConstraintForward();
//  }


  @Override
  public int ll_indexSizeMaybeNotCurrent() {
//IC see: https://issues.apache.org/jira/browse/UIMA-5833
//IC see: https://issues.apache.org/jira/browse/UIMA-5835
//IC see: https://issues.apache.org/jira/browse/UIMA-5834
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



//  /* (non-Javadoc)
//   * @see org.apache.uima.cas.impl.FSIteratorImplBase#moveTo(java.util.Comparator)
//   */
//  @Override
//  <TT extends AnnotationFS> void moveTo(int begin, int end) {
//    ((FSIterator_concurrentmod<T>)(this.it)).moveTo(begin, end);
//    adjustForConstraintForward();
//  }
}
