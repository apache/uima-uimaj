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

package org.apache.uima.tools.jcasgen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.uima.tools.images.Images;
import org.apache.uima.tools.util.gui.AboutDialog;

public class GUI extends JFrame {
  private static final long serialVersionUID = 1L;

  private AboutDialog aboutDialog;

  final static String NL = System.getProperties().getProperty("line.separator");

  static GUI theGUI;

  final Jg jg;

  class G extends JPanel implements ActionListener {
    private static final long serialVersionUID = 1L;

    final GUI gui;

    JLabel lbLabel6;

    JLabel lbLabel1;

    JButton btInputFileBrowse;

    JButton btOutDirBrowse;

    JButton btGenXmlBrowse;

    JLabel lbLabel0;

    JLabel lbLabel9;

    JTextArea tfInputFileName;

    JTextArea tfOutDirName;

    JTextArea tfGenXMLname;

    JLabel lbLabel11;

    JLabel lbLabel12;

    JLabel lbLabel13;

    JButton btGo;

    JTextArea taStatus;

    JScrollPane areaScrollPane;

    JLabel lbLabel15;

    JLabel lbLabel16;

    JLabel lbLabel17;

    JLabel lbLabel10;

    JLabel lbResult;

    public G(GUI gui) {
      super();
      this.gui = gui;

      GridBagLayout gbG = new GridBagLayout();
      setLayout(gbG);

      GridBagConstraints gbcG = new GridBagConstraints();
      // global, reused values
      gbcG.insets = new Insets(4, 4, 4, 4);
      gbcG.gridwidth = 1;
      gbcG.gridheight = 1;
      gbcG.weightx = 0;
      gbcG.weighty = 0;
      gbcG.fill = GridBagConstraints.NONE;
      gbcG.anchor = GridBagConstraints.CENTER;

      // layout:
      // 0 1 2
      // 0 labels i/o text area buttons

      // 1 title old
      // 2 inFile txt browse
      // 3 out dir txt browse
      // 4 gen xml txt
      // 5 status
      // 6 go-button txt-status result

      lbLabel17 = new JLabel("Welcome to the JCasGen tool. You can drag corners to resize.");
      lbLabel17.setRequestFocusEnabled(false);
      lbLabel17.setToolTipText(null);
      lbLabel17.setVerifyInputWhenFocusTarget(false);
      gbcG.gridx = 1;
      gbcG.gridy = 0;
      gbG.setConstraints(lbLabel17, gbcG);
      add(lbLabel17);

      lbLabel0 = new JLabel("Input File:");
      gbcG.gridx = 0;
      gbcG.gridy = 2;
      gbG.setConstraints(lbLabel0, gbcG);
      add(lbLabel0);

      lbLabel1 = new JLabel("Output Directory:");
      gbcG.gridy = 3;
      gbG.setConstraints(lbLabel1, gbcG);
      add(lbLabel1);

      btInputFileBrowse = new JButton("Browse");
      btInputFileBrowse.addActionListener(this);
      gbcG.gridx = 3;
      gbcG.gridy = 2;
      gbcG.anchor = GridBagConstraints.WEST;
      gbG.setConstraints(btInputFileBrowse, gbcG);
      add(btInputFileBrowse);

      btOutDirBrowse = new JButton("Browse");
      btOutDirBrowse.addActionListener(this);
      gbcG.gridy = 3;
      gbG.setConstraints(btOutDirBrowse, gbcG);
      add(btOutDirBrowse);

      tfInputFileName = new JTextArea();
      gbcG.gridx = 1;
      gbcG.gridy = 2;
      gbcG.fill = GridBagConstraints.BOTH;
      gbcG.weightx = 1;
      gbcG.weighty = 1;
      gbcG.anchor = GridBagConstraints.WEST;
      gbG.setConstraints(tfInputFileName, gbcG);
      add(tfInputFileName);

      tfOutDirName = new JTextArea();
      gbcG.gridy = 3;
      gbG.setConstraints(tfOutDirName, gbcG);
      add(tfOutDirName);

      lbLabel16 = new JLabel("Status");
      gbcG.gridy = 5;
      gbcG.fill = GridBagConstraints.NONE;
      gbcG.weightx = 1;
      gbcG.weighty = 0;
      gbcG.anchor = GridBagConstraints.CENTER;
      gbG.setConstraints(lbLabel16, gbcG);
      add(lbLabel16);

      taStatus = new JTextArea();
      gbcG.gridy = 6;
      gbcG.weighty = 5; // most weight goes here
      gbcG.fill = GridBagConstraints.BOTH;
      gbcG.anchor = GridBagConstraints.NORTHWEST;
      areaScrollPane = new JScrollPane(taStatus);
      areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
      gbG.setConstraints(areaScrollPane, gbcG);
      add(areaScrollPane);

      btGo = new JButton("Go");
      btGo.addActionListener(this);
      gbcG.gridx = 0;
      gbcG.gridy = 6;
      gbcG.fill = GridBagConstraints.NONE;
      gbcG.weightx = 0; // was 1
      gbcG.weighty = 0;
      gbcG.anchor = GridBagConstraints.CENTER;
      gbG.setConstraints(btGo, gbcG);
      add(btGo);

      lbResult = new JLabel(" ");
      gbcG.gridx = 3;
      gbcG.gridy = 6;
      gbcG.fill = GridBagConstraints.BOTH;
      gbcG.anchor = GridBagConstraints.NORTH;
      gbG.setConstraints(lbResult, gbcG);
      add(lbResult);
    }

