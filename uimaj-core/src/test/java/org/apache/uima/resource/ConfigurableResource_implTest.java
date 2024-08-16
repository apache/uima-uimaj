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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.util.Arrays.array;

import java.util.Map;
import java.util.stream.Stream;

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
import org.junit.jupiter.api.Test;

public class ConfigurableResource_implTest {

  @Test
  public void testReconfigure() throws Exception {
    // set up some resource metadata and create a resource
    ConfigurationParameter pString = new ConfigurationParameter_impl();
    pString.setName("StringParam");
    pString.setDescription("parameter with String data type");
    pString.setType(ConfigurationParameter.TYPE_STRING);

    ConfigurationParameter pInteger = new ConfigurationParameter_impl();
    pInteger.setName("IntegerParam");
    pInteger.setDescription("parameter with Integer data type");
    pInteger.setType(ConfigurationParameter.TYPE_INTEGER);

    ConfigurationParameter pLong = new ConfigurationParameter_impl();
    pLong.setName("LongParam");
    pLong.setDescription("parameter with Long data type");
    pLong.setType(ConfigurationParameter.TYPE_LONG);

    ConfigurationParameter pBoolean = new ConfigurationParameter_impl();
    pBoolean.setName("BooleanParam");
    pBoolean.setDescription("parameter with Boolean data type");
    pBoolean.setType(ConfigurationParameter.TYPE_BOOLEAN);

    ConfigurationParameter pFloat = new ConfigurationParameter_impl();
    pFloat.setName("FloatParam");
    pFloat.setDescription("parameter with Float data type");
    pFloat.setType(ConfigurationParameter.TYPE_FLOAT);

    ConfigurationParameter pDouble = new ConfigurationParameter_impl();
    pDouble.setName("DoubleParam");
    pDouble.setDescription("parameter with Double data type");
    pDouble.setType(ConfigurationParameter.TYPE_DOUBLE);

    ConfigurationParameter pStringArray = new ConfigurationParameter_impl();
    pStringArray.setName("StringArrayParam");
    pStringArray.setDescription("mutli-valued parameter with String data type");
    pStringArray.setType(ConfigurationParameter.TYPE_STRING);
    pStringArray.setMultiValued(true);

    ConfigurationParameter pIntegerArray = new ConfigurationParameter_impl();
    pIntegerArray.setName("IntegerArrayParam");
    pIntegerArray.setDescription("multi-valued parameter with Integer data type");
    pIntegerArray.setType(ConfigurationParameter.TYPE_INTEGER);
    pIntegerArray.setMultiValued(true);

    ConfigurationParameter pLongArray = new ConfigurationParameter_impl();
    pLongArray.setName("LongArrayParam");
    pLongArray.setDescription("multi-valued parameter with Long data type");
    pLongArray.setType(ConfigurationParameter.TYPE_LONG);
    pLongArray.setMultiValued(true);

    ConfigurationParameter pBooleanArray = new ConfigurationParameter_impl();
    pBooleanArray.setName("BooleanArrayParam");
    pBooleanArray.setDescription("multi-valued parameter with Boolean data type");
    pBooleanArray.setType(ConfigurationParameter.TYPE_BOOLEAN);
    pBooleanArray.setMultiValued(true);

    ConfigurationParameter pFloatArray = new ConfigurationParameter_impl();
    pFloatArray.setName("FloatArrayParam");
    pFloatArray.setDescription("multi-valued parameter with Float data type");
    pFloatArray.setType(ConfigurationParameter.TYPE_FLOAT);
    pFloatArray.setMultiValued(true);

    ConfigurationParameter pDoubleArray = new ConfigurationParameter_impl();
    pDoubleArray.setName("DoubleArrayParam");
    pDoubleArray.setDescription("multi-valued parameter with Double data type");
    pDoubleArray.setType(ConfigurationParameter.TYPE_DOUBLE);
    pDoubleArray.setMultiValued(true);

    ConfigurationParameter[] allParameters = array(pString, pBoolean, pInteger, pLong, pFloat,
            pDouble, pStringArray, pBooleanArray, pIntegerArray, pLongArray, pFloatArray,
            pDoubleArray);

    ResourceCreationSpecifier specifier = new MyTestSpecifier();
    ResourceMetaData md = specifier.getMetaData();
    md.setName("TestResource");
    md.setDescription("Resource used for Testing the Resource_impl base class");
    md.getConfigurationParameterDeclarations().setConfigurationParameters(allParameters);
    ConfigurableResource testResource1 = new MyTestResource();
    testResource1.initialize(specifier, null);

    String[] paramNames = Stream.of(allParameters) //
            .map(ConfigurationParameter::getName) //
            .toArray(String[]::new);

    // valid settings
    String theStr = "hello world";
    Boolean theBool = FALSE;
    Integer theInt = 42;
    Long theLong = 43l;
    Float theFloat = 2.718281828459045F;
    Double theDouble = Double.MAX_VALUE;
    String[] theStrArr = { "the", "quick", "brown", "fox" };
    Boolean[] theBoolArr = { FALSE, TRUE };
    Integer[] theIntArr = { 1, 2, 3 };
    Long[] theLongArr = { Long.MIN_VALUE, Long.MAX_VALUE, 716231278687123l };
    Float[] theFloatArr = { 3.0F, 3.1F, 3.14F };
    Double[] theDoubleArr = { Double.MIN_VALUE, Double.MAX_VALUE, Double.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY, Double.NaN, 0.3123289172312d };

    Object[] values = new Object[] { theStr, theBool, theInt, theLong, theFloat, theDouble,
        theStrArr, theBoolArr, theIntArr, theLongArr, theFloatArr, theDoubleArr };

    for (int i = 0; i < paramNames.length; i++) {
      testResource1.setConfigParameterValue(paramNames[i], values[i]);
    }
    testResource1.reconfigure();

    // check
    for (int i = 0; i < paramNames.length; i++) {
      assertThat(testResource1.getConfigParameterValue(paramNames[i])).isEqualTo(values[i]);
    }

    // invalid settings
    // wrong type
    assertThatExceptionOfType(ResourceConfigurationException.class)
            .as("Parameter must reject value of inappropriate type").isThrownBy(() -> {
              testResource1.setConfigParameterValue("StringParam", 13);
              testResource1.reconfigure();
            });

    assertThatExceptionOfType(ResourceConfigurationException.class)
            .as("Array parameter must reject array value containing inappropriate types")
            .isThrownBy(() -> {
              testResource1.setConfigParameterValue("IntegerArrayParam",
                      new Object[] { "A", "B", "C" });
              testResource1.reconfigure();
            });

    assertThatExceptionOfType(ResourceConfigurationException.class)
            .as("Single-valued features require a primitive value, not an array").isThrownBy(() -> {
              testResource1.setConfigParameterValue("FloatParam", new Float[] { 0.1F, 0.2F, 0.3F });
              testResource1.reconfigure();
            });

    assertThatExceptionOfType(ResourceConfigurationException.class)
            .as("Multi-valued features require an array type, not a primitive value")
            .isThrownBy(() -> {
              testResource1.setConfigParameterValue("BooleanArrayParam", TRUE);
              testResource1.reconfigure();
            });

    assertThatExceptionOfType(ResourceConfigurationException.class)
            .as("Required parameter cannot ben ull").isThrownBy(() -> {
              testResource1.setConfigParameterValue("StringParam", null);
              testResource1.reconfigure();
            });

    // Now try a resource that defines configuration groups
    // (instantiate metadata from XML TAE descriptor because it's convenient)
    XMLInputSource in = new XMLInputSource(JUnitExtension
            .getFile("ConfigurableResourceImplTest/AnnotatorWithConfigurationGroups.xml"));
    AnalysisEngineDescription desc = UIMAFramework.getXMLParser()
            .parseAnalysisEngineDescription(in);
    ResourceMetaData metadata = desc.getMetaData();
    MyTestSpecifier spec = new MyTestSpecifier();
    spec.setMetaData(metadata);
    ConfigurableResource testResource2 = new MyTestResource();
    testResource2.initialize(spec, null);

    // valid settings
    String[] groupNames = new String[] { "en", "en-US", "de", "zh" };
    String[][] grpParamNames = {
        new String[] { "StringParam", "StringArrayParam", "IntegerParam", "IntegerArrayParam" },
        new String[] { "StringParam", "StringArrayParam", "IntegerParam", "IntegerArrayParam" },
        new String[] { "StringParam", "StringArrayParam", "IntegerParam", "IntegerArrayParam" },
        new String[] { "StringParam", "StringArrayParam", "FloatParam", "FloatArrayParam" } };
    Object[][] grpValues = {
        new Object[] { "test", new String[] { "foo", "bar" }, 1024, new Integer[] { 1, 3, 5 } },
        new Object[] { "blah", new String[] { "abc", "def" }, 32768, new Integer[] { -1, -3, -5 } },
        new Object[] { "?", new String[] { "+", "-" }, 112376, new Integer[] { -1, 0, 1 } },
        new Object[] { "different", new String[] { "test", "ing" }, 49.95F,
            new Float[] { 3.14F, 2.71F, 1.4F } } };

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
        assertThat(grpValues[i][j]).isEqualTo(val);
      }
    }
  }

  /*
   * Test for Object getConfigParameterValue(String)
   */
  @Test
  public void testGetConfigParameterValueString() throws Exception {
    try {
      XMLInputSource in = new XMLInputSource(JUnitExtension
              .getFile("ConfigurableResourceImplTest/AnnotatorWithConfigurationGroups.xml"));
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(in);
      AnalysisEngine test = UIMAFramework.produceAnalysisEngine(desc);

      // test default fallback
      String str3 = (String) test.getConfigParameterValue("StringParam");
      assertThat(str3).isEqualTo("en");

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
  @Override
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

  @Override
  public void destroy() {
    // do nothing
  }
}

class MyTestSpecifier extends ResourceCreationSpecifier_impl {
  private static final long serialVersionUID = 3128559748798504638L;

  public MyTestSpecifier() {
    setMetaData(new ResourceMetaData_impl());
  }

  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("testSpecifier",
          new PropertyXmlInfo[] { new PropertyXmlInfo("metaData", null, false), });

}
