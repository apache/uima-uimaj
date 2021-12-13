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
package org.apache.uima.cas.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.test.AnnotatorInitializer;
import org.apache.uima.cas.test.CASInitializer;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.Test;

// @formatter:off
/**
 * CasTypeSystemMapper maintains resources to map between two type systems, and handles
 *   types present in one but not the other,
 *   same-named types present in both but with different feature sets
 *   
 * The correspondence is by the name of the types and their features.
 * 
 * Same-named features must have the same range.
 * Types with no features - OK
 *    
 * Testing: make instances of type systems 
 *   - with / without missing types
 *   - with same-named types having different features
 *   - with same named but not == types and features
 *   
 *   Verify appropriate mapping is there.
 */
//@formatter:on
public class CasTypeSystemMapperTest {

  private static TypeSystemImpl tsi = (TypeSystemImpl) CASFactory.createTypeSystem(); // just to get
                                                                                      // the
                                                                                      // built-ins
  private static int t0 = tsi.getNumberOfTypes();
  private static int t1 = t0 + 1;
  private static int t2 = t0 + 2;

  private TypeSystemImpl ts1, ts2;

  private TypeImpl t1t, t2t, ts1t1, ts1t2, ts2t1, ts2t2;

  private CasTypeSystemMapper m;

  // to run this test, change the visibility in TypeSystemImpl of typeSystemMappers to public
  // public void testCasReuseWithDifferentTypeSystems() throws Exception {
  // // Create a CAS
  // CAS cas = CasCreationUtils.createCas((TypeSystemDescription) null, null, null);
  // cas.setDocumentLanguage("latin");
  // cas.setDocumentText("test");
  //
  // // Serialize it
  // ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
  // Serialization.serializeWithCompression(cas, baos, cas.getTypeSystem());
  //
  // // Create a new CAS
  // long min = Long.MAX_VALUE;
  // long max = 0;
  // CAS cas2 = CasCreationUtils.createCas((TypeSystemDescription) null, null, null);
  // for (int i = 0; i < 100000; i++) {
  // // Simulate us reinitializing the CAS with a new type system.
  // TypeSystemImpl tgt = new TypeSystemImpl();
  // for (int t = 0; t < 1000; t++) {
  // tgt.addType("random" + t, tgt.getTopType());
  // }
  // tgt.commit();
  //
  // // Deserialize into the new type system
  // ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
  // Serialization.deserializeCAS(cas2, bais, tgt, null);
  //
  // long cur = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
  // max = Math.max(cur, max);
  // min = Math.min(cur, min);
  // if (i % 100 == 0) {
  // System.out.printf("Cached: %d Max: %d Room left: %d %n",
  // ((TypeSystemImpl) cas2.getTypeSystem()).typeSystemMappers.size(), max,
  // Runtime.getRuntime().maxMemory()
  // - max);
  // }
  // }
  // }

  @Test
  public void testCasTypeSystemMapperFull() throws ResourceInitializationException {
    ts1 = createTs(3, 0x1ffff, 0x1ffff);
    ts2 = createTs(3, 0x1ffff, 0x1ffff); // become == type systems
    m = new CasTypeSystemMapper(ts1, ts2);
    chkbase(m, t2); // check all are equal
    assertTrue(m.isEqual());
  }

  @Test
  public void testMissingType1() throws ResourceInitializationException {
    ts1 = createTs(3, 0x1ffff, 0x1ffff);
    ts1t2 = t2t;
    ts2 = createTs(1, 0x1ffff, 0x1ffff); // missing t2t

    m = new CasTypeSystemMapper(ts1, ts2);
    chkbase(m, t1); // should be the same up thru t1
    assertEquals(null, m.mapTypeSrc2Tgt(ts1t2)); // ts1t2 is missing, so this should map to null
    assertFalse(m.isEqual());
  }

