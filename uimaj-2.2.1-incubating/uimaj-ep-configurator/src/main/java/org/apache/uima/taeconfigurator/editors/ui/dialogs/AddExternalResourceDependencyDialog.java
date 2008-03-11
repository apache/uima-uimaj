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

package org.apache.uima.taeconfigurator.editors.ui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import org.apache.uima.resource.ExternalResourceDependency;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;
import org.apache.uima.taeconfigurator.editors.ui.ResourceDependencySection;

public class AddExternalResourceDependencyDialog extends AbstractDialogKeyVerifyJavaNames {

  private StyledText keyNameUI;

  public Button optionalUI;

  private StyledText interfaceNameUI;

  private Text descriptionUI;

  public String keyName;

  private String originalKeyName;

  public boolean optional;

  public String interfaceName;

  public String description;

  private ResourceDependencySection rdSection;

  private ExternalResourceDependency existingXRD;

  public AddExternalResourceDependencyDialog(AbstractSection aSection) {
    super(aSection, "Add External Resource Dependency", "Add an External Resource Dependency");
    rdSection = (ResourceDependencySection) aSection;
  }

  /**
   * Constructor for Editing an existing XRD
   * 
   * @param aSection
   * @param aExistingXRD
   */
  public AddExternalResourceDependencyDialog(AbstractSection aSection,
          ExternalResourceDependency aExistingXRD) {
    this(aSection);
    existingXRD = aExistingXRD;
  }

  protected Control createDialogArea(Composite parent) {
    Composite mainArea = (Composite) super.createDialogArea(parent, existingXRD);
    createWideLabel(
            mainArea,
            "The only required field is the key name,\nwhich must be unique within this primitive Analysis Engine descriptor.");

    // This part of the form looks like this sketch
    //   
    // keyName: Text field << in 2 grid composite
    // description: Text field << in 2 grid composite
    // impl Name: Text field << in 2 grid composite
    // (checkbox) is Optional
    // 

    Composite twoCol = new2ColumnComposite(mainArea);

    keyNameUI = newLabeledSingleLineStyledText(twoCol, "Key",
            "Name used by the Primitive Analysis Engine to refer to the resource");

    descriptionUI = newDescription(twoCol, "(Optional)Describes this resource dependency");

    interfaceNameUI = newLabeledSingleLineStyledText(
            twoCol,
            "Interface",
            "The fully qualified name of the Java Interface class used by the Analysis Engine to refer to the External Resource");

    newErrorMessage(twoCol, 2);

    optionalUI = newButton(mainArea, SWT.CHECK, "Check this box if this resource is optional",
            "Uncheck if this resource is required");

    if (null != existingXRD) {
      descriptionUI.setText(convertNull(existingXRD.getDescription()));
      optionalUI.setSelection(existingXRD.isOptional());
      keyNameUI.setText(originalKeyName = existingXRD.getKey());
      interfaceNameUI.setText(convertNull(existingXRD.getInterfaceName()));
    }

    return mainArea;
  }

  public void copyValuesFromGUI() {
    keyName = keyNameUI.getText();
    optional = optionalUI.getSelection();
    description = nullIf0lengthString(descriptionUI.getText());
    interfaceName = nullIf0lengthString(interfaceNameUI.getText());
  }

  public boolean isValid() {
    if (keyName.length() == 0)
      return false;
    if (!keyName.equals(originalKeyName) && rdSection.keyNameAlreadyDefined(keyName))
      return false;
    return true;
  }

  public void enableOK() {
    copyValuesFromGUI();
    okButton.setEnabled(keyName.length() > 0);
  }
}
