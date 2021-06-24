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
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.cas.TOP;

// @formatter:off
/**
 * Aggregate several FS iterators.  Simply iterates over one after the other
 * without any sorting or merging.
 * Used by getAllIndexedFS and FsIterator_subtypes when unordered
 *   underlying iterators could be any (bag, set, or ordered)
 *   underlying iterators could be complex (unambiguous annotation, filtered,...)
 * 
 * The iterators can be for single types or for types with subtypes. Exception: if the ll_index is
 * accessed, it is presumed to be of type FsIndex_subtypes.
 */
// @formatter:on
class FsIterator_aggregation_common<T extends FeatureStructure>
        extends FsIterator_multiple_indexes<T> {

  private final static AtomicInteger moveTo_error_msg_count = new AtomicInteger(0);

  // /** only used for moveTo */
  // final private boolean ignoreTypePriority;

  /** the index of the current iterator */
  private int current_it_idx = -1;

  FsIterator_aggregation_common(LowLevelIterator<T>[] iterators, FSIndex<T> index,
          Comparator<TOP> comparatorMaybeNoTypeWithoutId) {
    super((LowLevelIndex<T>) index, iterators, comparatorMaybeNoTypeWithoutId);
    moveToFirstNoReinit();
  }

  /** copy constructor */
  FsIterator_aggregation_common(FsIterator_aggregation_common<T> v) {
    super(v);
    current_it_idx = v.current_it_idx;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelIterator#moveToSupported()
   */
  @Override
  public boolean isMoveToSupported() {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#isValid()
   */
  @Override
  public boolean isValid() {
    return current_it_idx >= 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#getNvc()
   */
  @Override
  public T getNvc() throws NoSuchElementException {
    return nonEmptyIterators[current_it_idx].getNvc();
  }

  /**
   * MoveTo for this kind of iterator Happens for set or sorted indexes being operated without
   * rattling, or for other kinds of aggregation.
   * 
   * The meaning for set is to go to the position if it exists of the 1 element equal (using the
   * index's comparator) the arg. But since the set is unordered, there's no point in doing this.
   * 
   * The meaning for unordered other kinds: They're not really unordered, just the aggregate is
   * unordered. A use would be to partially restart iteration from some point. But since this is
   * unordered, there's not much point in doing this
   * 
   */
  @Override
  public void moveToNoReinit(FeatureStructure fs) {
    Misc.decreasingWithTrace(moveTo_error_msg_count,
            "MoveTo operations on unsorted iterators are likely mistakes.",
            UIMAFramework.getLogger());

    // Type typeCompare = fs.getType();

    int i = -1;
    int validNonMatch = -1;
    for (LowLevelIterator<T> it : nonEmptyIterators) {

      i++;

      // Type itType = (TypeImpl) it.getType();
      // if ( ! typeCompare.subsumes(itType) ) {
      // continue;
      // }

      it.moveToNoReinit(fs);
      if (it.isValid() && 0 == it.ll_getIndex().compare(fs, it.getNvc())) {
        current_it_idx = i;
        return; // perfect match
      }
      if (validNonMatch == -1 && it.isValid()) {
        validNonMatch = i; // capture first valid non-Match
      }
    }

    // nothing matched using iterator compare
    if (validNonMatch >= 0) {
      current_it_idx = validNonMatch;
      return;
    }
    // mark iterator invalid otherwise
    current_it_idx = -1;
  }

  // /**
  // * MoveToExact for this kind of iterator
  // */
  // public void moveToExactNoReinit(FeatureStructure fs) {
  // Type typeCompare = fs.getType();
  // int i = 0;
  // for (LowLevelIterator<T> it : nonEmptyIterators) {
  // if (it.getType() == typeCompare) {
  // it.moveToExactNoReinit(fs);
  // current_it_idx = it.isValid() ? i : -1;
  // return;
  // }
  // i++;
  // }
  // current_it_idx = -1;
  // }

  /** moves to the first non-empty iterator at its start position */
  @Override
  public void moveToFirstNoReinit() {
    current_it_idx = -1; // no valid index
    for (LowLevelIterator<T> it : nonEmptyIterators) {
      current_it_idx++;
      it.moveToFirstNoReinit();
      if (it.isValid()) {
        return;
      }
    }
  }

  @Override
  public void moveToLastNoReinit() {
    for (int i = nonEmptyIterators.length - 1; i >= 0; i--) {
      LowLevelIterator<T> it = nonEmptyIterators[i];
      it.moveToLastNoReinit();
      if (it.isValid()) {
        current_it_idx = i;
        return;
      }
    }
    current_it_idx = -1; // no valid index
  }

  @Override
  public void moveToNextNvc() {
    FSIterator<T> it = nonEmptyIterators[current_it_idx];
    it.moveToNextNvc();

    if (it.isValid()) {
      return;
    }

    final int nbrIt = nonEmptyIterators.length;
    for (int i = current_it_idx + 1; i < nbrIt; i++) {
      it = nonEmptyIterators[i];
      it.moveToFirst();
      if (it.isValid()) {
        current_it_idx = i;
        return;
      }
    }
    current_it_idx = -1; // invalid position
  }

  @Override
  public void moveToPreviousNvc() {

    LowLevelIterator<T> it = nonEmptyIterators[current_it_idx];
    it.moveToPreviousNvc();

    if (it.isValid()) {
      return;
    }

    for (int i = current_it_idx - 1; i >= 0; i--) {
      it = nonEmptyIterators[i];
      it.moveToLastNoReinit();
      if (it.isValid()) {
        current_it_idx = i;
        return;
      }
    }
    current_it_idx = -1; // invalid position
  }

  @Override
  public int ll_indexSizeMaybeNotCurrent() {
    int sum = 0;
    for (int i = nonEmptyIterators.length - 1; i >= 0; i--) {
      FSIterator<T> it = nonEmptyIterators[i];
      sum += ((LowLevelIterator<T>) it).ll_indexSizeMaybeNotCurrent();
    }
    return sum;
  }

  @Override
  public int ll_maxAnnotSpan() {
    throw Misc.internalError(); // should never be called, because this operation isn't useful
                                // in unordered indexes
    // int span = -1;
    // for (int i = nonEmptyIterators.length - 1; i >= 0; i--) {
    // FSIterator<T> it = nonEmptyIterators[i];
    // int x = ((LowLevelIterator<T>)it).ll_maxAnnotSpan();
    // if (x > span) {
    // span = x;
    // }
    // }
    // return (span == -1) ? Integer.MAX_VALUE : span;
  };

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  @Override
  public FsIterator_aggregation_common<T> copy() {
    return new FsIterator_aggregation_common<>(this);
  }

  @Override
  public String toString() {
    // Type type = this.ll_getIndex().getType();
    StringBuilder sb = new StringBuilder(this.getClass().getSimpleName()).append(":")
            .append(System.identityHashCode(this));

    if (nonEmptyIterators.length == 0) {
      sb.append(" empty iterator");
      return sb.toString();
    }

    sb.append((main_idx == null && nonEmptyIterators.length > 1) ? " over multiple Types: "
            : " over type: ");

    if (main_idx == null) {
      if (nonEmptyIterators.length > 1) {
        sb.append('[');
        for (FSIterator<T> it : nonEmptyIterators) {
          Type type = ((LowLevelIterator<T>) it).ll_getIndex().getType();
          sb.append(type.getName()).append(':').append(((TypeImpl) type).getCode()).append(' ');
        }
        sb.append(']');
      } else {
        Type type = ((LowLevelIterator<T>) nonEmptyIterators[0]).ll_getIndex().getType();
        sb.append(type.getName()).append(':').append(((TypeImpl) type).getCode()).append(' ');
      }
    } else {
      Type type = main_idx.getType();
      sb.append(type.getName()).append(':').append(((TypeImpl) type).getCode()).append(' ');
    }

    sb.append(", iterator size (may not match current index size): ")
            .append(this.ll_indexSizeMaybeNotCurrent());
    return sb.toString();
  }

  @Override
  public Comparator<TOP> getComparator() {
    return null; // This style is unordered
  }

}
