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
import static org.apache.uima.internal.util.ServiceLoaderUtil.loadServicesSafely;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;

/**
 * Validate a (J)CAS.
 */
public class Validator {
  private Collection<ValidationCheck> checks;

  public Validator(Collection<ValidationCheck> checks) {
    this.checks = checks;
  }

  public ValidationSummary check(JCas aJCas) throws ValidationException {
    ValidationSummary summary = new ValidationSummary();

    for (ValidationCheck check : checks) {
      try {
        summary.add(ValidationResult.trace(check, "Trying check..."));
        if (check instanceof CasValidationCheck) {
          summary.addAll(((CasValidationCheck) check).validate(aJCas.getCas()));
        } else if (check instanceof JCasValidationCheck) {
          summary.addAll(((JCasValidationCheck) check).validate(aJCas));
        } else {
          throw new IllegalArgumentException(
                  "Unknown ValidationCheck type: [" + check.getClass().getName() + "]");
        }
      } catch (ValidationCheckSkippedException e) {
        summary.add(ValidationResult.info(check, "Skipped check: %s", e.getMessage()));
      } catch (ValidationCheckException e) {
        summary.add(ValidationResult.error(check, "%s", e.getMessage()));
      }
    }

    return summary;
  }

  public ValidationSummary check(CAS cas) throws ValidationException {
    ValidationSummary summary = new ValidationSummary();

    for (ValidationCheck check : checks) {
      try {
        summary.add(ValidationResult.trace(check, "Trying check..."));
        if (check instanceof CasValidationCheck) {
          summary.addAll(((CasValidationCheck) check).validate(cas));
        } else if (check instanceof JCasValidationCheck) {
          try {
            summary.addAll(((JCasValidationCheck) check).validate(cas.getJCas()));
          } catch (CASException e) {
            throw new ValidationException(e);
          }
        } else {
          throw new IllegalArgumentException(
                  "Unknown ValidationCheck type: [" + check.getClass().getName() + "]");
        }
      } catch (ValidationCheckSkippedException e) {
        summary.add(ValidationResult.info(check, "Skipped check: %s", e.getMessage()));
      } catch (ValidationCheckException e) {
        summary.add(ValidationResult.error(check, "%s", e.getMessage()));
      }
    }

    return summary;
  }

  public Collection<ValidationCheck> getChecks() {
    return checks;
  }

  public static class Builder {

    private final Set<ValidationCheck> checks = new LinkedHashSet<>();
    private final Set<Pattern> includePatterns = new HashSet<>();
    private final Set<Class<?>> includeTypes = new HashSet<>();
    private final Set<Pattern> excludePatterns = new HashSet<>();
    private final Set<Class<?>> excludeTypes = new HashSet<>();
    private boolean skipAutoDetection = false;

    /**
     * Add the given check instance to the validator. This allows even adding checks which are not
     * available via the Java Service Locator, which take parameters or which are otherwise stateful
     * (assuming that the resulting validator is not shared between threads).
     * <p>
     * <b>Note:</b> Includes/excludes do also apply do checks added via this method.
     *
     * @param check
     *          a check instance to use.
     */
    public Validator.Builder withCheck(CasValidationCheck check) {
      checks.add(check);
      return this;
    }

    /**
     * Disable auto-detection of checks.
     */
    public Validator.Builder withoutAutoDetectedChecks() {
      skipAutoDetection = true;
      return this;
    }

    /**
     * Enable auto-detection of checks (the default behavior).
     */
    public Validator.Builder withAutoDetectedChecks() {
      skipAutoDetection = false;
      return this;
    }

    /**
     * Skip any checks with the given names. Subtypes of the given classes are not skipped.
     * <p>
     * <b>Note:</b> Excludes are applied after includes.
     *
     * @param className
     *          names of check classes to be excluded.
     */
    public Validator.Builder excludingByName(String... className) {
      stream(className).map(Pattern::quote).map(Pattern::compile).forEach(excludePatterns::add);
      return this;
    }

    /**
     * Skip any checks with names matching the given regular expressions.
     * <p>
     * <b>Note:</b> Excludes are applied after includes.
     *
     * @param patterns
     *          regular expressions matching check class names to be excluded.
     */
    public Validator.Builder excludingByPattern(String... patterns) {
      stream(patterns).map(Pattern::compile).forEach(excludePatterns::add);
      return this;
    }

    /**
     * Skips any checks of the given types (includes checks that are subclasses or implementations
     * of the given types).
     * <p>
     * <b>Note:</b> Excludes are applied after includes.
     *
     * @param types
     *          check type names to be excluded.
     */
    public Validator.Builder excludingByType(Class<?>... types) {
      stream(types).forEach(excludeTypes::add);
      return this;
    }

    /**
     * Retain only checks with the given names. Subtypes of the given classes are not retained.
     * <p>
     * <b>Note:</b> Excludes are applied after includes.
     *
     * @param className
     *          names of check classes to be included.
     */
    public Validator.Builder includingByName(String... className) {
      stream(className).map(Pattern::quote).map(Pattern::compile).forEach(includePatterns::add);
      return this;
    }

    /**
     * Retain any checks with names matching the given regular expressions.
     * <p>
     * <b>Note:</b> Excludes are applied after includes.
     *
     * @param patterns
     *          regular expressions matching check class names to be included.
     */
    public Validator.Builder includingByPattern(String... patterns) {
      stream(patterns).map(Pattern::compile).forEach(includePatterns::add);
      return this;
    }

    /**
     * Retain any checks of the given types (includes checks that are subclasses or implementations
     * of the given types).
     * <p>
     * <b>Note:</b> Excludes are applied after includes.
     *
     * @param types
     *          check type names to be included.
     */
    public Validator.Builder includingByType(Class<?>... types) {
      stream(types).forEach(includeTypes::add);
      return this;
    }

    private Validator.Builder autoDetectChecks() {
      loadServicesSafely(ValidationCheck.class).forEachOrdered(checks::add);
      return this;
    }

    public Validator build() {
      if (!skipAutoDetection) {
        autoDetectChecks();
      }

      if (!includePatterns.isEmpty()) {
        checks.removeIf(check -> includePatterns.stream()
                .noneMatch(p -> p.matcher(check.getClass().getName()).matches()));
      }

      if (!includeTypes.isEmpty()) {
        checks.removeIf(check -> includeTypes.stream()
                .noneMatch(t -> t.isAssignableFrom(check.getClass())));
      }

      if (!excludePatterns.isEmpty()) {
        checks.removeIf(check -> excludePatterns.stream()
                .anyMatch(p -> p.matcher(check.getClass().getName()).matches()));
      }

      if (!excludeTypes.isEmpty()) {
        checks.removeIf(
                check -> excludeTypes.stream().anyMatch(t -> t.isAssignableFrom(check.getClass())));
      }

      return new Validator(checks);
    }
  }
}
