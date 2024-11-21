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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

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
import org.apache.uima.cas.impl.FeatureStructureImplC;
import org.apache.uima.cas.impl.LowLevelIndex;
import org.apache.uima.cas.impl.LowLevelIndexRepository;
import org.apache.uima.cas.impl.LowLevelIterator;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.MultiThreadUtils;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class comment for IteratorTest.java goes here.
 * 
 */
public class IteratorTest {

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
  /** ss means snapshot */
  private FSIndex<FeatureStructure> ssBagIndex;

  private FSIndex<FeatureStructure> setIndex;
  /** ss means snapshot */
  private FSIndex<FeatureStructure> ssSetIndex;

  private FSIndex<FeatureStructure> sortedIndex;
  /** ss means snapshot */
  private FSIndex<FeatureStructure> ssSortedIndex;

  private FSIndex<?> jcasBagIndex;
  /** ss means snapshot */
  private FSIndex<?> jcasSsBagIndex;

  private FSIndex<?> jcasSetIndex;
  /** ss means snapshot */
  private FSIndex<?> jcasSsSetIndex;

  private FSIndex<?> jcasSortedIndex;
  /** ss means snapshot */
  private FSIndex<?> jcasSsSortedIndex;

  private JCas jcas;

  private FSIndex<FeatureStructure> wordSetIndex;
  /** ss means snapshot */
  private FSIndex<FeatureStructure> ssWordSetIndex;

  private Feature wordFeat;

  private Type wType;

  @BeforeEach
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
      cas = (CASImpl) ae.newCAS();
      jcas = cas.getJCas();
      assertTrue(cas != null);
      ts = cas.getTypeSystem();
      assertTrue(ts != null);
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

