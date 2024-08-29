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

package org.apache.uima.cas.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.TypeSystem2Xml;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import static org.assertj.core.api.Assertions.assertThat;

class TypeSystemTest {

  private class SetupTest implements AnnotatorInitializer {
    /**
     * @see org.apache.uima.cas.test.AnnotatorInitializer#initTypeSystem(org.apache.uima.cas.admin.TypeSystemMgr)
     */
    @Override
    public void initTypeSystem(TypeSystemMgr tsm) {

      // ///////////////////////////////////////////////////////////////////////
      // Check type names.

      boolean exc = false;
      Type annot = tsm.getType(CAS.TYPE_NAME_ANNOTATION);
      assertThat(annot != null).isTrue();
      // Check for some illegal characters in type names.
      try {
        tsm.addType("TestWithADash-", annot);
      } catch (CASAdminException e) {
        assertThat(e.getMessageKey().equals(CASAdminException.BAD_TYPE_SYNTAX)).isTrue();
        exc = true;
      }
      assertThat(exc).isTrue();
      exc = false;
      try {
        tsm.addType("test.with.a.slash/", annot);
      } catch (CASAdminException e) {
        assertThat(e.getMessageKey().equals(CASAdminException.BAD_TYPE_SYNTAX)).isTrue();
        exc = true;
      }
      assertThat(exc).isTrue();
      exc = false;
      // Check for empty identifiers (period at beginning or end, two or
      // more
      // periods in a row).
      try {
        tsm.addType("test.empty.identifier.", annot);
      } catch (CASAdminException e) {
        assertThat(e.getMessageKey().equals(CASAdminException.BAD_TYPE_SYNTAX)).isTrue();
        exc = true;
      }
      assertThat(exc).isTrue();
      exc = false;
      try {
        tsm.addType(".test.empty.identifier", annot);
      } catch (CASAdminException e) {
        assertThat(e.getMessageKey().equals(CASAdminException.BAD_TYPE_SYNTAX)).isTrue();
        exc = true;
      }
      assertThat(exc).isTrue();
      exc = false;
      try {
        tsm.addType("test.empty..identifier", annot);
      } catch (CASAdminException e) {
        assertThat(e.getMessageKey().equals(CASAdminException.BAD_TYPE_SYNTAX)).isTrue();
        exc = true;
      }
      assertThat(exc).isTrue();
      exc = false;
      // Test underscore behavior (leading underscores are out, other ones
      // in).
      try {
        tsm.addType("test._leading.Underscore", annot);
      } catch (CASAdminException e) {
        assertThat(e.getMessageKey().equals(CASAdminException.BAD_TYPE_SYNTAX)).isTrue();
        exc = true;
      }
      assertThat(exc).isTrue();
      exc = false;
      try {
        tsm.addType("test_embedded.Under__Score", annot);
      } catch (CASAdminException e) {
        assertThat(e.getMessageKey().equals(CASAdminException.BAD_TYPE_SYNTAX)).isTrue();
        exc = true;
      }
      assertThat(exc).isFalse();
      exc = false;
      // Test (un)qualified names.
      String qualName = "this.is.a.NameTestType";
      String shortName = "NameTestType";
      Type nameTest = tsm.addType(qualName, annot);
      assertThat(nameTest != null).isTrue();
      assertThat(qualName.equals(nameTest.getName())).isTrue();
      assertThat(shortName.equals(nameTest.getShortName())).isTrue();

      // ///////////////////////////////////////////////////////////////////////
      // Test features (names and inheritance

      // Set up: add two new annotation types, one inheriting from the
      // other.
      Type annot1 = tsm.addType("Annot1", annot);
      assertThat(annot1 != null).isTrue();
      String annot2name = "Annot2";
      Type annot2 = tsm.addType(annot2name, annot1);
      assertThat(annot2 != null).isTrue();
      // Another name test.
      assertThat(annot1.getName().equals(annot1.getShortName())).isTrue();
      String annot3name = "Annot3";
      Type annot3 = tsm.addType(annot3name, annot2);
      assertThat(annot3 != null).isTrue();

      // Check for bug reported by Marshall: feature names can not be
      // retrieved
      // by name on types more than one level removed from the introducing
      // type.
      assertThat(tsm.getFeatureByFullName(
          annot2name + TypeSystem.FEATURE_SEPARATOR + CAS.FEATURE_BASE_NAME_BEGIN) != null).isTrue();

      String inhTestFeat = "inhTestFeat";
      tsm.addFeature(inhTestFeat, annot1, nameTest);
      assertThat(tsm.getFeatureByFullName(
          annot2name + TypeSystem.FEATURE_SEPARATOR + inhTestFeat) != null).isTrue();

      // Test illegal feature names.
      Feature feat = null;
      try {
        feat = tsm.addFeature("_testLeadingUnderscore", annot1, annot);
      } catch (CASAdminException e) {
        exc = true;
      }
      assertThat(exc).isTrue();
      exc = false;
      try {
        feat = tsm.addFeature("test space", annot1, annot);
      } catch (CASAdminException e) {
        exc = true;
      }
      assertThat(exc).isTrue();
      exc = false;
      try {
        feat = tsm.addFeature("test.Qualified:name", annot1, annot);
      } catch (CASAdminException e) {
        exc = true;
      }
      assertThat(exc).isTrue();
      exc = false;

      // Now actually create a feature.
      String featName = "testInheritanceFeat";
      try {
        feat = tsm.addFeature(featName, annot2, annot);
      } catch (CASAdminException e) {
        assertThat(false).isTrue();
      }
      assertThat(feat != null).isTrue();

      // Check that a feature of the same name can be created on a
      // supertype if it has the same range
      Feature feat2 = null;
      try {
        feat2 = tsm.addFeature(featName, annot1, annot);
      } catch (CASAdminException e) {
        e.printStackTrace();
        assertThat(false).isTrue();
      }
      assertThat(feat2 != null).isTrue();
      // Check that a feature of the same name can not be created on a
      // subtype.
      /*
       * Test commented out with defect 3351: it's ok to want to create the same feature on a
       * subtype. try { feat2 = tsm.addFeature(featName, annot3, annot); } catch (CASAdminException
       * e) { assertTrue(false); } assertTrue(feat2 == null);
       */
      // Check that trying to create the same feature on a subtype with a
      // different
      // range type will raise an exception.
      exc = false;
      try {
        feat2 = tsm.addFeature(featName, annot2, annot2);
      } catch (CASAdminException e) {
        exc = true;
        assertThat(e.getMessageKey().equals(CASAdminException.DUPLICATE_FEATURE)).isTrue();
      }
      assertThat(exc).isTrue();
      // Check that a feature of the same name _can_ be created on a
      // different
      // type.
      String annot4Name = "Annot4";
      Type annot4 = tsm.addType(annot4Name, annot);
      assertThat(annot4 != null).isTrue();
      feat = tsm.addFeature(featName, annot4, annot1);
      assertThat(feat != null).isTrue();
      assertThat(featName.equals(feat.getShortName())).isTrue();
      assertThat(feat.getName().equals(annot4Name + TypeSystem.FEATURE_SEPARATOR + featName)).isTrue();
      // Check that we can't add features to top, inherit from arrays etc.
      Type top = tsm.getTopType();
      Type intT = tsm.getType(CAS.TYPE_NAME_INTEGER);
      exc = false;
      try {
        tsm.addFeature("testFeature", top, intT);
      } catch (CASAdminException e) {
        exc = true;
        assertThat(e.getMessageKey().equals(CASAdminException.TYPE_IS_FEATURE_FINAL)).isTrue();
      }
      assertThat(exc).isTrue();
      exc = false;
      try {
        tsm.addFeature("testFeature", intT, intT);
      } catch (CASAdminException e) {
        exc = true;
        assertThat(e.getMessageKey().equals(CASAdminException.TYPE_IS_FEATURE_FINAL)).isTrue();
      }
      assertThat(exc).isTrue();
      exc = false;
      try {
        tsm.addType("newType", intT);
      } catch (CASAdminException e) {
        exc = true;
        assertThat(e.getMessageKey().equals(CASAdminException.TYPE_IS_INH_FINAL)).isTrue();
      }
      assertThat(exc).isTrue();
      exc = false;
      try {
        tsm.addType("newType", tsm.getType(CAS.TYPE_NAME_FLOAT_ARRAY));
      } catch (CASAdminException e) {
        exc = true;
        assertThat(e.getMessageKey().equals(CASAdminException.TYPE_IS_INH_FINAL)).isTrue();
      }
      assertThat(exc).isTrue();
    }

