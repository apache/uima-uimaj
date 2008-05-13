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

import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.apache.uima.tools.cvd.MainFrame;

/**
 * Text pop-up action (for keyboard accessibility).
 */
public class TextContextMenuAction extends AbstractAction {

  private final MainFrame main;

  public TextContextMenuAction(MainFrame frame) {
    this.main = frame;
  }

  private static final long serialVersionUID = -5518456467913617514L;

  public void actionPerformed(ActionEvent arg0) {
    Point caretPos = this.main.getTextArea().getCaret().getMagicCaretPosition();
    if (caretPos == null) {
      // No idea why this is needed. Bug in JTextArea, or my poor understanding of the magics of
      // carets. The point is null when the text area is first focused.
      this.main.showTextPopup(0, 0);
    } else {
      this.main.showTextPopup(caretPos.x, caretPos.y);
    }
  }

}