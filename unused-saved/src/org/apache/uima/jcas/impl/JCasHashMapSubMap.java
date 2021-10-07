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

class JCasHashMapSubMap {
  
  // set to true to collect statistics for tuning
  // you have to also put a call to jcas.showJfsFromCaddrHistogram() at the end of the run
  private static final boolean TUNE = JCasHashMap.TUNE;

  
  private static final int PROBE_ADDR_INDEX = 0;
  private static final int PROBE_DELTA_INDEX = 1;


  private static class ReserveTopType extends TOP_Type {
    public ReserveTopType() {
      super();
    }
  }

  // package private for test case use
  static final TOP_Type RESERVE_TOP_TYPE_INSTANCE = new ReserveTopType(); 

  static final ThreadLocal<int[]> probeInfoGet = new ThreadLocal<int[]>() {
    protected int[] initialValue() { return new int[2]; } };
      
  static final ThreadLocal<int[]> probeInfoPut = new ThreadLocal<int[]>() {
    protected int[] initialValue() { return new int[2]; } };

  static final ThreadLocal<int[]> probeInfoPutInner = new ThreadLocal<int[]>() {
    protected int[] initialValue() { return new int[2]; } };

  //These are for tuning measurements
  int histogram [];
  int maxProbe = 0;
  int maxProbeAfterContinue = 0;
  int continues = 0;;

  private final ReentrantLock lock = new ReentrantLock();
  private final Condition lockCondition = lock.newCondition();
  
  private int sizeWhichTriggersExpansion;
  int size; // number of elements in the table  
  volatile FeatureStructureImpl [] table;
  private boolean secondTimeShrinkable = false;
  
  private final float loadFactor;
  private final int subMapInitialCapacity;
  private final int concurrencyLevelBits;
  
  JCasHashMapSubMap(float loadFactor, int subMapInitialCapacity, int concurrencyLevelBits) {
    this.loadFactor = loadFactor;
    this.subMapInitialCapacity = subMapInitialCapacity;
    this.concurrencyLevelBits = concurrencyLevelBits;
    newTable(subMapInitialCapacity);
  }
  
  private JCasHashMapSubMap newTable(int capacity) {
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
  void clear() {
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
  private FeatureStructureImpl find(final FeatureStructureImpl[] localTable, final int key, final int hash, final int[] probeInfo) {
    final int bitMask = localTable.length - 1;
    final int startProbe = probeInfo[PROBE_ADDR_INDEX];      
    final int probeAddr = (startProbe < 0) ? (hash & bitMask) : startProbe; 
    final FeatureStructureImpl m = localTable[probeAddr];
    // fast paths 
    if (m == null) {
      // not in table
      setProbeInfo(probeInfo, probeAddr, 0);
      return m;  // returns null
    }
    
    if (m.getAddress() == key) {
      setProbeInfo(probeInfo, probeAddr, 0);
      if (TUNE) {
        updateHistogram(1, probeInfo[PROBE_ADDR_INDEX] != -1);
      }
      return m;
    }
    
    return find2(localTable, key, probeInfo, probeAddr);
  }

  private FeatureStructureImpl find2(final FeatureStructureImpl[] localTable, final int key, final int[] probeInfo, int probeAddr) {
    final boolean isContinue = TUNE && (probeInfo[PROBE_ADDR_INDEX] != -1); 
    final int bitMask = localTable.length - 1;
//    assert((startProbe < 0) ? probeInfo[PROBE_DELTA_INDEX] == 1 : true);
    int probeDelta = probeInfo[PROBE_DELTA_INDEX];
    int nbrProbes = 2;  
    probeAddr = bitMask & (probeAddr + (probeDelta++));
    FeatureStructureImpl m = localTable[probeAddr];

    while (null != m) {  // loop to traverse bucket chain
      if (m.getAddress() == key) {
        setProbeInfo(probeInfo, probeAddr, 0);
        if (TUNE) {
          updateHistogram(nbrProbes, isContinue); 
        }
        return m;
      }
      nbrProbes++;
      probeAddr = bitMask & (probeAddr + (probeDelta++));
      m = localTable[probeAddr];
    }
    setProbeInfo(probeInfo, probeAddr, probeDelta);
    return m;  // returns null    
  }
  
  private void updateHistogram(int nbrProbes, boolean isContinue) {
    
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
  FeatureStructureImpl getReserve(final int key, final int hash) {

    boolean isLocked = false;
    final int[] probeInfo = probeInfoGet.get();
    try {

   retry:
      while (true) { // loop back point after locking against updates, to re-traverse the bucket chain from the beginning
        resetProbeInfo(probeInfo);
        final FeatureStructureImpl m;
        final FeatureStructureImpl[] localTable = table;
        
        if (isReal(m = find(localTable, key, hash, probeInfo))) {
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
        final FeatureStructureImpl[] localTable3;
        if (localTable != table) {
          // redo search from top, because table resized
          resetProbeInfo(probeInfo);
          localTable3 = table;
        } else {
          localTable3 = localTable;
        }
//        // re acquire the FeatureStructure m, because another thread could change it before the lock got acquired
//        final FeatureStructureImpl m2 = localTable[probeInfo[PROBE_ADDR_INDEX]];
//        if (isReal(m2)) {
//          return m2;  // another thread snuck in before the lock and switched this
//        }
        
        // note: localTable not used from this point, unless reset
        // note: this "find" either finds from the top (if size changed) or finds from current spot.
        final FeatureStructureImpl m2 = find(localTable3, key, hash, probeInfo);
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
    
      
  FeatureStructureImpl put(int key, FeatureStructureImpl value, int hash) {

    lock.lock();
    try {
      final int[] probeInfo = probeInfoPut.get();
      resetProbeInfo(probeInfo);
      final FeatureStructureImpl[] localTable = table;
      final FeatureStructureImpl prevValue = find(localTable, key, hash, probeInfo);
      localTable[probeInfo[PROBE_ADDR_INDEX]] = value;
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
  * always called with lock held
  * @param key -
  * @param value -
  * @param hash -
  */
  
  private void putInner(int key, FeatureStructureImpl value, int hash) {
    assert(lock.getHoldCount() > 0);
    final int[] probeInfo = probeInfoPutInner.get();
    resetProbeInfo(probeInfo);
    final FeatureStructureImpl[] localTable = table;

    final FeatureStructureImpl m = find(localTable, key, hash, probeInfo);
    assert(m == null);  // no dups in original table imply no hits in new one
    localTable[probeInfo[PROBE_ADDR_INDEX]] = value;
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
        final int hash = JCasHashMap.hashInt(key);
        putInner(key, fs, hash >>> concurrencyLevelBits);
      }   
    }
  }
  
  private static boolean isReserve(FeatureStructureImpl m) {
    return m != null && ((TOP)m).jcasType == RESERVE_TOP_TYPE_INSTANCE;
  }
  private static boolean isReal(FeatureStructureImpl m) {
    return m != null && ((TOP)m).jcasType != RESERVE_TOP_TYPE_INSTANCE;
  }
  
  private static void resetProbeInfo(int[] probeInfo) {
    probeInfo[PROBE_ADDR_INDEX] = -1;
    probeInfo[PROBE_DELTA_INDEX] = 1;
  }
  
  private static void setProbeInfo(int[] probeInfo, int probeAddr, int probeDelta) {
    probeInfo[PROBE_ADDR_INDEX] = probeAddr;
    probeInfo[PROBE_DELTA_INDEX] = probeDelta;   
  }
  

}

