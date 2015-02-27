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

import org.apache.uima.internal.util.IntArrayUtils;
import org.apache.uima.internal.util.StringUtils;

/**
 * Common part of Red-black tree implementation based on integer arrays. Preliminary performance measurements on
 * j2se 1.4 indicate that the performance improvement as opposed to an object-based implementation
 * are miniscule. This seems to indicate a much improved object creation handling in this vm.
 * However the space improvements are substantial.
 * 
 */
public class IntArrayRBTcommon {

  static final protected boolean useklrp = true;
  // Keys.
  protected int[] key;

  // Left daughters.
  protected int[] left;

  // Right daughters.
  protected int[] right;

  // Parents.
  protected int[] parent;
  
  // alternate layout
  protected int[] klrp;
  // the next 3 are for the rare cases where the number of entries
  // in this instance exceeds 512 * 1024 * 1024 - 1
  // which is the largest index that can be stored in klrp
  //   because it is shifted left by 2
  protected int[] klrp1;
  protected int[] klrp2;
  protected int[] klrp3;
  protected static final int MAXklrp0 = 512 * 1024 * 1024;
  protected static final int MAXklrpMask = MAXklrp0 - 1;

    
  protected int getXXX(int node, int offset) {
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

  protected int setXXX(int node, int offset, int value) {
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
  protected int next;

  // The current size of the tree. Since we can remove nodes, since needs to
  // be kept separate from the next free cell.
  protected int size;

  // The root of the tree.
  protected int root;

  // Keep a pointer to the largest node around so we can optimize for
  // inserting
  // keys that are larger than all keys already in the tree.
  protected int greatestNode;

  protected static final int default_size = 1024;  // must be pwr of 2 for useklrp true
  
  final protected int initialSize;

  final protected int growth_factor = 2;  // must be pwr of 2 for useklrp true

  final protected int multiplication_limit = 1024 * 1024 * 2;  // must be pwr of 2 for useklrp true
  
  // The NIL sentinel
  public static final int NIL = 0;

  // The colors.
  protected static final boolean red = true;

  protected static final boolean black = false;

  /**
   * Constructor for IntArrayRBT.
   */
  public IntArrayRBTcommon() {
    this(default_size);
  }

  public IntArrayRBTcommon(int initialSize) {
    if (initialSize < 4) {
      initialSize = 4;
    }
    initVars();
    this.initialSize = nextPowerOf2(initialSize);
    setupArrays();
  }
  
  protected int nextPowerOf2(int v) {
    // v is >= 0
    int v2 = Integer.highestOneBit(v);
    return (v2 < v) ? (v2 << 1) : v2;
  }
  
  protected void setupArrays() {
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

  protected void initVars() {
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

  /**
   * There are two strategies for storing data, controlled by useklrp.
   * If useklrp, then 4 elements are put together into one int vector,
   *   taking 4 words per element.
   *   Other elements are kept in their own vectors.
   * 
   * The growth strategy for the 4-element one is to 
   *   a) start at some minimum (a power of 2)
   *   b) grow by doubling up to 2 * 1024 *1024
   *   c) grow by adding 2 *1024 * 1024, until
   *   d) reaching the maximum size (the max index will be 1 less)
   *   e) when that size is reached, the next int[] is set up with the
   *      minimum, and it grows as above.
   * 
   * The test for growth and growing is made individually for the different parts.
   * For color (a boolean), the size for stopping doubling is 32 * 2 * 1024 * 1024,
   *   so the # of words is the same.
   * 
   * @param requiredSize -
   */
  
  protected void ensureCapacityKlrp(int requiredSize) {       
    // the klrp design stacks 4 pointers together into the int array
    // When growing, the last array is grown by the needed amount.
    //   Edge case: if the new required size jumps up by more than 
    //     about 2 million, the previous array might need to be 
    //     expanded too.
    //        This could in a real edge case require all previous arrays
    //        to be expanded.
    
    final int w = requiredSize >> 29;  // w is 0-3
    final int requiredCapacityForLastSegment = Math.max(1024, requiredSize - w * MAXklrp0);
    switch (w) {
    case 3: {
      if (klrp3 == null) {
        klrp3 = new int[requiredCapacityForLastSegment << 2];
      } else {
        klrp3 = ensureArrayCapacity(klrp3, requiredCapacityForLastSegment << 2);
      }
      maximize(klrp2);
      maximize(klrp1);
      maximize(klrp);
      }
      break;
    case 2: {
      if (klrp2 == null) {
        klrp2 = new int[requiredCapacityForLastSegment << 2];
      } else {
        klrp2 = ensureArrayCapacity(klrp2, requiredCapacityForLastSegment << 2);
      }
      maximize(klrp1);
      maximize(klrp);
      }
      break;
    case 1: {
      if (klrp1 == null) {
        klrp1 = new int[requiredCapacityForLastSegment << 2];
      } else {
        klrp1 = ensureArrayCapacity(klrp1, requiredCapacityForLastSegment << 2);
      }
      maximize(klrp);
      }
      break;
    case 0:
//        if (klrp == null) { // never true
//          klrp = new int[requiredSizeForLastSegment << 2];
//        } else {
        klrp = ensureArrayCapacity(klrp, requiredCapacityForLastSegment << 2);
//        }
      break;
    default:
      throw new RuntimeException();
    }
  }

  private void ensureCapacityNotKrlp(int requiredSize) {
    this.key = ensureArrayCapacity(this.key, requiredSize);
    this.left = ensureArrayCapacity(this.left, requiredSize);
    this.right = ensureArrayCapacity(this.right, requiredSize);
    this.parent = ensureArrayCapacity(this.parent, requiredSize);
  }
  
  protected void ensureCapacity(int requiredSize) {
    this.color = ensureBooleanArraySize(this.color, requiredSize);
  }

  // only called for krlp style
  private int[] maximize(int[] array) {
    if (array.length < MAXklrp0) {
      int[] a = new int[MAXklrp0];
      System.arraycopy(array, 0, a, 0, array.length);
      return a;
    }
    return array;
  }
  
  protected int newNode(final int k) {
    // Make sure the tree is big enough to accommodate a new node.

    if (useklrp) {
      int lenKlrp = (klrp.length >> 2);
      if (klrp1 != null) {
        lenKlrp += (klrp1.length >> 2);
        if (klrp2 != null) {
          lenKlrp += (klrp2.length >> 2);
          if (klrp3 != null) {
            lenKlrp += (klrp3.length >> 2);
          }
        }
      }
      if (this.next >= lenKlrp) {
        ensureCapacityKlrp(this.next + 1);
      }
    } else {
      // not using klrp format
      ensureCapacityNotKrlp(this.next + 1);
    }
    
    if (this.next >= this.color.length){
      this.color = ensureBooleanArraySize(this.color, this.next + 1);        
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

  protected final void setAsRoot(int x) {
    this.root = x;
    setParent(this.root, NIL);
  }

  /**
   * 
   * @param array - the array to expand - may be klrp0, 1, 2, 3, etc.
   * @param newSize = the total size - if in parts, the size of the part
   * @return expanded array
   */
  protected final int[] ensureArrayCapacity(final int[] array, final int newSize) {
    return IntArrayUtils.ensure_size(array, newSize, this.growth_factor, this.multiplication_limit);    
  }

  // times 32 to have the same tuning for expanding that int arrays do, for booleans
  private final boolean[] ensureBooleanArraySize(boolean[] array, int newSize) {
    return IntArrayUtils.ensure_size(array, newSize, this.growth_factor, this.multiplication_limit * 32);
  }

  protected final void leftRotate(final int x) {
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

  protected final void rightRotate(final int x) {
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
   * @param k -
   * @return the first node such that k &lt;= key[node].
   */
  protected int findKey(final int k) {
    return findKeyDown(k, this.root);
  }
  
  protected int findKeyDown(final int k, int node) {
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
   * @param k -
   * @return the node such that key[node] &ge; k and key[previous(node)] &lt; k.
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

  protected final int getFirstNode() {
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

  protected final int previousNode(int node) {
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

  protected void deleteNode(final int z) {
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

  protected void deleteFixup(int x) {
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

  protected boolean satisfiesRBProps(int node, final int blackDepth, int currentBlack) {
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

  protected int nodeDepth(int node, int depth, int k) {
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

  protected int maxDepth(int node, int depth) {
    if (node == NIL) {
      return depth;
    }
    int depth1 = maxDepth(getLeft(node), depth + 1);
    int depth2 = maxDepth(getRight(node), depth + 1);
    return (depth1 > depth2) ? depth1 : depth2;
  }

  protected int minDepth(int node, int depth) {
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

  protected final void printKeys(int node, int offset, StringBuffer buf) {
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
    buf.append('\n');
    printKeys(getLeft(node), offset + 2, buf);
    printKeys(getRight(node), offset + 2, buf);
  }

}
