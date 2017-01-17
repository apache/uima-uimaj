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

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.PearSpecifier;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;

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
    Parameter[] parameters = new Parameter[2];
    parameters[0] = UIMAFramework.getResourceSpecifierFactory().createParameter();
    parameters[0].setName("param1");
    parameters[0].setValue("val1");
    parameters[1] = UIMAFramework.getResourceSpecifierFactory().createParameter();
    parameters[1].setName("param2");
    parameters[1].setValue("val2");
    specifier.setParameters(parameters);  
      
    //compare created specifier with available test specifier
    XMLInputSource in = new XMLInputSource(
            JUnitExtension.getFile("XmlParserTest/TestPearSpecifier.xml"));
    PearSpecifier pearSpec = UIMAFramework.getXMLParser().parsePearSpecifier(in);
    
    Assert.assertEquals(pearSpec.getPearPath(), specifier.getPearPath());
    Assert.assertEquals(pearSpec.getParameters()[0].getValue(), specifier.getParameters()[0].getValue());
    Assert.assertEquals(pearSpec.getParameters()[1].getValue(), specifier.getParameters()[1].getValue());   
    
    //compare created specifier with a manually create pear specifier
    PearSpecifier manPearSpec = new PearSpecifier_impl();
    manPearSpec.setPearPath("/home/user/uimaApp/installedPears/testpear");
    manPearSpec.setParameters(new Parameter[] { new Parameter_impl("param1", "val1"),
        new Parameter_impl("param2", "val2") });

    Assert.assertEquals(manPearSpec.getPearPath(), specifier.getPearPath());
    Assert.assertEquals(manPearSpec.getParameters()[0].getValue(), specifier.getParameters()[0].getValue());
    Assert.assertEquals(manPearSpec.getParameters()[1].getValue(), specifier.getParameters()[1].getValue());   

  }
  
  /*
   * pearSpecifier xmlization test
   */
  public void testXmlization() throws Exception {
    try {
      PearSpecifier pearSpec = new PearSpecifier_impl();
      pearSpec.setPearPath("/home/user/uimaApp/installedPears/testpear");
      pearSpec.setParameters(new Parameter[] { new Parameter_impl("param1", "val1"),
          new Parameter_impl("param2", "val2") });

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
