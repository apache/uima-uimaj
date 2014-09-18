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
import java.io.OutputStream;
import java.io.Writer;

import org.w3c.dom.Element;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * An interface to be implemented by UIMA classes that can be written to and parsed from XML.
 * Classes must implement this interface in order for the UIMA {@link XMLParser} to generate them
 * from XML documents. XMLizable classes also must define a zero-argument constructor. When
 * constructing an object, the <code>XMLParser</code> will first create it using the zero-argument
 * constructor and then call {@link #buildFromXMLElement(Element,XMLParser)}.
 * 
 */
public interface XMLizable {

  /**
   * Writes this object's XML representation as a string. Note that if you want to write the XML to
   * a file or to a byte stream, it is highly recommended that you use {@link #toXML(OutputStream)} 
   * instead, as it ensures that output is written in UTF-8 encoding, which is the default encoding 
   * that should be used for XML files.
   * <p>
   * The XML String that is produced will have a header <code>&lt;?xml version="1.0" encoding="UTF-8"?&gt;</code>.  
   * Therefore you should not write this string out in any encoding other than UTF-8 (for example do not use the
   * default platform encoding), or you will produce output that will not be able to be parsed.
   * 
   * @param aWriter
   *          a Writer to which the XML string will be written
   * 
   * @throws IOException
   *           if an I/O failure occurs
   * @throws SAXException passthru
   */
  public void toXML(Writer aWriter) throws SAXException, IOException;

  /**
   * Writes this object's XML representation as a string in UTF-8 encoding.
   * 
   * @param aOutputStream
   *          an OutputStream to which the XML string will be written, in UTF-8 encoding.
   * 
   * @throws IOException
   *           if an I/O failure occurs
   * @throws SAXException pass thru
   */
  public void toXML(OutputStream aOutputStream) throws SAXException, IOException;

  /**
   * Writes this object's XML representation by making calls on a SAX {@link ContentHandler}. This
   * method just calls <code>toXML(aContentHandler,false)</code>, so subclasses should override
   * that version of this method, not this one.
   * 
   * @param aContentHandler
   *          the content handler to which this object will write events that describe its XML
   *          representation.
   * 
   * @throws SAXException pass thru
   */
  public void toXML(ContentHandler aContentHandler) throws SAXException;

  /**
   * Writes this object's XML representation by making calls on a SAX {@link ContentHandler}.
   * 
   * @param aContentHandler
   *          the content handler to which this object will write events that describe its XML
   *          representation.
   * @param aWriteDefaultNamespaceAttribute
   *          whether the namespace of this element should be written as the default namespace. This
   *          should be done only for the root element, and it defaults to false.
   * 
   * @throws SAXException pass thru
   */
  public void toXML(ContentHandler aContentHandler, boolean aWriteDefaultNamespaceAttribute)
          throws SAXException;

  /**
   * Initializes this object from its XML DOM representation. This method is typically called from
   * the {@link XMLParser}.
   * 
   * @param aElement
   *          the XML element that represents this object.
   * @param aParser
   *          a reference to the UIMA <code>XMLParser</code>. The
   *          {@link XMLParser#buildObject(Element)} method can be used to construct sub-objects.
   * 
   * @throws InvalidXMLException
   *           if the input XML element does not specify a valid object
   */
  public void buildFromXMLElement(Element aElement, XMLParser aParser) throws InvalidXMLException;

  /**
   * Initializes this object from its XML DOM representation. This method is typically called from
   * the {@link XMLParser}.
   * 
   * @param aElement
   *          the XML element that represents this object.
   * @param aParser
   *          a reference to the UIMA <code>XMLParser</code>. The
   *          {@link XMLParser#buildObject(Element)} method can be used to construct sub-objects.
   * @param aOptions
   *          option settings
   * 
   * @throws InvalidXMLException
   *           if the input XML element does not specify a valid object
   */
  public void buildFromXMLElement(Element aElement, XMLParser aParser,
          XMLParser.ParsingOptions aOptions) throws InvalidXMLException;
  
}
