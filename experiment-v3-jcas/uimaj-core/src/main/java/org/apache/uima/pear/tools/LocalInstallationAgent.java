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

package org.apache.uima.pear.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

import org.apache.uima.UIMAException;
import org.apache.uima.pear.tools.InstallationController.TestStatus;
import org.apache.uima.pear.tools.InstallationDescriptor.ComponentInfo;
import org.apache.uima.pear.util.FileUtil;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * The <code>LocalInstallationAgent</code> allows localizing files of UIMA compliant components
 * within the single PEAR structure, and verifying localized component's files using UIMA framework
 * API.
 * 
 * <br>
 * This class may be used in the following ways:
 * <ul>
 * <li>As a standalone Java application - <br>
 * <code>
 * java -Xmx512M -DUIMA_HOME=%UIMA_HOME% 
 * org.apache.uima.pear.tools.LocalInstallationAgent  
 * main_component_root_directory
 * </code><br>
 * where the <code>main_component_root_directory</code> is the path to the root directory of the
 * main component (root directory of the single PEAR structure); </li>
 * <li>As a Java object - <br>
 * in this case the caller is expected to set the <code>UIMA_HOME</code> variable, using the
 * <code>setUimaHomePath()</code> method, immediately after creating a new instance of the
 * <code>LocalInstallationAgent</code> class. <br>
 * <b>Note:</b> Some TAEs require large heap size, so the '-Xmx[heapSize]' JVM option may be
 * needed. <br>
 * Localization of component files is performed by using the <code>localizeComponent()</code>
 * method. <br>
 * Verification of localized files is performed by using the <code>verifyLocalizedComponent()</code>
 * method. <br>
 * The applications prints all messages to the standard output and error messages to the standard
 * error output. </li>
 * </ul>
 * In both modes the application uses the <code>metadata/PEAR.properties</code> file for the
 * component localization information. <br>
 * <b>Note:</b> during the localization phase the application creates backup copies of all files in
 * both the <code>conf</code> and <code>desc</code> directories, adding the extension ".$" to
 * each backup file. If the application fails, please make sure all original files in both the
 * directories are restored from appropriate "*.$" backup copies.
 * 
 * @see org.apache.uima.pear.tools.InstallationDescriptor
 * @see org.apache.uima.pear.tools.InstallationProcessor
 * @see org.apache.uima.pear.tools.InstallationTester
 */

public class LocalInstallationAgent {
  // Original file copy suffix
  protected static final String BACKUP_FILE_SUFFIX = ".$";

  // Path macros
  protected static final String MAIN_ROOT = "$main_root";

  protected static final String COMP_ROOT_PREFIX = "$";

  protected static final String COMP_ROOT_SUFFIX = "$root";

  // Attributes
  private String _osName;

  private String _uimaHomePath;

  private File _mainRootDir;

  private Properties _packageConfig = new Properties();

  private InstallationDescriptor _insdObject;

  private File[] _localizedFiles;

  /**
   * Checks that a specified PEAR configuration corresponds to a given installation descriptor.
   * 
   * @param packageConfig
   *          The specified PEAR configuration (<code>Properties</code> object).
   * @param insdObject
   *          The given installation descriptor object.
   * @return <code>true</code> if the specified PEAR configuration corresponds to the given
   *         installation descriptor, <code>false</code> otherwise.
   */
  public static boolean checkPackageConfig(Properties packageConfig,
          InstallationDescriptor insdObject) {
    boolean isOk = false;
    if (packageConfig.getProperty(MAIN_ROOT) != null) {
      Hashtable<String, ComponentInfo> dlgTable = insdObject.getDelegateComponents();
      Iterator<String> idList = dlgTable.keySet().iterator();
      int counter = 0;
      while (idList.hasNext()) {
        String id = idList.next();
        String idRoot = COMP_ROOT_PREFIX + id + COMP_ROOT_SUFFIX;
        if (packageConfig.getProperty(idRoot) != null)
          counter++;
      }
      if (counter == dlgTable.size())
        isOk = true;
    }
    return isOk;
  }

