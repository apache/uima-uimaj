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

package org.apache.uima.collection.impl.cpm.container.deployer.socket;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.base_cpm.CasProcessor;
import org.apache.uima.collection.impl.base_cpm.container.CasProcessorConfiguration;
import org.apache.uima.collection.impl.base_cpm.container.ProcessingContainer;
import org.apache.uima.collection.impl.base_cpm.container.deployer.CasProcessorDeployer;
import org.apache.uima.collection.impl.base_cpm.container.deployer.CasProcessorDeploymentException;
import org.apache.uima.collection.impl.cpm.container.CPEFactory;
import org.apache.uima.collection.impl.cpm.container.CasObjectNetworkCasProcessorImpl;
import org.apache.uima.collection.impl.cpm.container.CasProcessorConfigurationJAXBImpl;
import org.apache.uima.collection.impl.cpm.container.ProcessingContainer_Impl;
import org.apache.uima.collection.impl.cpm.container.ServiceProxyPool;
import org.apache.uima.collection.impl.cpm.engine.CPMEngine;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.util.Level;

/**
 * Reference implementation of the {@link CasProcessorDeployer} component responsible for launch and
 * termination of the fenced CasProcessor. It uses a plug-in {@link ProcessControllerAdapter} object
 * to delegate launch requests from the CPM to the external application.
 * 
 */
public class SocketCasProcessorDeployer implements CasProcessorDeployer {
  private CPEFactory cpeFactory = null;

  private URL[] serviceUrls = null; // changed to URL[] since there are multiple instances of

  // service -Adam

  private ProcessControllerAdapter controller = null;

  
  public SocketCasProcessorDeployer(ProcessControllerAdapter aController, CPEFactory aCpeFactory) {
    controller = aController;
    cpeFactory = aCpeFactory;
  }

  public SocketCasProcessorDeployer(ProcessControllerAdapter aController) {
    controller = aController;
  }

  /**
   * Deploys integrated Cas Processor. Number of instances this routine actually deploys depends on
   * number of processing threads defined in the CPE descriptor. There is one instance per
   * processing thread created here. The <i>aCasProcessorList</i> contains instantiated Cas
   * Processors. These are instantiated by the CPEFactory.
   * 
   * @param aCasProcessorList - list containing instantiated Cas Processors
   * @param redeploy - true when redeploying failed Cas Processor
   * 
   * @return - ProcessingContainer containing pool of CasProcessors
   */
  public ProcessingContainer deployCasProcessor(List aCasProcessorList, CPMEngine aEngine,
          boolean redeploy) throws ResourceConfigurationException {
    return deployCasProcessor(aCasProcessorList, redeploy);
  }

