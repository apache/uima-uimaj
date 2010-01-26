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
import java.net.URL;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.apache.uima.pear.util.FileUtil;

/**
 * The <code>PackageSelectorGUI</code> class provides simple Swing-based file dialog windows for
 * selecting root directories of installed PEAR packages, as well as PEAR package files, which
 * contain components that need to be installed.
 * 
 */

public class PackageSelectorGUI implements InstallationController.PackageSelector {
  /**
   * The <code>PackageDirFilter</code> class allows filtering directories.
   */
  public static class PackageDirFilter extends FileFilter {
    /**
     * Determines whether a given <code>File</code> object is accepted by this filter.
     * 
     * @param file
     *          The given <code>File</code> object.
     * @return <code>true</code>, if the given <code>File</code> object is accepted,
     *         <code>fase</code> otherwise.
     */
    public boolean accept(File file) {
      // accept only directories
      return file.isDirectory();
    }

    /**
     * @return The description of this filter.
     */
    public String getDescription() {
      return "Root directories of installed PEAR packages";
    }
  }

  /**
   * The <code>PackageFileFilter</code> class allows filtering PEAR package files.
   */
  public static class PackageFileFilter extends FileFilter {
    // valid file extensions
    private static final String TEAR_EXT = ".tear";

    private static final String PEAR_EXT = ".pear";

    /**
     * Determines whether a given <code>File</code> object is accepted by this filter.
     * 
     * @param file
     *          The given <code>File</code> object.
     * @return <code>true</code>, if the given <code>File</code> object is accepted,
     *         <code>fase</code> otherwise.
     */
    public boolean accept(File file) {
      if (file.isDirectory()) // show all directories
        return true;
      // show PEAR files
      String ext = FileUtil.getFileNameExtension(file.getName()).toLowerCase();
      return ext.equals(TEAR_EXT) || ext.equals(PEAR_EXT);
    }

    /**
     * @return The description of this filter.
     */
    public String getDescription() {
      return "PEAR package files";
    }
  }

  // persistent user preferences keys
  private static final String LAST_PACKAGE_DIR_KEY = "last_package_dir";

  private static final String LAST_PACKAGE_FILE_KEY = "last_package_file";

  // attributes
  private JFrame _dialogFrame = null;

  /**
   * Default constructor.
   */
  public PackageSelectorGUI() {
    _dialogFrame = new JFrame();
    _dialogFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
  }

  /**
   * Opens dialog window to select root directory of desired installed component package.
   * 
   * @param componentId
   *          The given component ID.
   * @return Selected package root directory or <code>null</code>, if the selection cancelled.
   */
  public synchronized File selectPackageDirectory(String componentId) {
    // get last package dir path
    Preferences userPrefs = Preferences.userNodeForPackage(getClass());
    String lastDirPath = (userPrefs != null) ? userPrefs.get(LAST_PACKAGE_DIR_KEY, "./") : "./";
    File lastDir = new File(lastDirPath);
    // create JFileChooser
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.addChoosableFileFilter(new PackageDirFilter());
    fileChooser.setCurrentDirectory(lastDir);
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fileChooser.setDialogTitle("Select " + componentId + " root directory");
    // show information dialog
    String message = "Select the root directory of installed\n" + "\n" + componentId + "\n"
            + "\ncomponent using the following file dialog,\n"
            + "or press 'Cancel' in the file dialog, if this\n"
            + "component is not installed in your file system.";
    JOptionPane.showMessageDialog(_dialogFrame, message);
    // open dialog window
    File selectedDir = null;
    int result = fileChooser.showDialog(_dialogFrame, "Select");
    if (result == JFileChooser.APPROVE_OPTION) {
      // set 'selected dir'
      selectedDir = fileChooser.getSelectedFile();
      if (selectedDir != null) {
        // set 'last dir' in user prefs
        lastDir = selectedDir.getParentFile();
        userPrefs.put(LAST_PACKAGE_DIR_KEY, lastDir.getAbsolutePath());
      }
    }
    return selectedDir;
  }

  /**
   * Opens dialog window to select desired PEAR package file for a given component.
   * 
   * @param componentId
   *          The given component ID.
   * @return Selected PEAR package file for the given component, or <code>null</code>, if the
   *         selection cancelled.
   */
  public synchronized File selectPackageFile(String componentId) {
    // get last package file path
    Preferences userPrefs = Preferences.userNodeForPackage(getClass());
    String lastFilePath = (userPrefs != null) ? userPrefs.get(LAST_PACKAGE_FILE_KEY, "") : "";
    File lastFile = lastFilePath.length() > 0 ? new File(lastFilePath) : null;
    File lastDir = lastFile != null ? lastFile.getParentFile() : new File(".");
    // create JFileChooser
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.addChoosableFileFilter(new PackageFileFilter());
    fileChooser.setCurrentDirectory(lastDir);
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setDialogTitle("Select " + componentId + " PEAR file");
    // show information dialog
    String message = "Select the PEAR file of the\n" + "\n" + componentId + "\n"
            + "\ncomponent using the following file dialog.";
    JOptionPane.showMessageDialog(_dialogFrame, message);
    // open dialog window
    File selectedFile = null;
    int result = fileChooser.showDialog(_dialogFrame, "Select");
    if (result == JFileChooser.APPROVE_OPTION) {
      // set 'selected file'
      selectedFile = fileChooser.getSelectedFile();
      if (selectedFile != null) {
        // set 'last file' in user prefs
        userPrefs.put(LAST_PACKAGE_FILE_KEY, selectedFile.getAbsolutePath());
      }
    }
    return selectedFile;
  }

  /**
   * This method is not implemented. It always returns <code>null</code>.
   * 
   * @param componentId
   *          The given component ID.
   * @return The PEAR package URL for the given component or <code>null</code>, if no URL is
   *         entered.
   */
  public URL selectPackageUrl(String componentId) {
    return null;
  }
}
