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
import java.util.NoSuchElementException;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.cas.TOP;

/**
 * An empty Low-level FS iterator
 */
public class LowLevelIterator_empty<T extends FeatureStructure> implements LowLevelIterator<T> {

  @Override
  public boolean isValid() {
    return false;
  }

  @Override
  public T getNvc() {
    throw new NoSuchElementException();
  }

  @Override
  public void moveToFirstNoReinit() {
  }

  @Override
  public void moveToLastNoReinit() {
  }

  @Override
  public void moveToNoReinit(FeatureStructure fs) {
  }

  @Override
  public LowLevelIterator_empty<T> copy() {
    return this;
  }

  @Override
  public void moveToNextNvc() {
  }

  @Override
  public void moveToPreviousNvc() {
  }

  @Override
  public int ll_indexSizeMaybeNotCurrent() {
    return 0;
  }

  @Override
  public int ll_maxAnnotSpan() {
    return Integer.MAX_VALUE;
  }

  @Override
  public LowLevelIndex<T> ll_getIndex() {
    return null;
  }

  @Override
  public boolean isIndexesHaveBeenUpdated() {
    return false;
  }

  @Override
  public boolean maybeReinitIterator() {
    return false;
  }

  @Override
  public Comparator<TOP> getComparator() {
    return null;
  }

  @Override
  public Type getType() {
    return TypeSystemImpl.staticTsi.getTopType();
  }

  @Override
  public int size() {
    return 0;
  }
}
