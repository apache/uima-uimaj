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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.internal.util.CopyOnWriteOrderedFsSet_array;
import org.apache.uima.jcas.cas.TOP;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FsIterator_set_sorted_pearTest {

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  void thatCopyRetainsPosition() throws Exception {
    FsIndex_set_sorted<FeatureStructure> ll_index = Mockito.mock(FsIndex_set_sorted.class);
    CopyOnWriteIndexPart cow_wrapper = Mockito.mock(CopyOnWriteOrderedFsSet_array.class);
    Comparator<TOP> comparatorMaybeNoTypeWithoutID = Mockito.mock(Comparator.class);

    FsIterator_set_sorted_pear<FeatureStructure> sut = Mockito.spy(new FsIterator_set_sorted_pear<>(
            ll_index, cow_wrapper, comparatorMaybeNoTypeWithoutID));
    sut.pos = 1;
    FsIterator_set_sorted_pear<FeatureStructure> sutCopy = sut.copy();
    assertThat(sutCopy.pos).isEqualTo(sut.pos);
  }
}
