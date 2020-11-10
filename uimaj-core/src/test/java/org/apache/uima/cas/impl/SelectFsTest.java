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
import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.text.AnnotationPredicates.coveredBy;
import static org.apache.uima.cas.text.AnnotationPredicates.overlappingAtEnd;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import x.y.z.Sentence;
import x.y.z.Token;

// Sorting only to keep the list in Eclipse ordered so it is easier spot if related tests fail
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(Parameterized.class)
public class SelectFsTest {
  @Parameters(name = "{0}")
  public static Collection<Object[]> prios() {
      return asList(new Object[][]{
        { 
          "Annotation first", 
          new String[] { TYPE_NAME_ANNOTATION, Sentence.class.getName(), Token.class.getName() } 
        },
        { 
          "Annotation last", 
          new String[] { Token.class.getName(), Sentence.class.getName(), TYPE_NAME_ANNOTATION } 
        },
      });
  }

  private static TypeSystemDescription typeSystemDescription;
  
  static private CASImpl cas;

  static File typeSystemFile1 = JUnitExtension.getFile("ExampleCas/testTypeSystem_token_sentence_no_features.xml"); 
  
  public SelectFsTest(String aName, String[] aPrioTypeNames) throws Exception {
    typeSystemDescription  = UIMAFramework.getXMLParser().parseTypeSystemDescription(
        new XMLInputSource(typeSystemFile1));
    
    TypePriorities prios = getResourceSpecifierFactory().createTypePriorities();
    TypePriorityList typePrioList = prios.addPriorityList();
    Arrays.stream(aPrioTypeNames).forEachOrdered(typePrioList::addType);
    
    cas = (CASImpl) CasCreationUtils.createCas(typeSystemDescription, prios, null);
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
    Annotation[] a = addToIndexes(
        new Annotation(cas.getJCas(), 0, 1),
        new Annotation(cas.getJCas(), 2, 3),
        new Annotation(cas.getJCas(), 4, 5));

    // uimaFIT: Arrays.asList(a, b), selectPreceding(this.jCas, Annotation.class, c, 2));
    // Produces reverse order
    assertThat(cas.select(Annotation.class).preceding(a[2]).limit(2).asList())
        .containsExactly(a[0], a[1]);
    
    assertThat(cas.select(Annotation.class).startAt(a[2]).shifted(-2).limit(2).asList())
        .containsExactly(a[0], a[1]);
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
    cas.setDocumentText("t1 t2 t3 t4");
    
    addToIndexes(
        new Token(cas.getJCas(), 0, 2),
        new Token(cas.getJCas(), 3, 5),
        new Token(cas.getJCas(), 6, 8),
        new Token(cas.getJCas(), 9, 11));

    // uimaFIT: JCasUtil.selectByIndex(jCas, Token.class, -1).getCoveredText()
    assertThat(cas.select(Token.class).backwards().get(0).getCoveredText())
        .isEqualTo("t4");
  }
  
  @Test
  public void thatIsEmptyWorks() {
    new Token(cas.getJCas(), 0, 2).addToIndexes();
    
    assertThat(cas.select(Token.class).isEmpty())
        .isFalse();
    
    cas.reset();
    
    assertThat(cas.select(Token.class).isEmpty())
        .isTrue();
  }
  
