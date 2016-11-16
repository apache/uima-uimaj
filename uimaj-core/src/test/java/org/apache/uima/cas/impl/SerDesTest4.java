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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.apache.uima.cas.test.AnnotatorInitializer;
import org.apache.uima.cas.test.CASInitializer;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.impl.SerializationMeasures;

import junit.framework.TestCase;

/**
 * Serializer and Deserializer testing
 * for version 4 of Binary Compressed 
 * 
 * Has main method for creating resources to use in testing
 *   will update resources in SerDes4.  If you do this by mistake, just revert those resources.
 */
public class SerDesTest4 extends TestCase {

  private static final boolean includeUid = false;
  private static final AtomicInteger aint = includeUid? new AtomicInteger(0) : null;
  private static final Random randomseed = new Random();
  private static long seed = 
// 1_449_257_605_347_913_923L;
// 949_754_466_380_345_024L;
// 6_761_039_426_734_540_557L;
// 1_217_808_400_762_898_611L;
   randomseed.nextLong();
//  static {
//    System.out.format("SerDesTest4 RandomSeed: %,d%n", seed);
//  }

  
  class MyRandom extends Random {

    @Override
    public int nextInt(int n) {
      int r = usePrevData ? readNextSavedInt() : super.nextInt(n);
      if (capture) writeSavedInt(r);
      return r;
    }

    @Override
    public int nextInt() {
      int r = usePrevData ? readNextSavedInt() : super.nextInt();
      if (capture) writeSavedInt(r);
      return r;
    }

    @Override
    public long nextLong() {
      int r = usePrevData ? readNextSavedInt() : super.nextInt();
      if (capture) writeSavedInt(r);      
      return r;
    }

    @Override
    public boolean nextBoolean() {
      int r = usePrevData ? readNextSavedInt() : super.nextInt(2);
      if (capture) writeSavedInt(r);      
      return r == 0;
    }

    @Override
    public float nextFloat() {
      int r = usePrevData ? readNextSavedInt() : super.nextInt(0x7ffff);
      if (capture) writeSavedInt(r);
      return Float.intBitsToFloat(r);
    }

    @Override
    public double nextDouble() {
      int r = usePrevData ? readNextSavedInt() : super.nextInt(0x7ffff);
      if (capture) writeSavedInt(r);
      return CASImpl.long2double((long) r);
    }
  }

  private final Random           random      = new MyRandom();
  private char[]                 sbSavedInts = new char[20];
  private BufferedReader         savedIntsStream;
  private OutputStreamWriter     savedIntsOutStream;

  private Type                   akof;
  private Type                   topType;
  private Type                   typeArrayInt;
  private Type                   typeArrayFs;
  private Type                   typeArrayFloat;
  private Type                   typeArrayDouble;
  private Type                   typeArrayLong;
  private Type                   typeArrayShort;
  private Type                   typeArrayByte;
  private Type                   typeArrayBoolean;
  private Type                   typeArrayString;

  private Type                   typeInt;
  private Type                   typeFloat;
  private Type                   typeDouble;
  private Type                   typeLong;
  private Type                   typeShort;
  private Type                   typeByte;
  private Type                   typeBoolean;
  private Type                   typeString;
  private Type                   typeFs;

  private Feature                akofUid;
  private Feature                akofInt;
  private Feature                akofFloat;
  private Feature                akofDouble;
  private Feature                akofLong;
  private Feature                akofShort;
  private Feature                akofByte;
  private Feature                akofBoolean;
  private Feature                akofString;
  private Feature                akofFs;

  private Feature                akofAint;
  private Feature                akofAfloat;
  private Feature                akofAdouble;
  private Feature                akofAlong;
  private Feature                akofAshort;
  private Feature                akofAbyte;
  private Feature                akofAboolean;
  private Feature                akofAstring;
  private Feature                akofAfs;

  private CASImpl                cas;
  private CASImpl                cas1;
  private CASImpl                cas2;
  private CASImpl                deserCas;
  private CASImpl                deltaCas;

  private TypeSystemImpl         ts;
  private List<FeatureStructure> lfs;
  private List<FeatureStructure> lfs2;

  private boolean                doPlain     = false;
  private boolean capture = false; // capture the serialized output
  private boolean                usePrevData = false;

  private int[]                  cas1FsIndexes;
  private int[]                  cas2FsIndexes;
  private MarkerImpl marker;

  public class CASTestSetup implements AnnotatorInitializer {

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

      akofUid = includeUid ? tsm.addFeature("akofUid", akof, typeInt) : null;
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

