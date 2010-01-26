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

package org.apache.vinci.transport.util;

/**
 * Methods for converting to / from regular strings and XML(entity ref)-encoded strings, as well as
 * HTML-formatted strings.
 */
public class XMLConverter {

  /**
   * Private constructor -- this class not meant to be instantiated.
   */
  private XMLConverter() {
  }

  /**
   * Convert to XML string format. Uses CDATA encoding if there are more than 4 characters requiring
   * ampersand encoding.
   * 
   * @param convert_me
   * @return
   * 
   * @pre convert_me != null
   */
  static public String convertStringToXMLString(String convert_me) {
    StringBuffer tmp = new StringBuffer();
    convertStringToXMLString(convert_me, tmp);
    return tmp.toString();
  }

  /**
   * Convert to XML string format without ever using CDATA encoding.
   * 
   * @param convert_me
   * @return
   * 
   * @pre convert_me != null
   */
  static public String simpleConvertStringToXMLString(String convert_me) {
    StringBuffer tmp = new StringBuffer();
    simpleConvertStringToXMLString(convert_me, tmp);
    return tmp.toString();
  }

  /**
   * 
   * @param convert_me
   * @param append_to_me
   * 
   * @pre convert_me != null
   * @pre append_to_me != null
   */
  static public void simpleConvertStringToXMLString(String convert_me, StringBuffer append_to_me) {
    for (int i = 0; i < convert_me.length(); i++) {
      switch (convert_me.charAt(i)) {
        case '<':
          append_to_me.append("&lt;");
          break;
        case '>':
          append_to_me.append("&gt;");
          break;
        case '&':
          append_to_me.append("&amp;");
          break;
        case '\'':
          append_to_me.append("&apos;");
          break;
        case '\"':
          append_to_me.append("&quot;");
          break;
        default:
          append_to_me.append(convert_me.charAt(i));
      }
    }
  }

  /**
   * @param convert_me
   * @param append_to_me
   * 
   * @pre convert_me != null
   * @pre append_to_me != null
   */
  static public void convertStringToXMLString(String convert_me, StringBuffer append_to_me) {
    int index = 0;
    int special_char_count = 0;
    while (special_char_count <= 4) {
      if (index == convert_me.length()) {
        simpleConvertStringToXMLString(convert_me, append_to_me);
        return;
      }
      switch (convert_me.charAt(index)) {
        case '<':
        case '>':
        case '&':
        case '\'':
        case '\"':
          special_char_count++;
        default:
      }
      index++;
    }
    // There are more than 4 special characters, so we use a CDATA section
    append_to_me.append("<![CDATA[");
    int end = convert_me.indexOf("]]>");
    if (end != -1) {
      append_to_me.append(convert_me.substring(0, end));
      append_to_me.append("]]>]]");
      convertStringToXMLString(convert_me.substring(end + 2), append_to_me);
    } else {
      append_to_me.append(convert_me);
      append_to_me.append("]]>");
    }
  }

  /**
   * @pre convert_me != null
   */
  static public String convertStringToHTMLString(String convert_me) {
    StringBuffer buf = new StringBuffer();
    convertStringToHTMLString(convert_me, buf);
    return buf.toString();
  }

  /**
   * 
   * @param convert_me
   * @param append_to_me
   * 
   * @pre convert_me != null
   * @pre append_to_me != null
   */
  static public void convertStringToHTMLString(String convert_me, StringBuffer append_to_me) {
    for (int i = 0; i < convert_me.length(); i++) {
      switch (convert_me.charAt(i)) {
        case '<':
          append_to_me.append("&lt;");
          break;
        case '>':
          append_to_me.append("&gt;");
          break;
        case '&':
          append_to_me.append("&amp;");
          break;
        case '\'':
          append_to_me.append("&#39;");
          // ^^ USed to use "&apos;" but IE doesn't display
          // that correctly.
          break;
        case '\"':
          append_to_me.append("&quot;");
          break;
        default:
          append_to_me.append(convert_me.charAt(i));
      }
    }
  }

  static public void main(String[] args) {
    if (args.length == 1) {
      System.out.println(convertStringToXMLString(args[0]));
    }
  }

} // class

