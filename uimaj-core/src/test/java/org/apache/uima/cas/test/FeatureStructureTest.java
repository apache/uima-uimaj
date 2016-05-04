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
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.impl.LowLevelTypeSystem;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * Class comment for FeatureStructureTest.java goes here.
 * 
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

	public void testErrorDerefDifferentCAS() {
	  CAS cas2 = CASInitializer.initCas(new CASTestSetup());
	  Type tokenType1 = this.ts.getType(CASTestSetup.TOKEN_TYPE);
	  Feature tokenTypeFeature = this.ts.getFeatureByFullName(CASTestSetup.TOKEN_TYPE + ":" + CASTestSetup.TOKEN_TYPE_FEAT);
	  FeatureStructure fs1 = cas2.createFS(tokenType1);
	  FeatureStructure fs = cas.createFS(tokenType1);
	  boolean caught = false;
	  try {
   	  fs.setFeatureValue(tokenTypeFeature, fs1);
	  } catch (Exception e) {
	    assertTrue( e instanceof CASRuntimeException);
	    caught = true;
	  }
	  assertTrue(caught);
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

	public void testSetFeatureValue() {
		FeatureStructure token = this.cas.createFS(this.tokenType);
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
			assertTrue(e.getMessageKey().equals(CASRuntimeException.PRIMITIVE_VAL_FEAT));
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
    LowLevelCAS llcas = cas.getLowLevelCAS();
    token.setFeatureValue(tokenTypeFeat, word);
    int fsRef = ((FeatureStructureImpl)token).getAddress();
    int fc = ((FeatureImpl)tokenTypeFeat).getCode();
    assertEquals(llcas.ll_getIntValue(fsRef, fc), word.hashCode());
    FeatureStructureImpl word2 = cas.createFS(wordType);
    llcas.ll_setIntValue(fsRef, fc, word2.hashCode());
    assertEquals(token.getFeatureValue(tokenTypeFeat), word2);
	}

	public void testSetFloatValue() {
		AnnotationFS token = (AnnotationFS) this.cas.createFS(this.tokenType);
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
			assertTrue(e.getMessageKey().equals(CASRuntimeException.INAPPROP_FEAT));
		}
		assertTrue(caughtExc);
		assertTrue(token.getFloatValue(this.tokenFloatFeat) == f);
	}

	public void testSetIntValue() {
		AnnotationFS token = (AnnotationFS) this.cas.createFS(this.tokenType);
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
