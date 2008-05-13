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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

/**
 * 
 * Represents a list of environment variable to be displayed in a table
 * 
 * 
 * 
 */
public class VarValList {

  public Vector tableRows = new Vector(10);

  private Set changeListeners = new HashSet();

  /**
   * Constructor
   */
  public VarValList() {
    super();
    this.initData();
  }

  /*
   * Initialize the table data.
   */
  private void initData() {
  }

  /**
   * Returns a vector of VarVal objects
   */
  public Vector getTableRows() {
    return tableRows;
  }

  /**
   * Adds a new table Row
   */
  public boolean addTableRow(VarVal tableRow) {
    if (!isDuplicate(tableRow)) {
      tableRows.add(tableRow);
      return true;
    } else
      return false;
  }

  /**
   * Adds a sample tableRow to the table
   */
  public boolean addTableRow() {
    VarVal tableRow = new VarVal("New_Variable", "Value");
    if (!isDuplicate(tableRow)) {
      tableRows.add(tableRows.size(), tableRow);
      Iterator iterator = changeListeners.iterator();
      while (iterator.hasNext())
        ((IVarValListViewer) iterator.next()).addTableRow(tableRow);
      return true;
    } else
      return false;
  }

  private boolean isDuplicate(VarVal tableRow) {
    boolean duplicate = false;
    String varName = tableRow.getVarName();
    Iterator itr = tableRows.iterator();
    while (itr.hasNext()) {
      VarVal vv = (VarVal) itr.next();
      if (vv.getVarName().equals(varName))
        duplicate = true;
    }
    return duplicate;
  }

  /**
   * @param tableRow
   */
  public void removeTableRow(VarVal tableRow) {
    tableRows.remove(tableRow);
    Iterator iterator = changeListeners.iterator();
    while (iterator.hasNext())
      ((IVarValListViewer) iterator.next()).removeTableRow(tableRow);
  }

  /**
   * Notify listeners by calling their updateTableRow() method
   * 
   * @param tableRow
   */
  public void tableRowChanged(VarVal tableRow) {
    Iterator iterator = changeListeners.iterator();
    while (iterator.hasNext())
      ((IVarValListViewer) iterator.next()).updateTableRow(tableRow);
  }

  /**
   * Removes a Change Listener
   * 
   * @param viewer
   *          A Chnage listener
   */
  public void removeChangeListener(IVarValListViewer viewer) {
    changeListeners.remove(viewer);
  }

  /**
   * 
   * Adds a change listener
   * 
   * @param viewer
   */
  public void addChangeListener(IVarValListViewer viewer) {
    changeListeners.add(viewer);
  }

}
