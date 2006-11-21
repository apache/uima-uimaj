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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.text.TCAS;

/**
 * Class comment for TypeSystemTest.java goes here.
 * 
 * @author Thilo Goetz
 */
public class TypeSystemTest extends TestCase {

  private class SetupTest implements AnnotatorInitializer {
    /**
     * @see org.apache.uima.cas.test.AnnotatorInitializer#initTypeSystem(org.apache.uima.cas.admin.TypeSystemMgr)
     */
    public void initTypeSystem(TypeSystemMgr tsm) {

      // ///////////////////////////////////////////////////////////////////////
      // Check type names.

      boolean exc = false;
      Type annot = tsm.getType(TCAS.TYPE_NAME_ANNOTATION);
      assertTrue(annot != null);
      // Check for some illegal characters in type names.
      try {
        tsm.addType("TestWithADash-", annot);
      } catch (CASAdminException e) {
        assertTrue(e.getError() == CASAdminException.BAD_TYPE_SYNTAX);
        exc = true;
      }
      assertTrue(exc);
      exc = false;
      try {
        tsm.addType("test.with.a.slash/", annot);
      } catch (CASAdminException e) {
        assertTrue(e.getError() == CASAdminException.BAD_TYPE_SYNTAX);
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
        assertTrue(e.getError() == CASAdminException.BAD_TYPE_SYNTAX);
        exc = true;
      }
      assertTrue(exc);
      exc = false;
      try {
        tsm.addType(".test.empty.identifier", annot);
      } catch (CASAdminException e) {
        assertTrue(e.getError() == CASAdminException.BAD_TYPE_SYNTAX);
        exc = true;
      }
      assertTrue(exc);
      exc = false;
      try {
        tsm.addType("test.empty..identifier", annot);
      } catch (CASAdminException e) {
        assertTrue(e.getError() == CASAdminException.BAD_TYPE_SYNTAX);
        exc = true;
      }
      assertTrue(exc);
      exc = false;
      // Test underscore behavior (leading underscores are out, other ones
      // in).
      try {
        tsm.addType("test._leading.Underscore", annot);
      } catch (CASAdminException e) {
        assertTrue(e.getError() == CASAdminException.BAD_TYPE_SYNTAX);
        exc = true;
      }
      assertTrue(exc);
      exc = false;
      try {
        tsm.addType("test_embedded.Under__Score", annot);
      } catch (CASAdminException e) {
        assertTrue(e.getError() == CASAdminException.BAD_TYPE_SYNTAX);
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
      assertTrue(tsm.getFeatureByFullName(annot2name + TypeSystem.FEATURE_SEPARATOR
                      + TCAS.FEATURE_BASE_NAME_BEGIN) != null);

      String inhTestFeat = "inhTestFeat";
      tsm.addFeature(inhTestFeat, annot1, nameTest);
      assertTrue(tsm.getFeatureByFullName(annot2name + TypeSystem.FEATURE_SEPARATOR + inhTestFeat) != null);

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

      // Check that a feature of the same name can not be created on a
      // supertype.
      Feature feat2 = null;
      try {
        feat2 = tsm.addFeature(featName, annot1, annot);
      } catch (CASAdminException e) {
        e.printStackTrace();
        assertTrue(false);
      }
      assertTrue(feat2 == null);
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
        assertTrue(e.getError() == CASAdminException.DUPLICATE_FEATURE);
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
        assertTrue(e.getError() == CASAdminException.TYPE_IS_FEATURE_FINAL);
      }
      assertTrue(exc);
      exc = false;
      try {
        tsm.addFeature("testFeature", intT, intT);
      } catch (CASAdminException e) {
        exc = true;
        assertTrue(e.getError() == CASAdminException.TYPE_IS_FEATURE_FINAL);
      }
      assertTrue(exc);
      exc = false;
      try {
        tsm.addType("newType", intT);
      } catch (CASAdminException e) {
        exc = true;
        assertTrue(e.getError() == CASAdminException.TYPE_IS_INH_FINAL);
      }
      assertTrue(exc);
      exc = false;
      try {
        tsm.addType("newType", tsm.getType(CAS.TYPE_NAME_FLOAT_ARRAY));
      } catch (CASAdminException e) {
        exc = true;
        assertTrue(e.getError() == CASAdminException.TYPE_IS_INH_FINAL);
      }
      assertTrue(exc);
    }

    /**
     * @see org.apache.uima.cas.test.AnnotatorInitializer#initIndexes(org.apache.uima.cas.admin.FSIndexRepositoryMgr,
     *      org.apache.uima.cas.TypeSystem)
     */
    public void initIndexes(FSIndexRepositoryMgr irm, TypeSystem ats) {
      // Do nothing for the purposes of this test.
    }

  }

