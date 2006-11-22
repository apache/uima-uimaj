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

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.collection.CasInitializer;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.pear.util.UIMAUtil;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.XMLInputSource;

/**
 * The <code>InstallationTester</code> application allows performing simple verification of the
 * installed UIMA compliant component by using standard UIMA framework interface.
 * 
 * @see org.apache.uima.pear.tools.InstallationController
 * @see org.apache.uima.pear.util.UIMAUtil
 */

public class InstallationTester {
  // attributes
  private boolean _passed = false;

  /**
   * Starts the application. The application requires standard UIMA classpath settings as well as
   * component specific environment settings.
   * 
   * @param args
   *          component_descriptor_file
   * @throws java.lang.Exception
   *           if any exception occurred.
   */
  public static void main(String[] args) throws Exception {
    if (System.getProperty("DEBUG") != null) {
      System.out.println("<DBG> [InstallationTester]: arg=" + args[0] + "; CP="
                      + System.getProperty("java.class.path"));
    }
    UIMAFramework.getLogger().setLevel(Level.OFF);
    try {
      if (args.length < 1)
        throw new IOException("Descriptor file not specified");
      InstallationTester tester = new InstallationTester(args[0]);
      if (tester._passed) {
        if (System.getProperty("DEBUG") != null)
          System.out.println("<DBG> [InstallationTester]: " + "test completed successfully");
        System.exit(0);
      } else {
        if (System.getProperty("DEBUG") != null)
          System.err.println("<DBG> [InstallationTester]: " + "test canceled");
        System.exit(1);
      }
    } catch (Throwable err) {
      err.printStackTrace(System.err);
      System.exit(-1);
    }
  }

  /**
   * Creates new instance of the <code>InstallationTester</code> class, identifies a specified
   * component using UIMA API, and invokes appropriate method to test the specified component.
   * 
   * @param compDescFilePath
   *          The given component descriptor file path.
   * @throws java.io.IOException
   *           if any I/O exception occurred.
   * @throws org.apache.uima.util.InvalidXMLException
   *           if component descriptor is invalid.
   * @throws org.apache.uima.resource.ResourceInitializationException
   *           if the specified component cannot be instantiated.
   * @throws org.apache.uima.UIMAException
   *           if this exception occurred while identifying UIMA component category.
   * @throws org.apache.uima.UIMARuntimeException
   *           if this exception occurred while identifying UIMA component category.
   */
  public InstallationTester(String compDescFilePath) throws IOException, InvalidXMLException,
                  ResourceInitializationException, UIMAException, UIMARuntimeException {
    // check UIMA category of the main component
    File compDescFile = new File(compDescFilePath);
    String uimaCategory = UIMAUtil.identifyUimaComponentCategory(compDescFile);
    if (uimaCategory == null) {
      Exception err = UIMAUtil.getLastErrorForXmlDesc(compDescFile);
      if (err != null) {
        if (err instanceof UIMAException)
          throw (UIMAException) err;
        else if (err instanceof UIMARuntimeException)
          throw (UIMARuntimeException) err;
        else
          throw new RuntimeException(err);
      }
    } else if (uimaCategory.equals(UIMAUtil.ANALYSIS_ENGINE_CTG))
      _passed = testAnalysisEngine(compDescFile);
    else if (uimaCategory.equals(UIMAUtil.CAS_CONSUMER_CTG))
      _passed = testCasConsumer(compDescFile);
    else if (uimaCategory.equals(UIMAUtil.CAS_INITIALIZER_CTG))
      _passed = testCasInitializer(compDescFile);
    else if (uimaCategory.equals(UIMAUtil.COLLECTION_READER_CTG))
      _passed = testCollectionReader(compDescFile);
    else if (uimaCategory.equals(UIMAUtil.CPE_CONFIGURATION_CTG))
      _passed = testCpeCongifuration(compDescFile);
    else if (uimaCategory.equals(UIMAUtil.TYPE_SYSTEM_CTG))
      _passed = testTypeSystem(compDescFile);
  }

