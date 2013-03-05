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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.BinaryCasSerDes6.ReuseInfo;
import org.apache.uima.cas.test.AnnotatorInitializer;
import org.apache.uima.cas.test.CASInitializer;
import org.apache.uima.internal.util.IntListIterator;
import org.apache.uima.internal.util.rb_trees.IntArrayRBT;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.impl.SerializationMeasures;

/**
 * Serializer and Deserializer testing
 * 
 * 
 */
public class SerDesTest extends TestCase {

  private Type akof;
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

  private CASImpl cas;
  private CASImpl deserCas;
  private CASImpl deltaCas;
  private CASImpl remoteCas;

  private TypeSystemImpl ts;
  private List<FeatureStructure> lfs;
  
  private boolean doPlain = false;

  public class CASTestSetup  implements AnnotatorInitializer {

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
      
      akof = tsm.addType("akof", topType);
      
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

      akofInt = tsm.addFeature("akofInt", akof, typeInt);
      akofFs = tsm.addFeature("akofFs", akof, typeFs);
      akofFloat = tsm.addFeature("akofFloat", akof, typeFloat);
      akofDouble = tsm.addFeature("akofDouble", akof, typeDouble);
      akofLong = tsm.addFeature("akofLong", akof, typeLong);
      akofShort = tsm.addFeature("akofShort", akof, typeShort);
      akofByte = tsm.addFeature("akofByte", akof, typeByte);
      akofBoolean = tsm.addFeature("akofBoolean", akof, typeBoolean);
      akofString = tsm.addFeature("akofStr", akof, typeString);
      
      akofAint = tsm.addFeature("akofAint", akof, typeArrayInt);
      akofAfs = tsm.addFeature("akofAfs", akof, typeArrayFs);
      akofAfloat = tsm.addFeature("akofAfloat", akof, typeArrayFloat);
      akofAdouble = tsm.addFeature("akofAdouble", akof, typeArrayDouble);
      akofAlong = tsm.addFeature("akofAlong", akof, typeArrayLong);
      akofAshort = tsm.addFeature("akofAshort", akof, typeArrayShort);
      akofAbyte = tsm.addFeature("akofAbyte", akof, typeArrayByte);
      akofAboolean = tsm.addFeature("akofAboolean", akof, typeArrayBoolean);
      akofAstring = tsm.addFeature("akofAstring", akof, typeArrayString);
    }

