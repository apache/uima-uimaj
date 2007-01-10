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
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Exception class for package org.apache.uima.cas. Automatically generated from message catalog.
 */
public class CASRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 2597111625069248081L;

  private static final String resource_file = "org.apache.uima.cas.cas_runtime_exception";

  private static final String missing_resource_error = "Could not load message catalog: "
          + resource_file;

  private static final int MESSAGES_NOT_FOUND = -1;

  /** Can''t create FS of type "{0}" with this method. */
  public static final int NON_CREATABLE_TYPE = 0;

  /** Array size must be >= 0. */
  public static final int ILLEGAL_ARRAY_SIZE = 1;

  /** Expected value of type "{0}", but found "{1}". */
  public static final int INAPPROP_TYPE = 2;

  /** Feature "{0}" is not defined for type "{1}". */
  public static final int INAPPROP_FEAT = 3;

  /** Trying to access value of feature "{0}" as "{1}", but range of feature is "{2}". */
  public static final int INAPPROP_RANGE = 4;

  /** Trying to access value of feature "{0}" as feature structure, but is primitive type. */
  public static final int PRIMITIVE_VAL_FEAT = 5;

  /** Error accessing type system: the type system has not been committed. */
  public static final int TYPESYSTEM_NOT_LOCKED = 6;

  /** Error setting string value: string "{0}" is not valid for a value of type "{1}". */
  public static final int ILLEGAL_STRING_VALUE = 7;

  /** Error applying FS constraint: no type "{0}" in current type system. */
  public static final int UNKNOWN_CONSTRAINT_TYPE = 8;

  /** Error applying FS constraint: no feature "{0}" in current type system. */
  public static final int UNKNOWN_CONSTRAINT_FEAT = 9;

  /** Error accessing child node in tree, index out of range. */
  public static final int CHILD_INDEX_OOB = 10;

  /** CAS type system doesn''t match JCas Type definition for type "{0}". */
  public static final int JCAS_CAS_MISMATCH = 11;

  /** JCas type "{0}" used in Java code, but was not declared in the XML type descriptor. */
  public static final int JCAS_TYPE_NOT_IN_CAS = 12;

  /**
   * Unknown JCas type used in Java code but was not declared or imported in the XML descriptor for
   * this component.
   */
  public static final int JCAS_UNKNOWN_TYPE_NOT_IN_CAS = 13;

  /** JCas getNthElement method called via invalid object - an empty list: {0}. */
  public static final int JCAS_GET_NTH_ON_EMPTY_LIST = 14;

  /** JCas getNthElement method called with index "{0}" which is negative. */
  public static final int JCAS_GET_NTH_NEGATIVE_INDEX = 15;

  /** JCas getNthElement method called with index "{0}" larger than the length of the list. */
  public static final int JCAS_GET_NTH_PAST_END = 16;

  /**
   * JCas is referencing via a JFSIterator or get method, a type, "{0}", which has no JCAS class
   * model. You must use FSIterator instead of JFSIterator.
   */
  public static final int JCAS_OLDSTYLE_REF_TO_NONJCAS_TYPE = 17;

  /**
   * A CAS iterator or createFS call is trying to make an instance of type "{0}", but that type has
   * been declared "abstract" in JCas, and no instances are allowed to be made.
   */
  public static final int JCAS_MAKING_ABSTRACT_INSTANCE = 18;

  /**
   * The method "{0}" is not supported by this JCAS because it is not associated with a TCAS view of
   * a CAS, but rather just with a base CAS.
   */
  public static final int JCAS_UNSUPPORTED_OP_NOT_TCAS = 19;

  /** A sofaFS with name {0} has already been created. */
  public static final int SOFANAME_ALREADY_EXISTS = 20;

  /** Data for Sofa feature {0} has already been set. */
  public static final int SOFADATA_ALREADY_SET = 21;

  /** No sofaFS with name {0} found. */
  public static final int SOFANAME_NOT_FOUND = 22;

  /** No sofaFS for specified sofaRef found. */
  public static final int SOFAREF_NOT_FOUND = 23;

  /** Can''t use standard set methods with SofaFS features. */
  public static final int PROTECTED_SOFA_FEATURE = 24;

  /** The JCAS cover class "{0}" could not be loaded. */
  public static final int JCAS_MISSING_COVERCLASS = 25;

  /** The feature path "{0}" is not valid. */
  public static final int INVALID_FEATURE_PATH = 26;

  /** The feature path does not end in a primitive valued feature. */
  public static final int NO_PRIMITIVE_TAIL = 27;

  /** Error trying to do binary serialization of CAS data and write the BLOB to an output stream. */
  public static final int BLOB_SERIALIZATION = 28;

  /** Error trying to read BLOB data from an input stream and deserialize into a CAS. */
  public static final int BLOB_DESERIALIZATION = 29;

  /** Error trying to open a stream to Sofa data. */
  public static final int SOFADATASTREAM_ERROR = 30;

  /** Can''t call method "{0}" on the base CAS. */
  public static final int INVALID_BASE_CAS_METHOD = 31;

  /**
   * Error - the Annotation "{0}" is over view "{1}" and cannot be added to indexes associated with
   * the different view "{2}".
   */
  public static final int ANNOTATION_IN_WRONG_INDEX = 32;

  /**
   * Error accessing index "{0}" for type "{1}".  Index "{0}" is over type "{2}", which is not a supertype of "{1}".
   */
  public static final int TYPE_NOT_IN_INDEX = 33;
  
  private static final String[] identifiers = { "NON_CREATABLE_TYPE", "ILLEGAL_ARRAY_SIZE",
      "INAPPROP_TYPE", "INAPPROP_FEAT", "INAPPROP_RANGE", "PRIMITIVE_VAL_FEAT",
      "TYPESYSTEM_NOT_LOCKED", "ILLEGAL_STRING_VALUE", "UNKNOWN_CONSTRAINT_TYPE",
      "UNKNOWN_CONSTRAINT_FEAT", "CHILD_INDEX_OOB", "JCAS_CAS_MISMATCH", "JCAS_TYPE_NOT_IN_CAS",
      "JCAS_UNKNOWN_TYPE_NOT_IN_CAS", "JCAS_GET_NTH_ON_EMPTY_LIST", "JCAS_GET_NTH_NEGATIVE_INDEX",
      "JCAS_GET_NTH_PAST_END", "JCAS_OLDSTYLE_REF_TO_NONJCAS_TYPE",
      "JCAS_MAKING_ABSTRACT_INSTANCE", "JCAS_UNSUPPORTED_OP_NOT_TCAS", "SOFANAME_ALREADY_EXISTS",
      "SOFADATA_ALREADY_SET", "SOFANAME_NOT_FOUND", "SOFAREF_NOT_FOUND", "PROTECTED_SOFA_FEATURE",
      "JCAS_MISSING_COVERCLASS", "INVALID_FEATURE_PATH", "NO_PRIMITIVE_TAIL", "BLOB_SERIALIZATION",
      "BLOB_DESERIALIZATION", "SOFADATASTREAM_ERROR", "INVALID_BASE_CAS_METHOD",
      "ANNOTATION_IN_WRONG_INDEX", "TYPE_NOT_IN_INDEX" };

  private int error;

  private ResourceBundle resource = null;

  private Object[] arguments = new Object[9];

  /**
   * Create a new <code>CASRuntimeException</code>
   * 
   * @param error
   *          The error code.
   */
  public CASRuntimeException(int error) {
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

  /** @return The same as getMessage(), but prefixed with <code>"CASRuntimeException: "</code>. */
  public String toString() {
    return "CASRuntimeException: " + this.getMessage();
  }

  /**
   * Add an argument to a <code>CASRuntimeException</code> object. Excess arguments will be
   * ignored, and missing arguments will have the value <code>null</code>. Add arguments in the
   * order in which they are specified in the message catalog (i.e. add %1 first, %2 next, and so
   * on). Adding a <code>null String</code> has no effect! So if you don't know the value of an
   * argument, use something like <code>""</code> or <code>"UNKNOWN"</code>, but not
   * <code>null</code>.
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
