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


/* First created by JCasGen Mon Dec 18 11:22:10 CET 2006 */
package org.apache.uima.cas.test;

import org.apache.uima.jcas.impl.JCas;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** 
 * Updated by JCasGen Mon Dec 18 11:22:10 CET 2006
 * @generated */
public class TestAnnotation_Type extends Annotation_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (TestAnnotation_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = TestAnnotation_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new TestAnnotation(addr, TestAnnotation_Type.this);
  			   TestAnnotation_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new TestAnnotation(addr, TestAnnotation_Type.this);
  	  }
    };
  /** @generated */
  public final static int typeIndexID = TestAnnotation.typeIndexID;
  /** @generated 
     @modifiable */
  public final static boolean featOkTst = JCas.getFeatOkTst("org.apache.uima.cas.test.TestAnnotation");
 
  /** @generated */
  final Feature casFeat_stringSetFeature;
  /** @generated */
  final int     casFeatCode_stringSetFeature;
  /** @generated */ 
  public String getStringSetFeature(int addr) {
        if (featOkTst && casFeat_stringSetFeature == null)
      JCas.throwFeatMissing("stringSetFeature", "org.apache.uima.cas.test.TestAnnotation");
    return ll_cas.ll_getStringValue(addr, casFeatCode_stringSetFeature);
  }
  /** @generated */    
  public void setStringSetFeature(int addr, String v) {
        if (featOkTst && casFeat_stringSetFeature == null)
      JCas.throwFeatMissing("stringSetFeature", "org.apache.uima.cas.test.TestAnnotation");
    ll_cas.ll_setStringValue(addr, casFeatCode_stringSetFeature, v);}
    
  


  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public TestAnnotation_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_stringSetFeature = jcas.getRequiredFeatureDE(casType, "stringSetFeature", "org.apache.uima.cas.test.StringSubtype", featOkTst);
    casFeatCode_stringSetFeature  = (null == casFeat_stringSetFeature) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_stringSetFeature).getCode();

  }
}



    