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

import junit.framework.TestCase;

/**
 * Test XML utilities.
 */
public class XmlUtilsTest extends TestCase {

  /**
   */
  public XmlUtilsTest(String arg0) {
    super(arg0);
  }

  public void testXMLChars() {
    // Create a string with chars from all the valid ranges.
    char[] chars = new char[] { 0x9, 0xA, 0xD, 0x20, 0x25, 0xD7FF, 0xE000, 0xEFFF, 0xFFFD, 0xD800,
        0xDC00, 0xDBFF, 0xDFFF };
    String s = new String(chars);
    assertTrue(XMLUtils.checkForNonXmlCharacters(s) < 0);
    
    // The follownig utf 16 code units are not valid XML chars, or can not stand by themselves
    // (i.e., they're surrogates).
    chars = new char[] { 0x0, 0xDFFF, 0xD800, 0xDC00, 0xFFFF };
    for (int i = 0; i < chars.length; i++) {
      assertTrue(XMLUtils.checkForNonXmlCharacters(new String(new char[] { chars[i] })) == 0);
    }
    
    //The following characters are valid in XML 1.1 but not 1.0
    chars = new char[] {0x1, 0xB};
    for (int i = 0; i < chars.length; i++) {
      assertTrue(XMLUtils.checkForNonXmlCharacters(new String(new char[] { chars[i] })) == 0);
      assertTrue(XMLUtils.checkForNonXmlCharacters(new String(new char[] { chars[i] }), true) < 0);
    }
    
    //Check high surrogate followed by low surrogate (legal combination)
    assertTrue(XMLUtils.checkForNonXmlCharacters(new String(new char [] { 0xD800, 0xDC00})) < 0);
    
    // Check low surrogate followed by high surrogate (illegal combination).
    assertTrue(XMLUtils.checkForNonXmlCharacters(new String(new char [] { 0xDC00, 0xD800})) == 0);
  }

}
