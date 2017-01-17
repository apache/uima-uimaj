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

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.annotator.AnnotatorContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.impl.TestResourceInterface;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;


public class AnnotatorContext_implTest extends TestCase {
  protected final String TEST_DATAPATH = JUnitExtension.getFile("AnnotatorContextTest").getPath()
          + System.getProperty("path.separator") + JUnitExtension.getFile("ResourceTest");

  protected final String TEST_EXTENSION_CLASSPATH = JUnitExtension.getFile(
          "ResourceTest/spaces in dir name").getPath();

  private AnnotatorContext mAC1;

  private AnnotatorContext mAC2;

  private AnnotatorContext mAC3;

  private AnnotatorContext mAC4;

  private AnnotatorContext mAC5;

  /**
   * Constructor for AnnotatorContext_implTest.
   * 
   * @param arg0
   */
  public AnnotatorContext_implTest(String arg0) {
    super(arg0);
  }

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    try {
      super.setUp();
      // create primitive analysis engine with configuration groups
      XMLInputSource in = new XMLInputSource(JUnitExtension
              .getFile("AnnotatorContextTest/AnnotatorWithConfigurationGroups.xml"));
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      PrimitiveAnalysisEngine_impl tae = new PrimitiveAnalysisEngine_impl();
      // set data path just to test that we can get it later
      Map<String, Object> map = new HashMap<String, Object>();
      ResourceManager rm = UIMAFramework.newDefaultResourceManager();
      rm.setDataPath(TEST_DATAPATH);
      rm.setExtensionClassPath(TEST_EXTENSION_CLASSPATH, true);
      map.put(AnalysisEngine.PARAM_RESOURCE_MANAGER, rm);
      tae.initialize(desc, map);
      // this should include an annotator context
      mAC1 = new AnnotatorContext_impl(tae.getUimaContextAdmin());

      // create aggregate analysis engine with configuration parameter overrides
      XMLInputSource in2 = new XMLInputSource(JUnitExtension
              .getFile("AnnotatorContextTest/AggregateTaeWithConfigParamOverrides.xml"));
      AnalysisEngineDescription aggDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in2);
      AggregateAnalysisEngine_impl aggTae = new AggregateAnalysisEngine_impl();
      aggTae.initialize(aggDesc, map);
      // get the primitive TAE
      PrimitiveAnalysisEngine_impl primTae = (PrimitiveAnalysisEngine_impl) aggTae._getASB()
              .getComponentAnalysisEngines().values().toArray()[0];
      // this should include an annotator context
      mAC2 = new AnnotatorContext_impl(primTae.getUimaContextAdmin());

