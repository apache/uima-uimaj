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
import org.apache.uima.cas.impl.FSIndexFlat.FSIteratorFlat;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * Only used for debugging
 * Takes two iterators, and compares them; returns the 1st, throws error if unequal
 *
 * @param <T> -
 */
public class FSIteratorWrapperDoubleCheck<T extends FeatureStructure> extends FSIteratorImplBase<T> {
  

  @Override
  public String toString() {
    return "FSIteratorWrapper [it=" + nonFlatIterator + "]";
  }

  FSIterator<T> nonFlatIterator;
  
  FSIteratorFlat<T> flatIterator;

  FSIteratorWrapperDoubleCheck(FSIterator<T> nonFlatIterator, FSIteratorFlat<T> flatIterator) {
    this.nonFlatIterator = nonFlatIterator;
    this.flatIterator = flatIterator;
  }

  /**
   * @see org.apache.uima.cas.FSIterator#isValid()
   */
  public boolean isValid() {
    if (nonFlatIterator.isValid() != flatIterator.isValid()) {
      error(String.format("IndexIsUpdateFree=%s, %s, valid for reg iter is %s, valid for flat it2 is %s%n it1 = %s%nit2 = %s%n",
          flatIterator.isUpdateFreeSinceLastCounterReset(),
          flatIterator.idInfo(),
          nonFlatIterator.isValid(), flatIterator.isValid(), nonFlatIterator, flatIterator));
    }
    return this.nonFlatIterator.isValid();
  }

  /**
   * @see org.apache.uima.cas.FSIterator#get()
   */
  public T get() {
    T v = nonFlatIterator.get();
    T v2 = flatIterator.get();
    if (v.hashCode() != v2.hashCode()) {
      error(String.format("IndexIsUpdateFree=%s, get, %s, regularHeapAddr= %s, flatAddr= %s%n regIterator = %s%nflatIterator = %s%nv1=%s%nv2=%s%n", 
          flatIterator.isUpdateFreeSinceLastCounterReset(),
          flatIterator.idInfo(),
          v.hashCode(), v2.hashCode(), nonFlatIterator, flatIterator, 
          toStringSafe(v), toStringSafe(v2)));
    }
    return v;
  }
  
  private String toStringSafe(T fs) {
    int typecode = -1111;
    try {
      typecode = ((CASImpl)fs.getCAS()).getTypeCode(fs.hashCode());
      return fs.toString();
    } catch (Exception e) {
      return String.format("<exception while doing toString on fs,"
          + "heapAddr = %d, typeCode = %d, msg = %s>",
          fs.hashCode(),
          typecode,
          e.getMessage());
    }
  }

  /**
   * @see org.apache.uima.cas.FSIterator#moveToNext()
   */
  public void moveToNext() {
    this.nonFlatIterator.moveToNext();
    this.flatIterator.moveToNext();
  }

  /**
   * @see org.apache.uima.cas.FSIterator#moveToPrevious()
   */
  public void moveToPrevious() {
    this.nonFlatIterator.moveToPrevious();
    this.flatIterator.moveToPrevious();
  }

  /**
   * @see org.apache.uima.cas.FSIterator#moveToFirst()
   */
  public void moveToFirst() {
    this.nonFlatIterator.moveToFirst();
    this.flatIterator.moveToFirst();
  }

  /**
   * @see org.apache.uima.cas.FSIterator#moveToLast()
   */
  public void moveToLast() {
    this.nonFlatIterator.moveToLast();
    this.flatIterator.moveToLast();
  }

  /**
   * @see org.apache.uima.cas.FSIterator#copy()
   */
  public FSIterator<T> copy() {
    return new FSIteratorWrapperDoubleCheck<T>(nonFlatIterator.copy(), flatIterator.copy());
  }

  /**
   * @see org.apache.uima.cas.FSIterator#moveTo(FeatureStructure)
   */
  public void moveTo(FeatureStructure fs) {
    this.nonFlatIterator.moveTo(fs);
    this.flatIterator.moveTo(fs);
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.cas.impl.FSIteratorImplBase#moveTo(java.util.Comparator)
   */
  @Override
  <TT extends AnnotationFS> void moveTo(int begin, int end) {
    ((FSIteratorImplBase<TT>)(this.nonFlatIterator)).moveTo(begin, end);
    this.flatIterator.moveTo(begin, end);
  }


  private void error(String msg) {
    msg = flatIterator.verifyFsaSubsumes() + msg;
    throw new RuntimeException(msg);
  }
}
