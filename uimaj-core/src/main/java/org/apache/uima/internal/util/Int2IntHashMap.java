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
 * A map&lt;int, int&gt;
 * 
 * based on JCasHashMap, but without the multi-threading support
 * 
 * This impl is for use in a single thread case only
 * 
 * Supports shrinking (reallocating the big table)
 * 
 * Implements Map - like interface:
 *   keys and values are ints
 *   Entry set not (yet) impl
 *   
 * keys must be non-0; 0 is reserved to be an empty slot
 * values can be anything, but 0 is the value returned by get if not found so 
 *   values probably should not be 0
 *   
 */
public class Int2IntHashMap {

  private class KeyValueIterator implements IntKeyValueIterator {

    private int curPosition;

    private KeyValueIterator() {
      moveToFirst();
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#dec()
     */
    public void dec() {
      if (isValid()) {
        curPosition = moveToPreviousFilled(curPosition - 1);
      }
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#get()
     */
    public int get() {
      if (!isValid()) {
        throw new NoSuchElementException();
      }
      return keys[curPosition];
    }
    
    public int getValue() {
      if (!isValid()) {
        throw new NoSuchElementException();
      }
      return values[curPosition];      
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#inc()
     */
    public void inc() {
      if (isValid()) {
        curPosition = moveToNextFilled(curPosition + 1);
      }
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#isValid()
     */
    public boolean isValid() {
      return curPosition >= 0 && curPosition < keys.length;
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#moveToFirst()
     */
    public void moveToFirst() {
      curPosition = moveToNextFilled(0); 
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#moveToLast()
     */
    public void moveToLast() {
      curPosition = moveToPreviousFilled(keys.length - 1);
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#copy()
     */
    public Object copy() {
      KeyValueIterator it = new KeyValueIterator();
      it.curPosition = curPosition;
      return it;
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#moveTo(int)
     */
    public void moveTo(int i) {
      throw new UnsupportedOperationException();
    }

  }

  private class KeyIterator implements IntListIterator {

    private int curPosition;

    private KeyIterator() {
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
  

  
  // set to true to collect statistics for tuning
  // you have to also put a call to showHistogram() at the end of the run
  private static final boolean TUNE = false;
  
  private final static int[] EMPTY_INT_ARRAY = new int[0];

  static int nextHigherPowerOf2(int i) {
    return (i < 1) ? 1 : Integer.highestOneBit(i) << ( (Integer.bitCount(i) == 1 ? 0 : 1));
  }
 
  // this load factor gives, for array doubling strategy:
  //   between 1.5 * 8 bytes (2 words, one for key, one for value) = 12 and
  //           3   * 8                                               24 bytes per entry
  // This compares with 20 bytes/entry for the Int2IntRBT impl
  
  // This can be reduced to 12 to 18 bytes per entry at some complexity cost and performance
  // cost by implementing a 1 1/2 expansion scheme (not done)
  
  // With this tuning, the performance is approx 6 to 10 x faster than int2intRBT,
  // for various sizes from 100 to 100,000.
  
  // See corresponding Int2IntPerfTest which is disabled normally
 
  private final float loadFactor = (float)0.66;  
  
  private final int initialCapacity; 

  private int histogram [];
  private int maxProbe = 0;

  private int sizeWhichTriggersExpansion;
  private int size; // number of elements in the table  
 
  private int [] keys;
  private int [] values;
  
  private boolean secondTimeShrinkable = false;

  public Int2IntHashMap() {
    this(16);
  }
  
  public Int2IntHashMap(int initialCapacity) {
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
    values = new int[capacity];
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
    final int [] oldValues = values;
    final int oldCapacity = oldKeys.length;
    int newCapacity = 2 * oldCapacity;
    
    if (TUNE) {
      System.out.println("Capacity increasing from " + oldCapacity + " to " + newCapacity);
    }
    newTableKeepSize(newCapacity);
    int vi = 0;
    for (int key : oldKeys) {
      if (key != 0) {
        putInner(key, oldValues[vi]);
      }
      vi++;
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
          Arrays.fill(values,  0);
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
    Arrays.fill(values, 0);
    resetHistogram();
  }

  /** It gets a ref to the current value of table, and then searches that int array.
  * 
  * @param key -
  * @return the probeAddr in keys array - might have a 0 value, or the key value if found
  */
 private int find(final int key) {
   if (key == 0) {
     throw new IllegalArgumentException("0 is an invalid key");
   }
   
   final int hash = JCasHashMap.hashInt(key);

   final int[] localKeys = keys;
   final int bitMask = localKeys.length - 1;
   int probeAddr = hash & bitMask;
   
   // fast paths
   final int testKey = localKeys[probeAddr];
   if (testKey == 0 || testKey == key) {
     if (TUNE) {
       updateHistogram(1);
     }
     return probeAddr;
   }
   
   return find2(localKeys, key, probeAddr);
 }
 
 
 private int find2(final int[] localKeys, final int key, int probeAddr) { 
   final int bitMask = localKeys.length - 1;
   int nbrProbes = 2;   
   int probeDelta = 1;
   probeAddr = bitMask & (probeAddr + (probeDelta++));

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

 private void updateHistogram(int nbrProbes) {
   histogram[nbrProbes] = 1 + histogram[nbrProbes];
   if (maxProbe < nbrProbes) {
     maxProbe = nbrProbes;
   }
 }

 public int get(int key) {
   return (key == 0) ? 0 : values[find(key)];
 }
 
 public boolean containsKey(int key) {
   int probeAddr = find(key);
   return keys[probeAddr] != 0;
 }
 
 public boolean isKeyValid(int position) {
   return (position != 0) && (keys[position] != 0);
 }

 public int put(int key, int value) {
   final int i = find(key);
   final boolean keyNotFound = keys[i] == 0;
   final int prevValue = values[i];
   keys[i] = key;
   values[i] = value;
   if (keyNotFound) {
     incrementSize();
   }
   return prevValue;
 }
 
 public void putInner(int key, int value) {
   final int i = find(key);
   assert(keys[i] == 0);
   keys[i] = key;
   values[i] = value;
 }
  
 public int size() {
   return size;
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
  * @return updated pos
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
  
  public int[] getSortedKeys() {
    final int size = size();
    if (size == 0) {
      return EMPTY_INT_ARRAY;
    }
    final int[] r = new int[size];
    int i = 0;
    for (int k : keys) {
      if (k != 0) {
        r[i++] = k;
      }
    }
    assert(i == size());
    Arrays.sort(r);
    return r;
  }

  public IntListIterator keyIterator() {
    return new KeyIterator();
  }
  
  public IntListIterator keyIterator(int aKey) {
    throw new UnsupportedOperationException();// only makes sense for sorted things
  }

  public IntKeyValueIterator keyValueIterator() {
    return new KeyValueIterator();
  }

  public IntKeyValueIterator keyValueIterator(int aKey) {
    throw new UnsupportedOperationException();// only makes sense for sorted things
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
      
      System.out.println("bytes / entry = " + (float) (keys.length) * 8 / size());
      System.out.format("size = %,d, prevExpansionTriggerSize = %,d, next = %,d%n",
          size(),
          (int) ((keys.length >>> 1) * loadFactor),
          (int) (keys.length * loadFactor));
    }
  }

}
