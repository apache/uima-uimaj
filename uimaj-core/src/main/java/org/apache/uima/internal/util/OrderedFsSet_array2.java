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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Supplier;

import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.util.impl.Constants;

/**
 * This one not in current use
 * Maybe be put back into service when the array becomes large and it starts outperforming the other
 * 
 * A set of FSs, ordered using a comparator 
 * Not thread-safe, use on single thread only
 * 
 * Use: set-sorted indexes in UIMA
 * 
 * Entries kept in order in 1 big ArrayList
 * 
 * Adds optimized:
 *   - maintain high mark, if &gt;, add to end
 *   - batch adds other than above
 *     -- do when reference needed
 *     -- sort the to be added
 *   - to add to pos p, shift elements in p to higher, insert   
 * 
 * shifting optimization: 
 *   removes replace element with null
 *   shift until hit null 
 *   
 * nullBlock - a group of nulls (free space) together
 *   - might be created by a batch add which 
 *     adds a block of space all at once
 *   - might arise from encountering 1 or more "nulls" created
 *     by removes
 *   - id by nullBlockStart (inclusive) and nullBlockEnd (exclusive)
 *   
 * bitset: 1 for avail slot
 *   used to compute move for array copy
 * 
 *   
 */
public class OrderedFsSet_array2 implements NavigableSet<TOP> {
//  public boolean specialDebug = false;
  final private static boolean TRACE = false;
  final private static boolean MEASURE = false;
  final private static int DEFAULT_MIN_SIZE = 8;  // power of 2 please
  final private static int MAX_DOUBLE_SIZE = 1024 * 1024 * 4;  // 4 million, power of 2 please
  final private static int MIN_SIZE = 8;
   
//  final private static MethodHandle getActualArray;
//  
//  static {
//    Field f;
//    try {
//      f = ArrayList.class.getDeclaredField("array");
//    } catch (NoSuchFieldException e) {
//      try {
//        f = ArrayList.class.getDeclaredField("elementData");
//      } catch (NoSuchFieldException e2) {
//        throw new RuntimeException(e2);
//      }
//    }
//    
//    f.setAccessible(true);
//    try {
//      getActualArray = Misc.UIMAlookup.unreflectGetter(f);
//    } catch (IllegalAccessException e) {
//      throw new RuntimeException(e);
//    }
//  }
  
    
  TOP[] a = new TOP[DEFAULT_MIN_SIZE];
  /**
   * index of slot at the end which is free, all following slots are free too
   */
  int a_nextFreeslot = 0;
  int a_firstUsedslot = 0;
  
  private final ArrayList<TOP> batch = new ArrayList<>();
  
  final public Comparator<TOP> comparatorWithID;
  final public Comparator<TOP> comparatorWithoutID;
  private int size = 0;
  private int maxSize = 0;
  
  private TOP highest = null;
  private int nullBlockStart = -1;  // inclusive
  private int nullBlockEnd = -1 ;    // exclusive
  
  private boolean doingBatchAdds = false;
  private int modificationCount = 0;
  /**
   * Tricky to maintain.
   * If holes are moved around, this value may need updating
   */
  private int lastRemovedPos = -1;
  
  private StringBuilder tr = TRACE ? new StringBuilder() : null;
//  private int nbrNewSlots;  // this is a field so it can be read and set by insertSpace
  
  // debug
//  private int itercount = 0;
  
  public OrderedFsSet_array2(Comparator<TOP> comparatorWithID, Comparator<TOP> comparatorWithoutID) {
    this.comparatorWithID = comparatorWithID;
    this.comparatorWithoutID = comparatorWithoutID;
  }
  
  /**
   * copy constructor - not currently used (06/2017)
   * @param set the original to be copied
   */
  public OrderedFsSet_array2(OrderedFsSet_array2 set) {
    set.processBatch();
    this.a = Arrays.copyOf(set.a, set.a.length);
    this.a_nextFreeslot = set.a_nextFreeslot;
    this.a_firstUsedslot = set.a_firstUsedslot;
    this.comparatorWithID = set.comparatorWithID;
    this.comparatorWithoutID = set.comparatorWithoutID;
    this.size = set.size;
    this.maxSize = set.maxSize;
    this.highest = set.highest;
    this.nullBlockStart = set.nullBlockStart;
    this.nullBlockEnd = set.nullBlockEnd;
    this.modificationCount = set.modificationCount;
    this.lastRemovedPos = set.lastRemovedPos;
  }

  /**
   * called to make a read-only copy
   * @param set -
   * @param isReadOnly -
   */
  public OrderedFsSet_array2(OrderedFsSet_array2 set, boolean isReadOnly) {
    if (!isReadOnly) Misc.internalError();
    set.processBatch();
    this.size = set.size;
    this.a = (size == 0) ? Constants.EMPTY_TOP_ARRAY : Arrays.copyOf(set.a, set.a.length);
    this.a_nextFreeslot = set.a_nextFreeslot;
    this.a_firstUsedslot = set.a_firstUsedslot;
    this.comparatorWithID = set.comparatorWithID;
    this.comparatorWithoutID = set.comparatorWithoutID;
    
    this.maxSize = set.maxSize;
    this.highest = set.highest;
    this.nullBlockStart = set.nullBlockStart;
    this.nullBlockEnd = set.nullBlockEnd;
    this.modificationCount = set.modificationCount;
    this.lastRemovedPos = set.lastRemovedPos;
  }
  
  

  /**
   * @see SortedSet#comparator()
   */
  @Override
  public Comparator<? super TOP> comparator() {
    return comparatorWithID;
  }

  /**
   * @see SortedSet#first()
   */
  @Override
  public TOP first() {
    processBatch();
    if (size == 0) {
      throw new NoSuchElementException();
    }
    for (int i = a_firstUsedslot; i < a_nextFreeslot; i++) {
      TOP item = a[i];
      if (null != item) {
        if (i > a_firstUsedslot) {
          a_firstUsedslot = i;
        }
        return item; 
      }
    }
    Misc.internalError();
    return null;
  }

  /**
   * @see SortedSet#last()
   */
  @Override
  public TOP last() {
    processBatch();
    if (size == 0) {
      throw new NoSuchElementException();
    }
    for (int i = a_nextFreeslot - 1; i >= a_firstUsedslot; i--) {
      TOP item = a[i];
      if (item != null) {
        if (i < a_nextFreeslot - 1) {
          a_nextFreeslot = i + 1;
        }
        return item;
      }
    }
    Misc.internalError();
    return null;
  }

  /**
   * @see Set#size()
   */
  @Override
  public int size() {
    processBatch();
    return size;
  }

  /**
   * @see Set#isEmpty()
   */
  @Override
  public boolean isEmpty() {
    return size == 0 && batch.size() == 0;
  }

  /**
   * @see Set#contains(Object)
   */
  @Override
  public boolean contains(Object o) {
    if (o == null) {
      throw new IllegalArgumentException();
    }
    if (isEmpty()) {
      return false;
    }
    TOP fs = (TOP) o;
    processBatch();
    return find(fs) >= 0;
  }

  /**
   * @see Set#toArray()
   */
  @Override
  public Object[] toArray() {
    Object [] r = new Object[size()];
    int i = 0;
    for (TOP item : a) {
      if (item != null) {
        r[i++] = item;
      }
    }
//    try { // debug
      assert r.length == i;
//    } catch (AssertionError e) { // debug
//      System.err.format("size: %,d, final index: %,d, array length: %,d%n", size(), i, a.length );
//      for (int di = 0; di < a.length; di++) {
//        System.err.format("a[%,d] = %s%n", di, a[di]);
//      }
//      System.err.format("first used slot: %,d, next free slot: %,d batch size: %,d,"
//          + " nullblockstart: %,d nullBlockEnd: %d, lastRemovedPos: %,d",
//          a_firstUsedslot, a_nextFreeslot, batch.size(), nullBlockStart, nullBlockEnd,
//          lastRemovedPos);
//      throw e;
//    }
    return r;
  }

