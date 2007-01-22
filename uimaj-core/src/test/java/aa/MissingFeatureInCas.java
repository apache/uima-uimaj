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

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.cas.TOP_Type;

/**
 * Updated by JCasGen Tue Feb 21 14:56:04 EST 2006 XML source:
 * C:/a/Eclipse/3.1/j4/jedii_jcas_tests/testTypes.xml
 * 
 * @generated
 */
public class MissingFeatureInCas extends TOP {
  /**
   * @generated
   * @ordered
   */
  public final static int typeIndexID = org.apache.uima.jcas.JCasRegistry.register(MissingFeatureInCas.class);

  /**
   * @generated
   * @ordered
   */
  public final static int type = typeIndexID;

  /** @generated */
  public int getTypeIndexID() {
    return typeIndexID;
  }

  /**
   * Never called. Disable default constructor
   * 
   * @generated
   */
  protected MissingFeatureInCas() {
  }

  /**
   * Internal - constructor used by generator
   * 
   * @generated
   */
  public MissingFeatureInCas(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }

  /** @generated */
  public MissingFeatureInCas(JCas jcas) {
    super(jcas);
    readObject();
  }

  /**
   * <!-- begin-user-doc --> Write your own initialization here <!-- end-user-doc -->
   * 
   * @generated modifiable
   */
  private void readObject() {
  }

  // *--------------*
  // * Feature: haveThisOne

  /**
   * getter for haveThisOne - gets
   * 
   * @generated
   */
  public int getHaveThisOne() {
    if (MissingFeatureInCas_Type.featOkTst
            && ((MissingFeatureInCas_Type) jcasType).casFeat_haveThisOne == null)
      this.jcasType.jcas.throwFeatMissing("haveThisOne", "aa.MissingFeatureInCas");
    return jcasType.ll_cas.ll_getIntValue(addr,
            ((MissingFeatureInCas_Type) jcasType).casFeatCode_haveThisOne);
  }

  /**
   * setter for haveThisOne - sets
   * 
   * @generated
   */
  public void setHaveThisOne(int v) {
    if (MissingFeatureInCas_Type.featOkTst
            && ((MissingFeatureInCas_Type) jcasType).casFeat_haveThisOne == null)
      this.jcasType.jcas.throwFeatMissing("haveThisOne", "aa.MissingFeatureInCas");
    jcasType.ll_cas.ll_setIntValue(addr,
            ((MissingFeatureInCas_Type) jcasType).casFeatCode_haveThisOne, v);
  }

  // *--------------*
  // * Feature: missingThisOne

  /**
   * getter for missingThisOne - gets
   * 
   * @generated
   */
  public float getMissingThisOne() {
    if (MissingFeatureInCas_Type.featOkTst
            && ((MissingFeatureInCas_Type) jcasType).casFeat_missingThisOne == null)
      this.jcasType.jcas.throwFeatMissing("missingThisOne", "aa.MissingFeatureInCas");
    return jcasType.ll_cas.ll_getFloatValue(addr,
            ((MissingFeatureInCas_Type) jcasType).casFeatCode_missingThisOne);
  }

  /**
   * setter for missingThisOne - sets
   * 
   * @generated
   */
  public void setMissingThisOne(float v) {
    if (MissingFeatureInCas_Type.featOkTst
            && ((MissingFeatureInCas_Type) jcasType).casFeat_missingThisOne == null)
      this.jcasType.jcas.throwFeatMissing("missingThisOne", "aa.MissingFeatureInCas");
    jcasType.ll_cas.ll_setFloatValue(addr,
            ((MissingFeatureInCas_Type) jcasType).casFeatCode_missingThisOne, v);
  }

  // *--------------*
  // * Feature: changedFType

  /**
   * getter for changedFType - gets
   * 
   * @generated
   */
  public String getChangedFType() {
    if (MissingFeatureInCas_Type.featOkTst
            && ((MissingFeatureInCas_Type) jcasType).casFeat_changedFType == null)
      this.jcasType.jcas.throwFeatMissing("changedFType", "aa.MissingFeatureInCas");
    return jcasType.ll_cas.ll_getStringValue(addr,
            ((MissingFeatureInCas_Type) jcasType).casFeatCode_changedFType);
  }

  /**
   * setter for changedFType - sets
   * 
   * @generated
   */
  public void setChangedFType(String v) {
    if (MissingFeatureInCas_Type.featOkTst
            && ((MissingFeatureInCas_Type) jcasType).casFeat_changedFType == null)
      this.jcasType.jcas.throwFeatMissing("changedFType", "aa.MissingFeatureInCas");
    jcasType.ll_cas.ll_setStringValue(addr,
            ((MissingFeatureInCas_Type) jcasType).casFeatCode_changedFType, v);
  }
}
