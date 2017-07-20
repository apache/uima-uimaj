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

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.internal.util.CopyOnWriteOrderedFsSet_array;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.OrderedFsSet_array;
import org.apache.uima.jcas.cas.TOP;

/**
 * NOTE: This is the version used for set/sorted iterators
 *   It is built directly on top of a CopyOnWrite wrapper for OrderedFsSet_array
 *   It uses the version of OrdereFsSet_array that has no embedded nulls
 * @param <T> the type of FSs being returned from the iterator, supplied by the calling context
 */
class FsIterator_set_sorted2<T extends FeatureStructure> extends FsIterator_singletype<T> implements LowLevelIterator<T> {

  // not final, because on moveToFirst/Last/FS, the semantics dictate that 
  // if the underlying index was updated, this should iterate over that.
  protected CopyOnWriteOrderedFsSet_array ofsa;  // orderedFsSet_array;
  
  private int pos;

  protected final FsIndex_set_sorted<T> ll_index;
  
  private final Comparator<TOP> comparatorWithoutID;
  
  public FsIterator_set_sorted2(FsIndex_set_sorted<T> ll_index, CopyOnWriteIndexPart cow_wrapper) {
    super((TypeImpl)ll_index.getType(), 
          ((CopyOnWriteOrderedFsSet_array)cow_wrapper).comparatorWithoutID);
    this.comparatorWithoutID = ((CopyOnWriteOrderedFsSet_array)cow_wrapper).comparatorWithoutID;
    
    this.ll_index = ll_index;
    ofsa = (CopyOnWriteOrderedFsSet_array) cow_wrapper;
    pos = ofsa.a_firstUsedslot;
//    incrToSkipOverNulls(); 
//    if (MEASURE) {
//      int s = ofsa.a_nextFreeslot - ofsa.a_firstUsedslot;
//      iterPctEmptySkip[(s - ofsa.size()) * 10 / s] ++;
//    }   
  }
  
