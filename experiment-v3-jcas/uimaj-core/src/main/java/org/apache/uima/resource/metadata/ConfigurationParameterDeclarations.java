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
 * The declarations of configuration parameters in a Resource. A Resource can either declare a
 * single list of {@link ConfigurationParameter}s or a list of {@link ConfigurationGroup}s, where
 * each group can contain a list of parameters. When groups are used, the Resource can also declare
 * a list of {@link #getCommonParameters() common parameters} shared by all groups.
 * 
 * 
 */
public interface ConfigurationParameterDeclarations extends MetaDataObject {

  /**
   * Gets the configuration parameters for this Resource. This gets configuration parameters that
   * are not defined within a group - see also {@link #getConfigurationGroups()}.
   * 
   * @return an array containing {@link ConfigurationParameter} objects, each of which describes a
   *         configuration parameter for this Resource.
   */
  public ConfigurationParameter[] getConfigurationParameters();

  /**
   * Sets the configuration parameters for this Resource. This sets the configuration parameters
   * that are not defined within groups - see also
   * {@link #setConfigurationGroups(ConfigurationGroup[])}.
   * 
   * @param aParams
   *          an array containing {@link ConfigurationParameter} objects, each of which describes a
   *          configuration parameter for this Resource.
   * 
   * @throws org.apache.uima.UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setConfigurationParameters(ConfigurationParameter[] aParams);

  /**
   * Adds a Configuration Parameter that is not in any group.
   * 
   * @param aConfigurationParameter
   *          the Configuration Parameter to add
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void addConfigurationParameter(ConfigurationParameter aConfigurationParameter);

  /**
   * Removes an Configuration Parameter that is not in any group.
   * 
   * @param aConfigurationParameter
   *          the Configuration Parameter to remove (must be == with an ConfigurationParameter in
   *          this collection, or this method will do nothing).
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void removeConfigurationParameter(ConfigurationParameter aConfigurationParameter);

  /**
   * Gets the configuration parameter groups for this Resource.
   * 
   * @return an array containing {@link ConfigurationGroup} objects, each of which describes a
   *         configuration parameter group for this Resource.
   */
  public ConfigurationGroup[] getConfigurationGroups();

  /**
   * Sets the configuration parameter groups for this Resource.
   * 
   * @param aGroups
   *          an array containing {@link ConfigurationGroup} objects, each of which describes a
   *          configuration parameter group for this Resource.
   * 
   * @throws org.apache.uima.UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setConfigurationGroups(ConfigurationGroup[] aGroups);

  /**
   * Adds a Configuration Group.
   * 
   * @param aConfigurationGroup
   *          the Configuration Group to add
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void addConfigurationGroup(ConfigurationGroup aConfigurationGroup);

  /**
   * Removes an Configuration Group
   * 
   * @param aConfigurationGroup
   *          the Configuration Group to remove (must be == with an ConfigurationGroup defined on
   *          this resource, or this method will do nothing).
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void removeConfigurationGroup(ConfigurationGroup aConfigurationGroup);

  /**
   * Gets the configuration parameters that are common to all groups. This property is only
   * meaningful if at least one group is defined.
   * 
   * @return an array containing {@link ConfigurationParameter} objects, each of which describes a
   *         parameter common to all groups.
   */
  public ConfigurationParameter[] getCommonParameters();

  /**
   * Sets the configuration parameters that are common to all groups. This property is only
   * meaningful if at least one group is defined.
   * 
   * @param aParams
   *          an array containing {@link ConfigurationParameter} objects, each of which describes a
   *          parameter common to all groups.
   */
  public void setCommonParameters(ConfigurationParameter[] aParams);

  /**
   * Adds a Configuration Parameter that is common to all groups.
   * 
   * @param aConfigurationParameter
   *          the Configuration Parameter to add
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void addCommonParameter(ConfigurationParameter aConfigurationParameter);

  /**
   * Removes an Configuration Parameter that is common to all groups.
   * 
   * @param aConfigurationParameter
   *          the Configuration Parameter to remove (must be == with an ConfigurationParameter in
   *          this collection, or this method will do nothing).
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void removeCommonParameter(ConfigurationParameter aConfigurationParameter);

  /**
   * Gets the name of the default configuration group. This must refer to the names of the
   * {@link #getConfigurationGroups() configuration groups}.
   * 
   * @return the name of the default configuration group
   */
  public String getDefaultGroupName();

  /**
   * Sets the name of the default configuration group. This must refer to the names of the
   * {@link #getConfigurationGroups() configuration groups}.
   * 
   * @param aGroupName
   *          the name of the default configuration group
   */
  public void setDefaultGroupName(String aGroupName);

  /**
   * Gets the configuration parameter search strategy. Valid values for this property are defined by
   * constants on this interface.
   * 
   * @return the configuration parameter search strategy
   */
  public String getSearchStrategy();

  /**
   * Sets the configuration parameter search strategy. Valid values for this property are defined by
   * constants on this interface.
   * 
   * @param aStrategy
   *          the configuration parameter search strategy
   */
  public void setSearchStrategy(String aStrategy);

  /**
   * Gets a configuration parameter.
   * 
   * @param aGroupName
   *          the name of a group, or <code>null</code> for no group
   * @param aParamName
   *          the name of the parameter
   * 
   * @return the specified parameter, <code>null</code> if it does not exist
   */
  public ConfigurationParameter getConfigurationParameter(String aGroupName, String aParamName);

  /**
   * Gets the declarations of a named configuration group. There may be more than one declaration
   * for a single group name; in this case, all parameters contained in each of these declarations
   * are considered part of the named group.
   * 
   * @param aGroupName
   *          the name of a group
   * 
   * @return an array of ConfigurationGroup declarations having the name <code>aGroupName</code>.
   *         If there are no such groups, an empty array is returned.
   */
  public ConfigurationGroup[] getConfigurationGroupDeclarations(String aGroupName);

  /**
   * A value for the <code>searchStrategy</code> property indicating that there is no fallback. If
   * a request is made for the value of a parameter in a group and there is no such value in that
   * exact group, <code>null</code> will be returned.
   */
  public static final String SEARCH_STRATEGY_NONE = "none";

  /**
   * A value for the <code>searchStrategy</code> property indicating that if there is no value
   * declared in a group, look in the {@link #getDefaultGroupName() default group}.
   */
  public static final String SEARCH_STRATEGY_DEFAULT_FALLBACK = "default_fallback";

  /**
   * A value for the <code>searchStrategy</code> property that is useful when ISO language and
   * country codes are used as configuration group names. If there is no value declared in a group,
   * look in more general groups. The fallback sequence is
   * <code>lang-country-region -%gt; lang-country -%gt; 
   * lang -%gt; default</code>. For example, if a
   * request is made for the value of a parameter in the "en-GB" group and no such group exists, the
   * value from the "en" group will be used instead.
   */
  public static final String SEARCH_STRATEGY_LANGUAGE_FALLBACK = "language_fallback";

}
