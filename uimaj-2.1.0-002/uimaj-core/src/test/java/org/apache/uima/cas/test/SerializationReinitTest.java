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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.internal.util.FileUtils;
import org.apache.uima.internal.util.TextStringTokenizer;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;

/**
 * Class comment for TokenizerTest.java goes here.
 * 
 */
public class SerializationReinitTest extends TestCase {

  public static final String TOKEN_TYPE = "Token";

  public static final String TOKEN_TYPE_FEAT = "type";

  public static final String TOKEN_TYPE_FEAT_Q = TOKEN_TYPE + TypeSystem.FEATURE_SEPARATOR
          + TOKEN_TYPE_FEAT;

  public static final String TOKEN_TYPE_TYPE = "TokenType";

  public static final String WORD_TYPE = "Word";

  public static final String SEP_TYPE = "Separator";

  public static final String EOS_TYPE = "EndOfSentence";

  public static final String SENT_TYPE = "Sentence";

  public static final String STRING_SUBTYPE_1 = "StringSubtype1";

  public static final String[] STR_1_VALS = { "test1", "test2" };

  private CASMgr casMgr;

  private CAS cas;

  private Type wordType;

  private Type separatorType;

  private Type eosType;

  private Type tokenType;

  private Feature tokenTypeFeature;

  private Type sentenceType;

  private Feature startFeature;

  private Feature endFeature;

  private Type strSub1;

  public SerializationReinitTest(String arg) {
    super(arg);
  }

