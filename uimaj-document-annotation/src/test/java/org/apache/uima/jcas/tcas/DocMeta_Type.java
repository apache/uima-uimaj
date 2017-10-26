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
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** 
 * Updated by JCasGen Mon Oct 02 16:39:47 EDT 2017
 * @generated */
public class DocMeta_Type extends DocumentAnnotation_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = DocMeta.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("org.apache.uima.jcas.tcas.DocMeta");
 
  /** @generated */
  final Feature casFeat_feat;
  /** @generated */
  final int     casFeatCode_feat;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getFeat(int addr) {
        if (featOkTst && casFeat_feat == null)
      jcas.throwFeatMissing("feat", "org.apache.uima.jcas.tcas.DocMeta");
    return ll_cas.ll_getStringValue(addr, casFeatCode_feat);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setFeat(int addr, String v) {
        if (featOkTst && casFeat_feat == null)
      jcas.throwFeatMissing("feat", "org.apache.uima.jcas.tcas.DocMeta");
    ll_cas.ll_setStringValue(addr, casFeatCode_feat, v);}
    
  
 
  /** @generated */
  final Feature casFeat_feat2;
  /** @generated */
  final int     casFeatCode_feat2;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getFeat2(int addr) {
        if (featOkTst && casFeat_feat2 == null)
      jcas.throwFeatMissing("feat2", "org.apache.uima.jcas.tcas.DocMeta");
    return ll_cas.ll_getStringValue(addr, casFeatCode_feat2);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setFeat2(int addr, String v) {
        if (featOkTst && casFeat_feat2 == null)
      jcas.throwFeatMissing("feat2", "org.apache.uima.jcas.tcas.DocMeta");
    ll_cas.ll_setStringValue(addr, casFeatCode_feat2, v);}
    
  
 
  /** @generated */
  final Feature casFeat_feat3;
  /** @generated */
  final int     casFeatCode_feat3;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getFeat3(int addr) {
        if (featOkTst && casFeat_feat3 == null)
      jcas.throwFeatMissing("feat3", "org.apache.uima.jcas.tcas.DocMeta");
    return ll_cas.ll_getStringValue(addr, casFeatCode_feat3);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setFeat3(int addr, String v) {
        if (featOkTst && casFeat_feat3 == null)
      jcas.throwFeatMissing("feat3", "org.apache.uima.jcas.tcas.DocMeta");
    ll_cas.ll_setStringValue(addr, casFeatCode_feat3, v);}
    
  
 
  /** @generated */
  final Feature casFeat_arraystr;
  /** @generated */
  final int     casFeatCode_arraystr;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getArraystr(int addr) {
        if (featOkTst && casFeat_arraystr == null)
      jcas.throwFeatMissing("arraystr", "org.apache.uima.jcas.tcas.DocMeta");
    return ll_cas.ll_getRefValue(addr, casFeatCode_arraystr);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArraystr(int addr, int v) {
        if (featOkTst && casFeat_arraystr == null)
      jcas.throwFeatMissing("arraystr", "org.apache.uima.jcas.tcas.DocMeta");
    ll_cas.ll_setRefValue(addr, casFeatCode_arraystr, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public String getArraystr(int addr, int i) {
        if (featOkTst && casFeat_arraystr == null)
      jcas.throwFeatMissing("arraystr", "org.apache.uima.jcas.tcas.DocMeta");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arraystr), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arraystr), i);
	return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arraystr), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setArraystr(int addr, int i, String v) {
        if (featOkTst && casFeat_arraystr == null)
      jcas.throwFeatMissing("arraystr", "org.apache.uima.jcas.tcas.DocMeta");
    if (lowLevelTypeChecks)
      ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arraystr), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arraystr), i);
    ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arraystr), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_arrayints;
  /** @generated */
  final int     casFeatCode_arrayints;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getArrayints(int addr) {
        if (featOkTst && casFeat_arrayints == null)
      jcas.throwFeatMissing("arrayints", "org.apache.uima.jcas.tcas.DocMeta");
    return ll_cas.ll_getRefValue(addr, casFeatCode_arrayints);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArrayints(int addr, int v) {
        if (featOkTst && casFeat_arrayints == null)
      jcas.throwFeatMissing("arrayints", "org.apache.uima.jcas.tcas.DocMeta");
    ll_cas.ll_setRefValue(addr, casFeatCode_arrayints, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getArrayints(int addr, int i) {
        if (featOkTst && casFeat_arrayints == null)
      jcas.throwFeatMissing("arrayints", "org.apache.uima.jcas.tcas.DocMeta");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getIntArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayints), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayints), i);
	return ll_cas.ll_getIntArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayints), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setArrayints(int addr, int i, int v) {
        if (featOkTst && casFeat_arrayints == null)
      jcas.throwFeatMissing("arrayints", "org.apache.uima.jcas.tcas.DocMeta");
    if (lowLevelTypeChecks)
      ll_cas.ll_setIntArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayints), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayints), i);
    ll_cas.ll_setIntArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayints), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_arrayFs;
  /** @generated */
  final int     casFeatCode_arrayFs;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getArrayFs(int addr) {
        if (featOkTst && casFeat_arrayFs == null)
      jcas.throwFeatMissing("arrayFs", "org.apache.uima.jcas.tcas.DocMeta");
    return ll_cas.ll_getRefValue(addr, casFeatCode_arrayFs);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArrayFs(int addr, int v) {
        if (featOkTst && casFeat_arrayFs == null)
      jcas.throwFeatMissing("arrayFs", "org.apache.uima.jcas.tcas.DocMeta");
    ll_cas.ll_setRefValue(addr, casFeatCode_arrayFs, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getArrayFs(int addr, int i) {
        if (featOkTst && casFeat_arrayFs == null)
      jcas.throwFeatMissing("arrayFs", "org.apache.uima.jcas.tcas.DocMeta");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayFs), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayFs), i);
	return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayFs), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setArrayFs(int addr, int i, int v) {
        if (featOkTst && casFeat_arrayFs == null)
      jcas.throwFeatMissing("arrayFs", "org.apache.uima.jcas.tcas.DocMeta");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayFs), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_arrayFs), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_arrayFs), i, v);
  }
 



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public DocMeta_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_feat = jcas.getRequiredFeatureDE(casType, "feat", "uima.cas.String", featOkTst);
    casFeatCode_feat  = (null == casFeat_feat) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_feat).getCode();

 
    casFeat_feat2 = jcas.getRequiredFeatureDE(casType, "feat2", "uima.cas.String", featOkTst);
    casFeatCode_feat2  = (null == casFeat_feat2) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_feat2).getCode();

 
    casFeat_feat3 = jcas.getRequiredFeatureDE(casType, "feat3", "uima.cas.String", featOkTst);
    casFeatCode_feat3  = (null == casFeat_feat3) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_feat3).getCode();

 
    casFeat_arraystr = jcas.getRequiredFeatureDE(casType, "arraystr", "uima.cas.StringArray", featOkTst);
    casFeatCode_arraystr  = (null == casFeat_arraystr) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_arraystr).getCode();

 
    casFeat_arrayints = jcas.getRequiredFeatureDE(casType, "arrayints", "uima.cas.IntegerArray", featOkTst);
    casFeatCode_arrayints  = (null == casFeat_arrayints) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_arrayints).getCode();

 
    casFeat_arrayFs = jcas.getRequiredFeatureDE(casType, "arrayFs", "uima.cas.FSArray", featOkTst);
    casFeatCode_arrayFs  = (null == casFeat_arrayFs) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_arrayFs).getCode();

  }
}



    