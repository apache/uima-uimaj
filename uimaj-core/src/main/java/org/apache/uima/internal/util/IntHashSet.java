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
import java.util.NoSuchElementException;

import org.apache.uima.jcas.impl.JCasHashMap;

/**
 * A set of non-zero ints.  
 *   
 * based on Int2IntHashMap
 * This impl is for use in a single thread case only
 * 
 * Supports shrinking (reallocating the big table)  
 *   
 */
public class IntHashSet {
  
  // set to true to collect statistics for tuning
  // you have to also put a call to showHistogram() at the end of the run
  private static final boolean TUNE = false;

  static int nextHigherPowerOf2(int i) {
    return (i < 1) ? 1 : Integer.highestOneBit(i) << ( (Integer.bitCount(i) == 1 ? 0 : 1));
  }
 
  // this load factor gives, for array doubling strategy:
  //   between 1.5 * 4 bytes (1 word for key) = 6 and
  //           3   * 4                          12 bytes per entry
  // This compares with 160 bytes/entry for the IntArrayRBT impl
  
  private final float loadFactor = 0.66F;  
  
  private final int initialCapacity; 

  private int histogram [];
  private int maxProbe = 0;

  private int sizeWhichTriggersExpansion;
  private int size; // number of elements in the table  
 
  private int [] keys;
  
  private boolean secondTimeShrinkable = false;
  
  private int mostPositive = 0;

  public IntHashSet() {
    this(16);
  }
  
  public IntHashSet(int initialCapacity) {
    this.initialCapacity = initialCapacity;
    newTableKeepSize(initialCapacity);
    size = 0;
    if (TUNE) {
      histogram = new int[200];
      Arrays.fill(histogram, 0);
      maxProbe = 0;
    }
  }
  
  private void newTableKeepSize(int capacity) {
    capacity = Math.max(16, nextHigherPowerOf2(capacity));
    keys = new int[capacity];
    sizeWhichTriggersExpansion = (int)(capacity * loadFactor);
  }

  private void incrementSize() {
    if (size >= sizeWhichTriggersExpansion) {
     increaseTableCapacity();
   }
   size++;
  }

  private void increaseTableCapacity() {
    final int [] oldKeys = keys;
    final int oldCapacity = oldKeys.length;
    int newCapacity = 2 * oldCapacity;
    
    if (TUNE) {
      System.out.println("Capacity increasing from " + oldCapacity + " to " + newCapacity);
    }
    newTableKeepSize(newCapacity);
    for (int key : oldKeys) {
      if (key != 0) {
        addInner(key);
      }
    }
  }
  
  // called by clear
  private void newTable(int capacity) {
    newTableKeepSize(capacity);
    size = 0;
    resetHistogram();
  }

  private void resetHistogram() {
    if (TUNE) {
      histogram = new int[200];
      Arrays.fill(histogram, 0);
      maxProbe = 0;
    }
  }
  
  public void clear() {
    // see if size is less than the 1/2 size that triggers expansion
    if (size <  (sizeWhichTriggersExpansion >>> 1)) {
      // if 2nd time then shrink by 50%
      //   this is done to avoid thrashing around the threshold
      if (secondTimeShrinkable) {
        secondTimeShrinkable = false;
        final int newCapacity = Math.max(initialCapacity, keys.length >>> 1);
        if (newCapacity < keys.length) { 
          newTable(newCapacity);  // shrink table by 50%
        } else { // don't shrink below minimum
          Arrays.fill(keys, 0);
        }
        size = 0;
        resetHistogram();
        return;
      } else {
        secondTimeShrinkable = true;
      }
    } else {
      secondTimeShrinkable = false; // reset this to require 2 triggers in a row
    }
    size = 0;
    Arrays.fill(keys, 0);
    resetHistogram();
  }

  /** 
  * returns a position in the key/value table
  *   if the key is not found, then the position will be to the
  *   place where the key would be put upon adding, and the 
  *   current value of keys[position] would be 0.
  *   
  *   if the key is found, then keys[position] == key
  * @param key -
  * @return the probeAddr in keys array - might have a 0 value, or the key value if found
  */
  private int find(final int key) {
    if (key == 0) {
      throw new IllegalArgumentException("0 is an invalid key");
    }

    final int hash = JCasHashMap.hashInt(key);
    int nbrProbes = 1;
    final int[] localKeys = keys;
    final int bitMask = localKeys.length - 1;
    int probeAddr = hash & bitMask;
    int probeDelta = 1;

    while (true) {
      final int testKey = localKeys[probeAddr];
      if (testKey == 0 || testKey == key) {
        break;
      }
      nbrProbes++;
      probeAddr = bitMask & (probeAddr + (probeDelta++));
    }

    if (TUNE) {
      final int pv = histogram[nbrProbes];

      histogram[nbrProbes] = 1 + pv;
      if (maxProbe < nbrProbes) {
        maxProbe = nbrProbes;
      }
    }

    return probeAddr;
  }
   
  public boolean contains(int key) {
    return (key == 0) ? false : keys[find(key)] == key;
  }
 
  /**
   * 
   * @param key
   * @return true if this set did not already contain the specified element
   */
  public boolean add(int key) {
    if (key > mostPositive) {
      mostPositive = key;
    }
    final int i = find(key);
    if (keys[i] == 0) {
      keys[i] = key;
      incrementSize();
      return true;
    }
    return false;
  }
  
  /**
   * 
   * @param key
   */
  private void addInner(int key) {
    final int i = find(key);
    assert(keys[i] == 0);
    keys[i] = key;
  }


  public int size() {
    return size;
  }
  
  public int getMostPositive() {
    return mostPositive;
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
      
      System.out.println("bytes / entry = " + (float) (keys.length) * 4 / size());
      System.out.format("size = %,d, prevExpansionTriggerSize = %,d, next = %,d%n",
          size(),
          (int) ((keys.length >>> 1) * loadFactor),
          (int) (keys.length * loadFactor));
    }
  }
  
  /**
   * advance pos until it points to a non 0 or is 1 past end
   * @param pos
   * @return
   */
  private int moveToNextFilled(int pos) {
    if (pos < 0) {
      pos = 0;
    }
    
    final int max = keys.length;
    while (true) {
      if (pos >= max) {
        return pos;
      }
      if (keys[pos] != 0) {
        return pos;
      }
      pos ++;
    }
  }
   
  /**
   * decrement pos until it points to a non 0 or is -1
   * @param pos
   * @return
   */
  private int moveToPreviousFilled(int pos) {
    final int max = keys.length;
    if (pos > max) {
      pos = max - 1;
    }
    
    while (true) {
      if (pos < 0) {
        return pos;
      }
      if (keys[pos] != 0) {
        return pos;
      }
      pos --;
    }
  }

  private class IntHashSetIterator implements IntListIterator {

    private int curPosition;

    private IntHashSetIterator() {
      this.curPosition = 0;
    }

    public final boolean hasNext() {
      curPosition = moveToNextFilled(curPosition);
      return curPosition < keys.length;
    }

    public final int next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return keys[curPosition++];
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#hasPrevious()
     */
    public boolean hasPrevious() {
      curPosition = moveToPreviousFilled(curPosition);
      return (curPosition >= 0);
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#previous()
     */
    public int previous() {
      if (!hasPrevious()) {
        throw new NoSuchElementException();
      }
      return keys[curPosition--];
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#moveToEnd()
     */
    public void moveToEnd() {
      curPosition = keys.length - 1;
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#moveToStart()
     */
    public void moveToStart() {
      curPosition = 0;
    }
  }
  
  
  public IntListIterator getIterator() {
    return new IntHashSetIterator();
  }

}
