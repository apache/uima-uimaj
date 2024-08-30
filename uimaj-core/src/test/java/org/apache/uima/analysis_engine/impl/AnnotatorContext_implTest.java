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

package org.apache.uima.analysis_engine.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.within;

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.annotator.AnnotatorContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.impl.TestResourceInterface;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @forRemoval 4.0.0 (Check if tests need to be migrated to UIMAContext)
 */
@Deprecated(since = "3.6.0")
class AnnotatorContext_implTest {
  protected final String TEST_DATAPATH = JUnitExtension.getFile("AnnotatorContextTest").getPath()
          + System.getProperty("path.separator") + JUnitExtension.getFile("ResourceTest");

  protected final String TEST_EXTENSION_CLASSPATH = JUnitExtension
          .getFile("ResourceTest/spaces in dir name").getPath();

  private AnnotatorContext mAC1;

  private AnnotatorContext mAC2;

  private AnnotatorContext mAC3;

  private AnnotatorContext mAC4;

  private AnnotatorContext mAC5;

  /*
   * @see TestCase#setUp()
   */
  @BeforeEach
  void setUp() throws Exception {
    // create primitive analysis engine with configuration groups
    var in = new XMLInputSource(
            JUnitExtension.getFile("AnnotatorContextTest/AnnotatorWithConfigurationGroups.xml"));
    var desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
    var tae = new PrimitiveAnalysisEngine_impl();
    // set data path just to test that we can get it later
    Map<String, Object> map = new HashMap<>();
    var rm = UIMAFramework.newDefaultResourceManager();
    rm.setDataPath(TEST_DATAPATH);
    rm.setExtensionClassPath(TEST_EXTENSION_CLASSPATH, true);
    map.put(AnalysisEngine.PARAM_RESOURCE_MANAGER, rm);
    tae.initialize(desc, map);
    // this should include an annotator context
    mAC1 = new AnnotatorContext_impl(tae.getUimaContextAdmin());

    // create aggregate analysis engine with configuration parameter overrides
    var in2 = new XMLInputSource(JUnitExtension
            .getFile("AnnotatorContextTest/AggregateTaeWithConfigParamOverrides.xml"));
    var aggDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in2);
    var aggTae = new AggregateAnalysisEngine_impl();
    aggTae.initialize(aggDesc, map);
    // get the primitive TAE
    var primTae = (PrimitiveAnalysisEngine_impl) aggTae._getASB().getComponentAnalysisEngines()
            .values().toArray()[0];
    // this should include an annotator context
    mAC2 = new AnnotatorContext_impl(primTae.getUimaContextAdmin());

