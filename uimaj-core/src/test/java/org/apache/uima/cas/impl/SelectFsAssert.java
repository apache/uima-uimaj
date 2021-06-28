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

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.identityHashCode;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.COVERED_BY;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.FOLLOWING;
import static org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition.PRECEDING;
import static org.apache.uima.cas.text.AnnotationPredicates.overlapping;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.IntFunction;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.SelectFSs;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationPredicateAssert.TestCase;
import org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.assertj.core.api.AutoCloseableSoftAssertions;

public class SelectFsAssert {
  private static boolean logAnnotationCreation = false;

  public static void assertSelectFS(RelativePosition aCondition,
          RelativeAnnotationPredicate aPredicate, List<TestCase> aTestCases) throws Exception {
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

  public static void assertSelectionIsEqualOnRandomData(RelativePosition xRelToY,
          String description, int aIterations, int aTypes, TypeByContextSelector aExpected,
          TypeByContextSelectorAsSelection aActual) throws Exception {
    long lockedSeed = -1l;
    IntFunction<Integer> annotationsPerIteration = iteration -> iteration * 3;

    // ============================================================================================
    // Quick overrides for debugging
    //
    // 1) Normally you run tests with all of the lines below commented out
    // 2) If you get a failure, you run the single failing test commenting in the
    // iterations, annotationsPerIteration and types overrides. Adjust them until you get
    // a setup that fails with a minimal number of types / annotations.
    // 3) Note the RANDOM SEED logged to the console and put it into the lockedSeed variable here
    // and comment it in
    //
    // Most of the time, it should be possible to find scenario that fails with max 3 types and 3
    // annotations - but it might take a very long time to find such a scenario using the random
    // approach. So try higher numbers until you find something, then try lowering the number until
    // you are happy with the scenario size and then implement a unit test for the scenario. In the
    // unit test, you can then try removing annotations and/or types while still having the scenario
    // fail. Once you have a minimal setup, debug and fix it.
    //
    // The tests should be implemented in the SelectFsTest class.
    // --------------------------------------------------------------------------------------------
    // lockedSeed = 779033675811369l;
    // aIterations = 100_000;
    // annotationsPerIteration = iteration -> 2;
    // aTypes = 1;
    // ============================================================================================

    // Override settings when using a locked seed to be more debugging-friendly
    aIterations = lockedSeed != -1l ? 1 : aIterations;
    logAnnotationCreation = lockedSeed != -1;

    System.out.print("Iteration: ");
    try {
      Map<Integer, Integer> sizeCounts = new HashMap<>();
      Map<String, Long> timings = new LinkedHashMap<>();
      for (int i = 0; i < aIterations; i++) {
        long seed = lockedSeed != -1 ? lockedSeed : System.nanoTime();
        Random rnd = new Random(seed);
        if (i % 10 == 0) {
          System.out.print(i);
          if (i > 0 && i % 100 == 0) {
            System.out.println();
          }
        } else {
          System.out.print(".");
        }

        TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
        Map<String, Type> types = new LinkedHashMap<>();
        if (logAnnotationCreation) {
          System.out.println();
        }
        for (int ti = 0; ti < aTypes; ti++) {
          String typeName = "test.Type" + (ti + 1);
          TypeDescription newType;
          if (rnd.nextInt() % 2 == 0 || types.size() == 0) {
            newType = tsd.addType(typeName, "", CAS.TYPE_NAME_ANNOTATION);
          } else {
            newType = tsd.addType(typeName, "",
                    new ArrayList<>(types.keySet()).get(rnd.nextInt(types.size())));
          }

          if (logAnnotationCreation) {
            System.out.printf("tsd.addType(\"%s\", \"\", \"%s\");%n", newType.getName(),
                    newType.getSupertypeName());
          }

          types.put(typeName, null);
        }

        CAS randomCas = CasCreationUtils.createCas(tsd, null, null, null);

        for (String typeName : types.keySet()) {
          types.put(typeName, randomCas.getTypeSystem().getType(typeName));
        }

        Iterator<Type> ti = types.values().iterator();
        Type typeY = ti.next();
        Type typeX = ti.hasNext() ? ti.next() : typeY;
        Type[] typeList = types.values().toArray(new Type[types.size()]);

        randomCas.reset();

        initRandomCas(rnd, randomCas, annotationsPerIteration.apply(i), 0, typeList);

        for (Annotation y : randomCas.<Annotation> select(typeY)) {
          switch (rnd.nextInt(3)) {
            case 0:
              // Randomly use a non-indexed annotation for selection so we test both cases (using an
              // indexed or non-indexed annotation).
              y = (Annotation) randomCas.createAnnotation(typeList[rnd.nextInt(typeList.length)],
                      y.getBegin(), y.getEnd());
              break;
            case 1:
              // Randomly use a completely new annotation
              int begin = rnd.nextInt(100);
              int end = begin + rnd.nextInt(30);
              y = (Annotation) randomCas.createAnnotation(typeList[rnd.nextInt(typeList.length)],
                      begin, end);
            default:
              // Nothing to do
          }

          long tExpected = System.currentTimeMillis();
          List<Annotation> expected = aExpected.select(randomCas, typeX, y).stream()
                  .map(a -> (Annotation) a).collect(toList());
          timings.compute("filt. full scan",
                  (k, v) -> v == null ? 0l : v + currentTimeMillis() - tExpected);

          sizeCounts.compute(expected.size(), (k, v) -> v == null ? 1 : v++);

          try {
            assertSelectionAsList(expected, randomCas, aActual, xRelToY, description, typeX, typeY,
                    y, timings);

            assertInitialPositionIsFirstPosition(expected, randomCas, aActual, xRelToY, description,
                    typeX, typeY, y, timings);

            assertSelectionAsForwardIteration(expected, randomCas, aActual, xRelToY, description,
                    typeX, typeY, y, timings);

            assertSelectionAsBackwardIteration(expected, randomCas, aActual, xRelToY, description,
                    typeX, typeY, y, timings);

            // @formatter:off
            // Non-Overlapping | Limited | Backwards
            // false           | false   | false
            // @formatter:on
            assertSelectionAsRandomIteration(rnd, expected, randomCas, aActual, xRelToY,
                    description, typeX, typeY, y, timings);

            // @formatter:off
            // Non-Overlapping | Limited | Backwards | Shifted
            // false           | false   | false     | true
            // @formatter:on
            assertShiftedSelectionAsRandomIteration(rnd, expected, randomCas, aActual, xRelToY,
                    description, typeX, typeY, y, timings);

            // @formatter:off
            // Non-Overlapping | Limited | Backwards
            // false           | false   | true
            // @formatter:on
            assertBackwardsSelectionAsRandomIteration(rnd, expected, randomCas, aActual, xRelToY,
                    description, typeX, typeY, y, timings);

            // @formatter:off
            // Non-Overlapping | Limited | Backwards
            // false           | true    | false
            // @formatter:on
            assertLimitedSelectionAsRandomIteration(rnd, expected, randomCas, aActual, xRelToY,
                    description, typeX, typeY, y, timings);

            // @formatter:off
            // Non-Overlapping | Limited | Backwards
            // false           | true    | true
            // @formatter:on
            assertLimitedBackwardsSelectionAsRandomIteration(rnd, expected, randomCas, aActual,
                    xRelToY, description, typeX, typeY, y, timings);

            // @formatter:off
            // Non-Overlapping | Limited | Backwards | Shifted
            // false           | true    | true      | true
            // @formatter:on
            assertShiftedLimitedBackwardsSelectionAsRandomIteration(rnd, expected, randomCas,
                    aActual, xRelToY, description, typeX, typeY, y, timings);

            // @formatter:off
            // Non-Overlapping | Limited | Backwards
            // true            | false   | false
            // @formatter:on
            assertNonOverlappingSelectionAsRandomIteration(rnd, expected, randomCas, aActual,
                    xRelToY, description, typeX, typeY, y, timings);

            // @formatter:off
            // Non-Overlapping | Limited | Backwards
            // true            | false   | true
            // @formatter:on
            assertBackwardsNonOverlappingSelectionAsRandomIteration(rnd, expected, randomCas,
                    aActual, xRelToY, description, typeX, typeY, y, timings);

            // @formatter:off
            // Non-Overlapping | Limited | Backwards
            // true            | true    | false
            // @formatter:on
            assertLimitedNonOverlappingSelectionAsRandomIteration(rnd, expected, randomCas, aActual,
                    xRelToY, description, typeX, typeY, y, timings);

            // @formatter:off
            // Non-Overlapping | Limited | Backwards
            // true            | true    | true
            // @formatter:on
            assertLimitedBackwardsNonOverlappingSelectionAsRandomIteration(rnd, expected, randomCas,
                    aActual, xRelToY, description, typeX, typeY, y, timings);

            // @formatter:off
            // Non-Overlapping | Limited | Backwards | Shifted
            // true            | true    | true      | true
            // @formatter:on
            assertShiftedLimitedBackwardsNonOverlappingSelectionAsRandomIteration(rnd, expected,
                    randomCas, aActual, xRelToY, description, typeX, typeY, y, timings);
          } catch (Throwable e) {
            // Set a breakpoint here to halt when an assert above fails. The select triggering the
            // assert is then re-executed below and you can look into its details. To allow
            // stopping and re-executing the test, you need to put the displayed random seed into
            // the static variable RANDOM_SEED at the beginning of the file. Don't forget to
            // Comment this out again and to re-set the RANDOM_SEED to timer-based when you are
            // done with debugging.
            System.out.printf("RANDOM SEED: %d%n", seed);
            aActual.select(randomCas, typeX, y);
            throw e;
          }
        }
      }

      System.out.print(aIterations);
      System.out.print(
              timings.entrySet().stream().map(e -> format("%s: %4dms", e.getKey(), e.getValue()))
                      .collect(joining(" | ", " (", ")")));
    } finally {
      System.out.println();
    }
  }

  private static void assertSelectionAsList(List<Annotation> expected, CAS randomCas,
          TypeByContextSelectorAsSelection aActual, RelativePosition aXRelToY, String description,
          Type typeX, Type typeY, Annotation y, Map<String, Long> timings) {
    long t = System.currentTimeMillis();
    List<Annotation> listActual = aActual.select(randomCas, typeX, y).asList();
    timings.compute("asList", (k, v) -> v == null ? 0l : v + currentTimeMillis() - t);

    assertThat(listActual)
            .as("Selecting X of type [%s] %s [%s]@[%d-%d][%d] %s asList%n%s%n", typeX.getName(),
                    aXRelToY, y.getType().getShortName(), y.getBegin(), y.getEnd(),
                    identityHashCode(y), description, casToString(randomCas))
            .containsExactlyElementsOf(expected);
  }

  private static void assertInitialPositionIsFirstPosition(List<Annotation> expected, CAS randomCas,
          TypeByContextSelectorAsSelection aActual, RelativePosition aXRelToY, String description,
          Type typeX, Type typeY, Annotation y, Map<String, Long> timings) {
    FSIterator<Annotation> it = aActual.select(randomCas, typeX, y).fsIterator();
    Annotation initial = it.isValid() ? it.get() : null;
    it.moveToFirst();

    assertThat(it.isValid() ? it.get() : null).as(
            "Annotation pointed at by iterator initially should match annotation after calling "
                    + "moveToFirst:%n%s%n%s%n"
                    + "Selecting X of type [%s] %s%s [%s]@[%d-%d][%d] iterator forward%n%s%n",
            initial, it.isValid() ? it.get() : null, typeX.getName(), aXRelToY, description,
            y.getType().getShortName(), y.getBegin(), y.getEnd(), identityHashCode(y),
            casToString(randomCas)).isEqualTo(initial);
  }

  private static void assertSelectionAsForwardIteration(List<Annotation> expected, CAS randomCas,
          TypeByContextSelectorAsSelection aActual, RelativePosition aXRelToY, String description,
          Type typeX, Type typeY, Annotation y, Map<String, Long> timings) {

    List<Annotation> actual = new ArrayList<>();
    long t = System.currentTimeMillis();
    FSIterator<Annotation> it = aActual.select(randomCas, typeX, y).fsIterator();
    it.moveToFirst();
    while (it.isValid()) {
      actual.add(it.get());
      it.moveToNext();
    }
    timings.compute("it. >>", (k, v) -> v == null ? 0l : v + currentTimeMillis() - t);

    assertThat(actual)
            .as("Selecting X of type [%s] %s [%s]@[%d-%d][%d] %s iterator forward%n%s%n",
                    typeX.getName(), aXRelToY, y.getType().getShortName(), y.getBegin(), y.getEnd(),
                    identityHashCode(y), description, casToString(randomCas))
            .containsExactlyElementsOf(expected);
  }

  private static void assertSelectionAsBackwardIteration(List<Annotation> expected, CAS randomCas,
          TypeByContextSelectorAsSelection aActual, RelativePosition aXRelToY, String description,
          Type typeX, Type typeY, Annotation y, Map<String, Long> timings) {

    List<Annotation> actual = new ArrayList<>();
    long t = System.currentTimeMillis();
    FSIterator<Annotation> it = aActual.select(randomCas, typeX, y).fsIterator();
    it.moveToLast();
    while (it.isValid()) {
      actual.add(0, it.get());
      it.moveToPrevious();
    }
    timings.compute("it. <<", (k, v) -> v == null ? 0l : v + currentTimeMillis() - t);

    assertThat(actual)
            .as("Selecting X of type [%s] %s [%s]@[%d-%d][%s] %s iterator backwards%n%s%n",
                    typeX.getName(), aXRelToY, y.getType().getShortName(), y.getBegin(), y.getEnd(),
                    identityHashCode(y), description, casToString(randomCas))
            .containsExactlyElementsOf(expected);
  }

  private static void assertLimitedSelectionAsRandomIteration(Random rnd, List<Annotation> expected,
          CAS randomCas, TypeByContextSelectorAsSelection aActual, RelativePosition aXRelToY,
          String description, Type typeX, Type typeY, Annotation y, Map<String, Long> timings) {
    int limit = rnd.nextInt(5);

    List<Annotation> limitedExpected = limit(expected, limit, aXRelToY);

    assertSelectionAsRandomIteration(rnd, limitedExpected, randomCas,
            (cas, type, context) -> aActual.select(cas, type, context).limit(limit), aXRelToY,
            description + " with limit(" + limit + ")", typeX, typeY, y, timings);
  }

  private static void assertLimitedBackwardsSelectionAsRandomIteration(Random rnd,
          List<Annotation> expected, CAS randomCas, TypeByContextSelectorAsSelection aActual,
          RelativePosition aXRelToY, String description, Type typeX, Type typeY, Annotation y,
          Map<String, Long> timings) {

    int limit = rnd.nextInt(5);

    // FIXME: Actually... I am pretty sure that all selection types should use the same
    // precedence for limit/backwards...
    List<Annotation> adjustedExpectation;
    if (asList(FOLLOWING, PRECEDING).contains(aXRelToY)) {
      // This works with FOLLOWING / PRECEDING
      adjustedExpectation = limit(expected, limit, aXRelToY);
      adjustedExpectation = backwards(adjustedExpectation);
    } else {
      // This works with COVERED_BY, COVERING, COLOCATED
      adjustedExpectation = backwards(expected);
      adjustedExpectation = limit(adjustedExpectation, limit, aXRelToY);
    }

    assertSelectionAsRandomIteration(rnd, adjustedExpectation, randomCas,
            (cas, type, context) -> aActual.select(cas, type, context).limit(limit).backwards(),
            aXRelToY, description + " backwards with limit(" + limit + ")", typeX, typeY, y,
            timings);
  }

  private static void assertShiftedLimitedBackwardsSelectionAsRandomIteration(Random rnd,
          List<Annotation> expected, CAS randomCas, TypeByContextSelectorAsSelection aActual,
          RelativePosition aXRelToY, String description, Type typeX, Type typeY, Annotation y,
          Map<String, Long> timings) {

    // Random shift in the range of [-2, 2]
    int shift = rnd.nextInt(2) - rnd.nextInt(4);
    int limit = rnd.nextInt(5);

    // FIXME: Actually... I am pretty sure that all selection types should use the same
    // precedence for limit/backwards...
    List<Annotation> adjustedExpectation;
    if (asList(FOLLOWING, PRECEDING).contains(aXRelToY)) {
      // This works with FOLLOWING / PRECEDING
      adjustedExpectation = shifted(expected, shift, aXRelToY);
      adjustedExpectation = limit(adjustedExpectation, limit, aXRelToY);
      adjustedExpectation = backwards(adjustedExpectation);
    } else {
      // This works with COVERED_BY, COVERING, COLOCATED
      adjustedExpectation = backwards(expected);
      adjustedExpectation = shifted(adjustedExpectation, shift, aXRelToY);
      adjustedExpectation = limit(adjustedExpectation, limit, aXRelToY);
    }

    assertSelectionAsRandomIteration(rnd, adjustedExpectation, randomCas,
            (cas, type, context) -> aActual.select(cas, type, context).shifted(shift).limit(limit)
                    .backwards(),
            aXRelToY, description + " backwards with limit(" + limit + ") shifted(" + shift + ")",
            typeX, typeY, y, timings);
  }

  private static void assertLimitedNonOverlappingSelectionAsRandomIteration(Random rnd,
          List<Annotation> expected, CAS randomCas, TypeByContextSelectorAsSelection aActual,
          RelativePosition aXRelToY, String description, Type typeX, Type typeY, Annotation y,
          Map<String, Long> timings) {
    if (!nonOverlappingSupported(aXRelToY)) {
      return;
    }

    int limit = rnd.nextInt(5);

    List<Annotation> adjustedExpectation = unambiguous(expected);
    adjustedExpectation = limit(adjustedExpectation, limit, aXRelToY);

    assertSelectionAsRandomIteration(rnd, adjustedExpectation, randomCas,
            (cas, type, context) -> aActual.select(cas, type, context).nonOverlapping()
                    .limit(limit),
            aXRelToY, description + " non-overlapping with limit(" + limit + ")", typeX, typeY, y,
            timings);
  }

  private static void assertLimitedBackwardsNonOverlappingSelectionAsRandomIteration(Random rnd,
          List<Annotation> expected, CAS randomCas, TypeByContextSelectorAsSelection aActual,
          RelativePosition aXRelToY, String description, Type typeX, Type typeY, Annotation y,
          Map<String, Long> timings) {
    if (!nonOverlappingSupported(aXRelToY)) {
      return;
    }

    int limit = rnd.nextInt(5);

    // FIXME: Actually... I am pretty sure that all selection types should use the same
    // precedence for limit/backwards...
    List<Annotation> adjustedExpectation;
    if (asList(FOLLOWING, PRECEDING).contains(aXRelToY)) {
      // This works with FOLLOWING / PRECEDING
      adjustedExpectation = unambiguous(expected);
      adjustedExpectation = limit(adjustedExpectation, limit, aXRelToY);
      adjustedExpectation = backwards(adjustedExpectation);
    } else {
      // This works with COVERED_BY, COVERING, COLOCATED
      adjustedExpectation = unambiguous(expected);
      adjustedExpectation = backwards(adjustedExpectation);
      adjustedExpectation = limit(adjustedExpectation, limit, aXRelToY);
      return;
    }

    assertSelectionAsRandomIteration(rnd, adjustedExpectation, randomCas,
            (cas, type, context) -> aActual.select(cas, type, context).nonOverlapping().limit(limit)
                    .backwards(),
            aXRelToY, description + " non-overlapping backwards with limit(" + limit + ")", typeX,
            typeY, y, timings);
  }

  private static void assertShiftedLimitedBackwardsNonOverlappingSelectionAsRandomIteration(
          Random rnd, List<Annotation> expected, CAS randomCas,
          TypeByContextSelectorAsSelection aActual, RelativePosition aXRelToY, String description,
          Type typeX, Type typeY, Annotation y, Map<String, Long> timings) {
    if (!nonOverlappingSupported(aXRelToY)) {
      return;
    }

    // Random shift in the range of [-2, 2]
    int shift = rnd.nextInt(2) - rnd.nextInt(4);
    int limit = rnd.nextInt(5);

    // FIXME: Actually... I am pretty sure that all selection types should use the same
    // precedence for limit/backwards...
    List<Annotation> adjustedExpectation;
    if (asList(FOLLOWING, PRECEDING).contains(aXRelToY)) {
      // This works with FOLLOWING / PRECEDING
      adjustedExpectation = unambiguous(expected);
      adjustedExpectation = shifted(adjustedExpectation, shift, aXRelToY);
      adjustedExpectation = limit(adjustedExpectation, limit, aXRelToY);
      adjustedExpectation = backwards(adjustedExpectation);
    } else {
      // This works with COVERED_BY, COVERING, COLOCATED
      adjustedExpectation = unambiguous(expected);
      adjustedExpectation = backwards(adjustedExpectation);
      adjustedExpectation = shifted(adjustedExpectation, shift, aXRelToY);
      adjustedExpectation = limit(adjustedExpectation, limit, aXRelToY);
      return;
    }

    assertSelectionAsRandomIteration(rnd, adjustedExpectation, randomCas,
            (cas, type, context) -> aActual.select(cas, type, context).nonOverlapping()
                    .shifted(shift).limit(limit).backwards(),
            aXRelToY, description + " non-overlapping backwards with limit(" + limit + ") shifted("
                    + shift + ")",
            typeX, typeY, y, timings);
  }

  private static void assertNonOverlappingSelectionAsRandomIteration(Random rnd,
          List<Annotation> expected, CAS randomCas, TypeByContextSelectorAsSelection aActual,
          RelativePosition aXRelToY, String description, Type typeX, Type typeY, Annotation y,
          Map<String, Long> timings) {
    if (!nonOverlappingSupported(aXRelToY)) {
      return;
    }

    List<Annotation> adjustedExpectation = unambiguous(expected);

    assertSelectionAsRandomIteration(rnd, adjustedExpectation, randomCas,
            (cas, type, context) -> aActual.select(cas, type, context).nonOverlapping(), aXRelToY,
            " non-overlapping", typeX, typeY, y, timings);
  }

  private static void assertBackwardsNonOverlappingSelectionAsRandomIteration(Random rnd,
          List<Annotation> expected, CAS randomCas, TypeByContextSelectorAsSelection aActual,
          RelativePosition aXRelToY, String description, Type typeX, Type typeY, Annotation y,
          Map<String, Long> timings) {
    if (!nonOverlappingSupported(aXRelToY)) {
      return;
    }

    List<Annotation> adjustedExpectation = backwards(unambiguous(expected));

    assertSelectionAsRandomIteration(rnd, adjustedExpectation, randomCas,
            (cas, type, context) -> aActual.select(cas, type, context).nonOverlapping().backwards(),
            aXRelToY, " backwards non-overlapping", typeX, typeY, y, timings);
  }

  private static void assertBackwardsSelectionAsRandomIteration(Random rnd,
          List<Annotation> expected, CAS randomCas, TypeByContextSelectorAsSelection aActual,
          RelativePosition aXRelToY, String description, Type typeX, Type typeY, Annotation y,
          Map<String, Long> timings) {
    List<Annotation> adjustedExpectation = backwards(expected);

    assertSelectionAsRandomIteration(rnd, adjustedExpectation, randomCas,
            (cas, type, context) -> aActual.select(cas, type, context).backwards(), aXRelToY,
            " backwards", typeX, typeY, y, timings);
  }

  private static void assertShiftedSelectionAsRandomIteration(Random rnd, List<Annotation> expected,
          CAS randomCas, TypeByContextSelectorAsSelection aActual, RelativePosition aXRelToY,
          String description, Type typeX, Type typeY, Annotation y, Map<String, Long> timings) {

    // Random shift in the range of [-2, 2]
    int shift = rnd.nextInt(2) - rnd.nextInt(4);

    List<Annotation> adjustedExpectation = shifted(expected, shift, aXRelToY);

    assertSelectionAsRandomIteration(rnd, adjustedExpectation, randomCas,
            (cas, type, context) -> aActual.select(cas, type, context).shifted(shift), aXRelToY,
            " shifted by " + shift, typeX, typeY, y, timings);
  }

  private static void assertSelectionAsRandomIteration(Random rnd, List<Annotation> expected,
          CAS randomCas, TypeByContextSelectorAsSelection aActual, RelativePosition aXRelToY,
          String description, Type typeX, Type typeY, Annotation y, Map<String, Long> timings) {
    FSIterator<Annotation> it = aActual.select(randomCas, typeX, y).fsIterator();

    if (expected.size() == 0) {
      assertThat(it.isValid()).as(
              "Selecting X of type [%s] %s [%s]@[%d-%d][%d] %s random iteration%n%s%n"
                      + "Expected is empty but iterator is not invalid", // and points at
                                                                         // [%s]@[%d-%d][%d].",
              typeX.getName(), aXRelToY, y.getType().getShortName(), y.getBegin(), y
                      .getEnd(),
              identityHashCode(y), description, casToString(randomCas) /*
                                                                        * , it.get().getType().
                                                                        * getShortName(),
                                                                        * it.get().getBegin(),
                                                                        * it.get().getEnd(),
                                                                        * identityHashCode(it.get())
                                                                        */).isFalse();
      return;
    }

    StringBuilder expectedLog = new StringBuilder();
    for (int i = 0; i < expected.size(); i++) {
      Annotation ann = expected.get(i);
      expectedLog.append(
              String.format("expected[%d] = %s@[%d-%d] [%d]%n", i, ann.getType().getShortName(),
                      ann.getBegin(), ann.getEnd(), System.identityHashCode(ann)));
    }

    int cursor = 0;
    List<String> history = new ArrayList<>();
    while (history.size() < 2) {
      switch (rnd.nextInt(9)) {
        case 0: // Move to beginning
          cursor = 0;
          it.moveToFirst();
          history.add(format("[%d][%d] Moved to first", history.size(), cursor));
          break;
        case 1: // Move to end
          cursor = expected.size() - 1;
          it.moveToLast();
          history.add(format("[%d][%d] Moved to last", history.size(), cursor));
          break;
        case 2: // Move to next
        case 3: // Move to next
        case 4: // Move to next
          if (cursor < expected.size() - 1) {
            cursor++;
            it.moveToNext();
            history.add(format("[%d][%d] Moved to next", history.size(), cursor));
          }
          break;
        case 5: // Move to next
        case 6: // Move to prev
        case 7: // Move to prev
          if (cursor > 0) {
            cursor--;
            it.moveToPrevious();
            history.add(format("[%d][%d] Moved to previous", history.size(), cursor));
          }
          break;
        case 8: // Move to specific FS
        case 9: // Move to specific FS
          cursor = rnd.nextInt(expected.size());
          it.moveTo(expected.get(cursor));
          history.add(format("[%d][%d] Moved to FS #%d [%d]", history.size(), cursor, cursor,
                  System.identityHashCode(expected.get(cursor))));
          break;
      }

      assertThat(it.isValid()).as(
              "Selecting X of type [%s] %s [%s]@[%d-%d][%d] %s random iteration%n%s%n"
                      + "%s%nHistory:%n%s%n%nValidity mismatch.",
              typeX.getName(), aXRelToY, y.getType().getShortName(), y.getBegin(), y.getEnd(),
              identityHashCode(y), description, casToString(randomCas), expectedLog,
              history.stream().collect(joining("\n"))).isTrue();
      assertThat(it.get())
              .as("Selecting X of type [%s] %s [%s]@[%d-%d][%d] %s random iteration%n%s%n"
                      + "%s%nHistory:%n%s%n%nExpectation mismatch. ", typeX.getName(), aXRelToY,
                      y.getType().getShortName(), y.getBegin(), y.getEnd(), identityHashCode(y),
                      description, casToString(randomCas), expectedLog,
                      history.stream().collect(joining("\n")))
              // We do not compare the exact annotation here because the moveTo operation moves to
              // the first annotation that matches the target annotation. If there are multiple
              // annoations with the same type/begin/end, then it moves to the first one of these,
              // even if the cursor is e.g. pointing to the second one.
              .isEqualToComparingOnlyGivenFields(expected.get(cursor), "begin", "end");

      // Since the moveTo operation may not select the annotation the cursor is pointing to but
      // instead the first matching one, we may have to adjust the cursor after the moveTo operation
      // to keep the cursor in sync with the actual iterator behavior (e.g. which elements are
      // then selected after moveToNext or moveToPrevious operations.
      if (cursor != expected.indexOf(it.get())) {
        cursor = expected.indexOf(it.get());
        history.add(format("[%d][%d] Adjusted cursor #%d", history.size(), cursor, cursor));
      }
    }
  }

  private static String casToString(CAS aCas) {
    int MAX_ANNOTATIONS = 100;
    if (aCas.select().count() > MAX_ANNOTATIONS) {
      return "CAS contains more than " + MAX_ANNOTATIONS
              + " annotations - try tweaking the test parameters to reproduce"
              + " the isssue with a smaller CAS.";
    }

    StringBuilder sb = new StringBuilder();
    aCas.select().forEach(fs -> {
      if (fs instanceof AnnotationFS) {
        AnnotationFS ann = (AnnotationFS) fs;
        sb.append(format("%s@[%3d-%3d] [%d] (parent type: %s)%n", ann.getType().getShortName(),
                ann.getBegin(), ann.getEnd(), System.identityHashCode(ann),
                ann.getCAS().getTypeSystem().getParent(ann.getType())));
      }
    });
    return sb.toString();
  }

  private static void initRandomCas(Random rnd, CAS aCas, int aSize, int aMinimumWidth,
          Type... aTypes) {
    List<Type> types = new ArrayList<>(asList(aTypes));

    // Shuffle the types
    for (int n = 0; n < 10; n++) {
      Type t = types.remove(rnd.nextInt(types.size()));
      types.add(t);
    }

    // Randomly generate annotations
    if (logAnnotationCreation) {
      System.out.println();
    }
    for (int n = 0; n < aSize; n++) {
      for (Type t : types) {
        int begin = rnd.nextInt(100);
        int end = begin + rnd.nextInt(30) + aMinimumWidth;
        AnnotationFS ann = aCas.createAnnotation(t, begin, end);
        if (logAnnotationCreation) {
          System.out.printf("cas.createAnnotation(%s, %d, %d)\t[%d]%n",
                  t.getShortName().toLowerCase(), begin, end, identityHashCode(ann));
        }
        aCas.addFsToIndexes(ann);
      }
    }
  }

  private static boolean nonOverlappingSupported(RelativePosition aXRelToY) {
    return asList(COVERED_BY, FOLLOWING, PRECEDING).contains(aXRelToY);
  }

  private static int insertionPoint(List<Annotation> expected, Annotation fs) {
    return Collections.binarySearch(expected, fs,
            comparing(AnnotationFS::getBegin).thenComparing(AnnotationFS::getEnd, reverseOrder()));
  }

  private static List<Annotation> shifted(List<Annotation> expected, int shift,
          RelativePosition aXRelToY) {
    return startAtShifted(expected, null, shift, aXRelToY);
  }

  private static List<Annotation> startAtShifted(List<Annotation> expected, Annotation startFs,
          int shift, RelativePosition aXRelToY) {

    // NOTE: for the moment, we do not use the startFs, so the insertionPoint seek and handling
    // of the insertion point might be buggy!

    int start;
    int end;
    if (aXRelToY == PRECEDING) {
      // Shifting always shifts away from the reference FS - since with preceding the reference FS
      // is at the end of the selection region, we need to shift from the end here.
      start = 0;
      end = startFs != null ? insertionPoint(expected, startFs) : expected.size();
      end -= shift;
    } else {
      start = startFs != null ? insertionPoint(expected, startFs) : 0;
      start += shift;
      end = expected.size();
    }

    start = min(max(0, start), expected.size());
    end = min(max(0, end), expected.size());

    if (start > end) {
      return emptyList();
    }

    return new ArrayList<>(expected.subList(start, end));
  }

  private static List<Annotation> backwards(List<Annotation> expected) {
    List<Annotation> reverseExpected = new ArrayList<>(expected);
    Collections.reverse(reverseExpected);
    return reverseExpected;
  }

  private static List<Annotation> unambiguous(List<Annotation> expected) {
    List<Annotation> unambigousExpected = new ArrayList<>();
    Annotation current = null;
    for (Annotation e : expected) {
      if (current == null || !overlapping(e, current)) {
        unambigousExpected.add(e);
        current = e;
      }
    }
    return unambigousExpected;
  }

  private static List<Annotation> limit(List<Annotation> expected, int limit,
          RelativePosition aXRelToY) {
    List<Annotation> limitedExpected = aXRelToY == PRECEDING
            ? expected.subList(max(0, expected.size() - limit), expected.size())
            : expected.subList(0, min(limit, expected.size()));
    return limitedExpected;
  }

  @FunctionalInterface
  public interface RelativeAnnotationPredicate {
    boolean apply(CAS cas, Type type, Annotation x, Annotation y);
  }

  @FunctionalInterface
  public interface TypeByContextSelector {
    List<AnnotationFS> select(CAS aCas, Type aType, Annotation aContext);
  }

  @FunctionalInterface
  public interface TypeByContextSelectorAsSelection {
    SelectFSs<Annotation> select(CAS aCas, Type aType, Annotation aContext);
  }
}
