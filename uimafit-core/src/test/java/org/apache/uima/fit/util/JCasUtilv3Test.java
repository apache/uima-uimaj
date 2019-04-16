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


 getCoveredAnnotations() contains code adapted from the UIMA Subiterator class.
 */
package org.apache.uima.fit.util;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.util.JCasUtil.getAnnotationType;
import static org.apache.uima.fit.util.JCasUtil.getView;
import static org.apache.uima.fit.util.JCasUtil.indexCovered;
import static org.apache.uima.fit.util.JCasUtil.indexCovering;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectAt;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;
import static org.apache.uima.fit.util.JCasUtil.selectSingleAt;
import static org.apache.uima.fit.util.JCasUtil.toText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.type.AnalyzedText;
import org.apache.uima.fit.type.Sentence;
import org.apache.uima.fit.type.Token;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Test;

/**
 * Test cases for {@link JCasUtil}.
 * 
 */
public class JCasUtilv3Test extends ComponentTestBase {
  /**
   * Test Tokens (Stems + Lemmas) overlapping with each other.
   */
  @Test
  public void testSelectCoveredOverlapping() {
    add(jCas, 3, 16);
    add(jCas, 37, 61);
    add(jCas, 49, 75);
    add(jCas, 54, 58);
    add(jCas, 66, 84);

    for (Token t : select(jCas, Token.class)) {
      // The naive approach is assumed to be correct
      // uimaFIT: selectCovered(jCas, Sentence.class, t.getBegin(), t.getEnd());
      List<Sentence> stem1 = jCas.select(Sentence.class).coveredBy(t.getBegin(), t.getEnd()).asList();
      // uimaFIT: selectCovered(jCas, Sentence.class, t);
      List<Sentence> stem2 = jCas.select(Sentence.class).coveredBy(t).asList();
      check(jCas, t, stem1, stem2);
    }
  }

  /**
   * Test what happens if there is actually nothing overlapping with the Token.
   */
  @Test
  public void testSelectCoveredNoOverlap() {
    new Sentence(jCas, 3, 31).addToIndexes();
    new Sentence(jCas, 21, 21).addToIndexes();
    new Sentence(jCas, 24, 44).addToIndexes();
    new Sentence(jCas, 30, 45).addToIndexes();
    new Sentence(jCas, 32, 43).addToIndexes();
    new Sentence(jCas, 47, 61).addToIndexes();
    new Sentence(jCas, 48, 77).addToIndexes();
    new Sentence(jCas, 65, 82).addToIndexes();
    new Sentence(jCas, 68, 80).addToIndexes();
    new Sentence(jCas, 72, 65).addToIndexes();

    new Token(jCas, 73, 96).addToIndexes();

    for (Token t : select(jCas, Token.class)) {
      // The naive approach is assumed to be correct
      // uimaFIT: selectCovered(jCas, Sentence.class, t.getBegin(), t.getEnd());
      List<Sentence> stem1 = jCas.select(Sentence.class).coveredBy(t.getBegin(), t.getEnd()).asList();
      // uimaFIT: selectCovered(jCas, Sentence.class, t);
      List<Sentence> stem2 = jCas.select(Sentence.class).coveredBy(t).asList();
      check(jCas, t, stem1, stem2);
    }
  }

  @Test
  public void testSelectCoverRandom() throws Exception {
    final int ITERATIONS = 10;

    for (int i = 0; i < ITERATIONS; i++) {
      CAS cas = jCas.getCas();
      initRandomCas(cas, 10 * i);

      JCas jcas = cas.getJCas();
      Collection<Sentence> sentences = select(jcas, Sentence.class);
      
      long timeNaive = 0;
      long timeOptimized = 0;
      
      // Prepare the index
      long timeIndexed = System.currentTimeMillis();
      Map<Sentence, List<Token>> index = indexCovered(jcas, Sentence.class, Token.class);
      timeIndexed = System.currentTimeMillis() - timeIndexed;
      
      // -- The order of entries in the index is NOT defined!
      // Check that order of indexed sentences corresponds to regular CAS-index order
      // List<Sentence> relevantSentences = new ArrayList<>(sentences);
      // relevantSentences.retainAll(index.keySet());
      // assertEquals(relevantSentences, new ArrayList<>(index.keySet()));
      
      for (Sentence t : sentences) {
        long ti = System.currentTimeMillis();
        // The naive approach is assumed to be correct
        // uimaFIT: selectCovered(jcas, Token.class, t.getBegin(), t.getEnd());
        List<Token> expected = jcas.select(Token.class).coveredBy(t.getBegin(), t.getEnd()).asList();
        timeNaive += System.currentTimeMillis() - ti;

        // Record time for optimized selectCovered
        ti = System.currentTimeMillis();
        // uimaFIT: selectCovered(jcas, Token.class, t);
        List<Token> actual1 = jcas.select(Token.class).coveredBy(t).asList();
        timeOptimized += System.currentTimeMillis() - ti;

        // Record index lookup time
        ti = System.currentTimeMillis();
        Collection<Token> actual2 = index.get(t);
        timeIndexed += System.currentTimeMillis() - ti;

        check(jcas, t, expected, actual1);
        check(jcas, t, expected, actual2);
        
        // System.out.printf("%n--- OK ---------------%n%n");
      }
      System.out.printf(
              "%3d Optimized: speed up factor %3.2f [naive:%4d optimized:%4d (diff:%4d)]%n", i,
              (double) timeNaive / (double) timeOptimized, timeNaive, timeOptimized,
              timeNaive - timeOptimized);
      System.out.printf(
              "%3d Indexed:   speed up factor %3.2f [naive:%4d indexed  :%4d (diff:%4d)]%n%n", i,
              (double) timeNaive / (double) timeIndexed, timeNaive, timeIndexed,
              timeNaive - timeIndexed);
    }
  }

