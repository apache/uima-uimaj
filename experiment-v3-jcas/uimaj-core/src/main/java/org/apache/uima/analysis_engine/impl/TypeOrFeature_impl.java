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

package org.apache.uima.analysis_engine.impl;

import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Reference implementation of {@link TypeOrFeature}.
 * 
 * 
 */
public class TypeOrFeature_impl extends MetaDataObject_impl implements TypeOrFeature {

  static final long serialVersionUID = 7184362666821154031L;

  private boolean mType;

  private String mName;

  private boolean mAllAnnotatorFeatures;

  /**
   * @see org.apache.uima.analysis_engine.TypeOrFeature#isType()
   */
  public boolean isType() {
    return mType;
  }

  /**
   * @see org.apache.uima.analysis_engine.TypeOrFeature#setType(boolean)
   */
  public void setType(boolean aType) {
    mType = aType;
  }

  /**
   * @see org.apache.uima.analysis_engine.TypeOrFeature#getName()
   */
  public String getName() {
    return mName;
  }

  /**
   * @see org.apache.uima.analysis_engine.TypeOrFeature#setName(java.lang.String)
   */
  public void setName(String aName) {
    mName = aName;
  }

  /**
   * @see org.apache.uima.analysis_engine.TypeOrFeature#isAllAnnotatorFeatures()
   */
  public boolean isAllAnnotatorFeatures() {
    return mAllAnnotatorFeatures;
  }

  /**
   * @see org.apache.uima.analysis_engine.TypeOrFeature#setAllAnnotatorFeatures(boolean)
   */
  public void setAllAnnotatorFeatures(boolean aAllAnnotatorFeatures) {
    mAllAnnotatorFeatures = aAllAnnotatorFeatures;
  }

  /**
   * Overridden to provide custom XML representation.
   * 
   * @see org.apache.uima.util.XMLizable#buildFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser)
   */
  public void buildFromXMLElement(Element aElement, XMLParser aParser,
          XMLParser.ParsingOptions aOptions) throws InvalidXMLException {
    // element tag determines whether this is a type or a feature
    if (aElement.getTagName().equals("type")) {
      setType(true);
      // AllAnnotatorFeatures determined by attribute
      setAllAnnotatorFeatures(aElement.getAttribute("allAnnotatorFeatures").equals("true"));
    } else // feature
    {
      setType(false);
    }

    // name is the tag's text
    setName(XMLUtils.getText(aElement).trim());
  }

  /**
   * Overridden to provide custom XML representation.
   * 
   * @see org.apache.uima.util.XMLizable#toXML(ContentHandler)
   */
  public void toXML(ContentHandler aContentHandler, boolean aWriteDefaultNamespaceAttribute)
          throws SAXException {
    if (null == serialContext.get()) {
      getSerialContext(aContentHandler);  
      try {
        toXMLinner(aWriteDefaultNamespaceAttribute);
      } finally {
        serialContext.remove();
      }
    } else {
      toXMLinner(aWriteDefaultNamespaceAttribute);
    }
  }
    
  public void toXMLinner(boolean aWriteDefaultNamespaceAttribute)
      throws SAXException {    
    SerialContext sc = serialContext.get();
    Serializer serializer = sc.serializer;
    
    String namespace = getXmlizationInfo().namespace;

    if (isType()) {
      // tag is "type"
      Node node = serializer.findMatchingSubElement("type");

      // if allAnnotatorFeatures is true, write that as an attribute
      if (isAllAnnotatorFeatures()) {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "allAnnotatorFeatures", "allAnnotatorFeatures", "", "true");
        serializer.outputStartElement(node, namespace, "type", "type", attrs);
//        aContentHandler.startElement(getXmlizationInfo().namespace, "type", "type", attrs);
      } else {
        serializer.outputStartElement(node, namespace, "type", "type", new AttributesImpl());
//        aContentHandler.startElement(getXmlizationInfo().namespace, "type", "type",
//                new AttributesImpl());
      }
      // write type name here
      serializer.writeSimpleValue(getName());

      serializer.outputEndElement(node, namespace, "type", "type");
//      aContentHandler.endElement(getXmlizationInfo().namespace, "type", "type");
    } else // feature
    {
      Node node = serializer.findMatchingSubElement("feature");
      serializer.outputStartElement(node, namespace, "feature", "feature",
          new AttributesImpl());
//      aContentHandler.startElement(getXmlizationInfo().namespace, "feature", "feature",
//              new AttributesImpl());

      serializer.writeSimpleValue(getName());
      serializer.outputEndElement(node, namespace, "feature", "feature");
//      aContentHandler.endElement(getXmlizationInfo().namespace, "feature", "feature");
    }
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(TypeOrFeature obj) {
    if (this == obj)
      return 0;

    // cast object
    TypeOrFeature_impl tof = (TypeOrFeature_impl) obj;
    return this.getName().compareTo(tof.getName());
  }

  /**
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXmlizationInfo()
   */
  protected XmlizationInfo getXmlizationInfo() {
    return new XmlizationInfo(null, null);
    // this object has custom XMLization routines
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + (mAllAnnotatorFeatures ? 1231 : 1237);
    result = prime * result + ((mName == null) ? 0 : mName.hashCode());
    result = prime * result + (mType ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    TypeOrFeature_impl other = (TypeOrFeature_impl) obj;
    if (mAllAnnotatorFeatures != other.mAllAnnotatorFeatures) {
      return false;
    }
    if (mName == null) {
      if (other.mName != null) {
        return false;
      }
    } else if (!mName.equals(other.mName)) {
      return false;
    }
    if (mType != other.mType) {
      return false;
    }
    return true;
  }
}
