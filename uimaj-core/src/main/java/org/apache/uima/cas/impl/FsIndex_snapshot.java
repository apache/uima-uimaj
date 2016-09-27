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

import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SelectFSs;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.cas.TOP;

/**
 * Implementation of light-weight wrapper of normal indexes, which support special kinds of iterators
 *   base on the setting of IteratorExtraFunction
 */
public class FsIndex_snapshot <T extends FeatureStructure> implements FSIndex<T> {
    
  /**
   * wrapped index 
   */
  private final FsIndex_iicp<T> wrapped;
  
  public FsIndex_snapshot(FsIndex_iicp<T> wrapped) {
    this.wrapped = wrapped;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIndex#getType()
   */
  @Override
  public Type getType() { return wrapped.getType(); }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIndex#contains(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public boolean contains(FeatureStructure fs) { return wrapped.contains(fs); }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIndex#find(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public T find(FeatureStructure fs) { return wrapped.find(fs); }
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIndex#iterator()
   */
  @Override
  public FSIterator<T> iterator() {
    return new FsIterator_subtypes_snapshot<T>(new FsIndex_flat<T>(wrapped));
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIndex#getIndexingStrategy()
   */
  @Override
  public int getIndexingStrategy() { return wrapped.getIndexingStrategy(); }
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.FSIndex#withSnapshotIterators()
   */
  @Override
  public FSIndex<T> withSnapshotIterators() {
    return new FsIndex_snapshot<T>(wrapped);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.LowLevelIndex#size()
   */
  @Override
  public int size() { return wrapped.size(); }

  
  /* (non-Javadoc)
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(FeatureStructure o1, FeatureStructure o2) { return wrapped.compare(o1,  o2); }

  /**
   * @return -
   * @see org.apache.uima.cas.impl.FsIndex_iicp#select()
   */
  public <N extends TOP> SelectFSs<N> select() {
    return wrapped.select();
  }

  /**
   * @param type -
   * @return -
   * @see org.apache.uima.cas.impl.FsIndex_iicp#select(org.apache.uima.cas.Type)
   */
  public <N extends TOP> SelectFSs<N> select(Type type) {
    return wrapped.select(type);
  }

  /**
   * @param clazz -
   * @return -
   * @see org.apache.uima.cas.impl.FsIndex_iicp#select(java.lang.Class)
   */
  public <N extends TOP> SelectFSs<N> select(Class<N> clazz) {
    return wrapped.select(clazz);
  }

  /**
   * @param jcasType -
   * @return -
   * @see org.apache.uima.cas.impl.FsIndex_iicp#select(int)
   */
  public <N extends TOP> SelectFSs<N> select(int jcasType) {
    return wrapped.select(jcasType);
  }

  /**
   * @param fullyQualifiedTypeName -
   * @return -
   * @see org.apache.uima.cas.impl.FsIndex_iicp#select(java.lang.String)
   */
  public <N extends TOP> SelectFSs<N> select(String fullyQualifiedTypeName) {
    return wrapped.select(fullyQualifiedTypeName);
  }

}
