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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;
import java.util.jar.JarFile;

import org.apache.uima.pear.util.FileUtil;
import org.apache.uima.pear.util.StringUtil;
import org.apache.uima.resource.RelativePathResolver;
import org.xml.sax.SAXException;

/**
 * The <code>PackageBrowser</code> class allows browsing both archived and unarchived PEAR
 * packages, finding package files and directories, loading installation descriptor files and
 * returning run-time environment settings required for installed components.
 * 
 * @see org.apache.uima.pear.tools.InstallationDescriptor
 */

public class PackageBrowser {
  // standard directories
  public static final String BINARY_DIR = File.separator + InstallationController.PACKAGE_BIN_DIR;

  public static final String CONFIGURATION_DIR = File.separator
          + InstallationController.PACKAGE_CONF_DIR;

  public static final String DATA_DIR = File.separator + InstallationController.PACKAGE_DATA_DIR;

  public static final String DESCRIPTORS_DIR = File.separator
          + InstallationController.PACKAGE_DESC_DIR;

  public static final String DOCUMENTATION_DIR = File.separator
          + InstallationController.PACKAGE_DOC_DIR;

  public static final String LIBRARY_DIR = File.separator + InstallationController.PACKAGE_LIB_DIR;

  public static final String METADATA_DIR = File.separator
          + InstallationController.PACKAGE_METADATA_DIR;

  public static final String RESOURCES_DIR = File.separator
          + InstallationController.PACKAGE_RESOURCES_DIR;

  public static final String SOURCES_DIR = File.separator
          + InstallationController.PACKAGE_SOURCES_DIR;

  // standard files
  public static final String INSTALLATION_DESCRIPTOR_FILE = File.separator
          + InstallationProcessor.INSD_FILE_PATH;

  public static final String PEAR_PROPERTIES_FILE = File.separator
          + InstallationController.PACKAGE_CONFIG_FILE;

  public static final String SUBMISSION_PROPERTIES_FILE = METADATA_DIR + "/submission.properties";

  public static final String SETENV_TXT_FILE = File.separator + InstallationController.SET_ENV_FILE;

  // attributes
  private File _rootDir;

  private JarFile _pearPackage;

  private File _pearFile;

  private boolean _archived;

  private TreeSet<File> _allFiles = new TreeSet<File>();

  private TreeSet<File> _allDirs = new TreeSet<File>();

  /**
   * Constructor that allows browsing a given PEAR package without unarchiving it.
   * 
   * @param pearPackage
   *          The given archived PEAR package to browse.
   * @throws IOException if a problem with IO
   */
  public PackageBrowser(JarFile pearPackage) throws IOException {
    _pearPackage = pearPackage;
    _pearFile = new File(pearPackage.getName());
    int nameEndIndex = _pearFile.getAbsolutePath().lastIndexOf('.');
    // set root dir = PEAR file path (w/o file name extension)
    String rootDirPath = (nameEndIndex > 0) ? _pearFile.getAbsolutePath()
            .substring(0, nameEndIndex) : _pearFile.getAbsolutePath();
    _rootDir = new File(rootDirPath);
    _archived = true;
    // add directories and files to the lists
    _allDirs.addAll(FileUtil.createDirList(pearPackage));
    _allFiles.addAll(FileUtil.createFileList(pearPackage));
  }

  /**
   * Constructor that allows browsing a given unacrhived PEAR package before or after its
   * installation.
   * 
   * @param pearPackageDir
   *          The root directory where the PEAR package was unarchived.
   * @throws IOException if a problem with IO
   */
  public PackageBrowser(File pearPackageDir) throws IOException {
    _rootDir = pearPackageDir;
    _archived = false;
    // add directories and files to the lists
    _allFiles.addAll(FileUtil.createFileList(_rootDir, true));
    _allDirs.addAll(FileUtil.createDirList(_rootDir, true));
  }

