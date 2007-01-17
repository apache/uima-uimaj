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

package org.apache.uima.jcas.tcas;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.impl.JCasImpl;

public class DocumentAnnotation_Type extends Annotation_Type {
  protected FSGenerator getFSGenerator() {
    return fsGenerator;
  }

  private final FSGenerator fsGenerator = new FSGenerator() {
    public FeatureStructure createFS(int addr, CASImpl cas) {
      if (DocumentAnnotation_Type.this.useExistingInstance) {
        // Return eq fs instance if already created
        FeatureStructure fs = DocumentAnnotation_Type.this.jcas.getJfsFromCaddr(addr);
        if (null == fs) {
          fs = new DocumentAnnotation(addr, DocumentAnnotation_Type.this);
          DocumentAnnotation_Type.this.jcas.putJfsFromCaddr(addr, fs);
          return fs;
        }
        return fs;
      } else
        return new DocumentAnnotation(addr, DocumentAnnotation_Type.this);
    }
  };

  public final static int typeIndexID = DocumentAnnotation.typeIndexID;

  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("uima.tcas.DocumentAnnotation");

  final Feature casFeat_language;

  final int casFeatCode_language;

  public String getLanguage(int addr) {
    if (featOkTst && casFeat_language == null)
      this.jcas.throwFeatMissing("language", "uima.tcas.DocumentAnnotation");
    return ll_cas.ll_getStringValue(addr, casFeatCode_language);
  }

  public void setLanguage(int addr, String v) {
    if (featOkTst && casFeat_language == null)
      this.jcas.throwFeatMissing("language", "uima.tcas.DocumentAnnotation");
    ll_cas.ll_setStringValue(addr, casFeatCode_language, v);
  }

  // * initialize variables to correspond with Cas Type and Features
  public DocumentAnnotation_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

    casFeat_language = jcas.getRequiredFeatureDE(casType, "language", "uima.cas.String", featOkTst);
    casFeatCode_language = (null == casFeat_language) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_language).getCode();
  }

  protected DocumentAnnotation_Type() { // block default new operator
    casFeat_language = null;
    casFeatCode_language = JCas.INVALID_FEATURE_CODE;
    throw new RuntimeException("Internal Error-this constructor should never be called.");
  }

}
