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

import org.apache.uima.tools.cvd.FSNode;
import org.apache.uima.tools.cvd.FSTreeNode;
import org.apache.uima.tools.cvd.MainFrame;

/**
 * Handle selection of annotations in annotation frame. If selected node represents an annotation,
 * highlight the corresponding extent in the text.
 */
public class FSTreeSelectionListener implements TreeSelectionListener {

  private final MainFrame main;

  public FSTreeSelectionListener(MainFrame frame) {
    this.main = frame;
  }

  /**
   * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
   */
  public void valueChanged(TreeSelectionEvent event) {
    // System.out.println("");
    FSTreeNode protoNode = (FSTreeNode) this.main.getFsTree().getLastSelectedPathComponent();
    if (!(protoNode instanceof FSNode)) {
      return;
    }
    FSNode node = (FSNode) protoNode;
    if (node == null) {
      return;
    }
    // Remeber start of current selection.
    final int currentSelStart = this.main.getTextArea().getSelectionStart();
    if (node.isAnnotation()) {
      if (null != this.main.getCas().getDocumentText()) {
        this.main.getTextArea().setSelectionStart(node.getStart());
        this.main.getTextArea().setSelectionEnd(node.getEnd());
        // System.out.println(
        // "Setting selection from " + node.getStart() + " to " +
        // node.getEnd());
        this.main.getTextArea().getCaret().setSelectionVisible(true);
      }
    } else {
      this.main.getTextArea().setSelectionEnd(currentSelStart);
    }

  }

}