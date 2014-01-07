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

import javax.swing.JComponent;
import javax.swing.WindowConstants;

import org.apache.uima.tools.cvd.MainFrame;

public class ShowTypesystemHandler implements ActionListener {

  
  private final MainFrame main;

  /**
   * @param frame
   */
  public ShowTypesystemHandler(MainFrame frame) {
    this.main = frame;
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
   */
  public void actionPerformed(ActionEvent event) {
    if (this.main.getCas() == null) {
      return;
    }
    org.apache.uima.tools.cvd.tsview.MainFrame tsFrame = new org.apache.uima.tools.cvd.tsview.MainFrame();
    tsFrame.addWindowListener(new CloseTypeSystemHandler(this.main));
    JComponent tsContentPane = (JComponent) tsFrame.getContentPane();
    this.main.setPreferredSize(tsContentPane, MainFrame.tsWindowSizePref);
    tsFrame.setTypeSystem(this.main.getCas().getTypeSystem());
    tsFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    tsFrame.pack();
    tsFrame.setVisible(true);
  }

}