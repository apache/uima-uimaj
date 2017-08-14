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

/**
 * Used for sorted indexes only
 * Uses IntVector (sorted) as the index (of FSs)
 * @param <T> the Java cover class type for this index, passed along to (wrapped) iterators producing Java cover classes
 */
public class FSIntArrayIndex<T extends FeatureStructure> extends FSLeafIndexImpl<T> {

  // The index, a vector of FS references.
  final private IntVector indexIntVector;
  
  final private int initialSize;
  
  final private boolean isAnnotationIndex;
    
  private IntComparator annotationIntComparator = null; // lazy init because index repo not set up initially

  FSIntArrayIndex(CASImpl cas, Type type, int initialSize, int indexType, boolean isAnnotationIndex) {
    super(cas, type, indexType);
    this.initialSize = initialSize;
    this.indexIntVector = new IntVector(initialSize);
    this.isAnnotationIndex = isAnnotationIndex;
  }

  /**
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#init(org.apache.uima.cas.admin.FSIndexComparator)
   */
  boolean init(FSIndexComparator comp) {
    boolean rc = super.init(comp);
    return rc;
  }

  IntVector getVector() {
    return this.indexIntVector;
  }

  public void flush() {
    // do this way to reset size if it grew
    if (this.indexIntVector.size() > this.initialSize) {
      this.indexIntVector.resetSize(initialSize);
    } else {
      this.indexIntVector.removeAllElements();
    }
  }

  // public final boolean insert(int fs) {
  // this.index.add(fs);
  // return true;
  // }

  public final boolean insert(int fs) {
    // First, check if we can insert at the end.
    final int[] indexArray = this.indexIntVector.getArray();
    final int length = this.indexIntVector.size();
    
    if (length == 0) {
      this.indexIntVector.add(fs);
      return true;
    }
    final int last = indexArray[length - 1];
    // can't use compare <= because the = implies (depending on IS_ALLOW_DUP_A...)
    //   more work to find the EQ one, or not
    if (compare(last, fs) < 0) {
      this.indexIntVector.add(fs);
      return true;
    }
    
    int pos = findExact(fs);
    
    // This rather complex logic can't be simplified due to edge cases, and the need
    // to have inserts for = compare but unequal identity things go in ascending 
    // over time insert order (a test case need)
    if (pos >= 0 && !FSIndexRepositoryImpl.IS_ALLOW_DUP_ADD_2_INDEXES) {
      return false; // was already exactly in the index, but it's not allowed to add duplicates, so skip
//      int pos2 = refineToExactFsSearch(fs, pos);
//      if (pos2 < 0) { 
//        // the exact match wasn't found, OK to add
//        pos = refineToV3Position(pos, fs);
//        this.indexIntVector.add(pos + 1, fs);
//        
//      }
    } else if (pos >= 0) {
      // was already exactly in the index, and it's ok to add duplicates
      this.indexIntVector.add(pos + 1, fs);
      return true;
    } else {
      // was not exactly in the index
      this.indexIntVector.add(-(pos + 1), fs);
    }
    return true;
  }

  final boolean insert(int fs, int count) {
    // First, check if we can insert at the end.
    final int[] indexArray = this.indexIntVector.getArray();
    final int length = this.indexIntVector.size();
    
    if (length == 0) {
      this.indexIntVector.multiAdd(fs, count);
      return true;
    }
    final int last = indexArray[length - 1];
    // can't use compare <= because the = implies (depending on IS_ALLOW_DUP_A...)
    //   more work to find the EQ one, or not
    if (compare(last, fs) < 0) {
      this.indexIntVector.multiAdd(fs, count);
      return true;
    }
    
    int pos = findExact(fs);
    
    // This rather complex logic can't be simplified due to edge cases, and the need
    // to have inserts for = compare but unequal identity things go in ascending 
    // over time insert order (a test case need)
    if (pos >= 0 && !FSIndexRepositoryImpl.IS_ALLOW_DUP_ADD_2_INDEXES) {
      return false; // was already exactly in the index, not ok to add dups, so skip
//      int pos2 = refineToExactFsSearch(fs, pos);
//      if (pos2 < 0) { 
//        // the exact match wasn't found, OK to add
//        this.indexIntVector.multiAdd(pos + 1, fs, count);
//      } else {
//        return false; // was already in the index, not ok to add dups, so skip       
//      }
    } else if (pos >= 0) {
      // was already exactly in the index, and it's ok to add duplicates
      this.indexIntVector.multiAdd(pos + 1, fs, count);
      return true;
    } else {
      // was not in the index
      this.indexIntVector.multiAdd(-(pos + 1), fs, count);
      return true;
    }
  }

