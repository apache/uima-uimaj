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

import java.io.IOException;
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
import org.apache.uima.collection.impl.base_cpm.container.KillPipelineException;
import org.apache.uima.collection.impl.base_cpm.container.ProcessingContainer;
import org.apache.uima.collection.impl.base_cpm.container.ServiceConnectionException;
import org.apache.uima.collection.impl.base_cpm.container.deployer.CasProcessorDeployer;
import org.apache.uima.collection.impl.cpm.Constants;
import org.apache.uima.collection.impl.cpm.container.CasObjectNetworkCasProcessorImpl;
import org.apache.uima.collection.impl.cpm.container.NetworkCasProcessorImpl;
import org.apache.uima.collection.impl.cpm.container.ProcessingContainer_Impl;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.ChunkMetadata;
import org.apache.uima.collection.impl.cpm.utils.CpmLocalizedMessage;
import org.apache.uima.collection.impl.cpm.vinci.DATACasUtils;
import org.apache.uima.collection.metadata.CpeConfiguration;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.Level;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.UimaTimer;
import org.apache.uima.util.impl.ProcessTrace_impl;

/**
 * This component executes the processing pipeline. Running in a seperate thread it continuously
 * reads bundles of Cas from the Work Queue filled by {@link ArtifactProducer} and sends it through
 * configured CasProcessors. The sequence in which CasProcessors are invoked is defined by the order
 * of Cas Processor listing in the cpe descriptor. The results of analysis produced be Cas
 * Processors is enqueued onto an output queue that is shared with Cas Consumers.
 * 
 * 
 */
public class ProcessingUnit extends Thread {
  public int threadState = 0;

  protected CPECasPool casPool;

  protected boolean releaseCAS = false;

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

  private boolean isCasConsumerPipeline = false;

  private boolean isRunning = false;

  public long timer01 = 0;

  public long timer02 = 0;

  public long timer03 = 0;

  public long timer04 = 0;

  public long timer05 = 0;

  public long timer06 = 0;

  public ProcessingUnit() {
    conversionCasArray = new CAS[1];
    // Instantiate a class responsible for converting CasData to CasObject and vice versa
    mConverter = new CasConverter();
  }

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
  public ProcessingUnit(CPMEngine acpm, BoundedWorkQueue aInputQueue, BoundedWorkQueue aOutputQueue) {
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
    maybeLogFinestWorkQueue("UIMA_CPM_initialize_pipeline__FINEST", workQueue);
  }

