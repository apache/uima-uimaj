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

import java.util.NoSuchElementException;

import org.apache.uima.cas.CAS;

public class LLUnambiguousIteratorImpl implements LowLevelIterator {

  private final int[] annots;

  private int pos = 0;

  private final int size;

  // We only need those for copy.
  private final LowLevelIterator theIterator;

  private final LowLevelCAS theCas;

  public LLUnambiguousIteratorImpl(LowLevelIterator it, LowLevelCAS cas) {
    super();
    this.theCas = cas;
    this.theIterator = it;
    this.annots = new int[it.ll_indexSize()];
    final LowLevelTypeSystem ts = cas.ll_getTypeSystem();
    final int annotType = ts.ll_getCodeForTypeName(CAS.TYPE_NAME_ANNOTATION);
    final int startFeat = ts.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_BEGIN);
    final int endFeat = ts.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_END);

    int lastSeenEnd = 0;
    int curRef;
    int curType;
    it.moveToFirst();
    int i = 0;
    // Iterate over the input iterator.
    while (it.isValid()) {
      // Get current ref and its type.
      curRef = it.ll_get();
      curType = cas.ll_getFSRefType(curRef);
      if (ts.ll_subsumes(annotType, curType)) {
        // Found an annotation.
        if (i == 0 || (cas.ll_getIntValue(curRef, startFeat) >= lastSeenEnd)) {
          // Either first annotation, or non-overlapping continuation.
          this.annots[i] = curRef;
          lastSeenEnd = cas.ll_getIntValue(curRef, endFeat);
          ++i;
        }
      }
      it.moveToNext();
    }
    // The current value of i is the size of the index view provided by
    // this iterator.
    this.size = i;
  }

  public void moveToFirst() {
    this.pos = 0;
  }

  public void moveToLast() {
    this.pos = this.size - 1;
  }

  public boolean isValid() {
    return (this.pos >= 0) && (this.pos < this.size);
  }

  public int ll_get() throws NoSuchElementException {
    if (!this.isValid()) {
      throw new NoSuchElementException();
    }
    return this.annots[this.pos];
  }

  public void moveToNext() {
    ++this.pos;
  }

  public void moveToPrevious() {
    --this.pos;
  }

  public void moveTo(int fsRef) {
    final int position = binarySearch(this.annots, fsRef, 0, this.size);
    if (position >= 0) {
      this.pos = position;
    } else {
      this.pos = -(position + 1);
    }

  }

  public Object copy() {
    LLUnambiguousIteratorImpl copy = new LLUnambiguousIteratorImpl(this.theIterator, this.theCas);
    copy.pos = this.pos;
    return copy;
  }

  public int ll_indexSize() {
    return this.size;
  }

  public LowLevelIndex ll_getIndex() {
    return this.theIterator.ll_getIndex();
  }

  // Do binary search on index.
  private final int binarySearch(int[] array, int ele, int start, int end) {
    --end; // Make end a legal value.
    int i; // Current position
    int comp; // Compare value
    while (start <= end) {
      i = (int)(((long)start + end) / 2);
      comp = this.ll_getIndex().ll_compare(ele, array[i]);
      if (comp == 0) {
        return i;
      }
      if (start == end) {
        if (comp < 0) {
          return (-i) - 1;
        }
        // comp > 0
        return (-i) - 2; // (-(i+1))-1
      }
      if (comp < 0) {
        end = i - 1;
      } else { // comp > 0
        start = i + 1;
      }
    }
    // This means that the input span is empty.
    return (-start) - 1;
  }

}
