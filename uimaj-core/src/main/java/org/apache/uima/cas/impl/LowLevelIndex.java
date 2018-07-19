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

import java.util.Comparator;

import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SelectFSs;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.internal.util.IntPointerIterator;
import org.apache.uima.jcas.cas.TOP;

/**
 * Low-level FS index object. Use to obtain low-level iterators.
 * 
 */
public interface LowLevelIndex<T extends FeatureStructure> extends FSIndex<T> {

  /**
   * Get a low-level FS iterator.
   * 
   * @return An iterator for this index.
   */
  default LowLevelIterator<T> ll_iterator() {
    return ll_iterator(true);
  };

  /**
   * Get a low-level, FS reference iterator. This iterator can be disambiguated. This means that
   * only non-overlapping annotations will be returned. Non-annotation FSs will be filtered in this
   * mode.
   * 
   * @param ambiguous
   *          When set to <code>false</code>, iterator will be disambiguated.
   * @return An iterator for this index.
   */
  LowLevelIterator<T> ll_iterator(boolean ambiguous);
  
//  /**
//   * Get a low-level, FS reference iterator specifying instances of
//   * the precise type <b>only</b> (i.e. without listing the subtypes).
//   * 
//   * @return An iterator for the root type of this index.
//   */
//  LowLevelIterator<T> ll_rootIterator();

  /**
   * Compare two Feature structures, referred to by IDs
   * @param ref1 -
   * @param ref2 -
   * @return -
   */
  default int ll_compare(int ref1, int ref2) {
    CASImpl cas = getCasImpl();
    return compare(cas.getFsFromId_checked(ref1), cas.getFsFromId_checked(ref2));
  };
  
  /**
   * @return a CAS View associated with this iterator
   */
  CASImpl getCasImpl();
  
  // incorporated from FSIndexImpl
  /**
   * This is **NOT** a comparator for Feature Structures, but rather 
   * something that compares two comparator specifications
   * @return -
   */
  FSIndexComparator getComparatorForIndexSpecs();

  /**
   * 
   * @return a comparator used by this index to compare Feature Structures
   *   For sets, the equal is used to determine set membership
   *   For sorted, the comparator is the sort order (this comparator is without the ID)
   */
  Comparator<TOP> getComparator(); 
  
  static final Comparator<TOP> FS_ID_COMPARATOR = 
      (TOP fs1, TOP fs2) -> Integer.compare(fs1._id, fs2._id); 
  
  default void flush() {   // probably not needed, but left for backwards compatibility  4/2015
    throw new UnsupportedOperationException();
  }

  default IntPointerIterator getIntIterator() {   // probably not needed, but left for backwards compatibility 4/2015
    return new IntPointerIterator() {

      private LowLevelIterator<T> it = ll_iterator();
   
      @Override
      public boolean isValid() { return it.isValid(); }
      @Override
      public int get() { return it.ll_get(); }
      @Override
      public void inc() { it.moveToNext(); }
      @Override
      public void dec() { it.moveToPrevious(); }
      @Override
      public void moveTo(int i) { it.moveTo(i); }
      @Override
      public void moveToFirst() { it.moveToFirst(); }
      @Override
      public void moveToLast() { it.moveToLast(); }
      @Override
      public Object copy() { 
        IntPointerIterator newIt = getIntIterator();
        if (isValid()) {
          newIt.moveTo(it.ll_get());
        } else { // is invalid
          newIt.moveToFirst();
          newIt.dec(); // make invalid
        }
        return newIt;
      }
    };
  }
  
  /**
   * @param type which is a subtype of this index's type
   * @param <U> the type the subindex is over
   * @return the index but just over this subtype
   */
  default <U extends T> LowLevelIndex<U> getSubIndex(Type type) {
    TypeImpl ti = (TypeImpl) type;
    return getCasImpl().indexRepository.getIndexBySpec(ti.getCode(), getIndexingStrategy(), (FSIndexComparatorImpl) getComparatorForIndexSpecs());
  }
  
  default <U extends T> LowLevelIndex<U> getSubIndex(Class<? extends TOP> clazz) {
    return getSubIndex(this.getCasImpl().getCasType(clazz));
  }

  /**
   * @return for annotation indexes, an conservative estimate the maximum span between begin and end
   * The value may be larger than actual.
   */
  int ll_maxAnnotSpan();  
  
  /**
   * @return true if the index is sorted
   */
  boolean isSorted();
  
  @Override
  default SelectFSs<T> select() {
    return ((SelectFSs_impl<T>)getCasImpl().select()).index(this);
  }

  @Override
  default <N extends T> SelectFSs<N> select(Type type) {
    return ((SelectFSs_impl)select()).type(type); // need cast to impl because type() not in interface
  }

  @Override
  default <N extends T> SelectFSs<N> select(Class<N> clazz) {
    return ((SelectFSs_impl)select()).type(clazz);
  }

  @Override
  default <N extends T> SelectFSs<N> select(int jcasType) {
    return ((SelectFSs_impl)select()).type(jcasType);
  }

  @Override
  default <N extends T> SelectFSs<N> select(String fullyQualifiedTypeName) {
    return ((SelectFSs_impl)select()).type(fullyQualifiedTypeName);
  }
  
  /**
   * Return an iterator over the index. The position of the iterator will be set to 
   * return the first item in the index.
   * If the index is empty, the iterator position will be marked as invalid.
   * 
   * @return An FSIterator positioned at the beginning, or an invalid iterator.
   */
  default LowLevelIterator<T> iterator() {
    return iterator(IS_ORDERED, IS_TYPE_ORDER);
  }
  
  /**
   * Internal use, used by select framework.
   * 
   * Return an iterator over the index. The position of the iterator will be set to 
   * return the first item in the index.
   * If the index is empty, the iterator position will be marked as invalid.
   * 
   * @param orderNotNeeded if true, skips work while iterating to keep iterators over multiple types in sync.
   * @param ignoreType if true, the comparator used for moveTo leftmost operations 
   *        will ignore typeOrder keys, if the index happens to define these
   * 
   * @return An FSIterator positioned at the beginning, or an invalid iterator.
   */
  LowLevelIterator<T> iterator(boolean orderNotNeeded, boolean ignoreType);

  /**
   * Internal use constants
   */
  static final boolean IS_ORDERED = false;
  static final boolean IS_TYPE_ORDER = false;

}