  public ProcessingUnit(CPMEngine acpm) {
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
   * Returns true if this component is in running state.
   * 
   * @return - true if running, false otherwise
   */
  public boolean isRunning() {
    return isRunning;
  }

  /**
   * Define a CasConsumer Pipeline identity for this instance
   */
  public void setCasConsumerPipelineIdentity() {
    isCasConsumerPipeline = true;
  }

  public boolean isCasConsumerPipeline() {
    return isCasConsumerPipeline;
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
    maybeLogFinest("UIMA_CPM_timer_class__FINEST", timer.getClass().getName());
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
      maybeLogFinest("UIMA_CPM_disabled_cp__FINEST", pc);
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
        maybeLogFinest("UIMA_CPM_disabled_cp__FINEST", pc);
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
        maybeLogFinest("UIMA_CPM_enabled_cp__FINEST", pc);
        pc.setStatus(Constants.CAS_PROCESSOR_RUNNING);
      }
    }
  }

  /**
   * Returns a {@link ProcessTrace} instance used by this component
   * 
   * @return - ProcessTrace instance
   */
  private ProcessTrace getProcessTrace() {
    ProcessTrace pT = null;

    if (timer != null) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        logFinest("UIMA_CPM_use_custom_timer__FINEST", timer.getClass().getName());
      }
      pT = new ProcessTrace_impl(timer, cpm.getPerformanceTuningSettings());
    } else {
      maybeLogFinest("UIMA_CPM_use_default_timer__FINEST");
      pT = new ProcessTrace_impl(cpm.getPerformanceTuningSettings());
    }
    return pT;
  }

  /**
   * Handles EOFToken. This object is received when the CPM terminates. This token is passed to each
   * running processing thread and cas consumer thread to allow orderly shutdown. The EOFToken may
   * be generated by ArtifactProducer if end of collection is reached, or the CPM itself can place
   * it in the Work Queue to force all processing threads to stop.
   * 
   * @throws Exception -
   */
  private void handleEOFToken() throws Exception {
    maybeLogFinest("UIMA_CPM_got_eof_token__FINEST");
    // Add EOF Token back to the work queue so that the next processing thread (if there is one) can
    // terminate. There will be more
    // processing if the CPE is configured (via CPE descriptor) to run in multipipeline mode.
    if (!isCasConsumerPipeline()) {
      // Check if there are additional processing threads to stop
      if (cpm.getThreadCount() > 1) {
        // Put EOF Token back to queue to ensure that all PUs get it
        workQueue.enqueue(artifact);
//        synchronized (workQueue) { redundant - the above enqueue call does this
//          workQueue.notifyAll();
//        }
      }
      if (outputQueue != null) {
        maybeLogFinest("UIMA_CPM_placed_eof_in_queue__FINEST", outputQueue.getName());
      }

      // Can't enquque EOFToken on output queue here, because if there are multiple processing
      // pipelines then the CAS Consumers may get the EOFToken before they get the CASes that
      // are being currently processed by the other pipelines. Instead, we inform the
      // CPMEngine that we are shutting down this pipeline, and let it decide when to put the
      // EOFToken on the output queue. -Adam
      // outputQueue.enqueue(artifact);
      cpm.processingUnitShutdown(this);
    }
  }

  /**
   * Release CAS back to the CAS Pool. This method is only used when chunk-aware queue is used. When
   * a document is chunked each chunk represents a portion of the document. These chunks are
   * ingested in sequential order by the Cas Consumer. The delivery of chunks in the correct
   * sequence ( chunk seg 1 before chunk sequence 2) is guaranteed. Since chunks are processed
   * asynchronously ( if multi pipeline configuration is used), they may arrive in the queue out of
   * sequence. If this happens the Cas Consumer will wait for an expected chunk sequence. If such
   * chunk does not arrive in configured interval the entire sequence ( all related chunks (CASes) )
   * are invalidated. Invalidated in the sense that they are marked as timed out. Each CAS will be
   * released back to the CAS Pool.
   * 
   * @param artifact -
   *          an array of CAS instances
   */
  private void releaseTimedOutCases(Object[] artifact) {
    for (int j = 0; j < artifact.length; j++) {
      if (artifact[j] != null) {
        // Release CASes that timed out back to the pool
        casPool.releaseCas((CAS) artifact[j]);
//        synchronized (casPool) { // redundant - the above releaseCas call does this
//          casPool.notifyAll();
//        }
        artifact[j] = null;
      }
    }
  }

  /**
   * 
   * 
   */
  private void isCpmPaused() {
    synchronized (cpm.lockForPause) {
      // Pause this thread if CPM has been paused
      while (cpm.isPaused()) {
        threadState = 2016;
        maybeLogFinest("UIMA_CPM_pausing_pp__FINEST");

        try {
          // Wait until resumed
          cpm.lockForPause.wait();
        } catch (InterruptedException e) {
        }
        maybeLogFinest("UIMA_CPM_resuming_pp__FINEST");
      }
    }
  }

  /**
   * Starts the Processing Pipeline thread. This thread waits for an artifact to arrive on
   * configured Work Queue. Once the CAS arrives, it is removed from the queue and sent through the
   * analysis pipeline.
   */
  public void run() {
    if (!cpm.isRunning()) {

      UIMAFramework.getLogger(this.getClass()).logrb(Level.WARNING, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_cpm_not_running__WARNING",
              new Object[] { Thread.currentThread().getName() });
      return;
    }
    maybeLogFinestWorkQueue("UIMA_CPM_start_pp__FINEST", workQueue);
    // Assign initial status to all Cas Processors in the processing pipeline
    for (int i = 0; i < processContainers.size(); i++) {
      ((ProcessingContainer) processContainers.get(i)).setStatus(Constants.CAS_PROCESSOR_RUNNING);
    }
    // Continue until CPE is stopped
    boolean run = true;
    int maxWaitTimeForEntity = 0;
    if (cpeConfiguration != null && cpeConfiguration.getMaxTimeToWait() > 0) {
      maxWaitTimeForEntity = cpeConfiguration.getMaxTimeToWait();
    }

    isRunning = true;

    while (run) {
      threadState = 2000; // Start the Loop
      // blocks if CPM is in pause state
      isCpmPaused();

      maybeLogFinestWorkQueue("UIMA_CPM_dequeue_artifact__FINEST", workQueue);
      artifact = null;
      Object entity = null;
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        logFinest("UIMA_CPM_maxwait_for_artifact__FINEST", String.valueOf(maxWaitTimeForEntity));
      }
      threadState = 2001; // Entering dequeue()

      // D E Q U E U E *************************************
      if (maxWaitTimeForEntity > 0) {
        entity = workQueue.dequeue(maxWaitTimeForEntity);
      } else {
        entity = workQueue.dequeue(0);

      }

      if (entity == null) {
        maybeLogFinest("UIMA_CPM_queue_empty__FINEST", workQueue.getName());
        continue;
      }
      try {
        if (entity instanceof WorkUnit) {
          artifact = (Object[]) ((WorkUnit) entity).get();
          if (((WorkUnit) entity).isTimedOut() && artifact instanceof CAS[]) {

            for (int i = 0; i < artifact.length && artifact[i] != null; i++) {
              ChunkMetadata meta = CPMUtils.getChunkMetadata((CAS) artifact[i]);
              if (meta != null) {
                EntityProcessStatusImpl enProcSt = new EntityProcessStatusImpl(
                        processingUnitProcessTrace);
                enProcSt.addEventStatus("Process", "Failed", new SkipCasException(
                        "Dropping CAS due chunk Timeout. Doc Id::" + meta.getDocId() + " Sequence:"
                                + meta.getSequence()));
                doNotifyListeners(artifact[i], true, enProcSt);
              } else {
                EntityProcessStatusImpl enProcSt = new EntityProcessStatusImpl(
                        processingUnitProcessTrace);
                enProcSt.addEventStatus("Process", "Failed", new SkipCasException(
                        "Dropping CAS due chunk Timeout. Chunk Metadata is not available."));
                doNotifyListeners(artifact[i], true, enProcSt);
              }
              releaseTimedOutCases(artifact);
            }

            continue;
          }
          if (((WorkUnit) entity).getCas() != null) {
            casCache = ((WorkUnit) entity).getCas();
          }
        } else {
          artifact = (Object[]) entity;
        }
        ProcessTrace pT = getProcessTrace();

        Object[] cases = artifact;

        // Check if the artifact is actually an EOFToken. If so, this is
        // marker that indicates end of processing. The assumption is that
        // the will not be anything enqueued after the EOFToken.
        if (cases.length > 0 && cases[0] instanceof EOFToken) {
          threadState = 2002; // End
          run = false;
          handleEOFToken();
          break; // Terminate Loop
        }
        
        maybeLogFinest("UIMA_CPM_call_processNext__FINEST");
        /* *********** EXECUTE PIPELINE ************ */
        processNext(artifact, pT);

        if (System.getProperty("DEBUG_EVENTS") != null) {
          maybeLogFinest("UIMA_CPM_dump_events__FINEST");
          CPMUtils.dumpEvents(pT);
        }
        // Update processing trace counts and timers
        synchronized (processingUnitProcessTrace) {
          processingUnitProcessTrace.aggregate(pT);
        }
      } catch (ResourceProcessException e) {
        maybeLogSevereException(e);
        if (e.getCause() instanceof KillPipelineException) {
          cpm.pipelineKilled(Thread.currentThread().getName());
          releaseCAS = true;
          break; // terminate the thread
        }
        threadState = 2003; // Killing

        this.cpm.killIt();
      } catch (Exception e) {
        maybeLogSevereException(e);
        threadState = 2003; // Killing

        this.cpm.killIt();

      } finally {
        if (releaseCAS) {
          clearCasCache();
        }

      }
    }
    maybeLogFinestWorkQueue("UIMA_CPM_exit_pp__FINEST", workQueue);
    // Always clear the cas cache on exit.
    clearCasCache();
    maybeLogFinest("UIMA_CPM_pp_terminated__FINEST");
    isRunning = false;

  }

  /**
   * Releases all CAS instances from the Cache back to the Cas Pool. Cas Cache is used as
   * optimization to store CAS in case it is needed for conversion. Specifically, in configurations
   * that use XCAS and CAS based AEs.
   * 
   */
  private void clearCasCache() {
    if (casCache != null) {
      for (int index = 0; index < casCache.length; index++) {
        if (casCache[index] != null) {
          // casCache[index].reset();
          maybeLogFinest("UIMA_CPM_release_cas_from_cache__FINEST");
          casPool.releaseCas(casCache[index]);
//          synchronized (casPool) { // redundant - the above releaseCas call does this
//            casPool.notifyAll();
//          }

          maybeLogFinest("UIMA_CPM_release_cas_from_cache_done__FINEST");
        }
      }
      casCache = null;
    }

  }

  /**
   * Consumes the input queue to make sure all bundles still there get processede before CPE
   * terminates.
   */
  public boolean consumeQueue() {
    Object artifact = null;

    maybeLogFinestWorkQueue("UIMA_CPM_dequeue_artifact__FINEST", workQueue);
    // Dequeue first bundle
    artifact = workQueue.dequeue();
    maybeLogFinest("UIMA_CPM_dequeued_artifact__FINEST", workQueue.getName());
    if (artifact != null) {
      try {
        ProcessTrace pT = new ProcessTrace_impl(cpm.getPerformanceTuningSettings());
        if (artifact instanceof Object[]) {
          Object[] oList = (Object[]) artifact;
          // Only consume CASs
          if (oList[0] != null && !(oList[0] instanceof EOFToken)) {
            maybeLogFinest("UIMA_CPM_call_processNext__FINEST");
            processNext((Object[]) artifact, pT);
            maybeLogFinest("UIMA_CPM_call_processNext_done__FINEST");
            synchronized (processingUnitProcessTrace) {
              processingUnitProcessTrace.aggregate(pT);
            }
            return true;
          }
        }
      } catch (Exception e) {
        maybeLogSevereException(e);
      }
    }
    return false;
  }

  /**
   * Executes the processing pipeline. Given bundle of Cas instances is processed by each Cas
   * Processor in the pipeline. Conversions between different types of Cas Processors is done on the
   * fly. Two types of Cas Processors are currently supported:
   * 
   * <ul>
   * <li> CasDataProcessor</li>
   * <li> CasObjectProcessor</li>
   * </ul>
   * 
   * The first operates on instances of CasData the latter operates on instances of CAS. The results
   * produced by Cas Processors are added to the output queue.
   * 
   * @param aCasObjectList - bundle of Cas to analyze
   * @param pTrTemp - object used to aggregate stats
   */
  protected boolean processNext(Object[] aCasObjectList, ProcessTrace pTrTemp)
          throws ResourceProcessException, IOException, CollectionException, AbortCPMException,
          KillPipelineException {
    maybeLogFinest("UIMA_CPM_start_analysis__FINEST");
    // String lastDocId = "";
    CasProcessor processor = null;
    // This is used to hold an index of the current CasObject
    // int currentIndex = -1;
    boolean doneAlready = false;
    // If there are no CASes in the list, return false since there is nothing else to do
    if (aCasObjectList == null || aCasObjectList[0] == null) {
      maybeLogFinest("UIMA_CPM_invalid_cas_reference__SEVERE");
      return false;
    }
    Object[] casObjects = null;
    // Determine if the Cas'es contained in the CasList are of type CAS. Samples the first CAS in
    // the list.
    // The list contains CASes of the same type ( either CasData or CAS ). Mixed model not
    // supported.
    boolean isCasObject = aCasObjectList[0] instanceof CAS;
    // String docid = "";
    maybeLogFinest("UIMA_CPM_entering_pipeline__FINEST");

    ProcessingContainer container = null;
    // *******************************************
    // ** P R O C E S S I N G P I P E L I N E **
    // *******************************************
    // Send Cas Object through the processing pipeline.
    for (int i = 0; processContainers != null && i < processContainers.size(); i++) {
      
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        logFinest("UIMA_CPM_retrieve_container__FINEST", String.valueOf(i));
      }
      container = (ProcessingContainer) processContainers.get(i);
      synchronized (container) {
        // Check to see if the CasProcessor is available for processing
        if (!isProcessorReady(container.getStatus())) {
          maybeLogFinest("UIMA_CPM_container_not_ready__FINEST", container);
          boolean breakTheLoop = (i == (processContainers.size() - 1));
          if (breakTheLoop && isCasObject) {
            releaseCases(aCasObjectList, true, container.getName());
            break;
          }

          // Skip any CasProcessor that is not ready to process
          continue;
        }
      }
      // Check if any of the Cas'es in the set has a required feature structure.
      if (!isCasObject && !container.processCas(aCasObjectList)) {
        maybeLogFinest("UIMA_CPM_skip_CAS__FINEST", container);
        container.incrementFilteredCount(aCasObjectList.length);
        container.logAbortedCases(aCasObjectList);
        continue;
      }

      long byteCount;
      // Flag controlling do-while loop that facilitates retries. Retries are defined in the
      // CasProcessor configuration.
      boolean retry = false;

      // Retry Loop.
      do {
        if (System.getProperty("SHOW_MEMORY") != null) {
          maybeLogMemoryFinest();
        }

        maybeLogFinest("UIMA_CPM_checkout_cp_from_container__FINEST", container);
        threadState = 2004; // Entering dequeue()

        processor = container.getCasProcessor();
        if (processor == null) {
          maybeLogSevere("UIMA_CPM_checkout_null_cp_from_container__SEVERE", container.getName());
          throw new ResourceProcessException(CpmLocalizedMessage.getLocalizedMessage(
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_EXP_invalid_component_reference__WARNING", new Object[] {
                      Thread.currentThread().getName(), "CasProcessor", "NULL" }), null);
        }
        // Check to see if the CasProcessor is available for processing
        // Container may have been disabled by another thread, so first check
        if (!isProcessorReady(container.getStatus())) {
          maybeLogFinest("UIMA_CPM_container_not_ready__FINEST", container);
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

        maybeLogFinest("UIMA_CPM_checkedout_cp_from_container__FINEST", container, processor);
        try {
          if (processor instanceof CasDataProcessor) {
            maybeLogFinest("UIMA_CPM_cas_data_processor__FINEST", container, processor);
            pTrTemp.startEvent(container.getName(), "Process", "");
            if (isCasObject == true) {
              CasData[] casDataObjects = new CasData[aCasObjectList.length];
              for (int casIndex = 0; casIndex < aCasObjectList.length; casIndex++) {
                casDataObjects[casIndex] = mConverter
                        .casContainerToCasData((CAS) aCasObjectList[casIndex]);
                if ((CAS) aCasObjectList[casIndex] != null) {
                  ((CAS) aCasObjectList[casIndex]).reset();
                }
              }

              casCache = (CAS[]) aCasObjectList;

              aCasObjectList = casDataObjects;
            }
            isCasObject = false;
            byteCount = 0;
            if (!retry) {

              for (int casIndex = 0; casIndex < aCasObjectList.length; casIndex++) {
                byteCount = getBytes(aCasObjectList[casIndex]);
                container.addBytesIn(byteCount);
              }
            }
            casObjects = aCasObjectList;
            long pStart = System.currentTimeMillis();
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              logFinest("UIMA_CPM_call_process__FINEST", container, processor);
              logFinest("UIMA_CPM_casObjects_class__FINEST", casObjects.getClass().getName());
            }
            if (!(casObjects instanceof CasData[])) {
              maybeLogFinest("UIMA_CPM_expected_casdata__FINEST", casObjects.getClass().getName());
            }

            maybeLogFinest("UIMA_CPM_call_process__FINEST", container, processor);
            casObjects = ((CasDataProcessor) processor).process((CasData[]) casObjects);
            maybeLogFinest("UIMA_CPM_call_process_completed__FINEST", container, processor);
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
          } else if (processor instanceof CasObjectProcessor) {
            maybeLogFinest("UIMA_CPM_casobject_processor__FINEST", container, processor);
            maybeLogMemoryFinest();
            casList = new CAS[aCasObjectList.length];
            for (int casIndex = 0; casIndex < aCasObjectList.length; casIndex++) {
              maybeLogFinest("UIMA_CPM_initialize_cas__FINEST", container);
              if (aCasObjectList[casIndex] == null) {
                if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
                  logSevere("UIMA_CPM_casobjectlist_is_null__SEVERE", 
                      container.getName(), String.valueOf(casIndex));
                }
                break;
              }
              if (isCasObject == false) {
                // The following may be true if the CollectionReader is CasData based and this is
                // the first CasObject based annotator in the chain.
                if (casCache == null || casCache[casIndex] == null) {
                  casList[casIndex] = null;

                  while (casList[casIndex] == null) {
                    maybeLogFinest("UIMA_CPM_get_cas_from_pool__FINEST", container);
                     // Retrieve a Cas from Cas Pool. Wait max 10 millis for an instance
                    casList[casIndex] = casPool.getCas(0);
                    maybeLogFinest("UIMA_CPM_got_cas_from_pool__FINEST", container);
                  }
                  if (casList[casIndex] != null) {
                    maybeLogFinest("UIMA_CPM_call_cas_reset__FINEST", container);
                    casList[casIndex].reset();
                  }
                } else {
                  casList[casIndex] = casCache[casIndex];
                  casList[casIndex].reset();
                  maybeLogFinest("UIMA_CPM_nullify_cas__FINEST", container);
                  // Cas is used up
                  casCache[casIndex] = null;
                }

                // Convert CasData to CAS
                mConverter.casDataToCasContainer((CasData) aCasObjectList[casIndex],
                        casList[casIndex], true);
              } else {
                casList[casIndex] = (CAS) aCasObjectList[casIndex];
              }
              //	Set the type from CasData to CasObject. When an error occurs in the proces()
              //	we need to know what type of object we deal with. 
              isCasObject = true;
              aCasObjectList = casList;

              if (processor instanceof AnalysisEngine) {
                maybeLogFinest("UIMA_CPM_call_process__FINEST", container, processor);
                threadState = 2005;

                pTrTemp.aggregate(((AnalysisEngine) processor).process(casList[casIndex]));
                maybeLogFinest("UIMA_CPM_call_process_completed__FINEST", container, processor);
              } else {
                pTrTemp.startEvent(container.getName(), "Process", "");
                threadState = 2006;
                maybeLogFinest("UIMA_CPM_call_process__FINEST", container, processor);
                ((CasObjectProcessor) processor).processCas(casList[casIndex]);
                maybeLogFinest("UIMA_CPM_call_process_completed__FINEST", container, processor);
                pTrTemp.endEvent(container.getName(), "Process", "success");
              }
            }
          }

          // Release the CAS and notify listeners if the end of the
          // pipeline is reached.
          if ((releaseCAS) && (i == (processContainers.size() - 1))) {
            // This flag is used to prevent multiple notifications
            doneAlready = true;
            EntityProcessStatus aEntityProcStatus = new EntityProcessStatusImpl(pTrTemp);
            maybeLogFinest("UIMA_CPM_notify_listeners__FINEST");
            threadState = 2007;

            notifyListeners(aCasObjectList, isCasObject, aEntityProcStatus);
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              logFinest("UIMA_CPM_done_notify_listeners__FINEST");
              logFinest("UIMA_CPM_releasing_cases__FINEST",
                container.getName(), String.valueOf(releaseCAS), "true");
            }
            if (casCache != null) {
              clearCasCache();
            }
            // Release CAS's.
            if (aCasObjectList instanceof CAS[]) {
              cpm.releaseCASes((CAS[]) aCasObjectList);
            }

            maybeLogFinest("UIMA_CPM_done_releasing_cases__FINEST", container);
          }

          maybeLogFinest("UIMA_CPM_pipeline_completed__FINEST");
          retry = false;
          // On successfull processing reset the restart counter. Restart counter determines how
          // many times to restart Cas Processor on the same CAS
          // Do this conditionally. If the CAS is to be dropped on Exception this restart counter
          // scope extends to the entire collection not just one CAS
          if (!cpm.dropCasOnException()) {
            container.resetRestartCount();
          }
        } catch (Exception e) {
          e.printStackTrace();
          if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {

            logSevere("UIMA_CPM_pipeline_exception__SEVERE", container.getName(), e.getMessage());

            maybeLogSevereException(e);

            logFinest("UIMA_CPM_pipeline_exception__FINEST", 
                container.getName(), String.valueOf(container.isPaused()));
          }

          EntityProcessStatusImpl enProcSt = new EntityProcessStatusImpl(pTrTemp);
          enProcSt.addEventStatus("Process", "Failed", e);
          threadState = 2008;

          notifyListeners(aCasObjectList, isCasObject, enProcSt);
          doneAlready = true;
          threadState = 2009;

          // Check the policy to determine what to do with the CAS on exception. Return the CAS back
          // to the pool
          // and stop the processing chain if required. The policy for what to do with the CAS on
          // exception is
          // defined in the CPE descriptor
          if (cpm.dropCasOnException()) {
            if (casCache != null) {
              clearCasCache();
            }
            if (aCasObjectList instanceof CAS[]) {
              cpm.invalidateCASes((CAS[]) aCasObjectList);
            }
            retry = false; // Dont retry. The CAS has been released
            maybeLogWarning("UIMA_CPM_drop_cas__WARNING", 
                container.getName(), processor.getClass().getName());
          } else {
            retry = true; // default on Exception
          }
          // If the container is in pause state dont increment errors since one thread has already
          // done this. While the container is in pause state the CPM is attempting to re-connect
          // to a failed service. Once that is done, the container is going to be resumed. While
          // in pause state ALL threads using the container will be suspended.
          if (processor instanceof CasObjectNetworkCasProcessorImpl && container.isPaused()) {
            threadState = 2010;

            maybeLogFinest("UIMA_CPM_container_paused_do_retry__FINEST", container);

            // Do conditional release of CAS instances. The release occurs only if the CasProcessor
            // is the last one
            // in processing chain. This only releases instances of CAS checked out from the Cas
            // Pool. If not done, the
            // pool gets depleted and no more work will be done.
            releaseCases(casList, (i == (processContainers.size() - 1)), container.getName());

            maybeLogFinest("UIMA_CPM_container_paused__FINEST", container);
            // Release current Cas Processor before continuing with the next Cas Processor in the
            // pipeline
            if (processor != null) {
              container.releaseCasProcessor(processor);
              maybeLogFinest("UIMA_CPM_ok_released_cp__FINEST", container);
              processor = null;
            }
            try {
              pTrTemp.endEvent(container.getName(), "Process", "failed");
            } catch (Exception exc) {
              // Just ignore out-of-phase endEvent exceptions for now.
            }
            continue;
          }
          if (pauseContainer(container, e, threadId)) // container.isRemote() &&
          // container.isSingleFencedService() &&
          // threadId == null )
          {
            threadState = 2011;

            // Pause the container while the CPM is re-connecting to un-managed service
            // that is shared by all processing threads
            container.pause();
            maybeLogFinest("UIMA_CPM_pausing_container__FINEST", container);
            threadId = Thread.currentThread().getName();
          }

          if (processor instanceof CasDataProcessor
                  || (processor instanceof CasObjectProcessor && !(processor instanceof AnalysisEngine))) {
            try {
              pTrTemp.endEvent(container.getName(), "Process", "failed");
            } catch (Exception exc) {
              // Just ignore out-of-phase endEvent exceptions for now.
            }
          }
          try {
            // Increments error counter and determines if any threshold have been reached. If
            // the max error rate is reached, the CasProcessor can be configured as follows:
            // - terminates CPM when threshold is reached ( method below throws AbortCPMException)
            // - disables CasProcessor ( method below throws AbortCasProcessorException )
            // - continue, CasProcessor continues to run dispite error
            container.incrementCasProcessorErrors(e);

            container.releaseCasProcessor(processor);
            processor = null;
            if (cpm.dropCasOnException()) {
              // Cas has already been returned to the CAS pool. The policy requires to stop the
              // processing chain for this CAS and
              // to get another CAS for processing.
              return true;
            } else {
              container.incrementRetryCount(1);
              continue;
            }
          } // check if the exception should terminate the CPM
          catch (KillPipelineException ex) {
            try {
              handleKillPipeline(container);
              processor = null;
            } catch (Exception innerE) {
              maybeLogWarning("UIMA_CPM_exception_on_pipeline_kill__WARNING",
                  container.getName(), innerE.getMessage());
            }
            // finally
            // {
            // // Throw Original Exception - Killing Pipeline
            // throw ex;
            // }
            throw ex;
          } catch (AbortCPMException ex) {
            try {
              handleAbortCPM(container, processor);
            } catch (Exception innerE) {
              maybeLogWarning("UIMA_CPM_exception_on_cpm_kill__WARNING", 
                  container.getName(), innerE.getMessage());
            }
            // finally
            // {
            // throw new AbortCPMException("Aborting CPM. CasProcessor::" + container.getName() + "
            // Configured to Abort the CPM.");
            // }
            throw ex;
          } // check if the CasProcessor is to be disabled due to excessive errors
          catch (AbortCasProcessorException ex) {
            try {
              handleAbortCasProcessor(container, processor);
              if (cpm.dropCasOnException()) {
                // Cas has already been returned to the CAS pool. The policy requires to stop the
                // processing chain for this CAS and
                // to get another CAS for processing.
                return true;
              } else {
                // Do conditional release of CAS instances. The release occurs only if the
                // CasProcessor is the last one
                // in processing chain. This only releases instances of CAS checked out from the Cas
                // Pool. If not done, the
                // pool gets depleted and no more work will be done.
                releaseCases(casList, (i == (processContainers.size() - 1)), container.getName());
              }
            } catch (ResourceProcessException rpe) {
              throw rpe;
            } catch (Exception rpe) {
              throw new ResourceProcessException(rpe);
            }

            break; // CasProcessor disabled move on to the next one
          } // check if need to redeploy the CasProcessor
          catch (ServiceConnectionException ex) {
            pTrTemp.startEvent(container.getName(), "Process", "");
            try {
              threadState = 2012;

              handleServiceException(container, processor, pTrTemp, ex);
              // processor = null;
              if (cpm.dropCasOnException()) {
                return true;
              } else {
                // Increment number of restarts
                container.incrementRestartCount(1);
                pTrTemp.endEvent(container.getName(), "Process", "success");
                continue; // retry the same CAS'es
              }
            } catch (ResourceProcessException rpe) {
              pTrTemp.endEvent(container.getName(), "Process", "failure");
              throw rpe;
            } catch (ResourceConfigurationException rpe) {
              if (rpe.getCause() != null && rpe.getCause() instanceof KillPipelineException) {
                try {
                  handleKillPipeline(container);
                  processor = null;
                } catch (Exception excep) {
                  // Just log the exception. We are killing the pipeline
                  maybeLogWarning("UIMA_CPM_exception_on_pipeline_kill__WARNING", 
                      container.getName(), excep.getMessage());
                }
              }
              pTrTemp.endEvent(container.getName(), "Process", "failure");
              throw new ResourceProcessException(rpe.getCause());
            } catch (Exception rpe) {
              pTrTemp.endEvent(container.getName(), "Process", "failure");
              throw new ResourceProcessException(rpe);
            }
          } catch (SkipCasException ex) {
            try {
              // Release current Cas Processor before continuing with the next Cas Processor in the
              // pipeline
              if (processor != null) {
                container.releaseCasProcessor(processor);
              }

              handleSkipCasProcessor(container, aCasObjectList,
                      (i == (processContainers.size() - 1)));
            } catch (Exception sEx) {
              throw new ResourceProcessException(sEx);
            }
            processor = null;
            if (cpm.dropCasOnException()) {
              return true;
            } else {
              break;
            }
          } catch (Exception ex) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINER)) {
              logCPM(Level.FINER, "UIMA_CPM_exception__FINER", new Object[] {ex.getMessage()});
              ex.printStackTrace();
            }

          }
          // CAS may have already been dropped (released) due to dropCasOnException policy defined
          // in the CPE Descriptor
          if (!cpm.dropCasOnException()) {
            // Do conditional release of CAS instances. The release occurs only if the CasProcessor
            // is the last one
            // in processing chain. This only releases instances of CAS checked out from the Cas
            // Pool. If not done, the
            // pool gets depleted and no more work will be done.
            releaseCases(casList, (i == (processContainers.size() - 1)), container.getName());
            container.incrementRetryCount(1);
          }
        } // catch

        // Let the container take action if the end-of-batch marker has been reached.
        // End-of-batch marker is defined in the cpm configuration for every CasProcessor.
        // This marker is defined in the <checkpoint> section of the CasProcessor Definition
        // and corresponds to the attribute "batch". If end-of-batch marker is reached the container
        // invokes batchProcessComplete() on the CasProcessor
        maybeLogFinest("UIMA_CPM_end_of_batch__FINEST", container, processor);
        doEndOfBatchProcessing(container, processor, pTrTemp, aCasObjectList);
        processor = null;
      } while (retry); // retry loop

      if (processor != null) {
        maybeLogFinest("UIMA_CPM_release_cp__FINEST", container, processor, casCache);
        container.releaseCasProcessor(processor);
        processor = null;
        maybeLogFinest("UIMA_CPM_ok_release_cp__FINEST", container, processor, casCache);
      }

    } // end of: For All CasProcessors

    try {
      postAnalysis(aCasObjectList, isCasObject, casObjects, pTrTemp, doneAlready);
    } catch (ResourceProcessException rpe) {
      throw rpe;
    } catch (Exception rpe) {
      throw new ResourceProcessException(rpe);
    }
    maybeLogFinest("UIMA_CPM_pipeline_completed__FINEST");
 
    return true;
  }

  /**
   * Notifies application listeners of completed analysis and stores results of analysis (CAS) in
   * the Output Queue that this thread shares with a Cas Consumer thread.
   * 
   * @param aCasObjectList -
   *          List of Artifacts just analyzed
   * @param isCasObject -
   *          determines the types of CAS just analyzed ( CasData vs CasObject)
   * @param casObjects
   * @param aProcessTr -
   *          ProcessTrace object holding events and stats
   * @param doneAlready -
   *          flag to indicate if the last Cas Processor was released back to its container
   * 
   * @throws Exception -
   */
  private void postAnalysis(Object[] aCasObjectList, boolean isCasObject, Object[] casObjects,
          ProcessTrace aProcessTr, boolean doneAlready) throws Exception {
    try {
      maybeLogFinest("UIMA_CPM_pipeline_completed__FINEST");
      // Notify Listeners that the entity has been processed.
      if (!doneAlready && notifyListeners) {
        maybeLogFinest("UIMA_CPM_notify_listeners__FINEST");
        threadState = 2013;
        // Notif Listeners
        EntityProcessStatus aEntityProcStatus = new EntityProcessStatusImpl(aProcessTr);
        notifyListeners(aCasObjectList, isCasObject, aEntityProcStatus);
        threadState = 2014;
        maybeLogFinest("UIMA_CPM_done_notify_listeners__FINEST");
      }
      // enqueue CASes. If the CPM is in shutdown mode due to hard kill dont allow enqueue of CASes
      if (outputQueue != null
              && (cpm.isRunning() == true || (cpm.isRunning() == false && cpm.isHardKilled() == false))) {
        maybeLogFinestWorkQueue("UIMA_CPM_add_cas_to_queue__FINEST", outputQueue);
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
      if (outputQueue == null && casObjects != null && casObjects instanceof CasData[]) {
        if (System.getProperty("DEBUG_RELEASE") != null) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            logFinest("UIMA_CPM_done_with_cas__FINEST", String.valueOf(Runtime.getRuntime().freeMemory() / 1024));  
          }
        }
        for (int i = 0; i < casObjects.length; i++) {
          maybeLogFinest("UIMA_CPM_show_local_cache__FINEST", casCache);
          casObjects[i] = null;
          aCasObjectList[i] = null;
          maybeLogFinest("UIMA_CPM_show_local_cache__FINEST", casCache);
        }
        if (System.getProperty("DEBUG_RELEASE") != null) {          
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            logFinest("UIMA_CPM_show_total_memory__FINEST", String.valueOf(Runtime.getRuntime().freeMemory() / 1024));
          }
        }
      }
    }
  }

  /**
   * Performs end of batch processing. It delegates the processing to Cas Processor container. The
   * container using configuration determines if its time to call Cas Processor's
   * batchProcessComplete() method.
   * 
   * @param aContainer -
   *          container performing end of batch processing
   * @param aProcessor -
   *          Cas Processor to call on end of batch
   * @param aProcessTr -
   *          Process Trace to use for aggregating events
   * @param aCasObjectList -
   *          CASes just analyzed
   */
  private void doEndOfBatchProcessing(ProcessingContainer aContainer, CasProcessor aProcessor,
          ProcessTrace aProcessTr, Object[] aCasObjectList) {
    String cName = aContainer.getName();
    try {
      aProcessTr.startEvent(aContainer.getName(), "End of Batch", "");
      aContainer.isEndOfBatch(aProcessor, aCasObjectList.length);
      aProcessTr.endEvent(cName, "End of Batch", "success");

      maybeLogFinest("UIMA_CPM_end_of_batch_completed__FINEST", aContainer);
    } catch (Exception ex) {
      maybeLogSevere("UIMA_CPM_end_of_batch_exception__SEVERE", 
          aContainer.getName(), ex.getMessage());
      aProcessTr.endEvent(cName, "End of Batch", "failed");

    } finally {
      // Release current Cas Processor before continuing with the next Cas Processor in the pipeline
      if (aProcessor != null) {
        aContainer.releaseCasProcessor(aProcessor);
        maybeLogFinest("UIMA_CPM_ok_released_cp__FINEST", aContainer);
      }
    }

  }

  /**
   * In case a CAS is skipped ( due to excessive exceptions that it causes ), increments stats and
   * totals
   * 
   * @param aContainer
   * @param aCasObjectList
   * @param isLastCP
   * @throws Exception -
   */
  private void handleSkipCasProcessor(ProcessingContainer aContainer, Object[] aCasObjectList,
          boolean isLastCP) throws Exception {

    maybeLogFinest("UIMA_CPM_skipping_cas__FINEST", aContainer);
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
            logSevere("UIMA_CPM_exception_releasing_cas__SEVERE", aContainer.getName());
            maybeLogSevereException(ex2);
            throw ex2;
          }
        }
      }
    }

  }

  /**
   * Handle exceptions related to remote invocations.
   * 
   * @param aContainer -
   *          container managing CasProcessor that failed
   * @param aProcessor -
   *          failed CasProcessor
   * @param aProcessTr -
   *          ProcessTrace object holding events
   * @param ex -
   *          Source exception
   * 
   * @throws Exception -
   */
  private void handleServiceException(ProcessingContainer aContainer, CasProcessor aProcessor,
          ProcessTrace aProcessTr, Exception ex) throws Exception {
    if (aProcessor instanceof NetworkCasProcessorImpl) {
      ((NetworkCasProcessorImpl) aProcessor).collectionProcessComplete(aProcessTr);
    }
    // Add Cas Processor to the list of failed Cas Processors. The list will be used
    // to re-initialize the Cas Processor. If the Cas Processor is fully initialized
    // the Deployer will put the Cas Processor back into the pool
    ((ProcessingContainer_Impl) aContainer).failedCasProcessorList.add(aProcessor);
    // Container is paused ONLY iff CasProcessor is Remote (unmanaged) and
    // all proxies share the same CasProcessor service AND a previous thread initiated service
    // restart. Only one thread can initiate a restart, other threads sharing the same service
    // will block until all connections are re-established

    if (aContainer.isRemote() && aContainer.isSingleFencedService()) {
      if (Thread.currentThread().getName().equals(threadId)) {
        maybeLogFinest("UIMA_CPM_service_connection_exception__FINEST", aContainer, aProcessor);
        aProcessTr.startEvent(aContainer.getName(), "Process", "");
        // Redeploy the CasProcessor
        maybeLogFinest("UIMA_CPM_redeploy_cp__FINEST", aContainer, aProcessor);
       // Reconnect the CPM to CasProcessor running in fenced mode
        cpm.redeployAnalysisEngine(aContainer);

        // Resume the container
        aContainer.resume();
        threadId = null;
        maybeLogFinest("UIMA_CPM_redeploy_cp_done__FINEST", aContainer, aProcessor);
      }

    } else {
      maybeLogFinest("UIMA_CPM_service_connection_exception__FINEST", aContainer, aProcessor);
      aProcessTr.startEvent(aContainer.getName(), "Process", "");
      maybeLogFinest("UIMA_CPM_redeploy_cp__FINEST", aContainer, aProcessor);
      // Reconnect the CPM to CasProcessor running in fenced mode
      cpm.redeployAnalysisEngine(aContainer);
      maybeLogFinest("UIMA_CPM_redeploy_cp_done__FINEST", aContainer, aProcessor);
    }
  }

  /**
   * Diables currect CasProcessor.
   * 
   * @param aContainer -
   *          a container that manages the current Cas Processor.
   * @param aProcessor -
   *          a Cas Processor to be disabled
   * @throws Exception -
   *           exception
   */
  private void handleAbortCasProcessor(ProcessingContainer aContainer, CasProcessor aProcessor)
          throws Exception {
    maybeLogFinest("UIMA_CPM_disable_due_to_action__FINEST", aContainer); 
    if (aContainer.isPaused()) {
      aContainer.resume();
    }
    aContainer.setStatus(Constants.CAS_PROCESSOR_DISABLED);

    // Release current Cas Processor before continuing with the next Cas Processor in the pipeline
    if (aProcessor != null) {
      aContainer.releaseCasProcessor(aProcessor);
      aProcessor = null;
    }
    maybeLogFinest("UIMA_CPM_disabled_cp__FINEST", aContainer);

  }

  /**
   * Terminates the CPM
   * 
   * @param aContainer -
   *          a container that manages the current Cas Processor.
   * @param aProcessor -
   *          a Cas Processor to be disabled
   * @throws Exception -
   *           exception
   */
  private void handleAbortCPM(ProcessingContainer aContainer, CasProcessor aProcessor)
          throws Exception {
    if (aContainer.isPaused()) {
      aContainer.resume();
    }
    aContainer.setStatus(Constants.CAS_PROCESSOR_KILLED);

    maybeLogSevere("UIMA_CPM_abort_cpm__SEVERE", aContainer.getName());
    aContainer.releaseCasProcessor(aProcessor);
    // Release Cas'es. Terminating the CPM. Catch any exception that may occur
    // during CAS release and allow the CPM to abort. The CAS may have already been
    // dropped (returned to the CAS pool) if the dropCasOnException policy is true.
    if (!cpm.dropCasOnException()) {
      try {
        releaseCAS = true;
        releaseCases(casList, true, aContainer.getName());
      } catch (Exception exc) {
        maybeLogSevere("UIMA_CPM_exception_on_cpm_kill__WARNING", aContainer.getName(), exc.getMessage());
        maybeLogSevereException(exc);
      }
    }
    throw new AbortCPMException(CpmLocalizedMessage.getLocalizedMessage(
            CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_configured_to_abort__WARNING",
            new Object[] { Thread.currentThread().getName(), aContainer.getName() }));

  }

  /**
   * Terminates the CPM
   * 
   * @param aContainer -
   *          a container that manages the current Cas Processor.
   * @param aProcessor -
   *          a Cas Processor to be disabled
   * @throws Exception -
   *           exception
   */
  private void handleKillPipeline(ProcessingContainer aContainer) throws Exception {
    if (aContainer.isPaused()) {
      aContainer.resume();
    }
    aContainer.setStatus(Constants.CAS_PROCESSOR_KILLED);
    maybeLogFinest("UIMA_CPM_kill_pipeline__FINEST", aContainer);
    releaseCAS = true;

    if (casCache != null) {
      // First release any CASes from the Cas Cache
      cpm.releaseCASes(casCache);
      casCache = null;
    }

    releaseCases(casList, true, aContainer.getName());
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
      logFinest("UIMA_CPM_releasing_cases__FINEST", 
          aName, String.valueOf(releaseCAS), String.valueOf(lastProcessor));
    }
    if (aCasList == null) {
      return;
    }

    if (releaseCAS && lastProcessor) {
      if (aCasList instanceof CAS[]) {
        if (casCache != null) {
          // First release any CASes from the Cas Cache
          cpm.releaseCASes(casCache);
          casCache = null;
        }
        cpm.releaseCASes((CAS[]) aCasList);
        maybeLogFinest("UIMA_CPM_done_releasing_cases__FINEST", aName);
      } else {
        maybeLogFinest("UIMA_CPM_casobject_class__FINEST", aName, aCasList.getClass().getName());
      }
    }

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
            logWarning("UIMA_CPM_exception_converting_CAS__WARNING");
          }
          casObjectCopy = conversionCas;
        }
        // Notify the listener that the Cas has been processed
