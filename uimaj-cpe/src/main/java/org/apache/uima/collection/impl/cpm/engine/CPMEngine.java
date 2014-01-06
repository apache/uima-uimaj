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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.uima.UIMAFramework;
import org.apache.uima.adapter.vinci.util.Descriptor;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.impl.AnalysisEngineImplBase;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.collection.CasInitializer;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.base_cpm.AbortCPMException;
import org.apache.uima.collection.base_cpm.BaseCollectionReader;
import org.apache.uima.collection.base_cpm.BaseStatusCallbackListener;
import org.apache.uima.collection.base_cpm.CasDataCollectionReader;
import org.apache.uima.collection.base_cpm.CasDataConsumer;
import org.apache.uima.collection.base_cpm.CasDataInitializer;
import org.apache.uima.collection.base_cpm.CasObjectProcessor;
import org.apache.uima.collection.base_cpm.CasProcessor;
import org.apache.uima.collection.base_cpm.RecoverableCollectionReader;
import org.apache.uima.collection.base_cpm.SkipCasException;
import org.apache.uima.collection.impl.EntityProcessStatusImpl;
import org.apache.uima.collection.impl.base_cpm.container.CasProcessorConfiguration;
import org.apache.uima.collection.impl.base_cpm.container.ProcessingContainer;
import org.apache.uima.collection.impl.base_cpm.container.deployer.CasProcessorDeployer;
import org.apache.uima.collection.impl.base_cpm.container.deployer.CasProcessorDeploymentException;
import org.apache.uima.collection.impl.cpm.CheckpointData;
import org.apache.uima.collection.impl.cpm.Constants;
import org.apache.uima.collection.impl.cpm.container.CPEFactory;
import org.apache.uima.collection.impl.cpm.container.deployer.DeployFactory;
import org.apache.uima.collection.impl.cpm.container.deployer.socket.ProcessControllerAdapter;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.ChunkMetadata;
import org.apache.uima.collection.impl.cpm.utils.CpmLocalizedMessage;
import org.apache.uima.collection.impl.cpm.utils.TimerFactory;
import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.collection.metadata.CpeCasProcessors;
import org.apache.uima.collection.metadata.CpeConfiguration;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.internal.util.JavaTimer;
import org.apache.uima.resource.CasManager;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.OperationalProperties;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.Level;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.ProcessTraceEvent;
import org.apache.uima.util.Progress;
import org.apache.uima.util.UimaTimer;
import org.apache.uima.util.impl.ProcessTrace_impl;

/**
 * Responsible for creating and initializing processing threads. This instance manages the lifecycle
 * of the CPE components. It exposes API for plugging in components programmatically instead of
 * declaratively. Running in its own thread, this components creates seperate Processing Pipelines
 * for Analysis Engines and Cas Consumers, launches configured CollectionReader and attaches all of
 * those components to form a pipeline from source to sink. The Collection Reader feeds Processing
 * Threads containing Analysis Engines, and Analysis Engines feed results of analysis to Cas
 * Consumers.
 * 
 * 
 * 
 */
public class CPMEngine extends Thread {
  private static final int MAX_WAIT_ON_QUEUE = 400;

  private static final int CAS_PROCESSED_MSG = 1000;

  private static final String SINGLE_THREADED_MODE = "single-threaded";

  public CPECasPool casPool;

  // Used internally for synchronization
  public final Object lockForPause = new Object();  

  // CollectionReader to be used by this CPM
  private BaseCollectionReader collectionReader = null;

  // Flag indicating if the CPM should pause
  // Accesses to this flag (read and write) must
  //   be done while holding the "lockForPause" lock
  //   via synch
  //  @GuardedBy(lockForPause)
  protected boolean pause = false;

  // Flag indicating if this CPM is running or not
  // Marked volatile because it is set and read on different threads without synchronization 
  protected volatile boolean isRunning = false;

  // Flag indicating if this CPM has been stopped
  // Marked volatile because it is set and read on different threads without synchronization
  protected volatile boolean stopped = false;

  // Flag indicating if this CPM has been killed
  // Marked volatile because it is set and read on different threads without synchronization
  protected volatile boolean killed = false;

  // Flag indicating if this CPM should be paused on exception
  private boolean pauseOnException = false;

  // List of all annotators
  private LinkedList annotatorList = new LinkedList();

  private LinkedList annotatorDeployList = new LinkedList();

  // List of CasConsumers
  private LinkedList consumerList = new LinkedList();

  private LinkedList consumerDeployList = new LinkedList();

  // Number of entities this CPM must process.
  private long numToProcess = -1;

  private int poolSize = 0;

  // ProcessTrace aggregating CPMs performance stats
  private ProcessTrace procTr = null;

  // private EntityProcessStatusImpl enProcSt = null;
  // used to during recovery stage after CPM failure or forced shutdown
  // private ProcessTrace restoredProcTr = null;
  // Map for storing runtime statistics. used for reporting
  private Map stats = new HashMap();

  // List of all callback listeners
  private ArrayList statusCbL = new ArrayList();

  // Number of entities to fetch for every getNext()
  private int readerFetchSize = 1;

  // Size of the work queue. This queue is shared among processing units with deployed annotators.
  // The ArtifactProducer deposits entities into this queue, while ProcessingUnits dequeue them.
  private int inputQueueSize = 1;

  // Size of the output queue. This queue is shared with deployed casconsumers.
  private int outputQueueSize = 1;

  // Number of concurrent processing units (pipelines)
  private int concurrentThreadCount = 1;

  private Hashtable analysisEngines = new Hashtable();

  private Hashtable consumers = new Hashtable();

  private CasProcessor[] casprocessorList;

  // Component responsible for asynchronous read from the CollectionReader. It places Cas'es into
  // work Queue
  private ArtifactProducer producer = null;

  // Factory responsible for instantiating CPE components from CPE descriptor
  private CPEFactory cpeFactory = null;

  // An array holding instances of components responsible for analysis
  protected ProcessingUnit[] processingUnits = null;

  // Instantiate a Processing Unit containing CasConsumers. There may be many Analysis Processing
  // Units
  // but there is one CasConsumer Processing Unit ( at least for now).
  private ProcessingUnit casConsumerPU = null;

  // Queue where result of analysis goes to be consumed by Consumers
  protected BoundedWorkQueue outputQueue = null;

  // Queue were Cas'es meant for analysis are deposited by ArtifactProducer
  protected BoundedWorkQueue workQueue = null;

  private CheckpointData checkpointData = null;

  private boolean mixedCasProcessorTypeSupport = false;

  private Properties mPerformanceTuningSettings = UIMAFramework
          .getDefaultPerformanceTuningProperties();

  private DebugControlThread dbgCtrlThread = null;

  private ProcessControllerAdapter pca = null;

  private int activeProcessingUnits = 1;

  private boolean hardKill = false;

  private Hashtable skippedDocs = new Hashtable();

  private Capability[] definedCapabilities = null;

  private boolean needsTCas = true;

  private long crFetchTime = 0;

  private int readerState = 0;

  private boolean dropCasOnExceptionPolicy = false;

  private boolean singleThreadedCPE = false;

  private NonThreadedProcessingUnit nonThreadedProcessingUnit = null;

  private NonThreadedProcessingUnit nonThreadedCasConsumerProcessingUnit = null;

  private LinkedList initial_cp_list = new LinkedList(); // this list is used to hold Cas

  // Processors

  // It contains both AEs and CCs.
  private boolean casProcessorsDeployed = false;

  private boolean consumerThreadStarted = false;

  private boolean readerThreadStarted = false;

  private int[] processingThreadsState = null;

  /**
   * Initializes Collection Processing Engine. Assigns this thread and all processing threads
   * created by this component to a common Thread Group.
   * 
   * @param aThreadGroup -
   *          contains all CPM related threads
   * @param aCpeFactory -
   *          CPE factory object responsible for parsing cpe descriptor and creating components
   * @param aProcTr -
   *          instance of the ProcessTrace where the CPM accumulates stats
   * @param aCheckpointData -
   *          checkpoint object facillitating restart from the last known point
   */
  public CPMEngine(CPMThreadGroup aThreadGroup, CPEFactory aCpeFactory, ProcessTrace aProcTr,
          CheckpointData aCheckpointData) throws Exception {
    super(aThreadGroup, "CPMEngine Thread");
    cpeFactory = aCpeFactory;
    // Accumulate trace info in provided ProcessTrace instance
    procTr = aProcTr;
    // Determine in which mode to start the engine: single or multi-threaded
    if (cpeFactory.getCPEConfig() != null
            && cpeFactory.getCPEConfig().getDeployment().equalsIgnoreCase(SINGLE_THREADED_MODE)) {
      if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_single_threaded_mode__CONFIG",
                new Object[] { Thread.currentThread().getName() });
      }
      singleThreadedCPE = true;
    } else {
      if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_multi_threaded_mode__CONFIG",
                new Object[] { Thread.currentThread().getName() });
      }
    }
    checkpointData = aCheckpointData;
    // enProcSt = new EntityProcessStatusImpl(procTr);
    CPEFactory factory = this.cpeFactory;
    if (factory != null) {
      CpeDescription desc = factory.getCpeDescriptor();
      if (desc != null) {
        CpeCasProcessors proc = desc.getCpeCasProcessors();
        if (proc != null) {
          dropCasOnExceptionPolicy = proc.getDropCasOnException();
        }
      }
    }
  }

  /**
   * Returns a list of Processing Containers for Analysis Engines. Each CasProcessor is managed by
   * its own container.
   * 
   */
  public LinkedList getProcessingContainers() {
    return annotatorList;
  }

  /**
   * Returns a list of All Processing Containers. Each CasProcessor is managed by its own container.
   * 
   */
  public LinkedList getAllProcessingContainers() {
    LinkedList all = new LinkedList();
    all.addAll(annotatorList);
    all.addAll(consumerList);
    return all;
  }

  /**
   * Returns number of processing threads
   * 
   * @return - number of processing threads
   * @throws ResourceConfigurationException -
   */
  public int getThreadCount() throws ResourceConfigurationException {
    return cpeFactory.getProcessingUnitThreadCount();
  }

  /**
   * Plugs in a map where the engine stores perfomance info at runtime
   * 
   * @param aMap -
   *          map for runtime stats and totals
   */
  public void setStats(Map aMap) {
    stats = aMap;
  }

  /**
   * Returns CPE stats
   * 
   * @return Map containing CPE stats
   */
  public Map getStats() {
    return stats;
  }

  /**
   * Sets a global flag to indicate to the CPM that it should pause whenever exception occurs
   * 
   * @param aPause -
   *          true if pause is requested on exception, false otherwise
   */
  public void setPauseOnException(boolean aPause) {
    pauseOnException = aPause;
  }

  /**
   * Returns if the CPM should pause when exception occurs
   * 
   * @return - true if the CPM pauses when exception occurs, false otherwise
   */
  public boolean isPauseOnException() {
    return pauseOnException;
  }

  /**
   * Defines the size of inputQueue. The queue stores this many entities read from the
   * CollectionReader. Every processing pipeline thread will read its entities from this input
   * queue. The CollectionReader is decoupled from the consumer of entities, and continuously
   * replenishes the input queue.
   * 
   * @param aInputQueueSize
   *          the size of the batch.
   */
  public void setInputQueueSize(int aInputQueueSize) {
    inputQueueSize = aInputQueueSize;
  }

  /**
   * Defines the size of outputQueue. The queue stores this many entities enqueued by every
   * processing pipeline thread.The results of analysis are dumped into this queue for consumer
   * thread to consume its contents.
   * 
   * @param aOutputQueueSize
   *          the size of the batch.
   */
  public void setOutputQueueSize(int aOutputQueueSize) {
    outputQueueSize = aOutputQueueSize;
  }

  /**
   * Defines the size of Cas Pool.
   * 
   * @param aPoolSize
   *          the size of the Cas pool.
   */
  public void setPoolSize(int aPoolSize) {
    poolSize = aPoolSize;
  }

  public int getPoolSize() {
    return poolSize;
  }

  /**
   * Defines number of threads executing the processing pipeline concurrently.
   * 
   * @param aConcurrentThreadSize
   *          the size of the batch.
   */
  public void setConcurrentThreadSize(int aConcurrentThreadSize) {
    concurrentThreadCount = aConcurrentThreadSize;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.BaseCPM#addStatusCallbackListener(org.apache.uima.collection.base_cpm.BaseStatusCallbackListener)
   */
  public void addStatusCallbackListener(BaseStatusCallbackListener aListener) {
    if ( aListener != null ) {
      statusCbL.add(aListener);
    }
  }

  /**
   * Returns a list of ALL callback listeners currently registered with the CPM
   * 
   * @return -
   */
  public ArrayList getCallbackListeners() {
    return statusCbL;
  }

  /**
   * Unregisters given listener from the CPM
   * 
   * @param aListener -
   *          instance of {@link BaseStatusCallbackListener} to unregister
   */
  public void removeStatusCallbackListener(BaseStatusCallbackListener aListener) {
    statusCbL.remove(aListener);
  }

  /**
   * Returns true if this engine has been killed
   * 
   * @return true if this engine has been killed
   */
  public boolean isKilled() {
    return killed;
  }

  /**
   * Dumps some internal state of the CPE. Used for debugging.
   * 
   */
  private void dumpState() {
    try {
      if (cpeFactory.getCPEConfig() != null
              && cpeFactory.getCPEConfig().getDeployment().equalsIgnoreCase(SINGLE_THREADED_MODE)) {
        UIMAFramework.getLogger(this.getClass())
                .logrb(
                        Level.INFO,
                        this.getClass().getName(),
                        "process",
                        CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                        "UIMA_CPM_show_cr_state__INFO",
                        new Object[] { Thread.currentThread().getName(),
                            String.valueOf(this.readerState) });

        for (int i = 0; processingUnits != null && i < processingUnits.length; i++) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.INFO,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_show_pu_state__INFO",
                  new Object[] { Thread.currentThread().getName(), String.valueOf(i),
                      String.valueOf(processingUnits[i].threadState) });
        }
        if (casConsumerPU != null) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.INFO,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_show_cc_state__INFO",
                  new Object[] { Thread.currentThread().getName(),
                      String.valueOf(casConsumerPU.threadState) });
        }

      } else {
        if (producer != null) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.INFO,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_show_cr_state__INFO",
                  new Object[] { Thread.currentThread().getName(),
                      String.valueOf(producer.threadState) });
        }
        for (int i = 0; processingUnits != null && i < processingUnits.length; i++) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.INFO,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_show_pu_state__INFO",
                  new Object[] { Thread.currentThread().getName(), String.valueOf(i),
                      String.valueOf(processingUnits[i].threadState) });
        }
        if (casConsumerPU != null) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.INFO,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_show_cc_state__INFO",
                  new Object[] { Thread.currentThread().getName(),
                      String.valueOf(casConsumerPU.threadState) });
        }
      }
    } catch (Exception e) { // ignore. This is called on stop()
    }

  }

  /**
   * Kill CPM the hard way. None of the entities in the queues will be processed. This methof simply
   * empties all queues and at the end adds EOFToken to the work queue so that all threads go away.
   * 
   */
  public void killIt() {
    isRunning = false;
    killed = true;
    hardKill = true;

    dumpState();
    if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_killing_cpm__INFO",
              new Object[] { Thread.currentThread().getName() });
    }
    if (workQueue != null) {
      while (workQueue.getCurrentSize() > 0) {
        workQueue.dequeue();
      }
    }
    if (outputQueue != null) {
      while (outputQueue.getCurrentSize() > 0) {
        outputQueue.dequeue();
      }
    }
    if (casPool != null) {
      synchronized (casPool) {
        casPool.notifyAll();
      }
    }
    if (workQueue != null) {
      Object[] eofToken = new Object[1];
      // only need one member in the array
      eofToken[0] = new EOFToken();
      workQueue.enqueue(eofToken);
      UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_terminate_pipelines__INFO",
              new Object[] { Thread.currentThread().getName(), String.valueOf(killed) });
