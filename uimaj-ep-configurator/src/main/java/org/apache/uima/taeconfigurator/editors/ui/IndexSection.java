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

package org.apache.uima.taeconfigurator.editors.ui;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.AddIndexDialog;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.AddIndexKeyDialog;
import org.apache.uima.taeconfigurator.wizards.FsIndexCollectionNewWizard;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableTree;
import org.eclipse.swt.custom.TableTreeItem;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.IManagedForm;

public class IndexSection extends AbstractSection {

  public static final String ANNOTATION_INDEX_BUILT_IN = "Annotation Index (Built-in)";

  public final static int INDEX_NAME_COL = 0;

  public final static int INDEX_TYPE_COL = 1;

  public final static int ASC_DES_COL = 1;

  public final static int INDEX_KIND_COL = 2;

  public TableTree tt; // accessed by inner class

  private Button addIndexButton;

  private Button addKeyButton;

  private Button editButton;

  private Button removeButton;

  private Button upButton;

  private Button downButton;

  FsIndexDescription m_builtInIndexDescription = null;

  private Button exportButton;

  private IndexImportSection indexImportSection;

  public IndexSection(MultiPageEditor editor, Composite parent) {
    super(editor, parent, "Indexes",
                    "The following indexes are defined on the type system for this engine.");
  }

