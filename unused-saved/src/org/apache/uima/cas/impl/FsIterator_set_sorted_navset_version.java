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

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.internal.util.CopyOnWriteOrderedFsSet_array;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.cas.TOP;

/**
 * @param <T> the type of FSs being returned from the iterator, supplied by the calling context
 */
class FsIterator_set_sorted_navset_version<T extends FeatureStructure> extends FsIterator_singletype<T> {

  // We use TOP instead of T because the 
  // signature of getting a "matching" element limits the type to the declared type, and 
  // in UIMA we can use, say an Annotation instance as a moveTo arg, for a navSet of some subtype of Annotation.
  private NavigableSet<TOP> navSet;  // == fsSortIndex.getNavigableSet()
  
  final protected FsIndex_set_sorted<T> fsSetSortIndex;  // for copy-on-write management, ll_getIndex, backwards compatibility
  
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

  private Iterator<T> iterator; // changes according to direction, starting point, etc.
  
  FsIterator_set_sorted_navset_version(FsIndex_set_sorted<T> fsSetSortIndex, TypeImpl ti, Comparator<FeatureStructure> comp) {
    super(ti, comp);
    this.fsSetSortIndex = fsSetSortIndex;
    moveToFirst();
    Misc.internalError();
  }

  @Override
  public boolean isValid() {return isCurrentElementFromLastGet ? true : iterator.hasNext();}

  @Override
  public void moveToFirst() {
//    fsSetSortIndex.maybeProcessBulkAdds();
    this.navSet = (NavigableSet<TOP>) fsSetSortIndex.getNonNullCow();
    iterator = (Iterator<T>) navSet.iterator();  // in case iterator was reverse, etc.
    resetConcurrentModification(); // follow create of iterator, which, in turn, does any pending batch processing
    isGoingForward = true;
    isCurrentElementFromLastGet = false;
  }

  @Override
  public void moveToLast() {
//    fsSetSortIndex.maybeProcessBulkAdds();
    this.navSet = (NavigableSet<TOP>) fsSetSortIndex.getNonNullCow();
    iterator =  (Iterator<T>) navSet.descendingIterator();
    resetConcurrentModification(); // follow create of iterator, which, in turn, does any pending batch processing
    isGoingForward = false;
    isCurrentElementFromLastGet = false;
  }

  @Override
  public void moveToNext() {
    if (!isValid()) {
      return;
    }
    moveToNextNvc();
  }
  
  @Override
  public void moveToNextNvc() { 
    if (isGoingForward) {
      if (isCurrentElementFromLastGet) {
        isCurrentElementFromLastGet = false;
      } else {
        maybeTraceCowUsingCopy(fsSetSortIndex, (CopyOnWriteIndexPart) navSet);
        currentElement = iterator.next();
        // leave isCurrentElementFromLastGet false because we just moved to next, but haven't retrieved that value
      } 
    } else {
      //reverse direction
      if (!isCurrentElementFromLastGet) {
        maybeTraceCowUsingCopy(fsSetSortIndex, (CopyOnWriteIndexPart) navSet);
        currentElement = iterator.next();  // need current value to do reverse iterator starting point
      }
      assert(currentElement != null);
      iterator = (Iterator<T>) navSet.tailSet((TOP)currentElement, false).iterator();
      isGoingForward = true;
      isCurrentElementFromLastGet = false;
    }
  }


  @Override
  public void moveToPrevious() {
    if (!isValid()) {
      return;
    }
    
    moveToPreviousNvc();
  }
  
  @Override
  public void moveToPreviousNvc() {
    if (!isGoingForward) {
      if (isCurrentElementFromLastGet) {
        isCurrentElementFromLastGet = false;
      } else {
        maybeTraceCowUsingCopy(fsSetSortIndex, (CopyOnWriteIndexPart) navSet);
        currentElement = iterator.next();
        // leave isCurrentElementFromLastGet false
      } 
    } else {
      //reverse direction
      if (!isCurrentElementFromLastGet) {
        maybeTraceCowUsingCopy(fsSetSortIndex, (CopyOnWriteIndexPart) navSet);
        currentElement = iterator.next();  // need current value to do reverse iterator starting point
      }
      assert(currentElement != null);
      iterator = (Iterator<T>) navSet.headSet((TOP)currentElement, false).descendingIterator();
      isGoingForward = false;
      isCurrentElementFromLastGet = false;
    }  
  }

