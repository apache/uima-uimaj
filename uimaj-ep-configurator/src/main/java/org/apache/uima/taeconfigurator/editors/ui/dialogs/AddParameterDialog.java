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
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;
import org.apache.uima.taeconfigurator.editors.ui.ParameterSection;
import org.apache.uima.taeconfigurator.model.ConfigGroup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;


/**
 * The Class AddParameterDialog.
 */
public class AddParameterDialog extends AbstractDialogKeyVerifyJavaNames {
  
  /** The parm name UI. */
  private StyledText parmNameUI;

  /** The ext parm name UI. */
  private StyledText extParmNameUI;
  
  /** The multi value UI. */
  public Button multiValueUI;

  /** The mandatory UI. */
  private Button mandatoryUI;

  /** The parm type UI. */
  public CCombo parmTypeUI;

  /** The description UI. */
  private Text descriptionUI;

  /** The parm name. */
  public String parmName;
  
  /** The ext parm name. */
  public String extParmName;

  /** The multi value. */
  public boolean multiValue;

  /** The mandatory. */
  public boolean mandatory;

  /** The parm type. */
  public String parmType;

  /** The description. */
  public String description;

  /** The parm section. */
  private ParameterSection parmSection;

  /** The existing CP. */
  private ConfigurationParameter existingCP;

  /** The original parm name. */
  private String originalParmName;

  /** The config group. */
  private ConfigGroup configGroup;

  /**
   * Instantiates a new adds the parameter dialog.
   *
   * @param aSection the a section
   */
  private AddParameterDialog(AbstractSection aSection) {
    super(aSection, "Add Parameter", "Specify a parameter name && type");
    parmSection = (ParameterSection) section;
  }

  /**
   * Constructor for Editing an existing parameter.
   *
   * @param aSection the a section
   * @param aExistingCP the a existing CP
   */
  public AddParameterDialog(AbstractSection aSection, ConfigurationParameter aExistingCP) {
    this(aSection);
    existingCP = aExistingCP;
  }

  /**
   * Constructor for Adding a new parameter to a group (may be the not-in-any one).
   *
   * @param aSection the a section
   * @param cg the cg
   */
  public AddParameterDialog(AbstractSection aSection, ConfigGroup cg) {
    this(aSection);
    configGroup = cg;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
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

    descriptionUI = newDescription(twoCol, "Description of parameter (optional)");

    extParmNameUI = newLabeledSingleLineStyledText(twoCol, "External Override",
            "External overrides allow a parameter's value to be overriden by an entry in\n" + 
            "an external settings file, independent of the descriptor hierarchy (optional)");

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
      extParmNameUI.setText(convertNull(existingCP.getExternalOverrideName()));
    }
    originalParmName = parmNameUI.getText(); // for validity testing in edit case
    return mainArea;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#copyValuesFromGUI()
   */
  @Override
  public void copyValuesFromGUI() {
    parmName = parmNameUI.getText();
    multiValue = multiValueUI.getSelection();
    mandatory = mandatoryUI.getSelection();
    description = nullIf0lengthString(descriptionUI.getText());
    parmType = parmTypeUI.getText();
    extParmName = nullIf0lengthString(extParmNameUI.getText());
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#isValid()
   */
  @Override
  public boolean isValid() {
    if (parmName.length() == 0)
      return false;
    if (!parmName.equals(originalParmName) && parmSection.parameterNameAlreadyDefined(parmName,configGroup))
      return false;
    if (parmType.length() == 0)
      return false;
    return true;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#enableOK()
   */
  @Override
  public void enableOK() {
    copyValuesFromGUI();
    okButton.setEnabled((parmName.length() > 0) && (parmType.length() > 0));
  }
}
