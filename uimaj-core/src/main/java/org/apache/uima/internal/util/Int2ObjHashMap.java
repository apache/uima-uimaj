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
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

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
 *   
 * values can be anything, but null is the value returned by get if not found so 
 *   values probably should not be null
 *   
 * remove supported by replacing the value slot with null, and replacing the key slot with a "removed" token.
 * A cleanout of removed items occurs when necessary.
 *   
 */
public class Int2ObjHashMap<T> extends Common_hash_support implements Iterable<IntEntry<T>>{
  
  private static final int REMOVED_KEY = Integer.MIN_VALUE;

  private class KeyIterator extends CommonKeyIterator implements IntListIterator {   
    
    @Override
    public final int nextNvc()  {   
      final int r = keys[curPosition];
      curPosition = moveToNextFilled(curPosition + 1);
      return r;     
    }
    
    @Override
    public int previousNvc() {
      curPosition = moveToPreviousFilled(curPosition - 1);
      return keys[curPosition];      
    }

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
 
//  private final float loadFactor = (float)0.66;  
//  
//  private final int initialCapacity; 
//
//  private int histogram [];
//  private int maxProbe = 0;
//
//  private int sizeWhichTriggersExpansion;
//  private int size; // number of elements in the table  
 
  private int [] keys;
  private T [] values;
  
//  private boolean secondTimeShrinkable = false;
  
  final private Class<T> componentType;  // needed to make new instances of the value array
  
//  /** set to the first found_removed when searching */
//  private int found_removed;
  
//  private int removed = 0;  // for rebalancing  
  
  public Int2ObjHashMap(Class<T> clazz) {
    this(clazz, MIN_SIZE);
  }
  
  public Int2ObjHashMap(Class<T> clazz, int initialSizeBeforeExpanding) {
    super(initialSizeBeforeExpanding);
    this.componentType = clazz;
    newTable(this.initialCapacity);
  }
  
  /** 
   * for use by copy
   * @param clazz
   * @param initialCapacity
   */
  private Int2ObjHashMap(Int2ObjHashMap orig) {
    super(orig);
    this.componentType = orig.componentType;
    this.keys = Arrays.copyOf(orig.keys, keys.length);
    this.values = (T[]) Arrays.copyOf(orig.values, values.length);
  }
        
//  private void newTableKeepSize(int capacity) {
//    capacity = Math.max(MIN_SIZE, Misc.nextHigherPowerOf2(capacity));
//    keys = new int[capacity];
//    values = (T[]) Array.newInstance(componentType, capacity);
//    sizeWhichTriggersExpansion = (int)(capacity * loadFactor);
//  }

//  protected void incrementSize() {
//    if (size + removed >= sizeWhichTriggersExpansion) {
//      increaseTableCapacity();
//    }
//    size++;
//  }

//  private void increaseTableCapacity() {
//    final int [] oldKeys = keys;
//    final T [] oldValues = values;
//    final int oldCapacity = oldKeys.length;
//    int newCapacity = 2 * oldCapacity;
//    
//    if (TUNE) {
//      System.out.println("Capacity increasing from " + oldCapacity + " to " + newCapacity);
//    }
//    removed = 0;
//    newTableKeepSize(newCapacity);
//    int vi = 0;
//    for (int key : oldKeys) {
//      if (key != 0 && key != REMOVED_KEY) {
//        putInner(key, oldValues[vi]);
//      }
//      vi++;
//    }
//  }
    
//  // called by clear
//  private void newTable(int capacity) {
//    newTableKeepSize(capacity);
//    size = 0;
//    resetHistogram();
//  }
  
//  private void resetHistogram() {
//    if (TUNE) {
//      histogram = new int[200];
//      Arrays.fill(histogram, 0);
//      maxProbe = 0;
//    }    
//  }

//  public void clear() {
//    // see if size is less than the 1/2 size that triggers expansion
//    if (size <  (sizeWhichTriggersExpansion >>> 1)) {
//      // if 2nd time then shrink by 50%
//      //   this is done to avoid thrashing around the threshold
//      if (secondTimeShrinkable) {
//        secondTimeShrinkable = false;
//        final int newCapacity = Math.max(initialCapacity, keys.length >>> 1);
//        if (newCapacity < keys.length) { 
//          newTable(newCapacity);  // shrink table by 50%
//        } else { // don't shrink below minimum
//          Arrays.fill(keys, 0);
//          Arrays.fill(values,  null);
//        }
//        size = 0;
//        resetHistogram();
//        return;
//      } else {
//        secondTimeShrinkable = true;
//      }
//    } else {
//      secondTimeShrinkable = false; // reset this to require 2 triggers in a row
//    }
//    size = 0;
//    Arrays.fill(keys, 0);
//    Arrays.fill(values, null);
//    resetHistogram();
//  }

