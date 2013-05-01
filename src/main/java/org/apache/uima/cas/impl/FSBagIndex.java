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

package org.apache.uima.cas.impl;

import java.util.NoSuchElementException;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.internal.util.ComparableIntPointerIterator;
import org.apache.uima.internal.util.IntComparator;
import org.apache.uima.internal.util.IntPointerIterator;
import org.apache.uima.internal.util.IntVector;

public class FSBagIndex extends FSLeafIndexImpl {

  private class IntVectorIterator implements ComparableIntPointerIterator, LowLevelIterator {

    private int itPos;

    private IntComparator comp;

    private int modificationSnapshot; // to catch illegal modifications

    private int[] detectIllegalIndexUpdates; // shared copy with Index Repository

    private int typeCode;

    public boolean isConcurrentModification() {
      return this.modificationSnapshot != this.detectIllegalIndexUpdates[this.typeCode];
    }

    public void resetConcurrentModification() {
      this.modificationSnapshot = this.detectIllegalIndexUpdates[this.typeCode];
    }

    private IntVectorIterator() {
      super();
      this.itPos = 0;
    }

    private IntVectorIterator(IntComparator comp) {
      this();
      this.comp = comp;
    }

    public boolean isValid() {
      return ((this.itPos >= 0) && (this.itPos < FSBagIndex.this.index.size()));
    }

    public void moveToFirst() {
      this.itPos = 0;
    }

    public void moveToLast() {
      this.itPos = FSBagIndex.this.index.size() - 1;
    }

    public void moveToNext() {
      ++this.itPos;
    }

    public void moveToPrevious() {
      --this.itPos;
    }

