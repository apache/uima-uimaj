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

import org.apache.uima.internal.util.BinaryTree;

/**
 * Node used in RedBlackTree, holds int Object pairs and links
 * Red-Black Tree node. Not for public use. Use the interface in RedBlackTree instead. This should
 * probably be an internal class to RedBlackTree, but it's easier to read in a seperate file. See
 * comments in RedBlackTree.
 * 
 */
class RBTNode<T> {

  // The colors.
  private static final boolean RED = true;

  private static final boolean BLACK = false;

  private int key; // The key of the node.

  private boolean color; // The color of the node.

  // A pointer to the parent node. This will be null if this is the
  // root of a tree.
  private RBTNode<T> parent;

  // The following variables are package private so they can be
  // accessed from RedBlackTree.

  RBTNode<T> left; // The left daughter.

  RBTNode<T> right; // The right daughter.

  T element; // The element contained in the node.

  // For the debugging print functions.
  private static final int indentInc = 2;

  /**
   * The real constructor, used only internally.
   */
  private RBTNode(int key, boolean color, RBTNode<T> parent, RBTNode<T> left, RBTNode<T> right,
          T element) {
    this.key = key;
    this.color = color;
    this.parent = parent;
    this.left = left;
    this.right = right;
    this.element = element;
  }

  /**
   * The standard constructor as used by RedBlackTree.
   * 
   * @param key
   *          The key to be inserted.
   * @param element
   *          The value to be inserted.
   */
  RBTNode(int key, T element) {
    // The default color is completely arbitrary.
    this(key, BLACK, null, null, null, element);
  }

  /**
   * Find a node with a certain key. Returns null if no such node exists.
   */
  static final <T> RBTNode<T> find(RBTNode<T> x, int key) {
    while (x != null && x.key != key) {
      if (key < x.key) {
        x = x.left;
      } else {
        x = x.right;
      }
    }
    return x;
  }

  /** 
   * Find the successor node to this. 
   */
  final RBTNode<T> successor() {
    RBTNode<T> x = this;
    // If this has a right daughter, then the successor will be the
    // leftmost daughter of this.right, if it exists, and x.right,
    // else.
    if (x.right != null) {
      x = x.right;
      while (x.left != null) {
        x = x.left;
      }
      return x;
    }
    // If this does not have a right daughter, we need to move up in
    // the tree until we hit a node we're the left daughter of. That
    // will be the successor.
    RBTNode<T> y = x.parent;
    while (y != null && x == y.right) {
      x = y;
      y = x.parent;
    }
    return y;
  }

  /** 
   * Insert a node into a tree. See CLR. 
   */
  static final <T> boolean insert(RedBlackTree<T> tree, RBTNode<T> x) {
    if (!treeInsert(tree, x)) {
      // A node with the same key as x already existed in the tree, so
      // there is nothing left to do.
      return false;
    }
    RBTNode<T> y;
    x.color = RED;
    while (x != tree.root && x.parent.color == RED) {
      if (x.parent == x.parent.parent.left) {
        y = x.parent.parent.right;
        if (colorOf(y) == RED) {
          x.parent.color = BLACK;
          setColor(y, BLACK);
          x.parent.parent.color = RED;
          x = x.parent.parent;
        } else {
          if (x == x.parent.right) {
            x = x.parent;
            x.leftRotate(tree);
          }
          x.parent.color = BLACK;
          x.parent.parent.color = RED;
          x.parent.parent.rightRotate(tree);
        }
      } else {
        y = x.parent.parent.left;
        if (colorOf(y) == RED) {
          x.parent.color = BLACK;
          setColor(y, BLACK);
          x.parent.parent.color = RED;
          x = x.parent.parent;
        } else {
          if (x == x.parent.left) {
            x = x.parent;
            x.rightRotate(tree);
          }
          x.parent.color = BLACK;
          x.parent.parent.color = RED;
          x.parent.parent.leftRotate(tree);
        }
      }
    }
    tree.root.color = BLACK;
    return true;
  }