  /**
   * @see junit.framework.TestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();
    casMgr = initCAS();
    cas = (CASImpl)casMgr;

    TypeSystem ts = cas.getTypeSystem();
    wordType = ts.getType(WORD_TYPE);
    // assert(wordType != null);
    separatorType = ts.getType(SEP_TYPE);
    eosType = ts.getType(EOS_TYPE);
    tokenType = ts.getType(TOKEN_TYPE);
    tokenTypeFeature = ts.getFeatureByFullName(TOKEN_TYPE_FEAT_Q);
    startFeature = ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_BEGIN);
    endFeature = ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_END);
    sentenceType = ts.getType(SENT_TYPE);
    strSub1 = ts.getType(STRING_SUBTYPE_1);
    assertTrue(strSub1 != null);
  }

  public void tearDown() {
    casMgr = null;
    cas = null;
    wordType = null;
    // assert(wordType != null);
    separatorType = null;
    eosType = null;
    tokenType = null;
    tokenTypeFeature = null;
    startFeature = null;
    endFeature = null;
    sentenceType = null;
    strSub1 = null;
  }

  // Initialize the first CAS.
  private static CASMgr initCAS() throws CASException {
    // Create an initial CASMgr from the factory.
    // CASMgr cas = CASFactory.createCAS();
    // assert(tsa != null);
    // Create a CASMgr. Ensures existence of AnnotationFS type.
    // CASMgr tcas = CASFactory.createCAS();
    CASMgr aCas = CASFactory.createCAS();
    try {
      CasCreationUtils.setupTypeSystem(aCas, (TypeSystemDescription) null);
    } catch (ResourceInitializationException e) {
      e.printStackTrace();
    }
    // Create a writable type system.
    TypeSystemMgr tsa = aCas.getTypeSystemMgr();
    // Add new types and features.
    Type topType = tsa.getTopType();
    Type annotType = tsa.getType(CAS.TYPE_NAME_ANNOTATION);
    // assert(annotType != null);
    tsa.addType(SENT_TYPE, annotType);
    Type tokenType = tsa.addType(TOKEN_TYPE, annotType);
    Type tokenTypeType = tsa.addType(TOKEN_TYPE_TYPE, topType);
    tsa.addType(WORD_TYPE, tokenTypeType);
    tsa.addType(SEP_TYPE, tokenTypeType);
    tsa.addType(EOS_TYPE, tokenTypeType);
    tsa.addFeature(TOKEN_TYPE_FEAT, tokenType, tokenTypeType);
    tsa.addStringSubtype(STRING_SUBTYPE_1, STR_1_VALS);
    // Commit the type system.
    ((CASImpl) aCas).commitTypeSystem();
    // assert(tsa.isCommitted());
    // // Create the CAS indexes.
    // tcas.initCASIndexes();
    // Create the Base indexes.
    try {
      aCas.initCASIndexes();
    } catch (CASException e) {
      e.printStackTrace();
    }

    // Commit the index repository.
    aCas.getIndexRepositoryMgr().commit();
    // assert(cas.getIndexRepositoryMgr().isCommitted());

    // Create the default text Sofa and return CAS view
    return (CASMgr) aCas.getCAS().getCurrentView();
  }

  public void testReset() {
    cas.reset();
    casMgr.enableReset(false);
    boolean exc = false;
    try {
      cas.reset();
    } catch (CASAdminException e) {
      assertTrue(e.getError() == CASAdminException.FLUSH_DISABLED);
      exc = true;
    }
    assertTrue(exc);
    casMgr.enableReset(true);
    cas.reset();
  }

  // Tokenize text.
  private void tokenize() throws Exception {
    // System.out.println("Tokenizing text.");

    // Create FSs for the token types.
    FeatureStructure wordFS = cas.createFS(wordType);
    FeatureStructure sepFS = cas.createFS(separatorType);
    FeatureStructure eosFS = cas.createFS(eosType);

    String text = cas.getDocumentText();
    TextStringTokenizer tokenizer = new TextStringTokenizer(text);
    tokenizer.setSeparators("/-*&@");
    tokenizer.addWhitespaceChars(",");
    tokenizer.setEndOfSentenceChars(".!?");
    tokenizer.setShowWhitespace(false);
    int tokenTypeCode;
    int wordCounter = 0;
    int sepCounter = 0;
    int endOfSentenceCounter = 0;
    AnnotationFS tokenAnnot;
    while (tokenizer.isValid()) {
      tokenAnnot = cas.createAnnotation(tokenType, tokenizer.getTokenStart(), tokenizer
              .getTokenEnd());
      tokenTypeCode = tokenizer.getTokenType();
      switch (tokenTypeCode) {
        case TextStringTokenizer.EOS: {
          ++endOfSentenceCounter;
          tokenAnnot.setFeatureValue(tokenTypeFeature, eosFS);
          break;
        }
        case TextStringTokenizer.SEP: {
          ++sepCounter;
          tokenAnnot.setFeatureValue(tokenTypeFeature, sepFS);
          break;
        }
        case TextStringTokenizer.WSP: {
          break;
        }
        case TextStringTokenizer.WCH: {
          ++wordCounter;
          tokenAnnot.setFeatureValue(tokenTypeFeature, wordFS);
          // if ((wordCounter % 100000) == 0) {
          // System.out.println("Number of words tokenized: " + wordCounter);
          // }
          break;
        }
        default: {
          throw new Exception("Something went wrong, fire up that debugger!");
        }
      }
      cas.getIndexRepository().addFS(tokenAnnot);
      tokenizer.setToNext();
      // System.out.println("Token: " + tokenizer.nextToken());
    }
    // time = System.currentTimeMillis() - time;
    // System.out.println("Number of words: " + wordCounter);
    // int allTokens = wordCounter + sepCounter + endOfSentenceCounter;
    // System.out.println("Number of tokens: " + allTokens);
    // System.out.println("Time used: " + new TimeSpan(time));

    // FSIterator it = cas.getAnnotationIndex(tokenType).iterator();
    // int count = 0;
    // while (it.isValid()) {
    // ++count;
    // it.moveToNext();
    // }
    // System.out.println("Number of tokens in index: " + count);
  }

  // Very (!) primitive EOS detection.
  private void createSentences() throws CASException {
    // TypeSystem ts = cas.getTypeSystem();
    // Type eosType = ts.getType(EOS_TYPE);
    // Type tokenType = ts.getType(TOKEN_TYPE);
    // //assert(tokenType != null);
    // Type sentenceType = ts.getType(SENT_TYPE);
    // Feature tokenTypeFeature = ts.getFeature(TOKEN_TYPE_FEAT);
    // Feature startFeature = ts.getFeature(CAS.START_FEAT);
    // Feature endFeature = ts.getFeature(CAS.END_FEAT);

    // System.out.println("\nCreating sentence annotations.");

    // Get a handle to the index repository.
    FSIndexRepository indexRepository = cas.getIndexRepository();
    // assert(indexRepository != null);
    Iterator labelIt = indexRepository.getLabels();
    assertTrue(labelIt != null);
    // Get the standard index for tokens.
    FSIndex tokenIndex = cas.getAnnotationIndex(tokenType);
    // assert(tokenIndex != null);
    // Get an iterator over tokens.
    FSIterator it = tokenIndex.iterator();
    // assert(it != null);
    // Now create sentences. We do this as follows: a sentence starts where
    // the first token after an EOS starts, and ends with an EOS.
    long time = System.currentTimeMillis();
    int endOfSentenceCounter = 0;
    it.moveToFirst();
    boolean lookForStart = true;
    int start = 0, end; // Initialize start to pacify compiler.
    FeatureStructure tokenFS, sentFS;
    while (it.isValid()) {
      if (lookForStart) {
        // If we're looking for the start of a sentence, just grab the start
        // of the current FS.
        start = it.get().getIntValue(startFeature);
        lookForStart = false;
      } else {
        // Check if we've reached the end of a sentence.
        tokenFS = it.get();
        if (tokenFS.getFeatureValue(tokenTypeFeature).getType() == eosType) {
          end = tokenFS.getIntValue(endFeature);
          sentFS = cas.createFS(sentenceType);
          sentFS.setIntValue(startFeature, start);
          sentFS.setIntValue(endFeature, end);
          cas.getIndexRepository().addFS(sentFS);
          ++endOfSentenceCounter;
          lookForStart = true;
        }
      }
      it.moveToNext();
    }
    time = System.currentTimeMillis() - time;
    // System.out.println("Created " + endOfSentenceCounter + " sentences: " + new TimeSpan(time));
  }
  
  private static final Pattern nlPattern = Pattern.compile("(?m)(.*?$)");
  /**
   * Test driver.
   */
  public void testMain() throws Exception {

    // System.out.println("Setting up CAS.");
    // Create the initial CAS.
    long time = System.currentTimeMillis();
    time = System.currentTimeMillis() - time;
    // System.out.println("CAS set up: " + new TimeSpan(time));

    time = System.currentTimeMillis();
    // Read the document into a String. I'm sure there are better ways to
    File textFile = JUnitExtension.getFile("data/moby.txt");
    String moby = FileUtils.file2String(textFile);
    // String moby = file2String(System.getProperty("cas.data.test") + "moby.txt");
    String line;
//    BufferedReader br = new BufferedReader(new StringReader(moby));
    StringBuffer buf = new StringBuffer(10000);
    ArrayList docs = new ArrayList();
    Matcher m = nlPattern.matcher(moby);
    while (m.find()) {
      line = m.group();
      if (line.startsWith(".. <p")) {
        docs.add(buf.toString());
        buf.setLength(0);
      } else {
        buf.append(line + "\n");
      }
    }
//    while ((line = br.readLine()) != null) {
//      if (line.startsWith(".. <p")) {
//        docs.add(buf.toString());
//        buf = new StringBuffer();
//      } else {
//        buf.append(line + "\n");
//      }
//    }
    m.appendTail(buf);
    docs.add(buf.toString());
    buf = null;

    final int numDocs = docs.size();
    final int max = 30;
    int docCount = 0;
    long overallTime = System.currentTimeMillis();
    int numTok, numSent;
    CASSerializer cs;
    while (docCount < max) {
      for (int i = 0; i < numDocs && docCount < max; i++) {
        // System.out.println("Processing document: " + i);
        // Set document text in first CAS.
        cas.setDocumentText((String) docs.get(i));

        tokenize();
        numTok = cas.getAnnotationIndex(tokenType).size();
        assertTrue(numTok > 0);
        // System.out.println(" Number of tokens: " + numTok);

        // System.out.println("Serializing...");
        cs = Serialization.serializeCAS(cas);
        cas = Serialization.createCAS(casMgr, cs);

        assertTrue(numTok == cas.getAnnotationIndex(tokenType).size());

        createSentences();
        numSent = cas.getAnnotationIndex(sentenceType).size();
        assertTrue(numSent > 0);
        // System.out.println(" Number of sentences: " + numSent);

        // System.out.println("Serializing...");
        cs = Serialization.serializeCAS(cas);
        cas = Serialization.createCAS(casMgr, cs);

        assertTrue(numTok == cas.getAnnotationIndex(tokenType).size());
        assertTrue(numSent == cas.getAnnotationIndex(sentenceType).size());

        // System.out.println("Serializing...");
        cs = Serialization.serializeCAS(cas);
        cas = Serialization.createCAS(casMgr, cs);

        assertTrue(numTok == cas.getAnnotationIndex(tokenType).size());
        assertTrue(numSent == cas.getAnnotationIndex(sentenceType).size());
        // System.out.println(" Verify: " + numTok + " tokens, " + numSent + " sentences.");

        casMgr.reset();

        ++docCount;
      }
      // System.out.println("Number of documents processed: " + docCount);
    }
    overallTime = System.currentTimeMillis() - overallTime;
    // System.out.println("Time taken over all: " + new TimeSpan(overallTime));

  }

