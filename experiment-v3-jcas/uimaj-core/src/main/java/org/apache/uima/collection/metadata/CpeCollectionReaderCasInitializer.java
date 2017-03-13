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

import org.apache.uima.resource.metadata.MetaDataObject;

/**
 * An object that holds configuration that is part of the CPE descriptor. Provides the means of
 * setting descriptor path containing CasInitializer configuration
 * 
 * 
 * @deprecated As of v2.0 CAS Initializers are deprecated.
 */
@Deprecated
public interface CpeCollectionReaderCasInitializer extends MetaDataObject {
  /**
   * Sets descriptor path containing configuration for the CasInitializer
   * 
   * @param aDescriptor -
   *          descriptor path
   */
  public void setDescriptor(CpeComponentDescriptor aDescriptor);

  /**
   * Returns {@link org.apache.uima.collection.metadata.CpeComponentDescriptor} containing
   * CasInitializer descriptor path.
   * 
   * @return {@link org.apache.uima.collection.metadata.CpeComponentDescriptor}
   */
  public CpeComponentDescriptor getDescriptor();

  /**
   * Returns {@link org.apache.uima.collection.metadata.CasProcessorConfigurationParameterSettings}
   * object containing overrides to parameter settings for this CasInitializer.
   * 
   * @return - object containing parameter setting overrides
   */
  public CasProcessorConfigurationParameterSettings getConfigurationParameterSettings();

  /**
   * Sets the {@link org.apache.uima.collection.metadata.CasProcessorConfigurationParameterSettings}
   * object containing overrides to parameter settings for this CasInitializer.
   * 
   * @param aSettings
   *          object containing parameter setting overrides
   * @throws CpeDescriptorException tbd         
   */
  public void setConfigurationParameterSettings(CasProcessorConfigurationParameterSettings aSettings)
          throws CpeDescriptorException;

  public CpeSofaMappings getSofaNameMappings();

  /**
   * @param mappings the sofa mappings
   */
  public void setSofaNameMappings(CpeSofaMappings mappings);

}
