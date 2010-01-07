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

import java.lang.reflect.InvocationTargetException;

import org.apache.uima.caseditor.CasEditorPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

/**
 * This import dialog can import text files into a corpus. Currently plain text and rich text format
 * is supported.
 */
public final class ImportDocumentWizard extends Wizard implements IImportWizard {
  private ImportDocumentWizardPage mMainPage;

  private IStructuredSelection mCurrentResourceSelection;

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    mCurrentResourceSelection = selection; // if corpus set as import corpus

    setWindowTitle("Import Documents");
  }

  @Override
  public void addPages() {
    mMainPage = new ImportDocumentWizardPage("ImportDocuments", mCurrentResourceSelection);

    addPage(mMainPage);
  }

  @Override
  public boolean performFinish() {
    IImportStructureProvider importProvider = new DocumentImportStructureProvider();

    ImportOperation operation =
            new ImportOperation(mMainPage.getImportDestinationPath(), importProvider, null,
            mMainPage.getFilesToImport());

    operation.setContext(getShell());

    operation.setOverwriteResources(false);

    try {
      getContainer().run(true, true, operation);
    } catch (InvocationTargetException e) {
      CasEditorPlugin.log(e);

      MessageDialog.openError(getContainer().getShell(), "Error during import", e.getMessage());

      return false;
    } catch (InterruptedException e) {
      return false;
    }

    return true;
  }
}
