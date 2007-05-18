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
import java.util.Iterator;

import javax.swing.JFileChooser;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.internal.util.Timer;
import org.apache.uima.tools.cvd.MainFrame;

/**
 * Load an XCAS file.
 */
public class XCASFileOpenEventHandler implements ActionListener {

  private final MainFrame main;

  public XCASFileOpenEventHandler(MainFrame frame) {
    super();
    this.main = frame;
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
   */
  public void actionPerformed(ActionEvent event) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Open XCAS file");
    if (this.main.getXcasFileOpenDir() != null) {
      fileChooser.setCurrentDirectory(this.main.getXcasFileOpenDir());
    }
    int rc = fileChooser.showOpenDialog(this.main);
    if (rc == JFileChooser.APPROVE_OPTION) {
      File xcasFile = fileChooser.getSelectedFile();
      if (xcasFile.exists() && xcasFile.isFile()) {
        try {
          this.main.setXcasFileOpenDir(xcasFile.getParentFile());
          Timer time = new Timer();
          time.start();
          SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
          XCASDeserializer xcasDeserializer = new XCASDeserializer(this.main.getCas()
              .getTypeSystem());
          this.main.getCas().reset();
          parser.parse(xcasFile, xcasDeserializer.getXCASHandler(this.main.getCas()));
          time.stop();
          // Populate sofa combo box with the names of all text
          // Sofas in the CAS
          this.main.setDisableSofaListener(true);
          String currentView = (String) this.main.getSofaSelectionComboBox().getSelectedItem();
          this.main.getSofaSelectionComboBox().removeAllItems();
          this.main.getSofaSelectionComboBox().addItem(CAS.NAME_DEFAULT_SOFA);
          Iterator sofas = ((CASImpl) this.main.getCas()).getBaseCAS().getSofaIterator();
          Feature sofaIdFeat = this.main.getCas().getTypeSystem().getFeatureByFullName(
              CAS.FEATURE_FULL_NAME_SOFAID);
          boolean nonDefaultSofaFound = false;
          while (sofas.hasNext()) {
            SofaFS sofa = (SofaFS) sofas.next();
            String sofaId = sofa.getStringValue(sofaIdFeat);
            if (!CAS.NAME_DEFAULT_SOFA.equals(sofaId)) {
              this.main.getSofaSelectionComboBox().addItem(sofaId);
              nonDefaultSofaFound = true;
            }
          }
          // reuse last selected view if found in new CAS
          int newIndex = 0;
          String newView = CAS.NAME_DEFAULT_SOFA;
          for (int i = 0; i < this.main.getSofaSelectionComboBox().getItemCount(); i++) {
            if (currentView.equals(this.main.getSofaSelectionComboBox().getItemAt(i))) {
              newIndex = i;
              newView = currentView;
              break;
            }
          }
          // make sofa selector visible if any text sofa other
          // than the default was found
          this.main.getSofaSelectionPanel().setVisible(nonDefaultSofaFound);
          this.main.setCas(this.main.getCas().getView(newView));
          this.main.setDisableSofaListener(false);

          this.main.getSofaSelectionComboBox().setSelectedIndex(newIndex);
          String text = this.main.getCas().getDocumentText();
          if (text == null) {
            text = this.main.getCas().getSofaDataURI();
            if (text != null) {
              text = "SofaURI = " + text;
            } else {
              if (this.main.getCas().getSofaDataArray() != null) {
                text = "Sofa array with mime type = "
                    + this.main.getCas().getSofa().getSofaMime();
              }
            }
          }
          this.main.getTextArea().setText(text);
          if (text == null) {
            this.main.getTextArea().repaint();
          }

          this.main.setTitle("XCAS");
          this.main.updateIndexTree(true);
          this.main.getRunOnCasMenuItem().setEnabled(true);
          this.main.setStatusbarMessage("Done loading XCAS file in " + time.getTimeSpan() + ".");
        } catch (Exception e) {
          e.printStackTrace();
          this.main.handleException(e);
        }
      }
    }
  }

}