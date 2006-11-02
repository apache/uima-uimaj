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

package org.apache.vinci.debug;

/**
 * The FatalException class is used to convert any checked exception into an unchecked exception
 * to terminate the current thread due to unrecoverable and unexpected error conditions.
 * 
 */
public class FatalException extends RuntimeException {
  /**
   * 
   */
  private static final long serialVersionUID = -8889142805115637932L;
  private Throwable         original_exception;

  /**
   * Create an unchecked exception. This should be the default unchecked exception
   * used in Vinci to indicate unrecoverable errors due to unexpected conditions.
   *
   * @param message A textual description of the error.
   */
  public FatalException(String message) {
    super(message);
    original_exception = null;
  }

  /**
   * Convert an exception into an unchecked exception, after appropriately
   * reporting the unchecked exception to the error stream.
   * 
   * @param e The exception to convert to an unchecked FatalException.
   * 
   * @pre e != null
   */
  public FatalException(Throwable e) {
    super(e.getMessage());
    Debug.reportFatalException(e);
    original_exception = e;
  }

  /**
   * Return the original checked exception that was converted into an unchecked exception (if
   * any)
   *
   * @return The original exception, or null if this fatal exception was not the result of
   * a checked exception conversion.
   */
  public Throwable getOriginalException() {
    return original_exception;
  }
}
