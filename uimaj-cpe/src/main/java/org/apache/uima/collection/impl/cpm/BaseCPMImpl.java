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

package org.apache.uima.collection.impl.cpm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.base_cpm.AbortCPMException;
import org.apache.uima.collection.base_cpm.BaseCPM;
import org.apache.uima.collection.base_cpm.BaseCollectionReader;
import org.apache.uima.collection.base_cpm.BaseStatusCallbackListener;
import org.apache.uima.collection.base_cpm.CasProcessor;
import org.apache.uima.collection.base_cpm.RecoverableCollectionReader;
import org.apache.uima.collection.base_cpm.SynchPoint;
import org.apache.uima.collection.impl.EntityProcessStatusImpl;
import org.apache.uima.collection.impl.base_cpm.container.ProcessingContainer;
import org.apache.uima.collection.impl.cpm.container.CPEFactory;
import org.apache.uima.collection.impl.cpm.container.deployer.socket.ProcessControllerAdapter;
import org.apache.uima.collection.impl.cpm.engine.CPMEngine;
import org.apache.uima.collection.impl.cpm.engine.CPMThreadGroup;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.CasMetaData;
import org.apache.uima.collection.impl.cpm.utils.CpmLocalizedMessage;
import org.apache.uima.collection.impl.cpm.utils.TimerFactory;
import org.apache.uima.collection.metadata.CpeConfiguration;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.util.Level;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.ProcessTraceEvent;
import org.apache.uima.util.Progress;
import org.apache.uima.util.UimaTimer;
import org.apache.uima.util.impl.ProcessTrace_impl;

/**
 * Main thread that launches CPE and manages it. An application interacts with the running CPE via
 * this object. Through an API, an application may start, pause, resume, and stop a CPE.
 * 
 * 
 */
public class BaseCPMImpl implements BaseCPM, Runnable {
  private boolean defaultProcessTrace;

  private CPMEngine cpEngine = null;

  private ProcessTrace procTr = null;

  BaseCollectionReader collectionReader = null;

  private Checkpoint checkpoint = null;

  private CheckpointData checkpointData = null;

  private long num2Process = -1; // Default ALL

  private boolean killed = false;

  private boolean completed = false;

  private CPEFactory cpeFactory = null;

  private boolean useJediiReport = true;

  private Map mEventTypeMap;

  public CPMThreadGroup cpmThreadGroup = null;

  /**
   * Instantiates and initializes CPE Factory with a given CPE Descriptor and defaults.
   * 
   * @param aDescriptor -
   *          parsed CPE descriptor
   * @throws Exception -
   */
  public BaseCPMImpl(CpeDescription aDescriptor) throws Exception {
    this(aDescriptor, null, true, UIMAFramework.getDefaultPerformanceTuningProperties());
    cpmThreadGroup = new CPMThreadGroup("CPM Thread Group");
  }

  /**
   * Instantiates and initializes CPE Factory responsible for creating individual components that
   * are part of the processing pipeline.
   * 
   * @param aDescriptor -
   *          parsed CPE descriptor
   * @param aResourceManager -
   *          ResourceManager instance to be used by the CPE
   * @param aDefaultProcessTrace -
   *          ProcessTrace instance to capture events and stats
   * @throws Exception -
   */
  public BaseCPMImpl(CpeDescription aDescriptor, ResourceManager aResourceManager,
          boolean aDefaultProcessTrace, Properties aProps) throws Exception {
    cpeFactory = new CPEFactory(aDescriptor, aResourceManager);
    defaultProcessTrace = aDefaultProcessTrace;
    cpmThreadGroup = new CPMThreadGroup("CPM Thread Group");
    init(false, aProps);
  }

  /**
   * Parses CPE descriptor
   * 
   * @param mode -
   *          indicates if the CPM should use a static descriptor or one provided
   * @param aDescriptor -
   *          provided descriptor path
   * @param aResourceManager
   *          ResourceManager to be used by CPM
   * 
   * @throws Exception -
   */
  public BaseCPMImpl(Boolean mode, String aDescriptor, ResourceManager aResourceManager)
          throws Exception {
    cpmThreadGroup = new CPMThreadGroup("CPM Thread Group");
    cpeFactory = new CPEFactory(aResourceManager);
    if (mode == null) {
      defaultProcessTrace = true;
      cpeFactory.parse();
    } else {
      defaultProcessTrace = mode.booleanValue();
      cpeFactory.parse(aDescriptor);
    }
    init(mode == null, UIMAFramework.getDefaultPerformanceTuningProperties());
  }

  /**
   * Plugs in custom perfomance tunning parameters
   * 
   * @param aPerformanceTuningSettings
   */
  public void setPerformanceTuningSettings(Properties aPerformanceTuningSettings) {
    cpEngine.setPerformanceTuningSettings(aPerformanceTuningSettings);
  }

