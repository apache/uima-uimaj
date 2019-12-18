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
package org.apache.uima.internal.util;

import java.util.Arrays;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/**
 * A common superclass for hash maps and hash sets
 *
 */
public abstract class Common_hash_support {

  // set to true to collect statistics for tuning
  // you have to also put a call to showHistogram() at the end of the run
  protected static final boolean TUNE = false;
  private static Common_hash_support tune_instance;

  
  
  protected static final int MIN_SIZE = 10;   // 10 / .66 =15.15
  protected static final int MIN_CAPACITY = 16;
  protected static final int MIN_CAPACITY_SHRINK = 64;  // don't shrink below this - thrashing
  
  protected final float loadFactor;  
  
  protected final int initialCapacity; 

  protected int histogram [];
  protected int maxProbe = 0;

  protected int sizeWhichTriggersExpansion;
  private int size = 0; // number of elements in the table  

  protected int removed = 0;  // for rebalancing  

  /** set to the first found_removed when searching */
  protected int found_removed;

  protected boolean secondTimeShrinkable = false;

  /**
   * @param initialSizeBeforeExpanding the number of elements the table should hold before expanding
   */
  public Common_hash_support(int initialSizeBeforeExpanding) {
    this(initialSizeBeforeExpanding, 0.66F);
  }
    
  public Common_hash_support(int initialSizeBeforeExpanding, float factor) {
    this.loadFactor = factor;
    this.initialCapacity = tableSpace(initialSizeBeforeExpanding, factor);
    if (TUNE) {
      tune_instance = this;
      histogram = new int[200];
      Arrays.fill(histogram, 0);
      maxProbe = 0;
    }
  }

  public Common_hash_support(Common_hash_support orig) {
    this(orig.initialCapacity);
    this.sizeWhichTriggersExpansion = orig.sizeWhichTriggersExpansion;
    this.size = orig.size;
    this.removed = orig.removed; 
    this.secondTimeShrinkable = orig.secondTimeShrinkable;

    // copy doesn't do tuning measurements
    this.histogram = null;
  }
  
  public void clear() {
    // see if size is less than the 1/2 size that triggers expansion
    if (size + removed <  (sizeWhichTriggersExpansion >>> 1)) {
      // if 2nd time then shrink by 50%
      //   this is done to avoid thrashing around the threshold
      if (secondTimeShrinkable) {
        secondTimeShrinkable = false;
        final int newCapacity = Math.max(initialCapacity, keys_length() >>> 1);
        if (newCapacity < keys_length()) { 
          newTable(newCapacity);  // shrink table by 50%
          size = 0;
          removed = 0;
          resetHistogram();
          if (PositiveIntSet.IS_TRACE_MODE_SWITCH) {
            System.out.println("TRAcE_MODE Common_hash clear 2nd time shrinkable, newCapacity=" + newCapacity + ", keys_length: " + keys_length());
          }
          return;
        } else { // don't shrink below minimum
          clearExisting();
          if (PositiveIntSet.IS_TRACE_MODE_SWITCH) {
            System.out.println("TRAcE_MODE Common_hash clear 2nd time shrinkable but nothing done, below minimum: newCapacity=" + newCapacity + ", keys_length: " + keys_length());
          }
          return;
        }
      } else {
        if (PositiveIntSet.IS_TRACE_MODE_SWITCH) System.out.println("TRACE_MODE Common_hash clear setting 2nd time shrinkable");
        secondTimeShrinkable = true;
      }
    } else {
      secondTimeShrinkable = false; // reset this to require 2 triggers in a row
    }
    clearExisting();
  }
  
  private void clearExisting() {
    clearKeysAndValues();
    size = 0;
    removed = 0;
    resetHistogram();    
  }
  
