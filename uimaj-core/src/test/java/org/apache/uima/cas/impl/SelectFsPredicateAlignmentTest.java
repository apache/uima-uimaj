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
import static org.apache.uima.cas.text.AnnotationPredicates.overlappingAtEnd;
import static org.apache.uima.cas.text.AnnotationPredicates.preceding;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.text.AnnotationPredicateAssert.TestCase;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

//Sorting only to keep the list in Eclipse ordered so it is easier spot if related tests fail
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SelectFsPredicateAlignmentTest {
  private List<TestCase> defaultPredicatesTestCases = union(NON_ZERO_WIDTH_TEST_CASES, ZERO_WIDTH_TEST_CASES);
  
  private static final int DEFAULT_ITERATIONS = 30;
  private static final int DEFAULT_TYPE_COUNT = 10;
  
  @Test
  public void thatSelectFollowingAlignsWithPrecedingPredicate() throws Exception {
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
  public void thatSelectPrecedingAlignsWithPrecedingPredicateOnCasRandomized() throws Exception
  {
    System.out.print("Preceding (CAS select by annotation)   -- ");
    assertSelectionIsEqualOnRandomData("preceding", DEFAULT_ITERATIONS, DEFAULT_TYPE_COUNT,
        (cas, type, y) -> cas.getAnnotationIndex(type).select()
            .filter(x -> x != y && preceding(x, y))
            .collect(toList()),
        (cas, type, y) -> cas.<Annotation>select(type)
            .preceding(y));
  }

  @Test
  public void thatSelectPrecedingAlignsWithPrecedingPredicateOnIndexRandomized() throws Exception
  {
    System.out.print("Preceding (Index select by annotation) -- ");
    assertSelectionIsEqualOnRandomData("preceding", DEFAULT_ITERATIONS, DEFAULT_TYPE_COUNT,
        (cas, type, y) -> cas.getAnnotationIndex(type).select()
            .filter(x -> x != y && preceding(x, y))
            .collect(toList()),
        (cas, type, y) -> cas.<Annotation>getAnnotationIndex(type).select()
            .preceding(y));
  }

  @Test
  public void thatSelectPrecedingAlignsWithFollowingPredicate() throws Exception {
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
  public void thatSelectFollowingAlignsWithFollowingPredicateOnCasRandomized() throws Exception
  {
    System.out.print("Following (CAS select by annotation)   -- ");
    assertSelectionIsEqualOnRandomData("following", DEFAULT_ITERATIONS, DEFAULT_TYPE_COUNT,
        (cas, type, y) -> cas.getAnnotationIndex(type).select()
            .filter(x -> x != y && following(x, y))
            .collect(toList()),
        (cas, type, y) -> cas.<Annotation>select(type)
            .following(y));
  }

  @Test
  public void thatSelectFollowingAlignsWithFollowingPredicateOnIndexRandomized() throws Exception
  {
    System.out.print("Following (Index select by annotation) -- ");
    assertSelectionIsEqualOnRandomData("following", DEFAULT_ITERATIONS, DEFAULT_TYPE_COUNT,
        (cas, type, y) -> cas.getAnnotationIndex(type).select()
            .filter(x -> x != y && following(x, y))
            .collect(toList()),
        (cas, type, y) -> cas.<Annotation>getAnnotationIndex(type).select()
            .following(y));
  }

  @Test
  public void thatSelectCoveringAlignsWithCoveredByPredicate() throws Exception {
    // X covered by Y means that Y is covering X, so we need to select the covering annotations
    // below.
    assertSelectFS(
        COVERED_BY,
        (cas, type, x, y) -> cas.select(type).covering(x).asList().contains(y),
        defaultPredicatesTestCases);
    
    assertSelectFS(
        COVERED_BY,
        (cas, type, x, y) -> cas.select(type).covering(x.getBegin(), x.getEnd()).asList().contains(y),
        defaultPredicatesTestCases);
    
    assertSelectFS(
        COVERED_BY,
        (cas, type, x, y) -> cas.select(type).filter((a) -> 
                coveredBy(x, (Annotation) a)).collect(toList()).contains(y),
        defaultPredicatesTestCases);
  }
  
  @Test
  public void thatSelectCoveredByAlignsWithCoveredByPredicateOnCasRandomized() throws Exception
  {
    System.out.print("CoveredBy (CAS select by annotation)   -- ");
    assertSelectionIsEqualOnRandomData("covered by", DEFAULT_ITERATIONS, DEFAULT_TYPE_COUNT,
        (cas, type, y) -> cas.<Annotation>getAnnotationIndex(type).select()
            .filter(x -> x != y && coveredBy(x, y))
            .collect(toList()),
        (cas, type, y) -> cas.<Annotation>select(type)
            .coveredBy(y));

    System.out.print("CoveredBy (CAS select by offsets)      -- ");
    assertSelectionIsEqualOnRandomData("covered by", DEFAULT_ITERATIONS, DEFAULT_TYPE_COUNT,
        (cas, type, y) -> cas.getAnnotationIndex(type).select()
            .filter(x -> coveredBy(x, y))
            .collect(toList()),
        (cas, type, y) -> cas.<Annotation>select(type)
            .coveredBy(y.getBegin(), y.getEnd()));
  }

  @Test
  public void thatSelectCoveredByAlignsWithCoveredByPredicateOnIndexRandomized() throws Exception
  {
    System.out.print("CoveredBy (Index select by annotation) -- ");
    assertSelectionIsEqualOnRandomData("covered by", DEFAULT_ITERATIONS, DEFAULT_TYPE_COUNT,
        (cas, type, y) -> cas.getAnnotationIndex(type).select()
            .filter(x -> x != y && coveredBy(x, y))
            .collect(toList()),
        (cas, type, y) -> cas.<Annotation>getAnnotationIndex(type).select()
            .coveredBy(y));

    System.out.print("CoveredBy (Index select by offsets)    -- ");
    assertSelectionIsEqualOnRandomData("covered by", DEFAULT_ITERATIONS, DEFAULT_TYPE_COUNT,
        (cas, type, y) -> cas.getAnnotationIndex(type).select()
            .filter(x -> coveredBy(x, y))
            .collect(toList()),
        (cas, type, y) -> cas.<Annotation>getAnnotationIndex(type).select()
            .coveredBy(y.getBegin(), y.getEnd()));
  }
  
  @Test
  public void thatSelectCoveredByAlignsWithCoveredByPredicateOnIndexRandomizedNonStrict() throws Exception
  {
    System.out.print("CoveredBy* (CAS select by annotation)  -- ");
    assertSelectionIsEqualOnRandomData("covered by (non-strict)", DEFAULT_ITERATIONS, DEFAULT_TYPE_COUNT,
        (cas, type, y) -> cas.getAnnotationIndex(type).select()
            .filter(x -> x != y && (coveredBy(x, y) || overlappingAtEnd(x, y)))
            .collect(toList()),
        (cas, type, y) -> cas.<Annotation>select(type)
            .coveredBy(y)
            .includeAnnotationsWithEndBeyondBounds());
  }  

  @Test
  public void thatSelectCoveredByAlignsWithCoveringPredicate() throws Exception {
    // X covering Y means that Y is covered by Y, so we need to select the covered by annotations
    // below.
    assertSelectFS(
        COVERING,
        (cas, type, x, y) -> cas.select(type).coveredBy(x).asList().contains(y),
        defaultPredicatesTestCases);

    assertSelectFS(
        COVERING,
        (cas, type, x, y) -> cas.select(type).coveredBy(x.getBegin(), x.getEnd()).asList().contains(y),
        defaultPredicatesTestCases);
    
    assertSelectFS(
        COVERING,
        (cas, type, x, y) -> cas.select(type).filter((a) -> 
                covering(x, (Annotation) a)).collect(toList()).contains(y),
        defaultPredicatesTestCases);
  }

  @Test
  public void thatSelectCoveringAlignsWithCoveringPredicateOnCasRandomized() throws Exception
  {
    System.out.print("Covering  (CAS select by annotation)   -- ");
    assertSelectionIsEqualOnRandomData("covering", DEFAULT_ITERATIONS, DEFAULT_TYPE_COUNT,
        (cas, type, y) -> cas.getAnnotationIndex(type).select()
            .filter(x -> x != y && covering(x, y))
            .collect(toList()),
        (cas, type, y) -> cas.<Annotation>select(type)
            .covering(y));
    
    System.out.print("Covering  (CAS select by offsets)      -- ");
    assertSelectionIsEqualOnRandomData("covering", DEFAULT_ITERATIONS, DEFAULT_TYPE_COUNT,
        (cas, type, y) -> cas.getAnnotationIndex(type).select()
            .filter(x -> covering(x, y))
            .collect(toList()),
        (cas, type, y) -> cas.<Annotation>select(type)
            .covering(y.getBegin(), y.getEnd()));
  }
  
  @Test
  public void thatSelectCoveringAlignsWithCoveringPredicateOnIndexRandomized() throws Exception
  {
    System.out.print("Covering  (Index select by annotation) -- ");
    assertSelectionIsEqualOnRandomData("covering", DEFAULT_ITERATIONS, DEFAULT_TYPE_COUNT,
        (cas, type, y) -> cas.getAnnotationIndex(type).select()
            .filter(x -> x != y && covering(x, y))
            .collect(toList()),
        (cas, type, y) -> cas.<Annotation>getAnnotationIndex(type).select()
            .covering(y));
    
    System.out.print("Covering  (Index select by offsets)    -- ");
    assertSelectionIsEqualOnRandomData("covering", DEFAULT_ITERATIONS, DEFAULT_TYPE_COUNT,
        (cas, type, y) -> cas.getAnnotationIndex(type).select()
            .filter(x -> covering(x, y))
            .collect(toList()),
        (cas, type, y) -> cas.<Annotation>getAnnotationIndex(type).select()
            .covering(y.getBegin(), y.getEnd()));
  }
  
  @Test
  public void thatSelectAtAlignsWithColocatedPredicate() throws Exception {
    // X covering Y means that Y is covered by Y, so we need to select the covered by annotations
    // below.
    assertSelectFS(
        COLOCATED,
        (cas, type, x, y) -> cas.select(type).at(x).asList().contains(y),
        defaultPredicatesTestCases);

    assertSelectFS(
        COLOCATED,
        (cas, type, x, y) -> cas.select(type).at(x.getBegin(), x.getEnd()).asList().contains(y),
        defaultPredicatesTestCases);

    assertSelectFS(
        COLOCATED,
        (cas, type, x, y) -> cas.select(type).filter((a) -> 
                colocated(x, (Annotation) a)).collect(toList()).contains(y),
        defaultPredicatesTestCases);
  }

  @Test
  public void thatSelectAtAlignsWithColocatedPredicatedOnCasRandomized() throws Exception
  {
    System.out.print("Colocated (CAS select by annotation)   -- ");
    assertSelectionIsEqualOnRandomData("colocated at", DEFAULT_ITERATIONS, DEFAULT_TYPE_COUNT,
        (cas, type, y) -> cas.getAnnotationIndex(type).select()
            .filter(x -> x != y && colocated(x, y))
            .collect(toList()),
        (cas, type, y) -> cas.<Annotation>select(type)
            .at(y));

    System.out.print("Colocated (CAS select by offsets)      -- ");
    assertSelectionIsEqualOnRandomData("colocated at", DEFAULT_ITERATIONS, DEFAULT_TYPE_COUNT,
        (cas, type, y) -> cas.getAnnotationIndex(type).select()
            .filter(x -> colocated(x, y))
            .collect(toList()),
        (cas, type, y) -> cas.<Annotation>select(type)
            .at(y.getBegin(), y.getEnd()));
  }

  @Test
  public void thatSelectAtAlignsWithColocatedPredicatedOnIndexRandomized() throws Exception
  {
    System.out.print("Colocated (Index select by annotation) -- ");
    assertSelectionIsEqualOnRandomData("colocated at", DEFAULT_ITERATIONS, DEFAULT_TYPE_COUNT,
        (cas, type, y) -> cas.getAnnotationIndex(type).select()
            .filter(x -> x != y && colocated(x, y))
            .collect(toList()),
        (cas, type, context) -> cas.<Annotation>getAnnotationIndex(type).select()
            .at(context));

    System.out.print("Colocated (Index select by offsets)    -- ");
    assertSelectionIsEqualOnRandomData("colocated at", DEFAULT_ITERATIONS, DEFAULT_TYPE_COUNT,
        (cas, type, y) -> cas.getAnnotationIndex(type).select()
            .filter(x -> colocated(x, y))
            .collect(toList()),
        (cas, type, y) -> cas.<Annotation>getAnnotationIndex(type).select()
            .at(y.getBegin(), y.getEnd()));
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
