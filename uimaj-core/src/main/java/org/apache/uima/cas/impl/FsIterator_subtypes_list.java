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
import java.util.Arrays;
import java.util.function.Function;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.cas.TOP;

public abstract class FsIterator_subtypes_list <T extends TOP>  extends FsIterator_subtypes<T> {

  // An array of iterators, one for each subtype.
  //   This array has the indexes for all the subtypes that were non-empty at the time of iterator creation
  protected FsIterator_singletype<T>[] iterators;

  protected int lastValidIndex = 0;
   
  public FsIterator_subtypes_list(FsIndex_iicp<T> iicp) {
    super(iicp);
    this.iterators = initIterators();
//    can't do moveToFirst, subtypes haven't yet set up enough things (like comparator)
    // subtypes must do this call after setup complete
//    moveToFirst();  // needed in order to set up lastvalid index, etc.
  }
  
  // skip including iterators for empty indexes
  //   The concurrent modification exception notification doesn't occur when subsequent "adds" are done, but
  //   that is the same as current: 
  //   where the move to first would mark the iterator "invalid" (because it was initially empty) and it would be
  //     subsequently ignored - same effect
  private FsIterator_singletype<T>[] initIterators() {
    iicp.createIndexIteratorCache();
    final ArrayList<FsIndex_singletype<TOP>> cachedSubIndexes = iicp.cachedSubFsLeafIndexes;
    
    // map from single-type index to iterator over that single type
    Function<FsIndex_singletype<TOP>, FsIterator_singletype<T>> m = 
        fsIndex_singletype -> (FsIterator_singletype<T>)(fsIndex_singletype.iterator());
    
    FsIterator_singletype<T>[] r = cachedSubIndexes.stream()
        .filter(leafIndex -> leafIndex.size() > 0)  // filter out empty ones
        .map(m)  // map fsIndex_singletype to an iterator over that
        .toArray(FsIterator_singletype[]::new);

    // if all are empty, put the first one in (to avoid handling 0 as a special case)
    return (r.length != 0) ? r : new FsIterator_singletype[] {m.apply(cachedSubIndexes.get(0))};
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#ll_indexSize()
   */
  @Override
  public int ll_indexSize() {
    return Arrays.stream(iterators).mapToInt(it -> it.ll_getIndex().size()).sum();
  }
  
}