//      synchronized (workQueue) { // redundant - enqueue call above does this
//        workQueue.notifyAll();
//      }
    }

  }

  /**
   * Returns if the CPE was killed hard. Soft kill allows the CPE to finish processing all
   * in-transit CASes. Hard kill causes the CPM to stop processing and to throw away all unprocessed
   * CASes from its queues.
   * 
   * @return true if the CPE was killed hard
   */
  public boolean isHardKilled() {
    return hardKill;
  }

  /**
   * @deprecated
   * 
   */
  @Deprecated
public void asynchStop() {
    if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.INFO, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_terminate_pipelines__INFO",
              new Object[] { Thread.currentThread().getName(), String.valueOf(killed) });
    }
    new Thread() {
      public void run() {
        Object[] eofToken = new Object[1];
        eofToken[0] = new EOFToken();
        workQueue.enqueue(eofToken);

        stopped = true;
        killed = true;
        if (!isRunning) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_already_stopped__FINEST",
                    new Object[] { Thread.currentThread().getName() });
          }
          // Already stopped
          return;
        }
        try {
          // Change global status
          isRunning = false;
          // terminate this thread if the thread has been previously suspended
          synchronized (lockForPause) {
            if (pause) {
              pause = false;
              lockForPause.notifyAll();
            }
          }
          // Let processing threads finish their work by emptying all queues. Even during a hard
          // stop we should try to clean things up as best as we can. First empty process queue or
          // work
          // queue, dump result of analysis into output queue and let the consumers process that.
          // When all queues are empty we are done.
          int cc = workQueue.getCurrentSize();
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "process",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_consuming_queue__FINEST",
                    new Object[] { Thread.currentThread().getName(), workQueue.getName(),
                        String.valueOf(cc) });
          }
          while (workQueue.getCurrentSize() > 0) {
            sleep(MAX_WAIT_ON_QUEUE);
            if (System.getProperty("DEBUG") != null) {
              if (cc < workQueue.getCurrentSize()) {
                if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
                  UIMAFramework.getLogger(this.getClass()).logrb(
                          Level.FINEST,
                          this.getClass().getName(),
                          "process",
                          CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                          "UIMA_CPM_wait_consuming_queue__FINEST",
                          new Object[] { Thread.currentThread().getName(), workQueue.getName(),
                              String.valueOf(workQueue.getCurrentSize()) });
                }
                cc = workQueue.getCurrentSize();
              }
            }
          }
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "process",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_consuming_queue__FINEST",
                    new Object[] { Thread.currentThread().getName(), workQueue.getName(),
                        String.valueOf(outputQueue.getCurrentSize()) });
          }
          while (outputQueue.getCurrentSize() > 0) {
            sleep(MAX_WAIT_ON_QUEUE);
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(
                      Level.FINEST,
                      this.getClass().getName(),
                      "process",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_wait_consuming_queue__FINEST",
                      new Object[] { Thread.currentThread().getName(), workQueue.getName(),
                          String.valueOf(outputQueue.getCurrentSize()) });
            }
          }
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_done_consuming_queue__FINEST",
                    new Object[] { Thread.currentThread().getName() });
          }
          for (int i = 0; processingUnits != null && i < processingUnits.length; i++) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_stop_processors__FINEST",
                      new Object[] { Thread.currentThread().getName(), String.valueOf(i) });
            }
            processingUnits[i].stopCasProcessors(false);
          }
        } catch (Exception e) {

          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINER, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_exception__FINER",
                  new Object[] { Thread.currentThread().getName(), e.getMessage() });
        }

      }
    }.start();

  }

  /**
   * Stops execution of the Processing Pipeline and this thread.
   */
  public void stopIt() {
    UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
            "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_stop_cpm__FINEST",
            new Object[] { Thread.currentThread().getName(), String.valueOf(killed) });

    dumpState();

    stopped = true;
    killed = true;
    if (!isRunning) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_already_stopped__FINEST",
              new Object[] { Thread.currentThread().getName() });
      // Already stopped
      return;
    }
