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

import org.apache.uima.UIMA_UnsupportedOperationException;

/**
 * A group of {@link ConfigurationParameter}s. Actually, a single <code>ConfigurationGroup</code>
 * object can have multiple group names, in which case it represents multiple groups that all share
 * the same parameters.
 * 
 * 
 */
public interface ConfigurationGroup extends MetaDataObject {

  /**
   * Gets the group names for this <code>ConfigurationGroup</code> object.
   * 
   * @return an array of group names.  Names are not allowed to contain
   *   whitespace.
   */
  public String[] getNames();

  /**
   * Sets the group names for this <code>ConfigurationGroup</code> object.
   * 
   * @param aNames an array of group names.  Names are not allowed to
   *   contain whitespace.
   */
  public void setNames(String[] aNames);

  /**
   * Gets the configuration parameters in this group.
   * 
   * @return an array containing {@link ConfigurationParameter} objects, each of which describes a
   *         configuration parameter in this group.
   */
  public ConfigurationParameter[] getConfigurationParameters();

  /**
   * Sets the configuration parameters in this group.
   * 
   * @param aParams
   *          an array containing {@link ConfigurationParameter} objects, each of which describes a
   *          configuration parameter in this group.
   */
  public void setConfigurationParameters(ConfigurationParameter[] aParams);

  /**
   * Adds a Configuration Parameter to this group.
   * 
   * @param aConfigurationParameter
   *          the Configuration Parameter to add
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void addConfigurationParameter(ConfigurationParameter aConfigurationParameter);

  /**
   * Removes an Configuration Parameter from this group.
   * 
   * @param aConfigurationParameter
   *          the Configuration Parameter to remove (must be == with an ConfigurationParameter in
   *          this group, or this method will do nothing).
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void removeConfigurationParameter(ConfigurationParameter aConfigurationParameter);

}
