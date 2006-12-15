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

import java.util.Arrays;
import java.util.Comparator;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;
import org.apache.uima.taeconfigurator.editors.ui.TypeSection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class AddTypeDialog extends AbstractDialogKeyVerifyJavaNames {

  private StyledText typeNameUI;

  private Text supertypeNameUI;

  private Text descriptionUI;

  public String typeName;

  private String originalTypeName;

  public String supertypeName;

  public String description;

  private TypeSection typeSection;

  private TypeDescription existingTd = null;

  private TypesWithNameSpaces allTypesList;

  // private boolean seenThisAlready = false;

  public AddTypeDialog(AbstractSection aSection) {
    super(aSection, "Add a Type", "Use this panel to specify a type.");
    typeSection = (TypeSection) aSection;
  }

  /**
   * Constructor for Editing an existing Type DescriptionD
   * 
   * @param aSection
   * @param aExistingTd
   */
  public AddTypeDialog(AbstractSection aSection, TypeDescription aExistingTd) {
    this(aSection);
    existingTd = aExistingTd;
  }

  protected Control createDialogArea(Composite parent) {
    Composite mainArea = (Composite) super.createDialogArea(parent, existingTd);
    createWideLabel(mainArea,
            "Type names must be globally unique, unless you are intentionally redefining another type.");

    // This part of the form looks like this sketch
    //   
    // typeName: Text field << in 2 grid composite
    // supertypeName: Text field << in 2 grid composite
    // description: Text field << in 2 grid composite

    Composite twoCol = new Composite(mainArea, SWT.NONE);
    twoCol.setLayout(new GridLayout(2, false)); // false = not equal width
    twoCol.setLayoutData(new GridData(GridData.FILL_BOTH));

    typeNameUI = newLabeledSingleLineStyledText(twoCol, "Type Name", S_);
    typeNameUI.setText("some.typename.you.Choose");

    new Label(twoCol, SWT.NONE).setText("Supertype:");
    supertypeNameUI = newTypeInput(section, twoCol);
    descriptionUI = newDescription(twoCol, S_);
    newErrorMessage(twoCol, 2);

    if (null != existingTd) {
      descriptionUI.setText(convertNull(existingTd.getDescription()));
      typeNameUI.setText(originalTypeName = existingTd.getName());
      supertypeNameUI.setText(convertNull(existingTd.getSupertypeName()));
    }
    // setting this triggers the handle event for modify
    // so this has to follow setting up the other widgets
    // because handle event reads them
    else
      supertypeNameUI.setText(CAS.TYPE_NAME_ANNOTATION);

    return mainArea;
  }

  /*
   * Supplies the list of valid types
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#getTypeSystemInfoList()
   */
  public TypesWithNameSpaces getTypeSystemInfoList() {
    TypesWithNameSpaces result = super.getTypeSystemInfoList();
    boolean hasFeatures = false;
    boolean hasAllowedValues = false;
    if (null != existingTd) {
      hasFeatures = ((null != existingTd.getFeatures()) && (existingTd.getFeatures().length > 0));
      hasAllowedValues = ((null != existingTd.getAllowedValues()) && (existingTd.getAllowedValues().length > 0));
      if (hasAllowedValues) {
        result.add(CAS.TYPE_NAME_STRING);
        allTypesList = result;
        return result;
      }
    }
    Type[] allTypes = (Type[]) editor.allTypes.get().values().toArray(new Type[0]);
    Arrays.sort(allTypes, new Comparator() {
      public int compare(Object o1, Object o2) {
        Type t1 = (Type) o1;
        Type t2 = (Type) o2;
        return t1.getShortName().compareTo(t2.getShortName());
      }
    });

    for (int i = 0; i < allTypes.length; i++) {
      Type type = allTypes[i];
      if (type.isInheritanceFinal() && !CAS.TYPE_NAME_STRING.equals(type.getName()))
        continue;
      if (hasFeatures && CAS.TYPE_NAME_STRING.equals(type.getName()))
        continue;
      result.add(type.getName());
    }
    allTypesList = result;
    return result;
  }

  public void copyValuesFromGUI() {
    typeName = typeNameUI.getText();
    description = nullIf0lengthString(descriptionUI.getText());
    supertypeName = supertypeNameUI.getText();
  }

  public boolean isValid() {
    if (typeName.length() == 0)
      return false;
    if (typeName.charAt(typeName.length() - 1) == '.') {
      setErrorMessage("Name cannot end with a period (.)");
      return false;
    }
    if (!typeName.equals(originalTypeName)) {
      String errMsg = typeSection.checkDuplTypeName(typeName);
      if (null != errMsg) {
        setErrorMessage(errMsg);
        return false;
      }
    }
    if (!typeContainedInTypeSystemInfoList(supertypeName, allTypesList)) {
      setErrorMessage("SuperType '" + supertypeName
              + "' is unknown. If this is intended, please define it first.");
      return false;
    }
    TypeDescription importedType = editor.getImportedTypeSystemDesription().getType(typeName);
    if (null != importedType) {
      if (!supertypeName.equals(importedType.getSupertypeName())) {
        setErrorMessage("The supertype specified must be '" + importedType.getSupertypeName()
                + "' due to merging with imported types.  Please change it to this type.");
        return false;
      }
    }
    return true;
  }

  public void enableOK() {
    copyValuesFromGUI();
    okButton.setEnabled(typeName.length() > 0);
  }
}
