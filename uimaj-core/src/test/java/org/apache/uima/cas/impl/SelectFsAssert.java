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

import static java.util.Arrays.asList;
import static org.apache.uima.cas.text.AnnotationPredicatesTest.TEST_CASES;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationPredicatesTest.RelativePosition;
import org.apache.uima.cas.text.AnnotationPredicatesTest.TestCase;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.assertj.core.api.AutoCloseableSoftAssertions;

public class SelectFsAssert {
  public static void assertSelectFS(RelativePosition aCondition, RelativeAnnotationPredicate aPredicate)
      throws Exception {
    CAS cas = CasCreationUtils.createCas();
    Type type = cas.getAnnotationType();

    try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
      for (TestCase testCase : TEST_CASES) {
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
    final int ITERATIONS = 50;

    String type1Name = "test.Type1";
    String type2Name = "test.Type2";

    TypeSystemDescription tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType(type1Name, "", CAS.TYPE_NAME_ANNOTATION);
    tsd.addType(type2Name, "", CAS.TYPE_NAME_ANNOTATION);

    CAS randomCas = CasCreationUtils.createCas(tsd, null, null, null);
    Type type1 = randomCas.getTypeSystem().getType(type1Name);
    Type type2 = randomCas.getTypeSystem().getType(type2Name);

    for (int i = 0; i < ITERATIONS; i++) {
      System.out.printf("Iteration %d%n", i);

      initRandomCas(randomCas, 3 * i, type1, type2);

      for (Annotation context : randomCas.<Annotation>select(type1)) {
        List<AnnotationFS> expected = aExpected.select(randomCas, type2, context);
        List<AnnotationFS> actual = aActual.select(randomCas, type2, context);

        assertThat(actual)
            .as("Selected [%s] with context [%s]@[%d..%d]", type2Name, type1Name, context.getBegin(), context.getEnd())
            .containsExactlyElementsOf(expected);
      }
    }
  }

  private static void initRandomCas(CAS aCas, int aSize, Type... aTypes) {
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
        int end = begin + rnd.nextInt(30);
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
