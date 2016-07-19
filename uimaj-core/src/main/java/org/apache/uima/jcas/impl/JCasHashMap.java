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

import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.internal.util.Utilities;

/**
 * Version 2 (2014) of map between CAS addr and JCasCover Objects
 * 
 * Note: in the general case, the cover object may *not* be a JCas one, but rather the general one
 *       This happens if there is no JCas cover object defined for the type.
 * 
 * Assumptions:  Each addr has a corresponding JCas; it is not
 * permitted to "update" an addr with a different JCas
 * cover class (unless the table is cleared first).
 * 
 * Table always a power of 2 in size - permits faster hashing
 * 
 * Accesses to this table are threadsafe, in order to support
 * read-only CASes being shared by multiple threads. 
 * Multiple iterators in different threads could be accessing the map and updating it.
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
 * No "get" method, only getReserve.  This method, if it doesn't
 * find the key, eventually finds an empty (null) slot - it then
 * stores a special "reserve" item with the same key value in that slot.
 *    Other threads doing getReserve calls, upon encountering this 
 *    reserve item, wait until the reserve is converted to a
 *    real value (a notifyAll happens when this is done), and
 *    then the getReserve returns the real item.
 *    
 * getReserve calls - when they find the item operate without any locking
 *    
 * Locking:
 *   There is one lock used for reading and updating the table
 *     -- not used for reading when item found, only if item not found or is reserved  
 * 
 * Strategy: have 1 outer implementation delegating to multiple inner ones
 *   number = concurrency level (a power of 2)
 *   
 *   The hash uses some # of low order bits to address the right inner one.
 *   
 *  This table is used to hold JCas cover classes for CAS feature structures.  
 *  There is one instance of this table associated with each CAS that is using it.
 * <p> 
 * The update occurs in the code in JCasGenerated classes, which do:
 *        a call to get the value of the map for a key
 *        if that is "null", it creates the new JCas cover object, and does a "put" to add the value.
 * <p> 
 * The creation of the new JCas cover object can, in turn, run arbitrary user code, 
 * which can result in updates to the JCasHashMap which occur before this original update occurs.
 * <p>
 * In a multi-threaded environment, multiple threads can do a "get" 
 * for the same Feature Structure instance.  If it's not in the Map, the correct behavior is:
 * <p>
 * one of the threads adds the new element
 * the other threads wait for the one thread to finish adding, and then return the object that the one thread added.
 * <p>
 * The implementation works as follows:
 * <p>
 * 1) The JCasHashMap is split into "n" sub-maps.   
 *    The number is the number of cores, but grows more slowly as the # of cores &gt; 16. 
 *    This number can be specified, but this is not currently exposed in the tuning parameters
 *    Locking occurs on the sub-maps; the outer method calls are not synchronized
 * 2) The number of sub maps is rounded to a power of 2, to allow the low order bits of the hash of the key 
 *     to be used to pick the map (via masking).
 * 3) A getReserve that results in not-found returns a null, but adds to the table a special reserved element.
 * 3a) This adding may result in table resizing
 * 4) A getReserve that finds a special reserved element, knows that some other thread 
 *    is in the process of adding an entry for that key, so it waits.
 * 5) A put, if it finds a reserved-for-that-key element, replaces that with the real element, 
 *    and then does a notifyAll to wake up any threads that were waiting (on this sub-map), 
 *    and these threads then re-do the get.  Multiple threads could be waiting on this, and they will all 
 *    wake-up.
 * <p>
 * All calls are of the getReserved, followed by a put if the getReserved returns null.
 *
 * (Experiment - disabled after no change noted
 * To improve locality of reference, an aux data structure of size to fit in one cache line of a Power7 (128 bytes)
 * caches the latest lookups)
 *   
 */
public class JCasHashMap {

  // set to true to collect statistics for tuning
  // you have to also put a call to jcas.showJfsFromCaddrHistogram() at the end of the run
  static final boolean TUNE = false;
  static final boolean check = false;  // message if concurrency level reduced because initial size was small
//  private static final boolean MEASURE_CACHE = false /* Misc.getNoValueSystemProperty("uima.measure.jcas.hashmap.cache")*/;


