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

import java.text.MessageFormat;

import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.ResourcePickerDialog;
import org.apache.uima.util.XMLizable;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ContextForPartDialog extends /*LimitedResourceSelectionDialog*/ 
        ResourcePickerDialog /*implements
        ICheckStateListener*/ {

  // private MultiPageEditor editor;
  private Text contextPathGUI;

  public String contextPath;

  private String initialPath;

  private XMLizable tbe;

  public ContextForPartDialog(Shell parentShell, IAdaptable rootElement,
          XMLizable thingBeingEdited, IPath aExcludeDescriptor, MultiPageEditor aEditor,
          String aInitialPath) {

    super(parentShell);
    initialPath = aInitialPath;
    setTitle("Context for importable part");
    tbe = thingBeingEdited;

/*
    super(parentShell, rootElement, "Context for importable part");
    // editor = aEditor;
    initialPath = aInitialPath;
    setTitle("Context for importable part");
    tbe = thingBeingEdited;
    setShellStyle(getShellStyle() | SWT.RESIZE);
    */
  }

  protected Control createDialogArea(Composite parent) {
    parent = new Composite(parent, SWT.NONE);
    parent.setLayout(new GridLayout(1, true));
    parent.setLayoutData(new GridData(SWT.LEAD, SWT.BEGINNING, true, false));
    ((GridLayout) parent.getLayout()).marginWidth = 15;
    // Show a text field with the path, allow editing
    // anything picked overrides the text field
    AbstractSection.spacer(parent);
    Label instructions = new Label(parent, SWT.WRAP);
    instructions
            .setText(MessageFormat
                    .format(
                            "You are about to edit a UIMA {0} descriptor.  \n"
                                    + "In order to do this, you need to specify another UIMA descriptor, which will supply "
                                    + "the needed context for this file.\n"
                                    + "It can be any of the following kinds of descriptors:\n\n    "
                                    + "{1}\n\n"
                                    + "The file below is a suggested context.  \n\n"
                                    + "     >>>  If it is correct, just push OK.  <<<\n\n"
                                    + "Otherwise you can change it by overtyping it,\n"
                                    + "or use the project explorer window below to pick the context file to use.",
                            new Object[] {
                                (tbe instanceof FsIndexCollection) ? "Index Definition"
                                        : (tbe instanceof TypePriorities) ? "Type Priority Definition"
                                                : (tbe instanceof ResourceManagerConfiguration) ? "Resource Manager Configuration"
                                                        : "unhandled - error",

                                (tbe instanceof FsIndexCollection) ? "A Type System or any descriptor containing or "
                                        + "importing the type system associated with this Index Definition,\n    other than a Collection Processing Engine"
                                        : (tbe instanceof TypePriorities) ? "A Type System or any descriptor containing or "
                                                + "importing the type system associated with this TypePriority Definition,\n    other than a Collection Processing Engine"
                                                : (tbe instanceof ResourceManagerConfiguration) ? "A descriptor (such as an Analysis Engine) containing "
                                                        + "(directly or via aggregate delegates)\n    "
                                                        + "the External Resource Dependencies referenced by this Resource Manager Configuration"
                                                        : "unhandled - error" }));

    AbstractSection.spacer(parent);

    contextPathGUI = new Text(parent, SWT.BORDER);
    contextPathGUI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    contextPathGUI.setText(null == initialPath ? "" : initialPath);
    // AbstractSection.spacer(parent);

    Composite composite = (Composite) super.createDialogArea(parent);
    return composite;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#handleEvent(org.eclipse.swt.widgets.Event)
   */
  public void handleEvent(Event event) {
    super.handleEvent(event);
    
    if (event.widget == resourcesUI && event.type == SWT.Selection) {
      if (null != pickedResource) { 
        IFile f = (IFile)getResult()[0];
        contextPathGUI.setText(f.getLocation().toOSString());
      }   
    }
  }

  protected void okPressed() {
    contextPath = contextPathGUI.getText();
    super.okPressed();
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#enableOK()
   */
  public void enableOK() {
    super.enableOK();
    String path = contextPathGUI.getText();
    if (null != path && 
        !"".equals(path))
      okButton.setEnabled(true);
  }

  
  /*
  public void checkStateChanged(CheckStateChangedEvent event) {
    // event.getChecked(); // true if checked
    // event.getElement(); // File with workspace-relative path
    if (event.getChecked() && event.getElement() instanceof IFile) {
      contextPathGUI.setText(((IFile) event.getElement()).getLocation().toString());
    }
    okButton.setEnabled(
            selectionGroup.getCheckedElementCount() > 0 || contextPathGUI.getText().length() > 0);
  }

  protected void initializeDialog() {
    selectionGroup.addCheckStateListener(this);
    getOkButton().setEnabled(contextPathGUI.getText().length() > 0);
  }
  */
}
