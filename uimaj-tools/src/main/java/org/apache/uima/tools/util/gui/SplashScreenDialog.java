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

package org.apache.uima.tools.util.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.uima.tools.images.Images;

/**
 * Dialog showing standard UIMA splash screen and OK button. To be used for "About" menu item in
 * GUIs.
 * 
 */
public class SplashScreenDialog extends JDialog {
  private static final long serialVersionUID = -3901327861122722078L;

  public SplashScreenDialog(JFrame aParentFrame, String aDialogTitle) {
    super(aParentFrame, aDialogTitle);

    getContentPane().setLayout(new BorderLayout());
    JButton closeButton = new JButton("OK");

    getContentPane().add(new JLabel(Images.getImageIcon(Images.SPLASH)), BorderLayout.CENTER);
    JPanel buttonPanel = new JPanel();
    buttonPanel.add(closeButton);
    getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    this.pack();
    this.setResizable(false);
    this.setModal(true);
    // event for the closeButton button
    closeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        SplashScreenDialog.this.setVisible(false);
      }
    });
  }
}
