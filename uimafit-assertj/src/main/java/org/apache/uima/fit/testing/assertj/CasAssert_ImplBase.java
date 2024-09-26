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
package org.apache.uima.fit.testing.assertj;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.apache.uima.fit.validation.ValidationResult.Severity.ERROR;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.validation.ValidationException;
import org.apache.uima.fit.validation.ValidationSummary;
import org.apache.uima.fit.validation.Validator;
import org.apache.uima.jcas.JCas;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Fail;

/**
 * Asserts related to the {@link CAS}.
 */
public class CasAssert_ImplBase<SELF extends CasAssert_ImplBase<SELF, ACTUAL>, ACTUAL>
        extends AbstractAssert<SELF, ACTUAL> {
  public CasAssert_ImplBase(ACTUAL aActual, Class<?> aSelfType) {
    super(aActual, aSelfType);
  }

  protected ValidationSummary validate(Validator aValidator) throws ValidationException {
    if (actual instanceof CAS) {
      return aValidator.check((CAS) actual);
    }
    if (actual instanceof JCas) {
      return aValidator.check((JCas) actual);
    }

    throw new IllegalArgumentException(
            "Unsupported CAS implementation [" + actual.getClass().getName() + "]");
  }

  /**
   * Checks that CAS is valid using the auto-detected validation checks.
   */
  public SELF isValid() {
    return isValidUsing(new Validator.Builder().build());
  }

  /**
   * Checks that CAS is valid using the given validator.
   */
  public SELF isValidUsing(Validator aValidator) {
    isNotNull();

    try {
      ValidationSummary summary = validate(aValidator);

      String messageBuffer = summary.getResults().stream()
              .filter(r -> r.getSeverity().isEquallyOrMoreSevereThan(ERROR))
              .map(r -> format("[%s] %s", r.getSource(), r.getMessage())).collect(joining("\n"));

      if (messageBuffer.length() > 0) {
        Fail.fail(messageBuffer);
      }
    } catch (ValidationException e) {
      Fail.fail("Unable to validate CAS", e);
    }

    return myself;
  }
}