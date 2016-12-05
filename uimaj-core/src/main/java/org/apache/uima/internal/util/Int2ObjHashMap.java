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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.uima.util.impl.Constants;

/**
 * A map&lt;int, T&gt;
 * 
 * based on JCasHashMap, but without the multi-threading support
 * 
 * This impl is for use in a single thread case only
 * 
 * Supports shrinking (reallocating the big table)
 * 
 * Implements Map - like interface:
 *   keys are non-0 ints
 *     - 0 is reserved for the empty key slot
 *     - Integer.MIN_VALUE is reserved for removed slot 
 *   Entry set not (yet) impl
 *   
 * values can be anything, but null is the value returned by get if not found so 
 *   values probably should not be null
 *   
 * remove not currently supported
 *   
 */
public class Int2ObjHashMap<T> {

  private class KeyIterator implements IntListIterator {

    /**
     * Keep this always pointing to a non-0 entry, or
     * if not valid, outside the range
     */
    private int curPosition;
    
    private final int firstPosition;

    private KeyIterator() {
      this.curPosition = 0;
      moveToNextFilled();
      firstPosition = curPosition;
    }
    
    public final boolean hasNext() {
      return curPosition < keys.length;
    }

    public final int next() {
      
//      if (!hasNext()) {
//        throw new NoSuchElementException();
//      }
      try {
        final int r = keys[curPosition++];
        moveToNextFilled();
        return r;
      } catch (IndexOutOfBoundsException e) {
        throw new NoSuchElementException();
      }
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#hasPrevious()
     */
    public boolean hasPrevious() {
      return (curPosition > firstPosition);
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#previous()
     */
    public int previous() {
      if (!hasPrevious()) {
        throw new NoSuchElementException();
      }
      curPosition --;
      moveToPreviousFilled();
      return keys[curPosition];
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#moveToEnd()
     */
    public void moveToEnd() {
      curPosition = keys.length - 1;
      moveToPreviousFilled();
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#moveToStart()
     */
    public void moveToStart() {
      curPosition = 0;
      moveToNextFilled();
    }
    
    /**
     * advance pos until it points to a non 0 or is 1 past end
     * @param pos
     * @return updated pos
     */
    private void moveToNextFilled() {      
      final int max = keys.length;
      while (true) {
        if (curPosition >= max) {
          return;
        }
        if (keys[curPosition] != 0) {
          return;
        }
        curPosition ++;
      }
    }
     
    /**
     * decrement pos until it points to a non 0 or is -1
     * @param pos
     * @return updated pos
     */
    private void moveToPreviousFilled() {
      final int max = keys.length;
      if (curPosition > max) {
        curPosition = max - 1;
      }
      
      while (true) {
        if (curPosition < 0) {
          return;
        }
        if (keys[curPosition] != 0) {
          return;
        }
        curPosition --;
      }
    }
  }
  

  
  // set to true to collect statistics for tuning
  // you have to also put a call to showHistogram() at the end of the run
  private static final boolean TUNE = false;
   
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
  private T [] values;
  
  private boolean secondTimeShrinkable = false;
  
  final private Class<T> componentType;

  public Int2ObjHashMap(Class<T> clazz) {
    this(clazz, 16);
  }
  
  public Int2ObjHashMap(Class<T> clazz, int initialCapacity) {
    this.componentType = clazz;
    initialCapacity = Misc.nextHigherPowerOf2(initialCapacity);
    this.initialCapacity = initialCapacity;
    newTableKeepSize(initialCapacity);
    size = 0;
    if (TUNE) {
      histogram = new int[200];
      Arrays.fill(histogram, 0);
      maxProbe = 0;
    }
  }
  
  /** 
   * for use by copy
   * @param clazz
   * @param initialCapacity
   */
  private Int2ObjHashMap(Class<T> clazz, int initialCapacity,
    int sizeWhichTriggersExpansion, int size, int[] keys, T [] values) {
    this.componentType = clazz;
    this.initialCapacity = Misc.nextHigherPowerOf2(initialCapacity);
    this.sizeWhichTriggersExpansion = sizeWhichTriggersExpansion;
    this.histogram = null;
    this.size = size;
    this.keys = Arrays.copyOf(keys, keys.length);
    this.values = Arrays.copyOf(values, values.length);
  }
        
  private void newTableKeepSize(int capacity) {
    capacity = Math.max(16, Misc.nextHigherPowerOf2(capacity));
    keys = new int[capacity];
    values = (T[]) Array.newInstance(componentType, capacity);
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
    final T [] oldValues = values;
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
          Arrays.fill(values,  null);
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
    Arrays.fill(values, null);
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

    final int hash = Misc.hashInt(key);

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

  public T get(int key) {
    return (key == 0) ? null : values[find(key)];
  }

  public boolean containsKey(int key) {
    int probeAddr = find(key);
    return probeAddr != 0 && keys[probeAddr] != 0;
  }
 
  public boolean isKeyValid(int position) {
    return (position != 0) && (keys[position] != 0);
  }

  public T put(int key, T value) {
    final int i = find(key);
    final boolean keyNotFound = keys[i] == 0;
    final T prevValue = values[i];
    keys[i] = key;
    values[i] = value;
    if (keyNotFound) {
      incrementSize();
    }
    return prevValue;
  }

  public void putInner(int key, T value) {
    final int i = find(key);
    assert (keys[i] == 0);
    keys[i] = key;
    values[i] = value;
  }

  public int size() {
    return size;
  }
  
  public int[] getSortedKeys() {
    final int size = size();
    if (size == 0) {
      return Constants.EMPTY_INT_ARRAY;
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

  public IntKeyValueIterator keyValueIterator(int aKey) {
    throw new UnsupportedOperationException();// only makes sense for sorted things
  }
  
  public Iterator<T> values() {
    return new Iterator<T>() {
      
      /**
       * Keep this always pointing to a non-0 entry, or
       * if not valid, outside the range
       */
      private int curPosition = 0;
      { moveToNextFilled(); }  // non-static initializer
      

      @Override
      public boolean hasNext() {
        return curPosition < keys.length;
      }

      @Override
      public T next() {
        try {
          final T r = values[curPosition++];
          moveToNextFilled();
          return r;
        } catch (IndexOutOfBoundsException e) {
          throw new NoSuchElementException();
        }
      }
      
      private void moveToNextFilled() {      
        final int max = keys.length;
        while (true) {
          if (curPosition >= max) {
            return;
          }
          if (keys[curPosition] != 0) {
            return;
          }
          curPosition ++;
        }
      }
    };
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

  public Int2ObjHashMap<T> copy() {
    return new Int2ObjHashMap<>(componentType, initialCapacity, sizeWhichTriggersExpansion, size, keys, values);
  }

}
