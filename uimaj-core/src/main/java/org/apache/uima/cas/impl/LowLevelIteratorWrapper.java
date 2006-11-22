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

import org.apache.uima.internal.util.IntPointerIterator;

/**
 * Implements the LowLevelIterator interface.
 * 
 */
class LowLevelIteratorWrapper implements LowLevelIterator {

  private final IntPointerIterator it;

  private final LowLevelIndex index;

  LowLevelIteratorWrapper(IntPointerIterator it, LowLevelIndex index) {
    super();
    this.it = it;
    this.index = index;
  }

  public final void moveToFirst() {
    this.it.moveToFirst();
  }

  public final void moveToLast() {
    this.it.moveToLast();
  }

  public final boolean isValid() {
    return this.it.isValid();
  }

  public final int ll_get() throws NoSuchElementException {
    return this.it.get();
  }

  public void moveToNext() {
    this.it.inc();
  }

  public void moveToPrevious() {
    this.it.dec();
  }

  public void moveTo(int fsRef) {
    this.it.moveTo(fsRef);
  }

  public Object copy() {
    return this.it.copy();
  }

  public int ll_indexSize() {
    // TODO: make this more efficient
    if (!this.isValid()) {
      return 0;
    }
    IntPointerIterator count = (IntPointerIterator) this.it.copy();
    count.moveToFirst();
    int size = 0;
    while (count.isValid()) {
      ++size;
      count.inc();
    }
    return size;
  }

  public LowLevelIndex ll_getIndex() {
    return this.index;
  }

}