//    try {
      // Change global status
      isRunning = false;
      // terminate this thread if the thread has been previously suspended
      synchronized (lockForPause) {
        if (pause) {
          pause = false;
          lockForPause.notifyAll();
        }
      }
      // Let processing threads finish their work by emptying all queues. Even during a hard
      // stop we should try to clean things up as best as we can. First empty process queue or work
      // queue, dump result of analysis into output queue and let the consumers process that.
      // When all queues are empty we are done.
      
      // The logic below (now commented out) has a race condition - 
      //   The workQueue / outputQueue can become (temporarily) empty, but then 
      //      can be filled with the eof token
      //     But this code proceeds to stop all the CAS processors, 
      //      which results in a hang because the pool isn't empty and the process thread waits for 
      //      an available cas processor forever.
      
      //   Fix is to not kill the cas processors.  Just let them finish normally.  The artifact producer
      //     will stop sending new CASes and send through an eof token, which causes normal shutdown to
      //     occur for all the threads.

      /*
      if (workQueue != null) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_consuming_queue__FINEST",
                  new Object[] { Thread.currentThread().getName(), workQueue.getName(),
                      String.valueOf(workQueue.getCurrentSize()) });

        }
        int cc = workQueue.getCurrentSize();
        while (workQueue.getCurrentSize() > 0) {
          sleep(MAX_WAIT_ON_QUEUE);
          if (System.getProperty("DEBUG") != null) {
            if (cc < workQueue.getCurrentSize()) {
              if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
                UIMAFramework.getLogger(this.getClass()).logrb(
                        Level.FINEST,
                        this.getClass().getName(),
                        "process",
                        CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                        "UIMA_CPM_wait_consuming_queue__FINEST",
                        new Object[] { Thread.currentThread().getName(), workQueue.getName(),
                            String.valueOf(workQueue.getCurrentSize()) });

              }
              cc = workQueue.getCurrentSize();
            }
          }
        }
      }
      if (outputQueue != null) {
        while (outputQueue.getCurrentSize() > 0) {
          sleep(MAX_WAIT_ON_QUEUE);
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "process",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_wait_consuming_queue__FINEST",
                    new Object[] { Thread.currentThread().getName(), outputQueue.getName(),
                        String.valueOf(outputQueue.getCurrentSize()) });
          }
        }
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_done_consuming_queue__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
      }

      for (int i = 0; processingUnits != null && i < processingUnits.length
              && processingUnits[i] != null; i++) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_stop_processors__FINEST",
                  new Object[] { Thread.currentThread().getName(), String.valueOf(i) });
        }
        processingUnits[i].stopCasProcessors(false);
      }
    
    } catch (Exception e) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINER)) {
        e.printStackTrace();
      }
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINER, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_exception__FINER",
              new Object[] { Thread.currentThread().getName(), e.getMessage() });
    }
    */
  }

  /**
   * Returns index to a CasProcessor with a given name in a given List
   * 
   * 
   * @param aDeployList -
   *          List of CasConsumers to be searched
   * @param aName -
   *          name of the CasConsumer we want to find
   * 
   * @return 0 - if a CasConsumer is not found in a list, else returns a position in the list where
   *         the CasConsumer can found
   * 
   */
  private int getIndexInList(List aDeployList, String aName) {
    for (int i = 0; i < aDeployList.size(); i++) {

      List innerList = (ArrayList) aDeployList.get(i);
      String currentCPName = ((CasProcessor) innerList.get(0)).getProcessingResourceMetaData()
              .getName();
      if (aName != null && aName.trim().equals(currentCPName.trim())) {
        return i;
      }
    }
    return 0;
  }

  /**
   * Find the position in the list of the Cas Processor with a given name
   * 
   * @param aName
   * @param aList
   * @return the position in the list of the Cas Processor with a given name
   */
  private int getPositionInListIfExists(String aName, List aList) {
    for (int i = 0; i < aList.size(); i++) {

      List innerList = (ArrayList) aList.get(i);
      // Get the name of the first CP in the list. The inner list contains CPs of the same kind
      String currentCPName = ((CasProcessor) innerList.get(0)).getProcessingResourceMetaData()
              .getName();
      if (aName != null && aName.trim().equals(currentCPName.trim())) {
        return i;
      }
    }
    return -1;

  }

  /**
   * Parses Cas Processor descriptor and checks if it is parallelizable.
   * 
   * @param aDescPath -
   *          fully qualified path to a CP descriptor
   * @param aCpName -
   *          name of the CP
   * @param isConsumer -
   *          true if the CP is a Cas Consumer, false otherwise
   * @return - true if CP is parallelizable, false otherwise
   * 
   * @throws Exception -
   */
  private boolean isMultipleDeploymentAllowed(String aDescPath, String aCpName, boolean isConsumer)
          throws Exception {
    OperationalProperties op = null;
    // Parse the descriptor to access Operational Properties
    ResourceSpecifier resourceSpecifier = cpeFactory.getSpecifier(new File(aDescPath).toURL());
    if (resourceSpecifier != null && resourceSpecifier instanceof ResourceCreationSpecifier) {
      ResourceMetaData md = ((ResourceCreationSpecifier) resourceSpecifier).getMetaData();
      if (md instanceof ProcessingResourceMetaData) {
        op = ((ProcessingResourceMetaData) md).getOperationalProperties();
        if (op == null) {
          // Operational Properties not defined, so use defaults
          if (isConsumer) {
            return false; // the default for CasConsumer
          }
          return true; // default for AEs
        }
        return op.isMultipleDeploymentAllowed();
      }
    }
    throw new ResourceConfigurationException(ResourceInitializationException.NOT_A_CAS_PROCESSOR,
            new Object[] { aCpName, "<unknown>", aDescPath });

  }

  /**
   * Determines if a given Cas Processor is parallelizable. Remote Cas Processors are by default
   * parallelizable. For integrated and managed the CPM consults Cas Processor's descriptor to
   * determine if it is parallelizable.
   * 
   * @param aProcessor -
   *          Cas Processor being checked
   * @param aCpName -
   *          name of the CP
   * @return - true if CP is parallelizable, false otherwise
   * 
   * @throws Exception -
   */
  public boolean isParallizable(CasProcessor aProcessor, String aCpName) throws Exception {
    boolean isConsumer = false;
    if (aProcessor instanceof CasConsumer || aProcessor instanceof CasDataConsumer) {
      isConsumer = true;
    }

    // casProcessingConfigMap may not contain configuration for this Cas Processor if this CP has
    // been
    // added dynamically via API. In this case, just go to metadata and determine via its
    // OperationalProperties if this is parallelizable component.
    if (!cpeFactory.casProcessorConfigMap.containsKey(aCpName)) {
      OperationalProperties op = aProcessor.getProcessingResourceMetaData()
              .getOperationalProperties();
      if (op != null) {
        return op.isMultipleDeploymentAllowed();
      }

      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_invalid_processor_configuration__SEVERE",
                new Object[] { Thread.currentThread().getName(), aCpName });

      }
      if (isConsumer) {
        return false; // by default the CasConsumer is not parallizable
      }
      return true; // by dafault AEs are parallizable
    }
    // Retrieve Cas Processor's CPE descriptor configuration.
    CpeCasProcessor casProcessorCPEConfig = (CpeCasProcessor) cpeFactory.casProcessorConfigMap
            .get(aCpName);

    if (Constants.DEPLOYMENT_LOCAL.equalsIgnoreCase(casProcessorCPEConfig.getDeployment())) {
      // Extract the client service descriptor.
      URL descriptorUrl = cpeFactory.getDescriptorURL(casProcessorCPEConfig);
      Descriptor descriptor = new Descriptor(descriptorUrl.toString());
      // From the client service descriptor extract the actual Cas Processor descriptor
      String aResourceSpecifierPath = descriptor.getResourceSpecifierPath();
      // Determine if this Cas Processor is parallelizable
      boolean is = isMultipleDeploymentAllowed(aResourceSpecifierPath, casProcessorCPEConfig
              .getName(), isConsumer);
      return is;
    } else if (Constants.DEPLOYMENT_INTEGRATED.equalsIgnoreCase(casProcessorCPEConfig
            .getDeployment())) {
      // If OperationalProperties are not defined use defaults based on CasProcessor type
      if (aProcessor.getProcessingResourceMetaData().getOperationalProperties() == null) {
        if (isConsumer) {
          return false; // default for CasConsumer
        }
        return true; // default for AEs
      }

      return aProcessor.getProcessingResourceMetaData().getOperationalProperties()
              .isMultipleDeploymentAllowed();
    }
    // Default is parallelizable
    return true;
  }

  /**
   * Adds Cas Processor to a single-threaded pipeline. This pipeline is fed by the output queue and
   * typicall contains Cas Consumers. AEs can alos be part of this pipeline.
   * 
   * @param aProcessor -
   *          Cas Processor to add to single-threaded pipeline
   * @param aCpName -
   *          name of the Cas Processor
   * 
   * @throws Exception -
   */
  private void addCasConsumer(CasProcessor aProcessor, String aCpName) throws Exception {
    if (consumers.containsKey(aCpName)) {
      if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_add_consumer_to_list__CONFIG",
                new Object[] { Thread.currentThread().getName(), aCpName });
      }
      int listIndex = getIndexInList(consumerDeployList, aCpName);
      ((List) consumerDeployList.get(listIndex)).add(aProcessor);
    } else {
      ArrayList newList = new ArrayList();
      newList.add(aProcessor);
      consumers.put(aCpName, newList);

      consumerDeployList.add(newList);

      if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_add_consumer_to_new_list__CONFIG",
                new Object[] { Thread.currentThread().getName(), aCpName });
      }
      if (cpeFactory.isDefault()) {
        cpeFactory.addCasProcessor(aProcessor);
      }
    }
  }

  /**
   * Add Cas Processor to a list of CPs that are to be run in the parallelizable pipeline. The fact
   * that the CP is in parallelizable pipeline does not mean that there will be instance per
   * pipeline of CP. Its allowed to have a single instance, shareable CP running in multi-threaded
   * pipeline.
   * 
   * @param aProcessor -
   *          CP to add to parallelizable pipeline
   * @param aCpName -
   *          name of the CP
   * 
   * @throws Exception -
   */
  private void addParallizableCasProcessor(CasProcessor aProcessor, String aCpName)
          throws Exception {
    UIMAFramework.getLogger(this.getClass()).log(Level.CONFIG, " Adding New Annotator:" + aCpName);
    if (analysisEngines.containsKey(aCpName)) {
      if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_add_pcp_to_existing_list__CONFIG",
                new Object[] { Thread.currentThread().getName(), aCpName });
      }
      int listIndex = getIndexInList(annotatorDeployList, aCpName);
      ((List) annotatorDeployList.get(listIndex)).add(aProcessor);
    } else {
      ArrayList newList = new ArrayList();
      newList.add(0, aProcessor);
      analysisEngines.put(aCpName, newList);
      annotatorDeployList.add(0, newList);
      if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {

        UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_add_pcp_to_new_list__CONFIG",
                new Object[] { Thread.currentThread().getName(), aCpName });
      }
      if (cpeFactory.isDefault()) {
        cpeFactory.addCasProcessor(aProcessor);
      }
    }

  }

  /**
   * Classify based on Cas Processor capability to run in parallel. Some Cas Processors need to run
   * as single instance only. It scans the list of Cas Processors backwords and moves those Cas
   * Processors that are not parallelizable to a separate single-threade pipeline. This process of
   * moving CPs continues until the first parallelizable Cas Processor is found. Beyond this all Cas
   * Processors are moved to a parallelizable pipeline. If the non-parallelizable CP is in the
   * parallelizable pipeline there simply will be a single instance of it that will be shared by all
   * processing threads.
   * 
   * @throws Exception -
   */
  private void classifyCasProcessors() throws Exception {
    boolean allowReorder = true;
    // Walk the list of Cas Processor backwards. The list of Cas Processors is actually a list of
    // lists.
    // Each sub-list contains instances of the same type of Cas Processor.
    for (int i = initial_cp_list.size(); i > 0; i--) {
      // Get the sub-list containing instances of Cas Processor
      ArrayList cp_instance_list = (ArrayList) initial_cp_list.get(i - 1);

      String previous = ""; // hold the previous name of the Cas Processor

      // Check the list of CP instances to check if its parallelizable
      for (int j = 0; j < cp_instance_list.size(); j++) {
        CasProcessor cp = (CasProcessor) cp_instance_list.get(j);
        String name = cp.getProcessingResourceMetaData().getName();
        // Check if the CP is parallelizable
        boolean parallizable = isParallizable(cp, name);

        // If Cas Processor is not parallizable and we have not yet hit a parallizable component
        // place the Cas Processor in a pipeline that supports single instance components
        if (!parallizable && allowReorder) {
          // There should only be one instance. The current implementation supports placing
          // non-parallizable Analysis Engines in the Cas Consumer Pipeline.
          if (!previous.equals(name)) {
            addCasConsumer(cp, name);
          }
        } else {
          // Hit the parallizable Cas Processor. From this point of all Cas Processors will be added
          // to the main processing pipeline ( as opposed to Cas Consumer Pipeline)
          allowReorder = false;
          // If the Cas Processor is non-parallizable ad just one instance of it to the Pipeline.
          if (parallizable || !previous.equals(name)) {
            addParallizableCasProcessor(cp, name);
          }
        }
        if (!parallizable) {
          cp_instance_list.remove(0);
        }
        previous = name;
      }
    }
  }

  /**
   * 
   * Adds a CASProcessor to the processing pipeline. If a CasProcessor already exists and its
   * status=DISABLED this method will re-enable the CasProcesser.
   * 
   * @param aCasProcessor
   *          CASProcessor to be added to the processing pipeline
   */
  public void addCasProcessor(CasProcessor aCasProcessor) throws ResourceConfigurationException {

    String name = aCasProcessor.getProcessingResourceMetaData().getName();

    // Set a global flag to indicate the we should support mixed CasProcessor types.
    // When this supported is enabled TCAS array will be instantiated to facilitate
    // conversions between CasData and TCAS.
    if (aCasProcessor instanceof CasObjectProcessor || aCasProcessor instanceof CasConsumer) {
      mixedCasProcessorTypeSupport = true;
    }
    ArrayList newList = null;
    int indexPos = getPositionInListIfExists(name, initial_cp_list);
    if (indexPos == -1) {
      newList = new ArrayList();
      newList.add(aCasProcessor);
      // New Cas Processor. Add it to a list
      initial_cp_list.add(newList);
    } else {
      newList = (ArrayList) initial_cp_list.get(indexPos);
      newList.add(aCasProcessor);
    }

  }

  /**
   * 
   * Adds a CASProcessor to the processing pipeline at a given place in the processing pipeline
   * 
   * @param aCasProcessor
   *          CASProcessor to be added to the processing pipeline
   * @param aIndex -
   *          insertion point for a given CasProcessor
   */
  public void addCasProcessor(CasProcessor aCasProcessor, int aIndex)
          throws ResourceConfigurationException {
    addCasProcessor(aCasProcessor);
  }

  /**
   * 
   * Removes a CASProcessor from the processing pipeline
   * 
   * @param aCasProcessorIndex -
   *          CasProcessor position in processing pipeline
   */
  public void removeCasProcessor(int aCasProcessorIndex) {
    if (aCasProcessorIndex < 0 || aCasProcessorIndex >= annotatorList.size()) {
      return;
    }
    annotatorList.remove(aCasProcessorIndex);
  }

  /**
   * 
   * Disable a CASProcessor in the processing pipeline
   * 
   * @param aCasProcessorIndex
   *          CASProcessor to be added to the processing pipeline
   */
  public void disableCasProcessor(int aCasProcessorIndex) {
    if (aCasProcessorIndex < 0 || aCasProcessorIndex > annotatorList.size()) {
      return;
    }
    ProcessingContainer pc = ((ProcessingContainer) annotatorList.get(aCasProcessorIndex));
    if (pc != null) {
      pc.setStatus(Constants.CAS_PROCESSOR_DISABLED);
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_diabled_cp__FINEST",
                new Object[] { Thread.currentThread().getName(), pc.getName() });
      }
    }
  }

  /**
   * 
   * Disable a CASProcessor in the processing pipeline
   * 
   * @param aCasProcessorName
   *          CASProcessor to be added to the processing pipeline
   */
  public void disableCasProcessor(String aCasProcessorName) {
    for (int i = 0; i < annotatorList.size(); i++) {
      ProcessingContainer pc = ((ProcessingContainer) annotatorList.get(i));
      if (pc.getName().equals(aCasProcessorName)) {
        pc.setStatus(Constants.CAS_PROCESSOR_DISABLED);
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_diabled_cp__FINEST",
                  new Object[] { Thread.currentThread().getName(), pc.getName() });
        }
      }
    }
  }

  /**
   * 
   * Disable a CASProcessor in the processing pipeline
   * 
   * @param aCasProcessorName
   *          CASProcessor to be added to the processing pipeline
   */
  public void enableCasProcessor(String aCasProcessorName) {
    for (int i = 0; i < annotatorList.size(); i++) {
      ProcessingContainer pc = ((ProcessingContainer) annotatorList.get(i));
      if (pc.getName().equals(aCasProcessorName)) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_enabled_cp__FINEST",
                  new Object[] { Thread.currentThread().getName(), aCasProcessorName });
        }
        pc.setStatus(Constants.CAS_PROCESSOR_RUNNING);
      }
    }
  }

  /**
   * Returns all CASProcesors in the processing pipeline
   */
  public CasProcessor[] getCasProcessors() {
    if (casprocessorList != null) {
      return casprocessorList;
    }
    // If CasProcessors have not yet been classified into AEs and CCs use the
    // initial_cp_list. This list is populated early, right after the CPE
    // descriptor is parsed. It is a list of lists, containing as many
    // instances of each Cas Processor as defined in the CPE descriptor.
    // The number of instances is determined based on number of processing
    // threads and CP property setting that determines if the CP is able
    // to run in parallel.
    if (casProcessorsDeployed == false) {
      CasProcessor[] casprocessorList = new CasProcessor[initial_cp_list.size()];
      ArrayList list;
      for (int i = 0; i < initial_cp_list.size(); i++) {
        list = (ArrayList) initial_cp_list.get(i);
        for (int j = 0; j < list.size(); j++) {
          casprocessorList[i] = (CasProcessor) list.get(j);
        }
      }
      return casprocessorList;
    }
    // CasProcessors have been classified into AEs and CCs, so merge the two lists
    ArrayList aList = new ArrayList();
    Iterator keyIt = analysisEngines.keySet().iterator();
    while (keyIt.hasNext()) {
      String keyName = (String) keyIt.next();
      List kList = (List) analysisEngines.get(keyName);
      if (kList != null) {
        for (int i = 0; i < kList.size(); i++) {
          aList.add(kList.get(i));
        }
      }
    }
    keyIt = consumers.keySet().iterator();
    while (keyIt.hasNext()) {
      String keyName = (String) keyIt.next();
      List kList = (List) consumers.get(keyName);
      if (kList != null) {
        for (int i = 0; i < kList.size(); i++) {
          aList.add(kList.get(i));
        }
      }
    }

    if (aList.size() == 0)
      return null;
    casprocessorList = new CasProcessor[aList.size()];
    for (int j = 0; j < aList.size(); j++) {
      casprocessorList[j] = (CasProcessor) aList.get(j);
    }
    return casprocessorList;
  }

  /**
   * Deploys all Cas Consumers
   * 
   * @throws AbortCPMException -
   */
  private void deployConsumers() throws AbortCPMException {

    if (consumerDeployList == null || consumerDeployList.size() == 0) {
      return;
    }
    CasProcessorDeployer deployer = null;
    // Deploy each CASProcessor in a seperate container
    for (int i = consumerDeployList.size(); i > 0; i--) {
      try {
        // Deployer deploys as many instances of CASProcessors as there are threads
        List cpList = (ArrayList) consumerDeployList.get((i - 1)); // list is zero-based
        String name = ((CasProcessor) cpList.get(0)).getProcessingResourceMetaData().getName();
        if (cpList.size() > 0) {
          //
          // Get a deployer for this type of CasProcessor. The type of deployer is determined from
          // the
          // CPE Configuration. Specifically from the deployment model for this CasProcessor.
          //	 
          CpeCasProcessor casProcessorType = (CpeCasProcessor) cpeFactory.casProcessorConfigMap
                  .get(name);
          deployer = DeployFactory.getDeployer(cpeFactory, casProcessorType, pca);
          // Deploy CasConsumer.
          ProcessingContainer container = deployer.deployCasProcessor(cpList, false);
          consumerList.add(container);
        }
      } catch (Exception e) {
        e.printStackTrace();
        throw new AbortCPMException(e.getMessage());
      }
    }
  }

  /**
   * Deploys CasProcessor and associates it with a {@link ProcessingContainer}
   * 
   * @param aProcessingContainer
   */
  public void redeployAnalysisEngine(ProcessingContainer aProcessingContainer) throws Exception {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_deploying_cp__FINEST",
              new Object[] { Thread.currentThread().getName(), aProcessingContainer.getName() });
    }
    CasProcessorDeployer deployer = aProcessingContainer.getDeployer();
    deployer.deployCasProcessor(aProcessingContainer);
  }

  /**
   * Deploys All Analysis Engines. Analysis Engines run in a replicated processing units seperate
   * from Cas Consumers.
   * 
   * @throws AbortCPMException -
   */
  private void deployAnalysisEngines() throws AbortCPMException {
    // When restoring the CPM from a checkpoint, its processing pipeline must be restored
    // to a previous state. So all CasProcessors that were disabled during the previous run
    // will remain disabled. All stats will be recovered as well.
    // if (restoredProcTr != null)
    if (checkpointData != null) {
      // Restore CPM related stats from the checkppoint
      restoreFromCheckpoint("CPM", "CPM PROCESSING TIME");
    }

    CasProcessorDeployer deployer = null;
    // Deploy each CASProcessor in a seperate container
    for (int i = 0; i < annotatorDeployList.size(); i++) {
      try {
        // Deployer deploys as many instances of CASProcessors as there are threads
        List cpList = (ArrayList) annotatorDeployList.get(i);
        String name = ((CasProcessor) cpList.get(0)).getProcessingResourceMetaData().getName();
        if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
                  "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_deploying_new_cp__CONFIG",
                  new Object[] { Thread.currentThread().getName(), name });
        }
        if (cpList.size() > 0) {

          //
          // Get a deployer for this type of CasProcessor. The type of deployer is determined from
          // the
          // CPE Configuration. Specifically from the deployment model for this CasProcessor. The
          // first
          //	 
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_lookup_cp__FINEST",
                    new Object[] { Thread.currentThread().getName(), name });
          }
          if (!cpeFactory.casProcessorConfigMap.containsKey(name)) {
            if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE,
                      this.getClass().getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_invalid_processor_configuration__SEVERE",
                      new Object[] { Thread.currentThread().getName(), name });
            }
            throw new Exception(CpmLocalizedMessage.getLocalizedMessage(
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_missing_cp__WARNING",
                    new Object[] { Thread.currentThread().getName(), name }));
          }
          CpeCasProcessor casProcessorCPEConfig = (CpeCasProcessor) cpeFactory.casProcessorConfigMap
                  .get(name);
          if (casProcessorCPEConfig == null) {
            if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {

              UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE,
                      this.getClass().getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_cp_configuration_not_defined__SEVERE",
                      new Object[] { Thread.currentThread().getName(), name });
            }
            throw new Exception(CpmLocalizedMessage.getLocalizedMessage(
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_missing_cp__WARNING",
                    new Object[] { Thread.currentThread().getName(), name }));
          } else if (casProcessorCPEConfig.getDeployment() == null
                  || casProcessorCPEConfig.getDeployment().trim().length() == 0) {
            if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE,
                      this.getClass().getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_cp_deployment_mode_not_defined__SEVERE",
                      new Object[] { Thread.currentThread().getName(), name });
            }
            throw new Exception(CpmLocalizedMessage.getLocalizedMessage(
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_Exception_invalid_deployment__WARNING", new Object[] {
                        Thread.currentThread().getName(), name,
                        casProcessorCPEConfig.getDeployment() }));
          }

          deployer = DeployFactory.getDeployer(cpeFactory, casProcessorCPEConfig, pca);
          // Deploy CasConsumer.
          ProcessingContainer container = deployer.deployCasProcessor(cpList, false);
          annotatorList.add(container);
        }
      } catch (Exception e) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_cp_failed_to_start__SEVERE",
                new Object[] { Thread.currentThread().getName(), e.getMessage() });

        throw new AbortCPMException(e.getMessage());
      }
    }

  }

  /**
   * Starts CASProcessor containers one a time. During this phase the container deploys a TAE as
   * local,remote, or integrated CasProcessor.
   * 
   */
  public void deployCasProcessors() throws AbortCPMException {
    try {
      classifyCasProcessors();
    } catch (Exception e) {
      e.printStackTrace();
      throw new AbortCPMException(e.getMessage());
    }
    if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {
      UIMAFramework.getLogger(this.getClass()).log(Level.CONFIG, "Deploying Analysis Engines");
    }
    deployAnalysisEngines();
    if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {
      UIMAFramework.getLogger(this.getClass()).log(Level.CONFIG, "Deploying CasConsumers");
    }
    deployConsumers();
    casProcessorsDeployed = true;
  }

  /**
   * Restores named events from the checkpoint
   * 
   * @param component -
   *          component name to restore named event for
   * @param aEvType -
   *          event to restore
   */
  private void restoreFromCheckpoint(String component, String aEvType) {
    if (checkpointData == null) {
      return; // nothing to restore
    }
    ProcessTrace restoredProcTr = checkpointData.getProcessTrace();
    try {
      // Retrieve all events associated with a named component
      List eList = restoredProcTr.getEventsByComponentName(component, true);
      if (!eList.isEmpty()) {
        // Copy named events found in checkpoint to the current procTr
        copyComponentEvents(aEvType, eList, procTr);
        eList.clear();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Copy given component events
   * 
   * @param aEvType -
   *          event type
   * @param aList -
   *          list of events to copy
   * @param aPTr -
   * @throws IOException -
   */
  private void copyComponentEvents(String aEvType, List aList, ProcessTrace aPTr)
          throws IOException {
    String typeS;

    for (int i = 0; i < aList.size(); i++) {
      ProcessTraceEvent prEvent = (ProcessTraceEvent) aList.get(i);
      typeS = prEvent.getType();
      if (aEvType != null && aEvType.equals(typeS)) {
        aPTr.addEvent(prEvent);
      }
    }
  }

  /**
   * Returns a global flag indicating if this Thread is in processing state
   * 
   */
  public boolean isRunning() {
    return isRunning;
  }

  /**
   * Returns a global flag indicating if this Thread is in pause state
   */
  public boolean isPaused() {
    synchronized (lockForPause) {
      return (pause == true);
    }
  }

  /**
   * Pauses this thread
   */
  public void pauseIt() {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_pause_cpe__FINEST",
              new Object[] { Thread.currentThread().getName() });
    }
    synchronized (lockForPause) {
      pause = true;
    }
  }

  /**
   * Resumes this thread
   */
  public void resumeIt() {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_resume_cpe__FINEST",
              new Object[] { Thread.currentThread().getName() });
    }
    synchronized (lockForPause) {
      pause = false;
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_notify_engine__FINEST",
                new Object[] { Thread.currentThread().getName() });
      }
      lockForPause.notifyAll();
    }

  }

  /**
   * Sets CollectionReader to use during processing
   * 
   * @param aCollectionReader
   *          aCollectionReader
   */
  public void setCollectionReader(BaseCollectionReader aCollectionReader) {
    collectionReader = aCollectionReader;
    if ( collectionReader != null ) {
      if (collectionReader.getProcessingResourceMetaData().getConfigurationParameterSettings()
              .getParameterValue("fetchSize") != null) {
        try {
          readerFetchSize = ((Integer) collectionReader.getProcessingResourceMetaData()
                  .getConfigurationParameterSettings().getParameterValue("fetchSize")).intValue();
        } catch (NumberFormatException nfe) {
          readerFetchSize = 1; // restore default
        }
      }
      if (checkpointData != null && checkpointData.getSynchPoint() != null ) {
        try {
          if (collectionReader instanceof RecoverableCollectionReader) {
            // Let the CollectionReader do the synchronization to the last known (good) read point
            ((RecoverableCollectionReader) collectionReader).moveTo(checkpointData.getSynchPoint());
            String readerName = collectionReader.getProcessingResourceMetaData().getName();
            if (readerName != null) {
              restoreFromCheckpoint(readerName, "COLLECTION READER PROCESSING TIME");
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Defines the size of the batch
   */

  public void setNumToProcess(long aNumToProcess) {
    numToProcess = aNumToProcess;
  }

  /**
   * Returns Id of the last document processed
   */
  public String getLastProcessedDocId() {
    return producer.getLastDocId();
  }

  public String getLastDocRepository() {
    return "";
  }

  /**
   * Instantiate custom Processing Pipeline
   * 
   * @param aClassName -
   *          name of a class that extends ProcessingUnit
   * 
   * @return - an instance of the ProcessingUnit
   * 
   * @throws Exception -
   */
  private ProcessingUnit producePU(String aClassName) throws Exception {
    Class currentClass = Class.forName(aClassName);
    // check to see if this is a subclass of aResourceClass
    ProcessingUnit pu = (ProcessingUnit) currentClass.newInstance();
    return pu;
  }

  private void startDebugControlThread() {
    String dbgCtrlFile = System.getProperty("DEBUG_CONTROL");
    dbgCtrlThread = new DebugControlThread(this, dbgCtrlFile, 1000);
    dbgCtrlThread.start();
  }

  /**
   * Instantiate custom Output Queue
   * 
   * @param aQueueSize -
   *          max size of the queue
   * @return - new instance of the output queue
   * 
   * @throws Exception -
   */
  private BoundedWorkQueue createOutputQueue(int aQueueSize) throws Exception {
    // Get the class that implements the queue
    if (cpeFactory.getCPEConfig().getOutputQueue() != null
            && cpeFactory.getCPEConfig().getOutputQueue().getClass() != null) {
      String outputQueueClass = cpeFactory.getCPEConfig().getOutputQueue().getQueueClass();
      if (outputQueueClass != null) {
        Class[] args = new Class[] { int.class, String.class, CPMEngine.class };
        Class cpClass = Class.forName(outputQueueClass);
        Constructor constructor = cpClass.getConstructor(args);
        Object[] oArgs = new Object[] { Integer.valueOf(aQueueSize), "Sequenced Output Queue", this };
        outputQueue = (BoundedWorkQueue) constructor.newInstance(oArgs);
      }
    } else {
      // default queue
      outputQueue = new BoundedWorkQueue(aQueueSize, "Output Queue", this);
    }
    return outputQueue;
  }

  /**
   * Notify listeners of a given exception
   * 
   * @param e -
   *          en exception to be sent to listeners
   */
  private void notifyListenersWithException(Exception e) {
    UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, e.getMessage(), e);

    ArrayList statusCbL = this.getCallbackListeners();
    EntityProcessStatusImpl enProcSt = new EntityProcessStatusImpl(procTr, true);
    // e is the actual exception.
    enProcSt.addEventStatus("CPM", "Failed", e);

    // Notify all listeners that the CPM has finished processing
    for (int j = 0; statusCbL != null && j < statusCbL.size(); j++) {
      BaseStatusCallbackListener st = (BaseStatusCallbackListener) statusCbL.get(j);
      if (st != null && st instanceof StatusCallbackListener) {
        ((StatusCallbackListener) st).entityProcessComplete(null, enProcSt);
      }
    }

  }

  /**
   * Callback method used to notify the engine when a processing pipeline is killed due to excessive
   * errors. This method is only called if the processing pipeline is unable to acquire a connection
   * to remote service and when configuration indicates 'kill-pipeline' as the action to take on
   * excessive errors. When running with multiple pipelines, routine decrements a global pipeline
   * counter and tests if there are no more left. When all pipelines are killed as described above,
   * the CPM needs to terminate. Since pipelines are prematurely killed, there are artifacts (CASes)
   * in the work queue. These must be removed from the work queue and disposed of (released) back to
   * the CAS pool so that the Collection Reader thread properly exits.
   * 
   * @param aPipelineThreadName -
   *          name of the pipeline thread exiting from its run() method
   */
  public synchronized void pipelineKilled(String aPipelineThreadName) {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_pipeline_terminated__FINEST",
              new Object[] { Thread.currentThread().getName(), aPipelineThreadName });
    }
    // Adjust the global counter
    activeProcessingUnits--;

    // Test if there are any processing pipelines left
    if (activeProcessingUnits <= 0) {
      // Change the global status of the CPM
      isRunning = false;
      // Check the work queue for any artifacts still unprocessed. This is a likely case, since the
      // Collection Reader is asynchronous. If there are artifacts still in the queue, this code
      // code needs to removed them and released them back to CAS pool. This needs to be done to
      // unblock those threads that are waiting for avaialble CAS instance. Most notably, Collection
      // Reader Thread.
      while (workQueue != null && workQueue.getCurrentSize() > 0) {
        // empty work queue
        try {
          Object anObject = workQueue.dequeue(1);
          if (anObject != null && anObject instanceof CAS[]) {
            // Notify listeners of the fact that the CPM is disposing the CAS
            notifyListeners(0, (CAS[]) anObject, procTr, new Exception(
                    "CPM Releases CAS before processing it due to premature CPM shutdown."));
            releaseCASes((CAS[]) anObject);
          }
        } catch (Exception e) {
          if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_exception_while_consuming_cases__SEVERE",
                    new Object[] { Thread.currentThread().getName(), e.getMessage() });
          }

          notifyListenersWithException(e);
        }
      }
    }
  }

  /**
   * Using given configuration creates and starts CPE processing pipeline. It is either
   * single-threaded or a multi-threaded pipeline. Which is actually used depends on the
   * configuration defined in the CPE descriptor. In multi-threaded mode, the CPE starts number of
   * threads: 1) ArtifactProducer Thread - this is a thread containing a Collection Reader. It runs
   * asynchronously and it fills a WorkQueue with CASes. 2) CasConsumer Thread - this is an optional
   * thread. It is only instantiated if there Cas Consumers in the pipeline 3) Processing Threads -
   * one or more processing threads, configured identically, that are performing analysis How many
   * threads are started depends on configuration in CPE descriptor
   * 
   * All threads started here are placed in a ThreadGroup. This provides a catch-all mechanism for
   * errors that may occur in the CPM. If error is thrown, the ThreadGroup is notified. The
   * ThreadGroup than notifies all registers listeners to give an application a chance to report the
   * error and do necessary cleanup. This routine manages all the threads and makes sure that all of
   * them are cleaned up before returning. The ThreadGroup must cleanup all threads under its
   * control otherwise a memory leak occurs. Even those threads that are not started must be cleaned
   * as they end up in the ThreadGroup when instantiated. The code uses number of state variables to
   * make decisions during cleanup.
   * 
   */
  public void run() {
    boolean consumerCompleted = false;
    boolean isStarted = false; // Indicates if all threads have been started

    if (isKilled()) {
      return;
    }
    // Single-threaded mode is enabled in the CPE descriptor. In the CpeConfig element check for the
    // value of deployAs
    // <deployAs>single-threaded</deployAs>
    if (singleThreadedCPE) {
      try {
        runSingleThreaded();
        return;
      } catch (Throwable t) {
        killed = true;
        t.printStackTrace();
        UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_exception_in_single_threaded_cpm__SEVERE",
                new Object[] { Thread.currentThread().getName(), t.getMessage() });
        return;
      } finally {
        ((CPMThreadGroup) getThreadGroup()).cleanup();
        // Fix for memory leak. CPMThreadGroup must be
        // destroyed, but not until AFTER all threads that it
        // owns, including this one, have ended. - Adam
        final ThreadGroup group = this.getThreadGroup();
        Thread threadGroupDestroyer = new Thread(group.getParent(), "threadGroupDestroyer") {
          public void run() {
            while (group.activeCount() > 0) {
              try {
                Thread.sleep(100);
              } catch (InterruptedException e) {
              }
            }
            group.destroy();
          }
        };
        threadGroupDestroyer.start();
      }
    }

    try {

      isRunning = true;
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_starting_cpe__FINEST",
                new Object[] { Thread.currentThread().getName() });
      }

      // How many entities to get for each fetch from the CollectionReader. Use default, otherwise
      // retrieve and override from ColectionReader descriptor.
      readerFetchSize = 1;
      if (collectionReader.getProcessingResourceMetaData().getConfigurationParameterSettings()
              .getParameterValue("fetchSize") != null) {
        readerFetchSize = ((Integer) collectionReader.getProcessingResourceMetaData()
                .getConfigurationParameterSettings().getParameterValue("fetchSize")).intValue();
      }
      if (System.getProperty("DEBUG_CONTROL") != null) {
        startDebugControlThread();
      }
      // CAS[] casList = null;

      if (mixedCasProcessorTypeSupport == false && collectionReader instanceof CollectionReader) {
        mixedCasProcessorTypeSupport = true;
      }

      // When the CPE is configured to run exclusively with CasDataProcessor type components (no
      // CasObjectProcessors)
      // there is no need to instantiate TCAS objects. These would never be used and woud waste
      // memory.
      if (mixedCasProcessorTypeSupport) {
        // Instantiate container for TCAS Instances

        try {
          // Register all type systems with the CAS Manager
          registerTypeSystemsWithCasManager();
          if (poolSize == 0) // Not set in the CpeDescriptor
          {
            poolSize = readerFetchSize * (inputQueueSize + outputQueueSize)
                    * cpeFactory.getProcessingUnitThreadCount() + 3;
            // This is a hack to limit # of CASes. In WF env where the WF Store decides the size of
            // readerFetchSize
            // we have a problem with memory. If the store decides to return 1000 entities we will
            // need a LOT of
            // memory to handle this. So for WF limit the pool size to something more reasonable
            if (poolSize > 100) {
              System.err
                      .println("CPMEngine.run()-CAS PoolSize exceeds hard limit(100). Redefining size to 60.");
              UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG,
                      this.getClass().getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_redefine_pool_size__CONFIG",
                      new Object[] { Thread.currentThread().getName() });
              poolSize = 60; // Hard limit
            }
          }

          if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
                    "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_show_cas_pool_size__CONFIG",
                    new Object[] { Thread.currentThread().getName(), String.valueOf(poolSize) });
          }
          casPool = new CPECasPool(poolSize, cpeFactory.getResourceManager().getCasManager(), 
                  mPerformanceTuningSettings);
          callTypeSystemInit();

        } catch (Exception e) {
          isRunning = false;
          killed = true;
          UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_cp_failed_to_start__SEVERE",
                  new Object[] { Thread.currentThread().getName(), e.getMessage() });

          UIMAFramework.getLogger(this.getClass()).log(Level.FINER, "", e);
          notifyListenersWithException(e);
          return;
        }
      }
      // Instantiate work queue. This queue is shared among all processing units.
      // The Producer thread fills this queue with CAS'es and processing units
      // retrieve these Cas'es for analysis.
      workQueue = new BoundedWorkQueue(poolSize, "Input Queue", this);

      // Instantiate output queue. The Cas'es containing result of analysis are deposited to
      // this queue, and the CasConsumer Processing Unit retrieves them.
      if (consumerList != null && consumerList.size() > 0) {
        outputQueue = createOutputQueue(poolSize);
      }

      if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_create_producer__CONFIG",
                new Object[] { Thread.currentThread().getName() });

      }
      // Producer is responsible for filling work queue with Cas'es. Runs in a seperate thread until
      // all entities are processed or the CPM stops.
      producer = new ArtifactProducer(this, casPool);
      try {
        // Plugin custom timer for measuring performance of the CollectionReader
        producer.setUimaTimer(getTimer());
      } catch (Exception e) {
        // Use default Timer. Ignore the exception
        producer.setUimaTimer(new JavaTimer());
      }
      // indicate how many entities to process
      producer.setNumEntitiesToProcess(numToProcess);
      producer.setCollectionReader(collectionReader);
      producer.setWorkQueue(workQueue);
      // producer.setOutputQueue(outputQueue);

      // collect stats in shared instance
      producer.setCPMStatTable(stats);

      //	
      for (int j = 0; j < statusCbL.size(); j++) {
        BaseStatusCallbackListener statCL = (BaseStatusCallbackListener) statusCbL.get(j);
        if (statCL != null)
          statCL.initializationComplete();
      }

      // Just in case check if the CPM has the right state to start
      if (isKilled()) {
        return;
      }

      // Nov 2005, postpone starting the Producer Thread until all other threads are up.
      // This prevents a problem when the Producer Thread starts, grabs all CASes, fills the
      // input queue and there is an exception BEFORE Processing Units starts. This may lead
      // to a hang, because the CR is waiting on the CAS Pool and no-one consumes the Input Queue.
      // Name the thread
      producer.setName("[CollectionReader Thread]::");

      // Create Cas Consumer Thread
      if (consumerList != null && consumerList.size() > 0) {
        // Create a CasConsumer Processing Unit if there is at least one CasConsumer configured in a
        // CPE descriptor
        casConsumerPU = new ProcessingUnit(this, outputQueue, null);

        casConsumerPU.setProcessingUnitProcessTrace(procTr);
        casConsumerPU.setContainers(consumerList);
        casConsumerPU.setCasPool(casPool);
        casConsumerPU.setReleaseCASFlag(true);
        casConsumerPU.setCasConsumerPipelineIdentity();
        // Add Callback Listeners
        for (int j = 0; j < statusCbL.size(); j++) {
          BaseStatusCallbackListener statCL = (BaseStatusCallbackListener) statusCbL.get(j);
          if (statCL != null) {
            casConsumerPU.addStatusCallbackListener(statCL);
          }
        }
        // Notify Callback Listeners when done processing entity
        casConsumerPU.setNotifyListeners(true);
        // Add custom timer
        try {
          casConsumerPU.setUimaTimer(getTimer());
        } catch (Exception e) {
          // Use default Timer
          casConsumerPU.setUimaTimer(new JavaTimer());
        }
        // name the thread
        casConsumerPU.setName("[CasConsumer Pipeline Thread]::");
        // start the CasConsumer Thread
        casConsumerPU.start();
        consumerThreadStarted = true;
      }
      if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.CONFIG,
                this.getClass().getName(),
                "initialize",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_create_pus__CONFIG",
                new Object[] { Thread.currentThread().getName(),
                    String.valueOf(workQueue.getCurrentSize()) });
      }

      // Adjust number of pipelines. Adjustment may be necessary in deployments using exclusive
      // service access. The adjustment is
      // based on number of available services that the CPM will connect to. If a static
      // configuration calls for 5 processing
      // pipelines but only three services are available (assuming exclusive access ), the CPM will
      // reduce number of processing
      // pipelines to 3.
      for (int indx = 0; indx < annotatorList.size(); indx++) {
        ProcessingContainer prContainer = (ProcessingContainer) annotatorList.get(indx);
        CasProcessorConfiguration configuration = prContainer.getCasProcessorConfiguration();

        if (configuration == null) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                  "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_cp_configuration_not_defined__SEVERE",
                  new Object[] { Thread.currentThread().getName(), prContainer.getName() });
          return;
        }
        String serviceAccess = configuration.getDeploymentParameter("service-access");
        if (serviceAccess != null && serviceAccess.equalsIgnoreCase("exclusive")) {
          if (prContainer.getPool() != null) {
            int totalInstanceCount = prContainer.getPool().getSize();

            if (totalInstanceCount == 0) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE,
                      this.getClass().getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_no_proxies__SEVERE",
                      new Object[] { Thread.currentThread().getName(), prContainer.getName() });
              return;
            }
            if (totalInstanceCount < concurrentThreadCount) {
              concurrentThreadCount = totalInstanceCount; // override
              UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG,
                      this.getClass().getName(), "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_reduce_pipelines__CONFIG",
                      new Object[] { Thread.currentThread().getName(), prContainer.getName() });
            }
          }
        }
      }

      // Setup Processing Pipelines
      processingUnits = new ProcessingUnit[concurrentThreadCount];
      synchronized (this) {
        activeProcessingUnits = concurrentThreadCount; // keeps track of how many threads are still
        // active. -Adam
      }

      // Capture the state of the pipelines. Initially the state is -1, meaning Not Started
      processingThreadsState = new int[concurrentThreadCount];
      for (int inx = 0; inx < concurrentThreadCount; inx++) {
        processingThreadsState[inx] = -1; // Not Started
      }

      // Configure Processing Pipelines, and start each running in a seperate thread
      for (int i = 0; i < concurrentThreadCount; i++) {
        // casList = new CAS[readerFetchSize];
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_initialize_pipeline__FINEST",
                  new Object[] { Thread.currentThread().getName(), String.valueOf(i) });
        }
        // Plug in custom ProcessingUnit via -DPROCESSING_PIPELINE_IMPL=class
        // Initialize Processing Pipeline with input and output queues
        if (System.getProperty("PROCESSING_PIPELINE_IMPL") != null) {
          String puClass = System.getProperty("PROCESSING_PIPELINE_IMPL");
          try {
            processingUnits[i] = producePU(puClass);
            processingUnits[i].setInputQueue(workQueue);
            processingUnits[i].setOutputQueue(outputQueue);
            processingUnits[i].setCPMEngine(this);
          } catch (Exception e) {
            UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, e.getMessage(), e);
            if (dbgCtrlThread != null) {
              dbgCtrlThread.stop();
            }
            return; // / DONE HERE !!!
          }
        } else {
          processingUnits[i] = new ProcessingUnit(this, workQueue, outputQueue);
        }
        // If there are no consumers in the pipeline, instruct the pipeline to release a CAS at the
        // end of processing
        if (consumerList == null || consumerList.size() == 0) {
          processingUnits[i].setReleaseCASFlag(true);
        }

        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "initialize",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_pipeline_impl_class__FINEST",
                  new Object[] { Thread.currentThread().getName(),
                      processingUnits[i].getClass().getName() });
        }
        // Add tracing instance so that performance and stats are globally aggregated for all
        // processing pipelines
        processingUnits[i].setProcessingUnitProcessTrace(procTr);
        // Add all annotators to the processing pipeline
        processingUnits[i].setContainers(annotatorList);
        // pass initialized list of cases to processing units in case cas conversion is required
        // between
        // CasData and CASObject based annotators.
        processingUnits[i].setCasPool(casPool);
        try {
          processingUnits[i].setUimaTimer(getTimer());
        } catch (Exception e) {
          processingUnits[i].setUimaTimer(new JavaTimer());
        }
        // Add Callback Listeners
        for (int j = 0; j < statusCbL.size(); j++) {
          BaseStatusCallbackListener statCL = (BaseStatusCallbackListener) statusCbL.get(j);
          if (statCL != null)
            processingUnits[i].addStatusCallbackListener(statCL);
        }

        // Start the Processing Unit thread
        processingUnits[i].setName("[Procesing Pipeline#" + (i + 1) + " Thread]::");

        // Start the Processing Pipeline
        processingUnits[i].start();
        processingThreadsState[i] = 1; // Started
      }

      producer.setProcessTrace(procTr);
      // Start the ArtifactProducer thread and the Collection Reader embedded therein. The
      // Collection Reader begins
      // processing and deposits CASes onto a work queue.
      producer.start();
      readerThreadStarted = true;

      // Indicate that ALL threads making up the CPE have been started
      isStarted = true;

      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_started_pipelines__FINEST",
                new Object[] { Thread.currentThread().getName() });
      }

      // ==============================================================================================
      // Now, wait for ALL CPE threads to finish. Join each thread created and wait for each to
      // finish.
      // ==============================================================================================

      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_join_threads__FINEST",
                new Object[] { Thread.currentThread().getName() });
      }
      // Join the producer as it knows when to stop processing. When it is done, it
      // simply terminates the thread. Once it terminates lets just make sure that
      // all threads finish and the work queue is completely depleted and all entities
      // are processed
      producer.join();
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_cr_thread_completed__FINEST",
                new Object[] { Thread.currentThread().getName() });
      }

      // Join each of the Processing Threads and wait for them to finish
      for (int i = 0; i < concurrentThreadCount; i++) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_join_pu__FINEST",
                  new Object[] { Thread.currentThread().getName(), processingUnits[i].getName(),
                      String.valueOf(i) });
        }
        processingUnits[i].join();
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_join_pu_complete__FINEST",
                  new Object[] { Thread.currentThread().getName(), processingUnits[i].getName(),
                      String.valueOf(i) });
        }
      }

      // Join the Consumer Thread and wait for it to finish
      if (casConsumerPU != null) {

        try {
          // Throw in a EOF token onto an output queue to indicate end of processing. The consumer
          // will stop the processing upon receiving this token
          Object[] eofToken = new Object[1];
          // only need one member in the array
          eofToken[0] = new EOFToken();
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_placed_eof_in_queue__FINEST",
                    new Object[] { Thread.currentThread().getName(), outputQueue.getName() });
          }
          outputQueue.enqueue(eofToken);
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_done_placed_eof_in_queue__FINEST",
                    new Object[] { Thread.currentThread().getName(), outputQueue.getName() });
          }
