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
 * An object that holds configuration information used for building CPE Descriptor. It provides the
 * means of configuring deployment parameter used by the CPE to deploy CasProcessor.
 * 
 * 
 */
public interface CasProcessorDeploymentParam extends MetaDataObject {
  /**
   * Sets the name of the parameter
   * 
   * @param aParamName -
   *          a name
   * @throws CpeDescriptorException tbd
   */
  public void setParameterName(String aParamName) throws CpeDescriptorException;

  /**
   * Returns the name of the parameter
   * 
   * @return - name as String
   * @throws CpeDescriptorException tbd
   */
  public String getParameterName() throws CpeDescriptorException;

  /**
   * Sets the value of the deployment parameter
   * 
   * @param aParamValue -
   *          parameter value
   * @throws CpeDescriptorException tbd
   */
  public void setParameterValue(String aParamValue) throws CpeDescriptorException;

  /**
   * Returns deployment parameter value
   * 
   * @return - value as String
   * @throws CpeDescriptorException tbd
   */
  public String getParameterValue() throws CpeDescriptorException;

  /**
   * Sets deployment parameter value type
   * 
   * @param aParamType -
   *          value type (string, int, etc)
   * @throws CpeDescriptorException tbd
   */
  public void setParameterType(String aParamType) throws CpeDescriptorException;

  /**
   * Returns deployment parameter value type
   * 
   * @return - value type (string, int, etc)
   * @throws CpeDescriptorException tbd
   */
  public String getParameterType() throws CpeDescriptorException;
}
