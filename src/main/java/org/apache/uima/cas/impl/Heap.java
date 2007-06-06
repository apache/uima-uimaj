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
import org.apache.uima.internal.util.SortedIntSet;

/**
 * A heap for CAS. Actually provides two heaps: a temporary one for per-document structures, and a
 * permanent one. The space in the temporary heap can be reused by calling
 * <code>resetTempHeap()</code>. Note though that the two heaps share an address space.
 * 
 * <p>
 * This class is agnostic about what you store on the heap. It only copies values from integer
 * arrays.
 * 
 * <p>
 * Finally, a word of caution. This class works internally with pages of a certain size (see
 * {@link Heap#DEFAULT_PAGE_SIZEDEFAULT_PAGE_SIZE}). It will not accept structures larger than this
 * size. You can set a larger value by using the appropriate constructor. Use a smaller value only
 * if you know exactly what you are doing.
 * 
 * 
 * @version $Id: Heap.java,v 1.8 2002/08/08 13:59:29 goetz Exp $
 */
public final class Heap {

  /**
   * Minimum size of internal pages. Currently set to <code>1000</code>.
   */
  public static final int MIN_PAGE_SIZE = 1000;

  /**
   * Default size of internal pages. Currently set to <code>500000</code>(2 MB).
   */
  public static final int DEFAULT_PAGE_SIZE = 500000; // 2 MB pages

  private int PAGE_SIZE;

  // The array that represents the actual heap is package private and
  // can be directly addressed by the LowLevelCAS.
  int[] heap;

  // private int size;
  private int tempPos;

  private int tempMax;

  private int permPos;

  private int permMax;

  private SortedIntSet availablePages;

  private SortedIntSet tempPages;

  // Serialization constants.
  private static final int SIZE_POS = 0;

  private static final int TMPP_POS = 1;

  private static final int TMPM_POS = 2;

  private static final int PRMP_POS = 3;

  private static final int PRMM_POS = 4;

  private static final int PGSZ_POS = 5;

  private static final int AVSZ_POS = 6;

  private static final int AVST_POS = 7;

  /**
   * Default constructor.
   */
  public Heap() {
    this(DEFAULT_PAGE_SIZE);
  }

  /**
   * Constructor lets you set page size. Use only if you know what you're doing.
   * 
   * @param pageSize
   *          The page size. If this is smaller than the {@link #MIN_PAGE_SIZE MIN_PAGE_SIZE}, the
   *          default will be used instead.
   */
  public Heap(int pageSize) {
    super();
    if (pageSize < MIN_PAGE_SIZE) {
      pageSize = MIN_PAGE_SIZE;
    }
    this.PAGE_SIZE = pageSize;
    this.initHeap();
  }

  private final void initHeap() {
    this.heap = new int[0];
    this.availablePages = new SortedIntSet();
    this.tempPages = new SortedIntSet();
    // System.out.println("Creating first temp page.");
    newTempPage();
    // System.out.println("Creating first perm page.");
    // newPermPage();
  }

  void reinit(int[] md, int[] shortHeap) {
    if (md == null) {
      reinitNoMetaData(shortHeap);
      return;
    }
    // assert(md != null);
    // assert(shortHeap != null);
    final int heapSize = md[SIZE_POS];
    this.tempPos = md[TMPP_POS];
    this.tempMax = md[TMPM_POS];
    this.permPos = md[PRMP_POS];
    this.permMax = md[PRMM_POS];
    this.PAGE_SIZE = md[PGSZ_POS];

    // Copy the shortened version of the heap into a full version.
    this.heap = new int[heapSize];
    System.arraycopy(shortHeap, 0, this.heap, 0, shortHeap.length);

    final int avMax = AVST_POS + md[AVSZ_POS];
    this.availablePages = new SortedIntSet();
    // assert(avMax <= md.length);
    for (int i = AVST_POS; i < avMax; i++) {
      this.availablePages.add(md[i]);
    }
    this.tempPages = new SortedIntSet();
    for (int i = avMax; i < md.length; i++) {
      this.tempPages.add(md[i]);
    }
  }

