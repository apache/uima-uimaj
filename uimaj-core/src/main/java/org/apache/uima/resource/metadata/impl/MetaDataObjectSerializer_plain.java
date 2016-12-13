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

package org.apache.uima.resource.metadata.impl;

import org.apache.uima.util.XMLSerializer.CharacterValidatingContentHandler;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


class MetaDataObjectSerializer_plain implements MetaDataObject_impl.Serializer {

  private static final Attributes EMPTY_ATTRIBUTES = new AttributesImpl();
  
  final private ContentHandler ch;
  
  MetaDataObjectSerializer_plain(ContentHandler ch) {
    this.ch = ch;
  }
  
  @Override
  public void saveAndAddNodeStore(Node infoset) {}

  @Override
  public void deleteNodeStore() {}
  
  @Override
  public boolean indentChildElements(XmlizationInfo info, MetaDataObject_impl mdo) {
    return false;  // return something, value not used
  }
  
  @Override
  public void insertNl() {}
  
  @Override
  public boolean shouldBeSkipped(PropertyXmlInfo propInfo, Object val, MetaDataObject_impl mdo) {
    return propInfo.omitIfNull && mdo.valueIsNullOrEmptyArray(val);   
  }
  
  @Override
  public boolean startElementProperty() {
    return true;
  }
  
  @Override
  public void addNodeStore() {};
  
  @Override
  public void writeDelayedStart(String name) {}
  
  @Override
  public void writeSimpleValue(Object val) throws SAXException {
    String valStr = val.toString();
    ch.characters(valStr.toCharArray(),  0,  valStr.length());
  }

  @Override
  public void writeSimpleValueWithTag(String className, Object o, Node node) throws SAXException {
    outputStartElement(node, "", className, className, EMPTY_ATTRIBUTES);
    String valStr = o.toString();
    ch.characters(valStr.toCharArray(),  0,  valStr.length());
    outputEndElement(node, "",  className,  className);
  }
  
  @Override
  public boolean shouldEncloseInArrayElement(Class propClass) {
    return propClass == Object.class;   
  }
  
  @Override
  public void outputStartElement(Node node, String aNamespace, String localname, String qname, Attributes attributes) throws SAXException {
    if (null == localname) {  // happens for <flowConstraints>
       // <fixedFlow> <== after outputting this,
       // called writePropertyAsElement
       // But there is no <array>...
       // <node>A</node>
       // <node>B</node>
      return;
    }
    ch.startElement(aNamespace, localname, qname, attributes);
  }

  // only used for default prefix, passing "" as prefix
  // if ever used for others, need to add endPrefixMapping(String prefix) as well
  // https://issues.apache.org/jira/browse/UIMA-5177 
  public void startPrefixMapping(String prefix, String uri) throws SAXException {
    ch.startPrefixMapping(prefix, uri);
  }
  
  @Override
  public void outputEndElement(Node node, String aNamespace, String localname, String qname) throws SAXException {
    if (null == localname) {
      return;
    }
    ch.endElement(aNamespace, localname, qname);
  }
  
  @Override
  public void outputStartElementForArrayElement(Node node,
      String nameSpace, String localName, String qName, Attributes attributes) throws SAXException {
    outputStartElement(node, nameSpace, localName, qName, attributes);
  }
  
  @Override
  public void outputEndElementForArrayElement(Node node, String aNamespace,
      String localname, String qname) throws SAXException {
    outputEndElement(node, aNamespace, localname, qname);
  }
    
  @Override
  public void maybeStartArraySymbol() {}
  
  @Override
  public void maybeEndArraySymbol() {}
  
  @Override
  public Node findMatchingSubElement(String elementName) {
    return null;
  }

  @Override
  public boolean isArrayHasIndentableElements(Object array) {return false;}

}