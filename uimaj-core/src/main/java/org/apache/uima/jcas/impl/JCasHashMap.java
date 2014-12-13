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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.cas.TOP_Type;

/**
 * Version 2 (2014) of map between CAS addr and JCasCover Objects
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
 */
public class JCasHashMap {

  // set to true to collect statistics for tuning
  // you have to also put a call to jcas.showJfsFromCaddrHistogram() at the end of the run
  private static final boolean TUNE = false;

  static int nextHigherPowerOf2(int i) {
    return (i < 1) ? 1 : Integer.highestOneBit(i) << ( (Integer.bitCount(i) == 1 ? 0 : 1));
  }

  /** 
   * must be a power of 2, > 0 
   * package private for testing
   * 
   * not final to allow test case to reset it
   */
  static int DEFAULT_CONCURRENCY_LEVEL;
  
  static {
    int cores = Runtime.getRuntime().availableProcessors();
    // cores:lvl   0:1  1:1  2:2  3-15:4  16-31:8  32-63:16  else 32  
    DEFAULT_CONCURRENCY_LEVEL = 
        (cores <= 1)  ? 1 :
        (cores <= 4)  ? 2 :  // assumption: cores used for other work, too
        (cores <= 32) ? 4 :
        (cores <= 128) ? 8 :
                        16;   // emprical guesswork, not scientifically set    
  }

  static int getDEFAULT_CONCURRENCY_LEVEL() {
    return DEFAULT_CONCURRENCY_LEVEL;
  }
  
  static void setDEFAULT_CONCURRENCY_LEVEL(int dEFAULT_CONCURRENCY_LEVEL) {
    DEFAULT_CONCURRENCY_LEVEL = nextHigherPowerOf2(dEFAULT_CONCURRENCY_LEVEL);
//    System.out.println("JCasHashMap setting concurrency level to " + DEFAULT_CONCURRENCY_LEVEL);
  }

  private static final int PROBE_ADDR_INDEX = 0;
  private static final int PROBE_DELTA_INDEX = 1;
    
  private static class ReserveTopType extends TOP_Type {
    public ReserveTopType() {
      super();
    }
  }
  
  // package private for test case use
  static final TOP_Type RESERVE_TOP_TYPE_INSTANCE = new ReserveTopType(); 
  
  private static boolean isReserve(FeatureStructureImpl m) {
    return m != null && ((TOP)m).jcasType == RESERVE_TOP_TYPE_INSTANCE;
  }
  private static boolean isReal(FeatureStructureImpl m) {
    return m != null && ((TOP)m).jcasType != RESERVE_TOP_TYPE_INSTANCE;
  }
  
  private static int[] resetProbeInfo(int[] probeInfo) {
    probeInfo[PROBE_ADDR_INDEX] = -1;
    probeInfo[PROBE_DELTA_INDEX] = 1;
    return probeInfo;
  }
  
  private static int[] setProbeInfo(int[] probeInfo, int probeAddr, int probeDelta) {
    probeInfo[PROBE_ADDR_INDEX] = probeAddr;
    probeInfo[PROBE_DELTA_INDEX] = probeDelta;
    return probeInfo;    
  }
  
  private final float loadFactor = (float)0.60;
    
  private final int initialCapacity; 

  private final boolean useCache;
  
  private final int concurrencyLevel;
  
  private final int concurrencyBitmask; 
  
  private final int concurrencyLevelBits;
  
  private final SubMap[] subMaps;
  
  private final int subMapInitialCapacity;

  // optimization for concurrency level 1
  private final SubMap oneSubmap;

  private class SubMap {

    //These are for tuning measurements
    private int histogram [];
    private int maxProbe = 0;
    private int maxProbeAfterContinue = 0;
    private int continues = 0;;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition lockCondition = lock.newCondition();
    
    private int sizeWhichTriggersExpansion;
    private int size; // number of elements in the table  
    private volatile FeatureStructureImpl [] table;
    private boolean secondTimeShrinkable = false;
    
