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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.ShortArrayFS;
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
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.internal.util.TextStringTokenizer;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.FileUtils;

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

  public static final String OSTR_TYPE = "theType";
  
  public static final String OSTR_TYPE_FEAT = "theString";
  
  public static final String OBYTE_TYPE_FEAT = "theByte";
  
  public static final String OSHORT_TYPE_FEAT = "theShort";
  
  public static final String OBYTEA_TYPE_FEAT = "theByteArray";
  
  public static final String OSHORTA_TYPE_FEAT = "theShortArray";
  
  public static final String OLONG_TYPE_FEAT = "theLong";

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

  private Type theTypeType;
  
  private Feature theStringFeature;
  
  private Feature theByteFeature;
  
  private Feature theShortFeature;
  
  private Feature theByteArrayFeature;
  
  private Feature theShortArrayFeature;
  
  private Feature theLongFeature;


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
    theTypeType = ts.getType(OSTR_TYPE);
    theStringFeature = ts.getFeatureByFullName(OSTR_TYPE + TypeSystem.FEATURE_SEPARATOR + OSTR_TYPE_FEAT);
    theByteFeature = ts.getFeatureByFullName(OSTR_TYPE + TypeSystem.FEATURE_SEPARATOR + OBYTE_TYPE_FEAT);
    theByteArrayFeature = ts.getFeatureByFullName(OSTR_TYPE + TypeSystem.FEATURE_SEPARATOR + OBYTEA_TYPE_FEAT);
    theShortFeature = ts.getFeatureByFullName(OSTR_TYPE + TypeSystem.FEATURE_SEPARATOR + OSHORT_TYPE_FEAT);
    theShortArrayFeature = ts.getFeatureByFullName(OSTR_TYPE + TypeSystem.FEATURE_SEPARATOR + OSHORTA_TYPE_FEAT);
    theLongFeature = ts.getFeatureByFullName(OSTR_TYPE + TypeSystem.FEATURE_SEPARATOR + OLONG_TYPE_FEAT);
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
    Type stringType = tsa.getType(CAS.TYPE_NAME_STRING);
    Type byteType = tsa.getType(CAS.TYPE_NAME_BYTE);
    Type byteArrayType = tsa.getType(CAS.TYPE_NAME_BYTE_ARRAY);
    Type shortType = tsa.getType(CAS.TYPE_NAME_SHORT);
    Type shortArrayType = tsa.getType(CAS.TYPE_NAME_SHORT_ARRAY);
    Type longType = tsa.getType(CAS.TYPE_NAME_LONG);
    Type theTypeType = tsa.addType(OSTR_TYPE, annotType);
    tsa.addFeature(OSTR_TYPE_FEAT, theTypeType, stringType);
    tsa.addFeature(OBYTE_TYPE_FEAT, theTypeType, byteType);
    tsa.addFeature(OSHORT_TYPE_FEAT, theTypeType, shortType);
    tsa.addFeature(OBYTEA_TYPE_FEAT, theTypeType, byteArrayType);
    tsa.addFeature(OSHORTA_TYPE_FEAT, theTypeType, shortArrayType);
    tsa.addFeature(OLONG_TYPE_FEAT, theTypeType, longType);
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


  /** Test basic blob serialization
   */
  public void testBlob() throws Exception {

    /*
     * Test that FS, indexes and strings work after repeated blob serialization
     * For each iteration, add two new FS, serialize and test all created so
     * The first FS sets the string feature using standard API => goes into stringlist
     * The second FS sets the string feature using lowlevel API => goes into stringheap 
     * 
     * Throw in tests of the byte, short and long heaps as well
     * 
     */
  String testString = "testString";
  cas.reset();
  LowLevelCAS ll_cas = cas.getLowLevelCAS();
  FSIndexRepository ir = cas.getIndexRepository();
  int ll_strfeatcode = ll_cas.ll_getTypeSystem().ll_getCodeForFeature(theStringFeature);
  int ll_bytefeatcode = ll_cas.ll_getTypeSystem().ll_getCodeForFeature(theByteFeature);
  int ll_shortfeatcode = ll_cas.ll_getTypeSystem().ll_getCodeForFeature(theShortFeature);
  int ll_bytearrayfeatcode = ll_cas.ll_getTypeSystem().ll_getCodeForFeature(theByteArrayFeature);
  int ll_shortarrayfeatcode = ll_cas.ll_getTypeSystem().ll_getCodeForFeature(theShortArrayFeature);
  int ll_longfeatcode = ll_cas.ll_getTypeSystem().ll_getCodeForFeature(theLongFeature);
  
  for (int cycle=0; cycle<10; cycle+=2) {
    FeatureStructure newFS1 = cas.createFS(theTypeType); 
    ir.addFS(newFS1);
    newFS1.setIntValue(startFeature, cycle);
    newFS1.setIntValue(endFeature, cycle+1);
    // set string using normal string feature create
    newFS1.setStringValue(theStringFeature, testString);
    newFS1.setByteValue(theByteFeature, (byte)cycle);
    newFS1.setShortValue(theShortFeature, (short)cycle);
    newFS1.setLongValue(theLongFeature, (long)cycle);
    ByteArrayFS newBA1 = cas.createByteArrayFS(1); 
    ShortArrayFS newSA1 = cas.createShortArrayFS(1); 
    newBA1.set(0, (byte)cycle);
    newSA1.set(0, (short)cycle);
    newFS1.setFeatureValue(theByteArrayFeature, newBA1);
    newFS1.setFeatureValue(theShortArrayFeature, newSA1);

    FeatureStructure newFS2 = cas.createFS(theTypeType);
    ByteArrayFS newBA2 = cas.createByteArrayFS(1);
    ShortArrayFS newSA2 = cas.createShortArrayFS(1); 
    newFS2.setIntValue(startFeature, cycle+1);
    newFS2.setIntValue(endFeature, cycle+2);
    ir.addFS(newFS2);
    // set string using lowlevel string create API
    final int llfs2 = ll_cas.ll_getFSRef(newFS2);
    final int llba2 = ll_cas.ll_getFSRef(newBA2);
    final int llsa2 = ll_cas.ll_getFSRef(newSA2);
    ll_cas.ll_setCharBufferValue(llfs2, ll_strfeatcode,
            testString.toCharArray(), 0, testString.length());
    ll_cas.ll_setByteValue(llfs2, ll_bytefeatcode, (byte)(cycle+1));
    ll_cas.ll_setShortValue(llfs2, ll_shortfeatcode, (short)(cycle+1));
    ll_cas.ll_setLongValue(llfs2, ll_longfeatcode, (long)(cycle+1));
    ll_cas.ll_setByteArrayValue(llba2, 0, (byte)(cycle+1));
    ll_cas.ll_setShortArrayValue(llsa2, 0, (short)(cycle+1));
    newFS2.setFeatureValue(theByteArrayFeature, newBA2);
    newFS2.setFeatureValue(theShortArrayFeature, newSA2);

    ByteArrayOutputStream fos = new ByteArrayOutputStream();
    Serialization.serializeCAS(cas, fos);
      cas.reset();
    ByteArrayInputStream fis = new ByteArrayInputStream(fos.toByteArray());
    Serialization.deserializeCAS(cas, fis);

    FSIndex idx = cas.getAnnotationIndex(theTypeType);
    FSIterator iter = idx.iterator();
    for (int tc=0; tc<cycle+1; tc++) {
      FeatureStructure testFS = iter.get();
      iter.moveToNext();
      assertTrue(tc == testFS.getIntValue(startFeature));
      assertTrue(testString.equals(testFS.getStringValue(theStringFeature)));
      assertTrue(tc == testFS.getByteValue(theByteFeature));
      assertTrue(tc == testFS.getShortValue(theShortFeature));
      assertTrue(tc == testFS.getLongValue(theLongFeature));
      ByteArrayFS ba = (ByteArrayFS)testFS.getFeatureValue(theByteArrayFeature);
      assertTrue(tc == ba.get(0));
      ShortArrayFS sa = (ShortArrayFS)testFS.getFeatureValue(theShortArrayFeature);
      assertTrue(tc == sa.get(0));
    }
    }  
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(SerializationReinitTest.class);
  }

}
