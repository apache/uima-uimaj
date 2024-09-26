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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.validation.ValidationResult;
import org.apache.uima.fit.validation.Validator;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

class ManagedCasTest {

  private ManagedCas sut = new ManagedCas();

  @Test
  void thatValidatorIsResetBeforeNextTest() throws Exception {
    Validator defaultValidator = sut.getValidator();
    Validator transientValidator = new Validator.Builder().build();
    sut.withValidator(transientValidator);
    assertThat(sut.getValidator()).isSameAs(transientValidator);
    sut.afterTestExecution(null);
    assertThat(sut.getValidator()).isSameAs(defaultValidator);
  }

  @Test
  void thatCasIsResetBeforeNextTest() throws Exception {
    CAS cas = sut.get();
    cas.setDocumentText("test");
    sut.afterTestExecution(null);
    assertThat(cas.getDocumentText()).isNull();
  }

  @Test
  void thatCasIsResetBeforeNextTestIfValidationFails() throws Exception {
    sut.withValidator(new Validator.Builder()
            .withCheck(cas -> asList(ValidationResult.error(this, "fail"))).build());
    CAS cas = sut.get();
    cas.setDocumentText("test");
    assertThatExceptionOfType(AssertionFailedError.class) //
            .as("Validation should fail") //
            .isThrownBy(() -> sut.afterTestExecution(null));
    assertThat(cas.getDocumentText()) //
            .as("Despite failed validation, CAS should have been reset") //
            .isNull();
  }
}