  @Test
  public void testSelectFollowingPrecedingDifferentTypes() {
    
    JCas jCas = cas.getJCas();
    jCas.setDocumentText("A B C D E");
    
    Token[] t = addToIndexes(
        new Token(jCas, 0, 1),
        new Token(jCas, 2, 3),
        new Token(jCas, 4, 5),
        new Token(jCas, 6, 7),
        new Token(jCas, 8, 9));
    
    Sentence sentence = new Sentence(jCas, 2, 5);
    sentence.addToIndexes();

    // uimaFIT: selectFollowing(this.jCas, Token.class, sentence, 1);
    
    assertThat(jCas.select(Token.class).following(sentence).limit(1).asList())
        .containsExactly(t[3]);

    Sentence s2 = new Sentence(jCas, 4, 5);
    
    assertThat(jCas.select(Token.class).preceding(s2).asArray(Token.class))
        .containsExactly(t[0], t[1]);

    assertThat(jCas.select(Token.class).preceding(s2).backwards().asList())
        .containsExactly(t[1], t[0]);
    
    assertThat(jCas.select(Token.class).preceding(s2).backwards().shifted(1).asList())
        .containsExactly(t[0]);

    assertThat(jCas.select(Token.class).following(sentence).shifted(1).asList())
        .containsExactly(t[4]);

    assertThat(jCas.select(Token.class).following(sentence).shifted(-1).asList())
        .containsExactly(t[2], t[3], t[4]);

    assertThat(jCas.select(Token.class).between(t[1], t[4]).asList())
        .containsExactly(t[2], t[3]);

    assertThat(jCas.select(Token.class).between(t[4], t[1]).asList())
        .containsExactly(t[2], t[3]);

    assertThat(jCas.select(Token.class).between(t[1], t[4]).backwards().asList())
        .containsExactly(t[3], t[2]);
  }
  
  @Test
  public void thatCoveredByWithBeyondEndsDoesNotReturnOverlapAtStart() throws Exception
  {
    Annotation y;
    addToIndexes(
        new Annotation(cas.getJCas(), 64, 90),
        y = new Annotation(cas.getJCas(), 68, 86),
        new Annotation(cas.getJCas(), 95, 98));

    assertThat(cas.select(Annotation.class)
        .filter(x -> x != y && (coveredBy(x, y) || overlappingAtEnd(x, y)))
        .collect(toList()))
        .isEmpty();

    assertThat(cas.select(Annotation.class).coveredBy(y)
        .includeAnnotationsWithEndBeyondBounds().asList())
        .isEmpty();    
  }

  @Test
  public void thatCoveredByWithBeyondEndsDoesNotReturnAnnotationStartingAtEnd() throws Exception
  {
    Annotation y;
    Annotation[] a = addToIndexes(
        y = new Annotation(cas.getJCas(), 68, 79),
        new Annotation(cas.getJCas(), 77, 78),
        new Annotation(cas.getJCas(), 79, 103));

    assertThat(cas.select(Annotation.class)
        .filter(x -> x != y && (coveredBy(x, y) || overlappingAtEnd(x, y)))
        .collect(toList()))
        .containsExactly(a[1]);

    assertThat(cas.select(Annotation.class).coveredBy(y)
        .includeAnnotationsWithEndBeyondBounds().asList())
        .containsExactly(a[1]);
  }

  @Test
  public void thatCoveredByWithBeyondEndsDoesNotReturnAnnotationStartingAtEnd2() throws Exception
  {
    Annotation y;
    addToIndexes(
        y = new Annotation(cas.getJCas(), 68, 79),
        new Annotation(cas.getJCas(), 79, 103));

    assertThat(cas.select(Annotation.class)
        .filter(x -> x != y && (coveredBy(x, y) || overlappingAtEnd(x, y)))
        .collect(toList()))
        .isEmpty();

    assertThat(cas.select(Annotation.class).coveredBy(y)
        .includeAnnotationsWithEndBeyondBounds().asList())
        .isEmpty();
  }

  @Test
  public void thatCoveredByWithBeyondEndsFindsAnnotationCoStartingAndExtendingBeyond() throws Exception
  {
    Annotation y;
    Annotation[] a = addToIndexes(
        new Annotation(cas.getJCas(), 44, 68),
        y = new Annotation(cas.getJCas(), 44, 60));

    assertThat(cas.select(Annotation.class)
        .filter(x -> x != y && (coveredBy(x, y) || overlappingAtEnd(x, y)))
        .collect(toList()))
        .containsExactly(a[0]);

    assertThat(cas.select(Annotation.class).coveredBy(y)
        .includeAnnotationsWithEndBeyondBounds().asList())
    .containsExactly(a[0]);
  }