    public void actionPerformed(ActionEvent e) {
      if (e.getSource() == btInputFileBrowse) {
        browseFile(tfInputFileName); // Action for btInputFileBrowse
      }
      if (e.getSource() == btOutDirBrowse) {
        browseDir(tfOutDirName); // Action for btOutDirBrowse
      }
      if (e.getSource() == btGenXmlBrowse) {
        browseFile(tfGenXMLname); // Action for btGenXmlBrowse
      }
      if (e.getSource() == btGo) {
        lbResult.setText("Working");
        go(); // Action for btGo
      }
    }

    void browseFile(JTextArea f) {
      String startingDir = f.getText();
      if (startingDir.length() == 0) {
        // default to user.dir
        startingDir = System.getProperty("user.dir");
      }
      JFileChooser c = new JFileChooser(startingDir);
      int returnVal = c.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        try {
          f.setText(c.getSelectedFile().getCanonicalPath());
          Prefs.set(gui);
        } catch (Exception e) { // do nothing
        }
      }
    }

    void browseDir(JTextArea f) {
      String startingDir = f.getText();
      if (startingDir.length() == 0) {
        // default to user.dir
        startingDir = System.getProperty("user.dir");
      }
      JFileChooser c = new JFileChooser(startingDir);
      c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      int returnVal = c.showOpenDialog(gui);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        try {
          f.setText(c.getSelectedFile().getCanonicalPath());
          Prefs.set(gui);
        } catch (Exception e) { // do nothing
        }
      }
    }

    void go() {
      final String outDirName = tfOutDirName.getText();
      final String inFileName = tfInputFileName.getText();
      Runnable r = new Runnable() {
        public void run() {
          jg.main0(new String[] { "-jcasgeninput", inFileName, "-jcasgenoutput", outDirName },
                  jg.merger, new GuiProgressMonitor(), new GuiErrorImpl());
        }
      };
      new Thread(r).start();
    }

    String showInStatus(String message) {
      taStatus.setText(taStatus.getText() + message + NL);
      areaScrollPane.getVerticalScrollBar().setValue(
              areaScrollPane.getVerticalScrollBar().getMaximum());
      gui.repaint();
      return message;
    }
  }

  G pnG;

  // for testing only
  public static void main(String args[]) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ClassNotFoundException e) { // do nothing
    } catch (InstantiationException e) { // do nothing
    } catch (IllegalAccessException e) { // do nothing
    } catch (UnsupportedLookAndFeelException e) { // do nothing
    }
    theGUI = new GUI(null);
  }

  public GUI(Jg jg) {
    super("JCasGen");
    theGUI = this;
    this.jg = jg;

    pnG = new G(this);

    setDefaultCloseOperation(EXIT_ON_CLOSE);

    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      // I don't think this should ever happen, but if it does just print error and continue
      // with defalt look and feel
      System.err.println("Could not set look and feel: " + e.getMessage());
    }

    // Set frame icon image
    try {
      this.setIconImage(Images.getImage(Images.MICROSCOPE));
    } catch (IOException e) {
      System.err.println("Image could not be loaded: " + e.getMessage());
    }

    this.getContentPane().setBackground(Color.WHITE);
    this.getContentPane().setLayout(new BorderLayout());

    JLabel banner = new JLabel(Images.getImageIcon(Images.BANNER));
    this.getContentPane().add(banner, BorderLayout.NORTH);

    this.getContentPane().add(pnG, BorderLayout.CENTER);

    aboutDialog = new AboutDialog(this, "About JCasGen");

    setJMenuBar(createMenuBar());

    pack();
    // show();
  }

  private JMenuBar createMenuBar() {
    JMenuBar menuBar = new JMenuBar();

    JMenu fileMenu = new JMenu("File");

    JMenuItem exitMenuItem = new JMenuItem("Exit");
    exitMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        GUI.this.processWindowEvent(new WindowEvent(GUI.this, WindowEvent.WINDOW_CLOSING));
      }
    });

    fileMenu.add(exitMenuItem);

    JMenu helpMenu = new JMenu("Help");
    JMenuItem aboutMenuItem = new JMenuItem("About");
    aboutMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        aboutDialog.setVisible(true);
      }
    });

    helpMenu.add(aboutMenuItem);
    menuBar.add(fileMenu);
    menuBar.add(helpMenu);

    return menuBar;
  }
}
