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
 * An object that holds configuration that is part of the CPE descriptor. It provides the means of
 * configuring CPE Collection Reader Iterator.
 * 
 */
public interface CpeCollectionReaderIterator extends MetaDataObject {
  /** 
   * @return Descriptor
   */
  public CpeComponentDescriptor getDescriptor();

  /**
   * Associate Descriptor with the Collection Reader Iterator
   * 
   * @param descriptor the component descriptor
   */
  public void setDescriptor(CpeComponentDescriptor descriptor);

  /**
   * @return override parameters. These override parameters in the Collection Reader component
   * descriptor. 
   */
  public CasProcessorConfigurationParameterSettings getConfigurationParameterSettings();

  /**
   * Set parameters that will override params defined in the Collection Reader component descriptor.
   * 
   * @param settings the configuration parameter settings
   */
  public void setConfigurationParameterSettings(CasProcessorConfigurationParameterSettings settings);

  public CpeSofaMappings getSofaNameMappings();

  /**
   * Set Sofa Name Mappings
   * 
   * @param mappings the sofa mappings
   */
  public void setSofaNameMappings(CpeSofaMappings mappings);

}