  /** 
   * Searches the keys for a match
   * @param key -
   * @return the probeAddr in keys array - The value[probeAddr] is 0 value if not found
   */
  
  private int findPosition(final int key) {
    
    if (key == 0) {  
      throw new IllegalArgumentException("0 is an invalid key");
    }
    if (key == REMOVED_KEY) {
      throw new IllegalArgumentException("Integer.MIN_VALUE is an invalid key");
    }
    
    
    return findPosition(
    
        // key hash
        Misc.hashInt(key),
        
        //is_eq_or_is_not_present
        i -> keys[i] == 0 || keys[i] == key,
        
        // is_removed_key
        i -> keys[i] == REMOVED_KEY
        
        );
        
  }
//  private int find(final int key) {
//    if (key == 0) {
//      throw new IllegalArgumentException("0 is an invalid key");
//    }
//    if (key == REMOVED_KEY) {
//      throw new IllegalArgumentException("Integer.MIN_VALUE is an invalid key");
//    }
//    found_removed = 0;  
//    final int hash = Misc.hashInt(key);
//
//    final int[] localKeys = keys;
//    final int bitMask = localKeys.length - 1;
//    int probeAddr = hash & bitMask;
//
//    // fast paths
//    final int testKey = localKeys[probeAddr];
//    if (testKey == 0 || testKey == key) {
//      if (TUNE) {
//        updateHistogram(1);
//      }
//      return probeAddr;
//    }
//    if (testKey == REMOVED_KEY) {
//      found_removed = probeAddr;
//    }
//    return find2(localKeys, key, probeAddr);
//  } 
 
//  private int find2(final int[] localKeys, final int key, int probeAddr) {
//    final int bitMask = localKeys.length - 1;
//    int nbrProbes = 2;
//    int probeDelta = 1;
//    probeAddr = bitMask & (probeAddr + (probeDelta++));
//
//    while (true) {
//      final int testKey = localKeys[probeAddr];
//      if (testKey == 0 || testKey == key) {
//        break;
//      }
//      if (found_removed == 0 && testKey == REMOVED_KEY) {
//        found_removed = probeAddr;
//      }
//      nbrProbes++;
//      probeAddr = bitMask & (probeAddr + (probeDelta++));
//    }
//
//    if (TUNE) {
//      final int pv = histogram[nbrProbes];
//
//      histogram[nbrProbes] = 1 + pv;
//      if (maxProbe < nbrProbes) {
//        maxProbe = nbrProbes;
//      }
//    }
//    return probeAddr;
//  }

//  private void updateHistogram(int nbrProbes) {
//    histogram[nbrProbes] = 1 + histogram[nbrProbes];
//    if (maxProbe < nbrProbes) {
//      maxProbe = nbrProbes;
//    }
//  }

  public T get(int key) {
    return (key == 0) ? null : values[findPosition(key)];
  }

  public T remove(int key) {
  int pos = findPosition(key);
  T v = values[pos];
  int k = keys[pos];                      
  if (k != 0) {
    values[pos] = null;
    keys[pos] = REMOVED_KEY;
    commonRemove();
  }
  return v;  
  }
  
//  private void maybeRebalanceRemoves() {
//    final int new_capacity = keys.length >> 1;
//    if (removed > REBALANCE_MIN_REMOVED &&
//        removed > new_capacity) {
//      // cleanup will remove more than 1/2 the items
//      
//      int [] oldKeys = keys;
//      T [] oldValues = values;
//      newTable(new_capacity);
//      removed = 0; // reset before put, otherwise, causes premature expansion
//      
//      for (int i = 0; i < oldKeys.length; i++) {
//        int k = oldKeys[i];
//        if (k != 0 && k != REMOVED_KEY) {
//          put(k, oldValues[i]);
//        }
//      }
//    }
//  }
  
  protected void copy_to_new_table(
      /* ignored */int newCapacity,
      /* ignored */int oldCapacity,
      CommonCopyOld2New commonCopy) {
    int [] oldKeys = keys;
    T [] oldValues = values;
    commonCopy.apply(
        
        // copyToNew 
        i -> 
          putInner(oldKeys[i], oldValues[i]),
        
        // is_valid_old_key 
        i ->  
          oldKeys[i] != 0 && oldKeys[i] != REMOVED_KEY);
  }
 
