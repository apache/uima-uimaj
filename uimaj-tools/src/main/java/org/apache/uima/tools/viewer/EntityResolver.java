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
package org.apache.uima.tools.viewer;

import org.apache.uima.jcas.tcas.Annotation;

/**
 * Pluggable interface that supports Entity View mode in the CasAnnotationViewer. Users implement
 * this interface with logic that for their particular type system can return the canonical form of
 * an annotation in the CAS.
 * <p>
 * In the viewer, all annotations whose canonical form strings are <code>equal</code> will be
 * displayed in the same color, and the canonical form will be shown in the legend.
 */
public interface EntityResolver {

  /**
   * Returns the canonical form String for an annotation.
   * <p>
   * For two annotations that refer to the same Entity, this should return canonical form strings that
   * are <code>equal</code>.
   * <p>
   * If the annotation does not represent an entity at all, <code>null</code> should be returned.
   * 
   * @param aAnnotation
   *          the annotation to resolve
   * 
   * @return the canonical form of the annotation, null if the annotation does not represent an
   *         entity
   */
  String getCanonicalForm(Annotation aAnnotation);
}
