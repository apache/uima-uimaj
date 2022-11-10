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
package org.apache.uima.fit.testing.junit;

import static java.lang.String.format;
import static java.util.Collections.newSetFromMap;
import static java.util.Collections.synchronizedSet;
import static java.util.stream.Collectors.joining;
import static org.apache.uima.fit.factory.CasFactory.createCas;
import static org.apache.uima.fit.validation.ValidationResult.Severity.ERROR;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Set;
import java.util.WeakHashMap;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.validation.ValidationException;
import org.apache.uima.fit.validation.ValidationSummary;
import org.apache.uima.fit.validation.Validator;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * Provides a {@link CAS} object which is automatically reset before the test. The idea of this
 * class is to re-use CAS objects across different test method to avoid the overhead of having to
 * set up a new CAS every time. Each thread requesting a CAS gets a different instance (the CASes
 * are internally managed as {@link ThreadLocal}. When a test completes, all of the CASses that
 * handed out to any thread are reset (except any CASes which may meanwhile have been garbage
 * collected).
 */
public final class ManagedCas implements TestWatcher, AfterTestExecutionCallback, AfterAllCallback {
  private final ThreadLocal<CAS> casHolder;

  private final Set<CAS> managedCases = synchronizedSet(newSetFromMap(new WeakHashMap<>()));

  private Validator validator = new Validator.Builder().build();

  /**
   * Provides a CAS with an auto-detected type system.
   */
  public ManagedCas() {
    casHolder = ThreadLocal.withInitial(() -> {
      try {
        CAS cas = createCas();
        managedCases.add(cas);
        return cas;
      } catch (UIMAException e) {
        return fail("Unable to initialize managed CAS", e);
      }
    });
  }

  /**
   * Provides a CAS with the specified type system.
   * 
   * @param aTypeSystemDescription
   *          the type system used to initialize the CAS.
   */
  public ManagedCas(TypeSystemDescription aTypeSystemDescription) {
    casHolder = ThreadLocal.withInitial(() -> {
      try {
        CAS cas = createCas(aTypeSystemDescription);
        managedCases.add(cas);
        return cas;
      } catch (UIMAException e) {
        return fail("Unable to initialize managed CAS", e);
      }
    });
  }

  /**
   * @return the CAS object managed by this rule.
   */
  public CAS get() {
    return casHolder.get();
  }

  @Override
  public void afterAll(ExtensionContext aContext) throws Exception {
    casHolder.remove();
  }

  @Override
  public void afterTestExecution(ExtensionContext context) throws Exception {
    managedCases.forEach(this::assertValid);
    managedCases.forEach(CAS::reset);
  }

  public ManagedCas skipValidation() {
    validator = null;
    return this;
  }

  public ManagedCas withValidator(Validator aValidator) {
    this.validator = aValidator;
    return this;
  }

  private void assertValid(CAS aJCas) {
    if (validator == null) {
      return;
    }

    try {
      ValidationSummary summary = validator.check(aJCas);

      String messageBuffer = summary.getResults().stream()
              .filter(r -> r.getSeverity().isEquallyOrMoreSevereThan(ERROR))
              .map(r -> format("[%s] %s", r.getSource(), r.getMessage())).collect(joining("\n"));

      if (messageBuffer.length() > 0) {
        Assertions.fail(messageBuffer);
      }
    } catch (ValidationException e) {
      Assertions.fail("Unable to validate CAS", e);
    }
  }
}