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
 * Wraps FSIterator<T>, runs it backwards
 */
class FsIterator_backwards<T extends FeatureStructure>  
          implements LowLevelIterator<T> {
  
  final private LowLevelIterator<T> it; // not just for single-type iterators
    
  FsIterator_backwards(FSIterator<T> iterator) {
    this.it = (LowLevelIterator<T>) iterator;
    it.moveToLast();
  }

  @Override
  public int ll_indexSize() {
    return it.ll_indexSize();
  }

  @Override
  public int ll_maxAnnotSpan() {
    return it.ll_maxAnnotSpan();
  }
  
  @Override
  public LowLevelIndex<T> ll_getIndex() {
    return it.ll_getIndex();
  }

  @Override
  public boolean isValid() {
    return it.isValid();
  }

  @Override
  public T get() throws NoSuchElementException {
    return it.get();
  }

  @Override
  public T getNvc() {
    return it.getNvc();
  }

  @Override
  public void moveToNext() {
    it.moveToPrevious();
  }

  @Override
  public void moveToNextNvc() {
    it.moveToPreviousNvc();
  }

  @Override
  public void moveToPrevious() {
    it.moveToNext();
  }

  @Override
  public void moveToPreviousNvc() {
    it.moveToNextNvc();
  }

  @Override
  public void moveToFirst() {
    it.moveToLast();
  }

  @Override
  public void moveToLast() {
    it.moveToFirst();
  }

  @Override
  public void moveTo(FeatureStructure fs) {
    it.moveTo(fs);  // moves to left most of equal, or one greater
    LowLevelIndex<T> lli = ll_getIndex();
    if (isValid()) {
      if (lli.compare(get(), fs) == 0) {
        // move to right most
        while (true) {
          it.moveToNextNvc();
          if (!isValid() || lli.compare(get(), fs) != 0) {
            break;
          }
        };
        if (isValid()) {
          it.moveToPreviousNvc();
        } else {
          it.moveToLast();
        }
      } else {
        // is valid, but not equal - went to wrong side
        it.moveToPreviousNvc();
      }
    } else {
      // moved to one past the end.  Backwards: would be at the (backwards) first position
      it.moveToLast();
    }
  }

  @Override
  public FSIterator<T> copy() {
    return new FsIterator_backwards<T>(it.copy());
  }


}
