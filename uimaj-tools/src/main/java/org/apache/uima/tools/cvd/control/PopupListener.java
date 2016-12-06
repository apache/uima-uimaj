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

import org.apache.uima.tools.cvd.MainFrame;


/**
 * Handle text pop-up (mouse) events. The actual logic for creating the text pop-up menu is not
 * here.
 *
// * @see PopupEvent
 */
public class PopupListener extends MouseAdapter {

  /** The main. */
  private final MainFrame main;

  /**
   * Instantiates a new popup listener.
   *
   * @param frame the frame
   */
  public PopupListener(MainFrame frame) {
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
      this.main.showTextPopup(e.getX(), e.getY());
    }
  }
}