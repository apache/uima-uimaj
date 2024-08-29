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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.internal.util.CopyOnWriteObjHashSet;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.cas.TOP;

class FsIterator_bag<T extends FeatureStructure> extends FsIterator_singletype<T> {

  private static final AtomicInteger moveToCount = new AtomicInteger(0);

  protected CopyOnWriteObjHashSet<T> bag;
  protected final FsIndex_bag<T> fsBagIndex; // just an optimization, is == to fsLeafIndexImpl from
                                             // super class, allows dispatch w/o casting

  private int position = -1;

  private boolean isGoingForward = true;

  FsIterator_bag(FsIndex_bag<T> fsBagIndex, TypeImpl ti, CopyOnWriteIndexPart<T> cow_wrapper) {
    super(ti);
    this.fsBagIndex = fsBagIndex; // need for copy()
    bag = (CopyOnWriteObjHashSet<T>) cow_wrapper;
    moveToFirst();
  }

  @Override
  public boolean maybeReinitIterator() {
    if (!bag.isOriginal()) {
      bag = (CopyOnWriteObjHashSet<T>) fsBagIndex.getNonNullCow();
      return true;
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#isValid()
   */
  @Override
  public boolean isValid() {
    return (position >= 0) && (position < bag.getCapacity());
  }

  @Override
  public T getNvc() {
    // checkConcurrentModification();
    return bag.get(position);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveToFirst()
   */
  @Override
  public void moveToFirstNoReinit() {
    // resetConcurrentModification();
    isGoingForward = true;
    position = (bag.size() == 0) ? -1 : bag.moveToNextFilled(0);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveToLast() If empty, make position -1 (invalid)
   */
  @Override
  public void moveToLastNoReinit() {
    // resetConcurrentModification();
    isGoingForward = false;
    position = (bag.size() == 0) ? -1 : bag.moveToPreviousFilled(bag.getCapacity() - 1);
  }

  @Override
  public void moveToNextNvc() {
    // checkConcurrentModification();
    isGoingForward = true;
    position = bag.moveToNextFilled(++position);
  }

  @Override
  public void moveToPreviousNvc() {
    // checkConcurrentModification();
    isGoingForward = false;
    position = bag.moveToPreviousFilled(--position);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveTo(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public void moveToNoReinit(FeatureStructure fs) {
    // throw new UnsupportedOperationException("MoveTo operations for unordered iterators is not
    // supported");
    Misc.decreasingWithTrace(moveToCount,
            "MoveTo operations on iterators over Bag indexes are likely mistakes.",
            UIMAFramework.getLogger());
    // resetConcurrentModification();
    // for backwards compatibility
    position = bag.moveTo(fs);
    if (position >= 0) {
      if (getNvc() == null) {
        position = -1; // mark invalid
      }
    }
  }

  // public void moveToExactNoReinit(FeatureStructure fs) {
  // position = bag.moveTo(fs);
  // }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  @Override
  public FsIterator_bag<T> copy() {
    FsIterator_bag<T> copy = new FsIterator_bag<>(fsBagIndex, ti, bag);
    copyCommonSetup(copy);
    return copy;
  }

  protected void copyCommonSetup(FsIterator_bag<T> copy) {
    copy.position = position;
    copy.isGoingForward = isGoingForward;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelIterator#ll_indexSize()
   */
  @Override
  public int ll_indexSizeMaybeNotCurrent() {
    return bag.size();
  }

  @Override
  public int ll_maxAnnotSpan() {
    return Integer.MAX_VALUE;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelIterator#ll_getIndex()
   */
  @Override
  public LowLevelIndex<T> ll_getIndex() {
    return fsBagIndex;
  }

  // /* (non-Javadoc)
  // * @see org.apache.uima.cas.impl.FsIterator_singletype#getModificationCountFromIndex()
  // */
  // @Override
  // protected int getModificationCountFromIndex() {
  // return bag.getModificationCount();
  // }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelIterator#isIndexesHaveBeenUpdated()
   */
  @Override
  public boolean isIndexesHaveBeenUpdated() {
    return bag != fsBagIndex.getCopyOnWriteIndexPart();
  }

  @Override
  public Comparator<TOP> getComparator() {
    return null; // not used for bag
  }

  @Override
  public int size() {
    return bag.size();
  }

}
