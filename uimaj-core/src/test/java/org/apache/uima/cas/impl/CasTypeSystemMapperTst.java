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

import java.util.BitSet;

import junit.framework.TestCase;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.test.AnnotatorInitializer;
import org.apache.uima.cas.test.CASInitializer;
import org.apache.uima.resource.ResourceInitializationException;

public class CasTypeSystemMapperTst extends TestCase {
  
  protected void setUp() throws Exception {
    super.setUp();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }
  
   // to run this test, change the visibility in TypeSystemImpl of typeSystemMappers to public
//  public void testCasReuseWithDifferentTypeSystems() throws Exception {
//    // Create a CAS
//    CAS cas = CasCreationUtils.createCas((TypeSystemDescription) null, null, null);
//    cas.setDocumentLanguage("latin");
//    cas.setDocumentText("test");
//
//    // Serialize it
//    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
//    Serialization.serializeWithCompression(cas, baos, cas.getTypeSystem());
//
//    // Create a new CAS
//    long min = Long.MAX_VALUE;
//    long max = 0;
//    CAS cas2 = CasCreationUtils.createCas((TypeSystemDescription) null, null, null);
//    for (int i = 0; i < 100000; i++) {
//      // Simulate us reinitializing the CAS with a new type system.
//      TypeSystemImpl tgt = new TypeSystemImpl();
//      for (int t = 0; t < 1000; t++) {
//        tgt.addType("random" + t, tgt.getTopType());
//      }
//      tgt.commit();
//
//      // Deserialize into the new type system
//      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
//      Serialization.deserializeCAS(cas2, bais, tgt, null);
//
//      long cur = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
//      max = Math.max(cur, max);
//      min = Math.min(cur, min);
//      if (i % 100 == 0) {
//        System.out.printf("Cached: %d   Max: %d   Room left: %d   %n",
//            ((TypeSystemImpl) cas2.getTypeSystem()).typeSystemMappers.size(), max, Runtime.getRuntime().maxMemory()
//                - max);
//      }
//    }
//  }

  public void testCasTypeSystemMapperFull() throws ResourceInitializationException {
    TypeSystemImpl ts1 = createTs(3, 0x1ffff, 0x1ffff);
    TypeSystemImpl ts2 = createTs(3, 0x1ffff, 0x1ffff); 
    CasTypeSystemMapper m = new CasTypeSystemMapper(ts1, ts2);
    chkbase(m, 38);
    assertTrue(m.isEqual());
  }
  
  public void testMissingType1() throws ResourceInitializationException {
    TypeSystemImpl ts1 = createTs(3, 0x1ffff, 0x1ffff);
    TypeSystemImpl ts2 = createTs(1, 0x1ffff, 0x1ffff); 
    CasTypeSystemMapper m = new CasTypeSystemMapper(ts1, ts2);
    chkbase(m, 37);
    assertEquals(0, m.mapTypeCodeSrc2Tgt(38));
    assertFalse(m.isEqual());
  }

  public void testMissingType2() throws ResourceInitializationException {
    TypeSystemImpl ts1 = createTs(3, 0x1ffff, 0x1ffff);
    TypeSystemImpl ts2 = createTs(2, 0x1ffff, 0x1ffff); 
    CasTypeSystemMapper m = new CasTypeSystemMapper(ts1, ts2);
    chkbase(m, 36);
    assertEquals( 0, m.mapTypeCodeSrc2Tgt(37));
    assertEquals(37, m.mapTypeCodeSrc2Tgt(38));
    assertEquals(38, m.mapTypeCodeTgt2Src(37));
    chkfeats(m, 38);
    assertFalse(m.isEqual());
  }
  
  public void testMissingType3() throws ResourceInitializationException {
    TypeSystemImpl ts1 = createTs(1, 0x1ffff, 0x1ffff);
    TypeSystemImpl ts2 = createTs(3, 0x1ffff, 0x1ffff); 
    CasTypeSystemMapper m = new CasTypeSystemMapper(ts1, ts2);
    chkbase(m, 37);
    assertEquals(0, m.mapTypeCodeTgt2Src(38));
    assertFalse(m.isEqual());
  }
  
