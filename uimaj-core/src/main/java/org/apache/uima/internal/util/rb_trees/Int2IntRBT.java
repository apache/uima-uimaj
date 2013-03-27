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

import org.apache.uima.internal.util.IntArrayUtils;
import org.apache.uima.internal.util.IntKeyValueIterator;
import org.apache.uima.internal.util.IntListIterator;
import org.apache.uima.internal.util.StringUtils;

/**
 * Int to Int Map, based on IntArrayRBT, used in no-duplicates mode
 * 
 * Implements Map - like interface:
 *   keys and values are ints
 *   Entry set not (yet) impl
 *   
 *   no keySet()
 *   no values()
 *   
 */
public class Int2IntRBT {

  private class KeyValueIterator implements IntKeyValueIterator {

    private int currentNode;

    private KeyValueIterator() {
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
      return Int2IntRBT.this.getKey(this.currentNode);
    }
    
    public int getValue() {
      if (!isValid()) {
        throw new NoSuchElementException();
      }
      return Int2IntRBT.this.getValue(this.currentNode);      
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
      this.currentNode = Int2IntRBT.this.greatestNode;
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#copy()
     */
    public Object copy() {
      KeyValueIterator it = new KeyValueIterator();
      it.currentNode = this.currentNode;
      return it;
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#moveTo(int)
     */
    public void moveTo(int i) {
      this.currentNode = findInsertionPointNoDups(i);
    }

  }

  private class KeyIterator implements IntListIterator {

    private int currentNode;

    private KeyIterator() {
      super();
      this.currentNode = NIL;
    }

    public final boolean hasNext() {
      return (this.currentNode != Int2IntRBT.this.greatestNode);
    }

    public final int next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      this.currentNode = (this.currentNode == NIL) ? getFirstNode() : nextNode(this.currentNode);
      return Int2IntRBT.this.getKey(this.currentNode);
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
      final int currentKey = Int2IntRBT.this.getKey(this.currentNode);
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
      this.currentNode = Int2IntRBT.this.greatestNode;
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#moveToStart()
     */
    public void moveToStart() {
      this.currentNode = NIL;
    }

    private final int getKey(int node) {
      return Int2IntRBT.this.getKey(node);
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

  /**
   * Given a node, get the key
   * @param node
   * @return
   */
  private int getKey(int node) {
    if (useklrp) {
      return getXXX(node, 0);
    }
    return key[node];
  }
  
  private int getValue(int node) {
    return values[node];
  }

  private int setKey(int node, int value) {
    if (useklrp) {
      return setXXX(node, 0, value);
    }
    return key[node] = value;
  }
  
  private void setValue(int node, int v) {
    values[node] = v;
  }
  
  private int getLeft(int node) {
    if (useklrp) {
      return getXXX(node, 1);
    }
    return left[node];
  }
  
  private int setLeft(int node, int value) {
    if (useklrp) {
      return setXXX(node, 1, value);
    }
    return left[node] = value;
  }
  
  private int getRight(int node) {
    if (useklrp) {
      return getXXX(node, 2);
    }
    return right[node];
  }
  
  private int setRight(int node, int value) {
    if (useklrp) {
      return setXXX(node, 2, value);
    }
    return right[node] = value;
  }
  
  private int getParent(int node) {
    if (useklrp) {
      return getXXX(node, 3);
    }
    return parent[node];
  }
  
  private int setParent(int node, int value) {
    if (useklrp) {
      return setXXX(node, 3, value);
    }
    return parent[node] = value;
  }
  
  private int[] values;
  
  private int prevValue;
  
  // Colors.
  private boolean[] color;

  // The index of the next node.
  private int next;

  // The current size of the tree. Since we can remove nodes, since needs to
  // be kept separate from the next free cell.
  private int size;

  // The root of the tree.
  private int root;
  
  private int lastNodeGotten;  // a speed up, maybe

  // Keep a pointer to the largest node around so we can optimize for
  // inserting
  // keys that are larger than all keys already in the tree.
  private int greatestNode;

  private static final int default_size = 1024;

  private static final int default_growth_factor = 2;

  private static final int default_multiplication_limit = 2000000;

  private final int growth_factor;

  private final int multiplication_limit;

  // The NIL sentinel
  public static final int NIL = 0;

  // The colors.
  private static final boolean red = true;

  private static final boolean black = false;

  /**
   * Constructor for IntArrayRBT.
   */
  public Int2IntRBT() {
    this(default_size);
  }

  public Int2IntRBT(int initialSize) {
    super();
    if (initialSize < 1) {
      initialSize = 1;
    }
    initVars();
    // Increase initialSize by one since we use one slot for sentinel.
    ++initialSize;
    this.growth_factor = default_growth_factor;
    this.multiplication_limit = default_multiplication_limit;
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
    this.values = new int[initialSize];
    setLeft(NIL, NIL);
    setRight(NIL, NIL);
    setParent(NIL, NIL);
    this.color[NIL] = black;
  }
  
  public Int2IntRBT copy() {
    Int2IntRBT c = new Int2IntRBT();
    if (useklrp) {
      c.klrp = klrp.clone();
      c.klrp1 = (klrp1 != null) ? klrp1.clone() : null;
      c.klrp2 = (klrp2 != null) ? klrp2.clone() : null;
      c.klrp3 = (klrp3 != null) ? klrp3.clone() : null;
    } else {
      c.key = key.clone();
      c.left = left.clone();
      c.right = right.clone();
      c.parent = parent.clone();
    }
    c.color = color.clone();
    c.values = values.clone();
    c.root = root;
    c.greatestNode = greatestNode;
    c.next = next;
    c.size = size;
    return c;
  }

  private void initVars() {
    this.root = NIL;
    this.greatestNode = NIL;
    this.next = 1;
    this.size = 0;
  }

  public void clear() {
    // All we do for flush is set the root to NIL and the size to 0.  Doesn't release space
    initVars();
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
    this.values = growPlainIntArray(this.values, requiredSize);
  }
  
  /**
   * 
   * @param k
   * @return negative index if key is found
   */
  private int treeInsert(final int k, final int v) {
    if ((this.greatestNode != NIL) && (getKey(this.greatestNode) < k)) {
      final int y = this.greatestNode;
      final int z = newNode(k);
      values[z] = v;
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
        // key found
        prevValue = values[x];
        values[x] = v;
        return -x;
      }
      x = (k < xKey) ? getLeft(x) : getRight(x);
    }
    // The key was not found, so we create a new node, inserting the
    // key.
    final int z = newNode(k);
    values[z] = v;
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


  private int newNode(final int k) {
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

  /**
   * 
   * @param array - the array to expand - may be klrp0, 1, 2, 3, etc.
   * @param newSize = the total size - if in parts, the size of the part
   * @return
   */
  private final int[] grow(final int[] array, final int newSize) {
    if (useklrp) {
      if (newSize < MAXklrp0) {
        return IntArrayUtils.ensure_size(array, newSize, this.growth_factor, this.multiplication_limit);
      } else {
        throw new RuntimeException(); // never happen
      }      
    }
    return growPlainIntArray(array, newSize);
  }
  
  private final int[] growPlainIntArray(int[] array, int newSize) {
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
  
  /**
   * Get the value for a given key
   * @param k
   * @return
   */
  public int get(final int k) {
    final int node = findKey(k);
    if (node == NIL) {
      return 0;
    }
    return values[node];
  }
  
  public int getMostlyClose(final int k) {
    final int node = findKeyFast(k);
    if (node == NIL) {
      return 0;
    }
    return values[node];
  }
      
  /**
   * adds a k, v pair.  
   *   if k already present, replaces v.
   *   returns previous value, or 0 if no prev value
   * @param k
   * @return previous value or 0 if key not previously present
   */
  public int put(final int k, final int v) {
    if (this.root == NIL) {
      final int x = newNode(k);
      values[x] = v;
      setAsRoot(x);
      this.color[this.root] = black;
      this.greatestNode = x;
      return 0;
    }
    int x = treeInsert(k, v);
    if (x < NIL) {
      return prevValue;
    }

    // inserted a new key, no previous value
    this.color[x] = red;
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
    return 0;
  }

  // private final boolean isNewNode(int node) {
  // return (node == (next - 1));
  // }

  /**
   * Fast version of findKey
   *   Keeps the last node referenced
   *   *** NOT THREAD SAFE ***
   *   
   *   Tries to shorten the search path, conditionally
   */
  
  private int findKeyFast(final int k) {
    final int node;
    if (lastNodeGotten == NIL) {
      node = findKey(k);
    } else {
      final int distanceToTop = Math.abs(k - getKey(this.root));
      final int distanceToLast = Math.abs(k - getKey(this.lastNodeGotten));
      node = (distanceToTop < distanceToLast) ? findKey(k) : findKeyFromLast(k);
    }
    if (node != NIL) {
      lastNodeGotten = node;
    }
    return node;
  }
  
 
  /**
   * 
   */
  
  private int findKeyFromLast(final int k) {
    int node = lastNodeGotten;
    int keyNode = getKey(node);
    int prevNode;
    if (k < keyNode) {
      do {
        prevNode = node;
        node = getParent(node);
        if (node == NIL) {
          break;
        }
        keyNode = getKey(node);
      } while  (k < keyNode);
      if (keyNode == k) {
        return node;
      }
      return findKeyDown(k, prevNode);
    }
    if (k > keyNode) {
      do {
        prevNode = node;
        node = getParent(node);
        if (node == NIL) {
          break;
        }
        keyNode = getKey(node);
      } while (k > keyNode);
      if (keyNode == k) {
        return node;
      }
      return findKeyDown(k, prevNode);        
    }
    return node;
  }
  
  /**
   * Find the first node such that k &lt;= key[node].
   */
  private int findKey(final int k) {
    return findKeyDown(k, this.root);
  }
  
  private int findKeyDown(final int k, int node) {
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

//  /**
//   * Find the node such that key[node] >= k and key[previous(node)] < k.
//   */
//  private int findInsertionPoint(final int k) {
//    int node = this.root;
//    int found = node;
//    while (node != NIL) {
//      found = node;
//      final int keyNode = getKey(node);
//      if (k < keyNode) {
//        node = getLeft(node);
//      } else if (k == keyNode) {
//        // In the presence of duplicates, we have to check if there are
//        // identical
//        // keys to the left of us.
//        while (true) {
//          final int left_node = getLeft(node);
//          if ((left_node == NIL) ||
//              (getKey(left_node) != keyNode)) {
//            break;
//          }
//          node = left_node;
//        }
////        while ((getLeft(node) != NIL) && (getKey(getLeft(node)) == keyNode)) {
////          node = getLeft(node);
////        }
//        return node;
//      } else {
//        node = getRight(node);
//      }
//    }
//    // node == NIL
//    return found;
//  }

  /**
   * Find the node such that key[node] >= k and key[previous(node)] < k.
   */
  private int findInsertionPointNoDups(final int k) {
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

  private final int nextNode(int node) {
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

//  public boolean deleteKey(int aKey) {
//    final int node = findKey(aKey);
//    if (node == NIL) {
//      return false;
//    }
//    deleteNode(node);
//    --this.size;
//    return true;
//  }

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
      setKey(z, y);
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

  public IntListIterator keyIterator() {
    return new KeyIterator();
  }
  
  public IntListIterator keyIterator(int aKey) {
    KeyIterator it = new KeyIterator();
    it.currentNode = this.findKey(aKey);
    return it;
  }

  public IntKeyValueIterator keyValueIterator() {
    return new KeyValueIterator();
  }

  public IntKeyValueIterator keyValueIterator(int aKey) {
    KeyValueIterator it = new KeyValueIterator();
    it.currentNode = this.findKey(aKey);
    return it;
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Debug utilities

  private boolean satisfiesRedBlackProperties() {
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
    Int2IntRBT tree = new Int2IntRBT();
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
