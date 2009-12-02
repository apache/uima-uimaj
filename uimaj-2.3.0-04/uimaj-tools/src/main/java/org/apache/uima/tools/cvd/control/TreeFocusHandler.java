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

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

public class TreeFocusHandler implements FocusListener {

  private JTree tree;

  public TreeFocusHandler(JTree tree) {
    super();
    this.tree = tree;
  }

  public void focusGained(FocusEvent arg0) {
    TreePath selPath = this.tree.getSelectionPath();
    if (selPath == null) {
      selPath = new TreePath(new Object[] { this.tree.getModel().getRoot() });
      this.tree.setSelectionPath(selPath);
    }
  }

  public void focusLost(FocusEvent arg0) {
    // Do nothing.
  }

}