  public void testMissingType4() throws ResourceInitializationException {
    TypeSystemImpl ts1 = createTs(2, 0x1ffff, 0x1ffff);
    TypeSystemImpl ts2 = createTs(3, 0x1ffff, 0x1ffff); 
    CasTypeSystemMapper m = new CasTypeSystemMapper(ts1, ts2);
    chkbase(m, 36);
    assertEquals(38, m.mapTypeCodeSrc2Tgt(37));
    assertEquals(37, m.mapTypeCodeTgt2Src(38));
    assertEquals(0, m.mapTypeCodeTgt2Src(37));
    chkfeats(m, 37);
    assertFalse(m.isEqual());
  }
  
  public void testMissingType5() throws ResourceInitializationException {
    TypeSystemImpl ts1 = createTs(3, 0x1ffff, 0x1ffff);
    TypeSystemImpl ts2 = createTs(0, 0x1ffff, 0x1ffff); 
    CasTypeSystemMapper m = new CasTypeSystemMapper(ts1, ts2);
    chkbase(m, 36);
    assertEquals( 0, m.mapTypeCodeSrc2Tgt(37));
    assertEquals(0, m.mapTypeCodeSrc2Tgt(38));
    assertFalse(m.isEqual());
  }

  public void testMissingType6() throws ResourceInitializationException {
    TypeSystemImpl ts1 = createTs(0, 0x1ffff, 0x1ffff);
    TypeSystemImpl ts2 = createTs(3, 0x1ffff, 0x1ffff); 
    CasTypeSystemMapper m = new CasTypeSystemMapper(ts1, ts2);
    chkbase(m, 36);
    assertEquals( 0, m.mapTypeCodeTgt2Src(37));
    assertEquals(0, m.mapTypeCodeTgt2Src(38));
    assertFalse(m.isEqual());
  }

  
  public void testMissingFeature0() throws ResourceInitializationException {
    TypeSystemImpl ts1 = createTs(3, 0x1ffff, 0x1ffff);
    for (int i = 0, mf = 1; i < 14; i++, mf = mf<<1) {
      TypeSystemImpl ts2 = createTs(3, 0x1ffff - mf, 0x1ffff);  
      CasTypeSystemMapper m = new CasTypeSystemMapper(ts1, ts2);
      chkbase(m);
      assertEquals(37, m.mapTypeCodeSrc2Tgt(37));
      assertEquals(38, m.mapTypeCodeSrc2Tgt(38));
      assertEquals(37, m.mapTypeCodeTgt2Src(37));
      assertEquals(38, m.mapTypeCodeTgt2Src(38));
      chkfeats(m, 38);
      chkMissingFeats1(m, 37, mf);    
      assertFalse(m.isEqual());
   } 
  }

  public void testMissingFeature0r() throws ResourceInitializationException {
    TypeSystemImpl ts1 = createTs(3, 0x1ffff, 0x1ffff);
    for (int i = 0, mf = 1; i < 14; i++, mf = mf<<1) {
      TypeSystemImpl ts2 = createTs(3, 0x1ffff, 0x1ffff - mf);  
      CasTypeSystemMapper m = new CasTypeSystemMapper(ts1, ts2);
      chkbase(m);
      assertEquals(37, m.mapTypeCodeSrc2Tgt(37));
      assertEquals(38, m.mapTypeCodeSrc2Tgt(38));
      assertEquals(37, m.mapTypeCodeTgt2Src(37));
      assertEquals(38, m.mapTypeCodeTgt2Src(38));
      chkfeats(m, 37);
      chkMissingFeats1(m, 38, mf);      
      assertFalse(m.isEqual());
    }
  }

