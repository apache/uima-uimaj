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

import java.util.Iterator;

import org.apache.uima.internal.util.BinaryTree;

/**
 * Map from int to T (Object)
 * 
 * An implementation of Red-Black Trees. This implementation follows quite closely the algorithms
 * described in Cormen, Leiserson and Rivest (1990): "Introduction to Algorithms" (henceforth CLR).
 * The main difference between our implementation and CLR is that our implementation does not allow
 * duplicate keys in a tree. Since we will generally use our implementation to represent sets, this
 * is a sensible restriction.
 * 
 * <p>
 * The difference between this implementation and {@link java.util.TreeMap TreeMap} is that we
 * assume that keys are ints. This should provide for a constant factor speed-up. We also assume
 * that we may copy this implementation to specialize for particular data element types.
 * 
 * <p>
 * This class implements most methods required for a {@link java.util.Map Map}. However, since we
 * use ints as keys, we can't implement the interface, as ints are not Objects, and so for example
 * {@link org.apache.uima.internal.util.rb_trees.RedBlackTree#containsKey
 * RedBlackTree.containsKey(int key)} does not specialize {@link java.util.Map#containsKey
 * Map.containsKey(Object key)}.
 * 
 * <p>
 * Note that this implementation is not thread-safe. A thread-safe version could easily be provided,
 * but would come with additional overhead.
 * 
 * 
 */
public class RedBlackTree<T> implements Iterable<T> {

  // A note on the implementation: we closely follow CLR, down to
  // function and variable names. Places where we depart from CLR are
  // specifically commented in the code. The main difference is that
  // we don't use a NIL sentinel, but null pointers instead. This
  // makes the code somewhat less elegant in places. The meat of the
  // implementation is in RBTNode.

  // The root node of the tree.
  RBTNode<T> root = null;

  // A counter to keep track of the size of the tree.
  int size = 0;

  /** Default constructor, does nothing. */
  public RedBlackTree() {
    super();
  }

  /**
   * @return The number of key/value pairs in the tree.
   */
  public final int size() {
    return this.size;
  }

  // //////////////////////////////////////////////////////////////////
  // Map interface methods
  // //////////////////////////////////////////////////////////////////

  /**
   * Remove all elements from the tree.
   */
  public final void clear() {
    this.root = null;
    this.size = 0;
  }

  /**
   * Checks if the key is contained in the tree.
   * 
   * @param key
   *          The key.
   * @return <code>true</code>, if key is defined; <code>false</code>, else.
   */
  public final boolean containsKey(int key) {
    return (RBTNode.find(this.root, key) == null) ? false : true;
  }

