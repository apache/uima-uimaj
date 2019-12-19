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
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.uima.cas.FeatureStructure;

/**
 * A set of Objects of type T 

 * This impl is for use in a single thread case only, when table is being updated.
 * Multiple reader threads are OK if there's no writing.
 * 
 * Supports shrinking (reallocating the big table)
 * 
 * Uses robin hood method, with 
 *   incrementing collision resolution, and shift backwards on remove, and 1 byte lower-hash-bits aux table 
 *   for locality of ref.
 */
public class ObjHashSetRh<T> extends Common_hash_support_rh implements Set<T> {
  
//  public static final float DEFAULT_LOAD_FACTOR = 0.66F;

  private final Class<T> clazz;  // for reifying the T type, resizing table, toArray operations
   
//  private final float loadFactor = DEFAULT_LOAD_FACTOR;  
  
//  private final int initialCapacity; 
//
//  private int histogram [];
//  private int maxProbe = 0;
//
//  private int sizeWhichTriggersExpansion;
//  private int size; // number of elements in the table
//  private int nbrRemoved; // number of removed elements (coded as removed)
  
  // the actual Object table, operated as a hashtable
  private T [] keys;
//  final private T [] emptyKeyArray;  

//  private boolean secondTimeShrinkable = false;
  
//  private int modificationCount = 0; // not currently used
  
  public ObjHashSetRh(Class<T> clazz) {
    this(12, clazz);  // default initial size
  }
    
  /**
   * @param initialCapacity - you can add this many before expansion
   * @param clazz - a superclass of the stored items
   * @param removedMarker - a unique value never stored in the table, used to mark removed items
   */
  public ObjHashSetRh(int initialCapacity, Class<T> clazz) {
    super(initialCapacity);
    this.clazz = clazz;
    newTable(initialCapacity);
  }
  
  /**
   * Copy constructor
   * @param ohs -
   */
  public ObjHashSetRh(ObjHashSetRh<T> ohs) {
    super(ohs);
    this.clazz = ohs.clazz;
    this.keys = Arrays.copyOf(ohs.keys, ohs.keys.length);
  }
  

