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
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.apache.uima.UIMAFramework;
import org.apache.uima.tools.images.Images;
import org.apache.uima.util.Level;

/**
 * Dialog showing standard UIMA splash screen and OK button. To be used for "About" menu item in
 * GUIs.
 * 
 */
public class AboutDialog extends JDialog {
  private static final long serialVersionUID = -3901327861122722078L;

  private static final String ABOUT_TEXT;
  
  public AboutDialog(JFrame aParentFrame, String aDialogTitle) {
    super(aParentFrame, aDialogTitle);

    getContentPane().setLayout(new BorderLayout());
    JButton closeButton = new JButton("OK");

    JLabel imageLabel = new JLabel(Images.getImageIcon(Images.UIMA_LOGO_BIG));
    JPanel imagePanel = new JPanel();
    imagePanel.setBackground(Color.WHITE);
    imagePanel.add(imageLabel);
    getContentPane().add(imagePanel, BorderLayout.WEST);
    
    String aboutText = ABOUT_TEXT.replaceAll("\\$\\{version\\}", UIMAFramework.getVersionString());
       
    JTextArea textArea = new JTextArea(aboutText);
    textArea.setEditable(false);
    getContentPane().add(textArea, BorderLayout.CENTER);
    
    JPanel buttonPanel = new JPanel();
    buttonPanel.add(closeButton);
    getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    this.pack();
    this.setResizable(false);
    this.setModal(true);
    // event for the closeButton button
    closeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        AboutDialog.this.setVisible(false);
      }
    });
  }

  //Read the dialog text from a resource file
  static {
    StringBuffer buf = new StringBuffer();
    try {
      InputStream textStream = AboutDialog.class.getResourceAsStream("about.txt"); 
      BufferedReader reader = new BufferedReader(new InputStreamReader(textStream));
      String line;
      while ((line = reader.readLine()) != null) {
        buf.append(line).append('\n');      
      }
    } catch (Exception e) {
      UIMAFramework.getLogger().log(Level.WARNING, "About text could not be loaded", e);
    }    
    ABOUT_TEXT = buf.toString();    
  }
}
