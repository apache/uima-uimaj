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

import org.apache.uima.util.impl.XMLParser_impl;

/**
 * A simple class used to describe how to render this object as XML.
 */
public class XmlizationInfo {
  /**
   * The tag name of the XML element that represents this object.
   */
  public String elementTagName;

  /**
   * The namespace of the XML element, null if none.
   */
  public String namespace;

  /**
   * Information about how this object's properties are represented in XML. The order of the
   * properties in this array defines the order in which they will be written to the XML.
   */
  public PropertyXmlInfo[] propertyInfo;

  /**
   * Creates an XmlizationInfo.
   * 
   * @param aElementTagName
   *          tag name of XML element that represents this object
   * @param aNamespace
   *          the namespace of the XML element, null if none
   * @param aPropInfo
   *          information about how to represent this object's properties
   */
  public XmlizationInfo(String aElementTagName, String aNamespace, PropertyXmlInfo[] aPropInfo) {
    elementTagName = aElementTagName;
    namespace = aNamespace;
    propertyInfo = aPropInfo;
  }

  /**
   * Creates an XmlizationInfo. Namespace defaults to XMLParser_impl.RESOURCE_SPECIFIER_NAMESPACE.
   * 
   * @param aElementTagName
   *          tag name of XML element that represents this object
   * @param aPropInfo
   *          information about how to represent this object's properties
   */
  public XmlizationInfo(String aElementTagName, PropertyXmlInfo[] aPropInfo) {
    this(aElementTagName, XMLParser_impl.RESOURCE_SPECIFIER_NAMESPACE, aPropInfo);
  }
}
