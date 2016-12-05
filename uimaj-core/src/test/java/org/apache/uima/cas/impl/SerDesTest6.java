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

import static org.apache.uima.cas.impl.SerDesTest6.TypeSystems.EqTwoTypes;
import static org.apache.uima.cas.impl.SerDesTest6.TypeSystems.OneType;
import static org.apache.uima.cas.impl.SerDesTest6.TypeSystems.OneTypeSubsetFeatures;
import static org.apache.uima.cas.impl.SerDesTest6.TypeSystems.TwoTypes;
import static org.apache.uima.cas.impl.SerDesTest6.TypeSystems.TwoTypesNoFeatures;
import static org.apache.uima.cas.impl.SerDesTest6.TypeSystems.TwoTypesSubsetFeatures;
import static org.apache.uima.cas.impl.SerDesTest6.Types.Akof1;
import static org.apache.uima.cas.impl.SerDesTest6.Types.Akof2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.impl.SerializationMeasures;

/**
 * Serializer and Deserializer testing
 * 
 * 
 */
public class SerDesTest6 extends SerDesTstCommon {

  enum TypeSystems {
    TwoTypes, EqTwoTypes, OneType, TwoTypesSubsetFeatures, OneTypeSubsetFeatures, TwoTypesNoFeatures,
  }
  
  enum Types {
    Akof1, Akof2,
  }

  private final String testDocText = "test document text";
  private CASImpl remoteCas;

  private List<FeatureStructure> lfs;
  
  private boolean doPlain = false;
//  private TypeSystemMgr tsmSrc, tsmTgt;
  private TTypeSystem mSrc;
  private CASImpl casSrc;
  private TTypeSystem[] alternateTTypeSystems;
  
  public class CASTestSetup implements AnnotatorInitializer {

    
    public TTypeSystem m; 
    public TypeSystemMgr tsm;
    final TypeSystems kind;
    
    public CASTestSetup(TypeSystems kind) {
      this.kind = kind;
    }
    
    // TwoTypes, EqTwoTypes, OneType, TwoTypesSubsetFeatures,
    // OneTypeSubsetFeatures, NoFeatures,
    public void initTypeSystem(TypeSystemMgr tsm) {
      if (kind == EqTwoTypes) {
        throw new RuntimeException();
      }
    
      this.tsm = tsm;
      m = new TTypeSystem(tsm, kind);
      addBuiltins();
      
      m.addType(Akof1.name(),"Top");
      if (kind != TwoTypesNoFeatures) {
        for (String fn : featureNameRoots) {
          if (kind != OneTypeSubsetFeatures ||
              random.nextInt(3) < 2) { 
            m.add(Akof1, fn);
          }
        }
      }
      
      switch (kind) {
      case TwoTypes: 
      case TwoTypesSubsetFeatures: 
      case TwoTypesNoFeatures:
        m.addType(Akof2.name(),"Top");
        if (kind != TwoTypesNoFeatures) {
          for (String fn : featureNameRoots) {
            if (kind != TwoTypesSubsetFeatures ||
                random.nextInt(3) < 2) { 
            m.add(Akof2, fn);
            }
          }
        }
        break;
      default: // skip the other cases
      } // end of switch
    }
    
    void addBuiltins() {
   // Add new types and features.
      m.addType(tsm.getTopType(), "Top");
      
      m.addType(tsm.getType(CAS.TYPE_NAME_INTEGER_ARRAY), "Aint");
      m.addType(tsm.getType(CAS.TYPE_NAME_FS_ARRAY), "Afs");
      m.addType(tsm.getType(CAS.TYPE_NAME_FLOAT_ARRAY), "Afloat");
      m.addType(tsm.getType(CAS.TYPE_NAME_DOUBLE_ARRAY), "Adouble");
      m.addType(tsm.getType(CAS.TYPE_NAME_LONG_ARRAY), "Along");
      m.addType(tsm.getType(CAS.TYPE_NAME_SHORT_ARRAY), "Ashort");
      m.addType(tsm.getType(CAS.TYPE_NAME_BYTE_ARRAY), "Abyte");
      m.addType(tsm.getType(CAS.TYPE_NAME_BOOLEAN_ARRAY), "Aboolean");
      m.addType(tsm.getType(CAS.TYPE_NAME_STRING_ARRAY), "Astring");
      
      m.addType(tsm.getType(CAS.TYPE_NAME_INTEGER), "Int");
      m.addType(tsm.getType(CAS.TYPE_NAME_FLOAT), "Float");
      m.addType(tsm.getType(CAS.TYPE_NAME_DOUBLE), "Double");
      m.addType(tsm.getType(CAS.TYPE_NAME_LONG), "Long");
      m.addType(tsm.getType(CAS.TYPE_NAME_SHORT), "Short");
      m.addType(tsm.getType(CAS.TYPE_NAME_BYTE), "Byte");
      m.addType(tsm.getType(CAS.TYPE_NAME_BOOLEAN), "Boolean");
      m.addType(tsm.getType(CAS.TYPE_NAME_STRING), "String");
      m.addType(tsm.getType(CAS.TYPE_NAME_TOP), "Fs");
    }

    public void initIndexes(FSIndexRepositoryMgr irm, TypeSystem ts) {
    }
  }
  static final List<String> featureNameRoots = Arrays.asList(new String[] { 
    "Int", "Fs", "Float", "Double", "Long", "Short", "Byte", "Boolean", "String", 
    "Aint", "Afs", "Afloat", "Adouble", "Along", "Ashort", "Abyte", "Aboolean", "Astring"});
  
  static class TTypeSystem {
    final TypeSystems kind;
    TypeSystemMgr tsm;
    Feature[][] featureTable = new Feature[Types.values().length][featureNameRoots.size()];
    Map<String, Type> mapString2Type = new HashMap<String, Type>();
    public TypeSystemImpl ts;
    public CASImpl cas;  // the Cas setup as part of initialization                                                                    // the
    
    public TTypeSystem(TypeSystemMgr tsm, TypeSystems kind) {
      this.tsm = tsm;
      this.kind = kind;
      this.ts = (TypeSystemImpl) tsm;
    }

    void addType(Type type, String shortName) {
      mapString2Type.put(shortName, type);
    }
    
    void addType(String type, String superType) {
      addType(tsm.addType(type, getType(superType)), type);
    }
    
    Type getType(String shortName) {
      return mapString2Type.get(shortName);
    }
    
    Type getType(Types type) {
      return getType(type.name());
    }

    void add(Type type, String featNameRoot) {
      String typeName = type.getShortName();
      int i2 = featureNameRoots.indexOf(featNameRoot);
      featureTable[Types.valueOf(typeName).ordinal()][i2] = tsm.addFeature(typeName + featNameRoot,
          type, mapString2Type.get(featNameRoot));
    }
    
    void add(Types typeKind, String featNameRoot) {
      add(getType(typeKind.name()), featNameRoot);
    }
    
    Feature getFeature(Types typeKind, String featNameRoot) {
      return featureTable[typeKind.ordinal()][featureNameRoots.indexOf(featNameRoot)];
    }
    
