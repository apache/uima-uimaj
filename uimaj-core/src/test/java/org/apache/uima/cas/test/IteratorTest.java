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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.cas.impl.LowLevelIndex;
import org.apache.uima.cas.impl.LowLevelIndexRepository;
import org.apache.uima.cas.impl.LowLevelIterator;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.MultiThreadUtils;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;

/**
 * Class comment for IteratorTest.java goes here.
 * 
 */
public class IteratorTest extends TestCase {

  private CASImpl cas;

  private TypeSystem ts;

  private Type annotationType;

  private Type stringType;

  private Type tokenType;

  private Type intType;

  private Type tokenTypeType;

  private Type wordType;

  private Feature tokenTypeFeat;

  private Feature lemmaFeat;

  private Feature sentLenFeat;

  private Feature tokenFloatFeat;

  private Feature startFeature;

  private Type sentenceType;
  
  private Type subsentenceType;
  
  private FSIndex<FeatureStructure> bagIndex;
  private FSIndex<FeatureStructure> ssBagIndex;
  
  private FSIndex<FeatureStructure> setIndex;
  private FSIndex<FeatureStructure> ssSetIndex;

  private FSIndex<FeatureStructure> sortedIndex;
  private FSIndex<FeatureStructure> ssSortedIndex;
  
  private FSIndex<?> jcasBagIndex;
  private FSIndex<?> jcasSsBagIndex;
  
  private FSIndex<?> jcasSetIndex;
  private FSIndex<?> jcasSsSetIndex;

  private FSIndex<?> jcasSortedIndex;
  private FSIndex<?> jcasSsSortedIndex;
  
  private JCas jcas;

  private FSIndex<FeatureStructure> wordSetIndex;
  private FSIndex<FeatureStructure> ssWordSetIndex;

  private Feature wordFeat;

  private Type wType;

  /**
   * Constructor for FilteredIteratorTest.
   * 
   * @param arg0
   */
  public IteratorTest(String arg0) {
    super(arg0);
  }

