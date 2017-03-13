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

/**
 * Contains configuration parameter settings for a CAS Processor. These settings override settings
 * in the CAS Processor's descriptor.
 */
public interface CasProcessorConfigurationParameterSettings {
  /**
   * Gets the settings for configuration parameters that are not in any group.
   * 
   * @return an array of <code>NameValuePair</code> objects, each of which contains a parameter
   *         name and the value of that parameter
   */
  public NameValuePair[] getParameterSettings();

  /**
   * Looks up the value of a parameter.
   * 
   * @param aParamName
   *          the name of a parameter
   * 
   * @return the value of the parameter with name <code>aParamName</code>
   */
  public Object getParameterValue(String aParamName);

  /**
   * Sets the value of a parameter.
   * 
   * @param aParamName
   *          the name of a parameter that is not in any group
   * @param aValue
   *          the value to assign to the parameter. This must be a String, Boolean, Integer, Float,
   *          or an array of one of those types.
   */
  public void setParameterValue(String aParamName, Object aValue);

}
