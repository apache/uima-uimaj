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

package org.apache.uima.caseditor.editor.action;


import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.caseditor.editor.ICasDocument;
import org.apache.uima.caseditor.editor.util.AnnotationSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

/**
 * Merges two or more annotations.
 *
 * TODO: also merge features - if one is null or primitive has default value take the the other one -
 * in conflict case do nothing
 */
public class MergeAnnotationAction extends BaseSelectionListenerAction {
  private ICasDocument mDocument;

  /**
   * Initializes the current instance.
   *
   * @param document
   */
  public MergeAnnotationAction(ICasDocument document) {
    super("MergeAnnotationAction");

    mDocument = document;

    setEnabled(false);
  }

  @Override
  protected boolean updateSelection(IStructuredSelection selection) {
    AnnotationSelection annotation = new AnnotationSelection(selection);

    return annotation.size() > 1;
  }

  /**
   * Executes the merge action
   */
  @Override
  public void run() {
    AnnotationSelection annotations = new AnnotationSelection(getStructuredSelection());

    CAS documentCAS = mDocument.getCAS();

    AnnotationFS mergedAnnotation = documentCAS.createAnnotation(annotations.getFirst().getType(),
            annotations.getFirst().getBegin(), annotations.getLast().getEnd());

    mDocument.removeAnnotations(annotations.toList());
    mDocument.addFeatureStructure(mergedAnnotation);
  }
}