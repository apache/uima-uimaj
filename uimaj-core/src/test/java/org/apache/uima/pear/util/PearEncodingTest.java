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

import java.io.File;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.test.junit_extension.JUnitExtension;

/**
 * Pear encoding tests
 * 
 */
public class PearEncodingTest extends TestCase {

  public void testUTF8NoSignature() throws Exception {
    // get XML file
    File xmlFile = JUnitExtension.getFile("pearTests/encodingTests/UTF8_no_signature.xml");
    // get encoding
    String encoding = XMLUtil.detectXmlFileEncoding(xmlFile);

    // normalize encoding
    encoding = encoding.toUpperCase();

    Assert.assertTrue(encoding.equals("UTF-8"));
  }

  public void testUTF8WithSignature() throws Exception {
	// cancel this test for Sun's Java 1.3.x or 1.4.x - it does not support BOM
    String javaVendor = System.getProperty("java.vendor");
    if( javaVendor.startsWith("Sun") ) {
        String javaVersion = System.getProperty("java.version");
        if( javaVersion.startsWith("1.3") || javaVersion.startsWith("1.4") )
            return;
    }
    // get XML file
    File xmlFile = JUnitExtension.getFile("pearTests/encodingTests/UTF8_with_signature.xml");
    // get encoding
    String encoding = XMLUtil.detectXmlFileEncoding(xmlFile);

    // normalize encoding
    encoding = encoding.toUpperCase();

    Assert.assertTrue(encoding.equals("UTF-8"));
  }

  public void testUTF16NoSignature() throws Exception {
    
    //NOTE: this test fails when using SUN JVM 1.4.2_12
    
    // get XML file
    File xmlFile = JUnitExtension.getFile("pearTests/encodingTests/UTF16_no_signature.xml");
    // get encoding
    String encoding = XMLUtil.detectXmlFileEncoding(xmlFile);

    // normalize encoding
    encoding = encoding.toUpperCase();

    Assert.assertTrue(encoding.equals("UTF-16LE"));
  }

  public void testUTF16WithSignature() throws Exception {
    // cancel this test for Sun's Java 1.3.x or 1.4.x - it does not support BOM
    String javaVendor = System.getProperty("java.vendor");
    if( javaVendor.startsWith("Sun") ) {
        String javaVersion = System.getProperty("java.version");
        if( javaVersion.startsWith("1.3") || javaVersion.startsWith("1.4") )
            return;
    }
    // get XML file
    File xmlFile = JUnitExtension.getFile("pearTests/encodingTests/UTF16_with_signature.xml");
    // get encoding
    String encoding = XMLUtil.detectXmlFileEncoding(xmlFile);

    // normalize encoding
    encoding = encoding.toUpperCase();

    Assert.assertTrue(encoding.equals("UTF-16"));
  }
}
