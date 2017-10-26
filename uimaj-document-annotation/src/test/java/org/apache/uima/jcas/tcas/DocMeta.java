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


/* First created by JCasGen Mon Oct 02 16:39:47 EDT 2017 */
package org.apache.uima.jcas.tcas;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.IntegerArray;


/** 
 * Updated by JCasGen Mon Oct 02 16:39:47 EDT 2017
 * XML source: C:/au/svnCheckouts/trunk/uimaj-current/uimaj/uimaj-document-annotation/src/test/resources/ExampleCas/testTypeSystem_docmetadata.xml
 * @generated */
public class DocMeta extends DocumentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(DocMeta.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected DocMeta() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public DocMeta(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public DocMeta(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public DocMeta(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: feat

  /** getter for feat - gets 
   * @generated
   * @return value of the feature 
   */
  public String getFeat() {
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_feat == null)
      jcasType.jcas.throwFeatMissing("feat", "org.apache.uima.jcas.tcas.DocMeta");
    return jcasType.ll_cas.ll_getStringValue(addr, ((DocMeta_Type)jcasType).casFeatCode_feat);}
    
  /** setter for feat - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setFeat(String v) {
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_feat == null)
      jcasType.jcas.throwFeatMissing("feat", "org.apache.uima.jcas.tcas.DocMeta");
    jcasType.ll_cas.ll_setStringValue(addr, ((DocMeta_Type)jcasType).casFeatCode_feat, v);}    
   
    
  //*--------------*
  //* Feature: feat2

  /** getter for feat2 - gets 
   * @generated
   * @return value of the feature 
   */
  public String getFeat2() {
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_feat2 == null)
      jcasType.jcas.throwFeatMissing("feat2", "org.apache.uima.jcas.tcas.DocMeta");
    return jcasType.ll_cas.ll_getStringValue(addr, ((DocMeta_Type)jcasType).casFeatCode_feat2);}
    
  /** setter for feat2 - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setFeat2(String v) {
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_feat2 == null)
      jcasType.jcas.throwFeatMissing("feat2", "org.apache.uima.jcas.tcas.DocMeta");
    jcasType.ll_cas.ll_setStringValue(addr, ((DocMeta_Type)jcasType).casFeatCode_feat2, v);}    
   
    
  //*--------------*
  //* Feature: feat3

  /** getter for feat3 - gets 
   * @generated
   * @return value of the feature 
   */
  public String getFeat3() {
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_feat3 == null)
      jcasType.jcas.throwFeatMissing("feat3", "org.apache.uima.jcas.tcas.DocMeta");
    return jcasType.ll_cas.ll_getStringValue(addr, ((DocMeta_Type)jcasType).casFeatCode_feat3);}
    
  /** setter for feat3 - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setFeat3(String v) {
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_feat3 == null)
      jcasType.jcas.throwFeatMissing("feat3", "org.apache.uima.jcas.tcas.DocMeta");
    jcasType.ll_cas.ll_setStringValue(addr, ((DocMeta_Type)jcasType).casFeatCode_feat3, v);}    
   
    
  //*--------------*
  //* Feature: arraystr

  /** getter for arraystr - gets 
   * @generated
   * @return value of the feature 
   */
  public StringArray getArraystr() {
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_arraystr == null)
      jcasType.jcas.throwFeatMissing("arraystr", "org.apache.uima.jcas.tcas.DocMeta");
    return (StringArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((DocMeta_Type)jcasType).casFeatCode_arraystr)));}
    
  /** setter for arraystr - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArraystr(StringArray v) {
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_arraystr == null)
      jcasType.jcas.throwFeatMissing("arraystr", "org.apache.uima.jcas.tcas.DocMeta");
    jcasType.ll_cas.ll_setRefValue(addr, ((DocMeta_Type)jcasType).casFeatCode_arraystr, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for arraystr - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public String getArraystr(int i) {
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_arraystr == null)
      jcasType.jcas.throwFeatMissing("arraystr", "org.apache.uima.jcas.tcas.DocMeta");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((DocMeta_Type)jcasType).casFeatCode_arraystr), i);
    return jcasType.ll_cas.ll_getStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((DocMeta_Type)jcasType).casFeatCode_arraystr), i);}

  /** indexed setter for arraystr - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArraystr(int i, String v) { 
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_arraystr == null)
      jcasType.jcas.throwFeatMissing("arraystr", "org.apache.uima.jcas.tcas.DocMeta");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((DocMeta_Type)jcasType).casFeatCode_arraystr), i);
    jcasType.ll_cas.ll_setStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((DocMeta_Type)jcasType).casFeatCode_arraystr), i, v);}
   
    
  //*--------------*
  //* Feature: arrayints

  /** getter for arrayints - gets 
   * @generated
   * @return value of the feature 
   */
  public IntegerArray getArrayints() {
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_arrayints == null)
      jcasType.jcas.throwFeatMissing("arrayints", "org.apache.uima.jcas.tcas.DocMeta");
    return (IntegerArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((DocMeta_Type)jcasType).casFeatCode_arrayints)));}
    
  /** setter for arrayints - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayints(IntegerArray v) {
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_arrayints == null)
      jcasType.jcas.throwFeatMissing("arrayints", "org.apache.uima.jcas.tcas.DocMeta");
    jcasType.ll_cas.ll_setRefValue(addr, ((DocMeta_Type)jcasType).casFeatCode_arrayints, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for arrayints - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public int getArrayints(int i) {
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_arrayints == null)
      jcasType.jcas.throwFeatMissing("arrayints", "org.apache.uima.jcas.tcas.DocMeta");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((DocMeta_Type)jcasType).casFeatCode_arrayints), i);
    return jcasType.ll_cas.ll_getIntArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((DocMeta_Type)jcasType).casFeatCode_arrayints), i);}

  /** indexed setter for arrayints - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayints(int i, int v) { 
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_arrayints == null)
      jcasType.jcas.throwFeatMissing("arrayints", "org.apache.uima.jcas.tcas.DocMeta");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((DocMeta_Type)jcasType).casFeatCode_arrayints), i);
    jcasType.ll_cas.ll_setIntArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((DocMeta_Type)jcasType).casFeatCode_arrayints), i, v);}
   
    
  //*--------------*
  //* Feature: arrayFs

  /** getter for arrayFs - gets 
   * @generated
   * @return value of the feature 
   */
  public FSArray getArrayFs() {
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_arrayFs == null)
      jcasType.jcas.throwFeatMissing("arrayFs", "org.apache.uima.jcas.tcas.DocMeta");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((DocMeta_Type)jcasType).casFeatCode_arrayFs)));}
    
  /** setter for arrayFs - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArrayFs(FSArray v) {
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_arrayFs == null)
      jcasType.jcas.throwFeatMissing("arrayFs", "org.apache.uima.jcas.tcas.DocMeta");
    jcasType.ll_cas.ll_setRefValue(addr, ((DocMeta_Type)jcasType).casFeatCode_arrayFs, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for arrayFs - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public Annotation getArrayFs(int i) {
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_arrayFs == null)
      jcasType.jcas.throwFeatMissing("arrayFs", "org.apache.uima.jcas.tcas.DocMeta");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((DocMeta_Type)jcasType).casFeatCode_arrayFs), i);
    return (Annotation)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((DocMeta_Type)jcasType).casFeatCode_arrayFs), i)));}

  /** indexed setter for arrayFs - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArrayFs(int i, Annotation v) { 
    if (DocMeta_Type.featOkTst && ((DocMeta_Type)jcasType).casFeat_arrayFs == null)
      jcasType.jcas.throwFeatMissing("arrayFs", "org.apache.uima.jcas.tcas.DocMeta");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((DocMeta_Type)jcasType).casFeatCode_arrayFs), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((DocMeta_Type)jcasType).casFeatCode_arrayFs), i, jcasType.ll_cas.ll_getFSRef(v));}
  }

    