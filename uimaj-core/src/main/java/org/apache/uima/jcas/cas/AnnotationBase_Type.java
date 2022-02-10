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
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

//*   Not used, only present to avoid compile errors
//*   for old v2 style _Type classes
/**
 * for v2 compiling only
 * 
 * @deprecated
 */
@Deprecated
public class AnnotationBase_Type extends org.apache.uima.jcas.cas.TOP_Type {
  public final static int typeIndexID = -1;

  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("uima.cas.AnnotationBase");

  final Feature casFeat_sofa;

  final int casFeatCode_sofa;

  @Override
  protected FSGenerator<?> getFSGenerator() {
    return null; // no longer used, but may be needed for compatibility with older existing
                 // JCasGen'd cover classes that might extend this class
  }

  public SofaFS getSofa(int addr) {
    throw new RuntimeException("not supported");
    // if (featOkTst && casFeat_sofa == null)
    // this.jcas.throwFeatMissing("sofa", "uima.cas.AnnotationBase");
    // return (SofaFS) ll_cas.ll_getFSForRef(addr);
  }

  public CAS getView(int addr) {
    throw new RuntimeException("not supported");
    // return casImpl.ll_getSofaCasView(addr);
  }

  // * initialize variables to correspond with Cas Type and Features
  public AnnotationBase_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casFeat_sofa = null;
    casFeatCode_sofa = JCas.INVALID_FEATURE_CODE;

    // casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());
    return;
    // casFeat_sofa = jcas.getRequiredFeatureDE(casType, "sofa", "uima.cas.Sofa", featOkTst);
    // casFeatCode_sofa = (null == casFeat_sofa) ? JCas.INVALID_FEATURE_CODE
    // : ((FeatureImpl) casFeat_sofa).getCode();
  }

  protected AnnotationBase_Type() { // block default new operator
    casFeat_sofa = null;
    casFeatCode_sofa = JCas.INVALID_FEATURE_CODE;
    throw new RuntimeException("Internal Error-this constructor should never be called.");
  }

}
