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

/**
 * The Class StyleMapTableModel.
 */
public class StyleMapTableModel extends AbstractTableModel {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 7573410680191060690L;

  /** The v. */
  Vector v;

  /** The column names. */
  // Data is held as a Vector of Vectors.
  String[] columnNames = new String[StyleConstants.NR_TABLE_COLUMNS];

  /** The debug. */
  boolean DEBUG = false;

  /**
   * Instantiates a new style map table model.
   *
   * @param columnNames
   *          the column names
   */
  StyleMapTableModel(String[] columnNames) {
    this.columnNames = columnNames;
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  @Override
  public int getColumnCount() {
    return columnNames.length;
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.TableModel#getRowCount()
   */
  @Override
  public int getRowCount() {
    if (v != null)
      return v.size();
    else
      return 0;

  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.AbstractTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int col) {
    return columnNames[col];
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  @Override
  public Object getValueAt(int row, int col) {

    return ((Vector) v.elementAt(row)).elementAt(col);
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
   */
  @Override
  public Class getColumnClass(int c) {
    return getValueAt(0, c).getClass();
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
   */
  @Override
  public boolean isCellEditable(int row, int col) {
    if (col == StyleConstants.TYPE_NAME_COLUMN)
      return false;
    else
      return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object, int, int)
   */
  @Override
  public void setValueAt(Object value, int row, int col) {

    Vector rowVector = (Vector) v.elementAt(row);
    rowVector.setElementAt(value, col);
    fireTableCellUpdated(row, col);

    if (DEBUG)
      printDebugData();
  }

  /**
   * Prints the debug data.
   */
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

  /**
   * Sets the.
   *
   * @param data
   *          the data
   */
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

  /**
   * Removes the row.
   *
   * @param row
   *          the row
   */
  public void removeRow(int row) {
    v.removeElementAt(row);
    fireTableRowsDeleted(row, row);
  }

  /**
   * Adds the row.
   *
   * @param rowVector
   *          the row vector
   */
  public void addRow(Vector rowVector) {
    v.addElement(rowVector);
    fireTableRowsInserted(v.size(), v.size());
  }

  /**
   * Move row up.
   *
   * @param row
   *          the row
   */
  public void moveRowUp(int row) {
    Vector rowVector = (Vector) v.elementAt(row);
    v.removeElementAt(row);
    v.insertElementAt(rowVector, row - 1);

    fireTableDataChanged();
  }

  /**
   * Move row down.
   *
   * @param row
   *          the row
   */
  public void moveRowDown(int row) {
    Vector rowVector = (Vector) v.elementAt(row);
    v.removeElementAt(row);
    v.insertElementAt(rowVector, row + 1);

    fireTableDataChanged();
  }

}
