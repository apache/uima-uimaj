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
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.cas_data.CasData;
import org.apache.uima.collection.base_cpm.AbortCPMException;
import org.apache.uima.collection.base_cpm.AbortCasProcessorException;
import org.apache.uima.collection.base_cpm.CasProcessor;
import org.apache.uima.collection.base_cpm.SkipCasException;
import org.apache.uima.collection.impl.CasConsumerDescription_impl;
import org.apache.uima.collection.impl.base_cpm.container.CasProcessorConfiguration;
import org.apache.uima.collection.impl.base_cpm.container.CasProcessorController;
import org.apache.uima.collection.impl.base_cpm.container.KillPipelineException;
import org.apache.uima.collection.impl.base_cpm.container.ProcessingContainer;
import org.apache.uima.collection.impl.base_cpm.container.RunnableContainer;
import org.apache.uima.collection.impl.base_cpm.container.ServiceConnectionException;
import org.apache.uima.collection.impl.base_cpm.container.deployer.CasProcessorDeployer;
import org.apache.uima.collection.impl.cpm.Constants;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.CpmLocalizedMessage;
import org.apache.uima.collection.impl.cpm.utils.Filter;
import org.apache.uima.collection.impl.cpm.vinci.DATACasUtils;
import org.apache.uima.internal.util.StringUtils;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.impl.ConfigurationParameterSettings_impl;
import org.apache.uima.util.Level;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.impl.ProcessTrace_impl;

/**
 * Manages a pool of CasProcessor instances. Provides access to CasProcessor instance to Processing
 * Thread. Processing threads check out an instance of Cas Processor and when done invoking its
 * process() method return it back to pool. The container aggregates counts and totals on behalf of
 * all instances of Cas Processor. It also manages error and restart thresholds for Cas Processors
 * as a group. Errors are aggregated for all instances of Cas Processor as a group NOT individually.
 * The container takes appropriate actions when threshold are exceeded. What action is taken depends
 * on declaritive specification in the cpe descriptor.
 */

public class ProcessingContainer_Impl extends ProcessingContainer implements RunnableContainer {

  /** The Constant CONTAINER_SLEEP_TIME. */
  private static final int CONTAINER_SLEEP_TIME = 100;

  /** The cas processor status. */
  private int casProcessorStatus = CasProcessorController.NOTINITIALIZED;

  /** The config params. */
  private ConfigurationParameterSettings configParams = null;

  /** The is local. */
  private boolean isLocal = false;

  /** The is remote. */
  private boolean isRemote = false;

  /** The is integrated. */
  private boolean isIntegrated = false;

  /** The batch counter. */
  private long batchCounter = 0;

  /** The error counter. */
  private int errorCounter = 0;

  /** The sample counter. */
  private long sampleCounter = 0;

  /** The failure threshold sample. */
  private long failureThresholdSample = 0;

  /** The configured error rate. */
  private int configuredErrorRate = 0;

  /** The batch size. */
  private int batchSize = 1;

  /** The processed. */
  private long processed = 0;

  /** The restart count. */
  private int restartCount = 0;

  /** The cas processor CPE configuration. */
  // private casProcessor casProcessorConfiguration = null;
  private CasProcessorConfiguration casProcessorCPEConfiguration = null;

  /** The bytes in. */
  private long bytesIn = 0L;

  /** The bytes out. */
  private long bytesOut = 0L;

  /** The retry count. */
  private int retryCount = 0;

  /** The abort count. */
  private int abortCount = 0;

  /** The filtered count. */
  private int filteredCount = 0;

  /** The remaining. */
  private long remaining = -1;

  /** The processed entity ids. */
  private Stack processedEntityIds = new Stack();

  /** The total time. */
  private long totalTime = 0L;

  /** The filter list. */
  private LinkedList filterList = null;

  /** The log path. */
  private String logPath = null;

  /** The logger. */
  private Logger logger = null;

  /** The initialized. */
  private boolean initialized = false;

  /** The last cas. */
  private Object lastCas = null;

  /** The cas processor pool. */
  public ServiceProxyPool casProcessorPool;

  /** The cas P deployer. */
  private CasProcessorDeployer casPDeployer = null;

  /** The metadata. */
  private ProcessingResourceMetaData metadata = null;

  /** The stat map. */
  private HashMap statMap = new HashMap();

  // access this only under monitor lock
  /** The is paused. */
  // monitor.notifyall called when switching from true -> false
  private boolean isPaused = false;

  /** The single fenced instance. */
  private boolean singleFencedInstance = false;

  /** The lock for is paused. */
  // This lock used only for isPaused field
  private final Object lockForIsPaused = new Object();

  /** The processor name. */
  private String processorName = null;

  /** The fetch time. */
  private long fetchTime = 0;

  /** The failed cas processor list. */
  public LinkedList failedCasProcessorList = new LinkedList();

  /**
   * Initialize container with CasProcessor configuration and pool containing instances of
   * CasProcessor instances.
   *
   * @param aCasProcessorConfig
   *          - CasProcessor configuration as defined in cpe descriptor
   * @param aMetaData
   *          the a meta data
   * @param aCasProcessorPool
   *          - pool of CasProcessor instances
   * @throws ResourceConfigurationException
   *           the resource configuration exception
   */
  public ProcessingContainer_Impl(CasProcessorConfiguration aCasProcessorConfig,
          ProcessingResourceMetaData aMetaData, ServiceProxyPool aCasProcessorPool)
          throws ResourceConfigurationException {
    isRemote = Constants.DEPLOYMENT_REMOTE.equals(aCasProcessorConfig.getDeploymentType());

    casProcessorPool = aCasProcessorPool;
    casProcessorCPEConfiguration = aCasProcessorConfig;
    configParams = new ConfigurationParameterSettings_impl();
    setMetadata(aMetaData);
    batchSize = casProcessorCPEConfiguration.getBatchSize();
    failureThresholdSample = casProcessorCPEConfiguration.getErrorSampleSize();
    configuredErrorRate = casProcessorCPEConfiguration.getErrorRate();
    filterList = casProcessorCPEConfiguration.getFilter();
  }

