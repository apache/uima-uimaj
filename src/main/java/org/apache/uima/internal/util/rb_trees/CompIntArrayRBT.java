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

import org.apache.uima.internal.util.IntComparator;

/**
 * Class comment for CompIntArrayRBT.java goes here.
 * 
 * 
 */
public class CompIntArrayRBT extends IntArrayRBT {

  private IntComparator comp;

  public CompIntArrayRBT(IntComparator comp) {
    this(comp, default_size);
  }

  /**
   * Constructor for CompIntArrayRBT.
   * 
   * @param initialSize
   */
  public CompIntArrayRBT(IntComparator comp, int initialSize) {
    super(initialSize);
    this.comp = comp;
  }

  // Insert a node for key. Returns index of new node if node was inserted, or
  // index of old node for the key.
  protected int treeInsert(final int k) {
    int x = this.root;
    int y = NIL;
    int z;
//    int cv; // Return value of compare().
    if ((this.greatestNode != NIL) && (this.comp.compare(getKey(this.greatestNode), k) < 0)) {
      y = this.greatestNode;
      z = newNode(k);
      this.greatestNode = z;
    } else {
      while (x != NIL) {
        y = x;
        final int cv = this.comp.compare(k, getKey(x));
        if (cv < 0) {
          x = getLeft(x);
        } else if (cv > 0) {
          x = getRight(x);
        } else { // cv == 0
          return x;
        }
      }
      // The key was not found, so we create a new node, inserting the
      // key.
      z = newNode(k);
    }
    if (y == NIL) {
      this.root = z;
      this.greatestNode = z;
      setParent(z, NIL);
    } else {
      setParent(z, y);
      final int cv = this.comp.compare(k, getKey(y));
      if (cv < 0) {
        setLeft(y, z);
      } else {
        setRight(y, z);
      }
    }
    return z;
  }

  protected int treeInsertWithDups(final int k) {
    int x = this.root;
    int y, z;
    boolean wentLeft = false;
    if ((this.greatestNode != NIL) && (this.comp.compare(getKey(this.greatestNode), k) <= 0)) {
      y = this.greatestNode;
      z = newNode(k);
      this.greatestNode = z;
      setParent(z, y);
      setRight(y, z);
      return z;
    }
    y = NIL;
    int xKey;
    while (x != NIL) {
      y = x;
      xKey = getKey(x);
      final int cv = this.comp.compare(k, xKey);
      if (cv < 0) {
        x = getLeft(x);
      } else if (cv > 0) {
        x = getRight(x);
      } else { // k == key[x]
        // Randomly search to the left or right.
        // if (false) {
        if (this.rand.nextBoolean()) {
          wentLeft = true;
          x = getLeft(x);
        } else {
          wentLeft = false;
          x = getRight(x);
        }
      }
    }
    z = newNode(k);
    if (y == NIL) {
      this.root = z;
      this.greatestNode = z;
      setParent(z, NIL);
    } else {
      setParent(z, y);
      final int cv = this.comp.compare(k, getKey(y));
      if (cv < 0) {
        setLeft(y, z);
      } else if (cv > 0) {
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
    // color[root] = black;
    return z;
  }

  public int findKey(final int k) {
    int node = this.root;
    int cv;
    while (node != NIL) {
      cv = this.comp.compare(k, getKey(node));
      if (cv < 0) {
        node = getLeft(node);
      } else if (cv > 0) {
        node = getRight(node);
      } else {
        return node;
      }
    }
    // node == NIL
    return NIL;
  }

  public int findInsertionPoint(final int k) {
    int node = this.root;
    int found = this.root;
    int cv = 0;
    while (node != NIL) {
      found = node;
      cv = this.comp.compare(k, getKey(node));
      if (cv < 0) {
        node = getLeft(node);
      } else if (cv > 0) {
        node = getRight(node);
      } else {
        return node;
      }
    }
    // node == NIL
    if (cv > 0) {
      return nextNode(found);
    }
    return found;
  }

}
