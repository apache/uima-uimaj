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

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.FSIntIteratorImplBase;

/**
 * Interface for a comparable IntPointerIterator. 
 * 
 * This allows two iterators to be compared with each other.  Two IntIterators are compared according to the
 * element that would be returned by the next call to next().
 * 
 * The purpose of this is for keeping multiple iterators, one for each subtype of a type, in a sorted order,
 * when desiring to iterate over a type and its subtypes, in a combined merged order.
 * 
 * This is only needed for iterators over Sorted indexes. Iterators over Bags and Sets have no ordering requirement.
 * 
 * (Note, however, that backwards compatibility may require this)
 * 
 * All ComparableIntPointerIterators must implement the ConcurrentModificationException mechanism, so that interface is included
 * 
 */
public interface ComparableIntPointerIterator<F extends FeatureStructure> extends IntPointerIterator, Comparable<FSIntIteratorImplBase<F>> {

//  public void checkConcurrentModification();
//
//  public void resetConcurrentModification();
  
}
