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



/**
 * An set of non-zero ints.
 * 
 *   Uses 2 1/2 different representations depending on the ratio of the max int stored and the total number of ints stored.
 *     For large sparse ints, it uses the IntHashSet
 *     
 *     For more dense ints, it uses bitSets or bitSets with an offset
 *     
 *     It switches when adding new members if the other representation is sufficiently smaller (using a Hysteresis of 16 words) 
 */
public class PositiveIntSet {
  
  // number of words a inthashset has to be better than intbitset to cause switch
  private static final int HYSTERESIS = 16;

  private IntBitSet intBitSet;
  
  private IntHashSet intHashSet;
  
  //package private for test case
  boolean isBitSet;  
  
  /**
   * Set false once we find we have to reduce the bit offset
   */
  //package private for test case
  boolean useOffset = true;
  
  public PositiveIntSet() {
    intBitSet = new IntBitSet();
    isBitSet = true;
  }
  
  public IntListIterator getUnorderedIterator() {
    if (isBitSet) {
      return intBitSet.getIterator();
    }
    return intHashSet.getIterator();
  }
  
  public IntListIterator getOrderedIterator() {
    if (isBitSet) {
      return intBitSet.getIterator();
    }
    
    int[] allValues = new int[size()];
    IntListIterator it = intHashSet.getIterator();
    int i = 0;
    while (it.hasNext()) {
      allValues[i++] = it.next();
    }
    Arrays.sort(allValues);
    return new IntListIteratorOverArray(allValues);
  }
  
  private class IntListIteratorOverArray implements IntListIterator {
    private int[] a;
    private int pos;
    
    IntListIteratorOverArray(int[] a) {
      this.a = a;
      pos = 0;
    }
    
    @Override
    public boolean hasNext() {
      return pos >= 0 && pos < a.length;
    }

    @Override
    public int next() throws NoSuchElementException {
      if (hasNext()) {
        return a[pos++];
      }
      throw new NoSuchElementException();
    }

    @Override
    public boolean hasPrevious() {
      return pos >= 0 && pos < a.length;      
    }

    @Override
    public int previous() {
      if (hasPrevious()) {
        return a[pos--];
      }
      throw new NoSuchElementException();
    }

    @Override
    public void moveToStart() {
      pos = 0;
    }

    @Override
    public void moveToEnd() {
      pos = a.length - 1;
    }   
  }
  
  public void clear() {
    if (isBitSet) {
      intBitSet.clear();
    } else {
      intHashSet.clear();
    }
  }
  
  public boolean contains(int key) {
    if (isBitSet) {
      return intBitSet.contains(key);
    } else {
      return intHashSet.contains(key);
    }
  }
 
  /**
   * 
   * @param key -
   * @return true if this set did not already contain the specified element
   */
  public boolean add(int key) {
    maybeSwitchRepresentation(key);
    if (isBitSet) {
      return intBitSet.add(key);
    } else {
      return intHashSet.add(key);
    }
  }

  /**
   * When a key is added which is lower than the offset, we need to adjust the whole int set table.
   * Although an array copy could do this, we don't have access to the bitset impl.
   * 
   * So we just set the offset to 0, and either convert the whole thing to a inthashset or copy all the bits to a new
   * bit set.
   * 
   * @param key
   */
  private void adjustBitSetForLowerOffset(int key) {
    final int largestInt = intBitSet.getLargestMenber();
    final int bitSetSpaceNeeded = getBitSetSpaceFromMaxInt(largestInt);  // doesn't use offset
    final int hashSetSpaceNeeded = getHashSetSpace(intBitSet.size());
    
    if (hashSetSpaceNeeded < (bitSetSpaceNeeded - HYSTERESIS)) {
      switchToHashSet(size());
    } else {
      IntBitSet bs = new IntBitSet(largestInt);
      IntListIterator it = intBitSet.getIterator();
      while (it.hasNext()) {
        bs.add(it.next());
      }
      intBitSet = bs;      
    }
    useOffset = false;  // stop using because we have evidence it isn't appropriate
  }
  
  public void add(int[] vs) {
    for (int i : vs) {
      add(i);
    }
  }
  
  public boolean remove(int key) {
    if (isBitSet) {
      return intBitSet.remove(key);
    } else {
      return intHashSet.remove(key);
    }   
  }

  public int size() {
    if (isBitSet) {
      return intBitSet.size();
    } else {
      return intHashSet.size();
    }
  }
  
