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
import java.util.NoSuchElementException;

import org.apache.uima.cas.FeatureStructure;

/**
 * A Map from non-null Objects of type T to ints
 *   int value 0 reserved to mean object is not in the table.

 * This impl is for use in a single thread case only, for table updates
 * Multiple reader threads are OK if there's no writing.
 * 
 * Supports shrinking (reallocating the big table)
 * 
 * Removed objects replaced with special marker object in the table
 * so find operations continue to work (they can't stop upon finding this object).
 *
 */
public class Obj2IntIdentityHashMap<T> {
  
  public static final float DEFAULT_LOAD_FACTOR = 0.66F;
  // set to true to collect statistics for tuning
  // you have to also put a call to showHistogram() at the end of the run
  private static final boolean TUNE = false;

  private final T removedMarker;

  private final Class<T> componentType;  // for rieifying the T type
   
  private final float loadFactor = DEFAULT_LOAD_FACTOR;  
  
  private final int initialCapacity; 

  private int histogram [];
  private int maxProbe = 0;

  private int sizeWhichTriggersExpansion;
  private int size; // number of elements in the table
  private int nbrRemoved; // number of removed elements (coded as removed)
  
  // the actual Object table
  private T [] keys;
  private int [] values;

  private boolean secondTimeShrinkable = false;
  
  
  public Obj2IntIdentityHashMap(Class<T> clazz, T removedMarker) {
    this(12, clazz, removedMarker);  // default initial size
  }
    
  /**
   * @param initialCapacity - you can add this many before expansion
   * @param clazz - a superclass of the stored items
   * @param removedMarker - a unique value never stored in the table, used to mark removed items
   */
  public Obj2IntIdentityHashMap(int initialCapacity, Class<T> clazz, T removedMarker) {
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
    this.removedMarker = removedMarker;
  }
  
  private void newTableKeepSize(int capacity) {
    keys = (T[]) Array.newInstance(componentType, capacity);
    values = new int[capacity];
    sizeWhichTriggersExpansion = (int)(capacity * loadFactor);
    nbrRemoved = 0;
  }

  private void incrementSize() {
    if (size + nbrRemoved >= sizeWhichTriggersExpansion) {
      increaseTableCapacity();
    }
    size++;
  }
        
//  public boolean wontExpand() {
//    return wontExpand(1);
//  }
//  
//  public boolean wontExpand(int n) {
//    return (size + nbrRemoved + n) < sizeWhichTriggersExpansion;  
//  }
  
  public int getCapacity() {
    return keys.length;
  }
    
  /**
   * This may not increase the table capacity, but may just 
   * clean out the REMOVED items
   */
  private void increaseTableCapacity() {
    final int oldCapacity = getCapacity();
    // keep same capacity if just removing the "removed" markers would 
    // shrink the used slots to the same they would have been had there been no removed, and 
    // the capacity was doubled.
    final int newCapacity = (nbrRemoved > size) ? oldCapacity : oldCapacity << 1;
    
    final T [] oldKeys = keys;
    final int [] oldValues = values;
    if (TUNE) {System.out.println("Capacity increasing from " + oldCapacity + " to " + newCapacity);}
    newTableKeepSize(newCapacity);
    for (int i = 0; i < oldKeys.length; i++) {
      T key = oldKeys[i];
      if (key != null && key != removedMarker) {
        addInner(key, oldValues[i]);
      }
    }
  }
  
  // called by clear
  private void newTable(int capacity) {
    newTableKeepSize(capacity);
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
    Arrays.fill(keys, null);
    Arrays.fill(values,  0);
    resetTable();
  }
  
  private void resetTable() {
    resetHistogram();
    size = 0;
  }
  
  public void clear() {
    secondTimeShrinkable = Misc.maybeShrink(
        secondTimeShrinkable, size, getCapacity(), 2, initialCapacity,
        newCapacity -> newTable(newCapacity),
        () -> resetArray());
  }

