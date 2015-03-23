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

import org.apache.uima.cas.DoubleArrayFS;

/**
 * Implementation of the {@link org.apache.uima.cas.DoubleArrayFS DoubleArrayFS} interface.
 * 
 * 
 */
public class DoubleArrayFSImpl extends CommonAuxArrayFSImpl implements DoubleArrayFS {

  private static class DoubleArrayGenerator implements FSGenerator<DoubleArrayFSImpl> {
    /**
     * @see org.apache.uima.cas.impl.FSGenerator#createFS(int, LowLevelCAS)
     */
    public DoubleArrayFSImpl createFS(int addr, CASImpl cas) {
      return new DoubleArrayFSImpl(addr, cas);
    }
  }

  public DoubleArrayFSImpl(int addr, CASImpl cas) {
    super(cas, addr); // note arg reversal
  }

  static FSGenerator<DoubleArrayFSImpl> generator() {
    return new DoubleArrayGenerator();
  }

  /**
   * @see org.apache.uima.cas.DoubleArrayFS#get(int)
   */
  public double get(int i) {
    casImpl.checkArrayBounds(addr, i); // don't need to check type code
    return casImpl.ll_getDoubleArrayValue(addr, i);
  }

  /**
   * @see org.apache.uima.cas.DoubleArrayFS#set(int, double)
   */
  public void set(int i, double val) {
    casImpl.checkArrayBounds(addr, i); // don't need to check type code
    casImpl.ll_setDoubleArrayValue(addr, i, val);
  }

  /**
   * @see org.apache.uima.cas.DoubleArrayFS#copyFromArray(double[], int, int, int)
   */
  public void copyFromArray(double[] src, int srcOffset, int destOffset, int length) {
    casImpl.checkArrayBounds(addr, destOffset, length);
    for (int i = 0; i < length; i++) {
      casImpl.ll_setDoubleArrayValue(addr, destOffset + i, src[srcOffset + i]);
    }
  }

  /**
   * @see org.apache.uima.cas.DoubleArrayFS#copyToArray(int, double[], int, int)
   */
  public void copyToArray(int srcOffset, double[] dest, int destOffset, int length) {
    casImpl.checkArrayBounds(addr, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = casImpl.ll_getDoubleArrayValue(addr, i + srcOffset);
    }
  }

  /**
   * @see org.apache.uima.cas.DoubleArrayFS#toArray()
   */
  public double[] toArray() {
    final int size = size();
    double[] outArray = new double[size];
    copyToArray(0, outArray, 0, size);
    return outArray;
  }

  /**
   * @see org.apache.uima.cas.CommonArrayFS#copyToArray(int, String[], int, int)
   */
  public void copyToArray(int srcOffset, String[] dest, int destOffset, int length) {
    casImpl.checkArrayBounds(addr, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = Double.toString(casImpl.ll_getDoubleArrayValue(addr, i + srcOffset));
    }
  }

  /**
   * @see org.apache.uima.cas.CommonArrayFS#copyFromArray(String[], int, int, int)
   */
  public void copyFromArray(String[] src, int srcOffset, int destOffset, int length) {
    casImpl.checkArrayBounds(addr, destOffset, length);
    for (int i = 0; i < length; i++) {
      casImpl.ll_setDoubleArrayValue(addr, destOffset + i, Double.parseDouble(src[i + srcOffset]));
    }
  }
}
