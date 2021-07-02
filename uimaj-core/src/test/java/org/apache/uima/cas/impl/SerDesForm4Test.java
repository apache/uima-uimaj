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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
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
import org.apache.uima.util.AutoCloseableNoException;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.impl.SerializationMeasures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Serializer and Deserializer testing for version 4 of Binary Compressed
 * 
 * Has main method for creating resources to use in testing will update resources in SerDes4. If you
 * do this by mistake, just revert those resources.
 */
public class SerDesForm4Test extends SerDesTstCommon {

  // FIXME need to understand why includeUid is false, seems to be disabling some testing Nov 2016
  private static final boolean includeUid = false;
  private static final AtomicInteger aint = includeUid ? new AtomicInteger(0) : null;
  // private static final Random randomseed = new Random();
  // private static long seed =
  // 1_449_257_605_347_913_923L;
  // 949_754_466_380_345_024L;
  // 6_761_039_426_734_540_557L;
  // 1_217_808_400_762_898_611L;
  // randomseed.nextLong();
  // static {
  // System.out.format("SerDesTest4 RandomSeed: %,d%n", seed);
  // }

  private Type akof;
  private Type topType;

  private Feature akofUid;
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
  private CASImpl cas1;
  private CASImpl cas2;
  private CASImpl deserCas;
  private CASImpl deltaCas;

  private TypeSystemImpl ts;
  private List<FeatureStructure> lfs;
  private List<FeatureStructure> lfs2;

  private MarkerImpl marker;

  public class CASTestSetup implements AnnotatorInitializer {

    // @formatter:off
    /**
     * Type system
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
      // Add new types and features.
      topType = tsm.getTopType();

      akof = tsm.addType("akof", topType);

      akofUid = includeUid ? tsm.addFeature("akofUid", akof, tsm.getType(CAS.TYPE_NAME_INTEGER))
              : null;
      akofInt = tsm.addFeature("akofInt", akof, tsm.getType(CAS.TYPE_NAME_INTEGER));
      akofFs = tsm.addFeature("akofFs", akof, tsm.getType(CAS.TYPE_NAME_TOP));
      akofFloat = tsm.addFeature("akofFloat", akof, tsm.getType(CAS.TYPE_NAME_FLOAT));
      akofDouble = tsm.addFeature("akofDouble", akof, tsm.getType(CAS.TYPE_NAME_DOUBLE));
      akofLong = tsm.addFeature("akofLong", akof, tsm.getType(CAS.TYPE_NAME_LONG));
      akofShort = tsm.addFeature("akofShort", akof, tsm.getType(CAS.TYPE_NAME_SHORT));
      akofByte = tsm.addFeature("akofByte", akof, tsm.getType(CAS.TYPE_NAME_BYTE));
      akofBoolean = tsm.addFeature("akofBoolean", akof, tsm.getType(CAS.TYPE_NAME_BOOLEAN));
      akofString = tsm.addFeature("akofStr", akof, tsm.getType(CAS.TYPE_NAME_STRING));

      akofAint = tsm.addFeature("akofAint", akof, tsm.getType(CAS.TYPE_NAME_INTEGER_ARRAY));
      akofAfs = tsm.addFeature("akofAfs", akof, tsm.getType(CAS.TYPE_NAME_FS_ARRAY));
      akofAfloat = tsm.addFeature("akofAfloat", akof, tsm.getType(CAS.TYPE_NAME_FLOAT_ARRAY));
      akofAdouble = tsm.addFeature("akofAdouble", akof, tsm.getType(CAS.TYPE_NAME_DOUBLE_ARRAY));
      akofAlong = tsm.addFeature("akofAlong", akof, tsm.getType(CAS.TYPE_NAME_LONG_ARRAY));
      akofAshort = tsm.addFeature("akofAshort", akof, tsm.getType(CAS.TYPE_NAME_SHORT_ARRAY));
      akofAbyte = tsm.addFeature("akofAbyte", akof, tsm.getType(CAS.TYPE_NAME_BYTE_ARRAY));
      akofAboolean = tsm.addFeature("akofAboolean", akof, tsm.getType(CAS.TYPE_NAME_BOOLEAN_ARRAY));
      akofAstring = tsm.addFeature("akofAstring", akof, tsm.getType(CAS.TYPE_NAME_STRING_ARRAY));
    }

    @Override
    public void initIndexes(FSIndexRepositoryMgr irm, TypeSystem typeSystem) {
      // Nothing to do
    }

    void reinitTypeSystem(TypeSystemImpl tsm) {
      topType = tsm.getTopType();

      akof = tsm.refreshType(akof);

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

  @BeforeEach
  public void setUp() throws Exception {
    // long seed = 1_449_257_605_347_913_923L;
    // long seed = 949_754_466_380_345_024L;
    // long seed = 6_761_039_426_734_540_557L;
    // long seed = 1_217_808_400_762_898_611L;
    // random.setSeed(randomseed.nextLong());
    // System.out.format("SerDesTest4 setup RandomSeed: %,d%n", seed);

    CASTestSetup cts = new CASTestSetup();
    cas = (CASImpl) CASInitializer.initCas(cts, t -> cts.reinitTypeSystem(t));
    ts = (TypeSystemImpl) this.cas.getTypeSystem();
    cas2 = (CASImpl) CasCreationUtils.createCas(ts, null, null, null);
    deserCas = (CASImpl) CasCreationUtils.createCas(ts, null, null, null);
    deltaCas = (CASImpl) CasCreationUtils.createCas(ts, null, null, null);
    lfs = new ArrayList<>();
    lfs2 = new ArrayList<>();
  }

  @AfterEach
  public void tearDown() {
    cas = null;
    ts = null;
    deserCas = null;
    deltaCas = null;
    lfs = null;
  }

  /**
   * Make one of each kind of artifact, including arrays serialize to byte stream, deserialize into
   * new cas, compare
   */

