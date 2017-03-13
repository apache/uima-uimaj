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

package org.apache.uima.resource.metadata.impl;

import org.apache.uima.resource.metadata.FeatureDescription;

/**
 * Reference implementation of {@link FeatureDescription}. Under construction.
 * 
 * 
 */
public class FeatureDescription_impl extends MetaDataObject_impl implements FeatureDescription {

  static final long serialVersionUID = 3661516916992500406L;

  /** Feature name. */
  private String mName;

  /** Verbose description of this Feature. */
  private String mDescription;

  /** Name of Range Type of this Feature. */
  private String mRangeTypeName;

  private String mElementType;

  private Boolean mMultipleReferencesAllowed;

  /**
   * Creates a new FeatureDescription_impl with null field values.
   */
  public FeatureDescription_impl() {
  }

  /**
   * Creates a new FeatureDescription_impl with the specified field values.
   * 
   * @param aName
   *          name of the feature
   * @param aDescription
   *          verbose description of the feature
   * @param aRangeTypeName
   *          name of the feature's range type
   */
  public FeatureDescription_impl(String aName, String aDescription, String aRangeTypeName) {
    setName(aName);
    setDescription(aDescription);
    setRangeTypeName(aRangeTypeName);
  }

  /**
   * Creates a new FeatureDescription_impl with the specified field values. This version is used for
   * array or list valued features, which may have additional attributes.
   * 
   * @param aName
   *          name of the feature
   * @param aDescription
   *          verbose description of the feature
   * @param aRangeTypeName
   *          name of the feature's range type
   * @param aElementTypeName
   *          type of element expected to be contained in the array or list
   * @param aMultipleReferencesAllowed
   *          whether an array or list that's assigned to this feature can also be referenced from
   *          another feature. This is a Boolean object so that the null value can be used to
   *          represent the case where the user has not specified a value.
   */
  public FeatureDescription_impl(String aName, String aDescription, String aRangeTypeName,
          String aElementTypeName, Boolean aMultipleReferencesAllowed) {
    setName(aName);
    setDescription(aDescription);
    setRangeTypeName(aRangeTypeName);
    setElementType(aElementTypeName);
    setMultipleReferencesAllowed(aMultipleReferencesAllowed);
  }

  /**
   * @see FeatureDescription#getName()
   */
  public String getName() {
    return mName;
  }

  /**
   * @see FeatureDescription#setName(String)
   */
  public void setName(String aName) {
    mName = aName;
  }

  /**
   * @see FeatureDescription#getDescription()
   */
  public String getDescription() {
    return mDescription;
  }

  /**
   * @see FeatureDescription#setDescription(java.lang.String)
   */
  public void setDescription(String aDescription) {
    mDescription = aDescription;
  }

  /**
   * @see FeatureDescription#getRangeTypeName()
   */
  public String getRangeTypeName() {
    return mRangeTypeName;
  }

  /**
   * @see FeatureDescription#setRangeTypeName(String)
   */
  public void setRangeTypeName(String aTypeName) {
    mRangeTypeName = aTypeName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.FeatureDescription#getElementType()
   */
  public String getElementType() {
    return mElementType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.FeatureDescription#isMultipleReferencesAllowed()
   */
  public Boolean getMultipleReferencesAllowed() {
    return mMultipleReferencesAllowed;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.FeatureDescription#setElementType(java.lang.String)
   */
  public void setElementType(String aElementType) {
    mElementType = aElementType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.FeatureDescription#setMultipleReferencesAllowed(Boolean)
   */
  public void setMultipleReferencesAllowed(Boolean aAllowed) {
    mMultipleReferencesAllowed = aAllowed;
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("featureDescription",
          new PropertyXmlInfo[] { new PropertyXmlInfo("name"),
              new PropertyXmlInfo("description", false),
              new PropertyXmlInfo("rangeTypeName", true), new PropertyXmlInfo("elementType", true),
              new PropertyXmlInfo("multipleReferencesAllowed") });
}
