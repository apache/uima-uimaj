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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;

class ColorEditor extends DefaultCellEditor {
  private static final long serialVersionUID = 4766162815461077066L;

  Color currentColor = null;

  public ColorEditor(JButton button) {
    super(new JCheckBox());
    // Unfortunately, the constructor expects a check box,
    // combo box, or text field.
    editorComponent = button;

    // Must do the following to ensure editing stops:
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        fireEditingStopped();
      }
    });
  }

  public void fireEditingStopped() {
    super.fireEditingStopped();
  }

  public Object getCellEditorValue() {
    return currentColor;
  }

  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
          int row, int column) {
    // ((JButton) editorComponent).setText(value.toString());
    currentColor = (Color) value;
    return editorComponent;
  }
}
