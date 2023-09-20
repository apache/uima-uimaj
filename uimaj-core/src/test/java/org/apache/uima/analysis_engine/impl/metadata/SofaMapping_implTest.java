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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.uima.UIMAFramework.getXMLParser;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import org.apache.uima.analysis_engine.metadata.impl.SofaMapping_impl;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SofaMapping_implTest {
  SofaMapping_impl sm1;

  SofaMapping_impl sm2;

  @BeforeEach
  void setUp() throws Exception {
    sm1 = new SofaMapping_impl();
    sm1.setAggregateSofaName("aggSofa");
    sm1.setComponentKey("myAnnotator");
    sm1.setComponentSofaName("compSofa");

    sm2 = new SofaMapping_impl();
    sm2.setAggregateSofaName("aggSofa");
    sm2.setComponentKey("myAnnotator2");
  }

  @Test
  void testXmlization() throws Exception {
    // write to XML
    var writer = new StringWriter();
    sm1.toXML(writer);
    var sm1Xml = writer.toString();

    writer = new StringWriter();
    sm2.toXML(writer);
    var sm2Xml = writer.toString();

    // parse from XML
    try (var is = new ByteArrayInputStream(sm1Xml.getBytes(UTF_8))) {
      var newSm1 = (SofaMapping_impl) getXMLParser().parse(new XMLInputSource(is));
      assertThat(newSm1).isEqualTo(sm1);
    }

    try (var is = new ByteArrayInputStream(sm2Xml.getBytes(UTF_8))) {
      var newSm2 = (SofaMapping_impl) getXMLParser().parse(new XMLInputSource(is));
      assertThat(newSm2).isEqualTo(sm2);
    }
  }
}