    void reinitTypeSystem(TypeSystemImpl tsm) {
      topType = tsm.getTopType();

      akof = tsm.refreshType(akof);

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

      akofUid = includeUid ? tsm.refreshFeature(akofUid) : null;
      akofInt = tsm.refreshFeature(akofInt);
      akofFs = tsm.refreshFeature(akofFs);
      akofFloat = tsm.refreshFeature(akofFloat);
      akofDouble = tsm.refreshFeature(akofDouble);
      akofLong = tsm.refreshFeature(akofLong);
      akofShort = tsm.refreshFeature(akofShort);
      akofByte = tsm.refreshFeature(akofByte);
      akofBoolean = tsm.refreshFeature(akofBoolean);
      akofString = tsm.refreshFeature(akofString);

      akofAint = tsm.refreshFeature(akofAint);
      akofAfs = tsm.refreshFeature(akofAfs);
      akofAfloat = tsm.refreshFeature(akofAfloat);
      akofAdouble = tsm.refreshFeature(akofAdouble);
      akofAlong = tsm.refreshFeature(akofAlong);
      akofAshort = tsm.refreshFeature(akofAshort);
      akofAbyte = tsm.refreshFeature(akofAbyte);
      akofAboolean = tsm.refreshFeature(akofAboolean);
      akofAstring = tsm.refreshFeature(akofAstring);
    }
  }

  public SerDesTest4() {
  }

  public void setUp() {
//    long seed = 1_449_257_605_347_913_923L;
    // long seed = 949_754_466_380_345_024L;
//     long seed = 6_761_039_426_734_540_557L;
//    long seed = 1_217_808_400_762_898_611L;
    random.setSeed(randomseed.nextLong());
//    System.out.format("SerDesTest4 setup RandomSeed: %,d%n", seed);

    try {
      CASTestSetup cts = new CASTestSetup();
      this.cas = (CASImpl) CASInitializer.initCas(cts, ts -> cts.reinitTypeSystem(ts));
      this.ts = (TypeSystemImpl) this.cas.getTypeSystem();
      this.cas2 = (CASImpl) CasCreationUtils.createCas(ts, null, null, null);
      deserCas = (CASImpl) CasCreationUtils.createCas(ts, null, null, null);
      deltaCas = (CASImpl) CasCreationUtils.createCas(ts, null, null, null);
      lfs = new ArrayList<FeatureStructure>();
      lfs2 = new ArrayList<>();
    } catch (Exception e) {
      assertTrue(false);
    }
  }

  public void tearDown() {
    this.cas = null;
    this.ts = null;
    deserCas = null;
    deltaCas = null;
    lfs = null;
  }

  /**
   * Make one of each kind of artifact, including arrays serialize to byte
   * stream, deserialize into new cas, compare
   */

  public void testAllKinds() {
    loadCas(lfs);
    verify("AllKinds");
  }

  /**
   * 1) create a base cas with some data
   * 1a) make a copy of the cas to that point
   *      Don't use CasCopier - it will reorder the fs's in the heap.
   *      Instead, use plain binary serialization / deserialization
   * 2) create the mark: cas.createMarker()
   * 3) add more cas data
   * 4) serialize with marker
   * 5) using copy, deserialize with marker
   * 6) check resulting cas = original in 4)
   * 
   * 
   */
  public void testDelta() {
    lfs.clear();
    loadCas(lfs);
    setupCas2ForDeltaSerialization();

    loadCas(lfs);
    cas = cas1;
    verifyDelta(marker, "Delta");
  }

  public void testDeltaWithRefsBelow() {
    lfs.clear();
    loadCas(lfs);
    setupCas2ForDeltaSerialization();

    FeatureStructure fs = cas.createFS(akof);
    if (includeUid) fs.setIntValue(akofUid, aint.getAndAdd(1));
    fs.setFeatureValue(akofFs, lfs2.get(0));
    ArrayFS fsafs = cas.createArrayFS(4);
    fsafs.set(1, lfs2.get(1));
    fsafs.set(2, lfs2.get(2));
    fsafs.set(3, lfs2.get(3));
    fs.setFeatureValue(akofAfs, fsafs);

    cas = cas1;
    verifyDelta(marker, "DeltaWithRefsBelow");
  }

  public void testDeltaWithMods() {
    lfs.clear();
    loadCas(lfs);
    
    setupCas2ForDeltaSerialization();

    FeatureStructure fs = cas.createFS(akof);
    if (includeUid) fs.setIntValue(akofUid, aint.getAndAdd(1));
    
    lfs2.get(0).setFeatureValue(akofFs, fs);

    cas = cas1;
    verifyDelta(marker, "DeltaWithMods");
  }
  
