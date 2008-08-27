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
 * This class implements a set of integers. It does not implement the <code>Set</code> interface
 * for performance reasons, though methods with the same name are equivalent.
 * 
 */
public class IntSet {

  /** The data. */
  private IntVector data;

  /** Creates a new instance of this set. */
  public IntSet() {
    this.data = new IntVector();
  }

  /**
   * Adds the specified int to this set.
   * 
   * @param element
   *          the integer to be added.
   * @return <code>true</code> if this set did not already contain this element,
   *         <code>false</code> otherwise.
   */
  public boolean add(int element) {
    if (!this.data.contains(element)) {
      this.data.add(element);
      return true;
    }
    return false;
  }

  /**
   * Tests if this set contains the specified element.
   * 
   * @param element
   *          the element to be tested.
   * @return <code>true</code> if the element is contained in this set, <code>false</code>
   *         otherwise.
   */
  public boolean contains(int element) {
    return this.data.contains(element);
  }

  /** @return the size of this set. */
  public int size() {
    return this.data.size();
  }

  /** @return the <code>n</code>-th element in this set. */
  public int get(int n) {
    return this.data.get(n);
  }

  /** Removes the <code>n</code>-th element in this set. */
  public void remove(int n) {
    this.data.remove(n);
  }

  /**
   * Tests if two sets are equal. This is the case if the two sets are of the same size, and every
   * element in one set in contained in the other set.<br>
   * Note that in order to increase performance, before the sets are actually compared the way
   * described above, the sums of the elements in both sets are calculated, ignoring possible int
   * overflows. If the sums are not equal, the two sets cannot be equal. In case the sums are equal,
   * the two sets are compared element by element.
   * 
   * @param s
   *          the set to be tested for equality with this set.
   * @return <code>true</code> if the sets are equal, <code>false</code> otherwise.
   */
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof IntSet)) {
      return false;
    }
    IntSet s = (IntSet) o;
    int size = size();
    if (size == s.size()) {
      int sum1 = 0;
      int sum2 = 0;
      for (int i = 0; i < size; i++) {
        sum1 += this.data.get(i);
        sum2 += s.get(i);
      }
      if (sum1 != sum2)
        return false;

      for (int i = 0; i < size; i++) {
        if (!s.contains(this.data.get(i)))
          return false;
      }
      return true;
    }
    return false;
  }

  public int hashCode() {
    if (this.data == null) {
      return 0;
    }
    int sum = 0;
    for (int i = 0; i < this.size(); i++) {
      sum += this.data.get(i);
    }
    return sum;
  }
  
  public int indexOf(int element) {
	return  this.data.indexOf(element);
  }
}
