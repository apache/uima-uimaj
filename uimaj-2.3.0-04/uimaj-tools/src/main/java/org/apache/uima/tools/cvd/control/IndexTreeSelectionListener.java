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

package org.apache.uima.tools.cvd.control;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.tools.cvd.IndexTreeNode;
import org.apache.uima.tools.cvd.MainFrame;
import org.apache.uima.tools.cvd.TypeTreeNode;

/**
 * Change the display of the FSTree if a type in an index is selected.
 */
public class IndexTreeSelectionListener implements TreeSelectionListener {

  private final MainFrame main;

  public IndexTreeSelectionListener(MainFrame frame) {
    this.main = frame;
  }

  /**
   * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
   */
  public void valueChanged(TreeSelectionEvent arg0) {
    // System.out.println("Tree selection value changed");
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) this.main.getIndexTree()
        .getLastSelectedPathComponent();
    if (node == null) {
      return;
    }
    Object userObject = node.getUserObject();
    String label = null;
    Type type = null;
    if (userObject instanceof IndexTreeNode) {
      IndexTreeNode indexNode = (IndexTreeNode) userObject;
      label = indexNode.getName();
      type = indexNode.getType();
    } else if (userObject instanceof TypeTreeNode) {
      TypeTreeNode typeNode = (TypeTreeNode) userObject;
      label = typeNode.getLabel();
      type = typeNode.getType();
    } else {
      return;
    }
    this.main.setIndexLabel(label);
    this.main.setAnnotationIndex(label.equals(CAS.STD_ANNOTATION_INDEX));
    this.main.setIndex(this.main.getCas().getIndexRepository().getIndex(label, type));
    this.main.updateFSTree(label, this.main.getIndex());
    this.main.setAllAnnotationViewerItemEnable(((CASImpl) this.main.getCas())
        .isAnnotationType(type));
    this.main.getTextArea().getCaret().setVisible(true);
  }

}