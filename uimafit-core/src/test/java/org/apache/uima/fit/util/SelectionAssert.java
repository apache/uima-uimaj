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
import static org.apache.uima.fit.util.SelectionAssert.RelativePosition.BEGINNING_WITH;
import static org.apache.uima.fit.util.SelectionAssert.RelativePosition.COLOCATED;
import static org.apache.uima.fit.util.SelectionAssert.RelativePosition.COVERED_BY;
import static org.apache.uima.fit.util.SelectionAssert.RelativePosition.COVERING;
import static org.apache.uima.fit.util.SelectionAssert.RelativePosition.ENDING_WITH;
import static org.apache.uima.fit.util.SelectionAssert.RelativePosition.FOLLOWING;
import static org.apache.uima.fit.util.SelectionAssert.RelativePosition.OVERLAPPING;
import static org.apache.uima.fit.util.SelectionAssert.RelativePosition.OVERLAPPING_AT_BEGIN;
import static org.apache.uima.fit.util.SelectionAssert.RelativePosition.OVERLAPPING_AT_END;
import static org.apache.uima.fit.util.SelectionAssert.RelativePosition.PRECEDING;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.assertj.core.api.AutoCloseableSoftAssertions;

public class SelectionAssert {
  public static enum RelativePosition {
    COLOCATED,
    OVERLAPPING,
    OVERLAPPING_AT_BEGIN,
    OVERLAPPING_AT_END,
    COVERING,
    COVERED_BY,
    PRECEDING,
    FOLLOWING,
    BEGINNING_WITH,
    ENDING_WITH
  }
  
  // Used as fixed references for the annotation relation cases.
  private static final int BEGIN = 10;
  private static final int END = 20;
  private static final int Z_POS = 10;

  public static final List<TestCase> NON_ZERO_WIDTH_TEST_CASES = asList(
      new TestCase("1) Y begins and ends after X (### [---])", 
          p -> p.apply(BEGIN, END, END + 1, MAX_VALUE),
          asList(PRECEDING)),
      new TestCase("2) Y begins at X's end and ends after X (###[---])", 
          p -> p.apply(BEGIN, END, END, MAX_VALUE),
          asList(PRECEDING)),
      new TestCase("3) Y begins within and ends after X (##[#--])", 
          p -> p.apply(BEGIN, END, END - 1 , MAX_VALUE),
          asList(OVERLAPPING, OVERLAPPING_AT_BEGIN)),
      new TestCase("4) Y begins and ends at X's boundries ([###])", 
          p -> p.apply(BEGIN, END, BEGIN, END),
          asList(OVERLAPPING, COLOCATED, COVERED_BY, COVERING, BEGINNING_WITH, ENDING_WITH)),
      new TestCase("5) Y begins and ends within X (#[#]#)", 
          p -> p.apply(BEGIN, END, BEGIN + 1, END - 1),
          asList(OVERLAPPING, COVERING)),
      new TestCase("6) Y begins at and ends before X's boundries ([##]#)", 
          p -> p.apply(BEGIN, END, BEGIN, END - 1),
          asList(OVERLAPPING, COVERING, BEGINNING_WITH)),
      new TestCase("7) Y begins after and ends at X's boundries (#[##])", 
          p -> p.apply(BEGIN, END, BEGIN + 1, END),
          asList(OVERLAPPING, COVERING, ENDING_WITH)),
      new TestCase("8) Y begins before and ends after X's boundries ([-###-])", 
          p -> p.apply(BEGIN, END, BEGIN - 1, END + 1),
          asList(OVERLAPPING, COVERED_BY)),
      new TestCase("9) X starts where Y begins and ends within Y ([##-])", 
          p -> p.apply(BEGIN, END, BEGIN, END + 1),
          asList(OVERLAPPING, COVERED_BY, BEGINNING_WITH)),
      new TestCase("10) X starts within Y and ends where Y ends ([-##])", 
          p -> p.apply(BEGIN, END, BEGIN - 1, END),
          asList(OVERLAPPING, COVERED_BY, ENDING_WITH)),
      new TestCase("11) Y begins before and ends within X ([--#]##)", 
          p -> p.apply(BEGIN, END, 0, BEGIN + 1),
          asList(OVERLAPPING, OVERLAPPING_AT_END)),
      new TestCase("12) Y begins before and ends where X begins ([---]###)", 
          p -> p.apply(BEGIN, END, 0, BEGIN),
          asList(FOLLOWING)),
      new TestCase("13) Y begins and ends before X begins ([---] ###)", 
          p -> p.apply(BEGIN, END, 0, BEGIN - 1),
          asList(FOLLOWING)));

