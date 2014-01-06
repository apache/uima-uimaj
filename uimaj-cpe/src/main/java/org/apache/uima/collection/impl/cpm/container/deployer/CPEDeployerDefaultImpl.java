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

package org.apache.uima.collection.impl.cpm.container.deployer;

import java.net.URL;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.collection.base_cpm.CasProcessor;
import org.apache.uima.collection.impl.base_cpm.container.CasProcessorConfiguration;
import org.apache.uima.collection.impl.base_cpm.container.ProcessingContainer;
import org.apache.uima.collection.impl.base_cpm.container.deployer.CasProcessorDeployer;
import org.apache.uima.collection.impl.base_cpm.container.deployer.CasProcessorDeploymentException;
import org.apache.uima.collection.impl.cpm.container.CPEFactory;
import org.apache.uima.collection.impl.cpm.container.CasProcessorConfigurationJAXBImpl;
import org.apache.uima.collection.impl.cpm.container.ProcessingContainer_Impl;
import org.apache.uima.collection.impl.cpm.container.ServiceProxyPool;
import org.apache.uima.collection.impl.cpm.engine.CPMEngine;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.CpmLocalizedMessage;
import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceServiceException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;

/**
 * Implements {@link CasProcessorDeployer}. Used to instantiate integrated Cas Processor.
 */
public class CPEDeployerDefaultImpl implements CasProcessorDeployer {
  private ServiceProxyPool casProcessorPool = null;

  private CPEFactory cpeFactory = null;

  private CPMEngine engine;

  /**
   * Initializes this instance with a reference to the CPE configuration
   * 
   * @param aCpeFactory -
   *          reference to CPE configuration
   */
  public CPEDeployerDefaultImpl(CPEFactory aCpeFactory) {
    cpeFactory = aCpeFactory;
  }