  // public IntIteratorStl iterator() {
  // return new IntVectorIterator();
  // }

  /**
   * In a sorted array of fsAddrs, find one (perhaps out of many) that matches the keys in fsRef, or
   * if none match, return a negative number of insertion point if not found
   * @param fsRef
   * @return index of an arbitrary FS that matches on the compare function or a negative number of insertion point if not found
   */
  private final int find(int fsRef) {
    return binarySearch(this.indexIntVector.getArray(), fsRef, 0, this.indexIntVector.size());
  }
  
  private final int findExact(int fsRef) {
    return binarySearchExact(this.indexIntVector.getArray(), fsRef, 0, this.indexIntVector.size());
  }
  
  /**
   * In a sorted array of fsAddrs, find the lowest one that matches the keys in fsRef, or
   * if none match, return a negative number of insertion point if not found
   * @param fsRef
   * @return index of the left-most FS for this particular type that matches on the compare function or a negative number of insertion point if not found
   */  
  final int findLeftmost(int fsRef) {
    int pos = find(fsRef);
    
    // https://issues.apache.org/jira/browse/UIMA-4094
    // make sure you go to earliest one
    if (pos < 0 || pos >= size()) {
      // this means the moveTo found the insert point at the end of the index
      // so just return invalid, since there's no way to return an insert point for a position
      // that satisfies the FS at that position is greater than fs  
      return pos;
    }    
    // Go back until we find a FS that is really smaller
    while (true) {
      pos --;
      if (pos >= 0) {
        int prev = indexIntVector.get(pos);
        if (compare(prev, fsRef) != 0) {
          pos++; // go back
          break;
        }
      } else {
        pos = 0;  // went to before first, so go back to 1st
        break;
      }
    }
    return pos;
  }
  
  /**
   * Like find, but if found, returns position of Exact FS spot or neg of an insert spot (if no == match)
   * @param fsRef
   * @return position of Exact FS spot or neg of an insert spot (if no == match)
   */
  final int findEq(int fsRef) {
    return findExact(fsRef);
//    int pos = find(fsRef);
//    if (pos < 0) {
//      return pos;
//    }
//    int pos2 = refineToExactFsSearch(fsRef, pos);
//    return (pos2 == -1) ? (-pos) -1 : pos2;
  }

  // private final int find(int ele)
  // {
  // final int[] array = this.index.getArray();
  // final int max = this.index.size();
  // for (int i = 0; i < max; i++)
  // {
  // if (compare(ele, array[i]) == 0)
  // {
  // return i;
  // }
  // }
  // return -1;
  // }

  // public int compare(int fs1, int fs2)
  // {
  // if (fs1 < fs2)
  // {
  // return -1;
  // } else if (fs1 > fs2)
  // {
  // return 1;
  // } else
  // {
  // return 0;
  // }
  // }

  // Do binary search on index.
  // return negative number of insertion point if not found
  //   insertion point is the index of the element after the one that's less than the key
  //   insertion point can be an index one past the end of the index if key is > all the elements
  // return index of an arbitrary FS that matches on the compare function
  private final int binarySearch(int[] array, int ele, int start, int end) {
    --end; // Make end a legal value.
    int i; // Current position
    int comp; // Compare value
    while (start <= end) {
      i = (int)(((long)start + end) / 2);
      comp = compare(ele, array[i]);
      if (comp == 0) {
        return i;
      }
      if (start == end) {
        if (comp < 0) {
          return (-i) - 1;
        }
        // comp > 0
        return (-i) - 2; // (-(i+1))-1
      }
      if (comp < 0) {
        end = i - 1;
      } else { // comp > 0
        start = i + 1;
      }
    }
    // This means that the input span is empty.
    return (-start) - 1;
  }

