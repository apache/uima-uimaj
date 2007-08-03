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

import javax.swing.event.ListSelectionEvent;

import org.apache.uima.tools.util.gui.ImageButton;

/**
 * Mediates GI elements in StyleMapEditor keeping buttons enabled or disabled depending on whether a
 * row is selected.
 * 
 */
public class TableGUIMediator {
  private StyleMapTable table;

  private ImageButton up, down, delete; // moves rows in style map

  private ImageButton addEntryButton; // moves classes into style map

  private int rowCount;

  public TableGUIMediator() {

  }

  public void setTable(StyleMapTable t) {
    table = t;
  }

  public void tableClicked(ListSelectionEvent ev) {
    // enable down arrow if there are more rows below the selection
    // enable up arrow if there is one row or more above selection
    int row = table.getSelectedRow();
    rowCount = table.getRowCount();
    if (row > 0)
      up.setEnabled(true);
    if (row < rowCount)
      down.setEnabled(true);
    if (row == (rowCount - 1))
      down.setEnabled(false);
    if (row == 0)
      up.setEnabled(false);
    if (row >= 0 && row < (rowCount)) {
      delete.setEnabled(true);
    }

  }

  public void setButtons(ImageButton up, ImageButton down, ImageButton delete) {
    this.up = up;
    this.down = down;
    this.delete = delete;
    up.setEnabled(false);
    down.setEnabled(false);
    delete.setEnabled(false);
  }

  public int getRowSelected() {
    return table.getEditingRow();
  }

  public int getColumnSelected() {
    return table.getEditingColumn();
  }

  // adds the Add table entry button to the mediator
  public void setEntryButton(ImageButton but) {
    addEntryButton = but;
    addEntryButton.setEnabled(false); // initially disabled
  }

  // when the tree is clicked the button can be enabled.
  public void treeClicked() {
    addEntryButton.setEnabled(true);
  }
}
