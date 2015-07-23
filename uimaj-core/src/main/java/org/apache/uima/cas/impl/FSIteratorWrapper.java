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

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.internal.util.IntPointerIterator;

public class FSIteratorWrapper<T extends FeatureStructure> extends FSIteratorImplBase<T> {

  @Override
  public String toString() {
    return "FSIteratorWrapper [it=" + it + "]";
  }

  IntPointerIterator it;

  CASImpl casImpl;
  
  final int beginOffset;
  final int endOffset;
  

  FSIteratorWrapper(IntPointerIterator it, CASImpl casImpl) {
    this.it = it;
    this.casImpl = casImpl;
    TypeSystemImpl tsi = casImpl.getTypeSystemImpl(); 
    beginOffset = casImpl.getFeatureOffset(TypeSystemImpl.startFeatCode);
    endOffset = casImpl.getFeatureOffset(TypeSystemImpl.endFeatCode);
  }

  /**
   * @see org.apache.uima.cas.FSIterator#isValid()
   */
  public boolean isValid() {
    return this.it.isValid();
  }

  /**
   * @see org.apache.uima.cas.FSIterator#get()
   */
  public T get() {
    return this.casImpl.createFS(this.it.get());
  }

  /**
   * @see org.apache.uima.cas.FSIterator#moveToNext()
   */
  public void moveToNext() {
    this.it.inc();
  }

  /**
   * @see org.apache.uima.cas.FSIterator#moveToPrevious()
   */
  public void moveToPrevious() {
    this.it.dec();
  }

  /**
   * @see org.apache.uima.cas.FSIterator#moveToFirst()
   */
  public void moveToFirst() {
    this.it.moveToFirst();
  }

  /**
   * @see org.apache.uima.cas.FSIterator#moveToLast()
   */
  public void moveToLast() {
    this.it.moveToLast();
  }

  /**
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  public FSIterator<T> copy() {
    return new FSIteratorWrapper<T>((IntPointerIterator) this.it.copy(), this.casImpl);
  }

  /**
   * @see org.apache.uima.cas.FSIterator#moveTo(FeatureStructure)
   */
  public void moveTo(FeatureStructure fs) {
    this.it.moveTo(((FeatureStructureImpl) fs).getAddress());
  }

  @Override
  int getBegin() {
    return casImpl.getHeapValue(it.get() + beginOffset);
  }

  @Override
  int getEnd() {
    return casImpl.getHeapValue(it.get() + endOffset);
  }

  @Override
  <TT extends AnnotationFS> void moveTo(int begin, int end) {
    ((FSIteratorImplBase<TT>)it).moveTo(begin, end);
  }
  
  

}
