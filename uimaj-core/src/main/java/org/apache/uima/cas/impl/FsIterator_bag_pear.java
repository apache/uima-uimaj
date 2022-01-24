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

import org.apache.uima.cas.FeatureStructure;

/**
 * This version of the FsIterator is used while iterating within a PEAR Indexes keep references to
 * the base (possibly non-pear) version of FSs. During iteration, within PEARs, if there's a
 * different JCas class for the type, the corresponding class instance needs to be found (or
 * created) and returned.
 * 
 * @param <T>
 *          the type of FSs being returned from the iterator, supplied by the calling context
 */
class FsIterator_bag_pear<T extends FeatureStructure> extends FsIterator_bag<T> {

  FsIterator_bag_pear(FsIndex_bag<T> fsBagIndex, TypeImpl ti, CopyOnWriteIndexPart cow_wrapper) {
    super(fsBagIndex, ti, cow_wrapper);
  }

  @Override
  public T getNvc() {
    return CASImpl.pearConvert(super.getNvc());
  }

  @Override
  public FsIterator_bag_pear<T> copy() {
    FsIterator_bag_pear<T> copy = new FsIterator_bag_pear<>(this.fsBagIndex, this.ti, this.bag);
    copyCommonSetup(copy);
    return copy;
  }
}