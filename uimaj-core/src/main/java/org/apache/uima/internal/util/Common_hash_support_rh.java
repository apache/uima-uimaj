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
import java.util.function.IntPredicate;

/**
 * A common superclass for hash maps and hash sets
 * Uses robin hood with backward shift for deletion 
 * http://codecapsule.com/2013/11/17/robin-hood-hashing-backward-shift-deletion/
 *
 * uses linear probing (no delta expansion)
 * 
 * extra table (1 byte per slot) holds lower 7 bits of hash 
 *   and therefore also serves to indicate distance from initial probe =
 *     probe spot % 128 - lower 7 bits of hash (if negative, add 128, unless wrap around?)
 * 
 * 8th bit , if 1 , is flag showing empty; no tombstones, because using backward-shift technique for removes
 * 
 * find: stop early, if # probe-spot > distance from initial probe.  (mod wrap-around)
 * 
 */
public abstract class Common_hash_support_rh {

  // set to true to collect statistics for tuning
  // you have to also put a call to showHistogram() at the end of the run
  protected static final boolean TUNE = false;
  
  protected static final int MIN_SIZE = 10;   // 10 / .66 =15.15
  protected static final int MIN_CAPACITY = 16;
  protected static final int MIN_CAPACITY_SHRINK = 64;  // don't shrink below this - thrashing
  
  private static final byte LHB_EMPTY = (byte) 0x80;
  private static final int LHB_HASH_MASK =  0x7f;
  
  protected final float loadFactor;  
  
  protected final int initialCapacity; 

  protected int histogram [];
  protected int maxProbe = 0;

  protected int sizeWhichTriggersExpansion;
  private int size = 0; // number of elements in the table  

  protected boolean secondTimeShrinkable = false;
  
  protected abstract boolean is_valid_key(int pos);
  protected abstract int keys_length();
  protected abstract void newKeysAndValues(int capacity);
  protected abstract void clearKeysAndValues();

  protected int bitMask;  // = key size -1
  /**
   * 7 bits of lower hash map, serves as distance from initial bucket probe 
   * initialize to "empty" = 0x80
   * If not "empty", the value is 
   *   - often the same as the lower 7 bits of the index
   *   - if not that, it's because there's a collision, and some slot with a higher index will have the lower 7 bits of the index
   *   - so, the distance from the initial probe is the probe addr with the matching value - initial probe addr.
   *   -   which, is the same as the 
   *         ((probe addr with the matching value) & LBH_HASH_MASK) 
   *         - matching value
   */
  private byte[] lower_hash_bits; 
  protected int initial_probe;
  protected byte initial_probe_lhb;
  private int smaller_of_mask;

  private static Common_hash_support_rh tune_instance;
  
  protected abstract void shift(int prev, int pos);
  protected abstract void setEmpty(int pos);
  
  protected abstract void expand_table(); // double table size

  /**
   * @param initialSizeBeforeExpanding the number of elements the table should hold before expanding
   */
  public Common_hash_support_rh(int initialSizeBeforeExpanding) {
    this(initialSizeBeforeExpanding, 0.66F);
  }
   
  public Common_hash_support_rh(int initialSizeBeforeExpanding, float factor) {
    this.loadFactor = factor;
    this.initialCapacity = tableSpace(initialSizeBeforeExpanding, factor);
    lower_hash_bits = init_lhb(this.initialCapacity);
    bitMask = this.initialCapacity - 1;
    setSmallerOfMask();
    if (TUNE) {
      tune_instance = this;
      histogram = new int[200];
      Arrays.fill(histogram, 0);
      maxProbe = 0;
    }
  }

  public Common_hash_support_rh(Common_hash_support_rh orig) {
    this(orig.initialCapacity);
    this.sizeWhichTriggersExpansion = orig.sizeWhichTriggersExpansion;
    this.size = orig.size;
    this.secondTimeShrinkable = orig.secondTimeShrinkable;

    // copy doesn't do tuning measurements
    this.histogram = null;
  }
  
