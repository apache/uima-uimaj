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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.jcas.cas.TOP;

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
 * Multi-threading:  For read-only CASes, multiple iterators in 
 * different threads could be accessing the map and updating it.
 * 
 * Strategy: have 1 outer implementation delegating to multiple inner ones
 *   number = concurrency level (a power of 2)
 *   
 *   The hash would use some # of low order bits to address the right inner one. 
 */
public class JCasHashMap {

  // set to true to collect statistics for tuning
  // you have to also put a call to jcas.showJfsFromCaddrHistogram() at the end of the run
  private static final boolean TUNE = true;

  private static final int DEFAULT_CONCURRENCY_LEVEL;
  static {
    int cores = Runtime.getRuntime().availableProcessors();
    DEFAULT_CONCURRENCY_LEVEL = (cores < 17) ? cores :
                                (cores < 33) ? 16 + (cores - 16) / 2 : 
                                               24 + (cores - 24) / 4;
  }
    
  //These are for tuning measurements
  private int histogram [];
  private int maxProbe = 0;

  private final float loadFactor = (float)0.60;

  private final int initialCapacity; 

  private final boolean useCache;
  
  private final int concurrencyLevel;
  
  private final int concurrencyBitmask; 
  
  private final int concurrencyLevelBits;
  
  private final SubMap[] subMaps;
  
  private final AtomicInteger aggregate_size = new AtomicInteger(0);

  private final int subMapInitialCapacity;

  private class SubMap {
    private int sizeWhichTriggersExpansion;
    private int size; // number of elements in the table  
    private FeatureStructureImpl [] table;
    private boolean secondTimeShrinkable = false;
    private int bitsMask;  // 1's to "and" with result to keep in range   
  
    // for testing only:
    int getbitsMask() {return bitsMask;}
    
    private SubMap newTable(int capacity) {
      assert(Integer.bitCount(capacity) == 1);
      table = new FeatureStructureImpl[capacity];
      bitsMask = capacity - 1;
      size = 0;
      sizeWhichTriggersExpansion = (int)(capacity * loadFactor);
      return this;
    }
    
    private synchronized void clear() {
      // see if size is less than the 1/2 size that triggers expansion
      if (size <  (sizeWhichTriggersExpansion >>> 1)) {
        // if 2nd time then shrink by 50%
        //   this is done to avoid thrashing around the threshold
        if (secondTimeShrinkable) {
          secondTimeShrinkable = false;
          final int newCapacity = Math.max(subMapInitialCapacity, table.length >>> 1);
          if (newCapacity < table.length) { 
            newTable(newCapacity);  // shrink table by 50%
          } else { // don't shrink below minimum
            Arrays.fill(table,  null);
          }
          size = 0;
          return;
        } else {
          secondTimeShrinkable = true;
        }
      } else {
        secondTimeShrinkable = false; // reset this to require 2 triggers in a row
      }
      size = 0;
      Arrays.fill(table, null);
    }      
    
//    private synchronized FeatureStructureImpl get(int key, int hash) {
//      int nbrProbes = 1;
//      int probeAddr = hash & bitsMask;
//      int probeDelta = 1;
//      FeatureStructureImpl maybe = table[probeAddr];
//      while ((null != maybe) && (maybe.getAddress() != key)) {
//        if (TUNE) {
//          nbrProbes++;
//        }
//        probeAddr = bitsMask & (probeAddr + (probeDelta++));
//        maybe = table[probeAddr];
//      }  
//
//      if (TUNE) {
//        histogram[Math.min(histogram.length - 1, nbrProbes)]++;
//        maxProbe = Math.max(maxProbe, nbrProbes);
//      }
//      return maybe;    
//    }
    
