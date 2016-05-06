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
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.internal.util.OrderedFsSet_array;
import org.apache.uima.jcas.cas.TOP;

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
public class FsIndex_set_sorted<T extends FeatureStructure> extends FsIndex_singletype<T> {
  
//  /**
//   * This impl of sorted set interface allows using the bulk add operation implemented in Java's 
//   * TreeSet - that tests if the argument being passed in is an instance of SortedSet and does a fast insert.
//   */
//  final private SortedSet<T> ss = new SortedSet<T>() {
//    
//    @Override
//    public int size() { return itemsToBeAdded.size(); }
//    @Override
//    public boolean isEmpty() { return false; }
//    @Override
//    public boolean contains(Object o) { throw new UnsupportedOperationException(); }
//    @Override
//    public Iterator<T> iterator() { return itemsToBeAdded.iterator(); }
//    @Override
//    public Object[] toArray() { throw new UnsupportedOperationException(); }
//    @Override
//    public <U> U[] toArray(U[] a) { throw new UnsupportedOperationException(); }
//    @Override
//    public boolean add(T e) { throw new UnsupportedOperationException(); }
//    @Override
//    public boolean remove(Object o) { throw new UnsupportedOperationException(); }
//    @Override
//    public boolean containsAll(Collection<?> c) { throw new UnsupportedOperationException(); }
//    @Override
//    public boolean addAll(Collection<? extends T> c) { throw new UnsupportedOperationException(); }
//    @Override
//    public boolean retainAll(Collection<?> c) { throw new UnsupportedOperationException(); }
//    @Override
//    public boolean removeAll(Collection<?> c) { throw new UnsupportedOperationException(); }
//    @Override
//    public void clear() { throw new UnsupportedOperationException(); }
//    @Override
//    public Comparator<? super T> comparator() { return (Comparator<? super T>) comparatorWithID; }
//    @Override
//    public SortedSet<T> subSet(FeatureStructure fromElement, FeatureStructure toElement) { throw new UnsupportedOperationException(); }
//    @Override
//    public SortedSet<T> headSet(FeatureStructure toElement) { throw new UnsupportedOperationException(); }
//    @Override
//    public SortedSet<T> tailSet(FeatureStructure fromElement) { throw new UnsupportedOperationException(); }
//    @Override
//    public T first() { throw new UnsupportedOperationException(); }
//    @Override
//    public T last() { throw new UnsupportedOperationException(); }  
//    
//    
//  };


  // The index, a NavigableSet. 
//  final private TreeSet<FeatureStructure> indexedFSs;
//  final private TreeSet<FeatureStructure> indexedFSs;
    final private OrderedFsSet_array indexedFSs;
    
  final private Comparator<TOP> comparatorWithID;
  final private Comparator<TOP> comparatorWithoutID;
  
//  final private Comparator<Object> comparatorO;
  
  // batching of adds
//  final private ArrayList<T> itemsToBeAdded = new ArrayList<>();  // to batch the adds
//  final private MethodHandle getItemsToBeAddedArray = Misc.getProtectedFieldGetter(ArrayList.class, "elementData");
//
//  private long lastTrimToSizeTime = System.nanoTime();
  
//  private T largestItemNotYetAdded = null;
   
  FsIndex_set_sorted(CASImpl cas, Type type, int indexType, FSIndexComparator comparatorForIndexSpecs, boolean useSorted) {
    super(cas, type, indexType, comparatorForIndexSpecs);
    FSIndexRepositoryImpl ir = this.casImpl.indexRepository;
    
    if (ir.isAnnotationIndex(comparatorForIndexSpecs, indexType)) {
      comparatorWithID = ir.getAnnotationFsComparatorWithId(); 
      comparatorWithoutID = ir.getAnnotationFsComparator();
    } else {
      comparatorWithoutID = (o1, o2) -> compare(o1,  o2);
      comparatorWithID = useSorted   
          ? (o1, o2) -> {
              final int c = compare(o1,  o2); 
              // augment normal comparator with one that compares IDs if everything else equal
              return (c == 0) ? (Integer.compare(o1.id(), o2.id())) : c;} 
          : comparatorWithoutID;
    }          
//    comparatorO = new Comparator() {
//
//      @Override
//      public int compare(Object o1, Object o2) {
//        return comparator.compare((FeatureStructure)o1, (FeatureStructure)o2);
//      }
//      
//    };

//    this.indexedFSs = new TreeSet<FeatureStructure>(comparator);
//    Comparator<TOP> c = new Comparator<TOP>() {
//
//      @Override
//      public int compare(TOP o1, TOP o2) {
//        return comparatorWithID.compare(o1, o2);
//      }      
//    };
    
    this.indexedFSs = new OrderedFsSet_array(comparatorWithID, comparatorWithoutID);
  }

