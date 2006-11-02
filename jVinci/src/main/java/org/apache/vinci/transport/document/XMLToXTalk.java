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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.vinci.debug.Debug;
import org.apache.vinci.debug.FatalException;
import org.apache.vinci.transport.ServiceException;
import org.apache.vinci.transport.XTalkTransporter;
import org.apache.vinci.transport.util.XMLConverter;

/**
 * Class for parsing an XML document and converting directly to XTalk.
 */
public class XMLToXTalk {

  /**
   * Utility class not intended to be instantiated.
   */
  private XMLToXTalk() {
  }

  private static class StackEntry {
    int childCount;
  }

  static String convert(String s) {
    return XMLConverter.simpleConvertStringToXMLString(s);
  }

  /**
   * This is a SAX document handler to parse XML into VinciFrames.
   */
  private static class XTalkHandler extends DefaultHandler {
    protected StackEntry top           = null;
    int                  depth         = 0;
    StackEntry[]         childrenCount = new StackEntry[6969];
    ArrayList            countList     = new ArrayList();
    OutputStream         os;
    Writer               xml_os;
    boolean              purgeWhitespace;

    XTalkHandler(OutputStream os, boolean purgeWhitespace, Writer xml_os) {
      this.os = os;
      this.xml_os = xml_os;
      childrenCount[0] = new StackEntry();
      this.purgeWhitespace = purgeWhitespace;
    }

    public void startElement(String uri, String name, String qName, org.xml.sax.Attributes a) {
      flushString();
      try {
        if (xml_os != null) {
          xml_os.write('<');
          xml_os.write(convert(qName));
        }
        os.write(XTalkTransporter.ELEMENT_MARKER);
        childrenCount[depth].childCount++;
        depth++;
        childrenCount[depth] = new StackEntry();
        countList.add(childrenCount[depth]);
        //Debug.p("Attributes: " + qName + " : " +  atts.getLength());
        XTalkTransporter.stringToBin(qName, os);
        XTalkTransporter.writeInt(a.getLength(), os);
        for (int i = 0; i < a.getLength(); i++) {
          XTalkTransporter.stringToBin(a.getQName(i), os);
          XTalkTransporter.stringToBin(a.getValue(i), os);
          if (xml_os != null) {
            xml_os.write(' ');
            xml_os.write(convert(a.getQName(i)));
            xml_os.write("=\"");
            xml_os.write(convert(a.getValue(i)));
            xml_os.write('\"');
          }
        }
        XTalkTransporter.writeInt(69, os); // placeholder for # of children, which is unknown
        if (xml_os != null) {
          xml_os.write('>');
        }
        // at this point.
      } catch (IOException e) {
        throw new FatalException(e);
      }
    }

    public void endElement(String uri, String name, String qName) {
      flushString();
      depth--;
      if (xml_os != null) {
        try {
          xml_os.write("</");
          xml_os.write(convert(qName));
          xml_os.write('>');
        } catch (IOException e) {
          throw new FatalException(e);
        }
      }
    }

    /**
     * @pre ch != null
     * @pre start < ch.length
     * @pre length >= start
     * @pre top != null
     * @pre top.sub_entries != null
     */
    StringBuffer buf = new StringBuffer();

    public void characters(char[] ch, int start, int length) {
      buf.append(ch, start, length);
    }