  @Test
  public void testAllKinds() {
    loadCas(lfs);
    // uncomment this to test toString()
    // int i = 0;
    // for (TOP item : cas.getIndexRepository().getAllIndexedFS(TOP.class)) {
    // System.out.println(Integer.toString(i++) + ": " + item.toString());
    // }
    verify("AllKinds");
  }

  @Test
  public void testAllKindsV2() {
    try (AutoCloseableNoException a = cas.ll_enableV2IdRefs();
            AutoCloseableNoException b = deserCas.ll_enableV2IdRefs()) {
      loadCas(lfs);
      int id = newAkof(lfs)._id(); // an unreachable fs
      verify("AllKindsV2");
      assertEquals(id, deserCas.getLowLevelCAS().ll_getFSForRef(id)._id());
    }
  }

  // @formatter:off
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
   */
  // @formatter:on
  @Test
  public void testDelta() {
    lfs.clear();
    loadCas(lfs);
    setupCas2ForDeltaSerialization();

    loadCas(lfs);
    cas = cas1;
    verifyDelta(marker, "Delta");
  }

  @Test
  public void testDeltaWithRefsBelow() {
    lfs.clear();
    loadCas(lfs);
    setupCas2ForDeltaSerialization();

    FeatureStructure fs = createFS(cas, akof);
    if (includeUid) {
      fs.setIntValue(akofUid, aint.getAndAdd(1));
    }
    fs.setFeatureValue(akofFs, lfs2.get(0));
    ArrayFS<FeatureStructure> fsafs = createArrayFS(cas, 4);
    fsafs.set(1, lfs2.get(1));
    fsafs.set(2, lfs2.get(2));
    fsafs.set(3, lfs2.get(3));
    fs.setFeatureValue(akofAfs, fsafs);

    cas = cas1;
    verifyDelta(marker, "DeltaWithRefsBelow");
  }

  @Test
  public void testDeltaWithMods() {
    lfs.clear();
    loadCas(lfs);

    setupCas2ForDeltaSerialization();

    FeatureStructure fs = createFS(cas, akof);
    if (includeUid) {
      fs.setIntValue(akofUid, aint.getAndAdd(1));
    }

    lfs2.get(0).setFeatureValue(akofFs, fs);

    cas = cas1;
    verifyDelta(marker, "DeltaWithMods");
  }

