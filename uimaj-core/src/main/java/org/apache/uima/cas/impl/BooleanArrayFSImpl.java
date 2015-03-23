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

import org.apache.uima.cas.BooleanArrayFS;

/**
 * Implementation of the {@link org.apache.uima.cas.BooleanArrayFS BooleanArrayFS} interface.
 * 
 * 
 */
public class BooleanArrayFSImpl extends CommonAuxArrayFSImpl implements BooleanArrayFS {

  private static class BooleanArrayGenerator implements FSGenerator<BooleanArrayFSImpl> {
    /**
     * @see org.apache.uima.cas.impl.FSGenerator#createFS(int, LowLevelCAS)
     */
    public BooleanArrayFSImpl createFS(int addr, CASImpl cas) {
      return new BooleanArrayFSImpl(addr, cas);
    }
  }

  public BooleanArrayFSImpl(int addr, CASImpl cas) {
    super(cas, addr); // note arg reversal
  }

  static FSGenerator<BooleanArrayFSImpl> generator() {
    return new BooleanArrayGenerator();
  }

  /**
   * @see org.apache.uima.cas.BooleanArrayFS#get(int)
   */
  public boolean get(int i) {
    casImpl.checkArrayBounds(addr, i); // don't need to check type code
    return casImpl.ll_getBooleanArrayValue(addr, i);
  }

  /**
   * @see org.apache.uima.cas.BooleanArrayFS#set(int, boolean)
   */
  public void set(int i, boolean val) throws ArrayIndexOutOfBoundsException {
    casImpl.checkArrayBounds(addr, i); // don't need to check type code
    casImpl.ll_setBooleanArrayValue(addr, i, val);
  }

  /**
   * @see org.apache.uima.cas.BooleanArrayFS#copyFromArray(boolean[], int, int, int)
   */
  public void copyFromArray(boolean[] src, int srcOffset, int destOffset, int length) {
    casImpl.checkArrayBounds(addr, destOffset, length);
    for (int i = 0; i < length; i++) {
      casImpl.ll_setBooleanArrayValue(addr, destOffset + i, src[srcOffset + i]);
    }
  }

  /**
   * @see org.apache.uima.cas.BooleanArrayFS#copyToArray(int, boolean[], int, int) int)
   */
  public void copyToArray(int srcOffset, boolean[] dest, int destOffset, int length) {
    casImpl.checkArrayBounds(addr, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = casImpl.ll_getBooleanArrayValue(addr, i + srcOffset);
    }
  }

  /**
   * @see org.apache.uima.cas.BooleanArrayFS#toArray()
   */
  public boolean[] toArray() {
    final int size = size();
    boolean[] outArray = new boolean[size];
    copyToArray(0, outArray, 0, size);
    return outArray;
  }

  /**
   * @see org.apache.uima.cas.CommonArrayFS#copyToArray(int, String[], int, int)
   */
  public void copyToArray(int srcOffset, String[] dest, int destOffset, int length) {
    casImpl.checkArrayBounds(addr, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = Boolean.toString(casImpl.ll_getBooleanArrayValue(addr, i + srcOffset));
    }
  }

  /**
   * @see org.apache.uima.cas.CommonArrayFS#copyFromArray(String[], int, int, int)
   */
  public void copyFromArray(String[] src, int srcOffset, int destOffset, int length) {
    casImpl.checkArrayBounds(addr, destOffset, length);
    for (int i = 0; i < length; i++) {
      final String item = src[srcOffset + i];
      casImpl.ll_setBooleanArrayValue(addr, destOffset + i, null != item
              && item.equalsIgnoreCase("true"));
    }
  }
}
