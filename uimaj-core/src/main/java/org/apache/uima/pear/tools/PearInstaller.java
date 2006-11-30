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
import java.io.IOException;
import java.util.jar.JarFile;

import org.apache.uima.UIMAFramework;
import org.apache.uima.pear.util.MessageRouter;
import org.apache.uima.util.Level;
import org.xml.sax.SAXException;

public class PearInstaller {

  private static final String PEAR_MESSAGE_RESOURCE_BUNDLE = "org.apache.uima.pear.pear_messages";

  /**
   * Installes the given pear package to the specified location.
   * 
   * @param installDir
   *          pear package install location
   * @param pearPackage
   *          pear package to install
   * @param verify
   *          if true a simple pear package verification is done after installation
   * @param installToRootDir
   *          if true the pear package is installed directly to the installDir, otherwise a
   *          subfolder with
   * @param localInstall
   *          if true local install mode is enabled the name of the pear package component id is
   *          created
   * 
   * @return PackageBrowser - pear package install settings
   * 
   * @throws PearInstallerException
   *           if an error during the pear installation occurs.
   */
  public static PackageBrowser installPearPackage(File installDir, File pearPackage, boolean verify,
          boolean installToRootDir, boolean localInstall) throws PearInstallerException {

    // componentId for the given pear pacakge
    String componentId;

    // get componentId from the given pear package, componentId needed to install the package
    // correctly
    try {
      // get installDescriptor from pear file
      JarFile jarFile = new JarFile(pearPackage);
      InstallationDescriptorHandler insdHandler = new InstallationDescriptorHandler();
      insdHandler.parseInstallationDescriptor(jarFile);
      InstallationDescriptor instObj = insdHandler.getInstallationDescriptor();
      if (instObj != null) {
        componentId = instObj.getMainComponentId();
      } else {
        throw new PearInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
                "error_parsing_pear_package_desc", new Object[] { pearPackage.getAbsolutePath() });
      }
    } catch (IOException ex) {
      throw new PearInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "error_parsing_pear_package_desc", new Object[] { pearPackage.getAbsolutePath() }, ex);
    } catch (SAXException ex) {
      throw new PearInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "error_parsing_pear_package_desc", new Object[] { pearPackage.getAbsolutePath() }, ex);
    }

    // create message listener for the pear installer
    MessageRouter.StdChannelListener msgListener = new MessageRouter.StdChannelListener() {
      public void errMsgPosted(String errMsg) {
        UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, errMsg);
      }

      public void outMsgPosted(String outMsg) {
        UIMAFramework.getLogger(this.getClass()).log(Level.INFO, outMsg);
      }
    };

    // create installation controller and to install the pear package
    InstallationController.setLocalMode(localInstall);
    InstallationController controller = new InstallationController(componentId, pearPackage,
            installDir, installToRootDir, msgListener);

    // install main component
    if (controller.installComponent() == null) {

      // installation failed
      String errorMessage = controller.getInstallationMsg();

      // stop controller messaging service
      controller.terminate();

      if (errorMessage != null) {
        throw new PearInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
                "error_installing_main_component", new Object[] { componentId, errorMessage });
      } else {
        throw new PearInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
                "error_installing_main_component_unknown", new Object[] { componentId });
      }
    }

    // installation now complete !!

    // save modified installation descriptor file
    try {
      controller.saveInstallationDescriptorFile();
    } catch (IOException ex) {

      // stop controller messaging service
      controller.terminate();

      throw new PearInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "error_installing_main_component", new Object[] { componentId, ex.getMessage() }, ex);
    }

    // create package browser object with the installed pear
    PackageBrowser pkgBrowser = null;
        
    try {
      //initialze package browser object based on the installation settings
      if (installToRootDir) {
        pkgBrowser = new PackageBrowser(installDir);
      } else {
        pkgBrowser = new PackageBrowser(new File(installDir, componentId));
      }

      // check if package browser could be initialized
      if (pkgBrowser == null) {
         throw new PearInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
                 "error_reading_installed_pear_settings", new Object[] { componentId });
      }
    } catch (IOException ex) {
      // stop controller messaging service
      controller.terminate();

      throw new PearInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "error_reading_installed_pear_settings", new Object[] { componentId }, ex);
    }

    // installation verification 
    if (verify) {
         if (!controller.verifyComponent()) {
           
           // verification failed
           String errorMessage = controller.getVerificationMsg();
           if (errorMessage != null) {
             throw new PearInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
                     "error_verify_installation", new Object[] { componentId, errorMessage });
           } else {
             throw new PearInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
                     "error_verify_installation_unknown", new Object[] { componentId });
           }
         }
    }

    // stop controller messaging service
    controller.terminate();
    
    return pkgBrowser;
  }
}
