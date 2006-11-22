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

import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.FileLanguageResourceSpecifier;
import org.apache.uima.resource.FileResourceSpecifier;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.impl.ExternalResourceDescription_impl;
import org.apache.uima.resource.impl.FileLanguageResourceSpecifier_impl;
import org.apache.uima.resource.impl.FileResourceSpecifier_impl;
import org.apache.uima.resource.metadata.ExternalResourceBinding;
import org.apache.uima.resource.metadata.impl.ExternalResourceBinding_impl;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.AddExternalResourceDialog;
import org.apache.uima.taeconfigurator.wizards.ResourceManagerConfigurationNewWizard;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.IManagedForm;

/**
 */
public class ExtnlResBindSection extends AbstractSection {
  private final static String boundHeader = "Bound to: ";

  private Composite sectionClient;

  private Tree tree;

  private Button addButton;

  private Button editButton;

  private Button removeButton;

  private Button bindButton;

  private Button exportButton;

  private ResourceDependencySection resourceDependencySection;

  private ImportResBindSection resBindImportSection;

  public ExtnlResBindSection(MultiPageEditor aEditor, Composite parent) {
    super(
                    aEditor,
                    parent,
                    "Resources Needs, Definitions and Bindings",
                    "Specify External Resources; Bind them to dependencies on the right panel by selecting the corresponding dependency and clicking Bind.");
  }

  public void initialize(IManagedForm form) {
    super.initialize(form);
    // set up Composite to hold widgets in the section
    sectionClient = new2ColumnComposite(getSection());

    tree = newTree(sectionClient);
    // Buttons
    Composite buttonContainer = newButtonContainer(sectionClient);

    addButton = newPushButton(buttonContainer, S_ADD,
                    "Click to add a new External Resource definition");
    editButton = newPushButton(buttonContainer, S_EDIT,
                    "Click to edit an External Resource definition");
    removeButton = newPushButton(buttonContainer, "Remove",
                    "Click to remove selected binding or External Resource");
    bindButton = newPushButton(buttonContainer, "Bind",
                    "Click to bind selected dependency with selected Resource");
    exportButton = newPushButton(buttonContainer, S_EXPORT, S_EXPORT_TIP);

    buttonContainer.pack();
    getSection().getParent().getParent().pack();
    getSection().getParent().getParent().layout();
    initialFormWidth = getSection().getSize().x;
    ((GridData) tree.getLayoutData()).widthHint = initialFormWidth - buttonContainer.getSize().x;

    enableBorders(sectionClient);
    toolkit.paintBordersFor(sectionClient);

    tree.addListener(SWT.MouseDoubleClick, this);
  }

  public void refresh() {
    if (null == resBindImportSection)
      resBindImportSection = editor.getResourcesPage().getResBindImportSection();
    super.refresh();
    resourceDependencySection = editor.getResourcesPage().getResourceDependencySection();
    ExternalResourceDescription[] xrds = getExternalResources();
    ExternalResourceBinding[] bindings = getExternalResourceBindings();
    tree.removeAll();
    if (null != xrds) {
      for (int i = 0; i < xrds.length; i++) {
        addExternalResourceDescriptionToGUI(xrds[i], bindings);
      }
    }
    enable();
  }

  private void addExternalResourceDescriptionToGUI(ExternalResourceDescription xrd,
                  ExternalResourceBinding[] bindings) {
    TreeItem item = new TreeItem(tree, SWT.NONE);
    fillXrdItem(item, xrd);
    fillBindings(item, xrd, bindings);
    item.setExpanded(true);
  }

  private void fillXrdItem(TreeItem item, ExternalResourceDescription xrd) {
    StringBuffer text = new StringBuffer();
    text.append(xrd.getName());
    ResourceSpecifier rs = xrd.getResourceSpecifier();
    if (rs instanceof FileLanguageResourceSpecifier) {
      FileLanguageResourceSpecifier flrs = (FileLanguageResourceSpecifier) rs;
      text.append("  URL_Prefix: ").append(flrs.getFileUrlPrefix()).append("  URL_Suffix: ")
                      .append(flrs.getFileUrlSuffix());
    } else if (rs instanceof FileResourceSpecifier) {
      FileResourceSpecifier frs = (FileResourceSpecifier) rs;
      text.append("  URL: ").append(frs.getFileUrl());
    } else {
      text.append("  Custom Resource Specifier");
    }
    String implName = xrd.getImplementationName();
    if (null != implName && !implName.equals("")) {
      text.append("  Implementation: ").append(implName);
    }
    item.setText(text.toString());
    item.setData(xrd);
  }