  public void testMissingFeature0f() throws ResourceInitializationException {
    TypeSystemImpl ts2 = createTs(3, 0x1ffff, 0x1ffff);  
    for (int i = 0, mf = 1; i < 14; i++, mf = mf<<1) {
      TypeSystemImpl ts1 = createTs(3, 0x1ffff - mf, 0x1ffff); // feat 8
      CasTypeSystemMapper m = new CasTypeSystemMapper(ts1, ts2);
      chkbase(m);
      assertEquals(37, m.mapTypeCodeSrc2Tgt(37));
      assertEquals(38, m.mapTypeCodeSrc2Tgt(38));
      assertEquals(37, m.mapTypeCodeTgt2Src(37));
      assertEquals(38, m.mapTypeCodeTgt2Src(38));
      chkfeats(m, 38);
      chkMissingFeats2(m, 37, mf);
      assertFalse(m.isEqual());
    }
  }

  public void testMissingFeature0f2() throws ResourceInitializationException {
    TypeSystemImpl ts2 = createTs(3, 0x1ffff, 0x1ffff);  
    for (int i = 0, mf = 1; i < 14; i++, mf = mf<<1) {
      TypeSystemImpl ts1 = createTs(3, 0x1ffff, 0x1ffff - mf); // feat 8
      CasTypeSystemMapper m = new CasTypeSystemMapper(ts1, ts2);
      chkbase(m);
      assertEquals(37, m.mapTypeCodeSrc2Tgt(37));
      assertEquals(38, m.mapTypeCodeSrc2Tgt(38));
      assertEquals(37, m.mapTypeCodeTgt2Src(37));
      assertEquals(38, m.mapTypeCodeTgt2Src(38));
      chkfeats(m, 37);
      chkMissingFeats2(m, 38, mf);
      assertFalse(m.isEqual());
    }
  }
  
  public void testMissingAllFeat1() throws ResourceInitializationException {
    int mf = 0x1ffff;
    TypeSystemImpl ts1 = createTs(3, 0x1ffff, 0x1ffff);
    TypeSystemImpl ts2 = createTs(3, 0x1ffff, 0x1ffff - mf);  
    CasTypeSystemMapper m = new CasTypeSystemMapper(ts1, ts2);
    chkbase(m);
    assertEquals(37, m.mapTypeCodeSrc2Tgt(37));
    assertEquals(38, m.mapTypeCodeSrc2Tgt(38));
    assertEquals(37, m.mapTypeCodeTgt2Src(37));
    assertEquals(38, m.mapTypeCodeTgt2Src(38));
    chkfeats(m, 37);
    chkMissingFeats1(m, 38, mf);      
    assertFalse(m.isEqual());
  }

  public void testMissingAllFeat2() throws ResourceInitializationException {
    int mf = 0x1ffff;
    TypeSystemImpl ts1 = createTs(3, 0x1ffff, 0x1ffff - mf);
    TypeSystemImpl ts2 = createTs(3, 0x1ffff, 0x1ffff);  
    CasTypeSystemMapper m = new CasTypeSystemMapper(ts1, ts2);
    chkbase(m);
    assertEquals(37, m.mapTypeCodeSrc2Tgt(37));
    assertEquals(38, m.mapTypeCodeSrc2Tgt(38));
    assertEquals(37, m.mapTypeCodeTgt2Src(37));
    assertEquals(38, m.mapTypeCodeTgt2Src(38));
    chkfeats(m, 37);
    chkMissingFeats2(m, 38, mf);      
    assertFalse(m.isEqual());
  }

  private void chkfeats(CasTypeSystemMapper m, int tCode) {
    if (tCode > 0) {
      final int[] tgtFeatOffsets = m.getTgtFeatOffsets2Src(tCode);
      for (int j = 0; j < tgtFeatOffsets.length; j++) {
        assertEquals(j, tgtFeatOffsets[j]);
      }
      final boolean[] featSrcInTg = m.getFSrcInTgt(tCode);
      for (int j = 0; j < featSrcInTg.length; j++) {
        assertTrue(featSrcInTg[j]);
      }  
    } 
  }

