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

import java.util.NoSuchElementException;

/**
 * Low-level FS iterator. Returns FS references, instead of FS objects.
 * 
 * @see org.apache.uima.cas.FSIterator
 * 
 */
public interface LowLevelIterator {
  /**
   * Move iterator to first FS in index. A subsequent call to <code>isValid()</code> will succeed
   * iff the index is non-empty.
   */
  void moveToFirst();

  /**
   * Move iterator to last FS in index. A subsequent call to <code>isValid()</code> will succeed
   * iff the index is non-empty.
   */
  void moveToLast();

  /**
   * Check if the iterator is currently valid.
   * 
   * @return <code>true</code> iff the iterator is valid.
   */
  boolean isValid();

  /**
   * Return the current FS reference.
   * 
   * @return The current FS reference.
   * @exception NoSuchElementException
   *              Iff the iterator is not valid.
   */
  int ll_get() throws NoSuchElementException;

  /**
   * Advance the iterator. This may invalidate the iterator.
   */
  void moveToNext();

  /**
   * Move the iterator back one position. This may invalidate the iterator.
   */
  void moveToPrevious();

  /**
   * Try to position the iterator so that the current element is greater than or equal to
   * <code>fsRef</code>, and previous elements are less than <code>fsRef</code>. This may
   * invalidate the iterator. If fsRef can not be compared to FSs in the index, the results are
   * undefined.
   * 
   * @param fsRef
   *          The FS reference the iterator should be set to.
   */
  void moveTo(int fsRef);

  /**
   * Create a copy of this iterator. The copy will point at the same element that this iterator is
   * currently pointing at.
   * 
   * @return A copy of this iterator.
   */
  Object copy();

  /**
   * Return the size of the underlying index.
   * 
   * @return The size of the index.
   */
  int ll_indexSize();

  /**
   * Get the index for just the top most type of this iterator (excludes subtypes).
   * 
   * @return The index.
   */
  LowLevelIndex ll_getIndex();
}
