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

/**
 * An object that contains all
 * {@link org.apache.uima.collection.metadata.CasProcessorDeploymentParam} instances. Provides the
 * means to add, get, and delete deployment parameters.
 * 
 * 
 */
public interface CasProcessorDeploymentParams {
  /**
   * Adds new {@link org.apache.uima.collection.metadata.CasProcessorDeploymentParam} param
   * 
   * @param aParam -
   *          parameter to add
   */
  public void add(CasProcessorDeploymentParam aParam);

  /**
   * Returns {@link org.apache.uima.collection.metadata.CasProcessorDeploymentParam} instance
   * identified by aParamName.
   * 
   * @param aParamName -
   *          name of the parameter to get
   * @return - instance of {@link org.apache.uima.collection.metadata.CasProcessorDeploymentParam}
   * @throws CpeDescriptorException tbd
   */
  public CasProcessorDeploymentParam get(String aParamName) throws CpeDescriptorException;

  /**
   * Returns all instances of
   * {@link org.apache.uima.collection.metadata.CasProcessorDeploymentParam}
   * 
   * @return - array of {@link org.apache.uima.collection.metadata.CasProcessorDeploymentParam}
   *         instances
   */
  public CasProcessorDeploymentParam[] getAll();

  /**
   * Deletes named instance of
   * {@link org.apache.uima.collection.metadata.CasProcessorDeploymentParam}
   * 
   * @param aParam -
   *          parameter to remove
   * @throws CpeDescriptorException tbd
   */
  public void remove(CasProcessorDeploymentParam aParam) throws CpeDescriptorException;
}
