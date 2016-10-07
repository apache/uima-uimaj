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
   * Return the size of the underlying index.
   * 
   * @return The size of the index.
   */
  int ll_indexSize();

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
   * an empty iterator
   */
  static final LowLevelIterator<FeatureStructure> FS_ITERATOR_LOW_LEVEL_EMPTY = new LowLevelIterator<FeatureStructure> () {
    @Override
    public boolean isValid() { return false; }
    @Override
    public FeatureStructure get() throws NoSuchElementException { throw new NoSuchElementException(); }
    @Override
    public FeatureStructure getNvc() { throw new NoSuchElementException(); }
    @Override
    public void moveTo(int i) {}
    @Override
    public void moveToFirst() {}
    @Override
    public void moveToLast() {}
    @Override
    public LowLevelIterator<FeatureStructure> copy() { return this; }
    @Override
    public void moveToNext() {}
    @Override
    public void moveToNextNvc() {}
    @Override
    public void moveToPrevious() {}
    @Override
    public void moveToPreviousNvc() {}
    @Override
    public void moveTo(FeatureStructure fs) {}
    @Override
    public int ll_indexSize() { return 0; }
    @Override
    public int ll_maxAnnotSpan() { return Integer.MAX_VALUE; }
    @Override
    public LowLevelIndex<FeatureStructure> ll_getIndex() { return null; }    
  };
}
