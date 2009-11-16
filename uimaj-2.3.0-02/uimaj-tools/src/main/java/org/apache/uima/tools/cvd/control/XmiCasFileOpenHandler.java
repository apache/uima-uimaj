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
import java.io.File;

import javax.swing.JFileChooser;

import org.apache.uima.tools.cvd.MainFrame;

/**
 * Load a CAS from an XMI file.
 */
public class XmiCasFileOpenHandler implements ActionListener {
  
  final MainFrame main;

  // Default constructor, not used.
  @SuppressWarnings("unused")
  private XmiCasFileOpenHandler() {
    super();
    this.main = null;
  }

  public XmiCasFileOpenHandler(MainFrame main) {
    super();
    this.main = main;
  }
  
  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent event) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Open XMI CAS file");
    if (this.main.getXcasFileOpenDir() != null) {
      fileChooser.setCurrentDirectory(this.main.getXcasFileOpenDir());
    }
    int rc = fileChooser.showOpenDialog(this.main);
    if (rc == JFileChooser.APPROVE_OPTION) {
      File xmiCasFile = fileChooser.getSelectedFile();
      if (xmiCasFile.exists() && xmiCasFile.isFile()) {
        this.main.loadXmiFile(xmiCasFile);
      }
    }
  }

}
