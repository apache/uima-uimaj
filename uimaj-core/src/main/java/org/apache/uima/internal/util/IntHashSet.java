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
 *   0 reserved to indicate "not in the map"  
 *   
 * based on Int2IntHashMap
 * This impl is for use in a single thread case only
 * 
 * Supports shrinking (reallocating the big table)
 *
 * Supports representing ints as "short" 2byte values if possible,
 *   together with an offset amount.
 *   
 *   Automatically switches to full int representation if needed  
 */
public class IntHashSet implements PositiveIntSet {
  
  public static final float DEFAULT_LOAD_FACTOR = 0.66F;
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
  
  private final float loadFactor = DEFAULT_LOAD_FACTOR;  
  
  private final int initialCapacity; 

  private int histogram [];
  private int maxProbe = 0;

  private int sizeWhichTriggersExpansion;
  private int size; // number of elements in the table
  
  // offset only used with keys2.  values stored are key - offset
  //   intent is to have them fit into short data type.
  //   - value of 0 reserved; if value is 0 - means no entry
  //   store values at or below the offset are stored as 1 less to avoid mapping to "0".
  private int offset;
 
  private int [] keys4;
  private short[] keys2;
  
  private boolean secondTimeShrinkable = false;
  
  // these are true values (before any offset adjustment)
  private int mostPositive = Integer.MIN_VALUE;
  private int mostNegative = Integer.MAX_VALUE;

  public IntHashSet() {
    this(12, 0);
  }
  
  public IntHashSet(int initialCapacity) {
    this(initialCapacity, 0);
  }
  
  /**
   * 
   * @param initialCapacity - you can add this many before expansion
   * @param offset - for values in the short range, the amount to subtract
   *                 before storing.
   *                 If == MIN_VALUE, then force 4 byte ints
   */
  public IntHashSet(int initialCapacity, int offset) {
    this.initialCapacity = initialCapacity;
    boolean force4 = offset == Integer.MIN_VALUE;
    this.offset = force4 ? 0 : offset;
    newTableKeepSize(tableSpace(initialCapacity), force4);
    size = 0;
    if (TUNE) {
      histogram = new int[200];
      Arrays.fill(histogram, 0);
      maxProbe = 0;
    }
  }
  
  /**
   * The number of 32 bit words that are reserved when 
   * creating a table to hold the specified number of elements
   * 
   * The number is a power of 2.
   * 
   * The number is at least 16.
   * 
   * The number is such that you could add this many elements without
   *   triggering the capacity expansion.
   *   
   * @param numberOfElements
   * @return
   */
  public int tableSpace(int numberOfElements) {
    return tableSpace(numberOfElements, loadFactor);
  }
  
  /**
   * 
   * @param numberOfElements
   * @param factor
   * @return capacity of the main table (either 2 byte or 4 byte entries)
   */
  public static int tableSpace(int numberOfElements, Float factor) {
    if (numberOfElements < 0) {
      throw new IllegalArgumentException("must be > 0");
    }
    final int capacity = Math.round(numberOfElements / factor);
    return  Math.max(16, nextHigherPowerOf2(capacity));
  }
  
  private void newTableKeepSize(int capacity, boolean make4) {
    if (!make4) {
      capacity = Math.max(4, nextHigherPowerOf2(capacity));
      make4 = (capacity >= 65536);
    }
    // don't use short values unless 
    //    the number of items is < 65536
    if (make4) {
      keys4 = new int[capacity];
      keys2 = null;
    } else {
      keys2 = new short[capacity];
      keys4 = null;
    }
    sizeWhichTriggersExpansion = (int)(capacity * loadFactor);
  }

  private void incrementSize() {
    if (size >= sizeWhichTriggersExpansion) {
     increaseTableCapacity();
   }
   size++;
  }
  
  public boolean wontExpand() {
    return wontExpand(1);
  }
  
  public boolean wontExpand(int n) {
    return (size + n) < sizeWhichTriggersExpansion;  
  }
  
  public int getSpaceUsedInWords() {
    return (keys4 != null) ? keys4.length : (keys2.length >> 1);  
  }
  
  public static int getSpaceOverheadInWords() {
    return 11;
  }
  
  private int getCapacity() {
    return (null == keys4) ? keys2.length : keys4.length;
  }
  
