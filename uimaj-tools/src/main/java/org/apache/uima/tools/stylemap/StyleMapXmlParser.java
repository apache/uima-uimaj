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

package org.apache.uima.tools.stylemap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Vector;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.uima.internal.util.XMLUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A simple SAX parser for Style Map XML documents. A GUI for editing style maps for the annotation
 * viewer.
 * 
 * 
 * 
 */
public class StyleMapXmlParser extends DefaultHandler {
  private static final String FEATURE_VALUE_PREFIX = "[@";

  public Vector annotType = new Vector();

  public Vector styleLabel = new Vector();

  public Vector styleColor = new Vector();

  public Vector featureValue = new Vector();

  private StringBuffer data = new StringBuffer();

  // constructor
  public StyleMapXmlParser(String xmlFile) {
    try {
      // create new SAX Parser
      SAXParserFactory saxParserFactory = XMLUtils.createSAXParserFactory();
      SAXParser parser = saxParserFactory.newSAXParser();

      XMLReader reader = parser.getXMLReader();
      // set the content handler
      reader.setContentHandler(this);
      // parse the document

      // InputSource inputSource = new InputSource(new FileInputStream(xmlFile));
      InputSource inputSource = new InputSource(new ByteArrayInputStream(xmlFile.getBytes()));
      reader.parse(inputSource);
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (FactoryConfigurationError e) {
      e.printStackTrace();
    }

  }

  public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
          throws SAXException {
  }

  public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
    // System.out.println("End Element: " + localName);
    // System.out.println("Characters: " + data.toString().trim());

    if ("pattern".equals(localName) || "pattern".equals(qName)) {
      String patternString = data.toString().trim();
      int featureValueIndex = patternString.indexOf(FEATURE_VALUE_PREFIX);
      if (featureValueIndex == -1) {
        // Simple annotation type pattern:
        annotType.add(patternString);
        featureValue.add("");
      } else {
        String annotationType = patternString.substring(0, featureValueIndex);
        int equalsSignIndex = patternString.indexOf('=');
        String featureName = patternString.substring(featureValueIndex + 2, equalsSignIndex);
        annotType.add((annotationType + ":" + featureName));

        int firstQuoteIndex = patternString.indexOf("'");
        int lastQuoteIndex = patternString.lastIndexOf("'");
        String fValue = patternString.substring(firstQuoteIndex + 1, lastQuoteIndex);
        featureValue.add(fValue);
      }
    } else if ("label".equals(localName) || "label".equals(qName)) {
      styleLabel.add(data.toString().trim());
    } else if ("style".equals(localName) || "style".equals(qName)) {
      styleColor.add(data.toString().trim());
    }

    data.delete(0, data.length());
  }

  public void characters(char[] ch, int start, int length) throws SAXException {
    data.append(ch, start, length);
  }

}
