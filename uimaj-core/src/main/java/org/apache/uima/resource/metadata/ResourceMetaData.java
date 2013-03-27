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

import org.apache.uima.UIMA_UnsupportedOperationException;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.util.InvalidXMLException;

/**
 * Encapsulates all metadata for a {@link org.apache.uima.resource.Resource}.
 * 
 * As with all {@link MetaDataObject}s, a <code>ResourceMetaData</code> may or may not be
 * modifiable. An application can find out by calling the {@link #isModifiable()} method.
 * 
 * 
 */
public interface ResourceMetaData extends MetaDataObject {

  /**
   * Gets the UUID (Universally Unique Identifier) for this Resource.
   * 
   * @return the UUID for this Resource
   */
  public String getUUID();

  /**
   * Sets the UUID (Universally Unique Identifier) for this Resource.
   * 
   * @param aUUID
   *          the UUID for this Resource
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setUUID(String aUUID);

  /**
   * Gets the name of this Resource.
   * 
   * @return the name of this Resource
   */
  public String getName();

  /**
   * Sets the name of this Resource.
   * 
   * @param aName
   *          the name of this Resource
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setName(String aName);

  /**
   * Gets the version number of this Resource.
   * 
   * @return the version number of this Resource, as a String
   */
  public String getVersion();

  /**
   * Sets the version number of this Resource.
   * 
   * @param aVersion
   *          the version number of this Resource, as a String
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setVersion(String aVersion);

  /**
   * Gets the description of this Resource.
   * 
   * @return the description of this Resource
   */
  public String getDescription();

  /**
   * Sets the description of this Resource.
   * 
   * @param aDescription
   *          the description of this Resource
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setDescription(String aDescription);

  /**
   * Gets the vendor of this Resource.
   * 
   * @return the vendor of this Resource, as a String
   */
  public String getVendor();

  /**
   * Sets the vendor of this Resource.
   * 
   * @param aVendor
   *          the vendor of this Resource, as a String
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setVendor(String aVendor);

  /**
   * Gets the copyright notice for this Resource.
   * 
   * @return the copyright notice for this Resource
   */
  public String getCopyright();

  /**
   * Sets the copyright notice for this Resource.
   * 
   * @param aCopyright
   *          the copyright notice for this Resource
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setCopyright(String aCopyright);

  /**
   * Gets the configuration parameter declarations for this Resource.
   * 
   * @return an object containing the configuration parameter declarations
   */
  public ConfigurationParameterDeclarations getConfigurationParameterDeclarations();

  /**
   * Gets the configuration parameter declarations for this Resource.
   * 
   * @param aDeclarations
   *          an object containing the configuration parameter declarations
   */
  public void setConfigurationParameterDeclarations(ConfigurationParameterDeclarations aDeclarations);

  /**
   * Gets the configuration parameter settings for this Resource.
   * 
   * @return an object containing the settings for this Resource's configuration parameters.
   */
  public ConfigurationParameterSettings getConfigurationParameterSettings();

  /**
   * Sets the configuration parameter settings for this Resource.
   * 
   * @param aSettings
   *          an object containing the settings for this Resource's configuration parameters.
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setConfigurationParameterSettings(ConfigurationParameterSettings aSettings);

  /**
   * Validates configuration parameter settings within this Resource MetaData, and throws an
   * exception if they are not valid.
   * <p>
   * This method checks to make sure that each configuration parameter setting corresponds to an
   * declared configuration parameter, and that the data types are compatible. It does NOT check
   * that all mandatory parameters have been assigned values - this should be done at resource
   * initialization time and not before.
   * 
   * @throws ResourceConfigurationException
   *           if the configuration parameter settings are invalid
   */
  public void validateConfigurationParameterSettings() throws ResourceConfigurationException;

  /**
   * Resolves any import declarations throughout this metadata. This base interface cannot contain
   * any interfaces, but subinterfaces may introduce new properties that can have imports that need
   * to be resolved.
   * 
   * @throws InvalidXMLException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports() throws InvalidXMLException;

  /**
   * Resolves any import declarations throughout this metadata. This base interface cannot contain
   * any interfaces, but subinterfaces may introduce new properties that can have imports that need
   * to be resolved.
   * 
   * @param aResourceManager
   *          the Resource Manager used to locate descriptors imported by name. For example, the
   *          path in which to locate these descriptors can be set via the
   *          {@link ResourceManager#setDataPath(String)} method.
   * 
   * @throws InvalidXMLException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports(ResourceManager aResourceManager) throws InvalidXMLException;

}
