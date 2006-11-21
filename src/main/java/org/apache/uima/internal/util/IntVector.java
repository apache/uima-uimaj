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

import java.io.Serializable;

/**
 * Like {@link java.util.Vector java.util.Vector}, but elements are <code>int</code>s. This is a
 * bare-bones implementation. May add features as I need them.
 * 
 * 
 * 
 */
public class IntVector implements Serializable {

  private static final int default_size = 16;

  private static final int default_growth_factor = 2;

  private static final int default_multiplication_limit = 2000000;

  private int growth_factor;

  private int multiplication_limit;

  // Points to the next free cell in the array.
  protected int pos = 0;

  protected int[] array = null;

  /**
   * Default constructor.
   */
  public IntVector() {
    this(default_size, default_growth_factor, default_multiplication_limit);
  }

  /**
   * Construct an IntVector from an array. The array is not copied and may subsequently be modified.
   * 
   * @param array
   *          The array the IntVector is initialized from.
   */
  public IntVector(int[] array) {
    if (array == null) {
      array = new int[0];
    }
    this.pos = array.length;
    this.array = array;
    this.growth_factor = default_growth_factor;
    this.multiplication_limit = default_multiplication_limit;
  }

  /**
   * Specify the initial capacity of this vector. Use to avoid internal copying if you know ahead of
   * time how large your vector is going to get (at least).
   * 
   * @param capacity
   *          Initial capacity of vector.
   */
  public IntVector(int capacity) {
    this(capacity, default_growth_factor, default_multiplication_limit);
  }

  /**
   * Specify the initial capacity, growth factor and multiplication limit of this vector. Use to
   * avoid internal copying if you know ahead of time how large your vector is going to get (at
   * least).
   * 
   * @param capacity
   *          Initial capacity of vector.
   * @param growth_factor
   *          Growth factor.
   * @param multiplication_limit
   *          Multiplication limit.
   */
  public IntVector(int capacity, int growth_factor, int multiplication_limit) {
    if (capacity < 0) {
      capacity = default_size;
    }
    if (growth_factor < 1) {
      growth_factor = default_growth_factor;
    }
    if (multiplication_limit < 1) {
      multiplication_limit = default_multiplication_limit;
    }
    this.growth_factor = growth_factor;
    this.multiplication_limit = multiplication_limit;
    this.array = new int[capacity];
  }

  public void setSize(int size) {
    if (size > 0) {
      this.ensure_size(size);
    }
  }

  /**
   * Add an element at the end of vector. Behaves like add(Object o) of
   * {@link java.util.Vector Vector}.
   */
  public void add(int element) {
    final int i = this.pos;
    ++this.pos;
    ensure_size(this.pos);
    this.array[i] = element;
  }

  /**
   * Add an element at a certain position in the vector. Elements later in the vector are shifted
   * right by one. If the position is past the end of the current vector, new <code>0</code>-valued
   * elements are added.
   */
  public void add(int index, int element) {
    if (index >= this.pos) {
      ensure_size(index + 1);
    } else {
      if (this.array.length <= this.pos) {
        ensure_size(this.pos + 1);
      } else {
        ++this.pos;
      }
      System.arraycopy(this.array, index, this.array, index + 1, this.pos - (index + 1));
    }
    this.array[index] = element;
  }

  /**
   * Set an element at a certain position in the vector.
   */
  public void set(int index, int element) {
    if (index >= this.pos) {
      throw new ArrayIndexOutOfBoundsException();
    }
    this.array[index] = element;
  }

  /**
   * Set an element at a certain position in the vector. Vector will grow.
   */
  public void put(int index, int element) {
    ensure_size(index + 1);
    this.array[index] = element;
  }

  /**
   * Retrieve the element at index.
   * 
   * @return The element at <code>index</code>.
   * @exception ArrayIndexOutOfBoundsException
   *              If <code>index</code> is not a valid index.
   */
  public int get(int index) {
    // Will throw an ArrayIndexOutOfBoundsException if out of bounds.
    if (index >= this.pos) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return this.array[index];
  }

  /**
   * Remove the element at a certain index.
   * 
   * @param index
   *          The index of the element to be removed.
   * @return The element at <code>index</code>.
   * @exception ArrayIndexOutOfBoundsException
   *              If <code>index</code> is not a valid index.
   */
  public int remove(int index) {
    if (index >= this.pos) {
      throw new ArrayIndexOutOfBoundsException();
    }
    --this.pos;
    int retval = this.array[index];
    for (int i = index; i < this.pos; i++) {
      this.array[i] = this.array[i + 1];
    }
    return retval;
  }

