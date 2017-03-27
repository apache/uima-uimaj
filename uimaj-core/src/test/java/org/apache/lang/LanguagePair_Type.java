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

package org.apache.lang;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP_Type;

/* comment 7 of 14 */
public class LanguagePair_Type extends TOP_Type {
  protected FSGenerator getFSGenerator() {
    return fsGenerator;
  }

  private final FSGenerator fsGenerator = new FSGenerator() {
    public FeatureStructure createFS(int addr, CASImpl cas) {
      if (instanceOf_Type.useExistingInstance) {
        // Return eq fs instance if already created
        FeatureStructure fs = instanceOf_Type.jcas.getJfsFromCaddr(addr);
        if (null == fs) {
          fs = new LanguagePair(addr, instanceOf_Type);
          instanceOf_Type.jcas.putJfsFromCaddr(addr, fs);
          return fs;
        }
        return fs;
      } else
        return new LanguagePair(addr, instanceOf_Type);
    }
  };

  public final static int typeIndexID = LanguagePair.typeIndexID;

  public final static boolean featOkTst = org.apache.uima.jcas.JCasRegistry.getFeatOkTst("org.apache.lang.LanguagePair");

  final Feature casFeat_lang1;

  final int casFeatCode_lang1;

  public String getLang1(int addr) {
    if (featOkTst && casFeat_lang1 == null)
      this.jcas.throwFeatMissing("lang1", "org.apache.lang.LanguagePair");
    return ll_cas.ll_getStringValue(addr, casFeatCode_lang1);
  }

  public void setLang1(int addr, String v) {
    if (featOkTst && casFeat_lang1 == null)
      this.jcas.throwFeatMissing("lang1", "org.apache.lang.LanguagePair");
    ll_cas.ll_setStringValue(addr, casFeatCode_lang1, v);
  }

  final Feature casFeat_lang2;

  final int casFeatCode_lang2;

  public String getLang2(int addr) {
    if (featOkTst && casFeat_lang2 == null)
      this.jcas.throwFeatMissing("lang2", "org.apache.lang.LanguagePair");
    return ll_cas.ll_getStringValue(addr, casFeatCode_lang2);
  }

  public void setLang2(int addr, String v) {
    if (featOkTst && casFeat_lang2 == null)
      this.jcas.throwFeatMissing("lang2", "org.apache.lang.LanguagePair");
    ll_cas.ll_setStringValue(addr, casFeatCode_lang2, v);
  }

//  final Feature casFeat_description;
//
//  final int casFeatCode_description;
//
//  public String getDescription(int addr) {
//    if (featOkTst && casFeat_description == null)
//      this.jcas.throwFeatMissing("description", "org.apache.lang.LanguagePair");
//    return ll_cas.ll_getStringValue(addr, casFeatCode_description);
//  }
//
//  public void setDescription(int addr, String v) {
//    if (featOkTst && casFeat_description == null)
//      this.jcas.throwFeatMissing("description", "org.apache.lang.LanguagePair");
//    ll_cas.ll_setStringValue(addr, casFeatCode_description, v);
//  }

  // * initialize variables to correspond with Cas Type and Features
  public LanguagePair_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

    casFeat_lang1 = jcas
            .getRequiredFeatureDE(casType, "lang1", "org.apache.lang.Group1", featOkTst);
    casFeatCode_lang1 = (null == casFeat_lang1) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_lang1).getCode();
    casFeat_lang2 = jcas
            .getRequiredFeatureDE(casType, "lang2", "org.apache.lang.Group2", featOkTst);
    casFeatCode_lang2 = (null == casFeat_lang2) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_lang2).getCode();
//    casFeat_description = jcas.getRequiredFeatureDE(casType, "description", "uima.cas.String",
//            featOkTst);
//    casFeatCode_description = (null == casFeat_description) ? JCas.INVALID_FEATURE_CODE
//            : ((FeatureImpl) casFeat_description).getCode();
  }

  protected LanguagePair_Type() { // block default new operator
    casFeat_lang1 = null;
    casFeatCode_lang1 = JCas.INVALID_FEATURE_CODE;
    casFeat_lang2 = null;
    casFeatCode_lang2 = JCas.INVALID_FEATURE_CODE;
//    casFeat_description = null;
//    casFeatCode_description = JCas.INVALID_FEATURE_CODE;
    throw new RuntimeException("Internal Error-this constructor should never be called.");
  }

}
