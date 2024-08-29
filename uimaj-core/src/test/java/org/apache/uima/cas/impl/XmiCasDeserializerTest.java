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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.Marker;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.impl.XmiSerializationSharedData.OotsElementData;
import org.apache.uima.cas.impl.XmiSerializationSharedData.XmiArrayElement;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas_data.impl.CasComparer;
import org.apache.uima.internal.util.MultiThreadUtils;
import org.apache.uima.internal.util.MultiThreadUtils.ThreadM;
import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.EmptyStringList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCopier;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmiCasDeserializerTest {

  // normally set to true, set to false for comparative performance measurement
  // because this adds ~ 10 seconds or so to the time it takes to run the junit tests

  public static final boolean IS_CAS_COMPARE = true;

  private FsIndexDescription[] indexes;

  private TypeSystemDescription typeSystem;

  @BeforeEach
  public void setUp() throws Exception {
    var typeSystemFile = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
    var indexesFile = JUnitExtension.getFile("ExampleCas/testIndexes.xml");

    // large type system
    typeSystem = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(typeSystemFile));

    // bag index for Entities and Relations
    indexes = UIMAFramework.getXMLParser().parseFsIndexCollection(new XMLInputSource(indexesFile))
            .getFsIndexes();
  }

  /*
   * test case for https://issues.apache.org/jira/projects/UIMA/issues/UIMA-5558
   */
  @Test
  void testSerialize_with_0_length_array() throws Exception {

    var typeSystemDescription = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(JUnitExtension
                    .getFile("ExampleCas/testTypeSystem_small_withoutMultiRefs.xml")));
    var cas = CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(), null);

    var ts = cas.getTypeSystem();
    var refType = ts.getType("RefType"); // super is Annotation
    var ref = refType.getFeatureByBaseName("ref");
    var ref_StringArray = refType.getFeatureByBaseName("ref_StringArray");
    var ref_StringList = refType.getFeatureByBaseName("ref_StringList");
    var jcas = cas.getJCas();

    String xml;

    // deserialize into another CAS
    var fact = SAXParserFactory.newInstance();
    var parser = fact.newSAXParser();
    var xmlReader = parser.getXMLReader();

    var cas2 = CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(), null);
    XmiCasDeserializer deser2;
    ContentHandler deserHandler2;

    {
      var stringArray = new StringArray(jcas, 0);

      var fsRef = cas.createAnnotation(refType, 0, 0);
      fsRef.setFeatureValue(ref_StringArray, stringArray);
      cas.addFsToIndexes(fsRef); // gets serialized in=place

      xml = serialize(cas, null);

      // deserialize into another CAS
      fact = SAXParserFactory.newInstance();
      parser = fact.newSAXParser();
      xmlReader = parser.getXMLReader();

      deser2 = new XmiCasDeserializer(cas2.getTypeSystem());
      deserHandler2 = deser2.getXmiCasHandler(cas2);
      xmlReader.setContentHandler(deserHandler2);
      xmlReader.parse(new InputSource(new StringReader(xml)));

      CasComparer.assertEquals(cas, cas2);
      var fs2 = cas2.getAnnotationIndex(refType).iterator().get();
      var fsa2 = (StringArrayFS) fs2.getFeatureValue(ref_StringArray);
      assertThat(fsa2.size()).isEqualTo(0);
    }

    // ------- repeat with lists in place of arrays --------------

    cas.reset();

    StringList stringlist0 = new EmptyStringList(jcas);

    // fsarray.addToIndexes(); // if added to indexes, forces serialization of FSArray as an element

    var fsRef = cas.createAnnotation(refType, 0, 0);
    fsRef.setFeatureValue(ref_StringList, stringlist0);
    cas.addFsToIndexes(fsRef); // gets serialized in=place

    xml = serialize(cas, null);

    // deserialize into another CAS
    parser = fact.newSAXParser();
    xmlReader = parser.getXMLReader();

    cas2.reset();

    deser2 = new XmiCasDeserializer(cas2.getTypeSystem());
    deserHandler2 = deser2.getXmiCasHandler(cas2);
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(new StringReader(xml)));

    CasComparer.assertEquals(cas, cas2);
    var fs2 = cas2.getAnnotationIndex(refType).iterator().get();
    var fsl2 = fs2.getFeatureValue(ref_StringList);
    assertThat(fsl2.getType().getShortName().equals("EmptyStringList")).isTrue();

  }

  /*
   * test case for https://issues.apache.org/jira/browse/UIMA-5532
   */
  @Test
  public void testSerialize_withPartialMultiRefs() throws Exception {

    var typeSystemDescription = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(
                    JUnitExtension.getFile("ExampleCas/testTypeSystem_small_withMultiRefs.xml")));
    var cas = CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(), null);

    var ts = cas.getTypeSystem();
    var refType = ts.getType("RefType");
    var ref = refType.getFeatureByBaseName("ref");
    var ref_FSArray = refType.getFeatureByBaseName("ref_FSArray");
    var ref_FSList = refType.getFeatureByBaseName("ref_FSList");
    var jcas = cas.getJCas();

    String xml; // = serialize(cas, null);

    // deserialize into another CAS
    var fact = SAXParserFactory.newInstance();
    var parser = fact.newSAXParser();
    var xmlReader = parser.getXMLReader();

    var cas2 = CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(), null);
    XmiCasDeserializer deser2;
    ContentHandler deserHandler2;

    {
      var fsarray = new FSArray<TOP>(jcas, 2);
      var fs1 = new TOP(jcas);
      var fs2 = new TOP(jcas);
      fsarray.set(0, fs1);
      fsarray.set(1, fs2);

      // fsarray.addToIndexes(); // if added to indexes, forces serialization of FSArray as an
      // element

      var fsRef = cas.createAnnotation(refType, 0, 0);
      fsRef.setFeatureValue(ref, fsarray);
      cas.addFsToIndexes(fsRef); // gets serialized in=place

      fsRef = cas.createAnnotation(refType, 0, 0);
      fsRef.setFeatureValue(ref_FSArray, fsarray);
      cas.addFsToIndexes(fsRef); // gets serialized as ref

      xml = serialize(cas, null);

      // deserialize into another CAS
      fact = SAXParserFactory.newInstance();
      parser = fact.newSAXParser();
      xmlReader = parser.getXMLReader();

      deser2 = new XmiCasDeserializer(cas2.getTypeSystem());
      deserHandler2 = deser2.getXmiCasHandler(cas2);
      xmlReader.setContentHandler(deserHandler2);
      xmlReader.parse(new InputSource(new StringReader(xml)));

      CasComparer.assertEquals(cas, cas2);

      // ------- repeat with lists in place of arrays --------------
    }

    cas.reset();

    var fs1 = new TOP(jcas); // https://issues.apache.org/jira/browse/UIMA-5544
    var fs2 = new TOP(jcas);

    FSList fslist2 = new EmptyFSList(jcas);
    FSList fslist1 = new NonEmptyFSList(jcas, fs2, fslist2);
    FSList fslist0 = new NonEmptyFSList(jcas, fs1, fslist1);

    // fsarray.addToIndexes(); // if added to indexes, forces serialization of FSArray as an element

    var fsRef = cas.createAnnotation(refType, 0, 0);
    fsRef.setFeatureValue(ref, fslist0);
    cas.addFsToIndexes(fsRef); // gets serialized in=place

    fsRef = cas.createAnnotation(refType, 0, 0);
    fsRef.setFeatureValue(ref_FSList, fslist0);
    cas.addFsToIndexes(fsRef); // gets serialized as ref

    xml = serialize(cas, null);

    // deserialize into another CAS
    parser = fact.newSAXParser();
    xmlReader = parser.getXMLReader();

    cas2.reset();
    // fs1 = new TOP(jcas); //https://issues.apache.org/jira/browse/UIMA-5544
    // fs2 = new TOP(jcas);
    deser2 = new XmiCasDeserializer(cas2.getTypeSystem());
    deserHandler2 = deser2.getXmiCasHandler(cas2);
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(new StringReader(xml)));

    CasComparer.assertEquals(cas, cas2);

  }

  @Test
  void testDeserializeAndReserialize() throws Exception {
    var tsWithNoMultiRefs = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
    doTestDeserializeAndReserialize(tsWithNoMultiRefs, false);
    var tsWithMultiRefs = JUnitExtension.getFile("ExampleCas/testTypeSystem_withMultiRefs.xml");
    doTestDeserializeAndReserialize(tsWithMultiRefs, false);
    // also test with JCas initialized
    doTestDeserializeAndReserialize(tsWithNoMultiRefs, true);
    doTestDeserializeAndReserialize(tsWithMultiRefs, true);
  }

  @Test
  void testDeserializeAndReserializeV2() throws Exception {
    try (var a = LowLevelCAS.ll_defaultV2IdRefs()) {
      var tsWithNoMultiRefs = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
      doTestDeserializeAndReserialize(tsWithNoMultiRefs, false);
      var tsWithMultiRefs = JUnitExtension.getFile("ExampleCas/testTypeSystem_withMultiRefs.xml");
      doTestDeserializeAndReserialize(tsWithMultiRefs, false);
      // also test with JCas initialized
      doTestDeserializeAndReserialize(tsWithNoMultiRefs, true);
      doTestDeserializeAndReserialize(tsWithMultiRefs, true);
    }
  }

  private void doTestDeserializeAndReserialize(File typeSystemDescriptorFile, boolean useJCas)
          throws Exception {
    // deserialize a complex CAS from XCAS
    var typeSystemDescription = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(typeSystemDescriptorFile));
    var cas = CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(), indexes);
    if (useJCas) {
      cas.getJCas();
    }

    var serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));

    XCASDeserializer.deserialize(serCasStream, cas, false); // not lenient

    // next is missing the support for v2 ids
    // XCASDeserializer deser = new XCASDeserializer(cas.getTypeSystem());
    // ContentHandler deserHandler = deser.getXCASHandler(cas);
    // SAXParserFactory fact = SAXParserFactory.newInstance();
    // SAXParser parser = fact.newSAXParser();
    // XMLReader xmlReader = parser.getXMLReader();
    // xmlReader.setContentHandler(deserHandler);
    // xmlReader.parse(new InputSource(serCasStream));
    serCasStream.close();

    // reserialize as XMI
    var xml = serialize(cas, null);
    // dumpStr2File(xml, "145");

    // deserialize into another CAS
    var cas2 = CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(),
            indexes);
    if (useJCas) {
      cas2.getJCas();
    }

    var xmlReader = XMLUtils.createSAXParserFactory().newSAXParser().getXMLReader();

    var deser2 = new XmiCasDeserializer(cas2.getTypeSystem());
    ContentHandler deserHandler2 = deser2.getXmiCasHandler(cas2);
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(new StringReader(xml)));

    // compare
    if (IS_CAS_COMPARE) {
      var cc = new CasCompare((CASImpl) cas, (CASImpl) cas2);
      // ids won't be the same, don't compare these
      // tune debug
      var start = System.nanoTime();
      var i = 0;
      // for (; i < 1000; i++) {
      assertThat(cc.compareCASes()).isTrue();
      // if (i % 10 == 0) {
      var end = System.nanoTime();
      System.out.format("compareCASes i: %d, time: %,d millisec%n", i, (end - start) / 1000000L);
      start = end;
      // }
      // }
    }

    assertThat(cas2.getAnnotationIndex().size()).isEqualTo(cas.getAnnotationIndex().size());
    assertThat(cas2.getDocumentText()).isEqualTo(cas.getDocumentText());
    CasComparer.assertEquals(cas, cas2);

    // check that array refs are not null
    var entityType = cas2.getTypeSystem().getType("org.apache.uima.testTypeSystem.Entity");
    var classesFeat = entityType.getFeatureByBaseName("classes");
    Iterator<FeatureStructure> iter = cas2.getIndexRepository().getIndex("testEntityIndex")
            .iterator();
    assertThat(iter.hasNext()).isTrue();
    while (iter.hasNext()) {
      var fs = iter.next();
      var arrayFS = (StringArrayFS) fs.getFeatureValue(classesFeat);
      assertThat(arrayFS).isNotNull();
      for (var i = 0; i < arrayFS.size(); i++) {
        assertThat(arrayFS.get(i)).isNotNull();
      }
    }
    var annotArrayTestType = cas2.getTypeSystem()
            .getType("org.apache.uima.testTypeSystem.AnnotationArrayTest");
    var annotArrayFeat = annotArrayTestType.getFeatureByBaseName("arrayOfAnnotations");
    Iterator<AnnotationFS> iter2 = cas2.getAnnotationIndex(annotArrayTestType).iterator();
    assertThat(iter2.hasNext()).isTrue();
    while (iter2.hasNext()) {
      FeatureStructure fs = iter2.next();
      var arrayFS = (ArrayFS) fs.getFeatureValue(annotArrayFeat);
      assertThat(arrayFS).isNotNull();
      for (var i = 0; i < arrayFS.size(); i++) {
        assertThat(arrayFS.get(i)).isNotNull();
      }
    }

    // test that lenient mode does not report errors
    var cas3 = CasCreationUtils.createCas(new TypeSystemDescription_impl(),
            new TypePriorities_impl(), FsIndexDescription.EMPTY_FS_INDEX_DESCRIPTIONS);
    if (useJCas) {
      cas3.getJCas();
    }
    var deser3 = new XmiCasDeserializer(cas3.getTypeSystem());
    ContentHandler deserHandler3 = deser3.getXmiCasHandler(cas3, true);
    xmlReader.setContentHandler(deserHandler3);
    xmlReader.parse(new InputSource(new StringReader(xml)));
  }

  /*
   * https://issues.apache.org/jira/browse/UIMA-3396
   */
  @Test
  void testDeltaCasIndexing() throws Exception {
    var cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    var cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    cas1.setDocumentText("This is a test document in the initial view");
    var ir1 = (FSIndexRepositoryImpl) cas1.getIndexRepository();

    var anAnnotBefore = cas1.createAnnotation(cas1.getAnnotationType(), 0, 2);
    ir1.addFS(anAnnotBefore);

    cas1.createMarker(); // will start journaling index updates

    var anAnnot1 = cas1.createAnnotation(cas1.getAnnotationType(), 0, 4);
    ir1.addFS(anAnnot1);
    ir1.removeFS(anAnnot1);
    ir1.addFS(anAnnot1);

    assertThat(ir1.getAddedFSs().size() == 1).isTrue();
    assertThat(ir1.getDeletedFSs().isEmpty()).isTrue();
    assertThat(ir1.getReindexedFSs().isEmpty()).isTrue();

    ir1.removeFS(anAnnotBefore);
    ir1.addFS(anAnnotBefore);

    assertThat(ir1.getAddedFSs().size() == 1).isTrue();
    assertThat(ir1.getDeletedFSs().isEmpty()).isTrue();
    assertThat(ir1.getReindexedFSs().size() == 1).isTrue();

    ir1.removeFS(anAnnotBefore);
    assertThat(ir1.getAddedFSs().size() == 1).isTrue();
    assertThat(ir1.getDeletedFSs().size() == 1).isTrue();
    assertThat(ir1.getReindexedFSs().isEmpty()).isTrue();
  }

  @Test
  void testMultiThreadedSerialize() throws Exception {
    var tsWithNoMultiRefs = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
    doTestMultiThreadedSerialize(tsWithNoMultiRefs);
    var tsWithMultiRefs = JUnitExtension.getFile("ExampleCas/testTypeSystem_withMultiRefs.xml");
    doTestMultiThreadedSerialize(tsWithMultiRefs);
  }

  private static class DoSerialize implements Runnable {
    private CAS cas;

    DoSerialize(CAS aCas) {
      cas = aCas;
    }

    @Override
    public void run() {
      try {
        while (true) {
          if (!MultiThreadUtils.wait4go((ThreadM) Thread.currentThread())) {
            break;
          }

          serialize(cas, null);
        }
        // serialize(cas, null);
        // serialize(cas, null);
        // serialize(cas, null);
      } catch (IOException | SAXException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static final int MAX_THREADS = 16;
  // do as sequence 1, 2, 4, 8, 16 and measure elapsed time
  private static int[] threadsToUse = new int[] { 1, 2, 4, 8, 16/* , 32, 64 */ };

  private void doTestMultiThreadedSerialize(File typeSystemDescriptor) throws Exception {
    // deserialize a complex CAS from XCAS
    var cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);

    var serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
    var deser = new XCASDeserializer(cas.getTypeSystem());
    var deserHandler = deser.getXCASHandler(cas);
    var fact = SAXParserFactory.newInstance();
    var parser = fact.newSAXParser();
    var xmlReader = parser.getXMLReader();
    xmlReader.setContentHandler(deserHandler);
    xmlReader.parse(new InputSource(serCasStream));
    serCasStream.close();

    // make n copies of the cas, so they all share
    // the same type system

    final var cases = new CAS[MAX_THREADS];

    for (var i = 0; i < MAX_THREADS; i++) {
      cases[i] = CasCreationUtils.createCas(cas.getTypeSystem(), new TypePriorities_impl(), indexes,
              null);
      CasCopier.copyCas(cas, cases[i], true);
    }

    // start n threads, serializing as XMI
    var threads = new MultiThreadUtils.ThreadM[MAX_THREADS];
    for (var i = 0; i < MAX_THREADS; i++) {
      threads[i] = new MultiThreadUtils.ThreadM(new DoSerialize(cases[i]));
      threads[i].start();
    }
    MultiThreadUtils.waitForAllReady(threads);

    for (int j : threadsToUse) {
      var sliceOfThreads = new ThreadM[j];
      System.arraycopy(threads, 0, sliceOfThreads, 0, j);

      var startTime = System.currentTimeMillis();

      MultiThreadUtils.kickOffThreads(sliceOfThreads);

      MultiThreadUtils.waitForAllReady(sliceOfThreads);

      System.out.println("\nNumber of threads serializing: " + j
              + "  Normalized millisecs (should be close to the same): "
              + (System.currentTimeMillis() - startTime) / j);
    }

    MultiThreadUtils.terminateThreads(threads);
  }

  @Test
  void testDeltaCasIndexExistingFsInView() throws Exception {
    var cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    var cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    cas1.setDocumentText("This is a test document in the initial view");
    var referentType = cas1.getTypeSystem().getType("org.apache.uima.testTypeSystem.Referent");
    var fs1 = cas1.createFS(referentType);
    cas1.getIndexRepository().addFS(fs1);

    // serialize complete
    var sharedData = new XmiSerializationSharedData();
    var xml = serialize(cas1, sharedData);
    // System.out.println(xml);
    var maxOutgoingXmiId = sharedData.getMaxXmiId();

    // deserialize into cas2
    var sharedData2 = new XmiSerializationSharedData();
    this.deserialize(xml, cas2, sharedData2, true, -1);
    CasComparer.assertEquals(cas1, cas2);

    // create Marker, add/modify fs and serialize in delta xmi format.
    var marker = cas2.createMarker();

    // create View
    var view = cas2.createView("NewView");
    // add FS to index
    var referentType2 = cas2.getTypeSystem().getType("org.apache.uima.testTypeSystem.Referent");
    Iterator<FeatureStructure> fsIter = cas2.getIndexRepository().getAllIndexedFS(referentType2);
    while (fsIter.hasNext()) {
      var fs = fsIter.next();
      view.getIndexRepository().addFS(fs);
    }
    var cas2newAnnot = view.createAnnotation(cas2.getAnnotationType(), 6, 8);
    view.getIndexRepository().addFS(cas2newAnnot);

    // serialize cas2 in delta format
    var deltaxml1 = serialize(cas2, sharedData2, marker);
    // System.out.println(deltaxml1);

    // deserialize delta xmi into cas1
    this.deserialize(deltaxml1, cas1, sharedData, true, maxOutgoingXmiId, AllowPreexistingFS.allow);

    // check that new View contains the FS
    var deserView = cas1.getView("NewView");
    Iterator<FeatureStructure> deserFsIter = deserView.getIndexRepository()
            .getAllIndexedFS(referentType);
    assertThat(deserFsIter.hasNext()).isTrue();
  }

  // test - initial view, no Sofa,
  @Test
  void testDeltaCasIndexExistingFsInInitialView() throws Exception {
    var cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    var cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    // no sofa
    // cas1.setDocumentText("This is a test document in the initial view");
    var referentType = cas1.getTypeSystem().getType("org.apache.uima.testTypeSystem.Referent");
    var fs1 = cas1.createFS(referentType);
    cas1.getIndexRepository().addFS(fs1); // index in initial view

    // serialize complete
    var sharedData = new XmiSerializationSharedData();
    var xml = serialize(cas1, sharedData);
    // System.out.println(xml);
    var maxOutgoingXmiId = sharedData.getMaxXmiId();

    // deserialize into cas2
    var sharedData2 = new XmiSerializationSharedData();
    this.deserialize(xml, cas2, sharedData2, true, -1);
    CasComparer.assertEquals(cas1, cas2);

    // create Marker, add/modify fs and serialize in delta xmi format.
    var marker = cas2.createMarker();

    // create View
    var view = cas2.createView("NewView");
    // add FS to index
    var referentType2 = cas2.getTypeSystem().getType("org.apache.uima.testTypeSystem.Referent");
    Iterator<FeatureStructure> fsIter = cas2.getIndexRepository().getAllIndexedFS(referentType2);
    while (fsIter.hasNext()) {
      var fs = fsIter.next();
      view.getIndexRepository().addFS(fs);
    }
    var cas2newAnnot = view.createAnnotation(cas2.getAnnotationType(), 6, 8);
    view.getIndexRepository().addFS(cas2newAnnot);

    // add fs to initial view index repo.

    fs1 = cas2.createFS(referentType);
    cas2.getIndexRepository().addFS(fs1);

    // serialize cas2 in delta format
    var deltaxml1 = serialize(cas2, sharedData2, marker);
    // System.out.println(deltaxml1);

    // deserialize delta xmi into cas1
    deserialize(deltaxml1, cas1, sharedData, true, maxOutgoingXmiId, AllowPreexistingFS.allow);

    // check that new View contains the FS
    var deserView = cas1.getView("NewView");
    Iterator<FeatureStructure> deserFsIter = deserView.getIndexRepository()
            .getAllIndexedFS(referentType);
    assertThat(deserFsIter.hasNext()).isTrue();
  }

  @Test
  void testDeltaCasIndexExistingFsInNewView() throws Exception {
    var cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    var cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    cas1.setDocumentText("This is a test document in the initial view");
    var referentType = cas1.getTypeSystem().getType("org.apache.uima.testTypeSystem.Referent");
    var fs1 = cas1.createFS(referentType);
    cas1.getIndexRepository().addFS(fs1);

    // serialize complete
    var sharedData = new XmiSerializationSharedData();
    var xml = serialize(cas1, sharedData);
    // System.out.println(xml);
    var maxOutgoingXmiId = sharedData.getMaxXmiId();

    // deserialize into cas2
    var sharedData2 = new XmiSerializationSharedData();
    this.deserialize(xml, cas2, sharedData2, true, -1);
    CasComparer.assertEquals(cas1, cas2);

    // create Marker, add/modify fs and serialize in delta xmi format.
    var marker = cas2.createMarker();

    // create View
    var view = cas2.createView("NewView");
    // add FS to index
    var referentType2 = cas2.getTypeSystem().getType("org.apache.uima.testTypeSystem.Referent");
    Iterator<FeatureStructure> fsIter = cas2.getIndexRepository().getAllIndexedFS(referentType2);
    while (fsIter.hasNext()) {
      var fs = fsIter.next();
      view.getIndexRepository().addFS(fs);
    }
    var cas2newAnnot = view.createAnnotation(cas2.getAnnotationType(), 6, 8);
    view.getIndexRepository().addFS(cas2newAnnot);

    // serialize cas2 in delta format
    var deltaxml1 = serialize(cas2, sharedData2, marker);
    // System.out.println(deltaxml1);

    // deserialize delta xmi into cas1
    deserialize(deltaxml1, cas1, sharedData, true, maxOutgoingXmiId, AllowPreexistingFS.allow);

    // check that new View contains the FS
    var deserView = cas1.getView("NewView");
    Iterator<FeatureStructure> deserFsIter = deserView.getIndexRepository()
            .getAllIndexedFS(referentType);
    assertThat(deserFsIter.hasNext()).isTrue();
  }

  @Test
  void testMultipleSofas() throws Exception {
    var cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
            FsIndexDescription.EMPTY_FS_INDEX_DESCRIPTIONS);
    // set document text for the initial view
    cas.setDocumentText("This is a test");
    // create a new view and set its document text
    var cas2 = cas.createView("OtherSofa");
    cas2.setDocumentText("This is only a test");

    // Change this test to create an instance of TOP because you cannot add an annotation to other
    // than
    // the view it is created in. https://issues.apache.org/jira/browse/UIMA-4099
    // create a TOP and add to index of both views
    var topType = cas.getTypeSystem().getTopType();
    var aTOP = cas.createFS(topType);
    cas.getIndexRepository().addFS(aTOP);
    cas2.getIndexRepository().addFS(aTOP);
    var it = cas.getIndexRepository().getAllIndexedFS(topType);
    var it2 = cas2.getIndexRepository().getAllIndexedFS(topType);
    it.next();
    it.next();
    it2.next();
    it2.next();
    assertThat(it.hasNext()).isFalse();
    assertThat(it2.hasNext()).isFalse();

    // serialize
    var sw = new StringWriter();
    var xmlSer = new XMLSerializer(sw, false);
    var xmiSer = new XmiCasSerializer(cas.getTypeSystem());
    xmiSer.serialize(cas, xmlSer.getContentHandler());
    var xml = sw.getBuffer().toString();

    // deserialize into another CAS (repeat twice to check it still works after reset)
    var newCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
            FsIndexDescription.EMPTY_FS_INDEX_DESCRIPTIONS);
    for (var i = 0; i < 2; i++) {
      var newDeser = new XmiCasDeserializer(newCas.getTypeSystem());
      ContentHandler newDeserHandler = newDeser.getXmiCasHandler(newCas);
      var fact = SAXParserFactory.newInstance();
      var parser = fact.newSAXParser();
      var xmlReader = parser.getXMLReader();
      xmlReader.setContentHandler(newDeserHandler);
      xmlReader.parse(new InputSource(new StringReader(xml)));

      // check sofas
      assertThat(newCas.getDocumentText()).isEqualTo("This is a test");
      var newCas2 = newCas.getView("OtherSofa");
      assertThat(newCas2.getDocumentText()).isEqualTo("This is only a test");

      // check that annotation is still indexed in both views
      // check that annotation is still indexed in both views
      it = newCas.getIndexRepository().getAllIndexedFS(topType);
      it2 = newCas2.getIndexRepository().getAllIndexedFS(topType);
      it.next();
      it.next();
      it2.next();
      it2.next();
      assertThat(it.hasNext()).isFalse();
      // assertFalse(it2.hasNext()); assertTrue(tIndex.size() == 2); // document annot and this
      // one
      // assertTrue(t2Index.size() == 2); // ditto

      newCas.reset();
    }
  }

  @Test
  void testTypeSystemFiltering() throws Exception {
    // deserialize a complex CAS from XCAS
    var cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);

    InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
    var deser = new XCASDeserializer(cas.getTypeSystem());
    ContentHandler deserHandler = deser.getXCASHandler(cas);
    var fact = SAXParserFactory.newInstance();
    var parser = fact.newSAXParser();
    var xmlReader = parser.getXMLReader();
    xmlReader.setContentHandler(deserHandler);
    xmlReader.parse(new InputSource(serCasStream));
    serCasStream.close();

    // now read in a TypeSystem that's a subset of those types
    var partialTypeSystemDesc = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            new XMLInputSource(JUnitExtension.getFile("ExampleCas/partialTestTypeSystem.xml")));
    var partialTypeSystem = CasCreationUtils.createCas(partialTypeSystemDesc, null, null)
            .getTypeSystem();

    // reserialize as XMI, filtering out anything that doesn't fit in the
    // partialTypeSystem
    var sw = new StringWriter();
    var xmlSer = new XMLSerializer(sw, false);
    var xmiSer = new XmiCasSerializer(partialTypeSystem);
    xmiSer.serialize(cas, xmlSer.getContentHandler());
    var xml = sw.getBuffer().toString();
    // System.out.println(xml);

    // deserialize into another CAS (which has the whole type system)
    var cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    var deser2 = new XmiCasDeserializer(cas2.getTypeSystem());
    ContentHandler deserHandler2 = deser2.getXmiCasHandler(cas2);
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(new StringReader(xml)));

    // check that types have been filtered out
    var orgType = cas2.getTypeSystem().getType("org.apache.uima.testTypeSystem.Organization");
    assertThat(orgType).isNotNull();
    assertThat(cas2.getAnnotationIndex(orgType).isEmpty()).isTrue();
    assertThat(!cas.getAnnotationIndex(orgType).isEmpty()).isTrue();

    // but that some types are still there
    var personType = cas2.getTypeSystem().getType("org.apache.uima.testTypeSystem.Person");
    FSIndex personIndex = cas2.getAnnotationIndex(personType);
    assertThat(personIndex.size() > 0).isTrue();

    // check that mentionType has been filtered out (set to null)
    var somePlace = personIndex.iterator().get();
    var mentionTypeFeat = personType.getFeatureByBaseName("mentionType");
    assertThat(mentionTypeFeat).isNotNull();
    assertThat(somePlace.getStringValue(mentionTypeFeat)).isNull();
  }

  @Test
  void testNoInitialSofa() throws Exception {
    var cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
            FsIndexDescription.EMPTY_FS_INDEX_DESCRIPTIONS);
    // create non-annotation type so as not to create the _InitialView Sofa
    var intArrayFS = cas.createIntArrayFS(5);
    intArrayFS.set(0, 1);
    intArrayFS.set(1, 2);
    intArrayFS.set(2, 3);
    intArrayFS.set(3, 4);
    intArrayFS.set(4, 5);
    cas.getIndexRepository().addFS(intArrayFS);

    // serialize the CAS
    var sw = new StringWriter();
    var xmlSer = new XMLSerializer(sw, false);
    var xmiSer = new XmiCasSerializer(cas.getTypeSystem());
    xmiSer.serialize(cas, xmlSer.getContentHandler());
    var xml = sw.getBuffer().toString();

    // deserialize into another CAS
    var cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
            FsIndexDescription.EMPTY_FS_INDEX_DESCRIPTIONS);

    var deser2 = new XmiCasDeserializer(cas2.getTypeSystem());
    ContentHandler deserHandler2 = deser2.getXmiCasHandler(cas2);
    var fact = SAXParserFactory.newInstance();
    var parser = fact.newSAXParser();
    var xmlReader = parser.getXMLReader();
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(new StringReader(xml)));

    // test that index is correctly populated
    var intArrayType = cas2.getTypeSystem().getType(CAS.TYPE_NAME_INTEGER_ARRAY);
    Iterator<FeatureStructure> iter = cas2.getIndexRepository().getAllIndexedFS(intArrayType);
    assertThat(iter.hasNext()).isTrue();
    var intArrayFS2 = (IntArrayFS) iter.next();
    assertThat(iter.hasNext()).isFalse();
    assertThat(intArrayFS2.size()).isEqualTo(5);
    assertThat(intArrayFS2.get(0)).isEqualTo(1);
    assertThat(intArrayFS2.get(1)).isEqualTo(2);
    assertThat(intArrayFS2.get(2)).isEqualTo(3);
    assertThat(intArrayFS2.get(3)).isEqualTo(4);
    assertThat(intArrayFS2.get(4)).isEqualTo(5);

    // test that serializing the new CAS produces the same XML
    sw = new StringWriter();
    xmlSer = new XMLSerializer(sw, false);
    xmiSer = new XmiCasSerializer(cas2.getTypeSystem());
    xmiSer.serialize(cas2, xmlSer.getContentHandler());
    var xml2 = sw.getBuffer().toString();
    assertThat(xml2.equals(xml)).isTrue();
  }

  @Test
  void testv1FormatXcas() throws Exception {
    var cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
            FsIndexDescription.EMPTY_FS_INDEX_DESCRIPTIONS);
    var ts = cas.getTypeSystem();
    var v1cas = CasCreationUtils.createCas(ts, new TypePriorities_impl(),
            FsIndexDescription.EMPTY_FS_INDEX_DESCRIPTIONS, null);

    // get a complex CAS
    var serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
    var deser = new XCASDeserializer(cas.getTypeSystem());
    var deserHandler = deser.getXCASHandler(cas);
    var fact = SAXParserFactory.newInstance();
    var parser = fact.newSAXParser();
    var xmlReader = parser.getXMLReader();
    xmlReader.setContentHandler(deserHandler);
    xmlReader.parse(new InputSource(serCasStream));
    serCasStream.close();

    // test it
    assertThat(CAS.NAME_DEFAULT_SOFA.equals(cas.getSofa().getSofaID())).isTrue();

    // get a v1 XMI version of the same CAS
    serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/v1xmiCas.xml"));
    var deser2 = new XmiCasDeserializer(v1cas.getTypeSystem());
    var deserHandler2 = deser2.getXmiCasHandler(v1cas);
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(serCasStream));
    serCasStream.close();

    // compare
    assertThat(v1cas.getAnnotationIndex().size()).isEqualTo(cas.getAnnotationIndex().size());
    assertThat(CAS.NAME_DEFAULT_SOFA.equals(v1cas.getSofa().getSofaID())).isTrue();

    // now a v1 XMI version of a multiple Sofa CAS
    v1cas.reset();
    serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/xmiMsCasV1.xml"));
    deser2 = new XmiCasDeserializer(v1cas.getTypeSystem());
    deserHandler2 = deser2.getXmiCasHandler(v1cas);
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(serCasStream));
    serCasStream.close();

    // test it
    var engView = v1cas.getView("EnglishDocument");
    assertThat(engView.getDocumentText().equals("this beer is good")).isTrue();
    assertThat(engView.getAnnotationIndex().size() == 5).isTrue(); // 4 annots plus
                                                                   // documentAnnotation
    var gerView = v1cas.getView("GermanDocument");
    assertThat(gerView.getDocumentText().equals("das bier ist gut")).isTrue();
    assertThat(gerView.getAnnotationIndex().size() == 5).isTrue(); // 4 annots plus
                                                                   // documentAnnotation
    assertThat(CAS.NAME_DEFAULT_SOFA.equals(v1cas.getSofa().getSofaID())).isTrue();
    assertThat(v1cas.getDocumentText().equals("some text for the default text sofa.")).isTrue();

    // reserialize as XMI
    var sw = new StringWriter();
    var xmlSer = new XMLSerializer(sw, false);
    var xmiSer = new XmiCasSerializer(v1cas.getTypeSystem());
    xmiSer.serialize(v1cas, xmlSer.getContentHandler());
    var xml = sw.getBuffer().toString();

    cas.reset();

    // deserialize into another CAS
    deser2 = new XmiCasDeserializer(cas.getTypeSystem());
    deserHandler2 = deser2.getXmiCasHandler(cas);
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(new StringReader(xml)));

    // test it
    engView = cas.getView("EnglishDocument");
    assertThat(engView.getDocumentText().equals("this beer is good")).isTrue();
    assertThat(engView.getAnnotationIndex().size() == 5).isTrue(); // 4 annots plus
                                                                   // documentAnnotation
    gerView = cas.getView("GermanDocument");
    assertThat(gerView.getDocumentText().equals("das bier ist gut")).isTrue();
    assertThat(gerView.getAnnotationIndex().size() == 5).isTrue(); // 4 annots plus
                                                                   // documentAnnotation
    assertThat(CAS.NAME_DEFAULT_SOFA.equals(v1cas.getSofa().getSofaID())).isTrue();
    assertThat(v1cas.getDocumentText().equals("some text for the default text sofa.")).isTrue();
  }

  @Test
  void testDuplicateNsPrefixes() throws Exception {
    TypeSystemDescription ts = new TypeSystemDescription_impl();
    ts.addType("org.bar.foo.Foo", "", "uima.tcas.Annotation");
    ts.addType("org.baz.foo.Foo", "", "uima.tcas.Annotation");
    var cas = CasCreationUtils.createCas(ts, null, null);
    cas.setDocumentText("Foo");
    var t1 = cas.getTypeSystem().getType("org.bar.foo.Foo");
    var t2 = cas.getTypeSystem().getType("org.baz.foo.Foo");
    var a1 = cas.createAnnotation(t1, 0, 3);
    cas.addFsToIndexes(a1);
    var a2 = cas.createAnnotation(t2, 0, 3);
    cas.addFsToIndexes(a2);

    var baos = new ByteArrayOutputStream();
    XmiCasSerializer.serialize(cas, baos);
    baos.close();
    var bytes = baos.toByteArray();

    var cas2 = CasCreationUtils.createCas(ts, null, null);
    var bais = new ByteArrayInputStream(bytes);
    XmiCasDeserializer.deserialize(bais, cas2);
    bais.close();

    CasComparer.assertEquals(cas, cas2);
  }

  @Test
  void testMerging() throws Exception {
    testMerging(false);
  }

  @Test
  void testDeltaCasMerging() throws Exception {
    testMerging(true);
  }

  // Test merging with or without using delta CASes
  private void testMerging(boolean useDeltas) throws Exception {
    // deserialize a complex CAS from XCAS
    var cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    var ts = cas.getTypeSystem();
    InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
    XCASDeserializer.deserialize(serCasStream, cas);
    serCasStream.close();
    var numAnnotations = cas.getAnnotationIndex().size(); // for comparison later
    var docText = cas.getDocumentText(); // for comparison later
    // add a new Sofa to test that multiple Sofas in original CAS work
    var preexistingView = cas.createView("preexistingView");
    var preexistingViewText = "John Smith blah blah blah";
    preexistingView.setDocumentText(preexistingViewText);
    createPersonAnnot(preexistingView, 0, 10);

    // do XMI serialization to a string, using XmiSerializationSharedData
    // to keep track of maximum ID generated
    var serSharedData = new XmiSerializationSharedData();
    var xmiStr = serialize(cas, serSharedData);
    var maxOutgoingXmiId = serSharedData.getMaxXmiId();

    // deserialize into two new CASes, each with its own instance of XmiSerializationSharedData
    // so we can get consistent IDs later when serializing back.
    var newCas1 = CasCreationUtils.createCas(ts, new TypePriorities_impl(), indexes, null);
    var deserSharedData1 = new XmiSerializationSharedData();
    deserialize(xmiStr, newCas1, deserSharedData1, false, -1);

    var newCas2 = CasCreationUtils.createCas(ts, new TypePriorities_impl(), indexes, null);
    var deserSharedData2 = new XmiSerializationSharedData();
    deserialize(xmiStr, newCas2, deserSharedData2, false, -1);

    Marker marker1 = null;
    Marker marker2 = null;
    if (useDeltas) {
      // create Marker before adding new FSs
      marker1 = newCas1.createMarker();
      marker2 = newCas2.createMarker();
    }

    // add new FS to each new CAS
    createPersonAnnot(newCas1, 0, 10);
    createPersonAnnot(newCas1, 20, 30);
    createPersonAnnot(newCas2, 40, 50);
    var person = createPersonAnnot(newCas2, 60, 70);

    // add an Owner relation that points to an organization in the original CAS,
    // to test links across merge boundary
    var orgType = newCas2.getTypeSystem().getType("org.apache.uima.testTypeSystem.Organization");
    var org = newCas2.getAnnotationIndex(orgType).iterator().next();
    var ownerType = newCas2.getTypeSystem().getType("org.apache.uima.testTypeSystem.Owner");
    var argsFeat = ownerType.getFeatureByBaseName("relationArgs");
    var componentIdFeat = ownerType.getFeatureByBaseName("componentId");
    var relArgsType = newCas2.getTypeSystem()
            .getType("org.apache.uima.testTypeSystem.BinaryRelationArgs");
    var domainFeat = relArgsType.getFeatureByBaseName("domainValue");
    var rangeFeat = relArgsType.getFeatureByBaseName("rangeValue");
    var ownerAnnot = newCas2.createAnnotation(ownerType, 0, 70);
    var relArgs = newCas2.createFS(relArgsType);
    relArgs.setFeatureValue(domainFeat, person);
    relArgs.setFeatureValue(rangeFeat, org);
    ownerAnnot.setFeatureValue(argsFeat, relArgs);
    ownerAnnot.setStringValue(componentIdFeat, "XCasDeserializerTest");
    newCas2.addFsToIndexes(ownerAnnot);
    var orgBegin = org.getBegin();
    var orgEnd = org.getEnd();

    // add Sofas
    var newView1 = newCas1.createView("newSofa1");
    final var sofaText1 = "This is a new Sofa, created in CAS 1.";
    newView1.setDocumentText(sofaText1);
    final var annotText = "Sofa";
    var annotStart1 = sofaText1.indexOf(annotText);
    var annot1 = newView1.createAnnotation(orgType, annotStart1, annotStart1 + annotText.length());
    newView1.addFsToIndexes(annot1);
    var newView2 = newCas2.createView("newSofa2");
    final var sofaText2 = "This is another new Sofa, created in CAS 2.";
    newView2.setDocumentText(sofaText2);
    var annotStart2 = sofaText2.indexOf(annotText);
    var annot2 = newView2.createAnnotation(orgType, annotStart2, annotStart2 + annotText.length());
    newView2.addFsToIndexes(annot2);

    // Add an FS with an array of existing annotations in another view
    var nToks = 3;
    var array = newView2.createArrayFS(nToks);
    var thingType = newCas2.getTypeSystem().getType("org.apache.uima.testTypeSystem.Thing");
    FSIterator thingIter = newCas2.getAnnotationIndex(thingType).iterator();
    for (var i = 0; i < nToks; ++i) {
      array.set(i, (FeatureStructure) thingIter.next());
    }
    var annotArrayTestType = newView2.getTypeSystem()
            .getType("org.apache.uima.testTypeSystem.AnnotationArrayTest");
    var annotArrayFeat = annotArrayTestType.getFeatureByBaseName("arrayOfAnnotations");
    var fsArrayTestAnno = newView2.createAnnotation(annotArrayTestType, 13, 27);
    fsArrayTestAnno.setFeatureValue(annotArrayFeat, array);
    newView2.addFsToIndexes(fsArrayTestAnno);

    // re-serialize each new CAS back to XMI, keeping consistent ids
    var newSerCas1 = serialize(newCas1, deserSharedData1, marker1);
    var newSerCas2 = serialize(newCas2, deserSharedData2, marker2);

    // merge the two XMI CASes back into the original CAS
    // the shared data will be reset and recreated if not using deltaCas
    if (useDeltas) {
      deserialize(newSerCas1, cas, serSharedData, false, maxOutgoingXmiId);
    } else {
      deserialize(newSerCas1, cas, serSharedData, false, -1);
    }
    assertThat(cas.getAnnotationIndex().size()).isEqualTo(numAnnotations + 2);

    deserialize(newSerCas2, cas, serSharedData, false, maxOutgoingXmiId);
    assertThat(cas.getAnnotationIndex().size()).isEqualTo(numAnnotations + 5);
    assertThat(cas.getDocumentText()).isEqualTo(docText);

    // Serialize/deserialize again in case merge created duplicate ids
    var newSerCasMerged = serialize(cas, serSharedData);

    deserialize(newSerCasMerged, cas, serSharedData, false, -1);

    // check covered text of annotations
    var iter = cas.getAnnotationIndex().iterator();
    while (iter.hasNext()) {
      var annot = (AnnotationFS) iter.next();
      assertThat(annot.getCoveredText())
              .isEqualTo(cas.getDocumentText().substring(annot.getBegin(), annot.getEnd()));
    }
    // check Owner annotation we created to test link across merge boundary
    iter = cas.getAnnotationIndex(ownerType).iterator();
    while (iter.hasNext()) {
      var annot = iter.next();
      var componentId = annot.getStringValue(componentIdFeat);
      if ("XCasDeserializerTest".equals(componentId)) {
        var targetRelArgs = annot.getFeatureValue(argsFeat);
        var targetDomain = (AnnotationFS) targetRelArgs.getFeatureValue(domainFeat);
        assertThat(targetDomain.getBegin()).isEqualTo(60);
        assertThat(targetDomain.getEnd()).isEqualTo(70);
        var targetRange = (AnnotationFS) targetRelArgs.getFeatureValue(rangeFeat);
        assertThat(targetRange.getBegin()).isEqualTo(orgBegin);
        assertThat(targetRange.getEnd()).isEqualTo(orgEnd);
      }
    }
    // check Sofas
    var targetView1 = cas.getView("newSofa1");
    assertThat(targetView1.getDocumentText()).isEqualTo(sofaText1);
    var targetView2 = cas.getView("newSofa2");
    assertThat(targetView2.getDocumentText()).isEqualTo(sofaText2);
    var targetAnnot1 = targetView1.getAnnotationIndex(orgType).iterator().get();
    assertThat(targetAnnot1.getCoveredText()).isEqualTo(annotText);
    var targetAnnot2 = targetView2.getAnnotationIndex(orgType).iterator().get();
    assertThat(targetAnnot2.getCoveredText()).isEqualTo(annotText);
    assertThat(targetView1.getSofa().getSofaRef() != targetView2.getSofa().getSofaRef()).isTrue();

    var checkPreexistingView = cas.getView("preexistingView");
    assertThat(checkPreexistingView.getDocumentText()).isEqualTo(preexistingViewText);
    var personType = cas.getTypeSystem().getType("org.apache.uima.testTypeSystem.Person");
    var targetAnnot3 = checkPreexistingView.getAnnotationIndex(personType).iterator().get();
    assertThat(targetAnnot3.getCoveredText()).isEqualTo("John Smith");

    // Check the FS with an array of pre-existing FSs
    iter = targetView2.getAnnotationIndex(annotArrayTestType).iterator();
    componentIdFeat = thingType.getFeatureByBaseName("componentId");
    while (iter.hasNext()) {
      var annot = (AnnotationFS) iter.next();
      var fsArray = (ArrayFS) annot.getFeatureValue(annotArrayFeat);
      assertThat(fsArray.size() == 3).isTrue();
      for (var i = 0; i < fsArray.size(); ++i) {
        var refAnno = (AnnotationFS) fsArray.get(i);
        assertThat(refAnno.getType().getName()).isEqualTo(thingType.getName());
        assertThat(refAnno.getStringValue(componentIdFeat)).isEqualTo("JResporator");
        assertThat(cas == refAnno.getView()).isTrue();
      }
    }

    // try an initial CAS that contains multiple Sofas

  }

  @Test
  void testDeltaCasIgnorePreexistingFS() throws Exception {
    var cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    var ts = cas1.getTypeSystem();
    var cas2 = CasCreationUtils.createCas(ts, new TypePriorities_impl(), indexes, null);
    cas1.setDocumentText("This is a test document in the initial view");
    var anAnnot1 = cas1.createAnnotation(cas1.getAnnotationType(), 0, 4);
    cas1.getIndexRepository().addFS(anAnnot1);
    var anAnnot2 = cas1.createAnnotation(cas1.getAnnotationType(), 5, 10);
    cas1.getIndexRepository().addFS(anAnnot2);
    FSIndex tIndex = cas1.getAnnotationIndex();
    assertThat(tIndex.size() == 3).isTrue(); // doc annot plus annots

    // serialize complete
    var sharedData = new XmiSerializationSharedData();
    var xml = this.serialize(cas1, sharedData);
    var maxOutgoingXmiId = sharedData.getMaxXmiId();
    // deserialize into cas2
    var sharedData2 = new XmiSerializationSharedData();
    // XmiCasDeserializer.deserialize(new StringBufferInputStream(xml), cas2, true, sharedData2);
    this.deserialize(xml, cas2, sharedData2, true, -1);
    CasComparer.assertEquals(cas1, cas2);

    // create Marker, add/modify fs and serialize in delta xmi format.
    var marker = cas2.createMarker();
    FSIndex<AnnotationFS> cas2tIndex = cas2.getAnnotationIndex();

    // create an annotation and add to index
    var cas2newAnnot = cas2.createAnnotation(cas2.getAnnotationType(), 6, 8);
    cas2.getIndexRepository().addFS(cas2newAnnot);
    assertThat(cas2tIndex.size() == 4).isTrue(); // prev annots and this new one

    // modify an existing annotation
    Iterator<AnnotationFS> tIndexIter = cas2tIndex.iterator();
    var docAnnot = (AnnotationFS) tIndexIter.next(); // doc annot
    // delete from index
    var delAnnot = (AnnotationFS) tIndexIter.next(); // annot
    cas2.getIndexRepository().removeFS(delAnnot);
    assertThat(cas2.getAnnotationIndex().size() == 3).isTrue();

    // modify language feature
    var languageF2 = cas2.getDocumentAnnotation().getType()
            .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_LANGUAGE);
    docAnnot.setStringValue(languageF2, "en");
    // serialize cas2 in delta format
    var deltaxml1 = serialize(cas2, sharedData2, marker);
    // System.out.println("delta cas");
    // System.out.println(deltaxml1);

    // deserialize delta xmi into cas1
    this.deserialize(deltaxml1, cas1, sharedData, true, maxOutgoingXmiId,
            AllowPreexistingFS.ignore);

    // check language feature of doc annot is not changed.
    // System.out.println(cas1.getDocumentAnnotation().getStringValue(languageF));
    var languageF1 = cas1.getDocumentAnnotation().getType()
            .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_LANGUAGE);

    assertThat(cas1.getAnnotationIndex().iterator().next().getStringValue(languageF1)
            .equals("x-unspecified")).isTrue();
    // check new annotation exists and preexisting is not deleted
    assertThat(cas1.getAnnotationIndex().size() == 4).isTrue();
  }

  @Test
  void testDeltaCasDisallowPreexistingFSMod() throws Exception {
    var cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    var ts = cas1.getTypeSystem();
    var cas2 = CasCreationUtils.createCas(ts, new TypePriorities_impl(), indexes, null);
    cas1.setDocumentText("This is a test document in the initial view");
    var anAnnot1 = cas1.createAnnotation(cas1.getAnnotationType(), 0, 4);
    cas1.getIndexRepository().addFS(anAnnot1);
    var anAnnot2 = cas1.createAnnotation(cas1.getAnnotationType(), 5, 10);
    cas1.getIndexRepository().addFS(anAnnot2);
    FSIndex<AnnotationFS> tIndex = cas1.getAnnotationIndex();
    assertThat(tIndex.size() == 3).isTrue(); // doc annot plus 2 annots

    // serialize complete
    var sharedData = new XmiSerializationSharedData();
    var xml = serialize(cas1, sharedData);
    var maxOutgoingXmiId = sharedData.getMaxXmiId();

    // deserialize into cas2
    var sharedData2 = new XmiSerializationSharedData();
    this.deserialize(xml, cas2, sharedData2, true, -1);
    CasComparer.assertEquals(cas1, cas2);

    // create Marker, add/modify fs and serialize in delta xmi format.
    var marker = cas2.createMarker();
    FSIndex<AnnotationFS> cas2tIndex = cas2.getAnnotationIndex();

    // create an annotation and add to index
    var cas2newAnnot = cas2.createAnnotation(cas2.getAnnotationType(), 6, 8);
    cas2.getIndexRepository().addFS(cas2newAnnot);
    assertThat(cas2tIndex).hasSize(4); // prev annots and this new one

    // modify language feature
    Iterator<AnnotationFS> tIndexIter = cas2tIndex.iterator();
    var docAnnot = (AnnotationFS) tIndexIter.next();
    var languageF = cas2.getDocumentAnnotation().getType()
            .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_LANGUAGE);
    docAnnot.setStringValue(languageF, "en");

    // serialize cas2 in delta format
    var deltaxml1 = serialize(cas2, sharedData2, marker);
    // System.out.println(deltaxml1);

    // deserialize delta xmi into cas1
    var threw = false;
    try {
      deserialize(deltaxml1, cas1, sharedData, true, maxOutgoingXmiId, AllowPreexistingFS.disallow);
    } catch (CASRuntimeException e) {
      assertThat(e.getMessageKey() == CASRuntimeException.DELTA_CAS_PREEXISTING_FS_DISALLOWED)
              .isTrue();
      threw = true;
    }
    assertThat(threw).isTrue();

    // check language feature of doc annot is not changed.
    // System.out.println(cas1.getDocumentAnnotation().getStringValue(languageF));
    assertThat(cas1.getAnnotationIndex().iterator().next().getStringValue(languageF)
            .equals("x-unspecified")).isTrue();
    // check new annotation exists
    assertThat(cas1.getAnnotationIndex().size() == 3).isTrue(); // cas2 should be unchanged.
  }

  @Test
  void testDeltaCasInvalidMarker() throws Exception {
    var cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    var ts = cas1.getTypeSystem();

    var cas2 = CasCreationUtils.createCas(ts, new TypePriorities_impl(), indexes, null);

    // serialize complete
    var sharedData = new XmiSerializationSharedData();
    var xml = serialize(cas1, sharedData);
    var maxOutgoingXmiId = sharedData.getMaxXmiId();

    // deserialize into cas2
    var sharedData2 = new XmiSerializationSharedData();
    this.deserialize(xml, cas2, sharedData2, true, -1);
    CasComparer.assertEquals(cas1, cas2);

    // create Marker, add/modify fs and serialize in delta xmi format.
    var marker = cas2.createMarker();
    var caughtMutlipleMarker = false;
    try {
      var marker2 = cas2.createMarker();
    } catch (UIMARuntimeException e) {
      caughtMutlipleMarker = true;
      System.out.format("Should catch MultipleCreateMarker message: %s%n", e.getMessage());
    }
    assertThat(caughtMutlipleMarker).isTrue();

    // reset cas
    cas2.reset();
    var serfailed = false;
    try {
      serialize(cas2, sharedData2, marker);
    } catch (CASRuntimeException e) {
      serfailed = true;
    }
    assertThat(serfailed).isTrue();

    // serfailed = false;
    // try {
    // serialize(cas2, sharedData2, marker2);
    // } catch (CASRuntimeException e) {
    // serfailed = true;
    // }
    // assertTrue(serfailed);
  }

  @Test
  void testDeltaCasNoChanges() throws Exception {
    var cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    var ts = cas1.getTypeSystem();
    var cas2 = CasCreationUtils.createCas(ts, new TypePriorities_impl(), indexes, null);

    // serialize complete
    var sharedData = new XmiSerializationSharedData();
    var xml = serialize(cas1, sharedData);
    var maxOutgoingXmiId = sharedData.getMaxXmiId();

    // deserialize into cas2
    var sharedData2 = new XmiSerializationSharedData();
    this.deserialize(xml, cas2, sharedData2, true, -1);
    CasComparer.assertEquals(cas1, cas2);

    // create Marker, add/modify fs and serialize in delta xmi format.
    var marker = cas2.createMarker();
    FSIndex<AnnotationFS> cas2tIndex = cas2.getAnnotationIndex();

    // serialize cas2 in delta format
    var deltaxml1 = serialize(cas2, sharedData2, marker);
    // System.out.println(deltaxml1);

    // deserialize delta xmi into cas1
    try {
      this.deserialize(deltaxml1, cas1, sharedData, true, maxOutgoingXmiId,
              AllowPreexistingFS.disallow);
    } catch (CASRuntimeException e) {
      assertThat(e.getMessageKey() == CASRuntimeException.DELTA_CAS_PREEXISTING_FS_DISALLOWED)
              .isTrue();
    }

    // check new annotation index
    assertThat(cas1.getAnnotationIndex().size() == 0).isTrue(); // cas2 should be unchanged.
  }

  @Test
  void testDeltaCasDisallowPreexistingFSViewMod() throws Exception {
    var cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    var ts = cas1.getTypeSystem();
    var cas2 = CasCreationUtils.createCas(ts, new TypePriorities_impl(), indexes, null);
    cas1.setDocumentText("This is a test document in the initial view");
    var anAnnot1 = cas1.createAnnotation(cas1.getAnnotationType(), 0, 4);
    cas1.getIndexRepository().addFS(anAnnot1);
    var anAnnot2 = cas1.createAnnotation(cas1.getAnnotationType(), 5, 10);
    cas1.getIndexRepository().addFS(anAnnot2);
    FSIndex<AnnotationFS> tIndex = cas1.getAnnotationIndex();
    assertThat(tIndex.size() == 3).isTrue(); // doc annot plus 2 annots

    // serialize complete
    var sharedData = new XmiSerializationSharedData();
    var xml = serialize(cas1, sharedData);
    var maxOutgoingXmiId = sharedData.getMaxXmiId();

    // deserialize into cas2
    var sharedData2 = new XmiSerializationSharedData();
    this.deserialize(xml, cas2, sharedData2, true, -1);
    CasComparer.assertEquals(cas1, cas2);

    // create Marker, add/modify fs and serialize in delta xmi format.
    var marker = cas2.createMarker();
    FSIndex<AnnotationFS> cas2tIndex = cas2.getAnnotationIndex();

    // create an annotation and add to index
    var cas2newAnnot = cas2.createAnnotation(cas2.getAnnotationType(), 6, 8);
    cas2.getIndexRepository().addFS(cas2newAnnot);
    assertThat(cas2tIndex.size() == 4).isTrue(); // prev annots and this new one

    // modify language feature
    Iterator<AnnotationFS> tIndexIter = cas2tIndex.iterator();
    var docAnnot = (AnnotationFS) tIndexIter.next();

    // delete annotation from index
    var delAnnot = (AnnotationFS) tIndexIter.next(); // annot
    cas2.getIndexRepository().removeFS(delAnnot);
    assertThat(cas2.getAnnotationIndex().size() == 3).isTrue();

    // serialize cas2 in delta format
    var deltaxml1 = serialize(cas2, sharedData2, marker);
    // System.out.println(deltaxml1);

    // deserialize delta xmi into cas1
    try {
      this.deserialize(deltaxml1, cas1, sharedData, true, maxOutgoingXmiId,
              AllowPreexistingFS.disallow);
    } catch (CASRuntimeException e) {
      assertThat(e.getMessageKey() == CASRuntimeException.DELTA_CAS_PREEXISTING_FS_DISALLOWED)
              .isTrue();
    }

    // check new annotation added and preexisitng FS not removed from index
    assertThat(cas1.getAnnotationIndex().size() == 4).isTrue();
  }

  //@formatter:off
  /*
   * This test looks at delta cas serialization and deserialization.
   * 
   * First it makes 3 CASs cas1, cas2, cas3, with shared typesystem.
   * Sets up types and features:
   *    Person  { componentId, confidence }  super is EntityAnnotation
   *    Organization
   *    Owner            { relationArgs
   *    EntityAnnotation { mentionType
   *    BinaryRelationArgs { domainValue, rangeValue }
   *    Entity             { classes, links, canonicalForm
   *    
   *    NonEmptyFSList     { head, tail
   *    EmptyFSList
   *    
   * cas1:
   *     _InitialView: docText = "This is a test document in the initial view"
   *       annot1 = Annotation  0,  4, -> index
   *       annot2 = Annotation  5,  6  -> index
   *       annot3 = Annotation  8, 13  -> index
   *       annot4 = Annotation 15, 30  -> index
   *       
   *       Entity  classes={"class1"}           -> index
   *               links= FsList: {annot1, annot2} 
   *     View1:
   *     
   *     preexistingView: docText = "John Smith blah blah blah"
   *       person1Annot = Person 0, 10, componentId: "deltacas1" -> index
   *       person2Annot = Person 0, 5                            -> index
   *       orgAnnot     = Organization 16, 24                    -> index
   *       ownerAnnot   = Owner 0, 24 relationArgs:              -> index 
   *                                    BinaryRelArg 
   *                                      domain: person1Annot
   * -------------------------------------------------------------------------- 
   * serialize cas1 (sharedData initially empty) -> xml  
   * deser  xml -> cas2, using sharedData2, compare.
   * ----------------------------
   * In cas2:
   *   create Marker.   
   *     _InitialView: 
   *       annot5 = Annotation  6,  8, -> index
   *     View1: docText = "This is the View1 document." 
   *       c2v1Annot = Annotation 1, 5                          -> index
   * ----------------------------      
   * modify existing annotation:
   *     _InitialView:
   *       documentAnnotation - modify language to "en"
   *       remove annot1 from index, mod annot1 end to 4 (was 4), add back to index
   *       remove annot2 from index
   *     _PreexistingView
   *       person1Annot: confidence = 99.99
   *       person2Annot: mentionType = "FIRSTNAME"
   *       orgAnnot:     mentionType = "ORGNAME"
   *       ownerAnnot:   args: rangeFeat: orgAnnot
   * ----------------------------------------------
   *    _InitialView:
   *      entityFS: classes: set string array 1-4: class2, 3, 4, 5
   *                links: add a 3rd node to the list, head: annot5
   * ----------------------------------------------
   * serialize cas2 -> deltaxml1 using sharedData2, marker
   * deserialize -> cas1 using it's sharedData  
   * 
   */
  //@formatter:on
  @Test
  void testDeltaCasAllowPreexistingFS() throws Exception {
    var cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    var sharedTS = cas1.getTypeSystem();
    var cas2 = CasCreationUtils.createCas(sharedTS, new TypePriorities_impl(), indexes, null);
    var cas3 = CasCreationUtils.createCas(sharedTS, new TypePriorities_impl(), indexes, null);

    var personType = cas1.getTypeSystem().getType("org.apache.uima.testTypeSystem.Person");
    var componentIdFeat = personType.getFeatureByBaseName("componentId");
    var confidenceFeat = personType.getFeatureByBaseName("confidence");
    var orgType = cas1.getTypeSystem().getType("org.apache.uima.testTypeSystem.Organization");
    var ownerType = cas1.getTypeSystem().getType("org.apache.uima.testTypeSystem.Owner");
    var entityAnnotType = cas1.getTypeSystem()
            .getType("org.apache.uima.testTypeSystem.EntityAnnotation");
    var mentionTypeFeat = entityAnnotType.getFeatureByBaseName("mentionType");
    var argsFeat = ownerType.getFeatureByBaseName("relationArgs");
    var relArgsType = cas1.getTypeSystem()
            .getType("org.apache.uima.testTypeSystem.BinaryRelationArgs");
    var domainFeat = relArgsType.getFeatureByBaseName("domainValue");
    var rangeFeat = relArgsType.getFeatureByBaseName("rangeValue");

    var entityType = cas1.getTypeSystem().getType("org.apache.uima.testTypeSystem.Entity");
    var classesFeat = entityType.getFeatureByBaseName("classes");
    var linksFeat = entityType.getFeatureByBaseName("links");
    var canonicalFormFeat = entityType.getFeatureByBaseName("canonicalForm");

    var nonEmptyFsListType = cas1.getTypeSystem().getType(CAS.TYPE_NAME_NON_EMPTY_FS_LIST);
    var emptyFsListType = cas1.getTypeSystem().getType(CAS.TYPE_NAME_EMPTY_FS_LIST);
    var headFeat = nonEmptyFsListType.getFeatureByBaseName("head");
    var tailFeat = nonEmptyFsListType.getFeatureByBaseName("tail");

    // cas1
    // initial set of feature structures
    // set document text for the initial view and create Annotations
    cas1.setDocumentText("This is a test document in the initial view");
    var anAnnot1 = cas1.createAnnotation(cas1.getAnnotationType(), 0, 4);
    cas1.getIndexRepository().addFS(anAnnot1);
    var anAnnot2 = cas1.createAnnotation(cas1.getAnnotationType(), 5, 6);
    cas1.getIndexRepository().addFS(anAnnot2);
    var anAnnot3 = cas1.createAnnotation(cas1.getAnnotationType(), 8, 13);
    cas1.getIndexRepository().addFS(anAnnot3);
    var anAnnot4 = cas1.createAnnotation(cas1.getAnnotationType(), 15, 30);
    cas1.getIndexRepository().addFS(anAnnot4);
    FSIndex<AnnotationFS> tIndex = cas1.getAnnotationIndex();
    assertThat(tIndex.size() == 5).isTrue(); // doc annot plus 4 annots

    var entityFS = cas1.createFS(entityType);
    cas1.getIndexRepository().addFS(entityFS);

    var strArrayFS = cas1.createStringArrayFS(5);
    strArrayFS.set(0, "class1");
    entityFS.setFeatureValue(classesFeat, strArrayFS);

    // create listFS and set the link feature
    var emptyNode = cas1.createFS(emptyFsListType);
    var secondNode = cas1.createFS(nonEmptyFsListType);
    secondNode.setFeatureValue(headFeat, anAnnot2);
    secondNode.setFeatureValue(tailFeat, emptyNode);
    var firstNode = cas1.createFS(nonEmptyFsListType);
    firstNode.setFeatureValue(headFeat, anAnnot1);
    firstNode.setFeatureValue(tailFeat, secondNode);
    entityFS.setFeatureValue(linksFeat, firstNode);

    // create a view w/o setting document text
    var view1 = cas1.createView("View1");

    // create another view
    var preexistingView = cas1.createView("preexistingView");
    var preexistingViewText = "John Smith blah blah blah";
    preexistingView.setDocumentText(preexistingViewText);
    var person1Annot = createPersonAnnot(preexistingView, 0, 10);
    person1Annot.setStringValue(componentIdFeat, "deltacas1");
    var person2Annot = createPersonAnnot(preexistingView, 0, 5);
    var orgAnnot = preexistingView.createAnnotation(orgType, 16, 24);
    preexistingView.addFsToIndexes(orgAnnot);

    var ownerAnnot = preexistingView.createAnnotation(ownerType, 0, 24);
    preexistingView.addFsToIndexes(ownerAnnot);
    var relArgs = cas1.createFS(relArgsType);
    relArgs.setFeatureValue(domainFeat, person1Annot);
    ownerAnnot.setFeatureValue(argsFeat, relArgs);

    // serialize complete
    var sharedData = new XmiSerializationSharedData();
    var xml = serialize(cas1, sharedData);
    var maxOutgoingXmiId = sharedData.getMaxXmiId();
    // System.out.println("CAS1 " + xml);
    // System.out.println("MaxOutgoingXmiId " + maxOutgoingXmiId);

    // deserialize into cas2
    var sharedData2 = new XmiSerializationSharedData();
    this.deserialize(xml, cas2, sharedData2, true, -1);
    CasComparer.assertEquals(cas1, cas2);

    // =======================================================================
    // create Marker, add/modify fs and serialize in delta xmi format.
    var marker = cas2.createMarker();
    FSIndex<AnnotationFS> cas2tIndex = cas2.getAnnotationIndex();
    var cas2preexistingView = cas2.getView("preexistingView");
    FSIndex cas2personIndex = cas2preexistingView.getAnnotationIndex(personType);
    FSIndex cas2orgIndex = cas2preexistingView.getAnnotationIndex(orgType);
    FSIndex cas2ownerIndex = cas2preexistingView.getAnnotationIndex(ownerType);

    // create an annotation and add to index
    var cas2anAnnot5 = cas2.createAnnotation(cas2.getAnnotationType(), 6, 8);
    cas2.getIndexRepository().addFS(cas2anAnnot5);
    assertThat(cas2tIndex.size() == 6).isTrue(); // prev annots and this new one

    // set document text of View1
    var cas2view1 = cas2.getView("View1");
    cas2view1.setDocumentText("This is the View1 document.");
    // create an annotation in View1
    var cas2view1Annot = cas2view1.createAnnotation(cas2.getAnnotationType(), 1, 5);
    cas2view1.getIndexRepository().addFS(cas2view1Annot);
    FSIndex cas2view1Index = cas2view1.getAnnotationIndex();
    assertThat(cas2view1Index.size() == 2).isTrue(); // document annot and this annot

    // modify an existing annotation
    Iterator<AnnotationFS> tIndexIter = cas2tIndex.iterator();
    var docAnnot = (AnnotationFS) tIndexIter.next(); // doc annot
    var modAnnot1 = (AnnotationFS) tIndexIter.next();
    var delAnnot = (AnnotationFS) tIndexIter.next();

    // modify language feature
    var languageF = cas2.getDocumentAnnotation().getType()
            .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_LANGUAGE);
    docAnnot.setStringValue(languageF, "en");

    // index update - reindex
    cas2.getIndexRepository().removeFS(modAnnot1);
    var endF = cas2.getAnnotationType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_END);
    modAnnot1.setIntValue(endF, 4);
    cas2.getIndexRepository().addFS(modAnnot1);
    // index update - remove annotation from index
    cas2.getIndexRepository().removeFS(delAnnot);

    // modify FS - string feature and FS feature.
    Iterator<FeatureStructure> personIter = cas2personIndex.iterator();
    var cas2person1 = (AnnotationFS) personIter.next();
    var cas2person2 = (AnnotationFS) personIter.next();

    cas2person1.setFloatValue(confidenceFeat, (float) 99.99);
    cas2person1.setStringValue(mentionTypeFeat, "FULLNAME");

    cas2person2.setStringValue(componentIdFeat, "delataCas2");
    cas2person2.setStringValue(mentionTypeFeat, "FIRSTNAME");

    Iterator<FeatureStructure> orgIter = cas2orgIndex.iterator();
    var cas2orgAnnot = (AnnotationFS) orgIter.next();
    cas2orgAnnot.setStringValue(mentionTypeFeat, "ORGNAME");

    // modify FS feature
    Iterator<FeatureStructure> ownerIter = cas2ownerIndex.iterator();
    var cas2ownerAnnot = (AnnotationFS) ownerIter.next();
    var cas2relArgs = cas2ownerAnnot.getFeatureValue(argsFeat);
    cas2relArgs.setFeatureValue(rangeFeat, cas2orgAnnot);

    // Test modification of a nonshared multivalued feature.
    // This should serialize the encompassing FS.
    Iterator<FeatureStructure> iter = cas2.getIndexRepository().getIndex("testEntityIndex")
            .iterator();
    var cas2EntityFS = iter.next();
    var cas2strarrayFS = (StringArrayFS) cas2EntityFS.getFeatureValue(classesFeat);
    cas2strarrayFS.set(1, "class2");
    cas2strarrayFS.set(2, "class3");
    cas2strarrayFS.set(3, "class4");
    cas2strarrayFS.set(4, "class5");

    // add to FSList
    var cas2linksFS = cas2EntityFS.getFeatureValue(linksFeat);
    var cas2secondNode = cas2linksFS.getFeatureValue(tailFeat);
    var cas2emptyNode = cas2secondNode.getFeatureValue(tailFeat);
    var cas2thirdNode = cas2.createFS(nonEmptyFsListType);
    cas2thirdNode.setFeatureValue(headFeat, cas2anAnnot5);
    cas2thirdNode.setFeatureValue(tailFeat, cas2emptyNode);
    cas2secondNode.setFeatureValue(tailFeat, cas2thirdNode);

    // // Test that the new access method returns an array containing just the right marker
    // removed per https://issues.apache.org/jira/browse/UIMA-2478
    // List<Marker> mkrs = cas2.getMarkers();
    // assertNotNull(mkrs);
    // assertEquals(1, mkrs.size());
    // assertEquals(marker, mkrs.get(0));

    // serialize cas2 in delta format
    var deltaxml1 = serialize(cas2, sharedData2, marker);
    // System.out.println("delta cas");
    // System.out.println(deltaxml1);

    // ======================================================================
    // deserialize delta xmi into cas1
    this.deserialize(deltaxml1, cas1, sharedData, true, maxOutgoingXmiId, AllowPreexistingFS.allow);

    // ======================================================================
    // serialize complete cas and deserialize into cas3 and compare with cas1.
    var fullxml = serialize(cas2, sharedData2);
    var sharedData3 = new XmiSerializationSharedData();
    this.deserialize(fullxml, cas3, sharedData3, true, -1);
    CasComparer.assertEquals(cas1, cas3);

    // System.out.println("CAS1 " + serialize(cas1, new XmiSerializationSharedData()));
    // System.out.println("CAS2 " + serialize(cas2, new XmiSerializationSharedData()));
  }

  @Test
  void testDeltaCasListFS() throws Exception {
    var cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    var ts = cas1.getTypeSystem();
    var cas2 = CasCreationUtils.createCas(ts, new TypePriorities_impl(), indexes, null);
    var cas3 = CasCreationUtils.createCas(ts, new TypePriorities_impl(), indexes, null);

    var entityType = cas1.getTypeSystem().getType("org.apache.uima.testTypeSystem.Entity");
    var classesFeat = entityType.getFeatureByBaseName("classes");
    var linksFeat = entityType.getFeatureByBaseName("links");
    var canonicalFormFeat = entityType.getFeatureByBaseName("canonicalForm");

    var nonEmptyFsListType = cas1.getTypeSystem().getType(CAS.TYPE_NAME_NON_EMPTY_FS_LIST);
    var emptyFsListType = cas1.getTypeSystem().getType(CAS.TYPE_NAME_EMPTY_FS_LIST);
    var headFeat = nonEmptyFsListType.getFeatureByBaseName("head");
    var tailFeat = nonEmptyFsListType.getFeatureByBaseName("tail");

    // cas1
    // initial set of feature structures
    // set document text for the initial view and create Annotations
    cas1.setDocumentText("This is a test document in the initial view");
    var anAnnot1 = cas1.createAnnotation(cas1.getAnnotationType(), 0, 4);
    cas1.getIndexRepository().addFS(anAnnot1);
    var anAnnot2 = cas1.createAnnotation(cas1.getAnnotationType(), 5, 6);
    cas1.getIndexRepository().addFS(anAnnot2);
    var anAnnot3 = cas1.createAnnotation(cas1.getAnnotationType(), 8, 13);
    cas1.getIndexRepository().addFS(anAnnot3);
    var anAnnot4 = cas1.createAnnotation(cas1.getAnnotationType(), 15, 30);
    cas1.getIndexRepository().addFS(anAnnot4);
    FSIndex tIndex = cas1.getAnnotationIndex();
    assertThat(tIndex.size() == 5).isTrue(); // doc annot plus 4 annots

    var entityFS = cas1.createFS(entityType);
    cas1.getIndexRepository().addFS(entityFS);

    var strArrayFS = cas1.createStringArrayFS(5);
    strArrayFS.set(0, "class1");
    entityFS.setFeatureValue(classesFeat, strArrayFS);

    // create listFS and set the link feature
    var emptyNode = cas1.createFS(emptyFsListType);
    var secondNode = cas1.createFS(nonEmptyFsListType);
    secondNode.setFeatureValue(headFeat, anAnnot2);
    secondNode.setFeatureValue(tailFeat, emptyNode);
    var firstNode = cas1.createFS(nonEmptyFsListType);
    firstNode.setFeatureValue(headFeat, anAnnot1);
    firstNode.setFeatureValue(tailFeat, secondNode);
    entityFS.setFeatureValue(linksFeat, firstNode);

    // create a view w/o setting document text
    var view1 = cas1.createView("View1");

    // serialize complete
    var sharedData = new XmiSerializationSharedData();
    var xml = serialize(cas1, sharedData);
    var maxOutgoingXmiId = sharedData.getMaxXmiId();
    // System.out.println("CAS1 " + xml);
    // System.out.println("MaxOutgoingXmiId " + maxOutgoingXmiId);

    // deserialize into cas2
    var sharedData2 = new XmiSerializationSharedData();
    this.deserialize(xml, cas2, sharedData2, true, -1);
    CasComparer.assertEquals(cas1, cas2);

    // =======================================================================
    // create Marker, add/modify fs and serialize in delta xmi format.
    var marker = cas2.createMarker();
    var cas2tIndex = cas2.getAnnotationIndex();

    // create an annotation and add to index
    var cas2anAnnot5 = cas2.createAnnotation(cas2.getAnnotationType(), 6, 8);
    cas2.getIndexRepository().addFS(cas2anAnnot5);
    assertThat(cas2tIndex.size() == 6).isTrue(); // prev annots and this new one
    // create an annotation and add to index
    var cas2anAnnot6 = cas2.createAnnotation(cas2.getAnnotationType(), 6, 8);
    cas2.getIndexRepository().addFS(cas2anAnnot6);
    assertThat(cas2tIndex.size() == 7).isTrue(); // prev annots and twonew one

    // add to FSList
    Iterator<FeatureStructure> iter = cas2.getIndexRepository().getIndex("testEntityIndex")
            .iterator();
    var cas2EntityFS = iter.next();
    var cas2linksFS = cas2EntityFS.getFeatureValue(linksFeat);
    var cas2secondNode = cas2linksFS.getFeatureValue(tailFeat);
    var cas2emptyNode = cas2secondNode.getFeatureValue(tailFeat);
    var cas2thirdNode = cas2.createFS(nonEmptyFsListType);
    var cas2fourthNode = cas2.createFS(nonEmptyFsListType);

    cas2secondNode.setFeatureValue(tailFeat, cas2thirdNode);
    cas2thirdNode.setFeatureValue(headFeat, cas2anAnnot5);
    cas2thirdNode.setFeatureValue(tailFeat, cas2fourthNode);
    cas2fourthNode.setFeatureValue(headFeat, cas2anAnnot6);
    cas2fourthNode.setFeatureValue(tailFeat, cas2emptyNode);

    // serialize cas2 in delta format
    var deltaxml1 = serialize(cas2, sharedData2, marker);
    // System.out.println("delta cas");
    // System.out.println(deltaxml1);

    // ======================================================================
    // deserialize delta xmi into cas1
    deserialize(deltaxml1, cas1, sharedData, true, maxOutgoingXmiId, AllowPreexistingFS.allow);
    CasComparer.assertEquals(cas2linksFS, entityFS.getFeatureValue(linksFeat));
    // ======================================================================
    // serialize complete cas and deserialize into cas3 and compare with cas1.
    var fullxml = serialize(cas2, sharedData2);
    var sharedData3 = new XmiSerializationSharedData();
    this.deserialize(fullxml, cas3, sharedData3, true, -1);
    CasComparer.assertEquals(cas1, cas3);

    // System.out.println("CAS1 " + serialize(cas1, new XmiSerializationSharedData()));
    // System.out.println("CAS2 " + serialize(cas2, new XmiSerializationSharedData()));
  }

  @Test
  void testOutOfTypeSystemData() throws Exception {
    // deserialize a simple XMI into a CAS with no TypeSystem
    var cas = CasCreationUtils.createCas(new TypeSystemDescription_impl(),
            new TypePriorities_impl(), FsIndexDescription.EMPTY_FS_INDEX_DESCRIPTIONS);
    var xmiFile = JUnitExtension.getFile("ExampleCas/simpleCas.xmi");
    var xmiStr = FileUtils.file2String(xmiFile, "UTF-8");

    var sharedData = new XmiSerializationSharedData();
    deserialize(xmiStr, cas, sharedData, true, -1);

    // do some checks on the out-of-type system data
    List ootsElems = sharedData.getOutOfTypeSystemElements();
    assertThat(ootsElems).hasSize(9);
    List ootsViewMembers = sharedData.getOutOfTypeSystemViewMembers("1");
    assertThat(ootsViewMembers).hasSize(7);

    // now reserialize including OutOfTypeSystem data
    var xmiStr2 = serialize(cas, sharedData);

    // deserialize both original and new XMI into CASes that do have the full typesystem
    var newCas1 = CasCreationUtils.createCas(typeSystem, null, indexes);
    var ts = newCas1.getTypeSystem();
    deserialize(xmiStr, newCas1, null, false, -1);
    var newCas2 = CasCreationUtils.createCas(ts, null, indexes, null);
    deserialize(xmiStr2, newCas2, null, false, -1);
    CasComparer.assertEquals(newCas1, newCas2);

    // Test a partial type system with a missing some missing features and
    // missing "Organization" type
    var partialTypeSystemFile = JUnitExtension.getFile("ExampleCas/partialTestTypeSystem.xml");
    var partialTypeSystem = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(partialTypeSystemFile));
    var partialTsCas = CasCreationUtils.createCas(partialTypeSystem, null, indexes);
    var sharedData2 = new XmiSerializationSharedData();
    deserialize(xmiStr, partialTsCas, sharedData2, true, -1);

    assertThat(sharedData2.getOutOfTypeSystemElements()).hasSize(1);
    var ootsFeats3 = sharedData2.getOutOfTypeSystemFeatures(sharedData2.getFsForXmiId(3));
    assertThat(ootsFeats3.attributes).hasSize(1);
    var ootsAttr = ootsFeats3.attributes.get(0);
    assertThat(ootsAttr.name).isEqualTo("mentionType");
    assertThat(ootsAttr.value).isEqualTo("NAME");
    var ootsFeats5 = sharedData2.getOutOfTypeSystemFeatures(sharedData2.getFsForXmiId(5));
    assertThat(ootsFeats5.attributes).isEmpty();
    assertThat(ootsFeats5.childElements).hasSize(1);
    var ootsChildElem = ootsFeats5.childElements.get(0);
    assertThat(ootsChildElem.name.qName).isEqualTo("mentionType");
    assertThat(ootsChildElem.contents).isEqualTo("NAME");

    var ootsFeats8 = sharedData2.getOutOfTypeSystemFeatures(sharedData2.getFsForXmiId(8));
    assertThat(ootsFeats8.attributes).hasSize(1);
    var ootsFeats10 = sharedData2.getOutOfTypeSystemFeatures(sharedData2.getFsForXmiId(10));
    assertThat(ootsFeats10.attributes).hasSize(1);
    var ootsFeats11 = sharedData2.getOutOfTypeSystemFeatures(sharedData2.getFsForXmiId(11));
    assertThat(ootsFeats11.childElements).hasSize(4);

    var xmiStr3 = serialize(partialTsCas, sharedData2);
    newCas2.reset();
    deserialize(xmiStr3, newCas2, null, false, -1);
    CasComparer.assertEquals(newCas1, newCas2);
  }

  @Test
  void testOutOfTypeSystemArrayElement() throws Exception {
    // add to type system an annotation type that has an FSArray feature
    var testAnnotTypeDesc = typeSystem.addType("org.apache.uima.testTypeSystem.TestAnnotation", "",
            "uima.tcas.Annotation");
    testAnnotTypeDesc.addFeature("arrayFeat", "", "uima.cas.FSArray");
    // populate a CAS with such an array
    var cas = CasCreationUtils.createCas(typeSystem, null, null);
    var testAnnotType = cas.getTypeSystem()
            .getType("org.apache.uima.testTypeSystem.TestAnnotation");
    var orgType = cas.getTypeSystem().getType("org.apache.uima.testTypeSystem.Organization");
    var orgAnnot1 = cas.createAnnotation(orgType, 0, 10);
    cas.addFsToIndexes(orgAnnot1);
    var orgAnnot2 = cas.createAnnotation(orgType, 10, 20);
    cas.addFsToIndexes(orgAnnot2);
    var testAnnot = cas.createAnnotation(testAnnotType, 0, 20);
    cas.addFsToIndexes(testAnnot);
    var arrayFs = cas.createArrayFS(2);
    arrayFs.set(0, orgAnnot1);
    arrayFs.set(1, orgAnnot2);
    var arrayFeat = testAnnotType.getFeatureByBaseName("arrayFeat");
    testAnnot.setFeatureValue(arrayFeat, arrayFs);

    // serialize to XMI
    var xmiStr = serialize(cas, null);

    // deserialize into a CAS that's missing the Organization type
    var partialTypeSystemFile = JUnitExtension.getFile("ExampleCas/partialTestTypeSystem.xml");
    var partialTypeSystem = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(partialTypeSystemFile));
    testAnnotTypeDesc = partialTypeSystem.addType("org.apache.uima.testTypeSystem.TestAnnotation",
            "", "uima.tcas.Annotation");
    testAnnotTypeDesc.addFeature("arrayFeat", "", "uima.cas.FSArray");
    var partialTsCas = CasCreationUtils.createCas(partialTypeSystem, null, null);
    var sharedData = new XmiSerializationSharedData();
    deserialize(xmiStr, partialTsCas, sharedData, true, -1);

    // check out of type system data
    var testAnnotType2 = partialTsCas.getTypeSystem()
            .getType("org.apache.uima.testTypeSystem.TestAnnotation");
    var testAnnot2 = partialTsCas.getAnnotationIndex(testAnnotType2).iterator().get();
    var arrayFeat2 = testAnnotType2.getFeatureByBaseName("arrayFeat");
    var arrayFs2 = testAnnot2.getFeatureValue(arrayFeat2);
    var ootsElems = sharedData.getOutOfTypeSystemElements();
    assertThat(ootsElems).hasSize(2);
    var ootsArrayElems = sharedData.getOutOfTypeSystemArrayElements((FSArray) arrayFs2);
    assertThat(ootsArrayElems).hasSize(2);
    for (var i = 0; i < 2; i++) {
      var oed = (OotsElementData) ootsElems.get(i);
      var arel = (XmiArrayElement) ootsArrayElems.get(i);
      assertThat(arel.xmiId).isEqualTo(oed.xmiId);
    }

    // reserialize along with out of type system data
    var xmiStr2 = serialize(partialTsCas, sharedData);

    // deserialize into a new CAS and compare
    var cas2 = CasCreationUtils.createCas(typeSystem, null, null);
    deserialize(xmiStr2, cas2, null, false, -1);

    CasComparer.assertEquals(cas, cas2);
  }

  @Test
  void testOutOfTypeSystemListElement() throws Exception {
    // add to type system an annotation type that has an FSList feature
    var testAnnotTypeDesc = typeSystem.addType("org.apache.uima.testTypeSystem.TestAnnotation", "",
            "uima.tcas.Annotation");
    testAnnotTypeDesc.addFeature("listFeat", "", "uima.cas.FSList");

    // populate a CAS with such an list
    var cas = CasCreationUtils.createCas(typeSystem, null, null);
    var testAnnotType = cas.getTypeSystem()
            .getType("org.apache.uima.testTypeSystem.TestAnnotation");
    var orgType = cas.getTypeSystem().getType("org.apache.uima.testTypeSystem.Organization");

    var orgAnnot1 = cas.createAnnotation(orgType, 0, 10);
    cas.addFsToIndexes(orgAnnot1);
    var orgAnnot2 = cas.createAnnotation(orgType, 10, 20);
    cas.addFsToIndexes(orgAnnot2);
    var testAnnot = cas.createAnnotation(testAnnotType, 0, 20);
    cas.addFsToIndexes(testAnnot);

    var nonEmptyFsListType = cas.getTypeSystem().getType(CAS.TYPE_NAME_NON_EMPTY_FS_LIST);
    var emptyFsListType = cas.getTypeSystem().getType(CAS.TYPE_NAME_EMPTY_FS_LIST);
    var headFeat = nonEmptyFsListType.getFeatureByBaseName("head");
    var tailFeat = nonEmptyFsListType.getFeatureByBaseName("tail");

    var emptyNode = cas.createFS(emptyFsListType);

    var secondNode = cas.createFS(nonEmptyFsListType);
    secondNode.setFeatureValue(headFeat, orgAnnot2);
    secondNode.setFeatureValue(tailFeat, emptyNode);

    var firstNode = cas.createFS(nonEmptyFsListType);
    firstNode.setFeatureValue(headFeat, orgAnnot1);
    firstNode.setFeatureValue(tailFeat, secondNode);

    var listFeat = testAnnotType.getFeatureByBaseName("listFeat");
    testAnnot.setFeatureValue(listFeat, firstNode);

    // serialize to XMI
    var xmiStr = serialize(cas, null);
    // System.out.println(xmiStr);

    // deserialize into a CAS that's missing the Organization type
    var partialTypeSystemFile = JUnitExtension.getFile("ExampleCas/partialTestTypeSystem.xml");
    var partialTypeSystem = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(partialTypeSystemFile));
    testAnnotTypeDesc = partialTypeSystem.addType("org.apache.uima.testTypeSystem.TestAnnotation",
            "", "uima.tcas.Annotation");
    testAnnotTypeDesc.addFeature("listFeat", "", "uima.cas.FSList");
    var partialTsCas = CasCreationUtils.createCas(partialTypeSystem, null, null);
    var sharedData = new XmiSerializationSharedData();
    deserialize(xmiStr, partialTsCas, sharedData, true, -1);

    // check out of type system data
    var testAnnotType2 = partialTsCas.getTypeSystem()
            .getType("org.apache.uima.testTypeSystem.TestAnnotation");
    FeatureStructure testAnnot2 = partialTsCas.getAnnotationIndex(testAnnotType2).iterator().get();
    var listFeat2 = testAnnotType2.getFeatureByBaseName("listFeat");
    var listFs = testAnnot2.getFeatureValue(listFeat2);
    var ootsElems = sharedData.getOutOfTypeSystemElements();
    assertThat(ootsElems).hasSize(2);

    var oed = sharedData.getOutOfTypeSystemFeatures((TOP) listFs);
    var attr = oed.attributes.get(0);
    assertThat(attr).isNotNull();
    assertThat(attr.name).isEqualTo(CAS.FEATURE_BASE_NAME_HEAD);
    assertThat(((OotsElementData) ootsElems.get(0)).xmiId).isEqualTo(attr.value);

    // re-serialize along with out of type system data
    var xmiStr2 = serialize(partialTsCas, sharedData);
    // System.out.println(xmiStr2);

    // deserialize into a new CAS and compare
    var cas2 = CasCreationUtils.createCas(typeSystem, null, null);
    deserialize(xmiStr2, cas2, null, false, -1);

    CasComparer.assertEquals(cas, cas2);
  }

  @Test
  void testOutOfTypeSystemDataComplexCas() throws Exception {
    // deserialize a complex XCAS
    var originalCas = CasCreationUtils.createCas(typeSystem, null, indexes);
    InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
    XCASDeserializer.deserialize(serCasStream, originalCas);
    serCasStream.close();

    // serialize to XMI
    var xmiStr = serialize(originalCas, null);

    // deserialize into a CAS with no type system
    var casWithNoTs = CasCreationUtils.createCas(new TypeSystemDescription_impl(),
            new TypePriorities_impl(), FsIndexDescription.EMPTY_FS_INDEX_DESCRIPTIONS);
    var sharedData = new XmiSerializationSharedData();
    deserialize(xmiStr, casWithNoTs, sharedData, true, -1);

    // now reserialize including OutOfTypeSystem data
    var xmiStr2 = serialize(casWithNoTs, sharedData);

    // deserialize into a new CAS that has the full type system
    var newCas = CasCreationUtils.createCas(typeSystem, null, indexes);
    deserialize(xmiStr2, newCas, null, false, -1);

    // compare
    CasComparer.assertEquals(originalCas, newCas);

    // Test a partial type system with a missing some missing features and
    // missing "Organization" type
    var partialTypeSystemFile = JUnitExtension.getFile("ExampleCas/partialTestTypeSystem.xml");
    var partialTypeSystem = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(partialTypeSystemFile));
    var partialTsCas = CasCreationUtils.createCas(partialTypeSystem, null, indexes);
    var sharedData2 = new XmiSerializationSharedData();
    deserialize(xmiStr, partialTsCas, sharedData2, true, -1);

    var xmiStr3 = serialize(partialTsCas, sharedData2);
    newCas.reset();
    deserialize(xmiStr3, newCas, null, false, -1);
    CasComparer.assertEquals(originalCas, newCas);
  }

  // public void testdebugnc() throws Exception {
  // CAS cas = CasCreationUtils.createCas(typeSystem, null, indexes);
  // InputStream serCasStream = new
  // FileInputStream(JUnitExtension.getFile("ExampleCas/simpleCas.xmi"));
  // XmiCasDeserializer.deserialize(serCasStream, cas);
  // serCasStream.close();
  //
  // XmiSerializationSharedData sharedData = new XmiSerializationSharedData();
  // String r = serialize(cas, sharedData);
  //
  // { File f = new File("C:/au/wksp431/apache/json-work/tempdebug.xml");
  // PrintWriter pw = new PrintWriter(f);
  // pw.println(r);
  // pw.close();
  // }
  //
  // }
  // public void testGetNumChildren() throws Exception {
  // // deserialize a complex XCAS
  // CAS cas = CasCreationUtils.createCas(typeSystem, null, indexes);
  //// InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
  //// XCASDeserializer.deserialize(serCasStream, cas);
  //// serCasStream.close();
  // InputStream serCasStream = new
  // FileInputStream(JUnitExtension.getFile("ExampleCas/simpleCas.xmi"));
  // XmiCasDeserializer.deserialize(serCasStream, cas);
  // serCasStream.close();
  //
  // // call serializer with a ContentHandler that checks numChildren
  // XmiCasSerializer xmiSer = new XmiCasSerializer(cas.getTypeSystem());
  // GetNumChildrenTestHandler handler = new GetNumChildrenTestHandler(xmiSer, (CASImpl)cas);
  // xmiSer.serialize(cas, handler, handler.tcds);
  // }

  /**
   * Utility method for serializing a CAS to an XMI String
   */
  private static String serialize(CAS cas, XmiSerializationSharedData serSharedData)
          throws IOException, SAXException {
    var baos = new ByteArrayOutputStream();
    XmiCasSerializer.serialize(cas, null, baos, false, serSharedData);
    baos.close();
    var xmiStr = new String(baos.toByteArray(), StandardCharsets.UTF_8); // note by default
                                                                         // XmiCasSerializer
                                                                         // generates UTF-8

    // workaround for newline serialization problem in Sun Java 1.4.2
    // this test file should contain CRLF line endings, but Sun Java loses them
    // when it serializes XML.
    if (!builtInXmlSerializationSupportsCRs()) {
      xmiStr = xmiStr.replaceAll("&#10;", "&#13;&#10;");
    }
    return xmiStr;
  }

  /**
   * Utility method for serializing a Delta CAS to XMI String
   */
  private static String serialize(CAS cas, XmiSerializationSharedData serSharedData, Marker marker)
          throws IOException, SAXException {
    var baos = new ByteArrayOutputStream();
    XmiCasSerializer.serialize(cas, null, baos, false, serSharedData, marker);
    baos.close();
    var xmiStr = new String(baos.toByteArray(), StandardCharsets.UTF_8); // note by default
                                                                         // XmiCasSerializer
                                                                         // generates UTF-8

    // workaround for newline serialization problem in Sun Java 1.4.2
    // this test file should contain CRLF line endings, but Sun Java loses them
    // when it serializes XML.
    if (!builtInXmlSerializationSupportsCRs()) {
      xmiStr = xmiStr.replaceAll("&#10;", "&#13;&#10;");
    }
    return xmiStr;
  }

  /** Utility method for deserializing a CAS from an XMI String */
  private void deserialize(String xmlStr, CAS cas, XmiSerializationSharedData sharedData,
          boolean lenient, int mergePoint) throws FactoryConfigurationError,
          ParserConfigurationException, SAXException, IOException {
    var bytes = xmlStr.getBytes(StandardCharsets.UTF_8); // this assumes the encoding is UTF-8,
                                                         // which is the default output encoding
                                                         // of the XmiCasSerializer
    var bais = new ByteArrayInputStream(bytes);
    XmiCasDeserializer.deserialize(bais, cas, lenient, sharedData, mergePoint);
    bais.close();
  }

  private void deserialize(String xmlStr, CAS cas, XmiSerializationSharedData sharedData,
          boolean lenient, int mergePoint, AllowPreexistingFS allow)
          throws FactoryConfigurationError, ParserConfigurationException, SAXException,
          IOException {
    var bytes = xmlStr.getBytes(StandardCharsets.UTF_8); // this assumes the encoding is UTF-8,
                                                         // which is the default output encoding
                                                         // of the XmiCasSerializer
    var bais = new ByteArrayInputStream(bytes);
    XmiCasDeserializer.deserialize(bais, cas, lenient, sharedData, mergePoint, allow);
    bais.close();
  }

  private AnnotationFS createPersonAnnot(CAS cas, int begin, int end) {
    var personType = cas.getTypeSystem().getType("org.apache.uima.testTypeSystem.Person");
    var person = cas.createAnnotation(personType, begin, end);
    cas.addFsToIndexes(person);
    return person;
  }

  /**
   * Checks the Java vendor and version and returns true if running a version of Java whose built-in
   * XSLT support can properly serialize carriage return characters, and false if not. It seems to
   * be the case that Sun JVMs prior to 1.5 do not properly serialize carriage return characters. We
   * have to modify our test case to account for this.
   * 
   * @return true if XML serialization of CRs behave properly in the current JRE
   */
  private static boolean builtInXmlSerializationSupportsCRs() {
    var javaVendor = System.getProperty("java.vendor");
    if (javaVendor.startsWith("Sun")) {
      var javaVersion = System.getProperty("java.version");
      if (javaVersion.startsWith("1.3") || javaVersion.startsWith("1.4")) {
        return false;
      }
    }
    return true;
  }

  /*
   * for debug
   */
  private static void dumpStr2File(String s, String namepart) throws FileNotFoundException {

    var f = new File("C:/au/wksp431/apache/json-work/temp" + namepart + ".xml");
    var pw = new PrintWriter(f);
    pw.println(s);
    pw.close();

  }
  // /**
  // * Extends the SAX Normal default handler used to output xml
  // * Keeps a stack of counts:
  // * For every startElement call, pushes the number of children of that element onto the stack
  // * For every endElement call, test if at number has been decremented to 0
  // * Inner endElement calls decrement the count (of their parentt,
  // * Inner "text" elements also do this (for their parent)
  // */
  // static class GetNumChildrenTestHandler extends DefaultHandler {
  // XmiCasSerializer xmiSer;
  // Stack<Integer> childCountStack = new Stack<Integer>();
  // private XmiDocSerializer tcds;
  //
  // GetNumChildrenTestHandler(XmiCasSerializer xmiSer, CASImpl cas) {
  // this.xmiSer = xmiSer;
  // tcds = xmiSer.getTestXmiDocSerializer(this, cas);
  // childCountStack.push(Integer.valueOf(1));
  // }
  //
  // /* (non-Javadoc)
  // * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String,
  // java.lang.String, org.xml.sax.Attributes)
  // */
  // public void startElement(String uri, String localName, String qName, Attributes attributes)
  // throws SAXException {
  // // TODO Auto-generated method stub
  // super.startElement(uri, localName, qName, attributes);
  // childCountStack.push(Integer.valueOf(tcds.getNumChildren()));
  // System.out.format("Debug: NumberOfChildren: Starting element %s, setting count to %d%n", qName,
  // tcds.getNumChildren());
  // }
  //
  // /* (non-Javadoc)
  // * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String,
  // java.lang.String)
  // */
  // public void endElement(String uri, String localName, String qName) throws SAXException {
  // // TODO Auto-generated method stub
  // super.endElement(uri, localName, qName);
  // //check that we've seen the expected number of child elements
  // //(count on top of stack should be 0)
  // Integer count = (Integer)childCountStack.pop();
  // assertEquals(0, count.intValue());
  //
  // //decremenet child count of our parent
  // count = (Integer)childCountStack.pop();
  // childCountStack.push(Integer.valueOf(count.intValue() - 1));
  // System.out.format("Debug: NumberOfChildren: ending element %s, decr parent count to %d%n",
  // qName, count - 1);
  //
  // }
  //
  // /* (non-Javadoc)
  // * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
  // */
  // public void characters(char[] ch, int start, int length) throws SAXException {
  // // text node is considered a child
  // if (length > 0) {
  // Integer count = (Integer)childCountStack.pop();
  // childCountStack.push(Integer.valueOf(count.intValue() - 1));
  // System.out.format("Debug: NumberOfChildren: ending text element, decr parent count to %d%n",
  // count - 1);
  // } else {
  // System.out.format("Debug: NumberOfChildren: ending empty text element, not decr count%n");
  // }
  // }
  //
  //
  // }
}
