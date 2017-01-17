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

package org.apache.uima.analysis_engine.impl.sequencer;

import java.io.File;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.test.junit_extension.FileCompare;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;

/**
 * Testclass for Sequencers with a capabilityLanguageFlow
 * 
 */
public class SequencerCapabilityLanguageTest extends TestCase {

  private File testBaseDir = null;

  /**
   * Constructor for SequencerTest
   * 
   * @param arg0
   */
  public SequencerCapabilityLanguageTest(String arg0) {
    super(arg0);
  }

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    this.testBaseDir = JUnitExtension.getFile("SequencerTest");
  }

  public void runTest(String desc, String language, String refFile, boolean doResultSpec)
          throws Exception {
    AnalysisEngine ae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(this.testBaseDir, "SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(JUnitExtension.getFile(desc));
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      ae = UIMAFramework.produceAnalysisEngine(specifier, null, null);
      // Create a new CAS.
      CAS cas = ae.newCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage(language);
      // Process the sample document.
      if (doResultSpec) {
        ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
                .createResultSpecification();
        resultSpec.addCapabilities(ae.getAnalysisEngineMetaData().getCapabilities());
        ae.process(cas, resultSpec);
      } else {
        ae.process(cas);
      }
      // check fileoutput
      boolean compare = FileCompare.compare(outputReferenceFile, JUnitExtension.getFile(refFile));
      Assert.assertTrue(compare);
      if (compare) {
        outputReferenceFile.delete();
      }
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (ae != null) {
        ae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageEsEn() throws Exception {

    runTest("SequencerTest/SequencerCapabilityLanguageAggregateES.xml", "en",
            "SequencerTest/SequencerCapabilityLanguageExpectedEsEn.txt", false);

  }

  public void testSequencerCapabilityLanguageEsEnResultSpec() throws Exception {

    runTest("SequencerTest/SequencerCapabilityLanguageAggregateES.xml", "en",
            "SequencerTest/SequencerCapabilityLanguageExpectedEsEnResultSpec.txt", true);

  }

  public void testSequencerCapabilityLanguageEsEnUS() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregateES.xml", "en-US",
            "SequencerTest/SequencerCapabilityLanguageExpectedEsEnUS.txt", false);
  }

  public void testSequencerCapabilityLanguageEsEnUSResultSpec() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregateES.xml", "en-US",
            "SequencerTest/SequencerCapabilityLanguageExpectedEsEnResultSpec.txt", true);
  }

  public void testSequencerCapabilityLanguageEsAr() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregateES.xml", "ar",
            "SequencerTest/SequencerCapabilityLanguageExpectedEsAr.txt", false);
  }

  public void testSequencerCapabilityLanguageEsArResultSpec() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregateES.xml", "ar",
            "SequencerTest/SequencerCapabilityLanguageExpectedEsAr.txt", true);
  }

  public void testSequencerCapabilityLanguageEsUnkown() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregateES.xml", "unknown",
            "SequencerTest/SequencerCapabilityLanguageExpectedEsUnknown.txt", false);
  }

  public void testSequencerCapabilityLanguageEsUnkownResultSpec() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregateES.xml", "unknown",
            "SequencerTest/SequencerCapabilityLanguageExpectedEsUnknown.txt", true);
  }

  public void testSequencerCapabilityLanguageEsZhCN() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregateES.xml", "zh-CN",
            "SequencerTest/SequencerCapabilityLanguageExpectedEsZhCN.txt", false);
  }

  public void testSequencerCapabilityLanguageEsZhCNResultSpec() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregateES.xml", "zh-CN",
            "SequencerTest/SequencerCapabilityLanguageExpectedEsZhCNResultSpec.txt", true);
  }

  public void testSequencerCapabilityLanguageEsXunSpec() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregateES.xml", "x-unspecified",
            "SequencerTest/SequencerCapabilityLanguageExpectedEsUnknown.txt", false);
  }

  public void testSequencerCapabilityLanguageEsXunSpecResultSpec() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregateES.xml", "x-unspecified",
            "SequencerTest/SequencerCapabilityLanguageExpectedEsUnknown.txt", true);
  }

  public void testSequencerCapabilityLanguageEn() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregate.xml", "en",
            "SequencerTest/SequencerCapabilityLanguageExpectedEn.txt", false);
  }

  public void testSequencerCapabilityLanguageEnResultSpec() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregate.xml", "en",
            "SequencerTest/SequencerCapabilityLanguageExpectedEnResultSpec.txt", true);
  }

  public void testSequencerCapabilityLanguageEnResultSpecCapital() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregate.xml", "EN",
            "SequencerTest/SequencerCapabilityLanguageExpectedEnResultSpec.txt", true);
  }

  public void testSequencerCapabilityLanguageJa() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregate.xml", "ja",
            "SequencerTest/SequencerCapabilityLanguageExpectedJa.txt", false);
  }

  public void testSequencerCapabilityLanguageJaResultSpec() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregate.xml", "ja",
            "SequencerTest/SequencerCapabilityLanguageExpectedJaResultSpec.txt", true);
  }

  public void testSequencerCapabilityLanguageXunSpec() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregate.xml", "x-unspecified",
            "SequencerTest/SequencerCapabilityLanguageExpectedXunSpec.txt", false);
  }

  public void testSequencerCapabilityLanguageXunSpecResultSpec() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregate.xml", "x-unspecified",
            "SequencerTest/SequencerCapabilityLanguageExpectedXunSpec.txt", true);
  }

  public void testSequencerCapabilityLanguageEsFooBar() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregateES.xml", "foo-BAR",
            "SequencerTest/SequencerCapabilityLanguageExpectedEsUnknown.txt", false);
  }

  public void testSequencerCapabilityLanguageEsFooBarResultSpec() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregateES.xml", "foo-BAR",
            "SequencerTest/SequencerCapabilityLanguageExpectedEsUnknown.txt", true);
  }

  public void testSequencerCapabilityLanguageEsZhCNSmall() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregateES.xml", "zh-cn",
            "SequencerTest/SequencerCapabilityLanguageExpectedEsZhCN.txt", false);
  }

  public void testSequencerCapabilityLanguageEsZhCNResultSpecSmall() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregateES.xml", "zh-cn",
            "SequencerTest/SequencerCapabilityLanguageExpectedEsZhCNResultSpec.txt", true);
  }

  public void testSequencerCapabilityLanguageResultSpecSetByFlowController() throws Exception {
    runTest("SequencerTest/SequencerCapabilityLanguageAggregateResultSpec.xml", "en",
            "SequencerTest/SequencerCapabilityLanguageExpectedResultSpecSetByFlowController.txt", false);
  }
}
