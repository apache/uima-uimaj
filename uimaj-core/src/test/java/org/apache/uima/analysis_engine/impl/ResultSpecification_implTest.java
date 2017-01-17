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

import static org.apache.uima.analysis_engine.impl.AnalysisEngineDescription_implTest.encoding;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.impl.Capability_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;


public class ResultSpecification_implTest extends TestCase {

  private TypeOrFeature[] mTypesAndFeatures;

  private Capability[] capabilities;
  
  private Capability cap4;

  private List<String[]> languages;

  private TypeOrFeature t1, t2, f1;
  
  /**
   * Constructor for ResultSpecification_implTest.
   * 
   * @param arg0
   */
  public ResultSpecification_implTest(String arg0) {
    super(arg0);
  }

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    try {
      // create array of types and features for use in testing
      t1 = new TypeOrFeature_impl();
      t1.setType(true);
      t1.setName("FakeType");
      t1.setAllAnnotatorFeatures(false);
      f1 = new TypeOrFeature_impl();
      f1.setType(false);
      f1.setName("FakeType:FakeFeature");
      t2 = new TypeOrFeature_impl();
      t2.setType(true);
      t2.setName("AnotherType");
      t2.setAllAnnotatorFeatures(true);
      mTypesAndFeatures = new TypeOrFeature[] { t1, f1, t2 };

      // create capability[] for language tests
      // capability 1 - using t1
      Capability cap1 = new Capability_impl();
      String[] languages1 = { "en", "de", "en-US", "en-GB" };
      TypeOrFeature[] tofs1 = { t1 };
      cap1.setLanguagesSupported(languages1);
      cap1.setOutputs(tofs1);

      // capability 2 - using f1
      Capability cap2 = new Capability_impl();
      String[] languages2 = { "ja", "en" };
      TypeOrFeature[] tofs2 = { f1 };
      cap2.setLanguagesSupported(languages2);
      cap2.setOutputs(tofs2);

      // capability 3 - using t2
      Capability cap3 = new Capability_impl();
      String[] languages3 = { "x-unspecified" };
      TypeOrFeature[] tofs3 = { t2 };
      cap3.setLanguagesSupported(languages3);
      cap3.setOutputs(tofs3);

      // capability 4 - using f1
      cap4 = new Capability_impl();
      String[] languages4 = {}; // length 0 string
      TypeOrFeature[] tofs4 = { f1 };
      cap4.setLanguagesSupported(languages4);
      cap4.setOutputs(tofs4);

      // make capability array with the above specified values
      capabilities = new Capability[] { cap1, cap2, cap3 };

      // make languages array
      languages = new Vector<String[]>(3);
      languages.add(0, languages1);
      languages.add(1, languages2);
      languages.add(2, languages3);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testAddCapabilities() throws Exception {
    try {
      // create ResultSpecification with capabilities
      ResultSpecification_impl resultSpecLanguage = new ResultSpecification_impl();
      // add capabilities to the result spec
      resultSpecLanguage.addCapabilities(capabilities);

      // check
      TypeOrFeature[] result = resultSpecLanguage.getResultTypesAndFeatures();
      // sort array before check
      Arrays.sort(result);
      Arrays.sort(mTypesAndFeatures);
      Assert.assertEquals(mTypesAndFeatures.length, result.length);
      for (int i = 0; i < result.length; i++) {
        Assert.assertEquals(mTypesAndFeatures[i], result[i]);
      }
      
      // test defaulting - if no language, should be x-unspecified
      resultSpecLanguage = new ResultSpecification_impl();
      resultSpecLanguage.addCapabilities(new Capability[] {cap4});
      assertTrue(resultSpecLanguage.containsFeature("FakeType:FakeFeature"));
      assertTrue(resultSpecLanguage.containsFeature("FakeType:FakeFeature", "en"));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testAddCapabilitiesWithoutLanguage() throws Exception {
     try {
       
        TypeOrFeature t4 = new TypeOrFeature_impl();
        t4.setType(true);
        t4.setName("AnotherFakeType");
        t4.setAllAnnotatorFeatures(false);

        // capability 4 - using t4 but now language
        Capability cap4 = new Capability_impl();
        TypeOrFeature[] tofs4 = { t4 };
        cap4.setOutputs(tofs4);

        // create ResultSpecification with capabilities
       ResultSpecification_impl resultSpec = new ResultSpecification_impl();
       // add capabilities to the result spec
       resultSpec.addCapabilities(new Capability[] { cap4 });

       assertTrue(resultSpec.containsType("AnotherFakeType"));
     } catch (Exception e) {
       JUnitExtension.handleException(e);
     }
   }

  public void testSetResultTypesAndFeatures() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.setResultTypesAndFeatures(mTypesAndFeatures);

      // check
      TypeOrFeature[] result = rs.getResultTypesAndFeatures();
      // sort array before check
      Arrays.sort(result);
      Arrays.sort(mTypesAndFeatures);
      Assert.assertEquals(mTypesAndFeatures.length, result.length);
      for (int i = 0; i < result.length; i++) {
        Assert.assertEquals(mTypesAndFeatures[i], result[i]);
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testSetResultTypesAndFeaturesWithLanguage() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.setResultTypesAndFeatures(mTypesAndFeatures, languages.get(0));

      // check for language en
      TypeOrFeature[] resultEn = rs.getResultTypesAndFeatures("en");
      // sort array before check
      Arrays.sort(resultEn);
      Arrays.sort(mTypesAndFeatures);
      Assert.assertEquals(mTypesAndFeatures.length, resultEn.length);
      for (int i = 0; i < resultEn.length; i++) {
        Assert.assertEquals(mTypesAndFeatures[i], resultEn[i]);
      }

      // check for language ja
      TypeOrFeature[] resultJa = rs.getResultTypesAndFeatures("ja");
      Assert.assertEquals(0, resultJa.length);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testAddResultTypeOrFeature() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      for (int i = 0; i < mTypesAndFeatures.length; i++) {
        rs.addResultTypeOrFeature(mTypesAndFeatures[i]);
      }

      // check
      TypeOrFeature[] result = rs.getResultTypesAndFeatures();
      Arrays.sort(result);
      Arrays.sort(mTypesAndFeatures);
      Assert.assertEquals(mTypesAndFeatures.length, result.length);
      for (int i = 0; i < result.length; i++) {
        Assert.assertEquals(mTypesAndFeatures[i], result[i]);
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testAddResultTypeOrFeatureWithLanguage() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      for (int i = 0; i < mTypesAndFeatures.length; i++) {
        rs.addResultTypeOrFeature(mTypesAndFeatures[i], languages.get(i));
      }

      // check for language en
      TypeOrFeature[] expectedEnResult = new TypeOrFeature[] { t1, t2, f1 };
      Arrays.sort(expectedEnResult);
      TypeOrFeature[] resultEn = rs.getResultTypesAndFeatures("en");
      Arrays.sort(resultEn);

      Assert.assertEquals(expectedEnResult.length, resultEn.length);
      for (int i = 0; i < resultEn.length; i++) {
        Assert.assertEquals(expectedEnResult[i], resultEn[i]);
      }

      // check for language de
      TypeOrFeature[] expectedDeResult = new TypeOrFeature[] { t1, t2 };
      Arrays.sort(expectedDeResult);
      TypeOrFeature[] resultDe = rs.getResultTypesAndFeatures("de");
      Arrays.sort(resultDe);

      Assert.assertEquals(expectedDeResult.length, resultDe.length);
      for (int i = 0; i < resultDe.length; i++) {
        Assert.assertEquals(expectedDeResult[i], resultDe[i]);
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testAddResultType() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.addResultType("FakeType", false);
      rs.addResultType("AnotherType", true);

      // check
      TypeOrFeature[] result = rs.getResultTypesAndFeatures();
      Assert.assertEquals(2, result.length);
      int ftIndex = result[0].getName().equals("FakeType") ? 0 : 1;
      int atIndex = ftIndex == 0 ? 1 : 0;
      Assert.assertEquals("FakeType", result[ftIndex].getName());
      Assert.assertEquals(true, result[ftIndex].isType());
      Assert.assertEquals(false, result[ftIndex].isAllAnnotatorFeatures());
      Assert.assertEquals("AnotherType", result[atIndex].getName());
      Assert.assertEquals(true, result[atIndex].isType());
      Assert.assertEquals(true, result[atIndex].isAllAnnotatorFeatures());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testAddResultTypeWithLanguage() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.addResultType("FakeType", false, languages.get(0));
      rs.addResultType("AnotherType", true, languages.get(2));
      rs.addResultType("NewDefinedType", true, new String[] { "ja" });

      // check for language en
      TypeOrFeature[] resultEn = rs.getResultTypesAndFeatures("en");
      Arrays.sort(resultEn);
      Assert.assertEquals(2, resultEn.length);
      int ftIndex = resultEn[0].getName().equals("FakeType") ? 0 : 1;
      int atIndex = ftIndex == 0 ? 1 : 0;
      Assert.assertEquals("AnotherType", resultEn[atIndex].getName());
      Assert.assertEquals(true, resultEn[atIndex].isType());
      Assert.assertEquals(true, resultEn[atIndex].isAllAnnotatorFeatures());
      Assert.assertEquals("FakeType", resultEn[ftIndex].getName());
      Assert.assertEquals(true, resultEn[ftIndex].isType());
      Assert.assertEquals(false, resultEn[ftIndex].isAllAnnotatorFeatures());

      // check for language ja
      TypeOrFeature[] resultJa = rs.getResultTypesAndFeatures("ja");
      Arrays.sort(resultJa);
      Assert.assertEquals(2, resultJa.length);
      atIndex = resultJa[0].getName().equals("AnotherType") ? 0 : 1;
      int ndtIndex = atIndex == 0 ? 1 : 0;
      Assert.assertEquals("AnotherType", resultJa[atIndex].getName());
      Assert.assertEquals(true, resultJa[atIndex].isType());
      Assert.assertEquals(true, resultJa[atIndex].isAllAnnotatorFeatures());
      Assert.assertEquals("NewDefinedType", resultJa[ndtIndex].getName());
      Assert.assertEquals(true, resultJa[ndtIndex].isType());
      Assert.assertEquals(true, resultJa[ndtIndex].isAllAnnotatorFeatures());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testAddResultFeature() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.addResultFeature("FakeType:FakeFeature");
      rs.addResultFeature("AnotherType:AnotherFeature");

      // check
      TypeOrFeature[] result = rs.getResultTypesAndFeatures();
      Arrays.sort(result);
      Assert.assertEquals(2, result.length);
      int atafIndex = result[0].getName().equals("AnotherType:AnotherFeature") ? 0 : 1;
      int ftffIndex = atafIndex == 0 ? 1 : 0;
      Assert.assertEquals("AnotherType:AnotherFeature", result[atafIndex].getName());
      Assert.assertEquals(false, result[atafIndex].isType());
      Assert.assertEquals("FakeType:FakeFeature", result[ftffIndex].getName());
      Assert.assertEquals(false, result[ftffIndex].isType());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testAddResultFeatureWithLanguage() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.addResultFeature("FakeType:FakeFeature", new String[] { "ja" });
      rs.addResultFeature("AnotherType:AnotherFeature", new String[] { "en" });

      // check for language en
      TypeOrFeature[] resultEn = rs.getResultTypesAndFeatures("en");
      Assert.assertEquals(1, resultEn.length);
      Assert.assertEquals("AnotherType:AnotherFeature", resultEn[0].getName());
      Assert.assertEquals(false, resultEn[0].isType());

      // check for language ja
      TypeOrFeature[] resultJa = rs.getResultTypesAndFeatures("ja");
      Assert.assertEquals(1, resultJa.length);
      Assert.assertEquals("FakeType:FakeFeature", resultJa[0].getName());
      Assert.assertEquals(false, resultJa[0].isType());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testContainsType() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.setResultTypesAndFeatures(mTypesAndFeatures);

      Assert.assertTrue(rs.containsType("FakeType"));
      Assert.assertFalse(rs.containsType("NotThere"));
      Assert.assertTrue(rs.containsType("AnotherType"));
      Assert.assertFalse(rs.containsType("FakeType:FakeFeature"));
      Assert.assertFalse(rs.containsType("AnotherType:AnotherFeature"));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testContainsTypeWithLanguage() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.addCapabilities(capabilities);

      Assert.assertTrue(rs.containsType("FakeType", "en"));
      Assert.assertFalse(rs.containsType("FakeType", "ja"));
      Assert.assertFalse(rs.containsType("NotThere", "en"));
      Assert.assertTrue(rs.containsType("AnotherType", "en-US"));
      Assert.assertTrue(rs.containsType("AnotherType", "x-unspecified"));
      Assert.assertFalse(rs.containsType("FakeType:FakeFeature", "de"));
      Assert.assertFalse(rs.containsType("AnotherType:AnotherFeature", "de"));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testContainsFeature() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.setResultTypesAndFeatures(mTypesAndFeatures);

      Assert.assertTrue(rs.containsFeature("FakeType:FakeFeature"));
      Assert.assertFalse(rs.containsType("FakeType:FakeFeature2"));
      Assert.assertTrue(rs.containsFeature("AnotherType:AnotherFeature"));
      Assert.assertTrue(rs.containsFeature("AnotherType:YetAnotherFeature"));
      Assert.assertTrue(rs.containsFeature("AnotherType:asdfghjkl"));
      Assert.assertFalse(rs.containsType("NotThere:FakeFeature"));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testContainsFeatureWithLanguage() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.addCapabilities(capabilities);

      Assert.assertTrue(rs.containsFeature("FakeType:FakeFeature", "ja"));
      Assert.assertTrue(rs.containsFeature("FakeType:FakeFeature", "en"));
      Assert.assertFalse(rs.containsFeature("FakeType:FakeFeature", "de"));
      Assert.assertFalse(rs.containsFeature("FakeType:FakeFeature2", "ja"));
      Assert.assertFalse(rs.containsFeature("FakeType:FakeFeature2", "x-unspecified"));
      Assert.assertTrue(rs.containsFeature("AnotherType:AnotherFeature", "en"));
      Assert.assertTrue(rs.containsFeature("AnotherType:YetAnotherFeature", "de"));
      Assert.assertFalse(rs.containsFeature("AnotherType1:YetAnotherFeature", "de"));
      Assert.assertTrue(rs.containsFeature("AnotherType:asdfghjkl", "ja"));
      Assert.assertFalse(rs.containsFeature("NotThere:FakeFeature", "ja"));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testRemoveTypeOrFeature() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.addCapabilities(capabilities);

      // remove t1
      rs.removeTypeOrFeature(t1);

      // check
      TypeOrFeature[] expectedResult = new TypeOrFeature[] { f1, t2 };
      TypeOrFeature[] result = rs.getResultTypesAndFeatures();
      Arrays.sort(result);
      Arrays.sort(expectedResult);
      Assert.assertEquals(expectedResult.length, result.length);
      for (int i = 0; i < result.length; i++) {
        Assert.assertEquals(expectedResult[i], result[i]);
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testCompile() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.setResultTypesAndFeatures(mTypesAndFeatures);

      // create Type System
      CASMgr casMgr = CASFactory.createCAS();
      TypeSystemMgr tsMgr = casMgr.getTypeSystemMgr();
      Type fakeType = tsMgr.addType("FakeType", tsMgr.getTopType());
      Type anotherType = tsMgr.addType("AnotherType", tsMgr.getTopType());
      tsMgr.addFeature("FakeFeature", fakeType, tsMgr.getTopType());
      tsMgr.addFeature("FakeFeature2", fakeType, tsMgr.getTopType());
      tsMgr.addFeature("AnotherFeature", anotherType, tsMgr.getTopType());
      tsMgr.addFeature("YetAnotherFeature", anotherType, tsMgr.getTopType());

      // compile
      rs.compile(tsMgr);

      // check
      Assert.assertTrue(rs.containsType("FakeType"));
      Assert.assertFalse(rs.containsType("NotThere"));
      Assert.assertTrue(rs.containsType("AnotherType"));
      Assert.assertFalse(rs.containsType("FakeType:FakeFeature"));
      Assert.assertFalse(rs.containsType("AnotherType:AnotherFeature"));
      Assert.assertTrue(rs.containsFeature("FakeType:FakeFeature"));
      Assert.assertFalse(rs.containsType("FakeType:FakeFeature2"));
      Assert.assertTrue(rs.containsFeature("AnotherType:AnotherFeature"));
      Assert.assertTrue(rs.containsFeature("AnotherType:YetAnotherFeature"));
      Assert.assertTrue(rs.containsFeature("AnotherType:asdfghjkl"));  // unknown features are there, if the type says allFeats
      Assert.assertFalse(rs.containsType("NotThere:FakeFeature"));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testCompileWithCapabilities() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.addCapabilities(capabilities);

      // create Type System
      CASMgr casMgr = CASFactory.createCAS();
      TypeSystemMgr tsMgr = casMgr.getTypeSystemMgr();
      Type fakeType = tsMgr.addType("FakeType", tsMgr.getTopType());
      Type anotherType = tsMgr.addType("AnotherType", tsMgr.getTopType());
      tsMgr.addFeature("FakeFeature", fakeType, tsMgr.getTopType());
      tsMgr.addFeature("FakeFeature2", fakeType, tsMgr.getTopType());
      tsMgr.addFeature("AnotherFeature", anotherType, tsMgr.getTopType());
      tsMgr.addFeature("YetAnotherFeature", anotherType, tsMgr.getTopType());
      tsMgr.addType("SubType", fakeType);

      // compile
      rs.compile(tsMgr);

      // check
      Assert.assertFalse(rs.containsType("FakeType"));
      Assert.assertTrue(rs.containsType("FakeType", "en"));
      Assert.assertTrue(rs.containsType("FakeType", "en-us"));      
      Assert.assertTrue(rs.containsType("FakeType", "EN_US"));
      Assert.assertFalse(rs.containsType("NotThere"));
      Assert.assertTrue(rs.containsType("AnotherType"));
      Assert.assertFalse(rs.containsType("FakeType:FakeFeature"));
      Assert.assertFalse(rs.containsType("AnotherType:AnotherFeature"));
      Assert.assertFalse(rs.containsFeature("FakeType:FakeFeature"));
      Assert.assertFalse(rs.containsType("FakeType:FakeFeature2"));
      Assert.assertTrue(rs.containsFeature("AnotherType:AnotherFeature"));
      Assert.assertTrue(rs.containsFeature("AnotherType:YetAnotherFeature"));
      Assert.assertTrue(rs.containsFeature("AnotherType:asdfghjkl")); // unknown features are there if type says allfeats
      Assert.assertFalse(rs.containsType("NotThere:FakeFeature"));
      Assert.assertFalse(rs.containsFeature("NotThere:FakeFeature"));
      Assert.assertFalse(rs.containsType("SubType"));
      Assert.assertTrue(rs.containsType("SubType", "en"));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testXmlization() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      Arrays.sort(mTypesAndFeatures);
      rs.setResultTypesAndFeatures(mTypesAndFeatures);

      // write object to XML
      StringWriter writer = new StringWriter();
      rs.toXML(writer);
      String rsXml = writer.getBuffer().toString();
      // System.out.println(rsXml);

      // parse object back from XML
      InputStream is = new ByteArrayInputStream(rsXml.getBytes(encoding));
      ResultSpecification newRS = UIMAFramework.getXMLParser().parseResultSpecification(
              new XMLInputSource(is, null));
      TypeOrFeature[] tofs = newRS.getResultTypesAndFeatures();
      Arrays.sort(tofs);
      newRS.setResultTypesAndFeatures(tofs);
      Assert.assertEquals(rs, newRS);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testClone() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.setResultTypesAndFeatures(mTypesAndFeatures);

      ResultSpecification_impl rsNew = (ResultSpecification_impl) rs.clone();

      TypeOrFeature[] rsToFs = rs.getResultTypesAndFeatures();
      TypeOrFeature[] rsNewToFs = rsNew.getResultTypesAndFeatures();
      Arrays.sort(rsToFs);
      Arrays.sort(rsNewToFs);

      Assert.assertEquals(rsToFs.length, rsNewToFs.length);

      for (int i = 0; i < rsToFs.length; i++) {
        Assert.assertEquals(rsToFs[i], rsNewToFs[i]);
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
}
