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

import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.impl.ExternalOverrideSettings_impl;


/**
 * A <code>TopLevelSettings</code> defines the values for the descriptor external override parameters

 * 
 * As with all {@link MetaDataObject}s, it may or may not be modifiable.
 * An application can find out by calling the {@link #isModifiable()} method.
 * 
 * 
 */
public interface ExternalOverrideSettings extends MetaDataObject {


  /**
   * Gets the imports holding external override settings.
   * 
   * @return an object containing the settings, or null if no import was used
   */
  public Import[] getImports();

  /**
   * Sets the imports that hold external override settings.
   * 
   * @param aImports
   *          an object containing the import information, or null if no import is to be used
   */
  public void setImports(Import[] aImports);
  
  /**
   * Gets the inline settings
   * 
   * @return a multi-line string of properties assignments
   */
  public String getSettings();

  /**
   * Sets the inline settings
   * 
   * @param settings
   *          a string of new-line separated property assignments
   */
  public void setSettings(String settings);

  /* (non-Javadoc)
   * Look up value for external name from the external override settings
   */
  public String resolveExternalName(String name) throws ResourceConfigurationException;
  
  /**
   * Processes the entries in the ExternalOverrideSettings element, if there is one. 
   * Loads the Settings object with any inline settings and imports
   * 
   * @throws ResourceConfigurationException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports() throws ResourceConfigurationException;

  /**
   * Processes the entries in the ExternalOverrideSettings element, if there is one. 
   * Loads the Settings object with any inline settings and imports
   * 
   * @param aResourceManager
   *          the Resource Manager used to locate an file imported by name
   * 
   * @throws ResourceConfigurationException
   *           if either the import target does not exist or is invalid
   */
  public void resolveImports(ResourceManager aResourceManager) throws ResourceConfigurationException;
  
  /**
   * Sets a link to the next higher priority overrides inherited from a parent descriptor
   * 
   * @param parent
   */
  public void setParentOverrides(ExternalOverrideSettings parent);
}
