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

/**
 * A simple class containing information on how to render a property in XML.
 */
public class PropertyXmlInfo {
  /**
   * Name of the property (must correspond to a property on this bean according to the JavaBeans
   * spec).
   */
  public String propertyName;

  /**
   * Name if the XML element that represents this property in XML. Defaults to the same value as
   * {@link #propertyName}.
   * <p>
   * If this is <code>null</code>, it indicates that this property is not represented by its own
   * element tag in the XML. Instead, the value of the property determines the XML tag that is
   * generated.
   */
  public String xmlElementName;

  /**
   * If true, this property should be omitted from the XML entirely if its value is null. Defaults
   * to true.
   */
  public boolean omitIfNull;

  /**
   * Only used for properties with array values, this determines the tag name of each array element.
   * As with <code>aXmlName</code>, this may be null, which will cause each array element to have
   * a tag determined by its class.
   */
  public String arrayElementTagName;

  /**
   * Creates a new, default PropertyXmlInfo. The XML name is assumed to be the same as the property
   * name, omitIfNull is true, and arrayElementTagName is null.
   * 
   * @param aPropName
   *          name of the property
   */
  public PropertyXmlInfo(String aPropName) {
    this(aPropName, aPropName, true, null);
  }

  /**
   * Creates a new PropertyXmlInfo.
   * 
   * @param aPropName
   *          name of the property
   * @param aOmitIfNull
   *          if true, this property should be omitted entirely from the XML if its value is null
   */
  public PropertyXmlInfo(String aPropName, boolean aOmitIfNull) {
    this(aPropName, aPropName, aOmitIfNull, null);
  }

  /**
   * Creates a new PropertyXmlInfo.
   * 
   * @param aPropName
   *          name of the property
   * @param aXmlName
   *          name of xml element that represents this property (may be null - see {@link #xmlElementName}).
   */
  public PropertyXmlInfo(String aPropName, String aXmlName) {
    this(aPropName, aXmlName, true, null);
  }

  /**
   * Creates a new PropertyXmlInfo.
   * 
   * @param aPropName
   *          name of the property
   * @param aXmlName
   *          name of xml element that represents this property (may be null - see {@link #xmlElementName}.
   * @param aOmitIfNull
   *          if true, this property should be omitted entirely from the XML if its value is null
   */
  public PropertyXmlInfo(String aPropName, String aXmlName, boolean aOmitIfNull) {
    this(aPropName, aXmlName, aOmitIfNull, null);
  }

  /**
   * Creates a new PropertyXmlInfo.
   * 
   * @param aPropName
   *          name of the property
   * @param aXmlElementName
   *          name of xml element that represents this property (may be null - see
   *          {@link #xmlElementName}.
   * @param aOmitIfNull
   *          if true, this property should be omitted entirely from the XML if its value is null
   * @param aArrayElementTagName
   *          only used for properties with array values, this determines the tag name of each array
   *          element. As with <code>aXmlName</code>, this may be null, which will cause each
   *          array element to have a tag determined by its class.
   */
  public PropertyXmlInfo(String aPropName, String aXmlElementName, boolean aOmitIfNull,
          String aArrayElementTagName) {
    propertyName = aPropName;
    xmlElementName = aXmlElementName;
    omitIfNull = aOmitIfNull;
    arrayElementTagName = aArrayElementTagName;
  }
}
