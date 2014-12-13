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
import java.util.Random;

import org.apache.uima.internal.util.ComparableIntIterator;
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

  /**
   * Implement a comparable iterator over the keys.
   * 
   * 
   */
  private class ComparablePointerIterator extends PointerIterator implements
          ComparableIntPointerIterator {

    private final IntComparator comp;

    private int modificationSnapshot; // to catch illegal modifications

    private int[] detectIllegalIndexUpdates; // shared copy with Index Repository

    private int typeCode;

    public boolean isConcurrentModification() {
      return modificationSnapshot != detectIllegalIndexUpdates[typeCode];
    }

    public void resetConcurrentModification() {
      modificationSnapshot = detectIllegalIndexUpdates[typeCode];
    }

    private ComparablePointerIterator(IntComparator comp) {
      super();
      this.comp = comp;
    }

    /**
     * @see java.lang.Comparable#compareTo(Object)
     */
    public int compareTo(Object o) throws NoSuchElementException {
      ComparableIntPointerIterator it = (ComparableIntPointerIterator) o;
      // assert(this.comp != null);
      return this.comp.compare(get(), it.get());
    }

    public Object copy() {
      ComparablePointerIterator copy = new ComparablePointerIterator(this.comp);
      copy.currentNode = this.currentNode;
      return copy;
    }

    @Override
    public String toString() {
      return "ComparablePointerIterator [comp=" + comp + ", typeCode=" + typeCode + ", currentNode=" + currentNode
          + "]";
    }
    
  }

  /**
   * Class comment for IntArrayRBT.java goes here.
   * 
   * 
   */
  private class PointerIterator implements IntPointerIterator {

    protected int currentNode;

    private PointerIterator() {
      super();
      moveToFirst();
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#dec()
     */
    public void dec() {
      this.currentNode = previousNode(this.currentNode);
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#get()
     */
    public int get() {
      if (!isValid()) {
        throw new NoSuchElementException();
      }
      return IntArrayRBT.this.getKey(this.currentNode);
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#inc()
     */
    public void inc() {
      this.currentNode = nextNode(this.currentNode);
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#isValid()
     */
    public boolean isValid() {
      return (this.currentNode != NIL);
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#moveToFirst()
     */
    public void moveToFirst() {
      this.currentNode = getFirstNode();
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#moveToLast()
     */
    public void moveToLast() {
      this.currentNode = IntArrayRBT.this.greatestNode;
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#copy()
     */
    public Object copy() {
      PointerIterator it = new PointerIterator();
      it.currentNode = this.currentNode;
      return it;
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#moveTo(int)
     */
    public void moveTo(int i) {
      this.currentNode = findInsertionPoint(i);
    }

  }

  private class IntArrayRBTKeyIterator implements IntListIterator {

    protected int currentNode;

    protected IntArrayRBTKeyIterator() {
      super();
      this.currentNode = NIL;
    }

    public final boolean hasNext() {
      return (this.currentNode != IntArrayRBT.this.greatestNode);
    }

    public final int next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      this.currentNode = (this.currentNode == NIL) ? getFirstNode() : nextNode(this.currentNode);
      return IntArrayRBT.this.getKey(this.currentNode);
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#hasPrevious()
     */
    public boolean hasPrevious() {
      return (this.currentNode != NIL);
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#previous()
     */
    public int previous() {
      if (!hasPrevious()) {
        throw new NoSuchElementException();
      }
      final int currentKey = IntArrayRBT.this.getKey(this.currentNode);
      if (this.currentNode == getFirstNode()) {
        this.currentNode = NIL;
      } else {
        this.currentNode = previousNode(this.currentNode);
      }
      return currentKey;
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#moveToEnd()
     */
    public void moveToEnd() {
      this.currentNode = IntArrayRBT.this.greatestNode;
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#moveToStart()
     */
    public void moveToStart() {
      this.currentNode = NIL;
    }

    protected final int getKey(int node) {
      return IntArrayRBT.this.getKey(node);
    }

  }

  private class ComparableIterator extends IntArrayRBTKeyIterator implements ComparableIntIterator {

    private final IntComparator comparator;

    private ComparableIterator(IntComparator comp) {
      super();
      this.comparator = comp;
    }

    /**
     * @see java.lang.Comparable#compareTo(Object)
     */
    public int compareTo(Object o) {
      ComparableIterator it = (ComparableIterator) o;
      return this.comparator.compare(
          IntArrayRBT.this.getKey(this.currentNode), 
          it.getKey(it.currentNode));
    }

  }


 

  // Random number generator to randomize inserts of identical keys.
  protected final Random rand;

  /**
   * Constructor for IntArrayRBT.
   */
  public IntArrayRBT() {
    this(default_size);
  }

  public IntArrayRBT(int initialSize) {
    super();
    this.rand = new Random();
  }
    
  public int getKeyForNode(final int node) {
    return getKey(node);
  }

  protected int treeInsert(final int k) {
    if ((this.greatestNode != NIL) && (getKey(this.greatestNode) < k)) {
      final int y = this.greatestNode;
      final int z = newNode(k);
      this.greatestNode = z;
      setRight(y, z);
      setParent(z, y);
      return z;
    } 
    int x = this.root;
    int y = NIL;
    while (x != NIL) {
      y = x;
      final int xKey = getKey(x);
      if (k == xKey) {
        return -x;
      }
      x = (k < xKey) ? getLeft(x) : getRight(x);
    }
    // The key was not found, so we create a new node, inserting the
    // key.
    final int z = newNode(k);
    if (y == NIL) {
      setAsRoot(z);
      this.greatestNode = z;
      setParent(z, NIL);
    } else {
      setParent(z, y);
      if (k < getKey(y)) {
        setLeft(y, z);
      } else {
        setRight(y, z);
      }
    }
    return z;
  }

  protected int treeInsertWithDups(final int k) {
    if ((this.greatestNode != NIL) && (getKey(this.greatestNode) <= k)) {
      final int y = this.greatestNode;
      final int z = newNode(k);
      this.greatestNode = z;
      setRight(y, z);
      setParent(z, y);
      return z;
    }
    int y = NIL;
    int x = this.root;
    while (x != NIL) {
      y = x;
      final int xKey = getKey(x);
      x = (k < xKey) ? getLeft(x) :
          (k > xKey) ? getRight(x) :
        //(k == xKey)
          (this.rand.nextBoolean()) ?  getLeft(x) : getRight(x);
    }
    final int z = newNode(k);
    if (y == NIL) {
      setAsRoot(z);
      this.greatestNode = z;
      setParent(z, NIL);
    } else {
      setParent(z, y);
      if (k < getKey(y)) {
        setLeft(y, z);
      } else if (k > getKey(y)) {
        setRight(y, z);
      } else { // k == key[y]
        // Randomly insert node to the left or right.
        if (this.rand.nextBoolean()) {
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

  public int insertKeyWithDups(int k) {
    return insertKey(k, true);
  }

  private int insertKey(final int k, final boolean withDups) {
    if (this.root == NIL) {
      final int x = newNode(k);
      setAsRoot(x);
      this.color[this.root] = black;
      this.greatestNode = x;
      return x;
    }
    final int x = withDups ? treeInsertWithDups(k) : treeInsert(k);
    if (x < NIL) {
      return -x;
    }
    return commonInsertKey(x);
  }
  
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

  /**
   * Find the first node such that k &lt;= key[node].
   */
  public int findKey(final int k) {
    int node = this.root;
    while (node != NIL) {
      final int keyNode = getKey(node);
      if (k < keyNode) {
        node = getLeft(node);
      } else if (k == keyNode) {
        return node;
      } else {
        node = getRight(node);
      }
    }
    // node == NIL
    return NIL;
  }

  /**
   * Find the node such that key[node] &ge; k and key[previous(node)] &lt; k.
   * @param k the key
   * @return the index of the node
   */
  public int findInsertionPoint(final int k) {
    int node = this.root;
    int found = node;
    while (node != NIL) {
      found = node;
      final int keyNode = getKey(node);
      if (k < keyNode) {
        node = getLeft(node);
      } else if (k == keyNode) {
        // In the presence of duplicates, we have to check if there are
        // identical
        // keys to the left of us.
        while (true) {
          final int left_node = getLeft(node);
          if ((left_node == NIL) ||
              (getKey(left_node) != keyNode)) {
            break;
          }
          node = left_node;
        }
//        while ((getLeft(node) != NIL) && (getKey(getLeft(node)) == keyNode)) {
//          node = getLeft(node);
//        }
        return node;
      } else {
        node = getRight(node);
      }
    }
    // node == NIL
    return found;
  }

//  private final boolean isLeftDtr(int node) {
//    return ((node != this.root) && (node == getLeft(getParent(node))));
//  }

  // private final boolean isRightDtr(int node) {
  // return ((node != root) && (node == right[parent[node]]));
  // }

  public boolean deleteKey(int aKey) {
    final int node = findKey(aKey);
    if (node == NIL) {
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
  
  /**
   * Method iterator.
   * @param comp comparator  
   * @return IntListIterator
   */
  public ComparableIntIterator iterator(IntComparator comp) {
    return new ComparableIterator(comp);
  }

  public IntListIterator iterator() {
    return new IntArrayRBTKeyIterator();
  }

  public IntPointerIterator pointerIterator() {
    return new PointerIterator();
  }

  public IntPointerIterator pointerIterator(int aKey) {
    PointerIterator it = new PointerIterator();
    it.currentNode = this.findKey(aKey);
    return it;
  }

  public ComparableIntPointerIterator pointerIterator(IntComparator comp,
          int[] detectIllegalIndexUpdates, int typeCode) {
    // assert(comp != null);
    ComparablePointerIterator cpi = new ComparablePointerIterator(comp);
    cpi.modificationSnapshot = detectIllegalIndexUpdates[typeCode];
    cpi.detectIllegalIndexUpdates = detectIllegalIndexUpdates;
    cpi.typeCode = typeCode;
    return cpi;
  }



}
