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

import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;

import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;

public class FsIterator_subtypes_snapshot<T extends FeatureStructure> implements LowLevelIterator<T>, Comparator<FeatureStructure> {
  
//  final private FsIndex_flat<T> flatIndex;  // a newly created one, just for this iterator
  final private T[] snapshot;  // local for ref speed
  private int pos = 0; 
  final private boolean is_unordered;
  final private LowLevelIndex<T> indexForComparator;
    
  public FsIterator_subtypes_snapshot(FsIndex_flat<T> flatIndex) {
    this.indexForComparator = flatIndex;
    this.snapshot = (T[]) flatIndex.getFlatArray();
    this.is_unordered = flatIndex.getIndexingStrategy() != FSIndex.SORTED_INDEX;
  }
  
  public FsIterator_subtypes_snapshot(T[] snapshot, LowLevelIndex<T> index, boolean is_unordered) {
    this.indexForComparator = (LowLevelIndex<T>) index;
    this.snapshot = snapshot;
    this.is_unordered = is_unordered;
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#isValid()
   */
  @Override
  public boolean isValid() {
    return (0 <= pos) && (pos < snapshot.length);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#get()
   */
  @Override
  public T get() throws NoSuchElementException {
    if (isValid()) {
      return snapshot[pos];
    }
    throw new NoSuchElementException();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#get()
   */
  @Override
  public T getNvc() {
    return snapshot[pos];
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveToNext()
   */
  @Override
  public void moveToNext() {
    if (isValid()) {
      pos++;
    }
  }

  @Override
  public void moveToNextNvc() {
    pos++;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveToPrevious()
   */
  @Override
  public void moveToPrevious() {
    if (isValid()) {
      pos--;
    }
  }
  
  @Override
  public void moveToPreviousNvc() {
    pos--;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveToFirst()
   */
  @Override
  public void moveToFirst() {
    pos = 0;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveToLast()
   */
  @Override
  public void moveToLast() {
    pos = snapshot.length - 1;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveTo(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public void moveTo(FeatureStructure fs) {
    if (is_unordered) {
      int i = 0;
      while ((i < snapshot.length) && compare(snapshot[i],  fs) < 0) {
        i++;
      }
      pos = i;
    } else {
      int c = Arrays.binarySearch(snapshot, 0, snapshot.length, fs, this);
      if (c < 0) {
        // was not found, c is (-(insertion point) - 1)
        //    insertion point      c                pos
        //         0               -1                0
        //         1               -2                1
        //       size - 1       -(size - 1) - 1     size - 1
        //        size           -size - 1          size (invalid)
        pos = (-c) - 1;   
      } else {
        // found an equal.  need to move to leftmost
        c--;
        while ((c >= 0) && compare(snapshot[c],  fs) == 0) {
          c--;
        }
        pos = c + 1;
      }
    }
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  @Override
  public FSIterator<T> copy() {
    FsIterator_subtypes_snapshot<T> it = new FsIterator_subtypes_snapshot<T>(
        this.snapshot, (LowLevelIndex<T>) this.indexForComparator, this.is_unordered);
    it.pos = pos;
    return it;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#ll_indexSize()
   */
  @Override
  public int ll_indexSize() {
    return snapshot.length;
  }
  
  @Override
  public int ll_maxAnnotSpan() {
    return indexForComparator.ll_maxAnnotSpan();   
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#ll_getIndex()
   */
  @Override
  public LowLevelIndex<T> ll_getIndex() {
    return indexForComparator;
  }
  
  public int compare(FeatureStructure fs1, FeatureStructure fs2) {
    return (null == this.indexForComparator) 
        ? (fs1.equals(fs2) ? 0 : -1)
        : this.indexForComparator.compare(fs1, fs2);
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#isIndexesHaveBeenUpdated()
   */
  @Override
  public boolean isIndexesHaveBeenUpdated() {
    return false;
  }

}