  /**
   * Performs localization of a given component file using information from a given installation
   * descriptor and a specified PEAR configuration.
   * 
   * @param file
   *          The given component file to be localized.
   * @param insdObject
   *          The given installation descriptor object.
   * @param packageConfig
   *          The specified PEAR configuration.
   * @throws java.io.IOException
   *           if any I/O exception occurred.
   */
  public static void localizeComponentFile(File file, InstallationDescriptor insdObject,
          Properties packageConfig) throws IOException {
    // substitute $main_root macros in the file
    String mainRootPath = packageConfig.getProperty(MAIN_ROOT);
    if (mainRootPath != null) {
      // substitute '$main_root_url'
      String replacement = FileUtil.localPathToFileUrl(mainRootPath);
      FileUtil.replaceStringInFile(file, InstallationProcessor.MAIN_ROOT_URL_REGEX, replacement);
      // substitute '$main_root'
      replacement = mainRootPath;
      FileUtil.replaceStringInFile(file, InstallationProcessor.MAIN_ROOT_REGEX, replacement);
    }
    // substitute $comp_id$root macros for all delegates
    Iterator<String> idList = insdObject.getDelegateComponents().keySet().iterator();
    while (idList.hasNext()) {
      String id = idList.next();
      String idRoot = COMP_ROOT_PREFIX + id + COMP_ROOT_SUFFIX;
      String compRootPath = packageConfig.getProperty(idRoot);
      // substitute '$dlg_comp_id$root_rel'
      String regExp = InstallationProcessor.componentIdRootRegExp(id,
              InstallationProcessor.DELEGATE_ROOT_REL_SUFFIX_REGEX);
      String replacement = FileUtil.computeRelativePath(file.getParentFile(),
              new File(compRootPath));
      FileUtil.replaceStringInFile(file, regExp, replacement);
      // substitute '$dlg_comp_id$root_url'
      regExp = InstallationProcessor.componentIdRootRegExp(id,
              InstallationProcessor.DELEGATE_ROOT_URL_SUFFIX_REGEX);
      replacement = FileUtil.localPathToFileUrl(compRootPath);
      FileUtil.replaceStringInFile(file, regExp, replacement);
      // substitute '$dlg_comp__id$root'
      regExp = InstallationProcessor.componentIdRootRegExp(id,
              InstallationProcessor.DELEGATE_ROOT_SUFFIX_REGEX);
      replacement = compRootPath;
      FileUtil.replaceStringInFile(file, regExp, replacement);
    }
  }

  /**
   * Performs localization of a given installation descriptor object using information from a
   * specified PEAR configuration.
   * 
   * @param insdObject installation descriptor object
   * @param packageConfig pear configuration properties
   */
  public static void localizeInstallationDescriptor(InstallationDescriptor insdObject,
          Properties packageConfig) {
    // set main root
    String mainRootPath = packageConfig.getProperty(MAIN_ROOT);
    insdObject.setMainComponentRoot(mainRootPath);
    // set root dirs for all delegates
    Iterator<String> idList = insdObject.getDelegateComponents().keySet().iterator();
    while (idList.hasNext()) {
      String id = idList.next();
      String idRoot = COMP_ROOT_PREFIX + id + COMP_ROOT_SUFFIX;
      String compRootPath = packageConfig.getProperty(idRoot);
      insdObject.setDelegateComponentRoot(id, compRootPath);
    }
  }

