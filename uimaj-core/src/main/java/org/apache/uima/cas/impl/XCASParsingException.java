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
public class XCASParsingException extends org.xml.sax.SAXParseException {

  private static final long serialVersionUID = -4915068920369582184L;

  private static final String resource_file = "org.apache.uima.cas.impl.xcas";

  private static final String missing_resource_error = "Could not load message catalog: "
          + resource_file;

  private static final int MESSAGES_NOT_FOUND = -1;

  /**
   * Error parsing XCAS from source {0} at line {1}, column {2}: expected &lt;CAS&gt; root tag but found:
   * &lt;{3}&gt;.
   */
  public static final int WRONG_ROOT_TAG = 0;

  /**
   * Error parsing XCAS from source {0} at line {1}, column {2}: expected text but found element:
   * &lt;{3}&gt;.
   */
  public static final int TEXT_EXPECTED = 1;

  /**
   * Error parsing XCAS from source {0} at line {1}, column {2}: expected array element &lt;i&gt; but
   * found: &lt;{3}&gt;.
   */
  public static final int ARRAY_ELE_EXPECTED = 2;

  /**
   * Error parsing XCAS from source {0} at line {1}, column {2}: array element &lt;i&gt; may not have
   * attributes.
   */
  public static final int ARRAY_ELE_ATTRS = 3;

  /** Error parsing XCAS from source {0} at line {1}, column {2}: unknown type: {3}. */
  public static final int UNKNOWN_TYPE = 4;

  /**
   * Error parsing XCAS from source {0} at line {1}, column {2}: value of _id attribute must be
   * integer, but is: {3}.
   */
  public static final int ILLEGAL_ID = 5;

  /**
   * Error parsing XCAS from source {0} at line {1}, column {2}: size of array must be &gt;= 0, but is:
   * {3}.
   */
  public static final int ILLEGAL_ARRAY_SIZE = 6;

  /**
   * Error parsing XCAS from source {0} at line {1}, column {2}: unknown attribute for array type:
   * {3}.
   */
  public static final int ILLEGAL_ARRAY_ATTR = 7;

  /** Error parsing XCAS from source {0} at line {1}, column {2}: unknown feature: {3}. */
  public static final int UNKNOWN_FEATURE = 8;

  /**
   * Error parsing XCAS from source {0} at line {1}, column {2}: expected integer value, but found:
   * {3}.
   */
  public static final int INTEGER_EXPECTED = 9;

  /**
   * Error parsing XCAS from source {0} at line {1}, column {2}: expected float value, but found:
   * {3}.
   */
  public static final int FLOAT_EXPECTED = 10;

  /**
   * Error parsing XCAS from source {0} at line {1}, column {2}: number of array elements exceeds
   * specified array size.
   */
  public static final int EXCESS_ARRAY_ELE = 11;

  /**
   * Error parsing XMI-CAS from source {0} at line {1}, column {2}: xmi id {3} is referenced but not
   *  defined.
   */
  public static final int UNKNOWN_ID = 12;

  private static final String[] identifiers = { "WRONG_ROOT_TAG", "TEXT_EXPECTED",
      "ARRAY_ELE_EXPECTED", "ARRAY_ELE_ATTRS", "UNKNOWN_TYPE", "ILLEGAL_ID", "ILLEGAL_ARRAY_SIZE",
      "ILLEGAL_ARRAY_ATTR", "UNKNOWN_FEATURE", "INTEGER_EXPECTED", "FLOAT_EXPECTED",
      "EXCESS_ARRAY_ELE", "UNKNOWN_ID" };

  private int error;

  private ResourceBundle resource = null;

  private Object[] arguments = new Object[9];

  /**
   * Create a new <code>XCASParsingException</code>
   * 
   * @param error
   *          The error code.
   */
  public XCASParsingException(int error) {
    super(null, null);
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

  /** @return The same as getMessage(), but prefixed with <code>"XCASParsingException: "</code>. */
  public String toString() {
    return "XCASParsingException: " + this.getMessage();
  }

  /**
   * Add an argument to a <code>XCASParsingException</code> object. Excess arguments will be
   * ignored, and missing arguments will have the value <code>null</code>. Add arguments in the
   * order in which they are specified in the message catalog (i.e. add %1 first, %2 next, and so
   * on). Adding a <code>null String</code> has no effect! So if you don't know the value of an
   * argument, use something like <code>""</code> or <code>"UNKNOWN"</code>, but not
   * <code>null</code>.
   * @param s the argument to add
   * @return true if the argument was added, false if the argument was already added or was out of range
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