  /**
   * Plugs in a given {@link ProcessControllerAdapter}. The CPM uses this adapter to request Cas
   * Processor restarts and shutdown.
   * 
   * @param aPca -
   *          instance of the ProcessControllerAdapter
   */
  public void setProcessControllerAdapter(ProcessControllerAdapter aPca) {
    cpEngine.setProcessControllerAdapter(aPca);
  }

  /**
   * Sets Jedii-style reporting resources and sets the global flag to indicate what report-style to
   * use at the end of processing. Jedii-style reporting shows a summary for this run. The CPM
   * default report shows more detail information.
   * 
   * @param aUseJediiReport
   */
  public void setJediiReport(boolean aUseJediiReport) {
    mEventTypeMap = new HashMap();
    mEventTypeMap.put(ProcessTraceEvent.ANALYSIS_ENGINE, "TAE");
    mEventTypeMap.put(ProcessTraceEvent.ANALYSIS, "Annotator");
    mEventTypeMap.put("CAS_PROCESSOR", "CAS Consumer");
    useJediiReport = aUseJediiReport;
  }

  /**
   * Instantiates and initializes a CPE.
   * 
   * @param aDummyCasProcessor -
   * 
   * @throws Exception -
   */
  public void init(boolean aDummyCasProcessor, Properties aProps) throws Exception {
    String uimaTimerClass = cpeFactory.getCPEConfig().getTimerImpl();
    try {
      new TimerFactory(uimaTimerClass);
    } catch (Exception e) {
      // e.printStackTrace();
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_use_default_timer__FINEST",
                new Object[] { Thread.currentThread().getName() });

      }
    }
    UimaTimer uimaTimer = TimerFactory.getTimer();
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_use_custom_timer__FINEST",
              new Object[] { Thread.currentThread().getName(), uimaTimer.getClass().getName() });
    }

    procTr = new ProcessTrace_impl(uimaTimer, aProps);
    String checkpointFileName = null;
    if (cpeFactory.getCPEConfig().getCheckpoint() != null
            && cpeFactory.getCPEConfig().getCheckpoint().getFilePath() != null) {
      // Retrieve from CPM configuration a name of the checkpoint file where the
      // CPM's runtime stats be deposited.
      checkpointFileName = cpeFactory.getCPEConfig().getCheckpoint().getFilePath();
    }
    if (checkpointFileName != null && checkpointFileName.trim().length() > 0) {
      File checkpointFile = new File(checkpointFileName);
      checkpoint = new Checkpoint(this, checkpointFileName, cpeFactory.getCPEConfig()
              .getCheckpoint().getFrequency());
      // Check if the checkpoint file already exists. If it does, the CPM did not complete
      // successfully during the previous run and CPM will start in recovery mode, restoring all
      // totals and status's from the recovered checkpoint. The processing pipeline state will
      // restored to the state as of before foreced shutdown. All CasProcessors that were disabled
      // during that run will remain disabled. The CollectionReader will be advanced to the
      // entity last processed by the previous CPM.
      if (checkpointFile.exists()) {
        try {
          Object restoredObject = checkpoint.restoreFromCheckpoint();
          if (restoredObject != null && restoredObject instanceof CheckpointData) {
            checkpointData = (CheckpointData) restoredObject;
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    // Instantiate class responsible for processing
    cpEngine = new CPMEngine(cpmThreadGroup, cpeFactory, procTr, checkpointData);
    if (!aDummyCasProcessor) {
      int concurrentThreadCount = cpeFactory.getCpeDescriptor().getCpeCasProcessors()
              .getConcurrentPUCount();
      for (int threadCount = 0; threadCount < concurrentThreadCount; threadCount++) {
        CasProcessor[] casProcessors = cpeFactory.getCasProcessors();
        for (int i = 0; i < casProcessors.length; i++) {
          if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.CONFIG,
                    this.getClass().getName(),
                    "process",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_add_cp__CONFIG",
                    new Object[] { Thread.currentThread().getName(),
                        casProcessors[i].getProcessingResourceMetaData().getName() });
          }
          addCasProcessor(casProcessors[i]);
        }
      }
    }
    int casPoolSize = 0;
    try {
      casPoolSize = cpeFactory.getCpeDescriptor().getCpeCasProcessors().getCasPoolSize();
      casPoolSize = (casPoolSize == -1) ? 0 : casPoolSize;
      cpEngine.setPoolSize(casPoolSize);
    } catch (NumberFormatException e) {
      // PoolSize is currently an optional parameter. If it does not exist in CPE descriptor a
      // default
      // value will be derived from defined queue sizes
    }

    try {
      int iqSize = 0;
      if (casPoolSize == 0) {
        iqSize = cpeFactory.getCpeDescriptor().getCpeCasProcessors().getInputQueueSize();
      }
      cpEngine.setInputQueueSize(casPoolSize == 0 ? iqSize : casPoolSize);
    } catch (NumberFormatException e) {
      throw new Exception(CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_EXP_queue_size_not_defined__WARNING", new Object[] {
                  Thread.currentThread().getName(), "inputQueueSize" }));
    }
    try {
      int oqSize = 0;
      if (casPoolSize == 0) {
        oqSize = cpeFactory.getCpeDescriptor().getCpeCasProcessors().getOutputQueueSize();
      }
      cpEngine.setOutputQueueSize(casPoolSize == 0 ? oqSize : casPoolSize + 2);
    } catch (NumberFormatException e) {
      throw new Exception(CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_EXP_queue_size_not_defined__WARNING", new Object[] {
                  Thread.currentThread().getName(), "outputQueueSize" }));
    }
    try {
      int threadCount = cpeFactory.getCpeDescriptor().getCpeCasProcessors().getConcurrentPUCount();
      cpEngine.setConcurrentThreadSize(threadCount);
    } catch (NumberFormatException e) {
      throw new Exception(CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_EXP_invalid_component_reference__WARNING", new Object[] {
                  Thread.currentThread().getName(), "casProcessors", "processingUnitThreadCount" }));
    }
  }

  /**
   * Returns {@link CPEConfig} object holding current CPE configuration
   * 
   * @return CPEConfig instance
   * 
   * @throws Exception -
   */
  public CpeConfiguration getCPEConfig() throws Exception {
    return cpeFactory.getCPEConfig();
  }

  /*
   * Returns All CasProcessors currently in the processing pipeline
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#getCasProcessors()
   */
  public CasProcessor[] getCasProcessors() {
    CasProcessor[] casProcs = cpEngine.getCasProcessors();
    return casProcs == null ? new CasProcessor[0] : casProcs;
  }

  /*
   * Adds given CasProcessor to the processing pipeline. A new CasProcessor is appended to the
   * current list.
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#addCasProcessor(org.apache.uima.collection.base_cpm.CasProcessor)
   */
  public void addCasProcessor(CasProcessor aCasProcessor) throws ResourceConfigurationException {
    cpEngine.addCasProcessor(aCasProcessor);
  }

  /*
   * Adds given CasProcessor to the processing pipeline. A new CasProcessor is inserted into a given
   * spot in the current list.
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#addCasProcessor(org.apache.uima.collection.base_cpm.CasProcessor,
   *      int)
   */
  public void addCasProcessor(CasProcessor aCasProcessor, int aIndex)
          throws ResourceConfigurationException {
    cpEngine.addCasProcessor(aCasProcessor, aIndex);
  }

  /*
   * Removes given CasProcessor from the processing pipeline.
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#removeCasProcessor(org.apache.uima.collection.base_cpm.CasProcessor)
   */
  public void removeCasProcessor(CasProcessor aCasProcessor) {
    cpEngine.removeCasProcessor(0);
  }

  /*
   * Disables given CasProcessor in the existing processing pipeline.
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#disableCasProcessor(org.apache.uima.collection.base_cpm.CasProcessor)
   */
  public void disableCasProcessor(String aCasProcessorName) {

    cpEngine.disableCasProcessor(aCasProcessorName);
  }

  /*
   * Disables given CasProcessor in the existing processing pipeline.
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#disableCasProcessor(org.apache.uima.collection.base_cpm.CasProcessor)
   */
  public void enableCasProcessor(String aCasProcessorName) {

    cpEngine.enableCasProcessor(aCasProcessorName);
  }

  /*
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#isSerialProcessingRequired()
   */
  public boolean isSerialProcessingRequired() {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#setSerialProcessingRequired(boolean)
   */
  public void setSerialProcessingRequired(boolean aRequired) {
  }

  /*
   * Returns true if this cpEngine pauses on exception. False otherwise.
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#isPauseOnException()
   */
  public boolean isPauseOnException() {
    return cpEngine.isPauseOnException();
  }

  /*
   * Defines if cpEngine should pause on exception
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#setPauseOnException(boolean)
   */
  public void setPauseOnException(boolean aPause) {
    cpEngine.setPauseOnException(aPause);
  }

  /*
   * Adds Event Listener. Important events like, end of entity processing, exceptions, etc will be
   * sent to the registered listeners.
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#addStatusCallbackListener(org.apache.uima.collection.base_cpm.BaseStatusCallbackListener)
   */
  public void addStatusCallbackListener(BaseStatusCallbackListener aListener) {
    cpEngine.addStatusCallbackListener(aListener);
  }

  /*
   * Remoces named listener from the listener list.
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#removeStatusCallbackListener(org.apache.uima.collection.base_cpm.BaseStatusCallbackListener)
   */
  public void removeStatusCallbackListener(BaseStatusCallbackListener aListener) {
    cpEngine.removeStatusCallbackListener(aListener);
  }

  /*
   * Starting point for the CPE. Before starting processing, the CPE must deploy all CasProcessors.
   * Once all are deployed the processing begins in Worker Thread called CPEngine. This thread
   * blocks until the CPEngine thread completes. CPMWorker Thread finishes.
   * 
   * @see java.lang.Runnable#run()
   */
  public void run() {
    long start, end;
    // name this thread
    Thread.currentThread().setName("BaseCPMImpl-Thread");

    start = System.currentTimeMillis();
    if (!useJediiReport) {
      procTr.startEvent("CPM", "CPM PROCESSING TIME", "");
    }

    // Specify how docs to process
    cpEngine.setNumToProcess(num2Process);
    try {
      // Deploy all CAS processors
      cpEngine.deployCasProcessors();
      cpEngine.setCollectionReader(collectionReader);

      // Start the checkpoint thread
      if (checkpoint != null) {
        new Thread(checkpoint).start();
      }
      cpEngine.start();
      // Joing the CPMWorker Thread and wait until it finishes
      cpEngine.join();

      completed = true;
      // If the entire collection has been processed there is no need for a checkpoint.
      // Delete it, there is no need to recover anything. Otherwise, the CPM has been killed
      // and may need to be recovered. Checkpoint file contains the status of the CPM, including
      // last document processed, status of all CasProcessors along with all counts and totals.
      if (!killed && checkpoint != null) {
        checkpoint.stop();
        checkpoint.delete();
        checkpoint = null;
      }
      // Terminate all threads and running services
      cpEngine.stopCasProcessors(false);
      // Notify Listeners that the processing pipeline has finished
      if (!useJediiReport) {
        procTr.endEvent("CPM", "CPM PROCESSING TIME", "success");
      }
      end = System.currentTimeMillis();
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_show_total_time_in_cpm__FINEST",
                new Object[] { Thread.currentThread().getName(), String.valueOf(end - start) });
      }
    } catch (AbortCPMException e) {
      if (!useJediiReport) {
        procTr.endEvent("CPM", "CPM PROCESSING TIME", "failed");
      }
      // Terminate all threads and running services
      try {
        cpEngine.stopCasProcessors(true);
      } catch (Exception ex) {
        UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, "" + ex);
      }
      killed = true;
    } catch (Exception e) {
      if (!useJediiReport) {
        procTr.endEvent("CPM", "CPM PROCESSING TIME", "failed");
      }
      UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, "" + e);
      killed = true;
      ArrayList statusCbL = cpEngine.getCallbackListeners();
      EntityProcessStatusImpl enProcSt = new EntityProcessStatusImpl(procTr, true);
      // e is the actual exception.
      enProcSt.addEventStatus("CPM", "Failed", e);

      // Notify all listeners that the CPM has finished processing
      for (int j = 0; j < statusCbL.size(); j++) {
        BaseStatusCallbackListener st = (BaseStatusCallbackListener) statusCbL.get(j);
        if (st != null && st instanceof StatusCallbackListener) {
          ((StatusCallbackListener) st).entityProcessComplete(null, enProcSt);
        }
      }
    }

    if (cpEngine.isKilled()) {
      killed = true;
    }

    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_cpm_stopped__FINEST",
              new Object[] { Thread.currentThread().getName(), String.valueOf(killed) });
    }
    ArrayList statusCbL = cpEngine.getCallbackListeners();
    // Notify all listeners that the CPM has finished processing
    for (int j = 0; j < statusCbL.size(); j++) {
      BaseStatusCallbackListener st = (BaseStatusCallbackListener) statusCbL.get(j);
      if ( st != null ) {
        if (!killed) {
          st.collectionProcessComplete();
        } else {
          st.aborted();
        }
      }
    }
  }

  /**
   * Called to cleanup CPE on shutdown
   * 
   */
  public void finalizeIt() {
    // Do cleanup before terminating self
    cpEngine.cleanup();

  }

  /**
   * This method is called by an applications to begin CPM processing with a given Collection. It
   * just creates a new thread and starts it.
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#process()
   * @deprecated
   */
  @Deprecated
