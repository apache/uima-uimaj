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

package org.apache.uima.analysis_engine.impl.metadata;

import static org.apache.uima.analysis_engine.impl.AnalysisEngineDescription_implTest.encoding;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.metadata.impl.SofaMapping_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;

public class SofaMapping_implTest extends TestCase {
  SofaMapping_impl sm1;

  SofaMapping_impl sm2;

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    sm1 = new SofaMapping_impl();
    sm1.setAggregateSofaName("aggSofa");
    sm1.setComponentKey("myAnnotator");
    sm1.setComponentSofaName("compSofa");

    sm2 = new SofaMapping_impl();
    sm2.setAggregateSofaName("aggSofa");
    sm2.setComponentKey("myAnnotator2");
  }

  public void testXmlization() throws Exception {
    try {
      // write to XML
      StringWriter writer = new StringWriter();
      sm1.toXML(writer);
      String sm1Xml = writer.getBuffer().toString();
      writer = new StringWriter();
      sm2.toXML(writer);
      String sm2Xml = writer.getBuffer().toString();
      // parse from XML
      InputStream is = new ByteArrayInputStream(sm1Xml.getBytes(encoding));
      SofaMapping_impl newSm1 = (SofaMapping_impl) UIMAFramework.getXMLParser().parse(
              new XMLInputSource(is, null));
      is = new ByteArrayInputStream(sm2Xml.getBytes(encoding));
      SofaMapping_impl newSm2 = (SofaMapping_impl) UIMAFramework.getXMLParser().parse(
              new XMLInputSource(is, null));

      Assert.assertEquals(sm1, newSm1);
      Assert.assertEquals(sm2, newSm2);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

}
