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

package org.apache.uima.internal.util.rb_trees;

import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.FSRBTSetIndex;
import org.apache.uima.cas.impl.IntIterator4set;
import org.apache.uima.internal.util.ComparableIntPointerIterator;
import org.apache.uima.internal.util.IntComparator;
import org.apache.uima.internal.util.IntListIterator;
import org.apache.uima.internal.util.IntPointerIterator;

/**
 * A set (not a map) of ints.
 * 
 * uses IntArrayRBTcommon
 * 
 * Red-black tree implementation based on integer arrays. Preliminary performance measurements on
 * j2se 1.4 indicate that the performance improvement as opposed to an object-based implementation
 * are miniscule. This seems to indicate a much improved object creation handling in this vm.
 * 
 * <p>
 * This tree implementation knows two modes of insertion: keys that are already in the tree can be
 * rejected, or inserted as duplicates. Duplicate key insertion is randomized so that the tree's
 * performance degrades gracefully in the presence of many identical keys.
 * 
 * 
 */
public class IntArrayRBT extends IntArrayRBTcommon {

//  /**
//   * This is the int iterator used for Set indexes
//   * Implement a comparable iterator over the keys.
//   * Adds support to PointerIterator for
//   *   - concurrent modification detection
//   *   - comparing two iterators (for type/subtype ordering)
//   * 
//   */
//  private class IntIteratorForSets extends PointerIterator implements
//          ComparableIntPointerIterator {
//
//    private final IntComparator comp;
//
//    private int modificationSnapshot; // to catch illegal modifications
//
//    private int[] detectIllegalIndexUpdates; // shared copy with Index Repository
//
//    private int typeCode;
//
//    public boolean isConcurrentModification() {
//      return modificationSnapshot != detectIllegalIndexUpdates[typeCode];
//    }
//
//    public void resetConcurrentModification() {
//      modificationSnapshot = detectIllegalIndexUpdates[typeCode];
//    }
//
//    private IntIteratorForSets(IntComparator comp) {
//      super();
//      this.comp = comp;
//    }
//
//    /**
//     * @see java.lang.Comparable#compareTo(Object)
//     */
//    public int compareTo(Object o) throws NoSuchElementException {
//      ComparableIntPointerIterator it = (ComparableIntPointerIterator) o;
//      // assert(this.comp != null);
//      return this.comp.compare(get(), it.get());
//    }
//
//    public Object copy() {
//      IntIteratorForSets copy = new IntIteratorForSets(this.comp);
//      copy.currentNode = this.currentNode;
//      return copy;
//    }
//
//    @Override
//    public String toString() {
//      return "ComparablePointerIterator [comp=" + comp + ", typeCode=" + typeCode + ", currentNode=" + currentNode
//          + "]";
//    }
//    
//  }

//  /**
//   * IntPointerIterator support for IntArrayRBT style indexes
//   * 
//   * No Concurrent Modification testing
//   * 
//   * No support for type/subtype iterator comparison
//   * 
//   * For these, see above class
//   * 
//   * 
//   */
//  private class PointerIterator implements IntPointerIterator {
//
//    protected int currentNode;
//
//    private PointerIterator() {
//      super();
//      moveToFirst();
//    }
//
//    /**
//     * @see org.apache.uima.internal.util.IntPointerIterator#dec()
//     */
//    public void dec() {
//      this.currentNode = previousNode(this.currentNode);
//    }
//
//    /**
//     * @see org.apache.uima.internal.util.IntPointerIterator#get()
//     */
//    public int get() {
//      if (!isValid()) {
//        throw new NoSuchElementException();
//      }
//      return IntArrayRBT.this.getKey(this.currentNode);
//    }
//
//    /**
//     * @see org.apache.uima.internal.util.IntPointerIterator#inc()
//     */
//    public void inc() {
//      this.currentNode = nextNode(this.currentNode);
//    }
//
//    /**
//     * @see org.apache.uima.internal.util.IntPointerIterator#isValid()
//     */
//    public boolean isValid() {
//      return (this.currentNode != NIL);
//    }
//
//    /**
//     * @see org.apache.uima.internal.util.IntPointerIterator#moveToFirst()
//     */
//    public void moveToFirst() {
//      this.currentNode = getFirstNode();
//    }
//
//    /**
//     * @see org.apache.uima.internal.util.IntPointerIterator#moveToLast()
//     */
//    public void moveToLast() {
//      this.currentNode = IntArrayRBT.this.greatestNode;
//    }
//
//    /**
//     * @see org.apache.uima.internal.util.IntPointerIterator#copy()
//     */
//    public Object copy() {
//      PointerIterator it = new PointerIterator();
//      it.currentNode = this.currentNode;
//      return it;
//    }
//
//    /**
//     * @see org.apache.uima.internal.util.IntPointerIterator#moveTo(int)
//     */
//    public void moveTo(int i) {
//      this.currentNode = findInsertionPoint(i);
//    }
//
//  }

