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
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.cas.text.AnnotationTree;

/**
 * Implementation of annotation indexes.
 * A wrapper not an extend, because 
 *   the constructor argument index has 
 *     customization for additional fns and
 *     a ref to it's enclosing instance of the FSIndexRepositoryImpl.
 */
public class AnnotationIndexImpl <T extends AnnotationFS> implements AnnotationIndex<T> {

  private FSIndex<T> index;
  
  public AnnotationIndexImpl(FSIndex<T> index) {
    super();
    this.index = index;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndex#size()
   */
  @Override
  public int size() {
    return this.index.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndex#getType()
   */
  @Override
  public Type getType() {
    return this.index.getType();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndex#contains(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public boolean contains(FeatureStructure fs) {
    return this.index.contains(fs);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndex#find(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public FeatureStructure find(FeatureStructure fs) {
    return this.index.find(fs);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndex#compare(org.apache.uima.cas.FeatureStructure,
   *      org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public int compare(FeatureStructure fs1, FeatureStructure fs2) {
    return this.index.compare(fs1, fs2);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndex#iterator()
   */
  @Override
  public FSIterator<T> iterator() {
    return (FSIterator<T>) this.index.iterator();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndex#iterator(org.apache.uima.cas.FeatureStructure)
   */
  @Override
  public FSIterator<T> iterator(FeatureStructure fs) {
    return (FSIterator<T>) this.index.iterator(fs);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIndex#getIndexingStrategy()
   */
  @Override
  public int getIndexingStrategy() {
    return this.index.getIndexingStrategy();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.text.AnnotationIndex#iterator(boolean)
   */
  @Override
  public FSIterator<T> iterator(boolean ambiguous) {
    if (ambiguous) {
      return (FSIterator<T>) this.index.iterator();
    }
    // return non-constrained, non-strict, unambiguous iterator
    boolean strict = false;
    boolean isBounded = false;
    return new Subiterator<T>(this.index.iterator(), null, 0, 0, ambiguous, strict, isBounded, ((FSIndexRepositoryImpl.IndexImpl<T>)(this.index)).getFsRepositoryImpl());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.text.AnnotationIndex#subiterator(org.apache.uima.cas.text.AnnotationFS)
   */
  @Override
  public FSIterator<T> subiterator(AnnotationFS annot) {
    return subiterator(annot, true, true);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.text.AnnotationIndex#subiterator(org.apache.uima.cas.text.AnnotationFS,
   *      boolean, boolean)
   */
  @Override
  public FSIterator<T> subiterator(AnnotationFS annot, boolean ambiguous, boolean strict) {
    boolean isBounded = true;
    return new Subiterator<T>(this.index.iterator(), annot, 0, 0, ambiguous, strict, isBounded, ((FSIndexRepositoryImpl.IndexImpl<T>)(this.index)).getFsRepositoryImpl());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.text.AnnotationIndex#tree(org.apache.uima.cas.text.AnnotationFS)
   */
  @Override
  public AnnotationTree<T> tree(T annot) {
    AnnotationTreeImpl<T> tree = new AnnotationTreeImpl<T>();
    AnnotationTreeNodeImpl<T> root = new AnnotationTreeNodeImpl<T>();
    tree.setRoot(root);
    root.set(annot);
    addChildren(root, subiterator(annot, false, true));
    return tree;
  }

  private void addChildren(AnnotationTreeNodeImpl<T> node, FSIterator<T> it) {
    AnnotationTreeNodeImpl<T> dtr;
    T annot;
    while (it.isValid()) {
      annot = it.get();
      it.moveToNext();
      dtr = new AnnotationTreeNodeImpl<T>();
      dtr.set(annot);
      node.addChild(dtr);
      addChildren(dtr, subiterator(annot, false, true));
    }
  }

  @Override
  public FSIndex<T> withSnapshotIterators() {
    return new AnnotationIndexImpl<T>(index.withSnapshotIterators());
  }
  
}
