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

import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.FeatureStructure;

/**
 * Implementation of the {@link org.apache.uima.cas.ByteArrayFS ByteArrayFS} interface.
 * 
 * 
 */
public class ByteArrayFSImpl extends CommonAuxArrayFSImpl implements ByteArrayFS {

  private static class ByteArrayGenerator implements FSGenerator<ByteArrayFSImpl> {

    /**
     * @see org.apache.uima.cas.impl.FSGenerator#createFS(int, LowLevelCAS)
     */
    public ByteArrayFSImpl createFS(int addr, CASImpl cas) {
      return new ByteArrayFSImpl(addr, cas);
    }
  }

  public ByteArrayFSImpl(int addr, CASImpl cas) {
    super(cas, addr); // note arg reversal
  }

  static FSGenerator<ByteArrayFSImpl> generator() {
    return new ByteArrayGenerator();
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#get(int)
   */
  public byte get(int i) {
    casImpl.checkArrayBounds(addr, i);
    return casImpl.ll_getByteArrayValue(addr, i);
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#set(int, FeatureStructure)
   */
  public void set(int i, byte val) {
    casImpl.checkArrayBounds(addr, i);
    casImpl.ll_setByteArrayValue(addr, i, val);
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyFromArray(FeatureStructure[], int, int, int)
   */
  public void copyFromArray(byte[] src, int srcOffset, int destOffset, int length) {
    copyFromJavaArray(src, srcOffset, this.casImpl.getByteHeap().heap, destOffset, length);
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyToArray(int, FeatureStructure[], int, int)
   */
  public void copyToArray(int srcOffset, byte[] dest, int destOffset, int length) {
    copyToJavaArray(this.casImpl.getByteHeap().heap, srcOffset, dest, destOffset, length);
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#toArray()
   */
  public byte[] toArray() {
    return (byte[]) toArray(this.casImpl.getByteHeap().heap);
  }

  /**
   * @see org.apache.uima.cas.CommonArrayFS#copyToArray(int, String[], int, int)
   */
  public void copyToArray(int srcOffset, String[] dest, int destOffset, int length) {
    casImpl.checkArrayBounds(addr, srcOffset, length);
    final int startOffset = srcOffset + casImpl.getHeap().heap[casImpl.getArrayStartAddress(addr)];
    final byte[] heap = this.casImpl.getByteHeap().heap;
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = Byte.toString(heap[i + startOffset]);
    }
  }

  /**
   * @see org.apache.uima.cas.CommonArrayFS#copyFromArray(String[], int, int, int)
   */
  public void copyFromArray(String[] src, int srcOffset, int destOffset, int length) {
    this.casImpl.checkArrayBounds(this.addr, destOffset, length);
    final int startOffset = destOffset + casImpl.getHeap().heap[casImpl.getArrayStartAddress(this.addr)];
    byte[] heap = this.casImpl.getByteHeap().heap;
    for (int i = 0; i < length; i++) {
      heap[i + startOffset] = Byte.parseByte(src[i + srcOffset]);
    }
  }
}
