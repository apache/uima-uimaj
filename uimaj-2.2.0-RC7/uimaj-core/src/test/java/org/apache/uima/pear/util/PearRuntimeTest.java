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
import java.io.FileNotFoundException;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.cas.CAS;
import org.apache.uima.pear.tools.PackageBrowser;
import org.apache.uima.pear.tools.PackageInstaller;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.test.junit_extension.JUnitExtension;

/**
 * The PEAR runtime tests installes two pears files that both use JCas classes 
 * in their processing.
 */
public class PearRuntimeTest extends TestCase {

  // Temporary working directory, used to install the pear package
  private File tempInstallDir = null;

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {

    // create temporary working directory
    File tempFile = File.createTempFile("pear_runtime_test_", "tmp");
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

  /**
   * @throws Exception
   */
  public void testPearRuntime() throws Exception {

    CAS cas = this.runPearRuntimeTestcase("pearTests/DateTime.pear", "pearTests/RoomNumber.pear");
    
    //check if 5 annotations are available in the CAS index
    Assert.assertEquals(cas.getAnnotationIndex().size(), 5);
   }

  /**
   * @throws Exception
   */

  public void testPearRuntimeDocAnnot() throws Exception {

    CAS cas = this.runPearRuntimeTestcase("pearTests/analysisEngine.pear", "pearTests/analysisEngine2.pear");

    //check if 3 annotations are available in the CAS index
    Assert.assertEquals(cas.getAnnotationIndex().size(), 3);
   }

  
  /**
   * @throws Exception
   */
  private CAS runPearRuntimeTestcase(String pear1, String pear2) throws Exception {

    // check temporary working directory
    if (this.tempInstallDir == null)
      throw new FileNotFoundException("temp directory not found");
    // check sample PEAR files

    // get pear files to install
    File pearFile1 = JUnitExtension.getFile(pear1);
    Assert.assertNotNull(pearFile1);

    File pearFile2 = JUnitExtension.getFile(pear2);
    Assert.assertNotNull(pearFile2);

    // Install PEAR packages
    PackageBrowser instPear1 = PackageInstaller
            .installPackage(this.tempInstallDir, pearFile1, true);

    // check pear PackageBrowser object
    Assert.assertNotNull(instPear1);

    PackageBrowser instPear2 = PackageInstaller
            .installPackage(this.tempInstallDir, pearFile2, true);

    // check pear PackageBrowser object
    Assert.assertNotNull(instPear2);

    // create aggregate analysis engine descriptor
    AnalysisEngineDescription desc = UIMAFramework.getResourceSpecifierFactory()
            .createAnalysisEngineDescription();
    desc.setPrimitive(false);

    // import pear specifiers
    Import impPear1 = UIMAFramework.getResourceSpecifierFactory().createImport();
    File import1 = new File(instPear1.getComponentPearDescPath());
    impPear1.setLocation(import1.toURI().getPath());

    // import main pear descriptor
    Import impPear2 = UIMAFramework.getResourceSpecifierFactory().createImport();
    File import2 = new File(instPear1.getComponentPearDescPath());
    impPear2.setLocation(import2.toURI().getPath());

    // add delegates as imports
    Map delegates = desc.getDelegateAnalysisEngineSpecifiersWithImports();
    delegates.put("Pear1", impPear1);
    delegates.put("Pear2", impPear2);

    // add sequence - fixed flow
    FixedFlow fixedFlow = UIMAFramework.getResourceSpecifierFactory().createFixedFlow();
    fixedFlow.setFixedFlow(new String[] { "Pear1", "Pear2" });

    // add analysis engine meta data
    AnalysisEngineMetaData md = desc.getAnalysisEngineMetaData();
    md.setName("PEAR aggregate");
    md.setDescription("combines tow PEARs");
    md.setVersion("1.0");
    md.setFlowConstraints(fixedFlow);

    // serialize descriptor  
//    String outputDir = JUnitExtension.getFile("pearTests/DateTime.pear").getParent(); File
//    outputFile = new File(outputDir, "PearAggregate.xml"); OutputStream outStream = new
//    BufferedOutputStream(new FileOutputStream(outputFile)); Writer writer = new
//    OutputStreamWriter(outStream, OutputFormat.Defaults.Encoding); XMLSerializer xmlSerializer =
//    new XMLSerializer(); xmlSerializer.setWriter(writer);
//    desc.toXML(xmlSerializer.getContentHandler(), true); writer.close();
    

    // Create analysis engine from aggregate ae description
    AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(desc, null, null);
    Assert.assertNotNull(ae);

    // Create a CAS with a sample document text and process the CAS
    CAS cas = ae.newCAS();
    cas.setDocumentText("Sample text to process with a date 05/29/07 and a time 9:45 AM and a Room number GN-K35 or two GN-K37");
    cas.setDocumentLanguage("en");
    ae.process(cas);
    
    return cas;
  }
}
