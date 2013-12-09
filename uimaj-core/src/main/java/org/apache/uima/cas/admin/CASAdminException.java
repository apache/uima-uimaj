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

package org.apache.uima.cas.admin;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Exception class for package org.apache.uima.cas.admin. Automatically generated from message
 * catalog.
 */
public class CASAdminException extends RuntimeException {

  private static final long serialVersionUID = 3968205828762300170L;

  private static final String resource_file = "org.apache.uima.cas.admin.admin_errors";

  private static final String missing_resource_error = "Could not load message catalog: "
          + resource_file;

  private static final int MESSAGES_NOT_FOUND = -1;

  /** Can't add index to a committed repository. */
  public static final int REPOSITORY_LOCKED = 0;

  /** Type system is committed; can't add types or features. */
  public static final int TYPE_SYSTEM_LOCKED = 1;

  /** Type system has not been committed; can't create index repository. */
  public static final int MUST_COMMIT_TYPE_SYSTEM = 2;

  /** Index repository has not been committed; can't create CAS. */
  public static final int MUST_COMMIT_INDEX_REPOSITORY = 3;

  /**
   * Invalid type name "{0}". Type names must start with a letter and consist only of letters,
   * digits, or underscores.
   */
  public static final int BAD_TYPE_SYNTAX = 4;

  /**
   * Invalid feature name "{0}". Feature names must start with a letter and consist only of letters,
   * digits, or underscores.
   */
  public static final int BAD_FEATURE_SYNTAX = 5;

  /** Can't derive from type "{0}" since it is inheritance final. */
  public static final int TYPE_IS_INH_FINAL = 6;

  /** Can't add feature to type "{0}" since it is feature final. */
  public static final int TYPE_IS_FEATURE_FINAL = 7;

  /** Error deserializing type system. */
  public static final int DESERIALIZATION_ERROR = 8;

  /** Can't flush CAS, flushing is disabled. */
  public static final int FLUSH_DISABLED = 9;

  /** {0} */
  public static final int JCAS_ERROR = 10;

  /**
   * Trying to define feature "{0}" on type "{1}" with range "{2}", but feature has already been
   * defined on (super)type "{3}" with range "{4}".
   */
  public static final int DUPLICATE_FEATURE = 11;

  private static final String[] identifiers = { "REPOSITORY_LOCKED", "TYPE_SYSTEM_LOCKED",
      "MUST_COMMIT_TYPE_SYSTEM", "MUST_COMMIT_INDEX_REPOSITORY", "BAD_TYPE_SYNTAX",
      "BAD_FEATURE_SYNTAX", "TYPE_IS_INH_FINAL", "TYPE_IS_FEATURE_FINAL", "DESERIALIZATION_ERROR",
      "FLUSH_DISABLED", "JCAS_ERROR", "DUPLICATE_FEATURE" };

  private int error;

  private ResourceBundle resource = null;

  private String[] arguments = new String[9];

  /**
   * Create a new <code>CASAdminException</code>
   * 
   * @param error
   *          The error code.
   */
  public CASAdminException(int error) {
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

  /** @return The same as getMessage(), but prefixed with <code>"CASAdminException: "</code>. */
  public String toString() {
    return "CASAdminException: " + this.getMessage();
  }

  /**
   * Add an argument to a <code>CASAdminException</code> object. Excess arguments will be ignored,
   * and missing arguments will have the value <code>null</code>. Add arguments in the order in
   * which they are specified in the message catalog (i.e. add %1 first, %2 next, and so on). Adding
   * a <code>null String</code> has no effect! So if you don't know the value of an argument, use
   * something like <code>""</code> or <code>"UNKNOWN"</code>, but not <code>null</code>.
   * @param s the argument to add
   * @return true if the argument was added
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

}
