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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.IntFunction;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationPredicateAssert.TestCase;
import org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.assertj.core.api.AutoCloseableSoftAssertions;

public class SelectFsAssert {
  private static final long RANDOM_SEED = System.nanoTime();
//  private static final long RANDOM_SEED = 1123487858940988l;
  
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
      TypeByContextSelector aExpected, TypeByContextSelector aActual) throws Exception {
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    
    IntFunction<Integer> annotationsPerIteration = iteration -> iteration * 3;
    // Quick overrides for debugging
//    annotationsPerIteration = iteration -> 10;
//    aIterations = 1_000;
//    aTypes = 4;

    Random rnd = new Random(RANDOM_SEED);
    Map<String, Type> types = new LinkedHashMap<>();
    for (int i = 0; i < aTypes; i++) {
      String typeName = "test.Type" + (i + 1);
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
    
    System.out.print("Iteration: ");
    try {
      Iterator<Type> ti = types.values().iterator();
      Type type1 = ti.next();
      Type type2 = ti.hasNext() ? ti.next() : type1;
      
      long timeExpected = 0;
      long timeActual = 0;
      for (int i = 0; i < aIterations; i++) {
        randomCas.reset();
        
        if (i % 10 == 0) {
          System.out.print(i);
          if (i > 0 && i % 100 == 0) {
            System.out.println();
          }
        }
        else {
          System.out.print(".");
        }
          
        initRandomCas(randomCas, annotationsPerIteration.apply(i), 0, types.values().toArray(new Type[types.size()]));
  
        for (Annotation y : randomCas.<Annotation>select(type1)) {
          long t1 = System.currentTimeMillis();
          List<AnnotationFS> expected = aExpected.select(randomCas, type2, y);
          timeExpected += System.currentTimeMillis() - t1;
          
          long t2 = System.currentTimeMillis();
          List<AnnotationFS> actual = aActual.select(randomCas, type2, y);
          timeActual += System.currentTimeMillis() - t2;
  
//          try {
            assertThat(actual)
                .as("Selecting X of type [%s] %s [%s]@[%d-%d]%n%s%n", type2.getName(), xRelToY,
                    y.getType().getShortName(), y.getBegin(), y.getEnd(),
                    casToString(randomCas))
                .containsExactlyElementsOf(expected);
//          }
//          catch (Throwable e) {
//            // Set a breakpoint here to halt when an assert above fails. The select triggering the
//            // assert is then re-executed below and you can look into its details. To allow
//            // stopping and re-executing the test, you need to put the displayed random seed into
//            // the static variable RANDOM_SEED at the beginning of the file. Don't forget to
//            // Comment this out again and to re-set the RANDOM_SEED to timer-based when you are
//            // done with debugging.
//            System.out.printf("RANDOM SEED: %d%n", RANDOM_SEED);
//            aActual.select(randomCas, type2, y);
//            throw e;
//          }
        }
      }
      System.out.print(aIterations);
      System.out.printf(" (time 1: %4dms / time 2: %4dms)", timeExpected, timeActual);
    }
    finally {
      System.out.println();
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

  private static void initRandomCas(CAS aCas, int aSize, int aMinimumWidth, Type... aTypes) {
    Random rnd = new Random(RANDOM_SEED);

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
}
