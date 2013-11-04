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

package org.apache.uima.cas.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;

/**
 * Test case for XMI serialization, in particular, invalid XML 1.0 characters. Other aspects of XMI
 * serialization are tested elsewhere.
 */
public class XmiCasSerializerTest extends TestCase {
  
  private  static boolean XML1_1_SUPPORTED = false;
  
  static {
    try {
      XML1_1_SUPPORTED = SAXParserFactory.newInstance().getFeature("http://xml.org/sax/features/xml-1.1");
    } catch (SAXNotRecognizedException e) {
    } catch (SAXNotSupportedException e) {
    } catch (ParserConfigurationException e) {
    }
  }

  private TypeSystemDescription typeSystemDesc = null;

  private File outputFile = null;

  /**
   * @param arg0
   */
  public XmiCasSerializerTest(String arg0) {
    super(arg0);
  }

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    File typeSystemFile = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
    // Temp output file, deleted on exit.
    this.outputFile = new File(JUnitExtension.getFile("ExampleCas"),
        "xmiSerializerInvalidCharsTestOutput.xmi");
    this.typeSystemDesc = UIMAFramework.getXMLParser().parseTypeSystemDescription(
        new XMLInputSource(typeSystemFile));
  }

  public void testInvalidCharsInDocumentText() throws Exception {
    CAS cas = CasCreationUtils.createCas(this.typeSystemDesc, null, null);
    char badChar = 0x1A;
    cas.setDocumentText("Text with bad char: " + badChar);
    OutputStream out = new FileOutputStream(this.outputFile);
    XMLSerializer xmlSerializer = new XMLSerializer(out);
    XmiCasSerializer xmiCasSerializer = new XmiCasSerializer(cas.getTypeSystem());
    boolean caughtException = false;
    try {
      xmiCasSerializer.serialize(cas, xmlSerializer.getContentHandler());
    } catch (SAXParseException e) {
      caughtException = true;
    }
    out.close();
    assertTrue("XMI serialization of document text with bad XML 1.0 char should throw exception",
        caughtException);
    
    //but when XML 1.1 output is being generated, don't fail on control chracters which are valid in 1.1.
    if (XML1_1_SUPPORTED) {
      out = new FileOutputStream(this.outputFile);
      try {
        XMLSerializer xml11Serializer = new XMLSerializer(out);
        xml11Serializer.setOutputProperty(OutputKeys.VERSION,"1.1");
        xmiCasSerializer.serialize(cas, xml11Serializer.getContentHandler());
      }
      finally {
        out.close();
      }
    }
  }

  public void testInvalidCharsInFeatureValue() throws Exception {
    CAS cas = CasCreationUtils.createCas(this.typeSystemDesc, null, null);
    char badChar = 0x1A;
    cas.setDocumentLanguage("a" + badChar);
    OutputStream out = new FileOutputStream(this.outputFile);
    XMLSerializer xmlSerializer = new XMLSerializer(out);
    XmiCasSerializer xmiCasSerializer = new XmiCasSerializer(cas.getTypeSystem());
    boolean caughtException = false;
    try {
      xmiCasSerializer.serialize(cas, xmlSerializer.getContentHandler());
    } catch (SAXParseException e) {
      caughtException = true;
    }
    out.close();
    assertTrue("XMI serialization of document text with bad XML 1.0 char should throw exception",
        caughtException);
    
    //but when XML 1.1 output is being generated, don't fail on control characters which are valid in 1.1.
    if (XML1_1_SUPPORTED) {
      out = new FileOutputStream(this.outputFile);
      try {
        XMLSerializer xml11Serializer = new XMLSerializer(out);
        xml11Serializer.setOutputProperty(OutputKeys.VERSION,"1.1");
        xmiCasSerializer.serialize(cas, xml11Serializer.getContentHandler());
      }
      finally {
        out.close();
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    if ((this.outputFile != null) && this.outputFile.exists()) {
      this.outputFile.delete();
    }
  }

}