  /*
   * Variations to cover: all kinds of slots multiple sets of values test diffs multiple orders (try
   * reverse and random order)
   * 
   * Driver for random values pick among random and "interesting" edge case values
   */
  @Test
  public void testDeltaWithAllMods() throws Exception {
    boolean prev = isKeep;
    isKeep = true;
    for (int i = 0; i < 100; i++) {
      checkDeltaWithAllMods1(random);
      tearDown();
      setUp();
    }
    isKeep = prev;
  }

  public void checkDeltaWithAllMods(Random r) {
    lfs.clear();
    makeRandomFss(7, lfs, r); // not added to indexes unless isKeep
    loadCas(lfs);

    setupCas2ForDeltaSerialization();

    int belowMarkSize = lfs2.size();

    makeRandomFss(8, lfs2, r);

    int i = 0;
    for (FeatureStructure fs : lfs2) {

      if (((i++) % 2) == 0) {
        int v = r.nextInt(lfs2.size());
        if (((TOP) fs)._getTypeImpl() == akof) {
          fs.setFeatureValue(akofFs, lfs2.get(v));
        }
      }
    }

    makeRandomUpdatesBelowMark(lfs2, belowMarkSize, r);

    cas = cas1;
    verifyDelta(marker, null);

  }

  // @formatter:off
  /**
   * Assuming that the delta serialization code has adequate support for
   * the use case of 
   *   a) create a CAS cas1
   *   b) fill it
   *   c) copy it via serialization/deserialization -> cas2
   *   d) add a mark to cas1
   *   e) delta serialize cas1  << not preceeded by a deserialization into cas1
   *   f) deserialize delta into cas2
   *   g) compare cas1 and 2.
   * @param r
   */
  // @formatter:on
  public void checkDeltaWithAllMods1(Random r) {
    lfs.clear();
    makeRandomFss(7, lfs, r); // not added to indexes unless isKeep
    loadCas(lfs);

    serialize_then_deserialize_into_cas2();

    // debug compare before mark
    // BinaryCasSerDes4 bcs = new BinaryCasSerDes4(ts, false);
    // assertTrue(CasCompare.compareCASes(cas, cas2));

    marker = (MarkerImpl) cas.createMarker();

    int belowMarkSize = lfs.size();

    makeRandomFss(8, lfs, r); // not indexed

    int i = 0;
    for (FeatureStructure fs : lfs) {

      if (((i++) % 2) == 0) {
        int v = r.nextInt(lfs.size());
        if (((TOP) fs)._getTypeImpl() == akof) {
          fs.setFeatureValue(akofFs, lfs.get(v));
        }
      }
    }

    makeRandomUpdatesBelowMark(lfs, belowMarkSize, r);

    verifyDelta1(marker, null);

  }

  private void setupCas2ForDeltaSerialization() {
    cas1 = cas;
    // lfs2.clear();
    // binaryCopyCas(cas1, cas2, lfs, lfs2); // don't use, cas copier reorders things in heap
    serialize_then_deserialize_into_cas2();

    cas = cas2;
    lfs2.clear();
    cas2.walkReachablePlusFSsSorted(fs -> lfs2.add(fs), null, null, null);

    marker = (MarkerImpl) cas2.createMarker();
  }

  /**
   * Serialize (not delta) from cas , then deserialize into cas2 Done for side effect of preparing
   * cas to receive delta
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

  @Test
  public void testDeltaWithIndexMods() {
    lfs.clear();
    loadCas(lfs);
    setupCas2ForDeltaSerialization();
    int i = lfs2.size();

    loadCas(lfs2);

    cas.getIndexRepository().removeFS(lfs2.get(0));
    cas.getIndexRepository().removeFS(lfs2.get(1));
    cas.getIndexRepository().addFS(lfs2.get(1)); // should appear as reindexed

    cas.getIndexRepository().removeFS(lfs2.get(i));
    cas.getIndexRepository().removeFS(lfs2.get(i + 1));
    cas.getIndexRepository().addFS(lfs2.get(i + 1));

    cas = cas1;
    verifyDelta(marker, "DeltaWithIndexMods");
  }

  @Test
  public void testArrayAux() {
    ArrayList<FeatureStructure> fsl = new ArrayList<>();
    // @formatter:off
    /**
     * Strings, non-array Long/Double:
     * Make equal items,
     * ser/deser, update one of the equal items, insure other not updated
     */
    // @formatter:on
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

