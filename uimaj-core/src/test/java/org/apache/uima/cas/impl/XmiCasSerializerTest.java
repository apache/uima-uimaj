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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

/**
 * Test case for XMI serialization, in particular, invalid XML 1.0 characters. Other aspects of XMI
 * serialization are tested elsewhere.
 */
class XmiCasSerializerTest {

  private static boolean XML1_1_SUPPORTED = false;

  private TypeSystemDescription typeSystemDesc = null;

  private File outputFile = null;

  @BeforeAll
  static void setUpBeforeClass() throws Exception {
    XML1_1_SUPPORTED = SAXParserFactory.newInstance()
            .getFeature("http://xml.org/sax/features/xml-1.1");
  }

  @BeforeEach
  void setUp() throws Exception {
    File typeSystemFile = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
    // Temp output file, deleted on exit.
    outputFile = new File(JUnitExtension.getFile("ExampleCas"),
            "xmiSerializerInvalidCharsTestOutput.xmi");
    typeSystemDesc = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(typeSystemFile));
  }

  @Test
  void testInvalidCharsInDocumentText() throws Exception {
    CAS cas = CasCreationUtils.createCas(typeSystemDesc, null, null);
    char badChar = 0x1A;
    cas.setDocumentText("Text with bad char: " + badChar);
    OutputStream out = new FileOutputStream(outputFile);
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

    // but when XML 1.1 output is being generated, don't fail on control characters which are valid
    // in 1.1.
    if (XML1_1_SUPPORTED) {
      out = new FileOutputStream(outputFile);
      try {
        XMLSerializer xml11Serializer = new XMLSerializer(out);
        xml11Serializer.setOutputProperty(OutputKeys.VERSION, "1.1");
        xmiCasSerializer.serialize(cas, xml11Serializer.getContentHandler());
      } finally {
        out.close();
      }

      outputFile.delete();
      out = new FileOutputStream(outputFile);
      CasIOUtils.save(cas, out, SerialFormat.XMI_1_1);
    }
  }

  @Test
  void testInvalidCharsInFeatureValue() throws Exception {
    CAS cas = CasCreationUtils.createCas(typeSystemDesc, null, null);
    char badChar = 0x1A;
    cas.setDocumentLanguage("a" + badChar);
    OutputStream out = new FileOutputStream(outputFile);
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

    // but when XML 1.1 output is being generated, don't fail on control characters which are valid
    // in 1.1.
    if (XML1_1_SUPPORTED) {
      out = new FileOutputStream(outputFile);
      try {
        XMLSerializer xml11Serializer = new XMLSerializer(out);
        xml11Serializer.setOutputProperty(OutputKeys.VERSION, "1.1");
        xmiCasSerializer.serialize(cas, xml11Serializer.getContentHandler());
      } finally {
        out.close();
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#tearDown()
   */
  @AfterEach
  public void tearDown() throws Exception {
    if ((outputFile != null) && outputFile.exists()) {
      outputFile.delete();
    }
  }
}
