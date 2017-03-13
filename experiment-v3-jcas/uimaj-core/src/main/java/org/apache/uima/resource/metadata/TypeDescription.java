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

package org.apache.uima.resource.metadata;

/**
 * A description of a CAS Type. This implements <code>MetaDataObject</code>, which implements
 * {@link org.apache.uima.util.XMLizable}, so it can be serialized to and deserialized from an XML
 * element.
 * 
 * 
 */
public interface TypeDescription extends MetaDataObject {

  public final static TypeDescription[] EMPTY_TYPE_DESCRIPTIONS = new TypeDescription[0];
  /**
   * Gets the name of this Type.
   * 
   * @return the name of this Type
   */
  public String getName();

  /**
   * Sets the name of this Type.
   * 
   * @param aName
   *          the name of this Type
   */
  public void setName(String aName);

  /**
   * Gets the verbose description of this Type.
   * 
   * @return the description of this Type
   */
  public String getDescription();

  /**
   * Sets the verbose description of this Type.
   * 
   * @param aDescription
   *          the description of this Type
   */
  public void setDescription(String aDescription);

  /**
   * Gets the name of the supertype for this Type. This is the Type from which this Type inherits.
   * 
   * @return the name of the supertype for this Type
   */
  public String getSupertypeName();

  /**
   * Sets the name of the supertype for this Type. This is the Type from which this Type inherits.
   * 
   * @param aTypeName
   *          the name of the supertype for this Type
   */
  public void setSupertypeName(String aTypeName);

  /**
   * Gets the descriptions of the features for this Type.
   * 
   * @return the descriptions of the features for this Type.
   */
  public FeatureDescription[] getFeatures();

  /**
   * Sets the descriptions of the features for this Type.
   * 
   * @param aFeatures
   *          descriptions of the features for this Type.
   */
  public void setFeatures(FeatureDescription[] aFeatures);

  /**
   * Gets the allowed values for instances of this Type. This is used only for special "enumerated
   * types" that extend the String type and define a specific set of allowed values. For all other
   * Types this will return <code>null</code>. Note that if a type has allowed values, it may not
   * have features.
   * 
   * @return the allowed values for instances of this Type
   */
  public AllowedValue[] getAllowedValues();

  /**
   * Sets the allowed values for instances of this Type. This is used only for special "enumerated
   * types" that extend the String type and define a specific set of allowed values. For all other
   * Types this property should be <code>null</code>. Note that if a type has allowed values, it
   * may not have features.
   * 
   * @param aAllowedValues
   *          the allowed values for instances of this Type
   */
  public void setAllowedValues(AllowedValue[] aAllowedValues);

  /**
   * Convenience method which adds a FeatureDescription to this TypeDescription.
   * 
   * @param aFeatureName
   *          name of feature to add
   * @param aDescription
   *          verbose description of the feature
   * @param aRangeTypeName
   *          name of feature's range type
   * 
   * @return description of the new Feature
   */
  public FeatureDescription addFeature(String aFeatureName, String aDescription,
          String aRangeTypeName);

  /**
   * Convenience method which adds a FeatureDescription to this TypeDescription. Used for array or
   * list valued features, which have additional attributes.
   * 
   * @param aFeatureName
   *          name of feature to add
   * @param aDescription
   *          verbose description of the feature
   * @param aRangeTypeName
   *          name of feature's range type
   * @param aElementTypeName
   *          type of element expected to be contained in the array or list
   * @param aMultipleReferencesAllowed
   *          whether an array or list that's assigned to this feature can also be referenced from
   *          another feature. This is a Boolean object so that the null value can be used to
   *          represent the case where the user has not specified a value.
   * 
   * @return description of the new Feature
   */
  public FeatureDescription addFeature(String aFeatureName, String aDescription,
          String aRangeTypeName, String aElementTypeName, Boolean aMultipleReferencesAllowed);
}