  public void clear() {
    // see if size is less than the 1/2 size that triggers expansion
    if (size <  (sizeWhichTriggersExpansion >>> 1)) {
      // if 2nd time then shrink by 50%
      //   this is done to avoid thrashing around the threshold
      if (secondTimeShrinkable) {
        secondTimeShrinkable = false;
        final int newCapacity = Math.max(initialCapacity, keys_length() >>> 1);
        if (newCapacity < keys_length()) { 
          newTable(newCapacity);  // shrink table by 50%
          size = 0;
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
    Arrays.fill(lower_hash_bits, LHB_EMPTY);
    size = 0;
    resetHistogram();    
  }
  
  /** It gets a ref to the current value of table, and then searches that array.
   * 
   * speedups: avoid looking in lower_hash_bits array for 90% of the time the hit is in the first 2 positions?
   * 
   * @param hash     the hash code of the key
   * @param is_eq    true if the key at the int position is == to the key, or is 0
   * @return the probeAddr in keys array.  
   *         If not found, the probe value is to empty cell or to some other value
   *         Use isEmpty(probeAddr) to distinguish.
   */
  protected int findPosition(
                   int hash,
                   IntPredicate is_eq_or_not_present
                   ) {
        
    int probeAddr = initial_probe = hash & bitMask;
    final byte initialProbeLhb = initial_probe_lhb = (byte) (hash & LHB_HASH_MASK);
 
    if (is_eq_or_not_present.test(probeAddr) ) {
      if (TUNE) {
        updateHistogram(0);
      }
      return probeAddr;
    }
    int extraProbes = 0;
    
    for (;;) {      

      byte plhb = lower_hash_bits[probeAddr];
          // not found if hit an empty slot
      if (plhb == LHB_EMPTY || 
       // or the distance to original probe for this slot is < number of probes
       //   (because otherwise, the other item would have replaced if present)
          extraProbesForEntry(plhb, probeAddr) < extraProbes
         ) {
        // not found
        if (TUNE) {
          updateHistogram(extraProbes);
        }
        return probeAddr;
      }
      
      if (plhb == initialProbeLhb) {
        if (is_eq_or_not_present.test(probeAddr)) {
          // found
          if (TUNE) {
            updateHistogram(extraProbes);
          }
          return probeAddr;
        }
        // else was collision, not equal
      }
      
     extraProbes++;
      
      probeAddr = incrPos(probeAddr);
    }
    
  } 
 
  /** 
   * Find the position for a new (guaranteed not in the table) item
   * 
   * @param hash     the hash code of the key
   * @return the probeAddr in keys array.  
   *         If not found, the probe value is to empty cell or to some other value
   *         Use isEmpty(probeAddr) to distinguish.
   */
  protected int findPosition_new(int hash) {
        
    int extraProbes = 0;
    int probeAddr = initial_probe = hash & bitMask;
    initial_probe_lhb = (byte) (hash & LHB_HASH_MASK);
    
    
    for (;;) {

      byte plhb = lower_hash_bits[probeAddr];
          // not found if hit an empty slot
      if (plhb == LHB_EMPTY || 
       // or the distance to original probe for this slot is < number of probes
       //   (because otherwise, the other item would have replaced if present)
          extraProbesForEntry(plhb, probeAddr) < extraProbes
         ) {
        // not found
        if (TUNE) {
          updateHistogram(extraProbes);
        }
        return probeAddr;
      }
            
      extraProbes++;
      
      probeAddr = incrPos(probeAddr);
    }
  }
  
  protected boolean isEmpty(int probeAddr) {
    return lower_hash_bits[probeAddr] == LHB_EMPTY;
  }
  
  protected byte setLhb(int pos, byte lbh) {
    byte r = lower_hash_bits[pos];
    assert lbh >= 0;
    if (extraProbesForEntry(lbh, pos) >= 20) {
      System.out.println("debug");
    }
    assert extraProbesForEntry(lbh, pos) < 20;
    lower_hash_bits[pos] = lbh;
    return r;
  }
    
  protected void remove_common(int position) {
    int prev = position;
    int i = incrPos(position);
    size --;
    for (;;) {
      byte lhb = lower_hash_bits[i];
      if (lhb == LHB_EMPTY || extraProbesForEntry(lhb, i) == 0 ) {
        lower_hash_bits[prev] = LHB_EMPTY;
        setEmpty(prev);
        return;
      }
      //debug
      if (extraProbesForEntry(lhb, prev) > 20) {
        System.out.println("debug");
      }
      lower_hash_bits[prev] = lhb;
      shift(prev, i);
      
      prev = i;
      i = incrPos(i);
    }
  }
  
//  /**
//   * This method calls the subclass's copy_to_new_table method, 
//   *   passing an inner lambda containing common code for copying old to new.
//   *   
//   *   That inner lambda, when invoked by the copy_to_new_table method, 
//   *     is passed another lambda of one argument (the old index) 
//   *     which is called to copy each element.  
//   * @param new_capacity
//   * @param old_capacity
//   */
//  private void copyOld2New(int new_capacity, int old_capacity) {
//    copy_to_new_table( 
//      new_capacity,
//      old_capacity,
//      
//        // this code assigned to "commonCopy" arg
//        (IntConsumer copyToNew, IntPredicate is_valid_old_key) -> {  
//      newTable(new_capacity);
//      for (int i = 0; i < old_capacity; i++) {
//        if (is_valid_old_key.test(i)) {
//          copyToNew.accept(i);
//        }
//      }
//    });    
//  }

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
    lower_hash_bits = init_lhb(capacity);
    setSmallerOfMask();
    bitMask = capacity - 1;
    sizeWhichTriggersExpansion = (int)(capacity * loadFactor);
  }
  
  protected void incrementSize() {
    size++;
    if (size >= sizeWhichTriggersExpansion) {
      maybeIncreaseTableCapacity();
    }
  }

  private void maybeIncreaseTableCapacity() {
    
    if (TUNE) {
      int old_capacity = keys_length();
      int new_capacity = 2 * old_capacity;
      System.out.println("Capacity increasing from " + old_capacity + " to " + new_capacity + ", size=" + size);
    }   
    expand_table();
  }
    
//  /**
//   * only called if actually found and removed an entry
//   */
//  protected void commonRemove() {
//    size --;
////    debugValidate();
//  }
  
  public int size() {
    return size;
  }
  
  protected void resetHistogram() {
//    if (TUNE) {
//      histogram = new int[200];
//      Arrays.fill(histogram, 0);
//      maxProbe = 0;
//    }    
  }

  private void updateHistogram(int extraProbes) {
    histogram[extraProbes] = 1 + histogram[extraProbes];
    if (maxProbe < extraProbes) {
      maxProbe = extraProbes;
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
      curPosition = moveToPreviousFilled(bitMask);
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
  
  protected int incrPos(int pos) {
    return bitMask & (pos + 1);
  }
  
  private byte[] init_lhb(int size) {
    byte[] lhb = new byte[size];
    Arrays.fill(lhb,  LHB_EMPTY);
    return lhb;
  }
  
  /**
   * Compute extra probes for an entry.
   * Handle cases where the table size is bigger or smaller than the
   * size of the lower-hash-bits
   * @param entry the lower-hash-bits, represents the original probe's last 7 bits
   * @param pos the current position
   * @return the extra distance of this entry from the original probe
   */
  private int extraProbesForEntry(byte entry, int pos) {
    return smaller_of_mask & (pos - entry);
  }
  
  private void setSmallerOfMask () {
    smaller_of_mask = Math.min(lower_hash_bits.length, 128) - 1; 
  }
}
