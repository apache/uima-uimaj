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
 * An object to contains configuration specific to error handling applicable to every CasProcossor.
 * It provides the means of configuring max # of restarts for CasProcessor, error thresholds, and
 * max timeout.
 * 
 * 
 */
public interface CasProcessorErrorHandling extends MetaDataObject {
  /**
   * Sets max number of restarts allowed for a CasProcessor and an action to take by the CPE in case
   * the threshold is reached.
   * 
   * @param aCasPRestarts -
   *          {@link org.apache.uima.collection.metadata.CasProcessorMaxRestarts} instance
   */
  public void setMaxConsecutiveRestarts(CasProcessorMaxRestarts aCasPRestarts);

  /**
   * Returns max number of restarts allowed for a CasProcessor and an action to take by the CPE in
   * case the threshold is reached.
   * 
   * @return {@link org.apache.uima.collection.metadata.CasProcessorMaxRestarts} instance
   */
  public CasProcessorMaxRestarts getMaxConsecutiveRestarts();

  /**
   * Sets max number of errors allowed for a CasProcessor and an action to take by the CPE in case
   * the threshold is reached.
   * 
   * @param aCasPErrorThreshold -
   *          {@link org.apache.uima.collection.metadata.CasProcessorErrorRateThreshold}
   */
  public void setErrorRateThreshold(CasProcessorErrorRateThreshold aCasPErrorThreshold);

  /**
   * Returns max number of restarts allowed for a CasProcessor and an action to take by the CPE in
   * case the threshold is reached.
   * 
   * @return {@link org.apache.uima.collection.metadata.CasProcessorErrorRateThreshold} instance
   */
  public CasProcessorErrorRateThreshold getErrorRateThreshold();

  /**
   * Sets the timeout the CPE will wait for a response from CasProcessor.
   * 
   * @param aTimeout
   *          {@link org.apache.uima.collection.metadata.CasProcessorTimeout}
   */
  public void setTimeout(CasProcessorTimeout aTimeout);

  /**
   * Returns the timeout the CPE will wait for a response from CasProcessor.
   * 
   * @return {@link org.apache.uima.collection.metadata.CasProcessorTimeout} instance
   */
  public CasProcessorTimeout getTimeout();
}