  /**
   * @see Set#toArray(Object[])
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] a1) {
    if (a1.length < size()) {
      a1 = (T[]) Array.newInstance(a.getClass(), size());
    }
    int i = 0;
    for (TOP item : a) {
      if (item != null) {
        a1[i++] = (T) item;
      }
    }
    if (i < a1.length) {
      a1[i] = null;  // contract for toArray, when array bigger than items
    }
    return a1;
  }

  /**
   * Note: doesn't implement the return value; always returns true;
   * @see Set#add(Object)
   */
  
  @Override
  public boolean add(TOP fs) {
    if (fs == null) {
      throw new IllegalArgumentException("Null cannot be added to this set.");
    }
    if (highest == null) {
      addNewHighest(fs);
      return true;
    }
    
    int c = comparatorWithID.compare(fs, highest);
    if (c > 0) {
      addNewHighest(fs);
      return true;
    }
    
    if (c == 0) {
      return false;
    }
    
    batch.add(fs);
    if (MEASURE) {
      addNotToEndCount ++;
    }
    return true;
  }
  
  private void addNewHighest(TOP fs) {
    highest = fs;
    ensureCapacity(1);
    a[a_nextFreeslot++] = fs;
    incrSize();
    if (MEASURE) {
      addToEndCount++;
    }
    return;
  }
  
  private void incrSize() {
    size++;
    maxSize = Math.max(maxSize, size);
    modificationCount++;
  }
  
//  /** validate array
//   *    number of non-null elements == size
//   */
//  // debug
//  private void validateA() {
//    synchronized (batch) {
//      try {
//        if (nullBlockStart != -1) {
//          assert a[nullBlockStart] == null;
//          if (nullBlockStart > 0) {
//            assert a[nullBlockStart - 1] != null;
//          }
//        }
//        int sz = 0;
//        for (TOP item : a) {
//          if (item != null) {
//            sz++;
//          }
//        }
//    //    if (sz != size) {
//    //      System.out.format("debug error OrderedFsSet_array size(): %,d array non-null element count: %,d%n",
//    //          size, sz);
//    //    }
//        assert sz == size;
//        for (int i = 0; i < a_firstUsedslot; i++) {
//          assert a[i] == null;
//        }
//        for (int i = a_nextFreeslot; i < a.length; i++) {
//          assert a[i] == null;
//        }
//        assert a_firstUsedslot < a_nextFreeslot;
//        TOP prev = a[a_firstUsedslot];
//        for (int i = a_firstUsedslot + 1; i < a_nextFreeslot; i++) {
//          TOP fs = a[i];
//          if (fs != null) {
//            assert comparatorWithID.compare(fs, prev) > 0;
//            prev = fs;
//          }
//        }
//      } catch (AssertionError e) {
//        e.printStackTrace();
//      }
//    }  
//  }

  private void ensureCapacity(int incr) {
    int szNeeded = a_nextFreeslot + incr;
    if (szNeeded <= a.length) {
      return;
    }
    int sz = a.length;
    do {
      sz = (sz < MAX_DOUBLE_SIZE) ? (sz << 1) : (sz + MAX_DOUBLE_SIZE);
    } while (sz < szNeeded);
    
    TOP[] aa = new TOP[sz];
    System.arraycopy(a, 0, aa, 0, a_nextFreeslot);
    a = aa;
  }
  
  private boolean shrinkCapacity() {
    int nextSmallerSize = getNextSmallerSize(2);
    if (nextSmallerSize == MIN_SIZE) {
      return false;
    }
    if (maxSize < nextSmallerSize) {
      a = new TOP[getNextSmallerSize(1)];
      maxSize = 0;
      return true;
    }
    maxSize = 0; 
    return false;
  }
  
  /**
   * get next smaller size
   * @param n number of increments
   * @return the size
   */
  private int getNextSmallerSize(int n) {
    int sz = a.length;
    if (sz <= MIN_SIZE) {
      return MIN_SIZE;
    }
    for (int i = 0; i < n; i ++) {
      sz = (sz > MAX_DOUBLE_SIZE) ? (sz - MAX_DOUBLE_SIZE) : sz >> 1;
    }
    return sz;
  }
  
  public void processBatch() {
    if (batch.size() != 0) {
//      validateA();
      doProcessBatch();
//      validateA();
    }
  }
  
