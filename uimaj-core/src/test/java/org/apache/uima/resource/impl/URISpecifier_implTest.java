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

import static org.apache.uima.analysis_engine.impl.AnalysisEngineDescription_implTest.encoding;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.URISpecifier;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;

public class URISpecifier_implTest extends TestCase {
  URISpecifier_impl uriSpec;

  protected void setUp() throws Exception {
    uriSpec = new URISpecifier_impl();
    uriSpec.setProtocol("Vinci");
    uriSpec.setUri("foo.bar");
    uriSpec.setParameters(new Parameter[] { new Parameter_impl("VNS_HOST", "myhost"),
        new Parameter_impl("VNS_PORT", "42") });
  }

  public void testXmlization() throws Exception {
    try {
      StringWriter sw = new StringWriter();
      uriSpec.toXML(sw);
      URISpecifier uriSpec2 = (URISpecifier) UIMAFramework.getXMLParser().parse(
              new XMLInputSource(new ByteArrayInputStream(sw.getBuffer().toString().getBytes(encoding)),
                      null));
      assertEquals(uriSpec, uriSpec2);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
}
