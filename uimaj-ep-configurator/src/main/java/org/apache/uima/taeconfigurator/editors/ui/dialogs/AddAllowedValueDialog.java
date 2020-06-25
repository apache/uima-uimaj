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

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import org.apache.uima.resource.metadata.AllowedValue;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;


/**
 * The Class AddAllowedValueDialog.
 */
public class AddAllowedValueDialog extends AbstractDialogKeyVerify {
  
  /** The allowed value UI. */
  private StyledText allowedValueUI;

  /** The description UI. */
  private Text descriptionUI;

  /** The allowed value. */
  public String allowedValue;

  /** The description. */
  public String description;

  /** The existing av. */
  private AllowedValue existingAv;

  /**
   * Constructor for Adding or Editing an Allowed Value.
   *
   * @param aSection the a section
   * @param aExistingAv the a existing av
   */
  public AddAllowedValueDialog(AbstractSection aSection, AllowedValue aExistingAv) {
    super(aSection, "Add an Allowed Value for a String subtype",
            "Use this panel to add or edit an allowed value.  The allowed value is any string.");
    existingAv = aExistingAv;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite mainArea = (Composite) super.createDialogArea(parent, existingAv);

    // This part of the form looks like this sketch
    //   
    // Allowed Value: Text field << in 2 grid composite
    // description: Text field << in 2 grid composite

    Composite twoCol = new2ColumnComposite(mainArea);

    allowedValueUI = newLabeledSingleLineStyledText(twoCol, "Allowed Value",
            "A literal string value which this string is allowed to have");
    descriptionUI = newDescription(twoCol, "Description of the allowed value");
    newErrorMessage(twoCol, 2);

    if (null != existingAv) {
      descriptionUI.setText(convertNull(existingAv.getDescription()));
      allowedValueUI.setText(convertNull(existingAv.getString()));
    }

    return mainArea;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#copyValuesFromGUI()
   */
  @Override
  public void copyValuesFromGUI() {
    allowedValue = allowedValueUI.getText();
    description = nullIf0lengthString(descriptionUI.getText());
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#isValid()
   */
  @Override
  public boolean isValid() {
    if (allowedValue.length() == 0)
      return false;
    return true;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#enableOK()
   */
  @Override
  public void enableOK() {
    copyValuesFromGUI();
    okButton.setEnabled(allowedValue.length() > 0);
  }

}
