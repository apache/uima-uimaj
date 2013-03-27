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

package org.apache.uima.cas_data;

import java.io.Serializable;

/**
 * An object in the CAS. Each FeatureStructure has an optional ID, a type (represented as a string),
 * and a collection of features, which are attribute-value pairs. Feature names are strings, and
 * their values may be primitives (String, integer, float) or references (via ID) to another
 * FeatureStructures. Circular references are allowed.
 * <p>
 * Arrays are represented by the subtypes {@link PrimitiveArrayFS} and {@link ReferenceArrayFS}.
 * Arrays are not primitive values. This means that if the value of a feature is conceptually, for
 * example, an integer array, this will be represented in the CasData as a reference, via ID, to a
 * PrimitiveArrayFS object that actually contains the integer array value.
 * <p>
 * FeatureStructures also have a property <code>indexed</code>, which determines whether the
 * FeatureStructure should be added to the CAS's indexes if the CAS Data is converted to a CAS
 * Object. The CasData itself does not provide indexes.
 * 
 * 
 */
public interface FeatureStructure extends Serializable// extends FeatureValue
{
  /**
   * Gets the ID of this FeatureStructure. IDs are optional, so this may return null. A
   * FeatureStructure must have an ID if it is to be the target of a reference.
   * 
   * @return this FeatureStructure's ID, null if none
   */
  public String getId();

  /**
   * Sets the ID of this FeatureStructure. IDs are optional, so null may be passed to this method. A
   * FeatureStructure must have an ID if it is to be the target of a reference.
   * 
   * @param aId
   *          the ID to assign to this FeatureStructure, null if none. It is the caller's
   *          responsibiltiy to ensure that this ID is unique within the CasData containing this
   *          FeatureStructure.
   */
  public void setId(String aId);

  /**
   * Gets the type of this FeatureStructure
   * 
   * @return this FeatureStructure's type, as a string
   */
  public String getType();

  /**
   * Sets the type of this FeatureStructure
   * 
   * @param aType
   *          this FeatureStructure's type, as a string
   */
  public void setType(String aType);

  /**
   * Gets the names of all features on this FeatureStructure.
   * 
   * @return an array of feature names
   */
  public String[] getFeatureNames();

  /**
   * Gets the value of a feature
   * 
   * @param aName
   *          name of feature
   * 
   * @return value of feature named <code>aName</code>, or null if there is no such feature
   */
  public FeatureValue getFeatureValue(String aName);

  /**
   * Sets the value of a feature
   * 
   * @param aName
   *          name of feature to set
   * @param aValue
   *          value of feature
   */
  public void setFeatureValue(String aName, FeatureValue aValue);

  /**
   * Gets whether this FeatureStructure should be indexed if the CasData is converted to a CAS
   * Object. The CasData itself does not provide indexes.
   * 
   * @return true if this FS should be indexed, false if not
   * @deprecated Use {@link #getIndexed()} instead
   */
  @Deprecated
  public boolean isIndexed();

  /**
   * Sets whether this FeatureStructure should be indexed if the CasData is converted to a CAS
   * Object. The CasData itself does not provide indexes.
   * 
   * @param aIndexed
   *          true if this FS should be indexed, false if not
   * @deprecated Use {@link #setIndexed(int[])} instead
   */
  @Deprecated
  public void setIndexed(boolean aIndexed);

  /**
   * Gets the index repositories that this FeatureStrucutre should be indexed in if the CasData is
   * converted to a CAS Object. The CasData itself does not provide indexes.
   * 
   * @return an array containing the numbers of the index repsositories that should contain this FS.
   *         Returns an empty array if this FS is not indexed.
   */
  public int[] getIndexed();

  /**
   * Sets the index repositories that this FeatureStrucutre should be indexed in if the CasData is
   * converted to a CAS Object. The CasData itself does not provide indexes.
   * 
   * @param aIndexed
   *          an array containing the numbers of the index repsositories that should contain this
   *          FS. Passing null is equivalent to passing an empty array.
   */
  public void setIndexed(int[] aIndexed);
}
