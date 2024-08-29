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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.cas.TOP;

/**
 * Common part of flattened indexes, used for both snapshot iterators and flattened sorted indexes
 * 
 * built from passed in instance of FsIndex_iicp
 * 
 * @param <T>
 *          the Java class type for this index
 */
public class FsIndex_flat<T extends FeatureStructure> extends FsIndex_singletype<T> {

  // The index, an array.
  private final TOP[] indexedFSs;

  private final FsIndex_iicp<T> iicp;

  private final Comparator<TOP> comparatorWithoutId;

  private final int maxAnnotSpan;

  FsIndex_flat(FsIndex_iicp<T> iicp) {
    super(iicp.getCasImpl(), iicp.fsIndex_singletype.getType(),
            iicp.fsIndex_singletype.getIndexingStrategy(),
            iicp.fsIndex_singletype.getComparatorImplForIndexSpecs());
    this.iicp = iicp;
    indexedFSs = fillFlatArray();
    comparatorWithoutId = iicp.fsIndex_singletype.comparatorWithoutID;
    maxAnnotSpan = iicp.ll_maxAnnotSpan();
  }

  /**
   * Flat array filled, ordered
   * 
   * @param flatArray
   *          the array to fill
   */
  private TOP[] fillFlatArray() {

    TOP[] a = (TOP[]) Array.newInstance(TOP.class, iicp.size());

    FSIterator<T> it = iicp.iterator();
    int i = 0;
    while (it.hasNext()) {
      a[i++] = (TOP) it.nextNvc();
    }

    if (i != a.length) {
      // System.out.println("Debug - got iterator invalid before all items filled, i = " + i + ",
      // size = " + flatArray.length);
      throw new ConcurrentModificationException();
    }
    return a;
  }

  FeatureStructure[] getFlatArray() {
    return indexedFSs;
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
   * @see org.apache.uima.cas.impl.FsIndex_singletype#iterator(boolean, boolean)
   */
  @Override
  public LowLevelIterator<T> iterator(boolean orderNotNeeded, boolean ignoreType) {
    FsIndex_singletype<T> idx = iicp.getFsIndex_singleType();
    Comparator<TOP> comp = orderNotNeeded ? null
            : ignoreType ? idx.comparatorNoTypeWithoutID : idx.comparatorWithoutID;
    return new FsIterator_subtypes_snapshot<>(this, comp);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.FsIndex_singletype#insert(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  void insert(T fs) {
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.FsIndex_singletype#contains(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public boolean contains(FeatureStructure fs) {
    return find(fs) != null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndex#find(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public T find(FeatureStructure fs) {
    if (isSorted()) {
      for (TOP item : indexedFSs) {
        if (comparatorWithoutId.compare(item, (TOP) fs) == 0) {
          return (T) item;
        }
      }
      return null;
    }

    // ordered case
    // r is index if found, otherwise, (-(insertion point) - 1).
    int r = Arrays.binarySearch(indexedFSs, (TOP) fs, comparatorWithoutId);
    return (r >= 0) ? (T) indexedFSs[r] : null;
  }

  // public T findEq(T fs) {
  //
  // if (isSorted()) {
  // Arrays.binarySearch((T[]) indexedFSs,
  // 0,
  // indexedFSs.length,
  // fs,
  // comparator augmented by id;
  // } else {
  // for (FeatureStructure item : indexedFSs) {
  // if (fs == item) {
  // return (T) item;
  // }
  // }
  // return null;
  // }
  //
  // // ordered case
  // // r is index if found, otherwise, (-(insertion point) - 1).
  // int r = Arrays.binarySearch(indexedFSs, fs, (f1, f2) -> Integer.compare(f1._id(), f2._id()));
  // return (r == 0) ? fs : null;
  // }

  /**
   * @see org.apache.uima.cas.FSIndex#size()
   */
  @Override
  public int size() {
    return indexedFSs.length;
  }

  /**
   * @see org.apache.uima.cas.impl.FsIndex_singletype#deleteFS(FeatureStructure)
   */
  @Override
  public boolean deleteFS(T fs) {
    throw new UnsupportedOperationException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.FsIndex_singletype#bulkAddTo(java.util.List)
   */
  @Override
  protected void bulkAddTo(List<T> v) {
    v.addAll((Collection<? extends T>) Arrays.asList(indexedFSs));
  }

  // maybe needed for backwards compatibility for now
  protected void bulkAddTo(IntVector v) {
    Arrays.stream(indexedFSs).mapToInt(FeatureStructure::_id).forEach(v::add);
  }

  /**
   * @see org.apache.uima.cas.FSIndex#compare(FeatureStructure, FeatureStructure)
   */
  @Override
  public int compare(FeatureStructure fs1, FeatureStructure fs2) {
    return comparatorWithoutId.compare((TOP) fs1, (TOP) fs2);
  }

  @Override
  protected CopyOnWriteIndexPart createCopyOnWriteIndexPart() {
    Misc.internalError(); // should never be called
    return null; // no copy on write index part implemented for this kind
  }

  // /* (non-Javadoc)
  // * @see org.apache.uima.cas.impl.CopyOnWriteIndexPart#makeCopy()
  // */
  // @Override
  // public void makeReadOnlyCopy() {
  // Misc.internalError(); // should never be called
  // }

  @Override
  public int ll_maxAnnotSpan() {
    return maxAnnotSpan;
  }

}
