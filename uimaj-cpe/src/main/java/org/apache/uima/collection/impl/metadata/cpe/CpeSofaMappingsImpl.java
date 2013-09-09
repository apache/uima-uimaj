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

import java.util.ArrayList;

import org.apache.uima.collection.metadata.CpeSofaMapping;
import org.apache.uima.collection.metadata.CpeSofaMappings;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class CpeSofaMappingsImpl extends MetaDataObject_impl implements CpeSofaMappings {
  private static final long serialVersionUID = -4193487704594409253L;

  private ArrayList sofaNameMappings = new ArrayList();

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  /**
   * Overridden to read "name" and "value" attributes.
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#buildFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser, org.apache.uima.util.XMLParser.ParsingOptions)
   */
  public void buildFromXMLElement(Element aElement, XMLParser aParser, ParsingOptions aOptions)
          throws InvalidXMLException {
    NodeList nodes = aElement.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);
      if (node instanceof Element) {
        CpeSofaMapping sofaMapping = new CpeSofaMappingImpl();
        NamedNodeMap atts = node.getAttributes();
        if (atts.getNamedItem("componentSofaName") != null
                && atts.getNamedItem("componentSofaName").getNodeValue() != null) {
          sofaMapping.setComponentSofaName(atts.getNamedItem("componentSofaName").getNodeValue());
        }
        if (atts.getNamedItem("cpeSofaName") != null
                && atts.getNamedItem("cpeSofaName").getNodeValue() != null) {
          sofaMapping.setCpeSofaName(atts.getNamedItem("cpeSofaName").getNodeValue());
        }
        sofaNameMappings.add(sofaMapping);
      }
    }
  }

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

    // write child elements
    for (int i = 0; i < sofaNameMappings.size(); i++) {
      ((CpeSofaMapping) sofaNameMappings.get(i)).toXML(aContentHandler,
              aWriteDefaultNamespaceAttribute);
    }

    // end element
    aContentHandler.endElement(inf.namespace, inf.elementTagName, inf.elementTagName);
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("sofaNameMappings",
          new PropertyXmlInfo[] { new PropertyXmlInfo("sofaNameMapping", null), });

  /**
   * @return sofa name mappings
   */
  public CpeSofaMapping[] getSofaNameMappings() {
    CpeSofaMapping[] sofaMappings = new CpeSofaMapping[sofaNameMappings.size()];
    return (CpeSofaMapping[]) sofaNameMappings.toArray(sofaMappings);
  }

  /**
   * @param sofaMappings
   */
  public void setSofaNameMappings(CpeSofaMapping[] sofaMappings) {
    for (int i = 0; sofaMappings != null && i < sofaMappings.length; i++) {
      sofaNameMappings.add(sofaMappings[i]);
    }
    // sofaNameMappings = list;
  }

}
