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

import org.apache.uima.internal.util.IntArrayUtils;
import org.apache.uima.internal.util.Misc;

/**
 * the v2 CAS heap - used in modeling some binary (de)serialization
 */
public final class Heap {

  private static final boolean debugLogShrink = false;
  // static {
  // debugLogShrink = System.getProperty("uima.debug.ihs") != null;
  // }

  /**
   * Minimum size of the heap. Currently set to <code>1000</code>.
   */
  public static final int MIN_SIZE = 1024;

  /**
   * Default size of the heap. Currently set to <code>500000</code>(2 MB).
   */
  public static final int DEFAULT_SIZE = 1024 * 512; // 2 MB pages

  private static final int MULTIPLICATION_LIMIT = 1024 * 1024 * 16;

  // Initial size of the heap. This is also the size the heap will be reset to
  // on a full reset.
  private int initialSize;

  // The array that represents the actual heap is package private and
  // can be directly addressed by the LowLevelCAS.
  int[] heap;

  // Next free position on the heap.
  private int pos;

  // End of heap. In the current implementation, this is the same as
  // this.heap.length at all times.
  private int max;

  private final int[] shrinkableCount = new int[1];

  // Serialization constants. There are holes in the numbering for historical
  // reasons. Keep the holes for compatibility.
  private static final int SIZE_POS = 0;

  private static final int TMPP_POS = 1;

  private static final int TMPM_POS = 2;

  private static final int PGSZ_POS = 5;

  private static final int AVSZ_POS = 6;

  private static final int AVST_POS = 7;

  /**
   * Default constructor.
   */
  public Heap() {
    this(DEFAULT_SIZE);
  }

  /**
   * Constructor lets you set initial heap size. Use only if you know what you're doing.
   * 
   * @param initialSize
   *          The initial heap size. If this is smaller than the {@link #MIN_SIZE MIN_SIZE}, the
   *          default will be used instead.
   */
  public Heap(int initialSize) {
    if (initialSize < MIN_SIZE) {
      initialSize = MIN_SIZE;
    }
    this.initialSize = initialSize;
    initHeap();
  }

  private void initHeap() {
    heap = new int[initialSize];
    pos = 1; // 0 is not a valid address
    max = heap.length;
  }

  private void initHeap(int size) {
    heap = new int[size];
    pos = 1; // 0 is not a valid address
    max = heap.length;
  }

  void reinit(int[] md, int[] shortHeap) {
    if (md == null) {
      reinitNoMetaData(shortHeap);
      return;
    }
    // assert(md != null);
    // assert(shortHeap != null);
    final int heapSize = md[SIZE_POS];
    pos = md[TMPP_POS];
    max = md[TMPM_POS];
    initialSize = md[PGSZ_POS];

    // Copy the shortened version of the heap into a full version.
    heap = new int[heapSize];
    System.arraycopy(shortHeap, 0, heap, 0, shortHeap.length);

  }

  /**
   * Re-init the heap without metadata. Use default values for metadata.
   * 
   * @param shortHeap
   */
  private void reinitNoMetaData(int[] shortHeap) {
    initialSize = (shortHeap.length < MIN_SIZE) ? MIN_SIZE : shortHeap.length;
    if (shortHeap.length >= initialSize) {
      heap = shortHeap;
    } else {
      System.arraycopy(shortHeap, 0, heap, 0, shortHeap.length);
    }
    // Set position and max.
    pos = shortHeap.length;
    // this.max = this.initialSize;
    max = heap.length; // heap could be repl by short heap
  }

  /**
   * Re-create the heap for the given size. Just use the size of the incoming heap, unless it's
   * smaller than our minimum. It is expected that the caller will then fill in the new heap up to
   * newSize.
   * 
   * @param newSize
   */
  void reinitSizeOnly(int newSize) {
    initialSize = (newSize < MIN_SIZE) ? MIN_SIZE : newSize;
    heap = new int[initialSize];
    // Set position and max.
    pos = newSize;
    max = initialSize;
  }

  /**
   * Return the number of cells used (including slot 0 reserved for null)
   */
  int getCellsUsed() {
    return pos;
  }

