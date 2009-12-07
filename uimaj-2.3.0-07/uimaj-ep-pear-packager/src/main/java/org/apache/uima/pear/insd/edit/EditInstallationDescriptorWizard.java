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

package org.apache.uima.pear.insd.edit;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.uima.pear.PearException;
import org.apache.uima.pear.PearPlugin;
import org.apache.uima.pear.insd.edit.vars.VarVal;
import org.apache.uima.pear.nature.ProjectCustomizer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;

import org.apache.uima.pear.tools.InstallationDescriptor;
import org.apache.uima.pear.tools.InstallationDescriptorHandler;

/**
 * Wizard to edit the PEAR Installation Descriptor.
 * 
 * 
 * 
 */
public class EditInstallationDescriptorWizard extends Wizard implements IWizard, InsdConstants {

  private IProject currentProject;

  private InstallationDescriptor insd;

  private Hashtable wizardData = new Hashtable();

  // WIZARD PAGES
  private INSDComponentPage componentPage;

  private INSDEnvironmentPage environmentPage;

  /**
   * Constructor.
   * 
   * @param project
   *          An IProject with the UIMA Nature
   */
  public EditInstallationDescriptorWizard(IProject project) {
    super();
    try {
      setWindowTitle("Edit PEAR Installation Descriptor");
      setDefaultPageImageDescriptor(PearPlugin.getImageDescriptor("editInsdWiz.gif"));
      setNeedsProgressMonitor(true);
      currentProject = project;
    } catch (Throwable e) {
      PearException subEx = new PearException(
              "Operation failed because the wizard could not be initialized.\nPlease report this error.",
              e);
      subEx.openErrorDialog(getShell());
      this.dispose();
    }
    try {
      insd = PearInstallationDescriptor.getInstallationDescriptor(currentProject);
    } catch (Throwable e) {
      e.printStackTrace();
      insd = new InstallationDescriptor();
    }
    try {
      ProjectCustomizer.customizeProject(currentProject, insd);
    } catch (Throwable e) {
      PearException subEx = new PearException(
              "Operation failed because the wizard could not customize your project as a UIMA project.",
              e);
      subEx.openErrorDialog(getShell());
      this.dispose();
    }
  }

  /**
   * Adds the wizaed pages.
   * 
   * @see org.eclipse.jface.wizard.IWizard#addPages()
   */
  public void addPages() {
    try {
      componentPage = new INSDComponentPage(currentProject, insd, wizardData);
      addPage(componentPage);

      environmentPage = new INSDEnvironmentPage(currentProject, insd, wizardData);
      addPage(environmentPage);

    } catch (Throwable e) {
      PearException subEx = new PearException(
              "Operation failed because the wizard's pages could not be initialized properly.", e);
      subEx.openErrorDialog(getShell());
      this.dispose();
    }

  }

  /**
   * This method is called when 'Finish' button is pressed in the wizard.
   * 
   * @see org.eclipse.jface.wizard.IWizard#performFinish()
   * 
   */
  public boolean performFinish() {
    try {
      editInstallationDescriptor();
    } catch (Throwable e) {
      e.printStackTrace();
      MessageDialog.openError(getShell(), "Error",
              "An error happened while trying to execute the wizard operetions: \n\nDetails:\n "
                      + e.getMessage());
    }
    return true;
  }

  private void editInstallationDescriptor() throws CoreException, IOException {
    handleComponentInformation();
    addEnvOptions();
    addEnvVars();
    PearInstallationDescriptor.saveInstallationDescriptor(currentProject, insd);
  }

  private void handleComponentInformation() {
    insd.setMainComponent(componentPage.compID);
    insd
            .setMainComponentDesc(PearInstallationDescriptor
                    .addMacro(componentPage.compDescriptorPath));
  }

  private void addEnvOptions() {
    insd.clearOSSpecs();
    insd.clearToolkitsSpecs();
    insd.clearFrameworkSpecs();

    String os = environmentPage.osCombo.getText();
    if (os != null && os.trim().length() > 0)
      insd.addOSSpec(InstallationDescriptorHandler.NAME_TAG, os);

    String jdkVersion = environmentPage.jdkVersionCombo.getText();
    if (jdkVersion != null && jdkVersion.trim().length() > 0)
      insd.addToolkitsSpec(InstallationDescriptorHandler.JDK_VERSION_TAG, jdkVersion);

  }

  private void addEnvVars() {
    insd.deleteInstallationActions(InstallationDescriptor.ActionInfo.SET_ENV_VARIABLE_ACT);
    Iterator envVarsItr = environmentPage.envVarList.tableRows.iterator();
    while (envVarsItr.hasNext()) {
      VarVal vv = (VarVal) envVarsItr.next();
      String envVarName = vv.getVarName();
      String envVarValue = vv.getVarValue();

      if (envVarName != null && envVarValue != null && envVarName.trim().length() > 0
              && envVarValue.trim().length() > 0) {
        InstallationDescriptor.ActionInfo actionInfo = new InstallationDescriptor.ActionInfo(
                InstallationDescriptor.ActionInfo.SET_ENV_VARIABLE_ACT);
        actionInfo.params.put("VAR_NAME", envVarName);
        actionInfo.params.put("VAR_VALUE", envVarValue);
        actionInfo.params.put("COMMENTS", "");

        insd.addInstallationAction(actionInfo);
      }
    }
  }

  /**
   * See IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
   */
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    // this.workbench = workbench;
    // this.selection = selection;
    setNeedsProgressMonitor(true);
  }

}