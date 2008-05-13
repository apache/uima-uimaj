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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.pear.generate.GeneratePearWizard;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

/**
 * 
 * This class handles the "Generate PEAR" action, which appear in the context menu of a project with
 * the UIMA nature.
 * 
 * 
 * 
 */
public class GeneratePearFileAction implements IObjectActionDelegate {

  private IStructuredSelection ssel;

  /**
   * Constructor
   */
  public GeneratePearFileAction() {
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
      IAdaptable a = (IAdaptable) ssel.getFirstElement();
      IProject selectedProject = (IProject) a.getAdapter(IProject.class);
      ssel = convertToResources(ssel);
      if (selectedProject != null) {
        try {
          // create the wizard
          // PearExportWizard wizard = new PearExportWizard();
          GeneratePearWizard wizard = new GeneratePearWizard(selectedProject);

          // Initialize the wizard
          wizard.init(PlatformUI.getWorkbench(), ssel);

          // Create the dialog to wrap the wizard
          WizardDialog dialog = new WizardDialog(shell, wizard);

          // Open Wizard Dialog
          dialog.open();
        } catch (Throwable e) {
          PearProjectCustomizationException pcEx = new PearProjectCustomizationException(
                  "An error occured during the PEAR generation process.", e);
          pcEx.openErrorDialog(shell);
        }
      } else
        MessageDialog.openWarning(shell, "Action not supported",
                "This action is not supported for the selected item. ");
    } catch (Throwable e) {
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

  /**
   * Attempt to convert the elements in the passed selection into resources by asking each for its
   * IResource property (iff it isn't already a resource). If all elements in the initial selection
   * can be converted to resources then answer a new selection containing these resources; otherwise
   * answer a new empty selection
   * 
   * @param originalSelection
   *          IStructuredSelection
   * @return IStructuredSelection
   */
  private IStructuredSelection convertToResources(IStructuredSelection originalSelection) {
    List result = new ArrayList();
    Iterator elements = originalSelection.iterator();

    while (elements.hasNext()) {
      Object currentElement = elements.next();
      if (currentElement instanceof IResource) { // already a resource
        result.add(currentElement);
      } else if (!(currentElement instanceof IAdaptable)) { // cannot be converted to resource
        return StructuredSelection.EMPTY; // so fail
      } else {
        Object adapter = ((IAdaptable) currentElement).getAdapter(IResource.class);
        if (!(adapter instanceof IResource)) // chose not to be converted to resource
          return StructuredSelection.EMPTY; // so fail
        result.add(adapter); // add the converted resource
      }
    }
    return new StructuredSelection(result.toArray()); // all converted fine, answer new selection
  }

}
