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

import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SelectFSs;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.internal.util.IntPointerIterator;

/**
 * Low-level FS index object. Use to obtain low-level iterators.
 * 
 */
public interface LowLevelIndex<T extends FeatureStructure> extends FSIndex<T> {

  /**
   * Get a low-level, FS reference iterator.
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

  int ll_compare(int ref1, int ref2);
  
  CASImpl getCasImpl();
  
  // incorporated from FSIndexImpl
  
  FSIndexComparator getComparatorForIndexSpecs();

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
   * @param ti type which is a subtype of this index's type
   * @return the index but just over this subtype
   */
  default <U extends T> LowLevelIndex<U> getSubIndex(TypeImpl ti) {
    return getCasImpl().indexRepository.getIndexBySpec(ti.getCode(), getIndexingStrategy(), (FSIndexComparatorImpl) getComparatorForIndexSpecs());
  }

  /**
   * @return for annotation indexes, an conservative estimate the maximum span between begin and end
   * The value may be larger than actual.
   */
  int ll_maxAnnotSpan();  
  
  
  @Override
  default SelectFSs<T> select() {
    return getCasImpl().select().index(this);
  }

  @Override
  default <N extends FeatureStructure> SelectFSs<N> select(Type type) {
    return select().type(type);
  }

  @Override
  default <N extends FeatureStructure> SelectFSs<N> select(Class<N> clazz) {
    return select().type(clazz);
  }

  @Override
  default <N extends FeatureStructure> SelectFSs<N> select(int jcasType) {
    return select().type(jcasType);
  }

  @Override
  default <N extends FeatureStructure> SelectFSs<N> select(String fullyQualifiedTypeName) {
    return select().type(fullyQualifiedTypeName);
  }
  
}
