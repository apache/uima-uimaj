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
  
  private final int[] shrinkableCount = new int[1];

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
          curCapacity, curSize, GROWTH_FACTOR, heapMultLimit, heapBaseSize, shrinkableCount);
      if (newSize == getCapacity()) { // means didn't shrink
        resetToZeros();
      } else {
        if (debugLogShrink) System.out.format("Debug shrink CommonAux from %,d to %,d for %s%n",
            curCapacity, newSize, this.getClass().getSimpleName());
        initMemory(newSize);
      }
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
   * This routine is used to compute the capacity an internal expandable array 
   * should be reallocated to, upon reset.
   * 
   * It returns at most a 1 increment shrink, based on the doubling up to multiplication
   * limit, then addition thereafter.
   * 
   * It maintains a shrinkableCount - the number of consecutive times this could be shrunk.
   * This is reset to 0 if current size requires current capacity,
   *   that is, no shrinkage is possible for current size.
   * Otherwise, the count is incremented.
   * 
   * If the shrinkableCount is incremented to exceed 20, 
   * the capacity is allowed to drop by 1 allocation unit.
   * 
   * This guarantees the shrinkages are delayed until 20 shrinkable sizes 
   * are found (with no intervening non-shrinkable ones).
   * When a shrinkage happens, the count is reset to 16; this delays subsequent
   * shrinkages to happen only every (20 - 16) 4 resets. 
   * 
   * @param capacity the current capacity
   * @param size_used the maximum number of used entries, <= current capacity
   * @param growth_factor is 2
   * @param multiplication_limit the point where we start adding this limit, vs using the growth factor
   * @param shrinkableCount a pass-by-reference int reflecting the number of times it was shrinkable
   * @return the capacity shrink down by one step, if that will still hold the size_used number of entries, 
   *   minimum limited to min_size.
   */
  static int computeShrunkArraySize(
      int capacity, 
      int size_used, 
      int growth_factor, 
      int multiplication_limit,
      int min_size, 
      int[] shrinkableCount) {  // pass by reference

    if (capacity < size_used) {
      throw new IllegalArgumentException("The Capacity " + capacity + " must be >= sized_used " + size_used);
    }
    
    // this if for shrinking down 1 step if possible
    int oneSizeLowerCapacity = ((capacity - multiplication_limit) < multiplication_limit) ?
        // the last expansion was by multiplying; the next expansion would be by adding
        (capacity / growth_factor) :
        (capacity - multiplication_limit);

    if (oneSizeLowerCapacity < min_size) {
      return capacity;
    }
    
    boolean isShrink = (size_used < oneSizeLowerCapacity);
    if (isShrink) {
      shrinkableCount[0] ++;
      if (shrinkableCount[0] > 20) {
        shrinkableCount[0] = 16;
        return oneSizeLowerCapacity;
      }
      return capacity;
    }
    shrinkableCount[0] = 0;
    return capacity;
  }
    
  int getSize() {
    return this.heapPos;
  }
  
  abstract int getCapacity();

}