  /**
   * Test setCAS().
   */
  public void testSetCAS() throws Exception {

    // Read the document into a String. 
    File textFile = JUnitExtension.getFile("data/moby.txt");
    String moby = FileUtils.file2String(textFile);
    // String moby = file2String(System.getProperty("cas.data.test") + "moby.txt");
    String line;
//    BufferedReader br = new BufferedReader(new StringReader(moby));
    StringBuffer buf = new StringBuffer(10000);
    ArrayList docs = new ArrayList();
    Matcher m = nlPattern.matcher(moby);
    while (m.find()) {
      line = m.group();
      if (line.startsWith(".. <p")) {
        docs.add(buf.toString());
        buf.setLength(0);
      } else {
        buf.append(line + "\n");
      }
    }
    
//    while ((line = br.readLine()) != null) {
//      if (line.startsWith(".. <p")) {
//        docs.add(buf.toString());
//        buf = new StringBuffer();
//      } else {
//        buf.append(line + "\n");
//      }
//    }
//    docs.add(buf.toString());
    m.appendTail(buf);
    docs.add(buf.toString()); 
    buf = null;

    final int numDocs = docs.size();
    final int max = 30;
    int docCount = 0;
    long overallTime = System.currentTimeMillis();
    int numTok, numSent;
    while (docCount < max) {
      for (int i = 0; i < numDocs && docCount < max; i++) {
        // System.out.println("Processing document: " + i);
        // Set document text in first CAS.
        cas.setDocumentText((String) docs.get(i));

        tokenize();
        numTok = cas.getAnnotationIndex(tokenType).size();
        assertTrue(numTok > 0);
        // System.out.println(" Number of tokens: " + numTok);

        // System.out.println("Serializing...");
        // CASMgr casMgr = CASFactory.createCAS();
        // casMgr.setCAS(cas);
        // cas = (CAS) casMgr.getCAS();
        CASMgr realCasMgr = CASFactory.createCAS();
        realCasMgr.setCAS(((CASImpl) cas).getBaseCAS());
        cas = ((CASImpl) realCasMgr).getCurrentView();
        casMgr = (CASMgr) cas;

        assertTrue(numTok == cas.getAnnotationIndex(tokenType).size());

        createSentences();
        numSent = cas.getAnnotationIndex(sentenceType).size();
        assertTrue(numSent > 0);
        // System.out.println(" Number of sentences: " + numSent);

        // System.out.println("Serializing...");
        // casMgr = CASFactory.createCAS();
        // casMgr.setCAS(cas);
        // cas = (CAS) casMgr.getCAS();
        realCasMgr = CASFactory.createCAS();
        realCasMgr.setCAS(((CASImpl) cas).getBaseCAS());
        cas = ((CASImpl) realCasMgr).getCurrentView();
        casMgr = (CASMgr) cas;

        assertTrue(numTok == cas.getAnnotationIndex(tokenType).size());
        assertTrue(numSent == cas.getAnnotationIndex(sentenceType).size());

        // System.out.println("Serializing...");
        // casMgr = CASFactory.createCAS();
        // casMgr.setCAS(cas);
        // cas = (CAS) casMgr.getCAS();
        realCasMgr = CASFactory.createCAS();
        realCasMgr.setCAS(((CASImpl) cas).getBaseCAS());
        cas = ((CASImpl) realCasMgr).getCurrentView();
        casMgr = (CASMgr) cas;

        assertTrue(numTok == cas.getAnnotationIndex(tokenType).size());
        assertTrue(numSent == cas.getAnnotationIndex(sentenceType).size());
        // System.out.println(" Verify: " + numTok + " tokens, " + numSent + " sentences.");

        casMgr.reset();

        ++docCount;
      }
      // System.out.println("Number of documents processed: " + docCount);
    }
    overallTime = System.currentTimeMillis() - overallTime;
    // System.out.println("Time taken over all: " + new TimeSpan(overallTime));

  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(SerializationReinitTest.class);
  }

}