  /**
   * Creates a string that should be added to the CLASSPATH to run the given installed component,
   * based on its installation descriptor specifications, as well as the contents of its
   * <code>lib</code> directory. The output string includes absolute path expressions for all
   * relevant objects containing in the component PEAR package. If the component package is
   * archived, returns <code>null</code>.
   * 
   * @return The string that needs to be added to the CLASSPATH to run the given installed
   *         component.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public String buildComponentClassPath() throws IOException {
    return buildComponentClassPath(false, true);
  }
  
  /**
   * Like {@link #buildComponentClassPath()}, but without all jars from the lib dir.
   * @return The runtime classpath for the component.
   * @throws IOException if there was an IO problem
   */
  public String buildComponentRuntimeClassPath() throws IOException {
    return buildComponentClassPath(false, false);
  }

  /**
   * Creates a string that should be added to the CLASSPATH to run the given installed component,
   * based on its installation descriptor specifications, as well as the contents of its
   * <code>lib</code> directory. The output string includes absolute or relative path expressions
   * for all relevant objects containing in the component PEAR package, depending on the value of a
   * given <code>boolean</code> argument. If the component package is archived, returns
   * <code>null</code>.
   * 
   * @param relativePath
   *          If <code>true</code>, the output string will include relative path expressions for
   *          all relevant objects containing in the component PEAR package, otherwise it will
   *          contain absolute path expressions for these objects.
   * @param addLibDir
   *          Whether to add jars from the lib dir to the classpath (true at packaging time, false
   *          at runtime).
   * @return The string that should be added to the CLASSPATH to run the given installed component.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public String buildComponentClassPath(boolean relativePath, boolean addLibDir) throws IOException {
    if (!isArchived()) {
      InstallationDescriptor insdObject = getInstallationDescriptor();
      if (insdObject != null) {
        String absoluteClassPath = InstallationController.buildComponentClassPath(
                getRootDirectory().getAbsolutePath(), insdObject, addLibDir);
        String absoluteRootDirPathExp = StringUtil.toRegExpReplacement(getRootDirectory()
                .getAbsolutePath().replace('\\', '/'));
        return relativePath ? absoluteClassPath.replaceAll(absoluteRootDirPathExp, "\\.")
                : absoluteClassPath;
      }
    }
    return null;
  }

  /**
   * Creates a string that should be added to the PATH to run the given installed component, based
   * on the PEAR package defaults and its installation descriptor specifications. The output string
   * includes absolute path expressions for all relevant objects containing in the component PEAR
   * package. If the component package is archived, returns <code>null</code>.
   * 
   * @return The string that needs to be added to the PATH to run the given installed component.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public String buildComponentPath() throws IOException {
    return buildComponentPath(false);
  }

  /**
   * Creates a string that should be added to the PATH to run the given installed component, based
   * on the PEAR package defaults and its installation descriptor specifications. The output string
   * includes absolute or relative path expressions for all relevant objects containing in the
   * component PEAR package, depending on the value of a given <code>boolean</code> argument. If
   * the component package is archived, returns <code>null</code>.
   * 
   * @param relativePath
   *          If <code>true</code>, the output string will include relative path expressions for
   *          all relevant objects containing in the component PEAR package, otherwise it will
   *          contain absolute path expressions for these objects.
   * @return The string that needs to be added to the PATH to run the given installed component.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public String buildComponentPath(boolean relativePath) throws IOException {
    if (!isArchived()) {
      InstallationDescriptor insdObject = getInstallationDescriptor();
      if (insdObject != null) {
        String absolutePath = InstallationController.buildComponentPath(getRootDirectory()
                .getAbsolutePath(), insdObject);
        String absoluteRootDirPathExp = StringUtil.toRegExpReplacement(getRootDirectory()
                .getAbsolutePath().replace('\\', '/'));
        return relativePath ? absolutePath.replaceAll(absoluteRootDirPathExp, "\\.") : absolutePath;
      }
    }
    return null;
  }

  /**
   * Looks for package directories matching a given directory name pattern in the PEAR package. If
   * the given directory name pattern starts with '/' or '\', the method looks for directory names
   * that start with the given name pattern, otherwise it looks for directory names that contain the
   * given name pattern as a substring. The method does not support wild cards.
   * 
   * @param dirNamePattern
   *          The given directory name pattern to look for.
   * @return The array of matching package directories.
   */
  public File[] findDirectory(String dirNamePattern) {
    String pattern = dirNamePattern.replace('\\', '/');
    File[] foundDirs = new File[0];
    ArrayList<File> foundDirList = new ArrayList<File>();
    Iterator<File> list = _allDirs.iterator();
    while (list.hasNext()) {
      File dir = list.next();
      String dirName = dir.getAbsolutePath().substring(_rootDir.getAbsolutePath().length())
              .replace('\\', '/');
      if (pattern.startsWith(File.separator)) {
        if (dirName.startsWith(pattern))
          foundDirList.add(dir);
      } else if (dirName.indexOf(pattern) >= 0)
        foundDirList.add(dir);
    }
    if (foundDirList.size() > 0) {
      foundDirs = new File[foundDirList.size()];
      foundDirList.toArray(foundDirs);
    }
    return foundDirs;
  }

