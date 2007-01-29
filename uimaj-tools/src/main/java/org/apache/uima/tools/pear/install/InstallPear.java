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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
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
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import org.apache.uima.internal.util.SystemEnvReader;
import org.apache.uima.pear.tools.InstallationController;
import org.apache.uima.pear.tools.InstallationDescriptor;
import org.apache.uima.pear.tools.InstallationDescriptorHandler;
import org.apache.uima.pear.util.MessageRouter;
import org.apache.uima.pear.util.ProcessUtil;
import org.apache.uima.pear.util.UIMAUtil;
import org.apache.uima.tools.images.Images;

/**
 * This GUI is used to install a pear file locally in a directory chosen by the user and then run
 * the installed AE in Gladis. <br />
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

  private JPanel aboutMenuItemPanel = null;

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

  private JLabel bannerLabel = null;

  private static JButton runButton = null;

  private JButton helpButton = null;

  private static JTextArea pearConsole = null;

  private static JScrollPane jScrollPane = null;

  private static File localTearFile = null;

  private static File installationDir = null;

  private static String mainComponentId;

  private static InstallationDescriptor insdObject;

  private static String mainComponentRootPath;

  private static Process gladisProcess = null;

  private static Properties gladisProperties = null;

  private static boolean helpExists = true;

  private static String message = null;

  private static boolean errorFlag = false;

  private static Preferences userPrefs;

  private static final String LAST_FILE_NAME_CHOOSEN_KEY = "LAST_FILE_NAME_CHOOSEN";

  private static final String LAST_DIRECTORY_CHOOSEN_KEY = "LAST_DIRECTORY_CHOOSEN";

  private static final String SET_ENV_FILE = "metadata/setenv.txt";

//  protected static final String UIMA_HOME_ENV = "UIMA_HOME";

//  protected static final String UIMA_DATAPATH_ENV = "uima.datapath";

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
    // check input parameters
    if (localPearFile != null && !localPearFile.exists()) {
      errorFlag = true;
      message = localPearFile.getAbsolutePath() + "file not found \n";
      printInConsole(errorFlag, message);
    }
    /* setting current working directory by default */
    if (installationDir == null) {
      installationDir = new File("./");
      pearConsole
              .append("installation directory is => " + installationDir.getAbsolutePath() + "\n");
    }
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
   * Method that loads specified properties file. The properties file should be on the CLASSPATH.
   * 
   * @param propFileName
   *          The specified properties file name.
   * @return The Properties object loaded from the specified file.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  private static Properties loadProperties(String propFileName) throws IOException {
    Properties propObject = null;
    InputStream propStream = null;
    try {
      propStream = InstallPear.class.getResourceAsStream(propFileName);
      if (propStream == null)
        throw new IOException(propFileName + " not found");
      propObject = new Properties();
      propObject.load(propStream);
    } catch (IOException exc) {
      throw exc;
    } finally {
      if (propStream != null) {
        try {
          propStream.close();
        } catch (Exception e) {
        }
      }
    }
    return (propObject != null) ? propObject : new Properties();
  }

  /**
   * This method runs the installed AE in CVD (Gladis).
   * 
   * @throws IOException
   *           If any I/O exception occurred.
   */
  private void runGladis() throws IOException {
    try {
      // load gladis properties
      if (gladisProperties == null)
        gladisProperties = loadProperties("gladis.properties");

      // get Gladis specific CLASSPATH
      String gladisClassPath = System.getProperty("java.class.path");// gladisProperties.getProperty("env.CLASSPATH");

      // get component specific PATH
      String compPath = InstallationController
              .buildComponentPath(mainComponentRootPath, insdObject);
      // get component specific CLASSPATH
      String compClassPath = InstallationController.buildComponentClassPath(mainComponentRootPath,
              insdObject);
      // get the rest of component specific env. vars
      Properties compEnvVars = InstallationController.buildTableOfEnvVars(insdObject);

      // build command array
      ArrayList cmdArrayList = new ArrayList();
      // set Java executable path - OS dependent
      String osName = System.getProperty("os.name");
      String javaHome = System.getProperty("java.home");
      String javaExeName = (osName.indexOf("Windows") >= 0) ? "java.exe" : "java";
      String javaExePath = null;
      File javaExeFile = null;
      // 1st - try 'java.home'/bin folder
      javaExePath = javaHome + java.io.File.separator + "bin" + java.io.File.separator
              + javaExeName;
      javaExeFile = new File(javaExePath);
      if (!javaExeFile.isFile()) {
        // 2nd - try 'java.home'/jre/bin folder
        javaExePath = javaHome + java.io.File.separator + "jre" + java.io.File.separator + "bin"
                + java.io.File.separator + javaExeName;
        javaExeFile = new java.io.File(javaExePath);
      }
      // start command with executable
      cmdArrayList.add(javaExeFile.getAbsolutePath());
      // add '-cp' JVM option
      if (gladisClassPath.length() > 0 || compClassPath.length() > 0) {
        cmdArrayList.add("-cp");
        StringBuffer cpBuffer = new StringBuffer();
        if (compClassPath.length() > 0)
          cpBuffer.append(compClassPath);
        if (gladisClassPath.length() > 0) {
          if (cpBuffer.length() > 0)
            cpBuffer.append(File.pathSeparatorChar);
          cpBuffer.append(gladisClassPath);
        }
        cmdArrayList.add(cpBuffer.toString());
      }
      // add Gladis JVM options
      String jvmOptions = gladisProperties.getProperty("jvm.options").trim();
      if (jvmOptions != null && jvmOptions.length() > 0) {
        StringTokenizer tokenList = new StringTokenizer(jvmOptions, " ");
        while (tokenList.hasMoreTokens()) {
          String token = tokenList.nextToken();
//          // substitute UIMA_HOME
//          cmdArrayList.add(token.replaceAll("%UIMA_HOME%", System.getProperty("uima.home").replace(
//                  '\\', '/')));
          cmdArrayList.add(token);
        }
      }
      // add component-specific JVM options (env.vars)
      Enumeration compEnvKeys = compEnvVars.keys();
      while (compEnvKeys.hasMoreElements()) {
        // set component-specific env.var. as JVM option
        String key = (String) compEnvKeys.nextElement();
        String value = compEnvVars.getProperty(key);
        // if the same JVM option already set, override it
        boolean valueSet = false;
        for (int i = 0; i < cmdArrayList.size(); i++) {
          String item = (String) cmdArrayList.get(i);
          if (item.startsWith("-D" + key + "=")) {
            item = "-D" + key + "=" + value;
            cmdArrayList.set(i, item);
            valueSet = true;
            break;
          }
        }
        // if the JVM option is still not set, do it now
        if (!valueSet)
          cmdArrayList.add("-D" + key + "=" + value);
      }
      // add java.library.path
      boolean addJavaLibPath = (compPath.length() > 0) ? true : false;
      Enumeration gladisPropKeys = gladisProperties.keys();
      while (gladisPropKeys.hasMoreElements()) {
        String key = (String) gladisPropKeys.nextElement();
        if (key.startsWith("jvm.arg.")) {
          String arg = key.substring(8).trim();
//          // substitute UIMA_HOME
//          String value = gladisProperties.getProperty(key).replaceAll("%UIMA_HOME%",
//                  System.getProperty("uima.home").replace('\\', '/'));
          String value = gladisProperties.getProperty(key);
          // if arg = java.library.path, add component path
          if (arg.equals("java.library.path")) {
            if (addJavaLibPath) {
              value = compPath + File.pathSeparator + value;
              addJavaLibPath = false;
            }
          }
          cmdArrayList.add("-D" + arg + "=" + value);
        }
      }
      // add java.library.path if not added before
      if (addJavaLibPath)
        cmdArrayList.add("-Djava.library.path=" + compPath);
      // add main class
      String mainClass = gladisProperties.getProperty("main.class").trim();
      cmdArrayList.add(mainClass);
      // add main class args sorted by arg name
      gladisPropKeys = gladisProperties.keys();
      TreeMap mainClassArgs = new TreeMap();
      while (gladisPropKeys.hasMoreElements()) {
        String key = (String) gladisPropKeys.nextElement();
        if (key.startsWith("main.class.arg.")) {
//          // substitute UIMA_HOME
//          String value = gladisProperties.getProperty(key).replaceAll("%UIMA_HOME%",
//                  System.getProperty("uima.home").replace('\\', '/'));
        	String value = gladisProperties.getProperty(key);
          // substitute DESCRIPTOR
          if (value.equals("%DESCRIPTOR%"))
            value = insdObject.getMainComponentDesc();
          // add to TreeMap for sorting
          mainClassArgs.put(key, value);
        }
      }
      // add args sorted by arg name
      Iterator mainClassArgKeys = mainClassArgs.keySet().iterator();
      while (mainClassArgKeys.hasNext()) {
        String key = (String) mainClassArgKeys.next();
        int index = key.indexOf('.', 15);
        String arg = key.substring(index + 1).trim();
        String value = (String) mainClassArgs.get(key);
        cmdArrayList.add(arg);
        if (value.length() > 0)
          cmdArrayList.add(value);
      }
      // copy cmd. array list to string array
      String[] cmdArray = new String[cmdArrayList.size()];
      cmdArrayList.toArray(cmdArray);
      if (System.getProperty("DEBUG") != null) {
        System.out.println("[DEBUG:runGladis()]: cmdArray =>");
        for (int i = 0; i < cmdArray.length; i++)
          System.out.println("\t" + cmdArray[i]);
      }

      // build array of environment variables
      Properties sysEnvVars = SystemEnvReader.getEnvVars();
      if (System.getProperty("DEBUG") != null) {
        System.out.println("[DEBUG:runGladis()]: system env vars =>");
        Enumeration keys = sysEnvVars.keys();
        while (keys.hasMoreElements()) {
          String key = (String) keys.nextElement();
          String value = sysEnvVars.getProperty(key);
          System.out.println("\t" + key + "=" + value);
        }
      }
      ArrayList envArrayList = new ArrayList();
      Enumeration sysEnvKeys = sysEnvVars.keys();
      boolean classPathAdded = false;
      boolean pathAdded = false;
      while (sysEnvKeys.hasMoreElements()) {
        String sysKey = (String) sysEnvKeys.nextElement();
        String sysValue = sysEnvVars.getProperty(sysKey);
        String value = sysValue;
        // append component/gladis path and classpath
        if (sysKey.equalsIgnoreCase("CLASSPATH")) {
          value = compClassPath + File.pathSeparator + gladisClassPath + File.pathSeparator
                  + sysValue;
          classPathAdded = true;
        } else if (sysKey.equalsIgnoreCase("PATH") || sysKey.equalsIgnoreCase("LD_LIBRARY_PATH")) {
          value = compPath + File.pathSeparator + sysValue;
          pathAdded = true;
        }
        // add to the env. array
        envArrayList.add(sysKey + "=" + value);
      }
      // check CLASSPATH and PATH
      if (!classPathAdded) {
        String classPath = compClassPath + File.pathSeparator + gladisClassPath;
        envArrayList.add("CLASSPATH=" + classPath);
      }
      if (!pathAdded) {
        String path = compPath;
        envArrayList.add("PATH=" + path);
        envArrayList.add("LD_LIBRARY_PATH=" + path);
      }
      // add the rest of component env. vars
      Enumeration cmpEnvKeys = compEnvVars.keys();
      while (cmpEnvKeys.hasMoreElements()) {
        String cmpKey = (String) cmpEnvKeys.nextElement();
        String cmpValue = compEnvVars.getProperty(cmpKey);
        envArrayList.add(cmpKey + "=" + cmpValue);
      }
      if (System.getProperty("DEBUG") != null) {
        System.out.println("[DEBUG:runGladis()]: envArrayList w/o Gladis =>");
        Iterator list = envArrayList.iterator();
        while (list.hasNext())
          System.out.println("\t" + (String) list.next());
      }
      // add the rest of gladis env. vars
      gladisPropKeys = gladisProperties.keys();
      while (gladisPropKeys.hasMoreElements()) {
        String key = (String) gladisPropKeys.nextElement();
        if (key.startsWith("env.") && !key.equals("env.PATH") && !key.equals("env.CLASSPATH")) {
          String arg = key.substring(4).trim();
//          String value = gladisProperties.getProperty(key).replaceAll("%UIMA_HOME%",
//                  System.getProperty("uima.home").replace('\\', '/'));
          String value = gladisProperties.getProperty(key);
          envArrayList.add(arg + "=" + value);
        }
      }
      // copy env. array list to string array
      String[] envArray = new String[envArrayList.size()];
      envArrayList.toArray(envArray);
      if (System.getProperty("DEBUG") != null) {
        System.out.println("[DEBUG:runGladis()]: envArray =>");
        for (int i = 0; i < envArray.length; i++)
          System.out.println("\t" + envArray[i]);
      }

      // add shutdown hook to terminate Gladis process
      Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
        public void run() {
          try {
            if (gladisProcess != null) {
              gladisProcess.destroy();
              gladisProcess.waitFor();
            }
          } catch (Throwable e) {
          }
        }
      }));

      // start Gladis GUI
      gladisProcess = Runtime.getRuntime().exec(cmdArray, envArray);
      new ProcessUtil.Runner(gladisProcess, "GLADIS");
    } catch (Throwable e) {
      pearConsole.append(" Error in runGladis() " + e.toString());
      if (System.getProperty("DEBUG") != null) {
        e.printStackTrace(System.err);
      }
    }
  }

  /**
   * This method initializes the pearFile TextField.
   * 
   * @return The initialized pearFile TextField.
   */
  private JTextField getPearFileTextField() {
    if (pearFileTextField == null) {
      pearFileTextField = new JTextField();
      pearFileTextField.setBounds(83, 90, 492, 20);
      // hazel's change
      pearFileTextField.setLayout(new BorderLayout());
      pearFileTextField.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent e) {
          if (e.getSource() instanceof JTextField) {
            pearFileTextField = (JTextField) e.getSource();
            browseDirButton.setEnabled(false);
            installButton.setEnabled(true);
            pearConsole.setText(pearFileTextField.getText());
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
      browseButton.setBounds(579, 90, 114, 20);
      browseButton.setText("Browse...");
      browseButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
      browseButton.setMnemonic('b');
      browseButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent e) {
          String pear, message = null;
          if ((e.getActionCommand().equals("Browse..."))) {
            runButton.setEnabled(false);
            pear = selectPear();
            if (pear == null)
              return;
            browseDirButton.setEnabled(true);
            boolean errorflag = false;
            pearConsole.setText("");
            message = "Selected PEAR => " + pearFileTextField.getText() + "\n";
            printInConsole(errorflag, message);
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
      installDirTextField.setBounds(83, 130, 492, 20);
      installDirTextField.isEditable();
      // handling text input
      installDirTextField.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent e) {
          if (e.getSource() instanceof JTextField) {
            installButton.setEnabled(true);
            installDirTextField = (JTextField) e.getSource();
            if (installDirTextField == null)
              pearConsole.setText("");

            else
              pearConsole.append(installDirTextField.getText());
          }
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
      browseDirButton.setBounds(579, 130, 114, 20);
      browseDirButton.setText("Browse Dir...");
      browseDirButton.setEnabled(false);
      browseDirButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
      browseDirButton.setMnemonic('d');
      browseDirButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent e) {
          String dir = null;
          File installationDir = null;
          if (e.getActionCommand().equals("Browse Dir...")) {
            dir = selectDir();
            if (dir == null) {
              installationDir = new File("./");
              installDirTextField.setText(installationDir.getAbsolutePath().toString());
              pearConsole.append(" If you choose to install pear file, installation directory is: "
                      + installationDir.getAbsolutePath() + "\n");
            } else {
              installDirTextField.setText(dir.toString());
              pearConsole.append("installation directory => " + dir + "\n\n");
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
      installButton.setBounds(100, 160, 108, 24);
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
   * This method initializes 'Run your AE in Gladis' Button.
   * 
   * @return The initialized 'Run your AE in Gladis' Button.
   */
  private JButton getRunButton() {
    if (runButton == null) {
      runButton = new JButton();
      runButton.setBounds(255, 160, 240, 24);
      runButton.setText("Run your AE in the CAS Visual Debugger");
      runButton.setEnabled(false);
      runButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
      runButton.setMnemonic('r');
      runButton.setRolloverEnabled(true);
      runButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent e) {
          if (e.getActionCommand().equals("Run your AE in the CAS Visual Debugger")) {
            installButton.setEnabled(false);
            try {
              runGladis();
            } catch (IOException e1) {
              e1.printStackTrace();
            }
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
      jScrollPane = new JScrollPane(getPearConsole(), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
              JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
      jScrollPane.setBounds(10, 190, 708, 352);
      jScrollPane.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
      jScrollPane.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
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
      helpButton.setBounds(540, 160, 108, 24);
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
              manFrame.show();
            } catch (Exception ex) {
              JOptionPane.showMessageDialog(InstallPear.this, ex.getMessage(), "Error showing help",
                      JOptionPane.ERROR_MESSAGE);
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

//    try {
//      String UIMA_HOME = System.getProperty("uima.home");
//      if (UIMA_HOME == null)
//        throw new RuntimeException("-Duima.home not defined");
//      System.setProperty(UIMA_HOME_ENV, UIMA_HOME);
//      // load main properties and replace %UIMA_HOME%
//    } catch (Exception exc) {
//      System.err.println("Error in InstallPear.main():" + exc.toString());
//      exc.printStackTrace(System.err);
//      System.exit(0);
//    }
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
      setIconImage(ImageIO.read(getClass().getResource(Images.UIMA_ICON_SMALL)));
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
            manFrame.show();
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
          JFrame messageFrame = new JFrame("About PEARInstaller");
          aboutMenuItemPanel = new JPanel();
          JLabel frameLabel = new JLabel(new ImageIcon(getClass().getResource(Images.SPLASH)));
          JOptionPane.showMessageDialog(messageFrame, aboutMenuItemPanel.add(frameLabel),
                  "About PEAR Installer Version 1.0", JOptionPane.PLAIN_MESSAGE);
        }
      }
    });
    helpMenu.add(helpMenuItem);
    helpMenu.add(aboutMenuItem);
    JPanel bannerPanel = new JPanel();
    bannerPanel.setSize(735, 60);
    bannerPanel.setBackground(Color.WHITE);
    bannerLabel = new JLabel(new ImageIcon(getClass().getResource(Images.BANNER)));
    bannerLabel.setBounds(0, 30, 735, 20);
    bannerPanel.add(bannerLabel);
    /*
     * Initialize jContentPane and add the initialized components into the content's pane
     */
    JLabel installDirLabel = new JLabel();
    JLabel pearFileLabel = new JLabel();
    JPanel jContentPane = new JPanel();
    jContentPane.setLayout(null);
    pearFileLabel.setBounds(83, 70, 126, 20);
    pearFileLabel.setText("PEAR File:");
    installDirLabel.setBounds(83, 110, 126, 20);
    installDirLabel.setText("Installation Directory:");
    jContentPane.add(pearFileLabel, null);
    jContentPane.add(getPearFileTextField(), null);
    jContentPane.add(getbrowseButton(), null);
    jContentPane.add(installDirLabel, null);
    jContentPane.add(getInstallDirTextField(), null);
    jContentPane.add(getBrowseDirButton(), null);
    jContentPane.add(getInstallButton(), null);
    jContentPane.add(getRunButton(), null);
    jContentPane.add(getHelpButton(), null);
    jContentPane.add(getPearConsole(), null);
    jContentPane.add(getJScrollPane(), null);
    jContentPane.add(bannerPanel);
    setContentPane(jContentPane);

  }

  /**
   * This method initializes the frame.
   */
  private void initialize() {
    this.setTitle("Local PEAR Installation, Verification and Testing");
    this.setSize(735, 600);
    this.setResizable(false);
    // center the frame on the screen
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension GUISize = this.getSize();
    int centerPosX = (screenSize.width - GUISize.width) / 2;
    int centerPosY = (screenSize.width - GUISize.width) / 2;
    setLocation(centerPosX, centerPosY);
  }

}
