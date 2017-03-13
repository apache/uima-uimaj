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

import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.search.Attribute;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.w3c.dom.Element;
import org.xml.sax.helpers.AttributesImpl;

/**
 * 
 * 
 */
public class Attribute_impl extends MetaDataObject_impl implements Attribute {
  private static final long serialVersionUID = -7368701438572498616L;

  private String mValue;

  private String mName;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.Attribute#getName()
   */
  public String getName() {
    return mName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.Attribute#setName(java.lang.String)
   */
  public void setName(String aName) {
    mName = aName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.Attribute#getValue()
   */
  public String getValue() {
    return mValue;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.Attribute#setValue(java.lang.String)
   */
  public void setValue(String aValue) {
    mValue = aValue;
  }

  /**
   * Overridden to write the name and value properties as XML attributes.
   * 
   * @see MetaDataObject_impl#getXMLAttributes()
   */
  protected AttributesImpl getXMLAttributes() {
    AttributesImpl attrs = super.getXMLAttributes();
    attrs.addAttribute("", "name", "name", "", getName());
    attrs.addAttribute("", "value", "value", "", getValue());
    return attrs;
  }

  /**
   * Overridden to read the name and value properties from XML attributes.
   * 
   * @see org.apache.uima.util.XMLizable#buildFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser)
   */
  public void buildFromXMLElement(Element aElement, XMLParser aParser,
          XMLParser.ParsingOptions aOptions) throws InvalidXMLException {
    setName(aElement.getAttribute("name"));
    setValue(aElement.getAttribute("value"));

    // call superclass method for good measure
    super.buildFromXMLElement(aElement, aParser, aOptions);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXmlizationInfo()
   */
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("attribute",
          new PropertyXmlInfo[] {
          // name and value are attributes, not elements
          });

}
