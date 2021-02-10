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

import org.apache.uima.collection.metadata.CpeInclude;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;
import org.w3c.dom.Element;
import org.xml.sax.helpers.AttributesImpl;


/**
 * The Class CpeIncludeImpl.
 */
public class CpeIncludeImpl extends MetaDataObject_impl implements CpeInclude {
  
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 5694100109656286384L;

  /** The href. */
  private String href;

  /**
   * Instantiates a new cpe include impl.
   */
  public CpeIncludeImpl() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeInclude#set(java.lang.String)
   */
  @Override
  public void set(String aPath) {
    href = aPath;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CpeInclude#get()
   */
  @Override
  public String get() {
    return href;
  }

  /**
   * Overridden to read "href" attribute.
   *
   * @param aElement the a element
   * @param aParser the a parser
   * @param aOptions the a options
   * @throws InvalidXMLException the invalid XML exception
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#buildFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser, org.apache.uima.util.XMLParser.ParsingOptions)
   */
  @Override
  public void buildFromXMLElement(Element aElement, XMLParser aParser, ParsingOptions aOptions)
          throws InvalidXMLException {
    setHref(aElement.getAttribute("href"));

  }

  /**
   * Overridden to handle "href" attribute.
   *
   * @return the XML attributes
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXMLAttributes()
   */
  @Override
  protected AttributesImpl getXMLAttributes() {
    AttributesImpl attrs = super.getXMLAttributes();
    attrs.addAttribute("", "href", "href", "CDATA", getHref());
    return attrs;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXmlizationInfo()
   */
  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  /** The Constant XMLIZATION_INFO. */
  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("include",
          new PropertyXmlInfo[0]);

  /**
   * Gets the href.
   *
   * @return the href
   */
  /*  METHODS CALLED BY THE PARSER * */
  public String getHref() {
    return href;
  }

  /**
   * Sets the href.
   *
   * @param string the new href
   */
  public void setHref(String string) {
    href = string;
  }
}
