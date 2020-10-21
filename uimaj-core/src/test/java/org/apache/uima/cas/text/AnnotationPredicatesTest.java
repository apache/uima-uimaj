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
package org.apache.uima.cas.text;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.text.AnnotationPredicates.colocated;
import static org.apache.uima.cas.text.AnnotationPredicates.coveredBy;
import static org.apache.uima.cas.text.AnnotationPredicates.covers;
import static org.apache.uima.cas.text.AnnotationPredicates.leftOf;
import static org.apache.uima.cas.text.AnnotationPredicates.overlaps;
import static org.apache.uima.cas.text.AnnotationPredicates.overlapsLeft;
import static org.apache.uima.cas.text.AnnotationPredicates.overlapsRight;
import static org.apache.uima.cas.text.AnnotationPredicates.rightOf;
import static org.apache.uima.cas.text.AnnotationPredicatesTest.RelativePosition.COLOCATED;
import static org.apache.uima.cas.text.AnnotationPredicatesTest.RelativePosition.COVERED_BY;
import static org.apache.uima.cas.text.AnnotationPredicatesTest.RelativePosition.COVERING;
import static org.apache.uima.cas.text.AnnotationPredicatesTest.RelativePosition.LEFT_OF;
import static org.apache.uima.cas.text.AnnotationPredicatesTest.RelativePosition.OVERLAPPING;
import static org.apache.uima.cas.text.AnnotationPredicatesTest.RelativePosition.OVERLAPPING_LEFT;
import static org.apache.uima.cas.text.AnnotationPredicatesTest.RelativePosition.OVERLAPPING_RIGHT;
import static org.apache.uima.cas.text.AnnotationPredicatesTest.RelativePosition.RIGHT_OF;

