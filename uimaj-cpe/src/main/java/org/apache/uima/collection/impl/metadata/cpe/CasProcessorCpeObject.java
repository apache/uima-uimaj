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

package org.apache.uima.collection.impl.metadata.cpe;

import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.CpmLocalizedMessage;
import org.apache.uima.collection.impl.metadata.CpeDefaultValues;
import org.apache.uima.collection.metadata.CasProcessorConfigurationParameterSettings;
import org.apache.uima.collection.metadata.CasProcessorDeploymentParam;
import org.apache.uima.collection.metadata.CasProcessorDeploymentParams;
import org.apache.uima.collection.metadata.CasProcessorErrorHandling;
import org.apache.uima.collection.metadata.CasProcessorErrorRateThreshold;
import org.apache.uima.collection.metadata.CasProcessorFilter;
import org.apache.uima.collection.metadata.CasProcessorMaxRestarts;
import org.apache.uima.collection.metadata.CasProcessorRunInSeperateProcess;
import org.apache.uima.collection.metadata.CasProcessorTimeout;
import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.collection.metadata.CpeCheckpoint;
import org.apache.uima.collection.metadata.CpeComponentDescriptor;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.collection.metadata.CpeSofaMappings;
import org.apache.uima.collection.metadata.NameValuePair;
import org.apache.uima.internal.util.StringUtils;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.impl.ConfigurationParameterSettings_impl;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;
import org.w3c.dom.Element;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Base class for all CpeCasProcessor objects in the reference implementation. Provides support for
 * getting and setting common configuration settings shared by all CpeCasProcessor objects
 * 
 * 
 */
public class CasProcessorCpeObject extends MetaDataObject_impl implements CpeCasProcessor {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -2424851648116984900L;

  /** The Constant actionArray. */
  private static final String[] actionArray = { "continue", "terminate", "disable",
      "kill-pipeline" };

  /** The Constant deployArray. */
  private static final String[] deployArray = { "integrated", "remote", "local" };

  /** The descriptor. */
  private CpeComponentDescriptor descriptor;

  /** The parameters. */
  private Parameter[] parameters;

  /** The run in separate process. */
  private CasProcessorRunInSeperateProcess runInSeparateProcess;

  /** The filter. */
  private CasProcessorFilter filter;

  /** The error handling. */
  private CasProcessorErrorHandling errorHandling;

  /** The checkpoint. */
  private CpeCheckpoint checkpoint;

  /** The deployment. */
  private String deployment;

  /** The name. */
  private String name;

  /** The deployment parameters. */
  private CasProcessorDeploymentParams deploymentParameters;

  /** The configuration parameter settings. */
  private CasProcessorConfigurationParameterSettings configurationParameterSettings;

  /** The parameter settings. */
  private ConfigurationParameterSettings parameterSettings;

  /** The sofa name mappings. */
  private CpeSofaMappings sofaNameMappings;

  /** The is parallelizable. */
  private boolean isParallelizable;

  /**
   * Associates deployment type with for this CasProcessor. Three types are currently supported:
   * <ul>
   * <li>integrated - CasProcessor is collocated with the CPM
   * <li>local - CasProcessor runs on the same machine as the CPM however in a different process
   * <li>remote - CasProcessor runs on remote machine
   * </ul>
   * 
   * @param aDeployMode
   *          - String identifying deployment type
   * @throws CpeDescriptorException
   *           - if invalid deployment type is provided
   */
  public void setDeployment(String aDeployMode) throws CpeDescriptorException {
    deployment = aDeployMode;
    for (int i = 0; i < deployArray.length; i++) {
      if (deployArray[i].equalsIgnoreCase(aDeployMode)) {
        deployment = aDeployMode;
        return;
      }
    }
    throw new CpeDescriptorException(CpmLocalizedMessage.getLocalizedMessage(
            CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_Exception_invalid_deployment__WARNING",
            new Object[] { Thread.currentThread().getName(), "", aDeployMode }));
  }

