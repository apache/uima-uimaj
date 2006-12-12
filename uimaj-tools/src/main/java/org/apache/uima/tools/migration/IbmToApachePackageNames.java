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
package org.apache.uima.tools.migration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.uima.internal.util.FileUtils;


/**
 * Migration utility for converting from IBM UIMA to Apache UIMA.
 * Updates import statements in .java files to reflect the new org.apache package names.
 */
public class IbmToApachePackageNames {
  private static Map packageMapping = new TreeMap();
  
  /**
   * Main program.  Expects one argument, the name of a directory containing files to
   * update.  Only .java files are updated.  Subdirectories are processed recursively.   
   * @param args  Command line arguments  
   * @throws IOException if an I/O error occurs
   */
  public static void main(String[] args) throws IOException{
    if (args.length < 1) {
      System.err.println("Usage: java " + IbmToApachePackageNames.class.getName() + " <directory>");
      System.exit(1);
    }
    //get map from IBM UIMA package names to Apache UIMA package names
    readPackageMapping();

    //do the replacement of import statements
    replaceImportsInAllFiles(new File(args[0]));
  }

  /**
   * Updates import statements in .java files, replacing packages in the
   * ibmUimaPackageNames list with the name at the corresponding index of
   * the apacheUimaPackageNames list.
   * 
   * @param dir diretory containing files to replace
   * @throws IOException if an I/O error occurs
   */
  private static void replaceImportsInAllFiles(File dir) throws IOException {
    File[] fileList = dir.listFiles();
    for (int i = 0; i < fileList.length; i++) {
      File file = fileList[i];
      // only apply to java files
      if (file.isFile() && file.getName().endsWith(".java")) {
        //check if we can read/write the file
        if (!file.canRead()) {
          System.err.println("Skipping unreadable file: " + file.getPath());
          continue;
        }
        if (!file.canWrite()) {
          System.err.println("Skipping unwritable file: " + file.getPath());
          continue;
        }
        replaceImports(file);
      }
      //recursively call on subdirectories
      if (file.isDirectory()) {
        replaceImportsInAllFiles(file);
      }
    }
  }
  
  /**
   * Update the import statements in a single file.
   * @param file the file to update
   */
  private static void replaceImports(File file) throws IOException {
    //read file
    String original = FileUtils.file2String(file);
    String contents = original;
    //loop over packages to replace
    Iterator entries = packageMapping.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry)entries.next();
      String ibmPkg = (String)entry.getKey();
      String apachePkg = (String)entry.getValue();
      //form regex to replace
      String regex = "import\\s*"+ibmPkg+"(\\.[^\\.]*;)";
      //replace
      contents = contents.replaceAll(regex, "import " + apachePkg + "$1");      
    }
    //write file if it changed
    if (!contents.equals(original)) {
      FileUtils.saveString2File(contents, file);
    }
  }

  /**
   * Reads the mapping from IBM UIMA package names to Apache UIMA
   * package names from a resource file and populates the packageMapping
   * field.
   */
  private static void readPackageMapping() throws IOException {
    URL pkgListFile = IbmToApachePackageNames.class.getResource("packageMapping.txt");
    InputStream inStream = pkgListFile.openStream();
    BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
    String line = reader.readLine();
    while (line != null) {
      String[] mapping = line.split("=");
      packageMapping.put(mapping[0],mapping[1]);
      line = reader.readLine();
    }
    inStream.close();
  }
}
