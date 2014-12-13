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

import java.lang.reflect.Array;

/**
 * Common part of array impl for those arrays of primitives which use auxilliary heaps. Is a super
 * class to those.
 */
public abstract class CommonAuxArrayFSImpl extends FeatureStructureImplC {

  protected CommonAuxArrayFSImpl() {
  }

  protected CommonAuxArrayFSImpl(CASImpl cas, int addr) {
    super(cas, addr);
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#size()
   * @return -
   */
  public int size() {
    return casImpl.ll_getArraySize(addr);
  }

  /**
   * copyFromArray - only works for things where the src and tgt are the same underlying type (long
   * and byte) src = external java object, tgt = internal CAS Aux heap
   * @param src -
   * @param srcOffset -
   * @param casAuxHeap -
   * @param tgtOffset - 
   * @param length -
   */
  protected void copyFromJavaArray(Object src, int srcOffset, Object casAuxHeap, int tgtOffset,
          int length) {
    this.casImpl.checkArrayBounds(this.addr, tgtOffset, length);
    final int startOffset = casImpl.getHeap().heap[casImpl.getArrayStartAddress(this.addr)];
    System.arraycopy(src, srcOffset, casAuxHeap, startOffset + tgtOffset, length);
  }

  /**
   * copyFromArray - only works for things where the src and tgt are the same underlying type (long
   * and byte) src = internal CAS Aux heap, tgt = external java object
   * @param casAuxHeap -
   * @param srcOffset -
   * @param tgt -
   * @param tgtOffset -
   * @param length -
   */
  protected void copyToJavaArray(Object casAuxHeap, int srcOffset, Object tgt, int tgtOffset,
          int length) {
    this.casImpl.checkArrayBounds(this.addr, srcOffset, length);
    final int startOffset = casImpl.getHeap().heap[casImpl.getArrayStartAddress(this.addr)];
    System.arraycopy(casAuxHeap, startOffset + srcOffset, tgt, tgtOffset, length);
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#toArray()
   * @param casAuxHeap -
   * @return -
   */
  protected Object toArray(Object casAuxHeap) {
    final int size = size();
    Object outArray = Array.newInstance(casAuxHeap.getClass().getComponentType(), size);
    copyToJavaArray(casAuxHeap, 0, outArray, 0, size);
    return outArray;
  }

  public abstract void copyToArray(int srcOffset, String[] dest, int destOffset, int length);

  public String[] toStringArray() {
    final int size = size();
    String[] strArray = new String[size];
    copyToArray(0, strArray, 0, size);
    return strArray;
  }
}
