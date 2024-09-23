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
package org.apache.uima.fit.factory;

import static org.apache.uima.fit.factory.ConfigurationParameterFactory.createConfigurationData;
import static org.apache.uima.fit.factory.ConfigurationParameterFactory.ensureParametersComeInPairs;
import static org.apache.uima.fit.factory.ConfigurationParameterFactory.setParameters;
import static org.apache.uima.fit.factory.ExternalResourceFactory.bindResourceOnce;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createResourceDependencies;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.Constants;
import org.apache.uima.fit.factory.ConfigurationParameterFactory.ConfigurationData;
import org.apache.uima.flow.FlowController;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.flow.impl.FlowControllerDescription_impl;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ResourceMetaData;

/**
 */

public final class FlowControllerFactory {
  private FlowControllerFactory() {
    // This class is not meant to be instantiated
  }

  /**
   * Creates a new FlowControllerDescription for a given class and configuration data
   * 
   * @param flowControllerClass
   *          the flow controller class
   * @param configurationData
   *          should be configuration parameter name / value pairs.
   * @return a flow controller description
   * @throws ResourceInitializationException
   *           if the description could not be created
   */
  public static FlowControllerDescription createFlowControllerDescription(
          Class<? extends FlowController> flowControllerClass, Object... configurationData)
          throws ResourceInitializationException {

    ensureParametersComeInPairs(configurationData);

    // Extract ExternalResourceDescriptions from configurationData
    // <ParamterName, ExternalResourceDescription> will be stored in this map
    Map<String, ExternalResourceDescription> externalResources = ExternalResourceFactory
            .extractResourceParameters(configurationData);

    // Create description normally
    ConfigurationData cdata = createConfigurationData(configurationData);
    return createFlowControllerDescription(flowControllerClass, cdata.configurationParameters,
            cdata.configurationValues, externalResources);
  }

  /**
   * @param flowControllerClass
   *          the flow controller class
   * @param configurationParameters
   *          the configuration parameters
   * @param configurationValues
   *          the configuration parameter values
   * @return a flow controller description
   * @throws ResourceInitializationException
   *           if the description could not be created
   */
  public static FlowControllerDescription createFlowControllerDescription(
          Class<? extends FlowController> flowControllerClass,
          ConfigurationParameter[] configurationParameters, Object[] configurationValues)
          throws ResourceInitializationException {
    return createFlowControllerDescription(flowControllerClass, configurationParameters,
            configurationValues, null);
  }

  /**
   * Creates a new FlowControllerDescription for a given class and configuration parameters with
   * values
   * 
   * @param flowControllerClass
   *          the flow controller class
   * @param configurationParameters
   *          the configuration parameters
   * @param configurationValues
   *          the configuration parameter values
   * @param externalResources
   *          the external resources
   * @return a flow controller description
   * @throws ResourceInitializationException
   *           if the description could not be created
   */
  public static FlowControllerDescription createFlowControllerDescription(
          Class<? extends FlowController> flowControllerClass,
          ConfigurationParameter[] configurationParameters, Object[] configurationValues,
          Map<String, ExternalResourceDescription> externalResources)
          throws ResourceInitializationException {
    FlowControllerDescription desc = new FlowControllerDescription_impl();
    desc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
    desc.setImplementationName(flowControllerClass.getName());

    // set parameters
    setParameters(desc, flowControllerClass, configurationParameters, configurationValues);

    // Configure resource meta data
    ResourceMetaData meta = desc.getMetaData();
    ResourceMetaDataFactory.configureResourceMetaData(meta, flowControllerClass);

    // Extract external resource dependencies
    desc.setExternalResourceDependencies(createResourceDependencies(flowControllerClass));

    // Bind External Resources
    if (externalResources != null) {
      for (Entry<String, ExternalResourceDescription> e : externalResources.entrySet()) {
        bindResourceOnce(desc, e.getKey(), e.getValue());
      }
    }

    return desc;
  }
}
