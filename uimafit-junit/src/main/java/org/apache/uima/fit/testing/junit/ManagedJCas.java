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
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.apache.uima.fit.validation.ValidationResult.Severity.ERROR;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.validation.ValidationException;
import org.apache.uima.fit.validation.ValidationSummary;
import org.apache.uima.fit.validation.Validator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

/**
 * Provides a {@link JCas} object which is automatically reset before the test. The idea of this
 * class is to re-use JCas objects across different test method to avoid the overhead of having to
 * set up a new JCas every time. Each thread requesting a JCas gets a different instance (the JCases
 * are internally managed as {@link ThreadLocal}. When a test completes, all of the JCasses that
 * handed out to any thread are reset (except any JCases which may meanwhile have been garbage
 * collected).
 */
public final class ManagedJCas
        implements TestWatcher, AfterTestExecutionCallback, AfterAllCallback {
  private final ThreadLocal<JCas> casHolder;

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Set<JCas> managedCases = synchronizedSet(newSetFromMap(new WeakHashMap<>()));

  private Validator defaultValidator = new Validator.Builder().build();
  private Validator validator = null;

  static {
    try {
      // Try creating a CAS to initialize the internal UIMA types.
      // Workaround for: https://github.com/apache/uima-uimaj/issues/234
      CasCreationUtils.createCas();
    } catch (Exception e) {
      fail("Unable to initialize UIMA");
    }
  }

  /**
   * Provides a JCas with an auto-detected type system.
   */
  public ManagedJCas() {
    casHolder = ThreadLocal.withInitial(() -> {
      try {
        JCas cas = createJCas();
        managedCases.add(cas);
        return cas;
      } catch (UIMAException e) {
        return fail("Unable to initialize managed JCas", e);
      }
    });
  }

  /**
   * Provides a JCas with the specified type system.
   * 
   * @param aTypeSystemDescription
   *          the type system used to initialize the CAS.
   */
  public ManagedJCas(TypeSystemDescription aTypeSystemDescription) {
    casHolder = ThreadLocal.withInitial(() -> {
      try {
        JCas cas = createJCas(aTypeSystemDescription);
        managedCases.add(cas);
        return cas;
      } catch (UIMAException e) {
        return fail("Unable to initialize managed JCas", e);
      }
    });
  }

  /**
   * @return the JCas object managed by this rule.
   */
  public JCas get() {
    return casHolder.get();
  }

  @Override
  public void afterAll(ExtensionContext aContext) throws Exception {
    casHolder.remove();
  }

  @Override
  public void afterTestExecution(ExtensionContext context) throws Exception {
    try {
      managedCases.forEach(this::assertValid);
    } finally {
      reset();
    }
  }

  private void reset() {
    this.validator = null;

    for (JCas cas : managedCases) {
      try {
        cas.reset();
      } catch (Exception e) {
        LOG.error(e, () -> "Unable to reset managed CAS");
      }
    }
  }

  /**
   * Skip validation for the current test run only.
   * 
   * @return the object for chaining.
   */
  public ManagedJCas skipValidation() {
    validator = new Validator.Builder().withoutAutoDetectedChecks().build();
    return this;
  }

  /**
   * Skip validation by default. If validation is enabled for a particular run using
   * {@link #withValidator(Validator)} it is reset to a no-op validator again after the test is
   * complete.
   * 
   * @return the object for chaining.
   */
  public ManagedJCas withoutDefaultValidator() {
    this.defaultValidator = new Validator.Builder().withoutAutoDetectedChecks().build();
    return this;
  }

  /**
   * Set a default validator for the all test runs.
   * 
   * @return the object for chaining.
   */
  public ManagedJCas withDefaultValidator(Validator aValidator) {
    this.defaultValidator = aValidator;
    return this;
  }

  /**
   * Set a validator for the current test run only.
   * 
   * @return the object for chaining.
   */
  public ManagedJCas withValidator(Validator aValidator) {
    this.validator = aValidator;
    return this;
  }

  Validator getValidator() {
    if (validator != null) {
      return validator;
    }

    return defaultValidator;
  }

  private void assertValid(JCas aJCas) {
    Validator activeValidator = getValidator();
    if (getValidator() == null) {
      return;
    }

    try {
      ValidationSummary summary = activeValidator.check(aJCas);

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