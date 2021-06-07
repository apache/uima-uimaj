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
import java.util.NoSuchElementException;
import java.util.function.IntFunction;

import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.util.IteratorNvc;

/**
 * Part of the JCasHashMap.
 * There are multiple instances of this class, one per concurrancy level
 */
class JCasHashMapSubMap implements Iterable<TOP> {
  
  // set to true to collect statistics for tuning
  // you have to also put a call to jcas.showJfsFromCaddrHistogram() at the end of the run
  private static final boolean TUNE = JCasHashMap.TUNE;

  // info kept per probe:  the address, and the current "delta"
  //   the delta is the one to use to go to the next "probe" address
  //   it starts at 1, and goes up by 1 to 23 (a prime)
  // These are kept in thread local constants, one per thread.
  private static final int PROBE_ADDR_INDEX = 0;
  private static final int PROBE_DELTA_INDEX = 1;

  
  static final ThreadLocal<int[]> probeInfoGet = new ThreadLocal<int[]>() {
    protected int[] initialValue() { return new int[2]; } };
      
  static final ThreadLocal<int[]> probeInfoPutInner = new ThreadLocal<int[]>() {
    protected int[] initialValue() { return new int[2]; } };

  //These are for tuning measurements
  int histogram [];
  int maxProbe = 0;
  int maxProbeAfterContinue = 0;
  int continues = 0;;

  /**
   * This lock is sometimes held by put, putIfAbsent, get, clear
   *   - not held if putIfAbsent or get find existing (non-reserve) item
   *     -- assumes no "remove" operation
   */
  private final Object synclock = new Object();
  
  private int sizeWhichTriggersExpansion;
  int size; // number of elements in the table 
  
  volatile TOP [] table;
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
  
  private TOP[] newTableKeepSize(int capacity) {
    assert(Integer.bitCount(capacity) == 1);
    TOP[] t = new TOP[capacity];
    sizeWhichTriggersExpansion = (int)(capacity * loadFactor);
    return t;
  }
  
  private void incrementSize() {
//    synchronized(synclock) {  // guaranteed by caller
  //    assert(lock.getHoldCount() > 0);
      if (size >= sizeWhichTriggersExpansion) {
        increaseTableCapacity();
      }
      size++;
//    }
  }
  
