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

package org.apache.uima.klt;

import org.apache.uima.jcas.impl.JCas;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** An annotation of a span of text that refers to some relation
 * Updated by JCasGen Thu Apr 21 11:20:08 EDT 2005
 * @generated */
public class RelationAnnotation_Type extends Annotation_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (instanceOf_Type.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = instanceOf_Type.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new RelationAnnotation(addr, instanceOf_Type);
  			   instanceOf_Type.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new RelationAnnotation(addr, instanceOf_Type);
  	  }
    };
  /** @generated */
  public final static int typeIndexID = RelationAnnotation.typeIndexID;
  /** @generated 
     @modifiable */
  public final static boolean featOkTst = JCas.getFeatOkTst("org.apache.uima.klt.RelationAnnotation");
 
  /** @generated */
  final Feature casFeat_links;
  /** @generated */
  final int     casFeatCode_links;
  /** @generated */ 
  public int getLinks(int addr) {
        if (featOkTst && casFeat_links == null)
      JCas.throwFeatMissing("links", "org.apache.uima.klt.RelationAnnotation");
    return ll_cas.ll_getRefValue(addr, casFeatCode_links);
  }
  /** @generated */    
  public void setLinks(int addr, int v) {
        if (featOkTst && casFeat_links == null)
      JCas.throwFeatMissing("links", "org.apache.uima.klt.RelationAnnotation");
    ll_cas.ll_setRefValue(addr, casFeatCode_links, v);}
    
  
 
  /** @generated */
  final Feature casFeat_predicate;
  /** @generated */
  final int     casFeatCode_predicate;
  /** @generated */ 
  public int getPredicate(int addr) {
        if (featOkTst && casFeat_predicate == null)
      JCas.throwFeatMissing("predicate", "org.apache.uima.klt.RelationAnnotation");
    return ll_cas.ll_getRefValue(addr, casFeatCode_predicate);
  }
  /** @generated */    
  public void setPredicate(int addr, int v) {
        if (featOkTst && casFeat_predicate == null)
      JCas.throwFeatMissing("predicate", "org.apache.uima.klt.RelationAnnotation");
    ll_cas.ll_setRefValue(addr, casFeatCode_predicate, v);}
    
  
 
  /** @generated */
  final Feature casFeat_relationArgs;
  /** @generated */
  final int     casFeatCode_relationArgs;
  /** @generated */ 
  public int getRelationArgs(int addr) {
        if (featOkTst && casFeat_relationArgs == null)
      JCas.throwFeatMissing("relationArgs", "org.apache.uima.klt.RelationAnnotation");
    return ll_cas.ll_getRefValue(addr, casFeatCode_relationArgs);
  }
  /** @generated */    
  public void setRelationArgs(int addr, int v) {
        if (featOkTst && casFeat_relationArgs == null)
      JCas.throwFeatMissing("relationArgs", "org.apache.uima.klt.RelationAnnotation");
    ll_cas.ll_setRefValue(addr, casFeatCode_relationArgs, v);}
    
  


  /** @generated */
  final Feature casFeat_componentId;
  /** @generated */
  final int     casFeatCode_componentId;
  /** @generated */ 
  public String getComponentId(int addr) {
        if (featOkTst && casFeat_componentId == null)
      JCas.throwFeatMissing("componentId", "org.apache.uima.klt.RelationAnnotation");
    return ll_cas.ll_getStringValue(addr, casFeatCode_componentId);
  }
  /** @generated */    
  public void setComponentId(int addr, String v) {
        if (featOkTst && casFeat_componentId == null)
      JCas.throwFeatMissing("componentId", "org.apache.uima.klt.RelationAnnotation");
    ll_cas.ll_setStringValue(addr, casFeatCode_componentId, v);}
    
  
 
  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public RelationAnnotation_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_links = jcas.getRequiredFeatureDE(casType, "links", "uima.cas.FSList", featOkTst);
    casFeatCode_links  = (null == casFeat_links) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_links).getCode();

 
    casFeat_componentId = jcas.getRequiredFeatureDE(casType, "componentId", "uima.cas.String", featOkTst);
    casFeatCode_componentId  = (null == casFeat_componentId) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_componentId).getCode();

 
    casFeat_predicate = jcas.getRequiredFeatureDE(casType, "predicate", "uima.tcas.Annotation", featOkTst);
    casFeatCode_predicate  = (null == casFeat_predicate) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_predicate).getCode();

 
    casFeat_relationArgs = jcas.getRequiredFeatureDE(casType, "relationArgs", "org.apache.uima.klt.RelationArgs", featOkTst);
    casFeatCode_relationArgs  = (null == casFeat_relationArgs) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_relationArgs).getCode();

  }
}



    
