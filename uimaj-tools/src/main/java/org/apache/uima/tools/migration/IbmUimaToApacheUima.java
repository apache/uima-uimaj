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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.util.FileUtils;


/**
 * Migration utility for converting from IBM UIMA to Apache UIMA.
 * Updates package names and does various other string replacements.
 * Should be run on java code, descriptors, and other files that may have UIMA
 * package names in them (e.g., launch configurations, scripts).
 */
public class IbmUimaToApacheUima {
  private static List replacements = new ArrayList();
  private static int MAX_FILE_SIZE = 1000000; //don't update files bigger than this
  private static Set extensions = new HashSet();
  private static int filesScanned = 0;
  private static int filesModified = 0;
  private static List filesNeedingManualAttention = new ArrayList();
  private static Set ibmPackageNames = new HashSet();

  private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^\\s*import\\s+([^;]*);\\s*$");
  private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("public\\s+(final\\s+|abstract\\s+)*class\\s+([A-Za-z0-9_]+)");
  private static final Pattern GET_NEXT_INDEX_PATTERN = Pattern.compile("JCas\\.getNextIndex\\(\\)");
  private static final Pattern THROW_FEAT_MISSING_PATTERN = Pattern.compile("JCas\\.throwFeatMissing");
  private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?m)^\\s*package\\s+([A-Za-z0-9_.]+)\\s*;") ;
  private static final Pattern GETDOCANNOT_PATTERN = Pattern.compile("[Jj][Cc][Aa][Ss](\\(\\))?\\s*\\.\\s*getDocumentAnnotation\\(");

  /**
   * Main program.  Expects one argument, the name of a directory containing files to
   * update.  Subdirectories are processed recursively.   
   * @param args  Command line arguments  
   * @throws IOException if an I/O error occurs
   */
  public static void main(String[] args) throws IOException{
    //parse command line
    String dir = null;
    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith("-")) {
        if (args[i].equals("-ext")) {
          if (i + 1 >= args.length) {
            printUsageAndExit();
          }
          parseCommaSeparatedList(args[++i], extensions);
        }
        else {
          System.err.println("Unknown switch " + args[i]);
          printUsageAndExit();
        }
      }
      else {
        if (dir != null) {
          printUsageAndExit();
        }
        else {
          dir = args[i];
        }
      }
    }
    if (dir == null) {
      printUsageAndExit();
    }

    //read resource files
    //map from IBM UIMA package names to Apache UIMA package names
    readMapping("packageMapping.txt", replacements, true);
    //other string replacements
    readMapping("stringReplacements.txt", replacements, false);

    //from system property, get list of file extensions to exclude
    
    //do the replacements
    System.out.println("Migrating your files...");
    replaceInAllFiles(new File(args[0]));
    
    System.out.println("Migration complete.");
    System.out.println("Scanned " + filesScanned + " files.  " + filesModified + " files modified.");
    if (filesNeedingManualAttention.size() > 0) {
      System.out.println("The following files may need manual attention:");
      for (int i = 0; i < filesNeedingManualAttention.size(); i++) {
        System.out.println("   " + filesNeedingManualAttention.get(i));
      }
      System.out.println("See the \"Migrating from IBM UIMA to Apache UIMA\" chapter in the " +
              "\"UIMA Overview and Setup\" document for details.");
    }
    else {
      System.out.println("No problems were detected.  However, if the code does not compilie " +
              "and run, see the \"Migrating from IBM UIMA to Apache UIMA\" chapter in the " +
              "\"UIMA Overview and Setup\" document for assistance.");
      
    }
  }

  /**
   * Parses a comma separated list, entering each value into the results Collection.
   * Trailing empty strings are included in the results Collection.
   * @param string string to parse
   * @param results Collection to which each value will be added
   */
  private static void parseCommaSeparatedList(String string, Collection results) {
    String[] components = string.split(",",-1);
    for (int i = 0; i < components.length; i++) {
      results.add(components[i]);
    }    
  }

  
  private static void printUsageAndExit() {
    System.err.println("Usage: java " + IbmUimaToApacheUima.class.getName() + " <directory> [-ext <fileExtensions>]");
    System.err.println("<fileExtensions> is a comma separated list of file extensions to process, e.g.: java,xml,properties");
    System.err.println("\tUse a trailing comma to include files with no extension (meaning their name contains no dot)");
    System.exit(1);
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
        //skip files with extensions specified in the excludes list
        if (!extensions.isEmpty()) {
          String filename = file.getName();
          String ext="";
          int lastDot = filename.lastIndexOf('.');
          if (lastDot > -1) {
            ext = filename.substring(lastDot+1);
          }
          if (!extensions.contains(ext.toLowerCase())) {
            continue;
          }
        }
        
        //skip files that we can't read and write
        if (!file.canRead()) {
          System.err.println("Skipping unreadable file: " + file.getCanonicalPath());
          continue;
        }
        if (!file.canWrite()) {
          System.err.println("Skipping unwritable file: " + file.getCanonicalPath());
          continue;
        }
        //skip files that are too big
        if (file.length() > MAX_FILE_SIZE) {
          System.out.println("Skipping file " + file.getCanonicalPath() + " with size: " + file.length() + " bytes");
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
    //apply replacements
    Iterator iter = replacements.iterator();
    while (iter.hasNext()) {
      Replacement replacement = (Replacement)iter.next();
      contents = contents.replaceAll(replacement.regex, replacement.replacementStr);
    }

    //for .java files do some additional processing
    if (file.getName().endsWith(".java")) {
      //updates for JCas/JCasRegistry refactoring
      contents = applyJCasRefactoring(contents);
      //remove duplicate imports (can be caused by some replacements)
      contents = removeDuplicateImports(contents);
    }
    
    //write file if it changed
    if (!contents.equals(original)) {
      FileUtils.saveString2File(contents, file);
      filesModified++;
    }
    filesScanned++;
    
    //check for situations that may need manual attention,
    //updates filesNeedingManualAttention field
    checkForManualAttentionNeeded(file, original);
    
  }

  /*
   * Applies changes needed due to JCas/JCasRegistry refactoring.  These are a little
   * more complicated than simple regex replacements.
   * 
   * JCas.getNextIndex -> JCasRegistry.register(ThisClass.class)
   * JCas.throwFeatMissing -> jcasType.jcas.throwFeatMissing [in cover class]
   * JCas.throwFeatMissing -> jcas.throwFeatMissing [in _Type class]
   */
  private static String applyJCasRefactoring(String contents) {
    //find the class name, we'll need it later
    Matcher classNameMatcher = CLASS_NAME_PATTERN.matcher(contents);
    if (!classNameMatcher.find()) 
      return contents;
    String className = classNameMatcher.group(2);
    
    //replace getNextIndex
    Matcher getNextIndexMatcher = GET_NEXT_INDEX_PATTERN.matcher(contents);
    String replacement = "org.apache.uima.jcas.JCasRegistry.register(" + className + ".class)";
    contents = getNextIndexMatcher.replaceAll(replacement);
    
    //replace throwFeatMissing (replacement depends on whether we're in _Type object or not)
    Matcher throwFeatMissingMatcher = THROW_FEAT_MISSING_PATTERN.matcher(contents);
    if (className.endsWith("_Type")) {
      contents = throwFeatMissingMatcher.replaceAll("this.jcas.throwFeatMissing");
    } 
    else {
      contents = throwFeatMissingMatcher.replaceAll("this.jcasType.jcas.throwFeatMissing");
    }
    return contents;      
  }

  /**
   * Remove duplicate imports from a Java source file.
   */
  private static String removeDuplicateImports(String contents) {
    HashSet classes = new HashSet();
    Matcher matcher = IMPORT_PATTERN.matcher(contents);
    int pos = 0;
    int endOfLastDuplicate = 0;
    StringBuffer result = null;
    while (matcher.find(pos)) {
      String className = matcher.group(1);
      //account for whitespace in class name
      className = className.replaceAll("\\s*","");
      if (!classes.add(className)) {
        //duplicate import found.  Do not append the import,
        //but get everything else before it.
        if (result == null) {
          result = new StringBuffer(contents.length());
        }
        result.append(contents.substring(endOfLastDuplicate, matcher.start()));
        endOfLastDuplicate = matcher.end();
      }
      pos = matcher.end();
    }
    if (result == null) {
      //no duplicates found
      return contents;
    }
    else {
      result.append(contents.substring(endOfLastDuplicate));
      return result.toString();
    }
  }
  
  
  /**
   * Scans for certain patterns in the string that indicate situations
   * that the migration tool doesn't resolve and may require user 
   * attention.  Updated the filesNeedingManualAttention field with a String
   * which is the file path plus the reason the file was flagged.
   * 
   * @param contents string to scan
   * @return true if the file needs manual attention
   */
  private static void checkForManualAttentionNeeded(File file, String contents) {
    // UIMA package name (includes most common case of DocumentAnnotation)
    Matcher packageNameMatcher = PACKAGE_PATTERN.matcher(contents);
    if (packageNameMatcher.find()) {
      String packageName = packageNameMatcher.group(1);
      if (ibmPackageNames.contains(packageName)) {
        filesNeedingManualAttention.add(file.getPath() + " (Uses an IBM UIMA Package Name)");
        return;
      }
    }
    //JCas.getDocumentAnnotation (fuzzy, only matches if variable name / method
    //ends with jcas)
    if (GETDOCANNOT_PATTERN.matcher(contents).find()) {
      filesNeedingManualAttention.add(file.getPath() + " (Calls JCas.getDocumentAnnotation())");
      return;
    }
    
    //xi:include
    if (contents.indexOf("<xi:include") >= 0) {
      filesNeedingManualAttention.add(file.getPath() + " (Uses xi:include)");
      return;
    }    
  }

  /**
   * Reads a mapping from a resource file, and populates a List of
   * Replacement objects.  We don't use a Map because the order in which
   * the replacements are applied can be important.
   * 
   * @param fileName name of file to read from (looked up looking using Class.getResource())
   * @param mappings List to which Replacement objects will be added.
   *   Each object contains the regex to search for and the replacement string.
   * @param treatAsPackageNames if true, the keys in the resource file will be considered
   *   package names, and this routine will produce regexes that replace any fully-qualified
   *   class name belonging to that package.  Also in this case updates the
   *   static ibmPackageNames HashSet.
   */
  private static void readMapping(String fileName, List mappings, boolean treatAsPackageNames) throws IOException {
    URL pkgListFile = IbmUimaToApacheUima.class.getResource(fileName);
    InputStream inStream = pkgListFile.openStream();
    BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
    String line = reader.readLine();
    while (line != null) {
      String[] mapping = line.split(" ");
      String regex, replaceStr;
      if (treatAsPackageNames) {
        //we do special processing for package names to try to handle the case where
        //user code exists in a package prefixed by com.ibm.uima.
        //We only replace the package name when it appears as part of a fully-qualified
        //class name in that package, not as a prefix of another package.

        //turn package name into a regex (have to escape the . and,
        //technically, should allow whitepsace around dots)
        String pkgRegex = mapping[0].replaceAll("\\.", "\\\\s*\\\\.\\\\s*");
        //form regex that will find any fully-qualified class name in this package
        regex = pkgRegex+"(\\.(\\*|[A-Z]\\w*))";
        replaceStr = mapping[1] + "$1";
        ibmPackageNames.add(mapping[0]);
      }
      else {
        //form regex from src, by escaping dots and allowing whitespace
        regex = mapping[0].replaceAll("\\.", "\\\\s*\\\\.\\\\s*");
        replaceStr = mapping[1];        
      }      
      
      Replacement replacement = new Replacement(regex, replaceStr);
      mappings.add(replacement);
      line = reader.readLine();
    }
    inStream.close();
  }
  
  private static class Replacement {
    String regex;
    String replacementStr;
    
    Replacement(String regex, String replacement) {
      this.regex = regex;
      this.replacementStr = replacement;
    }
  }
}
