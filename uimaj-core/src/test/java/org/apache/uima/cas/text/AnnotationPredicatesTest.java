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
public class AnnotationPredicatesTest {
  private static CAS cas;
  private SoftAssertions softly;
  private List<TestCase> testCases = union(NON_ZERO_WIDTH_TEST_CASES, ZERO_WIDTH_TEST_CASES);

  @BeforeAll
  public static void setupClass() throws Exception {
    cas = CasCreationUtils.createCas();
  }

  @BeforeEach
  public void setup() throws Exception {
    cas.reset();
    softly = new SoftAssertions();
  }

  @AfterEach
  public void teardown() throws Exception {
    softly.assertAll();
  }

  @Test
  public void thatCoveringWorks() throws Exception {
    asList(AnnotationPredicates::covering,
            toRelativePositionPredicate1(cas, AnnotationPredicates::covering),
            toRelativePositionPredicate2(cas, AnnotationPredicates::covering))
                    .forEach(predicate -> assertPosition(softly, COVERING, predicate, testCases));
  }

  @Test
  public void thatAxiomaticCoveringWorks() throws Exception {
    assertPosition(softly, COVERING, AxiomaticAnnotationPredicates::covering, testCases);
  }

  @Test
  public void thatCoveredByWorks() throws Exception {
    asList(AnnotationPredicates::coveredBy,
            toRelativePositionPredicate1(cas, AnnotationPredicates::coveredBy),
            toRelativePositionPredicate2(cas, AnnotationPredicates::coveredBy))
                    .forEach(predicate -> assertPosition(softly, COVERED_BY, predicate, testCases));
  }

  @Test
  public void thatAxiomaticCoveredByWorks() throws Exception {
    assertPosition(softly, COVERED_BY, AxiomaticAnnotationPredicates::coveredBy, testCases);
  }

  @Test
  public void thatColocatedWorks() throws Exception {
    asList(AnnotationPredicates::colocated,
            toRelativePositionPredicate1(cas, AnnotationPredicates::colocated),
            toRelativePositionPredicate1Inverse(cas, AnnotationPredicates::colocated),
            toRelativePositionPredicate2(cas, AnnotationPredicates::colocated),
            toRelativePositionPredicate2Inverse(cas, AnnotationPredicates::colocated))
                    .forEach(predicate -> assertPosition(softly, COLOCATED, predicate, testCases));
  }

  @Test
  public void thatAxiomaticColocatedWorks() throws Exception {
    assertPosition(softly, COLOCATED, AxiomaticAnnotationPredicates::colocated, testCases);
  }

  @Test
  public void thatOverlappingAtBeginWorks() throws Exception {
    asList(AnnotationPredicates::overlappingAtBegin,
            toRelativePositionPredicate1(cas, AnnotationPredicates::overlappingAtBegin),
            toRelativePositionPredicate2(cas, AnnotationPredicates::overlappingAtBegin))
                    .forEach(predicate -> assertPosition(softly, OVERLAPPING_AT_BEGIN, predicate,
                            testCases));
  }

  @Test
  public void thatAxiomaticOverlappingAtBeginWorks() throws Exception {
    assertPosition(softly, OVERLAPPING_AT_BEGIN, AxiomaticAnnotationPredicates::overlappingAtBegin,
            testCases);
  }

  @Test
  public void thatOverlappingAtEndWorks() throws Exception {
    asList(AnnotationPredicates::overlappingAtEnd,
            toRelativePositionPredicate1(cas, AnnotationPredicates::overlappingAtEnd),
            toRelativePositionPredicate2(cas, AnnotationPredicates::overlappingAtEnd)).forEach(
                    predicate -> assertPosition(softly, OVERLAPPING_AT_END, predicate, testCases));
  }

  @Test
  public void thatAxiomaticOverlappingAtEndWorks() throws Exception {
    assertPosition(softly, OVERLAPPING_AT_END, AxiomaticAnnotationPredicates::overlappingAtEnd,
            testCases);
  }

  @Test
  public void thatOverlappingWorks() throws Exception {
    asList(AnnotationPredicates::overlapping,
            toRelativePositionPredicate1(cas, AnnotationPredicates::overlapping),
            toRelativePositionPredicate1Inverse(cas, AnnotationPredicates::overlapping),
            toRelativePositionPredicate2(cas, AnnotationPredicates::overlapping),
            toRelativePositionPredicate2Inverse(cas, AnnotationPredicates::overlapping)).forEach(
                    predicate -> assertPosition(softly, OVERLAPPING, predicate, testCases));
  }

  @Test
  public void thatAxiomaticOverlappingWorks() throws Exception {
    assertPosition(softly, OVERLAPPING, AxiomaticAnnotationPredicates::overlapping, testCases);
  }

  @Test
  public void thatPrecedingWorks() throws Exception {
    asList(AnnotationPredicates::preceding,
            toRelativePositionPredicate1(cas, AnnotationPredicates::preceding),
            toRelativePositionPredicate2(cas, AnnotationPredicates::preceding))
                    .forEach(predicate -> assertPosition(softly, PRECEDING, predicate, testCases));
  }

  @Test
  public void thatAxiomaticPrecedingWorks() throws Exception {
    assertPosition(softly, PRECEDING, AxiomaticAnnotationPredicates::preceding, testCases);
  }

  @Test
  public void thatFollowingWorks() throws Exception {
    asList(AnnotationPredicates::following,
            toRelativePositionPredicate1(cas, AnnotationPredicates::following),
            toRelativePositionPredicate2(cas, AnnotationPredicates::following))
                    .forEach(predicate -> assertPosition(softly, FOLLOWING, predicate, testCases));
  }

  @Test
  public void thatAxiomaticFollowingWorks() throws Exception {
    assertPosition(softly, FOLLOWING, AxiomaticAnnotationPredicates::following, testCases);
  }

  @Test
  public void thatBeginningWithWorks() throws Exception {
    asList(AnnotationPredicates::beginningWith,
            toRelativePositionPredicate1(cas, AnnotationPredicates::beginningWith),
            toRelativePositionPredicate2(cas, AnnotationPredicates::beginningWith)).forEach(
                    predicate -> assertPosition(softly, BEGINNING_WITH, predicate, testCases));
  }

  @Test
  public void thatAxiomaticBeginningWithWorks() throws Exception {
    assertPosition(softly, BEGINNING_WITH, AxiomaticAnnotationPredicates::beginningWith, testCases);
  }

  @Test
  public void thatEndingWithWorks() throws Exception {
    asList(AnnotationPredicates::endingWith,
            toRelativePositionPredicate1(cas, AnnotationPredicates::endingWith),
            toRelativePositionPredicate2(cas, AnnotationPredicates::endingWith)).forEach(
                    predicate -> assertPosition(softly, ENDING_WITH, predicate, testCases));
  }

  @Test
  public void thatAxiomaticEndingWithWorks() throws Exception {
    assertPosition(softly, ENDING_WITH, AxiomaticAnnotationPredicates::endingWith, testCases);
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