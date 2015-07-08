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

  static final boolean debug = false;
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

  private int getKey(int node) {
    return getXXX(node, 0);
  }

  private int setKey(int node, int value) {
    return setXXX(node, 0, value);
  }
  
  protected int getLeft(int node) {
    return getXXX(node, 1);
  }
  
  protected int setLeft(int node, int value) {
    return setXXX(node, 1, value);
  }
  
  protected int getRight(int node) {
    return getXXX(node, 2);
  }
  
  protected int setRight(int node, int value) {
    return setXXX(node, 2, value);
  }
  
  protected int getParent(int node) {
    return getXXX(node, 3);
  }
  
  /**
   * @param node -
   * @param value -
   * @return the value
   */
  protected int setParent(int node, int value) {
    return setXXX(node, 3, value);
  }
  
  protected int getKeyForNode(int node) {
    return getKey(node);
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
  // keys that are larger than all keys already in the tree
  // internal use, public because used by iterators in different package
  public int greatestNode;

  protected static final int default_size = 8;  // must be pwr of 2 for useklrp true
  
  final protected int initialSize;

  final protected int growth_factor = 2;  // must be pwr of 2 for useklrp true

  final protected int multiplication_limit = 1024 * 1024 * 16;  // must be pwr of 2 for useklrp true
  
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
    klrp = new int[initialSize << 2];
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
    if (klrp.length > (initialSize << 2)) {
      setupArrays();
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
    final int requiredCapacityForLastSegment = nextPowerOf2(requiredSize - w * MAXklrp0);
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
        if (debug) {
          System.out.format("initializing klrp1 to %d%n", requiredCapacityForLastSegment << 2);
        }
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
    if (debug) {
      System.out.format("expanding array from to %,d to %,d%n", array.length, newSize);
    }
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
   * @return the first node such that k = key[node].
   */
  public int findKey(final int k) {
    return findKeyDown(k, this.root);
  }
  
  protected int findKeyDown(final int k, int node) {
    while (node != NIL) {
      final int cr = compare(k, getKey(node));
      if (0 == cr) {
        return node;
      }      
      node = (cr < 0) ? getLeft(node) : getRight(node);
    }
    return NIL;
  }

  /**
   * Find the node such that key[node] &ge; k and key[previous(node)] &lt; k.
   *   If k is less than all the nodes, then the first node is returned
   *   If k is greater than all the nodes, then NIL is returned (invalid signal)
   * @param k the key
   * @return the index of the node, or NIL if k &gt; all keys
   */
  public int findInsertionPoint(final int k) {
    return findInsertionPointCmn(k, true);
  }
     
     /**
      * Find the node such that key[node] &ge; k and key[previous(node)] &lt; k.
      * @param k -
      * @return the node such that key[node] &ge; k and key[previous(node)] &lt; k.
      */
  public int findInsertionPointNoDups(final int k) {
    return findInsertionPointCmn(k, false);
  }
  
  public int findInsertionPointCmn(final int k, boolean moveToLeftmost) {
    int node = this.root;
    int found = node;
    int cr = 0;
    
    while (node != NIL) {
      found = node;
      cr = compare(k, getKey(node));
      
      if (0 == cr) { 
        // found a match, but in the presence of duplicates, need to 
        // move to the left most one
        if (moveToLeftmost) {
          while (true) {
            int leftmost = previousNode(node);
            if (NIL == leftmost || (0 != compare(k, getKey(leftmost)))) {
              return node;
            }
            node = leftmost;
          }
        } else {
          return found;
        }
      }
      
      node = (cr < 0) ? getLeft(node) : getRight(node);
    }
    
    // compare not equal, and ran out of elements (not found)
    // if k > all nodes, return NIL
    return (cr > 0) ? NIL : found; 
  }

  public final boolean containsKey(int k) {
    return (findKey(k) != NIL);
  }
  
  public final boolean contains(int k) {
    return (findKey(k) != NIL);
  }

  // internal use, public to access by internal routine in another package
  public final int getFirstNode() {
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
    return node;
  }

  // internal use, public only to get cross package internal reference
  /**
   * Method: if there's a right descendant, return the left-most chain down on that link
   * Else, go up until parent has a right descendant not the previous, and return that.
   * 
   * @param node starting point
   * @return the node logically following this one.
   */
  public final int nextNode(int node) {
    if (node == NIL) {
      return NIL;
    }
    final int rightNode = getRight(node);
    if (rightNode != NIL) {
      node = rightNode;
      while (true) {
        final int leftNode = getLeft(node);
        if (leftNode == NIL) {
          return node;
        }
        node = leftNode;
      }
    }
    
    while (true) {
      if (node == this.root) { // if initial node is the root, can't go up.
        return NIL;
      }
      final int parentNode = getParent(node);  // guaranteed parentNode not NIL because it's tested above and below
      final int nextNode = getRight(parentNode);  // Can be NIL
      if (node != nextNode) {
        return parentNode;
      }
      if (parentNode == this.root) {
        return NIL;
      }
      node = parentNode;  
    }
  }

  // internal use, public only to get cross package internal reference
  /**
   * Method: if there's a left descendant, go to the right-most bottom of that
   * Otherwise, ascend up until the parent's left descendant isn't the previous link
   * @param node the current node index
   * @return the previous node index or NIL 
   */
  public final int previousNode(int node) {
    if (node == NIL) {
      return NIL;
    }
    final int leftNode = getLeft(node);
    if (leftNode != NIL) {
      node = leftNode;
      while (true) {
        final int rightNode = getRight(node);
        if (rightNode == NIL) {
          return node;
        }
        node = rightNode;
      }
    }
    
    // ascend until a parent node has left non-nil child not equal to the previous node
    //   this means the parent node's right child is this previous node. 

    while (true) {
      if (node == this.root) { // if initial node is the root, can't go up.
        return NIL;
      }
      final int parentNode = getParent(node);  // guaranteed parentNode not NIL because it's tested above and below
      final int nextNode = getLeft(parentNode);  // can be NIL
      if (node != nextNode) {
        return parentNode;  
      }
      node = parentNode;
    }
  } 
   

  /**
   * delete node z
   *   Step 1: locate a node to delete at the bottom of the tree.  Bottom means left or right (or both) descendant is NIL.
   *   
   *   There are 2 cases:  either the node to delete is z, or the node is the nextNode.
   *     If z has one or both descendants NIL, then it's the one to delete.
   *     Otherwise, the next node which is found by descending right then left until reaching the bottom (left = 0) node.
   *     
   *   y is node to remove from the tree.
   *   
   *   x is the non-NIL descendant of y (if one exists).  It will be reparented to y's parent, and y's parent's left or right
   *   will point to it, skipping over y.
   *       
   * @param z node to be removed, logically
   */
  protected void deleteNode(final int z) {
    // y is the node to remove from the tree; is the input node, or the next node.
    
    final int y = ((getLeft(z) == NIL) || (getRight(z) == NIL)) ?  z : nextNode(z);
    
    if (y == this.greatestNode) {
      if (y == z) {
        this.greatestNode = previousNode(z);
      } else {
        this.greatestNode = z;
      }
    }

    final int left_y = getLeft(y);  // will be NIL if y is nextNode(z)

    // x is the descendant of y (or NIL) that needs reparenting to the parent of y
    // and also serves as the initializing value for rebalancing
    final int x = (left_y != NIL) ? left_y : getRight(y);

    final int parent_y = getParent(y);
    
    // if x is NIL, we still may pass it to the red-black rebalancer; so set it's "parent" value in any case.  
    setParent(x, parent_y); // "splice out" y by pointing parent link of x to parent of y 
    
    if (parent_y == NIL) {
      setAsRoot(x);
    } else {
      if (y == getLeft(parent_y)) {
        setLeft(parent_y, x);  // splice out y 
      } else {
        setRight(parent_y, x); // splice out y
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

  protected int compare(int v1, int v2) {
    return (v1 < v2) ? -1 : ((v1 > v2) ? 1 : 0);
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