  private final int binarySearchExact(int[] array, int ele, int start, int end) {
    --end; // Make end a legal value.
    int i; // Current position
    int comp; // Compare value
    while (start <= end) {
      i = (int)(((long)start + end) / 2);
      comp = compare(ele, array[i]);
      if (comp == 0) {
        comp = Integer.compare(ele, array[i]);
        if (comp == 0) {
          return i;
        }
      }
      if (start == end) {
        if (comp < 0) {
          return (-i) - 1;
        }
        // comp > 0
        return (-i) - 2; // (-(i+1))-1
      }
      if (comp < 0) {
        end = i - 1;
      } else { // comp > 0
        start = i + 1;
      }
    }
    // This means that the input span is empty.
    return (-start) - 1;
  }

  /**
   * The comp value is only used when ordering iterators in type/subtype collections
   */
  public ComparableIntPointerIterator<T> pointerIterator(
      IntComparator comp, int[] detectIllegalIndexUpdates, int typeCode) {
    return new IntIterator4sorted<T>(this, detectIllegalIndexUpdates, comp);
  }

  /**
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#refIterator()
   */
  protected IntPointerIterator refIterator() {
    return new IntIterator4sorted<T>(this, null);  // null means no detectIllegalIndexUpdates checking
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelIndex#ll_iterator()
   */
  public LowLevelIterator ll_iterator() {
    return new IntIterator4sorted<T>(this, null);  // null means no detectIllegalIndexUpdates checking
  }

  /**
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#refIterator(int)
   */
  protected IntPointerIterator refIterator(int fsAddr) {
    IntIterator4sorted<T> it = new IntIterator4sorted<T>(this, null);
    it.moveTo(fsAddr);
    return it;
  }

  /**
   * @see org.apache.uima.cas.FSIndex#contains(FeatureStructure)
   * @param fs the feature structure
   * @return true if the fs is contained
   */
  public boolean contains(FeatureStructure fs) {
    return (find(((FeatureStructureImpl) fs).getAddress()) >= 0);
  }
  
  public boolean ll_containsEq(int fsAddr) {
    return findEq(fsAddr) >= 0;
  }

  public FeatureStructure find(FeatureStructure fs) {
    // Cast to implementation.
    FeatureStructureImpl fsi = (FeatureStructureImpl) fs;
    final int fsRef = fsi.getAddress();
    // Use internal find method.
    final int resultAddr = find(fsRef);
    // If found, create new FS to return.
    if (resultAddr >= 0) {
      int foundFsRef = this.indexIntVector.get(resultAddr);
      return (fsRef == foundFsRef) ? 
          fs : 
          fsi.getCASImpl().createFS(foundFsRef);
    }
    // Not found.
    return null;
  }

  /**
   * @see org.apache.uima.cas.FSIndex#size()
   */
  public int size() {
    return this.indexIntVector.size();
  }

  /**
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#deleteFS(org.apache.uima.cas.FeatureStructure)
   */
  public void deleteFS(FeatureStructure fs) {
    final int addr = ((FeatureStructureImpl) fs).getAddress();
    remove(addr);
  }
  
  /*
   * Some day we may want to remove all occurrences of this feature structure, not just the
   * first one we come to (in the case where the exact identical FS has been added to the 
   * index multiple times).  The issues around this are:
   *   multiple adds are lost on serialization/ deserialization
   *   it take time to remove all instances - especially from bag indexes
   */
  
  /*
   * This code is written to remove (if it exists)
   * the exact FS, not just one which matches in the sort comparator.
   * 
   * It only removes one of the exact FSs, if the same FS was indexed more than once.
   * @param fsRef the address of the fs to be removed
   * @return true if was in the index  
   */
  @Override
  public boolean remove(int fsRef) {
    int pos = findEq(fsRef);  // finds  == to fsRef identity equals
    if (pos < 0) {
      return false;  // not in index
    }
    this.indexIntVector.remove(pos);
    return true;
  }

  @Override
  protected void bulkAddTo(IntVector v) {
    v.addBulk(indexIntVector);
  }
  
  @Override
  public int compare(int fs1, int fs2) {
    if (isAnnotationIndex) {
      if (annotationIntComparator == null) {
        annotationIntComparator = lowLevelCAS.indexRepository.getAnnotationIntComparator();
      }
      return annotationIntComparator.compare(fs1, fs2);
    }
    return super.compare(fs1, fs2);
  }

}