  /**
   * Check if the value object is contained in the tree. Inefficient, since it requires a traverse
   * of the tree.
   * 
   * @param o
   *          The value we want to check.
   * @return <code>true</code>, if value is there; <code>false</code>, else.
   */
  public final boolean containsValue(T o) {
    Iterator<T> it = this.iterator();
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
   *          The key under which the Object is to be inserted.
   * @param el
   *          The Object to be inserted.
   * @return <code>true</code>, if the key was not in the tree; <code>false</code>, if an
   *         element with that key was already in the tree. The old element is overwritten with the
   *         new one.
   */
  public final boolean put(int key, T el) {
    if (put(new RBTNode<T>(key, el))) {
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
  public final T remove(int key) {
    RBTNode<T> node = RBTNode.find(this.root, key);
    T ret = null;
    if (node != null) {
      ret = node.element;
      this.size--;
      RBTNode.delete(this, node);
    }
    return ret;
  }

  /**
   * Get the object for a key. If the key is not contained in the tree, returns <code>null</code>.
   * Since <code>null</code> can also be a regular value, use {@link
   * org.apache.uima.internal.util.rb_trees.RedBlackTree#containsKey containsKey()} to check if a
   * key is defined or not.
   * 
   * @param key
   *          The key.
   * @return The corresponding element, or <code>null</code> if key is not defined.
   */
  public final T get(int key) {
    if (this.root == null) {
      return null;
    }
    RBTNode<T> node = RBTNode.find(this.root, key);
    if (node == null) {
      return null;
    }
    return node.element;
  }

  /**
   * Check if the map is empty.
   * 
   * @return <code>true</code> if map is empty; <code>false</code>, else.
   */
  public final boolean isEmpty() {
    return (this.root == null);
  }

  /**
   * Return the set of keys as a sorted array.
   * 
   * @return A sorted array of the keys.
   */
  public final int[] keySet() {
    int[] set = new int[this.size];
    if (this.root != null) {
      this.root.keys(0, set);
    }
    return set;
  }

  /** Insert a RBTNode into the tree. Only used internally. */
  private final boolean put(RBTNode<T> node) {
    return RBTNode.insert(this, node);
  }

  /**
   * @return The object associated with the smallest key, or <code>null</code> if the tree is
   *         empty.
   */
  public final T getFirst() {
    return this.getFirstNode().element;
  }

  private final RBTNode<T> getFirstNode() {
    if (this.root == null) {
      return null;
    }
    RBTNode<T> x = this.root;
    while (x.left != null) {
      x = x.left;
    }
    return x;
  }

  /**
   * @return An iterator over the elements in the tree. The elements are returned in ascending order
   *         of the corresponding keys.
   */
  public Iterator<T> iterator() {
    return new RBTIterator<T>(this);
  }

  // Iterator implementation.
  private static class RBTIterator<T> implements java.util.Iterator<T> {

    RBTNode<T> current;

    RBTIterator(RedBlackTree<T> tree) {
      this.current = tree.getFirstNode();
    }

    public boolean hasNext() {
      return (this.current != null);
    }

    public T next() {
      if (this.current == null) {
        throw new java.util.NoSuchElementException();
      }
      T ret = this.current.element;
      this.current = this.current.successor();
      return ret;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * @return a copy of the red-black tree as a BinaryTree. The node values are key-value pairs
   * (RBTKeyValuePair).
   */
  public BinaryTree getBinaryTree() {
    if (this.root == null) {
      return null;
    }
    BinaryTree tree = new BinaryTree();
    this.root.getBinaryTree(tree);
    return tree;
  }

  // public java.util.Iterator preorderIterator() {
  // if (this.root != null) {
  // return this.root.preorderIterator();
  // } else {
  // return new src.util.EmptyIterator();
  // }
  // }

  /** Debugging aid. */
  public void printKeys() {
    if (this.root != null) {
      this.root.printKeys(0);
    }
    System.out.println("Size: " + this.size);
  }

  public static void main(String[] args) {
    RedBlackTree<String> tree = new RedBlackTree<String>();
    tree.put(1, "a");
    tree.printKeys();
    System.out.println("");

    tree.put(2, "b");
    tree.printKeys();
    System.out.println("");

    tree.put(3, "c");
    tree.printKeys();
    System.out.println("");

    tree.put(4, "d");
    tree.printKeys();
    System.out.println("");

    tree.put(5, "e");
    tree.printKeys();
    System.out.println("");

    tree.put(6, "f");
    tree.printKeys();
    System.out.println("");

    tree.put(7, "g");
    tree.printKeys();
    System.out.println("");

    tree.put(8, "h");
    tree.printKeys();
    System.out.println("");

    tree.put(9, "i");
    tree.printKeys();
    System.out.println("");

    tree.put(10, "j");
    tree.printKeys();
    System.out.println("");

    tree.put(3, "k");
    tree.printKeys();
    System.out.println("");

    int[] a = tree.keySet();
    System.out.print('[');
    for (int i = 0; i < a.length; i++) {
      if (i != 0) {
        System.out.print(',');
      }
      System.out.print(a[i]);
    }
    System.out.println(']');

    tree.remove(1);
    tree.printKeys();
    System.out.println("");

    tree.remove(2);
    tree.printKeys();
    System.out.println("");

    tree.remove(3);
    tree.printKeys();
    System.out.println("");

    tree.remove(4);
    tree.printKeys();
    System.out.println("");

    tree.remove(5);
    tree.printKeys();
    System.out.println("");

    tree.remove(6);
    tree.printKeys();
    System.out.println("");

    // tree.root.printElements(0);

    a = tree.keySet();
    System.out.print('[');
    for (int i = 0; i < a.length; i++) {
      if (i != 0) {
        System.out.print(',');
      }
      System.out.print(a[i]);
    }
    System.out.println(']');

    return;
  }

}