  /**
   * Starts the application. This application expects the following JVM run-time settings:
   * <ul>
   * <li>-DUIMA_HOME=&lt;local_uima_root_dir&gt;
   * </ul>
   * 
   * @param args
   *          main_component_root_dir
   */
  public static void main(String[] args) {
    // check args
    if (args.length < 1) {
      System.err.println("usage: LocalInstallationAgent " + "main_component_root_dir");
      return;
    }
    LocalInstallationAgent installer = new LocalInstallationAgent(args[0]);
    try {
      // localize PEAR
      if (installer.localizeComponent()) {
        System.out.println("[LocalInstallationAgent]: " + "localization completed successfully");
        // verify localized PEAR
        if (installer.verifyLocalizedComponent())
          System.out.println("[LocalInstallationAgent]: " + "verification completed successfully");
        else
          System.out.println("[LocalInstallationAgent]: " + "verification failed");
      } else
        System.out.println("[LocalInstallationAgent]: " + "localization failed");
    } catch (Throwable err) {
      System.err.println("Error in LocalInstallationAgent: " + err);
      err.printStackTrace(System.err);
    } finally {
      try {
        if (!installer.undoComponentLocalization())
          throw new RuntimeException("failed to undo changes");
      } catch (Throwable e) {
        System.err.println("Error trying to undo component localization: " + e);
        System.out.println("> Please, make sure that all *.$ files " + "in conf and desc dirs\n"
                + "> are renamed back to their original names");
      }
    }
  }

  /**
   * Constructs new instance of the <code>LocalInstallationAgent</code> class, using a given main
   * component root directory.
   * 
   * @param mainComponentRootPath
   *          The given main component root directory path.
   */
  public LocalInstallationAgent(String mainComponentRootPath) {
    _uimaHomePath = System.getProperty(InstallationController.UIMA_HOME_ENV);
    if (_uimaHomePath != null)
      _uimaHomePath = _uimaHomePath.replace('\\', '/');
    _osName = System.getProperty("os.name");
    System.out.println("[LocalInstallationAgent]: " + "OS - " + _osName);
    _mainRootDir = new File(mainComponentRootPath);
  }

