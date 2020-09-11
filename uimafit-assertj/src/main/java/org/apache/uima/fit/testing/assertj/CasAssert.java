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

import static org.apache.uima.fit.validation.ValidationResult.Severity.ERROR;

import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.validation.ValidationException;
import org.apache.uima.fit.validation.ValidationResult;
import org.apache.uima.fit.validation.ValidationSummary;
import org.apache.uima.fit.validation.Validator;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Fail;

/**
 * Asserts related to the {@link CAS}.
 */
public class CasAssert
    extends AbstractAssert<CasAssert, CAS>
{

    public CasAssert(CAS actual)
    {
        super(actual, CasAssert.class);
    }

    public static CasAssert assertThat(CAS actual)
    {
        return new CasAssert(actual);
    }

    /**
     * Checks that the check is correctly registered and available to the Java Service Locator.
     */
    public CasAssert isValid()
    {
        isNotNull();

        Validator validator = new Validator.Builder().build();
        try {
            ValidationSummary summary = validator.check(actual);

            String messageBuffer = summary.getResults().stream()
                    .filter(result -> result.getSeverity().isEquallyOrMoreSevereThan(ERROR))
                    .map(ValidationResult::getMessage).collect(Collectors.joining("\n"));

            if (messageBuffer.length() > 0) {
                Fail.fail(messageBuffer);
            }
        }
        catch (ValidationException e) {
            Fail.fail("Unable to validate CAS", e);
        }

        return this;
    }
}