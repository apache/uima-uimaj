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

package org.apache.uima.jcas.cas;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/**
 * Updated by JCasGen Fri Apr 29 16:05:04 EDT 2005
 * 
 * @generated
 */
public class Sofa_Type extends TOP_Type {
//  /** @generated */
  protected FSGenerator<?> getFSGenerator() {
    return null; // no longer used, but may be needed for compatibility with older existing JCasGen'd cover classes that might extend this class
  }
//
//  /** @generated */
//  private final FSGenerator fsGenerator = new FSGenerator() {
//    public FeatureStructure createFS(int addr, CASImpl cas) {
//      if (Sofa_Type.this.useExistingInstance) {
//        // Return eq fs instance if already created
//        FeatureStructure fs = Sofa_Type.this.jcas.getJfsFromCaddr(addr);
//        if (null == fs) {
//          fs = new Sofa(addr, Sofa_Type.this);
//          Sofa_Type.this.jcas.putJfsFromCaddr(addr, fs);
//          return fs;
//        }
//        return fs;
//      } else
//        return new Sofa(addr, Sofa_Type.this);
//    }
//  };

  /** @generated */
  public final static int typeIndexID = Sofa.typeIndexID;

  /**
   * @generated
   * @modifiable
   */
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("uima.cas.Sofa");

  /** @generated */
  final Feature casFeat_sofaNum;

  /** @generated */
  final int casFeatCode_sofaNum;

  /** @generated */
  final Feature casFeat_sofaID;

  /** @generated */
  final int casFeatCode_sofaID;

  /** @generated */
  final Feature casFeat_mimeType;

  /** @generated */
  final int casFeatCode_mimeType;

  /**
   * initialize variables to correspond with Cas Type and Features
   * 
   * @param jcas the JCas
   * @param casType the Sofa Type Instance
   * @generated
   */
  public Sofa_Type(JCas jcas, Type casType) {
    super(jcas, casType);
//     casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

    casFeat_sofaNum = jcas.getRequiredFeatureDE(casType, "sofaNum", "uima.cas.Integer", featOkTst);
    casFeatCode_sofaNum = (null == casFeat_sofaNum) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_sofaNum).getCode();

    casFeat_sofaID = jcas.getRequiredFeatureDE(casType, "sofaID", "uima.cas.String", featOkTst);
    casFeatCode_sofaID = (null == casFeat_sofaID) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_sofaID).getCode();

    casFeat_mimeType = jcas.getRequiredFeatureDE(casType, "mimeType", "uima.cas.String", featOkTst);
    casFeatCode_mimeType = (null == casFeat_mimeType) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_mimeType).getCode();

  }
}
