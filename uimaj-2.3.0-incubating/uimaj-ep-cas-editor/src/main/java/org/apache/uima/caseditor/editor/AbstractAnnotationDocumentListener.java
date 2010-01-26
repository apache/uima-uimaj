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

  private Collection<AnnotationFS> filterAnnotations(Collection<FeatureStructure> strcutres) {
    Collection<AnnotationFS> annotations = new ArrayList<AnnotationFS>(strcutres.size());

    for (FeatureStructure structure : strcutres) {
      if (structure instanceof AnnotationFS) {
        annotations.add((AnnotationFS) structure);
      }
    }

    return annotations;
  }

  /**
   * Add notification.
   */
  public void added(Collection<FeatureStructure> structres) {
    Collection<AnnotationFS> annotations = filterAnnotations(structres);

    if (!annotations.isEmpty()) {
      addedAnnotation(annotations);
    }
  }

  /**
   * Remove notification.
   */
  public void removed(Collection<FeatureStructure> structres) {
    Collection<AnnotationFS> annotations = filterAnnotations(structres);

    if (!annotations.isEmpty()) {
      removedAnnotation(annotations);
    }
  }

  /**
   * Update notification.
   */
  public void updated(Collection<FeatureStructure> structres) {
    Collection<AnnotationFS> annotations = filterAnnotations(structres);

    if (!annotations.isEmpty()) {
      updatedAnnotation(annotations);
    }
  }

  protected abstract void addedAnnotation(Collection<AnnotationFS> annotations);

  protected abstract void removedAnnotation(Collection<AnnotationFS> annotations);

  protected abstract void updatedAnnotation(Collection<AnnotationFS> annotations);
}