  /**
   * Uses ProcessControllerAdapter instance to launch fenced CasProcessor.
   */
  public ProcessingContainer deployCasProcessor(List aCasProcessorList, boolean redeploy)
          throws ResourceConfigurationException {
    String name = null;
    CasProcessor cProcessor = null;
    CasProcessorConfiguration casProcessorConfig = null;
    ProcessingContainer processingContainer = null;
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_deploy_services__FINEST",
              Thread.currentThread().getName());
    }
    if (controller == null) {
      throw new ResourceConfigurationException(
              ResourceInitializationException.CONFIG_SETTING_ABSENT,
              new Object[] { "ProcessControllerAdapter" });
    }
    try {
      // Launch one instance of fenced CasProcessor per pipeline -Adam
      serviceUrls = controller.deploy(name, aCasProcessorList.size());

      ServiceProxyPool casProcessorPool = new ServiceProxyPool();
      // Deploy one Cas Processor at a time in sequential order
      for (int i = 0; i < aCasProcessorList.size(); i++) {
        cProcessor = (CasProcessor) aCasProcessorList.get(i);
        // Container may have already been instantiated. This will be the case if the CPM is
        // configured for concurrent
        // processing ( more than one processing pipeline). There is only one container per
        // CasProcessor type.
        // So each instance of the same CasProcessor will be associated with a single container.
        // Inside the
        // container instances are pooled. When deploying the very first CasProcessor of each type,
        // the
        // container will be created and initialized. Any subsequent deployments of this
        // CasProcessor will
        // simply use it, and will be added to this container's instance pool.
        if (processingContainer == null) {
          ProcessingResourceMetaData metaData = cProcessor.getProcessingResourceMetaData();
          CpeCasProcessor casProcessorType = (CpeCasProcessor) cpeFactory.casProcessorConfigMap
                  .get(metaData.getName());
          // Create a pool to hold instances of CasProcessors. Instances are managed by a container
          // through
          // getCasProcessor() and releaseProcessor() methods.
          // Create CasProcess Configuration holding info defined in the CPE descriptor
          casProcessorConfig = new CasProcessorConfigurationJAXBImpl(casProcessorType, cpeFactory.getResourceManager());

          // Associate CasProcessor configuration from CPE descriptor with this container
          processingContainer = new ProcessingContainer_Impl(casProcessorConfig, metaData,
                  casProcessorPool);
          processingContainer.setCasProcessorDeployer(this);
          processingContainer.setSingleFencedService(true);
          // Instantiate an object that encapsulates CasProcessor configuration
          // Determine deployment model for this CasProcessor
          // Each CasProcessor must have a name
          name = casProcessorConfig.getName();
          if (name == null) {
            if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE,
                      this.getClass().getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_unable_to_read_meta__SEVERE", Thread.currentThread().getName());
            }
            throw new ResourceConfigurationException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_casprocessor_no_name_found__SEVERE", new Object[] { Thread
                            .currentThread().getName() });
          }
        }
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_deploying_service__FINEST",
                  new Object[] { Thread.currentThread().getName(), name });
        }
        // Launch one instance of the fenced CasProcessor
        // URL[] urls = controller.deploy(name, 1); //Commented out by Adam -- deployment is now
        // done outside of loop
        if (cProcessor instanceof CasObjectNetworkCasProcessorImpl) {
          ((CasObjectNetworkCasProcessorImpl) cProcessor).connect(serviceUrls[i]);
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_service_deployed__FINEST",
                    new Object[] { Thread.currentThread().getName(), name });
          }
        }
        // Add CasProcess to the instance pool
        casProcessorPool.addCasProcessor(cProcessor);
      }

      // There is one instance of ProcessingContainer for set of CasProcessors
      if (processingContainer == null) {
        throw new ResourceConfigurationException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_invalid_container__SEVERE", new Object[] { Thread.currentThread()
                        .getName() });
      }
      // Assumption is that the container already exists and it contains CasProcessor configuration
      casProcessorConfig = processingContainer.getCasProcessorConfiguration();
      if (casProcessorConfig == null) {
        throw new ResourceConfigurationException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_container_configuration_not_found__SEVERE", new Object[] { Thread
                        .currentThread().getName() });
      }
    } catch (ResourceConfigurationException e) {
      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, e.getMessage(), e);
      }
      throw e;
    } catch (Exception e) {
      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, e.getMessage(), e);
      }
      throw new ResourceConfigurationException(e);
    }
    return processingContainer;
  }

  /**
   * Uses ProcessControllerAdapter instance to launch fenced CasProcessor.
   */
  public void deployCasProcessor(ProcessingContainer aProcessingContainer)
          throws ResourceConfigurationException {
    try {
      if (aProcessingContainer.isSingleFencedService()) {
        // Makes sure that all CasProcessors are back in the instance pool before
        // doing the restart. pool.getSize() returns the current pool size.
        // pool.getAllInstanceCount() returns number of CasProcessors managed by the pool.
        ServiceProxyPool pool = aProcessingContainer.getPool();
        if (pool == null) {
          if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
            UIMAFramework.getLogger(this.getClass())
                    .logrb(
                            Level.SEVERE,
                            this.getClass().getName(),
                            "initialize",
                            CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                            "UIMA_CPM_no_service_proxy__SEVERE",
                            new Object[] { Thread.currentThread().getName(),
                                aProcessingContainer.getName() });
          }
          throw new ResourceConfigurationException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_no_service_proxy__SEVERE", new Object[] {
                      Thread.currentThread().getName(), aProcessingContainer.getName() });

        }
        int totalPoolSize = pool.getAllInstanceCount();
        while (totalPoolSize != pool.getSize()) {
          try {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(
                      Level.SEVERE,
                      this.getClass().getName(),
                      "initialize",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_wait_for_service_proxy_pool__FINEST",
                      new Object[] { Thread.currentThread().getName(),
                          aProcessingContainer.getName() });
            }
            pool.wait();  // pool has notifyall when it changes the pool.getSize() result
          } catch (Exception e) {
          }
        }
        CasProcessor cProcessor = pool.checkOut();
        if (cProcessor != null && cProcessor instanceof CasObjectNetworkCasProcessorImpl) {

          URL undeployUrl = ((CasObjectNetworkCasProcessorImpl) cProcessor).getEndpoint();
          undeploy(undeployUrl);
          pool.checkIn(cProcessor);
          cProcessor = null;
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass())
                    .logrb(
                            Level.SEVERE,
                            this.getClass().getName(),
                            "initialize",
                            CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                            "UIMA_CPM_deploying_service__FINEST",
                            new Object[] { Thread.currentThread().getName(),
                                aProcessingContainer.getName() });

          }
          // Launch fenced CasProcessor instances (the same number as totalPoolSize)
          serviceUrls = controller.deploy(aProcessingContainer.getName(), totalPoolSize);
          LinkedList casProcessors = new LinkedList();
          // Checkout ALL CasProcessors from the Pool and reconnect them with the fenced service
          try {
            for (int i = 0; i < totalPoolSize; i++) {
              cProcessor = pool.checkOut();
              ((CasObjectNetworkCasProcessorImpl) cProcessor).connect(serviceUrls[i]);
              casProcessors.add(cProcessor);
            }
          } catch (Exception e) {
            if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
              UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, e.getMessage(), e);
            }
            throw e;
          } finally {
            // Now check ALL CasProcessor back into the pool
            while (casProcessors.size() > 0) {
              cProcessor = (CasProcessor) casProcessors.remove(0);
              pool.checkIn(cProcessor);
            }
          }
        }
      }
    } catch (Exception e) {
      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, e.getMessage(), e);
      }
      throw new ResourceConfigurationException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.container.deployer.CasProcessorDeployer#undeploy()
   */
  public void undeploy(URL aURL) throws CasProcessorDeploymentException {
    try {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.SEVERE,
                this.getClass().getName(),
                "initialize",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_undeploying_service__FINEST",
                new Object[] { Thread.currentThread().getName(), aURL.getHost(),
                    String.valueOf(aURL.getPort()) });

      }
      controller.undeploy(aURL);
    } catch (Exception e) {
      throw new CasProcessorDeploymentException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.container.deployer.CasProcessorDeployer#undeploy()
   */
  public void undeploy() throws CasProcessorDeploymentException {
    try {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).log(Level.FINEST, "Undeploying CasProcessor");
      }
      // loop over all service URLs and undeploy them. -Adam
      for (int i = 0; i < serviceUrls.length; i++) {
        controller.undeploy(serviceUrls[i]);
      }
    } catch (Exception e) {
      throw new CasProcessorDeploymentException(e);
    }

  }

}
