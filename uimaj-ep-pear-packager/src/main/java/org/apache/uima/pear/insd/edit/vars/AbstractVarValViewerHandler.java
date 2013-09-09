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

package org.apache.uima.pear.insd.edit.vars;

import java.util.Arrays;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * 
 * 
 */
abstract class AbstractVarValViewerHandler {

  public Table table;

  public TableViewer tableViewer;

  public Button add;

  public Button delete;

  protected Button closeButton;

  protected ITableLabelProvider labelProvider;

  protected VarValList tableRowList = new VarValList();

  protected String[] columnNames;

  public AbstractVarValViewerHandler(Composite parent, String[] columnNames, int numParentColumns,
          VarValList tableRowList, ITableLabelProvider labelProvider) {
    this.tableRowList = tableRowList;
    this.columnNames = columnNames;
    this.labelProvider = labelProvider;

    this.addChildControls(parent, numParentColumns);
  }

  protected abstract void createTableColumns();

  protected abstract CellEditor[] createCellEditors();

  protected abstract ICellModifier createCellModifiers();

  protected abstract ViewerSorter createSorter();

  /**
   * Release resources
   */
  public void dispose() {
    // Tell the label provider to release its ressources
    tableViewer.getLabelProvider().dispose();
  }

  /**
   * Create a new shell, add the widgets, open the shell
   */
  protected void addChildControls(Composite composite, int numColumns) {

    GridLayout layout = new GridLayout(numColumns, false);
    layout.marginWidth = 4;
    composite.setLayout(layout);

    // Create the table
    createTable(composite);

    // Create and setup the TableViewer
    createTableViewer();
    tableViewer.setContentProvider(new ExampleContentProvider());
    tableViewer.setLabelProvider(labelProvider);
    // The input for the table viewer is the instance of VarValList
    tableViewer.setInput(tableRowList);

    // Add the buttons
    createButtons(composite);
  }

  /**
   * Create the Table
   */
  protected void createTable(Composite parent) {
    int style = SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION
            | SWT.HIDE_SELECTION;

    table = new Table(parent, style);

    GridData gridData = new GridData(GridData.FILL_BOTH);
    gridData.grabExcessVerticalSpace = true;
    gridData.horizontalSpan = 3;
    gridData.heightHint = 60;
    table.setLayoutData(gridData);

    table.setLinesVisible(true);
    table.setHeaderVisible(true);

    createTableColumns();
  }

  /**
   * Create the TableViewer
   */
  protected void createTableViewer() {

    tableViewer = new TableViewer(table);
    tableViewer.setUseHashlookup(true);

    tableViewer.setColumnProperties(columnNames);

    // Assign the cell editors to the viewer
    tableViewer.setCellEditors(createCellEditors());
    // Set the cell modifier for the viewer
    tableViewer.setCellModifier(createCellModifiers());
    // Set the default sorter for the viewer
    tableViewer.setSorter(createSorter());
  }

  /*
   * Close the window and dispose of resources
   */
  public void close() {
    Shell shell = table.getShell();

    if (shell != null && !shell.isDisposed())
      shell.dispose();
  }

  /**
   * InnerClass that acts as a proxy for the VarValList providing content for the Table. It
   * implements the IVarValListViewer interface since it must register changeListeners with the
   * VarValList
   */
  class ExampleContentProvider implements IStructuredContentProvider, IVarValListViewer {

    public void inputChanged(Viewer v, Object oldInput, Object newInput) {
      if (newInput != null)
        ((VarValList) newInput).addChangeListener(this);
      if (oldInput != null)
        ((VarValList) oldInput).removeChangeListener(this);
    }

    public void dispose() {
      tableRowList.removeChangeListener(this);
    }

    // Return the tableRows as an array of Objects
    public Object[] getElements(Object parent) {
      return tableRowList.getTableRows().toArray();
    }

    /*
     * (non-Javadoc)
     * 
     * @see IVarValListViewer#addTableRow(VarVal)
     */
    public void addTableRow(VarVal tableRow) {
      tableViewer.add(tableRow);
    }

    /*
     * (non-Javadoc)
     * 
     * @see IVarValListViewer#removeTableRow(VarVal)
     */
    public void removeTableRow(VarVal tableRow) {
      tableViewer.remove(tableRow);
    }

    /*
     * (non-Javadoc)
     * 
     * @see IVarValListViewer#updateTableRow(VarVal)
     */
    public void updateTableRow(VarVal tableRow) {
      tableViewer.update(tableRow, null);
    }
  }

  /**
   * Add the "Add", "Delete" and "Close" buttons
   * 
   * @param parent
   *          the parent composite
   */
  protected void createButtons(Composite parent) {

    // Create and configure the "Add" button
    add = new Button(parent, SWT.PUSH | SWT.CENTER);
    add.setText("Add");

    GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    gridData.widthHint = 80;
    add.setLayoutData(gridData);
    add.addSelectionListener(new SelectionAdapter() {

      // Add a tableRow to the VarValList and refresh the view
      public void widgetSelected(SelectionEvent e) {
        if (!tableRowList.addTableRow()) {
          MessageDialog.openWarning(new Shell(), "Duplicate Variable",
                  "The variable 'New_Variable' already exists");
        }
      }
    });

    // Create and configure the "Delete" button
    delete = new Button(parent, SWT.PUSH | SWT.CENTER);
    delete.setText("Delete");
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    gridData.widthHint = 80;
    delete.setLayoutData(gridData);

    delete.addSelectionListener(new SelectionAdapter() {

      // Remove the selection and refresh the view
      public void widgetSelected(SelectionEvent e) {
        VarVal tableRow = (VarVal) ((IStructuredSelection) tableViewer.getSelection())
                .getFirstElement();
        if (tableRow != null) {
          tableRowList.removeTableRow(tableRow);
        }
      }
    });

  }

  /**
   * Return the column names in a collection
   * 
   * @return List containing column names
   */
  public java.util.List getColumnNames() {
    return Arrays.asList(columnNames);
  }

  /**
   * @return currently selected item
   */
  public ISelection getSelection() {
    return tableViewer.getSelection();
  }

  /**
   * Return the VarValList
   */
  public VarValList getTableRowList() {
    return tableRowList;
  }

  /**
   * Return the parent composite
   */
  public Control getControl() {
    return table.getParent();
  }

  /**
   * Return the 'close' Button
   */
  public Button getCloseButton() {
    return closeButton;
  }

}