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
  
  public T get() throws NoSuchElementException {
    maybeMakeInvalid();
    T r = iterator.get();
    count++;  
    return r;
  }

  public T getNvc() {
    maybeMakeInvalid();
    T r = iterator.getNvc();
    count++;
    return r;
  }

  public void moveToNext() {
    maybeMakeInvalid();
    iterator.moveToNext();
  }

  public void moveToNextNvc() {
    maybeMakeInvalid();
    iterator.moveToNextNvc();
  }

  public void moveToPrevious() {
    maybeMakeInvalid();
    iterator.moveToPrevious();
  }

  public void moveToPreviousNvc() {
    maybeMakeInvalid();
    iterator.moveToPreviousNvc();
  }

  public void moveToFirst() {
    iterator.moveToFirst();
    maybeMakeInvalid();
  }

  public void moveToLast() {
    iterator.moveToLast();
    maybeMakeInvalid();
  }

  public void moveTo(FeatureStructure fs) {
    iterator.moveTo(fs);
    maybeMakeInvalid();
  }

  public FSIterator<T> copy() {
    return iterator.copy();
  }

  public boolean isValid() {
    maybeMakeInvalid();
    return iterator.isValid();
  }

  @Override
  public int ll_indexSize() {
    return iterator.ll_indexSize();
  }

  @Override
  public LowLevelIndex<T> ll_getIndex() {
    return iterator.ll_getIndex();
  }

}
