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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.internal.util.CopyOnWriteOrderedFsSet_array;
import org.apache.uima.internal.util.OrderedFsSet_array;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Common index impl for set and sorted indexes.
 * 
 * Differences:
 *   - Number of "equal" (but not identical) FSs: Set: 1, Sorted, N
 *   - Iterators: Set: unordered, Sorted: ordered 
 *   
 * This is an index over just one type (excluding subtypes)
 * 
 * Uses a NavigableSet as the index (of FSs).  
 *   For sorted, because this is a set, to allow multiple (different) FSs having
 *     the same key values to be in the index, the key used for the index is augmented by a least-significant
 *     key which is the _id field of the FS itself.
 * 
 * @param <T> the Java class type for this index
 */
final public class FsIndex_set_sorted<T extends FeatureStructure> extends FsIndex_singletype<T> {
  
//  /**
//   * This impl of sorted set interface allows using the bulk add operation implemented in Java's 
//   * TreeSet - that tests if the argument being passed in is an instance of SortedSet and does a fast insert.
//   */


  // The index, a NavigableSet. 
  final private OrderedFsSet_array<T> indexedFSs;
    
  // only an optimization used for select.covering for AnnotationIndexes
  private int maxAnnotSpan = -1;
     
  FsIndex_set_sorted(CASImpl cas, Type type, int indexType, FSIndexComparator comparatorForIndexSpecs) {
    super(cas, type, indexType, comparatorForIndexSpecs);
   
    
    
    this.indexedFSs = new OrderedFsSet_array<T>(comparatorWithID, comparatorWithoutID);
  }

  @Override
  public void flush() {
    super.flush();
    this.indexedFSs.clear();
  }

  /**
   * @see org.apache.uima.cas.FSIndex#contains(FeatureStructure)
   * @param templateKey the feature structure
   * @return true if the fs is contained
   */
  @Override
  public boolean contains(FeatureStructure templateKey) {
    T r = find(templateKey);
    return r != null;
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.FsIndex_singletype#insert(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  void insert(T fs) {
    
    // past the initial load, or item is not > previous largest item to be added 
    maybeCopy();
    if (isAnnotIdx) {
      int span = ((Annotation)fs).getEnd() - ((Annotation)fs).getBegin();
      if (span > maxAnnotSpan) {
        maxAnnotSpan = span;
      }
    }
    indexedFSs.add(fs, isSorted() 
                              ? comparatorWithID 
                              : comparatorWithoutID);
  }

