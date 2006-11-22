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
import java.util.Collections;
import java.util.NoSuchElementException;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * Subiterator implementation.
 * 
 * 
 */

public class Subiterator extends FSIteratorImplBase {

  private ArrayList list;

  private int pos;

  /**
   * 
   */
  private Subiterator() {
    super();
    this.list = new ArrayList();
    this.pos = 0;
  }

  /**
   * Create a disambiguated iterator.
   * 
   * @param it
   *          The iterator to be disambiguated.
   */
  Subiterator(FSIterator it) {
    this();
    it.moveToFirst();
    if (!it.isValid()) {
      return;
    }
    AnnotationFS current, next;
    current = (AnnotationFS) it.get();
    this.list.add(current);
    it.moveToNext();
    while (it.isValid()) {
      next = (AnnotationFS) it.get();
      if (current.getEnd() <= next.getBegin()) {
        current = next;
        this.list.add(current);
      }
      it.moveToNext();
    }
  }

  Subiterator(FSIterator it, AnnotationFS annot, final boolean ambiguous, final boolean strict) {
    this();
    if (ambiguous) {
      initAmbiguousSubiterator(it, annot, strict);
    } else {
      initUnambiguousSubiterator(it, annot, strict);
    }
  }

  private void initAmbiguousSubiterator(FSIterator it, AnnotationFS annot, final boolean strict) {
    final int start = annot.getBegin();
    final int end = annot.getEnd();
    it.moveTo(annot);
    if (it.isValid() && it.get().equals(annot)) {
      it.moveToNext();
    }
    // Skip annotations whose start is before the start parameter.
    while (it.isValid() && ((AnnotationFS) it.get()).getBegin() < start) {
      it.moveToNext();
    }
    AnnotationFS current;
    while (it.isValid()) {
      current = (AnnotationFS) it.get();
      // If the start of the current annotation is past the end parameter,
      // we're done.
      if (current.getBegin() > end) {
        break;
      }
      it.moveToNext();
      if (strict && current.getEnd() > end) {
        continue;
      }
      this.list.add(current);
    }
  }

  private void initUnambiguousSubiterator(FSIterator it, AnnotationFS annot, final boolean strict) {
    final int start = annot.getBegin();
    final int end = annot.getEnd();
    it.moveTo(annot);
    if (it.isValid() && it.get().equals(annot)) {
      it.moveToNext();
    }
    if (!it.isValid()) {
      return;
    }
    annot = (AnnotationFS) it.get();
    this.list = new ArrayList();
    // Skip annotations with begin positions before the given start
    // position.
    while (it.isValid() && ((start > annot.getBegin()) || (strict && annot.getEnd() > end))) {
      it.moveToNext();
    }
    // Add annotations.
    AnnotationFS current, next;
    if (!it.isValid()) {
      return;
    }
    current = (AnnotationFS) it.get();
    this.list.add(current);
    it.moveToNext();
    while (it.isValid()) {
      next = (AnnotationFS) it.get();
      // If the next annotation overlaps, skip it.
      if (next.getBegin() < current.getEnd()) {
        it.moveToNext();
        continue;
      }
      // If we're past the end, stop.
      if (next.getBegin() > end) {
        break;
      }
      // We have an annotation that's within the boundaries and doesn't
      // overlap
      // with the previous annotation. We add this annotation if we're not
      // strict, or the end position is within the limits.
      if (!strict || next.getEnd() <= end) {
        current = next;
        this.list.add(current);
      }
      it.moveToNext();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#isValid()
   */
  public boolean isValid() {
    return (this.pos >= 0) && (this.pos < this.list.size());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#get()
   */
  public FeatureStructure get() throws NoSuchElementException {
    if (isValid()) {
      return (FeatureStructure) this.list.get(this.pos);
    }
    throw new NoSuchElementException();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveToNext()
   */
  public void moveToNext() {
    ++this.pos;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveToPrevious()
   */
  public void moveToPrevious() {
    --this.pos;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveToFirst()
   */
  public void moveToFirst() {
    this.pos = 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveToLast()
   */
  public void moveToLast() {
    this.pos = this.list.size() - 1;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#moveTo(org.apache.uima.cas.FeatureStructure)
   */
  public void moveTo(FeatureStructure fs) {
    final int found = Collections.binarySearch(this.list, fs);
    if (found >= 0) {
      this.pos = found;
    } else {
      this.pos = (-found) - 1;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  public FSIterator copy() {
    Subiterator copy = new Subiterator();
    copy.list = this.list;
    copy.pos = this.pos;
    return copy;
  }

}
