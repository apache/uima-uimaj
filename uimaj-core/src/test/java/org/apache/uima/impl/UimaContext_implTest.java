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

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.impl.ResourceCreationSpecifier_impl;
import org.apache.uima.resource.impl.TestResourceInterface;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.ResourceMetaData_impl;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;


public class UimaContext_implTest extends TestCase {
  protected final String TEST_DATAPATH = JUnitExtension.getFile("AnnotatorContextTest").getPath()
          + System.getProperty("path.separator") + JUnitExtension.getFile("ResourceTest");

  protected final String TEST_EXTENSION_CLASSPATH = JUnitExtension.getFile(
          "ResourceTest/spaces in dir name").getPath();

  private UimaContext mContext;

  private UimaContext mContext2;

  private UimaContext mContext3;

  private UimaContext mContext4;
  
  private UimaContext mContext5;

  /**
   * Constructor for UimaContext_implTest.
   * 
   * @param arg0
   */
  public UimaContext_implTest(String arg0) {
    super(arg0);
  }

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    try {
      // configure ResourceManager to allow test components to locate their resources
      ResourceManager rm = UIMAFramework.newDefaultResourceManager();
      rm.setDataPath(TEST_DATAPATH);
      rm.setExtensionClassPath(TEST_EXTENSION_CLASSPATH, true);

      // create a UimaContext with Config Params and Resources
      UIMAFramework.getXMLParser().enableSchemaValidation(true);
      CasConsumerDescription ccDesc = UIMAFramework.getXMLParser().parseCasConsumerDescription(
              new XMLInputSource(JUnitExtension
                      .getFile("UimaContextTest/CasConsumerForUimaContextTest.xml")));
      CasConsumer cc = UIMAFramework.produceCasConsumer(ccDesc, rm, null);
      mContext = cc.getUimaContext();

      // create a UimaContext with Config Params in Groups but no resources
      XMLInputSource in = new XMLInputSource(JUnitExtension
              .getFile("AnnotatorContextTest/AnnotatorWithConfigurationGroups.xml"));
      AnalysisEngineDescription taeDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(taeDesc, rm, null);
      mContext2 = ae.getUimaContext();

      // create a UimaContext with Groups and Groupless Parameters
      XMLInputSource in2 = new XMLInputSource(JUnitExtension
              .getFile("AnnotatorContextTest/AnnotatorWithGroupsAndNonGroupParams.xml"));
      AnalysisEngineDescription taeDesc2 = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in2);
      AnalysisEngine ae2 = UIMAFramework.produceAnalysisEngine(taeDesc2, rm, null);
      mContext3 = ae2.getUimaContext();

      // create a UimaContext with duplicate configuration groups
      XMLInputSource in3 = new XMLInputSource(JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AnnotatorWithDuplicateConfigurationGroups.xml"));
      AnalysisEngineDescription taeDesc3 = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in3);
      AnalysisEngine ae3 = UIMAFramework.produceAnalysisEngine(taeDesc3, rm, null);
      mContext4 = ae3.getUimaContext();
      super.setUp();