//        ((StatusCallbackListener) statCL).entityProcessComplete((CAS) casObjectCopy,
//                aEntityProcStatus);
        CPMEngine.callEntityProcessCompleteWithCAS((StatusCallbackListener) statCL, (CAS) casObjectCopy, aEntityProcStatus);
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

  /**
   * Called by the CPMEngine during setup to indicate that this thread is supposed to release a CAS
   * at the end of processing. This is typically done for Cas Consumer thread, but in configurations
   * not using Cas Consumers The processing pipeline may also release the CAS.
   * 
   * @param aFlag -
   *          true if this thread should release a CAS when analysis is complete
   */
  public void setReleaseCASFlag(boolean aFlag) {
    releaseCAS = aFlag;
  }

  /**
   * Stops all Cas Processors that are part of this PU.
   * 
   * @param kill -
   *          true if CPE has been stopped before finishing processing during external stop
   * 
   */
  public void stopCasProcessors(boolean kill) {
    maybeLogFinest("UIMA_CPM_stop_containers__FINEST");
   // Stop all running CASProcessors
    for (int i = 0; processContainers != null && i < processContainers.size(); i++) {
      ProcessingContainer container = (ProcessingContainer) processContainers.get(i);
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        logFinest("UIMA_CPM_show_container_time__FINEST", 
            container.getName(), String.valueOf(container.getTotalTime()));
      }
      synchronized (container) {
        // Change the status of this container to KILLED if the CPM has been stopped
        // before completing the collection and current status of CasProcessor is
        // either READY or RUNNING
        if (kill || (!cpm.isRunning() && isProcessorReady(container.getStatus()))) {
          maybeLogFinest("UIMA_CPM_kill_cp__FINEST", container);         
          container.setStatus(Constants.CAS_PROCESSOR_KILLED);
        } else {
          // If the CasProcessor has not been disabled during processing change its
          // status to COMPLETED.
          if (container.getStatus() != Constants.CAS_PROCESSOR_DISABLED) {
            container.setStatus(Constants.CAS_PROCESSOR_COMPLETED);
          }
        }
        
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          logFinest("UIMA_CPM_container_status__FINEST", 
              container.getName(), String.valueOf(container.getStatus()));
        }
        ProcessTrace pTrTemp = new ProcessTrace_impl(cpm.getPerformanceTuningSettings());
        pTrTemp.startEvent(container.getName(), "End of Batch", "");
        try {
          CasProcessorDeployer deployer = container.getDeployer();

          if (deployer != null) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              logFinest("UIMA_CPM_undeploy_cp_instances__FINEST", container.getName(), deployer.getClass().getName());
            }
            deployer.undeploy();
          }
          container.destroy();
        } catch (Exception e) {

          logWarning("UIMA_CPM_exception_during_cp_stop__WARNING", container.getName(), e.getMessage());
        } finally {
          pTrTemp.endEvent(container.getName(), "End of Batch", "");
          if (processingUnitProcessTrace != null) {
            this.processingUnitProcessTrace.aggregate(pTrTemp);
          }
        }
      }
    }
  }

  /**
   * Returns true if the CPM has finished analyzing the collection.
   * 
   * @param aCount -
   *          running total of documents processed so far
   * 
   * @return - true if CPM has processed all docs, false otherwise
   */
  protected boolean endOfProcessingReached(long aCount) {
    if (numToProcess == -1) {
      return false;
    } else if (numToProcess == 0) {
      return true;
    } else {
      return (aCount >= numToProcess);
    }
  }

  /**
   * 
   * @param anArtifact
   */
  protected void process(Object anArtifact) {
    if (anArtifact instanceof Object[]) {
      Object[] cases = (Object[]) anArtifact;
      showMetadata(cases);
    } else {

    }
  }

  /**
   * 
   * @param aCasList
   */
  protected void showMetadata(Object[] aCasList) {
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
   * @param aPool
   */
  public void setCasPool(CPECasPool aPool) {
    casPool = aPool;
  }

  private boolean filterOutTheCAS(ProcessingContainer aContainer, boolean isCasObject,
          Object[] aCasObjectList) {
    // Check if any of the Cas'es in the set has a required feature structure.
    // This is currently only supported for the CasData instances and provides
    // filtering mechanism
    if (!isCasObject && !aContainer.processCas(aCasObjectList)) {
      maybeLogFinest("UIMA_CPM_skip_CAS__FINEST", aContainer);
      aContainer.incrementFilteredCount(aCasObjectList.length);
      aContainer.logAbortedCases(aCasObjectList);
      return true; // skipped this CAS
    }
    return false;

  }

  private boolean containerDisabled(ProcessingContainer aContainer) {
    synchronized (aContainer) {
      // Check to see if the CasProcessor is available for processing
      if (!isProcessorReady(aContainer.getStatus())) {
        maybeLogFinest("UIMA_CPM_container_not_ready__FINEST", aContainer);
        // Skip any CasProcessor that is not ready to process. Cas Processors may be disabled during
        // processing
        return true;
      }
    }
    return false;
  }

  /* **************************************************************** */
  /**
   * An alternate processing loop designed for the single-threaded CPM.
   * 
   * @param aCasObjectList -
   *          a list of CASes to analyze
   * @param pTrTemp -
   *          process trace where statistics are added during analysis
   * 
   */
  protected boolean analyze(Object[] aCasObjectList, ProcessTrace pTrTemp) throws Exception // throws
  // ResourceProcessException,
  // IOException,
  // CollectionException,
  // AbortCPMException
  {
    long t1 = 0;
    maybeLogFinest("UIMA_CPM_start_analysis__FINEST");
    // String lastDocId = "";
    CasProcessor processor = null;
    // This is used to hold an index of the current CasObject
    boolean doneAlready = false;
    // If there are no CASes in the list, return false since there is nothing else to do
    if (aCasObjectList == null || aCasObjectList[0] == null) {
      maybeLogSevere("UIMA_CPM_invalid_cas_reference__SEVERE");
      return false;
    }
    Object[] casObjects = null;
    // Determine if the Cas'es contained in the CasList are of type CAS. Samples the first CAS in
    // the list.
    // The list contains CASes of the same type ( either CasData or CAS ). Mixed model not
    // supported.
    boolean isCasObject = aCasObjectList[0] instanceof CAS;
    // String docid = "";
    maybeLogFinest("UIMA_CPM_entering_pipeline__FINEST");

    ProcessingContainer container = null;
    String containerName = "";
    // *******************************************
    // ** P R O C E S S I N G P I P E L I N E **
    // *******************************************
    // Send Cas Object through the processing pipeline.
    for (int i = 0; processContainers != null && i < processContainers.size(); i++) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        logFinest("UIMA_CPM_retrieve_container__FINEST", String.valueOf(i));
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
            maybeLogMemoryFinest();
          }

          maybeLogFinest("UIMA_CPM_checkout_cp_from_container__FINEST", container);
          threadState = 2004;
          t1 = System.currentTimeMillis();
          // Get the CasProcessor from the pool managed by the container
          processor = container.getCasProcessor();
          timer01 += (System.currentTimeMillis() - t1);
          timer06 = ((ProcessingContainer_Impl) container).getFetchTime();
          if (processor == null) {
            maybeLogSevere("UIMA_CPM_checkout_null_cp_from_container__SEVERE", containerName);
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
            maybeLogFinest("UIMA_CPM_container_not_ready__FINEST", container);
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

          maybeLogFinest("UIMA_CPM_checkedout_cp_from_container__FINEST", container, processor);
          t1 = System.currentTimeMillis();
          // ************************* P E R F O R M A N A L Y S I S *************************
          if (processor instanceof CasDataProcessor) {
            invokeCasDataCasProcessor(container, processor, aCasObjectList, pTrTemp, isCasObject,
                    retry);
            isCasObject = false;
          } else if (processor instanceof CasObjectProcessor) {
            invokeCasObjectCasProcessor(container, processor, aCasObjectList, pTrTemp, isCasObject);
            isCasObject = true;
          }
          timer02 += (System.currentTimeMillis() - t1);
          maybeLogFinest("UIMA_CPM_analysis_successfull__FINEST", container, processor);
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
            maybeLogFinest("UIMA_CPM_end_of_batch__FINEST", container, processor);
            if (isProcessorReady(container.getStatus())) {
              t1 = System.currentTimeMillis();

              // Let the container take action if the end-of-batch marker has been reached.
              // End-of-batch marker is defined in the cpm configuration for every CasProcessor.
              // This marker is defined in the <checkpoint> section of the CasProcessor Definition
              // and corresponds to the attribute "batch". If end-of-batch marker is reached the
              // container
              // invokes batchProcessComplete() on the CasProcessor
              doEndOfBatch(container, processor, pTrTemp, aCasObjectList.length);
              timer03 += (System.currentTimeMillis() - t1);
            }
          } else {
            container.incrementRetryCount(1);
          }
          // Release current Cas Processor before continuing with the next Cas Processor in the
          // pipeline
          if (processor != null) {
            maybeLogFinest("UIMA_CPM_release_cp__FINEST", container, processor, casCache);
            t1 = System.currentTimeMillis();
            doReleaseCasProcessor(container, processor);
            timer04 += (System.currentTimeMillis() - t1);
            maybeLogFinest("UIMA_CPM_ok_release_cp__FINEST", container, processor, casCache);
            processor = null;
          }

        }

      } while (retry);
    } // end of: For All CasProcessors

    t1 = System.currentTimeMillis();
    postAnalysis(aCasObjectList, isCasObject, casObjects, pTrTemp, doneAlready);
    timer05 += (System.currentTimeMillis() - t1);
    casObjects = null;
    return true;
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
      maybeLogFinest("UIMA_CPM_end_of_batch_completed__FINEST", aContainer);
    } catch (Exception ex) {
        maybeLogSevere("UIMA_CPM_end_of_batch_exception__SEVERE", containerName, ex.getMessage());
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
    maybeLogSevereException(e);
    maybeLogSevere("UIMA_CPM_handle_exception__SEVERE", 
        containerName, aProcessor.getClass().getName(), e.getMessage());

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
      logWarning("UIMA_CPM_drop_cas__WARNING", containerName, aProcessor.getClass().getName());
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

      maybeLogFinest("UIMA_CPM_container_paused_do_retry__FINEST", aContainer);
      return true; // retry
    }
    if (e instanceof Exception && pauseContainer(aContainer, (Exception) e, threadId)) {
      maybeLogFinest("UIMA_CPM_pausing_container__FINEST", aContainer);
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
      maybeLogFinest("UIMA_CPM_EXP_configured_to_abort__WARNING", aProcessor);
      throw new AbortCPMException(CpmLocalizedMessage.getLocalizedMessage(
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_configured_to_abort__WARNING",
              new Object[] { Thread.currentThread().getName(), containerName }));
    } // check if the CasProcessor is to be disabled due to excessive errors
    catch (AbortCasProcessorException ex) {
      retry = false;
      maybeLogFinest("UIMA_CPM_disable_cp__SEVERE", aProcessor);
      if (aContainer.isPaused()) {
        aContainer.resume();
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
      maybeLogSevereException(ex);
      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        // done as 2 messages because there is no method supporting 
        // both a Throwable, and a message with substitutable args, in the logger
        logSevere("UIMA_CPM_unhandled_error__SEVERE", e.getLocalizedMessage());
        UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE,
            this.getClass().getName(),
            "handleErrors",
            CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
            "UIMA_CPM_unexpected_exception__SEVERE",
            ex);     
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
    maybeLogMemoryFinest();
    maybeLogFinest("UIMA_CPM_invoke_cp_process__FINEST", container, processor);
    casList = new CAS[aCasObjectList.length];
    for (int casIndex = 0; casIndex < aCasObjectList.length; casIndex++) {
      maybeLogFinest("UIMA_CPM_initialize_cas__FINEST", container);
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
        maybeLogFinest("UIMA_CPM_call_process__FINEST", container, processor);
        threadState = 2005;

        pTrTemp.aggregate(((AnalysisEngine) processor).process(casList[casIndex]));
        maybeLogFinest("UIMA_CPM_call_process_completed__FINEST", container, processor);
      } else {
        pTrTemp.startEvent(container.getName(), "Process", "");
        threadState = 2006;
        maybeLogFinest("UIMA_CPM_call_process__FINEST", container, processor);
        ((CasObjectProcessor) processor).processCas(casList[casIndex]);
        maybeLogFinest("UIMA_CPM_call_process_completed__FINEST", container, processor);
      }
      pTrTemp.endEvent(container.getName(), "Process", "success");
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
        maybeLogFinest("UIMA_CPM_get_cas_from_pool__FINEST", aContainerName);

        // Retrieve a Cas from Cas Pool. Wait max 10 millis for an instance
        casList[casIndex] = casPool.getCas(0);
        maybeLogFinest("UIMA_CPM_got_cas_from_pool__FINEST", aContainerName);
      }
      if (casList[casIndex] != null) {
        maybeLogFinest("UIMA_CPM_call_cas_reset__FINEST", aContainerName);
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
    maybeLogFinest("UIMA_CPM_cas_data_processor__FINEST", container, processor);
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
    if (!(casObjects instanceof CasData[])) {
      maybeLogFinest("UIMA_CPM_expected_casdata__FINEST", casObjects.getClass().getName());
    }
    maybeLogFinest("UIMA_CPM_call_process__FINEST", container, processor);
    casObjects = ((CasDataProcessor) processor).process((CasData[]) casObjects);
    maybeLogFinest("UIMA_CPM_call_process_completed__FINEST", container, processor);
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
  
 
  /**
   * loggers
   *   Special forms for frequent args sets
   *   "maybe" versions test isLoggable
   *   
   *   Additional args passed as object array to logger
   *
   */
  
  private static final Object [] zeroLengthObjectArray = new Object[0];
  private static final String thisClassName = ProcessingUnit.class.getName();
  
  private void logCPM(Level level, String msgBundleId, Object[] args) {
    if (null == args) {
      args = zeroLengthObjectArray;
    }
    Object[] aa = new Object[args.length + 1];
    aa[0] = Thread.currentThread().getName();
    System.arraycopy(args, 0, aa, 1, args.length);
  
    UIMAFramework.getLogger(this.getClass()).logrb(
        level,
        thisClassName,
        "process",  // used as the method name
        CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
        msgBundleId,
        aa
        );
  }

  // 0 arg
  private void maybeLogFinest(String msgBundleId) {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      logFinest(msgBundleId);
    }
  }
  
  private void logFinest(String msgBundleId) {
    logCPM(Level.FINEST, msgBundleId, null);
  }
 
  // 1 arg
  private void maybeLogFinest(String msgBundleId, String arg1) {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      logFinest(msgBundleId, arg1);
    }
  }
  
  private void logFinest(String msgBundleId, String arg1) {
    logCPM(Level.FINEST, msgBundleId, new Object [] {arg1});
  }

  // 2 args
  private void maybeLogFinest(String msgBundleId, String arg1, String arg2) {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      logFinest(msgBundleId, arg1, arg2);
    }
  }
  
  private void logFinest(String msgBundleId, String arg1, String arg2) {
    logCPM(Level.FINEST, msgBundleId, new Object [] {arg1, arg2});
  }
  
  // 3 args
  
  private void logFinest(String msgBundleId, String arg1, String arg2, String arg3) {
    logCPM(Level.FINEST, msgBundleId, new Object [] {arg1, arg2, arg3});
  }


  // special common 2 arg version with container, processor
  private void maybeLogFinest(String msgBundleId, ProcessingContainer container, CasProcessor processor) {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      logFinest(msgBundleId, container.getName(), processor.getClass().getName());
    }
  }
  
  private void logFinest(String msgBundleId, ProcessingContainer container, CasProcessor processor) {
    logFinest(msgBundleId, container.getName(), processor.getClass().getName());
  }
  
  private void maybeLogFinest(String msgBundleId, ProcessingContainer container) {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      logFinest(msgBundleId, container.getName());
    }
  }
  
  private void maybeLogFinest(String msgBundleId, CasProcessor processor) {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      logFinest(msgBundleId, processor.getClass().getName());
    }
  }
  
  private void maybeLogFinest(String msgBundleId, ProcessingContainer container, CasProcessor processor, CAS [] casCache) {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      logFinest(msgBundleId, container.getName(), processor.getClass().getName(),
          String.valueOf(casCache == null));
    }
  }
  
  private void maybeLogFinest(String msgBundleId, CAS [] casCache) {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      logFinest(msgBundleId, String.valueOf(casCache == null));
    }
  }

  
  private void maybeLogMemoryFinest() {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      logMemoryFinest();
    }
  }
  
  private void logMemoryFinest() {
    logFinest("UIMA_CPM_show_memory__FINEST", 
          String.valueOf(Runtime.getRuntime().totalMemory() / 1024),
          String.valueOf(Runtime.getRuntime().freeMemory() / 1024));
  }

  private void logWarning(String msgBundleId) {
    logCPM(Level.WARNING, msgBundleId, null);
  }

  private void maybeLogWarning(String msgBundleId, String arg1, String arg2) {
    if (UIMAFramework.getLogger().isLoggable(Level.WARNING)) {
      logWarning(msgBundleId, arg1, arg2);
    }
  }
  
  private void logWarning(String msgBundleId, String arg1, String arg2) {
    logCPM(Level.WARNING, msgBundleId, new Object [] {arg1, arg2});
  }
  
  private void maybeLogSevere(String msgBundleId) {
    if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
      logCPM(Level.SEVERE, msgBundleId, null);
    }
  }

  
  private void maybeLogSevere(String msgBundleId, String arg1) {
    if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
      logSevere(msgBundleId, arg1);
    }
  }
  
  private void logSevere(String msgBundleId, String arg1) {
    logCPM(Level.SEVERE, msgBundleId, new Object[] {arg1});
  }

  
  private void maybeLogSevere(String msgBundleId, String arg1, String arg2) {
    if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
      logSevere(msgBundleId, arg1, arg2);
    }
  }
  
  private void logSevere(String msgBundleId, String arg1, String arg2) {
    logCPM(Level.SEVERE, msgBundleId, new Object[] {arg1, arg2});
  }

  private void maybeLogSevere(String msgBundleId, String arg1, String arg2, String arg3) {
    if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
      logSevere(msgBundleId, arg1, arg2, arg3);
    }
  }
  private void logSevere(String msgBundleId, String arg1, String arg2, String arg3) {
    logCPM(Level.SEVERE, msgBundleId, new Object[] {arg1, arg2, arg3});
  }
  
  private void maybeLogSevereException(Throwable e) {
    if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
      String m = "Thread: " + Thread.currentThread().getName() + ", message: " +  e.getLocalizedMessage();
      UIMAFramework.getLogger().log(Level.SEVERE, m, e);
    }
  }
  
  private void maybeLogFinestWorkQueue(String msgBundleId, BoundedWorkQueue workQueue) {
    if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
      logFinest(msgBundleId, workQueue.getName(), String.valueOf(workQueue.getCurrentSize()));
    }
  }

}
