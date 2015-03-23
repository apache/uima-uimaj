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

// import java.lang.reflect.Constructor;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.jcas.JCas;

// *********************************
// * Implementation of FSArray_Type_JCasImpl*
// *********************************
/**
 * The java Cas model for the CAS FSArray Type This is <b>not final</b> because the migration from
 * pre v08 has the old FSArray_Type as a subclass of this.
 */
public class FSArray_Type extends CommonArray_Type {

  public final static int typeIndexID = FSArray.typeIndexID;

//  // generator used by the CAS system when it needs to make a new instance
//
  protected FSGenerator<?> getFSGenerator() {
    return null; // no longer used, but may be needed for compatibility with older existing JCasGen'd cover classes that might extend this class
  }
//
//  private final FSGenerator fsGenerator = new FSGenerator() {
//    @SuppressWarnings("unchecked")
//    public FSArray createFS(int addr, CASImpl cas) {
//      if (instanceOf_Type.useExistingInstance) {
//        // Return eq fs instance if already created
//        FSArray fs = instanceOf_Type.jcas.getJfsFromCaddr(addr);
//        if (null == fs) {
//          fs = new FSArray(addr, instanceOf_Type);
//          instanceOf_Type.jcas.putJfsFromCaddr(addr, fs);
//          return fs;
//        }
//        return fs;
//      } else
//        return new FSArray(addr, instanceOf_Type);
//    }
//  };

  public FSArray_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    // Do not factor to TOP_Type - requires access to instance values
    // which are not set when super is called (per JVM spec)
//     casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());
  }

  // ******************************************************
  // * Low level interface version
  // ******************************************************

  /**
   * @see org.apache.uima.cas.ArrayFS#get(int)
   * 
   * @param addr the low level CAS Feature Structure reference
   * @param i the index
   * @return the indexed value from the corresponding Cas FSArray as a JCas object.
   */
  public FeatureStructure get(int addr, int i) {
    if (lowLevelTypeChecks)
      return ll_cas.ll_getFSForRef(ll_cas.ll_getRefArrayValue(addr, i, true));
    if (lowLevelArrayBoundChecks)
      casImpl.checkArrayBounds(addr, i);
    return ll_cas.ll_getFSForRef(ll_cas.ll_getRefArrayValue(addr, i));
  }

  /**
   * updates the Cas, setting the indexed value to the passed in FeatureStructure value.
   * 
   * @see org.apache.uima.cas.ArrayFS#set(int, FeatureStructure)
   * 
   * @param addr the low level CAS Feature Structure reference
   * @param i the index
   * @param v the value
   */
  public void set(int addr, int i, FeatureStructure v) {
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(addr, i, ll_cas.ll_getFSRef(v), true);
    if (lowLevelArrayBoundChecks)
      casImpl.checkArrayBounds(addr, i);
    ll_cas.ll_setRefArrayValue(addr, i, ll_cas.ll_getFSRef(v));
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#copyFromArray(FeatureStructure[], int, int, int)
   * 
   * @param addr the low level CAS Feature Structure reference
   * @param src the Java array object to copy from
   * @param srcOffset the source offset
   * @param destOffset the destination offset
   * @param length the number of items to copy
   * @throws ArrayIndexOutOfBoundsException if index out of bounds
   */
  public void copyFromArray(int addr, FeatureStructure[] src, int srcOffset, int destOffset,
          int length) throws ArrayIndexOutOfBoundsException {
    if (lowLevelArrayBoundChecks)
      if ((destOffset < 0) || ((destOffset + length) > size(addr)))
        throw new ArrayIndexOutOfBoundsException();
    // destOffset += casImpl.getArrayStartAddress(addr);
    for (int i = 0; i < length; i++) {
      // casImpl.heap.heap[destOffset] = ll_cas.ll_getFSRef(src[srcOffset]);
      ll_cas.ll_setRefArrayValue(addr, destOffset, ll_cas.ll_getFSRef(src[srcOffset]));
      ++destOffset;
      ++srcOffset;
    }
  }
  
  /**
   * @see org.apache.uima.cas.ArrayFS#copyToArray(int, FeatureStructure[], int, int)
   * 
   * @param addr the low level CAS Feature Structure reference
   * @param srcOffset the CAS source offset
   * @param dest the Java object to copy into
   * @param destOffset the destination offset
   * @param length the number of items to copy
   * @throws ArrayIndexOutOfBoundsException if index out of bounds
   */
  public void copyToArray(int addr, int srcOffset, FeatureStructure[] dest, int destOffset,
          int length) throws ArrayIndexOutOfBoundsException {
    if (lowLevelArrayBoundChecks)
      if ((srcOffset < 0) || ((srcOffset + length) > size(addr)))
        throw new ArrayIndexOutOfBoundsException();
    // srcOffset += casImpl.getArrayStartAddress(addr);
    for (int i = 0; i < length; i++) {
      // dest[destOffset] = ll_cas.ll_getFSForRef(srcOffset);
      dest[destOffset] = ll_cas.ll_getFSForRef(ll_cas.ll_getRefArrayValue(addr, srcOffset));
      ++destOffset;
      ++srcOffset;
    }
  }

  /**
   * @see org.apache.uima.cas.ArrayFS#toArray()
   * 
   * @param addr the low level CAS Feature Structure reference
   * @return a copy of the CAS array as a Java object
   */
  public FeatureStructure[] toArray(int addr) {
    final int size = size(addr);
    FeatureStructure[] outArray = new FeatureStructure[size];
    copyToArray(addr, 0, outArray, 0, size);
    return outArray;
  }

}