  /** 
   * must be a power of 2, > 0 
   * package private for testing
   * 
   * not final to allow test case to reset it
   * must not be changed during multi-thread operation
   * 
   */
  static int DEFAULT_CONCURRENCY_LEVEL;
  
  static {
    // high concurrency can increase L1 cache dumping
    DEFAULT_CONCURRENCY_LEVEL =  // approx between 10-20% of the number of cores dropping to 5% at high core values (to control cache loading)
        // min is 1
        // max is 16 (the number of l1 slots is 256 in some high performance cpus (2015) so going higer than this 
        //  probably has too much cache loading
        // 
        1 + (int)(Utilities.numberOfCores * 
                   ((Utilities.numberOfCores > 64) ? .08 : 
                    (Utilities.numberOfCores > 32) ? .1  :
                    (Utilities.numberOfCores > 16) ? .2  :
                    (Utilities.numberOfCores > 8)  ? .3  : .4));
  }

  static int getDEFAULT_CONCURRENCY_LEVEL() {
    return DEFAULT_CONCURRENCY_LEVEL;
  }
  
  // used in test cases
  static void setDEFAULT_CONCURRENCY_LEVEL(int dEFAULT_CONCURRENCY_LEVEL) {
    DEFAULT_CONCURRENCY_LEVEL = Utilities.nextHigherPowerOf2(dEFAULT_CONCURRENCY_LEVEL);
  }
  
//  // size set to ~1 cache line 
//  private final static int CACHE_SIZE = 32;  // running testcases: 16 -> 558,688 hits, 32 ->  850,300, 24 ->679,175
//  private final static AtomicLong cacheHits = new AtomicLong(0);
//  private final static AtomicLong cacheMisses = new AtomicLong(0);
    
  
  private final float loadFactor = (float)0.60;
    
  private final int initialCapacity; 

  private final boolean useCache;
  
  private final int concurrencyLevel;
  
  private final int concurrencyBitmask; 
  
  private final int concurrencyLevelBits;
  
  private final JCasHashMapSubMap[] subMaps;
  
  private final int subMapInitialCapacity;

  // optimization for concurrency level 1
  private final JCasHashMapSubMap oneSubmap;
  
//  // cache to improve locality of reference for lookup
//  private final FeatureStructureImpl[] cacheFS = new FeatureStructureImpl[CACHE_SIZE];  // one cache line is 32 words, save some for length and java object overhead
//  private final int[] cacheInt = new int[CACHE_SIZE];
//  private int cacheNewIndex = 0;
  
  JCasHashMap(int capacity, boolean doUseCache) {
    // reduce concurrency so that capacity / concurrency >= 32
    //   that is, minimum sub-table capacity is 32 entries
    // if capacity/concurrency < 32,
    //   concurrency = capacity / 32
    this(capacity, doUseCache,
        ((capacity / DEFAULT_CONCURRENCY_LEVEL) < 32) ?
            Utilities.nextHigherPowerOf2(
                Math.max(1, capacity / 32)) :
            DEFAULT_CONCURRENCY_LEVEL);
    if (check && (capacity / DEFAULT_CONCURRENCY_LEVEL) < 32) {
      System.out.println(String.format("JCasHashMap concurrency reduced, capacity: %,d DefaultConcur: %d, concur: %d%n",
          capacity, DEFAULT_CONCURRENCY_LEVEL, 
          Utilities.nextHigherPowerOf2(Math.max(1, capacity / 32))));
    }
  }
  
