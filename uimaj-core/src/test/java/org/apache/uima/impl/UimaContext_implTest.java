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

package org.apache.uima.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Arrays;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.impl.ResourceCreationSpecifier_impl;
import org.apache.uima.resource.impl.TestResourceInterface;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.ResourceMetaData_impl;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UimaContext_implTest {
  protected final String[] TEST_DATAPATH = { //
      JUnitExtension.getFile("AnnotatorContextTest").getPath(),
      JUnitExtension.getFile("ResourceTest").getPath() };

  protected final String TEST_EXTENSION_CLASSPATH = JUnitExtension
          .getFile("ResourceTest/spaces in dir name").getPath();

  private UimaContext mContext;

  private UimaContext mContext2;

  private UimaContext mContext3;

  private UimaContext mContext4;

  private UimaContext mContext5;

  @BeforeEach
  void setUp() throws Exception {
    // configure ResourceManager to allow test components to locate their resources
    var rm = UIMAFramework.newDefaultResourceManager();
    rm.setDataPathElements(TEST_DATAPATH);
    rm.setExtensionClassPath(TEST_EXTENSION_CLASSPATH, true);

    // create a UimaContext with Config Params and Resources
    UIMAFramework.getXMLParser().enableSchemaValidation(true);
    var ccDesc = UIMAFramework.getXMLParser().parseCasConsumerDescription(new XMLInputSource(
            JUnitExtension.getFile("UimaContextTest/CasConsumerForUimaContextTest.xml")));
    var cc = UIMAFramework.produceCasConsumer(ccDesc, rm, null);
    mContext = cc.getUimaContext();

    // create a UimaContext with Config Params in Groups but no resources
    var in = new XMLInputSource(
            JUnitExtension.getFile("AnnotatorContextTest/AnnotatorWithConfigurationGroups.xml"));
    var taeDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
    var ae = UIMAFramework.produceAnalysisEngine(taeDesc, rm, null);
    mContext2 = ae.getUimaContext();

    // create a UimaContext with Groups and Groupless Parameters
    var in2 = new XMLInputSource(JUnitExtension
            .getFile("AnnotatorContextTest/AnnotatorWithGroupsAndNonGroupParams.xml"));
    var taeDesc2 = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in2);
    var ae2 = UIMAFramework.produceAnalysisEngine(taeDesc2, rm, null);
    mContext3 = ae2.getUimaContext();

    // create a UimaContext with duplicate configuration groups
    var in3 = new XMLInputSource(JUnitExtension
            .getFile("TextAnalysisEngineImplTest/AnnotatorWithDuplicateConfigurationGroups.xml"));
    var taeDesc3 = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in3);
    var ae3 = UIMAFramework.produceAnalysisEngine(taeDesc3, rm, null);
    mContext4 = ae3.getUimaContext();

    // create a UimaContext for a CAS Multiplier
    var in4 = new XMLInputSource(
            JUnitExtension.getFile("TextAnalysisEngineImplTest/NewlineSegmenter.xml"));
    var taeDesc4 = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in4);
    var ae4 = UIMAFramework.produceAnalysisEngine(taeDesc4);
    mContext5 = ae4.getUimaContext();
  }

  @AfterEach
  public void tearDown() throws Exception {
    UIMAFramework.getXMLParser().enableSchemaValidation(false);
  }

  @Test
  void testGetConfigParameterValueString() throws Exception {
    var str = (String) mContext.getConfigParameterValue("StringParam");
    assertThat(str).isEqualTo("myString");
    var strArr = (String[]) mContext.getConfigParameterValue("StringArrayParam");
    assertThat(Arrays.asList(strArr)).isEqualTo(Arrays.asList(new String[] { "one", "two" }));
    var integer = (Integer) mContext.getConfigParameterValue("IntegerParam");
    assertThat(integer).isEqualTo(Integer.valueOf(42));
    var intArr = (Integer[]) mContext.getConfigParameterValue("IntegerArrayParam");
    assertThat(Arrays.asList(intArr)).isEqualTo(Arrays.asList(new Integer[] { 1, 2, 3 }));
    var flt = (Float) mContext.getConfigParameterValue("FloatParam");
    assertThat(flt).isEqualTo(Float.valueOf(3.14F));

    // default fallback
    var str2 = (String) mContext2.getConfigParameterValue("StringParam");
    assertThat(str2).isEqualTo("en");

    // get groupless param
    var str3 = (String) mContext3.getConfigParameterValue("GrouplessParam1");
    assertThat(str3).isEqualTo("foo");
    // default fallback in presence of groupless params (of different names)
    var str4 = (String) mContext3.getConfigParameterValue("StringParam");
    assertThat(str4).isEqualTo("en");
  }

  @Test
  void testGetConfigParameterValueStringString() throws Exception {
    // en-US group
    var str = (String) mContext2.getConfigParameterValue("en-US", "StringParam");
    assertThat(str).isEqualTo("en"); // language fallback

    var strArr = (String[]) mContext2.getConfigParameterValue("en-US", "StringArrayParam");
    assertThat(strArr).containsExactly("e", "n", "-", "U", "S");

    var intVal = (Integer) mContext2.getConfigParameterValue("en-US", "IntegerParam");
    assertThat(intVal.intValue()).isEqualTo(1776);

    // language fallback
    var intArr = (Integer[]) mContext2.getConfigParameterValue("en-US", "IntegerArrayParam");
    assertThat(intArr).containsExactly(1, 2, 3);

    var floatVal = (Float) mContext2.getConfigParameterValue("en-US", "FloatParam");
    assertThat(floatVal).isNull();

    // de group
    var str2 = (String) mContext2.getConfigParameterValue("de", "StringParam");
    assertThat(str2).isEqualTo("de");

    var strArr2 = (String[]) mContext2.getConfigParameterValue("de", "StringArrayParam");
    assertThat(strArr2).containsExactly("d", "e");

    var intVal2 = (Integer) mContext2.getConfigParameterValue("de", "IntegerParam");
    assertThat(intVal2.intValue()).isEqualTo(42); // default fallback

    var intArr2 = (Integer[]) mContext2.getConfigParameterValue("de", "IntegerArrayParam");
    assertThat(intArr2).containsExactly(4, 5, 6);

    var floatVal2 = (Float) mContext2.getConfigParameterValue("de", "FloatParam");
    assertThat(floatVal2).isNull();

    // zh group
    var str3 = (String) mContext2.getConfigParameterValue("zh", "StringParam");
    assertThat(str3).isEqualTo("zh");

    var strArr3 = (String[]) mContext2.getConfigParameterValue("zh", "StringArrayParam");
    assertThat(strArr3).containsExactly("z", "h");

    var intVal3 = (Integer) mContext2.getConfigParameterValue("zh", "IntegerParam");
    assertThat(intVal3.intValue()).isEqualTo(42); // default fallback

    var intArr3 = (Integer[]) mContext2.getConfigParameterValue("zh", "IntegerArrayParam");
    assertThat(intArr3).containsExactly(1, 2, 3); // default fallback

    var floatVal3 = (Float) mContext2.getConfigParameterValue("zh", "FloatParam");
    assertThat(floatVal3).isCloseTo(3.14f, within(0.0001f));

    // testing duplicate groups
    assertThat(mContext4.getConfigParameterValue("a", "CommonParam")).isEqualTo("common-a");
    assertThat(mContext4.getConfigParameterValue("a", "ABParam")).isEqualTo("ab-a");
    assertThat(mContext4.getConfigParameterValue("a", "BCParam")).isNull();
    assertThat(mContext4.getConfigParameterValue("b", "CommonParam")).isEqualTo("common-b");
    assertThat(mContext4.getConfigParameterValue("b", "ABParam")).isEqualTo("ab-b");
    assertThat(mContext4.getConfigParameterValue("b", "BCParam")).isEqualTo("bc-b");
    assertThat(mContext4.getConfigParameterValue("c", "CommonParam")).isEqualTo("common-c");
    assertThat(mContext4.getConfigParameterValue("c", "BCParam")).isEqualTo("bc-c");
    assertThat(mContext4.getConfigParameterValue("c", "ABParam")).isNull();
  }

  @Test
  void testGetConfigurationGroupNames() {
    assertThat(mContext2.getConfigurationGroupNames()).containsExactlyInAnyOrder("en", "en-US",
            "de", "zh", "x-unspecified");

    // try on something with both groups and groupless parameters
    assertThat(mContext3.getConfigurationGroupNames()).containsExactlyInAnyOrder("en", "en-US",
            "de", "zh", "x-unspecified");

    // try on something that has no groups
    assertThat(mContext.getConfigurationGroupNames()).isEmpty();
  }

  @Test
  void thatGetConfigurationGroupNamesWorksWhenNoParametersHaveBeenDeclared() {
    UimaContext emptyContext = UIMAFramework.newUimaContext(UIMAFramework.getLogger(),
            UIMAFramework.newDefaultResourceManager(), UIMAFramework.newConfigurationManager());

    assertThat(emptyContext.getConfigurationGroupNames()).isEmpty();
  }

  @Test
  void testGetConfigParameterNames() {
    assertThat(mContext.getConfigParameterNames()).containsExactly("StringParam",
            "StringArrayParam", "IntegerParam", "IntegerArrayParam", "FloatParam",
            "FloatArrayParam");

    // try on something that has groups
    assertThat(mContext2.getConfigParameterNames()).isEmpty();

    // try on something with both groups and groupless parameters
    assertThat(mContext3.getConfigParameterNames()).containsExactly("GrouplessParam1",
            "GrouplessParam2");
  }

  @Test
  public void thatGetConfigParameterNamesWorksWhenNoParametersHaveBeenDeclared() {
    UimaContext emptyContext = UIMAFramework.newUimaContext(UIMAFramework.getLogger(),
            UIMAFramework.newDefaultResourceManager(), UIMAFramework.newConfigurationManager());

    assertThat(emptyContext.getConfigParameterNames()).isEmpty();
    assertThat(emptyContext.getConfigParameterNames("blah")).isEmpty();
  }

  @Test
  void testGetConfigParameterNamesString() {
    assertThat(mContext2.getConfigParameterNames("en")).containsExactlyInAnyOrder("StringParam",
            "StringArrayParam", "IntegerParam", "IntegerArrayParam");

    // try on nonexistent group
    assertThat(mContext2.getConfigParameterNames("foo")).isEmpty();

    // try on something that has no groups
    assertThat(mContext.getConfigParameterNames("en")).isEmpty();

    // try on something with both groups and groupless params
    assertThat(mContext3.getConfigParameterNames("en")).containsExactlyInAnyOrder("StringParam",
            "StringArrayParam", "IntegerParam", "IntegerArrayParam");

  }

  @Test
  void testGetResourceObjectString() throws Exception {
    // custom object
    var r = mContext.getResourceObject("TestResourceObject");
    assertThat(r).isNotNull();
    assertThat(r instanceof TestResourceInterface).isTrue();

    // standard data resource
    var r2 = mContext.getResourceObject("TestFileResource");
    assertThat(r2).isNotNull();
    assertThat(r2 instanceof DataResource).isTrue();

    // parameterized resources (should fail)
    ResourceAccessException ex = null;
    try {
      mContext.getResourceObject("TestFileLanguageResource");
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mContext.getResourceObject("TestLanguageResourceObject");
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // nonexistent resource (should return null)
    var r3 = mContext.getResourceObject("Unknown");
    assertThat(r3).isNull();
  }

  @Test
  void testGetResourceURLString() throws Exception {
    // standard data resource (should succeed)
    var url = mContext.getResourceURL("TestFileResource");
    assertThat(url).isNotNull();

    // custom resource object (should return null)
    var url2 = mContext.getResourceURL("TestResourceObject");
    assertThat(url2).isNull();

    // parameterized resources (should fail)
    ResourceAccessException ex = null;
    try {
      mContext.getResourceURL("TestFileLanguageResource");
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mContext.getResourceURL("TestLanguageResourceObject");
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // nonexistent resource (should return null)
    var url3 = mContext.getResourceURL("Unknown");
    assertThat(url3).isNull();

    // passthrough to class loader
    var url5 = mContext.getResourceURL("org/apache/uima/analysis_engine/impl/testDataFile3.dat");
    assertThat(url5).isNotNull();

    // passthrough to data path
    var url6 = mContext.getResourceURL("testDataFile.dat");
    assertThat(url6).isNotNull();

    // for directory
    var url7 = mContext.getResourceURL("subdir");
    assertThat(url7).isNotNull();

    // spaces as part of extension classpath (spaces should be URL-encoded)
    var url8 = mContext.getResourceURL("OtherFileResource");
    assertThat(url8).isNotNull();
    assertThat(url8.getPath().indexOf("%20") > -1).isTrue();
    assertThat(url8.getPath().indexOf(" ") == -1).isTrue();
  }

  @Test
  void testGetResourceURIString() throws Exception {
    // standard data resource (should succeed)
    var uri = mContext.getResourceURI("TestFileResource");
    assertThat(uri).isNotNull();

    // custom resource object (should return null)
    var uri2 = mContext.getResourceURI("TestResourceObject");
    assertThat(uri2).isNull();

    // parameterized resources (should fail)
    ResourceAccessException ex = null;
    try {
      mContext.getResourceURI("TestFileLanguageResource");
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mContext.getResourceURI("TestLanguageResourceObject");
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // nonexistent resource (should return null)
    var uri3 = mContext.getResourceURI("Unknown");
    assertThat(uri3).isNull();

    // passthrough to class loader
    var uri5 = mContext.getResourceURI("org/apache/uima/analysis_engine/impl/testDataFile3.dat");
    assertThat(uri5).isNotNull();

    // passthrough to data path
    var uri6 = mContext.getResourceURI("testDataFile.dat");
    assertThat(uri6).isNotNull();

    // for directory
    var uri7 = mContext.getResourceURI("subdir");
    assertThat(uri7).isNotNull();

    // spaces as part of extension classpath (spaces should be decoded, unlike with URL)
    var uri8 = mContext.getResourceURI("OtherFileResource");
    assertThat(uri8).isNotNull();
    assertThat(uri8.getPath().indexOf("%20") == -1).isTrue();
    assertThat(uri8.getPath().indexOf(" ") > -1).isTrue();
  }

  @Test
  void testGetResourceFilePathString() throws Exception {
    // standard data resource (should succeed)
    var path = mContext.getResourceFilePath("TestFileResource");
    assertThat(path).isNotNull();

    // custom resource object (should return null)
    var path2 = mContext.getResourceFilePath("TestResourceObject");
    assertThat(path2).isNull();

    // parameterized resources (should fail)
    ResourceAccessException ex = null;
    try {
      mContext.getResourceFilePath("TestFileLanguageResource");
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mContext.getResourceFilePath("TestLanguageResourceObject");
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // nonexistent resource (should return null)
    var path3 = mContext.getResourceFilePath("Unknown");
    assertThat(path3).isNull();

    // passthrough to class loader
    var path5 = mContext
            .getResourceFilePath("org/apache/uima/analysis_engine/impl/testDataFile3.dat");
    assertThat(path5).isNotNull();

    // passthrough to data path
    var path6 = mContext.getResourceFilePath("testDataFile.dat");
    assertThat(path6).isNotNull();

    // for directory
    var path7 = mContext.getResourceFilePath("subdir");
    assertThat(path7).isNotNull();

    // spaces as part of extension classpath (spaces should be decoded, unlike with URL)
    var path8 = mContext.getResourceFilePath("OtherFileResource");
    assertThat(path8).isNotNull();
    assertThat(path8.indexOf("%20") == -1).isTrue();
    assertThat(path8.indexOf(" ") > -1).isTrue();
  }

  @Test
  void testGetResourceAsStreamString() throws Exception {
    // standard data resource (should succeed)
    var strm = mContext.getResourceAsStream("TestFileResource");
    assertThat(strm).isNotNull();

    // custom resource object (should return null)
    var strm2 = mContext.getResourceAsStream("TestResourceObject");
    assertThat(strm2).isNull();

    // parameterized resources (should fail)
    ResourceAccessException ex = null;
    try {
      mContext.getResourceAsStream("TestFileLanguageResource");
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mContext.getResourceAsStream("TestLanguageResourceObject");
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // nonexistent resource (should return null)
    var strm3 = mContext.getResourceAsStream("Unknown");
    assertThat(strm3).isNull();

    // passthrough to class loader
    var strm4 = mContext
            .getResourceAsStream("org/apache/uima/analysis_engine/impl/testDataFile3.dat");
    assertThat(strm4).isNotNull();

    // passthrough to data path
    var strm5 = mContext.getResourceAsStream("testDataFile.dat");
    assertThat(strm5).isNotNull();

    // for directory
    var strm6 = mContext.getResourceAsStream("subdir");
    assertThat(strm6).isNotNull();

    // spaces as part of extension classpath
    var strm7 = mContext.getResourceAsStream("OtherFileResource");
    assertThat(strm7).isNotNull();
  }

  @Test
  void testGetResourceObjectStringStringArray() throws Exception {
    // standard data resource
    var r = mContext.getResourceObject("TestFileLanguageResource", new String[] { "en" });
    assertThat(r).isNotNull();
    assertThat(r instanceof DataResource).isTrue();
    var r2 = mContext.getResourceObject("TestFileLanguageResource", new String[] { "de" });
    assertThat(r2).isNotNull();
    assertThat(r2 instanceof DataResource).isTrue();
    assertThat(r).isNotEqualTo(r2);

    // custom object
    var r3 = mContext.getResourceObject("TestLanguageResourceObject", new String[] { "en" });
    assertThat(r3).isNotNull();
    assertThat(r3 instanceof TestResourceInterface).isTrue();
    var r4 = mContext.getResourceObject("TestLanguageResourceObject", new String[] { "de" });
    assertThat(r4).isNotNull();
    assertThat(r4 instanceof TestResourceInterface).isTrue();
    assertThat(r3).isNotEqualTo(r4);

    // parameter values for which no resource exists (should fail)
    ResourceAccessException ex = null;
    try {
      mContext.getResourceObject("TestFileLanguageResource", new String[] { "zh" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mContext.getResourceObject("TestFileLanguageResource", new String[] { "en", "p2" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // non-parameterized resources (should fail)
    ex = null;
    try {
      mContext.getResourceObject("TestFileResource", new String[] { "en" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mContext.getResourceObject("TestResourceObject", new String[] { "de" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // nonexistent resource (should return null)
    var r5 = mContext.getResourceObject("Unknown", new String[] { "en" });
    assertThat(r5).isNull();
  }

  @Test
  void testGetResourceAsStreamStringStringArray() throws Exception {
    // standard data resource
    var strm = mContext.getResourceAsStream("TestFileLanguageResource", new String[] { "en" });
    assertThat(strm).isNotNull();

    var strm2 = mContext.getResourceAsStream("TestFileLanguageResource", new String[] { "de" });
    assertThat(strm2).isNotNull();
    assertThat(strm).isNotEqualTo(strm2);

    // custom object (should return null)
    var strm3 = mContext.getResourceAsStream("TestLanguageResourceObject", new String[] { "en" });
    assertThat(strm3).isNull();

    // parameter values for which no resource exists (should fail)
    ResourceAccessException ex = null;
    try {
      mContext.getResourceAsStream("TestFileLanguageResource", new String[] { "zh" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mContext.getResourceAsStream("TestFileLanguageResource", new String[] { "en", "p2" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // non-parameterized resources (should fail)
    ex = null;
    try {
      mContext.getResourceAsStream("TestFileResource", new String[] { "en" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mContext.getResourceAsStream("TestResourceObject", new String[] { "de" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // nonexistent resource (should return null)
    var strm4 = mContext.getResourceAsStream("Unknown", new String[] { "en" });
    assertThat(strm4).isNull();
  }

  @Test
  void testGetResourceURLStringStringArray() throws Exception {
    // standard data resource
    var url = mContext.getResourceURL("TestFileLanguageResource", new String[] { "en" });
    assertThat(url).isNotNull();

    var url2 = mContext.getResourceURL("TestFileLanguageResource", new String[] { "de" });
    assertThat(url2).isNotNull();
    assertThat(url.toString()).isNotEqualTo(url2.toString());

    // custom object (should return null)
    var url3 = mContext.getResourceURL("TestLanguageResourceObject", new String[] { "en" });
    assertThat(url3).isNull();

    // parameter values for which no resource exists (should fail)
    ResourceAccessException ex = null;
    try {
      mContext.getResourceURL("TestFileLanguageResource", new String[] { "zh" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mContext.getResourceURL("TestFileLanguageResource", new String[] { "en", "p2" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // non-parameterized resources (should fail)
    ex = null;
    try {
      mContext.getResourceURL("TestFileResource", new String[] { "en" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mContext.getResourceURL("TestResourceObject", new String[] { "de" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // nonexistent resource (should return null)
    var url4 = mContext.getResourceURL("Unknown", new String[] { "en" });
    assertThat(url4).isNull();
  }

  @Test
  void testGetResourceURIStringStringArray() throws Exception {
    // standard data resource
    var uri = mContext.getResourceURI("TestFileLanguageResource", new String[] { "en" });
    assertThat(uri).isNotNull();

    var uri2 = mContext.getResourceURI("TestFileLanguageResource", new String[] { "de" });
    assertThat(uri2).isNotNull();
    assertThat(uri).isNotEqualTo(uri2);

    // custom object (should return null)
    var uri3 = mContext.getResourceURI("TestLanguageResourceObject", new String[] { "en" });
    assertThat(uri3).isNull();

    // parameter values for which no resource exists (should fail)
    ResourceAccessException ex = null;
    try {
      mContext.getResourceURI("TestFileLanguageResource", new String[] { "zh" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mContext.getResourceURI("TestFileLanguageResource", new String[] { "en", "p2" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // non-parameterized resources (should fail)
    ex = null;
    try {
      mContext.getResourceURI("TestFileResource", new String[] { "en" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mContext.getResourceURI("TestResourceObject", new String[] { "de" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // nonexistent resource (should return null)
    var uri4 = mContext.getResourceURI("Unknown", new String[] { "en" });
    assertThat(uri4).isNull();
  }

  @Test
  void testGetResourceFilePathStringStringArray() throws Exception {
    // standard data resource
    var path = mContext.getResourceFilePath("TestFileLanguageResource", new String[] { "en" });
    assertThat(path).isNotNull();

    var path2 = mContext.getResourceFilePath("TestFileLanguageResource", new String[] { "de" });
    assertThat(path2).isNotNull();
    assertThat(path).isNotEqualTo(path2);

    // custom object (should return null)
    var path3 = mContext.getResourceFilePath("TestLanguageResourceObject", new String[] { "en" });
    assertThat(path3).isNull();

    // parameter values for which no resource exists (should fail)
    ResourceAccessException ex = null;
    try {
      mContext.getResourceFilePath("TestFileLanguageResource", new String[] { "zh" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mContext.getResourceFilePath("TestFileLanguageResource", new String[] { "en", "p2" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // non-parameterized resources (should fail)
    ex = null;
    try {
      mContext.getResourceFilePath("TestFileResource", new String[] { "en" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    ex = null;
    try {
      mContext.getResourceFilePath("TestResourceObject", new String[] { "de" });
    } catch (ResourceAccessException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();

    // nonexistent resource (should return null)
    var path4 = mContext.getResourceFilePath("Unknown", new String[] { "en" });
    assertThat(path4).isNull();
  }

  @Test
  void testGetDataPathElements() throws Exception {
    assertThat(mContext.getDataPathElements()).containsExactly(TEST_DATAPATH);
    assertThat(mContext2.getDataPathElements()).containsExactly(TEST_DATAPATH);
  }

  @Test
  void testGetEmptyCas() throws Exception {
    var emptyCas = mContext5.getEmptyCas(CAS.class);
    // should be allowed to release this CAS
    emptyCas.release();
    // and then get it again
    emptyCas = mContext5.getEmptyCas(CAS.class);
    emptyCas.release();
  }
}

class MyTestSpecifier extends ResourceCreationSpecifier_impl {
  public MyTestSpecifier() {
    setMetaData(new ResourceMetaData_impl());
  }

  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  private static final XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("testSpecifier",
          new PropertyXmlInfo[] { new PropertyXmlInfo("metaData", null, false), });
}
