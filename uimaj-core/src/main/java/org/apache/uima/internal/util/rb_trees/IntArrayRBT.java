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
          IntArrayRBT.this.getKey(it.currentNode));
    }

  }

  static final private boolean useklrp = true;
  // Keys.
  private int[] key;

  // Left daughters.
  private int[] left;

  // Right daughters.
  private int[] right;

  // Parents.
  private int[] parent;
  
  // alternate layout
  private int[] klrp;
  // the next 3 are for the rare cases where the number of entries
  // in this instance exceeds 512 * 1024 * 1024 - 1
  // which is the largest index that can be stored in klrp
  //   because it is shifted left by 2
  private int[] klrp1;
  private int[] klrp2;
  private int[] klrp3;
  private static final int MAXklrp0 = 512 * 1024 * 1024;
  private static final int MAXklrpMask = MAXklrp0 - 1;

    
  private int getXXX(int node, int offset) {
    if (node < MAXklrp0) {
      return klrp[(node << 2) + offset];
    } else {
      final int w = node >> 29;
      final int i = ((node & MAXklrpMask) << 2) + offset;
      switch (w) {
      case 1:
        return klrp1[i];
      case 2:
        return klrp2[i];
      case 3:
        return klrp3[i];
      default:
        throw new RuntimeException();
      }
    }
  }

  private int setXXX(int node, int offset, int value) {
    if (node < MAXklrp0) {
//      if (((node << 2) + offset) >= klrp.length) {
//        System.out.println("caught");
//      }
      return klrp[(node << 2) + offset] = value;
    } else {
      final int w = node >> 29;
      final int i = ((node & MAXklrpMask) << 2) + offset;
      switch (w) {
      case 1:
        return klrp1[i] = value;
      case 2:
        return klrp2[i] = value;
      case 3:
        return klrp3[i] = value;
      default:
        throw new RuntimeException();
      }
    }
  }

  protected int getKey(int node) {
    if (useklrp) {
      return getXXX(node, 0);
    }
    return key[node];
  }

  protected int setKey(int node, int value) {
    if (useklrp) {
      return setXXX(node, 0, value);
    }
    return key[node] = value;
  }
  
  protected int getLeft(int node) {
    if (useklrp) {
      return getXXX(node, 1);
    }
    return left[node];
  }
  
  protected int setLeft(int node, int value) {
    if (useklrp) {
      return setXXX(node, 1, value);
    }
    return left[node] = value;
  }
  
  protected int getRight(int node) {
    if (useklrp) {
      return getXXX(node, 2);
    }
    return right[node];
  }
  
  protected int setRight(int node, int value) {
    if (useklrp) {
      return setXXX(node, 2, value);
    }
    return right[node] = value;
  }
  
  protected int getParent(int node) {
    if (useklrp) {
      return getXXX(node, 3);
    }
    return parent[node];
  }
  
  protected int setParent(int node, int value) {
    if (useklrp) {
      return setXXX(node, 3, value);
    }
    return parent[node] = value;
  }
  
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
  
  final private int initialSize;

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
    this.initialSize = initialSize;
    this.growth_factor = default_growth_factor;
    this.multiplication_limit = default_multiplication_limit;
    setupArrays();
    // Init the arrays.
    if (useklrp) {
      klrp = new int[initialSize << 2];
    } else {
      this.key = new int[initialSize];
      this.left = new int[initialSize];
      this.right = new int[initialSize];
      this.parent = new int[initialSize];
    }
    this.color = new boolean[initialSize];
    setLeft(NIL, NIL);
    setRight(NIL, NIL);
    setParent(NIL, NIL);
    this.color[NIL] = black;
  }
  
  private void setupArrays() {
    // Init the arrays.
    if (useklrp) {
      klrp = new int[initialSize << 2];
    } else {
      this.key = new int[initialSize];
      this.left = new int[initialSize];
      this.right = new int[initialSize];
      this.parent = new int[initialSize];
    }
    this.color = new boolean[initialSize];
    setLeft(NIL, NIL);
    setRight(NIL, NIL);
    setParent(NIL, NIL);
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
    // and potentially release extra storage
    if (useklrp) {
      if (klrp.length > (initialSize << 2)) {
        setupArrays();
      }
    } else {
      if (key.length > initialSize) {
        setupArrays();
      }
    }
  }

  public final int size() {
    return this.size;
  }

  private void grow(int requiredSize) {
    if (useklrp) {
      final int w = requiredSize >> 29;  // w is 0-3
      switch (w) {
      case 0:
        if (klrp == null) {
          klrp = new int[requiredSize << 2];
        } else {
          klrp = grow(klrp, requiredSize << 2);
        }
        break;
      case 1:
        if (klrp1 == null) {
          klrp1 = new int[(requiredSize & MAXklrpMask) << 2];
        } else {
          klrp1 = grow(klrp1, (requiredSize & MAXklrpMask) << 2);
        }
        break;
      case 2:
        if (klrp2 == null) {
          klrp2 = new int[(requiredSize & MAXklrpMask) << 2];
        } else {
          klrp2 = grow(klrp2, (requiredSize & MAXklrpMask) << 2);
        }
        break;
      case 3:
        if (klrp3 == null) {
          klrp3 = new int[(requiredSize & MAXklrpMask) << 2];
        } else {
          klrp3 = grow(klrp3, (requiredSize & MAXklrpMask) << 2);
        }
        break;
      default:
        throw new RuntimeException();
      }
    } else {
      this.key = grow(this.key, requiredSize);
      this.left = grow(this.left, requiredSize);
      this.right = grow(this.right, requiredSize);
      this.parent = grow(this.parent, requiredSize);
    }
    this.color = grow(this.color, requiredSize);
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
    int y = NIL;
    int x = this.root;
    y = NIL;
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

  protected int newNode(final int k) {
    // Make sure the tree is big enough to accomodate a new node.

    if (useklrp) {
      final int lenKlrp = (klrp.length >> 2) +
                    ((klrp1 != null) ? (klrp1.length >> 2) : 0) +
                    ((klrp2 != null) ? (klrp2.length >> 2) : 0) +
                    ((klrp3 != null) ? (klrp3.length >> 2) : 0);
      if (this.next >= lenKlrp) {
        grow(this.next + 1);
      }
    } else {
      if (this.next >= this.key.length) {
        grow(this.next + 1);
      }
    }
    // assert(key.length > next);
    final int z = this.next;
    ++this.next;
    ++this.size;
    setKey(z, k);
    setLeft(z, NIL);
    setRight(z, NIL);
    this.color[z] = red;
    return z;
  }

  private final void setAsRoot(int x) {
    this.root = x;
    setParent(this.root, NIL);
  }

  private final int[] grow(int[] array, int newSize) {
    if (useklrp) {
      if (newSize < MAXklrp0) {
        return IntArrayUtils.ensure_size(array, newSize, this.growth_factor, this.multiplication_limit);
      }
      final int w = newSize >> 29;
      switch (w) {
      case 1:
        if (klrp1 == null) {
          klrp1 = new int[newSize + 1];
        } else {
          
        }
        break;
      case 2:
        break;
      case 3:
        break;
      default:
        throw new RuntimeException();
      }
      
    }
    return IntArrayUtils.ensure_size(array, newSize, this.growth_factor, this.multiplication_limit);
  }

  private final boolean[] grow(boolean[] array, int newSize) {
    return IntArrayUtils.ensure_size(array, newSize, this.growth_factor, this.multiplication_limit);
  }

  private final void leftRotate(final int x) {
    final int y = getRight(x);
    final int left_of_y = getLeft(y);
    setRight(x, left_of_y );
    if (left_of_y  != NIL) {
      setParent(left_of_y , x);
    }
    setParent(y, getParent(x));
    if (this.root == x) {
      setAsRoot(y);
    } else {
      final int parent_x = getParent(x);
      if (x == getLeft(parent_x)) {
        setLeft(parent_x, y);
      } else {
        setRight(parent_x, y);
      }
    }
    setLeft(y, x);
    setParent(x, y);
  }

  private final void rightRotate(final int x) {
    final int y = getLeft(x);
    final int right_y = getRight(y);
    setLeft(x, right_y);
    if (right_y != NIL) {
      setParent(right_y, x);
    }
    final int parent_x = getParent(x);
    setParent(y, parent_x);
    if (this.root == x) {
      setAsRoot(y);
    } else {
      if (x == getRight(parent_x)) {
        setRight(parent_x, y);
      } else {
        setLeft(parent_x, y);
      }
    }
    setRight(y, x);
    setParent(x, y);
  }

  public int insertKey(int k) {
    return insertKey(k, false);
  }
  
  public int add(int k) {
    return insertKey(k, false);
  }
  
  /**
   * like add, but returns boolean flag true if not present before
   * @param k 
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
   * Find the node such that key[node] >= k and key[previous(node)] < k.
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

  /**
   * Find the node such that key[node] >= k and key[previous(node)] < k.
   */
  public int findInsertionPointNoDups(final int k) {
    int node = this.root;
    int found = node;
    while (node != NIL) {
      found = node;
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
    return found;
  }

  public final boolean containsKey(int k) {
    return (findKey(k) != NIL);
  }
  
  public final boolean contains(int k) {
    return (findKey(k) != NIL);
  }

//  private final boolean isLeftDtr(int node) {
//    return ((node != this.root) && (node == getLeft(getParent(node))));
//  }

  // private final boolean isRightDtr(int node) {
  // return ((node != root) && (node == right[parent[node]]));
  // }

  private final int getFirstNode() {
    if (this.root == NIL) {
      return NIL;
    }
    int node = this.root;
    while (true) {
      final int left_node = getLeft(node);
      if (left_node == NIL) {
        break;
      }
      node = left_node;
    }
//    while (getLeft(node) != NIL) {
//      node = getLeft(node);
//    }
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
    final int rightNode = getRight(node);
    if (rightNode != NIL) {
      node = rightNode;
      while (true) {
        final int leftNode = getLeft(node);
        if (leftNode == NIL) {
          break;
        }
        node = leftNode;
      }
//      while (getLeft(node) != NIL) {
//        node = getLeft(node);
//      }
    } else {
      y = getParent(node);
      while ((y != NIL) && (node == getRight(y))) {
        node = y;
        y = getParent(y);
      }
      node = y;
    }
    return node;
  }

  private final int previousNode(int node) {
    final int leftNode = getLeft(node);
    if (leftNode != NIL) {
      node = leftNode;
      while (true) {
        final int rightNode = getRight(node);
        if (rightNode == NIL) {
          break;
        }
        node = rightNode;
      }
//      while (getRight(node) != NIL) {
//        node = getRight(node);
//      }
    } else {
      while (true) {
        final int parentNode = getParent(node);
        if (node == this.root || (node != getLeft(parentNode))) {
          break;
        }
        node = parentNode;
      }
      
//      (node != this.root) && (node == getLeft(getParent(node))))
//      while (node != this.root && (node == getLeft(parentNode))) {
//        node = getParent(node);
//      }
      if (node == this.root) {
        return NIL;
      }
      // node is now a left dtr, so we can go one up.
      node = getParent(node);
    }
    return node;
  }

  public boolean deleteKey(int aKey) {
    final int node = findKey(aKey);
    if (node == NIL) {
      return false;
    }
    deleteNode(node);
    --this.size;
    return true;
  }

  private void deleteNode(final int z) {
    final int y = ((getLeft(z) == NIL) || (getRight(z) == NIL)) ?  z : nextNode(z);
//    if ((getLeft(z) == NIL) || (getRight(z) == NIL)) {
//      y = z;
//    } else {
//      y = nextNode(z);
//    }
    final int left_y = getLeft(y);
    final int x = (left_y != NIL) ? left_y : getRight(y);
//    if (left_y != NIL) {
//      x = left_y;
//    } else {
//      x = getRight(y);
//    }
    final int parent_y = getParent(y);
    setParent(x, parent_y);
    if (parent_y == NIL) {
      setAsRoot(x);
    } else {
      if (y == getLeft(parent_y)) {
        setLeft(parent_y, x);
      } else {
        setRight(parent_y, x);
      }
    }
    if (y != z) {
      setKey(z, getKey(y));
    }
    if (this.color[y] == black) {
      deleteFixup(x);
    }
  }

  private void deleteFixup(int x) {
    int w;
    while ((x != this.root) && (this.color[x] == black)) {
      final int parent_x = getParent(x);
      if (x == getLeft(parent_x)) {
        w = getRight(parent_x);
        if (this.color[w] == red) {
          this.color[w] = black;
          this.color[parent_x] = red;
          leftRotate(parent_x);
          w = getRight(parent_x);
        }
        if ((this.color[getLeft(w)] == black) && (this.color[getRight(w)] == black)) {
          this.color[w] = red;
          x = parent_x;
        } else {
          if (this.color[getRight(w)] == black) {
            this.color[getLeft(w)] = black;
            this.color[w] = red;
            rightRotate(w);
            w = getRight(parent_x);
          }
          this.color[w] = this.color[parent_x];
          this.color[parent_x] = black;
          this.color[getRight(w)] = black;
          leftRotate(parent_x);
          x = this.root;
        }
      } else {
        w = getLeft(parent_x);
        if (this.color[w] == red) {
          this.color[w] = black;
          this.color[parent_x] = red;
          rightRotate(parent_x);
          w = getLeft(parent_x);
        }
        if ((this.color[getLeft(w)] == black) && (this.color[getRight(w)] == black)) {
          this.color[w] = red;
          x = getParent(x);
        } else {
          if (this.color[getLeft(w)] == black) {
            this.color[getRight(w)] = black;
            this.color[w] = red;
            leftRotate(w);
            w = getLeft(getParent(x));
          }
          this.color[w] = this.color[parent_x];
          this.color[parent_x] = black;
          this.color[getLeft(w)] = black;
          rightRotate(parent_x);
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
      node = getLeft(node);
    }
    return satisfiesRBProps(this.root, blackDepth, 0);
  }

  private boolean satisfiesRBProps(int node, final int blackDepth, int currentBlack) {
    if (node == NIL) {
      return (currentBlack == blackDepth);
    }
    if (this.color[node] == red) {
      if (this.color[getLeft(node)] == red || this.color[getRight(node)] == red) {
        return false;
      }
    } else {
      ++currentBlack;
    }
    return (satisfiesRBProps(getLeft(node), blackDepth, currentBlack) && satisfiesRBProps(
            getRight(node), blackDepth, currentBlack));
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
    if (k == getKey(node)) {
      return depth;
    } else if (k < getKey(node)) {
      return nodeDepth(getLeft(node), depth + 1, k);
    } else {
      return nodeDepth(getRight(node), depth + 1, k);
    }
  }

  private int maxDepth(int node, int depth) {
    if (node == NIL) {
      return depth;
    }
    int depth1 = maxDepth(getLeft(node), depth + 1);
    int depth2 = maxDepth(getRight(node), depth + 1);
    return (depth1 > depth2) ? depth1 : depth2;
  }

  private int minDepth(int node, int depth) {
    if (node == NIL) {
      return depth;
    }
    int depth1 = maxDepth(getLeft(node), depth + 1);
    int depth2 = maxDepth(getRight(node), depth + 1);
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
    buf.append(Integer.toString(getKey(node)));
    if (this.color[node] == black) {
      buf.append(" BLACK");
    }
    buf.append("\n");
    printKeys(getLeft(node), offset + 2, buf);
    printKeys(getRight(node), offset + 2, buf);
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
