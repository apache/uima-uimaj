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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.pear.tools.PackageBrowser;
import org.apache.uima.pear.tools.PackageInstaller;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The PearInstallerTest tests the PEAR installation and checks some parameters of the installed
 * PEAR file
 * 
 */
public class PearInstallerTest {

  // Temporary working directory, used to install the pear package
  private File tempInstallDir = null;

  /**
   * @see junit.framework.TestCase#setUp()
   */
  @BeforeEach
  public void setUp() throws Exception {

    // create temporary working directory
    File tempFile = File.createTempFile("pear_installer_test_", "tmp");
    if (tempFile.delete()) {
      File tempDir = tempFile;
      if (tempDir.mkdirs()) {
        this.tempInstallDir = tempDir;
      }
    }
  }

  /**
   * @see junit.framework.TestCase#tearDown()
   */
  @AfterEach
  public void tearDown() throws Exception {
    if (this.tempInstallDir != null) {
      FileUtil.deleteDirectory(this.tempInstallDir);
    }
  }

  @Test
  public void testPearInstall() throws Exception {

    // check temporary working directory
    if (this.tempInstallDir == null) {
      throw new FileNotFoundException("temp directory not found");
      // check sample PEAR files
    }

    // get pear file to install
    File pearFile = JUnitExtension.getFile("pearTests/DateTime.pear");
    assertThat(pearFile).isNotNull();

    // Install PEAR package
    PackageBrowser instPear = PackageInstaller.installPackage(this.tempInstallDir, pearFile, true);

    // check pear PackageBrowser object
    assertThat(instPear).isNotNull();

    // check PEAR component ID
    String componentID = instPear.getInstallationDescriptor().getMainComponentId();
    assertThat(componentID).isEqualTo("uima.example.DateTimeAnnotator");

    // check PEAR datapath setting
    // pear file contains (uima.datapath = $main_root/my/test/data/path)
    File datapath = new File(this.tempInstallDir,
            "uima.example.DateTimeAnnotator/my/test/data/path");
    File pearDatapath = new File(instPear.getComponentDataPath());
    assertThat(pearDatapath).isEqualTo(datapath);

    // Create resouce manager and set PEAR package classpath
    ResourceManager rsrcMgr = UIMAFramework.newDefaultResourceManager();

    // Create analysis engine from the installed PEAR package
    XMLInputSource in = new XMLInputSource(instPear.getComponentPearDescPath());
    ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
    AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(specifier, rsrcMgr, null);
    assertThat(ae).isNotNull();

    // Create a CAS with a sample document text and process the CAS
    CAS cas = ae.newCAS();
    cas.setDocumentText("Sample text to process with a date 05/29/07 and a time 9:45 AM");
    cas.setDocumentLanguage("en");
    ae.process(cas);

  }
}
