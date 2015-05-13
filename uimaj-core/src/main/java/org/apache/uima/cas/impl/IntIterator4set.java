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
import org.apache.uima.internal.util.IntComparator;
import org.apache.uima.internal.util.rb_trees.IntArrayRBT;

// Internal use only
public class IntIterator4set<T extends FeatureStructure> extends FSIntIteratorImplBase<T> {
  private static final int NIL = 0;
  
  private int currentNode;
  
  final private IntComparator comp; 

  final private FSRBTSetIndex<T> fsSetIndex; // just an optimization, is == to fsLeafIndexImpl from super class  
  
  final private IntArrayRBT intArrayRBTindex;
  
  // Internal Use Only
  public IntIterator4set(FSRBTSetIndex<T> fsSetIndex, int[] detectIllegalIndexUpdates, IntComparator comp) {
    super(fsSetIndex, detectIllegalIndexUpdates);
    this.fsSetIndex = fsSetIndex;
    intArrayRBTindex = fsSetIndex.tree;
    this.comp = comp;
    moveToFirst();
  }

  @Override
  public boolean isValid() {
    return this.currentNode != NIL;
  }

  /**
   * If empty, make position -1 (invalid)
   */
  @Override
  public void moveToFirst() {
    resetConcurrentModification();
    this.currentNode = intArrayRBTindex.getFirstNode();
  }

  /**
   * If empty, make position -1 (invalid)
   */
  @Override
  public void moveToLast() {
    resetConcurrentModification();
    this.currentNode = intArrayRBTindex.greatestNode;
  }

  @Override
  public void moveToNext() {
    checkConcurrentModification();
    this.currentNode = intArrayRBTindex.nextNode(this.currentNode);
  }

  @Override
  public void moveToPrevious() {
    checkConcurrentModification(); 
    this.currentNode = intArrayRBTindex.previousNode(this.currentNode);
  }

  @Override
  public int get() {
    if (!isValid()) {
      throw new NoSuchElementException();
    }
    checkConcurrentModification(); 
    return intArrayRBTindex.getKeyForNode(this.currentNode);
  }

  /**
   * @see org.apache.uima.internal.util.IntPointerIterator#copy()
   */
  @Override
  public Object copy() {
    IntIterator4set<T> copy = new IntIterator4set<T>(this.fsSetIndex, this.detectIllegalIndexUpdates, this.comp);
    copy.currentNode = this.currentNode;
    return copy;
  }

  /**
   * @see org.apache.uima.internal.util.IntPointerIterator#moveTo(int)
   */
  @Override
  public void moveTo(int i) {
    resetConcurrentModification();
    this.currentNode = intArrayRBTindex.findInsertionPoint(i);
  }

  @Override
  public int ll_indexSize() {
    return fsSetIndex.size();
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.FSIntIteratorImplBase#compareTo(org.apache.uima.cas.impl.FSIntIteratorImplBase)
   */
  @Override
  public int compareTo(FSIntIteratorImplBase<T> o) {
    return this.comp.compare(get(), ((IntIterator4set<T>) o).get());
  }

}