    private SubMap newTable(int capacity) {
      table = newTableKeepSize(capacity);
      size = 0;
      if (TUNE) {
        histogram = new int[200];
        Arrays.fill(histogram, 0);
      }
      return this;
    }
    
    private FeatureStructureImpl[] newTableKeepSize(int capacity) {
      assert(Integer.bitCount(capacity) == 1);
      FeatureStructureImpl[] t = new FeatureStructureImpl[capacity];
      sizeWhichTriggersExpansion = (int)(capacity * loadFactor);
      return t;
    }
    
    private void incrementSize() {
      assert(lock.getHoldCount() > 0);
      if (size >= sizeWhichTriggersExpansion) {
        increaseTableCapacity();
      }
      size++;
    }
    
    // Does size management - shrinking overly large tables after the 2nd time
    private void clear() {
      lock.lock();
      try {
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
      } finally {
        lock.unlock();
      }
    }      
     
    /**
     * Can be called under lock or not.
     * It gets a ref to the current value of table, and then searches that int array.
     *   If, during the search, the table is resized, it continues using the
     *   ** before the resize ** int array referenced by localTable 
     *     The answer will only be OK if the key is found for a real value.  
     *     Results that yield null or Reserved slots must be re-searched, 
     *     under a lock (caller needs to do this).
     * @param key -
     * @param hash -
     * @param probeInfo - used to get/receive multiple int values;
     *    0: (in/out) startProbe or -1, 
     *    1: (in/out) probeDelta (starts at 1)
     * @return the probeAddr in original table (which might have been resized)
     */
    private FeatureStructureImpl find(final int key, final int hash, final int[] probeInfo) {
      int nbrProbes = 1;
      final boolean isContinue = TUNE && (probeInfo[PROBE_ADDR_INDEX] != -1); 
      FeatureStructureImpl[] localTable = table;
      final int bitMask = localTable.length - 1;
      final int startProbe = probeInfo[PROBE_ADDR_INDEX];      
      int probeAddr = (startProbe < 0) ? (hash & bitMask) : startProbe; 
      assert((startProbe < 0) ? probeInfo[PROBE_DELTA_INDEX] == 1 : true);
      int probeDelta = probeInfo[PROBE_DELTA_INDEX];
      FeatureStructureImpl m = localTable[probeAddr];

      while (null != m) {  // loop to traverse bucket chain
        if (m.getAddress() == key) {
          setProbeInfo(probeInfo, probeAddr, probeDelta);
          if (TUNE) {
            
            /* LOCK if not already, to update stats */
            final boolean needUnlock;
            if (!lock.isHeldByCurrentThread()) {
              lock.lock();
              needUnlock = true;
            } else {
              needUnlock = false;
            }
            
            try {
              histogram[nbrProbes] += 1;
              if (maxProbe < nbrProbes) {
                maxProbe = nbrProbes;
              }
              if (isContinue) {
                if (maxProbeAfterContinue < nbrProbes) {
                  maxProbeAfterContinue = nbrProbes;
                }
                continues ++;
              }
            } finally {
              if (needUnlock) {
                lock.unlock();
              }
            }
          }
          return m;
        }
        nbrProbes++;
        probeAddr = bitMask & (probeAddr + (probeDelta++));
        m = localTable[probeAddr];
      }
      setProbeInfo(probeInfo, probeAddr, probeDelta);
      return m;
    }
        
