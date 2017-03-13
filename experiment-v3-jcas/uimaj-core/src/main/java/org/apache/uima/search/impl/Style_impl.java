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
import org.apache.uima.search.Mapping;
import org.apache.uima.search.Style;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.w3c.dom.Element;
import org.xml.sax.helpers.AttributesImpl;

/**
 * 
 * 
 */
public class Style_impl extends MetaDataObject_impl implements Style {
  private static final long serialVersionUID = -1506108274843606087L;

  private Attribute[] mAttributes = new Attribute[0];

  private String mName;

  private Mapping[] mAttributeMappings;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.Style#getName()
   */
  public String getName() {
    return mName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.Style#setName(java.lang.String)
   */
  public void setName(String aName) {
    mName = aName.intern();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.Style#getAttributes()
   */
  public Attribute[] getAttributes() {
    return mAttributes;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.Style#setAttributes(org.apache.uima.search.Attribute[])
   */
  public void setAttributes(Attribute[] aAttributes) {
    mAttributes = (aAttributes == null) ? new Attribute[0] : aAttributes;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.search.Style#getAttribute(java.lang.String)
   */
  public String getAttribute(String aName) {
    Attribute[] attrs = getAttributes();
    for (int i = 0; i < attrs.length; i++) {
      if (aName.equals(attrs[i].getName())) {
        return attrs[i].getValue();
      }
    }
    return null;
  }

  public Mapping[] getAttributeMappings() {
    return mAttributeMappings;
  }

  public void setAttributeMappings(Mapping[] aMappings) {
    mAttributeMappings = aMappings;
  }

  /**
   * Overridden to write the name property as an XML attribute.
   * 
   * @see MetaDataObject_impl#getXMLAttributes()
   */
  protected AttributesImpl getXMLAttributes() {
    AttributesImpl attrs = super.getXMLAttributes();
    attrs.addAttribute("", "name", "name", "", getName());
    return attrs;
  }

  /**
   * Overridden to read the name property from XML attributes.
   * 
   * @see org.apache.uima.util.XMLizable#buildFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser)
   */
  public void buildFromXMLElement(Element aElement, XMLParser aParser,
          XMLParser.ParsingOptions aOptions) throws InvalidXMLException {
    setName(aElement.getAttribute("name"));

    // call superclass method to parse the "attributes" property, which is stored
    // as child elements (confusing, isn't it? :)
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

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("style",
          new PropertyXmlInfo[] {
          // name is an attribute, not an element
              new PropertyXmlInfo("attributes", null), new PropertyXmlInfo("attributeMappings") });
}
