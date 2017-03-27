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

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.tcas.Annotation;

/* comment 2 of 14 */
public class Token extends Annotation {

  public final static int typeIndexID = org.apache.uima.jcas.JCasRegistry.register(Token.class);

  public final static int type = typeIndexID;

  public int getTypeIndexID() {
    return typeIndexID;
  }

  // Never called. Disable default constructor
  protected Token() {
  }

 /* Internal - Constructor used by generator */
  public Token(int addr, TOP_Type type) {
    super(addr, type);
  }

  public Token(JCas jcas) {
    super(jcas);
  }

  public Token(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }

  /**
   * <!-- begin-user-doc --> Write your own initialization here <!-- end-user-doc -->
   * 
   * @generated modifiable
   */
  private void readObject() {
  }

  // *------------------*
  // * Feature: ttype
  /* getter for ttype * */
  public TokenType getTtype() {
    if (Token_Type.featOkTst && ((Token_Type) jcasType).casFeat_ttype == null)
      this.jcasType.jcas.throwFeatMissing("ttype", "x.y.z.Token");
    return (TokenType) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr,
            ((Token_Type) jcasType).casFeatCode_ttype)));
  }

  /* setter for ttype * */
  public void setTtype(TokenType v) {
    if (Token_Type.featOkTst && ((Token_Type) jcasType).casFeat_ttype == null)
      this.jcasType.jcas.throwFeatMissing("ttype", "x.y.z.Token");
    jcasType.ll_cas.ll_setRefValue(addr, ((Token_Type) jcasType).casFeatCode_ttype, jcasType.ll_cas
            .ll_getFSRef(v));
  }

  // *------------------*
  // * Feature: tokenFloatFeat
  /* getter for tokenFloatFeat * */
  public float getTokenFloatFeat() {
    if (Token_Type.featOkTst && ((Token_Type) jcasType).casFeat_tokenFloatFeat == null)
      this.jcasType.jcas.throwFeatMissing("tokenFloatFeat", "x.y.z.Token");
    return jcasType.ll_cas.ll_getFloatValue(addr,
            ((Token_Type) jcasType).casFeatCode_tokenFloatFeat);
  }

  /* setter for tokenFloatFeat * */
  public void setTokenFloatFeat(float v) {
    if (Token_Type.featOkTst && ((Token_Type) jcasType).casFeat_tokenFloatFeat == null)
      this.jcasType.jcas.throwFeatMissing("tokenFloatFeat", "x.y.z.Token");
    jcasType.ll_cas.ll_setFloatValue(addr, ((Token_Type) jcasType).casFeatCode_tokenFloatFeat, v);
  }

  // *------------------*
  // * Feature: lemma
  /* getter for lemma * */
  public String getLemma() {
    if (Token_Type.featOkTst && ((Token_Type) jcasType).casFeat_lemma == null)
      this.jcasType.jcas.throwFeatMissing("lemma", "x.y.z.Token");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Token_Type) jcasType).casFeatCode_lemma);
  }

  /* setter for lemma * */
  public void setLemma(String v) {
    if (Token_Type.featOkTst && ((Token_Type) jcasType).casFeat_lemma == null)
      this.jcasType.jcas.throwFeatMissing("lemma", "x.y.z.Token");
    jcasType.ll_cas.ll_setStringValue(addr, ((Token_Type) jcasType).casFeatCode_lemma, v);
  }

  // *------------------*
  // * Feature: sentenceLength
  /* getter for sentenceLength * */
//  public int getSentenceLength() {
//    if (Token_Type.featOkTst && ((Token_Type) jcasType).casFeat_sentenceLength == null)
//      this.jcasType.jcas.throwFeatMissing("sentenceLength", "x.y.z.Token");
//    return jcasType.ll_cas.ll_getIntValue(addr, ((Token_Type) jcasType).casFeatCode_sentenceLength);
//  }

  /* setter for sentenceLength * */
//  public void setSentenceLength(int v) {
//    if (Token_Type.featOkTst && ((Token_Type) jcasType).casFeat_sentenceLength == null)
//      this.jcasType.jcas.throwFeatMissing("sentenceLength", "x.y.z.Token");
//    jcasType.ll_cas.ll_setIntValue(addr, ((Token_Type) jcasType).casFeatCode_sentenceLength, v);
//  }

  // *------------------*
  // * Feature: lemmaList
  /* getter for lemmaList * */
  public StringArray getLemmaList() {
    if (Token_Type.featOkTst && ((Token_Type) jcasType).casFeat_lemmaList == null)
      this.jcasType.jcas.throwFeatMissing("lemmaList", "x.y.z.Token");
    return (StringArray) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr,
            ((Token_Type) jcasType).casFeatCode_lemmaList)));
  }

  /** indexed getter for lemmaList * */
  public String getLemmaList(int i) {
    if (Token_Type.featOkTst && ((Token_Type) jcasType).casFeat_lemmaList == null)
      this.jcasType.jcas.throwFeatMissing("lemmaList", "x.y.z.Token");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr,
            ((Token_Type) jcasType).casFeatCode_lemmaList), i);
    return jcasType.ll_cas.ll_getStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr,
            ((Token_Type) jcasType).casFeatCode_lemmaList), i);
  }

  /* setter for lemmaList * */
  public void setLemmaList(StringArray v) {
    if (Token_Type.featOkTst && ((Token_Type) jcasType).casFeat_lemmaList == null)
      this.jcasType.jcas.throwFeatMissing("lemmaList", "x.y.z.Token");
    jcasType.ll_cas.ll_setRefValue(addr, ((Token_Type) jcasType).casFeatCode_lemmaList,
            jcasType.ll_cas.ll_getFSRef(v));
  }

  /** indexed setter for lemmaList * */
  public void setLemmaList(int i, String v) {
    if (Token_Type.featOkTst && ((Token_Type) jcasType).casFeat_lemmaList == null)
      this.jcasType.jcas.throwFeatMissing("lemmaList", "x.y.z.Token");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr,
            ((Token_Type) jcasType).casFeatCode_lemmaList), i);
    jcasType.ll_cas.ll_setStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr,
            ((Token_Type) jcasType).casFeatCode_lemmaList), i, v);
  }
}