  // debug
//  private void dump(int[] ks) {
//    HashSet<Integer> found = new HashSet<>();
//    int nbr0 = 0;
//    int nbrRmv = 0;
//    for (int k : ks) {
//      if (k == 0) {
//        nbr0++;
//      } else if (k == REMOVED_KEY) {
//        nbrRmv ++;
//      } else {
//        boolean wasAdded = found.add(k);
//        if (! wasAdded) {
//          System.out.println("dups");
//        }
//      }
//    }
//  }
//    
//  private boolean checkKeyExists(int key) {
//    int c = 0;
//    for (int i = 0; i < keys.length; i++) {
//      int k = keys[i];
//      if (k == key) {
//        c++;
//        if (c > 1) {
//          System.out.println("found key " + key + " in position " + i);
//          return true;
//        }
//      }
//    }
//    return false;
//  }
  
  
  public boolean containsKey(int key) {
    int probeAddr = findPosition(key);
    return keys[probeAddr] != 0 ;
  }
 
  public T put(int key, T value) {
    int i = findPosition(key);
    final boolean keyNotFound = keys[i] == 0;
    final T prevValue = values[i];
    
    if (! keyNotFound) { // key found
      values[i] = value;
      return prevValue;
    }

    if (found_removed != -1) {
      i = found_removed;  // use the removed slot for the new value
    } 
//    //debug 
//    if (checkKeyExists(key)) {
//      find(key);
//      System.out.println("stop");
//    }
//    // debug
//    if (key == 322 && (i == 618 || i == 617)) {
//      find(key);
//      System.out.println("stop");
//    }

    keys[i] = key;
    values[i] = value;
    
    commonPutOrAddNotFound();
    return prevValue;
  }

  public void putInner(int key, T value) {
    final int i = findPosition(key);
    assert (keys[i] == 0);
    keys[i] = key;
    values[i] = value;
  }
 
  public int[] getSortedKeys() {
    final int size = size();
    if (size == 0) {
      return Constants.EMPTY_INT_ARRAY;
    }
    final int[] r = new int[size];
    int i = 0;
    for (int k : keys) {
      if (k != 0 && k != REMOVED_KEY) {
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
  
  /**
   * @return an iterator<T> over the values in random order
   */
  public Iterator<T> values() {
    return new Iterator<T>() {
      
      /**
       * Keep this always pointing to a non-0 entry, or
       * if not valid, outside the range
       */
      private int curPosition = moveToNextFilled(0);
      
      @Override
      public boolean hasNext() {
        return curPosition < keys.length;
      }

      @Override
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        T r = values[curPosition];
        curPosition = moveToNextFilled(curPosition + 1);
        return r;
      }
      
//      private void moveToNextFilled() {      
//        final int max = keys.length;
//        while (true) {
//          if (curPosition >= max) {
//            return;
//          }
//          int k = keys[curPosition];
//          if (k != 0 && k != REMOVED_KEY) {  
//            return;
//          }
//          curPosition ++;
//        }
//      }
    };
  }

  public Int2ObjHashMap<T> copy() {
    return new Int2ObjHashMap<>(this);
  }

  @Override
  public Iterator<IntEntry<T>> iterator() {
    
    return new Iterator<IntEntry<T>>() {
      
      /**
       * Keep this always pointing to a non-0 entry, or
       * if not valid, outside the range
       */
      private int curPosition = moveToNextFilled(0);
      
      @Override
      public boolean hasNext() {
        return curPosition < keys.length;
      }

      @Override
      public IntEntry next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        final IntEntry<T> r = new IntEntry<>(keys[curPosition], values[curPosition]);
        curPosition = moveToNextFilled(curPosition + 1);
        return r;        
      }
      
    };

  }

  protected int keys_length() {
    return keys.length;
  }

  @Override
  protected boolean is_valid_key(int pos) {
    return keys[pos] != 0 && keys[pos] != REMOVED_KEY;
  }

  @Override
  protected void newKeysAndValues(int size) {
    keys = new int[size];
    values = (T[]) Array.newInstance(componentType, size);    
  }

  @Override
  protected void clearKeysAndValues() {
    Arrays.fill(keys, 0);
    Arrays.fill(values, null);
  }

}
