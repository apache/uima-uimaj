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

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;

/**
 * A compound iterater, wrapping an FSIterator, and applying it to all views in some arbitrary order.
 * @param <T> the type of the iterator
 */
public class FsIterator_allViews<T extends FeatureStructure> 
               implements LowLevelIterator<T>, Comparable<FsIterator_singletype<T>> {
 
  private final LowLevelIterator<T> it;
  
  private final FSIndexRepository[] views;  

  public FsIterator_allViews(LowLevelIterator<T> it) {
    this.it = it;
    
    CASImpl cas = it.ll_getIndex().getCasImpl();
    final int nbrViews = cas.getNumberOfViews();
    views = new FSIndexRepository[nbrViews];
    
    for (int i = 1; i <= nbrViews; i++) {
      views[i - 1] =  
          ((i == 1) ? cas.getInitialView() : (CASImpl) cas.getView(i))
          .getIndexRepository();
    }

  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#isValid()
   */
  @Override
  public boolean isValid() {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#get()
   */
  @Override
  public T get() throws NoSuchElementException {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#getNvc()
   */
  @Override
  public T getNvc() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveToNext()
   */
  @Override
  public void moveToNext() {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveToNextNvc()
   */
  @Override
  public void moveToNextNvc() {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveToPrevious()
   */
  @Override
  public void moveToPrevious() {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveToPreviousNvc()
   */
  @Override
  public void moveToPreviousNvc() {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveToFirst()
   */
  @Override
  public void moveToFirst() {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveToLast()
   */
  @Override
  public void moveToLast() {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#moveTo(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public void moveTo(FeatureStructure fs) {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  @Override
  public FSIterator<T> copy() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(FsIterator_singletype<T> o) {
    // TODO Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#ll_indexSize()
   */
  @Override
  public int ll_indexSize() {
    // TODO Auto-generated method stub
    return 0;
  }
  
  @Override
  public int ll_maxAnnotSpan() {
    return Integer.MAX_VALUE;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#ll_getIndex()
   */
  @Override
  public LowLevelIndex<T> ll_getIndex() {
    // TODO Auto-generated method stub
    return null;
  }
  
  

  
}
