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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;

/**
 * The implementation of types in the type system.
 * 
 * 
 * @version $Revision: 1.3 $
 */
public class TypeImpl implements Type, Comparable<TypeImpl> {

  private final String name;

  private final int code;

  private final TypeSystemImpl ts;

  private boolean isFeatureFinal;

  private boolean isInheritanceFinal;

  /**
   * Create a new type. This should only be done by a <code>TypeSystemImpl</code>.
   */
  TypeImpl(String name, int code, TypeSystemImpl ts) {
    super();
    this.name = name;
    this.code = code;
    this.ts = ts;
    this.isInheritanceFinal = false;
    this.isFeatureFinal = false;
  }

  /**
   * Get the name of the type.
   * 
   * @return The name of the type.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Get the super type.
   * 
   * @return The super type or null for Top.
   */
  public Type getSuperType() {
    return this.ts.ll_getTypeForCode(this.ts.ll_getParentType(this.code));
  }

  public String toString() {
    return getName();
  }

  /**
   * Get a vector of the features for which this type is the domain. Features will be returned in no
   * particular order.
   * 
   * @return The vector.
   * @deprecated
   */
  @Deprecated
  public Vector<Feature> getAppropriateFeatures() {
    return new Vector<Feature>(getFeatures());

  }

  /**
   * Get the number of features for which this type defines the range.
   * 
   * @return The number of features.
   */
  public int getNumberOfFeatures() {
    return this.ts.ll_getAppropriateFeatures(this.code).length;
  }

  /**
   * Check if this is an annotation type.
   * 
   * @return <code>true</code>, if <code>this</code> is an annotation type; <code>false</code>,
   *         else.
   */
  public boolean isAnnotationType() {
    return false;
  }

  // /** Find out if this is a built-in type.
  // @return <code>true</code> iff this is a built-in type.
  // */
  // boolean isBuiltinType();

  /**
   * Get the type hierarchy that this type belongs to.
   * 
   * @return The type hierarchy.
   */
  public TypeSystem getTypeSystem() {
    return this.ts;
  }

  /**
   * Return the internal integer code for this type. This is only useful if you want to work with
   * the low-level API.
   * 
   * @return The internal code for this type, <code>&gt;=0</code>.
   */
  public int getCode() {
    return this.code;
  }

  /**
   * Note: you can only compare types from the same type system. If you compare types from different
   * type systems, the result is undefined.
   */
  public int compareTo(TypeImpl t) {
    if (this == t) {
      return 0;
    }

    return (this.code < t.code) ? -1 : 1;
  }

  /**
   * @see org.apache.uima.cas.Type#getFeatureByBaseName(String)
   */
  public Feature getFeatureByBaseName(String featureName) {
    return this.ts.getFeatureByFullName(this.name + TypeSystem.FEATURE_SEPARATOR + featureName);
  }

  /**
   * @see org.apache.uima.cas.Type#getShortName()
   */
  public String getShortName() {
    final int pos = this.name.lastIndexOf(TypeSystem.NAMESPACE_SEPARATOR);
    if (pos >= 0) {
      return this.name.substring(pos + 1, this.name.length());
    }
    return this.name;
  }

  /**
   * @see org.apache.uima.cas.Type#isPrimitive()
   */
  public boolean isPrimitive() {
    return !(this.getTypeSystem().getLowLevelTypeSystem().ll_isRefType(this.code));
  }

  /**
   * @see org.apache.uima.cas.Type#isFeatureFinal()
   */
  public boolean isFeatureFinal() {
    return this.isFeatureFinal;
  }

  /**
   * @see org.apache.uima.cas.Type#isInheritanceFinal()
   */
  public boolean isInheritanceFinal() {
    return this.isInheritanceFinal;
  }

  void setFeatureFinal() {
    this.isFeatureFinal = true;
  }

  void setInheritanceFinal() {
    this.isInheritanceFinal = true;
  }

  /**
   * @deprecated
   * @param featureName -
   * @return -
   */
  @Deprecated
  public Feature getFeature(String featureName) {
    return getFeatureByBaseName(featureName);
  }

  /**
   * guaranteed to be non-null, but might be empty list
   * @return -
   */
  public List<Feature> getFeatures() {
    int[] feats = this.ts.ll_getAppropriateFeatures(this.code);
    List<Feature> list = new ArrayList<Feature>(feats.length);
    for (int i = 0; i < feats.length; i++) {
      list.add(this.ts.ll_getFeatureForCode(feats[i]));
    }
    return list;
  }

  public boolean isArray() {
    return this.ts.ll_isArrayType(this.code);
  }

  public Type getComponentType() {
    if (!isArray()) {
      return null;
    }
    return this.ts.ll_getTypeForCode(this.ts.ll_getComponentType(this.code));
  }

}
