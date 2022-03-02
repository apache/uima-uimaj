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

/**
 * Interface identifying validation checks that can be located via the Java Service Locator
 * mechanism.
 */
public interface ValidationCheck {

  /**
   * Fail the check with the given message.
   * 
   * @param aMessage
   *          the failure message.
   * @throws ValidationCheckFailedException
   *           an exception carrying the failure message.
   */
  default void fail(String aMessage) throws ValidationCheckFailedException {
    throw new ValidationCheckFailedException(aMessage);
  }

  /**
   * Skip the check with the given reason.
   * 
   * @param aMessage
   *          the skip reason.
   * @throws ValidationCheckSkippedException
   *           an exception carrying the failure message.
   */
  default void skip(String aMessage) throws ValidationCheckSkippedException {
    throw new ValidationCheckSkippedException(aMessage);
  }
}
