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

import java.util.Comparator;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * Base class for FSIterator implementations. Defines the hasNext, next, and remove methods required
 * by java.util.Iterator.
 */
public abstract class FSIteratorImplBase<T extends FeatureStructure> implements FSIterator<T> {

  // Jira UIMA-464: add annotation comparator to be able to use Collections.binarySearch() on
  // annotation list.
//  protected static class AnnotationComparator<T extends FeatureStructure> implements Comparator<T> {
//
//    private AnnotationIndex<? extends AnnotationFS> index;  // used only to support compare
//
//    protected AnnotationComparator(AnnotationIndex<? extends FeatureStructure> index) {
//      super();
//      this.index = index;
//    }
//
//    public int compare(T fs1, T fs2) {
//      return this.index.compare(fs1, fs2);
//    }
//
//  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return isValid();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#next()
   */
  public T next() {
    T result = get();
    moveToNext();
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Iterator#remove()
   */
  public void remove() {
    throw new UnsupportedOperationException();
  }
  
  // methods to be overridden that can speed up this operation
  int getBegin() {
    return ((AnnotationFS)get()).getBegin();
  }
  
  int getEnd() {
    return ((AnnotationFS)get()).getEnd();
  }
  
  /**
   * A special version of moveTo for subtypes of AnnotationFS, which moves to a particular begin/end
   * (no type priority). 
   * @param begin the starting point (inclusive)
   * @param end the ending point (inclusive)
   */
  <TT extends AnnotationFS> void moveTo(int begin, int end) {
    throw new UnsupportedOperationException();
  }
}
