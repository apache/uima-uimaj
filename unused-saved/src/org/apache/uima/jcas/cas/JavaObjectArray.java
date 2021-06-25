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

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FeatureStructureImplC;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** Java Class model for Cas FSArray type */
public final class JavaObjectArray extends TOP implements CommonPrimitiveArray {

  /**
   * each cover class when loaded sets an index. used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public final static int typeIndexID = JCasRegistry.register(JavaObjectArray.class);

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

  private final Object[] theArray;
  
  // never called. Here to disable default constructor
  @SuppressWarnings("unused")
  private JavaObjectArray() {
    theArray = null;
  }

  /**
   * Make a new FSArray of given size
   * @param jcas The JCas
   * @param length The number of elements in the new array
   */
  public JavaObjectArray(JCas jcas, int length) {
    super(jcas);
    theArray = new Object[length];
    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    if (CASImpl.IS_USE_V2_IDS) {
      _casView.adjustLastFsV2size(length);
    }    
  }
  
  /**
   * used by generator
   * Make a new FSArray of given size
   * @param c -
   * @param t -
   * @param length the length of the array
   */
  public JavaObjectArray(TypeImpl t, CASImpl c, int length) {
    super(t, c);  
    theArray = new Object[length];
    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    if (CASImpl.IS_USE_V2_IDS) {
      _casView.adjustLastFsV2size(length);
    }    
  }


  /** return the indexed value from the corresponding Cas FSArray as a Java Model object. */
  public Object get(int i) {
    return theArray[i];
  }

  /** updates the Cas, setting the indexed value with the corresponding Cas FeatureStructure. */
  public void set(int i, Object v) {
    theArray[i] = v;
    _casView.maybeLogArrayUpdate(this, null, i);
  }

  /** return the size of the array. */
  public int size() {
    return theArray.length;
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyFromArray(FeatureStructure[], int, int, int)
   */
  public void copyFromArray(FeatureStructure[] src, int srcPos, int destPos, int length) {
    System.arraycopy(src, srcPos, theArray, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyToArray(int, FeatureStructure[], int, int)
   */
  public void copyToArray(int srcPos, FeatureStructure[] dest, int destPos, int length) {
    System.arraycopy(theArray, srcPos, dest, destPos, length);
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#toArray()
   */
  public Object[] toArray() {
    return theArray.clone();
  }

  /**
   * Not supported, will throw UnsupportedOperationException
   */
  public void copyFromArray(String[] src, int srcPos, int destPos, int length) {
    throw new UnsupportedOperationException();
  }
    
  /**
   * Copies an array of Feature Structures to an Array of Strings.
   * The strings are the "toString()" representation of the feature structures, 
   * 
   * @param srcPos
   *                The index of the first element to copy.
   * @param dest
   *                The array to copy to.
   * @param destPos
   *                Where to start copying into <code>dest</code>.
   * @param length
   *                The number of elements to copy.
   * @exception ArrayIndexOutOfBoundsException
   *                    If <code>srcPos &lt; 0</code> or
   *                    <code>length &gt; size()</code> or
   *                    <code>destPos + length &gt; destArray.length</code>.
   */
  public void copyToArray(int srcPos, String[] dest, int destPos, int length) {
    _casView.checkArrayBounds(theArray.length, srcPos, length);
    for (int i = 0; i < length; i++) {
      dest[i + destPos] = theArray[i + srcPos].toString();
    }
  }

  public String[] toStringArray() {
    final int size = size();
    String[] strArray = new String[size];
    copyToArray(0, strArray, 0, size);
    return strArray;
  }

  // internal use
  public Object[] _getTheArray() {
    return theArray;
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonArray#copyValuesFrom(org.apache.uima.jcas.cas.CommonArray)
   */
  @Override
  public void copyValuesFrom(CommonArray v) {
    JavaObjectArray bv = (JavaObjectArray) v;
    System.arraycopy(bv.theArray,  0,  theArray, 0, theArray.length);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonPrimitiveArray#setArrayValueFromString(int, java.lang.String)
   */
  @Override
  public void setArrayValueFromString(int i, String v) {
//    set(i, -- insert code for general string -> java object here
//              for use by XCAS, XMI);
    throw new UnsupportedOperationException("Java Object deserialization not yet supported");                  
  }

  
}