  private class IntArrayRBTKeyIterator implements IntListIterator {

    protected int currentNode;

    protected IntArrayRBTKeyIterator() {
      super();
      moveToStart();
    }

    @Override
    public final boolean hasNext() {
      return (this.currentNode != NIL);
    }

    @Override
    public final int next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      int r = IntArrayRBT.this.getKeyForNode(this.currentNode);
      this.currentNode = nextNode(this.currentNode);
      return r;
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#hasPrevious()
     */
    @Override
    public boolean hasPrevious() {
      return (previousNode(this.currentNode) != NIL);
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#previous()
     */
    @Override
    public int previous() {
      this.currentNode = previousNode(this.currentNode);
      if (this.currentNode != NIL) {
        return getKey(this.currentNode);
      }
      throw new NoSuchElementException();
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#moveToEnd()
     */
    @Override
    public void moveToEnd() {
      this.currentNode = IntArrayRBT.this.greatestNode;
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#moveToStart()
     */
    @Override
    public void moveToStart() {
      this.currentNode = getFirstNode();
    }

    protected final int getKey(int node) { 
      return IntArrayRBT.this.getKeyForNode(node);
    }

  }

//  private class ComparableIterator extends IntArrayRBTKeyIterator implements ComparableIntIterator {
//
//    private final IntComparator comparator;
//
//    private ComparableIterator(IntComparator comp) {
//      super();
//      this.comparator = comp;
//    }
//
//    /**
//     * @see java.lang.Comparable#compareTo(Object)
//     */
//    public int compareTo(Object o) {
//      ComparableIterator it = (ComparableIterator) o;
//      return this.comparator.compare(
//          IntArrayRBT.this.getKey(this.currentNode), 
//          it.getKey(it.currentNode));
//    }
//
//  }

  /**
   * Constructor for IntArrayRBT.
   */
  public IntArrayRBT() {
    this(default_size);
  }

  public IntArrayRBT(int initialSize) {
    super();
  }
    
  /**
   * 
   * @param k the value to insert
   * @return negative of the node number of the found key, if the key was found, else, the node number of the inserted new node
   */
  protected int treeInsert(final int k) {
    return treeInsert(k, false);
  }

  protected int treeInsertWithDups(final int k) {
    return treeInsert(k, true);
  }
    
  protected int treeInsert(final int k, boolean withDups) {  
    if (this.greatestNode != NIL) {
      final int lt = withDups? 1 : 0;
      if (compare(getKeyForNode(this.greatestNode), k) < lt) {
        final int y = this.greatestNode;
        final int z = newNode(k);
        this.greatestNode = z;
        setRight(y, z);
        setParent(z, y);
        return z;
      }
    }
    
    int x = this.root;  // could be NIL
    int y = NIL;
    
    // find existing value (key)
    int cr = 0;
    boolean wentLeft = false;
    ThreadLocalRandom rand = withDups? ThreadLocalRandom.current() : null;
    while (x != NIL) {
      y = x;
      cr = compare(k, getKeyForNode(x));
      if (cr == 0) {
        if (withDups) {
          if (rand.nextBoolean()) {
            x = getLeft(x);
            wentLeft = true;
          } else {
            x = getRight(x);
            wentLeft = false;
          }  
        } else {
          // not with dups, found, return negative of found index
          return -x;
        }
      } else {
        // cr not 0
        x = (cr < 0) ? getLeft(x) : getRight(x);
      }
    }
    
    // The key was not found or was found but inserting dups
    
    final int z = newNode(k);
    if (y == NIL) {  // only happens if this.root is NIL, e.g. table is empty.
      setAsRoot(z); // also set parent to NIL
      this.greatestNode = z;
    } else {
      setParent(z, y);
      if (cr < 0) {
        setLeft(y, z);
      } else if (cr > 0) {
        setRight(y, z);
      } else { // k == key[y]
        // Randomly insert node to the left or right.
        if (wentLeft) {
          setLeft(y, z);
        } else {
          setRight(y, z);
        }
      }
    }
    return z;
  }

  public int insertKey(int k) {
    return insertKey(k, false);
  }
  
  public int add(int k) {
    return insertKey(k, false);
  }
  
  /**
   * like add, but returns boolean flag true if not present before
   * @param k -
   * @return true if added (not present before)
   */
  public boolean addAdded(int k) {
    if (this.root == NIL) {
      final int x = newNode(k);
      setAsRoot(x);
      this.color[this.root] = black;
      this.greatestNode = x;
      return true;
    }
    final int x = treeInsert(k);
    if (x < NIL) {
      return false;  // negative if found
    }
    commonInsertKey(x);
    return true;
  }

//  public int insertKeyWithDups(int k) {
//    return insertKey(k, true);
//  }

  private int insertKey(final int k, final boolean withDups) {
    if (this.root == NIL) {
      final int x = newNode(k);
      setAsRoot(x);
      this.color[this.root] = black;
      this.greatestNode = x;
      return x;
    }
    final int x = treeInsert(k, withDups);
    if (x < NIL) {  // means was found, not inserted
      return -x;
    }
    return commonInsertKey(x);
  }
  
  //for testing only
  public int insertKeyShowNegative(int k) {
    if (this.root == NIL) {
      final int x = newNode(k);
      setAsRoot(x);
      this.color[this.root] = black;
      this.greatestNode = x;
      return x;
    }
    final int x = treeInsert(k, false);
    if (x < NIL) {  // means was found, not inserted
      return x;
    }
    return commonInsertKey(x);
    
  }
  /**
   * Code run after the insert operation, done to rebalance the red black tree
   * @param x -
   * @return -
   */
  private int commonInsertKey(int x) {
    this.color[x] = red;
    final int node = x;
    while ((x != this.root) && (this.color[getParent(x)] == red)) {
      final int parent_x = getParent(x);
      final int parent_parent_x = getParent(parent_x);
      if (parent_x == getLeft(parent_parent_x)) {
        final int y = getRight(parent_parent_x);
        if (this.color[y] == red) {
          this.color[parent_x] = black;
          this.color[y] = black;
          this.color[parent_parent_x] = red;
          x = parent_parent_x;
        } else {
          if (x == getRight(parent_x)) {
            x = parent_x;
            leftRotate(x);
          }
          final int parent2_x = getParent(x);
          this.color[parent2_x] = black;
          final int parent2_parent2_x = getParent(parent2_x);
          this.color[parent2_parent2_x] = red;
          rightRotate(parent2_parent2_x);
        }
      } else {
        final int y = getLeft(parent_parent_x);
        if (this.color[y] == red) {
          this.color[parent_x] = black;
          this.color[y] = black;
          this.color[parent_parent_x] = red;
          x = parent_parent_x;
        } else {
          if (x == getLeft(parent_x)) {
            x = parent_x;
            rightRotate(x);
          }
          final int parent2_x = getParent(x);
          this.color[parent2_x] = black;
          final int parent2_parent2_x = getParent(parent2_x);
          this.color[parent2_parent2_x] = red;
          leftRotate(parent2_parent2_x);
        }
      }
    }
    this.color[this.root] = black;
    return node;
  }

  // private final boolean isNewNode(int node) {
  // return (node == (next - 1));
  // }

//  private final boolean isLeftDtr(int node) {
//    return ((node != this.root) && (node == getLeft(getParent(node))));
//  }

  // private final boolean isRightDtr(int node) {
  // return ((node != root) && (node == right[parent[node]]));
  // }

  public boolean deleteKey(int aKey) {
    final int node = findKey(aKey);
    // for use with FS set index, key is the fs addr, and the compare fn is the UIMA Set compare definition
    //   Multiple FSs may compare equal, even though they are different FSs
    //   Only delete if the fs addr found is the one being looked for
    //   For this use, there are never multiple FSs comparing equal in the set because it's a set.
    if (node == NIL || getKeyForNode(node) != aKey) {
      return false;
    }
    deleteNode(node);
    --this.size;
    if (size == 0 && next > multiplication_limit) {
      flush();  // recover space 
    }
    return true;
  }
  
  public void clear() {
    flush();
  }
  
//  /**
//   * Method iterator.
//   * @param comp comparator  
//   * @return IntListIterator
//   */
//  public ComparableIntIterator iterator(IntComparator comp) {
//    return new ComparableIterator(comp);
//  }

  public IntListIterator iterator() {
    return new IntArrayRBTKeyIterator();
  }

  // this version doesn't do ConcurrentModificationException testing
  public <T extends FeatureStructure> IntPointerIterator pointerIterator(IntComparator comp, FSRBTSetIndex<T> fsSetIndex) {
    return new IntIterator4set<T>(fsSetIndex, null, comp);
  }

//  public IntPointerIterator pointerIterator(int aKey) {
//    PointerIterator it = new PointerIterator();
//    it.currentNode = this.findKey(aKey);
//    return it;
//  }

  public <T extends FeatureStructure> ComparableIntPointerIterator<T> pointerIterator(
      FSRBTSetIndex<T> fsSetIndex, int[] detectIllegalIndexUpdates, IntComparator comp) {
    IntIterator4set<T> cpi = new IntIterator4set<T>(fsSetIndex, detectIllegalIndexUpdates, comp);
    return cpi;
  }
  
  //debug
  public boolean debugScanFor(int key) {
    for (int i = 1; i < next; i++) {
      if (getKeyForNode(i) == key) {
        return true;
      }
    }
    return false;
  }

  public int getKeyForNode(int node) { // is public
    return super.getKeyForNode(node);  // is protected
  }
}