  /**
   * Deploys integrated Cas Processor. Number of instances this routine actually deploys depends on
   * number of processing threads defined in the CPE descriptor. There is one instance per
   * processing thread created here. The <i>aCasProcessorList</i> contains instantiated Cas
   * Processors. These are instantiated by the CPEFactory.
   * 
   * @param aCasProcessorList - list containing instantiated Cas Processors
   * @param aEngine the CPM engine
   * @param redeploy - true when redeploying failed Cas Processor
   * 
   * @return - ProcessingContainer containing pool of CasProcessors
   */
  public ProcessingContainer deployCasProcessor(List aCasProcessorList, CPMEngine aEngine,
          boolean redeploy) throws ResourceConfigurationException {
    engine = aEngine;
    return deployCasProcessor(aCasProcessorList, redeploy);
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
  public ProcessingContainer deployCasProcessor(List aCasProcessorList, boolean redeploy)
          throws ResourceConfigurationException {
    String name = null;
    CasProcessor casProcessor = null;
    CasProcessorConfiguration casProcessorConfig = null;
    ProcessingContainer processingContainer = null;
    String deployModel = null;

    try {
      for (int i = 0; i < aCasProcessorList.size(); i++) {
        casProcessor = (CasProcessor) aCasProcessorList.get(i);
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
          ProcessingResourceMetaData metaData = casProcessor.getProcessingResourceMetaData();

          CpeCasProcessor cpeCasProcessor = (CpeCasProcessor) cpeFactory.casProcessorConfigMap
                  .get(metaData.getName());
          if (engine != null) {
            boolean parallelizable = engine.isParallizable(casProcessor, metaData.getName());
            cpeCasProcessor.setIsParallelizable(parallelizable);
          }
          casProcessorPool = new ServiceProxyPool();
          // Instantiate an object that encapsulates CasProcessor configuration
          casProcessorConfig = new CasProcessorConfigurationJAXBImpl(cpeCasProcessor, cpeFactory.getResourceManager());

          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "initialize",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_cp_checkpoint__FINEST",
                    new Object[] { Thread.currentThread().getName(),
                        String.valueOf(casProcessorConfig.getBatchSize()) });// Checkpoint().getBatch())});
          }
          // Associate CasProcessor configuration from CPE descriptor with this container
          processingContainer = new ProcessingContainer_Impl(casProcessorConfig, metaData,
                  casProcessorPool);
          // Determine deployment model for this CasProcessor
          deployModel = casProcessorConfig.getDeploymentType();
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_cp_deployment__FINEST",
                    new Object[] { Thread.currentThread().getName(), deployModel });
          }
          // Each CasProcessor must have a name
          name = casProcessorConfig.getName();
          if (name == null) {
            if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE,
                      this.getClass().getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_cp_no_name__SEVERE",
                      new Object[] { Thread.currentThread().getName() });
            }
            throw new ResourceConfigurationException(
                    InvalidXMLException.REQUIRED_ATTRIBUTE_MISSING, new Object[] { "name",
                        "casProcessor" }, new Exception(CpmLocalizedMessage.getLocalizedMessage(
                            CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                            "UIMA_CPM_EXP_missing_attribute_from_xml_element__WARNING",
                            new Object[] { Thread.currentThread().getName(), "n/a", "name",
                                "casProcessor" })));
          }
        } else {
          // Assumption is that the container already exists and it contains CasProcessor
          // configuration
          casProcessorConfig = processingContainer.getCasProcessorConfiguration();
          if (casProcessorConfig == null) {
            throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND,
                    new Object[] { "<casProcessor>", "<casProcessors>" }, new Exception(
                            CpmLocalizedMessage.getLocalizedMessage(
                                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                                    "UIMA_CPM_EXP_bad_cpe_descriptor_no_cp__WARNING",
                                    new Object[] { Thread.currentThread().getName() })));
          }
        }
        // Add CasProcess to the instance pool
        casProcessorPool.addCasProcessor(casProcessor);

        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_deploying_cp__FINEST",
                  new Object[] { Thread.currentThread().getName(), name });
        }
      }

    } catch (ResourceConfigurationException e) {
      e.printStackTrace();
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new ResourceConfigurationException(ResourceServiceException.RESOURCE_UNAVAILABLE,
              new Object[] {}, e);
    }
    return processingContainer;
  }

  /**
   * Deploys integrated Cas Processor using configuration available in a given Container. This
   * routine is called when the CasProcessor fails and needs to be restarted.
   * 
   * @param aProcessingContainer -
   *          container managing Cas Processor
   */
  public void deployCasProcessor(ProcessingContainer aProcessingContainer)
          throws ResourceConfigurationException {
    try {
      CasProcessorConfiguration casProcessorConfig = aProcessingContainer
              .getCasProcessorConfiguration();
      String name = casProcessorConfig.getName();
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_redeploying_cp__FINEST",
                new Object[] { Thread.currentThread().getName(), name });
      }
      URL descriptorUrl = casProcessorConfig.getDescriptorUrl();
      CasProcessor casProcessor = produceIntegratedCasProcessor(descriptorUrl);
      casProcessorPool.addCasProcessor(casProcessor);
    } catch (ResourceConfigurationException e) {
      e.printStackTrace();
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new ResourceConfigurationException(ResourceServiceException.RESOURCE_UNAVAILABLE,
              new Object[] {}, e);
    }
  }

  /**
   * Creates an instance of integrated Cas Processor from a given descriptor
   * 
   * @param aDescriptor -
   *          Cas Processor descriptor
   * @return - instantiated CasProcessor
   * 
   * @throws ResourceConfigurationException wraps Exception
   */
  private CasProcessor produceIntegratedCasProcessor(URL aDescriptor)
          throws ResourceConfigurationException {
    CasProcessor casProcessor = null;
    try {
      if (aDescriptor != null) {
        ResourceSpecifier resourceSpecifier = cpeFactory.getSpecifier(aDescriptor);

        if (resourceSpecifier instanceof AnalysisEngineDescription) {
          casProcessor = UIMAFramework.produceAnalysisEngine(resourceSpecifier, this.cpeFactory
                  .getResourceManager(), null);
          // casProcessor.
        } else if (resourceSpecifier instanceof CasConsumerDescription) {
          if (cpeFactory.isDefinitionInstanceOf(CasConsumer.class, resourceSpecifier, aDescriptor.toString())) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_producing_cas_consumer__FINEST",
                      new Object[] { Thread.currentThread().getName() });
            }
            casProcessor = UIMAFramework.produceCasConsumer(resourceSpecifier, this.cpeFactory
                    .getResourceManager(), null);
          } else if (cpeFactory.isDefinitionInstanceOf(CasProcessor.class, resourceSpecifier,
                  aDescriptor.toString())) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_producing_cas_data_consumer__FINEST",
                      new Object[] { Thread.currentThread().getName() });
            }
            casProcessor = cpeFactory.produceCasDataConsumer(CasProcessor.class, resourceSpecifier,
                    null);
          }
        }

        // Check if CasProcesser has been instantiated
        if (casProcessor == null) {
          throw new ResourceConfigurationException(ResourceServiceException.RESOURCE_UNAVAILABLE,
                  new Object[] {}, new Exception(CpmLocalizedMessage.getLocalizedMessage(
                          CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                          "UIMA_CPM_EXP_instantiation_exception__WARNING", new Object[] {
                              Thread.currentThread().getName(), "Integrated Cas Processor" })));
        }
      }
    } catch (ResourceConfigurationException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new ResourceConfigurationException(ResourceServiceException.RESOURCE_UNAVAILABLE,
              new Object[] {}, new Exception(CpmLocalizedMessage.getLocalizedMessage(
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_EXP_instantiation_exception__WARNING", new Object[] {
                          Thread.currentThread().getName(), "Integrated Cas Processor" })));
    }
    return casProcessor;

  }

  
  public void undeploy() throws CasProcessorDeploymentException {

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.container.deployer.CasProcessorDeployer#undeploy()
   */
  public void undeploy(URL aURL) throws CasProcessorDeploymentException {
  }

}
