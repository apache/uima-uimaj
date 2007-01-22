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



/* First created by JCasGen Fri Dec 22 14:02:31 CET 2006 */
package org.apache.uima.cas.test;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Fri Dec 22 14:02:31 CET 2006
 * XML source: C:/code/trunk/uimaj-core/src/test/resources/CASTests/desc/StringSubtypeTest.xml
 * @generated */
public class StringSubtypeAnnotation extends Annotation {
  /** @generated
   * @ordered 
   */
  public final static int typeIndexID = org.apache.uima.jcas.JCasRegistry.register(StringSubtypeAnnotation.class);
  /** @generated
   * @ordered 
   */
  public final static int type = typeIndexID;
  /** @generated  */
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected StringSubtypeAnnotation() {}
    
  /** Internal - constructor used by generator 
   * @generated */
  public StringSubtypeAnnotation(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public StringSubtypeAnnotation(JCas jcas) {
    super(jcas);
    readObject();   
  } 
  
  /** @generated */
  public StringSubtypeAnnotation(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** <!-- begin-user-doc -->
    * Write your own initialization here
    * <!-- end-user-doc -->
  @generated modifiable */
  private void readObject() {}
     
 
    
  //*--------------*
  //* Feature: stringSetFeature

  /** getter for stringSetFeature - gets 
   * @generated */
  public String getStringSetFeature() {
    if (StringSubtypeAnnotation_Type.featOkTst && ((StringSubtypeAnnotation_Type)jcasType).casFeat_stringSetFeature == null)
      this.jcasType.jcas.throwFeatMissing("stringSetFeature", "org.apache.uima.cas.test.StringSubtypeAnnotation");
    return jcasType.ll_cas.ll_getStringValue(addr, ((StringSubtypeAnnotation_Type)jcasType).casFeatCode_stringSetFeature);}
    
  /** setter for stringSetFeature - sets  
   * @generated */
  public void setStringSetFeature(String v) {
    if (StringSubtypeAnnotation_Type.featOkTst && ((StringSubtypeAnnotation_Type)jcasType).casFeat_stringSetFeature == null)
      this.jcasType.jcas.throwFeatMissing("stringSetFeature", "org.apache.uima.cas.test.StringSubtypeAnnotation");
    jcasType.ll_cas.ll_setStringValue(addr, ((StringSubtypeAnnotation_Type)jcasType).casFeatCode_stringSetFeature, v);}    
  }

    