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

package org.apache.uima.resource.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.URISpecifier;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class URISpecifier_implTest {
  URISpecifier_impl sut;

  @BeforeEach
  void setUp() throws Exception {
    sut = new URISpecifier_impl();
    sut.setProtocol("Vinci");
    sut.setUri("foo.bar");
    sut.setParameters(new Parameter[] { //
        new Parameter_impl("VNS_HOST", "myhost"), //
        new Parameter_impl("VNS_PORT", "42") });
  }

  @Test
  void testXmlization() throws Exception {
    StringWriter sw = new StringWriter();
    sut.toXML(sw);

    try (var is = new ByteArrayInputStream(sw.toString().getBytes(UTF_8))) {
      URISpecifier uriSpec2 = (URISpecifier) UIMAFramework.getXMLParser()
              .parse(new XMLInputSource(is));
      assertEquals(sut, uriSpec2);
    }
  }
}
