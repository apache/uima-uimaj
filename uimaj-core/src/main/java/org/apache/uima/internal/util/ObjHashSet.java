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
 * 
 * This impl is for use in a single thread case only, when table is being updated. Multiple reader
 * threads are OK if there's no writing.
 * 
 * Supports shrinking (reallocating the big table)
 * 
 * Removed objects replaced with special marker object in the table so find operations continue to
 * work (they can't stop upon finding this object).
 *
 */
public class ObjHashSet<T> extends Common_hash_support implements Set<T> {

  // public static final float DEFAULT_LOAD_FACTOR = 0.66F;

  /** the object of type T indicating key is removed */
  private final T removedMarker;

  private final Class<T> clazz; // for reifying the T type

  // private final float loadFactor = DEFAULT_LOAD_FACTOR;

  // private final int initialCapacity;
  //
  // private int histogram [];
  // private int maxProbe = 0;
  //
  // private int sizeWhichTriggersExpansion;
  // private int size; // number of elements in the table
  // private int nbrRemoved; // number of removed elements (coded as removed)

  // the actual Object table, operated as a hashtable
  private T[] keys;
  // final private T [] emptyKeyArray;

  // private boolean secondTimeShrinkable = false;

  // private int modificationCount = 0; // not currently used

  public ObjHashSet(Class<T> clazz, T removedMarker) {
    this(12, clazz, removedMarker); // default initial size
  }

  /**
   * @param initialCapacity
   *          - you can add this many before expansion
   * @param clazz
   *          - a superclass of the stored items
   * @param removedMarker
   *          - a unique value never stored in the table, used to mark removed items
   */
  public ObjHashSet(int initialCapacity, Class<T> clazz, T removedMarker) {
    super(initialCapacity);
    this.clazz = clazz;
    // this.emptyKeyArray = (T[]) Array.newInstance(clazz, 1);
    // this.initialCapacity = initialCapacity;
    newTable(initialCapacity);
    // size = 0;
    // if (TUNE) {
    // histogram = new int[200];
    // Arrays.fill(histogram, 0);
    // maxProbe = 0;
    // }
    this.removedMarker = removedMarker;
  }

  /**
   * Copy constructor
   * 
   * @param ohs
   *          -
   */
  public ObjHashSet(ObjHashSet<T> ohs) {
    super(ohs);
    this.removedMarker = ohs.removedMarker;
    this.clazz = ohs.clazz;
    // this.initialCapacity = ohs.initialCapacity;
    // this.histogram = ohs.histogram;
    // this.maxProbe = ohs.maxProbe;
    // this.sizeWhichTriggersExpansion = ohs.sizeWhichTriggersExpansion;
    // this.size = ohs.size;
    // this.nbrRemoved = ohs.nbrRemoved;
    this.keys = Arrays.copyOf(ohs.keys, ohs.keys.length);
    // this.secondTimeShrinkable = ohs.secondTimeShrinkable;
    // this.modificationCount = ohs.modificationCount;
  }

  public ObjHashSet(ObjHashSet<T> ohs, boolean readOnly) {
    this(ohs);
    if (!readOnly)
      Misc.internalError();
  }

  // private T[] emptyKeyArray() {
  // if (emptyKeyArray == null) {
  // emptyKeyArray = (T[]) Array.newInstance(clazz, 1);
  // }
  // return emptyKeyArray;
  // }

  // private void newTableKeepSize(int capacity) {
  // capacity = Misc.nextHigherPowerOf2(capacity);
  // keys = (T[]) Array.newInstance(clazz, capacity);
  // sizeWhichTriggersExpansion = (int)(capacity * loadFactor);
  // nbrRemoved = 0;
  // }

  // private void incrementSize() {
  // if (size + nbrRemoved >= sizeWhichTriggersExpansion) {
  // maybeIncreaseTableCapacity();
  // }
  // size++;
  // }

  // public boolean wontExpand() {
  // return wontExpand(1);
  // }
  //
  // public boolean wontExpand(int n) {
  // return (size + nbrRemoved + n) < sizeWhichTriggersExpansion;
  // }
  // /**
  // * @return the current capacity, &gt;= size
  // */
  // public int getCapacity() {
  // return keys.length;
  // }

  // /**
  // * This may not increase the table capacity, but may just
  // * clean out the REMOVED items
  // */
  // private void maybeIncreaseTableCapacity() {
  // final int oldCapacity = getCapacity();
  // // keep same capacity if just removing the "removed" markers would
  // // shrink the used slots to the same they would have been had there been no removed, and
  // // the capacity was doubled.
  // final int newCapacity = (nbrRemoved > size) ? oldCapacity : oldCapacity << 1;
  //
  // final T [] oldKeys = keys;
  // if (TUNE) {System.out.println("Capacity increasing from " + oldCapacity + " to " +
  // newCapacity);}
  // newTableKeepSize(newCapacity);
  // for (T key : oldKeys) {
  // if (key != null && key != removedMarker) {
  // addInner(key);
  // }
  // }
  // }

  // // called by clear
  // private void newTable(int capacity) {
  // newTableKeepSize(capacity);
  // resetTable();
  // }
  //
  // private void resetHistogram() {
  // if (TUNE) {
  // histogram = new int[200];
  // Arrays.fill(histogram, 0);
  // maxProbe = 0;
  // }
  // }
  //
  // private void resetArray() {
  // Arrays.fill(keys, null);
  // resetTable();
  // }
  //
  // private void resetTable() {
  // resetHistogram();
  // size = 0;
  //// modificationCount ++;
  // }

  // @Override
  // public void clear() {
  // // see if size is less than the 1/4 size that triggers expansion
  // if (size < (sizeWhichTriggersExpansion >>> 2)) {
  // // if 2nd time then shrink by 50%
  // // this is done to avoid thrashing around the threshold
  // if (secondTimeShrinkable) {
  // secondTimeShrinkable = false;
  // final int currentCapacity = getCapacity();
  // final int newCapacity = Math.max(initialCapacity, currentCapacity >>> 1);
  // if (newCapacity < currentCapacity) {
  // newTable(newCapacity); // shrink table by 50%
  // } else { // don't shrink below minimum
  // resetArray();
  // }
  // return;
  //
  // } else {
  // secondTimeShrinkable = true;
  // }
  // } else {
  // secondTimeShrinkable = false; // reset this to require 2 triggers in a row
  // }
  // resetArray();
  // }

  /**
   * returns a position in the key/value table if the key is not found, then the position will be to
   * the place where the key would be put upon adding (without reusing REMOVED, and the current
   * internal value of keys[position] would be 0.
   * 
   * if the key is found, then keys[position] == key
   * 
   * @param obj
   *          the object to find
   * @return the probeAddr in keys array - might reference a slot holding null, or the key value if
   *         found
   */
  private int findPosition(final T key) {
    if (key == null) {
      throw new IllegalArgumentException("null is an invalid key");
    }
    if (key == removedMarker) {
      throw new IllegalArgumentException("A removed marker is an invalid key");
    }

    return findPosition(

            // key hash
            Misc.hashInt(key.hashCode()),

            // is_eq_or_is_not_present
            i -> keys[i] == null || keys[i].equals(key),

            // is removed key
            i -> keys[i] == removedMarker);

    // return findPosition(obj, false);
  }

  // private int findPosition(final T obj, final boolean includeRemoved) {
  // if (obj == null) {
  // throw new IllegalArgumentException("null is an invalid key");
  // }
  //
  // final int hash = Misc.hashInt(obj.hashCode());
  // int nbrProbes = 1;
  // int probeDelta = 1;
  // int probeAddr;
  //
  // final T[] localKeys = keys;
  // final int bitMask = localKeys.length - 1;
  // probeAddr = hash & bitMask;
  // while (true) {
  // final T testKey = localKeys[probeAddr];
  // if (testKey == null || testKey.equals(obj) || (includeRemoved && testKey == removedMarker)) {
  // break;
  // }
  //
  // nbrProbes++;
  // probeAddr = bitMask & (probeAddr + (probeDelta++));
  // }
  //
  // if (TUNE) {
  // final int pv = histogram[nbrProbes];
  //
  // histogram[nbrProbes] = 1 + pv;
  // if (maxProbe < nbrProbes) {
  // maxProbe = nbrProbes;
  // }
  // }
  //
  // return probeAddr;
  // }

  @Override
  public boolean contains(Object obj) { // arg must be Object to fit Collection API
    return (clazz.isAssignableFrom(obj.getClass())) ? (find((T) obj) != -1) : false;
  }

  /**
   * @param obj
   *          the object to find in the table (if it is there)
   * @return the position of obj in the table, or -1 if not in the table
   */
  public int find(T obj) {
    if (obj == null || size() == 0) {
      return -1;
    }

    final int pos = findPosition(obj);
    return obj.equals(keys[pos]) ? pos : -1; // keys[pos] can be null
  }

  /**
   * 
   * @param obj
   *          - the object to add
   * @return true if this set did not already contain the specified element
   */
  @Override
  public boolean add(T obj) {
    final int i = findPosition(obj);
    if (obj.equals(keys[i])) { // keys[i] may be null
      return false; // false if already present
    }
    // if (keys[i] == removedMarker) {
    // nbrRemoved --;
    // assert (nbrRemoved >= 0);
    // }
    keys[(found_removed != -1) ? found_removed : i] = obj;
    commonPutOrAddNotFound();
    // modificationCount ++;
    // val259();
    return true;
  }

  /**
   * used for increasing table size
   * 
   * @param rawKey
   */
  private void addInner(T obj) {
    final int i = findPosition(obj);
    // //debug
    // if (keys[i] != null) {
    // System.out.println("debug");
    // }
    assert (keys[i] == null);
    keys[i] = obj;
    // val259();
  }

  /**
   * Can't replace the item with a null because other keys that were stored in the table which
   * previously collided with the removed item won't be found. UIMA-4204
   * 
   * @param rawKey
   *          the value to remove
   * @return true if the key was present
   */
  @Override
  public boolean remove(Object rawKey) {
    if (rawKey == null) {
      return false;
    }

    final int pos = findPosition((T) rawKey); // null or equal obj

    if (keys[pos] == null) {
      return false;
    }

    keys[pos] = removedMarker;
    commonRemove();
    // val259();
    return true;
  }

  private void removeAtPosition(int pos) {
    keys[pos] = removedMarker; // at runtime, this cast is a no-op
    commonRemove();
    // val259();
  }

  // /**
  // * @see Set#size()
  // */
  // @Override
  // public int size() {
  // return size;
  // }

  // public void showHistogram() {
  // if (TUNE) {
  // int sumI = 0;
  // for (int i : histogram) {
  // sumI += i;
  // }
  //
  // System.out.format(
  // "Histogram of number of probes, loadfactor = %.1f, maxProbe=%,d nbr of find operations at last
  // table size=%,d%n",
  // loadFactor, maxProbe, sumI);
  // for (int i = 0; i <= maxProbe; i++) {
  // if (i == maxProbe && histogram[i] == 0) {
  // System.out.println("huh?");
  // }
  // System.out.println(i + ": " + histogram[i]);
  // }
  //
  // System.out.println("bytes / entry = " + (float) (keys.length) * 4 / size());
  // System.out.format("size = %,d, prevExpansionTriggerSize = %,d, next = %,d%n",
  // size(),
  // (int) ((keys.length >>> 1) * loadFactor),
  // (int) (keys.length * loadFactor));
  // }
  // }

  /**
   */
  /**
   * For iterator use
   * 
   * @param index
   *          - a magic number returned by the internal find
   * @return the T at that spot, or null if nothing there
   */
  public T get(int index) {
    T obj = keys[index];
    if (obj == null || obj == removedMarker) {
      return null; // null, not present
    }
    return obj;
  }

  //// /**
  //// * advance pos until it points to a non 0 or is 1 past end
  //// * @param pos -
  //// * @return updated pos
  //// */
  //// public int moveToNextFilled(int pos) {
  ////// if (pos < 0) {
  ////// pos = 0;
  ////// }
  ////
  //// final int max = getCapacity();
  //// while (true) {
  //// if (pos >= max) {
  //// return pos;
  //// }
  //// T v = keys[pos];
  //// if (v != null && v != removedMarker) {
  //// return pos;
  //// }
  //// pos ++;
  //// }
  //// }
  //
  // /**
  // * decrement pos until it points to a non 0 or is -1
  // * @param pos -
  // * @return updated pos
  // */
  // public int moveToPreviousFilled(int pos) {
  // final int max = getCapacity();
  // if (pos > max) {
  // pos = max - 1;
  // }
  //
  // while (true) {
  // if (pos < 0) {
  // return pos;
  // }
  // T v = keys[pos];
  // if (v != null && v != removedMarker) {
  // return pos;
  // }
  // pos --;
  // }
  // }

  private class ObjHashSetIterator implements Iterator<T> {

    /**
     * Keep this always pointing to a non-0 entry, or if not valid, outside the range
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

  // private int moveToLast() {
  // return (size() == 0) ? -1 : moveToPreviousFilled(getCapacity() -1);
  // }

  // private int moveToNext(int position) {
  // if (position < 0) {
  // return position;
  // }
  // final int n = moveToNextFilled(position + 1);
  // return (n >= getCapacity()) ? -1 : n;
  // }
  //
  // private int moveToPrevious(int position) {
  // if (position >= getCapacity()) {
  // return -1;
  // }
  // return moveToPreviousFilled(position - 1);
  // }

  // public boolean isValid(int position) {
  // return (position >= 0) && (position < getCapacity());
  // }

  /**
   * if the fs is in the set, the iterator should return it. if not, return -1 (makes iterator
   * invalid)
   * 
   * @param fs
   *          position to this fs
   * @return the index if present, otherwise -1;
   */
  public int moveTo(FeatureStructure fs) {
    if (clazz.isAssignableFrom(fs.getClass())) {
      int pos = find((T) fs);
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
        a[0] = null; // part of the contract of toArray, where the array a size is >
      }
      return a;
    }

    final T2[] r = (a.length >= s) ? a : (T2[]) Array.newInstance(a.getClass(), s);
    int pos = moveToFirst();
    for (int i = 0; i < s; i++) {
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

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return String.format(
            "%s [loadFactor=%s, initialCapacity=%s, sizeWhichTriggersExpansion=%s, size=%s, secondTimeShrinkable=%s%n keys=%s]",
            this.getClass().getName(), loadFactor, initialCapacity, sizeWhichTriggersExpansion,
            size(), secondTimeShrinkable, Arrays.toString(keys));
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
    boolean[] anyChanged = { false };
    c.stream().forEach(item -> anyChanged[0] |= add(item));
    return anyChanged[0];
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    boolean[] anyChanged = { false };
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
    return keys[pos] != null & keys[pos] != removedMarker;
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
  protected void copy_to_new_table(int new_capacity, int old_capacity,
          CommonCopyOld2New commonCopy) {

    final T[] oldKeys = keys;

    commonCopy.apply(

            // copyToNew
            i -> addInner(oldKeys[i]),

            i -> oldKeys[i] != null && oldKeys[i] != removedMarker);
  }

  // debug
  // private static final Integer I259 = new Integer(259);
  // private void val259() {
  // int sum = 0;
  // for (int i = 0; i < keys.length; i++) {
  // T k = keys[i];
  // if (I259.equals(k)) {
  // sum++;
  // System.out.println("debug 259 " + i);
  // }
  // }
  // if (sum > 1) {
  // System.out.println("debug");
  // }
  // }
  // /**
  // * @return the modificiation count
  // */
  // public int getModificationCount() {
  // return modificationCount;
  // }

}