  /**
   * Re-init the heap without metadata (i.e., most likely from TAF). Note that we have no good way
   * of determining page size. We just use the size of the incoming heap, unless it's smaller than
   * our minimum. Something more sophisticated could be implemented, but let's see how we do with
   * this simple strategy first.
   * 
   * @param shortHeap
   */
  private void reinitNoMetaData(int[] shortHeap) {
    this.PAGE_SIZE = (shortHeap.length < MIN_PAGE_SIZE) ? MIN_PAGE_SIZE : shortHeap.length;
    this.availablePages = new SortedIntSet();
    this.tempPages = new SortedIntSet();
    if (shortHeap.length >= this.PAGE_SIZE) {
      this.heap = shortHeap;
    } else {
      System.arraycopy(shortHeap, 0, this.heap, 0, shortHeap.length);
    }
    this.tempPages.add(0);
    // Add the rest of the heap as available pages.
    int nextPageEnd = this.PAGE_SIZE * 2;
    // int nextPageStart = PAGE_SIZE;
    int pageNumber = 1;
    while (nextPageEnd <= this.heap.length) {
      this.availablePages.add(pageNumber);
      // nextPageStart = nextPageEnd;
      nextPageEnd += this.PAGE_SIZE;
      ++pageNumber;
    }
    // Set the temp position and max.
    this.tempPos = shortHeap.length;
    // Current max is the size of the first page we created. The rest of the
    // space is held in availablePages.
    this.tempMax = this.PAGE_SIZE;
    // Create perm page (never used in current implementation).
    // WARNING: if we ever want to use a permanent heap, this needs to go
    // back
    // in.
    // newPermPage();
  }

  /**
   * Re-create the heap for the given size. Just use the size of the incoming heap, unless it's
   * smaller than our minimum. It is expected that the caller will then fill in the new heap up to
   * newSize.
   * 
   * @param newSize
   */
  void reinitSizeOnly(int newSize) {
    this.PAGE_SIZE = (newSize < MIN_PAGE_SIZE) ? MIN_PAGE_SIZE : newSize;
    this.availablePages = new SortedIntSet();
    this.tempPages = new SortedIntSet();
    this.heap = new int[this.PAGE_SIZE];
    this.tempPages.add(0);
    // Add the rest of the heap as available pages.
    int nextPageEnd = this.PAGE_SIZE * 2;
    // int nextPageStart = PAGE_SIZE;
    int pageNumber = 1;
    while (nextPageEnd <= this.heap.length) {
      this.availablePages.add(pageNumber);
      // nextPageStart = nextPageEnd;
      nextPageEnd += this.PAGE_SIZE;
      ++pageNumber;
    }
    // Set the temp position and max.
    this.tempPos = newSize;
    // Current max is the size of the first page we created. The rest of the
    // space is held in availablePages.
    this.tempMax = this.PAGE_SIZE;
  }

  /**
   * Return the number of cells used, ignoring permanent structures.
   */
  int getCurrentTempSize() {
    // Since this is only for the purposes of the CASBean, and we know there
    // are no permanent feature structures, just return tempPos.
    return this.tempPos;
  }

  int[] getMetaData() {
    final int arSize = AVST_POS + this.availablePages.size() + this.tempPages.size();
    int[] ar = new int[arSize];
    ar[SIZE_POS] = this.heap.length;
    ar[TMPP_POS] = this.tempPos;
    ar[TMPM_POS] = this.tempMax;
    ar[PRMP_POS] = this.permPos;
    ar[PRMM_POS] = this.permMax;
    ar[PGSZ_POS] = this.PAGE_SIZE;
    final int availablePagesSize = this.availablePages.size();
    ar[AVSZ_POS] = availablePagesSize;

    int[] pageData = this.availablePages.toArray();
    for (int i = 0; i < pageData.length; i++) {
      ar[i + AVST_POS] = pageData[i];
    }
    final int tmpOffset = AVST_POS + availablePagesSize;
    pageData = this.tempPages.toArray();
    for (int i = 0; i < pageData.length; i++) {
      ar[i + tmpOffset] = pageData[i];
    }
    return ar;
  }

  private int newPage() {
    final int start = this.heap.length;
    // This will grow the heap by by exactly one page.
    this.heap = IntArrayUtils.ensure_size(this.heap, start + this.PAGE_SIZE, 2, this.PAGE_SIZE);
    return start;
  }

