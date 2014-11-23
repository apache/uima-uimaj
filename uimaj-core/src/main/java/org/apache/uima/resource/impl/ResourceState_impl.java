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

import org.apache.uima.resource.metadata.NameValuePair;

/**
 * Encapsulates state information about a  Resource_impl . Note that this does not need to
 * include all state information, just that information that is modifiable by a client of a Resource
 * service. The purpose of this class is to enable instance pooling when Resources are deployed as
 * services.
 * <p>
 * Note that the methods of this class have default (package) visibility since only a
 *  Resource_Impl  object should be able to get and set its state.
 * 
 * 
 */

/**
 * This class is not referenced by anything 9/2013
 *
 */
public class ResourceState_impl {

  /**
   * Creates a new ResourceState_impl
   */
  ResourceState_impl() {
  }

  /**
   * Gets configuration parameter settings for the Resource.
   * 
   * @return an array of <code>NameValuePair</code> objects containing the names of configuration
   *         parameters and their current values.
   */
  NameValuePair[] getConfigurationParameterSettings() {
    return mConfigurationParameterSettings;
  }

  /**
   * Sets configuration parameter settings for the Resource.
   * 
   * @param aSettings
   *          an array of <code>NameValuePair</code> objects containing the names of configuration
   *          parameters and their current values.
   */
  void setConfigurationParameterSettings(NameValuePair[] aSettings) {
    mConfigurationParameterSettings = aSettings;
  }

  /**
   * Configuration parameter settings for the resource.
   */
  private NameValuePair[] mConfigurationParameterSettings;
}