  JCasHashMap(int capacity, boolean doUseCache, int aConcurrencyLevel) {
    this.useCache = doUseCache;

    if (aConcurrencyLevel < 1|| capacity < 1) {
      throw new RuntimeException(String.format("capacity %d and concurrencyLevel %d must be > 0", capacity, aConcurrencyLevel));
    }
    concurrencyLevel = Utilities.nextHigherPowerOf2(aConcurrencyLevel);
    concurrencyBitmask = concurrencyLevel - 1;
    // for clvl=1, lvlbits = 0,  
    // for clvl=2  lvlbits = 1;
    // for clvl=4, lvlbits = 2;
    concurrencyLevelBits = Integer.numberOfTrailingZeros(concurrencyLevel); 
    
    // capacity is the greater of the passed in capacity, rounded up to a power of 2, or 32.
    capacity = Math.max(32, Utilities.nextHigherPowerOf2(capacity));
    // if capacity / concurrencyLevel <32, increase capacity
    if ((capacity / concurrencyLevel) < 32) {
      capacity = 32 * concurrencyLevel;
    }
    
    initialCapacity = capacity;
    
    subMaps = new JCasHashMapSubMap[concurrencyLevel];
    subMapInitialCapacity = initialCapacity / concurrencyLevel;  // always 32 or more
    for (int i = 0; i < concurrencyLevel; i++) {
      subMaps[i] = new JCasHashMapSubMap(loadFactor, subMapInitialCapacity, concurrencyLevelBits);
    }
    oneSubmap = concurrencyLevel == 1 ? subMaps[0] : null;
  }
  
  /**
   * initial capacity (other than testing), is by default (from JCasImpl) is bigger of
   *   256 and cas heap initial size (500,000) / 16 = 31K
   *   but users may set it lower in their uima configuration
   *   
   * We use the current capacity of the JCasHashMap to set the concurrency limit 
   * 
   * @param casCapacity the capacity
   * @return true if the concurrency is limited, and could increase with reallocation
   */
  // method used in JCasImpl, when clearing
  static boolean concurrencyLimitedByInitialCapacity(int currentConcurrencyLevel, int curMapSize) {
    if (DEFAULT_CONCURRENCY_LEVEL <= currentConcurrencyLevel) {
      return false;
    }
    
    int submapSize = curMapSize / DEFAULT_CONCURRENCY_LEVEL;
    
    int newConcurrencyLevel = (submapSize < 32) ?
        Utilities.nextHigherPowerOf2(
            Math.max(1, curMapSize / 32)) :
              DEFAULT_CONCURRENCY_LEVEL;
        
    return newConcurrencyLevel > currentConcurrencyLevel;     
  }
  
  static int sizeAdjustedConcurrency(int curMapSize) {
    int submapSize = curMapSize / DEFAULT_CONCURRENCY_LEVEL;
    
    int newConcurrencyLevel = (submapSize < 32) ?
        Utilities.nextHigherPowerOf2(
            Math.max(1, curMapSize / 32)) :
              DEFAULT_CONCURRENCY_LEVEL;
    return Math.max(32 * newConcurrencyLevel,   curMapSize / 2);  
  }
      
  // cleared when cas reset
  // storage management:
  //   shrink if current number of entries
  //      wouldn't trigger an expansion if the size was reduced by 1/2 
  public synchronized void clear() {
    if (!this.useCache) {
      return;
    }
    for (JCasHashMapSubMap m : subMaps) {
      m.clear();
    }
//    Arrays.fill(cacheFS,  null);
//    Arrays.fill(cacheInt, 0);
  }
    
  private JCasHashMapSubMap getSubMap(int hash) {
    return (null != oneSubmap) ? oneSubmap : subMaps[hash & concurrencyBitmask];
  }
  
  public FeatureStructureImpl getReserve(int key) {
    if (!this.useCache) {
      return null;
    }
//    for (int i = 0; i < cacheInt.length; i++) {
//      final int vi = cacheInt[i];
//      if (vi == 0) {
//        break;
//      }
//        
//      if (vi == key) {
//        if (MEASURE_CACHE) {
//          cacheHits.incrementAndGet();
//        }
//        FeatureStructureImpl fsi = cacheFS[i];
//        if (fsi.getAddress() != key) { // recheck to avoid sync 
//          break; // entry was overwritten
//        }
//        return fsi;
////          
////          if (i != 0) {
////            // manage lru  - measurement showed no significant boost
////            System.arraycopy(cache, 0, cache, 2, i);
////            cache[0] = keyI;
////            cache[1] = r;
////          }
////          return r;
////        }
//      }
//    }
        
//    if (MEASURE_CACHE) {
//      cacheMisses.incrementAndGet();  // includes creates
//    }
    final int hash = hashInt(key);
    final FeatureStructureImpl r = getSubMap(hash).getReserve(key, hash >>> concurrencyLevelBits);
    
//    if (r != null) {
//      updateCache(key, r);
//    }
    return r;
  }

//  private void updateCache(int key, FeatureStructureImpl value) {
//    final int newIdx = cacheNewIndex;
//    cacheNewIndex = (cacheNewIndex == (CACHE_SIZE - 1)) ? 0 : cacheNewIndex + 1;
//    cacheFS[cacheNewIndex] = value;  // update this first to avoid putting in the same value multiple times
//    cacheInt[cacheNewIndex] = key;
//  }
  
