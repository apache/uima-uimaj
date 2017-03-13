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
 * An <code>ExternalResourceDependency</code> object describes an resources's requirements on an
 * external resource. This object has four properties:
 * <ul>
 * <li>A key, by which the annotator will identify the resource.</li>
 * <li>A textual description of the resource dependency.</li>
 * <li>The name of a Java interface through which the data will be accessed. This is optional; if
 * not specified, the default {@link org.apache.uima.resource.DataResource} interface will be used.</li>
 * <li>Whether the resource is required or optional.</li>
 * </ul>
 * 
 * 
 */
public interface ExternalResourceDependency extends MetaDataObject {

  /**
   * Retrieves the key by which the resource is identified.
   * 
   * @return the key for this resource.
   */
  public String getKey();

  /**
   * Sets the key by which the resource is identified.
   * 
   * @param aKey
   *          the key for this resource.
   */
  public void setKey(String aKey);

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
   * Retrieves the name of the Java interface through which this resource will be accessed.
   * 
   * @return the name of the Java interface for this external resource, <code>null</code> if none.
   */
  public String getInterfaceName();

  /**
   * Sets the name of the Java interface through which this resource will be accessed.
   * 
   * @param aName
   *          the name of the Java interface for this external resource, <code>null</code> if
   *          none.
   * 
   * @throws org.apache.uima.UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setInterfaceName(String aName);

  /**
   * Gets whether this resource dependency is optional. Dependencies that are not optional must be
   * linked to resource definitions prior to instantiating the Analysis Engine, or an exception will
   * be thrown.
   * 
   * @return true if this resource dependency is optional, false if not
   */
  public boolean isOptional();

  /**
   * Sets whether this resource dependency is optional. Dependencies that are not optional must be
   * linked to resource definitions prior to instantiating the Analysis Engine, or an exception will
   * be thrown.
   * 
   * @param aOptional
   *          true if this resource dependency is optional, false if not
   */
  public void setOptional(boolean aOptional);
}
