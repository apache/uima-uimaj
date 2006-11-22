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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;
import org.apache.uima.taeconfigurator.editors.ui.IndexSection;
import org.apache.uima.typesystem.TypeSystemInfo;

public class AddIndexDialog extends AbstractDialogKeyVerifyJavaNames {

  private static final String TYPE_PRIORITY = "Type Priority";

  private static final String STANDARD = "Standard";

  private static final String REVERSE = "Reverse";

  private StyledText indexNameUI;

  private Text indexTypeUI;

  private CCombo indexKindUI;

  private Table table;

  private Button addButton;

  private Button editButton;

  private Button removeButton;

  private Button upButton;

  private Button downButton;

  private Composite tableContainer;

  private Label keyTable;

  public String indexName;

  public String indexType;

  public String indexKind;

  public FsIndexKeyDescription[] keys;

  private String originalIndexName;

  private IndexSection indexSection;

  private FsIndexDescription existingNDX;

  public AddIndexDialog(AbstractSection aSection) {
    super(aSection, "Add an index", "Add or Edit an index specification");
    indexSection = (IndexSection) aSection;
  }

  /**
   * Constructor for Editing an existing XRD
   * 
   * @param aSection
   * @param aExistingXRD
   */
  public AddIndexDialog(AbstractSection aSection, FsIndexDescription aExistingNDX) {
    this(aSection);
    existingNDX = aExistingNDX;
  }

  protected Control createDialogArea(Composite parent) {
    Composite mainArea = (Composite) super.createDialogArea(parent, existingNDX);
    createWideLabel(mainArea, "The Index name must be globally unique.");

    // This part of the form looks like this sketch
    //   
    // IndexName: Text field << in 2 grid composite
    // IndexKind: combo
    // IndexType: Text field << assisted <browse button>
    // description: Text field << in 2 grid composite
    // keys: first is text field assisted <browseButton>
    // 2nd is combo asc/desc/typePriority

    Composite twoCol = new2ColumnComposite(mainArea);

    indexNameUI = newLabeledSingleLineStyledText(twoCol, "Index Name:",
                    "The globally unique index name");

    indexKindUI = newLabeledCCombo(twoCol, "Index Kind:",
                    "Specify the kind of index - sorted, set, or bag");
    indexKindUI.add("sorted");
    indexKindUI.add("bag");
    indexKindUI.add("set");

    new Label(twoCol, SWT.NONE).setText("CAS Type");
    indexTypeUI = newTypeInput(twoCol);

    setTextAndTip(
                    keyTable = new Label(twoCol, SWT.NONE),
                    "Index Keys:",
                    "For Set and Sorted index kinds, specify the keys; for Sorted indexes specify also the sort direction.");
    tableContainer = new2ColumnComposite(twoCol);
    table = newTable(tableContainer, SWT.SINGLE | SWT.FULL_SELECTION);
    table.setHeaderVisible(true);
    new TableColumn(table, SWT.NONE).setText("Feature Name");
    new TableColumn(table, SWT.NONE).setText("Sorting Direction");
    table.addListener(SWT.MouseDoubleClick, this);

    Composite buttonContainer = newButtonContainer(tableContainer);
    addButton = newPushButton(buttonContainer, S_ADD, "Click here to add an Index key");
    editButton = newPushButton(buttonContainer, S_EDIT, S_EDIT_TIP);
    removeButton = newPushButton(buttonContainer, S_REMOVE, S_REMOVE_TIP);
    upButton = newPushButton(buttonContainer, S_UP, S_UP_TIP);
    downButton = newPushButton(buttonContainer, S_DOWN, S_DOWN_TIP);

    newErrorMessage(twoCol, 2);

    if (null == existingNDX) {
      // set up defaults
      indexNameUI.setText("some.default.Name");
      indexKindUI.setText(indexKindUI.getItem(0));

    } else {
      indexNameUI.setText(originalIndexName = existingNDX.getLabel());
      indexKindUI.setText(AbstractSection.handleDefaultIndexKind(existingNDX.getKind()));
      indexTypeUI.setText(existingNDX.getTypeName());

      keys = existingNDX.getKeys();
      if (null != keys) {
        for (int i = 0; i < keys.length; i++) {
          addKey(keys[i]);
        }
      }
    }
    section.packTable(table);
    indexKindUI.addListener(SWT.Modify, this);
    boolean showKeys = "sorted".equals(indexKindUI.getText())
                    || "set".equals(indexKindUI.getText());
    tableContainer.setVisible(showKeys);
    keyTable.setVisible(showKeys);
    return mainArea;
  }

  public ArrayList getTypeSystemInfoList() {
    ArrayList result = new ArrayList();
    String[] allTypes = getAllTypesAsSortedArray();
    for (int i = 0; i < allTypes.length; i++) {
      result.add(new TypeSystemInfo(allTypes[i]));
    }
    return result;
  }

