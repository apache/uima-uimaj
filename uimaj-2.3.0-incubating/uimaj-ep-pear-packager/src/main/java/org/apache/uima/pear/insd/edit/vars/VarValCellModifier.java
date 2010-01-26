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

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.swt.widgets.TableItem;

/**
 * This class implements an ICellModifier An ICellModifier is called when the user modifes a cell in
 * the tableViewer
 * 
 * 
 */
class VarValCellModifier implements ICellModifier {
  private VarValViewerHandler tableViewerExample;

  VarValList tableRowList;

  /**
   * Constructor
   */
  public VarValCellModifier(VarValViewerHandler tableViewerExample, String[] columnNames,
          VarValList tableRowList) {
    super();
    this.tableRowList = tableRowList;
    this.tableViewerExample = tableViewerExample;
  }

  /**
   * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object, java.lang.String)
   */
  public boolean canModify(Object element, String property) {
    return true;
  }

  /**
   * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object, java.lang.String)
   */
  public Object getValue(Object element, String property) {

    // Find the index of the column
    int columnIndex = tableViewerExample.getColumnNames().indexOf(property);

    Object result = null;
    VarVal tableRow = (VarVal) element;

    switch (columnIndex) {
      case 0:
        result = tableRow.getVarName();
        break;
      case 1:
        result = tableRow.getVarValue();
        break;
      default:
        result = "";
    }
    return result;
  }

  /**
   * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object, java.lang.String,
   *      java.lang.Object)
   */
  public void modify(Object element, String property, Object value) {

    // Find the index of the column
    int columnIndex = tableViewerExample.getColumnNames().indexOf(property);

    TableItem item = (TableItem) element;
    VarVal tableRow = (VarVal) item.getData();
    String valueString;

    switch (columnIndex) {
      case 0:
        valueString = ((String) value).trim();
        tableRow.setVarName(valueString);
        break;
      case 1:
        valueString = ((String) value).trim();
        tableRow.setVarValue(valueString);
        break;
      default:
    }
    tableViewerExample.getTableRowList().tableRowChanged(tableRow);
  }
}
