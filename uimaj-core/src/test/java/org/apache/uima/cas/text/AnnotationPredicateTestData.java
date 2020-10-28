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
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.COLOCATED;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.COVERED_BY;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.COVERING;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.LEFT_OF;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.OVERLAPPING;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.OVERLAPPING_LEFT;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.OVERLAPPING_RIGHT;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.RIGHT_OF;

import java.util.List;

import org.apache.uima.cas.text.AnnotationPredicateAssert.TestCase;

public class AnnotationPredicateTestData {
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

  public static final List<TestCase> NON_ZERO_WIDTH_TEST_CASES = asList(
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
          asList(RIGHT_OF)));

  public static final List<TestCase> ZERO_WIDTH_TEST_CASES = asList(
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
          asList(OVERLAPPING, COVERED_BY)),
      new TestCase("Z5) Zero-width Y where X begins (|###)", 
          p -> p.apply(BEGIN, END, BEGIN, BEGIN),
          asList(OVERLAPPING, COVERING)),
      new TestCase("Z6) Zero-width Y within X (#|#)", 
          p -> p.apply(BEGIN, END, BEGIN + 1, BEGIN + 1),
          asList(OVERLAPPING, COVERING)),
      new TestCase("Z7) Zero-width Y at X's end (###|)", 
          p -> p.apply(BEGIN, END, END, END),
          asList(OVERLAPPING, COVERING)),
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
}
