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


import org.apache.uima.caseditor.editor.FeatureStructureSelection;
import org.apache.uima.caseditor.editor.ICasDocument;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

/**
 * Deletes all selected annotations.
 */
public class DeleteFeatureStructureAction extends BaseSelectionListenerAction {
  private ICasDocument mDocument;

  /**
   * Initializes the current instance.
   *
   * @param document
   */
  public DeleteFeatureStructureAction(ICasDocument document) {
    super("DeleteAction");

    mDocument = document;

    setEnabled(true);
  }

  @Override
  protected boolean updateSelection(IStructuredSelection selection) {
    FeatureStructureSelection featureStructures =
      new FeatureStructureSelection(selection);

    return featureStructures.size() > 0;
  }

  /**
   * Executes the action.
   */
  @Override
  public void run() {

    FeatureStructureSelection featureStructures =
      new FeatureStructureSelection(getStructuredSelection());

    mDocument.removeFeatureStructures(featureStructures.toList());
  }
}