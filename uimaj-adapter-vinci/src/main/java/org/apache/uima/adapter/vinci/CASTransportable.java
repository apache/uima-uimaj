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

package org.apache.uima.adapter.vinci;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.adapter.vinci.util.Constants;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.OutOfTypeSystemData;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.util.CasPool;
import org.apache.uima.util.Level;
import org.apache.vinci.debug.Debug;
import org.apache.vinci.transport.FrameLeaf;
import org.apache.vinci.transport.KeyValuePair;
import org.apache.vinci.transport.TransportConstants;
import org.apache.vinci.transport.Transportable;
import org.apache.vinci.transport.VinciFrame;
import org.apache.vinci.transport.XTalkTransporter;
import org.apache.vinci.transport.document.XTalkToSAX;

public class CASTransportable extends DefaultHandler implements Transportable {
  private CasPool myCasPool;

  private CAS myCas;

  private byte[] mybuf = new byte[512]; // temporary work buffer

  private OutOfTypeSystemData outOfTypeSystemData;

  private boolean incomingCommand, incomingError, incomingExtraData;

  private String lastqName;

  private String command;

  private String error;

  private int ready;

  private ContentHandler handler;

  private VinciFrame extraDataFrame;

  public UimaContext uimaContext; // needed for sofa mappings

  public boolean includeDocText;

  public boolean ignoreResponse = false; // for performance testing only.

  /**
   * This constructor is used on the service side - a CAS Pool reference is provided. We don't check
   * a CAS out of the pool until we get a request.
   * 
   * @param casPool
   * @param outOfTypeSystemData
   * @param uimaContext
   * @param includeDocText
   */
  public CASTransportable(CasPool casPool, OutOfTypeSystemData outOfTypeSystemData,
                  UimaContext uimaContext, boolean includeDocText) {
    // Debug.p("Creating new CASTransportable.");
    this.myCasPool = casPool;
    this.myCas = null;
    this.outOfTypeSystemData = outOfTypeSystemData;
    this.uimaContext = uimaContext;
    this.extraDataFrame = new VinciFrame();
    this.includeDocText = includeDocText;
  }

  /**
   * This constructor is used on the client side, where we have a dedicated CAS instance for the
   * request.
   * 
   * @param cas
   * @param outOfTypeSystemData
   * @param uimaContext
   * @param includeDocText
   */
  public CASTransportable(CAS cas, OutOfTypeSystemData outOfTypeSystemData,
                  UimaContext uimaContext, boolean includeDocText) {
    // Debug.p("Creating new CASTransportable.");
    this.myCas = cas;
    this.myCasPool = null;
    this.outOfTypeSystemData = outOfTypeSystemData;
    this.uimaContext = uimaContext;
    this.extraDataFrame = new VinciFrame();
    this.includeDocText = includeDocText;
  }

  public VinciFrame getExtraDataFrame() {
    return extraDataFrame;
  }