  /**
   * Returns deployment type associated with this CasProcessor.
   * 
   * @return String - deployment type
   */
  @Override
  public String getDeployment() {
    return deployment;
  }

  /**
   * Associates a given descriptor path with this CasProcessor.
   *
   * @param aDescriptorPath
   *          - path to the descriptor
   * @throws CpeDescriptorException
   *           tbd
   */
  @Override
  public void setDescriptor(String aDescriptorPath) throws CpeDescriptorException {
    if (descriptor == null) {
      CpeComponentDescriptor comp_desc = CpeDescriptorFactory
              .produceComponentDescriptor(aDescriptorPath);
      this.setCpeComponentDescriptor(comp_desc);
    } else {
      descriptor.getInclude().set(aDescriptorPath);
    }
  }

  /**
   * Returns a descriptor path associated with this CasProcessor.
   *
   * @return String - descriptor path
   * @deprecated Doesn't support the new import syntax. Use
   *             getCpeComponentDescriptor().findAbsoluteUrl() instead.
   */

  @Override
  @Deprecated
  public String getDescriptor() {
    if (descriptor != null && descriptor.getInclude() != null) {
      return descriptor.getInclude().get();
    }
    return null;
  }

  /**
   * Returns the {@link CpeComponentDescriptor} instance associated with this Cas Processor. That
   * object contains a path to the component descriptor.
   * 
   * @return {@link CpeComponentDescriptor} instance
   */
  @Override
  public CpeComponentDescriptor getCpeComponentDescriptor() {
    return descriptor;
  }

  /**
   * Associates a filter string with this CasProcessor. A filter provides a mechanism that
   * facilitates efficient routing of Cas's to the CasProcessor.
   * 
   * @param aFilterExpression
   *          - String containing a filter
   */
  @Override
  public void setCasProcessorFilter(String aFilterExpression) {
    if (filter == null) {
      filter = CpeDescriptorFactory.produceCasProcessorFilter("");
    }
    filter.setFilterString(aFilterExpression);
  }

  /**
   * Returns filter string associated with this CasProcessor.
   *
   * @return String - a filter string
   */
  @Override
  public String getCasProcessorFilter() {

    if (getFilter() == null || getFilter().getFilterString() == null) {
      return "";
    }
    String filterString = getFilter().getFilterString();
    if (filter != null) {
      if (filterString
              .indexOf(org.apache.uima.collection.impl.cpm.Constants.SHORT_COLON_TERM) > -1) {
        filterString = StringUtils.replaceAll(filterString,
                org.apache.uima.collection.impl.cpm.Constants.SHORT_COLON_TERM,
                org.apache.uima.collection.impl.cpm.Constants.LONG_COLON_TERM);
      }
      if (filterString.indexOf(org.apache.uima.collection.impl.cpm.Constants.LONG_DASH_TERM) > -1) {
        filterString = StringUtils.replaceAll(filterString,
                org.apache.uima.collection.impl.cpm.Constants.SHORT_DASH_TERM,
                org.apache.uima.collection.impl.cpm.Constants.LONG_DASH_TERM);
      }
    }
    return filterString;
  }

