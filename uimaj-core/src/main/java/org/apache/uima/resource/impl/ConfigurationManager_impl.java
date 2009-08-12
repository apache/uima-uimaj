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

package org.apache.uima.resource.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;

/**
 * Basic standalone Configuration Manager implmentation.
 * 
 */
public class ConfigurationManager_impl extends ConfigurationManagerImplBase {

  /**
   * Map containing configuration parameter values and links for parameter values shared by all
   * sessions.
   */
  private Map<String, Object> mSharedParamMap = Collections.synchronizedMap(new HashMap<String, Object>());

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.impl.ConfigurationManagerImplBase#declareParameters(java.lang.String,
   *      org.apache.uima.resource.metadata.ConfigurationParameter[],
   *      org.apache.uima.resource.metadata.ConfigurationParameterSettings, java.lang.String,
   *      java.lang.String)
   */
  protected void declareParameters(String aGroupName, ConfigurationParameter[] aParams,
          ConfigurationParameterSettings aSettings, String aContextName, String aParentContextName) {
    super.declareParameters(aGroupName, aParams, aSettings, aContextName, aParentContextName);
    // iterate over config. param _declarations_ and build mSharedParamNap
    if (aParams != null) {
      for (int i = 0; i < aParams.length; i++) {
        String qname = makeQualifiedName(aContextName, aParams[i].getName(), aGroupName);

        // get the actual setting and store it in the Map (even if it's a null value)
        Object paramValue = aSettings.getParameterValue(aGroupName, aParams[i].getName());
        mSharedParamMap.put(qname, paramValue);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.impl.ConfigurationManagerImplBase#lookupSharedParamNoLinks(java.lang.String)
   */
  protected Object lookupSharedParamNoLinks(String aCompleteName) {
    return mSharedParamMap.get(aCompleteName);
  }
}
