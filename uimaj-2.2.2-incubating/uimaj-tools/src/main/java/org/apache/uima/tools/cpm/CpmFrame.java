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

package org.apache.uima.tools.cpm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.apache.uima.tools.images.Images;
import org.apache.uima.tools.util.gui.FileChooserBugWorkarounds;
import org.apache.uima.tools.util.gui.AboutDialog;

public class CpmFrame extends JFrame implements ActionListener {
  private static final long serialVersionUID = 756368351780483658L;

  private CpmPanel cpmPanel;

  private JMenuBar menuBar;

  private JMenuItem exitMenuItem;

  private JMenuItem aboutMenuItem;

  private JMenuItem helpMenuItem;

  private JDialog aboutDialog;

  public CpmFrame() {
    super("Collection Processing Engine Configurator");

    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      // I don't think this should ever happen, but if it does just print error and continue
      // with defalt look and feel
      System.err.println("Could not set look and feel: " + e.getMessage());
    }
    // UIManager.put("Panel.background",Color.WHITE);
    // Need to set other colors as well

    // Workaround for bug #4711700
    FileChooserBugWorkarounds.fix();

    // Set frame icon image
    try {
      this.setIconImage(Images.getImage(Images.MICROSCOPE));
      // new ImageIcon(getClass().getResource(FRAME_ICON_IMAGE)).getImage());
    } catch (IOException e) {
      System.err.println("Image could not be loaded: " + e.getMessage());
    }

    this.getContentPane().setBackground(Color.WHITE);
    this.getContentPane().setLayout(new BorderLayout());

    JLabel banner = new JLabel(Images.getImageIcon(Images.BANNER));
    this.getContentPane().add(banner, BorderLayout.NORTH);

    cpmPanel = new CpmPanel();
    this.getContentPane().add(cpmPanel, BorderLayout.CENTER);

    setJMenuBar(createMenuBar());

    aboutDialog = new AboutDialog(this, "About Collection Processing Engine Configurator");

    this.setSize(800, 600);
    this.pack();
  }

  private JMenuBar createMenuBar() {
    menuBar = new JMenuBar();

    JMenu fileMenu = new JMenu("File");
    List fileMenuItems = cpmPanel.createFileMenuItems();
    Iterator iter = fileMenuItems.iterator();
    while (iter.hasNext()) {
      fileMenu.add((JMenuItem) iter.next());
    }

    exitMenuItem = new JMenuItem("Exit");
    exitMenuItem.addActionListener(this);
    fileMenu.add(exitMenuItem);
    
    JMenu viewMenu = new JMenu("View");
    List viewMenuItems = cpmPanel.createViewMenuItems();
    iter = viewMenuItems.iterator();
    while (iter.hasNext()) {
      viewMenu.add((JMenuItem) iter.next());
    }

    JMenu helpMenu = new JMenu("Help");
    aboutMenuItem = new JMenuItem("About");
    aboutMenuItem.addActionListener(this);
    helpMenuItem = new JMenuItem("Help");
    helpMenuItem.addActionListener(this);
    helpMenu.add(aboutMenuItem);
    helpMenu.add(helpMenuItem);
    menuBar.add(fileMenu);
    menuBar.add(viewMenu);
    menuBar.add(helpMenu);

    return menuBar;
  }

  public void actionPerformed(ActionEvent ev) {
    Object source = ev.getSource();

    if (source == aboutMenuItem) {
      aboutDialog.setVisible(true);
    } else if (source == helpMenuItem) {
      JOptionPane.showMessageDialog(CpmFrame.this, CpmPanel.HELP_MESSAGE,
              "Collection Processing Engine Configurator Help", JOptionPane.PLAIN_MESSAGE);
    } else if (source == exitMenuItem) {
      this.processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }
  }

  public Dimension getPreferredSize() {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    return new Dimension(screenSize.width, (screenSize.height - 65));
  }

  /**
   * Runs the application.
   */
  public static void main(String[] args) {
    //GUI creation must be done in the event handler thread, because Swing is
    //not thread-safe.  This is particularly important for the CPE Configurator
    //because it's initialization can be quite complex (it loads the last known    
    //CPE descriptor).
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        initGUI();        
      }
    });    
  }

  /**
   * Creates and shows the GUI.
   */
  private static void initGUI() {
    try {
      final CpmFrame frame = new CpmFrame();
      frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          if (frame.cpmPanel.confirmExit())
            System.exit(0);
        }

        public void windowActivated(WindowEvent e) {
          frame.cpmPanel.checkForOutOfSyncFiles();
        }
      });
      frame.pack();
      frame.setVisible(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
