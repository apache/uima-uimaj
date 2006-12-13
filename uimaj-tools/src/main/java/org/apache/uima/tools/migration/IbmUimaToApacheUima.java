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
 * Updates package names and does various other string replacements.
 * Should be run on java code, descriptors, and other files that may have UIMA
 * package names in them (e.g., launch configurations, scripts).
 */
public class IbmUimaToApacheUima {
  private static Map packageMapping = new TreeMap();
  private static Map stringReplacements = new TreeMap();
  private static int MAX_FILE_SIZE = 1000000; //don't update files bigger than this
  
  /**
   * Main program.  Expects one argument, the name of a directory containing files to
   * update.  Subdirectories are processed recursively.   
   * @param args  Command line arguments  
   * @throws IOException if an I/O error occurs
   */
  public static void main(String[] args) throws IOException{
    if (args.length < 1) {
      System.err.println("Usage: java " + IbmUimaToApacheUima.class.getName() + " <directory>");
      System.exit(1);
    }
    //read resource files
    //mapp from IBM UIMA package names to Apache UIMA package names
    readMapping("packageMapping.txt", packageMapping);
    //other string replacements
    readMapping("stringReplacements.txt", stringReplacements);

    //do the replacements
    replaceInAllFiles(new File(args[0]));
  }

  /**
   * Applies the necessary replacements to all files in the given directory.
   * Subdirectories are processed recursively.
   * 
   * @param dir diretory containing files to replace
   * @throws IOException if an I/O error occurs
   */
  private static void replaceInAllFiles(File dir) throws IOException {
    File[] fileList = dir.listFiles();
    for (int i = 0; i < fileList.length; i++) {
      File file = fileList[i];
      if (file.isFile()) {
        //skip files that we can't read and write
        if (!file.canRead()) {
          System.err.println("Skipping unreadable file: " + file.getPath());
          continue;
        }
        if (!file.canWrite()) {
          System.err.println("Skipping unwritable file: " + file.getPath());
          continue;
        }
        //skip files that are too big
        if (file.length() > MAX_FILE_SIZE) {
          System.err.println("Skipping file " + file.getPath() + " with size: " + file.length() + " bytes");
          continue;
        }
        
        //do the replacements
        replaceInFile(file);
      }
      
      //recursively call on subdirectories
      if (file.isDirectory()) {
        replaceInAllFiles(file);
      }
    }
  }
  

  /**
   * Applies replacements to a single file.
   * @param file the file to process
   */
  private static void replaceInFile(File file) throws IOException {
    //read file
    String original;
    try {
      original = FileUtils.file2String(file);
    }
    catch(IOException e) {
      System.err.println("Error reading " + file.getCanonicalPath());
      System.err.println(e.getMessage());
      return;
    }
    String contents = original;
    //loop over packages to replace
    //we do special processing for package names to try to handle the case where
    //user code exists in a package prefixed by com.ibm.uima.
    //in .java files, we only replace imports
    //in other files, we only replace the package name when it appears on its own,
    //not as a prefix of another package.
    Iterator entries = packageMapping.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry)entries.next();
      String ibmPkg = (String)entry.getKey();
      String apachePkg = (String)entry.getValue();
      //apply replacement (depends on whether this is a .java file)
      if (file.getName().endsWith(".java")) {
        String regex = "import\\s*"+ibmPkg+"(\\.[^\\.]*;)";
        contents = contents.replaceAll(regex, "import " + apachePkg + "$1");
      }
      else {
        String regex = ibmPkg+"(\\.[^\\.]*[\\W&&[^\\.]])";
        contents = contents.replaceAll(regex, apachePkg + "$1");
      }
    }
    //now apply simple string replacements
    entries = stringReplacements.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry)entries.next();
      String src = (String)entry.getKey();
      String dest = (String)entry.getValue();
      //replace
      contents = contents.replaceAll(src, dest);      
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
  private static void readMapping(String fileName, Map map) throws IOException {
    URL pkgListFile = IbmUimaToApacheUima.class.getResource(fileName);
    InputStream inStream = pkgListFile.openStream();
    BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
    String line = reader.readLine();
    while (line != null) {
      String[] mapping = line.split(" ");
      map.put(mapping[0],mapping[1]);
      line = reader.readLine();
    }
    inStream.close();
  }
}
