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
 * Implementation base class for {@link ConfigurableResource}s. Provides access to configuration
 * parameters as well as basic reconfiguration capability. Subclasses should override the
 * <code>initialize</code> and <code>reconfigure</code> methods to read specific configuration
 * parameters (after calling <code>super.initialize</code> or <code>super.reconfigure</code>).
 * 
 * 
 */
public abstract class ConfigurableResource_ImplBase extends Resource_ImplBase implements
        ConfigurableResource {

  /**
   * @see org.apache.uima.resource.ConfigurableResource#getConfigParameterValue(java.lang.String)
   */
  public Object getConfigParameterValue(String aParamName) {
    return getUimaContext().getConfigParameterValue(aParamName);
  }

  /**
   * @see org.apache.uima.resource.ConfigurableResource#getConfigParameterValue(java.lang.String,
   *      java.lang.String)
   */
  public Object getConfigParameterValue(String aGroupName, String aParamName) {
    return getUimaContext().getConfigParameterValue(aGroupName, aParamName);
  }

  /**
   * @see org.apache.uima.resource.ConfigurableResource#setConfigParameterValue(java.lang.String,
   *      java.lang.Object)
   */
  public void setConfigParameterValue(String aParamName, Object aValue) {
    getUimaContextAdmin().getConfigurationManager().setConfigParameterValue(
            getUimaContextAdmin().getQualifiedContextName() + aParamName, aValue);
  }

  /**
   * @see org.apache.uima.resource.ConfigurableResource#setConfigParameterValue(java.lang.String,
   *      java.lang.String, java.lang.Object)
   */
  public void setConfigParameterValue(String aGroupName, String aParamName, Object aValue) {
    getUimaContextAdmin().getConfigurationManager().setConfigParameterValue(
            getUimaContextAdmin().getQualifiedContextName() + aParamName, aGroupName, aValue);
  }

  /**
   * @see org.apache.uima.resource.ConfigurableResource#reconfigure()
   */
  public void reconfigure() throws ResourceConfigurationException {
    getUimaContextAdmin().getConfigurationManager().reconfigure(
            getUimaContextAdmin().getQualifiedContextName());
  }

}