    /**
     * Gets a value, but if the value isn't there, it reserves the slot where it will go
     * with a new instance where the key matches, but the type is null
     * @param key - the addr in the heap
     * @param hash - the hash that was already computed from the key
     * @return - the found fs, or null
     */
    private synchronized FeatureStructureImpl getReserve(int key, int hash) {
      int nbrProbes = 1;
      int probeAddr = hash & bitsMask;
      int probeDelta = 1;
      FeatureStructureImpl maybe = table[probeAddr];
      while ((null != maybe)) {
        if (maybe.getAddress() == key) {
          while (((TOP)maybe).jcasType == null) {
            // we hit a reserve marker - there is another thread in the process of creating an instance of this,
            // so wait for it to finish and then return it
            try {
              wait();  // releases the synchronized monitor, otherwise this segment blocked for others while waiting
            } catch (InterruptedException e) {
            }
            maybe = table[probeAddr];
          }
          if (TUNE) {
            histogram[Math.min(histogram.length - 1, nbrProbes)]++;
            maxProbe = Math.max(maxProbe, nbrProbes);
          }
          return maybe;
        }
        // is not null, but is wrong key
        if (TUNE) {
          nbrProbes++;
        }
        probeAddr = bitsMask & (probeAddr + (probeDelta++));
        maybe = table[probeAddr];
      }
      
      // maybe is null
        // reserve this slot to prevent other "getReserved" calls for this same instance from succeeding, 
        // causing them to wait until this slot gets filled in with a FS value
      table[probeAddr] = new TOP(key, null);  // null indicates its a RESERVE marker
      
     
      if (TUNE) {
        histogram[Math.min(histogram.length - 1, nbrProbes)]++;
        maxProbe = Math.max(maxProbe, nbrProbes);
      }
      return maybe;    
    }

    
    private synchronized void put(int key, FeatureStructureImpl value, int hash) {
      if (size >= sizeWhichTriggersExpansion) {
        increaseTableCapacity();
      }
      size++;
      
      int probeAddr = hash & bitsMask;
      int probeDelta = 1;
      int nbrProbes = 1;
      FeatureStructureImpl m = table[probeAddr];
      while (null != m) {
        if (((TOP)m).jcasType == null) { 
          // this slot was previously reserved - check to see if the key matches
          // (must be same key, otherwise, impl could deadlock)
          if (m.getAddress() == key) {
            // found the previously reserved slot
            table[probeAddr] = value;
            aggregate_size.incrementAndGet();
            notifyAll();
            if (TUNE) {
              histogram[Math.min(histogram.length - 1, nbrProbes)]++;
              maxProbe = Math.max(maxProbe, nbrProbes);
            }
            return;
          }
        }
        
        // skip if adding the same element to the table
        // probably never happens, though
        if (m.getAddress() == key) {
          if (TUNE) {
            System.err.format("JCasHashMap found already existing cover instance for key %,d, ignoring put%n", key);
            throw new RuntimeException(); //to get stack trace
          }
        }
        if (TUNE) {
          nbrProbes++;
        }
        probeAddr = bitsMask & (probeAddr + (probeDelta++));
        m = table[probeAddr];
      }
      if (TUNE) {
        histogram[Math.min(histogram.length - 1, nbrProbes)]++;
        maxProbe = Math.max(maxProbe, nbrProbes);
      }
      table[probeAddr] = value;
      aggregate_size.incrementAndGet();
    }
    
//    private synchronized FeatureStructureImpl putIfAbsent(
//        int key, 
//        Callable<FeatureStructureImpl> valueProducer, 
//        int hash) throws Exception {
//      int nbrProbes = 1;
//      int probeAddr = hash & bitsMask;
//      int probeDelta = 1;
//      FeatureStructureImpl maybe = table[probeAddr];
//      while ((null != maybe) && (maybe.getAddress() != key)) {
//        if (TUNE) {
//          nbrProbes++;
//        }
//        probeAddr = bitsMask & (probeAddr + (probeDelta++));
//        maybe = table[probeAddr];
//      }
//      
//      if (TUNE) {
//        histogram[Math.min(histogram.length - 1, nbrProbes)]++;
//        maxProbe = Math.max(maxProbe, nbrProbes);
//      }
//      
//      if (null == maybe) {
//        table[probeAddr] = maybe = valueProducer.call();
//        aggregate_size.incrementAndGet();
//      }
//      return maybe;    
//    }
    
//    private int findEmptySlot(int key, int hash) {
//      int probeAddr = hash & bitsMask;
//      int probeDelta = 1;
//      int nbrProbes = 1;
//      FeatureStructureImpl m = table[probeAddr];
//      while (null != m) {
//        if (((TOP)m).jcasType == null) { 
//          // this slot was previously reserved - check to see if the key matches
//          // (must be same key, otherwise, impl could deadlock)
//          if (m.getAddress() == key) {
//            
//          }
//        if (TUNE) {
//          nbrProbes++;
//        }
//        probeAddr = bitsMask & (probeAddr + (probeDelta++));
//      }
//      if (TUNE) {
//        histogram[Math.min(histogram.length - 1, nbrProbes)]++;
//        maxProbe = Math.max(maxProbe, nbrProbes);
//      }
//      return probeAddr;
//    }

    private void increaseTableCapacity() {
      final FeatureStructureImpl [] oldTable = table; 
      final int oldCapacity = oldTable.length;
    
      int newCapacity = 2 * oldCapacity;
      
      if (TUNE) {
        System.out.println("Size increasing from " + oldCapacity + " to " + newCapacity);
      }
      newTable(newCapacity);
      size = 0;
      for (int i = 0; i < oldCapacity; i++) {
        FeatureStructureImpl fs = oldTable[i];
        if (fs != null) {
          int key = fs.getAddress();
          int hash = hashInt(key);
          put(key, fs, hash >>> concurrencyLevelBits);
        }   
      }
    }
  }
  