  /**
   * find any arbitrary matching FS
   *   two comparators:  cp, and cpx (has extra id comparing)
   * 
   * First find an FS in the index that's the smallest that's GE to key using cpx
   *   - if none found, then all of the entries in the index are LessThan the key (using cpx); 
   *                    but one might be equal using cp
   *     -- if one or more would be equal using cp, it would be because 
   *           the only reason for the inequality using cpx was due to the _id miscompare.
   *           Therefore we only need to check the last of the previous ones to see if it is cp equal
   *  - if we find one that is GE using cpx, 
   *     -- if it is equal then return it (any equal one is ok)
   *     -- if it is GT, then the ones preceding it are LessThan (using cpx) the key.
   *           Do the same check as above to see if the last of the preceding ones is equal using cp.
   *   
   * @param templateKey the matching fs template
   * @return an arbitrary fs that matches 
   */
  @Override
  public T find(FeatureStructure templateKey) {
    int pos = this.indexedFSs.findWithoutID((TOP)templateKey);
    return (pos >= 0)
             ? this.indexedFSs.getAtPos(pos)
             : null;
  }
  
//  @Override
//  public T find(FeatureStructure templateKey) {
//    if (null == templateKey || this.indexedFSs.isEmpty()) {
//      return null;
//    }
//    TOP found;
//    TOP templateKeyTop = (TOP) templateKey;
//    TOP fs1GEfs = this.indexedFSs.ceiling(templateKeyTop);
//    
//    if (fs1GEfs == null) {  // then all elements are less-that the templateKey
//      found = indexedFSs.lower(templateKeyTop);  //highest of elements less-than the template key
//      return (found == null) 
//               ? null 
//               : (comparatorWithoutID.compare(found, templateKeyTop) == 0) 
//                   ? (T)found 
//                   : null;
//    }
//    
//    // fs1GEfs is the least element that is greater-than-or-equal to the template key, using the fine-grained comparator
//    if (0 == comparatorWithoutID.compare(fs1GEfs, templateKeyTop)) {
//      return (T) fs1GEfs; 
//    }
//    
//    // fs1GEfs not null, GreaterThan the templateKey using comparatorWithoutID
//    // Therefore, the ones preceding it are LE using comparatorWithoutID
//    found = indexedFSs.lower(templateKeyTop);  // the greatest element in this set strictly less than the templateKey
//    return (found == null) 
//              ? null 
//              : (comparatorWithoutID.compare(found, templateKeyTop) == 0) 
//                   ? (T)found 
//                   : null;
//  }
//
//  public T findLeftmost(TOP templateKey) {
//    // descending iterator over elements LessThan templateKey
//    // iterator is over TOP, not T, to make compare easier
//    Iterator<TOP> it = indexedFSs.headSet(templateKey, false).descendingIterator();
//  
//    TOP elementBefore = null;
//    TOP lastEqual = null;
//    // move to left until run out or have element not equal using compareWihtoutID to templateKey
//    while (it.hasNext()) {
//      if (0 != comparatorWithoutID.compare(elementBefore = it.next(), templateKey)) {
//        break;
//      }
//      lastEqual = elementBefore;
//    }
//  
//    if (!it.hasNext()) { // moved past beginning
//      return (T) elementBefore;  // might return null to indicate not found
//    }
//    return (T) lastEqual;
//  }

  /**
   * @see org.apache.uima.cas.FSIndex#size()
   */
  @Override
  public int size() {
    return this.indexedFSs.size()/* + itemsToBeAdded.size()*/;
  }

  /**
   * This code is written to remove (if it exists)
   * the exact FS, not just one which matches in the sort comparator.
   *
   * @see org.apache.uima.cas.impl.FSLeafIndexImpl#deleteFS(org.apache.uima.cas.FeatureStructure)
   * @param fs the feature structure to remove
   * @return true if it was in the index previously
   */
  /**
   * 
   */
  @Override
  public boolean deleteFS(T fs) {
//    maybeProcessBulkAdds(); // moved to OrderedFsSet_array class
    maybeCopy();
    return this.indexedFSs.remove((TOP) fs);
  }
  
  @Override
  protected void bulkAddTo(List<T> v) {
    v.addAll((Collection<? extends T>) indexedFSs);
  }
     
  @Override
  public LowLevelIterator<T> iterator() {
    CopyOnWriteIndexPart cow_wrapper = getNonNullCow();
    // if index is empty, return never-the-less a real iterator,
    //   not an empty one, because it may become non-empty
    return casImpl.inPearContext()
//             ? new FsIterator_set_sorted_pear<>(this, type, this)
//             : new FsIterator_set_sorted     <>(this, type, this);
             ? new FsIterator_set_sorted_pear<T>(this, cow_wrapper)
             : new FsIterator_set_sorted2<T>(this, cow_wrapper);
  }
  
  @Override
  protected CopyOnWriteIndexPart createCopyOnWriteIndexPart() {
    if (CASImpl.traceCow) {
      this.casImpl.traceCowCopy(this);
    }
    return new CopyOnWriteOrderedFsSet_array(indexedFSs);
  }
  
  public int ll_maxAnnotSpan() {
    return maxAnnotSpan;
  }
  
  Comparator<TOP> getComparatorWithId() {
    return comparatorWithID;
  }
}
