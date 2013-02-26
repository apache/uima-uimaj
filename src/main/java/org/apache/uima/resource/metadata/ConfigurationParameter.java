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
 * Completely specifies a configuration parameter on a UIMA resource.
 * <p>
 * A configuration parameter consists of the following fields:
 * <ul>
 * <li>Name</li>
 * <li>Description</li>
 * <li>Type (String, Boolean, Integer, or Float)</li>
 * <li>Is the parameter multi-valued?</li>
 * <li>Is a value mandatory?</li>
 * <li>Overrides (see below)</li>
 * </ul>
 * <p>
 * This interface does not provide access to the value of the parameter - that is a separate piece
 * of metadata associated with the resource.
 * <p>
 * In an aggregate resource, configuration parameters may override component resources' parameters.
 * This is done by the {@link #getOverrides() overrides} property. Overrides should always be
 * specified for aggregate resources. If no overrides are specified, the default behavior is to
 * override any parameter with the same name in any component resource. However, this usage is
 * discouraged and will generate a warning in the log file; it exists for backwards compatibility
 * purposes.
 * <p>
 * As with all {@link MetaDataObject}s, a <code>ConfigurationParameter</code> may or may not be
 * modifiable. An application can find out by calling the {@link #isModifiable()} method.
 * 
 * 
 */
public interface ConfigurationParameter extends MetaDataObject {

  /**
   * Retrieves the name of this configuration parameter.
   * 
   * @return the name of this configuration parameter.
   */
  public String getName();

  /**
   * Sets the name of this configuration parameter.
   * 
   * @param aName
   *          the name of this configuration parameter.
   * 
   * @throws org.apache.uima.UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setName(String aName);

  /**
   * Retrieves the external name of this configuration parameter.
   * 
   * @return the external name of this configuration parameter.
   */
  public String getExternalOverrideName();

  /**
   * Sets the external name of this configuration parameter.
   * 
   * @param aExternalOverrideName
   *          the external name of this configuration parameter.
   * 
   * @throws org.apache.uima.UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setExternalOverrideName(String aExternalOverrideName);
  
  /**
   * Retrieves the description of this configuration parameter.
   * 
   * @return the description of this configuration parameter.
   */
  public String getDescription();

  /**
   * Sets the description of this configuration parameter.
   * 
   * @param aDescription
   *          the description of this configuration parameter.
   * 
   * @throws org.apache.uima.UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setDescription(String aDescription);

  /**
   * Retrieves the data type of this configuration parameter.
   * 
   * @return the data type of this configuration parameter. This will be one of the TYPE constants
   *         defined on this interface.
   */
  public String getType();

  /**
   * Sets the data type of this configuration parameter.
   * 
   * @param aType
   *          the data type of this configuration parameter. This must be one of the TYPE constants
   *          defined on this interface.
   * 
   * @throws org.apache.uima.UIMA_IllegalArgumentException
   *           if <code>aType</code> is not a valid data type defined by a TYPE constant on this
   *           interface.
   * @throws org.apache.uima.UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setType(String aType);

  /**
   * Retrieves whether this parameter is multi-valued. Multi-valued parameters take an array of
   * values, each of which must be of the appropriate data type.
   * 
   * @return true if and only if this parameter is multi-valued.
   */
  public boolean isMultiValued();

  /**
   * Sets whether this parameter is multi-valued. Multi-valued parameters take an array of values,
   * each of which must be of the appropriate data type.
   * 
   * @param aMultiValued
   *          true if and only if this parameter is multi-valued.
   * 
   * @throws org.apache.uima.UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setMultiValued(boolean aMultiValued);

  /**
   * Retrieves whether this parameter is mandatory.
   * 
   * @return true if and only if this parameter is mandatory.
   */
  public boolean isMandatory();

  /**
   * Sets whether this parameter is mandatory.
   * 
   * @param aMandatory
   *          true if and only if this parameter is mandatory.
   * 
   * @throws org.apache.uima.UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  public void setMandatory(boolean aMandatory);

  /**
   * Retrieves whether this parameter is published to clients. A non-published parameter is used
   * only for initialization of the resource, and thereafter is not accessible to clients.
   * 
   * @return true if and only if this parameter is published
   */
  // public boolean isPublished();
  /**
   * Sets whether this parameter is published to clients. A non-published parameter is used only for
   * initialization of the resource, and thereafter is not accessible to clients.
   * 
   * @param aPublishes
   *          true if and only if this parameter is published
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   */
  // public void setPublished(boolean aPublished);
  /**
   * Gets the parameters that are this parameter overrides. This is used for aggregate resources
   * only. Overrides are expressed as strings of the form <i>componentName</i><code>/</code><i>parameterName</i>.
   * For example the overrides <code>annotator1/parameter1</code> would override the parameter
   * named <code>parameter1</code> within the component named <code>annotator1</code>.
   * 
   * @return the parameters this this parameter overrides
   */
  public String[] getOverrides();

  /**
   * Sets the parameters that are this parameter overrides. This is used for aggregate resources
   * only. Overrides are expressed as strings of the form <i>componentName</i><code>/</code><i>parameterName</i>.
   * For example the overrides <code>annotator1/parameter1</code> would override the parameter
   * named <code>parameter1</code> within the component named <code>annotator1</code>.
   * 
   * @param aOverrides
   *          the parameters this this parameter overrides
   */
  public void setOverrides(String[] aOverrides);

  /**
   * Adds an override to this configuration parameter.
   * 
   * @param aOverride
   *          the override to add
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   * @see #setOverrides(String[])
   */
  public void addOverride(String aOverride);

  /**
   * Removes an override from this configuration parameter.
   * 
   * @param aOverride
   *          the override to remove. Must equal (via the equals() method) one of the overrides on
   *          this parameter, or this method will do nothing.
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this object is not modifiable
   * @see #setOverrides(String[])
   */
  public void removeOverride(String aOverride);

  /**
   * Identifies the String data type. Values of the parameter will be of type java.lang.String.
   */
  public static final String TYPE_STRING = "String";

  /**
   * Identifies the Boolean data type. Values of the parameter will be of type java.lang.Boolean.
   */
  public static final String TYPE_BOOLEAN = "Boolean";

  /**
   * Identifies the Integer data type. Values of the parameter will be of type java.lang.Integer.
   */
  public static final String TYPE_INTEGER = "Integer";

  /**
   * Identifies the Float data type. Values of the parameter will be of type java.lang.Float.
   */
  public static final String TYPE_FLOAT = "Float";
}
