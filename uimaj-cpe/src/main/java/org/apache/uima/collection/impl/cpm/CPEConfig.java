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

import org.apache.uima.collection.CollectionReader;
import org.apache.uima.util.UimaTimer;

/**
 * Object containing global cpe configuration.
 * 
 * 
 */

public class CPEConfig {
  private CheckpointConfig chConfig = null;

  private long numToProcess = 0;

  private String deployAs = "";

  private String timerClass = null;

  private String startWith = "";

  private long inputQueueMaxMemoryThreshold = 0;

  private long maxTimeToWait = -1;

  private String outputQueueClass = null;

  /**
   * Returns CPE checkpoint configuration
   * 
   * @return CheckpointConfig object
   */
  public CheckpointConfig getChConfig() {
    return chConfig;
  }

  /**
   * Returns CPE statup mode as defined in the CPE descriptor
   * 
   * @return - statup mode
   */
  public String getDeployAs() {
    return deployAs;
  }

  /**
   * Returns number of entities to process by the CPE.
   * 
   * @return - number of entities to process
   */
  public long getNumToProcess() {
    return numToProcess;
  }

  /**
   * Returns an id of the first entity the {@link CollectionReader} will be told to read
   * 
   * @return - id of the first entity to read
   */
  public String getStartWith() {
    return startWith;
  }

  /**
   * Returns the name of custom {@link UimaTimer} class.
   * 
   * @return - class as String
   */
  public String getTimerClass() {
    return timerClass;
  }

  /**
   * Copies Checkpoint configuration
   * 
   * @param config -
   *          checkpoint configuration
   */
  public void setChConfig(CheckpointConfig config) {
    chConfig = config;
  }

  /**
   * Copies CPE startup mode
   * 
   * @param aCpeDeployMode -
   *          startup mode
   */
  public void setDeployAs(String aCpeDeployMode) {
    deployAs = aCpeDeployMode;
  }

  /**
   * Copies number of entities to process
   * 
   * @param aTotalCount -
   *          total number of entities to process
   */
  public void setNumToProcess(long aTotalCount) {
    numToProcess = aTotalCount;
  }

  /**
   * Copies ind of the first entity to start reading
   * 
   * @param aStartEntityId -
   *          id of entity
   */
  public void setStartWith(String aStartEntityId) {
    startWith = aStartEntityId;
  }

  /**
   * Copies a name of the custom {@link UimaTimer} class
   * 
   * @param aTimerClass -
   *          timer class
   */
  public void setTimerClass(String aTimerClass) {
    timerClass = aTimerClass;
  }

  public long getInputQueueMaxMemoryThreshold() {
    return inputQueueMaxMemoryThreshold;
  }

  public void setInputQueueMaxMemoryThreshold(long aInputQueueMaxMemoryThreshold) {
    inputQueueMaxMemoryThreshold = aInputQueueMaxMemoryThreshold;
  }

  public long getMaxTimeToWait() {
    return maxTimeToWait;
  }

  public void setMaxTimeToWait(long aMaxTimeToWait) {
    maxTimeToWait = aMaxTimeToWait;
  }

  public String getOutputQueueClass() {
    return outputQueueClass;
  }

  public void setOutputQueueClass(String aOutputQueueClass) {
    outputQueueClass = aOutputQueueClass;
  }

}
