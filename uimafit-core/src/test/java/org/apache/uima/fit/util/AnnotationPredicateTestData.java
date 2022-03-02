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
import static org.apache.uima.fit.util.AnnotationPredicateTestData.RelativePosition.BEGINNING_WITH;
import static org.apache.uima.fit.util.AnnotationPredicateTestData.RelativePosition.COLOCATED;
import static org.apache.uima.fit.util.AnnotationPredicateTestData.RelativePosition.COVERED_BY;
import static org.apache.uima.fit.util.AnnotationPredicateTestData.RelativePosition.COVERING;
import static org.apache.uima.fit.util.AnnotationPredicateTestData.RelativePosition.ENDING_WITH;
import static org.apache.uima.fit.util.AnnotationPredicateTestData.RelativePosition.FOLLOWING;
import static org.apache.uima.fit.util.AnnotationPredicateTestData.RelativePosition.OVERLAPPING;
import static org.apache.uima.fit.util.AnnotationPredicateTestData.RelativePosition.OVERLAPPING_AT_BEGIN;
import static org.apache.uima.fit.util.AnnotationPredicateTestData.RelativePosition.OVERLAPPING_AT_END;
import static org.apache.uima.fit.util.AnnotationPredicateTestData.RelativePosition.PRECEDING;

import java.util.List;

import org.apache.uima.fit.util.SelectionAssert.TestCase;

public class AnnotationPredicateTestData {
  public static enum RelativePosition {
    COLOCATED, OVERLAPPING, OVERLAPPING_AT_BEGIN, OVERLAPPING_AT_END, COVERING, COVERED_BY, PRECEDING, FOLLOWING, BEGINNING_WITH, ENDING_WITH
  }

  // Used as fixed references for the annotation relation cases.
  private static final int BEGIN = 10;
  private static final int END = 20;
  private static final int Z_POS = 10;

  public static final TestCase T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13;

  public static final List<TestCase> NON_ZERO_WIDTH_TEST_CASES = asList(
          T1 = new TestCase("1) Y begins and ends after X (### [---])",
                  p -> p.apply(BEGIN, END, END + 1, MAX_VALUE), asList(PRECEDING)),
          T2 = new TestCase("2) Y begins at X's end and ends after X (###[---])",
                  p -> p.apply(BEGIN, END, END, MAX_VALUE), asList(PRECEDING)),
          T3 = new TestCase("3) Y begins within and ends after X (##[#--])",
                  p -> p.apply(BEGIN, END, END - 1, MAX_VALUE),
                  asList(OVERLAPPING, OVERLAPPING_AT_BEGIN)),
          T4 = new TestCase("4) Y begins and ends at X's boundries ([###])",
                  p -> p.apply(BEGIN, END, BEGIN, END),
                  asList(OVERLAPPING, COLOCATED, COVERED_BY, COVERING, BEGINNING_WITH,
                          ENDING_WITH)),
          T5 = new TestCase("5) Y begins and ends within X (#[#]#)",
                  p -> p.apply(BEGIN, END, BEGIN + 1, END - 1), asList(OVERLAPPING, COVERING)),
          T6 = new TestCase("6) Y begins at and ends before X's boundries ([##]#)",
                  p -> p.apply(BEGIN, END, BEGIN, END - 1),
                  asList(OVERLAPPING, COVERING, BEGINNING_WITH, OVERLAPPING_AT_END)),
          T7 = new TestCase("7) Y begins after and ends at X's boundries (#[##])",
                  p -> p.apply(BEGIN, END, BEGIN + 1, END),
                  asList(OVERLAPPING, COVERING, ENDING_WITH, OVERLAPPING_AT_BEGIN)),
          T8 = new TestCase("8) Y begins before and ends after X's boundries ([-###-])",
                  p -> p.apply(BEGIN, END, BEGIN - 1, END + 1), asList(OVERLAPPING, COVERED_BY)),
          T9 = new TestCase("9) X starts where Y begins and ends within Y ([##-])",
                  p -> p.apply(BEGIN, END, BEGIN, END + 1),
                  asList(OVERLAPPING, COVERED_BY, BEGINNING_WITH)),
          T10 = new TestCase("10) X starts within Y and ends where Y ends ([-##])",
                  p -> p.apply(BEGIN, END, BEGIN - 1, END),
                  asList(OVERLAPPING, COVERED_BY, ENDING_WITH)),
          T11 = new TestCase("11) Y begins before and ends within X ([--#]##)",
                  p -> p.apply(BEGIN, END, 0, BEGIN + 1), asList(OVERLAPPING, OVERLAPPING_AT_END)),
          T12 = new TestCase("12) Y begins before and ends where X begins ([---]###)",
                  p -> p.apply(BEGIN, END, 0, BEGIN), asList(FOLLOWING)),
          T13 = new TestCase("13) Y begins and ends before X begins ([---] ###)",
                  p -> p.apply(BEGIN, END, 0, BEGIN - 1), asList(FOLLOWING)));