  /** It gets a ref to the current value of table, and then searches that array.
   * Side effect: found_removed is set to the position of the first REMOVED_KEY (if any) encountered
   * during the search.
   * 
   * @param hash     the hash code of the key
   * @param is_eq_or_not_present   true if the key at the int position is == to the key, or is 0
   * @param is_removed_key true if the key at the int position is "removed"
   * @return the probeAddr in keys array.  The value is the not-present-value if not found
   */
  protected int findPosition(
                   int hash,
                   IntPredicate is_eq_or_not_present,
                   IntPredicate is_removed_key) {
    
    found_removed = -1;  
//    final int hash = key_hash.getAsInt();

//    final int[] localKeys = keys;
    final int bitMask = keys_length() - 1;
    
    int nbrProbes = 1;
    int probeDelta = 0;
    int probeAddr = hash & bitMask;
    
    for (;;) {

      if (is_eq_or_not_present.test(probeAddr)) {
        if (TUNE) {
          updateHistogram(nbrProbes);
        }
        return probeAddr;
      }

      if (found_removed == -1 && is_removed_key.test(probeAddr)) {
        found_removed = probeAddr;
      }

      if (TUNE) nbrProbes++;
      
      if (probeDelta < 13) {
        probeDelta ++; // stop at a prime
                       // which guarantees all slots eventually traversed.
      }
      probeAddr = bitMask & (probeAddr + probeDelta);
    }
    
//    
//    // fast paths
//    if (is_eq_or_not_present.test(probeAddr)) {
////    final int testKey = localKeys[probeAddr];
////    if (testKey == 0 || testKey == key) {
//      if (TUNE) {
//        updateHistogram(1);
//      }
//      return probeAddr;
//    }
//    if (is_removed_key.test(probeAddr)) {
////        testKey == REMOVED_KEY) {
//      found_removed = probeAddr;
//    }
//    return findPosition2(is_eq_or_not_present, is_removed_key, probeAddr);
  } 
 
//  private int findPosition2(IntPredicate is_eq_or_not_present,
//                    IntPredicate is_removed_key, 
//                    int probeAddr) {
//    final int bitMask = keys_length() - 1;
//    int nbrProbes = 2;
//    int probeDelta = 1;
//    probeAddr = bitMask & (probeAddr + (probeDelta++));
//
//    while (true) {
//      if (is_eq_or_not_present.test(probeAddr)) {
////      final int testKey = localKeys[probeAddr];
////      if (testKey == 0 || testKey == key) {
//        break;
//      }
//      
//      if (found_removed == -1 && is_removed_key.test(probeAddr)) {
//        found_removed = probeAddr;
//      }
//      nbrProbes++;
//      if (probeDelta < 13) {
//        probeDelta ++; // stop at a prime
//                       // which guarantees all slots eventually traversed.
//      }
//      probeAddr = bitMask & (probeAddr + (probeDelta++));
//    }
//
//    if (TUNE) {
//      final int pv = histogram[nbrProbes];
//
//      histogram[nbrProbes] = 1 + pv;
//      if (maxProbe < nbrProbes) {
//        maxProbe = nbrProbes;
//      }
//    }
//    return probeAddr;
//  }
   
  /**
   * As REMOVED tokens populate,
   * the number of 0's (representing free cells and
   *   also the end of bucket chain searches) drops.
   *   
   *   If this drops a lot, then searches take much longer.
   *   If there are no 0's left, searches never terminate!
   *   
   * Keep the number of 0's at about 1 - load factor 
   * 
   */
  private void maybeRebalanceRemoves() {
    final int old_capacity = keys_length();
    if (old_capacity <= MIN_CAPACITY_SHRINK) {
      return;  
    }
    int new_capacity = old_capacity >> 1;
            
//    //debug
//      int sz = size;
      
    /******************************************************
     * Logic for clearing out the removed markers in bulk
     * 
     * 3 cases:  resize up (never happens), down, or the same
     * 
     * Don't clear out if there's room left unless
     *   there's too much room left and should downsize
     * 
     * Don't over do - avoid thrashing, avoid work for small sizes
     * 
     * Case for upsizing: none.  It's guaranteed there are always enough
     *   0's for the load factor, by put/add implementation.  
     *  
     * Case for downsizing: if the size < 1/3 of the new capacity or,
     *   number being removed would be > 1/3 of the new capacity  
     ******************************************************/
      
    if (removed + size >= sizeWhichTriggersExpansion) {
      Misc.internalError();  // put or add always keeps this false. remove always reduces or keeps this value the same
    }
    
    // REMOVED tags if there are enough of them to make it worthwhile
    // or if just should shrink because removed + size is smaller than 1/3 the new capacity
    int one_third_new_capacity = sizeWhichTriggersExpansion >> 2;
    int one_half_new_capacity = new_capacity >> 1;
    if (removed > one_half_new_capacity || (size < one_third_new_capacity)) {
      if (size >= one_third_new_capacity) {
        new_capacity = old_capacity;
      }
      if (TUNE) {
        System.out.println("Capacity (maybe) decreasing from " + old_capacity + " to " + new_capacity + " removed= " + removed + " size= " + size);
      }      
      copyOld2New(new_capacity, old_capacity);
    }
//    //debug
//    if (sz != size) 
//      System.out.println("debug");
  }
  
