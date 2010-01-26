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

/**
 * Low-level FS index object. Use to obtain low-level iterators.
 * 
 */
public interface LowLevelIndex {

  /**
   * Get a low-level, FS reference iterator.
   * 
   * @return An iterator for this index.
   */
  LowLevelIterator ll_iterator();

  /**
   * Get a low-level, FS reference iterator. This iterator can be disambiguated. This means that
   * only non-overlapping annotations will be returned. Non-annotation FSs will be filtered in this
   * mode.
   * 
   * @param ambiguous
   *          When set to <code>false</code>, iterator will be disambiguated.
   * @return An iterator for this index.
   */
  LowLevelIterator ll_iterator(boolean ambiguous);
  
  /**
   * Get a low-level, FS reference iterator specifying instances of
   * the precise type <b>only</b> (i.e. without listing the subtypes).
   * 
   * @return An iterator for the root type of this index.
   */
  LowLevelIterator ll_rootIterator();

  /**
   * Get the number of FSs in this index.
   * 
   * @return The size of this index.
   */
  int size();

  int ll_compare(int ref1, int ref2);
}
