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

package org.apache.uima.cas.impl;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Exception class for package org.apache.uima.cas.impl. Automatically generated from message
 * catalog.
 */
public class AnnotationImplException extends Exception {

  private static final long serialVersionUID = 2924905396059850361L;

  private static final String resource_file = "org.apache.uima.cas.impl.annot_impl";

  private static final String missing_resource_error = "Could not load message catalog: "
          + resource_file;

  private static final int MESSAGES_NOT_FOUND = -1;

  /**
   * Type system parsing error for file "{0}": couldn't add top type "{1}" at line {2}, column {3}.
   * Non-empty hierarchy?
   */
  public static final int CANT_ADD_TOP = 0;

  /** Error parsing types system file "{0}": expected {1} but found "{2}" at line {3}, column {4}. */
  public static final int PARSING_ERROR = 1;

  /**
   * Error parsing types system file "{0}": type "{1}" must be declared before it is used at line
   * {2}, column {3}.
   */
  public static final int UNKN_TYPE = 2;

  /**
   * Error parsing types system file "{0}": feature "{1}" could not be added at line {2}, column
   * {3}. Name already in use?
   */
  public static final int COULDNT_ADD_FEAT = 3;

  /**
   * Error parsing types system file "{0}": type "{1}" could not be added at line {2}, column {3}.
   * Name already in use?
   */
  public static final int COULDNT_ADD_TYPE = 4;

  /** Error printing type system: set docStream first. */
  public static final int NULL_DOCSTREAM = 5;

  private static final String[] identifiers = { "CANT_ADD_TOP", "PARSING_ERROR", "UNKN_TYPE",
      "COULDNT_ADD_FEAT", "COULDNT_ADD_TYPE", "NULL_DOCSTREAM" };

  private int error;

  private ResourceBundle resource = null;

  private String[] arguments = new String[9];

  /**
   * Create a new <code>AnnotationImplException</code>
   * 
   * @param error
   *          The error code.
   */
  public AnnotationImplException(int error) {
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

  /** @return The same as getMessage(), but prefixed with <code>"AnnotationImplException: "</code>. */
  public String toString() {
    return "AnnotationImplException: " + this.getMessage();
  }

  /**
   * Add an argument to a <code>AnnotationImplException</code> object. Excess arguments will be
   * ignored, and missing arguments will have the value <code>null</code>. Add arguments in the
   * order in which they are specified in the message catalog (i.e. add %1 first, %2 next, and so
   * on). Adding a <code>null String</code> has no effect! So if you don't know the value of an
   * argument, use something like <code>""</code> or <code>"UNKNOWN"</code>, but not
   * <code>null</code>.
   * @param s -
   * @return true if found a null spot to insert string s into
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
