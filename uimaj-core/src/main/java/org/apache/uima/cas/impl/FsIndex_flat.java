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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.internal.util.IntVector;

/**
 * Common part of flattened indexes, used for both snapshot iterators and 
 * flattened sorted indexes
 *  
 * @param <T> the Java class type for this index
 */
public class FsIndex_flat<T extends FeatureStructure> extends FsIndex_singletype<T> {

  // The index, an array.
  final private FeatureStructureImplC[] indexedFSs;
  
  final private FsIndex_iicp<T> iicp;
  
  final private Comparator<FeatureStructure> comparator;
    
  FsIndex_flat(FsIndex_iicp<T> iicp) {
    super(iicp.getCASImpl(), iicp.fsIndex_singletype.getType(), iicp.fsIndex_singletype.getIndexingStrategy());
    this.iicp = iicp;
    indexedFSs = fillFlatArray();
    comparator = iicp.fsIndex_singletype;
  }  
  
  /**
   * Flat array filled, ordered
   * @param flatArray the array to fill
   */
  private FeatureStructureImplC[] fillFlatArray() {
    
    FeatureStructureImplC[] a =  (FeatureStructureImplC[]) Array.newInstance(FeatureStructureImplC.class, iicp.size());
    
    FSIterator<T> it = iicp.iterator();
    int i = 0;
    while (it.hasNext()) {
      a[i++] = (FeatureStructureImplC) it.next();
    }
    
    if (i != a.length) {
//      System.out.println("Debug - got iterator invalid before all items filled, i = " + i + ", size = " + flatArray.length);
      throw new ConcurrentModificationException();
    }
    return a;
  }
  
  FeatureStructureImplC[] getFlatArray() {
    return indexedFSs;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIndex#iterator()
   */
  @Override
  public FSIterator<T> iterator() {
    return new FsIterator_subtypes_snapshot<T>(this);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.FsIndex_singletype#insert(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  boolean insert(T fs) {
    throw new UnsupportedOperationException();  
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.FsIndex_singletype#contains(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public boolean contains(FeatureStructure fs) {
    return find(fs) != null;
  }
  
  @Override
  public boolean containsEq(FeatureStructureImplC fs) {
    return findEq((T) fs) != null;
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIndex#find(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public T find(FeatureStructure fs) {
    if (isSorted()) {
      for (FeatureStructureImplC item : indexedFSs) {
        if (comparator.compare(item,  fs) == 0) {
          return (T) item;
        }
      }
      return null;
    }

    // ordered case
    // r is index if found, otherwise, (-(insertion point) - 1). 
    int r = Arrays.binarySearch(indexedFSs,  fs, comparator);
    return (r >= 0) ? (T) indexedFSs[r] : null;
  }

  public T findEq(T fs) {
    
    if (isSorted()) {
      Arrays.binarySearch((T[]) indexedFSs, 0, indexedFSs.length, fs, (T fs1, T fs2) -> fs1 == fs2 ? 0 : -1);
    } else {
      for (FeatureStructureImplC item : indexedFSs) {
        if (fs == item) {
          return (T) item;
        }
      }
      return null;
    }

    // ordered case
    // r is index if found, otherwise, (-(insertion point) - 1). 
    int r = Arrays.binarySearch(indexedFSs, (FeatureStructureImplC) fs, (FeatureStructureImplC f1, FeatureStructureImplC f2) -> Integer.compare(f1.id(), f2.id()));
    return (r == 0) ? fs : null;    
  }

  /**
   * @see org.apache.uima.cas.FSIndex#size()
   */
  public int size() {
    return this.indexedFSs.length;
  }

  /**
   * @see org.apache.uima.cas.impl.FsIndex_singletype#deleteFS(T)
   */
  public boolean deleteFS(T fs) {
    throw new UnsupportedOperationException();
  }  
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.FsIndex_singletype#bulkAddTo(java.util.List)
   */
  @Override
  protected void bulkAddTo(List<FeatureStructure> v) {
    v.addAll(Arrays.asList(indexedFSs));
  }
  
  @Override
  protected void bulkAddTo(IntVector v) {
    Arrays.stream(indexedFSs).mapToInt(FeatureStructureImplC::id()).forEach(v::add);
  }
  
  /**
   * @see org.apache.uima.cas.FSIndex#compare(T, T)
   */    
  @Override
  public int compare(FeatureStructure fs1, FeatureStructure fs2) {
    return comparator.compare(fs1,  fs2);
  }


}
