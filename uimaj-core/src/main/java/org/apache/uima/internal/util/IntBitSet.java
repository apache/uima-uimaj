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
import java.util.Iterator;
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
 */
public class IntBitSet {
    
  private BitSet set;
  
  public IntBitSet() {
    this(16);
  }
  
  public IntBitSet(int initialCapacity) {
    set = new BitSet(initialCapacity);
  }
  
  public void clear() {
    set.clear();
  }
   
  public boolean contains(int key) {
    return (key == 0) ? false : set.get(key);
  }
 
  /**
   * 
   * @param key
   * @return true if this set did not already contain the specified element
   */
  public boolean add(int key) {
    final boolean prev = set.get(key);
    set.set(key);;
    return prev == false;
  }
  
  /**
   * 
   * @param key -
   * @return true if this key was removed, false if not present
   */
  public boolean remove(int key) {
    final boolean prev = set.get(key);
    set.clear(key);
    return prev;
  }

  public int size() {
    return set.cardinality();
  }
   
  /**
   * 
   * @return space used in bytes
   */
  public int getSpaceUsed() {
    return set.length() >> 5;
  }
  
  /**
   * 
   * @return largest int stored plus 1, or 0 if no ints in table
   */
  public int getLargestIntP1() {
    return set.length();
  }
  
  private class IntBitSetIterator implements IntListIterator {

    protected int curKey;

    protected IntBitSetIterator() {
      curKey = set.nextSetBit(0);
    }

    public final boolean hasNext() {
      return (curKey > 0 && curKey < set.length());
    }

    public final int next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      final int r = curKey;
      curKey = set.nextSetBit(curKey + 1);
      return r;
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

  public IntBitSetIterator getIterator() {
    return new IntBitSetIterator();
  }
}
