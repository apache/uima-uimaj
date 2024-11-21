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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.SofaID;
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
import org.apache.uima.impl.SofaID_impl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

class JcasSofaTest {

  private CASMgr casMgr;

  private CAS cas;

  private JCas jcas;

  @BeforeEach
  void setUp() throws Exception {
    casMgr = CASFactory.createCAS();
    CasCreationUtils.setupTypeSystem(casMgr, (TypeSystemDescription) null);
    // Create a writable type system.
    TypeSystemMgr tsa = casMgr.getTypeSystemMgr();
    // Add new types and features.
    Type annotType = tsa.getType(CAS.TYPE_NAME_ANNOTATION);
    Type crossType = tsa.addType("org.apache.uima.cas.test.CrossAnnotation", annotType);
    tsa.addFeature("otherAnnotation", crossType, annotType);
    // Commit the type system.
    ((CASImpl) casMgr).commitTypeSystem();

    // Create the Base indexes.
    casMgr.initCASIndexes();
    FSIndexRepositoryMgr irm = casMgr.getIndexRepositoryMgr();
    // init.initIndexes(irm, casMgr.getTypeSystemMgr());
    irm.commit();

    cas = casMgr.getCAS().getView(CAS.NAME_DEFAULT_SOFA);
    jcas = cas.getJCas();
  }

  @AfterEach
  void tearDown() {
    casMgr = null;
    jcas = null;
    cas = null;
  }