  public ObjHashSetRh(ObjHashSetRh<T> ohs, boolean readOnly) {
    this(ohs);
    if (!readOnly) Misc.internalError();
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
  private int findPosition(final T key) {
    if (key == null) {
      throw new IllegalArgumentException("null is an invalid key");
    }
    
    return findPosition(
        
        // key hash
        Misc.hashInt(key.hashCode()),
        
        // is_eq_or_not_present
        i ->  keys[i] == null || keys[i].equals(key)); // keys[i] can be null  
  }
  

  @Override
  public boolean contains(Object obj) {  // arg must be Object to fit Collection API
    return (clazz.isAssignableFrom(obj.getClass())) ? (find((T) obj) != -1) : false;
  }
  
  /**
   * @param obj the object to find in the table (if it is there)
   * @return the position of obj in the table, or -1 if not in the table
   */
  public int find(T obj) {
    if (obj == null || size() == 0) {
      return -1;
    }
    
    final int pos = findPosition(obj);
    return obj.equals(keys[pos]) ? pos : -1;  // keys[pos] can be null
  }
      
  /**
   * 
   * @param obj - the object to add
   * @return true if this set did not already contain the specified element
   */
  @Override
  public boolean add(T obj) {           
    final int i = findPosition(obj);
    T c = keys[i];
    if (obj.equals(c)) { // keys[i] may be null
      return false;  // false if already present
    }
    
    // i is ref to empty or non-empty (but not equal) slot which should be stolen
    add_new(obj, c, i);
    incrementSize();
    return true;
  }
  
  private void add_new(T obj, T saved, int i) {
    keys[i] = obj;
    byte prev_lhb = setLhb(i, initial_probe_lhb);
    
    if (saved != null) {
      rh_add(incrPos(i), saved, prev_lhb);    
    }    
  }

  private void rh_add(int pos,T v, byte lhb) {
    while (true) {
      T c = keys[pos];
      T saved = c;  // if empty, is 0
      
      byte prev_lhb = setLhb(pos, lhb);
      keys[pos] = v;
      
      if (saved == null) {
        break;
      }
      // robin hood - if stole slot, add that slot back
      v = saved;
      lhb = prev_lhb;
      pos = incrPos(pos);
    } 
  }
  
  @Override
  protected void shift(int prev, int next) {
      keys[prev] = keys[next];
  }
  
  @Override
  protected void setEmpty(int pos) { 
    keys[pos] = null;
  }
    
  
  /**
   * Using robin hood shifting for removed item
   * @param rawKey the value to remove
   * @return true if the key was present
   */
  @Override
  public boolean remove(Object rawKey) {
    if (rawKey == null) {
      return false;
    }
    
    final int pos = findPosition((T) rawKey);  // null or equal or collision obj
    T c = keys[pos];
    if (c == null || ! c.equals(rawKey)) {
      return false;
    }
    
    remove_common(pos);
    return true;
  } 
  
  private void removeAtPosition(int pos) {
    remove_common(pos);
  }
  

//  /**
//   * @see Set#size()
//   */
//  @Override
//  public int size() {
//    return size;
//  }
  
//  public void showHistogram() {
//    if (TUNE) {
//      int sumI = 0;
//      for (int i : histogram) {
//        sumI += i;
//      }
//      
//      System.out.format(
//          "Histogram of number of probes, loadfactor = %.1f, maxProbe=%,d nbr of find operations at last table size=%,d%n",
//          loadFactor, maxProbe, sumI);
//      for (int i = 0; i <= maxProbe; i++) {
//        if (i == maxProbe && histogram[i] == 0) {
//          System.out.println("huh?");
//        }
//        System.out.println(i + ": " + histogram[i]);
//      }     
//      
//      System.out.println("bytes / entry = " + (float) (keys.length) * 4 / size());
//      System.out.format("size = %,d, prevExpansionTriggerSize = %,d, next = %,d%n",
//        size(),
//        (int) ((keys.length >>> 1) * loadFactor),
//        (int) (keys.length * loadFactor));        
//    }
//  }
  
  /**
   */
  /**
   * For iterator use
   * @param index - a magic number returned by the internal find
   * @return the T at that spot, or null if nothing there 
   */
  public T get(int index) {
    T obj = keys[index];
    if (obj == null) {
      return null;  // null, not present
    }
    return obj;
  }
  
////  /**
////   * advance pos until it points to a non 0 or is 1 past end
////   * @param pos -
////   * @return updated pos
////   */
////  public int moveToNextFilled(int pos) {
//////    if (pos < 0) {
//////      pos = 0;
//////    }
////    
////    final int max = getCapacity();
////    while (true) {
////      if (pos >= max) {
////        return pos;
////      }
////      T v = keys[pos];
////      if (v != null && v != removedMarker) {
////        return pos;
////      }
////      pos ++;
////    }
////  }
//   
//  /**
//   * decrement pos until it points to a non 0 or is -1
//   * @param pos -
//   * @return updated pos
//   */
//  public int moveToPreviousFilled(int pos) {
//    final int max = getCapacity();
//    if (pos > max) {
//      pos = max - 1;
//    }
//    
//    while (true) {
//      if (pos < 0) {
//        return pos;
//      }
//      T v = keys[pos];
//      if (v != null && v != removedMarker) {
//        return pos;
//      }
//      pos --;
//    }
//  }

  private class ObjHashSetIterator implements Iterator<T> {

    /**
     * Keep this always pointing to a non-0 entry, or
     * if not valid, outside the range
     */
    protected int curPosition;

    private ObjHashSetIterator() {
      this.curPosition = moveToNextFilled(0);
    }

    @Override
    public final boolean hasNext() {
      return curPosition < getCapacity();
    }

    @Override
    public final T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      T r = get(curPosition);
      curPosition = moveToNextFilled(curPosition + 1);
      return r;
    }

    @Override
    public void remove() {
      int pos = moveToPreviousFilled(curPosition - 1);
      if (pos >= 0) {
        removeAtPosition(pos);
      }
    }   
  }
  
    
  @Override
  public Iterator<T> iterator() {
    return new ObjHashSetIterator();
  }
  