//          synchronized (outputQueue) { // redundant, the above enqueue does this
//            outputQueue.notifyAll();
//          }
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_done_notifying_queue__FINEST",
                    new Object[] { Thread.currentThread().getName(), outputQueue.getName() });
          }
        } catch (Exception e) {
          e.printStackTrace();
          UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_exception_adding_eof__SEVERE",
                  new Object[] { Thread.currentThread().getName(), e.getMessage() });
          notifyListenersWithException(e);
        }
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_join_cc__FINEST",
                  new Object[] { Thread.currentThread().getName() });

        }

        casConsumerPU.join();
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_join_cc_completed__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
      }
      consumerCompleted = true;

      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "process",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_cc_completed__FINEST",
                new Object[] { Thread.currentThread().getName(), workQueue.getName(),
                    String.valueOf(workQueue.getCurrentSize()) });
      }
      boolean empty = false;

      while (!empty && outputQueue != null) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_pus_completed__FINEST",
                  new Object[] { Thread.currentThread().getName(), outputQueue.getName(),
                      String.valueOf(outputQueue.getCurrentSize()) });
        }
        synchronized (outputQueue) {
          if (outputQueue.getCurrentSize() == 0) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(
                      Level.FINEST,
                      this.getClass().getName(),
                      "process",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_pus_completed__FINEST",
                      new Object[] { Thread.currentThread().getName(), outputQueue.getName(),
                          String.valueOf(outputQueue.getCurrentSize()) });
            }
            break;
          }
        }
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_consuming_queue__FINEST",
                  new Object[] { Thread.currentThread().getName(), outputQueue.getName(),
                      String.valueOf(outputQueue.getCurrentSize()) });
        }

        if (casConsumerPU != null) {
          casConsumerPU.consumeQueue();
        }
      }

      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_cleaning_up_pus__FINEST",
                new Object[] { Thread.currentThread().getName() });

      }

      // Terminate Annotators and cleanup resources
      for (int i = 0; i < processingUnits.length; i++) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_stop_processors__FINEST",
                  new Object[] { Thread.currentThread().getName(), String.valueOf(i) });
        }
        processingUnits[i].stopCasProcessors(false);
      }
      if (casConsumerPU != null) {
        // Terminate CasConsumers and cleanup
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_stop_ccs__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
        casConsumerPU.stopCasProcessors(false);
      }

      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_engine_stopped__FINEST",
                new Object[] { Thread.currentThread().getName() });
      }
      if (dbgCtrlThread != null) {
        dbgCtrlThread.stop();
      }

      isRunning = false;

    } catch (Exception e) {
      isRunning = false;
      killed = true;
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINER, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_exception__FINER",
              new Object[] { Thread.currentThread().getName(), e.getMessage() });

      UIMAFramework.getLogger(this.getClass()).log(Level.FINER, "", e);
      notifyListenersWithException(e);
      // The CPE has not been started successfully. Perhaps only partially started. Meaning, that
      // some of its threads are started and some not. This may lead to a memory leak as not started
      // threads are never garbage collected. If this is the state of the CPE (!isStarted) go
      // through
      // a cleanup cycle checking each thread and starting those that have not been started. All
      // CPE threads in their run() method MUST check the state of the CPE by calling
      // cpe.isRunning()
      // as the first thing in their run() methods. If this query returns false, all threads should
      // return from run() without doing any work. But at least they will be garbage collected.

      if (!isStarted) {
        // Cleanup not started threads

        // First the ArtifactProducer Thread
        if (producer != null && !producer.isRunning()) {
          try {
            if (!readerThreadStarted) {
              producer.start();
            }
            producer.join();
          } catch (Exception ex1) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_cr_exception__SEVERE",
                    new Object[] { Thread.currentThread().getName(), ex1.getMessage() });

            UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, "", ex1);
            notifyListenersWithException(ex1);
          }
        }
        // Cleanup CasConsumer
        if (casConsumerPU != null && !casConsumerPU.isRunning()) {
          try {
            if (!consumerThreadStarted) {
              casConsumerPU.start();
            }
            casConsumerPU.join();
          } catch (Exception ex1) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_cc_exception__SEVERE",
                    new Object[] { Thread.currentThread().getName(), ex1.getMessage() });

            UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, "", ex1);
            notifyListenersWithException(ex1);
          }
        }

        try {
          // Place EOF Token onto work queue to force PUs shutdown
          forcePUShutdown();

          // Cleanup Processing Threads
          for (int i = 0; processingUnits != null && i < concurrentThreadCount; i++) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(
                      Level.FINEST,
                      this.getClass().getName(),
                      "process",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_join_pu__FINEST",
                      new Object[] { Thread.currentThread().getName(),
                          processingUnits[i].getName(), String.valueOf(i) });
            }
            if (processingUnits[i] != null) {
              // In case the processing thread was created BUT not started we need to
              // start it to make sure it is cleaned up by the ThreadGroup. Not started
              // threads hang around in the ThreadGroup despite the fact that are started.
              // The run() method is instrumented to immediately exit since the CPE is
              // not running. So the thread only starts for a brief moment and than stops.
              // This code is only executed in case where the thread is NOT started
              // In such a case 'processingThreadsState[i] = -1'

              if (processingThreadsState[i] == -1 && !processingUnits[i].isRunning()) {
                processingUnits[i].start();
              }
              try {
                processingUnits[i].join();
                if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
                  UIMAFramework.getLogger(this.getClass()).logrb(
                          Level.FINEST,
                          this.getClass().getName(),
                          "process",
                          CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                          "UIMA_CPM_join_pu_complete__FINEST",
                          new Object[] { Thread.currentThread().getName(),
                              processingUnits[i].getName(), String.valueOf(i) });
                }
              } catch (Exception ex1) {
                UIMAFramework.getLogger(this.getClass()).logrb(Level.FINER,
                        this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                        "UIMA_CPM_exception__FINER",
                        new Object[] { Thread.currentThread().getName(), ex1.getMessage() });
                UIMAFramework.getLogger(this.getClass()).log(Level.FINER, "", ex1);
                notifyListenersWithException(ex1);
              }

            }
          }
        } catch (Exception ex) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINER, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_exception__FINER",
                  new Object[] { Thread.currentThread().getName(), ex.getMessage() });
          UIMAFramework.getLogger(this.getClass()).log(Level.FINER, "", ex);
          notifyListenersWithException(ex);
        }
      }
    } finally {
      if (!consumerCompleted && casConsumerPU != null) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_join_cc__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }

        try {
          Object[] eofToken = new Object[1];
          // only need one member in the array
          eofToken[0] = new EOFToken();
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {

            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_placed_eof_in_queue__FINEST",
                    new Object[] { Thread.currentThread().getName(), outputQueue.getName() });
          }
          outputQueue.enqueue(eofToken);
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_done_placed_eof_in_queue__FINEST",
                    new Object[] { Thread.currentThread().getName(), outputQueue.getName() });
          }
