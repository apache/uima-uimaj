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
 * the v2 CAS long aux heap - used in modeling some binary (de)serialization
 */
final class LongHeap extends CommonAuxHeap {

  long[] heap; // is never null after construction

  LongHeap() {
    super(DEFAULT_HEAP_BASE_SIZE, DEFAULT_HEAP_MULT_LIMIT);
  }

  LongHeap(int heapBaseSize, int heapMultLimit) {
    super(heapBaseSize, heapMultLimit);
  }

  @Override
  void initMemory() {
    heap = new long[heapBaseSize];
  }

  @Override
  void initMemory(int size) {
    heap = new long[size];
  }

  @Override
  int getCapacity() {
    return heap.length;
  }

  @Override
  void growHeapIfNeeded() {
    if (heap.length >= heapPos)
      return;

    long[] new_array = new long[computeNewArraySize(heap.length, heapPos, GROWTH_FACTOR,
            heapMultLimit)];
    System.arraycopy(heap, 0, new_array, 0, heap.length);
    heap = new_array;
  }

  @Override
  void resetToZeros() {
    Arrays.fill(heap, 0, heapPos, NULL);
  }

  // Getters
  long getHeapValue(int offset) {
    return heap[offset];
  }

  // setters
  void setHeapValue(long val, int pos) {
    heap[pos] = val;
  }

  int addLong(long val) {
    int pos = reserve(1);
    heap[pos] = val;
    return pos;
  }

  int addLongArray(long[] val) {
    int pos = reserve(val.length);
    System.arraycopy(val, 0, heap, pos, val.length);
    return pos;
  }

  int addDoubleArray(double[] val) {
    int pos = reserve(val.length);
    int i = pos;
    for (double d : val) {
      heap[i++] = CASImpl.double2long(d);
    }
    return pos;
  }

  protected void reinit(long[] longHeap) {
    int argLength = longHeap.length;
    if (argLength > heap.length)
      heap = new long[argLength];

    System.arraycopy(longHeap, 0, heap, 0, argLength);
    heapPos = argLength;
  }

  public long[] toArray() {
    long[] r = new long[heapPos];
    System.arraycopy(heap, 0, r, 0, heapPos);
    return r;
  }

}
