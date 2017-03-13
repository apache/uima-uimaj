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

package org.apache.uima.resource.impl;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;

/**
 * Reference implementation of {@link AnalysisEngineDescription}. Note that this class has a
 * slightly nonstandard XML representation because the "key" property is represented in XML by an
 * attribute rather than a child element. 
 * 
 * 9/2013: toXML and buildFromXMLElement not overridden...
 * Therefore, we override the toXML() method and the
 * buildFromXMLElement(Element,XMLParser) method.
 * 
 * 
 */
public class ExternalResourceDescription_impl extends MetaDataObject_impl implements
        ExternalResourceDescription {

  static final long serialVersionUID = -6995615796561255268L;

  private String mName;

  private String mDescription;

  private ResourceSpecifier mResourceSpecifier;

  private String mImplementationName;

  /**
   * @see org.apache.uima.resource.ExternalResourceDescription#getName()
   */
  public String getName() {
    return mName;
  }

  /**
   * @see org.apache.uima.resource.ExternalResourceDescription#getResourceSpecifier()
   */
  public ResourceSpecifier getResourceSpecifier() {
    return mResourceSpecifier;
  }

  /**
   * @see org.apache.uima.resource.ExternalResourceDescription#getImplementationName()
   */
  public String getImplementationName() {
    return mImplementationName;
  }

  /**
   * @see org.apache.uima.resource.ExternalResourceDescription#setName(String)
   */
  public void setName(String aName) {
    mName = aName;
  }

  /**
   * @see org.apache.uima.resource.ExternalResourceDescription#setResourceSpecifier(ResourceSpecifier)
   */
  public void setResourceSpecifier(ResourceSpecifier aSpecifier) {
    mResourceSpecifier = aSpecifier;
  }

  /**
   * @see org.apache.uima.resource.ExternalResourceDescription#setImplementationName(String)
   */
  public void setImplementationName(String aName) {
    mImplementationName = aName;
  }

  /**
   * @see org.apache.uima.resource.ExternalResourceDescription#getDescription()
   */
  public String getDescription() {
    return mDescription;
  }

  /**
   * @see org.apache.uima.resource.ExternalResourceDescription#setDescription(java.lang.String)
   */
  public void setDescription(String aDescription) {
    mDescription = aDescription;
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("externalResource",
          new PropertyXmlInfo[] { new PropertyXmlInfo("name"),
              new PropertyXmlInfo("description", false),
              new PropertyXmlInfo("resourceSpecifier", null),
              new PropertyXmlInfo("implementationName") });
}
