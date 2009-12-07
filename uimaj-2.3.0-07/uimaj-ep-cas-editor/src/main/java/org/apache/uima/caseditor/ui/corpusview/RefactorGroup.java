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

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.DeleteResourceAction;
import org.eclipse.ui.actions.RenameResourceAction;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

/**
 * This group contains refactoring actions.
 */
final class RefactorGroup extends ActionGroup {

    /**
     * The Clipboard for the copy/paste actions. Must be disposed!
     */
    private Clipboard mClipboard;

    /**
     * Action that actually copy the resource.
     */
    private CopyAction mCopyAction;

    /**
     * The copy retarget action
     */
    private IWorkbenchAction mRetargetCopyAction;

    /**
     * Action that actually paste the resource.
     */
    private PasteAction mPasteAction;

    /**
     * The paste retarget action.
     */
    private IWorkbenchAction mRetargetPasteAction;

    /**
     * Action that actually delete the resource.
     */
    private DeleteResourceAction mDeleteAction;

    /**
     * The delete retarget action.
     */
    private IWorkbenchAction mRetargetDeleteAction;

    /**
     * Action that actually rename the resource.
     */
    private RenameResourceAction mRenameAction;

    /**
     * The rename retarget action.
     */
    private IWorkbenchAction mRetargetRenameAction;

    /**
     * Initializes a new instance.
     *
     * @param shell
     * @param window
     */
    @SuppressWarnings("deprecation")
    RefactorGroup(Shell shell, IWorkbenchWindow window) {
    mClipboard = new Clipboard(shell.getDisplay());

    // copy action
        mCopyAction = new CopyAction(mClipboard);

        mRetargetCopyAction = ActionFactory.COPY.create(window);

        // paste action
        mPasteAction = new PasteAction(shell, mClipboard);

        mRetargetPasteAction = ActionFactory.PASTE.create(window);

        // delete action
        mDeleteAction = new DeleteResourceAction(shell);

        mRetargetDeleteAction = ActionFactory.DELETE.create(window);

        // rename action
        mRenameAction = new RenameResourceAction(shell);

        mRetargetRenameAction = ActionFactory.RENAME.create(window);
    }

    /**
     * Fills the context menu with actions.
     */
  @Override
  public void fillContextMenu(IMenuManager menu) {
    IStructuredSelection selection = CorpusExplorerUtil
            .convertNLPElementsToResources((IStructuredSelection) getContext().getSelection());

    boolean isAResourceSelected = !selection.isEmpty();

    // Order as in "Eclipse User Interface Guidelines":

    // 1. Cut

    // 2. Copy
    menu.add(mRetargetCopyAction);

    // 3. Paste
    menu.add(mRetargetPasteAction);

    // 4. Delete
    if (isAResourceSelected) {
      menu.add(mRetargetDeleteAction);
    }

    // 5. Move
    // menu.add(ActionFactory.MOVE.create(mWindow));

    // 6. Rename
    if (selection.size() == 1) {
      menu.add(mRetargetRenameAction);
    }

    // 7. other refactoring commands
  }

 /**
   * Fill the ActionBars with defined actions.
   */
  @Override
  public void fillActionBars(IActionBars actionBars) {
    actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), mDeleteAction);

    actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), mCopyAction);

    actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), mPasteAction);

    actionBars.setGlobalActionHandler(ActionFactory.RENAME.getId(), mRenameAction);

    actionBars.updateActionBars();
  }

    /**
     * Update the selection of the actions.
     */
  @Override
  public void updateActionBars() {
    super.updateActionBars();

    IStructuredSelection selection = CorpusExplorerUtil
            .convertNLPElementsToResources((IStructuredSelection) getContext().getSelection());

        mCopyAction.selectionChanged(selection);
        mPasteAction.selectionChanged(selection);
        mDeleteAction.selectionChanged(selection);
        mRenameAction.selectionChanged(selection);
    }

	void handleKeyPressed(KeyEvent e) {
		if (e.keyCode == SWT.F2 && e.stateMask == 0) {
			if (mRenameAction.isEnabled()) {
				mRenameAction.run();
				e.doit = false;
			}
		}
		else if (e.keyCode == SWT.DEL && e.stateMask == 0) {
			if (mDeleteAction.isEnabled()) {
				mDeleteAction.run();
				e.doit = false;
			}
		}
	}

  /**
   * Destroy all swt elements which where created by this instance.
   */
  @Override
  public void dispose() {

    mClipboard.dispose();
    mRetargetCopyAction.dispose();
    mRetargetPasteAction.dispose();
    mRetargetDeleteAction.dispose();
    mRetargetRenameAction.dispose();

    super.dispose();
  }
}
