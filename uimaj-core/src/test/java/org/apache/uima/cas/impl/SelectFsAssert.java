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

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
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
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.assertj.core.api.AutoCloseableSoftAssertions;

public class SelectFsAssert {
  public static void assertSelectFS(RelativePosition aCondition, RelativeAnnotationPredicate aPredicate, 
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

  public static void assertSelectionIsEqualOnRandomData(String xRelToY, int aIterations, int aTypes, 
      TypeByContextSelector aExpected, TypeByContextSelectorAsSelection aActual) throws Exception {
    long lockedSeed = -1;
    IntFunction<Integer> annotationsPerIteration = iteration -> iteration * 3;

    // ============================================================================================
    // Quick overrides for debugging
//    annotationsPerIteration = iteration -> 5;
//    aIterations = 1000000;
//    aTypes = 3;
//    lockedSeed = 1547487011654502l;
    // ============================================================================================
    
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
        }
        else {
          System.out.print(".");
        }
        
        TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
        Map<String, Type> types = new LinkedHashMap<>();
        for (int ti = 0; ti < aTypes; ti++) {
          String typeName = "test.Type" + (ti + 1);
          if (rnd.nextInt() % 2 == 0 || types.size() == 0) {
            tsd.addType(typeName, "", CAS.TYPE_NAME_ANNOTATION);
          }
          else {
            tsd.addType(typeName, "", new ArrayList<>(types.keySet()).get(rnd.nextInt(types.size())));
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
  
        for (Annotation y : randomCas.<Annotation>select(typeY)) {
          // Randomly use a non-indexed annotation for selection so we test both cases (using an
          // indexed or non-indexed annotation).
          if (rnd.nextInt() % 2 == 0) {
            y = (Annotation) randomCas.createAnnotation(typeList[rnd.nextInt(typeList.length)], y.getBegin(),
                y.getEnd());
          }
          
          long tExpected = System.currentTimeMillis();
          List<Annotation> expected = aExpected.select(randomCas, typeX, y).stream().map(a -> (Annotation) a)
              .collect(toList());
          timings.compute("actual", (k, v) -> v == null ? 0l : v + currentTimeMillis() - tExpected);

          sizeCounts.compute(expected.size(), (k, v) -> v == null ? 1 : v++);
          
          try {
            assertSelectionAsList(expected, randomCas, aActual, xRelToY, typeX, typeY, y, timings);
            assertSelectionAsForwardIteration(expected, randomCas, aActual, xRelToY, typeX, typeY,
                y, timings);
            assertSelectionAsBackwardIteration(expected, randomCas, aActual, xRelToY, typeX, typeY,
                y, timings);
            // FIXME: WIP - the checks below still sometimes fail
//            assertSelectionAsRandomIteration(rnd, expected, randomCas, aActual, xRelToY, typeX,
//                typeY, y, timings);
//            assertSelectionAsRandomIteration(expected.subList(0, min(5, expected.size())), randomCas,
//                (cas, type, context) -> aActual.select(cas, type, context).limit(5), 
//                xRelToY, typeX, typeY, y, timings);
          }
          catch (Throwable e) {
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
      System.out.print(timings.entrySet().stream()
          .map(e -> format("%s: %4dms", e.getKey(), e.getValue()))
          .collect(joining(" | ", " (", ")")));
    }
    finally {
      System.out.println();
    }
  }
  
  private static void assertSelectionAsList(List<Annotation> expected, CAS randomCas,
      TypeByContextSelectorAsSelection aActual,
      String xRelToY, Type typeX, Type typeY, Annotation y, Map<String, Long> timings) {
    long tList = System.currentTimeMillis();
    List<Annotation> listActual = aActual.select(randomCas, typeX, y).asList();
    timings.compute("asList", (k, v) -> v == null ? 0l : v + currentTimeMillis() - tList);
    assertThat(listActual)
        .as("Selecting X of type [%s] %s [%s]@[%d-%d] asList%n%s%n", typeX.getName(), xRelToY,
            y.getType().getShortName(), y.getBegin(), y.getEnd(),
            casToString(randomCas))
        .containsExactlyElementsOf(expected);
  }

  private static void assertSelectionAsForwardIteration(List<Annotation> expected, CAS randomCas,
      TypeByContextSelectorAsSelection aActual,
      String xRelToY, Type typeX, Type typeY, Annotation y, Map<String, Long> timings) {
    long t = System.currentTimeMillis();
    
    FSIterator<Annotation> it = aActual.select(randomCas, typeX, y).fsIterator();
    Annotation initial = it.isValid() ? it.get() : null;
    List<Annotation> actual = new ArrayList<>();
    it.moveToFirst();
    assertThat(it.isValid() ? it.get() : null)
        .as("Annotation pointed at by iterator initially should match annotation after calling "
            + "moveToFirst:%n%s%n%s%n" +
            "Selecting X of type [%s] %s [%s]@[%d-%d] iterator forward%n%s%n",
            initial, it.isValid() ? it.get() : null, typeX.getName(), xRelToY,
            y.getType().getShortName(), y.getBegin(), y.getEnd(), casToString(randomCas))
        .isEqualTo(initial);
    
    while (it.isValid()) {
      actual.add(it.get());
      it.moveToNext();
    }
    
    timings.compute("iterator forward", (k, v) -> v == null ? 0l : v + currentTimeMillis() - t);
    
    assertThat(actual)
        .as("Selecting X of type [%s] %s [%s]@[%d-%d] iterator forward%n%s%n", typeX.getName(),
            xRelToY,
            y.getType().getShortName(), y.getBegin(), y.getEnd(),
            casToString(randomCas))
        .containsExactlyElementsOf(expected);
  }

  private static void assertSelectionAsBackwardIteration(List<Annotation> expected, CAS randomCas,
      TypeByContextSelectorAsSelection aActual,
      String xRelToY, Type typeX, Type typeY, Annotation y, Map<String, Long> timings) {
    long t = System.currentTimeMillis();
    
    FSIterator<Annotation> it = aActual.select(randomCas, typeX, y).fsIterator();
    List<Annotation> actual = new ArrayList<>();
    it.moveToLast();
    while (it.isValid()) {
      actual.add(0, it.get());
      it.moveToPrevious();
    }
    
    timings.compute("iterator backwards", (k, v) -> v == null ? 0l : v + currentTimeMillis() - t);
    
    assertThat(actual)
        .as("Selecting X of type [%s] %s [%s]@[%d-%d] iterator backwards%n%s%n", typeX.getName(), xRelToY,
            y.getType().getShortName(), y.getBegin(), y.getEnd(),
            casToString(randomCas))
        .containsExactlyElementsOf(expected);
  }
  
  private static void assertSelectionAsRandomIteration(Random rnd, List<Annotation> expected, CAS randomCas,
      TypeByContextSelectorAsSelection aActual,
      String xRelToY, Type typeX, Type typeY, Annotation y, Map<String, Long> timings) {
    FSIterator<Annotation> it = aActual.select(randomCas, typeX, y).fsIterator();

    if (expected.size() == 0) {
      assertThat(it.isValid()).isFalse();
      return;
    }
    
    List<String> history = new ArrayList<>();
    
    int cursor = 0;
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
        history.add(format("[%d][%d] Moved to FS #%d", history.size(), cursor, cursor));
      }
      
      assertThat(it.isValid())
          .as("Validity mismatch. History:\n" + history.stream().collect(joining("\n")))
          .isTrue();
      assertThat(it.get())
          .as("Expectation mismatch. History:\n" + history.stream().collect(joining("\n")))
          // We do not compare the exact annotation here because the moveTo operation moves to the
          // first annotation that matches the target annotation. If there are multiple annoations
          // with the same type/begin/end, then it moves to the first one of these, even if the
          // cursor is e.g. pointing to the second one.
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
          + " annotations - try tweaking the test parameters to reproduce" + " the isssue with a smaller CAS.";
    }
    
    StringBuilder sb = new StringBuilder();
    aCas.select().forEach(fs -> {
      if (fs instanceof AnnotationFS) {
        AnnotationFS ann = (AnnotationFS) fs;
        sb.append(format("%s@[%d-%d] (parent type: %s)%n", ann.getType().getShortName(), ann.getBegin(), 
            ann.getEnd(), ann.getCAS().getTypeSystem().getParent(ann.getType())));
      }
    });
    return sb.toString();
  }

  private static void initRandomCas(Random rnd, CAS aCas, int aSize, int aMinimumWidth, Type... aTypes) {
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
        System.out.printf("cas.createAnnotation(%s, %d, %d)%n", t.getShortName(), begin, end);
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
  public static interface TypeByContextSelectorAsSelection {
    SelectFSs<Annotation> select(CAS aCas, Type aType, Annotation aContext);
  }
}