  public int[] toUnorderedIntArray() {
    final int[] a = new int[size()];
    IntListIterator it = getUnorderedIterator();
    int i = 0;
    while (it.hasNext()) {
      a[i++] = it.next();
    }
    return a;
  }
  
  public int[] toOrderedIntArray() {
    int [] a = toUnorderedIntArray();
    if (isBitSet) {
      return a;
    } 
    Arrays.sort(a);
    return a;
  }
  
  /** 
   * number of 32 bit words in use by bit set, given the max key (less offset)
   * @param maxKeyLessOffset
   * @return number of words needed to store the highest value
   */
  private int getBitSetSpaceFromMaxInt(int maxKeyLessOffset) {
    // e.g., 31 key => 1 word, 32 key => 2 words
    return 2 + (maxKeyLessOffset >> 5) + 2;  // add 2 because the bits are stored in 2 words (1 long), and 2 more for IntBitSet object overhead
  }
  
  private int getHashSetSpace(int numberOfEntries) {
    long v = numberOfEntries * 3 + 8;  // plus 8 is for the overhead of the intHashSet object.
    if (v > Integer.MAX_VALUE) {
      throw new RuntimeException("value overflowed int, orig was " + numberOfEntries);
    }
    return (int) v;   // the number of 32 bit words needed is 1.5 to 3; 3 is worst case
  }
  
  /**
   * logic for switching representation
   * 
   * BitSet preferred - is faster, can be more compact, and permits ordered iteration
   * 
   * HashSet used when BitSet is too space inefficient.
   *    MaxInt stored determines size: = MaxInt / 8 bytes.
   *    HashSet takes 12 bytes / entry.
   *    
   *    switch to HashSet from BitSet if:
   *      HashSet space   <   BitSet space  
   *       size * 12            MaxInt / 8
   *    with hysteresis  
   *
   *   switch to BitSet from HashSet if:
   *      BitSet used when its space requirement is less than hashset, with hysteresis:
   *           
   */
  
  private void maybeSwitchRepresentation(int key) {
    if (isBitSet) {
      /********************
       *    Bit Set 
       ********************/
      
      /*********
       * Size is 0, 
       * use this opportunity to set the offset
       * unless that has been a bad strategy for this 
       * instance already 
       ********/
      if (intBitSet.size() == 0 && useOffset) {
        // initialize bitSetOffset to the key, minus some amount to allow a bit of previous values
        intBitSet.setOffset(Math.max(0, key - 63));
        // because of offset, won't have to switch to hash for this key
        return;
      }
      
      final int offset = intBitSet.getOffset();
      
      if (key < offset) {
        adjustBitSetForLowerOffset(key);
        return;
      }
      
      // return if key fits in existing allocation
      if ((key - offset) < intBitSet.getSpaceUsed_in_bits()) {
        return;
      }

      // space used in words (32 bit words)
      final int bitSetSpaceNeeded = getBitSetSpaceFromMaxInt(key - offset);
      final int hashSetSpaceNeeded = getHashSetSpace(intBitSet.size());
      // switch if hashmap space plus hysteresis would be < bitmap space
      if (hashSetSpaceNeeded < (bitSetSpaceNeeded - HYSTERESIS)) {
        switchToHashSet(intBitSet.size());
      }
      return;    
    } 
    
    /********************
     *    Hash Set 
     ********************/
    // maybe switch to IntBitSet
    // space used in words (32 bit words)
    // don't bother to include the new element - it might not be "added"
    //   if it was already there, and only serves to decrease hysteresis a bit
    
    final int hashSetSpaceNeeded = getHashSetSpace(size());    
    
    final int maxInt = intHashSet.getMostPositive();
    final int offset = useOffset ? intHashSet.getMostNegative() : 0;
    final int bitSetSpaceNeeded = getBitSetSpaceFromMaxInt(maxInt - offset);
 
    if (bitSetSpaceNeeded < (hashSetSpaceNeeded - HYSTERESIS)) {
      switchToBitSet(maxInt, offset);
    }  
  }
    
  private void switchToHashSet(int size) {
    isBitSet = false;
    intHashSet = new IntHashSet(size);
    IntListIterator it = intBitSet.getIterator();
    while (it.hasNext()) {
      intHashSet.add(it.next());
    }
    intBitSet = null;
  }
  
  private void switchToBitSet(int maxInt, int offset) {
    isBitSet = true;
    intBitSet = new IntBitSet(maxInt - offset);
    intBitSet.setOffset(offset);
    IntListIterator it = intHashSet.getIterator();
    while (it.hasNext()) {
      intBitSet.add(it.next());
    }
    intHashSet = null;
  }
   
}
