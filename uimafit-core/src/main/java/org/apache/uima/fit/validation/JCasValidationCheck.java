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

import java.util.List;

import org.apache.uima.jcas.JCas;

/**
 * JCas validation check.
 * <p>
 * <b>Note:</b> Implementations of this class are typically singletons which are obtained through
 * the Java Service Locator mechanism. This means that the implementations must be stateless to
 * ensure that they can be used by multiple threads concurrently.
 */
@FunctionalInterface
public interface JCasValidationCheck extends ValidationCheck {
  /**
   * Apply this check to the given CAS.
   * 
   * @param cas
   *          the CAS to check.
   * @return the results of the check.
   * @throws ValidationCheckException
   *           if there was a problem performing the validation.
   */
  List<ValidationResult> validate(JCas cas) throws ValidationCheckException;
}
