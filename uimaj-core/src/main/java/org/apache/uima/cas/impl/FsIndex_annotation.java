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
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.cas.text.AnnotationTree;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Implementation of annotation indexes.
 * Implements AnnotationIndex
 * replaces AnnotationIndexImpl in v2
 */
public class FsIndex_annotation <T extends AnnotationFS> 
                 extends FsIndex_iicp<T> 
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
  public FSIterator<T> iterator(boolean ambiguous) {
    if (ambiguous) {
      return iterator();
    }
    // return non-constrained, non-strict, unambiguous iterator
    boolean strict = false;  // https://issues.apache.org/jira/browse/UIMA-5063
    boolean isBounded = false;
    return new Subiterator<T>(iterator(), 
                              null, 
                              ambiguous, 
                              strict, 
                              isBounded,
                              true, // type priority used
                              true, // ignored
                              true, // ignored
                              this.getFsRepositoryImpl().getAnnotationFsComparator()
                             ); 
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
    return new Subiterator<T>(iterator(), 
        (Annotation) annot,
        ambiguous, 
        strict, 
        true,  // isBounded 
        true,  // uses type priority
        true,  // position uses type - ignored
        true,  // skip returning results equal to annot
        this.getFsRepositoryImpl().getAnnotationFsComparator()
        );
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
      annot = it.nextNvc();
      dtr = new AnnotationTreeNodeImpl<T>();
      dtr.set(annot);
      node.addChild(dtr);
      addChildren(dtr, subiterator(annot, false, true));
    }
  }

//  /* ***********************************
//   *  Support for withSnapshotIterators
//   *  using proxy
//   * ***********************************/
//  private final static Class<?>[] proxyInterface = new Class<?>[] {FsIndex_annotation.class};
//  
//  private class ProxySnapshotHandler implements InvocationHandler {
//    @Override
//    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//      if ("iterator".equals(method.getName())) {
//        if (args == null || args.length == 0) {
//          if (fsIndex_singletype.getIndexingStrategy() == FSIndex.SORTED_INDEX) {
//            return iterator(IteratorExtraFunction.SNAPSHOT);
//          }
//          return iterator(IteratorExtraFunction.UNORDERED_SNAPSHOT);
//        }
//        // iterator call with an arg boolean ambiguous
//        // TODO ambiguous/unambig with snapshot
//      }
//      
//      // pass thru all other methods
//      return method.invoke(args);
//    }    
//  }

  @Override
  public FSIndex<T> withSnapshotIterators() {
    return new FsIndex_snapshot<>(this);
  }
//  public FSIndex<T> withSnapshotIterators() {
//    return (FSIndex<T>) Proxy.newProxyInstance(
//        FSIndex.class.getClassLoader(),  // class loader 
//        proxyInterface,  
//        new ProxySnapshotHandler());
//  }
  
  /*********************************************************
   * Stream / SplitIterator support
   * 
   * Use:
   *   myAnnotIndex.annotStream()
   *      .reverse()
   *      .boundedBy(fs)
   *      .strict()
   *      .unambiguous()
   *      .noTypePriorities()
   *      .stream()  // or   .splititerator()
   *         ... stream operations;
   *         
   *********************************************************/
//  public AnnotStream<T> annotStream() {
//    return new AnnotStream<T>(this.fsIndex_singletype);
//  }
//  
//  public Stream<T extends TOP> stream() {
//    return StreamSupport.stream(, parallel)
//    
//  }
//  
}