  /**
   * Returns component's input/output capabilities.
   *
   * @return the metadata
   */
  public ProcessingResourceMetaData getMetadata() {
    return metadata;
  }

  /**
   * Sets component's input/output capabilities.
   *
   * @param aMetadata
   *          component capabilities
   */
  @Override
  public void setMetadata(ProcessingResourceMetaData aMetadata) {
    metadata = aMetadata;
    Capability[] capabilities = metadata.getCapabilities();
    for (int j = 0; capabilities != null && j < capabilities.length; j++) {
      Capability capability = capabilities[j];
      TypeOrFeature[] tORf = capability.getInputs();
      if (tORf != null) {
        String newKey;
        boolean modified = false;
        // Convert the types if necessary
        for (int i = 0; i < tORf.length; i++) {
          newKey = tORf[i].getName();
          if (tORf[i].getName()
                  .indexOf(org.apache.uima.collection.impl.cpm.Constants.SHORT_DASH_TERM) > -1) {
            newKey = StringUtils.replaceAll(tORf[i].getName(),
                    org.apache.uima.collection.impl.cpm.Constants.SHORT_DASH_TERM,
                    org.apache.uima.collection.impl.cpm.Constants.LONG_DASH_TERM);
            modified = true;
          }
          if (tORf[i].getName()
                  .indexOf(org.apache.uima.collection.impl.cpm.Constants.SHORT_COLON_TERM) > -1) {
            newKey = StringUtils.replaceAll(tORf[i].getName(),
                    org.apache.uima.collection.impl.cpm.Constants.SHORT_COLON_TERM,
                    org.apache.uima.collection.impl.cpm.Constants.LONG_COLON_TERM);
            modified = true;
          }
          if (modified) {
            tORf[i].setName(newKey);
            modified = false;
          }
        }
      }
    }

  }

  /**
   * Plug in deployer object used to launch/deploy the CasProcessor instance. Used for restarts.
   * 
   * @param aDeployer
   *          - object responsible for deploying/launching CasProcessor
   */
  @Override
  public void setCasProcessorDeployer(CasProcessorDeployer aDeployer) {
    casPDeployer = aDeployer;
  }

  /**
   * Returns deployer object used to launch the CasProcessor.
   *
   * @return - CasProcessorDeployer - deployer object
   */
  @Override
  public CasProcessorDeployer getDeployer() {
    return casPDeployer;
  }

