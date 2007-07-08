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

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;

/**
 * Class comment for FilteredIteratorTest.java goes here.
 * 
 */
public class AnnotationIteratorTest extends TestCase {

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

  private Feature endFeature;

  private Type sentenceType;

  /**
   * Constructor for FilteredIteratorTest.
   * 
   * @param arg0
   */
  public AnnotationIteratorTest(String arg0) {
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
    this.sepType = this.ts.getType(CASTestSetup.SEP_TYPE);
    assertTrue(this.sepType != null);
    this.eosType = this.ts.getType(CASTestSetup.EOS_TYPE);
    assertTrue(this.eosType != null);
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
    this.endFeature = this.ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_END);
    assertTrue(this.endFeature != null);
    this.sentenceType = this.ts.getType(CASTestSetup.SENT_TYPE);
    assertTrue(this.sentenceType != null);
  }

  public void tearDown() {
    this.cas = null;
    this.ts = null;
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
    this.endFeature = null;
    this.sentenceType = null;
  }

  public void testIterator1() {

    final String text = "aaaa bbbb cccc dddd aaaa bbbb cccc dddd aaaa bbbb cccc dddd ";

    try {
      this.cas.setDocumentText(text);
    } catch (CASRuntimeException e) {
      assertTrue(false);
    }

    int annotCount = 1; // Init with document annotation.
    // create token and sentence annotations
    for (int i = 0; i < text.length() - 5; i++) {
      ++annotCount;
      this.cas.getIndexRepository().addFS(this.cas.createAnnotation(this.tokenType, i, i + 5));
    }
    // for (int i = 0; i < text.length() - 5; i++) {
    // cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, i, i+5));
    // }
    for (int i = 0; i < text.length() - 10; i += 5) {
      ++annotCount;
      this.cas.getIndexRepository().addFS(this.cas.createAnnotation(this.sentenceType, i, i + 10));
    }

    int count;
    AnnotationIndex annotIndex = this.cas.getAnnotationIndex();
    FSIterator it = annotIndex.iterator(true);
    count = 0;
    while (it.isValid()) {
      ++count;
      it.moveToNext();
    }
    assertTrue(annotCount == count);
    // System.out.println("Size of ambiguous iterator: " + count);
    it = annotIndex.iterator(false);
    count = 0;
    while (it.isValid()) {
      ++count;
      it.moveToNext();
    }
    assertTrue(count == 1);
    // System.out.println("Size of unambiguous iterator: " + count);
    AnnotationFS bigAnnot = this.cas.createAnnotation(this.sentenceType, 10, 41);
    it = annotIndex.subiterator(bigAnnot, true, true);
    count = 0;
    while (it.isValid()) {
      ++count;
      // AnnotationFS a = (AnnotationFS) it.get();
      // System.out.println("Annotation from " + a.getBegin() + " to " + a.getEnd());
      it.moveToNext();
    }
    assertTrue(count == 32);
    count = 0;
    for (it.moveToLast(); it.isValid(); it.moveToPrevious()) {
      ++count;
    }
    assertTrue(count == 32);
    // System.out.println("Size of subiterator(true, true): " + count);
    it = annotIndex.subiterator(bigAnnot, false, true);
    count = 0;
    while (it.isValid()) {
      ++count;
      // AnnotationFS a = (AnnotationFS) it.get();
      // System.out.println("Annotation from " + a.getBegin() + " to " + a.getEnd());
      it.moveToNext();
    }
    assertTrue(count == 3);
    // System.out.println("Size of subiterator(false, true): " + count);
    count = 0;
    for (it.moveToLast(); it.isValid(); it.moveToPrevious()) {
      ++count;
    }
    assertTrue(count == 3);
    it = annotIndex.subiterator(bigAnnot, true, false);
    count = 0;
    while (it.isValid()) {
      ++count;
      // AnnotationFS a = (AnnotationFS) it.get();
      // System.out.println("Annotation from " + a.getBegin() + " to " + a.getEnd());
      it.moveToNext();
    }
    assertTrue(count == 39);
    // System.out.println("Size of subiterator(true, false): " + count);
    count = 0;
    for (it.moveToLast(); it.isValid(); it.moveToPrevious()) {
      ++count;
    }
    assertTrue(count == 39);
    it = annotIndex.subiterator(bigAnnot, false, false);
    count = 0;
    while (it.isValid()) {
      ++count;
      // AnnotationFS a = (AnnotationFS) it.get();
      // System.out.println("Annotation from " + a.getBegin() + " to " + a.getEnd());
      it.moveToNext();
    }
    assertTrue(count == 4);
    // System.out.println("Size of subiterator(false, false): " + count);
    count = 0;
    for (it.moveToLast(); it.isValid(); it.moveToPrevious()) {
      ++count;
    }
    assertTrue(count == 4);
    AnnotationFS sent = (AnnotationFS) this.cas.getAnnotationIndex(this.sentenceType).iterator().get();
    it = annotIndex.subiterator(sent, false, true);
    count = 0;
    while (it.isValid()) {
      ++count;
      // AnnotationFS a = (AnnotationFS) it.get();
      // System.out.println("Annotation from " + a.getBegin() + " to " + a.getEnd());
      it.moveToNext();
    }
    assertTrue(count == 2);
    // System.out.println("Size of subiterator(false, false): " + count);
    count = 0;
    for (it.moveToLast(); it.isValid(); it.moveToPrevious()) {
      ++count;
    }
    assertTrue(count == 2);
  }
  
  public void testIncorrectIndexTypeException() {
    boolean caughtException = false;
    try {
      this.cas.getAnnotationIndex(this.stringType);
    } catch (CASRuntimeException e) {
//      e.printStackTrace();
      caughtException = true;
    }
    assertTrue(caughtException);
    
    caughtException = false;
    try {
    	AnnotationIndex ai = this.cas.getAnnotationIndex(ts.getType(CASTestSetup.TOKEN_TYPE_TYPE));
    } catch (CASRuntimeException e) {
    	caughtException = true;
    }
    assertTrue(caughtException);
    try {
      this.cas.getAnnotationIndex(this.tokenType);
    } catch (CASRuntimeException e) {
      assertTrue(false);
    }
  }

  public static void main(String[] args) {
    AnnotationIteratorTest test = new AnnotationIteratorTest(null);
    test.run();
  }

}
