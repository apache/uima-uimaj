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

import static java.util.Arrays.asList;
import static org.apache.uima.cas.text.AnnotationPredicateAssert.assertPosition;
import static org.apache.uima.cas.text.AnnotationPredicateAssert.toRelativePositionPredicate1;
import static org.apache.uima.cas.text.AnnotationPredicateAssert.toRelativePositionPredicate1Inverse;
import static org.apache.uima.cas.text.AnnotationPredicateAssert.toRelativePositionPredicate2;
import static org.apache.uima.cas.text.AnnotationPredicateAssert.toRelativePositionPredicate2Inverse;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.NON_ZERO_WIDTH_TEST_CASES;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.ZERO_WIDTH_TEST_CASES;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.BEGINNING_WITH;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.COLOCATED;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.COVERED_BY;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.COVERING;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.ENDING_WITH;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.FOLLOWING;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.OVERLAPPING;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.OVERLAPPING_AT_BEGIN;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.OVERLAPPING_AT_END;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.PRECEDING;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationPredicateAssert.TestCase;
import org.apache.uima.util.CasCreationUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AnnotationPredicatesTest {
  private static CAS cas;
  private SoftAssertions softly;
  private List<TestCase> testCases = union(NON_ZERO_WIDTH_TEST_CASES, ZERO_WIDTH_TEST_CASES);

  @BeforeAll
  static void setupClass() throws Exception {
    cas = CasCreationUtils.createCas();
  }

  @BeforeEach
  void setup() throws Exception {
    cas.reset();
    softly = new SoftAssertions();
  }

  @AfterEach
  void teardown() {
    softly.assertAll();
  }

  @Test
  void thatCoveringWorks() {
    asList(AnnotationPredicates::covering,
            toRelativePositionPredicate1(cas, AnnotationPredicates::covering),
            toRelativePositionPredicate2(cas, AnnotationPredicates::covering))
                    .forEach(predicate -> assertPosition(softly, COVERING, predicate, testCases));
  }

  @Test
  void thatAxiomaticCoveringWorks() {
    assertPosition(softly, COVERING, AxiomaticAnnotationPredicates::covering, testCases);
  }

  @Test
  void thatCoveredByWorks() {
    asList(AnnotationPredicates::coveredBy,
            toRelativePositionPredicate1(cas, AnnotationPredicates::coveredBy),
            toRelativePositionPredicate2(cas, AnnotationPredicates::coveredBy))
                    .forEach(predicate -> assertPosition(softly, COVERED_BY, predicate, testCases));
  }

  @Test
  void thatAxiomaticCoveredByWorks() {
    assertPosition(softly, COVERED_BY, AxiomaticAnnotationPredicates::coveredBy, testCases);
  }

  @Test
  void thatColocatedWorks() {
    asList(AnnotationPredicates::colocated,
            toRelativePositionPredicate1(cas, AnnotationPredicates::colocated),
            toRelativePositionPredicate1Inverse(cas, AnnotationPredicates::colocated),
            toRelativePositionPredicate2(cas, AnnotationPredicates::colocated),
            toRelativePositionPredicate2Inverse(cas, AnnotationPredicates::colocated))
                    .forEach(predicate -> assertPosition(softly, COLOCATED, predicate, testCases));
  }

  @Test
  void thatAxiomaticColocatedWorks() {
    assertPosition(softly, COLOCATED, AxiomaticAnnotationPredicates::colocated, testCases);
  }

  @Test
  void thatOverlappingAtBeginWorks() {
    asList(AnnotationPredicates::overlappingAtBegin,
            toRelativePositionPredicate1(cas, AnnotationPredicates::overlappingAtBegin),
            toRelativePositionPredicate2(cas, AnnotationPredicates::overlappingAtBegin))
                    .forEach(predicate -> assertPosition(softly, OVERLAPPING_AT_BEGIN, predicate,
                            testCases));
  }

  @Test
  void thatAxiomaticOverlappingAtBeginWorks() {
    assertPosition(softly, OVERLAPPING_AT_BEGIN, AxiomaticAnnotationPredicates::overlappingAtBegin,
            testCases);
  }

  @Test
  void thatOverlappingAtEndWorks() {
    asList(AnnotationPredicates::overlappingAtEnd,
            toRelativePositionPredicate1(cas, AnnotationPredicates::overlappingAtEnd),
            toRelativePositionPredicate2(cas, AnnotationPredicates::overlappingAtEnd)).forEach(
                    predicate -> assertPosition(softly, OVERLAPPING_AT_END, predicate, testCases));
  }

  @Test
  void thatAxiomaticOverlappingAtEndWorks() {
    assertPosition(softly, OVERLAPPING_AT_END, AxiomaticAnnotationPredicates::overlappingAtEnd,
            testCases);
  }

  @Test
  void thatOverlappingWorks() {
    asList(AnnotationPredicates::overlapping,
            toRelativePositionPredicate1(cas, AnnotationPredicates::overlapping),
            toRelativePositionPredicate1Inverse(cas, AnnotationPredicates::overlapping),
            toRelativePositionPredicate2(cas, AnnotationPredicates::overlapping),
            toRelativePositionPredicate2Inverse(cas, AnnotationPredicates::overlapping)).forEach(
                    predicate -> assertPosition(softly, OVERLAPPING, predicate, testCases));
  }

  @Test
  void thatAxiomaticOverlappingWorks() {
    assertPosition(softly, OVERLAPPING, AxiomaticAnnotationPredicates::overlapping, testCases);
  }

  @Test
  void thatPrecedingWorks() {
    asList(AnnotationPredicates::preceding,
            toRelativePositionPredicate1(cas, AnnotationPredicates::preceding),
            toRelativePositionPredicate2(cas, AnnotationPredicates::preceding))
                    .forEach(predicate -> assertPosition(softly, PRECEDING, predicate, testCases));
  }

  @Test
  void thatAxiomaticPrecedingWorks() {
    assertPosition(softly, PRECEDING, AxiomaticAnnotationPredicates::preceding, testCases);
  }

  @Test
  void thatFollowingWorks() {
    asList(AnnotationPredicates::following,
            toRelativePositionPredicate1(cas, AnnotationPredicates::following),
            toRelativePositionPredicate2(cas, AnnotationPredicates::following))
                    .forEach(predicate -> assertPosition(softly, FOLLOWING, predicate, testCases));
  }

  @Test
  void thatAxiomaticFollowingWorks() {
    assertPosition(softly, FOLLOWING, AxiomaticAnnotationPredicates::following, testCases);
  }

  @Test
  void thatBeginningWithWorks() {
    asList(AnnotationPredicates::beginningWith,
            toRelativePositionPredicate1(cas, AnnotationPredicates::beginningWith),
            toRelativePositionPredicate2(cas, AnnotationPredicates::beginningWith)).forEach(
                    predicate -> assertPosition(softly, BEGINNING_WITH, predicate, testCases));
  }

  @Test
  void thatAxiomaticBeginningWithWorks() {
    assertPosition(softly, BEGINNING_WITH, AxiomaticAnnotationPredicates::beginningWith, testCases);
  }

  @Test
  void thatEndingWithWorks() {
    asList(AnnotationPredicates::endingWith,
            toRelativePositionPredicate1(cas, AnnotationPredicates::endingWith),
            toRelativePositionPredicate2(cas, AnnotationPredicates::endingWith)).forEach(
                    predicate -> assertPosition(softly, ENDING_WITH, predicate, testCases));
  }

  @Test
  void thatAxiomaticEndingWithWorks() {
    assertPosition(softly, ENDING_WITH, AxiomaticAnnotationPredicates::endingWith, testCases);
  }

  @SafeVarargs
  static <T> List<T> union(List<T>... aLists) {
    List<T> all = new ArrayList<>();
    for (List<T> list : aLists) {
      all.addAll(list);
    }
    return all;
  }
}