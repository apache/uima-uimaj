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
import org.apache.uima.collection.metadata.CpeCollectionReaderCasInitializer;
import org.apache.uima.collection.metadata.CpeComponentDescriptor;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.collection.metadata.CpeSofaMappings;
import org.apache.uima.collection.metadata.NameValuePair;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.impl.ConfigurationParameterSettings_impl;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;

/**
 * @deprecated As of v2.0, CAS Initializers are deprecated.
 */
@Deprecated
public class CpeCollectionReaderCasInitializerImpl extends MetaDataObject_impl implements
        CpeCollectionReaderCasInitializer {
  private static final long serialVersionUID = -6284616239685904940L;

  private CpeComponentDescriptor descriptor;

  private ConfigurationParameterSettings configurationParameterSettings;

  private CasProcessorConfigurationParameterSettings cfps;

  private CpeSofaMappings sofaNameMappings;

  public CpeCollectionReaderCasInitializerImpl() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCollectionReaderCasInitializer#setDescriptorPath(java.lang.String)
   */
  public void setDescriptor(CpeComponentDescriptor aDescriptor) {
    descriptor = aDescriptor;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeCollectionReaderCasInitializer#getDescriptorPath()
   */
  public CpeComponentDescriptor getDescriptor() {
    return descriptor;
  }

  /**
   * Returns configuration parameter settings for this CasInitializer.
   */
  public CasProcessorConfigurationParameterSettings getConfigurationParameterSettings() {
    return cfps;
  }

  /**
   * Sets configuration parameter settings for this CasInitializer.
   */
  public void setConfigurationParameterSettings(CasProcessorConfigurationParameterSettings settings)
          throws CpeDescriptorException {
    cfps = settings;

    if (settings != null && settings.getParameterSettings() != null) {
      int length = settings.getParameterSettings().length;
      if (length > 0) {
        configurationParameterSettings = new ConfigurationParameterSettings_impl();
        org.apache.uima.resource.metadata.NameValuePair[] nvp = new NameValuePair_impl[settings
                .getParameterSettings().length];
        for (int i = 0; i < settings.getParameterSettings().length; i++) {
          nvp[i] = new NameValuePair_impl(settings.getParameterSettings()[i].getName(), settings
                  .getParameterSettings()[i].getValue());
        }
        configurationParameterSettings.setParameterSettings(nvp);
      }

    }

  }

  /**
   * @return the parameter settings
   */
  public ConfigurationParameterSettings getParameterSettings() {
    ConfigurationParameterSettings local = null;
    if (cfps != null) {
      local = new ConfigurationParameterSettings_impl();

      NameValuePair[] nvp = cfps.getParameterSettings();

      for (int i = 0; nvp != null && i < nvp.length; i++) {
        local.setParameterValue(nvp[i].getName(), nvp[i].getValue());
      }
    } else
      local = configurationParameterSettings;

    return local;

  }

  /**
   * @param settings
   */
  public void setParameterSettings(ConfigurationParameterSettings settings) {
    configurationParameterSettings = settings;
    if (configurationParameterSettings != null) {
      cfps = new CasProcessorConfigurationParameterSettingsImpl(configurationParameterSettings);
    }

  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("casInitializer",
          new PropertyXmlInfo[] { new PropertyXmlInfo("descriptor", null),
              new PropertyXmlInfo("parameterSettings", null),
              new PropertyXmlInfo("sofaNameMappings", null),

          });

  /**
   * @return the sofa mappings
   */
  public CpeSofaMappings getSofaNameMappings() {
    return sofaNameMappings;
  }

  /**
   * @param mappings
   */
  public void setSofaNameMappings(CpeSofaMappings mappings) {
    sofaNameMappings = mappings;
  }

}
