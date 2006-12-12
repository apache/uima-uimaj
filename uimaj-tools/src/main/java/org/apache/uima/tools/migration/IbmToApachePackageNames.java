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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.uima.internal.util.FileUtils;


/**
 * Migration utility for converting from IBM UIMA to Apache UIMA.
 * Updates import statements in .java files to reflect the new org.apache package names.
 */
public class IbmToApachePackageNames {
  private static List apacheUimaPackageNames = new ArrayList();
  private static List ibmUimaPackageNames = new ArrayList();
  
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
    //get lists of apache UIMA package names and the corresponding IBM UIMA package names
    readApacheUimaPackageNames();
    for (int i = 0; i < apacheUimaPackageNames.size(); i++) {
      String apachePkg = (String)apacheUimaPackageNames.get(i);
      ibmUimaPackageNames.add(apachePackageToUimaPackage(apachePkg));
    }
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
    for (int i = 0; i < apacheUimaPackageNames.size(); i++) {
      String apachePkg = (String)apacheUimaPackageNames.get(i);
      String ibmPkg = (String)ibmUimaPackageNames.get(i);
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
   * @param apachePkg
   * @return
   */
  private static String apachePackageToUimaPackage(String apachePkg) {
    //mapping is complicated for "impl" classes other than cas.impl
    int indexOfImpl = apachePkg.indexOf(".impl");
    if (indexOfImpl > 0 && apachePkg.indexOf("cas.impl") == -1) {
      //remove the .impl, and replace org.apache.uima with com.ibm.uima.reference_impl
      String noImpl = apachePkg.replaceAll(".impl", "");
      return noImpl.replaceAll("org\\.apache\\.uima","com.ibm.uima.reference_impl");      
    }
    else {
      //just apply simple replacement of org.apache to com.ibm
      return apachePkg.replaceAll("org\\.apache", "com.ibm");
    }
    
  }


  /**
   * Reads the list of apache UIMA packages from a resource file.
   */
  private static void readApacheUimaPackageNames() throws IOException {
    URL pkgListFile = IbmToApachePackageNames.class.getResource("uimaPackageNames.txt");
    // Read from this URL into a string using a char buffer.
    char[] buf = new char[10000];
    int charsRead;
    InputStream inStream = pkgListFile.openStream();
    BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
    StringBuffer strbuf = new StringBuffer();
    while ((charsRead = reader.read(buf)) >= 0) {
      strbuf.append(buf, 0, charsRead);
    }
    reader.close();
    StringTokenizer tokenizer = new StringTokenizer(new String(buf),",");
    while (tokenizer.hasMoreTokens())
    {
      String tok = tokenizer.nextToken().trim();
      if (tok.length() > 0)
        apacheUimaPackageNames.add(tok);
    }
  }
}
