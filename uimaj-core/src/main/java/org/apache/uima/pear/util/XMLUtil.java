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

package org.apache.uima.pear.util;

import java.io.BufferedReader;
import java.io.CharConversionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.uima.internal.util.XMLUtils;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * The <code>XMLUtil</code> class provides miscellaneous XML utilities.
 * 
 */

public class XMLUtil {
  /*
   * XML constants
   */
  public static final String XML_HEADER_BEG = "<?xml version=\"1.0\"";

  public static final String XML_ENCODING_TAG = "encoding";

  public static final String XML_HEADER_END = "?>";

  public static final String CDATA_SECTION_BEG = "<![CDATA[";

  public static final String CDATA_SECTION_END = "]]>";

  /**
   * XML parser feature ids
   */
  // Namespaces feature id
  public static final String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";

  // Namespace prefixes feature id
  public static final String NAMESPACE_PREFIXES_FEATURE_ID = "http://xml.org/sax/features/namespace-prefixes";

  // Validation feature id
  public static final String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";

  // Schema validation feature id
  public static final String SCHEMA_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/schema";

  // Schema full checking feature id
  public static final String SCHEMA_FULL_CHECKING_FEATURE_ID = "http://apache.org/xml/features/validation/schema-full-checking";

  // Dynamic validation feature id
  public static final String DYNAMIC_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/dynamic";

  /*
   * XML parser default settings
   */
  // Default namespaces support (true).
  protected static final boolean DEFAULT_NAMESPACES = true;

  // Default namespace prefixes (false).
  protected static final boolean DEFAULT_NAMESPACE_PREFIXES = false;

  // Default validation support (false).
  protected static final boolean DEFAULT_VALIDATION = false;

  // Default Schema validation support (false).
  protected static final boolean DEFAULT_SCHEMA_VALIDATION = false;

  // Default Schema full checking support (false).
  protected static final boolean DEFAULT_SCHEMA_FULL_CHECKING = false;

  // Default dynamic validation support (false).
  protected static final boolean DEFAULT_DYNAMIC_VALIDATION = false;

  /*
   * XML encoding
   */
  // First 2 real characters in XML file
  protected static final String FIRST_XML_CHARS = "<?";

  /**
   * XMLUtil constructor comment.
   */
  public XMLUtil() {
    super();
  }

  /**
   * Gets new instance of the <code>SAXParser</code> class and sets standard features.
   * 
   * @return The new instance of the <code>SAXParser</code> class.
   * @exception org.xml.sax.SAXException
   *              if any parser exception occurred.
   */
  public static SAXParser createSAXParser() throws SAXException {
    SAXParser parser = null;
    try {
      // get SAX parser factory
      SAXParserFactory factory = XMLUtils.createSAXParserFactory();
      // set default SAX parser features
      factory.setFeature(NAMESPACES_FEATURE_ID, DEFAULT_NAMESPACES);
      factory.setFeature(NAMESPACE_PREFIXES_FEATURE_ID, DEFAULT_NAMESPACE_PREFIXES);
      factory.setFeature(VALIDATION_FEATURE_ID, DEFAULT_VALIDATION);
      // create SAX parser
      parser = factory.newSAXParser();
    } catch (ParserConfigurationException exc) {
      throw new SAXException(exc);
    }
    return parser;
  } // createSAXParser()

