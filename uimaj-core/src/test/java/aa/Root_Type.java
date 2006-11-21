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

import org.apache.uima.jcas.impl.JCas;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.cas.TOP_Type;

/**
 * Updated by JCasGen Tue Feb 21 14:56:04 EST 2006
 * 
 * @generated
 */
public class Root_Type extends TOP_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {
    return fsGenerator;
  }

  /** @generated */
  private final FSGenerator fsGenerator = new FSGenerator() {
    public FeatureStructure createFS(int addr, CASImpl cas) {
      if (Root_Type.this.useExistingInstance) {
        // Return eq fs instance if already created
        FeatureStructure fs = Root_Type.this.jcas.getJfsFromCaddr(addr);
        if (null == fs) {
          fs = new Root(addr, Root_Type.this);
          Root_Type.this.jcas.putJfsFromCaddr(addr, fs);
          return fs;
        }
        return fs;
      } else
        return new Root(addr, Root_Type.this);
    }
  };

  /** @generated */
  public final static int typeIndexID = Root.typeIndexID;

  /**
   * @generated
   * @modifiable
   */
  public final static boolean featOkTst = JCas.getFeatOkTst("aa.Root");

  /** @generated */
  final Feature casFeat_arrayInt;

  /** @generated */
  final int casFeatCode_arrayInt;

  /** @generated */
  public int getArrayInt(int addr) {
    if (featOkTst && casFeat_arrayInt == null)
      JCas.throwFeatMissing("arrayInt", "aa.Root");
    return ll_cas.ll_getRefValue(addr, casFeatCode_arrayInt);
  }

  /** @generated */
  public void setArrayInt(int addr, int v) {
    if (featOkTst && casFeat_arrayInt == null)
      JCas.throwFeatMissing("arrayInt", "aa.Root");
    ll_cas.ll_setRefValue(addr, casFeatCode_arrayInt, v);
  }

  /** @generated */
  public int getArrayInt(int addr, int i) {
    if (featOkTst && casFeat_arrayInt == null)
      JCas.throwFeatMissing("arrayInt", "aa.Root");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getIntArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayInt), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayInt), i);
    return ll_cas.ll_getIntArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayInt), i);
  }

  /** @generated */
  public void setArrayInt(int addr, int i, int v) {
    if (featOkTst && casFeat_arrayInt == null)
      JCas.throwFeatMissing("arrayInt", "aa.Root");
    if (lowLevelTypeChecks)
      ll_cas.ll_setIntArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayInt), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayInt), i);
    ll_cas.ll_setIntArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayInt), i, v);
  }

  /** @generated */
  final Feature casFeat_arrayRef;

  /** @generated */
  final int casFeatCode_arrayRef;

  /** @generated */
  public int getArrayRef(int addr) {
    if (featOkTst && casFeat_arrayRef == null)
      JCas.throwFeatMissing("arrayRef", "aa.Root");
    return ll_cas.ll_getRefValue(addr, casFeatCode_arrayRef);
  }

  /** @generated */
  public void setArrayRef(int addr, int v) {
    if (featOkTst && casFeat_arrayRef == null)
      JCas.throwFeatMissing("arrayRef", "aa.Root");
    ll_cas.ll_setRefValue(addr, casFeatCode_arrayRef, v);
  }

  /** @generated */
  public int getArrayRef(int addr, int i) {
    if (featOkTst && casFeat_arrayRef == null)
      JCas.throwFeatMissing("arrayRef", "aa.Root");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayRef), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayRef), i);
    return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayRef), i);
  }

  /** @generated */
  public void setArrayRef(int addr, int i, int v) {
    if (featOkTst && casFeat_arrayRef == null)
      JCas.throwFeatMissing("arrayRef", "aa.Root");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayRef), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayRef), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayRef), i, v);
  }

  /** @generated */
  final Feature casFeat_arrayFloat;

  /** @generated */
  final int casFeatCode_arrayFloat;

  /** @generated */
  public int getArrayFloat(int addr) {
    if (featOkTst && casFeat_arrayFloat == null)
      JCas.throwFeatMissing("arrayFloat", "aa.Root");
    return ll_cas.ll_getRefValue(addr, casFeatCode_arrayFloat);
  }

  /** @generated */
  public void setArrayFloat(int addr, int v) {
    if (featOkTst && casFeat_arrayFloat == null)
      JCas.throwFeatMissing("arrayFloat", "aa.Root");
    ll_cas.ll_setRefValue(addr, casFeatCode_arrayFloat, v);
  }

  /** @generated */
  public float getArrayFloat(int addr, int i) {
    if (featOkTst && casFeat_arrayFloat == null)
      JCas.throwFeatMissing("arrayFloat", "aa.Root");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getFloatArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayFloat), i,
                      true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayFloat), i);
    return ll_cas.ll_getFloatArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayFloat), i);
  }

  /** @generated */
  public void setArrayFloat(int addr, int i, float v) {
    if (featOkTst && casFeat_arrayFloat == null)
      JCas.throwFeatMissing("arrayFloat", "aa.Root");
    if (lowLevelTypeChecks)
      ll_cas.ll_setFloatArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayFloat), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayFloat), i);
    ll_cas.ll_setFloatArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayFloat), i, v);
  }

  /** @generated */
  final Feature casFeat_arrayString;

  /** @generated */
  final int casFeatCode_arrayString;

  /** @generated */
  public int getArrayString(int addr) {
    if (featOkTst && casFeat_arrayString == null)
      JCas.throwFeatMissing("arrayString", "aa.Root");
    return ll_cas.ll_getRefValue(addr, casFeatCode_arrayString);
  }

  /** @generated */
  public void setArrayString(int addr, int v) {
    if (featOkTst && casFeat_arrayString == null)
      JCas.throwFeatMissing("arrayString", "aa.Root");
    ll_cas.ll_setRefValue(addr, casFeatCode_arrayString, v);
  }

  /** @generated */
  public String getArrayString(int addr, int i) {
    if (featOkTst && casFeat_arrayString == null)
      JCas.throwFeatMissing("arrayString", "aa.Root");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayString), i,
                      true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayString), i);
    return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayString), i);
  }

  /** @generated */
  public void setArrayString(int addr, int i, String v) {
    if (featOkTst && casFeat_arrayString == null)
      JCas.throwFeatMissing("arrayString", "aa.Root");
    if (lowLevelTypeChecks)
      ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayString), i, v,
                      true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayString), i);
    ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayString), i, v);
  }

  /** @generated */
  final Feature casFeat_plainInt;

  /** @generated */
  final int casFeatCode_plainInt;

  /** @generated */
  public int getPlainInt(int addr) {
    if (featOkTst && casFeat_plainInt == null)
      JCas.throwFeatMissing("plainInt", "aa.Root");
    return ll_cas.ll_getIntValue(addr, casFeatCode_plainInt);
  }

  /** @generated */
  public void setPlainInt(int addr, int v) {
    if (featOkTst && casFeat_plainInt == null)
      JCas.throwFeatMissing("plainInt", "aa.Root");
    ll_cas.ll_setIntValue(addr, casFeatCode_plainInt, v);
  }

  /** @generated */
  final Feature casFeat_plainFloat;

  /** @generated */
  final int casFeatCode_plainFloat;

  /** @generated */
  public float getPlainFloat(int addr) {
    if (featOkTst && casFeat_plainFloat == null)
      JCas.throwFeatMissing("plainFloat", "aa.Root");
    return ll_cas.ll_getFloatValue(addr, casFeatCode_plainFloat);
  }

  /** @generated */
  public void setPlainFloat(int addr, float v) {
    if (featOkTst && casFeat_plainFloat == null)
      JCas.throwFeatMissing("plainFloat", "aa.Root");
    ll_cas.ll_setFloatValue(addr, casFeatCode_plainFloat, v);
  }

  /** @generated */
  final Feature casFeat_plainString;

  /** @generated */
  final int casFeatCode_plainString;

  /** @generated */
  public String getPlainString(int addr) {
    if (featOkTst && casFeat_plainString == null)
      JCas.throwFeatMissing("plainString", "aa.Root");
    return ll_cas.ll_getStringValue(addr, casFeatCode_plainString);
  }

  /** @generated */
  public void setPlainString(int addr, String v) {
    if (featOkTst && casFeat_plainString == null)
      JCas.throwFeatMissing("plainString", "aa.Root");
    ll_cas.ll_setStringValue(addr, casFeatCode_plainString, v);
  }

  /** @generated */
  final Feature casFeat_plainRef;

  /** @generated */
  final int casFeatCode_plainRef;

  /** @generated */
  public int getPlainRef(int addr) {
    if (featOkTst && casFeat_plainRef == null)
      JCas.throwFeatMissing("plainRef", "aa.Root");
    return ll_cas.ll_getRefValue(addr, casFeatCode_plainRef);
  }

  /** @generated */
  public void setPlainRef(int addr, int v) {
    if (featOkTst && casFeat_plainRef == null)
      JCas.throwFeatMissing("plainRef", "aa.Root");
    ll_cas.ll_setRefValue(addr, casFeatCode_plainRef, v);
  }

  /** @generated */
  final Feature casFeat_concreteString;

  /** @generated */
  final int casFeatCode_concreteString;

  /** @generated */
  public String getConcreteString(int addr) {
    if (featOkTst && casFeat_concreteString == null)
      JCas.throwFeatMissing("concreteString", "aa.Root");
    return ll_cas.ll_getStringValue(addr, casFeatCode_concreteString);
  }

  /** @generated */
  public void setConcreteString(int addr, String v) {
    if (featOkTst && casFeat_concreteString == null)
      JCas.throwFeatMissing("concreteString", "aa.Root");
    ll_cas.ll_setStringValue(addr, casFeatCode_concreteString, v);
  }

  /**
   * initialize variables to correspond with Cas Type and Features
   * 
   * @generated
   */
  public Root_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

    casFeat_arrayInt = jcas.getRequiredFeatureDE(casType, "arrayInt", "uima.cas.IntegerArray",
                    featOkTst);
    casFeatCode_arrayInt = (null == casFeat_arrayInt) ? JCas.INVALID_FEATURE_CODE
                    : ((FeatureImpl) casFeat_arrayInt).getCode();

    casFeat_arrayRef = jcas
                    .getRequiredFeatureDE(casType, "arrayRef", "uima.cas.FSArray", featOkTst);
    casFeatCode_arrayRef = (null == casFeat_arrayRef) ? JCas.INVALID_FEATURE_CODE
                    : ((FeatureImpl) casFeat_arrayRef).getCode();

    casFeat_arrayFloat = jcas.getRequiredFeatureDE(casType, "arrayFloat", "uima.cas.FloatArray",
                    featOkTst);
    casFeatCode_arrayFloat = (null == casFeat_arrayFloat) ? JCas.INVALID_FEATURE_CODE
                    : ((FeatureImpl) casFeat_arrayFloat).getCode();

    casFeat_arrayString = jcas.getRequiredFeatureDE(casType, "arrayString", "uima.cas.StringArray",
                    featOkTst);
    casFeatCode_arrayString = (null == casFeat_arrayString) ? JCas.INVALID_FEATURE_CODE
                    : ((FeatureImpl) casFeat_arrayString).getCode();

    casFeat_plainInt = jcas
                    .getRequiredFeatureDE(casType, "plainInt", "uima.cas.Integer", featOkTst);
    casFeatCode_plainInt = (null == casFeat_plainInt) ? JCas.INVALID_FEATURE_CODE
                    : ((FeatureImpl) casFeat_plainInt).getCode();

    casFeat_plainFloat = jcas.getRequiredFeatureDE(casType, "plainFloat", "uima.cas.Float",
                    featOkTst);
    casFeatCode_plainFloat = (null == casFeat_plainFloat) ? JCas.INVALID_FEATURE_CODE
                    : ((FeatureImpl) casFeat_plainFloat).getCode();

    casFeat_plainString = jcas.getRequiredFeatureDE(casType, "plainString", "uima.cas.String",
                    featOkTst);
    casFeatCode_plainString = (null == casFeat_plainString) ? JCas.INVALID_FEATURE_CODE
                    : ((FeatureImpl) casFeat_plainString).getCode();

    casFeat_plainRef = jcas.getRequiredFeatureDE(casType, "plainRef", "aa.Root", featOkTst);
    casFeatCode_plainRef = (null == casFeat_plainRef) ? JCas.INVALID_FEATURE_CODE
                    : ((FeatureImpl) casFeat_plainRef).getCode();

    casFeat_concreteString = jcas.getRequiredFeatureDE(casType, "concreteString",
                    "uima.cas.String", featOkTst);
    casFeatCode_concreteString = (null == casFeat_concreteString) ? JCas.INVALID_FEATURE_CODE
                    : ((FeatureImpl) casFeat_concreteString).getCode();

  }
}
