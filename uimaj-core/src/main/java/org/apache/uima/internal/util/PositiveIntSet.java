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
 *   Uses two different representations depending on the ratio of the max int stored and the total number of ints stored.
 * 
 */
public class PositiveIntSet {
  
  // number of words a inthashset has to be better than intbitset to cause switch
  private static final int HYSTERESIS = 4;

  IntBitSet intBitSet;
  
  IntHashSet intHashSet;
  
  boolean isBitSet;  
  
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
      allValues[i] = it.next();
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
   * @param key
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
  
  public void add(int[] vs) {
    for (int i : vs) {
      add(i);
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
   *    with hysteresis                              1 2 4 8 16 32 64 128 256 512 1024 2048 4096 8192 16k 32k 64k 128k 256k 512k 1m 2m 4m 8m 16m
   *       Don't switch unless space saving is         1 2 3  4  5  6  7   8   9   10   11   12   13   14  15  16  17   18   19  20 21 22 23 24
   *           intHashSet
   *          size    space         savings
   *            1       12         32768   
   *            1024   12288       32768     
   *            16384  196608      100000    1/2 of space
   *            
   *          max(32768, 1/2 of hash-map space). 
   *
   *   switch to BitSet from HashSet if:
   *      BitSet used when its space requirement is less than hashset, with hysteresis:
   *       Don't switch  unless space saving is max(32768, 1/4 of hash-map space)
   *           intHashSet
   *          size    space         savings
   *            1       12         no switch   
   *            1024   12288       no switch     
   *            16384  196608      100000    1/2 of space
   *           
   */
  
  private void maybeSwitchRepresentation(int key) {
    
    if (isBitSet) {
      /********************
       *    Bit Set 
       ********************/
      final int maxKeyP1 = intBitSet.getLargestIntP1(); // p1 = plus 1
      if (key < maxKeyP1) {
        return;
      }
      // space used in words (32 bit words)
      int bitSetSpace = 1 + (Math.max((maxKeyP1 - 1), key) >> 5);  // space in words
      // keep bit set unless key would grow it beyond 
//      if (bitSetSpace < 16 || key < bitSetSpace) {
//        return;
//      }
      
      // maybe switch to IntHashSet
      final int size = size();
      // space used in words (32 bit words)
      long hashsetSpace = (size << 1) + size;  // size * 3 , 1.5 - 3 words per entry in IntHashSet
      // switch if hashmap space plus hysteresis would be < bitmap space
      if (hashsetSpace < (bitSetSpace - HYSTERESIS)) {
        switchToHashSet(size);
      }
      return;    
    } 
    
    /********************
     *    Hash Set 
     ********************/
    // maybe switch to IntBitSet
    final int size = size();
    // space used in words (32 bit words)
    long hashsetSpace = (size << 1) + size;  // size * 3 , 1.5 - 3 words per entry in IntHashSet
    
    final int maxInt = intHashSet.getMostPositive();
    final int bitSetSpace = bitSetSpace(maxInt);
 
    if (bitSetSpace < (hashsetSpace - HYSTERESIS)) {
      switchToBitSet(maxInt);
    }  
  }
  
  private int bitSetSpace(int maxInt) {
    // 0 - 31 : 1;  32-63: 2
    return 1 + (maxInt >> 5);
  }
  
  private int hashSetSpace(int size) {
    return (size << 1) + size;  // size * 3
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
  
  private void switchToBitSet(int maxInt) {
    isBitSet = true;
    intBitSet = new IntBitSet(maxInt);
    IntListIterator it = intHashSet.getIterator();
    while (it.hasNext()) {
      intBitSet.add(it.next());
    }
  }
   
}
