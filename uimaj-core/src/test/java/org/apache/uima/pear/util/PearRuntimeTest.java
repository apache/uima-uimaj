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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.analysis_engine.metadata.SofaMapping;
import org.apache.uima.cas.CAS;
import org.apache.uima.pear.tools.PackageBrowser;
import org.apache.uima.pear.tools.PackageInstaller;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.test.junit_extension.JUnitExtension;

/**
 * The PEAR runtime tests installs two pears files that both use JCas classes 
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

  public void testPearSofaMapping() throws Exception {
    AnalysisEngineDescription desc = createAeDescriptionFromPears(new String[]{"pearTests/pearSofaMap.pear"});
    Capability[] capabilities = new Capability[]{UIMAFramework.getResourceSpecifierFactory().createCapability()};
    capabilities[0].setInputSofas(new String[] {"_InitialView"});
    capabilities[0].setOutputSofas(new String[] {"GermanDocument"});
    desc.getAnalysisEngineMetaData().setCapabilities(capabilities);
    SofaMapping[] sofaMappings = new SofaMapping[] 
      {UIMAFramework.getResourceSpecifierFactory().createSofaMapping(),
       UIMAFramework.getResourceSpecifierFactory().createSofaMapping()};
//    <componentKey>ProjectForPear_pear</componentKey>
//    <componentSofaName>EnglishDocument</componentSofaName>
//    <aggregateSofaName>_InitialView</aggregateSofaName>
  
    sofaMappings[0].setComponentKey("Pear0");
    sofaMappings[1].setComponentKey("Pear0");
    sofaMappings[0].setComponentSofaName("EnglishDocument");
    sofaMappings[1].setComponentSofaName("GermanDocument");
    sofaMappings[0].setAggregateSofaName("_InitialView");
    sofaMappings[1].setAggregateSofaName("GermanDocument");
    desc.setSofaMappings(sofaMappings);
    CAS cas = runDesc(desc);
  }

  public void testPearRuntime() throws Exception {

    CAS cas = this.runPearRuntimeTestcase(new String[]{"pearTests/DateTime.pear", "pearTests/RoomNumber.pear"});
    
    //check if 3 annotations are available in the CAS index
    // The 3 annotations are the doc annotation, plus 2 room numbers
    //  The date-time annotators are skipped because the default result spec is "en"
    //    and is missing the "x-unspecified"
    Assert.assertEquals(cas.getAnnotationIndex().size(), 3);
//    FSIterator i = cas.getAnnotationIndex().iterator();
//    while (i.hasNext()) {
//      System.out.println(i.next());
//    }
   }

  

  public void testPearRuntimeDocAnnot() throws Exception {

    CAS cas = this.runPearRuntimeTestcase(new String[]{"pearTests/analysisEngine.pear", "pearTests/analysisEngine2.pear"});

    //check if 3 annotations are available in the CAS index
    Assert.assertEquals(cas.getAnnotationIndex().size(), 3);
   }

  private Import [] installPears(String [] pears) throws IOException {
    List<Import> result = new ArrayList<Import>();
    for (String s : pears) {
      result.add(installPear(s));
    }
    return result.toArray(new Import[result.size()]);
  }
  
  private Import installPear(String pear) throws IOException {
    // check temporary working directory
    if (this.tempInstallDir == null)
      throw new FileNotFoundException("temp directory not found");
    // check sample PEAR files

    // get pear files to install
    File pearFile = JUnitExtension.getFile(pear);
    Assert.assertNotNull(pearFile);

    // Install PEAR packages
    PackageBrowser instPear = PackageInstaller
            .installPackage(this.tempInstallDir, pearFile, true);

    // check pear PackageBrowser object
    Assert.assertNotNull(instPear); 
 
    // import pear specifiers
    Import impPear = UIMAFramework.getResourceSpecifierFactory().createImport();
    File import1 = new File(instPear.getComponentPearDescPath());
    impPear.setLocation(import1.toURI().getPath());

    return impPear;
  }
  
  private AnalysisEngineDescription createAeDescriptionFromPears(String[] pears) throws Exception {
    Import[] impPears = installPears(pears);

    // create aggregate analysis engine descriptor
    AnalysisEngineDescription desc = UIMAFramework.getResourceSpecifierFactory()
            .createAnalysisEngineDescription();
    desc.setPrimitive(false);

    // add delegates as imports
    Map<String, MetaDataObject> delegates = desc.getDelegateAnalysisEngineSpecifiersWithImports();
    String [] keys = new String[impPears.length];
    for (int i = 0; i < impPears.length; i++) {
      keys[i] = "Pear" + i;
      delegates.put(keys[i], impPears[i]);
    }

    // add sequence - fixed flow
    FixedFlow fixedFlow = UIMAFramework.getResourceSpecifierFactory().createFixedFlow();
    fixedFlow.setFixedFlow(keys);

    // add analysis engine meta data
    AnalysisEngineMetaData md = desc.getAnalysisEngineMetaData();
    md.setName("PEAR aggregate");
    md.setDescription("combines one or more PEARs");
    md.setVersion("1.0");
    md.setFlowConstraints(fixedFlow);

    // serialize descriptor  
//    String outputDir = JUnitExtension.getFile("pearTests/DateTime.pear").getParent(); File
//    outputFile = new File(outputDir, "PearAggregate.xml"); OutputStream outStream = new
//    BufferedOutputStream(new FileOutputStream(outputFile)); Writer writer = new
//    OutputStreamWriter(outStream, OutputFormat.Defaults.Encoding); XMLSerializer xmlSerializer =
//    new XMLSerializer(); xmlSerializer.setWriter(writer);
//    desc.toXML(xmlSerializer.getContentHandler(), true); writer.close();
    return desc;

  }
  
  private CAS runDesc(AnalysisEngineDescription desc) throws Exception {
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
  

  private CAS runPearRuntimeTestcase(String[] pears) throws Exception {
    
    AnalysisEngineDescription desc = createAeDescriptionFromPears(pears);    
    return runDesc(desc);
  }
}
