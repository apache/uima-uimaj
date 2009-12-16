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
 * The <code>PackageInstaller</code> class is the main user API for installing
 * PEAR packages. The class has a static method <code>installPackage</code> to
 * install PEAR packages that returns a <code>PackageBrowser</code> object
 * containing all the needed information about the installed PEAR package.
 * 
 * @see org.apache.uima.pear.tools.PackageBrowser
 * 
 */
public class PackageInstaller {

   private static final String PEAR_MESSAGE_RESOURCE_BUNDLE = "org.apache.uima.pear.pear_messages";

   /**
    * Installs the specified PEAR package to the specified install location.
    * After the installation is completed, an optional installation verification
    * step can be executed. This verification uses the main component descriptor
    * to instantiate the UIMA resource, encapsulated in the PEAR package, and
    * may run some additional tests, if applicable
    * 
    * @param installDir
    *           PEAR package install location
    * @param pearPackage
    *           PEAR package file location to install
    * @param verify
    *           If true the PEAR package verification is done after the
    *           installation
    * 
    * @return Returns a <code>PackageBrowser</code> object containing all PEAR
    *         package install settings
    * 
    * @throws PackageInstallerException
    *            If an error occurred during the pear installation or
    *            verification.
    */
   public static PackageBrowser installPackage(File installDir,
         File pearPackage, boolean verify) throws PackageInstallerException {
      return PackageInstaller.installPackage(installDir, pearPackage, verify,
            true);

   }

   /**
    * Installs the specified PEAR package to the specified install location.
    * After the installation is completed, an optional installation verification
    * step can be executed. This verification uses the main component descriptor
    * to instantiate the UIMA resource, encapsulated in the PEAR package, and
    * may run some additional tests, if applicable
    * 
    * @param installDir
    *           PEAR package install location
    * @param pearPackage
    *           PEAR package file location to install
    * @param verify
    *           If true the PEAR package verification is done after the
    *           installation
    * @param cleanInstallDir
    *           If <code>true</code>, the target installation directory will
    *           be cleaned before the PEAR file is installed.
    * @return Returns a <code>PackageBrowser</code> object containing all PEAR
    *         package install settings
    * 
    * @throws PackageInstallerException
    *            If an error occurred during the pear installation or
    *            verification.
    */
   public static PackageBrowser installPackage(File installDir,
         File pearPackage, boolean verify, boolean cleanInstallDir)
         throws PackageInstallerException {
     return installPackage(installDir, pearPackage, verify, cleanInstallDir, false);
   }