  /**
   * Test what happens if there is actually nothing overlapping with the Token.
   */
  @Test
  public void testSelectBetweenInclusion() {
    Token t1 = new Token(jCas, 45, 57);
    t1.addToIndexes();
    Token t2 = new Token(jCas, 52, 52);
    t2.addToIndexes();

    new Sentence(jCas, 52, 52).addToIndexes();

    // uimaFIT: selectBetween(jCas, Sentence.class, t1, t2);
    List<Sentence> stem1 = jCas.select(Sentence.class).between(t1, t2).asList();
    assertTrue(stem1.isEmpty());
  }

  @Test
  public void testSelectBetweenRandom() throws Exception {
    final int ITERATIONS = 10;

    Random rnd = new Random();

    for (int i = 1; i <= ITERATIONS; i++) {
      CAS cas = jCas.getCas();
      initRandomCas(cas, 10 * i);

      JCas jcas = cas.getJCas();
      List<Token> tokens = new ArrayList<Token>(select(jcas, Token.class));

      long timeNaive = 0;
      long timeOptimized = 0;
      for (int j = 0; j < ITERATIONS; j++) {
        Token t1 = tokens.get(rnd.nextInt(tokens.size()));
        Token t2 = tokens.get(rnd.nextInt(tokens.size()));

        int left = Math.min(t1.getEnd(), t2.getEnd());
        int right = Math.max(t1.getBegin(), t2.getBegin());

        long ti;
        List<Sentence> reference;
        if ((t1.getBegin() < t2.getBegin() && t2.getBegin() < t1.getEnd())
                || (t1.getBegin() < t2.getEnd() && t2.getEnd() < t1.getEnd())
                || (t2.getBegin() < t1.getBegin() && t1.getBegin() < t2.getEnd())
                || (t2.getBegin() < t1.getEnd() && t1.getEnd() < t2.getEnd())) {
          // If the boundary annotations overlap, the result must be empty
          ti = System.currentTimeMillis();
          reference = new ArrayList<Sentence>();
          timeNaive += System.currentTimeMillis() - ti;
        } else {
          ti = System.currentTimeMillis();
          reference = selectCovered(jcas, Sentence.class, left, right);
          timeNaive += System.currentTimeMillis() - ti;
        }

        ti = System.currentTimeMillis();
        // uimaFIT: selectBetween(Sentence.class, t1, t2);
        List<Sentence> actual = jcas.select(Sentence.class).between(t1, t2).asList();
        timeOptimized += System.currentTimeMillis() - ti;

        assertEquals("Naive: Searching between " + t1 + " and " + t2, reference, actual);
      }

      System.out.format("Speed up factor %.2f [naive:%d optimized:%d diff:%d]\n",
              (double) timeNaive / (double) timeOptimized, timeNaive, timeOptimized, timeNaive
                      - timeOptimized);
    }
  }

  /**
   * Test Tokens (Stems + Lemmas) overlapping with each other.
   */
  @Test
  public void testSelectCoveringOverlapping() {
    add(jCas, 3, 16);
    add(jCas, 37, 61);
    add(jCas, 49, 75);
    add(jCas, 54, 58);
    add(jCas, 66, 84);

    // uimaFIT: selectCovering(jCas, Token.class, 36, 52).size()
    assertEquals(0, jCas.select(Token.class).covering(36, 52).count());
    // uimaFIT: selectCovering(jCas, Token.class, 37, 52).size()
    assertEquals(1, jCas.select(Token.class).covering(37, 52).count());
    // uimaFIT: selectCovering(jCas, Token.class, 49, 52).size()
    assertEquals(2, jCas.select(Token.class).covering(49, 52).count());
  }