  @Test
  public void thatCoveredByWithBeyondEndsFindsZeroWidthAtEnd() throws Exception
  {
    Annotation y;
    Annotation[] a = addToIndexes(
        y = new Annotation(cas.getJCas(), 8, 33),
        new Annotation(cas.getJCas(), 33, 60),
        new Annotation(cas.getJCas(), 33, 33));

    assertThat(cas.select(Annotation.class)
        .filter(x -> x != y && (coveredBy(x, y) || overlappingAtEnd(x, y)))
        .collect(toList()))
        .containsExactly(a[2]);

    assertThat(cas.select(Annotation.class).coveredBy(y)
        .includeAnnotationsWithEndBeyondBounds()
        .asList())
        .containsExactly(a[2]);
  }

  @Test
  public void thatCoveredByWithBeyondEndsCanSelectAnnotationsStartingAtSelectPosition() throws Exception
  {
    Annotation a = addToIndexes(new Annotation(cas.getJCas(), 0, 2));

    assertThat(cas.select(Annotation.class).coveredBy(0, 1)
        .includeAnnotationsWithEndBeyondBounds().asList())
        .as("Selection (0-1) including start position (0) but not end position (2)")
        .containsExactly(a);
  }
  
  @Test
  public void thatCoveredByWithBeyondEndsCanSelectAnnotationsStartingAtSelectPosition2() {
    Annotation[] a = addToIndexes(
        new Annotation(cas.getJCas(), 0, 4),
        new Annotation(cas.getJCas(), 1, 3));

    assertThat(cas.select(Annotation.class).coveredBy(0, 2)
        .includeAnnotationsWithEndBeyondBounds().asList())
        .containsExactly(a);
  }

  @Test
  public void thatCoveredByWithBeyondEndsCanSelectAnnotationsStartingAtSelectPosition3() {
    Annotation[] a = addToIndexes(
        new Annotation(cas.getJCas(), 0, 5),
        new Annotation(cas.getJCas(), 1, 4),
        new Annotation(cas.getJCas(), 2, 6));

    assertThat(cas.select(Annotation.class).coveredBy(0, 3)
        .includeAnnotationsWithEndBeyondBounds().asList())
        .containsExactly(a);
  }

  @Test
  public void thatCoveredByWithBeyondEndsCanSelectAnnotationsStartingAtSelectPosition4() {
    Annotation[] a = addToIndexes(
        new Annotation(cas.getJCas(), 0, 4),
        new Annotation(cas.getJCas(), 1, 5),
        new Annotation(cas.getJCas(), 2, 2));

    assertThat(cas.select(Annotation.class).coveredBy(0, 3)
        .includeAnnotationsWithEndBeyondBounds().asList())
        .containsExactly(a);
  }
  
  /**
   * @see <a href="https://issues.apache.org/jira/browse/UIMA-6282">UIMA-6282</a>
   */
  @Test
  public void thatSelectAtDoesNotFindFollowingAnnotation()
  {
    Annotation[] a = addToIndexes(
        new Annotation(cas.getJCas(), 10, 20),
        new Annotation(cas.getJCas(), 21, MAX_VALUE));
    
    assertThat(cas.select(Annotation.class).at(a[0]).asList().contains(a[1]))
        .isFalse();
  }
  
  @Test
  public void thatSelectFollowingDoesNotFindOtherZeroWidthAnnotationAtEnd()
  {
    Annotation[] a = addToIndexes(
        new Annotation(cas.getJCas(), 10, 20),
        new Annotation(cas.getJCas(), 20, 20));
    
    assertThat(cas.select(Annotation.class).following(a[0]).asList())
        .isEmpty();
  }

