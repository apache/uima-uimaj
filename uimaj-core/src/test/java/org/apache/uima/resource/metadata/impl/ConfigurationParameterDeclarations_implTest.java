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

package org.apache.uima.resource.metadata.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.uima.UIMAFramework;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class ConfigurationParameterDeclarations_implTest {
  @Test
  void testBuildFromXmlElement() throws Exception {
    // parse XML
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

    InputStream str = new FileInputStream(JUnitExtension
            .getFile("org/apache/uima/resource/metadata/impl/ConfigParamEmptyGroup.xml"));
    Document doc = docBuilder.parse(str);

    ConfigurationParameterDeclarations_impl obj = new ConfigurationParameterDeclarations_impl();
    obj.buildFromXMLElement(doc.getDocumentElement(), UIMAFramework.getXMLParser());

    assertThat(obj.getConfigurationGroups()).hasSize(1);
  }
}