  /**
   * Test driver.
   */
  @Test
  void testMain() throws Exception {
    try {
      // Create a Sofa using OLD APIs for now
      CAS view = cas.createView("EnglishDocument");
      // SofaID_impl id = new SofaID_impl();
      // id.setSofaID("EnglishDocument");
      // Sofa es = new Sofa(jcas, id, "text");
      // Initial View is #1!!!
      assertThat(view.getSofa().getSofaRef()).isEqualTo(2);
      // assertTrue(2 == es.getSofaRef());

      // Set the document text
      view.setSofaDataString("this beer is good", null);

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
      // id.setSofaID("GermanDocument");
      // Sofa gs = new Sofa(jcas, id, "text");
      JCas gerJcas = jcas.createView("GermanDocument");
      Sofa gs = gerJcas.getSofa();
      assertThat(gs.getSofaRef()).isEqualTo(3);

      // Set the document text
      // gs.setLocalSofaData("das bier ist gut");
      gerJcas.setDocumentText("das bier ist gut");

      // Test multiple Sofas across binary serialization
      CASSerializer cs = Serialization.serializeNoMetaData(cas);
      cas = Serialization.createCAS(casMgr, cs);

      // Add a new Sofa
      // id.setSofaID("FrenchDocument");
      // Sofa fs = new Sofa(jcas, id, "text");
      CAS frCas = jcas.getCas().createView("FrenchDocument");
      SofaFS fs = frCas.getSofa();
      assertThat(fs.getSofaRef()).isEqualTo(4);

      // Open JCas views of some Sofas
      JCas engJcas = view.getJCas();
      JCas frJcas = jcas.getView("FrenchDocument");

      // Set the document text after the Jcas view exists using JCas.setDocumentText method
      frJcas.setSofaDataString("cette biere est bonne", "text");

      // Create standard annotations against eng & fr and cross annotations against ger
      int engEnd = 0;
      int gerEnd = 0;
      int frEnd = 0;
      String engText = engJcas.getDocumentText();
      String gerText = gerJcas.getDocumentText();
      String frText = frJcas.getDocumentText();
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

        Annotation engAnnot = new Annotation(engJcas, engBegin, engEnd);
        engAnnot.addToIndexes();

        Annotation frAnnot = new Annotation(frJcas, frBegin, frEnd);
        frAnnot.addToIndexes();

        CrossAnnotation gerAnnot = new CrossAnnotation(gerJcas);
        gerAnnot.setBegin(gerBegin);
        gerAnnot.setEnd(gerEnd);
        gerAnnot.setOtherAnnotation(engAnnot);
        gerAnnot.addToIndexes();
      }

      // Test that the annotations are in separate index spaces, and that Sofas are indexed
      JFSIndexRepository indexes = jcas.getJFSIndexRepository();
      // FSIndex sofaIndex = indexes.getIndex(CAS.SOFA_INDEX_NAME);
      indexes = engJcas.getJFSIndexRepository();
      FSIndex engIndex = indexes.getAnnotationIndex(Annotation.type);
      indexes = gerJcas.getJFSIndexRepository();
      FSIndex gerIndex = indexes.getAnnotationIndex(Annotation.type);
      indexes = frJcas.getJFSIndexRepository();
      FSIndex frIndex = indexes.getAnnotationIndex(Annotation.type);
      FSIterator sofaIter = jcas.getSofaIterator();
      int numSofas = 0;
      while (sofaIter.isValid()) {
        numSofas++;
        sofaIter.moveToNext();
      }
      // assertTrue(sofaIndex.size() == 3); // 3 sofas
      assertThat(numSofas).isEqualTo(3);
      assertThat(engIndex.size()).isEqualTo(5); // 4 annots plus documentAnnotation
      assertThat(gerIndex.size()).isEqualTo(5); // 4 annots plus documentAnnotation
      assertThat(frIndex.size()).isEqualTo(5); // 4 annots plus documentAnnotation

      // Test that the annotations are of the correct types
      FSIterator engIt = engIndex.iterator();
      FSIterator gerIt = gerIndex.iterator();
      FSIterator frIt = frIndex.iterator();
      Annotation engAnnot = (Annotation) engIt.get();
      Annotation gerAnnot = (Annotation) gerIt.get();
      Annotation frAnnot = (Annotation) frIt.get();
      assertThat(engAnnot.getType().getName()).isEqualTo((CAS.TYPE_NAME_DOCUMENT_ANNOTATION));
      assertThat(gerAnnot.getType().getName()).isEqualTo((CAS.TYPE_NAME_DOCUMENT_ANNOTATION));
      assertThat(frAnnot.getType().getName()).isEqualTo((CAS.TYPE_NAME_DOCUMENT_ANNOTATION));

      engIt.moveToNext();
      gerIt.moveToNext();
      frIt.moveToNext();
      engAnnot = (Annotation) engIt.get();
      CrossAnnotation gerCrossAnnot = (CrossAnnotation) gerIt.get();
      frAnnot = (Annotation) frIt.get();
      assertThat(engAnnot.getType().getName()).isEqualTo((CAS.TYPE_NAME_ANNOTATION));
      assertThat(engAnnot.getCoveredText()).isEqualTo(("this"));
      assertThat(frAnnot.getType().getName()).isEqualTo((CAS.TYPE_NAME_ANNOTATION));
      assertThat(frAnnot.getCoveredText()).isEqualTo(("cette"));
      assertThat(gerCrossAnnot.getType().getName())
              .isEqualTo(("org.apache.uima.cas.test.CrossAnnotation"));
      assertThat(gerCrossAnnot.getCoveredText()).isEqualTo(("das"));

      // Test that the other annotation feature of cross annotations works
      Annotation crossAnnot = gerCrossAnnot.getOtherAnnotation();
      assertThat(crossAnnot.getType().getName()).isEqualTo((CAS.TYPE_NAME_ANNOTATION));
      assertThat(crossAnnot.getCoveredText()).isEqualTo(("this"));

      // Test that annotations accessed from a reference in the base CAS work correctly
      FSArray anArray = new FSArray(jcas, 3);
      anArray.set(0, engAnnot);
      anArray.set(1, frAnnot);
      anArray.set(2, gerCrossAnnot);
      Annotation tstAnnot = (Annotation) anArray.get(0);
      assertThat(tstAnnot.getCoveredText()).isEqualTo(("this"));
      tstAnnot = (Annotation) anArray.get(1);
      assertThat(tstAnnot.getCoveredText()).isEqualTo(("cette"));
      tstAnnot = (Annotation) anArray.get(2);
      assertThat(tstAnnot.getCoveredText()).isEqualTo(("das"));

      // // code to write out test cas used by other routines,
      // // normally commented out, unless need to regenerate
      // xcasFilename = "testTypeSystemNew.xml";
      // ser = new XCASSerializer(cas.getTypeSystem());
      // outputXCAS = new FileOutputStream(xcasFilename);
      // xmlSer = new XMLSerializer(outputXCAS);
      // try {
      // ser.serialize(cas, xmlSer.getContentHandler());
      // outputXCAS.close();
      // } catch (IOException e) {
      // e.printStackTrace();
      // } catch (SAXException e) {
      // e.printStackTrace();
      // }

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
      // Sofa strSofa = new Sofa(jcas, id, "text");
      // strSofa.setLocalSofaData("this beer is good");
      JCas stringView = jcas.createView("StringSofaData");
      stringView.setDocumentText("this beer is good");

      // create a int array fs
      IntegerArray intArrayFS = new IntegerArray(jcas, 5);
      intArrayFS.set(0, 1);
      intArrayFS.set(1, 2);
      intArrayFS.set(2, 3);
      intArrayFS.set(3, 4);
      intArrayFS.set(4, 5);
      // create a Sofa and set the SofaArray feature to an int array FS.
      // id = new SofaID_impl();
      // id.setSofaID("intArraySofaData");
      // SofaFS intarraySofaFS = new Sofa(jcas, id, "text");
      // intarraySofaFS.setLocalSofaData(intArrayFS);
      JCas intArrayView = jcas.createView("intArraySofaData");
      intArrayView.setSofaDataArray(intArrayFS, "integers");

      // create a float array fs
      FloatArray floatArrayFS = new FloatArray(jcas, 5);
      floatArrayFS.set(0, (float) 0.1);
      floatArrayFS.set(1, (float) 0.2);
      floatArrayFS.set(2, (float) 0.3);
      floatArrayFS.set(3, (float) 0.4);
      floatArrayFS.set(4, (float) 0.5);
      // create a sofa and set the SofaArray feature to the float array
      // id = new SofaID_impl();
      // id.setSofaID("floatArraySofaData");
      // SofaFS floatarraySofaFS = new Sofa(jcas, id, "text");
      // floatarraySofaFS.setLocalSofaData(floatArrayFS);
      JCas floatArrayView = jcas.createView("floatArraySofaData");
      floatArrayView.setSofaDataArray(floatArrayFS, "floats");

      // create a short array fs
      ShortArray shortArrayFS = new ShortArray(jcas, 5);
      shortArrayFS.set(0, (short) 128);
      shortArrayFS.set(1, (short) 127);
      shortArrayFS.set(2, (short) 126);
      shortArrayFS.set(3, (short) 125);
      shortArrayFS.set(4, (short) 124);
      // create a Sofa and set the SofaArray feature to an int array FS.
      JCas shortArrayView = jcas.createView("shortArraySofaData");
      shortArrayView.setSofaDataArray(shortArrayFS, "shorts");

      // create a byte array fs
      ByteArray byteArrayFS = new ByteArray(jcas, 5);
      byteArrayFS.set(0, (byte) 8);
      byteArrayFS.set(1, (byte) 16);
      byteArrayFS.set(2, (byte) 64);
      byteArrayFS.set(3, (byte) 128);
      byteArrayFS.set(4, (byte) 255);
      // create a Sofa and set the SofaArray feature.
      JCas byteArrayView = jcas.createView("byteArraySofaData");
      byteArrayView.setSofaDataArray(byteArrayFS, "bytes");

      // create a long array fs
      LongArray longArrayFS = new LongArray(jcas, 5);
      longArrayFS.set(0, Long.MAX_VALUE);
      longArrayFS.set(1, Long.MAX_VALUE - 1);
      longArrayFS.set(2, Long.MAX_VALUE - 2);
      longArrayFS.set(3, Long.MAX_VALUE - 3);
      longArrayFS.set(4, Long.MAX_VALUE - 4);
      // create a Sofa and set the SofaArray feature.
      JCas longArrayView = jcas.createView("longArraySofaData");
      longArrayView.setSofaDataArray(longArrayFS, "longs");

      DoubleArray doubleArrayFS = new DoubleArray(jcas, 5);
      doubleArrayFS.set(0, Double.MAX_VALUE);
      doubleArrayFS.set(1, Double.MIN_VALUE);
      doubleArrayFS.set(2, Double.parseDouble("1.5555"));
      doubleArrayFS.set(3, Double.parseDouble("99.000000005"));
      doubleArrayFS.set(4, Double.parseDouble("4.44444444444444444"));
      // create a Sofa and set the SofaArray feature.
      JCas doubleArrayView = jcas.createView("doubleArraySofaData");
      doubleArrayView.setSofaDataArray(doubleArrayFS, "doubles");

      // create remote sofa and set the SofaURI feature
      JCas remoteView = jcas.createView("remoteSofaData");
      String sofaFileName = "./Sofa.xcas";
      remoteView.setSofaDataURI("file:" + sofaFileName, "text");
      PrintWriter out = new PrintWriter(sofaFileName);
      out.print("this beer is good");
      out.close();

      // read sofa data
      InputStream is = stringView.getSofaDataStream();
      assertThat(is).isNotNull();
      byte[] dest = new byte[1];
      StringBuffer buf = new StringBuffer();
      while (is.read(dest) != -1) {
        buf.append((char) dest[0]);
      }
      assertThat(buf.toString()).isEqualTo("this beer is good");

      dest = new byte[4];
      is.close();
      is = intArrayView.getSofaDataStream();
      assertThat(is).isNotNull();
      BufferedInputStream bis = new BufferedInputStream(is);
      int i = 0;
      while (bis.read(dest) != -1) {
        assertThat(intArrayFS.get(i++)).isEqualTo(ByteBuffer.wrap(dest).getInt());
      }

      bis.close();

      is = floatArrayView.getSofaDataStream();
      assertThat(is).isNotNull();
      bis = new BufferedInputStream(is);
      i = 0;
      while (bis.read(dest) != -1) {
        assertThat(floatArrayFS.get(i++)).isCloseTo(ByteBuffer.wrap(dest).getFloat(), offset(0.0f));
      }

      dest = new byte[2];
      bis.close();
      is = shortArrayView.getSofaDataStream();
      assertThat(is).isNotNull();
      bis = new BufferedInputStream(is);
      i = 0;
      while (bis.read(dest) != -1) {
        assertThat(shortArrayFS.get(i++)).isEqualTo(ByteBuffer.wrap(dest).getShort());
      }

      dest = new byte[1];
      bis.close();
      is = byteArrayView.getSofaDataStream();
      assertThat(is).isNotNull();
      bis = new BufferedInputStream(is);
      i = 0;
      while (bis.read(dest) != -1) {
        assertThat(byteArrayFS.get(i++)).isEqualTo(ByteBuffer.wrap(dest).get());
      }

      dest = new byte[8];
      bis.close();
      is = longArrayView.getSofaDataStream();
      assertThat(is).isNotNull();
      bis = new BufferedInputStream(is);
      i = 0;
      while (bis.read(dest) != -1) {
        assertThat(longArrayFS.get(i++)).isEqualTo(ByteBuffer.wrap(dest).getLong());
      }

      bis.close();
      is = doubleArrayView.getSofaDataStream();
      assertThat(is).isNotNull();
      bis = new BufferedInputStream(is);
      i = 0;
      while (bis.read(dest) != -1) {
        assertThat(doubleArrayFS.get(i++)).isCloseTo(ByteBuffer.wrap(dest).getDouble(),
                offset(0.0));
      }

      dest = new byte[1];
      bis.close();
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
  void testIndexTwice() throws Exception {
    try {
      CAS newCas = CasCreationUtils.createCas(new TypeSystemDescription_impl(), null, null);
      JCas newJCas = newCas.getJCas();
      CAS view = newCas.createView("DetaggedView");
      view.getJCas();

      Annotation annot = new Annotation(newJCas);
      annot.addToIndexes();

      Iterator<Annotation> annotIter = newJCas.getAnnotationIndex(Annotation.type).iterator();
      Annotation annot2 = annotIter.next();
      assertThat(annot2).isEqualTo(annot);
      assertThat(annot2.getCASImpl().getSofa()).isEqualTo(annot2.getSofa());

      annot2.addToIndexes();
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Deprecated(since = "2.0.0")
  @Test
  void testGetSofa() throws Exception {
    File typeSystemFile = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
    TypeSystemDescription typeSystem = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(typeSystemFile));
    CAS newCas = CasCreationUtils.createCas(typeSystem, null, null);
    File xcasFile = JUnitExtension.getFile("ExampleCas/multiSofaCas.xml");
    XCASDeserializer.deserialize(new FileInputStream(xcasFile), newCas);
    JCas newJCas = newCas.getJCas();

    SofaID sofaId = new SofaID_impl("EnglishDocument");
    JCas view = newJCas.getView(newJCas.getSofa(sofaId));
  }
}
