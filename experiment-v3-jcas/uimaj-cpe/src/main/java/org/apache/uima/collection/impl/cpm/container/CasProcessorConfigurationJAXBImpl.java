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

package org.apache.uima.collection.impl.cpm.container;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.impl.base_cpm.container.CasProcessorConfiguration;
import org.apache.uima.collection.impl.cpm.Constants;
import org.apache.uima.collection.impl.cpm.container.deployer.JavaApplication;
import org.apache.uima.collection.impl.cpm.container.deployer.NonJavaApplication;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.CpmLocalizedMessage;
import org.apache.uima.collection.impl.cpm.utils.Execute;
import org.apache.uima.collection.impl.cpm.utils.Filter;
import org.apache.uima.collection.metadata.CasProcessorDeploymentParam;
import org.apache.uima.collection.metadata.CasProcessorDeploymentParams;
import org.apache.uima.collection.metadata.CasProcessorErrorHandling;
import org.apache.uima.collection.metadata.CasProcessorErrorRateThreshold;
import org.apache.uima.collection.metadata.CasProcessorMaxRestarts;
import org.apache.uima.collection.metadata.CasProcessorRunInSeperateProcess;
import org.apache.uima.collection.metadata.CasProcessorTimeout;
import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.collection.metadata.CpeCheckpoint;
import org.apache.uima.collection.metadata.CpeComponentDescriptor;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;

/**
 * A wrapper containing Cas Processor configuration. An instance of this class is associated with
 * each Cas Processor.
 * 
 * 
 */
public class CasProcessorConfigurationJAXBImpl implements CasProcessorConfiguration {
  private String actionOnMaxError;

  private String actionOnMaxRestarts;

  private String name;

  private CpeComponentDescriptor descriptor;

  private String deploymentType;

  private String filterString;

  private long errorSampleSize;

  private long timeOut;

  private int errorRate;

  private int maxErrorThreshold;

  private int maxRestartThreshold;

  private int maxRetryThreshold;

  private int batchSize;

  private boolean runInSeparateProcess;

  private boolean isJavaProcess;

  private NonJavaApplication nonJavaApp;

  private JavaApplication javaApp;

  private List deploymentParameters = null;

  private int waitTimeBetweenRestarts = 0;

  private boolean parallelizable = true; // default

  private boolean readOnly = false; // make this explicit
  
  private ResourceManager resourceManager;
  
