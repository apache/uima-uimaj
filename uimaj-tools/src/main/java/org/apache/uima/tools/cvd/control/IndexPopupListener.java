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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.uima.cas.CAS;
import org.apache.uima.tools.cvd.IndexTreeNode;
import org.apache.uima.tools.cvd.MainFrame;
import org.apache.uima.tools.cvd.TypeTreeNode;


/**
 * The listener interface for receiving indexPopup events.
 * The class that is interested in processing a indexPopup
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addIndexPopupListener</code> method. When
 * the indexPopup event occurs, that object's appropriate
 * method is invoked.
 *
// * @see IndexPopupEvent
 */
public class IndexPopupListener extends MouseAdapter {

  
  /** The main. */
  private final MainFrame main;

  /**
   * Instantiates a new index popup listener.
   *
   * @param frame the frame
   */
  public IndexPopupListener(MainFrame frame) {
    this.main = frame;
  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseAdapter#mousePressed(java.awt.event.MouseEvent)
   */
  @Override
  public void mousePressed(MouseEvent e) {
    maybeShowPopup(e);
  }

  /* (non-Javadoc)
   * @see java.awt.event.MouseAdapter#mouseReleased(java.awt.event.MouseEvent)
   */
  @Override
  public void mouseReleased(MouseEvent e) {
    maybeShowPopup(e);
  }

  /**
   * Maybe show popup.
   *
   * @param e the e
   */
  private void maybeShowPopup(MouseEvent e) {
    if (e.isPopupTrigger()) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) this.main.getIndexTree()
          .getLastSelectedPathComponent();
      if (node == null) {
        return;
      }
      Object userObject = node.getUserObject();
      String annotTitle = null;
      boolean isAnnotation = true;
      if (userObject instanceof IndexTreeNode) {
        IndexTreeNode iNode = (IndexTreeNode) userObject;
        if (!iNode.getName().equals(CAS.STD_ANNOTATION_INDEX)) {
          isAnnotation = false;
        }
        annotTitle = iNode.getType().getName();
      } else if (userObject instanceof TypeTreeNode) {
        TypeTreeNode tNode = (TypeTreeNode) userObject;
        if (!tNode.getLabel().equals(CAS.STD_ANNOTATION_INDEX)) {
          isAnnotation = false;
        }
        annotTitle = tNode.getType().getName();
      } else {
        isAnnotation = false;
      }
      JPopupMenu menu = new JPopupMenu();
      JMenuItem item = null;
      if (isAnnotation) {
        item = new JMenuItem("Show annotations: " + annotTitle);
        item.addActionListener(new ShowAnnotatedTextHandler(this.main));
      } else {
        item = new JMenuItem("No annotations selected");
      }
      menu.add(item);
      menu.show(e.getComponent(), e.getX(), e.getY());
    }
  }
}