    // create primitive analysis engine for resource testing
    var in3 = new XMLInputSource(
            JUnitExtension.getFile("AnnotatorContextTest/ResourceTestAnnotator.xml"));
    var resTestDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in3);
    var resTestTae = new PrimitiveAnalysisEngine_impl();
    resTestTae.initialize(resTestDesc, map);
    // this should include an annotator context
    mAC3 = new AnnotatorContext_impl(resTestTae.getUimaContextAdmin());

    // create primitive TAE with configuration groups and default fallback
    var in4 = new XMLInputSource(JUnitExtension
            .getFile("AnnotatorContextTest/AnnotatorWithDefaultFallbackConfiguration.xml"));
    var desc4 = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in4);
    var tae4 = new PrimitiveAnalysisEngine_impl();
    // set data path just to test that we can get it later
    tae4.initialize(desc4, null);
    // this should include an annotator context
    mAC4 = new AnnotatorContext_impl(tae4.getUimaContextAdmin());

    // create primitive TAE with configuration parameters (no groups)
    var in5 = new XMLInputSource(JUnitExtension
            .getFile("AnnotatorContextTest/AnnotatorWithConfigurationParameters.xml"));
    var desc5 = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in5);
    var tae5 = new PrimitiveAnalysisEngine_impl();
    // set data path just to test that we can get it later
    tae5.initialize(desc5, null);
    // this should include an annotator context
    mAC5 = new AnnotatorContext_impl(tae5.getUimaContextAdmin());
  }

  /*
   * Test for Object getConfigParameterValue(String)
   */
  @Test
  void testGetConfigParameterValueString() throws Exception {
    // this method should get parameter values from the default "en" group
    var str = (String) mAC1.getConfigParameterValue("StringParam");
    assertThat(str).isEqualTo("en");

    var strArr = (String[]) mAC1.getConfigParameterValue("StringArrayParam");
    assertThat(strArr).containsExactly("e", "n");

    var intVal = (Integer) mAC1.getConfigParameterValue("IntegerParam");
    assertThat(intVal).isEqualTo(42);

    var intArr = (Integer[]) mAC1.getConfigParameterValue("IntegerArrayParam");
    assertThat(intArr).containsExactly(1, 2, 3);

    var floatVal = (Float) mAC1.getConfigParameterValue("FloatParam");
    assertThat(floatVal).isNull();

    // test override
    var str2 = (String) mAC2.getConfigParameterValue("StringParam");
    assertThat(str2).isEqualTo("override");
    // other values should not be affected
    var intVal2 = (Integer) mAC1.getConfigParameterValue("IntegerParam");
    assertThat(intVal2.intValue()).isEqualTo(42);

    // test default fallback
    var str3 = (String) mAC4.getConfigParameterValue("StringParam");
    assertThat(str3).isEqualTo("test");

    var strArr2 = (String[]) mAC4.getConfigParameterValue("StringArrayParam");
    assertThat(strArr2).containsExactly("t", "e", "s", "t");
  }

  /*
   * Test for Object getConfigParameterValue(String, String)
   */
  @Test
  void testGetConfigParameterValueStringString() throws Exception {
    // en-US group
    // language fallback
    assertThat((String) mAC1.getConfigParameterValue("en-US", "StringParam")).isEqualTo("en");

    assertThat((String[]) mAC1.getConfigParameterValue("en-US", "StringArrayParam"))
            .containsExactly("e", "n", "-", "U", "S");

    assertThat(((Integer) mAC1.getConfigParameterValue("en-US", "IntegerParam")).intValue())
            .isEqualTo(1776);

    // language fallback
    assertThat((Integer[]) mAC1.getConfigParameterValue("en-US", "IntegerArrayParam"))
            .containsExactly(1, 2, 3);

    assertThat((Float) mAC1.getConfigParameterValue("en-US", "FloatParam")).isNull();

    // de group
    assertThat((String) mAC1.getConfigParameterValue("de", "StringParam")).isEqualTo("de");

    assertThat((String[]) mAC1.getConfigParameterValue("de", "StringArrayParam"))
            .containsExactly("d", "e");

    assertThat(((Integer) mAC1.getConfigParameterValue("de", "IntegerParam")).intValue())
            .isEqualTo(42); // default fallback

    assertThat((Integer[]) mAC1.getConfigParameterValue("de", "IntegerArrayParam"))
            .containsExactly(4, 5, 6);

    assertThat((Float) mAC1.getConfigParameterValue("de", "FloatParam")).isNull();

    // zh group
    assertThat((String) mAC1.getConfigParameterValue("zh", "StringParam")).isEqualTo("zh");

    assertThat((String[]) mAC1.getConfigParameterValue("zh", "StringArrayParam"))
            .containsExactly("z", "h");

    assertThat(((Integer) mAC1.getConfigParameterValue("zh", "IntegerParam")).intValue())
            .isEqualTo(42); // default fallback

    assertThat((Integer[]) mAC1.getConfigParameterValue("zh", "IntegerArrayParam"))
            .containsExactly(1, 2, 3); // default fallback

    assertThat((Float) mAC1.getConfigParameterValue("zh", "FloatParam")).isCloseTo(3.14f,
            within(0.0001f));

    // test override
    assertThat((String) mAC2.getConfigParameterValue("en", "StringParam")).isEqualTo("override");
    // fallback too
    assertThat((String) mAC2.getConfigParameterValue("en-GB", "StringParam")).isEqualTo("override");
    // other groups should not be affected
    assertThat((String) mAC2.getConfigParameterValue("de", "StringParam")).isEqualTo("de");

    // test empty string array
    assertThat((String[]) mAC2.getConfigParameterValue("x-unspecified", "StringArrayParam"))
            .isEmpty();

    // test nonexistent group
    // language_fallback for completely nonexistent language
    assertThat((String) mAC1.getConfigParameterValue("es", "StringParam")).isEqualTo("en");
    // default_fallback for nonexistent group
    assertThat((String) mAC4.getConfigParameterValue("es", "StringParam")).isEqualTo("test");
  }

  @Test
  void testGetConfigurationGroupNames() {
    assertThat(mAC1.getConfigurationGroupNames()).containsExactlyInAnyOrder("en", "en-US", "de",
            "zh", "x-unspecified");

    // try on something that has no groups
    assertThat(mAC5.getConfigurationGroupNames()).isEmpty();
  }

  @Test
  void testGetConfigParameterNames() {
    assertThat(mAC5.getConfigParameterNames()).containsExactly("StringParam", "StringArrayParam",
            "IntegerParam", "IntegerArrayParam", "FloatParam", "FloatArrayParam");

    // try on something that has groups
    assertThat(mAC1.getConfigParameterNames()).isEmpty();
  }

  @Test
  void testGetConfigParameterNamesString() {
    assertThat(mAC1.getConfigParameterNames("en")).containsExactlyInAnyOrder("StringParam",
            "StringArrayParam", "IntegerParam", "IntegerArrayParam");

    // try on nonexistent group
    assertThat(mAC1.getConfigParameterNames("foo")).isEmpty();

    // try on something that has no groups
    assertThat(mAC4.getConfigParameterNames("en")).isEmpty();
  }

  @Test
  void testGetResourceObjectString() throws Exception {
    // custom object
    var r = mAC3.getResourceObject("TestResourceObject");
    assertThat(r).isNotNull();
    assertThat(r).isInstanceOf(TestResourceInterface.class);

    // standard data resource
    var r2 = mAC3.getResourceObject("TestFileResource");
    assertThat(r2).isNotNull();
    assertThat(r2).isInstanceOf(DataResource.class);

    // parameterized resources (should fail)
    AnnotatorContextException ex = null;
    try {
      mAC3.getResourceObject("TestFileLanguageResource");
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mAC3.getResourceObject("TestLanguageResourceObject");
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // nonexistent resource (should return null)
    var r3 = mAC3.getResourceObject("Unknown");
    assertThat(r3).isNull();
  }

  @Test
  void testGetResourceURLString() throws Exception {
    // standard data resource (should succeed)
    assertThat(mAC3.getResourceURL("TestFileResource")).isNotNull();

    // custom resource object (should return null)
    assertThat(mAC3.getResourceURL("TestResourceObject")).isNull();

    // parameterized resources (should fail)
    AnnotatorContextException ex = null;
    try {
      mAC3.getResourceURL("TestFileLanguageResource");
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mAC3.getResourceURL("TestLanguageResourceObject");
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // nonexistent resource (should return null)
    assertThat(mAC3.getResourceURL("Unknown")).isNull();

    // passthrough to class loader
    assertThat(mAC3.getResourceURL("org/apache/uima/analysis_engine/impl/testDataFile3.dat"))
            .isNotNull();

    // passthrough to data path
    assertThat(mAC1.getResourceURL("testDataFile.dat")).isNotNull();

    // for directory
    assertThat(mAC3.getResourceURL("subdir")).isNotNull();

    // spaces as part of extension classpath (spaces should be URL-encoded)
    var url8 = mAC3.getResourceURL("OtherFileResource");
    assertThat(url8).isNotNull();
    assertThat(url8.getPath()).contains("%20").doesNotContain(" ");
  }

  @Test
  void testGetResourceURIString() throws Exception {
    // standard data resource (should succeed)
    assertThat(mAC3.getResourceURI("TestFileResource")).isNotNull();

    // custom resource object (should return null)
    assertThat(mAC3.getResourceURI("TestResourceObject")).isNull();

    // parameterized resources (should fail)
    AnnotatorContextException ex = null;
    try {
      mAC3.getResourceURI("TestFileLanguageResource");
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mAC3.getResourceURI("TestLanguageResourceObject");
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // nonexistent resource (should return null)
    assertThat(mAC3.getResourceURI("Unknown")).isNull();

    // passthrough to class loader
    assertThat(mAC3.getResourceURI("org/apache/uima/analysis_engine/impl/testDataFile3.dat"))
            .isNotNull();

    // passthrough to data path
    assertThat(mAC1.getResourceURI("testDataFile.dat")).isNotNull();

    // for directory
    assertThat(mAC3.getResourceURI("subdir")).isNotNull();

    // spaces as part of extension classpath (spaces should be decoded)
    var uri8 = mAC3.getResourceURI("OtherFileResource");
    assertThat(uri8).isNotNull();
    assertThat(!uri8.getPath().contains("%20")).isTrue();
    assertThat(uri8.getPath()).contains(" ");
  }

  @Test
  void testGetResourceFilePathString() throws Exception {
    // standard data resource (should succeed)
    assertThat(mAC3.getResourceFilePath("TestFileResource")).isNotNull();

    // custom resource object (should return null)
    assertThat(mAC3.getResourceFilePath("TestResourceObject")).isNull();

    // parameterized resources (should fail)
    AnnotatorContextException ex = null;
    try {
      mAC3.getResourceFilePath("TestFileLanguageResource");
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mAC3.getResourceFilePath("TestLanguageResourceObject");
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // nonexistent resource (should return null)
    assertThat(mAC3.getResourceFilePath("Unknown")).isNull();

    // passthrough to class loader
    var path5 = mAC3.getResourceFilePath("org/apache/uima/analysis_engine/impl/testDataFile3.dat");
    assertThat(path5).isNotNull();

    // passthrough to data path
    assertThat(mAC1.getResourceFilePath("testDataFile.dat")).isNotNull();

    // for directory
    assertThat(mAC3.getResourceFilePath("subdir")).isNotNull();

    // spaces as part of extension classpath (spaces should be decoded)
    var path8 = mAC3.getResourceFilePath("OtherFileResource");
    assertThat(path8).isNotNull();
    assertThat(path8.indexOf("%20") == -1).isTrue();
    assertThat(path8.indexOf(" ") > -1).isTrue();
  }

  @Test
  void testGetResourceAsStreamString() throws Exception {
    // standard data resource (should succeed)
    assertThat(mAC3.getResourceAsStream("TestFileResource")).isNotNull();

    // custom resource object (should return null)
    assertThat(mAC3.getResourceAsStream("TestResourceObject")).isNull();

    // parameterized resources (should fail)
    AnnotatorContextException ex = null;
    try {
      mAC3.getResourceAsStream("TestFileLanguageResource");
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mAC3.getResourceAsStream("TestLanguageResourceObject");
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // nonexistent resource (should return null)
    var strm3 = mAC3.getResourceAsStream("Unknown");
    assertThat(strm3).isNull();

    // passthrough to class loader
    var strm4 = mAC3.getResourceAsStream("org/apache/uima/analysis_engine/impl/testDataFile3.dat");
    assertThat(strm4).isNotNull();
    // for directory
    var strm5 = mAC3.getResourceAsStream("subdir");
    assertThat(strm5).isNotNull();

    // passthrough to data path
    var strm6 = mAC1.getResourceAsStream("testDataFile.dat");
    assertThat(strm6).isNotNull();
  }

  @Test
  void testGetResourceObjectStringStringArray() throws Exception {
    // standard data resource
    var r = mAC3.getResourceObject("TestFileLanguageResource", new String[] { "en" });
    assertThat(r) //
            .isNotNull() //
            .isInstanceOf(DataResource.class);
    var r2 = mAC3.getResourceObject("TestFileLanguageResource", new String[] { "de" });
    assertThat(r2) //
            .isNotNull() //
            .isInstanceOf(DataResource.class) //
            .isNotEqualTo(r);

    // custom object
    var r3 = mAC3.getResourceObject("TestLanguageResourceObject", new String[] { "en" });
    assertThat(r3) //
            .isNotNull() //
            .isInstanceOf(TestResourceInterface.class);
    var r4 = mAC3.getResourceObject("TestLanguageResourceObject", new String[] { "de" });
    assertThat(r4) //
            .isNotNull() //
            .isInstanceOf(TestResourceInterface.class) //
            .isNotEqualTo(r3);

    // parameter values for which no resource exists (should fail)
    assertThatExceptionOfType(AnnotatorContextException.class) //
            .isThrownBy(() -> {
              mAC3.getResourceObject("TestFileLanguageResource", new String[] { "zh" });
            });

    assertThatExceptionOfType(AnnotatorContextException.class) //
            .isThrownBy(() -> {
              mAC3.getResourceObject("TestFileLanguageResource", new String[] { "en", "p2" });
            });

    // non-parameterized resources (should fail)
    assertThatExceptionOfType(AnnotatorContextException.class) //
            .isThrownBy(() -> {
              mAC3.getResourceObject("TestFileResource", new String[] { "en" });
            });

    assertThatExceptionOfType(AnnotatorContextException.class) //
            .isThrownBy(() -> {
              mAC3.getResourceObject("TestResourceObject", new String[] { "de" });
            });

    // nonexistent resource (should return null)
    var r5 = mAC3.getResourceObject("Unknown", new String[] { "en" });
    assertThat(r5).isNull();
  }

  @Test
  void testGetResourceAsStreamStringStringArray() throws Exception {
    // standard data resource
    var strm = mAC3.getResourceAsStream("TestFileLanguageResource", new String[] { "en" });
    assertThat(strm).isNotNull();

    var strm2 = mAC3.getResourceAsStream("TestFileLanguageResource", new String[] { "de" });
    assertThat(strm2) //
            .isNotNull() //
            .isNotEqualTo(strm);

    // custom object (should return null)
    var strm3 = mAC3.getResourceAsStream("TestLanguageResourceObject", new String[] { "en" });
    assertThat(strm3).isNull();

    // parameter values for which no resource exists (should fail)
    AnnotatorContextException ex = null;
    try {
      mAC3.getResourceAsStream("TestFileLanguageResource", new String[] { "zh" });
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mAC3.getResourceAsStream("TestFileLanguageResource", new String[] { "en", "p2" });
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // non-parameterized resources (should fail)
    ex = null;
    try {
      mAC3.getResourceAsStream("TestFileResource", new String[] { "en" });
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mAC3.getResourceAsStream("TestResourceObject", new String[] { "de" });
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // nonexistent resource (should return null)
    var strm4 = mAC3.getResourceAsStream("Unknown", new String[] { "en" });
    assertThat(strm4).isNull();
  }

  @Test
  void testGetResourceURLStringStringArray() throws Exception {
    // standard data resource
    var url = mAC3.getResourceURL("TestFileLanguageResource", new String[] { "en" });
    assertThat(url).isNotNull();

    var url2 = mAC3.getResourceURL("TestFileLanguageResource", new String[] { "de" });
    assertThat(url2).isNotNull();
    assertThat(url2.toString()).isNotEqualTo(url.toString());

    // custom object (should return null)
    var url3 = mAC3.getResourceURL("TestLanguageResourceObject", new String[] { "en" });
    assertThat(url3).isNull();

    // parameter values for which no resource exists (should fail)
    AnnotatorContextException ex = null;
    try {
      mAC3.getResourceURL("TestFileLanguageResource", new String[] { "zh" });
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mAC3.getResourceURL("TestFileLanguageResource", new String[] { "en", "p2" });
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // non-parameterized resources (should fail)
    ex = null;
    try {
      mAC3.getResourceURL("TestFileResource", new String[] { "en" });
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mAC3.getResourceURL("TestResourceObject", new String[] { "de" });
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // nonexistent resource (should return null)
    var url4 = mAC3.getResourceURL("Unknown", new String[] { "en" });
    assertThat(url4).isNull();
  }

  @Test
  void testGetResourceURIStringStringArray() throws Exception {
    // standard data resource
    var uri = mAC3.getResourceURI("TestFileLanguageResource", new String[] { "en" });
    assertThat(uri).isNotNull();

    var uri2 = mAC3.getResourceURI("TestFileLanguageResource", new String[] { "de" });
    assertThat(uri2).isNotNull().isNotEqualTo(uri);

    // custom object (should return null)
    var uri3 = mAC3.getResourceURI("TestLanguageResourceObject", new String[] { "en" });
    assertThat(uri3).isNull();

    // parameter values for which no resource exists (should fail)
    AnnotatorContextException ex = null;
    try {
      mAC3.getResourceURI("TestFileLanguageResource", new String[] { "zh" });
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mAC3.getResourceURI("TestFileLanguageResource", new String[] { "en", "p2" });
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // non-parameterized resources (should fail)
    ex = null;
    try {
      mAC3.getResourceURI("TestFileResource", new String[] { "en" });
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mAC3.getResourceURI("TestResourceObject", new String[] { "de" });
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // nonexistent resource (should return null)
    var uri4 = mAC3.getResourceURI("Unknown", new String[] { "en" });
    assertThat(uri4).isNull();
  }

  @Test
  void testGetResourceFilePathStringStringArray() throws Exception {
    // standard data resource
    var path = mAC3.getResourceFilePath("TestFileLanguageResource", new String[] { "en" });
    assertThat(path).isNotNull();

    var path2 = mAC3.getResourceFilePath("TestFileLanguageResource", new String[] { "de" });
    assertThat(path2).isNotNull();
    assertThat(path2).isNotEqualTo(path);

    // custom object (should return null)
    var path3 = mAC3.getResourceFilePath("TestLanguageResourceObject", new String[] { "en" });
    assertThat(path3).isNull();

    // parameter values for which no resource exists (should fail)
    AnnotatorContextException ex = null;
    try {
      mAC3.getResourceFilePath("TestFileLanguageResource", new String[] { "zh" });
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mAC3.getResourceFilePath("TestFileLanguageResource", new String[] { "en", "p2" });
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // non-parameterized resources (should fail)
    ex = null;
    try {
      mAC3.getResourceFilePath("TestFileResource", new String[] { "en" });
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mAC3.getResourceFilePath("TestResourceObject", new String[] { "de" });
    } catch (AnnotatorContextException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // nonexistent resource (should return null)
    var path4 = mAC3.getResourceFilePath("Unknown", new String[] { "en" });
    assertThat(path4).isNull();
  }

  @Test
  void testGetDataPath() throws Exception {
    assertThat(mAC1.getDataPath()).isEqualTo(TEST_DATAPATH);
    assertThat(mAC4.getDataPath()).isEqualTo(System.getProperty("user.dir"));
  }
}
