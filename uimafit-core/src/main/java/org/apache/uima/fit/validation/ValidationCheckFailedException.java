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
 * Fail the current check.
 */
public class ValidationCheckFailedException extends ValidationCheckException {
  private static final long serialVersionUID = 7135158265431902494L;

  public ValidationCheckFailedException() {
    super();
  }

  public ValidationCheckFailedException(String aMessage, Throwable aCause,
          boolean aEnableSuppression, boolean aWritableStackTrace) {
    super(aMessage, aCause, aEnableSuppression, aWritableStackTrace);
  }

  public ValidationCheckFailedException(String aMessage, Throwable aCause) {
    super(aMessage, aCause);
  }

  public ValidationCheckFailedException(String aMessage) {
    super(aMessage);
  }

  public ValidationCheckFailedException(Throwable aCause) {
    super(aCause);
  }
}
