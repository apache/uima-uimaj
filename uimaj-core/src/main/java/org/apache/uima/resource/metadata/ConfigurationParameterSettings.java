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

package org.apache.uima.resource.metadata;

import java.util.Map;

/**
 * The values for {@link ConfigurationParameter}s in a Resource. When the Resource has declared
 * {@link ConfigurationGroup}s, there may be different values for each group.
 * 
 * 
 */
public interface ConfigurationParameterSettings extends MetaDataObject {

  /**
   * Gets the settings for configuration parameters that are not in any group.
   * 
   * @return an array of <code>NameValuePair</code> objects, each of which contains a parameter
   *         name and the value of that parameter
   */
  public NameValuePair[] getParameterSettings();

  /**
   * Sets the settings for configuration parameters that are not in any group.
   * 
   * @param aSettings
   *          an array of <code>NameValuePair</code> objects, each of which contains a parameter
   *          name and the value of that parameter
   */
  public void setParameterSettings(NameValuePair[] aSettings);

  /**
   * Gets the settings for configuration parameters that are defined within groups.
   * 
   * @return a Map with <code>String</code> keys (the group names) and {@link NameValuePair}[]
   *         values (the settings for parameters in that group.
   */
  public Map<String, NameValuePair[]> getSettingsForGroups();

  /**
   * Looks up the value of a parameter. This is a "dumb" getter and does not follow any fallback
   * strategies. It will only return the value of a parameter that is not defined in any group.
   * 
   * @param aParamName
   *          the name of a parameter that is not in any group
   * 
   * @return the value of the parameter with name <code>aParamName</code>
   */
  public Object getParameterValue(String aParamName);

  /**
   * Looks up the value of a parameter in a group. This is a "dumb" getter and does not follow any
   * fallback strategies.
   * 
   * @param aGroupName
   *          the name of a configuration group. If this parameter is
   *          <code>null</code>, this method will return the same value as
   *   {@link #getParameterValue(String)}.
   * @param aParamName the name of a parameter in the group
   * 
   * @return the value of the parameter in group <code>aGroupName</code> with 
   *         name <code>aParamName</code>
   */
  public Object getParameterValue(String aGroupName, String aParamName);

  /**
   * Sets the value of a parameter. This only works for a parameter that is not defined in any
   * group.
   * 
   * @param aParamName
   *          the name of a parameter that is not in any group
   * @param aValue
   *          the value to assign to the parameter
   */
  public void setParameterValue(String aParamName, Object aValue);

  /**
   * Sets the value of a parameter in a group.
   * 
   * @param aGroupName
   *          the name of a configuration group
   * @param aParamName
   *          the name of a parameter in the group
   * @param aValue
   *          the value to assign to the parameter
   */
  public void setParameterValue(String aGroupName, String aParamName, Object aValue);
}
