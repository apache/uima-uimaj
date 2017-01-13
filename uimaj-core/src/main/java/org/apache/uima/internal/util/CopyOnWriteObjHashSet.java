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

import java.util.Iterator;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CopyOnWriteIndexPart;
import org.apache.uima.internal.util.ObjHashSet;

/**
 * implements ObjHashSet partially, for iterator use
 */

public class CopyOnWriteObjHashSet<T> implements CopyOnWriteIndexPart {
  
  private ObjHashSet<T> ohs;
  
  
  public CopyOnWriteObjHashSet(ObjHashSet<T> original) {
    this.ohs = original;    
  }

  /**
   * Called by index when about to make an update
   */
  @Override
  public void makeReadOnlyCopy() {
    ohs = new ObjHashSet<>(ohs, true); // true - read-only copy
  }
  
  /*****************************************************
   * These methods to make this class easily usable by *
   * FsIterator_bag                                    *
   *****************************************************/
  
  /**
   * @param obj the object to find in the table (if it is there)
   * @return the position of obj in the table, or -1 if not in the table
   */
  public int find(T obj) {
    return ohs.find(obj);
  }
  
//  @Override
//  public int size() {
//    
//  }
  
  /**
   * For iterator use
   * @param index a magic number returned by the internal find
   * @return the T at that spot, or null if nothing there 
   */
  public T get(int index) {
    return ohs.get(index);
  }
  
  /**
   * advance pos until it points to a non 0 or is 1 past end
   * @param pos -
   * @return updated pos
   */
  public int moveToNextFilled(int pos) {
    return ohs.moveToNextFilled(pos);
  }

  /**
   * decrement pos until it points to a non 0 or is -1
   * @param pos -
   * @return updated pos
   */
  public int moveToPreviousFilled(int pos) {
    return ohs.moveToPreviousFilled(pos);
  }
  
  public Iterator<T> iterator() {
    return ohs.iterator();
  }

  /**
   * if the fs is in the set, the iterator should return it.
   * if not, return -1 (makes iterator invalid)
   * @param fs position to this fs
   * @return the index if present, otherwise -1;
   */
  public int moveTo(FeatureStructure fs) {
    return ohs.moveTo(fs);
  }
 
  @Override
  public String toString() {
    return ohs.toString();
  }

  /**
   * @see ObjHashSet#getModificationCount()
   * @return the modification count
   */
  public int getModificationCount() {
    return ohs.getModificationCount();
  }

  /**
   * @see ObjHashSet#getCapacity()
   * @return the capacity &gt;= size
   */
  public int getCapacity() {
    return ohs.getCapacity();
  }

  /**
   * @see ObjHashSet#size()
   * @return the size
   */
  public int size() {
    return ohs.size();
  }

}
