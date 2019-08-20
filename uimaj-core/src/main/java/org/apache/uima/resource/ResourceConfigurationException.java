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

import org.apache.uima.UIMAException;

/**
 * Thrown to indicate that a {@link Resource}'s configuration parameters could not be set. Most
 * commonly this will be because the caller has specified a nonexistent parameter name or a value of
 * the incorrect data type.
 * 
 * 
 */
public class ResourceConfigurationException extends UIMAException {

  private static final long serialVersionUID = -412324593044962476L;

  /**
   * Message key for a standard UIMA exception message: "No configuration parameter with name {0} is
   * declared in component "{1}."
   */
  public static final String NONEXISTENT_PARAMETER = "nonexistent_parameter";

  /**
   * Message key for a standard UIMA exception message: "No configuration parameter with name {0} is
   * declared in group {1} in this component "{2}"."
   */
  public static final String NONEXISTENT_PARAMETER_IN_GROUP = "nonexistent_parameter_in_group";

  /**
   * Message key for a standard UIMA exception message: "Parameter type mismatch in component "{0}".
   * A value of class {1} cannot be assigned to the configuration parameter {2}, which has type
   * {3}."
   */
  public static final String PARAMETER_TYPE_MISMATCH = "parameter_type_mismatch";

  /**
   * Message key for a standard UIMA exception message: "Configuration parameter "{0}" in component
   * "{1}" is multi-valued and must be assigned an array for its value."
   */
  public static final String ARRAY_REQUIRED = "array_required";

  /**
   * Message key for a standard UIMA exception message: "No value has been assigned to the mandatory
   * configuration parameter {0}."
   */
  public static final String MANDATORY_VALUE_MISSING = "mandatory_value_missing";

  /**
   * Message key for a standard UIMA exception message: "No value has been assigned to the mandatory
   * configuration parameter {0} in group {1}."
   */
  public static final String MANDATORY_VALUE_MISSING_IN_GROUP = "mandatory_value_missing_in_group";

  /**
   * Message key for a standard UIMA exception message: "The configuration data {0} for Configuraion
   * parameter {1} in the resource is absent or not valid"
   */
  public static final String RESOURCE_DATA_NOT_VALID = "resource_data_not_valid";

  /**
   * Message key for a standard UIMA exception message: Configuration setting for {0} is absent
   */
  public static final String CONFIG_SETTING_ABSENT = "config_setting_absent";

  /**
   * Message key for a standard UIMA exception message: Invalid value for parameter "{0}" in
   * component "{1}" -- directory "{2}" does not exist.
   */
  public static final String DIRECTORY_NOT_FOUND = "directory_not_found";
  
  /**
   * Message key for a standard UIMA exception message: External override variable "{0}" references the undefined variable "{1}"
   */
  public static final String EXTERNAL_OVERRIDE_INVALID = "external_override_invalid";

  /**
   * Message key for a standard UIMA exception message: Error loading external overrides from "{0}"
   */
  public static final String EXTERNAL_OVERRIDE_ERROR = "external_override_error";
  
  /**
   * Message key for a standard UIMA exception message: External override value for "{0}" has the wrong type (scalar or array)
   */
  public static final String EXTERNAL_OVERRIDE_TYPE_MISMATCH = "external_override_type_mismatch";
  
  /**
   * Message key for a standard UIMA exception message: External override value "{0}" is not an integer
   */
  public static final String EXTERNAL_OVERRIDE_NUMERIC_ERROR = "external_override_numeric_error";
  
  /**
   * Message key for a standard UIMA exception message: External override variable "{0}" has a circular reference to itself
   */
  public static final String EXTERNAL_OVERRIDE_CIRCULAR_REFERENCE = "external_override_circular_reference";
  
  /**
   * Creates a new exception with a null message.
   */
  public ResourceConfigurationException() {
    super();
  }

  /**
   * Creates a new exception with the specified cause and a null message.
   * 
   * @param aCause
   *          the original exception that caused this exception to be thrown, if any
   */
  public ResourceConfigurationException(Throwable aCause) {
    super(aCause);
  }

  /**
   * Creates a new exception with a the specified message.
   * 
   * @param aResourceBundleName
   *          the base name of the resource bundle in which the message for this exception is
   *          located.
   * @param aMessageKey
   *          an identifier that maps to the message for this exception. The message may contain
   *          placeholders for arguments as defined by the
   *          {@link java.text.MessageFormat MessageFormat} class.
   * @param aArguments
   *          The arguments to the message. <code>null</code> may be used if the message has no
   *          arguments.
   */
  public ResourceConfigurationException(String aResourceBundleName, String aMessageKey,
          Object[] aArguments) {
    super(aResourceBundleName, aMessageKey, aArguments);
  }

  /**
   * Creates a new exception with the specified message and cause.
   * 
   * @param aResourceBundleName
   *          the base name of the resource bundle in which the message for this exception is
   *          located.
   * @param aMessageKey
   *          an identifier that maps to the message for this exception. The message may contain
   *          placeholders for arguments as defined by the
   *          {@link java.text.MessageFormat MessageFormat} class.
   * @param aArguments
   *          The arguments to the message. <code>null</code> may be used if the message has no
   *          arguments.
   * @param aCause
   *          the original exception that caused this exception to be thrown, if any
   */
  public ResourceConfigurationException(String aResourceBundleName, String aMessageKey,
          Object[] aArguments, Throwable aCause) {
    super(aResourceBundleName, aMessageKey, aArguments, aCause);
  }

  /**
   * Creates a new exception with a message from the {@link #STANDARD_MESSAGE_CATALOG}.
   * 
   * @param aMessageKey
   *          an identifier that maps to the message for this exception. The message may contain
   *          placeholders for arguments as defined by the
   *          {@link java.text.MessageFormat MessageFormat} class.
   * @param aArguments
   *          The arguments to the message. <code>null</code> may be used if the message has no
   *          arguments.
   */
  public ResourceConfigurationException(String aMessageKey, Object[] aArguments) {
    super(aMessageKey, aArguments);
  }

  /**
   * Creates a new exception with the specified cause and a message from the
   * {@link #STANDARD_MESSAGE_CATALOG}.
   * 
   * @param aMessageKey
   *          an identifier that maps to the message for this exception. The message may contain
   *          placeholders for arguments as defined by the
   *          {@link java.text.MessageFormat MessageFormat} class.
   * @param aArguments
   *          The arguments to the message. <code>null</code> may be used if the message has no
   *          arguments.
   * @param aCause
   *          the original exception that caused this exception to be thrown, if any
   */
  public ResourceConfigurationException(String aMessageKey, Object[] aArguments, Throwable aCause) {
    super(aMessageKey, aArguments, aCause);
  }
}