  private void newTempPage() {
    // First, check if we have an old page lying around.
    if (this.availablePages.size() > 0) {
      final int pageCode = this.availablePages.get(0);
      this.availablePages.remove(pageCode);
      this.tempPages.add(pageCode);
      if (pageCode == 0) {
        this.tempPos = 1;
        this.tempMax = this.PAGE_SIZE;
      }
       else {
      // this.tempPos = pageCode * this.PAGE_SIZE;
         this.tempMax = this.tempPos + this.PAGE_SIZE;
       }
      return;
    }
    // Allocate a new page.
    final int start = newPage();
    // Do not use 0 as a valid address.
    if (start == 0) {
      this.tempPos = 1;
    }
    // else {
    // this.tempPos = start;
    // }
    this.tempMax = start + this.PAGE_SIZE;
    this.tempPages.add(start / this.PAGE_SIZE);
  }

  private void newPermPage() {
    final int start = newPage();
    this.permPos = start;
    this.permMax = start + this.PAGE_SIZE;
  }

  /**
   * Reset the temporary heap.
   */
  public void resetTempHeap() {
    this.resetTempHeap(false);
  }

  /**
   * Reset the temporary heap.
   */
  void resetTempHeap(boolean doFullReset) {
    if (doFullReset) {
      this.initHeap();
    } else {
      // Reset the temp areas of the heap to 0. Each page n starts at
      // PAGE_SIZE*n.
      int pageNum, pageStart, pageEnd;
      for (int i = 0; i < this.tempPages.size(); i++) {
        pageNum = this.tempPages.get(i);
        pageStart = this.PAGE_SIZE * pageNum;
        pageEnd = pageStart + this.PAGE_SIZE;
        if ((pageStart <= this.tempPos) && (this.tempPos < pageEnd)) {
          // If this page is the current page, it is not completely
          // full
          // and we
          // only need to reset up to the point it's been filled.
          pageEnd = this.tempPos;
        }
        Arrays.fill(this.heap, pageStart, pageEnd, 0);
      }

      // Could do this slightly more efficiently, but this way, it's
      // clear what's going on.
      this.availablePages.union(this.tempPages);
      this.tempPages.removeAll();
      newTempPage();
    }
  }

  /**
   * Add a structure to the temporary heap.
   * 
   * @param fs
   *          The input structure.
   * @return The position where the structure was added, i.e., a pointer to the first element of the
   *         structure.
   */
  public int addToTempHeap(int[] fs) {
    while ((this.tempPos + fs.length) >= this.tempMax) {
      newTempPage();
    }
    System.arraycopy(fs, 0, this.heap, this.tempPos, fs.length);
    final int pos = this.tempPos;
    this.tempPos += fs.length;
    return pos;
  }

  /**
   * Reserve space for <code>len</code> items on the temp heap and set the first item to
   * <code>val</code>. The other items are set to <code>0</code>.
   * 
   * @param len
   *          The length of the new structure.
   * @param val
   *          The value of the first cell in the new structure.
   * @return The position where the structure was added, i.e., a pointer to the first element of the
   *         structure.
   */
  public int addToTempHeap(int len, int val) {
    while ((this.tempPos + len) >= this.tempMax) {
      newTempPage();
    }
    final int pos = this.tempPos;
    this.tempPos += len;
    this.heap[pos] = val;
    return pos;
  }

  /**
   * Add a structure to the permanent heap.
   * 
   * @param fs
   *          The input structure.
   * @return The position where the structure was added, i.e., a pointer to the first element of the
   *         structure.
   */
  public int addToHeap(int[] fs) {
    if (fs.length > this.PAGE_SIZE) {
      // Change this to an appropriate exception.
      throw new ArrayIndexOutOfBoundsException();
    }
    if ((this.permPos + fs.length) >= this.permMax) {
      newPermPage();
    }
    System.arraycopy(fs, 0, this.heap, this.permPos, fs.length);
    final int pos = this.permPos;
    this.permPos += fs.length;
    return pos;
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
  public int addToHeap(int len, int val) {
    if (len > this.PAGE_SIZE) {
      // Change this to an appropriate exception.
      throw new ArrayIndexOutOfBoundsException();
    }
    if ((this.permPos + len) >= this.permMax) {
      newPermPage();
    }
    final int pos = this.permPos;
    this.permPos += len;
    this.heap[pos] = val;
    return pos;
  }

}
