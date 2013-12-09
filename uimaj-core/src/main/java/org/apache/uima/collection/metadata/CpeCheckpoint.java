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

package org.apache.uima.collection.metadata;

import org.apache.uima.resource.metadata.MetaDataObject;

/**
 * An object that holds configuration that is part of the CPE descriptor. It provides the means of
 * configuring CPE checkpoint. The checkpoint contains a name of the file where the recovery
 * information will be stored and a frequency of checkpoints.
 * 
 * 
 */
public interface CpeCheckpoint extends MetaDataObject {
  /**
   * Sets the file where checkpoint information will be stored
   * 
   * @param aCheckpointFilePath -
   *          checkpoint file path
   * @throws CpeDescriptorException tbd
   */
  public void setFilePath(String aCheckpointFilePath) throws CpeDescriptorException;

  /**
   * Returns file where checkpoint information is stored
   * 
   * @return - checkpoint file path
   */
  public String getFilePath();

  /**
   * Sets frequency of checkpoints. Currently only time-based checkpointing is supported.
   * 
   * @param aFrequency -
   *          number of millis between checkpoints
   * @param aTimeBased -
   *          true if checkpoint is based on time
   */
  public void setFrequency(int aFrequency, boolean aTimeBased);

  /**
   * Returns frequency of checkpoints.
   * 
   * @return - number of millis between checkpoints
   */
  public int getFrequency();

  /**
   * Returns true if frequency of checkpoints is time-based
   * 
   * @return true;
   */
  public boolean isTimeBased();

  /**
   * 
   * @param aBatchSize the size of the batch
   */
  public void setBatchSize(int aBatchSize);

  public int getBatchSize();

}
