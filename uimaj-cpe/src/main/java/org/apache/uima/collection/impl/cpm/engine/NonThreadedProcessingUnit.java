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

package org.apache.uima.collection.impl.cpm.engine;

import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas_data.CasData;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.base_cpm.AbortCPMException;
import org.apache.uima.collection.base_cpm.AbortCasProcessorException;
import org.apache.uima.collection.base_cpm.BaseStatusCallbackListener;
import org.apache.uima.collection.base_cpm.CasDataConsumer;
import org.apache.uima.collection.base_cpm.CasDataProcessor;
import org.apache.uima.collection.base_cpm.CasDataStatusCallbackListener;
import org.apache.uima.collection.base_cpm.CasObjectProcessor;
import org.apache.uima.collection.base_cpm.CasProcessor;
import org.apache.uima.collection.base_cpm.SkipCasException;
import org.apache.uima.collection.impl.CasConverter;
import org.apache.uima.collection.impl.EntityProcessStatusImpl;
import org.apache.uima.collection.impl.base_cpm.container.ProcessingContainer;
import org.apache.uima.collection.impl.base_cpm.container.ServiceConnectionException;
import org.apache.uima.collection.impl.base_cpm.container.deployer.CasProcessorDeployer;
import org.apache.uima.collection.impl.cpm.Constants;
import org.apache.uima.collection.impl.cpm.container.CasObjectNetworkCasProcessorImpl;
import org.apache.uima.collection.impl.cpm.container.NetworkCasProcessorImpl;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.CpmLocalizedMessage;
import org.apache.uima.collection.impl.cpm.vinci.DATACasUtils;
import org.apache.uima.collection.metadata.CpeConfiguration;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.Level;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.UimaTimer;
import org.apache.uima.util.impl.ProcessTrace_impl;


public class NonThreadedProcessingUnit {
  public int threadState = 0;

  protected CPECasPool casPool;

  protected boolean relaseCAS = false;

  protected CPMEngine cpm = null;

  protected BoundedWorkQueue workQueue = null;

  protected BoundedWorkQueue outputQueue = null;

  protected CasConverter mConverter;

  protected ProcessTrace processingUnitProcessTrace;

  protected LinkedList processContainers = new LinkedList();

  protected long numToProcess = 0;

  protected CAS[] casList;

  protected ArrayList statusCbL = new ArrayList();

  protected boolean notifyListeners = false;

  protected CAS conversionCas = null;

  protected Object[] artifact = null;

  protected CAS[] conversionCasArray;

  protected UimaTimer timer;

  protected String threadId = null;

  protected CpeConfiguration cpeConfiguration = null;