//          synchronized (outputQueue) { // redundant - the above enqueue does this
//            outputQueue.notifyAll();
//          }
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_done_notifying_queue__FINEST",
                    new Object[] { Thread.currentThread().getName(), outputQueue.getName() });
          }
        } catch (Exception e) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_exception_adding_eof__SEVERE",
                  new Object[] { Thread.currentThread().getName() });
          notifyListenersWithException(e);
        }
        try {
          casConsumerPU.join();
        } catch (InterruptedException e) {

        }
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_cc_completed__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
      }

      // Fix for memory leak. CPMThreadGroup must be
      // destroyed, but not until AFTER all threads that it
      // owns, including this one, have ended. - Adam
      final ThreadGroup group = this.getThreadGroup();
      Thread threadGroupDestroyer = new Thread(group.getParent(), "threadGroupDestroyer") {
        public void run() {
          Thread[] threads;
          while (group.activeCount() > 0) {
            try {
              Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            threads = new Thread[group.activeCount()];
            group.enumerate(threads);
            showThreads(threads);
          }
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_destroy_thread_group__FINEST",
                    new Object[] { Thread.currentThread().getName() });
          }

          group.destroy();
        }

        private void showThreads(Thread[] aThreadList) {
          for (int i = 0; aThreadList != null && i < aThreadList.length; i++) {
            if (aThreadList[i] != null && UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(
                      Level.FINEST,
                      this.getClass().getName(),
                      "process",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_show_thread__FINEST",
                      new Object[] { Thread.currentThread().getName(), String.valueOf(i),
                          aThreadList[i].getName() });
            }
          }
        }
      };
      threadGroupDestroyer.start();

    }
  }

  /**
   * Place EOF Token onto a work queue to force thread exit
   * 
   */
  private void forcePUShutdown() {
    try {

      Object[] eofToken = new Object[1];
      // only need one member in the array
      eofToken[0] = new EOFToken();
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_placed_eof_in_queue__FINEST",
                new Object[] { Thread.currentThread().getName(), workQueue.getName() });
      }
      workQueue.enqueue(eofToken);
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_done_placed_eof_in_queue__FINEST",
                new Object[] { Thread.currentThread().getName(), workQueue.getName() });
      }
