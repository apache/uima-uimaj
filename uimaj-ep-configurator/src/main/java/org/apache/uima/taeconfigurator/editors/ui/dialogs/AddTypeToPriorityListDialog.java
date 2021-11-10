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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;


/**
 * The Class AddTypeToPriorityListDialog.
 */
public class AddTypeToPriorityListDialog extends AbstractDialog {

  /** The m selected type names. */
  private String[] m_selectedTypeNames; // this is the selection

  /** The m available type names. */
  private String[] m_availableTypeNames;

  /** The type list. */
  private List typeList;


  /**
   * Instantiates a new adds the type to priority list dialog.
   *
   * @param aSection the a section
   * @param allowableTypeNameHash the allowable type name hash
   * @param typesInList the types in list
   */
  public AddTypeToPriorityListDialog(AbstractSection aSection, Set allowableTypeNameHash,
          String[] typesInList) {
    super(aSection, "Add Types to Priority List", "Select one or more types and press OK");

    m_availableTypeNames = getAvailableTypeNames(allowableTypeNameHash, typesInList);
  }

  /**
   * Gets the available type names.
   *
   * @param allowableTypeNameHash the allowable type name hash
   * @param alreadyUsedTypes the already used types
   * @return the available type names
   */
  private String[] getAvailableTypeNames(Set allowableTypeNameHash, String[] alreadyUsedTypes) {

    Arrays.sort(alreadyUsedTypes);

    HashSet availableHash = new HashSet();
    Iterator typeNameIterator = allowableTypeNameHash.iterator();

    while (typeNameIterator.hasNext()) {
      String sTypeName = (String) typeNameIterator.next();
      if (0 > Arrays.binarySearch(alreadyUsedTypes, sTypeName)
              && !CAS.TYPE_NAME_TOP.equals(sTypeName)) {
        availableHash.add(sTypeName);
      }
    }

    String[] availableTypeNames = (String[]) availableHash.toArray(stringArray0);
    Arrays.sort(availableTypeNames);

    return availableTypeNames;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    // create composite
    Composite composite = (Composite) super.createDialogArea(parent);

    typeList = new List(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.heightHint = 100;
    typeList.setLayoutData(gridData);

    for (int i = 0; i < m_availableTypeNames.length; i++) {
      typeList.add(m_availableTypeNames[i]);
    }

    return composite;
  }

  /**
   * Gets the selected type names.
   *
   * @return the selected type names
   */
  public String[] getSelectedTypeNames() {
    return m_selectedTypeNames.clone();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#copyValuesFromGUI()
   */
  @Override
  public void copyValuesFromGUI() {
    m_selectedTypeNames = new String[typeList.getSelectionCount()];
    for (int i = 0, j = 0; i < m_availableTypeNames.length; i++) {
      if (typeList.isSelected(i)) {
        m_selectedTypeNames[j++] = m_availableTypeNames[i];
      }
    }
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#isValid()
   */
  @Override
  public boolean isValid() {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#enableOK()
   */
  @Override
  public void enableOK() {
    okButton.setEnabled(true);
  }
}
