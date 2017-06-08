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
import java.util.List;
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
  
  final private FSIterator<T>[] allIterators; // not just for single-type iterators
  private FSIterator<T>[] nonEmptyIterators; 
  private FSIterator<T>[] emptyIterators; 
  
  private int lastValidIndex;
  
  final private FSIndex<T> index; // not used here, but returned via the ll_getIndex api.
  
  FsIterator_aggregation_common(FSIterator<T>[] iterators, FSIndex<T> index) {
    this.allIterators = iterators;
      // can't see the reason for needing to copy the iterators
      // There's a separate call copy() to do that if needed
//    for (int i = iterators.length - 1; i >=0; i--) {
//      this.allIterators[i] = iterators[i].copy();
//    }
    
    separateIntoEmptyAndNonEmptyIterators();
    
    this.index = index;
    moveToStart();
  }
  
  private void separateIntoEmptyAndNonEmptyIterators() {
    List<FSIterator<T>> nonEmptyOnes = new ArrayList<>();
    List<FSIterator<T>> emptyOnes = new ArrayList<>();
    for (FSIterator<T> it : allIterators) {
      if (((LowLevelIterator<T>)it).ll_indexSize() == 0) {
        emptyOnes.add(it);
      } else {
        nonEmptyOnes.add(it);
      }
    }
    nonEmptyIterators = nonEmptyOnes.toArray(new FSIterator[nonEmptyOnes.size()]);
    emptyIterators = emptyOnes.toArray(new FSIterator[emptyOnes.size()]);
  }
  
  public T get() throws NoSuchElementException {
    if (!isValid()) {
      throw new NoSuchElementException();
    }
    return nonEmptyIterators[lastValidIndex].get();
  }
  
  public T getNvc() {
    return nonEmptyIterators[lastValidIndex].getNvc();
  }

  public boolean isValid() {
    return lastValidIndex >= 0 &&
           lastValidIndex < nonEmptyIterators.length &&
           nonEmptyIterators[lastValidIndex].isValid();
  }

  public void moveTo(FeatureStructure fs) {
    if (firstChangedEmptyIterator() >= 0) {
      separateIntoEmptyAndNonEmptyIterators();
    } 
    // don't need to check isIndexesHaveBeenUpdated because
    // individual aggregated iterators will do that
    
    for (int i = 0, nbrIt = nonEmptyIterators.length; i < nbrIt; i++) {
      FSIterator<T> it = nonEmptyIterators[i];
      if (((LowLevelIterator<T>)it).ll_getIndex().contains(fs)) {
        lastValidIndex = i;
        it.moveTo(fs);
        return;
      }
    }
    moveToStart();  // default if not found
  }

  public void moveToFirst() {
    if (firstChangedEmptyIterator() >= 0) {
      separateIntoEmptyAndNonEmptyIterators();
    }
    // don't need to check isIndexesHaveBeenUpdated because
    // individual aggregated iterators will do that

    moveToStart();
  }
  
  private void moveToStart() {
    for (int i = 0, nbrIt = nonEmptyIterators.length; i < nbrIt; i++) {
      FSIterator<T> it = nonEmptyIterators[i];
      it.moveToFirst();
      if (it.isValid()) {
        lastValidIndex = i;
        return;
      }
    }
    lastValidIndex = -1; // no valid index
    
  }

  public void moveToLast() {
    if (firstChangedEmptyIterator() >= 0) {
      separateIntoEmptyAndNonEmptyIterators();
    }
    // don't need to check isIndexesHaveBeenUpdated because
    // individual aggregated iterators will do that

    for (int i = nonEmptyIterators.length -1; i >= 0; i--) {
      FSIterator<T> it = nonEmptyIterators[i];
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
    
    FSIterator<T> it = nonEmptyIterators[lastValidIndex];
    it.moveToNextNvc();

    if (it.isValid()) {
      return;
    }
    
    final int nbrIt = nonEmptyIterators.length;
    for (int i = lastValidIndex + 1; i < nbrIt; i++) {
      it = nonEmptyIterators[i];
      it.moveToFirst();
      if (it.isValid()) {
        lastValidIndex = i;
        return;
      }
    }
    lastValidIndex = nonEmptyIterators.length;  // invalid position
  }
    
  public void moveToNextNvc() {
    FSIterator<T> it = nonEmptyIterators[lastValidIndex];
    it.moveToNextNvc();

    if (it.isValid()) {
      return;
    }
    
    final int nbrIt = nonEmptyIterators.length;
    for (int i = lastValidIndex + 1; i < nbrIt; i++) {
      it = nonEmptyIterators[i];
      it.moveToFirst();
      if (it.isValid()) {
        lastValidIndex = i;
        return;
      }
    }
    lastValidIndex = nonEmptyIterators.length;  // invalid position
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
    
    FSIterator<T> it = nonEmptyIterators[lastValidIndex];
    it.moveToPreviousNvc();

    if (it.isValid()) {
      return;
    }
    
    for (int i = lastValidIndex - 1; i >=  0; i--) {
      it = nonEmptyIterators[i];
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
    for (int i = nonEmptyIterators.length - 1; i >=  0; i--) {
      FSIterator<T> it = nonEmptyIterators[i];
      sum += ((LowLevelIterator<T>)it).ll_indexSize();
    }
    return sum;
  }
  
  
  public int ll_maxAnnotSpan() {
    int span = -1;
    for (int i = nonEmptyIterators.length - 1; i >=  0; i--) {
      FSIterator<T> it = nonEmptyIterators[i];
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
    final FSIterator<T>[] ai = allIterators.clone();
    for (int i = 0; i < ai.length; i++) {
      ai[i] = ai[i].copy();
    }
    
    FsIterator_aggregation_common<T> it = new FsIterator_aggregation_common<T>(ai, index);
    
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
    return (LowLevelIndex<T>)
             ((index != null)
                ? index 
                : ((LowLevelIterator)allIterators[0]).ll_getIndex());
  }  
  
  @Override
  public String toString() {
//    Type type = this.ll_getIndex().getType();
    StringBuilder sb = new StringBuilder(this.getClass().getSimpleName()).append(":").append(System.identityHashCode(this));
    
    if (nonEmptyIterators.length == 0) {
      sb.append(" empty iterator");
      return sb.toString();
    }
    
    sb.append( (index == null && nonEmptyIterators.length > 1) 
                 ? " over multiple Types: "
                 : " over type: ");
 
    if (index == null) {
      if (nonEmptyIterators.length > 1) {
        sb.append('[');
        for (FSIterator it : nonEmptyIterators) {
          Type type = ((LowLevelIterator<FeatureStructure>)it).ll_getIndex().getType(); 
          sb.append(type.getName()).append(':').append(((TypeImpl)type).getCode()).append(' ');
        }
        sb.append(']');
      } else {
        Type type = ((LowLevelIterator<FeatureStructure>)nonEmptyIterators[0]).ll_getIndex().getType(); 
        sb.append(type.getName()).append(':').append(((TypeImpl)type).getCode()).append(' ');
      }
    } else {
      Type type = index.getType(); 
      sb.append(type.getName()).append(':').append(((TypeImpl)type).getCode()).append(' ');
    }
    
    sb.append(", size: ").append(this.ll_indexSize());
    return sb.toString();
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#isIndexesHaveBeenUpdated()
   */
  @Override
  public boolean isIndexesHaveBeenUpdated() {
    for (FSIterator<T> it : allIterators) {
      if (((LowLevelIterator)it).isIndexesHaveBeenUpdated()) {
        return true;
      }
    }
    return false;
  }
 
  private int firstChangedEmptyIterator() {
    for (int i = 0; i < emptyIterators.length; i++) {
      FSIterator<T> it = emptyIterators[i];
      if (((LowLevelIterator<?>)it).isIndexesHaveBeenUpdated()) {
        return i;
      }
    }
    return -1;
  }

}
