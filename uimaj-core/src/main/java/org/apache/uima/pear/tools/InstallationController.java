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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;

import org.apache.uima.UIMAFramework;
import org.apache.uima.internal.util.I18nUtil;
import org.apache.uima.pear.tools.InstallationDescriptor.ComponentInfo;
import org.apache.uima.pear.util.FileUtil;
import org.apache.uima.pear.util.MessageRouter;
import org.apache.uima.pear.util.StringUtil;
import org.apache.uima.resource.PearSpecifier;
import org.apache.uima.util.FileUtils;
import org.xml.sax.SAXException;

/**
 * The <code>InstallationController</code> class allows installing PEAR files that contain UIMA
 * compliant components. <br>
 * <b>Note:</b> current version works both in Windows and Linux.
 * 
 * <br>
 * This class may be used in the following ways:
 * <ul>
 * <li>As a standalone Java application - <br>
 * <code>
 * java -DUIMA_HOME=%UIMA_HOME% org.apache.uima.pear.tools.InstallationController 
 * {-local pear_file | component_id} [-root] [installation_directory]
 * </code><br>
 * where the <code>-local pear_file</code> option allows to install local PEAR file in the local
 * file system (without using SITH services); <br>
 * the <code>component_id</code> is the ID of the component to be installed using SITH services;
 * <br>
 * the <code>-root</code> option enables component installation directly in the specified
 * installation directory, as opposed to installing component in a <code>component_id</code>
 * subdirectory of the specified installation directory; <br>
 * the <code>installation_directory</code> is the directory where the new component will be
 * installed - if the <code>-root</code> option is specified, the component is installed in this
 * directory, otherwise it is installed in a <code>component_id</code> subdirectory of this
 * directory; by default - current working directory. </li>
 * <li>As a Java object - <br>
 * in this case, the caller is expected to set the <code>UIMA_HOME</code> variable, using the
 * <code>setUimaHomePath()</code> method, immediately after creating a new instance of the
 * <code>InstallationController</code> class. <br>
 * Installation is performed by using the <code>installComponent()</code> method. <br>
 * Installation verification is performed by using the <code>verifyComponent()</code> method.
 * <br>
 * Error messages can be retrieved by using the <code>getInstallationMsg()</code> and
 * <code>getVerificationMsg()</code> methods. <br>
 * <b>Note 1:</b> Starting from version 0.6, the <code>InstallationController</code> class
 * utilizes intra-process message routing (see {@link MessageRouter}).
 * Applications need to call the <code>terminate()</code> method on each instance of the
 * <code>InstallationController</code> class after all their operations are completed. <br>
 * The application can get output and error messages, printed by the
 * <code>InstallationController</code>, by adding standard channel listeners (see the
 * <code>addMsgListener()</code> method). By default, the output and error messages are printed to
 * the standard console streams. Alternatively, the application can use the
 * <code>InstallationController</code> constructor that accepts a custom message listener. In this
 * case, the output and error messages will not be printed to the standard console streams. <br>
 * <b>Note 2:</b> Starting from version 1.4, the <code>InstallationController</code> class
 * defines the {@link PackageSelector} interface and allows to plug-in custom package
 * selectors for manually or automatically selecting root directories of installed PEAR packages, as
 * well as PEAR package files that need to be installed. <br>
 * <b>Note 2:</b> Starting from version 1.5, the <code>InstallationController</code> class
 * defines the {@link InstallationMonitor} interface and allows to plug-in custom
 * installation monitors for reporting component installation status and location of installed
 * components. </li>
 * </ul>
 * 
 * @see org.apache.uima.pear.tools.InstallationDescriptor
 * @see org.apache.uima.pear.tools.InstallationProcessor
 * @see org.apache.uima.pear.tools.InstallationTester
 * @see org.apache.uima.pear.util.MessageRouter
 */

public class InstallationController {
  /**
   * The <code>InstallationMonitor</code> interface defines methods required for notifying of
   * component installation status and location of the installed PEAR packages.
   */
  public static interface InstallationMonitor {
    /**
     * Notifies of the installation status of a given component. Acceptable status values are
     * defined in the <code>InstallationController</code> class.
     * 
     * @param componentId
     *          The ID of the given component.
     * @param status
     *          Current installation status of the given component. <br>
     *          Note: Acceptable status values are defined in the
     *          <code>InstallationController</code> class.
     */
    public void setInstallationStatus(String componentId, String status);

    /**
     * Notifies of the installed PEAR package location for a given component.
     * 
     * @param componentId
     *          The ID of the given component.
     * @param componentRootPath
     *          The root directory path of the given installed PEAR package in the local file
     *          system.
     */
    public void setInstallationLocation(String componentId, String componentRootPath);
  }

  /**
   * The <code>PackageSelector</code> interface defines methods required for manually or
   * automatically selecting installed PEAR package root directories and PEAR package files.
   * 
   */
  public static interface PackageSelector {
    /**
     * Selects root directory of an installed PEAR package in the local file system. If the given
     * component is not installed yet, returns <code>null</code>.
     * 
     * @param componentId
     *          The ID of the given installed component.
     * @return The root directory of the installed PEAR package, or <code>null</code>, if the
     *         given component is not installed yet.
     */
    public File selectPackageDirectory(String componentId);

    /**
     * Selects a PEAR package file in the local file system. If the given component PEAR file is not
     * found, returns <code>null</code>.
     * 
     * @param componentId
     *          The ID of the given component.
     * @return The given PEAR package file, or <code>null</code>, if the PEAR file is not found
     *         in the local file system.
     */
    public File selectPackageFile(String componentId);

    /**
     * Selects a PEAR package URL in the network. If the given component PEAR package URL is not
     * found, returns <code>null</code>.
     * 
     * @param componentId
     *          The ID of the given component.
     * @return The given PEAR package URL, or <code>null</code>, if the PEAR package URL is not
     *         found.
     */
    public URL selectPackageUrl(String componentId);
  }

  /**
   * The <code>TestStatus</code> class encapsulates attributes related to the results of the
   * 'serviceability' verification test.
   */
  public static class TestStatus {

    final public static int TEST_SUCCESSFUL = 0;

    final public static int TEST_NOT_SUCCESSFUL = -1;

    private int retCode = TEST_NOT_SUCCESSFUL;

    private String message = null;

    /**
     * @return the message
     */
    public String getMessage() {
      return this.message;
    }

    /**
     * @param message
     *          the message to set
     */
    public void setMessage(String message) {
      this.message = message;
    }

    /**
     * @return the retCode
     */
    public int getRetCode() {
      return this.retCode;
    }

    /**
     * @param retCode
     *          the retCode to set
     */
    public void setRetCode(int retCode) {
      this.retCode = retCode;
    }
  }

  // Component installation status values
  public static final String INSTALLATION_IN_PROGRESS = "installation_in_progress";

  public static final String INSTALLATION_FAILED = "installation_failed";

  public static final String INSTALLATION_COMPLETED = "installation_completed";

  public static final String VERIFICATION_IN_PROGRESS = "verification_in_progress";

  public static final String VERIFICATION_FAILED = "verification_failed";

  public static final String VERIFICATION_COMPLETED = "verification_completed";

  public static final String VERIFICATION_CANCELLED = "verification_cancelled";

  // Verification application class
  protected static final String INSTALLATION_TESTER_APP = "org.apache.uima.pear.tools.InstallationTester";

  private static final String PEAR_MESSAGE_RESOURCE_BUNDLE = "org.apache.uima.pear.pear_messages";

  // Package configuration file
  public static final String PACKAGE_CONFIG_FILE = "metadata/PEAR.properties";

  // Standard package directories
  public static final String PACKAGE_METADATA_DIR = "metadata";

  public static final String PACKAGE_BIN_DIR = "bin";

  public static final String PACKAGE_CONF_DIR = "conf";

  public static final String PACKAGE_DATA_DIR = "data";

  public static final String PACKAGE_DESC_DIR = "desc";

  public static final String PACKAGE_DOC_DIR = "doc";

  public static final String PACKAGE_LIB_DIR = "lib";

  public static final String PACKAGE_RESOURCES_DIR = "resources";

  public static final String PACKAGE_SOURCES_DIR = "src";

  // file generated at the end of local installation
  public static final String SET_ENV_FILE = "metadata/setenv.txt";

  public static final String PEAR_DESC_FILE_POSTFIX = "_pear.xml";

  // UIMA constants
  protected static final String UIMA_HOME_ENV = "UIMA_HOME";

  // UIMA libraries dirs
  protected static final String UIMA_LIB_DIR = "/lib";

  protected static final String VINCI_LIB_DIR = "/lib/vinci";

  // UIMA libraries file extensions
  protected static final String JAR_FILE_EXT = ".jar";

  // special environment variables that need to be appended to JVM variables
  public static final String CLASSPATH_VAR = "classpath";

  protected static final String PATH_VAR = "path";

  // command line options
  protected static final String LOCAL_OPT = "-local";

  protected static final String INSTALL_IN_ROOT_OPT = "-root";

  // static attributes
  private static boolean __inLocalMode = false;

  private static String __osName = null;

  // instance attributes
  private String _mainComponentId;

  private String _installationDirPath;

  private File _installationDir;

  private boolean _cleanInstallDir = true;

  private String _mainComponentRootPath;

