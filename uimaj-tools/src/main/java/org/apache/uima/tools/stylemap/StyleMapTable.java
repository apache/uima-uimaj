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

import java.awt.Dimension;
import java.awt.dnd.DropTarget;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

/**
 * Data structure used by the editor, which represents an entry in the style map.
 */
public class StyleMapTable extends JTable {
  private static final long serialVersionUID = 3556134276343308170L;

  private TableGUIMediator med;

  public StyleMapTable(TableModel model, AnnotationFeaturesViewer av, StyleMapEditor edit,
          TableGUIMediator tmed) {
    super(model);
    med = tmed;
    setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    setDropTarget(new DropTarget(this, new TableDropAdapter(av, edit)));

    getTableHeader().setReorderingAllowed(false);
    ListSelectionModel lsm = this.getSelectionModel();
    lsm.addListSelectionListener(new TableSelectionListener(med));
  }

  public Dimension getPreferredScrollableViewportSize() {
    return this.getPreferredSize();
  }

}
