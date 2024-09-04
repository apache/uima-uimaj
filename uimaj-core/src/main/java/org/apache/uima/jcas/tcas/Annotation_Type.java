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
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

//*   Not used, only present to avoid compile errors
//*   for old v2 style _Type classes
/**
 * @deprecated
 * @forRemoval 4.0.0
 */
@Deprecated(since = "3.0.0")
public class Annotation_Type extends org.apache.uima.jcas.cas.AnnotationBase_Type {
  @Override
  protected FSGenerator<?> getFSGenerator() {
    return null; // no longer used, but may be needed for compatibility with older existing
                 // JCasGen'd cover classes that might extend this class
  }
  //
  // private final FSGenerator fsGenerator = new FSGenerator() {
  // @SuppressWarnings("unchecked")
  // public Annotation createFS(int addr, CASImpl cas) {
  // if (Annotation_Type.this.useExistingInstance) {
  // // Return eq fs instance if already created
  // Annotation fs = (Annotation) Annotation_Type.this.jcas.getJfsFromCaddr(addr);
  // if (null == fs) {
  // fs = new Annotation(addr, Annotation_Type.this);
  // Annotation_Type.this.jcas.putJfsFromCaddr(addr, fs);
  // return fs;
  // }
  // return fs;
  // } else
  // return new Annotation(addr, Annotation_Type.this);
  // }
  // };

  public static final int typeIndexID = -1;

  public static final boolean featOkTst = JCasRegistry.getFeatOkTst("uima.tcas.Annotation");

  final Feature casFeat_begin = null;

  final int casFeatCode_begin = -1;

  public int getBegin(int addr) {
    throw new RuntimeException("not supported");
    // not needed - is built in
    // if (featOkTst && casFeat_begin == null)
    // this.jcas.throwFeatMissing("begin", "uima.tcas.Annotation");
    // return casImpl.ll_getAnnotBegin(addr);
    // return ll_cas.ll_getIntValue(addr, casFeatCode_begin);
  }

  public void setBegin(int addr, int v) {
    throw new RuntimeException("JCas _Type v2 not supported");
    // if (featOkTst && casFeat_begin == null)
    // this.jcas.throwFeatMissing("begin", "uima.tcas.Annotation");
    // ll_cas.ll_setIntValue(addr, casFeatCode_begin, v);
  }

  final Feature casFeat_end = null;

  final int casFeatCode_end = -1;

  public int getEnd(int addr) {
    throw new RuntimeException("JCas _Type v2 not supported");
    // not needed - is built in
    // if (featOkTst && casFeat_end == null)
    // this.jcas.throwFeatMissing("end", "uima.tcas.Annotation");
    // return casImpl.ll_getAnnotEnd(addr);
    // return ll_cas.ll_getIntValue(addr, casFeatCode_end);
  }

  public void setEnd(int addr, int v) {
    throw new RuntimeException("JCas _Type v2 not supported");
    // if (featOkTst && casFeat_end == null)
    // this.jcas.throwFeatMissing("end", "uima.tcas.Annotation");
    // ll_cas.ll_setIntValue(addr, casFeatCode_end, v);
  }

  /**
   * @see org.apache.uima.cas.text.AnnotationFS#getCoveredText()
   * @param inst
   *          low level reference to a Feature Structure
   * @return null or the covered text
   */
  public String getCoveredText(int inst) {
    final CASImpl casView = ll_cas.ll_getSofaCasView(inst);
    final String text = casView.getDocumentText();
    if (text == null) {
      return null;
    }
    return text.substring(getBegin(inst), getEnd(inst));
  }

  // * initialize variables to correspond with Cas Type and Features
  public Annotation_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    // casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

    // casFeat_begin = jcas.getRequiredFeatureDE(casType, "begin", "uima.cas.Integer", featOkTst);
    // casFeatCode_begin = (null == casFeat_begin) ? JCas.INVALID_FEATURE_CODE
    // : ((FeatureImpl) casFeat_begin).getCode();
    // casFeat_end = jcas.getRequiredFeatureDE(casType, "end", "uima.cas.Integer", featOkTst);
    // casFeatCode_end = (null == casFeat_end) ? JCas.INVALID_FEATURE_CODE
    // : ((FeatureImpl) casFeat_end).getCode();
  }

  protected Annotation_Type() { // block default new operator
    // casFeat_begin = null;
    // casFeatCode_begin = JCas.INVALID_FEATURE_CODE;
    // casFeat_end = null;
    // casFeatCode_end = JCas.INVALID_FEATURE_CODE;
    // throw new RuntimeException("Internal Error-this constructor should never be called.");
  }
}
