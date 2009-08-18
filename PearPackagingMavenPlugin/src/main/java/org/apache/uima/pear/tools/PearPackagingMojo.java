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
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.uima.UIMAFramework;
import org.apache.uima.util.Level;

/**
 * PearPackagingMojo which generates an UIMA PEAR package. All the necessary
 * information from the UIMA nature is gathered and added to the PEAR package.
 * Additionally the generated jar file from the Maven build is added with the
 * according classpath information.
 * 
 * @goal package
 * 
 * @phase package
 */
public class PearPackagingMojo extends AbstractMojo {

   /**
    * Main component directory of the UIMA project that contains the UIMA
    * nature.
    * 
    * @parameter expression="${basedir}"
    * @required
    */
   private String mainComponentDir = null;

   /**
    * Required classpath settings for the PEAR package.
    * 
    * @parameter
    */
   private String classpath = null;

   /**
    * Main Component Descriptor path relative to the main component directory
    * 
    * @parameter
    * @required
    */
   private String mainComponentDesc = null;

   /**
    * PEAR package component ID
    * 
    * @parameter
    * @required
    */
   private String componentId = null;

   /**
    * Target directory for the PEAR package
    * 
    * @parameter expression="${basedir}/target"
    * @required
    */
   private String targetDir = null;

   /**
    * Required UIMA datapath settings for the PEAR package
    * 
    * @parameter default-value="$main_root/resources"
    */
   private String datapath = null;

   /**
    * Required environment variables for the PEAR package
    * 
    * @parameter
    */
   private Properties props = null;

   /**
    * The maven project.
    * 
    * @parameter expression="${project}"
    * @required
    * @readonly
    * @description "the maven project"
    */
   private MavenProject project;

   // the PEAR packaging directory contains all the stuff that is added to
   // the PEAR
   private File pearPackagingDir;

   /*
    * (non-Javadoc)
    * 
    * @see org.apache.maven.plugin.AbstractMojo#execute()
    */
   public void execute() throws MojoExecutionException {

      // create the PEAR packaging directory in the target directory
      this.pearPackagingDir = new File(this.targetDir, "pearPackaging");

      // save current UIMA log level
      Level level = getCurrentUIMALogLevel();
      // change UIMA log level to only log warnings and errors
      UIMAFramework.getLogger().setLevel(Level.WARNING);

      // create final PEAR file object
      File finalPearFileName = new File(this.targetDir, this.componentId
            + ".pear");

      // log PEAR packaging details
      Log log = getLog();
      log.info("Start building PEAR package for component " + this.componentId);
      log.debug("UIMA PEAR INFORMATION ");
      log.debug("======================");
      log.debug("main component dir:   " + this.mainComponentDir);
      log.debug("main component desc:  " + this.mainComponentDesc);
      log.debug("component id:         " + this.componentId);
      log.debug("classpath:            " + this.classpath);
      log.debug("datapath:             " + this.datapath);
      log.debug("target dir:           " + this.targetDir);
      log.debug("pear packaging dir:   "
            + this.pearPackagingDir.getAbsolutePath());
      log.debug("final PEAR file:      " + finalPearFileName.getAbsolutePath());

      // check Maven project packaging type - only jar packaging is supported
      if (!this.project.getPackaging().equals("jar")) {
         throw new MojoExecutionException(
               "Wrong packaging type, only 'jar' packaging is supported");
      }

      try {
         // copies all PEAR data to the PEAR packaging directory
         copyPearData();

         // copy created jar package to the PEAR packaging lib directory
         // get jar file name
         String jarFileName = this.project.getBuild().getFinalName() + ".jar";
         // get jar file location
         File finalJarFile = new File(this.project.getBuild().getDirectory(),
               jarFileName);
         // check if the jar file exist
         if (finalJarFile.exists()) {
            // specify the target directory for the jar file
            File target = new File(this.pearPackagingDir,
                  InstallationController.PACKAGE_LIB_DIR);
            File targetFile = new File(target, jarFileName);
            // copy the jar file to the target directory
            FileUtils.copyFile(finalJarFile, targetFile);
         } else {
            // jar file does not exist - build was not successful
            String errorMessage = "Jar package "
                  + finalJarFile.getAbsolutePath() + " not found";
            log.debug(errorMessage);
            throw new IOException(errorMessage);
         }

         // add compiled jar to the PEAR classpath
         StringBuffer buffer = new StringBuffer();
         buffer.append(";$main_root/");
         buffer.append(InstallationController.PACKAGE_LIB_DIR);
         buffer.append("/");
         buffer.append(this.project.getBuild().getFinalName());
         buffer.append(".jar");
         String classpathExtension = buffer.toString();
         if(this.classpath != null) {
            this.classpath = this.classpath + classpathExtension;
         } else {
            this.classpath = classpathExtension.substring(1,classpathExtension.length());
         }

         // create the PEAR package
         createPear();

         // log success message
         log.info("PEAR package for component " + this.componentId
               + " successfully created at: "
               + finalPearFileName.getAbsolutePath());

         // set UIMA logger back to the original log level
         UIMAFramework.getLogger().setLevel(level);

      } catch (PackageCreatorException e) {
         log.debug(e.getMessage());
         throw new MojoExecutionException(e.getMessage());
      } catch (IOException e) {
         log.debug(e.getMessage());
         throw new MojoExecutionException(e.getMessage());
      }

   }