  /**
   * Because multiple threads can be "reading" the CAS and using iterators,
   * the sync must insure that the setting of batch.size() to 0 occurs after
   * all the adding is done.
   * 
   * This keeps other threads blocked until the batch is completely processed.
   */
  private void doProcessBatch() {
    synchronized (batch) {
      int batchSize = batch.size();

      if (batchSize == 0) {
        return;  // another thread did this
      }
      if (doingBatchAdds == true) {
        return;  // bypass recursive calls from Eclipse IDE on same thread, 
                 // when its toString methods invoke this recursively to update the
                 // debug UI for instance, while single stepping.
      }
      try {
//        validateA();
        // debug 
//        assert (lastRemovedPos != -1) ? a[lastRemovedPos] == null : true;
        doingBatchAdds = true;
        if (MEASURE) {
          batchAddCount ++;
          batchAddTotal += batchSize;
          batchCountHistogram[31 - Integer.numberOfLeadingZeros(batchSize)] ++;
        }
 
        /* the number of new empty slots created, 
         *   may end up being larger than actually used because some of the items 
         *   being inserted may already be in the array
         *     - decreases as each item is actually inserted into the array
         */
        int nbrNewSlots = 1; // start at one, may increase
        
        if (batchSize > 1) {
          // Sort the items to add 
          batch.sort(comparatorWithID);
          TOP prev = batch.get(batchSize - 1);
        
//          nbrNewSlots = batch.size();
          // count dups (to reduce excess allocations)
          //   deDups done using the comparatorWithID
          final boolean useEq = comparatorWithID != comparatorWithoutID;  // true for Sorted, false for set
          for (int i = batchSize - 2; i >= 0; i--) {
            TOP item = batch.get(i);
            if (useEq ? (item == prev) : (comparatorWithID.compare(item, prev) == 0)) {
              batch.set(i + 1, null); // need to do this way so the order of adding is the same as v2
              if (i + 1 == batchSize - 1) {
                batchSize --;  // start with non-null when done
              }
            } else {
              prev = item;
              nbrNewSlots++;  // count of items that will actually be added; skips the duplicates
            }
          }
        } 
        
        int i_batch = batchSize - 1;
        int insertPosOfAddedSpace = 0;
        TOP itemToAdd;
        // skip entries already found
        itemToAdd = batch.get(i_batch);
        while (itemToAdd == null || (insertPosOfAddedSpace = find(itemToAdd)) >= 0) {
          // skip any entries at end of list if they're already in the set
          i_batch--;
          nbrNewSlots --;
          if (i_batch < 0) {
            batch.clear();
            return; // all were already in the index
          }
          itemToAdd = batch.get(i_batch);
        }
        
//        assert nbrNewSlots > 0; // otherwise batch would be empty and would have returned before
        
        // insertPosOfAddedSpace is index to non-null item that is > itemToAdd
        //                       or points to 1 beyond current size
        insertPosOfAddedSpace = (- insertPosOfAddedSpace) - 1;
        // insertPos is insert point, i_batch is index of first batch element to insert
        // there may be other elements in batch that duplicate; these won't be inserted, but 
        //   there will be space lost in this case
         
        int indexOfNewItem = insertSpace(insertPosOfAddedSpace, nbrNewSlots) // returns index of a non-null item
                                                                           // the new item goes one spot to the left of this
            - 1;  // inserts nulls at the insert point, shifting other cells down
        assert nbrNewSlots == nullBlockEnd - nullBlockStart;

        int nbrNewSlotsRemaining = nbrNewSlots;  // will be decremented as slots are used
        // process first item
        if (indexOfNewItem >= nullBlockStart) {
          nbrNewSlotsRemaining --;
        } // else, don't decr because we're using existing nulls
        //debug
//        assert (nbrNewSlotsRemaining > 0) ? indexOfNewItem != nullBlockStart : true;
//        assert (nbrNewSlotsRemaining > 0) ? nullBlockEnd - 1 > nullBlockStart : true;
        insertItem(indexOfNewItem, itemToAdd);
//        TOP prevItem = itemToAdd;
        if (indexOfNewItem + 1 == a_nextFreeslot) {
          highest = itemToAdd;
        }
        
        
        //debug
//        assert (nbrNewSlotsRemaining > 0) ? nullBlockStart != -1 : true;
        
        int bPos = i_batch - 1; // next after first one from end
        for (; bPos >= 0; bPos --) {
          itemToAdd = batch.get(bPos);
          if (null == itemToAdd) {
            continue;  // skipping a duplicate
          }
          int pos = findRemaining(itemToAdd); // search limited, ends at nullBlockstart
    
          if (pos >= 0) {
            continue;  // already in the list
          }
          pos = (-pos) - 1;  // pos is the insert point 
                             // new item goes 1 to left of this
          assert a[pos] != null;
          
          indexOfNewItem = pos - 1;  // where the new item goes, 1 to left of insert point
          if (nullBlockStart == 0) {
            // this and all the rest of the elements are lower, insert in bulk
            // because all are lower, none are in the array, so don't need the compare check
            insertItem(indexOfNewItem--, itemToAdd);
            nbrNewSlotsRemaining --;
            bPos--;
            
            for (;bPos >= 0; bPos--) {          
              itemToAdd = batch.get(bPos);
              if (itemToAdd == null) {
                continue;
              }
              insertItem(indexOfNewItem--, itemToAdd);
              nbrNewSlotsRemaining --;  // do this way to respect skipped items due to == to prev        
            }
            break;
          }
//          validateA();
//          boolean debugdidshift = false;
          if (indexOfNewItem == -1 || null != a[indexOfNewItem]) {
//            debugdidshift = true;
            indexOfNewItem = shiftFreespaceDown(pos, nbrNewSlotsRemaining) - 1;  // results in null being available at pos - 1         
            assert nbrNewSlotsRemaining == nullBlockEnd - nullBlockStart;
            nbrNewSlotsRemaining --;  // only decr if using a new slot, skip if filling in a null
          } else {
            // there was a null in the spot to insert
            // two cases: if the spot is within the nullBlock, need to decr nbrNewSlots
            if (indexOfNewItem < nullBlockEnd && indexOfNewItem >= nullBlockStart) {
              nbrNewSlotsRemaining --;  // the insertItem will adjust nullBlock start/end
            }
          }
//          //debug
//          assert (nbrNewSlotsRemaining > 0) ? nullBlockStart != -1 : true;
          insertItem(indexOfNewItem, itemToAdd);
//          //debug
//          assert nbrNewSlotsRemaining == nullBlockEnd - nullBlockStart;
//          assert (nbrNewSlotsRemaining > 0) ? nullBlockStart != -1 : true;

        }
        if (nbrNewSlotsRemaining > 0) {
          // have extra space left over due to dups not being added
          // If this space is not at beginning, move space to beginning or end (whichever is closer)
//          if (indexOfNewItem - nbrNewSlotsRemaining > 0) { 
          if (nullBlockEnd != a_firstUsedslot) {
          // space is not at beginning
          
            assert nbrNewSlotsRemaining == nullBlockEnd - nullBlockStart;
            int nullBlockEnd_end = a_nextFreeslot - nullBlockEnd;
            int nullBlockStart_start = nullBlockStart - a_firstUsedslot;
            assert nullBlockEnd_end > 0;
            assert nullBlockStart_start > 0;

            if (nullBlockStart_start <= nullBlockEnd_end) {
              shiftFreespaceDown(a_firstUsedslot, nbrNewSlotsRemaining);
//              System.arraycopy(a, indexOfNewItem - nbrNewSlots, a, 0, nbrNewSlots);
//              a_firstUsedslot += nbrNewSlots;
//              validateA();
            } else {
              shiftFreespaceUp(a_nextFreeslot, nbrNewSlotsRemaining);
              a_nextFreeslot -= nbrNewSlotsRemaining;
//              // move to end
//              System.arraycopy(a, indexOfNewItem, a, indexOfNewItem - nbrNewSlots, a_nextFreeslot - indexOfNewItem);
//              Arrays.fill(a, a_nextFreeslot - nbrNewSlots, a_nextFreeslot, null);
//              a_nextFreeslot -= nbrNewSlots;
//              validateA();
            }
          }
        }
        nullBlockStart = nullBlockEnd = -1;
//        validateA();
        batch.clear();
      } finally {
        doingBatchAdds = false;
//        //debug
//        assert (lastRemovedPos != -1) ? a[lastRemovedPos] == null : true;

      }

     }
    
    
  }
  
  /**
   * side effects:
   *   increment size
   *   reset a_firstUsedslot if adding in front
   *   ( a_nextFreeslot not updated, because this method only called to inserting before end )
   *   nullBlockEnd reduced conditionally
   *   lastRemovedPos is reset if that position is used
   * @param indexToUpdate - the index in the array to update with the item to add
   * @param itemToAdd -
   */
  private void insertItem(int indexToUpdate, TOP itemToAdd) {
//    validateA();
    try {
      assert indexToUpdate >= 0;
      assert null == a[indexToUpdate];
    } catch (AssertionError e) {
      if (TRACE) {
        System.err.println("OrderedFsSet_array caught assert.  array values around indexToUpdate: ");
        for (int i = indexToUpdate - 2; i < indexToUpdate + 3; i++) {
          if (i >= 0 && i < a.length) {
            System.err.format("a[%,d]: %s%n", i, a[i].toString(2));
          } else {
            System.err.format("a[%,d}: out-of-range%n", i);
          }
        }
        System.err.format("trace info: %n%s", tr);
      }
      throw e;
    }
 
    a[indexToUpdate] = itemToAdd;
    if (indexToUpdate == lastRemovedPos) {
      lastRemovedPos = -1;  // used up a last removed position
    }
    incrSize();
    if (indexToUpdate < a_firstUsedslot) {
      a_firstUsedslot = indexToUpdate;  
    }
    if (nullBlockEnd == indexToUpdate + 1) {
      nullBlockEnd --;
      if (nullBlockStart == nullBlockEnd) {
        nullBlockStart = nullBlockEnd = -1;
      }
    }
    if (nullBlockStart == indexToUpdate) {
      nullBlockStart = nullBlockEnd = -1;
    }
//    validateA();
  }