    public void initIndexes(FSIndexRepositoryMgr irm, TypeSystem ts) {
    }
  }

  public void setUp() {
    try {
      this.cas = (CASImpl) CASInitializer.initCas(new CASTestSetup());
      this.ts = (TypeSystemImpl) this.cas.getTypeSystem();
      deserCas = (CASImpl) CasCreationUtils.createCas(ts, null, null, null);
      deltaCas = (CASImpl) CasCreationUtils.createCas(ts, null, null, null);
      remoteCas = (CASImpl) CasCreationUtils.createCas(ts, null, null, null);
      lfs = new ArrayList<FeatureStructure>();
    } catch (Exception e) {
      assertTrue(false);
    }
  }

  public void tearDown() {
    this.cas = null;
    this.ts = null;
    deserCas = null;
    deltaCas = null;
    remoteCas = null;
    lfs = null;
  }

  public void testDeltaWithStringArrayMod() throws IOException {
    loadCas(cas);
    ReuseInfo[] ri = serializeDeserialize(cas, remoteCas, null, null);
    MarkerImpl marker = (MarkerImpl) remoteCas.createMarker();
    lfs = getIndexedFSs(remoteCas);
    FeatureStructure fs = lfs.get(10);
    StringArrayFS sa = (StringArrayFS) fs.getFeatureValue(akofAstring);
    sa.set(0, "change2");
    verifyDelta(marker, ri);
  }

  public void testDeltaWithDblArrayMod() throws IOException {
    loadCas(cas);
    ReuseInfo[] ri = serializeDeserialize(cas, remoteCas, null, null);
    MarkerImpl marker = (MarkerImpl) remoteCas.createMarker();
    lfs = getIndexedFSs(remoteCas);
    FeatureStructure fs = lfs.get(10);  /* has double array length 2 */
    DoubleArrayFS d = (DoubleArrayFS) fs.getFeatureValue(akofAdouble);
    d.set(0, 12.34D);
    verifyDelta(marker, ri);
  }
  
  public void testDeltaWithByteArrayMod() throws IOException {
    loadCas(cas);
    ReuseInfo[] ri = serializeDeserialize(cas, remoteCas, null, null);
    MarkerImpl marker = (MarkerImpl) remoteCas.createMarker();
    
    lfs = getIndexedFSs(remoteCas); // gets FSs below the line

    FeatureStructure fs = lfs.get(10);
    ByteArrayFS sfs = (ByteArrayFS) fs.getFeatureValue(akofAbyte);
    sfs.set(0, (byte)21);

    verifyDelta(marker, ri);
  }

  public void testDeltaWithStrArrayMod() throws IOException {
    loadCas(cas);
    ReuseInfo[] ri = serializeDeserialize(cas, remoteCas, null, null);
    MarkerImpl marker = (MarkerImpl) remoteCas.createMarker();
    
    lfs = getIndexedFSs(remoteCas);

    FeatureStructure fs = lfs.get(10);
    StringArrayFS sfs = (StringArrayFS) fs.getFeatureValue(akofAstring);
    sfs.set(0, "change");

    verifyDelta(marker, ri);
  }
  
  /**
   * Make one of each kind of artifact, including arrays
   * serialize to byte stream, deserialize into new cas, compare
   */
  
  public void testAllKinds() {
    loadCas(cas);  
    verify();
  }
  
  /**
   * 1) create a base cas with some data
   * 2) serialize it out and then back into a remoteCas
   *    This is needed to get the proper ordering in the remote cas of the 
   *    feature structures - they will be ordered by their fs addr, but omitting
   *    "skipped" FSs (not in the index or reachable, and not in the target ts)
   * 2) create the mark in the remoteCAS: cas.createMarker()
   * 3) add more cas data to the remoteCas
   * 4) serialize with marker
   * 5) deserialize back into base cas
   * 6) check resulting base cas = remote cas
   * 
   * 
   */
  public void testDelta() {
    loadCas(cas);
    ReuseInfo[] ri = serializeDeserialize(cas, remoteCas, null, null);
    
    MarkerImpl marker = (MarkerImpl) remoteCas.createMarker();
    loadCas(remoteCas);
    verifyDelta(marker, ri);
  }
  
  public void testDeltaWithRefsBelow() {
    lfs.clear();
    loadCas(cas);
    ReuseInfo ri[] = serializeDeserialize(cas, remoteCas, null, null);
    MarkerImpl marker = (MarkerImpl) remoteCas.createMarker();
    
    lfs = getIndexedFSs(remoteCas);
    FeatureStructure fs = remoteCas.createFS(akof);
    fs.setFeatureValue(akofFs, lfs.get(0));
    ArrayFS fsafs = remoteCas.createArrayFS(4);
    fsafs.set(1, lfs.get(1));
    fsafs.set(2, lfs.get(2));
    fsafs.set(3, lfs.get(3));
    fs.setFeatureValue(akofAfs, fsafs);
    
    verifyDelta(marker, ri);
  }

  public void testDeltaWithMods() {
    lfs.clear();
    loadCas(cas);
    ReuseInfo ri[] = serializeDeserialize(cas, remoteCas, null, null);
    MarkerImpl marker = (MarkerImpl) remoteCas.createMarker();
    
    lfs = getIndexedFSs(remoteCas);
    FeatureStructure fs = remoteCas.createFS(akof);
    lfs.get(0).setFeatureValue(akofFs, fs);
    
    verifyDelta(marker, ri);
  }
  
  /**
   * Variations to cover:
   *   all kinds of slots
   *   multiple sets of values
   *     test diffs
   *   multiple orders (try reverse and random order)
   *   
   * Driver for random values
   *   pick among random and "interesting" edge case values
   *     
   *   
   */
  public void testDeltaWithAllMods() {
    Random sg = new Random();
    long seed = sg.nextLong();
    Random r = new Random(seed);
    System.out.format("RandomSeed: %,d%n", seed);

    for (int i = 0; i < 100; i ++ ) {
      checkDeltaWithAllMods(r);
      tearDown();
      setUp();
    }
  }

  public void checkDeltaWithAllMods(Random r) {
    makeRandomFss(cas, 7, r);
    loadCas(cas);
    ReuseInfo ri[] = serializeDeserialize(cas, remoteCas, null, null);
    MarkerImpl marker = (MarkerImpl) remoteCas.createMarker();
    
    lfs = getIndexedFSs(remoteCas);
    
    makeRandomFss(remoteCas, 8, r);

    int i = 0;
    for (FeatureStructure fs : lfs) {
      if (((i++) % 2) == 0) {
        fs.setFeatureValue(akofFs, lfs.get(r.nextInt(lfs.size())));
      }
    }
    
    makeRandomUpdatesBelowMark(remoteCas, r);
    
    verifyDelta(marker, ri);

  }
  

  
  public void testDeltaWithIndexMods() throws IOException {
    loadCas(cas);
    ReuseInfo ri[] = serializeDeserialize(cas, remoteCas, null, null);
    MarkerImpl marker = (MarkerImpl) remoteCas.createMarker();
    
    lfs = new ArrayList<FeatureStructure>();
    loadCas(remoteCas);
    
    List<FeatureStructure> lfs2 = getIndexedFSs(remoteCas);

    remoteCas.getIndexRepository().removeFS(lfs2.get(0));
    remoteCas.getIndexRepository().removeFS(lfs2.get(1));
    remoteCas.getIndexRepository().addFS(lfs2.get(1));  // should appear as reindexed

    remoteCas.getIndexRepository().removeFS(lfs.get(0));
    remoteCas.getIndexRepository().removeFS(lfs.get(1));
    remoteCas.getIndexRepository().addFS(lfs.get(1)); 

    verifyDelta(marker, ri);
  }
  
  public void testWithOtherSerializer() {
    doPlain = true;
    testDeltaWithMods();
    tearDown(); setUp();
    testDeltaWithRefsBelow();
    tearDown(); setUp();
//    testDeltaWithAllMods();
    tearDown(); setUp();
    testAllKinds();
    tearDown(); setUp();
    testArrayAux();
  }

  public void testArrayAux() {
    ArrayList<FeatureStructure> fsl = new ArrayList<FeatureStructure>();
    /**
     * Strings, non-array Long/Double:
     * Make equal items,
     * ser/deser, update one of the equal items, insure other not updated
     */
    FeatureStructure fsAt1 = newAkof(cas, fsl);
    FeatureStructure fsAt2 = newAkof(cas, fsl);
    cas.addFsToIndexes(fsAt1);
    cas.addFsToIndexes(fsAt2);

    createStringA(cas, fsAt1, "at");
    createStringA(cas, fsAt2, "at");
    verify();
    
    FSIterator<FeatureStructure> it = deserCas.indexRepository.getAllIndexedFS(akof);
    FeatureStructure fsAt1d = it.next();
    FeatureStructure fsAt2d = it.next();
    StringArrayFS sa1 = (StringArrayFS) fsAt1d.getFeatureValue(akofAstring);
    StringArrayFS sa2 = (StringArrayFS) fsAt2d.getFeatureValue(akofAstring);
    sa1.set(1, "def");
    assertEquals(sa2.get(1), "abcat");
    assertEquals(sa1.get(1), "def");
    cas.reset();
    
    fsAt1 = newAkof(cas, fsl);
    fsAt2 = newAkof(cas, fsl);
    cas.addFsToIndexes(fsAt1);
    cas.addFsToIndexes(fsAt2);

    createLongA(cas, fsAt1, 9);
    createLongA(cas, fsAt2, 9);
    verify();
    
    it = deserCas.indexRepository.getAllIndexedFS(akof);
    fsAt1d = it.next();
    fsAt2d = it.next();
    LongArrayFS la1 = (LongArrayFS) fsAt1d.getFeatureValue(akofAlong);
    LongArrayFS la2 = (LongArrayFS) fsAt2d.getFeatureValue(akofAlong);
    la1.set(2, 123L);
    assertEquals(la2.get(2), -45 + 9);
    assertEquals(la1.get(2), 123);
  }
  
  
  
