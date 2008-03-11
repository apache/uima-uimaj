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
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;
import org.apache.uima.taeconfigurator.editors.ui.CapabilitySection;

public class AddSofaDialog extends AbstractDialogKeyVerify {

  private StyledText sofaNameUI;

  private CapabilitySection capabilitySection;

  private Capability capability;

  private String existingSofa;

  private boolean existingIsInput;

  private String originalSofa;

  public String sofaName;

  public boolean isInput;

  private Button inputButton;

  private Button outputButton;

  public AddSofaDialog(AbstractSection aSection, Capability c) {
    super(aSection, "Add a Sofa", "Use this panel to specify a Sofa Name.");
    capabilitySection = (CapabilitySection) aSection;
    capability = c;
  }

  /**
   * Constructor for Editing an existing Sofa Name
   * 
   * @param aSection
   * @param aExistingTd
   */
  public AddSofaDialog(AbstractSection aSection, Capability c, String aExistingSofa,
          boolean aIsInput) {
    this(aSection, c);
    existingSofa = aExistingSofa;
    existingIsInput = aIsInput;
  }

  protected Control createDialogArea(Composite parent) {
    Composite mainArea = (Composite) super.createDialogArea(parent, existingSofa);
    createWideLabel(mainArea, "Sofa names must be unique within a Capability Set, and are"
            + " simple names without name spaces (no dots in the name).\n\n" +
            		" As a special case, they may end in .*\n" +
            		"   - Use this form to designate a class of sofa names, where the class\n" +
            		"     is all names that match the part up to the dot.\n\n" 
            + "Type the name in the box below, and specify if it is an input Sofa\n"
            + "(created outside of this component), or an output Sofa (created by this component).");

    // This part of the form looks like this sketch
    //   
    // SofaName: Text field << in 2 grid composite
    // Input / Output: 2 radio checkboxes << in 2 grid composite
    //
    // Later: for Aggregates:
    // a table with sofa name, component key-name, input / output x's in columns
    // to allow easy "picking" for same-name things

    Composite twoCol = new Composite(mainArea, SWT.NONE);
    twoCol.setLayout(new GridLayout(2, false)); // false = not equal width
    twoCol.setLayoutData(new GridData(GridData.FILL_BOTH));

    sofaNameUI = newLabeledSingleLineStyledText(twoCol, "Sofa Name", S_);
    sofaNameUI.setText("someNewSofaName");

    new Label(twoCol, SWT.NONE).setText("Input / Output:");
    Composite io = new Composite(twoCol, SWT.NONE);
    io.setLayout(new GridLayout(2, true));
    io.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    inputButton = newButton(io, SWT.RADIO, "Input", "Click here to specify this Sofa is an input.");
    outputButton = newButton(io, SWT.RADIO, "Output",
            "Click here to specify this Sofa as an output.");
    inputButton.setSelection(true);
    outputButton.setSelection(false);

    newErrorMessage(twoCol, 2);

    if (null != existingSofa) {
      sofaNameUI.setText(originalSofa = existingSofa);
      if (existingIsInput) {
        inputButton.setSelection(true);
        outputButton.setSelection(false);
      } else {
        outputButton.setSelection(true);
        inputButton.setSelection(false);
      }
    }
    return mainArea;
  }

  public void copyValuesFromGUI() {
    sofaName = sofaNameUI.getText();
    isInput = inputButton.getSelection();
  }

  /**
   * Duplicate validity check: Duplicates are OK for sofas belonging to other capability sets,
   * provided they have the same Input or Output setting.
   */
  public boolean isValid() {
    if (sofaName.length() == 0)
      return false;
    if (!sofaName.equals(originalSofa) || // true for adding new sofa, or sofa name changed on edit
            isInput != existingIsInput) { // true if input / output switched for editing
      // sofa, not changed name
      String errMsg = checkDuplSofaName();
      if (null != errMsg) {
        setErrorMessage(errMsg);
        return false;
      }
    }
    if ((sofaName.contains(".") || sofaName.contains("*")) &&
        (sofaName.indexOf('.') != (sofaName.length() - 2) ||
         sofaName.indexOf('*') != (sofaName.length() - 1))) {
      setErrorMessage("Sofa Name cannot have the characters '.' or '*' except as the last 2 characters");
      return false;
    }
    return true;
  }

  public void enableOK() {
    copyValuesFromGUI();
    okButton.setEnabled(sofaName.length() > 0);
  }

  private String checkDuplSofaName() {

    Capability[] cSets = capabilitySection.getCapabilities();
    // check for dis-allowed duplicates in other capability sets
    for (int i = 0; i < cSets.length; i++) {
      Capability ci = cSets[i];
      if (ci == capability)
        continue;
      // "reverse" i and o - if input validate name not exist as output in other sets, etc.
      String[] sofaNames = isInput ? ci.getOutputSofas() : ci.getInputSofas();
      if (null != sofaNames)
        for (int j = 0; j < sofaNames.length; j++) {
          if (sofaName.equals(sofaNames[j]))
            return "This name exists as an " + (isInput ? "output" : "input")
                    + " in some capability set.  Please choose another name, or "
                    + "switch the input/output specification to the opposite setting.";
        }
    }
    // check for duplicates in this capability
    if (!sofaName.equals(originalSofa)) { // means adding new sofa or changing name of existing one
      if (checkDuplSofaName1(sofaName, capability.getInputSofas())
              || checkDuplSofaName1(sofaName, capability.getOutputSofas()))
        return "This name already in use; please choose a different name.";
    }
    return null;
  }

  private boolean checkDuplSofaName1(String name, String[] names) {
    if (null == names)
      return false;
    for (int i = 0; i < names.length; i++) {
      if (name.equals(names[i]))
        return true;
    }
    return false;
  }

  public boolean verifyKeyChecks(VerifyEvent event) {
    if (event.keyCode == SWT.CR || event.keyCode == SWT.TAB)
      return true;
    if (Character.isJavaIdentifierPart(event.character) ||
        event.character == '*' ||
        event.character == '.')
      return true;
    return false;
  }

}
