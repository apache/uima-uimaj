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
package org.apache.uima.util;

import static java.util.Arrays.asList;
import static org.apache.uima.UIMAFramework.getXMLParser;
import static org.apache.uima.cas.SerialFormat.COMPRESSED_FILTERED_TSI;
import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.apache.uima.util.CasLoadMode.DEFAULT;
import static org.apache.uima.util.CasLoadMode.LENIENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CasIOUtilsTest {

  private static final int SIMPLE_CAS_DEFAULT_INDEX_SIZE = 7;
  private static final int SIMPLE_CAS_DEFAULT_INDEX_SIZE_LENIENT = 5;
  private static final int SIMPLE_CAS_ALL_INDEXED_SIZE = 8;
  private static final int SIMPLE_CAS_ALL_INDEXED_SIZE_LENIENT = 6;

  private CAS cas;
  private CAS cas2;

  @BeforeEach
  void setUp() throws Exception {

    var indexes = getXMLParser()
            .parseFsIndexCollection(
                    new XMLInputSource(getClass().getResource("/ExampleCas/testIndexes.xml")))
            .getFsIndexes();

    var typeSystem = getXMLParser().parseTypeSystemDescription(
            new XMLInputSource(getClass().getResource("/ExampleCas/testTypeSystem.xml")));

    cas = createCas(typeSystem, new TypePriorities_impl(), indexes);

    try (FileInputStream casInputStream = new FileInputStream(
            JUnitExtension.getFile("ExampleCas/simpleCas.xmi"))) {
      CasIOUtils.load(casInputStream, cas);
    }

    var typeSystem2 = getXMLParser().parseTypeSystemDescription(
            new XMLInputSource(getClass().getResource("/ExampleCas/testTypeSystem_variation.xml")));
    cas2 = createCas(typeSystem2, new TypePriorities_impl(), indexes);
  }

  @AfterEach
  public void tearDown() throws Exception {
    cas.release();
  }

  @Test
  void testXMI() throws Exception {
    testXMI(false);
  }

  @Test
  void testXMILenient() throws Exception {
    testXMI(true);
  }

  void testXMI(boolean leniently) throws Exception {
    var casFile = new File("target/temp-test-output/simpleCas.xmi");
    casFile.getParentFile().mkdirs();
    try (var docOS = new FileOutputStream(casFile)) {
      CasIOUtils.save(cas, docOS, SerialFormat.XMI);
    }
    // NOTE - when Saxon saves the cas it omits the prefixes.
    // e.g. produces: <NULL id="0"/> instead of: <cas:NULL xmi:id="0"/>
    // This causes JUnit test failure "unknown type NULL"

    // Use a CAS initialized with the "correct" type system or with a different type system?
    var casToUse = leniently ? cas2 : cas;

    casToUse.reset();
    try (var is = new FileInputStream(casFile)) {
      CasIOUtils.load(is, null, casToUse, leniently ? LENIENT : DEFAULT);
    }
    assertCorrectlyLoaded(casToUse, leniently);

    casToUse.reset();
    CasIOUtils.load(casFile.toURI().toURL(), null, casToUse, leniently ? LENIENT : DEFAULT);
    assertCorrectlyLoaded(casToUse, leniently);
  }

  @ValueSource(booleans = { true, false })
  @ParameterizedTest
  void testXCAS(boolean leniently) throws Exception {
    var casFile = new File("target/temp-test-output/simpleCas.xcas");
    casFile.getParentFile().mkdirs();
    try (var os = new FileOutputStream(casFile)) {
      CasIOUtils.save(cas, os, SerialFormat.XCAS);
    }

    // Use a CAS initialized with the "correct" type system or with a different type system?
    var casToUse = leniently ? cas2 : cas;

    casToUse.reset();
    CasIOUtils.load(casFile.toURI().toURL(), null, casToUse, leniently ? LENIENT : DEFAULT);
    assertCorrectlyLoaded(casToUse, leniently);
  }

  @Test
  void testS() throws Exception {
    testFormat(SerialFormat.SERIALIZED, "bins", false);
  }

  @Test
  void testSp() throws Exception {
    testFormat(SerialFormat.SERIALIZED_TSI, "binsp", false);
  }

  @Test
  void testS6p() throws Exception {
    testFormat(SerialFormat.COMPRESSED_FILTERED_TSI, "bins6p", false);
  }

  @Test
  void testS6pTs() throws Exception {
    testFormat(SerialFormat.COMPRESSED_FILTERED_TS, "bins6pTs", false);
  }

  @Test
  void testS6pLenient() throws Exception {
    testFormat(SerialFormat.COMPRESSED_FILTERED_TSI, "bins6", true);
  }

  @Test
  void testS0() throws Exception {
    testFormat(SerialFormat.BINARY, "bins0", false);
  }

  @Test
  void testS0tsi() throws Exception {
    testFormat(SerialFormat.BINARY_TSI, "bins0", false);
  }

  @Test
  void testS4() throws Exception {
    testFormat(SerialFormat.COMPRESSED, "bins4", false);
  }

  @Test
  void testS4tsi() throws Exception {
    testFormat(SerialFormat.COMPRESSED_TSI, "bins4", false);
  }

  @Test
  void testS6() throws Exception {
    testFormat(SerialFormat.COMPRESSED_FILTERED, "bins6", false);
  }

  private void testFormat(SerialFormat format, String fileEnding, boolean leniently)
          throws Exception {
    var casFile = new File("target/temp-test-output/simpleCas." + fileEnding);
    casFile.getParentFile().mkdirs();
    try (var os = new FileOutputStream(casFile)) {
      CasIOUtils.save(cas, os, format);
    }

    // Use a CAS initialized with the "correct" type system or with a different type system?
    CAS casToUse = leniently ? cas2 : cas;
    casToUse.reset();

    try (var is = new FileInputStream(casFile)) {
      var loadedFormat = CasIOUtils.load(is, null, casToUse, leniently ? LENIENT : DEFAULT);
      assertThat(loadedFormat).isEqualTo(format);
      assertCorrectlyLoaded(casToUse, leniently);
    }
  }

  private static void assertCorrectlyLoaded(CAS cas, boolean leniently) throws Exception {
    // Check if all the annotations are there (mind the file contains FSes that are NOT
    // annotations!)
    assertThat(cas.getAnnotationIndex()).hasSize(
            leniently ? SIMPLE_CAS_DEFAULT_INDEX_SIZE_LENIENT : SIMPLE_CAS_DEFAULT_INDEX_SIZE);

    // Count ALL FSes now, including the ones that are not annotations!
    List<String> expectedTypes = new ArrayList<>(asList("org.apache.uima.testTypeSystem.Entity",
            "org.apache.uima.testTypeSystem.Organization", "org.apache.uima.testTypeSystem.Owner",
            "org.apache.uima.testTypeSystem.Person", "uima.tcas.DocumentAnnotation"));

    if (leniently) {
      // This type was renamed to "org.apache.uima.testTypeSystem.OwnerRenamed"
      expectedTypes.remove("org.apache.uima.testTypeSystem.Owner");
    }

    List<String> fsTypes = new ArrayList<>();
    // FSIterator<FeatureStructure> fsi = cas.getIndexRepository()
    // .getAllIndexedFS(cas.getTypeSystem().getTopType());
    Collection<TOP> s = cas.getIndexedFSs();
    Iterator<TOP> fsi = s.iterator();
    int fsCount = 0;
    while (fsi.hasNext()) {
      TOP fs = (TOP) fsi.next();
      String typeName = fs.getType().getName();
      if (!fsTypes.contains(typeName)) {
        fsTypes.add(typeName);
      }
      fsCount++;
    }
    Collections.sort(fsTypes);

    assertThat(fsCount).isEqualTo(
            leniently ? SIMPLE_CAS_ALL_INDEXED_SIZE_LENIENT : SIMPLE_CAS_ALL_INDEXED_SIZE);

    assertThat(fsTypes).isEqualTo(expectedTypes);
  }

  @Test
  void testWrongInputStream() throws Exception {
    byte[] casBytes;
    try (var bos = new ByteArrayOutputStream(); var os = new ObjectOutputStream(bos)) {
      os.writeObject("WRONG OBJECT");
      casBytes = bos.toByteArray();
    }

    try (ByteArrayInputStream casInputStream = new ByteArrayInputStream(casBytes)) {
      assertThatExceptionOfType(CASRuntimeException.class) //
              .isThrownBy(() -> CasIOUtils.load(casInputStream, cas)).satisfies(e -> {
                assertThat(e.getMessageKey()).isEqualTo("UNRECOGNIZED_SERIALIZED_CAS_FORMAT");
              });

    }
  }

  @Test
  void testWrongFormat() throws Exception {
    ThrowingCallable testFunc = () -> {
      try (var os = new ByteArrayOutputStream()) {
        CasIOUtils.save(cas, os, SerialFormat.UNKNOWN);
      }
    };

    assertThatExceptionOfType(IOException.class) //
            .isThrownBy(testFunc) //
            .withRootCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testDocumentAnnotationIsNotResurrected() throws Exception {
    var refererAnnoTypeName = "org.apache.uima.testing.Referer";
    var customDocAnnoTypeName = "org.apache.uima.testing.CustomDocumentAnnotation";

    var tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
    tsd.addType(customDocAnnoTypeName, "", CAS.TYPE_NAME_DOCUMENT_ANNOTATION);
    var refererType = tsd.addType(refererAnnoTypeName, "", CAS.TYPE_NAME_TOP);
    refererType.addFeature("ref", "", CAS.TYPE_NAME_DOCUMENT_ANNOTATION);

    CAS cas = createCas(tsd, null, null);

    // Initialize the default document annotation
    // ... then immediately remove it from the indexes.
    var da = cas.getDocumentAnnotation();

    assertThat(cas.select(cas.getTypeSystem().getType(CAS.TYPE_NAME_DOCUMENT_ANNOTATION)).asList())
            .extracting(fs -> fs.getType().getName())
            .containsExactly(CAS.TYPE_NAME_DOCUMENT_ANNOTATION);

    // Add a feature structure that references the original document annotation before we remove
    // it from the indexes
    var referer = cas.createFS(cas.getTypeSystem().getType(refererAnnoTypeName));
    referer.setFeatureValue(referer.getType().getFeatureByBaseName("ref"), da);
    cas.addFsToIndexes(referer);

    cas.removeFsFromIndexes(da);

    // Now add a new document annotation of our custom type
    var cda = cas.createFS(cas.getTypeSystem().getType(customDocAnnoTypeName));
    cas.addFsToIndexes(cda);

    assertThat(cas.select(cas.getTypeSystem().getType(CAS.TYPE_NAME_DOCUMENT_ANNOTATION)).asList())
            .extracting(fs -> fs.getType().getName()).containsExactly(customDocAnnoTypeName);

    // Serialize to a buffer
    byte[] data;
    try (var bos = new ByteArrayOutputStream()) {
      CasIOUtils.save(cas, bos, SerialFormat.SERIALIZED_TSI);
      data = bos.toByteArray();
    }

    // Deserialize from the buffer
    try (var bis = new ByteArrayInputStream(data)) {
      CasIOUtils.load(bis, cas);
    }

    assertThat(cas.select(cas.getTypeSystem().getType(CAS.TYPE_NAME_DOCUMENT_ANNOTATION)).asList())
            .extracting(fs -> fs.getType().getName()) //
            .containsExactly(customDocAnnoTypeName);
  }

  @Test
  void thatBinaryForm6DoesOnlyIncludeReachableFSes() throws Exception {
    CASImpl cas = (CASImpl) createCas();
    byte[] buf;
    try (var ctx = cas.ll_enableV2IdRefs(true)) {
      var ann = cas.createAnnotation(cas.getAnnotationType(), 0, 1);
      ann.addToIndexes();
      ann.removeFromIndexes();

      var allFSes = new LinkedHashSet<FeatureStructure>();
      cas.walkReachablePlusFSsSorted(allFSes::add, null, null, null);

      assertThat(allFSes) //
              .as("The annotation that was added and then removed before serialization should be found") //
              .containsExactly(cas.getSofa(), ann);

      try (var bos = new ByteArrayOutputStream()) {
        CasIOUtils.save(cas, bos, COMPRESSED_FILTERED_TSI);
        buf = bos.toByteArray();
      }
    }

    cas.reset();

    try (var ctx = cas.ll_enableV2IdRefs(true)) {
      CasIOUtils.load(new ByteArrayInputStream(buf), cas);

      var allFSes = new LinkedHashSet<FeatureStructure>();
      cas.walkReachablePlusFSsSorted(allFSes::add, null, null, null);

      assertThat(allFSes) //
              .as("The annotation that was added and then removed before serialization should not be found") //
              .containsExactly(cas.getSofa());
    }
  }
}
