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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.cas_data.CasData;
import org.apache.uima.cas_data.FeatureStructure;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CasDataToXCasTest {

  /*
   * Test for void generateXCas(CasData)
   */
  @Test
  public void testGenerateXCasCasData() throws Exception {
    try {
      CasData casData = new CasDataImpl();
      FeatureStructure testFS = new FeatureStructureImpl();
      testFS.setType("Test");
      testFS.setId("foo");
      testFS.setIndexed(new int[] { 1 });
      testFS.setFeatureValue("myFeature", new PrimitiveValueImpl("myValue"));
      testFS.setFeatureValue("value", new PrimitiveValueImpl("this should show up in XML content"));
      casData.addFeatureStructure(testFS);

      CasDataToXCas generator = new CasDataToXCas();
      TestContentHandler testContentHandler = new TestContentHandler("Test");
      generator.setContentHandler(testContentHandler);
      generator.generateXCas(casData);
      assertThat(testContentHandler.foundTestElement).isTrue();

      // also try colon and dash conversions
      casData = new CasDataImpl();
      testFS = new FeatureStructureImpl();
      testFS.setType("Test_colon_Foo_dash_Bar_colon_What_dash_a_dash_mess");
      testFS.setId("foo");
      testFS.setIndexed(new int[] { 1 });
      testFS.setFeatureValue("myFeature", new PrimitiveValueImpl("myValue"));
      testFS.setFeatureValue("value", new PrimitiveValueImpl("this should show up in XML content"));
      casData.addFeatureStructure(testFS);

      testContentHandler = new TestContentHandler("Test:Foo-Bar:What-a-mess");
      generator.setContentHandler(testContentHandler);
      generator.generateXCas(casData);
      assertThat(testContentHandler.foundTestElement).isTrue();

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public class TestContentHandler extends DefaultHandler {
    String inElement;

    StringBuffer buf;

    boolean foundTestElement = false;

    String testElementName;

    public TestContentHandler(String aTestElementName) {
      testElementName = aTestElementName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String,
     * java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(String arg0, String arg1, String arg2, Attributes arg3)
            throws SAXException {
      this.inElement = arg1;
      this.buf = new StringBuffer();

      if (this.testElementName.equals(arg1)) {
        foundTestElement = true;
        assertThat(arg3.getValue("myFeature")).isEqualTo("myValue");
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    @Override
    public void characters(char[] arg0, int arg1, int arg2) throws SAXException {
      buf.append(arg0, arg1, arg2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String,
     * java.lang.String)
     */
    @Override
    public void endElement(String arg0, String arg1, String arg2) throws SAXException {
      if (this.testElementName.equals(arg1)) {
        assertThat(buf.toString()).isEqualTo("this should show up in XML content");
      }
    }
  }

}
