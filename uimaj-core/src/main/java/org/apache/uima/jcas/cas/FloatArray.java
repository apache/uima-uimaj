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

import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** Java Cas model for Cas FloatArray. */
public final class FloatArray extends TOP implements FloatArrayFS {

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

  private FloatArray() { // never called. Here to disable default constructor
  }

  // * There are n creation actions
  // * 1 - create via factory.create() a new one
  // * 2 - iterator or dereferencing calling generator to create

 /* Internal - Constructor used by generator */
  public FloatArray(int addr, TOP_Type type) {
    super(addr, type);
  }

  /**
   * Make a new FloatArray of given size
   * @param jcas the JCas
   * @param length the size of the array
   */
  public FloatArray(JCas jcas, int length) {
    this(
    /* addr */
    jcas.getLowLevelCas().ll_createArray(jcas.getType(typeIndexID).casTypeCode, length),
    /* type */
    jcas.getType(typeIndexID));

    // at this point we can use the jcasType value, as it is set
    // can't do this earlier as the very first statement is required by
    // JAVA to be the super or alternate constructor call
    jcasType.casImpl.checkArrayPreconditions(length);
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
    jcasType.casImpl.checkArrayBounds(addr, i);
    return jcasType.ll_cas.ll_getFloatArrayValue(addr, i);
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
    jcasType.casImpl.checkArrayBounds(addr, i);
    jcasType.ll_cas.ll_setFloatArrayValue(addr, i, v);
  }

  /**
   * @see org.apache.uima.cas.FloatArrayFS#copyFromArray(float[], int, int, int)
   */
  public void copyFromArray(float[] src, int srcOffset, int destOffset, int length) {
    jcasType.casImpl.checkArrayBounds(addr, destOffset, length);
    for (int i = 0; i < length; i++) {
      jcasType.ll_cas.ll_setFloatArrayValue(addr, i + destOffset, src[i + srcOffset]);
    }
  }

  /**
   * @see org.apache.uima.cas.FloatArrayFS#copyToArray(int, float[], int, int)
   */
  public void copyToArray(int srcOffset, float[] dest, int destOffset, int length) {
    jcasType.casImpl.checkArrayBounds(addr, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = jcasType.ll_cas.ll_getFloatArrayValue(addr, i + srcOffset);
    }
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#toArray()
   */
  public float[] toArray() {
    final int size = size();
    float[] outArray = new float[size];
    copyToArray(0, outArray, 0, size);
    return outArray;
  }

  /**
   * return the size of the array
   * 
   * @return size of array
   */

  public int size() {
    return jcasType.casImpl.ll_getArraySize(addr);
  }

  /**
   * @see org.apache.uima.cas.FloatArrayFS#copyToArray(int, String[], int, int)
   */
  public void copyToArray(int srcOffset, String[] dest, int destOffset, int length) {
    jcasType.casImpl.checkArrayBounds(addr, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = Float.toString(jcasType.ll_cas.ll_getFloatArrayValue(addr, i
              + srcOffset));
    }
  }

  /**
   * @see org.apache.uima.cas.FloatArrayFS#copyFromArray(String[], int, int, int)
   */
  public void copyFromArray(String[] src, int srcOffset, int destOffset, int length) {
    jcasType.casImpl.checkArrayBounds(addr, destOffset, length);
    for (int i = 0; i < length; i++) {
      jcasType.ll_cas.ll_setFloatArrayValue(addr, i + destOffset, Float.parseFloat(src[i
              + srcOffset]));
    }
  }

  /**
   * @see org.apache.uima.cas.FloatArrayFS#toStringArray()
   */
  public String[] toStringArray() {
    final int size = size();
    String[] strArray = new String[size];
    copyToArray(0, strArray, 0, size);
    return strArray;
  }
}