    public void flushString() {
      if (buf.length() > 0) {
        String s = buf.toString();
        buf.setLength(0);
        if (purgeWhitespace) {
          boolean all_whitespace = true;
          int length = s.length();
          for (int i = 0; i < length; i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
              all_whitespace = false;
              break;
            }
          }
          if (all_whitespace) {
            //Debug.p("Purging whitespace: " + new String(ch, start, length));
            return;
          }
        }
        childrenCount[depth].childCount++;
        try {
          os.write(XTalkTransporter.STRING_MARKER);
          XTalkTransporter.stringToBin(s, os);
          if (xml_os != null) {
            xml_os.write(convert(s));
          }
        } catch (IOException e) {
          throw new FatalException(e);
        }
      }
    }
  }

  /**
   * Right now we assume there are NO processing instructions.
   * Given an XML file, create an XTalk representation of that data. If xml_filename is non-null, then
   * this method will also create a UTF-8 representation of the xml file, exactly mimicing the
   * XTalk encoding (e.g. removing irrelevant whitespace, expanding entity refs, etc). 
   */
  public static void xmlToXTalk(Reader r, String filename, boolean purgeWhitespace, String xml_filename)
      throws ServiceException, IOException {
    Writer xml_os = null;
    if (xml_filename != null) {
      xml_os = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(xml_filename), "UTF-8"));
    }
    File file = new File(filename);
    OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
    XTalkHandler handler = null;
    try {
      os.write(XTalkTransporter.DOCUMENT_MARKER);
      os.write(XTalkTransporter.VERSION_CODE);
      XTalkTransporter.writeInt(1, os);
      XMLReader xr;
      try {
        xr = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
      } catch (SAXException e) {
        throw new ServiceException("Error creating SAX Parser: " + e);
      } catch (ParserConfigurationException e) {
        throw new ServiceException("Error creating SAX Parser: " + e);
      }
      if (xml_os != null) {
        xml_os.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      }
      // prevent undeclared namespace warnings.
      try {
        xr.setFeature("http://xml.org/sax/features/namespaces", false);
        handler = new XTalkHandler(os, purgeWhitespace, xml_os);
        xr.setContentHandler(handler);
        xr.setErrorHandler(handler);
        xr.parse(new InputSource(r));
        Debug.p("Final depth: " + handler.depth);
        Debug.p("Final child list size: " + handler.countList.size());
      } catch (SAXException e) {
        Debug.reportException(e);
        throw new ServiceException("XML Parse error: " + e);
      }
    } finally {
      os.close();
      if (xml_os != null) {
        xml_os.close();
      }
    }
    RandomAccessFile raf = new RandomAccessFile(filename, "rw");
    try {
      raf.skipBytes(7);
      //int return_val = 
      updateElement(raf, handler.countList, 0);
      //Debug.p("Return val: " + return_val);
    } finally {
      raf.close();
    }

  }

  static private int updateElement(RandomAccessFile raf, ArrayList counts, int index) throws IOException {
    skipString(raf);
    int skipCount = raf.readInt();
    //Debug.p("Skip count: " + skipCount);
    for (int i = 0; i < skipCount; i++) {
      skipString(raf);
      skipString(raf);
    }
    int childCount = ((StackEntry) counts.get(index)).childCount;
    index++;
    raf.writeInt(childCount);
    //Debug.p("Wrote: " + childCount);
    for (int i = 0; i < childCount; i++) {
      int marker = raf.read();
      switch ((byte) marker) {
        case XTalkTransporter.ELEMENT_MARKER:
          index = updateElement(raf, counts, index);
          break;
        case XTalkTransporter.STRING_MARKER:
          skipString(raf);
          break;
        default:
          throw new IOException("Unexepcted marker: " + marker);
      }
    }
    return index;
  }

  static private void skipString(RandomAccessFile raf) throws IOException {
    int count = raf.readInt();
    //Debug.p("Skipping string of size: " + count);
    raf.skipBytes(count);
  }

  public static void main(String[] args) throws Exception {
    if (args[0].endsWith(".xtalk")) {
      InputStream is = new FileInputStream(args[0]);
      AFrame af = new AFrame();
      af.fromStream(is);
      Debug.p(af.toXML());
    } else if (args[0].endsWith(".xml")) {
      Reader r = new FileReader(args[0]);
      xmlToXTalk(r, "tmp.xtalk", args.length > 1, "tmp.xml");
    } else {
      throw new Exception("Unexpected filename suffix. Provide only .xml or .xtalk");
    }
  }

}