  private void chkMissingFeats2(CasTypeSystemMapper m, int tCode, int mFeats) {
    final int[] tgtFeatOffsets = m.getTgtFeatOffsets2Src(tCode);
    for (int j = 0, k = 0, mf = 1; j < tgtFeatOffsets.length; j++, k++, mf = mf<<1) {
      if ((mFeats & mf) == mf) {
        k--;
        assertEquals(-1, tgtFeatOffsets[j]);
        continue;
      }
      assertEquals(k, tgtFeatOffsets[j]);
    }
    final boolean[] featSrcInTg = m.getFSrcInTgt(tCode);
    for (int j = 0; j < featSrcInTg.length; j++) {
      assertEquals(true, featSrcInTg[j]);
    }  
  }

  private void chkMissingFeats1(CasTypeSystemMapper m, int tCode, int mFeats) {
    final int[] tgtFeatOffsets = m.getTgtFeatOffsets2Src(tCode);
    for (int j = 0, k = 0, mf = 1; j < tgtFeatOffsets.length; j++, k++, mf = mf<<1) {
      if ((mFeats & mf) == mf) {
        k++;
      }
      assertEquals(k, tgtFeatOffsets[j]);
    }
    final boolean[] featSrcInTg = m.getFSrcInTgt(tCode);
    for (int j = 0, mf = 1; j < featSrcInTg.length; j++, mf = mf<<1) {
      assertEquals((mFeats & mf) != mf, featSrcInTg[j]);
    }  
  }
  
  private void chkbase(CasTypeSystemMapper m) {
    chkbase(m, 36);
  }
  
  private void chkbase(CasTypeSystemMapper m, final int last) {
    for (int i = 1; i <= last; i++) {
      assertEquals(i, m.mapTypeCodeSrc2Tgt(i));
      assertEquals(i, m.mapTypeCodeTgt2Src(i));
      chkfeats(m, i);
    }        
  }

