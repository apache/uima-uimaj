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
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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
   * @param casPool the cas pool
   * @param outOfTypeSystemData the out of type system data
   * @param uimaContext the uima context
   * @param includeDocText the include doc text
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
   * @param cas the cas
   * @param outOfTypeSystemData the out of type system data
   * @param uimaContext the uima context
   * @param includeDocText the include doc text
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

  /**
   * Gets the extra data frame.
   *
   * @return the extra data frame
   */
  public VinciFrame getExtraDataFrame() {
    return extraDataFrame;
  }

  /**
   * Gets the out of type system data.
   *
   * @return the out of type system data
   */
  public OutOfTypeSystemData getOutOfTypeSystemData() {
    return this.outOfTypeSystemData;
  }

  /**
   * Gets the command.
   *
   * @return the command
   */
  public String getCommand() {
    return command;
  }

  /**
   * Sets the command.
   *
   * @param command the new command
   */
  public void setCommand(String command) {
    this.command = command;
  }

  /**
   * Gets the cas.
   *
   * @return the cas
   */
  public CAS getCas() {
    return myCas;
  }

  /**
   * This nested class handles serializing the CAS to XTalk through events provided by an
   * XCASSerializer.
   */
  class XTalkSerializer extends DefaultHandler {
    
    /** The os. */
    OutputStream os;

    XCASSerializer serializer;

    boolean started;

    /**
     * Instantiates a new x talk serializer.
     *
     * @param os the os
     * @param s the s
     */
    XTalkSerializer(OutputStream os, XCASSerializer s) {
      this.os = os;
      this.serializer = s;
    }

    /* (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#startDocument()
     */
    @Override
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
        throw wrapAsSAXException(e);
      }
    }

    /**
     * Attributes to X talk.
     *
     * @param attributes the attributes
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void attributesToXTalk(org.xml.sax.Attributes attributes) throws IOException {
      int size = attributes.getLength();
      XTalkTransporter.writeInt(size, os);
      // Debug.p("Serializing " + size + " attributes.");
      for (int i = 0; i < size; i++) {
        XTalkTransporter.stringToBin(attributes.getQName(i), os, mybuf);
        XTalkTransporter.stringToBin(attributes.getValue(i), os, mybuf);
      }
    }

    /* (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(String uri, String name, String qName) throws SAXException {
      // Debug only
      // Debug.p("Ending element: " + qName);
    }

    /* (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
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
        throw wrapAsSAXException(e);
      }
    }

    /* (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      // Debug.p("Chars: " + new String(ch, start, length));
      try {
        os.write(XTalkTransporter.STRING_MARKER);
        XTalkTransporter.stringToBin(ch, start, length, os, mybuf);
      } catch (IOException e) {
        throw wrapAsSAXException(e);
      }
    }
  }

  /* (non-Javadoc)
   * @see org.apache.vinci.transport.Transportable#fromStream(java.io.InputStream)
   */
  @Override
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
      //if SAXException wraps an IOException, throw the IOException.  This is
      //important since different types of IOExceptions (e.g. SocketTimeoutExceptions)
      //are treated differently by Vinci
      throw convertToIOException(e);
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
   *
   * @param os the os
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
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
        //if SAXException wraps an IOException, throw the IOException.  This is
        //important since different types of IOExceptions (e.g. SocketTimeoutExceptions)
        //are treated differently by Vinci
        throw convertToIOException(e);
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

  /* (non-Javadoc)
   * @see java.lang.Object#finalize()
   */
  @Override
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

  /* (non-Javadoc)
   * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
   */
  @Override
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
      // the data inside the KEYS element is the contents of an incoming CAS.
      // So this is where we need to grab a CAS from the CasPool and initialize 
      //the XCASDeserializer.
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
      //set the ready flag to indicate that following elements are CAS data
      ready++;
    }
  }

  /* (non-Javadoc)
   * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public void endElement(String uri, String name, String qName) throws SAXException {
    // Debug.p("End element: " + qName);
    if (Constants.KEYS.equals(qName)) {
      ready--;
      if (ready == 0) {
        handler.endElement("", "CAS", "CAS");
        handler.endDocument();
      }
    }
    if (ready > 0) {
      handler.endElement(uri, name, qName);
    }
  }

  /* (non-Javadoc)
   * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
   */
  @Override
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
  @Override
  public void startDocument() throws SAXException {
    this.ready = 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.helpers.DefaultHandler#endDocument()
   */
  @Override
  public void endDocument() throws SAXException {
  }
  
  /**
   * Create a SAXException that wraps the given IOException.
   * The wrapping is done using the standard Java 1.4 mechanism, 
   * so that getCause() will work.  Note that new SAXException(Exception) 
   * does NOT work.
   * @param e an IOException to wrap
   * @return a SAX exception for which <code>getCause()</code> will return <code>e</code>.
   */
  public SAXException wrapAsSAXException(IOException e) {
    SAXException saxEx =new SAXException(e.getMessage());
    saxEx.initCause(e);
    return saxEx;
  }
  
  /**
   * Converts a Throwable to an IOException.  If <code>t</code> is an IOException,
   * then <code>t</code> is returned.  If not, then if <code>t</code> was caused
   * by an IOException (directly or indirectly), then that IOException is returned.
   * Otherwise, a new IOException is created which wraps (is caused by) <code>t</code>.
   * @param t the throwable to convert
   * @return an IOException which is either t, one of the causes of t, or a new IOException
   *   that wraps t.
   */
  private IOException convertToIOException(Throwable t) {
   //if t is itself an IOException, just return it
   if (t instanceof IOException) {
     return (IOException)t;
   }
   
   //search for a cause that is an IOException.  If one is found, return that.
   Throwable cause = t.getCause();
   while (cause != null) {
     if (cause instanceof IOException) {
       return (IOException)cause;
     }
     cause = cause.getCause();
   }
   
   //otherwise, wrap t in a new IOException
   IOException ioex = new IOException();
   ioex.initCause(t);
   return ioex;
  }  
}