  private void initRandomCas(CAS cas, int size) {
    Random rnd = new Random();
    List<Type> types = new ArrayList<Type>();
    types.add(cas.getTypeSystem().getType(Token.class.getName()));
    types.add(cas.getTypeSystem().getType(Sentence.class.getName()));

    // Shuffle the types
    for (int n = 0; n < 10; n++) {
      Type t = types.remove(rnd.nextInt(types.size()));
      types.add(t);
    }

    // Randomly generate annotations
    for (int n = 0; n < size; n++) {
      for (Type t : types) {
        int begin = rnd.nextInt(100);
        int end = begin + rnd.nextInt(30);
        cas.addFsToIndexes(cas.createAnnotation(t, begin, end));
      }
    }
  }

  @SuppressWarnings("unused")
  private void print(Collection<? extends Annotation> annos) {
    for (Annotation a : annos) {
      System.out.println(a.getClass().getSimpleName() + " " + a.getBegin() + " " + a.getEnd());
    }
  }

  private Token add(JCas jcas, int begin, int end) {
    Token t = new Token(jcas, begin, end);
    t.addToIndexes();
    new Sentence(jcas, begin, end).addToIndexes();
    return t;
  }

  private void check(JCas jcas, Annotation t, Collection<? extends Annotation> a1,
          Collection<? extends Annotation> a2) {
    // List<Annotation> annos = new ArrayList<Annotation>();
    // FSIterator fs = jcas.getAnnotationIndex().iterator();
    // while (fs.hasNext()) {
    // annos.add((Annotation) fs.next());
    // }
    //
    // System.out.println("--- Index");
    // print(annos);
    // System.out.println("--- Container");
    // print(Collections.singleton(t));
    // System.out.println("--- Naive");
    // print(a1);
    // System.out.println("--- Optimized");
    // print(a2);
    assertEquals("Container: [" + t.getBegin() + ".." + t.getEnd() + "]", a1, a2);
  }

  @Test
  public void testIterator() throws Exception {
    String text = "Rot wood cheeses dew?";
    tokenBuilder.buildTokens(jCas, text);

    assertEquals(asList("Rot", "wood", "cheeses", "dew?"),
        // uimaFIT: toText(JCasUtil.select(jCas, Token.class))
        jCas.select(Token.class).map(AnnotationFS::getCoveredText).collect(Collectors.toList()));
  }

