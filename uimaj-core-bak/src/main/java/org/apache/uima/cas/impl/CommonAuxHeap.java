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
   // cannot be 0 because it grows by multiplying growth_factor
   protected static final int DEFAULT_HEAP_BASE_SIZE = 16;
   protected static final int DEFAULT_HEAP_MULT_LIMIT = 1024;  
   protected static final int MIN_HEAP_BASE_SIZE = 16;
   protected static final int GROWTH_FACTOR = 2;

   protected static final int NULL = 0;

   //start pos
   protected static final int FIRST_CELL_REF = 1;

   protected final int heapBaseSize;
   protected final int heapMultLimit;

   protected int heapPos = FIRST_CELL_REF;
   
   CommonAuxHeap() {
      this (DEFAULT_HEAP_BASE_SIZE,
      DEFAULT_HEAP_MULT_LIMIT);
   }

   CommonAuxHeap(int heapBaseSize, int heapMultLimit) {
      super();
      this.heapBaseSize = Math.max(heapBaseSize, MIN_HEAP_BASE_SIZE);
      this.heapMultLimit = Math.max(heapMultLimit, DEFAULT_HEAP_MULT_LIMIT);
      initMemory();
   }

  abstract void initMemory() ;
  abstract void resetToZeros();
  abstract void growHeapIfNeeded();
   
  void reset() {
    this.reset(false);
   }

  void reset(boolean doFullReset) {
      if (doFullReset) {
         this.initMemory();
      }
      else {
        resetToZeros();
      }
      this.heapPos = FIRST_CELL_REF;
   }   
   
   int reserve (int numCells) {
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
   
  int getSize() {
    return this.heapPos;
  }

}
