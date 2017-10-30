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

package org.apache.uima.internal.util;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;

import org.apache.uima.UIMAFramework;
import org.apache.uima.util.Level;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Some utilities for working with XML.
 * 
 * 
 */
public abstract class XMLUtils {
  
  // constants - not all Java versions define these
  
  private static final String ACCESS_EXTERNAL_STYLESHEET = "http://javax.xml.XMLConstants/property/accessExternalStylesheet";
  private static final String ACCESS_EXTERNAL_DTD = "http://javax.xml.XMLConstants/property/accessExternalDTD";
  
  private static final String DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";
  private static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
  private static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
  private static final String EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
  /**
   * Normalizes the given string for output to XML. This converts all special characters, e.g. &lt;,
   * %gt;, &amp;, to their XML representations, e.g. &amp;lt;, &amp;gt;, &amp;amp;. The normalized
   * string is appended to the specified StringBuffer.
   * 
   * @param aStr
   *          input string
   * @param aResultBuf
   *          the StringBuffer to which the normalized string will be appended
   */
  public static void normalize(String aStr, StringBuffer aResultBuf) {
    normalize(aStr, aResultBuf, false);
  }

  /**
   * Normalizes the given string for output to XML. This converts all special characters, e.g. &lt;,
   * %gt;, &amp;, to their XML representations, e.g. &amp;lt;, &amp;gt;, &amp;amp;. Also may convert
   * newlines to spaces, depending on the <code>aNewlinesToSpaces</code> parameter. The normalized
   * string is appended to the specified StringBuffer.
   * 
   * @param aStr
   *          input string
   * @param aNewlinesToSpaces
   *          iff true, newlines (\r and \n) will be converted to spaces
   * @param aResultBuf
   *          the StringBuffer to which the normalized string will be appended
   */
  public static void normalize(String aStr, StringBuffer aResultBuf, boolean aNewlinesToSpaces) {
    if (aStr != null) {
      int len = aStr.length();
      for (int i = 0; i < len; i++) {
        char c = aStr.charAt(i);
        if (c > 0x7F) {
          aResultBuf.append("&#").append((int) c).append(';');
        } else {
          switch (c) {
          case '<':
            aResultBuf.append("&lt;");
            break;
          case '>':
            aResultBuf.append("&gt;");
            break;
          case '&':
            aResultBuf.append("&amp;");
            break;
          case '"':
            aResultBuf.append("&quot;");
            break;
          case '\n':
            aResultBuf.append(aNewlinesToSpaces ? " " : "\n");
            break;
          case '\r':
            aResultBuf.append(aNewlinesToSpaces ? " " : "\r");
            break;
          default:
            aResultBuf.append(c);
          }
        }
      }
    }
  }

  /**
   * Normalizes the given string for output to XML, and writes the normalized string to the given
   * Writer. Normalization converts all special characters, e.g. &lt;, %gt;, &amp;, to their XML
   * representations, e.g. &amp;lt;, &amp;gt;, &amp;amp;. Also may convert newlines to spaces,
   * depending on the <code>aNewlinesToSpaces</code> parameter.
   * 
   * @param aStr
   *          input string
   * @param aWriter
   *          a Writer to which the normalized string will be written
   * @param aNewlinesToSpaces
   *          iff true, newlines (\r and \n) will be converted to spaces
   * 
   * @throws IOException
   *           if an I/O failure occurs when writing to <code>aWriter</code>
   */
  public static void writeNormalizedString(String aStr, Writer aWriter, boolean aNewlinesToSpaces)
      throws IOException {
    if (aStr == null)
      return;

    int len = aStr.length();
    for (int i = 0; i < len; i++) {
      char c = aStr.charAt(i);
      switch (c) {
      case '<':
        aWriter.write("&lt;");
        break;
      case '>':
        aWriter.write("&gt;");
        break;
      case '&':
        aWriter.write("&amp;");
        break;
      case '"':
        aWriter.write("&quot;");
        break;
      case '\n':
        aWriter.write(aNewlinesToSpaces ? " " : "\n");
        break;
      case '\r':
        aWriter.write(aNewlinesToSpaces ? " " : "\r");
        break;
      default:
        aWriter.write(c);
      }
    }
  }