  /**
   * Deploy Container specific logger to capture any exceptions occuring during CasProcessor(s)
   * lifespan. Each CasProcessor may have its own log. It is optional and available when there is a
   * parameter called 'containerLogPath' in the <deploymentParameters> section of the cpe
   * descriptor. If
   */
  private void deployLogger() {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_deploy_logger__FINEST",
              new Object[] { Thread.currentThread().getName(), getName() });
    }
    logPath = casProcessorCPEConfiguration.getDeploymentParameter("containerLogPath");

    if (logPath != null && logPath.trim().length() > 0) {
      logger = Logger.getLogger("cpm.container." + getName(), null);
      try {
        // Transalate backslash to a valid path separator. Tried to use replace but it kept
        // throwing Exceptions. Using brute force than.
        if (logPath.indexOf("\\") > -1) {
          char[] tmp = new char[logPath.length()];
          for (int i = 0; i < logPath.length(); i++) {
            if (logPath.charAt(i) == '\\') {
              tmp[i] = new Character(System.getProperty("file.separator").charAt(0));
            } else {
              tmp[i] = logPath.charAt(i);
            }
          }
          logPath = new String(tmp);
        }
        if (logPath.lastIndexOf(System.getProperty("file.separator")) != logPath.length()) {
          logPath = logPath + System.getProperty("file.separator");
        }
        Handler fh = new FileHandler(logPath + getName().replace(' ', '_') + ".log");
        Logger.getLogger("").addHandler(fh);
      } catch (Exception e) {
        logger = null;
        e.printStackTrace();
      }
    } else {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_logpath_not_defined__FINEST",
                new Object[] { Thread.currentThread().getName(), getName() });
      }
    }

  }

  /**
   * Logs Cas'es that could not be processed.
   * 
   * @param abortedCasList
   *          - an arrar of Cas'es that could not be processed by this CasProcessor
   * 
   */
  @Override
  public void logAbortedCases(Object[] abortedCasList) {
    if (!initialized) {
      initialized = true;
      deployLogger();
    }
    // check if there is a log associated with this Container
    if (logger != null) {

      for (int i = 0; i < abortedCasList.length; i++) {
        if (abortedCasList[i] != null && abortedCasList[i] instanceof CasData) {
          String value = DATACasUtils.getFeatureValueByType((CasData) abortedCasList[i],
                  org.apache.uima.collection.impl.cpm.Constants.METADATA_KEY,
                  org.apache.uima.collection.impl.cpm.Constants.DOC_ID);
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_log_docid__FINEST",
                    new Object[] { Thread.currentThread().getName(), getName(), value });

          }
          logger.info(value);
        }
      }
    }
  }

  /**
   * Resets sample counter. This counter is used to determine acceptable error rates associated with
   * hosted CasProcessor. Error rates are measured based on error rate in a given sample: 3 per 1000
   * for example, where 3 is an error rate and 1000 is a sample size.
   * 
   */
  private void resetSampleCounter() {
    sampleCounter = 0;
  }

  /**
   * Resets batch counter. This counter is used to determine if the hosted CasProcessor should do
   * special processing. A CasProcessor may buffer all Cas's in memory and only when its batch size
   * is reached, it does something usefull with them, like save them to a file, index them, etc
   * 
   */
  private void resetBatchCounter() {
    batchCounter = 0;
  }

  /**
   * Returns total number of bytes ingested so far by all CasProcessor instances managed by this
   * container.
   * 
   * @return - bytes processed
   */
  @Override
  public long getBytesIn() {
    return bytesIn;
  }

  /**
   * Aggregate total bytes ingested by the CasProcessor.
   * 
   * @param aBytesIn
   *          - number of ingested bytes
   */
  @Override
  public void addBytesIn(long aBytesIn) {
    bytesIn += aBytesIn;
  }

  /**
   * Returns total number of bytes processed so far by all CasProcessor instances managed by this
   * container.
   * 
   * @return - bytes processed
   */
  @Override
  public long getBytesOut() {
    return bytesOut;
  }

  /**
   * Aggregate total bytes processed by this CasProcessor.
   *
   * @param aBytesOut
   *          the a bytes out
   */
  @Override
  public void addBytesOut(long aBytesOut) {
    bytesOut += aBytesOut;
  }

  /**
   * Increment number of times the casProcessor was restarted due to failures.
   *
   * @param aCount
   *          - restart count
   */
  @Override
  public void incrementRestartCount(int aCount) {
    restartCount += aCount;
  }

  /**
   * Returns total number of all CasProcessor restarts.
   * 
   * @return number of restarts
   */
  @Override
  public int getRestartCount() {
    return restartCount;
  }

  /**
   * Increments number of times CasProceesor failed analyzing Cas'es due to timeout or some other
   * problems.
   *
   * @param aCount
   *          - failure count
   */
  @Override
  public void incrementRetryCount(int aCount) {
    retryCount += aCount;
  }

  /**
   * Return the up todate number of retries recorded by the container.
   * 
   * @return - retry count
   */
  @Override
  public int getRetryCount() {
    return retryCount;
  }

  /**
   * Increment number of aborted Cas'es due to inability to process the Cas.
   *
   * @param aCount
   *          - number of aborts while processing Cas'es
   */
  @Override
  public void incrementAbortCount(int aCount) {
    abortCount += aCount;
  }

  /**
   * Return the up todate number of aborts recorded by the container.
   *
   * @return - number of failed attempts to analyze CAS'es
   */
  @Override
  public int getAbortCount() {
    return abortCount;
  }

  /**
   * Increments number of CAS'es filtered by the CasProcessor. Filtered CAS'es dont contain required
   * features. Features that are required by the Cas Processor to perform analysis. Dependant
   * feateurs are defined in the filter expression in the CPE descriptor
   * 
   * @param aCount
   *          - number of filtered Cas'es
   */
  @Override
  public void incrementFilteredCount(int aCount) {
    filteredCount += aCount;
  }

  /**
   * Returns number of filtered Cas'es.
   *
   * @return # of filtered Cas'es
   */
  @Override
  public int getFilteredCount() {
    return filteredCount;
  }

  /**
   * Returns number of entities still to be processed by the CasProcessor It is a delta of total
   * number of entities to be processed by the CPE minus number of entities processed so far.
   * 
   * @return Number of entities yet to be processed
   */
  @Override
  public synchronized long getRemaining() {
    return remaining;
  }

  /**
   * Copies number of entities the CasProcessor has yet to process.
   * 
   * @param aRemainingCount
   *          - number of entities to process
   */
  @Override
  public synchronized void setRemaining(long aRemainingCount) {
    remaining = aRemainingCount;
  }

  /**
   * Copies id of the last entity processed by the CasProcessor.
   *
   * @param aEntityId
   *          - id of the entity
   */
  @Override
  public void setLastProcessedEntityId(String aEntityId) {
    processedEntityIds.push(aEntityId);
  }

  /**
   * Returns id of the last entity processed by the CasProcessor.
   *
   * @return - id of entity
   */
  @Override
  public String getLastProcessedEntityId() {
    if (processedEntityIds.isEmpty()) {
      return "";
    }
    return (String) processedEntityIds.lastElement();
  }

  /**
   * Copies the last Cas Processed.
   *
   * @param aCasObject
   *          the new last cas
   * @deprecated
   */
  @Override
  @Deprecated
  public void setLastCas(Object aCasObject) {
    lastCas = aCasObject;
  }

  /**
   * Returns the last Cas processed.
   *
   * @return the last cas
   * @deprecated
   */
  @Override
  @Deprecated
  public Object getLastCas() {
    return lastCas;
  }

  /**
   * Increment processed.
   *
   * @param aIncrement
   *          the a increment
   */
  public void incrementProcessed(int aIncrement) {
    processed += aIncrement;
  }

  /**
   * Used when recovering from checkpoint, sets the total number of entities before CPE stopped.
   * 
   * @param aProcessedCount
   *          - number of entities processed before CPE stopped
   */
  @Override
  public void setProcessed(long aProcessedCount) {
    processed = aProcessedCount;
  }

  /**
   * Returns number of entities processed so far.
   * 
   * @return - processed - number of entities processed
   */
  @Override
  public long getProcessed() {
    return processed;

  }

  /**
   * Re-initializes the error counter.
   */
  private void resetErrorCounter() {
    errorCounter = 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.impl.base_cpm.container.ProcessingContainer#resetRestartCount()
   */
  @Override
  public void resetRestartCount() {
    // System.out.println("resetRestartCount() was called");
    restartCount = 0;
  }

  /**
   * Increments total time spend in the process() method of the CasProcessor.
   *
   * @param aTime
   *          - total time in process()
   */
  @Override
  public void incrementTotalTime(long aTime) {
    totalTime += aTime;
  }

  /**
   * Returns total time spent in process().
   *
   * @return - number of millis spent in process()
   */
  @Override
  public long getTotalTime() {
    return totalTime;
  }

  /**
   * Returns true if maximum threshold for errors has been exceeded and the CasProcessor is
   * configured to force CPE shutdown. It looks at the value of the action attribute of the
   * &lt;errorRateThreshold&gt; element in the cpe descriptor.
   * 
   * @return - true if the CPE should stop processing, false otherwise
   */
  @Override
  public boolean abortCPMOnError() {
    String action = casProcessorCPEConfiguration.getActionOnError();
    if (action == null) {
      return false;
    }
    return (Constants.TERMINATE_CPE.equals(action.toLowerCase()));
  }

  /**
   * Returns true if the Exception cause is SocketTimeoutException.
   *
   * @param aThrowable
   *          - Exception to check for SocketTimeoutException
   * @return - true if Socket Timeout, false otherwise
   */
  private boolean isTimeout(Throwable aThrowable) {
    Throwable cause = aThrowable.getCause().getCause();
    if (cause != null && cause instanceof SocketTimeoutException) {
      return true;
    }
    return false;
  }

  /**
   * This routine determines what to do with an exception thrown during the CasProcessor processing.
   * It interprets given exception and throws a new one according to configuration specified in the
   * CPE descriptor. It examines provided thresholds and determines if the CPE should continue to
   * run, if it should disable the CasProcessor (and all its instances), or disregard the error and
   * continue.
   *
   * @param aThrowable
   *          - exception to examine
   * @throws Exception
   *           the exception
   */
  @Override
  public synchronized void incrementCasProcessorErrors(Throwable aThrowable) throws Exception {
    errorCounter++;
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_increment_cp_errors__FINEST",
              new Object[] { Thread.currentThread().getName(), getName() });
    }
    if (System.getProperty("DEBUG_EXCEPTIONS") != null) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_show_cp_error_count__FINEST",
                new Object[] { Thread.currentThread().getName(), getName(),
                    String.valueOf(errorCounter), aThrowable.getCause().getMessage() });
      }

    }

    if (aThrowable instanceof ResourceProcessException) {
      if (aThrowable.getCause() instanceof AbortCPMException) {
        if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_abort_cpm__SEVERE",
                  new Object[] { Thread.currentThread().getName(), getName() });
        }
        throw (AbortCPMException) aThrowable.getCause();
      } else if (aThrowable.getCause() instanceof AbortCasProcessorException) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_disable_cp__SEVERE",
                  new Object[] { Thread.currentThread().getName(), getName() });
        }
        throw (AbortCasProcessorException) aThrowable.getCause();
      }
      // If the errors dont exceed configurable threshold already try to ServiceConnection
      // exception.
      else if ((errorCounter < configuredErrorRate)
              && aThrowable.getCause() instanceof ServiceConnectionException
              && !isTimeout(aThrowable)) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_service_connection_exception__SEVERE",
                  new Object[] { Thread.currentThread().getName(), getName() });
        }
        // Increment number of CasProcessor restarts (redeploys)
        restartCount++;
        // get from configuration max restart count
        int rC = casProcessorCPEConfiguration.getMaxRetryCount();

        // Check if configured max restart count has been reached
        if (restartCount > rC) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_max_restart_reached__FINEST", new Object[] {
                        Thread.currentThread().getName(), getName(), String.valueOf(rC) });
          }
          // get from configuration action to be taken if max restart count reached
          String actionOnMaxRestarts = casProcessorCPEConfiguration.getActionOnMaxRestart();
          restartCount = 0;
          // Throw appropriate exception based on configured action when max restarts are reached
          if (Constants.TERMINATE_CPE.equals(actionOnMaxRestarts)) {
            if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_terminate_due_to_action__SEVERE",
                      new Object[] { Thread.currentThread().getName(), getName() });
            }
            throw new AbortCPMException(CpmLocalizedMessage.getLocalizedMessage(
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_configured_to_abort__WARNING",
                    new Object[] { Thread.currentThread().getName(), getName() }));
          } else if (Constants.DISABLE_CASPROCESSOR.equals(actionOnMaxRestarts)) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_disable_due_to_action__FINEST",
                      new Object[] { Thread.currentThread().getName(), getName() });
            }
            throw new AbortCasProcessorException(CpmLocalizedMessage.getLocalizedMessage(
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_configured_to_disable__WARNING",
                    new Object[] { Thread.currentThread().getName(), getName() }));
          } else if (Constants.KILL_PROCESSING_PIPELINE.equals(actionOnMaxRestarts)) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_kill_pipeline__FINEST",
                      new Object[] { Thread.currentThread().getName(), getName() });
            }
            throw new KillPipelineException(
                    CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                            "UIMA_CPM_EXP_configured_to_kill_pipeline__WARNING",
                            new Object[] { Thread.currentThread().getName(), getName() }));
          }
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_skip_CAS__FINEST",
                    new Object[] { Thread.currentThread().getName(), getName() });
          }
          throw new SkipCasException("");
        }

        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_force_reconnect__FINEST",
                  new Object[] { Thread.currentThread().getName(), getName() });
        }

        throw (ServiceConnectionException) aThrowable.getCause();
      }
      if (aThrowable.getCause() != null) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_other_exception__FINEST",
                  new Object[] { Thread.currentThread().getName(), getName(),
                      aThrowable.getCause().getClass().getName() });
        }
      }

    } else {
      // Not ResourceException, so this most likely is an exception from the integrated
      // CasProcessor. This is
      // subject to error thresholds defined in the Cpe descriptor. CPE doesnt restart integrated
      // CasProcessor
      // since it is difficult to know what the nature of the failure is.
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_no_resource_exception__FINEST",
                new Object[] { Thread.currentThread().getName(), getName(), aThrowable });
      }
    }

    // If a failure rate exceeds errorRateThreshold,
    // the CASProcessor is deemed unreliable. The failure is measured
    // against a defined sample size. An example could be
    // Fail if there were 3 failures per 1000 (sample size) documents
    // processed.
    if (errorCounter > configuredErrorRate) {
      if (abortCPMOnError()) {
        if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_abort_exceeded_error_threshold__SEVERE",
                  new Object[] { Thread.currentThread().getName(), getName(),
                      String.valueOf(configuredErrorRate) });
        }
        throw new AbortCPMException("");
      }
      if (isAbortable()) {
        if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_disable_exceeded_error_threshold__SEVERE",
                  new Object[] { Thread.currentThread().getName(), getName(),
                      String.valueOf(configuredErrorRate) });
        }
        throw new AbortCasProcessorException(CpmLocalizedMessage.getLocalizedMessage(
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_configured_to_disable__WARNING",
                new Object[] { Thread.currentThread().getName(), getName() }));
      }
      // CasProcessor configured to ignore errors. So just reset error counter
      resetErrorCounter();

      if (continueOnError()) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_skipping_cas__FINEST",
                  new Object[] { Thread.currentThread().getName(), getName() });
        }
        throw new SkipCasException("");
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.collection.impl.base_cpm.container.ProcessingContainer#isEndOfBatch(org.apache.
   * uima.collection.base_cpm.CasProcessor, int)
   */
  /*
   * Called after each entity set is processed and its purpose is to mark end of batch associated
   * with this Container. At the end of batch a CasProcessor may perform a specific processing like
   * writing to a store or do indexing.
   * 
   * @see
   * org.apache.uima.collection.base_cpm.container.ProcessingContainer#isEndOfBatch(org.apache.uima.
   * collection.base_cpm.CasProcessor, int)
   */
  @Override
  public synchronized boolean isEndOfBatch(CasProcessor aCasProcessor, int aProcessedSize)
          throws ResourceProcessException, IOException {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_show_cp_batch_size__FINEST",
              new Object[] { Thread.currentThread().getName(), getName(),
                  String.valueOf(batchSize) });
    }
    boolean eob = false;

    batchCounter += aProcessedSize;
    sampleCounter += aProcessedSize;
    // Reset sample counters. Error rate per given sample does not exceed configured maximum
    // sampleCounter may exceed failureThresholdSample if aProcessSize is > 1.
    if (sampleCounter > failureThresholdSample || sampleCounter % failureThresholdSample == 0) {
      resetSampleCounter();
      resetErrorCounter();
    }
    // Keep track how many entities are still to be processed by this container
    if (remaining > 0) {
      remaining -= aProcessedSize;
    }
    // Increment the total number of entities processed so far through this container
    processed += aProcessedSize;
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_show_cp_doc_count__FINEST",
              new Object[] { Thread.currentThread().getName(), getName(),
                  String.valueOf(processed) });
    }
    // Check for end-of-batch
    if (batchCounter > batchSize || batchCounter % batchSize == 0) {
      if (aCasProcessor == null) {
        if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_cp_is_null__SEVERE",
                  new Object[] { Thread.currentThread().getName(), getName() });
        }
        throw new ResourceProcessException(new Exception(CpmLocalizedMessage.getLocalizedMessage(
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_invalid_reference__WARNING",
                new Object[] { Thread.currentThread().getName(), "NULL" })));
      }

      // endOfBatch must be done on all CAS Processor, not just CAS Consumers
      // ProcessTrace object not really needed any more except for backwards compatibility
      aCasProcessor.batchProcessComplete(new ProcessTrace_impl());

      resetBatchCounter();

      // This was used before to keep track of what was the last document id. This is no longer
      // needed i think.
      if (!processedEntityIds.isEmpty()) {
        String lastEntityId = (String) processedEntityIds.lastElement();
        processedEntityIds.clear();
        processedEntityIds.push(lastEntityId);
      }
      eob = true;
    }
    return eob;
  }

  /**
   * Returns true if the Cas bundles should be processed by the CasProcessor. This routine checks
   * for existance of dependent featues defined in the filter expression defined for the
   * CasProcessor in the cpe descriptor. Currently this is done on per bundle basis. Meaning that
   * all Cas'es must contain required features. If even one Cas does not have them, the entire
   * bundle is skipped.
   *
   * @param aCasList
   *          - bundle containing instances of CAS
   * @return true, if successful
   */
  @Override
  public boolean processCas(Object[] aCasList) {

    if (aCasList == null) {
      return false;
    }
    boolean doProcess = false;

    // If at least one cas has a dependant feature, process the entire set
    for (int i = 0; i < aCasList.length; i++) {
      if (processCas(aCasList[i])) {
        doProcess = true;
        break;
      }
    }

    return doProcess;
  }

  /**
   * Used during filtering, determines if a given Cas has a required feature. Required featured are
   * defined in the filter. Filtering is optional and if not present in the cpe descriptor this
   * routine always returns true.
   * 
   * @param aCas
   *          - Cas instance to check
   * 
   * @return - true if feature is in the Cas, false otherwise
   */
  private boolean hasFeature(CasData aCas) {
    if (System.getProperty("DEBUG_FILTER") != null) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_check_filter__FINEST",
                new Object[] { Thread.currentThread().getName(), getName() });
      }
      if (filterList == null) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_filtering_disabled__FINEST",
                  new Object[] { Thread.currentThread().getName(), getName() });
        }
      } else if (filterList.size() == 0) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_filter_empty__FINEST",
                  new Object[] { Thread.currentThread().getName(), getName() });
        }
      }
    }
    for (int i = 0; filterList != null && i < filterList.size(); i++) {
      if (System.getProperty("DEBUG_FILTER") != null) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_filter_enabled__FINEST",
                  new Object[] { Thread.currentThread().getName(), getName() });
        }
      }
      Filter.Expression filterExpression = (Filter.Expression) filterList.get(i);
      String featureValue = DATACasUtils.getFeatureValueByType(aCas,
              filterExpression.getLeftPart().get());
      // This evaluates if the Feature with a given name exist
      if (filterExpression.getRightPart() == null) {
        if (System.getProperty("DEBUG_FILTER") != null) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_filter_enabled_no_right_operand__FINEST",
                  new Object[] { Thread.currentThread().getName(), getName() });

        }
        // The first check is to see if the feature exists in the CAS. In this case,
        // the featureValue must NOT be null.
        // The second check is to see if the the feature does NOT exist in the CAS. In
        // this case, the feature MUST be null.
        boolean exists = DATACasUtils.hasFeatureStructure(aCas,
                filterExpression.getLeftPart().get());
        if (System.getProperty("DEBUG_FILTER") != null) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_filter_enabled_left_part__FINEST",
                  new Object[] { Thread.currentThread().getName(), getName(),
                      filterExpression.getLeftPart().get(), String.valueOf(exists) });
        }

        if ((filterExpression.getOperand() == null
                || filterExpression.getOperand().getOperand() == null || !exists) || // this means
                                                                                     // that the
                                                                                     // feature must
                                                                                     // exist in CAS
                ("!".equals(filterExpression.getOperand().getOperand()) && exists)) // this
        // means
        // that
        // the
        // feature
        // must
        // not
        // be
        // present
        // in
        // CAS
        {
          if (System.getProperty("DEBUG_FILTER") != null) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_filter_false__FINEST",
                    new Object[] { Thread.currentThread().getName(), getName() });
          }
          return false;
        }
      } else {
        // evaluate if the feature equals specified value
        if ("=".equals(filterExpression.getOperand().getOperand())) {
          if (!filterExpression.getRightPart().get().equals(featureValue)) {
            return false;
          }
        }
        // evaluate if the feature doesnt equal specified value
        else if ("!=".equals(filterExpression.getOperand().getOperand())) {
          if (filterExpression.getRightPart().get().equals(featureValue)) {
            return false;
          }
        }
      }
    }
    return true;

  }

  /**
   * Checks if a given Cas has required features.
   * 
   * @param aCas
   *          - Cas instance to check
   * 
   * @return - true if feature is in the Cas, false otherwise
   */
  private boolean processCas(Object aCas) {
    if (!(aCas instanceof CasData) || DATACasUtils.isCasEmpty((CasData) aCas)) {
      return false; // dont process empty CAS'es
    }

    return hasFeature((CasData) aCas);
  }

  /**
   * Returns CasProcessor configuration object. This object represents xml configuration defined in
   * the &lt;casProcessor&gt; section of the cpe descriptor.
   * 
   * @return {@link CasProcessorConfiguration} instance
   */
  @Override
  public CasProcessorConfiguration getCasProcessorConfiguration() {
    return casProcessorCPEConfiguration;
  }

  /**
   * Start.
   *
   * @deprecated
   */
  @Override
  @Deprecated
  public void start() {
    new Thread(this);
  }

  /**
   * Stop.
   *
   * @deprecated
   */
  @Override
  @Deprecated
  public void stop() {
  }

  /**
   * Returns available instance of the CasProcessor from the instance pool. It will wait
   * indefinitely until an instance is available.
   *
   * @return the cas processor
   */
  @Override
  public CasProcessor getCasProcessor() {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).log(Level.FINEST, Thread.currentThread().getName());
    }
    // Wait until the container is resumed. The container is going to be paused when
    // the CPM is trying to reconnect to the fenced un-managed service. Needed to make
    // sure that all threads using the same remote service pause until connection is
    // re-established. This loop will be terminated when the CPM calls resume().
    synchronized (lockForIsPaused) {
      while (isPaused) {
        try {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_container_paused__FINEST",
                    new Object[] { Thread.currentThread().getName(), getName() });
          }

          lockForIsPaused.wait();

        } catch (InterruptedException e) {
        }

        if (!isPaused) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_resuming_container__FINEST",
                    new Object[] { Thread.currentThread().getName(), getName(),
                        String.valueOf(CONTAINER_SLEEP_TIME) });
          }
        }
      }
    }

    try {
      do {
        long t = System.currentTimeMillis();
        synchronized (casProcessorPool) {
          CasProcessor processor = casProcessorPool.checkOut();
          fetchTime += (System.currentTimeMillis() - t);
          if (processor == null) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_wait_no_processor__FINEST",
                      new Object[] { Thread.currentThread().getName(), getName() });
            }
            casProcessorPool.wait(); // wait for something to be checked in
          } else {
            return processor;
          }
        }
      } while (true);
    } catch (Exception e) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_failed_waiting_for_processor__SEVERE",
              new Object[] { Thread.currentThread().getName(), getName(), e.getMessage() });
    }

    return null;

  }

  /**
   * Returns a given casProcessor instance back to the pool.
   *
   * @param aCasProcessor
   *          - an instance of CasProcessor to return back to the pool
   * @see org.apache.uima.collection.impl.base_cpm.container.ProcessingContainer#releaseCasProcessor(org.apache.uima.collection.base_cpm.CasProcessor)
   */
  @Override
  public synchronized void releaseCasProcessor(CasProcessor aCasProcessor) {
    try {
      casProcessorPool.checkIn(aCasProcessor);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Returns the current status of the CasProcessor.
   *
   * @return the status
   */
  @Override
  public int getStatus() {
    return casProcessorStatus;
  }

  /**
   * Changes the status of the CasProcessor as a group.
   *
   * @param aStatus
   *          - new status
   */
  @Override
  public void setStatus(int aStatus) {
    casProcessorStatus = aStatus;
  }

  /**
   * Checks if is local.
   *
   * @return true, if is local
   * @deprecated
   */
  @Override
  @Deprecated
  public boolean isLocal() {
    return isLocal;
  }

  /**
   * Checks if is remote.
   *
   * @return true, if is remote
   * @deprecated
   */
  @Override
  @Deprecated
  public boolean isRemote() {
    return isRemote;
  }

  /**
   * Checks if is integrated.
   *
   * @return true, if is integrated
   * @deprecated
   */
  @Override
  @Deprecated
  public boolean isIntegrated() {
    return isIntegrated;
  }

  /**
   * Returns true if the CasProcessor has been configured to continue despite error.
   *
   * @return - true if ignoring errors, false otherwise
   */
  private boolean continueOnError() {
    String action = casProcessorCPEConfiguration.getActionOnError(); // (String)
    // getConfigParameterValue(Constants.ACTION_ON_MAX_ERRORS);
    if ("continue".equals(action.toLowerCase())) {
      return true;
    }
    return false;
  }

  /**
   * Determines if instances of CasProcessor managed by this container are abortable. Abortable
   * CasProcessor's action attribute in the &lt;errorRateThreshold&gt; element has a value of
   * 'disable'.
   * 
   * @return true if CasProcessor can be disabled
   */
  @Override
  public boolean isAbortable() {
    // First vefrify action string
    String anAction = "";
    try {
      anAction = casProcessorCPEConfiguration.getActionOnError();
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_show_cp_action_on_error__FINEST",
                new Object[] { Thread.currentThread().getName(), getName(),
                    casProcessorCPEConfiguration.getActionOnError() });
      }
      if (anAction == null) {
        if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_action_on_error_not_defined__SEVERE",
                  new Object[] { Thread.currentThread().getName(), getName() });
        }
      }
    } catch (Exception e) {
      if (casProcessorCPEConfiguration == null) {
        if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_cp_configuration_not_defined__SEVERE",
                  new Object[] { Thread.currentThread().getName(), getName() });
        }
      } else if (casProcessorCPEConfiguration.getActionOnError() == null) {
        if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_action_on_error_not_defined__SEVERE",
                  new Object[] { Thread.currentThread().getName(), getName() });
        }
      } else {
        UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, e.getMessage(), e);
      }
    }

    if (anAction != null && Constants.DISABLE_CASPROCESSOR.equals(anAction.toLowerCase())) {
      return true;
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.Resource_ImplBase#initialize(org.apache.uima.resource.
   * ResourceSpecifier, java.util.Map)
   */
  @Override
  public boolean initialize(ResourceSpecifier aSpecifier, Map aAdditionalParams)
          throws ResourceInitializationException {
    try {
      if (aSpecifier instanceof CasConsumerDescription_impl) {
        CasConsumerDescription_impl mDescription = (CasConsumerDescription_impl) aSpecifier;

        configParams = mDescription.getMetaData().getConfigurationParameterSettings();
        if (aAdditionalParams != null) {
          Set aKeySet = aAdditionalParams.keySet();
          Iterator it = aKeySet.iterator();
          while (it.hasNext()) {
            String aKey = (String) it.next();
            setConfigParameterValue(aKey, aAdditionalParams.get(aKey));
          }
        }
      }
    } catch (Exception fnfe) {
      fnfe.printStackTrace();
    }
    return true;
  }

  /**
   * Destroy instances of CasProcessors managed by this container. Before destroying the instance,
   * this method notifies it with CollectionProcessComplete so that the component finalizes its
   * logic and does appropriate cleanup before shutdown.
   * 
   */
  @Override
  public void destroy() {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_show_cp_proxy_pool_size__FINEST",
              new Object[] { Thread.currentThread().getName(), getName(),
                  String.valueOf(casProcessorPool.getSize()) });
    }
    try {
      synchronized (casProcessorPool) {
        while (casProcessorPool.getSize() > 0) {
          // Retrieve next instance of CasProcessor from the pool. Wait max. 50 ms for it.
          CasProcessor cp = casProcessorPool.checkOut(50);
          if (cp == null) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_wait_no_processor__FINEST",
                      new Object[] { Thread.currentThread().getName(), getName() });
            }
            break;
          }

          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_destroy_processor__FINEST",
                    new Object[] { Thread.currentThread().getName(), getName() });
          }
          ProcessTrace pt = new ProcessTrace_impl();
          cp.collectionProcessComplete(pt);
          if (cp instanceof Resource) {
            ((Resource) (cp)).destroy();
          }
        }
      }
    } catch (Exception e) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINER)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINER, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_exception__FINER",
                new Object[] { Thread.currentThread().getName(), getName() });

        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINER, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_exception__FINER",
                new Object[] { Thread.currentThread().getName(), e.getMessage() });
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ConfigurableResource#getConfigParameterValue(java.lang.String)
   */
  @Override
  public Object getConfigParameterValue(String aParamName) {
    return configParams.getParameterValue(aParamName);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ConfigurableResource#getConfigParameterValue(java.lang.String,
   * java.lang.String)
   */
  @Override
  public Object getConfigParameterValue(String aGroupName, String aParamName) {
    return configParams.getParameterValue(aGroupName, aParamName);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ConfigurableResource#setConfigParameterValue(java.lang.String,
   * java.lang.Object)
   */
  @Override
  public void setConfigParameterValue(String aParamName, Object aValue) {
    configParams.setParameterValue(aParamName, aValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ConfigurableResource#setConfigParameterValue(java.lang.String,
   * java.lang.String, java.lang.Object)
   */
  @Override
  public void setConfigParameterValue(String aGroupName, String aParamName, Object aValue) {
    configParams.setParameterValue(aGroupName, aParamName, aValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ConfigurableResource#reconfigure()
   */
  @Override
  public void reconfigure() throws ResourceConfigurationException {
    // no op

  }

  /**
   * Returns the name of this container. It is the name of the Cas Processor.
   *
   * @return the name
   */
  @Override
  public String getName() {
    if (processorName == null) {
      if (metadata != null && Constants.DEPLOYMENT_INTEGRATED
              .equalsIgnoreCase(casProcessorCPEConfiguration.getDeploymentType())) {
        processorName = metadata.getName().trim();
      } else {
        processorName = casProcessorCPEConfiguration.getName().trim();
      }
    }
    return processorName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.Resource#getMetaData()
   */
  @Override
  public ResourceMetaData getMetaData() {
    return metadata;
  }

  /**
   * Increment a value of a given stat.
   *
   * @param aStatName
   *          the a stat name
   * @param aStat
   *          the a stat
   */
  @Override
  public void incrementStat(String aStatName, Integer aStat) {
    synchronized (statMap) {
      if (statMap.containsKey(aStatName)) {
        Object stat = statMap.remove(aStatName);
        if (stat instanceof Integer) {
          int newValue = aStat + (Integer) stat;
          aStat = newValue;
        }
      }
      statMap.put(aStatName, aStat);
    }

  }

  /**
   * Add an arbitrary object and bind it to a given name.
   *
   * @param aStatName
   *          the a stat name
   * @param aStat
   *          the a stat
   */
  @Override
  public void addStat(String aStatName, Object aStat) {
    synchronized (statMap) {
      if (statMap.containsKey(aStatName)) {
        statMap.remove(aStatName);
      }
      statMap.put(aStatName, aStat);
    }
  }

  /**
   * Return an abject identified with a given name.
   *
   * @param aStatName
   *          the a stat name
   * @return the stat
   */
  @Override
  public Object getStat(String aStatName) {
    synchronized (statMap) {
      if (statMap.containsKey(aStatName)) {
        return statMap.get(aStatName);
      }
    }
    return null;
  }

  /**
   * Returns all stats aggregate during the CPM run.
   *
   * @return a map of all stats aggregated during the CPM run
   */
  @Override
  public HashMap getAllStats() {
    synchronized (statMap) {
      return (HashMap) statMap.clone();
    }
  }

  /**
   * Pauses the container until resumed. The CPM will pause to the Container while it is trying to
   * re-connect to a shared remote service. While the Container is paused getCasProcessor() will not
   * be allowed to return a new CasProcessor. All other methods are accessible and will function
   * fine.
   */
  @Override
  public void pause() {

    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_pause_container__FINEST",
              new Object[] { Thread.currentThread().getName(), getName() });
    }
    synchronized (lockForIsPaused) {
      isPaused = true;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.impl.base_cpm.container.ProcessingContainer#resume()
   */
  @Override
  public void resume() {
    synchronized (lockForIsPaused) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_resuming_container__FINEST",
                new Object[] { Thread.currentThread().getName(), getName() });
      }
      isPaused = false;
      lockForIsPaused.notifyAll();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.impl.base_cpm.container.ProcessingContainer#isPaused()
   */
  @Override
  public boolean isPaused() {
    synchronized (lockForIsPaused) {
      return isPaused;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.impl.base_cpm.container.ProcessingContainer#getPool()
   */
  @Override
  public ServiceProxyPool getPool() {
    return casProcessorPool;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.collection.impl.base_cpm.container.ProcessingContainer#setSingleFencedService(
   * boolean)
   */
  @Override
  public void setSingleFencedService(boolean aSingleFencedInstance) {
    singleFencedInstance = aSingleFencedInstance;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.collection.impl.base_cpm.container.ProcessingContainer#isSingleFencedService()
   */
  @Override
  public boolean isSingleFencedService() {
    return singleFencedInstance;
  }

  /**
   * Gets the fetch time.
   *
   * @return the fetch time
   */
  public long getFetchTime() {
    return fetchTime;
  }
}
