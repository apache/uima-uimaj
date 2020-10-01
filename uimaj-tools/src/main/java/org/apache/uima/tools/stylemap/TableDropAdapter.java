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

import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDropEvent;


/**
 * The Class TableDropAdapter.
 */
class TableDropAdapter extends DropTargetAdapter {
  
  /** The annotation features viewer. */
  private AnnotationFeaturesViewer annotationFeaturesViewer;

  /** The edit. */
  private StyleMapEditor edit;

  /**
   * Instantiates a new table drop adapter.
   *
   * @param av the av
   * @param edit the edit
   */
  public TableDropAdapter(AnnotationFeaturesViewer av, StyleMapEditor edit) {
    annotationFeaturesViewer = av;
    this.edit = edit;
  }

  /* (non-Javadoc)
   * @see java.awt.dnd.DropTargetListener#drop(java.awt.dnd.DropTargetDropEvent)
   */
  @Override
  public void drop(DropTargetDropEvent e) {
    DropTargetContext targetContext = e.getDropTargetContext();

    if ((e.getSourceActions() & DnDConstants.ACTION_COPY) != 0)
      e.acceptDrop(DnDConstants.ACTION_COPY);
    else {
      e.rejectDrop();
      return;
    }

    // We know drag is coming from tree so just get selection:
    String typeName = annotationFeaturesViewer.getSelection();
    edit.addRow(typeName);
    targetContext.dropComplete(true);
  }
}
