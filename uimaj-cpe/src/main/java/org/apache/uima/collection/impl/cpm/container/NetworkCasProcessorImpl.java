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

package org.apache.uima.collection.impl.cpm.container;

import java.io.IOException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas_data.CasData;
import org.apache.uima.collection.base_cpm.CasDataProcessor;
import org.apache.uima.collection.impl.base_cpm.container.ServiceConnectionException;
import org.apache.uima.collection.impl.cpm.Constants;
import org.apache.uima.collection.impl.cpm.container.deployer.VinciTAP;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.CpmLocalizedMessage;
import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.resource.ResourceServiceException;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.OperationalProperties;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.impl.ConfigurationParameterSettings_impl;
import org.apache.uima.resource.metadata.impl.OperationalProperties_impl;
import org.apache.uima.resource.metadata.impl.ProcessingResourceMetaData_impl;
import org.apache.uima.util.Level;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.impl.ProcessTrace_impl;
import org.apache.vinci.transport.ServiceException;

/**
 * Implementation of the {@link CasDataProcessor} interface used for both Local and Remote
 * CasDataProcessors. The CPE delegates analysis of entities to this instance. Each instance of this
 * class has a proxy to extenal service.
 * 
 * 
 * 
 */
public class NetworkCasProcessorImpl implements CasDataProcessor {
  private static final int DEFAULT_RETRY_COUNT = 3;

  private String name = "";

  private VinciTAP textAnalysisProxy = null;

  private CpeCasProcessor casProcessorType;

  private ProcessingResourceMetaData metadata = null;

  private int retryCount;

  // private String actionOnFailedRetry = "";
  private long totalTime = 0L;

  private ProcessingResourceMetaData resourceMetadata = null;

  /**
   * Initializes this instance with configuration defined in the CPE descriptor.
   * 
   * @param aCasProcessorType
   */
  public NetworkCasProcessorImpl(CpeCasProcessor aCasProcessorType) {
    casProcessorType = aCasProcessorType;
    retryCount = casProcessorType.getErrorHandling().getMaxConsecutiveRestarts().getRestartCount();
    if (retryCount == 0) {
      retryCount = DEFAULT_RETRY_COUNT;
    }

    // Instantiate metadata object to store configuration information
    metadata = new ProcessingResourceMetaData_impl();
    // Each CasProcessor has name
    name = casProcessorType.getName();
    metadata.setName(name);

    OperationalProperties operationalProperties = new OperationalProperties_impl();
    operationalProperties.setModifiesCas(true);
    operationalProperties.setMultipleDeploymentAllowed(true);
    metadata.setOperationalProperties(operationalProperties);

    ConfigurationParameterSettings settings = new ConfigurationParameterSettings_impl();
    settings.setParameterValue(Constants.CAS_PROCESSOR_CONFIG, casProcessorType);
    metadata.setConfigurationParameterSettings(settings);
  }

  /**
   * Associates a proxy to remote annotator service.
   * 
   * @param aTap -
   *          proxy to remote service
   */
  public void setProxy(VinciTAP aTap) {
    textAnalysisProxy = aTap;
    // associate a CONTENT TAG with the proxy. Default tag Detag:DetagContent may be overriden with
    // configuration defined in the CPE descriptor. Each CasProcessor may specify what CONTENT_TAG
    // to use when setting up XCAS for the transmission to the remote annotator.
    // if ( casProcessorType.getContentTag() != null &&
    // casProcessorType.getContentTag().trim().length() > 0 )
    // {
    // textAnalysisProxy.setContentTag(casProcessorType.getContentTag());
    // }

  }

  /**
   * Returns proxy to the remote AE service
   * 
   * @return - proxy to remote service
   */
  public VinciTAP getProxy() {
    return textAnalysisProxy;
  }

