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


import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.caseditor.editor.ICasDocument;
import org.apache.uima.caseditor.editor.util.AnnotationSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

/**
 * Lowers the left side of the currently selected annotation by one.
 */
public final class LowerLeftAnnotationSideAction extends BaseSelectionListenerAction {
  private ICasDocument mDocument;

  /**
   * Initializes a new instance.
   *
   * @param document
   *
   */
  public LowerLeftAnnotationSideAction(ICasDocument document) {
    super("LowerLeftAnnotationSide");

    mDocument = document;

    setEnabled(false);
  }

  @Override
  protected boolean updateSelection(IStructuredSelection selection) {
    AnnotationSelection annotation = new AnnotationSelection(selection);

    return annotation.size() == 1;
  }

  /**
   * Increases the begin index of an annotation by one.
   */
  @Override
  public void run() {
    AnnotationSelection annotations = new AnnotationSelection(getStructuredSelection());

    AnnotationFS annotation = annotations.getFirst();

    Type annotationType = annotation.getType();
    Feature beginFeature = annotationType.getFeatureByBaseName("begin");

    if (annotation.getBegin() < annotation.getEnd()) {
      annotation.setIntValue(beginFeature, annotation.getBegin() + 1);
    }

    mDocument.update(annotation);
  }
}