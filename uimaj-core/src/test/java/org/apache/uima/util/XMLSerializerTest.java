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
package org.apache.uima.util;

import java.io.ByteArrayOutputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import junit.framework.TestCase;

import org.apache.uima.internal.util.XMLUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;

public class XMLSerializerTest extends TestCase {

  public void testXml10() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    XMLSerializer sax2xml = new XMLSerializer(baos, false);
    ContentHandler ch = sax2xml.getContentHandler();    
    ch.startDocument();
    ch.startElement("","foo","foo", new AttributesImpl());
    ch.endElement("", "foo", "foo");
    ch.endDocument();
    String xmlStr = new String(baos.toByteArray(), "UTF-8");
    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><foo/>", xmlStr);    
  }
  
  public void testXml11() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    XMLSerializer sax2xml = new XMLSerializer(baos, false);
    sax2xml.setOutputProperty(OutputKeys.VERSION, "1.1");
    ContentHandler ch = sax2xml.getContentHandler();    
    ch.startDocument();
    ch.startElement("","foo","foo", new AttributesImpl());
    ch.endElement("", "foo", "foo");
    ch.endDocument();
    String xmlStr = new String(baos.toByteArray(), "UTF-8");
//    if (xmlStr.contains("1.0")) {
    // useful to investigate issues when bad XML output is produced
    //   related to which Java implementation is being used
      TransformerFactory transformerFactory = XMLUtils.createTransformerFactory();
      Transformer t = transformerFactory.newTransformer();
      t.setOutputProperty(OutputKeys.VERSION, "1.1");
      
      System.out.println("Java version is " + 
                            System.getProperty("java.vendor") + " " +
                            System.getProperty("java.version") + " " +
                            System.getProperty("java.vm.name") + " " +
                            System.getProperty("java.vm.version") + 
                         "\n  javax.xml.transform.TransformerFactory: " +
                            System.getProperty("javax.xml.transform.TransformerFactory") + 
                         "\n  Transformer version: " +
                            t.getOutputProperty(OutputKeys.VERSION));
//    }
    assertEquals("<?xml version=\"1.1\" encoding=\"UTF-8\"?><foo/>", xmlStr);
  }
  
  public void testXml10Error() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    XMLSerializer sax2xml = new XMLSerializer(baos, false);
    ContentHandler ch = sax2xml.getContentHandler();    
    ch.startDocument();
    char[] data = new char[] {32, 33, 5, 34};
    
    ch.startElement("","foo","foo", new AttributesImpl());
    boolean eh = false;
    try {
      ch.characters(data, 0, 4);
    } catch (SAXParseException e) {
      String msg = e.getMessage();
      String expected = "Trying to serialize non-XML 1.0 character: " + "0x5 at offset 2";
      assertEquals(msg.substring(0, expected.length()), expected);
      eh = true;
    }  
    assertTrue(eh);
  }

  public void testXml11Error() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    XMLSerializer sax2xml = new XMLSerializer(baos, false);
    sax2xml.setOutputProperty(OutputKeys.VERSION, "1.1");
    ContentHandler ch = sax2xml.getContentHandler();    
    ch.startDocument();
    char[] data = new char[] {32, 33, 5, 34};
    
    ch.startElement("","foo","foo", new AttributesImpl());
    boolean eh = false;
    try {
      ch.characters(data, 0, 4);
    } catch (SAXParseException e) {
      eh = true;
    }  
    assertFalse(eh);
  }

  public void testXml11Error2() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    XMLSerializer sax2xml = new XMLSerializer(baos, false);
    sax2xml.setOutputProperty(OutputKeys.VERSION, "1.1");
    ContentHandler ch = sax2xml.getContentHandler();    
    ch.startDocument();
    char[] data = new char[] {32, 33, 0, 34};
    
    ch.startElement("","foo","foo", new AttributesImpl());
    boolean eh = false;
    try {
      ch.characters(data, 0, 4);
    } catch (SAXParseException e) {
      String msg = e.getMessage();
      System.out.println(msg);
      String expected = "Trying to serialize non-XML 1.1 character: " + "0x0 at offset 2";
      assertEquals(msg.substring(0, expected.length()), expected);
      eh = true;
    }  
    assertTrue(eh);
  }


}
