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

package org.apache.uima.jcas.impl;

import java.util.Arrays;

import org.apache.uima.cas.impl.FeatureStructureImpl;

/**
 * Version 2 (2014) of map between CAS addr and JCasCover Objects
 * 
 * Assumptions:  Each addr has a corresponding JCas; it is not
 * permitted to "update" an addr with a different JCas
 * cover class (unless the table is cleared first).
 * 
 * Table always a power of 2 in size - permits faster hashing
 * 
 * Accesses to this table are not threadsafe.
 * 
 * Load factor tuning. 2,000,000 random inserts, 50 reps (for JIT)
 *   .5 (2x to 4x entries)  99%  5 probes   250 ms
 *   .6 (1.67 to 3.3)       99%  6 probes   285 ms
 *   .7 (1.43 to 2.86)      99%  8 probes   318 ms 
 *   .8 (1.25 to 2.5)       99%  11 probes  360 ms
 *   
 *    version 1 at load factor .5 ran about 570 ms * 1.x 
 *      (did 2 lookups for fetches if not found,) 
 *   
 * Other changes: 
 *   remember finding empty slot on "miss" in get, 
 *     reuse on next put 
 *   change put to assume adding new item not already in table
 * 
 * 
 */
public class JCasHashMap {
  
  // set to true to collect statistics for tuning
  // you have to also put a call to jcas.showJfsFromCaddrHistogram() at the end of the run
  private static final boolean TUNE = false;
  
  //These are for tuning measurements
  private int histogram [];
  private int nbrProbes;
  private int maxProbe = 0;
  
  private int sizeWhichTriggersExpansion;
  
  private final float loadFactor = (float)0.50;
  
  private final int initialCapacity; 
  
  private int size; // number of elements in the table
  
  private FeatureStructureImpl [] table;
    
  // These are for hashing the CAS address
  private int bitsMask;  // 1's to "and" with result to keep in range 
  
  private final boolean useCache;
  
  private int indexOfLastFreeCell;

  // for testing only:
  int getbitsMask() {return bitsMask;}
  
  JCasHashMap(int capacity, boolean doUseCache) {
    this.useCache = doUseCache;
    capacity = Math.max(32, capacity);
    int bits = (32 - Integer.numberOfLeadingZeros(capacity - 1));
    // initialSize = 2, bits = 1, 2
    // initialSize = 3, bits = 2, 4
    // initialSize = 4, bits = 2, 4
    // initialSize = 5, bits = 3, 8
    // initialSize = 6, bits = 3
    // initialSize = 7, bits = 3
    // initialSize = 8, bits = 3
    capacity = 1<<bits;    // rounds up to next power of 32
    newTable(capacity);
    initialCapacity = capacity;
    if (TUNE) {
      histogram = new int[200];
      Arrays.fill(histogram, 0);
    }
  }
  
  private void newTable(int capacity) {
    assert(Integer.bitCount(capacity) == 1);
    table = new FeatureStructureImpl[capacity];
    bitsMask = capacity - 1;
    size = 0;
    sizeWhichTriggersExpansion = (int)(capacity * loadFactor);
  }
    
  // cleared when cas reset
  public void clear() {
    if (!this.useCache) {
      return;
    }
    size = 0;
    if (table.length >>> 4 > initialCapacity) {
      newTable(table.length >>> 1);  // shrink table
      return;
    }
    Arrays.fill(table, null);
  }

  public FeatureStructureImpl get(int key) {
    if (!this.useCache) {
      return null;
    }
    
    int probeAddr = hashInt(key);
    int probeDelta = 1;
    FeatureStructureImpl maybe = table[probeAddr];
    while ((null != maybe) && (maybe.getAddress() != key)) {
      if (TUNE) {
        nbrProbes++;
      }
      probeAddr = bitsMask & (probeAddr + (probeDelta++));
      maybe = table[probeAddr];
    }  

    if (TUNE) {
      histogram[Math.min(histogram.length - 1, nbrProbes)]++;
      maxProbe = Math.max(maxProbe, nbrProbes);
    }
    indexOfLastFreeCell = (maybe == null) ? probeAddr : -1;
    return maybe;    
  }
  
  public void findEmptySlot(int key) {
    if (!this.useCache) {
      return;
    }
    int probeAddr = hashInt(key);
    int probeDelta = 1;
    while (null != table[probeAddr]) {
      if (TUNE) {
        nbrProbes++;
      }
      probeAddr = bitsMask & (probeAddr + (probeDelta++));
    }
    if (TUNE) {
      histogram[Math.min(histogram.length - 1, nbrProbes)]++;
      maxProbe = Math.max(maxProbe, nbrProbes);
    }
    indexOfLastFreeCell = probeAddr;
  }
  
  /**
   * When put is called, the caller must already have just 
   * previously used get or findEmptySlot to set the indexOfLastFreeCell
   * @param value
   */
  public void putAfterFindingEmptyCell(FeatureStructureImpl value) {
    if (!this.useCache) {
      return;
    }
    if (size >= sizeWhichTriggersExpansion) {
      increaseSize();
      findEmptySlot(value.getAddress());  //reset the indexOfLastFreeCell
    }
    size++;
    table[indexOfLastFreeCell] = value;   
  }
  
  public void put(FeatureStructureImpl value) {
    findEmptySlot(value.getAddress());
    putAfterFindingEmptyCell(value);
  }
  
  public int size() {
    return size;
  }

  // The hash function is derived from murmurhash3 32 bit, which
  // carries this statement:
  
  //  MurmurHash3 was written by Austin Appleby, and is placed in the public
  //  domain. The author hereby disclaims copyright to this source code.  
  
  // See also MurmurHash3 in wikipedia
  
  private static final int C1 = 0xcc9e2d51;
  private static final int C2 = 0x1b873593;
  private static final int seed = 0x39c2ab57;  // arbitrary bunch of bits

  public int hashInt(int k1) {
    k1 *= C1;
    k1 = Integer.rotateLeft(k1, 15);
    k1 *= C2;
    
    int h1 = seed ^ k1;
    h1 = Integer.rotateLeft(h1, 13);
    h1 = h1 * 5 + 0xe6546b64;
    
    h1 ^= h1 >>> 16;  // unsigned right shift
    h1 *= 0x85ebca6b;
    h1 ^= h1 >>> 13;
    h1 *= 0xc2b2ae35;
    h1 ^= h1 >>> 16;
    return h1 & bitsMask;
  }
     
  private void increaseSize() {
    final FeatureStructureImpl [] oldTable = table; 
    final int oldCapacity = oldTable.length;
  
    int newCapacity = 2 * oldCapacity;
    
   if (TUNE)
      System.out.println("Size increasing from " + oldCapacity + " to " + newCapacity);
    newTable(newCapacity);
    size = 0;
    for (int i = 0; i < oldCapacity; i++) {
      FeatureStructureImpl fs = oldTable[i];
      if (fs != null) {
        put(fs);
      }   
    }    
  }
  
  public void showHistogram() {
    if (TUNE) {
      System.out.println("Histogram of number of probes, factor = " + loadFactor + ", max = "
              + maxProbe);
      for (int i = 0; i <= maxProbe; i++) {
        System.out.println(i + ": " + histogram[i]);
      }      
      System.out.println("bytes / entry = " + (float) (table.length) * 4 / size);
      System.out.format("size = %,d, prevExpansionTriggerSize = %,d, next = %,d%n",
          size,
          (int) ((table.length >>> 1) * loadFactor),
          (int) (table.length * loadFactor));
    }
  }
}
