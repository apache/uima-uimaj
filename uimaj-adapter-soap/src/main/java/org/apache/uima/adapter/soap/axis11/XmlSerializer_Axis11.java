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

package org.apache.uima.adapter.soap.axis11;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.apache.axis.Constants;
import org.apache.axis.encoding.SerializationContext;
import org.apache.axis.encoding.Serializer;
import org.apache.axis.wsdl.fromJava.Types;
import org.apache.uima.util.XMLizable;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * An Axis serializer for any {@link XMLizable} object.
 * <p>
 * This class works only under Axis v1.1.
 */
public class XmlSerializer_Axis11 implements Serializer {

  private static final long serialVersionUID = -1528534494294634599L;

  /**
   * Serialize an element named name, with the indicated attributes and value.
   * 
   * @param name
   *          is the element name
   * @param attributes
   *          are the attributes...serializer is free to add more.
   * @param value
   *          is the value
   * @param context
   *          is the SerializationContext
   */
  public void serialize(QName name, Attributes attributes, Object value,
          SerializationContext context) throws IOException {
    if (value instanceof XMLizable) {
      try {
        // System.out.println("AxisResourceServiceSerializer::serialize(" + name + ")");
        context.startElement(name, attributes);

        SerializerContentHandler contentHandler = new SerializerContentHandler(context);
        ((XMLizable) value).toXML(contentHandler);
        context.endElement();
      } catch (SAXException e) {
        throw new IOException("SAXException: " + e.getMessage());
      }
    } else {
      throw new IOException("Can't serialize a " + value.getClass().getName()
              + " with an XmlSerializer.");
    }
  }

  public String getMechanismType() {
    return Constants.AXIS_SAX;
  }

  /**
   * @see org.apache.axis.encoding.Serializer#writeSchema(java.lang.Class,
   *      org.apache.axis.wsdl.fromJava.Types)
   */
  public Element writeSchema(Class javaType, Types types) throws Exception {
    return null;
  }

  /**
   * Inner class that implements SAX ContentHandler and writes events through to the Axis
   * SerializationContext.
   * 
   * 
   */
  static class SerializerContentHandler extends DefaultHandler {
    private SerializationContext mContext;

    SerializerContentHandler(SerializationContext aContext) {
      mContext = aContext;
    }

    /**
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] ch, int start, int length) throws SAXException {
      try {
        // System.out.println("Calling SerializationContext.writeChars");
        mContext.writeChars(ch, start, length);
      } catch (IOException e) {
        throw new SAXException("IOException:" + e.getMessage());
      }
    }

    /**
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    public void endElement(String uri, String localName, String qName) throws SAXException {
      try {
        // System.out.println("Calling SerializationContext.endElement(" + qName + ")");
        mContext.endElement();
      } catch (IOException e) {
        throw new SAXException("IOException:" + e.getMessage());
      }
    }

    /**
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String,
     *      java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
      try {
        // System.out.println("Calling SerializationContext.startElement(" + qName + ")");
        mContext.startElement(new QName(uri, localName), attributes);
      } catch (IOException e) {
        throw new SAXException("IOException:" + e.getMessage());
      }
    }

  }
}
