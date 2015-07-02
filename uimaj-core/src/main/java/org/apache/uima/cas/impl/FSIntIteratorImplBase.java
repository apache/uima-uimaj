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

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.internal.util.ComparableIntPointerIterator;

/**
 * Base class for int Iterators over indexes.
 * 
 * There are 3 styles of indexes, one for Sorted, one for Sets and one for Bag.
 *   There is a separate int iterator for each of these styles:
 *     IntIterator4bag
 *     IntIterator4sorted
 *     IntArrayRBTIterator
 *     
 *   There are also specialized int iterators:
 *     SnapshotPointerIterator - iterates over a one-time flat snapshot
 *     FlatIterator - iterates over a flattened array of Java Objects (not ints) - so this is
 *       excluded from this discussion because this class is only for int iterators.
 *    
 * This class is the superclass of the 3 standard int iterators, and the SnapshotPointerIterator.
 * 
 * It is an iterator for just one UIMA type (excludes subtypes).  Other wrappers handle combining
 * multiple of these kinds of iterators into one covering all the subtypes.
 *   
 */
public abstract class FSIntIteratorImplBase<T extends FeatureStructure> 
           implements ComparableIntPointerIterator<T>, LowLevelIterator {

  final private FSLeafIndexImpl<T> fsLeafIndexImpl;
  
  private int modificationSnapshot; // to catch illegal modifications

  /**
   * This is a ref to the shared value in the FSIndexRepositoryImpl
   * OR it may be null which means skip the checking (done for some internal routines
   * which know they are not updating the index, and assume no other thread is)
   */
  final protected int[] detectIllegalIndexUpdates; // shared copy with Index Repository

  final private int typeCode;
    
  public void checkConcurrentModification() {
    if ((null != this.detectIllegalIndexUpdates) && 
        (this.modificationSnapshot != this.detectIllegalIndexUpdates[this.typeCode])) {
      throw new ConcurrentModificationException();
    }
  }

  public void resetConcurrentModification() {
    this.modificationSnapshot = (null == this.detectIllegalIndexUpdates) ? 0 : this.detectIllegalIndexUpdates[this.typeCode];
  }

  /**
   * 
   * @param fsLeafIndexImpl the leaf index this iterator is over
   * @param detectIllegalIndexUpdates may be null
   */
  public FSIntIteratorImplBase(FSLeafIndexImpl<T> fsLeafIndexImpl, int[] detectIllegalIndexUpdates) {
    this.fsLeafIndexImpl = fsLeafIndexImpl;
    this.typeCode = (detectIllegalIndexUpdates == null) ? 0 : fsLeafIndexImpl.getTypeCode();
    this.detectIllegalIndexUpdates = detectIllegalIndexUpdates;
    resetConcurrentModification();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.internal.util.IntPointerIterator#inc()
   */
  @Override
  public void inc() {
    moveToNext(); 
  }

  /* (non-Javadoc)
   * @see org.apache.uima.internal.util.IntPointerIterator#dec()
   */
  @Override
  public void dec() {
    moveToPrevious();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#ll_getIndex()
   */
  @Override
  public LowLevelIndex ll_getIndex() {
    return fsLeafIndexImpl;
  }

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   * 
   * Only the sorted iterator(s) override this
   */
  @Override
  public int compareTo(FSIntIteratorImplBase<T> o) {
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#ll_get()
   */
  @Override
  public int ll_get() throws NoSuchElementException {
    return get();
  }
  
  FSLeafIndexImpl<T> getFSLeafIndexImpl() {
    return fsLeafIndexImpl;
  }
  
  void moveTo(int fs, boolean isExact) { 
    //default impl: ignore isExact for bag and set
    moveTo(fs);
  }
}