  @Test
  public void testMissingType2() throws ResourceInitializationException {
    ts1 = createTs(3, 0x1ffff, 0x1ffff);
    ts1t1 = t1t;
    ts1t2 = t2t;
    ts2 = createTs(2, 0x1ffff, 0x1ffff); // missing ts11

    m = new CasTypeSystemMapper(ts1, ts2);
    chkbase(m, t0);
    assertEquals(null, m.mapTypeSrc2Tgt(ts1t1));
    assertEquals(t1t, m.mapTypeSrc2Tgt(ts1t2));
    assertEquals(ts1t2, m.mapTypeCodeTgt2Src(t1));
    chkfeats(m, ts1t2, t1t);
    assertFalse(m.isEqual());
  }

  @Test
  public void testMissingType3() throws ResourceInitializationException {
    ts1 = createTs(1, 0x1ffff, 0x1ffff);
    ts2 = createTs(3, 0x1ffff, 0x1ffff);

    m = new CasTypeSystemMapper(ts1, ts2);
    chkbase(m, t1);
    assertEquals(null, m.mapTypeCodeTgt2Src(t2));
    assertFalse(m.isEqual());
  }

  @Test
  public void testMissingType4() throws ResourceInitializationException {
    ts1 = createTs(2, 0x1ffff, 0x1ffff);
    ts1t1 = t1t;
    ts2 = createTs(3, 0x1ffff, 0x1ffff);

    m = new CasTypeSystemMapper(ts1, ts2);
    chkbase(m, t0);
    assertEquals(t2t, m.mapTypeSrc2Tgt(ts1t1));
    assertEquals(ts1t1, m.mapTypeCodeTgt2Src(t2));
    assertEquals(null, m.mapTypeCodeTgt2Src(t1));
    chkfeats(m, ts1t1, t2t);
    assertFalse(m.isEqual());
  }

  @Test
  public void testMissingType5() throws ResourceInitializationException {
    ts1 = createTs(3, 0x1ffff, 0x1ffff);
    TypeImpl ts1t1 = t1t;
    TypeImpl ts1t2 = t2t;
    ts2 = createTs(0, 0x1ffff, 0x1ffff);

    CasTypeSystemMapper m = new CasTypeSystemMapper(ts1, ts2);
    chkbase(m, t0);
    assertEquals(null, m.mapTypeSrc2Tgt(ts1t1));
    assertEquals(null, m.mapTypeSrc2Tgt(ts1t2));
    assertFalse(m.isEqual());
  }

  @Test
  public void testMissingType6() throws ResourceInitializationException {
    ts1 = createTs(0, 0x1ffff, 0x1ffff);
    ts1t1 = t1t;
    ts1t2 = t2t;
    ts2 = createTs(3, 0x1ffff, 0x1ffff);

    CasTypeSystemMapper m = new CasTypeSystemMapper(ts1, ts2);
    chkbase(m, t0);
    assertEquals(null, m.mapTypeCodeTgt2Src(t1));
    assertEquals(null, m.mapTypeCodeTgt2Src(t2));
    assertFalse(m.isEqual());
  }

  @Test
  public void testMissingFeature0() throws ResourceInitializationException {
    ts1 = createTs(3, 0x1ffff, 0x1ffff);
    ts1t1 = t1t;
    ts1t2 = t2t;

    for (int i = 0, mf = 1; i < 14; i++, mf = mf << 1) {
      ts2 = createTs(3, 0x1ffff - mf, 0x1ffff);
      m = new CasTypeSystemMapper(ts1, ts2);
      chkbase(m);
      assertEquals(t1t, m.mapTypeSrc2Tgt(ts1t1));
      assertEquals(t2t, m.mapTypeSrc2Tgt(ts1t2));
      assertEquals(ts1t1, m.mapTypeCodeTgt2Src(t1));
      assertEquals(ts1t2, m.mapTypeCodeTgt2Src(t2));
      chkfeats(m, ts1t2, t2t);
      chkMissingFeats1(m, t1, mf);
      assertFalse(m.isEqual());
    }
  }

