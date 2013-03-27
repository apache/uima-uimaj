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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.uima.UIMAFramework;
import org.apache.uima.internal.util.I18nUtil;
import org.apache.uima.resource.RelativePathResolver;
import org.apache.uima.util.Level;

/**
 * Utility class to generate a pear package.
 * 
 */
public class PackageCreator {

  private static final String PEAR_MESSAGE_RESOURCE_BUNDLE = "org.apache.uima.pear.pear_messages";

  /**
   * Default method to generate a pear package. With the given information first the installation
   * descriptor is created and then the pear package is built.
   * 
   * @param componentID
   *          Specify a componentID for this component
   * 
   * @param mainComponentDesc
   *          Specify the main component descriptor path that is used to start the component. The
   *          path must be specified relative to the <code>mainComponentDir</code> parameter.
   * 
   * @param classpath
   *          Specify the classpath for this component. Use macros like <code>$main_root</code> to
   *          specify the classpath entries.
   * 
   * @param datapath
   *          Specify the datapath for this component. Use macros like <code>$main_root</code> to
   *          specify the datapath entities.
   * 
   * @param mainComponentDir
   *          Specify the main component root directory that contains all the resources for the
   *          component. The mainComponentDir is packaged to the pear file.
   * 
   * @param targetDir
   *          Specify the target directory where the generated pear file is written to.
   * 
   * @param envVars
   *          Specify additional environment variables for the pear component. May also use macros
   *          like <code>$main_root</code> to specify the key values if necessary.
   * 
   * @throws PackageCreatorException
   *           if an error occurs while create the pear package
   */
  public static void generatePearPackage(String componentID, String mainComponentDesc,
          String classpath, String datapath, String mainComponentDir, String targetDir,
          Properties envVars) throws PackageCreatorException {

    // create installation descriptor
    createInstallDescriptor(componentID, mainComponentDesc, classpath, datapath, mainComponentDir,
            envVars);

    // packagee pear file
    createPearPackage(componentID, mainComponentDir, targetDir);
  }

