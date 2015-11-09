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
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.cas.TOP;


public abstract class FsIterator_subtypes<T extends TOP> implements LowLevelIterator<T> {

  // The IICP
  final protected FsIndex_iicp<T> iicp;
 
  public FsIterator_subtypes(FsIndex_iicp<T> iicp) {
    this.iicp = iicp;
  } 
  
  protected FsIndex_iicp<? extends FeatureStructure> getIicp() {
    return iicp;
  }
  
  @Override
  public LowLevelIndex<T> ll_getIndex() {
    return iicp.fsIndex_singletype;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#ll_indexSize()
   */
  @Override
  public int ll_indexSize() {
    return ll_getIndex().size();
  }

  final static FsIterator_subtypes FS_ITERATOR_EMPTY = new FsIterator_subtypes(null) {
    @Override
    public boolean isValid() {return false;}
    @Override
    public FeatureStructure get() throws NoSuchElementException { throw new NoSuchElementException(); }
    @Override
    public void moveToNext() {}
    @Override
    public void moveToPrevious() {}
    @Override
    public void moveToFirst() {}
    @Override
    public void moveToLast() {}
    @Override
    public void moveTo(FeatureStructure fs) {}
    @Override
    public FSIterator<FeatureStructure> copy() { return this; }
  };
  
  @Override
  public String toString() {
    TypeImpl type = (TypeImpl) this.ll_getIndex().getType();
    StringBuilder sb = new StringBuilder(this.getClass().getSimpleName()).append(":").append(System.identityHashCode(this));
    sb.append(" over Type: ").append(type.getName()).append(":").append(type.getCode());
    sb.append(", size: ").append(this.ll_indexSize());
    return sb.toString();
  }
  
}