  @Test
  public void testMissingFeature0r() throws ResourceInitializationException {
    ts1 = createTs(3, 0x1ffff, 0x1ffff);
    ts1t1 = t1t;
    ts1t2 = t2t;

    for (int i = 0, mf = 1; i < 14; i++, mf = mf << 1) {
      ts2 = createTs(3, 0x1ffff, 0x1ffff - mf);
      m = new CasTypeSystemMapper(ts1, ts2);
      chkbase(m);
      assertEquals(t1t, m.mapTypeSrc2Tgt(ts1t1));
      assertEquals(t2t, m.mapTypeSrc2Tgt(ts1t2));
      assertEquals(ts1t1, m.mapTypeCodeTgt2Src(t1));
      assertEquals(ts1t2, m.mapTypeCodeTgt2Src(t2));
      chkfeats(m, ts1t1, t1t);
      chkMissingFeats1(m, t2, mf);
      assertFalse(m.isEqual());
    }
  }

  @Test
  public void testMissingFeature0f() throws ResourceInitializationException {
    ts2 = createTs(3, 0x1ffff, 0x1ffff);
    ts2t1 = t1t;
    ts2t2 = t2t;

    for (int i = 0, mf = 1; i < 14; i++, mf = mf << 1) {
      ts1 = createTs(3, 0x1ffff - mf, 0x1ffff); // feat 8
      m = new CasTypeSystemMapper(ts1, ts2);
      chkbase(m);
      assertEquals(ts2t1, m.mapTypeSrc2Tgt(t1t));
      assertEquals(ts2t2, m.mapTypeSrc2Tgt(t2t));
      assertEquals(t1t, m.mapTypeTgt2Src(ts2t1));
      assertEquals(t2t, m.mapTypeTgt2Src(ts2t2));
      chkfeats(m, t2t, ts2t2);
      chkMissingFeats2(m, t1, mf);
      assertFalse(m.isEqual());
    }
  }

  @Test
  public void testMissingFeature0f2() throws ResourceInitializationException {
    ts2 = createTs(3, 0x1ffff, 0x1ffff);
    ts2t1 = t1t;
    ts2t2 = t2t;

    for (int i = 0, mf = 1; i < 14; i++, mf = mf << 1) {
      ts1 = createTs(3, 0x1ffff, 0x1ffff - mf); // feat 8
      m = new CasTypeSystemMapper(ts1, ts2);
      chkbase(m);
      assertEquals(ts2t1, m.mapTypeSrc2Tgt(t1t));
      assertEquals(ts2t2, m.mapTypeSrc2Tgt(t2t));
      assertEquals(ts2t1, m.mapTypeCodeTgt2Src(t1));
      assertFalse(ts2t2.equals(m.mapTypeCodeTgt2Src(t2)));
      chkfeats(m, t1t, ts2t1);
      chkMissingFeats2(m, t2, mf);
      assertFalse(m.isEqual());
    }
  }

  @Test
  public void testMissingAllFeat1() throws ResourceInitializationException {
    int mf = 0x1ffff;
    ts1 = createTs(3, 0x1ffff, 0x1ffff);
    ts1t1 = t1t;
    ts1t2 = t2t;
    ts2 = createTs(3, 0x1ffff, 0x1ffff - mf);

    m = new CasTypeSystemMapper(ts1, ts2);
    chkbase(m);
    assertEquals(t1t, m.mapTypeSrc2Tgt(ts1t1));
    assertEquals(t2t, m.mapTypeSrc2Tgt(ts1t2));
    assertEquals(t1t, m.mapTypeCodeTgt2Src(t1));
    assertFalse(t2t.equals(m.mapTypeCodeTgt2Src(t2)));
    chkfeats(m, ts1t1, t1t);
    chkMissingFeats1(m, t2, mf);
    assertFalse(m.isEqual());
  }

