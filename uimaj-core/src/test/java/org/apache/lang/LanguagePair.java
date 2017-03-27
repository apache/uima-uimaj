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

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.cas.TOP_Type;

/* comment 7 of 14 */
public class LanguagePair extends TOP {

  public final static int typeIndexID = org.apache.uima.jcas.JCasRegistry.register(LanguagePair.class);

  public final static int type = typeIndexID;

  public int getTypeIndexID() {
    return typeIndexID;
  }

  // Never called. Disable default constructor
  protected LanguagePair() {
  }

 /* Internal - Constructor used by generator */
  public LanguagePair(int addr, TOP_Type type) {
    super(addr, type);
  }

  public LanguagePair(JCas jcas) {
    super(jcas);
  }

  /**
   * <!-- begin-user-doc --> Write your own initialization here <!-- end-user-doc -->
   * 
   * @generated modifiable
   */
  private void readObject() {
  }

  // *------------------*
  // * Feature: lang1
  /* getter for lang1 * */
  public String getLang1() {
    if (LanguagePair_Type.featOkTst && ((LanguagePair_Type) jcasType).casFeat_lang1 == null)
      this.jcasType.jcas.throwFeatMissing("lang1", "org.apache.lang.LanguagePair");
    return jcasType.ll_cas
            .ll_getStringValue(addr, ((LanguagePair_Type) jcasType).casFeatCode_lang1);
  }

  /* setter for lang1 * */
  public void setLang1(String v) {
    if (LanguagePair_Type.featOkTst && ((LanguagePair_Type) jcasType).casFeat_lang1 == null)
      this.jcasType.jcas.throwFeatMissing("lang1", "org.apache.lang.LanguagePair");
    jcasType.ll_cas.ll_setStringValue(addr, ((LanguagePair_Type) jcasType).casFeatCode_lang1, v);
  }

  // *------------------*
  // * Feature: lang2
  /* getter for lang2 * */
  public String getLang2() {
    if (LanguagePair_Type.featOkTst && ((LanguagePair_Type) jcasType).casFeat_lang2 == null)
      this.jcasType.jcas.throwFeatMissing("lang2", "org.apache.lang.LanguagePair");
    return jcasType.ll_cas
            .ll_getStringValue(addr, ((LanguagePair_Type) jcasType).casFeatCode_lang2);
  }

  /* setter for lang2 * */
  public void setLang2(String v) {
    if (LanguagePair_Type.featOkTst && ((LanguagePair_Type) jcasType).casFeat_lang2 == null)
      this.jcasType.jcas.throwFeatMissing("lang2", "org.apache.lang.LanguagePair");
    jcasType.ll_cas.ll_setStringValue(addr, ((LanguagePair_Type) jcasType).casFeatCode_lang2, v);
  }

//  // *------------------*
//  // * Feature: description
//  /* getter for description * */
//  public String getDescription() {
//    if (LanguagePair_Type.featOkTst && ((LanguagePair_Type) jcasType).casFeat_description == null)
//      this.jcasType.jcas.throwFeatMissing("description", "org.apache.lang.LanguagePair");
//    return jcasType.ll_cas.ll_getStringValue(addr,
//            ((LanguagePair_Type) jcasType).casFeatCode_description);
//  }
//
//  /* setter for description * */
//  public void setDescription(String v) {
//    if (LanguagePair_Type.featOkTst && ((LanguagePair_Type) jcasType).casFeat_description == null)
//      this.jcasType.jcas.throwFeatMissing("description", "org.apache.lang.LanguagePair");
//    jcasType.ll_cas.ll_setStringValue(addr, ((LanguagePair_Type) jcasType).casFeatCode_description,
//            v);
//  }
}
