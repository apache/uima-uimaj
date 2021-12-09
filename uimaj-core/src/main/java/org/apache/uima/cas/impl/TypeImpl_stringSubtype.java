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

package org.apache.uima.cas.impl;

import java.util.Collections;
import java.util.Set;

import org.apache.uima.cas.CASRuntimeException;

public class TypeImpl_stringSubtype extends TypeImpl_string { // string considered a primitive e.g.
                                                              // in index comparators

  private final Set<String> allowedValues;

  public TypeImpl_stringSubtype(String name, TypeSystemImpl tsi, TypeImpl supertype,
          Set<String> allowedValues) {
    super(name, tsi, supertype);
    this.allowedValues = Collections.unmodifiableSet(allowedValues);
  }

  Set<String> getAllowedValues() {
    return allowedValues;
  }

  void validateIsInAllowedValues(String s) {
    if (s != null && !allowedValues.contains(s)) {
      /** Error setting string value: string "{0}" is not valid for a value of type "{1}". */
      throw new CASRuntimeException(CASRuntimeException.ILLEGAL_STRING_VALUE, s, getName());
    }
  }

  @Override
  public boolean isStringSubtype() {
    return true;
  }
}