  /**
   * This method calls the subclass's copy_to_new_table method, 
   *   passing an inner lambda containing common code for copying old to new.
   *   
   *   That inner lambda, when invoked by the copy_to_new_table method, 
   *     is passed another lambda of one argument (the old index) 
   *     which is called to copy each element.  
   * @param new_capacity
   * @param old_capacity
   */
  private void copyOld2New(int new_capacity, int old_capacity) {
    copy_to_new_table( 
      new_capacity,
      old_capacity,
      
        // this code assigned to "commonCopy" arg
        (IntConsumer copyToNew, IntPredicate is_valid_old_key) -> {  
      newTable(new_capacity);
      removed = 0; // reset before put, otherwise, causes premature expansion
      for (int i = 0; i < old_capacity; i++) {
        if (is_valid_old_key.test(i)) {
          copyToNew.accept(i);
        }
      }
    });    
  }

  /**
   * advance pos until it points to a non 0 or is 1 past end
   * If pos is negative, start at 0.
   * Don't move if pos already has valid key
   * @param pos -
   * @return updated pos
   */
  protected int moveToNextFilled(int pos) {
    final int max = keys_length();
    if (pos < 0) {
      pos = 0;
    }
    while (true) {
      if (pos >= max) {
        return pos;
      }
      if (is_valid_key(pos)) {
        return pos;
      }
      pos ++;
    }
  }

  /**
   * decrement pos until it points to a non 0 or is -1
   * If pos is beyond end start at end.
   * Don't move if pos already has valid key
   * @param pos -
   * @return updated pos
   */
  protected int moveToPreviousFilled(int pos) {
    final int max = keys_length();
    if (pos > max) {
      pos = max - 1;
    }
    
    while (true) {
      if (pos < 0) {
        return pos;
      }
      if (is_valid_key(pos)) {
        return pos;
      }
      pos --;
    }
  }
  
  // called by clear, increase table size, decrease table size
  protected void newTable(int capacity) {
    capacity = Math.max(MIN_SIZE, Misc.nextHigherPowerOf2(capacity));
    newKeysAndValues(capacity);
    sizeWhichTriggersExpansion = (int)(capacity * loadFactor);
  }
  
  protected void incrementSize() {
    size++;
    if (size + removed >= sizeWhichTriggersExpansion) {
      maybeIncreaseTableCapacity();
    }
  }

  private void maybeIncreaseTableCapacity() {
    int old_capacity = keys_length();
    int new_capacity = (removed >= size) 
                         ? old_capacity 
                         : 2 * old_capacity;
    
    if (TUNE) {
      System.out.println("Capacity (maybe) increasing from " + old_capacity + " to " + new_capacity + ", removes= " + removed + " size=" + size);
    }
    
    copyOld2New(new_capacity, old_capacity);
  }
  
  protected void commonPutOrAddNotFound() {
    
    if (found_removed != -1) {
      removed --; // used up a removed slot
    }
    incrementSize();
//    debugValidate();
  }
  
