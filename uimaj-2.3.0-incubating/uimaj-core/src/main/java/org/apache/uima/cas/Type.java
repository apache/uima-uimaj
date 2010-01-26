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

package org.apache.uima.cas;

import java.util.List;
import java.util.Vector;

/**
 * The interface describing types in the type system.
 * 
 * <p>
 * <a name="names">Type names</a> are Java strings that look like Java class names. For example,
 * the built-in annotation type is called <code>uima.tcas.Annotation</code>. The whole string is
 * called the (fully) qualified type name. The part after the last period is called the short or
 * base name. The rest of the name is the name space of the type. This part can be empty, in which
 * case the qualified and the base name are identical.
 * 
 * <p>
 * <a name="identifiers">Type system identifiers</a> in general have the following syntax: they are
 * non-empty strings whose first character is a letter (Unicode letter), followed by an arbitrary
 * sequence of letters, digits and underscores. No other characters are legal parts of identifiers.
 * A type name is then a non-empty sequence of identifiers separated by periods. See also <a
 * href="./Feature.html#names">Feature names</a>.
 * 
 * 
 */
public interface Type {

  /**
   * Get the <a href="#names">fully qualified name</a> of the type.
   * 
   * @return The name of the type.
   */
  String getName();

  /**
   * Get the <a href="#names">unqualified, short name</a> of this type.
   * 
   * @return The short name of this type.
   */
  String getShortName();

  /**
   * Get a vector of the features for which this type is a subtype of the features' domain (i.e.,
   * inherited features are also returned). If you need to know which type introduces a feature, use
   * {@link Feature#getDomain() Feature.getDomain}. Features will be listed in no particular order.
   * 
   * @return The Vector of features.
   * @deprecated Use {@link #getFeatures() getFeatures()} instead.
   */
  @Deprecated
  Vector<Feature> getAppropriateFeatures();

  /**
   * Get a vector of the features for which this type is a subtype of the features' domain (i.e.,
   * inherited features are also returned). If you need to know which type introduces a feature, use
   * {@link Feature#getDomain() Feature.getDomain}. Features will be listed in no particular order.
   * 
   * @return The List of features defined for this type.
   */
  List<Feature> getFeatures();

  /**
   * Get the number of features for which this type defines the domain. This includes inherited
   * features.
   * 
   * @return The number of features.
   */
  int getNumberOfFeatures();

  /**
   * Retrieve a feature for this type. Inherited features can also be retrieved this way.
   * 
   * @param featureName
   *          The short, unqualified name of the feature.
   * @return The feature, if it exists; <code>null</code>, else.
   */
  Feature getFeatureByBaseName(String featureName);

  /**
   * Check if type is feature final, i.e., if no more new features may be defined for it.
   * 
   * @return If type is feature final.
   */
  boolean isFeatureFinal();

  /**
   * Check if type is inheritance final, i.e., if new types can be derived from it.
   * 
   * @return If type is inheritance final.
   */
  boolean isInheritanceFinal();

  /**
   * Check if the type is one of the primitive types.
   * 
   * @return <code>true</code> iff type is a primitive type.
   */
  boolean isPrimitive();

  /**
   * Check if the type is an array type.
   * 
   * @return <code>true</code> iff the type is an array type.
   */
  boolean isArray();

  /**
   * For array types, returns the component type of the array type. For all other types, it will
   * return <code>null</code>.
   * 
   * @return The component type of an array type.
   */
  Type getComponentType();

}