  private void fillBindings(TreeItem parent, ExternalResourceDescription xrd,
                  ExternalResourceBinding[] bindings) {
    if (null != bindings) {
      for (int i = 0; i < bindings.length; i++) {
        if (bindings[i].getResourceName().equals(xrd.getName())) {
          addBindingToGUI(parent, bindings[i].getKey(), bindings[i]);
        }
      }
    }
  }

  private void addBindingToGUI(TreeItem parent, String key, ExternalResourceBinding xrb) {
    TreeItem item = new TreeItem(parent, SWT.NONE);
    item.setText(boundHeader + key);
    item.setData(xrb);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
   */
  public void handleEvent(Event event) {
    if (event.widget == addButton)
      handleAdd();
    else if (event.widget == editButton || event.type == SWT.MouseDoubleClick)
      handleEdit();
    else if (event.widget == removeButton
                    || (event.widget == tree && event.type == SWT.KeyUp && event.character == SWT.DEL))
      handleRemove();
    else if (event.widget == bindButton)
      handleBind();
    else if (event.widget == exportButton) {
      try {
        resBindImportSection.exportImportablePart("<resourceManagerConfiguration>",
                        ResourceManagerConfigurationNewWizard.RESOURCEMANGERCONFIGURATION_TEMPLATE);
      } finally {
        refresh(); // update in case of throw, even
      }
    }

    enable();
  }

  public ExternalResourceDescription getXRDescriptionFromTreeItem(TreeItem item) {
    return (ExternalResourceDescription) item.getData();
  }

  /**
   * Bindings can't be edited. They can be removed or new ones bound. External resources can be
   * edited. Edit button is disabled unles an editable thing is selected. But user could double
   * click an illegal entry
   * 
   */
  private void handleEdit() {
    TreeItem item = tree.getSelection()[0];
    if (isBoundSpec(item))
      return;
    ExternalResourceDescription xrd = getXRDescriptionFromTreeItem(item);
    ResourceSpecifier rs = xrd.getResourceSpecifier();
    if (!((rs instanceof FileResourceSpecifier) || (rs instanceof FileLanguageResourceSpecifier))) {
      Utility.popMessage("Can''t edit custom resource", "This resource is a '"
                      + rs.getClass().getName()
                      + "', and any edits have to be done directly in the XML in the Source view.",
                      MessageDialog.INFORMATION);
      return;
    }

    AddExternalResourceDialog dialog = new AddExternalResourceDialog(this, xrd);

    if (dialog.open() == Window.CANCEL)
      return;
    alterExistingXRD(dialog, xrd, item);
  }

  /**
   * Add new external resource, with no bindings
   * 
   */
  private void handleAdd() {
    AddExternalResourceDialog dialog = new AddExternalResourceDialog(this);

    if (dialog.open() == Window.CANCEL)
      return;
    ExternalResourceDescription xrd = new ExternalResourceDescription_impl();
    TreeItem item = new TreeItem(tree, SWT.NONE);
    alterExistingXRD(dialog, xrd, item);
    getResourceManagerConfiguration().addExternalResource(xrd);
  }

  private void alterExistingXRD(AddExternalResourceDialog dialog, ExternalResourceDescription xrd,
                  TreeItem item) {
    valueChanged = false;
    xrd.setName(setValueChanged(dialog.xrName, xrd.getName()));
    xrd.setDescription(setValueChanged(dialog.xrDescription, xrd.getDescription()));
    xrd
                    .setImplementationName(setValueChanged(dialog.xrImplementation, xrd
                                    .getImplementationName()));

    ResourceSpecifier rs = xrd.getResourceSpecifier();
    if (null == dialog.xrUrlSuffix || "".equals(dialog.xrUrlSuffix)) {
      FileResourceSpecifier frs;
      if (null != rs && rs instanceof FileResourceSpecifier) {
        frs = (FileResourceSpecifier) rs;
      } else {
        frs = new FileResourceSpecifier_impl();
      }
      frs.setFileUrl(setValueChanged(dialog.xrUrl, frs.getFileUrl()));
      xrd.setResourceSpecifier(frs);
    } else {
      FileLanguageResourceSpecifier flrs;
      if (null != rs && rs instanceof FileLanguageResourceSpecifier) {
        flrs = (FileLanguageResourceSpecifier) rs;
      } else {
        flrs = new FileLanguageResourceSpecifier_impl();
      }
      flrs.setFileUrlPrefix(setValueChanged(dialog.xrUrl, flrs.getFileUrlPrefix()));
      flrs.setFileUrlSuffix(setValueChanged(dialog.xrUrlSuffix, flrs.getFileUrlSuffix()));
      xrd.setResourceSpecifier(flrs);
    }
    fillXrdItem(item, xrd);
    if (valueChanged)
      setFileDirty();
  }

  /**
   * Bind button - enabled only when one dependency is selected, and one External Resource, not
   * already bound to this key, is selected
   */
  private void handleBind() {
    TreeItem xrItem = tree.getSelection()[0];
    if (null != xrItem.getParentItem())
      xrItem = xrItem.getParentItem();
    ExternalResourceDescription xrd = getXRDescriptionFromTreeItem(xrItem);

    TableItem keyItem = resourceDependencySection.getTable().getSelection()[0];

    ExternalResourceBinding xrb = new ExternalResourceBinding_impl();
    String key = keyItem.getText(ResourceDependencySection.KEY_COL);
    xrb.setKey(key);
    xrb.setResourceName(xrd.getName());
    getResourceManagerConfiguration().addExternalResourceBinding(xrb);
    addBindingToGUI(xrItem, key, xrb);
    xrItem.setExpanded(true);
    keyItem.setText(0, "Bound");
    keyItem.getParent().getColumn(0).pack();
    setFileDirty();
  }

  /**
   * remove either a binding or an external resource. Removing the resource removes all bindings
   * associated with it
   * 
   */
  private void handleRemove() {
    int selectionCount = tree.getSelectionCount();
    if (1 != selectionCount)
      return;
    TreeItem item = tree.getSelection()[0];
    if (null == item.getParentItem()) { // case of removing a resource
      if (Window.CANCEL == Utility.popOkCancel("Removing Resource",
                      "Removing an External Resource and all its bindings. Resource name:"
                                      + item.getText(), MessageDialog.WARNING))
        return;
      removeAllBindings(item);
      removeResource(item);
    } else { // case of removing a binding
      removeBinding(item);
    }
  }

  public ExternalResourceBinding getXRBindingFromTreeItem(TreeItem item) {
    return (ExternalResourceBinding) item.getData();
  }

  private void removeBinding(TreeItem item) {
    ExternalResourceBinding xrb = getXRBindingFromTreeItem(item);
    getResourceManagerConfiguration().removeExternalResourceBinding(xrb);
    removeBoundFlagInDependencySection(xrb);
    item.dispose();
    setFileDirty();
  }

  private void removeBoundFlagInDependencySection(ExternalResourceBinding xrb) {
    String key = xrb.getKey();
    TableItem[] items = resourceDependencySection.getTable().getItems();
    for (int i = 0; i < items.length; i++) {
      if (key.equals(items[i].getText(ResourceDependencySection.KEY_COL)))
        items[i].setText(0, ""); // reset bound
    }
  }

  private void removeAllBindings(TreeItem item) {
    TreeItem[] items = item.getItems();
    for (int i = items.length - 1; i >= 0; i--) {
      removeBinding(items[i]);
    }
  }

  private void removeResource(TreeItem item) {
    ExternalResourceDescription xrd = getXRDescriptionFromTreeItem(item);
    getResourceManagerConfiguration().removeExternalResource(xrd);
    item.dispose();
    setFileDirty();
  }

  public void enable() {
    // bind enabled when one item in tree and one in table is selected
    bindButton
                    .setEnabled(tree.getSelectionCount() == 1
                                    && resourceDependencySection.getTable().getSelectionCount() == 1
                                    && "".equals(resourceDependencySection.getTable()
                                                    .getSelection()[0].getText(0))); // not bound

    removeButton.setEnabled(tree.getSelectionCount() > 0);
    editButton.setEnabled(tree.getSelectionCount() == 1 && !isBoundSpec(tree.getSelection()[0]));
    exportButton.setEnabled(tree.getItemCount() > 0);
  }

  private boolean isBoundSpec(TreeItem item) {
    return item.getText().startsWith(boundHeader);
  }

  public boolean resourceNameAlreadyDefined(String name) {
    ExternalResourceDescription[] xrds = getExternalResources();
    if (xrds != null) {
      for (int i = 0; i < xrds.length; i++) {
        if (xrds[i].getName().equals(name)) {
          Utility.popMessage("Name Already Defined",
                          "The External Resource Name specified is already defined",
                          MessageDialog.ERROR);
          return true;
        }
      }
    }
    return false;
  }

}
