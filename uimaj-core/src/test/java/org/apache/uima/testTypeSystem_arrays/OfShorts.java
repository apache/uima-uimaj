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


/* First created by JCasGen Wed May 23 14:54:19 EDT 2012 */
package org.apache.uima.testTypeSystem_arrays;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Wed May 23 14:55:00 EDT 2012
 * XML source: C:/au/svnCheckouts/trunks/uimaj/uimaj-core/src/test/resources/ExampleCas/testTypeSystem_arrays.xml
 * @generated */
public class OfShorts extends Annotation {
  /** @generated
   * @ordered 
   */
  public final static int typeIndexID = JCasRegistry.register(OfShorts.class);
  /** @generated
   * @ordered 
   */
  public final static int type = typeIndexID;
  /** @generated  */
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected OfShorts() {}
    
  /** Internal - constructor used by generator 
   * @generated */
  public OfShorts(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public OfShorts(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public OfShorts(JCas jcas, int begin, int end) {
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
  //* Feature: f1Shorts

  /** getter for f1Shorts - gets 
   * @generated */
  public ShortArray getF1Shorts() {
    if (OfShorts_Type.featOkTst && ((OfShorts_Type)jcasType).casFeat_f1Shorts == null)
      jcasType.jcas.throwFeatMissing("f1Shorts", "org.apache.uima.testTypeSystem_arrays.OfShorts");
    return (ShortArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((OfShorts_Type)jcasType).casFeatCode_f1Shorts)));}
    
  /** setter for f1Shorts - sets  
   * @generated */
  public void setF1Shorts(ShortArray v) {
    if (OfShorts_Type.featOkTst && ((OfShorts_Type)jcasType).casFeat_f1Shorts == null)
      jcasType.jcas.throwFeatMissing("f1Shorts", "org.apache.uima.testTypeSystem_arrays.OfShorts");
    jcasType.ll_cas.ll_setRefValue(addr, ((OfShorts_Type)jcasType).casFeatCode_f1Shorts, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for f1Shorts - gets an indexed value - 
   * @generated */
  public short getF1Shorts(int i) {
    if (OfShorts_Type.featOkTst && ((OfShorts_Type)jcasType).casFeat_f1Shorts == null)
      jcasType.jcas.throwFeatMissing("f1Shorts", "org.apache.uima.testTypeSystem_arrays.OfShorts");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((OfShorts_Type)jcasType).casFeatCode_f1Shorts), i);
    return jcasType.ll_cas.ll_getShortArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((OfShorts_Type)jcasType).casFeatCode_f1Shorts), i);}

  /** indexed setter for f1Shorts - sets an indexed value - 
   * @generated */
  public void setF1Shorts(int i, short v) { 
    if (OfShorts_Type.featOkTst && ((OfShorts_Type)jcasType).casFeat_f1Shorts == null)
      jcasType.jcas.throwFeatMissing("f1Shorts", "org.apache.uima.testTypeSystem_arrays.OfShorts");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((OfShorts_Type)jcasType).casFeatCode_f1Shorts), i);
    jcasType.ll_cas.ll_setShortArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((OfShorts_Type)jcasType).casFeatCode_f1Shorts), i, v);}
  }

    