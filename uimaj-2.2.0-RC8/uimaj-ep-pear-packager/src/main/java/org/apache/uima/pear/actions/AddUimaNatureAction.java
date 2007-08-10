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

import org.apache.uima.pear.PearException;
import org.apache.uima.pear.insd.edit.PearInstallationDescriptor;
import org.apache.uima.pear.nature.ProjectCustomizer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import org.apache.uima.pear.tools.InstallationDescriptor;

/**
 * 
 * This class handles the "Add UIMA Nature" action, which appear in the context menu of a project
 * 
 * 
 * 
 */
public class AddUimaNatureAction implements IObjectActionDelegate {

  private IStructuredSelection ssel;

  /**
   * Constructor
   */
  public AddUimaNatureAction() {
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
      IProject currentProject = (IProject) a.getAdapter(IProject.class);

      if (currentProject != null) {
        boolean addNature = false;
        if (currentProject.hasNature(ProjectCustomizer.UIMA_NATURE_ID)) {
          addNature = MessageDialog
                  .openQuestion(
                          shell,
                          "UIMA Nature",
                          "The UIMA Nature was previously added to '"
                                  + currentProject.getName()
                                  + "'.\nWould you like to rebuild it without overwriting existing files and folders?");
        } else {
          addNature = MessageDialog.openQuestion(shell, "Adding UIMA custom Nature",
                  "Would you like to add a UIMA Nature to the project '" + currentProject.getName()
                          + "' ?");
        }
        if (addNature) {
          InstallationDescriptor insd = null;
          try {
            insd = PearInstallationDescriptor.getInstallationDescriptor(currentProject);
          } catch (Throwable e) {
            e.printStackTrace();
            insd = new InstallationDescriptor();
          }
          try {
            ProjectCustomizer.customizeProject(currentProject, insd);
            if (currentProject.hasNature(ProjectCustomizer.UIMA_NATURE_ID)) {
              MessageDialog.openInformation(shell, "UIMA Nature",
                      "The UIMA Nature was added successfully to the '" + currentProject.getName()
                              + "' project.");
            }
          } catch (PearException subEx) {
            PearProjectCustomizationException pcEx = new PearProjectCustomizationException(
                    "The project customization did not finish properly.", subEx.getCause());
            pcEx.openErrorDialog(shell);
          } catch (Throwable e) {
            PearProjectCustomizationException pcEx = new PearProjectCustomizationException(
                    "The project customization did not finish properly.", e);
            pcEx.openErrorDialog(shell);
          }
        }
      } else
        MessageDialog.openWarning(shell, "Action not supported",
                "This action is not supported for the selected item. ");
    } catch (Throwable e) {
      e.printStackTrace();
      MessageDialog.openWarning(shell, "Action not supported",
              "This action is not supported for the selected item. ");
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
