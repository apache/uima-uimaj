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

import org.apache.uima.caseditor.core.model.CasProcessorFolder;
import org.apache.uima.caseditor.core.model.ConsumerElement;
import org.apache.uima.caseditor.core.model.CorpusElement;
import org.apache.uima.caseditor.core.model.NlpProject;
import org.apache.uima.caseditor.core.uima.CasConsumerConfiguration;
import org.apache.uima.caseditor.ui.action.ConsumerActionRunnable;
import org.apache.uima.caseditor.ui.action.RunnableAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.ActionGroup;

/**
 * This is an action group for cas consumer actions.
 */
final class ConsumerCorpusActionGroup extends ActionGroup {
  private Shell mShell;

  ConsumerCorpusActionGroup(Shell shell) {
    mShell = shell;
  }

  /**
   * Adds for each uima cas consumer an appropriate configured <code>CasConsumerAction</code> to
   * the given menu. The action appears only in the menu if a document or corpus is selected.
   */
  @Override
  @SuppressWarnings("unchecked")
  public void fillContextMenu(IMenuManager menu) {
    IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();

    if (CorpusExplorerUtil.isContaingOnlyNlpElements(selection)) {
      // TODO: add here also single documents
      LinkedList<CorpusElement> corpora = new LinkedList<CorpusElement>();

      for (Iterator resources = selection.iterator(); resources.hasNext();) {
        Object resource = resources.next();

        if (resource instanceof CorpusElement) {
          corpora.add((CorpusElement) resource);
        }
      }

      // TODO: refactor this here
      if (!corpora.isEmpty()) {
        CorpusElement aCorpus = corpora.getFirst();
        NlpProject project = aCorpus.getNlpProject();

        Collection<CasProcessorFolder> sourceFolders = project.getCasProcessorFolders();

        for (CasProcessorFolder sourceFolder : sourceFolders) {

          Collection<ConsumerElement> consumers = sourceFolder.getConsumers();

          for (ConsumerElement consumer : consumers) {
            CasConsumerConfiguration config = consumer.getConsumerConfiguration();

            if (config != null) {
              ConsumerActionRunnable consumerRunnableAction = new ConsumerActionRunnable(config,
                      corpora);

              RunnableAction consumerAction = new RunnableAction(mShell, consumer.getName(),
                      consumerRunnableAction);

              menu.add(consumerAction);

            }
          }
        }
      }
    }
  }
}