  /**
   * Looks for package files matching a given file name pattern in the PEAR package. If the given
   * file name pattern starts with '/' or '\', the method looks for file names that start from the
   * given name pattern, otherwise it looks for file names that contain the given name pattern as a
   * substring. The method does not support wild cards.
   * 
   * @param fileNamePattern
   *          The given file name pattern to look for.
   * @return The array of matching package files.
   */
  public File[] findFile(String fileNamePattern) {
    String pattern = fileNamePattern.replace('\\', '/');
    File[] foundFiles = new File[0];
    ArrayList<File> foundFileList = new ArrayList<File>();
    Iterator<File> list = _allFiles.iterator();
    while (list.hasNext()) {
      File file = list.next();
      String fileName = file.getAbsolutePath().substring(_rootDir.getAbsolutePath().length())
              .replace('\\', '/');
      if (pattern.startsWith(File.separator)) {
        if (fileName.startsWith(pattern))
          foundFileList.add(file);
      }
      if (fileName.indexOf(pattern) >= 0)
        foundFileList.add(file);
    }
    if (foundFileList.size() > 0) {
      foundFiles = new File[foundFileList.size()];
      foundFileList.toArray(foundFiles);
    }
    return foundFiles;
  }

  /**
   * Looks for a given standard package directory. This method assumes that the given standard
   * directory name is the full name of the directory in the package root directory.
   * 
   * @param stdDirName
   *          The given full standard package directory name.
   * @return The specified standard package directory, if this directory exists in the package,
   *         <code>null</code> otherwise.
   */
  public File findStandardDirectory(String stdDirName) {
    String dirName = stdDirName.replace('\\', File.separatorChar).replace('/', File.separatorChar);
    File dir = new File(_rootDir, dirName);
    File foundDir = (_allDirs.contains(dir)) ? dir : null;
    return foundDir;
  }

  /**
   * Looks for a given standard package file. This method assumes that the given standard file name
   * is the full name of the file in the package root directory.
   * 
   * @param stdFileName
   *          The given full standard package file name.
   * @return The specified standard package file, if this file exists in the package,
   *         <code>null</code> otherwise.
   */
  public File findStandardFile(String stdFileName) {
    String fileName = stdFileName.replace('\\', File.separatorChar)
            .replace('/', File.separatorChar);
    File file = new File(_rootDir, fileName);
    File foundFile = (_allFiles.contains(file)) ? file : null;
    return foundFile;
  }

  /**
   * @return Array of <code>File</code> objects representing all directories existing in the
   *         package.
   */
  public File[] getAllDirectories() {
    File[] array = new File[_allDirs.size()];
    _allDirs.toArray(array);
    return array;
  }

