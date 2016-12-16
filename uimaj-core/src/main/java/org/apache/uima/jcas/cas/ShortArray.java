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

import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** JCas class model for ShortArray */
public final class ShortArray extends TOP implements ShortArrayFS, Iterable<Short> {
  /**
   * Each cover class when loaded sets an index. Used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public final static int typeIndexID = JCasRegistry.register(ShortArray.class);

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

  // never called. Here to disable default constructor
  private ShortArray() {
  }

 /* Internal - Constructor used by generator */
  public ShortArray(int addr, TOP_Type type) {
    super(addr, type);
  }

  /**
   * Make a new ShortArray of given size
   * @param jcas The JCas
   * @param length The number of elements in the new array
   */
  public ShortArray(JCas jcas, int length) {
    this(jcas.getLowLevelCas().ll_createShortArray(length), jcas.getType(typeIndexID));
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#get(int)
   */
  public short get(int i) {
    jcasType.casImpl.checkArrayBounds(addr, i);
    return jcasType.ll_cas.ll_getShortArrayValue(addr, i);
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#set(int , short)
   */
  public void set(int i, short v) {
    jcasType.casImpl.checkArrayBounds(addr, i);
    jcasType.ll_cas.ll_setShortArrayValue(addr, i, v);
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#copyFromArray(short[], int, int, int)
   */
  public void copyFromArray(short[] src, int srcOffset, int destOffset, int length) {
    jcasType.casImpl.checkArrayBounds(addr, destOffset, length);
    for (int i = 0; i < length; i++) {
      jcasType.ll_cas.ll_setShortArrayValue(addr, i + destOffset, src[i + srcOffset]);
    }
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#copyToArray(int, short[], int, int)
   */
  public void copyToArray(int srcOffset, short[] dest, int destOffset, int length) {
    jcasType.casImpl.checkArrayBounds(addr, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = jcasType.ll_cas.ll_getShortArrayValue(addr, i + srcOffset);
    }
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#toArray()
   */
  public short[] toArray() {
    final int size = size();
    short[] outArray = new short[size];
    copyToArray(0, outArray, 0, size);
    return outArray;
  }

  /** return the size of the array */
  public int size() {
    return jcasType.casImpl.ll_getArraySize(addr);
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#copyToArray(int, String[], int, int)
   */
  public void copyToArray(int srcOffset, String[] dest, int destOffset, int length) {
    jcasType.casImpl.checkArrayBounds(addr, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = Short.toString(jcasType.ll_cas.ll_getShortArrayValue(addr, i
              + srcOffset));
    }
  }

  /**
   * @see org.apache.uima.cas.ShortArrayFS#copyFromArray(String[], int, int, int)
   */
  public void copyFromArray(String[] src, int srcOffset, int destOffset, int length) {
    jcasType.casImpl.checkArrayBounds(addr, destOffset, length);
    for (int i = 0; i < length; i++) {
      jcasType.ll_cas.ll_setShortArrayValue(addr, i + destOffset, Short.parseShort(src[i
              + srcOffset]));
    }
  }

  public String[] toStringArray() {
    final int size = size();
    String[] strArray = new String[size];
    copyToArray(0, strArray, 0, size);
    return strArray;
  }
  
  @Override
  public Iterator<Short> iterator() {
    return new Iterator<Short>() {
      int i = 0;
      
      @Override
      public boolean hasNext() {
        return i < size();
      }

      @Override
      public Short next() {
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
