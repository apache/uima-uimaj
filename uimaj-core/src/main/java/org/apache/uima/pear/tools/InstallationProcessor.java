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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

import org.apache.uima.pear.util.FileUtil;
import org.apache.uima.pear.util.StringUtil;

/**
 * The <code>InstallationProcessor</code> class implements operations specified in the
 * installation descriptor. This class also allows generating the default Vinci Service descriptor
 * for the specified component.
 * 
 * @see org.apache.uima.pear.tools.InstallationDescriptor
 */

public class InstallationProcessor {
  /*
   * Files
   */
  public static final String INSD_FILE_PATH = "metadata/install.xml";

  public static final String VS_DESCRIPTOR_PATH = "desc/vs_desc.xml";

  /*
   * Protected constants for regular expressions
   */
  protected static final String MAIN_ROOT_REGEX = "\\$main_root";

  protected static final String MAIN_ROOT_REL_REGEX = "\\$main_root_rel";

  protected static final String MAIN_ROOT_URL_REGEX = "\\$main_root_url";

  protected static final String DELEGATE_ROOT_PREFIX_REGEX = "\\$";

  protected static final String DELEGATE_ROOT_SUFFIX_REGEX = "\\$root";

  protected static final String DELEGATE_ROOT_REL_SUFFIX_REGEX = "\\$root_rel";

  protected static final String DELEGATE_ROOT_URL_SUFFIX_REGEX = "\\$root_url";

  // Static attributes
  private static StringBuffer __regexBuffer = new StringBuffer();

  // Attributes
  private String _mainRootPath;

  private Hashtable<String, String> _installationTable = new Hashtable<String, String>();

  private Hashtable<String, String> _urlSubstitutionTable = new Hashtable<String, String>();

  private Hashtable<String, String> _pathSubstitutionTable = new Hashtable<String, String>();

  private InstallationDescriptor _insdObject = null;

  private boolean _completed = false;

  private InstallationController _controller = null;

  /**
   * Builds $component_id$&lt;suffix&gt; regular expression string for a given component ID and a given
   * 'suffix' string. Valid 'suffix' strings are InstallationDescriptor.DELEGATE_ROOT_SUFFIX_REGEX
   * for absolute path, InstallationDescriptor.DELEGATE_ROOT_REL_SUFFIX_REGEX for relative path,
   * InstallationDescriptor.DELEGATE_ROOT_URL_SUFFIX_REGEX for URL.
   * 
   * @param componentId
   *          The given component ID.
   * @param suffix a suffix to be added to the component ID
   * Valid 'suffix' strings are InstallationDescriptor.DELEGATE_ROOT_SUFFIX_REGEX
   * for absolute path, InstallationDescriptor.DELEGATE_ROOT_REL_SUFFIX_REGEX for relative path,
   * InstallationDescriptor.DELEGATE_ROOT_URL_SUFFIX_REGEX for URL.
   * @return The $component_id$root regular expression string.
   */
  protected static String componentIdRootRegExp(String componentId, String suffix) {
    synchronized (__regexBuffer) {
      __regexBuffer.setLength(0);
      __regexBuffer.append(DELEGATE_ROOT_PREFIX_REGEX);
      __regexBuffer.append(componentId);
      __regexBuffer.append(suffix);
      return __regexBuffer.toString();
    }
  }