  /**
   * Main method used during analysis. The ProcessingUnit calls this method to initiate analysis of
   * the content in the CasData instance. This handles one Cas at a time processing mode.
   * 
   * @param aCas - instance of CasData to analyze
   * @return instance containing result of the analysis
   */
  public CasData process(CasData aCas) throws ResourceProcessException {
    if (textAnalysisProxy == null) {
      throw new ResourceProcessException(new Exception(Thread.currentThread().getName()
              + CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_EXP_no_proxy__WARNING", new Object[] { Thread.currentThread()
                              .getName() })));
    }

    CasData casWithAnalysis = null;
    ProcessTrace pt = new ProcessTrace_impl();
    // Send the content for analysis to the remote service via the proxy
    try {
      long pStart = System.currentTimeMillis();
      casWithAnalysis = textAnalysisProxy.analyze(aCas, pt, name);
      long pEnd = System.currentTimeMillis();
      totalTime += (pEnd - pStart);
    } catch (ServiceException e) {
      // check if the proxy is connected to the service
      if (textAnalysisProxy.isConnected() == false) {
        if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_service_down__INFO",
                  new Object[] { Thread.currentThread().getName(), name });
        }
        throw new ResourceProcessException(new ServiceConnectionException(Thread.currentThread()
                .getName()
                + CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                        "UIMA_CPM_EXP_service_down__WARNING", new Object[] {
                            Thread.currentThread().getName(), name })));
      }
      throw new ResourceProcessException(e);
    } catch (ServiceConnectionException e) {
      throw new ResourceProcessException(e);
    } catch (Exception e) {
      throw new ResourceProcessException(e);
    }
    return casWithAnalysis;
  }

  /**
   * Main method used during analysis. The ProcessingUnit calls this method to initiate analysis of
   * the content in the CasData instance. This handles processing of multiple Cas'es at a time.
   * 
   * @param aCasList - array of CasData instances to analyze
   * @return CasData - array of CasData instances containing results of the analysis
   */
  public CasData[] process(CasData[] aCasList) throws ResourceProcessException {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).log(
              Level.FINEST,
              Thread.currentThread().getName()
                      + " ===================================Calling Proxy");
    }
    if (textAnalysisProxy == null) {
      throw new ResourceProcessException(new Exception(Thread.currentThread().getName()
              + CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_EXP_no_proxy__WARNING", new Object[] { Thread.currentThread()
                              .getName() })));
    }
    try {
      ProcessTrace pt = new ProcessTrace_impl();
      return textAnalysisProxy.analyze(aCasList, pt, name);
    } catch (ServiceConnectionException e) {
      throw new ResourceProcessException(e);
    } catch (ServiceException e) {
      throw new ResourceProcessException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.CasProcessor#isStateless()
   */
  public boolean isStateless() {
    if (resourceMetadata == null) {
      try {
        resourceMetadata = textAnalysisProxy.getAnalysisEngineMetaData();
      } catch (ResourceServiceException e) {
        //can't throw exception from here so just log it and return the default
        UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, e.getMessage(), e);
        return true; 
      }
    }
    OperationalProperties opProps =  resourceMetadata.getOperationalProperties();
    if (opProps != null) {
      return !opProps.isMultipleDeploymentAllowed();
    }
    return true; //default
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.CasProcessor#isReadOnly()
   */
  public boolean isReadOnly() {
    if (resourceMetadata == null) {
      try {
        resourceMetadata = textAnalysisProxy.getAnalysisEngineMetaData();
      } catch (ResourceServiceException e) {
        //can't throw exception from here so just log it and return the default
        UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, e.getMessage(), e);
        return false; 
      }
    }
    OperationalProperties opProps =  resourceMetadata.getOperationalProperties();
    if (opProps != null) {
      return !opProps.getModifiesCas();
    }
    return false; //default  
  }

  /**
   * Returns Remote AE metadata.
   * 
   * This method returns the metadata associated with the annotator.
   */
  public ProcessingResourceMetaData getProcessingResourceMetaData() {
    if (textAnalysisProxy == null) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_use_default_metadata__FINEST",
                new Object[] { Thread.currentThread().getName(), name });
      }
      if (metadata == null) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_no_metadata__FINEST",
                  new Object[] { Thread.currentThread().getName(), name });
        }
      } else if (metadata.getConfigurationParameterSettings().getParameterValue(
              Constants.CAS_PROCESSOR_CONFIG) == null) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_no_cp_configuration_in_metadata__FINEST",
                  new Object[] { Thread.currentThread().getName(), name });
        }
      }
      return metadata;
    }
    try {
      // check if the proxy is connected to the service
      if (textAnalysisProxy.isConnected() == false) {
        if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_service_down__INFO",
                  new Object[] { Thread.currentThread().getName(), name });
        }
        throw new ResourceProcessException(new ServiceConnectionException("Service::" + name
                + " appears to be down"));
      }

      if (resourceMetadata == null) {
        resourceMetadata = textAnalysisProxy.getAnalysisEngineMetaData();
      }
      return resourceMetadata;
    } catch (Exception e) {
      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_unable_to_read_meta__SEVERE",
                new Object[] { Thread.currentThread().getName(), e });
      }
    }
    return metadata;
  }

  /**
   * Notifies Network AE that end-of-batch marker has been reached. The notification can be disabled
   * in the Cpe descriptor by setting batch=0 in the &lt;checkpoint&gt; element.
   * 
   * @see org.apache.uima.collection.base_cpm.CasProcessor#batchProcessComplete(org.apache.uima.util.ProcessTrace)
   */
  public void batchProcessComplete(ProcessTrace aTrace) throws ResourceProcessException,
          IOException {
    // Check if batch noitification is disabled == 0
    if (!doSendNotification()) {
      return;
    }

    if (textAnalysisProxy != null) {
      try {
        textAnalysisProxy.batchProcessComplete();
      } catch (Exception e) {
        throw new ResourceProcessException(e);
      }
    }
  }

  /**
   * This method gets called when the CPM completes processing the collection. Depending on the type
   * of deployment this routine may issue a shutdown command to the service.
   * 
   * @see org.apache.uima.collection.base_cpm.CasProcessor#collectionProcessComplete(org.apache.uima.util.ProcessTrace)
   */
  public void collectionProcessComplete(ProcessTrace aTrace) throws ResourceProcessException,
          IOException {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_stopping_cp__FINEST",
              new Object[] { Thread.currentThread().getName(), name });
    }
    // Send shutdown command to Local services, meaning services started by the CPM on the same
    // machine in a different JVM. Remote services will not be shutdown since these may be started
    // for
    // use by different clients not necessarily just by the CPM
    if (textAnalysisProxy != null) {
      if ("local".equals(casProcessorType.getDeployment().toLowerCase())) {
        // true = send shutdown to remote service
        textAnalysisProxy.shutdown(true, doSendNotification());
      } else {
        // false = dont send shutdown to remote service, just close the client
        textAnalysisProxy.shutdown(false, doSendNotification());
      }
    }
    textAnalysisProxy = null;
    metadata = null;
  }

  /**
   * Determines if CasProcessor wants to be notified when end-of-batch and end-of-processing events
   * happen.
   * 
   * @return - true when notification is required, false otherwise
   */
  private boolean doSendNotification() {
    // Checks if notification is to be sent by looking at the value of batch attribue of the
    // <checkpoint> element
    // in the Cpe descriptor for this Cas Processor.
    if (casProcessorType.getCheckpoint() != null
            && casProcessorType.getCheckpoint().getBatchSize() == 0) {
      return false;
    }
    return true;
  }
}
