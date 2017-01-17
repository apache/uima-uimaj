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

/*
 */

/**
 * TestCase for the Sequencer with a fixedFlow
 */
public class SequencerFixedTest extends TestCase {
  private File testBaseDir;

  /**
   * Constructor for SequencerTest
   * 
   * @param arg0
   */
  public SequencerFixedTest(String arg0) {
    super(arg0);
  }

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    // get test base path
    this.testBaseDir = JUnitExtension.getFile("SequencerTest");
  }

  public void testSequencerFixedEn() throws Exception {
    AnalysisEngine ae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(this.testBaseDir, "SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(JUnitExtension
              .getFile("SequencerTest/SequencerFixedAggregate.xml"));
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
      cas.setDocumentLanguage("en");
      // Process the sample document.
      ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();
      resultSpec.addCapabilities(ae.getAnalysisEngineMetaData().getCapabilities());
      ae.process(cas, resultSpec);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(outputReferenceFile, JUnitExtension
              .getFile("SequencerTest/SequencerFixedExpected.txt")));
      outputReferenceFile.delete();

    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (ae != null) {
        ae.destroy();
      }
    }
  }

  public void testSequencerFixedEN() throws Exception {
    AnalysisEngine ae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(this.testBaseDir, "SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(JUnitExtension
              .getFile("SequencerTest/SequencerFixedAggregate.xml"));
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
      cas.setDocumentLanguage("EN");
      // Process the sample document.
      ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();
      resultSpec.addCapabilities(ae.getAnalysisEngineMetaData().getCapabilities());
      ae.process(cas, resultSpec);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(outputReferenceFile, JUnitExtension
              .getFile("SequencerTest/SequencerFixedExpected.txt")));
      outputReferenceFile.delete();

    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (ae != null) {
        ae.destroy();
      }
    }
  }

  public void testSequencerFixedEnUS() throws Exception {
    AnalysisEngine ae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(this.testBaseDir, "SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(JUnitExtension
              .getFile("SequencerTest/SequencerFixedAggregate.xml"));
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
      cas.setDocumentLanguage("en-US");
      // Process the sample document.
      ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();
      resultSpec.addCapabilities(ae.getAnalysisEngineMetaData().getCapabilities());
      ae.process(cas, resultSpec);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(outputReferenceFile, JUnitExtension
              .getFile("SequencerTest/SequencerFixedExpected.txt")));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (ae != null) {
        ae.destroy();
      }
    }
  }

  public void testSequencerFixedEnus() throws Exception {
    AnalysisEngine ae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(this.testBaseDir, "SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(JUnitExtension
              .getFile("SequencerTest/SequencerFixedAggregate.xml"));
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
      cas.setDocumentLanguage("en-us");
      // Process the sample document.
      ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();
      resultSpec.addCapabilities(ae.getAnalysisEngineMetaData().getCapabilities());
      ae.process(cas, resultSpec);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(outputReferenceFile, JUnitExtension
              .getFile("SequencerTest/SequencerFixedExpected.txt")));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (ae != null) {
        ae.destroy();
      }
    }
  }

  public void testSequencerFixedUnkown() throws Exception {
    AnalysisEngine ae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(this.testBaseDir, "SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(JUnitExtension
              .getFile("SequencerTest/SequencerFixedAggregate.xml"));
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
      cas.setDocumentLanguage("unkown");
      // Process the sample document.
      ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();
      resultSpec.addCapabilities(ae.getAnalysisEngineMetaData().getCapabilities());
      ae.process(cas, resultSpec);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(outputReferenceFile, JUnitExtension
              .getFile("SequencerTest/SequencerFixedExpected.txt")));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (ae != null) {
        ae.destroy();
      }
    }
  }

  public void testSequencerFixedFooBar() throws Exception {
    AnalysisEngine ae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(this.testBaseDir, "SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(JUnitExtension
              .getFile("SequencerTest/SequencerFixedAggregate.xml"));
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
      cas.setDocumentLanguage("foo-BAR");
      // Process the sample document.
      ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();
      resultSpec.addCapabilities(ae.getAnalysisEngineMetaData().getCapabilities());
      ae.process(cas, resultSpec);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(outputReferenceFile, JUnitExtension
              .getFile("SequencerTest/SequencerFixedExpected.txt")));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (ae != null) {
        ae.destroy();
      }
    }
  }

  public void testSequencerFixedXunSpec() throws Exception {
    AnalysisEngine ae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(this.testBaseDir, "SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(JUnitExtension
              .getFile("SequencerTest/SequencerFixedAggregate.xml"));
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
      cas.setDocumentLanguage("x-unspecified");
      // Process the sample document.
      ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();
      resultSpec.addCapabilities(ae.getAnalysisEngineMetaData().getCapabilities());
      ae.process(cas, resultSpec);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(outputReferenceFile, JUnitExtension
              .getFile("SequencerTest/SequencerFixedExpected.txt")));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (ae != null) {
        ae.destroy();
      }
    }
  }

}
