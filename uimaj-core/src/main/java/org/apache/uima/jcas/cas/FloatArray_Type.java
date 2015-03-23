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
// * Implementation of JcasFloatArray_Type*
// *********************************
/**
 * The java Cas model for the CAS FloatArray Type This is <b>not final</b> because the migration
 * from pre v08 has the old xxx_Type as a subclass of this.
 */
public class FloatArray_Type extends CommonArray_Type {

  public final static int typeIndexID = FloatArray.typeIndexID;

  // generator used by the CAS system when it needs to make a new instance

  protected FSGenerator<?> getFSGenerator() {
    return null; // no longer used, but may be needed for compatibility with older existing JCasGen'd cover classes that might extend this class
  }
//
//  private final FSGenerator fsGenerator = new FSGenerator() {
//    public FeatureStructure createFS(int addr, CASImpl cas) {
//      if (instanceOf_Type.useExistingInstance) {
//        // Return eq fs instance if already created
//        FeatureStructure fs = instanceOf_Type.jcas.getJfsFromCaddr(addr);
//        if (null == fs) {
//          fs = new FloatArray(addr, instanceOf_Type);
//          instanceOf_Type.jcas.putJfsFromCaddr(addr, fs);
//          return fs;
//        }
//        return fs;
//      } else
//        return new FloatArray(addr, instanceOf_Type);
//    }
//  };

  private FloatArray_Type() {
  } // block default new operator

  public FloatArray_Type(JCas jcas, Type casType) {
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
   * @return the indexed value from the corresponding Cas floatArray as a Java float.
   */
  public float get(int addr, int i) {
    if (lowLevelTypeChecks)
      return ll_cas.ll_getFloatArrayValue(addr, i, true);
    if (lowLevelArrayBoundChecks)
      casImpl.checkArrayBounds(addr, i);
    return ll_cas.ll_getFloatArrayValue(addr, i);
  }

  /**
   * updates the Cas, setting the indexed value to the passed in Java float value.
   * @see org.apache.uima.cas.ArrayFS#set(int, FeatureStructure)
   * 
   * @param addr the low level CAS Feature Structure reference
   * @param i the index
   * @param value the value to set
   */
  public void set(int addr, int i, float value) {
    if (lowLevelTypeChecks)
      ll_cas.ll_setFloatArrayValue(addr, i, value, true);
    if (lowLevelArrayBoundChecks)
      casImpl.checkArrayBounds(addr, i);
    ll_cas.ll_setFloatArrayValue(addr, i, value);
  }

  /**
   * @see org.apache.uima.cas.FloatArrayFS#copyFromArray(float[], int, int, int)
   * 
   * @param addr the low level CAS Feature Structure reference to copy into
   * @param src the array to copy from
   * @param srcOffset the source offset
   * @param destOffset the destination offset
   * @param length the number of items to copy
   */
  public void copyFromArray(int addr, float[] src, int srcOffset, int destOffset, int length) {
    if (lowLevelArrayBoundChecks)
      casImpl.checkArrayBounds(addr, destOffset, length);
    for (int i = 0; i < length; i++) {
      ll_cas.ll_setFloatArrayValue(addr, i + destOffset, src[i + srcOffset]);
    }
  }
  /**
   * 
   * @param addr the low level CAS Feature Structure reference to copy from
   * @param srcOffset the source offset
   * @param dest the target to copy into
   * @param destOffset the destination offset
   * @param length the number of items to copy
   */
  public void copyToArray(int addr, int srcOffset, float[] dest, int destOffset, int length) {
    if (lowLevelArrayBoundChecks)
      casImpl.checkArrayBounds(addr, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = ll_cas.ll_getFloatArrayValue(addr, i + srcOffset);
    }
  }

  /**
   * @see org.apache.uima.cas.FloatArrayFS#toArray()
   * 
   * @param addr the low level CAS Feature Structure reference to copy from
   * @return a copy of the array
   */
  public float[] toArray(int addr) {
    final int size = size(addr);
    float[] outArray = new float[size];
    copyToArray(addr, 0, outArray, 0, size);
    return outArray;
  }
}
