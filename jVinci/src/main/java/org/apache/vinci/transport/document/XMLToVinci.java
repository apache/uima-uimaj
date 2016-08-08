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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.vinci.debug.Debug;
import org.apache.vinci.transport.FrameComponent;
import org.apache.vinci.transport.FrameLeaf;
import org.apache.vinci.transport.ServiceException;
import org.apache.vinci.transport.TransportConstants;
import org.apache.vinci.transport.Transportable;
import org.apache.vinci.transport.VinciFrame;
import org.apache.vinci.transport.util.TransportableConverter;

/**
 * Class for parsing an XML document and representing it using any of the various jVinci-compatible
 * document models.
 * 
 */
public class XMLToVinci {

  /**
   * Utility class not intended to be instantiated.
   */
  private XMLToVinci() {
  }

  private static class StackEntry {
    List sub_entries = null;

    StackEntry parent = null;

    String ename_or_pcdata;

    FrameComponent component = null;
  }

  private static class AStackEntry extends StackEntry {
    org.apache.vinci.transport.Attributes attributes = null;
  }

  /**
   * This is a SAX document handler to parse XML into VinciFrames.
   */
  private static class VinciFrameHandler extends DefaultHandler {
    protected StackEntry top = null;

    public void startElement(String uri, String name, String qName, org.xml.sax.Attributes atts) {
      StackEntry entry = new StackEntry();
      entry.ename_or_pcdata = qName;
      entry.sub_entries = new ArrayList(2);
      if (top == null) {
        top = entry;
      } else {
        top.sub_entries.add(entry);
        entry.parent = top;
        top = entry;
      }
    }

    /**
     * @pre top != null
     * @pre top.sub_entries != null
     */
    public void endElement(String uri, String name, String qName) {
      // Debug.p("End element: " + qName);
      if (top.sub_entries.size() == 0) {
        // Debug.p("Empty element.");
        top.component = new VinciFrame();
      } else {
        StackEntry child = (StackEntry) top.sub_entries.get(0);
        // Debug.p("Frameleaf.");
        if (top.sub_entries.size() == 1 && child.sub_entries == null) {
          // This is a frameleaf
          top.component = new FrameLeaf(child.ename_or_pcdata);
        } else {
          VinciFrame c = new VinciFrame();
          top.component = c;
          for (int i = 0; i < top.sub_entries.size(); i++) {
            child = (StackEntry) top.sub_entries.get(i);
            if (child.component != null) {
              c.add(child.ename_or_pcdata, child.component);
            } else {
              c.fadd(TransportConstants.PCDATA_KEY, child.ename_or_pcdata);
            }
            child.sub_entries = null;
          }
        }
      }
      // top.sub_entries = null;
      if (top.parent != null) {
        top = top.parent;
      }
    }

    /**
     * @pre ch != null
     * @pre start &lt; ch.length
     * @pre length &ge; start
     * @pre top != null
     * @pre top.sub_entries != null
     */
    public void characters(char[] ch, int start, int length) {
      if (top.sub_entries.size() != 0) {
        StackEntry entry = (StackEntry) top.sub_entries.get(top.sub_entries.size() - 1);
        if (entry.sub_entries == null) {
          entry.ename_or_pcdata += new String(ch, start, length);
          // Debug.p("pcdata1: " + entry.ename_or_pcdata);
          return;
        }
      }
      StackEntry entry = new StackEntry();
      top.sub_entries.add(entry);
      entry.ename_or_pcdata = new String(ch, start, length);
    }
  }

  /**
   * This is a SAX document handler to parse XML into AFrames.
   */
  private static class AFrameHandler extends VinciFrameHandler {

    AFrameHandler() {
    }

    public void startElement(String uri, String name, String qName, org.xml.sax.Attributes a) {
      // Debug.p("Attributes: " + qName + " : " + atts.getLength());
      AStackEntry entry = new AStackEntry();
      entry.ename_or_pcdata = qName;
      if (a.getLength() > 0) {
        org.apache.vinci.transport.Attributes frame_attributes = new org.apache.vinci.transport.Attributes();
        for (int i = 0; i < a.getLength(); i++) {
          // Debug.p("Att: " + a.getQName(i) + " : " + a.getValue(i));
          frame_attributes.fadd(a.getQName(i), a.getValue(i));
        }
        entry.attributes = frame_attributes;
      }
      entry.sub_entries = new ArrayList(2);
      if (top == null) {
        top = entry;
      } else {
        top.sub_entries.add(entry);
        entry.parent = top;
        top = entry;
      }
    }

