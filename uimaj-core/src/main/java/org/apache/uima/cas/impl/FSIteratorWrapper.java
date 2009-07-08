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

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.internal.util.IntPointerIterator;

/**
 * Class comment for FSIteratorWrapper.java goes here.
 * 
 * 
 */
public class FSIteratorWrapper extends FSIteratorImplBase {

  IntPointerIterator it;

  CASImpl casImpl;

  FSIteratorWrapper(IntPointerIterator it, CASImpl casImpl) {
    this.it = it;
    this.casImpl = casImpl;
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
  public FeatureStructure get() {
    return this.casImpl.createFS(this.it.get());
  }

  /**
   * @see org.apache.uima.cas.FSIterator#moveToNext()()
   */
  public void moveToNext() {
    this.it.inc();
  }

  /**
   * @see org.apache.uima.cas.FSIterator#moveToPrevious()()
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
  public FSIterator copy() {
    return new FSIteratorWrapper((IntPointerIterator) this.it.copy(), this.casImpl);
  }

  /**
   * @see org.apache.uima.cas.FSIterator#moveTo(FeatureStructure)
   */
  public void moveTo(FeatureStructure fs) {
    this.it.moveTo(((FeatureStructureImpl) fs).getAddress());
  }

}
