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
import org.apache.uima.cas.ShortArrayFS;

/**
 * Implementation of the {@link org.apache.uima.cas.ArrayFS ArrayFS} interface.
 * 
 * 
 */
public class ShortArrayFSImpl extends CommonAuxArrayFSImpl implements ShortArrayFS {

  private static class ShortArrayGenerator implements FSGenerator {
    /**
     * @see org.apache.uima.cas.impl.FSGenerator#createFS(int, LowLevelCAS)
     */
    public FeatureStructure createFS(int addr, CASImpl cas) {
      return new ShortArrayFSImpl(addr, cas);
    }
  }

  public ShortArrayFSImpl(int addr, CASImpl cas) {
    super(cas, addr); // note arg reversal
  }

  static FSGenerator generator() {
    return new ShortArrayGenerator();
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#get(int)
   */
  public short get(int i) {
    casImpl.checkArrayBounds(addr, i);
    return casImpl.ll_getShortArrayValue(addr, i);
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#set(int, short)
   */
  public void set(int i, short val) {
    casImpl.checkArrayBounds(addr, i);
    casImpl.ll_setShortArrayValue(addr, i, val);
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#copyFromArray(short[], int, int, int)
   */
  public void copyFromArray(short[] src, int srcOffset, int destOffset, int length) {
    copyFromJavaArray(src, srcOffset, casImpl.getShortHeap().heap, destOffset, length);
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#copyToArray(int, short[], int, int)
   */
  public void copyToArray(int srcOffset, short[] dest, int destOffset, int length) {
    copyToJavaArray(this.casImpl.getShortHeap().heap, srcOffset, dest, destOffset, length);
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#toArray()
   */
  public short[] toArray() {
    return (short[]) toArray(this.casImpl.getShortHeap().heap);
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#copyToArray(int, String[], int, int)
   */
  public void copyToArray(int srcOffset, String[] dest, int destOffset, int length) {
    casImpl.checkArrayBounds(addr, srcOffset, length);
    srcOffset += casImpl.getHeap().heap[casImpl.getArrayStartAddress(addr)];
    final short[] heap = this.casImpl.getShortHeap().heap;
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = Short.toString(heap[i + srcOffset]);
    }
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#copyFromArray(String[], int, int, int)
   */
  public void copyFromArray(String[] src, int srcOffset, int destOffset, int length)
          throws ArrayIndexOutOfBoundsException {
    short[] bArray = new short[length];
    for (int i = 0; i < length; i++) {
      bArray[i] = Short.parseShort(src[i]);
    }
    copyFromArray(bArray, srcOffset, destOffset, length);
  }
}
