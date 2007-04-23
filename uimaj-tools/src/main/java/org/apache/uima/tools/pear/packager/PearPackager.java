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

package org.apache.uima.tools.pear.packager;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.uima.internal.util.CommandLineParser;
import org.apache.uima.pear.tools.PackageCreator;

/**
 * PEAR package command line interface.
 * 
 * The class provides a command line interface to package a PEAR file or to just creat an PEAR
 * installation descriptor.
 * 
 * <pre>
 *     Usage:
 *     
 *     The PearPackager can be used in three different modes:
 *     Mode 1: create a complete PEAR package (default mode)
 *     Mode 2: just create the PEAR installation descriptor
 *     Mode 3: just package a PEAR file
 *     
 *     Mode 1 usage: java org.apache.uima.tools.pear.packager.PearPackager -compID &lt;componentID&amp;gt -mainCompDesc &lt;mainComponentDesc&amp;gt [-classpath &lt;classpath&amp;gt] [-datapath &lt;datapath&amp;gt] -mainCompDir &lt;mainComponentDir&amp;gt -targetDir &lt;targetDir&amp;gt [-envVars &lt;propertiesFilePath&amp;gt]
 *     Mode 2 usage: java org.apache.uima.tools.pear.packager.PearPackager -create -compID &lt;componentID&amp;gt -mainCompDesc &lt;mainComponentDesc&amp;gt [-classpath &lt;classpath&amp;gt] [-datapath &lt;datapath&amp;gt] -mainCompDir &lt;mainComponentDir&amp;gt [-envVars &lt;propertiesFilePath&amp;gt]
 *     Mode 3 usage: java org.apache.uima.tools.pear.packager.PearPackager -package -compID &lt;componentID&amp;gt -mainCompDir &lt;mainComponentDir&amp;gt -targetDir &lt;targetDir&amp;gt    
 * </pre>
 */
public class PearPackager {

  private static final String INSTALL_ACTION_PARAM = "-create";

  private static final String PACKAGE_ACTION_PARAM = "-package";

  private static final String COMPONENT_ID_PARAM = "-compID";

  private static final String MAIN_COMPONENT_DESC_PARAM = "-mainCompDesc";

  private static final String CLASSPATH_PARAM = "-classpath";

  private static final String DATAPATH_PARAM = "-datapath";

  private static final String ENV_VAR_PARAM = "-envVars";

  private static final String MAIN_COMPONENT_DIR_PARAM = "-mainCompDir";

  private static final String TARGET_DIR_PARAM = "-targetDir";

  private static final CommandLineParser createCmdLineParser() {
    CommandLineParser parser = new CommandLineParser();
    parser.addParameter(INSTALL_ACTION_PARAM, false);
    parser.addParameter(PACKAGE_ACTION_PARAM, false);
    parser.addParameter(COMPONENT_ID_PARAM, true);
    parser.addParameter(MAIN_COMPONENT_DESC_PARAM, true);
    parser.addParameter(CLASSPATH_PARAM, true);
    parser.addParameter(DATAPATH_PARAM, true);
    parser.addParameter(ENV_VAR_PARAM, true);
    parser.addParameter(MAIN_COMPONENT_DIR_PARAM, true);
    parser.addParameter(TARGET_DIR_PARAM, true);
    return parser;
  }

  private static final void printUsage() {
    System.out.println("The PearPackager can be used in three different modes: ");
    System.out.println("Mode 1: create a complete PEAR package (default mode)");
    System.out.println("Mode 2: just create the PEAR installation descriptor");
    System.out.println("Mode 3: just package a PEAR file \n");
    System.out
            .println("Mode 1 usage: java org.apache.uima.tools.pear.packager.PearPackager -compID <componentID> -mainCompDesc <mainComponentDesc> [-classpath <classpath>] [-datapath <datapath>] -mainCompDir <mainComponentDir> -targetDir <targetDir> [-envVars <propertiesFilePath>]\n");
    System.out
            .println("Mode 2 usage: java org.apache.uima.tools.pear.packager.PearPackager -create -compID <componentID> -mainCompDesc <mainComponentDesc> [-classpath <classpath>] [-datapath <datapath>] -mainCompDir <mainComponentDir> [-envVars <propertiesFilePath>]\n");
    System.out
            .println("Mode 3 usage: java org.apache.uima.tools.pear.packager.PearPackager -package -compID <componentID> -mainCompDir <mainComponentDir> -targetDir <targetDir>");
  }

