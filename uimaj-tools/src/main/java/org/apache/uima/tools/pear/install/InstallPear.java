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

package org.apache.uima.tools.pear.install;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import org.apache.uima.pear.tools.InstallationController;
import org.apache.uima.pear.tools.InstallationDescriptor;
import org.apache.uima.pear.tools.InstallationDescriptorHandler;
import org.apache.uima.pear.tools.PackageBrowser;
import org.apache.uima.pear.util.MessageRouter;
import org.apache.uima.pear.util.UIMAUtil;
import org.apache.uima.tools.cvd.CVD;
import org.apache.uima.tools.cvd.MainFrame;
import org.apache.uima.tools.images.Images;
import org.apache.uima.tools.util.gui.AboutDialog;

/**
 * This GUI is used to install a pear file locally in a directory chosen by the user and then run
 * the installed AE in CVD. <br>
 * The required field is : The pear file must be specified. The User may or may not specify the
 * installation directory. If the installation directory is not specified, the current working
 * directory is used by default.
 * 
 */
public class InstallPear extends JFrame {
  private static final long serialVersionUID = -450696952085640703L;

  /**
   * The <code>RunInstallation</code> class implements a thread that is used to run the
   * installation.
   */
  protected static class RunInstallation implements Runnable {
    private File pearFile;

    private File installationDir = null;

    /**
     * Constructor that sets a given input PEAR file and a given installation directory.
     * 
     * @param pearFile
     *          The given PEAR file.
     * @param installationDir
     *          The given installation directory.
     */
    public RunInstallation(File pearFile, File installationDir) {
      this.pearFile = pearFile;
      this.installationDir = installationDir;
    }

    /**
     * Runs the PEAR installation process. Notifies waiting threads upon completion.
     */
    public void run() {
      installPear(pearFile, installationDir);
      synchronized (this) {
        notifyAll();
      }
    }

  }

  /**
   * The <code>PEARFilter</code> class allows to filter directories, as well as '.tear' and
   * '.pear' files.
   */
  protected static class PEARFilter extends FileFilter {
    static final String TEAR_EXT = "tear";

    static final String PEAR_EXT = "pear";

    /**
     * Returns <code>true</code>, if the given input file is directory or has 'tear' or 'pear'
     * extension, <code>false</code> otherwise.
     * 
     * @return <code>true</code>, if the given input file is directory or has 'tear' or 'pear'
     *         extension, <code>false</code> otherwise.
     */
    public boolean accept(File file) {
      if (file.isDirectory())
        return true;
      String extension = getExtension(file);
      if (extension.equals(TEAR_EXT) || extension.equals(PEAR_EXT))
        return true;
      return false;
    }

    /**
     * Returns file name extension in lowercase.
     * 
     * @param f
     *          The given file.
     * @return The file name extension in lowercase.
     */
    private String getExtension(File f) {
      String s = f.getName();
      int i = s.lastIndexOf('.');
      if (i > 0 && i < s.length() - 1)
        return s.substring(i + 1).toLowerCase();
      return "";
    }

    /**
     * Returns the filter description.
     * 
     * @return The filter description.
     */
    public String getDescription() {
      return "PEAR files";
    }
  }

  // private JPanel aboutMenuItemPanel = null;

  private JTextField pearFileTextField = null;

  private JButton browseButton = null;

  private static JTextField installDirTextField = null;

  private JButton browseDirButton = null;

  private JButton installButton = null;

  private JMenuBar menuBar = null;

  private JMenuItem fileMenuItem = null;

  private JMenuItem helpMenuItem = null;

  private JMenuItem aboutMenuItem = null;

  private JMenu fileMenu = null;

  private JMenu helpMenu = null;

  // private JLabel bannerLabel = null;

  private static JButton runButton = null;

  private JButton helpButton = null;

  private static JTextArea pearConsole = null;

  private static JScrollPane jScrollPane = null;

  private static File localTearFile = null;

  private static File installationDir = null;

  private static String mainComponentId;

  private static InstallationDescriptor insdObject;

  private static String mainComponentRootPath;

  private static boolean helpExists = true;

  private static String message = null;

  private static boolean errorFlag = false;

  private static Preferences userPrefs;

  private static final String LAST_FILE_NAME_CHOOSEN_KEY = "LAST_FILE_NAME_CHOOSEN";

  private static final String LAST_DIRECTORY_CHOOSEN_KEY = "LAST_DIRECTORY_CHOOSEN";

  private static final String SET_ENV_FILE = "metadata/setenv.txt";

