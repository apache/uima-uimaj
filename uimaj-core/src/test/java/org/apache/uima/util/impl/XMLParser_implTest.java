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

package org.apache.uima.util.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.resource.CustomResourceSpecifier;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.PearSpecifier;
import org.apache.uima.resource.URISpecifier;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.XMLReader;

public class XMLParser_implTest {

  private XMLParser mXmlParser;

  @BeforeEach
  public void setUp() throws Exception {
    mXmlParser = UIMAFramework.getXMLParser();

    // Enable schema validation. Note that this will enable schema validation
    // for tests that run after this too, but that's not so bad since we'd like
    // to test the schema. This is currently the first test in CoreTests, so
    // schema validation will be enabled for the whole suite.
    mXmlParser.enableSchemaValidation(true);
  }

  /*
   * @see TestCase#tearDown()
   */
  @AfterEach
  public void tearDown() throws Exception {
    UIMAFramework.getXMLParser().enableSchemaValidation(false);
  }

  @Test
  public void testParse() throws Exception {
    try {
      // JTalentAndStringMatch.xml contains imports,
      // JTalentAndStringMatch_Expanded.xml has had them manually expanded
      File withImports = JUnitExtension.getFile("XmlParserTest/JTalentAndStringMatch.xml");
      File manuallyExpanded = JUnitExtension
              .getFile("XmlParserTest/JTalentAndStringMatch_Expanded.xml");

      // After parsing both files and calling resolveImports,
      // we should then be able to parse both files and get identical results.
      AnalysisEngineDescription desc1 = (AnalysisEngineDescription) mXmlParser
              .parse(new XMLInputSource(withImports));
      AnalysisEngineDescription desc2 = (AnalysisEngineDescription) mXmlParser
              .parse(new XMLInputSource(manuallyExpanded));
      assertThat(desc1).isNotNull();
      assertThat(desc2).isNotNull();
      assertThat(desc2.getDelegateAnalysisEngineSpecifiers())
              .isEqualTo(desc1.getDelegateAnalysisEngineSpecifiers());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    } finally {
      mXmlParser = null;
    }
  }

  @Test
  public void testParseXMLInputSourceParseOptions() throws Exception {
    try {
      // test for env var refs
      File envVarRefTest = JUnitExtension.getFile("XmlParserTest/EnvVarRefTest.xml");
      System.setProperty("uima.test.var1", "foo");
      System.setProperty("uima.test.var2", "bar");
      AnalysisEngineDescription taeDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(envVarRefTest),
                      new XMLParser.ParsingOptions(true, true));
      assertThat(taeDesc.getMetaData().getName()).isEqualTo("foo-bar");

      // parse with env var ref expansion disabled
      taeDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
              new XMLInputSource(envVarRefTest), new XMLParser.ParsingOptions(false));
      assertThat(taeDesc.getMetaData().getName()).isEqualTo(
              "<envVarRef>uima.test.var1</envVarRef>-<envVarRef>uima.test.var2</envVarRef>");

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    } finally {
      mXmlParser = null;
    }
  }

  @Test
  public void testParseResourceSpecifier() throws Exception {
    try {
      // can't run this test under Sun Java 1.4 with no Xerces installed, as
      // it doesn't support schema validation. The following is a test for that.
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser parser = factory.newSAXParser();
      XMLReader reader = parser.getXMLReader();
      try {
        reader.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation",
                "test");
      } catch (Exception e) {
        System.err.println(
                "Skipping XMLParser_implTest.testParseResourceSpecifier() because installed XML parser doesn't support schema validation.");
        return;
      }

      // test schema validation
      File invalid = JUnitExtension.getFile("XmlParserTest/NotConformingToSchema.xml");
      try {
        mXmlParser.parseResourceSpecifier(new XMLInputSource(invalid));
        fail();
      } catch (InvalidXMLException e) {
        // do nothing
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testParseFlowControllerDescription() throws Exception {
    XMLInputSource in = new XMLInputSource(
            JUnitExtension.getFile("TextAnalysisEngineImplTest/FlowControllerForErrorTest.xml"));
    FlowControllerDescription desc = mXmlParser.parseFlowControllerDescription(in);
    assertEquals("Flow Controller for Error Test", desc.getMetaData().getName());
  }

  @Test
  public void testParseURISpecifier() throws Exception {
    XMLInputSource in = new XMLInputSource(
            JUnitExtension.getFile("XmlParserTest/TestUriSpecifier.xml"));
    URISpecifier uriSpec = mXmlParser.parseURISpecifier(in);
    assertEquals("AnalysisEngine", uriSpec.getResourceType());
    assertEquals("Vinci", uriSpec.getProtocol());
    assertEquals(60000, uriSpec.getTimeout().intValue());
    Parameter[] params = uriSpec.getParameters();
    assertEquals(2, params.length);
    assertEquals("VNS_HOST", params[0].getName());
    assertEquals("some.internet.ip.name-or-address", params[0].getValue());
    assertEquals("VNS_PORT", params[1].getName());
    assertEquals("9000", params[1].getValue());
  }

  @Test
  public void testParseCustomResourceSpecifier() throws Exception {
    XMLInputSource in = new XMLInputSource(
            JUnitExtension.getFile("XmlParserTest/TestCustomResourceSpecifier.xml"));
    CustomResourceSpecifier uriSpec = mXmlParser.parseCustomResourceSpecifier(in);
    assertEquals("foo.bar.MyResource", uriSpec.getResourceClassName());
    Parameter[] params = uriSpec.getParameters();
    assertEquals(2, params.length);
    assertEquals("param1", params[0].getName());
    assertEquals("val1", params[0].getValue());
    assertEquals("param2", params[1].getName());
    assertEquals("val2", params[1].getValue());
  }

  @Test
  public void testParsePearSpecifier() throws Exception {
    XMLInputSource in = new XMLInputSource(
            JUnitExtension.getFile("XmlParserTest/TestPearSpecifier.xml"));
    PearSpecifier pearSpec = mXmlParser.parsePearSpecifier(in);
    assertEquals("/home/user/uimaApp/installedPears/testpear", pearSpec.getPearPath());

    assertThat(pearSpec.getParameters()).extracting(Parameter::getName, Parameter::getValue)
            .containsExactly(tuple("legacyParam1", "legacyVal1"),
                    tuple("legacyParam2", "legacyVal2"));

    assertThat(pearSpec.getPearParameters())
            .extracting(NameValuePair::getName, NameValuePair::getValue)
            .containsExactly(tuple("param1", "stringVal1"), tuple("param2", true));
  }
}
