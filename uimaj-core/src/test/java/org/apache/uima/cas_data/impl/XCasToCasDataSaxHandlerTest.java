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

package org.apache.uima.cas_data.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.cas_data.CasData;
import org.apache.uima.cas_data.FeatureStructure;
import org.apache.uima.cas_data.FeatureValue;
import org.apache.uima.cas_data.PrimitiveValue;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.Level;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Tests XCasToCasDataSaxHandler. Also Tests CasDataToXCas.
 * 
 */
public class XCasToCasDataSaxHandlerTest extends TestCase {

  /**
   * Constructor for XCasToCasDataSaxHandlerTest.
   * 
   * @param arg0
   */
  public XCasToCasDataSaxHandlerTest(String arg0) {
    super(arg0);
  }

  public void testParse() throws Exception {
    try {
      CasData casData = new CasDataImpl();
      XCasToCasDataSaxHandler handler = new XCasToCasDataSaxHandler(casData);

      SAXParserFactory fact = SAXParserFactory.newInstance();
      SAXParser parser = fact.newSAXParser();
      XMLReader xmlReader = parser.getXMLReader();
      xmlReader.setContentHandler(handler);
      xmlReader.parse(new InputSource(getClass().getResourceAsStream("xcastest.xml")));

      // System.out.println(casData);
      Iterator<FeatureStructure> fsIter = casData.getFeatureStructures();
      boolean foundCrawlUrl = false;
      while (fsIter.hasNext()) {
        FeatureStructure fs = fsIter.next();
        if ("Crawl_colon_URL".equals(fs.getType())) {
          // System.out.println("[" + fs.getFeatureValue("value") + "]");
          Assert
                  .assertEquals(
                          "http://www.nolimitmedia.com/index.php?act=group&gro=1&gron=Flash&PHPSESSID=5dcc31fb425c4a204b70d9eab92531a5",
                          fs.getFeatureValue("value").toString());
          foundCrawlUrl = true;
        }
      }
      assertTrue(foundCrawlUrl);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testConversions() throws Exception {
    try {
      // complex CAS obtained by deserialization
      File typeSystemFile = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
      TypeSystemDescription typeSystem = UIMAFramework.getXMLParser().parseTypeSystemDescription(
              new XMLInputSource(typeSystemFile));
      CAS cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
              new FsIndexDescription[0]);

      InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
      
      XCASDeserializer deser = new XCASDeserializer(cas.getTypeSystem());
      ContentHandler deserHandler = deser.getXCASHandler(cas);
      SAXParserFactory fact = SAXParserFactory.newInstance();
      SAXParser parser = fact.newSAXParser();
      XMLReader xmlReader = parser.getXMLReader();
      xmlReader.setContentHandler(deserHandler);
      xmlReader.parse(new InputSource(serCasStream));
      serCasStream.close();
      _testConversions(cas);

      // a CAS with multiple Sofas
      InputStream translatorAeStream = new FileInputStream(JUnitExtension
              .getFile("CpeSofaTest/TransAnnotator.xml"));
      AnalysisEngineDescription translatorAeDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(translatorAeStream, null));
      AnalysisEngine transAnnotator = UIMAFramework.produceAnalysisEngine(translatorAeDesc);
      CAS cas2 = transAnnotator.newCAS();
      CAS englishView = cas2.createView("EnglishDocument");
      englishView.setSofaDataString("this beer is good", "text/plain");
      transAnnotator.process(cas2);
      _testConversions(cas2);

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  private void _testConversions(CAS aCAS) throws IOException,
          ParserConfigurationException, SAXException, ResourceInitializationException,
          CASRuntimeException {
    // generate XCAS events and pipe them to XCasToCasDataSaxHandler
    CasData casData = new CasDataImpl();
    XCasToCasDataSaxHandler handler = new XCasToCasDataSaxHandler(casData);
    XCASSerializer xcasSer = new XCASSerializer(aCAS.getTypeSystem());
    xcasSer.serialize(aCAS, handler);

    Assert.assertNotNull(casData);
    assertValidCasData(casData, aCAS.getTypeSystem());
    // System.out.println(casData);

    // now generate XCAS from the CasData
    CasDataToXCas generator = new CasDataToXCas();

    StringWriter sw = new StringWriter();
    XMLSerializer xmlSer = new XMLSerializer(sw, false);
    generator.setContentHandler(xmlSer.getContentHandler());

    generator.generateXCas(casData);
    String xml = sw.getBuffer().toString();
    
    //workaround for XML serializatioj problem on Sun Java 1.4
    if (!builtInXmlSerializationSupportsCRs()) {
      xml = xml.replaceAll("&#10;", "&#13;&#10;");  
    }
    
    UIMAFramework.getLogger(XCasToCasDataSaxHandlerTest.class).log(Level.FINE, xml);

    // deserialize back into CAS for comparison
    // CASMgr tcasMgr = CASFactory.createCAS(aCAS.getTypeSystem());
    // tcasMgr.initCASIndexes();
    // tcasMgr.getIndexRepositoryMgr().commit();

    CAS cas2 = CasCreationUtils.createCas(null, aCAS.getTypeSystem(), null);
    XCASDeserializer deser = new XCASDeserializer(cas2.getTypeSystem());
    ContentHandler deserHandler = deser.getXCASHandler(cas2);

    SAXParserFactory fact = SAXParserFactory.newInstance();
    SAXParser parser = fact.newSAXParser();
    XMLReader xmlReader = parser.getXMLReader();
    xmlReader.setContentHandler(deserHandler);
    xmlReader.parse(new InputSource(new StringReader(xml)));

    // CASes should be identical
    CasComparer.assertEquals(aCAS, cas2);
  }

  /**
   * @param casData
   * @param system
   */
  private void assertValidCasData(CasData casData, TypeSystem typeSystem) {
    Type annotType = typeSystem.getType(CAS.TYPE_NAME_ANNOTATION);
    Type arrayType = typeSystem.getType(CAS.TYPE_NAME_ARRAY_BASE);
    Iterator<FeatureStructure> fsIter = casData.getFeatureStructures();
    while (fsIter.hasNext()) {
      org.apache.uima.cas_data.FeatureStructure fs = fsIter.next();
      String typeName = fs.getType();

      // don't do tests on the "fake" document text FS
      if (XCASSerializer.DEFAULT_DOC_TYPE_NAME.equals(typeName))
        continue;

      Type type = typeSystem.getType(typeName);
      Assert.assertNotNull(type);
      if (typeSystem.subsumes(annotType, type)) {
        // annotation type - check for presence of begin/end
        FeatureValue beginVal = fs.getFeatureValue("begin");
        Assert.assertTrue(beginVal instanceof PrimitiveValue);
        Assert.assertTrue(((PrimitiveValue) beginVal).toInt() >= 0);
        FeatureValue endVal = fs.getFeatureValue("end");
        Assert.assertTrue(endVal instanceof PrimitiveValue);
        Assert.assertTrue(((PrimitiveValue) endVal).toInt() >= 0);
      }
    }
  }
  
  /**
   * Checks the Java vendor and version and returns true if running a version
   * of Java whose built-in XSLT support can properly serialize carriage return
   * characters, and false if not.  It seems to be the case that Sun JVMs prior
   * to 1.5 do not properly serialize carriage return characters.  We have to
   * modify our test case to account for this.
   * @return true if XML serialization of CRs behave properly in the current JRE
   */
  private boolean builtInXmlSerializationSupportsCRs() {
    String javaVendor = System.getProperty("java.vendor");
    if( javaVendor.startsWith("Sun") ) {
        String javaVersion = System.getProperty("java.version");
        if( javaVersion.startsWith("1.3") || javaVersion.startsWith("1.4") )
            return false;
    }
    return true;
  }
}
