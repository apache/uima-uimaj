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

import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.internal.util.XMLUtils;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;

/**
 * Utility class that generates XML output from SAX events or DOM nodes.
 */
public class XMLSerializer {

  // See Javadocs for javax.xml.transform.TransformerFactory for details on how the TransformerFactory is found:
  //  the class specified in the system property: javax.xml.transform.TransformerFactory 
  //  or the value of this property in <jre>/lib/jaxp.properties
  //  or the class found in any jar that has an entry: META-INF/service/javax.xml.transform.TransformerFactory
  //  or a platform default.
  
  private static final SAXTransformerFactory transformerFactory = XMLUtils.createSaxTransformerFactory();

  private TransformerHandler mHandler;

  private Transformer mTransformer;

  private OutputStream mOutputStream;
  private Writer mWriter;
  
  public XMLSerializer() {
    this(true);
  }

  public XMLSerializer(boolean isFormattedOutput) {
    try {
      mHandler = transformerFactory.newTransformerHandler();
      mTransformer = mHandler.getTransformer();

      if (isFormattedOutput) {
        // set default output format
        mTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
        mTransformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        mTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        //   Saxon appears to ignore the above and use a default of 3 unless the following is used
        //mTransformer.setOutputProperty("{http://saxon.sf.net/}indent-spaces", "4");
        //   But this fails on Saxon9-HE with:
        //     net.sf.saxon.trans.LicenseException: Requested feature (custom serialization) requires Saxon-PE
        mTransformer.setOutputProperty(OutputKeys.METHOD, "xml");
      }

    } catch (TransformerConfigurationException e) {
      throw new UIMARuntimeException(e);
    }
  }

  public void setIndent(boolean yes) {
    mTransformer.setOutputProperty(OutputKeys.INDENT, yes ? "yes" : "no");
  }
  
  public XMLSerializer(OutputStream aOutputStream) {
    this();
    setOutputStream(aOutputStream);
  }

  public XMLSerializer(OutputStream aOutputStream, boolean isFormattedOutput) {
    this(isFormattedOutput);
    setOutputStream(aOutputStream);
  }

  public XMLSerializer(Writer aWriter) {
    this();
    setWriter(aWriter);
  }

  public XMLSerializer(Writer aWriter, boolean isFormattedOutput) {
    this(isFormattedOutput);
    setWriter(aWriter);
  }

  public void setOutputStream(OutputStream aOutputStream) {
    mWriter = null;
    mOutputStream = aOutputStream;
    mHandler.setResult(createSaxResultObject());
  }

  public void setWriter(Writer aWriter) {
    mOutputStream = null;
    mWriter = aWriter;
    mHandler.setResult(createSaxResultObject());
  }

  public ContentHandler getContentHandler() {
    String xmlVer = mTransformer.getOutputProperty(OutputKeys.VERSION);
    boolean xml10 = xmlVer == null || "1.0".equals(xmlVer);
    return new CharacterValidatingContentHandler(!xml10, mHandler);
  }

  private Result createSaxResultObject() {
    if (mOutputStream != null) {
      return new StreamResult(mOutputStream);
    } else if (mWriter != null) {
      return new StreamResult(mWriter); 
    } else {
      return null;
    }
  }

  public void serialize(Node node) {
    try {
      mTransformer.transform(new DOMSource(node), createSaxResultObject());
    } catch (TransformerException e) {
      throw new UIMARuntimeException(e);
    }
  }

  public void dom2sax(Node node, ContentHandler handler) {
    try {
      mTransformer.transform(new DOMSource(node), new SAXResult(handler));
    } catch (TransformerException e) {
      throw new UIMARuntimeException(e);
    }
  }

  public void setOutputProperty(String name, String value) {
    try {
      mTransformer.setOutputProperty(name, value);
    } catch (IllegalArgumentException e) {
      throw new UIMARuntimeException(e);
    }
    //re-create the Result object when properties change.  This fixes bug UIMA-1859 where setting the XML version was
    //not reflected in the output.
    Result result = createSaxResultObject();
    if (result != null) {
      mHandler.setResult(result);
    }
  }  
  
  /**
   * This class wraps the standard content handler
   */
  public class CharacterValidatingContentHandler implements ContentHandler, LexicalHandler {
    ContentHandler mHandler;  // the wrapped handler
    boolean mXml11;           
    
    private int indent = 0;  // tracks indentation for nicely indented output
    
    public int getIndent() {
      return indent;
    }
    
    public int nextIndent() {
      indent += indentDelta;
//    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
//    
//    System.out.format("++++++++ %3d %26s %4d <- %26s %4d <- %26s %4d <- %26s %4d%n",
//        indent, 
//        stackTraceElements[3].getMethodName(), 
//        stackTraceElements[3].getLineNumber(),
//        stackTraceElements[4].getMethodName(), 
//        stackTraceElements[4].getLineNumber(),
//        stackTraceElements[5].getMethodName(), 
//        stackTraceElements[5].getLineNumber(), 
//        stackTraceElements[6].getMethodName(), 
//        stackTraceElements[6].getLineNumber());
      return indent;
    }

    public int prevIndent() {
      indent -= indentDelta;
//      if (indent < 0) {
//        indent = 0;
//      }
//      StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
//      System.out.format("-------- %3d %26s %4d <- %26s %4d <- %26s %4d <- %26s %4d%n",
//          indent, 
//          stackTraceElements[3].getMethodName(), 
//          stackTraceElements[3].getLineNumber(),
//          stackTraceElements[4].getMethodName(), 
//          stackTraceElements[4].getLineNumber(),
//          stackTraceElements[5].getMethodName(), 
//          stackTraceElements[5].getLineNumber(), 
//          stackTraceElements[6].getMethodName(), 
//          stackTraceElements[6].getLineNumber());
      return indent;
    }
    
