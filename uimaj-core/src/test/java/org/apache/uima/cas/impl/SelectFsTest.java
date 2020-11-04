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

import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.cas.impl.SelectFsAssert.assertSelectFS;
import static org.apache.uima.cas.impl.SelectFsAssert.assertSelectionIsEqualOnRandomData;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.NON_ZERO_WIDTH_TEST_CASES;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.ZERO_WIDTH_TEST_CASES;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.COLOCATED;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.COVERED_BY;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.COVERING;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.FOLLOWING;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.PRECEDING;
import static org.apache.uima.cas.text.AnnotationPredicates.colocated;
import static org.apache.uima.cas.text.AnnotationPredicates.coveredBy;
import static org.apache.uima.cas.text.AnnotationPredicates.covering;
import static org.apache.uima.cas.text.AnnotationPredicates.following;
import static org.apache.uima.cas.text.AnnotationPredicates.preceding;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationPredicateAssert.TestCase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import x.y.z.Sentence;
import x.y.z.Token;

// Sorting only to keep the list in Eclipse ordered so it is easier spot if related tests fail
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SelectFsTest {

  private static TypeSystemDescription typeSystemDescription;
  
  static private CASImpl cas;

  static File typeSystemFile1 = JUnitExtension.getFile("ExampleCas/testTypeSystem_token_sentence_no_features.xml"); 
  
  private List<TestCase> defaultPredicatesTestCases = union(NON_ZERO_WIDTH_TEST_CASES, ZERO_WIDTH_TEST_CASES);
  
  @BeforeClass
  public static void setUpClass() throws Exception {
    typeSystemDescription  = UIMAFramework.getXMLParser().parseTypeSystemDescription(
        new XMLInputSource(typeSystemFile1));
    cas = (CASImpl) CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(), null);
  }
  
  @Before
  public void setup() {
    cas.reset();
  }
  
  @Test
  public void testSelect_asList() {
    JCas jcas = cas.getJCas();
    
    Token p1 = new Token(jcas, 0, 1); 
    Token p2 = new Token(jcas, 1, 2);
    Token c1 = new Token(jcas, 2, 3);
    Token p3 = new Token(jcas, 1, 3);
    new Token(jcas, 3, 4).addToIndexes();
    new Token(jcas, 4, 5).addToIndexes();
    
    asList(p1, p2, p3, c1).forEach(cas::addFsToIndexes);

    assertThat(jcas.select(Token.class).at(2, 3).get(0))
        .isSameAs(c1);
    
    // preceding -> backwards iteration, starting at annot whose end <= c's begin, therefore starts
    assertThat(jcas.select(Token.class).preceding(c1).asList())
        .containsExactly(p1, p2);
    
    assertThat(jcas.select(Token.class).preceding(c1).limit(2).asList())
        .containsExactly(p1, p2);
  }

  @Test
  public void testPrecedingAndShifted() {
    JCas jCas = cas.getJCas();
    Annotation a = new Annotation(jCas, 0, 1);
    Annotation b = new Annotation(jCas, 2, 3);
    Annotation c = new Annotation(jCas, 4, 5);

    asList(a, b, c).forEach(cas::addFsToIndexes);

    // uimaFIT: Arrays.asList(a, b), selectPreceding(this.jCas, Annotation.class, c, 2));
    // Produces reverse order
    assertThat(jCas.select(Annotation.class).preceding(c).limit(2).asList())
        .containsExactly(a, b);
    
    assertThat(jCas.select(Annotation.class).startAt(c).shifted(-2).limit(2).asList())
        .containsExactly(a, b);
  }
  
  @Test
  public void testBetween() {
    JCas jCas = cas.getJCas();
    
    Token t1 = new Token(jCas, 45, 57);
    Token t2 = new Token(jCas, 52, 52);
    Sentence s1 = new Sentence(jCas, 52, 52);

    asList(t1, t2, s1).forEach(cas::addFsToIndexes);

    // uimaFIT: selectBetween(jCas, Sentence.class, t1, t2);
    assertThat(jCas.select(Sentence.class).between(t1, t2).asList())
        .isEmpty();
    
    t1 = new Token(jCas, 45, 52);
    assertThat(jCas.select(Sentence.class).between(t1, t2).asList())
        .containsExactly(s1);
  }
  
  @Test
  public void testBackwards() {
    JCas jcas = cas.getJCas();
    cas.setDocumentText("t1 t2 t3 t4");
    
    Token p1 = new Token(jcas, 0, 2); 
    Token p2 = new Token(jcas, 3, 5);
    Token p3 = new Token(jcas, 6, 8);
    Token p4 = new Token(jcas, 9, 11);
    
    asList(p1, p2, p3, p4).forEach(cas::addFsToIndexes);

    // uimaFIT: JCasUtil.selectByIndex(jCas, Token.class, -1).getCoveredText()
    assertThat(jcas.select(Token.class).backwards().get(0).getCoveredText())
        .isEqualTo("t4");
  }
  
  @Test
  public void thatIsEmptyWorks() {
    cas.reset();
    JCas jcas = cas.getJCas();
    cas.setDocumentText("t1 t2 t3 t4");
    
    new Token(jcas, 0, 2).addToIndexes();
    
    assertThat(jcas.select(Token.class).isEmpty())
        .isFalse();
    
    cas.reset();
    
    assertThat(jcas.select(Token.class).isEmpty())
        .isTrue();
  }
  
  @Test
  public void testSelectFollowingPrecedingDifferentTypes() {
    
    JCas jCas = cas.getJCas();
    jCas.setDocumentText("A B C D E");
    
    Token a = new Token(jCas, 0, 1);
    Token b = new Token(jCas, 2, 3);
    Token c = new Token(jCas, 4, 5);
    Token d = new Token(jCas, 6, 7);
    Token e = new Token(jCas, 8, 9);
    
    asList(a, b, c, d, e).forEach(cas::addFsToIndexes);    
    
    Sentence sentence = new Sentence(jCas, 2, 5);
    sentence.addToIndexes();

    // uimaFIT: selectFollowing(this.jCas, Token.class, sentence, 1);
    
    List<Token> following1 = jCas.select(Token.class).following(sentence).limit(1).asList();
    // assertEquals(Arrays.asList("D"), JCasUtil.toText(following1));
    assertEquals(Arrays.asList(d), following1);

    Sentence s2 = new Sentence(jCas, 4, 5);
    Token[] prec1 = jCas.select(Token.class).preceding(s2).asArray(Token.class);
    assertEquals(2, prec1.length);
    assertTrue(Arrays.equals(new Token[] { a, b }, prec1));

    List<Token> prec2 = jCas.select(Token.class).preceding(s2).backwards().asList();
    assertEquals(Arrays.asList(b, a), prec2);

    prec2 = jCas.select(Token.class).preceding(s2).backwards().shifted(1).asList();
    assertEquals(Arrays.asList(a), prec2);

    prec2 = jCas.select(Token.class).following(sentence).shifted(1).asList();
    assertEquals(Arrays.asList(e), prec2);

    prec2 = jCas.select(Token.class).following(sentence).shifted(-1).asList();
    assertEquals(Arrays.asList(c, d, e), prec2);

    prec2 = jCas.select(Token.class).between(b, e).asList();
    assertEquals(Arrays.asList(c, d), prec2);

    prec2 = jCas.select(Token.class).between(e, b).asList();
    assertEquals(Arrays.asList(c, d), prec2);

    prec2 = jCas.select(Token.class).between(b, e).backwards().asList();
    assertEquals(Arrays.asList(d, c), prec2);
  }
  
  @Test
  public void thatCoveredByWithBeyondEndsCanSelectAnnotationsStartingAtSelectPosition() throws Exception
  {
    AnnotationFS annotation = cas.createAnnotation(cas.getAnnotationType(), 0, 2);
    cas.addFsToIndexes(annotation);

    List<AnnotationFS> result = cas.select(Annotation.class)
        .coveredBy(0, 1)
        .includeAnnotationsWithEndBeyondBounds()
        .collect(toList());

    assertThat(result)
        .as("Selection (0-1) including start position (0) but not end position (2)")
        .containsExactly(annotation);
  }
  
  @Test
  public void thatCoveredByWithBeyondEndsCanSelectAnnotationsStartingAtSelectPosition2() {
    AnnotationFS a1 = cas.createAnnotation(cas.getAnnotationType(), 0, 4);
    AnnotationFS a2 = cas.createAnnotation(cas.getAnnotationType(), 1, 3);
    cas.addFsToIndexes(a1);
    cas.addFsToIndexes(a2);

    List<AnnotationFS> result = cas.select(Annotation.class)
        .coveredBy(0, 2)
        .includeAnnotationsWithEndBeyondBounds()
        .collect(toList());

    assertThat(result).containsExactly(a1, a2);
  }

  @Test
  public void thatCoveredByWithBeyondEndsCanSelectAnnotationsStartingAtSelectPosition3() {
    AnnotationFS a1 = cas.createAnnotation(cas.getAnnotationType(), 0, 5);
    AnnotationFS a2 = cas.createAnnotation(cas.getAnnotationType(), 1, 4);
    AnnotationFS a3 = cas.createAnnotation(cas.getAnnotationType(), 2, 6);
    cas.addFsToIndexes(a1);
    cas.addFsToIndexes(a2);
    cas.addFsToIndexes(a3);

    List<AnnotationFS> result = cas.select(Annotation.class)
        .coveredBy(0, 3)
        .includeAnnotationsWithEndBeyondBounds()
        .collect(toList());

    assertThat(result).containsExactly(a1, a2, a3);
  }

  @Test
  public void thatCoveredByWithBeyondEndsCanSelectAnnotationsStartingAtSelectPosition4() {
    AnnotationFS a1 = cas.createAnnotation(cas.getAnnotationType(), 0, 4);
    AnnotationFS a2 = cas.createAnnotation(cas.getAnnotationType(), 1, 5);
    AnnotationFS a3 = cas.createAnnotation(cas.getAnnotationType(), 2, 2);
    cas.addFsToIndexes(a1);
    cas.addFsToIndexes(a2);
    cas.addFsToIndexes(a3);

    List<AnnotationFS> result = cas.select(Annotation.class)
        .coveredBy(0, 3)
        .includeAnnotationsWithEndBeyondBounds()
        .collect(toList());

    assertThat(result).containsExactly(a1, a2, a3);
  }
  
  /**
   * @see <a href="https://issues.apache.org/jira/browse/UIMA-6282">UIMA-6282</a>
   */
  @Test
  public void thatSelectAtDoesNotFindFollowingAnnotation()
  {
    AnnotationFS a1 = cas.createAnnotation(cas.getAnnotationType(), 10, 20);
    AnnotationFS a2 = cas.createAnnotation(cas.getAnnotationType(), 21, MAX_VALUE);
    
    asList(a1, a2).forEach(cas::addFsToIndexes);
    
    assertThat(cas.<Annotation>select(cas.getAnnotationType()).at(a1).asList().contains(a2)).isFalse();
  }
  
  @Test
  public void thatSelectFollowingDoesNotFindOtherZeroWidthAnnotationAtEnd()
  {
    Annotation a1 = cas.createAnnotation(cas.getAnnotationType(), 10, 20);
    Annotation a2 = cas.createAnnotation(cas.getAnnotationType(), 20, 20);
    
    asList(a1, a2).forEach(cas::addFsToIndexes);
    
    List<Annotation> selection = cas.select(Annotation.class)
        .following(a1)
        .asList();
    
    assertThat(selection)
        .isEmpty();
  }

  @Test
  public void thatSelectPrecedingDoesNotFindZeroWidthAnnotationAtStart()
  {
    Annotation a1 = cas.createAnnotation(cas.getAnnotationType(), 10, 20);
    Annotation a2 = cas.createAnnotation(cas.getAnnotationType(), 10, 10);
    
    asList(a1, a2).forEach(cas::addFsToIndexes);
    
    List<Annotation> selection = cas.select(Annotation.class)
        .preceding(a1)
        .asList();
    
    assertThat(selection)
        .isEmpty();
  }

  @Test
  public void thatSelectFollowingDoesNotFindOtherZeroWidthAnnotationAtSameLocation()
  {
    Annotation a1 = cas.createAnnotation(cas.getAnnotationType(), 10, 10);
    Annotation a2 = cas.createAnnotation(cas.getAnnotationType(), 10, 10);
    
    asList(a1, a2).forEach(cas::addFsToIndexes);
    
    List<Annotation> selection = cas.select(Annotation.class)
        .following(a1)
        .asList();
    
    assertThat(selection)
        .isEmpty();
  }

  @Test
  public void thatSelectFollowingDoesNotFindOtherAnnotationAtSameLocation()
  {
    Annotation a1 = cas.createAnnotation(cas.getAnnotationType(), 10, 20);
    Annotation a2 = cas.createAnnotation(cas.getAnnotationType(), 10, 20);
    
    asList(a1, a2).forEach(cas::addFsToIndexes);
    
    List<Annotation> selection = cas.select(Annotation.class)
        .following(a1)
        .asList();
    
    assertThat(selection)
        .isEmpty();
  }
  
  @Test
  public void thatSelectPrecedingDoesNotFindOtherZeroWidthAnnotationAtSameLocation()
  {
    Annotation a1 = cas.createAnnotation(cas.getAnnotationType(), 10, 10);
    Annotation a2 = cas.createAnnotation(cas.getAnnotationType(), 10, 10);
    
    asList(a1, a2).forEach(cas::addFsToIndexes);
    
    List<Annotation> selection = cas.select(Annotation.class)
        .preceding(a2)
        .asList();
    
    assertThat(selection)
        .isEmpty();
  }

  @Test
  public void thatSelectPrecedingDoesNotFindOtherAnnotationAtSameLocation()
  {
    Annotation a1 = cas.createAnnotation(cas.getAnnotationType(), 10, 20);
    Annotation a2 = cas.createAnnotation(cas.getAnnotationType(), 10, 20);
    
    asList(a1, a2).forEach(cas::addFsToIndexes);
    
    List<Annotation> selection = cas.select(Annotation.class)
        .preceding(a2)
        .asList();
    
    assertThat(selection)
        .isEmpty();
  }
  
  @Test
  public void thatSelectCoveredByZeroSizeAtEndOfContextIsIncluded()
  {
    Annotation a1 = cas.createAnnotation(cas.getCasType(Sentence.class), 0, 1);
    Annotation a2 = cas.createAnnotation(cas.getCasType(Token.class), 1, 1);
    
    asList(a1, a2).forEach(cas::addFsToIndexes);
    
    List<Annotation> selection = cas.<Annotation>select(cas.getCasType(Token.class))
        .coveredBy(a1)
        .asList();
    
    assertThat(selection)
        .containsExactly(a2);
  }

  @Test
  public void thatSelectPrecedingDoesNotFindNonZeroWidthAnnotationEndingAtZeroWidthAnnotation()
  {
    Annotation a1 = cas.createAnnotation(cas.getAnnotationType(), 20, 20);
    Annotation a2 = cas.createAnnotation(cas.getAnnotationType(), 10, 20);
    
    asList(a1, a2).forEach(cas::addFsToIndexes);
    
    List<Annotation> selection = cas.select(Annotation.class)
        .preceding(a1)
        .asList();
    
    assertThat(selection)
        .isEmpty();
  }

  @Test
  public void thatSelectFollowingReturnsAdjacentAnnotation()
  {
    Annotation a1 = cas.createAnnotation(cas.getAnnotationType(), 10, 20);
    Annotation a2 = cas.createAnnotation(cas.getAnnotationType(), 20, 30);
    
    asList(a1, a2).forEach(cas::addFsToIndexes);
    
    List<Annotation> selection = cas.select(Annotation.class)
        .following(a1)
        .asList();
    
    assertThat(selection)
        .containsExactly(a2);
  }

  @Test
  public void thatSelectFollowingSkipsAdjacentAnnotationAndReturnsNext()
  {
    Annotation a1 = cas.createAnnotation(cas.getAnnotationType(), 10, 20);
    Annotation a2 = cas.createAnnotation(cas.getAnnotationType(), 20, 30);
    Annotation a3 = cas.createAnnotation(cas.getAnnotationType(), 30, 40);
    
    asList(a1, a2, a3).forEach(cas::addFsToIndexes);
    
    List<Annotation> selection = cas.select(Annotation.class)
        .following(a1, 1)
        .asList();
    
    assertThat(selection)
        .containsExactly(a3);
  }
  
  @Test
  public void thatSelectPrecedingReturnsAdjacentAnnotation()
  {
    Annotation a1 = cas.createAnnotation(cas.getAnnotationType(), 10, 20);
    Annotation a2 = cas.createAnnotation(cas.getAnnotationType(), 20, 30);
    
    asList(a1, a2).forEach(cas::addFsToIndexes);
    
    List<Annotation> selection = cas.select(Annotation.class)
        .preceding(a2)
        .asList();
    
    assertThat(selection)
        .containsExactly(a1);
  }

  @Test
  public void thatSelectPrecedingSkipsAdjacentAnnotationAndReturnsNext()
  {
    Annotation a1 = cas.createAnnotation(cas.getAnnotationType(), 10, 20);
    Annotation a2 = cas.createAnnotation(cas.getAnnotationType(), 20, 30);
    Annotation a3 = cas.createAnnotation(cas.getAnnotationType(), 30, 40);
    
    asList(a1, a2, a3).forEach(cas::addFsToIndexes);
    
    List<Annotation> selection = cas.select(Annotation.class)
        .preceding(a3, 1)
        .asList();
    
    assertThat(selection)
        .containsExactly(a1);
  }
  

  @Test
  public void thatSelectFollowingDoesNotFindZeroWidthAnnotationAtEnd()
  {
    Annotation a1 = cas.createAnnotation(cas.getAnnotationType(), 10, 20);
    Annotation a2 = cas.createAnnotation(cas.getAnnotationType(), 20, 20);
    
    asList(a1, a2).forEach(cas::addFsToIndexes);
    
    List<Annotation> selection = cas.select(Annotation.class)
        .following(a1)
        .asList();
    
    assertThat(selection)
        .isEmpty();
  }

  @Test
  public void thatSelectFsBehaviorAlignsWithPrecedingPredicate() throws Exception {
    // In order to find annotations that X is preceding, we select the following annotations
    assertSelectFS(
        PRECEDING,
        (cas, type, x, y) -> cas.select(type).following(x).asList().contains(y),
        defaultPredicatesTestCases);

    assertSelectFS(
        PRECEDING,
        (cas, type, x, y) -> cas.select(type).filter((a) -> 
                preceding(x, (Annotation) a)).collect(toList()).contains(y),
        defaultPredicatesTestCases);
  }
  
  @Test
  public void thatCasSelectFsBehaviorAlignsWithPrecedingPredicateOnRandomData() throws Exception
  {
    System.out.print("Preceding (CAS select)   -- ");
    assertSelectionIsEqualOnRandomData(30, 10,
        (cas, type, context) -> cas.getAnnotationIndex(type).select()
            .filter(candidate -> preceding(candidate, context))
            .collect(toList()),
        (cas, type, context) -> cas.<Annotation>select(type)
            .preceding(context)
            .map(a -> (AnnotationFS) a)
            .collect(toList()));
  }

  @Test
  public void thatIndexSelectFsBehaviorAlignsWithPrecedingPredicateOnRandomData() throws Exception
  {
    System.out.print("Preceding (Index select) -- ");
    assertSelectionIsEqualOnRandomData(30, 10,
        (cas, type, context) -> cas.getAnnotationIndex(type).select()
            .filter(candidate -> preceding(candidate, context))
            .collect(toList()),
        (cas, type, context) -> cas.getAnnotationIndex(type).select()
            .preceding(context)
            .map(a -> (AnnotationFS) a)
            .collect(toList()));
  }

  @Test
  public void thatSelectFsBehaviorAlignsWithFollowingPredicate() throws Exception {
    // In order to find annotations that X is following, we select the preceding annotations
    assertSelectFS(
        FOLLOWING,
        (cas, type, x, y) -> cas.select(type).preceding(x).asList().contains(y),
        defaultPredicatesTestCases);

    assertSelectFS(
        FOLLOWING,
        (cas, type, x, y) -> cas.select(type).filter((a) -> 
                following(x, (Annotation) a)).collect(toList()).contains(y),
        defaultPredicatesTestCases);
  }
  
  @Test
  public void thatCasSelectFsBehaviorAlignsWithFollowingPredicateOnRandomData() throws Exception
  {
    System.out.print("Following (CAS select)   -- ");
    assertSelectionIsEqualOnRandomData(30, 10,
        (cas, type, context) -> cas.getAnnotationIndex(type).select()
            .filter(candidate -> following(candidate, context))
            .collect(toList()),
        (cas, type, context) -> cas.<Annotation>select(type)
            .following(context)
            .map(a -> (AnnotationFS) a)
            .collect(toList()));
  }

  @Test
  public void thatIndexSelectFsBehaviorAlignsWithFollowingPredicateOnRandomData() throws Exception
  {
    System.out.print("Following (Index select) -- ");
    assertSelectionIsEqualOnRandomData(30, 10,
        (cas, type, context) -> cas.getAnnotationIndex(type).select()
            .filter(candidate -> following(candidate, context))
            .collect(toList()),
        (cas, type, context) -> cas.getAnnotationIndex(type).select()
            .following(context)
            .map(a -> (AnnotationFS) a)
            .collect(toList()));
  }

  @Test
  public void thatSelectFsBehaviorAlignsWithCoveredByPredicate() throws Exception {
    // X covered by Y means that Y is covering X, so we need to select the covering annotations
    // below.
    assertSelectFS(
        COVERED_BY,
        (cas, type, x, y) -> cas.select(type).covering(x).asList().contains(y),
        defaultPredicatesTestCases);
    
    assertSelectFS(
        COVERED_BY,
        (cas, type, x, y) -> cas.select(type).filter((a) -> 
                coveredBy(x, (Annotation) a)).collect(toList()).contains(y),
        defaultPredicatesTestCases);
  }
  
  @Test
  public void thatCasSelectFsBehaviorAlignsWithCoveredByPredicateOnRandomData() throws Exception
  {
    System.out.print("CoveredBy (CAS select)   -- ");
    assertSelectionIsEqualOnRandomData(30, 10,
        (cas, type, context) -> cas.getAnnotationIndex(type).select()
            .filter(candidate -> coveredBy(candidate, context))
            .collect(toList()),
        (cas, type, context) -> cas.<Annotation>select(type)
            .coveredBy(context)
            .map(a -> (AnnotationFS) a)
            .collect(toList()));
  }

  @Test
  public void thatIndexSelectFsBehaviorAlignsWithCoveredByPredicateOnRandomData() throws Exception
  {
    System.out.print("CoveredBy (Index select) -- ");
    assertSelectionIsEqualOnRandomData(30, 10,
        (cas, type, context) -> cas.getAnnotationIndex(type).select()
            .filter(candidate -> coveredBy(candidate, context))
            .collect(toList()),
        (cas, type, context) -> cas.getAnnotationIndex(type).select()
            .coveredBy(context)
            .map(a -> (AnnotationFS) a)
            .collect(toList()));
  }
  
  @Test
  public void thatSelectFsBehaviorAlignsWithCoveringPredicate() throws Exception {
    // X covering Y means that Y is covered by Y, so we need to select the covered by annotations
    // below.
    assertSelectFS(
        COVERING,
        (cas, type, x, y) -> cas.select(type).coveredBy(x).asList().contains(y),
        defaultPredicatesTestCases);

    assertSelectFS(
        COVERING,
        (cas, type, x, y) -> cas.select(type).filter((a) -> 
                covering(x, (Annotation) a)).collect(toList()).contains(y),
        defaultPredicatesTestCases);
  }

  @Test
  public void thatCasSelectFsBehaviorAlignsWithCoveringPredicateOnRandomData() throws Exception
  {
    System.out.print("Covering  (CAS select)   -- ");
    assertSelectionIsEqualOnRandomData(30, 10,
        (cas, type, context) -> cas.getAnnotationIndex(type).select()
            .filter(candidate -> covering(candidate, context))
            .collect(toList()),
        (cas, type, context) -> cas.<Annotation>select(type)
            .covering(context)
            .map(a -> (AnnotationFS) a)
            .collect(toList()));
  }
  
  @Test
  public void thatIndexSelectFsBehaviorAlignsWithCoveringPredicateOnRandomData() throws Exception
  {
    System.out.print("Covering  (Index select) -- ");
    assertSelectionIsEqualOnRandomData(30, 10,
        (cas, type, context) -> cas.getAnnotationIndex(type).select()
            .filter(candidate -> covering(candidate, context))
            .collect(toList()),
        (cas, type, context) -> cas.getAnnotationIndex(type).select()
            .covering(context)
            .map(a -> (AnnotationFS) a)
            .collect(toList()));
  }
  
  @Test
  public void thatSelectFsBehaviorAlignsWithColocatedPredicate() throws Exception {
    // X covering Y means that Y is covered by Y, so we need to select the covered by annotations
    // below.
    assertSelectFS(
        COLOCATED,
        (cas, type, x, y) -> cas.select(type).at(x).asList().contains(y),
        defaultPredicatesTestCases);
    
    assertSelectFS(
        COLOCATED,
        (cas, type, x, y) -> cas.select(type).filter((a) -> 
                colocated(x, (Annotation) a)).collect(toList()).contains(y),
        defaultPredicatesTestCases);
  }  

  @Test
  public void thatCasSelectFsBehaviorAlignsWithColocatedPredicateOnRandomData() throws Exception
  {
    System.out.print("Colocated (CAS select)   -- ");
    assertSelectionIsEqualOnRandomData(30, 10,
        (cas, type, context) -> cas.getAnnotationIndex(type).select()
            .filter(candidate -> colocated(candidate, context))
            .collect(toList()),
        (cas, type, context) -> cas.<Annotation>select(type)
            .at(context)
            .map(a -> (AnnotationFS) a)
            .collect(toList()));
  }

  @Test
  public void thatIndexSelectFsBehaviorAlignsWithColocatedPredicateOnRandomData() throws Exception
  {
    System.out.print("Colocated (Index select) -- ");
    assertSelectionIsEqualOnRandomData(30, 10,
        (cas, type, context) -> cas.getAnnotationIndex(type).select()
            .filter(candidate -> colocated(candidate, context))
            .collect(toList()),
        (cas, type, context) -> cas.getAnnotationIndex(type).select()
            .at(context)
            .map(a -> (AnnotationFS) a)
            .collect(toList()));
  }

  @SafeVarargs
  public static <T> List<T> union(List<T>... aLists) {
      List<T> all = new ArrayList<>();
      for (List<T> list : aLists) {
        all.addAll(list);
      }
      return all;
  }
}
