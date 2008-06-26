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
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * The <code>SimplePackageSelector</code> class provides simple command line dialog for selecting
 * root directories of installed PEAR packages, as well as PEAR package files, which contain
 * components that need to be installed.
 * 
 */

public class SimplePackageSelector implements InstallationController.PackageSelector {

  // attributes
  private PrintWriter _stdOut = null;

  private PrintWriter _stdErr = null;

  private BufferedReader _stdIn;

  /**
   * Constructor that takes given standard console streams writers.
   * 
   * @param stdOut
   *          The given standard output stream writer.
   * @param stdErr
   *          The given standard error stream writer.
   */
  public SimplePackageSelector(PrintWriter stdOut, PrintWriter stdErr) {
    this();
    _stdOut = stdOut;
    _stdErr = stdErr;
  }

  /**
   * Default constructor.
   */
  public SimplePackageSelector() {
    _stdIn = new BufferedReader(new InputStreamReader(System.in));
  }

  /**
   * Allows entering the installation directory path for a given component from the console window.
   * 
   * @param componentId
   *          The given component ID.
   * @return The installation directory for the given component or <code>null</code>, if the
   *         entered string is empty.
   */
  public File selectPackageDirectory(String componentId) {
    if (_stdOut != null) {
      _stdOut.println("> If the " + componentId
              + " component is already installed in your file system,");
      _stdOut.print("> enter the " + componentId + " root directory: ");
      _stdOut.flush();
    } else {
      System.out.println("> If the " + componentId
              + " component is already installed in your file system,");
      System.out.print("> enter the " + componentId + " root directory: ");
      System.out.flush();
    }
    File selectedDir = null;
    try {
      String dirPath = _stdIn.readLine();
      if (dirPath != null && dirPath.length() > 0) {
        File dir = new File(dirPath);
        if (dir.isDirectory()) {
          selectedDir = dir;
        } else {
          if (_stdErr != null)
            _stdErr.println("> invalid directory");
          else
            System.err.println("> invalid directory");
        }
      }
    } catch (Exception e) {
      if (_stdErr != null)
        e.printStackTrace(_stdErr);
      else
        e.printStackTrace(System.err);
    }
    return selectedDir;
  }

  /**
   * Allows entering the PEAR file path for a given component from the console window.
   * 
   * @param componentId
   *          The given component ID.
   * @return The PEAR file for the given component or <code>null</code>, if the entered string is
   *         empty.
   */
  public File selectPackageFile(String componentId) {
    if (_stdOut != null) {
      _stdOut.print("> Enter the " + componentId + " PEAR file path: ");
      _stdOut.flush();
    } else {
      System.out.print("> Enter the " + componentId + " PEAR file path: ");
      System.out.flush();
    }
    File selectedFile = null;
    try {
      String filePath = _stdIn.readLine();
      if (filePath != null && filePath.length() > 0) {
        File file = new File(filePath);
        if (file.isFile()) {
          selectedFile = file;
        } else {
          if (_stdErr != null)
            _stdErr.println("> file not found");
          else
            System.err.println("> file not found");
          selectedFile = null;
        }
      }
    } catch (Exception e) {
      if (_stdErr != null)
        e.printStackTrace(_stdErr);
      else
        e.printStackTrace(System.err);
    }
    return selectedFile;
  }

  /**
   * Allows entering the PEAR package URL for a given component from the console window.
   * 
   * @param componentId
   *          The given component ID.
   * @return The PEAR package URL for the given component or <code>null</code>, if the entered
   *         string is empty.
   */
  public URL selectPackageUrl(String componentId) {
    if (_stdOut != null) {
      _stdOut.print("> Enter the " + componentId + " PEAR package URL: ");
      _stdOut.flush();
    } else {
      System.out.print("> Enter the " + componentId + " PEAR package URL: ");
      System.out.flush();
    }
    URL selectedUrl = null;
    try {
      String urlString = _stdIn.readLine();
      if (urlString != null && urlString.length() > 0) {
        try {
          selectedUrl = new URL(urlString);
        } catch (MalformedURLException e) {
          if (_stdErr != null)
            _stdErr.println("> invalid URL - " + e.toString());
          else
            System.err.println("> invalid URL - " + e.toString());
          selectedUrl = null;
        }
      }
    } catch (Exception e) {
      if (_stdErr != null)
        e.printStackTrace(_stdErr);
      else
        e.printStackTrace(System.err);
    }
    return selectedUrl;
  }
}
