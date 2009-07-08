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

import java.net.URL;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.SaxDeserializer;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLizable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Reference implementation of {@link SaxDeserializer}.
 * 
 * 
 */
public class SaxDeserializer_impl implements SaxDeserializer {
  static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

  static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

  static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

  private static final SAXTransformerFactory transformerFactory = (SAXTransformerFactory) SAXTransformerFactory
          .newInstance();

  private DOMResult mDOMResult;

  private XMLParser mUimaXmlParser;

  private XMLParser.ParsingOptions mOptions;
 
  private TransformerHandler mTransformerHandler;

  /**
   * Creates a new SAX Deserializer.
   * 
   * @param aUimaXmlParser
   *          the UIMA XML parser that knows the XML element to Java class mappings and which is
   *          used to assist in the serialization.
   * @param aOptions
   *          option settings
   */
  public SaxDeserializer_impl(XMLParser aUimaXmlParser, XMLParser.ParsingOptions aOptions) {
    mUimaXmlParser = aUimaXmlParser;
    mOptions = aOptions;

    // use a TransformerHandler to convert SAX events to DOM
    try {
      mTransformerHandler = transformerFactory.newTransformerHandler();
      mDOMResult = new DOMResult();
      mTransformerHandler.setResult(mDOMResult);
    } catch (TransformerConfigurationException e) {
      throw new UIMARuntimeException(e);
    }
  }
  
  /**
   * Creates a new SAX Deserializer.
   * 
   * @param aUimaXmlParser
   *          the UIMA XML parser that knows the XML element to Java class mappings and which is
   *          used to assist in the serialization.
   * @param aNamespaceForSchema
   *          not used
   * @param aSchemaUrl
   *          not used
   * @param aOptions
   *          option settings
   *          
   * @deprecated Use {@link #SaxDeserializer_impl(XMLParser, XMLParser.ParsingOptions)} instead.
   */
  @Deprecated
public SaxDeserializer_impl(XMLParser aUimaXmlParser, String aNamespaceForSchema, URL aSchemaUrl,
          XMLParser.ParsingOptions aOptions) {
    this(aUimaXmlParser, aOptions);
  }

  /**
   * @see org.apache.uima.util.SaxDeserializer#getObject()
   */
  public XMLizable getObject() throws InvalidXMLException {
    Node rootDomNode = ((Document) mDOMResult.getNode()).getDocumentElement();

    // build the object
    XMLizable result = mUimaXmlParser.buildObject((Element) rootDomNode, mOptions);

    // clear state to prepare for another parse
    mDOMResult = new DOMResult();
    mTransformerHandler.setResult(mDOMResult);

    return result;
  }

  /**
   * @see org.xml.sax.ContentHandler#characters(char[], int, int)
   */
  public void characters(char[] ch, int start, int length) throws SAXException {
    // System.out.println("SaxDeserializer_impl::characters");
    mTransformerHandler.characters(ch, start, length);
  }

  /**
   * @see org.xml.sax.ContentHandler#endDocument()
   */
  public void endDocument() throws SAXException {
    // System.out.println("SaxDeserializer_impl::endDocument");
    mTransformerHandler.endDocument();
  }

  /**
   * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
    // System.out.println("SaxDeserializer_impl::endElement");
    mTransformerHandler.endElement(namespaceURI, localName, qName);
  }

  /**
   * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
   */
  public void endPrefixMapping(String prefix) throws SAXException {
    // System.out.println("SaxDeserializer_impl::endPrefixMapping");
    mTransformerHandler.endPrefixMapping(prefix);
  }

  /**
   * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
   */
  public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    mTransformerHandler.ignorableWhitespace(ch, start, length);
  }

  /**
   * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String, java.lang.String)
   */
  public void processingInstruction(String target, String data) throws SAXException {
    // System.out.println("SaxDeserializer_impl::processingInstruction");
    mTransformerHandler.processingInstruction(target, data);
  }

  /**
   * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
   */
  public void setDocumentLocator(Locator locator) {
    // System.out.println("SaxDeserializer_impl::setDocumentLocator");
    mTransformerHandler.setDocumentLocator(locator);
  }

  /**
   * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
   */
  public void skippedEntity(String name) throws SAXException {
    mTransformerHandler.skippedEntity(name);
  }

  /**
   * @see org.xml.sax.ContentHandler#startDocument()
   */
  public void startDocument() throws SAXException {
    // System.out.println("SaxDeserializer_impl::startDocument");
    mTransformerHandler.startDocument();
  }

  /**
   * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String,
   *      java.lang.String, org.xml.sax.Attributes)
   */
  public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
          throws SAXException {
    // System.out.println("SaxDeserializer_impl::startElement("+namespaceURI+","+localName+","+qName+","+atts+")");
    mTransformerHandler.startElement(namespaceURI, localName, qName, atts);
  }

  /**
   * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
   */
  public void startPrefixMapping(String prefix, String uri) throws SAXException {
    // System.out.println("SaxDeserializer_impl::startPrefixMapping("+prefix+","+uri+")");
    mTransformerHandler.startPrefixMapping(prefix, uri);
  }
}