  /**
   * Initializes instance and copies configuation from cpe descriptor.
   * 
   * @param aCasProcessorConfig -
   *          configuration object containing Cas Processor configuration
   * @param aResourceManager - 
   *          needed to resolve import by name
   * @throws ResourceConfigurationException if descriptor error
   */
  public CasProcessorConfigurationJAXBImpl(CpeCasProcessor aCasProcessorConfig, ResourceManager aResourceManager)
          throws ResourceConfigurationException {
    this.resourceManager = aResourceManager;
    
    if (aCasProcessorConfig == null) {
      throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND, new Object[] {
          "<casProcessor>", "<casProcessors>" }, new Exception(CpmLocalizedMessage
              .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_EXP_bad_cpe_descriptor__WARNING", new Object[] { Thread
                              .currentThread().getName() })));
    }
    name = aCasProcessorConfig.getName();// getAttributeValue("name");
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "initialize",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_max_restart_action__FINEST",
              new Object[] { Thread.currentThread().getName(), name,
                  aCasProcessorConfig.getErrorHandling().getMaxConsecutiveRestarts().getAction() });
    }
    parallelizable = aCasProcessorConfig.getIsParallelizable();
    // readOnly = aCasProcessorConfig.getReadOnly();
    addErrorHandling(aCasProcessorConfig);
    addDeploymentParameters(aCasProcessorConfig);
    addDeploymentType(aCasProcessorConfig);
    addFiltering(aCasProcessorConfig);
    addBatchSize(aCasProcessorConfig);
    addDescriptor(aCasProcessorConfig);
    addRunInSeparateProcess(aCasProcessorConfig);
    addIsJavaProcess(aCasProcessorConfig);
    if (!isJavaProcess()) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_config_non_java_service__FINEST",
                new Object[] { Thread.currentThread().getName(), name });
      }
      nonJavaApp = new NonJavaApplication(this, aCasProcessorConfig); 
    } else {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_config_java_service__FINEST",
                new Object[] { Thread.currentThread().getName(), name });
      }
      javaApp = new JavaApplication(this, aCasProcessorConfig);
    }
  }

  /**
   * Returns how long to wait between resending CAS after failure
   */
  public int getMaxTimeToWaitBetweenRetries() {
    return waitTimeBetweenRestarts;
  }

  /**
   * Returns if the Cas Processor is able to run in parallel
   * 
   * @return - true if the component can run in parallel, false otherwise
   */
  public boolean isParallelizable() {
    return parallelizable;
  }

  /**
   * Returns if the Cas Processor is read only
   * 
   * @return - true if read only, false otherwise
   */
  public boolean readOnly() {
    return readOnly;
  }

  /**
   * Copies runtime information
   * 
   * @param aJaxbCasProcessorConfig -
   *          configuration object containing Cas Processor configuration
   */
  private void addRunInSeparateProcess(CpeCasProcessor aCasProcessorConfig) {
    runInSeparateProcess = aCasProcessorConfig.getRunInSeparateProcess() != null;
  }

  /**
   * Determines if this Cas Processor should run in java jvm.
   * 
   * @param aJaxbCasProcessorConfig -
   *          configuration object containing Cas Processor configuration
   */
  private void addIsJavaProcess(CpeCasProcessor aCasProcessorConfig) {
    isJavaProcess = false;
    CasProcessorRunInSeperateProcess runInProcessType = aCasProcessorConfig
            .getRunInSeparateProcess();
    if (runInProcessType != null && runInProcessType.getExecutable() != null
            && "java".equals(runInProcessType.getExecutable().getExecutable().trim())) {
      isJavaProcess = true;
    }
  }

  /**
   * Copies Error handling settings
   * 
   * @param aJaxbCasProcessorConfig -
   *          configuration object containing Cas Processor configuration
   */
  private void addErrorHandling(CpeCasProcessor aCasProcessorConfig)
          throws ResourceConfigurationException {
    CasProcessorErrorHandling casProcessorErrorHandling = aCasProcessorConfig.getErrorHandling();

    if (casProcessorErrorHandling == null) {
      throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND, new Object[] {
          "errorHandling", "casProcessor" }, new Exception(CpmLocalizedMessage.getLocalizedMessage(
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_missing_xml_element__WARNING",
              new Object[] { Thread.currentThread().getName(), name, "<errorHandling>" })));
    }
    CasProcessorMaxRestarts maxRestarts = casProcessorErrorHandling.getMaxConsecutiveRestarts();
    if (maxRestarts == null) {
      throw new ResourceConfigurationException(
              ResourceConfigurationException.MANDATORY_VALUE_MISSING, new Object[] {
                  "maxConsecutiveRestarts", "CPE" }, new Exception(CpmLocalizedMessage
                      .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                              "UIMA_CPM_EXP_missing_xml_element__WARNING", new Object[] {
                                  Thread.currentThread().getName(), name,
                                  "<maxConsecutiveRestarts>" })));
    }
    maxRetryThreshold = maxRestarts.getRestartCount();
    waitTimeBetweenRestarts = maxRestarts.getWaitTimeBetweenRetries();

    maxRestartThreshold = maxRestarts.getRestartCount();

    if (!validActionOnError(maxRestarts.getAction())) {
      throw new ResourceConfigurationException(
              ResourceConfigurationException.MANDATORY_VALUE_MISSING, new Object[] { "action",
                  "CPE" }, new Exception(CpmLocalizedMessage.getLocalizedMessage(
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_bad_action_string__WARNING",
                      new Object[] { Thread.currentThread().getName(), name,
                          "<maxConsecutiveRestarts>", maxRestarts.getAction() })));
    }
    actionOnMaxRestarts = maxRestarts.getAction();

    // Setup Error rate Threshold in terms of (count)/(sample size) eg. 3/1000
    CasProcessorErrorRateThreshold errorRateThresholdType = casProcessorErrorHandling
            .getErrorRateThreshold();
    if (errorRateThresholdType == null) {
      throw new ResourceConfigurationException(
              ResourceConfigurationException.MANDATORY_VALUE_MISSING, new Object[] {
                  "errorRateThreshold", "CPE" },
              new Exception(CpmLocalizedMessage.getLocalizedMessage(
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_EXP_missing_xml_element__WARNING", new Object[] {
                          Thread.currentThread().getName(), name, "<errorRateThreshold>" })));
    }

    errorRate = errorRateThresholdType.getMaxErrorCount();
    errorSampleSize = errorRateThresholdType.getMaxErrorSampleSize();

    if (!validActionOnError(errorRateThresholdType.getAction())) {
      throw new ResourceConfigurationException(
              ResourceConfigurationException.MANDATORY_VALUE_MISSING, new Object[] { "action",
                  "CPE" }, new Exception(CpmLocalizedMessage.getLocalizedMessage(
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_bad_action_string__WARNING",
                      new Object[] { Thread.currentThread().getName(), name,
                          "<errorRateThreshold>", maxRestarts.getAction() })));

    }
    actionOnMaxError = errorRateThresholdType.getAction();

    CasProcessorTimeout timeoutType = casProcessorErrorHandling.getTimeout();
    timeOut = timeoutType.get();

  }

  /**
   * Copies deployment parameters associated with this Cas Processor These parameters are used to
   * construct appropriate command line for launching the Cas Processor in external process
   * 
   * @param aJaxbCasProcessorConfig -
   *          configuration object containing Cas Processor configuration
   */
  private void addDeploymentParameters(CpeCasProcessor aCasProcessorConfig)
          throws ResourceConfigurationException {
    if (aCasProcessorConfig == null) {
      throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND, new Object[] {
          "<casProcessor>", "<casProcessors>" }, new Exception(CpmLocalizedMessage
              .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_EXP_bad_cpe_descriptor_no_cp__WARNING", new Object[] { Thread
                              .currentThread().getName() })));
    }
    CasProcessorDeploymentParams deployParams = aCasProcessorConfig.getDeploymentParams();

    if (deployParams != null) {
      CasProcessorDeploymentParam[] parameters = deployParams.getAll();
      for (int i = 0; parameters != null && i < parameters.length; i++) {
        try {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "initialize",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_show_cp_deploy_params__FINEST",
                    new Object[] { Thread.currentThread().getName(), name,
                        parameters[i].getParameterName(), parameters[i].getParameterValue() });
          }
          NameValuePair nvp = new NameValuePair_impl();
          nvp.setName(parameters[i].getParameterName());

          // If a value is a path to a file and IS a relative path convert to absolute path using
          // CPE_HOME variable
          String value = CPMUtils.convertToAbsolutePath(System.getProperty("CPM_HOME"),
                  CPEFactory.CPM_HOME, parameters[i].getParameterValue());
          nvp.setValue(value);
          if (deploymentParameters == null) {
            deploymentParameters = new ArrayList();
          }
          deploymentParameters.add(nvp);

        } catch (Exception e) {
          throw new ResourceConfigurationException(e);
        }
      }
    }
    CasProcessorRunInSeperateProcess rsp = null;
    if ((rsp = aCasProcessorConfig.getRunInSeparateProcess()) != null
            && rsp.getExecutable() != null) {
      // List args = rsp.getExecutable().getArg();
      if (deploymentParameters == null) {
        deploymentParameters = new ArrayList();
      }
    }
  }

  /**
   * Copies deployment type associated with this Cas Processor
   * 
   * @param aJaxbCasProcessorConfig - -
   *          configuration object containing Cas Processor configuration
   * @throws ResourceConfigurationException -
   */
  private void addDeploymentType(CpeCasProcessor aCasProcessorConfig)
          throws ResourceConfigurationException {
    if (aCasProcessorConfig == null) {
      throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND, new Object[] {
          "<casProcessor>", "<casProcessors>" }, new Exception(CpmLocalizedMessage
              .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_EXP_bad_cpe_descriptor_no_cp__WARNING", new Object[] { Thread
                              .currentThread().getName() })));
    }
    String deployType = aCasProcessorConfig.getDeployment();
    if (deployType == null || deployType.trim().length() == 0) {
      throw new ResourceConfigurationException(InvalidXMLException.REQUIRED_ATTRIBUTE_MISSING,
              new Object[] { "deployment", "casProcessor" }, new Exception(CpmLocalizedMessage
                      .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                              "UIMA_CPM_EXP_missing_attribute_from_xml_element__WARNING",
                              new Object[] { Thread.currentThread().getName(),
                                  aCasProcessorConfig.getName(), "deployment", "<casProcessor>" })));
    }
    deploymentType = deployType;
  }

  /**
   * Copies filter expression used during processing.
   * 
   * @param aJaxbCasProcessorConfig -
   *          configuration object containing Cas Processor configuration
   */
  private void addFiltering(CpeCasProcessor aCasProcessorConfig)
          throws ResourceConfigurationException {
    if (aCasProcessorConfig == null) {
      throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND, new Object[] {
          "<casProcessor>", "<casProcessors>" }, new Exception(CpmLocalizedMessage
              .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_EXP_bad_cpe_descriptor_no_cp__WARNING", new Object[] { Thread
                              .currentThread().getName() })));
    }
    filterString = aCasProcessorConfig.getCasProcessorFilter();
  }

  /**
   * Copies batch size associated with this Cas Processor
   * 
   * @param aJaxbCasProcessorConfig -
   *          configuration object containing Cas Processor configuration
   */
  private void addBatchSize(CpeCasProcessor aCasProcessorConfig)
          throws ResourceConfigurationException {
    if (aCasProcessorConfig == null) {
      throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND, new Object[] {
          "<casProcessor>", "<casProcessors>" }, new Exception(CpmLocalizedMessage
              .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_EXP_bad_cpe_descriptor_no_cp__WARNING", new Object[] { Thread
                              .currentThread().getName() })));
    }
    CpeCheckpoint checkpoint = aCasProcessorConfig.getCheckpoint();
    if (checkpoint == null) {
      throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND, new Object[] {
          "<checkpoint>", "<casProcessor>" }, new Exception(CpmLocalizedMessage
              .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_EXP_missing_xml_element__WARNING", new Object[] {
                          Thread.currentThread().getName(), aCasProcessorConfig.getName(),
                          "<checkpoint>" })));
    }

    try {
      if (checkpoint.getBatchSize() > 0) {
        batchSize = checkpoint.getBatchSize();
      }
    } catch (NumberFormatException e) {
      throw new ResourceConfigurationException(InvalidXMLException.REQUIRED_ATTRIBUTE_MISSING,
              new Object[] { "batch", "<checkpoint>" }, new Exception(CpmLocalizedMessage
                      .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                              "UIMA_CPM_EXP_missing_attribute_from_xml_element__WARNING",
                              new Object[] { Thread.currentThread().getName(),
                                  aCasProcessorConfig.getName(), "batch", "<checkpoint>" })));
    }

  }

  /**
   * Copies path of the Cas Processor descriptor.
   * 
   * @param aJaxbCasProcessorConfig -
   *          configuration object holding path to the descriptor
   * 
   * @throws ResourceConfigurationException -
   */
  private void addDescriptor(CpeCasProcessor aCasProcessorConfig)
          throws ResourceConfigurationException {
    if (aCasProcessorConfig == null) {
      throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND, new Object[] {
          "<casProcessor>", "<casProcessors>" }, new Exception(CpmLocalizedMessage
              .getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_EXP_bad_cpe_descriptor_no_cp__WARNING", new Object[] { Thread
                              .currentThread().getName() })));
    }
    descriptor = aCasProcessorConfig.getCpeComponentDescriptor();
    
    if (descriptor.getInclude() != null) {
      String descPath = descriptor.getInclude().get();
      if (descPath == null || descPath.trim().length() == 0) {
        throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND, new Object[] {
            "href", "include" }, new Exception(CpmLocalizedMessage.getLocalizedMessage(
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_EXP_missing_attribute_from_xml_element__WARNING", new Object[] {
                    Thread.currentThread().getName(), aCasProcessorConfig.getName(), "href",
                    "<include>" })));
      }
    }

  }

  /**
   * Varifies action String. It must any of the three possible values:
   * <p>
   * <li>continue</li>
   * <li>terminate</li>
   * <li>disable</li>
   * <li>kill-pipeline</li>
   * <p>
   * 
   * @param aActionOnError -
   *          action string to verify
   * 
   * @return - true if action is valid, false otherwise
   */
  private boolean validActionOnError(String aActionOnError) {
    if (Constants.CONTINUE_DESPITE_ERROR.equals(aActionOnError.toLowerCase())
            || Constants.DISABLE_CASPROCESSOR.equals(aActionOnError.toLowerCase())
            || Constants.TERMINATE_CPE.equals(aActionOnError.toLowerCase())
            || Constants.KILL_PROCESSING_PIPELINE.equals(aActionOnError.toLowerCase())) {
      return true;
    }
    return false;
  }

  /**
   * Returns an action as String to identify an action to take in case of excessive Cas Processor
   * errors.
   * <ul>
   * <li>continue</li>
   * <li>terminate</li>
   * <li>disable</li>
   * </ul>
   * 
   * @return - action
   */
  public String getActionOnError() {
    return actionOnMaxError;
  }

  /**
   * Returns an action as String to identify an action to take in case of excessive Cas Processor
   * restarts.
   * <ul>
   * <li>continue</li>
   * <li>terminate</li>
   * <li>disable</li>
   * </ul>
   * 
   * @return - action
   */
  public String getActionOnMaxRestart() {
    return actionOnMaxRestarts;
  }

  public int getErrorRate() {
    return errorRate;
  }

  public long getErrorSampleSize() {
    return errorSampleSize;
  }

  /**
   * Returns max number of tolerated errors
   * 
   * @return - max number of allowed errors
   */
  public int getMaxErrorCount() {
    return maxErrorThreshold;
  }

  /**
   * Returns max number of Cas Processor restarts
   * 
   * @return - max number of restarts
   */
  public int getMaxRestartCount() {
    return maxRestartThreshold;
  }

  /**
   * Returns max number of tries to process each bundle of Cas
   * 
   * @return - max retry count
   */
  public int getMaxRetryCount() {
    return maxRetryThreshold;
  }

  /**
   * Returns Cas Processor name
   * 
   * @return - Name
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the max amount of time the CPE will wait for Cas Processor reponse.
   * 
   * @return - value for timeout
   */
  public long getTimeout() {
    return timeOut;
  }

  /**
   * Returns a list of deployment parameters ssociated with this Cas Processor
   * 
   * @return - deployment paramaters as List
   */
  public List getDeploymentParameters() {
    return deploymentParameters;
  }

  /**
   * Returns deployment type associated with this Cas Processor
   * <ul>
   * <li>integrated</li>
   * <li>local</li>
   * <li>remote</li>
   * </ul>
   * 
   * @return - deployment type
   */
  public String getDeploymentType() {
    return deploymentType;
  }

  /**
   * Returns unparsed filter expression
   * 
   * @return - fliter String
   */
  public String getFilterString() {
    return filterString;
  }

  /**
   * Returns parsed filter expressions as List.
   * 
   */
  public LinkedList getFilter() throws ResourceConfigurationException {
    String filterExpression = null;
    try {
      filterExpression = getFilterString();
      if (filterExpression != null) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "initialize", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_show_cp_filter__FINEST",
                  new Object[] { Thread.currentThread().getName(), name, filterExpression });
        }
        Filter filter = new Filter();
        return filter.parse(filterExpression);
      }
    } catch (Exception e) {
      throw new ResourceConfigurationException(InvalidXMLException.INVALID_ELEMENT_TEXT,
              new Object[] { "filter" }, new Exception(CpmLocalizedMessage.getLocalizedMessage(
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_EXP_missing_xml_element__WARNING", new Object[] {
                          Thread.currentThread().getName(), name, "filer" })));
    }
    return null;
  }

  /**
   * Returns an array of types that should not be sent to Cas Processor. The drop types are defined
   * in the cpe descriptor.
   */
  public String[] getKeysToDrop() throws ResourceConfigurationException {
    try {
      // Now extract the file containing features to be dropped from the CAS when communicating with
      // the
      // annotator running as a remote service
      String dropFeatureFile = getDeploymentParameter("filterKeyMap");
      String[] keysToDrop = null;
      if (dropFeatureFile != null && dropFeatureFile.trim().length() > 0) {
        String descriptorPath = CPMUtils.convertToAbsolutePath(System.getProperty("CPM_HOME"),
                CPEFactory.CPM_HOME, dropFeatureFile);
        try {
          keysToDrop = CPMUtils.getKeys2Drop(descriptorPath); // CPMUtils.scrubThePath(dropFeatureFile));
          return keysToDrop;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } catch (Exception e) {
      throw new ResourceConfigurationException(InvalidXMLException.ELEMENT_NOT_FOUND,
              new Object[] { "parameter" }, new Exception(CpmLocalizedMessage.getLocalizedMessage(
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_EXP_missing_xml_element__WARNING", new Object[] {
                          Thread.currentThread().getName(), name, "parameter" })));
    }
    return null;
  }

  /**
   * Returns configured batch size setup for this Cas Processor
   * 
   * @return - batch size
   */
  public int getBatchSize() {
    return batchSize;
  }

  /**
   * Returns descriptor associated with this Cas Processor
   * 
   * @return object that identifies location of descriptor
   * 
   * @throws ResourceConfigurationException if an import could not be resolved
   */
  public URL getDescriptorUrl() throws ResourceConfigurationException {
    return descriptor.findAbsoluteUrl(resourceManager);
  }
  
  /**
   * Returns a value for a given deployment parameter
   * 
   * @param aDeployParameter - name of the parameter
   * @return - value for parameter name
   */
  public String getDeploymentParameter(String aDeployParameter) {
    String desc = null;
    if (aDeployParameter == null || deploymentParameters == null) {
      return null;
    }
    for (int i = 0; i < deploymentParameters.size(); i++) {
      NameValuePair nvp = (NameValuePair) deploymentParameters.get(i);
      if (aDeployParameter.equals(nvp.getName().trim())) {
        desc = (String) nvp.getValue();
        break;
      }
    }
    return desc;
  }

  /**
   * Returns true if this Cas Processor will run in its own process
   * 
   * @return - true if running in seperate process
   */
  public boolean runInSeparateProcess() {
    return runInSeparateProcess;
  }

  /**
   * Returns true it the Cas Processor is written in java and will be run with java jvm.
   * 
   */
  public boolean isJavaProcess() {
    return isJavaProcess;
  }

  /**
   * Returns executable section of the Cas Processor configuration. It contains the name of
   * executable program to be used when launching a seperate process with Cas Processor running as
   * vinci service.
   * 
   * @return - Execute object
   */
  public Execute getExecSpec() {
    if (!isJavaProcess()) {
      return nonJavaApp.getExecSpec();
    } else {
      return javaApp.getExecSpec();
    }
  }

}