     /**
      * Installs the specified PEAR package to the specified install location.
      * After the installation is completed, an optional installation verification
      * step can be executed. This verification uses the main component descriptor
      * to instantiate the UIMA resource, encapsulated in the PEAR package, and
      * may run some additional tests, if applicable.
      * 
      * @param installDir
      *           PEAR package install location
      * @param pearPackage
      *           PEAR package file location to install
      * @param verify
      *           If true the PEAR package verification is done after the
      *           installation
      * @param cleanInstallDir
      *           If <code>true</code>, the target installation directory will
      *           be cleaned before the PEAR file is installed.
      * @param installToTopLevelDir
      *           If <code>true</code>, the PEAR is installed directly into the 
      *           <code>installDir</code>.  No intermediate directory with the component name is
      *           created.  Defaults to <code>false</code> in the overloads of this method.
      * @return Returns a <code>PackageBrowser</code> object containing all PEAR
      *         package install settings
      * 
      * @throws PackageInstallerException
      *            If an error occurred during the pear installation or
      *            verification.
      */
     public static PackageBrowser installPackage(File installDir,
           File pearPackage, boolean verify, boolean cleanInstallDir, boolean installToTopLevelDir)
           throws PackageInstallerException {

      // componentId for the given pear package
      String componentId;

      JarFile jarFile = null;

      // get componentId from the given pear package, componentId needed to
      // install the package
      // correctly
      try {
         // check if pear file exists
         if (!pearPackage.canRead()) {
            throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
                  "error_pear_package_path_invalid", new Object[] { pearPackage
                        .getAbsolutePath() });
         }

         // get installDescriptor from pear file
         jarFile = new JarFile(pearPackage);
         InstallationDescriptorHandler insdHandler = new InstallationDescriptorHandler();
         insdHandler.parseInstallationDescriptor(jarFile);
         InstallationDescriptor instObj = insdHandler
               .getInstallationDescriptor();
         if (instObj != null) {
            componentId = instObj.getMainComponentId();
         } else {
            throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
                  "error_parsing_pear_package_desc", new Object[] { pearPackage
                        .getAbsolutePath() });
         }
      } catch (IOException ex) {
         throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
               "error_parsing_pear_package_desc", new Object[] { pearPackage
                     .getAbsolutePath() }, ex);
      } catch (SAXException ex) {
         throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
               "error_parsing_pear_package_desc", new Object[] { pearPackage
                     .getAbsolutePath() }, ex);
      } finally {
         if (jarFile != null)
            try {
               jarFile.close();
            } catch (IOException ioe) {
               throw new PackageInstallerException(
                     PEAR_MESSAGE_RESOURCE_BUNDLE,
                     "Can't close open PEAR file : " + jarFile.getName());
            }
      }

      // create message listener for the pear installer
      MessageRouter.StdChannelListener msgListener = new MessageRouter.StdChannelListener() {
         public void errMsgPosted(String errMsg) {
            UIMAFramework.getLogger(PackageInstaller.class).logrb(Level.SEVERE,
                  "PackageInstaller", "installPackage",
                  PEAR_MESSAGE_RESOURCE_BUNDLE, "package_installer_error",
                  errMsg);
         }

         public void outMsgPosted(String outMsg) {
            UIMAFramework.getLogger(PackageInstaller.class).logrb(Level.INFO,
                  "PackageInstaller", "installPackage",
                  PEAR_MESSAGE_RESOURCE_BUNDLE, "package_installer_message",
                  outMsg);
         }
      };

      // create installation controller and to install the pear package
      InstallationController controller = new InstallationController(
            componentId, pearPackage, installDir, installToTopLevelDir, msgListener,
            cleanInstallDir);

      // install main component
      if (controller.installComponent() == null) {

         // installation failed
         String errorMessage = controller.getInstallationMsg();

         // stop controller messaging service
         controller.terminate();

         if (errorMessage != null) {
            throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
                  "error_installing_main_component", new Object[] {
                        componentId, errorMessage });
         } else {
            throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
                  "error_installing_main_component_unknown",
                  new Object[] { componentId });
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
               "error_installing_main_component", new Object[] { componentId,
                     ex.getMessage() }, ex);
      }

      // create package browser object with the installed pear
      PackageBrowser pkgBrowser = null;

      try {
        File pearDir = (installToTopLevelDir ? installDir : new File(installDir, componentId));
         // initialize package browser object
         pkgBrowser = new PackageBrowser(pearDir);

         // check if package browser could be initialized
         if (pkgBrowser == null) {

            // stop controller messaging service
            controller.terminate();

            throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
                  "error_reading_installed_pear_settings",
                  new Object[] { componentId });
         }
      } catch (IOException ex) {
         // stop controller messaging service
         controller.terminate();

         throw new PackageInstallerException(PEAR_MESSAGE_RESOURCE_BUNDLE,
               "error_reading_installed_pear_settings",
               new Object[] { componentId }, ex);
      }

      // installation verification
      if (verify) {
         if (!controller.verifyComponent()) {

            // verification failed
            String errorMessage = controller.getVerificationMsg();

            // stop controller messaging service
            controller.terminate();

            if (errorMessage != null) {
               throw new PackageInstallerException(
                     PEAR_MESSAGE_RESOURCE_BUNDLE, "error_verify_installation",
                     new Object[] { componentId, errorMessage });
            } else {
               throw new PackageInstallerException(
                     PEAR_MESSAGE_RESOURCE_BUNDLE,
                     "error_verify_installation_unknown",
                     new Object[] { componentId });
            }
         } else {
            UIMAFramework.getLogger(PackageInstaller.class).logrb(Level.INFO,
                  "PackageInstaller", "installPackage",
                  PEAR_MESSAGE_RESOURCE_BUNDLE,
                  "installation_verification_completed", componentId);
         }
      }

      // stop controller messaging service
      controller.terminate();

      return pkgBrowser;
   }
}
