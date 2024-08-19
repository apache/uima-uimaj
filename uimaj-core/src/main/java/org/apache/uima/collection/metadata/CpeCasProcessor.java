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
 * configuring behavior that is common to all CasProcessor types.
 */
public interface CpeCasProcessor extends MetaDataObject {
  /**
   * Sets CasProcessor's name
   * 
   * @param aName
   *          - CasProcessor name
   * @throws CpeDescriptorException
   *           tbd
   */
  void setName(String aName) throws CpeDescriptorException;

  /**
   * Returns CasProcessor's name
   * 
   * @return - name
   */
  String getName();

  void setSOFA(String aSOFA) throws CpeDescriptorException;

  String getSOFA();

  /**
   * Sets CasProcessor descriptor path.
   * 
   * @param aDescriptor
   *          - descriptor path
   * @throws CpeDescriptorException
   *           tbd
   * @deprecated Doesn't support the new import syntax. Use setCpeComponentDescriptor() instead.
   */
  @Deprecated(since = "3.3.0")
  void setDescriptor(String aDescriptor) throws CpeDescriptorException;

  /**
   * Returns CasProcessor descriptor.
   * 
   * @return descriptor
   * @deprecated Doesn't support the new import syntax. Use
   *             getCpeComponentDescriptor().findAbsoluteUrl() instead.
   */
  @Deprecated(since = "3.3.0")
  String getDescriptor();

  /**
   * Returns the {@link CpeComponentDescriptor} instance associated with this Cas Processor. That
   * object contains a path to the component descriptor.
   * 
   * @return {@link CpeComponentDescriptor} instance
   */
  CpeComponentDescriptor getCpeComponentDescriptor();

  /**
   * Sets the {@link CpeComponentDescriptor} instance associated with this Cas Processor. That
   * object contains a path to the component descriptor.
   * 
   * @param aDescriptor
   *          {@link CpeComponentDescriptor} instance
   * @throws CpeDescriptorException
   *           tbd
   */
  void setCpeComponentDescriptor(CpeComponentDescriptor aDescriptor) throws CpeDescriptorException;

  /**
   * Returns CasProcessor deployment type. Three types are currently supported:
   * <ul>
   * <li>integrated
   * <li>remote
   * <li>local
   * </ul>
   * 
   * @return - deployment mode
   */
  String getDeployment();

  /**
   * Sets CasProcessor filter expression used by the CPE to route CASs. A CasProcessor can be
   * configured in the CPE Descriptor to use filtering using an SQL-like WHERE clause: where
   * featurespec [ and featurespec2 ...]. The featurespec can be one of these four forms and
   * meanings:
   * <ul>
   * <li>Feature Process CAS if the Feature is present (e.g. where Person)
   * <li>Feature! Process CAS if the Feature is not present (e.g. where Person!)
   * <li>Feature=value Process CAS if the Feature has this value( e.g. where Person=Bush)
   * <li>Feature!=value Process CAS if the Feature does not have this value (e.g. where
   * Person!=Bush)
   * </ul>
   * The featurespecs are implicitly connected with and operators and precedence rules are currently
   * not supported.
   * 
   * @param aFilterExpression
   *          - filter
   * @throws CpeDescriptorException
   *           tbd
   */
  void setCasProcessorFilter(String aFilterExpression) throws CpeDescriptorException;

  /**
   * Returns CasProcessor filter expression. A CasProcessor can be configured in the CPE Descriptor
   * to use filtering using an SQL-like WHERE clause: where featurespec [ and featurespec2 ...]. The
   * featurespec can be one of these four forms and meanings:
   * <ul>
   * <li>Feature Process CAS if the Feature is present (e.g. where Person)
   * <li>Feature! Process CAS if the Feature is not present (e.g. where Person!)
   * <li>Feature=value Process CAS if the Feature has this value( e.g. where Person=Bush)
   * <li>Feature!=value Process CAS if the Feature does not have this value (e.g. where
   * Person!=Bush)
   * </ul>
   * The featurespecs are implicitly connected with and operators and precedence rules are currently
   * not supported.
   * 
   * @return - filter expression
   */
  String getCasProcessorFilter();

  /**
   * Sets CasProcessor's batch size.
   * 
   * @param aBatchSize
   *          - size of CasProcessor batch
   */
  void setBatchSize(int aBatchSize);

  /**
   * Returns CasProcessor batch size
   * 
   * @return - batch size
   */
  int getBatchSize();

  /**
   * Returns {@link org.apache.uima.collection.metadata.CasProcessorErrorHandling} object containing
   * strategies to deal with errors that may occur during processing. This object provides the means
   * of modifying error thresholds and actions to take when error thresholds are exceeded.
   * 
   * @return {@link org.apache.uima.collection.metadata.CasProcessorErrorHandling}
   */
  CasProcessorErrorHandling getErrorHandling();

  /**
   * Returns {@link org.apache.uima.collection.metadata.CpeCheckpoint} object containing checkpoint
   * configuration ( checkpoint file, frequency of checkpoints )
   * 
   * @return {@link org.apache.uima.collection.metadata.CasProcessorErrorHandling}
   */
  CpeCheckpoint getCheckpoint();

