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
import java.util.Hashtable;
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas_data.CasData;
import org.apache.uima.collection.CasInitializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.base_cpm.BaseCollectionReader;
import org.apache.uima.collection.base_cpm.CasDataCollectionReader;
import org.apache.uima.collection.base_cpm.SkipCasException;
import org.apache.uima.collection.impl.EntityProcessStatusImpl;
import org.apache.uima.collection.impl.cpm.Constants;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.ChunkMetadata;
import org.apache.uima.collection.impl.cpm.vinci.DATACasUtils;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.util.Level;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.Progress;
import org.apache.uima.util.UimaTimer;
import org.apache.uima.util.impl.ProcessTrace_impl;

/**
 * Component responsible for continuously filling a work queue with bundles containing Cas'es. The
 * queue is shared with a Processing Pipeline that consumes bundles of Cas. As soon as the the
 * bundle is removed from the queue, this component fetches data from configured Collection Reader
 * and enques it onto the queue. This component facilitates asynchronous reading and processing of
 * CAS by seperate threads running in the CPE.
 * 
 * When end of processing is reached due to CPM shutdown or max number of entities are processed a
 * special token, called EOFToken is placed onto a queue. It marks end of processing for Processing
 * Units. No more data is expected to be placed on the work queue. The Processing Threads upon
 * seeing the EOFToken are expected to complete processing and do necessary cleanup.
 * 
 * 
 */
public class ArtifactProducer extends Thread {
  public int threadState = 0;

  private CPECasPool casPool;

  // Queue shared wit ProcessingUnits
  private BoundedWorkQueue workQueue = null;

  // private BoundedWorkQueue outputQueue = null;
  private BaseCollectionReader collectionReader = null;

  // Number of CAS'es for each fetch from the CollectionReader
  private int readerFetchSize = 1;

  private CAS[] casList;

  private long entityCount = 0;

  private long maxToProcess;

  private CPMEngine cpm = null;

  private Map cpmStatTable = null;

  private String[] lastDocId = { "" };

  private long totalFetchTime = 0;

  private UimaTimer timer = null;

  private ArrayList callbackListeners = null;

  private Hashtable timedoutDocs = new Hashtable();

  private boolean isRunning = false;

  private ProcessTrace globalSharedProcessTrace = null;

  /**
   * Instantiates and initializes this instance.
   * 
   * @param acpm
   */
  public ArtifactProducer(CPMEngine acpm) {
    cpm = acpm;
    if (cpm != null) {
      callbackListeners = cpm.getCallbackListeners();
    }
  }

  /**
   * Construct instance of this class with a reference to the cpe engine and a pool of cas'es.
   * 
   * @param acpm -
   *          reference to the cpe
   * @param aPool -
   *          pool of cases
   */
  public ArtifactProducer(CPMEngine acpm, CPECasPool aPool) {
    cpm = acpm;
    casPool = aPool;
    if (cpm != null) {
      callbackListeners = cpm.getCallbackListeners();
    }
  }

  public boolean isRunning() {
    return isRunning;
  }

  /**
   * Plug in Custom Timer to time events
   * 
   * @param aTimer -
   *          custom timer
   */
  public void setUimaTimer(UimaTimer aTimer) {
    timer = aTimer;
  }

  public void setProcessTrace(ProcessTrace aProcTrace) {
    globalSharedProcessTrace = aProcTrace;
  }

  /**
   * Returns total time spent when fetching entities from a CollectionReader. This provides a way of
   * gauging throughput of a particular CR.
   * 
   * @return total time spent when fetching entities. -1 when the fetch time is unknown.
   */
  public long getCollectionReaderTotalFetchTime() {
    if (timer != null && totalFetchTime > 0) {
      return totalFetchTime;
    }
    return -1;
  }

  /**
   * Null out fields of this object. Call this only when this object is no longer needed.
   */
  public void cleanup() {
    this.casPool = null;
    this.workQueue = null;
    this.collectionReader = null;
    this.casList = null;
    this.cpm = null;
    if (this.cpmStatTable != null) {
      this.cpmStatTable.clear();
      this.cpmStatTable = null;
    }
    this.lastDocId = null;
  }