  /**
   * @return The overall size of the heap (in words) (including unused space).
   */
  int getHeapSize() {
    return heap.length;
  }

  int[] getMetaData() {
    final int arSize = AVST_POS;
    int[] ar = new int[arSize];
    ar[SIZE_POS] = heap.length;
    ar[TMPP_POS] = pos;
    ar[TMPM_POS] = max;
    ar[PGSZ_POS] = initialSize;
    final int availablePagesSize = 0;
    ar[AVSZ_POS] = availablePagesSize;

    return ar;
  }

  // Grow the heap.
  private void grow() {
    final int start = heap.length;
    // This will grow the heap by doubling its size if it's smaller than
    // MULTIPLICATION_LIMIT, and by MULTIPLICATION_LIMIT if it's larger.
    heap = IntArrayUtils.ensure_size(heap, start + initialSize, 2, MULTIPLICATION_LIMIT);
    max = heap.length;
  }

  static int getRoundedSize(int size) {
    if (size < MULTIPLICATION_LIMIT) {
      return Misc.nextHigherPowerOf2(size);
    }
    return size + (MULTIPLICATION_LIMIT << 1);
  }

  /**
   * Reset the temporary heap.
   */
  public void reset() {
    this.reset(false);
  }

  /**
   * Reset the temporary heap.
   * 
   * Logic for shrinking:
   * 
   * Based on a short history of the sizes needed to hold the larger of the previous 2 sizes (Note:
   * can be overridden by calling reset() multiple times in a row) Never shrink below initialSize
   * 
   * Shrink in exact reverse sequence of growth - using the subtraction method and then (for small
   * enough sizes) the dividing method
   * 
   * Shrink 1/2 the distance to the size needed to hold the large of the prev 2 sizes
   */

  void reset(boolean doFullReset) {
    if (doFullReset) {
      if (debugLogShrink)
        System.out.format("Debug shrink Heap full reset from %,d%n", getHeapSize());
      this.initHeap();
    } else {
      final int curCapacity = getHeapSize();
      final int curSize = getCellsUsed();
      // shrink based on max of prevSize and curSize
      final int newCapacity = CommonAuxHeap.computeShrunkArraySize(curCapacity, curSize, 2,
              MULTIPLICATION_LIMIT, initialSize, shrinkableCount);
      if (newCapacity == curCapacity) {
        Arrays.fill(heap, 0, pos, 0);
      } else {
        if (debugLogShrink)
          System.out.format("Debug shrink Heap from %,d to %,d%n", curCapacity, newCapacity);
        this.initHeap(newCapacity);
      }
      pos = 1;
    }
  }

  /**
   * Add a structure to the heap.
   * 
   * @param fs
   *          The input structure.
   * @return The position where the structure was added, i.e., a pointer to the first element of the
   *         structure.
   */
  public int add(int[] fs) {
    while ((pos + fs.length) >= max) {
      grow();
    }
    System.arraycopy(fs, 0, heap, pos, fs.length);
    final int pos1 = pos;
    pos += fs.length;
    return pos1;
  }

  /**
   * Reserve space for <code>len</code> items on the heap and set the first item to
   * <code>val</code>. The other items are set to <code>0</code>.
   * 
   * @param len
   *          The length of the new structure.
   * @param val
   *          The value of the first cell in the new structure.
   * @return The position where the structure was added, i.e., a pointer to the first element of the
   *         structure.
   */
  public int add(int len, int val) {
    while ((pos + len) >= max) {
      grow();
    }
    final int pos1 = pos;
    pos += len;
    heap[pos1] = val;
    return pos1;
  }

  public int getNextId() {
    return pos;
  }

  public void grow(int len) {
    while ((pos + len) >= max) {
      grow();
    }
    pos += len;
  }

  // used by JCas to default the size the JCasHashMap
  public int getInitialSize() {
    return initialSize;
  }

  public int[] toArray() {
    int[] r = new int[pos];
    System.arraycopy(heap, 0, r, 0, pos);
    return r;
  }
}
