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

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

class ColorRenderer extends JLabel implements TableCellRenderer {

  private static final long serialVersionUID = 4260743930100354668L;

  private Border unselectedBorder = null;

  private Border selectedBorder = null;

  public ColorRenderer(StyleMapTable sTable) {
    super();

    setOpaque(true);
    // MUST do this for background to show up.

    if (selectedBorder == null)
      selectedBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, sTable.getSelectionBackground());

    if (unselectedBorder == null)
      unselectedBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, sTable.getBackground());
  }

  public Component getTableCellRendererComponent(JTable table, Object color, boolean isSelected,
          boolean hasFocus, int row, int column) {
    setBackground((Color) color);

    if (isSelected)
      setBorder(selectedBorder);
    else
      setBorder(unselectedBorder);

    return this;
  }
}