  /**
   * Performs localization of the component files in the 'conf' and 'desc' subdirectories of the
   * specified main root directory, as well as localization of the installation descriptor.
   * 
   * @return <code>true</code> if the localization process completed successfully,
   *         <code>false</code> otherwise.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public synchronized boolean localizeComponent() throws IOException {
    boolean completed = false;
    // load InsD file
    InstallationDescriptorHandler insdHandler = new InstallationDescriptorHandler();
    File insdFile = new File(_mainRootDir, InstallationProcessor.INSD_FILE_PATH);
    try {
      insdHandler.parse(insdFile);
    } catch (IOException exc) {
      throw exc;
    } catch (Exception err) {
      throw new RuntimeException(err);
    }
    _insdObject = insdHandler.getInstallationDescriptor();
    if (_insdObject == null)
      throw new RuntimeException("failed to load installation descriptor");
    System.out.println("[LocalInstallationAgent]: " + "loaded installation descriptor");
    // load PEAR configuration
    File packageConfigFile = new File(_mainRootDir, InstallationController.PACKAGE_CONFIG_FILE);
    InputStream iStream = null;
    try {
      iStream = new FileInputStream(packageConfigFile);
      _packageConfig.load(iStream);
      iStream.close();
      System.out.println("[LocalInstallationAgent]: " + "loaded PEAR configuration");
    } finally {
      if (iStream != null) {
        try {
          iStream.close();
        } catch (Exception e) {
          //ignore close exception
        }
      }
    }
    // check that package configuration has required properties
    if (checkPackageConfig(_packageConfig, _insdObject)) {
      // localize files in conf and desc dirs
      _localizedFiles = localizeComponentFiles();
      // localize installation descriptor
      localizeInstallationDescriptor(_insdObject, _packageConfig);
      completed = true;
    } else
      System.err.println("[LocalInstallationAgent]: "
              + "PEAR properties do not comply with installation descriptor");
    return completed;
  }

  /**
   * Performs localization of the component files in the 'conf' and 'desc' directories by replacing
   * $main_root and $component_id$root macros with the actual path values from the PEAR
   * configuration file. Back-up copies of original files are stored with '.$' extension.
   * 
   * @return The array of localized files.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  protected synchronized File[] localizeComponentFiles() throws IOException {
    // localize all files in conf & desc dirs
    File confDir = new File(_mainRootDir, InstallationController.PACKAGE_CONF_DIR);
    Collection<File> confDirFiles = FileUtil.createFileList(confDir, false);
    File descDir = new File(_mainRootDir, InstallationController.PACKAGE_DESC_DIR);
    Collection<File> descDirFiles = FileUtil.createFileList(descDir, false);
    File[] fileList = new File[confDirFiles.size() + descDirFiles.size()];
    int fileCounter = 0;
    // backup and localize files in conf dir
    Iterator<File> dirList = confDirFiles.iterator();
    while (dirList.hasNext()) {
      File orgFile = dirList.next();
      String bakFileName = orgFile.getName().concat(BACKUP_FILE_SUFFIX);
      File bakFile = new File(orgFile.getParent(), bakFileName);
      if (FileUtil.copyFile(orgFile, bakFile)) {
        // localize original file
        localizeComponentFile(orgFile, _insdObject, _packageConfig);
        // add to localized file list
        fileList[fileCounter++] = orgFile;
      }
    }
    // backup and localize files in desc dir
    dirList = descDirFiles.iterator();
    while (dirList.hasNext()) {
      File orgFile = dirList.next();
      String bakFileName = orgFile.getName().concat(BACKUP_FILE_SUFFIX);
      File bakFile = new File(orgFile.getParent(), bakFileName);
      if (FileUtil.copyFile(orgFile, bakFile)) {
        // localize original file
        localizeComponentFile(orgFile, _insdObject, _packageConfig);
        // add to localized file list
        fileList[fileCounter++] = orgFile;
      }
    }
    return fileList;
  }

  /**
   * Restores original files in the 'conf' and 'desc' directories from the back-up copies (with
   * extension '.$').
   * 
   * @return <code>true</code> if the operation completed successfully, <code>false</code>
   *         otherwise.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public synchronized boolean undoComponentLocalization() throws IOException {
    boolean completed = false;
    int counter = 0;
    for (int i = 0; i < _localizedFiles.length; i++) {
      File orgFile = _localizedFiles[i];
      String bakFileName = orgFile.getName().concat(BACKUP_FILE_SUFFIX);
      File bakFile = new File(orgFile.getParent(), bakFileName);
      if (FileUtil.copyFile(bakFile, orgFile)) {
        bakFile.delete();
        counter++;
      } else {
        System.err.println("[LocalInstallationAgent]: " + "failed to undo changes for the file "
                + orgFile.getAbsolutePath());
      }
    }
    completed = (counter == _localizedFiles.length);
    return completed;
  }

  /**
   * Performs verification of the localized component by running the <code>InstallationTester</code>
   * application.
   * 
   * @return <code>true</code> if the verification completed successfully, <code>false</code>
   *         otherwise.
   * 
   * @throws IOException
   *           if any I/O exception occurred.
   * 
   * @throws ResourceInitializationException
   *           if the specified component cannot be instantiated.
   *           
   * @throws UIMAException
   *           if this exception occurred while identifying UIMA component category.
   *           
   * @see org.apache.uima.pear.tools.InstallationTester
   */
  public synchronized boolean verifyLocalizedComponent() throws IOException,
          ResourceInitializationException, UIMAException {
    // check input parameters
    if (_insdObject == null)
      throw new RuntimeException("null installation descriptor");
    if (_mainRootDir == null)
      throw new RuntimeException("main root directory not specified");
    String mainDescPath = _insdObject.getMainComponentDesc();
    if (mainDescPath == null)
      throw new RuntimeException("main descriptor path not specified");

    // run installation verification test

    InstallationTester installTester = new InstallationTester(new PackageBrowser(new File(
            _mainRootDir.getAbsolutePath())));
    TestStatus status = installTester.doTest();
    
    if (status.getRetCode() == TestStatus.TEST_SUCCESSFUL) {
      return true;
    } else {
      System.err.println("[LocalInstallationAgent]: " + "localization test failed =>");
      System.out.println("> Error message: " + status.getMessage());
      return false;
    }
  }

  /**
   * Sets a given UIMA local home directory path.
   * 
   * @param uimaHomePath
   *          The given UIMA local home directory path.
   */
  public synchronized void setUimaHomePath(String uimaHomePath) {
    _uimaHomePath = uimaHomePath.replace('\\', '/');
  }
}
