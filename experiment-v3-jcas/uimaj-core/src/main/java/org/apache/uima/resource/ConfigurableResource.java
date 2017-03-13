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

package org.apache.uima.resource;

/**
 * A {@link Resource} that has configuration parameters that can be changed after the Resource has
 * been instantiated.
 * <p>
 * To set configuration parameters, call the {@link #setConfigParameterValue(String,String,Object)}
 * method for each parameter that is to be set, and then call the {@link #reconfigure()} method to
 * cause the Resource to re-read its configuration settings.
 * <p>
 * Note that if the Resource attempts to access its configuration settings between the call to
 * <code>setConfigParameterValue</code> and the call to <code>reconfigure</code>, it may
 * retrieve either the old value or the new value. This decision is left to the framework's
 * {@link ConfigurationManager} implementation.
 * 
 * 
 */
public interface ConfigurableResource extends Resource {

  /**
   * Looks up the value of a configuration parameter. This method will only return the value of a
   * parameter that is not defined in any group.
   * <p>
   * This method returns <code>null</code> if the parameter is optional and has not been assigned
   * a value. (For mandatory parameters, an exception is thrown during initialization if no value
   * has been assigned.) This method also returns <code>null</code> if there is no declared
   * configuration parameter with the specified name.
   * 
   * @param aParamName
   *          the name of a parameter that is not in any group
   * 
   * @return the value of the parameter with name <code>aParamName</code>, <code>null</code> is
   *         either the parameter does not exist or it has not been assigned a value.
   */
  public Object getConfigParameterValue(String aParamName);

  /**
   * Looks up the value of a configuration parameter in a group. If the parameter has no value
   * assigned within the group, fallback strategies will be followed.
   * <p>
   * This method returns <code>null</code> if the parameter is optional and has not been assigned
   * a value. (For mandatory parameters, an exception is thrown during initialization if no value
   * has been assigned.) This method also returns <code>null</code> if there is no declared
   * configuration parameter with the specified name.
   * 
   * @param aGroupName
   *          the name of a configuration group. If the group name is
   *          <code>null</code>, this method will return the same value as
   *   {@link #getConfigParameterValue(String) getParameterValue(String)}.
   * @param aParamName the name of a parameter in the group
   * 
   * @return the value of the parameter in group <code>aGroupName</code> with 
   *         name <code>aParamName</code>,,<code>null</code> is either the 
   *         parameter does not exist or it has not been assigned a value.
   */
  public Object getConfigParameterValue(String aGroupName, String aParamName);

  /**
   * Sets the value of a configuration parameter. This only works for a parameter that is not
   * defined in any group. Note that there is no guarantee that the change will take effect until
   * {@link #reconfigure()} is called.
   * 
   * @param aParamName
   *          the name of a parameter that is not in any group
   * @param aValue
   *          the value to assign to the parameter
   */
  public void setConfigParameterValue(String aParamName, Object aValue);

  /**
   * Sets the value of a configuration parameter in a group. Note that there is no guarantee that
   * the change will take effect until {@link #reconfigure()} is called.
   * 
   * @param aGroupName
   *          the name of a configuration group. If this parameter is
   *          <code>null</code>, this method will have the same effect as
   *   {@link #setConfigParameterValue(String,Object) setParameterValue(String,Object)}.
   * @param aParamName the name of a parameter in the group
   * @param aValue the value to assign to the parameter.
   */
  public void setConfigParameterValue(String aGroupName, String aParamName, Object aValue);

  /**
   * Instructs this Resource to re-read its configuration parameter settings.
   * 
   * @throws ResourceConfigurationException
   *           if the configuration is not valid
   */
  public void reconfigure() throws ResourceConfigurationException;
}