  public void setUp() throws CASException {
    // try {
    // this.cas = (CASImpl) CASInitializer.initCas(new CASTestSetup());
    // assertTrue(this.cas != null);
    // this.ts = this.cas.getTypeSystem();
    // assertTrue(this.ts != null);
    // } catch (Exception e) {
    // e.printStackTrace();
    // assertTrue(false);
    // }

    File descriptorFile = JUnitExtension.getFile("CASTests/desc/casTestCaseDescriptor.xml");
    assertTrue("Descriptor must exist: " + descriptorFile.getAbsolutePath(),
        descriptorFile.exists());

    try {
      XMLParser parser = UIMAFramework.getXMLParser();
      ResourceSpecifier spec = (ResourceSpecifier) parser.parse(new XMLInputSource(descriptorFile));
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(spec);
      this.cas = (CASImpl) ae.newCAS();
      this.jcas = cas.getJCas();
      assertTrue(this.cas != null);
      this.ts = this.cas.getTypeSystem();
      assertTrue(this.ts != null);
    } catch (IOException e) {
      e.printStackTrace();
      assertTrue(false);
    } catch (InvalidXMLException e) {
      e.printStackTrace();
      assertTrue(false);
    } catch (ResourceInitializationException e) {
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
    this.sentenceType = this.ts.getType(CASTestSetup.SENT_TYPE);
    assertTrue(this.sentenceType != null);
    this.annotationType = this.ts.getType(CAS.TYPE_NAME_ANNOTATION);
    assertTrue(this.annotationType != null);
    this.subsentenceType = this.ts.getType("SubTypeOfSentence");
    assertTrue(this.subsentenceType != null);
  }

  public void tearDown() {
    this.cas = null;
    this.ts = null;
    this.stringType = null;
    this.tokenType = null;
    this.intType = null;
    this.tokenTypeType = null;
    this.wordType = null;
    this.tokenTypeFeat = null;
    this.lemmaFeat = null;
    this.sentLenFeat = null;
    this.tokenFloatFeat = null;
    this.startFeature = null;
    this.sentenceType = null;
    this.annotationType = null;
  }
  
  private void setupindexes () {
    bagIndex = this.cas.getIndexRepository().getIndex(CASTestSetup.ANNOT_BAG_INDEX);
    setIndex = this.cas.getIndexRepository().getIndex(CASTestSetup.ANNOT_SET_INDEX);
    sortedIndex = this.cas.getIndexRepository().getIndex(CASTestSetup.ANNOT_SORT_INDEX);
    
    ssBagIndex = bagIndex.withSnapshotIterators();
    ssSetIndex = setIndex.withSnapshotIterators();
    ssSortedIndex = sortedIndex.withSnapshotIterators();
    
    jcasBagIndex = this.jcas.getIndexRepository().getIndex(CASTestSetup.ANNOT_BAG_INDEX);
    jcasSetIndex = this.jcas.getIndexRepository().getIndex(CASTestSetup.ANNOT_SET_INDEX);
    jcasSortedIndex = this.jcas.getIndexRepository().getIndex(CASTestSetup.ANNOT_SORT_INDEX);
    
    jcasSsBagIndex = jcasBagIndex.withSnapshotIterators();
    jcasSsSetIndex = jcasSetIndex.withSnapshotIterators();
    jcasSsSortedIndex = jcasSortedIndex.withSnapshotIterators();

  }
  
  public void testEmptySnapshotIterator() {
    setupindexes();
    FSIterator<FeatureStructure> it = sortedIndex.iterator();
    assertFalse(it.isValid());
    boolean ok = false;
    try {
      it.get();
    } catch (NoSuchElementException e) {
      ok = true;
    }
    assertTrue(ok);
    
    it = ssSortedIndex.iterator();
    assertFalse(it.isValid());
    ok = false;
    try {
      it.get();
    } catch (NoSuchElementException e) {
      ok = true;
    }
    assertTrue(ok);    
  }
  
  public void testGetIndexes() {
     Iterator<FSIndex<FeatureStructure>> it = this.cas.getIndexRepository().getIndexes();
    while (it.hasNext()) {
      assertNotNull(it.next());
    }
  }

  public void testMoveTo() {
    // Add some arbitrary annotations
    for (int i = 0; i < 10; i++) {
      createFSs(i);  // add annotation, sentence, and 3 tokens, all with same begin / end
    }
    final int start = 5;
    final int end = 7;
    FSIndexRepository repo = this.cas.getIndexRepository();
    for (int i = 0; i < 10; i++) {
      // add 10 annotations with start 5, end 7
      AnnotationFS annotation = this.cas.createAnnotation(this.annotationType, start, end);  
      repo.addFS(annotation);
    }
    AnnotationFS match = this.cas.createAnnotation(this.annotationType, start, end);
    FSIndex<AnnotationFS> index = this.cas.getAnnotationIndex();
    FSIndex<AnnotationFS> ssIndex = index.withSnapshotIterators();
    FSIterator<AnnotationFS> it = index.iterator();
    FSIterator<AnnotationFS> ssit = ssIndex.iterator();
    it.moveTo(match);  // should move to left-most of the 10 with start=5 end=7
    ssit.moveTo(match);
    assertTrue(index.compare(match, it.get()) == 0);
    assertTrue(index.compare(match, ssit.get()) == 0);
    
    // The contract of moveTo() says that any preceding FS must be smaller.
    it.moveToPrevious();
    ssit.moveToPrevious();
    assertTrue(index.compare(match, it.get()) > 0);
    assertTrue(index.compare(match, ssit.get()) > 0);
  }
  
  public void testMoveToPastEnd() {  // https://issues.apache.org/jira/browse/UIMA-4094
    this.cas.getIndexRepository().addFS(
        this.cas.createAnnotation(this.annotationType, 1,2));
    
    AnnotationFS pastEnd = this.cas.createAnnotation(this.annotationType,  2,  3);
    FSIndex<AnnotationFS> index = this.cas.getAnnotationIndex();
    FSIterator<AnnotationFS> it = index.iterator();
    it.moveTo(pastEnd);
    assertFalse(it.isValid());
    
    index = index.withSnapshotIterators();
    it = index.iterator();
    it.moveTo(pastEnd);
    assertFalse(it.isValid());

  }
  
  public void testMoveToFirstOfEqualOneType() {
    for (int i = 0; i < 2; i++) {
      cas.reset();
      if (i == 0) {
        this.cas.getIndexRepository().addFS(
            this.cas.createAnnotation(this.subsentenceType, 0, 1));        
      }
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.subsentenceType, 1,2));
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.subsentenceType, 1,2));
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.subsentenceType, 1,3));
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.subsentenceType, 2,2));
      this.cas.getIndexRepository().addFS(
          this.cas.createAnnotation(this.subsentenceType, 2, 5));
      
      AnnotationFS testAnnot = this.cas.createAnnotation(this.subsentenceType, 1, 2);
      
      FSIndex<AnnotationFS> index = this.cas.getAnnotationIndex(this.subsentenceType);
      FSIterator<AnnotationFS> it = index.iterator();
      it.moveTo(testAnnot);
      for (int j = 0; j < 2; j++) {
        assertTrue(it.isValid());
        AnnotationFS fs = it.get();
        assertEquals(1, fs.getBegin()); 
        assertEquals(2, fs.getEnd());
        it.moveToNext();
      }
      assertTrue(it.isValid());
      
      index = index.withSnapshotIterators();
      it = index.iterator();
      it.moveTo(testAnnot);
      for (int j = 0; j < 2; j++) {
        assertTrue(it.isValid());
        AnnotationFS fs = it.get();
        assertEquals(1, fs.getBegin()); 
        assertEquals(2, fs.getEnd());
        it.moveToNext();
      }
      assertTrue(it.isValid());

    }
  }
  

  private void createFSs(int i) {
    FeatureStructureImpl fsi;
    this.cas.getIndexRepository().addFS(
        this.cas.createAnnotation(this.annotationType, i * 2, (i * 2) + 1));
    this.cas.getIndexRepository().addFS(
        this.cas.createAnnotation(this.sentenceType, i * 2, (i * 2) + 1));
    this.cas.getIndexRepository().addFS(
        fsi = (FeatureStructureImpl) this.cas.createAnnotation(this.tokenType, i * 2, (i * 2) + 1));
    this.cas.getIndexRepository().addFS(
        this.cas.createAnnotation(this.tokenType, i * 2, (i * 2) + 1));
    this.cas.getIndexRepository().addFS(
        this.cas.createAnnotation(this.tokenType, i * 2, (i * 2) + 1));
//    //debug
//    System.out.format("Token at %,d %n", fsi.getAddress());
  }
  
