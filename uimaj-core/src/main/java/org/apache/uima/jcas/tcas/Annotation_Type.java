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
import org.apache.uima.jcas.impl.JCas;

public class Annotation_Type extends org.apache.uima.jcas.cas.AnnotationBase_Type {
  protected FSGenerator getFSGenerator() {
    return fsGenerator;
  }

  private final FSGenerator fsGenerator = new FSGenerator() {
    public FeatureStructure createFS(int addr, CASImpl cas) {
      if (Annotation_Type.this.useExistingInstance) {
        // Return eq fs instance if already created
        FeatureStructure fs = Annotation_Type.this.jcas.getJfsFromCaddr(addr);
        if (null == fs) {
          fs = new Annotation(addr, Annotation_Type.this);
          Annotation_Type.this.jcas.putJfsFromCaddr(addr, fs);
          return fs;
        }
        return fs;
      } else
        return new Annotation(addr, Annotation_Type.this);
    }
  };

  public final static int typeIndexID = Annotation.typeIndexID;

  public final static boolean featOkTst = JCas.getFeatOkTst("uima.tcas.Annotation");

  final Feature casFeat_begin;

  final int casFeatCode_begin;

  public int getBegin(int addr) {
    if (featOkTst && casFeat_begin == null)
      JCas.throwFeatMissing("begin", "uima.tcas.Annotation");
    return ll_cas.ll_getIntValue(addr, casFeatCode_begin);
  }

  public void setBegin(int addr, int v) {
    if (featOkTst && casFeat_begin == null)
      JCas.throwFeatMissing("begin", "uima.tcas.Annotation");
    ll_cas.ll_setIntValue(addr, casFeatCode_begin, v);
  }

  final Feature casFeat_end;

  final int casFeatCode_end;

  public int getEnd(int addr) {
    if (featOkTst && casFeat_end == null)
      JCas.throwFeatMissing("end", "uima.tcas.Annotation");
    return ll_cas.ll_getIntValue(addr, casFeatCode_end);
  }

  public void setEnd(int addr, int v) {
    if (featOkTst && casFeat_end == null)
      JCas.throwFeatMissing("end", "uima.tcas.Annotation");
    ll_cas.ll_setIntValue(addr, casFeatCode_end, v);
  }

  /**
   * @see org.apache.uima.cas.text.AnnotationFS#getCoveredText()
   */
  // public String getCoveredText(int inst) {
  // final TCASImpl tcasImpl = (TCASImpl) casImpl;
  // final String text = tcasImpl.getDocumentText();
  // if (text == null) return null;
  // return text.substring(getBegin(inst), getEnd(inst));
  // }
  // * initialize variables to correspond with Cas Type and Features
  public Annotation_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

    casFeat_begin = jcas.getRequiredFeatureDE(casType, "begin", "uima.cas.Integer", featOkTst);
    casFeatCode_begin = (null == casFeat_begin) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_begin).getCode();
    casFeat_end = jcas.getRequiredFeatureDE(casType, "end", "uima.cas.Integer", featOkTst);
    casFeatCode_end = (null == casFeat_end) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_end).getCode();
  }

  protected Annotation_Type() { // block default new operator
    casFeat_begin = null;
    casFeatCode_begin = JCas.INVALID_FEATURE_CODE;
    casFeat_end = null;
    casFeatCode_end = JCas.INVALID_FEATURE_CODE;
    throw new RuntimeException("Internal Error-this constructor should never be called.");
  }

}