  /**
   * Checks if a given AE specifier file can be used to produce an istance of AE. Returns
   * <code>true</code>, if an AE can be instantiated and a CAS object can be created,
   * <code>false</code> otherwise.
   * 
   * @param aeSpecifierFile
   *          The given AE specifier file.
   * @return <code>true</code>, if an AE can be instantiated and a CAS object can be created,
   *         <code>false</code> otherwise.
   * @throws IOException
   *           If an I/O exception occurred while creating <code>XMLInputSource</code>.
   * @throws InvalidXMLException
   *           If the XML parser failed to parse the given input file.
   * @throws ResourceInitializationException
   *           If the specified AE cannot be instantiated.
   */
  public static boolean testAnalysisEngine(File aeSpecifierFile) throws IOException,
                  InvalidXMLException, ResourceInitializationException {
    try {
      XMLInputSource xmlIn = new XMLInputSource(aeSpecifierFile);
      ResourceSpecifier aeSpecifier = UIMAFramework.getXMLParser().parseResourceSpecifier(xmlIn);
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(aeSpecifier);
      if (ae != null) {
        CAS cas = ae.newCAS();
        return cas != null;
      } else
        return false;
    } catch (IOException ioE) {
      System.err
                      .println("Error on creating XML source from descriptor file :: "
                                      + ioE.getMessage());
      throw ioE;
    } catch (InvalidXMLException inxE) {
      System.err.println("Error on parsing the XML source file :: " + inxE.getMessage());
      throw inxE;
    } catch (ResourceInitializationException riE) {
      System.err.println("Error on producing AE :: " + riE.getMessage());
      throw riE;
    }
  }

  /**
   * Checks if a given CC specifier file can be used to produce an instance of CC. Returns
   * <code>true</code>, if a CC can be instantiated, <code>false</code> otherwise.
   * 
   * @param ccSpecifierFile
   *          The given CC specifier file.
   * @return <code>true</code>, if a CC can be instantiated, <code>false</code> otherwise.
   * @throws IOException
   *           If an I/O exception occurred while creating <code>XMLInputSource</code>.
   * @throws InvalidXMLException
   *           If the XML parser failed to parse the given input file.
   * @throws ResourceInitializationException
   *           If the specified CC cannot be instantiated.
   */
  public static boolean testCasConsumer(File ccSpecifierFile) throws IOException,
                  InvalidXMLException, ResourceInitializationException {
    try {
      XMLInputSource xmlIn = new XMLInputSource(ccSpecifierFile);
      ResourceSpecifier ccSpecifier = UIMAFramework.getXMLParser().parseResourceSpecifier(xmlIn);
      CasConsumer cc = UIMAFramework.produceCasConsumer(ccSpecifier);
      return cc != null;
    } catch (IOException ioE) {
      System.err
                      .println("Error on creating XML source from descriptor file :: "
                                      + ioE.getMessage());
      throw ioE;
    } catch (InvalidXMLException inxE) {
      System.err.println("Error on parsing the XML source file :: " + inxE.getMessage());
      throw inxE;
    } catch (ResourceInitializationException riE) {
      System.err.println("Error on producing CC :: " + riE.getMessage());
      throw riE;
    }
  }

  /**
   * Checks if a given CI specifier file can be used to produce an instance of CI. Returns
   * <code>true</code>, if a CI can be instantiated, <code>false</code> otherwise.
   * 
   * @param ciSpecifierFile
   *          The given CI specifier file.
   * @return <code>true</code>, if a CI can be instantiated, <code>false</code> otherwise.
   * @throws IOException
   *           If an I/O exception occurred while creating <code>XMLInputSource</code>.
   * @throws InvalidXMLException
   *           If the XML parser failed to parse the given input file.
   * @throws ResourceInitializationException
   *           If the specified CI cannot be instantiated.
   */
  public static boolean testCasInitializer(File ciSpecifierFile) throws IOException,
                  InvalidXMLException, ResourceInitializationException {
    try {
      XMLInputSource xmlIn = new XMLInputSource(ciSpecifierFile);
      ResourceSpecifier ciSpecifier = UIMAFramework.getXMLParser().parseResourceSpecifier(xmlIn);
      CasInitializer ci = UIMAFramework.produceCasInitializer(ciSpecifier);
      return ci != null;
    } catch (IOException ioE) {
      System.err
                      .println("Error on creating XML source from descriptor file :: "
                                      + ioE.getMessage());
      throw ioE;
    } catch (InvalidXMLException inxE) {
      System.err.println("Error on parsing the XML source file :: " + inxE.getMessage());
      throw inxE;
    } catch (ResourceInitializationException riE) {
      System.err.println("Error on producing CC :: " + riE.getMessage());
      throw riE;
    }
  }

