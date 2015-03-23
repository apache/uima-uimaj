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

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.IntArrayFS;

/**
 * Implementation of the {@link org.apache.uima.cas.IntArrayFS IntArrayFS} interface.
 * 
 * 
 */
public class IntArrayFSImpl extends CommonArrayFSImpl implements IntArrayFS {

  private static class IntArrayFSGenerator implements FSGenerator<IntArrayFSImpl> {

    private IntArrayFSGenerator() {
      super(); // does nothing, super is Object, is implicit by Java Lang rules
    }

    /**
     * @see org.apache.uima.cas.impl.FSGenerator#createFS(int, LowLevelCAS)
     */
    public IntArrayFSImpl createFS(int addr, CASImpl cas) {
      return new IntArrayFSImpl(addr, cas);
    }

  }

  public IntArrayFSImpl(int addr, CASImpl cas) {
    super(cas, addr);
  }

  static FSGenerator<IntArrayFSImpl> generator() {
    return new IntArrayFSGenerator();
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#get(int)
   */
  public int get(int i) {
    casImpl.checkArrayBounds(addr, i);
    return casImpl.ll_getIntArrayValue(addr, i);
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#set(int, FeatureStructure)
   */
  public void set(int i, int value) {
    casImpl.checkArrayBounds(addr, i);
    casImpl.ll_setIntArrayValue(addr, i, value);
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#copyFromArray(int[], int, int, int)
   */
  public void copyFromArray(int[] src, int srcOffset, int destOffset, int length) {
    casImpl.checkArrayBounds(addr, destOffset, length);
    destOffset += this.casImpl.getArrayStartAddress(this.addr);
    System.arraycopy(src, srcOffset, this.casImpl.getHeap().heap, destOffset, length);
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#copyToArray(int, int[], int, int)
   */
  public void copyToArray(int srcOffset, int[] dest, int destOffset, int length) {
    casImpl.checkArrayBounds(addr, srcOffset, length);
    srcOffset += this.casImpl.getArrayStartAddress(this.addr);
    System.arraycopy(this.casImpl.getHeap().heap, srcOffset, dest, destOffset, length);
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#toArray()
   */
  public int[] toArray() {
    final int size = size();
    int[] outArray = new int[size];
    copyToArray(0, outArray, 0, size);
    return outArray;
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#copyToArray(int, String[], int, int)
   */
  public void copyToArray(int srcOffset, String[] dest, int destOffset, int length) {
    casImpl.checkArrayBounds(addr, srcOffset, length);
    final int[] heap = this.casImpl.getHeap().heap;
    srcOffset += this.casImpl.getArrayStartAddress(this.addr);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = Integer.toString(heap[i + srcOffset]);
    }
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#copyFromArray(String[], int, int, int)
   */
  public void copyFromArray(String[] src, int srcOffset, int destOffset, int length) {
    casImpl.checkArrayBounds(addr, destOffset, length);
    final int[] heap = casImpl.getHeap().heap;
    destOffset += casImpl.getArrayStartAddress(this.addr);
    for (int i = 0; i < length; i++) {
      heap[i + destOffset] = Integer.parseInt(src[i + srcOffset]);
    }
  }
}
