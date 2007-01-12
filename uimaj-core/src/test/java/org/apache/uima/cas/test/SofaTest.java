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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;

import junit.framework.TestCase;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.SofaFS;
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
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.SAXException;


public class SofaTest extends TestCase {

  private CASMgr casMgr;

  private CAS cas;

  private Type annotationType;

  private Type docAnnotationType;

  private Type crossType;

  private Feature otherFeat;

  private Feature annotSofaFeat;

  public SofaTest(String arg) {
    super(arg);
  }

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    try {
      super.setUp();
      casMgr = CASFactory.createCAS();
      CasCreationUtils.setupTypeSystem(casMgr, (TypeSystemDescription) null);
      // Create a writable type system.
      TypeSystemMgr tsa = casMgr.getTypeSystemMgr();
      // Add new types and features.
      // Type topType = tsa.getTopType();
      annotationType = tsa.getType(CAS.TYPE_NAME_ANNOTATION);
      annotSofaFeat = annotationType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_SOFA);
      docAnnotationType = tsa.getType(CAS.TYPE_NAME_DOCUMENT_ANNOTATION);
      assertTrue(annotationType != null);
      crossType = tsa.addType("sofa.test.CrossAnnotation", annotationType);
      otherFeat = tsa.addFeature("otherAnnotation", crossType, annotationType);
      // Commit the type system.
      ((CASImpl) casMgr).commitTypeSystem();

      // Create the Base indexes.
      casMgr.initCASIndexes();
      FSIndexRepositoryMgr irm = casMgr.getIndexRepositoryMgr();
      // init.initIndexes(irm, casMgr.getTypeSystemMgr());
      irm.commit();

