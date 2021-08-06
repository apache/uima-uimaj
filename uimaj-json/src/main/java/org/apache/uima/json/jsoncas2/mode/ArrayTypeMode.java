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

public enum ArrayTypeMode {
  /**
   * Serialize array fields using the the compact {@code TypeName[]} syntax.
   */
  AS_ARRAY_TYPED_RANGE,

  /**
   * Serialize array fields using the traditional {@code range} and {@code elementType} separation
   * that we know from the XML type system descriptors.
   */
  AS_RANGE_AND_ELEMENT;

  public static final String KEY = "UIMA.FeatureStructuresMode";

  public static void set(SerializerProvider aProvider, ArrayTypeMode aMode) {
    aProvider.setAttribute(KEY, aMode);
  }

  public static ArrayTypeMode get(SerializerProvider aProvider) {
    ArrayTypeMode mode = (ArrayTypeMode) aProvider.getAttribute(KEY);
    return mode != null ? mode : AS_RANGE_AND_ELEMENT;
  }
}