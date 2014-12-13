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
public class LowLevelException extends RuntimeException {

  private static final long serialVersionUID = -3115614380676282900L;

  private static final String resource_file = "org.apache.uima.cas.impl.ll_runtimeException";

  private static final String missing_resource_error = "Could not load message catalog: "
          + resource_file;

  private static final int MESSAGES_NOT_FOUND = -1;

  /** Error in low-level CAS APIs: attempted heap access with invalid FS reference: {0}. */
  public static final int INVALID_FS_REF = 0;

  /**
   * Error in low-level CAS APIs: attempt to interpret heap value {0} at {1} as type code, but {0}
   * is not a valid type code. This is likely caused by a bad FS reference.
   */
  public static final int VALUE_NOT_A_TYPE = 1;

  /** Error in low-level CAS APIs: {0} is not a valid feature code. */
  public static final int INVALID_FEATURE_CODE = 2;

  /** Error in low-level CAS APIs: type "{1}" (code: {0}) does not define feature "{3}" (code: {2}). */
  public static final int FEAT_DOM_ERROR = 3;

  /**
   * Error in low-level CAS APIs: feature "{1}" (code: {0}) does not take values of type "{3}"
   * (code: {2}).
   */
  public static final int FEAT_RAN_ERROR = 4;

  /**
   * Error in low-level CAS APIs: trying to access value of feature "{1}" (code: {0}) as FS
   * reference, but range of feature is "{2}".
   */
  public static final int FS_RAN_TYPE_ERROR = 5;

  /**
   * Error in low-level CAS APIs: trying to access value FS reference {0} as type "{2}" (code: {1}),
   * but is "{3}".
   */
  public static final int ACCESS_TYPE_ERROR = 6;

  /** Error in low-level CAS APIs: array index out of range: {0}. */
  public static final int ARRAY_INDEX_OUT_OF_RANGE = 7;

  /** Error in low-level CAS APIs: array index and or length out of range. index: {0}, length: {1}. */
  public static final int ARRAY_INDEX_LENGTH_OUT_OF_RANGE = 8;

  /** Error in low-level CAS APIs: can't create FS reference for type code {0}. */
  public static final int CREATE_FS_OF_TYPE_ERROR = 9;

  /** Error in low-level CAS APIs: trying to access index for invalid type code: {0}. */
  public static final int INVALID_INDEX_TYPE = 10;

  /**
   * Error in low-level CAS APIs: can't create array of type "{1}" (code: {0}). Must be a valid
   * (built-in) array type.
   */
  public static final int CREATE_ARRAY_OF_TYPE_ERROR = 11;

  /** Error in low-level CAS APIs: illegal array length specified: {0}. */
  public static final int ILLEGAL_ARRAY_LENGTH = 12;

  /** Error in low-level CAS APIs: illegal type code argument: {0}. */
  public static final int INVALID_TYPE_ARGUMENT = 13;

  /**
   * Error in low-level CAS APIs: attempting to access element {0} of array but array has null
   * value.
   */
  public static final int NULL_ARRAY_ACCESS = 14;

  private static final String[] identifiers = { "INVALID_FS_REF", "VALUE_NOT_A_TYPE",
      "INVALID_FEATURE_CODE", "FEAT_DOM_ERROR", "FEAT_RAN_ERROR", "FS_RAN_TYPE_ERROR",
      "ACCESS_TYPE_ERROR", "ARRAY_INDEX_OUT_OF_RANGE", "ARRAY_INDEX_LENGTH_OUT_OF_RANGE",
      "CREATE_FS_OF_TYPE_ERROR", "INVALID_INDEX_TYPE", "CREATE_ARRAY_OF_TYPE_ERROR",
      "ILLEGAL_ARRAY_LENGTH", "INVALID_TYPE_ARGUMENT", "NULL_ARRAY_ACCESS" };

  private int error;

  private ResourceBundle resource = null;

  private Object[] arguments = new Object[9];

  /**
   * Create a new <code>LowLevelException</code>
   * 
   * @param error
   *          The error code.
   */
  public LowLevelException(int error) {
    this.error = error;
  }

  /**
   * @return The error code for the exception. This may be useful when the error needs to be handed
   *         over language boundaries. Instead of handing over the complete exception object, return
   *         the error code, and the receiving application can look up the error in the message
   *         file. Unfortunately, the error parameters get lost that way.
   */
  public int getError() {
    return error;
  }

  /**
   * @return The message of the exception. Useful for including the text in another exception.
   */
  public String getMessage() {
    if (resource == null) {
      try {
        resource = ResourceBundle.getBundle(resource_file);
      } catch (MissingResourceException e) {
        error = MESSAGES_NOT_FOUND;
        return missing_resource_error;
      }
    }
    // Retrieve message from resource bundle, format using arguments,
    // and return resulting string.
    return (new MessageFormat(resource.getString(identifiers[error]))).format(arguments);
  }

  /** @return The same as getMessage(), but prefixed with <code>"LowLevelException: "</code>. */
  public String toString() {
    return "LowLevelException: " + this.getMessage();
  }

  /**
   * Add an argument to a <code>LowLevelException</code> object. Excess arguments will be ignored,
   * and missing arguments will have the value <code>null</code>. Add arguments in the order in
   * which they are specified in the message catalog (i.e. add %1 first, %2 next, and so on). Adding
   * a <code>null String</code> has no effect! So if you don't know the value of an argument, use
   * something like <code>""</code> or <code>"UNKNOWN"</code>, but not <code>null</code>.
   * @param s -
   * @return -
   */
  public boolean addArgument(String s) {
    int i = 0;
    while (i < arguments.length) {
      if (arguments[i] == null) {
        arguments[i] = s;
        return true;
      }
      i++;
    }
    return false;
  }

}