  /**
   * only called if actually found and removed an entry
   */
  protected void commonRemove() {
    removed ++;
    size --;
    maybeRebalanceRemoves();
//    debugValidate();
  }
  
  public int size() {
    return size;
  }

  @FunctionalInterface
  public interface CommonCopyOld2New {
    void apply(IntConsumer copyToNew, IntPredicate is_valid_old_key);
  }
  protected abstract boolean is_valid_key(int pos);
  protected abstract int keys_length();
  protected abstract void newKeysAndValues(int capacity);
  protected abstract void clearKeysAndValues();
  protected abstract void copy_to_new_table(int new_capacity, int old_capacity, CommonCopyOld2New r);
  
  protected void resetHistogram() {
//    if (TUNE) {
//      histogram = new int[200];
//      Arrays.fill(histogram, 0);
//      maxProbe = 0;
//    }    
  }

  private void updateHistogram(int nbrProbes) {
    histogram[nbrProbes] = 1 + histogram[nbrProbes];
    if (maxProbe < nbrProbes) {
      maxProbe = nbrProbes;
    }
  }

  public void showHistogram() {
    if (TUNE) {
      int sumI = 0;
      for (int i : histogram) {
        sumI += i;
      }
      
      System.out.format(
          "Histogram of number of probes, loadfactor = %.1f, maxProbe=%,d nbr of find operations at last table size=%,d%n",
          loadFactor, maxProbe, sumI);
      for (int i = 0; i <= maxProbe; i++) {
        if (i == maxProbe && histogram[i] == 0) {
          System.out.println("huh?");
        }
        System.out.println(i + ": " + histogram[i]);
      }     
      
      System.out.println("bytes / entry = " + (float) (keys_length()) * 8 / size());
      System.out.format("size = %,d, prevExpansionTriggerSize = %,d, next = %,d%n",
          size(),
          (int) ((keys_length() >>> 1) * loadFactor),
          (int) (keys_length() * loadFactor));
    }
  }

  static {
    if (TUNE) {
      Runtime.getRuntime().addShutdownHook(new Thread(null, () -> {
        tune_instance.showHistogram();
      }));     
    }
  }

  // test case use
  int getCapacity() {
    return keys_length();
  }
  
  protected abstract class CommonKeyIterator implements IntListIterator {

    protected int curPosition;
    
    protected final int firstPosition;
    
    protected CommonKeyIterator() {
        this.curPosition = moveToNextFilled(0);
        firstPosition = curPosition;
    }
    
    @Override
    public boolean hasNext() {
      return curPosition < keys_length() && curPosition >= 0;
    }

    @Override
    public boolean hasPrevious() {
      if (curPosition > keys_length() || curPosition <= 0) {
        return false;
      }
      
      int test = moveToPreviousFilled(curPosition - 1);
      return test >= 0;
    }

    @Override
    /**
     * @see org.apache.uima.internal.util.IntListIterator#moveToStart()
     */
    public void moveToStart() {
      curPosition = moveToNextFilled(0);
    }

    @Override
    /**
     * @see org.apache.uima.internal.util.IntListIterator#moveToEnd()
     */
    public void moveToEnd() {
      curPosition = moveToPreviousFilled(keys_length() - 1);
    }
    
  }
  
  /**
   * 
   * @param numberOfElements -
   * @param factor -
   * @return capacity of the main table (either 2 byte or 4 byte entries)
   */
  public static int tableSpace(int numberOfElements, Float factor) {
    if (numberOfElements < 0) {
      throw new IllegalArgumentException("must be > 0");
    }
    final int capacity = Math.round(numberOfElements / factor);
    return  Math.max(16, Misc.nextHigherPowerOf2(capacity));
  }

  protected void debugValidate() {
    // count non-0, non-removed, compare to size
    int sum = 0;
    for (int i = 0; i < keys_length(); i ++) {
      if (is_valid_key(i)) {
        sum ++;
        if (sum > size) {
          System.out.println("debug");
        }
      }
    }
  }
}
