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

package org.apache.uima.pear.actions;

import org.apache.uima.pear.insd.edit.EditInstallationDescriptorWizard;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

/**
 * 
 * This class handles the "Edit Installation Descriptor" action, which appear in the context menu of
 * a PEAR installation descriptor (install.xml)
 * 
 * 
 * 
 */
public class EditInstallationDescriptorAction implements IObjectActionDelegate {

  private IStructuredSelection ssel;

  /**
   * Constructor
   */
  public EditInstallationDescriptorAction() {
    super();
  }

  /**
   * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
   */
  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  /**
   * See IActionDelegate#run(IAction)
   */
  public void run(IAction action) {
    Shell shell = new Shell();

    try {

      IFile installFile = (IFile) ssel.getFirstElement();

      // create the wizard
      EditInstallationDescriptorWizard wizard = new EditInstallationDescriptorWizard(installFile
              .getProject());

      // Initialize the wizard
      wizard.init(PlatformUI.getWorkbench(), ssel);

      // Create the dialog to wrap the wizard
      WizardDialog dialog = new WizardDialog(shell, wizard);

      // Open Wizard Dialog
      dialog.open();

    } catch (Throwable e) {
      e.printStackTrace();
      MessageDialog.openWarning(shell, "Action not supported",
              "This action was not supported for the selected item. ");
    }
  }

  /**
   * See IActionDelegate#selectionChanged(IAction, ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {
    ssel = null;
    if (selection instanceof IStructuredSelection)
      ssel = (IStructuredSelection) selection;
  }

}