   /**
    * Returns the current UIMA log level for the UIMA root logger
    * 
    * @return the current UIMA log level
    */
   private Level getCurrentUIMALogLevel() {
      if (UIMAFramework.getLogger().isLoggable(Level.ALL)) {
         return Level.ALL;
      } else if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
         return Level.FINEST;
      } else if (UIMAFramework.getLogger().isLoggable(Level.FINER)) {
         return Level.FINER;
      } else if (UIMAFramework.getLogger().isLoggable(Level.FINE)) {
         return Level.FINE;
      } else if (UIMAFramework.getLogger().isLoggable(Level.CONFIG)) {
         return Level.CONFIG;
      } else if (UIMAFramework.getLogger().isLoggable(Level.INFO)) {
         return Level.INFO;
      } else if (UIMAFramework.getLogger().isLoggable(Level.WARNING)) {
         return Level.WARNING;
      } else if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
         return Level.SEVERE;
      } else {
         return Level.OFF;
      }
   }

   /**
    * Copies the given directory when available to the PEAR packaging directory
    * 
    * @param directory
    *           directory to copy
    * 
    * @throws IOException
    */
   private void copyDirIfAvailable(String directory) throws IOException {

      // check if directory exist
      File dirToCopy = new File(this.mainComponentDir, directory);
      if (dirToCopy.exists()) {
         File target = new File(this.pearPackagingDir, directory);
         FileUtils.copyDirectory(dirToCopy, target);
         // remove directories that begins with a "." -> e.g. .SVN
         removeDotDirectories(target);
      }
   }

   /**
    * Removes recursively all directories that begins with a "." e.g. ".SVN"
    * 
    * @param dir
    *           directory to check for Dot-directories
    * 
    * @throws IOException
    */
   @SuppressWarnings("unchecked")
   private void removeDotDirectories(File dir) throws IOException {
      ArrayList<File> subdirs = org.apache.uima.util.FileUtils.getSubDirs(dir);

      for (int i = 0; i < subdirs.size(); i++) {
         File current = subdirs.get(i);
         if (current.getName().startsWith(".")) {
            org.apache.uima.util.FileUtils.deleteRecursive(current);
         } else {
            removeDotDirectories(current);
         }
      }
   }

   /**
    * Copies all the necessary PEAR directories (UIMA nature) to the PEAR
    * packaging directory
    * 
    * @throws IOException
    */
   private void copyPearData() throws IOException {

      // select all necessary PEAR package directories that have to be copied
      String[] dirsToCopy = new String[] {
            InstallationController.PACKAGE_CONF_DIR,
            InstallationController.PACKAGE_DATA_DIR,
            InstallationController.PACKAGE_DESC_DIR,
            InstallationController.PACKAGE_DOC_DIR,
            InstallationController.PACKAGE_LIB_DIR,
            InstallationController.PACKAGE_METADATA_DIR,
            InstallationController.PACKAGE_RESOURCES_DIR,
            /* InstallationController.PACKAGE_SOURCES_DIR , */InstallationController.PACKAGE_BIN_DIR, };

      // copies the selected directories if they exists
      for (int i = 0; i < dirsToCopy.length; i++) {
         copyDirIfAvailable(dirsToCopy[i]);
      }
   }

   /**
    * create a PEAR package with
    * 
    * @throws PackageCreatorException
    */
   private void createPear() throws PackageCreatorException {
      //generates the PEAR packages with the given information
      PackageCreator
            .generatePearPackage(this.componentId, this.mainComponentDesc,
                  this.classpath, this.datapath, this.pearPackagingDir
                        .getAbsolutePath(), this.targetDir, this.props);
   }
}