  /**
   * This is called when inserting new items from the batch.
   * It does a bulk insert of space for all the items in the batch.
   * 
   * Attempts to move a small amount with a multi-part strategy:
   *   - make use of existing "nulls" at the insert spot
   *     -- if not enough,
   *       --- if just need one more, compute distance from 3 possible source:
   *            -- front, end, and lastRemovedPos (if not already included in existing "nulls")   
   *     combine with a new additional block that is moved down from the top.
   *   - make use of both beginning and end free space.
   * 
   * If there is already a "null" at the insert spot, use that space.
   *   - if there are enough nulls, return 
   *   
   * Sets (as side effect) nullBlockStart and nullBlockEnd
   *   The setting includes all of the nulls, both what might have been present at the 
   *   insert spot and any added new ones.
   *      nullBlockStart refs a null, 
   *      nullBlockEnd refs a non-null (or null if things are being inserted at the end) position
   *        - the insert position
   * 
   * @param positionToInsert position containing a value, to free up by moving the current free block
   *                         so that the last free element is at that (adjusted up) position.          
   * @param nbrNewSlots
   * @return adjusted positionToInsert, the free spot is just to the left of this position
   */
  private int insertSpace(final int positionToInsert, final int origNbrNewSlots) {
    if (TRACE) {
      tr.setLength(0);
      tr.append("Tracing OrderedFsSet_array\n");
      tr.append(String.format("insertSpace called with positionToInsert: %,d nbrNewSlots: %,d%n", positionToInsert, origNbrNewSlots));
    }
         
    // while the positionToInsert (a ref to non-null or 1 past end) 
    //   is > 0 && the pos to the left is null,
    //     reduce the nbrNewSlots
    int i = positionToInsert;
    int nullsBelowInsertMin = i;
    int nbrNewSlotsNeeded = origNbrNewSlots;
   
    /***********************************
     * count nulls already present     *
     * reduce nbrNewSlotsNeeded        *
     *   reset lastRemovedPos if using *
     ***********************************/
    while (i > 0 && a[i - 1] == null && nbrNewSlotsNeeded > 0) {
      i--;
      nbrNewSlotsNeeded--;
      nullsBelowInsertMin = i;
      if (i == lastRemovedPos) {
        lastRemovedPos = -1;  // subsumed by this calc
      }
    }
    
    int r = positionToInsert;
    
    /***********************************
     * Finish if nulls already found   *
     * for all new slots               *
     ***********************************/
 
    if (nbrNewSlotsNeeded != 0) {
      
      /***********************************
       * Compute closest space           *
       ***********************************/
      
//      //debug
//      itercount ++;
    
      int distanceFromLastRemoved = (lastRemovedPos == -1 || nbrNewSlotsNeeded != 1) 
                                      ? Integer.MAX_VALUE // skip using this
                                      : (positionToInsert - lastRemovedPos);
      int distanceFromEnd = a_nextFreeslot - positionToInsert;
      int distanceFromFront = (a_firstUsedslot < nbrNewSlotsNeeded)
                                ? Integer.MAX_VALUE
                                : positionToInsert - a_firstUsedslot;

      boolean useFront =
  //          // make sure size of front free space is not included in previous nulls already counted
  //          a_firstUsedslot > positionToInsert && 
          distanceFromFront < distanceFromEnd;
      boolean useLastRemoved = (Math.abs(distanceFromLastRemoved) < (useFront 
                                                                      ? distanceFromFront 
                                                                      : distanceFromEnd));

      if (!useLastRemoved && !useFront) {
        // using back, but reevaluate if would need to expand
        if (a.length < a_nextFreeslot + nbrNewSlotsNeeded) {
          // if use back space, a will need to expand;
          // use front space if available
          useFront = nbrNewSlotsNeeded <= a_firstUsedslot;
//          //debug
//          System.out.format("debug insertSpace, maybe overriding use front, space needed = %4d, space avail = %d%n",
//              nbrNewSlotsNeeded, a_firstUsedslot);
        }
      }
      if (TRACE) 
        tr.append(String.format("distances: %d %d %d, useFront: %s useLastRemoved: %s%n",
            distanceFromLastRemoved, distanceFromEnd, distanceFromFront, useFront, useLastRemoved));

//      // debug
//      if (itercount % 128 == 0) {
//        System.out.format("debug insertSpace: %4d distances: %10d %4d %10d, useFront: %5s useLastRemoved: %s%n",
//            itercount, distanceFromLastRemoved, distanceFromEnd, distanceFromFront, useFront, useLastRemoved);
//      }
//      //debug
//      if (itercount % 128 == 0 && itercount > 140000) {
//        System.out.format("debug insertSpace: space in front: %,5d space at end: %d%n",
//             a_firstUsedslot, a.length - a_nextFreeslot);
//      }
      
      if (useLastRemoved) {  // due to find skipping over nulls, the distanceFromLastRemoved is never 0
        nullBlockStart = lastRemovedPos;
        nullBlockEnd = lastRemovedPos + 1;
        
        if (distanceFromLastRemoved > 0) {
          assert distanceFromLastRemoved != 1; 
          shiftFreespaceUp(nullsBelowInsertMin, nbrNewSlotsNeeded); // move one slot (since nullblockstart/end set above        
        } else {
          r = shiftFreespaceDown(positionToInsert, nbrNewSlotsNeeded);
          if (TRACE) 
            tr.append(String.format("shiftFreespaceDown result was %,d%n", r));
        }
        lastRemovedPos = -1;
      } else if (useFront) {
        nullBlockStart = a_firstUsedslot - nbrNewSlotsNeeded;
        nullBlockEnd = a_firstUsedslot;
  //        if (null != a[nullBlockStart]) {
        if (a_firstUsedslot != positionToInsert) {
          // need to move the free slot if not already next to the insert position
          shiftFreespaceUp(positionToInsert, nbrNewSlotsNeeded);
        }
  //        a_firstUsedslot --;  // not done here, done in insert routine
      } else {
        // using space at end
        ensureCapacity(nbrNewSlotsNeeded);
        nullBlockStart = a_nextFreeslot;
        nullBlockEnd = nullBlockStart + nbrNewSlotsNeeded; 
        r = shiftFreespaceDown(positionToInsert, nbrNewSlotsNeeded);
        a_nextFreeslot += nbrNewSlotsNeeded;  // due to shift just done in line above
        if (TRACE) {
          tr.append(String.format("shiftFreespaceDown2 result was %,d, nullBlockStart: %,d nullBlockEnd: %,d a_nextFreeslot: %,d%n", 
              r, nullBlockStart, nullBlockEnd, a_nextFreeslot));
        }
        //reset null block to full size
      }
    } else {
//      //debug
//      System.out.format("debug insertSpace: using existing nulls, start: %,6d length: %,d%n", r - origNbrNewSlots, origNbrNewSlots);
    }
    nullBlockEnd = r;
    nullBlockStart = r - origNbrNewSlots;
//    // debug
//    for (int ii = nullBlockStart; ii < nullBlockEnd; ii++) {
//      assert a[ii] == null;
//    }
    return r;   
  }
  

  /**
   * Shift a block of free space lower in the array.
   * This is done by shifting the space at the insert point
   *   for length = start of free block - insert point 
   *   to the right by the nbrNewSlots
   *   and then resetting (filling) the freed up space with null
   *   
   * Example:  u = used, f = free space
   * 
   * before                      |--| 
   * uuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuffffffffuuuuuuu
   *                             ^ insert point
   * after                               |--|
   * uuuuuuuuuuuuuuuuuuuuuuuuuuuuffffffffuuuuuuuuuuu
   *                                     ^ insert point
   *                                    
   * before 
   * |------------------------------|
   * uuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuffffffffuuuuuuu
   * ^ insert point
   * after   |------------------------------| 
   * ffffffffuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu
   *         ^ insert point
   * before 
   *     |------------------------------|
   * ffffuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuffffffffuuuuuuu
   *     ^ insert point
   * after       |------------------------------| 
   * ffffffffffffuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu
   *             ^ insert point
   *                                    
   * move up by nbrNewSlots
   * length to move = nullBlockStart - insert point
   * new insert point is nbrOfFreeSlots higher (this points to a filled spot, prev spot is free)
   * 
   * fill goes from original newInsertPoint, for min(nbrNewSlots, length of move)
   *   
   * There may be nulls already at the insert point, or encountered along the way.
   *   - nulls along the way are kept, unchanged
   *   - nulls at the insert point are incorporated; the freespace added is combined (need to verify)
   *   
   * hidden param:  nullBlockStart
   * side effect: lastRemovedPosition maybe updated
   * @param insertPoint index of slot array, currently occupied, where an item is to be set into
   * @param nbrNewSlots - the size of the inserted space
   * @return the updated insert point, now moved up
   */
  private int shiftFreespaceDown(final int insertPoint, final int nbrNewSlots) {
    assert insertPoint >= 0;
    assert nbrNewSlots >= 0;
    int lengthToMove = nullBlockStart - insertPoint;

    try {
      // adjust lastRemovedPos if in moving part
      if (lastRemovedPos >= insertPoint && lastRemovedPos < (insertPoint + lengthToMove)) {
        lastRemovedPos = lastRemovedPos + nbrNewSlots;
      }
      System.arraycopy(a, insertPoint, a, insertPoint + nbrNewSlots, lengthToMove);
    } catch (ArrayIndexOutOfBoundsException e) {
      System.err.println("Internal error: OrderedFsSet_sorted got array out of bounds in shiftFreeSpaceDown " + e.toString());
      System.err.format("  array size: %,d insertPoint: %,d nbrNewSlots: %,d lengthToMove: %d%n",
          a.length, insertPoint, nbrNewSlots, lengthToMove);  // 32, 0, 1, -1 implies: nullBlockStart = -1
      throw e;
    }
    int lengthToClear = Math.min(nbrNewSlots, lengthToMove);
    Arrays.fill(a, insertPoint, insertPoint + lengthToClear, null);
    nullBlockStart = insertPoint;
    nullBlockEnd = nullBlockStart + nbrNewSlots;
    
    // adjust nullBlockStart to account for nulls in front
    int i = insertPoint - 1;
    for (; i >= 0; i--) {
      if (a[i] != null) {
        break;
      }
    }
    nullBlockStart = i + 1;
    
    if (MEASURE) {
      moveSizeHistogram[32 - Integer.numberOfLeadingZeros(lengthToMove)] ++;
      movePctHistogram[lengthToMove* 10 / (a_nextFreeslot - a_firstUsedslot)] ++;
      fillHistogram[32 - Integer.numberOfLeadingZeros(lengthToClear)] ++;
    }
    if (insertPoint == a_firstUsedslot) {
      a_firstUsedslot = insertPoint + nbrNewSlots;
    }
    return insertPoint + nbrNewSlots;
  }
  
