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

import org.apache.uima.UIMAFramework;
import org.apache.uima.internal.util.Timer;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.tools.cvd.MainFrame;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;

/**
 * Load a type system file.  Need to load type system before one can load XCAS.
 */
public class TypeSystemFileOpenEventHandler implements ActionListener {

  private final MainFrame main;

  public TypeSystemFileOpenEventHandler(MainFrame frame) {
    super();
    this.main = frame;
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
   */
  public void actionPerformed(ActionEvent event) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Open Type System File");
    if (this.main.getXcasFileOpenDir() != null) {
      fileChooser.setCurrentDirectory(this.main.getXcasFileOpenDir());
    }
    int rc = fileChooser.showOpenDialog(this.main);
    if (rc == JFileChooser.APPROVE_OPTION) {
      File tsFile = fileChooser.getSelectedFile();
      if (tsFile.exists() && tsFile.isFile()) {
        try {
          this.main.setXcasFileOpenDir(tsFile.getParentFile());
          Timer time = new Timer();
          time.start();
          Object descriptor = UIMAFramework.getXMLParser().parse(new XMLInputSource(tsFile));
          // instantiate CAS to get type system. Also build style
          // map file if there is none.
          TypeSystemDescription tsDesc = (TypeSystemDescription) descriptor;
          tsDesc.resolveImports();
          this.main.destroyAe();
          this.main.setCas(CasCreationUtils
              .createCas(tsDesc, null, new FsIndexDescription[0]));
          this.main.setRunOnCasEnabled();
          this.main.setRerunEnabled(false);
          this.main.getTextArea().setText("");
          this.main.resetTrees();
          this.main.setTypeSystemViewerEnabled(true);
          this.main.setEnableCasFileReadingAndWriting();
          time.stop();
          this.main.setStatusbarMessage("Done loading type system file in " + time.getTimeSpan() + ".");
        } catch (Exception e) {
          e.printStackTrace();
          this.main.handleException(e);
        }
      }
    }
  }

}