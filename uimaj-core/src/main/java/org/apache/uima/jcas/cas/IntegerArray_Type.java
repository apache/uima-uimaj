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

import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.jcas.JCas;

// *********************************
// * Implementation of IntegerArray_Type_JCasImpl*
// *********************************
/**
 * The java Cas model for the CAS IntegerArray_JCasImpl Type This is <b>not final</b> because the
 * migration from pre v08 has the old xxx_Type as a subclass of this.
 */
public class IntegerArray_Type extends CommonArray_Type {
  /**
   * this types ID - used to index a localTypeArray in JCas to get an index which indexes the global
   * typeArray in JCas instance to get a ref to this instance
   */
  public final static int typeIndexID = IntegerArray.typeIndexID;

//  // generator used by the CAS system when it needs to make a new instance
//
  protected FSGenerator<?> getFSGenerator() {
    return null; // no longer used, but may be needed for compatibility with older existing JCasGen'd cover classes that might extend this class
  }
//
//  private final FSGenerator fsGenerator = new FSGenerator() {
//    @SuppressWarnings("unchecked")
//    public IntegerArray createFS(int addr, CASImpl cas) {
//      if (instanceOf_Type.useExistingInstance) {
//        // Return eq fs instance if already created
//        IntegerArray fs = instanceOf_Type.jcas.getJfsFromCaddr(addr);
//        if (null == fs) {
//          fs = new IntegerArray(addr, instanceOf_Type);
//          instanceOf_Type.jcas.putJfsFromCaddr(addr, fs);
//          return fs;
//        }
//        return fs;
//      } else
//        return new IntegerArray(addr, instanceOf_Type);
//    }
//  };

  private IntegerArray_Type() {
  } // block default new operator

  public IntegerArray_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    // Do not factor to TOP_Type - requires access to instance values
    // which are not set when super is called (per JVM spec)
//     casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());
  }

  // ******************************************************
  // * Low level interface version
  // ******************************************************

  /**
   * @see org.apache.uima.cas.IntArrayFS#get(int)
   * 
   * @param addr low level CAS Feature Structure reference
   * @param i the index
   * @return the indexed value from the corresponding Cas IntegerArray 
   */
  public int get(int addr, int i) {
    if (lowLevelTypeChecks)
      return ll_cas.ll_getIntArrayValue(addr, i, true);
    if (lowLevelArrayBoundChecks)
      casImpl.checkArrayBounds(addr, i);
    return ll_cas.ll_getIntArrayValue(addr, i);
  }

  /**
   * updates the Cas from the passed in Java value.
   * @see org.apache.uima.cas.IntArrayFS#set(int, int)
   * 
   * @param addr low level CAS Feature Structure reference
   * @param i the index
   * @param v the value
   */
  public void set(int addr, int i, int v) {
    if (lowLevelTypeChecks)
      ll_cas.ll_setIntArrayValue(addr, i, v, true);
    if (lowLevelArrayBoundChecks)
      casImpl.checkArrayBounds(addr, i);
    ll_cas.ll_setIntArrayValue(addr, i, v);
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#copyFromArray(int[], int, int, int)
   * 
   * @param addr low level CAS Feature Structure reference
   * @param src the source (Java object) to copy from
   * @param srcOffset the source offset
   * @param destOffset the destination (the CAS Feature Structure) offset
   * @param length number of items to copy
   */
  public void copyFromArray(int addr, int[] src, int srcOffset, int destOffset, int length) {
    if (lowLevelArrayBoundChecks)
      casImpl.checkArrayBounds(addr, destOffset, length);
    for (int i = 0; i < length; i++) {
      ll_cas.ll_setIntArrayValue(addr, i + destOffset, src[i + srcOffset]);
    }
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#copyToArray(int, int[], int, int)
   * 
   * @param addr low level CAS Feature Structure reference
   * @param srcOffset The CAS source offset
   * @param dest the Java object to copy into
   * @param destOffset the destination offset
   * @param length number of items to copy
   */
  public void copyToArray(int addr, int srcOffset, int[] dest, int destOffset, int length) {
    if (lowLevelArrayBoundChecks)
      casImpl.checkArrayBounds(addr, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = ll_cas.ll_getIntArrayValue(addr, i + srcOffset);
    }
  }

  /**
   * @see org.apache.uima.cas.IntArrayFS#toArray()
   * 
   * @param addr low level CAS Feature Structure reference
   * @return a copy of the CAS array as a Java object
   */
  public int[] toArray(int addr) {
    final int size = size(addr);
    int[] outArray = new int[size];
    copyToArray(addr, 0, outArray, 0, size);
    return outArray;
  }
}
