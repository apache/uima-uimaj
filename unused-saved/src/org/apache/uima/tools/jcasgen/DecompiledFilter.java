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
package org.apache.uima.tools.jcasgen;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.apache.uima.tools.jcas.internal.AnalyzeContent;


/**
 * The Class DecompiledFilter.
 */
public class DecompiledFilter {

  /** The built in java names. */
  static private Set<String> BUILT_IN_JAVA_NAMES = new HashSet<>();
  
  static {
    for (TypeInfo ti : Jg.builtInTypes.values()) {
      BUILT_IN_JAVA_NAMES.add(ti.javaNameWithPkg + ".java");
    }
  }
  
  /** The common dir. */
  Path commonDir;
  
  /** The customized dir. */
  Path customizedDir;
  
  /**
   * Processes a directory of decompiled JCasgen files, and produces:
   *   a listing to system out of the fully qualified names of the non-customized files
   *   a parallel directory xxx-customized holding for all customized files
   *     a file named xxxCustomize.java
   *   built-in uima JCas cover classes are ignored.
   *       
   *
   * @param args  arg[0] is a relative or absolute path to the decompile directory
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static void main(String[] args) throws IOException {
    DecompiledFilter df = new DecompiledFilter();
    df.process(/*args[0]*/"/au/wksps/450uimaV3/learn/decompiled");
  }
  
  /**
   * Process.
   *
   * @param decompiledFilesStr the decompiled files str
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private void process(String decompiledFilesStr) throws IOException {
    Path decompiledFiles = FileSystems.getDefault().getPath(decompiledFilesStr);
    
    
    commonDir = decompiledFiles.getParent();
    customizedDir = Paths.get(commonDir.toString(), decompiledFiles.getFileName().toString() + "-customized");
    
//    System.out.println("List of all files:");
//    Files.list(decompiledFiles).forEachOrdered(p -> System.out.println(p));
    
    Files.list(decompiledFiles)
      .filter(p -> !p.toFile().isDirectory())  // skip directories
      .filter(p -> !isBuiltIn(p))            // skip if builtin
      .forEach(DecompiledFilter::extractCustomization);  
  }
  
  /**
   * Checks if is built in.
   *
   * @param p the p
   * @return true, if is built in
   */
  private static boolean isBuiltIn(Path p) {
    return BUILT_IN_JAVA_NAMES.contains(p.getFileName().toString());
  }
  
  // extract the customization (if any)
  /**
   * Extract customization.
   *
   * @param filePath the file path
   * @return true if the decompiled class is not customized.
   */
  private static boolean extractCustomization(Path filePath) {
    String content;
    try {
      content = new String(Files.readAllBytes(filePath), "UTF-8");
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return false;
    }
    
    AnalyzeContent m = new AnalyzeContent(content);
    
//    try {
//      if (m.isCustomized) {
//        if (!customizedDir.toFile().exists()) {
//          Files.createDirectory(customizedDir);
//        }
//        Files.write(Paths.get(customizedDir.toString()), m.customizedClass.toString().getBytes("UTF-8"));
//      }
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
    
    
//    if (m.isSubtypeOfAnnotation()) {
//      m.verifyCommonConstructorBeginEnd();
//    }
//    
//    m.verifyReadObject();
//    
//    if (m.isAtEnd()) return true;
//    
//    m.analyzeNextClause();
//    
//    if (m.isNextClauseCustomized()) {
//    
//    }
    if (m.isCustomized) {
//        System.out.println(filePath.toString());
      System.out.format("%s %s    %s%n", m.isCustomized ? "Cust   " : "notCust" , filePath.toString(), m.isCustomized ? m.msg : "");
    }
    return !m.isCustomized;
  }
  




}