  /** Auxiliary function for insert(). See CLR. */
  private static final <T> boolean treeInsert(RedBlackTree<T> tree, RBTNode<T> z) {
    RBTNode<T> y = null;
    RBTNode<T> x = tree.root;
    while (x != null) {
      y = x;
      if (z.key < x.key) {
        x = x.left;
      } else if (z.key > x.key) {
        x = x.right;
      } else { // z.key == x.key
        x.element = z.element;
        // No node was actually inserted.
        return false;
      }
    }
    z.parent = y;
    if (y == null) {
      tree.root = z;
    } else if (z.key < y.key) {
      y.left = z;
    } else {
      y.right = z;
    }
    return true;
  }

  /** Left rotation, used to keep the tree balanced. See CLR. */
  private final void leftRotate(RedBlackTree<T> tree) {
    RBTNode<T> y = this.right;
    this.right = y.left;
    if (y.left != null) {
      y.left.parent = this;
    }
    y.parent = this.parent;
    if (this.parent == null) {
      tree.root = y;
    } else if (this == this.parent.left) {
      this.parent.left = y;
    } else {
      this.parent.right = y;
    }
    y.left = this;
    this.parent = y;
    return;
  }

  /** Right rotation, used to keep the tree balanced. See CLR. */
  private final void rightRotate(RedBlackTree<T> tree) {
    RBTNode<T> y = this.left;
    this.left = y.right;
    if (y.right != null) {
      y.right.parent = this;
    }
    y.parent = this.parent;
    if (this.parent == null) {
      tree.root = y;
    } else if (this == this.parent.right) {
      this.parent.right = y;
    } else {
      this.parent.left = y;
    }
    y.right = this;
    this.parent = y;
    return;
  }

  /**
   * Delete a given node from the tree. The node must be contained in the tree! Our code is more
   * complicated than CLR because we don't use a NIL sentinel.
   */
  static final <T> void delete(RedBlackTree<T> tree, RBTNode<T> z) {
    RBTNode<T> x;
    RBTNode<T> y;
    RBTNode<T> xParent = null;

    if (z.left == null || z.right == null) {
      y = z;
    } else {
      y = z.successor();
    }
    if (y.left != null) {
      x = y.left;
    } else {
      x = y.right;
    }
    if (x != null) {
      x.parent = y.parent;
    } else {
      xParent = y.parent;
    }
    if (y.parent == null) {
      tree.root = x;
    } else {
      if (y == y.parent.left) {
        y.parent.left = x;
      } else {
        y.parent.right = x;
      }
    }
    if (y != z) {
      z.key = y.key;
      z.element = y.element;
    }
    if (y.color == BLACK) {
      if (x == null) {
        deleteFixupNull(tree, xParent);
      } else {
        deleteFixup(tree, x);
      }
    }
  }

  /** 
   * From CLR. x must not be null. 
   */
  private static final <T> void deleteFixup(RedBlackTree<T> tree, RBTNode<T> x) {
    RBTNode<T> w;
    while (x != tree.root && x.color == BLACK) {
      if (x == x.parent.left) {
        w = x.parent.right;
        if (w.color == RED) {
          w.color = BLACK;
          x.parent.color = RED;
          x.parent.leftRotate(tree);
          w = x.parent.right;
        }
        if (colorOf(leftOf(w)) == BLACK && colorOf(rightOf(w)) == BLACK) {
          w.color = RED;
          x = x.parent;
        } else {
          if (colorOf(rightOf(w)) == BLACK) {
            w.color = RED;
            w.rightRotate(tree);
            w = x.parent.right;
          }
          w.color = x.parent.color;
          x.parent.color = BLACK;
          if (w.right != null) {
            w.right.color = BLACK;
          }
          x.parent.leftRotate(tree);
          x = tree.root;
        }
      } else {
        w = x.parent.left;
        if (w.color == RED) {
          w.color = BLACK;
          x.parent.color = RED;
          x.parent.rightRotate(tree);
          w = x.parent.left;
        }
        if (colorOf(rightOf(w)) == BLACK && colorOf(leftOf(w)) == BLACK) {
          w.color = RED;
          x = x.parent;
        } else {
          if (colorOf(leftOf(w)) == BLACK) {
            w.color = RED;
            w.leftRotate(tree);
            w = x.parent.left;
          }
          w.color = x.parent.color;
          x.parent.color = BLACK;
          if (w.left != null) {
            w.left.color = BLACK;
          }
          x.parent.rightRotate(tree);
          x = tree.root;
        }
      }
    }
    x.color = BLACK;
  }

