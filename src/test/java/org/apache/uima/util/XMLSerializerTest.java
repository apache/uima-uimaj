package org.apache.uima.util;

import java.io.ByteArrayOutputStream;

import javax.xml.transform.OutputKeys;

import junit.framework.TestCase;

import org.xml.sax.ContentHandler;
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
    assertEquals("<?xml version=\"1.1\" encoding=\"UTF-8\"?><foo/>", xmlStr);
  }
}
