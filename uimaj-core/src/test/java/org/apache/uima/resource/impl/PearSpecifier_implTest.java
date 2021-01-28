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
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.PearSpecifier;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;

import org.junit.Assert;
import junit.framework.TestCase;

/**
 * PearSpecifier creation and Xmlization test
 */
public class PearSpecifier_implTest extends TestCase {
 
  /*
   * pearSpecifier creation test
   */
  public void testProducePearResource() throws Exception {
    PearSpecifier specifier = UIMAFramework.getResourceSpecifierFactory().createPearSpecifier();
    specifier.setPearPath("/home/user/uimaApp/installedPears/testpear");
    specifier.setParameters(new Parameter_impl("legacyParam1", "legacyVal1"),
        new Parameter_impl("legacyParam2", "legacyVal2"));
    specifier.setPearParameters(new NameValuePair_impl("param1", "stringVal1"), 
        new NameValuePair_impl("param2", true));
      
    //compare created specifier with available test specifier
    XMLInputSource in = new XMLInputSource(
            JUnitExtension.getFile("XmlParserTest/TestPearSpecifier.xml"));
    PearSpecifier pearSpec = UIMAFramework.getXMLParser().parsePearSpecifier(in);
    
    assertThat(specifier.getPearPath()).isEqualTo(pearSpec.getPearPath());

    assertThat(specifier.getParameters())
        .usingElementComparatorOnFields("name", "value")
        .containsExactly(pearSpec.getParameters());

    assertThat(specifier.getPearParameters())
        .usingElementComparatorOnFields("name", "value")
        .containsExactly(pearSpec.getPearParameters());
    
    
    //compare created specifier with a manually create pear specifier
    PearSpecifier manPearSpec = new PearSpecifier_impl();
    manPearSpec.setPearPath("/home/user/uimaApp/installedPears/testpear");
    manPearSpec.setParameters(new Parameter_impl("legacyParam1", "legacyVal1"),
            new Parameter_impl("legacyParam2", "legacyVal2"));
    manPearSpec.setPearParameters(new NameValuePair_impl("param1", "stringVal1"),
        new NameValuePair_impl("param2", true));

    assertThat(specifier.getParameters())
        .usingElementComparatorOnFields("name", "value")
        .containsExactly(manPearSpec.getParameters());
    
    assertThat(specifier.getPearParameters())
        .usingElementComparatorOnFields("name", "value")
        .containsExactly(manPearSpec.getPearParameters());
  }
  
  /*
   * pearSpecifier xmlization test
   */
  public void testXmlization() throws Exception {
    try {
      PearSpecifier pearSpec = new PearSpecifier_impl();
      pearSpec.setPearPath("/home/user/uimaApp/installedPears/testpear");
      pearSpec.setParameters(new Parameter_impl("param1", "val1"),
              new Parameter_impl("param2", "val2"));
      pearSpec.setPearParameters(new NameValuePair[] { new NameValuePair_impl("param1", "val1"),
          new NameValuePair_impl("param2", "val2") });

      StringWriter sw = new StringWriter();
      pearSpec.toXML(sw);
      PearSpecifier pearSpec2 = (PearSpecifier) UIMAFramework.getXMLParser().parse(
              new XMLInputSource(new ByteArrayInputStream(sw.getBuffer().toString().getBytes(encoding)),
                      null));
      assertEquals(pearSpec, pearSpec2);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  
 
}
