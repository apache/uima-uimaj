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

package org.apache.uima.pear.generate;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.uima.pear.PearException;
import org.apache.uima.pear.PearPlugin;
import org.apache.uima.pear.insd.edit.INSDComponentPage;
import org.apache.uima.pear.insd.edit.INSDEnvironmentPage;
import org.apache.uima.pear.insd.edit.InsdConstants;
import org.apache.uima.pear.insd.edit.PearInstallationDescriptor;
import org.apache.uima.pear.insd.edit.vars.VarVal;
import org.apache.uima.pear.nature.ProjectCustomizer;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.apache.uima.pear.tools.InstallationDescriptor;
import org.apache.uima.pear.tools.InstallationDescriptorHandler;

/**
 * Wizard to generate a PEAR file.
 * 
 * 
 * 
 */
public class GeneratePearWizard extends Wizard implements IWizard, InsdConstants {
  private IContainer currentContainer;

  private InstallationDescriptor insd;

  private Hashtable wizardData = new Hashtable();

  // WIZARD PAGES
  // private WizardNewProjectCreationPage projectPage;
  private INSDComponentPage componentPage;

  private INSDEnvironmentPage environmentPage;

  private PearFileResourceExportPage pearExportPage;

  /**
   * Constructor
   */
  public GeneratePearWizard(IContainer container) {
    super();
    try {
      setWindowTitle("PEAR Generation Wizard");
      setDefaultPageImageDescriptor(PearPlugin.getImageDescriptor("generatePearWiz.gif"));
      setNeedsProgressMonitor(true);
      currentContainer = container;
    } catch (Throwable e) {
      PearException subEx = new PearException(
              "Operation failed because the wizard could not be initialized.\nPlease report this error.",
              e);
      subEx.openErrorDialog(getShell());
      this.dispose();
    }
    try {
      AbstractUIPlugin plugin = PearPlugin.getDefault();
      IDialogSettings workbenchSettings = plugin.getDialogSettings();
      IDialogSettings section = workbenchSettings.getSection("PearFileExportWizard");//$NON-NLS-1$
      if (section == null)
        section = workbenchSettings.addNewSection("PearFileExportWizard");//$NON-NLS-1$
      setDialogSettings(section);
    } catch (Throwable e) {
      e.printStackTrace();
    }
    try {
      insd = PearInstallationDescriptor.getInstallationDescriptor(currentContainer);
    } catch (Throwable e) {
      e.printStackTrace();
      insd = new InstallationDescriptor();
    }
    try {
      ProjectCustomizer.customizeProject(currentContainer, insd);
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
      componentPage = new INSDComponentPage(currentContainer, insd, wizardData);
      addPage(componentPage);

      environmentPage = new INSDEnvironmentPage(currentContainer, insd, wizardData);
      addPage(environmentPage);
      pearExportPage = new PearFileResourceExportPage(new StructuredSelection(currentContainer
              .members()), currentContainer);
      addPage(pearExportPage);

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
      // currentContainer.refreshLocal(IResource.DEPTH_INFINITE,null);
      editInstallationDescriptor();
      final String file = pearExportPage.getDestinationValue();
      if (new File(file).exists()
              && !MessageDialog.openConfirm(getShell(), "File exists", "The file " + file
                      + " already exists. Do you want to overwrite it?")) {
        return false;
      }
      getContainer().run(false, true, pearExportPage.getExportRunnable());
      MessageDialog.openInformation(getShell(), "Done.", "The PEAR file export operation is done.");
      return true;
    } catch (Throwable e) {
      PearException.openErrorDialog(e, getShell());
      return false;
    }
  }

  private void editInstallationDescriptor() throws CoreException, IOException {
    handleComponentInformation();
    addEnvOptions();
    addEnvVars();
    PearInstallationDescriptor.saveInstallationDescriptor(currentContainer, insd);
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

  /*
   * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
   *      org.eclipse.jface.viewers.IStructuredSelection)
   */
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    // nothing to do
  }
}