  /**
   * Remove all elements and set size to 0. Will not change current capacity.
   */
  public void removeAllElements() {
    this.pos = 0;
  }

  /**
   * Compares the specified <code>Object</code> with this <code>IntVector</code> for equality.
   * Two <code>IntVector</code>s are equal if and only if the object passed in <code>o</code>
   * is of type <code>IntVector</code>, <code>this.size() == o.size()</code>, and the <i>n</i>-th
   * element in this <code>IntVector</code> is equal to the <i>n</i>-th element in <code>o</code>
   * for all <i>n</i> &lt; <code>this.size()</code>.
   * 
   * @return <code>true</code> if the <code>IntVector</code>s are equal, <code>false</code>
   *         otherwise.
   */
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (!getClass().equals(o.getClass())) {
      return false;
    }
    IntVector v = (IntVector) o;
    if (size() != v.size()) {
      return false;
    }
    for (int i = 0; i < size(); i++) {
      if (this.array[i] != v.get(i)) {
        return false;
      }
    }

    return true;
  }

  /**
   * @return The number of elements in the vector.
   */
  public int size() {
    return this.pos;
  }

  /**
   * Tests if the specified <code>int</code> is a component of this <code>IntVector</code>.
   * 
   * @return <code>true</code> if and only if the <code>int</code> is an element of this
   *         <code>IntVector</code>, <code>false</code> otherwise.
   */
  public boolean contains(int elem) {
    return (position(elem) >= 0);
  }

  /**
   * Return the position of the first occurence of <code>elem</code> in the IntVector, if it
   * exists.
   * 
   * @param elem
   *          The element we're looking for.
   * @return The position, or <code>-1</code> if it doesn't exist.
   */
  public int position(int elem) {
    int i = 0;
    while (i < this.pos) {
      if (this.array[i] == elem) {
        return i;
      }
      ++i;
    }
    return -1;
  }

  /**
   * Set every element of the vector to some value.
   * 
   * @param value
   *          The fill value.
   */
  public void fill(int value) {
    java.util.Arrays.fill(this.array, value);
  }

  /**
   * Return the underlying int array, where the length of the returned array is equal to the
   * vector's size. This is not a copy!
   */
  public int[] toArray() {
    trimToSize();
    return this.array;
  }

  /**
   * Return a copy of the underlying array.
   */
  public int[] toArrayCopy() {
    final int max = this.size();
    int[] copy = new int[max];
    System.arraycopy(this.array, 0, copy, 0, max);
    return copy;
  }

  /** Return the internal array. */
  public int[] getArray() {
    return this.array;
  }

  /**
   * Returns the index of the first occurence of the element specified in this vector.
   * 
   * @return the index or <code>-1</code> if the element was not found.
   */
  public int indexOf(int element) {
    int index = -1;
    for (int i = 0; i < this.pos; i++) {
      if (element == this.array[i]) {
        index = i;
        break;
      }
    }

    return index;
  }

  /**
   * Reduce the size of the internal array to the number of current elements. You should only use
   * this if you know that your vector will not grow anymore.
   */
  public void trimToSize() {
    if (this.pos == this.array.length) {
      return;
    }
    int[] new_array = new int[this.pos];
    System.arraycopy(this.array, 0, new_array, 0, this.pos);
    this.array = new_array;
    return;
  }

  public IntVector copy() {
    IntVector copy = new IntVector(this.array.length, this.growth_factor, this.multiplication_limit);
    copy.pos = this.pos;
    for (int i = 0; i < this.pos; i++) {
      copy.array[i] = this.array[i];
    }
    return copy;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("[");
    for (int i = 0; i < this.pos; i++) {
      if (i > 0) {
        buf.append(", ");
      }
      buf.append(this.array[i]);
    }
    buf.append("]");
    return buf.toString();
  }

  public void ensure_size(int req) {
    this.array = IntArrayUtils.ensure_size(this.array, req, this.growth_factor,
                    this.multiplication_limit);
    if (this.pos < req) {
      this.pos = req;
    }
  }

  public int hashCode() {
    if (this.array == null) {
      return 0;
    }
    int sum = 0;
    for (int i = 0; i < this.size(); i++) {
      sum += this.get(i);
    }
    return sum;
  }
}
