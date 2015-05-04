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
 *   0 reserved internally to indicate "not in the map";
 *   you will get an exception if you try to store 0 as a value.
 *   
 *   0 will be returned if the value is missing from the map.
 *    
 *   allowed range is Integer.MIN_VALUE + 1 to Integer.MAX_VALUE 
 *     0 is the value for an empty cell
 *     Integer.MIN_VALUE is the value for a deleted (removed) value
 *   
 * based on Int2IntHashMap
 * This impl is for use in a single thread case only
 * 
 * Supports shrinking (reallocating the big table)
 *
 * Supports representing ints as "short" 2byte values if possible,
 *   together with an offset amount.
 *   Because of the offset, the adjusted key could be == to the offset,
 *   so we subtract 1 from it to preserve 0 value as being the null / empty.
 *   For short values, the range is:
 *        Short.MIN_VALUE+2 to Short.MAX_VALUE after Offset,
 *        with the "0" value moved down by 1 and 
 *        the Short.MIN_VALUE used for deleted (removed) items 
 *   
 *   Automatically switches to full int representation if needed  
 */
public class IntHashSet implements PositiveIntSet {
  
  public static final int SIZE_NEEDING_4_BYTES = 256 * 256 - 2;  
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
  private int nbrRemoved; // number of removed elements (coded as removed)
  
  // offset only used with keys2.  values stored are key - offset
  //   intent is to have them fit into short data type.
  //   If the offset is 100,000, then the keys range from 100000 - 32766 to 100000 + 32767, includes "0"
  //                 -32767 == Short.MIN_VALUE + 1,   
  //                 Numbers 0 and below are stored as n - 1, so "0" itself isn't actually stored
  //                   because it's used to represent an empty slot
  //   - value after key adjustment of 0 reserved; if value is 0 - means no entry
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
   * @param numberOfElements -
   * @return -
   */
  public int tableSpace(int numberOfElements) {
    return tableSpace(numberOfElements, loadFactor);
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
    return  Math.max(16, nextHigherPowerOf2(capacity));
  }
  