  private TCAS cas;

  private TypeSystem ts;

  /**
   * Constructor for TypeSystemTest.
   * 
   * @param arg0
   */
  public TypeSystemTest(String arg0) {
    super(arg0);
  }

  /**
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    try {
      this.cas = CASInitializer.initCas(new CASTestSetup());
      this.ts = this.cas.getTypeSystem();
    } catch (Exception e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }

  public void tearDown() {
    this.cas = null;
    this.ts = null;
  }

  public void testNameChecking() {
    TCAS tcas = CASInitializer.initCas(new SetupTest());
    assertTrue(tcas != null);
  }

  public void testGetParent() {
    assertTrue(this.ts.getParent(this.ts.getType(CAS.TYPE_NAME_TOP)) == null);
    Type annotBase = this.ts.getType(TCAS.TYPE_NAME_ANNOTATION_BASE);
    assertTrue(this.ts.getParent(annotBase) == this.ts.getTopType());
    Type annot = this.ts.getType(TCAS.TYPE_NAME_ANNOTATION);
    assertTrue(this.ts.getParent(annot) == this.ts.getType(TCAS.TYPE_NAME_ANNOTATION_BASE));
    assertTrue(this.ts.getParent(this.ts.getType(CASTestSetup.TOKEN_TYPE)) == annot);
  }

  public void testGetType() {
    Type top = this.ts.getTopType();
    assertTrue(top != null);
    assertTrue(top.getName().equals(CAS.TYPE_NAME_TOP));
    Type annot = this.ts.getType(TCAS.TYPE_NAME_ANNOTATION);
    assertTrue(annot != null);
    Type token = this.ts.getType(CASTestSetup.TOKEN_TYPE);
    assertTrue(token != null);
  }

  /*
   * Test for Feature getFeature(String)
   */
  public void testGetFeature() {
    Type annot = this.ts.getType(TCAS.TYPE_NAME_ANNOTATION);
    Feature start = this.ts.getFeatureByFullName(TCAS.FEATURE_FULL_NAME_BEGIN);
    Feature end = this.ts.getFeatureByFullName(TCAS.FEATURE_FULL_NAME_END);
    Type integer = this.ts.getType(CAS.TYPE_NAME_INTEGER);
    assertTrue(start.getDomain() == annot);
    assertTrue(end.getDomain() == annot);
    assertTrue(start.getRange() == integer);
    assertTrue(end.getRange() == integer);
    Feature start1 = annot.getFeatureByBaseName(TCAS.FEATURE_BASE_NAME_BEGIN);
    assertTrue(start == start1);
    Feature start2 = this.ts.getType(CASTestSetup.TOKEN_TYPE).getFeatureByBaseName(
                    TCAS.FEATURE_BASE_NAME_BEGIN);
    assertTrue(start == start2);
  }

  public void testGetTypeIterator() {
    Iterator it = this.ts.getTypeIterator();
    // Put the type names in a vector and do some spot checks.
    ArrayList v = new ArrayList();
    while (it.hasNext()) {
      v.add(((Type) it.next()).getName());
    }
    assertTrue(v.contains(CAS.TYPE_NAME_TOP));
    assertTrue(v.contains(CAS.TYPE_NAME_FLOAT));
    assertTrue(v.contains(CAS.TYPE_NAME_FS_ARRAY));
    assertTrue(v.contains(TCAS.TYPE_NAME_ANNOTATION));
    assertTrue(v.contains(CASTestSetup.SENT_TYPE));
  }

  public void testGetFeatures() {
    Iterator it = this.ts.getFeatures();
    // Put feature names in vector and test for some known features.
    ArrayList v = new ArrayList();
    while (it.hasNext()) {
      v.add(((Feature) it.next()).getName());
    }
    String annotPrefix = TCAS.TYPE_NAME_ANNOTATION + TypeSystem.FEATURE_SEPARATOR;
    String arrayPrefix = CAS.TYPE_NAME_ARRAY_BASE + TypeSystem.FEATURE_SEPARATOR;
    assertTrue(arrayPrefix != null);
    String tokenPrefix = CASTestSetup.TOKEN_TYPE + TypeSystem.FEATURE_SEPARATOR;
    assertTrue(v.contains(annotPrefix + TCAS.FEATURE_BASE_NAME_BEGIN));
    assertTrue(v.contains(annotPrefix + TCAS.FEATURE_BASE_NAME_END));
    assertTrue(v.contains(tokenPrefix + CASTestSetup.TOKEN_TYPE_FEAT));
    // assertTrue(v.contains(arrayPrefix + CAS.ARRAY_LENGTH_FEAT));
  }

