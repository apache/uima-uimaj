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

/*
 * Collects the following information parm name (suggests they must be unique within the descriptor)
 * MultiValued Mandatory (for aggregates:) 1st overrides
 * 
 * For overrides, uses common code in section to allow use of a hierarchical browser that will go
 * thru defined/available keys, with parms at the bottom. Next sentence I think is false: This is
 * optional - user may not have fully defined things below. So won't be an error, here, but will be
 * when validated.
 */
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;
import org.apache.uima.taeconfigurator.editors.ui.ParameterSection;

public class AddParameterDialog extends AbstractDialogKeyVerifyJavaNames {
  private StyledText parmNameUI;

  public Button multiValueUI;

  private Button mandatoryUI;

  public CCombo parmTypeUI;

  private Text descriptionUI;

  public String parmName;

  public boolean multiValue;

  public boolean mandatory;

  public String parmType;

  public String description;

  private ParameterSection parmSection;

  private ConfigurationParameter existingCP;

  private String originalParmName;

  public AddParameterDialog(AbstractSection aSection) {
    super(aSection, "Add Parameter", "Specify a parameter name and press OK");
    parmSection = (ParameterSection) section;
  }

  /**
   * Constructor for Editing an existing parameter
   * 
   * @param aSection
   * @param aExistingCP
   */
  public AddParameterDialog(AbstractSection aSection, ConfigurationParameter aExistingCP) {
    this(aSection);
    existingCP = aExistingCP;
  }

  protected Control createDialogArea(Composite parent) {
    Composite mainArea = (Composite) super.createDialogArea(parent, existingCP);
    createWideLabel(mainArea, "Parameter names must be unique within this descriptor");

    // This part of the form looks like this sketch
    //   
    // parmName: Text field << in 2 grid composite
    // parmType: CCombo << in 2 grid composite
    // description: Text field << in 2 grid composite
    // (checkbox) is MultiValued
    // (checkbox) is Mandatory
    // 

    Composite twoCol = new2ColumnComposite(mainArea);

    parmNameUI = newLabeledSingleLineStyledText(twoCol, "Parameter Name",
                    "The unique name of the parameter");

    parmTypeUI = newLabeledCCombo(twoCol, "Parameter Type",
                    "Select the type of the parameter from the pull-down list");
    parmTypeUI.add("String");
    parmTypeUI.add("Float");
    parmTypeUI.add("Integer");
    parmTypeUI.add("Boolean");

    descriptionUI = newDescription(twoCol, S_);

    multiValueUI = newButton(mainArea, SWT.CHECK, "Parameter is multi-valued",
                    "Check the box if the parameter is multi-valued");

    mandatoryUI = newButton(mainArea, SWT.CHECK, "Parameter is mandatory",
                    "Check the box if the parameter is mandatory");

    if (section.isAggregate()) {
      multiValueUI.setEnabled(false); // can't change this
      parmTypeUI.setEnabled(false);
    }

    newErrorMessage(mainArea);

    if (null != existingCP) {
      descriptionUI.setText(convertNull(existingCP.getDescription()));
      multiValueUI.setSelection(existingCP.isMultiValued());
      mandatoryUI.setSelection(existingCP.isMandatory());
      parmNameUI.setText(convertNull(existingCP.getName()));
      parmTypeUI.setText(convertNull(existingCP.getType()));
    }
    originalParmName = parmNameUI.getText(); // for validity testing in edit case
    return mainArea;
  }

  public void copyValuesFromGUI() {
    parmName = parmNameUI.getText();
    multiValue = multiValueUI.getSelection();
    mandatory = mandatoryUI.getSelection();
    description = nullIf0lengthString(descriptionUI.getText());
    parmType = parmTypeUI.getText();
  }

  public boolean isValid() {
    if (parmName.length() == 0)
      return false;
    if (!parmName.equals(originalParmName) && parmSection.parameterNameAlreadyDefined(parmName))
      return false;
    if (parmType.length() == 0)
      return false;
    return true;
  }

  public void enableOK() {
    copyValuesFromGUI();
    okButton.setEnabled((parmName.length() > 0) && (parmType.length() > 0));
  }
}
