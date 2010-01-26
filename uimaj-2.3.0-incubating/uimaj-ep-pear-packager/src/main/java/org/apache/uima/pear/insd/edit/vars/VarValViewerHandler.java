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

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;

/**
 * 
 * Handles the display and management of an environment variables table
 * 
 * 
 */
public class VarValViewerHandler extends AbstractVarValViewerHandler {

  public VarValViewerHandler(Composite parent, String[] columnNames, int numParentColumns,
          VarValList tableRowList) {
    super(parent, columnNames, numParentColumns, tableRowList, new VarValLabelProvider());
  }

  protected ICellModifier createCellModifiers() {
    return new VarValCellModifier(this, columnNames, tableRowList);
  }

  protected ViewerSorter createSorter() {
    return new VarValSorter(VarValSorter.VAR_NAME);
  }

  protected void createTableColumns() {
    // 1st column with image/checkboxes - NOTE: The SWT.CENTER has no effect!!
    TableColumn column = new TableColumn(table, SWT.LEFT, 0);
    column.setText("Property Name");
    column.setWidth(200);
    // Add listener to column so tableRows are sorted by description when clicked
    column.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        tableViewer.setSorter(new VarValSorter(VarValSorter.VAR_NAME));
      }
    });

    // 2nd column with tableRow Description
    column = new TableColumn(table, SWT.LEFT, 1);
    column.setText("Property Value");
    column.setWidth(250);
    // Add listener to column so tableRows are sorted by description when clicked
    column.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        tableViewer.setSorter(new VarValSorter(VarValSorter.VAR_VALUE));
      }
    });

  }

  protected CellEditor[] createCellEditors() {

    CellEditor[] editors = new CellEditor[columnNames.length];
    TextCellEditor textEditor = new TextCellEditor(table);
    editors[0] = textEditor;

    textEditor = new TextCellEditor(table);
    editors[1] = textEditor;

    return editors;
  }
}