  /**
   * Attempts to detect file encoding of a given XML file by analyzing it's first characters. This
   * method can recognize the following 2 standard encodings: UTF-8 (ASCII) and UTF-16 (LE and BE).
   * If the given XML file is not valid or its encoding cannot be recognized, the method returns
   * <code>null</code>, otherwise it returns the detected encoding name. For more on UTF
   * encodings and its signatures see <a href="http://www.unicode.org/faq/utf_bom.html"
   * target="_blank"> FAQ - UTF and BOM</a>.
   * 
   * @param xmlFile
   *          The given XML file.
   * @return Detected XML file encoding name or <code>null</code>, if the file encoding cannot be
   *         recognized or the file is not a valid XML file.
   * @throws IOException
   *           If the given file cannot be read.
   */
  public static String detectXmlFileEncoding(File xmlFile) throws IOException {
    String encoding = null;
    FileInputStream iStream = null;
    BufferedReader fReader = null;
    try {
      // first, make sure - this is a valid XML file
      if (!isValidXmlFile(xmlFile)) {
        return null;
      }
      iStream = new FileInputStream(xmlFile);
      // read prefix - possible BOM or signature
      int byteCounter = 0;
      int nextByte = 0;
      int[] prefix = new int[16];
      do {
        nextByte = iStream.read();
        // store as possible UTF signature or BOM
        if (byteCounter < 16)
          prefix[byteCounter] = nextByte;
        byteCounter++;
        if (nextByte < 0)
          throw new IOException("cannot read file");
      } while (nextByte == 0xEF || nextByte == 0xBB || nextByte == 0xBF || nextByte == 0xFE
              || nextByte == 0xFF || nextByte == 0x00);
      int prefixLength = byteCounter < 17 ? byteCounter - 1 : 16;
      String utfSignature = (prefixLength > 0) ? FileUtil
              .identifyUtfSignature(prefix, prefixLength) : null;
      boolean utf8Signature = false;
      boolean utf16Signature = false;
      boolean utf32Signature = false;
      if (utfSignature != null) {
        // check signature name
        if (utfSignature.startsWith("UTF-8"))
          utf8Signature = true;
        else if (utfSignature.startsWith("UTF-16"))
          utf16Signature = true;
        else if (utfSignature.startsWith("UTF-32"))
          utf32Signature = true;
      }
      byte[] buffer = null;
      int bytes2put = 0;
      // if signature for UTF-16 or UTF-32 exists - put it to the buffer
      if (utf16Signature) {
        // UTF-16 - put 2 bytes of signature + 7x2 bytes
        bytes2put = 7 * 2; // <?xml?>
        buffer = new byte[prefixLength + bytes2put];
        for (int i = 0; i < prefixLength; i++)
          buffer[i] = (byte) prefix[i];
        byteCounter = prefixLength;
      } else if (utf32Signature) {
        // UTF-32 - put 4 bytes of signature + 7x4 bytes
        bytes2put = 7 * 4; // <?xml?>
        buffer = new byte[prefixLength + bytes2put];
        for (int i = 0; i < prefixLength; i++)
          buffer[i] = (byte) prefix[i];
        byteCounter = prefixLength;
      } else {
        // UTF8 or no signature - put only text characters
        bytes2put = 7; // <?xml?>
        buffer = new byte[bytes2put];
        byteCounter = 0;
      }
      // store the 1st text byte and read next 6 bytes of XML file
      buffer[byteCounter++] = (byte) nextByte;
      // this next bit is because the "read(...)" is not obliged to return all the bytes
      // and must be put in a while loop to guarantee getting them
      int offset = 0;
      while (offset < (bytes2put - 1)) {
        int bytesRead = iStream.read(buffer, offset + byteCounter, bytes2put - 1 - offset);
        if (bytesRead == -1)
          break;
        offset += bytesRead;
      }
      if (offset != (bytes2put - 1))
        throw new IOException("cannot read file");
      // check first XML header characters - '<?'
      // buffer is 7 bytes
      // some Javas won't properly decode an odd number of bytes for utf16 coding
      // https://issues.apache.org/jira/browse/UIMA-2099
      byte[] buffer6 = new byte[6];
      System.arraycopy(buffer, 0, buffer6, 0, 6);  
      if (utf8Signature) {
        // check for UTF-8
        String test = new String(buffer, "UTF-8");
        if (test.startsWith(FIRST_XML_CHARS))
          encoding = "UTF-8";
      } else if (utf16Signature) {
        // check for UTF-16
        String test = new String(buffer6, "UTF-16");
        if (test.startsWith(FIRST_XML_CHARS))
          encoding = "UTF-16";
      } else if (utf32Signature) {
        // we don't support this
      } else {
        // no signature - check for UTF-8 in XML header characters
        String test = new String(buffer, "UTF-8");
        if (test.startsWith(FIRST_XML_CHARS))
          encoding = "UTF-8";
        else {
          // next, check for UTF-16LE in XML header characters
          test = new String(buffer6, "UTF-16LE");
          if (test.startsWith(FIRST_XML_CHARS)) {
            encoding = "UTF-16LE";
          } else {
            // next, check for UTF-16BE in XML header characters
            test = new String(buffer6, "UTF-16BE");
            if (test.startsWith(FIRST_XML_CHARS)) {
              encoding = "UTF-16BE";
            }
          }
        }
      }
      iStream.close();
      if (encoding == null) {
        // last resort: check 1st non-space XML character - '<'
        // check 1st non-space XML character for UTF-8
        fReader = new BufferedReader(new InputStreamReader(new FileInputStream(xmlFile), "UTF-8"));
        String line = null;
        try {
          while ((line = fReader.readLine()) != null) {
            String xmlLine = line.trim();
            if (xmlLine.length() > 0) {
              if (xmlLine.charAt(0) == '<')
                encoding = "UTF-8";
              break;
            }
          }
        } catch (CharConversionException err) {
        }
        fReader.close();
        if (encoding == null) {
          // check 1st non-space XML character for UTF-16
          fReader = new BufferedReader(
                  new InputStreamReader(new FileInputStream(xmlFile), "UTF-16"));
          try {
            while ((line = fReader.readLine()) != null) {
              String xmlLine = line.trim();
              if (xmlLine.length() > 0) {
                if (xmlLine.charAt(0) == '<')
                  encoding = "UTF-16";
                break;
              }
            }
          } catch (CharConversionException err) {
          }
          fReader.close();
        }
      }
    } catch (IOException exc) {
      throw exc;
    } catch (Throwable err) {
      throw new IOException(err.toString());
    } finally {
      if (iStream != null)
        try {
          iStream.close();
        } catch (Exception e) {
        }
      if (fReader != null)
        try {
          fReader.close();
        } catch (Exception e) {
        }
    }
    return encoding;
  } // detectXmlFileEncoding()

