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

import java.util.NoSuchElementException;

/**
 * This class implements a set of integers. It does not implement the <code>Set</code> interface
 * for performance reasons, though methods with the same name are equivalent.
 * 
 *  This does not implement shorts + offset, like the IntHashSet does
 *    because by the time that might be of interest, we would switch to
 *    IntHashSet to get ~O(1) operations including contains.
 *    
 */
public class IntSet implements PositiveIntSet {

  /** The data. */
  private IntVector iVec;

  /** Creates a new instance of this set. */
  public IntSet() {
    this.iVec = new IntVector();
  }
  
  /**
   * 
   * @param capacity allocate enough space to hold at least this before expanding
   */
  public IntSet(int capacity) {
    this.iVec = new IntVector(capacity);
  }

  /**
   * Adds the specified int to this set.
   * 
   * @param element
   *          the integer to be added.
   * @return <code>true</code> if this set did not already contain this element,
   *         <code>false</code> otherwise.
   */
  @Override
  public boolean add(int element) {
    if (!this.iVec.contains(element)) {
      this.iVec.add(element);
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
  @Override
  public boolean contains(int element) {
    return this.iVec.contains(element);
  }

  @Override
  public int find(int element) {
    return this.iVec.indexOf(element);
  }

  /** @return the size of this set. */
  @Override
  public int size() {
    return this.iVec.size();
  }

  /** @return the <code>n</code>-th element in this set. */
  /**
   * Used for FsBagIndex, and internally to compute most positive/neg
   * @param n the position
   * @return the value at that position
   */
  @Override
  public int get(int n) {  
    return this.iVec.get(n);
  }

  /** Removes the <code>n</code>-th element in this set.
   * @param n - 
   * */
  public void removeElementAt(int n) {
    this.iVec.remove(n);
  }

  /**
   * Tests if two sets are equal. This is the case if the two sets are of the same size, and every
   * element in one set in contained in the other set.<br>
   * Note that in order to increase performance, before the sets are actually compared the way
   * described above, the sums of the elements in both sets are calculated, ignoring possible int
   * overflows. If the sums are not equal, the two sets cannot be equal. In case the sums are equal,
   * the two sets are compared element by element.
   * 
   * @param o
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
      // maybe a speedup - is order size(), vs order size*size
      int sum1 = 0;
      int sum2 = 0;
      final IntVector v1 = this.iVec;
      final IntVector v2 = s.iVec;
      for (int i = 0; i < size; i++) {
        sum1 += v1.get(i);
        sum2 += v2.get(i);
      }
      if (sum1 != sum2)
        return false;

      for (int i = 0; i < size; i++) {
        if (!s.contains(v1.get(i)))
          return false;
      }
      return true;
    }
    return false;
  }

  public int hashCode() {
    if (this.iVec == null) {
      return 0;
    }
    int sum = 0;
    for (int i = 0; i < this.size(); i++) {
      sum += this.iVec.get(i);
    }
    return sum;
  }
  
  public int indexOf(int element) {
	return  this.iVec.indexOf(element);
  }

  @Override
  public void clear() {
    iVec.removeAllElements(); // doesn't reallocate
  }

  @Override
  public boolean remove(int key) {
    int i = iVec.indexOfOptimizeAscending(key);
    if (i != -1) {
      iVec.remove(i);
    }
    return i != -1;
  }
  
  private class IntSetIterator implements IntListIterator {

    protected int pos = 0;
    
    protected IntSetIterator() {}
 
    @Override
    public final boolean hasNext() {
      return (pos >= 0 && pos < size());
    }

    @Override
    public final int next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return iVec.get(pos++);
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#hasPrevious()
     */
    @Override
    public boolean hasPrevious() {
      final int posm1 = pos - 1;
      return (posm1 >= 0 && posm1 < size());
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#previous()
     */
    @Override
    public int previous() {
      if (!hasPrevious()) {
        throw new NoSuchElementException();
      }
      return iVec.get(pos--);      
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#moveToEnd()
     */
    @Override
    public void moveToEnd() {
      pos = size() - 1;
    }

    /**
     * @see org.apache.uima.internal.util.IntListIterator#moveToStart()
     */
    @Override
    public void moveToStart() {
      pos = 0;
    }

  }

  @Override
  public IntSetIterator iterator() {
    return new IntSetIterator();
  }

  @Override
  public int moveToFirst() {
    return (size() == 0) ? -1 : 0;
  }

  @Override
  public int moveToLast() {
    return size() -1;
  }

  @Override
  public int moveToNext(int position) {
    if (position < 0) {
      return -1;
    }
    final int r = position + 1;
    return (size() <= r) ? -1 : r;
  }

  @Override
  public int moveToPrevious(int position) {
    if (position >= size()) {
      return -1;
    }
    return (position < 0) ? -1 : position - 1;
  }

  @Override
  public boolean isValid(int position) {
    // TODO Auto-generated method stub
    return (position >= 0) && (position < size());
  }

  @Override
  public void bulkAddTo(IntVector v) {
    v.addBulk(iVec);
  }

  @Override
  public int[] toIntArray() {
    return iVec.toIntArray();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return String.format("IntSet [iVec=%s]", iVec);
  }
  
  
}
