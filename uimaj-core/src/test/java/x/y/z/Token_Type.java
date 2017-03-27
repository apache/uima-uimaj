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

package x.y.z;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation_Type;

/* comment 2 of 14 */
public class Token_Type extends Annotation_Type {
  protected FSGenerator getFSGenerator() {
    return fsGenerator;
  }

  private final FSGenerator fsGenerator = new FSGenerator() {
    public FeatureStructure createFS(int addr, CASImpl cas) {
      if (instanceOf_Type.useExistingInstance) {
        // Return eq fs instance if already created
        FeatureStructure fs = instanceOf_Type.jcas.getJfsFromCaddr(addr);
        if (null == fs) {
          fs = new Token(addr, instanceOf_Type);
          instanceOf_Type.jcas.putJfsFromCaddr(addr, fs);
          return fs;
        }
        return fs;
      } else
        return new Token(addr, instanceOf_Type);
    }
  };

  public final static int typeIndexID = Token.typeIndexID;

  public final static boolean featOkTst = org.apache.uima.jcas.JCasRegistry.getFeatOkTst("x.y.z.Token");

  final Feature casFeat_ttype;

  final int casFeatCode_ttype;

  public int getTtype(int addr) {
    if (featOkTst && casFeat_ttype == null)
      this.jcas.throwFeatMissing("ttype", "x.y.z.Token");
    return ll_cas.ll_getRefValue(addr, casFeatCode_ttype);
  }

  public void setTtype(int addr, int v) {
    if (featOkTst && casFeat_ttype == null)
      this.jcas.throwFeatMissing("ttype", "x.y.z.Token");
    ll_cas.ll_setRefValue(addr, casFeatCode_ttype, v);
  }

  final Feature casFeat_tokenFloatFeat;

  final int casFeatCode_tokenFloatFeat;

  public float getTokenFloatFeat(int addr) {
    if (featOkTst && casFeat_tokenFloatFeat == null)
      this.jcas.throwFeatMissing("tokenFloatFeat", "x.y.z.Token");
    return ll_cas.ll_getFloatValue(addr, casFeatCode_tokenFloatFeat);
  }

  public void setTokenFloatFeat(int addr, float v) {
    if (featOkTst && casFeat_tokenFloatFeat == null)
      this.jcas.throwFeatMissing("tokenFloatFeat", "x.y.z.Token");
    ll_cas.ll_setFloatValue(addr, casFeatCode_tokenFloatFeat, v);
  }

  final Feature casFeat_lemma;

  final int casFeatCode_lemma;

  public String getLemma(int addr) {
    if (featOkTst && casFeat_lemma == null)
      this.jcas.throwFeatMissing("lemma", "x.y.z.Token");
    return ll_cas.ll_getStringValue(addr, casFeatCode_lemma);
  }

  public void setLemma(int addr, String v) {
    if (featOkTst && casFeat_lemma == null)
      this.jcas.throwFeatMissing("lemma", "x.y.z.Token");
    ll_cas.ll_setStringValue(addr, casFeatCode_lemma, v);
  }

//  final Feature casFeat_sentenceLength;
//
//  final int casFeatCode_sentenceLength;
//
//  public int getSentenceLength(int addr) {
//    if (featOkTst && casFeat_sentenceLength == null)
//      this.jcas.throwFeatMissing("sentenceLength", "x.y.z.Token");
//    return ll_cas.ll_getIntValue(addr, casFeatCode_sentenceLength);
//  }
//
//  public void setSentenceLength(int addr, int v) {
//    if (featOkTst && casFeat_sentenceLength == null)
//      this.jcas.throwFeatMissing("sentenceLength", "x.y.z.Token");
//    ll_cas.ll_setIntValue(addr, casFeatCode_sentenceLength, v);
//  }

  final Feature casFeat_lemmaList;

  final int casFeatCode_lemmaList;

  public int getLemmaList(int addr) {
    if (featOkTst && casFeat_lemmaList == null)
      this.jcas.throwFeatMissing("lemmaList", "x.y.z.Token");
    return ll_cas.ll_getRefValue(addr, casFeatCode_lemmaList);
  }

  public String getLemmaList(int addr, int i) {
    if (featOkTst && casFeat_lemmaList == null)
      this.jcas.throwFeatMissing("lemmaList", "x.y.z.Token");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_lemmaList), i,
              true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_lemmaList), i);
    return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_lemmaList), i);
  }

  public void setLemmaList(int addr, int v) {
    if (featOkTst && casFeat_lemmaList == null)
      this.jcas.throwFeatMissing("lemmaList", "x.y.z.Token");
    ll_cas.ll_setRefValue(addr, casFeatCode_lemmaList, v);
  }

  public void setLemmaList(int addr, int i, String v) {
    if (featOkTst && casFeat_lemmaList == null)
      this.jcas.throwFeatMissing("lemmaList", "x.y.z.Token");
    if (lowLevelTypeChecks)
      ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_lemmaList), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_lemmaList), i);
    ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_lemmaList), i, v);
  }

  // * initialize variables to correspond with Cas Type and Features
  public Token_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

    casFeat_ttype = jcas.getRequiredFeatureDE(casType, "ttype", "x.y.z.TokenType", featOkTst);
    casFeatCode_ttype = (null == casFeat_ttype) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_ttype).getCode();
    casFeat_tokenFloatFeat = jcas.getRequiredFeatureDE(casType, "tokenFloatFeat", "uima.cas.Float",
            featOkTst);
    casFeatCode_tokenFloatFeat = (null == casFeat_tokenFloatFeat) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_tokenFloatFeat).getCode();
    casFeat_lemma = jcas.getRequiredFeatureDE(casType, "lemma", "uima.cas.String", featOkTst);
    casFeatCode_lemma = (null == casFeat_lemma) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_lemma).getCode();
//    casFeat_sentenceLength = jcas.getRequiredFeatureDE(casType, "sentenceLength",
//            "uima.cas.Integer", featOkTst);
//    casFeatCode_sentenceLength = (null == casFeat_sentenceLength) ? JCas.INVALID_FEATURE_CODE
//            : ((FeatureImpl) casFeat_sentenceLength).getCode();
    casFeat_lemmaList = jcas.getRequiredFeatureDE(casType, "lemmaList", "uima.cas.StringArray",
            featOkTst);
    casFeatCode_lemmaList = (null == casFeat_lemmaList) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_lemmaList).getCode();
  }

  protected Token_Type() { // block default new operator
    casFeat_ttype = null;
    casFeatCode_ttype = JCas.INVALID_FEATURE_CODE;
    casFeat_tokenFloatFeat = null;
    casFeatCode_tokenFloatFeat = JCas.INVALID_FEATURE_CODE;
    casFeat_lemma = null;
    casFeatCode_lemma = JCas.INVALID_FEATURE_CODE;
//    casFeat_sentenceLength = null;
//    casFeatCode_sentenceLength = JCas.INVALID_FEATURE_CODE;
    casFeat_lemmaList = null;
    casFeatCode_lemmaList = JCas.INVALID_FEATURE_CODE;
    throw new RuntimeException("Internal Error-this constructor should never be called.");
  }

}
