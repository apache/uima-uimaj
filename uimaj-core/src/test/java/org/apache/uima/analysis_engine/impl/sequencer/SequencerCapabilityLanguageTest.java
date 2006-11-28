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

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.TextAnalysisEngine;
import org.apache.uima.cas.text.TCAS;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.test.junit_extension.FileCompare;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.test.junit_extension.TestPropertyReader;
import org.apache.uima.util.XMLInputSource;

/**
 * Testclass for Sequencers with a capabilityLanguageFlow
 * 
 * @author Michael Baessler
 */
public class SequencerCapabilityLanguageTest extends TestCase {
  private String junitTestBasePath;

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
    // get test base path setting
    junitTestBasePath = TestPropertyReader.getJUnitTestBasePath();
  }

  public void testSequencerCapabilityLanguageEsEn() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregateES.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("en");
      // Process the sample document.
      tae.process(cas);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(junitTestBasePath + "SequencerTest/SequencerTest.txt",
              junitTestBasePath + "SequencerTest/SequencerCapabilityLanguageExpectedEsEn.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageEsEnResultSpec() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregateES.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("en");
      // Process the sample document.
      ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();
      resultSpec.addCapabilities(tae.getAnalysisEngineMetaData().getCapabilities());
      tae.process(cas, resultSpec);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(junitTestBasePath + "SequencerTest/SequencerTest.txt",
              junitTestBasePath
                      + "SequencerTest/SequencerCapabilityLanguageExpectedEsEnResultSpec.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageEsEnUS() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregateES.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("en-US");
      // Process the sample document.
      tae.process(cas);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(junitTestBasePath + "SequencerTest/SequencerTest.txt",
              junitTestBasePath + "SequencerTest/SequencerCapabilityLanguageExpectedEsEn.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageEsEnUSResultSpec() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregateES.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("en-US");
      // Process the sample document.
      ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();
      resultSpec.addCapabilities(tae.getAnalysisEngineMetaData().getCapabilities());
      tae.process(cas, resultSpec);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(junitTestBasePath + "SequencerTest/SequencerTest.txt",
              junitTestBasePath + "SequencerTest/SequencerCapabilityLanguageExpectedEsEn.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageEsAr() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregateES.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("ar");
      // Process the sample document.
      tae.process(cas);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(junitTestBasePath + "SequencerTest/SequencerTest.txt",
              junitTestBasePath + "SequencerTest/SequencerCapabilityLanguageExpectedEsAr.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageEsArResultSpec() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregateES.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("ar");
      // Process the sample document.
      ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();
      resultSpec.addCapabilities(tae.getAnalysisEngineMetaData().getCapabilities());
      tae.process(cas, resultSpec);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(junitTestBasePath + "SequencerTest/SequencerTest.txt",
              junitTestBasePath + "SequencerTest/SequencerCapabilityLanguageExpectedEsAr.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageEsUnkown() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregateES.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("unknown");
      // Process the sample document.
      tae.process(cas);
      // check fileoutput
      Assert
              .assertTrue(FileCompare.compare(
                      junitTestBasePath + "SequencerTest/SequencerTest.txt", junitTestBasePath
                              + "SequencerTest/SequencerCapabilityLanguageExpectedEsUnknown.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageEsUnkownResultSpec() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregateES.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("unknown");
      // Process the sample document.
      ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();
      resultSpec.addCapabilities(tae.getAnalysisEngineMetaData().getCapabilities());
      tae.process(cas, resultSpec);
      // check fileoutput
      Assert
              .assertTrue(FileCompare.compare(
                      junitTestBasePath + "SequencerTest/SequencerTest.txt", junitTestBasePath
                              + "SequencerTest/SequencerCapabilityLanguageExpectedEsUnknown.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageEsZhCN() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregateES.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("zh-CN");
      // Process the sample document.
      tae.process(cas);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(junitTestBasePath + "SequencerTest/SequencerTest.txt",
              junitTestBasePath + "SequencerTest/SequencerCapabilityLanguageExpectedEsZhCN.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageEsZhCNResultSpec() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregateES.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("zh-CN");
      // Process the sample document.
      ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();
      resultSpec.addCapabilities(tae.getAnalysisEngineMetaData().getCapabilities());
      tae.process(cas, resultSpec);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(junitTestBasePath + "SequencerTest/SequencerTest.txt",
              junitTestBasePath
                      + "SequencerTest/SequencerCapabilityLanguageExpectedEsZhCNResultSpec.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageEsXunSpec() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregateES.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("x-unspecified");
      // Process the sample document.
      tae.process(cas);
      // check fileoutput
      Assert
              .assertTrue(FileCompare.compare(
                      junitTestBasePath + "SequencerTest/SequencerTest.txt", junitTestBasePath
                              + "SequencerTest/SequencerCapabilityLanguageExpectedEsUnknown.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageEsXunSpecResultSpec() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregateES.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("x-unspecified");
      // Process the sample document.
      ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();
      resultSpec.addCapabilities(tae.getAnalysisEngineMetaData().getCapabilities());
      tae.process(cas, resultSpec);
      // check fileoutput
      Assert
              .assertTrue(FileCompare.compare(
                      junitTestBasePath + "SequencerTest/SequencerTest.txt", junitTestBasePath
                              + "SequencerTest/SequencerCapabilityLanguageExpectedEsUnknown.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageEn() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregate.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("en");
      // Process the sample document.
      tae.process(cas);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(junitTestBasePath + "SequencerTest/SequencerTest.txt",
              junitTestBasePath + "SequencerTest/SequencerCapabilityLanguageExpectedEn.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageEnResultSpec() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregate.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("en");
      // Process the sample document.
      ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();
      resultSpec.addCapabilities(tae.getAnalysisEngineMetaData().getCapabilities());
      tae.process(cas, resultSpec);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(junitTestBasePath + "SequencerTest/SequencerTest.txt",
              junitTestBasePath
                      + "SequencerTest/SequencerCapabilityLanguageExpectedEnResultSpec.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageEnResultSpecCapital() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregate.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("EN");
      // Process the sample document.
      ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();
      resultSpec.addCapabilities(tae.getAnalysisEngineMetaData().getCapabilities());
      tae.process(cas, resultSpec);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(junitTestBasePath + "SequencerTest/SequencerTest.txt",
              junitTestBasePath
                      + "SequencerTest/SequencerCapabilityLanguageExpectedEnResultSpec.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageJa() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregate.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("ja");
      // Process the sample document.
      tae.process(cas);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(junitTestBasePath + "SequencerTest/SequencerTest.txt",
              junitTestBasePath + "SequencerTest/SequencerCapabilityLanguageExpectedJa.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageJaResultSpec() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregate.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("ja");
      // Process the sample document.
      ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();
      resultSpec.addCapabilities(tae.getAnalysisEngineMetaData().getCapabilities());
      tae.process(cas, resultSpec);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(junitTestBasePath + "SequencerTest/SequencerTest.txt",
              junitTestBasePath
                      + "SequencerTest/SequencerCapabilityLanguageExpectedJaResultSpec.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageXunSpec() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregate.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("x-unspecified");
      // Process the sample document.
      tae.process(cas);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(junitTestBasePath + "SequencerTest/SequencerTest.txt",
              junitTestBasePath + "SequencerTest/SequencerCapabilityLanguageExpectedXunSpec.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageXunSpecResultSpec() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregate.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("x-unspecified");
      // Process the sample document.
      ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();
      resultSpec.addCapabilities(tae.getAnalysisEngineMetaData().getCapabilities());
      tae.process(cas, resultSpec);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(junitTestBasePath + "SequencerTest/SequencerTest.txt",
              junitTestBasePath + "SequencerTest/SequencerCapabilityLanguageExpectedXunSpec.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageEsFooBar() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregateES.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("foo-BAR");
      // Process the sample document.
      tae.process(cas);
      // check fileoutput
      Assert
              .assertTrue(FileCompare.compare(
                      junitTestBasePath + "SequencerTest/SequencerTest.txt", junitTestBasePath
                              + "SequencerTest/SequencerCapabilityLanguageExpectedEsUnknown.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageEsFooBarResultSpec() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregateES.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("foo-BAR");
      // Process the sample document.
      ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();
      resultSpec.addCapabilities(tae.getAnalysisEngineMetaData().getCapabilities());
      tae.process(cas, resultSpec);
      // check fileoutput
      Assert
              .assertTrue(FileCompare.compare(
                      junitTestBasePath + "SequencerTest/SequencerTest.txt", junitTestBasePath
                              + "SequencerTest/SequencerCapabilityLanguageExpectedEsUnknown.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageEsZhCNSmall() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregateES.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("zh-cn");
      // Process the sample document.
      tae.process(cas);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(junitTestBasePath + "SequencerTest/SequencerTest.txt",
              junitTestBasePath + "SequencerTest/SequencerCapabilityLanguageExpectedEsZhCN.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

  public void testSequencerCapabilityLanguageEsZhCNResultSpecSmall() throws Exception {
    TextAnalysisEngine tae = null;
    try {
      // create TempFile for test
      File outputReferenceFile = new File(junitTestBasePath + "SequencerTest/SequencerTest.txt");
      outputReferenceFile.delete(); // delete file if exist
      outputReferenceFile.createNewFile(); // create new file
      outputReferenceFile.deleteOnExit(); // delete file after closing VM

      // Create an XML input source from the specifier file.
      XMLInputSource in = new XMLInputSource(junitTestBasePath
              + "SequencerTest/SequencerCapabilityLanguageAggregateES.xml");
      // Parse the specifier.
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
      // Create the Text Analysis Engine.
      tae = UIMAFramework.produceTAE(specifier, null, null);
      // Create a new TCAS.
      TCAS cas = tae.newTCAS();
      // Our sample text.
      String text = "Hello world!";
      // System.out.println("Processing text: \"" + text + "\"");
      // Set the document text on the CAS.
      cas.setDocumentText(text);
      cas.setDocumentLanguage("zh-cn");
      // Process the sample document.
      ResultSpecification resultSpec = UIMAFramework.getResourceSpecifierFactory()
              .createResultSpecification();
      resultSpec.addCapabilities(tae.getAnalysisEngineMetaData().getCapabilities());
      tae.process(cas, resultSpec);
      // check fileoutput
      Assert.assertTrue(FileCompare.compare(junitTestBasePath + "SequencerTest/SequencerTest.txt",
              junitTestBasePath
                      + "SequencerTest/SequencerCapabilityLanguageExpectedEsZhCNResultSpec.txt"));
      outputReferenceFile.delete();
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    } finally {
      // Destroy the CAS, releasing resources.
      if (tae != null) {
        tae.destroy();
      }
    }
  }

}