  // protected static final String UIMA_HOME_ENV = "UIMA_HOME";

  // protected static final String UIMA_DATAPATH_ENV = "uima.datapath";

  /**
   * Opens a dialog to select a PEAR file from the local file system.
   * 
   * @return Selected PEAR file path, or <code>null</code>, if nothing was selected.
   */
  private String selectPear() {
    userPrefs = Preferences.userNodeForPackage(this.getClass());
    JFileChooser fileChooser = new JFileChooser();
    String selectedFileName = null;
    fileChooser.addChoosableFileFilter(new PEARFilter());
    String lastFileName = (pearFileTextField.getText().length() > 0) ? pearFileTextField.getText()
            : userPrefs.get(LAST_FILE_NAME_CHOOSEN_KEY, "./");
    File directory = (lastFileName.length() > 0) ? new File(lastFileName).getParentFile()
            : new File("./");
    fileChooser.setCurrentDirectory(directory);
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    int result = fileChooser.showDialog(new JFrame(), "Select");
    if (result == JFileChooser.APPROVE_OPTION) {
      selectedFileName = fileChooser.getSelectedFile().getAbsolutePath();
      pearFileTextField.setText(selectedFileName);
      installButton.setEnabled(true);
      // save state of the window
      try {
        userPrefs.put(LAST_FILE_NAME_CHOOSEN_KEY, selectedFileName);
      } catch (NullPointerException ex) {
        pearConsole.append("NullPointerException" + ex);
      }
    } else if (result == JFileChooser.CANCEL_OPTION) {
      if (pearFileTextField != null)
        installButton.setEnabled(false);
      pearFileTextField.setText("");
      pearConsole.setText("Operation Cancelled! \n");
      return null;
    }

    return selectedFileName;
  }

  /**
   * Opens a dialog to select a directory for PEAR file installation.
   * 
   * @return Selected installation directory path, or current directory path, if nothing was
   *         selected.
   */
  private String selectDir() {
    userPrefs = Preferences.userNodeForPackage(this.getClass());
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.addChoosableFileFilter(new PEARFilter());
    String lastDirName = (installDirTextField.getText().length() > 0) ? installDirTextField
            .getText() : userPrefs.get(LAST_DIRECTORY_CHOOSEN_KEY, "./");
    String selectedDirName = null;
    File directory = (lastDirName.length() > 0) ? new File(lastDirName).getParentFile() : new File(
            "./");
    fileChooser.setCurrentDirectory(directory);
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    int result = fileChooser.showDialog(new JFrame(), "Select");
    // User selects the 'Select Button'
    if (result == JFileChooser.APPROVE_OPTION) {
      selectedDirName = fileChooser.getSelectedFile().getAbsolutePath();
      // saving the state of the window
      try {
        userPrefs.put(LAST_DIRECTORY_CHOOSEN_KEY, selectedDirName);
      } catch (NullPointerException ex) {
        pearConsole.append("NullPointerException" + ex);
      }
      installDirTextField.setText(selectedDirName);
      installButton.setEnabled(true);
    }
    // no selection was made
    else {
      installButton.setEnabled(true);
    }
    return selectedDirName;
  }

