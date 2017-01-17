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

package org.apache.uima.pear.util;

import java.io.File;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.pear.tools.InstallationController.TestStatus;
import org.apache.uima.pear.tools.InstallationTester;
import org.apache.uima.pear.tools.PackageBrowser;
import org.apache.uima.pear.tools.PackageInstaller;
import org.apache.uima.test.junit_extension.JUnitExtension;

/**
 * Test the pear installation verification
 * 
 */
public class TestPearInstallationVerification extends TestCase {

  // Temporary working directory, used to install the pear package
  private File tempInstallDir = null;

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    
    // create temporary working directory
    File tempFile = File.createTempFile("pear_verification_test_", "tmp");
    if (tempFile.delete()) {
      File tempDir = tempFile;
      if (tempDir.mkdirs())
        this.tempInstallDir = tempDir;
    }
  }

  /**
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    if (this.tempInstallDir != null) {
      FileUtil.deleteDirectory(this.tempInstallDir);
    }
  }

  
  public void testAePearVerification() throws Exception {
    
     //get pear file to install
    File pearFile = JUnitExtension.getFile("pearTests/analysisEngine.pear");
    Assert.assertNotNull("analysisEngine.pear file not found", pearFile);
    
    // Install PEAR package without verification
    PackageBrowser instPear = PackageInstaller.installPackage(
            this.tempInstallDir, pearFile, false);
    
    //check package browser
    Assert.assertNotNull("PackageBrowser is null", instPear);
       
    InstallationTester installTester = new InstallationTester(instPear);
    TestStatus status = installTester.doTest();
    
    Assert.assertEquals(status.getRetCode(), TestStatus.TEST_SUCCESSFUL);
  }

  public void testCcPearVerification() throws Exception {
    
    //get pear file to install
   File pearFile = JUnitExtension.getFile("pearTests/casConsumer.pear");
   Assert.assertNotNull("casConsumer.pear file not found", pearFile);
   
   // Install PEAR package without verification
   PackageBrowser instPear = PackageInstaller.installPackage(
           this.tempInstallDir, pearFile, false);
   
   //check package browser
   Assert.assertNotNull("PackageBrowser is null", instPear);
      
   InstallationTester installTester = new InstallationTester(instPear);
   TestStatus status = installTester.doTest();
   
   Assert.assertEquals(status.getRetCode(), TestStatus.TEST_SUCCESSFUL);
 }

  public void testTsPearVerification() throws Exception {
    
    //get pear file to install
   File pearFile = JUnitExtension.getFile("pearTests/typeSystem.pear");
   Assert.assertNotNull("typeSystem.pear file not found", pearFile);
   
   // Install PEAR package without verification
   PackageBrowser instPear = PackageInstaller.installPackage(
           this.tempInstallDir, pearFile, false);
   
   //check package browser
   Assert.assertNotNull("PackageBrowser is null", instPear);
      
   InstallationTester installTester = new InstallationTester(instPear);
   TestStatus status = installTester.doTest();
   
   Assert.assertEquals(status.getRetCode(), TestStatus.TEST_SUCCESSFUL);
 }

  //TODO: create testcases for ci, cr, cpe pear packages

}