  private File _mainComponentRoot;

  private String _mainPearFileLocation = null;

  private Hashtable<String, String> _installationTable = new Hashtable<String, String>();

  private Hashtable<String, InstallationDescriptor> _installationInsDs = new Hashtable<String, InstallationDescriptor>();

  private InstallationDescriptor _insdObject;

  private String _hostIpAddress;

  private String _uimaHomePath;

  private String _installationMsg;

  private String _verificationMsg;
  
  private MessageRouter _msgRouter = null;

  private MessageRouter.StdChannelListener _defaultMsgListener = null;

  private PackageSelector _packageSelector = null;

  private InstallationMonitor _installationMonitor = null;

  /**
   * Appends a list of JAR files in a given lib directory, separated with the OS dependent separator
   * (';' or ':'), to a given initial <code>StringBuffer</code> object. If <code>null</code> 
   * <code>StringBuffer</code>
   * object is specified, creates new <code>StringBuffer</code> object.
   * 
   * @param libDir
   *          The given lib directory.
   * @param listBuffer
   *          The initial <code>StringBuffer</code> object.
   * @return The list of JAR files in the given lib directory, appended to the given
   *         <code>StringBuffer</code>.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  protected static StringBuffer addListOfJarFiles(File libDir, StringBuffer listBuffer)
          throws IOException {
    // create list of JAR files in a given directory
    if (libDir.isDirectory()) {
      Collection<File> fileList = FileUtil.createFileList(libDir);
      Iterator<File> files = fileList.iterator();
      while (files.hasNext()) {
        File file = files.next();
        if (file.getName().toLowerCase().endsWith(JAR_FILE_EXT)) {
          if (listBuffer.length() > 0)
            listBuffer.append(File.pathSeparatorChar);
          listBuffer.append(file.getAbsolutePath().replace('\\', '/'));
        }
      }
    }
    return listBuffer;
  }

  /**
   * Adds a given local environment variable to appropriate system environment variable (before the
   * system value). The case of the local environment variable key is ignored.
   * 
   * @param sysEnvTable
   *          The table of system environment variables.
   * @param localKey
   *          The given local environment variable key.
   * @param localValue
   *          The given local environment variable value.
   * @return <code>true</code> if the local value was really added, <code>false</code>
   *         otherwise.
   */
  protected static boolean addToSystemEnvTable(Properties sysEnvTable, String localKey,
          String localValue) {
    boolean done = false;
    Enumeration<Object> sysKeys = sysEnvTable.keys();
    while (sysKeys.hasMoreElements() && !done) {
      String sysKey = (String) sysKeys.nextElement();
      // system key and local key could have different cases
      if (sysKey.equalsIgnoreCase(localKey)) {
        String sysValue = sysEnvTable.getProperty(sysKey);
        sysEnvTable.setProperty(sysKey, localValue + File.pathSeparator + sysValue);
        done = true;
      }
    }
    return done;
  }

  /**
   * Creates a string array that contains network parameters (in the JVM '-Dname=value' format)
   * specified in a given installation descriptor object.
   * 
   * @param insdObject
   *          The given installation descriptor object.
   * @return The string array of network parameters in the JVM format.
   */
  public static String[] buildArrayOfNetworkParams(InstallationDescriptor insdObject) {
    String[] paramsArray = new String[0];
    List<String> paramsList = new ArrayList<String>();
    StringBuffer itemBuffer = new StringBuffer();
    Set<String> pNames = insdObject.getMainComponentNetworkParamNames();
    // go through specified parameters and add them to the list
    if (pNames != null) {
      Iterator<String> pList = pNames.iterator();
      while (pList.hasNext()) {
        String pName = pList.next();
        Properties param = insdObject.getMainComponentNetworkParam(pName);
        Enumeration<Object> keys = param.keys();
        while (keys.hasMoreElements()) {
          String key = (String) keys.nextElement();
          String value = param.getProperty(key);
          if (value.length() > 0) {
            // add '-Dkey=value' to the list
            itemBuffer.setLength(0);
            itemBuffer.append("-D");
            itemBuffer.append(key.trim());
            itemBuffer.append('=');
            itemBuffer.append(value.trim());
            paramsList.add(itemBuffer.toString());
          }
        }
      }
    }
    if (paramsList.size() > 0) {
      // copy list to array
      paramsArray = new String[paramsList.size()];
      paramsList.toArray(paramsArray);
    }
    return paramsArray;
  }

  /**
   * Creates a string that should be added to the CLASSPATH for a given installed component
   * associated with a given installation descriptor object.
   * 
   * @param compRootDirPath
   *          The given root directory of the installed component.
   * @param insdObject
   *          The given installation descriptor object.
   * @param addLibDir
   *          Whether we should add jars from the libdir or not (true at packaging time, false at
   *          runtime).
   * @return The string that should be added to the CLASSPATH for the given component.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static String buildComponentClassPath(String compRootDirPath,
          InstallationDescriptor insdObject, boolean addLibDir) throws IOException {
    // create list of JAR files in lib dir.
    File compLibDir = new File(compRootDirPath, PACKAGE_LIB_DIR);
    StringBuffer cpBuffer = new StringBuffer();
    if (addLibDir) {
      cpBuffer = addListOfJarFiles(compLibDir, cpBuffer);
    }
    // append all specified CLASSPATH env.var. settings
    Iterator<InstallationDescriptor.ActionInfo> envActions = insdObject.getInstallationActions(
            InstallationDescriptor.ActionInfo.SET_ENV_VARIABLE_ACT).iterator();
    while (envActions.hasNext()) {
      // if env.var.name is CLASSPATH, append value to the buffer
      InstallationDescriptor.ActionInfo actInfo = envActions.next();
      if (actInfo.params != null) {
        String varName = actInfo.params.getProperty(InstallationDescriptorHandler.VAR_NAME_TAG);
        String varValue = actInfo.params.getProperty(InstallationDescriptorHandler.VAR_VALUE_TAG);
        if (varName != null && varValue != null && varName.equalsIgnoreCase(CLASSPATH_VAR)) {
          if (cpBuffer.length() > 0)
            cpBuffer.append(File.pathSeparatorChar);
          cpBuffer.append(varValue.replace('\\', '/'));
        }
      }
    }
    return cpBuffer.toString();
  }

  /**
   * Creates a string that should be added to the SPATH for a given installed component associated
   * with a given installation descriptor object.
   * 
   * @param compRootDirPath
   *          The given root directory of the installed component.
   * @param insdObject
   *          The given installation descriptor object.
   * @return The string that should be added to the SPATH for the given component.
   */
  public static String buildComponentPath(String compRootDirPath, InstallationDescriptor insdObject) {
    // append 'bin' directory to component path
    File compBinDir = new File(compRootDirPath, PACKAGE_BIN_DIR);
    StringBuffer pBuffer = new StringBuffer();
    if (compBinDir.isDirectory()) {
      pBuffer.append(compBinDir.getAbsolutePath().replace('\\', '/'));
    }
    // append all specified PATH env.var. settings
    Iterator<InstallationDescriptor.ActionInfo> envActions = insdObject.getInstallationActions(
            InstallationDescriptor.ActionInfo.SET_ENV_VARIABLE_ACT).iterator();
    while (envActions.hasNext()) {
      // if env.var.name is PATH, append value to the buffer
      InstallationDescriptor.ActionInfo actInfo = envActions.next();
      if (actInfo.params != null) {
        String varName = actInfo.params.getProperty(InstallationDescriptorHandler.VAR_NAME_TAG);
        String varValue = actInfo.params.getProperty(InstallationDescriptorHandler.VAR_VALUE_TAG);
        if (varName != null && varValue != null && varName.equalsIgnoreCase(PATH_VAR)) {
          if (pBuffer.length() > 0)
            pBuffer.append(File.pathSeparatorChar);
          pBuffer.append(varValue.replace('\\', '/'));
        }
      }
    }
    return pBuffer.toString();
  }

  /**
   * Creates a string that contains the list of environment variables settings (in the JVM
   * '-Dname=value' format) included in a given installation descriptor object.
   * 
   * @param insdObject
   *          The given installation descriptor object.
   * @return The string of environment variables settings in the JVM format.
   */
  public static String buildListOfEnvVars(InstallationDescriptor insdObject) {
    Properties envVarsTable = buildTableOfEnvVars(insdObject);
    StringBuffer envBuffer = new StringBuffer();
    // add env.var. setting in the JVM format
    Enumeration<Object> names = envVarsTable.keys();
    while (names.hasMoreElements()) {
      String varName = (String) names.nextElement();
      String varValue = envVarsTable.getProperty(varName);
      if (varName.length() > 0 && varValue != null && varValue.length() > 0) {
        if (envBuffer.length() > 0)
          envBuffer.append(' ');
        envBuffer.append("-D");
        envBuffer.append(varName);
        envBuffer.append('=');
        envBuffer.append(varValue);
      }
    }
    return envBuffer.toString();
  }

