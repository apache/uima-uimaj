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

import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.URISpecifier;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;

/**
 * Reference implementation of {@link org.apache.uima.resource.URISpecifier}.
 * 
 * 
 */
public class URISpecifier_impl extends MetaDataObject_impl implements URISpecifier {

  static final long serialVersionUID = -7910540167197537337L;

  /** URI of the Resource. */
  private String mUri;

  /** Protocol used to communicate with the Resource. */
  private String mProtocol;

  /** Timeout period in milliseconds. */
  private Integer mTimeout;

  /**
   * Type of Resource that the service at this URI is expected to implement. Value should be one of
   * the constants on the URISpecifier interface, or null if unspecified.
   */
  private String mResourceType;

  private Parameter[] mParameters;

  /**
   * Creates a new <code>URISpecifier_impl</code>.
   */
  public URISpecifier_impl() {
  }

  /**
   * @see org.apache.uima.resource.URISpecifier#getUri()
   */
  @Override
  public String getUri() {
    return mUri;
  }

  /**
   * @see org.apache.uima.resource.URISpecifier#setUri(String)
   */
  @Override
  public void setUri(String aUri) {
    mUri = aUri;
  }

  /**
   * @see org.apache.uima.resource.URISpecifier#getProtocol()
   */
  @Override
  public String getProtocol() {
    return mProtocol;
  }

  /**
   * @see org.apache.uima.resource.URISpecifier#setProtocol(String)
   */
  @Override
  public void setProtocol(String aProtocol) {
    mProtocol = aProtocol;
  }

  /**
   * @see org.apache.uima.resource.URISpecifier#getTimeout()
   */
  @Override
  public Integer getTimeout() {
    return mTimeout;
  }

  /**
   * @see org.apache.uima.resource.URISpecifier#setTimeout(Integer)
   */
  @Override
  public void setTimeout(Integer aTimeout) {
    mTimeout = aTimeout;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.URISpecifier#getResourceType()
   */
  @Override
  public String getResourceType() {
    return mResourceType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.URISpecifier#setResourceType(java.lang.String)
   */
  @Override
  public void setResourceType(String aResourceType) {
    mResourceType = aResourceType;
  }

  /**
   * @return Returns the Parameters.
   */
  @Override
  public Parameter[] getParameters() {
    return mParameters;
  }

  /**
   * @param parameters
   *          The Parameters to set.
   */
  @Override
  public void setParameters(Parameter... parameters) {
    mParameters = parameters;
  }

  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  private static final XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("uriSpecifier",
          new PropertyXmlInfo[] { new PropertyXmlInfo("resourceType"), new PropertyXmlInfo("uri"),
              new PropertyXmlInfo("protocol"), new PropertyXmlInfo("timeout"),
              new PropertyXmlInfo("parameters"), });
}