  /**
   * Creates the installation descriptor with the given information and save it in the metadata
   * folder of the <code>mainComponentDir</code>.
   * 
   * @param componentID
   *          Specify a componentID for this component
   * 
   * @param mainComponentDesc
   *          Specify the main component descriptor path that is used to start the component. The
   *          path must be specified relative to the <code>mainComponentDir</code> parameter.
   * 
   * @param classpath
   *          Specify the classpath for this component. Use macros like <code>$main_root</code> to
   *          specify the classpath entries.
   * 
   * @param datapath
   *          Specify the datapath for this component. Use macros like <code>$main_root</code> to
   *          specify the datapath entities.
   * 
   * @param mainComponentDir
   *          Specify the main component root directory that contains all the resources for the
   *          component. The mainComponentDir is packaged to the pear file.
   * 
   * @param envVars
   *          Specify additional environment variables for the pear component. May also use macros
   *          like <code>$main_root</code> to specify the key values if necessary.
   * 
   * @throws PackageCreatorException
   *           if an error occurs while creating the installation descriptor
   *           
   * @return Path to the created installation descriptor.
   */
  public static String createInstallDescriptor(String componentID, String mainComponentDesc,
          String classpath, String datapath, String mainComponentDir, Properties envVars)
          throws PackageCreatorException {

    //installation descriptor file path
    String installationDesc = null;
    
    // create new install descriptor
    InstallationDescriptor insd = new InstallationDescriptor();

    // set main component ID
    insd.setMainComponent(componentID);

    // set main component descriptor and add $main_root macro
    insd.setMainComponentDesc(addMacro(mainComponentDesc));

    // set Operation system where it was packaged
    insd.clearOSSpecs();
    insd.addOSSpec(InstallationDescriptorHandler.NAME_TAG, System.getProperty("os.name"));

    // set Java version where is was packaged
    insd.clearToolkitsSpecs();
    insd.addToolkitsSpec(InstallationDescriptorHandler.JDK_VERSION_TAG, System
            .getProperty("java.version"));

    // add classpath setting to the installation descriptor
    if (classpath != null) {
      // classpath setting should use ";" as separator
      if (classpath.indexOf(":") != -1) {
        // log warning that ";" as separator should be used.
        UIMAFramework.getLogger(PackageCreator.class).logrb(Level.WARNING, "PackageCreator",
                "createInstallDescriptor", PEAR_MESSAGE_RESOURCE_BUNDLE,
                "package_creator_classpath_not_valid_warning");
      }
      InstallationDescriptor.ActionInfo actionInfo = new InstallationDescriptor.ActionInfo(
              InstallationDescriptor.ActionInfo.SET_ENV_VARIABLE_ACT);
      actionInfo.params.put(InstallationDescriptorHandler.VAR_NAME_TAG,
              InstallationController.CLASSPATH_VAR);
      actionInfo.params.put(InstallationDescriptorHandler.VAR_VALUE_TAG, classpath);
      String commentMessage = I18nUtil.localizeMessage(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "package_creator_env_setting", new Object[] { InstallationController.CLASSPATH_VAR });
      actionInfo.params.put(InstallationDescriptorHandler.COMMENTS_TAG, commentMessage);
      insd.addInstallationAction(actionInfo);

    }

    // add datapath settings to the installation descriptor
    if (datapath != null) {
      // datapath setting should use ";" as separator
      if (datapath.indexOf(":") != -1) {
        // log warning that ";" as separator should be used.
        UIMAFramework.getLogger(PackageCreator.class).logrb(Level.WARNING, "PackageCreator",
                "createInstallDescriptor", PEAR_MESSAGE_RESOURCE_BUNDLE,
                "package_creator_datapath_not_valid_warning");
      }
      InstallationDescriptor.ActionInfo actionInfo = new InstallationDescriptor.ActionInfo(
              InstallationDescriptor.ActionInfo.SET_ENV_VARIABLE_ACT);
      actionInfo.params.put(InstallationDescriptorHandler.VAR_NAME_TAG,
              RelativePathResolver.UIMA_DATAPATH_PROP);
      actionInfo.params.put(InstallationDescriptorHandler.VAR_VALUE_TAG, datapath);
      String commentMessage = I18nUtil.localizeMessage(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "package_creator_env_setting",
              new Object[] { RelativePathResolver.UIMA_DATAPATH_PROP });
      actionInfo.params.put(InstallationDescriptorHandler.COMMENTS_TAG, commentMessage);
      insd.addInstallationAction(actionInfo);
    }

    // set additional environment variables
    if (envVars != null) {
      Enumeration<Object> keys = envVars.keys();
      while (keys.hasMoreElements()) {
        InstallationDescriptor.ActionInfo actionInfo = new InstallationDescriptor.ActionInfo(
                InstallationDescriptor.ActionInfo.SET_ENV_VARIABLE_ACT);
        String key = (String) keys.nextElement();
        actionInfo.params.put(InstallationDescriptorHandler.VAR_NAME_TAG, key);
        actionInfo.params
                .put(InstallationDescriptorHandler.VAR_VALUE_TAG, envVars.getProperty(key));
        String commentMessage = I18nUtil.localizeMessage(PEAR_MESSAGE_RESOURCE_BUNDLE,
                "package_creator_env_setting", new Object[] { key });
        actionInfo.params.put(InstallationDescriptorHandler.COMMENTS_TAG, commentMessage);
        insd.addInstallationAction(actionInfo);
      }
    }

    // save descriptor to metadata directory in the file system
    try {
      File metaDataDir = new File(mainComponentDir, InstallationController.PACKAGE_METADATA_DIR);
      if (!metaDataDir.exists()) {
        metaDataDir.mkdir();
      }
      File installDesc = new File(mainComponentDir, InstallationProcessor.INSD_FILE_PATH);
      InstallationDescriptorHandler.saveInstallationDescriptor(insd, installDesc);
      
      UIMAFramework.getLogger(PackageCreator.class).logrb(Level.INFO, "PackageCreator",
              "createInstallDescriptor", PEAR_MESSAGE_RESOURCE_BUNDLE,
              "package_creator_install_desc_created_info", new Object[] {installDesc});
      
      //set installation descriptor file path
      installationDesc = installDesc.getAbsolutePath();
      
    } catch (IOException ex) {
      throw new PackageCreatorException(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "error_package_creator_creating_pear_package", new Object[] { componentID }, ex);
    }   
    
    return installationDesc;
  }