import java.util.List;
import java.util.function.Function;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.util.CasCreationUtils;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AnnotationPredicatesTest {
    
    public static enum RelativePosition {
      COLOCATED,
      OVERLAPPING,
      OVERLAPPING_LEFT,
      OVERLAPPING_RIGHT,
      COVERING,
      COVERED_BY,
      LEFT_OF,
      RIGHT_OF
    }
    
    // Used as fixed references for the annotation relation cases.
    private static final int BEGIN = 10;
    private static final int END = 20;
    private static final int Z_POS = 10;

    public static final List<TestCase> TEST_CASES = asList(
        new TestCase("1) Y begins and ends after X (### [---])", 
                p -> p.apply(BEGIN, END, END + 1, MAX_VALUE),
                asList(LEFT_OF)),
        new TestCase("2) Y begins at X's end and ends after X (###[---])", 
                p -> p.apply(BEGIN, END, END, MAX_VALUE),
                asList(LEFT_OF)),
        new TestCase("3) Y begins within and ends after X (##[#--])", 
                p -> p.apply(BEGIN, END, END - 1 , MAX_VALUE),
                asList(OVERLAPPING, OVERLAPPING_LEFT)),
        new TestCase("4) Y begins and ends at X's boundries ([###])", 
                p -> p.apply(BEGIN, END, BEGIN, END),
                asList(OVERLAPPING, COLOCATED, COVERED_BY, COVERING)),
        new TestCase("5) Y begins and ends within X (#[#]#)", 
                p -> p.apply(BEGIN, END, BEGIN + 1, END - 1),
                asList(OVERLAPPING, COVERING)),
        new TestCase("6) Y begins at and ends before X's boundries ([##]#)", 
                p -> p.apply(BEGIN, END, BEGIN, END - 1),
                asList(OVERLAPPING, COVERING)),
        new TestCase("7) Y begins after and ends at X's boundries (#[##])", 
                p -> p.apply(BEGIN, END, BEGIN + 1, END),
                asList(OVERLAPPING, COVERING)),
        new TestCase("8) Y begins before and ends after X's boundries ([-###-])", 
                p -> p.apply(BEGIN, END, BEGIN - 1, END + 1),
                asList(OVERLAPPING, COVERED_BY)),
        new TestCase("9) X starts where Y begins and ends within Y ([##-])", 
                p -> p.apply(BEGIN, END, BEGIN, END + 1),
                asList(OVERLAPPING, COVERED_BY)),
        new TestCase("10) X starts within Y and ends where Y ends ([-##])", 
                p -> p.apply(BEGIN, END, BEGIN - 1, END),
                asList(OVERLAPPING, COVERED_BY)),
        new TestCase("11) Y begins before and ends within X ([--#]##)", 
                p -> p.apply(BEGIN, END, 0, BEGIN + 1),
                asList(OVERLAPPING, OVERLAPPING_RIGHT)),
        new TestCase("12) Y begins before and ends where X begins ([---]###)", 
                p -> p.apply(BEGIN, END, 0, BEGIN),
                asList(RIGHT_OF)),
        new TestCase("13) Y begins and ends before X begins ([---] ###)", 
                p -> p.apply(BEGIN, END, 0, BEGIN - 1),
                asList(RIGHT_OF)),
        new TestCase("Z1) Zero-width X before Y start (# [---])", 
                p -> p.apply(Z_POS, Z_POS, Z_POS + 10, Z_POS + 20),
                asList(LEFT_OF)),
        new TestCase("Z2) Zero-width Y after X's end (### |)", 
                p -> p.apply(BEGIN, END, END + 1, END + 1),
                asList(LEFT_OF)),
        new TestCase("Z3) Zero-width X at Y's start (#---])", 
                p -> p.apply(Z_POS, Z_POS, Z_POS, Z_POS + 10),
                asList(OVERLAPPING, COVERED_BY)),
        new TestCase("Z4) Zero-width X at Y's end ([---#)", 
                p -> p.apply(Z_POS, Z_POS, Z_POS-10, Z_POS),
                asList(RIGHT_OF)),
        new TestCase("Z5) Zero-width Y where X begins (|###)", 
                p -> p.apply(BEGIN, END, BEGIN, BEGIN),
                asList(OVERLAPPING, COVERING)),
        new TestCase("Z6) Zero-width Y within X (#|#)", 
                p -> p.apply(BEGIN, END, BEGIN + 1, BEGIN + 1),
                asList(OVERLAPPING, COVERING)),
        new TestCase("Z7) Zero-width Y at X's end (###|)", 
                p -> p.apply(BEGIN, END, END, END),
                asList(LEFT_OF)),
        new TestCase("Z8) Zero-width X with Y (-|-)", 
                p -> p.apply(Z_POS, Z_POS, Z_POS - 5, Z_POS + 5),
                asList(OVERLAPPING, COVERED_BY)),
        new TestCase("Z9) Zero-width X after Y's end ([---] #)", 
                p -> p.apply(Z_POS, Z_POS, Z_POS - 10, Z_POS - 5),
                asList(RIGHT_OF)),
        new TestCase("Z10) Zero-width Y before X begins (| ###)", 
                p -> p.apply(BEGIN, END, BEGIN - 1, BEGIN - 1),
                asList(RIGHT_OF)),
        new TestCase("Z11) Zero-width X matches zero-width Y start/end (#)", 
                p -> p.apply(Z_POS, Z_POS, Z_POS, Z_POS),
                asList(OVERLAPPING, COVERED_BY, COVERING, COLOCATED)));
  
    @Test
    public void thatCoveringWithIntervalsWorks() throws Exception {
      assertPosition(COVERING, AnnotationPredicates::covers);
    }

    @Test
    public void thatCoveringWithAnnotationAndIntervalWorks() throws Exception {
      CAS cas = CasCreationUtils.createCas();;
      Type type = cas.getAnnotationType();
      
      assertPosition(COVERING, (beginA, endA, beginB,
              endB) -> covers(cas.createAnnotation(type, beginA, endA), beginB, endB));
    }

    @Test
    public void thatCoveringWithAnnotationsWorks() throws Exception {
      CAS cas = CasCreationUtils.createCas();;
      Type type = cas.getAnnotationType();
      
      assertPosition(COVERING,
              (beginA, endA, beginB, endB) -> covers(cas.createAnnotation(type, beginA, endA),
                      cas.createAnnotation(type, beginB, endB)));
    }
    
    @Test
    public void thatCoveredByWithIntervalsWorks() throws Exception {
      assertPosition(COVERED_BY, AnnotationPredicates::coveredBy);
    }

    @Test
    public void thatCoveredByWithAnnotationAndIntervalWorks() throws Exception {
      CAS cas = CasCreationUtils.createCas();
      Type type = cas.getAnnotationType();
      
      assertPosition(COVERED_BY, (beginA, endA, beginB,
              endB) -> coveredBy(cas.createAnnotation(type, beginA, endA), beginB, endB));
    }

    @Test
    public void thatCoveredByWithAnnotationsWorks() throws Exception {
      CAS cas = CasCreationUtils.createCas();
      Type type = cas.getAnnotationType();
      
      assertPosition(COVERED_BY,
              (beginA, endA, beginB, endB) -> coveredBy(cas.createAnnotation(type, beginA, endA),
                      cas.createAnnotation(type, beginB, endB)));
    }

    @Test
    public void thatColocatedWithIntervalsWorks() throws Exception {
      assertPosition(COLOCATED, AnnotationPredicates::colocated);
      
      // It must also work if we switch the of the spans
      assertPosition(COLOCATED,
              (beginA, endA, beginB, endB) -> colocated(beginB, endB, beginA, endA));
    }

    @Test
    public void thatColocatedWithAnnotationAndIntervalWorks() throws Exception
    {
      CAS cas = CasCreationUtils.createCas();
      Type type = cas.getAnnotationType();
      
      assertPosition(COLOCATED, (beginA, endA, beginB,
              endB) -> colocated(cas.createAnnotation(type, beginA, endA), beginB, endB));
      
      // It must also work if we switch the of the spans
      assertPosition(COLOCATED, (beginA, endA, beginB,
              endB) -> colocated(cas.createAnnotation(type, beginB, endB), beginA, endA));
    }

    @Test
    public void thatColocatedWithAnnotationsWorks() throws Exception
    {
      CAS cas = CasCreationUtils.createCas();
      Type type = cas.getAnnotationType();
      
      assertPosition(COLOCATED,
              (beginA, endA, beginB, endB) -> colocated(cas.createAnnotation(type, beginA, endA),
                      cas.createAnnotation(type, beginB, endB)));
      
      // It must also work if we switch the of the spans
      assertPosition(COLOCATED,
              (beginA, endA, beginB, endB) -> colocated(cas.createAnnotation(type, beginB, endB),
                      cas.createAnnotation(type, beginA, endA)));
    }
    
    @Test
    public void thatOverlapsLeftWithIntervalsWorks() throws Exception {
      assertPosition(OVERLAPPING_LEFT, AnnotationPredicates::overlapsLeft);
    }

    @Test
    public void thatOverlapsLeftWithAnnotationAndIntervalWorks() throws Exception {
      CAS cas = CasCreationUtils.createCas();
      Type type = cas.getAnnotationType();
      
      assertPosition(OVERLAPPING_LEFT, (beginA, endA, beginB,
              endB) -> overlapsLeft(cas.createAnnotation(type, beginA, endA), beginB, endB));
    }

    @Test
    public void thatOverlapsLeftWithAnnotationsWorks() throws Exception {
      CAS cas = CasCreationUtils.createCas();
      Type type = cas.getAnnotationType();
      
      assertPosition(OVERLAPPING_LEFT,
              (beginA, endA, beginB, endB) -> overlapsLeft(cas.createAnnotation(type, beginA, endA),
                      cas.createAnnotation(type, beginB, endB)));
    }

    @Test
    public void thatOverlapsRightWithIntervalsWorks() throws Exception {
      assertPosition(OVERLAPPING_RIGHT, AnnotationPredicates::overlapsRight);
    }

    @Test
    public void thatOverlapsRightWithAnnotationAndIntervalWorks() throws Exception {
      CAS cas = CasCreationUtils.createCas();
      Type type = cas.getAnnotationType();
      
      assertPosition(OVERLAPPING_RIGHT, (beginA, endA, beginB,
              endB) -> overlapsRight(cas.createAnnotation(type, beginA, endA), beginB, endB));
    }

    @Test
    public void thatOverlapsRightWithAnnotationsWorks() throws Exception {
      CAS cas = CasCreationUtils.createCas();
      Type type = cas.getAnnotationType();
      
      assertPosition(OVERLAPPING_RIGHT,
              (beginA, endA, beginB, endB) -> overlapsRight(cas.createAnnotation(type, beginA, endA),
                      cas.createAnnotation(type, beginB, endB)));
    }
    
    @Test
    public void thatOverlapsWithIntervalsWorks() throws Exception {
      assertPosition(OVERLAPPING, AnnotationPredicates::overlaps);

      // It must also work if we switch the of the spans
      assertPosition(OVERLAPPING,
              (beginA, endA, beginB, endB) -> overlaps(beginB, endB, beginA, endA));
    }

    @Test
    public void thatOverlapsWithAnnotationAndIntervalWorks() throws Exception
    {
      CAS cas = CasCreationUtils.createCas();
      Type type = cas.getAnnotationType();
      
      assertPosition(OVERLAPPING, (beginA, endA, beginB,
              endB) -> overlaps(cas.createAnnotation(type, beginA, endA), beginB, endB));
      
      // It must also work if we switch the of the spans
      assertPosition(OVERLAPPING, (beginA, endA, beginB,
              endB) -> overlaps(cas.createAnnotation(type, beginB, endB), beginA, endA));
    }

    @Test
    public void thatOverlapsWithAnnotationsWorks() throws Exception
    {
      CAS cas = CasCreationUtils.createCas();
      Type type = cas.getAnnotationType();
      
      assertPosition(OVERLAPPING,
              (beginA, endA, beginB, endB) -> overlaps(cas.createAnnotation(type, beginA, endA),
                      cas.createAnnotation(type, beginB, endB)));
      
      // It must also work if we switch the of the spans
      assertPosition(OVERLAPPING,
              (beginA, endA, beginB, endB) -> overlaps(cas.createAnnotation(type, beginB, endB),
                      cas.createAnnotation(type, beginA, endA)));
    }
    
    @Test
    public void thatLeftOfWithIntervalsWorks() throws Exception {
      assertPosition(LEFT_OF, AnnotationPredicates::leftOf);
    }

    @Test
    public void thatLeftOfWithAnnotationAndIntervalWorks() throws Exception {
      CAS cas = CasCreationUtils.createCas();
      Type type = cas.getAnnotationType();
      
      assertPosition(LEFT_OF, (beginA, endA, beginB,
              endB) -> leftOf(cas.createAnnotation(type, beginA, endA), beginB, endB));
    }

    @Test
    public void thatLeftOfWithAnnotationsWorks() throws Exception {
      CAS cas = CasCreationUtils.createCas();
      Type type = cas.getAnnotationType();
      
      assertPosition(LEFT_OF,
              (beginA, endA, beginB, endB) -> leftOf(cas.createAnnotation(type, beginA, endA),
                      cas.createAnnotation(type, beginB, endB)));
    }

    @Test
    public void thatRightOfWithIntervalsWorks() throws Exception {
      assertPosition(RIGHT_OF, AnnotationPredicates::rightOf);
    }

    @Test
    public void thatRightOfWithAnnotationAndIntervalWorks() throws Exception {
      CAS cas = CasCreationUtils.createCas();
      Type type = cas.getAnnotationType();

      assertPosition(RIGHT_OF, (beginA, endA, beginB,
              endB) -> rightOf(cas.createAnnotation(type, beginA, endA), beginB, endB));
    }

    @Test
    public void thatRightOfWithAnnotationsWorks() throws Exception {
      CAS cas = CasCreationUtils.createCas();
      Type type = cas.getAnnotationType();

      assertPosition(RIGHT_OF,
              (beginA, endA, beginB, endB) -> rightOf(cas.createAnnotation(type, beginA, endA),
                      cas.createAnnotation(type, beginB, endB)));
    }
    
    public void assertPosition(RelativePosition aCondition, RelativePositionPredicate aPredicate)
            throws Exception {
        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
          for (TestCase testCase : TEST_CASES) {
            softly.assertThat(testCase.getTest().apply(aPredicate))
              .as(testCase.getDescription())
              .isEqualTo(testCase.getValidPositions().contains(aCondition));
          }
        }
    }
    
    @FunctionalInterface
    public static interface RelativePositionPredicate {
      boolean apply(int beginA, int endA, int beginB, int endB);
    }

    public static class TestCase {
      private final String description;

      private final Function<RelativePositionPredicate, Boolean> predicate;
      
      private final List<RelativePosition> validPositions;

      public TestCase(String aDescription, Function<RelativePositionPredicate, Boolean> aPredicate, List<RelativePosition> aValidPositions) {
        description = aDescription;
        predicate = aPredicate;
        validPositions = aValidPositions;
      }

      public String getDescription() {
        return description;
      }

      public Function<RelativePositionPredicate, Boolean> getTest() {
        return predicate;
      }
      
      public List<RelativePosition> getValidPositions() {
        return validPositions;
      }
    }
  }