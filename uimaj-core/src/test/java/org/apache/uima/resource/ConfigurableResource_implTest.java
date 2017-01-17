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

package org.apache.uima.resource;

import java.util.Map;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.impl.ResourceCreationSpecifier_impl;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.impl.ConfigurationParameter_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.ResourceMetaData_impl;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;


public class ConfigurableResource_implTest extends TestCase {

  /**
   * Constructor for ConfigurableResource_implTest.
   * 
   * @param arg0
   */
  public ConfigurableResource_implTest(String arg0) {
    super(arg0);
  }

  public void testReconfigure() throws Exception {
    try {
      // set up some resource metadata and create a resource
      ResourceCreationSpecifier specifier = new MyTestSpecifier();
      ResourceMetaData md = specifier.getMetaData();
      md.setName("TestResource");
      md.setDescription("Resource used for Testing the Resource_impl base class");
      ConfigurationParameter p1 = new ConfigurationParameter_impl();
      p1.setName("StringParam");
      p1.setDescription("parameter with String data type");
      p1.setType(ConfigurationParameter.TYPE_STRING);
      ConfigurationParameter p2 = new ConfigurationParameter_impl();
      p2.setName("IntegerParam");
      p2.setDescription("parameter with Integer data type");
      p2.setType(ConfigurationParameter.TYPE_INTEGER);
      ConfigurationParameter p3 = new ConfigurationParameter_impl();
      p3.setName("BooleanParam");
      p3.setDescription("parameter with Boolean data type");
      p3.setType(ConfigurationParameter.TYPE_BOOLEAN);
      ConfigurationParameter p4 = new ConfigurationParameter_impl();
      p4.setName("FloatParam");
      p4.setDescription("parameter with Float data type");
      p4.setType(ConfigurationParameter.TYPE_FLOAT);
      ConfigurationParameter p5 = new ConfigurationParameter_impl();
      p5.setName("StringArrayParam");
      p5.setDescription("mutli-valued parameter with String data type");
      p5.setType(ConfigurationParameter.TYPE_STRING);
      p5.setMultiValued(true);
      ConfigurationParameter p6 = new ConfigurationParameter_impl();
      p6.setName("IntegerArrayParam");
      p6.setDescription("multi-valued parameter with Integer data type");
      p6.setType(ConfigurationParameter.TYPE_INTEGER);
      p6.setMultiValued(true);
      ConfigurationParameter p7 = new ConfigurationParameter_impl();
      p7.setName("BooleanArrayParam");
      p7.setDescription("multi-valued parameter with Boolean data type");
      p7.setType(ConfigurationParameter.TYPE_BOOLEAN);
      p7.setMultiValued(true);
      ConfigurationParameter p8 = new ConfigurationParameter_impl();
      p8.setName("FloatArrayParam");
      p8.setDescription("multi-valued parameter with Float data type");
      p8.setType(ConfigurationParameter.TYPE_FLOAT);
      p8.setMultiValued(true);
      md.getConfigurationParameterDeclarations().setConfigurationParameters(
              new ConfigurationParameter[] { p1, p2, p3, p4, p5, p6, p7, p8 });
      ConfigurableResource testResource1 = new MyTestResource();
      testResource1.initialize(specifier, null);

      // valid settings
      String[] paramNames = { "StringParam", "BooleanParam", "IntegerParam", "FloatParam",
          "StringArrayParam", "BooleanArrayParam", "IntegerArrayParam", "FloatArrayParam" };

      String theStr = "hello world";
      Boolean theBool = Boolean.valueOf(false);
      Integer theInt = Integer.valueOf(42);
      Float theFloat = Float.valueOf(2.718281828459045F);
      String[] theStrArr = { "the", "quick", "brown", "fox" };
      Boolean[] theBoolArr = { Boolean.valueOf(false), Boolean.valueOf(true) };
      Integer[] theIntArr = { Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3) };
      Float[] theFloatArr = { Float.valueOf(3.0F), Float.valueOf(3.1F), Float.valueOf(3.14F) };

      Object[] values = new Object[] { theStr, theBool, theInt, theFloat, theStrArr, theBoolArr,
          theIntArr, theFloatArr };

      for (int i = 0; i < paramNames.length; i++) {
        testResource1.setConfigParameterValue(paramNames[i], values[i]);
      }
      testResource1.reconfigure();

      // check
      for (int i = 0; i < paramNames.length; i++) {
        Object val = testResource1.getConfigParameterValue(paramNames[i]);
        Assert.assertEquals(val, values[i]);
      }

      // invalid settings
      // wrong type
      Exception ex = null;
      testResource1.setConfigParameterValue("StringParam", Integer.valueOf(13));
      try {
        testResource1.reconfigure();
      } catch (ResourceConfigurationException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      testResource1.setConfigParameterValue("IntegerArrayParam", new Object[] { "A", "B", "C" });
      try {
        testResource1.reconfigure();
      } catch (ResourceConfigurationException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // inappropriate array
      ex = null;
      testResource1.setConfigParameterValue("FloatParam", new Float[] { Float.valueOf(0.1F),
          Float.valueOf(0.2F), Float.valueOf(0.3F) });
      try {
        testResource1.reconfigure();
      } catch (ResourceConfigurationException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // array required
      ex = null;
      testResource1.setConfigParameterValue("BooleanArrayParam", Boolean.valueOf(true));
      try {
        testResource1.reconfigure();
      } catch (ResourceConfigurationException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // required parameter set to null
      ex = null;
      testResource1.setConfigParameterValue("StringParam", null);
      try {
        testResource1.reconfigure();
      } catch (ResourceConfigurationException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // Now try a resource that defines configuration groups
      // (instantiate metadata from XML TAE descriptor because it's convenient)
      XMLInputSource in = new XMLInputSource(JUnitExtension
              .getFile("ConfigurableResourceImplTest/AnnotatorWithConfigurationGroups.xml"));
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      ResourceMetaData metadata = desc.getMetaData();
      MyTestSpecifier spec = new MyTestSpecifier();
      spec.setMetaData(metadata);
      ConfigurableResource testResource2 = new MyTestResource();
      testResource2.initialize(spec, null);

      // valid settings
      String[] groupNames = new String[] { "en", "en-US", "de", "zh" };
      String[][] grpParamNames = new String[][] {
          new String[] { "StringParam", "StringArrayParam", "IntegerParam", "IntegerArrayParam" },
          new String[] { "StringParam", "StringArrayParam", "IntegerParam", "IntegerArrayParam" },
          new String[] { "StringParam", "StringArrayParam", "IntegerParam", "IntegerArrayParam" },
          new String[] { "StringParam", "StringArrayParam", "FloatParam", "FloatArrayParam" } };
      Object[][] grpValues = new Object[][] {
          new Object[] { "test", new String[] { "foo", "bar" }, Integer.valueOf(1024),
              new Integer[] { Integer.valueOf(1), Integer.valueOf(3), Integer.valueOf(5) } },
          new Object[] { "blah", new String[] { "abc", "def" }, Integer.valueOf(32768),
              new Integer[] { Integer.valueOf(-1), Integer.valueOf(-3), Integer.valueOf(-5) } },
          new Object[] { "?", new String[] { "+", "-" }, Integer.valueOf(112376),
              new Integer[] { Integer.valueOf(-1), Integer.valueOf(0), Integer.valueOf(1) } },
          new Object[] { "different", new String[] { "test", "ing" }, Float.valueOf(49.95F),
              new Float[] { Float.valueOf(3.14F), Float.valueOf(2.71F), Float.valueOf(1.4F) } } };

      for (int i = 0; i < groupNames.length; i++) {
        String[] paramsInGrp = grpParamNames[i];
        for (int j = 0; j < paramsInGrp.length; j++) {
          testResource2.setConfigParameterValue(groupNames[i], paramsInGrp[j], grpValues[i][j]);
        }
      }
      testResource2.reconfigure();

      for (int i = 0; i < groupNames.length; i++) {
        String[] paramsInGrp = grpParamNames[i];
        for (int j = 0; j < paramsInGrp.length; j++) {
          Object val = testResource2.getConfigParameterValue(groupNames[i], paramsInGrp[j]);
          Assert.assertEquals(val, grpValues[i][j]);
        }
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  /*
   * Test for Object getConfigParameterValue(String)
   */
  public void testGetConfigParameterValueString() throws Exception {
    try {
      XMLInputSource in = new XMLInputSource(JUnitExtension
              .getFile("ConfigurableResourceImplTest/AnnotatorWithConfigurationGroups.xml"));
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
              in);
      AnalysisEngine test = UIMAFramework.produceAnalysisEngine(desc);

      // test default fallback
      String str3 = (String) test.getConfigParameterValue("StringParam");
      Assert.assertEquals("en", str3);

      // The below was commented out because it has a dependency on the jedii_cpe_impl module
      // //XMLInputSource in = new
      // XMLInputSource("CasConsumerWithDefaultFallbackConfiguration.xml");
      // XMLInputSource in = new XMLInputSource("GroupCasConsumer.xml");
      // CasConsumerDescription desc = UIMAFramework.getXMLParser().parseCasConsumerDescription(in);
      // //ConfigurableResource testResource1 = new TestResource();
      // CasConsumer test = UIMAFramework.produceCasConsumer(desc);
      // // testResource1.initialize(desc, null);
      //    
      // //test default fallback
      // String str3 = (String)test.getConfigParameterValue("FeatureIndex");
      // Assert.assertEquals("test",str3);
      //    
      // String[] strArr2 = (String[])testResource1.getConfigParameterValue("StringArrayParam");
      // Assert.assertEquals(4,strArr2.length);
      // Assert.assertEquals("t",strArr2[0]);
      // Assert.assertEquals("e",strArr2[1]);
      // Assert.assertEquals("s",strArr2[2]);
      // Assert.assertEquals("t",strArr2[3]);

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
}

/**
 * A simple concrete Resource class used for testing.
 */
class MyTestResource extends ConfigurableResource_ImplBase {
  public boolean initialize(ResourceSpecifier aSpecifier, Map aParams)
          throws ResourceInitializationException {
    return super.initialize(aSpecifier, aParams);
    // if (aSpecifier instanceof ResourceCreationSpecifier)
    // {
    // setMetaData(((ResourceCreationSpecifier)aSpecifier).getMetaData());
    // return true;
    // }
    //
    // return false;
  }

  public void destroy() {
    //do nothing
  }
}

class MyTestSpecifier extends ResourceCreationSpecifier_impl {
  public MyTestSpecifier() {
    setMetaData(new ResourceMetaData_impl());
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("testSpecifier",
          new PropertyXmlInfo[] { new PropertyXmlInfo("metaData", null, false), });

}