    /**
     * Gets a value, but if the value isn't there, it reserves the slot where it will go
     * with a new instance where the key matches, but the type is a unique value.
     * 
     * Threading: not synchronized for main path where get is finding an element.
     *   Since elements are never updated, there is no race if an element is found.
     *   And it doesn't matter if the table is resized (if the element is found).
     *   If it is not found, or a reserve is found, need to get the lock, and
     *     start over if resized, or
     *     continue from reserved or null spot if not    
     *
     * @param key - the addr in the heap
     * @param hash - the hash that was already computed from the key
     * @return - the found fs, or null
     */
    private FeatureStructureImpl getReserve(final int key, final int hash) {

      boolean isLocked = false;
      int[] probeInfo = new int[2];
      try {
 
     retry:
        while (true) { // loop back point after locking against updates, to re-traverse the bucket chain from the beginning
          probeInfo = resetProbeInfo(probeInfo);
          final FeatureStructureImpl m;
          final FeatureStructureImpl[] localTable = table;
          
          if (isReal(m = find(key, hash, probeInfo))) {
            return m;  // fast path for found item       
          }
          
          // is reserve or null. Redo or continue search under lock
          // need to do this for reserve-case because otherwise, could 
          //   wait, but notify could come before wait - hence, wait forever
          if (!isLocked) {
            lock.lock();
            isLocked = true;
          }
          /*****************
           *    LOCKED     *
           *****************/
          if (localTable != table) {
            // redo search from top, because table resized
            resetProbeInfo(probeInfo);
          }
//          // re acquire the FeatureStructure m, because another thread could change it before the lock got acquired
//          final FeatureStructureImpl m2 = localTable[probeInfo[PROBE_ADDR_INDEX]];
//          if (isReal(m2)) {
//            return m2;  // another thread snuck in before the lock and switched this
//          }
          
          // note: localTable not used from this point, unless reset
          // note: this "find" either finds from the top (if size changed) or finds from current spot.
          final FeatureStructureImpl m2 = find(key, hash, probeInfo);
          if (isReal(m2)) {
            return m2;  // fast path for found item       
          }
          
          while (isReserve(m2)) {
            final FeatureStructureImpl[] localTable2 = table;  // to see if table gets resized
            // can't wait on reserved item because would need to do lock.unlock() followed by wait, but
            //   inbetween these, another thread could already do the notify.
            try {
              /**********
               *  WAIT  *
               **********/
              lockCondition.await();  // wait on object that needs to be unlocked
            } catch (InterruptedException e) {
            }

            // at this point, the lock was released, and re-aquired
            if (localTable2 != table) { // table was resized
               continue retry;
            }
            final FeatureStructureImpl m3 = localTable2[probeInfo[PROBE_ADDR_INDEX]];
            if (isReserve(m3)) {
              continue;  // case = interruptedexception && no resize && not changed to real, retry
            }
            return m3; // return real item
          }
          
          /*************
           *  RESERVE  *
           *************/
          // is null. Reserve this slot to prevent other "getReserved" calls for this same instance from succeeding,
          // causing them to wait until this slot gets filled in with a FS value
          // Use table, not localTable, because resize may have occurred
          table[probeInfo[PROBE_ADDR_INDEX]] = new TOP(key, RESERVE_TOP_TYPE_INSTANCE);
          incrementSize();          
          return null;
        }
      } finally {
        if (isLocked) {
          lock.unlock();
        }
      }
    }
      
        
    private FeatureStructureImpl put(int key, FeatureStructureImpl value, int hash) {

      lock.lock();
      try {
        final int[] probeInfo = resetProbeInfo(new int[2]);
        FeatureStructureImpl prevValue = find(key, hash, probeInfo);
        table[probeInfo[PROBE_ADDR_INDEX]] = value;
        if (isReserve(prevValue)) {
          lockCondition.signalAll();
          // dont update size - was updated when reserve was added
          return null;
        } else if (prevValue == null) {
            incrementSize();  // otherwise, adding a new value
        } // else updating an existing value - don't increment the size
        return prevValue;
      } finally {
        lock.unlock();
      }
    }
     
       
   /**
    * Only used to fill in newly expanded table
    * @param key -
    * @param value -
    * @param hash -
    */
    
    private void putInner(int key, FeatureStructureImpl value, int hash) {
      assert(lock.getHoldCount() > 0);
      final int[] probeInfo = resetProbeInfo(new int[2]);
      
      FeatureStructureImpl m = find(key, hash, probeInfo);
      assert(m == null);  // no dups in original table imply no hits in new one
      table[probeInfo[PROBE_ADDR_INDEX]] = value;
    }
      