    public void endElement(String uri, String name, String qName) {
      if (top.sub_entries.size() == 0) {
        AFrame c = new AFrame();
        top.component = c;
      } else {
        StackEntry child = (StackEntry) top.sub_entries.get(0);
        if (top.sub_entries.size() == 1 && child.sub_entries == null) {
          AFrameLeaf l = new AFrameLeaf(child.ename_or_pcdata);
          top.component = l;
        } else {
          AFrame c = new AFrame();
          top.component = c;
          for (int i = 0; i < top.sub_entries.size(); i++) {
            child = (StackEntry) top.sub_entries.get(i);
            if (child.component != null) {
              c.add(child.ename_or_pcdata, child.component);
            } else {
              c.fadd(TransportConstants.PCDATA_KEY, child.ename_or_pcdata);
            }
            child.sub_entries = null;
          }
        }
      }
      org.apache.vinci.transport.Attributes a = ((AStackEntry) top).attributes;
      if (a != null) {
        top.component.setAttributes(a);
      }
      if (top.parent != null) {
        top = top.parent;
      }
    }

  }

  /**
   * Populate the empty document with the XML yielded by the provided reader.
   * 
   * @param empty
   *          An empty document to be populated.
   * @param r
   *          A reader providing the XML to populate the empty document.
   * @return -
   * @exception ServiceException
   *              if there is a parse error.
   */
  public static Transportable xmlToTransportable(Reader r, Transportable empty)
          throws ServiceException {
    TransportableConverter.convert(xmlToAFrame(r), empty);
    return empty;
  }

  /**
   * Convert the XML document (provided as a Reader) to a VinciFrame document model. Throws
   * ServiceException if the XML parser reports any error. WARNING: This method will silently ignore
   * any attributes or processing instructions within the document since VinciFrame cannot represent
   * them. Consider using AFrame if attribute support is required.
   * 
   * This implementation of xmlToVinciFrame uses apache SAX parser directly. It should be faster,
   * and it should be tolerant of undeclared namespaces, unlike the previous impl.
   * 
   * @param r
   *          A reader providing the XML to convert.
   * @return -
   * @exception ServiceException
   *              if there is a parse error.
   */
  public static VinciFrame xmlToVinciFrame(Reader r) throws ServiceException {
    XMLReader xr;
    try {
      xr = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
    } catch (SAXException e) {
      throw new ServiceException("Error creating SAX Parser: " + e);
    } catch (ParserConfigurationException e) {
      throw new ServiceException("Error creating SAX Parser: " + e);
    }
    // prevent undeclared namespace warnings.
    try {
      xr.setFeature("http://xml.org/sax/features/namespaces", false);
      VinciFrameHandler handler = new VinciFrameHandler();
      xr.setContentHandler(handler);
      xr.setErrorHandler(handler);
      xr.parse(new InputSource(r));
      return (VinciFrame) handler.top.component;
    } catch (IOException e) {
      Debug.reportException(e);
      throw new ServiceException("Reader IO error: " + e);
    } catch (SAXException e) {
      Debug.reportException(e);
      throw new ServiceException("XML Parse error: " + e);
    }
  }
  /*
   * public static AFrame xmlToAFrame(Reader r) throws ServiceException { return (AFrame)
   * xmlToTransportable(r, new AFrame()); }
   */
 
  /**
   * Convert the XML document (provided as a Reader) to the AFrame document model. Throws
   * ServiceException if the XML parser reports any error. WARNING: This method will silently ignore
   * any processing instructions within the document since AFrame cannot represent them.
   * 
   * @param r
   *          A reader providing the XML to convert.
   * @return -
   * @exception ServiceException
   *              if there is a parse error.
   */
  public static AFrame xmlToAFrame(Reader r) throws ServiceException {
    XMLReader xr;
    try {
      xr = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
    } catch (SAXException e) {
      throw new ServiceException("Error creating SAX Parser: " + e);
    } catch (ParserConfigurationException e) {
      throw new ServiceException("Error creating SAX Parser: " + e);
    }
    // prevent undeclared namespace warnings.
    try {
      xr.setFeature("http://xml.org/sax/features/namespaces", false);
      AFrameHandler handler = new AFrameHandler();
      xr.setContentHandler(handler);
      xr.setErrorHandler(handler);
      xr.parse(new InputSource(r));
      return (AFrame) handler.top.component;
    } catch (IOException e) {
      Debug.reportException(e);
      throw new ServiceException("Reader IO error: " + e);
    } catch (SAXException e) {
      Debug.reportException(e);
      throw new ServiceException("XML Parse error: " + e);
    }
  }
}