  public static final List<TestCase> ZERO_WIDTH_TEST_CASES = asList(
      new TestCase("Z1) Zero-width X before Y start (# [---])", 
          p -> p.apply(Z_POS, Z_POS, Z_POS + 10, Z_POS + 20),
          asList(PRECEDING)),
      new TestCase("Z2) Zero-width Y after X's end (### |)", 
          p -> p.apply(BEGIN, END, END + 1, END + 1),
          asList(PRECEDING)),
      new TestCase("Z3) Zero-width X at Y's start (#---])", 
          p -> p.apply(Z_POS, Z_POS, Z_POS, Z_POS + 10),
          asList(OVERLAPPING, COVERED_BY, BEGINNING_WITH)),
      new TestCase("Z4) Zero-width X at Y's end ([---#)", 
          p -> p.apply(Z_POS, Z_POS, Z_POS-10, Z_POS),
          asList(OVERLAPPING, COVERED_BY, ENDING_WITH)),
      new TestCase("Z5) Zero-width Y where X begins (|###)", 
          p -> p.apply(BEGIN, END, BEGIN, BEGIN),
          asList(OVERLAPPING, COVERING, BEGINNING_WITH)),
      new TestCase("Z6) Zero-width Y within X (#|#)", 
          p -> p.apply(BEGIN, END, BEGIN + 1, BEGIN + 1),
          asList(OVERLAPPING, COVERING)),
      new TestCase("Z7) Zero-width Y at X's end (###|)", 
          p -> p.apply(BEGIN, END, END, END),
          asList(OVERLAPPING, COVERING, ENDING_WITH)),
      new TestCase("Z8) Zero-width X with Y (-|-)", 
          p -> p.apply(Z_POS, Z_POS, Z_POS - 5, Z_POS + 5),
          asList(OVERLAPPING, COVERED_BY)),
      new TestCase("Z9) Zero-width X after Y's end ([---] #)", 
          p -> p.apply(Z_POS, Z_POS, Z_POS - 10, Z_POS - 5),
          asList(FOLLOWING)),
      new TestCase("Z10) Zero-width Y before X begins (| ###)", 
          p -> p.apply(BEGIN, END, BEGIN - 1, BEGIN - 1),
          asList(FOLLOWING)),
      new TestCase("Z11) Zero-width X matches zero-width Y start/end (#)", 
          p -> p.apply(Z_POS, Z_POS, Z_POS, Z_POS),
          asList(OVERLAPPING, COVERED_BY, COVERING, COLOCATED, BEGINNING_WITH, ENDING_WITH)));
  
  public static void assertSelection(RelativePosition aCondition, RelativeAnnotationPredicate aPredicate, 
      List<TestCase> aTestCases)
      throws Exception {
    CAS cas = CasCreationUtils.createCas();
    Type type = cas.getAnnotationType();

    try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
      for (TestCase testCase : aTestCases) {
        cas.reset();

        // Create annotations
        Annotation x = (Annotation) cas.createAnnotation(type, 0, 0);
        Annotation y = (Annotation) cas.createAnnotation(type, 0, 0);

        // Position the annotations according to the test data
        testCase.getTest().apply((beginA, endA, beginB, endB) -> {
          x.setBegin(beginA);
          x.setEnd(endA);
          y.setBegin(beginB);
          y.setEnd(endB);
          cas.addFsToIndexes(x);
          cas.addFsToIndexes(y);
          return true;
        });

        softly.assertThat(aPredicate.apply(cas, type, x, y)).as(testCase.getDescription())
            .isEqualTo(testCase.getValidPositions().contains(aCondition));
      }
    }
  }

  public static void assertSelectionIsEqualOnRandomData(TypeByContextSelector aExpected, TypeByContextSelector aActual)
      throws Exception {
    final int ITERATIONS = 30;
    final int TYPES = 5;

    TypeSystemDescription tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
    
    Map<String, Type> types = new LinkedHashMap<>();
    for (int i = 0; i < TYPES; i++) {
      String typeName = "test.Type" + (i + 1);
      tsd.addType(typeName, "", CAS.TYPE_NAME_ANNOTATION);
      types.put(typeName, null);
    }
    
    CAS randomCas = CasCreationUtils.createCas(tsd, null, null, null);

    for (String typeName : types.keySet()) {
      types.put(typeName, randomCas.getTypeSystem().getType(typeName));
    }
    
    System.out.print("Iteration: ");
    try {
      Iterator<Type> ti = types.values().iterator();
      Type type1 = ti.next();
      Type type2 = ti.next();
      
      for (int i = 0; i < ITERATIONS; i++) {
        if (i % 10 == 0) {
          System.out.print(i);
        }
        else {
          System.out.print(".");
        }
  
        initRandomCas(randomCas, 3 * i, 0, types.values().toArray(new Type[types.size()]));
  
        for (Annotation context : randomCas.<Annotation>select(type1)) {
          List<AnnotationFS> expected = aExpected.select(randomCas, type2, context);
          List<AnnotationFS> actual = aActual.select(randomCas, type2, context);
  
          assertThat(actual)
              .as("Selected [%s] with context [%s]@[%d..%d]", type2.getShortName(), 
                  type1.getShortName(), context.getBegin(), context.getEnd())
              .containsExactlyElementsOf(expected);
        }
      }
      System.out.print(ITERATIONS);
    }
    finally {
      System.out.println();
    }
  }

  private static void initRandomCas(CAS aCas, int aSize, int aMinimumWidth, Type... aTypes) {
    Random rnd = new Random();

    List<Type> types = new ArrayList<>(asList(aTypes));

    // Shuffle the types
    for (int n = 0; n < 10; n++) {
      Type t = types.remove(rnd.nextInt(types.size()));
      types.add(t);
    }

    // Randomly generate annotations
    for (int n = 0; n < aSize; n++) {
      for (Type t : types) {
        int begin = rnd.nextInt(100);
        int end = begin + rnd.nextInt(30) + aMinimumWidth;
        aCas.addFsToIndexes(aCas.createAnnotation(t, begin, end));
      }
    }
  }

  @FunctionalInterface
  public static interface RelativeAnnotationPredicate {
    boolean apply(CAS cas, Type type, Annotation x, Annotation y);
  }

  @FunctionalInterface
  public static interface TypeByContextSelector {
    List<AnnotationFS> select(CAS aCas, Type aType, Annotation aContext);
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
