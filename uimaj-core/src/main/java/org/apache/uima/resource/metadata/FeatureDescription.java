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
 * A description of a CAS feature. This implements <code>MetaDataObject</code>, which implements
 * {@link org.apache.uima.util.XMLizable}, so it can be serialized to and deserialized from an XML
 * element.
 * 
 * 
 */
public interface FeatureDescription extends MetaDataObject {

  /**
   * Gets the name of this Feature.
   * 
   * @return the name of this Feature
   */
  public String getName();

  /**
   * Sets the name of this Feature.
   * 
   * @param aName
   *          the name of this Feature
   */
  public void setName(String aName);

  /**
   * Gets the verbose description of this Feature.
   * 
   * @return the description of this Feature
   */
  public String getDescription();

  /**
   * Sets the verbose description of this Feature.
   * 
   * @param aDescription
   *          the description of this Feature
   */
  public void setDescription(String aDescription);

  /**
   * Gets the name of the range Type of this Feature.
   * 
   * @return the name of the range Type of this Feature
   */
  public String getRangeTypeName();

  /**
   * Sets the name of the range Type of this Feature.
   * 
   * @param aTypeName
   *          the name of the range Type of this Feature
   */
  public void setRangeTypeName(String aTypeName);

  /**
   * For a feature with a range type that is an array or list, gets the expected type of the
   * elements of that array or list. This is optional; if omitted the array or list can contain any
   * type. There is currently no guarantee that the framework will enforce this type restriction.
   * This property should not be set for features whose range type is not an array or list.
   * 
   * @return the expected element type of an array or list feature, null if there is no restriction.
   */
  public String getElementType();

  /**
   * For a multi-valued (array or list) feature, sets the expected type of the elements of that
   * array or list. This is optional; if omitted the array or list can contain any type. There is
   * currently no guarantee that the framework will enforce this type restriction. This property
   * should not be set for features whose range type is not an array or list.
   * 
   * @param aElementType
   *          the expected element type of an array or list feature, null if there is no
   *          restriction.
   */
  public void setElementType(String aElementType);

  /**
   * For a feature with a range type that is an array or list, gets whether value of the feature may
   * also be referenced from another feature elsewhere in the CAS.
   * <p>
   * This returns a Boolean object so that we can distinguish whether the descriptor contained no
   * setting (null) versus an explicit setting of false. We want to preserve this if the descriptor
   * is written out again.
   * <p>
   * Setting this to false (the default) indicates that this feature has exclusive ownership of the
   * array or list, so changes to the array or list are localized. Setting this to true indicates
   * that the array or list may be shared, so changes to it may affect other objects in the CAS.
   * <p>
   * There is currently no guarantee that the framework will enforce this restriction. However, this
   * setting may affect how the CAS is serialized.
   * <p>
   * This property should always be null for features whose range type is not an array or list.
   * 
   * @return true if multiple references to an array or list are allowed, false if not.
   */
  public Boolean getMultipleReferencesAllowed();

  /**
   * For a feature with a range type that is an array or list, sets whether value of the feature may
   * also be referenced from another feature elsewhere in the CAS.
   * <p>
   * This takes a Boolean object so that we can distinguish whether the descriptor contained no
   * setting (null) versus an explicit setting of false. We want to preserve this if the descriptor
   * is written out again.
   * <p>
   * Setting this to false (the default) indicates that this feature has exclusive ownership of the
   * array or list, so changes to the array or list are localized. Setting this to true indicates
   * that the array or list may be shared, so changes to it may affect other objects in the CAS.
   * <p>
   * There is currently no guarantee that the framework will enforce this restriction. However, this
   * setting may affect how the CAS is serialized.
   * <p>
   * This property should never be set for features whose range type is not an array or list.
   * 
   * @param aAllowed
   *          true if multiple references to an array or list are allowed, false if not.
   */
  public void setMultipleReferencesAllowed(Boolean aAllowed);
}
