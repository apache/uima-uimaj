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
package org.apache.uima.test.junit_extension;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.SAXParser;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.Assert;

/**
 * AnnotatorTester is the helper class to test annotators.
 * 
 * @deprecated To be removed without replacement.
 * @forRemoval 4.0.0
 */
@Deprecated
public class AnnotatorTester {

  /** The desc file. */
  // annotator descriptor
  private File descFile;

  /** The ae. */
  // analysis engine instance
  private AnalysisEngine ae;

  /** The mgr. */
  // Resource Manager
  private ResourceManager mgr;

  /**
   * Constructor save the specified descriptor file path and initialize the analysis engine.
   * 
   * @param descFilePath
   *          descriptor file path
   * @throws Exception
   *           passthru if an analysis engine initialize error occurs.
   */
  public AnnotatorTester(String descFilePath) throws Exception {
    this.descFile = new File(descFilePath);
    this.mgr = UIMAFramework.newDefaultResourceManager();
    setup();
  }

  /**
   * Constructor save the specified descriptor file path and initialize the analysis engine.
   * 
   * @param descFile
   *          descriptor file
   * @throws Exception
   *           passthru if an analysis engine initialize error occurs.
   */
  public AnnotatorTester(File descFile) throws Exception {
    this.descFile = descFile;
    this.mgr = UIMAFramework.newDefaultResourceManager();
    setup();
  }

  /**
   * Constructor save the specified descriptor file path and initialize the analysis engine.
   * 
   * @param descFilePath
   *          descriptor file path
   * @param mgr
   *          a ResourceManager
   * @throws Exception
   *           if an analysis engine initialize error occurs.
   */
  public AnnotatorTester(String descFilePath, ResourceManager mgr) throws Exception {
    this.descFile = new File(descFilePath);
    this.mgr = mgr;
    setup();
  }

  /**
   * initialize the analysis engine with the specified specifier.
   * 
   * @throws Exception
   *           passthru
   */
  private void setup() throws Exception {
    this.ae = null;
    // Create an XML input source from the specifier file.
    XMLInputSource in = new XMLInputSource(this.descFile);
    // Parse the specifier.
    ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
    // Create the Text Analysis Engine.
    this.ae = UIMAFramework.produceAnalysisEngine(specifier, this.mgr, null);
  }

  /**
   * change the parameter name for the given analysis engine.
   *
   * @param groupName
   *          group name, if no group is available, pass null
   * @param paramName
   *          parameter name
   * @param paramValue
   *          parameter value
   * @throws ResourceConfigurationException
   *           passthru
   */
  public void changeParameterSetting(String groupName, String paramName, Object paramValue)
          throws ResourceConfigurationException {
    if (groupName == null) {
      this.ae.setConfigParameterValue(paramName, paramValue);
    } else {
      this.ae.setConfigParameterValue(groupName, paramName, paramValue);
    }
    // call reconfigure to activate the change
    this.ae.reconfigure();
  }

  /**
   * change the parameter name for the given delegate analysis engine key.
   *
   * @param delegeteKey
   *          analysis engine key
   * @param groupName
   *          group name
   * @param paramName
   *          parameter name
   * @param paramValue
   *          parameter value
   * @throws InvalidXMLException
   *           passthru
   * @throws ResourceInitializationException
   *           passthru
   * @throws IOException
   *           passthru
   */
  public void changeDelegateParameterSetting(String delegeteKey, String groupName, String paramName,
          Object paramValue)
          throws InvalidXMLException, ResourceInitializationException, IOException {
    // create CasConsumer description
    AnalysisEngineDescription aeSpecifier = UIMAFramework.getXMLParser()
            .parseAnalysisEngineDescription(new XMLInputSource(this.descFile));

    // get delegates
    Map delegates = aeSpecifier.getDelegateAnalysisEngineSpecifiers();

    // check if delegeteKey is available
    if (delegates.containsKey(delegeteKey)) {
      // create new import
      AnalysisEngineDescription delegate = (AnalysisEngineDescription) delegates.get(delegeteKey);

      if (groupName == null) {
        delegate.getMetaData().getConfigurationParameterSettings().setParameterValue(paramName,
                paramValue);
      } else {
        delegate.getMetaData().getConfigurationParameterSettings().setParameterValue(groupName,
                paramName, paramValue);
      }
    }

    // produce new AE
    this.ae = UIMAFramework.produceAnalysisEngine(aeSpecifier, this.mgr, null);

  }

  /**
   * does configuration parameter test.
   *
   * @param configDescFilePath
   *          the config desc file path
   * @return AnalysisEngine
   * @throws Exception
   *           passthru
   */
  public static AnalysisEngine doConfigurationTest(String configDescFilePath) throws Exception {
    AnalysisEngine ae = null;
    // Create an XML input source from the specifier file.
    XMLInputSource in = new XMLInputSource(configDescFilePath);
    // Parse the specifier.
    ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
    // Create the Text Analysis Engine.
    ae = UIMAFramework.produceAnalysisEngine(specifier, null, null);

    // Create a new CAS.
    CAS cas = ae.newCAS();
    // Set the document text on the CAS.
    cas.setDocumentText("This is a simple text to check if the configuration works");
    cas.setDocumentLanguage("en");
    // Process the sample document.
    ae.process(cas);

    return ae;
  }