  /*
   * Variations to cover: all kinds of slots multiple sets of values test diffs
   * multiple orders (try reverse and random order)
   * 
   * Driver for random values pick among random and "interesting" edge case
   * values
   */
  public void testDeltaWithAllMods() throws IOException {

    for (int i = 0; i < 100; i++) {
      checkDeltaWithAllMods(random);
      tearDown();
      setUp();
    }
  }

  public void checkDeltaWithAllMods(Random r) {
    lfs.clear();
    makeRandomFss(7, lfs, r);
    loadCas(lfs);

    setupCas2ForDeltaSerialization();

    int belowMarkSize = lfs2.size();

    makeRandomFss(8, lfs2, r);

    int i = 0;
    for (FeatureStructure fs : lfs2) {
      if (((TOP) fs)._getTypeImpl() == akof) {
        if (((i++) % 2) == 0) {
          fs.setFeatureValue(akofFs, lfs2.get(r.nextInt(lfs2.size())));
        }
      }
    }

    makeRandomUpdatesBelowMark(lfs2, belowMarkSize, r);

    cas = cas1;
    verifyDelta(marker, null);

  }
  
  private void setupCas2ForDeltaSerialization() {
    cas1 = cas;
    serialize_then_deserialize_into_cas2();
  
    cas = cas2;
    lfs2.clear();
    cas2.walkReachablePlusFSsSorted(fs -> lfs2.add(fs), null, null, null);
  
    marker = (MarkerImpl) cas2.createMarker();
}   

  
  /**
   * Serialize (not delta) from cas , then deserialize into cas2 Done for side
   * effect of preparing cas to receive delta
   */
  private void serialize_then_deserialize_into_cas2() {
    try {
      ByteArrayInputStream bais;
      ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);

      BinaryCasSerDes4 bcs = new BinaryCasSerDes4(ts, false);

      if (doPlain) {
        Serialization.serializeCAS(cas, baos);
      } else {
        bcs.serialize(cas, baos);
      }

      bais = new ByteArrayInputStream(baos.toByteArray());
      BinaryCasSerDes bcsd_cas2 = cas2.getBinaryCasSerDes();
      bcsd_cas2.reinit(bais);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  public void testDeltaWithIndexMods() {
    lfs.clear();
    loadCas(lfs);
    setupCas2ForDeltaSerialization();
    int i = lfs2.size();

    loadCas(lfs2);

    cas.getIndexRepository().removeFS(lfs2.get(0));
    cas.getIndexRepository().removeFS(lfs2.get(1));
    cas.getIndexRepository().addFS(lfs2.get(1));  // should appear as reindexed

    cas.getIndexRepository().removeFS(lfs2.get(i));
    cas.getIndexRepository().removeFS(lfs2.get(i + 1));
    cas.getIndexRepository().addFS(lfs2.get(i + 1)); 

    cas = cas1;
    verifyDelta(marker, "DeltaWithIndexMods");
  }

  public void testArrayAux() {
    ArrayList<FeatureStructure> fsl = new ArrayList<FeatureStructure>();
    /**
     * Strings, non-array Long/Double:
     * Make equal items,
     * ser/deser, update one of the equal items, insure other not updated
     */
    FeatureStructure fsAt1 = newAkof(fsl);
    FeatureStructure fsAt2 = newAkof(fsl);
    cas.addFsToIndexes(fsAt1);
    cas.addFsToIndexes(fsAt2);

    createStringA(fsAt1, "at");
    createStringA(fsAt2, "at");
    verify("ArrayAuxStrings");

    FSIterator<FeatureStructure> it = deserCas.indexRepository.getAllIndexedFS(akof);
    FeatureStructure fsAt1d = it.next();
    FeatureStructure fsAt2d = it.next();
    StringArrayFS sa1 = (StringArrayFS) fsAt1d.getFeatureValue(akofAstring);
    StringArrayFS sa2 = (StringArrayFS) fsAt2d.getFeatureValue(akofAstring);
    sa1.set(1, "def");
    assertEquals(sa2.get(1), "abcat");
    assertEquals(sa1.get(1), "def");
    cas.reset();

    fsAt1 = newAkof(fsl);
    fsAt2 = newAkof(fsl);
    cas.addFsToIndexes(fsAt1);
    cas.addFsToIndexes(fsAt2);

    createLongA(fsAt1, 9);
    createLongA(fsAt2, 9);
    verify("ArrayAuxLongs");

    it = deserCas.indexRepository.getAllIndexedFS(akof);
    fsAt1d = it.next();
    fsAt2d = it.next();
    LongArrayFS la1 = (LongArrayFS) fsAt1d.getFeatureValue(akofAlong);
    LongArrayFS la2 = (LongArrayFS) fsAt2d.getFeatureValue(akofAlong);
    la1.set(2, 123L);
    assertEquals(la2.get(2), -45 + 9);
    assertEquals(la1.get(2), 123);
  }

  public void testWithOtherSerializer() throws IOException {
    doPlain = true;
    testDeltaWithMods();
    tearDown(); setUp();
    testDeltaWithRefsBelow();
    tearDown(); setUp();
    testDeltaWithAllMods();
    tearDown(); setUp();
    testAllKinds();
    tearDown(); setUp();
    testArrayAux();
  }

  public void testWithPrevGenerated() throws IOException {
    usePrevData = true;
    initReadSavedInts();
//    testDeltaWithMods(); // TODO temp skip delta until regen
    tearDown(); setUp();
//    testDeltaWithRefsBelow(); // TODO temp skip delta until regen
    tearDown(); setUp();
//    testDeltaWithAllMods();
    tearDown(); setUp();
//    testDeltaWithIndexMods();
    tearDown(); setUp();
    testAllKinds();
    tearDown(); setUp();
    testArrayAux();
    usePrevData = false;
    savedIntsStream.close();
  }

  public void captureGenerated() throws IOException {
    capture = true;
    initWriteSavedInts();
    setUp();
    testDeltaWithMods();
    tearDown(); setUp();
    testDeltaWithRefsBelow();
    tearDown(); setUp();
    testDeltaWithAllMods();
    tearDown(); setUp();
    testDeltaWithIndexMods();
    tearDown(); setUp();
    testAllKinds();
    tearDown(); setUp();
    testArrayAux();
    savedIntsOutStream.close();
  }

  /*******************************
   * Helper functions
   *******************************/

  private void createStringA(FeatureStructure fs, String x) {
    StringArrayFS strafs = cas.createStringArrayFS(5);
    strafs.set(3, null);
    strafs.set(2, "" + x);
    strafs.set(1, "abc" + x);
    strafs.set(0, "abc" + x);
    strafs.set(4, "def" + x);
    fs.setFeatureValue(akofAstring, strafs);
  }

  private void createIntA(FeatureStructure fs, int x) {
    IntArrayFS iafs = cas.createIntArrayFS(4 + x);
    iafs.set(0, Integer.MAX_VALUE - x);
    iafs.set(1, Integer.MIN_VALUE + x);
    iafs.set(2, 17 + 100 * x);
    fs.setFeatureValue(akofAint, iafs);
  }

  private void createFloatA(FeatureStructure fs, float x) {
    FloatArrayFS fafs = cas.createFloatArrayFS(6);
    fafs.set(0, Float.MAX_VALUE - x);
    // fafs.set(1, Float.MIN_NORMAL + x);
    fafs.set(2, Float.MIN_VALUE + x);
    fafs.set(3, Float.NaN);
    fafs.set(4, Float.NEGATIVE_INFINITY);
    fafs.set(5, Float.POSITIVE_INFINITY);
    fs.setFeatureValue(akofAfloat, fafs);
  }

  private void createDoubleA(FeatureStructure fs, double x) {
    DoubleArrayFS fafs = cas.createDoubleArrayFS(6);
    fafs.set(0, Double.MAX_VALUE - x);
    // fafs.set(1, Double.MIN_NORMAL + x);
    fafs.set(2, Double.MIN_VALUE + x);
    fafs.set(3, Double.NaN);
    fafs.set(4, Double.NEGATIVE_INFINITY);
    fafs.set(5, Double.POSITIVE_INFINITY);
    fs.setFeatureValue(akofAdouble, fafs);
  }

  private void createLongA(FeatureStructure fs, long x) {
    LongArrayFS lafs = cas.createLongArrayFS(4);
    lafs.set(0, Long.MAX_VALUE - x);
    lafs.set(1, Long.MIN_VALUE + x);
    lafs.set(2, -45 + x);
    fs.setFeatureValue(akofAlong, lafs);
  }

  private FeatureStructure newAkof(List<FeatureStructure> fsl) {
    FeatureStructure fs = cas.createFS(akof);
    if (includeUid) fs.setIntValue(akofUid, aint.getAndAdd(1));
    fsl.add(fs);
    return fs;
  }

  // make an instance of akof with all features set
  private FeatureStructure makeAkof(Random r) {
    FeatureStructure fs = cas.createFS(akof);
    if (includeUid) fs.setIntValue(akofUid, aint.getAndAdd(1));
    fs.setBooleanValue(akofBoolean, r.nextBoolean());
    fs.setByteValue(akofByte, (byte) r.nextInt());
    fs.setShortValue(akofShort, (short) r.nextInt());
    fs.setIntValue(akofInt, r.nextInt());
    fs.setFloatValue(akofFloat, r.nextFloat());
    fs.setLongValue(akofLong, r.nextLong());
    fs.setDoubleValue(akofDouble, r.nextDouble());
    fs.setStringValue(akofString, randomString(r));
    fs.setFeatureValue(akofFs, fs);

    fs.setFeatureValue(akofAint, randomIntA(r));
    fs.setFeatureValue(akofAfs, cas.createArrayFS(1));
    fs.setFeatureValue(akofAfloat, randomFloatA(r));
    fs.setFeatureValue(akofAdouble, randomDoubleA(r));
    fs.setFeatureValue(akofAlong, randomLongA(r));
    fs.setFeatureValue(akofAshort, randomShortA(r));
    fs.setFeatureValue(akofAbyte, randomByteA(r));
    fs.setFeatureValue(akofAboolean, cas.createBooleanArrayFS(2));
    fs.setFeatureValue(akofAstring, randomStringA(r));

    return fs;
  }

  private static final String[] stringValues = {
    "abc", "abcdef", null, "", "ghijklm", "a", "b"
  };

  private String randomString(Random r) {
    int i = r.nextInt(7);
    // if (i >= 7) {
    // System.out.println("debug");
    // }
    return stringValues[i];
  }

  private StringArrayFS randomStringA(Random r) {
    int length = r.nextInt(2) + 1;
    StringArrayFS fs = cas.createStringArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, stringValues[r.nextInt(stringValues.length)]);
    }
    return fs;
  }

  private IntArrayFS randomIntA(Random r) {
    int length = r.nextInt(2) + 1;
    IntArrayFS fs = cas.createIntArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, r.nextInt(101) - 50);
    }
    return fs;
  }

  private static final byte[] byteValues = {
    1, 0, -1, Byte.MAX_VALUE, Byte.MIN_VALUE, 9, -9  };

  private ByteArrayFS randomByteA(Random r) {
    int length = r.nextInt(2) + 1;
    ByteArrayFS fs = cas.createByteArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, byteValues[r.nextInt(byteValues.length)]);
    }
    return fs;
  }

  private static final long[] longValues = {
    1L, 0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, 11L, -11L  };

  private LongArrayFS randomLongA(Random r) {
    int length = r.nextInt(2) + 1;
    LongArrayFS fs = cas.createLongArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, longValues[r.nextInt(longValues.length)]);
    }
    return fs;
  }

  private static final short[] shortValues = {
    1, 0, -1, Short.MAX_VALUE, Short.MIN_VALUE, 22, -22  };

  private ShortArrayFS randomShortA(Random r) {
    int length = r.nextInt(2) + 1;
    ShortArrayFS fs = cas.createShortArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, shortValues[r.nextInt(shortValues.length)]);
    }
    return fs;
  }

  private static final double[] doubleValues = {
    1d, 0d, -1d, Double.MAX_VALUE, /*Double.MIN_NORMAL,*/ Double.MIN_VALUE, 33d, -33.33d  };

  private DoubleArrayFS randomDoubleA(Random r) {
    int length = r.nextInt(2) + 1;
    DoubleArrayFS fs = cas.createDoubleArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, doubleValues[r.nextInt(doubleValues.length)]);
    }
    return fs;
  }

  private static final float[] floatValues = {
    1f, 0f, -1f, Float.MAX_VALUE, /*Float.MIN_NORMAL,*/ Float.MIN_VALUE, 17f, -22.33f  };

  private FloatArrayFS randomFloatA(Random r) {
    int length = r.nextInt(2) + 1;
    FloatArrayFS fs = cas.createFloatArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, floatValues[r.nextInt(floatValues.length)]);
    }
    return fs;
  }

  private void makeRandomFss(int n, List<FeatureStructure> fss, Random r) {
    List<FeatureStructure> lfss = new ArrayList<FeatureStructure>();
    for (int i = 0; i < n; i++) {
      FeatureStructure fs = makeAkof(r);
      lfss.add(fs);
      fss.add(fs);
    }
    for (FeatureStructure fs : lfss) {
      fs.setFeatureValue(akofFs, lfss.get(r.nextInt(lfss.size())));
    }
  }

  private void loadCas(List<FeatureStructure> fsl) {
    FeatureStructure fs = newAkof(fsl);
    fs.setBooleanValue(akofBoolean, true);
    fs.setByteValue(akofByte, (byte) 109);
    fs.setShortValue(akofShort, (short) 23);
    fs.setIntValue(akofInt, 2345);
    fs.setFloatValue(akofFloat, 123f);
    fs.setLongValue(akofLong, 345L);
    fs.setDoubleValue(akofDouble, 334455.6677d);
    fs.setStringValue(akofString, "str1");
    fs.setFeatureValue(akofFs, fs);
    cas.addFsToIndexes(fs);
    FeatureStructure fs1 = fs;

    // extreme or unusual values
    fs = newAkof(fsl);
    fs.setBooleanValue(akofBoolean, false);
    fs.setByteValue(akofByte, Byte.MAX_VALUE);
    fs.setShortValue(akofShort, Short.MAX_VALUE);
    fs.setIntValue(akofInt, Integer.MAX_VALUE);
    fs.setFloatValue(akofFloat, Float.MAX_VALUE);
    fs.setLongValue(akofLong, Long.MAX_VALUE);
    fs.setDoubleValue(akofDouble, Double.MAX_VALUE);
    fs.setStringValue(akofString, "");
    fs.setFeatureValue(akofFs, fs1);
    cas.addFsToIndexes(fs);

    fs = newAkof(fsl);
    fs.setByteValue(akofByte, Byte.MIN_VALUE);
    fs.setShortValue(akofShort, Short.MIN_VALUE);
    fs.setIntValue(akofInt, Integer.MIN_VALUE);
    fs.setFloatValue(akofFloat, Float.MIN_VALUE);
    fs.setLongValue(akofLong, Long.MIN_VALUE);
    fs.setDoubleValue(akofDouble, Double.MIN_VALUE);
    fs.setStringValue(akofString, null);
    fs.setFeatureValue(akofFs, fs1);
    cas.addFsToIndexes(fs);
    FeatureStructure fs3 = fs;

    fs = newAkof(fsl);
    fs.setByteValue(akofByte, (byte) 0);
    fs.setShortValue(akofShort, (short) 0);
    fs.setIntValue(akofInt, 0);
    fs.setFloatValue(akofFloat, 0f);
    fs.setLongValue(akofLong, 0L);
    fs.setDoubleValue(akofDouble, 0D);
    fs.setFeatureValue(akofFs, fs1);
    cas.addFsToIndexes(fs);
    fs3.setFeatureValue(akofFs, fs); // make a forward ref
    FeatureStructure fs4 = fs;

    fs = newAkof(fsl);
    fs.setByteValue(akofByte, (byte) 1);
    fs.setShortValue(akofShort, (short) 1);
    fs.setIntValue(akofInt, 1);
    fs.setFloatValue(akofFloat, 1.0f);
    fs.setLongValue(akofLong, 1L);
    fs.setDoubleValue(akofDouble, 1.0D);
    cas.addFsToIndexes(fs);

    // fs = newAkof(fsl);
    // fs.setFloatValue(akofFloat, Float.MIN_NORMAL);
    // fs.setDoubleValue(akofDouble, Double.MIN_NORMAL);
    // cas.addFsToIndexes(fs);

    fs = newAkof(fsl);
    fs.setFloatValue(akofFloat, Float.MIN_VALUE);
    fs.setDoubleValue(akofDouble, Double.MIN_VALUE);
    cas.addFsToIndexes(fs);

    fs = newAkof(fsl);
    fs.setFloatValue(akofFloat, Float.NaN);
    fs.setDoubleValue(akofDouble, Double.NaN);
    cas.addFsToIndexes(fs);

    fs = newAkof(fsl);
    fs.setFloatValue(akofFloat, Float.POSITIVE_INFINITY);
    fs.setDoubleValue(akofDouble, Double.POSITIVE_INFINITY);
    cas.addFsToIndexes(fs);

    fs = newAkof(fsl);
    fs.setFloatValue(akofFloat, Float.NEGATIVE_INFINITY);
    fs.setDoubleValue(akofDouble, Double.NEGATIVE_INFINITY);
    cas.addFsToIndexes(fs);

    // test arrays
    fs = newAkof(fsl);
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

    fs = newAkof(fsl);
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

    fs = newAkof(fsl);
    cas.addFsToIndexes(fs);

    createIntA(fs, 0);

    // feature structure array
    ArrayFS fsafs = cas.createArrayFS(4);
    fsafs.set(1, fs8);
    fsafs.set(2, fs1);
    fsafs.set(3, fs4);
    fs.setFeatureValue(akofAfs, fsafs);

    createFloatA(fs, 0f);
    createDoubleA(fs, 0d);
    createLongA(fs, 0L);

    ShortArrayFS safs = cas.createShortArrayFS(4);
    safs.set(0, Short.MAX_VALUE);
    safs.set(1, Short.MIN_VALUE);
    safs.set(2, (short) -485);
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

    createStringA(fs, "");
    makeRandomFss(15, fsl, random);
  }

  private void verify(String fname) {
    try {
      BinaryCasSerDes4 bcs = new BinaryCasSerDes4(
          ts, false);
      ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
      ByteArrayInputStream bais;
      if (!usePrevData) {
        if (doPlain) {
          (new CASSerializer()).addCAS(cas, baos);
        } else {
          SerializationMeasures sm = bcs.serialize(cas, baos);
          if (null != sm) {
            System.out.println(sm);
          }
          if (capture) {
            writeout(baos, fname);
          }
        }
        bais = new ByteArrayInputStream(baos.toByteArray());
      } else {
        bais = new ByteArrayInputStream(readIn(fname));
      }
      deserCas.getBinaryCasSerDes().reinit(bais);
      assertTrue(bcs.getCasCompare().compareCASes(cas, deserCas));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Verifying deltas is somewhat tricky - the deserializing of a delta into a
   * CAS often requires that the base CAS has been serialized out first - in
   * order to set up the proper maps between serialized id forms and FSs -
   * example:
   * 
   * Caller does the following: create a CAS, fill 1 serialize , deserialize
   * into Cas2 modify Cas2 =========== This routine then does ========== delta
   * serialize Cas2 , deserialize into Cas1 compare Cas1 and Cas2
   * 
   * @param mark
   * @param fname
   */
  private void verifyDelta(MarkerImpl mark, String fname) {
    try {
      ByteArrayInputStream bais;
      ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);

      BinaryCasSerDes4 bcs = new BinaryCasSerDes4(ts, false);

      if (doPlain) {
        Serialization.serializeCAS(cas2, baos);
      } else {
        bcs.serialize(cas2, baos, mark);
      }
      if (capture) {
        writeout(baos, fname);
      }

      bais = (!usePrevData || fname == null) ? new ByteArrayInputStream(baos.toByteArray())
          : new ByteArrayInputStream(readIn(fname)); // use previous data

      BinaryCasSerDes bcsd_cas1 = cas.getBinaryCasSerDes();
      bcsd_cas1.reinit(bais);
      assertTrue(bcs.getCasCompare().compareCASes(cas, cas2));

      // verify indexed fs same; the order may be different so sort first
      getIndexes();

//      if (!Arrays.equals(cas1FsIndexes, cas2FsIndexes)) {
//        System.out.println("debug");
//      }
      assertTrue(Arrays.equals(cas1FsIndexes, cas2FsIndexes));

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void getIndexes() {
    cas1FsIndexes = getIndexInfo(cas1);
    cas2FsIndexes = getIndexInfo(cas2);
  }

  private int[] getIndexInfo(CASImpl cas) {
    int[] c = {2};
    IntVector iv = new IntVector();
    cas.walkReachablePlusFSsSorted(
        fs -> {   // filtered action (only on above mark)
               if (fs._getTypeImpl() == akof) { 
                 iv.add(includeUid ? fs.getIntValue(akofUid) : c[0]++);
               }
              }, 
        null,     // mark
        null,     // null or predicate to filter what gets included
        null);    // null or typeMapper to exclude things not in other ts
    int[] ia = iv.toArray();
    Arrays.sort(ia);
    return ia;
  }

  // try {
  // ByteArrayInputStream bais;
  // BinaryCasSerDes4 bcs = new BinaryCasSerDes4(ts, false);
  // if (!usePrevData || (fname == null)) {
  // ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
  // if (doPlain) {
  // Serialization.serializeCAS(cas, baos);
  // } else {
  // SerializationMeasures sm = bcs.serialize(cas, baos, mark);
  // if (null != sm) {
  // System.out.println(sm);
  // }
  // }
  //
  // if (capture) {
  // writeout(baos, fname);
  // }
  // bais = new ByteArrayInputStream(baos.toByteArray());
  // } else {
  // bais = new ByteArrayInputStream(readIn(fname));
  // }
  //
  // // prepare deltaCas to receive deserialized form
  // // do the serialize out to set up shared data structures needed when
  // deserializing
  // BinaryCasSerDes4 bcsDelta = new BinaryCasSerDes4(ts, false);
  //
  // ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
  // if (doPlain) {
  // Serialization.serializeCAS(deltaCas, baos);
  // } else {
  // SerializationMeasures sm = bcsDelta.serialize(deltaCas, baos);
  // }
  //
  // BinaryCasSerDes bcsd_deltaCas = deltaCas.getBinaryCasSerDes();
  // bcsd_deltaCas.reinit(bais);
  // assertTrue(bcs.getCasCompare().compareCASes(cas, deltaCas));
  //
  // // verify indexed fs same; the order may be different so sort first
  //
  // int[] fsIndexes1 = cas.getBinaryCasSerDes().getIndexedFSs(null);
  // int[] fsIndexes2 = bcsd_deltaCas.getIndexedFSs(null);
  // Arrays.sort(fsIndexes1);
  // Arrays.sort(fsIndexes2);
  // if (!Arrays.equals(fsIndexes1, fsIndexes2)) {
  // System.out.println("debug");
  // }
  // assertTrue(Arrays.equals(fsIndexes1, fsIndexes2));
  // } catch (IOException e) {
  // throw new RuntimeException(e);
  // }
  // }

  private void writeout(ByteArrayOutputStream baos, String fname) throws IOException {
    if (null == fname) {
      return;
    }
    BufferedOutputStream fos = setupFileOut(fname);
    fos.write(baos.toByteArray());
    fos.close();
  }

  private byte[] readIn(String fname) throws IOException {
    File f = new File("src/test/resources/SerDes4/" + fname + ".binary");
    int len = (int) f.length();
    byte[] buffer = new byte[len];
    BufferedInputStream inStream = 
      new BufferedInputStream(
          new FileInputStream(f));
    int br = inStream.read(buffer);
    if (br != len) {
      assertTrue(false);
    }
    inStream.close();
    return buffer;
  }

  private BufferedOutputStream setupFileOut(String fname) throws IOException {
    if (null == fname) {
      return null;
    }
    File dir = new File("src/test/resources/SerDes4/");
    if (!dir.exists()) {
      dir.mkdirs();
    }

    return
      new BufferedOutputStream(
        new FileOutputStream(
          new File("src/test/resources/SerDes4/" + fname + ".binary")));

  }

  private void initWriteSavedInts() {
    try {
      savedIntsOutStream = new OutputStreamWriter(setupFileOut("SavedInts"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void initReadSavedInts() {
    try {
      savedIntsStream = new BufferedReader(new FileReader("src/test/resources/SerDes4/SavedInts.binary"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeSavedInt(int i) {
    try {
      savedIntsOutStream.write(Integer.toString(i) + '\n');
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private int readNextSavedInt() {
    try {
      String s = savedIntsStream.readLine();
      return Integer.parseInt(s);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void makeRandomUpdatesBelowMark(List<FeatureStructure> fss, int belowMarkSize, Random r) {
    for (int i = 0; i < belowMarkSize; i++) {
      makeRandomUpdate(fss, i, r);
    }
  }

  private void makeRandomUpdate(List<FeatureStructure> fss, int fsi, Random r) {
    FeatureStructure fs = fss.get(fsi);
    int n = r.nextInt(3);
    for (int i = 0; i < n; i++) {
      if (((TOP) fs)._getTypeImpl() == akof) {
        switch (r.nextInt(26)) {
        case 0:
          fs.setBooleanValue(akofBoolean, r.nextBoolean());
          break;
        case 1:
          fs.setByteValue(akofByte, (byte) r.nextInt());
          break;
        case 2:
          fs.setShortValue(akofShort, (short) r.nextInt());
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
          fs.setFeatureValue(akofAint, randomIntA(r));
          break;
        case 10:
          fs.setFeatureValue(akofAfs, cas.createArrayFS(1));
          break;
        case 11:
          fs.setFeatureValue(akofAfloat, randomFloatA(r));
          break;
        case 12:
          fs.setFeatureValue(akofAdouble, randomDoubleA(r));
          break;
        case 13:
          fs.setFeatureValue(akofAlong, randomLongA(r));
          break;
        case 14:
          fs.setFeatureValue(akofAshort, randomShortA(r));
          break;
        case 15:
          fs.setFeatureValue(akofAbyte, randomByteA(r));
          break;
        case 16:
          fs.setFeatureValue(akofAboolean, cas.createBooleanArrayFS(2));
          break;
        case 17:
          fs.setFeatureValue(akofAstring, randomStringA(r));
          break;
        case 18: {
          IntArrayFS sfs = (IntArrayFS) fs.getFeatureValue(akofAint);
          if ((null != sfs) && (0 < sfs.size())) {
            sfs.set(0, 1);
          }
        }
          break;
        case 19: {
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
            sfs.set(0, (short) 1);
          }
        }
          break;
        case 24: {
          ByteArrayFS sfs = (ByteArrayFS) fs.getFeatureValue(akofAbyte);
          if ((null != sfs) && (0 < sfs.size())) {
            sfs.set(0, (byte) 1);
          }
        }
          break;
        case 25: {
          ArrayFS sfs = (ArrayFS) fs.getFeatureValue(akofAfs);
          if ((null != sfs) && (0 < sfs.size())) {
            sfs.set(0, fss.get(r.nextInt(lfs.size())));
          }
        }
          break;
        }
      }
    }
  }
  
  // disable to avoid accidentally overwriting test data
  static public void main(String[] args) throws IOException {
    (new SerDesTest4()).captureGenerated();
  }

}