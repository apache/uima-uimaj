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
import java.util.Comparator;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.cas.TOP;

// @formatter:off
/**
 * Common code for both
 *   aggregation of indexes (e.g. select, iterating over multiple views)
 * aggregation of indexes in type/subtype hierarchy
 *
 * Supports creating corresponding iterators just for the non-empty ones
 * Supports reinit - evaluating when one or more formerly empty indexes is no longer empty, and recalculating the 
 * iterator set
 * 
 * Supports move-to-leftmost when typeOrdering is to be ignored
 *   -- when no typeorder key
 *   -- when typeorder key, but select framework requests no typeordering for move to leftmost
 * 
 * @param <T>
 *          the highest type returned by these iterators
 */
// @formatter:on
public abstract class FsIterator_multiple_indexes<T extends FeatureStructure>
        implements LowLevelIterator<T> {

  // An array of iterators, one for each in the collection (e.g. subtypes, or views or ...)
  // split among empty and non-empty.
  final protected LowLevelIterator<T>[] allIterators;
  private LowLevelIterator<T>[] emptyIterators;
  protected LowLevelIterator<T>[] nonEmptyIterators;

  /**
   * for set and sorted, both ignore id because this comparator is not used for comparing within the
   * index, only for compares between index items and outside args. if ignoring type, uses that
   * style
   */
  final protected Comparator<TOP> comparatorMaybeNoTypeWithoutId;
  // final protected boolean ignoreType_moveToLeftmost;

  final protected LowLevelIndex<T> main_idx;

  // /** true if sorted index, with typepriority as a key, but ignoring it because
  // * either there are no type priorities defined, or
  // * using a select-API-created iterator configured without typePriority
  // *
  // * Not final for the use case where there's a type-order key, type priorities are specified,
  // * but a select-API-created iterator wants to ignore type priorities.
  // */
  // final protected boolean isSortedTypeOrder_but_IgnoringTypeOrder;

  public FsIterator_multiple_indexes(LowLevelIndex<T> main_idx, LowLevelIterator<T>[] iterators,
          // boolean ignoreType_moveToLeftmost) {
          Comparator<TOP> comparatorMaybeNoTypeWithoutId) {
    this.allIterators = iterators;
    this.main_idx = main_idx;
    this.comparatorMaybeNoTypeWithoutId = comparatorMaybeNoTypeWithoutId;
    separate_into_empty_indexes_and_non_empty_iterators();
  }

  /**
   * /** copy constructor
   * 
   * @param v
   *          the original to copy
   */
  public FsIterator_multiple_indexes(FsIterator_multiple_indexes<T> v) {
    allIterators = v.allIterators.clone();
    this.main_idx = v.main_idx;
    this.comparatorMaybeNoTypeWithoutId = v.comparatorMaybeNoTypeWithoutId;
    int i = 0;
    for (LowLevelIterator<T> it : allIterators) {
      allIterators[i++] = (LowLevelIterator<T>) it.copy();
    }
    separate_into_empty_indexes_and_non_empty_iterators();
  }

  /**
   * Also resets all non-empty iterators to current values
   */
  protected void separate_into_empty_indexes_and_non_empty_iterators() {

    ArrayList<LowLevelIterator<T>> emptyIteratorsAl = new ArrayList<>();
    ArrayList<LowLevelIterator<T>> nonEmptyIteratorsAl = new ArrayList<>();
    for (LowLevelIterator<T> it : allIterators) {
      LowLevelIndex<T> idx = it.ll_getIndex();
      if (idx == null || idx.size() == 0) {
        emptyIteratorsAl.add(it);
      } else {
        nonEmptyIteratorsAl.add(it);
      }
    }

    emptyIterators = emptyIteratorsAl.toArray(new LowLevelIterator[emptyIteratorsAl.size()]);
    nonEmptyIterators = nonEmptyIteratorsAl
            .toArray(new LowLevelIterator[nonEmptyIteratorsAl.size()]);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelIterator#ll_indexSize()
   */
  @Override
  public int ll_indexSizeMaybeNotCurrent() {
    int sz = 0;
    for (LowLevelIterator<T> it : nonEmptyIterators) {
      sz += it.ll_indexSizeMaybeNotCurrent();
    }
    return sz;
  }

  @Override
  public int ll_maxAnnotSpan() {
    int span = -1;
    for (LowLevelIterator<T> it : nonEmptyIterators) {
      int s = it.ll_maxAnnotSpan();
      if (s == Integer.MAX_VALUE) {
        return s;
      }
      if (s > span) {
        span = s;
      }
    }
    return (span == -1) ? Integer.MAX_VALUE : span;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelIterator#isIndexesHaveBeenUpdated()
   */
  @Override
  public boolean isIndexesHaveBeenUpdated() {
    for (LowLevelIterator<T> it : nonEmptyIterators) {
      if (it.isIndexesHaveBeenUpdated()) {
        return true;
      }
    }

    return empty_became_nonEmpty(); // slightly better than testing isIndexesHaveBeenUpdated
                                    // because if it went from empty -> not empty -> empty,
                                    // is not counted as having been updated for this purpose
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelIterator#maybeReinitIterator()
   */
  @Override
  public boolean maybeReinitIterator() {
    boolean empty_became_nonEmpty = empty_became_nonEmpty();
    if (empty_became_nonEmpty) {
      separate_into_empty_indexes_and_non_empty_iterators();
    }

    boolean any = false;
    for (LowLevelIterator<T> it : nonEmptyIterators) {
      any |= it.maybeReinitIterator(); // need to call on all, in order to reinit them if needed
    }
    return any;
  }

  private boolean empty_became_nonEmpty() {
    for (LowLevelIterator<T> it : emptyIterators) {
      if (it.ll_getIndex().size() > 0) { // don't test changed might have had insert then delete...
        return true;
      }
    }
    return false;
  }

  @Override
  public LowLevelIndex<T> ll_getIndex() {
    return (LowLevelIndex<T>) ((main_idx != null) ? main_idx
            : ((LowLevelIterator<T>) allIterators[0]).ll_getIndex());
  }

  @Override
  public int size() {
    int r = 0;
    for (LowLevelIterator<T> it : nonEmptyIterators) {
      r += it.size();
    }
    return r;
  }
}