  @Test
  public void testMissingAllFeat2() throws ResourceInitializationException {
    int mf = 0x1ffff;
    ts1 = createTs(3, 0x1ffff, 0x1ffff - mf);
    ts1t1 = t1t;
    ts1t2 = t2t;
    ts2 = createTs(3, 0x1ffff, 0x1ffff);

    m = new CasTypeSystemMapper(ts1, ts2);
    chkbase(m);
    assertEquals(t1t, m.mapTypeSrc2Tgt(ts1t1));
    assertEquals(t2t, m.mapTypeSrc2Tgt(ts1t2));
    assertEquals(t1t, m.mapTypeCodeTgt2Src(t1));
    assertFalse(t2t.equals(m.mapTypeCodeTgt2Src(t2)));
    chkfeats(m, ts1t1, t1t);
    chkMissingFeats2(m, t2, mf);
    assertFalse(m.isEqual());
  }

  /**
   * Check that all source features are in the target type according to the mapper
   * 
   * @param m
   * @param tCode
   */
  private void chkfeats(CasTypeSystemMapper m, TypeImpl srcType, TypeImpl tgtType) {
    Set<FeatureImpl> srcFeats = new HashSet<>(Arrays.asList(srcType.getFeatureImpls()));
    Set<FeatureImpl> tgtFeats = new HashSet<>(Arrays.asList(tgtType.getFeatureImpls()));
    assertTrue(srcFeats.equals(tgtFeats));
  }

  /**
   * given bitset of missing feats in src for a particular type, verify the map from target feat to
   * source is either null or the right feat
   * 
   * @param m
   *          -
   * @param tCode
   *          -
   * @param mFeats
   *          features that are missing
   */

  private void chkMissingFeats2(CasTypeSystemMapper m, int tCode, int mFeats) {
    TypeImpl srcType = m.tsSrc.getTypeForCode(tCode);
    TypeImpl tgtType = m.tsTgt.get().getTypeForCode(tCode);

    FeatureImpl[] tgtFeats = tgtType.getFeatureImpls();

    for (int j = 0, mf = 1; j < tgtFeats.length; j++, mf = mf << 1) {
      FeatureImpl tgtFeat = tgtFeats[j];
      if ((mFeats & mf) == mf) { // if the feature is supposed to be missing
        assertEquals(null, m.getSrcFeature(tgtType, tgtFeat));
        continue;
      }
      assertEquals(srcType.getFeatureByBaseName(tgtFeat.getShortName()),
              m.getSrcFeature(tgtType, tgtFeat));
    }
  }

  /**
   * given bitset of missing feats in tgt for a particular type, verify the map from target feat to
   * source is ok and the map entries from source to tgt are either null (if missing in tgt) or ok
   * 
   * @param m
   *          -
   * @param tCode
   *          -
   * @param mFeats
   *          features that are missing
   */
  private void chkMissingFeats1(CasTypeSystemMapper m, int tCode, int mFeats) {
    TypeImpl srcType = m.tsSrc.getTypeForCode(tCode);
    TypeImpl tgtType = m.tsTgt.get().getTypeForCode(tCode);

    FeatureImpl[] srcFeats = srcType.getFeatureImpls();

    for (int j = 0, mf = 1; j < srcFeats.length; j++, mf = mf << 1) {
      FeatureImpl srcFeat = srcFeats[j];
      if ((mFeats & mf) == mf) { // if the feature is supposed to be missing
        assertEquals(null, m.getTgtFeature(srcType, srcFeat));
        continue;
      }
      assertEquals(tgtType.getFeatureByBaseName(srcFeat.getShortName()),
              m.getTgtFeature(srcType, srcFeat));
    }
  }

  private void chkbase(CasTypeSystemMapper m) {
    chkbase(m, 36);
  }