  @Test
  public void thatSelectPrecedingDoesNotFindZeroWidthAnnotationAtStart()
  {
    Annotation[] a = addToIndexes(
        new Annotation(cas.getJCas(), 10, 20),
        new Annotation(cas.getJCas(), 10, 10));
    
    assertThat(cas.select(Annotation.class).preceding(a[0]).asList())
        .isEmpty();
  }

  @Test
  public void thatSelectFollowingDoesNotFindOtherZeroWidthAnnotationAtSameLocation()
  {
    Annotation[] a = addToIndexes(
        new Annotation(cas.getJCas(), 10, 10),
        new Annotation(cas.getJCas(), 10, 10));
    
    assertThat(cas.select(Annotation.class).following(a[0]).asList())
        .isEmpty();
  }

  @Test
  public void thatSelectFollowingDoesNotFindOtherAnnotationAtSameLocation()
  {
    Annotation[] a = addToIndexes(
        new Annotation(cas.getJCas(), 10, 20),
        new Annotation(cas.getJCas(), 10, 20));
    
    assertThat(cas.select(Annotation.class).following(a[0]).asList())
        .isEmpty();
  }
  
  @Test
  public void thatSelectPrecedingDoesNotFindOtherZeroWidthAnnotationAtSameLocation()
  {
    Annotation[] a = addToIndexes(
        new Annotation(cas.getJCas(), 10, 10),
        new Annotation(cas.getJCas(), 10, 10));
    
    assertThat(cas.select(Annotation.class).preceding(a[1]).asList())
        .isEmpty();
  }

  @Test
  public void thatSelectPrecedingDoesNotFindOtherAnnotationAtSameLocation()
  {
    Annotation[] a = addToIndexes(
        new Annotation(cas.getJCas(), 10, 20),
        new Annotation(cas.getJCas(), 10, 20));
    
    assertThat(cas.select(Annotation.class).preceding(a[1]).asList())
        .isEmpty();
  }
  
  @Test
  public void thatSelectCoveredByZeroSizeAtEndOfContextIsIncluded()
  {
    Annotation[] a = addToIndexes(
        new Sentence(cas.getJCas(), 0, 1),
        new Token(cas.getJCas(), 1, 1));
    
    assertThat(cas.select(Token.class).coveredBy(a[0]).asList())
        .containsExactly((Token) a[1]);
  }

  @Test
  public void thatSelectPrecedingDoesNotFindNonZeroWidthAnnotationEndingAtZeroWidthAnnotation()
  {
    Annotation[] a = addToIndexes(
        new Annotation(cas.getJCas(), 20, 20),
        new Annotation(cas.getJCas(), 10, 20));
    
    assertThat(cas.select(Annotation.class).preceding(a[0]).asList())
        .isEmpty();
  }

  @Test
  public void thatSelectFollowingReturnsAdjacentAnnotation()
  {
    Annotation[] a = addToIndexes(
        new Annotation(cas.getJCas(), 10, 20),
        new Annotation(cas.getJCas(), 20, 30));
    
    assertThat(cas.select(Annotation.class).following(a[0]).asList())
        .containsExactly(a[1]);
  }

  @Test
  public void thatSelectFollowingSkipsAdjacentAnnotationAndReturnsNext()
  {
    Annotation[] a = addToIndexes(
        new Annotation(cas.getJCas(), 10, 20),
        new Annotation(cas.getJCas(), 20, 30),
        new Annotation(cas.getJCas(), 30, 40));
    
    assertThat(cas.select(Annotation.class).following(a[0], 1).asList())
        .containsExactly(a[2]);
  }
  
  @Test
  public void thatSelectPrecedingReturnsAdjacentAnnotation()
  {
    Annotation[] a = addToIndexes(
        new Annotation(cas.getJCas(), 10, 20),
        new Annotation(cas.getJCas(), 20, 30));
    
    assertThat(cas.select(Annotation.class).preceding(a[1]).asList())
        .containsExactly(a[0]);
  }