  public void initialize(IManagedForm form) {
    super.initialize(form);

    // set up Composite to hold widgets in the section
    Composite sectionClient = new2ColumnComposite(getSection());
    enableBorders(sectionClient);

    tt = newTableTree(sectionClient, SWT.SINGLE | SWT.FULL_SELECTION);

    final Table table = tt.getTable();
    table.setHeaderVisible(true);
    newTableColumn(table).setText("Name");
    newTableColumn(table).setText("Type");
    newTableColumn(table).setText("Kind");

    final Composite buttonContainer = newButtonContainer(sectionClient);
    addIndexButton = newPushButton(buttonContainer, "Add Index", "Click here to add a new index.");
    addKeyButton = newPushButton(buttonContainer, "Add Key",
                    "Click here to add a new key for the selected index.");
    editButton = newPushButton(buttonContainer, S_EDIT, S_EDIT_TIP);
    removeButton = newPushButton(buttonContainer, S_REMOVE, S_REMOVE_TIP);
    upButton = newPushButton(buttonContainer, S_UP, S_UP_TIP);
    downButton = newPushButton(buttonContainer, S_DOWN, S_DOWN_TIP);
    exportButton = newPushButton(buttonContainer, S_EXPORT, S_EXPORT_TIP);

    // in addition to normal keyup and mouse up:
    table.addListener(SWT.MouseHover, this);

    toolkit.paintBordersFor(sectionClient);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.forms.IFormPart#refresh()
   */
  public void refresh() {
    if (null == indexImportSection)
      indexImportSection = editor.getIndexesPage().getIndexImportSection();
    super.refresh();
    tt.removeAll();

    // add built-in annotation index
    updateIndexSpec(new TableTreeItem(tt, SWT.NONE), getBuiltInIndexDescription());

    FsIndexDescription[] fsIndexes = getAnalysisEngineMetaData().getFsIndexes();

    if (fsIndexes != null) {
      for (int i = 0; i < fsIndexes.length; i++) {
        updateIndexSpec(new TableTreeItem(tt, SWT.NONE), fsIndexes[i]);
      }
    }
    packTable(tt.getTable());
    enable();
  }

  private void updateIndexSpec(TableTreeItem item, FsIndexDescription ndx) {
    item.setText(INDEX_NAME_COL, ndx.getLabel());
    item.setText(INDEX_TYPE_COL, formatName(ndx.getTypeName()));
    item.setText(INDEX_KIND_COL, handleDefaultIndexKind(ndx.getKind()));
    item.setData(ndx);
    removeChildren(item);
    FsIndexKeyDescription[] keys = ndx.getKeys();
    if (null != keys)
      for (int i = 0; i < keys.length; i++) {
        updateKeySpec(new TableTreeItem(item, SWT.NONE), keys[i]);
      }
  }

  private void updateKeySpec(TableTreeItem item, FsIndexKeyDescription key) {
    String name = key.getFeatureName();
    item.setText(INDEX_NAME_COL, null == name ? "TYPE PRIORITY" : name);
    item.setText(ASC_DES_COL,
                    key.getComparator() == FSIndexComparator.STANDARD_COMPARE ? "Standard"
                                    : "Reverse");
    item.setData(key);
  }

  public FsIndexKeyDescription createFsIndexKeyDescription() {
    return UIMAFramework.getResourceSpecifierFactory().createFsIndexKeyDescription();
  }

  public FsIndexDescription getBuiltInIndexDescription() {
    if (m_builtInIndexDescription == null) {
      m_builtInIndexDescription = UIMAFramework.getResourceSpecifierFactory()
                      .createFsIndexDescription();

      m_builtInIndexDescription.setLabel(ANNOTATION_INDEX_BUILT_IN);
      m_builtInIndexDescription.setTypeName(CAS.TYPE_NAME_ANNOTATION);
      m_builtInIndexDescription.setKind("sorted");

      FsIndexKeyDescription[] keys = new FsIndexKeyDescription[] { createFsIndexKeyDescription(),
          createFsIndexKeyDescription(), createFsIndexKeyDescription() };

      keys[0].setFeatureName("begin");
      keys[0].setComparator(FSIndexComparator.STANDARD_COMPARE);
      keys[1].setFeatureName("end");
      keys[1].setComparator(FSIndexComparator.REVERSE_STANDARD_COMPARE);
      keys[2].setTypePriority(true);

      m_builtInIndexDescription.setKeys(keys);
    }

    return m_builtInIndexDescription;
  }

  private boolean notAllowed(String message) {
    if (isIndexDescriptor() && !editor.getIsContextLoaded()) {
      Utility
                      .popMessage(
                                      "Not Allowed",
                                      "Editing or Adding Indexes can't be done here because the information about the type system is missing.",
                                      MessageDialog.INFORMATION);
      return true;
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
   */
  public void handleEvent(Event event) {

    if (event.widget == addIndexButton) {
      if (notAllowed("Adding an Index"))
        return;
      // creating the dialog and open it
      AddIndexDialog dialog = new AddIndexDialog(this);
      if (dialog.open() == Window.CANCEL)
        return;

      FsIndexDescription id = UIMAFramework.getResourceSpecifierFactory()
                      .createFsIndexDescription();

      id.setLabel(dialog.indexName);
      id.setTypeName(dialog.indexType);
      id.setKeys(dialog.keys);
      id.setKind(dialog.indexKind);

      addFsIndexDescription(id);

      updateIndexSpec(new TableTreeItem(tt, SWT.NONE), id);

      tt.getTable().setSelection(tt.getTable().getItemCount() - 1);
      packTable(tt.getTable());
      setFileDirty();
    } else if (event.widget == addKeyButton) {
      if (notAllowed("Adding an Index"))
        return;
      TableTreeItem parent = tt.getSelection()[0];
      if (null != parent.getParentItem())
        parent = parent.getParentItem();
      if (foolingAroundWithAnnotationIndex(parent))
        return;
      FsIndexDescription fsid = getFsIndexDescriptionFromTableTreeItem(parent);
      AddIndexKeyDialog dialog = new AddIndexKeyDialog(this, fsid.getTypeName(),
                      handleDefaultIndexKind(fsid.getKind()), getAlreadyUsedFeatures(fsid));
      FsIndexKeyDescription newKey = addOrEditIndexKey(dialog, null);
      if (null != newKey) {
        addFsIndexKeyDescription(fsid, newKey);
        updateKeySpec(new TableTreeItem(parent, SWT.NONE), newKey);
        parent.setExpanded(true);
        setFileDirty();
      }
    } else if (event.widget == removeButton) {
      TableTreeItem item = tt.getSelection()[0];
      if (foolingAroundWithAnnotationIndex(item))
        return;
      Object o = item.getData();
      if (o instanceof FsIndexDescription) {
        if (Window.CANCEL == Utility.popOkCancel("Confirm Remove",
                        "Do you want to remove this index?", MessageDialog.WARNING))
          return;
        removeFsIndexDescription((FsIndexDescription) o);
      } else {
        if (Window.CANCEL == Utility.popOkCancel("Confirm Remove",
                        "Do you want to remove this key?", MessageDialog.WARNING))
          return;
        TableTreeItem parent = item.getParentItem();
        FsIndexDescription fsid = getFsIndexDescriptionFromTableTreeItem(parent);
        removeFsIndexKeyDescription(fsid, (FsIndexKeyDescription) o);
      }
      tt.getTable().setSelection(tt.getTable().getSelectionIndex() - 1);
      item.dispose();
      setFileDirty();
    } else if (event.widget == editButton || event.type == SWT.MouseDoubleClick) {
      if (notAllowed("Adding an Index"))
        return;
      if (tt.getSelectionCount() != 1)
        return;
      TableTreeItem item = tt.getSelection()[0];
      if (foolingAroundWithAnnotationIndex(item))
        return;
      Object o = item.getData();
      if (o instanceof FsIndexDescription) {
        FsIndexDescription fsid = (FsIndexDescription) o;
        AddIndexDialog dialog = new AddIndexDialog(this, fsid);
        if (dialog.open() == Window.CANCEL)
          return;

        valueChanged = false;
        fsid.setLabel(setValueChanged(dialog.indexName, fsid.getLabel()));
        fsid.setTypeName(setValueChanged(dialog.indexType, fsid.getTypeName()));
        fsid.setKeys(setValueChangedKeys(dialog.keys, fsid.getKeys()));
        fsid.setKind(setValueChanged(dialog.indexKind, handleDefaultIndexKind(fsid.getKind())));

        updateIndexSpec(item, fsid);

        if (valueChanged) {
          packTable(tt.getTable());
          setFileDirty();
        }
      } else { // editing a key
        if (notAllowed("Adding an Index"))
          return;
        FsIndexKeyDescription key = (FsIndexKeyDescription) o;
        TableTreeItem parent = item.getParentItem();
        FsIndexDescription fsid = getFsIndexDescriptionFromTableTreeItem(parent);
        AddIndexKeyDialog dialog = new AddIndexKeyDialog(this, fsid.getTypeName(),
                        handleDefaultIndexKind(fsid.getKind()), getAlreadyUsedFeatures(fsid), key);
        valueChanged = false;
        addOrEditIndexKey(dialog, key);
        if (valueChanged) {
          updateKeySpec(item, key);
          packTable(tt.getTable());
          setFileDirty();
        }
      }
    } else if (event.widget == upButton) {
      int i = getIndex(tt.getSelection()[0]);
      swapIndexKeys(tt.getSelection()[0], i - 1);
      setFileDirty();
    } else if (event.widget == downButton) {
      int i = getIndex(tt.getSelection()[0]);

      TableTreeItem[] items = tt.getSelection()[0].getParentItem().getItems();
      swapIndexKeys(items[i + 1], i + 1);
    } else if (event.widget == exportButton) {
      try {
        indexImportSection.exportImportablePart("<fsIndexCollection>",
                        FsIndexCollectionNewWizard.FSINDEXCOLLECTION_TEMPLATE);
      } finally {
        refresh(); // update in case of throw, even
      }
    }
    enable();
  }

  private boolean foolingAroundWithAnnotationIndex(TableTreeItem item) {
    while (null != item.getParentItem())
      item = item.getParentItem();

    if (ANNOTATION_INDEX_BUILT_IN.equals(item.getText(0))) {
      Utility.popMessage("Not Allowed",
                      "You cannot edit or delete the built-in Annotation Index or its keys",
                      MessageDialog.ERROR);
      return true;
    }
    return false;
  }

  public void addFsIndexKeyDescription(FsIndexDescription fsid, FsIndexKeyDescription key) {
    FsIndexKeyDescription[] prevKeys = fsid.getKeys();
    FsIndexKeyDescription[] newKeys = new FsIndexKeyDescription[prevKeys == null ? 1
                    : prevKeys.length + 1];
    if (null != prevKeys)
      System.arraycopy(prevKeys, 0, newKeys, 0, prevKeys.length);
    newKeys[newKeys.length - 1] = key;
    fsid.setKeys(newKeys);
  }

  public void addFsIndexDescription(FsIndexDescription fsid) {
    FsIndexDescription[] oldFsIndexes = getAnalysisEngineMetaData().getFsIndexes();
    FsIndexDescription[] newFsIndexes = new FsIndexDescription[oldFsIndexes == null ? 1
                    : oldFsIndexes.length + 1];
    if (null != oldFsIndexes)
      System.arraycopy(oldFsIndexes, 0, newFsIndexes, 0, oldFsIndexes.length);
    newFsIndexes[newFsIndexes.length - 1] = fsid;
    getAnalysisEngineMetaData().setFsIndexes(newFsIndexes);
  }

  public void removeFsIndexDescription(FsIndexDescription fsid) {
    getAnalysisEngineMetaData().setFsIndexes(
                    (FsIndexDescription[]) Utility.removeElementFromArray(
                                    getAnalysisEngineMetaData().getFsIndexes(), fsid,
                                    FsIndexDescription.class));
  }

  public void removeFsIndexKeyDescription(FsIndexDescription fsid, FsIndexKeyDescription key) {
    fsid.setKeys((FsIndexKeyDescription[]) Utility.removeElementFromArray(fsid.getKeys(), key,
                    FsIndexKeyDescription.class));
  }

  public List getAlreadyUsedFeatures(FsIndexDescription ndx) {
    List result = new ArrayList();
    FsIndexKeyDescription[] items = ndx.getKeys();
    if (null == items)
      return result;
    for (int i = 0; i < items.length; i++) {
      result.add(items[i].getFeatureName());
    }
    return result;
  }

  public FsIndexKeyDescription addOrEditIndexKey(AddIndexKeyDialog dialog, FsIndexKeyDescription key) {
    if (dialog.open() == Window.CANCEL) {
      return null;
    }
    if (null == key)
      key = UIMAFramework.getResourceSpecifierFactory().createFsIndexKeyDescription();
    if (dialog.typePriority) {
      key.setTypePriority(setValueChangedBoolean(true, key.isTypePriority()));
      key.setFeatureName(null);
    } else {
      key.setFeatureName(setValueChanged(dialog.featureName, key.getFeatureName()));
      key.setComparator(setValueChangedInt(dialog.direction, key.getComparator()));
      key.setTypePriority(false);
    }
    return key;
  }

  /**
   * This has to check the resolvedImports, mergedWithDelegates version of the fsindexes
   * 
   * @param indexLabel
   * @return
   */
  public boolean isDuplicateIndexLabel(String indexLabel) {
    FsIndexDescription[] indexes = getAnalysisEngineMetaData().getFsIndexes();
    if (indexes == null) {
      return false;
    }
    for (int i = 0; i < indexes.length; i++) {
      if (indexes[i].getLabel().equals(indexLabel)) {
        return true;
      }
    }
    return false;
  }

  public void enable() {
    boolean selected = tt.getSelectionCount() == 1;
    TableTreeItem item = null;
    TableTreeItem parent = null;
    if (selected) {
      item = tt.getSelection()[0];
      parent = item.getParentItem();
    }
    boolean notBuiltInSelected = (selected && item != tt.getItems()[0]);
    notBuiltInSelected &= !(null != parent && parent == tt.getItems()[0]);

    addIndexButton.setEnabled(true);
    addKeyButton.setEnabled(notBuiltInSelected
                    && (null != parent || /* null == parent && */!"bag".equals(item
                                    .getText(INDEX_KIND_COL))));
    editButton.setEnabled(notBuiltInSelected);
    removeButton.setEnabled(notBuiltInSelected);
    exportButton.setEnabled(tt.getItemCount() > 1); // always one "built-in"

    upButton.setEnabled(false);
    downButton.setEnabled(false);
    if (selected) {
      if (null != parent && notBuiltInSelected) {
        TableTreeItem firstItem = parent.getItems()[0];
        TableTreeItem lastItem = parent.getItems()[parent.getItems().length - 1];
        upButton.setEnabled(item != firstItem);
        downButton.setEnabled(item != lastItem);
      }
    }

  }
}
