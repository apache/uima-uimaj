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

package org.apache.uima.internal.util;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Exception class for package org.apache.uima.util. Automatically generated from message catalog.
 */
public class UtilError extends RuntimeException {

  private static final long serialVersionUID = -6708991281527442356L;

  private static final String resource_file = "org.apache.uima.util.UtilError";

  private static final String missing_resource_error = "Could not load message catalog: "
          + resource_file;

  private static final int MESSAGES_NOT_FOUND = -1;

  /** Out of bounds error while retrieving daughter node. */
  public static final int ILLEGAL_DTR_INDEX = 0;

  /** Index out of bounds error while retrieving successor node. */
  public static final int ILLEGAL_SUCCESSOR_INDEX = 1;

  /** Index out of bounds error while retrieving predecessor node. */
  public static final int ILLEGAL_PREDECESSOR_INDEX = 2;

  private static final String[] identifiers = { "ILLEGAL_DTR_INDEX", "ILLEGAL_SUCCESSOR_INDEX",
      "ILLEGAL_PREDECESSOR_INDEX" };

  private int error;

  private ResourceBundle resource = null;

  private Object[] arguments = new Object[9];

  /**
   * Create a new <code>UtilError</code>
   * 
   * @param error
   *          The error code.
   */
  public UtilError(int error) {
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

  /**
   * @return The same as getMessage(), but prefixed with <code>"UtilError: "</code>.
   */
  public String toString() {
    return "UtilError: " + this.getMessage();
  }

  /**
   * Add an argument to a <code>UtilError</code> object. Excess arguments will be ignored, and
   * missing arguments will have the value <code>null</code>. Add arguments in the order in which
   * they are specified in the message catalog (i.e. add %1 first, %2 next, and so on). Adding a
   * <code>null String</code> has no effect! So if you don't know the value of an argument, use
   * something like <code>""</code> or <code>"UNKNOWN"</code>, but not <code>null</code>.
   * @param s -
   * @return -
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

}