  private void increaseTableCapacity() {
    final int oldCapacity = getCapacity();
    final int newCapacity = oldCapacity << 1;
    
    if (keys2 != null) {
      final short[] oldKeys = keys2;
      newTableKeepSize(newCapacity, false);
      if (newCapacity > 32768) {
        // switch to 4
        if (TUNE) {System.out.println("Switching to 4 byte keys, Capacity increasing from " + oldCapacity + " to " + newCapacity);}
        for (short adjKey : oldKeys) {
          if (adjKey != 0) {
            addInner4(adjKey + offset);
          }
        }
        
      } else {
        if (TUNE) {System.out.println("Keeping 2 byte keys, Capacity increasing from " + oldCapacity + " to " + newCapacity);}
        for (short adjKey : oldKeys) {
          if (adjKey != 0) {
            addInner2(adjKey);
          }
        }
      }
      
    } else {
      final int [] oldKeys = keys4;      
      if (TUNE) {System.out.println("Capacity increasing from " + oldCapacity + " to " + newCapacity);}
      newTableKeepSize(newCapacity, true);
      for (int key : oldKeys) {
        if (key != 0) {
          addInner4(key);
        }
      }
    }
  }
  
  // called by clear
  private void newTable(int capacity) {
    newTableKeepSize(capacity, false);
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
  
  private void resetArray() {
    if (keys4 == null) {
      Arrays.fill(keys2,  (short)0);
    } else {
      Arrays.fill(keys4, 0);
    }
    mostPositive = Integer.MIN_VALUE;
    mostNegative = Integer.MAX_VALUE;
    resetHistogram();
    size = 0;
  }
  
  @Override
  public void clear() {
    // see if size is less than the 1/2 size that triggers expansion
    if (size <  (sizeWhichTriggersExpansion >>> 1)) {
      // if 2nd time then shrink by 50%
      //   this is done to avoid thrashing around the threshold
      if (secondTimeShrinkable) {
        secondTimeShrinkable = false;
        final int currentCapacity = getCapacity();
        final int newCapacity = Math.max(initialCapacity, currentCapacity >>> 1);
        if (newCapacity < currentCapacity) { 
          newTable(newCapacity);  // shrink table by 50%
        } else { // don't shrink below minimum
          resetArray();
        }
        return;
        
      } else {
        secondTimeShrinkable = true;
      }
    } else {
      secondTimeShrinkable = false; // reset this to require 2 triggers in a row
    }
   resetArray();
  }

  /** 
  * returns a position in the key/value table
  *   if the key is not found, then the position will be to the
  *   place where the key would be put upon adding, and the 
  *   current value of keys[position] would be 0.
  *   
  *   if the key is found, then keys[position] == key
  * @param adjKey -
  * @return the probeAddr in keys array - might have a 0 value, or the key value if found
  */
  private int findPosition(final int adjKey) {
    if (adjKey == 0) {
      throw new IllegalArgumentException("0 is an invalid key");
    }

    final int hash = JCasHashMap.hashInt(adjKey);
    int nbrProbes = 1;
    final int[] localKeys4 = keys4;
    final short[] localKeys2 = keys2;
    final int bitMask = ((localKeys4 == null) ? localKeys2.length : localKeys4.length) - 1;
    int probeAddr = hash & bitMask;
    int probeDelta = 1;

    while (true) {
      final int testKey = (localKeys4 != null) ? localKeys4[probeAddr] : localKeys2[probeAddr];
      if (testKey == 0 || testKey == adjKey) {
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
   
  @Override
  public boolean contains(int rawKey) {
    if (keys4 == null) {
      final int adjKey = getAdjKey(rawKey); 
      return (rawKey == 0) ? false : keys2[findPosition(adjKey)] == adjKey;
    }
    return (rawKey == 0) ? false : keys4[findPosition(rawKey)] == rawKey;
  }
  
  @Override
  public int find(int rawKey) {
    final int adjKey = getAdjKey(rawKey);
    final int pos = findPosition(adjKey);
    if (keys4 == null) {
      return (keys2[pos] == adjKey) ? pos : -1;
    } else {
      return (keys4[pos] == adjKey) ? pos : -1;
    }
  }
 
  private int getAdjKey(int rawKey) {
    if (keys4 != null) {
      return rawKey;
    }
    int adjKey = rawKey - offset;
    if (adjKey <= 0) {
      adjKey --;  // because 0 is reserved for null
    }
    if ((adjKey > 32767) || (adjKey < -32768)) {
      // convert to 4 byte because values can't be offset and fit in a short
      final short[] oldKeys = keys2;
      newTableKeepSize(getCapacity(), true);  // make a 4 table
      // next fails to convert short to int
//      System.arraycopy(oldKeys, 0,  keys4,  0,  oldKeys.length);
      int i = 0;
      for (int v : oldKeys) {
        keys4[i++] = v;
      }
      return rawKey; // now using 4 byte table
    } else {     
      return adjKey;
    }
  }
  
  /**
   * 
   * @param key
   * @return true if this set did not already contain the specified element
   */
  @Override
  public boolean add(int rawKey) {
    final int adjKey = getAdjKey(rawKey);
    if (size == 0) {
      mostPositive = mostNegative = rawKey;
    } else {
      if (rawKey > mostPositive) {
        mostPositive = rawKey;
      }
      if (rawKey < mostNegative) {
        mostNegative = rawKey;
      }
    }
    final int i = findPosition(adjKey);
    if (keys4 == null) {
      if (keys2[i] == 0) {
        keys2[i] = (short) adjKey;
        incrementSize();
        return true;
      }
      return false;
    } 
    if (keys4[i] == 0) {
      keys4[i] = adjKey;
      incrementSize();
      return true;
    }
    return false;
  }
      
  /**
   * used for increasing table size
   * @param rawKey
   */
  private void addInner4(int rawKey) {
    final int i = findPosition(rawKey);
    if (null == keys4) {
      System.out.println("debug stop");
    }
    assert(keys4[i] == 0);
    keys4[i] = rawKey;
  }
  
  private void addInner2(short adjKey) {
    final int i = findPosition(adjKey);
    assert(keys2[i] == 0);
    keys2[i] = adjKey;
  }
  
  /**
   * mostPositive and mostNegative are not updated
   *   for removes.  So these values may be inaccurate,
   *   but mostPositive is always >= actual most positive,
   *   and mostNegative is always <= actual most negative.
   * No conversion from int to short
   * @param rawKey -
   * @return true if the key was present
   */
  @Override
  public boolean remove(int rawKey) {
    final int adjKey = getAdjKey(rawKey);
    final int i = findPosition(adjKey);
    boolean removed = false;
    if (keys4 == null) {
      if (keys2[i] != 0) {
        keys2[i] = 0;
        removed = true;
      }
    } else {
      if (keys4[i] != 0) {
        keys4[i] = 0;
        removed = true;
      }
    }
    
    if (removed) {
      size--;
      if (rawKey == mostPositive) {
        mostPositive --;  // a weak adjustment
      }
      if (rawKey == mostNegative) {
        mostNegative ++;  // a weak adjustment
      }
      return true;
    } 
    return false;
  }
  
  @Override
  public int size() {
    return size;
  }
  
  /**
   * 
   * @return a value that is >= the actual most positive value in the table.
   *   it will be == unless a remove operation has removed a most positive value
   */
  public int getMostPositive() {
    return mostPositive;
  }
  
  /**
   * 
   * @return a value that is <= the actual least positive value in the table.
   *   It will be == unless remove operations has removed a least positive value.
   */
  public int getMostNegative() {
    return mostNegative;
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
      
      if (keys4 == null) {
        System.out.println("bytes / entry = " + (float) (keys2.length) * 2 / size());
        System.out.format("size = %,d, prevExpansionTriggerSize = %,d, next = %,d%n",
          size(),
          (int) ((keys2.length >>> 1) * loadFactor),
          (int) (keys2.length * loadFactor));
      } else {
        System.out.println("bytes / entry = " + (float) (keys4.length) * 4 / size());
        System.out.format("size = %,d, prevExpansionTriggerSize = %,d, next = %,d%n",
          size(),
          (int) ((keys4.length >>> 1) * loadFactor),
          (int) (keys4.length * loadFactor));        
      }
    }
  }
  
  @Override
  public int get(int index) {
    return offset + ((keys4 == null) ? keys2[index] : keys4[index]);
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
    
    final int max = getCapacity();
    while (true) {
      if (pos >= max) {
        return pos;
      }
      if (get(pos) != 0) {
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
    final int max = getCapacity();
    if (pos > max) {
      pos = max - 1;
    }
    
    while (true) {
      if (pos < 0) {
        return pos;
      }
      if (get(pos) != 0) {
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
      return curPosition < getCapacity();
    }

    public final int next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return get(curPosition++);
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
      return get(curPosition--);
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#moveToEnd()
     */
    public void moveToEnd() {
      curPosition = getCapacity() - 1;
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#moveToStart()
     */
    public void moveToStart() {
      curPosition = 0;
    }
  } 
  
  @Override
  public IntListIterator iterator() {
    return new IntHashSetIterator();
  }

  @Override
  public int moveToFirst() {
    return (size() == 0) ? -1 : moveToNextFilled(0);
  }

  @Override
  public int moveToLast() {
    return (size() == 0) ? -1 : moveToPreviousFilled(getCapacity() -1);
  }

  @Override
  public int moveToNext(int position) {
    final int n = moveToNextFilled(position + 1); 
    return (n == getCapacity()) ? -1 : n;
  }

  @Override
  public int moveToPrevious(int position) {
    return moveToPreviousFilled(position - 1);
  }

  @Override
  public boolean isValid(int position) {
    return (position >= 0) && (position < getCapacity());
  }
}