    Feature getFeature(FeatureStructure fs, String featNameRoot) {
      Type t = fs.getType();
      return getFeature(Types.valueOf(t.getShortName()), featNameRoot);
    }

    void updateAfterCommit() { // needed for v3 only, but doesn't hurt for v2
      ts = cas.getTypeSystemImpl();
      tsm = ts;
      for (String typename : mapString2Type.keySet()) {
        TypeImpl ti = (TypeImpl) ts.getType(typename);
        mapString2Type.put(typename, ti);
      }

      for (Types typeKind : Types.values()) {
        Type ti = tsm.getType(typeKind.name());
        if (ti != null) {
          Feature[] features = featureTable[typeKind.ordinal()];
          for (int i = 0; i < features.length; i++) {
            features[i] = ti.getFeatureByBaseName(ti.getShortName() + featureNameRoots.get(i));
          }
        }
      }
    }
  }
  
  public TTypeSystem setupTTypeSystem(TypeSystems kind) {
    if (kind == EqTwoTypes) {
      TTypeSystem m = new TTypeSystem(mSrc.tsm, kind);
      m.cas = mSrc.cas;
      // m.ts = mSrc.cas.getTypeSystemImpl();
      return mSrc;
    }
    CASTestSetup setup = new CASTestSetup(kind);
    CASImpl cas = (CASImpl) CASInitializer.initCas(setup);
    TTypeSystem m = setup.m;
    m.cas = cas;
    m.updateAfterCommit();
    return m;
  }
  
  public void setUp() {
    mSrc = setupTTypeSystem(TwoTypes);
    casSrc = mSrc.cas;
    final TypeSystems[] tss = TypeSystems.values();
    final int nbrAltTs = tss.length;
    alternateTTypeSystems = new TTypeSystem[nbrAltTs];
    for (int i = 0; i < nbrAltTs; i++){
      alternateTTypeSystems[i] = setupTTypeSystem(tss[i]);
    }
    lfs = new ArrayList<FeatureStructure>();
  }
  
  public void tearDown() {
    remoteCas = null;
    lfs = null;
    mSrc = null;
    casSrc = null;
    alternateTTypeSystems = null;
    // don't reset random
  }
  
//  void setupTgtTs(String kind) {
//    CASTestSetup setupTgt = new CASTestSetup(kind);
//    casTgt = (CASImpl) CASInitializer.initCas(setupTgt);
//    mTgt = setupTgt.m;
//    mTgt.ts = casTgt.getTypeSystemImpl();   
//  }
  
  TTypeSystem getTT(TypeSystems kind) {
    return alternateTTypeSystems[kind.ordinal()];
  }
  
  CASImpl setupCas(TTypeSystem m) {
    return createCAS(m.ts);
  }

  CASImpl createCAS(TypeSystemImpl ts) {
    try {
      return (CASImpl) CasCreationUtils.createCas(ts, null, null, null, null);
    } catch (ResourceInitializationException e) {
      throw new RuntimeException(e);
    }
  }

