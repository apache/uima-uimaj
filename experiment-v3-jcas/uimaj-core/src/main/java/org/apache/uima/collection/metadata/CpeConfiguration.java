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
 * An object that holds configuration that is part of the CPE descriptor. Provides the means of
 * defining and obtaining CPE specific configuration that includes:
 * <ul>
 * <li> Number of entities to process
 * <li> Checkpoint file and frequency of checkpoints
 * <li> Plug-in timer class
 * </ul>
 * 
 */
public interface CpeConfiguration extends MetaDataObject {
  /**
   * Sets CPE deployment mode as "immediate", "vinceService", "interactive". The CPE does not
   * directly use this information, instead it is up to the application using the CPE to ingest this
   * and handle it as appropriate.
   * 
   * This element is used by an application that uses the CPE. The CPE is an embeddable component,
   * part of a larger application.
   * <ul>
   * <li> "immediate" mode: this is the way the CPE is typically run. In this mode the application
   * is initializing the CPE and starts in without user interaction. The CPE runs to completion in
   * this case. </li>
   * <li>"interactive" mode: The application interacts with the CPE via an API to stop, pause, or
   * resume the CPE. </li>
   * <li>"vinciService" mode: used to indicate control of CPE in terms of stop, pause, resume, and
   * query for performance info, from a remote console. </li>
   * </ul>
   * 
   * @param aDeploy -
   *          deployment mode of the CPE
   * @throws CpeDescriptorException tbd
   */
  public void setDeployment(String aDeploy) throws CpeDescriptorException;

  /**
   * Returns CPE deployment mode as "immediate", "vinceService", "interactive".
   * 
   * @see #setDeployment(String) for a description of these modes.
   * 
   * @return - deployment mode
   */
  public String getDeployment();

  /**
   * Sets number of entities to process by the CPE.
   * 
   * @param aNumToProcess -
   *          number of entities to process (-1 - for ALL)
   * @throws CpeDescriptorException tbd
   */
  public void setNumToProcess(int aNumToProcess) throws CpeDescriptorException;

  /**
   * Returns number of entities to process
   * 
   * @return - number of entities to process (-1 - for ALL)
   */
  public int getNumToProcess();

  /**
   * Sets id of the first entity the CPE will beging processing. Usefull when starting the CPE from
   * a known point.
   * 
   * @param aEntityId -
   *          id of first entity
   */
  public void setStartingEntityId(String aEntityId);

  /**
   * Returns id of the first entity the CPE will beging processing. Usefull when starting the CPE
   * from a known point.
   * 
   * @return - id of first entity
   */
  public String getStartingEntityId();

  /**
   * Sets Checkpoint object containing checkpoint file and frequency of checkpoints.
   * 
   * @param aCheckpoint -
   *          checkpoint object
   * @throws CpeDescriptorException tbd
   */
  public void setCheckpoint(CpeCheckpoint aCheckpoint) throws CpeDescriptorException;

  /**
   * Returns Checkpoint object containing checkpoint file and frequency of checkpoints.
   * 
   * @return {@link org.apache.uima.collection.metadata.CpeCheckpoint}
   */
  public CpeCheckpoint getCheckpoint();

  /**
   * Removes checkpoint object
   * 
   */
  public void removeCheckpoint();

  /**
   * Sets the timer class implementing UimeTimer interface. This timer will be used to time CPE
   * events. By default, the CPE uses System.currentTimeMillis() to obtain time.
   * 
   * @param aTimer -
   *          timer class
   */
  public void setCpeTimer(CpeTimer aTimer);

  /**
   * Returns the timer class implementing UimeTimer interface. This timer will be used to time CPE
   * events. By default, the CPE uses System.currentTimeMillis() to obtain time.
   * 
   * @return - timer class
   */
  public CpeTimer getCpeTimer();

  public String getTimerImpl();

  public void removeCpeTimer();

  public OutputQueue getOutputQueue();

  public int getMaxTimeToWait();

}
