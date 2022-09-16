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
import static org.apache.uima.cas.SerialFormat.COMPRESSED_FILTERED_TSI;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CasIOUtilsTest {

  private static final int SIMPLE_CAS_DEFAULT_INDEX_SIZE = 7;
  private static final int SIMPLE_CAS_DEFAULT_INDEX_SIZE_LENIENT = 5;
  private static final int SIMPLE_CAS_ALL_INDEXED_SIZE = 8;
  private static final int SIMPLE_CAS_ALL_INDEXED_SIZE_LENIENT = 6;

  private CAS cas;
  private CAS cas2;

  @BeforeEach
  public void setUp() throws Exception {
    File indexesFile = JUnitExtension.getFile("ExampleCas/testIndexes.xml");
    FsIndexDescription[] indexes = UIMAFramework.getXMLParser()
            .parseFsIndexCollection(new XMLInputSource(indexesFile)).getFsIndexes();

    File typeSystemFile = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
    TypeSystemDescription typeSystem = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(typeSystemFile));

    cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);

    try (FileInputStream casInputStream = new FileInputStream(
            JUnitExtension.getFile("ExampleCas/simpleCas.xmi"))) {
      CasIOUtils.load(casInputStream, cas);
    }

    File typeSystemFile2 = JUnitExtension.getFile("ExampleCas/testTypeSystem_variation.xml");
    TypeSystemDescription typeSystem2 = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(typeSystemFile2));
    cas2 = CasCreationUtils.createCas(typeSystem2, new TypePriorities_impl(), indexes);
  }

  @Test
  public void testXMI() throws Exception {
    testXMI(false);
  }

  @Test
  public void testXMILenient() throws Exception {
    testXMI(true);
  }

  public void testXMI(boolean leniently) throws Exception {
    File casFile = new File("target/temp-test-output/simpleCas.xmi");
    casFile.getParentFile().mkdirs();
    FileOutputStream docOS = new FileOutputStream(casFile);
    CasIOUtils.save(cas, docOS, SerialFormat.XMI);
    docOS.close();
    // NOTE - when Saxon saves the cas it omits the prefixes.
    // e.g. produces: <NULL id="0"/> instead of: <cas:NULL xmi:id="0"/>
    // This causes JUnit test failure "unknown type NULL"

    // Use a CAS initialized with the "correct" type system or with a different type system?
    CAS casToUse = leniently ? cas2 : cas;

    casToUse.reset();
    try (FileInputStream casInputStream = new FileInputStream(casFile)) {
      CasIOUtils.load(casInputStream, null, casToUse,
              leniently ? CasLoadMode.LENIENT : CasLoadMode.DEFAULT);
    }
    assertCorrectlyLoaded(casToUse, leniently);

    casToUse.reset();
    CasIOUtils.load(casFile.toURI().toURL(), null, casToUse,
            leniently ? CasLoadMode.LENIENT : CasLoadMode.DEFAULT);
    assertCorrectlyLoaded(casToUse, leniently);
  }

  @Test
  public void testXCAS() throws Exception {
    testXCAS(false);
  }

  @Test
  public void testXCASLenient() throws Exception {
    testXCAS(true);
  }

  public void testXCAS(boolean leniently) throws Exception {
    File casFile = new File("target/temp-test-output/simpleCas.xcas");
    casFile.getParentFile().mkdirs();
    try (FileOutputStream docOS = new FileOutputStream(casFile)) {
      CasIOUtils.save(cas, docOS, SerialFormat.XCAS);
    }

    // Use a CAS initialized with the "correct" type system or with a different type system?
    CAS casToUse = leniently ? cas2 : cas;

    casToUse.reset();
    CasIOUtils.load(casFile.toURI().toURL(), null, casToUse,
            leniently ? CasLoadMode.LENIENT : CasLoadMode.DEFAULT);
    assertCorrectlyLoaded(casToUse, leniently);
  }

  @Test
  public void testS() throws Exception {
    testFormat(SerialFormat.SERIALIZED, "bins", false);
  }

  @Test
  public void testSp() throws Exception {
    testFormat(SerialFormat.SERIALIZED_TSI, "binsp", false);
  }

  @Test
  public void testS6p() throws Exception {
    testFormat(SerialFormat.COMPRESSED_FILTERED_TSI, "bins6p", false);
  }

  @Test
  public void testS6pTs() throws Exception {
    testFormat(SerialFormat.COMPRESSED_FILTERED_TS, "bins6pTs", false);
  }

  @Test
  public void testS6pLenient() throws Exception {
    testFormat(SerialFormat.COMPRESSED_FILTERED_TSI, "bins6", true);
  }

  @Test
  public void testS0() throws Exception {
    testFormat(SerialFormat.BINARY, "bins0", false);
  }

  @Test
  public void testS0tsi() throws Exception {
    testFormat(SerialFormat.BINARY_TSI, "bins0", false);
  }

  @Test
  public void testS4() throws Exception {
    testFormat(SerialFormat.COMPRESSED, "bins4", false);
  }

  @Test
  public void testS4tsi() throws Exception {
    testFormat(SerialFormat.COMPRESSED_TSI, "bins4", false);
  }

  @Test
  public void testS6() throws Exception {
    testFormat(SerialFormat.COMPRESSED_FILTERED, "bins6", false);
  }

  private void testFormat(SerialFormat format, String fileEnding, boolean leniently)
          throws Exception {
    File casFile = new File("target/temp-test-output/simpleCas." + fileEnding);
    casFile.getParentFile().mkdirs();
    FileOutputStream docOS = new FileOutputStream(casFile);
    CasIOUtils.save(cas, docOS, format);
    docOS.close();

    // Use a CAS initialized with the "correct" type system or with a different type system?
    CAS casToUse = leniently ? cas2 : cas;
    casToUse.reset();

    FileInputStream casInputStream = new FileInputStream(casFile);
    SerialFormat loadedFormat = CasIOUtils.load(casInputStream, null, casToUse,
            leniently ? CasLoadMode.LENIENT : CasLoadMode.DEFAULT);
    casInputStream.close();
    Assert.assertEquals(format, loadedFormat);
    assertCorrectlyLoaded(casToUse, leniently);
  }

  private static void assertCorrectlyLoaded(CAS cas, boolean leniently) throws Exception {
    // Check if all the annotations are there (mind the file contains FSes that are NOT
    // annotations!)
    Assert.assertEquals(
            leniently ? SIMPLE_CAS_DEFAULT_INDEX_SIZE_LENIENT : SIMPLE_CAS_DEFAULT_INDEX_SIZE,
            cas.getAnnotationIndex().size());

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

    Assert.assertEquals(
            leniently ? SIMPLE_CAS_ALL_INDEXED_SIZE_LENIENT : SIMPLE_CAS_ALL_INDEXED_SIZE, fsCount);

    Assert.assertEquals(expectedTypes, fsTypes);
  }

  @Test
  public void testWrongInputStream() throws Exception {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ObjectOutput out = null;

    out = new ObjectOutputStream(byteArrayOutputStream);
    out.writeObject(new String("WRONG OBJECT"));

    byte[] casBytes = byteArrayOutputStream.toByteArray();
    out.close();
    ByteArrayInputStream casInputStream = new ByteArrayInputStream(casBytes);
    try {
      CasIOUtils.load(casInputStream, cas);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof CASRuntimeException);
      Assert.assertTrue(((CASRuntimeException) e).getMessageKey()
              .equals("UNRECOGNIZED_SERIALIZED_CAS_FORMAT"));
      casInputStream.close();
      return;
    }
    Assert.fail("An exception should have been thrown for wrong input.");
  }

  @Test
  public void testWrongFormat() throws Exception {
    File casFile = new File("target/temp-test-output/simpleCas.wrong");
    try {
      CasIOUtils.save(cas, new FileOutputStream(casFile), SerialFormat.UNKNOWN);
    } catch (Exception e) {
      // Assert.assertTrue(e instanceof IllegalArgumentException);
      return;
    }
    Assert.fail("An exception should have been thrown for wrong format.");
  }

  @Test
  public void testDocumentAnnotationIsNotResurrected() throws Exception {
    String refererAnnoTypeName = "org.apache.uima.testing.Referer";
    String customDocAnnoTypeName = "org.apache.uima.testing.CustomDocumentAnnotation";

    TypeSystemDescription tsd = UIMAFramework.getResourceSpecifierFactory()
            .createTypeSystemDescription();
    tsd.addType(customDocAnnoTypeName, "", CAS.TYPE_NAME_DOCUMENT_ANNOTATION);
    TypeDescription refererType = tsd.addType(refererAnnoTypeName, "", CAS.TYPE_NAME_TOP);
    refererType.addFeature("ref", "", CAS.TYPE_NAME_DOCUMENT_ANNOTATION);

    CAS cas = CasCreationUtils.createCas(tsd, null, null);

    // Initialize the default document annotation
    // ... then immediately remove it from the indexes.
    FeatureStructure da = cas.getDocumentAnnotation();

    assertThat(cas.select(cas.getTypeSystem().getType(CAS.TYPE_NAME_DOCUMENT_ANNOTATION)).asList())
            .extracting(fs -> fs.getType().getName())
            .containsExactly(CAS.TYPE_NAME_DOCUMENT_ANNOTATION);

    // Add a feature structure that references the original document annotation before we remove
    // it from the indexes
    FeatureStructure referer = cas.createFS(cas.getTypeSystem().getType(refererAnnoTypeName));
    referer.setFeatureValue(referer.getType().getFeatureByBaseName("ref"), da);
    cas.addFsToIndexes(referer);

    cas.removeFsFromIndexes(da);

    // Now add a new document annotation of our custom type
    FeatureStructure cda = cas.createFS(cas.getTypeSystem().getType(customDocAnnoTypeName));
    cas.addFsToIndexes(cda);

    assertThat(cas.select(cas.getTypeSystem().getType(CAS.TYPE_NAME_DOCUMENT_ANNOTATION)).asList())
            .extracting(fs -> fs.getType().getName()).containsExactly(customDocAnnoTypeName);

    // Serialize to a buffer
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    CasIOUtils.save(cas, bos, SerialFormat.SERIALIZED_TSI);

    // Deserialize from the buffer
    ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
    CasIOUtils.load(bis, cas);

    assertThat(cas.select(cas.getTypeSystem().getType(CAS.TYPE_NAME_DOCUMENT_ANNOTATION)).asList())
            .extracting(fs -> fs.getType().getName()).containsExactly(customDocAnnoTypeName);
  }

  @Test
  public void thatBinaryForm6DoesOnlyIncludeReachableFSes() throws Exception {
    CASImpl cas = (CASImpl) CasCreationUtils.createCas();
    byte[] buf;
    try (AutoCloseableNoException a = cas.ll_enableV2IdRefs(true)) {
      Annotation ann = cas.createAnnotation(cas.getAnnotationType(), 0, 1);
      ann.addToIndexes();
      ann.removeFromIndexes();

      Set<FeatureStructure> allFSes = new LinkedHashSet<>();
      cas.walkReachablePlusFSsSorted(allFSes::add, null, null, null);

      assertThat(allFSes) //
              .as("The annotation that was added and then removed before serialization should be found") //
              .containsExactly(cas.getSofa(), ann);

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      CasIOUtils.save(cas, bos, COMPRESSED_FILTERED_TSI);
      buf = bos.toByteArray();
    }

    cas.reset();

    try (AutoCloseableNoException a = cas.ll_enableV2IdRefs(true)) {
      CasIOUtils.load(new ByteArrayInputStream(buf), cas);

      Set<FeatureStructure> allFSes = new LinkedHashSet<>();
      cas.walkReachablePlusFSsSorted(allFSes::add, null, null, null);

      assertThat(allFSes) //
              .as("The annotation that was added and then removed before serialization should not be found") //
              .containsExactly(cas.getSofa());
    }
  }

  @AfterEach
  public void tearDown() throws Exception {
    cas.release();
  }
}
