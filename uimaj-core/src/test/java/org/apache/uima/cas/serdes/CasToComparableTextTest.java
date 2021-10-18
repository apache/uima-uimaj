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
package org.apache.uima.cas.serdes;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Test;

public class CasToComparableTextTest {
  private static final String TYPE_NAME_AKOF = "akof";
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

  @Test
  public void thatPrimitiveValuesWork() throws Exception {
    TypeSystemDescription tsd = UIMAFramework.getResourceSpecifierFactory()
            .createTypeSystemDescription();

    TypeDescription akofTD = tsd.addType(TYPE_NAME_AKOF, null, CAS.TYPE_NAME_TOP);

    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_INT, null, CAS.TYPE_NAME_INTEGER);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_FLOAT, null, CAS.TYPE_NAME_FLOAT);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_DOUBLE, null, CAS.TYPE_NAME_DOUBLE);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_LONG, null, CAS.TYPE_NAME_LONG);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_SHORT, null, CAS.TYPE_NAME_SHORT);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_BYTE, null, CAS.TYPE_NAME_BYTE);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_BOOLEAN, null, CAS.TYPE_NAME_BOOLEAN);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_STRING, null, CAS.TYPE_NAME_STRING);

    // akofTD.addFeature(FEATURE_BASE_NAME_AKOF_FS, null, CAS.TYPE_NAME_TOP);
    // akofTD.addFeature(FEATURE_BASE_NAME_AKOF_FS_ARRAY, null, CAS.TYPE_NAME_FS_ARRAY);

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type t = cas.getTypeSystem().getType(TYPE_NAME_AKOF);
    FeatureStructure fs = cas.createFS(t);

    fs.setIntValue(t.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_INT), 1);
    fs.setFloatValue(t.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_FLOAT), 10.2321321f);
    fs.setDoubleValue(t.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_DOUBLE), 10.2321321);
    fs.setLongValue(t.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_LONG), 10_000_000);
    fs.setShortValue(t.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_SHORT), (short) 31000);
    fs.setByteValue(t.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_BYTE), (byte) 64);
    fs.setBooleanValue(t.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_BOOLEAN), true);
    fs.setStringValue(t.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_STRING), "dummy");

    cas.addFsToIndexes(fs);

    String result = CasToComparableText.toComparableString(cas);

    assertThat(result).contains("akof*,true,64,10.2321321,10.232132,1,10000000,31000,dummy");
  }

  @Test
  public void thatPrimitiveWork() throws Exception {
    TypeSystemDescription tsd = UIMAFramework.getResourceSpecifierFactory()
            .createTypeSystemDescription();

    TypeDescription akofTD = tsd.addType(TYPE_NAME_AKOF, null, CAS.TYPE_NAME_TOP);

    // akofTD.addFeature(FEATURE_BASE_NAME_AKOF_INT, null, CAS.TYPE_NAME_INTEGER);
    // akofTD.addFeature(FEATURE_BASE_NAME_AKOF_FS, null, CAS.TYPE_NAME_TOP);
    // akofTD.addFeature(FEATURE_BASE_NAME_AKOF_FLOAT, null, CAS.TYPE_NAME_FLOAT);
    // akofTD.addFeature(FEATURE_BASE_NAME_AKOF_DOUBLE, null, CAS.TYPE_NAME_DOUBLE);
    // akofTD.addFeature(FEATURE_BASE_NAME_AKOF_LONG, null, CAS.TYPE_NAME_LONG);
    // akofTD.addFeature(FEATURE_BASE_NAME_AKOF_SHORT, null, CAS.TYPE_NAME_SHORT);
    // akofTD.addFeature(FEATURE_BASE_NAME_AKOF_BYTE, null, CAS.TYPE_NAME_BYTE);
    // akofTD.addFeature(FEATURE_BASE_NAME_AKOF_BOOLEAN, null, CAS.TYPE_NAME_BOOLEAN);
    // akofTD.addFeature(FEATURE_BASE_NAME_AKOF_STRING, null, CAS.TYPE_NAME_STRING);
    // akofTD.addFeature(FEATURE_BASE_NAME_AKOF_FS_ARRAY, null, CAS.TYPE_NAME_FS_ARRAY);

    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_INT_ARRAY, null, CAS.TYPE_NAME_INTEGER_ARRAY);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_FLOAT_ARRAY, null, CAS.TYPE_NAME_FLOAT_ARRAY);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_DOUBLE_ARRAY, null, CAS.TYPE_NAME_DOUBLE_ARRAY);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_LONG_ARRAY, null, CAS.TYPE_NAME_LONG_ARRAY);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_SHORT_ARRAY, null, CAS.TYPE_NAME_SHORT_ARRAY);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_BYTE_ARRAY, null, CAS.TYPE_NAME_BYTE_ARRAY);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_BOOLEAN_ARRAY, null, CAS.TYPE_NAME_BOOLEAN_ARRAY);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_STRING_ARRAY, null, CAS.TYPE_NAME_STRING_ARRAY);

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type t = cas.getTypeSystem().getType(TYPE_NAME_AKOF);
    FeatureStructure fs = cas.createFS(t);

    IntArrayFS intArrayFs = cas.createIntArrayFS(1);
    intArrayFs.set(0, 1);
    fs.setFeatureValue(t.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_INT_ARRAY), intArrayFs);

    FloatArrayFS floatArrayFs = cas.createFloatArrayFS(1);
    floatArrayFs.set(0, 10.2321321f);
    fs.setFeatureValue(t.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_FLOAT_ARRAY), floatArrayFs);

    DoubleArrayFS doubleArrayFs = cas.createDoubleArrayFS(1);
    doubleArrayFs.set(0, 10.2321321);
    fs.setFeatureValue(t.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_DOUBLE_ARRAY), doubleArrayFs);

    LongArrayFS longArrayFs = cas.createLongArrayFS(1);
    longArrayFs.set(0, 10_000_000);
    fs.setFeatureValue(t.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_LONG_ARRAY), longArrayFs);

    ShortArrayFS shortArrayFs = cas.createShortArrayFS(1);
    shortArrayFs.set(0, (short) 31000);
    fs.setFeatureValue(t.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_SHORT_ARRAY), shortArrayFs);

    ByteArrayFS byteArrayFs = cas.createByteArrayFS(1);
    byteArrayFs.set(0, (byte) 64);
    fs.setFeatureValue(t.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_BYTE_ARRAY), byteArrayFs);

    BooleanArrayFS booleanArrayFs = cas.createBooleanArrayFS(1);
    booleanArrayFs.set(0, true);
    fs.setFeatureValue(t.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_BOOLEAN_ARRAY),
            booleanArrayFs);

    StringArrayFS stringArrayFs = cas.createStringArrayFS(1);
    stringArrayFs.set(0, "dummy");
    fs.setFeatureValue(t.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_STRING_ARRAY), stringArrayFs);

    cas.addFsToIndexes(fs);

    String result = CasToComparableText.toComparableString(cas);

    assertThat(result)
            .contains("akof*,[true],[64],[10.2321321],[10.232132],[1],[10000000],[31000],[dummy]");
  }

  @Test
  public void thatReferenceValuesAndArraysWork() throws Exception {
    TypeSystemDescription tsd = UIMAFramework.getResourceSpecifierFactory()
            .createTypeSystemDescription();

    TypeDescription akofTD = tsd.addType(TYPE_NAME_AKOF, null, CAS.TYPE_NAME_TOP);

    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_FS, null, CAS.TYPE_NAME_TOP);
    akofTD.addFeature(FEATURE_BASE_NAME_AKOF_FS_ARRAY, null, CAS.TYPE_NAME_FS_ARRAY);

    CAS cas = CasCreationUtils.createCas(tsd, null, null, null);

    Type t = cas.getTypeSystem().getType(TYPE_NAME_AKOF);
    FeatureStructure fs = cas.createFS(t);

    fs.setFeatureValue(t.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_FS), fs);

    ArrayFS<FeatureStructure> arrayFs = cas.createArrayFS(1);
    arrayFs.set(0, fs);
    fs.setFeatureValue(t.getFeatureByBaseName(FEATURE_BASE_NAME_AKOF_FS_ARRAY), arrayFs);

    cas.addFsToIndexes(fs);

    String result = CasToComparableText.toComparableString(cas);

    assertThat(result).contains("akof*,[akof*],akof*");
  }
}
