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

package org.apache.uima.search.impl;

import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.search.Filter;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;
import org.w3c.dom.Element;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * 
 * 
 */
public class Filter_impl extends MetaDataObject_impl implements Filter {
  private static final long serialVersionUID = -5839668733093703591L;

  private String mExpression;

  private String mSyntax;

  public Filter_impl() {
  }

  /**
   * @param aSyntax -
   * @param aExpression -
   */
  public Filter_impl(String aSyntax, String aExpression) {
    setSyntax(aSyntax);
    setExpression(aExpression);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.Filter#getSyntax()
   */
  public String getSyntax() {
    return mSyntax;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.Filter#setSyntax(java.lang.String)
   */
  public void setSyntax(String aSyntax) {
    mSyntax = aSyntax;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.Filter#getExpression()
   */
  public String getExpression() {
    return mExpression;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.Filter#setExpression(java.lang.String)
   */
  public void setExpression(String aExpression) {
    mExpression = aExpression;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.XMLizable#buildFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser, org.apache.uima.util.XMLParser.ParsingOptions)
   */
  public void buildFromXMLElement(Element aElement, XMLParser aParser, ParsingOptions aOptions)
          throws InvalidXMLException {
    super.buildFromXMLElement(aElement, aParser, aOptions);
    setSyntax(aElement.getAttribute("syntax"));
    setExpression(XMLUtils.getText(aElement));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.util.XMLizable#toXML(org.xml.sax.ContentHandler, boolean)
   */
  public void toXML(ContentHandler aContentHandler, boolean aWriteDefaultNamespaceAttribute)
          throws SAXException {
    // write the element's start tag
    AttributesImpl attrs = new AttributesImpl();
    attrs.addAttribute("", "syntax", "syntax", "", getSyntax());

    // start element
    aContentHandler.startElement("", "filter", "filter", attrs);

    // write content
    String expr = getExpression();
    aContentHandler.characters(expr.toCharArray(), 0, expr.length());

    // end element
    aContentHandler.endElement("", "filter", "filter");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXmlizationInfo()
   */
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("filter",
          new PropertyXmlInfo[] {
          // custom XMLization -- syntax is an attribute, expression is content
          });

}