  /**
   * Creates a new fresh CAS instance which can be used for testing.
   * 
   * @return a new fresh CAS instance which can be used for testing
   * @throws Exception
   *           passthru
   */
  public CAS createCAS() throws Exception {
    return ae.newCAS();
  }

  /**
   * performs a test on the initialized annotator. The specified document is processed with the
   * given language.
   * 
   * @param text
   *          a document text
   * @param language
   *          the document text language
   * @return CAS - results of the analysis
   * @throws Exception
   *           passthru
   */
  public CAS performTest(String text, String language) throws Exception {
    // Create a new CAS.
    CAS cas = this.ae.newCAS();
    // Set the document text on the CAS.
    cas.setDocumentText(text);
    cas.setDocumentLanguage(language);
    // Process the sample document.
    this.ae.process(cas);

    return cas;
  }

  /**
   * performs a test on the initialized annotator. The specified CAS is processed and the results
   * are returned.
   * 
   * @param cas
   *          a CAS for processing
   * @return CAS - results of the analysis
   * @throws Exception
   *           passthru
   */
  public CAS performTest(CAS cas) throws Exception {
    // Process the sample document.
    this.ae.process(cas);

    return cas;
  }

  /**
   * performs a test with a special annotator configuration. For this a new AE is created and used
   * to process the specified document for the specified language.
   * 
   * @param descFilePath
   *          Descriptor file path
   * @param text
   *          a document text
   * @param language
   *          the document text language
   * @return CAS - results of the analysis
   * @throws Exception
   *           passthru
   */
  public static CAS performTest(String descFilePath, String text, String language)
          throws Exception {
    AnalysisEngine ae = null;
    // Create an XML input source from the specifier file.
    XMLInputSource in = new XMLInputSource(descFilePath);
    // Parse the specifier.
    ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
    // Create the Text Analysis Engine.
    ae = UIMAFramework.produceAnalysisEngine(specifier, null, null);

    // Create a new CAS.
    CAS cas = ae.newCAS();
    // Set the document text on the CAS.
    cas.setDocumentText(text);
    cas.setDocumentLanguage(language);
    // Process the sample document.
    ae.process(cas);

    return cas;
  }

  /**
   * create a CAS object from the given XCAS and typesystem files.
   *
   * @param tsFile
   *          - a typesystem file
   * @param xcasFile
   *          - a xcas file
   * @return CAS - CAS object created from the given input data
   * @throws Exception
   *           passthru
   */
  public static CAS getCASfromXCAS(File tsFile, File xcasFile) throws Exception {
    Object tsDescriptor = UIMAFramework.getXMLParser().parse(new XMLInputSource(tsFile));
    TypeSystemDescription tsDesc = (TypeSystemDescription) tsDescriptor;
    CAS cas = CasCreationUtils.createCas(tsDesc, null, new FsIndexDescription[0]);

    SAXParser parser = XMLUtils.createSAXParserFactory().newSAXParser();
    XCASDeserializer xcasDeserializer = new XCASDeserializer(cas.getTypeSystem());
    parser.parse(xcasFile, xcasDeserializer.getXCASHandler(cas));

    return cas;
  }

  /**
   * Reads the content form a file to a String with respect to the file encoding.
   * 
   * @param file
   *          a file with the source
   * @param encoding
   *          file encoding
   * @return String - file content
   * @throws Exception
   *           passthru
   */
  public static String readFileContent(File file, String encoding) throws Exception {
    return FileUtils.file2String(file, encoding);
  }

  /**
   * checkResult compares the analyzed document with the reference output.
   * 
   * @param cas
   *          a cas with the analyzed data
   * @param AnnotationTypes
   *          respected annotation types
   * @param refFile
   *          reference output
   * @param testFile
   *          test file for the current output
   * @throws Exception
   *           passthru
   */
  public static void checkResult(CAS cas, String[] AnnotationTypes, File refFile, File testFile)
          throws Exception {

    testFile.delete(); // delete file if exist
    testFile.createNewFile(); // create new file

    // Create an XML input source from the specifier file.
    XMLInputSource in = new XMLInputSource(JUnitExtension.getURL("AnnotationWriter.xml"));
    // Parse the specifier.
    ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);

    CasConsumer consumer = UIMAFramework.produceCasConsumer(specifier);

    consumer.setConfigParameterValue("AnnotationTypes", AnnotationTypes);

    consumer.setConfigParameterValue("outputFile", testFile.getAbsolutePath());

    consumer.reconfigure();

    consumer.processCas(cas);
    consumer.destroy();

    // check file output
    boolean isIdentical = FileCompare.compare(refFile, testFile);

    if (isIdentical) {
      testFile.delete();
    }

    Assert.assertTrue(isIdentical);
  }
}
