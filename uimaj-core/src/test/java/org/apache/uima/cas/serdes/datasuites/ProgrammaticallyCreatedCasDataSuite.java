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
package org.apache.uima.cas.serdes.datasuites;

import static java.lang.Float.MAX_VALUE;
import static java.lang.Float.MIN_VALUE;
import static java.lang.Float.NEGATIVE_INFINITY;
import static java.lang.Float.NaN;
import static java.lang.Float.POSITIVE_INFINITY;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_DOUBLE;
import static org.apache.uima.cas.CAS.TYPE_NAME_FLOAT;
import static org.apache.uima.cas.CAS.TYPE_NAME_FLOAT_ARRAY;
import static org.apache.uima.cas.CAS.TYPE_NAME_FLOAT_LIST;
import static org.apache.uima.cas.CAS.TYPE_NAME_FS_ARRAY;
import static org.apache.uima.cas.CAS.TYPE_NAME_FS_LIST;
import static org.apache.uima.cas.CAS.TYPE_NAME_INTEGER;
import static org.apache.uima.cas.CAS.TYPE_NAME_INTEGER_ARRAY;
import static org.apache.uima.cas.CAS.TYPE_NAME_INTEGER_LIST;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_LIST;
import static org.apache.uima.cas.CAS.TYPE_NAME_TOP;
import static org.apache.uima.util.CasCreationUtils.createCas;

import java.nio.charset.StandardCharsets;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.serdes.transitions.CasSourceTargetConfiguration;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;

