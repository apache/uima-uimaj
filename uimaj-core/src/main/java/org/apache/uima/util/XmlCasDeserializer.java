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

package org.apache.uima.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.impl.OutOfTypeSystemData;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.internal.util.XMLUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Deserializes a CAS from a standoff-XML format. This class can read the XMI format introduced in
 * UIMA v1.4 as well as the XCAS format from previous versions.
 */
public abstract class XmlCasDeserializer {
  /**
   * Deserializes a CAS from a standoff-XML format.
   * 
   * @param aStream
   *          input stream from which to read the XML document
   * @param aCAS
   *          CAS into which to deserialize. This CAS must be set up with a type system that is
   *          compatible with that in the XML.
   * 
   * @throws SAXException
   *           if an XML Parsing error occurs
   * @throws IOException
   *           if an I/O failure occurs
   */
  public static void deserialize(InputStream aStream, CAS aCAS) throws SAXException, IOException {
    deserialize(aStream, aCAS, false);
  }

  /**
   * Deserializes a CAS from XMI or XCAS.
   * 
   * @param aStream
   *          input stream from which to read the XML document
   * @param aCAS
   *          CAS into which to deserialize. This CAS must be set up with a type system that is
   *          compatible with that in the XML
   * @param aLenient
   *          if true, unknown Types will be ignored. If false, unknown Types will cause an
   *          exception. The default is false.
   * 
   * @throws SAXException
   *           if an XML Parsing error occurs
   * @throws IOException
   *           if an I/O failure occurs
   */
  public static void deserialize(InputStream aStream, CAS aCAS, boolean aLenient)
          throws SAXException, IOException {
    XMLReader xmlReader = XMLUtils.createXMLReader();
    XmlCasDeserializerHandler handler = new XmlCasDeserializerHandler(aCAS, aLenient);
    xmlReader.setContentHandler(handler);
    xmlReader.parse(new InputSource(aStream));
  }

  /**
   * Deserializes a CAS from XMI or XCAS, version returning the SerialFormat
   * 
   * @param aStream
   *          input stream from which to read the XML document
   * @param aCAS
   *          CAS into which to deserialize. This CAS must be set up with a type system that is
   *          compatible with that in the XML
   * @param aLenient
   *          if true, unknown Types will be ignored. If false, unknown Types will cause an
   *          exception. The default is false.
   * @return the format of the data  
   * 
   * @throws SAXException
   *           if an XML Parsing error occurs
   * @throws IOException
   *           if an I/O failure occurs
   */
  static SerialFormat deserializeR(InputStream aStream, CAS aCAS, boolean aLenient)
      throws SAXException, IOException {
    XMLReader xmlReader = XMLUtils.createXMLReader();
    XmlCasDeserializerHandler handler = new XmlCasDeserializerHandler(aCAS, aLenient);
    xmlReader.setContentHandler(handler);
    xmlReader.parse(new InputSource(aStream));
    return (handler.mDelegateHandler instanceof XmiCasDeserializer.XmiCasDeserializerHandler)
             ? SerialFormat.XMI
             : SerialFormat.XCAS;
  }

  static class XmlCasDeserializerHandler extends DefaultHandler {
    private CAS mCAS;

    private boolean mLenient;

    private ContentHandler mDelegateHandler; // will be set to either XMI or XCAS

    XmlCasDeserializerHandler(CAS cas, boolean lenient) {
      mCAS = cas;
      mLenient = lenient;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
      if (mDelegateHandler == null) {
        // try to find out whether we should use the XCAS or XMI deserializers
        // if there's an xmi:version attribute, always use XMI
        String xmiVer = attributes.getValue("xmi:version");
        if (xmiVer != null && xmiVer.length() > 0) {
          XmiCasDeserializer deser = new XmiCasDeserializer(mCAS.getTypeSystem());
          mDelegateHandler = deser.getXmiCasHandler(mCAS, mLenient);
        } else if ("CAS".equals(localName)) // use XCAS
        {
          XCASDeserializer deser = new XCASDeserializer(mCAS.getTypeSystem());
          mDelegateHandler = deser
                  .getXCASHandler(mCAS, mLenient ? new OutOfTypeSystemData() : null);
        } else // default to XMI
        {
          XmiCasDeserializer deser = new XmiCasDeserializer(mCAS.getTypeSystem());
          mDelegateHandler = deser.getXmiCasHandler(mCAS, mLenient);
        }
        mDelegateHandler.startDocument();
      }
      mDelegateHandler.startElement(uri, localName, qName, attributes);
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
      mDelegateHandler.characters(ch, start, length);
    }

    public void endDocument() throws SAXException {
      mDelegateHandler.endDocument();
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
      mDelegateHandler.endElement(uri, localName, qName);
    }

    public void error(SAXParseException e) throws SAXException {
      throw e;
    }

    public void fatalError(SAXParseException e) throws SAXException {
      throw e;
    }

    public void warning(SAXParseException e) throws SAXException {
      throw e;
    }
  }
}
