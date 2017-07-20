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
import java.util.NoSuchElementException;

import org.apache.uima.cas.FeatureStructure;

/**
 * Common code for both
 *   aggregation of indexes (e.g. select, iterating over multiple views)
 *   aggregation of indexes in type/subtype hierarchy
 *
 * Supports creating corresponding iterators just for the non-empty ones
 * Supports reinit - evaluating when one or more formerly empty indexes is no longer empty, and recalculating the 
 *                   iterator set
 * @param <T> the highest type returned by these iterators
 */
public abstract class FsIterator_multiple_indexes <T extends FeatureStructure>  implements LowLevelIterator<T> {

  // An array of iterators, one for each in the collection (e.g. subtypes, or views or ...)
  // split among empty and non-empty.
  protected LowLevelIterator<T>[] allIterators;
  private LowLevelIterator<T>[] emptyIterators;
  protected LowLevelIterator<T> [] nonEmptyIterators;

  /** index into nonEmptyIterators, shows last valid one */
  protected int lastValidIteratorIndex = -1;
   
  public FsIterator_multiple_indexes(LowLevelIterator<T>[] iterators) {
    this.allIterators = iterators;
    separate_into_empty_indexes_and_non_empty_iterators();
    
  }
  
  /** copy constructor */
  public FsIterator_multiple_indexes(FsIterator_multiple_indexes<T> v) {
    allIterators = v.allIterators.clone();
    int i = 0;
    for (LowLevelIterator<T> it : allIterators) {
      allIterators[i++] = (LowLevelIterator<T>) it.copy();
    }   
    separate_into_empty_indexes_and_non_empty_iterators();
    lastValidIteratorIndex = v.lastValidIteratorIndex;
  }
  
  /**
   * Also resets all non-empty iterators to current values
   */
  protected void separate_into_empty_indexes_and_non_empty_iterators() {
        
    ArrayList<LowLevelIterator<T>> emptyIteratorsAl = new ArrayList<>();
    ArrayList<LowLevelIterator<T>> nonEmptyIteratorsAl = new ArrayList<>();
    
    for (LowLevelIterator<T> it : allIterators) {
      if (it.ll_indexSize() == 0) {
        emptyIteratorsAl.add(it);  
      } else {
        nonEmptyIteratorsAl.add(it); 
      }
    }
    
    emptyIterators    = emptyIteratorsAl   .toArray(new LowLevelIterator[emptyIteratorsAl   .size()]);
    nonEmptyIterators = nonEmptyIteratorsAl.toArray(new LowLevelIterator[nonEmptyIteratorsAl.size()]);
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#isValid()
   */
  @Override
  public boolean isValid() {
    return lastValidIteratorIndex >= 0 &&
    lastValidIteratorIndex < nonEmptyIterators.length &&
    nonEmptyIterators[lastValidIteratorIndex].isValid();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIterator#getNvc()
   */
  @Override
  public T getNvc() {
    return nonEmptyIterators[lastValidIteratorIndex].getNvc();
  }

  
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#ll_indexSize()
   */
  @Override
  public int ll_indexSize() {
    int sz = 0;
    for (LowLevelIterator<T> it : nonEmptyIterators) {
      sz += it.ll_indexSize();      
    }
    return sz;
  }
  
  @Override
  public int ll_maxAnnotSpan() {
    int span = -1;
    for (LowLevelIterator<T> it : nonEmptyIterators) {
      int s = it.ll_maxAnnotSpan();
      if (s == Integer.MAX_VALUE) {
        return s;
      }
      if (s > span) {
        span = s;
      }
    }
    return (span == -1) ? Integer.MAX_VALUE : span;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#isIndexesHaveBeenUpdated()
   */
  @Override
  public boolean isIndexesHaveBeenUpdated() {
    for (LowLevelIterator<T> it : nonEmptyIterators) {
      if (it.isIndexesHaveBeenUpdated()) {
        return true;
      }
    } 
    
    return empty_became_nonEmpty();  // slightly better than testing isIndexesHaveBeenUpdated
                                     // because if it went from empty -> not empty -> empty, 
                                     // is not counted as having been updated for this purpose
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIterator#maybeReinitIterator()
   */
  @Override
  public boolean maybeReinitIterator() {
    boolean empty_became_nonEmpty = empty_became_nonEmpty();
    if (empty_became_nonEmpty) {
      separate_into_empty_indexes_and_non_empty_iterators();
    }
    
    boolean any = false;
    for (LowLevelIterator<T> it : nonEmptyIterators) {
      any |= it.maybeReinitIterator();  // need to call on all, in order to reinit them if needed
    }
    return any;
  }

  
  private boolean empty_became_nonEmpty() {
    for (LowLevelIterator<T> it : emptyIterators) {
      if (it.ll_getIndex().size() > 0) {  // don't test changed  might have had insert then delete...
        return true;
      }
    }
    return false;
  }
  
}
