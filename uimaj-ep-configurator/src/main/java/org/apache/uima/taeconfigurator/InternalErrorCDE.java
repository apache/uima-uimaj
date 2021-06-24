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

package org.apache.uima.taeconfigurator;

/**
 * The Class InternalErrorCDE.
 */
public class InternalErrorCDE extends RuntimeException {

  
  /**
   * Instantiates a new internal error CDE.
   */
  public InternalErrorCDE() {
  }

  /**
   * Instantiates a new internal error CDE.
   *
   * @param message the message
   */
  public InternalErrorCDE(String message) {
    super(message + " - Please see Eclipse Error Log for more information.");
  }

  /**
   * Instantiates a new internal error CDE.
   *
   * @param message the message
   * @param cause the cause
   */
  public InternalErrorCDE(String message, Throwable cause) {
    super(message + " - Please see Eclipse Error Log for more information.", cause);
  }

  /**
   * Instantiates a new internal error CDE.
   *
   * @param cause the cause
   */
  public InternalErrorCDE(Throwable cause) {
    super(cause);
  }

  /** The Constant serialVersionUID. */
  static final long serialVersionUID = 1041388340406853782L;
}
