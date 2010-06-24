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

import java.util.ArrayList;

import org.apache.uima.collection.metadata.CasProcessorConfigurationParameterSettings;
import org.apache.uima.collection.metadata.NameValuePair;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;

public class CasProcessorConfigurationParameterSettingsImpl implements
        CasProcessorConfigurationParameterSettings {
  private NameValuePair[] params = new NameValuePair[0];

  private ArrayList paramList = new ArrayList(0);

  protected CasProcessorConfigurationParameterSettingsImpl() {

  }

  protected CasProcessorConfigurationParameterSettingsImpl(ConfigurationParameterSettings aCps) {
    int size = 0;
    if (aCps != null) {
      size = aCps.getParameterSettings().length;
      params = new NameValuePair[size];
    }
    for (int i = 0; i < size; i++) {
      paramList.add(new NameValuePairImpl(aCps.getParameterSettings()[i].getName(), aCps
              .getParameterSettings()[i].getValue()));
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorDeploymentParams#getAll()
   */

  public NameValuePair[] getParameterSettings() {
    NameValuePair[] nvp = new NameValuePairImpl[paramList.size()];
    paramList.toArray(nvp);
    return nvp;
  }

  private NameValuePair getParamValueObject(String aParamName) {
    for (int i = 0; params != null && i < params.length; i++) {
      if (aParamName.equals(((NameValuePair) paramList.get(i)).getName())) {
        return (NameValuePair) paramList.get(i);
      }
    }
    return null;

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorConfigurationParameterSettings#getParameterValue(java.lang.String)
   */
  public Object getParameterValue(String aParamName) {
    NameValuePair valueObject = getParamValueObject(aParamName);
    if (valueObject != null) {
      return valueObject.getValue();
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorConfigurationParameterSettings#setParameterValue(java.lang.String,
   *      java.lang.Object)
   */
  public void setParameterValue(String aParamName, Object aValue) {
    NameValuePair valueObject = getParamValueObject(aParamName);
    if (valueObject != null) {
      valueObject.setValue(aValue);
    } else {
      paramList.add(new NameValuePairImpl(aParamName, aValue));
    }
  }

}
