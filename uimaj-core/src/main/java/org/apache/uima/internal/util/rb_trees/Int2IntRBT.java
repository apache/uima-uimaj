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

import org.apache.uima.internal.util.IntKeyValueIterator;
import org.apache.uima.internal.util.IntListIterator;

// @formatter:off
/**
 * A map&lt;int, int&gt;
 * 
 * uses IntArrayRBTcommon
 * 
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
//@formatter:on
public class Int2IntRBT extends IntArrayRBTcommon {

  private class KeyValueIterator implements IntKeyValueIterator {

    private int currentNode;

    private KeyValueIterator() {
      moveToFirst();
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#dec()
     */
    @Override
    public void dec() {
      currentNode = previousNode(currentNode);
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#get()
     */
    @Override
    public int get() {
      if (!isValid()) {
        throw new NoSuchElementException();
      }
      return getKeyForNode(currentNode);
    }

    @Override
    public int getValue() {
      if (!isValid()) {
        throw new NoSuchElementException();
      }
      return Int2IntRBT.this.getValue(currentNode);
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#inc()
     */
    @Override
    public void inc() {
      currentNode = nextNode(currentNode);
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#isValid()
     */
    @Override
    public boolean isValid() {
      return (currentNode != NIL);
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#moveToFirst()
     */
    @Override
    public void moveToFirst() {
      currentNode = getFirstNode();
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#moveToLast()
     */
    @Override
    public void moveToLast() {
      currentNode = Int2IntRBT.this.greatestNode;
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#copy()
     */
    @Override
    public Object copy() {
      KeyValueIterator it = new KeyValueIterator();
      it.currentNode = currentNode;
      return it;
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#moveTo(int)
     */
    @Override
    public void moveTo(int i) {
      currentNode = findInsertionPointNoDups(i);
    }

  }

  private class KeyIterator implements IntListIterator {

    private int currentNode;

    private KeyIterator() {
      currentNode = getFirstNode();
    }

    @Override
    public final boolean hasNext() {
      return currentNode != NIL;
    }

    @Override
    public final int nextNvc() {
      int v = getKeyForNode(currentNode);
      currentNode = nextNode(currentNode);
      return v;
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#hasPrevious()
     */
    @Override
    public boolean hasPrevious() {
      return currentNode != NIL && currentNode != getFirstNode();
    }

    @Override
    public int previousNvc() {
      currentNode = previousNode(currentNode);
      return getKeyForNode(currentNode);
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#moveToEnd()
     */
    @Override
    public void moveToEnd() {
      currentNode = Int2IntRBT.this.greatestNode;
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#moveToStart()
     */
    @Override
    public void moveToStart() {
      currentNode = NIL;
    }

  }

  protected int[] values;

  private int getValue(int node) {
    return values[node];
  }

  private int prevValue;

  private int lastNodeGotten; // a speed up, maybe

  /**
   * Constructor for IntArrayRBT.
   */
  public Int2IntRBT() {
    super(default_size);
  }

  public Int2IntRBT(int initialSize) {
    super(initialSize);
  }

  @Override
  protected void setupArrays() {
    super.setupArrays();
    values = new int[initialSize];
  }

  public Int2IntRBT copy() {
    Int2IntRBT c = new Int2IntRBT();
    c.klrp = klrp.clone();
    c.klrp1 = (klrp1 != null) ? klrp1.clone() : null;
    c.klrp2 = (klrp2 != null) ? klrp2.clone() : null;
    c.klrp3 = (klrp3 != null) ? klrp3.clone() : null;
    c.color = color.clone();
    c.values = values.clone();
    c.root = root;
    c.greatestNode = greatestNode;
    c.next = next;
    c.size = size;
    return c;
  }

  public void clear() {
    flush();
  }

  @Override
  public void flush() {
    super.flush();
    lastNodeGotten = NIL;
  }

  @Override
  protected void ensureCapacityKlrp(int requiredSize) {
    super.ensureCapacityKlrp(requiredSize);
    values = ensureArrayCapacity(values, requiredSize);
  }

  /**
   * 
   * @param k
   *          the key to insert
   * @param v
   *          the value to insert (or replace, if key already present)
   * @return negative index if key is found
   */
  private int treeInsert(final int k, final int v) {
    if ((greatestNode != NIL) && (getKeyForNode(greatestNode) < k)) {
      final int y = greatestNode;
      final int z = newNode(k);
      values[z] = v; // addition
      greatestNode = z;
      setRight(y, z);
      setParent(z, y);
      return z;
    }
    int x = root;
    int y = NIL;
    while (x != NIL) {
      y = x;
      final int xKey = getKeyForNode(x);
      if (k == xKey) {
        // key found
        prevValue = values[x]; // addition
        values[x] = v; // addition
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
      greatestNode = z;
      setParent(z, NIL);
    } else {
      setParent(z, y);
      if (k < getKeyForNode(y)) {
        setLeft(y, z);
      } else {
        setRight(y, z);
      }
    }
    return z;
  }

  /**
   * Get the value for a given key
   * 
   * @param k
   *          -
   * @return the value
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
   * adds a k, v pair. if k already present, replaces v. returns previous value, or 0 if no prev
   * value
   * 
   * @param k
   *          -
   * @param v
   *          -
   * @return previous value or 0 if key not previously present
   */
  public int put(final int k, final int v) {
    if (root == NIL) {
      final int x = newNode(k);
      values[x] = v;
      setAsRoot(x);
      color[root] = black;
      greatestNode = x;
      return 0;
    }
    int x = treeInsert(k, v);
    if (x < NIL) {
      return prevValue;
    }

    // inserted a new key, no previous value
    color[x] = red;
    while ((x != root) && (color[getParent(x)] == red)) {
      final int parent_x = getParent(x);
      final int parent_parent_x = getParent(parent_x);
      if (parent_x == getLeft(parent_parent_x)) {
        final int y = getRight(parent_parent_x);
        if (color[y] == red) {
          color[parent_x] = black;
          color[y] = black;
          color[parent_parent_x] = red;
          x = parent_parent_x;
        } else {
          if (x == getRight(parent_x)) {
            x = parent_x;
            leftRotate(x);
          }
          final int parent2_x = getParent(x);
          color[parent2_x] = black;
          final int parent2_parent2_x = getParent(parent2_x);
          color[parent2_parent2_x] = red;
          rightRotate(parent2_parent2_x);
        }
      } else {
        final int y = getLeft(parent_parent_x);
        if (color[y] == red) {
          color[parent_x] = black;
          color[y] = black;
          color[parent_parent_x] = red;
          x = parent_parent_x;
        } else {
          if (x == getLeft(parent_x)) {
            x = parent_x;
            rightRotate(x);
          }
          final int parent2_x = getParent(x);
          color[parent2_x] = black;
          final int parent2_parent2_x = getParent(parent2_x);
          color[parent2_parent2_x] = red;
          leftRotate(parent2_parent2_x);
        }
      }
    }
    color[root] = black;
    return 0;
  }

  // private final boolean isNewNode(int node) {
  // return (node == (next - 1));
  // }

  /**
   * Fast version of findKey Keeps the last node referenced
   * 
   * *** NOT THREAD SAFE ***
   * 
   * Tries to shorten the search path, conditionally
   * 
   * @param k
   *          -
   * @return -
   */
  protected int findKeyFast(final int k) {
    final int node;
    if (lastNodeGotten == NIL) {
      node = findKey(k);
    } else {
      final int distanceToTop = Math.abs(k - getKeyForNode(root));
      final int distanceToLast = Math.abs(k - getKeyForNode(lastNodeGotten));
      node = (distanceToTop < distanceToLast) ? findKey(k) : findKeyFromLast(k);
    }
    if (node != NIL) {
      lastNodeGotten = node;
    }
    return node;
  }

  private int findKeyFromLast(final int k) {
    int node = lastNodeGotten;
    int keyNode = getKeyForNode(node);
    int prevNode;
    if (k < keyNode) {
      do {
        prevNode = node;
        node = getParent(node);
        if (node == NIL) {
          break;
        }
        keyNode = getKeyForNode(node);
      } while (k < keyNode);
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
        keyNode = getKeyForNode(node);
      } while (k > keyNode);
      if (keyNode == k) {
        return node;
      }
      return findKeyDown(k, prevNode);
    }
    return node;
  }

  // /**
  // * Find the node such that key[node] >= k and key[previous(node)] < k.
  // */
  // private int findInsertionPoint(final int k) {
  // int node = this.root;
  // int found = node;
  // while (node != NIL) {
  // found = node;
  // final int keyNode = getKeyForNode(node);
  // if (k < keyNode) {
  // node = getLeft(node);
  // } else if (k == keyNode) {
  // // In the presence of duplicates, we have to check if there are
  // // identical
  // // keys to the left of us.
  // while (true) {
  // final int left_node = getLeft(node);
  // if ((left_node == NIL) ||
  // (getKeyForNode(left_node) != keyNode)) {
  // break;
  // }
  // node = left_node;
  // }
  //// while ((getLeft(node) != NIL) && (getKeyForNode(getLeft(node)) == keyNode)) {
  //// node = getLeft(node);
  //// }
  // return node;
  // } else {
  // node = getRight(node);
  // }
  // }
  // // node == NIL
  // return found;
  // }

  // private final boolean isLeftDtr(int node) {
  // return ((node != this.root) && (node == getLeft(getParent(node))));
  // }

  // private final boolean isRightDtr(int node) {
  // return ((node != root) && (node == right[parent[node]]));
  // }

  public IntListIterator keyIterator() {
    return new KeyIterator();
  }

  public IntListIterator keyIterator(int aKey) {
    KeyIterator it = new KeyIterator();
    it.currentNode = findKey(aKey);
    return it;
  }

  public IntKeyValueIterator keyValueIterator() {
    return new KeyValueIterator();
  }

  public IntKeyValueIterator keyValueIterator(int aKey) {
    KeyValueIterator it = new KeyValueIterator();
    it.currentNode = findKey(aKey);
    return it;
  }

}
