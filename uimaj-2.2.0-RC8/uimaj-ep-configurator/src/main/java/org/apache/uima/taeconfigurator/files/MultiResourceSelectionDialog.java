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

package org.apache.uima.taeconfigurator.files;

import java.util.ArrayList;

import org.apache.uima.taeconfigurator.CDEpropertyPage;
import org.apache.uima.taeconfigurator.Messages;
import org.apache.uima.taeconfigurator.TAEConfiguratorPlugin;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.ResourcePickerDialog;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class MultiResourceSelectionDialog extends ResourcePickerDialog {

  private Button browseButton; // for browsing the file system

  private Button importByNameUI;

  private Button importByLocationUI;

  public boolean isImportByName;

  private MultiPageEditor editor;

  public MultiResourceSelectionDialog(Shell parentShell, IAdaptable rootElement, String message,
          IPath aExcludeDescriptor, MultiPageEditor aEditor) {
    super(parentShell);
    editor = aEditor;

    /*
    super(parentShell, rootElement, message);
    editor = aEditor;
    setTitle(Messages.getString("ResourceSelectionDialog.title")); //$NON-NLS-1$

    if (message != null)
      setMessage(message);
    else
      setMessage(Messages.getString("ResourceSelectionDialog.message")); //$NON-NLS-1$
    setShellStyle(getShellStyle() | SWT.RESIZE);
 */
  }

  protected Control createDialogArea(Composite parent) {
    // page group
    Composite composite = (Composite) super.createDialogArea(parent);
    FormToolkit factory = new FormToolkit(TAEConfiguratorPlugin.getDefault().getFormColors(
            parent.getDisplay()));
    Label label = new Label(composite, SWT.WRAP /* SWT.CENTER */);
    label.setText(Messages.getString("MultiResourceSelectionDialog.Or")); //$NON-NLS-1$
    browseButton = factory.createButton(composite, Messages
            .getString("MultiResourceSelectionDialog.BrowseFileSys"), //$NON-NLS-1$
            SWT.PUSH);
    browseButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
    browseButton.pack(false);
    browseButton.addListener(SWT.MouseUp, new Listener() {
      public void handleEvent(Event event) {
        FileDialog dialog = new FileDialog(getShell(), /* SWT.OPEN | */
        SWT.MULTI);
        String[] extensions = { Messages.getString("MultiResourceSelectionDialog.starDotXml") }; //$NON-NLS-1$
        dialog.setFilterExtensions(extensions);
        String sStartDir = TAEConfiguratorPlugin.getWorkspace().getRoot().getLocation()
                .toOSString();
        dialog.setFilterPath(sStartDir);
        String file = dialog.open();

        if (file != null && !file.equals("")) { //$NON-NLS-1$
          // close();
          okPressed();
          ArrayList list = new ArrayList();
          IPath iPath = new Path(file);
          list.add(iPath);
          localSetResult(list);
        }
      }

    });

    new Label(composite, SWT.NONE).setText("");
    importByNameUI = new Button(composite, SWT.RADIO);
    importByNameUI.setText("Import by Name");
    importByNameUI
            .setToolTipText("Importing by name looks up the name on the datapath, and if not found there, on the classpath.");

    importByLocationUI = new Button(composite, SWT.RADIO);
    importByLocationUI.setText("Import By Location");
    importByLocationUI.setToolTipText("Importing by location requires a relative or absolute URL");

    String defaultBy = CDEpropertyPage.getImportByDefault(editor.getProject());
    if (defaultBy.equals("location")) {
      importByNameUI.setSelection(false);
      importByLocationUI.setSelection(true);
    } else {
      importByNameUI.setSelection(true);
      importByLocationUI.setSelection(false);
    }
    return composite;
  }

  protected void okPressed() {
    isImportByName = importByNameUI.getSelection();
    CDEpropertyPage.setImportByDefault(editor.getProject(), isImportByName ? "name" : "location");
    super.okPressed();
  }

  // This is to avoid synthetic access method warning
  protected void localSetResult(ArrayList list) {
    setResult(list);
  }

}
