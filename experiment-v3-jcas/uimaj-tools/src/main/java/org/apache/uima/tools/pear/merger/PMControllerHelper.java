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

package org.apache.uima.tools.pear.merger;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.uima.pear.tools.InstallationDescriptor;
import org.apache.uima.pear.tools.InstallationDescriptorHandler;
import org.apache.uima.pear.tools.InstallationProcessor;
import org.apache.uima.pear.tools.PackageBrowser;
import org.apache.uima.pear.util.FileUtil;

/**
 * The <code>PMControllerHelper</code> class implements utility methods that are utilized by the
 * <code>{@link PMController}</code> class.
 */

public class PMControllerHelper {
  // Macros
  static final String MAIN_ROOT = "$main_root";

  static final String MAIN_ROOT_REGEX = "\\$main_root";

  static final String STANDARD_PATH_SEPARATOR = ";";

  static final char STANDARD_PATH_SEPARATOR_CHAR = ';';

  /**
   * Creates a 'set_env_variable' action object based on given environment variable name and value.
   * 
   * @param envVarName
   *          The given environment variable name.
   * @param envVarValue
   *          The given environment variable value.
   * @return The <code>InstallationDescriptor.ActionInfo</code> object, containing the
   *         'set_env_variable' action.
   */
  private static InstallationDescriptor.ActionInfo createEnvAction(String envVarName,
          String envVarValue) {
    InstallationDescriptor.ActionInfo action = new InstallationDescriptor.ActionInfo(
            InstallationDescriptor.ActionInfo.SET_ENV_VARIABLE_ACT);
    action.params.setProperty(InstallationDescriptorHandler.VAR_NAME_TAG, envVarName);
    action.params.setProperty(InstallationDescriptorHandler.VAR_VALUE_TAG, envVarValue);
    return action;
  }

  /**
   * Creates a 'find_and_replace_path' action object based on given file path and 'macro' path,
   * which is used as both the search string and the replacement string.
   * 
   * @param filePath
   *          The given file path, that specifies the target file for this action.
   * @param macroPath
   *          The given 'macro' path that is used as both the search string and the replacement
   *          string (e.g. $main_root).
   * @return The <code>InstallationDescriptor.ActionInfo</code> object, containing the
   *         'find_and_replace_path' action.
   */
  private static InstallationDescriptor.ActionInfo createFileAction(String filePath,
          String macroPath) {
    InstallationDescriptor.ActionInfo action = new InstallationDescriptor.ActionInfo(
            InstallationDescriptor.ActionInfo.FIND_AND_REPLACE_PATH_ACT);
    action.params.setProperty(InstallationDescriptorHandler.FILE_TAG, filePath);
    action.params.setProperty(InstallationDescriptorHandler.FIND_STRING_TAG, macroPath);
    action.params.setProperty(InstallationDescriptorHandler.REPLACE_WITH_TAG, macroPath);
    return action;
  }

  /**
   * Creates and saves installation descriptor for the merged PEAR, based on given output root
   * directory, output aggregate component descriptor file, output aggregate component name (ID),
   * array of delegate (input) installation descriptors and array of output delegate root
   * directories. Returns <code>InstallationDescriptor</code> object for the merged PEAR.
   * 
   * @param rootDir
   *          The given output root directory.
   * @param aggDescFile
   *          The given output aggregate component descriptor file.
   * @param aggCompName
   *          The given output aggregate component name (ID).
   * @param dlgInstDescs
   *          The given array of delegate (input) installation descriptors.
   * @param dlgRootDirs
   *          The given array of output delegate root directories.
   * @return The <code>InstallationDescriptor</code> object for the merged PEAR.
   * @throws IOException
   *           If an I/O exception occurred.
   */
  static InstallationDescriptor generateMergedInstallationDescriptor(File rootDir,
          String aggCompName, File aggDescFile, InstallationDescriptor[] dlgInstDescs,
          File[] dlgRootDirs) throws IOException {
    // create aggregate installation descriptor
    File aggInsdFile = new File(rootDir, PackageBrowser.INSTALLATION_DESCRIPTOR_FILE);
    InstallationDescriptor aggInsdObject = new InstallationDescriptor(aggInsdFile);
    aggInsdObject.setMainComponent(aggCompName);
    // 1st step: set aggregate component descriptor
    String aggDescPath = MAIN_ROOT + "/"
            + FileUtil.getRelativePath(rootDir, aggDescFile.getAbsolutePath());
    aggInsdObject.setMainComponentDesc(aggDescPath);
    // 2nd step: collect installation actions from delegates
    for (int i = 0; i < dlgInstDescs.length; i++) {
      InstallationDescriptor dlgInsdObject = dlgInstDescs[i];
      // add implicit installation actions
      File dlgRootDir = dlgRootDirs[i];
      String adjMainRoot = MAIN_ROOT + "/" + dlgRootDir.getName();
      StringBuffer cpBuffer = new StringBuffer();
      // add JAR files in delegate lib dir to CLASSPATH
      File dlgLibDir = new File(dlgRootDir, PackageBrowser.LIBRARY_DIR);
      if (dlgLibDir.isDirectory()) {
        // get list of JAR files
        File[] jarFiles = dlgLibDir.listFiles(new FileUtil.ExtFilenameFilter(".jar"));
        // build delegate CLASSPATH buffer
        cpBuffer.setLength(0);
        for (int n = 0; n < jarFiles.length; n++) {
          if (cpBuffer.length() > 0)
            cpBuffer.append(STANDARD_PATH_SEPARATOR_CHAR);
          cpBuffer.append(adjMainRoot + "/"
                  + FileUtil.getRelativePath(dlgRootDir, jarFiles[n].getAbsolutePath()));
        }
        // add 'set_env_variable' action
        if (cpBuffer.length() > 0)
          aggInsdObject.addInstallationAction(createEnvAction("CLASSPATH", cpBuffer.toString()));
      }
      // add delegate bin dir to CLASSPATH and PATH
      File dlgBinDir = new File(dlgRootDir, PackageBrowser.BINARY_DIR);
      if (dlgBinDir.isDirectory()) {
        String adjBinDirPath = adjMainRoot + "/"
                + FileUtil.getRelativePath(dlgRootDir, dlgBinDir.getAbsolutePath());
        aggInsdObject.addInstallationAction(createEnvAction("CLASSPATH", adjBinDirPath));
        aggInsdObject.addInstallationAction(createEnvAction("PATH", adjBinDirPath));
      }
      // add explicit installation actions
      Collection dlgInstActs = dlgInsdObject.getInstallationActions();
      Iterator list = dlgInstActs.iterator();
      while (list.hasNext())
        aggInsdObject.addInstallationAction((InstallationDescriptor.ActionInfo) list.next());
    }
    // save aggregate installation descriptor file
    InstallationDescriptorHandler.saveInstallationDescriptor(aggInsdObject, aggInsdFile);
    return aggInsdObject;
  }

