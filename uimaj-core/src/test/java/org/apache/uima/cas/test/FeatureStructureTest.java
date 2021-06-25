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

/**
 * Class comment for FeatureStructureTest.java goes here.
 * 
 */
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

  private Feature arrayFsWithSubtypeTypeFeat;

  private Type group1Type;

  private Type group2Type;

  private Type langPairType;

  private Type neListType;

  private Feature lang1;

  private Feature lang2;

  private Feature descr;

  private Feature tokenTypeFeat;

  private Feature lemmaFeat;

  private Feature sentLenFeat;

  private Feature tokenFloatFeat;
  private Feature tokenLongFeat;
  private Feature tokenDoubleFeat;

  private Feature startFeature;

  private Feature tlFeature;

  private Feature hdFeature;

  @BeforeEach
  public void setUp() {
    try {
      this.cas = (CASImpl) CASInitializer.initCas(new CASTestSetup(), null);
      assertTrue(this.cas != null);
      this.ts = (TypeSystemImpl) this.cas.getTypeSystem();
      assertTrue(this.ts != null);
    } catch (Exception e) {
      e.printStackTrace();
      assertTrue(false);
    }
    this.topType = this.ts.getType(CAS.TYPE_NAME_TOP);
    assertTrue(this.topType != null);
    this.stringType = this.ts.getType(CAS.TYPE_NAME_STRING);
    assertTrue(this.stringType != null);
    this.tokenType = this.ts.getType(CASTestSetup.TOKEN_TYPE);
    assertTrue(this.stringType != null);
    this.intType = this.ts.getType(CAS.TYPE_NAME_INTEGER);
    assertTrue(this.intType != null);
    this.tokenTypeType = this.ts.getType(CASTestSetup.TOKEN_TYPE_TYPE);
    assertTrue(this.tokenTypeType != null);
    this.wordType = this.ts.getType(CASTestSetup.WORD_TYPE);
    assertTrue(this.wordType != null);
    this.arrayFsWithSubtypeType = this.ts.getType(CASTestSetup.ARRAYFSWITHSUBTYPE_TYPE);
    assertTrue(this.arrayFsWithSubtypeType != null);
    this.arrayFsWithSubtypeTypeFeat = this.ts
            .getFeatureByFullName(CASTestSetup.ARRAYFSWITHSUBTYPE_TYPE_FEAT_Q);
    this.group1Type = this.ts.getType(CASTestSetup.GROUP_1);
    assertTrue(this.group1Type != null);
    this.group2Type = this.ts.getType(CASTestSetup.GROUP_2);
    assertTrue(this.group2Type != null);
    this.tokenTypeFeat = this.ts.getFeatureByFullName(CASTestSetup.TOKEN_TYPE_FEAT_Q);
    assertTrue(this.tokenTypeFeat != null);
    this.lemmaFeat = this.ts.getFeatureByFullName(CASTestSetup.LEMMA_FEAT_Q);
    assertTrue(this.lemmaFeat != null);
    this.sentLenFeat = this.ts.getFeatureByFullName(CASTestSetup.SENT_LEN_FEAT_Q);
    assertTrue(this.sentLenFeat != null);
    this.tokenFloatFeat = this.ts.getFeatureByFullName(CASTestSetup.TOKEN_FLOAT_FEAT_Q);
    assertTrue(this.tokenFloatFeat != null);
    this.tokenDoubleFeat = this.ts.getFeatureByFullName(CASTestSetup.TOKEN_DOUBLE_FEAT_Q);
    assertTrue(this.tokenDoubleFeat != null);
    this.tokenLongFeat = this.ts.getFeatureByFullName(CASTestSetup.TOKEN_LONG_FEAT_Q);
    assertTrue(this.tokenLongFeat != null);
    this.startFeature = this.ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_BEGIN);
    assertTrue(this.startFeature != null);
    this.langPairType = this.ts.getType(CASTestSetup.LANG_PAIR);
    assertTrue(this.langPairType != null);
    this.lang1 = this.langPairType.getFeatureByBaseName(CASTestSetup.LANG1);
    assertTrue(this.lang1 != null);
    this.lang2 = this.langPairType.getFeatureByBaseName(CASTestSetup.LANG2);
    assertTrue(this.lang2 != null);
    this.descr = this.langPairType.getFeatureByBaseName(CASTestSetup.DESCR_FEAT);
    assertTrue(this.descr != null);
    this.neListType = this.ts.getType(CAS.TYPE_NAME_NON_EMPTY_FS_LIST);
    assertTrue(this.neListType != null);
    this.tlFeature = this.neListType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_TAIL);
    assertTrue(this.tlFeature != null);
    this.hdFeature = this.neListType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD);
    assertTrue(this.hdFeature != null);
  }

  @AfterEach
  public void tearDown() {
    this.cas = null;

    this.ts = null;
    this.topType = null;
    this.stringType = null;
    this.tokenType = null;

    this.intType = null;
    this.tokenTypeType = null;
    this.wordType = null;
    this.group1Type = null;
    this.group2Type = null;

    this.tokenTypeFeat = null;
    this.lemmaFeat = null;
    this.sentLenFeat = null;
    this.tokenFloatFeat = null;
    this.startFeature = null;
    this.langPairType = null;
    this.lang1 = null;
    this.lang2 = null;
    this.descr = null;
  }

  @Test
  public void testErrorDerefDifferentCAS() {
    CAS cas2 = CASInitializer.initCas(new CASTestSetup(), null);
    Type tokenType1 = this.ts.getType(CASTestSetup.TOKEN_TYPE);
    Feature tokenTypeFeature = this.ts
            .getFeatureByFullName(CASTestSetup.TOKEN_TYPE + ":" + CASTestSetup.TOKEN_TYPE_FEAT);
    FeatureStructure fs1 = cas2.createFS(tokenType1);
    FeatureStructure fs = cas.createFS(tokenType1);
    boolean caught = false;
    try {
      fs.setFeatureValue(tokenTypeFeature, fs1);
    } catch (Exception e) {
      assertTrue(e instanceof CASRuntimeException);
      caught = true;
    }
    assertTrue(caught);
  }

  @Test
  public void testGetType() {
    Type tokenType1 = this.ts.getType(CASTestSetup.TOKEN_TYPE);
    Type wordType1 = this.ts.getType(CASTestSetup.WORD_TYPE);
    FeatureStructure word = this.cas.createFS(wordType1);
    FeatureStructure token = this.cas.createFS(tokenType1);
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

    Annotation token = this.cas.createFS(tokenType);
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
    Assertions.assertThat(token.getFloatValue(tokenFloatFeat)).isEqualTo(1.1f);
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
    Assertions.assertThat(token.getFloatValue(tokenFloatFeat)).isEqualTo(0.0f);
    assertEquals(null, token.getFeatureValue(tokenTypeFeat));
    assertTrue(fsa.get(0) == token);
    assertTrue(fsl.getHead() == token);

  }

  @Test
  public void testSetArrayValuedFeature() {
    FeatureStructure testFS = this.cas.createFS(this.arrayFsWithSubtypeType);
    assertTrue(testFS.getFeatureValue(this.arrayFsWithSubtypeTypeFeat) == null);
    ArrayFS arrayFS = this.cas.createArrayFS(1);
    testFS.setFeatureValue(this.arrayFsWithSubtypeTypeFeat, arrayFS);
    assertTrue(true);
    boolean caughtExc = false;
    try {
      testFS.setFeatureValue(this.arrayFsWithSubtypeTypeFeat, testFS);
    } catch (CASRuntimeException e) {
      caughtExc = true;
      assertTrue(e.getMessageKey().equals(CASRuntimeException.INAPPROP_RANGE));
    }
    assertTrue(caughtExc);
  }

  @Test
  public void testSetFeatureValue() {
    // FeatureStructure token = this.cas.createFS(this.tokenType);
    LowLevelCAS llcas = cas.getLowLevelCAS();
    int i = llcas.ll_createFS(this.tokenType.getCode());
    AnnotationFS token = llcas.ll_getFSForRef(i);

    assertTrue(token.getFeatureValue(this.tokenTypeFeat) == null);
    assertTrue(token.getStringValue(this.lemmaFeat) == null);
    boolean caughtExc = false;
    try {
      token.getFeatureValue(this.sentLenFeat);
    } catch (CASRuntimeException e) {
      caughtExc = true;
      assertTrue(e.getMessageKey().equals(CASRuntimeException.INAPPROP_FEAT));
    }
    assertTrue(caughtExc);
    FeatureStructure word = this.cas.createFS(this.wordType);
    token.setFeatureValue(this.tokenTypeFeat, word);
    caughtExc = false;
    try {
      token.setFeatureValue(this.lemmaFeat, word);
    } catch (CASRuntimeException e) {
      caughtExc = true;
      assertTrue(e.getMessageKey().equals(CASRuntimeException.INAPPROP_RANGE));
    }
    assertTrue(caughtExc);

    try {
      token.setFeatureValue(this.tokenTypeFeat, null);
    } catch (CASRuntimeException e) {
      assertTrue(false);
    }

    caughtExc = false;
    try {
      token.setFeatureValue(this.startFeature, null);
    } catch (CASRuntimeException e) {
      assertTrue(e.getMessageKey().equals(CASRuntimeException.INAPPROP_RANGE));
      caughtExc = true;
    }
    assertTrue(caughtExc);

    // a "getter" test, not "setter" test, on purpose
    caughtExc = false;
    try {
      token.getFeatureValue(this.startFeature);
    } catch (CASRuntimeException e) {
      assertTrue(e.getMessageKey().equals(CASRuntimeException.INAPPROP_RANGE_NOT_FS));
      caughtExc = true;
    }
    assertTrue(caughtExc);

    assertTrue(token.getStringValue(this.lemmaFeat) == null);
    String testString = "test";
    token.setStringValue(this.lemmaFeat, testString);
    assertTrue(token.getStringValue(this.lemmaFeat).equals(testString));
    testString = "";
    token.setStringValue(this.lemmaFeat, testString);
    assertTrue(token.getStringValue(this.lemmaFeat).equals(testString));

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
    int i = llcas.ll_createFS(this.tokenType.getCode());
    Annotation token = llcas.ll_getFSForRef(i);
    assertTrue(token.getFloatValue(this.tokenFloatFeat) == 0.0f);
    float f = -3.2f;
    token.setFloatValue(this.tokenFloatFeat, f);
    assertTrue(token.getFloatValue(this.tokenFloatFeat) == f);
    f = 51234.132f;
    token.setFloatValue(this.tokenFloatFeat, f);
    assertTrue(token.getFloatValue(this.tokenFloatFeat) == f);
    boolean caughtExc = false;
    try {
      token.setFloatValue(this.tokenTypeFeat, 0.0f);
    } catch (CASRuntimeException e) {
      caughtExc = true;
      assertTrue(e.getMessageKey().equals(CASRuntimeException.INAPPROP_RANGE));
    }
    assertTrue(caughtExc);
    assertTrue(token.getFloatValue(this.tokenFloatFeat) == f);
    caughtExc = false;
    try {
      token.setFloatValue(this.sentLenFeat, 0.0f);
    } catch (CASRuntimeException e) {
      caughtExc = true;
      assertTrue(e.getMessageKey().equals(CASRuntimeException.INAPPROP_RANGE));
    }
    assertTrue(caughtExc);
    assertTrue(token.getFloatValue(this.tokenFloatFeat) == f);

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
    int i = llcas.ll_createFS(this.tokenType.getCode());
    AnnotationFS token = llcas.ll_getFSForRef(i);
    assertTrue(token.getLongValue(this.tokenLongFeat) == 0.0f);
    long f = -34L;
    token.setLongValue(this.tokenLongFeat, f);
    assertTrue(token.getLongValue(this.tokenLongFeat) == f);
    f = 8_000_000_003L;
    token.setLongValue(this.tokenLongFeat, f);
    assertTrue(token.getLongValue(this.tokenLongFeat) == f);
    f = -8_000_000_003L;
    token.setLongValue(this.tokenLongFeat, f);
    assertTrue(token.getLongValue(this.tokenLongFeat) == f);

    // low level
    int ffc = ((FeatureImpl) tokenLongFeat).getCode();
    int h = llcas.ll_getIntValue(token._id(), ffc);
    assertEquals(1, h);

    long g = 23;
    token.setLongValue(this.tokenLongFeat, g);
    assertEquals(g, token.getLongValue(this.tokenLongFeat));

    llcas.ll_setIntValue(token._id(), ffc, h);
    assertEquals(f, token.getLongValue(this.tokenLongFeat));
  }

  @Test
  public void testSetDoubleValue() {
    // AnnotationFS token = (AnnotationFS) this.cas.createFS(this.tokenType);
    LowLevelCAS llcas = cas.getLowLevelCAS();
    int i = llcas.ll_createFS(this.tokenType.getCode());
    AnnotationFS token = llcas.ll_getFSForRef(i);
    assertTrue(token.getDoubleValue(this.tokenDoubleFeat) == 0.0f);
    double f = -34.56D;
    token.setDoubleValue(this.tokenDoubleFeat, f);
    assertTrue(token.getDoubleValue(this.tokenDoubleFeat) == f);
    f = 8_000_000_003.24852D;
    token.setDoubleValue(this.tokenDoubleFeat, f);
    assertTrue(token.getDoubleValue(this.tokenDoubleFeat) == f);
    f = -8_000_000_003D;
    token.setDoubleValue(this.tokenDoubleFeat, f);
    assertTrue(token.getDoubleValue(this.tokenDoubleFeat) == f);

    // low level
    int ffc = ((FeatureImpl) tokenDoubleFeat).getCode();
    int h = llcas.ll_getIntValue(token._id(), ffc);
    assertEquals(1, h);

    double g = 23;
    token.setDoubleValue(this.tokenDoubleFeat, g);
    Assertions.assertThat(token.getDoubleValue(this.tokenDoubleFeat)).isEqualTo(g);

    llcas.ll_setIntValue(token._id(), ffc, h);
    Assertions.assertThat(token.getDoubleValue(this.tokenDoubleFeat)).isEqualTo(f);
  }

  @Test
  public void testSetIntValue() {
    // AnnotationFS token = (AnnotationFS) this.cas.createFS(this.tokenType);
    // AnnotationFS token = (AnnotationFS) this.cas.createFS(this.tokenType);
    LowLevelCAS llcas = cas.getLowLevelCAS();
    int j = llcas.ll_createFS(this.tokenType.getCode());
    AnnotationFS token = llcas.ll_getFSForRef(j);
    assertTrue(token.getIntValue(this.startFeature) == 0);
    int i = 3;
    token.setIntValue(this.startFeature, i);
    assertTrue(token.getIntValue(this.startFeature) == i);
    i = -123456;
    token.setIntValue(this.startFeature, i);
    assertTrue(token.getIntValue(this.startFeature) == i);
    boolean caughtExc = false;
    try {
      token.setIntValue(this.tokenTypeFeat, 0);
    } catch (CASRuntimeException e) {
      caughtExc = true;
      assertTrue(e.getMessageKey().equals(CASRuntimeException.INAPPROP_RANGE));
    }
    assertTrue(caughtExc);
    assertTrue(token.getIntValue(this.startFeature) == i);
    caughtExc = false;
    try {
      token.setIntValue(this.sentLenFeat, 0);
    } catch (CASRuntimeException e) {
      caughtExc = true;
      assertTrue(e.getMessageKey().equals(CASRuntimeException.INAPPROP_FEAT));
    }
    assertTrue(caughtExc);
    assertTrue(token.getIntValue(this.startFeature) == i);
  }

  @Test
  public void testStrings() {
    FeatureStructure lp = this.cas.createFS(this.langPairType);
    assertTrue(lp != null);
    // Check that all strings are initially null.
    try {
      assertTrue(lp.getStringValue(this.lang1) == null);
    } catch (Exception e) {
      assertTrue(false);
    }
    try {
      assertTrue(lp.getStringValue(this.lang2) == null);
    } catch (Exception e) {
      assertTrue(false);
    }
    try {
      assertTrue(lp.getStringValue(this.descr) == null);
    } catch (Exception e) {
      assertTrue(false);
    }
    // FeatureStructure topFS = cas.createFS(topType);
    String val = "Some string.";
    try {
      lp.setStringValue(this.descr, val);
      assertTrue(val.equals(lp.getStringValue(this.descr)));
    } catch (CASRuntimeException e) {
      assertTrue(false);
    }
    try {
      lp.setStringValue(this.descr, null);
      assertTrue(lp.getStringValue(this.descr) == null);
    } catch (CASRuntimeException e) {
      assertTrue(false);
    }
    try {
      lp.setStringValue(this.lang1, CASTestSetup.GROUP_1_LANGUAGES[0]);
      lp.setStringValue(this.lang2, CASTestSetup.GROUP_2_LANGUAGES[2]);
    } catch (Exception e) {
      assertTrue(false);
    }
    boolean exc = false;
    try {
      lp.setStringValue(this.lang1, CASTestSetup.GROUP_2_LANGUAGES[0]);
    } catch (CASRuntimeException e) {
      assertTrue(e.getMessageKey().equals(CASRuntimeException.ILLEGAL_STRING_VALUE));
      exc = true;
    }
    assertTrue(exc);
    exc = false;
    try {
      lp.setStringValue(this.lang2, val);
    } catch (CASRuntimeException e) {
      assertTrue(e.getMessageKey().equals(CASRuntimeException.ILLEGAL_STRING_VALUE));
      exc = true;
    }
    assertTrue(exc);

    // Regression: toString() used to fail because string subtypes were
    // incorrectly classified as ref types.
    lp.toString();

    LowLevelCAS llc = this.cas.getLowLevelCAS();
    LowLevelTypeSystem llts = llc.ll_getTypeSystem();
    final int tokenTypeCode = llts.ll_getCodeForType(this.tokenType);
    final int addr = llc.ll_createFS(tokenTypeCode);
    final int lemmaFeatCode = llts.ll_getCodeForFeature(this.lemmaFeat);
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
    FeatureStructure listFS = this.cas.createFS(this.neListType);
    listFS.setFeatureValue(this.tlFeature, listFS);
    System.out.println("toString for fslist, tail -> node, head is null");
    System.out.println(listFS.toString());

    FeatureStructure value = this.cas.createFS(this.tokenType);
    FeatureStructure newList = this.cas.createFS(this.neListType);
    newList.setFeatureValue(this.tlFeature, listFS);
    newList.setFeatureValue(this.hdFeature, value);
    listFS.setFeatureValue(this.hdFeature, value);
    System.out.println(
            "toString for fslist, tail is prev, prev's head: new token, head is same as rpev's head");
    System.out.println(newList.toString());
  }
}