  /**
   * Checks if a given CR specifier file can be used to produce an instance of CR. Returns
   * <code>true</code>, if a CR can be instantiated, <code>false</code> otherwise.
   * 
   * @param crSpecifierFile
   *          The given CR specifier file.
   * @return <code>true</code>, if a CR can be instantiated, <code>false</code> otherwise.
   * @throws IOException
   *           If an I/O exception occurred while creating <code>XMLInputSource</code>.
   * @throws InvalidXMLException
   *           If the XML parser failed to parse the given input file.
   * @throws ResourceInitializationException
   *           If the specified CR cannot be instantiated.
   */
  public static boolean testCollectionReader(File crSpecifierFile) throws IOException,
                  InvalidXMLException, ResourceInitializationException {
    try {
      XMLInputSource xmlIn = new XMLInputSource(crSpecifierFile);
      ResourceSpecifier crSpecifier = UIMAFramework.getXMLParser().parseResourceSpecifier(xmlIn);
      CollectionReader cr = UIMAFramework.produceCollectionReader(crSpecifier);
      return cr != null;
    } catch (IOException ioE) {
      System.err
                      .println("Error on creating XML source from descriptor file :: "
                                      + ioE.getMessage());
      throw ioE;
    } catch (InvalidXMLException inxE) {
      System.err.println("Error on parsing the XML source file :: " + inxE.getMessage());
      throw inxE;
    } catch (ResourceInitializationException riE) {
      System.err.println("Error on producing CC :: " + riE.getMessage());
      throw riE;
    }
  }

  /**
   * Checks if a given CPE specifier file can be used to produce an instance of CPE. Returns
   * <code>true</code>, if a CPE can be instantiated, <code>false</code> otherwise.
   * 
   * @param cpeSpecifierFile
   *          The given CPE specifier file.
   * @return <code>true</code>, if a CPE can be instantiated, <code>false</code> otherwise.
   * @throws IOException
   *           If an I/O exception occurred while creating <code>XMLInputSource</code>.
   * @throws InvalidXMLException
   *           If the XML parser failed to parse the given input file.
   * @throws ResourceInitializationException
   *           If the specified CPE cannot be instantiated.
   */
  public static boolean testCpeCongifuration(File cpeSpecifierFile) throws IOException,
                  InvalidXMLException, ResourceInitializationException {
    try {
      XMLInputSource xmlIn = new XMLInputSource(cpeSpecifierFile);
      CpeDescription cpeDescription = UIMAFramework.getXMLParser().parseCpeDescription(xmlIn);
      CollectionProcessingEngine cpe = UIMAFramework
                      .produceCollectionProcessingEngine(cpeDescription);
      return cpe != null;
    } catch (IOException ioE) {
      System.err
                      .println("Error on creating XML source from descriptor file :: "
                                      + ioE.getMessage());
      throw ioE;
    } catch (InvalidXMLException inxE) {
      System.err.println("Error on parsing the XML source file :: " + inxE.getMessage());
      throw inxE;
    } catch (ResourceInitializationException riE) {
      System.err.println("Error on producing CC :: " + riE.getMessage());
      throw riE;
    }
  }

  /**
   * Checks if a given TS specifier file can be used to create an instance of CAS. Returns
   * <code>true</code>, if a CAS can be created for a given TS, <code>false</code> otherwise.
   * 
   * @param tsSpecifierFile
   *          The given TS specifier file.
   * @return <code>true</code>, if a CAS can be created for the given TS, <code>false</code>
   *         otherwise.
   * @throws IOException
   *           If an I/O exception occurred while creating <code>XMLInputSource</code>.
   * @throws InvalidXMLException
   *           If the XML parser failed to parse the given input file.
   * @throws ResourceInitializationException
   *           If the specified TS cannot be used to create a CAS.
   */
  public static boolean testTypeSystem(File tsSpecifierFile) throws IOException,
                  InvalidXMLException, ResourceInitializationException {
    try {
      XMLInputSource xmlIn = new XMLInputSource(tsSpecifierFile);
      TypeSystemDescription tsDescription = UIMAFramework.getXMLParser()
                      .parseTypeSystemDescription(xmlIn);
      TypePriorities tPriorities = UIMAFramework.getResourceSpecifierFactory()
                      .createTypePriorities();
      FsIndexDescription[] fsIndexes = new FsIndexDescription[0];
      CAS cas = CasCreationUtils.createCas(tsDescription, tPriorities, fsIndexes);
      return cas != null;
    } catch (IOException ioE) {
      System.err
                      .println("Error on creating XML source from descriptor file :: "
                                      + ioE.getMessage());
      throw ioE;
    } catch (InvalidXMLException inxE) {
      System.err.println("Error on parsing the XML source file :: " + inxE.getMessage());
      throw inxE;
    } catch (ResourceInitializationException riE) {
      System.err.println("Error on producing CC :: " + riE.getMessage());
      throw riE;
    }
  }
}
