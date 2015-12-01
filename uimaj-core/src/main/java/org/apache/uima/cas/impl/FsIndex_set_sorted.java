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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.jcas.cas.TOP;

/**
 * Common index impl for set and sorted indexes.
 * 
 * Differences:
 *   - Number of "equal" (but not identical) FSs: Set: 1, Sorted, N
 *   - Iterators: Set: unordered, Sorted: ordered 
 * 
 * The FSs are kept in a TreeSet in an ordering, to permit faster searching.
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
public class FsIndex_set_sorted<T extends TOP> extends FsIndex_singletype<T> {
  
  final private SortedSet<TOP> ss = new SortedSet<TOP>() {
    
    @Override
    public int size() { return itemsToBeAdded.size(); }
    @Override
    public boolean isEmpty() { return false; }
    @Override
    public boolean contains(Object o) { throw new UnsupportedOperationException(); }
    @Override
    public Iterator<TOP> iterator() { return itemsToBeAdded.iterator(); }
    @Override
    public Object[] toArray() { throw new UnsupportedOperationException(); }
    @Override
    public <U> U[] toArray(U[] a) { throw new UnsupportedOperationException(); }
    @Override
    public boolean add(TOP e) { throw new UnsupportedOperationException(); }
    @Override
    public boolean remove(Object o) { throw new UnsupportedOperationException(); }
    @Override
    public boolean containsAll(Collection<?> c) { throw new UnsupportedOperationException(); }
    @Override
    public boolean addAll(Collection<? extends TOP> c) { throw new UnsupportedOperationException(); }
    @Override
    public boolean retainAll(Collection<?> c) { throw new UnsupportedOperationException(); }
    @Override
    public boolean removeAll(Collection<?> c) { throw new UnsupportedOperationException(); }
    @Override
    public void clear() { throw new UnsupportedOperationException(); }
    @Override
    public Comparator<TOP> comparator() { return comparator; }
    @Override
    public SortedSet<TOP> subSet(TOP fromElement, TOP toElement) { throw new UnsupportedOperationException(); }
    @Override
    public SortedSet<TOP> headSet(TOP toElement) { throw new UnsupportedOperationException(); }
    @Override
    public SortedSet<TOP> tailSet(TOP fromElement) { throw new UnsupportedOperationException(); }
    @Override
    public TOP first() { throw new UnsupportedOperationException(); }
    @Override
    public TOP last() { throw new UnsupportedOperationException(); }     
  };


  // The index, a NavigableSet. 
  final private TreeSet<TOP> indexedFSs;
  
  final private Comparator<TOP> comparator;
  
  final private ArrayList<TOP> itemsToBeAdded = new ArrayList<>();  // to batch the adds
  
  private TOP largestItem = null;
   
  FsIndex_set_sorted(CASImpl cas, Type type, int indexType, FSIndexComparator comparatorForIndexSpecs, boolean useSorted) {
    super(cas, type, indexType, comparatorForIndexSpecs);
    FSIndexRepositoryImpl ir = this.casImpl.indexRepository;
    
    if (ir.isAnnotationIndex(comparatorForIndexSpecs, indexType)) {
      comparator = ir.getAnnotationFsComparatorWithId();   
    } else {
      comparator = useSorted   
          ? (o1, o2) -> {
              final int c = compare(o1,  o2); 
              // augment normal comparator with one that compares IDs if everything else equal
              return (c == 0) ? (Integer.compare(o1.id(), o2.id())) : c;} 
          : (o1, o2) -> compare(o1,  o2);
    }          
    this.indexedFSs = new TreeSet<TOP>(comparator);
  }

  @Override
  public void flush() {
    this.indexedFSs.clear();
    this.itemsToBeAdded.clear();
    this.itemsToBeAdded.trimToSize();
    this.largestItem = null;
  }

  /**
   * @see org.apache.uima.cas.FSIndex#contains(FeatureStructure)
   * @param fs the feature structure
   * @return true if the fs is contained
   */
  @Override
  public boolean contains(FeatureStructure templateKey) {
    T r = find(templateKey);
    return r != null;
  }
  
  /**
   * @param fs the FeatureStructure to see if it is in the set
   * @return true if this exact fs is in the set
   */
  @Override
  public boolean containsEq(FeatureStructureImplC fs) {
    maybeProcessBulkAdds();
    return CASImpl.isSameCAS(casImpl, fs.getCAS()) && indexedFSs.contains(fs);
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.FsIndex_singletype#insert(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  boolean insert(T fs) {
    // measured - not efficient.
//    if (indexedFSs.size() > 1024) {
//      // optimize for insert at end
//      TOP last = indexedFSs.last();
//      if (indexedFSs.comparator().compare(last, fs) <= 0) {
//        // insert at end fast path maybe
//        return indexedFSs.tailSet(last, true).add(fs);
//      }
//    }
    if (largestItem != null && comparator.compare(fs,  largestItem) > 0) {
      itemsToBeAdded.add(fs);
      largestItem = fs;
      return true;
    } else {
      if (largestItem == null) {
        largestItem = fs;
        itemsToBeAdded.add(fs);
        return true;
      }
      
      maybeProcessBulkAdds(); // we do this so the return value from add is accurate
      return this.indexedFSs.add((TOP)fs);
    }
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
   * @param fs the matching fs template
   * @return an arbitrary fs that matches 
   */
  @Override
  public T find(FeatureStructure templateKeyIn) {
    maybeProcessBulkAdds();
    TOP templateKey = (TOP) templateKeyIn;
    if (null == templateKey || this.indexedFSs.size() == 0) {
      return null;
    }
    T found;
    TOP fs1GEfs = this.indexedFSs.ceiling(templateKey);
    
    if (fs1GEfs == null) {  // then all elements are less-that the templateKey
      found = (T) indexedFSs.lower(templateKey);  //highest of elements less-than the template key
      return (found == null) ? null : (compare(found, templateKey) == 0) ? found : null;
    }
    
    // fs1GEfs is the least element that is greater-than-or-equal to the template key, using the fine-grained comparator
    if (0 == compare(fs1GEfs, templateKey)) {
      return (T) fs1GEfs; 
    }
    
    // fs1GEfs not null, GreaterThan the templateKey using compare
    // Therefore, the ones preceding it are LessThan using the index comparator, but 
    //   may be equal using the compare.
    found = (T) indexedFSs.lower(templateKey);  // the greatest element in this set strictly less than the templateKey
    return (found == null) ? null : (compare(found, templateKey) == 0) ? found : null;
  }

  public T findLeftmost(TOP templateKey) {
    maybeProcessBulkAdds();
    // descending iterator over elements LessThan templateKey
    Iterator<T> it = (Iterator<T>) indexedFSs.headSet(templateKey, false).descendingIterator();
  
    T elementBefore = null;
    T lastEqual = null;
    // move to left until run out or have element not equal using compare to templateKey
    while (it.hasNext() && (0 == compare(elementBefore = it.next(), templateKey))) {
      lastEqual = elementBefore;
    }
  
    if (!it.hasNext()) { // moved past beginning
      return elementBefore;  // might return null to indicate not found
    }
    return lastEqual;
  }

  /**
   * @see org.apache.uima.cas.FSIndex#size()
   */
  @Override
  public int size() {
    return this.indexedFSs.size() + itemsToBeAdded.size();
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
    maybeProcessBulkAdds();
    return this.indexedFSs.remove(fs);
  }
  
  @Override
  protected void bulkAddTo(List<TOP> v) {
    maybeProcessBulkAdds();
    v.addAll(indexedFSs);
  }
  
//  @Override
//  protected void bulkAddTo(IntVector v) {
//    this.indexedFSs.stream().mapToInt(fs -> ((FeatureStructureImplC)fs).id()).forEach(v::add);
//  }
  
  NavigableSet<TOP> getNavigableSet() { //used by FsIterator_sorted to compute various derivitive nav sets
    maybeProcessBulkAdds();
    return indexedFSs;
  }
   
  @Override
  public FSIterator<T> iterator() {
    maybeProcessBulkAdds();
    return new FsIterator_set_sorted<T>(this, getDetectIllegalIndexUpdates(), getTypeCode(), this);
  }
    
  private synchronized void maybeProcessBulkAdds() {
    final int sz = itemsToBeAdded.size();
    if (sz > 0) {
      
      // debug
  //    if (sz > 1) {
  //      TOP prev = itemsToBeAdded.get(0);
  //      for (int i = 1; i < sz; i++) {
  //        TOP next = itemsToBeAdded.get(i);
  //        if (comparator.compare(next,  prev) <= 0) {
  //          System.out.println("debug");
  //        }
  //        prev = next;
  //      }
  //    }
      indexedFSs.addAll(ss); 
      itemsToBeAdded.clear();
    }
  }
}