  public FeatureStructureImpl put(FeatureStructureImpl value) {
    if (!this.useCache) {
      return null;
    }
    final int key = value.getAddress();
//    updateCache(key, value);
    final int hash = hashInt(key);
    return getSubMap(hash).put(key, value, hash >>> concurrencyLevelBits);
  }
    
  // The hash function is derived from murmurhash3 32 bit, which
  // carries this statement:
  
  //  MurmurHash3 was written by Austin Appleby, and is placed in the public
  //  domain. The author hereby disclaims copyright to this source code.  
  
  // See also MurmurHash3 in wikipedia
  
  private static final int C1 = 0xcc9e2d51;
  private static final int C2 = 0x1b873593;
  private static final int seed = 0x39c2ab57;  // arbitrary bunch of bits

  public static int hashInt(int k1) {
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
     
  //test case use
  int[] getCapacities() {
    int[] r = new int[subMaps.length];
    int i = 0;
    for (JCasHashMapSubMap subMap : subMaps) {
      r[i++] = subMap.table.length;
    }
    return r;
  }
  
  int getCapacity() {
    int r = 0;
    for (JCasHashMapSubMap subMap : subMaps) {
      r += subMap.table.length;
    }
    return r;    
  }
  
  //test case use
  int getApproximateSize() {
    int s = 0;
    for (JCasHashMapSubMap subMap : subMaps) {
      synchronized (subMap) {
        s += subMap.size;
      }
    }
    return s;
  }
  
  public void showHistogram() {
    if (TUNE) {
      int sm = -1;
      int agg_tableLength = 0;
      for (JCasHashMapSubMap m : subMaps) {
        sm++;
        int sumI = 0;
        
        for (int i : m.histogram) {
          sumI += i;
        }
        
        System.out.format(
            "Histogram %d of number of probes, loadfactor = %.1f, maxProbe=%,d afterContinue=%,d nbr regs=%,d nbrContinues=%,d%n",
            sm, loadFactor, m.maxProbe, m.maxProbeAfterContinue, sumI, m.continues);
        for (int i = 0; i <= m.maxProbe; i++) {
          System.out.println(i + ": " + m.histogram[i]);
        }     
        agg_tableLength += m.table.length;
      }
      
      System.out.println("bytes / entry = " + (float) (agg_tableLength) * 4 / getApproximateSize());
      System.out.format("size = %,d, prevExpansionTriggerSize = %,d, next = %,d%n",
          getApproximateSize(),
          (int) ((agg_tableLength >>> 1) * loadFactor),
          (int) (agg_tableLength * loadFactor));
    }
  }
  
  public int getConcurrencyLevel() {
    return concurrencyLevel;
  }
    
//  private static final Thread dumpMeasurements = MEASURE_CACHE ? new Thread(new Runnable() {
//    @Override
//    public void run() {
//      System.out.println(String.format("JCasHashMap cores = %d cache hits: %,d miss: %,d percent: %d%n",
//          cores, 
//          cacheHits.get(), cacheMisses.get(), (100 * cacheHits.get()) / (cacheHits.get() + cacheMisses.get())));
//    }
//  }) : null;
//  
//  static {if (MEASURE_CACHE) {Runtime.getRuntime().addShutdownHook(dumpMeasurements);}}
}
