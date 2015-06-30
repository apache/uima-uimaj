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

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.internal.util.IntComparator;
import org.apache.uima.internal.util.IntVector;

class IntIterator4sorted<T extends FeatureStructure> extends FSIntIteratorImplBase<T> {

  private int itPos;

  final private IntComparator comp;

  final private IntVector indexIntVector;
  final private FSIntArrayIndex<T> fsIntArrayIndex;  // just an optimization, is == to fsLeafIndexImpl from super class
  
  // used when iterating without ordering among iterators
  IntIterator4sorted(FSIntArrayIndex<T> index, int[] detectIllegalIndexUpdates) {
    super(index, detectIllegalIndexUpdates);
    this.indexIntVector = index.getVector();
    this.fsIntArrayIndex = index;
    this.itPos = 0;
    comp = null;
  }

  IntIterator4sorted(FSIntArrayIndex<T> index, int[] detectIllegalIndexUpdates, IntComparator comp) {
    super(index, detectIllegalIndexUpdates);
    this.fsIntArrayIndex = index;
    this.indexIntVector = index.getVector();
    this.comp = comp;
    this.itPos = 0;
  }

  @Override
  public boolean isValid() {
    return ((this.itPos >= 0) && (this.itPos < this.indexIntVector.size()));
  }

  @Override
  public void moveToFirst() {
    resetConcurrentModification();
    this.itPos = 0;
  }

  @Override
  public void moveToLast() {
    resetConcurrentModification();
    this.itPos = this.indexIntVector.size() - 1;
  }

  @Override
  public void moveToNext() {
    if (itPos < 0) {
      return;
    }
    checkConcurrentModification();
    ++this.itPos;
  }

  @Override
  public void moveToPrevious() {
    if (itPos >= this.indexIntVector.size()) {
      return;
    }
    checkConcurrentModification(); 
    --this.itPos;
  }

  @Override
  public int get() {
    if (!isValid()) {
      throw new NoSuchElementException();
    }
    checkConcurrentModification(); 
    return this.indexIntVector.get(this.itPos);
  }

  /**
   * @see org.apache.uima.internal.util.IntPointerIterator#copy()
   */
  @Override
  public Object copy() {
    IntIterator4sorted<T> copy = new IntIterator4sorted<T>(this.fsIntArrayIndex, this.detectIllegalIndexUpdates, this.comp);
    copy.itPos = this.itPos;
    return copy;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.FSIntIteratorImplBase#compareTo(org.apache.uima.cas.impl.FSIntIteratorImplBase)
   */
  @Override
  public int compareTo(FSIntIteratorImplBase<T> o) {
    return this.comp.compare(get(), ((IntIterator4sorted<T>) o).get());
  }

  /**
   * @see org.apache.uima.internal.util.IntPointerIterator#moveTo(int)
   */
  @Override
  public void moveTo(int i) {
    moveTo(i, false);
  }
  
  void moveTo(int i, boolean isExact) {
    resetConcurrentModification();
    final int pos = isExact ? fsIntArrayIndex.findEq(i) : fsIntArrayIndex.findLeftmost(i);
    if (pos >= 0) {
      itPos = pos;
    } else {
      if (isExact) {
        throw new UIMARuntimeException(); // internal error
      }
      itPos = -(pos + 1);
    }
//
//      
//      
//      final int position = find(i);
//      boolean found = false;
//      if (position >= 0) {
//        this.itPos = position;
//        found = true;
//      } else {  // not found
//        this.itPos = -(position + 1);
//      }
//      
//      // https://issues.apache.org/jira/browse/UIMA-4094
//      // make sure you go to earliest one
//      if (!found || !isValid()) {
//        // this means the moveTo found the insert point at the end of the index
//        // so just return invalid, since there's no way to return an insert point for a position
//        // that satisfies the FS at that position is greater than fs  
//        return;
//      }    
//      // Go back until we find a FS that is really smaller
//      while (true) {
//        moveToPrevious();
//        if (isValid()) {
//          int prev = get();
//          if (compare(prev, i) != 0) {
//            moveToNext(); // go back
//            break;
//          }
//        } else {
//          moveToFirst();  // went to before first, so go back to 1st
//          break;
//        }
//      }
  }



  @Override
  public int ll_indexSize() {
    return indexIntVector.size();
  }

}

