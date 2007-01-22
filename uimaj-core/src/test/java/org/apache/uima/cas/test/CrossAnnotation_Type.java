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

package org.apache.uima.cas.test;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation_Type;

/**
 * This class is part of the JCas internals. The Get/Set accessors are for low-level CAS access,
 * only.
 */
public class CrossAnnotation_Type extends Annotation_Type {
  protected FSGenerator getFSGenerator() {
    return fsGenerator;
  }

  private final FSGenerator fsGenerator = new FSGenerator() {
    public FeatureStructure createFS(int addr, CASImpl cas) {
      if (instanceOf_Type.useExistingInstance) {
        // Return eq fs instance if already created
        FeatureStructure fs = instanceOf_Type.jcas.getJfsFromCaddr(addr);
        if (null == fs) {
          fs = new CrossAnnotation(addr, instanceOf_Type);
          instanceOf_Type.jcas.putJfsFromCaddr(addr, fs);
          return fs;
        }
        return fs;
      } else
        return new CrossAnnotation(addr, instanceOf_Type);
    }
  };

  public final static int typeIndexID = CrossAnnotation.typeIndexID;

  public final static boolean featOkTst = org.apache.uima.jcas.JCasRegistry.getFeatOkTst("uima.tcas.CrossAnnotation");

  final Feature casFeat_otherAnnotation;

  final int casFeatCode_otherAnnotation;

  public int getOtherAnnotation(int addr) {
    if (featOkTst && casFeat_otherAnnotation == null)
      this.jcas.throwFeatMissing("otherAnnotation", "uima.tcas.CrossAnnotation");
    return ll_cas.ll_getRefValue(addr, casFeatCode_otherAnnotation);
  }

  public void setOtherAnnotation(int addr, int v) {
    if (featOkTst && casFeat_otherAnnotation == null)
      this.jcas.throwFeatMissing("otherAnnotation", "uima.tcas.CrossAnnotation");
    ll_cas.ll_setRefValue(addr, casFeatCode_otherAnnotation, v);
  }

  // * initialize variables to correspond with Cas Type and Features
  public CrossAnnotation_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

    casFeat_otherAnnotation = jcas.getRequiredFeatureDE(casType, "otherAnnotation",
            "uima.tcas.Annotation", featOkTst);
    casFeatCode_otherAnnotation = (null == casFeat_otherAnnotation) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_otherAnnotation).getCode();
  }

  protected CrossAnnotation_Type() { // block default new operator
    casFeat_otherAnnotation = null;
    casFeatCode_otherAnnotation = JCas.INVALID_FEATURE_CODE;
    throw new RuntimeException("Internal Error-this constructor should never be called.");
  }

}