  /**
   * Like deleteFixup(), only that the node we should be working on is null, and we actually hand in
   * the node's mother. Special case because we don't use sentinels.
   */
  private static final <T> void deleteFixupNull(RedBlackTree<T> tree, RBTNode<T> x) {
    // if x == null, we just deleted the only node in the tree
    if (x == null) {
      return;
    }

    RBTNode<T> w;

    if (x.left == null) {
      w = x.right;
      if (w.color == RED) {
        w.color = BLACK;
        x.color = RED;
        x.leftRotate(tree);
        w = x.right;
      }
      if (colorOf(leftOf(w)) == BLACK && colorOf(rightOf(w)) == BLACK) {
        w.color = RED;
      } else {
        if (colorOf(rightOf(w)) == BLACK) {
          w.color = RED;
          w.rightRotate(tree);
          w = x.right;
        }
        w.color = x.color;
        x.color = BLACK;
        if (w.right != null) {
          w.right.color = BLACK;
        }
        x.leftRotate(tree);
        x = tree.root;
      }
    } else {
      w = x.left;
      if (w.color == RED) {
        w.color = BLACK;
        x.color = RED;
        x.rightRotate(tree);
        w = x.left;
      }
      if (colorOf(rightOf(w)) == BLACK && colorOf(leftOf(w)) == BLACK) {
        w.color = RED;
      } else {
        if (colorOf(leftOf(w)) == BLACK) {
          w.color = RED;
          w.leftRotate(tree);
          w = x.left;
        }
        w.color = x.color;
        x.color = BLACK;
        if (w.left != null) {
          w.left.color = BLACK;
        }
        x.rightRotate(tree);
        x = tree.root;
      }
    }
    while (x != tree.root && x.color == BLACK) {
      if (x == x.parent.left) {
        w = x.parent.right;
        if (w.color == RED) {
          w.color = BLACK;
          x.parent.color = RED;
          x.parent.leftRotate(tree);
          w = x.parent.right;
        }
        if (colorOf(leftOf(w)) == BLACK && colorOf(rightOf(w)) == BLACK) {
          w.color = RED;
          x = x.parent;
        } else {
          if (colorOf(rightOf(w)) == BLACK) {
            w.color = RED;
            w.rightRotate(tree);
            w = x.parent.right;
          }
          w.color = x.parent.color;
          x.parent.color = BLACK;
          if (w.right != null) {
            w.right.color = BLACK;
          }
          x.parent.leftRotate(tree);
          x = tree.root;
        }
      } else {
        w = x.parent.left;
        if (w.color == RED) {
          w.color = BLACK;
          x.parent.color = RED;
          x.parent.rightRotate(tree);
          w = x.parent.left;
        }
        if (colorOf(rightOf(w)) == BLACK && colorOf(leftOf(w)) == BLACK) {
          w.color = RED;
          x = x.parent;
        } else {
          if (colorOf(leftOf(w)) == BLACK) {
            w.color = RED;
            w.leftRotate(tree);
            w = x.parent.left;
          }
          w.color = x.parent.color;
          x.parent.color = BLACK;
          if (w.left != null) {
            w.left.color = BLACK;
          }
          x.parent.rightRotate(tree);
          x = tree.root;
        }
      }
    }
    x.color = BLACK;
  }