  /**
   * Generates default Vinci Service descriptor for a specified component, and puts it to a
   * specified location.
   * 
   * @param insdObject
   *          The given installation descriptor of the component.
   * @param mainRootDir
   *          The given root directory of the component.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static void generateVSDescriptor(InstallationDescriptor insdObject, File mainRootDir)
          throws IOException {
    File vsDescriptorFile = new File(mainRootDir, VS_DESCRIPTOR_PATH);
    PrintWriter oWriter = null;
    try {
      oWriter = new PrintWriter(new FileWriter(vsDescriptorFile));
      String xmlContent = generateVSDescriptorContent(insdObject);
      oWriter.println(xmlContent);
      oWriter.close();
    } finally {
      if (oWriter != null) {
        try {
          oWriter.close();
        } catch (Exception e) {
        }
      }
    }
  }

  /**
   * Generates default Vinci Service descriptor for a specified component, and returns the content
   * of the descriptor as a stream (for Eclipse plug-in).
   * 
   * @param insdObject
   *          The given installation descriptor of the component.
   * @return The stream that contains the default Vinci Service descriptor for the specified
   *         component.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static InputStream generateVSDescriptorAsStream(InstallationDescriptor insdObject)
          throws IOException {
    InputStream iStream = null;
    String xmlContent = generateVSDescriptorContent(insdObject);
    byte[] xmlContentBytes = xmlContent.getBytes();
    iStream = new ByteArrayInputStream(xmlContentBytes);
    return iStream;
  }

  /**
   * Generates the default Vinci Service descriptor content for a specified component.
   * 
   * @param insdObject
   *          The given installation descriptor of the component.
   * @return The content of the default Vinci Service descriptor for the specified component.
   */
  protected static String generateVSDescriptorContent(InstallationDescriptor insdObject) {
    StringBuffer buffer = new StringBuffer();
    buffer.append("<?xml version=\"1.0\"?>\n");
    buffer.append("<deployment name=\"Vinci ");
    buffer.append(insdObject.getMainComponentName());
    buffer.append(" Service\">\n");
    buffer.append("\t<service name=\"");
    buffer.append(insdObject.getMainComponentId());
    buffer.append("\" host=\"localhost\" provider=\"vinci\">\n");
    buffer.append("\t\t<parameter name=\"resourceSpecifierPath\" ");
    buffer.append("value=\"");
    buffer.append(insdObject.getMainComponentDesc());
    buffer.append("\"/>\n");
    buffer.append("\t\t<parameter name=\"numInstances\" value=\"1\"/>\n");
    buffer.append("\t</service>\n");
    buffer.append("</deployment>");
    return buffer.toString();
  }

  /**
   * Performs a specified 'find_and_replace_path' installation action.
   * 
   * @param action
   *          The given 'find_and_replace_path' installation action.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  protected static void findAndReplacePath(InstallationDescriptor.ActionInfo action)
          throws IOException {
    // get action parameters
    String filePath = action.params.getProperty(InstallationDescriptorHandler.FILE_TAG);
    if (filePath == null)
      throw new RuntimeException("no " + InstallationDescriptorHandler.FILE_TAG + " defined");
    String findString = action.params.getProperty(InstallationDescriptorHandler.FIND_STRING_TAG);
    if (findString == null)
      throw new RuntimeException("no " + InstallationDescriptorHandler.FIND_STRING_TAG + " defined");
    String replaceWith = action.params.getProperty(InstallationDescriptorHandler.REPLACE_WITH_TAG);
    if (replaceWith == null)
      throw new RuntimeException("no " + InstallationDescriptorHandler.REPLACE_WITH_TAG
              + " defined");
    // replace all specified path-strings in specified file
    File inputFile = new File(filePath);
    FileUtil.replaceStringInFile(inputFile, StringUtil.toRegExpString(findString), replaceWith);
  }

  /**
   * Substitutes '$dlg_comp_id$root_url' and '$dlg_comp_id$root' macros in a given string for a
   * given delegate component.
   * 
   * @param source
   *          The given source string.
   * @param compId
   *          The given component ID.
   * @param compRootPath
   *          The given component root directory path.
   * @return The result string with all the 'delegate' macros substituted and all '\' characters
   *         replaced with '/' characters.
   */
  protected static String substituteCompIdRootInString(String source, String compId,
          String compRootPath) {
    String result = source;
    // substitute '$dlg_comp_id$root_url'
    String regExp = componentIdRootRegExp(compId, DELEGATE_ROOT_URL_SUFFIX_REGEX);
    String fileUrl = FileUtil.localPathToFileUrl(compRootPath);
    String replacement = StringUtil.toRegExpReplacement(fileUrl);
    result = result.replaceAll(regExp, replacement);
    // substitute '$dlg_comp_id$root'
    regExp = componentIdRootRegExp(compId, DELEGATE_ROOT_SUFFIX_REGEX);
    replacement = StringUtil.toRegExpReplacement(compRootPath);
    result = result.replaceAll(regExp, replacement);
    return result.trim().replace('\\', '/');
  }

  /**
   * Substitutes '$main_root_url' and '$main_root' macros in a given string.
   * 
   * @param source
   *          The given source string.
   * @param mainRootPath
   *          The given main component root directory path.
   * @return The result string with all the 'main' macros substituted and all '\' characters
   *         replaced with '/' characters.
   */
  protected static String substituteMainRootInString(String source, String mainRootPath) {
    String result = source;
    // substitute '$main_root_url'
    String replacement = StringUtil.toRegExpReplacement(FileUtil.localPathToFileUrl(mainRootPath));
    result = result.replaceAll(MAIN_ROOT_URL_REGEX, replacement);
    // substitute '$main_root'
    replacement = StringUtil.toRegExpReplacement(mainRootPath);
    result = result.replaceAll(MAIN_ROOT_REGEX, replacement);
    return result.trim().replace('\\', '/');
  }

