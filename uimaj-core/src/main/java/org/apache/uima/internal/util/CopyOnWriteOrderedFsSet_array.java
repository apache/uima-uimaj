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

import java.util.Comparator;

import org.apache.uima.cas.impl.CopyOnWriteIndexPart;
import org.apache.uima.jcas.cas.TOP;

/**
 * implements OrderedFsSet_array partially, for iterator use
 */

public class CopyOnWriteOrderedFsSet_array implements CopyOnWriteIndexPart {
  
  private OrderedFsSet_array set;
  
  final public Comparator<TOP> comparatorWithoutID;
  final public Comparator<TOP> comparatorWithID;
  
  final public int a_firstUsedslot;
  final public int a_nextFreeslot;
  final public OrderedFsSet_array original;
  
  public TOP[] a;  // derived from "set" above
   
  public CopyOnWriteOrderedFsSet_array(OrderedFsSet_array original) {
    this.set = original;    
    this.original = original;
    this.comparatorWithoutID = original.comparatorWithoutID;
    this.comparatorWithID = original.comparatorWithID;
    this.a_firstUsedslot = original.a_firstUsedslot;
    this.a_nextFreeslot = original.a_nextFreeslot;
    this.a = original.a;
  }
  
  /**
   * Called by index when about to make an update
   * This copy captures the state of things before the update happens
   */
  @Override
  public void makeReadOnlyCopy() {
    this.set = new OrderedFsSet_array(set, true); // true = make read only copy
    this.a = set.a;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.CopyOnWriteIndexPart#isOriginal(java.lang.Object)
   */
  @Override
  public boolean isOriginal() {
    return set == original;
  }
  
  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return set.hashCode();
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    return set.equals(obj);
  }

  /**
   * @see OrderedFsSet_array#size()
   * @return the size of this version of the index (maybe not the current index size)
   */
  public int size() {
    return set.size();
  }

//  /**
//   * @return the modification count
//   */
//  public int getModificationCount() {
//    return set.getModificationCount();
//  }

  /**
   * @see OrderedFsSet_array#toString()
   */
  @Override
  public String toString() {
    return set.toString();
  }
    
  public OrderedFsSet_array getOfsa() {
    return set;
  }

}