  @Test
  public void testWithOtherSerializer() throws Exception {
    doPlain = true;
    testDeltaWithMods();
    tearDown();

    setUp();
    testDeltaWithRefsBelow();
    tearDown();

    setUp();
    testDeltaWithAllMods();
    tearDown();

    setUp();
    testAllKinds();
    tearDown();

    setUp();
    testArrayAux();
  }

  // @formatter:off
  /**
   * See if can read Version 2 serialized things and de-serialize them
   * 
   * Note: Delta won't work unless the previous v2 test case indexed or ref'd all the FSs,
   *   because otherwise, some FSs will be "deleted" by the modeling V3 does for the CAS
   *   layout because they're not findable during scanning, and therefore, delta mods won't be correct.
   */
  // @formatter:on
  @Test
  public void testWithPrevGenerated() throws Exception {
    isKeep = true; // forces all akof fss to be indexed
    usePrevData = true;
    initReadSavedInts();
    // tests must be in same order as v2 tests
    tstPrevGenV2(this::testDeltaWithMods);
    tstPrevGenV2(this::testDeltaWithRefsBelow);

    // this test does the delta serialization using v3, so we expect it to work
    tearDown();
    setUp();
    testDeltaWithAllMods();

    tstPrevGenV2(this::testDeltaWithIndexMods);

    isKeep = false; // for next test
    tearDown();
    setUp();
    testAllKinds(); // works, is not delta

    tearDown();
    setUp();
    testArrayAux(); // works, is not delta

    usePrevData = false;
    isKeep = false;
    savedIntsStream.close();
  }

  private void tstPrevGenV2(Runnable m) throws Exception {
    tearDown();
    setUp();

    assertThatExceptionOfType(CASRuntimeException.class).isThrownBy(() -> m.run())
            .satisfies(e -> e.hasMessageKey(CASRuntimeException.DESERIALIZING_V2_DELTA_V3));
  }

  public void captureGenerated() throws Exception {
    capture = true;
    initWriteSavedInts();

    setUp();
    testDeltaWithMods();
    tearDown();

    setUp();
    testDeltaWithRefsBelow();
    tearDown();

    setUp();
    testDeltaWithAllMods();
    tearDown();

    setUp();
    testDeltaWithIndexMods();
    tearDown();

    setUp();
    testAllKinds();
    tearDown();

    setUp();
    testArrayAux();

    savedIntsOutStream.close();
  }

  /*******************************
   * Helper functions
   *******************************/

  private void createStringA(FeatureStructure fs, String x) {
    StringArrayFS strafs = createStringArrayFS(cas, 5);
    strafs.set(3, null);
    strafs.set(2, "" + x);
    strafs.set(1, "abc" + x);
    strafs.set(0, "abc" + x);
    strafs.set(4, "def" + x);
    fs.setFeatureValue(akofAstring, strafs);
  }

  private void createIntA(FeatureStructure fs, int x) {
    IntArrayFS iafs = createIntArrayFS(cas, 4 + x);
    iafs.set(0, Integer.MAX_VALUE - x);
    iafs.set(1, Integer.MIN_VALUE + x);
    iafs.set(2, 17 + 100 * x);
    fs.setFeatureValue(akofAint, iafs);
  }

  private void createFloatA(FeatureStructure fs, float x) {
    FloatArrayFS fafs = createFloatArrayFS(cas, 6);
    fafs.set(0, Float.MAX_VALUE - x);
    // fafs.set(1, Float.MIN_NORMAL + x);
    fafs.set(2, Float.MIN_VALUE + x);
    fafs.set(3, Float.NaN);
    fafs.set(4, Float.NEGATIVE_INFINITY);
    fafs.set(5, Float.POSITIVE_INFINITY);
    fs.setFeatureValue(akofAfloat, fafs);
  }

