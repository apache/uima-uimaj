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

import org.apache.uima.cas.FeatureStructure;

class IntIterator4bag<T extends FeatureStructure> extends FSIntIteratorImplBase<T> {

  private int itPos;

  final private FSBagIndex<T> fsBagIndex; // just an optimization, is == to fsLeafIndexImpl from super class, allows dispatch w/o casting
    

  IntIterator4bag(FSBagIndex<T> fsBagIndex, int[] detectIllegalIndexUpdates) {
    super(fsBagIndex, detectIllegalIndexUpdates);
    this.fsBagIndex = fsBagIndex;
    moveToFirst();
  }

  @Override
  public boolean isValid() {
    return fsBagIndex.isValid(this.itPos);
  }

  /**
   * If empty, make position -1 (invalid)
   */
  @Override
  public void moveToFirst() {
    resetConcurrentModification();
    this.itPos = fsBagIndex.moveToFirst();
  }

  /**
   * If empty, make position -1 (invalid)
   */
  @Override
  public void moveToLast() {
    resetConcurrentModification();
    this.itPos = fsBagIndex.moveToLast();
  }

  @Override
  public void moveToNext() {
    checkConcurrentModification(); 
    this.itPos = fsBagIndex.moveToNext(itPos);
  }

  @Override
  public void moveToPrevious() {
    checkConcurrentModification(); 
    this.itPos = fsBagIndex.moveToPrevious(itPos);
  }

  @Override
  public int get() {
    if (!isValid()) {
      throw new NoSuchElementException();
    }
    checkConcurrentModification(); 
    return fsBagIndex.get(itPos);
  }

  /**
   * @see org.apache.uima.internal.util.IntPointerIterator#copy()
   */
  @Override
  public Object copy() {
    IntIterator4bag<T> copy = new IntIterator4bag<T>(this.fsBagIndex, this.detectIllegalIndexUpdates);
    copy.itPos = this.itPos;
    return copy;
  }

  /**
   * @see org.apache.uima.internal.util.IntPointerIterator#moveTo(int)
   */
  @Override
  public void moveTo(int i) {
    resetConcurrentModification();
    this.itPos = fsBagIndex.findLeftmost(i);
  }

  @Override
  public int ll_indexSize() {
    return fsBagIndex.size();
  }

}

