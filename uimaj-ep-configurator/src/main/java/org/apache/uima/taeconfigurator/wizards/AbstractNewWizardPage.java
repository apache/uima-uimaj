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

package org.apache.uima.taeconfigurator.wizards;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

import org.apache.uima.taeconfigurator.TAEConfiguratorPlugin;

public abstract class AbstractNewWizardPage extends WizardPage {

  protected ISelection selection;

  protected Text containerText;

  protected Text fileText;

  private String defaultNewName;

  public AbstractNewWizardPage(ISelection pSelection, String image, String title,
                  String description, String defaultNewName) {
    super("wizardPage");
    setTitle(title);
    setDescription(description);
    setImageDescriptor(TAEConfiguratorPlugin.getImageDescriptor("big_t_s.gif"));

    selection = pSelection;
    this.defaultNewName = defaultNewName;
  }

  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    container.setLayout(layout);
    layout.numColumns = 3;
    layout.verticalSpacing = 9;
    Label label = new Label(container, SWT.NULL);
    label.setText("Parent &Folder:");

    containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    containerText.setLayoutData(gd);
    containerText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });

    Button button = new Button(container, SWT.PUSH);
    button.setText("Browse...");
    button.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        handleBrowse();
      }
    });
    label = new Label(container, SWT.NULL);
    label.setText("&File name:");

    fileText = new Text(container, SWT.BORDER | SWT.SINGLE);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    fileText.setLayoutData(gd);
    fileText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });
    initialize();
    dialogChanged();
    setControl(container);
  }

  private void initialize() {

    if (selection != null && selection.isEmpty() == false
                    && selection instanceof IStructuredSelection) {
      IStructuredSelection ssel = (IStructuredSelection) selection;
      if (ssel.size() > 1)
        return;
      Object obj = ssel.getFirstElement();

      if (!(obj instanceof IResource) && obj instanceof IAdaptable)
        obj = ((IAdaptable) obj).getAdapter(IResource.class);

      if (obj instanceof IResource) {
        IContainer container = null;
        if (obj instanceof IContainer)
          container = (IContainer) obj;
        else
          container = ((IResource) obj).getParent();
        if (container.isAccessible())
          containerText.setText(container.getFullPath().toString());
      }
    }
    fileText.setText(defaultNewName);
  }

  void handleBrowse() {
    ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), ResourcesPlugin
                    .getWorkspace().getRoot(), false, "Select a containing folder");
    if (dialog.open() == ContainerSelectionDialog.OK) {
      Object[] result = dialog.getResult();
      if (result.length == 1) {
        containerText.setText(((Path) result[0]).toOSString());
      }
    }
  }

  void dialogChanged() {
    String container = getContainerName();
    String fileName = getFileName();

    if (container.length() == 0) {
      updateStatus("Parent folder must be specified");
      return;
    }
    if (fileName.length() == 0) {
      updateStatus("File name must be specified");
      return;
    }
    int dotLoc = fileName.lastIndexOf('.');
    if (dotLoc != -1) {
      String ext = fileName.substring(dotLoc + 1);
      if (ext.equalsIgnoreCase("xml") == false) {
        updateStatus("File extension must be \"xml\"");
        return;
      }
    }
    updateStatus(null);
  }

  public String getContainerName() {
    return containerText.getText();
  }

  public String getFileName() {
    return fileText.getText();
  }

  private void updateStatus(String message) {
    setErrorMessage(message);
    setPageComplete(message == null);
  }

}
