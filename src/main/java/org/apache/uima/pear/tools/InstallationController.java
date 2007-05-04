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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.impl.PearAnalysisEngineWrapper;
import org.apache.uima.internal.util.SystemEnvReader;
import org.apache.uima.pear.util.FileUtil;
import org.apache.uima.pear.util.MessageRouter;
import org.apache.uima.pear.util.StringUtil;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.PearSpecifier;
import org.xml.sax.SAXException;

/**
 * The <code>InstallationController</code> class allows installing PEAR files that contain UIMA
 * compliant components. <br />
 * <b>Note:</b> current version works both in Windows and Linux.
 * 
 * <br />
 * This class may be used in the following ways:
 * <ul>
 * <li>As a standalone Java application - <br />
 * <code>
 * java -DUIMA_HOME=%UIMA_HOME% org.apache.uima.pear.tools.InstallationController 
 * {-local pear_file | component_id} [-root] [installation_directory]
 * </code><br />
 * where the <code>-local pear_file</code> option allows to install local PEAR file in the local
 * file system (without using SITH services); <br />
 * the <code>component_id</code> is the ID of the component to be installed using SITH services;
 * <br />
 * the <code>-root</code> option enables component installation directly in the specified
 * installation directory, as opposed to installing component in a <code>component_id</code>
 * subdirectory of the specified installation directory; <br />
 * the <code>installation_directory</code> is the directory where the new component will be
 * installed - if the <code>-root</code> option is specified, the component is installed in this
 * directory, otherwise it is installed in a <code>component_id</code> subdirectory of this
 * directory; by default - current working directory. </li>
 * <li>As a Java object - <br />
 * in this case, the caller is expected to set the <code>UIMA_HOME</code> variable, using the
 * <code>setUimaHomePath()</code> method, immediately after creating a new instance of the
 * <code>InstallationController</code> class. <br />
 * Installation is performed by using the <code>installComponent()</code> method. <br />
 * Installation verification is performed by using the <code>verifyComponent()</code> method.
 * <br />
 * Error messages can be retrieved by using the <code>getInstallationMsg()</code> and
 * <code>getVerificationMsg()</code> methods. <br />
 * <b>Note 1:</b> Starting from version 0.6, the <code>InstallationController</code> class
 * utilizes intra-process message routing (see <code>{@link MessageRouter}</code> class).
 * Applications need to call the <code>terminate()</code> method on each instance of the
 * <code>InstallationController</code> class after all their operations are completed. <br />
 * The application can get output and error messages, printed by the
 * <code>InstallationController</code>, by adding standard channel listeners (see the
 * <code>addMsgListener()</code> method). By default, the output and error messages are printed to
 * the standard console streams. Alternatively, the application can use the
 * <code>InstallationController</code> constructor that accepts a custom message listener. In this
 * case, the output and error messages will not be printed to the standard console streams. <br />
 * <b>Note 2:</b> Starting from version 1.4, the <code>InstallationController</code> class
 * defines the <code>{@link PackageSelector}</code> interface and allows to plug-in custom package
 * selectors for manually or automatically selecting root directories of installed PEAR packages, as
 * well as PEAR package files that need to be installed. <br />
 * <b>Note 2:</b> Starting from version 1.5, the <code>InstallationController</code> class
 * defines the <code>{@link InstallationMonitor}</code> interface and allows to plug-in custom
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
     *          Current installation status of the given component. <br />
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
    // public attributes
    public int retCode;

    public String message;
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

  private String _mainComponentRootPath;

  private File _mainComponentRoot;

  private String _mainPearFileLocation = null;

  private Hashtable _installationTable = new Hashtable();

  private Hashtable _installationInsDs = new Hashtable();

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
      Collection fileList = FileUtil.createFileList(libDir);
      Iterator files = fileList.iterator();
      while (files.hasNext()) {
        File file = (File) files.next();
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
    Enumeration sysKeys = sysEnvTable.keys();
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
    ArrayList paramsList = new ArrayList();
    StringBuffer itemBuffer = new StringBuffer();
    Set pNames = insdObject.getMainComponentNetworkParamNames();
    // go through specified parameters and add them to the list
    if (pNames != null) {
      Iterator pList = pNames.iterator();
      while (pList.hasNext()) {
        String pName = (String) pList.next();
        Properties param = insdObject.getMainComponentNetworkParam(pName);
        Enumeration keys = param.keys();
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
   * @return The string that should be added to the CLASSPATH for the given component.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static String buildComponentClassPath(String compRootDirPath,
          InstallationDescriptor insdObject) throws IOException {
    // create list of JAR files in lib dir.
    File compLibDir = new File(compRootDirPath + "/" + PACKAGE_LIB_DIR);
    StringBuffer cpBuffer = new StringBuffer();
    cpBuffer = addListOfJarFiles(compLibDir, cpBuffer);
    // append all specified CLASSPATH env.var. settings
    Iterator envActions = insdObject.getInstallationActions(
            InstallationDescriptor.ActionInfo.SET_ENV_VARIABLE_ACT).iterator();
    while (envActions.hasNext()) {
      // if env.var.name is CLASSPATH, append value to the buffer
      InstallationDescriptor.ActionInfo actInfo = (InstallationDescriptor.ActionInfo) envActions
              .next();
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
    File compBinDir = new File(compRootDirPath + "/" + PACKAGE_BIN_DIR);
    StringBuffer pBuffer = new StringBuffer();
    if (compBinDir.isDirectory()) {
      pBuffer.append(compBinDir.getAbsolutePath().replace('\\', '/'));
    }
    // append all specified PATH env.var. settings
    Iterator envActions = insdObject.getInstallationActions(
            InstallationDescriptor.ActionInfo.SET_ENV_VARIABLE_ACT).iterator();
    while (envActions.hasNext()) {
      // if env.var.name is PATH, append value to the buffer
      InstallationDescriptor.ActionInfo actInfo = (InstallationDescriptor.ActionInfo) envActions
              .next();
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
    Enumeration names = envVarsTable.keys();
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
    Iterator envActions = insdObject.getInstallationActions(
            InstallationDescriptor.ActionInfo.SET_ENV_VARIABLE_ACT).iterator();
    while (envActions.hasNext()) {
      // add env.var. settings to the table
      InstallationDescriptor.ActionInfo actInfo = (InstallationDescriptor.ActionInfo) envActions
              .next();
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
      Hashtable dlgComponents = insdObject.getDelegateComponents();
      Enumeration dlgCompIds = dlgComponents.keys();
      while (dlgCompIds.hasMoreElements()) {
        String dlgCompId = (String) dlgCompIds.nextElement();
        if (!deleteInstalledFiles(dlgCompId, parentDir, true))
          done = false;
      }
    }
    if (!FileUtil.deleteDirectory(rootDir))
      done = false;
    return done;
  }

  /**
   * Creates command for running the installation test for a given installed component, including
   * possible separately installed delegate components, runs the test, and returns the
   * <code>TestStatus</code> object with the test results.
   * 
   * @param mainRootDirPath
   *          The given main component root directory path.
   * @param mainInsD
   *          The given main component <code>InstallationDescriptor</code> object.
   * @param mainDescPath
   *          The given main component XML descriptor file path.
   * @param compClassPath
   *          The given CLASSPATH for the installed component, including CLASSPATH for possible
   *          delegate components.
   * @param javaLibPath
   *          The given PATH for the installed component, including PATH for possible delegate
   *          components.
   * @param tableOfEnvVars
   *          The given table of required environment variables for the installed component,
   *          including possible delegate components.
   * @param uimaClassPath
   *          The required UIMA CLASSPATH.
   * @return The instance of the <code>TestStatus</code> class that contains the return code and
   *         possible error message of the given command.
   * @throws IOException
   *           if any I/O error occurred.
   */
  protected static TestStatus deployInstallationVerificationTest(String mainRootDirPath,
          InstallationDescriptor mainInsD, String mainDescPath, String compClassPath,
          String javaLibPath, Properties tableOfEnvVars, String uimaClassPath) throws IOException {
    // build command array to run installation tester app.
    ArrayList cmdArrayList = new ArrayList();
    StringBuffer cmdBuffer = new StringBuffer();
    // set Java executable path - OS dependent
    String osName = System.getProperty("os.name");
    String javaHome = System.getProperty("java.home");
    String javaExeName = (osName.indexOf("Windows") >= 0) ? "java.exe" : "java";
    String javaExePath = null;
    File javaExeFile = null;
    // 1st - try in 'java.home'/bin folder
    javaExePath = javaHome + java.io.File.separator + "bin" + java.io.File.separator + javaExeName;
    javaExeFile = new File(javaExePath);
    if (!javaExeFile.isFile()) {
      // 2nd - try in 'java.home'/jre/bin folder
      javaExePath = javaHome + java.io.File.separator + "jre" + java.io.File.separator + "bin"
              + java.io.File.separator + javaExeName;
      javaExeFile = new java.io.File(javaExePath);
    }
    cmdArrayList.add(javaExeFile.getAbsolutePath());
    // specify heap size
    cmdArrayList.add("-Xmx512M");
    // specify classpath
    cmdArrayList.add("-cp");
    cmdBuffer.setLength(0);
    cmdBuffer.append(compClassPath);
    cmdBuffer.append(File.pathSeparatorChar);
    cmdBuffer.append(uimaClassPath);
    cmdArrayList.add(cmdBuffer.toString());
    // specify java.library.path
    if (javaLibPath.length() > 0) {
      cmdBuffer.setLength(0);
      cmdBuffer.append("-Djava.library.path=");
      cmdBuffer.append(javaLibPath);
      cmdArrayList.add(cmdBuffer.toString());
    }
    // for 'network' AEs: add network parameters
    if (mainInsD.getMainComponentDeployment().equals(InstallationDescriptorHandler.NETWORK_TAG)) {
      String[] networkParams = buildArrayOfNetworkParams(mainInsD);
      for (int i = 0; i < networkParams.length; i++)
        cmdArrayList.add(networkParams[i]);
    }
    // specify env vars and create array
    ArrayList envArrayList = new ArrayList();
    // get global system env vars
    Properties tableOfSysEnvVars = new Properties();
    try {
      tableOfSysEnvVars = SystemEnvReader.getEnvVars();
    } catch (Throwable err) {
      throw new IOException("system environment error: " + err.toString());
    }
    // add local CLASSPATH, PATH and LD_LIBRARY_PATH to system env
    String localClasspath = tableOfEnvVars.getProperty("CLASSPATH");
    if (localClasspath != null && localClasspath.length() > 0) {
      if (addToSystemEnvTable(tableOfSysEnvVars, "CLASSPATH", localClasspath))
        tableOfEnvVars.remove("CLASSPATH");
    }
    String localPath = tableOfEnvVars.getProperty("PATH");
    if (localPath != null && localPath.length() > 0) {
      if (addToSystemEnvTable(tableOfSysEnvVars, "PATH", localPath))
        tableOfEnvVars.remove("PATH");
    }
    String localLdlPath = tableOfEnvVars.getProperty("LD_LIBRARY_PATH");
    if (localLdlPath != null && localLdlPath.length() > 0) {
      if (addToSystemEnvTable(tableOfSysEnvVars, "LD_LIBRARY_PATH", localLdlPath))
        tableOfEnvVars.remove("LD_LIBRARY_PATH");
    }
    // add system env vars to the list
    StringBuffer envBuffer = new StringBuffer();
    Enumeration sysKeys = tableOfSysEnvVars.keys();
    while (sysKeys.hasMoreElements()) {
      String key = (String) sysKeys.nextElement();
      String value = tableOfSysEnvVars.getProperty(key);
      if (value.length() > 0) {
        envBuffer.setLength(0);
        envBuffer.append(key);
        envBuffer.append('=');
        envBuffer.append(value);
        envArrayList.add(envBuffer.toString());
      }
    }
    // add the rest of local env vars
    Enumeration envKeys = tableOfEnvVars.keys();
    while (envKeys.hasMoreElements()) {
      String key = (String) envKeys.nextElement();
      String value = tableOfEnvVars.getProperty(key);
      if (value.length() > 0) {
        envBuffer.setLength(0);
        envBuffer.append(key);
        envBuffer.append('=');
        envBuffer.append(value);
        envArrayList.add(envBuffer.toString());
        cmdBuffer.setLength(0);
        cmdBuffer.append("-D");
        cmdBuffer.append(envBuffer.toString());
        cmdArrayList.add(cmdBuffer.toString());
      }
    }
    // specify application class and its args
    cmdArrayList.add(INSTALLATION_TESTER_APP);
    cmdBuffer.setLength(0);
    cmdBuffer.append(mainDescPath);
    cmdArrayList.add(cmdBuffer.toString());
    // run installation verification app.
    String[] cmdArray = new String[cmdArrayList.size()];
    cmdArrayList.toArray(cmdArray);
    String[] envArray = new String[envArrayList.size()];
    envArrayList.toArray(envArray);
    File workDir = new File(mainRootDirPath);
    return runInstallationVerificationTest(cmdArray, envArray, workDir);
  }

  /**
   * Extracts files with a given extension from a given PEAR file into a given target directory. If
   * the given filename extension is <code>null</code>, extracts all the files from a given PEAR
   * file. Returns the path to the new component root directory.
   * 
   * @param componentId
   *          The given component ID.
   * @param pearFileLocation
   *          The given PEAR file location.
   * @param fileExt
   *          The given filename extension.
   * @param targetDir
   *          The given target directory.
   * @return The path to the new component root directory.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static String extractFilesFromPEARFile(String componentId, String pearFileLocation,
          String fileExt, File targetDir) throws IOException {
    return extractFilesFromPEARFile(componentId, pearFileLocation, fileExt, targetDir, null);
  }

  /**
   * Internal implementatiton of the <code>extractFilesFromPEARFile</code> method, which allows
   * sending messages to the OUT and ERR queues.
   * 
   * @param componentId
   *          The given component ID.
   * @param pearFileLocation
   *          The given PEAR file location.
   * @param fileExt
   *          The given filename extension.
   * @param targetDir
   *          The given target directory.
   * @param controller
   *          The instance of the <code>InstallationController<code> class that provides OUT and ERR 
   * message routing, or <code>null</code>.
   * @return The path to the new component root directory.
   * @throws IOException if any I/O exception occurred.
   */
  protected static String extractFilesFromPEARFile(String componentId, String pearFileLocation,
          String fileExt, File targetDir, InstallationController controller) throws IOException {
    // get PEAR file size
    long fileSize = FileUtil.getFileSize(pearFileLocation);
    // create root directory
    if (!targetDir.isDirectory() && !targetDir.mkdirs())
      throw new IOException("cannot create directory " + targetDir.getAbsolutePath());
    // specify local PEAR file
    File pearFile = null;
    boolean removeLocalCopy = false;
    boolean done = false;
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
      JarFile jarFile = new JarFile(pearFile);
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
    }
    return done ? targetDir.getAbsolutePath() : null;
  }

  /**
   * Extracts all files of a given component from a given PEAR file into a given target directory.
   * Returns the path to the new component root directory.
   * 
   * @param componentId
   *          The given component ID.
   * @param pearFileLocation
   *          The given PEAR file location.
   * @param installationDir
   *          The given target directory.
   * @return The path to the new component root directory.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static String extractPEARFile(String componentId, String pearFileLocation,
          File installationDir) throws IOException {
    return extractFilesFromPEARFile(componentId, pearFileLocation, null, installationDir);
  }

  /**
   * Internal implementation of the <code>extractPEARFile</code> method, which allows sending
   * messages to the OUT and ERR queues.
   * 
   * @param componentId
   *          The given component ID.
   * @param pearFileLocation
   *          The given PEAR file location.
   * @param installationDir
   *          The given target directory.
   * @param controller
   *          The instance of the <code>InstallationController<code> class that provides OUT and ERR 
   * message routing, or <code>null</code>.
   * @return The path to the new component root directory.
   * @throws IOException if any I/O exception occurred.
   */
  protected static String extractPEARFile(String componentId, String pearFileLocation,
          File installationDir, InstallationController controller) throws IOException {
    return extractFilesFromPEARFile(componentId, pearFileLocation, null, installationDir,
            controller);
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
  protected static Hashtable getDelegateInstallationDescriptors(Hashtable installationTable)
          throws IOException {
    // get list of separately installed delegate components
    Enumeration dlgIdList = installationTable.keys();
    // build Hashtable of delegate InsD objects
    Hashtable dlgInsdObjects = new Hashtable();
    while (dlgIdList.hasMoreElements()) {
      // process next delegate component
      String dlgId = (String) dlgIdList.nextElement();
      String dlgRootPath = (String) installationTable.get(dlgId);
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
   *          <code>PackageSelector<code> class that allows selecting root directory 
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
   *          <code>PackageSelector<code> class that allows selecting location of the 
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
   * <li>-DUIMA_HOME=<local_uima_root_dir>
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
   * Runs the installation test executing a given command. Sets the attributes of the returned
   * <code>TestStatus</code> object.
   * 
   * @param cmdArray
   *          The array of strings that represents the given command to be executed.
   * @param envArray
   *          The array of environment variables settings - (key=value) pairs.
   * @param workDir
   *          The working directory where the installation test needs to be run.
   * @return The instance of the <code>TestStatus</code> class that contains the return code and
   *         possible error message of the given command.
   * @throws IOException
   *           if any I/O error occurred.
   */
  protected static TestStatus runInstallationVerificationTest(String[] cmdArray, String[] envArray,
          File workDir) throws IOException {

    if (System.getProperty("DEBUG") != null) {
      System.out.println(">>> DBG: command array => ");
      for (int i = 0; i < cmdArray.length; i++)
        System.out.println("\t[" + i + "]=" + cmdArray[i]);
      System.out.println(">>> DBG: env array => ");
      for (int i = 0; i < envArray.length; i++)
        System.out.println("\t" + envArray[i]);
      System.out.println(">>> DBG: working dir => ");
      System.out.println("\t" + workDir.getAbsolutePath());
    }
    BufferedReader errReader = null;
    TestStatus status = new TestStatus();
    try {
      // run specified command with specified env.vars as a separate process
      Process process = Runtime.getRuntime().exec(cmdArray, envArray, workDir);
      // get error message (if exists)
      errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
      StringWriter msgBuffer = new StringWriter();
      PrintWriter msgWriter = new PrintWriter(msgBuffer);
      String line;
      while ((line = errReader.readLine()) != null)
        msgWriter.println(line);
      try {
        status.retCode = process.waitFor();
      } catch (InterruptedException e) {
        
      }
      status.message = msgBuffer.toString();
    } finally {
      if (errReader != null) {
        try {
          errReader.close();
        } catch (Exception e) {
        }
      }
    }
    return status;
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
   * Creates command for running the installation test for a given installed component, including
   * possible separately installed delegate components, runs the test, and returns the
   * <code>TestStatus</code> object with the test results. If a given table of separately
   * installed delegate components is not empty, adds environment variables for the specified
   * delegate components to the test process environment.
   * 
   * @param controller
   *          The given active <code>InstallationController</code> object.
   * @param uimaHomePath
   *          The UIMA_HOME directory path.
   * @return The <code>TestStatus</code> object that contains the return code and possible error
   *         message of the test.
   */
  public static synchronized TestStatus verifyComponentInstallation(
          InstallationController controller, String uimaHomePath) {
    try {
      // check input parameters
      if (controller._insdObject == null)
        throw new RuntimeException("null installation descriptor");
      InstallationDescriptor mainInsD = controller._insdObject;
      String mainRootDirPath = mainInsD.getMainComponentRoot();
      if (mainRootDirPath == null)
        throw new RuntimeException("main root directory not specified");
      String mainDescPath = mainInsD.getMainComponentDesc();
      if (mainDescPath == null)
        throw new RuntimeException("main descriptor path not specified");

      String uimaClassPath = null;
      if (uimaHomePath == null) {
        // build UIMA classpath using application class path
        uimaClassPath = System.getProperty("java.class.path", null);
      } else {
        // build UIMA classpath using UIMA_HOME env variable
        uimaClassPath = buildUIMAClassPath(uimaHomePath);
      }
      if (uimaClassPath == null) {
        throw new RuntimeException(UIMA_HOME_ENV + " variable not specified");
      }

      // build component classpath, including dlg components
      String compClassPath = controller.buildComponentClassPath();

      // set java.library.path, including dlg components
      String javaLibPath = controller.buildComponentPath();
      // set other required env vars
      Properties tableOfEnvVars = controller.buildTableOfEnvVars();
      // add CLASSPATH, PATH and LD_LIBRARY_PATH to the table of env.vars
      if (compClassPath.length() > 0)
        tableOfEnvVars.setProperty("CLASSPATH", compClassPath);
      if (javaLibPath.length() > 0) {
        tableOfEnvVars.setProperty("PATH", javaLibPath);
        tableOfEnvVars.setProperty("LD_LIBRARY_PATH", javaLibPath);
      }
      return deployInstallationVerificationTest(mainRootDirPath, mainInsD, mainDescPath,
              compClassPath, javaLibPath, tableOfEnvVars, uimaClassPath);
    } catch (Throwable exc) {
      // print exception as 'verification message'
      StringWriter strWriter = new StringWriter();
      PrintWriter oWriter = new PrintWriter(strWriter);
      exc.printStackTrace(oWriter);
      TestStatus status = new TestStatus();
      status.retCode = -1;
      status.message = strWriter.toString();
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
   *          root directory.
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
   *          root directory.
   * @param msgListener
   *          The given custom message listener or <code>null</code>.
   */
  public InstallationController(String componentId, String rootDirPath, boolean installInRootDir,
          MessageRouter.StdChannelListener msgListener) {
    this(componentId, rootDirPath, installInRootDir, null, msgListener);
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
   */
  protected InstallationController(String componentId, String rootDirPath,
          boolean installInRootDir, MessageRouter msgRouter,
          MessageRouter.StdChannelListener msgListener) {
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
    this(componentId, localPearFile, rootDir, false);
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
   */
  public InstallationController(String componentId, File localPearFile, File rootDir,
          boolean installInRootDir) {
    this(componentId, rootDir.getAbsolutePath(), installInRootDir);
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
    this(componentId, rootDir.getAbsolutePath(), installInRootDir, msgListener);
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
      String mainClassPath = buildComponentClassPath(_mainComponentRootPath, _insdObject);
      cpBuffer.append(mainClassPath);
      // add component classpath for possible delegate components
      if (_installationTable.size() > 0) {
        Enumeration dlgIdList = _installationTable.keys();
        while (dlgIdList.hasMoreElements()) {
          // process next delegate component
          String dlgId = (String) dlgIdList.nextElement();
          String dlgRootPath = (String) _installationTable.get(dlgId);
          InstallationDescriptor dlgInsD = (InstallationDescriptor) _installationInsDs.get(dlgId);
          String dlgClassPath = buildComponentClassPath(dlgRootPath, dlgInsD);
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
        Enumeration dlgIdList = _installationTable.keys();
        while (dlgIdList.hasMoreElements()) {
          // process next delegate component
          String dlgId = (String) dlgIdList.nextElement();
          String dlgRootPath = (String) _installationTable.get(dlgId);
          InstallationDescriptor dlgInsD = (InstallationDescriptor) _installationInsDs.get(dlgId);
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
        Enumeration dlgIdList = _installationTable.keys();
        while (dlgIdList.hasMoreElements()) {
          // process next delegate component
          String dlgId = (String) dlgIdList.nextElement();
          InstallationDescriptor dlgInsD = (InstallationDescriptor) _installationInsDs.get(dlgId);
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
      if (extractPEARFile(_mainComponentId, _mainPearFileLocation, _mainComponentRoot, this) == null) {
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
      if (extractFilesFromPEARFile(_mainComponentId, _mainPearFileLocation, ".xml",
              _mainComponentRoot, this) == null) {
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
   * @return The <code>Hashtable</code> that contains the
   *         <code>(component_id, root_directory)</code> pairs for the installed delegate
   *         components.
   */
  protected synchronized void installDelegateComponents() {
    // get list of separate delegate components IDs
    Enumeration dlgList = _insdObject.getDelegateComponents().keys();
    while (dlgList.hasMoreElements()) {
      // get next separate delegate component ID
      String componentId = (String) dlgList.nextElement();
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
                _installationDirPath, false, this._msgRouter, this._defaultMsgListener);
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
    Enumeration dlgList = _insdObject.getDelegateComponents().keys();
    while (dlgList.hasMoreElements()) {
      // get next separate delegate component ID
      String componentId = (String) dlgList.nextElement();
      // install XML descriptors of the next delegate component
      InstallationController dlgController = new InstallationController(componentId,
              _installationDirPath, false, this._msgRouter, this._defaultMsgListener);
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
   * generates the pearSpecifier to run the installed pear component. The descriptor that is created has
   * the filename &lt;componentID&gt;_pear.xml and is created in the main component root directory. 
   * If the file already exist, it will be overridden. 
   * 
   * @param mainComponentRootPath
   *          main component root path where the pear was installed to
   *           
   * @param mainComponentId
   *          main component ID of the installed pear file
   *          
   * @throws IOException
   * @throws SAXException
   */
  protected static synchronized void generatePearSpecifier(String mainComponentRootPath, String mainComponentId) throws IOException, SAXException{    
    PearSpecifier pearSpec = UIMAFramework.getResourceSpecifierFactory().createPearSpecifier();
    pearSpec.setPearPath(mainComponentRootPath);
    File outputFile = new File(mainComponentRootPath, mainComponentId + PEAR_DESC_FILE_POSTFIX);      
    pearSpec.toXML(new FileOutputStream(outputFile));
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
      Enumeration envVarList = envVarTable.keys();
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
          }
        }
      }
    }
    // set local config params
    packageConfig.setProperty(LocalInstallationAgent.MAIN_ROOT, _mainComponentRootPath.replace(
            '\\', '/'));
    Iterator dlgIdList = _installationTable.keySet().iterator();
    while (dlgIdList.hasNext()) {
      String id = (String) dlgIdList.next();
      String idRoot = LocalInstallationAgent.COMP_ROOT_PREFIX + id
              + LocalInstallationAgent.COMP_ROOT_SUFFIX;
      packageConfig.setProperty(idRoot, ((String) _installationTable.get(id)).replace('\\', '/'));
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
      TestStatus status = verifyComponentInstallation(this, _uimaHomePath);
      if (status.retCode == 0) {
        // verification successful
        success = true;
        _verificationMsg = null;
        if (_installationMonitor != null) // notify monitor
          _installationMonitor.setInstallationStatus(_mainComponentId, VERIFICATION_COMPLETED);
      } else if (status.retCode == -1) {
        // verification failed
        _verificationMsg = status.message;
        if (_installationMonitor != null) // notify monitor
          _installationMonitor.setInstallationStatus(_mainComponentId, VERIFICATION_FAILED);
      } else {
        // verification cancelled
        _verificationMsg = null;
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
