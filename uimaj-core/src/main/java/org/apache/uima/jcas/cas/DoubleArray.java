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
import java.util.PrimitiveIterator.OfDouble;
import java.util.Spliterator;
import java.util.function.DoubleConsumer;
import java.util.stream.DoubleStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.DoubleArrayFSImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** JCas class model for DoubleArray */
public final class DoubleArray extends TOP implements CommonPrimitiveArray<Double>, DoubleArrayFSImpl, Iterable<Double> {

  /* public static string for use where constants are needed, e.g. in some Java Annotations */
  public final static String _TypeName = CAS.TYPE_NAME_DOUBLE_ARRAY;
  
  /**
   * Each cover class when loaded sets an index. Used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public final static int typeIndexID = JCasRegistry.register(DoubleArray.class);

  public final static int type = typeIndexID;

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

  private final double[] theArray;
  
  // never called. Here to disable default constructor
  @SuppressWarnings("unused")
  private DoubleArray() {
    theArray = null;
  }

  /**
   *  Make a new DoubleArray of given size
   * @param jcas The JCas
   * @param length the length of the array 
   */
  public DoubleArray(JCas jcas, int length) {
    super(jcas);
    theArray = new double[length];
    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    if (_casView.isId2Fs()) {
      _casView.adjustLastFsV2size_nonHeapStoredArrays(); 
    }     
  }
  
  /**
   * used by generator
   * Make a new DoubleArray of given size
   * @param c -
   * @param t -
   * @param length the length of the array in bytes
   */
  public DoubleArray(TypeImpl t, CASImpl c, int length) {
    super(t, c);  
    theArray = new double[length];
    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    if (_casView.isId2Fs()) {
      _casView.adjustLastFsV2size_nonHeapStoredArrays(); 
    }     
  }

  /**
   * @see org.apache.uima.cas.DoubleArrayFS#get(int)
   */
  @Override
  public double get(int i) {
    return theArray[i];
  }

  /**
   * @see org.apache.uima.cas.DoubleArrayFS#set(int , double)
   */
  @Override
  public void set(int i, double v) {
    theArray[i] = v;
    _casView.maybeLogArrayUpdate(this, null, i);
  }

  /**
   * @see org.apache.uima.cas.DoubleArrayFS#copyFromArray(double[], int, int, int)
   */
  @Override
  public void copyFromArray(double[] src, int srcPos, int destPos, int length) {
    System.arraycopy(src, srcPos, theArray, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.DoubleArrayFS#copyToArray(int, double[], int, int)
   */
  @Override
  public void copyToArray(int srcPos, double[] dest, int destPos, int length) {
    System.arraycopy(theArray, srcPos, dest, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.DoubleArrayFS#toArray()
   */
  @Override
  public double[] toArray() {
    return Arrays.copyOf(theArray, theArray.length);
  }

  /** return the size of the array */
  @Override
  public int size() {
    return theArray.length;
  }

  /**
   * @see org.apache.uima.cas.DoubleArrayFS#copyToArray(int, String[], int, int)
   */
  @Override
  public void copyToArray(int srcPos, String[] dest, int destPos, int length) {
    _casView.checkArrayBounds(theArray.length, srcPos, length);
    for (int i = 0; i < length; i++) {
      dest[i + destPos] = Double.toString(theArray[i + srcPos]);
    }
  }

  /**
   * @see org.apache.uima.cas.DoubleArrayFS#copyFromArray(String[], int, int, int)
   */
  @Override
  public void copyFromArray(String[] src, int srcPos, int destPos, int length) {
    _casView.checkArrayBounds(theArray.length, destPos, length);
    for (int i = 0; i < length; i++) {
      theArray[i + destPos] = Double.parseDouble(src[i + srcPos]);
    }
  }
  
  // internal use
  public double[] _getTheArray() {
    return theArray;
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonArray#copyValuesFrom(org.apache.uima.jcas.cas.CommonArray)
   */
  @Override
  public void copyValuesFrom(CommonArrayFS v) {
    DoubleArray bv = (DoubleArray) v;
    System.arraycopy(bv.theArray,  0,  theArray, 0, theArray.length);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonPrimitiveArray#setArrayValueFromString(int, java.lang.String)
   */
  @Override
  public void setArrayValueFromString(int i, String v) {
    set(i, Double.parseDouble(v));    
  }
  
  @Override
  public Spliterator.OfDouble spliterator() {
    return Arrays.spliterator(theArray);
  }
  
  @Override
  public OfDouble iterator() {
    return new OfDouble() {
      int i = 0;
      
      @Override
      public boolean hasNext() {
        return i < size();
      }

      @Override
      public Double next() {
        if (!hasNext())
          throw new NoSuchElementException();
        return get(i++);
      }

      @Override
      public double nextDouble() {
        if (!hasNext())
          throw new NoSuchElementException();
        return get(i++);
      }
    };
  }
  
  /**
   * @return an DoubleStream over the elements of the array
   */
  public DoubleStream stream() {
    return Arrays.stream(theArray);
  }
  
  /**
   * @param jcas Which CAS to create the array in
   * @param a the source for the array's initial values
   * @return a newly created and populated array
   */
  public static DoubleArray create(JCas jcas, double[] a) {
    DoubleArray doubleArray = new DoubleArray(jcas, a.length);
    doubleArray.copyFromArray(a, 0, 0, a.length);
    return doubleArray;
  }

  /**
   * non boxing version 
   * @param action -
   */
  public void forEach(DoubleConsumer action) {
    for (double d : theArray) {
      action.accept(d);
    }
  }


  /**
   * @param item the item to see if is in the array
   * @return true if the item is in the array
   */
  public boolean contains(double item) {
    for (double b : theArray) {
      if (b == item) {
        return true;
      }
    }
    return false;
  }

}