    // called under lock
    private void increaseTableCapacity() {
      final FeatureStructureImpl [] oldTable = table; 
      final int oldCapacity = oldTable.length;
      int newCapacity = 2 * oldCapacity;
      
      if (TUNE) {
        System.out.println("Capacity increasing from " + oldCapacity + " to " + newCapacity);
      }
      table = newTableKeepSize(newCapacity);
      for (int i = 0; i < oldCapacity; i++) {
        FeatureStructureImpl fs = oldTable[i];
        if (fs != null) {
          final int key = fs.getAddress();
          final int hash = hashInt(key);
          putInner(key, fs, hash >>> concurrencyLevelBits);
        }   
      }
    }
  }
  
  JCasHashMap(int capacity, boolean doUseCache) {
    // reduce concurrency so that capacity / concurrency >= 32
    //   that is, minimum sub-table capacity is 32 entries
    // if capacity/concurrency < 32,
    //   concurrency = capacity / 32
    this(capacity, doUseCache,
        ((capacity / DEFAULT_CONCURRENCY_LEVEL) < 32) ?
            nextHigherPowerOf2(
                Math.max(1, capacity / 32)) :
            DEFAULT_CONCURRENCY_LEVEL);
  }
  
  JCasHashMap(int capacity, boolean doUseCache, int aConcurrencyLevel) {
    this.useCache = doUseCache;

    if (aConcurrencyLevel < 1|| capacity < 1) {
      throw new RuntimeException(String.format("capacity %d and concurrencyLevel %d must be > 0", capacity, aConcurrencyLevel));
    }
    concurrencyLevel = nextHigherPowerOf2(aConcurrencyLevel);
    concurrencyBitmask = concurrencyLevel - 1;
    // for clvl=1, lvlbits = 0,  
    // for clvl=2  lvlbits = 1;
    // for clvl=4, lvlbits = 2;
    concurrencyLevelBits = Integer.numberOfTrailingZeros(concurrencyLevel); 
    
    // capacity is the greater of the passed in capacity, rounded up to a power of 2, or 32.
    capacity = Math.max(32,  nextHigherPowerOf2(capacity));
    // if capacity / concurrencyLevel <32, increase capacity
    if ((capacity / concurrencyLevel) < 32) {
      capacity = 32 * concurrencyLevel;
    }
    
    initialCapacity = capacity;
    
    subMaps = new SubMap[concurrencyLevel];
    subMapInitialCapacity = initialCapacity / concurrencyLevel;  // always 32 or more
    for (int i = 0; i < concurrencyLevel; i++) {
      subMaps[i] = (new SubMap()).newTable(subMapInitialCapacity);
    }
    oneSubmap = concurrencyLevel == 1 ? subMaps[0] : null;
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
  }
  
  private SubMap getSubMap(int hash) {
    return (null != oneSubmap) ? oneSubmap : subMaps[hash & concurrencyBitmask];
  }
  
  public FeatureStructureImpl getReserve(int key) {
    if (!this.useCache) {
      return null;
    }
    final int hash = hashInt(key);
    return getSubMap(hash).getReserve(key, hash >>> concurrencyLevelBits);
  }

  public FeatureStructureImpl put(FeatureStructureImpl value) {
    if (!this.useCache) {
      return null;
    }
    final int key = value.getAddress();
    final int hash = hashInt(key);
    return getSubMap(hash).put(key,  value,  hash >>> concurrencyLevelBits);
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
    for (SubMap subMap : subMaps) {
      r[i++] = subMap.table.length;
    }
    return r;
  }
  
  int getCapacity() {
    int r = 0;
    for (SubMap subMap : subMaps) {
      r += subMap.table.length;
    }
    return r;    
  }
  
  //test case use
  int getApproximateSize() {
    int s = 0;
    for (SubMap subMap : subMaps) {
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
      for (SubMap m : subMaps) {
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
}
