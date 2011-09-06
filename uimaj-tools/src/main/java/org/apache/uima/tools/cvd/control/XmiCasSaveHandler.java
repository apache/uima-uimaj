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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.tools.cvd.MainFrame;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.SAXException;

/**
 * Save CAS to disk as XMI.
 */
public class XmiCasSaveHandler implements ActionListener {

  private final MainFrame main;
  
  // Default constructor
  @SuppressWarnings("unused")
  private XmiCasSaveHandler() {
    super();
    this.main = null;
  }
  
  public XmiCasSaveHandler(MainFrame main) {
    super();
    this.main = main;
  }

  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent event) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Save XMI CAS file");
    if (this.main.getXcasFileOpenDir() != null) {
      fileChooser.setCurrentDirectory(this.main.getXcasFileOpenDir());
    }
    int rc = fileChooser.showSaveDialog(this.main);
    if (rc == JFileChooser.APPROVE_OPTION) {
      File xmiCasFile = fileChooser.getSelectedFile();
      if (!this.main.confirmOverwrite(xmiCasFile)) {
        return;
      }
      this.main.setXcasFileOpenDir(xmiCasFile.getParentFile());
      try {
        OutputStream outStream = new BufferedOutputStream(new FileOutputStream(xmiCasFile));
        XMLSerializer xmlSerializer = new XMLSerializer(outStream);
        XmiCasSerializer xmiCasSerializer = new XmiCasSerializer(this.main.getCas().getTypeSystem());
        xmiCasSerializer.serialize(this.main.getCas(), xmlSerializer.getContentHandler());
        outStream.close();
      } catch (IOException e) {
        this.main.handleException(e);
      } catch (SAXException e) {
        this.main.handleException(e);
      }
    }
  }

}
