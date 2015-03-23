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
import org.apache.uima.cas.StringArrayFS;

/**
 * Implementation of the {@link org.apache.uima.cas.ArrayFS ArrayFS} interface.
 * 
 * 
 */
public class StringArrayFSImpl extends CommonArrayFSImpl implements StringArrayFS {

  private static class StringArrayGenerator implements FSGenerator<StringArrayFSImpl> {

    private StringArrayGenerator() {
      super();
    }

    /**
     * @see org.apache.uima.cas.impl.FSGenerator#createFS(int, LowLevelCAS)
     */
    public  StringArrayFSImpl createFS(int addr, CASImpl cas) {
      return new StringArrayFSImpl(addr, cas);
    }

  }

  public StringArrayFSImpl(int addr, CASImpl cas) {
    super(cas, addr);
  }

  static FSGenerator<StringArrayFSImpl> generator() {
    return new StringArrayGenerator();
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#size()
   */
  public int size() {
    return this.casImpl.ll_getArraySize(this.addr);
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#get(int)
   */
  public String get(int i) {
    if (i < 0 || i >= size()) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return this.casImpl.getStringForCode(this.casImpl.getArrayValue(this.addr, i));
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#set(int, FeatureStructure)
   */
  public void set(int i, String str) throws ArrayIndexOutOfBoundsException {
    this.casImpl.setArrayValue(this.addr, i, this.casImpl.addString(str));
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyFromArray(FeatureStructure[], int, int, int)
   */
  public void copyFromArray(String[] src, int srcOffset, int destOffset, int length)
          throws ArrayIndexOutOfBoundsException {
    if ((destOffset < 0) || ((destOffset + length) > size())) {
      throw new ArrayIndexOutOfBoundsException();
    }
    destOffset += this.casImpl.getArrayStartAddress(this.addr);
    for (int i = 0; i < length; i++) {
      // cas.getHeap().heap[destOffset] =
      // ((FeatureStructureImpl)src[srcOffset]).getAddress();
      this.casImpl.getHeap().heap[destOffset] = this.casImpl.addString(src[srcOffset]);
      ++destOffset;
      ++srcOffset;
    }
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyToArray(int, FeatureStructure[], int, int)
   */
  public void copyToArray(int srcOffset, String[] dest, int destOffset, int length)
          throws ArrayIndexOutOfBoundsException {
    if ((srcOffset < 0) || ((srcOffset + length) > size())) {
      throw new ArrayIndexOutOfBoundsException();
    }
    for (int i = 0; i < length; i++) {
      dest[destOffset] = this.casImpl.ll_getStringArrayValue(this.addr, srcOffset);
      ++destOffset;
      ++srcOffset;
    }
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#toArray()
   */
  public String[] toArray() {
    final int size = size();
    String[] outArray = new String[size];
    copyToArray(0, outArray, 0, size);
    return outArray;
  }

}
