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

package org.apache.uima.internal.util;

import java.util.NoSuchElementException;

/**
 * Int iterator in the Java style, but returning/using ints. Contrast with IntPointerIterator, which
 * is in the UIMA style allowing forward and backwards movement.
 */
public interface IntListIterator {

  /**
   * Check if there is a next element. Does not move the iterator.
   * 
   * @return <code>true</code> iff there is a next element.
   */
  boolean hasNext();

  /**
   * Return the next int in the list and increment the iterator.
   * 
   * @return The next int.
   * @exception NoSuchElementException
   *              If no next element exists, i.e., when the iterator points at the last position in
   *              the index.
   */
  default int next() throws NoSuchElementException {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return nextNvc();
  }

  /**
   * version of next() which bypasses the validity check. Only use this if you've already done this
   * check yourself.
   * 
   * @return the next int in the list and increment the iterator.
   */
  int nextNvc();

  /**
   * Check if there is a previous element. Does not move the iterator.
   * 
   * @return <code>true</code> iff there is a previous element.
   */
  boolean hasPrevious();

  /**
   * Return the previous int and decrement the iterator.
   * 
   * @return the previous int (found by first moving the iterator one backwards).
   * @exception NoSuchElementException
   *              If no previous element exists, i.e., when the iterator points at the first
   *              position in the index.
   */
  default int previous() throws NoSuchElementException {
    if (!hasPrevious()) {
      throw new NoSuchElementException();
    }
    return previousNvc();
  }

  /**
   * version of previous that bypasses the validity check. Only use this if you've already done this
   * check yourself.
   * 
   * @return the previous int (found by first moving the iterator one backwards).
   */
  int previousNvc();

  /**
   * Move the iterator to the start of the underlying index.
   */
  void moveToStart();

  /**
   * Move the iterator to the end of the underlying index.
   */
  void moveToEnd();

}