  /**
   * Shift a block of free space higher in the array.
   * This is done by shifting the space at the insert point
   *   of length = insert point - (end+1) of free block 
   *   to the left by the nbrNewSlots
   *   and then resetting (filling) the freed up space with null
   *   
   * Example:  u = used, f = free space
   * 
   * before              |-|   << block shifted 
   * uuuuuuuuuuuuuuufffffuuuuuuuuuuuuuuuuuuuuuuuuuuu
   *                        ^ insert point
   * after          |-|   << block shifted
   * uuuuuuuuuuuuuuuuuufffffuuuuuuuuuuuuuuuuuuu
   *                        ^ insert point
   *                                    
   * before                                  |----|
   * uuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuffffuuuuuuu
   *                                               ^ insert point
   *     note: insert point is never beyond last because
   *     those are added immediately
   * after                               |----|
   * uuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuffffu
   *                                               ^ insert point
   *                                    
   * before       |--|   
   * uuuuuuuuuuuufuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu
   *                  ^ insert point
   * after       |--|
   * uuuuuuuuuuuuuuuufuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu
   *                  ^ insert point
   *                                    
   *     |--------|  before 
   * ffffuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu
   *               ^ insert point
   * |--------|
   * uuuuuuuuuuffffuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu
   *               ^ insert point
   *                                    
   *                                    
   * move down by nbrNewSlots
   * length to move = insert point - null block end (which is 1 plus index of last free)
   * new insert point is the same as the old one (this points to a filled spot, prev spot is free)
   * 
   * fill goes from original null block end, for min(nbrNewSlots, length of move)
   *   
   * hidden param:  nullBlock Start, nullBlockEnd = 1 past end of last free slot
   * @param newInsertPoint index of slot array, currently occupied, where an item is to be set into
   * @param nbrNewSlots - the size of the inserted space
   * @return the updated insert point, now moved up
   */
  
  private int shiftFreespaceUp(int newInsertPoint, int nbrNewSlots) {
    boolean need2setFirstUsedslot = nullBlockEnd == a_firstUsedslot;
    int lengthToMove = newInsertPoint - nullBlockEnd;
   
    // adjust lastRemovedPos if in moving part
    if (lastRemovedPos >= nullBlockEnd && lastRemovedPos < (nullBlockEnd + lengthToMove)) {
      lastRemovedPos = lastRemovedPos - nbrNewSlots;
    }
    
    System.arraycopy(a, nullBlockEnd, a, nullBlockStart, lengthToMove);
    int lengthToClear = Math.min(nbrNewSlots, lengthToMove);
    Arrays.fill(a, newInsertPoint - lengthToClear, newInsertPoint, null);
    nullBlockStart = newInsertPoint - nbrNewSlots;
    nullBlockEnd = newInsertPoint;
    if (need2setFirstUsedslot) {
      a_firstUsedslot = 0;
    }
    return newInsertPoint;
  }
    
//  /**
//   * @param from start of items to shift, inclusive
//   * @param to end of items to shift, exclusive
//   */
//  private void shiftBy2(int from, int to) {
//    if (to == -1) {
//      to = theArray.size();
//      theArray.add(null);
//      theArray.add(null);
//    }
//      try {
//        Object[] aa = (Object[]) getActualArray.invokeExact(theArray);
//        System.arraycopy(aa, from, aa, from + 2, to - from);
//      } catch (Throwable e) {
//        throw new RuntimeException(e);
//      }
//  }

  /**
   * Never returns an index to a "null" (deleted) item.
   * If all items are LT key, returns - size - 1 
   * @param fs the key
   * @return the lowest position whose item is equal to or greater than fs;
   *         if not equal, the item's position is returned as -insertionPoint - 1. 
   *         If the key is greater than all elements, return -size - 1). 
   */
  private int find(TOP fs) {
    if (size == 0) {
      return -1;
    }
    return binarySearch(fs);
  }
  
  /**
   * find, within constricted range: start: a_firstUsedslot, end = nullBlockStart
   * @param fs -
   * @return - the slot matching, or the one just above, if none match,
   *           but limited to the nullBlockStart position.
   *           If the answer is not found, and the insert position is
   *           the nullBlockStart, then return the nullBlockEnd as the position
   *           (using the not-found encoding).
   */
  private int findRemaining(TOP fs) {
    int pos = binarySearch(fs, a_firstUsedslot, nullBlockStart, a, nullBlockStart, nullBlockEnd, comparatorWithID);
    return pos < 0 && ((-pos) - 1 == nullBlockStart) 
            ? ( -(nullBlockEnd) - 1) 
            : pos;
  }
    
  /**
   * Special version of binary search that ignores null values
   * @param fs the value to look for
   * @return the position whose non-null value is equal to fs, or is gt fs (in which case, return (-pos) - 1)
   */
  private int binarySearch(final TOP fs) {
    return binarySearch(fs, a_firstUsedslot, a_nextFreeslot, a, nullBlockStart, nullBlockEnd, comparatorWithID);
  }
  
  /**
   * At the start, the start and end positions are guaranteed to refer to non-null entries
   * But during operation, lower may refer to "null" entries (upper always non-null)
   * 
   * @param fs - the fs to search for
   * @param start the index representing the lower bound (inclusive) to search for
   * @param end the index representing the upper bound (exclusive) to search for
   * @param _a the array
   * @param _nullBlockStart inclusive
   * @param _nullBlockEnd exclusive
   * @param _comparatorWithID -
   * @return - the index of the found item, or if not found, the (-index) -1 of the 
   *           poosition one more than where the item would go
   */
  public static int binarySearch(final TOP fs, int start, int end,
      TOP[] _a, 
      int _nullBlockStart,
      int _nullBlockEnd,
      Comparator<TOP> _comparatorWithID) {

    if (start < 0 || end - start <= 0) {
      return (start < 0) ? -1 : ( (-start) - 1);  // means not found, insert at position start
    }
    int lower = start, upper = end;
    for (;;) {
    
      int mid = (lower + upper) >>> 1;  // overflow aware
      TOP item = _a[mid];
      int delta = 0;
      int midup = mid;
      int middwn = mid;
      int pos = mid;
    
      while (null == item) {  // skip over nulls
        
        /**
         * lower (inclusive) may point to null,
         * upper (exclusive) guaranteed to not point to a null
         * 
         * the mid position is point to a null; 
         *   We split the mid into two items: midup and middown.
         *     - both may point to a non-null item eventually
         *     - the one that gets to a non-null first is used, unless:
         *       -- it is == to the upper, in which case we attempt to find the
         *          middown non-null.
         *       -- it is below the lower (only happens if the lower is ref-ing a null), in which case
         *          we attempt to find the midup non-null
         *       -- if both the midup == upper and middown < lower, then 
         *            not found, return (-upper) -1;
         *            
         *   This may be inside a null block - in which case
         *     shortcut: speed the midup and middown to the edges (1st non-null positions)
         */
        
        
        if (_nullBlockStart != -1 && 
            middwn >= _nullBlockStart && 
            midup  < _nullBlockEnd) {
          // in the null block
          // move to edges
          midup  = _nullBlockEnd;   // midup exclusive, nullBlockEnd exclusive
          middwn = _nullBlockStart - 1; // middwn and nullBlockStart inclusive
        } else {
          delta ++;
        }
        
        // belowUpper == true means there's an item available to compare, at the midup + delta point, which is < upper.
        //       is < because upper is exclusive
        boolean belowUpper = (pos = midup + delta) < upper;
        if (belowUpper && null != (item = _a[pos])) {
          break;  // have a non-null candidate, below the upper, to compare
        }
        // belowLower == true means we've gone past the last place to compare with, below.
        // if belowLower == false, then there's an item available to compare, at the middwn - delta point, which is >= lower
        boolean belowLower = (pos = middwn - delta) < lower; 
        if (!belowLower && null != (item = _a[pos])) { 
          break;  // have a non-null candidate, = or above the lower, to compare
        }
        
        if (! belowUpper && belowLower) {
          return (-upper) - 1; // return previous
        }
      }
     
      int c = _comparatorWithID .compare(fs, item);
      if (c == 0) {
        return pos;
      }
      
      if (c < 0) {  // fs is smaller than item at pos in array; search downwards
        upper = pos;  // upper is exclusive
        if (upper == lower) {
          return (-upper) - 1;
        }
      } else {  // fs is larger than item at pos in array; search upwards
        lower = pos + 1;             // lower is inclusive
        if (lower == upper) {
          return (-upper) - 1;
        }
      }
    }
  }
  
