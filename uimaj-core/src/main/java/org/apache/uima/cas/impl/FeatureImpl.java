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

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;

/**
 * The implementation of features in the type system.
 * 
 * 
 * @version $Revision: 1.4 $
 */
public class FeatureImpl implements Feature {

  private final int code;

  private final String name;

  private final TypeSystemImpl ts;

  private final boolean isMultipleRefsAllowed;

  FeatureImpl(int code, String name, TypeSystemImpl ts, boolean isMultipleRefsAllowed) {
    this.code = code;
    this.name = name;
    this.ts = ts;
    this.isMultipleRefsAllowed = isMultipleRefsAllowed;
  }

  /**
   * @return the internal code of this feature. Necessary when using low-level APIs.-
   */
  public int getCode() {
    return this.code;
  }

  /**
   * Get the domain type for this feature.
   * 
   * @return The domain type. This can not be <code>null</code>.
   */
  public Type getDomain() {
    return this.ts.ll_getTypeForCode(this.ts.intro(this.code));
  }

  /**
   * Get the range type for this feature.
   * 
   * @return The range type. This can not be <code>null</code>.
   */
  public Type getRange() {
    return this.ts.ll_getTypeForCode(this.ts.range(this.code));
  }

  /**
   * Get the name for this feature.
   * 
   * @return The name. This can not be <code>null</code>.
   */
  public String getName() {
    return this.name;
  }

  public String toString() {
    return getName();
  }

  public String getShortName() {
    return this.name.substring(this.name.indexOf(TypeSystem.FEATURE_SEPARATOR) + 1, this.name
            .length());
  }

  /**
   * Get the type hierarchy that this feature belongs to.
   * 
   * @return The type hierarchy.
   */
  public TypeSystem getTypeSystem() {
    return this.ts;
  }

  /**
   * Note: you can only compare features from the same type system. If you compare features from
   * different type systems, the result is undefined.
   */
  public int compareTo(Feature o) {
    if (this == o) {
      return 0;
    }
    FeatureImpl f = (FeatureImpl) o;
    return (this.code < f.code) ? -1 : 1;
  }

  public boolean isMultipleReferencesAllowed() {
    return this.isMultipleRefsAllowed;
  }

}