  /**
   * Assign total number of entities to process
   * 
   * @param aNumToProcess -
   *          number of entities to read from the Collection Reader
   */
  public void setNumEntitiesToProcess(long aNumToProcess) {
    maxToProcess = aNumToProcess;
  }

  /**
   * Assign CollectionReader to be used for reading
   * 
   * @param aCollectionReader -
   *          collection reader as source of data
   */
  public void setCollectionReader(BaseCollectionReader aCollectionReader) {
    collectionReader = aCollectionReader;
    if (collectionReader.getProcessingResourceMetaData().getConfigurationParameterSettings()
            .getParameterValue("fetchSize") != null) {
      // Determines how many at a time this Collection Reader will return
      // for each fetch
      readerFetchSize = ((Integer) collectionReader.getProcessingResourceMetaData()
              .getConfigurationParameterSettings().getParameterValue("fetchSize")).intValue();
    }
  }

  /**
   * Assigns a queue where the artifacts produced by this component will be deposited
   * 
   * @param aQueue -
   *          queue for the artifacts this class is producing
   */
  public void setWorkQueue(BoundedWorkQueue aQueue) {
    workQueue = aQueue;
  }

  /**
   * Add table that will contain statistics gathered while reading entities from a Collection This
   * table is used for non-uima reports.
   * 
   * @param aStatTable
   */
  public void setCPMStatTable(Map aStatTable) {
    cpmStatTable = aStatTable;
  }

  /**
   * Determines if the CPM has processed configured number of entities. Called after each fetch from
   * the Collection Reader.
   * 
   * @return true - all configurted entities processed, false otherwise
   */
  private boolean endOfProcessingReached() {
    // Special case, -1 means all entities in the corpus
    if (maxToProcess == -1) {
      return false;
    } else if (maxToProcess == 0) {
      return true;
    } else {
      // check if exceeded or matched the configured max number of
      // entities
      return (entityCount >= maxToProcess);
    }
  }