  /**
   * Adds default configuration shared by CasProcessors.
   *
   * @throws CpeDescriptorException
   *           tbd
   */
  protected void addDefaults() throws CpeDescriptorException {
    if (getCasProcessorFilter() == null) {
      CasProcessorFilter filter = CpeDescriptorFactory.produceCasProcessorFilter("");
      setCasProcessorFilter(filter);
    }
    if (getErrorHandling() == null) {
      CasProcessorErrorHandling errorHandling = CpeDescriptorFactory
              .produceCasProcessorErrorHandling();
      CasProcessorMaxRestarts maxRestart = CpeDescriptorFactory.produceCasProcessorMaxRestarts();
      maxRestart.setRestartCount(30);
      maxRestart.setAction("terminate");
      errorHandling.setMaxConsecutiveRestarts(maxRestart);
      CasProcessorTimeout timeout = CpeDescriptorFactory.produceCasProcessorTimeout();
      timeout.set(100000);
      errorHandling.setTimeout(timeout);
      CasProcessorErrorRateThreshold errorThreshold = CpeDescriptorFactory
              .produceCasProcessorErrorRateThreshold();
      errorThreshold.setMaxErrorCount(100);
      errorThreshold.setMaxErrorSampleSize(1000);
      errorThreshold.setAction("terminate");
      errorHandling.setErrorRateThreshold(errorThreshold);
      setErrorHandling(errorHandling);
    }
    if (getCheckpoint() == null) {
      CpeCheckpoint checkpoint = CpeDescriptorFactory.produceCpeCheckpoint();
      checkpoint.setBatchSize(1);
      checkpoint.setFilePath(CpeDefaultValues.PROCESSOR_CHECKPOINT_FILE);
      checkpoint.setFrequency(1000, true);
      setCheckpoint(checkpoint);
    }
  }

  /**
   * Associates a batch size with this CasProcessor.
   * 
   * @param aBatchSize
   *          - batch size of this CasProcessor
   */
  @Override
  public void setBatchSize(int aBatchSize) {
    checkpoint.setBatchSize(aBatchSize);
  }

  /**
   * Returns a batch size associated with this CasProcessor.
   *
   * @return - batch size as int, 0 if not defined.
   */
  @Override
  public int getBatchSize() {
    return checkpoint.getBatchSize();
  }

  /**
   * Deletes a given param from a param list if it exists. Returns a position in the current Param
   * List for a given 'aParamName'.
   *
   * @param aParamName
   *          - name of the param to find.
   * @return - position in the list as int, -1 if not found
   * @throws CpeDescriptorException
   *           the cpe descriptor exception
   */
  private int deleteParam(String aParamName) throws CpeDescriptorException {
    CasProcessorDeploymentParams depParams = getDeploymentParameters();
    if (depParams != null) {
      CasProcessorDeploymentParam[] paramArray = depParams.getAll();
      for (int i = 0; paramArray != null && i < paramArray.length; i++) {
        if (aParamName.equals(paramArray[i].getParameterName())) {
          depParams.remove(paramArray[i]);
          return i;
        }
      }
    }
    return -1;
  }

  /**
   * Adds a given deployment param to the param list. If a param with a given name exists in the
   * list its value will be over-written.
   * 
   * @param aParamName
   *          - name of the new parameter
   * @param aParamValue
   *          - value of the new parameter
   * 
   * @throws CpeDescriptorException
   *           tbd
   */
  @Override
  public void addDeployParam(String aParamName, String aParamValue) throws CpeDescriptorException {
    boolean found = false;

    CasProcessorDeploymentParam[] params = deploymentParameters.getAll();
    for (int i = 0; params != null && i < params.length; i++) {
      if (aParamName.equals(params[i].getParameterName())) {
        params[i].setParameterValue(aParamValue);
        found = true;
        break;
      }
    }
    if (!found) {
      deploymentParameters
              .add(new CasProcessorDeploymentParamImpl(aParamName, aParamValue, "string"));
    }
  }

  /**
   * Sets the deployment params.
   *
   * @param aParams
   *          the new deployment params
   * @throws CpeDescriptorException
   *           tbd
   */
  protected void setDeploymentParams(CasProcessorDeploymentParams aParams)
          throws CpeDescriptorException {
    deploymentParameters = aParams;
  }

  /**
   * Returns deployment parameters for this CasProcessor.
   * 
   * @return the deployment parameters
   * @see org.apache.uima.collection.metadata.CasProcessorDeploymentParams instance
   */
  @Override
  public CasProcessorDeploymentParams getDeploymentParams() {
    return deploymentParameters;
  }

