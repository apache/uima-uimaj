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

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.FeatureStructure;

/**
 * Implementation of the {@link org.apache.uima.cas.ArrayFS ArrayFS} interface.
 * 
 * 
 */
public class ArrayFSImpl extends CommonArrayFSImpl implements ArrayFS {

  public ArrayFSImpl(int addr, CASImpl cas) {
    super(cas, addr);
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#size()
   */
  public int size() {
    return this.getCASImpl().ll_getArraySize(this.getAddress());
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#get(int)
   */
  public FeatureStructure get(int i) {
    if (i < 0 || i >= size()) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return this.casImpl.createFS(this.casImpl.getArrayValue(this.addr, i));
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#set(int, FeatureStructure)
   */
  public void set(int i, FeatureStructure fs) throws ArrayIndexOutOfBoundsException {
    this.casImpl.setArrayValue(this.addr, i, this.getCASImpl().ll_getFSRef(fs));
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyFromArray(FeatureStructure[], int,
   *      int, int)
   */
  public void copyFromArray(FeatureStructure[] src, int srcOffset, int destOffset, int length)
      throws ArrayIndexOutOfBoundsException {
    if ((destOffset < 0) || ((destOffset + length) > size())) {
      throw new ArrayIndexOutOfBoundsException();
    }
    destOffset += this.casImpl.getArrayStartAddress(this.addr);
    for (int i = 0; i < length; i++) {
      // cas.getHeap().heap[destOffset] =
      // ((FeatureStructureImpl)src[srcOffset]).getAddress();
      this.casImpl.getHeap().heap[destOffset] = this.getCASImpl().ll_getFSRef(src[srcOffset]);
      ++destOffset;
      ++srcOffset;
    }
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyToArray(int, FeatureStructure[], int,
   *      int)
   */
  public void copyToArray(int srcOffset, FeatureStructure[] dest, int destOffset, int length)
      throws ArrayIndexOutOfBoundsException {
    if ((srcOffset < 0) || ((srcOffset + length) > size())) {
      throw new ArrayIndexOutOfBoundsException();
    }
    srcOffset += this.casImpl.getArrayStartAddress(this.addr);
    for (int i = 0; i < length; i++) {
      FeatureStructure fs = null;
      final int address = this.casImpl.getHeap().heap[srcOffset];
      if (address != LowLevelCAS.NULL_FS_REF) {
        fs = this.casImpl.createFS(address);
      }
      dest[destOffset] = fs;
      ++destOffset;
      ++srcOffset;
    }
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#toArray()
   */
  public FeatureStructure[] toArray() {
    final int size = size();
    FeatureStructure[] outArray = new FeatureStructure[size];
    copyToArray(0, outArray, 0, size);
    return outArray;
  }

  public void copyToArray(int srcOffset, String[] dest, int destOffset, int length) {
    final CASImpl ll = this.casImpl;
    ll.checkArrayBounds(this.addr, srcOffset, length);
    for (int i = 0; i < length; i++) {
      final int address = ll.ll_getRefArrayValue(this.addr, i + srcOffset);
      String value = null;
      if (address != LowLevelCAS.NULL_FS_REF) {
        value = ll.ll_getFSForRef(address).toString();
      }
      dest[i + destOffset] = value;
    }
  }

  public void copyFromArray(String[] src, int srcOffset, int destOffset, int length) {
    throw new UnsupportedOperationException();
  }

}