  public static final TestCase TZ1, TZ2, TZ3, TZ4, TZ5, TZ6, TZ7, TZ8, TZ9, TZ10, TZ11;

  public static final List<TestCase> ZERO_WIDTH_TEST_CASES = asList(
          TZ1 = new TestCase("Z1) Zero-width X before Y start (# [---])",
                  p -> p.apply(Z_POS, Z_POS, Z_POS + 10, Z_POS + 20), asList(PRECEDING)),
          TZ2 = new TestCase("Z2) Zero-width Y after X's end (### |)",
                  p -> p.apply(BEGIN, END, END + 1, END + 1), asList(PRECEDING)),
          TZ3 = new TestCase("Z3) Zero-width X at Y's start (#---])",
                  p -> p.apply(Z_POS, Z_POS, Z_POS, Z_POS + 10),
                  asList(PRECEDING, OVERLAPPING, COVERED_BY, BEGINNING_WITH)),
          TZ4 = new TestCase("Z4) Zero-width X at Y's end ([---#)",
                  p -> p.apply(Z_POS, Z_POS, Z_POS - 10, Z_POS),
                  asList(FOLLOWING, OVERLAPPING, COVERED_BY, ENDING_WITH)),
          TZ5 = new TestCase("Z5) Zero-width Y where X begins (|###)",
                  p -> p.apply(BEGIN, END, BEGIN, BEGIN),
                  asList(FOLLOWING, OVERLAPPING, COVERING, BEGINNING_WITH)),
          TZ6 = new TestCase("Z6) Zero-width Y within X (#|#)",
                  p -> p.apply(BEGIN, END, BEGIN + 1, BEGIN + 1), asList(OVERLAPPING, COVERING)),
          TZ7 = new TestCase("Z7) Zero-width Y at X's end (###|)",
                  p -> p.apply(BEGIN, END, END, END),
                  asList(PRECEDING, OVERLAPPING, COVERING, ENDING_WITH)),
          TZ8 = new TestCase("Z8) Zero-width X with Y (-|-)",
                  p -> p.apply(Z_POS, Z_POS, Z_POS - 5, Z_POS + 5),
                  asList(OVERLAPPING, COVERED_BY)),
          TZ9 = new TestCase("Z9) Zero-width X after Y's end ([---] #)",
                  p -> p.apply(Z_POS, Z_POS, Z_POS - 10, Z_POS - 5), asList(FOLLOWING)),
          TZ10 = new TestCase("Z10) Zero-width Y before X begins (| ###)",
                  p -> p.apply(BEGIN, END, BEGIN - 1, BEGIN - 1), asList(FOLLOWING)),
          TZ11 = new TestCase("Z11) Zero-width X matches zero-width Y start/end (#)",
                  p -> p.apply(Z_POS, Z_POS, Z_POS, Z_POS),
                  asList(FOLLOWING, PRECEDING, OVERLAPPING, COVERED_BY, COVERING, COLOCATED,
                          BEGINNING_WITH, ENDING_WITH)));
}
