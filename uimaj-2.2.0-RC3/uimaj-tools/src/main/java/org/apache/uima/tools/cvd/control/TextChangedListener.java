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

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.uima.tools.cvd.MainFrame;

/**
 * Listener for changes in text frame. When the text is changed, the CAS is removed as the text from
 * the CAS does no longer correspond to the text that is displayed in the frame, hence annotation
 * offsets are no longer correct.
 */
public class TextChangedListener implements DocumentListener {

  private final MainFrame main;

  public TextChangedListener(MainFrame frame) {
    this.main = frame;
  }

  public void changedUpdate(DocumentEvent arg0) {
    // Do nothing.
  }

  public void insertUpdate(DocumentEvent arg0) {
    removeUpdate(arg0);
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
   */
  public void removeUpdate(DocumentEvent arg0) {
    if (!this.main.isDirty()) {
      this.main.setDirty(true);
      this.main.setTitle();
      if (this.main.getCas() != null) {
        this.main.setStatusbarMessage("Text changed, CAS removed.");
      }
      this.main.resetTrees();
    }
  }

}