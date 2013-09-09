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

import java.util.Hashtable;

import org.apache.uima.pear.PearException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;

import org.apache.uima.pear.tools.InstallationDescriptor;

/**
 * 
 * This is wizard page to edit UIMA component basic information
 * 
 */
public class INSDComponentPage extends WizardPage implements InsdConstants {

  InstallationDescriptor insd;

  private Group gr;

  public String compID = "";

  public String compDescriptorPath = "";

  public String compType = COMP_TYPE_ANALYSIS_ENGINE;

  private Text compIDText;

  private Text compDescriptorPathText;

  IContainer currentContainer;

  Hashtable wizardData;

  /**
   * Constructor
   */
  public INSDComponentPage(IContainer currentContainer, InstallationDescriptor insd,
          Hashtable wizardData) {
    super("wizardPage");
    setTitle("UIMA - Installation Descriptor - Component Information");
    setDescription("Enter information about your UIMA component. The required fields are indicated with a (*).\n"
            + "The descriptor must be specified using paths relative to the project's root (e.g. \"desc/MyTAE.xml\").\n");
    this.wizardData = wizardData;
    this.insd = insd;
    this.currentContainer = currentContainer;
    // initialize Page with insd content;
    try {
      initializePage();
      validateCompInfo();
    } catch (Throwable e) {
      PearException subEx = new PearException(
              "The operation failed because the wizard's pages could not be initialized properly.",
              e);
      subEx.openErrorDialog(getShell());
      this.dispose();
    }
  }

  /**
   * Removes the $main_root macro from the given String.
   * 
   * @param s
   *          A String instance
   * @return The given String after removing the $main_root macro.
   */
  public static String removeMacros(String s) {
    s = s.replaceFirst("\\$main_root\\./", "");
    s = s.replaceFirst("\\$main_root\\.\\\\", "");
    s = s.replaceFirst("\\$main_root/", "");
    s = s.replaceFirst("\\$main_root\\\\", "");
    s = s.replaceFirst("\\$main_root", "");
    return s;
  }

  void initializePage() {

    String temp = "";

    temp = insd.getMainComponentId();
    if (temp == null || temp.trim().length() == 0)
      compID = currentContainer.getName();
    else
      compID = temp;

    temp = insd.getMainComponentDesc();
    compDescriptorPath = temp == null ? "" : removeMacros(temp);

  }

