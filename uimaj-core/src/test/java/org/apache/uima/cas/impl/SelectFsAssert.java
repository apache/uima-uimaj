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

  public static void assertSelectionIsEqualOnRandomData(int aIterations, int aTypes, 
      TypeByContextSelector aExpected, TypeByContextSelector aActual) throws Exception {
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
    
    Map<String, Type> types = new LinkedHashMap<>();
    for (int i = 0; i < aTypes; i++) {
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
      
      long timeExpected = 0;
      long timeActual = 0;
      for (int i = 0; i < aIterations; i++) {
        if (i % 10 == 0) {
          System.out.print(i);
        }
        else {
          System.out.print(".");
        }
  
        initRandomCas(randomCas, 3 * i, 0, types.values().toArray(new Type[types.size()]));
  
        for (Annotation context : randomCas.<Annotation>select(type1)) {
          long t1 = System.currentTimeMillis();
          List<AnnotationFS> expected = aExpected.select(randomCas, type2, context);
          timeExpected += System.currentTimeMillis() - t1;
          
          long t2 = System.currentTimeMillis();
          List<AnnotationFS> actual = aActual.select(randomCas, type2, context);
          timeActual += System.currentTimeMillis() - t2;
  
          assertThat(actual)
              .as("Selected [%s] with context [%s]@[%d..%d]%n%s%n", type2.getShortName(), 
                  type1.getShortName(), context.getBegin(), context.getEnd(), 
                  casToString(randomCas))
              .containsExactlyElementsOf(expected);
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
    if (aCas.select().count() > 10) {
      return "CAS contains more than 10 annotations - try tweaking the test parameters to reproduce"
          + " the isssue with a smaller CAS.";
    }
    
    StringBuilder sb = new StringBuilder();
    aCas.select().forEach(fs -> {
      if (fs instanceof AnnotationFS) {
        AnnotationFS ann = (AnnotationFS) fs;
        sb.append(format("%s@[%d-%d]%n", ann.getType().getShortName(), ann.getBegin(), 
            ann.getEnd()));
      }
    });
    return sb.toString();
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
}
