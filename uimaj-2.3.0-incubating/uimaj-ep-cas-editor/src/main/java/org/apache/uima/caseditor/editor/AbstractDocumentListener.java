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

package org.apache.uima.caseditor.editor;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.uima.cas.FeatureStructure;

/**
 * TODO: add javadoc here
 */
public abstract class AbstractDocumentListener implements ICasDocumentListener {

  /**
   * Forwards the call.
   */
  public void added(FeatureStructure newAnnotation) {
    Collection<FeatureStructure> structures = new ArrayList<FeatureStructure>(1);

    structures.add(newAnnotation);

    added(structures);
  }

  /**
   * Forwards the call.
   */
  public void removed(FeatureStructure deletedAnnotation) {
    Collection<FeatureStructure> structures = new ArrayList<FeatureStructure>(1);

    structures.add(deletedAnnotation);

    removed(structures);
  }

  /**
   * Forwards the call.
   */
  public void updated(FeatureStructure annotation) {
    Collection<FeatureStructure> structures = new ArrayList<FeatureStructure>(1);

    structures.add(annotation);

    updated(structures);
  }
}