      cas = casMgr.getCAS().getView(CAS.NAME_DEFAULT_SOFA);
      assertTrue(cas.getSofa() == null);
      assertTrue(cas.getViewName().equals(CAS.NAME_DEFAULT_SOFA));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }

  }

  /**
   * Test driver.
   */
  public void testMain() throws Exception {
    try {

      // Create a Sofa (using old APIs for now)
      SofaID_impl id = new SofaID_impl();
      id.setSofaID("EnglishDocument");
      SofaFS es = cas.createSofa(id, "text");
      // Initial View is #1!!!
      assertTrue(2 == es.getSofaRef());

      // Set the document text
      es.setLocalSofaData("this beer is good");

      // Test Multiple Sofas across XCAS serialization
      XCASSerializer ser = new XCASSerializer(cas.getTypeSystem());
      OutputStream outputXCAS = (OutputStream) new FileOutputStream("Sofa.xcas");
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
      InputStream inputXCAS = (InputStream) new FileInputStream("Sofa.xcas");
      try {
        XCASDeserializer.deserialize(inputXCAS, cas, false);
        inputXCAS.close();
      } catch (SAXException e2) {
        e2.printStackTrace();
      } catch (IOException e2) {
        e2.printStackTrace();
      }

      // Add a new Sofa
      // SofaID_impl gid = new SofaID_impl();
      // gid.setSofaID("GermanDocument");
      // SofaFS gs = ((CASImpl)cas).createSofa(gid,"text");
      CAS gerTcas = cas.createView("GermanDocument");
      assertTrue(gerTcas.getViewName().equals("GermanDocument"));
      SofaFS gs = gerTcas.getSofa();

      assertTrue(gs != null);
      assertTrue(3 == gs.getSofaRef());

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
      assertTrue(frT.getViewName().equals("FrenchDocument"));
      SofaFS fs = frT.getSofa();
      assertTrue(fs != null);
      assertTrue(4 == fs.getSofaRef());

      // Test multiple Sofas across blob serialization
      ByteArrayOutputStream fos = new ByteArrayOutputStream();
      Serialization.serializeCAS(cas, fos);
      cas.reset();
      ByteArrayInputStream fis = new ByteArrayInputStream(fos.toByteArray());
      Serialization.deserializeCAS(cas, fis);

      // Open TCas views of some Sofas
      CAS engTcas = cas.getView(es);
      assertTrue(engTcas.getViewName().equals("EnglishDocument"));
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
        assertTrue(gst.hasMoreTokens());
        assertTrue(fst.hasMoreTokens());

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
      FSIterator sofaIter = cas.getSofaIterator();
      int numSofas = 0;
      while (sofaIter.isValid()) {
        numSofas++;
        sofaIter.moveToNext();
      }
      FSIndex engIndex = engTcas.getAnnotationIndex();
      FSIndex gerIndex = gerTcas.getAnnotationIndex();
      FSIndex frIndex = frTcas.getAnnotationIndex();
      // assertTrue(sofaIndex.size() == 3); // 3 sofas
      assertTrue(numSofas == 3);
      assertTrue(engIndex.size() == 5); // 4 annots plus
      // documentAnnotation
      assertTrue(gerIndex.size() == 5); // 4 annots plus
      // documentAnnotation
      assertTrue(frIndex.size() == 5); // 4 annots plus
      // documentAnnotation

      // Test that the annotations are of the correct types
      FSIterator engIt = engIndex.iterator();
      FSIterator gerIt = gerIndex.iterator();
      FSIterator frIt = frIndex.iterator();
      AnnotationFS engAnnot = (AnnotationFS) engIt.get();
      AnnotationFS gerAnnot = (AnnotationFS) gerIt.get();
      AnnotationFS frAnnot = (AnnotationFS) frIt.get();
      assertTrue(docAnnotationType.getName().equals(engAnnot.getType().getName()));
      assertTrue(docAnnotationType.getName().equals(gerAnnot.getType().getName()));
      assertTrue(docAnnotationType.getName().equals(frAnnot.getType().getName()));

      engIt.moveToNext();
      gerIt.moveToNext();
      frIt.moveToNext();
      engAnnot = (AnnotationFS) engIt.get();
      gerAnnot = (AnnotationFS) gerIt.get();
      frAnnot = (AnnotationFS) frIt.get();
      assertTrue(annotationType.getName().equals(engAnnot.getType().getName()));
      assertTrue(("this").equals(engAnnot.getCoveredText()));
      assertTrue(annotationType.getName().equals(frAnnot.getType().getName()));
      assertTrue(("cette").equals(frAnnot.getCoveredText()));
      assertTrue(crossType.getName().equals(gerAnnot.getType().getName()));
      assertTrue(("das").equals(gerAnnot.getCoveredText()));

      // Test that the other annotation feature of cross annotations works
      AnnotationFS crossAnnot = (AnnotationFS) gerAnnot.getFeatureValue(otherFeat);
      assertTrue(annotationType.getName().equals(crossAnnot.getType().getName()));
      assertTrue(("this").equals(crossAnnot.getCoveredText()));

      // Test that annotations accessed from a reference in the base CAS
      // work correctly
      ArrayFS anArray = cas.createArrayFS(3);
      anArray.set(0, engAnnot);
      anArray.set(1, frAnnot);
      anArray.set(2, gerAnnot);
      AnnotationFS tstAnnot = (AnnotationFS) anArray.get(0);
      assertTrue(("this").equals(tstAnnot.getCoveredText()));
      tstAnnot = (AnnotationFS) anArray.get(1);
      assertTrue(("cette").equals(tstAnnot.getCoveredText()));
      tstAnnot = (AnnotationFS) anArray.get(2);
      assertTrue(("das").equals(tstAnnot.getCoveredText()));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }

  }

  /**
   * Test stream access to Sofa Data.
   * 
   * @throws Exception
   */
  public void testSofaDataStream() throws Exception {
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

      // read sofa data
      // InputStream is = strSofa.getSofaDataStream();
      InputStream is = stringView.getSofaDataStream();
      assertTrue(is != null);
      byte[] dest = new byte[1];
      StringBuffer buf = new StringBuffer();
      while (is.read(dest) != -1) {
        buf.append((char) dest[0]);
      }
      assertTrue(buf.toString().equals("this beer is good"));

      dest = new byte[4];
      // is = intarraySofaFS.getSofaDataStream();
      is.close();
      is = intArrayView.getSofaDataStream();
      assertTrue(is != null);
      int i = 0;
      while (is.read(dest) != -1) {
        assertTrue(ByteBuffer.wrap(dest).getInt() == intArrayFS.get(i++));
        ;
      }

      // is = floatarraySofaFS.getSofaDataStream();
      is.close();
      is = floatArrayView.getSofaDataStream();
      assertTrue(is != null);
      i = 0;
      while (is.read(dest) != -1) {
        assertTrue(ByteBuffer.wrap(dest).getFloat() == floatArrayFS.get(i++));
      }

      dest = new byte[2];
      // is = shortarraySofaFS.getSofaDataStream();
      is.close();
      is = shortArrayView.getSofaDataStream();
      assertTrue(is != null);
      i = 0;
      while (is.read(dest) != -1) {
        assertTrue(ByteBuffer.wrap(dest).getShort() == shortArrayFS.get(i++));
      }

      dest = new byte[1];
      // is = bytearraySofaFS.getSofaDataStream();
      is.close();
      is = byteArrayView.getSofaDataStream();
      assertTrue(is != null);
      i = 0;
      while (is.read(dest) != -1) {
        assertTrue(ByteBuffer.wrap(dest).get() == byteArrayFS.get(i++));
      }

      dest = new byte[8];
      // is = longarraySofaFS.getSofaDataStream();
      is.close();
      is = longArrayView.getSofaDataStream();
      assertTrue(is != null);
      i = 0;
      while (is.read(dest) != -1) {
        assertTrue(ByteBuffer.wrap(dest).getLong() == longArrayFS.get(i++));
      }

      // is = doublearraySofaFS.getSofaDataStream();
      is.close();
      is = doubleArrayView.getSofaDataStream();
      assertTrue(is != null);
      i = 0;
      while (is.read(dest) != -1) {
        assertTrue(ByteBuffer.wrap(dest).getDouble() == doubleArrayFS.get(i++));
      }

      dest = new byte[1];
      // is = remoteSofa.getSofaDataStream();
      is = remoteView.getSofaDataStream();
      assertTrue(is != null);

      is.close();
      // Delete the generated file.
      File xcasFile = new File(sofaFileName);
      if (xcasFile.exists()) {
        assertTrue(xcasFile.delete());
      }

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(SofaTest.class);
  }

}