  /**
   * Processes all delegate installation descriptors, component descriptors and configuration files,
   * adjusting 'macros' for a given delegate root directory. Returns the delegate installation
   * descriptor with adjusted 'macros'.
   * 
   * @param rootDir
   *          The given delegate root directory.
   * @return The elegate installation descriptor with adjusted 'macros'.
   * @throws IOException
   *           If an I/O exception occurred.
   */
  static InstallationDescriptor processDescriptors(File rootDir) throws IOException {
    // build adjusted string for $main_root replacement
    String adjMainRoot = MAIN_ROOT + "/" + rootDir.getName();
    // 1st: process installation descriptor file
    File insdFile = new File(rootDir, InstallationProcessor.INSD_FILE_PATH);
    FileUtil.replaceStringInFile(insdFile, MAIN_ROOT_REGEX, adjMainRoot);
    // load installation descriptor
    InstallationDescriptorHandler insdHandler = new InstallationDescriptorHandler();
    try {
      insdHandler.parse(insdFile);
    } catch (Exception e) {
      throw new IOException(e.toString());
    }
    InstallationDescriptor insdObject = insdHandler.getInstallationDescriptor();
    // add installation descriptor file to 'find_and_replace_path' actions
    insdObject.addInstallationAction(createFileAction(adjMainRoot + "/"
            + InstallationProcessor.INSD_FILE_PATH, MAIN_ROOT));
    // 2nd: process files under 'desc' folder, if exists
    processFiles(rootDir, PackageBrowser.DESCRIPTORS_DIR, insdObject);
    // 3rd: process files under 'conf' folder, if exists
    processFiles(rootDir, PackageBrowser.CONFIGURATION_DIR, insdObject);
    return insdObject;
  }

  /**
   * Processes all files in a given target directory, adjusting 'macros' for a given delegate root
   * directory. Adds appropriate 'find_and_replace_path' actions to a specified delegate
   * installation descriptor.
   * 
   * @param rootDir
   *          The given delegate root directory.
   * @param targetDirName
   *          The name of the given target directory in the delegate root directory.
   * @param insdObject
   *          The given delegate installation descriptor.
   * @throws IOException
   *           If an I/O exception occurred.
   */
  static void processFiles(File rootDir, String targetDirName, InstallationDescriptor insdObject)
          throws IOException {
    // build adjusted string for $main_root replacement
    String adjMainRoot = MAIN_ROOT + "/" + rootDir.getName();
    // build list of files in target dir, including sub-dirs
    File targetDir = new File(rootDir, targetDirName);
    if (!targetDir.isDirectory())
      return;
    Iterator fileList = FileUtil.createFileList(targetDir, true).iterator();
    while (fileList.hasNext()) {
      File file = (File) fileList.next();
      // adjust $main_root in this file
      int counter = FileUtil.replaceStringInFile(file, MAIN_ROOT_REGEX, adjMainRoot);
      if (counter > 0) {
        // add this file to 'find_and_replace_path' actions
        String relFilePath = FileUtil.getRelativePath(rootDir, file.getAbsolutePath());
        insdObject.addInstallationAction(createFileAction(adjMainRoot + "/" + relFilePath,
                MAIN_ROOT));
      }
    }
  }
}