    public int ll_get() {
      if (!isValid()) {
        throw new NoSuchElementException();
      }
      return FSBagIndex.this.index.get(this.itPos);
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#copy()
     */
    public Object copy() {
      IntVectorIterator copy = new IntVectorIterator(this.comp);
      copy.itPos = this.itPos;
      return copy;
    }

    /**
     * @see java.lang.Comparable#compareTo(Object)
     */
    public int compareTo(Object o) throws NoSuchElementException {
      return this.comp.compare(get(), ((IntVectorIterator) o).get());
    }

    /**
     * @see org.apache.uima.internal.util.IntPointerIterator#moveTo(int)
     */
    public void moveTo(int i) {
      final int position = find(i);
      if (position >= 0) {
        this.itPos = position;
      } else {
        this.itPos = -(position + 1);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.cas.impl.LowLevelIterator#ll_get()
     */
    public int get() throws NoSuchElementException {
      return ll_get();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.cas.impl.LowLevelIterator#moveToNext()
     */
    public void inc() {
      moveToNext();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.cas.impl.LowLevelIterator#moveToPrevious()
     */
    public void dec() {
      moveToPrevious();
    }

    public int ll_indexSize() {
      return FSBagIndex.this.size();
    }

    public LowLevelIndex ll_getIndex() {
      return FSBagIndex.this;
    }

  }

  // The index, a vector of FS references.
  private IntVector index;

  private int initialSize;

  FSBagIndex(CASImpl cas, Type type, int initialSize, int indexType) {
    super(cas, type, indexType);
    this.initialSize = initialSize;
    this.index = new IntVector(initialSize);
  }

  /**
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#init(org.apache.uima.cas.admin.FSIndexComparator)
   */
  boolean init(FSIndexComparator comp) {
    // The comparator for a bag index must be empty, except for the type. If
    // it
    // isn't, we create an empty one.
    FSIndexComparator newComp;
    if (comp.getNumberOfKeys() > 0) {
      newComp = new FSIndexComparatorImpl(this.lowLevelCAS);
      newComp.setType(comp.getType());
    } else {
      newComp = comp;
    }
    return super.init(newComp);
  }

  IntVector getVector() {
    return this.index;
  }

  public void flush() {
    this.index.removeAllElements();
    // don't do this - some iterators/ indexes are holding references to the index, and don't pick up the new one.
//    this.index = new IntVector(this.initialSize);
  }

  public final boolean insert(int fs) {
    this.index.add(fs);
    return true;
  }

  /*
   * public final boolean insert(int fs) { // First, check if we can insert at the end. final int []
   * indexArray = this.index.getArray(); final int length = this.index.size(); if (length == 0) {
   * this.index.add(fs); return true; } final int last = indexArray[length - 1]; if (compare(last,
   * fs) < 0) { index.add(fs); return true; } final int pos = this.binarySearch(indexArray, fs, 0,
   * length); if (pos >= 0) { return false; } index.add(-(pos+1), fs); return true; } // public
   * IntIteratorStl iterator() { // return new IntVectorIterator(); // }
   */

  // private final int find(int ele) {
  // return this.index.indexOf(ele);
  // // return binarySearch(this.index.getArray(), ele, 0, this.index.size());
  // }
  private final int find(int ele) {
    final int[] array = this.index.getArray();
    final int max = this.index.size();
    for (int i = 0; i < max; i++) {
      if (ele == array[i]) {
        return i;
      }
    }
    return -1;
  }

  public int compare(int fs1, int fs2) {
    if (fs1 < fs2) {
      return -1;
    } else if (fs1 > fs2) {
      return 1;
    } else {
      return 0;
    }
  }

  /*
   * // Do binary search on index. 
   * private final int binarySearch(int [] array, int ele, int start, int end) { 
   *   --end;  // Make end a legal value. 
   *   int i; // Current position 
   *   int comp; // Compare value 
   *   while (start <= end) { 
   *     i = (start + end) / 2; 
   *     comp = compare(ele, array[i]); 
   *     if (comp ==  0) { 
   *       return i; 
   *     } 
   *     if (start == end) {
   *       if (comp < 0) {
   *         return (-i)-1;
   *       } else { // comp > 0 
   *         return (-i)-2; // (-(i+1))-1
   *       } 
   *     } 
   *     if (comp < 0) {
   *       end = i-1; 
   *     } else { // comp > 0 
   *       start = i+1;
   *     } 
   *   } //This means that the input span is empty. 
   *   return (-start)-1; 
   * }
   */

  public ComparableIntPointerIterator pointerIterator(IntComparator comp,
          int[] detectIllegalIndexUpdates, int typeCode) {
    IntVectorIterator ivi = new IntVectorIterator(comp);
    ivi.modificationSnapshot = detectIllegalIndexUpdates[typeCode];
    ivi.detectIllegalIndexUpdates = detectIllegalIndexUpdates;
    ivi.typeCode = typeCode;
    return ivi;
  }

  /**
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#refIterator()
   */
  protected IntPointerIterator refIterator() {
    return new IntVectorIterator();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelIndex#ll_iterator()
   */
  public LowLevelIterator ll_iterator() {
    return new IntVectorIterator();
  }

  /**
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#refIterator(int)
   */
  protected IntPointerIterator refIterator(int fsCode) {
    IntVectorIterator it = new IntVectorIterator();
    final int pos = find(fsCode);
    if (pos >= 0) {
      it.itPos = pos;
    } else {
      it.itPos = -(pos + 1);
    }
    return it;
  }

  /**
   * @see org.apache.uima.cas.FSIndex#contains(FeatureStructure)
   */
  public boolean contains(FeatureStructure fs) {
    return (find(((FeatureStructureImpl) fs).getAddress()) >= 0);
  }

  public FeatureStructure find(FeatureStructure fs) {
    // Cast to implementation.
    FeatureStructureImpl fsi = (FeatureStructureImpl) fs;
    // Use internal find method.
    final int resultAddr = find(fsi.getAddress());
    // If found, create new FS to return.
    if (resultAddr > 0) {
      return fsi.getCASImpl().createFS(this.index.get(resultAddr));
    }
    // Not found.
    return null;
  }

  /**
   * @see org.apache.uima.cas.FSIndex#size()
   */
  public int size() {
    return this.index.size();
  }

  /**
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#deleteFS(org.apache.uima.cas.FeatureStructure)
   */
  public void deleteFS(FeatureStructure fs) {
    final int addr = ((FeatureStructureImpl) fs).getAddress();
    final int pos = this.index.indexOf(addr);
    if (pos >= 0) {
      this.index.remove(pos);
    }
  }

  public void remove(int fsRef) {
    final int pos = this.index.indexOf(fsRef);
    if (pos >= 0) {
      this.index.remove(pos);
    }
  }

  public int hashCode() {
    throw new UnsupportedOperationException();
  }

}
