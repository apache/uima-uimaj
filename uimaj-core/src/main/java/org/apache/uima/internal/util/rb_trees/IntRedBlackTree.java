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


/**
 * map&lt;int, int%gt; uses separate objects (IntRBTNode) as nodes  
 * 
 * See the {@link org.apache.uima.internal.util.rb_trees.RedBlackTree RedBlackTree} class. This is a
 * specialized instance with ints as elements.
 * 
 * 
 */
public class IntRedBlackTree {

  // A note on the implementation: we closely follow CLR, down to
  // function and variable names. Places where we depart from CLR are
  // specifically commented in the code. The main difference is that
  // we don't use a NIL sentinel, but null pointers instead. This
  // makes the code somewhat less elegant in places. The meat of the
  // implementation is in IntRBTNode.

  // The root node of the tree.
  IntRBTNode root = null;

  // A counter to keep track of the size of the tree.
  int size = 0;

  /** Default constructor, does nothing. */
  public IntRedBlackTree() {
    super();
  }

  public final int size() {
    return this.size;
  }

  // ////////////////////////////////////////////////////////////////
  // Map interface methods //
  // ////////////////////////////////////////////////////////////////

  public final void clear() {
    this.root = null;
    this.size = 0;
  }

  public final boolean containsKey(int key) {
    return (IntRBTNode.find(this.root, key) == null) ? false : true;
  }

  public final boolean containsValue(int o) {
    IntRBTIterator it = this.iterator();
    while (it.hasNext()) {
      if (o == it.next()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Insert an object with a given key into the tree.
   * 
   * @param key
   *          The key under which the int is to be inserted.
   * @param el
   *          The int to be inserted.
   * @return <code>true</code>, if the key was not in the tree; <code>false</code>, if an
   *         element with that key was already in the tree. The old element is overwritten with the
   *         new one.
   */
  public final boolean put(int key, int el) {
    if (put(new IntRBTNode(key, el))) {
      this.size++;
      return true;
    }
    return false;
  }

  /**
   * Delete the node with the given key from the tree, if it exists.
   * 
   * @param key
   *          The key to be deleted.
   * @return -
   */
  public final int remove(int key) throws java.util.NoSuchElementException {
    IntRBTNode node = IntRBTNode.find(this.root, key);
    int ret;
    if (node != null) {
      ret = node.element;
      this.size--;
      IntRBTNode.delete(this, node);
    } else {
      throw new java.util.NoSuchElementException();
    }
    return ret;
  }

  public final int get(int key) throws java.util.NoSuchElementException {
    if (this.root == null) {
      throw new java.util.NoSuchElementException();
    }
    IntRBTNode node = IntRBTNode.find(this.root, key);
    if (node == null) {
      throw new java.util.NoSuchElementException();
    }
    return node.element;
  }

  public final boolean isEmpty() {
    return (this.root == null);
  }

  public final int[] keySet() {
    int[] set = new int[this.size];
    if (this.root != null) {
      this.root.keys(0, set);
    }
    return set;
  }

  /** Insert a IntRBTNode into the tree. Only used internally. */
  private final boolean put(IntRBTNode node) {
    return IntRBTNode.insert(this, node);
  }

  public final int getFirst() {
    return this.getFirstNode().element;
  }

  private final IntRBTNode getFirstNode() {
    if (this.root == null) {
      return null;
    }
    IntRBTNode x = this.root;
    while (x.left != null) {
      x = x.left;
    }
    return x;
  }

  public IntRBTIterator iterator() {
    return new IntRBTIterator(this);
  }

  public static class IntRBTIterator {

    IntRBTNode current;

    IntRBTIterator(IntRedBlackTree tree) {
      this.current = tree.getFirstNode();
    }

    public boolean hasNext() {
      return (this.current != null);
    }

    public int next() {
      if (this.current == null) {
        throw new java.util.NoSuchElementException();
      }
      int ret = this.current.element;
      this.current = this.current.successor();
      return ret;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /** Debugging aid. */
  public void printKeys() {
    if (this.root != null) {
      this.root.printKeys(0);
    }
    System.out.println("Size: " + this.size);
  }

  /**
   * Provides an array representation of the IntRedBlackTree. See
   * {@link org.apache.uima.internal.util.rb_trees.IntRBTArray IntRBTArray} for the memory layout of
   * the array. Note that the red-black information is lost in the translation. The resulting array
   * is only meant to be read, not grown. The array is meant as input to construct an
   * {@link org.apache.uima.internal.util.rb_trees.IntRBTArray IntRBTArray} object.
   * 
   * @param offset
   *          An offset for internal addressing. If <code>offset &gt; 0</code>, the addresses
   *          generated for right daughters in two-daughter nodes are shifted to the right. This is
   *          useful if the resulting array will be copied to a certain <code>offset</code>
   *          position in a different array.
   * @return The resulting array representation.
   */
  public int[] toArray(int offset) {
    if (this.root == null) {
      return new int[0];
    }
    return this.root.toArray(offset);
  }

  public IntRedBlackTree copy() {
    IntRedBlackTree c = new IntRedBlackTree();
    c.root = (null == root) ? null : root.copyNode(null);
    c.size = size;
    return c;
  }

  
}
