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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.internal.util.TextStringTokenizer;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;

/**
 * Class comment for TokenizerTest.java goes here.
 * 
 */
public class SerializationNoMDTest extends TestCase {

  public static final String TOKEN_TYPE = "Token";

  public static final String TOKEN_TYPE_FEAT = "type";

  public static final String TOKEN_TYPE_FEAT_Q = TOKEN_TYPE + TypeSystem.FEATURE_SEPARATOR
          + TOKEN_TYPE_FEAT;

  public static final String TOKEN_TYPE_TYPE = "TokenType";

  public static final String WORD_TYPE = "Word";

  public static final String SEP_TYPE = "Separator";

  public static final String EOS_TYPE = "EndOfSentence";

  public static final String SENT_TYPE = "Sentence";

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

  public SerializationNoMDTest(String arg) {
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
  }

  public void tearDown() {
    casMgr = null;
    cas = null;
    wordType = null;
    separatorType = null;
    eosType = null;
    tokenType = null;
    tokenTypeFeature = null;
    startFeature = null;
    endFeature = null;
    sentenceType = null;

  }

  // Initialize the first CAS.
  private static CASMgr initCAS() {
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
  private void createSentences() {
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
    Iterator<String> labelIt = indexRepository.getLabels();
    assertTrue(labelIt != null);
    // Get the standard index for tokens.
    FSIndex<AnnotationFS> tokenIndex = cas.getAnnotationIndex(tokenType);
    // assert(tokenIndex != null);
    // Get an iterator over tokens.
    FSIterator<AnnotationFS> it = tokenIndex.iterator();
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

  // Check results.
  private void checkSentences() {
    TypeSystem ts = cas.getTypeSystem();
    Type localSentenceType = ts.getType(SENT_TYPE);
    // Feature tokenTypeFeature = ts.getFeatureByFullName(TOKEN_TYPE_FEAT);
    // Feature startFeature = ts.getFeatureByFullName(CAS.FEATURE_BASE_NAME_BEGIN);
    // Feature endFeature = ts.getFeatureByFullName(CAS.FEATURE_BASE_NAME_END);

    // Print the first few sentences.
    // System.out.println("\nThe first 10 sentences:\n");
    FSIndex<AnnotationFS> sentenceIndex = cas.getAnnotationIndex(localSentenceType);
    FSIterator<AnnotationFS> it = sentenceIndex.iterator();
    AnnotationFS sentFS;
    if (it.isValid()) {
      sentFS = it.get();
      assertTrue(sentFS.getCoveredText() != null);
    }
    // int counter = 0;
    String text = cas.getDocumentText();
    assertTrue(text != null);
    // while (it.isValid() && counter < 10) {
    // sentFS = (AnnotationFS)it.get();
    // System.out.println(
    // "Sentence: "
    // + sentFS.getCoveredText());
    // it.moveToNext();
    // ++counter;
    // }

    // Now get an iterator over all annotations.
    FSIndex<AnnotationFS> annotIndex = cas.getAnnotationIndex();
    // System.out.println("\nNumber of annotations in index: " + annotIndex.size());

    // Print the first few sentences.
    // System.out.println("The first 50 annotations:\n");

    it = annotIndex.iterator();
    // assert(it.isValid());
    // counter = 0;
    // AnnotationFS fs;
    // while (it.isValid() && counter < 50) {
    // fs = (AnnotationFS)it.get();
    // System.out.print(fs.getType().getName() + ": ");
    // if (fs.getType().getName().equals(CASMgr.DOCUMENT_TYPE)) {
    // // When we see the document, we don't print the whole text ;-)
    // System.out.println("...");
    // } else {
    // System.out.println(
    // fs.getCoveredText());
    // }
    // it.moveToNext();
    // ++counter;
    // }
  }

  // private static String file2String(String file) throws IOException {
  // return file2String(new File(file));
  // }

  /**
   * Read the contents of a file into a string, using the default platform encoding.
   * 
   * @param file
   *          The file to be read in.
   * @return String The contents of the file.
   * @throws IOException
   *           Various I/O errors.
   */
  public static String file2String(File file) throws IOException {
    // Read the file into a string using a char buffer.
    FileReader reader = null;
    int bufSize = (int) file.length(); // length in bytes >= length in chars due to encoding
    char[] buf = new char[bufSize];
    int read_so_far = 0;
    try {
      reader = new FileReader(file);  
      while (read_so_far < bufSize) {
        int count = reader.read(buf, read_so_far, bufSize - read_so_far);
        if (count < 0) {
          break;
        }
        read_so_far += count;
      }

    } finally {
      if (null != reader)
        reader.close();
    }
    return new String(buf, 0, read_so_far);    
  }

  /**
   * Test driver.
   */
  public void testMain() throws Exception {

    // Read the document into a String. I'm sure there are better ways to
    // do this.
    File textFile = JUnitExtension.getFile("data/moby.txt");
    String moby = file2String(textFile);
    // String moby = file2String(System.getProperty("cas.data.test") + "moby.txt");
    String line;
    BufferedReader br = new BufferedReader(new StringReader(moby));
    StringBuffer buf = new StringBuffer();
    List<String> docs = new ArrayList<String>();
    while ((line = br.readLine()) != null) {
      if (line.startsWith(".. <p")) {
        docs.add(buf.toString());
        buf = new StringBuffer();
      } else {
        buf.append(line + "\n");
      }
    }
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
        cas.setDocumentText(docs.get(i));

        tokenize();
        numTok = cas.getAnnotationIndex(tokenType).size();
        assertTrue(numTok > 0);
        // System.out.println(" Number of tokens: " + numTok);

        // System.out.println("Serializing...");
        cs = Serialization.serializeNoMetaData(cas);
        cas = Serialization.createCAS(casMgr, cs);

        assertTrue(numTok == cas.getAnnotationIndex(tokenType).size());

        createSentences();
        numSent = cas.getAnnotationIndex(sentenceType).size();
        assertTrue(numSent > 0);
        // System.out.println(" Number of sentences: " + numSent);

        // System.out.println("Serializing...");
        cs = Serialization.serializeNoMetaData(cas);
        cas = Serialization.createCAS(casMgr, cs);

        assertTrue(numTok == cas.getAnnotationIndex(tokenType).size());
        assertTrue(numSent == cas.getAnnotationIndex(sentenceType).size());
        // System.out.println(" Number of tokens: " + numTok);
        checkSentences();

        // System.out.println("Serializing...");
        cs = Serialization.serializeNoMetaData(cas);
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

  public static void main(String[] args) {
    junit.textui.TestRunner.run(SerializationNoMDTest.class);
  }

}
