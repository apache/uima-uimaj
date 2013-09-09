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

import org.apache.uima.collection.metadata.CpeSofaMapping;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.helpers.AttributesImpl;

public class CpeSofaMappingImpl extends MetaDataObject_impl implements CpeSofaMapping {
  private static final long serialVersionUID = -2488866857646083657L;

  private String componentSofaName;

  private String cpeSofaName;

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("sofaNameMapping",
          new PropertyXmlInfo[0]);

  /**
   * Overridden to handle "name" and "value" attributes.
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#getXMLAttributes()
   */
  protected AttributesImpl getXMLAttributes() {
    AttributesImpl attrs = super.getXMLAttributes();
    try {
      if (getCpeSofaName() != null) {
        attrs.addAttribute("", "cpeSofaName", "cpeSofaName", "CDATA", getCpeSofaName());
      }
      if (getComponentSofaName() != null) {
        attrs.addAttribute("", "componentSofaName", "componentSofaName", "CDATA",
                getComponentSofaName());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return attrs;
  }

  /**
   * Overridden to read "name" and "value" attributes.
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#buildFromXMLElement(org.w3c.dom.Element,
   *      org.apache.uima.util.XMLParser, org.apache.uima.util.XMLParser.ParsingOptions)
   */
  public void buildFromXMLElement(Element aElement, XMLParser aParser, ParsingOptions aOptions)
          throws InvalidXMLException {
    // assumes all children are parameter elements
    NamedNodeMap nodeMap = aElement.getAttributes();
    setCpeSofaName(nodeMap.getNamedItem("cpeSofaName").getNodeValue());
    setComponentSofaName(nodeMap.getNamedItem("componentSofaName").getNodeValue());
  }

  /**
   * @return component sofa name
   */
  public String getComponentSofaName() {
    return componentSofaName;
  }

  /**
   * @return cpe sofa name
   */
  public String getCpeSofaName() {
    return cpeSofaName;
  }

  /**
   * @param string
   */
  public void setComponentSofaName(String string) {
    componentSofaName = string;
  }

  /**
   * @param string
   */
  public void setCpeSofaName(String string) {
    cpeSofaName = string;
  }

}
