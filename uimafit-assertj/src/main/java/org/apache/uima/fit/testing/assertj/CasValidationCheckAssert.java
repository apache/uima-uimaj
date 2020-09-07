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

import static java.util.ServiceLoader.load;
import static java.util.stream.StreamSupport.stream;

import java.util.ServiceLoader;

import org.apache.uima.fit.validation.CasValidationCheck;
import org.assertj.core.api.AbstractAssert;

/**
 * Asserts related to {@link CasValidationCheck} implementations.
 */
public class CasValidationCheckAssert
        extends AbstractAssert<CasValidationCheckAssert, CasValidationCheck> {
  public CasValidationCheckAssert(CasValidationCheck actual) {
    super(actual, CasValidationCheckAssert.class);
  }

  public static CasValidationCheckAssert assertThat(CasValidationCheck actual) {
    return new CasValidationCheckAssert(actual);
  }

  /**
   * Checks that the check is correctly registered and available to the Java Service Locator.
   */
  public CasValidationCheckAssert isAvailableToServiceLoader() {
    isNotNull();

    ServiceLoader<CasValidationCheck> loader = load(CasValidationCheck.class);
    boolean found = stream(loader.spliterator(), false)
            .anyMatch(check -> check.getClass().equals(actual.getClass()));

    if (!found) {
      failWithMessage(
              "[%s] cannot be found by the service loader. Ensure it is registered in [META-INF/services/%s]",
              actual.getClass(), CasValidationCheck.class.getName());
    }

    return this;
  }
}