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

package org.apache.uima.collection.impl.cpm.utils;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;

/**
 * Callback Listener. Receives event notifications from CPE.
 */
public class TestStatusCallbackListener implements StatusCallbackListener {

  private boolean isFinished = false;

  private boolean isAborted = false;

  private boolean isInitialized = false;

  // counters for function calls
  private int initializationCount = 0;

  private int batchCompletCount = 0;

  private int collProcessingCount = 0;

  private int pausedCount = 0;

  private int resumedCount = 0;

  private int abortedCount = 0;

  private int entityProcessCompleteCount = 0;

  private CAS lastCas = null;

  private EntityProcessStatus lastStatus = null;

  /*
   * Interface methodes from {@link StatusCallbackListener}
   */

  /**
   * Called when the initialization is completed.
   * 
   * @see org.apache.uima.collection.processing.StatusCallbackListener#initializationComplete()
   */
  public void initializationComplete() {
    isInitialized = true;
  }

  /**
   * Called when the batchProcessing is completed.
   * 
   * @see org.apache.uima.collection.processing.StatusCallbackListener#batchProcessComplete()
   * 
   */
  public void batchProcessComplete() {
    batchCompletCount++;
  }

  /**
   * Called when the collection processing is completed.
   * 
   * @see org.apache.uima.collection.processing.StatusCallbackListener#collectionProcessComplete()
   */
  public void collectionProcessComplete() {
    isFinished = true;
    collProcessingCount++;
  }

  /**
   * Called when the CPM is paused.
   * 
   * @see org.apache.uima.collection.processing.StatusCallbackListener#paused()
   */
  public void paused() {
    pausedCount++;
  }

  /**
   * Called when the CPM is resumed after a pause.
   * 
   * @see org.apache.uima.collection.processing.StatusCallbackListener#resumed()
   */
  public void resumed() {
    resumedCount++;
  }

  /**
   * Called when the CPM is stopped abruptly due to errors.
   * 
   * @see org.apache.uima.collection.processing.StatusCallbackListener#aborted()
   */
  public void aborted() {
    isAborted = true;
    abortedCount++;
  }

  /**
   * Called when the processing of a Document is completed. <br>
   * The process status can be looked at and corresponding actions taken.
   * 
   * @param aCas
   *          CAS corresponding to the completed processing
   * @param aStatus
   *          EntityProcessStatus that holds the status of all the events for aEntity
   */
  public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
    entityProcessCompleteCount++;
    lastCas = aCas;
    lastStatus = aStatus;
  }

  /*
   * getters for methodecounts
   */

  public int getAbortedCount() {
    return abortedCount;
  }

  public int getBatchCompletCount() {
    return batchCompletCount;
  }

  public int getCollProcessingCount() {
    return collProcessingCount;
  }

  public int getInitializationCount() {
    return initializationCount;
  }

  public int getPausedCount() {
    return pausedCount;
  }

  public int getResumedCount() {
    return resumedCount;
  }

  public int getEntityProcessCompleteCount() {
    return entityProcessCompleteCount;
  }

  public EntityProcessStatus getLastStatus() {
    return lastStatus;
  }

  public CAS getLastCas() {
    return lastCas;
  }

  /*
   * status methods
   */
  public boolean isFinished() {
    return isFinished;
  }

  public boolean isAborted() {
    return isAborted;
  }

  public boolean isInitialized() {
    return isInitialized;
  }
}
