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
import org.apache.uima.cas.text.AnnotationFS;

/**
 * This listener listens only for {@link AnnotationFS} change events. All other change events for
 * {@link FeatureStructure}s are filtered.
 */
public abstract class AbstractAnnotationDocumentListener extends AbstractDocumentListener {

  /**
   * Filter annotations.
   *
   * @param structures
   *          the structures
   * @return the collection
   */
  private Collection<AnnotationFS> filterAnnotations(Collection<FeatureStructure> structures) {
    Collection<AnnotationFS> annotations = new ArrayList<>(structures.size());

    for (FeatureStructure structure : structures) {
      if (structure instanceof AnnotationFS) {
        annotations.add((AnnotationFS) structure);
      }
    }

    return annotations;
  }

  /**
   * Add notification.
   *
   * @param structures
   *          the structures
   */
  @Override
  public void added(Collection<FeatureStructure> structures) {
    Collection<AnnotationFS> annotations = filterAnnotations(structures);

    if (!annotations.isEmpty()) {
      addedAnnotation(annotations);
    }
  }

  /**
   * Remove notification.
   *
   * @param structures
   *          the structures
   */
  @Override
  public void removed(Collection<FeatureStructure> structures) {
    Collection<AnnotationFS> annotations = filterAnnotations(structures);

    if (!annotations.isEmpty()) {
      removedAnnotation(annotations);
    }
  }

  /**
   * Update notification.
   *
   * @param structures
   *          the structures
   */
  @Override
  public void updated(Collection<FeatureStructure> structures) {
    Collection<AnnotationFS> annotations = filterAnnotations(structures);

    if (!annotations.isEmpty()) {
      updatedAnnotation(annotations);
    }
  }

  /**
   * Added annotation.
   *
   * @param annotations
   *          the annotations
   */
  protected abstract void addedAnnotation(Collection<AnnotationFS> annotations);

  /**
   * Removed annotation.
   *
   * @param annotations
   *          the annotations
   */
  protected abstract void removedAnnotation(Collection<AnnotationFS> annotations);

  /**
   * Updated annotation.
   *
   * @param annotations
   *          the annotations
   */
  protected abstract void updatedAnnotation(Collection<AnnotationFS> annotations);
}