  /**
   * @see Set#remove(Object)
   */
  @Override
  public boolean remove(Object o) {
    if (o == null) {
      throw new IllegalArgumentException("Null cannot be the argument to remove");
    }
    processBatch();
    TOP fs = (TOP) o;
    
    int pos = find(fs);
    if (pos < 0) {
      return false;
    }
    
    // at this point, pos points to a spot that compares "equal" using the comparator
    // for sets, this is the single item that is in the index
    // for sorted, because find uses the compareWithID comparator, this is the unique equal element
    assert a[pos] != null;
    a[pos] = null;
    size --;
    modificationCount ++;
    if (size == 0) {
      clearResets();  // also clears last removed pos
    } else {
      // size is > 0
      if (pos == a_firstUsedslot) {
        do {  // removed the first used slot
          a_firstUsedslot ++;
        } while (a[a_firstUsedslot] == null);
      } else if (pos == a_nextFreeslot - 1) {
        do {
          a_nextFreeslot --;
        } while (a[a_nextFreeslot - 1] == null);
        highest = a[a_nextFreeslot - 1];
      } 
      
      if (size < ((a_nextFreeslot - a_firstUsedslot) >> 1) &&
          size > 8) {
        compressOutRemoves();  // also clears lastRemovedPos
      } else {
        // update lastRemovedPos
        lastRemovedPos = (pos > a_firstUsedslot && pos < (a_nextFreeslot - 1))
                           ? pos  // is a valid position 
                           : -1;  // is not a valid position
      }
      
      // non-empty case: capacity shrinking: do when
      //   capacity > 64  (skip for 64 or less)
      //   space from a_nextFreeSlot to (capacity >> 2) + a_firstUsedslot > 32  
      //   time since last add > 5 seconds  not done - test might be too expensive
      
      //compute space + buffer (another power of 2) to save from both front and back.  Back part might be negative
      int spaceToSave = a_firstUsedslot + (a.length >> 2) - a_nextFreeslot;
      if (spaceToSave > 32 
//          && System.currentTimeMillis() - lastAddTime > 5000 // avoid as test might be more expensive than copy
          ) {
        
        // compute actual space available at each end to save, without extra buffer
        // space to save at beginning is just a_firstUsedslot
        // space in front is 0 or positive, space at end may be negative.
        int spaceAtEnd = (a.length >> 1) - a_nextFreeslot;
        
        // divide space between front and back, 1/2 and 1/2
        
        int totalSpaceToSave = spaceAtEnd + a_firstUsedslot;

        int spaceToHaveAtFront = totalSpaceToSave >> 1;
        
        int spaceToReclaimAtFront = Math.max(0,  a_firstUsedslot - spaceToHaveAtFront);
        
//        System.out.format("debug shrinking, a_firstUsedslot: %d, spaceToReclaimAtFront: %d,"
//            + " spaceAtEnd: %d%n", a_firstUsedslot, spaceToReclaimAtFront, spaceAtEnd);
        
        a = Arrays.copyOfRange(a, spaceToReclaimAtFront, spaceToReclaimAtFront + (a.length >> 1));

        a_firstUsedslot -= spaceToReclaimAtFront;
        a_nextFreeslot -= spaceToReclaimAtFront;
        if (lastRemovedPos != -1) {
          assert lastRemovedPos > spaceToReclaimAtFront;
          lastRemovedPos -= spaceToReclaimAtFront;
        }
        
//        System.out.println("debug space in front: " + a_firstUsedslot);
        
      }

    }
    return true;
  }
  
  /**
   * When the main array between the first used slot and the next free slot has too many nulls 
   * representing removed items, scan and gc them.
   *   assumes: first used slot is not null, nextFreeslot - 1 is not null
   */
  private void compressOutRemoves() {
    int j = a_firstUsedslot + 1; // outside of for loop because need value of j after loop ends
    for (int i = a_firstUsedslot + 1; i < a_nextFreeslot; i++, j++) {
      while (a[i] == null) {
        i ++;
      }
      if (i > j) {
        a[j] = a[i];
      }
    }
    
    Arrays.fill(a, j, a_nextFreeslot, null); // j is one past last filled slot
    a_nextFreeslot = j;
    lastRemovedPos = -1;
  }
  
  /**
   * @see Set#containsAll(Collection)
   */
  @Override
  public boolean containsAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  /**
   * @see Set#addAll(Collection)
   */
  @Override
  public boolean addAll(Collection<? extends TOP> c) {
    boolean changed = false;
    for (TOP item : c) {
      changed |= add(item);
    }
    return changed;
  }
  
  /**
   * @see Set#retainAll(Collection)
   */
  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  /**
   * @see Set#removeAll(Collection)
   */
  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }
  
  /**
   * @see Set#clear()
   */
  @Override
  public void clear() {
    if (isEmpty()) {
      return;
    }
    if (!shrinkCapacity()) {
//      //debug 
//      if (a_firstUsedslot == -1) {
//        System.out.println("a_firstUsedslot was -1");
//      }
//      if (a_nextFreeslot == -1) {
//        System.out.println("a_nextFreeslot was -1");
//      }
      Arrays.fill(a, a_firstUsedslot, a_nextFreeslot, null);      
    }
    clearResets();
  }
  
  private void clearResets() {
    a_firstUsedslot = 0;
    a_nextFreeslot = 0;
    batch.clear();
    size = 0;
    maxSize = 0;
    nullBlockStart = -1;
    nullBlockEnd = -1;
    doingBatchAdds = false; // just for safety, not logically needed I think.
    highest = null;    
    modificationCount ++;
    lastRemovedPos = -1;
  }

  /**
   * @see NavigableSet#lower(Object)
   */
  @Override
  public TOP lower(TOP fs) {
    int pos = lowerPos(fs);
    return (pos < 0) ? null : a[pos];
  }
  
  /**
   * @param fs element to test
   * @return pos of greatest element less that fs or -1 if no such
   */
  public int lowerPos(TOP fs) {
    processBatch();
    int pos = find(fs); // position of lowest item GE fs  
    pos = (pos < 0) ? ((-pos) - 2) : (pos - 1);
    // above line subtracts 1 from LE pos; pos is now lt, may be -1
    while (pos >= a_firstUsedslot) {
      if (a[pos] != null) {
        return pos;
      }
      pos --;
    }
    return -1; 
  }


  /**
   * @see NavigableSet#floor(Object)
   */
  @Override
  public TOP floor(TOP fs) {
    int pos = floorPos(fs);
    return (pos < 0) ? null : a[pos];
  }
  
  /**
   * @param fs -
   * @return -
   */
  public int floorPos(TOP fs) {
    processBatch();
    int pos = find(fs);  // position of lowest item GE fs
    if (pos < 0){
      pos = (-pos) - 2;
    }
    // pos is = or lt, may be -1
    while (pos >= a_firstUsedslot) {
      if (a[pos] != null) {
        return pos;
      }
      pos --;
    }
    return -1;
  }

  /**
   * @see NavigableSet#ceiling(Object)
   */
  @Override
  public TOP ceiling(TOP fs) {
    int pos = ceilingPos(fs);
    return (pos < a_nextFreeslot) ? a[pos] : null;
  }
  

  /**
   * @param fs -
   * @return -
   */
  public int ceilingPos(TOP fs) {
    processBatch();
    int pos = find(fs); // position of lowest item GE fs
    if (pos < 0){
      pos = (-pos) -1;
    } else {
      return pos;
    }
    
    while (pos < a_nextFreeslot) {
      if (a[pos] != null) {
        return pos;
      }
      pos ++;
    }
    return pos;
  }

  /**
   * @see NavigableSet#higher(Object)
   */
  @Override
  public TOP higher(TOP fs) {
    int pos = higherPos(fs);
    return (pos < a_nextFreeslot) ? a[pos] : null;
  }

  /**
   * @param fs the Feature Structure to use for positioning
   * @return the position that's higher
   */
  public int higherPos(TOP fs) {
    processBatch();
    int pos = find(fs); // position of lowest item GE fs
    pos = (pos < 0) ? ((-pos) -1) : (pos + 1);
    
    while (pos < a_nextFreeslot) {
      if (a[pos] != null) {
        return pos;
      }
      pos ++;
    }
    return pos;
  }

  /**
   * @see NavigableSet#pollFirst()
   */
  @Override
  public TOP pollFirst() {
    throw new UnsupportedOperationException();
  }

  /**
   * @see NavigableSet#pollLast()
   */
  @Override
  public TOP pollLast() {
    throw new UnsupportedOperationException();
  }

  /**
   * @see Iterable#iterator()
   */
  @Override
  public Iterator<TOP> iterator() {
    processBatch();
    if (a_nextFreeslot == 0) {
      return Collections.emptyIterator();
    }
    return new Iterator<TOP>() {
      private int pos = a_firstUsedslot;
      { incrToSkipOverNulls(); 
        if (MEASURE) {
          int s = a_nextFreeslot - a_firstUsedslot;
          iterPctEmptySkip[(s - size()) * 10 / s] ++;
        }
      }
       
      @Override
      public boolean hasNext() {
        processBatch();
        return pos < a_nextFreeslot;
      }
      
      @Override
      public TOP next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }

        TOP r = a[pos++];
        incrToSkipOverNulls();
        return r;        
      }
      
      private void incrToSkipOverNulls() {
        while (pos < a_nextFreeslot) {
          if (a[pos] != null) {
            break;
          }
          pos ++;
        }
      }
    };
  }