    /**
     * @see org.apache.uima.cas.test.AnnotatorInitializer#initIndexes(org.apache.uima.cas.admin.FSIndexRepositoryMgr,
     *      org.apache.uima.cas.TypeSystem)
     */
    @Override
    public void initIndexes(FSIndexRepositoryMgr irm, TypeSystem ats) {
      // Do nothing for the purposes of this test.
    }

  }

  private CAS cas;

  private TypeSystem ts;

  @BeforeEach
  public void setUp() throws Exception {
    try {
      cas = CASInitializer.initCas(new CASTestSetup(), null);
      ts = cas.getTypeSystem();
    } catch (Exception e) {
      e.printStackTrace();
      assertThat(false).isTrue();
    }
  }

  @AfterEach
  public void tearDown() {
    cas = null;
    ts = null;
  }

  @Test
  void testSuperTypeBuiltIn() {
    CAS cas = CASInitializer.initCas(new SetupTest(), null);
    TypeSystem ts = cas.getTypeSystem();
    Type stringArray = ts.getType("uima.cas.StringArray");
    assertThat(ts.getParent(stringArray).getName()).isEqualTo("uima.cas.ArrayBase");
  }

  @Test
  void testNameChecking() {
    CAS tcas = CASInitializer.initCas(new SetupTest(), null);
    assertThat(tcas != null).isTrue();
  }