public class ProgrammaticallyCreatedCasDataSuite
        extends AbstractCollection<CasSourceTargetConfiguration> implements CasDataSuite {

  private final List<CasSourceTargetConfiguration> confs;

  private ProgrammaticallyCreatedCasDataSuite(Builder builder) {
    confs = asList( //
            CasSourceTargetConfiguration.builder() //
                    .withTitle("emptyCas") //
                    .withSourceCasSupplier(ProgrammaticallyCreatedCasDataSuite::emptyCas) //
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build(),
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithText") //
                    .withSourceCasSupplier(ProgrammaticallyCreatedCasDataSuite::casWithText) //
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build(),
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithoutTextButWithAnnotations") //
                    .withSourceCasSupplier(
                            ProgrammaticallyCreatedCasDataSuite::casWithoutTextButWithAnnotations)
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build(),
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithTextAndAnnotations") //
                    .withSourceCasSupplier(
                            ProgrammaticallyCreatedCasDataSuite::casWithTextAndAnnotations)
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build(),
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithEmojiUnicodeTextAndAnnotations") //
                    .withSourceCasSupplier(
                            ProgrammaticallyCreatedCasDataSuite::casWithEmojiUnicodeTextAndAnnotations)
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build(),
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithLeftToRightTextAndAnnotations") //
                    .withSourceCasSupplier(
                            ProgrammaticallyCreatedCasDataSuite::casWithLeftToRightTextAndAnnotations)
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build(),
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithTraditionalChineseTextAndAnnotations") //
                    .withSourceCasSupplier(
                            ProgrammaticallyCreatedCasDataSuite::casWithTraditionalChineseTextAndAnnotations)
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build(),
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithSofaDataURI") //
                    .withSourceCasSupplier(ProgrammaticallyCreatedCasDataSuite::casWithSofaDataURI)
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build(),
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithSofaDataArray") //
                    .withSourceCasSupplier(
                            ProgrammaticallyCreatedCasDataSuite::casWithSofaDataArray)
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build(),
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithFloatingPointSpecialValues") //
                    .withSourceCasSupplier(
                            ProgrammaticallyCreatedCasDataSuite::casWithFloatingPointSpecialValues)
                    .withTargetCasSupplier(() -> createCas(
                            typeSystemWithFloatingPointSpecialValues(), null, null, null)) //
                    .build(),
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithLists") //
                    .withSourceCasSupplier(ProgrammaticallyCreatedCasDataSuite::casWithListFeatures)
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build(),
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithArrays") //
                    .withSourceCasSupplier(
                            ProgrammaticallyCreatedCasDataSuite::casWithArrayFeatures)
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build());
  }

  @Override
  public Iterator<CasSourceTargetConfiguration> iterator() {
    return confs.iterator();
  }

  @Override
  public int size() {
    return confs.size();
  }

  public static CAS emptyCas() throws Exception {
    return CasCreationUtils.createCas();
  }

  public static CAS casWithText() throws Exception {
    CAS cas = CasCreationUtils.createCas();
    cas.setDocumentText("This is a test.");
    return cas;
  }

  public static CAS casWithoutTextButWithAnnotations() throws Exception {
    CAS cas = CasCreationUtils.createCas();
    StringBuilder sb = new StringBuilder();
    createAnnotatedText(cas, sb, "This", " ");
    createAnnotatedText(cas, sb, "is", " ");
    createAnnotatedText(cas, sb, "a", " ");
    createAnnotatedText(cas, sb, "test");
    // Not adding the text to the CAS here! This is intentional!
    return cas;
  }

  public static CAS casWithTextAndAnnotations() throws Exception {
    CAS cas = CasCreationUtils.createCas();
    StringBuilder sb = new StringBuilder();
    createAnnotatedText(cas, sb, "This", " ");
    createAnnotatedText(cas, sb, "is", " ");
    createAnnotatedText(cas, sb, "a", " ");
    createAnnotatedText(cas, sb, "test");
    cas.setDocumentText(sb.toString());
    return cas;
  }

  public static CAS casWithEmojiUnicodeTextAndAnnotations() throws Exception {
    CAS cas = CasCreationUtils.createCas();
    StringBuilder sb = new StringBuilder();
    createAnnotatedText(cas, sb, "🥳", " ");
    createAnnotatedText(cas, sb, "This", " ");
    createAnnotatedText(cas, sb, "👳🏻‍♀️", " ");
    createAnnotatedText(cas, sb, "is", " ");
    createAnnotatedText(cas, sb, "✆", " ");
    createAnnotatedText(cas, sb, "a", " ");
    createAnnotatedText(cas, sb, "🧔🏾‍♂️", " ");
    createAnnotatedText(cas, sb, "test", " ");
    createAnnotatedText(cas, sb, "👻");
    cas.setDocumentText(sb.toString());
    return cas;
  }

  public static CAS casWithLeftToRightTextAndAnnotations() throws Exception {
    CAS cas = CasCreationUtils.createCas();
    StringBuilder sb = new StringBuilder();
    // "this is a test" per Google Translate
    createAnnotatedText(cas, sb, "هذا", " ");
    createAnnotatedText(cas, sb, "اختبار");
    cas.setDocumentText(sb.toString());
    return cas;
  }

  public static CAS casWithTraditionalChineseTextAndAnnotations() throws Exception {
    CAS cas = CasCreationUtils.createCas();
    StringBuilder sb = new StringBuilder();
    // "This is a test" per Google Translate
    createAnnotatedText(cas, sb, "這");
    createAnnotatedText(cas, sb, "是");
    createAnnotatedText(cas, sb, "一個");
    createAnnotatedText(cas, sb, "測試");
    cas.setDocumentText(sb.toString());
    return cas;
  }

  public static CAS casWithSofaDataURI() throws Exception {
    CAS cas = CasCreationUtils.createCas();
    // NOTE: UIMA does not try to resolve the URI and also does not support "classpath:" URIs!
    cas.setSofaDataURI("classpath:/ProgrammaticallyCreatedCasDataSuite/document.txt", "text/plain");

    return cas;
  }

  public static CAS casWithSofaDataArray() throws Exception {
    CAS cas = CasCreationUtils.createCas();

    byte[] byteArray = "This is a test".getBytes(StandardCharsets.UTF_8);
    ByteArrayFS sofaDataArray = cas.createByteArrayFS(byteArray.length);
    for (int i = 0; i < byteArray.length; i++) {
      sofaDataArray.set(i, byteArray[i]);
    }

    cas.setSofaDataArray(sofaDataArray, "text/plain");

    return cas;
  }

  public static TypeSystemDescription typeSystemWithFloatingPointSpecialValues() throws Exception {
    TypeSystemDescription tsd = UIMAFramework.getResourceSpecifierFactory()
            .createTypeSystemDescription();

    TypeDescription typeDesc = tsd.addType("SpecialValuesType", null, TYPE_NAME_TOP);
    typeDesc.addFeature("doubleZero", null, TYPE_NAME_DOUBLE);
    typeDesc.addFeature("doubleOne", null, TYPE_NAME_DOUBLE);
    typeDesc.addFeature("doublePosInfinity", null, TYPE_NAME_DOUBLE);
    typeDesc.addFeature("doubleNegInfinity", null, TYPE_NAME_DOUBLE);
    typeDesc.addFeature("doubleNan", null, TYPE_NAME_DOUBLE);
    typeDesc.addFeature("floatZero", null, TYPE_NAME_FLOAT);
    typeDesc.addFeature("floatOne", null, TYPE_NAME_FLOAT);
    typeDesc.addFeature("floatPosInfinity", null, TYPE_NAME_FLOAT);
    typeDesc.addFeature("floatNegInfinity", null, TYPE_NAME_FLOAT);
    typeDesc.addFeature("floatNan", null, TYPE_NAME_FLOAT);

    return tsd;
  }

  public static TypeSystemDescription typeSystemWithListFeatures() throws Exception {
    TypeSystemDescription tsd = UIMAFramework.getResourceSpecifierFactory()
            .createTypeSystemDescription();

    TypeDescription fsListHolder = tsd.addType("FsListHolder", null, TYPE_NAME_TOP);
    fsListHolder.addFeature("fsList", null, TYPE_NAME_FS_LIST, TYPE_NAME_TOP, false);

    TypeDescription fsListHolderMr = tsd.addType("FsListHolderMR", null, TYPE_NAME_TOP);
    fsListHolderMr.addFeature("fsList", null, TYPE_NAME_FS_LIST, TYPE_NAME_TOP, true);

    TypeDescription intListHolder = tsd.addType("IntListHolder", null, TYPE_NAME_TOP);
    intListHolder.addFeature("intList", null, TYPE_NAME_INTEGER_LIST, TYPE_NAME_INTEGER, false);

    TypeDescription intListHolderMr = tsd.addType("IntListHolderMR", null, TYPE_NAME_TOP);
    intListHolderMr.addFeature("intList", null, TYPE_NAME_INTEGER_LIST, TYPE_NAME_INTEGER, true);

    TypeDescription floatListHolder = tsd.addType("FloatListHolder", null, TYPE_NAME_TOP);
    floatListHolder.addFeature("floatList", null, TYPE_NAME_FLOAT_LIST, TYPE_NAME_FLOAT, false);

    TypeDescription floatListHolderMr = tsd.addType("FloatListHolderMR", null, TYPE_NAME_TOP);
    floatListHolderMr.addFeature("floatList", null, TYPE_NAME_FLOAT_LIST, TYPE_NAME_FLOAT, true);

    TypeDescription stringListHolder = tsd.addType("StringListHolder", null, TYPE_NAME_TOP);
    stringListHolder.addFeature("stringList", null, TYPE_NAME_STRING_LIST, TYPE_NAME_STRING, false);

    TypeDescription stringListHolderMr = tsd.addType("StringListHolderMR", null, TYPE_NAME_TOP);
    stringListHolderMr.addFeature("stringList", null, TYPE_NAME_STRING_LIST, TYPE_NAME_STRING,
            true);

    return tsd;
  }

  public static TypeSystemDescription typeSystemWithArrayFeatures() throws Exception {
    TypeSystemDescription tsd = UIMAFramework.getResourceSpecifierFactory()
            .createTypeSystemDescription();

    TypeDescription fsArrayHolder = tsd.addType("FsArrayHolder", null, TYPE_NAME_TOP);
    fsArrayHolder.addFeature("fsArray", null, TYPE_NAME_FS_ARRAY, TYPE_NAME_TOP, false);

    TypeDescription fsArrayHolderMr = tsd.addType("FsArrayHolderMR", null, TYPE_NAME_TOP);
    fsArrayHolderMr.addFeature("fsArray", null, TYPE_NAME_FS_ARRAY, TYPE_NAME_TOP, true);

    TypeDescription intArrayHolder = tsd.addType("IntArrayHolder", null, TYPE_NAME_TOP);
    intArrayHolder.addFeature("intArray", null, TYPE_NAME_INTEGER_ARRAY, TYPE_NAME_INTEGER, false);

    TypeDescription intArrayHolderMr = tsd.addType("IntArrayHolderMR", null, TYPE_NAME_TOP);
    intArrayHolderMr.addFeature("intArray", null, TYPE_NAME_INTEGER_ARRAY, TYPE_NAME_INTEGER, true);

    TypeDescription floatArrayHolder = tsd.addType("FloatArrayHolder", null, TYPE_NAME_TOP);
    floatArrayHolder.addFeature("floatArray", null, TYPE_NAME_FLOAT_ARRAY, TYPE_NAME_FLOAT, false);

    TypeDescription floatArrayHolderMr = tsd.addType("FloatArrayHolderMR", null, TYPE_NAME_TOP);
    floatArrayHolderMr.addFeature("floatArray", null, TYPE_NAME_FLOAT_ARRAY, TYPE_NAME_FLOAT, true);

    TypeDescription stringArrayHolder = tsd.addType("StringArrayHolder", null, TYPE_NAME_TOP);
    stringArrayHolder.addFeature("stringArray", null, TYPE_NAME_STRING_ARRAY, TYPE_NAME_STRING,
            false);

    TypeDescription stringArrayHolderMr = tsd.addType("StringArrayHolderMR", null, TYPE_NAME_TOP);
    stringArrayHolderMr.addFeature("stringArray", null, TYPE_NAME_STRING_ARRAY, TYPE_NAME_STRING,
            true);

    return tsd;
  }

  public static CAS casWithFloatingPointSpecialValues() throws Exception {
    CAS cas = createCas(typeSystemWithFloatingPointSpecialValues(), null, null, null);

    Type type = cas.getTypeSystem().getType("SpecialValuesType");
    FeatureStructure fs = cas.createFS(type);
    fs.setDoubleValue(type.getFeatureByBaseName("doubleZero"), 0.0);
    fs.setDoubleValue(type.getFeatureByBaseName("doubleOne"), 1.0);
    fs.setDoubleValue(type.getFeatureByBaseName("doublePosInfinity"), Double.POSITIVE_INFINITY);
    fs.setDoubleValue(type.getFeatureByBaseName("doubleNegInfinity"), Double.NEGATIVE_INFINITY);
    fs.setDoubleValue(type.getFeatureByBaseName("doubleNan"), Double.NaN);
    fs.setFloatValue(type.getFeatureByBaseName("floatZero"), 0.0f);
    fs.setFloatValue(type.getFeatureByBaseName("floatOne"), 1.0f);
    fs.setFloatValue(type.getFeatureByBaseName("floatPosInfinity"), Float.POSITIVE_INFINITY);
    fs.setFloatValue(type.getFeatureByBaseName("floatNegInfinity"), Float.NEGATIVE_INFINITY);
    fs.setFloatValue(type.getFeatureByBaseName("floatNan"), Float.NaN);
    cas.addFsToIndexes(fs);

    DoubleArrayFS doubleArrayFs = cas.createDoubleArrayFS(5);
    doubleArrayFs.set(0, 0.0);
    doubleArrayFs.set(1, 1.0);
    doubleArrayFs.set(2, Double.NEGATIVE_INFINITY);
    doubleArrayFs.set(3, Double.POSITIVE_INFINITY);
    doubleArrayFs.set(4, Double.NaN);
    cas.addFsToIndexes(doubleArrayFs);

    FloatArrayFS floatArrayFs = cas.createFloatArrayFS(5);
    floatArrayFs.set(0, 0.0f);
    floatArrayFs.set(1, 1.0f);
    floatArrayFs.set(2, Float.NEGATIVE_INFINITY);
    floatArrayFs.set(3, Float.POSITIVE_INFINITY);
    floatArrayFs.set(4, Float.NaN);
    cas.addFsToIndexes(floatArrayFs);

    return cas;
  }

  public static CAS casWithListFeatures() throws Exception {
    CAS cas = createCas(typeSystemWithListFeatures(), null, null, null);

    AnnotationFS a1 = cas.createAnnotation(cas.getAnnotationType(), 0, 1);
    AnnotationFS a2 = cas.createAnnotation(cas.getAnnotationType(), 1, 2);
    cas.addFsToIndexes(cas.emptyFSList().push((TOP) a1).push((TOP) a2));

    Type fsListHolderType = cas.getTypeSystem().getType("FsListHolder");
    FeatureStructure fsListHolder = cas.createFS(fsListHolderType);
    fsListHolder.setFeatureValue(fsListHolderType.getFeatureByBaseName("fsList"),
            cas.emptyFSList().push((TOP) a1).push((TOP) a2));
    cas.addFsToIndexes(fsListHolder);

    Type fsListHolderMrType = cas.getTypeSystem().getType("FsListHolderMR");
    FeatureStructure fsListHolderMr = cas.createFS(fsListHolderMrType);
    fsListHolderMr.setFeatureValue(fsListHolderMrType.getFeatureByBaseName("fsList"),
            cas.emptyFSList().push((TOP) a1).push((TOP) a2));
    cas.addFsToIndexes(fsListHolderMr);

    Type intListHolderType = cas.getTypeSystem().getType("IntListHolder");
    FeatureStructure intListHolder = cas.createFS(intListHolderType);
    intListHolder.setFeatureValue(intListHolderType.getFeatureByBaseName("intList"),
            cas.emptyIntegerList().push(1).push(2));
    cas.addFsToIndexes(intListHolder);

    Type intListHolderMrType = cas.getTypeSystem().getType("IntListHolderMR");
    FeatureStructure intListHolderMr = cas.createFS(intListHolderMrType);
    intListHolderMr.setFeatureValue(intListHolderMrType.getFeatureByBaseName("intList"),
            cas.emptyIntegerList().push(1).push(2));
    cas.addFsToIndexes(intListHolderMr);

    Type floatListHolderType = cas.getTypeSystem().getType("FloatListHolder");
    FeatureStructure floatListHolder = cas.createFS(floatListHolderType);
    floatListHolder.setFeatureValue(floatListHolderType.getFeatureByBaseName("floatList"),
            cas.emptyFloatList().push(-1.0f).push(0.0f).push(1.0f).push(NaN).push(NEGATIVE_INFINITY)
                    .push(POSITIVE_INFINITY).push(MIN_VALUE).push(MAX_VALUE));
    cas.addFsToIndexes(floatListHolder);

    Type floatListHolderMrType = cas.getTypeSystem().getType("FloatListHolderMR");
    FeatureStructure floatListHolderMr = cas.createFS(floatListHolderMrType);
    floatListHolderMr.setFeatureValue(floatListHolderMrType.getFeatureByBaseName("floatList"),
            cas.emptyFloatList().push(-1.0f).push(0.0f).push(1.0f).push(NaN).push(NEGATIVE_INFINITY)
                    .push(POSITIVE_INFINITY).push(MIN_VALUE).push(MAX_VALUE));
    cas.addFsToIndexes(floatListHolderMr);

    // We do not push null or the empty string here because different formats handle it differently
    // and we have tests for that in other places
    Type stringListHolderType = cas.getTypeSystem().getType("StringListHolder");
    FeatureStructure stringListHolder = cas.createFS(stringListHolderType);
    stringListHolder.setFeatureValue(stringListHolderType.getFeatureByBaseName("stringList"),
            cas.emptyStringList().push("blah").push("blub"));
    cas.addFsToIndexes(stringListHolder);

    Type stringListHolderMrType = cas.getTypeSystem().getType("StringListHolderMR");
    FeatureStructure stringListHolderMr = cas.createFS(stringListHolderMrType);
    stringListHolderMr.setFeatureValue(stringListHolderMrType.getFeatureByBaseName("stringList"),
            cas.emptyStringList().push("blah").push("blub"));
    cas.addFsToIndexes(stringListHolderMr);

    return cas;
  }

  public static CAS casWithArrayFeatures() throws Exception {
    CAS cas = createCas(typeSystemWithArrayFeatures(), null, null, null);

    AnnotationFS a1 = cas.createAnnotation(cas.getAnnotationType(), 0, 1);
    AnnotationFS a2 = cas.createAnnotation(cas.getAnnotationType(), 1, 2);
    FeatureStructure[] fsArrayValue = { a1, a2 };

    ArrayFS<FeatureStructure> arrayFs = cas.createArrayFS(fsArrayValue.length);
    arrayFs.copyFromArray(fsArrayValue, 0, 0, fsArrayValue.length);
    cas.addFsToIndexes(arrayFs);

    Type fsArrayHolderType = cas.getTypeSystem().getType("FsArrayHolder");
    FeatureStructure fsArrayHolder = cas.createFS(fsArrayHolderType);
    ArrayFS<FeatureStructure> arrayFs1 = cas.createArrayFS(fsArrayValue.length);
    arrayFs1.copyFromArray(fsArrayValue, 0, 0, fsArrayValue.length);
    fsArrayHolder.setFeatureValue(fsArrayHolderType.getFeatureByBaseName("fsArray"), arrayFs1);
    cas.addFsToIndexes(fsArrayHolder);

    FeatureStructure fsArrayHolderEmpty = cas.createFS(fsArrayHolderType);
    fsArrayHolderEmpty.setFeatureValue(fsArrayHolderType.getFeatureByBaseName("fsArray"),
            cas.emptyFSArray());
    cas.addFsToIndexes(fsArrayHolder);

    Type fsArrayHolderMrType = cas.getTypeSystem().getType("FsArrayHolderMR");
    FeatureStructure fsArrayHolderMr = cas.createFS(fsArrayHolderMrType);
    ArrayFS<FeatureStructure> arrayFsMr = cas.createArrayFS(fsArrayValue.length);
    arrayFsMr.copyFromArray(fsArrayValue, 0, 0, fsArrayValue.length);
    fsArrayHolderMr.setFeatureValue(fsArrayHolderMrType.getFeatureByBaseName("fsArray"), arrayFsMr);
    cas.addFsToIndexes(fsArrayHolderMr);

    int[] intArrayValue = { 1, 2 };

    Type intArrayHolderType = cas.getTypeSystem().getType("IntArrayHolder");
    FeatureStructure intArrayHolder = cas.createFS(intArrayHolderType);
    IntArrayFS intArray = cas.createIntArrayFS(intArrayValue.length);
    intArray.copyFromArray(intArrayValue, 0, 0, intArrayValue.length);
    intArrayHolder.setFeatureValue(intArrayHolderType.getFeatureByBaseName("intArray"), intArray);
    cas.addFsToIndexes(intArrayHolder);

    FeatureStructure intArrayHolderEmpty = cas.createFS(intArrayHolderType);
    intArrayHolderEmpty.setFeatureValue(intArrayHolderType.getFeatureByBaseName("intArray"),
            cas.emptyIntegerArray());
    cas.addFsToIndexes(intArrayHolderEmpty);

    Type intArrayHolderMrType = cas.getTypeSystem().getType("IntArrayHolderMR");
    FeatureStructure intArrayHolderMr = cas.createFS(intArrayHolderMrType);
    IntArrayFS intArrayMr = cas.createIntArrayFS(intArrayValue.length);
    intArrayMr.copyFromArray(intArrayValue, 0, 0, intArrayValue.length);
    intArrayHolderMr.setFeatureValue(intArrayHolderMrType.getFeatureByBaseName("intArray"),
            intArrayMr);
    cas.addFsToIndexes(intArrayHolderMr);

    float[] floatArrayValue = { -1.0f, 0.0f, 1.0f, NaN, NEGATIVE_INFINITY, POSITIVE_INFINITY,
        MIN_VALUE, MAX_VALUE };

    Type floatArrayHolderType = cas.getTypeSystem().getType("FloatArrayHolder");
    FeatureStructure floatArrayHolder = cas.createFS(floatArrayHolderType);
    FloatArrayFS floatArray = cas.createFloatArrayFS(floatArrayValue.length);
    floatArray.copyFromArray(floatArrayValue, 0, 0, floatArrayValue.length);
    floatArrayHolder.setFeatureValue(floatArrayHolderType.getFeatureByBaseName("floatArray"),
            floatArray);
    cas.addFsToIndexes(floatArrayHolder);

    FeatureStructure floatArrayHolderEmpty = cas.createFS(floatArrayHolderType);
    floatArrayHolderEmpty.setFeatureValue(floatArrayHolderType.getFeatureByBaseName("floatArray"),
            cas.emptyFloatArray());
    cas.addFsToIndexes(floatArrayHolderEmpty);

    Type floatArrayHolderMrType = cas.getTypeSystem().getType("FloatArrayHolderMR");
    FeatureStructure floatArrayHolderMr = cas.createFS(floatArrayHolderMrType);
    FloatArrayFS floatArrayMr = cas.createFloatArrayFS(floatArrayValue.length);
    floatArrayMr.copyFromArray(floatArrayValue, 0, 0, floatArrayValue.length);
    floatArrayHolderMr.setFeatureValue(floatArrayHolderMrType.getFeatureByBaseName("floatArray"),
            floatArrayMr);
    cas.addFsToIndexes(floatArrayHolderMr);

    String[] stringArrayValue = { "blah", "blub" };

    // We do not push null or the empty string here because different formats handle it differently
    // and we have tests for that in other places
    Type stringArrayHolderType = cas.getTypeSystem().getType("StringArrayHolder");
    FeatureStructure stringArrayHolder = cas.createFS(stringArrayHolderType);
    StringArrayFS stringArray = cas.createStringArrayFS(stringArrayValue.length);
    stringArray.copyFromArray(stringArrayValue, 0, 0, stringArrayValue.length);
    stringArrayHolder.setFeatureValue(stringArrayHolderType.getFeatureByBaseName("stringArray"),
            stringArray);
    cas.addFsToIndexes(stringArrayHolder);

    FeatureStructure stringArrayHolderEmpty = cas.createFS(stringArrayHolderType);
    stringArrayHolderEmpty.setFeatureValue(
            stringArrayHolderType.getFeatureByBaseName("stringArray"), cas.emptyStringArray());
    cas.addFsToIndexes(stringArrayHolderEmpty);

    Type stringArrayHolderMrType = cas.getTypeSystem().getType("StringArrayHolderMR");
    FeatureStructure stringArrayHolderMr = cas.createFS(stringArrayHolderMrType);
    StringArrayFS stringArrayMr = cas.createStringArrayFS(stringArrayValue.length);
    stringArrayMr.copyFromArray(stringArrayValue, 0, 0, stringArrayValue.length);
    stringArrayHolderMr.setFeatureValue(stringArrayHolderMrType.getFeatureByBaseName("stringArray"),
            stringArrayMr);
    cas.addFsToIndexes(stringArrayHolderMr);

    return cas;
  }

  private static void createAnnotatedText(CAS aCas, StringBuilder aBuffer, String aText,
          String... aSuffix) {
    int begin = aBuffer.length();
    aBuffer.append(aText);
    AnnotationFS a = aCas.createAnnotation(aCas.getAnnotationType(), begin, aBuffer.length());
    aCas.addFsToIndexes(a);
    for (String s : aSuffix) {
      aBuffer.append(s);
    }
  }

  /**
   * Creates builder to build {@link ProgrammaticallyCreatedCasDataSuite}.
   * 
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link ProgrammaticallyCreatedCasDataSuite}.
   */
  public static final class Builder {
    private Builder() {
    }

    public ProgrammaticallyCreatedCasDataSuite build() {
      return new ProgrammaticallyCreatedCasDataSuite(this);
    }
  }
}