  @Test
  public void thatSelectPrecedingSkipsAdjacentAnnotationAndReturnsNext()
  {
    Annotation[] a = addToIndexes(
        new Annotation(cas.getJCas(), 10, 20),
        new Annotation(cas.getJCas(), 20, 30),
        new Annotation(cas.getJCas(), 30, 40));
    
    assertThat(cas.select(Annotation.class).preceding(a[2], 1).asList())
        .containsExactly(a[0]);
  }
  

  @Test
  public void thatSelectFollowingDoesNotFindZeroWidthAnnotationAtEnd()
  {
    Annotation[] a = addToIndexes(
        new Annotation(cas.getJCas(), 10, 20),
        new Annotation(cas.getJCas(), 20, 20));
    
    assertThat(cas.select(Annotation.class).following(a[1]).asList())
        .isEmpty();
  }
  
  @Test
  public void thatCoveredByFindsTypeUsingSubtype() throws Exception {
    Annotation superType = addToIndexes(new Annotation(cas.getJCas(), 5, 10));
    Token subType = addToIndexes(new Token(cas.getJCas(), 5, 10));

    assertThat(cas.select(Annotation.class).coveredBy(subType).asList())
        .containsExactly(superType);
  }
  
  @Test
  public void thatCoveredByFindsTypeUsingUnindexedSubtype() throws Exception {
    Annotation superType = addToIndexes(new Annotation(cas.getJCas(), 5, 10));
    Token subType = addToIndexes(new Token(cas.getJCas(), 5, 10));

    assertThat(cas.select(Annotation.class).coveredBy(subType).asList())
        .containsExactly(superType);
  }

  @Test
  public void thatCoveredByFindsSubtypeUsingType() throws Exception {
    Annotation superType = addToIndexes(new Annotation(cas.getJCas(), 5, 10));
    Annotation subType = addToIndexes(new Token(cas.getJCas(), 5, 10));

    assertThat(cas.select(Annotation.class).coveredBy(superType).asList())
        .containsExactly(subType);
  }

  @Test
  public void thatCoveredByWorksWithOffsets() throws Exception {
    Annotation a = addToIndexes(new Annotation(cas.getJCas(), 5, 10));

    assertThat(cas.select(Annotation.class).coveredBy(5, 10).asList())
        .containsExactly(a);
  }
  
  @Test
  public void thatCoveredBySkipsIndexedAnchorAnnotation() throws Exception {
    JCas jCas = cas.getJCas();

    Annotation[] a = addToIndexes(
        new Annotation(jCas, 5, 10),
        new Annotation(jCas, 5, 15),
        new Annotation(jCas, 0, 10),
        new Annotation(jCas, 0, 15),
        new Annotation(jCas, 5, 7),
        new Annotation(jCas, 8, 10),
        new Annotation(jCas, 6, 9),
        new Annotation(jCas, 5, 10));

    assertThat(jCas.select(Annotation.class).coveredBy(a[0]).asList())
        .containsExactly(a[7], a[4], a[6], a[5]);

    Annotation subType = addToIndexes(new Token(cas.getJCas(), 5, 10));

    assertThat(cas.select(Annotation.class).coveredBy(subType).asList())
        .containsExactly(a[0], a[7], a[4], a[6], a[5]);
  }

  @Test
  public void thatSelectAtFindsSupertype() throws Exception {
    Annotation[] a = addToIndexes(
        new Annotation(cas.getJCas(), 5, 10),
        new Token(cas.getJCas(), 5, 10));

    assertThat(cas.select(Annotation.class).at(a[1]).asList())
        .containsExactly(a[0]);
  }

  @Test
  public void thatSelectBetweenWorks() throws Exception {
    Annotation[] a = addToIndexes(
        new Sentence(cas.getJCas(), 47, 67),
        new Sentence(cas.getJCas(), 55, 66),
        new Token(cas.getJCas(), 24, 29),
        new Token(cas.getJCas(), 66, 92));

    assertThat(cas.select(Sentence.class).between(a[2], a[3]).asList())
        .containsExactly((Sentence) a[1]);
  }

