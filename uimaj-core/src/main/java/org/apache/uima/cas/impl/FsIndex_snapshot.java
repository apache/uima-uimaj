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

import java.util.AbstractCollection;
import java.util.Comparator;

import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.jcas.cas.TOP;

/**
 * Implementation of light-weight wrapper of normal indexes, which support special kinds of
 * iterators base on the setting of IteratorExtraFunction
 */
public class FsIndex_snapshot<T extends FeatureStructure> extends AbstractCollection<T>
        implements LowLevelIndex<T>, Comparator<FeatureStructure> {

  /**
   * wrapped index
   */
  private final FsIndex_iicp<T> wrapped;
  private final Comparator<TOP> comparatorWithoutId;
  private final Comparator<TOP> comparatorNoTypeWithoutId;

  public FsIndex_snapshot(FsIndex_iicp<T> wrapped, Comparator<TOP> comparatorWithoutId,
          Comparator<TOP> comparatorTypeWithoutId) {
    this.wrapped = wrapped;
    this.comparatorWithoutId = comparatorWithoutId;
    this.comparatorNoTypeWithoutId = comparatorTypeWithoutId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndex#getType()
   */
  @Override
  public Type getType() {
    return wrapped.getType();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndex#contains(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public boolean contains(FeatureStructure fs) {
    return wrapped.contains(fs);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndex#find(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public T find(FeatureStructure fs) {
    return wrapped.find(fs);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndex#iterator()
   */
  @Override
  public LowLevelIterator<T> iterator() {
    return iterator(IS_ORDERED, IS_TYPE_ORDER);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelIndex#iterator(boolean, boolean)
   */
  @Override
  public LowLevelIterator<T> iterator(boolean orderNotNeeded, boolean ignoreType) {
    Comparator<TOP> comparatorMaybeNoTypeWithoutID = ignoreType ? comparatorNoTypeWithoutId
            : comparatorWithoutId;
    return new FsIterator_subtypes_snapshot<>(new FsIndex_flat<>(wrapped),
            comparatorMaybeNoTypeWithoutID);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndex#getIndexingStrategy()
   */
  @Override
  public int getIndexingStrategy() {
    return wrapped.getIndexingStrategy();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndex#withSnapshotIterators() acts like a copy
   */
  @Override
  public FSIndex<T> withSnapshotIterators() {
    return new FsIndex_snapshot<>(wrapped, comparatorWithoutId, comparatorNoTypeWithoutId);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelIndex#size()
   */
  @Override
  public int size() {
    return wrapped.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(FeatureStructure o1, FeatureStructure o2) {
    return wrapped.compare(o1, o2);
  }

  @Override
  public LowLevelIterator<T> ll_iterator(boolean ambiguous) {
    LowLevelIterator<T> it = iterator(IS_ORDERED, IS_TYPE_ORDER);
    return ambiguous ? it : new LLUnambiguousIteratorImpl<>(it);
  }

  @Override
  public int ll_compare(int ref1, int ref2) {
    return wrapped.ll_compare(ref1, ref2);
  }

  @Override
  public CASImpl getCasImpl() {
    return wrapped.getCasImpl();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelIndex#getComparator()
   */
  @Override
  public Comparator<TOP> getComparator() {
    return wrapped.getComparator();
  }

  @Override
  public FSIndexComparator getComparatorForIndexSpecs() {
    return wrapped.getComparatorForIndexSpecs();
  }

  @Override
  public int ll_maxAnnotSpan() {
    return wrapped.ll_maxAnnotSpan();
  }

  @Override
  public boolean isSorted() {
    return wrapped.isSorted();
  }

}
