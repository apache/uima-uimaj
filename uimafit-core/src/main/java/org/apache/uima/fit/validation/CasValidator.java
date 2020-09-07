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
package org.apache.uima.fit.validation;

import static java.util.Arrays.stream;
import static java.util.ServiceLoader.load;
import static java.util.stream.StreamSupport.stream;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.uima.cas.CAS;

/**
 * Validate a CAS.
 */
public class CasValidator {

  private Collection<CasValidationCheck> checks;

  public CasValidator(Collection<CasValidationCheck> checks) {
    this.checks = checks;
  }

  public CasValidationSummary check(CAS cas) {
    CasValidationSummary summary = new CasValidationSummary();

    for (CasValidationCheck check : checks) {
      summary.addAll(check.check(cas));
    }

    return summary;
  }

  public Collection<CasValidationCheck> getChecks() {
    return checks;
  }

  public static class Builder {

    private Set<CasValidationCheck> checks = new LinkedHashSet<>();
    private Set<Pattern> excludePatterns = new HashSet<>();
    private Set<Class<?>> excludeTypes = new HashSet<>();
    private boolean skipAutoDetection = false;

    /**
     * Add the given check instance to the validator. This allows even adding checks which are not
     * available via the Java Service Locator, which take parameters or which are otherwise stateful
     * (assuming that the resulting validator is not shared between threads).
     * 
     * @param check
     *          a check instance to use.
     */
    public void withCheck(CasValidationCheck check) {
      checks.add(check);
    }

    public void withoutAutoDetectedChecks() {
      skipAutoDetection = true;
    }

    public void witAutoDetectedChecks() {
      skipAutoDetection = false;
    }

    /**
     * Skip auto-detection of any checks with the given names. Subtypes of the given classes are not
     * excluded from auto-detection.
     */
    public void excludingFromAutoDetectionByName(String... className) {
      stream(className)
          .map(Pattern::quote)
          .map(Pattern::compile)
          .forEach(excludePatterns::add);
    }

    /**
     * Skip auto-detection of any checks with the given regular expressions.
     */
    public void excludingFromAutoDetectionByPattern(String... patterns) {
      stream(patterns)
          .map(Pattern::compile)
          .forEach(excludePatterns::add);
    }

    /**
     * Skips auto-detection of any checks of the given types (includes checks that are subclasses or
     * implementations of the given types).
     */
    public void excludingFromAutoDetectionByType(Class<?>... classes) {
      stream(classes).forEach(excludeTypes::add);
    }

    private void autoDetectChecks() {
      stream(load(CasValidationCheck.class).spliterator(), false)
              .filter(check -> excludePatterns.stream()
                      .noneMatch(p -> p.matcher(check.getClass().getName()).matches()))
              .filter(check -> excludeTypes.stream()
                      .noneMatch(t -> t.isAssignableFrom(check.getClass())))
              .forEachOrdered(checks::add);
    }

    public CasValidator build() {
      if (!skipAutoDetection) {
        autoDetectChecks();
      }

      return new CasValidator(checks);
    }
  }
}