  private int moveToFirst() {
    return (size() == 0) ? -1 : moveToNextFilled(0);
  }

//  private int moveToLast() {
//    return (size() == 0) ? -1 : moveToPreviousFilled(getCapacity() -1);
//  }

//  private int moveToNext(int position) {
//    if (position < 0) {
//      return position;
//    }
//    final int n = moveToNextFilled(position + 1); 
//    return (n >= getCapacity()) ? -1 : n;
//  }
//
//  private int moveToPrevious(int position) {
//    if (position >= getCapacity()) {
//      return -1;
//    }
//    return moveToPreviousFilled(position - 1);
//  }

//  public boolean isValid(int position) {
//    return (position >= 0) && (position < getCapacity());
//  }
  
  /**
   * if the fs is in the set, the iterator should return it.
   * if not, return -1 (makes iterator invalid)
   * @param fs position to this fs
   * @return the index if present, otherwise -1;
   */
  public int moveTo(FeatureStructure fs) {
    if (clazz.isAssignableFrom(fs.getClass())) {
      int pos = find((T)fs);
      if (pos >= 0) {
        return pos;
      }
    }
    return -1; 
  }

  @Override
  public <T2> T2[] toArray(T2[] a) {
    final int s = size();
    if (s == 0) {
      if (a.length >= 1) { 
        a[0] = null;  // part of the contract of toArray, where the array a size is > 
      }
      return a;
    }
    
    final T2[] r = (a.length >= s)? a : (T2[]) Array.newInstance(a.getClass(), s);
    int pos = moveToFirst();
    for (int i = 0; i < s; i ++) {
      r[i] = (T2) get(pos);
      pos = moveToNextFilled(pos + 1);
    }
    if (a.length > s) {
      r[s] = null; // part of the contract of toArray, where the array a size is > 
    }
    return r;
  }

  @Override
  public T[] toArray() {
    return toArray((T[]) Array.newInstance(clazz, size()));
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return String
        .format(
            "%s [loadFactor=%s, initialCapacity=%s, sizeWhichTriggersExpansion=%s, size=%s, secondTimeShrinkable=%s%n keys=%s]",
            this.getClass().getName(), loadFactor, initialCapacity, sizeWhichTriggersExpansion, size(), secondTimeShrinkable, 
            Arrays.toString(keys));
  }

  // Collection methods
  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    c.stream().allMatch(item -> contains(item));
    return false;
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    boolean[] anyChanged = {false};
    c.stream().forEach(item -> anyChanged[0] |= add(item));
    return anyChanged[0];
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    boolean[] anyChanged = {false};
    c.stream().forEach(item -> anyChanged[0] |= remove(item));
    return anyChanged[0];
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    boolean anyChanged = false;
    Iterator<T> it = iterator();
    while (it.hasNext()) {
      T v = it.next();
      if (!c.contains(v)) {
        anyChanged = true;
        it.remove();
      }
    }
    return anyChanged;
  }

  @Override
  protected boolean is_valid_key(int pos) {
    return keys[pos] != null;
  }

  @Override
  protected int keys_length() {
    return keys.length;
  }

  @Override
  protected void newKeysAndValues(int capacity) {
    keys = (T[]) Array.newInstance(clazz, capacity);
  }

  @Override
  protected void clearKeysAndValues() {
    Arrays.fill(keys, null);   
  }

  @Override
  protected void expand_table() {
    int old_capacity = keys_length();
    int new_capacity = old_capacity * 2;
    
    final T[] oldKeys = keys;
    newTable(new_capacity);
    for (T v : oldKeys) {
      if (v != null) { 
        int pos = findPosition_new( Misc.hashInt(v.hashCode()));
        add_new(v, keys[pos], pos);
      }
    }      
  }

  
}
