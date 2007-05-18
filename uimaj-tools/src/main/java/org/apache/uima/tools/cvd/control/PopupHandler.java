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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.tree.TreePath;

import org.apache.uima.tools.cvd.FSTreeModel;
import org.apache.uima.tools.cvd.MainFrame;

/**
 * Action handler for text pop-up menu items.  Select chosen annotation in FS tree, make visible.
 */
public class PopupHandler implements ActionListener {

  private final MainFrame main;

  private final int node;

  public PopupHandler(MainFrame frame, int n) {
    super();
    this.main = frame;
    this.node = n;
  }

  public void actionPerformed(ActionEvent e) {
    FSTreeModel treeModel = (FSTreeModel) this.main.getFsTree().getModel();
    TreePath path = treeModel.pathToNode(this.node);
    this.main.getFsTree().setSelectionPath(path);
    this.main.getFsTree().scrollPathToVisible(path);
  }

}