  /** 
  * returns a position in the key/value table
  *   if the key is not found, then the position will be to the
  *   place where the key would be put upon adding (without reusing REMOVED, and the 
  *   current internal value of keys[position] would be 0.
  *   
  *   if the key is found, then keys[position] == key
  * @param obj the object to find
  * @return the probeAddr in keys array - might reference a slot holding null, or the key value if found
  */
  private int findPosition(final T obj) {
    return findPosition(obj, false);
  }
  
  private int findPosition(final T obj, final boolean includeRemoved) {
    if (obj == null) {
      throw new IllegalArgumentException("null is an invalid key");
    }

    final int hash = Misc.hashInt(System.identityHashCode(obj));  // identity hash map
    int nbrProbes = 1;
    int probeDelta = 1;
    int probeAddr;
    
    final T[] localKeys = keys;
    final int bitMask = localKeys.length - 1;
    probeAddr = hash & bitMask;
    while (true) {
      final T testKey = localKeys[probeAddr];
      if (testKey == null || testKey == obj || (includeRemoved && testKey == removedMarker)) { 
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

  public boolean contains(Object obj) {  // arg must be Object to fit Collection API
    return (componentType.isAssignableFrom(obj.getClass())) ? (find((T) obj) != -1) : false;
  }
  
  /**
   * @param obj the object to find in the table (if it is there)
   * @return the position of obj in the table, or -1 if not in the table
   */
  public int find(T obj) {
    if (obj == null || size == 0) {
      return -1;
    }
    
    final int pos = findPosition(obj);
    return (keys[pos] == obj) ? pos : -1;
  }
      
  public int get(T obj) {
    int pos = find(obj);
    return (pos >= 0) ? values[pos] : 0;
  }
  
  /**
   * 
   * @param obj - the object to add
   * @return the previous value, or 0 if this map did not already contain the specified key
   */
  public int put(T obj, int value) {
    if (obj == null) {
      throw new IllegalArgumentException("argument must be non-null");
    }
           
    final int i = findPosition(obj, true);  // include REMOVED
    if (keys[i] == obj) {  // identityHashMap
      int r = values[i];
      values[i] = value;
      return r;  
    }
    
    if (keys[i] == removedMarker) {
      nbrRemoved --;
      assert (nbrRemoved >= 0);
    }
    keys[i] = obj;
    int r = values[i];
    values[i] = value;
    incrementSize();
    return r;
  }
        
  /**
   * used for increasing table size
   * @param rawKey
   */
  private void addInner(T obj, int value) {
    final int i = findPosition(obj);
    assert(keys[i] == null);
    keys[i] = obj;
    values[i] = value;
  }
  
  /**
   * Can't replace the item with a null because other keys that were
   * stored in the table which previously collided with the removed item
   * won't be found.  UIMA-4204
   * @param rawKey the value to remove
   * @return the value previously associated with the key, or 0 if none
   */

  public int remove(Object rawKey) {
    if (rawKey == null) {
      return 0;
    }
    
    final int pos = findPosition((T) rawKey);  // null or equal obj
    
    return (rawKey == keys[pos]) ? removeAtPosition(pos) : 0;
  } 
  
  private int removeAtPosition(int pos) { 
    // found, remove it
    keys[pos] = (T) removedMarker;  // at runtime, this cast is a no-op
    int r = values[pos];
    values[pos] = 0;
    size--;
    nbrRemoved ++;
    return r;
  }
  
  public int size() {
    return size;
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
      
      System.out.println("bytes / entry = " + (float) (keys.length) * 4 / size());
      System.out.format("size = %,d, prevExpansionTriggerSize = %,d, next = %,d%n",
        size(),
        (int) ((keys.length >>> 1) * loadFactor),
        (int) (keys.length * loadFactor));        
    }
  }
  
//  /**
//   */
//  /**
//   * For iterator use
//   * @param index - a magic number returned by the internal find
//   * @return the T at that spot, or null if nothing there 
//   */
//  public int get(int index) {
//    T obj = keys[index];
//    if (obj == null || obj == removedMarker) {
//      return 0;  // not present
//    }
//    return values[index];
//  }
  
  /**
   * advance pos until it points to a non 0 or is 1 past end
   * @param pos
   * @return updated pos
   */
  public int moveToNextFilled(int pos) {
    if (pos < 0) {
      pos = 0;
    }
    
    final int max = getCapacity();
    while (true) {
      if (pos >= max) {
        return pos;
      }
      Object v = keys[pos];
      if (v != null && v != removedMarker) {
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
  public int moveToPreviousFilled(int pos) {
    final int max = getCapacity();
    if (pos > max) {
      pos = max - 1;
    }
    
    while (true) {
      if (pos < 0) {
        return pos;
      }
      T v = keys[pos];
      if (v != null && v != removedMarker) {
        return pos;
      }
      pos --;
    }
  }

  private class Obj2IntIdentityHashMapIterator implements IntListIterator {

    protected int curPosition;

    private Obj2IntIdentityHashMapIterator() {
      this.curPosition = moveToNextFilled(0);
    }

    @Override
    public final boolean hasNext() {
      curPosition = moveToNextFilled(curPosition);
      return curPosition < getCapacity();
    }

    @Override
    public final int next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      int r = values[curPosition];
      curPosition = moveToNextFilled(curPosition + 1);
      return r;
    }

    // if uncomment this, need to add it to the IntListIterator Interface
//    public void remove() {
//      int pos = moveToPrevious(curPosition - 1);
//      if (pos >= 0) {
//        removeAtPosition(pos);
//      } 
//    }

    @Override
    public boolean hasPrevious() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public int previous() {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public void moveToStart() {
      this.curPosition = moveToNextFilled(0);
    }

    @Override
    public void moveToEnd() {
      this.curPosition = moveToPreviousFilled(getCapacity() - 1);     
    }   
  }
  
  public T[] getKeys() {
    T[] r = (T[]) Array.newInstance(componentType, size());
    int i = 0;
    for (T key : keys) {
      if (key != null && key != removedMarker) {
        r[i++] = key;
      }
    }
    return r;  
  }
    
  public IntListIterator iterator() {
    return new Obj2IntIdentityHashMapIterator();
  }
  
  public int moveToFirst() {
    return (size() == 0) ? -1 : moveToNextFilled(0);
  }

  public int moveToLast() {
    return (size() == 0) ? -1 : moveToPreviousFilled(getCapacity() -1);
  }

  public int moveToNext(int position) {
    if (position < 0) {
      return position;
    }
    final int n = moveToNextFilled(position + 1); 
    return (n >= getCapacity()) ? -1 : n;
  }

  public int moveToPrevious(int position) {
    if (position >= getCapacity()) {
      return -1;
    }
    return moveToPreviousFilled(position - 1);
  }

  public boolean isValid(int position) {
    return (position >= 0) && (position < getCapacity());
  }
  
  /**
   * if the fs is in the set, the iterator should return it.
   * if not, move to the first - just to return something.
   * @param fs position to this fs
   * @return the index if present, otherwise moveToNextFileed(0);
   */
  public int moveTo(FeatureStructure fs) {
    if (componentType.isAssignableFrom(fs.getClass())) {
      int pos = find((T)fs);
      if (pos >= 0) {
        return pos;
      }
    }
    return moveToFirst(); 
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return String
        .format(
            "%s [loadFactor=%s, initialCapacity=%s, sizeWhichTriggersExpansion=%s, size=%s, secondTimeShrinkable=%s%n keys=%s]",
            this.getClass().getName(), loadFactor, initialCapacity, sizeWhichTriggersExpansion, size, secondTimeShrinkable, 
            Arrays.toString(keys));
  }

  public boolean isEmpty() {
    return size == 0;
  }

}