  @Test
  void testGetParent() {
    assertThat(ts.getParent(ts.getType(CAS.TYPE_NAME_TOP)) == null).isTrue();
    Type annotBase = ts.getType(CAS.TYPE_NAME_ANNOTATION_BASE);
    assertThat(ts.getParent(annotBase) == ts.getTopType()).isTrue();
    Type annot = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    assertThat(ts.getParent(annot) == ts.getType(CAS.TYPE_NAME_ANNOTATION_BASE)).isTrue();
    assertThat(ts.getParent(ts.getType(CASTestSetup.TOKEN_TYPE)) == annot).isTrue();
  }

  @Test
  void testGetType() {
    Type top = ts.getTopType();
    assertThat(top != null).isTrue();
    assertThat(top.getName().equals(CAS.TYPE_NAME_TOP)).isTrue();
    Type annot = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    assertThat(annot != null).isTrue();
    Type token = ts.getType(CASTestSetup.TOKEN_TYPE);
    assertThat(token != null).isTrue();
  }

  /*
   * Test for Feature getFeature(String)
   */
  @Test
  void testGetFeature() {
    Type annot = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    Feature start = ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_BEGIN);
    Feature end = ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_END);
    Type integer = ts.getType(CAS.TYPE_NAME_INTEGER);
    assertThat(start.getDomain() == annot).isTrue();
    assertThat(end.getDomain() == annot).isTrue();
    assertThat(start.getRange() == integer).isTrue();
    assertThat(end.getRange() == integer).isTrue();
    Feature start1 = annot.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_BEGIN);
    assertThat(start == start1).isTrue();
    Feature start2 = ts.getType(CASTestSetup.TOKEN_TYPE)
            .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_BEGIN);
    assertThat(start == start2).isTrue();
  }

  @Test
  void testGetTypeIterator() {
    Iterator<Type> it = ts.getTypeIterator();
    // Put the type names in a vector and do some spot checks.
    List<String> v = new ArrayList<>();
    while (it.hasNext()) {
      v.add(it.next().getName());
    }
    assertThat(v.contains(CAS.TYPE_NAME_TOP)).isTrue();
    assertThat(v.contains(CAS.TYPE_NAME_FLOAT)).isTrue();
    assertThat(v.contains(CAS.TYPE_NAME_FS_ARRAY)).isTrue();
    assertThat(v.contains(CAS.TYPE_NAME_ANNOTATION)).isTrue();
    assertThat(v.contains(CASTestSetup.SENT_TYPE)).isTrue();
  }

  @Test
  void testGetFeatures() {
    Iterator<Feature> it = ts.getFeatures();
    // Put feature names in vector and test for some known features.
    List<String> v = new ArrayList<>();
    while (it.hasNext()) {
      v.add(it.next().getName());
    }
    String annotPrefix = CAS.TYPE_NAME_ANNOTATION + TypeSystem.FEATURE_SEPARATOR;
    String arrayPrefix = CAS.TYPE_NAME_ARRAY_BASE + TypeSystem.FEATURE_SEPARATOR;
    assertThat(arrayPrefix != null).isTrue();
    String tokenPrefix = CASTestSetup.TOKEN_TYPE + TypeSystem.FEATURE_SEPARATOR;
    assertThat(v.contains(annotPrefix + CAS.FEATURE_BASE_NAME_BEGIN)).isTrue();
    assertThat(v.contains(annotPrefix + CAS.FEATURE_BASE_NAME_END)).isTrue();
    assertThat(v.contains(tokenPrefix + CASTestSetup.TOKEN_TYPE_FEAT)).isTrue();
    // assertTrue(v.contains(arrayPrefix + CAS.ARRAY_LENGTH_FEAT));
  }

  @Test
  void testGetTopType() {
    Type top = ts.getTopType();
    assertThat(top != null).isTrue();
    assertThat(top.getName().equals(CAS.TYPE_NAME_TOP)).isTrue();
  }

  @Test
  void testGetDirectlySubsumedTypes() {
    List<Type> subTypes = ts.getDirectSubtypes(ts.getType(CAS.TYPE_NAME_TOP));
    Type intType = ts.getType(CAS.TYPE_NAME_INTEGER);
    assertThat(subTypes.contains(intType)).isTrue();
    Type annotBaseType = ts.getType(CAS.TYPE_NAME_ANNOTATION_BASE);
    assertThat(subTypes.contains(annotBaseType)).isTrue();
    Type annotType = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    assertThat(!subTypes.contains(annotType)).isTrue();
    Type tokenType = ts.getType(CASTestSetup.TOKEN_TYPE);
    assertThat(!subTypes.contains(tokenType)).isTrue();
  }

  /*
   * Test for boolean subsumes(Type, Type)
   */
  @Test
  void testSubsumes() {
    Type top = ts.getTopType();
    Type intType = ts.getType(CAS.TYPE_NAME_INTEGER);
    Type annotType = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    Type tokenType = ts.getType(CASTestSetup.TOKEN_TYPE);
    assertThat(ts.subsumes(top, intType)).isTrue();
    assertThat(ts.subsumes(top, annotType)).isTrue();
    assertThat(ts.subsumes(top, tokenType)).isTrue();
    assertThat(ts.subsumes(annotType, tokenType)).isTrue();
    assertThat(!ts.subsumes(tokenType, annotType)).isTrue();
    assertThat(!ts.subsumes(tokenType, top)).isTrue();

    Type stringType = ts.getType(CAS.TYPE_NAME_STRING);
    Type substringType = ts.getType(CASTestSetup.GROUP_1);
    assertThat(ts.subsumes(stringType, substringType)).isTrue();
  }

  /**
   * Test presence of builtin types and their properties.
   */
  @Test
  void testBuiltinTypes() {
    assertThat(cas.getTypeSystem().getType(CAS.TYPE_NAME_FLOAT).isInheritanceFinal()).isTrue();
    assertThat(cas.getTypeSystem().getType(CAS.TYPE_NAME_FLOAT).isFeatureFinal()).isTrue();
    assertThat(cas.getTypeSystem().getTopType().isFeatureFinal()).isTrue();
    assertThat(cas.getTypeSystem().getTopType().isInheritanceFinal()).isFalse();
  }

  /**
   * Test creation of type system with static [T]CASFactory methods.
   */
  @Test
  void testCreateTypeSystem() {
    // Test creation of CAS type system.
    TypeSystemMgr tsMgr = CASFactory.createTypeSystem();
    Type top = tsMgr.getTopType();
    assertThat(top).isNotNull();
    assertThat(tsMgr.getType(CAS.TYPE_NAME_FLOAT).isInheritanceFinal()).isTrue();
    assertThat(tsMgr.getType(CAS.TYPE_NAME_FLOAT).isFeatureFinal()).isTrue();
    assertThat(tsMgr.getTopType().isFeatureFinal()).isTrue();
    assertThat(tsMgr.getTopType().isInheritanceFinal()).isFalse();
    Type someType = tsMgr.addType("some.type", top);
    assertThat(someType).isNotNull();

  }

  /*
   * Test array types.
   */
  @Test
  void testArrayTypes() {
    // Our version of object arrays. Type is built-in and has special name,
    // for backwards compatibility.
    Type fsArrayType = ts.getType(CAS.TYPE_NAME_FS_ARRAY);
    assertThat(fsArrayType).isNotNull();
    assertThat(fsArrayType.isArray()).isTrue();
    assertThat(fsArrayType.getComponentType()).isNotNull();
    assertThat(fsArrayType.getComponentType().equals(ts.getTopType())).isTrue();
    assertThat(fsArrayType.equals(ts.getArrayType(ts.getTopType()))).isTrue();
    // Int arrays are also built-in, but are primitive-valued.
    Type intArrayType = ts.getType(CAS.TYPE_NAME_INTEGER_ARRAY);
    assertThat(intArrayType).isNotNull();
    assertThat(intArrayType.isArray()).isTrue();
    assertThat(intArrayType.getComponentType()).isNotNull();
    assertThat(intArrayType.getComponentType().equals(ts.getType(CAS.TYPE_NAME_INTEGER))).isTrue();
    assertThat(intArrayType.equals(ts.getArrayType(ts.getType(CAS.TYPE_NAME_INTEGER)))).isTrue();
    // Negative tests: make sure regular types are not classified as arrays.
    Type stringType = ts.getType(CAS.TYPE_NAME_STRING);
    assertThat(stringType.isArray()).isFalse();
    assertThat(stringType.getComponentType()).isNull();
    Type topType = ts.getTopType();
    assertThat(topType.isArray()).isFalse();
    assertThat(topType.getComponentType()).isNull();
    // Create an array of arrays.
    Type intMatrix = ts.getArrayType(intArrayType);
    assertThat(intMatrix).isNotNull();
    assertThat(intMatrix.isArray()).isTrue();
    assertThat(intMatrix.getComponentType().equals(intArrayType)).isTrue();
    // Check array inheritance.
    Type annotationArray = ts.getArrayType(ts.getType(CAS.TYPE_NAME_ANNOTATION));
    assertThat(ts.subsumes(fsArrayType, annotationArray)).isTrue();
    // assertFalse(this.ts.subsumes(annotationArray, fsArrayType));
  }

  @Test
  void testSerializeTypeSystem() {
    File descriptorFile = JUnitExtension.getFile("CASTests/desc/arrayValueDescriptor.xml");
    assertThat(descriptorFile.exists()).as("Descriptor must exist: " + descriptorFile.getAbsolutePath()).isTrue();
    TypeSystem typeSystem = null;
    try {
      XMLParser parser = UIMAFramework.getXMLParser();
      AnalysisEngineDescription spec = (AnalysisEngineDescription) parser
              .parse(new XMLInputSource(descriptorFile));
      typeSystem = UIMAFramework.produceAnalysisEngine(spec).newCAS().getTypeSystem();
    } catch (ResourceInitializationException e) {
      e.printStackTrace();
      assertThat(false).isTrue();
    } catch (InvalidXMLException e) {
      e.printStackTrace();
      assertThat(false).isTrue();
    } catch (IOException e) {
      e.printStackTrace();
      assertThat(false).isTrue();
    }
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      TypeSystem2Xml.typeSystem2Xml(typeSystem, os);
    } catch (SAXException e) {
      assertThat(false).isTrue();
    } catch (IOException e) {
      assertThat(false).isTrue();
    }
    try {
      os.close();
    } catch (IOException e) {
      assertThat(false).isTrue();
    }
    InputStream is = new ByteArrayInputStream(os.toByteArray());
    // System.out.println(os.toString());
    XMLInputSource xis = new XMLInputSource(is, new File("."));
    Object descriptor = null;
    try {
      descriptor = UIMAFramework.getXMLParser().parse(xis);
    } catch (InvalidXMLException e) {
      assertThat(false).isTrue();
    }
    // instantiate CAS to get type system. Also build style
    // map file if there is none.
    TypeSystemDescription tsDesc = (TypeSystemDescription) descriptor;
    try {
      tsDesc.resolveImports();
    } catch (InvalidXMLException e) {
      assertThat(false).isTrue();
    }
    try {
      CasCreationUtils.createCas(tsDesc, null, new FsIndexDescription[] {});
    } catch (ResourceInitializationException e) {
      assertThat(false).isTrue();
    }
  }

  @Test
  void testSerializeParameterizedArrayTypeSystem() {

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      TypeSystem2Xml.typeSystem2Xml(ts, os);
    } catch (SAXException e) {
      assertThat(false).isTrue();
    } catch (IOException e) {
      assertThat(false).isTrue();
    }
    try {
      os.close();
    } catch (IOException e) {
      assertThat(false).isTrue();
    }
    InputStream is = new ByteArrayInputStream(os.toByteArray());
    // System.out.println(os.toString());
    XMLInputSource xis = new XMLInputSource(is, new File("."));
    Object descriptor = null;
    try {
      descriptor = UIMAFramework.getXMLParser().parse(xis);
    } catch (InvalidXMLException e) {
      assertThat(false).isTrue();
    }
    // instantiate CAS to get type system. Also build style
    // map file if there is none.
    TypeSystemDescription tsDesc = (TypeSystemDescription) descriptor;
    try {
      tsDesc.resolveImports();
    } catch (InvalidXMLException e) {
      assertThat(false).isTrue();
    }
    try {
      CasCreationUtils.createCas(tsDesc, null, new FsIndexDescription[] {});
    } catch (ResourceInitializationException e) {
      assertThat(false).isTrue();
    }
  }
}
