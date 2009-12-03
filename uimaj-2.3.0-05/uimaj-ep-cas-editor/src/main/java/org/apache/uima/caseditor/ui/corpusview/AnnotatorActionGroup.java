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

package org.apache.uima.caseditor.ui.corpusview;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.uima.caseditor.core.model.AnnotatorElement;
import org.apache.uima.caseditor.core.model.CasProcessorFolder;
import org.apache.uima.caseditor.core.model.CorpusElement;
import org.apache.uima.caseditor.core.model.DocumentElement;
import org.apache.uima.caseditor.core.model.NlpProject;
import org.apache.uima.caseditor.core.uima.AnnotatorConfiguration;
import org.apache.uima.caseditor.ui.action.AnnotatorActionRunnable;
import org.apache.uima.caseditor.ui.action.RunnableAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.ActionGroup;

/**
 * This is an action group for annotator actions.
 */
final class AnnotatorActionGroup extends ActionGroup {
  private Shell mShell;

  /**
   * Initializes the current instance with the given shell.
   *
   * @param shell
   */
  AnnotatorActionGroup(Shell shell) {
    mShell = shell;
  }

  /**
   * Adds for each uima annotator an appropriate configured <code>AnnotatorAction</code> to the
   * given menu.
   *
   * Note: The action appears only in the menu if a document or corpus is selected.
   *
   * @param menu -
   *          the context menu manager
   */
  @Override
  @SuppressWarnings("unchecked")
  public void fillContextMenu(IMenuManager menu) {
    IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();

    LinkedList<DocumentElement> documentElements = new LinkedList<DocumentElement>();

    if (CorpusExplorerUtil.isContaingOnlyNlpElements(selection)) {
      Iterator resources = selection.iterator();
      while (resources.hasNext()) {
        Object resource = resources.next();

        if (resource instanceof CorpusElement) {
          documentElements.addAll(((CorpusElement) resource).getDocuments());
        }

        if (resource instanceof DocumentElement) {
          documentElements.add(((DocumentElement) resource));
        }
      }
    }

    // TODO: refactor this
    // how to retrieve the project of the selected elements ?
    // what happens if someone selects DocumentElements form
    // different projects ?
    if (!documentElements.isEmpty()) {
      DocumentElement aDocument = documentElements.getFirst();

      NlpProject project = aDocument.getNlpProject();

      Collection<CasProcessorFolder> sourceFolders = project.getCasProcessorFolders();

      for (CasProcessorFolder sourceFolder : sourceFolders) {
        Collection<AnnotatorElement> annotators = sourceFolder.getAnnotators();

        for (AnnotatorElement annotator : annotators) {
          AnnotatorConfiguration config = annotator.getAnnotatorConfiguration();

          if (config != null) {
            IRunnableWithProgress annotatorRunnableAction = new AnnotatorActionRunnable(config,
                    documentElements);

            RunnableAction annotatorAction = new RunnableAction(mShell, annotator.getName(),
                    annotatorRunnableAction);

            menu.add(annotatorAction);
          }
        }
      }
    }
  }
}
