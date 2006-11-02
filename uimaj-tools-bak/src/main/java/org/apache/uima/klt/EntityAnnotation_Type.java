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

/** An annotation of a span of text that refers to some entity
 * Updated by JCasGen Thu Apr 21 11:20:08 EDT 2005
 * @generated */
public class EntityAnnotation_Type extends Annotation_Type {
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
  		       fs = new EntityAnnotation(addr, instanceOf_Type);
  			   instanceOf_Type.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new EntityAnnotation(addr, instanceOf_Type);
  	  }
    };
  /** @generated */
  public final static int typeIndexID = EntityAnnotation.typeIndexID;
  /** @generated 
     @modifiable */
  public final static boolean featOkTst = JCas.getFeatOkTst("org.apache.uima.klt.EntityAnnotation");
 
  /** @generated */
  final Feature casFeat_links;
  /** @generated */
  final int     casFeatCode_links;
  /** @generated */ 
  public int getLinks(int addr) {
        if (featOkTst && casFeat_links == null)
      JCas.throwFeatMissing("links", "org.apache.uima.klt.EntityAnnotation");
    return ll_cas.ll_getRefValue(addr, casFeatCode_links);
  }
  /** @generated */    
  public void setLinks(int addr, int v) {
        if (featOkTst && casFeat_links == null)
      JCas.throwFeatMissing("links", "org.apache.uima.klt.EntityAnnotation");
    ll_cas.ll_setRefValue(addr, casFeatCode_links, v);}
    
  
 
  /** @generated */
  final Feature casFeat_componentId;
  /** @generated */
  final int     casFeatCode_componentId;
  /** @generated */ 
  public String getComponentId(int addr) {
        if (featOkTst && casFeat_componentId == null)
      JCas.throwFeatMissing("componentId", "org.apache.uima.klt.EntityAnnotation");
    return ll_cas.ll_getStringValue(addr, casFeatCode_componentId);
  }
  /** @generated */    
  public void setComponentId(int addr, String v) {
        if (featOkTst && casFeat_componentId == null)
      JCas.throwFeatMissing("componentId", "org.apache.uima.klt.EntityAnnotation");
    ll_cas.ll_setStringValue(addr, casFeatCode_componentId, v);}
    
  
 
  /** @generated */
  final Feature casFeat_mentionType;
  /** @generated */
  final int     casFeatCode_mentionType;
  /** @generated */ 
  public String getMentionType(int addr) {
        if (featOkTst && casFeat_mentionType == null)
      JCas.throwFeatMissing("mentionType", "org.apache.uima.klt.EntityAnnotation");
    return ll_cas.ll_getStringValue(addr, casFeatCode_mentionType);
  }
  /** @generated */    
  public void setMentionType(int addr, String v) {
        if (featOkTst && casFeat_mentionType == null)
      JCas.throwFeatMissing("mentionType", "org.apache.uima.klt.EntityAnnotation");
    ll_cas.ll_setStringValue(addr, casFeatCode_mentionType, v);}
    
  


  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public EntityAnnotation_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_links = jcas.getRequiredFeatureDE(casType, "links", "uima.cas.FSList", featOkTst);
    casFeatCode_links  = (null == casFeat_links) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_links).getCode();

 
    casFeat_componentId = jcas.getRequiredFeatureDE(casType, "componentId", "uima.cas.String", featOkTst);
    casFeatCode_componentId  = (null == casFeat_componentId) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_componentId).getCode();

 
    casFeat_mentionType = jcas.getRequiredFeatureDE(casType, "mentionType", "uima.cas.String", featOkTst);
    casFeatCode_mentionType  = (null == casFeat_mentionType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_mentionType).getCode();

  }
}



    
