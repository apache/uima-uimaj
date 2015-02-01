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

public interface PositiveIntSet {

  /**
   * remove all members of the set
   */
  void clear();

  /**
   * @param key -
   * @return true if key is in the set
   */
  boolean contains(int key);

  /**
   * 
   * @param key -
   * @return true if this set did not already contain the specified element
   */
  boolean add(int key);

  /**
   * add all elements in this set to the IntVector v as a bulk operation
   * @param v - to be added to
   */
  void bulkAddTo(IntVector v);
  /**
   * 
   * @param key -
   * @return true if the set had this element before the remove
   */
  boolean remove(int key);

  /**
   * @return number of elements in the set
   */
  int size();
  
  /**
   * @return the set as an arbitrarily ordered int array
   */
  int[] toIntArray();
  /**
   * @return an iterator (may be ordered or unordered) over the members of the set
   */
  IntListIterator iterator();
  
  /**
   * @param element an item which may be in the set
   * @return -1 if the item is not in the set, or a position value that can be used with iterators to start at that item.
   */
  int find(int element);

  /**
   * For FSBagIndex low level iterator use
   *   DOESN"T WORK WITH INCREMENTING position VALUES
   * @param position - get the element at this position.  This is for iterator use only, and is not related to any key
   * @return the element
   */
  int get(int position);
  
  /**
   * For FSBagIndex low level iterator use
   * @return the position of the first element, or -1;
   */
  int moveToFirst();
  
  /**
   * For FSBagIndex low level iterator use
   * @return the position of the last element, or -1;
   */
  int moveToLast();
  
  /**
   * For FSBagIndex low level iterator use
   * @param position -
   * @return the position of the next element, or -1;
   */
  int moveToNext(int position);
  
  /**
   * For FSBagIndex low level iterator use
   * @param position -
   * @return the position of the next element, or -1;
   */
  int moveToPrevious(int position);

  /**
   * For FSBagIndex low level iterator use
   * @param position -
   * @return true if the position is between the first and last element inclusive.
   */
  boolean isValid(int position);
}