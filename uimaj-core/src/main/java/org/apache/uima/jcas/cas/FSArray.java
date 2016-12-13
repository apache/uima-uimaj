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

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** Java Class model for Cas FSArray type */
public final class FSArray extends TOP implements Iterable<FeatureStructure>, ArrayFS {

  /**
   * each cover class when loaded sets an index. used in the JCas typeArray to go from the cover
   * class or class instance to the corresponding instance of the _Type class
   */
  public final static int typeIndexID = JCasRegistry.register(FSArray.class);

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
  private FSArray() {
  }

 /* Internal - Constructor used by generator */
  public FSArray(int addr, TOP_Type type) {
    super(addr, type);
  }

  /**
   * Make a new FSArray of given size
   * @param jcas The JCas
   * @param length The number of elements in the new array
   */
  public FSArray(JCas jcas, int length) {
    this(
    /* addr */jcas.getLowLevelCas().ll_createArray(jcas.getType(typeIndexID).casTypeCode, length),
    /* type */jcas.getType(typeIndexID));

    // at this point we can use the jcasType value, as it is set
    // can't do this earlier as the very first statement is required by
    // JAVA to be the super or alternate constructor call
    jcasType.casImpl.checkArrayPreconditions(length);
  }

  // /** create a new FSArray of a given size.
  // *
  // * @param jcas
  // * @param length
  // */
  //  
  // public FSArray create(JCas jcas, int length){
  // return new FSArray(jcas, length);
  // }

  /** return the indexed value from the corresponding Cas FSArray as a Java Model object. */
  public FeatureStructure get(int i) {
    jcasType.casImpl.checkArrayBounds(addr, i);
    final LowLevelCAS ll_cas = jcasType.ll_cas;
    return ll_cas.ll_getFSForRef(ll_cas.ll_getRefArrayValue(addr, i));
  }

  /** updates the Cas, setting the indexed value with the corresponding Cas FeatureStructure. */
  public void set(int i, FeatureStructure v) {
    jcasType.casImpl.checkArrayBounds(addr, i);
    jcasType.ll_cas.ll_setRefArrayValue(addr, i, jcasType.ll_cas.ll_getFSRef(v));
  }

  /** return the size of the array. */
  public int size() {
    return jcasType.casImpl.ll_getArraySize(addr);
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyFromArray(FeatureStructure[], int, int, int)
   */
  public void copyFromArray(FeatureStructure[] src, int srcOffset, int destOffset, int length) {
    jcasType.casImpl.checkArrayBounds(addr, destOffset, length);
    for (int i = 0; i < length; i++) {
      jcasType.ll_cas.ll_setRefArrayValue(addr, i + destOffset, jcasType.ll_cas.ll_getFSRef(src[i
              + srcOffset]));
    }
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyToArray(int, FeatureStructure[], int, int)
   */
  public void copyToArray(int srcOffset, FeatureStructure[] dest, int destOffset, int length) {
    jcasType.casImpl.checkArrayBounds(addr, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(
              addr, i + srcOffset));
    }
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#toArray()
   */
  public FeatureStructure[] toArray() {
    final int size = size();
    FeatureStructure[] outArray = new FeatureStructure[size];
    copyToArray(0, outArray, 0, size);
    return outArray;
  }

  /**
   * Not supported, will throw UnsupportedOperationException
   */
  public void copyFromArray(String[] src, int srcOffset, int destOffset, int length) {
    throw new UnsupportedOperationException();
  }
    
  /**
   * Copies an array of Feature Structures to an Array of Strings.
   * The strings are the "toString()" representation of the feature structures, 
   * which are probably something that looks like FeatureStructure@123456
   * 
   * @param srcOffset
   *                The index of the first element to copy.
   * @param dest
   *                The array to copy to.
   * @param destOffset
   *                Where to start copying into <code>dest</code>.
   * @param length
   *                The number of elements to copy.
   * @exception ArrayIndexOutOfBoundsException
   *                    If <code>srcOffset &lt; 0</code> or
   *                    <code>length &gt; size()</code> or
   *                    <code>destOffset + length &gt; destArray.length</code>.
   */
  public void copyToArray(int srcOffset, String[] dest, int destOffset, int length) {
    CASImpl ll = jcasType.casImpl;
    ll.checkArrayBounds(addr, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = ll.ll_getFSForRef(ll.ll_getRefArrayValue(this.addr, i + srcOffset)).toString();
    }
  }

  public String[] toStringArray() {
    final int size = size();
    String[] strArray = new String[size];
    copyToArray(0, strArray, 0, size);
    return strArray;
  }

  @Override
  public Iterator<FeatureStructure> iterator() {
    return new Iterator<FeatureStructure>() {
      int i = 0;
      
      @Override
      public boolean hasNext() {
        return i < size();
      }

      @Override
      public FeatureStructure next() {
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