  public boolean maybeReinitIterator() {
    if (!ofsa.isOriginal()) {
      // can't share this copy with other iterators - they may have not done a moveToFirst, etc.
      //   and need to continue with the previous view
      ofsa = (CopyOnWriteOrderedFsSet_array) ll_index.getNonNullCow();
      return true;
    }
    return false;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#isValid()
   */
  @Override
  public boolean isValid() {
    return pos >= ofsa.a_firstUsedslot && pos < ofsa.a_nextFreeslot;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#getNvc()
   */
  @Override
  public T getNvc() {
    return (T) ofsa.a[pos];
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveToNextNvc()
   */
  @Override
  public void moveToNextNvc() {
    pos++;
//    incrToSkipOverNulls();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveToPreviousNvc()
   */
  @Override
  public void moveToPreviousNvc() {
    pos--;
//    decrToSkipOverNulls();
  }
  
  // Internal use
  public void moveToFirstNoReinit() {
    pos = ofsa.a_firstUsedslot;
  }
  
  // Internal use
  public void moveToLastNoReinit() {
    pos = ofsa.a_nextFreeslot - 1;       
  }
  
  // Internal use
  public void moveToNoReinit(FeatureStructure fs) {
    pos = ofsa.getOfsa().findWithoutID((TOP) fs); 

    if (pos < 0) {
      pos = (-pos) -1;  // insertion point, one above
      return;
    }
    
    int savedPos = pos;
    
    // pos is the equal-with-id item

    moveToPreviousNvc();
    if (isValid()) {
      if (0 == comparatorWithoutID.compare((TOP)get(), (TOP) fs)) {
        moveToLeftMost(fs);
      } else {
        // did not compare equal, so previous was the right position
        pos = savedPos;
      }
    } else {
      // went one before start, restore to start
      pos = savedPos;
    }
    return;   
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  @Override
  public FsIterator_singletype<T> copy() {
    FsIterator_set_sorted2<T> r = new FsIterator_set_sorted2<>(ll_index, ofsa);
    r.pos = pos;
    return r;
  }

//  /* (non-Javadoc)
//   * @see org.apache.uima.cas.FSIterator#getType()
//   */
//  @Override
//  public Type getType() {
//    // TODO Auto-generated method stub
//    return LowLevelIterator.super.getType();
//  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#ll_indexSize()
   */
  @Override
  public int ll_indexSize() {
    return ofsa.size();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#ll_getIndex()
   */
  @Override
  public LowLevelIndex<T> ll_getIndex() {
    return ll_index;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#ll_maxAnnotSpan()
   */
  @Override
  public int ll_maxAnnotSpan() {
    FsIndex_set_sorted<T> ss_idx = (FsIndex_set_sorted<T>)ll_index;
    return ss_idx.isAnnotIdx 
        ? ss_idx.ll_maxAnnotSpan()
        : Integer.MAX_VALUE;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#isIndexesHaveBeenUpdated()
   * This is local to this class because it references the ofsa
   */
  @Override
  public boolean isIndexesHaveBeenUpdated() {
    return ofsa != ll_index.getCopyOnWriteIndexPart();
  }
      
//  private void incrToSkipOverNulls() {
//    while (pos < ofsa.a_nextFreeslot) {
//      if (ofsa.a[pos] != null) {
//        break;
//      }
//      pos ++;
//    }
//  }
//
//  private void decrToSkipOverNulls() {
//    while (pos >= ofsa.a_firstUsedslot) {
//      if (ofsa.a[pos] != null) {
//        break;
//      }
//      pos --;
//    }
//  }
  
  /**
   * Starting at a position where the item is equal to fs
   * using the compare without id, 
   * move to the leftmost one
   * 
   * search opportunistically, starting at 1 before, 2, 4, 8, 16, etc.
   * then doing binary search in the opposite dir
   * 
   * These methods are in this class because they manipulate "pos"
   * @param fs -
   */
  private void moveToLeftMost(FeatureStructure fs) {
    
    // adjust to move to left-most equal item
    boolean comparedEqual = false;
    int origPos = pos;
    int span = 1;
    while (isValid()) {
      int upperValidPos = pos;
      pos = origPos - span;
      pos = Math.max(-1,  pos);
//      decrToSkipOverNulls();
      if (!isValid()) {
        moveToLeftMostUp(fs, upperValidPos);
        return;
      }
      comparedEqual = (0 == comparatorWithoutID.compare((TOP)get(), (TOP) fs));
      if (!comparedEqual) {
        moveToLeftMostUp(fs, upperValidPos);
        return;
      }
      span = span << 1;
    }
  }
  
  /**
   * Must be possible to leave the pos == to upperValidPos.
   * Starts searching from next above current pos
   * @param fs
   * @param upperValidPos
   */
  private void moveToLeftMostUp(FeatureStructure fs, int upperValidPos) {
    if (pos < ofsa.a_firstUsedslot) {
      moveToFirst();
    } else {
      moveToNext();
    }
    // binary search between pos (inclusive) and upperValidPos (exclusive)
    if (!isValid()) {
      Misc.internalError();
    }
    if (pos == upperValidPos) {
      return;
    }
    pos = ofsa.getOfsa().binarySearchLeftMostEqual((TOP) fs, pos, upperValidPos);
  }

//  @Override
//  protected int getModificationCountFromIndex() {
//    return ofsa.getModificationCount();
//  }
  
//  /**
//   * Never returns an index to a "null" (deleted) item.
//   * If all items are LT key, returns - size - 1 
//   * @param fs the key
//   * @return the lowest position whose item is equal to or greater than fs;
//   *         if not equal, the item's position is returned as -insertionPoint - 1. 
//   *         If the key is greater than all elements, return -size - 1). 
//   */
//  private int find(TOP fs) {
//    return ofsa.getOfsa().find(fs);
//  }
  
  /**
   * @param fs - the fs to search for
   * @param start the index representing the lower bound (inclusive) to search for
   * @param end the index representing the upper bound (exclusive) to search for
   * @return - the index of the leftmost equal (without id) item
   */
  private int binarySearchLeftMostEqual(final TOP fs, int start, int end) {

//    assert start >= 0;    
//    assert start < end;
    
    int lower = start, upper = end;
    for (;;) {
    
      int mid = (lower + upper) >>> 1;  // overflow aware
      TOP item = ofsa.a[mid];
      int pos = mid;
         
      int c = comparatorWithoutID.compare(item, fs);
      if (c == 0) {
        upper = pos;  // upper is exclusive
        if (upper == lower) {
          return upper;
        }
      } else {  // item is less than fs; search upwards
        lower = pos + 1;             // lower is inclusive
        if (lower == upper) {
          return upper;
        }
      }
    }
  }


}