  private void chkbase(CasTypeSystemMapper m, final int last) {
    for (int i = 1; i <= last; i++) {
      TypeImpl typeSrc = m.tsSrc.types.get(i);
      TypeImpl typeTgt = m.tsTgt.get().types.get(i);
      assertEquals(m.mapTypeCodeTgt2Src(i), typeSrc);
      assertEquals(m.mapTypeSrc2Tgt(m.tsSrc.types.get(i)), typeTgt);
      assertEquals(m.mapTypeTgt2Src(typeTgt), typeSrc);

      chkfeats(m, typeSrc, typeTgt);
    }
  }

  /**
   * Create a type system having all kinds of types
   */
  private TypeSystemImpl createTs(int types, int feats1, int feats2) {
    CASTestSetup cts = new CASTestSetup(types, feats1, feats2);
    return (TypeSystemImpl) CASInitializer.initCas(cts, tsi -> cts.reinitTs(tsi)).getTypeSystem();
  }

  private Type akof;
  private Type akof2;

  private Type topType;
  private Type typeArrayInt;
  private Type typeArrayFs;
  private Type typeArrayFloat;
  private Type typeArrayDouble;
  private Type typeArrayLong;
  private Type typeArrayShort;
  private Type typeArrayByte;
  private Type typeArrayBoolean;
  private Type typeArrayString;

  private Type typeInt;
  private Type typeFloat;
  private Type typeDouble;
  private Type typeLong;
  private Type typeShort;
  private Type typeByte;
  private Type typeBoolean;
  private Type typeString;
  private Type typeFs;

  private Feature akofInt;
  private Feature akofFloat;
  private Feature akofDouble;
  private Feature akofLong;
  private Feature akofShort;
  private Feature akofByte;
  private Feature akofBoolean;
  private Feature akofString;
  private Feature akofFs;

  private Feature akofAint;
  private Feature akofAfloat;
  private Feature akofAdouble;
  private Feature akofAlong;
  private Feature akofAshort;
  private Feature akofAbyte;
  private Feature akofAboolean;
  private Feature akofAstring;
  private Feature akofAfs;

  private Feature akof2Int;
  private Feature akof2Float;
  private Feature akof2Double;
  private Feature akof2Long;
  private Feature akof2Short;
  private Feature akof2Byte;
  private Feature akof2Boolean;
  private Feature akof2String;
  private Feature akof2Fs;

  private Feature akof2Aint;
  private Feature akof2Afloat;
  private Feature akof2Adouble;
  private Feature akof2Along;
  private Feature akof2Ashort;
  private Feature akof2Abyte;
  private Feature akof2Aboolean;
  private Feature akof2Astring;
  private Feature akof2Afs;

  public class CASTestSetup implements AnnotatorInitializer {
    final int typesToGenerate;
    final boolean[] ttgb;
    final int featsToInclude1;
    final int featsToInclude2;
    final BitSet ftg1 = new BitSet(32);
    final BitSet ftg2 = new BitSet(32);

    /**
     * 
     * @param types
     *          treated as a bit set - 3 generates both types
     * @param feats1
     *          treated as a bit set, bits converted to ftg1 and 2 which are bit sets
     * @param feats2
     */
    CASTestSetup(int types, int feats1, int feats2) {
      this.typesToGenerate = types;
      this.featsToInclude1 = feats1;
      this.featsToInclude2 = feats2;
      ttgb = new boolean[2];
      ttgb[0] = (1 == (typesToGenerate & 1));
      ttgb[1] = (2 == (typesToGenerate & 2));
      for (int i = 0; i < 32; i++) {
        int v = 1 << i;
        ftg1.set(i, v == (featsToInclude1 & v));
        ftg2.set(i, v == (featsToInclude2 & v));
      }
    }