//  private void debugls() {
//    LowLevelIndexRepository llir = this.cas.ll_getIndexRepository();
//    LowLevelIndex setIndexForType = llir.ll_getIndex(CASTestSetup.ANNOT_SET_INDEX, ((TypeImpl)tokenType).getCode());
//    LowLevelIterator it = setIndexForType.ll_iterator();
//    it.moveToLast();
//    System.out.format("Last token in set index is %,d%n", it.ll_get());
//  }
  
  private void setupFSs() {
    for (int i = 0; i < 10; i++) {
      createFSs(i);   // 0 ... 9
    }
    for (int i = 19; i >= 10; i--) {
      createFSs(i);   // 19 ... 10
    }
  }
  
  public void testMultithreadedIterator() {
    setupFSs();
    final FSIndex<FeatureStructure> bagIndex = this.cas.getIndexRepository().getIndex(
        CASTestSetup.ANNOT_BAG_INDEX);
    final FSIndex<FeatureStructure> setIndex = this.cas.getIndexRepository().getIndex(
        CASTestSetup.ANNOT_SET_INDEX);
    final FSIndex<FeatureStructure> sortedIndex = this.cas.getIndexRepository().getIndex(
        CASTestSetup.ANNOT_SORT_INDEX);
  
    int numberOfCores = Math.min(50, Runtime.getRuntime().availableProcessors() * 5);
    
    System.out.println("test multicore iterator with " + numberOfCores + " threads");
    MultiThreadUtils.ThreadM[] threads = new MultiThreadUtils.ThreadM[numberOfCores];
    
    final Throwable[] tthrowable = new Throwable[1];  // trick to get a return value in a parameter
    tthrowable[0] = null;
    
    for (int i = 0; i < numberOfCores; i++) {
      final int finalI = i;
      threads[i] = new MultiThreadUtils.ThreadM() {

        public void run() {
          try {
            while (true) {
              if (!MultiThreadUtils.wait4go(this)) {
                break;
              }
              setIteratorWithoutMods(setIndex, finalI);
              sortedIteratorWithoutMods(sortedIndex);
              bagIteratorWithoutMods(bagIndex);
            }
          } catch (Throwable e) {
            tthrowable[0] = e;
            e.printStackTrace();
            throw new RuntimeException(e);
          }
        }};
      threads[i].start();      
    }
    
    
    for (int r = 0; r < 10; r++) {
            
      MultiThreadUtils.kickOffThreads(threads);
      
      MultiThreadUtils.waitForAllReady(threads);
      
      for (int i = 0; i < numberOfCores; i++) {
          if (tthrowable[0] != null) {
            assertTrue(false);
          }
          if (tthrowable[0] != null) {
            assertTrue(false);
          }
      }
    }
    
    MultiThreadUtils.terminateThreads(threads);
  }
  
  public void testIterator() {
    setupFSs();
    
    setupindexes();
    
    setIteratorWithoutMods(setIndex, -1);
    setIteratorWithoutMods(ssSetIndex, -2);
    
    sortedIteratorWithoutMods(sortedIndex);
    sortedIteratorWithoutMods(ssSortedIndex);
    
    bagIteratorWithoutMods(bagIndex);
    bagIteratorWithoutMods(ssBagIndex);

    // test find()
    findTst(setIndex, jcasSetIndex);
    findTst(ssSetIndex, jcasSsSetIndex);
    findTst(sortedIndex, jcasSortedIndex);
    findTst(ssSortedIndex, jcasSsSortedIndex);
    findTst(bagIndex, jcasBagIndex);
    findTst(ssBagIndex, jcasSsBagIndex);
    
//    debugls();  //debug
    
    basicRemoveAdd(bagIndex, 20, 21);
    basicRemoveAdd(ssBagIndex, 20, 21);
    basicRemoveAdd(sortedIndex, 38, 39);
//    debugls();  //debug
    basicRemoveAdd(ssSortedIndex, 38, 39);
    basicRemoveAdd(setIndex, 38, 39);
    basicRemoveAdd(ssSetIndex, 38, 39);

   
    
    // /////////////////////////////////////////////////////////////////////////
    // Test fast fail.

    fastFailTst(setIndex, true);
    fastFailTst(ssSetIndex, false);
    
    fastFailTst(bagIndex, true);
    fastFailTst(ssBagIndex, false);
   
    fastFailTst(sortedIndex, true);  
    fastFailTst(ssSortedIndex, false);
    
//    debugls();  //debug
    
    

    // Test find()
    setupWords();
    
    tstWord(wordSetIndex);
    tstWord(ssWordSetIndex);

//    debugls();  //debug
    

    // moved IntArrayRBTtest for pointer iterators here
    LowLevelIndexRepository llir = this.cas.ll_getIndexRepository();
    LowLevelIndex setIndexForType = llir.ll_getIndex(CASTestSetup.ANNOT_SET_INDEX, ((TypeImpl)tokenType).getCode());
    int[] expected = {17, 53, 89, 125, 161, 197, 233, 269, 305, 341, 701, 665, 629, 593, 557, 521, 485, 449, 413, 377};
    setIndexIterchk(setIndexForType, expected);
    
    setIndexForType = llir.ll_getIndex(CASTestSetup.ANNOT_SET_INDEX, ((TypeImpl)sentenceType).getCode());
    expected = new int[] {12, 48, 84, 120, 156, 192, 228, 264, 300, 336, 696, 660, 624, 588, 552, 516, 480, 444, 408, 372};
    setIndexIterchk(setIndexForType, expected);
    
    setIndexForType = llir.ll_getIndex(CASTestSetup.ANNOT_SET_INDEX);
    expected = new int[] {
        1,   44,  80,  116, 152, 188, 224, 260, 296, 332,         692, 656, 620, 584, 548, 512, 476, 440, 404, 368, 
        12,  48,  84,  120, 156, 192, 228, 264, 300, 336,         696, 660, 624, 588, 552, 516, 480, 444, 408, 372, 
        17,  53,  89,  125, 161, 197, 233, 269, 305, 341,         701, 665, 629, 593, 557, 521, 485, 449, 413, 377};
    setIndexIterchk(setIndexForType, expected);
    
    setIndexForType = llir.ll_getIndex(CASTestSetup.ANNOT_SET_INDEX, ((TypeImpl)tokenType).getCode());
    LowLevelIterator it = setIndexForType.ll_iterator();
    assertTrue(it.isValid());
    it.moveToPrevious();
    assertFalse(it.isValid());
    it.moveToNext();
    assertFalse(it.isValid());
    it.moveToLast();
    assertTrue(it.isValid());
    it.moveToNext();
    assertFalse(it.isValid());
    it.moveToPrevious();
    assertFalse(it.isValid());
   }
  
  private void setIndexIterchk(LowLevelIndex idx, int[] expected) {
    LowLevelIterator it = idx.ll_iterator();
    int[] r = new int[70];
    int i = 0;
    while (it.isValid()) {
      r[i++] = it.ll_get();
      it.moveToNext();
    }
    // r 17, 53, 89, 125, 161, 197, 233, 269, 305, 341, 719, 665, 629, 593, 557, 521, 485, 449, 413, 395
    assertTrue(Arrays.equals(Arrays.copyOfRange(r, 0, expected.length), expected));
  }
  
  private void setIteratorWithoutMods(FSIndex<FeatureStructure> setIndex, int threadNumber) {
    // /////////////////////////////////////////////////////////////////////////
    // Create a reverse iterator for the set index and check that the result
    // is the same as for forward iteration.

    IntVector v = new IntVector();
    FSIterator<FeatureStructure> it = setIndex.iterator();
    AnnotationFS a, b = null;
    int ii = 0;
//    StringBuilder sb = new StringBuilder();
    while (it.isValid()) {
      a = (AnnotationFS) it.get();
      if (b != null) {
        // note compare may be equal for two items of different types
        // because setIndex is not using type priorities (I think) (2/2015)
        int compareResult = setIndex.compare(b, a);
        if (compareResult == 0) {
          // types must be different
          assertFalse(a.getType().getName().equals(b.getType().getName()));
        }
        if (a.hashCode() == b.hashCode()) {
          System.err.format("set Iterator: should not have 2 identical elements%n%s%n", it);
          assertTrue(false);
        }
//        if (a.getType() == b.getType()) {
//          assertTrue(setIndex.compare(b, a) < 0);
//          System.out.println("diff types");
//        }
      }
      b = a;
//       sb.append(String.format("%d %d debug: n=%d, type=%s, start=%d, end-%d%n",
//           threadNumber,
//           ii++,
//           a.hashCode(),
//           a.getType().getName(),
//           a.getBegin(),
//           a.getEnd()));
      v.add(it.get().hashCode());
      it.moveToNext();
    }
    // System.out.println("Number of annotations: " + v.size());
    if (v.size() != ((10 * 3) + (10 * 3))) {
      System.err.format("Expected number in set was 60, but has %d elements%n%s%n", v.size(), it);
//      System.err.println(sb);
      assertTrue(false);
    }
//    else
//      System.out.println(sb);

    it = setIndex.iterator();
    it.moveToLast();
    int current = v.size() - 1;
    while (it.isValid() && (current >= 0)) {
      // System.out.println("Current: " + current);
      a = (AnnotationFS) it.get();
      // System.out.println(
      // a.getType().getName() + " - " + a.getStart() + " - " +
      // a.getEnd());
      assertTrue(it.get().hashCode() == v.get(current));
      it.moveToPrevious();
      --current;
    }
    assertTrue(current == -1);
    assertFalse(it.isValid());

    // /////////////////////////////////////////////////////////////////////////
    // Use an iterator to move forwards and backwards and make sure the
    // sequence
    // remains constant.
    it = setIndex.iterator();
    it.moveToFirst(); // This is redundant.
    current = 1;
    // System.out.println("Codes: " + v);
    while (current < (v.size() - 1)) {
      it.moveToNext();
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(current));
      it.moveToNext();
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(current + 1));
      it.moveToPrevious();
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(current));
      ++current;
    }

    // also test Java-style iteration
    Iterator<FeatureStructure> javaIt = setIndex.iterator();
    current = 0;
    while (javaIt.hasNext()) {
      assertEquals(javaIt.next().hashCode(), v.get(current++));
    }
  }
  
  private void findTst(FSIndex index, FSIndex jcasIndex) {
    findTestCas(index);
    findTestJCas(jcasIndex);
  }
  
  private void basicRemoveAdd(FSIndex<FeatureStructure> index, int begin, int end) {
    FSIterator<FeatureStructure> it = index.iterator();
    it.moveToLast();
    AnnotationFS a = (AnnotationFS) it.get();
    cas.removeFsFromIndexes(a);
    cas.addFsToIndexes(a);

    it.moveToLast();
    a = (AnnotationFS) it.get();
    assertEquals(tokenType, a.getType());
    assertEquals(begin, a.getBegin());
    assertEquals(end, a.getEnd());
  }
  
  private void fastFailTst(FSIndex<FeatureStructure> index, boolean isShouldFail) {
    FSIterator<FeatureStructure> it = index.iterator();
    it.moveToLast();
    it.moveToFirst();
    // moved to first, 2.7.0, because new bag iterator is more forgiving re concurrentmodexception
    FeatureStructure a = it.get();
    
    cas.removeFsFromIndexes(a);
    cas.addFsToIndexes(a);    
    
    expectCCE(a, it, isShouldFail);
    expectCCE(a, it, false);  // ok because above expectCCE reset the iterator   
  }
  
  private void expectCCE(FeatureStructure a, FSIterator it, boolean isShouldFail) {
    boolean ok = false;
    try {
      it.moveToNext();
    } catch (ConcurrentModificationException e) {
      ok = true;
    }
    assertTrue(isShouldFail ? ok : !ok);
    
    it.moveTo(a);  // should reset concurrent mod, 
    ok = true;
    try {
      it.moveToNext();
    } catch (ConcurrentModificationException e) {
      ok = false;
    }
//    if (!ok) {
//      System.out.println("debug");
//    }
    assertTrue(ok);
  }
  
  private void setupWords() {
    wType = this.cas.getTypeSystem().getType("org.apache.uima.cas.test.types.Word");

    wordFeat = wType.getFeatureByBaseName("word");

    for (int i = 0; i < 20; i++) {
      FeatureStructure fs = this.cas.createFS(wType);
      fs.setStringValue(wordFeat, "word" + i);
      this.cas.getIndexRepository().addFS(fs);
    }

    wordSetIndex = this.cas.getIndexRepository().getIndex("Word Set Index");
    ssWordSetIndex = wordSetIndex.withSnapshotIterators();

  }
  
  private void tstWord(FSIndex<FeatureStructure> index) {
    FSIterator<FeatureStructure> it = index.iterator();
    it.moveToLast();

    FeatureStructure fs = this.cas.createFS(wType);
    fs.setStringValue(wordFeat, "word1");

    // TEST moveTo() and get()
    it.moveTo(fs);

    assertSame(fs.getType(), it.get().getType());

    Type t1 = fs.getType();
    Type t2 = wordSetIndex.find(fs).getType();
    assertSame(t1, t2);

  }
  
  private void findTestCas(FSIndex<FeatureStructure> index) {
    AnnotationFS annot = (AnnotationFS) index.iterator().get();  // first element
    assertNotNull(index.find(annot));
    assertNull(index.find(this.cas.createAnnotation(this.annotationType, -1, -1)));
  }
  
  private void findTestJCas(FSIndex<?> index) {
    Annotation annot = (Annotation) index.iterator().get();  // first element
    assertNotNull(index.find(annot));
    assertNull(index.find(this.cas.createAnnotation(this.annotationType, -1, -1)));    
  }
  
  
  private void sortedIteratorWithoutMods(FSIndex<FeatureStructure> sortedIndex) {
    // /////////////////////////////////////////////////////////////////////////
    // Test sorted index.

    // FSIndex sortedIndex = cas.getAnnotationIndex(); // using different
    // typeOrder
    // System.out.println("Number of annotations: " + sortedIndex.size());
    // for (it = sortedIndex.iterator(); it.hasNext(); it.next()) {
    // System.out.println(it.get());
    // }

    if (sortedIndex.size() != 100) {  
      assertTrue(false);
    }    
    IntVector v = new IntVector();
    FSIterator<FeatureStructure> it = sortedIndex.iterator();
    it.moveToFirst();
    AnnotationFS a, b = null;
    while (it.isValid()) {
      a = (AnnotationFS) it.get();
      // System.out.println(a);
      assertTrue(a != null);
      if (b != null) {
        // System.out.println("b = " + b);
        assertTrue(sortedIndex.compare(b, a) <= 0);
      }
      b = a;
      int hc = a.hashCode();
      v.add(hc);
//      if ((hc % 2) == 1) {
        Thread.yield();
//      }
      it.moveToNext();
    }
    assertTrue(sortedIndex.size() == v.size());

    // Test moveTo()
    List<AnnotationFS> list = new ArrayList<AnnotationFS>();
    FSIterator<AnnotationFS> it2 = this.cas.getAnnotationIndex().iterator();
    for (it2.moveToFirst(); it2.isValid(); it2.moveToNext()) {
      list.add(it2.get());
    }
    // AnnotationFS an;
    for (int i = 0; i < list.size(); i++) {
      // System.out.println("Iteration: " + i);
      it2.moveToFirst();
      it2.moveTo(list.get(i));
      assertTrue(it2.get().getBegin() == list.get(i).getBegin());
      assertTrue(it2.get().getEnd() == list.get(i).getEnd());
    }

    // Check that reverse iterator produces reverse sequence.
    // Note: this test is not valid. It is by no means guaranteed that reverse iteration of a
    // sorted index will produce the reverse result of forward iteration. I no two annotations
    // are equal wrt the sort order, this works of course. However, if some FSs are equal wrt
    // the sort order, those may be returned in any order.
    // it.moveToLast();
    // System.out.println(it.get());
    // for (int i = v.size() - 1; i >= 0; i--) {
    // assertTrue(it.isValid());
    // assertTrue(it.get().hashCode() == v.get(i));
    // it.moveToPrevious();
    // }

    // /////////////////////////////////////////////////////////////////////////
    // Use an iterator to move forwards and backwards and make sure the
    // sequence
    // remains constant.
    it = sortedIndex.iterator();
    it.moveToFirst(); // This is redundant.
    int current = 1;
    // System.out.println("Codes: " + v);
    while (current < (v.size() - 1)) {
      it.moveToNext();
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(current));
      it.moveToNext();
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(current + 1));
      it.moveToPrevious();
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(current));
      ++current;
    }

    // also test Java-style iteration
    Iterator<FeatureStructure> javaIt = sortedIndex.iterator();
    current = 0;
    while (javaIt.hasNext()) {
      assertEquals(javaIt.next().hashCode(), v.get(current++));
    }
  
  }
  
  private void bagIteratorWithoutMods(FSIndex<FeatureStructure> bagIndex) {
    // /////////////////////////////////////////////////////////////////////////
    // Test bag index.
    // System.out.println("Number of annotations: " + sortedIndex.size());
    assertTrue(bagIndex.size() == 100);
    IntVector v = new IntVector();
    FSIterator<FeatureStructure> it = bagIndex.iterator();
    AnnotationFS a, b = null;
//    int debug_i = 0;
    while (true) {
//      if (debug_i == 20) {
//        System.out.println("Debug");
//      }
      if (!it.isValid()) {
        break;
      }
       a = (AnnotationFS) it.get();
//      debug_i ++;
      assertTrue(a != null);
      // bag indices no longer are in sort by fs order
//      if (b != null) {
//        assertTrue(bagIndex.compare(b, a) <= 0);
//      }
      b = a;
      v.add(a.hashCode());
      it.moveToNext();
    }
    assertTrue(bagIndex.size() == v.size());

    // Check that reverse iterator produces reverse sequence.
    it.moveToLast();
    for (int i = v.size() - 1; i >= 0; i--) {
//      if (!it.isValid()) {
//        System.out.println("debug");
//      }
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(i));
      it.moveToPrevious();
    }

    // /////////////////////////////////////////////////////////////////////////
    // Use an iterator to move forwards and backwards and make sure the
    // sequence
    // remains constant.
    it = bagIndex.iterator();
    it.moveToFirst(); // This is redundant.
    int current = 1;
    // System.out.println("Codes: " + v);
    while (current < (v.size() - 1)) {
      it.moveToNext();
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(current));
      it.moveToNext();
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(current + 1));
//      if (current == 19) {
//        System.out.println("debug");
//      }
      it.moveToPrevious();
      assertTrue(it.isValid());
      assertTrue(it.get().hashCode() == v.get(current));
      ++current;
    }

    // also test Java-style iteration
    Iterator<FeatureStructure> javaIt = bagIndex.iterator();
    current = 0;
    while (javaIt.hasNext()) {
      assertEquals(javaIt.next().hashCode(), v.get(current++));
    }

    // Test iterator copy.
    FSIterator<AnnotationFS> source, copy;
    source = this.cas.getAnnotationIndex().iterator();
    // Count items.
    int count = 0;
    for (source.moveToFirst(); source.isValid(); source.moveToNext()) {
      ++count;
    }

    final int max = count;
    count = 0;
    source.moveToFirst();
    copy = source.copy();
    copy.moveToFirst();
    // System.out.println("Max: " + max);
    while (count < max) {
      // System.out.println("Count: " + count);
      assertTrue(source.isValid());
      assertTrue(copy.isValid());
      String out = source.get().toString() + copy.get().toString();
      assertTrue(out, source.get().equals(copy.get()));
      source.moveToNext();
      copy.moveToNext();
      ++count;
    }

  }
  
  private void addAnnotations(AnnotationFS[] fsArray, Type type) {
    FSIndexRepository ir = this.cas.getIndexRepository();    
    for (int i = 0; i < fsArray.length; i++) {
      // key order:
      //   0 ... 50 200 ... 160  66 66 66 ..
      // item order
      //   0 - 50, 90 - 99, 89 - 60
      int j = (i >= 90) ? 66 :           // some constant keys
              (i > 50) ? 200 - i :       // some decreasing keys
              i;                         // some increasing keys
      fsArray[i] = this.cas.createAnnotation(type, j * 5, (j * 5) + 4);
      ir.addFS(fsArray[i]);
    }
  }

  /**
   * Test deleting FSs from indexes.
   */
  public void testDelete() {
    // Create a bunch of FSs.
    // have 10% of them be the same key
    // have the order be scrambled somewhat, not strictly increasing
    AnnotationFS[] fsArray = new AnnotationFS[100];
    FSIndexRepository ir = this.cas.getIndexRepository();
    addAnnotations(fsArray, this.tokenType);

    FSIndex<FeatureStructure> setIndex = this.cas.getIndexRepository().getIndex(
        CASTestSetup.ANNOT_SET_INDEX, this.tokenType);
    FSIterator<FeatureStructure> setIt = setIndex.iterator();
    
    FSIndex<AnnotationFS> sortedIndex = this.cas.getAnnotationIndex(this.tokenType);
    FSIterator<AnnotationFS> sortedIt = sortedIndex.iterator();
    
    FSIndex<FeatureStructure> bagIndex = ir.getIndex(CASTestSetup.ANNOT_BAG_INDEX, this.tokenType);
    FSIterator<FeatureStructure> bagIt = bagIndex.iterator();
    
    // verify that the index is the right type https://issues.apache.org/jira/browse/UIMA-2883
    assertEquals(setIndex.getIndexingStrategy(),FSIndex.SET_INDEX);
    assertEquals(sortedIndex.getIndexingStrategy(),FSIndex.SORTED_INDEX);
    assertEquals(bagIndex.getIndexingStrategy(),FSIndex.BAG_INDEX);
    
    // For each index, check that the FSs are actually in the index.
    for (int i = 0; i < fsArray.length; i++) {
      setIt.moveTo(fsArray[i]);
      assertTrue(setIt.isValid());
      assertTrue(setIt.get().equals(fsArray[(i < 90) ? i : 90]));

      bagIt.moveTo(fsArray[i]);
      assertTrue(bagIt.isValid());
      assertTrue(bagIt.get().equals(fsArray[i]));
      
      sortedIt.moveTo(fsArray[i]);
      assertTrue(sortedIt.isValid());
      fsBeginEndEqual(sortedIt.get(), fsArray[i]);
    }
    sortedIt.moveToFirst();
    // item order
    //   0 - 50, 90 - 99, 89 - 60    
    for (int i = 0; i < fsArray.length; i++) {
      int j = (i >= 61) ? (89 - (i - 61)) :
              (i >= 51) ? 90 :
              i;
      fsBeginEndEqual(sortedIt.get(), fsArray[j]);
      sortedIt.moveToNext();
    }
    assertFalse(sortedIt.isValid());
    
    // Remove an annotation, then add it again. Try setting the iterators to
    // that FS. The iterator should either be invalid, or point to a
    // different FS.
    for (int i = 0; i < fsArray.length; i++) {
      ir.removeFS(fsArray[i]);
      ir.removeFS(fsArray[i]);  // a 2nd remove should be a no-op https://issues.apache.org/jira/browse/UIMA-2934
      setIt.moveTo(fsArray[i]);
      if (setIt.isValid()) {
        int oldRef = this.cas.ll_getFSRef(fsArray[i]);
        int newRef = this.cas.ll_getFSRef(setIt.get());
        assertTrue(oldRef != newRef);
        assertTrue(!setIt.get().equals(fsArray[i]));
      }
      bagIt.moveTo(fsArray[i]);
      if (bagIt.isValid()) {
        assertTrue(!bagIt.get().equals(fsArray[i]));
      }
      sortedIt.moveTo(fsArray[i]);
      if (sortedIt.isValid()) {
        assertTrue(!sortedIt.get().equals(fsArray[i]));
      }
      ir.addFS(fsArray[i]);
    }
    // Remove all annotations.
    for (int i = 0; i < fsArray.length; i++) {
      ir.removeFS(fsArray[i]);
    }
    // All iterators should be invalidated when being reset.
    bagIt.moveToFirst();
    assertFalse(bagIt.isValid());
    setIt.moveToFirst();
    assertFalse(setIt.isValid());
    sortedIt.moveToFirst();
    assertFalse(sortedIt.isValid());
  }
  
  private void verifyMoveToFirst(FSIterator<?> it, boolean expected) {
    it.moveToFirst();
    assertEquals(it.isValid(), expected);
  }
  
  private void verifyHaveSubset(FSIterator<?> x, int nbr, Type type) {
    x.moveToFirst();
    int i = 0;
    while (x.hasNext()) {
      i++;
      assertEquals(type, x.get().getType());
      x.moveToNext();
    }
    assertEquals(nbr, i);
  }

  public void testRemoveAll() {
    AnnotationFS[] fsArray = new AnnotationFS[100];
    AnnotationFS[] subFsArray = new AnnotationFS[100];
    FSIndexRepository ir = this.cas.getIndexRepository();
    
    addAnnotations(fsArray, ts.getType("Sentence"));
    addAnnotations(subFsArray, ts.getType("SubTypeOfSentence"));
    
    FSIndex<FeatureStructure> setIndex = ir.getIndex(CASTestSetup.ANNOT_SET_INDEX, this.sentenceType);
    FSIndex<FeatureStructure> bagIndex = ir.getIndex(CASTestSetup.ANNOT_BAG_INDEX, this.sentenceType);
    FSIndex<AnnotationFS> sortedIndex = this.cas.getAnnotationIndex(this.sentenceType);

    FSIndex<FeatureStructure> subsetIndex = ir.getIndex(CASTestSetup.ANNOT_SET_INDEX, this.subsentenceType);
    FSIndex<FeatureStructure> subbagIndex = ir.getIndex(CASTestSetup.ANNOT_BAG_INDEX, this.subsentenceType);
    FSIndex<AnnotationFS>     subsortedIndex = this.cas.getAnnotationIndex(this.subsentenceType);

    FSIterator<FeatureStructure> setIt = setIndex.iterator();
    FSIterator<FeatureStructure> bagIt = bagIndex.iterator();
    FSIterator<AnnotationFS> sortedIt = sortedIndex.iterator();

    FSIterator<FeatureStructure> subsetIt = subsetIndex.iterator();
    FSIterator<FeatureStructure> subbagIt = subbagIndex.iterator();
    FSIterator<AnnotationFS> subsortedIt = subsortedIndex.iterator();

    verifyMoveToFirst(setIt, true);
    verifyMoveToFirst(bagIt, true);
    verifyMoveToFirst(sortedIt, true);
    verifyMoveToFirst(subsetIt, true);
    verifyMoveToFirst(subbagIt, true);
    verifyMoveToFirst(subsortedIt, true);
    
    ir.removeAllIncludingSubtypes(sentenceType);
    verifyConcurrantModificationDetected(setIt);
    verifyConcurrantModificationDetected(bagIt);
    verifyConcurrantModificationDetected(sortedIt);
    verifyConcurrantModificationDetected(subsetIt);
    verifyConcurrantModificationDetected(subbagIt);
    verifyConcurrantModificationDetected(subsortedIt);

    verifyMoveToFirst(setIt, false);
    verifyMoveToFirst(bagIt, false);
    verifyMoveToFirst(sortedIt, false);
    verifyMoveToFirst(subsetIt, false);
    verifyMoveToFirst(subbagIt, false);
    verifyMoveToFirst(subsortedIt, false);

    for (AnnotationFS fs : fsArray) {
      ir.addFS(fs);
    }
    for (AnnotationFS fs : subFsArray) {
      ir.addFS(fs);
    }

    verifyMoveToFirst(setIt, true);
    verifyMoveToFirst(bagIt, true);
    verifyMoveToFirst(sortedIt, true);
    verifyMoveToFirst(subsetIt, true);
    verifyMoveToFirst(subbagIt, true);
    verifyMoveToFirst(subsortedIt, true);

    ir.removeAllExcludingSubtypes(this.sentenceType);
    
    verifyHaveSubset(setIt, 91, subsentenceType);
    verifyHaveSubset(bagIt, 100, subsentenceType);
    verifyHaveSubset(sortedIt, 100, subsentenceType);
    verifyHaveSubset(subsetIt, 91, subsentenceType);
    verifyHaveSubset(subbagIt, 100, subsentenceType);
    verifyHaveSubset(subsortedIt, 100, subsentenceType);
    
    for (AnnotationFS fs : fsArray) {
      ir.addFS(fs);
    }
    
    ir.removeAllExcludingSubtypes(subsentenceType);

    verifyHaveSubset(setIt, 91, sentenceType);
    verifyHaveSubset(bagIt, 100, sentenceType);
    verifyHaveSubset(sortedIt, 100, sentenceType);
    verifyMoveToFirst(subsetIt, false);
    verifyMoveToFirst(subbagIt, false);
    verifyMoveToFirst(subsortedIt, false);
  }
  
  
  private void verifyConcurrantModificationDetected(FSIterator<?> it) {
    boolean caught = false;
    try {
      it.moveToNext();
    } catch (Exception e) {
      caught = true;
    }
//    if (caught != true) {
//      System.out.println("Debug");
//    }
    assertTrue(caught);
  }
  
  
  public void testInvalidIndexRequest() {
    boolean exc = false;
    try {
      this.cas.getIndexRepository().getIndex(CASTestSetup.ANNOT_BAG_INDEX, this.stringType);
    } catch (CASRuntimeException e) {
      exc = true;
    }
    assertTrue(exc);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(IteratorTest.class);
  }

  private void fsBeginEndEqual(AnnotationFS fs1, AnnotationFS fs2) {
    assertEquals(fs1.getBegin(), fs2.getBegin());
    assertEquals(fs1.getEnd(), fs2.getEnd());     
  }
}
