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

package org.apache.uima.tools.pear.install;

import java.io.File;
import java.io.IOException;

import org.apache.uima.pear.tools.PackageBrowser;
import org.apache.uima.pear.tools.PackageInstaller;
import org.apache.uima.pear.tools.PackageInstallerException;

public class InstallPearCli {

  /**
   * @param args
   */
  
  public static void main(String[] args) {
//    OptionParser parser = new OptionParser();
//    OptionSpec<File> infile = parser.acceptsAll(Arrays.asList("f","file"), "PEAR file")
//    .withRequiredArg().ofType( File.class ).describedAs( "filename" );
//    OptionSpec<File> outdir = parser.acceptsAll(Arrays.asList("d","dir"), "installation directory")
//    .withRequiredArg().ofType( File.class ).describedAs( "directory (default: use PEAR name)" );
//    parser.acceptsAll(Arrays.asList("c", "check", "verify"), "do verification");
//    parser.acceptsAll(Arrays.asList("v", "verbose"), "be more verbose");
//    parser.acceptsAll(Arrays.asList( "h", "?" ), "show help");
    
    if (null == args || 
        args.length == 0 ||
        "?".equals(args[0]) ||
        "-h".equals(args[0]) ||
        "-help".equals(args[0])) {
      printUsageAndExit();
    }
    File pathToPear = new File(args[0]);
    File installDir = null;
    boolean doVerification = false;
    for (int i = 1 ; i < args.length; i++) {
      String a = args[i];
      if (a.equals("-c") || a.equals("-check") || a.equals("-verify")) {
        doVerification = true;
      } else if (a.startsWith("-")) {
        printUsageAndExit();
      } else {
        installDir = new File(a);
      }
    }
    
    if (installDir == null) {
      installDir = new File(pathToPear.getName().replace(".pear", "")); 
    }
    
    installPear(installDir, pathToPear, doVerification);
  }

  private static void installPear(File installDir, File pearFile, boolean doVerification) {

    try {
      // install PEAR package
      PackageBrowser instPear = PackageInstaller.installPackage(
          installDir, pearFile, doVerification);

      // retrieve installed PEAR data
      // PEAR package classpath
      String classpath = instPear.buildComponentClassPath();
      // PEAR package datapath
      String datapath = instPear.getComponentDataPath();
      // PEAR package main component descriptor
      String mainComponentDescriptor = instPear
      .getInstallationDescriptor().getMainComponentDesc();
      // PEAR package component ID
      String mainComponentID = instPear
      .getInstallationDescriptor().getMainComponentId();
      // PEAR package pear descriptor
      String pearDescPath = instPear.getComponentPearDescPath();

      // print out settings
      System.out.println("PEAR package class path: " + classpath);
      System.out.println("PEAR package datapath: " + datapath);
      System.out.println("PEAR package mainComponentDescriptor: "
          + mainComponentDescriptor);
      System.out.println("PEAR package mainComponentID: "
          + mainComponentID);
      System.out.println("PEAR package specifier path: " + pearDescPath);
      System.out.println("PEAR installed successfully");
    } catch (PackageInstallerException ex) {
      // catch PackageInstallerException - PEAR installation failed
      ex.printStackTrace();
      System.out.println("PEAR installation failed");
    } catch (IOException ex) {
      ex.printStackTrace();
      System.out.println("Error retrieving installed PEAR settings");
    }
  }
  private static void printUsageAndExit() {
    System.out
            .println("Usage: installPearCli pathToPearFile [directoryToInstallInto] [options]\n\n"
                    + "  *** items in [] are optional ***"
                    + "Install a Pear file into a specified directory\n"
                    + "Install directory defaults to the current directory/pearFileName\n"
                    + "Options:\n"
                    + " -c or -check or -verify: Run the Pear validation checks after installing\n");
    System.exit(1);
  }
}