 // @formatter:off
    /** 
     * Type system  called to initialize the type system.
     *
     * akof    - type: all kinds of features
     *   akofInt  
     *   akofFloat
     *   akofByte
     *   akofBoolean
     *   akofShort
     *   akofStr
     *   akofLong
     *   akofDouble
     *   akofHeapRef
     *   akofArrayRef 
     */
 // @formatter:on
    @Override
    public void initTypeSystem(TypeSystemMgr tsm) {
      initBuiltInTypes(tsm);

      // Add new types and features.

      if (ttgb[0]) {
        akof = tsm.addType("akof", topType);
        if (ftg1.get(0)) {
          akofInt = tsm.addFeature("akofInt", akof, typeInt);
        }
        if (ftg1.get(1)) {
          akofFs = tsm.addFeature("akofFs", akof, typeFs);
        }
        if (ftg1.get(2)) {
          akofFloat = tsm.addFeature("akofFloat", akof, typeFloat);
        }
        if (ftg1.get(3)) {
          akofDouble = tsm.addFeature("akofDouble", akof, typeDouble);
        }
        if (ftg1.get(4)) {
          akofLong = tsm.addFeature("akofLong", akof, typeLong);
        }
        if (ftg1.get(5)) {
          akofShort = tsm.addFeature("akofShort", akof, typeShort);
        }
        if (ftg1.get(6)) {
          akofByte = tsm.addFeature("akofByte", akof, typeByte);
        }
        if (ftg1.get(7)) {
          akofBoolean = tsm.addFeature("akofBoolean", akof, typeBoolean);
        }
        if (ftg1.get(8)) {
          akofString = tsm.addFeature("akofStr", akof, typeString);
        }

        if (ftg1.get(9)) {
          akofAint = tsm.addFeature("akofAint", akof, typeArrayInt);
        }
        if (ftg1.get(10)) {
          akofAfs = tsm.addFeature("akofAfs", akof, typeArrayFs);
        }
        if (ftg1.get(11)) {
          akofAfloat = tsm.addFeature("akofAfloat", akof, typeArrayFloat);
        }
        if (ftg1.get(12)) {
          akofAdouble = tsm.addFeature("akofAdouble", akof, typeArrayDouble);
        }
        if (ftg1.get(13)) {
          akofAlong = tsm.addFeature("akofAlong", akof, typeArrayLong);
        }
        if (ftg1.get(14)) {
          akofAshort = tsm.addFeature("akofAshort", akof, typeArrayShort);
        }
        if (ftg1.get(15)) {
          akofAbyte = tsm.addFeature("akofAbyte", akof, typeArrayByte);
        }
        if (ftg1.get(16)) {
          akofAboolean = tsm.addFeature("akofAboolean", akof, typeArrayBoolean);
        }
        if (ftg1.get(17)) {
          akofAstring = tsm.addFeature("akofAstring", akof, typeArrayString);
        }
      }
      if (ttgb[1]) {
        akof2 = tsm.addType("akof2", topType);
        if (ftg2.get(0)) {
          akof2Int = tsm.addFeature("akof2Int", akof2, typeInt);
        }
        if (ftg2.get(1)) {
          akof2Fs = tsm.addFeature("akof2Fs", akof2, typeFs);
        }
        if (ftg2.get(2)) {
          akof2Float = tsm.addFeature("akof2Float", akof2, typeFloat);
        }
        if (ftg2.get(3)) {
          akof2Double = tsm.addFeature("akof2Double", akof2, typeDouble);
        }
        if (ftg2.get(4)) {
          akof2Long = tsm.addFeature("akof2Long", akof2, typeLong);
        }
        if (ftg2.get(5)) {
          akof2Short = tsm.addFeature("akof2Short", akof2, typeShort);
        }
        if (ftg2.get(6)) {
          akof2Byte = tsm.addFeature("akof2Byte", akof2, typeByte);
        }
        if (ftg2.get(7)) {
          akof2Boolean = tsm.addFeature("akof2Boolean", akof2, typeBoolean);
        }
        if (ftg2.get(8)) {
          akof2String = tsm.addFeature("akof2Str", akof2, typeString);
        }

        if (ftg2.get(9)) {
          akof2Aint = tsm.addFeature("akof2Aint", akof2, typeArrayInt);
        }
        if (ftg2.get(10)) {
          akof2Afs = tsm.addFeature("akof2Afs", akof2, typeArrayFs);
        }
        if (ftg2.get(11)) {
          akof2Afloat = tsm.addFeature("akof2Afloat", akof2, typeArrayFloat);
        }
        if (ftg2.get(12)) {
          akof2Adouble = tsm.addFeature("akof2Adouble", akof2, typeArrayDouble);
        }
        if (ftg2.get(13)) {
          akof2Along = tsm.addFeature("akof2Along", akof2, typeArrayLong);
        }
        if (ftg2.get(14)) {
          akof2Ashort = tsm.addFeature("akof2Ashort", akof2, typeArrayShort);
        }
        if (ftg2.get(15)) {
          akof2Abyte = tsm.addFeature("akof2Abyte", akof2, typeArrayByte);
        }
        if (ftg2.get(16)) {
          akof2Aboolean = tsm.addFeature("akof2Aboolean", akof2, typeArrayBoolean);
        }
        if (ftg2.get(17)) {
          akof2Astring = tsm.addFeature("akof2Astring", akof2, typeArrayString);
        }
      }
      if (ttgb[0]) {
        t1t = (TypeImpl) akof;
        t2t = ttgb[1] ? (TypeImpl) akof2 : null;
      } else {
        t1t = ttgb[1] ? (TypeImpl) akof2 : null;
        t2t = null;
      }
    }