  /**
   * Create a type system having all kinds of types
   * @return
   */
  private TypeSystemImpl createTs(int types, int feats1, int feats2) {
    return (TypeSystemImpl) CASInitializer.initCas(new CASTestSetup(types, feats1, feats2)).getTypeSystem();
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


  public class CASTestSetup  implements AnnotatorInitializer {
    final int typesToGenerate;
    final boolean[] ttgb;
    final int featsToInclude1;
    final int featsToInclude2;
    final BitSet ftg1 = new BitSet(32);
    final BitSet ftg2 = new BitSet(32);

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
    
    
    /** 
     * Type system

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
    
    public void initTypeSystem(TypeSystemMgr tsm) {
      // Add new types and features.
      topType = tsm.getTopType();
      typeArrayInt = tsm.getType(CAS.TYPE_NAME_INTEGER_ARRAY);
      typeArrayFs = tsm.getType(CAS.TYPE_NAME_FS_ARRAY);
      typeArrayFloat = tsm.getType(CAS.TYPE_NAME_FLOAT_ARRAY);
      typeArrayDouble = tsm.getType(CAS.TYPE_NAME_DOUBLE_ARRAY);
      typeArrayLong = tsm.getType(CAS.TYPE_NAME_LONG_ARRAY);
      typeArrayShort = tsm.getType(CAS.TYPE_NAME_SHORT_ARRAY);
      typeArrayByte = tsm.getType(CAS.TYPE_NAME_BYTE_ARRAY);
      typeArrayBoolean= tsm.getType(CAS.TYPE_NAME_BOOLEAN_ARRAY);
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

      if (ttgb[0]) {
        akof = tsm.addType("akof", topType);
        if (ftg1.get(0)) {akofInt = tsm.addFeature("akofInt", akof, typeInt);}
        if (ftg1.get(1)) {akofFs = tsm.addFeature("akofFs", akof, typeFs);}
        if (ftg1.get(2)) {akofFloat = tsm.addFeature("akofFloat", akof, typeFloat);}
        if (ftg1.get(3)) {akofDouble = tsm.addFeature("akofDouble", akof, typeDouble);}
        if (ftg1.get(4)) {akofLong = tsm.addFeature("akofLong", akof, typeLong);}
        if (ftg1.get(5)) {akofShort = tsm.addFeature("akofShort", akof, typeShort);}
        if (ftg1.get(6)) {akofByte = tsm.addFeature("akofByte", akof, typeByte);}
        if (ftg1.get(7)) {akofBoolean = tsm.addFeature("akofBoolean", akof, typeBoolean);}
        if (ftg1.get(8)) {akofString = tsm.addFeature("akofStr", akof, typeString);}
        
        if (ftg1.get(9)) {akofAint = tsm.addFeature("akofAint", akof, typeArrayInt);}
        if (ftg1.get(10)) {akofAfs = tsm.addFeature("akofAfs", akof, typeArrayFs);}
        if (ftg1.get(11)) {akofAfloat = tsm.addFeature("akofAfloat", akof, typeArrayFloat);}
        if (ftg1.get(12)) {akofAdouble = tsm.addFeature("akofAdouble", akof, typeArrayDouble);}
        if (ftg1.get(13)) {akofAlong = tsm.addFeature("akofAlong", akof, typeArrayLong);}
        if (ftg1.get(14)) {akofAshort = tsm.addFeature("akofAshort", akof, typeArrayShort);}
        if (ftg1.get(15)) {akofAbyte = tsm.addFeature("akofAbyte", akof, typeArrayByte);}
        if (ftg1.get(16)) {akofAboolean = tsm.addFeature("akofAboolean", akof, typeArrayBoolean);}
        if (ftg1.get(17)) {akofAstring = tsm.addFeature("akofAstring", akof, typeArrayString);}        
      }
      if (ttgb[1]) {
        akof2 = tsm.addType("akof2", topType);
        if (ftg2.get(0)) {akof2Int = tsm.addFeature("akof2Int", akof2, typeInt);}
        if (ftg2.get(1)) {akof2Fs = tsm.addFeature("akof2Fs", akof2, typeFs);}
        if (ftg2.get(2)) {akof2Float = tsm.addFeature("akof2Float", akof2, typeFloat);}
        if (ftg2.get(3)) {akof2Double = tsm.addFeature("akof2Double", akof2, typeDouble);}
        if (ftg2.get(4)) {akof2Long = tsm.addFeature("akof2Long", akof2, typeLong);}
        if (ftg2.get(5)) {akof2Short = tsm.addFeature("akof2Short", akof2, typeShort);}
        if (ftg2.get(6)) {akof2Byte = tsm.addFeature("akof2Byte", akof2, typeByte);}
        if (ftg2.get(7)) {akof2Boolean = tsm.addFeature("akof2Boolean", akof2, typeBoolean);}
        if (ftg2.get(8)) {akof2String = tsm.addFeature("akof2Str", akof2, typeString);}
        
        if (ftg2.get(9)) {akof2Aint = tsm.addFeature("akof2Aint", akof2, typeArrayInt);}
        if (ftg2.get(10)) {akof2Afs = tsm.addFeature("akof2Afs", akof2, typeArrayFs);}
        if (ftg2.get(11)) {akof2Afloat = tsm.addFeature("akof2Afloat", akof2, typeArrayFloat);}
        if (ftg2.get(12)) {akof2Adouble = tsm.addFeature("akof2Adouble", akof2, typeArrayDouble);}
        if (ftg2.get(13)) {akof2Along = tsm.addFeature("akof2Along", akof2, typeArrayLong);}
        if (ftg2.get(14)) {akof2Ashort = tsm.addFeature("akof2Ashort", akof2, typeArrayShort);}
        if (ftg2.get(15)) {akof2Abyte = tsm.addFeature("akof2Abyte", akof2, typeArrayByte);}
        if (ftg2.get(16)) {akof2Aboolean = tsm.addFeature("akof2Aboolean", akof2, typeArrayBoolean);}
        if (ftg2.get(17)) {akof2Astring = tsm.addFeature("akof2Astring", akof2, typeArrayString);}        
      }
    }
    public void initIndexes(FSIndexRepositoryMgr irm, TypeSystem ts) {
    }
  }


}