  /**
   * create pear package based on the given information. This method can be called if a installation
   * descriptor is already available or generated. The method will not generate the installation
   * descriptor if it is not present.
   * 
   * @param componentID
   *          Specify a componentID for this component
   * 
   * @param mainComponentDir
   *          Specify the main component root directory that contains all the resources for the
   *          component. The mainComponentDir is packaged to the pear file.
   * 
   * @param targetDir
   *          Specify the target directory where the generated pear file is written to.
   * 
   * @throws PackageCreatorException
   *           if the pear package can not be created successfully.
   * 
   * @return Retuns path absolute path to the created pear package.
   */
  public static String createPearPackage(String componentID, String mainComponentDir, String targetDir)
          throws PackageCreatorException {
    // package pear file with all data from the mainComponentDir
    ZipOutputStream zipFile;
    File pearFile;
    
    try {
      pearFile = new File(targetDir, componentID + ".pear");
      zipFile = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(pearFile)));
      File mainDir = new File(mainComponentDir);
      zipDirectory(componentID, mainDir.getAbsolutePath(), mainDir, zipFile);
      zipFile.close();
      
      UIMAFramework.getLogger(PackageCreator.class).logrb(Level.INFO, "PackageCreator",
              "createInstallDescriptor", PEAR_MESSAGE_RESOURCE_BUNDLE,
              "package_creator_pear_created_info", new Object[] {pearFile});
    } catch (FileNotFoundException ex) {
      throw new PackageCreatorException(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "error_package_creator_creating_pear_package", new Object[] { componentID }, ex);
    } catch (IOException ex) {
      throw new PackageCreatorException(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "error_package_creator_creating_pear_package", new Object[] { componentID }, ex);
    }
    
    return pearFile.getAbsolutePath();
  }

  /**
   * adds the $main_root/ macro as a prefix to the given string
   * 
   * @param s
   *          a String representing a relative path to the project root
   * @return The String with the macro
   */
  private static String addMacro(String s) {
    String macro = "$main_root";
    if (s == null || s.length() == 0)
      return "";
    else {
      if (s.startsWith("/") || s.startsWith("\\"))
        s = macro + s;
      else
        s = macro + File.separator + s;
      return s;
    }
  }

  /**
   * returns a postfix path of the <code>currendPath</code> based on the <code>mainDir</code>
   * prefix.
   * 
   * @param mainDir
   *          Main directory - this prefix is cut from the <code>currentPath</code>
   * 
   * @param currentPath
   *          a Path element that has the <code>mainDir</code> as prefix.
   * 
   * @return returns the postfix of the <code>currendPath</code> based on the <code>mainDir</code>
   *         prefix.
   */
  private static String getRelativePath(String mainDir, File currentPath) {
    int prefixLength = mainDir.length();
    return currentPath.getAbsolutePath().substring(prefixLength + 1);
  }

  /**
   * adds recursivly all files and directories based on the source directory to the
   * <code>ZipOutputStream</code>.
   * 
   * @param componentID
   *          componentID of the pear package.
   * 
   * @param mainDir
   *          Main directory path that should be zipped.
   * 
   * @param sourceDir
   *          Source directory that should be zipped.
   * 
   * @param zipOut
   *          zip file output stream
   * 
   * @throws PackageCreatorException
   *           if an error occurs while adding the file and directories to the zip file.
   */
  private static void zipDirectory(String componentID, String mainDir, File sourceDir,
          ZipOutputStream zipOut) throws PackageCreatorException {
    if (!sourceDir.isDirectory()) {
      throw new PackageCreatorException(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "error_package_creator_invalid_directory",
              new Object[] { sourceDir.getAbsolutePath() });
    }

    try {
      // get all files and directories of the sourceDir
      String[] fileList = sourceDir.list();
      byte[] buffer = new byte[5120];

      File current = null;
      ZipEntry currentEntry = null;
      BufferedInputStream bufIn = null;

      // loop over all enties and zip them in case of files, for directories, recursivly call
      // zipDirectory()
      for (int i = 0; i < fileList.length; i++) {
        current = new File(sourceDir, fileList[i]);
        if (current.isDirectory()) {
          // if the current file is a directory, recursivly call zipDirectory()
          zipDirectory(componentID, mainDir, current, zipOut);
          continue;

        } else {
          // crate zip entry
          String path = getRelativePath(mainDir, current);
          path = path.replaceAll("\\\\", "/");
          currentEntry = new ZipEntry(path);
          zipOut.putNextEntry(currentEntry);

          // add file content to zip
          bufIn = new BufferedInputStream(new FileInputStream(current));
          int bytesRead = -1;
          while ((bytesRead = bufIn.read(buffer)) != -1) {
            zipOut.write(buffer, 0, bytesRead);
          }
          bufIn.close();
          zipOut.closeEntry();
        }
      }
    } catch (IOException ex) {
      throw new PackageCreatorException(PEAR_MESSAGE_RESOURCE_BUNDLE,
              "error_package_creator_creating_pear_package", new Object[] { componentID }, ex);
    }
  }
}
