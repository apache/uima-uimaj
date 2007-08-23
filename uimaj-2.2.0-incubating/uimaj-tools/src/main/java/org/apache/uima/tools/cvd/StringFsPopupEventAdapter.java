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

package org.apache.uima.tools.cvd;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

/**
 * Pop-up event adapter for string valued features in the FS display.  Displays a pop-up for string
 * values that were shortened for display purposes.  Users can display long strings in a separate
 * window.
 */
public class StringFsPopupEventAdapter extends MouseAdapter {

  /**
   * ActionListener for the pop-up menu.  Just shows text in a separate text window.
   */
  private static class ShowStringHandler implements ActionListener {

    private String string;

    private ShowStringHandler(String s) {
      super();
      this.string = s;
    }

    public void actionPerformed(ActionEvent e) {
      // Show string in a new window.
      JFrame frame = new JFrame("Full string value");
      JTextArea textArea = new JTextArea(this.string);
      textArea.setEditable(false);
      JScrollPane scrollPane = new JScrollPane(textArea);
      frame.setContentPane(scrollPane);
      frame.pack();
      frame.setVisible(true);
    }

  }

  public StringFsPopupEventAdapter() {
    super();
  }

  public void mousePressed(MouseEvent e) {
    showPopupMaybe(e);
  }

  public void mouseReleased(MouseEvent e) {
    showPopupMaybe(e);
  }

  private void showPopupMaybe(MouseEvent e) {
    // Mouse event is pop-up trigger?
    if (e.isPopupTrigger()) {
      Object o = e.getSource();
      // Event was triggered over the tree?
      if (o instanceof javax.swing.JTree) {
	JTree tree = (JTree) o;
	TreePath path = tree.getPathForLocation(e.getX(), e.getY());
	// Get the node in the tree model where context click occurred.
	Object leafComponent = path.getLastPathComponent();
	if (leafComponent instanceof FSNode) {
	  FSNode node = (FSNode) leafComponent;
	  // FSNode is a string node and was shortened?
	  if (node.getNodeClass() == FSNode.STRING_FS && node.isShortenedString()) {
	    // Show pop-up
	    JPopupMenu menu = new JPopupMenu();
	    JMenuItem showStringItem = new JMenuItem("Show full string");
	    showStringItem.addActionListener(new ShowStringHandler(node.getFullString()));
	    menu.add(showStringItem);
	    menu.show(e.getComponent(), e.getX(), e.getY());
	  }
	}
      }
    }
  }

}
