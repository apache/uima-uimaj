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

package org.apache.uima.caseditor.ui.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * Wizard to create a new file.
 */
final public class WizardNewFileCreation extends Wizard implements INewWizard {
  /**
   * The ID of the new nlp project wizard.
   */
  public static final String ID = "org.apache.uima.caseditor.ui.wizards.WizardNewFileCreation";

  // private WizardNewFileCreationPage mMainPage;

  /**
   * Initializes the <code>NLPProjectWizard</code>.
   */
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    setWindowTitle("New file");
  }

  /**
   * Adds the project wizard page to the wizard.
   */
  @Override
  public void addPages() {
    // mMainPage = new WizardNewFileCreationPage("File", selection);
    // mMainPage.setTitle("File creation");
    // mMainPage.setDescription("Create a file");
    // addPage(mMainPage);
  }

  /**
   * Creates the nlp project.
   */
  @Override
  public boolean performFinish() {
    // mMainPage.createNewFile();
    return true;
  }
}
