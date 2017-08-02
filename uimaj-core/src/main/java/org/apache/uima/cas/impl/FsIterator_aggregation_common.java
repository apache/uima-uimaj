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
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.cas.TOP;

/**
 * Aggregate several FS iterators.  Simply iterates over one after the other
 * without any sorting or merging.
 * Used by getAllIndexedFS and FsIterator_subtypes_unordered
 *   underlying iterators could be any (bag, set, or ordered
 *   underlying iterators could be complex (unambiguous annotation, filtered,...)
 * 
 * The iterators can be for single types or for types with subtypes.
 *   Exception: if the ll_index is accessed, it is presumed to be of type FsIndex_subtypes.
 */
class FsIterator_aggregation_common<T extends FeatureStructure> extends FsIterator_multiple_indexes<T> {
      
  final private FSIndex<T> index; // not used here, but returned via the ll_getIndex api.
  
  FsIterator_aggregation_common(LowLevelIterator<T>[] iterators, FSIndex<T> index) {
    super(iterators);
    this.index = index;
    moveToFirstNoReinit();
  }
  
  /** copy constructor */
  FsIterator_aggregation_common(FsIterator_aggregation_common v) {
    super(v);
    this.index = v.index;
  }
      
      
  /**
   * MoveTo for this kind of iterator
   * Happens for set or sorted indexes being operated without rattling
   * 
   */
  public void moveToNoReinit(FeatureStructure fs) {
    lastValidIteratorIndex = -1;
    LowLevelIndex<T> idx = ll_getIndex();
    Comparator<TOP> comparatorWithoutId = idx.getComparator();
    int i = -1;
    for (LowLevelIterator<T> it : nonEmptyIterators) {
      i++;
      it.moveTo(fs);
      if (it.isValid() && 0 == comparatorWithoutId.compare((TOP)it.getNvc(), (TOP) fs)) {
        lastValidIteratorIndex = i;
        return;
      }
    }
  }
  
  /** moves to the first non-empty iterator at its start position */
  public void moveToFirstNoReinit() {
    lastValidIteratorIndex = -1; // no valid index
    int i = -1;
    for (LowLevelIterator<T> it : nonEmptyIterators) {
      i++;
      it.moveToFirstNoReinit();
      if (it.isValid()) {
        lastValidIteratorIndex = i;
        return;
      }
    }        
  }

  public void moveToLastNoReinit() {
    lastValidIteratorIndex = -1; // no valid index
    for (int i = nonEmptyIterators.length -1; i >= 0; i--) {
      LowLevelIterator<T> it = nonEmptyIterators[i];
      it.moveToLastNoReinit();
      if (it.isValid()) {
        lastValidIteratorIndex = i;
        return;
      }
    }
    lastValidIteratorIndex = -1; // no valid index
  }

  public void moveToNextNvc() {
    FSIterator<T> it = nonEmptyIterators[lastValidIteratorIndex];
    it.moveToNextNvc();

    if (it.isValid()) {
      return;
    }
    
    final int nbrIt = nonEmptyIterators.length;
    for (int i = lastValidIteratorIndex + 1; i < nbrIt; i++) {
      it = nonEmptyIterators[i];
      it.moveToFirst();
      if (it.isValid()) {
        lastValidIteratorIndex = i;
        return;
      }
    }
    lastValidIteratorIndex = nonEmptyIterators.length;  // invalid position
  }
  
  @Override
  public void moveToPreviousNvc() {
    
    LowLevelIterator<T> it = nonEmptyIterators[lastValidIteratorIndex];
    it.moveToPreviousNvc();

    if (it.isValid()) {
      return;
    }
    
    for (int i = lastValidIteratorIndex - 1; i >=  0; i--) {
      it = nonEmptyIterators[i];
      it.moveToLastNoReinit();
      if (it.isValid()) {
        lastValidIteratorIndex = i;
        return;
      }
    }
    lastValidIteratorIndex = -1;  // invalid position
  }

  public int ll_indexSizeMaybeNotCurrent() {
    int sum = 0;
    for (int i = nonEmptyIterators.length - 1; i >=  0; i--) {
      FSIterator<T> it = nonEmptyIterators[i];
      sum += ((LowLevelIterator<T>)it).ll_indexSizeMaybeNotCurrent();
    }
    return sum;
  }
  
  
  public int ll_maxAnnotSpan() {
    throw Misc.internalError();  // should never be called, because this operation isn't useful
                                 // in unordered indexes
//    int span = -1;
//    for (int i = nonEmptyIterators.length - 1; i >=  0; i--) {
//      FSIterator<T> it = nonEmptyIterators[i];
//      int x = ((LowLevelIterator<T>)it).ll_maxAnnotSpan();
//      if (x > span) {
//        span = x;
//      }
//    }
//    return (span == -1) ? Integer.MAX_VALUE : span;
  };

  @Override
  public LowLevelIndex<T> ll_getIndex() {
    return (LowLevelIndex<T>)
             ((index != null)
                ? index 
                : ((LowLevelIterator<T>)allIterators[0]).ll_getIndex());
  }  
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  @Override
  public FsIterator_aggregation_common<T> copy() {
    return new FsIterator_aggregation_common<>(this);
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
    
    sb.append(", iterator size (may not match current index size): ").append(this.ll_indexSizeMaybeNotCurrent());
    return sb.toString();
  }
  
}
