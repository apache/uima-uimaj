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
import java.util.stream.Stream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.StringArrayFSImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** JCas class model for StringArray */
public final class StringArray extends TOP implements Iterable<String>, CommonPrimitiveArray<String>, StringArrayFSImpl {

  /* public static string for use where constants are needed, e.g. in some Java Annotations */
  public final static String _TypeName = CAS.TYPE_NAME_STRING_ARRAY;

  /**
   * Each cover class when loaded sets an index. Used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public final static int typeIndexID = JCasRegistry.register(StringArray.class);

  public final static int type = typeIndexID;

  /**
   * used to obtain reference to the TOP_Type instance
   * 
   * @return the type array index
   */
  // can't be factored - refs locally defined field
  public int getTypeIndexID() {
    return typeIndexID;
  }

  private final String[] theArray;
  // never called. Here to disable default constructor
  @SuppressWarnings("unused")
  private StringArray() {
    theArray = null;
  }

  /**
   * Make a new StringArray of given size
   * @param jcas The JCas
   * @param length The number of elements in the new array
   */
  public StringArray(JCas jcas, int length) {
    super(jcas);
    theArray = new String[length];
    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    if (_casView.isId2Fs()) {
      _casView.adjustLastFsV2Size_arrays(length);
    }    
  }

  /**
   * used by generator
   * Make a new StringArray of given size
   * @param c -
   * @param t -
   * @param length the length of the array in bytes
   */
  public StringArray(TypeImpl t, CASImpl c, int length) {
    super(t, c);  
    theArray = new String[length];
    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    if (_casView.isId2Fs()) {
      _casView.adjustLastFsV2Size_arrays(length);
    }    
  }

  /**
   * @see org.apache.uima.cas.StringArrayFS#get(int)
   */
  public String get(int i) {
    return theArray[i];
  }

  /**
   * @see org.apache.uima.cas.StringArrayFS#set(int, String)
   */
  public void set(int i, String v) {
    theArray[i] = v;
    _casView.maybeLogArrayUpdate(this, null, i);
  }

  /**
   * @see org.apache.uima.cas.StringArrayFS#copyFromArray(String[], int, int, int)
   */
  public void copyFromArray(String[] src, int srcPos, int destPos, int length) {
    System.arraycopy(src, srcPos, theArray, destPos, length);
    _casView.maybeLogArrayUpdates(this, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.StringArrayFS#copyToArray(int, String[], int, int)
   */
  public void copyToArray(int srcPos, String[] dest, int destPos, int length) {
    System.arraycopy(theArray, srcPos, dest, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.StringArrayFS#toArray()
   */
  public String[] toArray() {
    return Arrays.copyOf(theArray, theArray.length);
  }

  /** return the size of the array */
  public int size() {
    return theArray.length;
  }

  // internal use
  public String[] _getTheArray() {
    return theArray;
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonArray#copyValuesFrom(org.apache.uima.jcas.cas.CommonArray)
   */
  @Override
  public void copyValuesFrom(CommonArrayFS v) {
    StringArray bv = (StringArray) v;
    System.arraycopy(bv.theArray,  0,  theArray, 0, theArray.length);
    _casView.maybeLogArrayUpdates(this, 0, size());
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonPrimitiveArray#setArrayValueFromString(int, java.lang.String)
   */
  @Override
  public void setArrayValueFromString(int i, String v) {
    set(i, v);    
  }

  @Override
  public Iterator<String> iterator() {
    return new Iterator<String>() {

      int i = 0;
      
      @Override
      public boolean hasNext() {
        return i < size();
      }

      @Override
      public String next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return get(i++);
      }      
    };
  }

  /**
   * @param jcas Which CAS to create the array in
   * @param a the source for the array's initial values
   * @return a newly created and populated array
   */
  public static StringArray create(JCas jcas, String[] a) {
    StringArray stringArray = new StringArray(jcas, a.length);
    stringArray.copyFromArray(a, 0, 0, a.length);
    return stringArray;
  }
  
  /**
   * @param v the compare object
   * @return true if v is equal to one (or more) of the array elements
   */
  public boolean contains(String v) {
    return Misc.contains(theArray, v);
  }

  public Stream<String> stream() {
    return Arrays.stream(theArray);
  }

}
