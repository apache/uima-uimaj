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



/**
 * The <code>PackageInstaller</code> class is the main user API for installing PEAR packages.
 * The class has a static method <code>installPackage</code> to install PEAR packages that returns a 
 * <code>PackageBrowser</code> object containing all the needed information about the installed
 * PEAR package. 
 * 
 * @see org.apache.uima.pear.tools.PackageBrowser
 *
 */
public class PackageInstaller {

  private static final String PEAR_MESSAGE_RESOURCE_BUNDLE = "org.apache.uima.pear.pear_messages";

  /**
   * Installes the specified PEAR package to the specified install location. After the installation
   * a simple installation verification step can be executed. This verification starts the PEAR package
   * and call the <code>process</code> method.
   * 
   * @param installDir
   *          PEAR package install location
   * @param pearPackage
   *          PEAR package file location to install
   * @param verify
   *          If true the PEAR package verification is done after the installation
   * 
   * @return Returns a <code>PackageBrowser</code> object containing all PEAR package install settings
   * 
   * @throws PackageInstallerException
   *           If an error occured during the pear installation or verification.
   */
  public static PackageBrowser installPackage(File installDir, File pearPackage, boolean verify)
          throws PackageInstallerException {

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
        throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
                "error_parsing_pear_package_desc", new Object[] { pearPackage.getAbsolutePath() });
      }
    } catch (IOException ex) {
      throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "error_parsing_pear_package_desc", new Object[] { pearPackage.getAbsolutePath() }, ex);
    } catch (SAXException ex) {
      throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "error_parsing_pear_package_desc", new Object[] { pearPackage.getAbsolutePath() }, ex);
    }

    // create message listener for the pear installer
    MessageRouter.StdChannelListener msgListener = new MessageRouter.StdChannelListener() {
      public void errMsgPosted(String errMsg) {
        UIMAFramework.getLogger(PackageInstaller.class).logrb(Level.SEVERE, "PackageInstaller",
                "installPackage", PEAR_MESSAGE_RESOURCE_BUNDLE, "package_installer_error", errMsg);
      }

      public void outMsgPosted(String outMsg) {
        UIMAFramework.getLogger(PackageInstaller.class)
                .logrb(Level.INFO, "PackageInstaller", "installPackage",
                        PEAR_MESSAGE_RESOURCE_BUNDLE, "package_installer_message", outMsg);
      }
    };

    // create installation controller and to install the pear package
    InstallationController controller = new InstallationController(componentId, pearPackage,
            installDir, false, msgListener);

    // install main component
    if (controller.installComponent() == null) {

      // installation failed
      String errorMessage = controller.getInstallationMsg();

      // stop controller messaging service
      controller.terminate();

      if (errorMessage != null) {
        throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
                "error_installing_main_component", new Object[] { componentId, errorMessage });
      } else {
        throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
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

      throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "error_installing_main_component", new Object[] { componentId, ex.getMessage() }, ex);
    }

    // create package browser object with the installed pear
    PackageBrowser pkgBrowser = null;

    try {
      // initialze package browser object
      pkgBrowser = new PackageBrowser(new File(installDir, componentId));

      // check if package browser could be initialized
      if (pkgBrowser == null) {
        
        // stop controller messaging service
        controller.terminate();

        throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
                "error_reading_installed_pear_settings", new Object[] { componentId });
      }
    } catch (IOException ex) {
      // stop controller messaging service
      controller.terminate();

      throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "error_reading_installed_pear_settings", new Object[] { componentId }, ex);
    }

    // installation verification
    if (verify) {
      if (!controller.verifyComponent()) {

        // verification failed
        String errorMessage = controller.getVerificationMsg();
        
        // stop controller messaging service
        controller.terminate();

        if (errorMessage != null) {
          throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
                  "error_verify_installation", new Object[] { componentId, errorMessage });
        } else {
          throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
                  "error_verify_installation_unknown", new Object[] { componentId });
        }
      } else {
        UIMAFramework.getLogger(PackageInstaller.class).logrb(Level.INFO, "PackageInstaller",
                "installPackage", PEAR_MESSAGE_RESOURCE_BUNDLE,
                "installation_verification_completed", componentId);
      }
    }

    // stop controller messaging service
    controller.terminate();

    return pkgBrowser;
  }
}
