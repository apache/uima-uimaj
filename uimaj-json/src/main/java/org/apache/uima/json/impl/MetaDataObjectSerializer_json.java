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

package org.apache.uima.json.impl;

import java.io.IOException;
import java.lang.reflect.Array;

import org.apache.uima.resource.metadata.AllowedValue;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.XMLizable;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonGenerator;

public class MetaDataObjectSerializer_json implements MetaDataObject_impl.Serializer  {
  
  final private JsonContentHandlerJacksonWrapper jch;
  final private JsonGenerator jg;
  final private boolean isFormattedOutput;

  public MetaDataObjectSerializer_json(JsonContentHandlerJacksonWrapper jch) {
    this.jch = jch;
    jg = jch.getJsonGenerator();
    isFormattedOutput = jch.isFormattedOutput();
  }
 
  @Override
  public void outputStartElement(Node node, String nameSpace, String localName, String qName, Attributes attributes) throws SAXException {
   jch.startElement(null, null, qName, attributes); 
  }
  
  @Override
  public void outputEndElement(Node node, String aNamespace, String localname, String qname) throws SAXException {
    jch.endElement(null, localname, qname);
  }

  
  @Override
  public void outputStartElementForArrayElement(Node node, String nameSpace, String localName, String qName, Attributes attributes) throws SAXException {}
 
  @Override
  public void outputEndElementForArrayElement(Node node, String aNamespace, String localname, String qname) throws SAXException {}

  @Override
  public void saveAndAddNodeStore(Node infoset) {}

  @Override
  public void deleteNodeStore() {}
  
  @Override
  public boolean indentChildElements(XmlizationInfo info, MetaDataObject_impl mdo) {
    return isFormattedOutput && hasXMLizableChild(info.propertyInfo, mdo);
  }
  
  @Override
  public void insertNl() {
    jch.writeNlJustBeforeNext();
  }
  
  @Override
  public boolean shouldBeSkipped(PropertyXmlInfo propInfo, Object val, MetaDataObject_impl mdo) {
    return mdo.valueIsNullOrEmptyArray(val);   
  }
  
  @Override
  public boolean startElementProperty() {
    return false;  // the start is done later for JSON in case omitted.
  }
  
  @Override
  public void addNodeStore() {};
  
  @Override
  public void writeDelayedStart(String name) throws SAXException {
    jgWriteFieldName(name);
  }
  
  @Override
  public void writeSimpleValue(Object val) throws SAXException {
    writePrimitiveJsonValue(val, jg);
  }
    
  @Override
  public boolean shouldEncloseInArrayElement(Class propClass) {
    return false;   
  }
  
  @Override
  public boolean isArrayHasIndentableElements(Object array) {
    Object firstElement = Array.get(array, 0);
    return  !(firstElement instanceof AllowedValue) && (firstElement instanceof XMLizable);
  }
  
  @Override
  public void maybeStartArraySymbol() throws SAXException {
    jgWriteStartArray();
  }
  
  @Override
  public void maybeEndArraySymbol() throws SAXException {
    jgWriteEndArray();
  }
  
  @Override
  /*
   * (non-Javadoc)
   * write {"type" : "value}
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl.Serializer#writeSimpleValueWithTag(java.lang.String, java.lang.Object, org.w3c.dom.Node)
   */
  public void writeSimpleValueWithTag(String className, Object o, Node node) throws SAXException {
    jgWriteStartObject();
    jgWriteFieldName(className);    
    String valStr = (String)o;
    jgWriteString(valStr);
    jgWriteEndObject();
  }

  private boolean hasXMLizableChild(PropertyXmlInfo[] ia, MetaDataObject_impl mdo) {
    for (PropertyXmlInfo pi : ia) {
      Object val = mdo.getAttributeValue(pi.propertyName);
      if (val != null && val instanceof XMLizable) {
        return true;        
      }
    }
    return false;
  }
  
  private void jgWriteFieldName(String name) throws SAXException {
    try {jg.writeFieldName(name);} catch (IOException e) {throw new SAXException(e);}
  }

  private void jgWriteStartArray() throws SAXException {
    try {jg.writeStartArray();} catch (IOException e) {throw new SAXException(e);}
  }

  private void jgWriteEndArray() throws SAXException {
    try {jg.writeEndArray();} catch (IOException e) {throw new SAXException(e);}
  }

  private void jgWriteString(String s) throws SAXException {
    try {jg.writeString(s);} catch (IOException e) {throw new SAXException(e);}
  }
  
  private void jgWriteStartObject() throws SAXException {
    try {jg.writeStartObject();} catch (IOException e) {throw new SAXException(e);}
  }

  private void jgWriteEndObject() throws SAXException {
    try {jg.writeEndObject();} catch (IOException e) {throw new SAXException(e);}
  }


  private static void writePrimitiveJsonValue(Object val, JsonGenerator jg) throws SAXException {
    try {
    if (val instanceof Boolean)
      jg.writeBoolean((Boolean) val);
    else if (val instanceof Integer)
      jg.writeNumber((Integer) val);
    else if (val instanceof Long)
      jg.writeNumber((Long) val);
    else if (val instanceof Short)
      jg.writeNumber((Short) val);
    else if (val instanceof Byte)
      jg.writeNumber((Byte) val);
    else if (val instanceof Float)
      jg.writeNumber((Float) val);
    else if (val instanceof Double)
      jg.writeNumber((Double) val);
    else if (val instanceof String)
      jg.writeString((String) val);
    else
      throw new RuntimeException("unhandled value type");
    } catch (IOException e) {
      throw new SAXException(e);
    }
  }

  @Override
  public Node findMatchingSubElement(String elementName) {return null;}

}