      // create a UimaContext for a CAS Multiplier
      XMLInputSource in4 = new XMLInputSource(JUnitExtension
              .getFile("TextAnalysisEngineImplTest/NewlineSegmenter.xml"));
      AnalysisEngineDescription taeDesc4 = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in4);
      AnalysisEngine ae4 = UIMAFramework.produceAnalysisEngine(taeDesc4);
      mContext5 = ae4.getUimaContext();
      super.setUp();
      
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetConfigParameterValueString() throws Exception {
    try {
      String str = (String) mContext.getConfigParameterValue("StringParam");
      Assert.assertEquals("myString", str);
      String[] strArr = (String[]) mContext.getConfigParameterValue("StringArrayParam");
      Assert.assertEquals(Arrays.asList(new String[] { "one", "two" }), Arrays.asList(strArr));
      Integer integer = (Integer) mContext.getConfigParameterValue("IntegerParam");
      Assert.assertEquals(Integer.valueOf(42), integer);
      Integer[] intArr = (Integer[]) mContext.getConfigParameterValue("IntegerArrayParam");
      Assert.assertEquals(Arrays.asList(new Integer[] { Integer.valueOf(1), Integer.valueOf(2),
          Integer.valueOf(3) }), Arrays.asList(intArr));
      Float flt = (Float) mContext.getConfigParameterValue("FloatParam");
      Assert.assertEquals(Float.valueOf(3.14F), flt);

      // default fallback
      String str2 = (String) mContext2.getConfigParameterValue("StringParam");
      Assert.assertEquals("en", str2);

      // get groupless param
      String str3 = (String) mContext3.getConfigParameterValue("GrouplessParam1");
      Assert.assertEquals("foo", str3);
      // default fallback in presence of groupless params (of different names)
      String str4 = (String) mContext3.getConfigParameterValue("StringParam");
      Assert.assertEquals("en", str4);
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
      String str = (String) mContext2.getConfigParameterValue("en-US", "StringParam");
      Assert.assertEquals("en", str); // language fallback

      String[] strArr = (String[]) mContext2.getConfigParameterValue("en-US", "StringArrayParam");
      Assert.assertEquals(5, strArr.length);
      Assert.assertEquals("e", strArr[0]);
      Assert.assertEquals("n", strArr[1]);
      Assert.assertEquals("-", strArr[2]);
      Assert.assertEquals("U", strArr[3]);
      Assert.assertEquals("S", strArr[4]);

      Integer intVal = (Integer) mContext2.getConfigParameterValue("en-US", "IntegerParam");
      Assert.assertEquals(1776, intVal.intValue());

      Integer[] intArr = (Integer[]) mContext2
              .getConfigParameterValue("en-US", "IntegerArrayParam");
      Assert.assertEquals(3, intArr.length); // language fallback
      Assert.assertEquals(1, intArr[0].intValue());
      Assert.assertEquals(2, intArr[1].intValue());
      Assert.assertEquals(3, intArr[2].intValue());

      Float floatVal = (Float) mContext2.getConfigParameterValue("en-US", "FloatParam");
      Assert.assertEquals(null, floatVal);

      // de group
      String str2 = (String) mContext2.getConfigParameterValue("de", "StringParam");
      Assert.assertEquals("de", str2);

      String[] strArr2 = (String[]) mContext2.getConfigParameterValue("de", "StringArrayParam");
      Assert.assertEquals(2, strArr2.length);
      Assert.assertEquals("d", strArr2[0]);
      Assert.assertEquals("e", strArr2[1]);

      Integer intVal2 = (Integer) mContext2.getConfigParameterValue("de", "IntegerParam");
      Assert.assertEquals(42, intVal2.intValue()); // default fallback

      Integer[] intArr2 = (Integer[]) mContext2.getConfigParameterValue("de", "IntegerArrayParam");
      Assert.assertEquals(3, intArr2.length);
      Assert.assertEquals(4, intArr2[0].intValue());
      Assert.assertEquals(5, intArr2[1].intValue());
      Assert.assertEquals(6, intArr2[2].intValue());

      Float floatVal2 = (Float) mContext2.getConfigParameterValue("de", "FloatParam");
      Assert.assertEquals(null, floatVal2);

      // zh group
      String str3 = (String) mContext2.getConfigParameterValue("zh", "StringParam");
      Assert.assertEquals("zh", str3);

      String[] strArr3 = (String[]) mContext2.getConfigParameterValue("zh", "StringArrayParam");
      Assert.assertEquals(2, strArr3.length);
      Assert.assertEquals("z", strArr3[0]);
      Assert.assertEquals("h", strArr3[1]);

      Integer intVal3 = (Integer) mContext2.getConfigParameterValue("zh", "IntegerParam");
      Assert.assertEquals(42, intVal3.intValue()); // default fallback

      Integer[] intArr3 = (Integer[]) mContext2.getConfigParameterValue("zh", "IntegerArrayParam");
      Assert.assertEquals(3, intArr3.length); // default fallback
      Assert.assertEquals(1, intArr3[0].intValue());
      Assert.assertEquals(2, intArr3[1].intValue());
      Assert.assertEquals(3, intArr3[2].intValue());

      Float floatVal3 = (Float) mContext2.getConfigParameterValue("zh", "FloatParam");
      Assert.assertEquals(3.14, floatVal3.floatValue(), 0.0001);

      // testing duplicate groups
      assertEquals("common-a", mContext4.getConfigParameterValue("a", "CommonParam"));
      assertEquals("ab-a", mContext4.getConfigParameterValue("a", "ABParam"));
      assertNull(mContext4.getConfigParameterValue("a", "BCParam"));
      assertEquals("common-b", mContext4.getConfigParameterValue("b", "CommonParam"));
      assertEquals("ab-b", mContext4.getConfigParameterValue("b", "ABParam"));
      assertEquals("bc-b", mContext4.getConfigParameterValue("b", "BCParam"));
      assertEquals("common-c", mContext4.getConfigParameterValue("c", "CommonParam"));
      assertEquals("bc-c", mContext4.getConfigParameterValue("c", "BCParam"));
      assertNull(mContext4.getConfigParameterValue("c", "ABParam"));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetConfigurationGroupNames() {
    String[] names = mContext2.getConfigurationGroupNames();
    Assert.assertEquals(5, names.length);
    ArrayList<String> l = new ArrayList<String>(Arrays.asList(names));
    Assert.assertTrue(l.contains("en"));
    Assert.assertTrue(l.contains("en-US"));
    Assert.assertTrue(l.contains("de"));
    Assert.assertTrue(l.contains("zh"));
    Assert.assertTrue(l.contains("x-unspecified"));

    // try on something with both groups and groupless parameters
    names = mContext3.getConfigurationGroupNames();
    Assert.assertEquals(5, names.length);
    l = new ArrayList<String>(Arrays.asList(names));
    Assert.assertTrue(l.contains("en"));
    Assert.assertTrue(l.contains("en-US"));
    Assert.assertTrue(l.contains("de"));
    Assert.assertTrue(l.contains("zh"));
    Assert.assertTrue(l.contains("x-unspecified"));

    // try on something that has no groups
    names = mContext.getConfigurationGroupNames();
    Assert.assertEquals(0, names.length);
  }

  public void testGetConfigParameterNames() {
    String[] names = mContext.getConfigParameterNames();
    Assert.assertEquals(6, names.length);
    Assert.assertEquals("StringParam", names[0]);
    Assert.assertEquals("StringArrayParam", names[1]);
    Assert.assertEquals("IntegerParam", names[2]);
    Assert.assertEquals("IntegerArrayParam", names[3]);
    Assert.assertEquals("FloatParam", names[4]);
    Assert.assertEquals("FloatArrayParam", names[5]);

    // try on something that has groups
    names = mContext2.getConfigParameterNames();
    Assert.assertEquals(0, names.length);

    // try on something with both groups and groupless parameters
    names = mContext3.getConfigParameterNames();
    Assert.assertEquals(2, names.length);
    Assert.assertEquals("GrouplessParam1", names[0]);
    Assert.assertEquals("GrouplessParam2", names[1]);
  }

  public void testGetConfigParameterNamesString() {
    String[] names = mContext2.getConfigParameterNames("en");
    Assert.assertEquals(4, names.length);
    ArrayList<String> l = new ArrayList<String>(Arrays.asList(names));
    Assert.assertTrue(l.contains("StringParam"));
    Assert.assertTrue(l.contains("StringArrayParam"));
    Assert.assertTrue(l.contains("IntegerParam"));
    Assert.assertTrue(l.contains("IntegerArrayParam"));

    // try on nonexistent group
    names = mContext2.getConfigParameterNames("foo");
    Assert.assertEquals(0, names.length);

    // try on something that has no groups
    names = mContext.getConfigParameterNames("en");
    Assert.assertEquals(0, names.length);

    // try on something with both groups and groupless params
    names = mContext3.getConfigParameterNames("en");
    Assert.assertEquals(4, names.length);
    l = new ArrayList<String>(Arrays.asList(names));
    Assert.assertTrue(l.contains("StringParam"));
    Assert.assertTrue(l.contains("StringArrayParam"));
    Assert.assertTrue(l.contains("IntegerParam"));
    Assert.assertTrue(l.contains("IntegerArrayParam"));

  }

  public void testGetResourceObjectString() throws Exception {
    try {
      // custom object
      Object r = mContext.getResourceObject("TestResourceObject");
      Assert.assertNotNull(r);
      Assert.assertTrue(r instanceof TestResourceInterface);

      // standard data resource
      Object r2 = mContext.getResourceObject("TestFileResource");
      Assert.assertNotNull(r2);
      Assert.assertTrue(r2 instanceof DataResource);

      // parameterized resources (should fail)
      ResourceAccessException ex = null;
      try {
        mContext.getResourceObject("TestFileLanguageResource");
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mContext.getResourceObject("TestLanguageResourceObject");
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      Object r3 = mContext.getResourceObject("Unknown");
      Assert.assertNull(r3);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetResourceURLString() throws Exception {
    try {
      // standard data resource (should succeed)
      URL url = mContext.getResourceURL("TestFileResource");
      Assert.assertNotNull(url);

      // custom resource object (should return null)
      URL url2 = mContext.getResourceURL("TestResourceObject");
      Assert.assertNull(url2);

      // parameterized resources (should fail)
      ResourceAccessException ex = null;
      try {
        mContext.getResourceURL("TestFileLanguageResource");
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mContext.getResourceURL("TestLanguageResourceObject");
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      URL url3 = mContext.getResourceURL("Unknown");
      Assert.assertNull(url3);

      // passthrough to class loader
      URL url5 = mContext.getResourceURL("org/apache/uima/analysis_engine/impl/testDataFile3.dat");
      Assert.assertNotNull(url5);

      // passthrough to data path
      URL url6 = mContext.getResourceURL("testDataFile.dat");
      Assert.assertNotNull(url6);

      // for directory
      URL url7 = mContext.getResourceURL("subdir");
      Assert.assertNotNull(url7);

      // spaces as part of extension classpath (spaces should be URL-encoded)
      URL url8 = mContext.getResourceURL("OtherFileResource");
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
      URI uri = mContext.getResourceURI("TestFileResource");
      Assert.assertNotNull(uri);

      // custom resource object (should return null)
      URI uri2 = mContext.getResourceURI("TestResourceObject");
      Assert.assertNull(uri2);

      // parameterized resources (should fail)
      ResourceAccessException ex = null;
      try {
        mContext.getResourceURI("TestFileLanguageResource");
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mContext.getResourceURI("TestLanguageResourceObject");
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      URI uri3 = mContext.getResourceURI("Unknown");
      Assert.assertNull(uri3);

      // passthrough to class loader
      URI uri5 = mContext.getResourceURI("org/apache/uima/analysis_engine/impl/testDataFile3.dat");
      Assert.assertNotNull(uri5);

      // passthrough to data path
      URI uri6 = mContext.getResourceURI("testDataFile.dat");
      Assert.assertNotNull(uri6);

      // for directory
      URI uri7 = mContext.getResourceURI("subdir");
      Assert.assertNotNull(uri7);

      // spaces as part of extension classpath (spaces should be decoded, unlike with URL)
      URI uri8 = mContext.getResourceURI("OtherFileResource");
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
      String path = mContext.getResourceFilePath("TestFileResource");
      Assert.assertNotNull(path);

      // custom resource object (should return null)
      String path2 = mContext.getResourceFilePath("TestResourceObject");
      Assert.assertNull(path2);

      // parameterized resources (should fail)
      ResourceAccessException ex = null;
      try {
        mContext.getResourceFilePath("TestFileLanguageResource");
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mContext.getResourceFilePath("TestLanguageResourceObject");
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      String path3 = mContext.getResourceFilePath("Unknown");
      Assert.assertNull(path3);

      // passthrough to class loader
      String path5 = mContext
              .getResourceFilePath("org/apache/uima/analysis_engine/impl/testDataFile3.dat");
      Assert.assertNotNull(path5);

      // passthrough to data path
      String path6 = mContext.getResourceFilePath("testDataFile.dat");
      Assert.assertNotNull(path6);

      // for directory
      String path7 = mContext.getResourceFilePath("subdir");
      Assert.assertNotNull(path7);

      // spaces as part of extension classpath (spaces should be decoded, unlike with URL)
      String path8 = mContext.getResourceFilePath("OtherFileResource");
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
      InputStream strm = mContext.getResourceAsStream("TestFileResource");
      Assert.assertNotNull(strm);

      // custom resource object (should return null)
      InputStream strm2 = mContext.getResourceAsStream("TestResourceObject");
      Assert.assertNull(strm2);

      // parameterized resources (should fail)
      ResourceAccessException ex = null;
      try {
        mContext.getResourceAsStream("TestFileLanguageResource");
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mContext.getResourceAsStream("TestLanguageResourceObject");
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      InputStream strm3 = mContext.getResourceAsStream("Unknown");
      Assert.assertNull(strm3);

      // passthrough to class loader
      InputStream strm4 = mContext
              .getResourceAsStream("org/apache/uima/analysis_engine/impl/testDataFile3.dat");
      Assert.assertNotNull(strm4);

      // passthrough to data path
      InputStream strm5 = mContext.getResourceAsStream("testDataFile.dat");
      Assert.assertNotNull(strm5);

      // for directory
      InputStream strm6 = mContext.getResourceAsStream("subdir");
      Assert.assertNotNull(strm6);

      // spaces as part of extension classpath
      InputStream strm7 = mContext.getResourceAsStream("OtherFileResource");
      assertNotNull(strm7);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetResourceObjectStringStringArray() throws Exception {
    try {
      // standard data resource
      Object r = mContext.getResourceObject("TestFileLanguageResource", new String[] { "en" });
      Assert.assertNotNull(r);
      Assert.assertTrue(r instanceof DataResource);
      Object r2 = mContext.getResourceObject("TestFileLanguageResource", new String[] { "de" });
      Assert.assertNotNull(r2);
      Assert.assertTrue(r2 instanceof DataResource);
      Assert.assertFalse(r2.equals(r));

      // custom object
      Object r3 = mContext.getResourceObject("TestLanguageResourceObject", new String[] { "en" });
      Assert.assertNotNull(r3);
      Assert.assertTrue(r3 instanceof TestResourceInterface);
      Object r4 = mContext.getResourceObject("TestLanguageResourceObject", new String[] { "de" });
      Assert.assertNotNull(r4);
      Assert.assertTrue(r4 instanceof TestResourceInterface);
      Assert.assertFalse(r4.equals(r3));

      // parameter values for which no resource exists (should fail)
      ResourceAccessException ex = null;
      try {
        mContext.getResourceObject("TestFileLanguageResource", new String[] { "zh" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mContext.getResourceObject("TestFileLanguageResource", new String[] { "en", "p2" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // non-parameterized resources (should fail)
      ex = null;
      try {
        mContext.getResourceObject("TestFileResource", new String[] { "en" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mContext.getResourceObject("TestResourceObject", new String[] { "de" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      Object r5 = mContext.getResourceObject("Unknown", new String[] { "en" });
      Assert.assertNull(r5);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetResourceAsStreamStringStringArray() throws Exception {
    try {
      // standard data resource
      InputStream strm = mContext.getResourceAsStream("TestFileLanguageResource",
              new String[] { "en" });
      Assert.assertNotNull(strm);

      InputStream strm2 = mContext.getResourceAsStream("TestFileLanguageResource",
              new String[] { "de" });
      Assert.assertNotNull(strm2);
      Assert.assertFalse(strm2.equals(strm));

      // custom object (should return null)
      InputStream strm3 = mContext.getResourceAsStream("TestLanguageResourceObject",
              new String[] { "en" });
      Assert.assertNull(strm3);

      // parameter values for which no resource exists (should fail)
      ResourceAccessException ex = null;
      try {
        mContext.getResourceAsStream("TestFileLanguageResource", new String[] { "zh" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mContext.getResourceAsStream("TestFileLanguageResource", new String[] { "en", "p2" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // non-parameterized resources (should fail)
      ex = null;
      try {
        mContext.getResourceAsStream("TestFileResource", new String[] { "en" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mContext.getResourceAsStream("TestResourceObject", new String[] { "de" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      InputStream strm4 = mContext.getResourceAsStream("Unknown", new String[] { "en" });
      Assert.assertNull(strm4);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetResourceURLStringStringArray() throws Exception {
    try {
      // standard data resource
      URL url = mContext.getResourceURL("TestFileLanguageResource", new String[] { "en" });
      Assert.assertNotNull(url);

      URL url2 = mContext.getResourceURL("TestFileLanguageResource", new String[] { "de" });
      Assert.assertNotNull(url2);
      Assert.assertFalse(url2.toString().equals(url.toString()));

      // custom object (should return null)
      URL url3 = mContext.getResourceURL("TestLanguageResourceObject", new String[] { "en" });
      Assert.assertNull(url3);

      // parameter values for which no resource exists (should fail)
      ResourceAccessException ex = null;
      try {
        mContext.getResourceURL("TestFileLanguageResource", new String[] { "zh" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mContext.getResourceURL("TestFileLanguageResource", new String[] { "en", "p2" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // non-parameterized resources (should fail)
      ex = null;
      try {
        mContext.getResourceURL("TestFileResource", new String[] { "en" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mContext.getResourceURL("TestResourceObject", new String[] { "de" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      URL url4 = mContext.getResourceURL("Unknown", new String[] { "en" });
      Assert.assertNull(url4);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetResourceURIStringStringArray() throws Exception {
    try {
      // standard data resource
      URI uri = mContext.getResourceURI("TestFileLanguageResource", new String[] { "en" });
      Assert.assertNotNull(uri);

      URI uri2 = mContext.getResourceURI("TestFileLanguageResource", new String[] { "de" });
      Assert.assertNotNull(uri2);
      Assert.assertFalse(uri2.equals(uri));

      // custom object (should return null)
      URI uri3 = mContext.getResourceURI("TestLanguageResourceObject", new String[] { "en" });
      Assert.assertNull(uri3);

      // parameter values for which no resource exists (should fail)
      ResourceAccessException ex = null;
      try {
        mContext.getResourceURI("TestFileLanguageResource", new String[] { "zh" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mContext.getResourceURI("TestFileLanguageResource", new String[] { "en", "p2" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // non-parameterized resources (should fail)
      ex = null;
      try {
        mContext.getResourceURI("TestFileResource", new String[] { "en" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mContext.getResourceURI("TestResourceObject", new String[] { "de" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      URI uri4 = mContext.getResourceURI("Unknown", new String[] { "en" });
      Assert.assertNull(uri4);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetResourceFilePathStringStringArray() throws Exception {
    try {
      // standard data resource
      String path = mContext.getResourceFilePath("TestFileLanguageResource", new String[] { "en" });
      Assert.assertNotNull(path);

      String path2 = mContext
              .getResourceFilePath("TestFileLanguageResource", new String[] { "de" });
      Assert.assertNotNull(path2);
      Assert.assertFalse(path2.equals(path));

      // custom object (should return null)
      String path3 = mContext.getResourceFilePath("TestLanguageResourceObject",
              new String[] { "en" });
      Assert.assertNull(path3);

      // parameter values for which no resource exists (should fail)
      ResourceAccessException ex = null;
      try {
        mContext.getResourceFilePath("TestFileLanguageResource", new String[] { "zh" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mContext.getResourceFilePath("TestFileLanguageResource", new String[] { "en", "p2" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // non-parameterized resources (should fail)
      ex = null;
      try {
        mContext.getResourceFilePath("TestFileResource", new String[] { "en" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      ex = null;
      try {
        mContext.getResourceFilePath("TestResourceObject", new String[] { "de" });
      } catch (ResourceAccessException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // nonexistent resource (should return null)
      String path4 = mContext.getResourceFilePath("Unknown", new String[] { "en" });
      Assert.assertNull(path4);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetDataPath() throws Exception {
    try {
      Assert.assertEquals(TEST_DATAPATH, mContext.getDataPath());
      Assert.assertEquals(TEST_DATAPATH, mContext2.getDataPath());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
  
  public void testGetEmptyCas() throws Exception {
    try {
      CAS emptyCas = mContext5.getEmptyCas(CAS.class);
      //should be allowed to release this CAS 
      emptyCas.release();
      //and then get it again
      emptyCas = mContext5.getEmptyCas(CAS.class);
      emptyCas.release();      
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }    
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
