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
import org.apache.uima.jcas.cas.TOP_Type;

/** Anything that may be referred to
 * Updated by JCasGen Thu Apr 21 11:20:08 EDT 2005
 * @generated */
public class Referent_Type extends TOP_Type {
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
  		       fs = new Referent(addr, instanceOf_Type);
  			   instanceOf_Type.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Referent(addr, instanceOf_Type);
  	  }
    };
  /** @generated */
  public final static int typeIndexID = Referent.typeIndexID;
  /** @generated 
     @modifiable */
  public final static boolean featOkTst = JCas.getFeatOkTst("org.apache.uima.klt.Referent");
 
  /** @generated */
  final Feature casFeat_links;
  /** @generated */
  final int     casFeatCode_links;
  /** @generated */ 
  public int getLinks(int addr) {
        if (featOkTst && casFeat_links == null)
      JCas.throwFeatMissing("links", "org.apache.uima.klt.Referent");
    return ll_cas.ll_getRefValue(addr, casFeatCode_links);
  }
  /** @generated */    
  public void setLinks(int addr, int v) {
        if (featOkTst && casFeat_links == null)
      JCas.throwFeatMissing("links", "org.apache.uima.klt.Referent");
    ll_cas.ll_setRefValue(addr, casFeatCode_links, v);}
    
  
 
  /** @generated */
  final Feature casFeat_canonicalForm;
  /** @generated */
  final int     casFeatCode_canonicalForm;
  /** @generated */ 
  public String getCanonicalForm(int addr) {
        if (featOkTst && casFeat_canonicalForm == null)
      JCas.throwFeatMissing("canonicalForm", "org.apache.uima.klt.Referent");
    return ll_cas.ll_getStringValue(addr, casFeatCode_canonicalForm);
  }
  /** @generated */    
  public void setCanonicalForm(int addr, String v) {
        if (featOkTst && casFeat_canonicalForm == null)
      JCas.throwFeatMissing("canonicalForm", "org.apache.uima.klt.Referent");
    ll_cas.ll_setStringValue(addr, casFeatCode_canonicalForm, v);}
    
  
 
  /** @generated */
  final Feature casFeat_variantForms;
  /** @generated */
  final int     casFeatCode_variantForms;
  /** @generated */ 
  public int getVariantForms(int addr) {
        if (featOkTst && casFeat_variantForms == null)
      JCas.throwFeatMissing("variantForms", "org.apache.uima.klt.Referent");
    return ll_cas.ll_getRefValue(addr, casFeatCode_variantForms);
  }
  /** @generated */    
  public void setVariantForms(int addr, int v) {
        if (featOkTst && casFeat_variantForms == null)
      JCas.throwFeatMissing("variantForms", "org.apache.uima.klt.Referent");
    ll_cas.ll_setRefValue(addr, casFeatCode_variantForms, v);}
    
  
 
  /** @generated */
  final Feature casFeat_componentId;
  /** @generated */
  final int     casFeatCode_componentId;
  /** @generated */ 
  public String getComponentId(int addr) {
        if (featOkTst && casFeat_componentId == null)
      JCas.throwFeatMissing("componentId", "org.apache.uima.klt.Referent");
    return ll_cas.ll_getStringValue(addr, casFeatCode_componentId);
  }
  /** @generated */    
  public void setComponentId(int addr, String v) {
        if (featOkTst && casFeat_componentId == null)
      JCas.throwFeatMissing("componentId", "org.apache.uima.klt.Referent");
    ll_cas.ll_setStringValue(addr, casFeatCode_componentId, v);}
    
  
 
  /** @generated */
  final Feature casFeat_classes;
  /** @generated */
  final int     casFeatCode_classes;
  /** @generated */ 
  public int getClasses(int addr) {
        if (featOkTst && casFeat_classes == null)
      JCas.throwFeatMissing("classes", "org.apache.uima.klt.Referent");
    return ll_cas.ll_getRefValue(addr, casFeatCode_classes);
  }
  /** @generated */    
  public void setClasses(int addr, int v) {
        if (featOkTst && casFeat_classes == null)
      JCas.throwFeatMissing("classes", "org.apache.uima.klt.Referent");
    ll_cas.ll_setRefValue(addr, casFeatCode_classes, v);}
    
   /** @generated */
  public String getClasses(int addr, int i) {
        if (featOkTst && casFeat_classes == null)
      JCas.throwFeatMissing("classes", "org.apache.uima.klt.Referent");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_classes), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_classes), i);
  return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_classes), i);
  }
   
  /** @generated */ 
  public void setClasses(int addr, int i, String v) {
        if (featOkTst && casFeat_classes == null)
      JCas.throwFeatMissing("classes", "org.apache.uima.klt.Referent");
    if (lowLevelTypeChecks)
      ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_classes), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_classes), i);
    ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_classes), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_id;
  /** @generated */
  final int     casFeatCode_id;
  /** @generated */ 
  public String getId(int addr) {
        if (featOkTst && casFeat_id == null)
      JCas.throwFeatMissing("id", "org.apache.uima.klt.Referent");
    return ll_cas.ll_getStringValue(addr, casFeatCode_id);
  }
  /** @generated */    
  public void setId(int addr, String v) {
        if (featOkTst && casFeat_id == null)
      JCas.throwFeatMissing("id", "org.apache.uima.klt.Referent");
    ll_cas.ll_setStringValue(addr, casFeatCode_id, v);}
    
  


  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public Referent_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_links = jcas.getRequiredFeatureDE(casType, "links", "uima.cas.FSList", featOkTst);
    casFeatCode_links  = (null == casFeat_links) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_links).getCode();

 
    casFeat_componentId = jcas.getRequiredFeatureDE(casType, "componentId", "uima.cas.String", featOkTst);
    casFeatCode_componentId  = (null == casFeat_componentId) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_componentId).getCode();

 
    casFeat_classes = jcas.getRequiredFeatureDE(casType, "classes", "uima.cas.StringArray", featOkTst);
    casFeatCode_classes  = (null == casFeat_classes) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_classes).getCode();

 
    casFeat_canonicalForm = jcas.getRequiredFeatureDE(casType, "canonicalForm", "uima.cas.String", featOkTst);
    casFeatCode_canonicalForm  = (null == casFeat_canonicalForm) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_canonicalForm).getCode();

 
    casFeat_variantForms = jcas.getRequiredFeatureDE(casType, "variantForms", "uima.cas.StringList", featOkTst);
    casFeatCode_variantForms  = (null == casFeat_variantForms) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_variantForms).getCode();

 
    casFeat_id = jcas.getRequiredFeatureDE(casType, "id", "uima.cas.String", featOkTst);
    casFeatCode_id  = (null == casFeat_id) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_id).getCode();

  }
}



    