//  /*******************************
//   * Helper functions
//   * @throws IOException 
//   *******************************/
//  private ReuseInfo getReuseInfo() {
//    BinaryCasSerDes6 bcs = new BinaryCasSerDes6(cas); 
//    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
//    try {
//      bcs.serialize(baos);
//    } catch (IOException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }
//    return bcs.getReuseInfo();
//  }
  
  
  private void createStringA(CASImpl cas, FeatureStructure fs, String x) {
    StringArrayFS strafs = cas.createStringArrayFS(5);
    strafs.set(3, null);
    strafs.set(2, "" + x);
    strafs.set(1, "abc" + x);
    strafs.set(0, "abc" + x);
    strafs.set(4, "def" + x);
    fs.setFeatureValue(akofAstring, strafs);
  }
  
  private void createIntA (CASImpl cas, FeatureStructure fs, int x) {
    IntArrayFS iafs = cas.createIntArrayFS(4 + x);
    iafs.set(0, Integer.MAX_VALUE - x);
    iafs.set(1, Integer.MIN_VALUE + x);
    iafs.set(2, 17 + 100 * x);
    fs.setFeatureValue(akofAint, iafs);
  }
  
  private void createFloatA (CASImpl cas, FeatureStructure fs, float x) {
    FloatArrayFS fafs = cas.createFloatArrayFS(6);
    fafs.set(0, Float.MAX_VALUE - x);
//    fafs.set(1, Float.MIN_NORMAL + x);
    fafs.set(2, Float.MIN_VALUE + x);
    fafs.set(3, Float.NaN);
    fafs.set(4, Float.NEGATIVE_INFINITY);
    fafs.set(5, Float.POSITIVE_INFINITY);
    fs.setFeatureValue(akofAfloat, fafs);
  }

  private void createDoubleA (CASImpl cas, FeatureStructure fs, double x) {
    DoubleArrayFS fafs = cas.createDoubleArrayFS(6);
    fafs.set(0, Double.MAX_VALUE - x);
//    fafs.set(1, Double.MIN_NORMAL + x);
    fafs.set(2, Double.MIN_VALUE + x);
    fafs.set(3, Double.NaN);
    fafs.set(4, Double.NEGATIVE_INFINITY);
    fafs.set(5, Double.POSITIVE_INFINITY);
    fs.setFeatureValue(akofAdouble, fafs);
  }

  private void createLongA (CASImpl cas, FeatureStructure fs, long x) {
    LongArrayFS lafs = cas.createLongArrayFS(4);
    lafs.set(0, Long.MAX_VALUE - x);
    lafs.set(1, Long.MIN_VALUE + x);
    lafs.set(2, -45 + x);
    fs.setFeatureValue(akofAlong, lafs);
  }
  
