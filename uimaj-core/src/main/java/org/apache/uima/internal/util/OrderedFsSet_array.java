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
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.cas.TOP;

//@formatter:off
/**
 * This one is being used, the other one (ending in 2) may be put back into service for large sizes,
 * later. (7/2017)
 * 
 * 
 * A set of FSs, ordered using a comparator Not thread-safe, use on single thread only
 * 
 * Use: set-sorted indexes in UIMA
 * 
 * Entries kept in order in 1 big TOP[]
 *   have ensureCapacity - grows by doubling up to multiplication-limit point, then by addition
 * 
 * Adds optimized:
 *   - maintain high mark, if &gt;, add to end
 * 
 * shifting optimization:
 *   for removes: shift space to back or front, whichever is closer 
 *   for adds: shift space from back or front, whichever is closer
 */
//@formatter:on
public class OrderedFsSet_array<T extends FeatureStructure> implements Iterable<T> {
  // public boolean specialDebug = false;
  final private static boolean TRACE = false;
  final private static boolean MEASURE = false;

  private static final int DEFAULT_SIZE = 8;

  private static final int DEFAULT_MULTIPLICATION_LIMIT = 1024 * 1024 * 16;

  final private int multiplication_limit = DEFAULT_MULTIPLICATION_LIMIT;

  TOP[] a;
  /**
   * index of slot at the end which is free, all following slots are free too
   */
  int a_nextFreeslot = 0;
  int a_firstUsedslot = 0;
  // comparators are over TOP, not "T", because it's allowed to compare
  // items which are supertypes of the index's items
  // e.g. compare something of type Annotation with "Token"
  final private Comparator<TOP> comparatorNoTypeWithID;
  final private Comparator<TOP> comparatorNoTypeWithoutID;
  private int maxSize = 0; // managing shrinking

  // private TOP highest = null;

  // maybe not needed due to cow - for tracking if any mods have been done
  // private int modificationCount = 0;

  private StringBuilder tr = TRACE ? new StringBuilder() : null;

  public OrderedFsSet_array(Comparator<TOP> comparatorNoTypeWithID,
          Comparator<TOP> comparatorNoTypeWithoutID) {
    this.comparatorNoTypeWithID = comparatorNoTypeWithID;
    this.comparatorNoTypeWithoutID = comparatorNoTypeWithoutID;
    a = new TOP[DEFAULT_SIZE];
  }

  // //debug
  // private static int callnbr = 0;
  /**
   * called to make a read-only copy
   * 
   * @param set
   *          -
   * @param isReadOnly
   *          -
   */
  public OrderedFsSet_array(OrderedFsSet_array<T> set, boolean isReadOnly) {
    if (!isReadOnly)
      Misc.internalError();

    // //debug
    // if ((callnbr++)%1024 == 0) {
    // System.out.format("debug shrink, a_firstUsedslot: %,4d set.a_nextFreeslot: %,4d, array
    // length: %,4d size: %,d%n", set.a_firstUsedslot, set.a_nextFreeslot, set.a.length, size);
    // }

    // Iterators have refs into this, so don't change the start offset
    // No issue with truncating though - these are read-only
    a = new TOP[set.a.length];
    System.arraycopy(set.a, 0, a, 0, set.a_nextFreeslot);
    a_firstUsedslot = set.a_firstUsedslot;
    a_nextFreeslot = set.a_nextFreeslot;
    comparatorNoTypeWithID = set.comparatorNoTypeWithID;
    comparatorNoTypeWithoutID = set.comparatorNoTypeWithoutID;

    maxSize = set.maxSize;
    // this.modificationCount = set.modificationCount;
  }

