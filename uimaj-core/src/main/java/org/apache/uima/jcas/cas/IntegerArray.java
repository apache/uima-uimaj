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
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator.OfInt;
import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.IntArrayFSImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** The Java Class model corresponding to the Cas IntegerArray_JCasImpl type. */
public final class IntegerArray extends TOP
        implements CommonPrimitiveArray<Integer>, IntArrayFSImpl, Iterable<Integer> {

  /* public static string for use where constants are needed, e.g. in some Java Annotations */
  public static final String _TypeName = CAS.TYPE_NAME_INTEGER_ARRAY;

  /**
   * Each cover class when loaded sets an index. Used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public static final int typeIndexID = JCasRegistry.register(IntegerArray.class);

  public static final int type = typeIndexID;

  /**
   * used to obtain reference to the _Type instance
   * 
   * @return the type array index
   */
  // can't be factored - refs locally defined field
  @Override
  public int getTypeIndexID() {
    return typeIndexID;
  }

  private final int[] theArray;

  @SuppressWarnings("unused")
  private IntegerArray() { // never called. Here to disable default constructor
    theArray = null;
  }

  /**
   * Make a new IntegerArray of given size
   * 
   * @param jcas
   *          The JCas
   * @param length
   *          The number of elements in the new array
   */
  public IntegerArray(JCas jcas, int length) {
    super(jcas);
    theArray = new int[length];
    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    if (_casView.isId2Fs()) {
      _casView.adjustLastFsV2Size_arrays(length);
    }
  }

  /**
   * used by generator Make a new IntegerArray of given size
   * 
   * @param c
   *          -
   * @param t
   *          -
   * @param length
   *          the length of the array in bytes
   */
  public IntegerArray(TypeImpl t, CASImpl c, int length) {
    super(t, c);
    theArray = new int[length];
    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    if (_casView.isId2Fs()) {
      _casView.adjustLastFsV2Size_arrays(length);
    }
  }

  /**
   * return the indexed value from the corresponding Cas IntegerArray_JCasImpl as an int.
   */
  @Override
  public int get(int i) {
    return theArray[i];
  }

  /**
   * update the Cas, setting the indexed value to the passed in Java int value.
   */
  @Override
  public void set(int i, int v) {
    theArray[i] = v;
    _casView.maybeLogArrayUpdate(this, null, i);
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#copyFromArray(int[], int, int, int)
   * 
   */
  @Override
  public void copyFromArray(int[] src, int srcPos, int destPos, int length) {
    System.arraycopy(src, srcPos, theArray, destPos, length);
    _casView.maybeLogArrayUpdates(this, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#copyToArray(int, int[], int, int)
   */
  @Override
  public void copyToArray(int srcPos, int[] dest, int destPos, int length) {
    System.arraycopy(theArray, srcPos, dest, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#toArray()
   */
  @Override
  public int[] toArray() {
    return Arrays.copyOf(theArray, theArray.length);
  }

  /** return the size of the array */
  @Override
  public int size() {
    return theArray.length;
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#copyToArray(int, String[], int, int)
   */
  @Override
  public void copyToArray(int srcOffset, String[] dest, int destOffset, int length) {
    _casView.checkArrayBounds(theArray.length, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = Integer.toString(theArray[i + srcOffset]);
    }
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#copyFromArray(String[], int, int, int)
   */
  @Override
  public void copyFromArray(String[] src, int srcOffset, int destOffset, int length) {
    _casView.checkArrayBounds(theArray.length, srcOffset, length);
    for (int i = 0; i < length; i++) {
      // use set to get proper logging
      set(i + destOffset, Integer.parseInt(src[i + srcOffset]));
    }
  }

  // internal use only
  public int[] _getTheArray() {
    return theArray;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.cas.CommonArray#copyValuesFrom(org.apache.uima.jcas.cas.CommonArray)
   */
  @Override
  public void copyValuesFrom(CommonArrayFS v) {
    IntegerArray bv = (IntegerArray) v;
    System.arraycopy(bv.theArray, 0, theArray, 0, theArray.length);
    _casView.maybeLogArrayUpdates(this, 0, size());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.cas.CommonPrimitiveArray#setArrayValueFromString(int,
   * java.lang.String)
   */
  @Override
  public void setArrayValueFromString(int i, String v) {
    set(i, Integer.parseInt(v));
  }

  @Override
  public Spliterator.OfInt spliterator() {
    return Arrays.spliterator(theArray);
  }

  @Override
  public OfInt iterator() {
    return new OfInt() {

      int i = 0;

      /*
       * (non-Javadoc)
       * 
       * @see java.util.PrimitiveIterator.OfInt#forEachRemaining(java.util.function.IntConsumer)
       */
      @Override
      public void forEachRemaining(IntConsumer action) {
        final int sz = size();
        for (; i < sz; i++) {
          action.accept(theArray[i]);
        }
      }

      @Override
      public boolean hasNext() {
        return i < size();
      }

      @Override
      public Integer next() {
        if (!hasNext())
          throw new NoSuchElementException();
        return get(i++);
      }

      @Override
      public int nextInt() {
        if (!hasNext())
          throw new NoSuchElementException();
        return get(i++);
      }
    };
  }

  /**
   * @return an IntStream over the elements of the array
   */
  public IntStream stream() {
    return Arrays.stream(theArray);
  }

  /**
   * @param jcas
   *          Which CAS to create the array in
   * @param a
   *          the source for the array's initial values
   * @return a newly created and populated array
   */
  public static IntegerArray create(JCas jcas, int[] a) {
    IntegerArray intArray = new IntegerArray(jcas, a.length);
    intArray.copyFromArray(a, 0, 0, a.length);
    return intArray;
  }

  /**
   * non boxing version
   * 
   * @param action
   *          -
   */
  public void forEach(IntConsumer action) {
    for (int d : theArray) {
      action.accept(d);
    }
  }

  /**
   * @param item
   *          the item to see if is in the array
   * @return true if the item is in the array
   */
  public boolean contains(int item) {
    for (int b : theArray) {
      if (b == item) {
        return true;
      }
    }
    return false;
  }

}
