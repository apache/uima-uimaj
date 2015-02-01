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
 * An set of non-zero integers, ability to iterate over them (possibly in a sorted way),
 * with O(1) operations for adding, removing, and testing for contains.
 * 
 * Optimized for adding mostly increasing integers (with some space for less-than-first-one-added).
 *   - major uses: indexes of Feature Structures.
 * Optimize for small sets 
 * 
 * Graceful degradation for completely random integer operations on large sets; 
 *   keep O(1) operations, at the expense of extra space (&lt; 3 x size).
 * 
 *   Uses several different representations depending on the range of ints stored and the total number of ints stored.
 *   
 *     Sizes:  Tiny, medium, large 
 *     Ranges: semi-knowable, unknowable;  
 *               if semi-knowable, dense, small-range (&lt; 65K), large-range
 *     
 *     For all sizes, 
 *       if dense, use IntBitSet (with offset)
 *       
 *     else 
 *       For large, (implies large-range, too) use IntHashSet  
 *       
 *       For medium,
 *         if small-range &lt; 65K, use IntHashSet with offset 
 *         else use IntHashSet 
 *   
 *   Arrange switching between representations
 *     to occur infrequently, especially as cost of switching (size) grows
 *   Arrange checking for switching to occur infrequently, taking into account how underlying data structures grow
 *     (which is often by doubling)
 *   Switch when adding new member(s) if alternative representation is sufficiently smaller
 *   
 */
public class PositiveIntSet_impl implements PositiveIntSet {
  
  // number of words a inthashset has to be better than intbitset to cause switch
  private static final int HYSTERESIS = 16;
  
  private static final int INT_SET_MAX_SIZE = 16;
  
  private static final int HASH_SET_SHORT_MAX_SIZE = (1 << 16) - 1; // 65535 
  
  // Extra space in bitset for expansion - can add an int (- offset) up to that size
  // Initial space = 2 words = 1 long.
  //   64 *4 is 8 words, = 256.
  private static final int BIT_SET_OVERALLOCATE = 64 * 4; // 8 words minimum
  
  // Extra space in hashset for initial capacity - when creating from bitmap set - multiplier of existing size
  private static final int HASH_SET_OVERALLOCATE_DIVIDER_SHIFT = 1; // bit shift right = divide by 2 = 50% of existing capacity
  
  public static final int[] EMPTY_INT_ARRAY = new int[0];

  private PositiveIntSet intSet;  // one of 3 representations: IntBitSet, IntHashSet, IntSet (based on IntVector)
  
  //package private for test case
  boolean isBitSet = false;
  boolean isIntSet = false;  
  boolean isHashSet = false;
    
  boolean secondTimeShrinkable = false;
  
  /**
   * Set false once we find we have to reduce the bit offset
   */
  //package private for test case
  boolean useOffset = true;
  
  public PositiveIntSet_impl() {
    this(0, 0, 0);
  }
  
  /**
   * Set up a Positive Bit Set
   * @param initialSize - if 0, don't allocate yet, wait for first add.
   *           If isBitSetDense, then this number is interpreted as  
   *             the first int to be added, typically with an offset.  
   *           The next two params are used only if initialSize is not 0.
   * @param estMin - the estimated minimum int value to be added
   * @param estMax - the estimated max int value to be added
   */
  public PositiveIntSet_impl(int initialSize, int estMin, int estMax) {
    if (initialSize == 0) {
      isBitSet = true;  // try first as bit set with offset perhaps
      return;  // delay allocation until we know what the 1st int will be
    }
    
    if ((initialSize < 0) || 
        (estMin < 0)      || 
        (estMax < estMin)) {
      throw new IllegalArgumentException();
    }

    estMax = Math.max(initialSize, estMax); // because its a positive int set

    
    // offsets are different for bit set and hash set
    //   for bit set, it's set to the estMin - capped 12.5% from the estMin (cap is 16 words)
    //   for hash set, if the size > 65K, it's ignored
    //                 it's set to 3/4 up the "range" of est max - est min
    
    int offsetBitSet = estimatedBitSetOffset(estMin, -1);

    int offsetHashSet = getHashSetOffset(estMax, estMin);
    
    int bitSetWordsNeeded = getBitSetSpaceFromRange(estMax - offsetBitSet, 0);
    int hashSetWordsNeeded = getHashSetSpace(initialSize, estMax, estMin);

    // no HYSTERESIS in test below - pick absolute smaller for initial alloc
    if (bitSetWordsNeeded < hashSetWordsNeeded) {
      allocateIntBitSet(estMax, estMin, offsetBitSet);
      isBitSet = true;
      return;
    }
    
    // choose between IntHashSet and IntSet based on size
    if (initialSize <= INT_SET_MAX_SIZE) {
      intSet = new IntSet(initialSize);
      isIntSet = true;
      return;
    }

    intSet = new IntHashSet(initialSize, offsetHashSet);
    isHashSet = true;
    
  }
  
