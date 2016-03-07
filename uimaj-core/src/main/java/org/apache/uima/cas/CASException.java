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

import org.apache.uima.UIMAException;

/**
 * Exception class for package org.apache.uima.cas. Automatically generated from message catalog.
 */
public class CASException extends UIMAException {

  private static final long serialVersionUID = 2990279532203726966L;

  /** The value of the feature {0} cannot be accessed as type {1}, because it is {2}. */
  public static final String INAPPROP_TYPE_EXCEPTION = "INAPPROP_TYPE_EXCEPTION";

  /**
   * The value of the feature {0} on the structure of type {1} cannot be accessed, because {0} is
   * not defined for {1}.
   */
  public static final String UNDEFINED_FEATURE = "UNDEFINED_FEATURE";

  /**
   * The feature structure of type {0} cannot be created. Structures of this type cannot be created
   * directly.
   */
  public static final String CANT_CREATE_BUILTIN_FS = "CANT_CREATE_BUILTIN_FS";

  /** The structure of type {0} cannot be accessed as a String. */
  public static final String NOT_A_STRING = "NOT_A_STRING";

  /**
   * The types are added in the wrong sort order. Adding {0} &lt; {1} makes the sort order
   * inconsistent.
   */
  public static final String CYCLE_IN_TYPE_ORDER = "CYCLE_IN_TYPE_ORDER";

  /** The JCas cannot be initialized. The following errors occurred: {0} */
  public static final String JCAS_INIT_ERROR = "JCAS_INIT_ERROR";

  /** Type information from the CAS cannot be accessed while initializing the JCas type {0} */
  public static final String JCAS_TYPENOTFOUND_ERROR = "JCAS_TYPENOTFOUND_ERROR";

  /**
   * JCas Type "{0}" implements getters and setters for feature "{1}", but the type system doesn't define that feature.
   */
  public static final String JCAS_FEATURENOTFOUND_ERROR = "JCAS_FEATURENOTFOUND_ERROR";

  /**
   * The JCAS range type {2} for feature {1} of type {0} does not match the CAS range type {3} for
   * the feature.
   */
  public static final String JCAS_FEATURE_WRONG_TYPE = "JCAS_FEATURE_WRONG_TYPE";

  /** The type sort order cannot be built because type {0} is unknown. */
  public static final String TYPEORDER_UNKNOWN_TYPE = "TYPEORDER_UNKNOWN_TYPE";

  /** Type system has not been committed; cannot create base index. */
  public static final String MUST_COMMIT_TYPE_SYSTEM = "MUST_COMMIT_TYPE_SYSTEM";

  public CASException() {
    super();
  }

  public CASException(String aMessageKey, Object[] aArguments, Throwable aCause) {
    super(aMessageKey, aArguments, aCause);
  }

  public CASException(String aMessageKey, Object[] aArguments) {
    super(aMessageKey, aArguments);
  }

  public CASException(String aResourceBundleName, String aMessageKey, Object[] aArguments, Throwable aCause) {
    super(aResourceBundleName, aMessageKey, aArguments, aCause);
  }

  public CASException(String aResourceBundleName, String aMessageKey, Object[] aArguments) {
    super(aResourceBundleName, aMessageKey, aArguments);
  }

  public CASException(Throwable aCause) {
    super(aCause);
  }

  

}