  private CAS[] casCache = null;

  
  /**
   * Initialize the PU
   * 
   * @param acpm -
   *          component managing life cycle of the CPE
   * @param aInputQueue -
   *          queue to read from
   * @param aOutputQueue -
   *          queue to write to
   */
  public NonThreadedProcessingUnit(CPMEngine acpm, BoundedWorkQueue aInputQueue,
          BoundedWorkQueue aOutputQueue) {
    cpm = acpm;
    try {
      cpeConfiguration = cpm.getCpeConfig();
    } catch (Exception e) {
    }
    workQueue = aInputQueue;
    conversionCasArray = new CAS[1];
    // Instantiate a class responsible for converting CasData to CasObject and vice versa
    mConverter = new CasConverter();

    outputQueue = aOutputQueue;
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_initialize_pipeline__FINEST",
              new Object[] { Thread.currentThread().getName(), workQueue.getName(),
                  String.valueOf(workQueue.getCurrentSize()) });
    }
  }

  public NonThreadedProcessingUnit(CPMEngine acpm) {
    cpm = acpm;
    try {
      cpeConfiguration = cpm.getCpeConfig();
    } catch (Exception e) {
    }
    conversionCasArray = new CAS[1];
    // Instantiate a class responsible for converting CasData to CasObject and vice versa
    mConverter = new CasConverter();
  }

  /**
   * Alternative method of providing a queue from which this PU will read bundle of Cas
   * 
   * @param aInputQueue -
   *          read queue
   */
  public void setInputQueue(BoundedWorkQueue aInputQueue) {
    workQueue = aInputQueue;
  }

  /**
   * Alternative method of providing a queue where this PU will deposit results of analysis
   * 
   * @param aOutputQueue -
   *          queue to write to
   */
  public void setOutputQueue(BoundedWorkQueue aOutputQueue) {
    outputQueue = aOutputQueue;
  }

  /**
   * Alternative method of providing the reference to the component managing the lifecycle of the
   * CPE
   * 
   * @param acpm -
   *          reference to the contrlling engine
   */
  public void setCPMEngine(CPMEngine acpm) {
    cpm = acpm;
  }

  /**
   * Null out fields of this object. Call this only when this object is no longer needed.
   */
  public void cleanup() {
    this.casPool = null;
    this.cpm = null;
    this.workQueue = null;
    this.outputQueue = null;
    this.mConverter = null;
    this.processingUnitProcessTrace = null;
    this.processContainers.clear();
    this.processContainers = null;
    this.casList = null;
    this.conversionCas = null;
    this.artifact = null;
    this.statusCbL = null;
    this.conversionCasArray = null;
  }

  /**
   * Set a flag indicating if notifications should be made via configured Listeners
   * 
   * @param aDoNotify -
   *          true if notification is required, false otherwise
   */
  public void setNotifyListeners(boolean aDoNotify) {
    notifyListeners = aDoNotify;
  }

  /**
   * Plugs in Listener object used for notifications.
   * 
   * @param aListener -
   *          {@link org.apache.uima.collection.base_cpm.BaseStatusCallbackListener} instance
   */
  public void addStatusCallbackListener(BaseStatusCallbackListener aListener) {
    statusCbL.add(aListener);
  }

  /**
   * Returns list of listeners used by this PU for callbacks.
   * 
   * @return - lif of {@link org.apache.uima.collection.base_cpm.BaseStatusCallbackListener}
   *         instances
   */
  public ArrayList getCallbackListeners() {
    return statusCbL;
  }

  /**
   * Removes given listener from the list of listeners
   * 
   * @param aListener -
   *          object to remove from the list
   */
  public void removeStatusCallbackListener(BaseStatusCallbackListener aListener) {
    statusCbL.remove(aListener);
  }

  /**
   * Plugs in ProcessTrace object used to collect statistics
   * 
   * @param aProcessingUnitProcessTrace -
   *          object to compile stats
   */
  public void setProcessingUnitProcessTrace(ProcessTrace aProcessingUnitProcessTrace) {
    processingUnitProcessTrace = aProcessingUnitProcessTrace;

  }

  /**
   * Plugs in custom timer used by the PU for getting time
   * 
   * @param aTimer -
   *          custom timer to use
   */
  public void setUimaTimer(UimaTimer aTimer) {
    timer = aTimer;
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_timer_class__FINEST",
              new Object[] { Thread.currentThread().getName(), timer.getClass().getName() });
    }
  }

  /**
   * Plugs in a list of Cas Processor containers. During processing Cas Processors in this list are
   * called sequentially. Each Cas Processor is contained in the container that is managing errors,
   * counts and totals, and restarts.
   * 
   * @param processorList
   *          CASProcessor to be added to the processing pipeline
   */
  public void setContainers(LinkedList processorList) {
    processContainers = processorList;
  }

  /**
   * 
   * Disable a CASProcessor in the processing pipeline. Locate it by provided index. The disabled
   * Cas Processor remains in the Processing Pipeline, however it is not used furing processing.
   * 
   * @param aCasProcessorIndex -
   *          location in the pipeline of the Cas Processor to delete
   */
  public void disableCasProcessor(int aCasProcessorIndex) {
    if (aCasProcessorIndex < 0 || aCasProcessorIndex > processContainers.size()) {
      return;
    }
    // Retrive container with a reference to the CasProcessor
    ProcessingContainer pc = ((ProcessingContainer) processContainers.get(aCasProcessorIndex));
    if (pc != null) {
      pc.setStatus(Constants.CAS_PROCESSOR_DISABLED);
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_disabled_cp__FINEST",
                new Object[] { Thread.currentThread().getName(), pc.getName() });
      }
    }
  }

  /**
   * 
   * Alternative method to disable Cas Processor. Uses a name to locate it.
   * 
   * @param aCasProcessorName -
   *          a name of the Cas Processor to disable
   */
  public void disableCasProcessor(String aCasProcessorName) {
    for (int i = 0; i < processContainers.size(); i++) {
      ProcessingContainer pc = ((ProcessingContainer) processContainers.get(i));
      if (pc.getName().equals(aCasProcessorName)) {
        pc.setStatus(Constants.CAS_PROCESSOR_DISABLED);
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_disabled_cp__FINEST",
                  new Object[] { Thread.currentThread().getName(), pc.getName() });
        }
      }
    }
  }

  /**
   * 
   * Enables Cas Processor with a given name. Enabled Cas Processor will immediately begin to
   * receive bundles of Cas.
   * 
   * @param aCasProcessorName -
   *          name of the Cas Processor to enable
   */
  public void enableCasProcessor(String aCasProcessorName) {
    for (int i = 0; i < processContainers.size(); i++) {
      ProcessingContainer pc = ((ProcessingContainer) processContainers.get(i));
      if (pc.getName().equals(aCasProcessorName)) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_enabled_cp__FINEST",
                  new Object[] { Thread.currentThread().getName(), pc.getName() });
        }
        pc.setStatus(Constants.CAS_PROCESSOR_RUNNING);
      }
    }
  }

  protected boolean analyze(Object[] aCasObjectList, ProcessTrace pTrTemp) throws Exception // throws
  // ResourceProcessException,
  // IOException,
  // CollectionException,
  // AbortCPMException
  {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_start_analysis__FINEST",
              new Object[] { Thread.currentThread().getName() });
    }
    // String lastDocId = "";
    CasProcessor processor = null;
    // This is used to hold an index of the current CasObject
    boolean doneAlready = false;
    // If there are no CASes in the list, return false since there is nothing else to do
    if (aCasObjectList == null || aCasObjectList[0] == null) {
      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_invalid_cas_reference__SEVERE",
                new Object[] { Thread.currentThread().getName() });
      }
      return false;
    }
    Object[] casObjects = null;
    // Determine if the Cas'es contained in the CasList are of type CAS. Samples the first CAS in
    // the list.
    // The list contains CASes of the same type ( either CasData or CAS ). Mixed model not
    // supported.
    boolean isCasObject = aCasObjectList[0] instanceof CAS;
    // String docid = "";
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_entering_pipeline__FINEST",
              new Object[] { Thread.currentThread().getName() });
    }

    ProcessingContainer container = null;
    String containerName = "";
    // *******************************************
    // ** P R O C E S S I N G P I P E L I N E **
    // *******************************************
    // Send Cas Object through the processing pipeline.
    for (int i = 0; processContainers != null && i < processContainers.size(); i++) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_retrieve_container__FINEST",
                new Object[] { Thread.currentThread().getName(), String.valueOf(i) });
      }
      // Retrieve the container. Container manages one or more instances of CAS Processor
      container = (ProcessingContainer) processContainers.get(i);
      // container can be disabled in multi-processing pipeline configurations. The container is
      // disabled
      // when one of the processing threads is in the process of restarting/reconnecting to a shared
      // fenced service. Shared, meaning that all processing pipelines use the same service for
      // invocations.
      // Container must be disabled to prevent concurrent restarts.
      if (containerDisabled(container) || filterOutTheCAS(container, isCasObject, aCasObjectList)) {
        continue;
      }
      containerName = container.getName();

      // Flag controlling do-while loop that facilitates retries. Retries are defined in the
      // CasProcessor configuration.
      boolean retry = false;
      do // Retry
      {

        try {
          if (System.getProperty("SHOW_MEMORY") != null) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(
                      Level.FINEST,
                      this.getClass().getName(),
                      "process",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_show_memory__FINEST",
                      new Object[] { Thread.currentThread().getName(),
                          String.valueOf(Runtime.getRuntime().totalMemory() / 1024),
                          String.valueOf(Runtime.getRuntime().freeMemory() / 1024) });
            }
          }

          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_checkout_cp_from_container__FINEST",
                    new Object[] { Thread.currentThread().getName(), containerName });
          }
          threadState = 2004;
          // Get the CasProcessor from the pool managed by the container
          processor = container.getCasProcessor();
          if (processor == null) {
            if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_checkout_null_cp_from_container__SEVERE",
                      new Object[] { Thread.currentThread().getName(), containerName });
            }
            throw new ResourceProcessException(CpmLocalizedMessage.getLocalizedMessage(
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_EXP_invalid_component_reference__WARNING", new Object[] {
                        Thread.currentThread().getName(), "CasProcessor", "NULL" }), null);
          }
          // Check to see if the CasProcessor is available for processing
          // The CasProcessor may have been disabled due to excessive errors and error policy
          // defined
          // in the CPE descriptor.
          if (!isProcessorReady(container.getStatus())) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_container_not_ready__FINEST",
                      new Object[] { Thread.currentThread().getName(), containerName });
            }
            if (container.getStatus() == Constants.CAS_PROCESSOR_KILLED) {
              container.releaseCasProcessor(processor);
              // Another thread has initiated CPM Abort. That Thread has already notified
              // the application of the Abort. Here we just return as the CPM has been
              // killed most likely due to excessive errors.
              return false;
            }

            // Skip any CasProcessor that is not ready to process
            break;
          }

          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "process",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_checkedout_cp_from_container__FINEST",
                    new Object[] { Thread.currentThread().getName(), containerName,
                        processor.getClass().getName() });
          }
          // ************************* P E R F O R M A N A L Y S I S *************************
          if (processor instanceof CasDataProcessor) {
            invokeCasDataCasProcessor(container, processor, aCasObjectList, pTrTemp, isCasObject,
                    retry);
            isCasObject = false;
          } else if (processor instanceof CasObjectProcessor) {
            invokeCasObjectCasProcessor(container, processor, aCasObjectList, pTrTemp, isCasObject);
            isCasObject = true;
          }
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "process",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_analysis_successfull__FINEST",
                    new Object[] { Thread.currentThread().getName(), containerName,
                        processor.getClass().getName() });
          }
          retry = false;
          // On successfull processing reset the restart counter. Restart counter determines how
          // many times to restart Cas Processor on the same CAS
          // Do this conditionally. If the CAS is to be dropped on Exception this restart counter
          // scope extends to the entire collection not just one CAS
          if (!cpm.dropCasOnException()) {
            container.resetRestartCount();
          }
        } catch (Exception e) {
          retry = handleErrors(e, container, processor, pTrTemp, aCasObjectList, isCasObject);
          if (cpm.dropCasOnException()) {
            retry = false; // override
            return false; // Dont pass the CAS to the CasConsumer. CAS has been dropped
          }
        } finally {
          if (retry == false) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(
                      Level.FINEST,
                      this.getClass().getName(),
                      "process",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_end_of_batch__FINEST",
                      new Object[] { Thread.currentThread().getName(), containerName,
                          processor.getClass().getName() });
            }
            if (isProcessorReady(container.getStatus())) {

              // Let the container take action if the end-of-batch marker has been reached.
              // End-of-batch marker is defined in the cpm configuration for every CasProcessor.
              // This marker is defined in the <checkpoint> section of the CasProcessor Definition
              // and corresponds to the attribute "batch". If end-of-batch marker is reached the
              // container
              // invokes batchProcessComplete() on the CasProcessor
              doEndOfBatch(container, processor, pTrTemp, aCasObjectList.length);
            }
          } else {
            container.incrementRetryCount(1);
          }
          // Release current Cas Processor before continuing with the next Cas Processor in the
          // pipeline
          if (processor != null) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(
                      Level.FINEST,
                      this.getClass().getName(),
                      "process",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_release_cp__FINEST",
                      new Object[] { Thread.currentThread().getName(), containerName,
                          processor.getClass().getName(), String.valueOf(casCache == null) });
            }
            doReleaseCasProcessor(container, processor);
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(
                      Level.FINEST,
                      this.getClass().getName(),
                      "process",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_ok_release_cp__FINEST",
                      new Object[] { Thread.currentThread().getName(), containerName,
                          processor.getClass().getName(), String.valueOf(casCache == null) });
            }
            processor = null;
          }

        }

      } while (retry);
    } // end of: For All CasProcessors

    postAnalysis(aCasObjectList, isCasObject, casObjects, pTrTemp, doneAlready);
    casObjects = null;
    return true;
  }

  /**
   * 
   * @param aFlag
   */
  public void setReleaseCASFlag(boolean aFlag) {
    relaseCAS = aFlag;
  }

  /**
   * @param aPool
   */
  public void setCasPool(CPECasPool aPool) {
    casPool = aPool;
  }

  /**
   * 
   * @param aCasObjectList
   * @param isCasObject
   * @param casObjects
   * @param aProcessTr
   * @param doneAlready
   * @throws Exception -
   */
  private void postAnalysis(Object[] aCasObjectList, boolean isCasObject, Object[] casObjects,
          ProcessTrace aProcessTr, boolean doneAlready) throws Exception {
    try {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_pipeline_completed__FINEST",
                new Object[] { Thread.currentThread().getName() });
      }
      // Notify Listeners that the entity has been processed.
      if (!doneAlready && notifyListeners) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_notify_listeners__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
        threadState = 2013;

        EntityProcessStatus aEntityProcStatus = new EntityProcessStatusImpl(aProcessTr);
        notifyListeners(aCasObjectList, isCasObject, aEntityProcStatus);
        threadState = 2014;
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_done_notify_listeners__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
      }
      // enqueue CASes. If the CPM is in shutdown mode due to hard kill dont allow enqueue of CASes
      if (outputQueue != null
              && (cpm.isRunning() == true || (cpm.isRunning() == false && cpm.isHardKilled() == false))) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_add_cas_to_queue__FINEST",
                  new Object[] { Thread.currentThread().getName(), outputQueue.getName(),
                      String.valueOf(outputQueue.getCurrentSize()) });
        }
        WorkUnit workUnit = new WorkUnit(aCasObjectList);
        if (casCache != null && casCache[0] != null) {
          workUnit.setCas(casCache);
        }
        threadState = 2015;

        outputQueue.enqueue(workUnit);

        casCache = null;

