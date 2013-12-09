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
 * An object to contains configuration specific to error handling applicable to every CasProcessor.
 * It provides the means of configuring error thresholds and a strategy to deal with them when
 * thresholds are exceeded.
 * 
 * 
 */
public interface CasProcessorErrorRateThreshold extends MetaDataObject {
  /**
   * Sets the max number of errors tolerated by the CPE. If the the threshold is exceeded the CPE
   * will take an action based on defined strategy. Max Error is defined in terms of a quotient,
   * like 3/1000. Where 3 is the actual max error tolerance and 1000 is a sample size. So the above
   * is interpreted as 3 errors per thousand entities processed.
   * 
   * @param aErrorCount -
   *          max tolerated errors for CasProcessor
   */
  public void setMaxErrorCount(int aErrorCount);

  /**
   * Returns max number of errors tolerated by the CPE. If the the threshold is exceeded the CPE
   * will take an action based on defined strategy. Max Error is defined in terms of a quotient,
   * like 3/1000. Where 3 is the actual max error tolerance and 1000 is a sample size. So the above
   * is interpreted as 3 errors per thousand entities processed.
   * 
   * @return - max error count
   */
  public int getMaxErrorCount();

  /**
   * Sets the sample size. Max Error is defined in terms of a quotient, like 3/1000. Where 3 is the
   * actual max error tolerance and 1000 is a sample size. So the above is interpreted as 3 errors
   * per thousand entities processed.
   * 
   * @param aSampleSize the sample size
   */
  public void setMaxErrorSampleSize(int aSampleSize);

  /**
   * Returns sample size. Max Error is defined in terms of a quotient, like 3/1000. Where 3 is the
   * actual max error tolerance and 1000 is a sample size. So the above is interpreted as 3 errors
   * per thousand entities processed.
   * 
   * @return - sample size
   */
  public int getMaxErrorSampleSize();

  /**
   * Sets a strategy for dealing with exceeding error thresholds. The three supported strategies
   * are:
   * <ul>
   * <li> terminate - termines the CPE
   * <li> continue - continue despite the error
   * <li> disable - disable CasProcessor
   * </ul>
   * 
   * @param aAction -
   *          action to take
   */
  public void setAction(String aAction);

  /**
   * Returns strategy for dealing with exceeding error thresholds. The three supported strategies
   * are:
   * <ul>
   * <li> terminate - termines the CPE
   * <li> continue - continue despite the error
   * <li> disable - disable CasProcessor
   * </ul>
   * 
   * @return String - action to take
   */
  public String getAction();
}
