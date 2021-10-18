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

/**
 * String or String Subtype
 */
public class TypeImpl_string extends TypeImpl_primitive { // string considered a primitive e.g. in
                                                          // index comparators

  public TypeImpl_string(String name, TypeSystemImpl tsi, TypeImpl supertype) {
    super(name, tsi, supertype, String.class);
  }

  public TypeImpl_string(String name, TypeSystemImpl tsi, TypeImpl supertype, Class<?> javaType) {
    super(name, tsi, supertype, javaType);
  }

  @Override
  public boolean isStringOrStringSubtype() {
    return true;
  }

  @Override
  public boolean subsumes(TypeImpl ti) {
    if (this.isStringSubtype()) {
      return this == ti; // a string subtype only can subsume itself
    }
    return ti.isStringOrStringSubtype();
  }

}
