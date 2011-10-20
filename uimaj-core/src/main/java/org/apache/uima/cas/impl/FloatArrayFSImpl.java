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
import org.apache.uima.cas.FloatArrayFS;

/**
 * Implementation of the {@link org.apache.uima.cas.IntArrayFS IntArrayFS} interface.
 * 
 * 
 */
public class FloatArrayFSImpl extends CommonArrayFSImpl implements FloatArrayFS {

  private static class FloatArrayFSGenerator implements FSGenerator {

    private FloatArrayFSGenerator() {
      super();
    }

    /**
     * @see org.apache.uima.cas.impl.FSGenerator#createFS(int, LowLevelCAS)
     */
    public FeatureStructure createFS(int addr, CASImpl cas) {
      return new FloatArrayFSImpl(addr, cas);
    }

  }

  public FloatArrayFSImpl(int addr, CASImpl cas) {
    super(cas, addr);
  }

  static FSGenerator generator() {
    return new FloatArrayFSGenerator();
  }

  /**
   * @see org.apache.uima.cas.FloatArrayFS#get(int)
   */
  public float get(int i) {
    casImpl.checkArrayBounds(addr, i);
    return casImpl.ll_getFloatArrayValue(addr, i);
  }

  /**
   * @see org.apache.uima.cas.FloatArrayFS#set(int, float)
   */
  public void set(int i, float value) throws ArrayIndexOutOfBoundsException {
    casImpl.checkArrayBounds(addr, i);
    casImpl.ll_setFloatArrayValue(addr, i, value);
  }

  /**
   * @see org.apache.uima.cas.FloatArrayFS#copyFromArray(float[], int, int, int)
   */
  public void copyFromArray(float[] src, int srcOffset, int destOffset, int length) {
    casImpl.checkArrayBounds(addr, destOffset, length);
    final int[] heap = this.casImpl.getHeap().heap;
    destOffset += this.casImpl.getArrayStartAddress(this.addr);
    for (int i = 0; i < length; i++) {
      heap[i + destOffset] = CASImpl.float2int(src[i + srcOffset]);
    }
  }

  /**
   * @see org.apache.uima.cas.FloatArrayFS#copyToArray(int, float[], int, int)
   */
  public void copyToArray(int srcOffset, float[] dest, int destOffset, int length) {
    casImpl.checkArrayBounds(addr, srcOffset, length);
    final int[] heap = this.casImpl.getHeap().heap;
    srcOffset += this.casImpl.getArrayStartAddress(this.addr);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = CASImpl.int2float(heap[i + srcOffset]);
    }
  }

  /**
   * @see org.apache.uima.cas.FloatArrayFS#toArray()
   */
  public float[] toArray() {
    final int size = size();
    float[] outArray = new float[size];
    copyToArray(0, outArray, 0, size);
    return outArray;
  }

  /**
   * @see org.apache.uima.cas.FloatArrayFS#copyToArray(int, String[], int, int)
   */
  public void copyToArray(int srcOffset, String[] dest, int destOffset, int length) {
    casImpl.checkArrayBounds(addr, srcOffset, length);
    final int[] heap = this.casImpl.getHeap().heap;
    srcOffset += this.casImpl.getArrayStartAddress(this.addr);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = Float.toString(CASImpl.int2float(heap[i + srcOffset]));
    }
  }

  /**
   * @see org.apache.uima.cas.FloatArrayFS#copyFromArray(String[], int, int, int)
   */
  public void copyFromArray(String[] src, int srcOffset, int destOffset, int length) {
    casImpl.checkArrayBounds(addr, destOffset, length);
    final int[] heap = casImpl.getHeap().heap;
    destOffset += casImpl.getArrayStartAddress(this.addr);
    for (int i = 0; i < length; i++) {
      heap[i + destOffset] = CASImpl.float2int(Float.parseFloat(src[i + srcOffset]));
    }
  }
}
