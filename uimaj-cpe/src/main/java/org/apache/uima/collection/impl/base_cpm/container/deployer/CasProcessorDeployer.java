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

package org.apache.uima.collection.impl.base_cpm.container.deployer;

import java.net.URL;
import java.util.List;

import org.apache.uima.collection.impl.base_cpm.container.ProcessingContainer;
import org.apache.uima.resource.ResourceConfigurationException;

/**
 * Base interface for implementing Deployer class used by the CPM to deply Cas Processors.
 * 
 */
public interface CasProcessorDeployer {
  /**
   * Deploys all Cas Processors in <i>aCasProcessorList</i>. The list contains one or more
   * instances of the same Cas Processor.
   * 
   * @param aCasProcessorList -
   *          list of Cas Processors to deploy
   * @param redeploy -
   *          true if the Cas Processor is being redeployed as part of the recovery, false otherwise
   * @return ProcessingContainer managing deployed Cas Processors
   * 
   * @throws ResourceConfigurationException -
   *           failed to deploy Cas Processor
   */
  public ProcessingContainer deployCasProcessor(List aCasProcessorList, boolean redeploy)
          throws ResourceConfigurationException;

  /**
   * Method used to redeploy a single instance of a Cas Processor.
   * 
   * @param aProcessingContainer -
   *          ProcessingContainer managing deployed Cas Processor to redeploy
   * 
   * @throws ResourceConfigurationException -
   *           failed to deploy Cas Processor
   */
  public void deployCasProcessor(ProcessingContainer aProcessingContainer)
          throws ResourceConfigurationException;

  public void undeploy() throws CasProcessorDeploymentException;

  public void undeploy(URL aUrl) throws CasProcessorDeploymentException;

}