  /**
   * Tries to parse a given XML file using SAX parser. Returns <code>true</code>, if the parser
   * does not encounter fatal error, otherwise returns <code>false</code>.
   * 
   * @param xmlFile
   *          The given XML file to be tested.
   * @return <code>true</code>, if the given XML file can be parsed, <code>false</code>
   *         otherwise.
   * @throws IOException
   *           If the given file cannot be read.
   */
  public static boolean isValidXmlFile(File xmlFile) throws IOException {
    boolean isValid = true;
    InputStream iStream = null;
    try {
      iStream = new FileInputStream(xmlFile);
      SAXParser parser = createSAXParser();
      parser.parse(iStream, new DefaultHandler());
    } catch (IOException exc) {
      throw exc;
    } catch (SAXException err) {
      isValid = false;
    } finally {
      if (iStream != null)
        try {
          iStream.close();
        } catch (Exception e) {
        }
    }
    return isValid;
  } // isValidXmlFile()

  /**
   * Prints SAX error message.
   * @param type type
   * @param ex exception
   */
  public static void printError(String type, SAXParseException ex) {

    System.err.print('[');
    System.err.print(type);
    System.err.print("] ");

    if (ex == null) {
      System.err.print("SAX Parse Exception was null! Therefore, no further details are available.");
    } else {
      String systemId = ex.getSystemId();
      if (systemId != null) {
        int index = systemId.lastIndexOf('/');
        if (index != -1)
          systemId = systemId.substring(index + 1);
        System.err.print(systemId);
      }
      System.err.print(':');
      System.err.print(ex.getLineNumber());
      System.err.print(':');
      System.err.print(ex.getColumnNumber());
      System.err.print(": ");
      System.err.print(ex.getMessage());
    }
    System.err.println();
    System.err.flush();
  } // printError(String,SAXParseException)