  public void testGetTopType() {
    Type top = this.ts.getTopType();
    assertTrue(top != null);
    assertTrue(top.getName().equals(CAS.TYPE_NAME_TOP));
  }

  public void testGetDirectlySubsumedTypes() {
    List subTypes = this.ts.getDirectSubtypes(this.ts.getType(CAS.TYPE_NAME_TOP));
    Type intType = this.ts.getType(CAS.TYPE_NAME_INTEGER);
    assertTrue(subTypes.contains(intType));
    Type annotBaseType = this.ts.getType(TCAS.TYPE_NAME_ANNOTATION_BASE);
    assertTrue(subTypes.contains(annotBaseType));
    Type annotType = this.ts.getType(TCAS.TYPE_NAME_ANNOTATION);
    assertTrue(!subTypes.contains(annotType));
    Type tokenType = this.ts.getType(CASTestSetup.TOKEN_TYPE);
    assertTrue(!subTypes.contains(tokenType));
  }

  /*
   * Test for boolean subsumes(Type, Type)
   */
  public void testSubsumes() {
    Type top = this.ts.getTopType();
    Type intType = this.ts.getType(CAS.TYPE_NAME_INTEGER);
    Type annotType = this.ts.getType(TCAS.TYPE_NAME_ANNOTATION);
    Type tokenType = this.ts.getType(CASTestSetup.TOKEN_TYPE);
    assertTrue(this.ts.subsumes(top, intType));
    assertTrue(this.ts.subsumes(top, annotType));
    assertTrue(this.ts.subsumes(top, tokenType));
    assertTrue(this.ts.subsumes(annotType, tokenType));
    assertTrue(!this.ts.subsumes(tokenType, annotType));
    assertTrue(!this.ts.subsumes(tokenType, top));
  }

  /**
   * Test presence of builtin types and their properties.
   */
  public void testBuiltinTypes() {
    assertTrue(this.cas.getTypeSystem().getType(CAS.TYPE_NAME_FLOAT).isInheritanceFinal());
    assertTrue(this.cas.getTypeSystem().getType(CAS.TYPE_NAME_FLOAT).isFeatureFinal());
    assertTrue(this.cas.getTypeSystem().getTopType().isFeatureFinal());
    assertFalse(this.cas.getTypeSystem().getTopType().isInheritanceFinal());
  }

  /**
   * Test creation of type system with static [T]CASFactory methods.
   */
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
  public void testArrayTypes() {
    // Our version of object arrays. Type is built-in and has special name,
    // for backwards compatibility.
    Type fsArrayType = this.ts.getType(CAS.TYPE_NAME_FS_ARRAY);
    assertNotNull(fsArrayType);
    assertTrue(fsArrayType.isArray());
    assertNotNull(fsArrayType.getComponentType());
    assertTrue(fsArrayType.getComponentType().equals(this.ts.getTopType()));
    assertTrue(fsArrayType.equals(this.ts.getArrayType(this.ts.getTopType())));
    // Int arrays are also built-in, but are primitive-valued.
    Type intArrayType = this.ts.getType(CAS.TYPE_NAME_INTEGER_ARRAY);
    assertNotNull(intArrayType);
    assertTrue(intArrayType.isArray());
    assertNotNull(intArrayType.getComponentType());
    assertTrue(intArrayType.getComponentType().equals(this.ts.getType(CAS.TYPE_NAME_INTEGER)));
    assertTrue(intArrayType.equals(this.ts.getArrayType(this.ts.getType(CAS.TYPE_NAME_INTEGER))));
    // Negative tests: make sure regular types are not classified as arrays.
    Type stringType = this.ts.getType(CAS.TYPE_NAME_STRING);
    assertFalse(stringType.isArray());
    assertNull(stringType.getComponentType());
    Type topType = this.ts.getTopType();
    assertFalse(topType.isArray());
    assertNull(topType.getComponentType());
    // Create an array of arrays.
    Type intMatrix = this.ts.getArrayType(intArrayType);
    assertNotNull(intMatrix);
    assertTrue(intMatrix.isArray());
    assertTrue(intMatrix.getComponentType().equals(intArrayType));
    // Check array inheritance.
    Type annotationArray = this.ts.getArrayType(this.ts.getType(TCAS.TYPE_NAME_ANNOTATION));
    assertTrue(this.ts.subsumes(fsArrayType, annotationArray));
    // assertFalse(this.ts.subsumes(annotationArray, fsArrayType));
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(TypeSystemTest.class);
  }

}
