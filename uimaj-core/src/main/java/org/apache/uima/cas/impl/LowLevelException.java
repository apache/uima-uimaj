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

import org.apache.uima.UIMARuntimeException;

/**
 * Exception class for package org.apache.uima.cas.impl. Automatically generated from message
 * catalog.
 */
public class LowLevelException extends UIMARuntimeException {

  private static final long serialVersionUID = -3115614380676282900L;

  private static final String resource_file = "org.apache.uima.cas.impl.ll_runtimeException";

  /** Error in low-level CAS APIs: accessing FS with id {0}, but no such FS exists in this CAS. */
  public static final String INVALID_FS_REF = "INVALID_FS_REF";

  /**
   * Error in low-level CAS APIs: attempt to interpret heap value {0} at {1} as type code, but {0}
   * is not a valid type code. This is likely caused by a bad FS reference.
   */
  public static final String VALUE_NOT_A_TYPE = "VALUE_NOT_A_TYPE";

  /** Error in low-level CAS APIs: {0} is not a valid feature code. */
  public static final String INVALID_FEATURE_CODE = "INVALID_FEATURE_CODE";

  /**
   * Error in low-level CAS APIs: type "{1}" (code: {0}) does not define feature "{3}" (code: {2}).
   */
  public static final String FEAT_DOM_ERROR = "FEAT_DOM_ERROR";

  /**
   * Error in low-level CAS APIs: feature "{1}" (code: {0}) does not take values of type "{3}"
   * (code: {2}).
   */
  public static final String FEAT_RAN_ERROR = "FEAT_RAN_ERROR";

  /**
   * Error in low-level CAS APIs: trying to access value of feature "{1}" (code: {0}) as FS
   * reference, but range of feature is "{2}".
   */
  public static final String FS_RAN_TYPE_ERROR = "FS_RAN_TYPE_ERROR";

  /**
   * Error in low-level CAS APIs: trying to access value FS reference {0} as type "{2}" (code: {1}),
   * but is "{3}".
   */
  public static final String ACCESS_TYPE_ERROR = "ACCESS_TYPE_ERROR";

  /** Error in low-level CAS APIs: array index out of range: {0}. */
  public static final String ARRAY_INDEX_OUT_OF_RANGE = "ARRAY_INDEX_OUT_OF_RANGE";

  /**
   * Error in low-level CAS APIs: array index and or length out of range. index: {0}, length: {1}.
   */
  public static final String ARRAY_INDEX_LENGTH_OUT_OF_RANGE = "ARRAY_INDEX_LENGTH_OUT_OF_RANGE";

  /** Error in low-level CAS APIs: can't create FS reference for type code {0}. */
  public static final String CREATE_FS_OF_TYPE_ERROR = "CREATE_FS_OF_TYPE_ERROR";

  /** Error in low-level CAS APIs: trying to access index for invalid type code: {0}. */
  public static final String INVALID_INDEX_TYPE = "INVALID_INDEX_TYPE";

  /**
   * Error in low-level CAS APIs: can't create array of type "{1}" (code: {0}). Must be a valid
   * (built-in) array type.
   */
  public static final String CREATE_ARRAY_OF_TYPE_ERROR = "CREATE_ARRAY_OF_TYPE_ERROR";

  /** Error in low-level CAS APIs: illegal array length specified: {0}. */
  public static final String ILLEGAL_ARRAY_LENGTH = "ILLEGAL_ARRAY_LENGTH";

  /** Error in low-level CAS APIs: illegal type code argument: {0}. */
  public static final String INVALID_TYPE_ARGUMENT = "INVALID_TYPE_ARGUMENT";

  /** Invalid Type Code value: {0}. */
  public static final String INVALID_TYPECODE = "INVALID_TYPECODE";

  /**
   * Error in low-level CAS APIs: attempting to access element {0} of array but array has null
   * value.
   */
  public static final String NULL_ARRAY_ACCESS = "NULL_ARRAY_ACCESS";

  public LowLevelException(String aMessageKey, Object... aArguments) {
    super(aMessageKey, aArguments);
  }

  public LowLevelException(String aMessageKey, Object[] aArguments, Throwable aCause) {
    super(aCause, aMessageKey, aArguments);
  }

  /** @return The same as getMessage(), but prefixed with <code>"LowLevelException: "</code>. */
  @Override
  public String toString() {
    return "LowLevelException: " + getMessage();
  }

  /**
   * Gets the base name of the resource bundle in which the message for this exception is located.
   * 
   * @return the resource bundle base name. May return <code>null</code> if this exception has no
   *         message.
   */
  @Override
  public String getResourceBundleName() {
    return resource_file;
  }

}
