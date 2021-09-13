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
package org.apache.uima.cas.serdes.generators;

import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.apache.uima.cas.serdes.generators.MultiFeatureRandomCasGenerator.StringArrayMode.ALLOW_NULL_AND_EMPTY_STRINGS;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;

public class MultiFeatureRandomCasGenerator implements CasGenerator {

  private static final String TYPE_NAME_AKOF = "akof";
  private static final String FEATURE_BASE_NAME_AKOF_UID = "akofUid";
  private static final String FEATURE_BASE_NAME_AKOF_INT = "akofInt";
  private static final String FEATURE_BASE_NAME_AKOF_FS = "akofFs";
  private static final String FEATURE_BASE_NAME_AKOF_FLOAT = "akofFloat";
  private static final String FEATURE_BASE_NAME_AKOF_DOUBLE = "akofDouble";
  private static final String FEATURE_BASE_NAME_AKOF_LONG = "akofLong";
  private static final String FEATURE_BASE_NAME_AKOF_SHORT = "akofShort";
  private static final String FEATURE_BASE_NAME_AKOF_BYTE = "akofByte";
  private static final String FEATURE_BASE_NAME_AKOF_BOOLEAN = "akofBoolean";
  private static final String FEATURE_BASE_NAME_AKOF_STRING = "akofStr";
  private static final String FEATURE_BASE_NAME_AKOF_INT_ARRAY = "akofAInt";
  private static final String FEATURE_BASE_NAME_AKOF_FS_ARRAY = "akofAFs";
  private static final String FEATURE_BASE_NAME_AKOF_FLOAT_ARRAY = "akofAFloat";
  private static final String FEATURE_BASE_NAME_AKOF_DOUBLE_ARRAY = "akofADouble";
  private static final String FEATURE_BASE_NAME_AKOF_LONG_ARRAY = "akofALong";
  private static final String FEATURE_BASE_NAME_AKOF_SHORT_ARRAY = "akofAShort";
  private static final String FEATURE_BASE_NAME_AKOF_BYTE_ARRAY = "akofAByte";
  private static final String FEATURE_BASE_NAME_AKOF_BOOLEAN_ARRAY = "akofABoolean";
  private static final String FEATURE_BASE_NAME_AKOF_STRING_ARRAY = "akofAStr";

  private static final String[] STRING_VALUES = { "abc", "abcdef", null, "", "ghijklm", "a", "b" };
  private static final byte[] BYTE_VALUES = { 1, 0, -1, Byte.MAX_VALUE, Byte.MIN_VALUE, 9, -9 };
  private static final long[] LONG_VALUES = { 1L, 0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, 11L,
      -11L };
  private static final short[] SHORT_VALUES = { 1, 0, -1, Short.MAX_VALUE, Short.MIN_VALUE, 22,
      -22 };
  private static final double[] DOUBLE_VALUES = { 1d, 0d, -1d, Double.MAX_VALUE,
      /* Double.MIN_NORMAL, */ Double.MIN_VALUE, 33d, -33.33d, Double.NaN, Double.NEGATIVE_INFINITY,
      Double.POSITIVE_INFINITY };
  private static final float[] FLOAT_VALUES = { 1f, 0f, -1f, Float.MAX_VALUE,
      /* Float.MIN_NORMAL, */ Float.MIN_VALUE, 17f, -22.33f, Float.NaN, Float.NEGATIVE_INFINITY,
      Float.POSITIVE_INFINITY };

  /**
   * set to true to change FS creation to keep references to all created FS.
   * 
   * Needed for testing backward compatibility with delta CAS.
   * 
   * Done by adding to indexes FSs which otherwise would be lost.
   */
  private final boolean isKeep;
  private final boolean includeUid;
  private final Random rnd;
  private final int size;
  private final StringArrayMode stringArrayMode;

  // akof = all kinds of features
  private Type akof;
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
  private AtomicInteger aint;

  private MultiFeatureRandomCasGenerator(Builder builder) {
    isKeep = builder.isKeep;
    includeUid = builder.includeUid;
    rnd = builder.randomGenerator;
    size = builder.size;
    stringArrayMode = builder.stringArrayMode;
    aint = includeUid ? new AtomicInteger(0) : null;
  }

