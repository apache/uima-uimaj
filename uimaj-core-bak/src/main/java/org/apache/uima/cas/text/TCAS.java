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

package org.apache.uima.cas.text;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;

/** 
 * TCAS: Text Common Analysis System.
 * <p>
 * This interface exists only for backwards-compatiblity with UIMA SDK v1.x.
 * New applications should use the {@link CAS} or {@link JCAS} interface.
 * <p>
 * Historially, this interface contained convenience methods for text analysis.
 * In v2.0, all methods and constants on this interface were moved to the
 * {@link CAS} interface. 
 */
public interface TCAS extends CAS {

  /**
   * Name of TCAS name space. 
   */
  static final String NAME_SPACE_UIMA_TCAS =
    "uima" + TypeSystem.NAMESPACE_SEPARATOR + "tcas";
  
  /**
   * Name of annotation type.
   */
  static final String TYPE_NAME_ANNOTATION =
    NAME_SPACE_UIMA_TCAS + TypeSystem.NAMESPACE_SEPARATOR + "Annotation";

  /**
   * Name of document annotation type.
   */
  static final String TYPE_NAME_DOCUMENT_ANNOTATION =
    NAME_SPACE_UIMA_TCAS + TypeSystem.NAMESPACE_SEPARATOR + "DocumentAnnotation";

  /**
   * Sofa ID feature that is the handle to a text Sofa.
   */
  static final String FEATURE_BASE_NAME_SOFA = "sofa";
  /**
   * Base name of annotation begin feature.
   */
  static final String FEATURE_BASE_NAME_BEGIN = "begin";
  /**
   * Base name of annotation end feature.
   */
  static final String FEATURE_BASE_NAME_END = "end";
  
  /**
   * Base name of document language feature.
   */
  static final String FEATURE_BASE_NAME_LANGUAGE = "language";

  /**
   * Fully qualified name of annotation begin feature.
   */
  static final String FEATURE_FULL_NAME_BEGIN =
    TYPE_NAME_ANNOTATION
      + TypeSystem.FEATURE_SEPARATOR
      + FEATURE_BASE_NAME_BEGIN;

  /**
   * Fully qualified name of annotation sofa feature.
   */
  static final String FEATURE_FULL_NAME_SOFA =
    TYPE_NAME_ANNOTATION
      + TypeSystem.FEATURE_SEPARATOR
      + FEATURE_BASE_NAME_SOFA;
      
  /**
   * Fully qualified name of annotation end feature.
   */
  static final String FEATURE_FULL_NAME_END =
    TYPE_NAME_ANNOTATION
      + TypeSystem.FEATURE_SEPARATOR
      + FEATURE_BASE_NAME_END;

  /**
   * Fully qualified name of document language feature.
   */
  static final String FEATURE_FULL_NAME_LANGUAGE =
    TYPE_NAME_DOCUMENT_ANNOTATION
      + TypeSystem.FEATURE_SEPARATOR
      + FEATURE_BASE_NAME_LANGUAGE;

  ////////////////////////////////////////////////////////////
  // Feature names

  /**
   * Name of the standard index on annotations.  The standard index is
   * automatically defined if you use a TCAS.
   */
  static final String STD_ANNOTATION_INDEX = "AnnotationIndex";
  
  static final String DEFAULT_LANGUAGE_NAME = "x-unspecified";
}
