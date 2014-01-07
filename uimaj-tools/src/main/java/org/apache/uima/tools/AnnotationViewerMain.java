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

package org.apache.uima.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.UIManager;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.tools.docanalyzer.PrefsMediator;
import org.apache.uima.tools.docanalyzer.AnnotationViewerDialog;
import org.apache.uima.tools.images.Images;
import org.apache.uima.tools.util.gui.Caption;
import org.apache.uima.tools.util.gui.FileSelector;
import org.apache.uima.tools.util.gui.AboutDialog;
import org.apache.uima.tools.util.gui.SpringUtilities;
import org.apache.uima.tools.util.htmlview.AnnotationViewGenerator;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

/**
 * Main Annotation Viewer GUI. Allows user to choose directory of XCAS or XMI files, then
 * launches the AnnotationViewerDialog.
 * 
 * 
 */
public class AnnotationViewerMain extends JFrame {
  private static final long serialVersionUID = -3201723535833938833L;

  private static final String HELP_MESSAGE = "Instructions for using Annotation Viewer:\n\n"
          + "1) In the \"Input Directory\" field, either type or use the browse\n"
          + "button to select a directory containing the analyzed documents\n "
          + "(in XMI or XCAS format) that you want to view.\n\n"
          + "2) In the \"TypeSystem or AE Descriptor File\" field, either type or use the browse\n"
          + "button to select the TypeSystem or AE descriptor for the AE that generated the\n"
          + "XMI or XCAS files.  (This is needed for type system infornation only.\n"
          + "Analysis will not be redone.)\n\n"
          + "3) Click the \"View\" button at the buttom of the window.\n\n"
          + "A list of the analyzed documents will be displayed.\n\n\n"
          + "4) Select the view type -- either the Java annotation viewer, HTML,\n"
          + "or XML.  The Java annotation viewer is recommended.\n\n"
          + "5) Double-click on a document to view it.\n";

  private File uimaHomeDir;

  private FileSelector inputFileSelector;

  private FileSelector taeDescriptorFileSelector;

  private JButton viewButton;

  private JDialog aboutDialog;

  /** Stores user preferences */
  private Preferences prefs = Preferences.userRoot().node("org/apache/uima/tools/AnnotationViewer");

  /**
   * Constructor. Sets up the GUI.
   */
  public AnnotationViewerMain() {
    super("Annotation Viewer");

    // set UIMA home dir
    uimaHomeDir = new File(System.getProperty("uima.home", "C:/Program Files/apache-uima"));

    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      // I don't think this should ever happen, but if it does just print error and continue
      // with defalt look and feel
      System.err.println("Could not set look and feel: " + e.getMessage());
    }
    // UIManager.put("Panel.background",Color.WHITE);
    // Need to set other colors as well

    // Set frame icon image
    try {
      this.setIconImage(Images.getImage(Images.MICROSCOPE));
      // new ImageIcon(getClass().getResource(FRAME_ICON_IMAGE)).getImage());
    } catch (IOException e) {
      System.err.println("Image could not be loaded: " + e.getMessage());
    }

    this.getContentPane().setBackground(Color.WHITE);

    // create about dialog
    aboutDialog = new AboutDialog(this, "About Annotation Viewer");

    // Create Menu Bar
    JMenuBar menuBar = new JMenuBar();
    setJMenuBar(menuBar);

    JMenu fileMenu = new JMenu("File");
    JMenu helpMenu = new JMenu("Help");

    // Menu Items
    JMenuItem aboutMenuItem = new JMenuItem("About");
    JMenuItem helpMenuItem = new JMenuItem("Help");
    JMenuItem exitMenuItem = new JMenuItem("Exit");

    fileMenu.add(exitMenuItem);
    helpMenu.add(aboutMenuItem);
    helpMenu.add(helpMenuItem);
    menuBar.add(fileMenu);
    menuBar.add(helpMenu);

    // Labels to identify the text fields
    final Caption labelInputDir = new Caption("Input Directory: ");
    final Caption labelStyleMapFile = new Caption("TypeSystem or AE Descriptor File: ");

    JPanel controlPanel = new JPanel();
    controlPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    controlPanel.setLayout(new SpringLayout());

    // Once we add components to controlPanel, we'll
    // call SpringUtilities::makeCompactGrid on it.

    // controlPanel.setLayout(new GridLayout(4, 2, 8, 4));

    // Set default values for input fields
    File inputDir = new File(uimaHomeDir, "examples/data/processed");
    inputFileSelector = new FileSelector("", "Input Directory", JFileChooser.DIRECTORIES_ONLY,
            inputDir);
    inputFileSelector.setSelected(inputDir.getAbsolutePath());

