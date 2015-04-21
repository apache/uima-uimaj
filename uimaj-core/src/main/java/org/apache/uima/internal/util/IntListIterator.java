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
 * Int iterator in the Java style, but returning/using ints.
 * Contrast with IntPointerIterator, which is in the UIMA style allowing forward and backwards movement.
 */
public interface IntListIterator {

  /**
   * Check if there is a next element. Does not move the iterator.
   * 
   * @return <code>true</code> iff there is a next element.
   */
  boolean hasNext();

  /**
   * Return the next feature structure and increment the iterator.
   * 
   * @return The next feature structure.
   * @exception NoSuchElementException
   *              If no next element exists, i.e., when the iterator points at the last position in
   *              the index.
   */
  int next() throws NoSuchElementException;

  /**
   * Check if there is a previous element. Does not move the iterator.
   * 
   * @return <code>true</code> iff there is a previous element.
   */
  boolean hasPrevious();

  /**
   * Return the previous feature structure and decrement the iterator.
   * 
   * @return The previous feature structure.
   * @exception NoSuchElementException
   *              If no previous element exists, i.e., when the iterator points at the first
   *              position in the index.
   */
  int previous();

  /**
   * Move the iterator to the start of the underlying index.
   */
  void moveToStart();

  /**
   * Move the iterator to the end of the underlying index.
   */
  void moveToEnd();

}
