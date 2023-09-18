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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

/**
 * Class comment for TypeSystemTest.java goes here.
 * 
 */
public class TypeSystemTest {

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
      assertTrue(annot != null);
      // Check for some illegal characters in type names.
      try {
        tsm.addType("TestWithADash-", annot);
      } catch (CASAdminException e) {
        assertTrue(e.getMessageKey().equals(CASAdminException.BAD_TYPE_SYNTAX));
        exc = true;
      }
      assertTrue(exc);
      exc = false;
      try {
        tsm.addType("test.with.a.slash/", annot);
      } catch (CASAdminException e) {
        assertTrue(e.getMessageKey().equals(CASAdminException.BAD_TYPE_SYNTAX));
        exc = true;
      }
      assertTrue(exc);
      exc = false;
      // Check for empty identifiers (period at beginning or end, two or
      // more
      // periods in a row).
      try {
        tsm.addType("test.empty.identifier.", annot);
      } catch (CASAdminException e) {
        assertTrue(e.getMessageKey().equals(CASAdminException.BAD_TYPE_SYNTAX));
        exc = true;
      }
      assertTrue(exc);
      exc = false;
      try {
        tsm.addType(".test.empty.identifier", annot);
      } catch (CASAdminException e) {
        assertTrue(e.getMessageKey().equals(CASAdminException.BAD_TYPE_SYNTAX));
        exc = true;
      }
      assertTrue(exc);
      exc = false;
      try {
        tsm.addType("test.empty..identifier", annot);
      } catch (CASAdminException e) {
        assertTrue(e.getMessageKey().equals(CASAdminException.BAD_TYPE_SYNTAX));
        exc = true;
      }
      assertTrue(exc);
      exc = false;
      // Test underscore behavior (leading underscores are out, other ones
      // in).
      try {
        tsm.addType("test._leading.Underscore", annot);
      } catch (CASAdminException e) {
        assertTrue(e.getMessageKey().equals(CASAdminException.BAD_TYPE_SYNTAX));
        exc = true;
      }
      assertTrue(exc);
      exc = false;
      try {
        tsm.addType("test_embedded.Under__Score", annot);
      } catch (CASAdminException e) {
        assertTrue(e.getMessageKey().equals(CASAdminException.BAD_TYPE_SYNTAX));
        exc = true;
      }
      assertFalse(exc);
      exc = false;
      // Test (un)qualified names.
      String qualName = "this.is.a.NameTestType";
      String shortName = "NameTestType";
      Type nameTest = tsm.addType(qualName, annot);
      assertTrue(nameTest != null);
      assertTrue(qualName.equals(nameTest.getName()));
      assertTrue(shortName.equals(nameTest.getShortName()));

      // ///////////////////////////////////////////////////////////////////////
      // Test features (names and inheritance

      // Set up: add two new annotation types, one inheriting from the
      // other.
      Type annot1 = tsm.addType("Annot1", annot);
      assertTrue(annot1 != null);
      String annot2name = "Annot2";
      Type annot2 = tsm.addType(annot2name, annot1);
      assertTrue(annot2 != null);
      // Another name test.
      assertTrue(annot1.getName().equals(annot1.getShortName()));
      String annot3name = "Annot3";
      Type annot3 = tsm.addType(annot3name, annot2);
      assertTrue(annot3 != null);

      // Check for bug reported by Marshall: feature names can not be
      // retrieved
      // by name on types more than one level removed from the introducing
      // type.
      assertTrue(tsm.getFeatureByFullName(
              annot2name + TypeSystem.FEATURE_SEPARATOR + CAS.FEATURE_BASE_NAME_BEGIN) != null);

      String inhTestFeat = "inhTestFeat";
      tsm.addFeature(inhTestFeat, annot1, nameTest);
      assertTrue(tsm.getFeatureByFullName(
              annot2name + TypeSystem.FEATURE_SEPARATOR + inhTestFeat) != null);

