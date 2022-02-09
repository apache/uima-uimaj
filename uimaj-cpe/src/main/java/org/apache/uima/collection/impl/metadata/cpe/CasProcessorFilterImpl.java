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

package org.apache.uima.collection.impl.metadata.cpe;

import org.apache.uima.collection.metadata.CasProcessorFilter;
import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;
import org.w3c.dom.Element;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * The Class CasProcessorFilterImpl.
 */
public class CasProcessorFilterImpl extends MetaDataObject_impl implements CasProcessorFilter {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1879442561195094666L;

  /** The filter. */
  private String filter;

  /**
   * Instantiates a new cas processor filter impl.
   */
  public CasProcessorFilterImpl() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorFilter#setFilterString(java.lang.String)
   */
  @Override
  public void setFilterString(String aFilterString) {
    filter = aFilterString;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorFilter#getFilterString()
   */
  @Override
  public String getFilterString() {
    return filter;
  }

  /**
   * Overridden to read "name" and "value" attributes.
   *
   * @param aElement
   *          the a element
   * @param aParser
   *          the a parser
   * @param aOptions
   *          the a options
   * @throws InvalidXMLException
   *           the invalid XML exception
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#buildFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser, org.apache.uima.util.XMLParser.ParsingOptions)
   */
  @Override
  public void buildFromXMLElement(Element aElement, XMLParser aParser, ParsingOptions aOptions)
          throws InvalidXMLException {
    filter = XMLUtils.getText(aElement);

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.resource.metadata.impl.MetaDataObject_impl#toXML(org.xml.sax.ContentHandler,
   * boolean)
   */
  @Override
  public void toXML(ContentHandler aContentHandler, boolean aWriteDefaultNamespaceAttribute)
          throws SAXException {
    XmlizationInfo inf = getXmlizationInfo();

    // write the element's start tag
    // get attributes (can be provided by subclasses)
    AttributesImpl attrs = getXMLAttributes();
    // add default namespace attr if desired
    if (aWriteDefaultNamespaceAttribute) {
      if (inf.namespace != null) {
        attrs.addAttribute("", "xmlns", "xmlns", null, inf.namespace);
      }
    }

    // start element
    aContentHandler.startElement(inf.namespace, inf.elementTagName, inf.elementTagName, attrs);

    aContentHandler.characters(filter.toCharArray(), 0, filter.length());

    // end element
    aContentHandler.endElement(inf.namespace, inf.elementTagName, inf.elementTagName);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXmlizationInfo()
   */
  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  /** The Constant XMLIZATION_INFO. */
  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("filter",
          new PropertyXmlInfo[0]);

  /**
   * Gets the filter.
   *
   * @return the filter
   */
  public String getFilter() {
    return filter;
  }

  /**
   * Sets the filter.
   *
   * @param string
   *          the new filter
   */
  public void setFilter(String string) {
    filter = string;
  }

}
