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

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

public class AnnotationBase_Type extends org.apache.uima.jcas.cas.TOP_Type {
  protected FSGenerator<?> getFSGenerator() {
    return null; // no longer used, but needed for compatibility with existing JCasGen'd cover classes
  }
//
//  private final FSGenerator fsGenerator = new FSGenerator() {
//    @SuppressWarnings("unchecked")
//    public AnnotationBase createFS(int addr, CASImpl cas) {
//      if (AnnotationBase_Type.this.useExistingInstance) {
//        // Return eq fs instance if already created
//        AnnotationBase fs = AnnotationBase_Type.this.jcas.getJfsFromCaddr(addr);
//        if (null == fs) {
//          fs = new AnnotationBase(addr, AnnotationBase_Type.this);
//          AnnotationBase_Type.this.jcas.putJfsFromCaddr(addr, fs);
//          return fs;
//        }
//        return fs;
//      } else
//        return new AnnotationBase(addr, AnnotationBase_Type.this);
//    }
//  };

  public final static int typeIndexID = AnnotationBase.typeIndexID;

  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("uima.cas.AnnotationBase");

  final Feature casFeat_sofa;

  final int casFeatCode_sofa;

  public SofaFS getSofa(int addr) {
    if (featOkTst && casFeat_sofa == null)
      this.jcas.throwFeatMissing("sofa", "uima.cas.AnnotationBase");
    return (SofaFS) ll_cas.ll_getFSForRef(addr);
  }

  public CAS getView(int addr) {
    return casImpl.ll_getSofaCasView(addr);
  }

  // * initialize variables to correspond with Cas Type and Features
  public AnnotationBase_Type(JCas jcas, Type casType) {
    super(jcas, casType);
//     casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

    casFeat_sofa = jcas.getRequiredFeatureDE(casType, "sofa", "uima.cas.Sofa", featOkTst);
    casFeatCode_sofa = (null == casFeat_sofa) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_sofa).getCode();
  }

  protected AnnotationBase_Type() { // block default new operator
    casFeat_sofa = null;
    casFeatCode_sofa = JCas.INVALID_FEATURE_CODE;
    throw new RuntimeException("Internal Error-this constructor should never be called.");
  }

}
