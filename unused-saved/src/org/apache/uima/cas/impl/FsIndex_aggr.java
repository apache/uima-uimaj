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
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.jcas.cas.TOP;

/**
 * FsIndex_aggr supports iterators over various aggregations of other iterators involving multiple indexes
 *   examples: iterate over all views
 *             iterate over all FSs in a view
 *             iterate over an index + all of its sub-indexes (iicp)
 * 
 * compareTo() is delegatedbased on types and the comparator of the index.
 * 
 * T is the Java cover class of the top type (root) in the index set
 * 
 * Also includes a lazily initialized reference to a corresponding FSIndexFlat instance.
 * 
 * This class is package private to share with FSIndexFlat
 * For Internal Use
 */  
class FsIndex_aggr<T extends FeatureStructure> 
          implements Comparator<FeatureStructure>,
                     LowLevelIndex<T> {

//  private final static boolean DEBUG = false;

//  final FSIndexRepositoryImpl fsIndexRepositoryImpl;
  
  /**
   * A list of indexes that make up the aggregate 
   * 
   * This is set up lazily on first need, to avoid extra work when won't be accessed
   */
  final private LowLevelIndex<T>[] indexes;
      
  FsIndex_aggr(LowLevelIndex<T>[] indexes) {
    this.indexes = indexes;    
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(this.getClass().getSimpleName()).append(", indexes=");
    for (FSIndex idx : indexes) {
      sb.append("\n   ").append(idx.toString());
    }
    return sb.toString();
  }
  
  /**
   * Two iicps are equal if and only if:
   *   - the types they index are the same, and
   *   - the comparators are equal, and
   *   - the indexing stragtegy (bag/set/sorted) are the same
   * Used when creating the index iterator cache to select from the
   *   set of all instances of these the one that goes with the same index definition
   * Used by CasComplete serialization to merge multiple index names referring to the same index  
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof FsIndex_aggr)) {
      return false;
    }
    final FsIndex_aggr<?> idx = (FsIndex_aggr<?>) o;
    return Arrays.equals(indexes, idx.indexes);
  }

// Implement a hashCode; 
// previously tried just throwing an exception, but if the hashCode method throws, 
//   then the Eclipse debugger fails to show the object saying 
//   com.sun.jdi.InvocationException occurred invoking method. 
  @Override
  public int hashCode() {
    return Arrays.hashCode(indexes);
  }

  
  /**
   * 
   * @return the sum of the sizes of the indexes of the type + all subtypes
   */
  @Override
  public int size() {
    int size = 0;
    for (LowLevelIndex<T> idx : indexes) {
      size += idx.size();
    }
    return size;
  }
  
  @Override
  public int ll_maxAnnotSpan() {
    int span = -1;
    for (LowLevelIndex<T> idx : indexes) {
      span = Math.max(span,  idx.ll_maxAnnotSpan());
    }
    return span;
  }
  
  public boolean isEmpty() {
    for (LowLevelIndex<T> idx : indexes) {
      if (idx.size() > 0) {
        return false;
      }
    } 
    return true;
  }
  
  /**
   * A faster version of size() when there are lots indexes
   * 
   * Guess by adding the sizes of up to the first 3 type/subtypes, 
   * then add 1 more for each subtype in addition.
   * 
   * @return a guess at the size, done quickly
   */
  int guessedSize() {
    final int len = indexes.length;
    final int lim = Math.min(3, len);
    int size = 0;
    for (int i = 0; i < lim; i++) {
      size += indexes[i].size();
    }
    size += len - lim;
    return size;
  }
     
  @Override
  public int getIndexingStrategy() {
    throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIndex#getComparator()
   */
  @Override
  public Comparator<TOP> getComparator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public FSIndexComparator getComparatorForIndexSpecs() {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public int compare(FeatureStructure fs1, FeatureStructure fs2) {
    throw new UnsupportedOperationException();
  }
    
  @Override
  public boolean contains(FeatureStructure fs) {
    return find(fs) != null;
  }

  @Override
  public T find(FeatureStructure fs) {    
    for (LowLevelIndex<T> idx : indexes) {
     T result = idx.find(fs);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public Type getType() {
    throw new UnsupportedOperationException();
  }
  
  int getTypeCode() {
    throw new UnsupportedOperationException();
  }

  @Override
  public CASImpl getCasImpl() {
    return indexes[0].getCasImpl();
  }
  
  @Override
  public LowLevelIterator<T> iterator() {  
    throw new UnsupportedOperationException();
  } 
  
  public LowLevelIterator<T> iteratorUnordered() {
    throw new UnsupportedOperationException();
  }

  @Override
  public LowLevelIterator<T> ll_iterator(boolean ambiguous) {
    throw new UnsupportedOperationException();
  }
     
  @Override
  public FSIndex<T> withSnapshotIterators() {
    throw new UnsupportedOperationException();
  }

  /**
   * @return a stream of FSIndex_singletype, for all non-empty indexes
   */
  public Stream<LowLevelIndex<T>> streamNonEmptyIndexes() {
    return Arrays.stream(indexes).filter(idx -> idx.size() > 0);
  }
    
}