    private int indentDelta = 0;  // set to positive # to indent each level
    
    public int getIndentDelta() {
      return indentDelta;
    }

    public void setIndentDelta(int indentDelta) {
      this.indentDelta = indentDelta;
    }

    private List<Node> mLastOutputNode = new ArrayList<Node>();  // the last output node for repeated subelement nodes
    
    public void lastOutputNodeAddLevel() {
      mLastOutputNode.add(null);
    }
    
    public void setLastOutputNode(Node n) {
      mLastOutputNode.set(mLastOutputNode.size() -1, n);
    }

    public Node getLastOutputNode() {
      return mLastOutputNode.get(mLastOutputNode.size() -1);
    }
    
    public Node getLastOutputNodePrevLevel() {
      int lastIndex = mLastOutputNode.size() -1;
      if (lastIndex > 0) {
        return mLastOutputNode.get(lastIndex - 1); 
      }
      return null;
    }
    
    public void lastOutputNodeClearLevel() {
      mLastOutputNode.remove(mLastOutputNode.size() -1);
    }
    
    public boolean prevWasEndElement = false;
    
    public boolean prevNL = false;
    
    CharacterValidatingContentHandler(boolean xml11, ContentHandler serializerHandler) {
      mHandler = serializerHandler;  
      mXml11 = xml11;
      String indentDeltaString = mTransformer.getOutputProperty("{http://xml.apache.org/xslt}indent-amount");
      if (null != indentDeltaString) {
        try {
          indentDelta = Integer.parseInt(indentDeltaString);
        } catch (NumberFormatException e) {
          indentDelta = 0;
        }
      }
      mLastOutputNode.add(null);
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
      for (int i = 0; i < atts.getLength(); i++) {
        String val = atts.getValue(i);
        checkForInvalidXmlChars(val, mXml11);
      }
      mHandler.startElement(uri, localName, qName, atts); 
      prevWasEndElement = false;
      prevNL = false;
    }
    
    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] ch, int start, int length) throws SAXException {
      checkForInvalidXmlChars(ch, start, length, mXml11);
      mHandler.characters(ch, start, length);
      prevNL = false;
      for (int i = start; i < start + length; i++) {
        if (ch[i] == '\n') {
          prevNL = true;
          break;
        }
      }
//      nlOK = false;  //unfortunately, non validating dom parsers can't detect ignorable whitespace,
      // so they use characters instead...
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#endDocument()
     */
    public void endDocument() throws SAXException {
      mHandler.endDocument();
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    public void endElement(String uri, String localName, String qName) throws SAXException {
      mHandler.endElement(uri, localName, qName);
      prevWasEndElement = true;
      prevNL = false;
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
     */
    public void endPrefixMapping(String prefix) throws SAXException {
      mHandler.endPrefixMapping(prefix);
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
     */
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
      mHandler.ignorableWhitespace(ch, start, length);
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String, java.lang.String)
     */
    public void processingInstruction(String target, String data) throws SAXException {
      mHandler.processingInstruction(target, data);
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
     */
    public void setDocumentLocator(Locator locator) {
      mHandler.setDocumentLocator(locator);
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
     */
    public void skippedEntity(String name) throws SAXException {
      mHandler.skippedEntity(name);
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#startDocument()
     */
    public void startDocument() throws SAXException {
      indent = 0;
      mHandler.startDocument();
    }

    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
     */
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
      mHandler.startPrefixMapping(prefix, uri);
    }
    
    private final void checkForInvalidXmlChars(String s, boolean xml11) throws SAXParseException {
      final int index = XMLUtils.checkForNonXmlCharacters(s, xml11);
      if (index >= 0) {
        String startStr = (index == 0) 
            ? "[The Very First Character]"
            : s.substring(0, Math.min(index, Math.min(100,  s.length())));
        String msg =  String.format("Trying to serialize non-XML %s character: 0x%x at offset %,d in string starting with %s",
                                      (xml11 ? "1.1" : "1.0"),
//                                    s.charAt(index),  // don't try to output this, causes problems with other tooling 
                                      (int)s.charAt(index),
                                      index,
                                      startStr); 
        throw new SAXParseException(msg, null);
      }
    }
    
    private final void checkForInvalidXmlChars(char[] ch, int start, int length, boolean xml11) throws SAXParseException {
      final int index = XMLUtils.checkForNonXmlCharacters(ch, start, length, xml11);
      if (index >= 0) {
        String startStr = (index == 0) 
            ? "[The Very First Character]"
            : new String(ch).substring(0, Math.min(index, Math.min(100,  ch.length)));
        String msg =  String.format("Trying to serialize non-XML %s character: 0x%x at offset %,d in string starting with %s",
            (xml11 ? "1.1" : "1.0"),
//            ch[index],  // don't try to output this, causes problems with other tooling  
            (int)(ch[index]),
            index,
            startStr); 
        throw new SAXParseException(msg, null);
      }
    }

    public void comment(char[] ch, int start, int length) throws SAXException {
      ((LexicalHandler)mHandler).comment(ch, start, length);
      prevNL = false;
    }

    public void endCDATA() throws SAXException {}
    public void endDTD() throws SAXException {}
    public void endEntity(String arg0) throws SAXException {}
    public void startCDATA() throws SAXException {}
    public void startDTD(String arg0, String arg1, String arg2) throws SAXException {}
    public void startEntity(String arg0) throws SAXException {}    
  }
}