    taeDescriptorFileSelector = new FileSelector("", "TAE Descriptor File",
            JFileChooser.FILES_ONLY, uimaHomeDir);

    File descriptorFile = new File(uimaHomeDir,
            "examples/descriptors/analysis_engine/PersonTitleAnnotator.xml");
    taeDescriptorFileSelector.setSelected(descriptorFile.getAbsolutePath());

    controlPanel.add(labelInputDir);
    controlPanel.add(inputFileSelector);
    controlPanel.add(labelStyleMapFile);
    controlPanel.add(taeDescriptorFileSelector);

    SpringUtilities.makeCompactGrid(controlPanel, 2, 2, // rows, cols
            4, 4, // initX, initY
            4, 4); // xPad, yPad

    // Event Handlling of "Exit" Menu Item
    exitMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        savePreferences();
        System.exit(0);
      }
    });

    // Event Handlling of "About" Menu Item
    aboutMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        aboutDialog.setVisible(true);
      }
    });

    // Event Handlling of "Help" Menu Item
    helpMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        JOptionPane.showMessageDialog(AnnotationViewerMain.this, HELP_MESSAGE,
                "Annotation Viewer Help", JOptionPane.PLAIN_MESSAGE);
      }
    });

    // Add the panels to the frame
    Container contentPanel = getContentPane();
    contentPanel.add(controlPanel, BorderLayout.CENTER);

    // add banner
    JLabel banner = new JLabel(Images.getImageIcon(Images.BANNER));
    contentPanel.add(banner, BorderLayout.NORTH);

    // Add the view Button to run TAE
    viewButton = new JButton("View");

    // Add the view button to another panel
    JPanel lowerButtonsPanel = new JPanel();
    lowerButtonsPanel.add(viewButton);

    contentPanel.add(lowerButtonsPanel, BorderLayout.SOUTH);
    setContentPane(contentPanel);

    // Event Handling of view Button
    viewButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ee) {
        try {
          viewDocuments();
        } catch (Exception e) {
          displayError(e);
        }
      }
    });

    // load user preferences
    if (System.getProperty("uima.noprefs") == null) {
      restorePreferences();
    }
  }

  public void viewDocuments() throws InvalidXMLException, IOException,
          ResourceInitializationException {
    File descriptorFile = new File(taeDescriptorFileSelector.getSelected());
    if (!descriptorFile.exists() || descriptorFile.isDirectory()) {
      displayError("Descriptor File \"" + descriptorFile.getPath() + "\" does not exist.");
      return;
    }
    File inputDir = new File(inputFileSelector.getSelected());
    if (!inputDir.exists() || !inputDir.isDirectory()) {
      displayError("Input Directory \"" + inputDir.getPath() + "\" does not exist.");
      return;
    }

    // parse descriptor. Could be either AE or TypeSystem descriptor
    Object descriptor = UIMAFramework.getXMLParser().parse(new XMLInputSource(descriptorFile));
    // instantiate CAS to get type system. Also build style map file if there is none.
    CAS cas;
    File styleMapFile;
    if (descriptor instanceof AnalysisEngineDescription) {
      cas = CasCreationUtils.createCas((AnalysisEngineDescription) descriptor);
      styleMapFile = getStyleMapFile((AnalysisEngineDescription) descriptor, descriptorFile
              .getPath());
    } else if (descriptor instanceof TypeSystemDescription) {
      TypeSystemDescription tsDesc = (TypeSystemDescription) descriptor;
      tsDesc.resolveImports();
      cas = CasCreationUtils.createCas(tsDesc, null, new FsIndexDescription[0]);
      styleMapFile = getStyleMapFile((TypeSystemDescription) descriptor, descriptorFile.getPath());
    } else {
      displayError("Invalid Descriptor File \"" + descriptorFile.getPath() + "\""
              + "Must be either an AnalysisEngine or TypeSystem descriptor.");
      return;
    }

    // create Annotation Viewer Main Panel
    PrefsMediator prefsMed = new PrefsMediator();
    // set OUTPUT dir in PrefsMediator, not input dir.
    // PrefsMediator is also used in DocumentAnalyzer, where the
    // output dir is the directory containing XCAS files.
    prefsMed.setOutputDir(inputDir.toString());
    AnnotationViewerDialog viewerDialog = new AnnotationViewerDialog(this,
            "Analyzed Documents", prefsMed, styleMapFile, null, cas.getTypeSystem(), null, false,
            cas);
    viewerDialog.pack();
    viewerDialog.setModal(true);
    viewerDialog.setVisible(true);
  }

  /**
   * @param tad 
   * @param descFileName
   * @return the style map file
   * @throws IOException -
   */
  private File getStyleMapFile(AnalysisEngineDescription tad, String descFileName)
          throws IOException {
    File styleMapFile = getStyleMapFileName(descFileName);
    if (!styleMapFile.exists()) {
      // generate default style map
      String xml = AnnotationViewGenerator.autoGenerateStyleMap(tad.getAnalysisEngineMetaData());

      PrintWriter writer;
      writer = new PrintWriter(new BufferedWriter(new FileWriter(styleMapFile)));
      writer.println(xml);
      writer.close();
    }
    return styleMapFile;
  }

  /**
   * @param tsd
   * @param descFileName
   * @return the style map file
   * @throws IOException -
   */
  private File getStyleMapFile(TypeSystemDescription tsd, String descFileName) throws IOException {
    File styleMapFile = getStyleMapFileName(descFileName);
    if (!styleMapFile.exists()) {
      // generate default style map
      String xml = AnnotationViewGenerator.autoGenerateStyleMap(tsd);

      PrintWriter writer;
      writer = new PrintWriter(new BufferedWriter(new FileWriter(styleMapFile)));
      writer.println(xml);
      writer.close();
    }
    return styleMapFile;
  }

  /**
   * Gets the name of the style map file for the given AE or TypeSystem descriptor filename.
   */
  public File getStyleMapFileName(String aDescriptorFileName) {
    String baseName;
    int index = aDescriptorFileName.lastIndexOf(".");
    if (index > 0) {
      baseName = aDescriptorFileName.substring(0, index);
    } else {
      baseName = aDescriptorFileName;
    }
    return new File(baseName + "StyleMap.xml");
  }

  public static void main(String[] args) {
    final AnnotationViewerMain frame = new AnnotationViewerMain();

    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        frame.savePreferences();
        System.exit(0);
      }
    });
    frame.pack();
    frame.setVisible(true);
  }

  /**
   * Save user's preferences using Java's Preference API.
   */
  public void savePreferences() {
    prefs.put("inDir", inputFileSelector.getSelected());
    prefs.put("taeDescriptorFile", taeDescriptorFileSelector.getSelected());
  }

  /**
   * Reset GUI to preferences last saved via {@link #savePreferences}.
   */
  public void restorePreferences() {
    // figure defaults
    File defaultInputDir = new File(uimaHomeDir, "examples/data/processed");
    File defaultTaeDescriptorFile = new File(uimaHomeDir,
            "examples/descriptors/analysis_engine/PersonTitleAnnotator.xml");

    // restore preferences
    inputFileSelector.setSelected(prefs.get("inDir", defaultInputDir.toString()));
    taeDescriptorFileSelector.setSelected(prefs.get("taeDescriptorFile", defaultTaeDescriptorFile
            .toString()));
  }

  /**
   * Displays an error message to the user.
   * 
   * @param aErrorString
   *          error message to display
   */
  public void displayError(String aErrorString) {
    // word-wrap long mesages
    StringBuffer buf = new StringBuffer(aErrorString.length());
    final int CHARS_PER_LINE = 80;
    int charCount = 0;
    StringTokenizer tokenizer = new StringTokenizer(aErrorString, " \n", true);

    while (tokenizer.hasMoreTokens()) {
      String tok = tokenizer.nextToken();

      if (tok.equals("\n")) {
        buf.append("\n");
        charCount = 0;
      } else if ((charCount > 0) && ((charCount + tok.length()) > CHARS_PER_LINE)) {
        buf.append("\n").append(tok);
        charCount = tok.length();
      } else {
        buf.append(tok);
        charCount += tok.length();
      }
    }

    JOptionPane.showMessageDialog(AnnotationViewerMain.this, buf.toString(), "Error",
            JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Displays an error message to the user.
   * 
   * @param aThrowable
   *          Throwable whose message is to be displayed.
   */
  public void displayError(Throwable aThrowable) {
    aThrowable.printStackTrace();

    String message = aThrowable.toString();

    // For UIMAExceptions or UIMARuntimeExceptions, add cause info.
    // We have to go through this nonsense to support Java 1.3.
    // In 1.4 all exceptions can have a cause, so this wouldn't involve
    // all of this typecasting.
    while ((aThrowable instanceof UIMAException) || (aThrowable instanceof UIMARuntimeException)) {
      if (aThrowable instanceof UIMAException) {
        aThrowable = ((UIMAException) aThrowable).getCause();
      } else if (aThrowable instanceof UIMARuntimeException) {
        aThrowable = ((UIMARuntimeException) aThrowable).getCause();
      }

      if (aThrowable != null) {
        message += ("\nCausedBy: " + aThrowable.toString());
      }
    }

    displayError(message);
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see java.awt.Component#getPreferredSize()
   */
  public Dimension getPreferredSize() {
    return new Dimension(640, 200);
  }  
}