  /**
   * Method that installs the given PEAR file to the given installation directory.
   * 
   * @param localPearFile
   *          The given PEAR file path.
   * @param installationDir
   *          The given installation directory.
   */
  private static void installPear(File localPearFile, File installationDir) {
    InstallationController.setLocalMode(true);
    InstallationDescriptorHandler installationDescriptorHandler = new InstallationDescriptorHandler();
    pearConsole.setText("");
    printInConsole(false, "");
    // check input parameters
    if (localPearFile != null && !localPearFile.exists()) {
      errorFlag = true;
      message = localPearFile.getAbsolutePath() + "file not found \n";
      printInConsole(errorFlag, message);
    } else {
      if (localPearFile != null) {
        pearConsole.append("PEAR file to install is => " + localPearFile.getAbsolutePath() + "\n");
      }
    }
    /* setting current working directory by default */
    if (installationDir == null) {
      installationDir = new File("./");
    }
    pearConsole.append("Installation directory is => " + installationDir.getAbsolutePath() + "\n");

    try {
      JarFile jarFile = new JarFile(localPearFile);
      installationDescriptorHandler.parseInstallationDescriptor(jarFile);
      insdObject = installationDescriptorHandler.getInstallationDescriptor();

      if (insdObject != null)
        mainComponentId = insdObject.getMainComponentId();

      else {
        pearConsole.setForeground(new Color(0xFF0000));
        throw new FileNotFoundException("installation descriptor not found \n");
      }
      // this version does not support separate delegate components
      if (insdObject.getDelegateComponents().size() > 0) {
        throw new RuntimeException("separate delegate components not supported \n");
      }
    } catch (Exception err) {
      errorFlag = true;
      message = " terminated \n" + err.toString();
      printInConsole(errorFlag, message);
      return;
    }
    InstallationController installationController = new InstallationController(mainComponentId,
            localPearFile, installationDir);
    // adding installation controller message listener
    installationController.addMsgListener(new MessageRouter.StdChannelListener() {
      public void errMsgPosted(String errMsg) {
        printInConsole(true, errMsg);
      }

      public void outMsgPosted(String outMsg) {
        printInConsole(false, outMsg);
      }
    });
    insdObject = installationController.installComponent();
    if (insdObject == null) {
      runButton.setEnabled(false);
      /* installation failed */
      errorFlag = true;
      message = " \nInstallation of " + mainComponentId + " failed => \n "
              + installationController.getInstallationMsg();
      printInConsole(errorFlag, message);

    } else {
      try {

        /* save modified installation descriptor file */
        installationController.saveInstallationDescriptorFile();
        mainComponentRootPath = insdObject.getMainComponentRoot();
        errorFlag = false;
        message = " \nInstallation of " + mainComponentId + " completed \n";
        printInConsole(errorFlag, message);
        message = "The " + mainComponentRootPath + "/" + SET_ENV_FILE
                + " \n    file contains required " + "environment variables for this component\n";
        printInConsole(errorFlag, message);
        /* 2nd step: verification of main component installation */
        if (installationController.verifyComponent()) {
          // enable 'run' button only for AE
          File xmlDescFile = new File(insdObject.getMainComponentDesc());
          try {
            String uimaCompCtg = UIMAUtil.identifyUimaComponentCategory(xmlDescFile);
            if (UIMAUtil.ANALYSIS_ENGINE_CTG.equals(uimaCompCtg))
              runButton.setEnabled(true);
          } catch (Exception e) {
            // Ignore exceptions!
          }
          errorFlag = false;
          message = "Verification of " + mainComponentId + " completed \n";
          printInConsole(errorFlag, message);
        } else {
          runButton.setEnabled(false);
          errorFlag = true;
          message = "Verification of " + mainComponentId + " failed => \n "
                  + installationController.getVerificationMsg();
          printInConsole(errorFlag, message);
        }
      } catch (Exception exc) {
        errorFlag = true;
        message = "Error in InstallationController.main(): " + exc.toString();
        printInConsole(errorFlag, message);
      } finally {
        installationController.terminate();
      }
    }
  }

  /**
   * This method runs the installed AE in CVD (Gladis).
   * 
   * @throws IOException
   *           If any I/O exception occurred.
   */
  private void runCVD() {

    
    Runnable runCVD = new Runnable() {

      public void run() {
        try {
          // create PackageBrowser object
          PackageBrowser pkgBrowser = new PackageBrowser(new File(mainComponentRootPath));

          // get pear descriptor
          String pearDesc = pkgBrowser.getComponentPearDescPath();

          // start CVD
          MainFrame frame = CVD.createMainFrame();

          // Prevent CVD from shutting down JVM after exit
          frame.setExitOnClose(false);

          // load pear descriptor
          frame.loadAEDescriptor(new File(pearDesc));

          // run CVD
          frame.runAE(true);
        } catch (Throwable e) {
          pearConsole.append(" Error in runCVD() " + e.toString());
          StringWriter strWriter = new StringWriter();
          PrintWriter printWriter = new PrintWriter(strWriter, true);
          e.printStackTrace(printWriter);
          printWriter.flush();
          strWriter.flush();
          pearConsole.setForeground(new Color(0xFF0000));
          pearConsole.append(strWriter.toString());
        }

      }
    };
    
    Thread th = new Thread(runCVD);
    th.start();
  }