  private void createDoubleA(FeatureStructure fs, double x) {
    DoubleArrayFS fafs = createDoubleArrayFS(cas, 6);
    fafs.set(0, Double.MAX_VALUE - x);
    // fafs.set(1, Double.MIN_NORMAL + x);
    fafs.set(2, Double.MIN_VALUE + x);
    fafs.set(3, Double.NaN);
    fafs.set(4, Double.NEGATIVE_INFINITY);
    fafs.set(5, Double.POSITIVE_INFINITY);
    fs.setFeatureValue(akofAdouble, fafs);
  }

  private void createLongA(FeatureStructure fs, long x) {
    LongArrayFS lafs = createLongArrayFS(cas, 4);
    lafs.set(0, Long.MAX_VALUE - x);
    lafs.set(1, Long.MIN_VALUE + x);
    lafs.set(2, -45 + x);
    fs.setFeatureValue(akofAlong, lafs);
  }

  // copy FSs from c1 to c2, including indexes.
  // and make copies (if not already done) of FSs
  // that may be unreachable.
  //
  // DO NOT USE because
  // CasCopier will reorder FSs in the heap, and delta testing is depending on that
  // private void binaryCopyCas(
  // CASImpl c1,
  // CASImpl c2,
  // List<FeatureStructure> fss,
  // List<FeatureStructure> copies) {
  // CasCopier cc = new CasCopier(c1, c2);
  // for (FeatureStructure fs : fss) {
  // copies.add(cc.copyFs(fs));
  // }
  //
  // // this next copies any referenced items not indexed,
  // // and adds items to the index
  // c1.forAllViews(v -> cc.copyCasView(v, true));
  // }

  private FeatureStructure newAkof(List<FeatureStructure> fsl) {
    FeatureStructure fs = createFS(cas, akof);
    if (includeUid) {
      fs.setIntValue(akofUid, aint.getAndAdd(1));
    }
    fsl.add(fs);
    return fs;
  }

  // make an instance of akof with all features set
  // ** NOT added to index unless isKeep
  private FeatureStructure makeAkof(Random r) {
    FeatureStructure fs = createFS(cas, akof);
    if (includeUid) {
      fs.setIntValue(akofUid, aint.getAndAdd(1));
    }
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
    fs.setFeatureValue(akofAfs, createArrayFS(cas, 1));
    fs.setFeatureValue(akofAfloat, randomFloatA(r));
    fs.setFeatureValue(akofAdouble, randomDoubleA(r));
    fs.setFeatureValue(akofAlong, randomLongA(r));
    fs.setFeatureValue(akofAshort, randomShortA(r));
    fs.setFeatureValue(akofAbyte, randomByteA(r));
    fs.setFeatureValue(akofAboolean, createBooleanArrayFS(cas, 2));
    fs.setFeatureValue(akofAstring, randomStringA(r));

    if (isKeep) {
      ((TOP) fs).addToIndexes();
    }
    return fs;
  }

  private static final String[] stringValues = { "abc", "abcdef", null, "", "ghijklm", "a", "b" };

  private String randomString(Random r) {
    int i = r.nextInt(7);
    // if (i >= 7) {
    // System.out.println("debug");
    // }
    return stringValues[i];
  }

  private StringArrayFS randomStringA(Random r) {
    int length = r.nextInt(2) + 1;
    StringArrayFS fs = createStringArrayFS(cas, length);
    for (int i = 0; i < length; i++) {
      fs.set(i, stringValues[r.nextInt(stringValues.length)]);
    }
    return fs;
  }

  private IntArrayFS randomIntA(Random r) {
    int length = r.nextInt(2) + 1;
    IntArrayFS fs = createIntArrayFS(cas, length);
    for (int i = 0; i < length; i++) {
      fs.set(i, r.nextInt(101) - 50);
    }
    return fs;
  }

  private static final byte[] byteValues = { 1, 0, -1, Byte.MAX_VALUE, Byte.MIN_VALUE, 9, -9 };

  private ByteArrayFS randomByteA(Random r) {
    int length = r.nextInt(2) + 1;
    ByteArrayFS fs = createByteArrayFS(cas, length);
    for (int i = 0; i < length; i++) {
      int bvidx = r.nextInt(byteValues.length);
      fs.set(i, byteValues[bvidx]);
    }
    return fs;
  }

