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
import java.util.NoSuchElementException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.cas.TOP;

/**
 * Low-level FS iterator. Returns FS references, instead of FS objects.
 * 
 * @see org.apache.uima.cas.FSIterator
 * 
 */
public interface LowLevelIterator<T extends FeatureStructure> extends FSIterator<T> {

  /**
   * Return the current FS reference.
   * 
   * @return The current FS reference.
   * @exception NoSuchElementException
   *              Iff the iterator is not valid.
   */
  default int ll_get() throws NoSuchElementException {
    return get()._id();
  };

  /**
   * Try to position the iterator so that the current element is greater than or equal to
   * <code>fsRef</code>, and previous elements are less than <code>fsRef</code>. This may
   * invalidate the iterator. If fsRef can not be compared to FSs in the index, the results are
   * undefined.
   * 
   * @param fsRef
   *          The FS reference the iterator should be set to.
   */
  default void moveTo(int fsRef) {
    moveTo(ll_getIndex().getCasImpl().ll_getFSForRef(fsRef));
  }
  
  

  /**
   * @return The size of the index.  In case of copy-on-write, this returns the size of the
   *         index at the time the iterator was created, or at the last moveTo, moveToFirst, or moveToLast.
   *         To get the current index size, use ll_getIndex().getSize()
   */
  int ll_indexSizeMaybeNotCurrent();

  /**
   * Get the index for just the top most type of this iterator (excludes subtypes).
   * 
   * @return The index.
   */
  LowLevelIndex<T> ll_getIndex();
  
  /**
   * @return an estimate of the maximum span over all annotations (end - begin)
   */
  int ll_maxAnnotSpan();
  
  /**
   * @return true if one or more of the underlying indexes this iterator goes over, has been updated
   *   since initialization or resetting operation (moveToFirst/Last/feature_structure).
   *   This includes empty iterators becoming non-empty.
   */
  boolean isIndexesHaveBeenUpdated();
 
  /**
   * Internal use
   * @return true if the iterator was refreshed to match the current index
   */
  boolean maybeReinitIterator();
 
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveToFirst()
   */
  @Override
  default void moveToFirst() {
    maybeReinitIterator();
    moveToFirstNoReinit();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveToLast()
   */
  @Override
  default void moveToLast() {
    maybeReinitIterator();
    moveToLastNoReinit();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveTo(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  default void moveTo(FeatureStructure fs) {
    maybeReinitIterator();
    moveToNoReinit(fs);
  }
    

  /**
   * Internal use
   * same as moveToFirst, but won't reset to use current contents of index if index has changed
   */
  void moveToFirstNoReinit();
  
  /**
   * Internal use
   * same as moveToLast, but won't reset to use current contents of index if index has changed
   */
  void moveToLastNoReinit();

  /**
   * Internal use
   * same as moveTo(fs), but won't reset to use current contents of index if index has changed
   * @param fs the fs to use as the template identifying the place to move to
   */
  void moveToNoReinit(FeatureStructure fs);
  
  /**
   * @return the comparator used by this iterator.  It is always a withoutID style, and may be
   *         either a withType or NoType style.  
   */
  Comparator<TOP> getComparator();
    
  default void ll_remove() {
    LowLevelIndex<T> idx = ll_getIndex();
    if (null == idx) {
      UIMAFramework.getLogger().warn("remove called on UIMA iterator but iterator not over any index");
    } else {
      idx.getCasImpl().removeFsFromIndexes(get());
    }
  }


  /**
   * an empty iterator
   */
  static final LowLevelIterator<FeatureStructure> FS_ITERATOR_LOW_LEVEL_EMPTY = new LowLevelIterator_empty<>();
  
  /**
   * Internal use constants
   */
  static final boolean IS_ORDERED = false;
  
  /**
   * @return false if this iterator is over an unordered collection or set or bag
   */
  default boolean isMoveToSupported() { return false; }

  /**
   * @param arrayList updated by adding elements representing the collection of items the iterator would return
   * from its current position to the end
   * 
   * NOTE: This operation will move the iterator from its current position to the end.
   */
  default void getArrayList(ArrayList<? super T> arrayList) {
    while (isValid()) {
      arrayList.add(nextNvc());
    }
  }

}
