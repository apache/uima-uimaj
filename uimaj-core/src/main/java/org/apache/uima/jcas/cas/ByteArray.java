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
import org.apache.uima.cas.impl.ByteArrayFSImpl;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** JCas class model for ByteArray */
public final class ByteArray extends TOP
        implements CommonPrimitiveArray<Byte>, ByteArrayFSImpl, Iterable<Byte> {

  /* public static string for use where constants are needed, e.g. in some Java Annotations */
  public final static String _TypeName = CAS.TYPE_NAME_BYTE_ARRAY;

  /**
   * Each cover class when loaded sets an index. Used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public final static int typeIndexID = JCasRegistry.register(ByteArray.class);

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

  private final byte[] theArray;

  // never called. Here to disable default constructor
  @SuppressWarnings("unused")
  private ByteArray() {
    theArray = null;
  }

  /**
   * Make a new ByteArray of given size
   * 
   * @param jcas
   *          the JCas
   * @param length
   *          the length of the array in bytes
   */
  public ByteArray(JCas jcas, int length) {
    super(jcas);
    theArray = new byte[length];
    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    if (_casView.isId2Fs()) {
      _casView.adjustLastFsV2size_nonHeapStoredArrays();
    }
  }

  /**
   * used by generator Make a new ByteArray of given size
   * 
   * @param c
   *          -
   * @param t
   *          -
   * @param length
   *          the length of the array in bytes
   */
  public ByteArray(TypeImpl t, CASImpl c, int length) {
    super(t, c);
    theArray = new byte[length];
    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    if (_casView.isId2Fs()) {
      _casView.adjustLastFsV2size_nonHeapStoredArrays();
    }
  }

  /**
   * @see org.apache.uima.cas.ByteArrayFS#get(int)
   */
  @Override
  public byte get(int i) {
    return theArray[i];
  }

  /**
   * @see org.apache.uima.cas.ByteArrayFS#set(int , byte)
   */
  @Override
  public void set(int i, byte v) {
    theArray[i] = v;
    _casView.maybeLogArrayUpdate(this, null, i);
  }

  /**
   * @see org.apache.uima.cas.ByteArrayFS#copyFromArray(byte[], int, int, int)
   */
  @Override
  public void copyFromArray(byte[] src, int srcPos, int destPos, int length) {
    System.arraycopy(src, srcPos, theArray, destPos, length);
    _casView.maybeLogArrayUpdates(this, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.ByteArrayFS#copyToArray(int, byte[], int, int)
   */
  @Override
  public void copyToArray(int srcPos, byte[] dest, int destPos, int length) {
    System.arraycopy(theArray, srcPos, dest, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.ByteArrayFS#toArray()
   */
  @Override
  public byte[] toArray() {
    return Arrays.copyOf(theArray, theArray.length);
  }

  /** return the size of the array */
  @Override
  public int size() {
    return theArray.length;
  }

  /**
   * @see org.apache.uima.cas.ByteArrayFS#copyToArray(int, String[], int, int)
   */
  @Override
  public void copyToArray(int srcPos, String[] dest, int destPos, int length) {
    _casView.checkArrayBounds(theArray.length, srcPos, length);
    for (int i = 0; i < length; i++) {
      dest[i + destPos] = Byte.toString(theArray[i + srcPos]);
    }
  }

  /**
   * @see org.apache.uima.cas.ByteArrayFS#copyFromArray(String[], int, int, int)
   */
  @Override
  public void copyFromArray(String[] src, int srcPos, int destPos, int length) {
    _casView.checkArrayBounds(theArray.length, destPos, length);
    for (int i = 0; i < length; i++) {
      theArray[i + destPos] = Byte.parseByte(src[i + srcPos]);
    }
    _casView.maybeLogArrayUpdates(this, destPos, length);
  }

  // internal use
  public byte[] _getTheArray() {
    return theArray;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.cas.CommonArray#copyValuesFrom(org.apache.uima.jcas.cas.CommonArray)
   */
  @Override
  public void copyValuesFrom(CommonArrayFS<Byte> v) {
    ByteArray bv = (ByteArray) v;
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
    set(i, Byte.parseByte(v));
  }

  @Override
  public Iterator<Byte> iterator() {
    return new Iterator<Byte>() {
      int i = 0;

      @Override
      public boolean hasNext() {
        return i < size();
      }

      @Override
      public Byte next() {
        if (!hasNext())
          throw new NoSuchElementException();
        return get(i++);
      }
    };
  }

  /**
   * @param jcas
   *          Which CAS to create the array in
   * @param a
   *          the source for the array's initial values
   * @return a newly created and populated array
   */
  public static ByteArray create(JCas jcas, byte[] a) {
    ByteArray byteArray = new ByteArray(jcas, a.length);
    byteArray.copyFromArray(a, 0, 0, a.length);
    return byteArray;
  }

  /**
   * @param item
   *          the item to see if is in the array
   * @return true if the item is in the array
   */
  public boolean contains(byte item) {
    for (byte b : theArray) {
      if (b == item) {
        return true;
      }
    }
    return false;
  }
}