  /**
   * Returns {@link org.apache.uima.collection.metadata.CasProcessorDeploymentParams} object
   * containing deployment parameters used for launching CasProcessor. This object provides the
   * means of adding, getting, and removing
   * {@link org.apache.uima.collection.metadata.CasProcessorDeploymentParam} objects.
   * 
   * @return - object containing list of
   *         {@link org.apache.uima.collection.metadata.CasProcessorDeploymentParam}
   */
  CasProcessorDeploymentParams getDeploymentParams();

  /**
   * Sets the max number of errors tolerated by the CPE. If the the threshold is exceeded the CPE
   * will take an action based on defined strategy. Max Error is defined in terms of a quotient,
   * like 3/1000. Where 3 is the actual max error tolerance and 1000 is a sample size. So the above
   * is interpreted as 3 errors per thousand entities processed.
   * 
   * @param aErrorCount
   *          - max error threshold
   */
  void setMaxErrorCount(int aErrorCount);

  /**
   * Returns the max number of errors tolerated by the CPE. If the the threshold is exceeded the CPE
   * will take an action based on defined strategy. Max Error is defined in terms of a quotient,
   * like 3/1000. Where 3 is the actual max error tolerance and 1000 is a sample size. So the above
   * is interpreted as 3 errors per thousand entities processed.
   * 
   * @return - max error threshold
   */
  int getMaxErrorCount();

  /**
   * Sets the sample size. Max Error is defined in terms of a quotient, like 3/1000. Where 3 is the
   * actual max error tolerance and 1000 is a sample size. So the above is interpreted as 3 errors
   * per thousand entities processed.
   * 
   * @param aErrorSampleSize
   *          the sample size
   */
  void setMaxErrorSampleSize(int aErrorSampleSize);

  /**
   * Returns sample size. Max Error is defined in terms of a quotient, like 3/1000. Where 3 is the
   * actual max error tolerance and 1000 is a sample size. So the above is interpreted as 3 errors
   * per thousand entities processed.
   * 
   * @return - sample size
   */
  int getMaxErrorSampleSize();

  /**
   * Sets a strategy for dealing with exceeding error thresholds. The three supported strategies
   * are:
   * <ul>
   * <li>terminate - terminates the CPE
   * <li>continue - continue despite the error
   * <li>disable - disable CasProcessor
   * </ul>
   * 
   * @param aAction
   *          - action to take
   */
  void setActionOnMaxError(String aAction);

  /**
   * Returns strategy for dealing with exceeding error thresholds. The three supported strategies
   * are:
   * <ul>
   * <li>terminate - terminates the CPE
   * <li>continue - continue despite the error
   * <li>disable - disable CasProcessor
   * </ul>
   * 
   * @return - action to take
   */
  String getActionOnMaxError();

  /**
   * Sets max tolerated restarts threshold for CasProcessor
   * 
   * @param aErrorCount
   *          - max restart count
   */
  void setMaxRestartCount(int aErrorCount);

  /**
   * @return max restarts threshold from CasProcessor
   */
  int getMaxRestartCount();

  /**
   * Sets strategy for dealing with exceeding error thresholds. The three supported strategies are:
   * <ul>
   * <li>terminate - terminates the CPE
   * <li>continue - continue despite the error
   * <li>disable - disable CasProcessor
   * </ul>
   * 
   * @param aAction
   *          - action to take
   */
  void setActionOnMaxRestart(String aAction);

  /**
   * Returns strategy for dealing with exceeding error thresholds. The three supported strategies
   * are:
   * <ul>
   * <li>terminate - terminates the CPE
   * <li>continue - continue despite the error
   * <li>disable - disable CasProcessor
   * </ul>
   * 
   * @return the action to take when the error threshold is exceeded
   */
  String getActionOnMaxRestart();

  /**
   * Sets the timeout value. The amount of time the CPE will wait for response from CasProcessor
   * 
   * @param aTimeoutValue
   *          - timeout value
   */
  void setTimeout(int aTimeoutValue);

  /**
   * Returns the timeout value. The amount of time the CPE will wait for response from CasProcessor
   * 
   * @return - timeout value
   */
  int getTimeout();

  /**
   * Adds deployment parameter used by the CPE when launching CasProcessor
   * 
   * @param aParamName
   *          - param name
   * @param aParamValue
   *          - param value
   * @throws CpeDescriptorException
   *           tbd
   */
  void addDeployParam(String aParamName, String aParamValue) throws CpeDescriptorException;

  /**
   * Returns {@link org.apache.uima.collection.metadata.CasProcessorConfigurationParameterSettings}
   * object containing overrides to parameter settings for this CAS Processor.
   * 
   * @return - object containing parameter setting overrides
   */
  CasProcessorConfigurationParameterSettings getConfigurationParameterSettings();

  /**
   * Sets the {@link org.apache.uima.collection.metadata.CasProcessorConfigurationParameterSettings}
   * object containing overrides to parameter settings for this CAS Processor.
   * 
   * @param aSettings
   *          object containing parameter setting overrides
   * @throws CpeDescriptorException
   *           tbd
   */
  void setConfigurationParameterSettings(CasProcessorConfigurationParameterSettings aSettings)
          throws CpeDescriptorException;

  CpeSofaMappings getSofaNameMappings();

  /**
   * @param mappings
   *          -
   */
  void setSofaNameMappings(CpeSofaMappings mappings);

  CasProcessorRunInSeperateProcess getRunInSeparateProcess();

  void setIsParallelizable(boolean isP);

  boolean getIsParallelizable();
}
