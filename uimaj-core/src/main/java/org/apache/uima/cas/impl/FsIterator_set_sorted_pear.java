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

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.cas.TOP;

/**
 * @param <T>
 *          the type of FSs being returned from the iterator, supplied by the calling context
 */
class FsIterator_set_sorted_pear<T extends FeatureStructure> extends FsIterator_set_sorted2<T> {

  FsIterator_set_sorted_pear(FsIndex_set_sorted<T> ll_index, CopyOnWriteIndexPart cow_wrapper,
          Comparator<TOP> comparatorMaybeNoTypeWithoutID) {
    super(ll_index, cow_wrapper, comparatorMaybeNoTypeWithoutID);
  }

  // FsIterator_set_sorted_pear createInstance(OrderedFsSet_array orderedFsSet_array, LowLevelIndex
  // ll_index) {
  // orderedFsSet_array.new LL_Iterator(ll_index);
  // }

  @Override
  public T getNvc() {
    return CASImpl.pearConvert(super.getNvc());
  }

  @Override
  public FsIterator_set_sorted_pear<T> copy() {
    FsIterator_set_sorted_pear<T> r = new FsIterator_set_sorted_pear<>(ll_index, ofsa,
            comparatorMaybeNoTypeWithoutID);
    copyCommonSetup(r);
    return r;
  }
}