  @Test
  public void thatSelectColocatedFindsOtherAnnotation() throws Exception {
    Annotation x, y;
    addToIndexes(
        new Annotation(cas.getJCas(), 66, 84),
        y = new Annotation(cas.getJCas(), 66, 70),
        x = new Annotation(cas.getJCas(), 66, 70));

    assertThat(cas.select(Annotation.class).at(y).asList())
        .containsExactly(x);
  }

  @Test
  public void thatSelectColocatedFindsSiblingType() throws Exception {
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");
    tsd.addType("test.Type3", "", "test.Type1");
    
    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);
    
    Type type2 = cas.getTypeSystem().getType("test.Type2");
    Type type3 = cas.getTypeSystem().getType("test.Type3");
    
    AnnotationFS x, y;
    addToIndexes(
        x = cas.createAnnotation(type2, 16, 42),
        y = cas.createAnnotation(type3, 16, 42)
        );

    assertThat(cas.select(type2).at(y).asList())
        .containsExactly((Annotation) x);
  }

  @Test
  public void thatSelectColocatedFindsSiblingType2() throws Exception {
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");
    tsd.addType("test.Type3", "", "test.Type1");
    tsd.addType("test.Type4", "", "test.Type2");
    
    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);
    
//    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");
    Type type3 = cas.getTypeSystem().getType("test.Type3");
    Type type4 = cas.getTypeSystem().getType("test.Type4");
    
    AnnotationFS x, y;
    addToIndexes(
        x = cas.createAnnotation(type2, 16, 42),
        cas.createAnnotation(type4, 16, 41),
        y = cas.createAnnotation(type3, 16, 42)
        );

    assertThat(cas.select(type2).at(y).asList())
        .containsExactly((Annotation) x);
  }
  
  @Test
  public void thatSelectCoveringDoesNotFindItselfWhenSelectingSupertype() throws Exception {
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");
    tsd.addType("test.Type3", "", "test.Type2");
    
    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);
    
    Type type2 = cas.getTypeSystem().getType("test.Type2");
    Type type3 = cas.getTypeSystem().getType("test.Type3");
    
    AnnotationFS y;
    addToIndexes(
        y = cas.createAnnotation(type3, 4, 33));

    assertThat(cas.select(type2).covering(y).asList())
        .isEmpty();
  }
  
  @Test
  public void thatSelectAtInitialPositionIsSameAsFirstPosition() throws Exception {
    Annotation y = new Token(cas.getJCas(), 42, 71);
    addToIndexes(
        new Token(cas.getJCas(), 13, 34),
        new Token(cas.getJCas(), 42, 71));
    
    FSIterator<Token> it = cas.select(Token.class).at(y).fsIterator();
    assertThat(it.isValid()).isTrue();
    Annotation initial = it.isValid() ? it.get() : null;
    it.moveToFirst();
    assertThat(it.isValid() ? it.get() : null).isSameAs(initial);
  }

  @Test
  public void thatSelectFollowingInitialPositionIsSameAsFirstPosition() throws Exception {
    Annotation y = new Token(cas.getJCas(), 13, 34);
    addToIndexes(
        new Token(cas.getJCas(), 13, 34),
        new Token(cas.getJCas(), 42, 71));
    
    FSIterator<Token> it = cas.select(Token.class).following(y).fsIterator();
    assertThat(it.isValid()).isTrue();
    Annotation initial = it.isValid() ? it.get() : null;
    it.moveToFirst();
    assertThat(it.isValid() ? it.get() : null).isSameAs(initial);
  }

  private static <T extends FeatureStructure> T addToIndexes(T fses) {
    asList(fses).forEach(fs -> fs.getCAS().addFsToIndexes(fs));
    return fses;
  }

  @SafeVarargs
  private static <T extends FeatureStructure> T[] addToIndexes(T... fses) {
    asList(fses).forEach(fs -> fs.getCAS().addFsToIndexes(fs));
    return fses;
  }
}
