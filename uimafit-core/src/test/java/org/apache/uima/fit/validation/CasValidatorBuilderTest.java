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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.fit.validation.checks.EndAfterBeginCheckForTesting;
import org.apache.uima.fit.validation.checks.EndSameAsBeginCheckForTesting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CasValidatorBuilderTest {

  private Validator.Builder sut;

  @BeforeEach
  public void setup() {
    sut = new Validator.Builder();
  }

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void thatExcludeByNameWorks() {
    sut.excludingByName(EndAfterBeginCheckForTesting.class.getName());

    Validator validator = sut.build();

    assertThat(validator.getChecks()).extracting(Object::getClass)
            .containsExactly((Class) EndSameAsBeginCheckForTesting.class);
  }

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void thatExcludeByTypeWorks() {
    sut.excludingByType(EndAfterBeginCheckForTesting.class);

    Validator validator = sut.build();

    assertThat(validator.getChecks()).extracting(Object::getClass)
            .containsExactly((Class) EndSameAsBeginCheckForTesting.class);
  }
}