  JCasHashMap(int capacity, boolean doUseCache) {
    this(capacity, doUseCache, DEFAULT_CONCURRENCY_LEVEL);
  }
  
  JCasHashMap(int capacity, boolean doUseCache, int aConcurrencyLevel) {
    this.useCache = doUseCache;
    concurrencyLevel = (aConcurrencyLevel > 1) ? 
        Integer.highestOneBit(1 + aConcurrencyLevel) :  // 1 to 128
        1;
    concurrencyBitmask = concurrencyLevel - 1;
    concurrencyLevelBits = Integer.numberOfTrailingZeros(concurrencyLevel); 
    capacity = Integer.highestOneBit(1 + Math.max(31, Math.max(concurrencyLevel, capacity)));
    // initialSize = 2, bits = 1, 2
    // initialSize = 3, bits = 2, 4
    // initialSize = 4, bits = 2, 4
    // initialSize = 5, bits = 3, 8
    // initialSize = 6, bits = 3
    // initialSize = 7, bits = 3
    // initialSize = 8, bits = 3
    initialCapacity = capacity;
    subMaps = new SubMap[concurrencyLevel];
    subMapInitialCapacity = initialCapacity / concurrencyLevel;  // always 2 or more
    for (int i = 0; i < concurrencyLevel; i++) {
      subMaps[i] = (new SubMap()).newTable(subMapInitialCapacity);
    }
    if (TUNE) {
      histogram = new int[200];
      Arrays.fill(histogram, 0);
    }
  }
      
  // cleared when cas reset
  // storage management:
  //   shrink if current number of entries
  //      wouldn't trigger an expansion if the size was reduced by 1/2 
  public synchronized void clear() {
    if (!this.useCache) {
      return;
    }
    for (SubMap m : subMaps) {
      m.clear();
    }
    aggregate_size.set(0);
  }

//  public FeatureStructureImpl get(int key) {
//    if (!this.useCache) {
//      return null;
//    }
//    int hash = hashInt(key);
//    int subMapIndex = hash & concurrencyBitmask;
//    
//    SubMap m = subMaps[subMapIndex];
//    return m.get(key, hash >>> concurrencyLevelBits);    
//  }
  
  public FeatureStructureImpl getReserve(int key) {
    if (!this.useCache) {
      return null;
    }
    int hash = hashInt(key);
    int subMapIndex = hash & concurrencyBitmask;
    
    SubMap m = subMaps[subMapIndex];
    return m.getReserve(key, hash >>> concurrencyLevelBits);    
  }

  
//  public FeatureStructureImpl putIfAbsent(int key, Callable<FeatureStructureImpl> valueProducer) throws Exception {
//    if (!this.useCache) {
//      return valueProducer.call();
//    }
//    int hash = hashInt(key);
//    int subMapIndex = hash & concurrencyBitmask;
//    
//    SubMap m = subMaps[subMapIndex];
//    return m.putIfAbsent(key, valueProducer, hash >>> concurrencyLevelBits);   
//  }
//  

  
  public void put(FeatureStructureImpl value) {
    if (!this.useCache) {
      return;
    }
    int key = value.getAddress();
    int hash = hashInt(key);
    int subMapIndex = hash & concurrencyBitmask;
    
    SubMap m = subMaps[subMapIndex];
    m.put(key, value, hash >>> concurrencyLevelBits);
  }
  
  public int size() {
    return aggregate_size.get();
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
    return h1;
  }
     
  int[] getCapacities() {
    int[] r = new int[subMaps.length];
    int i = 0;
    for (SubMap subMap : subMaps) {
      r[i++] = subMap.bitsMask + 1;
    }
    return r;
  }
  
  public void showHistogram() {
    if (TUNE) {
      System.out.println("Histogram of number of probes, factor = " + loadFactor + ", max = "
              + maxProbe);
      for (int i = 0; i <= maxProbe; i++) {
        System.out.println(i + ": " + histogram[i]);
      }     
      int agg_tableLength = 0;
      for (SubMap m : subMaps) {
        agg_tableLength += m.table.length;
      }
      System.out.println("bytes / entry = " + (float) (agg_tableLength) * 4 / size());
      System.out.format("size = %,d, prevExpansionTriggerSize = %,d, next = %,d%n",
          size(),
          (int) ((agg_tableLength >>> 1) * loadFactor),
          (int) (agg_tableLength * loadFactor));
    }
  }
}