  private static final long[] longValues = { 1L, 0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, 11L,
      -11L };

  private LongArrayFS randomLongA(Random r) {
    int length = r.nextInt(2) + 1;
    LongArrayFS fs = createLongArrayFS(cas, length);
    for (int i = 0; i < length; i++) {
      fs.set(i, longValues[r.nextInt(longValues.length)]);
    }
    return fs;
  }

  private static final short[] shortValues = { 1, 0, -1, Short.MAX_VALUE, Short.MIN_VALUE, 22,
      -22 };

  private ShortArrayFS randomShortA(Random r) {
    int length = r.nextInt(2) + 1;
    ShortArrayFS fs = createShortArrayFS(cas, length);
    for (int i = 0; i < length; i++) {
      fs.set(i, shortValues[r.nextInt(shortValues.length)]);
    }
    return fs;
  }

  private static final double[] doubleValues = { 1d, 0d, -1d, Double.MAX_VALUE,
      /* Double.MIN_NORMAL, */ Double.MIN_VALUE, 33d, -33.33d };

  private DoubleArrayFS randomDoubleA(Random r) {
    int length = r.nextInt(2) + 1;
    DoubleArrayFS fs = createDoubleArrayFS(cas, length);
    for (int i = 0; i < length; i++) {
      fs.set(i, doubleValues[r.nextInt(doubleValues.length)]);
    }
    return fs;
  }

  private static final float[] floatValues = { 1f, 0f, -1f, Float.MAX_VALUE,
      /* Float.MIN_NORMAL, */ Float.MIN_VALUE, 17f, -22.33f };

  private FloatArrayFS randomFloatA(Random r) {
    int length = r.nextInt(2) + 1;
    FloatArrayFS fs = createFloatArrayFS(cas, length);
    for (int i = 0; i < length; i++) {
      fs.set(i, floatValues[r.nextInt(floatValues.length)]);
    }
    return fs;
  }

  /**
   * Make a bunch of Akof fs's, not indexed, linked randomly to each other. In v3, these might be
   * dropped due to no refs, no indexing
   * 
   * @param n
   *          -
   * @param fss
   *          -
   * @param r
   *          -
   */
  private void makeRandomFss(int n, List<FeatureStructure> fss, Random r) {
    List<FeatureStructure> lfss = new ArrayList<>();
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
    fs.setFeatureValue(akofAint, createIntArrayFS(cas, 0));
    fs.setFeatureValue(akofAfs, createArrayFS(cas, 0));
    fs.setFeatureValue(akofAfloat, createFloatArrayFS(cas, 0));
    fs.setFeatureValue(akofAdouble, createDoubleArrayFS(cas, 0));
    fs.setFeatureValue(akofAlong, createLongArrayFS(cas, 0));
    fs.setFeatureValue(akofAshort, createShortArrayFS(cas, 0));
    fs.setFeatureValue(akofAbyte, createByteArrayFS(cas, 0));
    fs.setFeatureValue(akofAboolean, createBooleanArrayFS(cas, 0));
    fs.setFeatureValue(akofAstring, createStringArrayFS(cas, 0));
    cas.addFsToIndexes(fs);
    FeatureStructure fs8 = fs;

    fs = newAkof(fsl);
    fs.setFeatureValue(akofAint, createIntArrayFS(cas, 2));
    fs.setFeatureValue(akofAfs, createArrayFS(cas, 2));
    fs.setFeatureValue(akofAfloat, createFloatArrayFS(cas, 2));
    fs.setFeatureValue(akofAdouble, createDoubleArrayFS(cas, 2));
    fs.setFeatureValue(akofAlong, createLongArrayFS(cas, 2));
    fs.setFeatureValue(akofAshort, createShortArrayFS(cas, 2));
    fs.setFeatureValue(akofAbyte, createByteArrayFS(cas, 2));
    fs.setFeatureValue(akofAboolean, createBooleanArrayFS(cas, 2));
    fs.setFeatureValue(akofAstring, createStringArrayFS(cas, 2));
    cas.addFsToIndexes(fs);

    fs = newAkof(fsl);
    cas.addFsToIndexes(fs);

    createIntA(fs, 0);

    // feature structure array
    ArrayFS fsafs = createArrayFS(cas, 4);
    fsafs.set(1, fs8);
    fsafs.set(2, fs1);
    fsafs.set(3, fs4);
    fs.setFeatureValue(akofAfs, fsafs);

    createFloatA(fs, 0f);
    createDoubleA(fs, 0d);
    createLongA(fs, 0L);

    ShortArrayFS safs = createShortArrayFS(cas, 4);
    safs.set(0, Short.MAX_VALUE);
    safs.set(1, Short.MIN_VALUE);
    safs.set(2, (short) -485);
    fs.setFeatureValue(akofAshort, safs);

    ByteArrayFS bafs = createByteArrayFS(cas, 4);
    bafs.set(0, Byte.MAX_VALUE);
    bafs.set(1, Byte.MIN_VALUE);
    bafs.set(2, (byte) 33);
    fs.setFeatureValue(akofAbyte, bafs);

    BooleanArrayFS booafs = createBooleanArrayFS(cas, 4);
    booafs.set(0, true);
    booafs.set(1, false);
    fs.setFeatureValue(akofAboolean, booafs);

    createStringA(fs, "");
    makeRandomFss(15, fsl, random); // not added to indexes unless isKeep
  }

