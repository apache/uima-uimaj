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

package aa;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP_Type;

/**
 * Updated by JCasGen Tue Feb 21 14:56:04 EST 2006
 * 
 * @generated
 */
public class MissingFeatureInCas_Type extends TOP_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {
    return fsGenerator;
  }

  /** @generated */
  private final FSGenerator fsGenerator = new FSGenerator() {
    public FeatureStructure createFS(int addr, CASImpl cas) {
      if (MissingFeatureInCas_Type.this.useExistingInstance) {
        // Return eq fs instance if already created
        FeatureStructure fs = MissingFeatureInCas_Type.this.jcas.getJfsFromCaddr(addr);
        if (null == fs) {
          fs = new MissingFeatureInCas(addr, MissingFeatureInCas_Type.this);
          MissingFeatureInCas_Type.this.jcas.putJfsFromCaddr(addr, fs);
          return fs;
        }
        return fs;
      } else
        return new MissingFeatureInCas(addr, MissingFeatureInCas_Type.this);
    }
  };

  /** @generated */
  public final static int typeIndexID = MissingFeatureInCas.typeIndexID;

  /**
   * @generated
   * @modifiable
   */
  public final static boolean featOkTst = org.apache.uima.jcas.JCasRegistry.getFeatOkTst("aa.MissingFeatureInCas");

  /** @generated */
  final Feature casFeat_haveThisOne;

  /** @generated */
  final int casFeatCode_haveThisOne;

  /** @generated */
  public int getHaveThisOne(int addr) {
    if (featOkTst && casFeat_haveThisOne == null)
      this.jcas.throwFeatMissing("haveThisOne", "aa.MissingFeatureInCas");
    return ll_cas.ll_getIntValue(addr, casFeatCode_haveThisOne);
  }

  /** @generated */
  public void setHaveThisOne(int addr, int v) {
    if (featOkTst && casFeat_haveThisOne == null)
      this.jcas.throwFeatMissing("haveThisOne", "aa.MissingFeatureInCas");
    ll_cas.ll_setIntValue(addr, casFeatCode_haveThisOne, v);
  }

  /** @generated */
  final Feature casFeat_missingThisOne;

  /** @generated */
  final int casFeatCode_missingThisOne;

  /** @generated */
  public float getMissingThisOne(int addr) {
    if (featOkTst && casFeat_missingThisOne == null)
      this.jcas.throwFeatMissing("missingThisOne", "aa.MissingFeatureInCas");
    return ll_cas.ll_getFloatValue(addr, casFeatCode_missingThisOne);
  }

  /** @generated */
  public void setMissingThisOne(int addr, float v) {
    if (featOkTst && casFeat_missingThisOne == null)
      this.jcas.throwFeatMissing("missingThisOne", "aa.MissingFeatureInCas");
    ll_cas.ll_setFloatValue(addr, casFeatCode_missingThisOne, v);
  }

  /** @generated */
  final Feature casFeat_changedFType;

  /** @generated */
  final int casFeatCode_changedFType;

  /** @generated */
  public String getChangedFType(int addr) {
    if (featOkTst && casFeat_changedFType == null)
      this.jcas.throwFeatMissing("changedFType", "aa.MissingFeatureInCas");
    return ll_cas.ll_getStringValue(addr, casFeatCode_changedFType);
  }

  /** @generated */
  public void setChangedFType(int addr, String v) {
    if (featOkTst && casFeat_changedFType == null)
      this.jcas.throwFeatMissing("changedFType", "aa.MissingFeatureInCas");
    ll_cas.ll_setStringValue(addr, casFeatCode_changedFType, v);
  }

  /**
   * initialize variables to correspond with Cas Type and Features
   * 
   * @generated
   */
  public MissingFeatureInCas_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

    casFeat_haveThisOne = jcas.getRequiredFeatureDE(casType, "haveThisOne", "uima.cas.Integer",
            featOkTst);
    casFeatCode_haveThisOne = (null == casFeat_haveThisOne) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_haveThisOne).getCode();

    casFeat_missingThisOne = jcas.getRequiredFeatureDE(casType, "missingThisOne", "uima.cas.Float",
            featOkTst);
    casFeatCode_missingThisOne = (null == casFeat_missingThisOne) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_missingThisOne).getCode();

    casFeat_changedFType = jcas.getRequiredFeatureDE(casType, "changedFType", "uima.cas.String",
            featOkTst);
    casFeatCode_changedFType = (null == casFeat_changedFType) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_changedFType).getCode();

  }
}