public void process(BaseCollectionReader aCollectionReader)
          throws ResourceInitializationException {
    // Retrieve number of entities to process from the CPM configuration
    try {
      num2Process = cpeFactory.getCPEConfig().getNumToProcess();
    } catch (InstantiationException e) {
      throw new ResourceInitializationException(e);
    }
    collectionReader = aCollectionReader;
    if (cpeFactory.isDefault()) {
      cpeFactory.addCollectionReader(collectionReader);
    }
    cpmThreadGroup.setProcessTrace(procTr);
    cpmThreadGroup.setListeners(cpEngine.getCallbackListeners());
    new Thread(this).start();
  }

  /*
   * This method is called by an application to begin processing given Collection. It creates a new
   * thread, adds it to a ThreadGroup and starts it.
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#process(org.apache.uima.collection.base_cpm.BaseCollectionReader)
   */
  public void process() throws ResourceInitializationException {
    // Retrieve number of entities to process from the CPM configuration
    try {
      num2Process = cpeFactory.getCPEConfig().getNumToProcess();
      if (collectionReader == null) {
        collectionReader = cpeFactory.getCollectionReader();
      }
    } catch (InstantiationException e) {
      throw new ResourceInitializationException(e);
    } catch (Exception e) {
      throw new ResourceInitializationException(e);
    }
    if (cpeFactory.isDefault()) {
      cpeFactory.addCollectionReader(collectionReader);
    }
    cpmThreadGroup.setProcessTrace(procTr);
    cpmThreadGroup.setListeners(cpEngine.getCallbackListeners());

    new Thread(this).start();
  }

  /**
   * 
   * This method is called by an applications to begin CPM processing with a given Collection. It
   * just creates a new thread and starts it.
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#process()
   * @deprecated
   */
  @Deprecated
