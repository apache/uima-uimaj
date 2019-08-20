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

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FloatArrayFSImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** Java Cas model for Cas FloatArray. */
public final class FloatArray extends TOP 
                              implements CommonPrimitiveArray<Float>,
                                         Iterable<Float>,
                                         FloatArrayFSImpl {

  /* public static string for use where constants are needed, e.g. in some Java Annotations */
  public final static String _TypeName = CAS.TYPE_NAME_FLOAT_ARRAY;

  /**
   * Each cover class when loaded sets an index. Used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public final static int typeIndexID = JCasRegistry.register(FloatArray.class);

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
  
  private final float[] theArray;

  private FloatArray() { // never called. Here to disable default constructor
    theArray = null;
  }

  /**
   * Make a new FloatArray of given size
   * @param jcas the JCas
   * @param length the size of the array
   */
  public FloatArray(JCas jcas, int length) {
    super(jcas);
    theArray = new float[length];
    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    if (_casView.isId2Fs()) {
      _casView.adjustLastFsV2Size_arrays(length);
    }    
  }
  
  /**
   * used by generator
   * Make a new FloatArray of given size
   * @param c -
   * @param t -
   * @param length the length of the array in bytes
   */
  public FloatArray(TypeImpl t, CASImpl c, int length) {
    super(t, c);  
    theArray = new float[length];
    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    if (_casView.isId2Fs()) {
      _casView.adjustLastFsV2Size_arrays(length);
    }    
  }


  // /**
  // * create a new FloatArray of a given size.
  // *
  // * @param jcas
  // * @param length
  // */
  //
  // public FloatArray create(JCas jcas, int length) {
  // return new FloatArray(jcas, length);
  // }

  /**
   * return the indexed value from the corresponding Cas FloatArray as a float,
   */
  public float get(int i) {
    return theArray[i];
  }

  /**
   * update the Cas, setting the indexed value to the passed in Java float value.
   * 
   * @param i
   *          index
   * @param v
   *          value to set
   */
  public void set(int i, float v) {
    theArray[i] = v;
    _casView.maybeLogArrayUpdate(this, null, i);
  }

  /**
   * @see org.apache.uima.cas.FloatArrayFS#copyFromArray(float[], int, int, int)
   */
  public void copyFromArray(float[] src, int srcPos, int destPos, int length) {
    System.arraycopy(src, srcPos, theArray, destPos, length);
    _casView.maybeLogArrayUpdates(this, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.FloatArrayFS#copyToArray(int, float[], int, int)
   */
  public void copyToArray(int srcPos, float[] dest, int destPos, int length) {
    System.arraycopy(theArray, srcPos, dest, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#toArray()
   */
  public float[] toArray() {
    return Arrays.copyOf(theArray, theArray.length);
  }

  /**
   * return the size of the array
   * 
   * @return size of array
   */

  public int size() {
    return theArray.length;
  }

  /**
   * @see org.apache.uima.cas.FloatArrayFS#copyToArray(int, String[], int, int)
   */
  public void copyToArray(int srcOffset, String[] dest, int destOffset, int length) {
    _casView.checkArrayBounds(theArray.length, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = Float.toString(theArray[i + srcOffset]);
    }
  }

  /**
   * @see org.apache.uima.cas.FloatArrayFS#copyFromArray(String[], int, int, int)
   */
  public void copyFromArray(String[] src, int srcOffset, int destOffset, int length) {
    _casView.checkArrayBounds(theArray.length, destOffset, length);
    for (int i = 0; i < length; i++) {
      // use "set" to get proper journaling
      set(i + destOffset, Float.parseFloat(src[i + srcOffset]));
    }
  }
  
  // internal use only
  public float[] _getTheArray() {
    return theArray;
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonArray#copyValuesFrom(org.apache.uima.jcas.cas.CommonArray)
   */
  @Override
  public void copyValuesFrom(CommonArrayFS v) {
    FloatArray bv = (FloatArray) v;
    System.arraycopy(bv.theArray,  0,  theArray, 0, theArray.length);
    _casView.maybeLogArrayUpdates(this, 0, size());
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonPrimitiveArray#setArrayValueFromString(int, java.lang.String)
   */
  @Override
  public void setArrayValueFromString(int i, String v) {
    set(i, Float.parseFloat(v));    
  }
  
  /**
   * @param jcas Which CAS to create the array in
   * @param a the source for the array's initial values
   * @return a newly created and populated array
   */
  public static FloatArray create(JCas jcas, float[] a) {
    FloatArray floatArray = new FloatArray(jcas, a.length);
    floatArray.copyFromArray(a, 0, 0, a.length);
    return floatArray;
  }

  /* (non-Javadoc)
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator<Float> iterator() {
    return new Iterator<Float>() {

      int i = 0;
      
      @Override
      public boolean hasNext() {
        return i < size();
      }

      @Override
      public Float next() {
        if (!hasNext())
          throw new NoSuchElementException();
        return get(i++);
      }
      
    };
  }


  /**
   * @param item the item to see if is in the array
   * @return true if the item is in the array
   */
  public boolean contains(float item) {
    for (float b : theArray) {
      if (b == item) {
        return true;
      }
    }
    return false;
  }

}