  private void verify(String fname) {
    try {
      BinaryCasSerDes4 bcs = new BinaryCasSerDes4(ts, false);
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
      assertTrue(CasCompare.compareCASes(cas, deserCas));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // @formatter:off
  /**
   * Verifying deltas is somewhat tricky - the deserializing of a delta into a
   * CAS may (?) require that the base CAS has been serialized out first - in
   * order to set up the proper maps between serialized id forms and FSs -
   * example:
   * 
   * Caller does the following: 
   *   create a CAS, 
   *   fill 1 serialize, 
   *   deserialize into Cas2 
   *   modify Cas2 
   *   =========== This routine then does ========== 
   *   delta serialize Cas2, 
   *   deserialize into Cas1 compare Cas1 and Cas2
   * 
   * @param mark
   * @param fname
   */
  // @formatter:on
  private void verifyDelta(MarkerImpl mark, String fname) {
    try {
      ByteArrayInputStream bais;
      ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);

      BinaryCasSerDes4 bcs = new BinaryCasSerDes4(ts, false);

      if (doPlain) {
        Serialization.serializeCAS(cas2, baos);
      } else {
        bcs.serialize(cas2, baos, mark);
        if (capture) {
          writeout(baos, fname);
        }
      }

      bais = (!usePrevData || fname == null) ? new ByteArrayInputStream(baos.toByteArray())
              : new ByteArrayInputStream(readIn(fname)); // use previous data

      BinaryCasSerDes bcsd_cas1 = cas.getBinaryCasSerDes();
      bcsd_cas1.reinit(bais);
      assertThat(CasCompare.compareCASes(cas, cas2)).isTrue();

      // verify indexed fs same; the order may be different so sort first
      // currently seems broken... not comparing anything of use
      assertThat(getIndexInfo(cas1)).containsExactly(getIndexInfo(cas2));

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // @formatter:off
  /**
   * This version closer to v2 version
   *   delta serialize of "cas"
   *   deserialize into "cas2"
   *   compare cas and cas2
   * @param mark -
   * @param fname -
   */
  // @formatter:on
  private void verifyDelta1(MarkerImpl mark, String fname) {
    try {
      cas1 = cas;
      ByteArrayInputStream bais;
      BinaryCasSerDes4 bcs = new BinaryCasSerDes4(ts, false);
      // skip serialization step if we're going to read prev serialized value
      if (!usePrevData || fname == null) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);

        if (doPlain) {
          Serialization.serializeCAS(cas, baos);
        } else {
          bcs.serialize(cas, baos, mark);
        }
        if (capture) {
          writeout(baos, fname);
        }
        bais = new ByteArrayInputStream(baos.toByteArray());
      } else {
        bais = new ByteArrayInputStream(readIn(fname));
      }

      BinaryCasSerDes bcsd_cas2 = cas2.getBinaryCasSerDes();
      bcsd_cas2.reinit(bais);
      assertThat(CasCompare.compareCASes(cas, cas2)).isTrue();

      // verify indexed fs same; the order may be different so sort first
      // currently seems broken... not comparing anything of use
      assertThat(getIndexInfo(cas1)).containsExactly(getIndexInfo(cas2));

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // seems like an invalid thing since includeUid is false. Nov 2016
  private int[] getIndexInfo(CASImpl cas) {
    int[] c = { 2 };
    IntVector iv = new IntVector();
    cas.walkReachablePlusFSsSorted(fs -> { // filtered action (only on above mark)
      if (fs._getTypeImpl() == akof) {
        iv.add(includeUid ? fs.getIntValue(akofUid) : c[0]++);
      }
    }, null, // mark
            null, // null or predicate to filter what gets included
            null); // null or typeMapper to exclude things not in other ts
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
  // assertTrue(CasCompare.compareCASes(cas, deltaCas));
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
            fs.setFeatureValue(akofAfs, createArrayFS(cas, 1));
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
            fs.setFeatureValue(akofAboolean, createBooleanArrayFS(cas, 2));
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

  private FeatureStructure createFS(CAS cas, Type type) {
    if (isKeep) {
      LowLevelCAS llCas = cas.getLowLevelCAS();
      return llCas.ll_getFSForRef(llCas.ll_createFS(((TypeImpl) type).getCode()));
    }
    return cas.createFS(type);
  }

  private TOP createArray(CAS cas, Type type, int length) {
    if (isKeep) {
      LowLevelCAS llCas = cas.getLowLevelCAS();
      return llCas.ll_getFSForRef(llCas.ll_createArray(((TypeImpl) type).getCode(), length));
    }
    return ((CASImpl) cas).createArray((TypeImpl) type, length);
  }

  private ArrayFS<FeatureStructure> createArrayFS(CAS c, int length) {
    return (ArrayFS<FeatureStructure>) createArray(c, ((CASImpl) c).getTypeSystemImpl().fsArrayType,
            length);
  }

  private StringArrayFS createStringArrayFS(CAS c, int length) {
    return (StringArrayFS) createArray(c, ((CASImpl) c).getTypeSystemImpl().stringArrayType,
            length);
  }

  private IntArrayFS createIntArrayFS(CAS c, int length) {
    return (IntArrayFS) createArray(c, ((CASImpl) c).getTypeSystemImpl().intArrayType, length);
  }

  private FloatArrayFS createFloatArrayFS(CAS c, int length) {
    return (FloatArrayFS) createArray(c, ((CASImpl) c).getTypeSystemImpl().floatArrayType, length);
  }

  private DoubleArrayFS createDoubleArrayFS(CAS c, int length) {
    return (DoubleArrayFS) createArray(c, ((CASImpl) c).getTypeSystemImpl().doubleArrayType,
            length);
  }

  private LongArrayFS createLongArrayFS(CAS c, int length) {
    return (LongArrayFS) createArray(c, ((CASImpl) c).getTypeSystemImpl().longArrayType, length);
  }

  private ShortArrayFS createShortArrayFS(CAS c, int length) {
    return (ShortArrayFS) createArray(c, ((CASImpl) c).getTypeSystemImpl().shortArrayType, length);
  }

  private ByteArrayFS createByteArrayFS(CAS c, int length) {
    return (ByteArrayFS) createArray(c, ((CASImpl) c).getTypeSystemImpl().byteArrayType, length);
  }

  private BooleanArrayFS createBooleanArrayFS(CAS c, int length) {
    return (BooleanArrayFS) createArray(c, ((CASImpl) c).getTypeSystemImpl().booleanArrayType,
            length);
  }

  @Override
  protected String getTestRootName() {
    return "SerDes4";
  }
  // disable to avoid accidentally overwriting test data
  // static public void main(String[] args) throws IOException {
  // (new SerDesTest4()).captureGenerated();
  // }

}