  /**
   * Constructs an instance of the <code>InstallationProcessor</code> class, using given main
   * component root path and a table of installed delegate components.
   * 
   * @param mainRootPath
   *          The given main component root path.
   * @param installationTable
   *          The given table of installed delegate components.
   */
  public InstallationProcessor(String mainRootPath, Hashtable<String, String> installationTable) {
    this(mainRootPath, installationTable, null);
  }

  /**
   * Similar to previous constructor, but sets a given <code>InstallationController</code> object
   * as the requestor.
   * 
   * @param mainRootPath
   *          The given main component root path.
   * @param installationTable
   *          The given table of installed delegate components.
   * @param controller
   *          The given <code>InstallationController</code> requestor.
   */
  public InstallationProcessor(String mainRootPath, Hashtable<String, String> installationTable,
          InstallationController controller) {
    _controller = controller;
    _mainRootPath = mainRootPath.replace('\\', '/');
    _installationTable = installationTable;
    initSubstitutionTables(mainRootPath);
  }

  /**
   * @return The modified <code>InstallationDescriptor</code> object, if the processing completed,
   *         <code>null</code> otherwise.
   */
  public synchronized InstallationDescriptor getInstallationDescriptor() {
    return _completed ? _insdObject : null;
  }

  /**
   * Initializes two <code>Hashtable</code> objects that are used to substitute $dlg_comp_id$root
   * macros in URL and absolute path expressions.
   * 
   * @param mainRootPath
   *          The given main component root directory path.
   */
  protected void initSubstitutionTables(String mainRootPath) {
    Enumeration<String> idList = _installationTable.keys();
    while (idList.hasMoreElements()) {
      String id = idList.nextElement();
      String compIdRootUrlRegEx = componentIdRootRegExp(id, DELEGATE_ROOT_URL_SUFFIX_REGEX);
      String compIdRootRegEx = componentIdRootRegExp(id, DELEGATE_ROOT_SUFFIX_REGEX);
      // put 1 entry for URL and absolute path
      String rootPath = _installationTable.get(id);
      String rootPathUrl = FileUtil.localPathToFileUrl(rootPath);
      _urlSubstitutionTable.put(compIdRootUrlRegEx, rootPathUrl);
      _pathSubstitutionTable.put(compIdRootRegEx, rootPath);
    }
  }