  public void testDocText() {
    try {
      CAS cas = CasCreationUtils.createCas((TypeSystemDescription) null, null, null);
      cas.setDocumentLanguage("latin");
      cas.setDocumentText("test");

      ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
      
      Serialization.serializeWithCompression(cas, baos, cas.getTypeSystem());
      
      CAS cas2 = CasCreationUtils.createCas((TypeSystemDescription) null, null, null);
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      Serialization.deserializeCAS(cas2, bais); 

      assertEquals("latin", cas2.getDocumentLanguage());
      assertEquals("test", cas2.getDocumentText());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public void testDocumentText() {
//     serdesSimple(getTT(EqTwoTypes));
    remoteCas = setupCas(getTT(EqTwoTypes));
    casSrc.reset();
    casSrc.setDocumentText(testDocText);
    loadCas(casSrc, mSrc);  
    verify(remoteCas, "DocumentText");
    assertEquals(remoteCas.getDocumentText(), testDocText);
    
    // test case where serialization is done without type filtering,
    //   and deserialization is done with filtering
    remoteCas.reset();
    verifyDeserFilter(remoteCas, "WithDeserFilterDocumentText");
    assertEquals(remoteCas.getDocumentText(), testDocText);
  }
  
  /**
   * Make one of each kind of artifact, including arrays
   * serialize to byte stream, deserialize into new cas, compare
   */
  
  public void testAllKinds() {
    if (doPlain) {
      serdesSimple(getTT(EqTwoTypes), "EqTwoTypes");
    } else {
      int i = 0;
      for (TTypeSystem m : alternateTTypeSystems) {
        switch (m.kind){
        // note: case statements *not* grouped in order to faclitate debugging
        case OneTypeSubsetFeatures:
          serdesSimple(m, "OneTypeSubsetFeatures" + m.kind.toString());
          break;
        case TwoTypesSubsetFeatures:
          serdesSimple(m, "TwoTypesSubsetFeatures" + m.kind.toString());
          break;
        case TwoTypes:
          i++;
          serdesSimple(m, "OtherAllKinds" + m.kind.toString() + Integer.toString(i));
          break;
        case EqTwoTypes:
        case OneType:
        case TwoTypesNoFeatures:
          serdesSimple(m, "OtherAllKinds" + m.kind.toString());
          break;
        }
      }
    }
  }

  
  // Test chains going through filtered type
  //   Repeat below with OneType, and TwoTypes with filtered slot == fsRef
  
  //   T1 fsArray ref -> T2 -> T1 (new) (not indexed)
  //   T1         ref -> T2 -> T1 (new) (not indexed)
  //   T1 fsArray ref -> T2 -> T1 (new) (indexed)
  //   T1         ref -> T2 -> T1 (new) (indexed)

  public void testRefThroughFilteredType() {
    reftft(OneType, 0);
    for (int i = 0; i < 10; i++) {
      reftft(TwoTypesSubsetFeatures, i);
    }
    reftft(TwoTypesNoFeatures, 0);
  }
  
  private void reftft(TypeSystems tskind, int i) {
    reftft(tskind, true, i);
    reftft(tskind, false, i);
  }
  
  /**
   * Inner part of test of refs through filtered type
   * 
   * @param tskind -
   * @param indexed -
   */
  private void reftft(TypeSystems tskind, boolean indexed, int i) {
    lfs.clear();
    
    TTypeSystem m = getTT(tskind);
    remoteCas = setupCas(m);
    // casSrc.reset();
    makeFeaturesForAkof(casSrc, mSrc, Akof1);
    
    FeatureStructure otherTsFs = casSrc.createFS(mSrc.getType(Akof2));
    FeatureStructure fsOrig = lfs.get(0);
    fsOrig.setFeatureValue(mSrc.getFeature(fsOrig, "Fs"), otherTsFs);
    
    FeatureStructure ts1Fs = casSrc.createFS(mSrc.getType(Akof1));
    otherTsFs.setFeatureValue(mSrc.getFeature(otherTsFs, "Fs"), ts1Fs);
    
    if (indexed) {
      casSrc.addFsToIndexes(ts1Fs);
    }
    
    verify(remoteCas, "refThroughFilteredType" 
       + (indexed ? "Indexed" : "NotIndexed") 
       + tskind.toString() + Integer.toString(i));
    
  }
  // broken out special instances of random tests
  public void testDeltaWithStringArrayMod() {
    // casSrc -> remoteCas,remoteCas updated, serialized back to srcCas
    for (int i = 0; i < 10; i++) {
      TTypeSystem m = getTT(EqTwoTypes);
      remoteCas = setupCas(m);
      loadCas(casSrc, mSrc);
      ReuseInfo[] ri = serializeDeserialize(casSrc, remoteCas, null, null);
      MarkerImpl marker = (MarkerImpl) remoteCas.createMarker();
      lfs = getIndexedFSs(remoteCas, m);
      FeatureStructure fs = lfs.get(10);
      StringArrayFS sa = (StringArrayFS) maybeGetFeatureKind(fs, m, "Astring");
      if (sa == null) {  // could happen because features are randomly omitted
        System.out.println("    Astring feature omitted, retrying");
      } else if (sa.size() == 0) {
        System.out.println("    Astring feature array has 0 length, retrying");    
      } else {
        sa.set(0, "change2");
        verifyDelta(marker, ri);
        break;
      }
      // setRandom();
      setUp();
      long seed = random.nextLong();
      random.setSeed(seed);
      System.out.println(" testDelta w/ String array mod random = " + seed + ", i = " + i);
    }
  }

  public void testDeltaWithDblArrayMod() {
    for (int i = 0; i < 10; i++) {
      TTypeSystem m = getTT(EqTwoTypes);
      remoteCas = setupCas(m);
      loadCas(casSrc, mSrc);
      ReuseInfo[] ri = serializeDeserialize(casSrc, remoteCas, null, null);
      MarkerImpl marker = (MarkerImpl) remoteCas.createMarker();
      lfs = getIndexedFSs(remoteCas, m);
      FeatureStructure fs = lfs.get(10);  /* has double array length 2 */
      DoubleArrayFS d = (DoubleArrayFS) maybeGetFeatureKind(fs, m, "Adouble");
      if (d == null) {  // could happen because features are randomly omitted
        System.out.println("    Adouble feature omitted, retrying");
      } else if (d.size() == 0) {
        System.out.println("    Adouble feature array has 0 length, retrying");    
      } else {
        d.set(0, 12.34D);
        verifyDelta(marker, ri);
        break;
      }      
      // setRandom();
      setUp();
      long seed = random.nextLong();
      random.setSeed(seed);
      System.out.println(" testDelta w/ dbl array mod random = " + seed + ", i = " + i);
    }
  }
  
  public void testDeltaWithByteArrayMod() {
    for (int i = 0; i < 10; i++) {
      TTypeSystem m = getTT(EqTwoTypes);
      remoteCas = setupCas(m);
      loadCas(casSrc, mSrc);
      ReuseInfo[] ri = serializeDeserialize(casSrc, remoteCas, null, null);
      MarkerImpl marker = (MarkerImpl) remoteCas.createMarker();
      
      lfs = getIndexedFSs(remoteCas, m); // gets FSs below the line
  
      FeatureStructure fs = lfs.get(10);
      ByteArrayFS sfs = (ByteArrayFS) maybeGetFeatureKind(fs, m, "Abyte");
      if (sfs == null) {  // could happen because features are randomly omitted
        System.out.println("    Abyte feature omitted, retrying");
      } else if (sfs.size() == 0) {
        System.out.println("    Abyte feature array has 0 length, retrying");    
      } else {
        sfs.set(0, (byte)21);
        verifyDelta(marker, ri);
        break;
      }
      // setRandom(); // retry with different random number
      setUp();
      System.out.println("  testDelta w byte array mod retrying, i = " + i);
    }
  }

  public void testDeltaWithStrArrayMod() {
    TTypeSystem m = getTT(EqTwoTypes);
    remoteCas = setupCas(m);
    loadCas(casSrc, mSrc);
    ReuseInfo[] ri = serializeDeserialize(casSrc, remoteCas, null, null);
    MarkerImpl marker = (MarkerImpl) remoteCas.createMarker();
    
    lfs = getIndexedFSs(remoteCas, m);

    FeatureStructure fs = lfs.get(10);
    StringArrayFS sfs = (StringArrayFS) maybeGetFeatureKind(fs, m, "Astring");
    if (sfs != null) {
      sfs.set(0, "change");
    }

    verifyDelta(marker, ri);
  }
  
  private void serdesSimple(TTypeSystem m, String kind) {
    remoteCas = setupCas(m);
    casSrc.reset();
    loadCas(casSrc, mSrc);  
    verify(remoteCas, kind);
    
    // test case where serialization is done without type filtering,
    //   and deserialization is done with filtering
    remoteCas.reset();
    verifyDeserFilter(remoteCas, "WithDeserFilter" + kind);
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
    if (doPlain) {
      serdesDelta(getTT(EqTwoTypes));
    } else {
      for (TTypeSystem m : alternateTTypeSystems) {
        switch (m.kind){
        case TwoTypesSubsetFeatures:
          serdesDelta(m);
          break;
        case OneTypeSubsetFeatures:
          serdesDelta(m);
          break;
        case TwoTypes:
        case EqTwoTypes:
        case OneType:
        case TwoTypesNoFeatures:
          serdesDelta(m);
          break;
        }
      }
    }
  }
  
  private void serdesDelta(TTypeSystem m) {
    remoteCas = setupCas(m); // create empty new CAS with specified type system from m.ts
//    casSrc.reset();
    loadCas(casSrc, mSrc); // load up the src cas using mSrc spec
    // src -> serialize -> deserialize -> rmt
    ReuseInfo[] ri = serializeDeserialize(casSrc, remoteCas, null, null);
    
    MarkerImpl marker = (MarkerImpl) remoteCas.createMarker();
    loadCas(remoteCas, m); // load some changes into remote
    // rmt -> serialize(full ts) -> deserialize(2 ts) -> src, then compare src & rmt
    verifyDelta(marker, ri); 
  }
  
  public void testDeltaWithRefsBelow() {
    lfs.clear();
    TTypeSystem m = getTT(EqTwoTypes);
    remoteCas = setupCas(m);
    loadCas(casSrc, mSrc);
    ReuseInfo ri[] = serializeDeserialize(casSrc, remoteCas, null, null);
    MarkerImpl marker = (MarkerImpl) remoteCas.createMarker();
    
    lfs = getIndexedFSs(remoteCas, m);
    FeatureStructure fs = remoteCas.createFS(m.getType(Akof1));
    maybeSetFeature(fs, m, lfs.get(0));
    ArrayFS fsafs = remoteCas.createArrayFS(4);
    fsafs.set(1, lfs.get(1));
    fsafs.set(2, lfs.get(2));
    fsafs.set(3, lfs.get(3));
    maybeSetFeatureKind(fs, m, "Afs", fsafs);
    
    verifyDelta(marker, ri);
  }

  public void testDeltaWithMods() {
    lfs.clear();
    TTypeSystem m = getTT(EqTwoTypes);
    remoteCas = setupCas(m);
    loadCas(casSrc, mSrc);
    ReuseInfo ri[] = serializeDeserialize(casSrc, remoteCas, null, null);
    MarkerImpl marker = (MarkerImpl) remoteCas.createMarker();
    
    lfs = getIndexedFSs(remoteCas, m); // get list of all "Akof1" FS
    FeatureStructure fs = remoteCas.createFS(m.getType(Akof1));
    // set the lfs.get(0) featurestructure's feature "Fs" to the new fs
    maybeSetFeatureKind( lfs.get(0), m, "Fs", fs);
    
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

    for (int i = 0; i < 100; i ++ ) {
      checkDeltaWithAllMods();
      tearDown();
      setUp();
    }
  }

  public void checkDeltaWithAllMods() {
    makeRandomFss(casSrc, mSrc, Akof1, 7);
    TTypeSystem m = getTT(EqTwoTypes);
    remoteCas = setupCas(m);
    loadCas(casSrc, mSrc);
    ReuseInfo ri[] = serializeDeserialize(casSrc, remoteCas, null, null);
    MarkerImpl marker = (MarkerImpl) remoteCas.createMarker();
    
    lfs = getIndexedFSs(remoteCas, m);
    
    makeRandomFss(remoteCas, m, Akof1, 8);

    int i = 0;
    for (FeatureStructure fs : lfs) {
      if (((i++) % 2) == 0) {
        maybeSetFeature(fs, m, lfs.get(random.nextInt(lfs.size())));
      }
    }
    
    makeRandomUpdatesBelowMark(remoteCas, m, Akof1);
    
    verifyDelta(marker, ri);

  }
  
  public void testDeltaWithIndexMods() {
    TTypeSystem m = getTT(EqTwoTypes);
    remoteCas = setupCas(m);
    loadCas(casSrc, mSrc);
    ReuseInfo ri[] = serializeDeserialize(casSrc, remoteCas, null, null);
    MarkerImpl marker = (MarkerImpl) remoteCas.createMarker();
    
    lfs = new ArrayList<FeatureStructure>();
    loadCas(remoteCas, m);
    
    List<FeatureStructure> lfs2 = getIndexedFSs(remoteCas, m);

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

  private void runCaptureSet() {
    //  Java 8 style
//    setupRunTeardown(this::testDocText);
//    setupRunTeardown(this::testDocumentText);
//    setupRunTeardown(this::testAllKinds);
//    setupRunTeardown(this::testRefThroughFilteredType);
//    setupRunTeardown(this::testDeltaWithStringArrayMod);
//    setupRunTeardown(this::testDeltaWithDblArrayMod);
//    setupRunTeardown(this::testDeltaWithByteArrayMod);
//    setupRunTeardown(this::testDeltaWithStrArrayMod);
//    setupRunTeardown(this::testDelta);
//    setupRunTeardown(this::testDeltaWithRefsBelow);
//    setupRunTeardown(this::testDeltaWithMods);
//    setupRunTeardown(this::testDeltaWithIndexMods);
//    setupRunTeardown(this::testArrayAux);
    
    // Java 7 style
    setupRunTeardown(new Runnable() {public void run() {testDocText();}});     
    setupRunTeardown(new Runnable() {public void run() {testDocumentText();}});
    setupRunTeardown(new Runnable() {public void run() {testAllKinds();}});
    setupRunTeardown(new Runnable() {public void run() {testRefThroughFilteredType();}});
    setupRunTeardown(new Runnable() {public void run() {testDeltaWithStringArrayMod();}});
    setupRunTeardown(new Runnable() {public void run() {testDeltaWithDblArrayMod();}});
    setupRunTeardown(new Runnable() {public void run() {testDeltaWithByteArrayMod();}});
    setupRunTeardown(new Runnable() {public void run() {testDeltaWithStrArrayMod();}});
    setupRunTeardown(new Runnable() {public void run() {testDelta();}});
    setupRunTeardown(new Runnable() {public void run() {testDeltaWithRefsBelow();}});
    setupRunTeardown(new Runnable() {public void run() {testDeltaWithMods();}});
    setupRunTeardown(new Runnable() {public void run() {testDeltaWithIndexMods();}});
    setupRunTeardown(new Runnable() {public void run() {testArrayAux();}});

  }

  public void captureGenerated() {
    capture = true;
    initWriteSavedInts();
    runCaptureSet();
    try {
      savedIntsOutStream.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    capture = false;

  }

  /**
   * See if can read Version 2 serialized things and deserialize them Note:
   * Delta won't work unless the previous v2 test case indexed or ref'd all the
   * FSs, because otherwise, some FSs will be "deleted" by the modelling V3 does
   * for the CAS layout because they're not findable during scanning, and
   * therefore, delta mods won't be correct.
   */
  public void testWithPrevGenerated() {
    isKeep = true; // forces all akof fss to be indexed
    usePrevData = true;
    initReadSavedInts();
    runCaptureSet();
    isKeep = false;
    usePrevData = false;
    try {
      savedIntsStream.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void setupRunTeardown(Runnable tst) {
    setUp();
    tst.run();
    tearDown();
  }

  public void testArrayAux() {
    ArrayList<FeatureStructure> fsList = new ArrayList<FeatureStructure>();
    /**
     * Strings, non-array Long/Double:
     * Make equal items,
     * ser/deser, update one of the equal items, insure other not updated
     */
    FeatureStructure fsAt1 = newAkof(casSrc, mSrc, Akof1, fsList);
    FeatureStructure fsAt2 = newAkof(casSrc, mSrc, Akof1, fsList);
    casSrc.addFsToIndexes(fsAt1);
    casSrc.addFsToIndexes(fsAt2);

    createStringA(casSrc, mSrc, fsAt1, "at");
    createStringA(casSrc, mSrc, fsAt2, "at");
    TTypeSystem m = getTT(EqTwoTypes);
    remoteCas = setupCas(m);
    verify(remoteCas, "ArrayAuxString");
    
    FSIterator<FeatureStructure> it = remoteCas.indexRepository.getAllIndexedFS(m.getType(Akof1));
    FeatureStructure fsAt1d = it.next();
    FeatureStructure fsAt2d = it.next();
    StringArrayFS sa1 = (StringArrayFS) maybeGetFeatureKind(fsAt1d, m, "Astring");
    StringArrayFS sa2 = (StringArrayFS) maybeGetFeatureKind(fsAt2d, m, "Astring");
    sa1.set(1, "def");
    assertEquals(sa2.get(1), "abcat");
    assertEquals(sa1.get(1), "def");
    
    casSrc.reset();
    
    fsAt1 = newAkof(casSrc, mSrc, Akof1, fsList);
    fsAt2 = newAkof(casSrc, mSrc, Akof1, fsList);
    casSrc.addFsToIndexes(fsAt1);
    casSrc.addFsToIndexes(fsAt2);

    createLongA(casSrc, mSrc, fsAt1, 9);
    createLongA(casSrc, mSrc, fsAt2, 9);
    remoteCas.reset();
    verify(remoteCas, "ArrayAuxLong");
    
    it = remoteCas.indexRepository.getAllIndexedFS(m.getType(Akof1));
    fsAt1d = it.next();
    fsAt2d = it.next();
    LongArrayFS la1 = (LongArrayFS) maybeGetFeatureKind(fsAt1d, m, "Along");
    LongArrayFS la2 = (LongArrayFS) maybeGetFeatureKind(fsAt2d, m, "Along");
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
  
  
  private void createStringA(CASImpl cas, TTypeSystem m, FeatureStructure fs, String x) {
    StringArrayFS strafs = cas.createStringArrayFS(5);
    strafs.set(3, null);
    strafs.set(2, "" + x);
    strafs.set(1, "abc" + x);
    strafs.set(0, "abc" + x);
    strafs.set(4, "def" + x);
    maybeSetFeatureKind(fs, m, "Astring", strafs);
  }
  
  private void createIntA (CASImpl cas, TTypeSystem m, FeatureStructure fs, int x) {
    IntArrayFS iafs = cas.createIntArrayFS(4 + x);
    iafs.set(0, Integer.MAX_VALUE - x);
    iafs.set(1, Integer.MIN_VALUE + x);
    iafs.set(2, 17 + 100 * x);
    maybeSetFeatureKind(fs, m, "Aint", iafs);
  }
  
  private void createFloatA (CASImpl cas, TTypeSystem m, FeatureStructure fs, float x) {
    FloatArrayFS fafs = cas.createFloatArrayFS(6);
    fafs.set(0, Float.MAX_VALUE - x);
//    fafs.set(1, Float.MIN_NORMAL + x);
    fafs.set(2, Float.MIN_VALUE + x);
    fafs.set(3, Float.NaN);
    fafs.set(4, Float.NEGATIVE_INFINITY);
    fafs.set(5, Float.POSITIVE_INFINITY);
    maybeSetFeatureKind(fs, m, "Afloat", fafs);
  }

  private void createDoubleA (CASImpl cas, TTypeSystem m, FeatureStructure fs, double x) {
    DoubleArrayFS fafs = cas.createDoubleArrayFS(6);
    fafs.set(0, Double.MAX_VALUE - x);
//    fafs.set(1, Double.MIN_NORMAL + x);
    fafs.set(2, Double.MIN_VALUE + x);
    fafs.set(3, Double.NaN);
    fafs.set(4, Double.NEGATIVE_INFINITY);
    fafs.set(5, Double.POSITIVE_INFINITY);
    maybeSetFeatureKind(fs, m, "Adouble", fafs);
  }

  private void createLongA (CASImpl cas, TTypeSystem m, FeatureStructure fs, long x) {
    LongArrayFS lafs = cas.createLongArrayFS(4);
    lafs.set(0, Long.MAX_VALUE - x);
    lafs.set(1, Long.MIN_VALUE + x);
    lafs.set(2, -45 + x);
    maybeSetFeatureKind(fs, m, "Along", lafs);
  }
  
//  private void binaryCopyCas(CASImpl c1, CASImpl c2) {
//    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//    Serialization.serializeCAS(cas, baos);
//    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
//    c2.reinit(bais);
//  }
  
  private FeatureStructure newAkof(CASImpl cas, TTypeSystem m, Types typeKind,
      List<FeatureStructure> fsl) {
    FeatureStructure fs = cas.createFS(m.getType(typeKind.name()));
    fsl.add(fs);
    return fs;
  }
  
  // make an instance of akof with all features set
  private FeatureStructure makeAkof(CASImpl cas, TTypeSystem m, Types typeKind) {
    FeatureStructure fs = cas.createFS(m.getType(Akof1));
    maybeSetBoolean(fs, m, random.nextBoolean());
    maybeSetByte(fs, m, (byte)random.nextInt());
    maybeSetShort(fs, m, (short)random.nextInt());
    maybeSetInt(fs, m, random.nextInt());
    maybeSetFloat(fs, m, random.nextFloat());
    maybeSetLong(fs, m, random.nextLong());
    maybeSetDouble(fs, m, random.nextDouble());
    maybeSetString(fs, m,  randomString());
    maybeSetFeature(fs, m, fs);
    
    maybeSetFeatureKind(fs, m, "Aint", randomIntA(cas));
    maybeSetFeatureKind(fs, m, "Afs", cas.createArrayFS(1));
    maybeSetFeatureKind(fs, m, "Afloat", randomFloatA(cas));
    maybeSetFeatureKind(fs, m, "Adouble", randomDoubleA(cas));
    maybeSetFeatureKind(fs, m, "Along", randomLongA(cas));
    maybeSetFeatureKind(fs, m, "Ashort", randomShortA(cas));
    maybeSetFeatureKind(fs, m, "Abyte", randomByteA(cas));
    maybeSetFeatureKind(fs, m, "Aboolean", cas.createBooleanArrayFS(2));
    maybeSetFeatureKind(fs, m, "Astring", randomStringA(cas));

    return fs;    
  }
    
  private static final String[] stringValues = {
    "abc", "abcdef", null, "", "ghijklm", "a", "b"
  };
  private String randomString() {
    return stringValues[random.nextInt(7)];
  }

  private StringArrayFS randomStringA(CASImpl cas) {
    int length = random.nextInt(2) + 1;
    StringArrayFS fs = cas.createStringArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, stringValues[random.nextInt(stringValues.length)]);
    }
    return fs;
  }

  
  private IntArrayFS randomIntA(CASImpl cas) {
    int length = random.nextInt(2) + 1;
    IntArrayFS fs = cas.createIntArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, random.nextInt(101) - 50);
    }
    return fs;
  }
  
  private static final byte[] byteValues = {
    1, 0, -1, Byte.MAX_VALUE, Byte.MIN_VALUE, 9, -9  };
  
  private ByteArrayFS randomByteA(CASImpl cas) {
    int length = random.nextInt(2) + 1;
    ByteArrayFS fs = cas.createByteArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, byteValues[random.nextInt(byteValues.length)]);
    }
    return fs;
  }

  private static final long[] longValues = {
    1L, 0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, 11L, -11L  };
  
  private LongArrayFS randomLongA(CASImpl cas) {
    int length = random.nextInt(2) + 1;
    LongArrayFS fs = cas.createLongArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, longValues[random.nextInt(longValues.length)]);
    }
    return fs;
  }

  private static final short[] shortValues = {
    1, 0, -1, Short.MAX_VALUE, Short.MIN_VALUE, 22, -22  };
  
  private ShortArrayFS randomShortA(CASImpl cas) {
    int length = random.nextInt(2) + 1;
    ShortArrayFS fs = cas.createShortArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, shortValues[random.nextInt(shortValues.length)]);
    }
    return fs;
  }

  private static final double[] doubleValues = {
    1d, 0d, -1d, Double.MAX_VALUE, /*Double.MIN_NORMAL,*/ Double.MIN_VALUE, 33d, -33.33d  };
  
  private DoubleArrayFS randomDoubleA(CASImpl cas) {
    int length = random.nextInt(2) + 1;
    DoubleArrayFS fs = cas.createDoubleArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, doubleValues[random.nextInt(doubleValues.length)]);
    }
    return fs;
  }

  private static final float[] floatValues = {
    1f, 0f, -1f, Float.MAX_VALUE, /*Float.MIN_NORMAL,*/ Float.MIN_VALUE, 17f, -22.33f  };
  
  private FloatArrayFS randomFloatA(CASImpl cas) {
    int length = random.nextInt(2) + 1;
    FloatArrayFS fs = cas.createFloatArrayFS(length);
    for (int i = 0; i < length; i++) {
      fs.set(i, floatValues[random.nextInt(floatValues.length)]);
    }
    return fs;
  }
  
  private void makeRandomFss(CASImpl cas, TTypeSystem m, Types typeKind, int n) {
    List<FeatureStructure> lfss = new ArrayList<FeatureStructure>();
    for (int i = 0; i < n; i++) {
      FeatureStructure fs = makeAkof(cas, m, typeKind);
      if (random.nextBoolean()) {
        cas.addFsToIndexes(fs);
        lfss.add(fs);
        lfs.add(fs);
      }
    }
    for (FeatureStructure fs : lfss) {
      maybeSetFeature(fs, m, lfss.get(random.nextInt(lfss.size())));
    }
  }
  
  private void loadCas(CASImpl cas, TTypeSystem m) {
    makeFeaturesForAkof(cas, m, Akof1);
    // TwoTypes, EqTwoTypes, OneType, TwoTypesSubsetFeatures, OneTypeSubsetFeatures, NoFeatures,
    switch (m.kind){
    case TwoTypes:
    case EqTwoTypes:
    case TwoTypesSubsetFeatures:
    case TwoTypesNoFeatures:
      makeFeaturesForAkof(cas, m, Akof2);
      break;
    default:
    }
  }
  
  private void maybeSetBoolean(FeatureStructure fs, TTypeSystem m, boolean value) {
    Feature f = m.getFeature(fs, "Boolean");
    if (f != null) {
      fs.setBooleanValue(f, value);
    }
  }
  
  private void maybeSetByte(FeatureStructure fs, TTypeSystem m, byte value) {
    Feature f = m.getFeature(fs, "Byte");
    if (f != null) {
      fs.setByteValue(f, value);
    }
  }

  private void maybeSetShort(FeatureStructure fs, TTypeSystem m, short value) {
    Feature f = m.getFeature(fs, "Short");
    if (f != null) {
      fs.setShortValue(f, value);
    }
  }

  private void maybeSetInt(FeatureStructure fs, TTypeSystem m, int value) {
    Feature f = m.getFeature(fs, "Int");
    if (f != null) {
      fs.setIntValue(f, value);
    }
  }

  private void maybeSetFloat(FeatureStructure fs, TTypeSystem m, float value) {
    Feature f = m.getFeature(fs, "Float");
    if (f != null) {
      fs.setFloatValue(f, value);
    }
  }

  private void maybeSetLong(FeatureStructure fs, TTypeSystem m, long value) {
    Feature f = m.getFeature(fs, "Long");
    if (f != null) {
      fs.setLongValue(f, value);
    }
  }

  private void maybeSetDouble(FeatureStructure fs, TTypeSystem m, double value) {
    Feature f = m.getFeature(fs, "Double");
    if (f != null) {
      fs.setDoubleValue(f, value);
    }
  }

  private void maybeSetString(FeatureStructure fs, TTypeSystem m, String value) {
    Feature f = m.getFeature(fs, "String");
    if (f != null) {
      fs.setStringValue(f, value);
    }
  }

  private void maybeSetFeature(FeatureStructure fs, TTypeSystem m, FeatureStructure value) {
    Feature f = m.getFeature(fs, "Fs");
    if (f != null) {
      fs.setFeatureValue(f, value);
    }
  }

  /**********
   * ARRAYS
   **********/
  
  private void maybeSetFeature(FeatureStructure fs, Feature f, FeatureStructure value) {
    if (f != null) {
      fs.setFeatureValue(f, value);
    }    
  }
  
  private void maybeSetFeatureKind(FeatureStructure fs, TTypeSystem m, String kind, FeatureStructure value) {
    maybeSetFeature(fs, m.getFeature(fs, kind), value);
  }
  
  private FeatureStructure maybeGetFeatureKind(FeatureStructure fs, TTypeSystem m, String kind) {
    Feature f = m.getFeature(fs, kind);
    return (f == null) ? null : fs.getFeatureValue(f);
  }
  

  private void makeFeaturesForAkof(CASImpl cas, TTypeSystem m, Types typeKind) {
    /* lfs index: 0 */
    FeatureStructure fs = newAkof(cas, m, typeKind, lfs);
      
    maybeSetBoolean(fs, m, true);
    maybeSetByte(fs, m, (byte)109);
    maybeSetShort(fs, m, (short) 23);
    maybeSetInt(fs, m,  2345);
    maybeSetFloat(fs, m, 123f);
    maybeSetLong(fs, m, 345L);
    maybeSetDouble(fs, m, 334455.6677d);
    maybeSetString(fs, m, "str1");
    maybeSetFeature(fs, m, fs);
    cas.addFsToIndexes(fs);
    FeatureStructure fs1 = fs;
    
    //extreme or unusual values
    /* lfs index: 1 */
    fs = newAkof(cas, m, typeKind, lfs);
    maybeSetBoolean(fs, m, false);
    maybeSetByte(fs, m, Byte.MAX_VALUE);
    maybeSetShort(fs, m, Short.MAX_VALUE);
    maybeSetInt(fs, m, Integer.MAX_VALUE);
    maybeSetFloat(fs, m, Float.MAX_VALUE);
    maybeSetLong(fs, m, Long.MAX_VALUE);
    maybeSetDouble(fs, m, Double.MAX_VALUE);
    maybeSetString(fs, m, "");
    maybeSetFeature(fs, m, fs1);
    cas.addFsToIndexes(fs);

    /* lfs index: 2 */
    fs = newAkof(cas, m, typeKind, lfs);
    maybeSetByte(fs, m, Byte.MIN_VALUE);
    maybeSetShort(fs, m, Short.MIN_VALUE);
    maybeSetInt(fs, m, Integer.MIN_VALUE);
    maybeSetFloat(fs, m, Float.MIN_VALUE);
    maybeSetLong(fs, m, Long.MIN_VALUE);
    maybeSetDouble(fs, m, Double.MIN_VALUE);
    maybeSetString(fs, m,  null);
    maybeSetFeature(fs, m, fs1);
    cas.addFsToIndexes(fs);
    FeatureStructure fs3 = fs;

    /* lfs index: 3 */
    fs = newAkof(cas, m, typeKind, lfs);
    maybeSetByte(fs, m, (byte)0);
    maybeSetShort(fs, m, (short) 0);
    maybeSetInt(fs, m, 0);
    maybeSetFloat(fs, m, 0f);
    maybeSetLong(fs, m, 0L);
    maybeSetDouble(fs, m, 0D);
    maybeSetFeature(fs, m, fs1);
    cas.addFsToIndexes(fs);
    maybeSetFeature(fs3, m, fs);  // make a forward ref
    FeatureStructure fs4 = fs;

    /* lfs index: 4 */
    fs = newAkof(cas, m, typeKind, lfs);
    maybeSetByte(fs, m, (byte)1);
    maybeSetShort(fs, m, (short)1);
    maybeSetInt(fs, m, 1);
    maybeSetFloat(fs, m, 1.0f);
    maybeSetLong(fs, m, 1L);
    maybeSetDouble(fs, m, 1.0D);
    cas.addFsToIndexes(fs);
    
//    fs = newAkof(cas, m, lfs);
//    maybeSetFloat(fs, m, Float.MIN_NORMAL);
//    maybeSetDouble(fs, m, Double.MIN_NORMAL);
//    cas.addFsToIndexes(fs);
    
    /* lfs index: 5 */
    fs = newAkof(cas, m, typeKind, lfs);
    maybeSetFloat(fs, m, Float.MIN_VALUE);
    maybeSetDouble(fs, m, Double.MIN_VALUE);
    cas.addFsToIndexes(fs);

    /* lfs index: 6 */
    fs = newAkof(cas, m, typeKind, lfs);
    maybeSetFloat(fs, m, Float.NaN);
    maybeSetDouble(fs, m, Double.NaN);
    cas.addFsToIndexes(fs);

    /* lfs index: 7 */
    fs = newAkof(cas, m, typeKind, lfs);
    maybeSetFloat(fs, m, Float.POSITIVE_INFINITY);
    maybeSetDouble(fs, m, Double.POSITIVE_INFINITY);
    cas.addFsToIndexes(fs);

    /* lfs index: 8 */
    fs = newAkof(cas, m, typeKind, lfs);
    maybeSetFloat(fs, m, Float.NEGATIVE_INFINITY);
    maybeSetDouble(fs, m, Double.NEGATIVE_INFINITY);
    cas.addFsToIndexes(fs);

    
    // test arrays
    /* lfs index: 9 */
    fs = newAkof(cas, m, typeKind, lfs);
    maybeSetFeatureKind(fs, m, "Aint", cas.createIntArrayFS(0));
    maybeSetFeatureKind(fs, m, "Afs",  cas.createArrayFS(0));
    maybeSetFeatureKind(fs, m, "Afloat", cas.createFloatArrayFS(0));
    maybeSetFeatureKind(fs, m, "Adouble", cas.createDoubleArrayFS(0));
    maybeSetFeatureKind(fs, m, "Along", cas.createLongArrayFS(0));
    maybeSetFeatureKind(fs, m, "Ashort", cas.createShortArrayFS(0));
    maybeSetFeatureKind(fs, m, "Abyte", cas.createByteArrayFS(0));
    maybeSetFeatureKind(fs, m, "Aboolean", cas.createBooleanArrayFS(0));
    maybeSetFeatureKind(fs, m, "Astring", cas.createStringArrayFS(0));
    cas.addFsToIndexes(fs);
    FeatureStructure fs8 = fs;

    /* lfs index: 10 */
    fs = newAkof(cas, m, typeKind, lfs);
    maybeSetFeatureKind(fs, m, "Aint",  cas.createIntArrayFS(2));
    maybeSetFeatureKind(fs, m, "Afs",  cas.createArrayFS(2));
    maybeSetFeatureKind(fs, m, "Afloat", cas.createFloatArrayFS(2));
    maybeSetFeatureKind(fs, m, "Adouble", cas.createDoubleArrayFS(2));
    maybeSetFeatureKind(fs, m, "Along", cas.createLongArrayFS(2));
    maybeSetFeatureKind(fs, m, "Ashort", cas.createShortArrayFS(2));
    maybeSetFeatureKind(fs, m, "Abyte", cas.createByteArrayFS(2));
    maybeSetFeatureKind(fs, m, "Aboolean", cas.createBooleanArrayFS(2));
    maybeSetFeatureKind(fs, m, "Astring", cas.createStringArrayFS(2));
    cas.addFsToIndexes(fs);
    
    /* lfs index: 11 */
    fs = newAkof(cas, m, typeKind, lfs);
    cas.addFsToIndexes(fs);
    
    createIntA(cas, m, fs, 0);
    
    // feature structure array
    /* lfs index: 12 */
    ArrayFS fsafs = cas.createArrayFS(4);
    fsafs.set(1, fs8);
    fsafs.set(2, fs1);
    fsafs.set(3, fs4);
    maybeSetFeatureKind(fs, m, "Afs",  fsafs);
    
    createFloatA(cas, m, fs, 0f);
    createDoubleA(cas, m, fs, 0d);
    createLongA(cas, m, fs, 0L);
    
    ShortArrayFS safs = cas.createShortArrayFS(4);
    safs.set(0, Short.MAX_VALUE);
    safs.set(1, Short.MIN_VALUE);
    safs.set(2, (short)-485);
    maybeSetFeatureKind(fs, m, "Ashort", safs);
    
    ByteArrayFS bafs = cas.createByteArrayFS(4);
    bafs.set(0, Byte.MAX_VALUE);
    bafs.set(1, Byte.MIN_VALUE);
    bafs.set(2, (byte) 33);
    maybeSetFeatureKind(fs, m, "Abyte", bafs);
    
    BooleanArrayFS booafs = cas.createBooleanArrayFS(4);
    booafs.set(0, true);
    booafs.set(1, false);
    maybeSetFeatureKind(fs, m, "Aboolean", booafs);
    
    createStringA(cas, m, fs, "");
    makeRandomFss(cas, m, typeKind, 15);
  }

  private void verify(CASImpl casTgt, String fname) {
    // no delta case:
    // casSrc -> deserCas 
    BinaryCasSerDes6 bcs = null;
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
      ByteArrayInputStream bais;
      if (!usePrevData) {
      if (doPlain) {
        (new CASSerializer()).addCAS(casSrc, baos);      
      } else {
        bcs = new BinaryCasSerDes6(casSrc, casTgt.getTypeSystemImpl());
        Serialization.serializeWithCompression(casSrc, baos, casTgt.getTypeSystemImpl());
//        bcs = new BinaryCasSerDes6(casSrc, casTgt.getTypeSystemImpl());
//        SerializationMeasures sm = bcs.serialize(baos);
//        if (null != sm) {
//          System.out.println(sm);
//        }
          if (capture) {
            writeout(baos, fname);
      }
        }
        bais = new ByteArrayInputStream(baos.toByteArray());
      } else {
        bcs = new BinaryCasSerDes6(casSrc, casTgt.getTypeSystemImpl());
        bais = new ByteArrayInputStream(readIn(fname));
      }
      casTgt.reinit(bais);
      if (doPlain) {
        assertTrue(new BinaryCasSerDes6(casSrc).compareCASes(casSrc, casTgt));
      } else {
        // have to reuse the bcs instance with the type system mappings        
        assertTrue(bcs.compareCASes(casSrc, casTgt));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ResourceInitializationException e) {
      e.printStackTrace();
      fail();
    }    
  }
  
  private void verifyDeserFilter(CASImpl casTgt, String fname) {
    // serialize w/o filter
    BinaryCasSerDes6 bcs = null;
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
      if (doPlain) {
        return;   
      } else {      
        bcs = new BinaryCasSerDes6(casSrc, (ReuseInfo) null);
        bcs.serialize(baos);
        if (capture) {
          writeout(baos, fname);
      }
      }
      ByteArrayInputStream bais = (!usePrevData || fname == null) 
          ? new ByteArrayInputStream(baos.toByteArray())
          : new ByteArrayInputStream(readIn(fname));
          
      Serialization.deserializeCAS(casTgt, bais, casSrc.getTypeSystemImpl(), null);

      bcs = new BinaryCasSerDes6(casSrc, casTgt.getTypeSystemImpl());
      assertTrue(bcs.compareCASes(casSrc, casTgt));
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ResourceInitializationException e) {
      e.printStackTrace();
      fail();
    }    
   
  }

  // casSrc -> remoteCas
  private ReuseInfo[] serializeDeserialize(CASImpl casSrc, CASImpl casTgt, ReuseInfo ri,
      MarkerImpl mark) {
    ReuseInfo[] riToReturn = new ReuseInfo[2];
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
      if (doPlain) {
        if (null == mark) {
          Serialization.serializeCAS(casSrc, baos);
        } else {
          Serialization.serializeCAS(casSrc, baos, mark);
        }
      } else {
        BinaryCasSerDes6 bcs = new BinaryCasSerDes6(casSrc, casTgt.getTypeSystemImpl());
        SerializationMeasures sm = bcs.serialize(baos);
        if (sm != null) {
          System.out.println(sm);
        }
        riToReturn[0] = bcs.getReuseInfo();
      }
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      if (doPlain) {
        casTgt.reinit(bais);
      } else {
        riToReturn[1] = Serialization.deserializeCAS(casTgt, bais, null, null).getReuseInfo();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ResourceInitializationException e) {
      e.printStackTrace();
      fail();
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
        if (sm != null) {
          System.out.println(sm);
        }
      }
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      if (doPlain) {
        casSrc.reinit(bais);
        assertTrue(new BinaryCasSerDes6(casSrc).compareCASes(casSrc, remoteCas));
      } else {
        BinaryCasSerDes6 bcsDeserialize = Serialization.deserializeCAS(casSrc, bais,
            remoteCas.getTypeSystemImpl(), ri[0]);
          assertTrue(bcsDeserialize.compareCASes(casSrc, remoteCas));
      }      
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ResourceInitializationException e) {
      e.printStackTrace();
      fail();
    }
  }
  
  private void makeRandomUpdatesBelowMark(CASImpl cas, TTypeSystem m, Types typeKind) {
    for (FeatureStructure fs : lfs) {
      makeRandomUpdate(cas, m, typeKind, fs);
    }
  }

  private void makeRandomUpdate(CASImpl cas, TTypeSystem m, Types typeKind, FeatureStructure fs) {
    int n = random.nextInt(3);
    for (int i = 0 ; i < n; i++) {
      switch (random.nextInt(26)) {
      case 0:
        maybeSetBoolean(fs, m, random.nextBoolean());
        break;
      case 1:
        maybeSetByte(fs, m, (byte)random.nextInt());
        break;
      case 2:
        maybeSetShort(fs, m, (short)random.nextInt());
        break;
      case 3:
        maybeSetInt(fs, m, random.nextInt());
        break;
      case 4:
        maybeSetFloat(fs, m, random.nextFloat());
        break;
      case 5:
        maybeSetLong(fs, m, random.nextLong());
        break;
      case 6:
        maybeSetDouble(fs, m, random.nextDouble());
        break;
      case 7:
        maybeSetString(fs, m,  randomString());
        break;
      case 8:
        maybeSetFeature(fs, m, fs);
        break;
      case 9:
        maybeSetFeatureKind(fs, m, "Aint", randomIntA(cas));
        break;
      case 10:
        maybeSetFeatureKind(fs, m, "Afs",  cas.createArrayFS(1));
        break;
      case 11:
        maybeSetFeatureKind(fs, m, "Afloat", randomFloatA(cas));
        break;
      case 12:
        maybeSetFeatureKind(fs, m, "Adouble", randomDoubleA(cas));
        break;
      case 13:
        maybeSetFeatureKind(fs, m, "Along", randomLongA(cas));
        break;
      case 14:
        maybeSetFeatureKind(fs, m, "Ashort", randomShortA(cas));
        break;
      case 15:
        maybeSetFeatureKind(fs, m, "Abyte", randomByteA(cas));
        break;
      case 16:
        maybeSetFeatureKind(fs, m, "Aboolean", cas.createBooleanArrayFS(2));
        break;
      case 17: 
        maybeSetFeatureKind(fs, m, "Astring", randomStringA(cas));
        break;
      case 18: {
          IntArrayFS sfs = (IntArrayFS) maybeGetFeatureKind(fs, m, "Aint");
          if ((null != sfs) && (0 < sfs.size())) {
            sfs.set(0, 1);
          }
        }
        break;
      case 19:{
          StringArrayFS sfs = (StringArrayFS) maybeGetFeatureKind(fs, m, "Astring");
          if ((null != sfs) && (0 < sfs.size())) {
            sfs.set(0, "change");
          }
        }
        break;
      case 20: {
          FloatArrayFS sfs = (FloatArrayFS) maybeGetFeatureKind(fs, m, "Afloat");
          if ((null != sfs) && (0 < sfs.size())) {
            sfs.set(0, 1F);
          }
        }
        break;
      case 21: {
          DoubleArrayFS sfs = (DoubleArrayFS) maybeGetFeatureKind(fs, m, "Adouble");
          if ((null != sfs) && (0 < sfs.size())) {
            sfs.set(0, 1D);
          }
        }
        break;
      case 22: {
          LongArrayFS sfs = (LongArrayFS) maybeGetFeatureKind(fs, m, "Along");
          if ((null != sfs) && (0 < sfs.size())) {
            sfs.set(0, 1L);
          }
        }
        break;
      case 23: {
          ShortArrayFS sfs = (ShortArrayFS) maybeGetFeatureKind(fs, m, "Ashort");
          if ((null != sfs) && (0 < sfs.size())) {
            sfs.set(0, (short)1);
          }
        }
        break;
      case 24: {
          ByteArrayFS sfs = (ByteArrayFS) maybeGetFeatureKind(fs, m, "Abyte");
          if ((null != sfs) && (0 < sfs.size())) {
            sfs.set(0, (byte)1);
          }
        }
        break;
      case 25: {
          ArrayFS sfs = (ArrayFS) maybeGetFeatureKind(fs, m, "Afs");
          if ((null != sfs) && (0 < sfs.size())) {
            sfs.set(0, lfs.get(random.nextInt(lfs.size())));
          }
        }
      break;
      }
    }
  }

  private List<FeatureStructure> getIndexedFSs(CASImpl cas, TTypeSystem m) {
    FSIterator<FeatureStructure> it = cas.getIndexRepository().getAllIndexedFS(m.getType(Akof1));
    List<FeatureStructure> lfs = new ArrayList<FeatureStructure>();
    while (it.hasNext()) {
      lfs.add(it.next());
    }
    return lfs;
  }

  @Override
  protected String getTestRootName() {
    return "SerDes6";
  }

  // disable to avoid accidentally overwriting test data
  static public void main(String[] args) throws IOException {
    new SerDesTest6().captureGenerated();
  }

}