  @Override
  public void flush() {
    this.indexedFSs.clear();
//    this.itemsToBeAdded.clear();
//    this.largestItemNotYetAdded = null;
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
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.FsIndex_singletype#insert(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  void insert(T fs) {
    
//    /**
//     * The implementation tries for speedup for the initial loading of the treeset
//     *   Taking advantage of the bulk addAll optimization implemented in TreeSet when the 
//     *      items being added are sorted, and the tree set is initially empty
//     */
//    // measured - not efficient.
//    if (indexedFSs.size() > 1024) {
//      // optimize for insert at end
//      TOP last = indexedFSs.last();
//      if (indexedFSs.comparator().compare(last, fs) <= 0) {
//        // insert at end fast path maybe
//        return indexedFSs.tailSet(last, true).add(fs);
//      }
//    }
//    if (indexedFSs.isEmpty()) {
//      if (largestItemNotYetAdded == null ||
//          comparatorWithID.compare((TOP)fs,  (TOP)largestItemNotYetAdded) > 0) {
//        // batch the add
//        itemsToBeAdded.add(fs);
//        largestItemNotYetAdded = fs;
//        return;
//      }       
////      maybeProcessBulkAdds();
//    }
//    
    // past the initial load, or item is not > previous largest item to be added 
    
    indexedFSs.add((TOP)fs);
//    // batch this add 
//    largestItemNotYetAdded = fs;
//    itemsToBeAdded.add(fs);
//    return true;
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
  public T find(FeatureStructure templateKey) {
//    maybeProcessBulkAdds();
    if (null == templateKey || this.indexedFSs.isEmpty()) {
      return null;
    }
    TOP found;
    TOP templateKeyTop = (TOP) templateKey;
    TOP fs1GEfs = this.indexedFSs.ceiling(templateKeyTop);
    
    if (fs1GEfs == null) {  // then all elements are less-that the templateKey
      found = indexedFSs.lower(templateKeyTop);  //highest of elements less-than the template key
      return (found == null) 
               ? null 
               : (comparatorWithoutID.compare(found, templateKeyTop) == 0) 
                   ? (T)found 
                   : null;
    }
    
    // fs1GEfs is the least element that is greater-than-or-equal to the template key, using the fine-grained comparator
    if (0 == comparatorWithoutID.compare(fs1GEfs, templateKeyTop)) {
      return (T) fs1GEfs; 
    }
    
    // fs1GEfs not null, GreaterThan the templateKey using comparatorWithoutID
    // Therefore, the ones preceding it are LE using comparatorWithoutID
    found = indexedFSs.lower(templateKeyTop);  // the greatest element in this set strictly less than the templateKey
    return (found == null) 
              ? null 
              : (comparatorWithoutID.compare(found, templateKeyTop) == 0) 
                   ? (T)found 
                   : null;
  }

  public T findLeftmost(TOP templateKey) {
//    maybeProcessBulkAdds();
    // descending iterator over elements LessThan templateKey
    // iterator is over TOP, not T, to make compare easier
    Iterator<TOP> it = indexedFSs.headSet(templateKey, false).descendingIterator();
  
    TOP elementBefore = null;
    TOP lastEqual = null;
    // move to left until run out or have element not equal using compareWihtoutID to templateKey
    while (it.hasNext()) {
      if (0 != comparatorWithoutID.compare(elementBefore = it.next(), templateKey)) {
        break;
      }
      lastEqual = elementBefore;
    }
  
    if (!it.hasNext()) { // moved past beginning
      return (T) elementBefore;  // might return null to indicate not found
    }
    return (T) lastEqual;
  }

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
//    maybeProcessBulkAdds();
    return this.indexedFSs.remove(fs);
  }
  
  @Override
  protected void bulkAddTo(List<T> v) {
//    maybeProcessBulkAdds();
    v.addAll((Collection<? extends T>) indexedFSs);
  }
  
//  @Override
//  protected void bulkAddTo(IntVector v) {
//    this.indexedFSs.stream().mapToInt(fs -> ((FeatureStructureImplC)fs).id()).forEach(v::add);
//  }
  
  NavigableSet<T> getNavigableSet() { //used by FsIterator_set_sorted to compute various derivitive nav sets
//    maybeProcessBulkAdds();
    return (NavigableSet<T>) indexedFSs;
  }
   
  @Override
  public FSIterator<T> iterator() {
//    maybeProcessBulkAdds();
    return new FsIterator_set_sorted<T>(this, type, this);
  }
    
//  synchronized void maybeProcessBulkAdds() {
//    final int sz = itemsToBeAdded.size();
//    if (sz > 0) {
//      
//      // debug
//  //    if (sz > 1) {
//  //      TOP prev = itemsToBeAdded.get(0);
//  //      for (int i = 1; i < sz; i++) {
//  //        TOP next = itemsToBeAdded.get(i);
//  //        if (comparator.compare(next,  prev) <= 0) {
//  //          System.out.println("debug");
//  //        }
//  //        prev = next;
//  //      }
//  //    }
////      Object[] tba;
////      try {
////        tba =  (Object[])getItemsToBeAddedArray.invokeExact(itemsToBeAdded);
////      } catch (Throwable e) {
////        Misc.internalError(e);  // always throws
////        return;  // only to get rid of compile issue
////      }
////      if (sz > 100) {
////        Arrays.sort(tba, 0, sz, comparatorO);
////      } else {
////        Arrays.sort(tba, 0, sz, comparatorO);
////      }
////      
////      FeatureStructure prev = null;
////      indexedFSs.addAll(ss);
//      for (FeatureStructure fs : itemsToBeAdded) {
////        if (fs != prev) { // the itemsToBeAdded may contain duplicates
//          indexedFSs.add((TOP)fs);   
////          prev = fs;
////        }
//      }
//      itemsToBeAdded.clear();
//      itemsToBeAdded.trimToSize();
//    }
//  }  
}
