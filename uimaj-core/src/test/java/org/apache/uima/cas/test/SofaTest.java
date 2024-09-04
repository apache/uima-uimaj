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

package org.apache.uima.cas.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.impl.SofaID_impl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

class SofaTest {

  private CASMgr casMgr;

  private CAS cas;

  private Type annotationType;

  private Type docAnnotationType;

  private Type crossType;

  private Feature otherFeat;

  // private Feature annotSofaFeat;
  @BeforeEach
  void setUp() throws Exception {
    try {
      casMgr = CASFactory.createCAS();
      CasCreationUtils.setupTypeSystem(casMgr, (TypeSystemDescription) null);
      // Create a writable type system.
      TypeSystemMgr tsa = casMgr.getTypeSystemMgr();
      // Add new types and features.
      // Type topType = tsa.getTopType();
      annotationType = tsa.getType(CAS.TYPE_NAME_ANNOTATION);
      // annotSofaFeat = annotationType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_SOFA);
      docAnnotationType = tsa.getType(CAS.TYPE_NAME_DOCUMENT_ANNOTATION);
      assertThat(annotationType).isNotNull();
      crossType = tsa.addType("sofa.test.CrossAnnotation", annotationType);
      otherFeat = tsa.addFeature("otherAnnotation", crossType, annotationType);
      // Commit the type system.
      ((CASImpl) casMgr).commitTypeSystem();

      // reinit type system values because the commit might reuse an existing one
      tsa = casMgr.getTypeSystemMgr();
      annotationType = tsa.getType(CAS.TYPE_NAME_ANNOTATION);
      docAnnotationType = tsa.getType(CAS.TYPE_NAME_DOCUMENT_ANNOTATION);
      crossType = tsa.getType("sofa.test.CrossAnnotation");
      otherFeat = crossType.getFeatureByBaseName("otherAnnotation");

      // Create the Base indexes.
      casMgr.initCASIndexes();
      FSIndexRepositoryMgr irm = casMgr.getIndexRepositoryMgr();
      // init.initIndexes(irm, casMgr.getTypeSystemMgr());
      irm.commit();

      cas = casMgr.getCAS().getView(CAS.NAME_DEFAULT_SOFA);
      assertThat(cas.getSofa()).isNull();
      assertThat(cas.getViewName()).isEqualTo(CAS.NAME_DEFAULT_SOFA);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @AfterEach
  public void tearDown() {
    casMgr = null;
    cas = null;
    annotationType = null;
    docAnnotationType = null;
    crossType = null;
    otherFeat = null;
  }

  /**
   * Test driver.
   */
  @Test
  void testMain() throws Exception {
    try {

      // Create a Sofa (using old APIs for now)
      SofaID_impl id = new SofaID_impl();
      id.setSofaID("EnglishDocument");
      SofaFS es = cas.createSofa(id, "text");
      // Initial View is #1!!!
      assertThat(es.getSofaRef()).isEqualTo(2);

      // Set the document text
      es.setLocalSofaData("this beer is good");

      // Test Multiple Sofas across XCAS serialization
      String xcasFilename = "Sofa.xcas";
      XCASSerializer ser = new XCASSerializer(cas.getTypeSystem());
      OutputStream outputXCAS = new FileOutputStream(xcasFilename);
      XMLSerializer xmlSer = new XMLSerializer(outputXCAS);
      try {
        ser.serialize(cas, xmlSer.getContentHandler());
        outputXCAS.close();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (SAXException e) {
        e.printStackTrace();
      }

      // Deserialize XCAS
      cas.reset();
      InputStream inputXCAS = new FileInputStream(xcasFilename);
      try {
        XCASDeserializer.deserialize(inputXCAS, cas, false);
        inputXCAS.close();
      } catch (SAXException e2) {
        e2.printStackTrace();
      } catch (IOException e2) {
        e2.printStackTrace();
      }
      // Delete the generated file.
      File xcasFile = new File(xcasFilename);
      if (xcasFile.exists()) {
        assertThat(xcasFile.delete()).isTrue();
      }

      // Add a new Sofa
      // SofaID_impl gid = new SofaID_impl();
      // gid.setSofaID("GermanDocument");
      // SofaFS gs = ((CASImpl)cas).createSofa(gid,"text");
      CAS gerTcas = cas.createView("GermanDocument");
      assertThat(gerTcas.getViewName()).isEqualTo("GermanDocument");
      SofaFS gs = gerTcas.getSofa();

      assertThat(gs).isNotNull();
      assertThat(gs.getSofaRef()).isEqualTo(3);

      // Set the document text
      // gs.setLocalSofaData("das bier ist gut");
      gerTcas.setDocumentText("das bier ist gut");

      // Test multiple Sofas across binary serialization
      CASSerializer cs = Serialization.serializeNoMetaData(cas);
      cas = Serialization.createCAS(casMgr, cs);

      // Add a new Sofa
      // SofaID_impl fid = new SofaID_impl();
      // fid.setSofaID("FrenchDocument");
      // SofaFS fs = ((CASImpl)cas).createSofa(fid, "text");
      CAS frT = cas.createView("FrenchDocument");
      assertThat(frT.getViewName()).isEqualTo("FrenchDocument");
      SofaFS fs = frT.getSofa();
      assertThat(fs).isNotNull();
      assertThat(fs.getSofaRef()).isEqualTo(4);

      // Test multiple Sofas across blob serialization
      ByteArrayOutputStream fos = new ByteArrayOutputStream();
      Serialization.serializeCAS(cas, fos);
      cas.reset();
      ByteArrayInputStream fis = new ByteArrayInputStream(fos.toByteArray());
      Serialization.deserializeCAS(cas, fis);

      // Open TCas views of some Sofas
      CAS engTcas = cas.getView(es);
      assertThat(engTcas.getViewName()).isEqualTo("EnglishDocument");
      CAS frTcas = cas.getView("FrenchDocument");

      // Set the document text off SofaFS after the CAS view exists
      frTcas.setSofaDataString("cette biere est bonne", "text");

      // Create standard annotations against one and cross annotations
      // against the other
      int engEnd = 0;
      int gerEnd = 0;
      int frEnd = 0;
      String engText = engTcas.getDocumentText();
      String gerText = gerTcas.getDocumentText();
      String frText = frTcas.getDocumentText();
      StringTokenizer est = new StringTokenizer(engText);
      StringTokenizer gst = new StringTokenizer(gerText);
      StringTokenizer fst = new StringTokenizer(frText);

      while (est.hasMoreTokens()) {
        assertThat(gst.hasMoreTokens()).isTrue();
        assertThat(fst.hasMoreTokens()).isTrue();

        String eTok = est.nextToken();
        int engBegin = engText.indexOf(eTok, engEnd);
        engEnd = engBegin + eTok.length();

        String gTok = gst.nextToken();
        int gerBegin = gerText.indexOf(gTok, gerEnd);
        gerEnd = gerBegin + gTok.length();

        String fTok = fst.nextToken();
        int frBegin = frText.indexOf(fTok, frEnd);
        frEnd = frBegin + fTok.length();

        AnnotationFS engAnnot = engTcas.createAnnotation(annotationType, engBegin, engEnd);
        engTcas.getIndexRepository().addFS(engAnnot);

        // should throw an error, because you can't add to index a FS which is a subtype of
        // AnnotationBase, whose
        // whose sofa ref is to a different View
        try {
          frTcas.getIndexRepository().addFS(engAnnot);
        } catch (Exception e) {
          assertThat(e instanceof CASRuntimeException).isTrue();
          CASRuntimeException c = (CASRuntimeException) e;
          assertThat(c.getMessageKey()).isEqualTo("ANNOTATION_IN_WRONG_INDEX");
        }

        AnnotationFS frAnnot = frTcas.createAnnotation(annotationType, frBegin, frEnd);
        frTcas.getIndexRepository().addFS(frAnnot);

        AnnotationFS gerAnnot = gerTcas.createAnnotation(crossType, gerBegin, gerEnd);
        gerAnnot.setFeatureValue(otherFeat, engAnnot);
        gerTcas.getIndexRepository().addFS(gerAnnot);
      }

      // Test that the annotations are in separate index spaces, and that
      // Sofas are indexed
      // FSIndex sofaIndex =
      // cas.getIndexRepository().getIndex(CAS.SOFA_INDEX_NAME);
      FSIterator<SofaFS> sofaIter = cas.getSofaIterator();
      int numSofas = 0;
      while (sofaIter.isValid()) {
        numSofas++;
        sofaIter.moveToNext();
      }
      FSIndex<AnnotationFS> engIndex = engTcas.getAnnotationIndex();
      FSIndex<AnnotationFS> gerIndex = gerTcas.getAnnotationIndex();
      FSIndex<AnnotationFS> frIndex = frTcas.getAnnotationIndex();
      // assertTrue(sofaIndex.size() == 3); // 3 sofas
      assertThat(numSofas).isEqualTo(3);
      assertThat(engIndex.size()).isEqualTo(5); // 4 annots plus
      // documentAnnotation
      assertThat(gerIndex.size()).isEqualTo(5); // 4 annots plus
      // documentAnnotation
      assertThat(frIndex.size()).isEqualTo(5); // 4 annots plus
      // documentAnnotation

      // Test that the annotations are of the correct types
      FSIterator<AnnotationFS> engIt = engIndex.iterator();
      FSIterator<AnnotationFS> gerIt = gerIndex.iterator();
      FSIterator<AnnotationFS> frIt = frIndex.iterator();
      AnnotationFS engAnnot = engIt.get();
      AnnotationFS gerAnnot = gerIt.get();
      AnnotationFS frAnnot = frIt.get();
      assertThat(engAnnot.getType().getName()).isEqualTo(docAnnotationType.getName());
      assertThat(gerAnnot.getType().getName()).isEqualTo(docAnnotationType.getName());
      assertThat(frAnnot.getType().getName()).isEqualTo(docAnnotationType.getName());

      engIt.moveToNext();
      gerIt.moveToNext();
      frIt.moveToNext();
      engAnnot = engIt.get();
      gerAnnot = gerIt.get();
      frAnnot = frIt.get();
      assertThat(engAnnot.getType().getName()).isEqualTo(annotationType.getName());
      assertThat(engAnnot.getCoveredText()).isEqualTo(("this"));
      assertThat(frAnnot.getType().getName()).isEqualTo(annotationType.getName());
      assertThat(frAnnot.getCoveredText()).isEqualTo(("cette"));
      assertThat(gerAnnot.getType().getName()).isEqualTo(crossType.getName());
      assertThat(gerAnnot.getCoveredText()).isEqualTo(("das"));

      // Test that the other annotation feature of cross annotations works
      AnnotationFS crossAnnot = (AnnotationFS) gerAnnot.getFeatureValue(otherFeat);
      assertThat(crossAnnot.getType().getName()).isEqualTo(annotationType.getName());
      assertThat(crossAnnot.getCoveredText()).isEqualTo(("this"));

      // Test equals method for same annotation obtained through different views
      assertThat(crossAnnot).isEqualTo(engAnnot);

      // Test that annotations accessed from a reference in the base CAS
      // work correctly
      ArrayFS anArray = cas.createArrayFS(3);
      anArray.set(0, engAnnot);
      anArray.set(1, frAnnot);
      anArray.set(2, gerAnnot);
      AnnotationFS tstAnnot = (AnnotationFS) anArray.get(0);
      assertThat(tstAnnot.getCoveredText()).isEqualTo(("this"));
      tstAnnot = (AnnotationFS) anArray.get(1);
      assertThat(tstAnnot.getCoveredText()).isEqualTo(("cette"));
      tstAnnot = (AnnotationFS) anArray.get(2);
      assertThat(tstAnnot.getCoveredText()).isEqualTo(("das"));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }

  }

  /*
   * Test stream access to Sofa Data.
   */
  @Test
  void testSofaDataStream() throws Exception {
    try {

      // Create Sofas
      // Create a local Sofa and set string feature
      // SofaID_impl id = new SofaID_impl();
      // id.setSofaID("StringSofaData");
      // SofaFS strSofa = cas.createSofa(id, "text");
      // strSofa.setLocalSofaData("this beer is good");
      CAS stringView = cas.createView("StringSofaData");
      stringView.setDocumentText("this beer is good");

      // create a int array fs
      IntArrayFS intArrayFS = cas.createIntArrayFS(5);
      intArrayFS.set(0, 1);
      intArrayFS.set(1, 2);
      intArrayFS.set(2, 3);
      intArrayFS.set(3, 4);
      intArrayFS.set(4, 5);
      // create a Sofa and set the SofaArray feature to an int array FS.
      // id = new SofaID_impl();
      // id.setSofaID("intArraySofaData");
      // SofaFS intarraySofaFS = cas.createSofa(id, "text");
      // intarraySofaFS.setLocalSofaData(intArrayFS);
      CAS intArrayView = cas.createView("intArraySofaData");
      intArrayView.setSofaDataArray(intArrayFS, "integers");

      // create a string array fs
      StringArrayFS stringArrayFS = cas.createStringArrayFS(5);
      stringArrayFS.set(0, "This");
      stringArrayFS.set(1, "beer");
      stringArrayFS.set(2, "is");
      stringArrayFS.set(3, "really");
      stringArrayFS.set(4, "good");
      CAS stringArrayView = cas.createView("stringArraySofaData");
      stringArrayView.setSofaDataArray(stringArrayFS, "strings");

      // create a float array fs
      FloatArrayFS floatArrayFS = cas.createFloatArrayFS(5);
      floatArrayFS.set(0, (float) 0.1);
      floatArrayFS.set(1, (float) 0.2);
      floatArrayFS.set(2, (float) 0.3);
      floatArrayFS.set(3, (float) 0.4);
      floatArrayFS.set(4, (float) 0.5);
      // create a sofa and set the SofaArray feature to the float array
      // id = new SofaID_impl();
      // id.setSofaID("floatArraySofaData");
      // SofaFS floatarraySofaFS = cas.createSofa(id,"text");
      // floatarraySofaFS.setLocalSofaData(floatArrayFS);
      CAS floatArrayView = cas.createView("floatArraySofaData");
      floatArrayView.setSofaDataArray(floatArrayFS, "floats");

      // create a short array fs
      ShortArrayFS shortArrayFS = cas.createShortArrayFS(5);
      shortArrayFS.set(0, (short) 128);
      shortArrayFS.set(1, (short) 127);
      shortArrayFS.set(2, (short) 126);
      shortArrayFS.set(3, (short) 125);
      shortArrayFS.set(4, (short) 124);
      // create a Sofa and set the SofaArray feature to an int array FS.
      // id = new SofaID_impl();
      // id.setSofaID("shortArraySofaData");
      // SofaFS shortarraySofaFS = cas.createSofa(id, "text");
      // shortarraySofaFS.setLocalSofaData(shortArrayFS);
      CAS shortArrayView = cas.createView("shortArraySofaData");
      shortArrayView.setSofaDataArray(shortArrayFS, "shorts");

      // create a byte array fs
      ByteArrayFS byteArrayFS = cas.createByteArrayFS(5);
      byteArrayFS.set(0, (byte) 8);
      byteArrayFS.set(1, (byte) 16);
      byteArrayFS.set(2, (byte) 64);
      byteArrayFS.set(3, (byte) 128);
      byteArrayFS.set(4, (byte) 255);
      // create a Sofa and set the SofaArray feature.
      // id = new SofaID_impl();
      // id.setSofaID("byteArraySofaData");
      // SofaFS bytearraySofaFS = cas.createSofa(id, "text");
      // bytearraySofaFS.setLocalSofaData(byteArrayFS);
      CAS byteArrayView = cas.createView("byteArraySofaData");
      byteArrayView.setSofaDataArray(byteArrayFS, "bytes");

      // create a long array fs
      LongArrayFS longArrayFS = cas.createLongArrayFS(5);
      longArrayFS.set(0, Long.MAX_VALUE);
      longArrayFS.set(1, Long.MAX_VALUE - 1);
      longArrayFS.set(2, Long.MAX_VALUE - 2);
      longArrayFS.set(3, Long.MAX_VALUE - 3);
      longArrayFS.set(4, Long.MAX_VALUE - 4);
      // create a Sofa and set the SofaArray feature.
      // id = new SofaID_impl();
      // id.setSofaID("longArraySofaData");
      // SofaFS longarraySofaFS = cas.createSofa(id, "text");
      // longarraySofaFS.setLocalSofaData(longArrayFS);
      CAS longArrayView = cas.createView("longArraySofaData");
      longArrayView.setSofaDataArray(longArrayFS, "longs");

      DoubleArrayFS doubleArrayFS = cas.createDoubleArrayFS(5);
      doubleArrayFS.set(0, Double.MAX_VALUE);
      doubleArrayFS.set(1, Double.MIN_VALUE);
      doubleArrayFS.set(2, Double.parseDouble("1.5555"));
      doubleArrayFS.set(3, Double.parseDouble("99.000000005"));
      doubleArrayFS.set(4, Double.parseDouble("4.44444444444444444"));
      // create a Sofa and set the SofaArray feature.
      // id = new SofaID_impl();
      // id.setSofaID("doubleArraySofaData");
      // SofaFS doublearraySofaFS = cas.createSofa(id, "text");
      // doublearraySofaFS.setLocalSofaData(doubleArrayFS);
      CAS doubleArrayView = cas.createView("doubleArraySofaData");
      doubleArrayView.setSofaDataArray(doubleArrayFS, "doubles");

      // create remote sofa and set the SofaURI feature
      // id = new SofaID_impl();
      // id.setSofaID("remoteSofaData");
      // SofaFS remoteSofa = cas.createSofa(id, "text");
      // remoteSofa.setRemoteSofaURI("file:.\\Sofa.xcas");
      CAS remoteView = cas.createView("remoteSofaData");
      String sofaFileName = "./Sofa.xcas";
      remoteView.setSofaDataURI("file:" + sofaFileName, "text");
      PrintWriter out = new PrintWriter(sofaFileName);
      out.print("this beer is good");
      out.close();

      // read sofa data
      // InputStream is = strSofa.getSofaDataStream();
      InputStream is = stringView.getSofaDataStream();
      assertThat(is).isNotNull();
      byte[] dest = new byte[1];
      StringBuffer buf = new StringBuffer();
      while (is.read(dest) != -1) {
        buf.append((char) dest[0]);
      }
      assertThat(buf.toString()).isEqualTo("this beer is good");

      dest = new byte[4];
      // is = intarraySofaFS.getSofaDataStream();
      is.close();
      is = intArrayView.getSofaDataStream();
      assertThat(is).isNotNull();
      int i = 0;
      while (is.read(dest) != -1) {
        assertThat(intArrayFS.get(i++)).isEqualTo(ByteBuffer.wrap(dest).getInt());
      }

      is.close();
      is = stringArrayView.getSofaDataStream();
      assertThat(is).isNotNull();
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      i = 0;
      while (br.ready()) {
        assertThat(br.readLine()).isEqualTo(stringArrayFS.get(i++));
      }

      // is = floatarraySofaFS.getSofaDataStream();
      is.close();
      is = floatArrayView.getSofaDataStream();
      assertThat(is).isNotNull();
      i = 0;
      while (is.read(dest) != -1) {
        assertThat(floatArrayFS.get(i++)).isCloseTo(ByteBuffer.wrap(dest).getFloat(), offset(0.0f));
      }

      dest = new byte[2];
      // is = shortarraySofaFS.getSofaDataStream();
      is.close();
      is = shortArrayView.getSofaDataStream();
      assertThat(is).isNotNull();
      i = 0;
      while (is.read(dest) != -1) {
        assertThat(shortArrayFS.get(i++)).isEqualTo(ByteBuffer.wrap(dest).getShort());
      }

      dest = new byte[1];
      // is = bytearraySofaFS.getSofaDataStream();
      is.close();
      is = byteArrayView.getSofaDataStream();
      assertThat(is).isNotNull();
      i = 0;
      while (is.read(dest) != -1) {
        assertThat(byteArrayFS.get(i++)).isEqualTo(ByteBuffer.wrap(dest).get());
      }

      dest = new byte[8];
      // is = longarraySofaFS.getSofaDataStream();
      is.close();
      is = longArrayView.getSofaDataStream();
      assertThat(is).isNotNull();
      i = 0;
      while (is.read(dest) != -1) {
        assertThat(longArrayFS.get(i++)).isEqualTo(ByteBuffer.wrap(dest).getLong());
      }

      // is = doublearraySofaFS.getSofaDataStream();
      is.close();
      is = doubleArrayView.getSofaDataStream();
      assertThat(is).isNotNull();
      i = 0;
      while (is.read(dest) != -1) {
        assertThat(doubleArrayFS.get(i++)).isCloseTo(ByteBuffer.wrap(dest).getDouble(),
                offset(0.0));
      }

      dest = new byte[1];
      is.close();
      // is = remoteSofa.getSofaDataStream();
      is = remoteView.getSofaDataStream();
      assertThat(is).isNotNull();
      buf = new StringBuffer();
      while (is.read(dest) != -1) {
        buf.append((char) dest[0]);
      }
      assertThat(buf.toString()).isEqualTo("this beer is good");
      is.close();

      // Delete the generated file.
      File xcasFile = new File(sofaFileName);
      if (xcasFile.exists()) {
        assertThat(xcasFile.delete()).isTrue();
      }

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  void testSetSofaDataString() {
    final String TEST_TEXT = "this is a test";
    final String TEST_MIME = "text/plain";
    CAS testView = cas.createView("TestView");
    testView.setSofaDataString(TEST_TEXT, TEST_MIME);
    assertThat(testView.getSofa().getLocalStringData()).isEqualTo(TEST_TEXT);
    assertThat(testView.getSofa().getSofaMime()).isEqualTo(TEST_MIME);
    assertThat(testView.getSofaDataString()).isEqualTo(TEST_TEXT);
  }

  @Test
  void testSetSofaDataStringOnInitialView() {
    final String TEST_TEXT = "this is a test";
    final String TEST_MIME = "text/plain";
    cas.setSofaDataString(TEST_TEXT, TEST_MIME);
    assertThat(cas.getSofa().getLocalStringData()).isEqualTo(TEST_TEXT);
    assertThat(cas.getSofa().getSofaMime()).isEqualTo(TEST_MIME);
    assertThat(cas.getSofaDataString()).isEqualTo(TEST_TEXT);
  }

  @Test
  void testSetSofaDataURI() {
    final String TEST_URI = "file:/test";
    final String TEST_MIME = "text/plain";
    CAS testView = cas.createView("TestView");
    testView.setSofaDataURI(TEST_URI, TEST_MIME);
    assertThat(testView.getSofa().getSofaURI()).isEqualTo(TEST_URI);
    assertThat(testView.getSofa().getSofaMime()).isEqualTo(TEST_MIME);
  }

  @Test
  void testSetSofaDataURIonInitialView() throws Exception {
    // This test uses platform encoding both for reading and writing.
    String someText = "remote text.";
    String someTextFile = "./someUriText.txt";
    FileWriter output = new FileWriter(someTextFile);
    output.write(someText);
    output.close();

    final String TEST_URI = "file:" + someTextFile;
    final String TEST_MIME = "text/plain";
    cas.setSofaDataURI(TEST_URI, TEST_MIME);
    assertThat(cas.getSofa().getSofaURI()).isEqualTo(TEST_URI);
    assertThat(cas.getSofa().getSofaMime()).isEqualTo(TEST_MIME);

    InputStream is = cas.getSofaDataStream();
    assertThat(is).isNotNull();

    // This obviously can't work on all platforms
    // byte[] dest = new byte[1];
    // StringBuffer buf = new StringBuffer();
    // while (is.read(dest) != -1) {
    // buf.append((char) dest[0]);
    // }
    // is.close();
    // assertTrue(buf.toString().equals(someText));

    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String textFromFile = reader.readLine();
    is.close();
    assertThat(someText).isEqualTo(textFromFile);
    File testFile = new File(someTextFile);
    assertThat(testFile.delete()).isTrue();
  }

  @Test
  void testSetSofaDataArray() {
    final String TEST_MIME = "text/plain";
    CAS testView = cas.createView("TestView");
    ByteArrayFS sofaDataArray = testView.createByteArrayFS(2);
    sofaDataArray.set(0, (byte) 0);
    sofaDataArray.set(1, (byte) 42);
    testView.setSofaDataArray(sofaDataArray, TEST_MIME);
    assertThat(testView.getSofa().getLocalFSData()).isEqualTo(sofaDataArray);
    assertThat(testView.getSofa().getSofaMime()).isEqualTo(TEST_MIME);
  }

  @Test
  void testSetSofaDataArrayOnInitialView() {
    final String TEST_MIME = "text/plain";
    ByteArrayFS sofaDataArray = cas.createByteArrayFS(2);
    sofaDataArray.set(0, (byte) 0);
    sofaDataArray.set(1, (byte) 42);
    cas.setSofaDataArray(sofaDataArray, TEST_MIME);
    assertThat(cas.getSofa().getLocalFSData()).isEqualTo(sofaDataArray);
    assertThat(cas.getSofa().getSofaMime()).isEqualTo(TEST_MIME);
  }

  @Test
  void testReset() {
    cas.reset();
    cas.setDocumentText("setDocumentText creates the _InitialView Sofa");
    CAS testView = cas.createView("TestView");
    testView.setDocumentText("create a 2nd Sofa");
    assertThat(cas.getViewName()).isEqualTo("_InitialView");
    assertThat(testView.getViewName()).isEqualTo("TestView");

    cas.reset();
    SofaID_impl id = new SofaID_impl();
    id.setSofaID("TestView");
    SofaFS testSofa = cas.createSofa(id, "text");
    CAS newView = cas.getView(testSofa);
    assertThat(newView.getViewName()).isEqualTo("TestView");
  }

  private void checkViewsExist(Iterator it, AbstractCas... cas_s) {
    List<AbstractCas> casList = Arrays.asList(cas_s);
    int i = 0;
    while (it.hasNext()) {
      assertThat(casList.contains(it.next())).isTrue();
      i++;
    }
    assertThat(cas_s.length).isEqualTo(i);
  }

  @Test
  void testGetViewIterator() throws Exception {
    cas.reset();
    CAS view1 = cas.createView("View1");
    CAS view2 = cas.createView("View2");
    checkViewsExist(cas.getViewIterator(), cas, view1, view2);

    CAS viewE1 = cas.createView("EnglishDocument");
    CAS viewE2 = cas.createView("EnglishDocument.2");
    checkViewsExist(cas.getViewIterator("EnglishDocument"), viewE1, viewE2);

    // try with Sofa mappings
    UimaContextAdmin rootCtxt = UIMAFramework.newUimaContext(UIMAFramework.getLogger(),
            UIMAFramework.newDefaultResourceManager(), UIMAFramework.newConfigurationManager());
    Map<String, String> sofamap = new HashMap<>();
    sofamap.put("SourceDocument", "EnglishDocument");
    UimaContextAdmin childCtxt = rootCtxt.createChild("test", sofamap);
    cas.setCurrentComponentInfo(childCtxt.getComponentInfo());
    checkViewsExist(cas.getViewIterator("SourceDocument"), viewE1, viewE2);

    cas.setCurrentComponentInfo(null);

    // repeat with JCas
    cas.reset();
    JCas jcas = cas.getJCas();
    JCas jview1 = jcas.createView("View1");
    JCas jview2 = jcas.createView("View2");
    checkViewsExist(jcas.getViewIterator(), jcas, jview1, jview2);

    JCas jviewE1 = jcas.createView("EnglishDocument");
    JCas jviewE2 = jcas.createView("EnglishDocument.2");
    checkViewsExist(jcas.getViewIterator("EnglishDocument"), jviewE1, jviewE2);

    // try with Sofa mappings
    cas.setCurrentComponentInfo(childCtxt.getComponentInfo());
    checkViewsExist(jcas.getViewIterator("SourceDocument"), jviewE1, jviewE2);
  }
}
