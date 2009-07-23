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
import org.apache.uima.internal.util.IntArrayUtils;
import org.apache.uima.internal.util.IntComparator;
import org.apache.uima.internal.util.IntListIterator;
import org.apache.uima.internal.util.IntPointerIterator;
import org.apache.uima.internal.util.StringUtils;

/**
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
public class IntArrayRBT {

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
      return IntArrayRBT.this.key[this.currentNode];
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
      this.currentNode = nextNode(this.currentNode);
      return IntArrayRBT.this.key[this.currentNode];
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
      final int currentKey = IntArrayRBT.this.key[this.currentNode];
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
      return IntArrayRBT.this.key[node];
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
      return this.comparator.compare(IntArrayRBT.this.key[this.currentNode], it
              .getKey(it.currentNode));
    }

  }

  // Keys.
  protected int[] key;

  // Left daughters.
  protected int[] left;

  // Right daughters.
  protected int[] right;

  // Parents.
  protected int[] parent;

  // Colors.
  protected boolean[] color;

  // The index of the next node.
  private int next;

  // The current size of the tree. Since we can remove nodes, since needs to
  // be kept separate from the next free cell.
  private int size;

  // The root of the tree.
  protected int root;

  // Keep a pointer to the largest node around so we can optimize for
  // inserting
  // keys that are larger than all keys already in the tree.
  protected int greatestNode;

  protected static final int default_size = 1024;

  private static final int default_growth_factor = 2;

  private static final int default_multiplication_limit = 2000000;

  private int growth_factor;

  private int multiplication_limit;

  // The NIL sentinel
  public static final int NIL = 0;

  // The colors.
  protected static final boolean red = true;

  protected static final boolean black = false;

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
    if (initialSize < 1) {
      initialSize = 1;
    }
    initVars();
    // Increase initialSize by one since we use one slot for sentinel.
    ++initialSize;
    this.growth_factor = default_growth_factor;
    this.multiplication_limit = default_multiplication_limit;
    // Init the arrays.
    this.key = new int[initialSize];
    this.left = new int[initialSize];
    this.right = new int[initialSize];
    this.parent = new int[initialSize];
    this.color = new boolean[initialSize];
    this.left[NIL] = NIL;
    this.right[NIL] = NIL;
    this.parent[NIL] = NIL;
    this.color[NIL] = black;
  }

  private void initVars() {
    this.root = NIL;
    this.greatestNode = NIL;
    this.next = 1;
    this.size = 0;
  }

  public void flush() {
    // All we do for flush is set the root to NIL and the size to 0.
    initVars();
  }

  public final int size() {
    return this.size;
  }

  private void grow(int initialSize) {
    this.key = grow(this.key, initialSize);
    this.left = grow(this.left, initialSize);
    this.right = grow(this.right, initialSize);
    this.parent = grow(this.parent, initialSize);
    this.color = grow(this.color, initialSize);
  }

  protected int treeInsert(int k) {
    int x = this.root;
    int y, z;
    if ((this.greatestNode != NIL) && (this.key[this.greatestNode] < k)) {
      y = this.greatestNode;
      z = newNode(k);
      this.greatestNode = z;
    } else {
      y = NIL;
      int xKey;
      while (x != NIL) {
        y = x;
        xKey = this.key[x];
        if (k < xKey) {
          x = this.left[x];
        } else if (k == xKey) {
          return -x;
        } else { // k == key[x]
          x = this.right[x];
        }
      }
      // The key was not found, so we create a new node, inserting the
      // key.
      z = newNode(k);
    }
    if (y == NIL) {
      setAsRoot(z);
      this.greatestNode = z;
      this.parent[z] = NIL;
    } else {
      this.parent[z] = y;
      if (k < this.key[y]) {
        this.left[y] = z;
      } else {
        this.right[y] = z;
      }
    }
    return z;
  }

  protected int treeInsertWithDups(int k) {
    int x = this.root;
    int y, z;
    if ((this.greatestNode != NIL) && (this.key[this.greatestNode] <= k)) {
      y = this.greatestNode;
      z = newNode(k);
      this.greatestNode = z;
      this.right[y] = z;
      this.parent[z] = y;
      return z;
    }
    y = NIL;
    int xKey;
    while (x != NIL) {
      y = x;
      xKey = this.key[x];
      if (k < xKey) {
        x = this.left[x];
      } else if (k > xKey) {
        x = this.right[x];
      } else { // k == key[x]
        // Randomly search to the left or right.
        if (this.rand.nextBoolean()) {
          x = this.left[x];
        } else {
          x = this.right[x];
        }
      }
    }
    z = newNode(k);
    if (y == NIL) {
      setAsRoot(z);
      this.greatestNode = z;
      this.parent[z] = NIL;
    } else {
      this.parent[z] = y;
      if (k < this.key[y]) {
        this.left[y] = z;
      } else if (k > this.key[y]) {
        this.right[y] = z;
      } else { // k == key[y]
        // Randomly insert node to the left or right.
        if (this.rand.nextBoolean()) {
          this.left[y] = z;
        } else {
          this.right[y] = z;
        }
      }
    }
    return z;
  }

  protected int newNode(int k) {
    // Make sure the tree is big enough to accomodate a new node.
    if (this.next >= this.key.length) {
      grow(this.next + 1);
    }
    // assert(key.length > next);
    final int z = this.next;
    ++this.next;
    ++this.size;
    this.key[z] = k;
    this.left[z] = NIL;
    this.right[z] = NIL;
    this.color[z] = red;
    return z;
  }

  private final void setAsRoot(int x) {
    this.root = x;
    this.parent[this.root] = NIL;
  }

  private final int[] grow(int[] array, int newSize) {
    return IntArrayUtils.ensure_size(array, newSize, this.growth_factor, this.multiplication_limit);
  }

  private final boolean[] grow(boolean[] array, int newSize) {
    return IntArrayUtils.ensure_size(array, newSize, this.growth_factor, this.multiplication_limit);
  }

  private final void leftRotate(int x) {
    final int y = this.right[x];
    this.right[x] = this.left[y];
    if (this.left[y] != NIL) {
      this.parent[this.left[y]] = x;
    }
    this.parent[y] = this.parent[x];
    if (this.root == x) {
      setAsRoot(y);
    } else {
      if (x == this.left[this.parent[x]]) {
        this.left[this.parent[x]] = y;
      } else {
        this.right[this.parent[x]] = y;
      }
    }
    this.left[y] = x;
    this.parent[x] = y;
  }

  private final void rightRotate(int x) {
    final int y = this.left[x];
    this.left[x] = this.right[y];
    if (this.right[y] != NIL) {
      this.parent[this.right[y]] = x;
    }
    this.parent[y] = this.parent[x];
    if (this.root == x) {
      setAsRoot(y);
    } else {
      if (x == this.right[this.parent[x]]) {
        this.right[this.parent[x]] = y;
      } else {
        this.left[this.parent[x]] = y;
      }
    }
    this.right[y] = x;
    this.parent[x] = y;
  }

  public int insertKey(int k) {
    return insertKey(k, false);
  }

  public int insertKeyWithDups(int k) {
    return insertKey(k, true);
  }

  private int insertKey(int k, boolean withDups) {
    int x;
    if (this.root == NIL) {
      x = newNode(k);
      setAsRoot(x);
      this.color[this.root] = black;
      this.greatestNode = x;
      return x;
    }
    if (withDups) {
      x = treeInsertWithDups(k);
    } else {
      x = treeInsert(k);
      if (x < NIL) {
        return -x;
      }
    }
    this.color[x] = red;
    int y;
    final int node = x;
    while ((x != this.root) && (this.color[this.parent[x]] == red)) {
      if (this.parent[x] == this.left[this.parent[this.parent[x]]]) {
        y = this.right[this.parent[this.parent[x]]];
        if (this.color[y] == red) {
          this.color[this.parent[x]] = black;
          this.color[y] = black;
          this.color[this.parent[this.parent[x]]] = red;
          x = this.parent[this.parent[x]];
        } else {
          if (x == this.right[this.parent[x]]) {
            x = this.parent[x];
            leftRotate(x);
          }
          this.color[this.parent[x]] = black;
          this.color[this.parent[this.parent[x]]] = red;
          rightRotate(this.parent[this.parent[x]]);
        }
      } else {
        y = this.left[this.parent[this.parent[x]]];
        if (this.color[y] == red) {
          this.color[this.parent[x]] = black;
          this.color[y] = black;
          this.color[this.parent[this.parent[x]]] = red;
          x = this.parent[this.parent[x]];
        } else {
          if (x == this.left[this.parent[x]]) {
            x = this.parent[x];
            rightRotate(x);
          }
          this.color[this.parent[x]] = black;
          this.color[this.parent[this.parent[x]]] = red;
          leftRotate(this.parent[this.parent[x]]);
        }
      }
    }
    this.color[this.root] = black;
    return node;
  }

  // private final boolean isNewNode(int node) {
  // return (node == (next - 1));
  // }

  public int findKey(int k) {
    int node = this.root;
    while (node != NIL) {
      if (k < this.key[node]) {
        node = this.left[node];
      } else if (k == this.key[node]) {
        return this.key[node];
      } else {
        node = this.right[node];
      }
    }
    // node == NIL
    return NIL;
  }

  /**
   * Find the node such that key[node] >= k and key[previous(node)] < k.
   */
  public int findInsertionPoint(int k) {
    int node = this.root;
    int found = node;
    while (node != NIL) {
      found = node;
      if (k < this.key[node]) {
        node = this.left[node];
      } else if (k == this.key[node]) {
        // In the presence of duplicates, we have to check if there are
        // identical
        // keys to the left of us.
        while ((this.left[node] != NIL) && (this.key[this.left[node]] == this.key[node])) {
          node = this.left[node];
        }
        return node;
      } else {
        node = this.right[node];
      }
    }
    // node == NIL
    return found;
  }

  /**
   * Find the node such that key[node] >= k and key[previous(node)] < k.
   */
  public int findInsertionPointNoDups(int k) {
    int node = this.root;
    int found = node;
    while (node != NIL) {
      found = node;
      if (k < this.key[node]) {
        node = this.left[node];
      } else if (k == this.key[node]) {
        return node;
      } else {
        node = this.right[node];
      }
    }
    // node == NIL
    return found;
  }

  public final boolean containsKey(int k) {
    return (findKey(k) != NIL);
  }

  private final boolean isLeftDtr(int node) {
    return ((node != this.root) && (node == this.left[this.parent[node]]));
  }

  // private final boolean isRightDtr(int node) {
  // return ((node != root) && (node == right[parent[node]]));
  // }

  private final int getFirstNode() {
    if (this.root == NIL) {
      return NIL;
    }
    int node = this.root;
    while (this.left[node] != NIL) {
      node = this.left[node];
    }
    return node;
  }

  // private final int nextNode(int node) {
  // if (right[node] != NIL) {
  // node = right[node];
  // while (left[node] != NIL) {
  // node = left[node];
  // }
  // } else {
  // while (isRightDtr(node)) {
  // node = parent[node];
  // }
  // if (node == root) {
  // return NIL;
  // }
  // // node is now a left dtr, so we can go one up.
  // node = parent[node];
  // }
  // return node;
  // }

  protected final int nextNode(int node) {
    int y;
    if (this.right[node] != NIL) {
      node = this.right[node];
      while (this.left[node] != NIL) {
        node = this.left[node];
      }
    } else {
      y = this.parent[node];
      while ((y != NIL) && (node == this.right[y])) {
        node = y;
        y = this.parent[y];
      }
      node = y;
    }
    return node;
  }

  private final int previousNode(int node) {
    if (this.left[node] != NIL) {
      node = this.left[node];
      while (this.right[node] != NIL) {
        node = this.right[node];
      }
    } else {
      while (isLeftDtr(node)) {
        node = this.parent[node];
      }
      if (node == this.root) {
        return NIL;
      }
      // node is now a left dtr, so we can go one up.
      node = this.parent[node];
    }
    return node;
  }

  public boolean deleteKey(int aKey) {
    int node = findKey(aKey);
    if (node == NIL) {
      return false;
    }
    deleteNode(node);
    --this.size;
    return true;
  }

  private void deleteNode(int z) {
    int x, y;
    if ((this.left[z] == NIL) || (this.right[z] == NIL)) {
      y = z;
    } else {
      y = nextNode(z);
    }
    if (this.left[y] != NIL) {
      x = this.left[y];
    } else {
      x = this.right[y];
    }
    this.parent[x] = this.parent[y];
    if (this.parent[y] == NIL) {
      setAsRoot(x);
    } else {
      if (y == this.left[this.parent[y]]) {
        this.left[this.parent[y]] = x;
      } else {
        this.right[this.parent[y]] = x;
      }
    }
    if (y != z) {
      this.key[z] = this.key[y];
    }
    if (this.color[y] == black) {
      deleteFixup(x);
    }
  }

  private void deleteFixup(int x) {
    int w;
    while ((x != this.root) && (this.color[x] == black)) {
      if (x == this.left[this.parent[x]]) {
        w = this.right[this.parent[x]];
        if (this.color[w] == red) {
          this.color[w] = black;
          this.color[this.parent[x]] = red;
          leftRotate(this.parent[x]);
          w = this.right[this.parent[x]];
        }
        if ((this.color[this.left[w]] == black) && (this.color[this.right[w]] == black)) {
          this.color[w] = red;
          x = this.parent[x];
        } else {
          if (this.color[this.right[w]] == black) {
            this.color[this.left[w]] = black;
            this.color[w] = red;
            rightRotate(w);
            w = this.right[this.parent[x]];
          }
          this.color[w] = this.color[this.parent[x]];
          this.color[this.parent[x]] = black;
          this.color[this.right[w]] = black;
          leftRotate(this.parent[x]);
          x = this.root;
        }
      } else {
        w = this.left[this.parent[x]];
        if (this.color[w] == red) {
          this.color[w] = black;
          this.color[this.parent[x]] = red;
          rightRotate(this.parent[x]);
          w = this.left[this.parent[x]];
        }
        if ((this.color[this.left[w]] == black) && (this.color[this.right[w]] == black)) {
          this.color[w] = red;
          x = this.parent[x];
        } else {
          if (this.color[this.left[w]] == black) {
            this.color[this.right[w]] = black;
            this.color[w] = red;
            leftRotate(w);
            w = this.left[this.parent[x]];
          }
          this.color[w] = this.color[this.parent[x]];
          this.color[this.parent[x]] = black;
          this.color[this.left[w]] = black;
          rightRotate(this.parent[x]);
          x = this.root;
        }
      }
    }
    this.color[x] = black;
  }

  /**
   * Method iterator.
   * 
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

  // ///////////////////////////////////////////////////////////////////////////
  // Debug utilities

  public boolean satisfiesRedBlackProperties() {
    // Compute depth of black nodes.
    int node = this.root;
    int blackDepth = 0;
    while (node != NIL) {
      if (this.color[node] == black) {
        ++blackDepth;
      }
      node = this.left[node];
    }
    return satisfiesRBProps(this.root, blackDepth, 0);
  }

  private boolean satisfiesRBProps(int node, final int blackDepth, int currentBlack) {
    if (node == NIL) {
      return (currentBlack == blackDepth);
    }
    if (this.color[node] == red) {
      if (this.color[this.left[node]] == red || this.color[this.right[node]] == red) {
        return false;
      }
    } else {
      ++currentBlack;
    }
    return (satisfiesRBProps(this.left[node], blackDepth, currentBlack) && satisfiesRBProps(
            this.right[node], blackDepth, currentBlack));
  }

  public int maxDepth() {
    return maxDepth(this.root, 0);
  }

  public int minDepth() {
    return minDepth(this.root, 0);
  }

  public int nodeDepth(int k) {
    return nodeDepth(this.root, 1, k);
  }

  private int nodeDepth(int node, int depth, int k) {
    if (node == NIL) {
      return -1;
    }
    if (k == this.key[node]) {
      return depth;
    } else if (k < this.key[node]) {
      return nodeDepth(this.left[node], depth + 1, k);
    } else {
      return nodeDepth(this.right[node], depth + 1, k);
    }
  }

  private int maxDepth(int node, int depth) {
    if (node == NIL) {
      return depth;
    }
    int depth1 = maxDepth(this.left[node], depth + 1);
    int depth2 = maxDepth(this.right[node], depth + 1);
    return (depth1 > depth2) ? depth1 : depth2;
  }

  private int minDepth(int node, int depth) {
    if (node == NIL) {
      return depth;
    }
    int depth1 = maxDepth(this.left[node], depth + 1);
    int depth2 = maxDepth(this.right[node], depth + 1);
    return (depth1 > depth2) ? depth2 : depth1;
  }

  public final void printKeys() {
    if (this.size() == 0) {
      System.out.println("Tree is empty.");
      return;
    }
    StringBuffer buf = new StringBuffer();
    printKeys(this.root, 0, buf);
    System.out.println(buf);
  }

  private final void printKeys(int node, int offset, StringBuffer buf) {
    if (node == NIL) {
      // StringUtils.printSpaces(offset, buf);
      // buf.append("NIL\n");
      return;
    }
    StringUtils.printSpaces(offset, buf);
    buf.append(Integer.toString(this.key[node]));
    if (this.color[node] == black) {
      buf.append(" BLACK");
    }
    buf.append("\n");
    printKeys(this.left[node], offset + 2, buf);
    printKeys(this.right[node], offset + 2, buf);
  }

  public static void main(String[] args) {
    System.out.println("Constructing tree.");
    IntArrayRBT tree = new IntArrayRBT();
    tree.insertKeyWithDups(2);
    tree.insertKeyWithDups(1);
    // assert(tree.color[0] == black);
    // assert(tree.size() == 0);
    // assert(tree.insertKey(5) == 1);
    // assert(tree.size() == 1);
    // assert(tree.insertKeyWithDups(5) == 2);
    // assert(tree.insertKey(3) == 3);
    // assert(tree.size() == 3);
    // assert(tree.insertKey(4) == 4);
    // assert(tree.size() == 4);
    // assert(tree.insertKey(2) == 5);
    // assert(tree.size() == 5);
    // tree.printKeys();
    // System.out.println("Constructing tree.");
    // tree = new IntArrayRBT();
    // int max = 10;
    // for (int i = 1; i <= max; i++) {
    // tree.insertKeyWithDups(i);
    // }
    // for (int i = 1; i <= max; i++) {
    // tree.insertKeyWithDups(i);
    // }
    // tree.printKeys();
    // tree = new IntArrayRBT();
    // max = 100;
    // // System.out.println("Creating tree.");
    // for (int i = 1; i <= max; i++) {
    // tree.insertKeyWithDups(1);
    // }
    // // System.out.println("Printing tree.");
    // tree.printKeys();
    // // IntIterator it = tree.iterator();
    // // int numElements = 0;
    // // while (it.hasNext()) {
    // // it.next();
    // // ++numElements;
    // // }
  }

}