  /**
   * Starts processing installation instructions from the specified installation descriptor.
   * 
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public synchronized void process() throws IOException {
    _completed = false;
    // load XML InsD file
    File mainRootDir = new File(_mainRootPath);
    File xmlInsDFile = new File(mainRootDir, INSD_FILE_PATH);
    if (_controller != null)
      _controller.getOutMsgWriter().println(
              "[InstallationProcessor]: " + "start processing InsD file - "
                      + xmlInsDFile.getAbsolutePath());
    else
      System.out.println("[InstallationProcessor]: " + "start processing InsD file - "
              + xmlInsDFile.getAbsolutePath());
    InstallationDescriptorHandler insdHandler = new InstallationDescriptorHandler();
    try {
      insdHandler.parse(xmlInsDFile);
    } catch (Exception exc) {
      if (exc instanceof IOException)
        throw (IOException) exc;
      throw new IOException(exc.toString());
    }
    _insdObject = insdHandler.getInstallationDescriptor();
    // set main root path
    _insdObject.setMainComponentRoot(_mainRootPath);
    // perform required actions
    Iterator<InstallationDescriptor.ActionInfo> actionList = _insdObject.getInstallationActions().iterator();
    while (actionList.hasNext()) {
      InstallationDescriptor.ActionInfo action = actionList
              .next();
      // substitute string 'variables' in action parameters
      substituteStringVariablesInAction(action.params);
      // perform FIND_AND_REPLACE_PATH_ACT action
      if (action.getName().equals(InstallationDescriptor.ActionInfo.FIND_AND_REPLACE_PATH_ACT))
        findAndReplacePath(action);
    }
    // perform default actions: substitute 'variables' in all conf/ files
    String confDirPath = _mainRootPath + "/" + InstallationController.PACKAGE_CONF_DIR;
    File confDir = new File(confDirPath);
    if (confDir.isDirectory())
      substituteStringVariablesInFiles(confDir);
    // perform default actions: substitute 'variables' in all desc/ files
    String descDirPath = _mainRootPath + "/" + InstallationController.PACKAGE_DESC_DIR;
    File descDir = new File(descDirPath);
    if (descDir.isDirectory())
      substituteStringVariablesInFiles(descDir);
    _completed = true;
  }

  /**
   * Substitutes two $main_root as well as two $comp_id$root macros in a given 'action'
   * <code>Properties</code> object values.
   * 
   * @param params
   *          The given <code>Properties</code> object.
   */
  protected void substituteStringVariablesInAction(Properties params) {
    Enumeration<?> paramNames = params.propertyNames();
    while (paramNames.hasMoreElements()) {
      String paramName = (String) paramNames.nextElement();
      String paramValue = params.getProperty(paramName);
      // replace all ';' with OS-dependent separator
      // in VAR_VALUE_TAG value
      if (paramName.equals(InstallationDescriptorHandler.VAR_VALUE_TAG))
        paramValue = paramValue.replace(';', File.pathSeparatorChar);
      if (paramName.equals(InstallationDescriptorHandler.FILE_TAG)
              || paramName.equals(InstallationDescriptorHandler.REPLACE_WITH_TAG)
              || paramName.equals(InstallationDescriptorHandler.VAR_VALUE_TAG)) {
        // substitute '$main_root_url' and '$main_root'
        paramValue = substituteMainRootInString(paramValue, _mainRootPath);
        // substitute '$dlg_comp_id$root_url'
        Enumeration<String> regexList = _urlSubstitutionTable.keys();
        while (regexList.hasMoreElements()) {
          String regex = regexList.nextElement();
          String replacement = _urlSubstitutionTable.get(regex);
          paramValue = paramValue.replaceAll(regex, StringUtil.toRegExpReplacement(replacement));
        }
        // substitute '$dlg_comp_id$root'
        regexList = _pathSubstitutionTable.keys();
        while (regexList.hasMoreElements()) {
          String regex = regexList.nextElement();
          String replacement = _pathSubstitutionTable.get(regex);
          paramValue = paramValue.replaceAll(regex, StringUtil.toRegExpReplacement(replacement));
        }
        // reset (modified) property value
        params.setProperty(paramName, paramValue);
      }
    }
  }

  /**
   * Substitutes two $main_root as well as three $comp_id$root macros in all files in a given
   * directory, including its sub-directories.
   * 
   * @param dir
   *          The given directory.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  protected void substituteStringVariablesInFiles(File dir) throws IOException {
    // get list of files in the given dir with subdirs
    Iterator<File> fileList = FileUtil.createFileList(dir, true).iterator();
    while (fileList.hasNext()) {
      File file = fileList.next();
      // substitute '$main_root_url'
      String replacement = FileUtil.localPathToFileUrl(_mainRootPath);
      FileUtil.replaceStringInFile(file, MAIN_ROOT_URL_REGEX, replacement);
      // substitute '$main_root'
      replacement = _mainRootPath;
      FileUtil.replaceStringInFile(file, MAIN_ROOT_REGEX, replacement);
      // substitute '$dlg_comp_id$root_rel'
      Enumeration<String> compList = _installationTable.keys();
      while (compList.hasMoreElements()) {
        String compId = compList.nextElement();
        String compRootPath = _installationTable.get(compId);
        String regex = componentIdRootRegExp(compId, DELEGATE_ROOT_REL_SUFFIX_REGEX);
        try {
          replacement = FileUtil.computeRelativePath(file.getParentFile(), new File(compRootPath));
          if (replacement != null)
            FileUtil.replaceStringInFile(file, regex, replacement);
        } catch (Exception e) {
        }
      }
      // substitute '$dlg_comp_id$root_url'
      Enumeration<String> regexList = _urlSubstitutionTable.keys();
      while (regexList.hasMoreElements()) {
        String regex = regexList.nextElement();
        replacement = _urlSubstitutionTable.get(regex);
        FileUtil.replaceStringInFile(file, regex, replacement);
      }
      // substitute '$dlg_comp__id$root'
      regexList = _pathSubstitutionTable.keys();
      while (regexList.hasMoreElements()) {
        String regex = regexList.nextElement();
        replacement = _pathSubstitutionTable.get(regex);
        FileUtil.replaceStringInFile(file, regex, replacement);
      }
    }
  }
}