  @Test
  public void testSelectByIndex() {
    String text = "Rot wood cheeses dew?";
    tokenBuilder.buildTokens(jCas, text);

    // uimaFIT: JCasUtil.selectByIndex(jCas, Token.class, -1).getCoveredText()
    assertEquals("dew?", jCas.select(Token.class).backwards().get(0).getCoveredText());
    // uimaFIT: JCasUtil.selectByIndex(jCas, Token.class, 3).getCoveredText()
    assertEquals("dew?", jCas.select(Token.class).get(3).getCoveredText());
    // uimaFIT: JCasUtil.selectByIndex(jCas, Token.class, 0).getCoveredText()
    assertEquals("Rot", jCas.select(Token.class).get(0).getCoveredText());
    // uimaFIT: JCasUtil.selectByIndex(jCas, Token.class, -4).getCoveredText()
    assertEquals("Rot", jCas.select(Token.class).backwards().get(3).getCoveredText());
    // uimaFIT: assertNull(JCasUtil.selectByIndex(jCas, Token.class, -5));
    assertThatExceptionOfType(CASRuntimeException.class)
        .isThrownBy(() -> jCas.select(Token.class).backwards().get(4))
        .withMessage("CAS does not contain any '" + Token.class.getName() + "' instances  shifted by: 4.");
    // uimaFIT: assertNull(JCasUtil.selectByIndex(jCas, Token.class, 4));
    assertThatExceptionOfType(CASRuntimeException.class)
        .isThrownBy(() -> jCas.select(Token.class).backwards().get(4))
        .withMessage("CAS does not contain any '" + Token.class.getName() + "' instances  shifted by: 4.");
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testSelectOnArrays() throws Exception {
    String text = "Rot wood cheeses dew?";
    tokenBuilder.buildTokens(jCas, text);

    Collection<TOP> allFS = select(jCas, TOP.class);
    FSArray allFSArray = new FSArray(jCas, allFS.size());
    int i = 0;
    for (FeatureStructure fs : allFS) {
      allFSArray.set(i, fs);
      i++;
    }

    // Print what is expected
    for (FeatureStructure fs : allFS) {
      System.out.println("Type: " + fs.getType().getName() + "]");
    }
    System.out.println("Tokens: [" + toText(select(jCas, Token.class)) + "]");

    // Document Annotation, one sentence and 4 tokens.
    assertEquals(6, allFS.size());

    // uimaFIT: toText(select(allFSArray, Token.class))
    assertEquals(toText(select(jCas, Token.class)), toText(allFSArray.select(Token.class)));

    assertEquals(toText((Iterable) jCas.select(Token.class)),
            toText((Iterable) allFSArray.select(Token.class)));
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testSelectOnLists() throws Exception {
    String text = "Rot wood cheeses dew?";
    tokenBuilder.buildTokens(jCas, text);

    Collection<TOP> allFS = select(jCas, TOP.class);

    // Building a list... OMG!
    NonEmptyFSList allFSList = new NonEmptyFSList(jCas);
    NonEmptyFSList head = allFSList;
    Iterator<TOP> i = allFS.iterator();
    while (i.hasNext()) {
      head.setHead(i.next());
      if (i.hasNext()) {
        head.setTail(new NonEmptyFSList(jCas));
        head = (NonEmptyFSList) head.getTail();
      } else {
        head.setTail(new EmptyFSList(jCas));
      }
    }

    // Print what is expected
    for (FeatureStructure fs : allFS) {
      System.out.println("Type: " + fs.getType().getName() + "]");
    }
    System.out.println("Tokens: [" + toText(select(jCas, Token.class)) + "]");

    // Document Annotation, one sentence and 4 tokens.
    assertEquals(6, allFS.size());

    // uimaFIT: toText(select(allFSArray, Token.class))
    assertEquals(toText(select(jCas, Token.class)), toText(allFSList.select(Token.class)));

    assertEquals(toText((Iterable) select(jCas, Token.class)),
            toText((Iterable) select(allFSList, Token.class)));
  }

  @Test
  public void testToText() {
    String text = "Rot wood cheeses dew?";
    tokenBuilder.buildTokens(jCas, text);
    // uimaFIT: toText(select(allFSArray, Token.class))
    assertEquals(asList(text.split(" ")), toText(jCas.select(Token.class)));
  }

  @Test
  public void testSelectSingleRelative() {
    String text = "one two three";
    tokenBuilder.buildTokens(jCas, text);
    List<Token> token = new ArrayList<Token>(select(jCas, Token.class));

    // uimaFIT: selectSingleRelative(jCas, Token.class, token.get(1), -1)
    Token preceding =  jCas.select(Token.class).startAt(token.get(1)).get(-1);
    assertEquals(token.get(0).getCoveredText(), preceding.getCoveredText());

    // selectSingleRelative(jCas, Token.class, token.get(1), 1);
    Token following = jCas.select(Token.class).startAt(token.get(1)).get(1);
    assertEquals(token.get(2).getCoveredText(), following.getCoveredText());
  }

  @Test
  public void testSingleRelativePreceedingDifferentType() {
    String text = "one two three";
    tokenBuilder.buildTokens(jCas, text);

    List<Token> tokens = new ArrayList<Token>(select(jCas, Token.class));
    
    for (Token token : tokens) {
      new AnalyzedText(jCas, token.getBegin(), token.getEnd()).addToIndexes();
    }    
    
    Token lastToken = tokens.get(tokens.size()-1);
    Token preLastToken = tokens.get(tokens.size()-2);
    // uimaFIT selectSingleRelative(jCas, AnalyzedText.class, lastToken, -1);
    AnalyzedText a = jCas.select(AnalyzedText.class).startAt(lastToken).shifted(-1).get();
    assertEquals(preLastToken.getBegin(), a.getBegin());
    assertEquals(preLastToken.getEnd(), a.getEnd());
  }

  @Test
  public void testSingleRelativeFollowingDifferentType() {
    String text = "one two three";
    tokenBuilder.buildTokens(jCas, text);

    List<Token> tokens = new ArrayList<Token>(select(jCas, Token.class));
    
    for (Token token : tokens) {
      new AnalyzedText(jCas, token.getBegin(), token.getEnd()).addToIndexes();
    }    
    
    Token firstToken = tokens.get(0);
    Token secondToken = tokens.get(1);
    // uimaFIT: 
    // AnalyzedText a = selectSingleRelative(jCas, AnalyzedText.class, firstToken, 1);
    AnalyzedText a = jCas.select(AnalyzedText.class).startAt(firstToken).get(1);
    assertEquals(secondToken.getBegin(), a.getBegin());
    assertEquals(secondToken.getEnd(), a.getEnd());
  }
  
  // Actually, in UIMAv3 this does not fail - and it is ok to not fail
  @Deprecated()
  @Test
  public void testSingleRelativeDifferentTypeSamePositionFail() {
    String text = "one two three";
    tokenBuilder.buildTokens(jCas, text);

    List<Token> tokens = new ArrayList<Token>(select(jCas, Token.class));
    
    for (Token token : tokens) {
      new AnalyzedText(jCas, token.getBegin(), token.getEnd()).addToIndexes();
    }    
    
    Token firstToken = tokens.get(0);
    // uimaFIT:
    // assertThatExceptionOfType(IllegalArgumentException.class)
    //    .isThrownBy(() -> selectSingleRelative(jCas, AnalyzedText.class, firstToken, 0));
    
    assertThat(jCas.select(AnalyzedText.class).startAt(firstToken).shifted(0).get())
        .isSameAs(jCas.select(AnalyzedText.class).get());
  }

  @Test
  public void testSingleRelativeSameTypeSamePositionOk() {
    String text = "one two three";
    tokenBuilder.buildTokens(jCas, text);

    List<Token> tokens = new ArrayList<Token>(select(jCas, Token.class));
    
    for (Token token : tokens) {
      new AnalyzedText(jCas, token.getBegin(), token.getEnd()).addToIndexes();
    }    
    
    Token firstToken = tokens.get(0);
    // uimaFIT: selectSingleRelative(jCas, Token.class, firstToken, 0);
    assertEquals(firstToken, jCas.select(Token.class).startAt(firstToken).shifted(0).get());
  }

  @Test
  public void testSelectFollowing() {
    String text = "one two three";
    tokenBuilder.buildTokens(jCas, text);
    List<Token> token = new ArrayList<Token>(select(jCas, Token.class));

    // uimaFIT: selectFollowing(jCas, Token.class, token.get(1), 1).get(0).getCoveredText())
    assertEquals(token.get(2).getCoveredText(), jCas.select(Token.class).following(token.get(1))
            .get().getCoveredText());
  }

  @Test
  public void testSelectPreceding() {
    String text = "one two three";
    tokenBuilder.buildTokens(jCas, text);
    List<Token> token = new ArrayList<Token>(select(jCas, Token.class));

    // uimaFIT: selectPreceding(jCas, Token.class, token.get(1), 1).get(0).getCoveredText());
    assertEquals(token.get(0).getCoveredText(), jCas.select(Token.class).preceding(token.get(1))
            .get().getCoveredText());
  }

  @Test
  public void testSelectPrecedingWithOverlaps() {
    String text = "a b c d e";
    tokenBuilder.buildTokens(jCas, text);
    new Token(jCas, 2, 7).addToIndexes();
    
    Token c = JCasUtil.selectAt(jCas, Token.class, 4, 5).get(0);

    // uimaFIT: selectPreceding(jCas, Token.class, c, 2);
    List<Token> preceedingTokens = jCas.select(Token.class).preceding(c).limit(2).asList();
    
    assertEquals(2, preceedingTokens.size());
    assertEquals("b", preceedingTokens.get(1).getCoveredText());
    assertEquals("a", preceedingTokens.get(0).getCoveredText());
  }

  @Test
  public void testSelectPrecedingWithOverlaps2() {
    jCas.setDocumentText("abcde");
    new Token(jCas, 0, 1).addToIndexes();
    new Token(jCas, 1, 2).addToIndexes();
    new Token(jCas, 2, 3).addToIndexes();
    new Token(jCas, 3, 4).addToIndexes();
    new Token(jCas, 4, 5).addToIndexes();
    new Token(jCas, 1, 3).addToIndexes();
    
    Token c = JCasUtil.selectAt(jCas, Token.class, 2, 3).get(0);

    // selectPreceding(jCas, Token.class, c, 2);
    List<Token> preceedingTokens = jCas.select(Token.class).preceding(c).limit(2).asList();
    
    assertEquals(2, preceedingTokens.size());
    assertEquals("b", preceedingTokens.get(1).getCoveredText());
    assertEquals("a", preceedingTokens.get(0).getCoveredText());
  }

  @Test
  public void testPrecedingDifferentType() {
    String text = "one two three";
    tokenBuilder.buildTokens(jCas, text);

    List<Token> tokens = new ArrayList<Token>(select(jCas, Token.class));
    
    for (Token token : tokens) {
      new AnalyzedText(jCas, token.getBegin(), token.getEnd()).addToIndexes();
    }    
    
    Token lastToken = tokens.get(tokens.size()-1);
    Token preLastToken = tokens.get(tokens.size()-2);
    // selectPreceding(jCas, AnalyzedText.class, lastToken, 1).get(0);
    AnalyzedText a = jCas.select(AnalyzedText.class).preceding(lastToken).limit(1).get();
    assertEquals(preLastToken.getBegin(), a.getBegin());
    assertEquals(preLastToken.getEnd(), a.getEnd());
  }

  @Test
  public void testFollowingDifferentType() {
    String text = "one two three";
    tokenBuilder.buildTokens(jCas, text);

    List<Token> tokens = new ArrayList<Token>(select(jCas, Token.class));
    
    for (Token token : tokens) {
      new AnalyzedText(jCas, token.getBegin(), token.getEnd()).addToIndexes();
    }    
    
    Token firstToken = tokens.get(0);
    Token secondToken = tokens.get(1);
    // selectFollowing(jCas, AnalyzedText.class, firstToken, 1).get(0);
    AnalyzedText a = jCas.select(AnalyzedText.class).following(firstToken).get();
    assertEquals(secondToken.getBegin(), a.getBegin());
    assertEquals(secondToken.getEnd(), a.getEnd());
  }

  @Test
  public void testSelectFollowingPrecedingBuiltinTypes() {
    this.jCas.setDocumentText("A B C");
    // remove the DocumentAnnotation
    for (Annotation ann : JCasUtil.select(jCas, Annotation.class)) {
      ann.removeFromIndexes();
    }
    Annotation a = new Annotation(this.jCas, 0, 1);
    Annotation b = new Annotation(this.jCas, 2, 3);
    Annotation c = new Annotation(this.jCas, 4, 5);
    for (Annotation ann : Arrays.asList(a, b, c)) {
      ann.addToIndexes();
    }

    // uimaFIT: selectPreceding(this.jCas, Annotation.class, b, 2));
    assertEquals(Arrays.asList(a), jCas.select(Annotation.class).preceding(b).limit(2).asList());
    // uimaFIT: Arrays.asList(a, b), selectPreceding(this.jCas, Annotation.class, c, 2));
    // Produces the wrong order
    assertEquals(Arrays.asList(a, b), jCas.select(Annotation.class).preceding(c).limit(2).asList());
    // uimaFIT: Arrays.asList(b, c), selectFollowing(this.jCas, Annotation.class, a, 2));
    assertEquals(Arrays.asList(b, c), jCas.select(Annotation.class).following(a).limit(2).asList());
    // uimaFIT: Arrays.asList(c), selectFollowing(this.jCas, Annotation.class, b, 2));
    assertEquals(Arrays.asList(c), jCas.select(Annotation.class).following(b).limit(2).asList());
  }

  @Test
  public void testSelectFollowingPrecedingDifferentTypes() {
    this.jCas.setDocumentText("A B C D E");
    Token a = new Token(this.jCas, 0, 1);
    Token b = new Token(this.jCas, 2, 3);
    Token c = new Token(this.jCas, 4, 5);
    Token d = new Token(this.jCas, 6, 7);
    Token e = new Token(this.jCas, 8, 9);
    for (Token token : Arrays.asList(a, b, c, d, e)) {
      token.addToIndexes();
    }
    Sentence sentence = new Sentence(this.jCas, 2, 5);
    sentence.addToIndexes();

    // uimaFIT: selectPreceding(this.jCas, Token.class, sentence, 1)
    List<Token> preceding = jCas.select(Token.class).preceding(sentence).limit(1).asList();
    assertEquals(Arrays.asList("A"), JCasUtil.toText(preceding));
    assertEquals(Arrays.asList(a), preceding);
    // uimaFIT: selectPreceding(this.jCas, Token.class, sentence, 2)
    preceding = jCas.select(Token.class).preceding(sentence).limit(2).asList();
    assertEquals(Arrays.asList("A"), JCasUtil.toText(preceding));
    assertEquals(Arrays.asList(a), preceding);

    // uimaFIT: selectFollowing(this.jCas, Token.class, sentence, 1);
    List<Token> following1 = jCas.select(Token.class).following(sentence).limit(1).asList();
    assertEquals(Arrays.asList("D"), JCasUtil.toText(following1));
    assertEquals(Arrays.asList(d), following1);
    
    // uimaFIT: selectFollowing(this.jCas, Token.class, sentence, 2);
    List<Token> following2 = jCas.select(Token.class).following(sentence).limit(2).asList();
    assertEquals(Arrays.asList("D", "E"), JCasUtil.toText(following2));
    assertEquals(Arrays.asList(d, e), following2);
    
    // uimaFIT: selectFollowing(this.jCas, Token.class, sentence, 3);
    List<Token> following3 = jCas.select(Token.class).following(sentence).limit(3).asList();
    assertEquals(Arrays.asList("D", "E"), JCasUtil.toText(following3));
    assertEquals(Arrays.asList(d, e), following3);
  }

  @Test
  public void testSelectFollowingPrecedingDifferentTypesMatchingSpansReversePriorities() {
    this.jCas.setDocumentText("A B C D E");
    Sentence a = new Sentence(this.jCas, 0, 1);
    Sentence b = new Sentence(this.jCas, 2, 3);
    Sentence c = new Sentence(this.jCas, 4, 5);
    Sentence d = new Sentence(this.jCas, 6, 7);
    Sentence e = new Sentence(this.jCas, 8, 9);
    for (Sentence sentence : Arrays.asList(a, b, c, d, e)) {
      sentence.addToIndexes();
    }
    AnalyzedText text = new AnalyzedText(this.jCas, 2, 3);
    text.addToIndexes();

    // uimaFIT: selectPreceding(this.jCas, Sentence.class, text, 1);
    List<Sentence> preceding = jCas.select(Sentence.class).preceding(text).limit(1).asList();
    assertEquals(Arrays.asList("A"), JCasUtil.toText(preceding));
    assertEquals(Arrays.asList(a), preceding);
    // uimaFIT: selectPreceding(this.jCas, Sentence.class, text, 2);
    preceding = jCas.select(Sentence.class).preceding(text).limit(2).asList();
    assertEquals(Arrays.asList("A"), JCasUtil.toText(preceding));
    assertEquals(Arrays.asList(a), preceding);

    // uimaFIT: selectFollowing(this.jCas, Sentence.class, text, 1);
    List<Sentence> following = jCas.select(Sentence.class).following(text).limit(1).asList();
    assertEquals(Arrays.asList("C"), JCasUtil.toText(following));
    assertEquals(Arrays.asList(c), following);
    // uimaFIT: selectFollowing(this.jCas, Sentence.class, text, 2);
    following = jCas.select(Sentence.class).following(text).limit(2).asList();
    assertEquals(Arrays.asList("C", "D"), JCasUtil.toText(following));
    assertEquals(Arrays.asList(c, d), following);
  }

  @Test
  public void testExists() throws UIMAException {
    JCas jcas = CasCreationUtils.createCas(createTypeSystemDescription(), null, null).getJCas();

    // uimaFIT: exists(jcas, Token.class)
    assertFalse(jcas.select(Token.class).findAny().isPresent());

    new Token(jcas, 0, 1).addToIndexes();

    // uimaFIT: exists(jcas, Token.class)
    assertTrue(jcas.select(Token.class).findAny().isPresent());
  }

  @Test
  public void testSelectSingle() throws UIMAException {
    JCas jcas = CasCreationUtils.createCas(createTypeSystemDescription(), null, null).getJCas();

    // uimaFIT:
    // assertThatExceptionOfType(IllegalArgumentException.class)
    //    .isThrownBy(() -> selectSingle(jcas, Token.class)); 
    
    assertThatExceptionOfType(CASRuntimeException.class)
        .isThrownBy(() -> jcas.select(Token.class).single()); 

    new Token(jcas, 0, 1).addToIndexes();

    // uimaFIT: selectSingle(jcas, Token.class);
    jcas.select(Token.class).single();

    new Token(jcas, 1, 2).addToIndexes();

    // uimaFIT:
    // assertThatExceptionOfType(IllegalArgumentException.class)
    //    .isThrownBy(() -> selectSingle(jcas, Token.class))
    //    .as("selectSingle must fail if there is more than one annotation of the type"); 

    assertThatExceptionOfType(CASRuntimeException.class)
      .isThrownBy(() -> jcas.select(Token.class).single())
      .as("selectSingle must fail if there is more than one annotation of the type"); 
  }

  @Test
  public void testSelectIsCovered() {
    String text = "Will you come home today ? \n No , tomorrow !";
    tokenBuilder.buildTokens(jCas, text);

    List<Sentence> sentences = new ArrayList<Sentence>(select(jCas, Sentence.class));
    List<Token> tokens = new ArrayList<Token>(select(jCas, Token.class));

    // uimaFIT: selectCovered(Token.class, sentences.get(0)).size()
    assertEquals(6, jCas.select(Token.class).coveredBy(sentences.get(0)).count());
    // uimaFIT: selectCovered(Token.class, sentences.get(1)).size()
    assertEquals(4, jCas.select(Token.class).coveredBy(sentences.get(1)).count());

    // uimaFIT: contains(jCas, sentences.get(0), Token.class)
    assertTrue(jCas.select(Token.class).coveredBy(sentences.get(0)).findAny().isPresent());
    tokens.get(0).removeFromIndexes();
    tokens.get(1).removeFromIndexes();
    tokens.get(2).removeFromIndexes();
    tokens.get(3).removeFromIndexes();
    tokens.get(4).removeFromIndexes();
    tokens.get(5).removeFromIndexes();
    // uimaFIT: contains(jCas, sentences.get(0), Token.class)
    assertFalse(jCas.select(Token.class).coveredBy(sentences.get(0)).findAny().isPresent());
  }

  @Test
  public void testGetInternalUimaType() {
    // uimaFIT: getType(jCas, Annotation.class);
    Type t = jCas.getCasType(Annotation.class);
    assertNotNull(t);
  }

  @Test
  public void testGetView() throws Exception {
    JCas jcas = CasCreationUtils.createCas(createTypeSystemDescription(), null, null).getJCas();

    assertNull(getView(jcas, "view1", null));
    assertNotNull(getView(jcas, "view1", true));
    assertNotNull(getView(jcas, "view1", null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetNonExistingView() throws Exception {
    JCas jcas = CasCreationUtils.createCas(createTypeSystemDescription(), null, null).getJCas();
    assertNull(getView(jcas, "view1", false));
  }

  @Test
  public void testGetType() {
    String text = "Rot wood cheeses dew?";
    tokenBuilder.buildTokens(jCas, text);

    // uimaFIT: getType(jCas, Token.class).getName());
    assertEquals(Token.class.getName(), jCas.getCasType(Token.class).getName());
    // uimaFIT: getAnnotationType(jCas, Token.class).getName());
    assertEquals(Token.class.getName(), jCas.getCasType(Token.class).getName());
    // uimaFIT: getType(jCas, TOP.class).getName());
    assertEquals("uima.cas.TOP", jCas.getCasType(TOP.class).getName());
    // uimaFIT: getType(jCas, Annotation.class).getName());
    assertEquals("uima.tcas.Annotation", jCas.getCasType(Annotation.class).getName());
    // uimaFIT: getAnnotationType(jCas, Annotation.class).getName());
    assertEquals("uima.tcas.Annotation", jCas.getCasType(Annotation.class).getName());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetNonAnnotationType() {
    String text = "Rot wood cheeses dew?";
    tokenBuilder.buildTokens(jCas, text);

    // There is no alternative in UIMA v3
    getAnnotationType(jCas, TOP.class);
  }

  @Test
  public void testIndexCovering() throws Exception {
    String text = "Will you come home today ? \n No , tomorrow !";
    tokenBuilder.buildTokens(jCas, text);

    List<Sentence> sentences = new ArrayList<Sentence>(select(jCas, Sentence.class));
    List<Token> tokens = new ArrayList<Token>(select(jCas, Token.class));

    Map<Token, List<Sentence>> index = indexCovering(jCas, Token.class, Sentence.class);

    // Check covering annotations are found
    assertEquals(asList(sentences.get(0)), index.get(tokens.get(0)));
    assertEquals(asList(sentences.get(1)), index.get(tokens.get(tokens.size() - 1)));

    // Check sentence 0 contains first token
    assertTrue(index.get(tokens.get(0)).contains(sentences.get(0)));

    // Check sentence 0 does not contain last token.
    assertFalse(index.get(tokens.get(tokens.size() - 1)).contains(sentences.get(0)));

    // Check the first token is contained in any sentence
    assertTrue(!index.get(tokens.get(0)).isEmpty());
    // After removing the annotation the index has to be rebuilt.
    sentences.get(0).removeFromIndexes();
    index = indexCovering(jCas, Token.class, Sentence.class);
    // Check the first token is not contained in any sentence
    assertFalse(!index.get(tokens.get(0)).isEmpty());
  }
  
  @Test
  public void testSelectAt() throws Exception {
    this.jCas.setDocumentText("A B C D E");
    Token a = new Token(this.jCas, 0, 1);
    Token b = new Token(this.jCas, 2, 3);
    Token bc = new Token(this.jCas, 2, 5);
    Token c = new Token(this.jCas, 4, 5);
    Token c1 = new Token(this.jCas, 4, 5);
    Token d = new Token(this.jCas, 4, 7);
    Token cd = new Token(this.jCas, 6, 7);
    Token e = new Token(this.jCas, 8, 9);
    for (Token token : Arrays.asList(a, b, bc, c, c1, d, cd, e)) {
      token.addToIndexes();
    }

    List<Token> tokensAt = selectAt(jCas, Token.class, c.getBegin(), c.getEnd());
    
    assertEquals(2, tokensAt.size());
    assertEquals(c.getBegin(), tokensAt.get(0).getBegin());
    assertEquals(c.getEnd(), tokensAt.get(0).getEnd());
    assertEquals(c.getBegin(), tokensAt.get(1).getBegin());
    assertEquals(c.getEnd(), tokensAt.get(1).getEnd());
  }

  @Test
  public void testSelectSingleAt() throws Exception {
    this.jCas.setDocumentText("A B C D E");
    Token a = new Token(this.jCas, 0, 1);
    Token b = new Token(this.jCas, 2, 3);
    Token bc = new Token(this.jCas, 2, 5);
    Token c = new Token(this.jCas, 4, 5);
    Token c1 = new Token(this.jCas, 4, 5);
    Token d = new Token(this.jCas, 4, 7);
    Token cd = new Token(this.jCas, 6, 7);
    Token e = new Token(this.jCas, 8, 9);
    for (Token token : Arrays.asList(a, b, bc, c, c1, d, cd, e)) {
      token.addToIndexes();
    }

    try {
      selectSingleAt(jCas, Token.class, c.getBegin(), c.getEnd());
      fail("Expected exception not thrown");
    }
    catch (IllegalArgumentException ex) {
      // Ignore.
    }

    try {
      selectSingleAt(jCas, Token.class, 1, 4);
      fail("Expected exception not thrown");
    }
    catch (IllegalArgumentException ex) {
      // Ignore.
    }

    Token tokenAt = selectSingleAt(jCas, Token.class, b.getBegin(), b.getEnd());
  
    assertEquals(b.getBegin(), tokenAt.getBegin());
    assertEquals(b.getEnd(), tokenAt.getEnd());
  }
}