  /**
   * Prints entries of a given <code>Properties</code> object as XML elements to a given
   * <code>PrintWriter</code>, maintaining a given indentation level.
   * 
   * @param elements
   *          The given <code>Properties</code> object.
   * @param oWriter
   *          The given <code>PrintWriter</code> object.
   * @param level
   *          The given indentation level.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static void printAllXMLElements(Properties elements, PrintWriter oWriter, int level)
          throws IOException {
    printAllXMLElements(elements, null, oWriter, level);
  }

  /**
   * Prints entries of a given <code>Properties</code> object as XML elements to a given
   * <code>PrintWriter</code>, maintaining a specified tag order and a given indentation level.
   * Some elements may contain multiple values delimited by a specified value delimiter. Inserts new
   * line after each printed element.
   * 
   * @param elements
   *          The given <code>Properties</code> object.
   * @param valueDelimiter
   *          The specified value delimiter for multi-valued elements.
   * @param tagOrder
   *          The <code>String</code> array that specifies the tag order.
   * @param oWriter
   *          The given <code>PrintWriter</code> object.
   * @param level
   *          The given indentation level.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static void printAllXMLElements(Properties elements, String valueDelimiter,
          String[] tagOrder, PrintWriter oWriter, int level) throws IOException {
    // check if elements might be multi- valued
    boolean multiValue = valueDelimiter != null && valueDelimiter.length() > 0;
    if (elements != null) {
      if (tagOrder != null) {
        // print elements in mandatory order
        for (int i = 0; i < tagOrder.length; i++) {
          String tag = tagOrder[i];
          String eValue = elements.getProperty(tag);
          if (eValue != null) {
            // print XML element(s)
            if (multiValue)
              printXMLElements(tag, eValue, valueDelimiter, oWriter, level);
            else {
              printXMLElement(tag, eValue, oWriter, level);
              // insert new line
              oWriter.println();
            }
          }
        }
      }
      // print all other elements
      Enumeration<Object> keys = elements.keys();
      while (keys.hasMoreElements()) {
        String tag = (String) keys.nextElement();
        // check if already printed
        boolean done = false;
        if (tagOrder != null) {
          for (int i = 0; i < tagOrder.length; i++) {
            if (tag.equals(tagOrder[i])) {
              done = true;
              break;
            }
          }
        }
        if (!done) {
          // print XML element(s)
          String eValue = elements.getProperty(tag);
          if (multiValue)
            printXMLElements(tag, eValue, valueDelimiter, oWriter, level);
          else {
            printXMLElement(tag, eValue, oWriter, level);
            // insert new line
            oWriter.println();
          }
        }
      }
    }
  }

  /**
   * Prints entries of a given <code>Properties</code> object as XML elements to a given
   * <code>PrintWriter</code>, maintaining a specified tag order and a given indentation level.
   * 
   * @param elements
   *          The given <code>Properties</code> object.
   * @param tagOrder
   *          The <code>String</code> array that specifies the tag order.
   * @param oWriter
   *          The given <code>PrintWriter</code> object.
   * @param level
   *          The given indentation level.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static void printAllXMLElements(Properties elements, String[] tagOrder,
          PrintWriter oWriter, int level) throws IOException {
    printAllXMLElements(elements, null, tagOrder, oWriter, level);
  }

  /**
   * Prints a given XML element, which contains only given attributes, to a given
   * <code>PrintWriter</code>, maintaining a given indentation level.
   * 
   * @param tag
   *          The given XML tag.
   * @param attributes
   *          The given XML element attributes.
   * @param oWriter
   *          The given <code>PrintWriter</code> object.
   * @param level
   *          The given indentation level (number of marginal '\t' symbols).
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static void printXMLElement(String tag, Properties attributes, PrintWriter oWriter,
          int level) throws IOException {
    printXMLElement(tag, attributes, null, oWriter, level);
  }

  /**
   * Prints a given XML element, which contains given attributes and a given string value, to a
   * given <code>PrintWriter</code>, maintaining a given indentation level. The string element
   * value (if exists) is printed 'as is' - without a CDATA section.
   * 
   * @param tag
   *          The given XML element tag.
   * @param attributes
   *          The given XML element attributes.
   * @param elemValue
   *          The given XML element value.
   * @param oWriter
   *          The given <code>PrintWriter</code> object.
   * @param level
   *          The given indentation level (number of marginal '\t' symbols).
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static void printXMLElement(String tag, Properties attributes, String elemValue,
          PrintWriter oWriter, int level) throws IOException {
    printXMLElement(tag, attributes, elemValue, false, oWriter, level, false);
  }

  /**
   * Prints a given XML element, which contains given attributes and a given string value, to a
   * given <code>PrintWriter</code>, maintaining a given indentation level. The string element
   * value (if exists) is printed within or without the CDATA section, depending on a given
   * <code>boolean</code> flag value.
   * 
   * @param tag
   *          The given XML element tag.
   * @param attributes
   *          The given XML element tag.
   * @param elemValue
   *          The given XML element value.
   * @param putInCdataSection
   *          If <code>true</code>, puts the element value in the <code>CDATA</code> section,
   *          otherwise prints the element value without the <code>CDATA</code> section.
   * @param oWriter
   *          The given <code>PrintWriter</code> object.
   * @param level
   *          The given indentation level (number of marginal '\t' symbols).
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static void printXMLElement(String tag, Properties attributes, String elemValue,
          boolean putInCdataSection, PrintWriter oWriter, int level) throws IOException {
    printXMLElement(tag, attributes, elemValue, putInCdataSection, oWriter, level, false);
  }

  /**
   * Prints a given XML element, which contains given attributes and a given string value, to a
   * given <code>PrintWriter</code>, maintaining a given indentation level. The string element
   * value (if exists) may be printed inside a CDATA section, depending on the value of a given
   * 'CDATA' <code>boolean</code> flag. The element value may be printed on the same line as the
   * element tags, or on a new line, depending on the value of a given 'new-line'
   * <code>boolean</code> flag.
   * 
   * @param tag
   *          The given XML element tag.
   * @param attributes
   *          The given XML element attributes.
   * @param elemValue
   *          The given XML element value.
   * @param putInCdataSection
   *          If <code>true</code>, the given element value is printed inside the CDATA section,
   *          otherwise it's printed 'as is'.
   * @param oWriter
   *          The given <code>PrintWriter</code> object.
   * @param level
   *          The given indentation level (number of marginal '\t' symbols).
   * @param useNewLine4Value
   *          If <code>true</code>, the given element value is printed on a new line, otherwise
   *          it's printed on the same line as the element tags.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static void printXMLElement(String tag, Properties attributes, String elemValue,
          boolean putInCdataSection, PrintWriter oWriter, int level, boolean useNewLine4Value)
          throws IOException {
    // print XML tag beginning
    printXMLTag(tag, attributes, oWriter, false, level);
    if (useNewLine4Value) {
      oWriter.println();
      // print element value, adding marginal tabs
      printXMLElementValue(elemValue, putInCdataSection, oWriter, level);
      oWriter.println();
      // print XML tag end, adding marginal tabs
      printXMLTag(tag, oWriter, true, level);
    } else {
      // print element value w/o marginal tabs
      printXMLElementValue(elemValue, putInCdataSection, oWriter, 0);
      // print XML tag end w/o marginal tabs
      printXMLTag(tag, oWriter, true, 0);
    }
  }

  /**
   * Prints a given XML element, which contains only a given string value, to a given
   * <code>PrintWriter</code>, maintaining a given indentation level. The string element value is
   * printed 'as-is' without the CDATA block.
   * 
   * @param tag
   *          The given XML tag.
   * @param elemValue
   *          The given XML element value.
   * @param oWriter
   *          The given <code>PrintWriter</code> object.
   * @param level
   *          The given indentation level (number of marginal '\t' symbols).
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static void printXMLElement(String tag, String elemValue, PrintWriter oWriter, int level)
          throws IOException {
    printXMLElement(tag, null, elemValue, oWriter, level);
  }

  /**
   * Prints a given XML element, which contains only a given string value, to a given
   * <code>PrintWriter</code>, maintaining a given indentation level. The string element value
   * (if exists) is put into the CDATA block, if a given <code>boolean</code> flag is
   * <code>true</code>.
   * 
   * @param tag
   *          The given XML tag.
   * @param elemValue
   *          The given XML element value.
   * @param putInCdataSection
   *          If <code>true</code>, puts the element value in the <code>CDATA</code> section,
   *          otherwise prints the element value without the <code>CDATA</code> section.
   * @param oWriter
   *          The given <code>PrintWriter</code> object.
   * @param level
   *          The given indentation level (number of marginal '\t' symbols).
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static void printXMLElement(String tag, String elemValue, boolean putInCdataSection,
          PrintWriter oWriter, int level) throws IOException {
    printXMLElement(tag, null, elemValue, putInCdataSection, oWriter, level);
  }

  /**
   * Prints multiple values of a given XML element, separated with a specified delimiter, to a given
   * <code>PrintWriter</code>, maintaining a given indentation level. Inserts new line after each
   * printed element value.
   * 
   * @param tag
   *          The given XML element tag.
   * @param elemValue
   *          The given XML element values (multi-value), separated with the given delimiter.
   * @param valueDelimiter
   *          The given delimiter for multi-value elements.
   * @param oWriter
   *          The given <code>PrintWriter</code> object.
   * @param level
   *          The given indentation level (number of marginal '\t' symbols).
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static void printXMLElements(String tag, String elemValue, String valueDelimiter,
          PrintWriter oWriter, int level) throws IOException {
    if (elemValue != null) {
      // get list of tokens in the element value
      StringTokenizer elemTokens = new StringTokenizer(elemValue, valueDelimiter);
      while (elemTokens.hasMoreTokens()) {
        // repeat for all tokens in the element value
        String elemToken = elemTokens.nextToken();
        XMLUtil.printXMLElement(tag, elemToken, oWriter, level);
        oWriter.println();
      }
    }
  }

  /**
   * Prints a given element value to a given <code>PrintWriter</code>, maintaining a given
   * indentation level. By default, prints the element value 'as is' - not using the
   * <code>CDATA</code> section.
   * 
   * @param elemValue
   *          The given element value.
   * @param oWriter
   *          The given <code>PrintWriter</code> object.
   * @param level
   *          The given indentation level.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static void printXMLElementValue(String elemValue, PrintWriter oWriter, int level)
          throws IOException {
    printXMLElementValue(elemValue, false, oWriter, level);
  }

  /**
   * Prints a given element value to a given <code>PrintWriter</code>, maintaining a given
   * indentation level. If a given <code>boolean</code> 'CDATA' flag is <code>true</code>, puts
   * the element value in the <code>CDATA</code> section, otherwise prints the element value 'as
   * is'.
   * 
   * @param elemValue
   *          The given element value.
   * @param putInCdataSection
   *          If <code>true</code>, puts the element value in the <code>CDATA</code> section,
   *          otherwise prints the element value without the <code>CDATA</code> section.
   * @param oWriter
   *          The given <code>PrintWriter</code> object.
   * @param level
   *          The given indentation level.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static void printXMLElementValue(String elemValue, boolean putInCdataSection,
          PrintWriter oWriter, int level) throws IOException {
    // add marginal tabs
    for (int l = 0; l < level; l++)
      oWriter.print('\t');
    if (elemValue != null) {
      // print XML element value
      if (putInCdataSection)
        oWriter.print(CDATA_SECTION_BEG);
      oWriter.print(elemValue.trim());
      if (putInCdataSection)
        oWriter.print(CDATA_SECTION_END);
      oWriter.flush();
    }
  }

  /**
   * Prints standard XML 1.0 header with a specified encoding to a given <code>PrintStream</code>.
   * If no encoding is specified (<code>null</code> or empty string), does not include the
   * encoding name in the header.
   * 
   * @param encoding
   *          The given XML encoding name or <code>null</code>.
   * @param oWriter
   *          The given output <code>PrintStream</code>.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static void printXMLHeader(String encoding, PrintWriter oWriter) throws IOException {
    oWriter.print(XML_HEADER_BEG);
    if (encoding != null && encoding.length() > 0) // add encoding
      oWriter.print(" " + XML_ENCODING_TAG + "=\"" + encoding + "\"");
    oWriter.println(XML_HEADER_END);
  }

  /**
   * Prints a given XML tag to a given <code>PrintWriter</code>, maintaining a given indentation
   * level.
   * 
   * @param tag
   *          The given XML tag.
   * @param oWriter
   *          The given <code>PrintWriter</code> object.
   * @param tagEnd
   *          If <code>false</code> prints the XML tag beginning brackets, otherwise prints the
   *          the XML tag ending brackets.
   * @param level
   *          The given indentation level (number of marginal '\t' symbols).
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static void printXMLTag(String tag, PrintWriter oWriter, boolean tagEnd, int level)
          throws IOException {
    printXMLTag(tag, null, oWriter, tagEnd, level);
  }

  /**
   * Prints a given XML tag with given element attributes to a given <code>PrintWriter</code>,
   * maintaining a given indentation level.
   * 
   * @param tag
   *          The given XML tag.
   * @param attributes
   *          The given XML element attributes.
   * @param oWriter
   *          The given <code>PrintWriter</code> object.
   * @param tagEnd
   *          If <code>false</code> prints the XML tag beginning brackets, otherwise prints the
   *          the XML tag ending brackets.
   * @param level
   *          The given indentation level (number of marginal '\t' symbols).
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static void printXMLTag(String tag, Properties attributes, PrintWriter oWriter,
          boolean tagEnd, int level) throws IOException {
    // add marginal tabs
    for (int l = 0; l < level; l++)
      oWriter.print('\t');
    // print XML tag prefix
    if (tagEnd)
      oWriter.print("</");
    else
      oWriter.print('<');
    // print XML tag name
    oWriter.print(tag);
    if (!tagEnd && attributes != null) {
      // print attributes
      Enumeration<Object> attrNames = attributes.keys();
      while (attrNames.hasMoreElements()) {
        // print attribute: name="value"
        String name = (String) attrNames.nextElement();
        String value = attributes.getProperty(name);
        oWriter.print(' ');
        oWriter.print(name);
        oWriter.print("=\"");
        oWriter.print(value);
        oWriter.print('\"');
      }
    }
    // print XML tag suffix
    oWriter.print('>');
    oWriter.flush();
  }
}