  /**
   * Creates a string that contains network parameters (in the JVM '-Dname=value' format) specified
   * in a given installation descriptor object.
   * 
   * @param insdObject
   *          The given installation descriptor object.
   * @return The string of network parameters in the JVM format.
   */
  public static String buildListOfNetworkParams(InstallationDescriptor insdObject) {
    StringBuffer paramsBuffer = new StringBuffer();
    String[] paramsArray = buildArrayOfNetworkParams(insdObject);
    for (int i = 0; i < paramsArray.length; i++) {
      // add '-Dkey=value' to the string
      paramsBuffer.append(paramsArray[i]);
      paramsBuffer.append(' ');
    }
    return paramsBuffer.toString();
  }

  /**
   * Creates a <code>Properties</code> table that contains (name, value) pairs of environment
   * variables settings for a given installation descriptor object.
   * 
   * @param insdObject
   *          The given installation descriptor object.
   * @return The <code>Properties</code> table that contains environment variables settings for
   *         the given installation descriptor object.
   */
  public static Properties buildTableOfEnvVars(InstallationDescriptor insdObject) {
    Properties envVarsTable = new Properties();
    // find all 'set_env_variable' actions
    Iterator<InstallationDescriptor.ActionInfo> envActions = insdObject.getInstallationActions(
            InstallationDescriptor.ActionInfo.SET_ENV_VARIABLE_ACT).iterator();
    while (envActions.hasNext()) {
      // add env.var. settings to the table
      InstallationDescriptor.ActionInfo actInfo = envActions.next();
      String varName = actInfo.params.getProperty(InstallationDescriptorHandler.VAR_NAME_TAG);
      String varValue = actInfo.params.getProperty(InstallationDescriptorHandler.VAR_VALUE_TAG);
      // exclude CLASSPATH and PATH
      if (varName != null && varValue != null && !varName.equalsIgnoreCase(CLASSPATH_VAR)
              && !varName.equalsIgnoreCase(PATH_VAR)) {
        // check if the same name exists
        String curValue = envVarsTable.getProperty(varName);
        if (curValue != null) {
          // add new value
          curValue = curValue + File.pathSeparator + varValue;
          envVarsTable.setProperty(varName, curValue);
        } else
          envVarsTable.setProperty(varName, varValue);
      }
    }
    return envVarsTable;
  }

  /**
   * Creates a string that should be added to the CLASSPATH environment variable for UIMA framework.
   * 
   * @param uimaHomeEnv
   *          The location of UIMA framework (UIMA_HOME environment variable value).
   * @return The CLASSPATH string for UIMA framework.
   */
  public static String buildUIMAClassPath(String uimaHomeEnv) {
    try {
      StringBuffer cpBuffer = new StringBuffer();
      File uimaLibDir = new File(uimaHomeEnv + UIMA_LIB_DIR);
      cpBuffer = addListOfJarFiles(uimaLibDir, cpBuffer);
      File vinciLibDir = new File(uimaHomeEnv + VINCI_LIB_DIR);
      cpBuffer = addListOfJarFiles(vinciLibDir, cpBuffer);
      return cpBuffer.toString();
    } catch (IOException exc) {
      return File.pathSeparator;
    }
  }

  /**
   * Deletes all installed files for a given component in a given parent directory. If the
   * <code>includeDelegates</code> flag is <code>true</code>, deletes also all files installed
   * in a given parent directory for separate delegate components, specified in the main
   * installation descriptor.
   * 
   * @param componentId
   *          The given main component ID.
   * @param parentDir
   *          The given parent directory of the main component root directory.
   * @param includeDelegates
   *          Indicates whether files of the specified separate delegate components should be
   *          deleted.
   * @return <code>true</code>, if the deletion operation completed successfully,
   *         <code>false</code> otherwise.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static boolean deleteInstalledFiles(String componentId, File parentDir,
          boolean includeDelegates) throws IOException {
    boolean done = true;
    File rootDir = new File(parentDir, componentId);
    if (!rootDir.isDirectory()) {
      return false;
    }
    if (includeDelegates) {
      // get main installation descriptor
      InstallationDescriptorHandler insdHandler = new InstallationDescriptorHandler();
      File insdFile = new File(rootDir, InstallationProcessor.INSD_FILE_PATH);
      try {
        insdHandler.parse(insdFile);
      } catch (IOException e) {
        throw e;
      } catch (Exception err) {
        throw new IOException(err.getMessage());
      }
      InstallationDescriptor insdObject = insdHandler.getInstallationDescriptor();
      // get list of separate delegate components
      Hashtable<String, ComponentInfo> dlgComponents = insdObject.getDelegateComponents();
      Enumeration<String> dlgCompIds = dlgComponents.keys();
      while (dlgCompIds.hasMoreElements()) {
        String dlgCompId = dlgCompIds.nextElement();
        if (!deleteInstalledFiles(dlgCompId, parentDir, true))
          done = false;
      }
    }
    if (!FileUtil.deleteDirectory(rootDir))
      done = false;
    return done;
  }

  /**
   * Extracts files with a given extension from a given PEAR file into a given target directory. If
   * the given filename extension is <code>null</code>, extracts all the files from a given PEAR
   * file. Returns the path to the new component root directory.
   * 
   * @param pearFileLocation
   *          The given PEAR file location.
   * @param fileExt
   *          The given filename extension.
   * @param targetDir
   *          The given target directory.
   * @param cleanTarget
   *          If true, the target directory is cleaned before the PEAR file is installed to it.
   * @return The path to the new component root directory.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static String extractFilesFromPEARFile(String pearFileLocation, String fileExt,
          File targetDir, boolean cleanTarget) throws IOException {
    return extractFilesFromPEARFile(pearFileLocation, fileExt, targetDir, null, cleanTarget);
  }

  /**
   * Internal implementatiton of the <code>extractFilesFromPEARFile</code> method, which allows
   * sending messages to the OUT and ERR queues.
   * 
   * @param pearFileLocation
   *          The given PEAR file location.
   * @param fileExt
   *          The given filename extension.
   * @param targetDir
   *          The given target directory.
   * @param controller
   *          The instance of the <code>InstallationController</code> class that provides OUT and ERR 
   * @param cleanTarget
   *          If true, the target directory is cleaned before the PEAR file is installed to it. 
   * message routing, or <code>null</code>.
   * @return The path to the new component root directory.
   * @throws IOException if any I/O exception occurred.
   */
  protected static String extractFilesFromPEARFile(String pearFileLocation, String fileExt,
          File targetDir, InstallationController controller, boolean cleanTarget)
          throws IOException {
    // get PEAR file size
    long fileSize = FileUtil.getFileSize(pearFileLocation);
    // create root directory
    if (!targetDir.isDirectory() && !targetDir.mkdirs()) {
      //create localized error message
      String message = I18nUtil.localizeMessage(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "installation_controller_error_creating_install_dir", new Object[] { targetDir
                      .getAbsolutePath() });
      throw new IOException(message);
    }
    // clean target directory
    if (cleanTarget) {
      if (FileUtils.deleteRecursive(targetDir)) {
        if (!targetDir.mkdirs()) {
          //create localized error message
          String message = I18nUtil.localizeMessage(PEAR_MESSAGE_RESOURCE_BUNDLE,
                  "installation_controller_error_creating_install_dir", new Object[] { targetDir
                          .getAbsolutePath() });
          throw new IOException(message);
        }
      } else {
        //create localized error message
        String message = I18nUtil.localizeMessage(PEAR_MESSAGE_RESOURCE_BUNDLE,
                "installation_controller_error_cleaning_install_dir", new Object[] { targetDir
                        .getAbsolutePath() });
        throw new IOException(message);
      }
    }
    // specify local PEAR file
    File pearFile = null;
    boolean removeLocalCopy = false;
    boolean done = false;
    JarFile jarFile = null;
    
    try {
      // try to find PEAR file in the local file system
      pearFile = new File(pearFileLocation);
      if (!pearFile.isFile()) {
        // copy PEAR file to the target directory using URL connection
        URL pearFileUrl = new URL(pearFileLocation);
        if (controller != null) // write message to OUT msg queue
          controller.getOutMsgWriter().println(
                  "[InstallationController]: copying " + fileSize + " bytes from "
                          + pearFileUrl.toExternalForm() + " to " + targetDir.getAbsolutePath());
        else
          // print message to console
          System.out.println("[InstallationController]: copying " + fileSize + " bytes from "
                  + pearFileUrl.toExternalForm() + " to " + targetDir.getAbsolutePath());
        String pearFileName = (new File(pearFileUrl.getFile())).getName();
        pearFile = new File(targetDir, pearFileName);
        if (!FileUtil.copyFile(pearFileUrl, pearFile))
          throw new IOException("cannot copy " + pearFileUrl + " to file "
                  + pearFile.getAbsolutePath());
        removeLocalCopy = true;
      }
      if (controller != null) // write message to OUT msg queue
        controller.getOutMsgWriter().println(
                "[InstallationController]: extracting " + pearFile.getAbsolutePath());
      else
        // print message to console
        System.out.println("[InstallationController]: extracting " + pearFile.getAbsolutePath());
      
      jarFile = new JarFile(pearFile);
      long totalBytes = (fileExt == null) ? FileUtil.extractFilesFromJar(jarFile, targetDir) : // all
              // files
              FileUtil.extractFilesWithExtFromJar( // files with extension
                      jarFile, fileExt, targetDir);
      if (controller != null) // write message to OUT msg queue
        controller.getOutMsgWriter().println(
                "[InstallationController]: " + totalBytes + " bytes extracted");
      else
        // print message to console
        System.out.println("[InstallationController]: " + totalBytes + " bytes extracted");
      if (removeLocalCopy) {
        // remove local copy of PEAR file
        if (!pearFile.delete())
          pearFile.deleteOnExit();
      }
      done = true;
    } catch (MalformedURLException urlExc) {
      throw new FileNotFoundException(pearFileLocation);
    } catch (IOException ioExc) {
      throw ioExc;
    } catch (Throwable err) {
      throw new IOException(err.toString());
    } finally {
        if(jarFile != null) 
            try{
                jarFile.close();
            } catch(IOException ioe) {
                IOException e = new IOException("Can't close open PEAR file :" + jarFile.getName());
                e.initCause(ioe);
                throw e;
            }
    }
    return done ? targetDir.getAbsolutePath() : null;
  }

