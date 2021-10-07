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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;


/**
 * The Class SelectTypeDialog.
 */
public class SelectTypeDialog extends AbstractDialog {

  /** The type name UI. */
  private Text typeNameUI;
  
  /** The matching types UI. */
  private Table matchingTypesUI;
  
  /** The name spaces UI. */
  private Table nameSpacesUI;
  
  /** The type name. */
  public String typeName = "error-TypeName-never-set";
  
  /** The name space name. */
  public String nameSpaceName = "error-NameSpaceName-never-set";
  
  /** The types. */
  private TypesWithNameSpaces types;

  /**
   * Instantiates a new select type dialog.
   *
   * @param section the section
   * @param types the types
   */

  public SelectTypeDialog(AbstractSection section, TypesWithNameSpaces types) {
    super(section, "Select Type Name", "Select an Existing CAS Type name from the set of defined types");
    this.types = types;
  }

  /*
   * (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   * 
   * Use labels on left (2 column layout)
   * Create a text input area labeled: Type:
   * Create a list output area labeled: Matching:
   * Create a list output area labeled: NameSpaces:
   * Bottom gets normal OK / Cancel buttons
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite mainArea = (Composite)super.createDialogArea(parent);
    createWideLabel(mainArea, "Type Name:");
    
    typeNameUI = newText(mainArea, SWT.SINGLE, "Specify the type name");
    typeNameUI.addListener(SWT.Modify, this);
    
    createWideLabel(mainArea, "Matching Types:");
    
    matchingTypesUI = newTable(mainArea, SWT.SINGLE);
    ((GridData)matchingTypesUI.getLayoutData()).heightHint = 250;
    ((GridData)matchingTypesUI.getLayoutData()).minimumHeight = 100;
    typeNameUI.addListener(SWT.Selection, this);

    
    createWideLabel(mainArea, "NameSpaces:");
    
    nameSpacesUI = newTable(mainArea, SWT.SINGLE);
    ((GridData)nameSpacesUI.getLayoutData()).heightHint = 75;
    ((GridData)nameSpacesUI.getLayoutData()).minimumHeight = 40;
    
    displayFilteredTypes("");
        
    return mainArea;
  }
   
  /**
   * Display filtered types.
   *
   * @param aTypeName the a type name
   */
  private void displayFilteredTypes(String aTypeName) {
    matchingTypesUI.setRedraw(false);
    matchingTypesUI.removeAll();
    Map.Entry topEntry = null;
    aTypeName = aTypeName.toLowerCase();
    for (Iterator it = types.sortedNames.entrySet().iterator(); it.hasNext();) {
      Map.Entry entry = (Map.Entry) it.next();
      String candidateTypeName = ((String)entry.getKey()).toLowerCase();
      if (candidateTypeName.startsWith(aTypeName)) {
        if (null == topEntry) {
            topEntry = entry;
        }
        TableItem item = new TableItem(matchingTypesUI, SWT.NULL);
        item.setText((String) entry.getKey());
        item.setData(entry);
      }
    }
    if (matchingTypesUI.getItemCount() > 0) {
      matchingTypesUI.select(0);
      displayNameSpacesForSelectedItem(topEntry);
    }
    
    matchingTypesUI.setRedraw(true);    
  }

  /**
   * Display name spaces for selected item.
   *
   * @param entry the entry
   */
  private void displayNameSpacesForSelectedItem(Map.Entry entry) {
    Set nameSpaces = (Set)entry.getValue();
    nameSpacesUI.removeAll();
    for (Iterator it = nameSpaces.iterator(); it.hasNext();) {
      String nameSpace = (String) it.next();
      TableItem item = new TableItem(nameSpacesUI, SWT.NULL);
      item.setText(nameSpace);
      item.setData(entry);
    }
    if (nameSpacesUI.getItemCount() > 0) {
      nameSpacesUI.select(0);
    }
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#handleEvent(org.eclipse.swt.widgets.Event)
   */
  @Override
  public void handleEvent(Event event) {
    if (event.widget == typeNameUI && event.type == SWT.Modify) {
      typeName = typeNameUI.getText(); 
      displayFilteredTypes(typeName);
    }
    
    else if (event.widget == matchingTypesUI && event.type == SWT.Selection) {
      displayNameSpacesForSelectedItem(
              (Map.Entry)(matchingTypesUI.getSelection()[0].getData()));
    }
    super.handleEvent(event);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#copyValuesFromGUI()
   */
  @Override
  public void copyValuesFromGUI() {
    if (0 < matchingTypesUI.getSelectionCount()) {
      typeName = matchingTypesUI.getSelection()[0].getText();
    }
    if (0 < nameSpacesUI.getSelectionCount()) {
      nameSpaceName = nameSpacesUI.getSelection()[0].getText();
    }
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#enableOK()
   */
  @Override
  public void enableOK() {
    copyValuesFromGUI();
    okButton.setEnabled( (0 < nameSpacesUI.getSelectionCount()) &&
                         (0 < matchingTypesUI.getSelectionCount()));
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#isValid()
   */
  @Override
  public boolean isValid() {
    return true;
  }

}
