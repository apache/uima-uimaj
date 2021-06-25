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

import org.apache.uima.UIMARuntimeException;

public class CASAdminException extends UIMARuntimeException {

  private static final long serialVersionUID = 1L;

  private static String DEFAULT_RESOURCE_BUNDLE_NAME = "org.apache.uima.cas.admin.admin_errors";

  /** Can't add index to a committed repository. */
  public static final String REPOSITORY_LOCKED = "REPOSITORY_LOCKED";

  /** Type system is committed; can't add types or features. */
  public static final String TYPE_SYSTEM_LOCKED = "TYPE_SYSTEM_LOCKED";

  /** Type system has not been committed; can't create index repository. */
  public static final String MUST_COMMIT_TYPE_SYSTEM = "MUST_COMMIT_TYPE_SYSTEM";

  /** Index repository has not been committed; can't create CAS. */
  public static final String MUST_COMMIT_INDEX_REPOSITORY = "MUST_COMMIT_INDEX_REPOSITORY";

  /**
   * Invalid type name "{0}". Type names must start with a letter and consist only of letters,
   * digits, or underscores.
   */
  public static final String BAD_TYPE_SYNTAX = "BAD_TYPE_SYNTAX";

  /**
   * Invalid feature name "{0}". Feature names must start with a letter and consist only of letters,
   * digits, or underscores.
   */
  public static final String BAD_FEATURE_SYNTAX = "BAD_FEATURE_SYNTAX";

  /** Can't derive from type "{0}" since it is inheritance final. */
  public static final String TYPE_IS_INH_FINAL = "TYPE_IS_INH_FINAL";

  /** Can't add feature to type "{0}" since it is feature final. */
  public static final String TYPE_IS_FEATURE_FINAL = "TYPE_IS_FEATURE_FINAL";

  /** Error deserializing type system. */
  public static final String DESERIALIZATION_ERROR = "DESERIALIZATION_ERROR";

  /** Can't flush CAS, flushing is disabled. */
  public static final String FLUSH_DISABLED = "FLUSH_DISABLED";

  /** {0} */
  public static final String JCAS_ERROR = "JCAS_ERROR";

  /**
   * Trying to define feature "{0}" on type "{1}" with range "{2}", but feature has already been
   * defined on (super)type "{3}" with range "{4}".
   */
  public static final String DUPLICATE_FEATURE = "DUPLICATE_FEATURE";

  /**
   * Trying to define type "{0}", but this type has already been defined as "{1}".
   */
  public static final String DUPLICATE_TYPE = "DUPLICATE_TYPE";

  /**
   * Tried to obtain a UIMA Array type for component "{0}", but no such array type is defined.
   */
  public static final String MISSING_ARRAY_TYPE_FOR_COMPONENT = "MISSING_ARRAY_TYPE_FOR_COMPONENT";

  /**
   * Can't define a Subtype of String "{0}" with the same name as an existing non String Subtype
   * "{1}".
   */
  public static final String STRING_SUBTYPE_REDEFINE_NAME_CONFLICT = "STRING_SUBTYPE_REDEFINE_NAME_CONFLICT";

  /**
   * Can't define a Subtype of String "{0}" with allowed Values "{1}", has the same name as an
   * existing String Subtype with different allowed values "{2}".
   */
  public static final String STRING_SUBTYPE_CONFLICTING_ALLOWED_VALUES = "STRING_SUBTYPE_CONFLICTING_ALLOWED_VALUES";

  /**
   * uima.allow_duplicate_add_to_indexes is not supported in UIMA Version 3 and later
   */
  public static final String INDEX_DUPLICATES_NOT_SUPPORTED = "INDEX_DUPLICATES_NOT_SUPPORTED";

  /**
   * Total number of UIMA types, {0}, exceeds the maximum of 32767.
   */
  public static final String TOO_MANY_TYPES = "TOO_MANY_TYPES";

  private String resourceBundleName = DEFAULT_RESOURCE_BUNDLE_NAME;

  public CASAdminException(String aResourceBundleName, Throwable aCause, String aMessageKey,
          Object... aArguments) {
    super(aCause, aResourceBundleName, aMessageKey, aArguments);
  }

  public CASAdminException(String aMessageKey, Object... aArguments) {
    super(aMessageKey, aArguments);
  }

  public CASAdminException(Throwable aCause, String aMessageKey, Object... aArguments) {
    super(aCause, aMessageKey, aArguments);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.UIMARuntimeException#getResourceBundleName()
   */
  @Override
  public String getResourceBundleName() {
    return resourceBundleName;
  }
}