  /**
   * Writes a standard XML representation of the specified Object, in the form:<br>
   * <code>&lt;className&gt;string value%lt;/className%gt;</code>
   * <p>
   * where <code>className</code> is the object's java class name without the package and made
   * lowercase, e.g. "string","integer", "boolean" and <code>string value</code> is the result of
   * <code>Object.toString()</code>.
   * <p>
   * This is intended to be used for Java Strings and wrappers for primitive value classes (e.g.
   * Integer, Boolean).
   * 
   * @param aObj
   *          the object to write
   * @param aWriter
   *          a Writer to which the XML will be written
   * 
   * @throws IOException
   *           if an I/O failure occurs when writing to <code>aWriter</code>
   */
  public static void writePrimitiveValue(Object aObj, Writer aWriter) throws IOException {
    String className = aObj.getClass().getName();
    int lastDotIndex = className.lastIndexOf(".");
    if (lastDotIndex > -1)
      className = className.substring(lastDotIndex + 1).toLowerCase();

    aWriter.write("<");
    aWriter.write(className);
    aWriter.write(">");

    writeNormalizedString(aObj.toString(), aWriter, true);

    aWriter.write("</");
    aWriter.write(className);
    aWriter.write(">");
  }

  
  // This method moved to MetaDataObject_impl, made private non-static, and integrated with the comment/whitespace preserving change. 6/2012 schor
//  /**
//   * Writes a standard XML representation of the specified Object, in the form:<br>
//   * <code>&lt;className&gt;string value%lt;/className%gt;</code>
//   * <p>
//   * where <code>className</code> is the object's java class name without the package and made
//   * lowercase, e.g. "string","integer", "boolean" and <code>string value</code> is the result of
//   * <code>Object.toString()</code>.
//   * <p>
//   * This is intended to be used for Java Strings and wrappers for primitive value classes (e.g.
//   * Integer, Boolean).
//   * 
//   * @param aObj
//   *          the object to write
//   * @param aContentHandler
//   *          the SAX ContentHandler to which events will be sent
//   * 
//   * @throws SAXException
//   *           if the ContentHandler throws an exception
//   */
//  public static void writePrimitiveValue(Object aObj, ContentHandler aContentHandler)
//      throws SAXException {
//    final Attributes EMPTY_ATTRIBUTES = new AttributesImpl();
//
//    String className = aObj.getClass().getName();
//    int lastDotIndex = className.lastIndexOf(".");
//    if (lastDotIndex > -1)
//      className = className.substring(lastDotIndex + 1).toLowerCase();
//
//    aContentHandler.startElement(null, className, className, EMPTY_ATTRIBUTES);
//    String valStr = aObj.toString();
//    aContentHandler.characters(valStr.toCharArray(), 0, valStr.length());
//    aContentHandler.endElement(null, className, className);
//  }

  /**
   * Gets the first child of the given Element with the given tag name.
   * 
   * @param aElem
   *          the parent element
   * @param aName
   *          tag name of the child to retrieve
   * 
   * @return the first child of <code>aElem</code> with tag name <code>aName</code>,
   *         <code>null</code> if there is no such child.
   */
  public static Element getChildByTagName(Element aElem, String aName) {
    NodeList matches = aElem.getElementsByTagName(aName);
    // this gets all descendants - we just want children
    for (int i = 0; i < matches.getLength(); i++) {
      Element childElem = (Element) matches.item(i);
      if (childElem.getParentNode() == aElem) {
        return childElem;
      }
    }
    // if we get here, no child was found
    return null;
  }

  /**
   * Gets the first child of the given Element.
   * 
   * @param aElem
   *          the parent element
   * 
   * @return the first child of <code>aElem</code>, <code>null</code> if it has no children.
   */
  public static Element getFirstChildElement(Element aElem) {
    NodeList children = aElem.getChildNodes();
    int len = children.getLength();
    for (int i = 0; i < len; i++) {
      Node curNode = children.item(i);
      if (curNode instanceof Element) {
        return (Element) curNode;
      }
    }
    return null;
  }