  public int size() {
    return a_nextFreeslot - a_firstUsedslot;
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public boolean add(T fs1) {
    throw new UnsupportedOperationException();
  }

  /**
   * 
   * @param fs1
   *          item to add
   * @param comparator
   *          either the comparator without type with ID for sorted indexes, or the comparator
   *          withoutType without ID for set indexes
   * @return true if fs was added (not already present)
   */
  public boolean add(T fs1, Comparator<TOP> comparator) {
    if (fs1 == null) {
      throw new IllegalArgumentException("Null cannot be added to this set.");
    }

    TOP fs = (TOP) fs1;

    int c = 1;

    boolean highest = false;

    if (size() == 0 || (c = comparator.compare(fs, a[a_nextFreeslot - 1])) > 0) {
      highest = true;
      // addNewHighest(fs);
      //// modificationCount++;
      // maxSize = Math.max(maxSize, size());
      // return true;
    }

    if (c == 0) { // found as equal to last item, skip insert
      return false;
    }

    /******************
     * add a new item *
     ******************/
    int insertPosOfAddedSpace;

    if (highest) {
      insertPosOfAddedSpace = a_nextFreeslot;
    } else {

      insertPosOfAddedSpace = find(fs, comparator);
      if (insertPosOfAddedSpace >= 0) {
        // was found
        return false;
      }

      insertPosOfAddedSpace = (-insertPosOfAddedSpace) - 1;
    }

    int indexOfNewItem = insertSpace(insertPosOfAddedSpace, highest) - 1;

    a[indexOfNewItem] = fs;
    // modificationCount++;
    maxSize = Math.max(maxSize, size());
    return true;
  }

  private void ensureCapacity() {
    // if space at end or space at beginning
    if (a_nextFreeslot < a.length || a_firstUsedslot > 0) {
      return;
    }

    int newSize = (a.length > multiplication_limit) ? a.length + multiplication_limit
            : (a.length << 1);

    a = Arrays.copyOf(a, newSize);
  }

//@formatter:off
  /**
   * This is called when inserting new items. May be called to insert at top
   * 
   * Side effects:  a_firstUsedslot adjusted if insert before first
   *                a_nextFreeslot adjusted if after last
   * 
   * Rebalancing: 
   *   normally not done, instead just the smaller distance to front/back things are moved
   *     by 1 position.
   * 
   *   for highest insert when out of space there 
   *      rebalance by moving 1/2 the space from front to end.
   * 
   *   for lowest  insert when out of space there
   *      rebalance by moving 1/2 the space from end to front.
   * 
   * @param insertPosOfAddedSpace
   *          position where new item goes 1 to the left
   * @param highest
   *          true if inserting at end
   * @return adjusted insertPosOfAddedSpace, the free spot is just to the left of this position
   */
//@formatter:on
  private int insertSpace(int insertPosOfAddedSpace, boolean highest) {
    if (TRACE) {
      tr.setLength(0);
      tr.append("Tracing OrderedFsSet_array\n");
      tr.append(String.format("insertSpace called with insertPosOfAddedSpace: %,d %n",
              insertPosOfAddedSpace));
    }

    ensureCapacity(); // add space at end if no space at front or end meaning capacity == size()

    final boolean useFront;

    if (highest) {

      if (a_nextFreeslot >= a.length) { // there's no room at end, only room in front.
        insertPosOfAddedSpace -= rebalanceMoveSpaceToEnd(); // updates a_nextFreeslot and
                                                            // a_firstUsedslot
      }

      a_nextFreeslot++;
      return insertPosOfAddedSpace + 1;

    } else if (insertPosOfAddedSpace == a_firstUsedslot) {

      // special case: add before first
      if (a_firstUsedslot == 0) { // there's no room at beginning, only room at end
        insertPosOfAddedSpace += rebalanceMoveSpaceToFront();
      }
      a_firstUsedslot--;
      return insertPosOfAddedSpace;
    }

    // not highest, not before first element
    int distanceFromEnd = a_nextFreeslot - insertPosOfAddedSpace;
    int distanceFromFront = insertPosOfAddedSpace - a_firstUsedslot;
    useFront = distanceFromFront < distanceFromEnd;

    /*******************
     * Use Front Space *
     *******************/
    if (useFront) {
      if (a_firstUsedslot == 0) {
        insertPosOfAddedSpace += rebalanceMoveSpaceToFront();
      }
      System.arraycopy(a, a_firstUsedslot, a, a_firstUsedslot - 1,
              insertPosOfAddedSpace - a_firstUsedslot);
      a_firstUsedslot--;
      return insertPosOfAddedSpace;
    }

    /*******************
     * Use End Space *
     *******************/
    if (a_nextFreeslot >= a.length) {
      insertPosOfAddedSpace -= rebalanceMoveSpaceToEnd();
    }
    System.arraycopy(a, insertPosOfAddedSpace, a, insertPosOfAddedSpace + 1,
            a_nextFreeslot - insertPosOfAddedSpace);
    a_nextFreeslot++;
    return insertPosOfAddedSpace + 1;
  }

  /**
   * move 1/2 of space at front to end
   * 
   * @return amount of space shifted to end, amount to decr insertPosOfAddedSpace
   */
  private int rebalanceMoveSpaceToEnd() {
    int amtOfShift = (a_firstUsedslot + 1) >> 1; // is a min of 1
    assert amtOfShift > 0;
    System.arraycopy(a, a_firstUsedslot, a, a_firstUsedslot - amtOfShift, size());
    Arrays.fill(a, a_nextFreeslot - amtOfShift, a_nextFreeslot, null);
    a_nextFreeslot -= amtOfShift;
    a_firstUsedslot -= amtOfShift;
    return amtOfShift;
  }

  /**
   * move 1/2 of space at end to front
   * 
   * @return amount of space shifted to end, amount to incr insertPosOfAddedSpace
   */
  private int rebalanceMoveSpaceToFront() {
    int amtOfShift = (1 + a.length - a_nextFreeslot) >> 1; // is a min of 1
    assert amtOfShift > 0;
    System.arraycopy(a, a_firstUsedslot, a, a_firstUsedslot + amtOfShift, size());
    Arrays.fill(a, a_firstUsedslot, a_firstUsedslot + amtOfShift, null);
    a_nextFreeslot += amtOfShift;
    a_firstUsedslot += amtOfShift;
    return amtOfShift;
  }

  // /**
  // * If all items are LT key, returns - size - 1
  // * @param fs the key
  // * @return the lowest position whose item is equal to or greater than fs;
  // * if not equal, the item's position is returned as -insertionPoint - 1.
  // * If the key is greater than all elements, return -size - 1).
  // */
  // private int find(TOP fs) {
  // return binarySearch(a, a_firstUsedslot, a_nextFreeslot, fs, comparatorNoTypeWithID);
  // }

  /**
   * using NoType because all callers of this have already used the type of fs to select the right
   * index.
   * 
   * @param fs
   *          -
   * @return -
   */
  public int findWithoutID(TOP fs) {
    return binarySearch(a, a_firstUsedslot, a_nextFreeslot, fs, comparatorNoTypeWithoutID);
  }

  public int find(TOP fs, Comparator<TOP> comparator) {
    return binarySearch(a, a_firstUsedslot, a_nextFreeslot, fs, comparator);
  }

  /**
   * 
   * @param _a
   *          the array
   * @param start
   *          the index representing the lower bound (inclusive) to search for
   * @param end
   *          the index representing the upper bound (exclusive) to search for
   * @param fs
   *          - the fs to search for
   * @param _comparatorWithID
   *          -
   * @return - the index of the found item, or if not found, the (-index) -1 of the position one
   *         more than where the item would go
   */
  public int binarySearch(TOP[] _a, int start, int end, final TOP fs,
          Comparator<TOP> _comparatorWithID) {
    return Arrays.binarySearch(_a, start, end, fs, _comparatorWithID);

    // if (start < 0 || end - start <= 0) {
    // return (start < 0) ? -1 : ( (-start) - 1); // means not found, insert at position start
    // }
    // int lower = start, upper = end;
    // for (;;) {
    //
    // int mid = (lower + upper) >>> 1; // overflow aware
    // TOP item = _a[mid];
    // int pos = mid;
    //
    // int c = _comparatorWithID .compare(fs, item);
    // if (c == 0) {
    // return pos;
    // }
    //
    // if (c < 0) { // fs is smaller than item at pos in array; search downwards
    // upper = pos; // upper is exclusive
    // if (upper == lower) {
    // return (-upper) - 1;
    // }
    // } else { // fs is larger than item at pos in array; search upwards
    // lower = pos + 1; // lower is inclusive
    // if (lower == upper) {
    // return (-upper) - 1;
    // }
    // }
    // }
  }

  /**
   * Removes the exactly matching (including ID) FS if present
   * 
   * Only called when type of FS matches this index's type, so the NoType comparator is used.
   * 
   * @param o
   *          the object (must be a FS of the type of this index) to remove
   * @return true if it was removed, false if it wasn't in the index
   */

  public boolean remove(Object o) {
    if (o == null) {
      throw new IllegalArgumentException("Null cannot be the argument to remove");
    }

    if (!(o instanceof TOP)) {
      return false;
    }

    TOP fs = (TOP) o;

    int pos = binarySearch(a, a_firstUsedslot, a_nextFreeslot, fs, comparatorNoTypeWithID);
    // find(fs); // using ID as part of comparator
    if (pos < 0) {
      return false;
    }

    // at this point, pos points to a spot that compares "equal" using the comparator
    // for sets, this is the single item that is in the index
    // for sorted, because find uses the compareWithID comparator, this is the unique equal element

    // compute closest space

    /******************************************
     * remove by shifting using closest space *
     ******************************************/
    final int distanceFromEnd = a_nextFreeslot - pos;
    final int distanceFromFront = pos - a_firstUsedslot;

    if (distanceFromFront < distanceFromEnd) {
      if (distanceFromFront > 0) { // skip when distance is 0 - no move needed
        System.arraycopy(a, a_firstUsedslot, a, a_firstUsedslot + 1, distanceFromFront);
      }
      a[a_firstUsedslot] = null;
      a_firstUsedslot++;
    } else {
      if (distanceFromEnd > 1) { // skip when distance from end == 0, no move needed
        System.arraycopy(a, pos + 1, a, pos, distanceFromEnd - 1); // sub 1 because a_nextFreeslot
                                                                   // is exclusive
      }
      a_nextFreeslot--;
      a[a_nextFreeslot] = null;
    }
    // modificationCount ++;
    return true;
  }

  /**
   * @see Set#clear()
   */
  public void clear() {
    if (isEmpty()) {
      return;
    }

    int len = a.length;
    if (maxSize < (len >> 3) && len > 128) {
      int newSize = len >> 1;
      a = new TOP[newSize];
    } else {
      Arrays.fill(a, null);
    }
    a_firstUsedslot = 0;
    a_nextFreeslot = 0;
    // modificationCount ++;
    maxSize = 0;

  }

  /**
   * Guaranteed by caller to have an equal (withoutID) item, but might be the "end" item searching
   * up to find it.
   * 
   * @param fs
   *          - the fs to search for
   * @param start
   *          the index representing the lower bound (inclusive) to search for
   * @param end
   *          the index representing the upper bound (exclusive) to search for Not called unless
   *          there's one equal item below this.
   * @param comparator
   *          the comparator to use (with or without type)
   * @return - the index of the leftmost equal (without id) item
   */
  public int binarySearchLeftMostEqual(final TOP fs, int start, int end,
          Comparator<TOP> comparator) {

    // assert start >= 0;
    // assert start < end;

    int lower = start, upper = end;
    for (;;) {

      int mid = (lower + upper) >>> 1; // overflow aware
      TOP item = a[mid];
      int pos = mid;

      int c = comparator.compare(item, fs);
      if (c == 0) {
        upper = pos; // upper is exclusive
        if (upper == lower) {
          return upper;
        }
      } else { // item is less than fs; search upwards
        lower = pos + 1; // lower is inclusive
        if (lower == upper) {
          return upper;
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {

      int pos = a_firstUsedslot;

      @Override
      public boolean hasNext() {
        return pos >= 0 && pos < a_nextFreeslot;
      }

      @Override
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return (T) a[pos++];
      }
    };
  }

  public TOP[] toArray() {
    TOP[] r = new TOP[size()];
    System.arraycopy(a, a_firstUsedslot, r, 0, size());
    return r;
  }

  public <U> U[] toArray(U[] a1) {
    if (a1.length < size()) {
      a1 = (U[]) Array.newInstance(a1.getClass(), size());
    }
    System.arraycopy(a1, a_firstUsedslot, a1, 0, size());
    return a1;
  }

  // public int getModificationCount() {
  // return modificationCount;
  // }

  public T getAtPos(int pos) {
    return (T) a[pos];
  }

  @Override
  public String toString() {
    // processBatch();
    StringBuilder b = new StringBuilder();
    b.append("OrderedFsSet_array [a=");
    if (a != null) {
      boolean firstTime = true;
      for (TOP i : a) {
        if (firstTime) {
          firstTime = false;
        } else {
          b.append(",\n");
        }
        if (i != null) {
          b.append(((TOP) i).toShortString());
        } else {
          b.append("null");
        }
        // prettyPrint(0, 2, b, true);
      }
    } else {
      b.append("null");
    }
    b.append(", a_nextFreeslot=").append(a_nextFreeslot).append(", a_firstUsedslot=")
            .append(a_firstUsedslot).append(", origComparator=").append(comparatorNoTypeWithID)
            .append(", maxSize=").append(maxSize).append("]");
    return b.toString();
  }

  // these are approximate - don't take into account multi-thread access
  static private int addToEndCount = 0;
  // static private int addNotToEndCount = 0;
  static private int batchCountHistogram[];
  static private int batchAddCount = 0;
  static private int batchAddTotal = 0; // includes things not added because of dups
  static private int moveSizeHistogram[];
  static private int movePctHistogram[];
  static private int fillHistogram[];
  static private int iterPctEmptySkip[];

  static {
    if (MEASURE) {
      batchCountHistogram = new int[24]; // slot x = 2^x to (2^(x+1) - 1) counts
                                         // slot 0 = 1, slot 1 = 2-3, etc
      Arrays.fill(batchCountHistogram, 0);

      moveSizeHistogram = new int[24];
      movePctHistogram = new int[10]; // slot 0 = 0-9% 1 = 10-19% 9 = 90 - 100%
      fillHistogram = new int[24];

      iterPctEmptySkip = new int[10];

      Runtime.getRuntime().addShutdownHook(new Thread(null, () -> {
        System.out.println("Histogram measures of Ordered Set add / remove operations");
        System.out.format(" - Add to end: %,d,  batch add count: %,d  batch add tot: %,d%n",
                addToEndCount, batchAddCount, batchAddTotal);
        for (int i = 0; i < batchCountHistogram.length; i++) {
          int v = batchCountHistogram[i];
          if (v == 0)
            continue;
          System.out.format(" batch size: %,d, count: %,d%n", 1 << i, v);
        }
        for (int i = 0; i < moveSizeHistogram.length; i++) {
          int v = moveSizeHistogram[i];
          if (v == 0)
            continue;
          System.out.format(" move size: %,d, count: %,d%n", (i == 0) ? 0 : 1 << (i - 1), v);
        }
        for (int i = 0; i < movePctHistogram.length; i++) {
          int v = movePctHistogram[i];
          if (v == 0)
            continue;
          System.out.format(" move Pct: %,d - %,d, count: %,d%n", i * 10, (i + 1) * 10, v);
        }
        for (int i = 0; i < fillHistogram.length; i++) {
          int v = fillHistogram[i];
          if (v == 0)
            continue;
          System.out.format(" fill size: %,d, count: %,d%n", (i == 0) ? 0 : 1 << (i - 1), v);
        }
        for (int i = 0; i < iterPctEmptySkip.length; i++) {
          int v = iterPctEmptySkip[i];
          if (v == 0)
            continue;
          System.out.format(" iterator percent empty needing skip: %,d - %,d, count: %,d%n", i * 10,
                  (i + 1) * 10, v);
        }

      }, "dump measures OrderedFsSetSorted"));
    }

  }

  // /* (non-Javadoc)
  // * @see java.util.Set#contains(java.lang.Object)
  // */
  // @Override
  // public boolean contains(Object o) {
  // if (o == null) {
  // throw new IllegalArgumentException();
  // }
  // if (isEmpty()) {
  // return false;
  // }
  // if (! (o instanceof TOP)) {
  // return false;
  // }
  // TOP fs = (TOP) o;
  // return findWithoutID(fs) >= 0;
  // }

  //
  //
  // /* (non-Javadoc)
  // * @see java.util.Set#containsAll(java.util.Collection)
  // */
  // @Override
  // public boolean containsAll(Collection<?> c) {
  // throw new UnsupportedOperationException();
  // }
  //
  // /* (non-Javadoc)
  // * @see java.util.Set#addAll(java.util.Collection)
  // */
  // @Override
  // public boolean addAll(Collection<? extends T> c) {
  // boolean changed = false;
  // for (T item : c) {
  // changed |= add(item);
  // }
  // return changed;
  // }
  //
  // /* (non-Javadoc)
  // * @see java.util.Set#retainAll(java.util.Collection)
  // */
  // @Override
  // public boolean retainAll(Collection<?> c) {
  // throw new UnsupportedOperationException();
  // }
  //
  // /* (non-Javadoc)
  // * @see java.util.Set#removeAll(java.util.Collection)
  // */
  // @Override
  // public boolean removeAll(Collection<?> c) {
  // throw new UnsupportedOperationException();
  // }
  //
  //
  //
}
