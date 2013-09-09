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

package org.apache.uima.caseditor.ide;

import org.apache.uima.caseditor.CasEditorPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Type System Property Page to set the default type system location
 * of a project.
 */
public class TypeSystemLocationPropertyPage extends PropertyPage {

  public final static String TYPE_SYSTEM_PROPERTY = "UimaCasEditorTypeSystemPath";

  private static final String DEFAULT_TYPE_SYSTEM_PATH = "TypeSystem.xml";

  private Text typeSystemText;

  
  IProject getProject() {
    return (IProject) getElement().getAdapter(IProject.class);
  }
  
  String getDefaultTypeSystemLocation() {
    
    IProject project = getProject();
    
    if (project != null)
        return project.getFile(DEFAULT_TYPE_SYSTEM_PATH).getFullPath().toString();
    else
      return "";
  }
  
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    composite.setLayout(layout);

    GridData data = new GridData();
    data.verticalAlignment = GridData.FILL;
    data.horizontalAlignment = GridData.FILL;
    composite.setLayoutData(data);

    Label instructions = new Label(composite, SWT.WRAP);
    instructions.setText("Select the default type system which is used to open CASes:");
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 2;
    gd.grabExcessHorizontalSpace = true;
    instructions.setLayoutData(gd);

    typeSystemText = new Text(composite,SWT.BORDER);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    typeSystemText.setLayoutData(gd);

    typeSystemText.addModifyListener(new ModifyListener() {
      
      public void modifyText(ModifyEvent event) {
        updateApplyButton();
      }
    });
    
    try {
      String typeSystemPath = ((IResource) getElement()).getPersistentProperty(new QualifiedName("",
              TYPE_SYSTEM_PROPERTY));
      typeSystemText.setText((typeSystemPath != null) ? typeSystemPath : getDefaultTypeSystemLocation());
    } catch (CoreException e) {
      typeSystemText.setText(DEFAULT_TYPE_SYSTEM_PATH);
    }
    
    Button browseButton = new Button(composite, SWT.PUSH);
    browseButton.setText("Browse ...");
    browseButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(),
                new WorkbenchLabelProvider(), new WorkbenchContentProvider());
        dialog.setTitle("Select descriptor");
        dialog.setMessage("Select descriptor");
        dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
        dialog.setInitialSelection(ResourcesPlugin.getWorkspace().getRoot().
                findMember(typeSystemText.getText()));
        if (dialog.open() == IDialogConstants.OK_ID) {
          IResource resource = (IResource) dialog.getFirstResult();
          if (resource != null) {
            String fileLoc = resource.getFullPath().toString();
            typeSystemText.setText(fileLoc);
          }
        }
      }
    });
    
    return composite;
  }

  protected void performDefaults() {
    typeSystemText.setText(getDefaultTypeSystemLocation());
  }
  
  public boolean performOk() {
    
    // have check, so performOk is only done when ts file is a valid file string
    
    // store the value in the owner text field
    try {
      ((IResource) getElement()).setPersistentProperty(
              new QualifiedName("", TYPE_SYSTEM_PROPERTY), typeSystemText.getText());
    } catch (CoreException e) {
      return false;
    }
    return true;
  }

  /**
   * Retrieves the type system or null if its not set.
   * 
   * @param project
   * @return the type system location or null if its not set
   */
  public static IFile getTypeSystemLocation(IProject project) {
    
    IFile defaultTypeSystemFile = project.getFile(DEFAULT_TYPE_SYSTEM_PATH);
    
    String typeSystemLocation;
    try {
      typeSystemLocation = project.getPersistentProperty(new QualifiedName("", TYPE_SYSTEM_PROPERTY));
    } catch (CoreException e) {
      typeSystemLocation = null;
    }
    
    IFile typeSystemFile = null;
    
    // Type system location is null when it was never set it anyway,
    if (typeSystemLocation != null) {
      
      if (typeSystemLocation.length() > 0) {
          IResource potentialTypeSystemResource = ResourcesPlugin.getWorkspace().getRoot().findMember(typeSystemLocation);
          if (potentialTypeSystemResource instanceof IFile)
            typeSystemFile = (IFile) potentialTypeSystemResource;
      }
      // Empty string means user does not want a type system to be set
      else {
        return null;
      }
    }
    
    if (typeSystemFile == null) {
      typeSystemFile = defaultTypeSystemFile;
    }
    
    return typeSystemFile;
  }

  public static void setTypeSystemLocation(IProject project, String typeSystemLocation) {
    
    try {
      project.setPersistentProperty(new QualifiedName("", TYPE_SYSTEM_PROPERTY), typeSystemLocation);
    } catch (CoreException e) {
      CasEditorPlugin.log(e);
    }
  }
}
