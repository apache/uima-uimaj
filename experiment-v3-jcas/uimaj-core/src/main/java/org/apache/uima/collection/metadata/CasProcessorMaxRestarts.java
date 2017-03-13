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
 * An object that holds configuration related to handling max restarts for CasProcessors. It
 * provides the means of configuring threshold for max restart tolerance, and defining a strategy to
 * apply when thresholds are exceeded. Used when constucting a CPE descriptor.
 */
public interface CasProcessorMaxRestarts extends MetaDataObject {
  /**
   * Sets max tolerated restarts threshold for CasProcessor
   * 
   * @param aRestartCount -
   *          max restart count
   */
  public void setRestartCount(int aRestartCount);

  /**
   * Returns max restarts threshold fro CasProcessor
   * 
   * @return - restart count
   */
  public int getRestartCount();

  /**
   * Sets a strategy to apply by the CPE when max restart count is exceeded. The three supported
   * strategies are:
   * <ul>
   * <li> terminate - termines the CPE
   * <li> continue - continue despite the error
   * <li> disable - disable CasProcessor
   * </ul>
   * @param aAction -
   *          action to take
   */
  public void setAction(String aAction);

  /**
   * Returns a strategy to apply by the CPE when max restart count is exceeded. The three supported
   * strategies are:
   * <ul>
   * <li> terminate - termines the CPE
   * <li> continue - continue despite the error
   * <li> disable - disable CasProcessor
   * </ul>
   * @return - action as String
   */
  public String getAction();

  /**
   * 
   * @return the wait time between successive retries
   */
  public int getWaitTimeBetweenRetries();

  /**
   * @param i time to wait between successive retries
   */
  public void setWaitTimeBetweenRetries(int i);

}
