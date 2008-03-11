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

package org.apache.uima.resource;

/**
 * A type of <code>ResourceSpecifier</code> that locates an existing <code>Resource</code>
 * service by its URI.
 * 
 * 
 */
public interface URISpecifier extends ResourceServiceSpecifier {

  /**
   * Value for {@link #getResourceType()} representing an Analysis Engine.
   */
  public static final String RESOURCE_TYPE_ANALYSIS_ENGINE = "AnalysisEngine";

  /**
   * Value for {@link #getResourceType()} representing a CAS Consumer.
   */
  public static final String RESOURCE_TYPE_CAS_CONSUMER = "CasConsumer";

  /**
   * Retrieves the type of Resource (e.g. Analysis Engine, CAS Consumer) that the service at this
   * URI is expected to implement. This is optional, but useful for clients to know what to do with
   * the URISpecifier.
   * 
   * @return the type of Resource. This should be one of the constants on this class, or null if the
   *         resource type is not specified.
   */
  public String getResourceType();

  /**
   * Sets the type of Resource (e.g. Analysis Engine, CAS Consumer) that the service at this URI is
   * expected to implement. This is optional, but useful for clients to know what to do with the
   * URISpecifier.
   * 
   * @param aResourceType
   *          the type of Resource. This should be one of the constants on this class, or null to
   *          indicate that the resource type is not specified.
   */
  public void setResourceType(String aResourceType);

  /**
   * Retrieves the URI at which a Resource may be located.
   * 
   * @return a URI string
   */
  public String getUri();

  /**
   * Gets the name of the Protocol used to communicate with the service. Protocol names are defined
   * in the {@link org.apache.uima.Constants} class.
   * 
   * @return the name of the protocol.
   */
  public String getProtocol();

  /**
   * Gets the timeout period in milliseconds. If a call takes longer than this amount of time, an
   * exception will be thrown.
   * 
   * @return the timeout period in milliseconds. A null value indicates that the transport layer's
   *         default value will be used.
   */
  public Integer getTimeout();

  /**
   * Sets the URI at which a Resource may be located.
   * 
   * @param aUri
   *          a URI string
   */
  public void setUri(String aUri);

  /**
   * Sets the name of the Protocol used to communicate with the service. Protocol names are defined
   * in the {@link org.apache.uima.Constants} class.
   * 
   * @param aProtocol
   *          the name of the protocol.
   */
  public void setProtocol(String aProtocol);

  /**
   * Sets the timeout period in milliseconds. If a call takes longer than this amount of time, an
   * exception will be thrown.
   * 
   * @param aTimeout
   *          the timeout period in milliseconds. A null value indicates that the transport layer's
   *          default value will be used.
   */
  public void setTimeout(Integer aTimeout);

  /**
   * @return Returns the Parameters.
   */
  public Parameter[] getParameters();

  /**
   * @param parameters
   *          The Parameters to set.
   */
  public void setParameters(Parameter[] parameters);

}
