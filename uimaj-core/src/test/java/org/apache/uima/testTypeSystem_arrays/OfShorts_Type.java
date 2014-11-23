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
public class OfShorts_Type extends Annotation_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (OfShorts_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = OfShorts_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new OfShorts(addr, OfShorts_Type.this);
  			   OfShorts_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new OfShorts(addr, OfShorts_Type.this);
  	  }
    };
  /** @generated */
  public final static int typeIndexID = OfShorts.typeIndexID;
  /** @generated 
     @modifiable */
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("org.apache.uima.testTypeSystem_arrays.OfShorts");
 
  /** @generated */
  final Feature casFeat_f1Shorts;
  /** @generated */
  final int     casFeatCode_f1Shorts;
  /** @generated */ 
  public int getF1Shorts(int addr) {
        if (featOkTst && casFeat_f1Shorts == null)
      jcas.throwFeatMissing("f1Shorts", "org.apache.uima.testTypeSystem_arrays.OfShorts");
    return ll_cas.ll_getRefValue(addr, casFeatCode_f1Shorts);
  }
  /** @generated */    
  public void setF1Shorts(int addr, int v) {
        if (featOkTst && casFeat_f1Shorts == null)
      jcas.throwFeatMissing("f1Shorts", "org.apache.uima.testTypeSystem_arrays.OfShorts");
    ll_cas.ll_setRefValue(addr, casFeatCode_f1Shorts, v);}
    
   /** @generated */
  public short getF1Shorts(int addr, int i) {
        if (featOkTst && casFeat_f1Shorts == null)
      jcas.throwFeatMissing("f1Shorts", "org.apache.uima.testTypeSystem_arrays.OfShorts");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getShortArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_f1Shorts), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_f1Shorts), i);
  return ll_cas.ll_getShortArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_f1Shorts), i);
  }
   
  /** @generated */ 
  public void setF1Shorts(int addr, int i, short v) {
        if (featOkTst && casFeat_f1Shorts == null)
      jcas.throwFeatMissing("f1Shorts", "org.apache.uima.testTypeSystem_arrays.OfShorts");
    if (lowLevelTypeChecks)
      ll_cas.ll_setShortArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_f1Shorts), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_f1Shorts), i);
    ll_cas.ll_setShortArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_f1Shorts), i, v);
  }
 



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public OfShorts_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_f1Shorts = jcas.getRequiredFeatureDE(casType, "f1Shorts", "uima.cas.ShortArray", featOkTst);
    casFeatCode_f1Shorts  = (null == casFeat_f1Shorts) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_f1Shorts).getCode();

  }
}



    