  /**
   * Fill an array with the keys contained in the tree. The array must at least have the size of the
   * tree! Returns the size of the tree, for internal reasons.
   */
  int keys(int pos, int[] keys) {
    int cur = pos;
    if (this.left != null) {
      cur = this.left.keys(cur, keys);
    }
    keys[cur] = this.key;
    ++cur;
    if (this.right != null) {
      cur = this.right.keys(cur, keys);
    }
    return cur;
  }

  // Use as accessor when node might be null.
  private static final <T> boolean colorOf(RBTNode<T> x) {
    return (x == null) ? BLACK : x.color;
  }

  // Below some accessor functions to be used when the node might be
  // null.

  // Use when node might be null.
  private static final <T> void setColor(RBTNode<T> x, boolean c) {
    if (x != null) {
      x.color = c;
    }
  }

  // Use when node might be null.
  private static final <T> RBTNode<T> leftOf(RBTNode<T> x) {
    return (x == null) ? null : x.left;
  }

  // Use when node might be null.
  private static final <T> RBTNode<T> rightOf(RBTNode<T> x) {
    return (x == null) ? null : x.right;
  }

  // Debugging aid.
  public void printKeys(int indent) {
    for (int i = 0; i < indent; i++) {
      System.out.print(' ');
    }
    System.out.print(this.key);
    System.out.print(':');
    if (this.color == RED) {
      System.out.println("red");
    } else {
      System.out.println("black");
    }
    indent += indentInc;
    if (this.left != null) {
      this.left.printKeys(indent);
    }
    if (this.right != null) {
      this.right.printKeys(indent);
    }
    return;
  }

  // Debugging aid.
  public void printElements(int indent) {
    for (int i = 0; i < indent; i++) {
      System.out.print(' ');
    }
    System.out.println(this.element.toString());
    indent += indentInc;
    if (this.left != null) {
      this.left.printElements(indent);
    }
    if (this.right != null) {
      this.right.printElements(indent);
    }
    return;
  }

  void getBinaryTree(BinaryTree tree) {
    tree.setValue(new RBTKeyValuePair(this.key, this.element));
    if (this.left != null) {
      BinaryTree newLeft = tree.newLeftDtr();
      this.left.getBinaryTree(newLeft);
    }
    if (this.right != null) {
      BinaryTree newRight = tree.newRightDtr();
      this.right.getBinaryTree(newRight);
    }
  }

  // public class PreorderIterator implements Iterator {

  // private RBTNode current;

  // private PreorderIterator() {
  // this.current = null;
  // }

  // PreorderIterator(RBTNode node) {
  // this.current = node;
  // }

  // public boolean hasNext() {
  // return (this.current == null);
  // }

  // public Object next() {
  // if (this.current == null) {
  // throw new java.util.NoSuchElementException();
  // }
  // RBTNode node = this.current;
  // // Return this at the end.
  // final RBTKeyValuePair rv = new RBTKeyValuePair(node.key, node.element);
  // // Find the next value.
  // if (node.left != null) {
  // // Go down left branch if it exists.
  // node = node.left;
  // } else if (node.right != null) {
  // // Else, go down right branch, if it exists.
  // node = node.right;
  // } else {
  // // Else, back up the tree.
  // while (node.parent != null) {
  // if (node == node.parent.right || node.parent.right == null) {
  // // This is a right dtr or an only child, continue backing
  // // up the tree.
  // node = node.parent;
  // } else {
  // // This is a left dtr and a right dtr exists, so this is
  // // our next candidate.
  // node = node.parent.right;
  // break;
  // }
  // }
  // if (node.parent == null) {
  // this.current = null;
  // } else {
  // this.current = node;
  // }
  // }
  // return rv;
  // }

  // public void remove() {
  // throw new java.lang.UnsupportedOperationException();
  // }
  // }

  // java.util.Iterator preorderIterator() {
  // return new PreorderIterator(this);
  // }

}
