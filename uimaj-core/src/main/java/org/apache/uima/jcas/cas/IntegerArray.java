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

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** The Java Class model corresponding to the Cas IntegerArray_JCasImpl type. */
public final class IntegerArray extends TOP implements IntArrayFS, Iterable<Integer> {
  /**
   * Each cover class when loaded sets an index. Used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public final static int typeIndexID = JCasRegistry.register(IntegerArray.class);

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

  private IntegerArray() { // never called. Here to disable default constructor
  }

 /* Internal - Constructor used by generator */
  public IntegerArray(int addr, TOP_Type type) {
    super(addr, type);
  }

  /**
   * Make a new IntegerArray of given size
   * @param jcas The JCas
   * @param length The number of elements in the new array
   */  
  public IntegerArray(JCas jcas, int length) {
    this(
    /* addr */jcas.getLowLevelCas().ll_createArray(jcas.getType(typeIndexID).casTypeCode, length),
    /* type */jcas.getType(typeIndexID));
    // at this point we can use the jcasType value, as it is set
    // can't do this earlier as the very first statement is required by
    // JAVA to be the super or alternate constructor call
    jcasType.casImpl.checkArrayPreconditions(length);
  }

  // /**
  // * create a new IntegerArray of a given size.
  // *
  // * @param jcas
  // * @param length
  // */
  //
  // public IntegerArray create(JCas jcas, int length) {
  // return new IntegerArray(jcas, length);
  // }

  /**
   * return the indexed value from the corresponding Cas IntegerArray_JCasImpl as an int.
   */
  public int get(int i) {
    jcasType.casImpl.checkArrayBounds(addr, i);
    return jcasType.ll_cas.ll_getIntArrayValue(addr, i);
  }

  /**
   * update the Cas, setting the indexed value to the passed in Java int value.
   */
  public void set(int i, int v) {
    jcasType.casImpl.checkArrayBounds(addr, i);
    jcasType.ll_cas.ll_setIntArrayValue(addr, i, v);
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#copyFromArray(int[], int, int, int)
   * 
   */
  public void copyFromArray(int[] src, int srcOffset, int destOffset, int length) {
    jcasType.casImpl.checkArrayBounds(addr, destOffset, length);
    for (int i = 0; i < length; i++) {
      jcasType.ll_cas.ll_setIntArrayValue(addr, i + destOffset, src[i + srcOffset]);
    }
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#copyToArray(int, int[], int, int)
   */
  public void copyToArray(int srcOffset, int[] dest, int destOffset, int length) {
    jcasType.casImpl.checkArrayBounds(addr, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = jcasType.ll_cas.ll_getIntArrayValue(addr, i + srcOffset);
    }
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#toArray()
   */
  public int[] toArray() {
    final int size = size();
    int[] outArray = new int[size];
    copyToArray(0, outArray, 0, size);
    return outArray;
  }

  /** return the size of the array */
  public int size() {
    return jcasType.casImpl.ll_getArraySize(addr);
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#copyToArray(int, String[], int, int)
   */
  public void copyToArray(int srcOffset, String[] dest, int destOffset, int length) {
    jcasType.casImpl.checkArrayBounds(addr, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = Integer.toString(jcasType.ll_cas.ll_getIntArrayValue(addr, i
              + srcOffset));
    }
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#copyFromArray(String[], int, int, int)
   */
  public void copyFromArray(String[] src, int srcOffset, int destOffset, int length) {
    jcasType.casImpl.checkArrayBounds(addr, destOffset, length);
    for (int i = 0; i < length; i++) {
      jcasType.ll_cas.ll_setIntArrayValue(addr, i + destOffset, Integer
              .parseInt(src[i + srcOffset]));
    }
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#toStringArray()
   */
  public String[] toStringArray() {
    final int size = size();
    String[] strArray = new String[size];
    copyToArray(0, strArray, 0, size);
    return strArray;
  }
  
  @Override
  public Iterator<Integer> iterator() {
    return new Iterator<Integer>() {
      int i = 0;
      
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
      public void remove() {
        throw new UnsupportedOperationException();
      }
      
    };
  }
}
