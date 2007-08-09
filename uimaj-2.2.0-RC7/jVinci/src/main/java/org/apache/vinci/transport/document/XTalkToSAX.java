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

package org.apache.vinci.transport.document;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import org.apache.vinci.transport.XTalkTransporter;

/**
 * Class for converting XTalk streams into SAX events.
 */
public class XTalkToSAX {

  public static final int INITIAL_BUF_SIZE = 256;

  private static final String cdataType = "CDATA";

  private char[] charBuffer;

  private byte[] byteBuffer;

  private AttributesImpl workAttributes;

  // members initialzed by parse() to reduce argument passing.
  private InputStream is;

  private ContentHandler handler;

  public XTalkToSAX() {
    init(INITIAL_BUF_SIZE);
  }

  public XTalkToSAX(int bufSize) {
    init(bufSize);
  }

  private void init(int bufSize) {
    this.workAttributes = new AttributesImpl();
    this.byteBuffer = new byte[bufSize];
    this.charBuffer = new char[bufSize];
  }

  /**
   * Initially, the XTalkToSAX processor creates a byte buffer and char buffer of size
   * INITIAL_BUF_SIZE. These buffer may grow during parsing to handle very large strings. Users can
   * determine the size of these arrays with this method. This method in conjunction with
   * resetBuffers lets application implement their own buffer management. Buffers can be reset
   * during parsing, but not from another thread.
   */
  public int bufferSize() {
    return byteBuffer.length;
  }

  /**
   * Resets buffers to their initial size... this is useful because buffers can grow during parsing
   * and this allows the space to be reclaimed without having to undo references to the parser
   * object.
   */
  public void resizeBuffers(int toSize) {
    if (this.byteBuffer.length != toSize) {
      this.byteBuffer = new byte[toSize];
      this.charBuffer = new char[toSize];
    }
  }

  /**
   * Parse one document off of the incoming XTalk stream into SAX events. A side effect of parsing
   * is that internal arrays will grow to the size of the largest character string encountered in
   * the document. Use bufferSize() and resizeBuffers to manage memory in applications where very
   * large strings may be encountered and the same object is used to parse many incoming documents.
   * 
   * @throws IOException
   *           if underlying IOException from the stream or if XTalk format is invalid.
   * @throws SAXException
   *           if SAXException thrown by the handler
   * 
   * @pre handler != null
   * @pre is != null
   */
  public void parse(InputStream is, ContentHandler handler) throws IOException, SAXException {
    this.is = is;
    this.handler = handler;
    try {
      int marker = is.read();
      if (marker == -1) {
        throw new EOFException();
      }
      if ((byte) marker != XTalkTransporter.DOCUMENT_MARKER) {
        throw new IOException("Expected document marker: " + (char) marker);
      }
      int version = is.read();
      if ((byte) version != XTalkTransporter.VERSION_CODE) {
        throw new IOException("Xtalk version code doesn't match "
                + (int) XTalkTransporter.VERSION_CODE + ": " + version);
      }
      handler.startDocument();
      doTopLevelParse();
      handler.endDocument();
    } finally {
      // nullify refs to allow GC
      is = null;
      handler = null;
    }
  }

  private void doTopLevelParse() throws IOException, SAXException {
    int top_field_count = XTalkTransporter.readInt(is);
    // Skip over intro PI's.
    int marker;
    if (top_field_count < 1) {
      throw new IOException("No top level element.");
    }
    while ((marker = is.read()) == XTalkTransporter.PI_MARKER) {
      String target = consumeString();
      String data = consumeString();
      handler.processingInstruction(target, data);
      top_field_count--;
      if (top_field_count < 1) {
        throw new IOException("No top level element.");
      }
    }
    if ((byte) marker != XTalkTransporter.ELEMENT_MARKER) {
      throw new IOException("Expected element marker: " + (char) marker);
    }
    doElement();
    top_field_count--;
    // Handle trailing PI's
    while (top_field_count > 0) {
      if (is.read() != XTalkTransporter.PI_MARKER) {
        throw new IOException("Expected PI marker.");
      }
      doProcessingInstruction();
      top_field_count--;
    }
  }

  private void doProcessingInstruction() throws IOException, SAXException {
    String target = consumeString();
    String data = consumeString();
    handler.processingInstruction(target, data);
  }

  private void ensureCapacity(int bytesToRead) {
    if (byteBuffer.length < bytesToRead) {
      byteBuffer = new byte[byteBuffer.length + bytesToRead];
      charBuffer = new char[charBuffer.length + bytesToRead];
    }
  }

  private String consumeString() throws IOException {
    int bytesToRead = XTalkTransporter.readInt(is);
    ensureCapacity(bytesToRead);
    int charsRead = XTalkTransporter.consumeCharacters(is, byteBuffer, charBuffer, bytesToRead);
    return new String(charBuffer, 0, charsRead);
  }

  private void doElement() throws IOException, SAXException {
    // Parse an incoming element.
    String tagName = consumeString();
    int attribute_count = XTalkTransporter.readInt(is);
    workAttributes.clear();
    for (int i = 0; i < attribute_count; i++) {
      String attrName = consumeString();
      String attrValue = consumeString();
      workAttributes.addAttribute(null, null, attrName, cdataType, attrValue);
    }
    handler.startElement(null, null, tagName, workAttributes);
    int field_count = XTalkTransporter.readInt(is);
    for (int i = 0; i < field_count; i++) {
      int marker = is.read();
      switch ((byte) marker) {
        case XTalkTransporter.PI_MARKER:
          doProcessingInstruction();
          break;
        case XTalkTransporter.STRING_MARKER:
          int bytesToRead = XTalkTransporter.readInt(is);
          ensureCapacity(bytesToRead);
          int charsRead = XTalkTransporter.consumeCharacters(is, byteBuffer, charBuffer,
                  bytesToRead);
          handler.characters(charBuffer, 0, charsRead);
          break;
        case XTalkTransporter.ELEMENT_MARKER:
          doElement();
          break;
        default:
          throw new IOException("Unexpected marker: " + (char) marker);
      }
    }
    handler.endElement(null, null, tagName);
  }

}
