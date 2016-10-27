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

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SelectFSs;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.SelectFSs_impl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** Java Class model for Cas FSArray type */
public final class FSArray extends TOP implements CommonArray, ArrayFS {

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

  private final TOP[] theArray;
  
  // never called. Here to disable default constructor
  @SuppressWarnings("unused")
  private FSArray() {
    theArray = null;
  }

  /**
   * Make a new FSArray of given size
   * @param jcas The JCas
   * @param length The number of elements in the new array
   */
  public FSArray(JCas jcas, int length) {
    super(jcas);
    _casView.validateArraySize(length);
    theArray = new TOP[length];

    if (CASImpl.traceFSs) {
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
  public FSArray(TypeImpl t, CASImpl c, int length) {
    super(t, c);  
    _casView.validateArraySize(length);
    theArray = new TOP[length];
    
    if (CASImpl.traceFSs) {
      _casView.traceFSCreate(this);
    }
    if (CASImpl.IS_USE_V2_IDS) {
      _casView.adjustLastFsV2size(length);
    }    
  }


  /** return the indexed value from the corresponding Cas FSArray as a Java Model object. */
  public TOP get(int i) {
    return _maybeGetPearFs(theArray[i]);
  }

  /** updates the Cas, setting the indexed value with the corresponding Cas FeatureStructure. */
  public void set(int i, FeatureStructure v) {
    TOP vt = (TOP) v;
    if (vt != null && _casView.getBaseCAS() != vt._casView.getBaseCAS()) {
      /** Feature Structure {0} belongs to CAS {1}, may not be set as the value of an array or list element in a different CAS {2}.*/
      throw new CASRuntimeException(CASRuntimeException.FS_NOT_MEMBER_OF_CAS, vt, vt._casView, _casView);
    }
    theArray[i] = _maybeGetBaseForPearFs(vt);
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
    int srcEnd = srcPos + length;
    int destEnd = destPos + length;
    if (srcPos < 0 ||
        srcEnd > src.length ||
        destEnd > size()) {
      throw new ArrayIndexOutOfBoundsException(
          String.format("FSArray.copyFromArray, srcPos: %,d destPos: %,d length: %,d",  srcPos, destPos, length));
    }
    for (;srcPos < srcEnd && destPos < destEnd;) {
      set(destPos++, src[srcPos++]);
    }
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyToArray(int, FeatureStructure[], int, int)
   */
  public void copyToArray(int srcPos, FeatureStructure[] dest, int destPos, int length) {
    int srcEnd = srcPos + length;
    int destEnd = destPos + length;
    if (srcPos < 0 ||
        srcEnd > size() ||
        destEnd > dest.length) {
      throw new ArrayIndexOutOfBoundsException(
          String.format("FSArray.copyToArray, srcPos: %,d destPos: %,d length: %,d",  srcPos, destPos, length));
    }
    for (;srcPos < srcEnd && destPos < destEnd;) {
      dest[destPos++] = _maybeGetPearFs(get(srcPos++));
    }
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#toArray()
   */
  public FeatureStructure[] toArray() {
    FeatureStructure[] r = new FeatureStructure[size()];
    copyToArray(0, r, 0, size());
    return r;
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
      FeatureStructure fs = _maybeGetPearFs(theArray[i + srcPos]);
      dest[i + destPos] = (fs == null) ? null : fs.toString();
    }
  }

  public String[] toStringArray() {
    final int size = size();
    String[] strArray = new String[size];
    copyToArray(0, strArray, 0, size);
    return strArray;
  }

  // internal use
  // used by serializers
  // no conversion to Pear trampolines done
  public TOP[] _getTheArray() {
    return theArray;
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonArray#copyValuesFrom(org.apache.uima.jcas.cas.CommonArray)
   * no conversion to Pear trampolines done
   */
  @Override
  public void copyValuesFrom(CommonArray v) {
    FSArray bv = (FSArray) v;
    System.arraycopy(bv.theArray,  0,  theArray, 0, theArray.length);
  }
  
  /**
   * Treat an FSArray as a source for SelectFSs. 
   * @return a new instance of SelectFSs
   */
  public <T extends FeatureStructure> SelectFSs<T> select() {
    return new SelectFSs_impl<>(this);
  }

  /**
   * Treat an FSArray as a source for SelectFSs. 
   * @param filterByType only includes elements of this type
   * @return a new instance of SelectFSs
   */
  public <T extends FeatureStructure> SelectFSs<T> select(Type filterByType) {
    return new SelectFSs_impl<>(this).type(filterByType);
  }

  /**
   * Treat an FSArray as a source for SelectFSs.  
   * @param filterByType only includes elements of this JCas class
   * @return a new instance of SelectFSs
   */
  public <T extends FeatureStructure> SelectFSs<T> select(Class<T> filterByType) {
    return new SelectFSs_impl<>(this).type(filterByType);
  }
  
  /**
   * Treat an FSArray as a source for SelectFSs. 
   * @param filterByType only includes elements of this JCas class's type
   * @return a new instance of SelectFSs
   */
  public <T extends FeatureStructure> SelectFSs<T> select(int filterByType) {
    return new SelectFSs_impl<>(this).type(filterByType);
  }
  
  /**
   * Treat an FSArray as a source for SelectFSs. 
   * @param filterByType only includes elements of this type (fully qualifined type name)
   * @return a new instance of SelectFSs
   */
  public <T extends FeatureStructure> SelectFSs<T> select(String filterByType) {
    return new SelectFSs_impl<>(this).type(filterByType);
  }

  public static FSArray create(JCas jcas, FeatureStructure[] a) {
    FSArray fsa = new FSArray(jcas, a.length);
    fsa.copyFromArray(a, 0, 0, a.length);
    return fsa;
  }
}
