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
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;

import org.apache.uima.cas.FeatureStructure;

class FsIterator_set_sorted<T extends FeatureStructure> extends FsIterator_singletype<T> {

  // We use FeatureStructure instead of T because the 
  // signature of getting a "matching" element limits the type to the declared type, and 
  // in UIMA we can use, say an Annotation instance as a moveTo arg, for a navSet of some subtype of Annotation.
  final private NavigableSet<FeatureStructure> navSet;  // == fsSortIndex.getNavigableSet()
  
  final private FsIndex_set_sorted<T> fsSetSortIndex;
  
  private T currentElement;
  
  private boolean isGoingForward = true;
  
  /**
   * true if a next() set the the currentElement, and advanced position to next
   *
   * if true, then
   *   currentElement is the one that get should return, and position is already advanced
   * if false, then 
   *   get needs to do a next() to retrieve the now-currentElement, and advances position to next
   */
  private boolean isCurrentElementFromLastGet = false;

  private Iterator<T> iterator; 
  
  FsIterator_set_sorted(FsIndex_set_sorted<T> fsSetSortIndex, int[] detectIllegalIndexUpdates, int typeCode, Comparator<FeatureStructure> comp) {
    super(detectIllegalIndexUpdates, typeCode, comp);
    this.fsSetSortIndex = fsSetSortIndex;
    this.navSet = fsSetSortIndex.getNavigableSet();
    iterator = (Iterator<T>) navSet.iterator();  // can't use fsSortIndex.iterator - that recursively calls this
  }

  @Override
  public boolean isValid() {return isCurrentElementFromLastGet ? true : iterator.hasNext();}

  @Override
  public void moveToFirst() {
    resetConcurrentModification();
    iterator = (Iterator<T>) navSet.iterator();
    isGoingForward = true;
    isCurrentElementFromLastGet = false;
  }

  @Override
  public void moveToLast() {
    resetConcurrentModification();
    iterator =  (Iterator<T>) navSet.descendingIterator();
    isGoingForward = false;
    isCurrentElementFromLastGet = false;
  }

  @Override
  public void moveToNext() {
    if (!isValid()) {
      return;
    }
  
    checkConcurrentModification();
    if (isGoingForward) {
      if (isCurrentElementFromLastGet) {
        isCurrentElementFromLastGet = false;
      } else {
        currentElement = iterator.next();
        // leave isCurrentElementFromLastGet false because we just moved to next, but haven't retrieved that value
      } 
    } else {
      //reverse direction
      if (!isCurrentElementFromLastGet) {
        currentElement = iterator.next();  // need current value to do reverse iterator starting point
      }
      assert(currentElement != null);
      iterator = (Iterator<T>) navSet.tailSet(currentElement, false).iterator();
      isGoingForward = true;
      isCurrentElementFromLastGet = false;
    }
  }

  @Override
  public void moveToPrevious() {
    if (!isValid()) {
      return;
    }

    checkConcurrentModification();
    if (!isGoingForward) {
      if (isCurrentElementFromLastGet) {
        isCurrentElementFromLastGet = false;
      } else {
        currentElement = iterator.next();
        // leave isCurrentElementFromLastGet false
      } 
    } else {
      //reverse direction
      if (!isCurrentElementFromLastGet) {
        currentElement = iterator.next();  // need current value to do reverse iterator starting point
      }
      assert(currentElement != null);
      iterator = (Iterator<T>) navSet.headSet(currentElement, false).descendingIterator();
      isGoingForward = false;
      isCurrentElementFromLastGet = false;
    }  
  }

  @Override
  public T get() {
    if (!isValid()) {
      throw new NoSuchElementException();
    }
    checkConcurrentModification();
    if (!isCurrentElementFromLastGet) {
      currentElement = iterator.next();
      isCurrentElementFromLastGet = true;
    }
    return currentElement;
  }

  /**
   * @see org.apache.uima.internal.util.IntPointerIterator#copy()
   */
  @Override
  public FsIterator_set_sorted<T> copy() {
    return new FsIterator_set_sorted<T>(this.fsSetSortIndex, this.detectIllegalIndexUpdates, typeCode, this.comparator);
  }

  /**
   * move to the "left most" fs that is equal using the comparator
   *   - this means the one after a LT compare or the beginning.
   * reset isCurrentElementFromLastSet
   * set isGoingForward
   * @param fs the template FS indicating the position
   */
  @Override
  public void moveTo(FeatureStructure fs) {
    isGoingForward = true;
    isCurrentElementFromLastGet = false;
    currentElement = null;
    resetConcurrentModification();    
    Iterator<T> it = (Iterator<T>) navSet.headSet(fs, false).descendingIterator();

    // if the 1st previous element doesn't exist, then start at the first element 
    if (!it.hasNext()) {
      moveToFirst();
      return;
    }
    
    // it iterator is valid.  Move backwards until either hit the end or find element not equal
    T elementBefore = null;
    boolean comparedEqual = false;  // value is ignored, but needed for Java compile
    while (it.hasNext() && 
           (comparedEqual = (0 == comparator.compare(elementBefore = it.next(), fs))));     
    
    if (comparedEqual) { // then we ran off the end
      moveToFirst();
      return;
    }
    
    iterator = (Iterator<T>) navSet.tailSet(elementBefore, false).iterator();
    return;
  }
  
  @Override
  public int ll_indexSize() {
    return fsSetSortIndex.size();
  }
  
  @Override
  public LowLevelIndex<T> ll_getIndex() {
    return fsSetSortIndex;
  }
}