  /**
   * This method initializes the pearFile TextField.
   * 
   * @return The initialized pearFile TextField.
   */
  private JTextField getPearFileTextField() {
    if (pearFileTextField == null) {
      pearFileTextField = new JTextField();
      pearFileTextField.setBounds(83, 40, 492, 20);
      // hazel's change
      pearFileTextField.setLayout(new BorderLayout());
      pearFileTextField.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent e) {
          if (e.getSource() instanceof JTextField) {
            pearFileTextField = (JTextField) e.getSource();
            installButton.setEnabled(true);
            runButton.setEnabled(false);
            if (pearFileTextField != null) {
              pearConsole.setText("");
              printInConsole(false, "");
              pearConsole.append(pearFileTextField.getText() + "\n");
            }

          }
        }
      });

      this.pearFileTextField.getDocument().addDocumentListener(new DocumentListener() {
        public void changedUpdate(DocumentEvent e) {
          // do nothing
        }

        public void insertUpdate(DocumentEvent e) {
          InstallPear.runButton.setEnabled(false);
          String inputPear = InstallPear.this.pearFileTextField.getText();
          if (inputPear.length() > 0) {
            InstallPear.this.installButton.setEnabled(true);
          }
        }

        public void removeUpdate(DocumentEvent e) {
          InstallPear.runButton.setEnabled(false);
          String inputPear = InstallPear.this.pearFileTextField.getText();
          if (inputPear.length() == 0) {
            InstallPear.this.installButton.setEnabled(false);
          }
        }
      });
    }
    return pearFileTextField;
  }

  /**
   * This method initializes the Browse Button.
   * 
   * @return The initialized Browse Button.
   */
  private JButton getbrowseButton() {
    if (browseButton == null) {
      browseButton = new JButton();
      browseButton.setBounds(579, 40, 114, 20);
      browseButton.setText("Browse...");
      browseButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
      browseButton.setMnemonic('b');
      browseButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent e) {
          String pear = null;
          if ((e.getActionCommand().equals("Browse..."))) {
            runButton.setEnabled(false);
            pear = selectPear();
            if (pear == null)
              return;
          }
        }
      });
    }
    return browseButton;
  }

  /**
   * This method initializes installDirTextField.
   * 
   * @return The initialized installDirTextField.
   */
  private JTextField getInstallDirTextField() {
    if (installDirTextField == null) {
      installDirTextField = new JTextField();
      installDirTextField.setBounds(83, 80, 492, 20);
      installDirTextField.isEditable();
      // handling text input
      installDirTextField.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent e) {
          if (e.getSource() instanceof JTextField) {
            runButton.setEnabled(false);
            installDirTextField = (JTextField) e.getSource();
            if (installDirTextField != null) {
              pearConsole.append(installDirTextField.getText() + "\n");
            }
          }
        }
      });

      installDirTextField.getDocument().addDocumentListener(new DocumentListener() {
        public void changedUpdate(DocumentEvent e) {
          // do nothing
        }

        public void insertUpdate(DocumentEvent e) {
          runButton.setEnabled(false);
        }

        public void removeUpdate(DocumentEvent e) {
          runButton.setEnabled(false);
        }
      });

    }
    return installDirTextField;
  }

  /**
   * This method initializes the browseDir Button.
   * 
   * @return The initialized browseDir Button.
   */
  private JButton getBrowseDirButton() {
    if (browseDirButton == null) {
      browseDirButton = new JButton();
      browseDirButton.setBounds(579, 80, 114, 20);
      browseDirButton.setText("Browse Dir...");
      browseDirButton.setEnabled(false);
      browseDirButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
      browseDirButton.setMnemonic('d');
      browseDirButton.setEnabled(true);
      browseDirButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent e) {
          String dir = null;
          File installationDir = null;
          runButton.setEnabled(false);
          if (e.getActionCommand().equals("Browse Dir...")) {
            dir = selectDir();
            if (dir == null) {
              installationDir = new File("./");
              installDirTextField.setText(installationDir.getAbsolutePath());
            } else {
              installDirTextField.setText(dir);
            }
          }
        }
      });
    }
    return browseDirButton;
  }

  /**
   * This method initializes the Install Button.
   * 
   * @return The initialized Install Button.
   */
  private JButton getInstallButton() {
    if (installButton == null) {
      installButton = new JButton();
      installButton.setBounds(100, 120, 108, 24);
      installButton.setText("Install");
      installButton.setEnabled(false);
      installButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
      installButton.setMnemonic('i');
      installButton.setRolloverEnabled(true);
      // Handling mouse click event
      installButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent e) {
          if (e.getActionCommand().equals("Install")) {
            localTearFile = new File(pearFileTextField.getText());
            installationDir = new File(installDirTextField.getText());
            RunInstallation installPear = new RunInstallation(localTearFile, installationDir);
            Thread thread = new Thread(installPear);
            thread.start();
            installButton.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            synchronized (installPear) {
              try {
                installPear.wait(500000);
              } catch (InterruptedException ex) {
                errorFlag = true;
                message = "InterruptedException " + ex;
                printInConsole(errorFlag, message);
              }
            }
            installButton.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
          }
        }

      });
    }
    return installButton;
  }

  /**
   * This method initializes 'Run your AE in CVD' Button.
   * 
   * @return The initialized 'Run your AE in CVD' Button.
   */
  private JButton getRunButton() {
    if (runButton == null) {
      runButton = new JButton();
      runButton.setBounds(255, 120, 240, 24);
      runButton.setText("Run your AE in the CAS Visual Debugger");
      runButton.setEnabled(false);
      runButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
      runButton.setMnemonic('r');
      runButton.setRolloverEnabled(true);
      runButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent e) {
          if (e.getActionCommand().equals("Run your AE in the CAS Visual Debugger")) {
            installButton.setEnabled(false);
            runCVD();
          }
        }
      });
    }
    return runButton;
  }

  /**
   * This method initializes pearConsole.
   * 
   * @return The initialized pearConsole.
   */
  private JTextArea getPearConsole() {
    if (pearConsole == null) {
      pearConsole = new JTextArea();
      pearConsole.setEditable(false);
      pearConsole.setForeground(new Color(0x0066ff));
      pearConsole.setLineWrap(true);
      pearConsole.setWrapStyleWord(true);

    }
    return pearConsole;
  }

  /**
   * Prints messages and set foreground color in the console according to a given errorFlag.
   * 
   * @param errorFlag
   *          The given error flag.
   * @param message
   *          The given message to print.
   */
  private static void printInConsole(boolean errorFlag, String message) {
    if (errorFlag) {
      pearConsole.setForeground(new Color(0xFF0000));
      pearConsole.setText(message);
    } else {
      pearConsole.setForeground(new Color(0x0066ff));
      pearConsole.append(message);
    }
  }

  /**
   * This method initializes the Scroll Pane.
   * 
   * @return The initialized Scroll Pane.
   */
  private JScrollPane getJScrollPane() {
    if (jScrollPane == null) {
      jScrollPane = new JScrollPane(getPearConsole(),
              ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
      jScrollPane.setBounds(10, 150, 708, 352);
      jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
      jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    }
    return jScrollPane;
  }

  /**
   * This method initializes the help Button.
   * 
   * @return The initialized help Button.
   */
  private JButton getHelpButton() {
    if (helpButton == null) {
      helpButton = new JButton();
      helpButton.setBounds(540, 120, 108, 24);
      helpButton.setText("Help");
      helpButton.setEnabled(helpExists);
      helpButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
      helpButton.setMnemonic('h');
      helpButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent e) {
          if (e.getActionCommand() == "Help") {
            try {
              URL helpFileUrl = getClass().getResource("piHelp.html");
              if (helpFileUrl == null) {
                String msg = "PEAR Installer help file does not exist.";
                JOptionPane.showMessageDialog(InstallPear.this, msg, "Error showing help",
                        JOptionPane.ERROR_MESSAGE);
                return;
              }
              JFrame manFrame = new JFrame("PEAR Installer Help");
              JEditorPane editorPane = new JEditorPane();
              editorPane.setEditable(false);
              editorPane.setPage(helpFileUrl);
              JScrollPane scrollPane = new JScrollPane(editorPane);
              scrollPane.setPreferredSize(new Dimension(700, 800));
              manFrame.setContentPane(scrollPane);
              manFrame.pack();
              manFrame.setVisible(true);
            } catch (Exception ex) {
              JOptionPane.showMessageDialog(InstallPear.this, ex.getMessage(),
                      "Error showing help", JOptionPane.ERROR_MESSAGE);
            }
          }
        }
      });
    }
    return helpButton;
  }

  /**
   * Method to create and display the frame.
   */
  private static void createAndShowGUI() {
    InstallPear installPear = new InstallPear();
    installPear.setDefaultCloseOperation(EXIT_ON_CLOSE);
    installPear.setVisible(true);
  }

  /**
   * Starts the GUI application.
   * 
   * @param args
   *          None.
   */
  public static void main(String[] args) {

    // try {
    // String UIMA_HOME = System.getProperty("uima.home");
    // if (UIMA_HOME == null)
    // throw new RuntimeException("-Duima.home not defined");
    // System.setProperty(UIMA_HOME_ENV, UIMA_HOME);
    // // load main properties and replace %UIMA_HOME%
    // } catch (Exception exc) {
    // System.err.println("Error in InstallPear.main():" + exc.toString());
    // exc.printStackTrace(System.err);
    // System.exit(0);
    // }
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        createAndShowGUI();
      }
    });
  }

  /**
   * This is the default constructor.
   */
  public InstallPear() {
    super();
    initialize();
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception exception) {
      System.err.println("Could not set look and feel: " + exception.getMessage());
    }
    try {
      setIconImage(ImageIO.read(getClass().getResource(Images.MICROSCOPE)));
    } catch (IOException ioexception) {
      System.err.println("Image could not be loaded: " + ioexception.getMessage());
    }
    menuBar = new JMenuBar();
    setJMenuBar(menuBar);
    fileMenu = new JMenu("File");
    fileMenu.setMnemonic('f');
    fileMenuItem = new JMenuItem("Exit");
    fileMenuItem.setMnemonic('e');
    menuBar.add(fileMenu);
    fileMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.exit(0);
      }
    });
    fileMenu.add(fileMenuItem);

    // the help menu and items
    helpMenu = new JMenu("Help");
    helpMenu.setMnemonic('h');
    aboutMenuItem = new JMenuItem("About");
    aboutMenuItem.setMnemonic('a');
    helpMenuItem = new JMenuItem("Help");
    helpMenuItem.setMnemonic('h');
    menuBar.add(helpMenu);
    helpMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand() == "Help") {
          try {
            URL helpFileUrl = getClass().getResource("piHelp.html");
            if (helpFileUrl == null) {
              String msg = "PEAR Installer help file does not exist.";
              JOptionPane.showMessageDialog(InstallPear.this, msg, "Error showing help",
                      JOptionPane.ERROR_MESSAGE);
              return;
            }
            JFrame manFrame = new JFrame("PEAR Installer Help");
            JEditorPane editorPane = new JEditorPane();
            editorPane.setEditable(false);
            editorPane.setPage(helpFileUrl);
            JScrollPane scrollPane = new JScrollPane(editorPane);
            scrollPane.setPreferredSize(new Dimension(700, 800));
            manFrame.setContentPane(scrollPane);
            manFrame.pack();
            manFrame.setVisible(true);
          } catch (Exception ex) {
            JOptionPane.showMessageDialog(InstallPear.this, ex.getMessage(), "Error showing help",
                    JOptionPane.ERROR_MESSAGE);
          }
        }
      }
    });

    aboutMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand() == "About") {
          AboutDialog dialog = new AboutDialog(InstallPear.this, "About PEAR Installer");
          dialog.setVisible(true);
        }
      }
    });
    helpMenu.add(helpMenuItem);
    helpMenu.add(aboutMenuItem);

    /*
     * Initialize Main Pane and add the initialized components
     */
    JLabel installDirLabel = new JLabel();
    JLabel pearFileLabel = new JLabel();
    JPanel mainPane = new JPanel();
    mainPane.setLayout(null);
    pearFileLabel.setBounds(83, 20, 126, 20);
    pearFileLabel.setText("PEAR File:");
    installDirLabel.setBounds(83, 60, 126, 20);
    installDirLabel.setText("Installation Directory:");
    mainPane.add(pearFileLabel, null);
    mainPane.add(getPearFileTextField(), null);
    mainPane.add(getbrowseButton(), null);
    mainPane.add(installDirLabel, null);
    mainPane.add(getInstallDirTextField(), null);
    mainPane.add(getBrowseDirButton(), null);
    mainPane.add(getInstallButton(), null);
    mainPane.add(getRunButton(), null);
    mainPane.add(getHelpButton(), null);
    mainPane.add(getPearConsole(), null);
    mainPane.add(getJScrollPane(), null);

    /*
     * Add main pane and UIMA banner to content pane
     */
    Container contentPanel = getContentPane();
    contentPanel.setBackground(Color.WHITE);
    JLabel banner = new JLabel(Images.getImageIcon(Images.BANNER));
    contentPanel.add(banner, BorderLayout.NORTH);
    contentPanel.add(mainPane, BorderLayout.CENTER);
  }

  /**
   * This method initializes the frame.
   */
  private void initialize() {
    this.setTitle("Local PEAR Installation, Verification and Testing");
    this.setSize(735, 620);
    this.setResizable(false);
    // center the frame on the screen
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension GUISize = this.getSize();
    int centerPosX = (screenSize.width - GUISize.width) / 2;
    int centerPosY = (screenSize.width - GUISize.width) / 2;
    setLocation(centerPosX, centerPosY);
  }

}
