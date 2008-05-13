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

package org.apache.uima.tools.stylemap;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class StyleMapTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 7573410680191060690L;

  Vector v;

  // Data is held as a Vector of Vectors.
  String[] columnNames = new String[StyleConstants.NR_TABLE_COLUMNS];

  boolean DEBUG = false;

  StyleMapTableModel(String[] columnNames) {
    this.columnNames = columnNames;
  }

  public int getColumnCount() {
    return columnNames.length;
  }

  public int getRowCount() {
    if (v != null)
      return v.size();
    else
      return 0;

  }

  public String getColumnName(int col) {
    return columnNames[col];
  }

  public Object getValueAt(int row, int col) {

    return ((Vector) v.elementAt(row)).elementAt(col);
  }

  public Class getColumnClass(int c) {
    return getValueAt(0, c).getClass();
  }

  public boolean isCellEditable(int row, int col) {
    if (col == StyleConstants.TYPE_NAME_COLUMN)
      return false;
    else
      return true;
  }

  public void setValueAt(Object value, int row, int col) {

    Vector rowVector = (Vector) v.elementAt(row);
    rowVector.setElementAt(value, col);
    fireTableCellUpdated(row, col);

    if (DEBUG)
      printDebugData();
  }

  private void printDebugData() {
    int numRows = getRowCount();
    int numCols = getColumnCount();

    for (int row = 0; row < numRows; row++) {
      System.out.print("    row " + row + ":");
      for (int column = 0; column < numCols; column++) {
        Object value = getValueAt(row, column);
        if (value != null)
          System.out.println("  " + value);
      }
    }

    System.out.println("--------------------------");
  }

  public void set(Object[][] data) {
    if (data.length <= 0)
      return;

    v = new Vector(data.length);
    for (int row = 0; row < data.length; row++) {
      Vector rowVector = new Vector(columnNames.length);
      for (int column = 0; column < columnNames.length; column++)
        rowVector.addElement(data[row][column]);

      v.addElement(rowVector);
    }
  }

  public void removeRow(int row) {
    v.removeElementAt(row);
    fireTableRowsDeleted(row, row);
  }

  public void addRow(Vector rowVector) {
    v.addElement(rowVector);
    fireTableRowsInserted(v.size(), v.size());
  }

  public void moveRowUp(int row) {
    Vector rowVector = (Vector) v.elementAt(row);
    v.removeElementAt(row);
    v.insertElementAt(rowVector, row - 1);

    fireTableDataChanged();
  }

  public void moveRowDown(int row) {
    Vector rowVector = (Vector) v.elementAt(row);
    v.removeElementAt(row);
    v.insertElementAt(rowVector, row + 1);

    fireTableDataChanged();
  }

}
