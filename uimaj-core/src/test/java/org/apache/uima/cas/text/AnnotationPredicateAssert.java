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

import java.util.List;
import java.util.function.Function;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationPredicateTestData.RelativePosition;
import org.assertj.core.api.SoftAssertions;

public class AnnotationPredicateAssert {
  public static void assertPosition(SoftAssertions aSoftly, RelativePosition aCondition,
      RelativePositionPredicate aPredicate, List<TestCase> aTestCases) {
    for (TestCase testCase : aTestCases) {
      aSoftly.assertThat(testCase.getTest().apply(aPredicate))
          .as(testCase.getDescription())
          .isEqualTo(testCase.getValidPositions().contains(aCondition));
    }
  }
  
  @FunctionalInterface
  public static interface RelativePositionPredicate {
    boolean apply(int beginA, int endA, int beginB, int endB);
  }

  @FunctionalInterface
  public static interface RelativeAnnotationPositionPredicate {
    boolean apply(AnnotationFS aAnnotation, int beginB, int endB);
  }

  @FunctionalInterface
  public static interface RelativeAnnotationPredicate {
    boolean apply(AnnotationFS aAnnotationA, AnnotationFS aAnnotationB);
  }

  public static RelativePositionPredicate toRelativePositionPredicate1(CAS aCas,
      RelativeAnnotationPositionPredicate aPred) {
    return (beginA, endA, beginB, endB) -> { 
      aCas.reset();
      Type type = aCas.getAnnotationType();
      return aPred.apply(aCas.createAnnotation(type, beginA, endA), beginB, endB);};
  }

  public static RelativePositionPredicate toRelativePositionPredicate1Inverse(CAS aCas,
      RelativeAnnotationPositionPredicate aPred) {
    return (beginA, endA, beginB, endB) -> { 
      aCas.reset();
      Type type = aCas.getAnnotationType();
      return aPred.apply(aCas.createAnnotation(type, beginB, endB), beginA, endA);};
  }

  public static RelativePositionPredicate toRelativePositionPredicate2(CAS aCas, RelativeAnnotationPredicate aPred) {
    return (beginA, endA, beginB, endB) -> {
      aCas.reset();
      Type type = aCas.getAnnotationType();
      return aPred.apply(
          aCas.createAnnotation(type, beginA, endA), 
          aCas.createAnnotation(type, beginB, endB));
      };
  }

  public static RelativePositionPredicate toRelativePositionPredicate2Inverse(CAS aCas, RelativeAnnotationPredicate aPred) {
    return (beginA, endA, beginB, endB) -> {
      aCas.reset();
      Type type = aCas.getAnnotationType();
      return aPred.apply(
          aCas.createAnnotation(type, beginB, endB), 
          aCas.createAnnotation(type, beginA, endA));
      };
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