  /**
   * Fills the queue up to capacity. This is called before activating ProcessingPipeline as means of
   * optimizing processing. When pipelines start up there are already entities in the work queue to
   * process.
   */
  public void fillQueue() throws Exception {
    // Create an array holding CAS'es. Configuration of the Reader may
    // include
    // a number of CAS'es to fetch at a time. In this case the array will
    // have
    // size greater than the default (1)
    Object[] casObjectList = new Object[1];
    long capacity = workQueue.getCapacity();
    if (capacity > maxToProcess) {
      capacity = maxToProcess;
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_reset_queue_size__FINEST",
                new Object[] { Thread.currentThread().getName(), String.valueOf(capacity) });
      }
    }

    try {
      // Fill the work queue with entities from the CollectionReader
      // capacity=number of slots in the work queue
      for (int i = 0; i < capacity; i++) {
        if (collectionReader.hasNext()) {
          // The CollectionReader returns 1 or more entities at a
          // time. The CollectionReader
          // configuration determines how many it will return for each
          // getNext() call. In case
          // WF Large Store, the 'readerFetchSize' parameter is not
          // used, since the store itself
          // determines how many entities to return for each fetch.
          casObjectList = readNext(readerFetchSize);
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "process",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_enqueue_cas_bundle__FINEST",
                    new Object[] { Thread.currentThread().getName(),
                        String.valueOf(casObjectList.length) });
          }
          // Count number of entities fetched so far
          entityCount += casObjectList.length;

          // append entities to queue
          workQueue.enqueue(casObjectList);
          // If CollectionReader returns bundles instead of individual
          // entity
          // make the loop termines if exceeding total number of
          // entities to
          // process
          if (entityCount > maxToProcess) {
            break;
          }
        }
      }
      if (cpmStatTable != null) {
        Progress[] progress = collectionReader.getProgress();
        if (progress != null) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "process",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_show_cr_progress__FINEST",
                    new Object[] { Thread.currentThread().getName(),
                        String.valueOf(progress[0].getCompleted()) });
          }
        }
        cpmStatTable.put("COLLECTION_READER_PROGRESS", progress);
      }
    } catch (Exception e) {
      if (casObjectList == null) {
        notifyListeners(null, e);
      } else {
        // Release CAS's back to the cas pool.
        for (int i = 0; casObjectList != null && i < casObjectList.length; i++) {
          if (casObjectList[i] != null && casObjectList[i] instanceof CAS) {
            notifyListeners((CAS) casObjectList[i], e);
            casPool.releaseCas(casList[i]);
            casList[i] = null;
//            synchronized (casPool) {  // removed - redundant, because done as part of releaseCas
//              casPool.notifyAll();
//            }

          } else {
            notifyListeners(null, e);
          }
        }

      }
      throw e;
    }

  }

  /**
   * Reads next set of entities from the CollectionReader. This method may return more than one Cas
   * at a time.
   * 
   * @parma fetchSize - number of entities the CollectionReader should return for each fetch. It is
   *        hint as the Collection Reader ultimately decides how many to return.
   * 
   * @return - The Object returned from the method depends on the type of the CollectionReader.
   *         Either CASData[] or CASObject[] initialized with document metadata and content is
   *         returned. If the CollectionReader has no more entities (EOF), null is returned.
   * 
   * @throws IOException -
   *           error while reading corpus
   * @throws CollectionException -
   */
  private Object[] readNext(int fetchSize) throws IOException, CollectionException {
    ProcessTrace localTrace = new ProcessTrace_impl(cpm.getPerformanceTuningSettings());

    boolean success = false;
    Object[] casObjects = null;
    threadState = 1000; // Entering hasNext()
    // Checks if the CollectionReader has any documents left
    long start = 0;
    if (timer != null) {
      start = timer.getTimeInMillis();
    }
    boolean eventStarted = false;

    // CasObject based CollectionReader does not support returning more than
    // one CAS at a time. So
    // fake support for this by calling its getNext() until the casList is
    // filled to max capacity.
    // The capacity of casList is equal to the CollectionReader fetchSize,
    // defined in CR descriptor.
    if (collectionReader instanceof CollectionReader) {
      casList = new CAS[fetchSize];
      for (int i = 0; i < fetchSize; i++) {

        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_cr_fetch_new_cas__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }

        threadState = 1001; // Waiting for CAS
        // Get the cas from the pool.
        while (cpm.isRunning() && (casList[i] = casPool.getCas(0)) == null)
          ; // intentionally empty while loop

        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_cr_check_cas_for_null__FINEST",
                  new Object[] { Thread.currentThread().getName(),
                      String.valueOf((casList[i] == null)) });
        }
        if (cpm.isRunning() == false) {
          // CPM is in shutdown stage. No need to enqueue additional
          // documents/CAS'es. Just release
          // those that have been aquired so far back to the pool and
          // return null, indicating
          // end of processing.
          if (timer != null) {
            totalFetchTime += (timer.getTimeInMillis() - start);
          }
          for (int listCounter = 0; casList != null && casList[i] != null
                  && listCounter < casList.length; listCounter++) {
            casPool.releaseCas(casList[listCounter]);
//            synchronized (casPool) { // redundant - releaseCas call does this
//              casPool.notifyAll();
//            }
          }
          if (cpmStatTable != null) {
            Progress[] progress = collectionReader.getProgress();
            cpmStatTable.put("COLLECTION_READER_PROGRESS", progress);
            cpmStatTable.put("COLLECTION_READER_TIME", Long.valueOf(totalFetchTime));
          }
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_in_shutdown_state__FINEST",
                    new Object[] { Thread.currentThread().getName() });
          }
          return null;
        }
        if (casList[i] == null) {
          return null;
        }
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_got_new_cas__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_call_cas_reset__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
        casList[i].reset();

        // If Collection Reader and CAS Initilaizer do not declare any
        // output SofAs, must be passed the default view (meaning whatever's 
        //mapped to _InitialView) for backward compatiblity
        Capability[] capabilities;
        CasInitializer casIni = ((CollectionReader) collectionReader).getCasInitializer();
        if (casIni != null)
          capabilities = casIni.getProcessingResourceMetaData().getCapabilities();
        else
          capabilities = ((CollectionReader) collectionReader).getProcessingResourceMetaData()
                  .getCapabilities();

        boolean sofaUnaware = true;
        for (int j = 0; j < capabilities.length; j++) {
          if (capabilities[j].getOutputSofas().length > 0) {
            sofaUnaware = false;
            break;
          }
        }

        threadState = 1003; // Entering

        // set the current component info of the CAS, so that it knows
        // the sofa
        // mappings for the component that's about to process it
        UimaContextAdmin context = ((CollectionReader) collectionReader).getUimaContextAdmin();
        casList[i].setCurrentComponentInfo(context.getComponentInfo());
        try {
          if (sofaUnaware) {
            // sofa-unaware CR, give it whatever is mapped to the
            // initial view (creating that view first if it's not the default)
            String absSofaName = context.getComponentInfo().mapToSofaID(CAS.NAME_DEFAULT_SOFA);
            if (!CAS.NAME_DEFAULT_SOFA.equals(absSofaName)) {
              casList[i].createView(CAS.NAME_DEFAULT_SOFA);
            }            
            CAS view = casList[i].getView(CAS.NAME_DEFAULT_SOFA);
            
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_call_cr_next__FINEST",
                      new Object[] { Thread.currentThread().getName(), "CAS" });
            }
            localTrace.startEvent(collectionReader.getProcessingResourceMetaData().getName(),
                    "Process", "");
            eventStarted = true;
            ((CollectionReader) collectionReader).getNext(view);
            localTrace.endEvent(collectionReader.getProcessingResourceMetaData().getName(),
                    "Process", "success");

            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_call_cr_next_finished__FINEST",
                      new Object[] { Thread.currentThread().getName(), "CAS" });
            }
          } else
          // sofa-aware CR, give it the base CAS
          {
            CAS baseCas = ((CASImpl) casList[i]).getBaseCAS();
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_call_cr_next__FINEST",
                      new Object[] { Thread.currentThread().getName(), "CAS" });

            }
            localTrace.startEvent(collectionReader.getProcessingResourceMetaData().getName(),
                    "Process", "");
            eventStarted = true;
            ((CollectionReader) collectionReader).getNext(baseCas);
            localTrace.endEvent(collectionReader.getProcessingResourceMetaData().getName(),
                    "Process", "success");

            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_call_cr_next_finished__FINEST",
                      new Object[] { Thread.currentThread().getName(), "CAS" });
            }
          }
          success = true;
        } finally {
          // be sure to unset the component info in the CAS, since the
          // CAS is no longer
          // being processed by the CollectionReader
          casList[i].setCurrentComponentInfo(null);
          if (eventStarted) // use this to make sure we dont end event that has not been explicitely
          // started
          {
            if (!success) {
              localTrace.endEvent(collectionReader.getProcessingResourceMetaData().getName(),
                      "Process", "failure");

            }
            synchronized (globalSharedProcessTrace) {
              globalSharedProcessTrace.aggregate(localTrace);
            }

          }

        }
      }
      casObjects = casList;
      if (casObjects != null && casObjects.length > 0) {
        try {
          if (((CASImpl) casList[0]).isBackwardCompatibleCas()) {
            CAS view = casList[0].getView(CAS.NAME_DEFAULT_SOFA);
            lastDocId[0] = ConsumerCasUtils.getStringFeatValue(view, Constants.METADATA_KEY,
                    Constants.DOC_ID);
          } else {
            lastDocId[0] = "";
          }
        } catch (Exception e) {
          lastDocId[0] = "";
        }
      }
    } else {
      // Retrieve next set of CAS'es. fetchSize is hint to the
      // CollectionReader how many to return
      // Some CollectionReaders return a batch with size different than
      // recommended in
      // fetchSize. Most notably, Large WF Store decides itself how many
      // entities to return for
      // each fetch.

      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_call_cr_next__FINEST",
                new Object[] { Thread.currentThread().getName(), "CasData" });
      }
      localTrace.startEvent(collectionReader.getProcessingResourceMetaData().getName(), "Process",
              "");
      try {
        casObjects = ((CasDataCollectionReader) collectionReader).getNext(fetchSize);
        success = true;
      } finally {
        if (!success) {
          localTrace.endEvent(collectionReader.getProcessingResourceMetaData().getName(),
                  "Process", "failure");

        } else {
          localTrace.endEvent(collectionReader.getProcessingResourceMetaData().getName(),
                  "Process", "success");

        }
        synchronized (globalSharedProcessTrace) {
          globalSharedProcessTrace.aggregate(localTrace);
        }

      }
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_call_cr_next_finished__FINEST",
                new Object[] { Thread.currentThread().getName(), "CasData" });
      }
      if (casObjects != null && casObjects.length > 0) {
        lastDocId = DATACasUtils.getFeatureStructureValues((CasData) casObjects[0],
                Constants.METADATA_KEY, Constants.DOC_ID);
      }
    }
    if (timer != null) {
      totalFetchTime += (timer.getTimeInMillis() - start);
    }

    if (cpmStatTable != null) {
      Progress[] progress = collectionReader.getProgress();
      cpmStatTable.put("COLLECTION_READER_PROGRESS", progress);
      cpmStatTable.put("COLLECTION_READER_TIME", Long.valueOf(totalFetchTime));
    }
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_return_cases_from_cr__FINEST",
              new Object[] { Thread.currentThread().getName(), "CAS" });
    }
    return casObjects;
  }

  /**
   * Runs this thread until the CPM halts or the CollectionReader has no more entities. It
   * continuously fills the work queue with entities returned by the CollectionReader.
   */
  public void run() {
    boolean crEventCompleted = false; // this flag is used to mark the
    // ProcessTrace event
    if (!cpm.isRunning()) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.WARNING, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_cpm_not_running__WARNING",
              new Object[] { Thread.currentThread().getName() });
      return;
    }

    Object[] casObjectList = null;
    // Check if
    if (endOfProcessingReached()) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_cr_done_producing__FINEST",
                new Object[] { Thread.currentThread().getName() });
      }
      placeEOFToken();
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_eof_marker_enqueued__FINEST",
                new Object[] { Thread.currentThread().getName() });
      }
      return;
    }
    isRunning = true;
    ProcessTrace localTrace = new ProcessTrace_impl(cpm.getPerformanceTuningSettings());
    while (cpm.isRunning()) {

      casList = null;
      casObjectList = null;
      synchronized (cpm.lockForPause) {
        if (cpm.isPaused()) {
          try {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_pausing_cr__FINEST",
                      new Object[] { Thread.currentThread().getName() });
            }
            // Wait until resumed
            cpm.lockForPause.wait();
          } catch (Exception e) {
          }
          if (!cpm.isRunning()) {
            break;
          }
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_resume_cr__FINEST",
                    new Object[] { Thread.currentThread().getName() });
          }
        }
      }

      try {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_call_hasnext__FINEST",
                  new Object[] { Thread.currentThread().getName() });
        }
        threadState = 1004; // Entering hasNext()

        // start the CR event
        localTrace.startEvent(collectionReader.getProcessingResourceMetaData().getName(),
                "Process", "");
        crEventCompleted = false;
        if (collectionReader.hasNext()) {
          localTrace.endEvent(collectionReader.getProcessingResourceMetaData().getName(),
                  "Process", "success");
          crEventCompleted = true;

          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_get_cas_from_cr__FINEST",
                    new Object[] { Thread.currentThread().getName() });
          }
          casObjectList = readNext(readerFetchSize);
          if (casObjectList != null) {
            if (casObjectList instanceof CAS[]) {
              boolean releasedCas = false;
              for (int i = 0; i < casObjectList.length && casObjectList[i] != null; i++) {
                ChunkMetadata meta = CPMUtils.getChunkMetadata((CAS) casObjectList[i]);
                if (meta != null) {
                  if (timedoutDocs.containsKey(meta.getDocId())) {
                    notifyListeners(casList[i], new ResourceProcessException(new SkipCasException(
                            "Dropping CAS due chunk Timeout. Doc Id::" + meta.getDocId()
                                    + " Sequence:" + meta.getSequence())));

                    casPool.releaseCas((CAS) casObjectList[i]);
//                    synchronized (casPool) {  // redundant, releaseCas call does this
//                      casPool.notifyAll();
//                    }
                    releasedCas = true;
                  }
                }
              }
              if (releasedCas) {
                continue;
              }
            }
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(
                      Level.FINEST,
                      this.getClass().getName(),
                      "process",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_place_cas_in_queue__FINEST",
                      new Object[] { Thread.currentThread().getName(),
                          String.valueOf(casObjectList.length) });
            }
            // Prevent processing of new CASes if the CPM has been
            // killed hard. Allow processing of CASes
            // while the CPM is in normal shutdown state.
            // (Moved this code inside if (casObjectList != null)
            // block to avoid NullPointerException. -Adam
            if (cpm.isRunning() == true
                    || (cpm.isRunning() == false && cpm.isHardKilled() == false)) {
              threadState = 1005; // Entering enqueue
              workQueue.enqueue(casObjectList);
//              synchronized (workQueue) { // redundant, enqueue does this
//                workQueue.notifyAll();
//              }
              threadState = 1006; // Done Entering enqueue
              entityCount += casObjectList.length;
              if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
                UIMAFramework.getLogger(this.getClass()).logrb(
                        Level.FINEST,
                        this.getClass().getName(),
                        "process",
                        CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                        "UIMA_CPM_placed_cas_in_queue__FINEST",
                        new Object[] { Thread.currentThread().getName(),
                            String.valueOf(casObjectList.length) });
              }
            } else {
              break; // CPM has been killed
            }
          } else {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_terminate_cr_thread__FINEST",
                      new Object[] { Thread.currentThread().getName() });
            }
            break; // Null should not be returned from getNext
            // unless the CPM is in shutdown mode
          }
        } else {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_processed_all__FINEST",
                    new Object[] { Thread.currentThread().getName() });
          }
          // Stops the CPM and all of the running threads.
          // cpm.stopIt(); APL - don't stop, just terminate this
          // thread, which CPMEngine has joined on
          break;

        }
        // Check if the CollectionReader retrieved expected number of
        // entities
        if (endOfProcessingReached()) {
          threadState = 1010; // End of processing

          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_end_of_processing__FINEST",
                    new Object[] { Thread.currentThread().getName() });
          }
          break;
        }
      } catch (Exception e) {
        // The following conditional is true if hasNext() has failed
        if (!crEventCompleted) {
          localTrace.endEvent(collectionReader.getProcessingResourceMetaData().getName(),
                  "Process", "failure");
        }
        // e.printStackTrace();
        // changed from FINER to WARNING: https://issues.apache.org/jira/browse/UIMA-2440
        if (UIMAFramework.getLogger().isLoggable(Level.WARNING)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.WARNING, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_exception__WARNING",
                  new Object[] { Thread.currentThread().getName(), e.getMessage() });

          UIMAFramework.getLogger(this.getClass()).log(Level.WARNING, e.getMessage(), e);
        }
        if (casList == null) {
          notifyListeners(null, e);
        } else {
          // Notify Listeners and release CAS's back to the cas pool.
          for (int i = 0; casList != null && i < casList.length; i++) {
            if (casList[i] != null) {
              notifyListeners(casList[i], e);
              casPool.releaseCas(casList[i]);
              casList[i] = null;
//              synchronized (casPool) { // redundant, releaseCas does this
//                casPool.notifyAll();
//              }

            } else {
              notifyListeners(null, e);
            }
            casList = null;
          }

        }
      } finally {
        // Clear all events
        synchronized (globalSharedProcessTrace) {
          globalSharedProcessTrace.aggregate(localTrace);
        }
        localTrace.clear();
      }
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_show_cpm_running_status__FINEST",
                new Object[] { Thread.currentThread().getName(), String.valueOf(cpm.isRunning()) });
      }
    }

    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_show_cpm_running_status__FINEST",
              new Object[] { Thread.currentThread().getName(), String.valueOf(cpm.isRunning()) });
    }
    // Done with processing. Create a "special" EOF token and place it in
    // the queue.
    // Consumers of the queue must interpret this token as End Of File
    // event, meaning
    // end of processing. Such components must do appropriate cleanup and
    // terminate.
    placeEOFToken();
    isRunning = false;
    // Interrupt any waiting threads
    Thread.currentThread().interrupt();
  }

  /**
   * Notify registered callback listeners of a given exception.
   * 
   * @param anException -
   *          exception to propagate to callback listeners
   */
  private void notifyListeners(CAS aCas, Exception anException) {
    for (int i = 0; callbackListeners != null && i < callbackListeners.size(); i++) {
      StatusCallbackListener statCL = (StatusCallbackListener) callbackListeners.get(i);
      if ( statCL != null ) {
        ProcessTrace prTrace = new ProcessTrace_impl(cpm.getPerformanceTuningSettings());
        EntityProcessStatusImpl aEntityProcStatus = new EntityProcessStatusImpl(prTrace);
        aEntityProcStatus.addEventStatus("Collection Reader Failure", "failed", anException);
        // Notify the listener that the Cas has been processed
        CPMEngine.callEntityProcessCompleteWithCAS(statCL, aCas, aEntityProcStatus);
//        statCL.entityProcessComplete(aCas, aEntityProcStatus);
      }
    }
  }

  /**
   * Place terminating EOFToken into a Work Queue. Any thread reading this token from the queue is
   * responsible for terminating itself.
   * 
   */
  private void placeEOFToken() {
    // Done with processing. Create a "special" EOF token and place it in
    // the queue.
    // Consumers of the queue must interpret this token as End Of File
    // event, meaning
    // end of processing. Such components must do appropriate cleanup and
    // terminate.
    try {
      Object[] eofToken = new Object[1];
      // only need one member in the array
      eofToken[0] = new EOFToken();
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_enqueue_eof_token__FINEST",
                new Object[] { Thread.currentThread().getName(), String.valueOf(cpm.isRunning()) });
      }
      workQueue.enqueue(eofToken);
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_done_enqueue_eof_token__FINEST",
                new Object[] { Thread.currentThread().getName(), String.valueOf(cpm.isRunning()) });

      }
//      synchronized (workQueue) { // redundant, the enqueue call above does this
//        workQueue.notifyAll();
//      }
    } catch (Exception e) {
      e.printStackTrace();
      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.SEVERE, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_exception__FINER",
                new Object[] { Thread.currentThread().getName(), e.getMessage() });
        UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, "", e);
      }
    }
  }

  public String getLastDocId() {
    if (lastDocId != null && lastDocId.length > 0) {
      return lastDocId[0];
    } else {
      return "N/A";
    }
  }

  public void invalidate(CAS[] aCasList) {
    for (int i = 0; aCasList != null && i < aCasList.length && aCasList[i] != null; i++) {
      ChunkMetadata meta = CPMUtils.getChunkMetadata(aCasList[i]);
      // Add the docId into a cache of documents that have been dropped
      // due to exception
      // during processing. This is only done for chunked documents
      // (sequence > 0)
      if (meta != null && meta.getSequence() > 0 && !timedoutDocs.containsKey(meta.getDocId())) {
        timedoutDocs.put(meta.getDocId(), meta.getDocId());
      }
    }
  }
}
