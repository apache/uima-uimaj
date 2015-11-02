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

import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.internal.util.IntVector;

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
public class FsIndex_set_sorted<T extends FeatureStructure> extends FsIndex_singletype<T> {

  // The index, a NavigableSet. 
  // Should be over T, but has to be over FeatureStructure in order to have the comparator take FeatureStructures
  final private NavigableSet<FeatureStructure> indexedFSs;
   
  FsIndex_set_sorted(CASImpl cas, Type type, int indexType, boolean useSorted) {
    super(cas, type, indexType);
    this.indexedFSs = useSorted 
                        ?  new TreeSet<FeatureStructure>(
                            (FeatureStructure o1, FeatureStructure o2) -> {
                              final int c = compare(o1,  o2); 
                              // augment normal comparator with one that compares IDs if everything else equal
                              return (c == 0) ? (Integer.compare(o1.id(), o2.id())) : c;})
                        : new TreeSet<FeatureStructure>( (FeatureStructure o1, FeatureStructure o2) -> compare(o1,  o2));     
  }

  @Override
  public void flush() {
    this.indexedFSs.clear();
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
    return CASImpl.isSameCAS(casImpl, fs.getCAS()) && indexedFSs.contains(fs);
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.FsIndex_singletype#insert(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  boolean insert(T fs) {
    return this.indexedFSs.add(fs);
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
    if (null == templateKey || this.indexedFSs.size() == 0) {
      return null;
    }
    T found;
    FeatureStructure fs1GEfs = this.indexedFSs.ceiling(templateKey);
    
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

  public T findLeftmost(FeatureStructure templateKey) {
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
    return this.indexedFSs.size();
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
    return this.indexedFSs.remove(fs);
  }
  
  @Override
  protected void bulkAddTo(List<FeatureStructure> v) {
    v.addAll(indexedFSs);
  }
  
  @Override
  protected void bulkAddTo(IntVector v) {
    this.indexedFSs.stream().mapToInt(fs -> ((FeatureStructureImplC)fs).id()).forEach(v::add);
  }
  
  NavigableSet<FeatureStructure> getNavigableSet() { //used by FsIterator_sorted to compute various derivitive nav sets
    return indexedFSs;
  }
   
  @Override
  public FSIterator<T> iterator() {
    return new FsIterator_set_sorted<T>(this, getDetectIllegalIndexUpdates(), getTypeCode(), this);
  }
}