    private void reinitTs(TypeSystemImpl tsm) {
      initBuiltInTypes(tsm);

      if (ttgb[0]) {
        akof = tsm.getType("akof");
        if (ftg1.get(0)) {
          akofInt = akof.getFeatureByBaseName("akofInt");
        }
        if (ftg1.get(1)) {
          akofFs = akof.getFeatureByBaseName("akofFs");
        }
        if (ftg1.get(2)) {
          akofFloat = akof.getFeatureByBaseName("akofFloat");
        }
        if (ftg1.get(3)) {
          akofDouble = akof.getFeatureByBaseName("akofDouble");
        }
        if (ftg1.get(4)) {
          akofLong = akof.getFeatureByBaseName("akofLong");
        }
        if (ftg1.get(5)) {
          akofShort = akof.getFeatureByBaseName("akofShort");
        }
        if (ftg1.get(6)) {
          akofByte = akof.getFeatureByBaseName("akofByte");
        }
        if (ftg1.get(7)) {
          akofBoolean = akof.getFeatureByBaseName("akofBoolean");
        }
        if (ftg1.get(8)) {
          akofString = akof.getFeatureByBaseName("akofStr");
        }

        if (ftg1.get(9)) {
          akofAint = akof.getFeatureByBaseName("akofAint");
        }
        if (ftg1.get(10)) {
          akofAfs = akof.getFeatureByBaseName("akofAfs");
        }
        if (ftg1.get(11)) {
          akofAfloat = akof.getFeatureByBaseName("akofAfloat");
        }
        if (ftg1.get(12)) {
          akofAdouble = akof.getFeatureByBaseName("akofAdouble");
        }
        if (ftg1.get(13)) {
          akofAlong = akof.getFeatureByBaseName("akofAlong");
        }
        if (ftg1.get(14)) {
          akofAshort = akof.getFeatureByBaseName("akofAshort");
        }
        if (ftg1.get(15)) {
          akofAbyte = akof.getFeatureByBaseName("akofAbyte");
        }
        if (ftg1.get(16)) {
          akofAboolean = akof.getFeatureByBaseName("akofAboolean");
        }
        if (ftg1.get(17)) {
          akofAstring = akof.getFeatureByBaseName("akofAstring");
        }
      }
      if (ttgb[1]) {
        akof2 = tsm.getType("akof2");
        if (ftg2.get(0)) {
          akof2Int = akof2.getFeatureByBaseName("akof2Int");
        }
        if (ftg2.get(1)) {
          akof2Fs = akof2.getFeatureByBaseName("akof2Fs");
        }
        if (ftg2.get(2)) {
          akof2Float = akof2.getFeatureByBaseName("akof2Float");
        }
        if (ftg2.get(3)) {
          akof2Double = akof2.getFeatureByBaseName("akof2Double");
        }
        if (ftg2.get(4)) {
          akof2Long = akof2.getFeatureByBaseName("akof2Long");
        }
        if (ftg2.get(5)) {
          akof2Short = akof2.getFeatureByBaseName("akof2Short");
        }
        if (ftg2.get(6)) {
          akof2Byte = akof2.getFeatureByBaseName("akof2Byte");
        }
        if (ftg2.get(7)) {
          akof2Boolean = akof2.getFeatureByBaseName("akof2Boolean");
        }
        if (ftg2.get(8)) {
          akof2String = akof2.getFeatureByBaseName("akof2Str");
        }

        if (ftg2.get(9)) {
          akof2Aint = akof2.getFeatureByBaseName("akof2Aint");
        }
        if (ftg2.get(10)) {
          akof2Afs = akof2.getFeatureByBaseName("akof2Afs");
        }
        if (ftg2.get(11)) {
          akof2Afloat = akof2.getFeatureByBaseName("akof2Afloat");
        }
        if (ftg2.get(12)) {
          akof2Adouble = akof2.getFeatureByBaseName("akof2Adouble");
        }
        if (ftg2.get(13)) {
          akof2Along = akof2.getFeatureByBaseName("akof2Along");
        }
        if (ftg2.get(14)) {
          akof2Ashort = akof2.getFeatureByBaseName("akof2Ashort");
        }
        if (ftg2.get(15)) {
          akof2Abyte = akof2.getFeatureByBaseName("akof2Abyte");
        }
        if (ftg2.get(16)) {
          akof2Aboolean = akof2.getFeatureByBaseName("akof2Aboolean");
        }
        if (ftg2.get(17)) {
          akof2Astring = akof2.getFeatureByBaseName("akof2Astring");
        }
      }
    }

