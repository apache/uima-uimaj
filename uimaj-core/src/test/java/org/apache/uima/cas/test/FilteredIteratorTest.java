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

import java.util.ArrayList;

import junit.framework.TestCase;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.ConstraintFactory;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.FSStringConstraint;
import org.apache.uima.cas.FSTypeConstraint;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeaturePath;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.FSIndexFlat;
import org.apache.uima.cas.impl.FSIteratorWrapper;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * Class comment for FilteredIteratorTest.java goes here.
 * 
 */
public class FilteredIteratorTest extends TestCase {

  private CAS cas;

  private TypeSystem ts;

  private Type stringType;

  private Type tokenType;

  private Type intType;

  private Type tokenTypeType;

  private Type wordType;

  private Type sepType;

  private Type eosType;

  private Feature tokenTypeFeat;

  private Feature lemmaFeat;

  private Feature sentLenFeat;

  private Feature tokenFloatFeat;

  private Feature startFeature;

  private Type sentenceType;

  private Type annotationType;

  /**
   * Constructor for FilteredIteratorTest.
   * 
   * @param arg0
   */
  public FilteredIteratorTest(String arg0) {
    super(arg0);
  }

  public void setUp() {
    try {
      this.cas = CASInitializer.initCas(new CASTestSetup());
      assertTrue(cas != null);
      this.ts = this.cas.getTypeSystem();
      assertTrue(ts != null);
    } catch (Exception e) {
      e.printStackTrace();
      assertTrue(false);
    }
    this.stringType = ts.getType(CAS.TYPE_NAME_STRING);
    assertTrue(stringType != null);
    this.tokenType = ts.getType(CASTestSetup.TOKEN_TYPE);
    assertTrue(stringType != null);
    this.intType = ts.getType(CAS.TYPE_NAME_INTEGER);
    assertTrue(intType != null);
    this.tokenTypeType = ts.getType(CASTestSetup.TOKEN_TYPE_TYPE);
    assertTrue(tokenTypeType != null);
    this.wordType = ts.getType(CASTestSetup.WORD_TYPE);
    assertTrue(wordType != null);
    this.sepType = ts.getType(CASTestSetup.SEP_TYPE);
    assertTrue(sepType != null);
    this.eosType = ts.getType(CASTestSetup.EOS_TYPE);
    assertTrue(eosType != null);
    this.tokenTypeFeat = ts.getFeatureByFullName(CASTestSetup.TOKEN_TYPE_FEAT_Q);
    assertTrue(tokenTypeFeat != null);
    this.lemmaFeat = ts.getFeatureByFullName(CASTestSetup.LEMMA_FEAT_Q);
    assertTrue(lemmaFeat != null);
    this.sentLenFeat = ts.getFeatureByFullName(CASTestSetup.SENT_LEN_FEAT_Q);
    assertTrue(sentLenFeat != null);
    this.tokenFloatFeat = ts.getFeatureByFullName(CASTestSetup.TOKEN_FLOAT_FEAT_Q);
    assertTrue(tokenFloatFeat != null);
    this.startFeature = ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_BEGIN);
    assertTrue(startFeature != null);
    this.sentenceType = ts.getType(CASTestSetup.SENT_TYPE);
    assertTrue(sentenceType != null);
    this.annotationType = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    assertTrue(annotationType != null);
  }

  public void tearDown() {
    this.cas = null;
    this.ts = null;
    this.stringType = null;
    this.tokenType = null;
    this.intType = null;
    this.tokenTypeType = null;
    this.wordType = null;
    this.sepType = null;
    this.eosType = null;
    this.tokenTypeFeat = null;
    this.lemmaFeat = null;
    this.sentLenFeat = null;
    this.tokenFloatFeat = null;
    this.startFeature = null;
    this.sentenceType = null;
    this.annotationType = null;
  }

  public void testIterator1() {

    try {
      // cas.setDocumentText("A test."); can't set document text twice
    } catch (CASRuntimeException e) {
      assertTrue(false);
    }

    try {
      cas.setDocumentText("This is a test.");
    } catch (CASRuntimeException e) {
      assertTrue(false);
    }
    // create token and sentence annotations
    cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, 0, 4));
    cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, 5, 7));
    cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, 8, 9));
    cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, 10, 14));
    cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, 14, 15));
    cas.getIndexRepository().addFS(cas.createAnnotation(sentenceType, 0, 15));

    iterAndCount1(false);
    
    if (FSIndexFlat.enabled) {
      expandBeyondFlatThreshold(6);  // enables flat iterator
      iterAndCount1(true);
    }
    
  }
  
  private void iterAndCount1(boolean isFlat) {
    // create filtered iterator over Tokens only
    FSIterator<AnnotationFS> it = cas.getAnnotationIndex().iterator();  // always non-flat because just did index update
    
    FSTypeConstraint constraint = cas.getConstraintFactory().createTypeConstraint();
    constraint.add(tokenType);
    
    it = cas.createFilteredIterator(it, constraint);
    
    // do iteration
    while (it.isValid()) {
      AnnotationFS a = it.get();
      assertTrue(a.getType().equals(tokenType));
      // System.out.println("Annotation type: " + a.getType().getName());
      // System.out.println("Covered text: " + a.getCoveredText());
      it.moveToNext();
    }

    // Count number of annotations.
    it = cas.getAnnotationIndex().iterator();  
    assertTrue( (isFlat) ? (it instanceof FSIndexFlat.FSIteratorFlat) : it instanceof FSIteratorWrapper);
    int countAll = 0;
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      ++countAll;
    }

    // create filtered iterator over annotations
    it = cas.getAnnotationIndex().iterator();
    constraint = cas.getConstraintFactory().createTypeConstraint();
    constraint.add(annotationType);
    it = cas.createFilteredIterator(it, constraint);

    // do iteration
    int countFiltered = 0;
    while (it.isValid()) {
      AnnotationFS a = it.get();
      assertTrue(ts.subsumes(annotationType, a.getType()));
      // System.out.println("Annotation type: " + a.getType().getName());
      // System.out.println("Covered text: " + a.getCoveredText());
      it.moveToNext();
      ++countFiltered;
    }
    assertTrue(countAll == countFiltered);
  }

  public void testIterator1a() {

    try {
      // cas.setDocumentText("A test."); can't set document text twice!
    } catch (CASRuntimeException e) {
      assertTrue(false);
    }

    try {
      cas.setDocumentText("This is a test.");
    } catch (CASRuntimeException e) {
      assertTrue(false);
    }
    // create token and sentence annotations
    cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, 0, 4));
    cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, 5, 7));
    cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, 8, 9));
    cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, 10, 14));
    cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, 14, 15));
    cas.getIndexRepository().addFS(cas.createAnnotation(sentenceType, 0, 15));

    iterAndCount1a();
    
    expandBeyondFlatThreshold(6);  // enables flat iterator
    iterAndCount1a();
  }
  
  private void iterAndCount1a() {
    // create filtered iterator over Tokens only
    FSIterator<AnnotationFS> it = cas.getAnnotationIndex().iterator();
    FSTypeConstraint constraint = cas.getConstraintFactory().createTypeConstraint();
    constraint.add(tokenType.getName());
    it = cas.createFilteredIterator(it, constraint);

    // do iteration
    while (it.isValid()) {
      AnnotationFS a = it.get();
      assertTrue(a.getType().equals(tokenType));
      // System.out.println("Annotation type: " + a.getType().getName());
      // System.out.println("Covered text: " + a.getCoveredText());
      it.moveToNext();
    }

  }

  // test uses constraint compiler
  /*
   * public void testIterator1b() {
   * 
   * try { cas.setDocumentText("A test."); } catch (CASRuntimeException e) { assertTrue(false); }
   * ((CASMgr) cas).enableSetText(false); boolean exc = false; try { cas.setDocumentText("A
   * test."); } catch (CASRuntimeException e) { assertTrue(e.getError() ==
   * CASRuntimeException.SET_DOC_TEXT_DISABLED); exc = true; } assertTrue(exc); ((CASMgr)
   * cas).enableSetText(true);
   * 
   * try { ((CASMgr) cas).setDocumentText("This is a test."); } catch (CASRuntimeException e) {
   * assertTrue(false); } //create token and sentence annotations
   * cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, 0, 4));
   * cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, 5, 7));
   * cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, 8, 9));
   * cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, 10, 14));
   * cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, 14, 15));
   * cas.getIndexRepository().addFS(cas.createAnnotation(sentenceType, 0, 15));
   * 
   * //create filtered iterator over Tokens only FSIterator it =
   * cas.getAnnotationIndex().iterator(); // FSTypeConstraint constraint = //
   * cas.getConstraintFactory().createTypeConstraint(); // constraint.add(tokenType.getName());
   * 
   * FSMatchConstraint constraint = null; try { ConstraintParser parser =
   * ConstraintParserFactory.getDefaultConstraintParser(); constraint = parser.parse("isa " +
   * tokenType.getName()); } catch (Exception e) { e.printStackTrace(); assertTrue(false); }
   * 
   * it = cas.createFilteredIterator(it, constraint);
   * 
   * //do iteration while (it.isValid()) { AnnotationFS a = (AnnotationFS) it.get();
   * assertTrue(a.getType().equals(tokenType)); // System.out.println("Annotation type: " +
   * a.getType().getName()); // System.out.println("Covered text: " + a.getCoveredText());
   * it.moveToNext(); } }
   */

  public void testIterator2() {
    try {
      cas.setDocumentText("This is a test with the word \"the\" in it.");

      // create token and sentence annotations
      String type1 = "type1";
      String type2 = "type2";
      AnnotationFS token;
      token = cas.createAnnotation(tokenType, 0, 4);
      token.setStringValue(lemmaFeat, type1);
      cas.getIndexRepository().addFS(token);
      token = cas.createAnnotation(tokenType, 5, 7);
      token.setStringValue(lemmaFeat, "the");
      cas.getIndexRepository().addFS(token);
      token = cas.createAnnotation(tokenType, 8, 9);
      token.setStringValue(lemmaFeat, type2);
      cas.getIndexRepository().addFS(token);
      token = cas.createAnnotation(tokenType, 10, 14);
      token.setStringValue(lemmaFeat, type1);
      cas.getIndexRepository().addFS(token);
      token = cas.createAnnotation(tokenType, 14, 15);
      token.setStringValue(lemmaFeat, type1);
      cas.getIndexRepository().addFS(token);
      token = cas.createAnnotation(tokenType, 0, 15);
      token.setStringValue(lemmaFeat, type1);
      cas.getIndexRepository().addFS(token);

      iterAndCount2();
      
      expandBeyondFlatThreshold(6);  // enables flat iterator
      iterAndCount2();
          
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  private void iterAndCount2() {
    String lemma = "the";
    // create filtered iterator over Tokens of type 1
    FSIterator<AnnotationFS> it = cas.getAnnotationIndex(tokenType).iterator();
    FSStringConstraint type1Constraint = cas.getConstraintFactory().createStringConstraint();
    type1Constraint.equals(lemma);
    FeaturePath path = cas.createFeaturePath();
    path.addFeature(lemmaFeat);
    FSMatchConstraint cons = cas.getConstraintFactory().embedConstraint(path, type1Constraint);
    it = cas.createFilteredIterator(it, cons);

    int count = 0;
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      ++count;
    }

    // /////////////////////////////////////////////////////////////
    // Count instances of tokens with lemma "the".

    // Create an iterator over Token annotations.
    FSIndex<AnnotationFS> tokenIndex = cas.getAnnotationIndex(tokenType);
    FSIterator<AnnotationFS> tokenIt = tokenIndex.iterator();
    // Create a counter.
    int theCount = 0;
    // Iterate over the tokens.
    for (tokenIt.moveToFirst(); tokenIt.isValid(); tokenIt.moveToNext()) {
      AnnotationFS tok = tokenIt.get();
      if (tok.getStringValue(lemmaFeat).equals(lemma)) {
        ++theCount;
        // System.out.println("Found token: " + tok.getCoveredText());
      }
    }
    assertTrue(count == theCount);
    // System.out.println(
    // "Number of tokens with \"" + lemma + "\": " + theCount);
    // System.out.println("Number of tokens overall: " + tokenIndex.size());

    // System.out.println("Count: " + count);
    // assertTrue(count == 4);
  }
  
  public void testIterator2a() {
    try {
      cas.setDocumentText("This is a test with the word \"the\" in it.");

      // create token and sentence annotations
      String type1 = "type1";
      String type2 = "type2";
      AnnotationFS token;
      token = cas.createAnnotation(tokenType, 0, 4);
      token.setStringValue(lemmaFeat, type1);
      cas.getIndexRepository().addFS(token);
      token = cas.createAnnotation(tokenType, 5, 7);
      token.setStringValue(lemmaFeat, "the");
      cas.getIndexRepository().addFS(token);
      token = cas.createAnnotation(tokenType, 8, 9);
      token.setStringValue(lemmaFeat, type2);
      cas.getIndexRepository().addFS(token);
      token = cas.createAnnotation(tokenType, 10, 14);
      token.setStringValue(lemmaFeat, type1);
      cas.getIndexRepository().addFS(token);
      token = cas.createAnnotation(tokenType, 14, 15);
      token.setStringValue(lemmaFeat, type1);
      cas.getIndexRepository().addFS(token);
      token = cas.createAnnotation(tokenType, 0, 15);
      token.setStringValue(lemmaFeat, type1);
      cas.getIndexRepository().addFS(token);

      iterAndCount2a();
      
      expandBeyondFlatThreshold(6);  // enables flat iterator
      iterAndCount2a();
      
    } catch (Exception e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }
  
  private void iterAndCount2a() {
    String lemma = "the";
    FSIterator<AnnotationFS> it = cas.getAnnotationIndex(tokenType).iterator();
    FSStringConstraint type1Constraint = cas.getConstraintFactory().createStringConstraint();
    type1Constraint.equals(lemma);
    ArrayList<String> path = new ArrayList<String>();
    path.add(lemmaFeat.getShortName());
    FSMatchConstraint cons = cas.getConstraintFactory().embedConstraint(path, type1Constraint);
    it = cas.createFilteredIterator(it, cons);

    int count = 0;
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      ++count;
    }

    // /////////////////////////////////////////////////////////////
    // Count instances of tokens with lemma "the".

    // Create an iterator over Token annotations.
    FSIndex<AnnotationFS> tokenIndex = cas.getAnnotationIndex(tokenType);
    FSIterator<AnnotationFS> tokenIt = tokenIndex.iterator();
    // Create a counter.
    int theCount = 0;
    // Iterate over the tokens.
    for (tokenIt.moveToFirst(); tokenIt.isValid(); tokenIt.moveToNext()) {
      AnnotationFS tok = tokenIt.get();
      if (tok.getStringValue(lemmaFeat).equals(lemma)) {
        ++theCount;
        // System.out.println("Found token: " + tok.getCoveredText());
      }
    }
    assertTrue(count == theCount);
    // System.out.println(
    // "Number of tokens with \"" + lemma + "\": " + theCount);
    // System.out.println("Number of tokens overall: " + tokenIndex.size());

    // System.out.println("Count: " + count);
    // assertTrue(count == 4);

  }

  public void testIterator2b() {
    try {
      cas.setDocumentText("This is a test with the word \"the\" in it.");

      FeatureStructure wordFS = this.cas.createFS(wordType);
      FeatureStructure sepFS = this.cas.createFS(sepType);
      FeatureStructure eosFS = this.cas.createFS(eosType);

      // create token and sentence annotations
      String type1 = "type1";
      String type2 = "type2";
      AnnotationFS token;
      token = cas.createAnnotation(tokenType, 0, 4);
      token.setStringValue(lemmaFeat, type1);
      token.setFeatureValue(tokenTypeFeat, wordFS);
      cas.getIndexRepository().addFS(token);
      token = cas.createAnnotation(tokenType, 5, 7);
      token.setStringValue(lemmaFeat, "the");
      token.setFeatureValue(tokenTypeFeat, sepFS);
      cas.getIndexRepository().addFS(token);
      token = cas.createAnnotation(tokenType, 8, 9);
      token.setStringValue(lemmaFeat, type2);
      token.setFeatureValue(tokenTypeFeat, eosFS);
      cas.getIndexRepository().addFS(token);
      token = cas.createAnnotation(tokenType, 10, 14);
      token.setStringValue(lemmaFeat, type1);
      token.setFeatureValue(tokenTypeFeat, wordFS);
      cas.getIndexRepository().addFS(token);
      token = cas.createAnnotation(tokenType, 14, 15);
      token.setStringValue(lemmaFeat, type1);
      token.setFeatureValue(tokenTypeFeat, sepFS);
      cas.getIndexRepository().addFS(token);
      token = cas.createAnnotation(tokenType, 0, 15);
      token.setStringValue(lemmaFeat, type1);
      token.setFeatureValue(tokenTypeFeat, eosFS);
      cas.getIndexRepository().addFS(token);

      iterAndCount2b();
      
      expandBeyondFlatThreshold(6);  // enables flat iterator
      iterAndCount2b();
      
    } catch (Exception e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }
  
  private void iterAndCount2b() {
    FSIterator<AnnotationFS> it = cas.getAnnotationIndex(tokenType).iterator();

    ConstraintFactory cf = this.cas.getConstraintFactory();
    FSTypeConstraint tc = cf.createTypeConstraint();
    tc.add(sepType);
    tc.add(eosType.getName());
    ArrayList<String> path = new ArrayList<String>();
    path.add(tokenTypeFeat.getShortName());
    FSMatchConstraint cons = cf.embedConstraint(path, tc);
    it = this.cas.createFilteredIterator(it, cons);
    int count = 0;
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      ++count;
    }
    assertTrue(count == 4);

  }

  // test uses constraint compiler
  /*
   * public void testIterator2c() { try { ((CASMgr) cas).setDocumentText( "This is a test with the
   * word \"the\" in it.");
   * 
   * FeatureStructure wordFS = this.cas.createFS(wordType); FeatureStructure sepFS =
   * this.cas.createFS(sepType); FeatureStructure eosFS = this.cas.createFS(eosType);
   * 
   * //create token and sentence annotations String type1 = "type1"; String type2 = "type2";
   * AnnotationFS token; token = cas.createAnnotation(tokenType, 0, 4);
   * token.setStringValue(lemmaFeat, type1); token.setFeatureValue(tokenTypeFeat, wordFS);
   * cas.getIndexRepository().addFS(token); token = cas.createAnnotation(tokenType, 5, 7);
   * token.setStringValue(lemmaFeat, "the"); token.setFeatureValue(tokenTypeFeat, sepFS);
   * cas.getIndexRepository().addFS(token); token = cas.createAnnotation(tokenType, 8, 9);
   * token.setStringValue(lemmaFeat, type2); token.setFeatureValue(tokenTypeFeat, eosFS);
   * cas.getIndexRepository().addFS(token); token = cas.createAnnotation(tokenType, 10, 14);
   * token.setStringValue(lemmaFeat, type1); token.setFeatureValue(tokenTypeFeat, wordFS);
   * cas.getIndexRepository().addFS(token); token = cas.createAnnotation(tokenType, 14, 15);
   * token.setStringValue(lemmaFeat, type1); token.setFeatureValue(tokenTypeFeat, sepFS);
   * cas.getIndexRepository().addFS(token); token = cas.createAnnotation(tokenType, 0, 15);
   * token.setStringValue(lemmaFeat, type1); token.setFeatureValue(tokenTypeFeat, eosFS);
   * cas.getIndexRepository().addFS(token);
   * 
   * FSIterator it = cas.getAnnotationIndex(tokenType).iterator();
   * 
   * FSMatchConstraint cons = null; try { ConstraintParser parser =
   * ConstraintParserFactory.getDefaultConstraintParser(); cons = parser.parse(
   * tokenTypeFeat.getShortName() + " isa (" + sepType.getName() + "|" + eosType.getName() + ")"); }
   * catch (Exception e) { assertTrue(false); } it = this.cas.createFilteredIterator(it, cons); int
   * count = 0; for (it.moveToFirst(); it.isValid(); it.moveToNext()) { ++count; } assertTrue(count ==
   * 4); } catch (Exception e) { e.printStackTrace(); assertTrue(false); } }
   * 
   * public static void main(String[] args) { FilteredIteratorTest test = new
   * FilteredIteratorTest(null); test.run(); }
   */
  
  // add enough tokens to make the total be > THRESHOLD_FOR_FLATTENING, ii is the current number...
  // this is so that the flattening can happen 
  private void expandBeyondFlatThreshold(int ii) {
    int t = FSIndexFlat.THRESHOLD_FOR_FLATTENING;
    FeatureStructure wordFS = this.cas.createFS(wordType);
    for (int i = 0; i < t - ii; i++) {
      AnnotationFS token = cas.createAnnotation(tokenType, 99, 99);
      token.setStringValue(lemmaFeat, "dummytype");  // stuff to make the filter not throw null pointer exceptions
      token.setFeatureValue(tokenTypeFeat, wordFS);
      cas.getIndexRepository().addFS(token);
    }
  }
}
