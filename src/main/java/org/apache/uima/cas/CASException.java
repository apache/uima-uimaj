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

package org.apache.uima.cas;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.MissingResourceException;

/**
 * Exception class for package org.apache.uima.cas. Automatically generated from message catalog.
 */
public class CASException extends Exception {

  private static final String resource_file = "org.apache.uima.cas.cas_exception";

  private static final String missing_resource_error = "Could not load message catalog: "
                  + resource_file;

  private static final int MESSAGES_NOT_FOUND = -1;

  /** The value of the feature {0} cannot be accessed as type {1}, because it is {2}. */
  public static final int INAPPROP_TYPE_EXCEPTION = 0;

  /**
   * The value of the feature {0} on the structure of type {1} cannot be accessed, because {0} is
   * not defined for {1}.
   */
  public static final int UNDEFINED_FEATURE = 1;

  /**
   * The feature structure of type {0} cannot be created. Structures of this type cannot be created
   * directly.
   */
  public static final int CANT_CREATE_BUILTIN_FS = 2;

  /** The structure of type {0} cannot be accessed as a String. */
  public static final int NOT_A_STRING = 3;

  /**
   * The types are added in the wrong sort order. Adding {0} < {1} makes the sort order
   * inconsistent.
   */
  public static final int CYCLE_IN_TYPE_ORDER = 4;

  /** The JCas cannot be initialized. The following errors occurred: {0} */
  public static final int JCAS_INIT_ERROR = 5;

  /** Type information from the CAS cannot be accessed while initializing the JCas type {0} */
  public static final int JCAS_TYPENOTFOUND_ERROR = 6;

  /**
   * Feature information from the CAS cannot be accessed while initializing the JCAS type {0} with
   * feature {1}.
   */
  public static final int JCAS_FEATURENOTFOUND_ERROR = 7;

  /**
   * The JCAS range type {2} for feature {1} of type {0} does not match the CAS range type {3} for
   * the feature.
   */
  public static final int JCAS_FEATURE_WRONG_TYPE = 8;

  /** The type sort order cannot be built because type {0} is unknown. */
  public static final int TYPEORDER_UNKNOWN_TYPE = 9;

  /** Type system has not been committed; cannot create base index. */
  public static final int MUST_COMMIT_TYPE_SYSTEM = 10;

  private static final String[] identifiers = { "INAPPROP_TYPE_EXCEPTION", "UNDEFINED_FEATURE",
      "CANT_CREATE_BUILTIN_FS", "NOT_A_STRING", "CYCLE_IN_TYPE_ORDER", "JCAS_INIT_ERROR",
      "JCAS_TYPENOTFOUND_ERROR", "JCAS_FEATURENOTFOUND_ERROR", "JCAS_FEATURE_WRONG_TYPE",
      "TYPEORDER_UNKNOWN_TYPE", "MUST_COMMIT_TYPE_SYSTEM" };

  private int error;

  private ResourceBundle resource = null;

  private String[] arguments = new String[9];

  /**
   * Create a new <code>CASException</code>
   * 
   * @param error
   *          The error code.
   */
  public CASException(int error) {
    this.error = error;
  }

  /**
   * @return The error code for the exception. This may be useful when the error needs to be handed
   *         over language boundaries. Instead of handing over the complete exception object, return
   *         the error code, and the receiving application can look up the error in the message
   *         file. Unfortunately, the error parameters get lost that way.
   */
  public int getError() {
    return this.error;
  }

  /**
   * @return The message of the exception. Useful for including the text in another exception.
   */
  public String getMessage() {
    if (this.resource == null) {
      try {
        this.resource = ResourceBundle.getBundle(resource_file);
      } catch (MissingResourceException e) {
        this.error = MESSAGES_NOT_FOUND;
        return missing_resource_error;
      }
    }
    // Retrieve message from resource bundle, format using arguments,
    // and return resulting string.
    return (new MessageFormat(this.resource.getString(identifiers[this.error])))
                    .format(this.arguments);
  }

  /** @return The same as getMessage(), but prefixed with <code>"CASException: "</code>. */
  public String toString() {
    return "CASException: " + this.getMessage();
  }

  /**
   * Add an argument to a <code>CASException</code> object. Excess arguments will be ignored, and
   * missing arguments will have the value <code>null</code>. Add arguments in the order in which
   * they are specified in the message catalog (i.e. add %1 first, %2 next, and so on). Adding a
   * <code>null String</code> has no effect! So if you don't know the value of an argument, use
   * something like <code>""</code> or <code>"UNKNOWN"</code>, but not <code>null</code>.
   */
  public boolean addArgument(String s) {
    int i = 0;
    while (i < this.arguments.length) {
      if (this.arguments[i] == null) {
        this.arguments[i] = s;
        return true;
      }
      i++;
    }
    return false;
  }

  /**
   * Get the string identifier for this exception.
   * 
   * @return The internal message key.
   */
  public String getMessageCode() {
    return identifiers[this.error];
  }

  /**
   * Get the arguments to the exception string.
   * 
   * @return The arguments to the exception.
   */
  public String[] getArguments() {
    return this.arguments;
  }

  /**
   * Get the short name of the message bundle, i.e., the name without the package prefix.
   * 
   * @return The short name of the message bundle.
   */
  public String getBundleShortName() {
    if (resource_file.indexOf('.') >= 0) {
      return resource_file.substring(resource_file.lastIndexOf('.') + 1);
    }
    return resource_file;
  }

  /**
   * Gets the base name of the resource bundle in which the message for this exception is located.
   */
  public String getResourceBundleName() {
    return resource_file;
  }

  /**
   * Gets the identifier for this exception's message.
   * 
   */
  public String getMessageKey() {
    if (this.error >= 0 && this.error < identifiers.length) {
      return identifiers[this.error];
    }
    return null;
  }

}
