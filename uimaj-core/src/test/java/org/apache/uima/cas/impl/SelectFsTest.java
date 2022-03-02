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
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.text.AnnotationPredicates.coveredBy;
import static org.apache.uima.cas.text.AnnotationPredicates.overlappingAtEnd;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SelectFSs;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runners.MethodSorters;

import x.y.z.Sentence;
import x.y.z.Token;

// Sorting only to keep the list in Eclipse ordered so it is easier spot if related tests fail
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SelectFsTest {
  private enum Mode {
    ANNOTATION_FIRST, ANNOTATION_LAST;
  }

  public static Stream<Arguments> prios() {
    return Stream.of(
            Arguments.of(Mode.ANNOTATION_FIRST,
                    new String[] { TYPE_NAME_ANNOTATION, Sentence.class.getName(),
                        Token.class.getName() }),
            Arguments.of(Mode.ANNOTATION_LAST, new String[] { Token.class.getName(),
                Sentence.class.getName(), TYPE_NAME_ANNOTATION }));
  }

  private Mode mode;
  private TypeSystemDescription typeSystemDescription;
  private CASImpl cas;

  static File typeSystemFile1 = JUnitExtension
          .getFile("ExampleCas/testTypeSystem_token_sentence_no_features.xml");

  public void setup(Mode aMode, String... aPrioTypeNames) throws Exception {
    mode = aMode;
    typeSystemDescription = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(typeSystemFile1));

    TypePriorities prios = getResourceSpecifierFactory().createTypePriorities();
    TypePriorityList typePrioList = prios.addPriorityList();
    Arrays.stream(aPrioTypeNames).forEachOrdered(typePrioList::addType);

    cas = (CASImpl) CasCreationUtils.createCas(typeSystemDescription, prios, null);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void testSelect_asList(Mode aMode, String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);

    JCas jcas = cas.getJCas();

    Token p1 = new Token(jcas, 0, 1);
    Token p2 = new Token(jcas, 1, 2);
    Token c1 = new Token(jcas, 2, 3);
    Token p3 = new Token(jcas, 1, 3);
    new Token(jcas, 3, 4).addToIndexes();
    new Token(jcas, 4, 5).addToIndexes();

    asList(p1, p2, p3, c1).forEach(cas::addFsToIndexes);

    assertThat(jcas.select(Token.class).at(2, 3).get(0)).isSameAs(c1);

    // preceding -> backwards iteration, starting at annot whose end <= c's begin, therefore starts
    assertThat(jcas.select(Token.class).preceding(c1).asList()).containsExactly(p1, p2);

    assertThat(jcas.select(Token.class).preceding(c1).limit(2).asList()).containsExactly(p1, p2);

    assertThat(jcas.select(Token.class).preceding(c1).limit(0).asList()).isEmpty();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void testPrecedingAndShifted(Mode aMode, String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 0, 1),
            new Annotation(cas.getJCas(), 2, 3), new Annotation(cas.getJCas(), 4, 5));

    // uimaFIT: Arrays.asList(a, b), selectPreceding(this.jCas, Annotation.class, c, 2));
    // Produces reverse order
    assertThat(cas.select(Annotation.class).preceding(a[2]).limit(2).asList()).containsExactly(a[0],
            a[1]);

    assertThat(cas.select(Annotation.class).preceding(a[2]).limit(0).asList()).isEmpty();

    assertThat(cas.select(Annotation.class).startAt(a[2]).shifted(-2).limit(2).asList())
            .containsExactly(a[0], a[1]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void testBetween(Mode aMode, String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    JCas jCas = cas.getJCas();

    Token t1 = new Token(jCas, 45, 57);
    Token t2 = new Token(jCas, 52, 52);
    Sentence s1 = new Sentence(jCas, 52, 52);

    asList(t1, t2, s1).forEach(cas::addFsToIndexes);

    // uimaFIT: selectBetween(jCas, Sentence.class, t1, t2);
    assertThat(jCas.select(Sentence.class).between(t1, t2).asList()).isEmpty();

    t1 = new Token(jCas, 45, 52);
    assertThat(jCas.select(Sentence.class).between(t1, t2).asList()).containsExactly(s1);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void testBackwards(Mode aMode, String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    cas.setDocumentText("t1 t2 t3 t4");

    addToIndexes(new Token(cas.getJCas(), 0, 2), new Token(cas.getJCas(), 3, 5),
            new Token(cas.getJCas(), 6, 8), new Token(cas.getJCas(), 9, 11));

    // uimaFIT: JCasUtil.selectByIndex(jCas, Token.class, -1).getCoveredText()
    assertThat(cas.select(Token.class).backwards().get(0).getCoveredText()).isEqualTo("t4");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatIsEmptyWorks(Mode aMode, String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    new Token(cas.getJCas(), 0, 2).addToIndexes();

    assertThat(cas.select(Token.class).isEmpty()).isFalse();

    cas.reset();

    assertThat(cas.select(Token.class).isEmpty()).isTrue();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void testSelectFollowingPrecedingDifferentTypes(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);

    JCas jCas = cas.getJCas();
    jCas.setDocumentText("A B C D E");

    Token[] t = addToIndexes(new Token(jCas, 0, 1), new Token(jCas, 2, 3), new Token(jCas, 4, 5),
            new Token(jCas, 6, 7), new Token(jCas, 8, 9));

    Sentence sentence = new Sentence(jCas, 2, 5);
    sentence.addToIndexes();

    // uimaFIT: selectFollowing(this.jCas, Token.class, sentence, 1);

    assertThat(jCas.select(Token.class).following(sentence).limit(1).asList())
            .containsExactly(t[3]);

    Sentence s2 = new Sentence(jCas, 4, 5);

    // Selects the preceding tokens in document order
    assertThat(jCas.select(Token.class).preceding(s2).asArray(Token.class)).containsExactly(t[0],
            t[1]);

    // Selects the preceding tokens in document order
    // NOTE: Because the selection order is actually reverse (from the starting annotation towards
    // the beginning of the document), the shift operation shifts towards the beginning of the
    // document, so the result is t[0] and not t[1]. t[1] could be expected if the user believes
    // that the shift operator moves to the right in the result list which would be (t[0], t[1])
    // if there had been no shift.
    // REC: I find this behavior of shift quite confusing....
    assertThat(jCas.select(Token.class).preceding(s2).shifted(1).asList()).containsExactly(t[0]);

    // Selects the preceding tokens in reverse document order
    assertThat(jCas.select(Token.class).preceding(s2).backwards().asList()).containsExactly(t[1],
            t[0]);

    assertThat(jCas.select(Token.class).preceding(s2).backwards().shifted(1).asList())
            .containsExactly(t[0]);

    assertThat(jCas.select(Token.class).following(sentence).shifted(1).asList())
            .containsExactly(t[4]);

    // What according to the old test check that I commented out should happen is that instead of
    // returning all annotations following the startFS is that the first annotation of the
    // selection type that occurs *before* the startFS is also included in the result.
    // This might look useful e.g. to select left/right windows of the startFS, but it is a
    // nightmare implementation-wise because the selection boundaries are no longer determined by
    // the bounds of the startFS. It starts becoming sensitive to the index order.
    //
    // For an index-order-sensitive window-like selection, using startAt with shift and limit is
    // the appropriate approach:
    //
    // jCas.select(Token.class).startAt(s2).shifted(-1).limit(3)
    //
    // What does happen right now is that the negative shift makes the underlying iterator invalid
    // because it causes it to move outside its boundary (i.e. into the startFS).
    assertThat(jCas.select(Token.class).following(s2).shifted(-1).asList()).containsExactly(t[3],
            t[4]);
    // Old reference value
    // .containsExactly(t[2], t[3], t[4]);

    // See comment above for select-following-with-negative-shift
    assertThat(jCas.select(Token.class).preceding(s2).backwards().shifted(-1).asList())
            .containsExactly(t[1], t[0]);
    // .containsExactly(t[0], t[1], t[2]);

    assertThat(jCas.select(Token.class).startAt(s2).asList()).containsExactly(t[2], t[3], t[4]);

    switch (mode) {
      case ANNOTATION_FIRST:
        assertThat(jCas.select(Token.class).typePriority().startAt(s2).backwards().asList())
                .containsExactly(t[1], t[0]);
        break;
      case ANNOTATION_LAST:
        assertThat(jCas.select(Token.class).typePriority().startAt(s2).backwards().asList())
                .containsExactly(t[2], t[1], t[0]);
        break;
    }

    assertThat(jCas.select(Token.class).startAt(s2).backwards().asList()).containsExactly(t[2],
            t[1], t[0]);

    assertThat(jCas.select(Token.class).startAt(s2).shifted(-1).limit(3).asList())
            .containsExactly(t[1], t[2], t[3]);

    switch (mode) {
      case ANNOTATION_FIRST:
        assertThat(jCas.select(Token.class).typePriority().startAt(s2).shifted(-1).limit(3)
                .backwards().asList()).containsExactly(t[2], t[1], t[0]);
        break;
      case ANNOTATION_LAST:
        assertThat(jCas.select(Token.class).typePriority().startAt(s2).shifted(-1).limit(3)
                .backwards().asList()).containsExactly(t[3], t[2], t[1]);
        break;
    }

    assertThat(jCas.select(Token.class).startAt(s2).shifted(-1).limit(3).backwards().asList())
            .containsExactly(t[3], t[2], t[1]);

    assertThat(jCas.select(Token.class).between(t[1], t[4]).asList()).containsExactly(t[2], t[3]);

    assertThat(jCas.select(Token.class).between(t[4], t[1]).asList()).containsExactly(t[2], t[3]);

    assertThat(jCas.select(Token.class).between(t[1], t[4]).backwards().asList())
            .containsExactly(t[3], t[2]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatCoveredByWithNonOverlappingWorks(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);

    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", "uima.tcas.Annotation");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");

    AnnotationFS y = cas.createAnnotation(type1, 30, 57);
    AnnotationFS expected;
    addToIndexes(cas.createAnnotation(type1, 41, 41),
            expected = cas.createAnnotation(type1, 39, 41));

    FSIterator<Annotation> it = cas.<Annotation> select(type1).coveredBy(y).nonOverlapping()
            .fsIterator();
    it.moveToLast();
    assertThat(it.get()).isSameAs(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatCoveredByWithBeyondEndsDoesNotReturnOverlapAtStart(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);

    Annotation y;
    addToIndexes(new Annotation(cas.getJCas(), 64, 90), y = new Annotation(cas.getJCas(), 68, 86),
            new Annotation(cas.getJCas(), 95, 98));

    assertThat(cas.select(Annotation.class)
            .filter(x -> x != y && (coveredBy(x, y) || overlappingAtEnd(x, y))).collect(toList()))
                    .isEmpty();

    assertThat(cas.select(Annotation.class).coveredBy(y).includeAnnotationsWithEndBeyondBounds()
            .asList()).isEmpty();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatCoveredByWithBeyondEndsDoesNotReturnAnnotationStartingAtEnd(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);

    Annotation y;
    Annotation[] a = addToIndexes(y = new Annotation(cas.getJCas(), 68, 79),
            new Annotation(cas.getJCas(), 77, 78), new Annotation(cas.getJCas(), 79, 103));

    assertThat(cas.select(Annotation.class)
            .filter(x -> x != y && (coveredBy(x, y) || overlappingAtEnd(x, y))).collect(toList()))
                    .containsExactly(a[1]);

    assertThat(cas.select(Annotation.class).coveredBy(y).includeAnnotationsWithEndBeyondBounds()
            .asList()).containsExactly(a[1]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatCoveredByWithBeyondEndsDoesNotReturnAnnotationStartingAtEnd2(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);

    Annotation y;
    addToIndexes(y = new Annotation(cas.getJCas(), 68, 79), new Annotation(cas.getJCas(), 79, 103));

    assertThat(cas.select(Annotation.class)
            .filter(x -> x != y && (coveredBy(x, y) || overlappingAtEnd(x, y))).collect(toList()))
                    .isEmpty();

    assertThat(cas.select(Annotation.class).coveredBy(y).includeAnnotationsWithEndBeyondBounds()
            .asList()).isEmpty();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatCoveredByWithBeyondEndsFindsAnnotationCoStartingAndExtendingBeyond(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);

    Annotation y;
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 44, 68),
            y = new Annotation(cas.getJCas(), 44, 60));

    assertThat(cas.select(Annotation.class)
            .filter(x -> x != y && (coveredBy(x, y) || overlappingAtEnd(x, y))).collect(toList()))
                    .containsExactly(a[0]);

    assertThat(cas.select(Annotation.class).coveredBy(y).includeAnnotationsWithEndBeyondBounds()
            .asList()).containsExactly(a[0]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatCoveredByWithBeyondEndsFindsZeroWidthAtEnd(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);

    Annotation y;
    Annotation[] a = addToIndexes(y = new Annotation(cas.getJCas(), 8, 33),
            new Annotation(cas.getJCas(), 33, 60), new Annotation(cas.getJCas(), 33, 33));

    assertThat(cas.select(Annotation.class)
            .filter(x -> x != y && (coveredBy(x, y) || overlappingAtEnd(x, y))).collect(toList()))
                    .containsExactly(a[2]);

    assertThat(cas.select(Annotation.class).coveredBy(y).includeAnnotationsWithEndBeyondBounds()
            .asList()).containsExactly(a[2]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatCoveredByWithBeyondEndsCanSelectAnnotationsStartingAtSelectPosition(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);

    Annotation a = addToIndexes(new Annotation(cas.getJCas(), 0, 2));

    assertThat(cas.select(Annotation.class).coveredBy(0, 1).includeAnnotationsWithEndBeyondBounds()
            .asList()).as("Selection (0-1) including start position (0) but not end position (2)")
                    .containsExactly(a);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatCoveredByWithBeyondEndsCanSelectAnnotationsStartingAtSelectPosition2(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 0, 4),
            new Annotation(cas.getJCas(), 1, 3));

    assertThat(cas.select(Annotation.class).coveredBy(0, 2).includeAnnotationsWithEndBeyondBounds()
            .asList()).containsExactly(a);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatCoveredByWithBeyondEndsCanSelectAnnotationsStartingAtSelectPosition3(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 0, 5),
            new Annotation(cas.getJCas(), 1, 4), new Annotation(cas.getJCas(), 2, 6));

    assertThat(cas.select(Annotation.class).coveredBy(0, 3).includeAnnotationsWithEndBeyondBounds()
            .asList()).containsExactly(a);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatCoveredByWithBeyondEndsCanSelectAnnotationsStartingAtSelectPosition4(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 0, 4),
            new Annotation(cas.getJCas(), 1, 5), new Annotation(cas.getJCas(), 2, 2));

    assertThat(cas.select(Annotation.class).coveredBy(0, 3).includeAnnotationsWithEndBeyondBounds()
            .asList()).containsExactly(a);
  }

  /**
   * @see <a href="https://issues.apache.org/jira/browse/UIMA-6282">UIMA-6282</a>
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectAtDoesNotFindFollowingAnnotation(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 10, 20),
            new Annotation(cas.getJCas(), 21, MAX_VALUE));

    assertThat(cas.select(Annotation.class).at(a[0]).asList().contains(a[1])).isFalse();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectFollowingDoesFindOtherZeroWidthAnnotationAtEnd(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 10, 20),
            new Annotation(cas.getJCas(), 20, 20));

    assertThat(cas.select(Annotation.class).following(a[0]).asList()).containsExactly(a[1]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectPrecedingDoesFindZeroWidthAnnotationAtStart(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 10, 20),
            new Annotation(cas.getJCas(), 10, 10));

    assertThat(cas.select(Annotation.class).preceding(a[0]).asList()).containsExactly(a[1]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectFollowingDoesFindOtherZeroWidthAnnotationAtSameLocation(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 10, 10),
            new Annotation(cas.getJCas(), 10, 10));

    assertThat(cas.select(Annotation.class).following(a[0]).asList()).containsExactly(a[1]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectFollowingDoesNotFindOtherAnnotationAtSameLocation(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 10, 20),
            new Annotation(cas.getJCas(), 10, 20));

    assertThat(cas.select(Annotation.class).following(a[0]).asList()).isEmpty();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectPrecedingDoesFindOtherZeroWidthAnnotationAtSameLocation(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 10, 10),
            new Annotation(cas.getJCas(), 10, 10));

    assertThat(cas.select(Annotation.class).preceding(a[1]).asList()).containsExactly(a[0]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectPrecedingDoesNotFindOtherAnnotationAtSameLocation(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 10, 20),
            new Annotation(cas.getJCas(), 10, 20));

    assertThat(cas.select(Annotation.class).preceding(a[1]).asList()).isEmpty();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectCoveredByZeroSizeAtEndOfContextIsIncluded(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Sentence(cas.getJCas(), 0, 1),
            new Token(cas.getJCas(), 1, 1));

    assertThat(cas.select(Token.class).coveredBy(a[0]).asList()).containsExactly((Token) a[1]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectPrecedingDoesFindNonZeroWidthAnnotationEndingAtZeroWidthAnnotation(
          Mode aMode, String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 20, 20),
            new Annotation(cas.getJCas(), 10, 20));

    assertThat(cas.select(Annotation.class).preceding(a[0]).asList()).containsExactly(a[1]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectFollowingReturnsAdjacentAnnotation(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 10, 20),
            new Annotation(cas.getJCas(), 20, 30));

    assertThat(cas.select(Annotation.class).following(a[0]).asList()).containsExactly(a[1]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectFollowingSkipsAdjacentAnnotationAndReturnsNext(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 10, 20),
            new Annotation(cas.getJCas(), 20, 30), new Annotation(cas.getJCas(), 30, 40));

    assertThat(cas.select(Annotation.class).following(a[0], 1).asList()).containsExactly(a[2]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectFollowingBackwardsWorks(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation x = new Annotation(cas.getJCas(), 5, 14);
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 24, 46),
            new Annotation(cas.getJCas(), 76, 90));

    assertThat(cas.select(Annotation.class).following(x).backwards().asList()).containsExactly(a[1],
            a[0]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectPrecedingReturnsAdjacentAnnotation(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 10, 20),
            new Annotation(cas.getJCas(), 20, 30));

    assertThat(cas.select(Annotation.class).preceding(a[1]).asList()).containsExactly(a[0]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectPrecedingSkipsAdjacentAnnotationAndReturnsNext(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 10, 20),
            new Annotation(cas.getJCas(), 20, 30), new Annotation(cas.getJCas(), 30, 40));

    assertThat(cas.select(Annotation.class).preceding(a[2], 1).asList()).containsExactly(a[0]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectPrecedingSeekWorks(Mode aMode, String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);

    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");

    AnnotationFS[] expected = new AnnotationFS[2];
    Annotation y = (Annotation) cas.createAnnotation(type1, 61, 76);
    addToIndexes(expected[1] = cas.createAnnotation(type1, 40, 44),
            expected[0] = cas.createAnnotation(type1, 38, 50), cas.createAnnotation(type1, 61, 76));

    assertThat(cas.<Annotation> select(type1).preceding(y).asList())
            .containsExactly((Annotation) expected[0], (Annotation) expected[1]);

    FSIterator<Annotation> it = cas.<Annotation> select(type1).preceding(y).fsIterator();
    it.moveTo(expected[0]);
    assertThat(it.isValid()).isTrue();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectPrecedingSeekWorks2(Mode aMode, String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);

    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type3", "", "test.Type2");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");
    Type type3 = cas.getTypeSystem().getType("test.Type3");

    AnnotationFS[] expected = new AnnotationFS[4];
    AnnotationFS y;
    addToIndexes(expected[0] = cas.createAnnotation(type3, 37, 65),
            expected[3] = cas.createAnnotation(type3, 64, 67),
            expected[1] = cas.createAnnotation(type3, 58, 59),
            expected[2] = cas.createAnnotation(type2, 58, 59),
            y = cas.createAnnotation(type1, 76, 101));

    assertThat(cas.<Annotation> select(type2).preceding((Annotation) y).asList()).containsExactly(
            asList(expected).stream().map(a -> (Annotation) a).toArray(Annotation[]::new));

    FSIterator<Annotation> it = cas.<Annotation> select(type2).preceding((Annotation) y)
            .fsIterator();
    it.moveTo(expected[1]);
    assertThat(it.get()).isSameAs(expected[1]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectPrecedingSeekWithLimitWorks(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);

    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");

    AnnotationFS[] expected = new AnnotationFS[2];
    AnnotationFS y;
    addToIndexes(expected[1] = cas.createAnnotation(type1, 61, 70),
            y = cas.createAnnotation(type1, 88, 116),
            expected[0] = cas.createAnnotation(type1, 46, 49));

    assertThat(cas.<Annotation> select(type1).preceding((Annotation) y).limit(5).asList())
            .containsExactly(
                    asList(expected).stream().map(a -> (Annotation) a).toArray(Annotation[]::new));

    FSIterator<Annotation> it = cas.<Annotation> select(type1).preceding((Annotation) y).limit(5)
            .fsIterator();
    it.moveToNext();
    assertThat(it.get()).isSameAs(expected[1]);
    it.moveTo(expected[1]);
    assertThat(it.isValid()).isTrue();
    assertThat(it.get()).isSameAs(expected[1]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectFollowingDoesNotFindZeroWidthAnnotationAtEnd(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 10, 20),
            new Annotation(cas.getJCas(), 20, 20));

    assertThat(cas.select(Annotation.class).following(a[1]).asList()).isEmpty();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectFollowingSeekUnambiguousWorks(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");

    AnnotationFS[] expected = new AnnotationFS[2];
    AnnotationFS y = cas.createAnnotation(type1, 13, 28);
    addToIndexes(cas.createAnnotation(type1, 13, 28),
            expected[0] = cas.createAnnotation(type1, 29, 43),
            expected[1] = cas.createAnnotation(type1, 95, 115));

    assertThat(cas.<Annotation> select(type1).following((Annotation) y).nonOverlapping().asList())
            .containsExactly(
                    asList(expected).stream().map(a -> (Annotation) a).toArray(Annotation[]::new));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectFollowingSeekUnambiguousWorks2a(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");

    AnnotationFS y;
    addToIndexes(y = cas.createAnnotation(type1, 95, 95), cas.createAnnotation(type1, 43, 55),
            cas.createAnnotation(type1, 15, 22));

    assertThat(cas.<Annotation> select(type1).following((Annotation) y).nonOverlapping().asList())
            .isEmpty();

    FSIterator<Annotation> it = cas.<Annotation> select(type1).following((Annotation) y)
            .nonOverlapping().fsIterator();
    assertThat(it.isValid()).isFalse();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectFollowingSeekUnambiguousWorks2b(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");

    AnnotationFS y = cas.createAnnotation(type1, 95, 95);
    AnnotationFS expected;
    addToIndexes(expected = cas.createAnnotation(type1, 95, 95),
            cas.createAnnotation(type1, 43, 55), cas.createAnnotation(type1, 15, 22));

    assertThat(cas.<Annotation> select(type1).following((Annotation) y).nonOverlapping().asList())
            .containsExactly((Annotation) expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectFollowingSeekUnambiguousWorks3(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");

    Annotation[] expected = new Annotation[2];
    AnnotationFS y = cas.createAnnotation(type1, 55, 55);
    addToIndexes(expected[1] = (Annotation) cas.createAnnotation(type1, 76, 82),
            cas.createAnnotation(type1, 24, 49),
            expected[0] = (Annotation) cas.createAnnotation(type1, 55, 55));

    assertThat(cas.<Annotation> select(type1).following((Annotation) y).nonOverlapping().asList())
            .containsExactly(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectFollowingSeekAmbiguousWorks4(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");

    AnnotationFS y = cas.createAnnotation(type1, 83, 83);
    AnnotationFS[] expected = new AnnotationFS[2];
    addToIndexes(expected[0] = cas.createAnnotation(type1, 83, 111),
            expected[1] = cas.createAnnotation(type1, 83, 83), cas.createAnnotation(type1, 43, 44));

    assertThat(cas.<Annotation> select(type1).following((Annotation) y).asList())
            .containsExactly(stream(expected).map(a -> (Annotation) a).toArray(Annotation[]::new));

    assertThat(cas.<Annotation> select(type1).following(y.getBegin()).asList())
            .containsExactly(stream(expected).map(a -> (Annotation) a).toArray(Annotation[]::new));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectFollowingSeekUnambiguousWorks4(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");

    AnnotationFS y = cas.createAnnotation(type1, 83, 83);
    AnnotationFS expected;
    addToIndexes(expected = cas.createAnnotation(type1, 83, 111),
            cas.createAnnotation(type1, 83, 83), cas.createAnnotation(type1, 43, 44));

    assertThat(cas.<Annotation> select(type1).following((Annotation) y).nonOverlapping().asList())
            .containsExactly((Annotation) expected);

    assertThat(cas.<Annotation> select(type1).following(y.getBegin()).nonOverlapping().asList())
            .containsExactly((Annotation) expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectFollowingSeekUnambiguousWorks5(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");

    AnnotationFS y = cas.createAnnotation(type1, 82, 85);
    AnnotationFS expected;
    addToIndexes(expected = cas.createAnnotation(type1, 85, 85),
            cas.createAnnotation(type1, 24, 28), cas.createAnnotation(type1, 82, 85));

    assertThat(cas.<Annotation> select(type1).following((Annotation) y).nonOverlapping().asList())
            .containsExactly((Annotation) expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectPrecedingSeekUnambiguousWorks(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");

    AnnotationFS y;
    addToIndexes(cas.createAnnotation(type1, 96, 118), y = cas.createAnnotation(type1, 88, 88),
            cas.createAnnotation(type1, 81, 91));

    assertThat(cas.<Annotation> select(type1).preceding((Annotation) y).nonOverlapping().asList())
            .isEmpty();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectPrecedingUnambiguousFindsZeroWidth(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");

    AnnotationFS y = cas.createAnnotation(type1, 73, 73);
    AnnotationFS[] expected = new AnnotationFS[2];
    addToIndexes(cas.createAnnotation(type1, 97, 100),
            expected[0] = cas.createAnnotation(type1, 35, 62),
            expected[1] = cas.createAnnotation(type1, 73, 73));

    assertThat(cas.select(type1).preceding((Annotation) y).nonOverlapping().asList())
            .containsExactly(stream(expected).map(a -> (Annotation) a).toArray(Annotation[]::new));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatCoveredByFindsTypeUsingSubtype(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation superType = addToIndexes(new Annotation(cas.getJCas(), 5, 10));
    Token subType = addToIndexes(new Token(cas.getJCas(), 5, 10));

    assertThat(cas.select(Annotation.class).coveredBy(subType).asList()).containsExactly(superType);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatCoveredByFindsTypeUsingUnindexedSubtype(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation superType = addToIndexes(new Annotation(cas.getJCas(), 5, 10));
    Token subType = addToIndexes(new Token(cas.getJCas(), 5, 10));

    assertThat(cas.select(Annotation.class).coveredBy(subType).asList()).containsExactly(superType);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatCoveredByFindsSubtypeUsingType(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation superType = addToIndexes(new Annotation(cas.getJCas(), 5, 10));
    Annotation subType = addToIndexes(new Token(cas.getJCas(), 5, 10));

    assertThat(cas.select(Annotation.class).coveredBy(superType).asList()).containsExactly(subType);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatCoveredByFindsZeroWidth(Mode aMode, String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation x = addToIndexes(new Annotation(cas.getJCas(), 5, 10));
    Annotation y = addToIndexes(new Annotation(cas.getJCas(), 5, 10));

    assertThat(cas.select(Annotation.class).coveredBy(x).asList()).containsExactly(y);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatCoveredByWorksWithOffsets(Mode aMode, String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation a = addToIndexes(new Annotation(cas.getJCas(), 5, 10));

    assertThat(cas.select(Annotation.class).coveredBy(5, 10).asList()).containsExactly(a);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatCoveredBySkipsIndexedAnchorAnnotation(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    JCas jCas = cas.getJCas();

    Annotation[] a = addToIndexes(new Annotation(jCas, 5, 10), new Annotation(jCas, 5, 15),
            new Annotation(jCas, 0, 10), new Annotation(jCas, 0, 15), new Annotation(jCas, 5, 7),
            new Annotation(jCas, 8, 10), new Annotation(jCas, 6, 9), new Annotation(jCas, 5, 10));

    assertThat(jCas.select(Annotation.class).coveredBy(a[0]).asList()).containsExactly(a[7], a[4],
            a[6], a[5]);

    Annotation subType = addToIndexes(new Token(cas.getJCas(), 5, 10));

    assertThat(cas.select(Annotation.class).coveredBy(subType).asList()).containsExactly(a[0], a[7],
            a[4], a[6], a[5]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectAtFindsSupertype(Mode aMode, String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Annotation(cas.getJCas(), 5, 10),
            new Token(cas.getJCas(), 5, 10));

    assertThat(cas.select(Annotation.class).at(a[1]).asList()).containsExactly(a[0]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectBetweenWorks(Mode aMode, String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation[] a = addToIndexes(new Sentence(cas.getJCas(), 47, 67),
            new Sentence(cas.getJCas(), 55, 66), new Token(cas.getJCas(), 24, 29),
            new Token(cas.getJCas(), 66, 92));

    assertThat(cas.select(Sentence.class).between(a[2], a[3]).asList())
            .containsExactly((Sentence) a[1]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectColocatedFindsOtherAnnotation(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation x, y;
    addToIndexes(new Annotation(cas.getJCas(), 66, 84), y = new Annotation(cas.getJCas(), 66, 70),
            x = new Annotation(cas.getJCas(), 66, 70));

    assertThat(cas.select(Annotation.class).at(y).asList()).containsExactly(x);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectColocatedFindsSiblingType(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");
    tsd.addType("test.Type3", "", "test.Type1");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type2 = cas.getTypeSystem().getType("test.Type2");
    Type type3 = cas.getTypeSystem().getType("test.Type3");

    AnnotationFS x, y;
    addToIndexes(x = cas.createAnnotation(type2, 16, 42), y = cas.createAnnotation(type3, 16, 42));

    assertThat(cas.select(type2).at(y).asList()).containsExactly((Annotation) x);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectColocatedFindsSiblingType2(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");
    tsd.addType("test.Type3", "", "test.Type1");
    tsd.addType("test.Type4", "", "test.Type2");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    // Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");
    Type type3 = cas.getTypeSystem().getType("test.Type3");
    Type type4 = cas.getTypeSystem().getType("test.Type4");

    AnnotationFS x, y;
    addToIndexes(x = cas.createAnnotation(type2, 16, 42), cas.createAnnotation(type4, 16, 41),
            y = cas.createAnnotation(type3, 16, 42));

    assertThat(cas.select(type2).at(y).asList()).containsExactly((Annotation) x);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectCoveringDoesNotFindItselfWhenSelectingSupertype(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");
    tsd.addType("test.Type3", "", "test.Type2");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type2 = cas.getTypeSystem().getType("test.Type2");
    Type type3 = cas.getTypeSystem().getType("test.Type3");

    AnnotationFS y;
    addToIndexes(y = cas.createAnnotation(type3, 4, 33));

    assertThat(cas.select(type2).covering(y).asList()).isEmpty();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectAtInitialPositionIsSameAsFirstPosition(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation y = new Token(cas.getJCas(), 42, 71);
    addToIndexes(new Token(cas.getJCas(), 13, 34), new Token(cas.getJCas(), 42, 71));

    FSIterator<Token> it = cas.select(Token.class).at(y).fsIterator();
    assertThat(it.isValid()).isTrue();
    Annotation initial = it.isValid() ? it.get() : null;
    it.moveToFirst();
    assertThat(it.isValid() ? it.get() : null).isSameAs(initial);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectFollowingInitialPositionIsSameAsFirstPosition(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation y = new Token(cas.getJCas(), 13, 34);
    addToIndexes(new Token(cas.getJCas(), 13, 34), new Token(cas.getJCas(), 42, 71));

    FSIterator<Token> it = cas.select(Token.class).following(y).fsIterator();
    assertThat(it.isValid()).isTrue();
    Annotation initial = it.isValid() ? it.get() : null;
    it.moveToFirst();
    assertThat(it.isValid() ? it.get() : null).isSameAs(initial);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectBackwardsIterationMatchesForward(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    addToIndexes(new Sentence(cas.getJCas(), 15, 25), new Token(cas.getJCas(), 14, 19),
            new Token(cas.getJCas(), 13, 18), new Token(cas.getJCas(), 12, 17),
            new Sentence(cas.getJCas(), 12, 31), new Token(cas.getJCas(), 11, 16),
            new Token(cas.getJCas(), 10, 15), new Sentence(cas.getJCas(), 10, 20));

    List<Annotation> expected = cas.select(Annotation.class).limit(7).asList();
    List<Annotation> actual = toListBackwards(cas.select(Annotation.class).limit(7));

    assertThat(actual).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectFollowingBackwardsIterationMatchesForward(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation y = new Token(cas.getJCas(), 13, 17);
    addToIndexes(new Token(cas.getJCas(), 13, 17), new Token(cas.getJCas(), 83, 96));

    List<Token> expected = cas.select(Token.class).following(y).asList();
    List<Token> actual = toListBackwards(cas.select(Token.class).following(y));

    assertThat(actual).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectColocatedBackwardsIterationMatchesForward(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation y = new Token(cas.getJCas(), 99, 123);
    addToIndexes(new Token(cas.getJCas(), 99, 123), new Token(cas.getJCas(), 99, 102));

    List<Token> expected = cas.select(Token.class).at(y).asList();
    List<Token> actual = toListBackwards(cas.select(Token.class).at(y));

    assertThat(actual).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectColocatedIterationWithMoveTo(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    Annotation y;
    Token expected;
    addToIndexes(expected = new Token(cas.getJCas(), 99, 123),
            y = new Token(cas.getJCas(), 99, 123));

    FSIterator<Token> it = cas.select(Token.class).at(y).fsIterator();
    it.moveTo(y);

    assertThat(it.get()).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectCoveringBackwardsIterationMatchesForwardWithCoEndingAnnotations()
          throws Exception {
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type3", "", "test.Type2");
    tsd.addType("test.Type4", "", "test.Type3");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");
    Type type3 = cas.getTypeSystem().getType("test.Type3");
    Type type4 = cas.getTypeSystem().getType("test.Type4");

    AnnotationFS[] expected = new AnnotationFS[2];
    AnnotationFS y = cas.createAnnotation(type1, 77, 94);
    addToIndexes(expected[0] = cas.createAnnotation(type2, 65, 94),
            expected[1] = cas.createAnnotation(type3, 75, 99), cas.createAnnotation(type4, 71, 83));

    List<AnnotationFS> selection = toListBackwards(cas.<Annotation> select(type2).covering(y));

    printIndexOrder(cas, asList(expected), selection);

    assertThat(selection).containsExactly(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectCoveringBackwardsIterationMatchesForward2(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");
    tsd.addType("test.Type3", "", "test.Type2");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type2 = cas.getTypeSystem().getType("test.Type2");
    Type type3 = cas.getTypeSystem().getType("test.Type3");

    AnnotationFS[] expected = new AnnotationFS[2];
    AnnotationFS y = cas.createAnnotation(type2, 39, 42);
    addToIndexes(cas.createAnnotation(type2, 32, 35),
            expected[1] = cas.createAnnotation(type2, 39, 42),
            expected[0] = cas.createAnnotation(type3, 26, 45));

    List<AnnotationFS> selection = toListBackwards(cas.<Annotation> select(type2).covering(y));

    printIndexOrder(cas, asList(expected), selection);

    assertThat(selection).containsExactly(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectCoveredBackwardsIterationMatchesForward(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");

    AnnotationFS[] expected = new AnnotationFS[2];
    AnnotationFS y = cas.createAnnotation(type2, 71, 76);
    addToIndexes(expected[0] = cas.createAnnotation(type2, 71, 76),
            cas.createAnnotation(type2, 76, 84), expected[1] = cas.createAnnotation(type2, 76, 76));

    List<AnnotationFS> selection = toListBackwards(
            cas.<Annotation> select(type2).coveredBy(y).includeAnnotationsWithEndBeyondBounds());

    printIndexOrder(cas, asList(expected), selection);

    assertThat(selection).containsExactly(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectCoveredBackwardsIterationMatchesForward2(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");

    AnnotationFS[] expected = new AnnotationFS[2];
    AnnotationFS y = cas.createAnnotation(type1, 5, 33);
    addToIndexes(cas.createAnnotation(type2, 8, 35),
            expected[1] = cas.createAnnotation(type2, 9, 31),
            expected[0] = cas.createAnnotation(type2, 5, 29));

    List<AnnotationFS> selection = toListBackwards(cas.<Annotation> select(type2).coveredBy(y));

    printIndexOrder(cas, asList(expected), selection);

    assertThat(selection).containsExactly(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectFollowingBackwardsIterationMatchesForward2(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");

    AnnotationFS[] expected = new AnnotationFS[3];
    Annotation y = (Annotation) cas.createAnnotation(type2, 4, 4);
    addToIndexes(expected[0] = cas.createAnnotation(type2, 4, 14),
            expected[1] = cas.createAnnotation(type2, 66, 90),
            expected[2] = cas.createAnnotation(type2, 97, 99));

    List<AnnotationFS> selection = toListBackwards(cas.<Annotation> select(type2).following(y));

    printIndexOrder(cas, asList(expected), selection);

    assertThat(selection).containsExactly(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectCoveredBySeekToInitialThenMoveToNextWorks(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");

    AnnotationFS[] expected = new AnnotationFS[2];
    Annotation y = (Annotation) cas.createAnnotation(type2, 42, 59);
    addToIndexes(expected[0] = cas.createAnnotation(type2, 42, 59),
            expected[1] = cas.createAnnotation(type1, 42, 59));

    assertThat(cas.<Annotation> select(type1).coveredBy(y).asList())
            .containsExactly((Annotation) expected[0], (Annotation) expected[1]);

    FSIterator<Annotation> it = cas.<Annotation> select(type1).coveredBy(y).fsIterator();
    assertThat(it.get()).isSameAs(expected[0]);
    it.moveTo(y);
    assertThat(it.get()).isSameAs(expected[0]);

    it.moveToNext();
    assertThat(it.isValid()).isTrue();
    assertThat(it.get()).isSameAs(expected[1]);

    FSIterator<Annotation> it2 = cas.<Annotation> select(type1).coveredBy(y).fsIterator();
    assertThat(it2.get()).isSameAs(expected[0]);
    it2.moveTo(it2.get());
    assertThat(it2.get()).isSameAs(expected[0]);

    it2.moveToNext();
    assertThat(it2.isValid()).isTrue();
    assertThat(it2.get()).isSameAs(expected[1]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectCoveredBySeekNonStrictWorks(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");

    AnnotationFS[] expected = new AnnotationFS[2];
    Annotation y = (Annotation) cas.createAnnotation(type2, 9, 35);
    addToIndexes(expected[1] = cas.createAnnotation(type2, 9, 12),
            cas.createAnnotation(type1, 6, 26), expected[0] = cas.createAnnotation(type2, 9, 35),
            cas.createAnnotation(type1, 22, 31), cas.createAnnotation(type2, 67, 74),
            cas.createAnnotation(type1, 69, 80));

    assertThat(cas.<Annotation> select(type2).coveredBy(y).includeAnnotationsWithEndBeyondBounds()
            .asList()).containsExactly((Annotation) expected[0], (Annotation) expected[1]);

    FSIterator<Annotation> it = cas.<Annotation> select(type2)
            .includeAnnotationsWithEndBeyondBounds().coveredBy(y).fsIterator();
    it.moveTo(expected[1]);
    assertThat(it.get()).isSameAs(expected[1]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectCoveredBySeekUnambiguousNonStrictWorks(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");
    tsd.addType("test.Type3", "", "test.Type2");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");
    Type type3 = cas.getTypeSystem().getType("test.Type3");

    AnnotationFS expected, y;
    addToIndexes(expected = cas.createAnnotation(type3, 63, 90),
            cas.createAnnotation(type1, 86, 90), cas.createAnnotation(type2, 7, 36),
            cas.createAnnotation(type3, 46, 48), cas.createAnnotation(type1, 54, 80),
            cas.createAnnotation(type2, 30, 35), cas.createAnnotation(type3, 21, 37),
            cas.createAnnotation(type1, 82, 109), cas.createAnnotation(type2, 42, 45),
            cas.createAnnotation(type3, 41, 50), y = cas.createAnnotation(type1, 63, 76),
            cas.createAnnotation(type2, 70, 97), cas.createAnnotation(type3, 2, 26),
            cas.createAnnotation(type1, 24, 49), cas.createAnnotation(type2, 70, 94));

    assertThat(cas.<Annotation> select(type1).coveredBy(y).nonOverlapping()
            .includeAnnotationsWithEndBeyondBounds().asList())
                    .containsExactly((Annotation) expected);

    FSIterator<Annotation> it = cas.<Annotation> select(type1).nonOverlapping()
            .includeAnnotationsWithEndBeyondBounds().coveredBy(y).fsIterator();
    it.moveToLast();
    assertThat(it.get()).isSameAs(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectCoveredBySeekUnambiguousWorks(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type3", "", "test.Type2");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");
    Type type3 = cas.getTypeSystem().getType("test.Type3");

    AnnotationFS expected;
    AnnotationFS y = cas.createAnnotation(type1, 16, 29);
    addToIndexes(cas.createAnnotation(type2, 19, 19), cas.createAnnotation(type1, 28, 48),
            cas.createAnnotation(type3, 19, 32), expected = cas.createAnnotation(type2, 18, 19),
            cas.createAnnotation(type1, 63, 86), cas.createAnnotation(type3, 65, 70),
            cas.createAnnotation(type2, 25, 42), cas.createAnnotation(type1, 16, 29),
            cas.createAnnotation(type3, 26, 41));

    assertThat(cas.<Annotation> select(type2).coveredBy(y).nonOverlapping().asList())
            .containsExactly((Annotation) expected);

    FSIterator<Annotation> it = cas.<Annotation> select(type2).coveredBy(y).nonOverlapping()
            .fsIterator();
    it.moveToFirst();
    assertThat(it.isValid()).isTrue();
    assertThat(it.get()).isSameAs(expected);
    it.moveToLast();
    assertThat(it.isValid()).isTrue();
    assertThat(it.get()).isSameAs(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectCoveredBySeekLimitedWorks(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");
    tsd.addType("test.Type3", "", "test.Type2");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");
    Type type3 = cas.getTypeSystem().getType("test.Type3");

    AnnotationFS[] expected = new AnnotationFS[2];
    Annotation y = (Annotation) cas.createAnnotation(type1, 15, 31);
    addToIndexes(cas.createAnnotation(type1, 38, 43),
            expected[0] = cas.createAnnotation(type3, 15, 31),
            expected[1] = cas.createAnnotation(type2, 16, 21), cas.createAnnotation(type1, 15, 39),
            cas.createAnnotation(type3, 12, 18), cas.createAnnotation(type2, 14, 35),
            cas.createAnnotation(type1, 66, 85), cas.createAnnotation(type3, 55, 66),
            cas.createAnnotation(type2, 63, 80));

    assertThat(cas.<Annotation> select(type2).coveredBy(y).limit(5).asList())
            .containsExactly((Annotation) expected[0], (Annotation) expected[1]);

    FSIterator<Annotation> it = cas.<Annotation> select(type2).coveredBy(y).limit(5).fsIterator();
    it.moveToNext();
    assertThat(it.get()).isSameAs(expected[1]);
    it.moveTo(expected[1]);
    assertThat(it.isValid()).isTrue();
    assertThat(it.get()).isSameAs(expected[1]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectCoveredByUnambiguousSeekWorks(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");
    tsd.addType("test.Type3", "", "test.Type2");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");
    Type type3 = cas.getTypeSystem().getType("test.Type3");

    AnnotationFS[] expected = new AnnotationFS[3];
    Annotation y = (Annotation) cas.createAnnotation(type2, 24, 49);
    addToIndexes(cas.createAnnotation(type3, 63, 90), cas.createAnnotation(type1, 86, 90),
            cas.createAnnotation(type2, 7, 36), expected[2] = cas.createAnnotation(type3, 46, 48),
            cas.createAnnotation(type1, 54, 80), expected[0] = cas.createAnnotation(type2, 30, 35),
            cas.createAnnotation(type3, 21, 37), cas.createAnnotation(type1, 82, 109),
            expected[1] = cas.createAnnotation(type2, 42, 45), cas.createAnnotation(type3, 41, 50),
            cas.createAnnotation(type1, 63, 76), cas.createAnnotation(type2, 70, 97),
            cas.createAnnotation(type3, 2, 26), cas.createAnnotation(type1, 24, 49),
            cas.createAnnotation(type2, 70, 94));

    assertThat(cas.<Annotation> select(type2).coveredBy(y).nonOverlapping().asList())
            .containsExactly(
                    asList(expected).stream().map(a -> (Annotation) a).toArray(Annotation[]::new));

    FSIterator<Annotation> it = cas.<Annotation> select(type2).coveredBy(y).nonOverlapping()
            .fsIterator();
    it.moveToFirst();
    assertThat(it.get()).isSameAs(expected[0]);
    it.moveToLast();
    assertThat(it.get()).isSameAs(expected[2]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectColocatedSeekToInitialThenMoveToNextWorks(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");

    AnnotationFS expected, y;
    addToIndexes(cas.createAnnotation(type1, 77, 104), y = cas.createAnnotation(type2, 97, 126),
            cas.createAnnotation(type1, 51, 79), expected = cas.createAnnotation(type2, 97, 126),
            cas.createAnnotation(type1, 93, 99), cas.createAnnotation(type2, 89, 109));

    FSIterator<Annotation> it = cas.<Annotation> select(type2).at(y).fsIterator();
    it.moveToFirst();
    assertThat(it.get()).isSameAs(expected);
    it.moveTo(expected);
    assertThat(it.isValid());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectCoveringSeekToInitialThenMoveToNextWorks(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");
    tsd.addType("test.Type3", "", "test.Type2");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");
    Type type3 = cas.getTypeSystem().getType("test.Type3");

    AnnotationFS[] expected = new AnnotationFS[2];
    Annotation y = (Annotation) cas.createAnnotation(type2, 77, 83);
    addToIndexes(cas.createAnnotation(type2, 72, 72), cas.createAnnotation(type3, 25, 25),
            cas.createAnnotation(type1, 80, 80), cas.createAnnotation(type2, 36, 55),
            cas.createAnnotation(type3, 38, 41), cas.createAnnotation(type1, 53, 55),
            cas.createAnnotation(type2, 69, 71), cas.createAnnotation(type3, 44, 62),
            cas.createAnnotation(type1, 82, 103), cas.createAnnotation(type2, 29, 40),
            expected[0] = cas.createAnnotation(type3, 77, 83), cas.createAnnotation(type1, 85, 97),
            expected[1] = cas.createAnnotation(type2, 77, 83), cas.createAnnotation(type3, 36, 37),
            cas.createAnnotation(type1, 37, 42));

    assertThat(cas.<Annotation> select(type2).covering(y).asList())
            .containsExactly((Annotation) expected[0], (Annotation) expected[1]);

    FSIterator<Annotation> it = cas.<Annotation> select(type2).covering(y).fsIterator();
    assertThat(it.get()).as("Initial iterator position").isSameAs(expected[0]);

    it.moveTo(expected[0]);
    assertThat(it.get()).isSameAs(expected[0]);

    it.moveToNext();
    assertThat(it.isValid()).isTrue();
    assertThat(it.get()).isSameAs(expected[1]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectColocatedSeekWithLimitWorks(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");
    tsd.addType("test.Type3", "", "test.Type2");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");
    Type type3 = cas.getTypeSystem().getType("test.Type3");

    AnnotationFS[] expected = new AnnotationFS[2];
    AnnotationFS y = cas.createAnnotation(type1, 45, 51);
    addToIndexes(expected[0] = cas.createAnnotation(type3, 45, 51),
            expected[1] = cas.createAnnotation(type2, 45, 51));

    FSIterator<Annotation> it = cas.<Annotation> select(type2).at(y).limit(5).fsIterator();
    it.moveToNext();
    assertThat(it.get()).isSameAs(expected[1]);
    // Move back to the first via moveTo
    it.moveTo(expected[0]);
    assertThat(it.isValid()).isTrue();
    assertThat(it.get()).isSameAs(expected[0]);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectCoveringSeekThenMoveToNextWorks(Mode aMode, String[] aPrioTypeNames)
          throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");

    AnnotationFS[] expected = new AnnotationFS[2];
    Annotation y = (Annotation) cas.createAnnotation(type1, 60, 82);
    addToIndexes(expected[0] = cas.createAnnotation(type2, 60, 82),
            cas.createAnnotation(type1, 62, 91), expected[1] = cas.createAnnotation(type1, 60, 82));

    assertThat(cas.<Annotation> select(type1).covering(y).asList())
            .containsExactly((Annotation) expected[0], (Annotation) expected[1]);

    FSIterator<Annotation> it = cas.<Annotation> select(type1).covering(y).fsIterator();
    System.out.println("---- moveTo");
    it.moveTo(expected[0]);
    assertThat(it.get()).isSameAs(expected[0]);
    System.out.println("---- moveToNext");
    it.moveToNext();
    assertThat(it.isValid()).isTrue();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectColocatedSeekWorks(Mode aMode, String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");

    AnnotationFS expected, y;
    addToIndexes(y = cas.createAnnotation(type2, 52, 53), cas.createAnnotation(type1, 92, 102),
            expected = cas.createAnnotation(type2, 52, 53), cas.createAnnotation(type2, 6, 19));

    assertThat(cas.<Annotation> select(type2).at(y).asList())
            .containsExactly((Annotation) expected);

    FSIterator<Annotation> it = cas.<Annotation> select(type1).at(y).fsIterator();
    System.out.println("---- moveTo");
    it.moveTo(expected);
    assertThat(it.isValid()).isTrue();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectColocatedSeekWorks2(Mode aMode, String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");

    AnnotationFS expected, y;
    addToIndexes(y = cas.createAnnotation(type2, 6, 31),
            expected = cas.createAnnotation(type2, 6, 31));

    assertThat(cas.<Annotation> select(type2).at(y).asList())
            .containsExactly((Annotation) expected);

    FSIterator<Annotation> it = cas.<Annotation> select(type1).at(y).fsIterator();
    System.out.println("---- moveTo");
    it.moveTo(expected);
    assertThat(it.isValid()).isTrue();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSelectColocatedSeekWorks3(Mode aMode, String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", "test.Type1");

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");

    AnnotationFS expected, y;
    addToIndexes(y = cas.createAnnotation(type2, 18, 39),
            expected = cas.createAnnotation(type2, 18, 39), cas.createAnnotation(type2, 18, 43));

    assertThat(cas.<Annotation> select(type2).at(y).asList())
            .containsExactly((Annotation) expected);

    FSIterator<Annotation> it = cas.<Annotation> select(type1).at(y).fsIterator();
    System.out.println("---- moveToLast");
    it.moveToLast();
    assertThat(it.get()).isSameAs(expected);
    System.out.println("---- moveTo");
    it.moveTo(expected);
    assertThat(it.isValid()).isTrue();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("prios")
  public void thatSeekingIteratorToOutOfIndexPositionOnRightIsInvalid(Mode aMode,
          String[] aPrioTypeNames) throws Exception {
    setup(aMode, aPrioTypeNames);
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType("test.Type1", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type2", "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType("test.Type3", "", CAS.TYPE_NAME_ANNOTATION);

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type type1 = cas.getTypeSystem().getType("test.Type1");
    Type type2 = cas.getTypeSystem().getType("test.Type2");
    Type type3 = cas.getTypeSystem().getType("test.Type3");

    AnnotationFS window, seekPoint;
    addToIndexes(window = cas.createAnnotation(type1, 0, 10), cas.createAnnotation(type2, 5, 6),
            seekPoint = cas.createAnnotation(type3, 8, 9), cas.createAnnotation(type2, 15, 16));

    FSIterator<AnnotationFS> it = cas.getAnnotationIndex(type2).select().coveredBy(window)
            .fsIterator();

    it.moveTo(seekPoint);

    assertThat(it.isValid()).isFalse();
  }

  @Test
  public void thatSelectingNonExistingTypeCreatesException() throws Exception {
    setup(Mode.ANNOTATION_FIRST);

    Type tokenType = cas.getCasType(Token.class);
    CAS localCas = CasCreationUtils.createCas();

    assertThat((Iterable<Annotation>) localCas.select(Annotation.class))
            .as("Select existing type by JCas cover-class") //
            .isEmpty();

    assertThatExceptionOfType(CASRuntimeException.class) //
            .isThrownBy(() -> localCas.select(Token.class))
            .as("Select non-existing type by JCas cover-class");

    assertThat((Iterable<TOP>) localCas.select(localCas.getAnnotationType()))
            .as("Select existing type by CAS type") //
            .isEmpty();

    assertThatExceptionOfType(IllegalArgumentException.class) //
            .isThrownBy(() -> localCas.select(tokenType))
            .as("Select non-existing type by CAS type (obtained from another CAS)");

    assertThat((Iterable<TOP>) localCas.select(Annotation.type))
            .as("Select existing type by JCas type ID") //
            .isEmpty();

    assertThatExceptionOfType(CASRuntimeException.class) //
            .isThrownBy(() -> localCas.select(Token.type))
            .as("Select non-existing type by JCas type ID");

    assertThat((Iterable<TOP>) localCas.select(Annotation._TypeName))
            .as("Select existing type by type name") //
            .isEmpty();

    assertThatExceptionOfType(IllegalArgumentException.class) //
            .isThrownBy(() -> localCas.select(Token._TypeName))
            .as("Select non-existing type by type name");

  }

  @SuppressWarnings("unchecked")
  private static <T extends AnnotationFS, R extends AnnotationFS> List<R> toListBackwards(
          SelectFSs<T> select) {
    FSIterator<T> it = select.fsIterator();
    List<R> selection = new ArrayList<>();
    it.moveToLast();
    while (it.isValid()) {
      selection.add(0, (R) it.get());
      it.moveToPrevious();
    }
    return selection;
  }

  private static void printIndexOrder(CAS cas, List<AnnotationFS> expected,
          List<AnnotationFS> selection) {
    System.out.println("Annotations in index order:");
    cas.select(Annotation.class).filter(ann -> ann.getType().getName().startsWith("test."))
            .forEach(ann -> System.out.printf("%s [%d, %d] %s %n", ann.getType().getShortName(),
                    ann.getBegin(), ann.getEnd(),
                    expected.contains(ann) && selection.contains(ann) ? "TP"
                            : selection.contains(ann) ? "FP" : ""));
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