//      synchronized (workQueue) { // redundant - the above enqueue does this
//        workQueue.notifyAll();
//      }
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_done_notifying_queue__FINEST",
                new Object[] { Thread.currentThread().getName(), workQueue.getName() });
      }
    } catch (Exception e) {
      e.printStackTrace();
      UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_exception_adding_eof__SEVERE",
              new Object[] { Thread.currentThread().getName(), e.getMessage() });
      notifyListenersWithException(e);
    }

  }

  /**
   * Return timer to measure performace of the cpm. The timer can optionally be configured in the
   * CPE descriptor. If none defined, the method returns default timer.
   * 
   * @return - customer timer or JavaTimer (default)
   * 
   * @throws Exception -
   */
  private UimaTimer getTimer() throws Exception {
    String uimaTimerClass = cpeFactory.getCPEConfig().getCpeTimer().get();
    if (uimaTimerClass != null) {
      new TimerFactory(uimaTimerClass);
      return TimerFactory.getTimer();
    }
    // If not timer defined return default timer based on System.currentTimeMillis()
    return new JavaTimer();
  }

  /**
   * Null out fields of this object. Call this only when this object is no longer needed.
   */
  public void cleanup() {
    try {
      if (processingUnits != null) {
        for (int i = 0; i < this.processingUnits.length; i++) {
          this.processingUnits[i].cleanup();
        }
      }

      if (dbgCtrlThread != null) {
        dbgCtrlThread.stop();
      }

      if (casConsumerPU != null) {
        this.casConsumerPU.cleanup();
      }
      this.casConsumerPU = null;

      if (collectionReader != null) {
        this.collectionReader.close();
      }
      this.collectionReader = null;

      if (producer != null) {
        this.producer.cleanup();
      }
      this.producer = null;

      if (consumerDeployList != null) {
        this.consumerDeployList.clear();
      }
      this.consumerDeployList = null;

      if (analysisEngines != null) {
        this.analysisEngines.clear();
      }
      this.analysisEngines = null;

      if (annotatorDeployList != null) {
        this.annotatorDeployList.clear();
      }
      this.annotatorDeployList = null;

      if (annotatorList != null) {
        this.annotatorList.clear();
      }
      this.annotatorList = null;

      if (consumerList != null) {
        this.consumerList.clear();
      }
      this.consumerList = null;

      if (consumers != null) {
        this.consumers.clear();
      }
      this.consumers = null;

      this.processingUnits = null;
      this.casprocessorList = null;
      // this.enProcSt = null;
      this.stats = null;
      this.statusCbL = null;
      // this.tcas = null;
      this.casPool = null;
      // this.restoredProcTr = null;
      this.checkpointData = null;
      this.procTr = null;
      this.cpeFactory = null;
    } catch (Exception e) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINER, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_exception__FINER",
              new Object[] { Thread.currentThread().getName(), e.getMessage() });

    }
  }

  /**
   * Registers Type Systems of all components with the CasManager.
   * 
   */
  private void registerTypeSystemsWithCasManager() throws Exception {
    CasManager manager= this.cpeFactory.getResourceManager().getCasManager();
    
    ProcessingResourceMetaData crMetaData = collectionReader.getProcessingResourceMetaData();
    if (crMetaData != null) {
      manager.addMetaData(crMetaData);
    }
    if (collectionReader instanceof CollectionReader) {
      CasInitializer casIni = ((CollectionReader) collectionReader).getCasInitializer();
      if (casIni != null && casIni.getProcessingResourceMetaData() != null) {
        manager.addMetaData(casIni.getProcessingResourceMetaData());
      }
    } else if (collectionReader instanceof CasDataCollectionReader) {
      CasDataInitializer casIni = ((CasDataCollectionReader) collectionReader)
              .getCasDataInitializer();
      if (casIni != null && casIni.getCasInitializerMetaData() != null) {
        manager.addMetaData(casIni.getCasInitializerMetaData());
      }
    }
    for (int i = 0; i < annotatorList.size(); i++) {
      ProcessingContainer container = (ProcessingContainer) annotatorList.get(i);
      if (container.getStatus() == Constants.CAS_PROCESSOR_DISABLED) {
        continue; // skip over disabled CasProcessors
      }
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_checkout_cp_from_container__FINEST",
                new Object[] { Thread.currentThread().getName(), container.getName() });
      }
      CasProcessor processor = container.getCasProcessor();
      try {
        if (processor instanceof AnalysisEngineImplBase) {
          //Integrated AEs already have added their metadata to the CasManager during
          //their initialization, so we don't need to do it again.
          //(Exception: when running from "old" CPM interface - where AEs are created outside 
          // and passed in, the AE may not share a ResourceManager with the CPE.  In that case
          // we DO need to register its metadata.)
          if (((AnalysisEngine)processor).getResourceManager() == this.cpeFactory.getResourceManager())
            continue;        
        }
        ProcessingResourceMetaData md = processor.getProcessingResourceMetaData();
  
        if (md != null) {
          manager.addMetaData(md);
        }
      }
      finally {
        container.releaseCasProcessor(processor);
      }
    }
    for (int i = 0; i < consumerList.size(); i++) {
      ProcessingContainer container = (ProcessingContainer) consumerList.get(i);
      if (container.getStatus() == Constants.CAS_PROCESSOR_DISABLED) {
        continue; // skip over disabled CasProcessors
      }

      if (UIMAFramework.getLogger().isLoggable(Level.FINE)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINE, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_checkout_cp_from_container__FINEST",
                new Object[] { Thread.currentThread().getName(), container.getName() });
      }

      CasProcessor processor = container.getCasProcessor();
      try {
        if (processor instanceof AnalysisEngineImplBase) {
          //(Exception: when running from "old" CPM interface - where AEs are created outside 
          // and passed in, the AE may not share a ResourceManager with the CPE.  In that case
          // we DO need to register its metadata.)
          if (((AnalysisEngine)processor).getResourceManager() == this.cpeFactory.getResourceManager())
            continue;         
        }
        ProcessingResourceMetaData md = processor.getProcessingResourceMetaData();
  
        if (md != null) {
          manager.addMetaData(md);
        }
      }
      finally {
        container.releaseCasProcessor(processor);
      }
    }
  }

  /**
   * Call typeSystemInit method on each component
   */
  private void callTypeSystemInit() throws ResourceInitializationException {

    CAS cas = casPool.getCas();

    try {
      if (collectionReader instanceof CollectionReader) {
        ((CollectionReader) collectionReader).typeSystemInit(cas.getTypeSystem());

        CasInitializer casIni = ((CollectionReader) collectionReader).getCasInitializer();
        if (casIni != null) {
          casIni.typeSystemInit(cas.getTypeSystem());
        }
      }

      for (int i = 0; i < annotatorList.size(); i++) {
        ProcessingContainer container = (ProcessingContainer) annotatorList.get(i);
        if (container.getStatus() == Constants.CAS_PROCESSOR_DISABLED) {
          continue; // skip over disabled CasProcessors
        }
        CasProcessor processor = container.getCasProcessor();
        if (processor instanceof CasObjectProcessor) {
          ((CasObjectProcessor) processor).typeSystemInit(cas.getTypeSystem());
        }
        container.releaseCasProcessor(processor);
      }
      for (int i = 0; i < consumerList.size(); i++) {
        ProcessingContainer container = (ProcessingContainer) consumerList.get(i);
        if (container.getStatus() == Constants.CAS_PROCESSOR_DISABLED) {
          continue; // skip over disabled CasProcessors
        }
        CasProcessor processor = container.getCasProcessor();
        if (processor instanceof CasObjectProcessor) {
          ((CasObjectProcessor) processor).typeSystemInit(cas.getTypeSystem());
        }
        container.releaseCasProcessor(processor);
      }
    } catch (ResourceInitializationException e) {
      throw e;
    } catch (Exception e) {
      throw new ResourceInitializationException(e);
    } finally {
      casPool.releaseCas(cas);
//      synchronized (casPool) { // redundant, the above releaseCas call does this
//        casPool.notifyAll();
//      }
    }
  }

  /**
   * Stops All Cas Processors and optionally changes the status according to kill flag
   * 
   * @param kill - true if CPE has been stopped before completing normally
   */

  public void stopCasProcessors(boolean kill) throws CasProcessorDeploymentException {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_stop_containers__FINEST",
              new Object[] { Thread.currentThread().getName() });
    }
    // Stop all running CASProcessors
    for (int i = 0; annotatorList != null && i < annotatorList.size(); i++) {
      ProcessingContainer container = (ProcessingContainer) annotatorList.get(i);

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
        // CasProcessor processor = container.getCasProcessor();
        // Change the status of this container to KILLED if the CPM has been stopped
        // before completing the collection and current status of CasProcessor is
        // either READY or RUNNING
        if (kill || (stopped && isProcessorReady(container.getStatus()))) {
          container.setStatus(Constants.CAS_PROCESSOR_KILLED);
        } else {
          // If the CasProcessor has not been disabled during processing change its
          // status to COMPLETED.
          if (container.getStatus() != Constants.CAS_PROCESSOR_DISABLED) {
            container.setStatus(Constants.CAS_PROCESSOR_COMPLETED);
          }
        }
        saveStat("ProcessorStatus", String.valueOf(container.getStatus()), container);
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_stop_container__FINEST",
                  new Object[] { Thread.currentThread().getName(), container.getName() });
        }
        CasProcessorDeployer deployer = container.getDeployer();
        if (deployer != null)
          deployer.undeploy();
        container.destroy();
      }
    }
    // Destroy Cas Consumers
    for (int i = 0; consumerList != null && i < consumerList.size(); i++) {
      ProcessingContainer container = (ProcessingContainer) consumerList.get(i);
      container.destroy();
    }

  }

  /**
   * Returns collectionReader progress.
   */
  public Progress[] getProgress() {
    if (collectionReader == null) {
      return null;
    }
    return collectionReader.getProgress();
  }

  private HashMap getStatForContainer(ProcessingContainer aContainer) {
    HashMap cpStatMap = null;
    if (stats != null && (cpStatMap = (HashMap) stats.get(aContainer.getName())) != null) {
      return cpStatMap;
    }
    return null;
  }

  private void saveStat(String aStatLabel, String aStatValue, ProcessingContainer aContainer) {

    HashMap cpStatMap = getStatForContainer(aContainer);

    if (cpStatMap != null) {
      cpStatMap.put(aStatLabel, aStatValue);
    }
  }

  /**
   * Check if the CASProcessor status is available for processing
   */
  private boolean isProcessorReady(int aStatus) {
    if (aStatus == Constants.CAS_PROCESSOR_READY || aStatus == Constants.CAS_PROCESSOR_RUNNING) {
      return true;
    }

    return false;
  }

  public void invalidateCASes(CAS[] aCASList) {
    if (producer != null) {
      producer.invalidate(aCASList);
    } else {
      ChunkMetadata meta = CPMUtils.getChunkMetadata(aCASList[0]);
      if (meta != null && meta.isOneOfMany() && skippedDocs.containsKey(meta.getDocId()) == false) {
        skippedDocs.put(meta.getDocId(), meta.getDocId());
      }
    }
    if (outputQueue != null) {
      outputQueue.invalidate(aCASList);
    }
    releaseCASes(aCASList);
  }

  /**
   * Releases given cases back to pool.
   * 
   * @param aCASList -
   *          cas list to release
   */
  public void releaseCASes(CAS[] aCASList) {
    for (int i = 0; i < aCASList.length; i++) {
      if (aCASList[i] != null) {
        // aCASList[i].reset();
        casPool.releaseCas(aCASList[i]);
//        synchronized (casPool) {  // redundant - the above releaseCas call does this
//          casPool.notifyAll();
//        }

      } else {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_release_tcas__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
      }
    }
  }

  /**
   * Overrides the default performance tuning settings for this CPE. This affects things such as CAS
   * sizing parameters.
   * 
   * @param aPerformanceTuningSettings
   *          the new settings
   * 
   * @see UIMAFramework#getDefaultPerformanceTuningProperties()
   */
  public void setPerformanceTuningSettings(Properties aPerformanceTuningSettings) {
    mPerformanceTuningSettings = aPerformanceTuningSettings;
  }

  /**
   * @return Returns the PerformanceTuningSettings.
   */
  public Properties getPerformanceTuningSettings() {
    return mPerformanceTuningSettings;
  }

  /**
   * 
   * @param aPca
   */
  public void setProcessControllerAdapter(ProcessControllerAdapter aPca) {
    pca = aPca;
  }

  /*
   * Return CPE Configuration params. Limit access to classes in the same package
   */
  protected CpeConfiguration getCpeConfig() throws Exception {
    return cpeFactory.getCPEConfig();
  }

  /**
   * Called from the ProcessingUnits when they shutdown due to having received the EOFToken. When
   * all ProcessingUnits have shut down, we put an EOFToken on the output queue so that The CAS
   * Consumers will also shut down. -Adam
   */
  synchronized void processingUnitShutdown(ProcessingUnit unit) {
    activeProcessingUnits--;
    if (activeProcessingUnits == 0 && outputQueue != null) {
      Object[] eofToken = new Object[1];
      eofToken[0] = new EOFToken();

      outputQueue.enqueue(eofToken);
//      synchronized (outputQueue) { // redundant - the above enqueue call does this
//        outputQueue.notifyAll();
//      }
     
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
            "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
            "UIMA_CPM_done_placed_eof_in_queue__FINEST",
            new Object[] { Thread.currentThread().getName(), outputQueue.getName() });
      }
    }

  }

  public boolean dropCasOnException() {
    return dropCasOnExceptionPolicy;
  }

  private Object getCasWithSOFA(Object entity, ProcessTrace pTrTemp) {
    CAS[] casList = new CAS[1];
    // CasObject based CollectionReader does not support returning more than one CAS at a time. So
    // fake support for this by calling its getNext() until the casList is filled to max capacity.
    // The capacity of casList is equal to the CollectionReader fetchSize, defined in CR descriptor.
    try {
      if (collectionReader instanceof CollectionReader) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_call_get_cas__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }

        if (entity != null && entity instanceof CAS[]) {
          casList = (CAS[]) entity;
        } else {
          readerState = 1001;
          while (this.isRunning && (casList[0] = casPool.getCas(0)) == null)
            ; // intentionally empty while loop
          entity = casList;
        }

        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_call_get_cas_returns_null_FINEST",
                  new Object[] { Thread.currentThread().getName(),
                      String.valueOf((casList[0] == null)) });
        }
        if (this.isRunning() == false) {
          readerState = 1009;
          casPool.releaseCas(casList[0]);
//          synchronized (casPool) { // redundant - the above releaseCas call does this
//            casPool.notifyAll();
//          }
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_in_shutdown_state__FINEST",
                    new Object[] { Thread.currentThread().getName() });
          }
          readerState = 1010;
          return null;
        }
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_got_cas_from_pool__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }

        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_call_cas_reset__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
        casList[0].reset();

        boolean sofaUnaware = needsView();
        readerState = 1003;
        long st00 = System.currentTimeMillis();
        if (sofaUnaware) {
          // sofa-unaware style CR, give it the initial view
          CAS view = casList[0].getView(CAS.NAME_DEFAULT_SOFA);
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_call_cr_next__FINEST",
                    new Object[] { Thread.currentThread().getName(), "TCAS" });
          }
          ((CollectionReader) collectionReader).getNext(view);
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_call_cr_next_finished__FINEST",
                    new Object[] { Thread.currentThread().getName(), "TCAS" });
          }
        } else // sofa-aware CR, give it the base CAS
        {
          CAS baseCas = ((CASImpl) casList[0]).getBaseCAS();
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_call_cr_next__FINEST",
                    new Object[] { Thread.currentThread().getName(), "CAS" });
          }
          ((CollectionReader) collectionReader).getNext(baseCas);
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_call_cr_next_finished__FINEST",
                    new Object[] { Thread.currentThread().getName(), "CAS" });
          }
        }
        crFetchTime += (System.currentTimeMillis() - st00);
      }
      entity = casList;
      return entity;
    } catch (Exception e) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINER)) {
        e.printStackTrace();
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINER, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_exception__FINER",
                new Object[] { Thread.currentThread().getName(), e.getMessage() });
        UIMAFramework.getLogger(this.getClass()).log(Level.WARNING,
                Thread.currentThread().getName() + "" + e);
      }
      handleException(e, casList, pTrTemp);
      releaseCASes(casList);
    }
    return null;
  }

  /**
   * 
   * @return true if needsTCas 
   */
  private boolean needsView() {
    if (definedCapabilities == null) {
      // If Collection Reader and CAS Initilaizer do not declare any output SofAs,
      // must be sent the default view (meaning whatever's mapped to _InitialView)
      // for backward compatiblity
      CasInitializer casIni = ((CollectionReader) collectionReader).getCasInitializer();
      if (casIni != null) {
        definedCapabilities = casIni.getProcessingResourceMetaData().getCapabilities();
      } else {
        definedCapabilities = ((CollectionReader) collectionReader).getProcessingResourceMetaData()
                .getCapabilities();
      }

      for (int j = 0; j < definedCapabilities.length; j++) {
        if (definedCapabilities[j].getOutputSofas().length > 0) {
          needsTCas = false;
          break;
        }
      }

    }

    return needsTCas;
  }

  /**
   * Initialize the CPE
   * 
   * @throws Exception -
   */
  private void bootstrapCPE() throws Exception {
    registerTypeSystemsWithCasManager();
    casPool = new CPECasPool(getPoolSize(), cpeFactory.getResourceManager().getCasManager(), mPerformanceTuningSettings);
    callTypeSystemInit();

    setupProcessingPipeline();
    setupConsumerPipeline();
  }

  /**
   * Setup single threaded pipeline
   * 
   * @throws Exception -
   */
  private void setupProcessingPipeline() throws Exception {
    // activeProcessingUnits = 1;

    nonThreadedProcessingUnit = new NonThreadedProcessingUnit(this);
    // Assign initial status to all Cas Processors in the processing pipeline
    for (int i = 0; i < annotatorList.size(); i++) {
      ((ProcessingContainer) annotatorList.get(i)).setStatus(Constants.CAS_PROCESSOR_RUNNING);
    }

    nonThreadedProcessingUnit.setContainers(annotatorList);
    nonThreadedProcessingUnit.setCasPool(casPool);
    for (int j = 0; j < statusCbL.size(); j++) {
      BaseStatusCallbackListener statCL = (BaseStatusCallbackListener) statusCbL.get(j);
      if (statCL != null)
        nonThreadedProcessingUnit.addStatusCallbackListener(statCL);
    }
  }

  /**
   * Setup Cas Consumer pipeline as single threaded
   * 
   * @throws Exception -
   */
  private void setupConsumerPipeline() throws Exception {
    if (consumerList != null && consumerList.size() > 0) {
      nonThreadedCasConsumerProcessingUnit = new NonThreadedProcessingUnit(this);
      // Assign initial status to all Cas Processors in the processing pipeline
      for (int i = 0; i < consumerList.size(); i++) {
        ((ProcessingContainer) consumerList.get(i)).setStatus(Constants.CAS_PROCESSOR_RUNNING);
      }
      nonThreadedCasConsumerProcessingUnit.setContainers(consumerList);
      nonThreadedCasConsumerProcessingUnit.setCasPool(casPool);
      nonThreadedCasConsumerProcessingUnit.setReleaseCASFlag(false);

      // Add Callback Listeners
      for (int j = 0; j < statusCbL.size(); j++) {
        BaseStatusCallbackListener statCL = (BaseStatusCallbackListener) statusCbL.get(j);
        if (statCL != null)
          nonThreadedCasConsumerProcessingUnit.addStatusCallbackListener(statCL);
      }
      // Notify Callback Listeners when done processing entity
      nonThreadedCasConsumerProcessingUnit.setNotifyListeners(false);
      // Add custom timer
      try {
        nonThreadedCasConsumerProcessingUnit.setUimaTimer(getTimer());
      } catch (Exception e) {
        // Use default Timer
        nonThreadedCasConsumerProcessingUnit.setUimaTimer(new JavaTimer());
      }

    }

  }

  /**
   * Determines if a given CAS should be skipped
   * 
   * @param entity -
   *          container for CAS
   * @return true if a given CAS should be skipped
   */
  private boolean skipDroppedDocument(Object[] entity) {
    if (entity instanceof CAS[]) {
      ChunkMetadata meta = CPMUtils.getChunkMetadata((CAS) entity[0]);
      if (meta != null && skippedDocs.containsKey(meta.getDocId())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Runs the CPE in a single thread without queues.
   * 
   * @throws Exception -
   */
  public void runSingleThreaded() throws Exception {
    Object entity = null;
    isRunning = true;
    bootstrapCPE();
    ProcessTrace pTrTemp = getProcessTrace();
    boolean success = true;
    long entityCount = 0;
    // long start = System.currentTimeMillis();
    long aggTime = 0;
    long ppTime = 0;
    long ccTime = 0;
    long crTime = 0;

    for (int j = 0; j < statusCbL.size(); j++) {
      BaseStatusCallbackListener statCL = (BaseStatusCallbackListener) statusCbL.get(j);
      if (statCL != null)
        statCL.initializationComplete();
    }

    while (isRunning) {
      try {
        // Check if processed all entities as defined in the Cpe Descriptor.
        if (endOfProcessingReached(entityCount)) {
          break;
        }
        waitForCpmToResumeIfPaused(); // blocks if CPM is paused
        // check again the state of the cpm after pause
        if (!isRunning)
          break;
        readerState = 1000;
        if (!collectionReader.hasNext())
          break;
        long st0 = System.currentTimeMillis();
        entity = getCasWithSOFA(entity, pTrTemp);
        crTime += (System.currentTimeMillis() - st0);

        if (entity == null) {
          success = false;
          continue;
        }

        if (entity instanceof CAS[] && skipDroppedDocument((Object[]) entity)) {
          notifyListeners(CAS_PROCESSED_MSG, (Object[]) entity, pTrTemp, new SkipCasException(
                  "Skipping Document Due To Dropped Cas in a Sequence"));
          releaseCASes((CAS[]) entity);
          continue;
        } else {
          // Clear the cache of bad documents
          if (skippedDocs.size() > 0) {
            skippedDocs.clear();
          }
        }
        long st1 = System.currentTimeMillis();

        // If CAS has been dropped due to an exception dont call CasConsumer
        success = nonThreadedProcessingUnit.analyze((Object[]) entity, pTrTemp);
        ppTime += (System.currentTimeMillis() - st1);
        if (success) {
          long st2 = System.currentTimeMillis();
          nonThreadedCasConsumerProcessingUnit.analyze((Object[]) entity, pTrTemp);

          ccTime += (System.currentTimeMillis() - st2);
        }

      } catch (Throwable t) {
        // may change the state of the isRunning on fatal exception
        handleException(t, (Object[]) entity, pTrTemp);
        success = false;
      } finally {
        entityCount++;
        // After sucessfull analysis notify listeners. If there was an exception, it has
        // already been reported
        if (success) {
          readerState = 2007;
          if (entity == null) {
            notifyListeners(CAS_PROCESSED_MSG, null, pTrTemp);
          } else {
            notifyListeners(CAS_PROCESSED_MSG, (Object[]) entity, pTrTemp);
          }
        }
        if (entity != null && entity instanceof CAS[]) {
          releaseCASes((CAS[]) entity);
          entity = null;
        }

        // Update processing trace counts and timers
        synchronized (procTr) {
          long st = System.currentTimeMillis();
          procTr.aggregate(pTrTemp);
          pTrTemp.clear();
          aggTime += (System.currentTimeMillis() - st);
        }
      }
    } // while
    tearDownCPE();
  }

  /**
   * Determines if the CPM processed all documents
   * 
   * @param entityCount -
   *          number of documents processed so far
   * 
   * @return true if all documents processed, false otherwise
   */
  private boolean endOfProcessingReached(long entityCount) {
    // Special case, -1 means all entities in the corpus
    if (numToProcess == -1) {
      return false;
    } else if (numToProcess == 0) {
      return true;
    } else {
      // check if exceeded or matched the configured max number of entities
      return (entityCount >= numToProcess);
    }
  }

  /**
   * Handle given exception
   * 
   * @param t -
   *          exception to handle
   * @param entity -
   *          CAS container
   * @param aPTrace -
   *          process trace
   */
  private void handleException(Throwable t, Object[] entity, ProcessTrace aPTrace) {
    t.printStackTrace();
    if (t instanceof AbortCPMException || t instanceof Error) {
      isRunning = false;
      killed = true;
    }
    notifyListeners(CAS_PROCESSED_MSG, entity, aPTrace, t);
  }

  /**
   * 
   * @param aMsgType
   * @param entity
   * @param aPTrace
   */
  private void notifyListeners(int aMsgType, Object[] entity, ProcessTrace aPTrace) {
    notifyListeners(aMsgType, entity, aPTrace, null);
  }

  private void notifyListeners(int aMsgType, Object[] entity, ProcessTrace aPTrace, Throwable t) {
    // Add Callback Listeners
    for (int j = 0; j < statusCbL.size(); j++) {
      BaseStatusCallbackListener statCL = (BaseStatusCallbackListener) statusCbL.get(j);
      if (statCL != null) {
        EntityProcessStatusImpl eps = new EntityProcessStatusImpl(aPTrace);
        // eps = new EntityProcessStatusImpl(aPTrace);
        if (entity == null) {
          if (t != null) {
            eps.addEventStatus("Process", "Failed", t);
          }
          ((StatusCallbackListener) statCL).entityProcessComplete(null, eps);
        } else {
          for (int i = 0; i < entity.length; i++) {
            if (t != null) {
              eps.addEventStatus("Process", "Failed", t);
            }
            if (entity[i] != null && entity[i] instanceof CAS) {
              callEntityProcessCompleteWithCAS((StatusCallbackListener)statCL, (CAS)entity[i], eps);
//              ((StatusCallbackListener) statCL).entityProcessComplete((CAS) entity[i], eps);
            } else {
              ((StatusCallbackListener) statCL).entityProcessComplete(null, eps);
            }
          }
        }
      }
    }

  }

  /**
   * Internal use only, public for crss package access. switches class loaders and locks cas
   * @param statCL status call back listener
   * @param cas cas
   * @param eps entity process status
   */
  public static void callEntityProcessCompleteWithCAS(StatusCallbackListener statCL, CAS cas, EntityProcessStatus eps) {
    if ( statCL != null ) {
      try {
        if (null != cas)
          ((CASImpl)cas).switchClassLoaderLockCas(statCL);
        statCL.entityProcessComplete(cas, eps);
      } finally {
        if (null != cas) 
          ((CASImpl)cas).restoreClassLoaderUnlockCas();
      }
    }
  }  
  
  private ProcessTrace getProcessTrace() throws Exception {
    ProcessTrace pT = null;
    UimaTimer uTimer = getTimer();
    if (uTimer != null) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_use_custom_timer__FINEST",
                new Object[] { Thread.currentThread().getName(), uTimer.getClass().getName() });
      }
      pT = new ProcessTrace_impl(uTimer, this.getPerformanceTuningSettings());
    } else {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_use_default_timer__FINEST",
                new Object[] { Thread.currentThread().getName() });
      }
      pT = new ProcessTrace_impl(this.getPerformanceTuningSettings());
    }
    return pT;
  }

  /**
   * Stop and cleanup single-threaded CPE.
   * 
   */
  private void tearDownCPE() {
    nonThreadedProcessingUnit.stopCasProcessors(false);
    nonThreadedCasConsumerProcessingUnit.stopCasProcessors(false);
    this.nonThreadedProcessingUnit.cleanup();
    this.nonThreadedCasConsumerProcessingUnit.cleanup();
  }
  
  private void waitForCpmToResumeIfPaused() {
    synchronized (lockForPause) {
      // Pause this thread if CPM has been paused
      while (pause) {
//        threadState = 2016;  thread state is not kept here, only in the ProcessingUnit
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_pausing_pp__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }

        try {
          // Wait until resumed
          lockForPause.wait();
        } catch (InterruptedException e) {
        }
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_resuming_pp__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
      }
    }
  }

}