  @Override
  public IntListIterator iterator() {
    return getUnorderedIterator();
  }
  
  public IntListIterator getUnorderedIterator() {
    if (null == intSet) {
      intSet = new IntSet(0);
    }
    return intSet.iterator();
  }
  
  public IntListIterator getOrderedIterator() {
    if (isBitSet) {
      if (null == intSet) {
        return new IntListIteratorOverArray(EMPTY_INT_ARRAY);
      }
      return intSet.iterator();
    }
    
    int[] allValues = new int[size()];
    IntListIterator it = intSet.iterator();
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
  
  /* (non-Javadoc)
   * @see org.apache.uima.internal.util.PositiveIntSetI#clear()
   */
  @Override
  public void clear() {
    if (null != intSet) {
      if (isBitSet) {
        if (secondTimeShrinkable) {
          secondTimeShrinkable = false;
          IntBitSet ib = (IntBitSet) intSet;
          int max = ib.getLargestMenber();
          int offset = ib.getOffset();
          intSet = new IntBitSet((max - offset) >> 1, offset);
        } else {
          intSet.clear();
          secondTimeShrinkable = true;
        }
      } else {
        intSet.clear();
      }
    }
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.internal.util.PositiveIntSetI#contains(int)
   */
  @Override
  public boolean contains(int key) {
    return (null != intSet) ? intSet.contains(key) : false;
  }
 
  @Override
  public int find(int element) {
    return (null != intSet) ? intSet.find(element) : -1;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.internal.util.PositiveIntSetI#add(int)
   */
  @Override
  public boolean add(int key) {
    maybeSwitchRepresentation(key);
    return intSet.add(key);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.internal.util.PositiveIntSetI#remove(int)
   */
  @Override
  public boolean remove(int key) {
    return (null != intSet) ? intSet.remove(key) : false;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.internal.util.PositiveIntSetI#size()
   */
  @Override
  public int size() {
    return (null != intSet) ? intSet.size() : 0;
  }
  
//  public void add(int[] vs) {
//    for (int i : vs) {
//      add(i);
//    }
//  }
  
  
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
    final IntBitSet intBitSet = (IntBitSet) intSet; 
    final int size = intBitSet.size() + 1;
    final int largestInt = intBitSet.getLargestMenber();  // not called if key > largest member
    final int bitSetSpaceNeeded = getBitSetSpaceFromRange(largestInt, 0);  // doesn't use offset
    final int hashSetSpaceNeeded = getHashSetSpace(size, largestInt, key); // computes using load factor,
    useOffset = false;  // stop using because we have evidence it isn't appropriate
    
    if (hashSetSpaceNeeded < (bitSetSpaceNeeded - HYSTERESIS)) {
      int offset = getHashSetOffset(largestInt, key);
      switchFromBitSet(size, offset);
      
      // keep as bit set - it's smaller, but drop the offset
    } else {
      IntListIterator it = intSet.iterator();
      allocateIntBitSet(largestInt, key, 0);
      while (it.hasNext()) {
        intSet.add(it.next());
      }
    }
  }
  
  private static int getHashSetOffset(int estMax, int estMin) {
    int range = estMax - estMin;
    assert (range >= 0);
    return  (range > HASH_SET_SHORT_MAX_SIZE) ?
        Integer.MIN_VALUE :  // signal to force use of 4 byte ints
        // make the offset such that the 0 point is 3/4 of the way
        // toward the top of the range, because there's more likelyhood
        // of expansion in that direction
        estMax - (range >> 2);   
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
   * Only called if key won't fit in existing IntHashSet, IntSet or IntBitSet allocation
   * 
   * If IntBitSet (spaceUsed != 0) compute what its size() (capacity) would be if the 
   * key were added (causing doubling); include the overhead of the intbitset class.
   * 
   * long words required = larger of double the current size, or the number of long words required 

   * @param adjKey - the range of bits needed
   * @param spaceUsed - the number of bits currently allocated (if currently a bit set), else 0
   * @return number of words needed to store the range, 
   */
  private static int getBitSetSpaceFromRange(int adjKey, int spaceUsed) {
    int w64 = 1 + (adjKey >> 6); // 64 bits per long, add one because 0 to 63 takes one word)
    spaceUsed = spaceUsed >> (6 - 1);  // in # of long words, * 2 because the alloc doubles the space 
    
    int newSpace = Math.max(spaceUsed, w64) << 1; // <<1 to convert to # of words from # of longs  
    // e.g., 31 key => 1 word, 32 key => 2 words
    return newSpace + 2;  // 2 more for IntBitSet object overhead
  }
  
  /**
   * 
   * @param size including new element to be added
   * @return number of words including overhead to store that many elements
   *    unless is bigger than INT_SET_MAX_SIZE, then return MAX_VALUE
   */
  private static int getIntSetSpace(int size) {
    return (size < INT_SET_MAX_SIZE) ? size + 4 : Integer.MAX_VALUE;
  }
  
  /**
   * Only called if key won't fit in existing allocation
   * 
   * returns new hash table size ( usually double) + overhead 
   * 
   * @param numberOfEntries
   * @return new hash table size ( usually double) + overhead
   */
  private int getHashSetSpace() {
    return ((IntHashSet)intSet).getSpaceUsedInWords() * 2 + IntHashSet.getSpaceOverheadInWords();
  }
  
  /**
   * For the case where the intHashSet doesn't yet exist
   * @param numberOfElements -
   * @param estMax - the largest int to be stored
   * @param estMin - the smallest int to be stored
   * @return the size in words
   */
  private static int getHashSetSpace(int numberOfElements, int estMax, int estMin) {
    boolean isShort;
    if (numberOfElements > HASH_SET_SHORT_MAX_SIZE) {
      isShort = false;
    } else {
      isShort = (estMax - estMin) < HASH_SET_SHORT_MAX_SIZE;
    }
    int numberOfTableElements =  IntHashSet.tableSpace(numberOfElements, IntHashSet.DEFAULT_LOAD_FACTOR);
    return (numberOfTableElements >> ((isShort) ? 1 : 0)) 
        + IntHashSet.getSpaceOverheadInWords();
  }

  /**
   * When converting from bitset to hash set, the initial hash set should be 
   * big enough so that it takes a while before it is doubled-expanded.
   * 
   * Reduce overallocation for small sizes because the cost to switch is low, and the % overallocate
   *   could be a large.
   * 
   * @param existingSize
   * @return overallocate size
   */
  private static int getHashSetOverAllocateSize(int existingSize) {
    return existingSize + (existingSize >> HASH_SET_OVERALLOCATE_DIVIDER_SHIFT);
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

  private void maybeSwitchRepresentation(int newKey) {
    if (isBitSet) {
      handleBitSet(newKey);
      return;
    } 
    if (isIntSet) {
      handleIntSet(newKey);
      return;
    }
    handleHashSet(newKey);
  }
  
  
  /********************
   *    Bit Set 
   ********************/
  private void handleBitSet(int newKey) {
    IntBitSet intBitSet = (IntBitSet) intSet;

    if (intBitSet == null) {
      /*********
       * Size is 0, 
       * use this opportunity to set the offset
       * unless that has been a bad strategy for this 
       * instance already 
       ********/
      boolean guaranteedFits = allocateIntBitSet(newKey, newKey, -1);
      intBitSet = (IntBitSet) intSet;
      if (guaranteedFits) {
        return;
      }
    }
    
    final int offset = intBitSet.getOffset();
    
    if (newKey < offset) {
      // may change representation; always abandons this bit set
      adjustBitSetForLowerOffset(newKey);
      return;
    }
    
    // return if newKey fits in existing allocation
    final int spaceUsed = intBitSet.getSpaceUsed_in_bits_no_overhead();
    final int adjKey = newKey - offset;
    if (adjKey < spaceUsed) {
      return;
    }
    
    
    // this newKey doesn't fit in existing bit set allocation; it's too large
    //   figure out if we should expand this bit set or switch representations

    final int bitSetSpaceNeeded = getBitSetSpaceFromRange(adjKey, spaceUsed);  // in 32 bit words
    final int sizeNeeded = intBitSet.size() + 1;
    
    if (getIntSetSpace(sizeNeeded) < bitSetSpaceNeeded) {
      switchToIntSet(sizeNeeded);
      return;
    }
    
    // when computing hashset size, overallocate by 50% to avoid immediately doubling
    final int hashSetOverAllocateSize = getHashSetOverAllocateSize(sizeNeeded);
    final int hashSetOverAllocateSpace = getHashSetSpace(hashSetOverAllocateSize, newKey, offset);
    // switch if hashmap space plus hysteresis would be < bitmap space
    if (hashSetOverAllocateSpace < (bitSetSpaceNeeded - HYSTERESIS)) {
      switchToHashSet(hashSetOverAllocateSize, getHashSetOffset(newKey, offset));
    }
    return;    
  }
  
  /********************
   *     IntSet       * 
   ********************/
  private void handleIntSet(int newKey) {
    IntSet is = (IntSet) intSet;
    final int size = is.size() + 1;
    if (size <= INT_SET_MAX_SIZE) {
      return;
    }
    
  
    // switch to bit or IntHash
    
    // first, compute max and min values
    int mostPos = Integer.MIN_VALUE;
    int mostNeg = Integer.MAX_VALUE;
    for (int i = 0; i < size - 1; i++) {
      final int v = is.get(i);
      if (v > mostPos) { mostPos = v;}
      if (v < mostNeg) { mostNeg = v;}
    }
    // include the newKey in the computation of these
    if (newKey > mostPos) { mostPos = newKey;}
    if (newKey < mostNeg) { mostNeg = newKey;}
    
    // make a bit set or hash set.
    //   have 16 members, so the hash set is approx 32 + 11 words = 43.
    //   bit set is approx 2 + range / 32
    final int bitSetSpace = getBitSetSpaceFromRange(mostPos - mostNeg, 0);
    final int hashSetOverAllocSize = getHashSetOverAllocateSize(size);
     final int hashSetSpace = getHashSetSpace(hashSetOverAllocSize, mostPos, mostNeg);
    if (bitSetSpace < hashSetSpace) {
      switchToBitSet(mostPos, mostNeg, estimatedBitSetOffset(mostNeg, -1));
      return;
    }
    switchToHashSet(hashSetOverAllocSize, getHashSetOffset(mostPos, mostNeg));
  }
  
  /********************
   *    Hash Set 
   ********************/
  private void handleHashSet(int newKey) {
    // maybe switch to IntBitSet
    // space used in words (32 bit words)
    // don't bother to include the new element - it might not be "added"
    //   if it was already there, and only serves to decrease hysteresis a bit
    final IntHashSet intHashSet = (IntHashSet) intSet;
    if (intHashSet.wontExpand()) {
      return;
    }
    
    final int hashSetSpaceNeeded = getHashSetSpace(); //computes the doubling of space   
    
    final int maxInt = intHashSet.getMostPositive();
    // https://issues.apache.org/jira/browse/UIMA-4159
    final int offset = useOffset ? Math.min(intHashSet.getMostNegative(), newKey) : 0;
    final int bitSetSpaceNeeded = getBitSetSpaceFromRange(BIT_SET_OVERALLOCATE + maxInt - offset, 0);
 
    if (bitSetSpaceNeeded < (hashSetSpaceNeeded - HYSTERESIS)) {
      switchToBitSet(maxInt, intHashSet.getMostNegative(), offset);
    }  
  }
  
  /**
   * Allocate a new IntBitSet 
   * @param estMax the largest int to store in the bit set - determines the initial size
   * @param estMin this is a lower bound on the value that can be in the bit set
   * @param offsetSpec this is the offset to use, or -1, to calculate the offset from
   *                   the initial_size (assuming a small amount of adds will be for
   *                   ints somewhat less than the initial_size entry (being treated
   *                   as the first int to be added)
   * @return true if allocated with offset, implying guarantee the estMax key fits.
   */
  private boolean allocateIntBitSet(int estMax, int estMin, int offsetSpec) {
    isBitSet = true;
    isHashSet = isIntSet = false;
    if (useOffset) {
      
      // initialize bitSetOffset to the key, minus some amount to allow some storage of previous values
      //   a minimum of 63, a maximum of 512 == 16 words, when key is > 4K (128 words) 
      final int offset = estimatedBitSetOffset(estMin, offsetSpec);
      intSet = new IntBitSet(BIT_SET_OVERALLOCATE + estMax - offset, offset);
      // because of offset, won't have to switch to hash for this key
      return true;
    } else {
      intSet = new IntBitSet(BIT_SET_OVERALLOCATE + estMax);
      return false;
    }
  }
  
  private static int estimatedBitSetOffset(int estMin, int offsetSpec) {
    return   // initialize bitSetOffset to the key, minus some amount to allow some storage of previous values
             //   a minimum of 63, a maximum of 512 == 16 words, when key is > 4K (128 words) 
       (offsetSpec == -1) ? 
            Math.max(0, estMin - Math.min(1023,  Math.max(63, estMin >> 3))) :
            offsetSpec;
  }
  
  /**
   * switching from bit set to either IntSet or IntHashSet
   * @param size - space needed including new item
   * @param offset - for IntHashSet values kept as short, the offset subtracted from values before storing them
   *                 offset == Integer.MIN_VALUE means use 4 byte ints              
   */
  private void switchFromBitSet(int size, int offset) {
    if (size < INT_SET_MAX_SIZE) {
      switchToIntSet(size);
      return;
    }
    switchToHashSet(size, offset);
  }
  
  /**
   * switch to IntSet
   * @param size number of elements to be able to store without expanding
   */
  private void switchToIntSet(int size) {
    IntListIterator it = intSet.iterator();

    intSet = new IntSet(size);
    isIntSet = true;
    isBitSet = isHashSet = false;
    
    while (it.hasNext()) {
      intSet.add(it.next());
    }
    return;
  }
  
  /**
   * Switch from any intSet impl to Hash Set
   * @param size - the capacity - this many elements could be stored before swtiching
   * @param offset - used only when the size < 65K, and is a value subtracted from elements before
   *                 storing them, in an attempt to fit the results into just "short"s instead of "int"s
   *                 if == MIN_VALUE, then force 4 byte ints
   */
  private void switchToHashSet(int size, int offset) {
    IntListIterator it = intSet.iterator();

    intSet = new IntHashSet(size, offset);
    isIntSet = isBitSet = false;
    isHashSet = true;
    
    while (it.hasNext()) {
      intSet.add(it.next());
    }
  }
  
  private void switchToBitSet(int estMax, int estMin, int offset) {
    final IntListIterator it = intSet.iterator();
    
    allocateIntBitSet(estMax, estMin, offset);
    
    while (it.hasNext()) {
      intSet.add(it.next());
    }
  }

  @Override
  public int get(int position) {
    if (null == intSet) {
      throw new NoSuchElementException();
    }
    return intSet.get(position);
  }

  @Override
  public int moveToFirst() {
    return (null == intSet) ? -1 : intSet.moveToFirst();
  }

  @Override
  public int moveToLast() {
    return (null == intSet) ? -1 : intSet.moveToLast();
  }

  @Override
  public int moveToNext(int position) {
    return (null == intSet) ? -1 : intSet.moveToNext(position);
  }

  @Override
  public int moveToPrevious(int position) {
    return (null == intSet) ? -1 : intSet.moveToPrevious(position);
  }

  @Override
  public boolean isValid(int position) {
    return (null == intSet) ? false : intSet.isValid(position);
  }

  @Override
  public void bulkAddTo(IntVector v) {
    if (null != intSet) {
      intSet.bulkAddTo(v);
    }
  }

  @Override
  public int[] toIntArray() {
    if (null != intSet) {
      return intSet.toIntArray();
    }
    return EMPTY_INT_ARRAY;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return String.format("PositiveIntSet_impl [%n  intSet=%s%n secondTimeShrinkable=%s, useOffset=%s]",
        intSet, secondTimeShrinkable, useOffset);
  }
  
  // for testing
  boolean isOffsetBitSet() {
    return (isBitSet && ((IntBitSet)intSet).getOffset() != 0);
  }

  // for testing
  boolean isShortHashSet() {
    return (isHashSet && ((IntHashSet)intSet).isShortHashSet());
  }
  
}