      // Test illegal feature names.
      Feature feat = null;
      try {
        feat = tsm.addFeature("_testLeadingUnderscore", annot1, annot);
      } catch (CASAdminException e) {
        exc = true;
      }
      assertTrue(exc);
      exc = false;
      try {
        feat = tsm.addFeature("test space", annot1, annot);
      } catch (CASAdminException e) {
        exc = true;
      }
      assertTrue(exc);
      exc = false;
      try {
        feat = tsm.addFeature("test.Qualified:name", annot1, annot);
      } catch (CASAdminException e) {
        exc = true;
      }
      assertTrue(exc);
      exc = false;

      // Now actually create a feature.
      String featName = "testInheritanceFeat";
      try {
        feat = tsm.addFeature(featName, annot2, annot);
      } catch (CASAdminException e) {
        assertTrue(false);
      }
      assertTrue(feat != null);

      // Check that a feature of the same name can be created on a
      // supertype if it has the same range
      Feature feat2 = null;
      try {
        feat2 = tsm.addFeature(featName, annot1, annot);
      } catch (CASAdminException e) {
        e.printStackTrace();
        assertTrue(false);
      }
      assertTrue(feat2 != null);
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
        assertTrue(e.getMessageKey().equals(CASAdminException.DUPLICATE_FEATURE));
      }
      assertTrue(exc);
      // Check that a feature of the same name _can_ be created on a
      // different
      // type.
      String annot4Name = "Annot4";
      Type annot4 = tsm.addType(annot4Name, annot);
      assertTrue(annot4 != null);
      feat = tsm.addFeature(featName, annot4, annot1);
      assertTrue(feat != null);
      assertTrue(featName.equals(feat.getShortName()));
      assertTrue(feat.getName().equals(annot4Name + TypeSystem.FEATURE_SEPARATOR + featName));
      // Check that we can't add features to top, inherit from arrays etc.
      Type top = tsm.getTopType();
      Type intT = tsm.getType(CAS.TYPE_NAME_INTEGER);
      exc = false;
      try {
        tsm.addFeature("testFeature", top, intT);
      } catch (CASAdminException e) {
        exc = true;
        assertTrue(e.getMessageKey().equals(CASAdminException.TYPE_IS_FEATURE_FINAL));
      }
      assertTrue(exc);
      exc = false;
      try {
        tsm.addFeature("testFeature", intT, intT);
      } catch (CASAdminException e) {
        exc = true;
        assertTrue(e.getMessageKey().equals(CASAdminException.TYPE_IS_FEATURE_FINAL));
      }
      assertTrue(exc);
      exc = false;
      try {
        tsm.addType("newType", intT);
      } catch (CASAdminException e) {
        exc = true;
        assertTrue(e.getMessageKey().equals(CASAdminException.TYPE_IS_INH_FINAL));
      }
      assertTrue(exc);
      exc = false;
      try {
        tsm.addType("newType", tsm.getType(CAS.TYPE_NAME_FLOAT_ARRAY));
      } catch (CASAdminException e) {
        exc = true;
        assertTrue(e.getMessageKey().equals(CASAdminException.TYPE_IS_INH_FINAL));
      }
      assertTrue(exc);
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
      assertTrue(false);
    }
  }

  @AfterEach
  public void tearDown() {
    cas = null;
    ts = null;
  }

  @Test
  public void testSuperTypeBuiltIn() {
    CAS cas = CASInitializer.initCas(new SetupTest(), null);
    TypeSystem ts = cas.getTypeSystem();
    Type stringArray = ts.getType("uima.cas.StringArray");
    assertEquals("uima.cas.ArrayBase", ts.getParent(stringArray).getName());
  }

  @Test
  public void testNameChecking() {
    CAS tcas = CASInitializer.initCas(new SetupTest(), null);
    assertTrue(tcas != null);
  }

  @Test
  public void testGetParent() {
    assertTrue(ts.getParent(ts.getType(CAS.TYPE_NAME_TOP)) == null);
    Type annotBase = ts.getType(CAS.TYPE_NAME_ANNOTATION_BASE);
    assertTrue(ts.getParent(annotBase) == ts.getTopType());
    Type annot = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    assertTrue(ts.getParent(annot) == ts.getType(CAS.TYPE_NAME_ANNOTATION_BASE));
    assertTrue(ts.getParent(ts.getType(CASTestSetup.TOKEN_TYPE)) == annot);
  }

  @Test
  public void testGetType() {
    Type top = ts.getTopType();
    assertTrue(top != null);
    assertTrue(top.getName().equals(CAS.TYPE_NAME_TOP));
    Type annot = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    assertTrue(annot != null);
    Type token = ts.getType(CASTestSetup.TOKEN_TYPE);
    assertTrue(token != null);
  }

  /*
   * Test for Feature getFeature(String)
   */
  @Test
  public void testGetFeature() {
    Type annot = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    Feature start = ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_BEGIN);
    Feature end = ts.getFeatureByFullName(CAS.FEATURE_FULL_NAME_END);
    Type integer = ts.getType(CAS.TYPE_NAME_INTEGER);
    assertTrue(start.getDomain() == annot);
    assertTrue(end.getDomain() == annot);
    assertTrue(start.getRange() == integer);
    assertTrue(end.getRange() == integer);
    Feature start1 = annot.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_BEGIN);
    assertTrue(start == start1);
    Feature start2 = ts.getType(CASTestSetup.TOKEN_TYPE)
            .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_BEGIN);
    assertTrue(start == start2);
  }

  @Test
  public void testGetTypeIterator() {
    Iterator<Type> it = ts.getTypeIterator();
    // Put the type names in a vector and do some spot checks.
    List<String> v = new ArrayList<>();
    while (it.hasNext()) {
      v.add(it.next().getName());
    }
    assertTrue(v.contains(CAS.TYPE_NAME_TOP));
    assertTrue(v.contains(CAS.TYPE_NAME_FLOAT));
    assertTrue(v.contains(CAS.TYPE_NAME_FS_ARRAY));
    assertTrue(v.contains(CAS.TYPE_NAME_ANNOTATION));
    assertTrue(v.contains(CASTestSetup.SENT_TYPE));
  }

  @Test
  public void testGetFeatures() {
    Iterator<Feature> it = ts.getFeatures();
    // Put feature names in vector and test for some known features.
    List<String> v = new ArrayList<>();
    while (it.hasNext()) {
      v.add(it.next().getName());
    }
    String annotPrefix = CAS.TYPE_NAME_ANNOTATION + TypeSystem.FEATURE_SEPARATOR;
    String arrayPrefix = CAS.TYPE_NAME_ARRAY_BASE + TypeSystem.FEATURE_SEPARATOR;
    assertTrue(arrayPrefix != null);
    String tokenPrefix = CASTestSetup.TOKEN_TYPE + TypeSystem.FEATURE_SEPARATOR;
    assertTrue(v.contains(annotPrefix + CAS.FEATURE_BASE_NAME_BEGIN));
    assertTrue(v.contains(annotPrefix + CAS.FEATURE_BASE_NAME_END));
    assertTrue(v.contains(tokenPrefix + CASTestSetup.TOKEN_TYPE_FEAT));
    // assertTrue(v.contains(arrayPrefix + CAS.ARRAY_LENGTH_FEAT));
  }

  @Test
  public void testGetTopType() {
    Type top = ts.getTopType();
    assertTrue(top != null);
    assertTrue(top.getName().equals(CAS.TYPE_NAME_TOP));
  }

  @Test
  public void testGetDirectlySubsumedTypes() {
    List<Type> subTypes = ts.getDirectSubtypes(ts.getType(CAS.TYPE_NAME_TOP));
    Type intType = ts.getType(CAS.TYPE_NAME_INTEGER);
    assertTrue(subTypes.contains(intType));
    Type annotBaseType = ts.getType(CAS.TYPE_NAME_ANNOTATION_BASE);
    assertTrue(subTypes.contains(annotBaseType));
    Type annotType = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    assertTrue(!subTypes.contains(annotType));
    Type tokenType = ts.getType(CASTestSetup.TOKEN_TYPE);
    assertTrue(!subTypes.contains(tokenType));
  }

  /*
   * Test for boolean subsumes(Type, Type)
   */
  @Test
  public void testSubsumes() {
    Type top = ts.getTopType();
    Type intType = ts.getType(CAS.TYPE_NAME_INTEGER);
    Type annotType = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    Type tokenType = ts.getType(CASTestSetup.TOKEN_TYPE);
    assertTrue(ts.subsumes(top, intType));
    assertTrue(ts.subsumes(top, annotType));
    assertTrue(ts.subsumes(top, tokenType));
    assertTrue(ts.subsumes(annotType, tokenType));
    assertTrue(!ts.subsumes(tokenType, annotType));
    assertTrue(!ts.subsumes(tokenType, top));

    Type stringType = ts.getType(CAS.TYPE_NAME_STRING);
    Type substringType = ts.getType(CASTestSetup.GROUP_1);
    assertTrue(ts.subsumes(stringType, substringType));
  }

  /**
   * Test presence of builtin types and their properties.
   */
  @Test
  public void testBuiltinTypes() {
    assertTrue(cas.getTypeSystem().getType(CAS.TYPE_NAME_FLOAT).isInheritanceFinal());
    assertTrue(cas.getTypeSystem().getType(CAS.TYPE_NAME_FLOAT).isFeatureFinal());
    assertTrue(cas.getTypeSystem().getTopType().isFeatureFinal());
    assertFalse(cas.getTypeSystem().getTopType().isInheritanceFinal());
  }

  /**
   * Test creation of type system with static [T]CASFactory methods.
   */
  @Test
  public void testCreateTypeSystem() {
    // Test creation of CAS type system.
    TypeSystemMgr tsMgr = CASFactory.createTypeSystem();
    Type top = tsMgr.getTopType();
    assertNotNull(top);
    assertTrue(tsMgr.getType(CAS.TYPE_NAME_FLOAT).isInheritanceFinal());
    assertTrue(tsMgr.getType(CAS.TYPE_NAME_FLOAT).isFeatureFinal());
    assertTrue(tsMgr.getTopType().isFeatureFinal());
    assertFalse(tsMgr.getTopType().isInheritanceFinal());
    Type someType = tsMgr.addType("some.type", top);
    assertNotNull(someType);

  }

  /**
   * Test array types.
   * 
   */
  @Test
  public void testArrayTypes() {
    // Our version of object arrays. Type is built-in and has special name,
    // for backwards compatibility.
    Type fsArrayType = ts.getType(CAS.TYPE_NAME_FS_ARRAY);
    assertNotNull(fsArrayType);
    assertTrue(fsArrayType.isArray());
    assertNotNull(fsArrayType.getComponentType());
    assertTrue(fsArrayType.getComponentType().equals(ts.getTopType()));
    assertTrue(fsArrayType.equals(ts.getArrayType(ts.getTopType())));
    // Int arrays are also built-in, but are primitive-valued.
    Type intArrayType = ts.getType(CAS.TYPE_NAME_INTEGER_ARRAY);
    assertNotNull(intArrayType);
    assertTrue(intArrayType.isArray());
    assertNotNull(intArrayType.getComponentType());
    assertTrue(intArrayType.getComponentType().equals(ts.getType(CAS.TYPE_NAME_INTEGER)));
    assertTrue(intArrayType.equals(ts.getArrayType(ts.getType(CAS.TYPE_NAME_INTEGER))));
    // Negative tests: make sure regular types are not classified as arrays.
    Type stringType = ts.getType(CAS.TYPE_NAME_STRING);
    assertFalse(stringType.isArray());
    assertNull(stringType.getComponentType());
    Type topType = ts.getTopType();
    assertFalse(topType.isArray());
    assertNull(topType.getComponentType());
    // Create an array of arrays.
    Type intMatrix = ts.getArrayType(intArrayType);
    assertNotNull(intMatrix);
    assertTrue(intMatrix.isArray());
    assertTrue(intMatrix.getComponentType().equals(intArrayType));
    // Check array inheritance.
    Type annotationArray = ts.getArrayType(ts.getType(CAS.TYPE_NAME_ANNOTATION));
    assertTrue(ts.subsumes(fsArrayType, annotationArray));
    // assertFalse(this.ts.subsumes(annotationArray, fsArrayType));
  }

  @Test
  public void testSerializeTypeSystem() {
    File descriptorFile = JUnitExtension.getFile("CASTests/desc/arrayValueDescriptor.xml");
    assertTrue("Descriptor must exist: " + descriptorFile.getAbsolutePath(),
            descriptorFile.exists());
    TypeSystem typeSystem = null;
    try {
      XMLParser parser = UIMAFramework.getXMLParser();
      AnalysisEngineDescription spec = (AnalysisEngineDescription) parser
              .parse(new XMLInputSource(descriptorFile));
      typeSystem = UIMAFramework.produceAnalysisEngine(spec).newCAS().getTypeSystem();
    } catch (ResourceInitializationException e) {
      e.printStackTrace();
      assertTrue(false);
    } catch (InvalidXMLException e) {
      e.printStackTrace();
      assertTrue(false);
    } catch (IOException e) {
      e.printStackTrace();
      assertTrue(false);
    }
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      TypeSystem2Xml.typeSystem2Xml(typeSystem, os);
    } catch (SAXException e) {
      assertTrue(false);
    } catch (IOException e) {
      assertTrue(false);
    }
    try {
      os.close();
    } catch (IOException e) {
      assertTrue(false);
    }
    InputStream is = new ByteArrayInputStream(os.toByteArray());
    // System.out.println(os.toString());
    XMLInputSource xis = new XMLInputSource(is, new File("."));
    Object descriptor = null;
    try {
      descriptor = UIMAFramework.getXMLParser().parse(xis);
    } catch (InvalidXMLException e) {
      assertTrue(false);
    }
    // instantiate CAS to get type system. Also build style
    // map file if there is none.
    TypeSystemDescription tsDesc = (TypeSystemDescription) descriptor;
    try {
      tsDesc.resolveImports();
    } catch (InvalidXMLException e) {
      assertTrue(false);
    }
    try {
      CasCreationUtils.createCas(tsDesc, null, new FsIndexDescription[] {});
    } catch (ResourceInitializationException e) {
      assertTrue(false);
    }
  }

  @Test
  public void testSerializeParameterizedArrayTypeSystem() {

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      TypeSystem2Xml.typeSystem2Xml(ts, os);
    } catch (SAXException e) {
      assertTrue(false);
    } catch (IOException e) {
      assertTrue(false);
    }
    try {
      os.close();
    } catch (IOException e) {
      assertTrue(false);
    }
    InputStream is = new ByteArrayInputStream(os.toByteArray());
    // System.out.println(os.toString());
    XMLInputSource xis = new XMLInputSource(is, new File("."));
    Object descriptor = null;
    try {
      descriptor = UIMAFramework.getXMLParser().parse(xis);
    } catch (InvalidXMLException e) {
      assertTrue(false);
    }
    // instantiate CAS to get type system. Also build style
    // map file if there is none.
    TypeSystemDescription tsDesc = (TypeSystemDescription) descriptor;
    try {
      tsDesc.resolveImports();
    } catch (InvalidXMLException e) {
      assertTrue(false);
    }
    try {
      CasCreationUtils.createCas(tsDesc, null, new FsIndexDescription[] {});
    } catch (ResourceInitializationException e) {
      assertTrue(false);
    }
  }
}