public void process(BaseCollectionReader aCollectionReader, int aBatchSize)
          throws ResourceInitializationException {
    // Let the application define the size of Collection.
    num2Process = aBatchSize;
    collectionReader = aCollectionReader;
    if (cpeFactory.isDefault()) {
      cpeFactory.addCollectionReader(collectionReader);
    }
    cpmThreadGroup.setProcessTrace(procTr);
    cpmThreadGroup.setListeners(cpEngine.getCallbackListeners());

    new Thread(cpmThreadGroup, this).start();
  }

  /**
   * Sets the Collection Reader for this CPE.
   * 
   * @param aCollectionReader
   *          the collection reader
   */
  public void setCollectionReader(BaseCollectionReader aCollectionReader) {
    collectionReader = aCollectionReader;
    if (cpeFactory.isDefault()) {
      cpeFactory.addCollectionReader(collectionReader);
    }
  }

  /**
   * Returns a Collection Reader for this CPE.
   * 
   * @return the collection reader
   */
  public BaseCollectionReader getCollectionReader() {
    try {
      if (this.collectionReader == null) {
        this.collectionReader = this.cpeFactory.getCollectionReader();
      }
      return this.collectionReader;
    } catch (ResourceConfigurationException e) {
      throw new UIMARuntimeException(e);
    }
  }

  /*
   * Returns current state of the CPM.
   * 
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#isProcessing()
   */
  public boolean isProcessing() {
    return cpEngine.isRunning();
  }

  /*
   * Pauses the CPM
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#pause()
   */
  public void pause() {
    cpEngine.pauseIt();
    if (checkpoint != null) {
      checkpoint.doCheckpoint();
      checkpoint.pause();
    }
  }

  /*
   * Returns true if the CPM is in pause state
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#isPaused()
   */
  public boolean isPaused() {
    return cpEngine.isPaused();
  }

  /*
   * Resumes the CPM
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#resume(boolean)
   */
  public void resume(boolean aRetryFailed) {
    resume();
  }

  /*
   * Resumes the CPM
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#resume()
   */
  public void resume() {
    cpEngine.resumeIt();
    if (checkpoint != null) {
      checkpoint.resume();
    }
  }

  /**
   * Kills the CPM hard. CASes in transit are not processed.
   * 
   * 
   */
  public void kill() {
    if (UIMAFramework.getLogger().isLoggable(Level.WARNING)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.WARNING, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_killing_cpm__WARNING",
              new Object[] { Thread.currentThread().getName() });
    }
    killed = true;
    // Stop processing pipeline. The CPMWorker will finish processing of the current
    // entity through the processing pipeline and than will terminate processing.
    cpEngine.killIt();

    // If valid checkpoint reference and not already deleted do the next checkpoint and stop
    // the checkpoint thread. Checkpoint file may be deleted if the CPM successfully completes
    // its run. See BaseCPM#run().
    if (checkpoint != null && !completed) {
      checkpoint.doCheckpoint();
      checkpoint.stop();
    }

  }

  /*
   * Stops the CPM and all of its processing components. The CPM finishes processing of all CASes
   * that are in its queues.
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#stop()
   */
  public void stop() {
    if (UIMAFramework.getLogger().isLoggable(Level.WARNING)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.WARNING, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_stop_cpm__WARNING",
              new Object[] { Thread.currentThread().getName() });
    }
    killed = true;
    // Stop processing pipeline. The CPMWorker will finish processing of the current
    // entity through the processing pipeline and than will terminate processing.
    cpEngine.stopIt();

    // If valid checkpoint reference and not already deleted do the next checkpoint and stop
    // the checkpoint thread. Checkpoint file may be deleted if the CPM successfully completes
    // its run. See BaseCPM#run().
    if (checkpoint != null && !completed) {
      checkpoint.doCheckpoint();
      checkpoint.stop();
    }
  }

  /*
   * Stops/kills the CPM and all of its processing components.
   * 
   * @deprecated
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#stop()
   */
  public void asynchStop() {
    if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.WARNING, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_asynch_stop_cpm__WARNING",
              new Object[] { Thread.currentThread().getName() });
    }
    killed = true;
    // Stop processing pipeline. The CPMWorker will finish processing of the current
    // entity through the processing pipeline and than will terminate processing.
    cpEngine.asynchStop();

    // If valid checkpoint reference and not already deleted do the next checkpoint and stop
    // the checkpoint thread. Checkpoint file may be deleted if the CPM successfully completes
    // its run. See BaseCPM#run().
    if (checkpoint != null && !completed) {
      checkpoint.doCheckpoint();
      checkpoint.stop();
    }
  }

  /*
   * Returns a String describing a given CASProcessor state.
   * 
   * @param aStatus - status of the CasProcessor @return - String corresponding to a given state of
   * the CasProcessor
   */
  private String decodeStatus(int aStatus) {
    try {
      switch (aStatus) {
        case Constants.CAS_PROCESSOR_COMPLETED:
          return Constants.COMPLETED;
        case Constants.CAS_PROCESSOR_DISABLED:
          return Constants.DISABLED;
        case Constants.CAS_PROCESSOR_READY:
          return Constants.READY;
        case Constants.CAS_PROCESSOR_RUNNING:
          return Constants.RUNNING;
        case Constants.CAS_PROCESSOR_KILLED:
          return Constants.KILLED;
      }
    } catch (NumberFormatException e) {
      e.printStackTrace();
    }
    return Constants.UNKNOWN;
  }

  /*
   * Copies events of a given type found in the list to a provided ProcessTrace instance
   * 
   * @param - aEvType, event type to copy from the list @param - List, list of events @param
   * ProcessTrace, where to copy events of a given type
   * 
   */
  private void copyComponentEvents(String aEvType, List aList, ProcessTrace aPTr)
          throws IOException {
    for (int i = 0; i < aList.size(); i++) {
      ProcessTraceEvent prEvent = (ProcessTraceEvent) aList.get(i);
      if (aEvType != null && aEvType.equals(prEvent.getType())) {
        aPTr.addEvent(prEvent);
      }
    }

  }

  /**
   * Helper method to display stats and totals
   * 
   * @param aProcessTrace -
   *          trace containing stats
   * 
   * @param aNumDocsProcessed -
   *          number of entities processed so far
   */
  public void displayStats(ProcessTrace aProcessTrace, int aNumDocsProcessed) {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
              "Documents Processed: " + aNumDocsProcessed);
    }
    // count total time
    int totalTime = 0;
    Iterator it = aProcessTrace.getEvents().iterator();
    while (it.hasNext()) {
      ProcessTraceEvent event = (ProcessTraceEvent) it.next();
      // Dont add total time the CPM ran for. Just add all of the times of all components to
      // get the time.
      if ("CPM".equals(event.getComponentName())) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).log(
                  Level.FINEST,
                  "Current Component::" + event.getComponentName() + " Time::"
                          + event.getDuration());
        }
        continue;
      }
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
                "Current Component::" + event.getComponentName());
      }
      totalTime += event.getDuration();
    }
    float totalTimeSeconds = (float) totalTime / 1000;
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
              "Total Time: " + totalTimeSeconds + " seconds");
    }

    // create root tree node
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
              "100% (" + totalTime + "ms) - Collection Processing Engine");
    }
    // build tree
    it = aProcessTrace.getEvents().iterator();
    while (it.hasNext()) {
      ProcessTraceEvent event = (ProcessTraceEvent) it.next();
      buildEventTree(event, totalTime);
    }
  }

  /**
   * Helper method to help build the CPM report
   * 
   * @param aEvent
   * @param aTotalTime
   */
  public void buildEventTree(ProcessTraceEvent aEvent, int aTotalTime) {
    // Skip reporting the CPM time.This time has already been acquired by summing up
    // times from all individual components
    if ("CPM".equals(aEvent.getComponentName())) {
      return;
    }

    int duration = aEvent.getDuration();
    float pct = (float) ((duration * 100 * 10) / aTotalTime) / 10;

    String type = (String) mEventTypeMap.get(aEvent.getType());
    if (type == null) {
      type = aEvent.getType();
    }

    if (System.getProperty("DEBUG") != null)
      UIMAFramework.getLogger(this.getClass()).log(
              Level.FINEST,
              "" + pct + "% (" + duration + "ms) - " + aEvent.getComponentName() + " (" + type
                      + ")");
    Iterator it = aEvent.getSubEvents().iterator();
    while (it.hasNext()) {
      ProcessTraceEvent event = (ProcessTraceEvent) it.next();
      buildEventTree(event, aTotalTime);
    }
  }

  /**
   * Returns PerformanceReport for the CPM. This report contains a snapshot of the CPM state.
   * 
   */
  public ProcessTrace getPerformanceReport() {
    Map perfReport = cpEngine.getStats();
    Progress[] colReaderProgress = (Progress[]) perfReport.get("COLLECTION_READER_PROGRESS");

    ProcessTrace processTrace = new ProcessTrace_impl(cpEngine.getPerformanceTuningSettings());
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
              "-------------------------------------------");
    }
    if (useJediiReport) {
      try {
        synchronized (procTr) {
          List eventList = procTr.getEvents();

          for (int j = 0; eventList != null && j < eventList.size(); j++) {
            ProcessTraceEvent prEvent = (ProcessTraceEvent) eventList.get(j);
            processTrace.addEvent(prEvent);
          }
        }
        return processTrace;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (defaultProcessTrace) {
      createDefaultProcessTrace(getCasProcessors(), procTr, processTrace);

      return processTrace;
    }
    try {

      // To facilitate recovery from CPM untimely shutdown ( due to external STOP), it must
      // have access to last entity id. CollectionReader will use this marker to synch itself
      // up to the last known entity before the CPM shut itself down. More complicated
      // recovery mechanism may be supported as long as CAS METADATA contains information
      // about the last known point. For example, in case WF Large Store the cas must
      // hold the entire EDATA frame, containing restart information. Its up to the
      // CollectionReader to know what part of the CAS Metadata should be used for recovery.
      // The "last cas" CasMetaData is added after each successfull read from the CollectionReader
      // by the cpEngine in its run() processing loop.
      CasMetaData casMetaData = (CasMetaData) perfReport.get("CPM_LAST_CAS_METADATA");
      if (casMetaData != null) {
        NameValuePair[] nvp = casMetaData.getCasMetaData();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < nvp.length && nvp[i] != null; i++) {
          if (i != 0) {
            // Add separator between name-value pairs. StringTokenizer will parse this string
            // and will use the separator to extract nvp.
            sb.append(",");
          }
          sb.append(nvp[i].getName() + "=" + (String) nvp[i].getValue());
        }
        processTrace.addEvent("CPM", "CPM_LAST_CAS_METADATA", sb.toString(), 0, null);
      }
      List eList = null;
      synchronized (procTr) {
        eList = procTr.getEventsByComponentName("CPM", true);
      }
      if (!useJediiReport) {
        copyComponentEvents("CPM PROCESSING TIME", eList, processTrace);
      }
      eList.clear();
      if (colReaderProgress != null) {
        Long totalCollectionReaderTime = (Long) perfReport.get("COLLECTION_READER_TIME");
        String readerName = collectionReader.getProcessingResourceMetaData().getName();
        if (totalCollectionReaderTime != null) {
          processTrace.addEvent(readerName, "COLLECTION_READER_TIME", String
                  .valueOf(totalCollectionReaderTime), 0, null);
        }
        for (int i = 0; i < colReaderProgress.length; i++) {
          if (Progress.BYTES.equals(colReaderProgress[i].getUnit())) {
            processTrace.addEvent(readerName, Constants.COLLECTION_READER_BYTES_PROCESSED, String
                    .valueOf(colReaderProgress[i].getCompleted()), 0, null);
          } else if (Progress.ENTITIES.equals(colReaderProgress[i].getUnit())) {
            processTrace.addEvent(readerName, Constants.COLLECTION_READER_DOCS_PROCESSED, String
                    .valueOf(colReaderProgress[i].getCompleted()), 0, null);
          }
        }

        synchronized (procTr) {
          eList = procTr.getEventsByComponentName(readerName, true);
        }
        copyComponentEvents("COLLECTION READER PROCESSING TIME", eList, processTrace);
        eList.clear();
        processTrace.addEvent(readerName, "Last Entity ID Read", cpEngine.getLastProcessedDocId(),
                0, null);
      }

      LinkedList processors = cpEngine.getAllProcessingContainers();
      for (int i = 0; i < processors.size(); i++) {
        ProcessingContainer container = (ProcessingContainer) processors.get(i);
        synchronized (procTr) {
          eList = procTr.getEventsByComponentName(container.getName(), true);
        }
        copyComponentEvents("Process", eList, processTrace);

        processTrace.addEvent(container.getName(), "Documents Processed", String.valueOf(container
                .getProcessed()), 0, null);
        String status = decodeStatus(container.getStatus());
        processTrace.addEvent(container.getName(), "Processor Status", status, 0, null);

        long bytesIn = container.getBytesIn();
        processTrace.addEvent(container.getName(), "Processor BYTESIN", String.valueOf(bytesIn), 0,
                null);

        long bytesOut = container.getBytesOut();
        processTrace.addEvent(container.getName(), "Processor BYTESOUT", String.valueOf(bytesOut),
                0, null);

        int restartCount = container.getRestartCount();
        processTrace.addEvent(container.getName(), "Processor Restarts", String
                .valueOf(restartCount), 0, null);

        int retryCount = container.getRetryCount();
        processTrace.addEvent(container.getName(), "Processor Retries", String.valueOf(retryCount),
                0, null);

        int filteredCount = container.getFilteredCount();
        processTrace.addEvent(container.getName(), "Filtered Entities", String
                .valueOf(filteredCount), 0, null);

        long remainingCount = container.getRemaining();
        processTrace.addEvent(container.getName(), "Processor Remaining", String
                .valueOf(remainingCount), 0, null);

        HashMap aMap = container.getAllStats();

        if (aMap.keySet() != null) {
          if (System.getProperty("SHOW_CUSTOM_STATS") != null)
            UIMAFramework.getLogger(this.getClass()).log(Level.FINEST, "Adding Custom Stats");
          Iterator it = aMap.keySet().iterator();
          while (it != null && it.hasNext()) {

            String key = (String) it.next();
            if (key != null) {
              Object o = aMap.get(key);
              if (o instanceof String) {
                processTrace.addEvent(container.getName(), key, (String) o, 0, null);
                if (System.getProperty("SHOW_CUSTOM_STATS") != null)
                  UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
                          "Custom String Stat-" + key + " Value=" + (String) o);
              } else if (o instanceof Integer) {
                processTrace.addEvent(container.getName(), key, String.valueOf(((Integer) o)
                        .intValue()), 0, null);
                if (System.getProperty("SHOW_CUSTOM_STATS") != null)
                  UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,
                          "Custom Integer Stat-" + key + " Value=" + ((Integer) o).intValue());
              } else {
                if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
                  UIMAFramework.getLogger(this.getClass()).log(
                          Level.FINEST,
                          "Invalid Type Found When Generating Status For " + key + ". Type::"
                                  + o.getClass().getName()
                                  + " Not supported. Use Integer or String instead.");
                }
              }
            }
          }
        }
        try {
          String lastDocId = container.getLastProcessedEntityId();
          if (lastDocId != null) {
            processTrace.addEvent(container.getName(), "Processor Last EntityId", lastDocId, 0,
                    null);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return processTrace;
  }

  /**
   * @param processors
   * @param procTr
   * @param processTrace
   */
  private void createDefaultProcessTrace(CasProcessor[] aProcessors, ProcessTrace srcProcTr,
          ProcessTrace aProcessTrace) {
    for (int i = 0; aProcessors != null && i < aProcessors.length; i++) {
      String name = aProcessors[i].getProcessingResourceMetaData().getName();
      if (name == null) {
        name = aProcessors[i].getClass().getName();
      }
      synchronized (srcProcTr) {

        List eventList = srcProcTr.getEventsByComponentName(name, false);
        for (int j = 0; j < eventList.size(); j++) {
          ProcessTraceEvent prEvent = (ProcessTraceEvent) eventList.get(j);
          aProcessTrace.addEvent(prEvent);
        }
      }
    }
  }

  /**
   * Returns current CPE progress. How many entities processed and bytes processed.
   */
  public Progress[] getProgress() {
    return cpEngine.getProgress();
  }

  /**
   * Returns a CPE descriptor as a String
   * 
   * @param aList -
   *          list of components
   * 
   * @return - descriptor populated with a given components
   */
  public String getDescriptor(List aList) throws ResourceConfigurationException {
    return cpeFactory.getDescriptor(aList);
  }

  /**
   * Returns a {@link SynchPoint} object initialized by the Collection Reader if the Collection
   * Reader implements {@link RecoverableCollectionReader}. The synchpoint object contains the
   * current snapshot that includes the last document processed.
   * 
   * @return - instance of SynchPoint if the Collection Reader is recoverable, null otherwise
   */
  public SynchPoint getSynchPoint() {
    SynchPoint synchPoint = null;
    // Check if the CR is recoverable
    if (collectionReader != null && collectionReader instanceof RecoverableCollectionReader) {
      synchPoint = ((RecoverableCollectionReader) collectionReader).getSynchPoint();
    }
    return synchPoint;
  }
}
