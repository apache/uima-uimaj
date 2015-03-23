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
import org.apache.uima.cas.LongArrayFS;

/**
 * Implementation of the {@link org.apache.uima.cas.ArrayFS ArrayFS} interface.
 * 
 * 
 */
public class LongArrayFSImpl extends CommonAuxArrayFSImpl implements LongArrayFS {

  private static class LongArrayGenerator implements FSGenerator<LongArrayFSImpl> {

    private LongArrayGenerator() {
      super();
    }

    /**
     * @see org.apache.uima.cas.impl.FSGenerator#createFS(int, LowLevelCAS)
     */
    public LongArrayFSImpl createFS(int addr, CASImpl cas) {
      return new LongArrayFSImpl(addr, cas);
    }

  }

  public LongArrayFSImpl(int addr, CASImpl cas) {
    super(cas, addr);
  }

  static FSGenerator<LongArrayFSImpl> generator() {
    return new LongArrayGenerator();
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#size()
   */
  public int size() {
    return casImpl.ll_getArraySize(addr);
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#get(int)
   */
  public long get(int i) {
    if (i < 0 || i >= size()) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return casImpl.ll_getLongArrayValue(addr, i);
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#set(int, FeatureStructure)
   */
  public void set(int i, long val) throws ArrayIndexOutOfBoundsException {
    casImpl.ll_setLongArrayValue(addr, i, val);
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyFromArray(FeatureStructure[], int, int, int)
   */
  public void copyFromArray(long[] src, int srcOffset, int destOffset, int length)
          throws ArrayIndexOutOfBoundsException {
    if ((destOffset < 0) || ((destOffset + length) > size())) {
      throw new ArrayIndexOutOfBoundsException();
    }

    final int startoffset = casImpl.getHeap().heap[casImpl.getArrayStartAddress(this.addr)];
    destOffset += startoffset;
    for (int i = 0; i < length; i++) {
      // cas.getHeap().heap[destOffset] = ((FeatureStructureImpl)src[srcOffset]).getAddress();
      this.casImpl.ll_setLongArrayValue(this.addr, destOffset, src[srcOffset]);
      ++destOffset;
      ++srcOffset;
    }
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyToArray(int, FeatureStructure[], int, int)
   */
  public void copyToArray(int srcOffset, long[] dest, int destOffset, int length)
          throws ArrayIndexOutOfBoundsException {
    if ((srcOffset < 0) || ((srcOffset + length) > size())) {
      throw new ArrayIndexOutOfBoundsException();
    }

    // srcOffset += this.casImpl.getHeap().heap[this.casImpl.getArrayStartAddress(addr)];
    for (int i = 0; i < length; i++) {
      dest[destOffset] = this.casImpl.ll_getLongArrayValue(this.addr, srcOffset);
      ++destOffset;
      ++srcOffset;
    }
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#toArray()
   */
  public long[] toArray() {
    final int size = size();
    long[] outArray = new long[size];
    copyToArray(0, outArray, 0, size);
    return outArray;
  }

  public String[] toStringArray() {
    final int size = size();
    String[] strArray = new String[size];
    copyToArray(0, strArray, 0, size);
    return strArray;
  }

  public void copyToArray(int srcOffset, String[] dest, int destOffset, int length)
          throws ArrayIndexOutOfBoundsException {
    if ((srcOffset < 0) || ((srcOffset + length) > size())) {
      throw new ArrayIndexOutOfBoundsException();
    }
    long[] bDest = new long[dest.length];
    copyToArray(srcOffset, bDest, destOffset, length);

    for (int i = 0; i < length; i++) {
      dest[destOffset] = Long.toString(bDest[destOffset]);
      ++destOffset;
    }

  }

  public void copyFromArray(String[] src, int srcOffset, int destOffset, int length)
          throws ArrayIndexOutOfBoundsException {
    long[] bArray = new long[length];
    for (int i = 0; i < length; i++) {
      bArray[i] = Long.parseLong(src[i]);
    }
    copyFromArray(bArray, srcOffset, destOffset, length);
  }

}