//        synchronized (outputQueue) { // redundant - the above enqueue call does this
//          outputQueue.notifyAll();
//        }

      }
      return;
    } catch (Exception e) {
      throw e;
    } finally {
      // if (casObjects != null && casObjects instanceof CasData[])
      if (outputQueue == null && casObjects != null && casObjects instanceof CasData[]) {
        if (System.getProperty("DEBUG_RELEASE") != null) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "process",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_done_with_cas__FINEST",
                    new Object[] { Thread.currentThread().getName(),
                        String.valueOf(Runtime.getRuntime().freeMemory() / 1024) });
          }
        }
        for (int i = 0; i < casObjects.length; i++) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "process",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_show_local_cache__FINEST",
                    new Object[] { Thread.currentThread().getName(),
                        String.valueOf(casCache == null) });
          }
          casObjects[i] = null;
          aCasObjectList[i] = null;
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "process",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_show_local_cache__FINEST",
                    new Object[] { Thread.currentThread().getName(),
                        String.valueOf(casCache == null) });
          }
        }
        if (System.getProperty("DEBUG_RELEASE") != null) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "process",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_show_total_memory__FINEST",
                    new Object[] { Thread.currentThread().getName(),
                        String.valueOf(Runtime.getRuntime().freeMemory() / 1024) });
          }
        }
      }
    }
  }

  private void doReleaseCasProcessor(ProcessingContainer aContainer, CasProcessor aCasProcessor) {
    if (aCasProcessor != null && aContainer != null) {
      aContainer.releaseCasProcessor(aCasProcessor);
    }
  }

  private void doEndOfBatch(ProcessingContainer aContainer, CasProcessor aProcessor,
          ProcessTrace aProcessTr, int howManyCases) {
    String containerName = aContainer.getName();
    try {
      aContainer.isEndOfBatch(aProcessor, howManyCases);

      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_end_of_batch_completed__FINEST",
                new Object[] { Thread.currentThread().getName(), containerName });
      }
    } catch (Exception ex) {
      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_end_of_batch_exception__SEVERE",
                new Object[] { Thread.currentThread().getName(), containerName, ex.getMessage() });
      }
    }

  }

  /**
   * Main routine that handles errors occuring in the processing loop.
   * 
   * @param e -
   *          exception in the main processing loop
   * @param aContainer -
   *          current container of the Cas Processor
   * @param aProcessor -
   *          current Cas Processor
   * @param aProcessTrace -
   *          an object containing stats for this procesing loop
   * @param aCasObjectList -
   *          list of CASes being analyzed
   * @param isCasObject -
   *          determines type of CAS in the aCasObjectList ( CasData or CasObject)
   * @return boolean
   * @throws Exception -
   */
  private boolean handleErrors(Throwable e, ProcessingContainer aContainer,
          CasProcessor aProcessor, ProcessTrace aProcessTrace, Object[] aCasObjectList,
          boolean isCasObject) throws Exception {
    boolean retry = true;

    String containerName = aContainer.getName();
    e.printStackTrace();
    if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
      UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, Thread.currentThread().getName(),
              e);
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.SEVERE,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_handle_exception__SEVERE",
              new Object[] { Thread.currentThread().getName(), containerName,
                  aProcessor.getClass().getName(), e.getMessage() });
    }

    EntityProcessStatusImpl enProcSt = new EntityProcessStatusImpl(aProcessTrace);
    enProcSt.addEventStatus("Process", "Failed", e);
    threadState = 2008;
    // Send exception notifications to all registered listeners
    notifyListeners(aCasObjectList, isCasObject, enProcSt);
    threadState = 2009;

    // Check the policy to determine what to do with the CAS on exception. Return the CAS back to
    // the pool
    // and stop the processing chain if required. The policy for what to do with the CAS on
    // exception is
    // defined in the CPE descriptor
    if (cpm.dropCasOnException()) {
      if (casCache != null) {
        clearCasCache();
      }
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.WARNING,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_drop_cas__WARNING",
              new Object[] { Thread.currentThread().getName(), containerName,
                  aProcessor.getClass().getName() });

      // Release CASes and notify listeners
      cpm.invalidateCASes((CAS[]) aCasObjectList);
      retry = false; // Dont retry. The CAS has been released
    }
    // If the container is in pause state dont increment errors since one thread has already
    // done this. While the container is in pause state the CPM is attempting to re-connect
    // to a failed service. Once that is done, the container is going to be resumed. While
    // in pause state ALL threads using the container will be suspended.
    if (aProcessor instanceof CasObjectNetworkCasProcessorImpl && aContainer.isPaused()) {
      threadState = 2010;

      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_container_paused_do_retry__FINEST",
                new Object[] { Thread.currentThread().getName(), containerName });

      }
      return true; // retry
    }
    if (e instanceof Exception && pauseContainer(aContainer, (Exception) e, threadId)) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_pausing_container__FINEST",
                new Object[] { Thread.currentThread().getName(), containerName });
      }
      threadState = 2011;

      // New Code 02/23/05
      // Pause the container while the CPM is re-connecting to un-managed service
      // that is shared by all processing threads
      aContainer.pause();
      threadId = Thread.currentThread().getName();
    }

    try {
      // Increments error counter and determines if any threshold have been reached. If
      // the max error rate is reached, the CasProcessor can be configured as follows:
      // - terminates CPM when threshold is reached ( method below throws AbortCPMException)
      // - disables CasProcessor ( method below throws AbortCasProcessorException )
      // - continue, CasProcessor continues to run dispite error
      aContainer.incrementCasProcessorErrors(e);
      // End of new code
    } // check if the exception should terminate the CPM
    catch (AbortCPMException ex) {
      retry = false;
      if (aContainer.isPaused()) {
        aContainer.resume();
      }
      aContainer.setStatus(Constants.CAS_PROCESSOR_KILLED);
      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_abort_cpm__SEVERE",
                new Object[] { Thread.currentThread().getName(), aProcessor.getClass().getName() });
      }
      throw new AbortCPMException(CpmLocalizedMessage.getLocalizedMessage(
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_configured_to_abort__WARNING",
              new Object[] { Thread.currentThread().getName(), containerName }));
    } // check if the CasProcessor is to be disabled due to excessive errors
    catch (AbortCasProcessorException ex) {
      retry = false;

      if (aContainer.isPaused()) {
        aContainer.resume();
      }
      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_disable_cp__SEVERE",
                new Object[] { Thread.currentThread().getName(), aProcessor.getClass().getName() });
      }
      aContainer.setStatus(Constants.CAS_PROCESSOR_DISABLED);

    } // check if need to redeploy the CasProcessor
    catch (ServiceConnectionException ex) {
      aProcessTrace.startEvent(containerName, "Process", "");
      String status = "failure";
      try {
        threadState = 2012;

        handleServiceException(aContainer, aProcessor, aProcessTrace, ex);
        // Increment number of restarts
        // aContainer.incrementRestartCount(1);
        status = "success";
      } catch (ResourceProcessException rpe) {
        throw rpe;
      } catch (Exception rpe) {
        throw new ResourceProcessException(rpe);
      } finally {
        aProcessTrace.endEvent(containerName, "Process", status);
      }
    } catch (SkipCasException ex) {
      try {
        handleSkipCasProcessor(aContainer, aCasObjectList, false);
        retry = false;
      } catch (Exception sEx) {
        throw new ResourceProcessException(sEx);
      }
    } catch (Exception ex) {
      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE,
                Thread.currentThread().getName(), e);
      }
      retry = false;
      ex.printStackTrace();
    }
    return retry;
  }

  /**
   * 
   * @param container
   * @param processor
   * @param aCasObjectList
   * @param pTrTemp
   * @param isCasObject
   * @throws Exception -
   */
  private void invokeCasObjectCasProcessor(ProcessingContainer container, CasProcessor processor,
          Object[] aCasObjectList, ProcessTrace pTrTemp, boolean isCasObject) throws Exception {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_show_memory__FINEST",
              new Object[] { Thread.currentThread().getName(),
                  String.valueOf(Runtime.getRuntime().totalMemory() / 1024),
                  String.valueOf(Runtime.getRuntime().freeMemory() / 1024) });

      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_invoke_cp_process__FINEST",
              new Object[] { Thread.currentThread().getName(), container.getName(),
                  processor.getClass().getName() });

    }
    casList = new CAS[aCasObjectList.length];
    for (int casIndex = 0; casIndex < aCasObjectList.length; casIndex++) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_initialize_cas__FINEST",
                new Object[] { Thread.currentThread().getName(), container.getName() });
      }
      if (aCasObjectList[casIndex] == null) {
        if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.SEVERE,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_casobjectlist_is_null__SEVERE",
                  new Object[] { Thread.currentThread().getName(), container.getName(),
                      String.valueOf(casIndex) });
        }
        break;
      }
      if (isCasObject == false) {
        convertCasDataToCasObject(casIndex, container.getName(), aCasObjectList);
      } else {
        casList[casIndex] = (CAS) aCasObjectList[casIndex];
      }
      if (processor instanceof AnalysisEngine) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_call_process__FINEST",
                  new Object[] { Thread.currentThread().getName(), container.getName(),
                      processor.getClass().getName() });
        }
        threadState = 2005;

        pTrTemp.aggregate(((AnalysisEngine) processor).process(casList[casIndex]));
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_call_process_completed__FINEST",
                  new Object[] { Thread.currentThread().getName(), container.getName(),
                      processor.getClass().getName() });
        }
      } else {
        pTrTemp.startEvent(container.getName(), "Process", "");
        threadState = 2006;

        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_call_process__FINEST",
                  new Object[] { Thread.currentThread().getName(), container.getName(),
                      processor.getClass().getName() });
        }
        ((CasObjectProcessor) processor).processCas(casList[casIndex]);
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_call_process_completed__FINEST",
                  new Object[] { Thread.currentThread().getName(), container.getName(),
                      processor.getClass().getName() });
        }
        pTrTemp.endEvent(container.getName(), "Process", "success");
      }
    }
    aCasObjectList = casList;

  }

  /**
   * 
   * @param casIndex
   * @param aContainerName
   * @param aCasObjectList
   * @throws Exception -
   */
  private void convertCasDataToCasObject(int casIndex, String aContainerName,
          Object[] aCasObjectList) throws Exception {
    // The following may be true if the CollectionReader is CasData based and this is the first
    // CasObject based annotator in the chain.
    if (casCache == null || casCache[casIndex] == null) {
      // casList[casIndex] = casPool.getCas();
      casList[casIndex] = null;

      while (casList[casIndex] == null) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_get_cas_from_pool__FINEST",
                  new Object[] { Thread.currentThread().getName(), aContainerName });
        }

        // Retrieve a Cas from Cas Pool. Wait max 10 millis for an instance
        casList[casIndex] = casPool.getCas(0);
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_got_cas_from_pool__FINEST",
                  new Object[] { Thread.currentThread().getName(), aContainerName });
        }
      }
      if (casList[casIndex] != null) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_call_cas_reset__FINEST",
                  new Object[] { Thread.currentThread().getName(), aContainerName });
        }
        casList[casIndex].reset();
      }
    } else {
      casList[casIndex] = casCache[casIndex];
      casList[casIndex].reset();
      // Cas is used up
      casCache[casIndex] = null;
    }
    // Convert CasData to CAS
    mConverter.casDataToCasContainer((CasData) aCasObjectList[casIndex], casList[casIndex], true);
  }

  /**
   * 
   * @param container
   * @param processor
   * @param aCasObjectList
   * @param pTrTemp
   * @param isCasObject
   * @param retry
   * @throws Exception -
   */
  private void invokeCasDataCasProcessor(ProcessingContainer container, CasProcessor processor,
          Object[] aCasObjectList, ProcessTrace pTrTemp, boolean isCasObject, boolean retry)
          throws Exception {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_invoke_cp_process__FINEST",
              new Object[] { Thread.currentThread().getName(), container.getName(),
                  processor.getClass().getName() });
    }
    pTrTemp.startEvent(container.getName(), "Process", "");
    // Check if the CasObject to CasData conversion is necessary
    if (isCasObject == true) {
      CasData[] casDataObjects = new CasData[aCasObjectList.length];
      for (int casIndex = 0; casIndex < aCasObjectList.length; casIndex++) {
        casDataObjects[casIndex] = mConverter.casContainerToCasData((CAS) aCasObjectList[casIndex]);
        // After the conversion reset the CAS so that it can be reused
        if ((CAS) aCasObjectList[casIndex] != null) {
          ((CAS) aCasObjectList[casIndex]).reset();
        }
      }
      // Cache the CAS list for possible reuse
      casCache = (CAS[]) aCasObjectList;
      // aCasObjectList is working list.
      aCasObjectList = casDataObjects;
    }
    long byteCount = 0;
    if (!retry) {
      for (int casIndex = 0; casIndex < aCasObjectList.length; casIndex++) {
        byteCount = getBytes(aCasObjectList[casIndex]);
        container.addBytesIn(byteCount);
      }
    }
    Object[] casObjects = aCasObjectList;
    long pStart = System.currentTimeMillis();
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_casobject_class__FINEST",
              new Object[] { Thread.currentThread().getName(), container.getName(),
                  casObjects.getClass().getName() });
    }
    if (!(casObjects instanceof CasData[])) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "process",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_expected_casdata_class__FINEST",
                new Object[] { Thread.currentThread().getName(), container.getName(),
                    casObjects.getClass().getName() });
      }
    }

    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_call_process__FINEST",
              new Object[] { Thread.currentThread().getName(), container.getName(),
                  processor.getClass().getName() });
    }
    casObjects = ((CasDataProcessor) processor).process((CasData[]) casObjects);
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_call_process_completed__FINEST",
              new Object[] { Thread.currentThread().getName(), container.getName(),
                  processor.getClass().getName() });
    }
    long pEnd = System.currentTimeMillis();
    container.incrementTotalTime((pEnd - pStart));
    if (casObjects != null) {
      if (processor instanceof CasDataConsumer) {
        container.addBytesOut(byteCount);
      } else {
        aCasObjectList = casObjects;
        if (!retry) {
          for (int casIndex = 0; casIndex < aCasObjectList.length; casIndex++) {
            byteCount = getBytes(aCasObjectList[casIndex]);
            container.addBytesOut(byteCount);
          }
        }
      }
    }
    pTrTemp.endEvent(container.getName(), "Process", "success");

  }

  private boolean containerDisabled(ProcessingContainer aContainer) {
    synchronized (aContainer) {
      // Check to see if the CasProcessor is available for processing
      if (!isProcessorReady(aContainer.getStatus())) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_container_not_ready__FINEST",
                  new Object[] { Thread.currentThread().getName(), aContainer.getName() });
        }
        // Skip any CasProcessor that is not ready to process. Cas Processors may be disabled during
        // processing
        return true;
      }
    }
    return false;
  }

  /**
   * Check if the CASProcessor status is available for processing
   */
  protected boolean isProcessorReady(int aStatus) {
    if (aStatus == Constants.CAS_PROCESSOR_READY || aStatus == Constants.CAS_PROCESSOR_RUNNING) {
      return true;
    }

    return false;
  }

  private boolean filterOutTheCAS(ProcessingContainer aContainer, boolean isCasObject,
          Object[] aCasObjectList) {
    // Check if any of the Cas'es in the set has a required feature structure.
    // This is currently only supported for the CasData instances and provides
    // filtering mechanism
    if (!isCasObject && !aContainer.processCas(aCasObjectList)) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_skip_CAS__FINEST",
                new Object[] { Thread.currentThread().getName(), aContainer.getName() });
      }
      aContainer.incrementFilteredCount(aCasObjectList.length);
      aContainer.logAbortedCases(aCasObjectList);
      return true; // skipped this CAS
    }
    return false;

  }

  /**
   * Notifies Listeners of the fact that the pipeline has finished processing the current set Cas'es
   * 
   * @param aCas -
   *          object containing an array of OR a single instance of Cas
   * @param isCasObject -
   *          true if instance of Cas is of type Cas, false otherwise
   * @param aEntityProcStatus -
   *          status object that may contain exceptions and trace
   */
  protected void notifyListeners(Object aCas, boolean isCasObject,
          EntityProcessStatus aEntityProcStatus) {
    if (aCas instanceof Object[]) {
      for (int i = 0; i < ((Object[]) aCas).length; i++) {
        doNotifyListeners(((Object[]) aCas)[i], isCasObject, aEntityProcStatus);
      }
    } else {
      doNotifyListeners(aCas, isCasObject, aEntityProcStatus);
    }
  }

  /**
   * Notifies all configured listeners. Makes sure that appropriate type of Cas is sent to the
   * listener. Convertions take place to ensure compatibility.
   * 
   * @param aCas -
   *          Cas to pass to listener
   * @param isCasObject -
   *          true is Cas is of type CAS
   * @param aEntityProcStatus -
   *          status object containing exceptions and trace info
   */
  protected void doNotifyListeners(Object aCas, boolean isCasObject,
          EntityProcessStatus aEntityProcStatus) {
    // Notify Listener that the entity has been processed
    Object casObjectCopy = aCas;
    // Notify ALL listeners
    for (int j = 0; j < statusCbL.size(); j++) {
      BaseStatusCallbackListener statCL = (BaseStatusCallbackListener) statusCbL.get(j);
      // Based on type of listener do appropriate conversions of Cas if necessary
      if (statCL instanceof CasDataStatusCallbackListener) {
        // The Cas is of type CAS, need to convert it to CasData
        if (isCasObject == true) {
          // Convert CAS to CasData object
          casObjectCopy = mConverter.casContainerToCasData((CAS) casObjectCopy);
        }
        // Notify the listener that the Cas has been processed
        ((CasDataStatusCallbackListener) statCL).entityProcessComplete((CasData) casObjectCopy,
                aEntityProcStatus);
      } else if (statCL instanceof StatusCallbackListener) {
        boolean casFromPool = false;
        // The cas is of type CasData, need to convert it to CAS
        if (isCasObject == false) {
          conversionCas = null;
          if (casCache != null && casCache[0] != null) {
            conversionCas = casCache[0];
          } else {
            while (conversionCas == null) {
              conversionCas = casPool.getCas(0);
            }
            casFromPool = true;
          }
          try {
            mConverter.casDataToCasContainer((CasData) casObjectCopy, conversionCas, true);
          } catch (CollectionException e) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.WARNING,
                    this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_exception_converting_CAS__WARNING",
                    new Object[] { Thread.currentThread().getName() });
          }
          casObjectCopy = conversionCas;
        }
        // Notify the listener that the Cas has been processed
//        ((StatusCallbackListener) statCL).entityProcessComplete((CAS) casObjectCopy,
//                aEntityProcStatus);
        CPMEngine.callEntityProcessCompleteWithCAS(
                (StatusCallbackListener) statCL, (CAS) casObjectCopy, aEntityProcStatus);
        if (conversionCas != null) {
          if (casFromPool) {
            conversionCasArray[0] = conversionCas;
            cpm.releaseCASes(conversionCasArray);
          }
          conversionCas = null;
          if (casCache != null && casCache[0] != null) {
            casCache[0].reset();
          }
        }

      }
    }

  }

  private void clearCasCache() {
    if (casCache != null) {
      for (int index = 0; index < casCache.length; index++) {
        if (casCache[index] != null) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_release_cas_from_cache__FINEST",
                    new Object[] { Thread.currentThread().getName() });
          }
          casPool.releaseCas(casCache[index]);
//          synchronized (casPool) { // redundant - the above releaseCas call does this
//            casPool.notifyAll();
//          }

          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_release_cas_from_cache_done__FINEST",
                    new Object[] { Thread.currentThread().getName() });
          }
        }
      }
      casCache = null;
    }
    // End New Code 05/30/05
  }

  /**
   * Determines if the thread should be paused. Pausing container effectively pauses ALL Cas
   * Processors that are managed by the container. The pause is needed when there are multiple
   * pipelines shareing a common service. If this service dies (Socket Down), only one thread should
   * initiate service restart. While the service is being restarted no invocations on the service
   * should be done. Containers will be resumed on successfull service restart.
   * 
   * @param aContainer -
   *          a container that manages the current Cas Processor.
   * @param aProcessor -
   *          a Cas Processor to be disabled
   * @param aThreadId -
   *          id of the current thread
   * 
   * @throws Exception -
   *           exception
   */
  private boolean pauseContainer(ProcessingContainer aContainer, Exception aException,
          String aThreadId) {
    if (aContainer.isRemote() && aContainer.isSingleFencedService()
            && aException.getCause() instanceof ServiceConnectionException && aThreadId == null) {
      return true;
    }

    return false;
  }

  /**
   * 
   * @param aContainer
   * @param aProcessor
   * @param aProcessTr
   * @param ex
   * @throws Exception -
   */
  private void handleServiceException(ProcessingContainer aContainer, CasProcessor aProcessor,
          ProcessTrace aProcessTr, Exception ex) throws Exception {
    if (aProcessor instanceof NetworkCasProcessorImpl) {
      ((NetworkCasProcessorImpl) aProcessor).collectionProcessComplete(aProcessTr);
    }
    // New Code 07/13/04
    // Release current Cas Processor before continuing with the next Cas Processor in the pipeline
    aContainer.releaseCasProcessor(aProcessor);

    // End New Code 07/13/04

    // New Code 2/25/2005 JC Container is paused ONLY iff CasProcessor is Remote (unmanaged) and
    // all proxies share the same CasProcessor service AND a previous thread initiated service
    // restart. Only one thread can initiate a restart, other threads sharing the same service
    // will block until all connections are re-established

    if (aContainer.isRemote() && aContainer.isSingleFencedService()) {
      if (Thread.currentThread().getName().equals(threadId)) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_service_connection_exception__FINEST",
                  new Object[] { Thread.currentThread().getName(), aContainer.getName(),
                      aProcessor.getClass().getName() });
        }
        aProcessTr.startEvent(aContainer.getName(), "Process", "");
        // Redeploy the CasProcessor
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_redeploy_cp__FINEST",
                  new Object[] { Thread.currentThread().getName(), aContainer.getName(),
                      aProcessor.getClass().getName() });
        }
        // Reconnect the CPM to CasProcessor running in fenced mode
        cpm.redeployAnalysisEngine(aContainer);

        // New Code 02/23/05
        // Resume the container
        aContainer.resume();
        threadId = null;
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_redeploy_cp_done__FINEST",
                  new Object[] { Thread.currentThread().getName(), aContainer.getName(),
                      aProcessor.getClass().getName() });
        }
      }

    } else {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "process",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_service_connection_exception__FINEST",
                new Object[] { Thread.currentThread().getName(), aContainer.getName(),
                    aProcessor.getClass().getName() });
      }
      aProcessTr.startEvent(aContainer.getName(), "Process", "");
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "process",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_redeploy_cp__FINEST",
                new Object[] { Thread.currentThread().getName(), aContainer.getName(),
                    aProcessor.getClass().getName() });
      }
      // Reconnect the CPM to CasProcessor running in fenced mode
      cpm.redeployAnalysisEngine(aContainer);
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "process",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_redeploy_cp_done__FINEST",
                new Object[] { Thread.currentThread().getName(), aContainer.getName(),
                    aProcessor.getClass().getName() });
      }
    }
  }

  /**
   * 
   * @param aContainer
   * @param aCasObjectList
   * @param isLastCP
   * @throws Exception -
   */
  private void handleSkipCasProcessor(ProcessingContainer aContainer, Object[] aCasObjectList,
          boolean isLastCP) throws Exception {

    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_skip_CAS__FINEST",
              new Object[] { Thread.currentThread().getName(), aContainer.getName() });
    }
    if (aContainer.isPaused()) {
      aContainer.resume();
    }

    aContainer.incrementAbortCount(aCasObjectList.length);

    // CAS may have already been dropped (released) due to dropCasOnException policy defined in the
    // CPE Descriptor
    if (!cpm.dropCasOnException()) {
      try {
        aContainer.logAbortedCases(aCasObjectList);
      } catch (Exception e) {
        throw e;
      } finally {
        // Do conditional release of CAS instances. The release occurs only if the CasProcessor is
        // the last one
        // in processing chain. This only releases instances of CAS checked out from the Cas Pool.
        // If not done, the
        // pool gets depleted and no more work will be done.
        try {
          releaseCases(casList, isLastCP, aContainer.getName());
        } catch (Exception ex2) {
          if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_exception_releasing_cas__SEVERE",
                    new Object[] { Thread.currentThread().getName(), aContainer.getName() });
            throw ex2;
          }
        }
      }
    }

  }

  /**
   * Returns the size of the CAS object. Currently only CASData is supported.
   * 
   * @param aCas -
   *          Cas to get the size for
   * 
   * @return the size of the CAS object. Currently only CASData is supported.
   */
  protected long getBytes(Object aCas) {
    try {
      if (aCas instanceof CasData) {
        return DATACasUtils.getByteCount((CasData) aCas);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return 0;
  }

  /**
   * Conditionally, releases CASes back to the CAS pool. The release only occurs if the Cas
   * Processor is the last in the processing chain.
   * 
   * @param aCasList -
   *          list of CASes to release
   * @param lastProcessor -
   *          determines if the release takes place
   * @param aContainer -
   *          current container
   */
  private void releaseCases(Object aCasList, boolean lastProcessor, String aName) // ProcessingContainer
  // aContainer)
  {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_releasing_cases__FINEST",
              new Object[] { Thread.currentThread().getName(), aName, String.valueOf(relaseCAS),
                  String.valueOf(lastProcessor) });
    }
    if (aCasList == null) {
      return;
    }

    if (relaseCAS && lastProcessor) {
      if (aCasList instanceof CAS[]) {
        // New Code 06/01/05 JC
        if (casCache != null) {
          // First release any CASes from the Cas Cache
          cpm.releaseCASes(casCache);
          casCache = null;
        }
        // End New Code 06/01/05 JC
        cpm.releaseCASes((CAS[]) aCasList);
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_done_releasing_cases__FINEST",
                  new Object[] { Thread.currentThread().getName(), aName });
        }
      } else {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_casobject_class__FINEST",
                  new Object[] { Thread.currentThread().getName(), aName,
                      aCasList.getClass().getName() });
        }
      }
    }

  }

  /**
   * Stops all Cas Processors that are part of this PU.
   * 
   * @param kill -
   *          true if CPE has been stopped before finishing processing during external stop
   * 
   */
  public void stopCasProcessors(boolean kill) {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_stop_containers__FINEST",
              new Object[] { Thread.currentThread().getName() });
    }
    // while (consumeQueue());
    //		
    // Object[] eofToken = new Object[1];
    // // only need to one member in the array
    // eofToken[0] = new EOFToken();
    //
    // workQueue.enqueue(eofToken);

    // Stop all running CASProcessors
    for (int i = 0; processContainers != null && i < processContainers.size(); i++) {
      ProcessingContainer container = (ProcessingContainer) processContainers.get(i);
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {

        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "process",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_show_container_time__FINEST",
                new Object[] { Thread.currentThread().getName(), container.getName(),
                    String.valueOf(container.getTotalTime()) });
      }
      synchronized (container) {
        // Change the status of this container to KILLED if the CPM has been stopped
        // before completing the collection and current status of CasProcessor is
        // either READY or RUNNING
        if (kill || (!cpm.isRunning() && isProcessorReady(container.getStatus()))) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_kill_cp__FINEST",
                    new Object[] { Thread.currentThread().getName(), container.getName() });
          }
          container.setStatus(Constants.CAS_PROCESSOR_KILLED);
        } else {
          // If the CasProcessor has not been disabled during processing change its
          // status to COMPLETED.
          if (container.getStatus() != Constants.CAS_PROCESSOR_DISABLED) {
            container.setStatus(Constants.CAS_PROCESSOR_COMPLETED);
          }
        }
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_container_status__FINEST",
                  new Object[] { Thread.currentThread().getName(), container.getName(),
                      String.valueOf(container.getStatus()) });
        }
        ProcessTrace pTrTemp = new ProcessTrace_impl(cpm.getPerformanceTuningSettings());
        pTrTemp.startEvent(container.getName(), "End of Batch", "");
        try {
          CasProcessorDeployer deployer = container.getDeployer();

          if (deployer != null) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(
                      Level.FINEST,
                      this.getClass().getName(),
                      "process",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_undeploy_cp_instances__FINEST",
                      new Object[] { Thread.currentThread().getName(), container.getName(),
                          deployer.getClass().getName() });
            }
            deployer.undeploy();
          }
          container.destroy();
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          pTrTemp.endEvent(container.getName(), "End of Batch", "");
          if (processingUnitProcessTrace != null) {
            this.processingUnitProcessTrace.aggregate(pTrTemp);
          }
        }
      }
    }
  }

}