  // Does size management - shrinking overly large tables after the 2nd time
  void clear() {
    synchronized(synclock) {
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
  }      
   
  /**
   * find a real item or a reserve item, matching the key
   * Can be called under lock or not.
   * Using a ref to the current value of table, searches that int array.
   *   If, during the search, the table is resized, it continues using the
   *   ** before the resize ** int array referenced by localTable 
   *     The answer will only be OK if the key is found for a real value.  
   *     Results that yield null or Reserved slots must be re-searched, 
   *     under a lock (caller needs to do this).
   * @param key -
   * @param hash -
   * @param probeInfo - used to get/receive multiple int values;
   *    0: (in/out) startProbe or -1; -1 starts at the hash & bitMask
   *    1: (in/out) probeDelta (starts at 1)
   * @return the probeAddr in original table (which might have been resized)
   */
  private TOP find(final TOP[] localTable, final int key, final int hash, final int[] probeInfo) {
    int nbrProbes = 1;  // for histogram
    final int localTblLength = localTable.length;
    final int bitMask = localTblLength - 1;

    final int startProbe = probeInfo[PROBE_ADDR_INDEX];
    final boolean isInitialProbe = startProbe == -1;
    final boolean isContinue = TUNE && !isInitialProbe; 
    int probeAddr = isInitialProbe ? (hash & bitMask) : startProbe;
    
    int probeDelta = probeInfo[PROBE_DELTA_INDEX];
    //debug
//    if (probeDelta <= 0) {
//      System.out.println("debug");
//    }
    assert probeDelta > 0;
    
    // Next modification is overall, slower (very slightly)
    
//    int initialAdj = 0; 
//    if (probeDelta == 1) {
//      // This is an attempt to reduce collision chain clustering for things that hash to
//      //   the same spot, by slightly varying the starting delta
//      // xxxx xxxx  xxxx xxxx   xxxx xxxx  xxxx xxxx
//      //    x xxxx  xxxx xxxx   xxxx xxxx  xxxx xxxx    zzz after >>> concurrencyLevelBits eg 3
//      //    | ||  3 bits for randomizing
//      final int shiftAmt = 32 - concurrencyLevelBits   // number of significant bits
//                       - 3;  // 3 gives low order 3 bits, a number from 0 to 7
//      initialAdj = (hash >>> shiftAmt);
//    }
    
    TOP m = localTable[probeAddr];  // first probe doesn't add delta, facilitates restarting after acquiring lock

    while (true) {
      if (m == null) {
        // not in table
        setProbeInfo(probeInfo, probeAddr, probeDelta);  
        return null;  
      }
      
      if (m._id() == key) {
        setProbeInfo(probeInfo, probeAddr, probeDelta);
        if (TUNE) {
          updateHistogram(nbrProbes, isContinue); 
        }
        return m;
      }
      if (TUNE) {
        nbrProbes++;
        if (nbrProbes > localTblLength) {
          Misc.internalError();
        }
      }
      probeAddr = bitMask & (probeAddr + probeDelta 
//          + initialAdj
          );
//      initialAdj = 0;
      m = localTable[probeAddr];

      if (probeDelta < 11) { // a prime 
        // insures all possible slots in the table are probed,
        // and improves locality of reference (saw measurable improvement)
        probeDelta ++;
      }     
    }
  }

  private void updateHistogram(int nbrProbes, boolean isContinue) {
    
    synchronized(synclock) {
//    /* LOCK if not already, to update stats */
//    final boolean needUnlock;
//    if (!lock.isHeldByCurrentThread()) {
//      lockit();
//      needUnlock = true;
//    } else {
//      needUnlock = false;
//    }
    
//    try {
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
//    } finally {
//      if (needUnlock) {
//        lock.unlock();
//      }
    }

  }
  
  /**
   * If an entry isn't already present for this key,
   *   calls a Supplier to create a value and puts it into the table.
   *   otherwise, doesn't call the Supplier and returns the already present value.
   *   
   * If the key isn't present, gets a lock, and 
   *   (partially, as necessary) redoes the find. 
   *     - assert key still not present.
   *     - add a "reserve" for the slot where it will go
   *       -- reserve is a pseudo FS with a matching key, but with null _casView
   *     - release the lock
   *          -- eval the creator to create the item (may cause table to be updated)
   *     - require the lock
   *     - if resized, redo the find() till find the reserved item, and replace it with value.
   *     - if not resized, replace the prev reserved spot.  
   * 
   * Threading: not synchronized for main path where finding the element (if already in table).
   *   Since elements are never updated, there is no race if an element is found, except for
   *   table being resized.
   *   And it doesn't matter if the table is resized (if the element is found).
   *   
   * @param key - the id to use as the key
   * @param hash - the hash that was already computed from the key
   * @param creatorFromKey - the function to call to create the item.
   * @return - the found fs in the table with the same key, or the newly created item.
   */
    
  TOP putIfAbsent(final int key, final int hash, final IntFunction<TOP> creatorFromKey) {

    final int[] probeInfo = probeInfoGet.get();
    
    // not locked
    
    resetProbeInfo(probeInfo);
    TOP[] localTable = table;
    TOP m = find(table, key, hash, probeInfo);
    
    if (m != null) {
      if (!m._isJCasHashMapReserve()) {
        return m;
      }
            
//      // another thread is in the process of setting this value
//      // wait for it and return it
//      synchronized(synclock) {
//        if (localTable == table) { table wasn't resized
//          return 
//        }
//      }
//      return waitForReserve(localTable, key, hash, probeInfo);
    }
   
    synchronized(synclock) {
//    lockit();
//    boolean isLocked = true;
//    
//    try {
    
      // locked
//      localTable = table; // in case table was updated, to get updated values into localTable
      m = re_find(localTable, key, hash, probeInfo);
      if (m != null) {
        assert !m._isJCasHashMapReserve();
        return m;
      }
//        }
//        System.out.println("debug never get here");
//        throw new RuntimeException();
//        lock.unlock();
//        isLocked = false;
//        return waitForReserve(localTable, key, hash, probeInfo);
     
      
      /*************
       *  RESERVE  *
       *************/
      // is null. Reserve this slot to prevent other "putIfAbsent" calls for some other key
      // from using this slot.  This could happen when the createFromKey.apply is called,
      // since arbitrary Java code can run here (on the same thread). Other threads are 
      // blocked due to synclock.
      
      TOP reserve = TOP._createJCasHashMapReserve(key);
      table[probeInfo[PROBE_ADDR_INDEX]] = reserve; 
      localTable = table; // to see if table gets resized.
      int saved_reserved_index = probeInfo[PROBE_ADDR_INDEX]; // because the creator.get() call might recursively invoke this
  
      incrementSize();
      
//      assert lock.isLocked();
//      lock.unlock();
      
     // may recursively invoke this method, may throw exception
      m = creatorFromKey.apply(key);
     
  //    System.out.println("debug waiting to reacquire lock after creator." + Thread.currentThread().getName());
//      lockit();
  //    System.out.println("debug after reacquire lock after creator." + Thread.currentThread().getName());
      
      if (localTable == table) {
        // UIMA-6367 we maybe already set it in constructor
        assert table[saved_reserved_index] == reserve || table[saved_reserved_index] == m;
        table[saved_reserved_index] = m;
//        debugcheck(saved_reserved_index);
        
      } else {
        resetProbeInfo(probeInfo);
        TOP r = find(table, key, hash, probeInfo);
        // UIMA-6367 we maybe already set it in constructor
        assert isReserve(r) || r == m;
//        assert r == null;
//        assert r._id() == key;
        table[probeInfo[PROBE_ADDR_INDEX]] = m;  // set real value
//        debugcheck(probeInfo[PROBE_ADDR_INDEX]);
      }
//    } finally {
//      if (isLocked) {
//        if (notifyAllNeeded.getAndSet(false)) { // set must be done under lock, test must be done before unlock
//          lock.unlock(); // must be done outside of syncForWait
//          synchronized (syncForWait) {
//            syncForWait.notifyAll();  // in case waiting on resolution of Reserved
//          }
//        } else {
//          lock.unlock(); 
//        }
//      }
    }
    return m;    
  }

//  private void debugcheck(int i) {
//    TOP v = table[i];
//    TOP b = (i > 1) ? table[i - 1] : null;
//    TOP a = (i < table.length - 1) ? table[i + 1] : null;
//    if (b != null && v._id() == b._id()) {
//      System.out.println("debug");
//    }
//    if (a != null && v._id() == a._id()) {
//      System.out.println("debug");      
//    }    
//  }

  
  // got a reserve - just wait for it
  // may need to loop this because lock in thread holding the reserve is temporarily released
  //   when running the creator code
//  private TOP waitForReserve(TOP[] localTable, int key, int hash, int[] probeInfo) {
//    TOP m;
//    lockit(); // serves to wait for reserve to finish
//    try {
//      if (table == localTable) {
//        m = table[probeInfo[PROBE_ADDR_INDEX]]; 
//      } else {
//        resetProbeInfo(probeInfo);
//        m = find(table, key, hash, probeInfo);
//      }
//      assert m != null;
//      if (!m._isJCasHashMapReserve()) {
//        return m;
//      }
//      
//      // need to wait for reserve to clear
//      System.out.println("debug never get here");
//      throw new RuntimeException();
////      while (true) {
//////        notifyAllNeeded.set(true);
////        synchronized (syncForWait) {
////          try {
////            lock.unlock();
////            syncForWait.wait();
////          } catch (InterruptedException e) {
////          }
////        }
////        lockit();
////        
////        if (table == localTable) {
////          m = table[probeInfo[PROBE_ADDR_INDEX]]; 
////        } else {
////          resetProbeInfo(probeInfo);
////          m = find(table, key, hash, probeInfo);
////        }
////        assert m != null;
////        if (!m._isJCasHashMapReserve()) {
////          break;
////        }
////        // otherwise, loop around, got a spurious wakeup.
////      }
////      return m;
//    } finally {
////        if (lock.getHoldCount() > 1) {
////          System.out.println("debug");
////        }
//      lock.unlock();        
//    }
////    
////      // start the sleep at 1 microsec, incr by 2x each time around the loop
////      if (i > 10_000_000_000L) { 
////        throw new RuntimeException("Reserve not obtained in more than 10 seconds");
////      }
////      i = i * 2;
////      long d = System.nanoTime();
////      while (true) {
////        try {
////          Thread.sleep((int)(i / 1000000), (int)(i % 1000000)); // better than yield, which might be ignored?
////        } catch (InterruptedException e) {
////        }
////        if (System.nanoTime() - d > i) break;
//////        System.out.println("debug retry nanotime - start = " + (System.nanoTime() - d));
////      }
//////      System.out.println("debug i = " + i);
////    }    
//  }
  
  /**
   * Puts a new value into the table, replacing an existing one if there is an entry already,
   * or adding a new entry
   *
   * Starts by acquiring the lock.
   *   
   * @param key - the id to use as the key
   * @param hash - the hash that was already computed from the key
   * @param creator - the new value
   * @return - the previous fs in the table with the same key, or null
   */
  final TOP put(final int key, final TOP value, final int hash) {

    final int[] probeInfo = probeInfoGet.get();
    resetProbeInfo(probeInfo);

    synchronized(synclock) {
//    lockit();
      TOP previous;
//    try {
    
      previous = find(table, key, hash, probeInfo);
      
      if (previous != value) {
        table[probeInfo[PROBE_ADDR_INDEX]] = value;
//        debugcheck(probeInfo[PROBE_ADDR_INDEX]);
      }
      if (previous == null) {
        incrementSize();
      }
//    } finally {
//      lock.unlock();
      
      return previous;
    }
  }    
        
  /**
   * Gets a value.
   * 
   * Threading: not synchronized for main path where get is finding an element.
   *   Since elements are never updated, there is no race if an element is found.
   *   And it doesn't matter if the table is resized (if the element is found).
   *   If it is not found, need to get the lock in order to get memory synch, and
   *     start over if resized, or
   *     continue from reserved or null spot if not    
   *
   * @param key - the addr in the heap
   * @param hash - the hash that was already computed from the key
   * @return - the found fs, or null
   */
  final TOP get(final int key, final int hash) {

    final int[] probeInfo = probeInfoGet.get();
    resetProbeInfo(probeInfo);
    
    TOP[] localTable = table;
    TOP m = find(localTable, key, hash, probeInfo);
    if (m != null) {
      if (!isReserve(m)) {  
        return m;
      }
//      } else {
//        return waitForReserve(localTable, key, hash, probeInfo);      
//      }
    }
      
    
    // redo under lock to get memory synch
    synchronized(synclock) {
//    lockit();
//    try {
      m = re_find(localTable, key, hash, probeInfo);
//    } finally {
//      lock.unlock();
    }
//    if (m != null) {
//      assert isReal(m);  
//    }    
    return m;
  }   
     
 /**
  * Only used to fill in newly expanded table
  * always called with lock held
  * @param key -
  * @param value -
  * @param hash -
  */
  
  // called under lock
  private void putInner(int key, TOP value, int hash, int[] probeInfo) {
//    assert(lock.getHoldCount() > 0);

    resetProbeInfo(probeInfo);
    final TOP[] localTable = table;

    final TOP m = find(localTable, key, hash, probeInfo);
    assert(m == null);  // no dups in original table imply no hits in new one
    localTable[probeInfo[PROBE_ADDR_INDEX]] = value;
  }
    

  // called under lock
  private void increaseTableCapacity() {
    final TOP [] oldTable = table; 
    final int oldCapacity = oldTable.length;
    int newCapacity = 2 * oldCapacity;
    
    if (TUNE) {
      System.out.println("Capacity increasing from " + oldCapacity + " to " + newCapacity);
    }
    table = newTableKeepSize(newCapacity);
    final int[] probeInfo = probeInfoPutInner.get();
    for (int i = 0; i < oldCapacity; i++) {
      TOP fs = oldTable[i];
      if (fs != null) {
        final int key = fs._id();
        final int hash = JCasHashMap.hashInt(key);
        putInner(key, fs, hash >>> concurrencyLevelBits, probeInfo);
      }   
    }
  }
  
  private static boolean isReserve(TOP m) {
    return m != null && m._isJCasHashMapReserve();
  }
  
//  private static boolean isReal(TOP m) {
//    return m != null && !m._isJCasHashMapReserve();
//  }
  
  private static void resetProbeInfo(int[] probeInfo) {
    probeInfo[PROBE_ADDR_INDEX] = -1;
    probeInfo[PROBE_DELTA_INDEX] = 1;
  }
  
  private static void setProbeInfo(int[] probeInfo, int probeAddr, int probeDelta) {
    probeInfo[PROBE_ADDR_INDEX] = probeAddr;
    probeInfo[PROBE_DELTA_INDEX] = probeDelta;   
  }

  
  private TOP re_find(TOP[] localTable, int key, int hash, int[] probeInfo) {
    if (localTable != table) {
      resetProbeInfo(probeInfo);
    }
    return find(table, key, hash, probeInfo);
  }

  @Override
  public IteratorNvc<TOP> iterator() {
    return new IteratorNvc<TOP>() {
      int i = moveToNextValid(0);

      @Override
      public boolean hasNext() {
        return i < table.length;
      }

      @Override
      public TOP next() {
        if (!hasNext()) throw new NoSuchElementException();
        return nextNvc();
      }
      
      @Override
      public TOP nextNvc() {
        TOP r = table[i];
        i = moveToNextValid(i+1);
        return r;        
      }
      
      int moveToNextValid(int pos) {
        while (pos < table.length && 
               (table[pos] == null || 
                table[pos]._isJCasHashMapReserve())) {
          pos ++;
        }
        return pos;
      }
    };
    
  }
  
//  private void lockit() {
//    // might have recursive locking on same thread if creator invokes this recursively
////    if (lock.getHoldCount() > 0) {
////      System.out.println("debug");
////    }
////    assert lock.getHoldCount() == 0;
//    lock.lock();
//  }
//  private TOP locked_find(int key, int hash, int[] probeInfo) {    
//
//  retry_find: 
//    while (true) { // loop context while finding a reserved element      
//      final TOP[] localTable = table;
//      
//      TOP m = find(localTable, key, hash, probeInfo);
//      if (isReal(m)) {
//        return m;  // fast path for found item       
//      }
//      
//      while (isReserve(m)) {
//        // is for another FS, and could occur in use case:
//        //   the create-fs code creates other FSs
//        //   also in test case, 
//        
//        // get here when another thread has a reserve pending on this slot
//        // assert must be for another key.
//        final TOP[] localTable2 = table;  // save ref to see if table gets resized
//        // can't wait on reserved item because would need to do lock.unlock() followed by wait, but
//        //   inbetween these, another thread could already do the notify.
//        try {
//          /**********
//           *  WAIT  *
//           **********/
//          lockCondition.await();  // wait on the lock, lockCondition is the condition for "lock" object. 
//        } catch (InterruptedException e) {
//        }
//  
//        // at this point, the lock was released, and re-aquired
//        if (localTable2 != table) { // table was resized
//          resetProbeInfo(probeInfo); // redo find from the top
//          continue retry_find;  
//        }
//        final TOP m3 = localTable2[probeInfo[PROBE_ADDR_INDEX]];
//        if (isReserve(m3)) {
//          // still reserved - wait some more.
//          // case = interruptedexception && no resize && not changed to real, retry
//          // not continuing from the top, but from the current probe
//          // redoes the wait
//          continue;  
//        }
//      }
//      
//      // is not reserved anymore, and no table size change. re-find from here
//      m = find(table, key, hash, probeInfo);
//      assert m == null;
//    }
//  }
}

