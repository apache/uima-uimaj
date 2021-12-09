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
import org.apache.uima.cas.impl.Subiterator.BoundsUse;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.cas.text.AnnotationTree;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Implementation of annotation indexes. Implements AnnotationIndex replaces AnnotationIndexImpl in
 * v2
 */
public class FsIndex_annotation<T extends AnnotationFS> extends FsIndex_iicp<T>
        implements AnnotationIndex<T> {

  public FsIndex_annotation(FsIndex_singletype<T> fsIndex_singletype) {
    super(fsIndex_singletype);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.text.AnnotationIndex#iterator(boolean)
   */
  @Override
  public LowLevelIterator<T> iterator(boolean ambiguous) {
    if (ambiguous) {
      return iterator();
    }
    // return non-constrained, non-strict, unambiguous iterator
    boolean strict = false; // https://issues.apache.org/jira/browse/UIMA-5063
    return new Subiterator<>(iterator(), null, // no bounding fs
            null, // no bounding fs
            ambiguous, strict, null, // no BoundsUse
            true, // type priority used
            true, // ignored
            true, // isStrictIncludesAnnotationsStartingAtEndPosition
            true, // isIncludeZeroWidthAtBegin
            true // isIncludeZeroWidthAtEnd
    );
  }

  /**
   * @param ambiguous
   *          false for unambiguous
   * @param strict
   *          true for strict
   * @param orderNotNeeded
   *          true for unordered
   * @param ignoreType
   *          -
   * @return -
   */
  public LowLevelIterator<T> iterator(boolean ambiguous, boolean strict, boolean orderNotNeeded,
          boolean ignoreType) {
    if (ambiguous) {
      return iterator(orderNotNeeded, ignoreType);
    }
    // return non-constrained, non-strict, unambiguous iterator
    return new Subiterator<>(iterator(orderNotNeeded, ignoreType), null, // no bounding fs
            null, // no bounding fs
            ambiguous, strict, null, // no BoundsUse
            !ignoreType, // type priority used
            true, // ignored - only for coveredBy or covering,
            true, // isStrictIncludesAnnotationsStartingAtEndPosition
            true, // isIncludeZeroWidthAtBegin
            true // isIncludeZeroWidthAtEnd
    );
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.cas.text.AnnotationIndex#subiterator(org.apache.uima.cas.text.AnnotationFS)
   */
  @Override
  public FSIterator<T> subiterator(AnnotationFS annot) {
    return subiterator(annot, true, true);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.cas.text.AnnotationIndex#subiterator(org.apache.uima.cas.text.AnnotationFS,
   * boolean, boolean)
   */
  @Override
  public FSIterator<T> subiterator(AnnotationFS annot, boolean ambiguous, boolean strict) {
    return new Subiterator<>(iterator(), (Annotation) annot, null, // no secondary bounding fs
            ambiguous, strict, BoundsUse.coveredBy, // isBounded
            true, // uses type priority
            true, // skip returning results equal to annot
            true, // isStrictIncludesAnnotationsStartingAtEndPosition
            true, // isIncludeZeroWidthAtBegin
            true // isIncludeZeroWidthAtEnd
    );
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.text.AnnotationIndex#tree(org.apache.uima.cas.text.AnnotationFS)
   */
  @Override
  public AnnotationTree<T> tree(T annot) {
    AnnotationTreeImpl<T> tree = new AnnotationTreeImpl<>();
    AnnotationTreeNodeImpl<T> root = new AnnotationTreeNodeImpl<>();
    tree.setRoot(root);
    root.set(annot);
    addChildren(root, subiterator(annot, false, true));
    return tree;
  }

  private void addChildren(AnnotationTreeNodeImpl<T> node, FSIterator<T> it) {
    AnnotationTreeNodeImpl<T> dtr;
    T annot;
    while (it.isValid()) {
      annot = it.nextNvc();
      dtr = new AnnotationTreeNodeImpl<>();
      dtr.set(annot);
      node.addChild(dtr);
      addChildren(dtr, subiterator(annot, false, true));
    }
  }

  @Override
  public FSIndex<T> withSnapshotIterators() {
    FsIndex_singletype<T> idx = getFsIndex_singleType();
    return new FsIndex_snapshot<>(this, idx.comparatorWithoutID, idx.comparatorNoTypeWithoutID);
  }

}
