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

package org.apache.uima.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


/**
 * Package all sources for UIMA into one Jar
 * 
 *  arguments: 
 *  
 *  Arg[0] - path to install location for source version of UIMA
 *             where path/proj-name/src/main/java dirs can be found
 *  Arg[1] - (optional) where to write the output Jar. 
 *           If not specified, the current directory is used.
 */
public class PackageAllJavaSourcesIntoJar {

  public static void main(String[] args) throws IOException {
    if (args == null || args.length == 0) {
      System.err.println("Usage: java -jar uima-tools.jar PackageAllJavaSourceIntoJar path-to-sources optional-where-to-put-created-jar.");
      System.err.println("  First argument is path to the install location for the source version of UIMA,");
      System.err.println("    where path/proj-name/src/main/java directories can be found.");
      System.err.println("  The second argument is optional, and specifies the path to a directory where the resulting jar will be written;");
      System.err.println("   If not specified, the current directory is used.");
      System.err.println("  JAVA_HOME environment variable, if set, is used to locate the jar command.");
      System.exit(1);
    }    

    String outputDir = (args.length == 2) ? args[1] : System.getProperty("user.dir");
    File dirSources = new File(args[0]);
    if (!dirSources.isDirectory()) {
      System.err.format("The specified path to the sources directory, %s, is not a directory.", args[0]);
      return;
    }
    String dirSourcesPath = dirSources.getAbsolutePath();
    
    List<String> pathsToSources = getPathsToSources(dirSourcesPath);
    
    createSourceJar(pathsToSources, outputDir);
    System.out.println("Finished");
  }
  
  private static void createSourceJar(List<String> pathsToSources, String outputDir) throws IOException {
    String javaHome = System.getenv("JAVA_HOME");
    String jarCmd = (javaHome == null) ? "jar" : 
      javaHome + File.separator + "bin" + File.separator + "jar";

    StringBuilder sb = new StringBuilder();
    for (String s : pathsToSources) {
      if (sb.length() != 0) {
        sb.append(" ");
      }
      sb.append('"');
      sb.append(s);
      sb.append('"');
    }
    
    
    ProcessBuilder pb = new ProcessBuilder(
        "\"" + jarCmd + "\"", 
        "-cvf",
        "\"" + outputDir + File.separator + "uima-sources.jar\"",
        sb.toString()
        );
    pb.redirectErrorStream(true); // merges error stream into output stream coming from subprocess
   
    Process p = pb.start();

    // read the output stream
    BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
    String line;
    while((line = in.readLine()) != null) {
      System.out.println(line);
    }
    
    try {
      p.waitFor();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    in.close();
  }
    
  private static List<String> getPathsToSources(String dirSourcesPath) {
    List<String> result = new ArrayList<String>();
    File dirSources = new File(dirSourcesPath);
    if (!dirSources.isDirectory()) {
      System.err.format("The specified path to the sources directory is missing %s or is not a directory.", dirSourcesPath);
      throw new RuntimeException();
    }

    for (File proj : dirSources.listFiles()) {
      File test = new File(proj.getAbsoluteFile() 
          + File.separator + "src" + File.separator + "main" + File.separator + "java");
      if (test.exists() && test.isDirectory()) {
        result.add(test.getAbsolutePath());
      }
    }
    return result;
  }
  
}