  /**
   * Extracts all files of a given component from a given PEAR file into a given target directory.
   * Returns the path to the new component root directory.
   * 
   * @param pearFileLocation
   *          The given PEAR file location.
   * @param installationDir
   *          The given target directory.
   * @param cleanTarget
   *          If true, the target directory is cleaned before the PEAR file is installed to it.
   * @return The path to the new component root directory.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static String extractPEARFile(String pearFileLocation, File installationDir,
          boolean cleanTarget) throws IOException {
    return extractFilesFromPEARFile(pearFileLocation, null, installationDir, cleanTarget);
  }

  /**
   * Internal implementation of the <code>extractPEARFile</code> method, which allows sending
   * messages to the OUT and ERR queues.
   * 
   * @param pearFileLocation
   *          The given PEAR file location.
   * @param installationDir
   *          The given target directory.
   * @param controller
   *          The instance of the <code>InstallationController</code> class that provides OUT and ERR 
   * message routing, or <code>null</code>.
   * @param cleanTarget
   *          If true, the target directory is cleaned before the PEAR file is installed to it. 
   * @return The path to the new component root directory.
   * @throws IOException if any I/O exception occurred.
   */
  protected static String extractPEARFile(String pearFileLocation, File installationDir,
          InstallationController controller, boolean cleanTarget) throws IOException {
    return extractFilesFromPEARFile(pearFileLocation, null, installationDir, controller,
            cleanTarget);
  }

  /**
   * Creates a <code>Hashtable</code> that contains (compId, InsD) pairs for all separate delegate
   * components specified in a given installation table.
   * 
   * @param installationTable
   *          The given installation table that specifies (compId, rootDirPath) pairs for all
   *          separate delegate components.
   * @return The <code>Hashtable</code> that contains (compId, InsD) pairs for all separate
   *         delegate components specified in the given installation table.
   * @throws IOException
   *           If an I/O exception occurred while loading the installation descriptor files.
   */
  protected static Hashtable<String, InstallationDescriptor> getDelegateInstallationDescriptors(Hashtable<String, String> installationTable)
          throws IOException {
    // get list of separately installed delegate components
    Enumeration<String> dlgIdList = installationTable.keys();
    // build Hashtable of delegate InsD objects
    Hashtable<String, InstallationDescriptor> dlgInsdObjects = new Hashtable<String, InstallationDescriptor>();
    while (dlgIdList.hasMoreElements()) {
      // process next delegate component
      String dlgId = dlgIdList.nextElement();
      String dlgRootPath = installationTable.get(dlgId);
      // get InsD object for this component
      PackageBrowser dlgBrowser = new PackageBrowser(new File(dlgRootPath));
      InstallationDescriptor dlgInsdObject = dlgBrowser.getInstallationDescriptor();
      dlgInsdObjects.put(dlgId, dlgInsdObject);
    }
    return dlgInsdObjects;
  }

  /**
   * @return The local host IP address.
   */
  public static String getHostIpAddress() {
    String hostAddress = "127.0.0.1";
    try {
      hostAddress = InetAddress.getLocalHost().getHostAddress();
    } catch (Exception e) {
      // in case of errors use localhost IP address
    }
    return hostAddress;
  } // end of getHostIpAddress() method

  /**
   * Retrieves the root directory path of a given component, installed in the local file system, by
   * using a given <code>PackageSelector</code> input. If the given <code>PackageSelector</code>
   * is <code>null</code>, the default <code>PackageSelector</code> implementation is used.
   * 
   * @param componentId
   *          The given installed component ID.
   * @param pkgSelector
   *          The instance of the
   *          <code>PackageSelector</code> class that allows selecting root directory 
   * of the installed component in the local file system.
   * @return The root directory path of the given component in the local 
   * file system, or <code>null</code>, if the component is not installed.
   */
  protected static String getInstalledComponentRootPath(String componentId,
          PackageSelector pkgSelector) {
    String componentRootPath = null;
    PackageSelector packageSelector = (pkgSelector != null) ? pkgSelector
            : new SimplePackageSelector();
    // check if this component is already installed locally
    File componentRootDir = packageSelector.selectPackageDirectory(componentId);
    if (componentRootDir != null)
      componentRootPath = componentRootDir.getAbsolutePath();
    return componentRootPath;
  } // end of getInstalledComponentRootPath() method

  /**
   * Gets the PEAR file location (file path or URL) for a given component by using SITH DB a given
   * <code>PackageSelector</code> input. If the given <code>PackageSelector</code> is
   * <code>null</code>, the default <code>PackageSelector</code> implementation is used.
   * 
   * @param componentId
   *          The given component ID.
   * @param pkgSelector
   *          The instance of the
   *          <code>PackageSelector</code> class that allows selecting location of the 
   * given component PEAR file in the local file system, or in the network.
   * @return The location of the PEAR file for the given component, or 
   * <code>null</code>, if the PEAR file was not found.
   */
  protected static String getPEARFileLocation(String componentId, PackageSelector pkgSelector) {
    String pearFileLocation = null;
    PackageSelector packageSelector = (pkgSelector != null) ? pkgSelector
            : new SimplePackageSelector();
    // check if the PEAR file is in the local FS
    File pearFile = packageSelector.selectPackageFile(componentId);
    if (pearFile != null)
      pearFileLocation = pearFile.getAbsolutePath();
    else {
      // enter PEAR file URL
      URL pearFileUrl = packageSelector.selectPackageUrl(componentId);
      if (pearFileUrl != null)
        pearFileLocation = pearFileUrl.toString();
    }
    return pearFileLocation;
  } // end of getPEARFileLocation()

  /**
   * Starts the application. This application expects the following JVM run-time settings:
   * <ul>
   * <li>-DUIMA_HOME=&lt;local_uima_root_dir&gt;
   * </ul>
   * 
   * @param args
   *          {-local pear_file | main_component_id} [-default] [installation_dir]
   */
  public static void main(String[] args) {
    // check args
    if (args.length < 1) {
      System.err.println("usage: InstallationController "
              + "{-local pear_file | main_component_id} [-root] " + "[installation_dir]");
      return;
    }
    File localPearFile = null;
    String mainComponentId = null;
    boolean installInRootDir = false;
    File installationDir = null;
    // check local mode
    int argNo = 0;
    if (args[argNo].equals(LOCAL_OPT)) {
      // work in 'local' mode
      setLocalMode(true);
      if (args.length > argNo + 1)
        localPearFile = new File(args[++argNo]);
    } else {
      // work in standard SITH mode
      mainComponentId = args[argNo++];
    }
    // parse command line
    for (int i = argNo; i < args.length; i++) {
      if (args[i].equals(INSTALL_IN_ROOT_OPT)) {
        installInRootDir = true;
      } else {
        installationDir = new File(args[i]);
      }
    }
    if (localPearFile == null && mainComponentId == null) {
      System.err.println("usage: InstallationController "
              + "{-local pear_file | main_component_id} " + "[-root] [installation_dir]");
      return;
    }
    // check input parameters
    if (localPearFile != null && !localPearFile.exists()) {
      System.err.println("[InstallationController]: " + localPearFile.getAbsolutePath()
              + " file not found");
    }
    if (installationDir == null) // set CWD by default
      installationDir = new File(".");
    else if (!installationDir.isDirectory()) {
      System.err.println("[InstallationController]: " + installationDir.getAbsolutePath()
              + " directory not found");
      return;
    }
    // in local mode - get component ID from PEAR file
    if (__inLocalMode) {
      try {
        JarFile jarFile = new JarFile(localPearFile);
        InstallationDescriptorHandler insdHandler = new InstallationDescriptorHandler();
        insdHandler.parseInstallationDescriptor(jarFile);
        InstallationDescriptor insd = insdHandler.getInstallationDescriptor();
        if (insd != null)
          mainComponentId = insd.getMainComponentId();
        else
          throw new FileNotFoundException("installation descriptor not found");
      } catch (Exception err) {
        System.err.println("[InstallationController]: terminated \n" + err.toString());
        return;
      }
    }
    // create instance of InstallationController
    InstallationController controller = __inLocalMode ? new InstallationController(mainComponentId,
            localPearFile, installationDir, installInRootDir) : new InstallationController(
            mainComponentId, installationDir.getAbsolutePath(), installInRootDir);
    // set PackageSelector
    controller.setPackageSelector(new PackageSelectorGUI());
    // 1st step: install main component
    if (controller.installComponent() == null) {
      // installation failed
      controller.getErrMsgWriter().println(
              "[InstallationController]: installation of " + mainComponentId + " failed => \n"
                      + controller.getInstallationMsg());
    } else {
      try {
        controller.getOutMsgWriter().println(
                "[InstallationController]: installation of " + mainComponentId + " completed");
        // 2nd step: verify main component installation
        if (controller.verifyComponent()) {
          controller.getOutMsgWriter().println(
                  "[InstallationController]: verification of " + mainComponentId + " completed");
          controller.getOutMsgWriter().println(
                  "[InstallationController]: " + mainComponentId + " installed in the "
                          + controller._mainComponentRootPath + " directory.");
        } else {
          controller.getOutMsgWriter().println(
                  "[InstallationController]: verification of " + mainComponentId + " failed => \n"
                          + controller.getVerificationMsg());
        }
      } catch (Exception exc) {
        System.err.println("Error in InstallationController.main(): " + exc.toString());
        exc.printStackTrace(System.err);
      }
    }
    // stop controller messaging service
    controller.terminate();
    System.exit(0);
  }