    stringType = ts.getType(CAS.TYPE_NAME_STRING);
    assertTrue(stringType != null);
    tokenType = ts.getType(CASTestSetup.TOKEN_TYPE);
    assertTrue(stringType != null);
    intType = ts.getType(CAS.TYPE_NAME_INTEGER);
    assertTrue(intType != null);
    tokenTypeType = ts.getType(CASTestSetup.TOKEN_TYPE_TYPE);
    assertTrue(tokenTypeType != null);
    wordType = ts.getType(CASTestSetup.WORD_TYPE);
    assertTrue(wordType != null);
    tokenTypeFeat = ts.getFeatureByFullName(CASTestSetup.TOKEN_TYPE_FEAT_Q);
    assertTrue(tokenTypeFeat != null);
    lemmaFeat = ts.getFeatureByFullName(CASTestSetup.LEMMA_FEAT_Q);
    assertTrue(lemmaFeat != null);
    sentLenFeat = ts.getFeatureByFullName(CASTestSetup.SENT_LEN_FEAT_Q);
    assertTrue(sentLenFeat != null);
    tokenFloatFeat = ts.getFeatureByFullName(CASTestSetup.TOKEN_FLOAT_FEAT_Q);
    assertTrue(tokenFloatFeat != null);
    startFeature = ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_BEGIN);
    assertTrue(startFeature != null);
    sentenceType = ts.getType(CASTestSetup.SENT_TYPE);
    assertTrue(sentenceType != null);
    annotationType = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    assertTrue(annotationType != null);
    subsentenceType = ts.getType("SubTypeOfSentence");
    assertTrue(subsentenceType != null);
  }

  @AfterEach
  public void tearDown() {
    cas = null;
    ts = null;
    stringType = null;
    tokenType = null;
    intType = null;
    tokenTypeType = null;
    wordType = null;
    tokenTypeFeat = null;
    lemmaFeat = null;
    sentLenFeat = null;
    tokenFloatFeat = null;
    startFeature = null;
    sentenceType = null;
    annotationType = null;
  }

  private void setupindexes() {
    bagIndex = cas.getIndexRepository().getIndex(CASTestSetup.ANNOT_BAG_INDEX);
    setIndex = cas.getIndexRepository().getIndex(CASTestSetup.ANNOT_SET_INDEX);
    sortedIndex = cas.getIndexRepository().getIndex(CASTestSetup.ANNOT_SORT_INDEX);

    ssBagIndex = bagIndex.withSnapshotIterators();
    ssSetIndex = setIndex.withSnapshotIterators();
    ssSortedIndex = sortedIndex.withSnapshotIterators();

    jcasBagIndex = jcas.getIndexRepository().getIndex(CASTestSetup.ANNOT_BAG_INDEX);
    jcasSetIndex = jcas.getIndexRepository().getIndex(CASTestSetup.ANNOT_SET_INDEX);
    jcasSortedIndex = jcas.getIndexRepository().getIndex(CASTestSetup.ANNOT_SORT_INDEX);

    jcasSsBagIndex = jcasBagIndex.withSnapshotIterators();
    jcasSsSetIndex = jcasSetIndex.withSnapshotIterators();
    jcasSsSortedIndex = jcasSortedIndex.withSnapshotIterators();

  }

  @Test
  public void testEmptySnapshotIterator() {
    setupindexes();
    FSIterator<FeatureStructure> it = sortedIndex.iterator();
    assertEquals(0, it.size());
    assertFalse(it.isValid());
    boolean ok = false;
    try {
      it.get();
    } catch (NoSuchElementException e) {
      ok = true;
    }
    assertTrue(ok);

    it = ssSortedIndex.iterator();
    assertEquals(0, it.size());
    assertFalse(it.isValid());
    ok = false;
    try {
      it.get();
    } catch (NoSuchElementException e) {
      ok = true;
    }
    assertTrue(ok);
  }

  @Test
  public void testGetIndexes() {
    Iterator<FSIndex<FeatureStructure>> it = cas.getIndexRepository().getIndexes();
    while (it.hasNext()) {
      assertNotNull(it.next());
    }
  }

  @Test
  public void testMoveTo() {
    // Add some arbitrary annotations
    for (int i = 0; i < 10; i++) {
      createFSs(i); // add annotation, sentence, and 3 tokens, all with same begin / end
    }
    final int start = 5;
    final int end = 7;
    FSIndexRepository repo = cas.getIndexRepository();
    for (int i = 0; i < 10; i++) {
      // add 10 annotations with start 5, end 7
      AnnotationFS annotation = cas.createAnnotation(annotationType, start, end);
      repo.addFS(annotation);
    }
    AnnotationFS match = cas.createAnnotation(annotationType, start, end);
    FSIndex<AnnotationFS> index = cas.getAnnotationIndex();
    FSIndex<AnnotationFS> ssIndex = index.withSnapshotIterators();
    FSIterator<AnnotationFS> it = index.iterator();
    assertEquals(60, it.size());
    FSIterator<AnnotationFS> ssit = ssIndex.iterator();
    assertEquals(60, it.size());
    it.moveTo(match); // should move to left-most of the 10 with start=5 end=7
    ssit.moveTo(match);
    assertTrue(index.compare(match, it.get()) == 0);
    assertTrue(index.compare(match, ssit.get()) == 0);

    // The contract of moveTo() says that any preceding FS must be smaller.
    it.moveToPrevious();
    ssit.moveToPrevious();
    assertTrue(index.compare(match, it.get()) > 0);
    assertTrue(index.compare(match, ssit.get()) > 0);
  }

  @Test
  public void testMoveToPastEnd() { // https://issues.apache.org/jira/browse/UIMA-4094
    cas.getIndexRepository().addFS(cas.createAnnotation(annotationType, 1, 2));

    AnnotationFS pastEnd = cas.createAnnotation(annotationType, 2, 3);
    FSIndex<AnnotationFS> index = cas.getAnnotationIndex();
    FSIterator<AnnotationFS> it = index.iterator();
    it.moveTo(pastEnd);
    assertFalse(it.isValid());

    index = index.withSnapshotIterators();
    it = index.iterator();
    assertEquals(1, it.size());
    it.moveTo(pastEnd);
    assertFalse(it.isValid());

  }

  @Test
  public void testMoveToFirstOfEqualOneType() {
    for (int i = 0; i < 2; i++) {
      cas.reset();
      if (i == 0) {
        cas.getIndexRepository().addFS(cas.createAnnotation(subsentenceType, 0, 1));
      }
      cas.getIndexRepository().addFS(cas.createAnnotation(subsentenceType, 1, 2));
      cas.getIndexRepository().addFS(cas.createAnnotation(subsentenceType, 1, 2));
      cas.getIndexRepository().addFS(cas.createAnnotation(subsentenceType, 1, 3));
      cas.getIndexRepository().addFS(cas.createAnnotation(subsentenceType, 2, 2));
      cas.getIndexRepository().addFS(cas.createAnnotation(subsentenceType, 2, 5));

      AnnotationFS testAnnot = cas.createAnnotation(subsentenceType, 1, 2);

      FSIndex<AnnotationFS> index = cas.getAnnotationIndex(subsentenceType);
      FSIterator<AnnotationFS> it = index.iterator();
      assertEquals((i == 0) ? 6 : 5, it.size());
      it.moveTo(testAnnot);
      for (int j = 0; j < 2; j++) {
        assertTrue(it.isValid());
        AnnotationFS fs = it.get();
        cas.getIndexRepository().addFS(testAnnot);
        cas.getIndexRepository().removeFS(testAnnot);
        assertEquals(1, fs.getBegin());
        assertEquals(2, fs.getEnd());
        it.moveToNext();
      }
      assertTrue(it.isValid());

      index = index.withSnapshotIterators();
      it = index.iterator();
      assertEquals((i == 0) ? 6 : 5, it.size());
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
    FeatureStructureImplC fsi;
    cas.getIndexRepository().addFS(cas.createAnnotation(annotationType, i * 2, (i * 2) + 1));
    cas.getIndexRepository().addFS(cas.createAnnotation(sentenceType, i * 2, (i * 2) + 1));
    cas.getIndexRepository().addFS(fsi = cas.createAnnotation(tokenType, i * 2, (i * 2) + 1));
    cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, i * 2, (i * 2) + 1));
    cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, i * 2, (i * 2) + 1));
    // //debug
    // System.out.format("Token at %,d %n", fsi.getAddress());
  }

  private void createFSsU() {
    cas.getIndexRepository().removeAllIncludingSubtypes(TOP.class);

    for (int i = 0; i < 5; i++) {
      cas.getIndexRepository().addFS(cas.createAnnotation(annotationType, i * 2, (i * 2) + 1));
    }
  }

  // private void debugls() {
  // LowLevelIndexRepository llir = this.cas.ll_getIndexRepository();
  // LowLevelIndex setIndexForType = llir.ll_getIndex(CASTestSetup.ANNOT_SET_INDEX,
  // ((TypeImpl)tokenType).getCode());
  // LowLevelIterator it = setIndexForType.ll_iterator();
  // it.moveToLast();
  // System.out.format("Last token in set index is %,d%n", it.ll_get());
  // }

  private void setupFSs() {
    cas.getIndexRepository().removeAllIncludingSubtypes(TOP.class);

    for (int i = 0; i < 10; i++) {
      createFSs(i); // i = 0 .. 9, 5 annot per: annotation, sentence, token, token, token
    }
    for (int i = 19; i >= 10; i--) {
      createFSs(i); // i = 19 .. 10 5 annot per: annotation, sentence, token, token, token
    }
  }

  @Test
  public void testMultithreadedIterator() {
    setupFSs();
    final FSIndex<FeatureStructure> bagIndex = cas.getIndexRepository()
            .getIndex(CASTestSetup.ANNOT_BAG_INDEX);
    final FSIndex<FeatureStructure> setIndex = cas.getIndexRepository()
            .getIndex(CASTestSetup.ANNOT_SET_INDEX);
    final FSIndex<FeatureStructure> sortedIndex = cas.getIndexRepository()
            .getIndex(CASTestSetup.ANNOT_SORT_INDEX);

    int numberOfCores = Math.min(50, Runtime.getRuntime().availableProcessors() * 5);

    System.out.println("test multicore iterator with " + numberOfCores + " threads");
    MultiThreadUtils.ThreadM[] threads = new MultiThreadUtils.ThreadM[numberOfCores];

    final Throwable[] tthrowable = new Throwable[1]; // trick to get a return value in a parameter
    tthrowable[0] = null;

    for (int i = 0; i < numberOfCores; i++) {
      final int finalI = i;
      threads[i] = new MultiThreadUtils.ThreadM() {

        @Override
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
        }
      };
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

  @Test
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

    // debugls(); //debug

    basicRemoveAdd(bagIndex);
    basicRemoveAdd(sortedIndex, 38, 39);
    // debugls(); //debug
    basicRemoveAdd(ssSortedIndex, 38, 39);
    basicRemoveAdd(setIndex, 38, 39);
    basicRemoveAdd(ssSetIndex, 38, 39);

    // /////////////////////////////////////////////////////////////////////////
    // Test fast fail. - uima v3 doesn't throw ConcurrentModificationException

    // fastFailTst(setIndex, true);
    // fastFailTst(ssSetIndex, false);
    //
    // fastFailTst(bagIndex, true);
    // fastFailTst(ssBagIndex, false);
    //
    // fastFailTst(sortedIndex, true);
    // fastFailTst(ssSortedIndex, false);

 // @formatter:off
    /**
     * Test copy-on-write - 
     *   insure that index mods are ignored in normal iteration
     *   insure that index mods are picked up for moveTo, moveToFirst, moveToLast
     */
 // @formatter:on
    createFSsU();

    cowTst(setIndex, true);
    cowTst(ssSetIndex, false);

    cowTst(bagIndex, true);
    cowTst(ssBagIndex, false);

    cowTst(sortedIndex, true);
    cowTst(ssSortedIndex, false);

    // debugls(); //debug

    setupFSs();

    // Test find()
    setupWords();

    tstWord(wordSetIndex);
    tstWord(ssWordSetIndex);

    // debugls(); //debug

    // moved IntArrayRBTtest for pointer iterators here
    FSIndexRepository iri = cas.getIndexRepository();
    FSIndex<FeatureStructure> setIndexOverTokens = iri.getIndex(CASTestSetup.ANNOT_SET_INDEX,
            tokenType);
    int[] expected = new int[setIndexOverTokens.size()];
    assertEquals(setIndexOverTokens.size(), 20);
    int i = 0;
    for (FeatureStructure fs : setIndexOverTokens) {
      expected[i++] = fs._id();
    }

    LowLevelIndexRepository llir = cas.ll_getIndexRepository();
    LowLevelIndex setIndexForType = llir.ll_getIndex(CASTestSetup.ANNOT_SET_INDEX,
            ((TypeImpl) tokenType).getCode());
    // int[] expected = {17, 53, 89, 125, 161, 197, 233, 269, 305, 341, 701, 665, 629, 593, 557,
    // 521, 485, 449, 413, 377};
    setIndexIterchk(setIndexForType, expected);

    FSIndex<FeatureStructure> setIndexOverSentences = iri.getIndex(CASTestSetup.ANNOT_SET_INDEX,
            sentenceType);
    expected = new int[setIndexOverSentences.size()];
    assertEquals(setIndexOverSentences.size(), 20);
    i = 0;
    for (FeatureStructure fs : setIndexOverSentences) {
      expected[i++] = fs._id();
    }

    setIndexForType = llir.ll_getIndex(CASTestSetup.ANNOT_SET_INDEX,
            ((TypeImpl) sentenceType).getCode());
    // expected = new int[] {12, 48, 84, 120, 156, 192, 228, 264, 300, 336, 696, 660, 624, 588, 552,
    // 516, 480, 444, 408, 372};
    setIndexIterchk(setIndexForType, expected);

    expected = new int[setIndex.size()];
    assertEquals(setIndex.size(), 60);
    i = 0;
    for (FeatureStructure fs : setIndex) {
      expected[i++] = fs._id();
    }

    setIndexForType = llir.ll_getIndex(CASTestSetup.ANNOT_SET_INDEX);
    // expected = new int[] {
    // 1, 44, 80, 116, 152, 188, 224, 260, 296, 332, 692, 656, 620, 584, 548, 512, 476, 440, 404,
    // 368,
    // 12, 48, 84, 120, 156, 192, 228, 264, 300, 336, 696, 660, 624, 588, 552, 516, 480, 444, 408,
    // 372,
    // 17, 53, 89, 125, 161, 197, 233, 269, 305, 341, 701, 665, 629, 593, 557, 521, 485, 449, 413,
    // 377};
    setIndexIterchk(setIndexForType, expected);

    setIndexForType = llir.ll_getIndex(CASTestSetup.ANNOT_SET_INDEX,
            ((TypeImpl) tokenType).getCode());
    LowLevelIterator it = setIndexForType.ll_iterator();
    assertEquals(20, it.size());
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
    assertEquals(expected.length, it.size());
    int[] r = new int[70];
    int i = 0;
    while (it.isValid()) {
      r[i++] = it.ll_get();
      it.moveToNext();
    }
    // r 17, 53, 89, 125, 161, 197, 233, 269, 305, 341, 719, 665, 629, 593, 557, 521, 485, 449, 413,
    // 395
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
    // StringBuilder sb = new StringBuilder();
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
        if (a._id() == b._id()) {
          System.err.format("set Iterator: should not have 2 identical elements%n%s%n", it);
          assertTrue(false);
        }
        // if (a.getType() == b.getType()) {
        // assertTrue(setIndex.compare(b, a) < 0);
        // System.out.println("diff types");
        // }
      }
      b = a;
      // sb.append(String.format("%d %d debug: n=%d, type=%s, start=%d, end-%d%n",
      // threadNumber,
      // ii++,
      // a._id(),
      // a.getType().getName(),
      // a.getBegin(),
      // a.getEnd()));
      v.add(it.get()._id());
      it.moveToNext();
    }
    // System.out.println("Number of annotations: " + v.size());
    if (v.size() != ((10 * 3) + (10 * 3))) {
      System.err.format("Expected number in set was 60, but has %d elements%n%s%n", v.size(), it);
      // System.err.println(sb);
      assertTrue(false);
    }
    // else
    // System.out.println(sb);

    it = setIndex.iterator();
    it.moveToLast();
    int current = v.size() - 1;
    while (it.isValid() && (current >= 0)) {
      // System.out.println("Current: " + current);
      a = (AnnotationFS) it.get();
      // System.out.println(
      // a.getType().getName() + " - " + a.getStart() + " - " +
      // a.getEnd());
      assertTrue(it.get()._id() == v.get(current));
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
      assertTrue(it.get()._id() == v.get(current));
      it.moveToNext();
      assertTrue(it.isValid());
      assertTrue(it.get()._id() == v.get(current + 1));
      it.moveToPrevious();
      assertTrue(it.isValid());
      assertTrue(it.get()._id() == v.get(current));
      ++current;
    }

    // also test Java-style iteration
    Iterator<FeatureStructure> javaIt = setIndex.iterator();
    current = 0;
    while (javaIt.hasNext()) {
      assertEquals(javaIt.next()._id(), v.get(current++));
    }
  }

  private void findTst(FSIndex index, FSIndex jcasIndex) {
    findTestCas(index);
    findTestJCas(jcasIndex);
  }

  // called for bag indexes - can't know the begin/end for these - they're hash sets
  private void basicRemoveAdd(FSIndex<FeatureStructure> index) {
    FSIterator<FeatureStructure> it = index.iterator();
    it.moveToLast();
    Annotation a = (Annotation) it.get();
    basicRemoveAdd(index, a.getBegin(), a.getEnd());
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

  /**
   * 
   * @param index
   *          - the index to manipulate
   * @param isCow
   *          - false for "snapshot" indexes - these don't do cow (copy on write) things
   */
  private void cowTst(FSIndex<FeatureStructure> index, boolean isCow) {
    LowLevelIterator<FeatureStructure> it = (LowLevelIterator) index.iterator();
    it.moveToLast();
    it.moveToFirst();
    // moved to first, 2.7.0, because new bag iterator is more forgiving re concurrentmodexception
    FeatureStructure a = it.get();

    cas.removeFsFromIndexes(a);
    it.next();
    it.previous();
    assertTrue(it.get() == a); // gets the removed one
    it.moveToFirst(); // resets cow, does nothing for snapshot
    assertTrue(isCow ? (it.get() != a) : (it.get() == a));

    cas.addFsToIndexes(a);
    it.moveToLast();
    a = it.get();

    cas.removeFsFromIndexes(a);
    it.previous();
    it.next();
    assertTrue(0 == index.compare(it.get(), a));
    it.moveToLast();
    assertTrue(isCow ? (it.get() != a) : (it.get() == a));

    cas.addFsToIndexes(a); // index mod causes cow to copy, and index wr_cow to be set to null
    it.moveToFirst(); // moveToFirst -> maybeReinitIterator (if cow is copy), new cow, index wr_cow
                      // refs it

    it.moveToNext();

    a = it.get();

    cas.removeFsFromIndexes(a); // resets wr_cow to null for assoc. index, because it now ref-ng
                                // copy
    it.previous();
    it.isValid();
    it.next();
    assertTrue(it.get() == a); // gets the removed one
    it.moveTo(a); // causes COW reset, a is not present (has been removed)
    // if (isCow && index == setIndex || index == bagIndex) {
    // assertFalse(it.isValid()); // moveTo on set with no match gives invalid iterator
    // } else {
    assertTrue(isCow ? (it.isMoveToSupported() ? it.get() != a : true) : (it.get() == a));
    // }
    cas.addFsToIndexes(a); // add it back for subsequent tests
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
    expectCCE(a, it, false); // ok because above expectCCE reset the iterator
  }

  private void expectCCE(FeatureStructure a, FSIterator it, boolean isShouldFail) {
    boolean ok = false;
    isShouldFail = false; // after fix
    try {
      it.moveToNext();
      it.get(); // for set/sorted, the get does the actual "move" operation
    } catch (ConcurrentModificationException e) {
      ok = true;
    }
    assertTrue(isShouldFail ? ok : !ok);

    it.moveTo(a); // should reset concurrent mod,
    ok = true;
    try {
      it.moveToNext();
    } catch (ConcurrentModificationException e) {
      ok = false;
    }
    // if (!ok) {
    // System.out.println("debug");
    // }
    assertTrue(ok);
  }

  private void setupWords() {
    wType = cas.getTypeSystem().getType("org.apache.uima.cas.test.types.Word");

    wordFeat = wType.getFeatureByBaseName("word");

    for (int i = 0; i < 20; i++) {
      FeatureStructure fs = cas.createFS(wType);
      fs.setStringValue(wordFeat, "word" + i);
      cas.getIndexRepository().addFS(fs);
    }

    wordSetIndex = cas.getIndexRepository().getIndex("Word Set Index");
    ssWordSetIndex = wordSetIndex.withSnapshotIterators();

  }

  private void tstWord(FSIndex<FeatureStructure> index) {
    FSIterator<FeatureStructure> it = index.iterator();
    assertEquals(20, it.size()); // test size
    it.moveToLast();

    FeatureStructure fs = cas.createFS(wType);
    fs.setStringValue(wordFeat, "word1");

    // TEST moveTo() and get()
    it.moveTo(fs);

    assertSame(fs.getType(), it.get().getType());

    Type t1 = fs.getType();
    Type t2 = wordSetIndex.find(fs).getType();
    assertSame(t1, t2);

  }

  private void findTestCas(FSIndex<FeatureStructure> index) {
    AnnotationFS annot = (AnnotationFS) index.iterator().get(); // first element
    // if (null == index.find(annot)) {
    // System.out.println("debug");
    // }
    assertNotNull(index.find(annot));
    assertNull(index.find(cas.createAnnotation(annotationType, -1, -1)));
  }

  private void findTestJCas(FSIndex<?> index) {
    Annotation annot = (Annotation) index.iterator().get(); // first element
    assertNotNull(index.find(annot));
    assertNull(index.find(cas.createAnnotation(annotationType, -1, -1)));
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
      int hc = a._id();
      v.add(hc);
      // if ((hc % 2) == 1) {
      Thread.yield();
      // }
      it.moveToNext();
    }
    assertTrue(sortedIndex.size() == v.size());

    // Test moveTo()
    List<AnnotationFS> list = new ArrayList<>();
    FSIterator<AnnotationFS> it2 = cas.getAnnotationIndex().iterator();
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
    // assertTrue(it.get()._id() == v.get(i));
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
      assertTrue(it.get()._id() == v.get(current));
      it.moveToNext();
      assertTrue(it.isValid());
      assertTrue(it.get()._id() == v.get(current + 1));
      it.moveToPrevious();
      assertTrue(it.isValid());
      assertTrue(it.get()._id() == v.get(current));
      ++current;
    }

    // also test Java-style iteration
    Iterator<FeatureStructure> javaIt = sortedIndex.iterator();
    current = 0;
    while (javaIt.hasNext()) {
      assertEquals(javaIt.next()._id(), v.get(current++));
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
    // int debug_i = 0;
    while (true) {
      // if (debug_i == 20) {
      // System.out.println("Debug");
      // }
      if (!it.isValid()) {
        break;
      }
      a = (AnnotationFS) it.get();
      // debug_i ++;
      assertTrue(a != null);
      // bag indices no longer are in sort by fs order
      // if (b != null) {
      // assertTrue(bagIndex.compare(b, a) <= 0);
      // }
      b = a;
      v.add(a._id());
      it.moveToNext();
    }
    assertTrue(bagIndex.size() == v.size());

    // Check that reverse iterator produces reverse sequence.
    it.moveToLast();
    for (int i = v.size() - 1; i >= 0; i--) {
      // if (!it.isValid()) {
      // System.out.println("debug");
      // }
      assertTrue(it.isValid());
      assertTrue(it.get()._id() == v.get(i));
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
      assertTrue(it.get()._id() == v.get(current));
      it.moveToNext();
      assertTrue(it.isValid());
      assertTrue(it.get()._id() == v.get(current + 1));
      // if (current == 19) {
      // System.out.println("debug");
      // }
      it.moveToPrevious();
      assertTrue(it.isValid());
      assertTrue(it.get()._id() == v.get(current));
      ++current;
    }

    // also test Java-style iteration
    Iterator<FeatureStructure> javaIt = bagIndex.iterator();
    current = 0;
    while (javaIt.hasNext()) {
      assertEquals(javaIt.next()._id(), v.get(current++));
    }

    // Test iterator copy.
    FSIterator<AnnotationFS> source, copy;
    source = cas.getAnnotationIndex().iterator();
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
    FSIndexRepository ir = cas.getIndexRepository();
    for (int i = 0; i < fsArray.length; i++) {
      // key order:
      // 0 ... 50 200 ... 160 66 66 66 ..
      // item order
      // 0 - 50, 90 - 99, 89 - 60
      int j = (i >= 90) ? 66 : // some constant keys
              (i > 50) ? 200 - i : // some decreasing keys
                      i; // some increasing keys
      fsArray[i] = cas.createAnnotation(type, j * 5, (j * 5) + 4);
      ir.addFS(fsArray[i]);
    }
  }

  /**
   * Test deleting FSs from indexes.
   */
  @Test
  public void testDelete() {
    // Create a bunch of FSs.
    // have 10% of them be the same key
    // have the order be scrambled somewhat, not strictly increasing
    AnnotationFS[] fsArray = new AnnotationFS[100];
    FSIndexRepository ir = cas.getIndexRepository();
    addAnnotations(fsArray, tokenType);

    FSIndex<FeatureStructure> setIndex = cas.getIndexRepository()
            .getIndex(CASTestSetup.ANNOT_SET_INDEX, tokenType);
    FSIterator<FeatureStructure> set_iterator = setIndex.iterator();

    FSIndex<AnnotationFS> sortedIndex = cas.getAnnotationIndex(tokenType);
    FSIterator<AnnotationFS> sortedIt = sortedIndex.iterator();

    FSIndex<FeatureStructure> bagIndex = ir.getIndex(CASTestSetup.ANNOT_BAG_INDEX, tokenType);
    FSIterator<FeatureStructure> bagIt = bagIndex.iterator();

    // verify that the index is the right type https://issues.apache.org/jira/browse/UIMA-2883
    assertEquals(setIndex.getIndexingStrategy(), FSIndex.SET_INDEX);
    assertEquals(sortedIndex.getIndexingStrategy(), FSIndex.SORTED_INDEX);
    assertEquals(bagIndex.getIndexingStrategy(), FSIndex.BAG_INDEX);

    // For each index, check that the FSs are actually in the index.
    for (int i = 0; i < fsArray.length; i++) {
      set_iterator.moveTo(fsArray[i]);
      assertTrue(set_iterator.isValid());
      assertTrue(set_iterator.get().equals(fsArray[(i < 90) ? i : 90]));

      bagIt.moveTo(fsArray[i]);
      assertTrue(bagIt.isValid());
      assertTrue(bagIt.get().equals(fsArray[i]));

      sortedIt.moveTo(fsArray[i]);
      assertTrue(sortedIt.isValid());
      fsBeginEndEqual(sortedIt.get(), fsArray[i]);
    }
    sortedIt.moveToFirst();
    // item order
    // 0 - 50, 90 - 99, 89 - 60
    for (int i = 0; i < fsArray.length; i++) {
      int j = (i >= 61) ? (89 - (i - 61)) : (i >= 51) ? 90 : i;
      fsBeginEndEqual(sortedIt.get(), fsArray[j]);
      sortedIt.moveToNext();
    }
    assertFalse(sortedIt.isValid());

    // Remove an annotation, then add it again. Try setting the iterators to
    // that FS. The iterator should either be invalid, or point to a
    // different FS.
    for (int i = 0; i < fsArray.length; i++) {
      ir.removeFS(fsArray[i]);
      ir.removeFS(fsArray[i]); // a 2nd remove should be a no-op
                               // https://issues.apache.org/jira/browse/UIMA-2934

      // due to copy on write, need a new instance of set_iterator
      set_iterator = setIndex.iterator();
      set_iterator.moveTo(fsArray[i]);
      if (set_iterator.isValid()) {
        int oldRef = cas.ll_getFSRef(fsArray[i]);
        int newRef = cas.ll_getFSRef(set_iterator.get());
        assertTrue(oldRef != newRef);
        assertTrue(!set_iterator.get().equals(fsArray[i]));
      }

      // due to copy on write, need a new instance of set_iterator
      bagIt = bagIndex.iterator();
      // assertFalse(bagIt.hasNext()); // invalid test: removing one item from a bag index doesn't
      // remove the "last" element.
      assertEquals(99, bagIndex.size());
      bagIt.moveTo(fsArray[i]);
      assertFalse(bagIt.isValid());

      // due to copy on write, need a new instance of set_iterator
      sortedIt = sortedIndex.iterator();
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

    // due to copy on write, need a new instance of set_iterator
    bagIt = bagIndex.iterator();
    bagIt.moveToFirst();
    assertFalse(bagIt.isValid());

    // due to copy on write, need a new instance of set_iterator
    set_iterator = setIndex.iterator();
    set_iterator.moveToFirst();
    assertFalse(set_iterator.isValid());

    // due to copy on write, need a new instance of set_iterator
    sortedIt = sortedIndex.iterator();
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

  @Test
  public void testRemoveAll() {
    AnnotationFS[] fsArray = new AnnotationFS[100];
    AnnotationFS[] subFsArray = new AnnotationFS[100];
    FSIndexRepository ir = cas.getIndexRepository();

    addAnnotations(fsArray, ts.getType("Sentence"));
    addAnnotations(subFsArray, ts.getType("SubTypeOfSentence"));

    FSIndex<FeatureStructure> setIndex = ir.getIndex(CASTestSetup.ANNOT_SET_INDEX, sentenceType);
    FSIndex<FeatureStructure> bagIndex = ir.getIndex(CASTestSetup.ANNOT_BAG_INDEX, sentenceType);
    FSIndex<AnnotationFS> sortedIndex = cas.getAnnotationIndex(sentenceType);

    FSIndex<FeatureStructure> subsetIndex = ir.getIndex(CASTestSetup.ANNOT_SET_INDEX,
            subsentenceType);
    FSIndex<FeatureStructure> subbagIndex = ir.getIndex(CASTestSetup.ANNOT_BAG_INDEX,
            subsentenceType);
    FSIndex<AnnotationFS> subsortedIndex = cas.getAnnotationIndex(subsentenceType);

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

    // copy on write should prevent any change
    assertEquals(182, ((LowLevelIterator<FeatureStructure>) setIt).ll_indexSizeMaybeNotCurrent());
    assertEquals(200, ((LowLevelIterator<FeatureStructure>) bagIt).ll_indexSizeMaybeNotCurrent());
    assertEquals(200, ((LowLevelIterator<AnnotationFS>) sortedIt).ll_indexSizeMaybeNotCurrent());
    assertEquals(91, ((LowLevelIterator<FeatureStructure>) subsetIt).ll_indexSizeMaybeNotCurrent());
    assertEquals(100,
            ((LowLevelIterator<FeatureStructure>) subbagIt).ll_indexSizeMaybeNotCurrent());
    assertEquals(100, ((LowLevelIterator<AnnotationFS>) subsortedIt).ll_indexSizeMaybeNotCurrent());

    ir.removeAllIncludingSubtypes(sentenceType);

    assertEquals(182, ((LowLevelIterator<FeatureStructure>) setIt).ll_indexSizeMaybeNotCurrent());
    assertEquals(200, ((LowLevelIterator<FeatureStructure>) bagIt).ll_indexSizeMaybeNotCurrent());
    assertEquals(200, ((LowLevelIterator<AnnotationFS>) sortedIt).ll_indexSizeMaybeNotCurrent());
    assertEquals(91, ((LowLevelIterator<FeatureStructure>) subsetIt).ll_indexSizeMaybeNotCurrent());
    assertEquals(100,
            ((LowLevelIterator<FeatureStructure>) subbagIt).ll_indexSizeMaybeNotCurrent());
    assertEquals(100, ((LowLevelIterator<AnnotationFS>) subsortedIt).ll_indexSizeMaybeNotCurrent());

    // skip - no concurrent mod
    // verifyConcurrantModificationDetected(setIt);
    // verifyConcurrantModificationDetected(bagIt);
    // verifyConcurrantModificationDetected(sortedIt);
    // verifyConcurrantModificationDetected(subsetIt);
    // verifyConcurrantModificationDetected(subbagIt);
    // verifyConcurrantModificationDetected(subsortedIt);

    // due to copy on write, need to get new iterators
    setIt = setIndex.iterator();
    bagIt = bagIndex.iterator();
    sortedIt = sortedIndex.iterator();

    subsetIt = subsetIndex.iterator();
    subbagIt = subbagIndex.iterator();
    subsortedIt = subsortedIndex.iterator();

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

    // due to copy on write, need to get new iterators
    setIt = setIndex.iterator();
    bagIt = bagIndex.iterator();
    sortedIt = sortedIndex.iterator();

    subsetIt = subsetIndex.iterator();
    subbagIt = subbagIndex.iterator();
    subsortedIt = subsortedIndex.iterator();

    verifyMoveToFirst(setIt, true);
    verifyMoveToFirst(bagIt, true);
    verifyMoveToFirst(sortedIt, true);
    verifyMoveToFirst(subsetIt, true);
    verifyMoveToFirst(subbagIt, true);
    verifyMoveToFirst(subsortedIt, true);

    ir.removeAllExcludingSubtypes(sentenceType);

    // due to copy on write, need to get new iterators
    setIt = setIndex.iterator();
    bagIt = bagIndex.iterator();
    sortedIt = sortedIndex.iterator();

    subsetIt = subsetIndex.iterator();
    subbagIt = subbagIndex.iterator();
    subsortedIt = subsortedIndex.iterator();

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

    // due to copy on write, need to get new iterators
    setIt = setIndex.iterator();
    bagIt = bagIndex.iterator();
    sortedIt = sortedIndex.iterator();

    subsetIt = subsetIndex.iterator();
    subbagIt = subbagIndex.iterator();
    subsortedIt = subsortedIndex.iterator();

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

    assertFalse(caught); // because of copy-on-write

    // if (it.isValid()) {
    // it.isValid(); // debug
    // assertTrue(caught); // v3: it becomes invalid
    // }
    // if (caught != true) {
    // System.out.println("Debug");
    // }
  }

  @Test
  public void testInvalidIndexRequest() {
    boolean exc = false;
    try {
      cas.getIndexRepository().getIndex(CASTestSetup.ANNOT_BAG_INDEX, stringType);
    } catch (CASRuntimeException e) {
      exc = true;
    }
    assertTrue(exc);
  }

  private void fsBeginEndEqual(AnnotationFS fs1, AnnotationFS fs2) {
    assertEquals(fs1.getBegin(), fs2.getBegin());
    assertEquals(fs1.getEnd(), fs2.getEnd());
  }
}
