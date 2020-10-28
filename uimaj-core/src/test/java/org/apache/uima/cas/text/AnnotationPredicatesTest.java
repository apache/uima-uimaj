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
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.COLOCATED;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.COVERED_BY;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.COVERING;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.LEFT_OF;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.OVERLAPPING;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.OVERLAPPING_LEFT;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.OVERLAPPING_RIGHT;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.RIGHT_OF;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationPredicateAssert.TestCase;
import org.apache.uima.util.CasCreationUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AnnotationPredicatesTest {
    private static CAS cas;
    private SoftAssertions softly;
    private List<TestCase> testCases = union(NON_ZERO_WIDTH_TEST_CASES, ZERO_WIDTH_TEST_CASES);

    @BeforeClass
    public static void setupClass() throws Exception {
      cas = CasCreationUtils.createCas();
    }

    @Before
    public void setup() throws Exception {
      cas.reset();
      softly = new SoftAssertions();
    }
    
    @After 
    public void teardown() throws Exception {
      softly.assertAll();
    }
    
    @Test
    public void thatCoveringWorks() throws Exception {
      asList(
          AnnotationPredicates::covers,
          toRelativePositionPredicate1(cas, AnnotationPredicates::covers),
          toRelativePositionPredicate2(cas, AnnotationPredicates::covers))
      .forEach(predicate -> assertPosition(softly, COVERING, predicate, testCases));
      
    }

    @Test
    public void thatCoveredByWorks() throws Exception {
      asList(
          AnnotationPredicates::coveredBy,
          toRelativePositionPredicate1(cas, AnnotationPredicates::coveredBy),
          toRelativePositionPredicate2(cas, AnnotationPredicates::coveredBy))
      .forEach(predicate -> assertPosition(softly, COVERED_BY, predicate, testCases));
    }

    @Test
    public void thatColocatedWorks() throws Exception {
      asList(
          AnnotationPredicates::colocated,
          toRelativePositionPredicate1(cas, AnnotationPredicates::colocated),
          toRelativePositionPredicate1Inverse(cas, AnnotationPredicates::colocated),
          toRelativePositionPredicate2(cas, AnnotationPredicates::colocated),
        toRelativePositionPredicate2Inverse(cas, AnnotationPredicates::colocated))
      .forEach(predicate -> assertPosition(softly, COLOCATED, predicate, testCases));
    }

    @Test
    public void thatOverlapsLeftWorks() throws Exception {
      asList(
          AnnotationPredicates::overlapsLeft,
          toRelativePositionPredicate1(cas, AnnotationPredicates::overlapsLeft),
          toRelativePositionPredicate2(cas, AnnotationPredicates::overlapsLeft))
      .forEach(predicate -> assertPosition(softly, OVERLAPPING_LEFT, predicate, testCases));
    }

    @Test
    public void thatOverlapsRightWorks() throws Exception {
      asList(
          AnnotationPredicates::overlapsRight,
          toRelativePositionPredicate1(cas, AnnotationPredicates::overlapsRight),
          toRelativePositionPredicate2(cas, AnnotationPredicates::overlapsRight))
      .forEach(predicate -> assertPosition(softly, OVERLAPPING_RIGHT, predicate, testCases));
    }

    @Test
    public void thatOverlapWorks() throws Exception {
      asList(
          AnnotationPredicates::overlaps,
          toRelativePositionPredicate1(cas, AnnotationPredicates::overlaps),
          toRelativePositionPredicate1Inverse(cas, AnnotationPredicates::overlaps),
          toRelativePositionPredicate2(cas, AnnotationPredicates::overlaps),
          toRelativePositionPredicate2Inverse(cas, AnnotationPredicates::overlaps))
      .forEach(predicate -> assertPosition(softly, OVERLAPPING, predicate, testCases));
    }

    @Test
    public void thatLeftOfWorks() throws Exception {
      asList(
          AnnotationPredicates::leftOf,
          toRelativePositionPredicate1(cas, AnnotationPredicates::leftOf),
          toRelativePositionPredicate2(cas, AnnotationPredicates::leftOf))
      .forEach(predicate -> assertPosition(softly, LEFT_OF, predicate, testCases));
    }

    @Test
    public void thatRightOfWorks() throws Exception {
      asList(
          AnnotationPredicates::rightOf,
          toRelativePositionPredicate1(cas, AnnotationPredicates::rightOf),
          toRelativePositionPredicate2(cas, AnnotationPredicates::rightOf))
      .forEach(predicate -> assertPosition(softly, RIGHT_OF, predicate, testCases));
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