  /**
   * Switches between the 'local' and 'DB' modes, depending on a given <code>boolean</code> flag.
   * 
   * @param inLocalMode
   *          if <code>true</code> the utility operates in the 'local' mode, otherwise it operates
   *          in the 'DB' mode.
   */
  public static synchronized void setLocalMode(boolean inLocalMode) {
    __inLocalMode = inLocalMode;
  }

  /**
   * Runs the installation test for a given installed pear component, and returns the
   * <code>TestStatus</code> object with the test results.
   * 
   * @param pkgBrowser
   *          The given package browser object of the installed pear package.
   * @return The <code>TestStatus</code> object that contains the return code and possible error
   *         message of the test.
   */
  public static synchronized TestStatus verifyComponentInstallation(PackageBrowser pkgBrowser) {
    try {
      // check package browser parameters
      if (pkgBrowser != null) {
        if (pkgBrowser.getInstallationDescriptor() == null) {
          throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
                  "installation_verification_install_desc_not_available");
        }
        if (pkgBrowser.getInstallationDescriptor().getMainComponentDesc() == null) {
          throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
                  "installation_verification_main_desc_not_available", new Object[] { pkgBrowser
                          .getInstallationDescriptor().getMainComponentId() });
        }
        if (pkgBrowser.getInstallationDescriptor().getMainComponentRoot() == null) {
          throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
                  "installation_verification_main_root_not_available", new Object[] { pkgBrowser
                          .getInstallationDescriptor().getMainComponentId() });
        }
      }

      // create InstallationTester object
      InstallationTester installTester = new InstallationTester(pkgBrowser);
      return installTester.doTest();

    } catch (Throwable exc) {
      // print exception as 'verification message'
      StringWriter strWriter = new StringWriter();
      PrintWriter oWriter = new PrintWriter(strWriter);
      exc.printStackTrace(oWriter);
      TestStatus status = new TestStatus();
      status.setRetCode(TestStatus.TEST_NOT_SUCCESSFUL);
      status.setMessage(strWriter.toString());
      return status;
    }
  }

  /**
   * Constructs an instance of the <code>InstallationController</code> class for a given component
   * and a given installation root directory. By default, the <code>InstallationController</code>
   * creates a <code>component_id</code> subdirectory for the component code and resources. By
   * default, the <code>InstallationController</code> class sends all stdout and stderr messages
   * to the default message listener, which prints them to the standard console streams.
   * 
   * @param componentId
   *          The given component ID.
   * @param rootDirPath
   *          The given installation root directory path.
   */
  public InstallationController(String componentId, String rootDirPath) {
    this(componentId, rootDirPath, false);
  }

  /**
   * Constructs an instance of the <code>InstallationController</code> class for a given component
   * and a given installation root directory. If the <code>installInRootDir</code> flag is
   * <code>true</code>, the component will be installed in the given root directory, otherwise
   * the <code>InstallationController</code> will create a <code>component_id</code>
   * subdirectory for the component code and resources. By default, the
   * <code>InstallationController</code> class sends all stdout and stderr messages to the default
   * message listener, which prints them to the standard console streams.
   * 
   * @param componentId
   *          The given component ID.
   * @param rootDirPath
   *          The given installation root directory path.
   * @param installInRootDir
   *          If <code>true</code>, the component will be installed in the given root directory,
   *          otherwise it will be installed in the <code>component_id</code> subdirectory of the
   *          root directory. Note: the installation directory will be cleaned before the PEAR file is 
   *          installed to it.
   */
  public InstallationController(String componentId, String rootDirPath, boolean installInRootDir) {
    this(componentId, rootDirPath, installInRootDir, null);
  }

  /**
   * Constructs an instance of the <code>InstallationController</code> class for a given component
   * and a given installation root directory. If the <code>installInRootDir</code> flag is
   * <code>true</code>, the component will be installed in the given root directory, otherwise
   * the <code>InstallationController</code> will create a <code>component_id</code>
   * subdirectory for the component code and resources. If a given custom message listener is not
   * <code>null</code>, the <code>InstallationController</code> instance will sends all stdout
   * and stderr messages to the given message listener, otherwise these messages are sent to the
   * default message listener, which prints them to the standard console streams.
   * 
   * @param componentId
   *          The given component ID.
   * @param rootDirPath
   *          The given installation root directory path.
   * @param installInRootDir
   *          If <code>true</code>, the component will be installed in the given root directory,
   *          otherwise it will be installed in the <code>component_id</code> subdirectory of the
   *          root directory. Note: the installation directory will be cleaned before the PEAR file is 
   *          installed to it.
   * @param msgListener
   *          The given custom message listener or <code>null</code>.
   */
  public InstallationController(String componentId, String rootDirPath, boolean installInRootDir,
          MessageRouter.StdChannelListener msgListener) {
    this(componentId, rootDirPath, installInRootDir, null, msgListener, true);
    // print program information
    getOutMsgWriter().println(
            "[InstallationController]: " + "OS - " + __osName + ", Host - " + _hostIpAddress);
    if (__inLocalMode) {
      getOutMsgWriter().println("[InstallationController]: " + "working in 'local' mode");
    }
  }

  /**
   * Internal constructor that creates an instance of the <code>InstallationController</code>
   * class for a given component and a given installation root directory. If the
   * <code>installInRootDir</code> flag is <code>true</code>, the component will be installed
   * in the given root directory, otherwise the <code>InstallationController</code> will create a
   * <code>component_id</code> subdirectory for the component code and resources. If a given
   * custom <code>MessageRouter</code> is not <code>null</code>, the new
   * <code>InstallationController</code> instance will use the given message router, otherwise it
   * will create a new message router object. If a given custom message listener is not
   * <code>null</code>, the <code>InstallationController</code> instance will send all stdout
   * and stderr messages to the given message listener, otherwise these messages are sent to the
   * default message listener, which prints them to the standard console streams.
   * 
   * @param componentId
   *          The given component ID.
   * @param rootDirPath
   *          The given installation root directory.
   * @param installInRootDir
   *          If <code>true</code>, the component will be installed in the given root directory,
   *          otherwise it will be installed in the <code>component_id</code> subdirectory of the
   *          root directory.
   * @param msgRouter
   *          The given custom <code>MessageRouter</code> object or <code>null</code>.
   * @param msgListener
   *          The given custom message listener object or <code>null</code>.
   * @param cleanInstallDir
   *          If <code>true</code>, the target installation directory will be cleaned before the
   *          PEAR file is installed.
   */
  protected InstallationController(String componentId, String rootDirPath,
          boolean installInRootDir, MessageRouter msgRouter,
          MessageRouter.StdChannelListener msgListener, boolean cleanInstallDir) {
    if (msgRouter == null)
      _msgRouter = new MessageRouter();
    else
      _msgRouter = msgRouter;
    if (msgListener == null) {
      // set default standard message channel listener
      _defaultMsgListener = new MessageRouter.StdChannelListener() {
        public void errMsgPosted(String errMsg) {
          System.err.print(errMsg);
          System.err.flush();
        }

        public void outMsgPosted(String outMsg) {
          System.out.print(outMsg);
          System.out.flush();
        }
      };
    } else
      // set custom standard message channel listener
      _defaultMsgListener = msgListener;
    addMsgListener(_defaultMsgListener);
    if (!_msgRouter.isRunning()) // start messenger
      _msgRouter.start();
    // initialize attributes
    _mainComponentId = componentId;
    _cleanInstallDir = cleanInstallDir;
    if (installInRootDir) {
      _mainComponentRootPath = rootDirPath;
      _mainComponentRoot = new File(_mainComponentRootPath);
      _installationDir = _mainComponentRoot.getParentFile();
      _installationDirPath = _installationDir.getAbsolutePath();
    } else {
      _installationDirPath = rootDirPath;
      _installationDir = new File(_installationDirPath);
      _mainComponentRoot = new File(_installationDir, componentId);
      _mainComponentRootPath = _mainComponentRoot.getAbsolutePath();
    }
    _uimaHomePath = System.getProperty(UIMA_HOME_ENV);
    if (_uimaHomePath != null)
      _uimaHomePath = _uimaHomePath.replace('\\', '/');
    if (__osName == null)
      __osName = System.getProperty("os.name");
    _hostIpAddress = getHostIpAddress();
    // set default package selector
    _packageSelector = new SimplePackageSelector(getOutMsgWriter(), getErrMsgWriter());
  }

  /**
   * Constructor for the 'local' mode, which specifies component ID, local PEAR file and a local
   * root directory where the component will be installed. By default, the
   * <code>InstallationController</code> creates a <code>component_id</code> subdirectory for
   * the component code and resources. By default, the <code>InstallationController</code> class
   * sends all stdout and stderr messages to the default message listener, which prints them to the
   * standard console streams.
   * 
   * @param componentId
   *          The given component ID.
   * @param localPearFile
   *          The given local PEAR file.
   * @param rootDir
   *          The given local root directory for installation.
   */
  public InstallationController(String componentId, File localPearFile, File rootDir) {
    this(componentId, rootDir.getAbsolutePath(), false, null, null, true);
    _mainPearFileLocation = localPearFile.getAbsolutePath();
  }
  
  /**
   * Constructor for the 'local' mode, which specifies component ID, local PEAR file and a local
   * root directory where the component will be installed. If the <code>installInRootDir</code>
   * flag is <code>true</code>, the component code and resources will be installed in the
   * specified root directory, otherwise the <code>InstallationController</code> will create a
   * <code>component_id</code> subdirectory for the component code and resources. By default, the
   * <code>InstallationController</code> class sends all stdout and stderr messages to the default
   * message listener, which prints them to the standard console streams.
   * 
   * @param componentId
   *          The given component ID.
   * @param localPearFile
   *          The given local PEAR file.
   * @param rootDir
   *          The given local root directory for installation.
   * @param installInRootDir
   *          If <code>true</code>, the component will be installed in the given root directory,
   *          otherwise it will be installed in the <code>component_id</code> subdirectory of the
   *          root directory.
   * @param cleanInstallDir
   *          If <code>true</code>, the target installation directory will be cleaned before the
   *          PEAR file is installed.
   */
  public InstallationController(String componentId, File localPearFile, File rootDir,
          boolean installInRootDir, boolean cleanInstallDir) {
    this(componentId, rootDir.getAbsolutePath(), installInRootDir, null, null, cleanInstallDir);
    _mainPearFileLocation = localPearFile.getAbsolutePath();
  }


  /**
   * Constructor for the 'local' mode, which specifies component ID, local PEAR file and a local
   * root directory where the component will be installed. If the <code>installInRootDir</code>
   * flag is <code>true</code>, the component code and resources will be installed in the
   * specified root directory, otherwise the <code>InstallationController</code> will create a
   * <code>component_id</code> subdirectory for the component code and resources. By default, the
   * <code>InstallationController</code> class sends all stdout and stderr messages to the default
   * message listener, which prints them to the standard console streams.
   * 
   * @param componentId
   *          The given component ID.
   * @param localPearFile
   *          The given local PEAR file.
   * @param rootDir
   *          The given local root directory for installation.
   * @param installInRootDir
   *          If <code>true</code>, the component will be installed in the given root directory,
   *          otherwise it will be installed in the <code>component_id</code> subdirectory of the
   *          root directory. Note: the installation directory will be cleaned before the PEAR file is 
   *          installed to it. 
   */
  public InstallationController(String componentId, File localPearFile, File rootDir,
          boolean installInRootDir) {
    this(componentId, rootDir.getAbsolutePath(), installInRootDir, null, null, true);
    _mainPearFileLocation = localPearFile.getAbsolutePath();
  }

  /**
   * Constructor for the 'local' mode, which specifies component ID, local PEAR file and a local
   * root directory where the component will be installed. If the <code>installInRootDir</code>
   * flag is <code>true</code>, the component code and resources will be installed in the
   * specified root directory, otherwise the <code>InstallationController</code> will create a
   * <code>component_id</code> subdirectory for the component code and resources. If the custom
   * message listener is not <code>null</code>, the <code>InstallationController</code> class
   * sends all stdout and stderr messages to this message listener, otherwise these messages are
   * sent to the default message listener, which prints them to the standard console streams.
   * 
   * @param componentId
   *          The given component ID.
   * @param localPearFile
   *          The given local PEAR file.
   * @param rootDir
   *          The given local root directory for installation.
   * @param installInRootDir
   *          If <code>true</code>, the component will be installed in the given root directory,
   *          otherwise it will be installed in the <code>component_id</code> subdirectory of the
   *          root directory.
   * @param msgListener
   *          The given custom message listener or <code>null</code>.
   */
  public InstallationController(String componentId, File localPearFile, File rootDir,
          boolean installInRootDir, MessageRouter.StdChannelListener msgListener) {
    this(componentId, rootDir.getAbsolutePath(), installInRootDir, null, msgListener, true);
    _mainPearFileLocation = localPearFile.getAbsolutePath();
  }

  /**
   * Constructor for the 'local' mode, which specifies component ID, local PEAR file and a local
   * root directory where the component will be installed. If the <code>installInRootDir</code>
   * flag is <code>true</code>, the component code and resources will be installed in the
   * specified root directory, otherwise the <code>InstallationController</code> will create a
   * <code>component_id</code> subdirectory for the component code and resources. If the custom
   * message listener is not <code>null</code>, the <code>InstallationController</code> class
   * sends all stdout and stderr messages to this message listener, otherwise these messages are
   * sent to the default message listener, which prints them to the standard console streams.
   * 
   * @param componentId
   *          The given component ID.
   * @param localPearFile
   *          The given local PEAR file.
   * @param rootDir
   *          The given local root directory for installation.
   * @param installInRootDir
   *          If <code>true</code>, the component will be installed in the given root directory,
   *          otherwise it will be installed in the <code>component_id</code> subdirectory of the
   *          root directory.
   * @param msgListener
   *          The given custom message listener or <code>null</code>.
   * @param cleanInstallDir
   *          If <code>true</code>, the target installation directory will be cleaned before the
   *          PEAR file is installed.
   */
  public InstallationController(String componentId, File localPearFile, File rootDir,
          boolean installInRootDir, MessageRouter.StdChannelListener msgListener,
          boolean cleanInstallDir) {
    this(componentId, rootDir.getAbsolutePath(), installInRootDir, null, msgListener,
            cleanInstallDir);
    _mainPearFileLocation = localPearFile.getAbsolutePath();
  }

  /**
   * Adds a given object, implementing the <code>MessageRouter.StdChannelListener</code> interface
   * to the list of standard channel listeners.
   * 
   * @param listener
   *          The given <code>MessageRouter.StdChannelListener</code> object to be added to the
   *          list.
   */
  public void addMsgListener(MessageRouter.StdChannelListener listener) {
    _msgRouter.addChannelListener(listener);
  }

  /**
   * Builds <code>CLASSPATH</code> for the installed component, including <code>CLASSPATH</code>
   * for all separate delegate components that are utilized by the main installed component, if any.
   * 
   * @return The <code>CLASSPATH</code> for the installed component, or <code>null</code>, if
   *         the component has not been installed.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public String buildComponentClassPath() throws IOException {
    if (_insdObject != null) {
      StringBuffer cpBuffer = new StringBuffer();
      // build main component classpath
      String mainClassPath = buildComponentClassPath(_mainComponentRootPath, _insdObject, true);
      cpBuffer.append(mainClassPath);
      // add component classpath for possible delegate components
      if (_installationTable.size() > 0) {
        Enumeration<String> dlgIdList = _installationTable.keys();
        while (dlgIdList.hasMoreElements()) {
          // process next delegate component
          String dlgId = dlgIdList.nextElement();
          String dlgRootPath = _installationTable.get(dlgId);
          InstallationDescriptor dlgInsD = _installationInsDs.get(dlgId);
          String dlgClassPath = buildComponentClassPath(dlgRootPath, dlgInsD, true);
          if (dlgClassPath.length() > 0) {
            if (cpBuffer.length() > 0
                    && cpBuffer.charAt(cpBuffer.length() - 1) != File.pathSeparatorChar)
              cpBuffer.append(File.pathSeparatorChar);
            cpBuffer.append(dlgClassPath);
          }
        }
      }
      return cpBuffer.toString();
    }
    return null;
  }

  /**
   * Builds <code>PATH</code> for the installed component, including <code>PATH</code> for all
   * separate delegate components that are utilized by the main installed component, if any.
   * 
   * @return The <code>PATH</code> for the installed component, or <code>null</code>, if the
   *         component has not been installed.
   */
  public String buildComponentPath() {
    if (_insdObject != null) {
      StringBuffer pBuffer = new StringBuffer();
      // build main component path
      String mainPath = buildComponentPath(_mainComponentRootPath, _insdObject);
      pBuffer.append(mainPath);
      // add component path for possible delegate components
      if (_installationTable.size() > 0) {
        Enumeration<String> dlgIdList = _installationTable.keys();
        while (dlgIdList.hasMoreElements()) {
          // process next delegate component
          String dlgId = dlgIdList.nextElement();
          String dlgRootPath = _installationTable.get(dlgId);
          InstallationDescriptor dlgInsD = _installationInsDs.get(dlgId);
          String dlgPath = buildComponentPath(dlgRootPath, dlgInsD);
          if (dlgPath.length() > 0) {
            if (pBuffer.length() > 0
                    && pBuffer.charAt(pBuffer.length() - 1) != File.pathSeparatorChar)
              pBuffer.append(File.pathSeparatorChar);
            pBuffer.append(dlgPath);
          }
        }
      }
      return pBuffer.toString();
    }
    return null;
  }

  /**
   * Builds <code>Properties</code> table of required environment variables for the installed
   * component, including environment variables for all separate delegate components that are
   * utilized by the main installed component, if any.
   * 
   * @return <code>Properties</code> table of required environment variables for the installed
   *         component, or <code>null</code>, if the component has not been installed.
   */
  public Properties buildTableOfEnvVars() {
    if (_insdObject != null) {
      // set required env vars for main component
      Properties envVars = buildTableOfEnvVars(_insdObject);
      // add required env vars for possible delegate components
      if (_installationTable.size() > 0) {
        Enumeration<String> dlgIdList = _installationTable.keys();
        while (dlgIdList.hasMoreElements()) {
          // process next delegate component
          String dlgId = dlgIdList.nextElement();
          InstallationDescriptor dlgInsD = _installationInsDs.get(dlgId);
          Properties dlgEnvVars = buildTableOfEnvVars(dlgInsD);
          envVars = StringUtil.appendProperties(envVars, dlgEnvVars, false);
        }
      }
      return envVars;
    }
    return null;
  }

  /**
   * Overrides standard <code>finalize</code> method.
   */
  protected void finalize() {
    _msgRouter.terminate();
  }

  /**
   * Performs installation of the specified component in the specified target directory, including
   * all delegate components (if exist). If the installation completed successfully, returns the
   * <code>InstallationDescriptor</code> object for the installed component. If the installation
   * failed, returns <code>null</code>, and sets the installation error message that can be
   * retrieved using the <code>getInstallationMsg()</code> method.
   * 
   * @return The <code>InstallationDescriptor</code> object for the installed component, if the
   *         installation succeeded, <code>null</code> otherwise.
   */
  public synchronized InstallationDescriptor installComponent() {
    try {
      if (_installationMonitor != null) // notify installation monitor
        _installationMonitor.setInstallationStatus(_mainComponentId, INSTALLATION_IN_PROGRESS);
      if (_mainPearFileLocation == null) // get PEAR file location
        _mainPearFileLocation = getPEARFileLocation(_mainComponentId, _packageSelector);
      // extract PEAR file in a specified directory
      if (extractPEARFile(_mainPearFileLocation, _mainComponentRoot, this, _cleanInstallDir) == null) {
        // PEAR extraction failed
        // set error message
        setInstallationError(new IOException("PEAR extraction failed"));
        if (_installationMonitor != null) // notify monitor
          _installationMonitor.setInstallationStatus(_mainComponentId, INSTALLATION_FAILED);
        return null;
      }
      // load installation descriptor
      InstallationDescriptorHandler insdHandler = new InstallationDescriptorHandler();
      File insdFile = new File(_mainComponentRoot, InstallationProcessor.INSD_FILE_PATH);
      insdHandler.parse(insdFile);
      _insdObject = insdHandler.getInstallationDescriptor();
      // install separate delegate components
      installDelegateComponents();
      // complete installation process for main component
      InstallationProcessor processor = new InstallationProcessor(_mainComponentRootPath,
              _installationTable, this);
      processor.process();
      _insdObject = processor.getInstallationDescriptor();
      // save modified installation descriptor file
      saveInstallationDescriptorFile();
      // generate PEAR.properties file
      generatePackageConfigFile();
      // generate 'setenv.bat' file
      generateSetEnvFile();
      generatePearSpecifier(_mainComponentRootPath, _mainComponentId);
      getOutMsgWriter().println(
              "[InstallationController]: " + "the " + SET_ENV_FILE + " file contains required "
                      + "environment variables for this component");
      getOutMsgWriter().println(
              "[InstallationController]: component " + _mainComponentId
                      + " installation completed.");
      if (_installationMonitor != null) {
        // notify installation monitor
        _installationMonitor.setInstallationLocation(_mainComponentId, _mainComponentRootPath);
        _installationMonitor.setInstallationStatus(_mainComponentId, INSTALLATION_COMPLETED);
      }
    } catch (Exception exc) {
      getErrMsgWriter().println("Error in InstallationController: " + exc);
      exc.printStackTrace(getErrMsgWriter());
      // set error message
      setInstallationError(exc);
      if (_installationMonitor != null) // notify monitor
        _installationMonitor.setInstallationStatus(_mainComponentId, INSTALLATION_FAILED);
      return null;
    }
    return _insdObject;
  }

  /**
   * Performs installation of XML descriptors of the specified component in the specified target
   * directory, including XML descriptors of all the delegate components (if exist). If the
   * installation completed successfully, returns the <code>InstallationDescriptor</code> object
   * for the partially installed component. If the installation failed, returns <code>null</code>,
   * and sets the installation error message that can be retrieved using the
   * <code>getInstallationMsg()</code> method.
   * 
   * @return The <code>InstallationDescriptor</code> object for the partially installed component,
   *         if the installation succeeded, <code>null</code> otherwise.
   */
  public synchronized InstallationDescriptor installComponentDescriptors() {
    try {
      if (_mainPearFileLocation == null) // get PEAR file location
        _mainPearFileLocation = getPEARFileLocation(_mainComponentId, _packageSelector);
      // extract main XML descriptors in a specified directory
      if (extractFilesFromPEARFile(_mainPearFileLocation, ".xml", _mainComponentRoot, this,
              _cleanInstallDir) == null) {
        // PEAR extraction failed
        // set error message
        setInstallationError(new IOException("PEAR extraction failed"));
        return null;
      }
      // load installation descriptor
      InstallationDescriptorHandler insdHandler = new InstallationDescriptorHandler();
      File insdFile = new File(_mainComponentRoot, InstallationProcessor.INSD_FILE_PATH);
      insdHandler.parse(insdFile);
      _insdObject = insdHandler.getInstallationDescriptor();
      // install XML descriptors of separate delegate components
      installDelegateComponentsDescriptors();
      // complete installation process for main component
      InstallationProcessor processor = new InstallationProcessor(_mainComponentRootPath,
              _installationTable);
      processor.process();
      _insdObject = processor.getInstallationDescriptor();
      getOutMsgWriter().println(
              "[InstallationController]: component " + _mainComponentId
                      + " descriptors installation completed.");
    } catch (Exception exc) {
      getErrMsgWriter().println("Error in InstallationController: " + exc);
      exc.printStackTrace(getErrMsgWriter());
      // set error message
      setInstallationError(exc);
      return null;
    }
    return _insdObject;
  }

  /**
   * Performs installation of all separate delegate components for the specified main component.
   * 
   */
  protected synchronized void installDelegateComponents() {
    // get list of separate delegate components IDs
    Enumeration<String> dlgList = _insdObject.getDelegateComponents().keys();
    while (dlgList.hasMoreElements()) {
      // get next separate delegate component ID
      String componentId = dlgList.nextElement();
      // check if the same component is available (not in use)
      String componentRootPath = null;
      try {
        componentRootPath = getInstalledComponentRootPath(componentId, _packageSelector);
      } catch (Exception e) {
        getErrMsgWriter().println(
                "[InstallationController]: " + "failed to query " + componentId + " location - "
                        + e);
      }
      if (componentRootPath == null) {
        // install next separate delegate component
        InstallationController dlgController = new InstallationController(componentId,
                _installationDirPath, false, this._msgRouter, this._defaultMsgListener,
                _cleanInstallDir);
        dlgController.setPackageSelector(this._packageSelector);
        InstallationDescriptor dlgInsdObject = dlgController.installComponent();
        if (dlgInsdObject == null) {
          getErrMsgWriter().println(
                  "[InstallationController]: " + "failed to install dlg component " + componentId);
          throw new RuntimeException("failed to install dlg component " + componentId);
        }
        componentRootPath = dlgInsdObject.getMainComponentRoot();
        // add installation info to the table
        _installationTable.put(componentId, componentRootPath);
        _installationInsDs.put(componentId, dlgInsdObject);
      } else {
        // add installation info to the table
        _installationTable.put(componentId, componentRootPath);
        // get InsD object for this component
        try {
          PackageBrowser dlgBrowser = new PackageBrowser(new File(componentRootPath));
          InstallationDescriptor dlgInsdObject = dlgBrowser.getInstallationDescriptor();
          _installationInsDs.put(componentId, dlgInsdObject);
        } catch (IOException e) {
          // this should never happen
        }
      }
    }
  }

  /**
   * Performs installation of XML descriptors for all separate delegate components of the specified
   * main component.
   */
  protected synchronized void installDelegateComponentsDescriptors() {
    // get list of separate delegate components IDs
    Enumeration<String> dlgList = _insdObject.getDelegateComponents().keys();
    while (dlgList.hasMoreElements()) {
      // get next separate delegate component ID
      String componentId = dlgList.nextElement();
      // install XML descriptors of the next delegate component
      InstallationController dlgController = new InstallationController(componentId,
              _installationDirPath, false, this._msgRouter, this._defaultMsgListener,
              _cleanInstallDir);
      dlgController.setPackageSelector(this._packageSelector);
      InstallationDescriptor dlgInsdObject = dlgController.installComponentDescriptors();
      if (dlgInsdObject == null) {
        getErrMsgWriter().println(
                "[InstallationController]: " + "failed to install descriptors for dlg component "
                        + componentId);
        throw new RuntimeException("failed to install descriptors for dlg component " + componentId);
      }
      String componentRootPath = dlgInsdObject.getMainComponentRoot();
      // add installation info to the table
      _installationTable.put(componentId, componentRootPath);
      _installationInsDs.put(componentId, dlgInsdObject);
    }
  }

  /**
   * generates the pearSpecifier to run the installed pear component. The descriptor that is created
   * has the filename &lt;componentID&gt;_pear.xml and is created in the main component root
   * directory. If the file already exist, it will be overridden.
   * 
   * @param mainComponentRootPath
   *          main component root path where the pear was installed to
   * 
   * @param mainComponentId
   *          main component ID of the installed pear file
   * 
   * @throws IOException if IO Exception
   * @throws SAXException if SAX Exception
   */
  protected static synchronized void generatePearSpecifier(String mainComponentRootPath,
          String mainComponentId) throws IOException, SAXException {
    PearSpecifier pearSpec = UIMAFramework.getResourceSpecifierFactory().createPearSpecifier();
    pearSpec.setPearPath(mainComponentRootPath);
    File outputFile = new File(mainComponentRootPath, mainComponentId + PEAR_DESC_FILE_POSTFIX);
    FileOutputStream fos = null;

    try
    {
        fos = new FileOutputStream(outputFile);
        pearSpec.toXML(fos);
    }
    finally
    {
        if (fos != null)
        {
            fos.close();
        }
    }
  }

  /**
   * Generates the file (batch file for Windows) containing specific environment variables that
   * should be used to run the component.
   * 
   * @throws IOException
   *           if any I/O exception occurred.
   */
  protected synchronized void generateSetEnvFile() throws IOException {
    File setEnvFile = new File(_mainComponentRoot, SET_ENV_FILE);
    PrintWriter fWriter = null;
    try {
      fWriter = new PrintWriter(new FileWriter(setEnvFile));
      fWriter.println("### Add the following environment variables");
      fWriter.println("### to appropriate existing environment variables");
      fWriter.println("### to run the " + _mainComponentId + " component");
      fWriter.println();
      // CLASSPATH
      String classPath = buildComponentClassPath();
      if (classPath.length() > 0)
        fWriter.println("CLASSPATH=" + classPath);
      // PATH
      String path = buildComponentPath();
      if (path.length() > 0)
        fWriter.println("PATH=" + path);
      // the rest of env.vars.
      Properties envVarTable = buildTableOfEnvVars();
      Enumeration<Object> envVarList = envVarTable.keys();
      while (envVarList.hasMoreElements()) {
        String varName = (String) envVarList.nextElement();
        String varValue = envVarTable.getProperty(varName);
        // add env.var. setting
        if (varName.length() > 0 && varValue.length() > 0
                && !varName.equalsIgnoreCase(CLASSPATH_VAR) && !varName.equalsIgnoreCase(PATH_VAR)) {
          fWriter.println(varName + "=" + varValue);
        }
      }
    } finally {
      if (fWriter != null) {
        try {
          fWriter.close();
        } catch (Exception e) {
          // ignore close exception
        }
      }
    }
  }

  /**
   * Generates/updates the PEAR configuration file setting the main component root directory, as
   * well as root directories of all related delegate components.
   * 
   * @throws IOException
   *           if any I/O exception occurred.
   */
  protected synchronized void generatePackageConfigFile() throws IOException {
    Properties packageConfig = new Properties();
    File packageConfigFile = new File(_mainComponentRoot, PACKAGE_CONFIG_FILE);
    if (packageConfigFile.exists()) {
      // loading existing pear config file
      InputStream iStream = null;
      try {
        iStream = new FileInputStream(packageConfigFile);
        packageConfig.load(iStream);
      } finally {
        if (iStream != null) {
          try {
            iStream.close();
          } catch (Exception e) {
            // ignore close exception
          }
        }
      }
    }
    // set local config params
    packageConfig.setProperty(LocalInstallationAgent.MAIN_ROOT, _mainComponentRootPath.replace(
            '\\', '/'));
    Iterator<String> dlgIdList = _installationTable.keySet().iterator();
    while (dlgIdList.hasNext()) {
      String id = dlgIdList.next();
      String idRoot = LocalInstallationAgent.COMP_ROOT_PREFIX + id
              + LocalInstallationAgent.COMP_ROOT_SUFFIX;
      packageConfig.setProperty(idRoot, _installationTable.get(id).replace('\\', '/'));
    }
    // save pear config file
    OutputStream oStream = null;
    try {
      String header = _mainComponentId;
      oStream = new FileOutputStream(packageConfigFile);
      packageConfig.store(oStream, header);
    } finally {
      if (oStream != null) {
        try {
          oStream.close();
        } catch (Exception e) {
          // ignore close exception
        }
      }
    }
  }

  /**
   * @return Error message writer for intraprocess messaging.
   */
  protected PrintWriter getErrMsgWriter() {
    return _msgRouter.errWriter();
  }

  /**
   * @return The installation message (error message).
   */
  public String getInstallationMsg() {
    return _installationMsg;
  }

  /**
   * @return Output message writer for intraprocess messaging.
   */
  protected PrintWriter getOutMsgWriter() {
    return _msgRouter.outWriter();
  }

  /**
   * @return The verification message (error message).
   */
  public String getVerificationMsg() {
    return _verificationMsg;
  }

  /**
   * Removes a given <code>MessageRouter.StdChannelListener</code> object from the list of
   * standard channel listeners.
   * 
   * @param listener
   *          The given <code>MessageRouter.StdChannelListener</code> object to be removed from
   *          the list.
   */
  public void removeMsgListener(MessageRouter.StdChannelListener listener) {
    _msgRouter.removeChannelListener(listener);
  }

  /**
   * Saves modified installation descriptor file.
   * 
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public synchronized void saveInstallationDescriptorFile() throws IOException {
    if (_insdObject != null) {
      File insdFile = _insdObject.getInstallationDescriptorFile();
      InstallationDescriptorHandler.saveInstallationDescriptor(_insdObject, insdFile);
    }
  }

  /**
   * Prints the stack trace of a given <code>Exception</code> object as the installation error
   * message.
   * 
   * @param error
   *          The given <code>Exception</code> object.
   */
  protected synchronized void setInstallationError(Exception error) {
    _installationMsg = StringUtil.errorStackTraceContent(error);
  }

  /**
   * Plugs-in a given implementation of the <code>InstallationMonitor</code> interface.
   * 
   * @param monitor
   *          The given implementation of the <code>InstallationMonitor</code> interface.
   */
  public synchronized void setInstallationMonitor(InstallationMonitor monitor) {
    if (monitor != null)
      _installationMonitor = monitor;
  }

  /**
   * Plugs-in a given implementation of the <code>PackageSelector</code> interface.
   * 
   * @param selector
   *          The given implementation of the <code>PackageSelector</code> interface.
   */
  public synchronized void setPackageSelector(PackageSelector selector) {
    if (selector != null)
      _packageSelector = selector;
  }

  /**
   * Prints the stack trace of a given <code>Exception</code> object as the verification error
   * message.
   * 
   * @param error
   *          The given <code>Exception</code> object.
   */
  protected synchronized void setVerificationError(Exception error) {
    _verificationMsg = StringUtil.errorStackTraceContent(error);
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

  /**
   * Terminates the <code>MessageRouter</code> thread. This method should be called after all the
   * processing is finished.
   */
  public void terminate() {
    _msgRouter.terminate();
  }

  /**
   * Verifies installations of the main component, and sets appropriate component status in the SITH
   * DB.
   * 
   * @return <code>true</code> if the verification completed successfully, <code>false</code>
   *         otherwise.
   */
  public synchronized boolean verifyComponent() {
    boolean success = false;
    try {
      if (_installationMonitor != null) // notify monitor
        _installationMonitor.setInstallationStatus(_mainComponentId, VERIFICATION_IN_PROGRESS);

      // create PackageBrowser object for the installed PEAR
      PackageBrowser installedPear = new PackageBrowser(this._mainComponentRoot);
      TestStatus status = verifyComponentInstallation(installedPear);
      if (status.getRetCode() == TestStatus.TEST_SUCCESSFUL) {
        // verification successful
        success = true;
        _verificationMsg = null;
        if (_installationMonitor != null) // notify monitor
          _installationMonitor.setInstallationStatus(_mainComponentId, VERIFICATION_COMPLETED);
      } else if (status.getRetCode() == TestStatus.TEST_NOT_SUCCESSFUL) {
        // verification failed
        _verificationMsg = status.getMessage();
        if (_installationMonitor != null) // notify monitor
          _installationMonitor.setInstallationStatus(_mainComponentId, VERIFICATION_FAILED);
      } else {
        // verification cancelled
        _verificationMsg = status.getMessage();
        if (_installationMonitor != null) // notify monitor
          _installationMonitor.setInstallationStatus(_mainComponentId, VERIFICATION_CANCELLED);
      }
    } catch (Exception err) {
      _verificationMsg = err.toString();
      if (_installationMonitor != null) // notify monitor
        _installationMonitor.setInstallationStatus(_mainComponentId, VERIFICATION_FAILED);
    }
    return success;
  }
}
