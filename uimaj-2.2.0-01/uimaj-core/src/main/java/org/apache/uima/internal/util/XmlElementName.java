package org.apache.uima.internal.util;

/**
 * Data structure used to encapsulate the different pieces of information that 
 * make up the name of an XML element - namely, the Namespace URI, the local 
 * name, and the qname (qualified name).
 */
public class XmlElementName {
  public XmlElementName(String nsUri, String localName, String qName) {
    this.nsUri = nsUri;
    this.localName = localName;
    this.qName = qName;
  }

  public String nsUri;

  public String localName;

  public String qName;
}