  public Text addTextField(Composite parent, String strLabel, String strText, boolean editable) {
    Label label = new Label(parent, SWT.NULL);
    label.setText(strLabel);

    Text text = new Text(parent, SWT.BORDER | SWT.SINGLE);
    text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    text.setText(strText);
    text.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });
    text.setEditable(editable);
    return text;
  }

  /**
   * See IDialogPage#createControl(Composite)
   */
  public void createControl(Composite parent) {

    try {

      // Add fields with default values, and modifyListener that call dialogchange()
      Composite container = new Composite(parent, SWT.NULL);

      FormLayout formLayout = new FormLayout();
      container.setLayout(formLayout);

      gr = new Group(container, SWT.NONE);
      gr.setText("Component Information");

      FormData data = new FormData();
      data.width = 450;
      data.left = new FormAttachment(0, 10);
      gr.setLayoutData(data);

      GridLayout grLayout = new GridLayout();
      grLayout.numColumns = 3;
      grLayout.verticalSpacing = 4;
      gr.setLayout(grLayout);

      compIDText = addTextField(gr, "&Component ID*:", compID, true);
      Label compIDLabel = new Label(gr, SWT.NULL);
      compIDLabel.setText(" ");

      compDescriptorPathText = addTextField(gr, "&Component Descriptor*:         ",
              compDescriptorPath, true);
      addButton(gr, "   &Browse...   ", true, compDescriptorPathText);

      String fileValidationMsg = validateFiles();
      setErrorMessage(fileValidationMsg);

      dialogChanged();
      setControl(container);
    } catch (Throwable e) {
      PearException subEx = new PearException(
              "The operation failed because the wizard's pages could not be initialized properly.",
              e);
      subEx.openErrorDialog(getShell());
      this.dispose();
    }
  }

  /**
   * Ensures that all required field are set.
   */
  private void dialogChanged() {

    compID = compIDText.getText();
    System.out.println("compID: " + compID);
    compDescriptorPath = compDescriptorPathText.getText();
    System.out.println("compDescriptorPath: " + compDescriptorPath);
    insd.setMainComponentDesc(compDescriptorPath);

    saveWizardData();
    if (compID == null || compID.trim().length() == 0 || compDescriptorPath == null
            || compDescriptorPath.trim().length() == 0) {

      setPageComplete(false);
      setErrorMessage(null);
    } else
      updateStatus(null);

  }

  void validateCompInfo() {
    if (compID == null || compID.trim().length() == 0 || compDescriptorPath == null
            || compDescriptorPath.trim().length() == 0) {

      setPageComplete(false);
      setErrorMessage(null);
    } else
      updateStatus(null);
  }

  void saveWizardData() {
    wizardData.put(COMP_DESCRIPTOR_PATH, compDescriptorPath);
  }

  private void updateStatus(String message) {

    if (message == null) {
      String fileValidationMsg = validateFiles();
      setErrorMessage(fileValidationMsg);
      setPageComplete(fileValidationMsg == null);
    } else {
      setErrorMessage(message);
      setPageComplete(false);
    }
  }

  private String validateFiles() {
    String message = null;
    StringBuffer sb = new StringBuffer();

    String[] files = { compDescriptorPath }; // ,collectionIteratorDescriptorPath,casInitializerDescriptorPath,casConsumerDescriptorPath};

    for (int i = 0; i < files.length; i++) {
      String filename = files[i];
      if (filename != null && filename.trim().length() > 0) {
        filename = filename.trim();
        IFile iFile = currentContainer.getFile(new Path(filename));
        if (!iFile.exists())
          sb.append("\n  \"" + filename + "\" was not found in the current project!");
        else if (filename.trim().indexOf(".xml") == -1)
          sb.append("\n  \"" + filename + "\" is not an xml file!");
      }
    }
    String s = sb.toString();
    if (s.length() > 0)
      message = s;

    return message;
  }

  /**
   * Creates a new button
   * <p>
   * The <code>Dialog</code> implementation of this framework method creates a standard push
   * button, registers for selection events including button presses and registers default buttons
   * with its shell. The button id is stored as the buttons client data. Note that the parent's
   * layout is assumed to be a GridLayout and the number of columns in this layout is incremented.
   * Subclasses may override.
   * </p>
   * 
   * @param parent
   *          the parent composite
   * @param label
   *          the label from the button
   * @param defaultButton
   *          <code>true</code> if the button is to be the default button, and <code>false</code>
   *          otherwise
   */
  protected Button addButton(Composite parent, String label, boolean defaultButton, final Text text) {

    Button button = new Button(parent, SWT.PUSH);
    button.setText(label);

    if (defaultButton) {
      Shell shell = parent.getShell();
      if (shell != null) {
        shell.setDefaultButton(button);
      }
      button.setFocus();
    }
    button.setFont(parent.getFont());

    SelectionListener listener = new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {

        ResourceSelectionDialog dialog = new ResourceSelectionDialog(getShell(), currentContainer,
                "Selection Dialog");
        dialog.setTitle("Selection Dialog");
        dialog.setMessage("Please select a file:");
        dialog.open();
        Object[] result = dialog.getResult();
        if (result[0] != null) {
          IResource res = (IResource) result[0];
          text.setText(res.getProjectRelativePath().toOSString());
        }

      }
    };
    button.addSelectionListener(listener);

    return button;
  }

  /**
   * Creates a new Radio button
   * 
   * @param parent
   *          the parent composite
   * @param label
   *          the label from the button
   * @param initialSelection
   *          <code>true</code> if the button is to be the default button, and <code>false</code>
   *          otherwise
   */
  protected Button addRadioButton(Composite parent, String label, boolean initialSelection) {

    Button button = new Button(parent, SWT.RADIO);
    button.setText(label);
    button.setFont(parent.getFont());
    button.setSelection(initialSelection);

    SelectionListener listener = new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        dialogChanged();
      }
    };
    button.addSelectionListener(listener);
    return button;
  }

  /*
   * see @DialogPage.setVisible(boolean)
   */
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible)
      compIDText.setFocus();
  }
}