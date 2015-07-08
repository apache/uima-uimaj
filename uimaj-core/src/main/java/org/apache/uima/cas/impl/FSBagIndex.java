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

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.internal.util.ComparableIntPointerIterator;
import org.apache.uima.internal.util.IntComparator;
import org.apache.uima.internal.util.IntPointerIterator;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.PositiveIntSet;
import org.apache.uima.internal.util.PositiveIntSet_impl;

/**
 * Used for UIMA FS Bag Indexes
 * Uses IntVector or PositiveIntSet to hold values of FSs
 * @param <T> the Java cover class type for this index, passed along to (wrapped) iterators producing Java cover classes
 */
public class FSBagIndex<T extends FeatureStructure> extends FSLeafIndexImpl<T> {
  
  // package private
  final static boolean USE_POSITIVE_INT_SET = !FSIndexRepositoryImpl.IS_ALLOW_DUP_ADD_2_INDEXES;

  // The index, a vector of FS references.
  final private IntVector index;
  
  final private PositiveIntSet indexP = USE_POSITIVE_INT_SET ? new PositiveIntSet_impl() : null;

  private int initialSize;

  FSBagIndex(CASImpl cas, Type type, int initialSize, int indexType) {
    super(cas, type, indexType);
    this.initialSize = initialSize;
    this.index = USE_POSITIVE_INT_SET ? null : new IntVector(initialSize);
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

  public void flush() {
    // done this way to reset to initial size if it grows
    if (USE_POSITIVE_INT_SET) {
      indexP.clear();      
    } else {
      if (this.index.size() > this.initialSize) {
        this.index.resetSize(this.initialSize);
//        this.index = new IntVector(this.initialSize);
      } else {
        this.index.removeAllElements();
      }
    }
  }

  public final boolean insert(int fs) {
    if (USE_POSITIVE_INT_SET) {
      return indexP.add(fs);
    } else {
      index.add(fs);
      return true;  // supposed to return true if added, but can't tell, return value ignored anyways
    }
  }

  /**
   * 
   * @param ele the element to find
   * @return the position of the element, or if not found, -1
   */
  private int find(int ele) {
    if (USE_POSITIVE_INT_SET) {
      return indexP.find(ele);
    } else {
      return this.index.indexOfOptimizeAscending(ele);
    }
  }

  /**
   * Left most in case there are multiple instances of the same item
   * This only works if not use Positive Int Set
   * because that's the only time there are multiple instances of the same
   * (meaning having the same heap address) item
   * @param ele the featuresturcture to match
   * @return -1 if the item is not found, or a position value that can be used with iterators to start at that item.
   */
  int findLeftmost(int ele) {
    if (USE_POSITIVE_INT_SET) {
      return indexP.find(ele);
    } else {
      return this.index.indexOf(ele);
    }
  }
  
  /**
   * For bag indexes, compare equal only if identical addresses
   */
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

  public ComparableIntPointerIterator<T> pointerIterator(IntComparator comp,
          int[] detectIllegalIndexUpdates, int typeCode) {
    return new IntIterator4bag<T>(this, detectIllegalIndexUpdates);
  }

  /**
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#refIterator()
   */
  protected IntPointerIterator refIterator() {
    return new IntIterator4bag<T>(this, null);  // no concurrent mod checking, internal use
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelIndex#ll_iterator()
   */
  public LowLevelIterator ll_iterator() {
    return new IntIterator4bag<T>(this, null); // no concurrent mod checking
  }

  /**
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#refIterator(int)
   */
  protected IntPointerIterator refIterator(int fsCode) {
    IntIterator4bag<T> it = new IntIterator4bag<T>(this, null); // no concurrent mod checking, internal use
    it.moveTo(fsCode);
    return it;
  }

  /**
   * @see org.apache.uima.cas.FSIndex#contains(FeatureStructure)
   * @param fs A Feature Structure used a template to match for equality with the
   *           FSs in the index.
   * @return <code>true</code> if the index contains such an element.
   */
  public boolean contains(FeatureStructure fs) {
    return ll_contains(((FeatureStructureImpl) fs).getAddress());
  }
  
  boolean ll_contains(int fsAddr) {
    return USE_POSITIVE_INT_SET ?
        indexP.contains(fsAddr) :
        (find(fsAddr) >= 0);
  }

  public FeatureStructure find(FeatureStructure fs) {
    if (USE_POSITIVE_INT_SET) {
      final FeatureStructureImpl fsi = (FeatureStructureImpl) fs;
      final int addr = fsi.getAddress();
      return (indexP.contains(addr)) ? fsi.getCASImpl().createFS(addr) : null;
    }
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
    return USE_POSITIVE_INT_SET ? indexP.size() : index.size();
  }

  /**
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#deleteFS(org.apache.uima.cas.FeatureStructure)
   */
  public void deleteFS(FeatureStructure fs) {
    remove( ((FeatureStructureImpl) fs).getAddress());  
  }
  
  @Override
  public boolean remove(int fsRef) {
    if (USE_POSITIVE_INT_SET) {
      return indexP.remove(fsRef);
    } else {
      final int pos = this.index.indexOfOptimizeAscending(fsRef);
      if (pos >= 0) {
        this.index.remove(pos);
        return true;
      } else {
        return false;
      }
    }
  }

  public int hashCode() {
    throw new UnsupportedOperationException();
  }

  @Override
  boolean insert(int fs, int count) {
    // only need this multi-insert to support Set and Sorted indexes for
    // protectIndexes kinds of things
    throw new UnsupportedOperationException();
  }

  @Override
  protected void bulkAddTo(IntVector v) {
    if (USE_POSITIVE_INT_SET) {
      indexP.bulkAddTo(v);
    } else {
      v.addBulk(index);
    }    
  }
  
  /*
   * Iterator support 
   */
  boolean isValid(int itPos) {
    if (USE_POSITIVE_INT_SET) {
      return indexP.isValid(itPos);
    } else {
      return (itPos >=0) && (itPos < index.size());
    }
  }
  
  int moveToFirst() {
    return USE_POSITIVE_INT_SET ? 
          indexP.moveToFirst() : 
          ((index.size() == 0) ? -1 : 0);
  }

  int moveToLast() {
    return FSBagIndex.USE_POSITIVE_INT_SET ? 
        indexP.moveToLast() :
        index.size() - 1;
  }
  
  int moveToNext(int itPos) {
    return USE_POSITIVE_INT_SET ? 
      indexP.moveToNext(itPos) :
      (itPos < 0) ? -1 : itPos + 1;
  }
  
  int moveToPrevious(int itPos) {
    return USE_POSITIVE_INT_SET ? 
      indexP.moveToPrevious(itPos) :
      (itPos >= index.size())? -1 : (itPos - 1);
  }
  
  int get(int itPos) {
    return FSBagIndex.USE_POSITIVE_INT_SET ?
        indexP.get(itPos) :
        index.get(itPos);
  }

}
