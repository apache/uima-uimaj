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

import org.apache.uima.resource.metadata.MetaDataObject;

/**
 * An <code>ExternalResourceDescription</code> object describes a resource that is loaded in the
 * {@link org.apache.uima.resource.ResourceManager} and may be shared between components. This
 * object has four properties:
 * <ul>
 * <li>A name, used to identify the resource.</li>
 * <li>A textual description of the resource.</li>
 * <li>A {@link org.apache.uima.resource.ResourceSpecifier} that describes how to create the
 * resource (for simple data resources this will be a
 * {@link org.apache.uima.resource.FileResourceSpecifier} that contains a URL to the data file.</li>
 * <li>The name of a Java class that implements the specified interface and which also implements
 * {@link org.apache.uima.resource.SharedResourceObject}. </li>
 * </ul>
 * 
 * 
 */
public interface ExternalResourceDescription extends MetaDataObject {

  public static final ExternalResourceDescription[] EMPTY_EXTERNAL_RESORUCE_DESCRIPTIONS = new ExternalResourceDescription[0];
  /**
   * Retrieves the name by which the resource is identified.
   * 
   * @return the name of this resource.
   */
  public String getName();

  /**
   * Sets the name by which the resource is identified.
   * 
   * @param aName
   *          the name of this resource.
   */
  public void setName(String aName);

  /**
   * Retrieves the textual description of the resource.
   * 
   * @return the textual description of the resource.
   */
  public String getDescription();

  /**
   * Retrieves the textual description of the resource.
   * 
   * @param aDescription
   *          the textual description of the resource.
   */
  public void setDescription(String aDescription);

  /**
   * Retrieves the <code>ResourceSpecifier</code> that describes how to create the resource.
   * 
   * @return the <code>ResourceSpecifier</code> for this external resource
   */
  public ResourceSpecifier getResourceSpecifier();

  /**
   * Sets the <code>ResourceSpecifier</code> that describes how to create the resource.
   * 
   * @param aSpecifier
   *          the <code>ResourceSpecifier</code> for this external resource
   * 
   * @throws org.apache.uima.UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setResourceSpecifier(ResourceSpecifier aSpecifier);

  /**
   * Retrieves the name of the Java class to be instantiated from the resource data. This must
   * extend {@link org.apache.uima.resource.SharedResourceObject} as well as the interface specified
   * by {@link ExternalResourceDependency#getInterfaceName()}.
   * 
   * @return the name of the Java class implementing the resource access
   */
  public String getImplementationName();

  /**
   * Sets the name of the Java class to be instantiated from the resource data. This must extend
   * {@link org.apache.uima.resource.SharedResourceObject} as well as the interface specified by
   * {@link ExternalResourceDependency#getInterfaceName()}.
   * 
   * @param aName
   *          the name of the Java class implementing the resource access
   * 
   * @throws org.apache.uima.UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setImplementationName(String aName);

}
