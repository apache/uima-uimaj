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

import static org.apache.uima.cas.CAS.FEATURE_BASE_NAME_HEAD;
import static org.apache.uima.cas.CAS.FEATURE_BASE_NAME_TAIL;
import static org.apache.uima.cas.CAS.FEATURE_FULL_NAME_BEGIN;
import static org.apache.uima.cas.CAS.TYPE_NAME_INTEGER;
import static org.apache.uima.cas.CAS.TYPE_NAME_NON_EMPTY_FS_LIST;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.cas.CAS.TYPE_NAME_TOP;
import static org.apache.uima.cas.test.CASTestSetup.ARRAYFSWITHSUBTYPE_TYPE;
import static org.apache.uima.cas.test.CASTestSetup.ARRAYFSWITHSUBTYPE_TYPE_FEAT_Q;
import static org.apache.uima.cas.test.CASTestSetup.DESCR_FEAT;
import static org.apache.uima.cas.test.CASTestSetup.GROUP_1;
import static org.apache.uima.cas.test.CASTestSetup.GROUP_1_LANGUAGES;
import static org.apache.uima.cas.test.CASTestSetup.GROUP_2;
import static org.apache.uima.cas.test.CASTestSetup.GROUP_2_LANGUAGES;
import static org.apache.uima.cas.test.CASTestSetup.LANG1;
import static org.apache.uima.cas.test.CASTestSetup.LANG2;
import static org.apache.uima.cas.test.CASTestSetup.LANG_PAIR;
import static org.apache.uima.cas.test.CASTestSetup.LEMMA_FEAT_Q;
import static org.apache.uima.cas.test.CASTestSetup.LEMMA_LIST_FEAT_Q;
import static org.apache.uima.cas.test.CASTestSetup.SENT_LEN_FEAT_Q;
import static org.apache.uima.cas.test.CASTestSetup.TOKEN_DOUBLE_FEAT_Q;
import static org.apache.uima.cas.test.CASTestSetup.TOKEN_FLOAT_FEAT_Q;
import static org.apache.uima.cas.test.CASTestSetup.TOKEN_LONG_FEAT_Q;
import static org.apache.uima.cas.test.CASTestSetup.TOKEN_TYPE;
import static org.apache.uima.cas.test.CASTestSetup.TOKEN_TYPE_FEAT;
import static org.apache.uima.cas.test.CASTestSetup.TOKEN_TYPE_FEAT_Q;
import static org.apache.uima.cas.test.CASTestSetup.TOKEN_TYPE_TYPE;
import static org.apache.uima.cas.test.CASTestSetup.WORD_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.impl.LowLevelTypeSystem;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemConstants;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FeatureStructureTest {

  private CASImpl cas;

  private TypeSystemImpl ts;

  private Type topType;
  private Type stringType;
  private TypeImpl tokenType;
  private Type intType;
  private TypeImpl tokenTypeType;
  private Type wordType;
  private Type arrayFsWithSubtypeType;
  private Type group1Type;
  private Type group2Type;
  private Type langPairType;
  private Type neListType;

  private Feature arrayFsWithSubtypeTypeFeat;
  private Feature lang1;
  private Feature lang2;
  private Feature descr;
  private Feature tokenTypeFeat;
  private Feature lemmaFeat;
  private Feature lemmaListFeat;
  private Feature sentLenFeat;
  private Feature tokenFloatFeat;
  private Feature tokenLongFeat;
  private Feature tokenDoubleFeat;
  private Feature startFeature;
  private Feature tlFeature;
  private Feature hdFeature;

  @BeforeEach
  public void setUp() throws Exception {
    cas = (CASImpl) CASInitializer.initCas(new CASTestSetup(), null);
    assertTrue(cas != null);

    ts = (TypeSystemImpl) cas.getTypeSystem();
    assertTrue(ts != null);

    topType = ts.getType(TYPE_NAME_TOP);
    assertTrue(topType != null);

    stringType = ts.getType(TYPE_NAME_STRING);
    assertTrue(stringType != null);

    tokenType = ts.getType(TOKEN_TYPE);
    assertTrue(stringType != null);

    intType = ts.getType(TYPE_NAME_INTEGER);
    assertTrue(intType != null);

    tokenTypeType = ts.getType(TOKEN_TYPE_TYPE);
    assertTrue(tokenTypeType != null);

    wordType = ts.getType(WORD_TYPE);
    assertTrue(wordType != null);

    arrayFsWithSubtypeType = ts.getType(ARRAYFSWITHSUBTYPE_TYPE);
    assertTrue(arrayFsWithSubtypeType != null);

    arrayFsWithSubtypeTypeFeat = ts.getFeatureByFullName(ARRAYFSWITHSUBTYPE_TYPE_FEAT_Q);

    group1Type = ts.getType(GROUP_1);
    assertTrue(group1Type != null);

    group2Type = ts.getType(GROUP_2);
    assertTrue(group2Type != null);

    tokenTypeFeat = ts.getFeatureByFullName(TOKEN_TYPE_FEAT_Q);
    assertTrue(tokenTypeFeat != null);

    lemmaFeat = ts.getFeatureByFullName(LEMMA_FEAT_Q);
    assertTrue(lemmaFeat != null);

    lemmaListFeat = ts.getFeatureByFullName(LEMMA_LIST_FEAT_Q);
    assertTrue(lemmaListFeat != null);

    sentLenFeat = ts.getFeatureByFullName(SENT_LEN_FEAT_Q);
    assertTrue(sentLenFeat != null);

    tokenFloatFeat = ts.getFeatureByFullName(TOKEN_FLOAT_FEAT_Q);
    assertTrue(tokenFloatFeat != null);

    tokenDoubleFeat = ts.getFeatureByFullName(TOKEN_DOUBLE_FEAT_Q);
    assertTrue(tokenDoubleFeat != null);

    tokenLongFeat = ts.getFeatureByFullName(TOKEN_LONG_FEAT_Q);
    assertTrue(tokenLongFeat != null);

    startFeature = ts.getFeatureByFullName(FEATURE_FULL_NAME_BEGIN);
    assertTrue(startFeature != null);

    langPairType = ts.getType(LANG_PAIR);
    assertTrue(langPairType != null);

    lang1 = langPairType.getFeatureByBaseName(LANG1);
    assertTrue(lang1 != null);

    lang2 = langPairType.getFeatureByBaseName(LANG2);
    assertTrue(lang2 != null);

    descr = langPairType.getFeatureByBaseName(DESCR_FEAT);
    assertTrue(descr != null);

    neListType = ts.getType(TYPE_NAME_NON_EMPTY_FS_LIST);
    assertTrue(neListType != null);

    tlFeature = neListType.getFeatureByBaseName(FEATURE_BASE_NAME_TAIL);
    assertTrue(tlFeature != null);

    hdFeature = neListType.getFeatureByBaseName(FEATURE_BASE_NAME_HEAD);
    assertTrue(hdFeature != null);
  }

  @AfterEach
  public void tearDown() {
    cas = null;

    ts = null;
    topType = null;
    stringType = null;
    tokenType = null;

    intType = null;
    tokenTypeType = null;
    wordType = null;
    group1Type = null;
    group2Type = null;

    tokenTypeFeat = null;
    lemmaFeat = null;
    sentLenFeat = null;
    tokenFloatFeat = null;
    startFeature = null;
    langPairType = null;
    lang1 = null;
    lang2 = null;
    descr = null;
  }

  @Test
  public void testErrorDerefDifferentCAS() {
    CAS cas2 = CASInitializer.initCas(new CASTestSetup(), null);
    Type tokenType1 = ts.getType(TOKEN_TYPE);
    Feature tokenTypeFeature = ts.getFeatureByFullName(TOKEN_TYPE + ":" + TOKEN_TYPE_FEAT);
    FeatureStructure fs1 = cas2.createFS(tokenType1);
    FeatureStructure fs = cas.createFS(tokenType1);

    assertThatExceptionOfType(CASRuntimeException.class) //
            .isThrownBy(() -> fs.setFeatureValue(tokenTypeFeature, fs1));
  }

  @Test
  public void testGetType() {
    Type tokenType1 = ts.getType(TOKEN_TYPE);
    Type wordType1 = ts.getType(WORD_TYPE);

    FeatureStructure word = cas.createFS(wordType1);
    FeatureStructure token = cas.createFS(tokenType1);

    assertTrue(word.getType().equals(wordType1));
    assertTrue(token.getType().equals(tokenType1));
  }

  /**
   * This test tests V2 backwards compatibility The goal is to match what V2 did for low level cas
   * access The area this is testing is the use of the LL int operations to change the type of an
   * existing feature structure.
   */
  @Test
  public void testLLsetType() {
    LowLevelCAS llc = cas.getLowLevelCAS();
    FSArray fsa = new FSArray(ts.getType(CAS.TYPE_NAME_FS_ARRAY), cas, 3);
    fsa.addToIndexes(); // otherwise won't be replaced later
    NonEmptyFSList fsl = new NonEmptyFSList(ts.getType(CAS.TYPE_NAME_NON_EMPTY_FS_LIST), cas);
    fsl.addToIndexes(); // otherwise won't be replaced later

    Annotation token = cas.createFS(tokenType);
    cas.setId2FSsMaybeUnconditionally(token);

    // set up some refs; these must be updated if the type changes in a way to require a new FS
    fsa.set(0, token); // set the 0th element of a FS Array to point to the "token"
    fsl.setHead(token); // set the head element of a FS List to point to the "token"
    int tokId = token._id();

    // set some feature values; some of these are copied (if there's room, etc.)
    TOP ttfv = cas.createFS(tokenTypeType);
    token.setFeatureValue(tokenTypeFeat, ttfv);
    token.setFloatValue(tokenFloatFeat, 1.1f);
    Assertions.assertThat(token.getFloatValue(tokenFloatFeat)).isEqualTo(1.1f);
    token.setDoubleValue(tokenDoubleFeat, 1.7d);
    Assertions.assertThat(token.getDoubleValue(tokenDoubleFeat)).isEqualTo(1.7d);
    token.setBegin(3);
    token.setEnd(5);

    Sofa sofa = (Sofa) token.getSofa();
    assertTrue(sofa != null);
    assertTrue(fsa.get(0) == token);
    assertTrue(fsl.getHead() == token);

    // change the type to just Annotation
    // because this is a supertype, it should not create a new FS

    llc.ll_setIntValue(tokId, 0, TypeSystemConstants.annotTypeCode);
    Annotation fs = cas.getFsFromId(tokId);
    assertTrue(fs == token);
    assertTrue(fs._id() == token._id());
    assertEquals(ts.annotType, fs._getTypeImpl());
    assertEquals(fs.getBegin(), 3);
    assertEquals(fs.getEnd(), 5);
    assertEquals(sofa, fs.getSofa());
    assertTrue(fsa.get(0) == fs);
    assertTrue(fsl.getHead() == fs);

    // Change Annotation back to Token type

    llc.ll_setIntValue(tokId, 0, tokenType.getCode());
    token = cas.getFsFromId(tokId);
    assertTrue(fs == token);
    assertTrue(fs._id() == token._id());
    assertEquals(fs.getBegin(), 3);
    assertEquals(fs.getEnd(), 5);
    assertEquals(sofa, fs.getSofa());
    assertThat(token.getFloatValue(tokenFloatFeat)).isEqualTo(1.1f);
    assertEquals(ttfv, token.getFeatureValue(tokenTypeFeat));
    assertTrue(fsa.get(0) == token);
    assertTrue(fsl.getHead() == token);

    // change type where the type forces a copy
    // token -> token_type_type
    // These types are completely orthogonal, one doesn't subsume the other

    llc.ll_setIntValue(tokId, 0, tokenTypeType.getCode());
    TOP ttt = cas.getFsFromId(tokId);
    assertTrue(ttt != token);
    assertTrue(ttt._id() == tokId);
    assertEquals(ttt._getTypeImpl(), tokenTypeType);
    assertTrue(fsa.get(0) == ttt);
    assertTrue(fsl.getHead() == ttt);

    llc.ll_setIntValue(tokId, 0, tokenType.getCode());
    token = cas.getFsFromId(tokId);
    assertTrue(ttt != token);
    assertTrue(ttt._id() == token._id());
    assertEquals(token.getBegin(), 0);
    assertEquals(token.getEnd(), 0);
    assertEquals(sofa, token.getSofa());
    assertThat(token.getFloatValue(tokenFloatFeat)).isEqualTo(0.0f);
    assertEquals(null, token.getFeatureValue(tokenTypeFeat));
    assertTrue(fsa.get(0) == token);
    assertTrue(fsl.getHead() == token);
  }

  @Test
  public void testSetArrayValuedFeature() {
    FeatureStructure testFS = cas.createFS(arrayFsWithSubtypeType);

    assertThat(testFS.getFeatureValue(arrayFsWithSubtypeTypeFeat)) //
            .as("Initial value is null") //
            .isNull();

    ArrayFS arrayFS = cas.createArrayFS(1);
    testFS.setFeatureValue(arrayFsWithSubtypeTypeFeat, arrayFS);
    assertThat(testFS.getFeatureValue(arrayFsWithSubtypeTypeFeat)) //
            .isSameAs(arrayFS);

    assertThatExceptionOfType(CASRuntimeException.class) //
            .as("Array-valued feature cannot be set with non-array value") //
            .isThrownBy(() -> testFS.setFeatureValue(arrayFsWithSubtypeTypeFeat, testFS));
  }

  @Test
  public void testSetFeatureValue() {
    // FeatureStructure token = this.cas.createFS(this.tokenType);
    LowLevelCAS llcas = cas.getLowLevelCAS();
    int i = llcas.ll_createFS(tokenType.getCode());
    AnnotationFS token = llcas.ll_getFSForRef(i);

    assertTrue(token.getFeatureValue(tokenTypeFeat) == null);
    assertTrue(token.getStringValue(lemmaFeat) == null);

    assertThatExceptionOfType(CASRuntimeException.class) //
            .isThrownBy(() -> token.getFeatureValue(sentLenFeat)) //
            .extracting(CASRuntimeException::getMessageKey) //
            .isEqualTo(CASRuntimeException.INAPPROP_FEAT);

    FeatureStructure word = cas.createFS(wordType);
    token.setFeatureValue(tokenTypeFeat, word);

    assertThatExceptionOfType(CASRuntimeException.class) //
            .isThrownBy(() -> token.setFeatureValue(lemmaFeat, word));

    assertThatNoException() //
            .isThrownBy(() -> token.setFeatureValue(tokenTypeFeat, null));

    assertThatExceptionOfType(CASRuntimeException.class) //
            .isThrownBy(() -> token.setFeatureValue(startFeature, null)) //
            .extracting(CASRuntimeException::getMessageKey) //
            .isEqualTo(CASRuntimeException.INAPPROP_RANGE);

    // a "getter" test, not "setter" test, on purpose
    assertThatExceptionOfType(CASRuntimeException.class) //
            .isThrownBy(() -> token.getFeatureValue(startFeature)) //
            .extracting(CASRuntimeException::getMessageKey) //
            .isEqualTo(CASRuntimeException.INAPPROP_RANGE_NOT_FS);

    assertThat(token.getStringValue(lemmaFeat)) //
            .as("String feature value is initially null") //
            .isNull();

    token.setStringValue(lemmaFeat, "test");
    assertThat(token.getStringValue(lemmaFeat)) //
            .isEqualTo("test");

    token.setStringValue(lemmaFeat, "");
    assertThat(token.getStringValue(lemmaFeat)) //
            .isEqualTo("");

    assertThatExceptionOfType(CASRuntimeException.class) //
            .as("Cannot set boolean value on string array feature") //
            .isThrownBy(() -> token.setBooleanValue(lemmaListFeat, true));

    assertThatExceptionOfType(CASRuntimeException.class) //
            .as("Cannot set string value on string array feature") //
            .isThrownBy(() -> token.setStringValue(lemmaListFeat, "test"));

    // test low level
    token.setFeatureValue(tokenTypeFeat, word);
    int fsRef = token._id();
    int fc = ((FeatureImpl) tokenTypeFeat).getCode();
    assertEquals(llcas.ll_getIntValue(fsRef, fc), word._id());

    int word2_id = llcas.ll_createFS(((TypeImpl) wordType).getCode());
    TOP word2 = llcas.ll_getFSForRef(word2_id);
    // TOP word2 = cas.createFS(wordType);
    llcas.ll_setIntValue(fsRef, fc, word2._id());
    assertEquals(token.getFeatureValue(tokenTypeFeat), word2);
  }

  @Test
  public void testSetFloatValue() {
    // AnnotationFS token = (AnnotationFS) this.cas.createFS(this.tokenType);
    LowLevelCAS llcas = cas.getLowLevelCAS();
    int i = llcas.ll_createFS(tokenType.getCode());
    Annotation token = llcas.ll_getFSForRef(i);
    assertTrue(token.getFloatValue(tokenFloatFeat) == 0.0f);

    float f = -3.2f;
    token.setFloatValue(tokenFloatFeat, f);
    assertTrue(token.getFloatValue(tokenFloatFeat) == f);

    f = 51234.132f;
    token.setFloatValue(tokenFloatFeat, f);
    assertTrue(token.getFloatValue(tokenFloatFeat) == f);

    assertThatExceptionOfType(CASRuntimeException.class) //
            .isThrownBy(() -> token.setFloatValue(tokenTypeFeat, 0.0f)) //
            .extracting(CASRuntimeException::getMessageKey) //
            .isEqualTo(CASRuntimeException.INAPPROP_RANGE);
    assertTrue(token.getFloatValue(tokenFloatFeat) == f);

    assertThatExceptionOfType(CASRuntimeException.class) //
            .isThrownBy(() -> token.setFloatValue(sentLenFeat, 0.0f)) //
            .extracting(CASRuntimeException::getMessageKey) //
            .isEqualTo(CASRuntimeException.INAPPROP_RANGE);
    assertTrue(token.getFloatValue(tokenFloatFeat) == f);

    // low level
    int ffc = ((FeatureImpl) tokenFloatFeat).getCode();
    llcas.ll_setIntValue(token._id(), ffc, CASImpl.float2int(123.456f));
    Assertions.assertThat(token.getFloatValue(tokenFloatFeat)).isEqualTo(123.456f);
    assertEquals(llcas.ll_getIntValue(token._id(), ffc), CASImpl.float2int(123.456f));
  }

  @Test
  public void testSetLongValue() {
    // AnnotationFS token = (AnnotationFS) this.cas.createFS(this.tokenType);
    LowLevelCAS llcas = cas.getLowLevelCAS();
    int i = llcas.ll_createFS(tokenType.getCode());
    AnnotationFS token = llcas.ll_getFSForRef(i);
    assertTrue(token.getLongValue(tokenLongFeat) == 0.0f);

    long f = -34L;
    token.setLongValue(tokenLongFeat, f);
    assertTrue(token.getLongValue(tokenLongFeat) == f);

    f = 8_000_000_003L;
    token.setLongValue(tokenLongFeat, f);
    assertTrue(token.getLongValue(tokenLongFeat) == f);

    f = -8_000_000_003L;
    token.setLongValue(tokenLongFeat, f);
    assertTrue(token.getLongValue(tokenLongFeat) == f);

    // low level
    int ffc = ((FeatureImpl) tokenLongFeat).getCode();
    int h = llcas.ll_getIntValue(token._id(), ffc);
    assertEquals(1, h);

    long g = 23;
    token.setLongValue(tokenLongFeat, g);
    assertEquals(g, token.getLongValue(tokenLongFeat));

    llcas.ll_setIntValue(token._id(), ffc, h);
    assertEquals(f, token.getLongValue(tokenLongFeat));
  }

  @Test
  public void testSetDoubleValue() {
    // AnnotationFS token = (AnnotationFS) this.cas.createFS(this.tokenType);
    LowLevelCAS llcas = cas.getLowLevelCAS();
    int i = llcas.ll_createFS(tokenType.getCode());
    AnnotationFS token = llcas.ll_getFSForRef(i);
    assertTrue(token.getDoubleValue(tokenDoubleFeat) == 0.0f);

    double f = -34.56D;
    token.setDoubleValue(tokenDoubleFeat, f);
    assertTrue(token.getDoubleValue(tokenDoubleFeat) == f);

    f = 8_000_000_003.24852D;
    token.setDoubleValue(tokenDoubleFeat, f);
    assertTrue(token.getDoubleValue(tokenDoubleFeat) == f);

    f = -8_000_000_003D;
    token.setDoubleValue(tokenDoubleFeat, f);
    assertTrue(token.getDoubleValue(tokenDoubleFeat) == f);

    // low level
    int ffc = ((FeatureImpl) tokenDoubleFeat).getCode();
    int h = llcas.ll_getIntValue(token._id(), ffc);
    assertEquals(1, h);

    double g = 23;
    token.setDoubleValue(tokenDoubleFeat, g);
    Assertions.assertThat(token.getDoubleValue(tokenDoubleFeat)).isEqualTo(g);

    llcas.ll_setIntValue(token._id(), ffc, h);
    Assertions.assertThat(token.getDoubleValue(tokenDoubleFeat)).isEqualTo(f);
  }

  @Test
  public void testSetIntValue() {
    // AnnotationFS token = (AnnotationFS) this.cas.createFS(this.tokenType);
    // AnnotationFS token = (AnnotationFS) this.cas.createFS(this.tokenType);
    LowLevelCAS llcas = cas.getLowLevelCAS();
    int j = llcas.ll_createFS(tokenType.getCode());
    AnnotationFS token = llcas.ll_getFSForRef(j);
    assertTrue(token.getIntValue(startFeature) == 0);

    int i = 3;
    token.setIntValue(startFeature, i);
    assertTrue(token.getIntValue(startFeature) == i);

    i = -123456;
    token.setIntValue(startFeature, i);
    assertTrue(token.getIntValue(startFeature) == i);

    assertThatExceptionOfType(CASRuntimeException.class) //
            .isThrownBy(() -> token.setIntValue(tokenTypeFeat, 0)) //
            .extracting(CASRuntimeException::getMessageKey) //
            .isEqualTo(CASRuntimeException.INAPPROP_RANGE);
    assertTrue(token.getIntValue(startFeature) == i);

    assertThatExceptionOfType(CASRuntimeException.class) //
            .isThrownBy(() -> token.setIntValue(sentLenFeat, 0)) //
            .extracting(CASRuntimeException::getMessageKey) //
            .isEqualTo(CASRuntimeException.INAPPROP_FEAT);
    assertTrue(token.getIntValue(startFeature) == i);
  }

  @Test
  public void testStrings() {
    FeatureStructure lp = cas.createFS(langPairType);
    assertTrue(lp != null);

    // Check that all strings are initially null.
    assertTrue(lp.getStringValue(lang1) == null);
    assertTrue(lp.getStringValue(lang2) == null);
    assertTrue(lp.getStringValue(descr) == null);

    // FeatureStructure topFS = cas.createFS(topType);
    String val = "Some string.";
    lp.setStringValue(descr, val);
    assertTrue(val.equals(lp.getStringValue(descr)));

    lp.setStringValue(descr, null);
    assertTrue(lp.getStringValue(descr) == null);

    lp.setStringValue(lang1, GROUP_1_LANGUAGES[0]);
    lp.setStringValue(lang2, GROUP_2_LANGUAGES[2]);

    assertThatExceptionOfType(CASRuntimeException.class) //
            .isThrownBy(() -> lp.setStringValue(lang1, GROUP_2_LANGUAGES[0]))
            .extracting(CASRuntimeException::getMessageKey) //
            .isEqualTo(CASRuntimeException.ILLEGAL_STRING_VALUE);

    assertThatExceptionOfType(CASRuntimeException.class) //
            .isThrownBy(() -> lp.setStringValue(lang2, val))
            .extracting(CASRuntimeException::getMessageKey) //
            .isEqualTo(CASRuntimeException.ILLEGAL_STRING_VALUE);

    // Regression: toString() used to fail because string subtypes were
    // incorrectly classified as ref types.
    lp.toString();

    LowLevelCAS llc = cas.getLowLevelCAS();
    LowLevelTypeSystem llts = llc.ll_getTypeSystem();
    final int tokenTypeCode = llts.ll_getCodeForType(tokenType);
    final int addr = llc.ll_createFS(tokenTypeCode);
    final int lemmaFeatCode = llts.ll_getCodeForFeature(lemmaFeat);
    llc.ll_setStringValue(addr, lemmaFeatCode, "test", true);
    assertTrue(llc.ll_getCharBufferValueSize(addr, lemmaFeatCode) == 4);

    // try accessing low level strings using ll_setIntValue

    final int stringcode = llc.ll_getIntValue(addr, lemmaFeatCode);
    assertTrue(stringcode == 1);

    llc.ll_setStringValue(addr, lemmaFeatCode, "test", true);
    assertEquals(llc.ll_getIntValue(addr, lemmaFeatCode), 1); // should not change

    llc.ll_setStringValue(addr, lemmaFeatCode, "test2", true);
    assertEquals(llc.ll_getIntValue(addr, lemmaFeatCode), 2);

    llc.ll_setIntValue(addr, lemmaFeatCode, 1);
    assertEquals(llc.ll_getIntValue(addr, lemmaFeatCode), 1);
    assertEquals(llc.ll_getStringValue(addr, lemmaFeatCode), "test");

    llc.ll_setIntValue(addr, lemmaFeatCode, 0);
    assertEquals(llc.ll_getIntValue(addr, lemmaFeatCode), 0);
    assertTrue(llc.ll_getStringValue(addr, lemmaFeatCode) == null);

    llc.ll_setIntValue(addr, lemmaFeatCode, 2);
    assertEquals(llc.ll_getStringValue(addr, lemmaFeatCode), "test2");

    // check that equal strings are shared

    llc.ll_setStringValue(addr, lemmaFeatCode, new String("test"));
    assertEquals(1, llc.ll_getIntValue(addr, lemmaFeatCode));
  }

  @Test
  public void testEquals() {
    // ???
  }

  @Test
  public void testToString() {
    FeatureStructure listFS = cas.createFS(neListType);
    listFS.setFeatureValue(tlFeature, listFS);
    System.out.println("toString for fslist, tail -> node, head is null");
    System.out.println(listFS.toString());

    FeatureStructure value = cas.createFS(tokenType);
    FeatureStructure newList = cas.createFS(neListType);
    newList.setFeatureValue(tlFeature, listFS);
    newList.setFeatureValue(hdFeature, value);
    listFS.setFeatureValue(hdFeature, value);
    System.out.println(
            "toString for fslist, tail is prev, prev's head: new token, head is same as rpev's head");
    System.out.println(newList.toString());
  }
}
