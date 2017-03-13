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

import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.Settings;

/**
 * Manages the configuration parameters of all components within a possibly aggregate
 * {@link org.apache.uima.resource.Resource}. Note that the <code>ConfigurationManager</code>
 * needs to make use of the {@link Session} object in order to store configuration settings that are
 * specific to a particular client session.
 * 
 * 
 */
public interface ConfigurationManager {

  /**
   * Creates and sets up a new configuration parameter context. This method is called during the
   * initialization of each Resource that uses this Configuration Manager. Validation will be
   * performed on the configuration parameters declared in this context; if an error is found an
   * exception will be thrown.
   * 
   * Multi-threading: This may be called with the same parameters on multiple threads.  
   * Implementations should check for repeated calls to create the same context and just return in that case
   * 
   * @param aContextName
   *          the name of the context in which the configuration parameter is being accessed. This
   *          corresponds to the path through the aggregate resource, e.g /subAE1/annotator1.
   * @param aResourceMetaData
   *          metadata object containing the configuration parameter declarations and settings.
   * @param externalSettings the external overrides
   * 
   * @throws ResourceConfigurationException
   *           if the configuration settings are invalid
   */
  public void createContext(String aContextName, ResourceMetaData aResourceMetaData, Settings externalSettings)
          throws ResourceConfigurationException;

  /**
   * Sets the current <code>Session</code>. The Configuration Manager uses the
   * <code>Session</code> object to store changes to configuration settings made by calling the
   * <code>setConfigParameterValue</code> methods. This ensures that in a multi-client deployment
   * those settings only apply to the same client that set them.
   * <p>
   * Code that uses this class must be sure to call <code>setSession</code> before using the other
   * methods (except <code>createContext</code>) on this class.
   * 
   * @param aSession
   *          the session object used to store configuration parameter overrides made by a
   *          particular client.
   */
  public void setSession(Session aSession);

  /**
   * Retrieves the value for a configuration parameter.
   * 
   * @param aQualifiedName
   *          the fully-qualified configuration parameter name. This is of the form
   *          <code>ContextName + / + ParameterName</code>.
   * 
   * @return the value of the parameter with the given name. The caller is expected to know the data
   *         type of the parameter. If the parameter does not exist,<code>null</code> is
   *         returned.
   */
  public Object getConfigParameterValue(String aQualifiedName);

  /**
   * Retrieves the value for a configuration parameter in a group.
   * 
   * @param aQualifiedParamName
   *          the fully-qualified configuration parameter name. This is of the form
   *          <code>ContextName + / + ParameterName</code>.
   * @param aGroupName the name of the parameter group
   * 
   * @return the value of the parameter with the given name. The caller is expected to know the data
   *         type of the parameter. If the parameter does not exist,<code>null</code> is
   *         returned.
   */
  public Object getConfigParameterValue(String aQualifiedParamName, String aGroupName);

  /**
   * Sets the value of a configuration parameter. This only works for a parameter that is not
   * defined in any group. Note that there is no guarantee that the change will take effect until
   * {@link #reconfigure(String)} is called.
   * 
   * @param aQualifiedParamName
   *          the fully-qualified configuration parameter name. This is of the form
   *          <code>ContextName + / + ParameterName</code>.
   * @param aValue
   *          the value to assign to the parameter
   */
  public void setConfigParameterValue(String aQualifiedParamName, Object aValue);

  /**
   * Sets the value of a configuration parameter in a group. Note that there is no guarantee that
   * the change will take effect until {@link #reconfigure(String)} is called.
   * 
   * @param aQualifiedParamName
   *          the fully-qualified configuration parameter name. This is of the form
   *          <code>ContextName + / + ParameterName</code>.
   * @param aGroupName
   *          the name of a configuration group. If this parameter is
   *          <code>null</code>, this method will have the same effect as
   *   {@link #setConfigParameterValue(String,Object) setParameterValue(String,Object)}.
   * @param aValue the value to assign to the parameter
   */
  public void setConfigParameterValue(String aQualifiedParamName, String aGroupName, Object aValue);

  /**
   * Completes the reconfiguration of parameters within the specified context. Also validates the
   * parameter settings.
   * 
   * @param aContextName
   *          the name of the context being reconfigured
   * 
   * @throws ResourceConfigurationException
   *           if the new configuration is invalid
   */
  public void reconfigure(String aContextName) throws ResourceConfigurationException;

  /**
   * Gets the ConfigurationParameterDeclarations for the given context.
   * 
   * @param aContextName
   *          the name for which to get the parameter declarations
   * 
   * @return parameter declarations for the context
   */
  public ConfigurationParameterDeclarations getConfigParameterDeclarations(String aContextName);

  /**
   * Gets an object containing the current settings for all configuration parameters within the
   * given context.
   * 
   * @param aContextName
   *          name of context for which to retrieve parameter settings
   * 
   * @return an object containing the current configuration parameter settings
   */
  public ConfigurationParameterSettings getCurrentConfigParameterSettings(String aContextName);

}
