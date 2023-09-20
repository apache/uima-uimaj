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
package org.apache.uima.cas.impl;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static org.apache.uima.cas.impl.Subiterator.BoundsUse.coveredBy;
import static org.apache.uima.cas.impl.Subiterator.BoundsUse.covering;
import static org.apache.uima.cas.impl.Subiterator.BoundsUse.notBounded;
import static org.apache.uima.cas.impl.Subiterator.BoundsUse.sameBeginEnd;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.SelectFSs;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.Subiterator.BoundsUse;
import org.apache.uima.cas.test.CASInitializer;
import org.apache.uima.cas.test.CASTestSetup;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

//@formatter:off
/**
 * Setup:  all kinds of types, primitives and non-primitives
 *         see CASTestSetup class
 *         
 *         Multiple Indexes, some sorted\
 *         
 *         setupTheCas - puts in tokens / phrases / sentences, overlapping
 *           tokens 0-4, 1-5, 2-6, etc.
 *           sentences 0-10, 5-15, 10-20, etc.    
 *             + 12-31
 *           phrases  0-5,  6-9,  10-16, 14-19, ...
 */
//@formatter:on
public class AnnotationIteratorTest {

  private static final boolean showFSs = false;

  private CAS cas;
  private Annotation[] ann;

  private TypeSystem ts;
  private Type stringType;
  private Type tokenType;
  private Type intType;
  private Type tokenTypeType;
  private Type wordType;
  private Type sepType;
  private Type eosType;
  private Type sentenceType;
  private Type phraseType;

  private Feature tokenTypeFeat;
  private Feature lemmaFeat;
  private Feature sentLenFeat;
  private Feature tokenFloatFeat;
  private Feature startFeature;
  private Feature endFeature;

  private boolean isSave;
  private List<Annotation> fss;
  private List<Integer> fssStarts = new ArrayList<>();
  private int callCount = -1;
  private Type[] types = new Type[3];

  @BeforeEach
  public void setUp() throws Exception {
    // make a cas with various types, fairly complex -- see CASTestSetup class
    cas = CASInitializer.initCas(new CASTestSetup(), null);
    assertTrue(cas != null);
    ts = cas.getTypeSystem();
    assertTrue(ts != null);

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
    sepType = ts.getType(CASTestSetup.SEP_TYPE);
    assertTrue(sepType != null);
    eosType = ts.getType(CASTestSetup.EOS_TYPE);
    assertTrue(eosType != null);
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
    endFeature = ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_END);
    assertTrue(endFeature != null);
    sentenceType = ts.getType(CASTestSetup.SENT_TYPE);
    assertTrue(sentenceType != null);
    phraseType = ts.getType(CASTestSetup.PHRASE_TYPE);
    assertTrue(phraseType != null);
    types[0] = sentenceType;
    types[1] = phraseType;
    types[2] = tokenType;
  }

  @AfterEach
  public void tearDown() {
    cas = null;
    ts = null;
    tokenType = null;
    intType = null;
    tokenTypeType = null;
    wordType = null;
    sepType = null;
    eosType = null;
    tokenTypeFeat = null;
    lemmaFeat = null;
    sentLenFeat = null;
    tokenFloatFeat = null;
    startFeature = null;
    endFeature = null;
    sentenceType = null;
  }

  // //debug
  // // explore which isValid calls can be eliminated
  // public void testIsValid() {
  // int annotCount = setupTheCas();
  // FSIndexRepository ir = cas.getIndexRepository();
  //
  // FSIterator<AnnotationFS> it = cas.getAnnotationIndex().iterator();
  // it.moveToLast();
  // int c = 0;
  // while (it.hasPrevious()) {
  // it.previous();
  // c++;
  // }
  // System.out.println("debug count = " + c);
  // }

  @Test
  public void testIterator1() throws Exception {
    final int annotCount = setupTheCas();

    FSIndexRepository indexRepository = cas.getIndexRepository();

    /***************************************************
     * iterate over them
     ***************************************************/
    fss = new ArrayList<>();
    callCount = -1;
    iterateOverAnnotations(annotCount, fss); // annotCount is the total number of sentences and
                                             // tokens

    callCount = -1;
    iterateOverAnnotations(annotCount, fss); // should be using flattened version

    /***************************************************
     * test skipping over multiple equal items at front
     ***************************************************/
    callCount = -1;
    fss.clear();
    isSave = true;

    AnnotationFS a1 = cas.createAnnotation(tokenType, 1, 6);
    a1.setStringValue(lemmaFeat, "lemma1");
    indexRepository.addFS(a1);

    AnnotationFS a2 = cas.createAnnotation(tokenType, 1, 6);
    a2.setStringValue(lemmaFeat, "lemma2");
    indexRepository.addFS(a2);

    AnnotationIndex<Annotation> tokenIndex = cas.getAnnotationIndex(tokenType);
    FSIterator<Annotation> it = tokenIndex.subiterator(a1);
    assertCount("multi equal", 0, it);

    FSIterator<Annotation> it2 = tokenIndex.subiterator(a1);
    // make a new iterator that hasn't been converted to a list form internally
    it2.moveTo(cas.getDocumentAnnotation());
    assertFalse(it2.isValid());
  }