  /**
   * Associates a name with this CasProcessor.
   *
   * @param aName
   *          - name as string
   * @throws CpeDescriptorException
   *           tbd
   */
  @Override
  public void setName(String aName) throws CpeDescriptorException {

    if (aName == null || aName.trim().length() == 0) {
      throw new CpeDescriptorException(CpmLocalizedMessage.getLocalizedMessage(
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_EXP_invalid_reference__WARNING",
              new Object[] { Thread.currentThread().getName(), "casProcessor name=NULL" }));
    }
    name = aName;
  }

  /**
   * Returns a name of this CasProcessor.
   *
   * @return - CasProcessor name as string, null if name undefined
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Sets the sofa.
   *
   * @param aSoFa
   *          the new sofa
   * @throws CpeDescriptorException
   *           tbd
   * @deprecated
   */
  @Override
  @Deprecated
  public void setSOFA(String aSoFa) throws CpeDescriptorException {
    // if (casProcessor != null )
    // {
    // casProcessor.setContentTag(aSoFa);
    // }
  }

  /**
   * Gets the sofa.
   *
   * @return the sofa
   * @see org.apache.uima.collection.metadata.CpeCasProcessor#getSOFA()
   * @deprecated (non-Javadoc)
   */
  @Override
  @Deprecated
  public String getSOFA() {

    // if (casProcessor != null )
    // {
    // return casProcessor.getContentTag();
    // }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCasProcessor#setDescriptorPath(java.lang.String)
   */
  @Override
  public void setCpeComponentDescriptor(CpeComponentDescriptor aDescriptor)
          throws CpeDescriptorException {
    descriptor = aDescriptor;
  }

  /**
   * Sets the cas processor filter.
   *
   * @param aFilter
   *          the new cas processor filter
   * @throws CpeDescriptorException
   *           the cpe descriptor exception
   */
  public void setCasProcessorFilter(CasProcessorFilter aFilter) throws CpeDescriptorException {
    filter = aFilter;
  }

  /**
   * Sets the error handling.
   *
   * @param aErrorHandling
   *          the new error handling
   * @throws CpeDescriptorException
   *           the cpe descriptor exception
   */
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.collection.metadata.CpeCasProcessor#setErrorHandling(org.apache.uima.collection
   * .metadata.CasProcessorErrorHandling)
   */
  public void setErrorHandling(CasProcessorErrorHandling aErrorHandling)
          throws CpeDescriptorException {
    errorHandling = aErrorHandling;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCasProcessor#getErrorHandling()
   */
  @Override
  public CasProcessorErrorHandling getErrorHandling() {
    return errorHandling;
  }

  /**
   * Associates a threshold for maximum error tolerance. Errors are defined in terms as a quotient.
   * For example, 4/1000 which means max 4 errors per thousand (sample size) CAS's processed. The
   * sample size is defined seperately.
   * 
   * 
   * @param aErrorCount
   *          - max error tolerance
   */
  @Override
  public void setMaxErrorCount(int aErrorCount) {
    CasProcessorErrorHandling eh = getErrorHandling();
    if (eh != null && eh.getErrorRateThreshold() != null) {
      eh.getErrorRateThreshold().setMaxErrorCount(aErrorCount);
    }
  }

  /**
   * Associates a threshold for maximum error tolerance. Errors are defined in terms as a quotient.
   * For example, 4/1000 which means max 4 errors per thousand (sample size) CAS's processed. The
   * sample size is defined seperately.
   * 
   * 
   * @return max error tolerance
   */
  @Override
  public int getMaxErrorCount() {

    CasProcessorErrorHandling eh = getErrorHandling();
    if (eh != null && eh.getErrorRateThreshold() != null) {
      return eh.getErrorRateThreshold().getMaxErrorCount();
    }

    return 0;
  }

  /**
   * Associates a threshold for maximum error tolerance. Errors are defined in terms as a quotient.
   * For example, 4/1000 which means max 4 errors per thousand (sample size) CAS's processed. The
   * sample size is defined seperately.
   * 
   * 
   * @param aErrorSampleSize
   *          - max error tolerance
   */
  @Override
  public void setMaxErrorSampleSize(int aErrorSampleSize) {

    CasProcessorErrorHandling eh = getErrorHandling();
    if (eh != null && eh.getErrorRateThreshold() != null) {
      eh.getErrorRateThreshold().setMaxErrorSampleSize(aErrorSampleSize);
    }

  }

  /**
   * Returns error sample size. The value is used to determine the max error tolerance for this
   * CasProcessor. Error thresholds are defines as quotients. Error Count / Sample Size for example,
   * 3/1000, which means 3 errors per thousand.
   * 
   * @return - the sample size
   */
  @Override
  public int getMaxErrorSampleSize() {
    CasProcessorErrorHandling eh = getErrorHandling();
    if (eh != null && eh.getErrorRateThreshold() != null) {
      return eh.getErrorRateThreshold().getMaxErrorSampleSize();
    }

    return 0;
  }

  /**
   * Check if the action String is valid.
   *
   * @param aAction
   *          - action as string
   * @return - true is valid, false otherwise
   */
  private boolean validAction(String aAction) {
    for (int i = 0; i < actionArray.length; i++) {
      if (actionArray[i].equalsIgnoreCase(aAction)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Associates action in the event the errors exceed max tolerance. In such case, the action
   * determines appropriate strategy ( terminate, continue, disable).
   * 
   * @param aAction
   *          - action string
   */
  @Override
  public void setActionOnMaxError(String aAction) {
    CasProcessorErrorHandling eh = getErrorHandling();
    if (eh != null && eh.getErrorRateThreshold() != null) {
      eh.getErrorRateThreshold().setAction(aAction);
    }
  }

  /**
   * Return action associated with CasProcessor error tolerance.
   * 
   * @return - action as string ( terminate, continue, disable), null when not defined
   */
  @Override
  public String getActionOnMaxError() {
    CasProcessorErrorHandling eh = getErrorHandling();
    if (eh != null && eh.getErrorRateThreshold() != null) {
      return eh.getErrorRateThreshold().getAction();
    }
    return null;
  }

  /**
   * Associates action in the event CasProcessor restarts exceed max tolerance. In such case, the
   * action determines appropriate strategy ( terminate, continue, disable).
   * 
   * @param aAction
   *          - action string
   */
  @Override
  public void setActionOnMaxRestart(String aAction) {
    CasProcessorErrorHandling eh = getErrorHandling();
    if (eh != null && eh.getMaxConsecutiveRestarts() != null) {
      eh.getMaxConsecutiveRestarts().setAction(aAction);
    }
  }

  /**
   * Return action associated with CasProcessor restart tolerance.
   * 
   * @return - action as string ( terminate, continue, disable), null when not defined
   */
  @Override
  public String getActionOnMaxRestart() {
    CasProcessorErrorHandling eh = getErrorHandling();
    if (eh != null && eh.getMaxConsecutiveRestarts() != null) {
      return eh.getMaxConsecutiveRestarts().getAction();
    }
    return null;
  }

  /**
   * Associates max tolerance for CasProcessor restarts.
   * 
   * @param aRestartCount
   *          - max number of restarts
   */
  @Override
  public void setMaxRestartCount(int aRestartCount) {
    CasProcessorErrorHandling eh = getErrorHandling();
    if (eh != null && eh.getMaxConsecutiveRestarts() != null) {
      eh.getMaxConsecutiveRestarts().setRestartCount(aRestartCount);
    }
  }

  /**
   * Returns max restart tolerance for this CasProcessor.
   * 
   * @return - restart count as int, 0 if not defined
   */
  @Override
  public int getMaxRestartCount() {
    CasProcessorErrorHandling eh = getErrorHandling();
    if (eh != null && eh.getMaxConsecutiveRestarts() != null) {
      return eh.getMaxConsecutiveRestarts().getRestartCount();
    }
    return 0;
  }

  /**
   * Associates timeout in terms of ms, with this CasProcessor. It is the max number of millis to
   * wait for response.
   * 
   * @param aTimeoutValue
   *          - millis to wait for response
   */
  @Override
  public void setTimeout(int aTimeoutValue) {
    CasProcessorErrorHandling eh = getErrorHandling();
    if (eh != null && eh.getTimeout() != null) {
      eh.getTimeout().set(aTimeoutValue);
    }
  }

  /**
   * Returns max millis to wait for CasProcessor response.
   *
   * @return - millis, 0 if not defined
   */
  @Override
  public int getTimeout() {
    CasProcessorErrorHandling eh = getErrorHandling();
    if (eh != null && eh.getTimeout() != null) {
      return eh.getTimeout().get();
    }

    return 0;
  }

  /**
   * Sets configuration parameter settings for this CasProcessor.
   *
   * @param settings
   *          the new configuration parameter settings
   * @throws CpeDescriptorException
   *           the cpe descriptor exception
   */
  @Override
  public void setConfigurationParameterSettings(CasProcessorConfigurationParameterSettings settings)
          throws CpeDescriptorException {
    configurationParameterSettings = settings;
    if (settings != null && settings.getParameterSettings() != null) {
      int length = settings.getParameterSettings().length;
      if (length > 0) {
        parameterSettings = new ConfigurationParameterSettings_impl();
        org.apache.uima.resource.metadata.NameValuePair[] nvp = new NameValuePair_impl[settings
                .getParameterSettings().length];
        for (int i = 0; i < settings.getParameterSettings().length; i++) {
          nvp[i] = new NameValuePair_impl(settings.getParameterSettings()[i].getName(),
                  settings.getParameterSettings()[i].getValue());
        }
        parameterSettings.setParameterSettings(nvp);
      }

    }

  }

  /**
   * Returns configuration parameter settings for this CasProcessor.
   *
   * @return the configuration parameter settings
   */
  @Override
  public CasProcessorConfigurationParameterSettings getConfigurationParameterSettings() {
    return configurationParameterSettings;
  }

  /**
   * Sets the parameter settings.
   *
   * @param settings
   *          the new parameter settings
   */
  public void setParameterSettings(ConfigurationParameterSettings settings) {
    parameterSettings = settings;
    if (parameterSettings != null) {
      configurationParameterSettings = new CasProcessorConfigurationParameterSettingsImpl(
              parameterSettings);
    }
  }

  /**
   * Gets the parameter settings.
   *
   * @return configuration parameter settings
   */
  public ConfigurationParameterSettings getParameterSettings() {
    ConfigurationParameterSettings local = null;
    if (configurationParameterSettings != null) {
      local = new ConfigurationParameterSettings_impl();

      NameValuePair[] nvp = configurationParameterSettings.getParameterSettings();

      for (int i = 0; nvp != null && i < nvp.length; i++) {
        local.setParameterValue(nvp[i].getName(), nvp[i].getValue());
      }
    } else {
      local = parameterSettings;
    }

    return local;

  }

  /**
   * Gets the parameters.
   *
   * @return parameters
   */
  public Parameter[] getParameters() {
    return parameters;
  }

  /**
   * Gets the filter.
   *
   * @return filter
   */
  public CasProcessorFilter getFilter() {
    return filter;
  }

  /**
   * Sets the checkpoint.
   *
   * @param checkpoint
   *          the new checkpoint
   */
  public void setCheckpoint(CpeCheckpoint checkpoint) {
    this.checkpoint = checkpoint;
  }

  /**
   * Sets the parameters.
   *
   * @param aparameters
   *          the new parameters
   */
  public void setParameters(Parameter[] aparameters) {
    parameters = aparameters;
  }

  /**
   * Sets the filter.
   *
   * @param aFilter
   *          the new filter
   */
  public void setFilter(CasProcessorFilter aFilter) {
    filter = aFilter;
  }

  /**
   * Gets the run in separate process.
   *
   * @return container with configuration info for running CasProcessor in separate process
   */
  @Override
  public CasProcessorRunInSeperateProcess getRunInSeparateProcess() {
    return runInSeparateProcess;
  }

  /**
   * Sets the run in separate process.
   *
   * @param process
   *          container with configuration info for running CasProcessor in separate process
   */
  public void setRunInSeparateProcess(CasProcessorRunInSeperateProcess process) {
    runInSeparateProcess = process;
  }

  /**
   * Gets the deployment parameters.
   *
   * @return deployment parameters
   */
  public CasProcessorDeploymentParams getDeploymentParameters() {
    return deploymentParameters;
  }

  /**
   * Sets the deployment parameters.
   *
   * @param parameters
   *          deployment parameters
   */
  public void setDeploymentParameters(CasProcessorDeploymentParams parameters) {
    deploymentParameters = parameters;
  }

  /**
   * Overridden to read "name" and "value" attributes.
   *
   * @param aElement
   *          the a element
   * @param aParser
   *          the a parser
   * @param aOptions
   *          the a options
   * @throws InvalidXMLException
   *           the invalid XML exception
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#buildFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser, org.apache.uima.util.XMLParser.ParsingOptions)
   */
  @Override
  public void buildFromXMLElement(Element aElement, XMLParser aParser, ParsingOptions aOptions)
          throws InvalidXMLException {
    super.buildFromXMLElement(aElement, aParser, aOptions);
    String field = "name";
    try {
      setName(aElement.getAttribute("name"));
      field = "deployment";
      setDeployment(aElement.getAttribute("deployment"));
    } catch (Exception e) {
      throw new InvalidXMLException(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_EXP_missing_attribute_from_xml_element__WARNING", new Object[] {
                  Thread.currentThread().getName(), "casProcessor", field, "casProcessor" });

    }
  }

  /**
   * Overridden to handle "name" and "value" attributes.
   *
   * @return the XML attributes
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXMLAttributes()
   */
  @Override
  protected AttributesImpl getXMLAttributes() {
    // TODO Auto-generated method stub
    AttributesImpl attrs = super.getXMLAttributes();

    attrs.addAttribute("", "deployment", "deployment", "CDATA", getDeployment());
    attrs.addAttribute("", "name", "name", "CDATA", getName());
    return attrs;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXmlizationInfo()
   */
  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  /** The Constant XMLIZATION_INFO. */
  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("casProcessor",
          new PropertyXmlInfo[] { new PropertyXmlInfo("cpeComponentDescriptor", null),
              new PropertyXmlInfo("deploymentParameters", null),
              new PropertyXmlInfo("runInSeparateProcess", null),
              new PropertyXmlInfo("filter", null), new PropertyXmlInfo("errorHandling", null),
              new PropertyXmlInfo("checkpoint", null),
              new PropertyXmlInfo("sofaNameMappings", null),
              new PropertyXmlInfo("parameterSettings", null), });

  /**
   * Gets the checkpoint.
   *
   * @return configuration for a checkpoint
   */
  @Override
  public CpeCheckpoint getCheckpoint() {
    return checkpoint;
  }

  /**
   * Gets the sofa name mappings.
   *
   * @return sofa name mappings
   */
  @Override
  public CpeSofaMappings getSofaNameMappings() {
    return sofaNameMappings;
  }

  /**
   * Sets the sofa name mappings.
   *
   * @param mappings
   *          sofa name mappings
   */
  @Override
  public void setSofaNameMappings(CpeSofaMappings mappings) {
    sofaNameMappings = mappings;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCasProcessor#setIsParallelizable(boolean)
   */
  @Override
  public void setIsParallelizable(boolean isP) {
    isParallelizable = isP;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCasProcessor#getIsParallelizable()
   */
  @Override
  public boolean getIsParallelizable() {
    return isParallelizable;
  }

}
