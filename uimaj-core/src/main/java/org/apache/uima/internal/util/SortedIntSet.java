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

package org.apache.uima.internal.util;

/**
 * A set of integers, maintained as a sorted array. Note that the actual array used to implement
 * this class grows in larger increments, so it is efficient to use this class even when doing lots
 * of insertions.
 * 
 * 
 */
public class SortedIntSet {

  // Use an IntVector for automatic array management.
  private IntVector vector;

  /** Default constructor. */
  public SortedIntSet() {
    super();
    this.vector = new IntVector();
  }

  public SortedIntSet(int[] array) {
    this();
    for (int i = 0; i < array.length; i++) {
      this.add(array[i]);
    }
  }

  /**
   * Find position of <code>ele</code> in set.
   * 
   * @param ele
   *          The element we're looking for.
   * @return The position, if found; a negative value, else. See
   *         {@link org.apache.uima.internal.util.IntArrayUtils#binarySearch IntArrayUtils.binarySearch()}.
   */
  public int find(int ele) {
    int[] array = this.vector.getArray();
    return IntArrayUtils.binarySearch(array, ele, 0, this.vector.size());
  }

  /**
   * @param ele - 
   * @return <code>true</code> iff <code>ele</code> is contained in
   *  the set.
   */
  public boolean contains(int ele) {
    return this.find(ele) >= 0;
  }

  /**
   * Add element to set.
   * 
   * @param ele - 
   * @return <code>true</code> iff <code>ele</code> was not already contained in the set.
   */
  public boolean add(int ele) {
    final int pos = this.find(ele);
    if (pos >= 0) {
      return false;
    }
    this.vector.add(-(pos + 1), ele);
    return true;
  }

  /**
   * Remove element from set.
   * @param ele - 
   * @return <code>true</code> iff <code>ele</code> was actually contained in the set.
   */
  public boolean remove(int ele) {
    final int pos = this.find(ele);
    if (pos < 0) {
      return false;
    }
    this.vector.remove(pos);
    return true;
  }

  /**
   * Number of elements in set.
   * 
   * @return Current number of elements in set.
   */
  public int size() {
    return this.vector.size();
  }

  /**
   * Get element at position.
   * 
   * @param pos
   *          Get element at this position.
   * @return The element at this position.
   */
  public int get(int pos) {
    return this.vector.get(pos);
  }

  public void union(SortedIntSet set) {
    final int max = set.size();
    for (int i = 0; i < max; i++) {
      this.add(set.get(i));
    }
  }

  public void removeAll() {
    this.vector.removeAllElements();
  }

  public int[] toArray() {
    return this.vector.toArrayCopy();
  }
  
  public int[] getArray() {
    return vector.getArray();
  }

  // public static void main(String [] args) {

  // SortedIntSet set = new SortedIntSet();
  // assert set.size() == 0;
  // assert !set.contains(0);

  // set.add(3);
  // set.add(5);
  // set.add(1);
  // assert set.find(1) == 0;
  // assert set.find(3) == 1;
  // assert set.find(5) == 2;
  // assert set.get(0) == 1;
  // assert set.get(1) == 3;
  // assert set.get(2) == 5;

  // set.add(2);
  // set.add(0);
  // set.add(4);

  // assert set.find(0) == 0;
  // assert set.find(1) == 1;
  // assert set.find(2) == 2;
  // assert set.find(3) == 3;
  // assert set.find(4) == 4;
  // assert set.find(5) == 5;
  // assert set.get(0) == 0;
  // assert set.get(1) == 1;
  // assert set.get(2) == 2;
  // assert set.size() == 6;

  // assert set.remove(1);
  // assert set.remove(3);
  // assert set.remove(5);
  // assert set.get(0) == 0;
  // assert set.get(1) == 2;
  // assert set.get(2) == 4;

  // }

}
