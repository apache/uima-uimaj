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

import org.apache.uima.collection.metadata.CasProcessorDeploymentParam;
import org.apache.uima.collection.metadata.CasProcessorDeploymentParams;
import org.apache.uima.collection.metadata.CpeDescriptorException;
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

public class CasProcessorDeploymentParamsImpl extends MetaDataObject_impl implements
        CasProcessorDeploymentParams {
  private static final long serialVersionUID = 4871710283477856271L;

  private ArrayList params = new ArrayList();

  public CasProcessorDeploymentParamsImpl() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorDeploymentParams#add(org.apache.uima.collection.metadata.CasProcessorDeploymentParam)
   */
  public void add(CasProcessorDeploymentParam aParam) {
    params.add(aParam);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorDeploymentParams#get(java.lang.String)
   */
  public CasProcessorDeploymentParam get(String aParamName) throws CpeDescriptorException {
    for (int i = 0; params != null && i < params.size(); i++) {
      if (aParamName.equals(((CasProcessorDeploymentParam) params.get(i)).getParameterName())) {
        return (CasProcessorDeploymentParam) params.get(i);
      }
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorDeploymentParams#getAll()
   */
  public CasProcessorDeploymentParam[] getAll() {
    CasProcessorDeploymentParam[] parameters = new CasProcessorDeploymentParamImpl[params.size()];
    params.toArray(parameters);
    return parameters;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.metadata.CasProcessorDeploymentParams#remove(org.apache.uima.collection.metadata.CasProcessorDeploymentParam)
   */
  public void remove(CasProcessorDeploymentParam aParam) throws CpeDescriptorException {
    for (int i = 0; params != null && i < params.size(); i++) {
      if (aParam.equals(params.get(i))) {
        params.remove(i);
      }
    }
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
        // assumes all children are parameter elements
        NamedNodeMap nodeMap = node.getAttributes();
        String paramName = nodeMap.getNamedItem("name").getNodeValue();
        String paramValue = nodeMap.getNamedItem("value").getNodeValue();
        String paramType = "string"; // default
        if (nodeMap.getNamedItem("type") != null) {
          paramType = nodeMap.getNamedItem("type").getNodeValue();
        }
        // nodeMap.getNamedItem("type").getNodeValue();
        CasProcessorDeploymentParam p = new CasProcessorDeploymentParamImpl(paramName, paramValue,
                paramType);
        params.add(p);
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
    for (int i = 0; i < params.size(); i++) {
      ((CasProcessorDeploymentParam) params.get(i)).toXML(aContentHandler,
              aWriteDefaultNamespaceAttribute);
    }

    // end element
    aContentHandler.endElement(inf.namespace, inf.elementTagName, inf.elementTagName);
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("deploymentParameters",
          new PropertyXmlInfo[] { new PropertyXmlInfo("parameter"), });

}
