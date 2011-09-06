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

public class FileSaveAsEventHandler implements ActionListener {

  private final MainFrame main;

  public FileSaveAsEventHandler(MainFrame frame) {
    super();
    this.main = frame;
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
   */
  public void actionPerformed(ActionEvent event) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Save file as...");
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
    if (this.main.getFileOpenDir() != null) {
      fileChooser.setCurrentDirectory(this.main.getFileOpenDir());
    }
    int rc = fileChooser.showSaveDialog(this.main);
    if (rc == JFileChooser.APPROVE_OPTION) {
      File tmp = this.main.getTextFile();
      File fileToSaveTo = fileChooser.getSelectedFile();
      if (!this.main.confirmOverwrite(fileToSaveTo)) {
        return;
      }      
      this.main.setTextFile(fileToSaveTo);
      boolean fileSaved = this.main.saveFile();
      if (fileSaved) {
        this.main.setDirty(false);
        this.main.setTitle();
        this.main.setSaveTextFileEnable(true);
        this.main.setFileStatusMessage();
        this.main.setStatusbarMessage("Text file " + this.main.getTextFile().getName() + " saved.");
      } else {
        this.main.setTextFile(tmp);
      }
    }
  }

}