  /**
   * @return Array of <code>File</code> objects representing all files existing in the package.
   */
  public File[] getAllFiles() {
    File[] array = new File[_allFiles.size()];
    _allFiles.toArray(array);
    return array;
  }

  /**
   * Loads the <code>INSTALLATION_DESCRIPTOR_FILE</code> file, and creates the
   * <code>InstallationDescriptor</code> (InsD) object. <b>Note:</b> if the component package has
   * been installed, the InsD object contains real specifications of package directories, otherwise
   * it may contain macros like <code>$main_root</code>.
   * 
   * @return The InsD object corresponding the installation descriptor file.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public InstallationDescriptor getInstallationDescriptor() throws IOException {
    InstallationDescriptorHandler insdHandler = new InstallationDescriptorHandler();
    InstallationDescriptor insdObject = null;
    if (isArchived()) {
      try {
        insdHandler.parseInstallationDescriptor(_pearPackage);
        insdObject = insdHandler.getInstallationDescriptor();
      } catch (SAXException err) {
        throw new IOException(err.toString());
      }
    } else {
      File insdFile = findStandardFile(INSTALLATION_DESCRIPTOR_FILE);
      if (insdFile != null) {
        try {
          insdHandler.parse(insdFile);
          insdObject = insdHandler.getInstallationDescriptor();
          insdObject.setMainComponentRoot(getRootDirectory().getAbsolutePath());
        } catch (SAXException err) {
          throw new IOException(err.toString());
        }
      }
    }
    return insdObject;
  }

  /**
   * @return The package root directory, where the package was unarchived, or the directory
   *         corresponding to the package file path without its extension, if the archived package
   *         was specified.
   */
  public File getRootDirectory() {
    return _rootDir;
  }

  /**
   * @return <code>true</code>, if the archived package was specified, <code>false</code>
   *         otherwise.
   */
  public boolean isArchived() {
    return _archived;
  }

  /**
   * returns the pear component pearSpecifier file path.
   * 
   * @return returns the pear component pearSpecifier file path or null if an archived package was
   *         used.
   * 
   * @throws IOException if there was an IO problem
   */
  public String getComponentPearDescPath() throws IOException {

    // if the package is not installed, return null
    if (_archived) {
      return null;
    } else {
      // get pear descriptor file and return it as file path
      File pearDescFile = new File(this._rootDir, this.getInstallationDescriptor()
              .getMainComponentId()
              + InstallationController.PEAR_DESC_FILE_POSTFIX);
      return pearDescFile.getAbsolutePath();
    }
  }

  /**
   * Returns the UIMA datapath setting for the component.
   * 
   * The datapath of the component must be specified as environment variable with the key
   * <code>uima.datapath</code>.
   * 
   * @return the datapath setting for the component or null if the datapath is not specified.
   * 
   * @throws IOException
   *           If any I/O exception occurred while reading the component meta data.
   */
  public String getComponentDataPath() throws IOException {

    // get all environment variables that are specified for the current pear file
    Properties pearEnvProps = InstallationController.buildTableOfEnvVars(this
            .getInstallationDescriptor());

    // return the uima datapath setting if available. If not return null
    return (String) pearEnvProps.get(RelativePathResolver.UIMA_DATAPATH_PROP);

  }

  /**
   * Returns the environment variable settings for the component. The variable settings does not
   * contain the <code>classpath</code> and <code>uima.datapath</code> settings for the
   * component.
   * 
   * @return returns the environment variable settings for the component
   * 
   * @throws IOException
   *           If any I/O exception occurred while reading the component meta data.
   */
  public Properties getComponentEnvVars() throws IOException {
    // get all environment variables that are specified for the current pear file
    Properties pearEnvProps = InstallationController.buildTableOfEnvVars(this
            .getInstallationDescriptor());

    // removes the UIMA datapath setting if available since it is already returned with the
    // getComponentDataPath() method.
    pearEnvProps.remove(RelativePathResolver.UIMA_DATAPATH_PROP);

    // return the environment variables specified for the component
    return pearEnvProps;

  }
}
