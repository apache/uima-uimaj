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
 *     It switches when adding new members if the other representation is sufficiently smaller 
 */
public class PositiveIntSet {
  
  // number of words a inthashset has to be better than intbitset to cause switch
  private static final int HYSTERESIS = 16;
  
  // Extra space in bitset for initial capacity - can add an int up to that size 
  //   64 *4 is 8 words, = 256.
  private static final int BIT_SET_OVERALLOCATE = 64 * 4;
  
  // Extra space in hashset for initial capacity - can add a minimum of that many more members
  //   without hitting the hash-set doubling.
  private static final int HASH_SET_OVERALLOCATE = 8;
  // Extra space in hashset for initial capacity - when creating from bitmap set - multiplier of existing size
  private static final int HASH_SET_OVERALLOCATE_MULTIPLIER = 1; // bit shift right = divide by 2 = 50% of existing capacity
  
  private IntBitSet intBitSet = null;
  
  private IntHashSet intHashSet = null;
  
  //package private for test case
  boolean isBitSet = true;  
  
  /**
   * Set false once we find we have to reduce the bit offset
   */
  //package private for test case
  boolean useOffset = true;
  
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
    final int bitSetSpaceNeeded = getBitSetSpaceFromMaxInt(largestInt, 0);  // doesn't use offset
    final int hashSetSpaceNeeded = getHashSetSpace(intBitSet.size());
    
    if (hashSetSpaceNeeded < (bitSetSpaceNeeded - HYSTERESIS)) {
      switchToHashSet(size());
    } else {
      IntBitSet bs = new IntBitSet(largestInt + BIT_SET_OVERALLOCATE);
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
   * Only called if key won't fit in existing hashset allocation
   * 
   * Using existing bitset, compute what its size() (capacity) would be if the 
   * key were added; include the overhead of the intbitset class.
   * 
   * long words required = larger of double the current size, or the number of words required 

   * @param maxKeyLessOffset
   * @return number of words needed to store the highest value
   */
  private int getBitSetSpaceFromMaxInt(int maxKeyLessOffset, int spaceUsed) {
    int w64 = 1 + (maxKeyLessOffset >> 6); // 64 bits per long, add one because 0 to 63 takes one word)
    spaceUsed = spaceUsed >> 5;  // in # of long words, * 2 because the alloc doubles the space 
    
    int newSpace = Math.max(spaceUsed, w64) << 1; // size is in 32 bit words at the end  
    // e.g., 31 key => 1 word, 32 key => 2 words
    return newSpace + 2;  // 2 more for IntBitSet object overhead
  }
  
  /**
   * Only called if key won't fit in existing allocation
   * 
   * returns new hash table size ( usually double) + overhead 
   * 
   * @param numberOfEntries
   * @return
   */
  private int getHashSetSpace() {
    return intHashSet.getSpaceUsedInWords() * 2 + IntHashSet.getSpaceOverheadInWords();
  }
  
  /**
   * When converting from bitset to hash set, the initial hash set should be 
   * big enough so that it takes a while before it is doubled-expanded.
   * 
   * @param existingSize
   * @return
   */
  private int getHashSetOverAllocateSize(int existingSize) {
    return existingSize + (existingSize >> HASH_SET_OVERALLOCATE_MULTIPLIER) + HASH_SET_OVERALLOCATE;
  }
  
  /**
   * For the case where the intHashSet doesn't yet exist
   * @param numberOfElements -
   * @return the size in words
   */
  private int getHashSetSpace(int numberOfElements) {
    return IntHashSet.tableSpace(numberOfElements, IntHashSet.DEFAULT_LOAD_FACTOR) + 
           IntHashSet.getSpaceOverheadInWords();
  }
  
  /**
   * logic for switching representation
   * 
   * BitSet preferred - is faster, can be more compact, and permits ordered iteration
   * HashSet used when BitSet is too space inefficient.
   * 
   * Avoid switching if the new key being added won't increase the representation size
   *   - for hash: if number of elements +1 won't cause doubling of the hash table
   *   - for bitset: if key will fit in existing "capacity"
   *   
   * Compute space needed after adding
   *   - bitset:  array doubles
   *   - hashset: array doubles
   *
   * Preventing jitters back and forth:
   *   - removes don't cause switching
   *   - compare against other size including its non-linear space jumps.   
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
      if (intBitSet == null) {
        if (useOffset) {
      
          // initialize bitSetOffset to the key, minus some amount to allow some storage of previous values
          //   a minimum of 63, a maximum of 512 == 16 words, when key is > 4K (128 words) 
          final int spaceForLesserKeys = Math.min(1023,  Math.max(63, key >> 3));
          final int offset = Math.max(0, key - spaceForLesserKeys);
          intBitSet = new IntBitSet(BIT_SET_OVERALLOCATE + key - offset, offset);
          // because of offset, won't have to switch to hash for this key
          // 
          // force an initial allocation of this sufficient to keep
          //   a) the space below, plus
          //   b) 
          return;
        } else {
          intBitSet = new IntBitSet(BIT_SET_OVERALLOCATE + key);
          // don't return, see if we should immediately switch to hash representation
        }
      }
      
      final int offset = intBitSet.getOffset();
      
      if (key < offset) {
        adjustBitSetForLowerOffset(key);
        return;
      }
      
      // return if key fits in existing allocation
      final int spaceUsed = intBitSet.getSpaceUsed_in_bits();
      if ((key - offset) < spaceUsed) {
        return;
      }

      // space used in words (32 bit words)
      final int bitSetSpaceNeeded = getBitSetSpaceFromMaxInt(key - offset, spaceUsed);
      final int hashSetOverAllocateSize = getHashSetOverAllocateSize(intBitSet.size() + 1);
      final int hashSetOverAllocateSpace = getHashSetSpace(hashSetOverAllocateSize);
      // switch if hashmap space plus hysteresis would be < bitmap space
      if (hashSetOverAllocateSpace < (bitSetSpaceNeeded - HYSTERESIS)) {
        switchToHashSet(hashSetOverAllocateSize);
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
  
    if (intHashSet.wontExpand()) {
      return;
    }
    
    final int hashSetSpaceNeeded = getHashSetSpace();    
    
    final int maxInt = intHashSet.getMostPositive();
    final int offset = useOffset ? intHashSet.getMostNegative() : 0;
    final int bitSetSpaceNeeded = getBitSetSpaceFromMaxInt(BIT_SET_OVERALLOCATE + maxInt - offset, 0);
 
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
    intBitSet = new IntBitSet(BIT_SET_OVERALLOCATE + maxInt - offset, offset);
    IntListIterator it = intHashSet.getIterator();
    while (it.hasNext()) {
      intBitSet.add(it.next());
    }
    intHashSet = null;
  }
   
}
