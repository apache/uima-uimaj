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

import java.util.Arrays;

/**
 * the v2 CAS byte aux heap - used in modeling some binary (de)serialization
 */
final class ByteHeap extends CommonAuxHeap {

  byte[] heap;

  ByteHeap() {
    super(DEFAULT_HEAP_BASE_SIZE, DEFAULT_HEAP_MULT_LIMIT);
  }

  ByteHeap(int heapBaseSize, int heapMultLimit) {
    super(heapBaseSize, heapMultLimit);
  }

  @Override
  void initMemory() {
    heap = new byte[heapBaseSize];
  }

  @Override
  void initMemory(int size) {
    heap = new byte[size];
  }

  @Override
  int getCapacity() {
    return heap.length;
  }

  @Override
  void growHeapIfNeeded() {
    if (heap.length >= heapPos)
      return;

    byte[] new_array = new byte[computeNewArraySize(heap.length, heapPos, GROWTH_FACTOR,
            heapMultLimit)];
    System.arraycopy(heap, 0, new_array, 0, heap.length);
    heap = new_array;
  }

  @Override
  void resetToZeros() {
    Arrays.fill(heap, 0, heapPos, (byte) NULL);
  }

  // Getters
  byte getHeapValue(int offset) {
    return heap[offset];
  }

  // setters
  void setHeapValue(byte val, int pos) {
    heap[pos] = val;
  }

  int addByte(byte val) {
    int pos = reserve(1);
    heap[pos] = val;
    return pos;
  }

  int addByteArray(byte[] val) {
    int pos = reserve(val.length);
    System.arraycopy(val, 0, heap, pos, val.length);
    return pos;
  }

  int addBooleanArray(boolean[] val) {
    int pos = reserve(val.length);
    int i = pos;
    for (boolean v : val) {
      heap[i++] = v ? (byte) 1 : (byte) 0;
    }
    return pos;
  }

  int addBooleanArrayNoStore(boolean[] val) { // for compress4
    return reserve(val.length);
  }

  protected void reinit(byte[] byteHeap) {
    int argLength = byteHeap.length;
    if (argLength > heap.length)
      heap = new byte[argLength];

    System.arraycopy(byteHeap, 0, heap, 0, argLength);
    heapPos = argLength;
  }

  public byte[] toArray() {
    byte[] r = new byte[heapPos];
    System.arraycopy(heap, 0, r, 0, heapPos);
    return r;
  }

}