      // create primitive analysis engine for resource testing
      XMLInputSource in3 = new XMLInputSource(JUnitExtension
              .getFile("AnnotatorContextTest/ResourceTestAnnotator.xml"));
      AnalysisEngineDescription resTestDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in3);
      PrimitiveAnalysisEngine_impl resTestTae = new PrimitiveAnalysisEngine_impl();
      resTestTae.initialize(resTestDesc, map);
      // this should include an annotator context
      mAC3 = new AnnotatorContext_impl(resTestTae.getUimaContextAdmin());

      // create primitive TAE with configuration groups and default fallback
      XMLInputSource in4 = new XMLInputSource(JUnitExtension
              .getFile("AnnotatorContextTest/AnnotatorWithDefaultFallbackConfiguration.xml"));
      AnalysisEngineDescription desc4 = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in4);
      PrimitiveAnalysisEngine_impl tae4 = new PrimitiveAnalysisEngine_impl();
      // set data path just to test that we can get it later
      tae4.initialize(desc4, null);
      // this should include an annotator context
      mAC4 = new AnnotatorContext_impl(tae4.getUimaContextAdmin());

      // create primitive TAE with configuration parameters (no groups)
      XMLInputSource in5 = new XMLInputSource(JUnitExtension
              .getFile("AnnotatorContextTest/AnnotatorWithConfigurationParameters.xml"));
      AnalysisEngineDescription desc5 = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in5);
      PrimitiveAnalysisEngine_impl tae5 = new PrimitiveAnalysisEngine_impl();
      // set data path just to test that we can get it later
      tae5.initialize(desc5, null);
      // this should include an annotator context
      mAC5 = new AnnotatorContext_impl(tae5.getUimaContextAdmin());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  /*
   * Test for Object getConfigParameterValue(String)
   */
  public void testGetConfigParameterValueString() throws Exception {
    try {
      // this method should get parameter values from the default "en" group
      String str = (String) mAC1.getConfigParameterValue("StringParam");
      Assert.assertEquals("en", str);

      String[] strArr = (String[]) mAC1.getConfigParameterValue("StringArrayParam");
      Assert.assertEquals(2, strArr.length);
      Assert.assertEquals("e", strArr[0]);
      Assert.assertEquals("n", strArr[1]);

      Integer intVal = (Integer) mAC1.getConfigParameterValue("IntegerParam");
      Assert.assertEquals(42, intVal.intValue());

      Integer[] intArr = (Integer[]) mAC1.getConfigParameterValue("IntegerArrayParam");
      Assert.assertEquals(3, intArr.length);
      Assert.assertEquals(1, intArr[0].intValue());
      Assert.assertEquals(2, intArr[1].intValue());
      Assert.assertEquals(3, intArr[2].intValue());

      Float floatVal = (Float) mAC1.getConfigParameterValue("FloatParam");
      Assert.assertEquals(null, floatVal);

      // test override
      String str2 = (String) mAC2.getConfigParameterValue("StringParam");
      Assert.assertEquals("override", str2);
      // other values should not be affected
      Integer intVal2 = (Integer) mAC1.getConfigParameterValue("IntegerParam");
      Assert.assertEquals(42, intVal2.intValue());

      // test default fallback
      String str3 = (String) mAC4.getConfigParameterValue("StringParam");
      Assert.assertEquals("test", str3);

      String[] strArr2 = (String[]) mAC4.getConfigParameterValue("StringArrayParam");
      Assert.assertEquals(4, strArr2.length);
      Assert.assertEquals("t", strArr2[0]);
      Assert.assertEquals("e", strArr2[1]);
      Assert.assertEquals("s", strArr2[2]);
      Assert.assertEquals("t", strArr2[3]);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  /*
   * Test for Object getConfigParameterValue(String, String)
   */
  public void testGetConfigParameterValueStringString() throws Exception {
    try {
      // en-US group
      String str = (String) mAC1.getConfigParameterValue("en-US", "StringParam");
      Assert.assertEquals("en", str); // language fallback

      String[] strArr = (String[]) mAC1.getConfigParameterValue("en-US", "StringArrayParam");
      Assert.assertEquals(5, strArr.length);
      Assert.assertEquals("e", strArr[0]);
      Assert.assertEquals("n", strArr[1]);
      Assert.assertEquals("-", strArr[2]);
      Assert.assertEquals("U", strArr[3]);
      Assert.assertEquals("S", strArr[4]);

      Integer intVal = (Integer) mAC1.getConfigParameterValue("en-US", "IntegerParam");
      Assert.assertEquals(1776, intVal.intValue());

      Integer[] intArr = (Integer[]) mAC1.getConfigParameterValue("en-US", "IntegerArrayParam");
      Assert.assertEquals(3, intArr.length); // language fallback
      Assert.assertEquals(1, intArr[0].intValue());
      Assert.assertEquals(2, intArr[1].intValue());
      Assert.assertEquals(3, intArr[2].intValue());

      Float floatVal = (Float) mAC1.getConfigParameterValue("en-US", "FloatParam");
      Assert.assertEquals(null, floatVal);

      // de group
      String str2 = (String) mAC1.getConfigParameterValue("de", "StringParam");
      Assert.assertEquals("de", str2);

      String[] strArr2 = (String[]) mAC1.getConfigParameterValue("de", "StringArrayParam");
      Assert.assertEquals(2, strArr2.length);
      Assert.assertEquals("d", strArr2[0]);
      Assert.assertEquals("e", strArr2[1]);

      Integer intVal2 = (Integer) mAC1.getConfigParameterValue("de", "IntegerParam");
      Assert.assertEquals(42, intVal2.intValue()); // default fallback

      Integer[] intArr2 = (Integer[]) mAC1.getConfigParameterValue("de", "IntegerArrayParam");
      Assert.assertEquals(3, intArr2.length);
      Assert.assertEquals(4, intArr2[0].intValue());
      Assert.assertEquals(5, intArr2[1].intValue());
      Assert.assertEquals(6, intArr2[2].intValue());

      Float floatVal2 = (Float) mAC1.getConfigParameterValue("de", "FloatParam");
      Assert.assertEquals(null, floatVal2);

      // zh group
      String str3 = (String) mAC1.getConfigParameterValue("zh", "StringParam");
      Assert.assertEquals("zh", str3);

      String[] strArr3 = (String[]) mAC1.getConfigParameterValue("zh", "StringArrayParam");
      Assert.assertEquals(2, strArr3.length);
      Assert.assertEquals("z", strArr3[0]);
      Assert.assertEquals("h", strArr3[1]);

      Integer intVal3 = (Integer) mAC1.getConfigParameterValue("zh", "IntegerParam");
      Assert.assertEquals(42, intVal3.intValue()); // default fallback

      Integer[] intArr3 = (Integer[]) mAC1.getConfigParameterValue("zh", "IntegerArrayParam");
      Assert.assertEquals(3, intArr3.length); // default fallback
      Assert.assertEquals(1, intArr3[0].intValue());
      Assert.assertEquals(2, intArr3[1].intValue());
      Assert.assertEquals(3, intArr3[2].intValue());

      Float floatVal3 = (Float) mAC1.getConfigParameterValue("zh", "FloatParam");
      Assert.assertEquals(3.14, floatVal3.floatValue(), 0.0001);

      // test override
      String str4 = (String) mAC2.getConfigParameterValue("en", "StringParam");
      Assert.assertEquals("override", str4);
      // fallback too
      String str5 = (String) mAC2.getConfigParameterValue("en-GB", "StringParam");
      Assert.assertEquals("override", str5);
      // other groups should not be affected
      String str6 = (String) mAC2.getConfigParameterValue("de", "StringParam");
      Assert.assertEquals("de", str6);

      // test empty string array
      String[] strArr4 = (String[]) mAC2.getConfigParameterValue("x-unspecified",
              "StringArrayParam");
      Assert.assertTrue(Arrays.equals(strArr4, new String[0]));

      // test nonexistent group
      String str7 = (String) mAC1.getConfigParameterValue("es", "StringParam");
      Assert.assertEquals("en", str7); // language_fallback for completely nonexistent language
      String str8 = (String) mAC4.getConfigParameterValue("es", "StringParam");
      Assert.assertEquals("test", str8); // default_fallback for nonexistent group
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetConfigurationGroupNames() {
    String[] names = mAC1.getConfigurationGroupNames();
    Assert.assertEquals(5, names.length);
    List<String> l = new ArrayList<String>(Arrays.asList(names));
    Assert.assertTrue(l.contains("en"));
    Assert.assertTrue(l.contains("en-US"));
    Assert.assertTrue(l.contains("de"));
    Assert.assertTrue(l.contains("zh"));
    Assert.assertTrue(l.contains("x-unspecified"));

    // try on something that has no groups
    names = mAC5.getConfigurationGroupNames();
    Assert.assertEquals(0, names.length);
  }

  public void testGetConfigParameterNames() {
    String[] names = mAC5.getConfigParameterNames();
    Assert.assertEquals(6, names.length);
    Assert.assertEquals("StringParam", names[0]);
    Assert.assertEquals("StringArrayParam", names[1]);
    Assert.assertEquals("IntegerParam", names[2]);
    Assert.assertEquals("IntegerArrayParam", names[3]);
    Assert.assertEquals("FloatParam", names[4]);
    Assert.assertEquals("FloatArrayParam", names[5]);

    // try on something that has groups
    names = mAC1.getConfigParameterNames();
    Assert.assertEquals(0, names.length);
  }

  public void testGetConfigParameterNamesString() {
    String[] names = mAC1.getConfigParameterNames("en");
    Assert.assertEquals(4, names.length);
    List<String> l = new ArrayList<String>(Arrays.asList(names));
    Assert.assertTrue(l.contains("StringParam"));
    Assert.assertTrue(l.contains("StringArrayParam"));
    Assert.assertTrue(l.contains("IntegerParam"));
    Assert.assertTrue(l.contains("IntegerArrayParam"));

    // try on nonexistent group
    names = mAC1.getConfigParameterNames("foo");
    Assert.assertEquals(0, names.length);

    // try on something that has no groups
    names = mAC4.getConfigParameterNames("en");
    Assert.assertEquals(0, names.length);
  }

  public void testGetResourceObjectString() throws Exception {
    try {
      // custom object
      Object r = mAC3.getResourceObject("TestResourceObject");
      Assert.assertNotNull(r);
      Assert.assertTrue(r instanceof TestResourceInterface);

      // standard data resource
      Object r2 = mAC3.getResourceObject("TestFileResource");
      Assert.assertNotNull(r2);
      Assert.assertTrue(r2 instanceof DataResource);

      // parameterized resources (should fail)
      AnnotatorContextException ex = null;
      try {
        mAC3.getResourceObject("TestFileLanguageResource");
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mAC3.getResourceObject("TestLanguageResourceObject");
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      Object r3 = mAC3.getResourceObject("Unknown");
      Assert.assertNull(r3);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetResourceURLString() throws Exception {
    try {
      // standard data resource (should succeed)
      URL url = mAC3.getResourceURL("TestFileResource");
      Assert.assertNotNull(url);

      // custom resource object (should return null)
      URL url2 = mAC3.getResourceURL("TestResourceObject");
      Assert.assertNull(url2);

      // parameterized resources (should fail)
      AnnotatorContextException ex = null;
      try {
        mAC3.getResourceURL("TestFileLanguageResource");
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mAC3.getResourceURL("TestLanguageResourceObject");
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      URL url3 = mAC3.getResourceURL("Unknown");
      Assert.assertNull(url3);

      // passthrough to class loader
      URL url5 = mAC3.getResourceURL("org/apache/uima/analysis_engine/impl/testDataFile3.dat");
      Assert.assertNotNull(url5);

      // passthrough to data path
      URL url6 = mAC1.getResourceURL("testDataFile.dat");
      Assert.assertNotNull(url6);

      // for directory
      URL url7 = mAC3.getResourceURL("subdir");
      Assert.assertNotNull(url7);

      // spaces as part of extension classpath (spaces should be URL-encoded)
      URL url8 = mAC3.getResourceURL("OtherFileResource");
      assertNotNull(url8);
      assertTrue(url8.getPath().indexOf("%20") > -1);
      assertTrue(url8.getPath().indexOf(" ") == -1);

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetResourceURIString() throws Exception {
    try {
      // standard data resource (should succeed)
      URI uri = mAC3.getResourceURI("TestFileResource");
      Assert.assertNotNull(uri);

      // custom resource object (should return null)
      URI uri2 = mAC3.getResourceURI("TestResourceObject");
      Assert.assertNull(uri2);

      // parameterized resources (should fail)
      AnnotatorContextException ex = null;
      try {
        mAC3.getResourceURI("TestFileLanguageResource");
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mAC3.getResourceURI("TestLanguageResourceObject");
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      URI uri3 = mAC3.getResourceURI("Unknown");
      Assert.assertNull(uri3);

      // passthrough to class loader
      URI uri5 = mAC3.getResourceURI("org/apache/uima/analysis_engine/impl/testDataFile3.dat");
      Assert.assertNotNull(uri5);

      // passthrough to data path
      URI uri6 = mAC1.getResourceURI("testDataFile.dat");
      Assert.assertNotNull(uri6);

      // for directory
      URI uri7 = mAC3.getResourceURI("subdir");
      Assert.assertNotNull(uri7);

      // spaces as part of extension classpath (spaces should be decoded)
      URI uri8 = mAC3.getResourceURI("OtherFileResource");
      assertNotNull(uri8);
      assertTrue(uri8.getPath().indexOf("%20") == -1);
      assertTrue(uri8.getPath().indexOf(" ") > -1);

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetResourceFilePathString() throws Exception {
    try {
      // standard data resource (should succeed)
      String path = mAC3.getResourceFilePath("TestFileResource");
      Assert.assertNotNull(path);

      // custom resource object (should return null)
      String path2 = mAC3.getResourceFilePath("TestResourceObject");
      Assert.assertNull(path2);

      // parameterized resources (should fail)
      AnnotatorContextException ex = null;
      try {
        mAC3.getResourceFilePath("TestFileLanguageResource");
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mAC3.getResourceFilePath("TestLanguageResourceObject");
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      String path3 = mAC3.getResourceFilePath("Unknown");
      Assert.assertNull(path3);

      // passthrough to class loader
      String path5 = mAC3
              .getResourceFilePath("org/apache/uima/analysis_engine/impl/testDataFile3.dat");
      Assert.assertNotNull(path5);

      // passthrough to data path
      String path6 = mAC1.getResourceFilePath("testDataFile.dat");
      Assert.assertNotNull(path6);

      // for directory
      String path7 = mAC3.getResourceFilePath("subdir");
      Assert.assertNotNull(path7);

      // spaces as part of extension classpath (spaces should be decoded)
      String path8 = mAC3.getResourceFilePath("OtherFileResource");
      assertNotNull(path8);
      assertTrue(path8.indexOf("%20") == -1);
      assertTrue(path8.indexOf(" ") > -1);

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetResourceAsStreamString() throws Exception {
    try {
      // standard data resource (should succeed)
      InputStream strm = mAC3.getResourceAsStream("TestFileResource");
      Assert.assertNotNull(strm);

      // custom resource object (should return null)
      InputStream strm2 = mAC3.getResourceAsStream("TestResourceObject");
      Assert.assertNull(strm2);

      // parameterized resources (should fail)
      AnnotatorContextException ex = null;
      try {
        mAC3.getResourceAsStream("TestFileLanguageResource");
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mAC3.getResourceAsStream("TestLanguageResourceObject");
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      InputStream strm3 = mAC3.getResourceAsStream("Unknown");
      Assert.assertNull(strm3);

      // passthrough to class loader
      InputStream strm4 = mAC3
              .getResourceAsStream("org/apache/uima/analysis_engine/impl/testDataFile3.dat");
      Assert.assertNotNull(strm4);
      // for directory
      InputStream strm5 = mAC3.getResourceAsStream("subdir");
      Assert.assertNotNull(strm5);

      // passthrough to data path
      InputStream strm6 = mAC1.getResourceAsStream("testDataFile.dat");
      Assert.assertNotNull(strm6);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetResourceObjectStringStringArray() throws Exception {
    try {
      // standard data resource
      Object r = mAC3.getResourceObject("TestFileLanguageResource", new String[] { "en" });
      Assert.assertNotNull(r);
      Assert.assertTrue(r instanceof DataResource);
      Object r2 = mAC3.getResourceObject("TestFileLanguageResource", new String[] { "de" });
      Assert.assertNotNull(r2);
      Assert.assertTrue(r2 instanceof DataResource);
      Assert.assertFalse(r2.equals(r));

      // custom object
      Object r3 = mAC3.getResourceObject("TestLanguageResourceObject", new String[] { "en" });
      Assert.assertNotNull(r3);
      Assert.assertTrue(r3 instanceof TestResourceInterface);
      Object r4 = mAC3.getResourceObject("TestLanguageResourceObject", new String[] { "de" });
      Assert.assertNotNull(r4);
      Assert.assertTrue(r4 instanceof TestResourceInterface);
      Assert.assertFalse(r4.equals(r3));

      // parameter values for which no resource exists (should fail)
      AnnotatorContextException ex = null;
      try {
        mAC3.getResourceObject("TestFileLanguageResource", new String[] { "zh" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mAC3.getResourceObject("TestFileLanguageResource", new String[] { "en", "p2" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // non-parameterized resources (should fail)
      ex = null;
      try {
        mAC3.getResourceObject("TestFileResource", new String[] { "en" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mAC3.getResourceObject("TestResourceObject", new String[] { "de" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      Object r5 = mAC3.getResourceObject("Unknown", new String[] { "en" });
      Assert.assertNull(r5);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetResourceAsStreamStringStringArray() throws Exception {
    try {
      // standard data resource
      InputStream strm = mAC3
              .getResourceAsStream("TestFileLanguageResource", new String[] { "en" });
      Assert.assertNotNull(strm);

      InputStream strm2 = mAC3.getResourceAsStream("TestFileLanguageResource",
              new String[] { "de" });
      Assert.assertNotNull(strm2);
      Assert.assertFalse(strm2.equals(strm));

      // custom object (should return null)
      InputStream strm3 = mAC3.getResourceAsStream("TestLanguageResourceObject",
              new String[] { "en" });
      Assert.assertNull(strm3);

      // parameter values for which no resource exists (should fail)
      AnnotatorContextException ex = null;
      try {
        mAC3.getResourceAsStream("TestFileLanguageResource", new String[] { "zh" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mAC3.getResourceAsStream("TestFileLanguageResource", new String[] { "en", "p2" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // non-parameterized resources (should fail)
      ex = null;
      try {
        mAC3.getResourceAsStream("TestFileResource", new String[] { "en" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mAC3.getResourceAsStream("TestResourceObject", new String[] { "de" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      InputStream strm4 = mAC3.getResourceAsStream("Unknown", new String[] { "en" });
      Assert.assertNull(strm4);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetResourceURLStringStringArray() throws Exception {
    try {
      // standard data resource
      URL url = mAC3.getResourceURL("TestFileLanguageResource", new String[] { "en" });
      Assert.assertNotNull(url);

      URL url2 = mAC3.getResourceURL("TestFileLanguageResource", new String[] { "de" });
      Assert.assertNotNull(url2);
      Assert.assertFalse(url2.toString().equals(url.toString()));

      // custom object (should return null)
      URL url3 = mAC3.getResourceURL("TestLanguageResourceObject", new String[] { "en" });
      Assert.assertNull(url3);

      // parameter values for which no resource exists (should fail)
      AnnotatorContextException ex = null;
      try {
        mAC3.getResourceURL("TestFileLanguageResource", new String[] { "zh" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mAC3.getResourceURL("TestFileLanguageResource", new String[] { "en", "p2" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // non-parameterized resources (should fail)
      ex = null;
      try {
        mAC3.getResourceURL("TestFileResource", new String[] { "en" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mAC3.getResourceURL("TestResourceObject", new String[] { "de" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      URL url4 = mAC3.getResourceURL("Unknown", new String[] { "en" });
      Assert.assertNull(url4);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetResourceURIStringStringArray() throws Exception {
    try {
      // standard data resource
      URI uri = mAC3.getResourceURI("TestFileLanguageResource", new String[] { "en" });
      Assert.assertNotNull(uri);

      URI uri2 = mAC3.getResourceURI("TestFileLanguageResource", new String[] { "de" });
      Assert.assertNotNull(uri2);
      Assert.assertFalse(uri2.equals(uri));

      // custom object (should return null)
      URI uri3 = mAC3.getResourceURI("TestLanguageResourceObject", new String[] { "en" });
      Assert.assertNull(uri3);

      // parameter values for which no resource exists (should fail)
      AnnotatorContextException ex = null;
      try {
        mAC3.getResourceURI("TestFileLanguageResource", new String[] { "zh" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mAC3.getResourceURI("TestFileLanguageResource", new String[] { "en", "p2" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // non-parameterized resources (should fail)
      ex = null;
      try {
        mAC3.getResourceURI("TestFileResource", new String[] { "en" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mAC3.getResourceURI("TestResourceObject", new String[] { "de" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      URI uri4 = mAC3.getResourceURI("Unknown", new String[] { "en" });
      Assert.assertNull(uri4);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetResourceFilePathStringStringArray() throws Exception {
    try {
      // standard data resource
      String path = mAC3.getResourceFilePath("TestFileLanguageResource", new String[] { "en" });
      Assert.assertNotNull(path);

      String path2 = mAC3.getResourceFilePath("TestFileLanguageResource", new String[] { "de" });
      Assert.assertNotNull(path2);
      Assert.assertFalse(path2.equals(path));

      // custom object (should return null)
      String path3 = mAC3.getResourceFilePath("TestLanguageResourceObject", new String[] { "en" });
      Assert.assertNull(path3);

      // parameter values for which no resource exists (should fail)
      AnnotatorContextException ex = null;
      try {
        mAC3.getResourceFilePath("TestFileLanguageResource", new String[] { "zh" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mAC3.getResourceFilePath("TestFileLanguageResource", new String[] { "en", "p2" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // non-parameterized resources (should fail)
      ex = null;
      try {
        mAC3.getResourceFilePath("TestFileResource", new String[] { "en" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mAC3.getResourceFilePath("TestResourceObject", new String[] { "de" });
      } catch (AnnotatorContextException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      String path4 = mAC3.getResourceFilePath("Unknown", new String[] { "en" });
      Assert.assertNull(path4);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetDataPath() throws Exception {
    try {
      Assert.assertEquals(TEST_DATAPATH, mAC1.getDataPath());
      Assert.assertEquals(System.getProperty("user.dir"), mAC4.getDataPath());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
}