  private void newTableKeepSize(int capacity, boolean make4) {
    if (!make4) {
      capacity = Math.max(4, nextHigherPowerOf2(capacity));
      make4 = (capacity >= SIZE_NEEDING_4_BYTES);
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
    nbrRemoved = 0;
  }

  private void incrementSize() {
    if ((size + nbrRemoved) >= sizeWhichTriggersExpansion) {
      increaseTableCapacity();
    }
    size++;
  }
  
  public boolean wontExpand() {
    return wontExpand(1);
  }
  
  public boolean wontExpand(int n) {
    return (size + nbrRemoved + n) < sizeWhichTriggersExpansion;  
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
  
  /**
   * Only call this if using short values with offset
   * @param adjKey
   * @return raw key
   */
  private int getRawFromAdjKey(int adjKey) {
    assert (adjKey != Short.MIN_VALUE);
    return adjKey + offset + ((adjKey < 0) ? 1 : 0); 
  }
  
  private void increaseTableCapacity() {
    final int oldCapacity = getCapacity();
    // keep same capacity if just removing the "removed" markers would 
    // shrink the used slots to the same they would have been had there been no removed, and 
    // the capacity was doubled.
    final int newCapacity = (nbrRemoved > size) ? oldCapacity : oldCapacity << 1;
    
    if (keys2 != null) {
      final short[] oldKeys = keys2;
      newTableKeepSize(newCapacity, false);
      if (newCapacity >= 256 * 256) {  // = 65536
        // switch to 4
        if (TUNE) {System.out.println("Switching to 4 byte keys, Capacity increasing from " + oldCapacity + " to " + newCapacity);}
        for (short adjKey : oldKeys) {
          if (adjKey != 0 && adjKey != Short.MIN_VALUE) {
            addInner4(getRawFromAdjKey(adjKey));
          }
        }
        
      } else {
        if (TUNE) {System.out.println("Keeping 2 byte keys, Capacity increasing from " + oldCapacity + " to " + newCapacity);}
        for (short adjKey : oldKeys) {
          if (adjKey != 0 && adjKey != Short.MIN_VALUE) {
            addInner2(adjKey);
          }
        }
      }
      
    } else {
      final int [] oldKeys = keys4;      
      if (TUNE) {System.out.println("Capacity increasing from " + oldCapacity + " to " + newCapacity);}
      newTableKeepSize(newCapacity, true);
      for (int key : oldKeys) {
        if (key != 0 && key != Integer.MIN_VALUE) {
          addInner4(key);
        }
      }
    }
  }
  
  // called by clear
  private void newTable(int capacity) {
    newTableKeepSize(capacity, false);
    resetTable();
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
    resetTable();
  }
  
  private void resetTable() {
    mostPositive = Integer.MIN_VALUE;
    mostNegative = Integer.MAX_VALUE;
    resetHistogram();
    size = 0;
    nbrRemoved = 0;    
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
  *   current internal value of keys[position] would be 0.
  *   
  *   if the key is found, then keys[position] == key
  * @param adjKey - raw key - offset (-1 if this value is 0 or negative, to skip 0, if shorts)
  * @return the probeAddr in keys array - might have a 0 value, or the key value if found
  */
  private int findPosition(final int adjKey) {
    if (adjKey == 0) {
      throw new IllegalArgumentException("0 is an invalid key");
    }

    final int hash = JCasHashMap.hashInt(adjKey);
    int nbrProbes = 1;
    int probeDelta = 1;
    int probeAddr;
    
    if (keys4 == null) {
      final short[] localKeys2 = keys2;
      final int bitMask = localKeys2.length - 1;
      probeAddr = hash & bitMask;
      while (true) {
        final int testKey = localKeys2[probeAddr];
        if (testKey == 0 || testKey == adjKey) {
          break;
        }
        nbrProbes++;
        probeAddr = bitMask & (probeAddr + (probeDelta++));
      }
    } else {
      final int[] localKeys4 = keys4;
      final int bitMask = localKeys4.length - 1;
      probeAddr = hash & bitMask;
      while (true) {
        final int testKey = localKeys4[probeAddr];
        if (testKey == 0 || testKey == adjKey) {
          break;
        }
        nbrProbes++;
        probeAddr = bitMask & (probeAddr + (probeDelta++));
      }
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
   
  // only called when keys are shorts
  private boolean isAdjKeyOutOfRange(int adjKey) {
    return (adjKey > Short.MAX_VALUE ||
           // the minimum adjKey value stored in a short is
           // Short.MIN_VALUE + 1
            adjKey <= Short.MIN_VALUE);   
  }
  
  @Override
  public boolean contains(int rawKey) {
    return find(rawKey) != -1;
  }
  
  @Override
  public int find(int rawKey) {
    if (rawKey == 0) {
      return -1;
    }

    if (keys4 == null) {
      // check for keys never stored in short table 
      //   adjKey of Short.MIN_VALUE which is the removed flag
      final int adjKey = getAdjKey(rawKey);
      if (isAdjKeyOutOfRange(adjKey)) {
        return -1;
      }
      final int pos = findPosition(adjKey);   
      return (keys2[pos] == adjKey) ? pos : -1;
    }
    
    // using 4 byte keys, no offset
    if (rawKey == Integer.MIN_VALUE) {
      return -1;  // not stored in table (reserved value for removed items)
    }
    final int pos = findPosition(rawKey);
    return (keys4[pos] == rawKey) ? pos : -1;
  }
   
  /**
   * return the adjusted key.
   *   never called for 4 byte form
   *   for 2 byte key mode, subtract the offset, and adjust by -1 if 0 or less
   *     Note: returned value can be less than Short.MIN_VALUE 
   * @param rawKey
   * @return adjusted key
   */
  private int getAdjKey(int rawKey) {
//    if (rawKey == 0 || (rawKey == Integer.MIN_VALUE)) {
//      throw new ArrayIndexOutOfBoundsException(rawKey);
//    }
//    if (keys4 != null) {
//      return rawKey;
//    }
    int adjKey = rawKey - offset;
    return adjKey - (  (adjKey <= 0) ? 1 : 0);
  }
  
  private void switchTo4byte() {
    // convert to 4 byte because values can't be offset and fit in a short
    final short[] oldKeys = keys2;
    newTableKeepSize(getCapacity(), true);  // make a 4 table. same size
    for (short adjKey : oldKeys) {
      if (adjKey != 0 && adjKey != Short.MIN_VALUE) {
        addInner4(getRawFromAdjKey(adjKey));
      }
    } 
  }
   
  /**
   * 
   * @param rawKey -
   * @return true if this set did not already contain the specified element
   */
  @Override
  public boolean add(int rawKey) {
    if (rawKey == 0) {
      throw new IllegalArgumentException("argument must be non-zero");
    }
       
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
    
    if (keys4 != null) {
      return find4(rawKey);
      
      // short keys
    } else {
      int adjKey = getAdjKey(rawKey);
      if (isAdjKeyOutOfRange(adjKey)) {
        switchTo4byte();
        return find4(rawKey);
        
        // key in range
      } else {
        final int i = findPosition(adjKey);
        if (keys2[i] == adjKey) {
          return false;
        }
        keys2[i] = (short) adjKey;
        incrementSize();
        return true;
      }
    }
  }
  
  private boolean find4(int rawKey) {
    final int i = findPosition(rawKey);
    if (keys4[i] == rawKey) {
      return false;
    }
    keys4[i] = rawKey;
    incrementSize();
    return true;
  }
  
//    int adjKey = getAdjKey(rawKey);
//
//    if (keys4 == null && isAdjKeyOutOfRange(adjKey)) {
//      switchTo4byte();
//      adjKey = rawKey;
//    }
//    final int i = findPosition(adjKey);
//    if (keys4 == null) {
//      if (keys2[i] == 0) {
//        keys2[i] = (short) adjKey;
//      } else {
//        return false;
//      }
//    } else { 
//      if (keys4[i] == 0) {
//        keys4[i] = adjKey;
//      } else {
//        return false;
//      }
//    }
//    incrementSize();
//    return true;
//  }
      
  /**
   * used for increasing table size
   * @param rawKey
   */
  private void addInner4(int rawKey) {
    final int i = findPosition(rawKey);
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
   *   but mostPositive is always &gt;= actual most positive,
   *   and mostNegative is always &lt;= actual most negative.
   * No conversion from int to short
   * 
   * Can't replace the item with a 0 because other keys that were
   * stored in the table which previously collided with the removed item
   * won't be found.  UIMA-4204
   * @param rawKey the value to remove
   * @return true if the key was present
   */
  @Override
  public boolean remove(int rawKey) {
    final int pos = find(rawKey);
    if (pos < 0) {
      return false;
    }
    
    if (keys4 == null) {
      keys2[pos] = Short.MIN_VALUE;
    } else {
      keys4[pos] = Integer.MIN_VALUE;
    }
    
    size--;
    nbrRemoved ++;
    if (rawKey == mostPositive) {
      mostPositive --;  // a weak adjustment
    }
    if (rawKey == mostNegative) {
      mostNegative ++;  // a weak adjustment
    }
    return true;
  }    
  
  @Override
  public int size() {
    return size;
  }
  
  /**
   * 
   * @return a value that is &gt;= the actual most positive value in the table.
   *   it will be == unless a remove operation has removed a most positive value
   */
  public int getMostPositive() {
    return mostPositive;
  }
  
  /**
   * 
   * @return a value that is &lt;= the actual least positive value in the table.
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
  
  /**
   * For iterator use, position is a magic number returned by the internal find
   * For short keys, the value stored for adjKey == 0 is -1, adjKey == -1 is -2, etc.
   */
  @Override
  public int get(int index) {
    final int adjKey;
    if (keys4 == null) {
      adjKey = keys2[index];
      if (adjKey == 0 || adjKey == Short.MIN_VALUE) {
        return 0;  // null, not present
      }
      return getRawFromAdjKey(adjKey);
    } else {
      adjKey = keys4[index];
      if (adjKey == 0 || adjKey == Integer.MIN_VALUE) {
        return 0;  // null, not present
      }
      return adjKey;
    }
  }
  
  /**
   * advance pos until it points to a non 0 or is 1 past end
   * @param pos
   * @return updated pos
   */
  private int moveToNextFilled(int pos) {
    if (pos < 0) {
      pos = 0;
    }
    
    final int max = getCapacity();
    if (null == keys4) {
      while (true) {
        if (pos >= max) {
          return pos;
        }
        int v = get(pos);
        if (v != 0 && v != Short.MIN_VALUE) {
          return pos;
        }
        pos++;
      }
    } else {
      // keys4 case
      while (true) {
        if (pos >= max) {
          return pos;
        }
        int v = get(pos);
        if (v != 0 && v != Integer.MIN_VALUE) {
          return pos;
        }
        pos ++;
      }
    }
  }
   
  /**
   * decrement pos until it points to a non 0 or is -1
   * @param pos
   * @return updated pos
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
      int v = get(pos);
      if (v != 0 && v != ( (keys4 == null) ? Short.MIN_VALUE : Integer.MIN_VALUE)) {
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
    if (position < 0) {
      return position;
    }
    final int n = moveToNextFilled(position + 1); 
    return (n >= getCapacity()) ? -1 : n;
  }

  @Override
  public int moveToPrevious(int position) {
    if (position >= getCapacity()) {
      return -1;
    }
    return moveToPreviousFilled(position - 1);
  }

  @Override
  public boolean isValid(int position) {
    return (position >= 0) && (position < getCapacity());
  }

  @Override
  public void bulkAddTo(IntVector v) {
    if (null == keys4) {
      for (int k : keys2) {
        if (k != 0 && k != Short.MIN_VALUE) {
          v.add(getRawFromAdjKey(k));
        }
      }
    } else {
      for (int k : keys4) {
        if (k != 0 && k != Integer.MIN_VALUE) {
          v.add(k);
        }
      }
    }
  }

  @Override
  public int[] toIntArray() {
    final int s = size();
    if (s == 0) {
      return PositiveIntSet_impl.EMPTY_INT_ARRAY;
    }
    final int[] r = new int[size()];
    int pos = moveToFirst();
    for (int i = 0; i < r.length; i ++) {
      r[i] = get(pos);
      pos = moveToNextFilled(pos + 1);
    }
    return r;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return String
        .format(
            "IntHashSet [loadFactor=%s, initialCapacity=%s, sizeWhichTriggersExpansion=%s, size=%s, offset=%s%n keys4=%s%n keys2=%s%n secondTimeShrinkable=%s, mostPositive=%s, mostNegative=%s]",
            loadFactor, initialCapacity, sizeWhichTriggersExpansion, size, offset,
            Arrays.toString(keys4), Arrays.toString(keys2), secondTimeShrinkable, mostPositive,
            mostNegative);
  }
  
  // for testing
  boolean isShortHashSet() {
    return keys2 != null;
  }
  
  int getOffset() {
    return offset;
  }
  
  
}
