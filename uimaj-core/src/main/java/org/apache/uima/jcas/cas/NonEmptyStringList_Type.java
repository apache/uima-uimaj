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

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

public class NonEmptyStringList_Type extends StringList_Type {
  protected FSGenerator<?> getFSGenerator() {
    return null; // no longer used, but may be needed for compatibility with older existing JCasGen'd cover classes that might extend this class
  }
//
//  private final FSGenerator fsGenerator = new FSGenerator() {
//    @SuppressWarnings("unchecked")
//    public NonEmptyStringList createFS(int addr, CASImpl cas) {
//      if (instanceOf_Type.useExistingInstance) {
//        // Return eq fs instance if already created
//        NonEmptyStringList fs = instanceOf_Type.jcas.getJfsFromCaddr(addr);
//        if (null == fs) {
//          fs = new NonEmptyStringList(addr, instanceOf_Type);
//          instanceOf_Type.jcas.putJfsFromCaddr(addr, fs);
//          return fs;
//        }
//        return fs;
//      } else
//        return new NonEmptyStringList(addr, instanceOf_Type);
//    }
//  };

  public final static int typeIndexID = NonEmptyStringList.typeIndexID;

  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("uima.cas.NonEmptyStringList");

  final Feature casFeat_head;

  final int casFeatCode_head;

  public String getHead(int addr) {
    if (featOkTst && casFeat_head == null)
      this.jcas.throwFeatMissing("head", "uima.cas.NonEmptyStringList");
    return ll_cas.ll_getStringValue(addr, casFeatCode_head);
  }

  public void setHead(int addr, String v) {
    if (featOkTst && casFeat_head == null)
      this.jcas.throwFeatMissing("head", "uima.cas.NonEmptyStringList");
    ll_cas.ll_setStringValue(addr, casFeatCode_head, v);
  }

  final Feature casFeat_tail;

  final int casFeatCode_tail;

  public int getTail(int addr) {
    if (featOkTst && casFeat_tail == null)
      this.jcas.throwFeatMissing("tail", "uima.cas.NonEmptyStringList");
    return ll_cas.ll_getRefValue(addr, casFeatCode_tail);
  }

  public void setTail(int addr, int v) {
    if (featOkTst && casFeat_tail == null)
      this.jcas.throwFeatMissing("tail", "uima.cas.NonEmptyStringList");
    ll_cas.ll_setRefValue(addr, casFeatCode_tail, v);
  }

  // * initialize variables to correspond with Cas Type and Features
  public NonEmptyStringList_Type(JCas jcas, Type casType) {
    super(jcas, casType);
//     casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

    casFeat_head = jcas.getRequiredFeatureDE(casType, "head", "uima.cas.String", featOkTst);
    casFeatCode_head = (null == casFeat_head) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_head).getCode();
    casFeat_tail = jcas.getRequiredFeatureDE(casType, "tail", "uima.cas.StringList", featOkTst);
    casFeatCode_tail = (null == casFeat_tail) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_tail).getCode();
  }

  protected NonEmptyStringList_Type() { // block default new operator
    casFeat_head = null;
    casFeatCode_head = JCas.INVALID_FEATURE_CODE;
    casFeat_tail = null;
    casFeatCode_tail = JCas.INVALID_FEATURE_CODE;
    throw new RuntimeException("Internal Error-this constructor should never be called.");
  }

}
