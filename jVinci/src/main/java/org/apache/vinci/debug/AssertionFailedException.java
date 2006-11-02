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
 * The AssertionFailedException is thrown by Debug.java in response to failed assertion checks.
 */
public class AssertionFailedException extends RuntimeException {
  private static final long serialVersionUID = 5054530397155879663L;

  /**
   * Standard constructor.
   * 
   * @param message Provides a description of the assertion test that failed.
   */
  public AssertionFailedException(String message) {
    super(message);
  }
}