//  /**
//   * Directly implement FSIterator
//   *   for GC efficiency
//   * @return low level iterator
//   */
//  public <T extends FeatureStructure> LowLevelIterator<T> ll_Iterator(LowLevelIndex ll_index, CopyOnWriteIndexPart cow_wrapper) {
//    processBatch();
//    return new LL_Iterator<T>(ll_index, cow_wrapper);
//  }

  /**
   * @see NavigableSet#descendingSet()
   */
  @Override
  public NavigableSet<TOP> descendingSet() {
    throw new UnsupportedOperationException();
  }

  /**
   * @see NavigableSet#descendingIterator()
   */
  @Override
  public Iterator<TOP> descendingIterator() {
    processBatch();
    return new Iterator<TOP>() {
      private int pos = a_nextFreeslot - 1;    // 2 slots:  next free = 2, first slot = 0
                                               // 1 slot:   next free = 1, first slot = 0
                                               // 0 slots:  next free = 0; first slot = 0 (not -1)
      { if (pos >= 0) {  // pos is -1 if set is empty
        decrToNext(); 
        }
      }
       
      @Override
      public boolean hasNext() {
        return pos >= a_firstUsedslot;
      }
      
      @Override
      public TOP next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }

        TOP r = a[pos--];
        decrToNext();
        return r;        
      }
      
      private void decrToNext() {
        while (pos >= a_firstUsedslot) {
          if (a[pos] != null) {
            break;
          }
          pos --;
        }
      }
    };
  }

  /**
   * @see NavigableSet#subSet(Object, boolean, Object, boolean)
   */
  @Override
  public NavigableSet<TOP> subSet(TOP fromElement, boolean fromInclusive, TOP toElement, boolean toInclusive) {
    return new SubSet(() -> this, fromElement, fromInclusive, toElement, toInclusive, false, null);
  }

  /**
   * @see NavigableSet#headSet(Object, boolean)
   */
  @Override
  public NavigableSet<TOP> headSet(TOP toElement, boolean inclusive) {
    if (isEmpty()) {
      return this; 
    }
    return subSet(first(), true, toElement, inclusive);     
  }

  /**
   * @see NavigableSet#tailSet(Object, boolean)
   */  
  @Override
  public NavigableSet<TOP> tailSet(TOP fromElement, boolean inclusive) {
    if (isEmpty()) {
      return this;
    }
    return subSet(fromElement, inclusive, last(), true);
  }

  /**
   * @see NavigableSet#subSet(Object, Object)
   */
  @Override
  public SortedSet<TOP> subSet(TOP fromElement, TOP toElement) {
    return subSet(fromElement, true, toElement, false);
  }

  /**
   * @see NavigableSet#headSet(Object)
   */
  @Override
  public SortedSet<TOP> headSet(TOP toElement) {
    return headSet(toElement, false);
  }

  /**
   * @see NavigableSet#tailSet(Object)
   */
  @Override
  public SortedSet<TOP> tailSet(TOP fromElement) {
    return tailSet(fromElement, true);
  }
  
  
  /**
   * This is used in a particular manner:
   *   only used to create iterators over that subset
   *     -- no insert/delete
   */
  public static class SubSet implements NavigableSet<TOP> {
    final Supplier<OrderedFsSet_array2> theSet;
    final private TOP fromElement;
    final private TOP toElement;
    final private boolean fromInclusive;
    final private boolean toInclusive;
    
    final private int firstPosInRange;
    final private int lastPosInRange;  // inclusive
    
    final private TOP firstElementInRange;
    final private TOP lastElementInRange;
        
    private int sizeSubSet = -1; // lazy - computed on first ref

    private OrderedFsSet_array2 theSet() {
      return theSet.get();
    }
    
    SubSet(Supplier<OrderedFsSet_array2> theSet, TOP fromElement, boolean fromInclusive, TOP toElement, boolean toInclusive) {
      this(theSet, fromElement, fromInclusive, toElement, toInclusive, true, theSet.get().comparatorWithID);
    }
    
    SubSet(Supplier<OrderedFsSet_array2> theSet, TOP fromElement, boolean fromInclusive, TOP toElement, boolean toInclusive, boolean doTest, Comparator<TOP> comparator) {
      this.theSet = theSet;
      this.fromElement = fromElement;
      this.toElement = toElement;
      this.fromInclusive = fromInclusive;
      this.toInclusive = toInclusive;
      if (doTest && comparator.compare(fromElement, toElement) > 0) {
        throw new IllegalArgumentException();
      }
      OrderedFsSet_array2 s = theSet();
      theSet().processBatch();    
      firstPosInRange = fromInclusive ? s.ceilingPos(fromElement) : s.higherPos(fromElement);
      lastPosInRange  = toInclusive ? s.floorPos(toElement) : s.lowerPos(toElement);
      // lastPosInRange can be LT firstPosition if fromInclusive is false
      //   In this case, the subset is empty
      if (lastPosInRange < firstPosInRange) {
        firstElementInRange = null;
        lastElementInRange = null;
      } else {
        firstElementInRange = s.a[firstPosInRange];
        lastElementInRange = s.a[lastPosInRange];
      }
    }
    
    @Override
    public Comparator<? super TOP> comparator() {
      return theSet().comparatorWithID;
    }

    @Override
    public TOP first() {
      return firstElementInRange;
    }

    @Override
    public TOP last() {
      return lastElementInRange;
    }

    @Override
    public int size() {
      if (firstElementInRange == null) {
        return 0;
      }
      if (sizeSubSet == -1) {
        Iterator<TOP> it = iterator();
        int i = 0;
        while (it.hasNext()) {
          it.next();
          i++;
        }
        sizeSubSet = i;
      }
      return sizeSubSet;
    }

    @Override
    public boolean isEmpty() {
      return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
      TOP fs = (TOP) o;
      if (!isInRange(fs)) {
        return false;
      }
      return theSet().contains(o);
    }

    @Override
    public Object[] toArray() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a1) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(TOP e) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends TOP> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override
    public TOP lower(TOP fs) {
      if (lastElementInRange == null || isLeFirst(fs)) {
        return null;
      }
      // if the key is > lastElement, 
      //   return last element
      if (isGtLast(fs)) {
        return lastElementInRange;
      }
      // in range
      return theSet().lower(fs);
    }

    @Override
    public TOP floor(TOP fs) {
      
      // if the key is < the first element in the range, return null
      if (lastElementInRange == null || isLtFirst(fs)) {
        return null;
      }
      
      // if the key is >= lastElement, 
      //   return last element
      if (isGeLast(fs)) {
        return lastElementInRange;
      }
      
      return theSet().floor(fs);
    }

    @Override
    public TOP ceiling(TOP fs) {
      // if the key is > the last element in the range, return null
      if (firstElementInRange == null || isGtLast(fs)) {
        return null;
      }
      
      if (isLeFirst(fs)) {
        return firstElementInRange;
      }
      
      return theSet().ceiling(fs);
    }

    @Override
    public TOP higher(TOP fs) {
      if (firstElementInRange == null || isGeLast(fs)) {
        return null;
      }
      
      if (isLtFirst(fs)) {
        return firstElementInRange;
      }
      
      return theSet().higher(fs);
    }

    @Override
    public TOP pollFirst() {
      throw new UnsupportedOperationException();
    }

    @Override
    public TOP pollLast() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<TOP> iterator() {
      if (firstElementInRange == null) {
        return Collections.emptyIterator();
      }
      return new Iterator<TOP>() {
        private int pos = firstPosInRange;
         
        @Override
        public boolean hasNext() {
          return pos <= lastPosInRange;  // lastPos is inclusive
        }
        
        @Override
        public TOP next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }

          TOP r = theSet().a[pos++];
          incrToSkipOverNulls();
          return r;        
        }
        
        private void incrToSkipOverNulls() {
          while (pos <= lastPosInRange) {
            if (theSet().a[pos] != null) {
              break;
            }
            pos ++;
          }
        }
      };
    }

    @Override
    public NavigableSet<TOP> descendingSet() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<TOP> descendingIterator() {
      if (firstElementInRange == null) {
        return Collections.emptyIterator();
      }
      return new Iterator<TOP>() {
        private int pos = lastPosInRange;
         
        @Override
        public boolean hasNext() {
          return pos >= firstPosInRange;  
        }
        
        @Override
        public TOP next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }

          TOP r = theSet().a[pos--];
          decrToNext();
          return r;        
        }
        
        private void decrToNext() {
          while (pos >= firstPosInRange) {
            if (theSet().a[pos] != null) {
              break;
            }
            pos --;
          }
        }
      };
    }

    @Override
    public NavigableSet<TOP> subSet(TOP fromElement1, boolean fromInclusive1, TOP toElement1,
        boolean toInclusive1) {
      if (!isInRange(fromElement1) || !isInRange(toElement1)) {
        throw new IllegalArgumentException();
      }
      return theSet().subSet(fromElement1, fromInclusive1, toElement1, toInclusive1);  
    }

    @Override
    public NavigableSet<TOP> headSet(TOP toElement1, boolean inclusive) {
      return subSet(fromElement, fromInclusive, toElement1, inclusive);
    }

    @Override
    public NavigableSet<TOP> tailSet(TOP fromElement1, boolean inclusive) {
      return subSet(fromElement1, inclusive, toElement, toInclusive);
    }

    @Override
    public SortedSet<TOP> subSet(TOP fromElement1, TOP toElement1) {
      return subSet(fromElement1, true, toElement1, false);
    }

    @Override
    public SortedSet<TOP> headSet(TOP toElement1) {
      return headSet(toElement1, true);
    }

    @Override
    public SortedSet<TOP> tailSet(TOP fromElement1) {
      return tailSet(fromElement1, false);
    }
  
    private boolean isGtLast(TOP fs) {
      return theSet().comparatorWithID.compare(fs, lastElementInRange) > 0;      
    }
    
    private boolean isGeLast(TOP fs) {
      return theSet().comparatorWithID.compare(fs,  lastElementInRange) >= 0;
    }

    private boolean isLtFirst(TOP fs) {
      return theSet().comparatorWithID.compare(fs, firstElementInRange) < 0;
    }

    private boolean isLeFirst(TOP fs) {
      return theSet().comparatorWithID.compare(fs, firstElementInRange) <= 0;
    }
    
    private boolean isInRange(TOP fs) {
      return isInRangeLower(fs) && isInRangeHigher(fs);
    }
      
    private boolean isInRangeLower(TOP fs) {
      if (firstElementInRange == null) {
        return false;
      }
      int r = theSet().comparatorWithID.compare(fs, firstElementInRange);
      return fromInclusive ? (r >= 0) : (r > 0);
    }
    
    private boolean isInRangeHigher(TOP fs) {
      if (lastElementInRange == null) {
        return false;
      }
      int r = theSet().comparatorWithID.compare(fs, lastElementInRange);
      return toInclusive ? (r <= 0) : (r < 0);
    }
  }

  public int getModificationCount() {
    return modificationCount;
  }
  
  @Override
  public String toString() {
//    processBatch();
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
          b.append(i.toShortString());
        } else {
          b.append("null");
        }
  //      prettyPrint(0, 2, b, true); 
      }
    } else {
      b.append("null");
    }
    b   .append(", a_nextFreeslot=").append(a_nextFreeslot)
        .append(", a_firstUsedslot=").append(a_firstUsedslot)
        .append(", batch=").append(batch)
        .append(", origComparator=").append(comparatorWithID)
        .append(", size=").append(size)
        .append(", maxSize=").append(maxSize)
        .append(", highest=").append(highest)
        .append(", nullBlockStart=").append(nullBlockStart)
        .append(", nullBlockEnd=").append(nullBlockEnd).append("]");
    return b.toString();
  } 
 
  // these are approximate - don't take into account multi-thread access
  static private int addToEndCount = 0;
  static private int addNotToEndCount = 0;
  static private int batchCountHistogram[];
  static private int batchAddCount = 0; 
  static private int batchAddTotal = 0; // includes things not added because of dups
  static private int moveSizeHistogram[];
  static private int movePctHistogram[];
  static private int fillHistogram[];
  static private int iterPctEmptySkip[];
  
  static {
    if (MEASURE) {
      batchCountHistogram = new int[24];  // slot x = 2^x to (2^(x+1) - 1) counts
                                          // slot 0 = 1, slot 1 = 2-3, etc
      Arrays.fill(batchCountHistogram,  0);
      
      moveSizeHistogram = new int[24];
      movePctHistogram = new int[10];  // slot 0 = 0-9%  1 = 10-19% 9 = 90 - 100%
      fillHistogram = new int[24];
      
      iterPctEmptySkip = new int[10];
          
      Runtime.getRuntime().addShutdownHook(new Thread(null, () -> {
        System.out.println("Histogram measures of Ordered Set add / remove operations");
        System.out.format(" - Add to end: %,d,  batch add count: %,d  batch add tot: %,d%n", 
            addToEndCount, batchAddCount, batchAddTotal);
        for (int i = 0; i < batchCountHistogram.length; i++) {
          int v = batchCountHistogram[i];
          if (v == 0) continue;
          System.out.format(" batch size: %,d, count: %,d%n", 1 << i, v);
        }
        for (int i = 0; i < moveSizeHistogram.length; i++) {
          int v = moveSizeHistogram[i];
          if (v == 0) continue;
          System.out.format(" move size: %,d, count: %,d%n", 
              (i == 0) ? 0 : 1 << (i - 1), v);
        }
        for (int i = 0; i < movePctHistogram.length; i++) {
          int v = movePctHistogram[i];
          if (v == 0) continue;
          System.out.format(" move Pct: %,d - %,d, count: %,d%n", i*10, (i+1)*10, v);
        }
        for (int i = 0; i < fillHistogram.length; i++) {
          int v = fillHistogram[i];
          if (v == 0) continue;
          System.out.format(" fill size: %,d, count: %,d%n", 
              (i == 0) ? 0 : 1 << (i - 1), v);
        }
        for (int i = 0; i < iterPctEmptySkip.length; i++) {
          int v = iterPctEmptySkip[i];
          if (v == 0) continue;
          System.out.format(" iterator percent empty needing skip: %,d - %,d, count: %,d%n", i*10, (i+1)*10, v);
        }


      }, "dump measures OrderedFsSetSorted"));
    }

  }
  
}
