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

import org.apache.uima.caseditor.core.model.INlpElement;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.ImportResourcesAction;
import org.eclipse.ui.dialogs.PropertyDialogAction;

/**
 * Main corpus explorer action group.
 */
final class CorpusExplorerActionGroup extends ActionGroup implements ICorpusExplorerActionGroup {
  private OpenActionGroup openActionGroup;

  private RefactorGroup refactorGroup;

  protected ImportResourcesAction importAction;

  // protected ExportResourcesAction mExportAction;

  private WorkspaceActionGroup workspaceGroup;

  private AnnotatorActionGroup annotatorActionGroup;

  private ConsumerCorpusActionGroup consumerCorpusActionGroup;

  private UimaRefactorActionGroup uimaRefactorActionGroup;

  private PropertyDialogAction propertyAction;

  private IWorkbenchWindow mWindow;

  private IAction mRetargetPropertiesAction;

  /**
   * Creates a <code>CorpusExplorerActionGroup</code> object.
   *
   * @param view -
   *          the corresponding <code>CorpusExplorerView</code>
   */
  CorpusExplorerActionGroup(CorpusExplorerView view) {
    mWindow = view.getSite().getPage().getWorkbenchWindow();

    Shell shell = view.getSite().getShell();

    openActionGroup = new OpenActionGroup(view.getSite().getPage());

    refactorGroup = new RefactorGroup(shell, mWindow);

    importAction = new ImportResourcesAction(mWindow);

    // mExportAction = new ExportResourcesAction(mWindow);

    workspaceGroup = new WorkspaceActionGroup(shell, mWindow);

    annotatorActionGroup = new AnnotatorActionGroup(shell);

    consumerCorpusActionGroup = new ConsumerCorpusActionGroup(shell);

    uimaRefactorActionGroup = new UimaRefactorActionGroup(shell);

    propertyAction = new PropertyDialogAction(new SameShellProvider(shell), view.getTreeViewer());

    mRetargetPropertiesAction = ActionFactory.PROPERTIES.create(mWindow);
  }

    /**
     * Fills the context menu with all the actions.
     */
  @Override
  public void fillContextMenu(IMenuManager menu) {
    IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();

    // For action order see "Eclipse User Interface Guidelines"

    // 1. New actions
    IAction newAction = ActionFactory.NEW.create(mWindow);
    newAction.setText("New");
    menu.add(newAction);
    menu.add(new Separator());

        // 2. Open actions
    openActionGroup.fillContextMenu(menu);
    menu.add(new Separator());

    // 3. Navigate + Show In

    // 4.1 Cut, Copy, Paste, Delete, Rename and other refactoring commands
    refactorGroup.fillContextMenu(menu);
    menu.add(new Separator());

    // 4.2
    menu.add(ActionFactory.IMPORT.create(mWindow));

    // menu.add(ActionFactory.EXPORT.create(mWindow));

    menu.add(new Separator());

    // 5. Other Plugin Additions
    workspaceGroup.fillContextMenu(menu);
    menu.add(new Separator());

    // 5.2 annotator additions
    MenuManager taggerMenu = new MenuManager("Annotator");
    menu.add(taggerMenu);

    annotatorActionGroup.fillContextMenu(taggerMenu);

    // 5.3 consumer additions
    MenuManager trainerMenu = new MenuManager("Consumer");
    menu.add(trainerMenu);

    consumerCorpusActionGroup.fillContextMenu(trainerMenu);

    MenuManager uimaRefactorMenu = new MenuManager("Refactor");
    menu.add(uimaRefactorMenu);

    uimaRefactorActionGroup.fillContextMenu(uimaRefactorMenu);

    // 5.4 Annotator plugin additions
    menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        // 6. Properties action
    boolean isOnlyOneResourceSelected = selection.size() == 1;
    if (isOnlyOneResourceSelected) {
      menu.add(mRetargetPropertiesAction);
    }
  }

    /**
     * Fills the action bars
     */
  @Override
  public void fillActionBars(IActionBars actionBars) {
    actionBars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), propertyAction);

    actionBars.updateActionBars();

    openActionGroup.fillActionBars(actionBars);
    refactorGroup.fillActionBars(actionBars);
    workspaceGroup.fillActionBars(actionBars);
  }

    /**
     * Updates the actions.
     */
  @Override
  public void updateActionBars() {
    IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();

    propertyAction.setEnabled(selection.size() == 1);

    openActionGroup.updateActionBars();
    refactorGroup.updateActionBars();
    workspaceGroup.updateActionBars();
  }

    /**
     * Sets the context to the action groups.
     */
  @Override
  public void setContext(ActionContext context) {
    super.setContext(context);

    openActionGroup.setContext(context);
    refactorGroup.setContext(context);
    workspaceGroup.setContext(context);
    annotatorActionGroup.setContext(context);
    consumerCorpusActionGroup.setContext(context);
    uimaRefactorActionGroup.setContext(context);
  }

    /**
     * Executes the default action, in this case the open action.
     */
  public void executeDefaultAction(IStructuredSelection selection) {
    if (selection.getFirstElement() instanceof INlpElement) {
      INlpElement nlpElement = (INlpElement) selection.getFirstElement();

      openActionGroup.executeDefaultAction(new StructuredSelection(nlpElement.getResource()));
    } else {
      openActionGroup.executeDefaultAction(selection);
    }
  }

    /**
   * Dispose all resources created by the current object.
   */
  @Override
  public void dispose() {
    super.dispose();

    openActionGroup.dispose();
    refactorGroup.dispose();
    importAction.dispose();
    //        mExportAction.dispose();
    workspaceGroup.dispose();
    annotatorActionGroup.dispose();
    consumerCorpusActionGroup.dispose();
    uimaRefactorActionGroup.dispose();
    propertyAction.dispose();
  }

  void handleKeyPressed(KeyEvent e) {
    refactorGroup.handleKeyPressed(e);
    workspaceGroup.handleKeyPressed(e);
  }
}
