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

import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.jcas.JCas;

// *********************************
// * Implementation of ByteArray_Type for JCas
// *********************************
/**
 * The java Cas model for the CAS ByteArray_Type
 */
public final class ByteArray_Type extends CommonArray_Type {
  /**
   * this types ID - used to index a localTypeArray in JCas to get an index which indexes the global
   * typeArray in JCas instance to get a ref to this instance
   */
  public final static int typeIndexID = ByteArray.typeIndexID;

//  // generator used by the CAS system when it needs to make a new instance
//
  protected FSGenerator<?> getFSGenerator() {
    return null; // no longer used, but may be needed for compatibility with older existing JCasGen'd cover classes that might extend this class
  }
//
//  private final FSGenerator fsGenerator = new FSGenerator() {
//    @SuppressWarnings("unchecked")
//    public ByteArray createFS(int addr, CASImpl cas) {
//      if (ByteArray_Type.this.useExistingInstance) {
//        // Return eq fs instance if already created
//        ByteArray fs = (ByteArray) ByteArray_Type.this.jcas.getJfsFromCaddr(addr);
//        if (null == fs) {
//          fs = new ByteArray(addr, ByteArray_Type.this);
//          ByteArray_Type.this.jcas.putJfsFromCaddr(addr, fs);
//          return fs;
//        }
//        return fs;
//      } else
//        return new ByteArray(addr, ByteArray_Type.this);
//    }
//  };

  public ByteArray_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    // Do not factor to TOP_Type - requires access to instance values
    // which are not set when super is called (per JVM spec)
//     casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());
  }

  // ******************************************************
  // * Low level interface version
  // ******************************************************

  /**
   * @see org.apache.uima.cas.ByteArrayFS#get(int)
   * 
   * @param addr low level CAS Feature Structure reference to get value from
   * @param i the index
   * @return the indexed value from the corresponding Cas ByesArray.
   */
  public byte get(int addr, int i) {
    if (lowLevelTypeChecks)
      return ll_cas.ll_getByteArrayValue(addr, i, true);
    if (lowLevelArrayBoundChecks)
      casImpl.checkArrayBounds(addr, i);
    return ll_cas.ll_getByteArrayValue(addr, i);
  }

  /**
   * updates the Cas, setting the indexed value to the passed in Java value
   * @see org.apache.uima.cas.ByteArrayFS#set(int, byte)
   * @param addr low level CAS Feature Structure reference to set value into
   * @param i the index
   * @param v the value
   */
  public void set(int addr, int i, byte v) {
    if (lowLevelTypeChecks)
      ll_cas.ll_setByteArrayValue(addr, i, v, true);
    if (lowLevelArrayBoundChecks)
      casImpl.checkArrayBounds(addr, i);
    ll_cas.ll_setByteArrayValue(addr, i, v);
  }

  /**
   * @see org.apache.uima.cas.ByteArrayFS#copyFromArray(byte[], int, int, int)
   * 
   * @param addr low level reference to the FS in the CAS
   * @param src where to copy data from
   * @param srcOffset the source offset
   * @param destOffset the destination offset
   * @param length the number of bytes to copy
   */
  public void copyFromArray(int addr, byte[] src, int srcOffset, int destOffset, int length) {
    if (lowLevelArrayBoundChecks)
      casImpl.checkArrayBounds(addr, destOffset, length);
    for (int i = 0; i < length; i++) {
      ll_cas.ll_setByteArrayValue(addr, i + destOffset, src[i + srcOffset]);
    }
  }

  /**
   * @see org.apache.uima.cas.ByteArrayFS#copyToArray(int, byte[], int, int)
   * 
   * @param addr low level reference to the FS in the CAS
   * @param srcOffset the source offset
   * @param dest the array to copy into
   * @param destOffset the destination offset
   * @param length the number of bytes to copy
   */
  public void copyToArray(int addr, int srcOffset, byte[] dest, int destOffset, int length) {
    if (lowLevelArrayBoundChecks)
      casImpl.checkArrayBounds(addr, srcOffset, length);
    for (int i = 0; i < length; i++) {
      dest[i + destOffset] = ll_cas.ll_getByteArrayValue(addr, i + srcOffset);
    }
  }

  /**
   * @see org.apache.uima.cas.ByteArrayFS#toArray()
   * 
   * @param addr low level reference to the FS in the CAS
   * @return a copy of the byte array as a Java object
   */
  public byte[] toArray(int addr) {
    final int size = size(addr);
    byte[] outArray = new byte[size];
    copyToArray(addr, 0, outArray, 0, size);
    return outArray;
  }
}
