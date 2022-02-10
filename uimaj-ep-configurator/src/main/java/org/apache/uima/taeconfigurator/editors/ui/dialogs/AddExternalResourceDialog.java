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

import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.FileLanguageResourceSpecifier;
import org.apache.uima.resource.FileResourceSpecifier;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;
import org.apache.uima.taeconfigurator.editors.ui.ExtnlResBindSection;
import org.apache.uima.taeconfigurator.editors.ui.Utility;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * The Class AddExternalResourceDialog.
 */
public class AddExternalResourceDialog extends AbstractDialogKeyVerify {

  /** The existing XRD. */
  private ExternalResourceDescription existingXRD = null;

  /** The xr name UI. */
  private StyledText xrNameUI;

  /** The original xr name. */
  private String originalXrName;

  /** The xr description UI. */
  private Text xrDescriptionUI;

  /** The xr url UI. */
  private StyledText xrUrlUI;

  /** The xr url suffix UI. */
  private StyledText xrUrlSuffixUI;

  /** The xr implementation UI. */
  private StyledText xrImplementationUI;

  /** The extnl res bind section. */
  private ExtnlResBindSection extnlResBindSection;

  /** The xr name. */
  public String xrName;

  /** The xr description. */
  public String xrDescription;

  /** The xr url. */
  public String xrUrl;

  /** The xr url suffix. */
  public String xrUrlSuffix;

  /** The xr implementation. */
  public String xrImplementation;

  /**
   * Instantiates a new adds the external resource dialog.
   *
   * @param aSection
   *          the a section
   */
  public AddExternalResourceDialog(AbstractSection aSection) {
    super(aSection, "Add an External Resource Definition", "Define and name an external resource");
    extnlResBindSection = (ExtnlResBindSection) aSection;
  }

  /**
   * Instantiates a new adds the external resource dialog.
   *
   * @param aSection
   *          the a section
   * @param aExistingXRD
   *          the a existing XRD
   */
  public AddExternalResourceDialog(AbstractSection aSection,
          ExternalResourceDescription aExistingXRD) {
    this(aSection);
    existingXRD = aExistingXRD;
    originalXrName = existingXRD.getName();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#createDialogArea(org.eclipse.
   * swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite composite = (Composite) super.createDialogArea(parent, existingXRD);

    // name: styledText name
    // description: Text multi-line
    // URL or
    // URL_Prefix: styledText URL
    // (optional)
    //
    // URL_Suffix: styledText URL_Suffix
    // (optional)
    //
    // Implementation: styledText
    // (optional)

    createWideLabel(composite,
            "The first URL field is used to identify the external resource.\nIf both URL fields are used, they form a name by concatenating the first with the document language and then with the second (suffix) URL.\nThe (optional) Implementation specifies a Java class which implements the interface used by the Analysis Engine to access the resource.");

    Composite twoCol = new2ColumnComposite(composite);

    xrNameUI = newLabeledSingleLineStyledText(twoCol, "Name:",
            "(Required) The name of this resource; it must be unique in this Analysis Engine.");
    xrDescriptionUI = newDescription(twoCol, "(Optional) Description of the External Resource");
    xrUrlUI = newLabeledSingleLineStyledText(twoCol, "URL:",
            "(Required) A URL for this resource, or the URL prefix if a suffix is being used");
    xrUrlSuffixUI = newLabeledSingleLineStyledText(twoCol, "URL Suffix",
            "(Optional) A URL part that will be suffixed to the prefix with the language being used inserted in-between");
    xrImplementationUI = newLabeledSingleLineStyledText(twoCol, "Implementation",
            "(Optional) The name of a Java class implementing the interface used by the Analysis Engine to access this resource.");
    newErrorMessage(twoCol, 2);

    if (null != existingXRD) {
      xrNameUI.setText(existingXRD.getName());
      xrDescriptionUI.setText(convertNull(existingXRD.getDescription()));
      ResourceSpecifier rs = existingXRD.getResourceSpecifier();
      if (rs instanceof FileResourceSpecifier)
        xrUrlUI.setText(((FileResourceSpecifier) rs).getFileUrl());
      else if (rs instanceof FileLanguageResourceSpecifier) {
        xrUrlUI.setText(((FileLanguageResourceSpecifier) rs).getFileUrlPrefix());
        xrUrlSuffixUI.setText(((FileLanguageResourceSpecifier) rs).getFileUrlSuffix());
      } else {
        Utility.popMessage("Unknown resource type",
                "The resource type '" + rs.getClass().getName()
                        + "' is unknown.  Editing should be done by hand in the source view.",
                MessageDialog.WARNING);
      }
      String impName = existingXRD.getImplementationName();
      xrImplementationUI.setText(convertNull(impName));
    }
    return composite;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#copyValuesFromGUI()
   */
  @Override
  public void copyValuesFromGUI() {
    xrName = xrNameUI.getText();
    xrDescription = nullIf0lengthString(xrDescriptionUI.getText());
    xrUrl = xrUrlUI.getText();
    xrUrlSuffix = nullIf0lengthString(xrUrlSuffixUI.getText());
    xrImplementation = nullIf0lengthString(xrImplementationUI.getText());
  }

  /**
   * Called for many widgets.
   *
   * @param event
   *          the event
   * @return true, if successful
   */
  @Override
  public boolean verifyKeyChecks(VerifyEvent event) {
    if (event.keyCode == SWT.CR || event.keyCode == SWT.TAB)
      return true;

    if (Character.isJavaIdentifierPart(event.character) || event.character == '.')
      return true;

    if ((event.widget == xrUrlUI || event.widget == xrUrlSuffixUI)
            && (event.character == '/' || event.character == ':'))
      return true;
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#isValid()
   */
  @Override
  public boolean isValid() {
    if (xrName.length() == 0)
      return false;
    if (!xrName.equals(originalXrName) && extnlResBindSection.resourceNameAlreadyDefined(xrName))
      return false;
    if (xrUrl.length() == 0)
      return false;
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#enableOK()
   */
  @Override
  public void enableOK() {
    copyValuesFromGUI();
    okButton.setEnabled(xrName.length() > 0 && xrUrl.length() > 0);
  }
}
