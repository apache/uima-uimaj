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

package org.apache.uima.caseditor.ide.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.ide.CasEditorIdePlugin;
import org.apache.uima.caseditor.ide.CasEditorIdePreferenceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
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

    // Did not find a way to retrieve the "Import" string
    // from an eclipse message file
    setWindowTitle("Import");
  }

  @Override
  public void addPages() {
    mMainPage = new ImportDocumentWizardPage("ImportDocuments", mCurrentResourceSelection);

    addPage(mMainPage);
  }

  @Override
  public boolean performFinish() {
    
    String usedEncoding = mMainPage.getTextEncoding();
    
    IPreferenceStore store = CasEditorIdePlugin.getDefault().getPreferenceStore();
    
    String lastUsedEncodingsString = store.getString(
            CasEditorIdePreferenceConstants.CAS_IMPORT_WIZARD_LAST_USED_ENCODINGS);
    
    List<String> lastUsedEncodings = new ArrayList<String>(Arrays.asList(lastUsedEncodingsString.split(
            CasEditorIdePreferenceConstants.STRING_DELIMITER)));
    
    int usedEncodingIndex = lastUsedEncodings.indexOf(usedEncoding);
    
    if (usedEncodingIndex != -1) {
      lastUsedEncodings.remove(usedEncodingIndex);
    }
    
    lastUsedEncodings.add(0, usedEncoding);
    
    int maxUserItemCount = 10;
    
    if (lastUsedEncodings.size() > maxUserItemCount) {
      lastUsedEncodings = lastUsedEncodings.subList(0, maxUserItemCount - 1);
    }
    
    StringBuilder updatedLastUsedEncodingsString = new StringBuilder();
    
    for (String encoding : lastUsedEncodings) {
      updatedLastUsedEncodingsString.append(encoding);
      updatedLastUsedEncodingsString.append(
              CasEditorIdePreferenceConstants.STRING_DELIMITER);
    }
    
    store.setValue(CasEditorIdePreferenceConstants.CAS_IMPORT_WIZARD_LAST_USED_ENCODINGS,
            updatedLastUsedEncodingsString.toString());
    
    IImportStructureProvider importProvider = new DocumentImportStructureProvider(mMainPage.getLanguage(),
    		mMainPage.getTextEncoding(), mMainPage.getCasFormat());
    
    // BUG: We cannot pass null here for the overwrite query
    ImportOperation operation =
            new ImportOperation(mMainPage.getImportDestinationPath(), importProvider, new OverwriteQuery(getShell()),
            mMainPage.getFilesToImport());

    operation.setContext(getShell());

    operation.setOverwriteResources(false);

    try {
      getContainer().run(true, true, operation);
    } catch (InvocationTargetException e) {
      CasEditorPlugin.log(e);
      
      String message = "Unkown error during import, see the log file for details";
      
      Throwable cause = e.getCause();
      if (cause != null) {
        
        String causeMessage = cause.getMessage();
        
        if (causeMessage != null)
          message = causeMessage;
      }
      
      MessageDialog.openError(getContainer().getShell(), "Import failed", message);

      return false;
    } catch (InterruptedException e) {
      return false;
    }

    return true;
  }
}