//  private void binaryCopyCas(CASImpl c1, CASImpl c2) {
//    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//    Serialization.serializeCAS(cas, baos);
//    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
//    c2.reinit(bais);
//  }
  
  private FeatureStructure newAkof(CASImpl cas, List<FeatureStructure> fsl) {
    FeatureStructure fs = cas.createFS(akof);
    fsl.add(fs);
    return fs;
  }
  
  // make an instance of akof with all features set
  private FeatureStructure makeAkof(CASImpl cas, Random r) {
    FeatureStructure fs = cas.createFS(akof);
    fs.setBooleanValue(akofBoolean, r.nextBoolean());
    fs.setByteValue(akofByte, (byte)r.nextInt());
    fs.setShortValue(akofShort, (short)r.nextInt());
    fs.setIntValue(akofInt, r.nextInt());
    fs.setFloatValue(akofFloat, r.nextFloat());
    fs.setLongValue(akofLong, r.nextLong());
    fs.setDoubleValue(akofDouble, r.nextDouble());
    fs.setStringValue(akofString, randomString(r));
    fs.setFeatureValue(akofFs, fs);
    
    fs.setFeatureValue(akofAint, randomIntA(cas, r));
    fs.setFeatureValue(akofAfs, cas.createArrayFS(1));
    fs.setFeatureValue(akofAfloat, randomFloatA(cas, r));
    fs.setFeatureValue(akofAdouble, randomDoubleA(cas, r));
    fs.setFeatureValue(akofAlong, randomLongA(cas, r));
    fs.setFeatureValue(akofAshort, randomShortA(cas, r));
    fs.setFeatureValue(akofAbyte, randomByteA(cas, r));
    fs.setFeatureValue(akofAboolean, cas.createBooleanArrayFS(2));
    fs.setFeatureValue(akofAstring, randomStringA(cas, r));

    return fs;    
  }
    
  private static final String[] stringValues = {
    "abc", "abcdef", null, "", "ghijklm", "a", "b"
  };
  private String randomString(Random r) {
    return stringValues[r.nextInt(7)];
  }

  private StringArrayFS randomStringA(CASImpl cas, Random r) {
    int length = r.nextInt(2) + 1;
    StringArrayFS fs = cas.createStringArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, stringValues[r.nextInt(stringValues.length)]);
    }
    return fs;
  }

  
  private IntArrayFS randomIntA(CASImpl cas, Random r) {
    int length = r.nextInt(2) + 1;
    IntArrayFS fs = cas.createIntArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, r.nextInt(101) - 50);
    }
    return fs;
  }
  
  private static final byte[] byteValues = {
    1, 0, -1, Byte.MAX_VALUE, Byte.MIN_VALUE, 9, -9  };
  
  private ByteArrayFS randomByteA(CASImpl cas, Random r) {
    int length = r.nextInt(2) + 1;
    ByteArrayFS fs = cas.createByteArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, byteValues[r.nextInt(byteValues.length)]);
    }
    return fs;
  }

  private static final long[] longValues = {
    1L, 0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, 11L, -11L  };
  
  private LongArrayFS randomLongA(CASImpl cas, Random r) {
    int length = r.nextInt(2) + 1;
    LongArrayFS fs = cas.createLongArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, longValues[r.nextInt(longValues.length)]);
    }
    return fs;
  }

  private static final short[] shortValues = {
    1, 0, -1, Short.MAX_VALUE, Short.MIN_VALUE, 22, -22  };
  
  private ShortArrayFS randomShortA(CASImpl cas, Random r) {
    int length = r.nextInt(2) + 1;
    ShortArrayFS fs = cas.createShortArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, shortValues[r.nextInt(shortValues.length)]);
    }
    return fs;
  }

  private static final double[] doubleValues = {
    1d, 0d, -1d, Double.MAX_VALUE, /*Double.MIN_NORMAL,*/ Double.MIN_VALUE, 33d, -33.33d  };
  
  private DoubleArrayFS randomDoubleA(CASImpl cas, Random r) {
    int length = r.nextInt(2) + 1;
    DoubleArrayFS fs = cas.createDoubleArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, doubleValues[r.nextInt(doubleValues.length)]);
    }
    return fs;
  }

  private static final float[] floatValues = {
    1f, 0f, -1f, Float.MAX_VALUE, /*Float.MIN_NORMAL,*/ Float.MIN_VALUE, 17f, -22.33f  };
  
  private FloatArrayFS randomFloatA(CASImpl cas, Random r) {
    int length = r.nextInt(2) + 1;
    FloatArrayFS fs = cas.createFloatArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, floatValues[r.nextInt(floatValues.length)]);
    }
    return fs;
  }
  
  private void makeRandomFss(CASImpl cas, int n, Random r) {
    List<FeatureStructure> lfss = new ArrayList<FeatureStructure>();
    for (int i = 0; i < n; i++) {
      FeatureStructure fs = makeAkof(cas, r);
      if (r.nextBoolean()) {
        cas.addFsToIndexes(fs);
        lfss.add(fs);
        lfs.add(fs);
      }
    }
    for (FeatureStructure fs : lfss) {
      fs.setFeatureValue(akofFs, lfss.get(r.nextInt(lfss.size())));
    }
  }
  
  private void loadCas(CASImpl cas) {
    /* lfs index: 0 */
    FeatureStructure fs = newAkof(cas, lfs);
    fs.setBooleanValue(akofBoolean, true);
    fs.setByteValue(akofByte, (byte)109);
    fs.setShortValue(akofShort, (short) 23);
    fs.setIntValue(akofInt, 2345);
    fs.setFloatValue(akofFloat, 123f);
    fs.setLongValue(akofLong, 345L);
    fs.setDoubleValue(akofDouble, 334455.6677d);
    fs.setStringValue(akofString, "str1");
    fs.setFeatureValue(akofFs, fs);
    cas.addFsToIndexes(fs);
    FeatureStructure fs1 = fs;
    
    //extreme or unusual values
    /* lfs index: 1 */
    fs = newAkof(cas, lfs);
    fs.setBooleanValue(akofBoolean, false);
    fs.setByteValue(akofByte, Byte.MAX_VALUE);
    fs.setShortValue(akofShort, (short) Short.MAX_VALUE);
    fs.setIntValue(akofInt, Integer.MAX_VALUE);
    fs.setFloatValue(akofFloat, Float.MAX_VALUE);
    fs.setLongValue(akofLong, Long.MAX_VALUE);
    fs.setDoubleValue(akofDouble, Double.MAX_VALUE);
    fs.setStringValue(akofString, "");
    fs.setFeatureValue(akofFs, fs1);
    cas.addFsToIndexes(fs);

    /* lfs index: 2 */
    fs = newAkof(cas, lfs);
    fs.setByteValue(akofByte, Byte.MIN_VALUE);
    fs.setShortValue(akofShort, (short) Short.MIN_VALUE);
    fs.setIntValue(akofInt, Integer.MIN_VALUE);
    fs.setFloatValue(akofFloat, Float.MIN_VALUE);
    fs.setLongValue(akofLong, Long.MIN_VALUE);
    fs.setDoubleValue(akofDouble, Double.MIN_VALUE);
    fs.setStringValue(akofString, null);
    fs.setFeatureValue(akofFs, fs1);
    cas.addFsToIndexes(fs);
    FeatureStructure fs3 = fs;

    /* lfs index: 3 */
    fs = newAkof(cas, lfs);
    fs.setByteValue(akofByte, (byte)0);
    fs.setShortValue(akofShort, (short) 0);
    fs.setIntValue(akofInt, 0);
    fs.setFloatValue(akofFloat, 0f);
    fs.setLongValue(akofLong, 0L);
    fs.setDoubleValue(akofDouble, 0D);
    fs.setFeatureValue(akofFs, fs1);
    cas.addFsToIndexes(fs);
    fs3.setFeatureValue(akofFs, fs);  // make a forward ref
    FeatureStructure fs4 = fs;

    /* lfs index: 4 */
    fs = newAkof(cas, lfs);
    fs.setByteValue(akofByte, (byte)1);
    fs.setShortValue(akofShort, (short)1);
    fs.setIntValue(akofInt, 1);
    fs.setFloatValue(akofFloat, 1.0f);
    fs.setLongValue(akofLong, 1L);
    fs.setDoubleValue(akofDouble, 1.0D);
    cas.addFsToIndexes(fs);
    
//    fs = newAkof(cas, lfs);
//    fs.setFloatValue(akofFloat, Float.MIN_NORMAL);
//    fs.setDoubleValue(akofDouble, Double.MIN_NORMAL);
//    cas.addFsToIndexes(fs);
    
    /* lfs index: 5 */
    fs = newAkof(cas, lfs);
    fs.setFloatValue(akofFloat, Float.MIN_VALUE);
    fs.setDoubleValue(akofDouble, Double.MIN_VALUE);
    cas.addFsToIndexes(fs);

    /* lfs index: 6 */
    fs = newAkof(cas, lfs);
    fs.setFloatValue(akofFloat, Float.NaN);
    fs.setDoubleValue(akofDouble, Double.NaN);
    cas.addFsToIndexes(fs);

    /* lfs index: 7 */
    fs = newAkof(cas, lfs);
    fs.setFloatValue(akofFloat, Float.POSITIVE_INFINITY);
    fs.setDoubleValue(akofDouble, Double.POSITIVE_INFINITY);
    cas.addFsToIndexes(fs);

    /* lfs index: 8 */
    fs = newAkof(cas, lfs);
    fs.setFloatValue(akofFloat, Float.NEGATIVE_INFINITY);
    fs.setDoubleValue(akofDouble, Double.NEGATIVE_INFINITY);
    cas.addFsToIndexes(fs);

    
    // test arrays
    /* lfs index: 9 */
    fs = newAkof(cas, lfs);
    fs.setFeatureValue(akofAint, cas.createIntArrayFS(0));
    fs.setFeatureValue(akofAfs, cas.createArrayFS(0));
    fs.setFeatureValue(akofAfloat, cas.createFloatArrayFS(0));
    fs.setFeatureValue(akofAdouble, cas.createDoubleArrayFS(0));
    fs.setFeatureValue(akofAlong, cas.createLongArrayFS(0));
    fs.setFeatureValue(akofAshort, cas.createShortArrayFS(0));
    fs.setFeatureValue(akofAbyte, cas.createByteArrayFS(0));
    fs.setFeatureValue(akofAboolean, cas.createBooleanArrayFS(0));
    fs.setFeatureValue(akofAstring, cas.createStringArrayFS(0));
    cas.addFsToIndexes(fs);
    FeatureStructure fs8 = fs;

    /* lfs index: 10 */
    fs = newAkof(cas, lfs);
    fs.setFeatureValue(akofAint, cas.createIntArrayFS(2));
    fs.setFeatureValue(akofAfs, cas.createArrayFS(2));
    fs.setFeatureValue(akofAfloat, cas.createFloatArrayFS(2));
    fs.setFeatureValue(akofAdouble, cas.createDoubleArrayFS(2));
    fs.setFeatureValue(akofAlong, cas.createLongArrayFS(2));
    fs.setFeatureValue(akofAshort, cas.createShortArrayFS(2));
    fs.setFeatureValue(akofAbyte, cas.createByteArrayFS(2));
    fs.setFeatureValue(akofAboolean, cas.createBooleanArrayFS(2));
    fs.setFeatureValue(akofAstring, cas.createStringArrayFS(2));
    cas.addFsToIndexes(fs);
    
    /* lfs index: 11 */
    fs = newAkof(cas, lfs);
    cas.addFsToIndexes(fs);
    
    createIntA(cas, fs, 0);
    
    // feature structure array
    /* lfs index: 12 */
    ArrayFS fsafs = cas.createArrayFS(4);
    fsafs.set(1, fs8);
    fsafs.set(2, fs1);
    fsafs.set(3, fs4);
    fs.setFeatureValue(akofAfs, fsafs);
    
    createFloatA(cas, fs, 0f);
    createDoubleA(cas, fs, 0d);
    createLongA(cas, fs, 0L);
    
    ShortArrayFS safs = cas.createShortArrayFS(4);
    safs.set(0, Short.MAX_VALUE);
    safs.set(1, Short.MIN_VALUE);
    safs.set(2, (short)-485);
    fs.setFeatureValue(akofAshort, safs);
    
    ByteArrayFS bafs = cas.createByteArrayFS(4);
    bafs.set(0, Byte.MAX_VALUE);
    bafs.set(1, Byte.MIN_VALUE);
    bafs.set(2, (byte) 33);
    fs.setFeatureValue(akofAbyte, bafs);
    
    BooleanArrayFS booafs = cas.createBooleanArrayFS(4);
    booafs.set(0, true);
    booafs.set(1, false);
    fs.setFeatureValue(akofAboolean, booafs);
    
    createStringA(cas, fs, "");
    makeRandomFss(cas, 15, new Random());
  }

  private void verify() {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
      if (doPlain) {
        (new CASSerializer()).addCAS(cas, baos);      
      } else {      
        BinaryCasSerDes6 bcs = new BinaryCasSerDes6(cas);
        SerializationMeasures sm = bcs.serialize(baos);
        if (null != sm) {
          System.out.println(sm);
        }
      }
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      deserCas.reinit(bais);
      assertTrue(BinaryCasSerDes6.compareCASes(cas, deserCas));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }    
  }

  private ReuseInfo[] serializeDeserialize(
      CASImpl casSrc, 
      CASImpl casTgt, 
      ReuseInfo ri, 
      MarkerImpl mark) {
    ReuseInfo[] riToReturn = new ReuseInfo[2];
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
      if (doPlain) {
        if (null == mark) {
          Serialization.serializeCAS(cas, baos);
        } else {
          Serialization.serializeCAS(casSrc, baos, mark);
        }
      } else {
        BinaryCasSerDes6 bcs = new BinaryCasSerDes6(casSrc, mark);
        SerializationMeasures sm = bcs.serialize(baos);
        if (sm != null) {System.out.println(sm);}
        riToReturn[0] = bcs.getReuseInfo();
      }
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      if (doPlain) {
        casTgt.reinit(bais);
      } else {
        BinaryCasSerDes6 bcsDeserRmt = new BinaryCasSerDes6(casTgt);
        bcsDeserRmt.deserialize(bais);
        riToReturn[1] = bcsDeserRmt.getReuseInfo();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return riToReturn;
  }
  
  private void verifyDelta(MarkerImpl mark, ReuseInfo[] ri) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
      if (doPlain) {
        Serialization.serializeCAS(remoteCas, baos, mark);
      } else {
        BinaryCasSerDes6 bcs = new BinaryCasSerDes6(remoteCas, mark, null, ri[1]);
        SerializationMeasures sm = bcs.serialize(baos);
        if (sm != null) {System.out.println(sm);}
      }
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      if (doPlain) {
        cas.reinit(bais);
      } else {
          BinaryCasSerDes6 bcsDeserialize = new BinaryCasSerDes6(cas, null, null, ri[0]);
//        cas.reinit(bais, ri);
          bcsDeserialize.deserialize(bais);
      }
      assertTrue(BinaryCasSerDes6.compareCASes(cas, remoteCas));
      
      // verify indexed fs same, and in same order - already done by compareCASes
//      int[] fsIndexes1 = cas.getIndexedFSs();
//      int[] fsIndexes2 = deltaCas.getIndexedFSs();
//      assertTrue(Arrays.equals(fsIndexes1, fsIndexes2));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  private void makeRandomUpdatesBelowMark(CASImpl cas, Random r) {
    for (FeatureStructure fs : lfs) {
      makeRandomUpdate(cas, fs, r);
    }
  }

  private void makeRandomUpdate(CASImpl cas, FeatureStructure fs, Random r) {
    int n = r.nextInt(3);
    for (int i = 0 ; i < n; i++) {
      switch (r.nextInt(26)) {
      case 0:
        fs.setBooleanValue(akofBoolean, r.nextBoolean());
        break;
      case 1:
        fs.setByteValue(akofByte, (byte)r.nextInt());
        break;
      case 2:
        fs.setShortValue(akofShort, (short)r.nextInt());
        break;
      case 3:
        fs.setIntValue(akofInt, r.nextInt());
        break;
      case 4:
        fs.setFloatValue(akofFloat, r.nextFloat());
        break;
      case 5:
        fs.setLongValue(akofLong, r.nextLong());
        break;
      case 6:
        fs.setDoubleValue(akofDouble, r.nextDouble());
        break;
      case 7:
        fs.setStringValue(akofString, randomString(r));
        break;
      case 8:
        fs.setFeatureValue(akofFs, fs);
        break;
      case 9:
        fs.setFeatureValue(akofAint, randomIntA(cas, r));
        break;
      case 10:
        fs.setFeatureValue(akofAfs, cas.createArrayFS(1));
        break;
      case 11:
        fs.setFeatureValue(akofAfloat, randomFloatA(cas, r));
        break;
      case 12:
        fs.setFeatureValue(akofAdouble, randomDoubleA(cas, r));
        break;
      case 13:
        fs.setFeatureValue(akofAlong, randomLongA(cas, r));
        break;
      case 14:
        fs.setFeatureValue(akofAshort, randomShortA(cas, r));
        break;
      case 15:
        fs.setFeatureValue(akofAbyte, randomByteA(cas, r));
        break;
      case 16:
        fs.setFeatureValue(akofAboolean, cas.createBooleanArrayFS(2));
        break;
      case 17: 
        fs.setFeatureValue(akofAstring, randomStringA(cas, r));
        break;
      case 18: {
          IntArrayFS sfs = (IntArrayFS) fs.getFeatureValue(akofAint);
          if ((null != sfs) && (0 < sfs.size())) {
            sfs.set(0, 1);
          }
        }
        break;
      case 19:{
          StringArrayFS sfs = (StringArrayFS) fs.getFeatureValue(akofAstring);
          if ((null != sfs) && (0 < sfs.size())) {
            sfs.set(0, "change");
          }
        }
        break;
      case 20: {
          FloatArrayFS sfs = (FloatArrayFS) fs.getFeatureValue(akofAfloat);
          if ((null != sfs) && (0 < sfs.size())) {
            sfs.set(0, 1F);
          }
        }
        break;
      case 21: {
          DoubleArrayFS sfs = (DoubleArrayFS) fs.getFeatureValue(akofAdouble);
          if ((null != sfs) && (0 < sfs.size())) {
            sfs.set(0, 1D);
          }
        }
        break;
      case 22: {
          LongArrayFS sfs = (LongArrayFS) fs.getFeatureValue(akofAlong);
          if ((null != sfs) && (0 < sfs.size())) {
            sfs.set(0, 1L);
          }
        }
        break;
      case 23: {
          ShortArrayFS sfs = (ShortArrayFS) fs.getFeatureValue(akofAshort);
          if ((null != sfs) && (0 < sfs.size())) {
            sfs.set(0, (short)1);
          }
        }
        break;
      case 24: {
          ByteArrayFS sfs = (ByteArrayFS) fs.getFeatureValue(akofAbyte);
          if ((null != sfs) && (0 < sfs.size())) {
            sfs.set(0, (byte)1);
          }
        }
        break;
      case 25: {
          ArrayFS sfs = (ArrayFS) fs.getFeatureValue(akofAfs);
          if ((null != sfs) && (0 < sfs.size())) {
            sfs.set(0, lfs.get(r.nextInt(lfs.size())));
          }
        }
      break;
      }
    }
  }

  private List<FeatureStructure> getIndexedFSs(CASImpl cas) {
    FSIterator<FeatureStructure> it = cas.getIndexRepository().getAllIndexedFS(akof);
    List<FeatureStructure> lfs = new ArrayList<FeatureStructure>();
    while (it.hasNext()) {
      lfs.add(it.next());
    }
    return lfs;
  }
}
