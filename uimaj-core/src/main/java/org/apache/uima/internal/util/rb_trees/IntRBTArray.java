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

// The following is only imported to trick javadoc.
// Not used (except for some static constants  8/2014

/**
 * Helper class to read array-based binary search trees with integers as keys and values. No write
 * access to the tree is provided. See
 * {@link org.apache.uima.internal.util.rb_trees.IntRedBlackTree IntRedBlackTree} on how to generate
 * such an array representation. The name is a bit of a misnomer, since nothing in this class is
 * specific to red-black trees.
 * 
 * <p>
 * Suppose <code>i</code> is the position of the first cell encoding a tree node in array
 * <code>a</code>. Then the expected memory layout of <code>a</code> is:
 * <ul>
 * <li><code>a[i]</code> is the key of the node</li>
 * <li><code>a[i+1]</code> is the element of the node</li>
 * <li><code>a[i+2]</code> is one of:
 * <ul>
 * <li><code>IntRBTArray.TERMINAL</code>: this is a terminal node</li>
 * <li><code>IntRBTArray.LEFTDTR</code>: this node only has a left daughter, so
 * <code>a[i+3]</code> is the first cell of the left daughter node</li>
 * <li><code>IntRBTArray.RIGHTDTR</code>: this node only has a right daughter, so
 * <code>a[i+3]</code> is the first cell of the right daughter node</li>
 * <li><code>IntRBTArray.TWODTRS</code>: this node has two daughters. <code>a[i+3]</code>
 * contains the address of the right daughter, and <code>a[i+4]</code> is the start of the left
 * daughter node</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * <p>
 * Note that the array from which an IntRBTArray object is constructed can contain other data as
 * well. However, we assume that the addressing (the right daughter addresses, to be precise), must
 * be absolute (i.e., not relative to some starting point within the array).
 */
public class IntRBTArray {

  /** Code for a terminal node in the array. */
  public static final int TERMINAL = 0;

  /** Code for a node with only a left daughter. */
  public static final int LEFTDTR = 1;

  /** Code for a node with only a right daughter. */
  public static final int RIGHTDTR = 2;

  /** Code for a node with two daughters. */
  public static final int TWODTRS = 3;

  // The array that holds the search tree.
  private int[] array;

  // The address of the root node.
  private int offset;

  /**
   * Constructor that takes a start point as parameter.
   * 
   * @param start
   *          Address of the root node of the tree.
   * @param array
   *          The array containing the search tree.
   */
  public IntRBTArray(int[] array, int start) {
    this.offset = start;
    this.array = array;
  }

  /**
   * This constructor assumes that the root node is located at <code>0</code>.
   * 
   * @param array
   *          The array containing the search tree.
   */
  public IntRBTArray(int[] array) {
    this(array, 0);
  }

  /**
   * Getter for the internal array.
   * 
   * @return The internal array.
   */
  public int[] toArray() {
    return this.array;
  }

  /**
   * Set the address of the root node of the tree.
   * 
   * @param start
   *          the address.
   */
  public void setRootAddress(int start) {
    this.offset = start;
  }

  /**
   * Retrieve the value for a certain key.
   * 
   * @param i
   *          The input key.
   * @return The value, if key was found.
   * @throws NoSuchElementException
   *           If the key is not defined in the tree.
   */
  public int get(int i) throws NoSuchElementException {
    int pos = getPosition(i);
    if (pos >= 0) {
      return this.array[pos];
    }
    throw new NoSuchElementException();
  }

  /**
   * Get the position of a value for a key.
   * 
   * @param i
   *          The key.
   * @return The address of the value for <code>i</code>, if it's found; <code>-1</code>,
   *         else. This routine may also return <code>-1</code> when the tree is corrupted. Of
   *         course, with a corrupted tree, results will in general be unpredictable. However, this
   *         routine will not throw an
   *         {@link java.lang.ArrayIndexOutOfBoundsException ArrayIndexOutOfBoundsException}.
   */
  public int getPosition(int i) throws NoSuchElementException {
    // See the comments about the memory layout of the array at the
    // top of the file.
    int current = this.offset;
    if (this.array == null || this.array.length < (current + 3)) {
      return -1;
    }
    int key;
    int dtrCode;
    while (current >= 0 && this.array.length >= (current + 3)) {
      key = this.array[current];
      dtrCode = this.array[current + 2];
      if (key > i) {
        switch (dtrCode) {
          case TERMINAL:
            return -1;
          case LEFTDTR:
            current += 3;
            break;
          case RIGHTDTR:
            return -1;
          case TWODTRS:
            current += 4;
            break;
        }
      } else if (key < i) {
        switch (dtrCode) {
          case TERMINAL:
            return -1;
          case LEFTDTR:
            return -1;
          case RIGHTDTR:
            current += 3;
            break;
          case TWODTRS:
            if ((current + 3) > this.array.length) {
              return -1;
            }
            current = this.array[current + 3];
            break;
        }
      } else { // key == i
        return current + 1;
      }
    }
    return -1;
  }

  // /** Get the position of a value for a key. THIS VERSION DOES NOT WORK!
  // @param i The key.
  // @return The address of the value for <code>i</code>, if it's
  // found; <code>-1</code>, else. This routine may also return
  // <code>-1</code> when the tree is corrupted. Of course, with a
  // corrupted tree, results will in general be unpredictable.
  // In that case, this routine may throw an
  // {@link ArrayIndexOutOfBoundsException ArrayIndexOutOfBoundsException}.
  // */
  // public int getPosition(int i) {
  // // See the comments about the memory layout of the array at the
  // // top of the file.
  // int current = this.offset;
  // if (array == null || array.length < (current+2)) {
  // return -1;
  // }
  // int key = array[current];
  // current += 2;
  // int dtrCode = array[current];
  // while (current > 1 && array.length >= current) {
  // if (key > i) {
  // switch (dtrCode) {
  // case TERMINAL:
  // return -1;
  // case LEFTDTR:
  // ++current;
  // break;
  // case RIGHTDTR:
  // return -1;
  // case TWODTRS:
  // current += 2;
  // break;
  // }
  // } else if (key < i) {
  // switch (dtrCode) {
  // case TERMINAL:
  // return -1;
  // case LEFTDTR:
  // return -1;
  // case RIGHTDTR:
  // ++current;
  // break;
  // case TWODTRS:
  // ++current;
  // current = array[current];
  // break;
  // }
  // } else { // key == i
  // return current-1;
  // }
  // key = array[current];
  // current += 2;
  // dtrCode = array[current];
  // }
  // return -1;
  // }

}
