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

import java.util.BitSet;
import java.util.NoSuchElementException;

/**
 * A set of non-zero positive ints.  
 *   
 * This is better (size) than IntHashSet unless
 *   the expected max int to be contained is &gt; size of the set * 100
 *   or you need to store negative ints
 *   
 * This impl is for use in a single thread case only
 * 
 * This impl supports offset, to let this bit set store items in a range n &rarr; n + small number
 * 
 * If using offset, you must add ints in a range equal to or above the offset
 * 
 */
public class IntBitSet implements PositiveIntSet {
    
  private final BitSet set;
  
  private int size = 0;  // tracked here to avoid slow counting of cardinality
  
  private final int offset;
  
//  /**
//   * Sets the lowest int value that can be kept; trying to add a lower one throws an error
//   * <ul>
//   *   <li>Allows for a more compact representation, potentially</li>
//   *   <li>only can be set if the size is 0</li>
//   * </ul>
//   * @param offset - the lowest value that can be kept in the set. 0 is allowed.
//   */
//  public void setOffset(int offset) {
//    if (size != 0) {
//      throw new IllegalStateException("Cannot set offset unless set is empty");
//    }
//    if (offset < 0) {
//      throw new IllegalArgumentException("Offset must be 0 or a positive int, was " + offset);
//    }
//    this.offset = offset;
//  }
  /**
   * @return the current offset
   */
  public int getOffset() {
    return offset;
  }
  
  /**
   * Construct an IntBitSet capable of holding ints from 0 to 63, (perhaps plus an offset)
   */
  public IntBitSet() {
    this(63);  // in current java impls, this is the minimum 1 long int is allocated  
  }
    
  /**
   * Construct an IntBitSet capable of holding ints from 0 to maxInt (perhaps plus an offset)
   * @param maxInt the biggest int (perhaps plus an offset) that can be held without growing the space
   */
  public IntBitSet(int maxInt) {
    this(maxInt, 0);
  }

  public IntBitSet(int maxAdjKey, int offset) {
    set = new BitSet(Math.max(1, maxAdjKey));
    this.offset = offset;
  }
  
  /**
   * empty the IntBitSet.
   * keeps the offset
   */
  @Override
  public void clear() {
    set.clear();
    size = 0;
  }
   
  /**
   * 
   * @param key - the integer (not adjusted for offset)
   * @return -
   */
  @Override
  public boolean contains(int key) {
    return (key == 0) ? false : 
           (key - offset < 0) ? false :
           set.get(key - offset);
  }
 

  @Override
  public int find(int element) {
    return contains(element) ? element - offset : -1;
  }
  /**
   * 
   * @param original_key - the int to add to the set
   * @return true if this set did not already contain the specified element
   */
  @Override
  public boolean add(int original_key) {
    if (original_key < offset) {
      throw new IllegalArgumentException("key " + original_key + " must be greater than or equal to the offset: " + offset);
    }
    
    final int adjKey = original_key - offset;
    final boolean prev = set.get(adjKey);
    set.set(adjKey);
    if (!prev) {
      size ++;
      return true;
    }
    return false;
  }
  
  /**
   * 
   * @param original_key -
   * @return true if this key was removed, false if not present
   */
  @Override
  public boolean remove(int original_key) {
    final int adjKey = original_key - offset;
    if (adjKey < 0) {
      return false;
    }
    final boolean prev = set.get(adjKey);
    if (prev) {
      set.clear(adjKey);  // avoid clearing which may expand bit set, if not present
      size --;
      return true;
    }
    return false;
  }

  /**
   * 
   * @return the number of elements in this set
   */
  @Override
  public int size() {
    return size;    // bit set cardinality() is slow
  }
  
  public int getSpaceUsed_in_bits_no_overhead() {
    return set.size();
  }
   
  /**
   * 
   * @return space used in 32 bit words
   */
  public int getSpaceUsed_in_words_no_overhead() {
    return getSpaceUsed_in_bits_no_overhead() >> 5;  // divide by 32
  }
  
  /**
   * 
   * @return largest int in the set
   * If the set has no members, 0 is returned
   */
  public int getLargestMenber() {
    return set.length() - 1 + offset;
  }
  
  @Override
  public int get(int position) {
    assert(set.get(position));
    return position + offset;
  }
  
  private class IntBitSetIterator implements IntListIterator {

    /**
     * This is the bit set position which is -1 (if invalid) or
     *   the position in the bit set of the key - offset
     *   
     *   If the offset is not 0, then the position != key
     */
    protected int curKey = set.nextSetBit(0);

    protected IntBitSetIterator() {}

    public final boolean hasNext() {
      return (curKey >= 0);
    }

    public final int next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      final int r = curKey;
      curKey = set.nextSetBit(curKey + 1);
      return r + offset;
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#hasPrevious()
     */
    public boolean hasPrevious() {
      throw new UnsupportedOperationException();
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#previous()
     */
    public int previous() {
      throw new UnsupportedOperationException();
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#moveToEnd()
     */
    public void moveToEnd() {
      throw new UnsupportedOperationException();
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#moveToStart()
     */
    public void moveToStart() {
      curKey = set.nextSetBit(0);
    }

  }

  @Override
  public IntBitSetIterator iterator() {
    return new IntBitSetIterator();
  }

  @Override
  public int moveToFirst() {
    return set.nextSetBit(0);
  }

  @Override
  public int moveToLast() {
    return set.length() - 1;
  }

  @Override
  public int moveToNext(int position) {
    return (position < 0) ? position : set.nextSetBit(position + 1);
  }

  @Override
  public int moveToPrevious(int position) {
    return (position < 0) ? position : set.previousSetBit(position - 1);  
  }

  /**
   * This impl depends on position always pointing to a valid (== non 0) 
   * element of the set, when it should be valid 
   */
  @Override
  public boolean isValid(int position) {
    return (position >= 0) && set.get(position);
  }

  /**
   * Add all elements of this bit set to the passed in IntVector
   */
  @Override
  public void bulkAddTo(IntVector v) {
    int pos = -1;
    while (-1 != (pos = set.nextSetBit(pos + 1))) {
      v.add(pos + offset);
    }
  }

  @Override
  public int[] toIntArray() {
    final int s = size();
    if (s == 0) {
      return PositiveIntSet_impl.EMPTY_INT_ARRAY;
    }
    final int[] r = new int[s];
    int pos = moveToFirst();
    for (int i = 0; i < s; i++) {
      r[i] = get(pos);
      pos = set.nextSetBit(pos + 1);
    }
    return r;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return String.format("IntBitSet [set=%s, size=%s, offset=%s]", set, size, offset);
  }

}
