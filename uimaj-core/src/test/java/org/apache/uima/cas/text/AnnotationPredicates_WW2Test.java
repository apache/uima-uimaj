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
import static org.apache.uima.cas.text.AnnotationPredicateTestData.NON_ZERO_WIDTH_WIDE_NARROW_TEST_CASES;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.UNAMBIGUOUS_NON_ZERO_WIDTH_TEST_CASES;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.UNAMBIGUOUS_ZERO_WIDTH_TEST_CASES;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.ZERO_WIDTH_WIDE_WIDE_TEST_CASES_2;
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
public class AnnotationPredicates_WW2Test {
    private static CAS cas;
    private AnnotationPredicates_WW2 sut;
    private SoftAssertions softly;
    private List<TestCase> testCases = union(
        UNAMBIGUOUS_NON_ZERO_WIDTH_TEST_CASES, 
        NON_ZERO_WIDTH_WIDE_NARROW_TEST_CASES, 
        UNAMBIGUOUS_ZERO_WIDTH_TEST_CASES,
        ZERO_WIDTH_WIDE_WIDE_TEST_CASES_2);

    @BeforeClass
    public static void setupClass() throws Exception {
      cas = CasCreationUtils.createCas();
    }

    @Before
    public void setup() throws Exception {
      cas.reset();
      softly = new SoftAssertions();
      sut = new AnnotationPredicates_WW2();
    }
    
    @After 
    public void teardown() throws Exception {
      softly.assertAll();
    }
    
    @Test
    public void thatCoveringWorks() throws Exception {
      asList(
          sut::covers,
          toRelativePositionPredicate1(cas, sut::covers),
          toRelativePositionPredicate2(cas, sut::covers))
      .forEach(predicate -> assertPosition(softly, COVERING, predicate, testCases));
      
    }

    @Test
    public void thatCoveredByWorks() throws Exception {
      asList(
          sut::coveredBy,
          toRelativePositionPredicate1(cas, sut::coveredBy),
          toRelativePositionPredicate2(cas, sut::coveredBy))
      .forEach(predicate -> assertPosition(softly, COVERED_BY, predicate, testCases));
    }

    @Test
    public void thatColocatedWorks() throws Exception {
      asList(
          sut::colocated,
          toRelativePositionPredicate1(cas, sut::colocated),
          toRelativePositionPredicate1Inverse(cas, sut::colocated),
          toRelativePositionPredicate2(cas, sut::colocated),
        toRelativePositionPredicate2Inverse(cas, sut::colocated))
      .forEach(predicate -> assertPosition(softly, COLOCATED, predicate, testCases));
    }

    @Test
    public void thatOverlapsLeftWorks() throws Exception {
      asList(
          sut::overlapsLeft,
          toRelativePositionPredicate1(cas, sut::overlapsLeft),
          toRelativePositionPredicate2(cas, sut::overlapsLeft))
      .forEach(predicate -> assertPosition(softly, OVERLAPPING_LEFT, predicate, testCases));
    }

    @Test
    public void thatOverlapsRightWorks() throws Exception {
      asList(
          sut::overlapsRight,
          toRelativePositionPredicate1(cas, sut::overlapsRight),
          toRelativePositionPredicate2(cas, sut::overlapsRight))
      .forEach(predicate -> assertPosition(softly, OVERLAPPING_RIGHT, predicate, testCases));
    }

    @Test
    public void thatOverlapWorks() throws Exception {
      asList(
          sut::overlaps,
          toRelativePositionPredicate1(cas, sut::overlaps),
          toRelativePositionPredicate1Inverse(cas, sut::overlaps),
          toRelativePositionPredicate2(cas, sut::overlaps),
          toRelativePositionPredicate2Inverse(cas, sut::overlaps))
      .forEach(predicate -> assertPosition(softly, OVERLAPPING, predicate, testCases));
    }

    @Test
    public void thatLeftOfWorks() throws Exception {
      asList(
          sut::leftOf,
          toRelativePositionPredicate1(cas, sut::leftOf),
          toRelativePositionPredicate2(cas, sut::leftOf))
      .forEach(predicate -> assertPosition(softly, LEFT_OF, predicate, testCases));
    }

    @Test
    public void thatRightOfWorks() throws Exception {
      asList(
          sut::rightOf,
          toRelativePositionPredicate1(cas, sut::rightOf),
          toRelativePositionPredicate2(cas, sut::rightOf))
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