  private static final boolean checkCmdLineSyntax(CommandLineParser clp) {

    // check parameters depended on the action
    // check parameters for the creation of the installation descriptor
    if (clp.isInArgsList(INSTALL_ACTION_PARAM)) {
      if ((!clp.isInArgsList(COMPONENT_ID_PARAM)) || (!clp.isInArgsList(MAIN_COMPONENT_DESC_PARAM))
              || (!clp.isInArgsList(MAIN_COMPONENT_DIR_PARAM))) {
        return false;
      }
    }
    // check parameters for the pear packaging
    if (clp.isInArgsList(PACKAGE_ACTION_PARAM)) {
      if ((!clp.isInArgsList(COMPONENT_ID_PARAM)) || (!clp.isInArgsList(TARGET_DIR_PARAM))
              || (!clp.isInArgsList(MAIN_COMPONENT_DIR_PARAM))) {
        return false;
      }
    }
    // check parameters if no special action is set - do install descriptor creation and pear
    // packaging
    if ((!clp.isInArgsList(INSTALL_ACTION_PARAM)) && (!clp.isInArgsList(PACKAGE_ACTION_PARAM))) {
      if ((!clp.isInArgsList(COMPONENT_ID_PARAM)) || (!clp.isInArgsList(MAIN_COMPONENT_DESC_PARAM))
              || (!clp.isInArgsList(MAIN_COMPONENT_DIR_PARAM))
              || (!clp.isInArgsList(TARGET_DIR_PARAM))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Main method to call the command line PearPackager
   * 
   * @param args
   *          Command line arguments to configure the PEAR packaging
   */
  public static void main(String[] args) {

    String installDesc = null;
    String pearFile = null;
    
    try {
      // create command line parser
      CommandLineParser clp = createCmdLineParser();

      // parse command line parameters and check syntax
      clp.parseCmdLine(args);
      if (!checkCmdLineSyntax(clp)) {
        printUsage();
        System.exit(2);
      }

      // check what we have to do
      boolean doInstallAction = false;
      boolean doPackageAction = false;

      // if install action is set, we have to create the installation descriptor
      if (clp.isInArgsList(INSTALL_ACTION_PARAM)) {
        doInstallAction = true;
      }
      // if package action is set, we have to create the pear package
      if (clp.isInArgsList(PACKAGE_ACTION_PARAM)) {
        doPackageAction = true;
      }
      // if both actions are not set, we have to create the installation descriptor and the pear
      // package
      if ((!clp.isInArgsList(INSTALL_ACTION_PARAM)) && (!clp.isInArgsList(PACKAGE_ACTION_PARAM))) {
        doInstallAction = true;
        doPackageAction = true;
      }

      // do install action
      if (doInstallAction) {

        // check envVars properties file
        String filename = null;
        Properties properties = null;
        if ((filename = clp.getParamArgument(ENV_VAR_PARAM)) != null) {
          properties = new Properties();
          properties.load(new FileInputStream(filename));
        }

        // create installation descriptor
        installDesc = PackageCreator.createInstallDescriptor(clp
                .getParamArgument(COMPONENT_ID_PARAM), clp
                .getParamArgument(MAIN_COMPONENT_DESC_PARAM),
                clp.getParamArgument(CLASSPATH_PARAM), clp.getParamArgument(DATAPATH_PARAM), clp
                        .getParamArgument(MAIN_COMPONENT_DIR_PARAM), properties);

      }

      // do package action
      if (doPackageAction) {
        pearFile = PackageCreator
                .createPearPackage(clp.getParamArgument(COMPONENT_ID_PARAM), clp
                        .getParamArgument(MAIN_COMPONENT_DIR_PARAM), clp
                        .getParamArgument(TARGET_DIR_PARAM));
      }

    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }
    
    System.out.println("Operation successfully finished");
    
    if(installDesc != null && pearFile == null) {
      System.out.println("Installation descriptor created at: " + installDesc);
    }
    if(pearFile != null) {
      System.out.println("Pear package created at: " + pearFile);
    }

  }
}