    private void initBuiltInTypes(TypeSystemMgr tsm) {
      topType = tsm.getTopType();
      typeArrayInt = tsm.getType(CAS.TYPE_NAME_INTEGER_ARRAY);
      typeArrayFs = tsm.getType(CAS.TYPE_NAME_FS_ARRAY);
      typeArrayFloat = tsm.getType(CAS.TYPE_NAME_FLOAT_ARRAY);
      typeArrayDouble = tsm.getType(CAS.TYPE_NAME_DOUBLE_ARRAY);
      typeArrayLong = tsm.getType(CAS.TYPE_NAME_LONG_ARRAY);
      typeArrayShort = tsm.getType(CAS.TYPE_NAME_SHORT_ARRAY);
      typeArrayByte = tsm.getType(CAS.TYPE_NAME_BYTE_ARRAY);
      typeArrayBoolean = tsm.getType(CAS.TYPE_NAME_BOOLEAN_ARRAY);
      typeArrayString = tsm.getType(CAS.TYPE_NAME_STRING_ARRAY);

      typeInt = tsm.getType(CAS.TYPE_NAME_INTEGER);
      typeFloat = tsm.getType(CAS.TYPE_NAME_FLOAT);
      typeDouble = tsm.getType(CAS.TYPE_NAME_DOUBLE);
      typeLong = tsm.getType(CAS.TYPE_NAME_LONG);
      typeShort = tsm.getType(CAS.TYPE_NAME_SHORT);
      typeByte = tsm.getType(CAS.TYPE_NAME_BYTE);
      typeBoolean = tsm.getType(CAS.TYPE_NAME_BOOLEAN);
      typeString = tsm.getType(CAS.TYPE_NAME_STRING);
      typeFs = tsm.getType(CAS.TYPE_NAME_TOP);
    }

    @Override
    public void initIndexes(FSIndexRepositoryMgr irm, TypeSystem ts) {
    }

  }

}
