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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ResultSpecification_implTest {

  private TypeOrFeature[] mTypesAndFeatures;

  private Capability[] capabilities;

  private Capability cap4;

  private List<String[]> languages;

  private TypeOrFeature t1, t2, f1;

  @BeforeEach
  public void setUp() throws Exception {
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
      languages = new Vector<>(3);
      languages.add(0, languages1);
      languages.add(1, languages2);
      languages.add(2, languages3);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
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
      assertThat(result.length).isEqualTo(mTypesAndFeatures.length);
      for (int i = 0; i < result.length; i++) {
        assertThat(result[i]).isEqualTo(mTypesAndFeatures[i]);
      }

      // test defaulting - if no language, should be x-unspecified
      resultSpecLanguage = new ResultSpecification_impl();
      resultSpecLanguage.addCapabilities(new Capability[] { cap4 });
      assertTrue(resultSpecLanguage.containsFeature("FakeType:FakeFeature"));
      assertTrue(resultSpecLanguage.containsFeature("FakeType:FakeFeature", "en"));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
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

  @Test
  public void testSetResultTypesAndFeatures() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.setResultTypesAndFeatures(mTypesAndFeatures);

      // check
      TypeOrFeature[] result = rs.getResultTypesAndFeatures();
      // sort array before check
      Arrays.sort(result);
      Arrays.sort(mTypesAndFeatures);
      assertThat(result.length).isEqualTo(mTypesAndFeatures.length);
      for (int i = 0; i < result.length; i++) {
        assertThat(result[i]).isEqualTo(mTypesAndFeatures[i]);
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testSetResultTypesAndFeaturesWithLanguage() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.setResultTypesAndFeatures(mTypesAndFeatures, languages.get(0));

      // check for language en
      TypeOrFeature[] resultEn = rs.getResultTypesAndFeatures("en");
      // sort array before check
      Arrays.sort(resultEn);
      Arrays.sort(mTypesAndFeatures);
      assertThat(resultEn.length).isEqualTo(mTypesAndFeatures.length);
      for (int i = 0; i < resultEn.length; i++) {
        assertThat(resultEn[i]).isEqualTo(mTypesAndFeatures[i]);
      }

      // check for language ja
      TypeOrFeature[] resultJa = rs.getResultTypesAndFeatures("ja");
      assertThat(resultJa.length).isEqualTo(0);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
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
      assertThat(result.length).isEqualTo(mTypesAndFeatures.length);
      for (int i = 0; i < result.length; i++) {
        assertThat(result[i]).isEqualTo(mTypesAndFeatures[i]);
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
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

      assertThat(resultEn.length).isEqualTo(expectedEnResult.length);
      for (int i = 0; i < resultEn.length; i++) {
        assertThat(resultEn[i]).isEqualTo(expectedEnResult[i]);
      }

      // check for language de
      TypeOrFeature[] expectedDeResult = new TypeOrFeature[] { t1, t2 };
      Arrays.sort(expectedDeResult);
      TypeOrFeature[] resultDe = rs.getResultTypesAndFeatures("de");
      Arrays.sort(resultDe);

      assertThat(resultDe.length).isEqualTo(expectedDeResult.length);
      for (int i = 0; i < resultDe.length; i++) {
        assertThat(resultDe[i]).isEqualTo(expectedDeResult[i]);
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testAddResultType() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.addResultType("FakeType", false);
      rs.addResultType("AnotherType", true);

      // check
      TypeOrFeature[] result = rs.getResultTypesAndFeatures();
      assertThat(result.length).isEqualTo(2);
      int ftIndex = result[0].getName().equals("FakeType") ? 0 : 1;
      int atIndex = ftIndex == 0 ? 1 : 0;
      assertThat(result[ftIndex].getName()).isEqualTo("FakeType");
      assertThat(result[ftIndex].isType()).isEqualTo(true);
      assertThat(result[ftIndex].isAllAnnotatorFeatures()).isEqualTo(false);
      assertThat(result[atIndex].getName()).isEqualTo("AnotherType");
      assertThat(result[atIndex].isType()).isEqualTo(true);
      assertThat(result[atIndex].isAllAnnotatorFeatures()).isEqualTo(true);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testAddResultTypeWithLanguage() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.addResultType("FakeType", false, languages.get(0));
      rs.addResultType("AnotherType", true, languages.get(2));
      rs.addResultType("NewDefinedType", true, new String[] { "ja" });

      // check for language en
      TypeOrFeature[] resultEn = rs.getResultTypesAndFeatures("en");
      Arrays.sort(resultEn);
      assertThat(resultEn.length).isEqualTo(2);
      int ftIndex = resultEn[0].getName().equals("FakeType") ? 0 : 1;
      int atIndex = ftIndex == 0 ? 1 : 0;
      assertThat(resultEn[atIndex].getName()).isEqualTo("AnotherType");
      assertThat(resultEn[atIndex].isType()).isEqualTo(true);
      assertThat(resultEn[atIndex].isAllAnnotatorFeatures()).isEqualTo(true);
      assertThat(resultEn[ftIndex].getName()).isEqualTo("FakeType");
      assertThat(resultEn[ftIndex].isType()).isEqualTo(true);
      assertThat(resultEn[ftIndex].isAllAnnotatorFeatures()).isEqualTo(false);

      // check for language ja
      TypeOrFeature[] resultJa = rs.getResultTypesAndFeatures("ja");
      Arrays.sort(resultJa);
      assertThat(resultJa.length).isEqualTo(2);
      atIndex = resultJa[0].getName().equals("AnotherType") ? 0 : 1;
      int ndtIndex = atIndex == 0 ? 1 : 0;
      assertThat(resultJa[atIndex].getName()).isEqualTo("AnotherType");
      assertThat(resultJa[atIndex].isType()).isEqualTo(true);
      assertThat(resultJa[atIndex].isAllAnnotatorFeatures()).isEqualTo(true);
      assertThat(resultJa[ndtIndex].getName()).isEqualTo("NewDefinedType");
      assertThat(resultJa[ndtIndex].isType()).isEqualTo(true);
      assertThat(resultJa[ndtIndex].isAllAnnotatorFeatures()).isEqualTo(true);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testAddResultFeature() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.addResultFeature("FakeType:FakeFeature");
      rs.addResultFeature("AnotherType:AnotherFeature");

      // check
      TypeOrFeature[] result = rs.getResultTypesAndFeatures();
      Arrays.sort(result);
      assertThat(result.length).isEqualTo(2);
      int atafIndex = result[0].getName().equals("AnotherType:AnotherFeature") ? 0 : 1;
      int ftffIndex = atafIndex == 0 ? 1 : 0;
      assertThat(result[atafIndex].getName()).isEqualTo("AnotherType:AnotherFeature");
      assertThat(result[atafIndex].isType()).isEqualTo(false);
      assertThat(result[ftffIndex].getName()).isEqualTo("FakeType:FakeFeature");
      assertThat(result[ftffIndex].isType()).isEqualTo(false);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testAddResultFeatureWithLanguage() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.addResultFeature("FakeType:FakeFeature", new String[] { "ja" });
      rs.addResultFeature("AnotherType:AnotherFeature", new String[] { "en" });

      // check for language en
      TypeOrFeature[] resultEn = rs.getResultTypesAndFeatures("en");
      assertThat(resultEn.length).isEqualTo(1);
      assertThat(resultEn[0].getName()).isEqualTo("AnotherType:AnotherFeature");
      assertThat(resultEn[0].isType()).isEqualTo(false);

      // check for language ja
      TypeOrFeature[] resultJa = rs.getResultTypesAndFeatures("ja");
      assertThat(resultJa.length).isEqualTo(1);
      assertThat(resultJa[0].getName()).isEqualTo("FakeType:FakeFeature");
      assertThat(resultJa[0].isType()).isEqualTo(false);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testContainsType() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.setResultTypesAndFeatures(mTypesAndFeatures);

      assertThat(rs.containsType("FakeType")).isTrue();
      assertThat(rs.containsType("NotThere")).isFalse();
      assertThat(rs.containsType("AnotherType")).isTrue();
      assertThat(rs.containsType("FakeType:FakeFeature")).isFalse();
      assertThat(rs.containsType("AnotherType:AnotherFeature")).isFalse();
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testContainsTypeWithLanguage() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.addCapabilities(capabilities);

      assertThat(rs.containsType("FakeType", "en")).isTrue();
      assertThat(rs.containsType("FakeType", "ja")).isFalse();
      assertThat(rs.containsType("NotThere", "en")).isFalse();
      assertThat(rs.containsType("AnotherType", "en-US")).isTrue();
      assertThat(rs.containsType("AnotherType", "x-unspecified")).isTrue();
      assertThat(rs.containsType("FakeType:FakeFeature", "de")).isFalse();
      assertThat(rs.containsType("AnotherType:AnotherFeature", "de")).isFalse();
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testContainsFeature() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.setResultTypesAndFeatures(mTypesAndFeatures);

      assertThat(rs.containsFeature("FakeType:FakeFeature")).isTrue();
      assertThat(rs.containsType("FakeType:FakeFeature2")).isFalse();
      assertThat(rs.containsFeature("AnotherType:AnotherFeature")).isTrue();
      assertThat(rs.containsFeature("AnotherType:YetAnotherFeature")).isTrue();
      assertThat(rs.containsFeature("AnotherType:asdfghjkl")).isTrue();
      assertThat(rs.containsType("NotThere:FakeFeature")).isFalse();
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testContainsFeatureWithLanguage() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.addCapabilities(capabilities);

      assertThat(rs.containsFeature("FakeType:FakeFeature", "ja")).isTrue();
      assertThat(rs.containsFeature("FakeType:FakeFeature", "en")).isTrue();
      assertThat(rs.containsFeature("FakeType:FakeFeature", "de")).isFalse();
      assertThat(rs.containsFeature("FakeType:FakeFeature2", "ja")).isFalse();
      assertThat(rs.containsFeature("FakeType:FakeFeature2", "x-unspecified")).isFalse();
      assertThat(rs.containsFeature("AnotherType:AnotherFeature", "en")).isTrue();
      assertThat(rs.containsFeature("AnotherType:YetAnotherFeature", "de")).isTrue();
      assertThat(rs.containsFeature("AnotherType1:YetAnotherFeature", "de")).isFalse();
      assertThat(rs.containsFeature("AnotherType:asdfghjkl", "ja")).isTrue();
      assertThat(rs.containsFeature("NotThere:FakeFeature", "ja")).isFalse();
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
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
      assertThat(result.length).isEqualTo(expectedResult.length);
      for (int i = 0; i < result.length; i++) {
        assertThat(result[i]).isEqualTo(expectedResult[i]);
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
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
      assertThat(rs.containsType("FakeType")).isTrue();
      assertThat(rs.containsType("NotThere")).isFalse();
      assertThat(rs.containsType("AnotherType")).isTrue();
      assertThat(rs.containsType("FakeType:FakeFeature")).isFalse();
      assertThat(rs.containsType("AnotherType:AnotherFeature")).isFalse();
      assertThat(rs.containsFeature("FakeType:FakeFeature")).isTrue();
      assertThat(rs.containsType("FakeType:FakeFeature2")).isFalse();
      assertThat(rs.containsFeature("AnotherType:AnotherFeature")).isTrue();
      assertThat(rs.containsFeature("AnotherType:YetAnotherFeature")).isTrue();
      assertThat(rs.containsFeature("AnotherType:asdfghjkl")).isTrue(); // unknown features are
                                                                        // there,
      // if the type says allFeats
      assertThat(rs.containsType("NotThere:FakeFeature")).isFalse();
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
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
      assertThat(rs.containsType("FakeType")).isFalse();
      assertThat(rs.containsType("FakeType", "en")).isTrue();
      assertThat(rs.containsType("FakeType", "en-us")).isTrue();
      assertThat(rs.containsType("FakeType", "EN_US")).isTrue();
      assertThat(rs.containsType("NotThere")).isFalse();
      assertThat(rs.containsType("AnotherType")).isTrue();
      assertThat(rs.containsType("FakeType:FakeFeature")).isFalse();
      assertThat(rs.containsType("AnotherType:AnotherFeature")).isFalse();
      assertThat(rs.containsFeature("FakeType:FakeFeature")).isFalse();
      assertThat(rs.containsType("FakeType:FakeFeature2")).isFalse();
      assertThat(rs.containsFeature("AnotherType:AnotherFeature")).isTrue();
      assertThat(rs.containsFeature("AnotherType:YetAnotherFeature")).isTrue();
      assertThat(rs.containsFeature("AnotherType:asdfghjkl")).isTrue(); // unknown features are
                                                                        // there
      // if type says allfeats
      assertThat(rs.containsType("NotThere:FakeFeature")).isFalse();
      assertThat(rs.containsFeature("NotThere:FakeFeature")).isFalse();
      assertThat(rs.containsType("SubType")).isFalse();
      assertThat(rs.containsType("SubType", "en")).isTrue();
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
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
      ResultSpecification newRS = UIMAFramework.getXMLParser()
              .parseResultSpecification(new XMLInputSource(is, null));
      TypeOrFeature[] tofs = newRS.getResultTypesAndFeatures();
      Arrays.sort(tofs);
      newRS.setResultTypesAndFeatures(tofs);
      assertThat(newRS).isEqualTo(rs);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testClone() throws Exception {
    try {
      ResultSpecification_impl rs = new ResultSpecification_impl();
      rs.setResultTypesAndFeatures(mTypesAndFeatures);

      ResultSpecification_impl rsNew = (ResultSpecification_impl) rs.clone();

      TypeOrFeature[] rsToFs = rs.getResultTypesAndFeatures();
      TypeOrFeature[] rsNewToFs = rsNew.getResultTypesAndFeatures();
      Arrays.sort(rsToFs);
      Arrays.sort(rsNewToFs);

      assertThat(rsNewToFs.length).isEqualTo(rsToFs.length);

      for (int i = 0; i < rsToFs.length; i++) {
        assertThat(rsNewToFs[i]).isEqualTo(rsToFs[i]);
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
}
