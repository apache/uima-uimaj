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

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** 
 * Updated by JCasGen Wed May 23 14:55:02 EDT 2012
 * @generated */
public class OfStrings_Type extends Annotation_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (OfStrings_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = OfStrings_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new OfStrings(addr, OfStrings_Type.this);
  			   OfStrings_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new OfStrings(addr, OfStrings_Type.this);
  	  }
    };
  /** @generated */
  public final static int typeIndexID = OfStrings.typeIndexID;
  /** @generated 
     @modifiable */
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("org.apache.uima.testTypeSystem_arrays.OfStrings");
 
  /** @generated */
  final Feature casFeat_f1Strings;
  /** @generated */
  final int     casFeatCode_f1Strings;
  /** @generated */ 
  public int getF1Strings(int addr) {
        if (featOkTst && casFeat_f1Strings == null)
      jcas.throwFeatMissing("f1Strings", "org.apache.uima.testTypeSystem_arrays.OfStrings");
    return ll_cas.ll_getRefValue(addr, casFeatCode_f1Strings);
  }
  /** @generated */    
  public void setF1Strings(int addr, int v) {
        if (featOkTst && casFeat_f1Strings == null)
      jcas.throwFeatMissing("f1Strings", "org.apache.uima.testTypeSystem_arrays.OfStrings");
    ll_cas.ll_setRefValue(addr, casFeatCode_f1Strings, v);}
    
   /** @generated */
  public String getF1Strings(int addr, int i) {
        if (featOkTst && casFeat_f1Strings == null)
      jcas.throwFeatMissing("f1Strings", "org.apache.uima.testTypeSystem_arrays.OfStrings");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_f1Strings), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_f1Strings), i);
  return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_f1Strings), i);
  }
   
  /** @generated */ 
  public void setF1Strings(int addr, int i, String v) {
        if (featOkTst && casFeat_f1Strings == null)
      jcas.throwFeatMissing("f1Strings", "org.apache.uima.testTypeSystem_arrays.OfStrings");
    if (lowLevelTypeChecks)
      ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_f1Strings), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_f1Strings), i);
    ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_f1Strings), i, v);
  }
 



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public OfStrings_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_f1Strings = jcas.getRequiredFeatureDE(casType, "f1Strings", "uima.cas.StringArray", featOkTst);
    casFeatCode_f1Strings  = (null == casFeat_f1Strings) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_f1Strings).getCode();

  }
}



    