//@formatter:off
  /**
   * The tests include:
   *   a) running with / w/o "flattened" indexes
   *   b) running forwards and backwards (testing moveToLast, isValid)
   *   c) testing strict and unambiguous variants
   *   d) running over all annotations and restricting to just a particular subtype
   *   
   *   new tests:  
   *     verifying bounding FS < all returned, including multiples of it
   *     strict at 1st element, at last element
   *     (not done yet) ConcurrentModificationException testing
   *     
   *     (not done yet) Testing with different bound styles
   *     
   * @param annotCount -
   * @param afss -
   */
//@formatter:on
  // called twice, the 2nd time should be with flattened indexes (List afss non empty the 2nd time)
  private void iterateOverAnnotations(final int annotCount, List<Annotation> afss)
          throws Exception {
    fss = afss;
    isSave = fss.size() == 0; // on first call is 0, so save on first call

    JCas jcas = cas.getJCas();
    AnnotationIndex<Annotation> annotIndex = cas.getAnnotationIndex();
    AnnotationIndex<Annotation> sentIndex = cas.getAnnotationIndex(sentenceType);

    // assertTrue((isSave) ? it instanceof FSIteratorWrapper :
    // FSIndexFlat.enabled ? it instanceof FSIndexFlat.FSIteratorFlat : it instanceof
    // FSIteratorWrapper);
    assertCount("Normal ambiguous annot iterator", annotCount, annotIndex.iterator(true));

    assertCount("Normal ambiguous select annot iterator", annotCount, annotIndex.select());
    assertCount("Normal ambiguous select annot iterator (type priorities)", annotCount,
            annotIndex.select().typePriority());
    assertEquals(annotCount, annotIndex.select().toArray().length); // stream op
    assertEquals(annotCount, annotIndex.select().asArray(Annotation.class).length); // select op
    assertEquals(annotCount - 5, annotIndex.select().startAt(2).asArray(Annotation.class).length);

    FSArray<Annotation> fsa = FSArray.create(jcas, annotIndex.select().asArray(Annotation.class));
    assertCount("fsa ambiguous select annot iterator", annotCount, fsa.select());
    assertCount("fsa ambiguous select annot iterator (type priorities)", annotCount,
            fsa.select().typePriority());

    NonEmptyFSList<Annotation> fslhead = (NonEmptyFSList<Annotation>) FSList
            .<Annotation, Annotation> create(jcas, annotIndex.select().asArray(Annotation.class));
    assertCount("fslhead ambiguous select annot iterator", annotCount, fslhead.select());
    assertCount("fslhead ambiguous select annot iterator (type priorities)", annotCount,
            fslhead.select().typePriority());

    // backwards
    assertCount("Normal select backwards ambiguous annot iterator", annotCount,
            annotIndex.select().backwards());
    assertCount("Normal select backwards ambiguous annot iterator (type priorities)", annotCount,
            annotIndex.select().typePriority().backwards());

    // because of document Annotation - spans the whole range
    assertCount("Unambiguous annot iterator", 1,
            // false means create an unambiguous iterator
            annotIndex.iterator(false));

    // because of document Annotation - spans the whole range
    assertCount("Unambiguous select annot iterator", 1, annotIndex.select().nonOverlapping());
    assertCount("Unambiguous select annot iterator (type priorities)", 1,
            annotIndex.select().typePriority().nonOverlapping());

    // because of document Annotation - spans the whole range
    assertCount("Unambiguous select backwards annot iterator", 1,
            annotIndex.select().nonOverlapping().backwards(true));
    assertCount("Unambiguous select backwards annot iterator (type priorities)", 1,
            annotIndex.select().typePriority().nonOverlapping().backwards(true));

    // false means create an unambiguous iterator
    assertCount("Unambigous sentence iterator", 5, sentIndex.iterator(false));

    assertCount("Unambigous select sentence iterator", 5,
            annotIndex.select(sentenceType).nonOverlapping(true));
    assertCount("Unambigous select sentence iterator (type priorities)", 5,
            annotIndex.select(sentenceType).typePriority().nonOverlapping(true));
    assertCount("Unambigous select sentence iterator", 5, sentIndex.select().nonOverlapping());
    assertCount("Unambigous select sentence iterator (type priorities)", 5,
            sentIndex.select().typePriority().nonOverlapping());

    AnnotationFS bigBound = cas.createAnnotation(sentenceType, 10, 41);
    // ambiguous, and strict
    assertThat(annotIndex.subiterator(bigBound, true, true)).toIterable().hasSize(38);
    assertCount("Subiterator over annot with big bound, strict", 38,
            annotIndex.subiterator(bigBound, true, true));
    assertCount("Subiterator select over annot with big bound, strict", 38, annotIndex.select()
            .coveredBy((Annotation) bigBound).includeAnnotationsWithEndBeyondBounds(false));
    assertCount("Subiterator select over annot with big bound, strict (type priorities)", 38,
            annotIndex.select().typePriority().coveredBy((Annotation) bigBound)
                    .includeAnnotationsWithEndBeyondBounds(false));

    assertThat(annotIndex.select().coveredBy(bigBound).limit(7)
            .includeAnnotationsWithEndBeyondBounds().asList())
                    .as("Subiterator select limit 7 over annot with big bound, strict")
                    .extracting(a -> a.getType(), a -> a.getBegin(), a -> a.getEnd())
                    .containsExactly( //
                            tuple(sentenceType, 10, 20), //
                            tuple(tokenType, 10, 15), //
                            tuple(tokenType, 11, 16), //
                            tuple(sentenceType, 12, 31), //
                            tuple(tokenType, 12, 17), //
                            tuple(tokenType, 13, 18), //
                            tuple(tokenType, 14, 19));
    assertCount("Subiterator select limit 7 over annot with big bound, strict", 7, annotIndex
            .select().coveredBy(bigBound).limit(7).includeAnnotationsWithEndBeyondBounds());
    assertCount("Subiterator select limit 7 over annot with big bound, strict (type priorities)", 7,
            annotIndex.select().typePriority().coveredBy(bigBound).limit(7)
                    .includeAnnotationsWithEndBeyondBounds());

    // uncomment these to check compile-time generic arguments OK
    // comment these out for running, because Token not a type
    // FSIndex<Token> token_index = annotIndex.subType(Token.class);
    // token_index.select().fsIterator();
    // select(token_index).fsIterator();
    // annotIndex.select(Token.class).fsIterator();
    // cas.select(Token.class).fsIterator();
    // token_index.select(Token.class).fsIterator();

    assertThat(annotIndex.select().coveredBy(bigBound).skip(3).toArray()).hasSize(35);

    Object[] o1 = annotIndex.select().coveredBy(bigBound).toArray();
    List<Annotation> l2 = annotIndex.select().coveredBy(bigBound).backwards().asList();
    Deque<Annotation> l2r = new ArrayDeque<>();
    for (Annotation fs : l2) {
      l2r.push(fs);
    }

    assertThat(o1).isEqualTo(l2r.toArray());

    // unambiguous, strict bigBound= sentenceType 10-41
    assertCount("Subiterator over annot unambiguous strict", 3,
            annotIndex.subiterator(bigBound, false, true));
    assertCount("Subiterator select over annot unambiguous strict", 3,
            annotIndex.select().coveredBy((Annotation) bigBound)
                    .includeAnnotationsWithEndBeyondBounds(false).nonOverlapping());
    assertCount("Subiterator select over annot unambiguous strict (type priorities)", 3,
            annotIndex.select().typePriority().coveredBy((Annotation) bigBound)
                    .includeAnnotationsWithEndBeyondBounds(false).nonOverlapping());
    assertCount("Subiterator select over annot unambiguous strict", 3,
            annotIndex.select().backwards().coveredBy((Annotation) bigBound)
                    .includeAnnotationsWithEndBeyondBounds(false).nonOverlapping());
    assertCount("Subiterator select over annot unambiguous strict (type priorities)", 3,
            annotIndex.select().backwards().coveredBy((Annotation) bigBound)
                    .includeAnnotationsWithEndBeyondBounds(false).nonOverlapping());

    // it = annotIndex.subiterator(bigBound, true, false);
    // while (it.hasNext()) {
    // Annotation a = (Annotation) it.next();
    // System.out.format("debug %s:%d b:%d e:%d%n", a.getType().getShortName(), a._id(),
    // a.getBegin(), a.getEnd());
    // }

    assertThat(annotIndex.subiterator(bigBound, true, false)).toIterable().containsExactly( //
            ann[58], ann[11], ann[12], //
            ann[76], ann[13], ann[14], ann[15], //
            ann[59], ann[16], //
            ann[69], ann[17], ann[18], ann[19], ann[20], //
            ann[60], ann[21], ann[22], //
            ann[70], ann[23], ann[24], //
            ann[71], ann[25], //
            ann[61], ann[26], ann[27], ann[28], ann[29], ann[30], //
            ann[62], ann[31], //
            ann[72], ann[32], ann[33], ann[34], ann[35], //
            ann[63], ann[36], ann[37], //
            ann[73], ann[38], ann[39], //
            ann[74], ann[40], //
            ann[64], ann[41], ann[42]);
    assertThat(annotIndex.subiterator(bigBound, true, false)).toIterable()
            .extracting(a -> asList(ann).indexOf(a), a -> a.getType(), a -> a.getBegin(),
                    a -> a.getEnd())
            .hasSize(46);
    assertCount("Subiterator over annot ambiguous not-strict", 46,
            annotIndex.subiterator(bigBound, true, false));
    // Using selectFS, we do not consider annotations that start at the end of the bounding
    // annotation to be included in the result, hence it is 45 here and not 46.
    assertCount("Subiterator select over annot ambiguous not-strict", 45,
            annotIndex.select().coveredBy(bigBound).includeAnnotationsWithEndBeyondBounds(true));
    assertCount("Subiterator select over annot ambiguous not-strict (type priorities)", 45,
            annotIndex.select().typePriority().coveredBy(bigBound)
                    .includeAnnotationsWithEndBeyondBounds(true));

    // covered by implies endWithinBounds
    assertCount("Subiterator select over annot ambiguous strict", 38,
            annotIndex.select().coveredBy(bigBound));
    assertCount("Subiterator select over annot ambiguous strict (type priorities)", 38,
            annotIndex.select().typePriority().coveredBy(bigBound));
    assertCount("Subiterator select over annot ambiguous strict", 38,
            annotIndex.select().coveredBy(bigBound).includeAnnotationsWithEndBeyondBounds(false));
    assertCount("Subiterator select over annot ambiguous strict (type priorities)", 38,
            annotIndex.select().typePriority().coveredBy(bigBound)
                    .includeAnnotationsWithEndBeyondBounds(false));

    // unambiguous, not strict
    assertCount("Subiterator over annot, unambiguous, not-strict", 4,
            annotIndex.subiterator(bigBound, false, false));
    assertCount("Subiterator select over annot unambiguous not-strict", 4, annotIndex.select()
            .nonOverlapping().coveredBy(bigBound).includeAnnotationsWithEndBeyondBounds(true));
    assertCount("Subiterator select over annot unambiguous not-strict (type priorities)", 4,
            annotIndex.select().typePriority().nonOverlapping().coveredBy(bigBound)
                    .includeAnnotationsWithEndBeyondBounds(true));

    AnnotationFS sent = cas.getAnnotationIndex(sentenceType).iterator().get();
    assertThat(annotIndex.subiterator(sent, false, true)).toIterable()
            .as("Subiterator over annot unambiguous strict")
            .extracting(a -> a.getType(), a -> a.getBegin(), a -> a.getEnd()).containsExactly( //
                    tuple(tokenType, 0, 5), //
                    tuple(tokenType, 5, 10));
    assertCount("Subiterator over annot unambiguous strict", 2,
            annotIndex.subiterator(sent, false, true));

    assertThat(annotIndex.select().nonOverlapping().coveredBy(sent).asList())
            .as("Subiterator select over annot unambiguous strict")
            .extracting(a -> a.getType(), a -> a.getBegin(), a -> a.getEnd()).containsExactly( //
                    tuple(tokenType, 0, 5), //
                    tuple(tokenType, 5, 10));
    assertCount("Subiterator select over annot unambiguous strict", 2,
            annotIndex.select().nonOverlapping().coveredBy(sent));
    assertCount("Subiterator select over annot unambiguous strict (type priorities)", 2,
            annotIndex.select().typePriority().nonOverlapping().coveredBy(sent));

    // strict skips first item
    bigBound = cas.createAnnotation(sentenceType, 11, 30);
    assertCount("Subiteratover over sent ambiguous strict", 4,
            sentIndex.subiterator(bigBound, true, true));
    assertCount("Subiteratover over sent ambiguous", 9,
            sentIndex.subiterator(bigBound, true, false));
    assertCount("Subiteratover over sent unambiguous", 1,
            sentIndex.subiterator(bigBound, false, false));

    // single, get, nullOK
    assertThat(annotIndex.select().nonOverlapping().get().getType().getShortName())
            .isEqualTo("DocumentAnnotation");
    assertThatExceptionOfType(CASRuntimeException.class)
            .isThrownBy(() -> annotIndex.select().nullOK(false).coveredBy(3, 3).get())
            .matches(e -> e.hasMessageKey(CASRuntimeException.SELECT_GET_NO_INSTANCES));

    assertNull(annotIndex.select().coveredBy(3, 3).nullOK().get());
    assertNotNull(annotIndex.select().get(3));
    assertNull(annotIndex.select().nullOK().coveredBy(3, 5).get(3));

    assertThatExceptionOfType(CASRuntimeException.class)
            .isThrownBy(() -> annotIndex.select().coveredBy(3, 5).get(3))
            .matches(e -> e.hasMessageKey(CASRuntimeException.SELECT_GET_NO_INSTANCES));

    assertThat(annotIndex.select().nonOverlapping().get().getType().getShortName())
            .isEqualTo("DocumentAnnotation");

    // because of document Annotation - spans the whole range
    assertCount("Unambiguous select annot iterator", 1, annotIndex.select().nonOverlapping());
    assertCount("Unambiguous select annot iterator (type priorities)", 1,
            annotIndex.select().typePriority().nonOverlapping());
    // because of document Annotation - spans the whole range
    assertCount("Unambiguous select backwards annot iterator", 1,
            annotIndex.select().nonOverlapping().backwards(true));
    assertCount("Unambiguous select backwards annot iterator (type priorities)", 1,
            annotIndex.select().typePriority().nonOverlapping().backwards(true));
    assertNotNull(annotIndex.select().nonOverlapping().single());

    assertThatExceptionOfType(CASRuntimeException.class)
            .isThrownBy(() -> annotIndex.select().coveredBy(3, 10).single())
            .matches(e -> e.hasMessageKey(CASRuntimeException.SELECT_GET_TOO_MANY_INSTANCES));

    assertThatExceptionOfType(CASRuntimeException.class)
            .isThrownBy(() -> annotIndex.select().coveredBy(3, 10).singleOrNull())
            .matches(e -> e.hasMessageKey(CASRuntimeException.SELECT_GET_TOO_MANY_INSTANCES));

    assertThatExceptionOfType(CASRuntimeException.class)
            .isThrownBy(() -> annotIndex.select().coveredBy(3, 5).single())
            .matches(e -> e.hasMessageKey(CASRuntimeException.SELECT_GET_NO_INSTANCES));

    annotIndex.select().coveredBy(3, 5).singleOrNull();

    assertCount("Following with limit", 2, annotIndex.select().following(2, 39).limit(2));
    assertCount("Following with limit (type priorities)", 2,
            annotIndex.select().typePriority().following(2, 39).limit(2));
    assertCount("Following backwards with limit", 2,
            annotIndex.select().following(2, 39).backwards().limit(2));
    assertCount("Following backwards with limit (type priorities)", 2,
            annotIndex.select().typePriority().following(2, 39).backwards().limit(2));

    assertCount("select source array", 21, fsa.select(sentenceType));
    assertCount("select source array (type priorities)", 21,
            fsa.select(sentenceType).typePriority());
    assertCount("select source array", 21, fslhead.select(sentenceType));
    assertCount("select source array (type priorities)", 21,
            fslhead.select(sentenceType).typePriority());

    /** covering **/
    annotIndex.select(sentenceType).covering(20, 30).forEachOrdered(
            f -> System.out.format("found fs start at %d end %d%n", f.getBegin(), f.getEnd()));

    annotIndex.select(sentenceType).covering(15, 19)
            .forEachOrdered(f -> System.out.format("covering 15, 19:   %s:%d   %d -  %d%n",
                    f.getType().getShortName(), f._id(), f.getBegin(), f.getEnd()));

    annotIndex.select(sentenceType).covering(37, 39).forEachOrdered(
            f -> System.out.format("covering sentences 37, 39:   %s:%d   %d -  %d%n",
                    f.getType().getShortName(), f._id(), f.getBegin(), f.getEnd()));

    annotIndex.select(phraseType).covering(15, 19)
            .forEachOrdered(f -> System.out.format("covering phrases 15, 19:   %s:%d   %d -  %d%n",
                    f.getType().getShortName(), f._id(), f.getBegin(), f.getEnd()));

    annotIndex.select(phraseType).covering(37, 39)
            .forEachOrdered(f -> System.out.format("covering phrases 37, 39:   %s:%d   %d -  %d%n",
                    f.getType().getShortName(), f._id(), f.getBegin(), f.getEnd()));
  }

  private String flatStateMsg(String s) {
    return s + (isSave ? "" : " with flattened index");
  }

  private <T extends Annotation> void assertCount(String msg, int expected, SelectFSs<T> select) {
    SelectFSs_impl<T> fssImpl = (SelectFSs_impl<T>) select;
    assertCount(msg, expected, select.fsIterator(), fssImpl.usesTypePriority());
  }

  private void assertCount(String msg, int expected, FSIterator<? extends Annotation> it) {
    assertCount(msg, expected, it, true);
  }

  private void assertCount(String msg, int expected, FSIterator<? extends Annotation> it,
          boolean typePriorities) {
    int fssStart = assertCountCmn(msg, expected, it);
    msg = flatStateMsg(msg);
    int count = expected;
    if (count > 0) {
      // test moveTo(fs) in middle, first, and last

      AnnotationFS middleFs = fss.get(fssStart + (count >> 1));
      // //debug
      // System.out.println(posFs.toString());
      // debug
      it.moveToLast();
      it.next();

      // Move to middle
      it.moveTo(middleFs);
      assertThat(it.get()).as(msg + " - middle position can be found by iterator")
              .usingComparator(
                      typePriorities ? comparing(Object::hashCode)
                              : comparing(Annotation::getBegin).thenComparing(Annotation::getEnd),
                      typePriorities ? "hashCode()" : "begin/end")
              .isEqualTo(middleFs);

      // Move to first
      Annotation firstFs = fss.get(fssStart);
      it.moveTo(firstFs);
      assertThat(it.get()).as(msg + " - first position can be found by iterator")
              .usingComparator(
                      typePriorities ? comparing(Object::hashCode)
                              : comparing(Annotation::getBegin).thenComparing(Annotation::getEnd),
                      typePriorities ? "hashCode()" : "begin/end")
              .isEqualTo(firstFs);

      it.moveToFirst();
      assertThat(it.get()).as(msg + " - moveToFirst positions at last result element")
              .usingComparator(
                      typePriorities ? comparing(Object::hashCode)
                              : comparing(Annotation::getBegin).thenComparing(Annotation::getEnd),
                      typePriorities ? "hashCode()" : "begin/end")
              .isEqualTo(firstFs);

      // Move to last
      Annotation lastFs = fss.get(fssStart + count - 1);
      it.moveTo(lastFs);
      assertThat(it.get()).as(msg + " - last position can be found by iterator")
              .usingComparator(
                      typePriorities ? comparing(Object::hashCode)
                              : comparing(Annotation::getBegin).thenComparing(Annotation::getEnd),
                      typePriorities ? "hashCode()" : "begin/end")
              .isEqualTo(lastFs);

      it.moveToLast();
      assertThat(it.get()).as(msg + " - moveToLast positions at last result element")
              .usingComparator(
                      typePriorities ? comparing(Object::hashCode)
                              : comparing(Annotation::getBegin).thenComparing(Annotation::getEnd),
                      typePriorities ? "hashCode()" : "begin/end")
              .isEqualTo(lastFs);
    } else {
      // count is 0
      it.moveToFirst();
      assertFalse(it.isValid());

      it.moveToLast();
      assertFalse(it.isValid());

      it.moveTo(cas.getDocumentAnnotation());
      assertFalse(it.isValid());
    }

    // Check that forwards step-by-step iteration yields same results as backwards step-by-step.
    List<AnnotationFS> annotations1 = new ArrayList<>();
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      annotations1.add(0, it.get());
    }

    count = 0;
    List<AnnotationFS> annotations2 = new ArrayList<>();
    for (it.moveToLast(); it.isValid(); it.moveToPrevious()) {
      annotations2.add(it.get());
      ++count;
    }

    assertThat(annotations2)
            .as("Found %d annotations forward but %d backwards", annotations1.size(),
                    annotations2.size())
            .extracting(a -> a.getType(), a -> a.getBegin(), a -> a.getEnd())
            .containsExactly(annotations1.stream()
                    .map(a -> tuple(a.getType(), a.getBegin(), a.getEnd())).toArray(Tuple[]::new));

    assertEquals(msg, expected, count);
  }

  // called by assertCount
  // called by asserCountLimit
  private int assertCountCmn(String msg, int expected, FSIterator<? extends Annotation> it) {
    // add with-flattened-index if isSave is false
    msg = flatStateMsg(msg);
    int count = 0;
    callCount++;

    int fssStart;
    if (isSave) {
      fssStarts.add(fssStart = fss.size());
    } else {
      fssStart = fssStarts.get(callCount);
    }

    while (it.isValid()) {
      ++count;
      Annotation fs = it.next();
      if (showFSs) {
        System.out.format("assertCountCmn: %2d %s   %10s  %d - %d%n", count, msg,
                fs.getType().getName(), fs.getBegin(), fs.getEnd());
      }
      if (isSave) {
        fss.add(fs);
      } else {
        assertEquals(msg, fss.get(fssStart + count - 1).hashCode(), fs.hashCode());
      }
    }

    assertEquals(msg, expected, count);
    return fssStart;
  }

  @Test
  public void testIncorrectIndexTypeException() {
    boolean caughtException = false;
    try {
      cas.getAnnotationIndex(stringType);
    } catch (CASRuntimeException e) {
      // e.printStackTrace();
      caughtException = true;
    }
    assertTrue(caughtException);

    caughtException = false;
    try {
      cas.getAnnotationIndex(ts.getType(CASTestSetup.TOKEN_TYPE_TYPE));
    } catch (CASRuntimeException e) {
      caughtException = true;
    }
    assertTrue(caughtException);
    try {
      cas.getAnnotationIndex(tokenType);
    } catch (CASRuntimeException e) {
      assertTrue(false);
    }
  }

  /**
   * UIMA-2808 - There was a bug in Subiterator causing the first annotation of the type of the
   * index the subiterator was applied to always to be returned, even if outside the boundary
   * annotation.
   */
  @Test
  public void testUnambiguousSubiteratorOnIndex() {
    try {
      // @formatter:off
      //                        0    0    1    1    2    2    3    3    4    4    5
      //                        0    5    0    5    0    5    0    5    0    5    0
      //                        ------- sentence ---------
      //                                                  -------- sentence ---------
      //                                                                        -tk-
      // @formatter:on
      cas.setDocumentText("Sentence A with no value. Sentence B with value 377.");
    } catch (CASRuntimeException e) {
      assertTrue(false);
    }
    AnnotationIndex<Annotation> ai = cas.getAnnotationIndex();

    cas.addFsToIndexes(cas.createAnnotation(sentenceType, 0, 25));
    cas.addFsToIndexes(cas.createAnnotation(sentenceType, 26, 52));
    cas.addFsToIndexes(cas.createAnnotation(tokenType, 48, 51));
    AnnotationIndex<Annotation> tokenIdx = cas.getAnnotationIndex(tokenType);

    // AnnotationIndex<AnnotationFS> si = cas.getAnnotationIndex(this.sentenceType);
    for (Annotation sa : ai.select(sentenceType)) {
      FSIterator<Annotation> ti2 = tokenIdx.subiterator(sa, false, false);

      while (ti2.hasNext()) {
        AnnotationFS t = ti2.next();
        assertTrue("Subiterator returned annotation outside boundaries",
                t.getBegin() < sa.getEnd());
      }
    }

    SelectFSs<Annotation> ssi = ai.select(sentenceType);

    for (AnnotationFS sa : ssi) {
      FSIterator<Annotation> ti2 = tokenIdx.select().coveredBy(sa)
              .includeAnnotationsWithEndBeyondBounds(false).nonOverlapping().fsIterator();

      while (ti2.hasNext()) {
        AnnotationFS t = ti2.next();
        assertTrue("Subiterator returned annotation outside boundaries",
                t.getBegin() < sa.getEnd());
      }
    }
  }

  // @formatter:off
  /**
   * Test subiterator edge cases
   * 
   *   skip over variations:                           -, i, t1, tn
   *     no match                                             -
   *     match - == id, using == id test                      i
   *     match - != id, using type test,                      t1 or tn
   *                  --  alternative: 1 or multiple to skip over 
   * 
   *   nothing before bound, nothing in bound, nothing after  n n n
   *   nothing before, nothing in bound, stuff after          n n s
   *   nothing before, something in bound, nothing after      n s n    skip over variation
   *   nothing before, something in bound, something after    n s s
   *                                                          
   *   stuff before bound, nothing in bound, nothing after    s n n
   *   stuff before bound, nothing in bound, stuff after      s n s
   *   stuff before, something in bound, nothing after        s s n
   *   stuff before, something in bound, something after      s s s
   *   
   *   test with bound before / after having their begin / end be different or the same
   *     (if the same, have the same or different type;
   *                   if the same type, have the equals-to-bound test be for the same type or same id
   *          
   *       begin end type idtst
   *         d    d   -     -    
   *         d    s   -     -
   *         s    d   -     -
   *         s    s   d     -  test with nnn, nns, nsn, nss, snn, sns, ssn, sss
   *                     p-    test with or without type priority  
   *         s    s   s     n    insure skip over both/multiple
   *         s    s   s     y    insure skip over just 1
   *      
   *   test with type priorities:
   *     skip (only covering)
   *     skipoverbound: if type priority off, can have bound in middle 
   *           
   *   setup notation:  any number of tuples separated by ':'
   *       xxx : yyy : zzz 
   *     each is either - or x-y-t  where x == begin, y == end, t = 0 1 or 2 type order
   */
  // @formatter:on
  private void setupEdges(String s) {
    String[] sa = s.split("\\:");
    for (String x : sa) {
      x = x.trim();
      if ("-".equals(x)) {
        continue;
      }
      String[] i3 = x.split("\\-");
      indexNew(types[Integer.parseInt(i3[2])], Integer.parseInt(i3[0]), Integer.parseInt(i3[1]));
    }
  }

  @Test
  public void testEdges() {
    Annotation ba = indexNew(phraseType, 10, 20); // the bounding annotation
    edge(ba, "-", coveredBy, "--:--:--:--", 0);
    edge(ba, "-", covering, "--:--:--:--", 0);
    edge(ba, "-", sameBeginEnd, "--:--:--:--", 0);
    edge(ba, "-", notBounded, "--:--:--:--", 0);

    edge(ba, "0-10-2", coveredBy, "--:--:--:--", 0);
    edge(ba, "0-10-2", covering, "--:--:--:--", 0);

    edge(ba, "0-10-2:11-20-2", coveredBy, "--:--:--:--", 1);
    edge(ba, "0-10-2:11-21-2", coveredBy, "--:--:--:--", 0);
    edge(ba, "0-10-2:11-21-2", coveredBy, "--:--:LE:--", 1);
  }

  /**
   * @param ba
   *          -
   * @param setup
   *          -
   * @param boundsUse
   *          -
   * @param flags
   *          TP type priority NO non overlapping LE include annotation with ends beyond bounds ST
   *          skip when same begin end type
   * @param count
   *          -
   */
  private void edge(Annotation ba, String setup, BoundsUse boundsUse, String flags, int count) {
    String[] fa = flags.split("\\:");
    cas.reset();
    AnnotationIndex<Annotation> ai = cas.getAnnotationIndex();
    SelectFSs<Annotation> sa;

    setupEdges(setup);

    switch (boundsUse) {
      case notBounded:
        sa = ai.select();
        break;
      case coveredBy:
        sa = ai.select().coveredBy(ba);
        break;
      case sameBeginEnd:
        sa = ai.select().at(ba);
        break;
      default:
      case covering:
        sa = ai.select().covering(ba);
        break;
    }

    if (fa[0].equals("TP"))
      sa.typePriority();
    if (fa[1].equals("NO"))
      sa.nonOverlapping();
    if (fa[2].equals("LE"))
      sa.includeAnnotationsWithEndBeyondBounds();
    if (fa[3].equals("ST"))
      sa.skipWhenSameBeginEndType();

    assertEquals(count, sa.fsIterator().size());
  }

  //
  // public void testEdges() {
  //
  // }

  private Annotation indexNew(Type type, int begin, int end) {
    Annotation a;
    cas.addFsToIndexes(a = (Annotation) cas.createAnnotation(type, begin, end));
    return a;
  }

  private int setupTheCas() {
    List<AnnotationFS> annotationList = new ArrayList<>();

    //@formatter:off
//  Tokens                +---+
//                       +---+
//                      +---+
//  BigBound                      +-----------------------------+
   final String text = "aaaa bbbb cccc dddd aaaa bbbb cccc dddd aaaa bbbb cccc dddd ";
//                      +--------+
//  Sentences                +--------+
//                                +----------+
//  one xtr sent                    +-----------------+  (12, 31)
//
//  Phrases             some overlap, some dont, 3-7 length
//
//  bound4strict                   +------------------+            
//  sentence4strict                 +-----------------------------+
   //@formatter:n
   
   try {
     cas.setDocumentText(text);
   } catch (CASRuntimeException e) {
     fail();
   }

    /***************************************************
     * Create and index tokens and sentences
     ***************************************************/
    FSIndexRepository ir = cas.getIndexRepository();
    int annotCount = 1; // Init with document annotation.
    annotationList.add(cas.getDocumentAnnotation());
    // create token and sentence annotations
    AnnotationFS fs;
    for (int i = 0; i < text.length() - 5; i++) {
      ++annotCount;
      ir.addFS(fs = cas.createAnnotation(tokenType, i, i + 5));
      annotationList.add(fs);
      if (showFSs) {
        System.out.format("creating: %d begin: %d end: %d type: %s%n", annotCount, fs.getBegin(),
                fs.getEnd(), fs.getType().getName());
      }
    }
    // for (int i = 0; i < text.length() - 5; i++) {
    // cas.getIndexRepository().addFS(cas.createAnnotation(tokenType, i, i+5));
    // }

    // create overlapping sentences for unambigious testing
    // begin = 0, 5, 10, ...
    // end = 10, 15, 20, ...
    // non-overlapping: 0-10, 10-20, etc.
    for (int i = 0; i < text.length() - 10; i += 5) {
      ++annotCount;
      ir.addFS(fs = cas.createAnnotation(sentenceType, i, i + 10));
      annotationList.add(fs);
      if (showFSs) {
        System.out.format("creating: %d begin: %d end: %d type: %s%n", annotCount, fs.getBegin(),
                fs.getEnd(), fs.getType().getName());
      }
    }

    // create overlapping and non-overlapping phrases
    // begin = 0, 6, 9, 15, 21, 24, 30, 36, ...
    // end = 5, 9, 16, 20, 24, 31, 35, 39, ...

    int beginAlt = 0, endAlt = 0;
    for (int i = 0; i < text.length() - 10; i += 5) {
      ++annotCount;
      ir.addFS(fs = cas.createAnnotation(phraseType, i + beginAlt, i + 5 + endAlt));
      annotationList.add(fs);
      beginAlt = (beginAlt == 1) ? -1 : beginAlt + 1; // sequence: start @ 0, then 1, -1, 0, 1, ...
      endAlt = (endAlt == -1) ? 1 : endAlt - 1; // sequence: start At 0, then -1, 1, 0, -1, ...
      if (showFSs) {
        System.out.format("creating: %d begin: %d end: %d type: %s%n", annotCount, fs.getBegin(),
                fs.getEnd(), fs.getType().getName());
      }
    }

    ++annotCount;
    ir.addFS(fs = cas.createAnnotation(sentenceType, 12, 31));
    annotationList.add(fs);
    if (showFSs) {
      System.out.format("creating: %d begin: %d end: %d type: %s%n", annotCount, fs.getBegin(),
              fs.getEnd(), fs.getType().getName());
    }

    assertThat(annotationList.size()).isEqualTo(annotCount);
    ann = annotationList.stream().toArray(Annotation[]::new);

    return annotCount;
  }
}
