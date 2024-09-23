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

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Collection;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;

/**
 */
public final class ResourceCreationSpecifierFactory {
  private ResourceCreationSpecifierFactory() {
    // This class is not meant to be instantiated
  }

  /**
   * Parse a ResourceCreationSpecifier from the URL of an XML descriptor file, setting additional
   * configuration parameters as necessary.
   * 
   * @param descriptorURL
   *          The URL of the XML descriptor file.
   * @param parameters
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return The ResourceCreationSpecifier for the XML descriptor with all the configuration
   *         parameters set.
   * @throws IOException
   *           if an I/O error occurs
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid {@link ResourceSpecifier}
   */
  public static ResourceCreationSpecifier createResourceCreationSpecifier(URL descriptorURL,
          Object[] parameters) throws InvalidXMLException, IOException {
    return createResourceCreationSpecifier(new XMLInputSource(descriptorURL), parameters);
  }

  /**
   * Parse a ResourceCreationSpecifier from XML descriptor file input, setting additional
   * configuration parameters as necessary.
   * 
   * @param xmlInput
   *          The descriptor file as an XMLInputSource.
   * @param parameters
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return The ResourceCreationSpecifier for the XML descriptor with all the configuration
   *         parameters set.
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid {@link ResourceSpecifier}
   */
  public static ResourceCreationSpecifier createResourceCreationSpecifier(XMLInputSource xmlInput,
          Object[] parameters) throws InvalidXMLException {
    ConfigurationParameterFactory.ensureParametersComeInPairs(parameters);

    ResourceCreationSpecifier specifier;
    XMLParser parser = UIMAFramework.getXMLParser();
    specifier = (ResourceCreationSpecifier) parser.parseResourceSpecifier(xmlInput);
    setConfigurationParameters(specifier, parameters);

    return specifier;
  }

  /**
   * Parse a ResourceCreationSpecifier from an XML descriptor file, setting additional configuration
   * parameters as necessary.
   * 
   * @param descriptorPath
   *          The path to the XML descriptor file.
   * @param parameters
   *          Any additional configuration parameters to be set. These should be supplied as (name,
   *          value) pairs, so there should always be an even number of parameters.
   * @return The ResourceCreationSpecifier for the XML descriptor with all the configuration
   *         parameters set.
   * @throws IOException
   *           if an I/O error occurs
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid {@link ResourceSpecifier}
   */
  public static ResourceCreationSpecifier createResourceCreationSpecifier(String descriptorPath,
          Object[] parameters) throws InvalidXMLException, IOException {
    return createResourceCreationSpecifier(new XMLInputSource(descriptorPath), parameters);
  }

  /**
   * Create configuration parameter declarations and settings from a list of (name, value) pairs.
   * 
   * @param specifier
   *          The ResourceCreationSpecifier whose parameters are to be set.
   * @param configurationData
   *          The configuration parameters to be set. These should be supplied as (name, value)
   *          pairs, so there should always be an even number of parameters.
   */
  public static void setConfigurationParameters(ResourceCreationSpecifier specifier,
          Object... configurationData) {
    if (configurationData == null || configurationData.length == 0) {
      return;
    }

    ConfigurationParameterFactory.ensureParametersComeInPairs(configurationData);

    ConfigurationParameter[] configurationParameters = new ConfigurationParameter[configurationData.length
            / 2];
    Object[] configurationValues = new Object[configurationData.length / 2];

    for (int i = 0; i < configurationValues.length; i++) {
      String name = (String) configurationData[i * 2];
      Object value = configurationData[i * 2 + 1];
      ConfigurationParameter param = ConfigurationParameterFactory.createPrimitiveParameter(name,
              value.getClass(), null, false);
      configurationParameters[i] = param;
      configurationValues[i] = ConfigurationParameterFactory.convertParameterValue(param, value);
    }
    setConfigurationParameters(specifier, configurationParameters, configurationValues);
  }

  /**
   * This method passes through to
   * {@link #setConfigurationParameters(ResourceMetaData, ConfigurationParameter[], Object[])}
   * 
   * @param specifier
   *          The ResourceCreationSpecifier whose parameters are to be set.
   * @param configurationParameters
   *          the configuration parameter declarations.
   * @param configurationValues
   *          the configuration parameter values.
   */
  public static void setConfigurationParameters(ResourceCreationSpecifier specifier,
          ConfigurationParameter[] configurationParameters, Object[] configurationValues) {
    setConfigurationParameters(specifier.getMetaData(), configurationParameters,
            configurationValues);
  }

  /**
   * This method sets the configuration parameters of a resource. The length of
   * configurationParameters and configurationValues should be equal
   * 
   * @param metaData
   *          The ResourceMetaData whose parameters are to be set.
   * @param configurationParameters
   *          an array of configuration parameters
   * @param configurationValues
   *          an array of configuration parameter values
   */
  public static void setConfigurationParameters(ResourceMetaData metaData,
          ConfigurationParameter[] configurationParameters, Object[] configurationValues) {
    ConfigurationParameterDeclarations paramDecls = metaData
            .getConfigurationParameterDeclarations();
    ConfigurationParameterSettings paramSettings = metaData.getConfigurationParameterSettings();
    for (int i = 0; i < configurationParameters.length; i++) {
      ConfigurationParameter decl = paramDecls.getConfigurationParameter(null,
              configurationParameters[i].getName());
      if (paramDecls != null && decl == null) {
        paramDecls.addConfigurationParameter(configurationParameters[i]);
        decl = configurationParameters[i];
      }

      // Upgrade single-value to multi-value if necessary
      Object value = configurationValues[i];
      if ((value != null) && decl.isMultiValued() && !isMultiValue(value)) {
        value = Array.newInstance(value.getClass(), 1);
        Array.set(value, 0, configurationValues[i]);
      }

      paramSettings.setParameterValue(configurationParameters[i].getName(), value);
    }
  }

  /**
   * Check if the given parameter represents one value or multiple values.
   */
  private static boolean isMultiValue(Object aObject) {
    if (aObject != null) {
      return (aObject instanceof Collection) || aObject.getClass().isArray();
    } else {
      return false;
    }
  }
}
