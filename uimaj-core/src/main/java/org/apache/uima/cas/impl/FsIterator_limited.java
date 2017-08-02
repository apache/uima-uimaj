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

import java.util.NoSuchElementException;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;

/**
 * Wraps FSIterator<T>, limits results to n gets
 */
class FsIterator_limited<T extends FeatureStructure>  
          implements LowLevelIterator<T> {
  
  final private LowLevelIterator<T> iterator; // not just for single-type iterators
  final private int limit;
  private int count = 0;
    
  FsIterator_limited(FSIterator<T> iterator, int limit) {
    this.iterator = (LowLevelIterator<T>) iterator;
    this.limit = limit;
  }

  private void maybeMakeInvalid() {
    if (count == limit) {
      iterator.moveToFirst();
      iterator.moveToPrevious();
    }
  }
  
  public T getNvc() {
    maybeMakeInvalid();
    T r = iterator.get();  // not getNvc because of above line
    count++;
    return r;
  }

  public void moveToNextNvc() {
    maybeMakeInvalid();
    iterator.moveToNext();   // not getNvc because of above line
  }

  public void moveToPreviousNvc() {
    maybeMakeInvalid();
    iterator.moveToPrevious();  // not getNvc because of above line
  }

  public void moveToFirstNoReinit() {
    iterator.moveToFirstNoReinit();
    maybeMakeInvalid();
  }

  public void moveToLastNoReinit() {
    iterator.moveToLastNoReinit();
    maybeMakeInvalid();
  }

  public void moveToNoReinit(FeatureStructure fs) {
    iterator.moveToNoReinit(fs);
    maybeMakeInvalid();
  }

  public FSIterator<T> copy() {
    return new FsIterator_limited<T>(iterator.copy(), limit);
  }

  public boolean isValid() {
    maybeMakeInvalid();
    return iterator.isValid();
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

  /* (non-Javadoc)
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

}