  /**
   * Reads a primitive value from its standard DOM representation. (This is the representation
   * produced by {@link #writePrimitiveValue(Object, Writer)}.
   * <p>
   * This is intended to be used for Java Strings and wrappers for primitive value classes (e.g.
   * Integer, Boolean).
   * 
   * @param aElem
   *          the element representing the value
   * 
   * @return the value that was read, <code>null</code> if a primitive value could not be
   *         constructed from the element
   */
  public static Object readPrimitiveValue(Element aElem) {
    // the element's tag name is the lowercase name of the class, minus the
    // package name
    String tagName = aElem.getTagName();
    // Hack: strip of trailing _p to get backwards compatibility with CPE descriptors
    // that were used in UIMA SDK v1.3.0 - 1.3.2.
    if (tagName.endsWith("_p")) {
      tagName = tagName.substring(0, tagName.lastIndexOf("_p"));
    }

    char[] chars = tagName.toCharArray();
    chars[0] = Character.toUpperCase(chars[0]);

    // the package name must be java.lang
    String className = "java.lang." + new String(chars);

    // the element's text is the string representation of the object
    String stringifiedObject = getText(aElem, true);

    try {
      // load class
      Class<?> theClass = Class.forName(className);
      // must have a constructor that takes a String parameter
      Constructor<?> constructor = theClass.getConstructor(new Class[] { String.class });
      // construct the object and return it
      return constructor.newInstance(new Object[] { stringifiedObject });
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Gets the text of this Element. Leading and trailing whitespace is removed.
   * 
   * @param aElem
   *          the element
   * 
   * @return the text of <code>aElem</code>
   */
  public static String getText(Element aElem) {
    StringBuffer buf = new StringBuffer();
    NodeList children = aElem.getChildNodes();
    int len = children.getLength();
    for (int i = 0; i < len; i++) {
      Node curNode = children.item(i);
      if (curNode instanceof Text) {
        buf.append(((Text) curNode).getData());
      } else if (curNode instanceof Element) {
        buf.append('<').append(((Element) curNode).getTagName()).append('>');
        buf.append(getText((Element) curNode));
        buf.append("</").append(((Element) curNode).getTagName()).append('>');
      }
    }

    return buf.toString().trim();
  }

  /**
   * Gets the text of this Element. Leading and trailing whitespace is removed. Environment variable
   * references of the form &lt;envVarRef%gt;PARAM_NAME&lt;/envVarRef&gt; may be expanded.
   * 
   * @param aElem
   *          the element
   * @param aExpandEnvVarRefs
   *          whether to expand environment variable references. Defaults to false.
   * 
   * @return the text of <code>aElem</code>
   */
  public static String getText(Element aElem, boolean aExpandEnvVarRefs) {
    StringBuffer buf = new StringBuffer();
    NodeList children = aElem.getChildNodes();
    int len = children.getLength();
    for (int i = 0; i < len; i++) {
      Node curNode = children.item(i);
      if (curNode instanceof Text) {
        buf.append(((Text) curNode).getData());
      } else if (curNode instanceof Element) {
        Element subElem = (Element) curNode;
        if (aExpandEnvVarRefs && "envVarRef".equals(subElem.getTagName())) {
          String varName = getText(subElem, false);
          String value = System.getProperty(varName);
          if (value != null) {
            buf.append(value);
          }
        } else {
          buf.append('<').append(((Element) curNode).getTagName()).append('>');
          buf.append(getText((Element) curNode, aExpandEnvVarRefs));
          buf.append("</").append(((Element) curNode).getTagName()).append('>');
        }
      }
    }

    return buf.toString().trim();
  }

  /**
   * Check the input string for non-XML 1.0 characters. If non-XML characters are found, return the
   * position of first offending character. Else, return <code>-1</code>.
   * 
   * <p>
   * From the XML 1.0 spec:
   * 
   * <pre>
   *   Char ::= #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF] // any Unicode
   *    character, excluding the surrogate blocks, FFFE, and FFFF.  
   * </pre>
   * 
   * <p>
   * And from the UTF-16 spec:
   * 
   * <p>
   * Characters with values between 0x10000 and 0x10FFFF are represented by a 16-bit integer with a
   * value between 0xD800 and 0xDBFF (within the so-called high-half zone or high surrogate area)
   * followed by a 16-bit integer with a value between 0xDC00 and 0xDFFF (within the so-called
   * low-half zone or low surrogate area).
   * 
   * @param s
   *          Input string
   * @return The position of the first invalid XML character encountered. <code>-1</code> if no
   *         invalid XML characters found.
   */
  public static final int checkForNonXmlCharacters(String s) {
    return checkForNonXmlCharacters(s, false);
  }
  
  /**
   * Check the input string for non-XML characters. If non-XML characters are found, return the
   * position of first offending character. Else, return <code>-1</code>.
   * <p>
   * The definition of an XML character is different for
   * XML 1.0 and 1.1.  This method will check either version, depending on the value of the
   * <code>xml11</code> argument.  
   * 
   * <p>
   * From the XML 1.0 spec:
   * 
   * <pre>
   *   Char ::= #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF] // any Unicode
   *    character, excluding the surrogate blocks, FFFE, and FFFF.  
   * </pre>
   * 
   * <p>
   * From the XML 1.1 spec:
   * <pre>
   *  Char     ::=    [#x1-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
   * </pre>
   * <p>
   * And from the UTF-16 spec:
   * 
   * <p>
   * Characters with values between 0x10000 and 0x10FFFF are represented by a 16-bit integer with a
   * value between 0xD800 and 0xDBFF (within the so-called high-half zone or high surrogate area)
   * followed by a 16-bit integer with a value between 0xDC00 and 0xDFFF (within the so-called
   * low-half zone or low surrogate area).
   * 
   * @param s
   *          Input string
   * @param xml11 true to check for invalid XML 1.1 characters, false to check for invalid XML 1.0 characters.
   *   The default is false.
   * @return The position of the first invalid XML character encountered. <code>-1</code> if no
   *         invalid XML characters found.
   */  
  public static final int checkForNonXmlCharacters(String s, boolean xml11) {
    if (s == null) {
      return -1;
    }
    char c;
    for (int i = 0; i < s.length(); i++) {
      c = s.charAt(i);
      if (isValidXmlUtf16int(c, xml11)) {
        // The easy case: this code unit is ok by itself, no further checking required.
        continue;
      }
      if ((c >= 0xD800) && (c <= 0xDBFF)) {
        // The case for Unicode code points #x10000-#x10FFFF. Check if a high surrogate is followed
        // by a low surrogate, which is the only allowable combination.
        int iNext = i + 1;
        if (iNext < s.length()) {
          char cNext = s.charAt(iNext);
          if ((cNext >= 0xDC00) && (cNext <= 0xDFFF)) {
            ++i;
            continue;
          }
        }
      }
      return i;
    }
    return -1;
  }

  /**
   * Check the input character array for non-XML characters. If non-XML characters are found, return the
   * position of first offending character. Else, return <code>-1</code>.
   * 
   * @param ch Input character array
   * @param start offset of first char to check
   * @param length number of chars to check
   * @param xml11 true to check for invalid XML 1.1 characters, false to check for invalid XML 1.0 characters.
   *   The default is false.
   * @return The position of the first invalid XML character encountered. <code>-1</code> if no
   *         invalid XML characters found.
   * @see #checkForNonXmlCharacters(String, boolean) 
   */  
  public static final int checkForNonXmlCharacters(char[] ch, int start, int length, boolean xml11) {
    if (ch == null) {
      return -1;
    }
    char c;
    for (int i = start; i < start + length; i++) {
      c = ch[i];
      if (isValidXmlUtf16int(c, xml11)) {
        // The easy case: this code unit is ok by itself, no further checking required.
        continue;
      }
      if ((c >= 0xD800) && (c <= 0xDBFF)) {
        // The case for Unicode code points #x10000-#x10FFFF. Check if a high surrogate is followed
        // by a low surrogate, which is the only allowable combination.
        int iNext = i + 1;
        if (iNext < start + length) {
          char cNext = ch[iNext];
          if ((cNext >= 0xDC00) && (cNext <= 0xDFFF)) {
            ++i;
            continue;
          }
        }
      }
      return i;
    }
    return -1;
  }  
  
  // Check if the utf 16 code unit we're looking at is a valid XML character in its own right.
  private static final boolean isValidXmlUtf16int(char c, boolean xml11) {
    if (xml11)
      return (c >= 0x1 && c <= 0xD7FF) || (c >= 0xE000) && (c <= 0xFFFD);
    else
      return ((c == 0x9) || (c == 0xA) || (c == 0xD) || ((c >= 0x20) && (c <= 0xD7FF)) || 
        (c >= 0xE000 && c <= 0xFFFD));
  }

  public static SAXParserFactory createSAXParserFactory() {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    try {
      factory.setFeature(DISALLOW_DOCTYPE_DECL, true);
    } catch (SAXNotRecognizedException e) {
      UIMAFramework.getLogger().log(Level.WARNING, 
          "SAXParserFactory didn't recognize feature " + DISALLOW_DOCTYPE_DECL);
    } catch (SAXNotSupportedException e) {
      UIMAFramework.getLogger().log(Level.WARNING, 
          "SAXParserFactory doesn't support feature " + DISALLOW_DOCTYPE_DECL);
    } catch (ParserConfigurationException e) {
      UIMAFramework.getLogger().log(Level.WARNING, 
          "SAXParserFactory doesn't support feature " + DISALLOW_DOCTYPE_DECL);
    }
    
    try {
      factory.setFeature(LOAD_EXTERNAL_DTD, false);
    } catch (SAXNotRecognizedException e) {
      UIMAFramework.getLogger().log(Level.WARNING, 
          "SAXParserFactory didn't recognize feature " + LOAD_EXTERNAL_DTD);
    } catch (SAXNotSupportedException e) {
      UIMAFramework.getLogger().log(Level.WARNING, 
          "SAXParserFactory doesn't support feature " + LOAD_EXTERNAL_DTD);
    } catch (ParserConfigurationException e) {
      UIMAFramework.getLogger().log(Level.WARNING, 
          "SAXParserFactory doesn't support feature " + LOAD_EXTERNAL_DTD);
    }
    
    factory.setXIncludeAware(false);
    return factory;
  }
  
  public static XMLReader createXMLReader() throws SAXException {
    XMLReader xmlReader = XMLReaderFactory.createXMLReader();
    try {
      xmlReader.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
    } catch (SAXNotRecognizedException e) {
      UIMAFramework.getLogger().log(Level.WARNING, 
          "XMLReader didn't recognize feature " + EXTERNAL_GENERAL_ENTITIES);
    } catch (SAXNotSupportedException e) {
      UIMAFramework.getLogger().log(Level.WARNING, 
          "XMLReader doesn't support feature " + EXTERNAL_GENERAL_ENTITIES);
    }

    try {
      xmlReader.setFeature(EXTERNAL_PARAMETER_ENTITIES, false);
    } catch (SAXNotRecognizedException e) {
      UIMAFramework.getLogger().log(Level.WARNING, 
          "XMLReader didn't recognize feature " + EXTERNAL_PARAMETER_ENTITIES);
    } catch (SAXNotSupportedException e) {
      UIMAFramework.getLogger().log(Level.WARNING, 
          "XMLReader doesn't support feature " + EXTERNAL_PARAMETER_ENTITIES);
    }

    try {
      xmlReader.setFeature(LOAD_EXTERNAL_DTD,false);
    } catch (SAXNotRecognizedException e) {
      UIMAFramework.getLogger().log(Level.WARNING, 
          "XMLReader didn't recognized feature " + LOAD_EXTERNAL_DTD);
    } catch (SAXNotSupportedException e) {
      UIMAFramework.getLogger().log(Level.WARNING, 
          "XMLReader doesn't support feature " + LOAD_EXTERNAL_DTD);
    }

    return xmlReader;
  }
  
  public static SAXTransformerFactory createSaxTransformerFactory() {
    SAXTransformerFactory saxTransformerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();    
    try {
      saxTransformerFactory.setAttribute(ACCESS_EXTERNAL_DTD, "");
    } catch (IllegalArgumentException e) {
      UIMAFramework.getLogger().log(Level.WARNING, 
          "SAXTransformerFactory didn't recognize setting attribute " + ACCESS_EXTERNAL_DTD);
    }

    try {
      saxTransformerFactory.setAttribute(ACCESS_EXTERNAL_STYLESHEET, "");
    } catch (IllegalArgumentException e) {
      UIMAFramework.getLogger().log(Level.WARNING, 
          "SAXTransformerFactory didn't recognize setting attribute " + ACCESS_EXTERNAL_STYLESHEET);
    }

    return saxTransformerFactory;
  }

  public static TransformerFactory createTransformerFactory() {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    try {
      transformerFactory.setAttribute(ACCESS_EXTERNAL_DTD, "");
    } catch (IllegalArgumentException e) {
      UIMAFramework.getLogger().log(Level.WARNING, 
          "TransformerFactory didn't recognize setting attribute " + ACCESS_EXTERNAL_DTD);
    }
    
    try {
      transformerFactory.setAttribute(ACCESS_EXTERNAL_STYLESHEET, "");
    } catch (IllegalArgumentException e) {
      UIMAFramework.getLogger().log(Level.WARNING, 
          "TransformerFactory didn't recognize setting attribute " + ACCESS_EXTERNAL_STYLESHEET);
    }

    return transformerFactory;
  }
  
  public static DocumentBuilderFactory createDocumentBuilderFactory() { 
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    try {
      documentBuilderFactory.setFeature(DISALLOW_DOCTYPE_DECL, true);
    } catch (ParserConfigurationException e1) {
      UIMAFramework.getLogger().log(Level.WARNING, 
          "DocumentBuilderFactory didn't recognize setting feature " + DISALLOW_DOCTYPE_DECL);
    }
    
    try {
      documentBuilderFactory.setFeature(LOAD_EXTERNAL_DTD, false);
    } catch (ParserConfigurationException e) {
      UIMAFramework.getLogger().log(Level.WARNING, 
          "DocumentBuilderFactory doesn't support feature " + LOAD_EXTERNAL_DTD);
    }
    
    documentBuilderFactory.setXIncludeAware(false);
    documentBuilderFactory.setExpandEntityReferences(false);
    
    return documentBuilderFactory;
  }
}

