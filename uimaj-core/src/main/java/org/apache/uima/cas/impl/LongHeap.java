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
 * Encapsulate 64 bit storage for a CAS.
 */
final class LongHeap extends CommonAuxHeap {

  long[] heap; // is never null after construction

  LongHeap() {
    super(DEFAULT_HEAP_BASE_SIZE, DEFAULT_HEAP_MULT_LIMIT);
  }

  LongHeap(int heapBaseSize, int heapMultLimit) {
    super(heapBaseSize, heapMultLimit);
  }

  final void initMemory() {
    this.heap = new long[this.heapBaseSize];
  }
  
  final void initMemory(int size) {
    this.heap = new long[size];
  }

  final int getCapacity() {
    return this.heap.length;
  }
  
  void growHeapIfNeeded() {
    if (heap.length >= heapPos)
      return;

    long[] new_array = new long[computeNewArraySize(heap.length, heapPos, GROWTH_FACTOR,
            heapMultLimit)];
    System.arraycopy(heap, 0, new_array, 0, heap.length);
    heap = new_array;
  }

  void resetToZeros() {
    Arrays.fill(this.heap, 0, this.heapPos, NULL);
  }

  // Getters
  long getHeapValue(int offset) {
    return this.heap[offset];
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

  protected void reinit(long[] longHeap) {
    int argLength = longHeap.length;
    if (argLength > heap.length)
      heap = new long[argLength];

    System.arraycopy(longHeap, 0, heap, 0, argLength);
    this.heapPos = argLength;
  }

}
