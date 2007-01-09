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

import junit.framework.TestCase;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.impl.LowLevelTypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.TCAS;

/**
 * Class comment for FeatureStructureTest.java goes here.
 * 
 * @author Thilo Goetz
 */
public class FeatureStructureTest extends TestCase {

  private CAS cas;

  private TypeSystem ts;

  private Type topType;

  private Type stringType;

  private Type tokenType;

  private Type intType;

  private Type tokenTypeType;

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

  private Feature startFeature;

  private Feature tlFeature;

  private Feature hdFeature;

  /**
   * Constructor for FeatureStructureTest.
   * 
   * @param arg0
   */
  public FeatureStructureTest(String arg0) {
    super(arg0);
  }

  public void setUp() {
    try {
      this.cas = CASInitializer.initCas(new CASTestSetup());
      assertTrue(this.cas != null);
      this.ts = this.cas.getTypeSystem();
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
    this.arrayFsWithSubtypeTypeFeat = this.ts.getFeatureByFullName(CASTestSetup.ARRAYFSWITHSUBTYPE_TYPE_FEAT_Q);
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
    this.startFeature = this.ts.getFeatureByFullName(TCAS.FEATURE_FULL_NAME_BEGIN);
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

  public void testGetType() {
    Type tokenType1 = this.ts.getType(CASTestSetup.TOKEN_TYPE);
    Type wordType1 = this.ts.getType(CASTestSetup.WORD_TYPE);
    FeatureStructure word = this.cas.createFS(wordType1);
    FeatureStructure token = this.cas.createFS(tokenType1);
    assertTrue(word.getType().equals(wordType1));
    assertTrue(token.getType().equals(tokenType1));
  }

  public void testSetArrayValuedFeature() {
    FeatureStructure testFS = this.cas.createFS(this.arrayFsWithSubtypeType);
    assertTrue(testFS.getFeatureValue(this.arrayFsWithSubtypeTypeFeat) == null);
    ArrayFS arrayFS = cas.createArrayFS(1);
    testFS.setFeatureValue(arrayFsWithSubtypeTypeFeat, arrayFS);
    assertTrue(true); 
    boolean caughtExc = false;
    try {
      testFS.setFeatureValue(arrayFsWithSubtypeTypeFeat, testFS);
    } catch (CASRuntimeException e) {
      caughtExc = true;
      assertTrue(e.getError() == CASRuntimeException.INAPPROP_RANGE);
    }
    assertTrue(caughtExc);
  }
  
  public void testSetFeatureValue() {
    FeatureStructure token = this.cas.createFS(this.tokenType);
    assertTrue(token.getFeatureValue(this.tokenTypeFeat) == null);
    assertTrue(token.getStringValue(this.lemmaFeat) == null);
    boolean caughtExc = false;
    try {
      token.getFeatureValue(this.sentLenFeat);
    } catch (CASRuntimeException e) {
      caughtExc = true;
      assertTrue(e.getError() == CASRuntimeException.INAPPROP_FEAT);
    }
    assertTrue(caughtExc);
    FeatureStructure word = this.cas.createFS(wordType);
    token.setFeatureValue(tokenTypeFeat, word);
    caughtExc = false;
    try {
      token.setFeatureValue(lemmaFeat, word);
    } catch (CASRuntimeException e) {
      caughtExc = true;
      assertTrue(e.getError() == CASRuntimeException.INAPPROP_RANGE);
    }
    assertTrue(caughtExc);

    try {
      token.setFeatureValue(tokenTypeFeat, null);
    } catch (CASRuntimeException e) {
      assertTrue(false);
    }

    caughtExc = false;
    try {
      token.setFeatureValue(startFeature, null);
    } catch (CASRuntimeException e) {
      assertTrue(e.getError() == CASRuntimeException.PRIMITIVE_VAL_FEAT);
      caughtExc = true;
    }
    assertTrue(caughtExc);

    assertTrue(token.getStringValue(lemmaFeat) == null);
    String testString = "test";
    token.setStringValue(lemmaFeat, testString);
    assertTrue(token.getStringValue(lemmaFeat).equals(testString));
    testString = "";
    token.setStringValue(lemmaFeat, testString);
    assertTrue(token.getStringValue(lemmaFeat).equals(testString));
  }

  public void testSetFloatValue() {
    AnnotationFS token = (AnnotationFS) this.cas.createFS(tokenType);
    assertTrue(token.getFloatValue(tokenFloatFeat) == 0.0f);
    float f = -3.2f;
    token.setFloatValue(tokenFloatFeat, f);
    assertTrue(token.getFloatValue(tokenFloatFeat) == f);
    f = 51234.132f;
    token.setFloatValue(tokenFloatFeat, f);
    assertTrue(token.getFloatValue(tokenFloatFeat) == f);
    boolean caughtExc = false;
    try {
      token.setFloatValue(tokenTypeFeat, 0.0f);
    } catch (CASRuntimeException e) {
      caughtExc = true;
      assertTrue(e.getError() == CASRuntimeException.INAPPROP_RANGE);
    }
    assertTrue(caughtExc);
    assertTrue(token.getFloatValue(tokenFloatFeat) == f);
    caughtExc = false;
    try {
      token.setFloatValue(sentLenFeat, 0.0f);
    } catch (CASRuntimeException e) {
      caughtExc = true;
      assertTrue(e.getError() == CASRuntimeException.INAPPROP_FEAT);
    }
    assertTrue(caughtExc);
    assertTrue(token.getFloatValue(tokenFloatFeat) == f);
  }

  public void testSetIntValue() {
    AnnotationFS token = (AnnotationFS) this.cas.createFS(tokenType);
    assertTrue(token.getIntValue(startFeature) == 0);
    int i = 3;
    token.setIntValue(startFeature, i);
    assertTrue(token.getIntValue(startFeature) == i);
    i = -123456;
    token.setIntValue(startFeature, i);
    assertTrue(token.getIntValue(startFeature) == i);
    boolean caughtExc = false;
    try {
      token.setIntValue(tokenTypeFeat, 0);
    } catch (CASRuntimeException e) {
      caughtExc = true;
      assertTrue(e.getError() == CASRuntimeException.INAPPROP_RANGE);
    }
    assertTrue(caughtExc);
    assertTrue(token.getIntValue(startFeature) == i);
    caughtExc = false;
    try {
      token.setIntValue(sentLenFeat, 0);
    } catch (CASRuntimeException e) {
      caughtExc = true;
      assertTrue(e.getError() == CASRuntimeException.INAPPROP_FEAT);
    }
    assertTrue(caughtExc);
    assertTrue(token.getIntValue(startFeature) == i);
  }

  public void testCreateCasFromTypeSystem() {
    // TODO discuss with Thilo
    // CAS newCAS = null;
    // try {
    // TCASMgr tcasMgr = TCASFactory.createTCAS(10000,
    // this.cas.getTypeSystem());
    // tcasMgr.initTCASIndexes();
    // tcasMgr.getIndexRepositoryMgr().commit();
    // newCAS = tcasMgr.getTCAS();
    // } catch (Exception e) {
    // e.printStackTrace();
    // assertTrue(false);
    // }
    CAS newCAS = this.cas;
    assertTrue(newCAS != null);
    FeatureStructure lp = newCAS.createFS(langPairType);
    assertTrue(lp != null);
    // Check that all strings are initially null.
    try {
      assertTrue(lp.getStringValue(lang1) == null);
    } catch (Exception e) {
      assertTrue(false);
    }
    try {
      assertTrue(lp.getStringValue(lang2) == null);
    } catch (Exception e) {
      assertTrue(false);
    }
    try {
      assertTrue(lp.getStringValue(descr) == null);
    } catch (Exception e) {
      assertTrue(false);
    }
    // FeatureStructure topFS = newCAS.createFS(topType);
    String val = "Some string.";
    try {
      lp.setStringValue(descr, val);
      assertTrue(val.equals(lp.getStringValue(descr)));
    } catch (CASRuntimeException e) {
      assertTrue(false);
    }
    try {
      lp.setStringValue(descr, null);
      assertTrue(lp.getStringValue(descr) == null);
    } catch (CASRuntimeException e) {
      assertTrue(false);
    }
    try {
      lp.setStringValue(lang1, CASTestSetup.GROUP_1_LANGUAGES[0]);
      lp.setStringValue(lang2, CASTestSetup.GROUP_2_LANGUAGES[2]);
    } catch (Exception e) {
      assertTrue(false);
    }
    boolean exc = false;
    try {
      lp.setStringValue(lang1, CASTestSetup.GROUP_2_LANGUAGES[0]);
    } catch (CASRuntimeException e) {
      assertTrue(e.getError() == CASRuntimeException.ILLEGAL_STRING_VALUE);
      exc = true;
    }
    assertTrue(exc);
    exc = false;
    try {
      lp.setStringValue(lang2, val);
    } catch (CASRuntimeException e) {
      assertTrue(e.getError() == CASRuntimeException.ILLEGAL_STRING_VALUE);
      exc = true;
    }
    assertTrue(exc);

    LowLevelCAS llc = newCAS.getLowLevelCAS();
    LowLevelTypeSystem llts = llc.ll_getTypeSystem();
    final int tokenTypeCode = llts.ll_getCodeForType(tokenType);
    final int addr = llc.ll_createFS(tokenTypeCode);
    final int lemmaFeatCode = llts.ll_getCodeForFeature(lemmaFeat);
    llc.ll_setStringValue(addr, lemmaFeatCode, "test", true);
    assertTrue(llc.ll_getCharBufferValueSize(addr, lemmaFeatCode) == 4);

  }

  public void testStrings() {
    FeatureStructure lp = cas.createFS(langPairType);
    assertTrue(lp != null);
    // Check that all strings are initially null.
    try {
      assertTrue(lp.getStringValue(lang1) == null);
    } catch (Exception e) {
      assertTrue(false);
    }
    try {
      assertTrue(lp.getStringValue(lang2) == null);
    } catch (Exception e) {
      assertTrue(false);
    }
    try {
      assertTrue(lp.getStringValue(descr) == null);
    } catch (Exception e) {
      assertTrue(false);
    }
    // FeatureStructure topFS = cas.createFS(topType);
    String val = "Some string.";
    try {
      lp.setStringValue(descr, val);
      assertTrue(val.equals(lp.getStringValue(descr)));
    } catch (CASRuntimeException e) {
      assertTrue(false);
    }
    try {
      lp.setStringValue(descr, null);
      assertTrue(lp.getStringValue(descr) == null);
    } catch (CASRuntimeException e) {
      assertTrue(false);
    }
    try {
      lp.setStringValue(lang1, CASTestSetup.GROUP_1_LANGUAGES[0]);
      lp.setStringValue(lang2, CASTestSetup.GROUP_2_LANGUAGES[2]);
    } catch (Exception e) {
      assertTrue(false);
    }
    boolean exc = false;
    try {
      lp.setStringValue(lang1, CASTestSetup.GROUP_2_LANGUAGES[0]);
    } catch (CASRuntimeException e) {
      assertTrue(e.getError() == CASRuntimeException.ILLEGAL_STRING_VALUE);
      exc = true;
    }
    assertTrue(exc);
    exc = false;
    try {
      lp.setStringValue(this.lang2, val);
    } catch (CASRuntimeException e) {
      assertTrue(e.getError() == CASRuntimeException.ILLEGAL_STRING_VALUE);
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

  }

  public void testEquals() {
    // ???
  }

  public void testToString() {
    FeatureStructure listFS = this.cas.createFS(this.neListType);
    listFS.setFeatureValue(this.tlFeature, listFS);
    // System.out.println(listFS.toString());

    FeatureStructure value = this.cas.createFS(this.tokenType);
    FeatureStructure newList = this.cas.createFS(this.neListType);
    newList.setFeatureValue(this.tlFeature, listFS);
    newList.setFeatureValue(this.hdFeature, value);
    listFS.setFeatureValue(this.hdFeature, value);
    // System.out.println("\n" + newList.toString());
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(FeatureStructureTest.class);
  }

}