  @Override
  public T get() {
    if (!isValid()) {
      throw new NoSuchElementException();
    }
    if (!isCurrentElementFromLastGet) {      
      currentElement = iterator.next();
      isCurrentElementFromLastGet = true;
    }
    maybeTraceCowUsingCopy(fsSetSortIndex, (CopyOnWriteIndexPart) navSet); 
    return currentElement;
  }

  @Override
  public T getNvc() {
    if (!isCurrentElementFromLastGet) {
      currentElement = iterator.next();
      isCurrentElementFromLastGet = true;
    }
    maybeTraceCowUsingCopy(fsSetSortIndex, (CopyOnWriteIndexPart) navSet);
    return currentElement;
  }

  /**
   * @see org.apache.uima.internal.util.IntPointerIterator#copy()
   */
  @Override
  public FsIterator_set_sorted_navset_version<T> copy() {
    return new FsIterator_set_sorted_navset_version<T>(this.fsSetSortIndex, ti, this.comparator);
  }

  /**
   * move to the "left most" fs that is equal using the comparator
   *   - this means the one after a LT compare or the beginning.
   * reset isCurrentElementFromLastSet
   * set isGoingForward
   * @param fs the template FS indicating the position
   */
  @Override
  public void moveTo(FeatureStructure fsIn) {
    TOP fs = (TOP) fsIn;
    isGoingForward = true;
    isCurrentElementFromLastGet = false;
    currentElement = null;   
    this.navSet = (NavigableSet<TOP>) fsSetSortIndex.getNonNullCow();
//    fsSetSortIndex.maybeProcessBulkAdds();  // not needed, always done due to previous size() call when creating iterator    
    Iterator<T> it = (Iterator<T>) navSet.headSet(fs, false).descendingIterator();  // may have a bunch of equal (using withoutID compare) at end
    // last element in headSet is 1 before the one LE fs.
    //   define "target element" to be the found in the search
    //                           the last element in the headSet if was "inclusive" mode
    //     target element is LE fs including id compare
    //       not including ID compare: target element is LE fs, maybe more likely equal
    //     last element for "exclusive":
    //       target if target is LT,
    //       one before target if EQ 
    //   by including ID, sometimes last element may be EQ to target
   
    // if the 1st previous element doesn't exist, then start at the first element 
    if (!it.hasNext()) {
      moveToFirst();
      return;
    }
    
    // iterator is valid.  Move backwards until either hit the end or find element not equal
    TOP elementBefore = null;
    boolean comparedEqual = false;  // value is ignored, but needed for Java compile
    while (it.hasNext()) {
      comparedEqual = (0 == ((CopyOnWriteOrderedFsSet_array)navSet).comparatorWithoutID.compare(elementBefore = (TOP)it.next(), fs));
      if (!comparedEqual) {
        break;
      }
    }
           
    if (comparedEqual) { // then we ran off the end
      moveToFirst();
      return;
    }
    
    iterator = (Iterator<T>) navSet.tailSet(elementBefore, false).iterator();
    resetConcurrentModification(); // follow create of iterator, which, in turn, does any pending batch processing
    return;
  }
    
  @Override
  public int ll_indexSize() {
    return navSet.size();
  }
  

  @Override
  public int ll_maxAnnotSpan() {
    return fsSetSortIndex.isAnnotIdx 
        ?   fsSetSortIndex.ll_maxAnnotSpan()
        : Integer.MAX_VALUE;
  }

  @Override
  public LowLevelIndex<T> ll_getIndex() {
    return fsSetSortIndex;
  }
  
  @Override
  protected int getModificationCountFromIndex() {
    return ((CopyOnWriteOrderedFsSet_array)navSet).getModificationCount();
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#isIndexesHaveBeenUpdated()
   */
  @Override
  public boolean isIndexesHaveBeenUpdated() {
    return navSet != fsSetSortIndex.getCopyOnWriteIndexPart();
  }

}

