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

import org.apache.uima.collection.metadata.CasProcessorConfigurationParameterSettings;
import org.apache.uima.collection.metadata.CpeCollectionReaderIterator;
import org.apache.uima.collection.metadata.CpeComponentDescriptor;
import org.apache.uima.collection.metadata.CpeSofaMappings;
import org.apache.uima.collection.metadata.NameValuePair;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.impl.ConfigurationParameterSettings_impl;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;

/**
 * The Class CpeCollectionReaderIteratorImpl.
 */
public class CpeCollectionReaderIteratorImpl extends MetaDataObject_impl
        implements CpeCollectionReaderIterator {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -9208074797482603808L;

  /** The descriptor. */
  private CpeComponentDescriptor descriptor;

  /** The configuration parameter settings. */
  private CasProcessorConfigurationParameterSettings configurationParameterSettings;

  /** The sofa name mappings. */
  private CpeSofaMappings sofaNameMappings;

  /** The config parameter settings. */
  private ConfigurationParameterSettings configParameterSettings;

  /**
   * Gets the descriptor.
   *
   * @return the component descriptor
   */
  @Override
  public CpeComponentDescriptor getDescriptor() {
    return descriptor;
  }

  /**
   * Sets the descriptor.
   *
   * @param descriptor
   *          the new descriptor
   */
  @Override
  public void setDescriptor(CpeComponentDescriptor descriptor) {
    this.descriptor = descriptor;
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
  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("collectionIterator",
          new PropertyXmlInfo[] { new PropertyXmlInfo("descriptor", null),
              new PropertyXmlInfo("configParameterSettings", null),
              new PropertyXmlInfo("sofaNameMappings", null),

          });

  /**
   * Gets the configuration parameter settings.
   *
   * @return the parameter settings
   */

  @Override
  public CasProcessorConfigurationParameterSettings getConfigurationParameterSettings() {
    return configurationParameterSettings;

  }

  /**
   * Sets the configuration parameter settings.
   *
   * @param settings
   *          the new configuration parameter settings
   */
  @Override
  public void setConfigurationParameterSettings(
          CasProcessorConfigurationParameterSettings settings) {
    configurationParameterSettings = settings;
    if (settings != null && settings.getParameterSettings() != null) {
      int length = settings.getParameterSettings().length;
      if (length > 0) {
        configParameterSettings = new ConfigurationParameterSettings_impl();
        org.apache.uima.resource.metadata.NameValuePair[] nvp = new NameValuePair_impl[settings
                .getParameterSettings().length];
        for (int i = 0; i < settings.getParameterSettings().length; i++) {
          nvp[i] = new NameValuePair_impl(settings.getParameterSettings()[i].getName(),
                  settings.getParameterSettings()[i].getValue());
        }
        configParameterSettings.setParameterSettings(nvp);
      }

    }
  }

  /**
   * Gets the config parameter settings.
   *
   * @return the parameter settings
   */
  public ConfigurationParameterSettings getConfigParameterSettings() {
    ConfigurationParameterSettings local = null;
    if (configurationParameterSettings != null) {
      local = new ConfigurationParameterSettings_impl();

      NameValuePair[] nvp = configurationParameterSettings.getParameterSettings();

      for (int i = 0; nvp != null && i < nvp.length; i++) {
        local.setParameterValue(nvp[i].getName(), nvp[i].getValue());
      }
    } else
      local = configParameterSettings;

    return local;
  }

  /**
   * Sets the config parameter settings.
   *
   * @param settings
   *          the new config parameter settings
   */
  public void setConfigParameterSettings(ConfigurationParameterSettings settings) {
    configParameterSettings = settings;
    if (configParameterSettings != null) {
      configurationParameterSettings = new CasProcessorConfigurationParameterSettingsImpl(
              configParameterSettings);
    }
  }

  /**
   * Gets the sofa name mappings.
   *
   * @return the sofa name mappings
   */
  @Override
  public CpeSofaMappings getSofaNameMappings() {
    return sofaNameMappings;
  }

  /**
   * Sets the sofa name mappings.
   *
   * @param mappings
   *          the new sofa name mappings
   */
  @Override
  public void setSofaNameMappings(CpeSofaMappings mappings) {
    sofaNameMappings = mappings;
  }
}
