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

import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.resource.metadata.AllowedValue;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;

/**
 * Reference implementation of {@link TypeDescription}. Under construction.
 * 
 * 
 */
public class TypeDescription_impl extends MetaDataObject_impl implements TypeDescription {

  static final long serialVersionUID = 7505580429981863281L;

  /** Name of this Type. */
  private String mName;

  /** Verbose description of this Type. */
  private String mDescription;

  /** Name of the Type from which this Type inherits. */
  private String mSupertypeName;

  /** Descriptions of all Features defined on this Type. */
  private FeatureDescription[] mFeatures = new FeatureDescription[0];

  /** Allowed Values for an Enumerated type. */
  private AllowedValue[] mAllowedValues = new AllowedValue[0];

  /**
   * Creates a new TypeDescription_impl with null field values.
   */
  public TypeDescription_impl() {
  }

  /**
   * Creates a new TypeDescription_impl with the specified field values.
   * 
   * @param aName
   *          name of the Type
   * @param aDescription -
   * @param aSupertypeName
   *          name of the type's supertype
   */
  public TypeDescription_impl(String aName, String aDescription, String aSupertypeName) {
    setName(aName);
    setDescription(aDescription);
    setSupertypeName(aSupertypeName);
  }

  /**
   * @see TypeDescription#getName()
   */
  public String getName() {
    return mName;
  }

  /**
   * @see TypeDescription#setName(String)
   */
  public void setName(String aName) {
    mName = aName;
  }

  /**
   * @see TypeDescription#getDescription()
   */
  public String getDescription() {
    return mDescription;
  }

  /**
   * @see TypeDescription#setDescription(java.lang.String)
   */
  public void setDescription(String aDescription) {
    mDescription = aDescription;
  }

  /**
   * @see TypeDescription#getSupertypeName()
   */
  public String getSupertypeName() {
    return mSupertypeName;
  }

  /**
   * @see TypeDescription#setSupertypeName(String)
   */
  public void setSupertypeName(String aTypeName) {
    mSupertypeName = aTypeName;
  }

  /**
   * @see TypeDescription#getFeatures()
   */
  public FeatureDescription[] getFeatures() {
    return mFeatures;
  }

  /**
   * @see TypeDescription#setFeatures(FeatureDescription[])
   */
  public void setFeatures(FeatureDescription[] aFeatures) {
    if (aFeatures == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aFeatures", "setFeatures" });
    }
    mFeatures = aFeatures;
  }

  /**
   * @see TypeDescription#getAllowedValues()
   */
  public AllowedValue[] getAllowedValues() {
    return mAllowedValues;
  }

  /**
   * @see TypeDescription#setAllowedValues(AllowedValue[])
   */
  public void setAllowedValues(AllowedValue[] aAllowedValues) {
    mAllowedValues = aAllowedValues;
  }

  /**
   * @see TypeDescription#addFeature(String, String, String)
   */
  public FeatureDescription addFeature(String aFeatureName, String aDescription,
          String aRangeTypeName) {
    return addFeature(aFeatureName, aDescription, aRangeTypeName, null, null);
  }

  /**
   * @see TypeDescription#addFeature(String, String, String, String, Boolean)
   */
  public FeatureDescription addFeature(String aFeatureName, String aDescription,
          String aRangeTypeName, String aElementTypeName, Boolean aMultipleReferencesAllowed) {
    // create new feature description
    FeatureDescription newFeature = new FeatureDescription_impl(aFeatureName, aDescription,
            aRangeTypeName, aElementTypeName, aMultipleReferencesAllowed);

    // add to array
    FeatureDescription[] features = getFeatures();
    if (features == null) {
      setFeatures(new FeatureDescription[] { newFeature });
    } else {
      FeatureDescription[] newArray = new FeatureDescription[features.length + 1];
      System.arraycopy(features, 0, newArray, 0, features.length);
      newArray[features.length] = newFeature;
      setFeatures(newArray);
    }

    return newFeature;
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("typeDescription",
          new PropertyXmlInfo[] { new PropertyXmlInfo("name"),
              new PropertyXmlInfo("description", false), new PropertyXmlInfo("supertypeName"),
              new PropertyXmlInfo("features"), new PropertyXmlInfo("allowedValues") });
}
