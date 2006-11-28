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

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.impl.JCas;

/**
 * A directional link between two instances Updated by JCasGen Thu Apr 21 11:20:08 EDT 2005
 * 
 * @generated
 */
public class Link_Type extends TOP_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {
    return fsGenerator;
  }

  /** @generated */
  private final FSGenerator fsGenerator = new FSGenerator() {
    public FeatureStructure createFS(int addr, CASImpl cas) {
      if (instanceOf_Type.useExistingInstance) {
        // Return eq fs instance if already created
        FeatureStructure fs = instanceOf_Type.jcas.getJfsFromCaddr(addr);
        if (null == fs) {
          fs = new Link(addr, instanceOf_Type);
          instanceOf_Type.jcas.putJfsFromCaddr(addr, fs);
          return fs;
        }
        return fs;
      } else
        return new Link(addr, instanceOf_Type);
    }
  };

  /** @generated */
  public final static int typeIndexID = Link.typeIndexID;

  /**
   * @generated
   * @modifiable
   */
  public final static boolean featOkTst = JCas.getFeatOkTst("org.apache.uima.klt.Link");

  /** @generated */
  final Feature casFeat_from;

  /** @generated */
  final int casFeatCode_from;

  /** @generated */
  public int getFrom(int addr) {
    if (featOkTst && casFeat_from == null)
      JCas.throwFeatMissing("from", "org.apache.uima.klt.Link");
    return ll_cas.ll_getRefValue(addr, casFeatCode_from);
  }

  /** @generated */
  public void setFrom(int addr, int v) {
    if (featOkTst && casFeat_from == null)
      JCas.throwFeatMissing("from", "org.apache.uima.klt.Link");
    ll_cas.ll_setRefValue(addr, casFeatCode_from, v);
  }

  /** @generated */
  final Feature casFeat_to;

  /** @generated */
  final int casFeatCode_to;

  /** @generated */
  public int getTo(int addr) {
    if (featOkTst && casFeat_to == null)
      JCas.throwFeatMissing("to", "org.apache.uima.klt.Link");
    return ll_cas.ll_getRefValue(addr, casFeatCode_to);
  }

  /** @generated */
  public void setTo(int addr, int v) {
    if (featOkTst && casFeat_to == null)
      JCas.throwFeatMissing("to", "org.apache.uima.klt.Link");
    ll_cas.ll_setRefValue(addr, casFeatCode_to, v);
  }

  /** @generated */
  final Feature casFeat_componentId;

  /** @generated */
  final int casFeatCode_componentId;

  /** @generated */
  public String getComponentId(int addr) {
    if (featOkTst && casFeat_componentId == null)
      JCas.throwFeatMissing("componentId", "org.apache.uima.klt.Link");
    return ll_cas.ll_getStringValue(addr, casFeatCode_componentId);
  }

  /** @generated */
  public void setComponentId(int addr, String v) {
    if (featOkTst && casFeat_componentId == null)
      JCas.throwFeatMissing("componentId", "org.apache.uima.klt.Link");
    ll_cas.ll_setStringValue(addr, casFeatCode_componentId, v);
  }

  /**
   * initialize variables to correspond with Cas Type and Features
   * 
   * @generated
   */
  public Link_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

    casFeat_componentId = jcas.getRequiredFeatureDE(casType, "componentId", "uima.cas.String",
            featOkTst);
    casFeatCode_componentId = (null == casFeat_componentId) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_componentId).getCode();

    casFeat_from = jcas.getRequiredFeatureDE(casType, "from", "uima.cas.TOP", featOkTst);
    casFeatCode_from = (null == casFeat_from) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_from).getCode();

    casFeat_to = jcas.getRequiredFeatureDE(casType, "to", "uima.cas.TOP", featOkTst);
    casFeatCode_to = (null == casFeat_to) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl) casFeat_to)
            .getCode();

  }
}
