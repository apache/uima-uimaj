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

package org.apache.uima.collection.metadata;

import java.net.URL;

import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.MetaDataObject;

/**
 * An object that holds configuration that is part of the CPE descriptor. Provides the means of
 * setting component descriptor file path containing configuration
 * 
 * 
 */
public interface CpeComponentDescriptor extends MetaDataObject {
  /**
   * Sets component's descriptor file path
   * 
   * @param aInclude -
   *          {@link org.apache.uima.collection.metadata.CpeInclude} containing file path
   */
  public void setInclude(CpeInclude aInclude);

  /**
   * Returns component's descriptor file path
   * 
   * @return {@link org.apache.uima.collection.metadata.CpeInclude}
   */
  public CpeInclude getInclude();
  
  /** 
   * Gets the Import object that declares where the component descriptor is located.
   * Import objects support locating the component descriptor either using a
   * path that's relative to the CPE descriptor's location ("import by location")
   * or using the classpath/datapath ("import by name").
   * 
   * @return the import, null if none
   */
  public Import getImport();
  
  /** 
   * Sets the Import object that declares where the component descriptor is located.
   * Import objects support locating the component descriptor either using a
   * path that's relative to the CPE descriptor's location ("import by location")
   * or using the classpath/datapath ("import by name").
   * 
   * @param aImport the import, null if none
   */
  public void setImport(Import aImport);
  
  /**
   * Returns the absolute URL where the component descriptor is located.  This will use either the
   * include or import property, whichever is specified.
   * 
   * @param aResourceManager
   *          resource manager to use to do import-by-name lookups
   * 
   * @return the absolute URL of the component descriptor
   * 
   * @throws ResourceConfigurationException
   *           if an import could not be resolved
   */
  public URL findAbsoluteUrl(ResourceManager aResourceManager) throws ResourceConfigurationException;  
}