  @Override
  public TypeSystemDescription generateTypeSystem() {
    TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();

    TypeDescription akofTD = tsd.addType(TYPE_NAME_AKOF, null, CAS.TYPE_NAME_TOP);

    if (includeUid) {
      akofTD.addFeature(FEATURE_BASE_NAME_AKOF_UID, null, CAS.TYPE_NAME_INTEGER);
    }

    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_INT, null, CAS.TYPE_NAME_INTEGER);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_FS, null, CAS.TYPE_NAME_TOP);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_FLOAT, null, CAS.TYPE_NAME_FLOAT);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_DOUBLE, null, CAS.TYPE_NAME_DOUBLE);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_LONG, null, CAS.TYPE_NAME_LONG);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_SHORT, null, CAS.TYPE_NAME_SHORT);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_BYTE, null, CAS.TYPE_NAME_BYTE);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_BOOLEAN, null, CAS.TYPE_NAME_BOOLEAN);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_STRING, null, CAS.TYPE_NAME_STRING);

    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_INT_ARRAY, null, CAS.TYPE_NAME_INTEGER_ARRAY);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_FS_ARRAY, null, CAS.TYPE_NAME_FS_ARRAY);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_FLOAT_ARRAY, null, CAS.TYPE_NAME_FLOAT_ARRAY);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_DOUBLE_ARRAY, null, CAS.TYPE_NAME_DOUBLE_ARRAY);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_LONG_ARRAY, null, CAS.TYPE_NAME_LONG_ARRAY);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_SHORT_ARRAY, null, CAS.TYPE_NAME_SHORT_ARRAY);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_BYTE_ARRAY, null, CAS.TYPE_NAME_BYTE_ARRAY);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_BOOLEAN_ARRAY, null, CAS.TYPE_NAME_BOOLEAN_ARRAY);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_STRING_ARRAY, null, CAS.TYPE_NAME_STRING_ARRAY);

    return tsd;
  }

  @Override
  public CAS generateCas(TypeSystemDescription aTsd) throws ResourceInitializationException {
    cas = (CASImpl) CasCreationUtils.createCas(aTsd, null, null, null);

    TypeSystem ts = cas.getTypeSystem();

    akof = ts.getType(TYPE_NAME_AKOF);
    akofUid = akof.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_UID);
    akofInt = akof.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_INT);
    akofFloat = akof.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_FLOAT);
    akofDouble = akof.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_DOUBLE);
    akofLong = akof.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_LONG);
    akofShort = akof.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_SHORT);
    akofByte = akof.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_BYTE);
    akofBoolean = akof.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_BOOLEAN);
    akofString = akof.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_STRING);
    akofFs = akof.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_FS);
    akofAint = akof.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_INT_ARRAY);
    akofAfloat = akof.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_FLOAT_ARRAY);
    akofAdouble = akof.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_DOUBLE_ARRAY);
    akofAlong = akof.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_LONG_ARRAY);
    akofAshort = akof.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_SHORT_ARRAY);
    akofAbyte = akof.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_BYTE_ARRAY);
    akofAboolean = akof.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_BOOLEAN_ARRAY);
    akofAstring = akof.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_STRING_ARRAY);
    akofAfs = akof.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_FS_ARRAY);

    makeRandomFss();

    return cas;
  }

  /**
   * Make a bunch of Akof fs's, not indexed, linked randomly to each other. In v3, these might be
   * dropped due to no refs, no indexing.
   */
  private void makeRandomFss() {
    List<FeatureStructure> lfss = new ArrayList<>();

    for (int i = 0; i < size; i++) {
      FeatureStructure fs = makeAkof();
      lfss.add(fs);
    }

    // Randomly link feature structures to each other
    for (FeatureStructure fs : lfss) {
      fs.setFeatureValue(akofFs, lfss.get(rnd.nextInt(lfss.size())));
      ((ArrayFS) fs.getFeatureValue(akofAfs)).set(0, lfss.get(rnd.nextInt(lfss.size())));
    }
  }

  // make an instance of akof with all features set
  // ** NOT added to index unless isKeep
  private FeatureStructure makeAkof() {
    FeatureStructure fs = maybeKeep(cas.createFS(akof));

    if (includeUid) {
      fs.setIntValue(akofUid, aint.getAndAdd(1));
    }

    fs.setBooleanValue(akofBoolean, rnd.nextBoolean());
    fs.setByteValue(akofByte, (byte) rnd.nextInt());
    fs.setShortValue(akofShort, (short) rnd.nextInt());
    fs.setIntValue(akofInt, rnd.nextInt());
    fs.setFloatValue(akofFloat, rnd.nextFloat());
    fs.setLongValue(akofLong, rnd.nextLong());
    fs.setDoubleValue(akofDouble, rnd.nextDouble());
    fs.setStringValue(akofString, randomString(rnd));
    fs.setFeatureValue(akofFs, fs);

    fs.setFeatureValue(akofAint, randomIntA(rnd));
    fs.setFeatureValue(akofAfs, maybeKeep(cas.createArrayFS(1)));
    fs.setFeatureValue(akofAfloat, randomFloatA(rnd));
    fs.setFeatureValue(akofAdouble, randomDoubleA(rnd));
    fs.setFeatureValue(akofAlong, randomLongA(rnd));
    fs.setFeatureValue(akofAshort, randomShortA(rnd));
    fs.setFeatureValue(akofAbyte, randomByteA(rnd));
    fs.setFeatureValue(akofAboolean, maybeKeep(cas.createBooleanArrayFS(2)));
    fs.setFeatureValue(akofAstring, randomStringA(rnd));

    if (isKeep) {
      ((TOP) fs).addToIndexes();
    }

    return fs;
  }

  private String randomString(Random r) {
    String v = STRING_VALUES[r.nextInt(STRING_VALUES.length)];

    switch (stringArrayMode) {
      case ALLOW_NULL_AND_EMPTY_STRINGS:
        return v;
      case EMPTY_STRINGS_AS_NULL:
        return v != null && v.isEmpty() ? null : v;
      case NULL_STRINGS_AS_EMPTY:
        return v == null ? "" : v;
      default:
        throw new IllegalArgumentException("Unsupported string array mode: " + stringArrayMode);
    }
  }

  private StringArrayFS randomStringA(Random r) {
    int length = r.nextInt(2) + 1;
    StringArrayFS fs = maybeKeep(cas.createStringArrayFS(length));
    for (int i = 0; i < length; i++) {
      fs.set(i, randomString(r));
    }
    return fs;
  }

  private IntArrayFS randomIntA(Random r) {
    int length = r.nextInt(2) + 1;
    IntArrayFS fs = maybeKeep(cas.createIntArrayFS(length));
    for (int i = 0; i < length; i++) {
      fs.set(i, r.nextInt(101) - 50);
    }
    return fs;
  }

  private ByteArrayFS randomByteA(Random r) {
    int length = r.nextInt(2) + 1;
    ByteArrayFS fs = maybeKeep(cas.createByteArrayFS(length));
    for (int i = 0; i < length; i++) {
      int bvidx = r.nextInt(BYTE_VALUES.length);
      fs.set(i, BYTE_VALUES[bvidx]);
    }
    return fs;
  }

  private LongArrayFS randomLongA(Random r) {
    int length = r.nextInt(2) + 1;
    LongArrayFS fs = maybeKeep(cas.createLongArrayFS(length));
    for (int i = 0; i < length; i++) {
      fs.set(i, LONG_VALUES[r.nextInt(LONG_VALUES.length)]);
    }
    return fs;
  }

  private ShortArrayFS randomShortA(Random r) {
    int length = r.nextInt(2) + 1;
    ShortArrayFS fs = maybeKeep(cas.createShortArrayFS(length));
    for (int i = 0; i < length; i++) {
      fs.set(i, SHORT_VALUES[r.nextInt(SHORT_VALUES.length)]);
    }
    return fs;
  }

  private DoubleArrayFS randomDoubleA(Random r) {
    int length = r.nextInt(2) + 1;
    DoubleArrayFS fs = maybeKeep(cas.createDoubleArrayFS(length));
    for (int i = 0; i < length; i++) {
      fs.set(i, DOUBLE_VALUES[r.nextInt(DOUBLE_VALUES.length)]);
    }
    return fs;
  }

  private FloatArrayFS randomFloatA(Random r) {
    int length = r.nextInt(2) + 1;
    FloatArrayFS fs = maybeKeep(cas.createFloatArrayFS(length));
    for (int i = 0; i < length; i++) {
      fs.set(i, FLOAT_VALUES[r.nextInt(FLOAT_VALUES.length)]);
    }
    return fs;
  }

  @SuppressWarnings("unchecked")
  private <T extends FeatureStructure> T maybeKeep(T aFS) {
    if (isKeep) {
      return aFS;
    }

    LowLevelCAS llCas = cas.getLowLevelCAS();
    return (T) llCas.ll_getFSForRef(llCas.ll_getFSRef(aFS));
  }

  /**
   * Creates builder to build {@link MultiFeatureRandomCasGenerator}.
   * 
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link MultiFeatureRandomCasGenerator}.
   */
  public static final class Builder {
    private boolean isKeep = true;
    private boolean includeUid;
    private Random randomGenerator;
    private int size;
    private StringArrayMode stringArrayMode = ALLOW_NULL_AND_EMPTY_STRINGS;

    private Builder() {
    }

    public Builder withReferenceKeeping(boolean aIsKeep) {
      isKeep = aIsKeep;
      return this;
    }

    public Builder withUid(boolean aIncludeUid) {
      includeUid = aIncludeUid;
      return this;
    }

    public Builder withRandomGenerator(Random aRandom) {
      randomGenerator = aRandom;
      return this;
    }

    public Builder withSize(int aSize) {
      size = aSize;
      return this;
    }

    public Builder withStringArrayMode(StringArrayMode aStringArrayMode) {
      stringArrayMode = aStringArrayMode;
      return this;
    }

    public MultiFeatureRandomCasGenerator build() {
      if (randomGenerator == null) {
        randomGenerator = new Random();
      }

      return new MultiFeatureRandomCasGenerator(this);
    }
  }

  public enum StringArrayMode {
    /**
     * Instead of generating an empty string, generate a {@code null} value (mainly for XCAS).
     */
    EMPTY_STRINGS_AS_NULL,

    /**
     * Instead of generating a {@code null} value, generate an empty string (mainly for XMI).
     */
    NULL_STRINGS_AS_EMPTY,

    /**
     * Generate both {@code null} values and empty strings (this is what (de)serializers should
     * normally support and be tested with).
     */
    ALLOW_NULL_AND_EMPTY_STRINGS;
  }
}
