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

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.metadata.FlowControllerDeclaration;
import org.apache.uima.resource.ExternalResourceDependency;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.impl.ExternalResourceDependency_impl;
import org.apache.uima.resource.metadata.ExternalResourceBinding;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.AddExternalResourceDependencyDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.IManagedForm;

/**
 * Declaration of primitive external resource dependencies A 4 col table: bound/unbound, keys, opt
 * flag, and interface name.
 */

public class ResourceDependencySection extends AbstractSection {

  /** The Constant KEY_COL. */
  public final static int KEY_COL = 2;

  /** The Constant OPT_COL. */
  public final static int OPT_COL = 1;

  /** The Constant BOUND. */
  private final static String BOUND = "Bound";

  /** The table. */
  public Table table; // accessed by inner class

  /** The add button. */
  private Button addButton;

  /** The edit button. */
  private Button editButton;

  /** The remove button. */
  private Button removeButton;

  /**
   * Instantiates a new resource dependency section.
   *
   * @param editor
   *          the editor
   * @param parent
   *          the parent
   */
  public ResourceDependencySection(MultiPageEditor editor, Composite parent) {
    super(editor, parent, "Resource Dependencies",
            "Primitives declare what resources they need. A primitive can only bind to one external resource.");
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.taeconfigurator.editors.ui.AbstractSection#initialize(org.eclipse.ui.forms.
   * IManagedForm)
   */
  @Override
  public void initialize(IManagedForm form) {
    super.initialize(form);

    // set up Composite to hold widgets in the section
    Composite sectionClient = new2ColumnComposite(getSection());
    enableBorders(sectionClient);

    table = newTable(sectionClient, SWT.SINGLE | SWT.FULL_SELECTION, 50, 0);
    table.setHeaderVisible(true);
    newTableColumn(table).setText(BOUND);
    newTableColumn(table).setText("Optional?");
    newTableColumn(table).setText("Keys");
    newTableColumn(table).setText("Interface Name");

    if (isPrimitive()) {
      final Composite buttonContainer = newButtonContainer(sectionClient);
      addButton = newPushButton(buttonContainer, S_ADD, "Click here to add a dependency.");
      editButton = newPushButton(buttonContainer, S_EDIT, S_EDIT_TIP);
      removeButton = newPushButton(buttonContainer, S_REMOVE, S_REMOVE_TIP);
    }

    // in addition to normal keyup and mouse up:
    table.addListener(SWT.MouseHover, this);
    if (isPrimitive()) // only primitives can edit
      table.addListener(SWT.MouseDoubleClick, this);

    toolkit.paintBordersFor(sectionClient);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
   */
  @Override
  public void refresh() {
    super.refresh();
    table.getParent().setRedraw(false);
    table.removeAll();
    if (isPrimitive())
      addPrimitiveToGUI("", editor.getAeDescription());
    else { // is aggregate
      for (Iterator it = editor.getResolvedDelegates().entrySet().iterator(); it.hasNext();) {
        Map.Entry item = (Map.Entry) it.next();
        addDelegateToGUI("", (String) item.getKey(), (ResourceSpecifier) item.getValue());
      }
      FlowControllerDeclaration fcd = editor.getResolvedFlowControllerDeclaration();
      if (null != fcd) {
        addDelegateToGUI("", fcd.getKey(), fcd.getSpecifier());
      }
    }
    enable();
    table.getParent().setRedraw(true);
  }

  /**
   * Adds the delegate to GUI.
   *
   * @param keys
   *          the keys
   * @param newKey
   *          the new key
   * @param o
   *          the o
   */
  private void addDelegateToGUI(String keys, String newKey, ResourceSpecifier o) {
    if (o instanceof AnalysisEngineDescription) {
      AnalysisEngineDescription aeDescription = (AnalysisEngineDescription) o;
      if (aeDescription.isPrimitive())
        addPrimitiveToGUI(keys + newKey + "/", aeDescription);
      else {
        for (Iterator it = editor.getDelegateAEdescriptions(aeDescription).entrySet().iterator(); it
                .hasNext();) {
          Map.Entry item = (Map.Entry) it.next();
          addDelegateToGUI(keys + newKey + "/", (String) item.getKey(),
                  (ResourceSpecifier) item.getValue());
        }
        FlowControllerDeclaration fcd = getFlowControllerDeclaration();
        if (null != fcd) {
          addPrimitiveToGUI(keys + fcd.getKey() + "/", ((ResourceCreationSpecifier) editor
                  .getResolvedFlowControllerDeclaration().getSpecifier()));
        }
      }
    }
  }

  /**
   * Adds the primitive to GUI.
   *
   * @param keys
   *          the keys
   * @param aeDescription
   *          the ae description
   */
  private void addPrimitiveToGUI(String keys, ResourceCreationSpecifier aeDescription) {
    ExternalResourceDependency[] xrd = aeDescription.getExternalResourceDependencies();
    if (null != xrd) {
      for (int i = 0; i < xrd.length; i++) {
        addXrdToGUI(keys, xrd[i]);
      }
    }
  }

  /**
   * Update xrd to GUI.
   *
   * @param item
   *          the item
   * @param xrd
   *          the xrd
   * @param keys
   *          the keys
   */
  private void updateXrdToGUI(TableItem item, ExternalResourceDependency xrd, String keys) {
    String key = keys + xrd.getKey();
    item.setText(0, isBound(key) ? BOUND : "");
    item.setText(KEY_COL, key);
    item.setText(OPT_COL, (xrd.isOptional()) ? "optional" : "required");
    item.setText(3, convertNull(xrd.getInterfaceName()));
    item.setData(xrd);
  }

  /**
   * Checks if is bound.
   *
   * @param key
   *          the key
   * @return true, if is bound
   */
  private boolean isBound(String key) {
    ResourceManagerConfiguration rmc = editor.getResolvedExternalResourcesAndBindings();
    if (null == rmc) { // happens if there is no such xml element in the descriptor
      return false;
    }
    ExternalResourceBinding[] xrb = rmc.getExternalResourceBindings();
    if (null != xrb)
      for (int i = 0; i < xrb.length; i++) {
        if (key.equals(xrb[i].getKey()))
          return true;
      }
    return false;
  }

  /**
   * Propagate key change.
   *
   * @param newKey
   *          the new key
   * @param oldKey
   *          the old key
   */
  private void propagateKeyChange(String newKey, String oldKey) {
    ExternalResourceBinding[] xrb = getExternalResourceBindings();
    if (null != xrb)
      for (int i = 0; i < xrb.length; i++) {
        if (oldKey.equals(xrb[i].getKey())) {
          xrb[i].setKey(newKey);
          editor.getResourcesPage().getResourceBindingsSection().markStale();
          return; // only 1 binding at most
        }
      }
  }

  /**
   * Adds the xrd to GUI.
   *
   * @param keys
   *          either "" or key/key/
   * @param xrd
   *          the xrd
   */
  private void addXrdToGUI(String keys, ExternalResourceDependency xrd) {
    TableItem item = new TableItem(table, SWT.NONE);
    updateXrdToGUI(item, xrd, keys);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
   */
  @Override
  public void handleEvent(Event event) {
    if (event.widget == addButton) {
      handleAdd();
    } else if (event.widget == removeButton
            || (event.type == SWT.KeyUp && event.character == SWT.DEL)) {
      handleRemove();
    } else if (event.widget == editButton || event.type == SWT.MouseDoubleClick) {
      handleEdit();
    }
    // else if (event.type == SWT.MouseDown && event.button == 3) {
    // handleTableContextMenuRequest(event);
    // }
    else if (event.type == SWT.MouseHover) {
      handleTableHoverHelp(event);
    } else if (event.type == SWT.Selection) {
      editor.getResourcesPage().getResourceBindingsSection().enable();
    }
    enable();
  }

  /**
   * Gets the XR dependency from table item.
   *
   * @param item
   *          the item
   * @return the XR dependency from table item
   */
  public ExternalResourceDependency getXRDependencyFromTableItem(TableItem item) {
    return (ExternalResourceDependency) item.getData();
  }

  // *****************************************************
  // * When hovering over an item in the table, show the
  // * description
  /**
   * Handle table hover help.
   *
   * @param event
   *          the event
   */
  // *****************************************************
  private void handleTableHoverHelp(Event event) {
    TableItem item = table.getItem(new Point(event.x, event.y));
    if (null != item) {
      ExternalResourceDependency xrd = getXRDependencyFromTableItem(item);
      setToolTipText(table, xrd.getDescription());
    } else {
      table.setToolTipText(""); // not needed? - tool tip goes away by itself
    }
  }

  /**
   * Handle edit.
   */
  private void handleEdit() {
    TableItem item = table.getSelection()[0];
    ExternalResourceDependency xrd = getXRDependencyFromTableItem(item);
    AddExternalResourceDependencyDialog dialog = new AddExternalResourceDependencyDialog(this, xrd);
    if (dialog.open() == Window.CANCEL)
      return;

    alterExistingExternalResourceDependency(xrd, dialog);
    updateXrdToGUI(item, xrd, "");
  }

  /**
   * Finish action.
   */
  private void finishAction() {
    packChangingColumns();
    setFileDirty();
  }

  /**
   * Pack changing columns.
   */
  private void packChangingColumns() {
    table.getColumn(KEY_COL).pack();
    table.getColumn(3).pack();
  }

  /**
   * Handle remove.
   */
  private void handleRemove() {
    TableItem item = table.getSelection()[0];
    editor.getAeDescription()
            .setExternalResourceDependencies((ExternalResourceDependency[]) Utility
                    .removeElementFromArray(getExternalResourceDependencies(),
                            getXRDependencyFromTableItem(item), ExternalResourceDependency.class));

    table.setSelection(table.getSelectionIndices()[0] - 1);
    item.dispose();
    finishAction();
  }

  /**
   * add a external resource dependency to the model.
   */
  private void handleAdd() {
    AddExternalResourceDependencyDialog dialog = new AddExternalResourceDependencyDialog(this);

    if (dialog.open() == Window.CANCEL)
      return;

    ExternalResourceDependency xrd = addNewExternalResourceDependency(dialog);
    addXrdToGUI("", xrd);
  }

  /**
   * Alter existing external resource dependency.
   *
   * @param xrd
   *          the xrd
   * @param dialog
   *          the dialog
   */
  private void alterExistingExternalResourceDependency(ExternalResourceDependency xrd,
          AddExternalResourceDependencyDialog dialog) {
    valueChanged = false;
    String oldKey = xrd.getKey();
    xrd.setKey(setValueChanged(dialog.keyName, xrd.getKey()));
    if (valueChanged)
      propagateKeyChange(dialog.keyName, oldKey);
    xrd.setDescription(setValueChanged(multiLineFix(dialog.description), xrd.getDescription()));
    xrd.setInterfaceName(setValueChanged(dialog.interfaceName, xrd.getInterfaceName()));
    if (dialog.optional != xrd.isOptional()) {
      xrd.setOptional(dialog.optional);
      valueChanged = true;
    }
    if (valueChanged)
      finishAction();
  }

  /**
   * Adds the new external resource dependency.
   *
   * @param dialog
   *          the dialog
   * @return the external resource dependency
   */
  private ExternalResourceDependency addNewExternalResourceDependency(
          AddExternalResourceDependencyDialog dialog) {
    ExternalResourceDependency[] xrds = getExternalResourceDependencies();

    ExternalResourceDependency xrd = new ExternalResourceDependency_impl();
    alterExistingExternalResourceDependency(xrd, dialog);

    if (null == xrds)
      editor.getAeDescription()
              .setExternalResourceDependencies(new ExternalResourceDependency[] { xrd });
    else {
      ExternalResourceDependency[] newXrds = new ExternalResourceDependency[xrds.length + 1];
      System.arraycopy(xrds, 0, newXrds, 0, xrds.length);
      newXrds[newXrds.length - 1] = xrd;
      editor.getAeDescription().setExternalResourceDependencies(newXrds);
    }
    return xrd;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.taeconfigurator.editors.ui.AbstractSection#enable()
   */
  @Override
  public void enable() {
    packTable(table);
    if (isPrimitive()) {
      addButton.setEnabled(true);
      editButton.setEnabled(table.getSelectionCount() == 1);
      removeButton.setEnabled(table.getSelectionCount() > 0);
    }
  }

  /**
   * Key name already defined.
   *
   * @param key
   *          the key
   * @return true, if successful
   */
  public boolean keyNameAlreadyDefined(String key) {
    ExternalResourceDependency[] xrds = getExternalResourceDependencies();
    if (null != xrds) {
      for (int i = 0; i < xrds.length; i++) {
        if (key.equals(xrds[i].getKey())) {
          Utility.popMessage("Key Already Defined", MessageFormat.format(
                  "The key name you specified, ''{0}'', is already defined.  Please pick a different key name.",
                  new String[] { key }), MessageDialog.ERROR);
          return true;
        }

      }
    }
    return false;
  }

  /**
   * Gets the table.
   *
   * @return the table
   */
  public Table getTable() {
    return table;
  }
}
