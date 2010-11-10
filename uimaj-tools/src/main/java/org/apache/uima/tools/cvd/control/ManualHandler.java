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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

import org.apache.uima.tools.cvd.CVD;
import org.apache.uima.tools.cvd.MainFrame;

/**
 * Show the CVD manual in a Swing html widget.  Unfortunately, the html we currently produce from
 * our docbook source is too advanced for the the simple html widget, and is virtually unreadable.
 * That makes this option relatively useless atm (it's better the more recent the Java version).
 */
public class ManualHandler implements ActionListener {

  private final MainFrame main;

  public ManualHandler(MainFrame frame) {
    this.main = frame;
  }

  private class Hyperactive implements HyperlinkListener {

    public void hyperlinkUpdate(HyperlinkEvent e) {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        JEditorPane pane = (JEditorPane) e.getSource();
        if (e instanceof HTMLFrameHyperlinkEvent) {
          HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent) e;
          HTMLDocument doc = (HTMLDocument) pane.getDocument();
          doc.processHTMLFrameHyperlinkEvent(evt);
        } else {
          try {
            pane.setPage(e.getURL());
          } catch (Throwable t) {
            t.printStackTrace();
          }
        }
      }
    }
  }

  public void actionPerformed(ActionEvent event) {
    try {
      String manFileName = "tools.html";
      JFrame manFrame = new JFrame("CVD Manual");
      JEditorPane editorPane = new JEditorPane();
      editorPane.setEditable(false);
      editorPane.addHyperlinkListener(new Hyperactive());
      URL manURL = ClassLoader.getSystemResource(manFileName);
      if (manURL == null) {
        String manpath = System.getProperty(CVD.MAN_PATH_PROPERTY, null);
        if (manpath != null) {
          File manDir = new File(manpath);
          File manFile = new File(manDir, manFileName);
          if (manFile.exists()) {
            manURL = manFile.toURL();
          } else {
            String msg = String.format("Can't find manual in directory: %s", manpath);
            if (!manDir.exists()) {
              msg += String.format("\n Directory doesn't exist");
            }
            JOptionPane.showMessageDialog(this.main, msg, "Error loading manual",
                JOptionPane.ERROR_MESSAGE);
            return;
          }
        }
      }
      if (manURL == null) {
        String msg = "Can't find manual. The manual is loaded via the classpath,\n"
            + "so make sure the manual folder is in the classpath.";
        JOptionPane.showMessageDialog(this.main, msg, "Error loading manual",
            JOptionPane.ERROR_MESSAGE);
        return;
      }
      editorPane.setPage(manURL);
      JScrollPane scrollPane = new JScrollPane(editorPane);
      scrollPane.setPreferredSize(new Dimension(700, 800));
      manFrame.setContentPane(scrollPane);
      manFrame.pack();
      manFrame.setVisible(true);
      URL cvdLinkUrl = new URL(manURL.toString() + "#ugr.tools.cvd");
      HyperlinkEvent e = new HyperlinkEvent(editorPane, HyperlinkEvent.EventType.ACTIVATED,
          cvdLinkUrl);
      editorPane.fireHyperlinkUpdate(e);
    } catch (Exception e) {
      this.main.handleException(e);
    }
  }

}