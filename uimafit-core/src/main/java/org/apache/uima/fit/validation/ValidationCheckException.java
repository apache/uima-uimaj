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
 * Exception by a particular validation check. These exceptions should not abort the validation but
 * rather be rendered as results for the particular check.
 */
public abstract class ValidationCheckException extends ValidationException {
  private static final long serialVersionUID = -5685006985598972648L;

  public ValidationCheckException() {
    super();
  }

  public ValidationCheckException(String aMessage, Throwable aCause, boolean aEnableSuppression,
          boolean aWritableStackTrace) {
    super(aMessage, aCause, aEnableSuppression, aWritableStackTrace);
  }

  public ValidationCheckException(String aMessage, Throwable aCause) {
    super(aMessage, aCause);
  }

  public ValidationCheckException(String aMessage) {
    super(aMessage);
  }

  public ValidationCheckException(Throwable aCause) {
    super(aCause);
  }
}
