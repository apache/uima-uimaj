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
package org.apache.uima.fit.util;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.AnnotationPredicates.colocated;
import static org.apache.uima.fit.util.AnnotationPredicates.coveredBy;
import static org.apache.uima.fit.util.AnnotationPredicates.covers;
import static org.apache.uima.fit.util.AnnotationPredicates.leftOf;
import static org.apache.uima.fit.util.AnnotationPredicates.overlaps;
import static org.apache.uima.fit.util.AnnotationPredicates.overlapsLeft;
import static org.apache.uima.fit.util.AnnotationPredicates.overlapsRight;
import static org.apache.uima.fit.util.AnnotationPredicates.rightOf;
import static org.apache.uima.fit.util.AnnotationPredicatesTest.RelativePosition.COLOCATED;
import static org.apache.uima.fit.util.AnnotationPredicatesTest.RelativePosition.COVERED_BY;
import static org.apache.uima.fit.util.AnnotationPredicatesTest.RelativePosition.COVERING;
import static org.apache.uima.fit.util.AnnotationPredicatesTest.RelativePosition.LEFT_OF;
import static org.apache.uima.fit.util.AnnotationPredicatesTest.RelativePosition.OVERLAPPING;
import static org.apache.uima.fit.util.AnnotationPredicatesTest.RelativePosition.OVERLAPPING_LEFT;
import static org.apache.uima.fit.util.AnnotationPredicatesTest.RelativePosition.OVERLAPPING_RIGHT;
import static org.apache.uima.fit.util.AnnotationPredicatesTest.RelativePosition.RIGHT_OF;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.factory.CasFactory;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.Test;

public class AnnotationPredicatesTest {
    
    static enum RelativePosition {
      COLOCATED,
      OVERLAPPING,
      OVERLAPPING_LEFT,
      OVERLAPPING_RIGHT,
      COVERING,
      COVERED_BY,
      LEFT_OF,
      RIGHT_OF
}
  
    @Test
    public void thatCoveringWithIntervalsWorks() throws Exception {
      assertPosition(COVERING, AnnotationPredicates::covers);
    }

    @Test
    public void thatCoveringWithAnnotationAndIntervalWorks() throws Exception {
      CAS cas = CasFactory.createCas();
      Type type = cas.getAnnotationType();
      
      assertPosition(COVERING, (beginA, endA, beginB,
              endB) -> covers(cas.createAnnotation(type, beginA, endA), beginB, endB));
    }

    @Test
    public void thatCoveringWithAnnotationsWorks() throws Exception {
      CAS cas = CasFactory.createCas();
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
      CAS cas = CasFactory.createCas();
      Type type = cas.getAnnotationType();
      
      assertPosition(COVERED_BY, (beginA, endA, beginB,
              endB) -> coveredBy(cas.createAnnotation(type, beginA, endA), beginB, endB));
    }

    @Test
    public void thatCoveredByWithAnnotationsWorks() throws Exception {
      CAS cas = CasFactory.createCas();
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
      CAS cas = CasFactory.createCas();
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
      CAS cas = CasFactory.createCas();
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
      CAS cas = CasFactory.createCas();
      Type type = cas.getAnnotationType();
      
      assertPosition(OVERLAPPING_LEFT, (beginA, endA, beginB,
              endB) -> overlapsLeft(cas.createAnnotation(type, beginA, endA), beginB, endB));
    }

    @Test
    public void thatOverlapsLeftWithAnnotationsWorks() throws Exception {
      CAS cas = CasFactory.createCas();
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
      CAS cas = CasFactory.createCas();
      Type type = cas.getAnnotationType();
      
      assertPosition(OVERLAPPING_RIGHT, (beginA, endA, beginB,
              endB) -> overlapsRight(cas.createAnnotation(type, beginA, endA), beginB, endB));
    }

    @Test
    public void thatOverlapsRightWithAnnotationsWorks() throws Exception {
      CAS cas = CasFactory.createCas();
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
      CAS cas = CasFactory.createCas();
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
      CAS cas = CasFactory.createCas();
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
      CAS cas = CasFactory.createCas();
      Type type = cas.getAnnotationType();
      
      assertPosition(LEFT_OF, (beginA, endA, beginB,
              endB) -> leftOf(cas.createAnnotation(type, beginA, endA), beginB, endB));
    }

    @Test
    public void thatLeftOfWithAnnotationsWorks() throws Exception {
      CAS cas = CasFactory.createCas();
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
      CAS cas = CasFactory.createCas();
      Type type = cas.getAnnotationType();

      assertPosition(RIGHT_OF, (beginA, endA, beginB,
              endB) -> rightOf(cas.createAnnotation(type, beginA, endA), beginB, endB));
    }

    @Test
    public void thatRightOfWithAnnotationsWorks() throws Exception {
      CAS cas = CasFactory.createCas();
      Type type = cas.getAnnotationType();

      assertPosition(RIGHT_OF,
              (beginA, endA, beginB, endB) -> rightOf(cas.createAnnotation(type, beginA, endA),
                      cas.createAnnotation(type, beginB, endB)));
    }

