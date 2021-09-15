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
package org.apache.uima.json.jsoncas2.mode;

import com.fasterxml.jackson.databind.SerializerProvider;

public enum TypeSystemMode {

  /**
   * Include the full type system in the JSON file.
   */
  FULL,

  /**
   * Include only the types actually used.
   */
  MINIMAL,

  /**
   * Do not include the type system in the JSON file. The reader must obtain the type system by some
   * other means.
   */
  NONE;

  public static final String KEY = "UIMA.TypeSystemMode";

  public static void set(SerializerProvider aProvider, TypeSystemMode aMode) {
    aProvider.setAttribute(KEY, aMode);
  }

  public static TypeSystemMode get(SerializerProvider aProvider) {
    TypeSystemMode mode = (TypeSystemMode) aProvider.getAttribute(KEY);
    return mode != null ? mode : FULL;
  }
}