  public OutOfTypeSystemData getOutOfTypeSystemData() {
    return this.outOfTypeSystemData;
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public CAS getCas() {
    return myCas;
  }

  /**
   * This nested class handles serializing the CAS to XTalk through events provided by an
   * XCASSerializer.
   */
  class XTalkSerializer extends DefaultHandler {
    OutputStream os;

    XCASSerializer serializer;

    boolean started;

    XTalkSerializer(OutputStream os, XCASSerializer s) {
      this.os = os;
      this.serializer = s;
    }

    public void startDocument() throws SAXException {
      try {
        os.write(XTalkTransporter.HEADER);
        XTalkTransporter.stringToBin("vinci:FRAME", os, mybuf);
        XTalkTransporter.writeInt(0, os); // no attributes
        if (command == null) {
          XTalkTransporter.writeInt(1, os); // 1 child (DATA)
        } else {
          XTalkTransporter.writeInt(2, os); // 2 children (vinci:COMMAND & DATA)
          // Write the vinci:COMMAND
          os.write(XTalkTransporter.ELEMENT_MARKER);
          XTalkTransporter.stringToBin(TransportConstants.COMMAND_KEY, os, mybuf);
          XTalkTransporter.writeInt(0, os); // no attributes
          XTalkTransporter.writeInt(1, os); // 1 child (pcdata)
          os.write(XTalkTransporter.STRING_MARKER);
          XTalkTransporter.stringToBin(command, os, mybuf);
        }
        // write the DATA sub-frame header
        os.write(XTalkTransporter.ELEMENT_MARKER);
        XTalkTransporter.stringToBin("DATA", os, mybuf);
        XTalkTransporter.writeInt(0, os); // no attributes
        int children = 1 + extraDataFrame.getKeyValuePairCount();
        XTalkTransporter.writeInt(children, os); // 1 child (KEYS) + extra data fields...
        started = false; // triggers first startElement() call to write "KEYS" instead of "CAS"
        // Write extra data...
        for (int i = 0; i < extraDataFrame.getKeyValuePairCount(); i++) {
          KeyValuePair k = extraDataFrame.getKeyValuePair(i);
          os.write(XTalkTransporter.ELEMENT_MARKER);
          XTalkTransporter.stringToBin(k.getKey(), os, mybuf);
          XTalkTransporter.writeInt(0, os); // no attributes
          XTalkTransporter.writeInt(1, os); // 1 child (pcdata)
          os.write(XTalkTransporter.STRING_MARKER);
          XTalkTransporter.stringToBin(k.getValueAsString(), os, mybuf);
        }
      } catch (IOException e) {
        throw new SAXException("IOException: " + e);
      }
    }

    void attributesToXTalk(org.xml.sax.Attributes attributes) throws IOException {
      int size = attributes.getLength();
      XTalkTransporter.writeInt(size, os);
      // Debug.p("Serializing " + size + " attributes.");
      for (int i = 0; i < size; i++) {
        XTalkTransporter.stringToBin(attributes.getQName(i), os, mybuf);
        XTalkTransporter.stringToBin(attributes.getValue(i), os, mybuf);
      }
    }

    public void endElement(String uri, String name, String qName) throws SAXException {
      // Debug only
      // Debug.p("Ending element: " + qName);
    }

    public void startElement(String uri, String name, String qName, org.xml.sax.Attributes atts)
                    throws SAXException {
      try {
        // Debug.p("Start element: " + qName + " : " + serializer.getNumChildren());
        os.write(XTalkTransporter.ELEMENT_MARKER);
        if (!started) {
          Debug.Assert(XCASSerializer.casTagName.equals(qName));
          started = true;
          // for some reason we have to replace "CAS" with "KEYS" as the CAS root tag.
          XTalkTransporter.stringToBin(Constants.KEYS, os);
          started = true;
        } else {
          XTalkTransporter.stringToBin(qName, os);
        }
        attributesToXTalk(atts);
        XTalkTransporter.writeInt(serializer.getNumChildren(), os); // HACK to find out # of
                                                                    // children
      } catch (IOException e) {
        throw new SAXException("IOException: " + e);
      }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
      // Debug.p("Chars: " + new String(ch, start, length));
      try {
        os.write(XTalkTransporter.STRING_MARKER);
        XTalkTransporter.stringToBin(ch, start, length, os, mybuf);
      } catch (IOException e) {
        throw new SAXException("IOException: " + e);
      }
    }
  }

  public KeyValuePair fromStream(InputStream is) throws IOException {
    // Debug.p("CASTransportable.fromStream");
    boolean done = false;
    try {
      XTalkToSAX converter = new XTalkToSAX();
      // Debug.p("parsing...");
      converter.parse(is, this);
      // Debug.p("...done parsing.");
      done = true;
    } catch (SAXException e) {
      throw new IOException("Unexpected SAXException: " + e);
    } finally {
      if (!done) {
        cleanup(); // release the cas back to the pool if we didn't parse successfully.
      }
    }
    if (error != null) {
      return new KeyValuePair(TransportConstants.ERROR_KEY, new FrameLeaf(error));
    }
    // Debug.p("Testing: " + extraDataFrame.toXML());
    return null;
  }

  /**
   * Serialize the CAS to the stream in XTalk format. After serialization is complete the cas is
   * returned to the pool (if it was allocated from a pool.)
   */
  public void toStream(OutputStream os) throws IOException {
    try {
      UIMAFramework.getLogger().log(Level.FINEST, "Serializing CAS.");
      XCASSerializer xcasSerializer = new XCASSerializer(myCas.getTypeSystem(), this.uimaContext);
      // Not sure why we need to do the next two lines:
      xcasSerializer.setDocumentTypeName(Constants.VINCI_DETAG);
      xcasSerializer.setDocumentTextFeature(null);
      XTalkSerializer s = new XTalkSerializer(os, xcasSerializer);
      try {
        xcasSerializer.serialize(myCas, s, includeDocText, outOfTypeSystemData);
      } catch (org.xml.sax.SAXException e) {
        throw new IOException("SAX Exception: " + e);
      }
      UIMAFramework.getLogger().log(Level.FINEST, "CAS Serialization Complete.");
    } catch (IOException e) {
      UIMAFramework.getLogger().log(Level.WARNING, e.getMessage(), e);
      throw e;
    } catch (RuntimeException e) {
      UIMAFramework.getLogger().log(Level.WARNING, e.getMessage(), e);
      throw e;
    } finally {
      if (myCasPool != null) {
        myCasPool.releaseCas(myCas);
        myCas = null;
        UIMAFramework.getLogger().log(Level.FINEST, "Released CAS back to pool.");
      }
    }
  }

  public void cleanup() {
    if (myCas != null && myCasPool != null) {
      myCasPool.releaseCas(myCas);
      myCas = null;
    }
  }

  protected void finalize() {
    // Though unlikely, there could be unusual cases where
    // toStream is not ever invoked, so in these cases this
    // finalizer will ensure the cas is returned to the pool to
    // avoid a leak.
    if (myCas != null && myCasPool != null) {
      Debug.p("WARNING: releasing cas in finalizer.");
      myCasPool.releaseCas(myCas);
    }
  }

  public void startElement(String uri, String name, String qName, org.xml.sax.Attributes atts)
                  throws SAXException {
    // Debug.p("Start element: " + qName);
    if (ready > 0) {
      handler.startElement(uri, name, qName, atts);
    } else {
      if (TransportConstants.COMMAND_KEY.equals(qName)) {
        incomingCommand = true;
      } else if (TransportConstants.ERROR_KEY.equals(qName) || "Error".equals(qName)) {
        incomingError = true;
      } else {
        lastqName = qName;
        incomingExtraData = true;
      }
    }
    if (Constants.KEYS.equals(qName)) {
      ready++;
    }
  }

  public void endElement(String uri, String name, String qName) throws SAXException {
    // Debug.p("End element: " + qName);
    if (Constants.KEYS.equals(qName)) {
      ready--;
    }
    if (ready > 0) {
      handler.endElement(uri, name, qName);
    }
  }

  public void characters(char[] ch, int start, int length) throws SAXException {
    // Debug.p("characters: " + new String(ch, start, length) + " : " + incomingCommand);
    if (ready > 0) {
      handler.characters(ch, start, length);
    } else if (incomingCommand) {
      command = new String(ch, start, length);
      incomingCommand = false;
    } else if (incomingError) {
      error = new String(ch, start, length);
      incomingError = false;
    } else if (incomingExtraData) {
      extraDataFrame.fadd(lastqName, new String(ch, start, length));
      incomingExtraData = false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.helpers.DefaultHandler#startDocument()
   */
  public void startDocument() throws SAXException {
    // this gets called from the XTalkToSax parser after it confirms
    // that there is data to be read from the socket. So this is where we
    // need to grab a CAS from the CasPool and initialize the XCASDeserializer.
    if (myCas == null) {
      myCas = myCasPool.getCas(0);
    }
    myCas.reset();
    XCASDeserializer deser = new XCASDeserializer(myCas.getTypeSystem(), this.uimaContext);
    deser.setDocumentTypeName("Detag:DetagContent");
    if (!ignoreResponse) {
      handler = deser.getXCASHandler(myCas, outOfTypeSystemData);
    } else {
      handler = new DefaultHandler();
    }
    handler.startDocument();
    handler.startElement("", "CAS", "CAS", null);
    this.ready = 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.helpers.DefaultHandler#endDocument()
   */
  public void endDocument() throws SAXException {
    handler.endElement("", "CAS", "CAS");
    handler.endDocument();
  }
}