    @FunctionalInterface
    private static interface RelativePositionPredicate {
      boolean apply(int beginA, int endA, int beginB, int endB);
    }

    public void assertPosition(RelativePosition aCondition, RelativePositionPredicate aPredicate)
            throws Exception {
        // Define a fixed interval around which we build most of the tests by applying different
        // selections to it
        int begin = 10;
        int end = 20;

        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
          softly.assertThat(aPredicate.apply(begin, end, begin - 1, begin - 1))
              .as("Zero-width B before A begins (| ###)")
              .isEqualTo(asList(RIGHT_OF).contains(aCondition));
        
          softly.assertThat(aPredicate.apply(begin, end, 0, begin - 1))
              .as("B begins and ends before A begins ([---] ###)")
              .isEqualTo(asList(RIGHT_OF).contains(aCondition));
        
          softly.assertThat(aPredicate.apply(begin, end, 0, begin))
              .as("B begins before and ends where A begins ([---]###)")
              .isEqualTo(asList(RIGHT_OF).contains(aCondition));

          softly.assertThat(aPredicate.apply(begin, end, begin, begin))
              .as("Zero-width B where A begins (|###)")
              .isEqualTo(asList(OVERLAPPING, COVERING).contains(aCondition));

          softly.assertThat(aPredicate.apply(begin, end, 0, begin + 1))
              .as("B begins before and ends within A ([--#]##)")
              .isEqualTo(asList(OVERLAPPING, OVERLAPPING_RIGHT).contains(aCondition));
        
          softly.assertThat(aPredicate.apply(begin, end, begin + 1, end - 1))
              .as("B begins and ends within A (#[#]#)")
              .isEqualTo(asList(OVERLAPPING, COVERING).contains(aCondition));

          softly.assertThat(aPredicate.apply(begin, end, begin + 1, end))
              .as("B begins after and ends at A's boundries (#[##])")
              .isEqualTo(asList(OVERLAPPING, COVERING).contains(aCondition));

          softly.assertThat(aPredicate.apply(begin, end, begin - 1, end + 1))
              .as("B begins and ends at A's boundries ([-###-])")
              .isEqualTo(asList(OVERLAPPING, COVERED_BY).contains(aCondition));
          
          softly.assertThat(aPredicate.apply(begin, end, begin, end))
              .as("B begins and ends at A's boundries ([###])")
              .isEqualTo(asList(OVERLAPPING, COLOCATED, COVERED_BY, COVERING).contains(aCondition));
        
          softly.assertThat(aPredicate.apply(begin, end, begin + 1, begin + 1))
              .as("Zero-width B within A (#|#)")
              .isEqualTo(asList(OVERLAPPING, COVERING).contains(aCondition));

          softly.assertThat(aPredicate.apply(begin, end, begin, end - 1))
              .as("B begins at and ends before A's boundries ([##]#)")
              .isEqualTo(asList(OVERLAPPING, COVERING).contains(aCondition));

          softly.assertThat(aPredicate.apply(begin, end, end - 1 , MAX_VALUE))
              .as("B begins before and ends within A (##[#--])")
              .isEqualTo(asList(OVERLAPPING, OVERLAPPING_LEFT).contains(aCondition));

          softly.assertThat(aPredicate.apply(begin, end, end, MAX_VALUE))
              .as("B begins at A's end and ends after A (###[---])")
              .isEqualTo(asList(LEFT_OF).contains(aCondition));

          softly.assertThat(aPredicate.apply(begin, end, end, end))
              .as("Zero-width B at A's end (###|)")
              .isEqualTo(asList(LEFT_OF).contains(aCondition));

          softly.assertThat(aPredicate.apply(begin, end, end + 1, MAX_VALUE))
              .as("B begins and ends after A (### [---])")
              .isEqualTo(asList(LEFT_OF).contains(aCondition));
        
          softly.assertThat(aPredicate.apply(begin, end, end + 1, end + 1))
              .as("Zero-width B after A's end (### |)")
              .isEqualTo(asList(LEFT_OF).contains(aCondition));
        
          begin = 10;
          end = 10;
          
          softly.assertThat(aPredicate.apply(begin, end, 20, 30))
              .as("Zero-width A before B start (# [---])")
              .isEqualTo(asList(LEFT_OF).contains(aCondition));
    
          softly.assertThat(aPredicate.apply(begin, end, 10, 20))
              .as("Zero-width A at B's start (#---])")
              .isEqualTo(asList(OVERLAPPING, COVERED_BY).contains(aCondition));
          
          softly.assertThat(aPredicate.apply(begin, end, 0, 10))
              .as("Zero-width A at B's end ([---#)")
              .isEqualTo(asList(RIGHT_OF).contains(aCondition));
    
          softly.assertThat(aPredicate.apply(begin, end, 10, 10))
              .as("Zero-width A matches zero-width B start/end (#)")
              .isEqualTo(asList(OVERLAPPING, COVERED_BY, COVERING, COLOCATED).contains(aCondition));
          
          softly.assertThat(aPredicate.apply(begin, end, 0, 5))
              .as("Zero-width A after B's end ([---] #)")
              .isEqualTo(asList(RIGHT_OF).contains(aCondition));
        }
    }
}