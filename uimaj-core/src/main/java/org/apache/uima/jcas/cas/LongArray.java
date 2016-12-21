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

package org.apache.uima.jcas.cas;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator.OfLong;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;
import java.util.Spliterator;

import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** JCas class model for LongArray */
public final class LongArray extends TOP implements CommonPrimitiveArray, LongArrayFS, Iterable<Long> {

  /* public static string for use where constants are needed, e.g. in some Java Annotations */
  public final static String _TypeName = "org.apache.uima.cas.jcas.LongArray";

  
  /**
   * Each cover class when loaded sets an index. Used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public final static int typeIndexID = JCasRegistry.register(LongArray.class);

  public final static int type = typeIndexID;

  /**
   * used to obtain reference to the _Type instance
   * 
   * @return the type array index
   */
  // can't be factored - refs locally defined field
  public int getTypeIndexID() {
    return typeIndexID;
  }

  private final long[] theArray;
  // never called. Here to disable default constructor
  @SuppressWarnings("unused")
  private LongArray() {
    theArray = null;
  }

  /**
   * Make a new LongArray of given size
   * @param jcas The JCas
   * @param length The number of elements in the new array
   */
  public LongArray(JCas jcas, int length) {
    super(jcas);
    theArray = new long[length];
    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    if (CASImpl.IS_USE_V2_IDS) {
      _casView.adjustLastFsV2size(2); // space for length and ref
    }     
  }

  /**
   * used by generator
   * Make a new LongArray of given size
   * @param c -
   * @param t -
   * @param length the length of the array in bytes
   */
  public LongArray(TypeImpl t, CASImpl c, int length) {
    super(t, c);  
    theArray = new long[length];
    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    if (CASImpl.IS_USE_V2_IDS) {
      _casView.adjustLastFsV2size(2); // space for length and ref
    }     
  }
  
  /**
   * @see org.apache.uima.cas.LongArrayFS#get(int)
   */
  public long get(int i) {
    return theArray[i];
  }

  /**
   * @see org.apache.uima.cas.LongArrayFS#set(int , long)
   */
  public void set(int i, long v) {
    theArray[i] = v;
    _casView.maybeLogArrayUpdate(this, null, i);
  }

  /**
   * @see org.apache.uima.cas.LongArrayFS#copyFromArray(long[], int, int, int)
   */
  public void copyFromArray(long[] src, int srcPos, int destPos, int length) {
    System.arraycopy(src, srcPos, theArray, destPos, length);
    _casView.maybeLogArrayUpdates(this, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.LongArrayFS#copyToArray(int, long[], int, int)
   */
  public void copyToArray(int srcPos, long[] dest, int destPos, int length) {
    System.arraycopy(theArray, srcPos, dest, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.LongArrayFS#toArray()
   */
  public long[] toArray() {
    return theArray.clone();
  }

  /** return the size of the array */
  public int size() {
    return theArray.length;
  }

  /**
   * @see org.apache.uima.cas.LongArrayFS#copyToArray(int, String[], int, int)
   */
  public void copyToArray(int srcPos, String[] dest, int destPos, int length) {
    _casView.checkArrayBounds(theArray.length, srcPos, length);
    for (int i = 0; i < length; i++) {
      dest[i + destPos] = Long.toString(theArray[i + srcPos]);
    }
  }

  /**
   * @see org.apache.uima.cas.LongArrayFS#copyFromArray(String[], int, int, int)
   */
  public void copyFromArray(String[] src, int srcPos, int destPos, int length) {
    _casView.checkArrayBounds(theArray.length, destPos, length);
    for (int i = 0; i < length; i++) {
      theArray[i + destPos] = Long.parseLong(src[i + srcPos]);
    }
    _casView.maybeLogArrayUpdates(this, destPos, length);
  }

  public String[] toStringArray() {
    final int size = size();
    String[] strArray = new String[size];
    copyToArray(0, strArray, 0, size);
    return strArray;
  }
  
  // internal use
  public long[] _getTheArray() {
    return theArray;
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonArray#copyValuesFrom(org.apache.uima.jcas.cas.CommonArray)
   */
  @Override
  public void copyValuesFrom(CommonArray v) {
    LongArray bv = (LongArray) v;
    System.arraycopy(bv.theArray,  0,  theArray, 0, theArray.length);
    _casView.maybeLogArrayUpdates(this, 0, size());
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonPrimitiveArray#setArrayValueFromString(int, java.lang.String)
   */
  @Override
  public void setArrayValueFromString(int i, String v) {
    set(i, Long.parseLong(v));
  }
  
  public Spliterator.OfLong spliterator() {
    return Arrays.spliterator(theArray);
  }
  
  @Override
  public OfLong iterator() {
    return new OfLong() {
      int i = 0;
      
      @Override
      public boolean hasNext() {
        return i < size();
      }

      @Override
      public Long next() {
        if (!hasNext())
          throw new NoSuchElementException();
        return get(i++);
      }

      @Override
      public long nextLong() {
        if (!hasNext())
          throw new NoSuchElementException();
        return get(i++);
      }
   };
  }
  
  /**
   * @return an LongStream over the elements of the array
   */
  public LongStream stream() {
    return Arrays.stream(theArray);
  }

  /**
   * @param jcas Which CAS to create the array in
   * @param a the source for the array's initial values
   * @return a newly created and populated array
   */
  public static LongArray createFromArray(JCas jcas, long[] a) {
    LongArray longArray = new LongArray(jcas, a.length);
    longArray.copyFromArray(a, 0, 0, a.length);
    return longArray;
  }

}
