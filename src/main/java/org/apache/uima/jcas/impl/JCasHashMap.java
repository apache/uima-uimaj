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
import java.util.Random;

import org.apache.uima.cas.impl.FeatureStructureImpl;

/**
 * Special space-saving table that maps between CAS addr and
 * JCas cover objects.
 * 
 * Assumptions:  Each addr has a corresponding JCas; it is not
 * permitted to "update" an addr with a different JCas
 * cover class (unless the table is cleared first).
 * 
 * Table always a power of 2 in size - permits faster hashing
 *
 */
public class JCasHashMap {
  
  // set to true to collect statistics for tuning
  // you have to also put a call to jcas.showJfsFromCaddrHistogram() at the end of the run
  private static final boolean TUNE = false;
  
  // This inner class is only here to get access to the "next(bits)" method of Random
  private static class MyRandom extends Random {
    protected int next(int bits) {
      return super.next(bits);
    }
    private static final long serialVersionUID = 1L;   
  }

  //These are for tuning
  private int histogram [];
  private int nbrProbes;
  private int maxProbe = 0;
  
  private int sizeWhichTriggersExpansion;
  
  private float loadFactor = (float)0.50;
  
  private int size; // number of elements in the table
  
  private FeatureStructureImpl [] table;
  
  private MyRandom random = new MyRandom(); 
  
  // These are for hashing the CAS address
  private int bits;  // number of random bits to generate for a probe
  private int bitsMask;  // 1's to "and" with result to keep in range
  private int casAddr;  
  
  private final boolean useCache;
  
  JCasHashMap(int initialSize, boolean doUseCache) {
    this.useCache = true;
    // round initialSize to a power of 2
    int n = initialSize;
    int i = 0;
    while (n != 0) {
      i++;
      n = n >> 1;
    }
    // n = 1,       i = 1
    // n = 2,3      i = 2
    // n = 4,5,6,7  i = 3
    
    if (1<< (i - 1) == initialSize) {
      i = i - 1;  // if initial size was a power of 2, correct for that
    }
    // n = 2        i = 1
    // n = 3,4      i = 2
    // n = 5,6,7,8  i = 3
    bits = i;
    bitsMask = (1<<i) - 1;
    initialSize = 1<<i;
    table = new FeatureStructureImpl[initialSize];
    sizeWhichTriggersExpansion = (int)(initialSize * loadFactor);
    size = 0;
    if (TUNE) {
      histogram = new int[30];
      Arrays.fill(histogram, 0);
    }
  }
    
  public void clear() {
    if (!this.useCache) {
      return;
    }
    Arrays.fill(table, null);
    size = 0;
  }

  public FeatureStructureImpl get(int key) {
    if (!this.useCache) {
      return null;
    }
    FeatureStructureImpl maybe = table[probe(key)];
    while ((null != maybe) && (maybe.getAddress() != key)) {
      maybe = table[nextProbe()];
    }
    if (TUNE) {
      histogram[Math.min(histogram.length - 1, nbrProbes - 1)]++;
      maxProbe = Math.max(maxProbe, nbrProbes);
    }
    return maybe;    
  }

  public void put(FeatureStructureImpl value) {
    if (!this.useCache) {
      return;
    }
    final int key = value.getAddress();
    int probeAddr = probe(key);
    if (TUNE) {
      if (key < 200) {
        System.out.println("key = " + key + ", probe= " + probeAddr);
      }
    }
    while (null != table[probeAddr]) {
      probeAddr = nextProbe();
    }
    if (TUNE) {
      histogram[Math.min(histogram.length - 1, nbrProbes - 1)]++;
      maxProbe = Math.max(maxProbe, nbrProbes);
    }
    table[probeAddr] = value;
    size++;
    if (size >= sizeWhichTriggersExpansion) {
      increaseSize();
    }
  }

  public int size() {
    return size;
  }

  private int probe(int addr) {
    casAddr = addr;
    random.setSeed(addr);
    random.next(1);  // once to randomize all bits
    if (TUNE)
      nbrProbes = 0;
    return nextProbe();
  }
  
  private int nextProbe() {
    if (TUNE) {
      nbrProbes++;
    }
    // adding the casAddr insures the probe sequence is
    // different for different addrs, even if by chance the
    // random bit sequence is the same for two casAddrs.
    return (random.next(bits) + casAddr) & bitsMask;
  }
   
  private void increaseSize() {
    final FeatureStructureImpl [] oldTable = table; 
    final int oldCapacity = oldTable.length;
  
    int newCapacity = 2 * oldCapacity;
    bits += 1;
    bitsMask = (1<<bits) - 1;
    
    sizeWhichTriggersExpansion = (int)(newCapacity * loadFactor);
    if (TUNE)
      System.out.println("Size increasing from " + oldCapacity + " to " + newCapacity);
    table = new FeatureStructureImpl [newCapacity];
    size = 0;
    for (int i = 0; i < oldCapacity; i++) {
      if (oldTable[i] != null) {
        put(oldTable[i]);
      }   
    }
  }
  
  public void showHistogram() {
    if (TUNE) {
      System.out.println("Histogram of number of probes, factor = " + loadFactor + ", max = "
              + maxProbe);
      System.out.println("bytes / entry = " + (float) (table.length) * 4 / size);
      for (int i = 0; i < histogram.length; i++) {
        System.out.println(i + ": " + histogram[i]);
      }
    } 
  }
}
