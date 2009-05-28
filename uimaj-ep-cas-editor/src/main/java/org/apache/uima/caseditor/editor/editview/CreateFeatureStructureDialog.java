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
 */package org.apache.uima.caseditor.editor.editview;

import java.util.Collection;
import java.util.HashSet;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.caseditor.editor.fsview.ITypePaneListener;
import org.apache.uima.caseditor.editor.fsview.TypeCombo;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IconAndMessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class CreateFeatureStructureDialog extends IconAndMessageDialog {

  private final String title;

  private Label sizeLabel;

  private Text sizeText;

  private int arraySize;

  private final TypeSystem typeSystem;

  private final Type superType;

  private boolean isArraySizeDisplayed;

  private TypeCombo typeSelection;

  private Type selectedType;

  private Collection<Type> filterTypes;

  /**
   * Initializes a the current instance.
   *
   * @param parentShell
   */
  protected CreateFeatureStructureDialog(Shell parentShell, Type superType, TypeSystem typeSystem) {

    super(parentShell);

    this.superType = superType;

    this.typeSystem = typeSystem;

    if (!superType.isArray()) {
      title = "Choose type";
      message = "Please choose the type to create.";
    } else {
      title = "Array size";
      message = "Please enter the size of the array.";
    }

    filterTypes = new HashSet<Type>();
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_ARRAY_BASE));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_BYTE));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_ANNOTATION_BASE));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_SHORT));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_LONG));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_FLOAT));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_DOUBLE));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_BOOLEAN));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_FLOAT));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_INTEGER));
//    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_LIST_BASE));
//    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_NON_EMPTY_FLOAT_LIST));
//    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_NON_EMPTY_FS_LIST));
//    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_NON_EMPTY_INTEGER_LIST));
//    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_NON_EMPTY_STRING_LIST));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_SOFA));
    filterTypes.add(typeSystem.getType(CAS.TYPE_NAME_STRING));

  }


  @Override
  protected void configureShell(Shell newShell) {
    newShell.setText(title);
  }

  private void enableSizeEnter(Composite parent) {

    if (!isArraySizeDisplayed) {

      sizeLabel = new Label(parent, SWT.NONE);
      sizeLabel.setText("Size:");

      GridData sizeLabelData = new GridData();
      sizeLabelData.horizontalAlignment = SWT.LEFT;
      sizeLabel.setLayoutData(sizeLabelData);

      sizeText = new Text(parent, SWT.BORDER);

      GridData sizeTextData = new GridData();
      sizeTextData.grabExcessHorizontalSpace = true;
      sizeTextData.horizontalAlignment = SWT.FILL;
      sizeText.setLayoutData(sizeTextData);

      sizeText.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent event) {
          try {
            arraySize = Integer.parseInt(sizeText.getText());
          } catch (NumberFormatException e) {
            arraySize = -1;
          }
        }
      });

      isArraySizeDisplayed = true;
    }
  }

  private void disableSizeEnter() {

    if (isArraySizeDisplayed) {
      sizeLabel.dispose();
      sizeText.dispose();
      isArraySizeDisplayed = false;
    }
  }

  @Override
  protected Control createDialogArea(final Composite parent) {

    createMessageArea(parent);

    final Composite labelAndText = (Composite) super.createDialogArea(parent);
    ((GridLayout) labelAndText.getLayout()).numColumns = 1;

    GridData labelAndTextData = new GridData(GridData.FILL_BOTH);
    labelAndTextData.horizontalSpan = 2;
    labelAndText.setLayoutData(labelAndTextData);

    if (!superType.isArray()) {
      
      Composite typePanel = new Composite(labelAndText, SWT.NULL);
      
      GridLayout typePanelLayout = new GridLayout();
      typePanelLayout.numColumns = 2;
      typePanel.setLayout(typePanelLayout);
      
      Label typeLabel = new Label(typePanel, SWT.NONE);
      typeLabel.setText("Type: ");
      
      typeSelection = new TypeCombo(typePanel, superType, typeSystem, filterTypes);

      selectedType = typeSelection.getType();

      // maybe consider to show the type of the array and disable the selector
      GridData typeSelectionData = new GridData();
      typeSelectionData.horizontalSpan = 1;
      typeSelectionData.horizontalAlignment = SWT.FILL;
      typeSelectionData.grabExcessHorizontalSpace = true;

      typeSelection.setLayoutData(typeSelectionData);

      typeSelection.addListener(new ITypePaneListener() {
        public void typeChanged(Type newType) {
          selectedType = newType;

          if (newType.isArray()) {
            enableSizeEnter(labelAndText);
          } else {
            disableSizeEnter();
          }

          parent.pack(true);
        }
      });
    }

    if (superType.isArray()) {
      enableSizeEnter(labelAndText);
    }

    return labelAndText;
  }

    @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, "Create", true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  @Override
  protected Image getImage() {
    return getShell().getDisplay().getSystemImage(SWT.ICON_QUESTION);
  }

  int getArraySize() {
    return arraySize;
  }

  Type getType() {
    return selectedType;
  }
}
