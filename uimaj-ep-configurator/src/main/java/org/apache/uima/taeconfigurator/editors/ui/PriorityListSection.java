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

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.AddTypeToPriorityListDialog;
import org.apache.uima.taeconfigurator.wizards.TypePrioritiesNewWizard;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.IManagedForm;

public class PriorityListSection extends AbstractSection {

  public static final String PRIORITY_LIST = "<Priority List>";

  private Tree tree;

  private Button addSetButton;

  private Button addButton;

  private Button removeButton;

  private Button upButton;

  private Button downButton;

  private TypePriorityImportSection typePriorityImportSection;

  private Button exportButton;

  public PriorityListSection(MultiPageEditor editor, Composite parent) {
    super(editor, parent, "Priority Lists", "This section shows the defined Prioirity Lists");
  }

  public void initialize(IManagedForm form) {
    super.initialize(form);

    Composite sectionClient = new2ColumnComposite(getSection());
    enableBorders(sectionClient);

    tree = newTree(sectionClient);

    final Composite buttonContainer = newButtonContainer(sectionClient);
    addSetButton = newPushButton(buttonContainer, "Add Set",
                    "Click here to add another priority list.");
    addButton = newPushButton(buttonContainer, S_ADD, "Click here to add a type");
    removeButton = newPushButton(buttonContainer, S_REMOVE, S_REMOVE_TIP);
    new Button(buttonContainer, SWT.PUSH).setVisible(false); // spacer
    upButton = newPushButton(buttonContainer, S_UP,
                    "Click here to move the selected item up in the priority order.");
    downButton = newPushButton(buttonContainer, S_DOWN,
                    "Click here to move the selected item down in the priority order");
    exportButton = newPushButton(buttonContainer, S_EXPORT, S_EXPORT_TIP);

    toolkit.paintBordersFor(sectionClient);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.forms.IFormPart#refresh()
   */
  public void refresh() {
    if (null == typePriorityImportSection)
      typePriorityImportSection = editor.getIndexesPage().getTypePriorityImportSection();

    super.refresh();
    tree.removeAll();

    TypePriorities typePriorities = getTypePriorities();

    if (typePriorities != null) {
      TypePriorityList[] priorityLists = typePriorities.getPriorityLists();
      for (int i = 0; i < priorityLists.length; i++) {
        TreeItem item = new TreeItem(tree, SWT.NONE);
        item.setText(PRIORITY_LIST);
        String[] types = priorityLists[i].getTypes();
        if (null != types) {
          for (int j = 0; j < types.length; j++) {
            TreeItem tItem = new TreeItem(item, SWT.NONE);
            tItem.setText(formatName(types[j]));
          }
        }
        item.setExpanded(true);
      }
    }
    if (tree.getItemCount() > 0)
      tree.setSelection(new TreeItem[] { tree.getItems()[0] });
    enable();
  }

  public TypePriorityList getTypePriorityListFromTreeItem(TreeItem item) {
    TypePriorityList[] typePriorityLists = getAnalysisEngineMetaData().getTypePriorities()
                    .getPriorityLists();
    return typePriorityLists[tree.indexOf(item)];
  }

  public void handleEvent(Event event) {
    if (event.widget == addSetButton) {
      TypePriorityList typePriorityList = UIMAFramework.getResourceSpecifierFactory()
                      .createTypePriorityList();

      TypePriorities typePriorities = getTypePriorities();
      if (typePriorities == null) {
        typePriorities = UIMAFramework.getResourceSpecifierFactory().createTypePriorities();
        getAnalysisEngineMetaData().setTypePriorities(typePriorities);
      }

      getTypePriorities().addPriorityList(typePriorityList);

      TreeItem item = new TreeItem(tree, SWT.NONE);
      item.setText(PRIORITY_LIST);

      tree.setSelection(new TreeItem[] { item });
      setFileDirty();
    } else if (event.widget == addButton) { // add type to set
      if (editor.isTypePriorityDescriptor() && !editor.getIsContextLoaded()) {
        Utility
                        .popMessage(
                                        "Can''t add types here",
                                        "Types cannot be added here, because there is no loaded context type system to pick the types from",
                                        MessageDialog.WARNING);
        return;
      }
      TreeItem parent = tree.getSelection()[0];
      if (null != parent.getParentItem())
        parent = parent.getParentItem();
      AddTypeToPriorityListDialog dialog = new AddTypeToPriorityListDialog(this,
                      editor.definedTypesWithSupers.get(), // types
                      getTypePriorityListFromTreeItem(parent).getTypes()); // types already in list

      if (dialog.open() == Window.CANCEL)
        return;

      TypePriorityList typePriorityList = getTypePriorityListFromTreeItem(parent);

      String[] newTypeNames = dialog.getSelectedTypeNames();
      for (int i = 0; i < newTypeNames.length; i++) {
        typePriorityList.addType(newTypeNames[i]);
        TreeItem item = new TreeItem(parent, SWT.NONE);
        item.setText(formatName(newTypeNames[i]));
      }

      setFileDirty();
    } else if (event.widget == removeButton) {
      TreeItem item = tree.getSelection()[0];
      TreeItem parent = item.getParentItem();

      if (null == parent) { // removing a priority set
        if (Window.CANCEL == Utility.popOkCancel("ConfirmRemove", "ConfirmRemoveSet",
                        MessageDialog.WARNING))
          return;
        TypePriorityList removedTypePriorityList = getTypePriorityListFromTreeItem(item);
        TypePriorityList[] oldPriorityLists = getAnalysisEngineMetaData().getTypePriorities()
                        .getPriorityLists();
        TypePriorityList[] newPriorityLists = new TypePriorityList[oldPriorityLists.length - 1];

        for (int i = 0, j = 0; i < oldPriorityLists.length; i++) {
          if (oldPriorityLists[i] != removedTypePriorityList) {
            newPriorityLists[j++] = oldPriorityLists[i];
          }
        }

        getAnalysisEngineMetaData().getTypePriorities().setPriorityLists(newPriorityLists);

      } else { // removing a type
        if (Window.CANCEL == Utility.popOkCancel("ConfirmRemove", "ConfirmRemoveType",
                        MessageDialog.WARNING))
          return;
        TypePriorityList typePriorityList = getTypePriorityListFromTreeItem(parent);
        typePriorityList.removeType(item.getText());
      }

      TreeItem previousSelection = getPreviousSelection(parent == null ? tree.getItems() : parent
                      .getItems(), item);
      if (null != previousSelection)
        tree.setSelection(new TreeItem[] { previousSelection });
      item.dispose();
      setFileDirty();
    }
    // only enabled for types
    else if (event.widget == downButton || event.widget == upButton) {
      TreeItem item = tree.getSelection()[0];
      TreeItem parent = item.getParentItem();
      TreeItem[] items = parent.getItems();
      int i = getItemIndex(items, item);

      TypePriorityList typePriorityList = getTypePriorityListFromTreeItem(parent);
      String[] types = typePriorityList.getTypes();
      String temp = types[i];
      if (event.widget == downButton) {
        types[i] = types[i + 1];
        types[i + 1] = temp;
        typePriorityList.setTypes(types);

        new TreeItem(parent, SWT.NONE, i).setText(formatName(types[i]));
        TreeItem t = new TreeItem(parent, SWT.NONE, i + 1);
        t.setText(formatName(types[i + 1]));
        tree.setSelection(new TreeItem[] { t });

        items[i].dispose();
        items[i + 1].dispose();
      } else {
        types[i] = types[i - 1];
        types[i - 1] = temp;
        typePriorityList.setTypes(types);

        TreeItem t = new TreeItem(parent, SWT.NONE, i - 1);
        t.setText(formatName(types[i - 1]));
        tree.setSelection(new TreeItem[] { t });
        new TreeItem(parent, SWT.NONE, i).setText(formatName(types[i]));

        items[i - 1].dispose();
        items[i].dispose();
        setFileDirty();
      }
      TypePriorityList[] tpl = getTypePriorities().getPriorityLists();
      tpl[tree.indexOf(parent)] = typePriorityList;
      getTypePriorities().setPriorityLists(tpl);
    } else if (event.widget == exportButton) {
      typePriorityImportSection.exportImportablePart("<typePriorities>",
                      TypePrioritiesNewWizard.TYPEPRIORITIES_TEMPLATE);
      refresh();
    }
    enable();
  }

  public void enable() {

    if (tree.getSelectionCount() == 1) {
      addButton.setEnabled(true);
      TreeItem item = tree.getSelection()[0];
      removeButton.setEnabled(true);
      if (null != item.getParentItem()) {
        TreeItem[] items = item.getParentItem().getItems();
        int i = getItemIndex(items, item);
        upButton.setEnabled(i > 0);
        downButton.setEnabled(i < (items.length - 1));
      }
    } else {
      addButton.setEnabled(false);
      removeButton.setEnabled(false);
      upButton.setEnabled(false);
      downButton.setEnabled(false);
    }
    exportButton.setEnabled(tree.getItemCount() > 0);
  }
}
