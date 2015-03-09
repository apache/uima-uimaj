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

/**
 * Encapsulate 8, 16, and 64 bit storage for the CAS.
 */
abstract class CommonAuxHeap {
  
  private static final boolean debugLogShrink = false;
//  static {
//    debugLogShrink = System.getProperty("uima.debug.ihs") != null;
//  }
  
  // cannot be 0 because it grows by multiplying growth_factor
  protected static final int DEFAULT_HEAP_BASE_SIZE = 16;

  // Jira: https://issues.apache.org/jira/browse/UIMA-2385, 
  // https://issues.apache.org/jira/browse/UIMA-4279
  protected static final int DEFAULT_HEAP_MULT_LIMIT = 1024 * 1024 * 16; 

  protected static final int MIN_HEAP_BASE_SIZE = 16;

  protected static final int GROWTH_FACTOR = 2;

  protected static final int NULL = 0;

  // start pos
  protected static final int FIRST_CELL_REF = 1;

  protected final int heapBaseSize;

  protected final int heapMultLimit;

  protected int heapPos = FIRST_CELL_REF;
  
  private int prevSize = 1;

  CommonAuxHeap() {
    this(DEFAULT_HEAP_BASE_SIZE, DEFAULT_HEAP_MULT_LIMIT);
  }

  CommonAuxHeap(int heapBaseSize, int heapMultLimit) {
    super();
    this.heapBaseSize = Math.max(heapBaseSize, MIN_HEAP_BASE_SIZE);
    this.heapMultLimit = Math.max(heapMultLimit, DEFAULT_HEAP_MULT_LIMIT);
    initMemory();
  }

  abstract void initMemory();
  
  abstract void initMemory(int size);

  abstract void resetToZeros();

  abstract void growHeapIfNeeded();

  void reset() {
    this.reset(false);
  }

  /**
   * Logic for shrinking:
   * 
   *   Based on a short history of the capacity needed to hold the larger of the previous 2 sizes
   *     (Note: can be overridden by calling reset() multiple times in a row)
   *   Never shrink below initialSize
   *   
   *   Shrink in exact reverse sequence of growth - using the subtraction method 
   *   and then (for small enough sizes) the dividing method
   *   
   *   Shrink by one jump if that is large enough to hold the larger of the prev 2 sizes

   * @param doFullReset true means reallocate from scratch
   */
  void reset(boolean doFullReset) {
    if (doFullReset) {
      if (debugLogShrink) System.out.format("Debug shrink CommonAux full reset from %,d to %,d for %s%n",
          getCapacity(), heapBaseSize, this.getClass().getSimpleName());
      this.initMemory();
    } else {
      final int curCapacity = getCapacity();
      final int curSize = getSize();
      int newSize = computeShrunkArraySize(
          curCapacity, Math.max(prevSize, curSize), GROWTH_FACTOR, heapMultLimit, heapBaseSize);
      if (newSize == getCapacity()) { // means didn't shrink
        resetToZeros();
      } else {
        if (debugLogShrink) System.out.format("Debug shrink CommonAux from %,d to %,d, prevSize=%,d for %s%n",
            curCapacity, newSize, prevSize, this.getClass().getSimpleName());
        initMemory(newSize);
      }
      prevSize = curSize;
    }
    this.heapPos = FIRST_CELL_REF;
  }

  int reserve(int numCells) {
    int cellRef = this.heapPos;
    this.heapPos += numCells;
    growHeapIfNeeded();
    return cellRef;
  }

  int computeNewArraySize(int size, int needed_size, int growth_factor, int multiplication_limit) {
    do {
      if (size < multiplication_limit) {
        size *= growth_factor;
      } else {
        size += multiplication_limit;
      }
    } while (size < needed_size);
    return size;
  }
  
  /**
   * Guard against two resets in a row, by having the minimum size
   * @param capacity the current capacity
   * @param size_used the maximum number of used entries, <= current capacity
   * @param growth_factor is 2
   * @param multiplication_limit the point where we start adding this limit, vs using the growth factor
   * @return the capacity shrink down by one step, if that will still hold the size_used number of entries, 
   *   minimum limited to min_size.
   */
  static int computeShrunkArraySize(
      int capacity, 
      int size_used, 
      int growth_factor, 
      int multiplication_limit, 
      int min_size) {
    int nbrOfSteps = 0;
    if (capacity < size_used) {
      throw new IllegalArgumentException("The Capacity " + capacity + " must be >= sized_used " + size_used);
    }
    
    // this if for shrinking down 1 step if possible
    int shrunk = ((capacity - multiplication_limit) < multiplication_limit) ?
    // the last expansion was by multiplying; the next expansion would be by adding
    capacity / growth_factor :
    capacity - multiplication_limit;

    return (size_used > shrunk) ? capacity : shrunk;
    
    
    // this is for shrinking down to the minimum needed, and then expanding back up halfway (by # of steps)
//    while (true) {
//      int shrunk = ((capacity - multiplication_limit) < multiplication_limit) ?
//        // the last expansion was by multiplying; the next expansion would be by adding
//        capacity / growth_factor :
//        capacity - multiplication_limit;
//      if (size_used > shrunk) {
//        return computeHalfWaySize(capacity, nbrOfSteps, growth_factor, multiplication_limit);
//      }
//      if (shrunk < min_size) {
//        return computeHalfWaySize(min_size, nbrOfSteps, growth_factor, multiplication_limit);
//      }
//      nbrOfSteps ++;
//      capacity = shrunk;
//    }
  }
  
  static int computeHalfWaySize(int shrunkCapacity, final int nbrOfSteps, int growth_factor, int multiplication_limit) {
    // n is nbrOfSteps / 2 rounded up, except for 1, where it is rounded down.
    //   to permit shrinking all the way to initial size
    int n2 = nbrOfSteps >> 1;
    int n = (nbrOfSteps       == 1) ? 0 : 
            ((nbrOfSteps % 2) == 0) ? n2 :
                                      n2 + 1;
               
    for (int i = 0; i < n; i++) {
      shrunkCapacity = (shrunkCapacity < multiplication_limit)? shrunkCapacity * growth_factor : shrunkCapacity + multiplication_limit;
    }
    return shrunkCapacity;
  }

  int getSize() {
    return this.heapPos;
  }
  
  abstract int getCapacity();

}
