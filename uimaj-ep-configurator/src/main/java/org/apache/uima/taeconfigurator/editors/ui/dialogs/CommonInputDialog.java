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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;

/**
 */
public class CommonInputDialog extends AbstractDialogKeyVerify {

  public static final int PLAIN_NAME = 1;

  public static final int DOTTED_NAME = 1 << 1;

  public static final int SPACED_NAMES = 1 << 2;

  public static final int LANGUAGE = 1 << 3;

  public static final int ALLOK = 1 << 4;

  public static final int TRUE_FALSE = 1 << 5;

  public static final int INTEGER = 1 << 6;

  public static final int FLOAT = 1 << 7;

  public static final int GROUP_NAMES = DOTTED_NAME | SPACED_NAMES | LANGUAGE;

  private int validation;

  private String existing;

  private StyledText text;

  private String result;

  public CommonInputDialog(AbstractSection aSection, String title, String dialogDescription,
                  int aKind) {
    super(aSection, title, dialogDescription);
    validation = aKind;
  }

  public CommonInputDialog(AbstractSection aSection, String title, String dialogDescription,
                  int aKind, String aExisting) {
    this(aSection, title, dialogDescription, aKind);
    existing = aExisting;
  }

  protected Control createDialogArea(Composite parent) {
    Composite mainArea = (Composite) super.createDialogArea(parent, existing);

    text = newSingleLineStyledText(mainArea, "");
    AbstractSection.spacer(mainArea);
    newErrorMessage(mainArea);

    if (null != existing)
      text.setText(existing);
    else
      text.setText(S_);

    return mainArea;
  }

  public boolean verifyKeyChecks(VerifyEvent event) {
    char ch = event.character;

    boolean validateDottedName = ((validation & DOTTED_NAME) == DOTTED_NAME);
    boolean validateSpaces = ((validation & SPACED_NAMES) == SPACED_NAMES);
    boolean validateLanguage = ((validation & LANGUAGE) == LANGUAGE);
    boolean validateAllOK = ((validation & ALLOK) == ALLOK);
    boolean validateTrueFalse = ((validation & TRUE_FALSE) == TRUE_FALSE);
    boolean validateInteger = ((validation & INTEGER) == INTEGER);
    boolean validateFloat = ((validation & FLOAT) == FLOAT);

    if (event.keyCode == SWT.CR || event.keyCode == SWT.TAB || event.keyCode == SWT.BS)
      return true;

    if (validateTrueFalse) {
      return ("truefalse".indexOf(ch) >= 0);
    }

    if (validateSpaces && ch == ' ')
      return true;

    if (validateDottedName && ch == '.')
      return true;

    if ((!validateTrueFalse) && (!validateInteger) && (!validateFloat)
                    && Character.isJavaIdentifierPart(ch))
      return true;

    if (validateLanguage && ch == '-')
      return true;

    if (validateAllOK)
      return true;

    if (validateInteger)
      if (Character.isDigit(ch) || ch == '-')
        return true;

    if (validateFloat) {
      if (Character.isDigit(ch) || ch == '-' || ch == 'E' || ch == 'e' || ch == '.')
        return true;
    }
    return false;
  }

  public String getValue() {
    return result;
  }

  public void enableOK() {
    copyValuesFromGUI();
    okButton.setEnabled("".equals(errorMessageUI.getText()) && (text.getText().length() > 0));
  }

  public void copyValuesFromGUI() {
    result = text.getText();
  }

  public boolean isValid() {
    return true;
  }

}