  private void addKey(FsIndexKeyDescription key) {
    if (null == key)
      return;
    TableItem item = new TableItem(table, SWT.NONE);
    updateKey(item, key);
  }

  private void updateKey(TableItem item, FsIndexKeyDescription key) {
    if (null == key)
      return;
    if (key.isTypePriority()) {
      item.setText(0, S_);
      item.setText(1, TYPE_PRIORITY);
    } else {
      item.setText(0, key.getFeatureName());
      item.setText(1, key.getComparator() == FSIndexComparator.STANDARD_COMPARE ? STANDARD
                      : REVERSE);
    }
  }

  public void handleEvent(Event event) {
    if (event.widget == indexKindUI) {
      boolean showKeys = "sorted".equals(indexKindUI.getText())
                      || "set".equals(indexKindUI.getText());
      tableContainer.setVisible(showKeys);
      keyTable.setVisible(showKeys);
    } else if (event.widget == addButton) {
      AddIndexKeyDialog dialog = new AddIndexKeyDialog(section, indexTypeUI.getText(), indexKindUI
                      .getText(), alreadyDefined(table.getItems()));
      addKey(indexSection.addOrEditIndexKey(dialog, null));
      section.packTable(table);
    } else if (event.widget == editButton || event.type == SWT.MouseDoubleClick) {
      TableItem item = table.getItem(table.getSelectionIndex());
      AddIndexKeyDialog dialog = new AddIndexKeyDialog(section, indexTypeUI.getText(), indexKindUI
                      .getText(), alreadyDefined(table.getItems()), makeKey(item));
      FsIndexKeyDescription key = indexSection.addOrEditIndexKey(dialog, null);
      updateKey(item, key);
      section.packTable(table);
    } else if (event.widget == removeButton) {
      TableItem item = table.getItem(table.getSelectionIndex());
      table.setSelection(table.getSelectionIndex() - 1);
      item.dispose();
      section.packTable(table);
    } else if (event.widget == upButton) {
      AbstractSection.swapTableItems(table.getSelection()[0], table.getSelectionIndex() - 1);
    } else if (event.widget == downButton) {
      int i = table.getSelectionIndex();
      TableItem[] items = table.getItems();
      AbstractSection.swapTableItems(items[i + 1], i + 1);
    }

    enableOK();
  }

  public List alreadyDefined(TableItem[] items) {
    List result = new ArrayList();
    if (null == items)
      return result;
    for (int i = 0; i < items.length; i++) {
      result.add(items[i].getText(0));
    }
    return result;
  }

  public void copyValuesFromGUI() {
    indexName = indexNameUI.getText();
    indexKind = indexKindUI.getText();
    indexType = indexTypeUI.getText();
    TableItem[] items = table.getItems();
    if (null != items) {
      keys = new FsIndexKeyDescription[items.length];
      for (int i = 0; i < items.length; i++) {
        keys[i] = makeKey(items[i]);
      }
    } else
      keys = null;
  }

  private FsIndexKeyDescription makeKey(TableItem item) {
    FsIndexKeyDescription key = UIMAFramework.getResourceSpecifierFactory()
                    .createFsIndexKeyDescription();
    boolean typePriority = TYPE_PRIORITY.equals(item.getText(1));
    key.setTypePriority(typePriority);
    if (!typePriority) {
      key.setFeatureName(item.getText(0));
      key.setComparator(STANDARD.equals(item.getText(1)) ? FSIndexComparator.STANDARD_COMPARE
                      : FSIndexComparator.REVERSE_STANDARD_COMPARE);
    }
    return key;
  }

  public boolean isValid() {
    if (indexName.length() == 0 || indexType.length() == 0)
      return false;
    if (!indexName.equals(originalIndexName) && indexSection.isDuplicateIndexLabel(indexName)) {
      errorMessageUI
                      .setText("The name on this index duplicates anexisting name.  Please specify a globally unique name.");
      return false;
    }
    return true;
  }

  public void enableOK() {
    copyValuesFromGUI();
    okButton.setEnabled(indexName.length() > 0);
    boolean keysUsed = "sorted".equals(indexKind) || "set".equals(indexKind);
    addButton.setEnabled(indexName.length() != 0 && indexType.length() != 0 && keysUsed);
    boolean selected = table.getSelectionCount() == 1;
    removeButton.setEnabled(selected);
    editButton.setEnabled(selected);
    upButton.setEnabled(false);
    downButton.setEnabled(false);
    if (selected) {
      upButton.setEnabled(table.getSelectionIndex() != 0);
      downButton.setEnabled(table.getSelectionIndex() != (table.getItemCount() - 1));
    }
  }
}
