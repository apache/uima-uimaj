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

import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;

/**
 * Aggregate several FS iterators.  Simply iterates over one after the other
 * without any sorting or merging.
 * Used by getAllIndexedFS and FsIterator_subtypes_unordered
 * 
 * The iterators can be for single types or for types with subtypes.
 *   Exception: if the ll_index is accessed, it is presumed to be of type FsIndex_subtypes.
 * 
 * Doesn't do concurrent mod checking - that's done if wanted by the individual iterators
 * being aggregated over.  
 * This results in allowing a few concurrent modifications, when crossing from one iterator to another
 * in moveToNext/Previous (because those get translated to move to first/last, which reset concurrent modification)
 */
class FsIterator_aggregation_common<T extends FeatureStructure> 
          implements LowLevelIterator<T> {
  
  final private FSIterator<T>[] iterators; // not just for single-type iterators
  
  private int lastValidIndex;
  
  final private FSIndex<T> index;
  
  FsIterator_aggregation_common(FSIterator<T>[] iterators, FSIndex<T> index) {
    this.iterators = iterators;
    for (int i = iterators.length - 1; i >=0; i--) {
      this.iterators[i] = iterators[i].copy();
    }
    this.index = index;
    moveToFirst();
  }
  
  public T get() throws NoSuchElementException {
    if (!isValid()) {
      throw new NoSuchElementException();
    }
    return iterators[lastValidIndex].get();
  }
  
  public T getNvc() {
    return iterators[lastValidIndex].getNvc();
  }

  public boolean isValid() {
    return lastValidIndex >= 0 &&
           lastValidIndex < iterators.length &&
           iterators[lastValidIndex].isValid();
  }

  public void moveTo(FeatureStructure fs) {
    for (int i = 0, nbrIt = iterators.length; i < nbrIt; i++) {
      FSIterator<T> it = iterators[i];
      if (((LowLevelIterator<T>)it).ll_getIndex().contains(fs)) {
        lastValidIndex = i;
        it.moveTo(fs);
        return;
      }
    }
    moveToFirst();  // default if not found
  }

  public void moveToFirst() {
    for (int i = 0, nbrIt = iterators.length; i < nbrIt; i++) {
      FSIterator<T> it = iterators[i];
      it.moveToFirst();
      if (it.isValid()) {
        lastValidIndex = i;
        return;
      }
    }
    lastValidIndex = -1; // no valid index
  }

  public void moveToLast() {
    for (int i = iterators.length -1; i >= 0; i--) {
      FSIterator<T> it = iterators[i];
      it.moveToLast();
      if (it.isValid()) {
        lastValidIndex = i;
        return;
      }
    }
    lastValidIndex = -1; // no valid index
  }

  public void moveToNext() {
    // No point in going anywhere if iterator is not valid.
    if (!isValid()) {
      return;
    }
    
    FSIterator<T> it = iterators[lastValidIndex];
    it.moveToNextNvc();

    if (it.isValid()) {
      return;
    }
    
    final int nbrIt = iterators.length;
    for (int i = lastValidIndex + 1; i < nbrIt; i++) {
      it = iterators[i];
      it.moveToFirst();
      if (it.isValid()) {
        lastValidIndex = i;
        return;
      }
    }
    lastValidIndex = iterators.length;  // invalid position
  }
    
  public void moveToNextNvc() {
    FSIterator<T> it = iterators[lastValidIndex];
    it.moveToNextNvc();

    if (it.isValid()) {
      return;
    }
    
    final int nbrIt = iterators.length;
    for (int i = lastValidIndex + 1; i < nbrIt; i++) {
      it = iterators[i];
      it.moveToFirst();
      if (it.isValid()) {
        lastValidIndex = i;
        return;
      }
    }
    lastValidIndex = iterators.length;  // invalid position
  }

  public void moveToPrevious() {
    // No point in going anywhere if iterator is not valid.
    if (!isValid()) {
      return;
    }
    
    moveToPreviousNvc();
  }
  
  @Override
  public void moveToPreviousNvc() {
    
    FSIterator<T> it = iterators[lastValidIndex];
    it.moveToPreviousNvc();

    if (it.isValid()) {
      return;
    }
    
    for (int i = lastValidIndex - 1; i >=  0; i--) {
      it = iterators[i];
      it.moveToLast();
      if (it.isValid()) {
        lastValidIndex = i;
        return;
      }
    }
    lastValidIndex = -1;  // invalid position
  }

  public int ll_indexSize() {
    int sum = 0;
    for (int i = iterators.length - 1; i >=  0; i--) {
      FSIterator<T> it = iterators[i];
      sum += ((LowLevelIterator<T>)it).ll_indexSize();
    }
    return sum;
  }
  
  
  public int ll_maxAnnotSpan() {
    int span = -1;
    for (int i = iterators.length - 1; i >=  0; i--) {
      FSIterator<T> it = iterators[i];
      int x = ((LowLevelIterator<T>)it).ll_maxAnnotSpan();
      if (x > span) {
        span = x;
      }
    }
    return (span == -1) ? Integer.MAX_VALUE : span;
  };

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  @Override
  public FSIterator<T> copy() {
    FsIterator_aggregation_common<T> it = new FsIterator_aggregation_common<T>(iterators.clone(), index);
    for (int i = 0; i < iterators.length; i++) {
      it.iterators[i] = iterators[i].copy();
    }
    
    if (!isValid()) {
      it.moveToFirst();
      it.moveToPrevious();  // make it also invalid
    } else {
      T targetFs = get();
      it.moveTo(targetFs);  // moves to left-most match
      while (targetFs != it.get()) {
        it.moveToNext();
      }
    }
    return it;
  }


  @Override
  public LowLevelIndex<T> ll_getIndex() {
    return (LowLevelIndex<T>) index;
  }  
  
  @Override
  public String toString() {
    Type type = this.ll_getIndex().getType();
    StringBuilder sb = new StringBuilder(this.getClass().getSimpleName()).append(":").append(System.identityHashCode(this));
    sb.append(" over Type: ").append(type.getName()).append(":").append(((TypeImpl)type).getCode());
    sb.append(", size: ").append(this.ll_indexSize());
    return sb.toString();
  }

}
