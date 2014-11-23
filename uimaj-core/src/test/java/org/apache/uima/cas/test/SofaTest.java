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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContextAdmin;
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
import org.xml.sax.SAXException;


public class SofaTest extends TestCase {

  private CASMgr casMgr;

  private CAS cas;

  private Type annotationType;

  private Type docAnnotationType;

  private Type crossType;

  private Feature otherFeat;

//  private Feature annotSofaFeat;

  public SofaTest(String arg) {
    super(arg);
  }

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    try {
      super.setUp();
      this.casMgr = CASFactory.createCAS();
      CasCreationUtils.setupTypeSystem(this.casMgr, (TypeSystemDescription) null);
      // Create a writable type system.
      TypeSystemMgr tsa = this.casMgr.getTypeSystemMgr();
      // Add new types and features.
      // Type topType = tsa.getTopType();
      this.annotationType = tsa.getType(CAS.TYPE_NAME_ANNOTATION);
//      annotSofaFeat = annotationType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_SOFA);
      this.docAnnotationType = tsa.getType(CAS.TYPE_NAME_DOCUMENT_ANNOTATION);
      assertTrue(this.annotationType != null);
      this.crossType = tsa.addType("sofa.test.CrossAnnotation", this.annotationType);
      this.otherFeat = tsa.addFeature("otherAnnotation", this.crossType, this.annotationType);
      // Commit the type system.
      ((CASImpl) this.casMgr).commitTypeSystem();

      // Create the Base indexes.
      this.casMgr.initCASIndexes();
      FSIndexRepositoryMgr irm = this.casMgr.getIndexRepositoryMgr();
      // init.initIndexes(irm, casMgr.getTypeSystemMgr());
      irm.commit();

      this.cas = this.casMgr.getCAS().getView(CAS.NAME_DEFAULT_SOFA);
      assertTrue(this.cas.getSofa() == null);
      assertTrue(this.cas.getViewName().equals(CAS.NAME_DEFAULT_SOFA));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
  
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
  public void testMain() throws Exception {
    try {

      // Create a Sofa (using old APIs for now)
      SofaID_impl id = new SofaID_impl();
      id.setSofaID("EnglishDocument");
      SofaFS es = this.cas.createSofa(id, "text");
      // Initial View is #1!!!
      assertTrue(2 == es.getSofaRef());

      // Set the document text
      es.setLocalSofaData("this beer is good");

      // Test Multiple Sofas across XCAS serialization
      String xcasFilename = "Sofa.xcas";
      XCASSerializer ser = new XCASSerializer(this.cas.getTypeSystem());
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
     this.cas.reset();
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
        assertTrue(xcasFile.delete());
      }
      
      // Add a new Sofa
      // SofaID_impl gid = new SofaID_impl();
      // gid.setSofaID("GermanDocument");
      // SofaFS gs = ((CASImpl)cas).createSofa(gid,"text");
      CAS gerTcas =this.cas.createView("GermanDocument");
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
      CAS frT =this.cas.createView("FrenchDocument");
      assertTrue(frT.getViewName().equals("FrenchDocument"));
      SofaFS fs = frT.getSofa();
      assertTrue(fs != null);
      assertTrue(4 == fs.getSofaRef());

      // Test multiple Sofas across blob serialization
      ByteArrayOutputStream fos = new ByteArrayOutputStream();
      Serialization.serializeCAS(cas, fos);
      this.cas.reset();
      ByteArrayInputStream fis = new ByteArrayInputStream(fos.toByteArray());
      Serialization.deserializeCAS(cas, fis);

      // Open TCas views of some Sofas
      CAS engTcas =this.cas.getView(es);
      assertTrue(engTcas.getViewName().equals("EnglishDocument"));
      CAS frTcas =this.cas.getView("FrenchDocument");

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
        
        // should throw an error, because you can't add to index a FS which is a subtype of AnnotationBase, whose
        // whose sofa ref is to a different View
        try {
          frTcas.getIndexRepository().addFS(engAnnot);
        } catch (Exception e) {
          assertTrue(e instanceof CASRuntimeException);
          CASRuntimeException c = (CASRuntimeException) e;
          assertTrue("ANNOTATION_IN_WRONG_INDEX".equals(c.getMessageKey()));
        }
        
        AnnotationFS frAnnot = frTcas.createAnnotation(this.annotationType, frBegin, frEnd);
        frTcas.getIndexRepository().addFS(frAnnot);

        AnnotationFS gerAnnot = gerTcas.createAnnotation(this.crossType, gerBegin, gerEnd);
        gerAnnot.setFeatureValue(this.otherFeat, engAnnot);
        gerTcas.getIndexRepository().addFS(gerAnnot);
      }

      // Test that the annotations are in separate index spaces, and that
      // Sofas are indexed
      // FSIndex sofaIndex =
      // cas.getIndexRepository().getIndex(CAS.SOFA_INDEX_NAME);
      FSIterator<SofaFS> sofaIter =this.cas.getSofaIterator();
      int numSofas = 0;
      while (sofaIter.isValid()) {
        numSofas++;
        sofaIter.moveToNext();
      }
      FSIndex<AnnotationFS> engIndex = engTcas.getAnnotationIndex();
      FSIndex<AnnotationFS> gerIndex = gerTcas.getAnnotationIndex();
      FSIndex<AnnotationFS> frIndex = frTcas.getAnnotationIndex();
      // assertTrue(sofaIndex.size() == 3); // 3 sofas
      assertTrue(numSofas == 3);
      assertTrue(engIndex.size() == 5); // 4 annots plus
      // documentAnnotation
      assertTrue(gerIndex.size() == 5); // 4 annots plus
      // documentAnnotation
      assertTrue(frIndex.size() == 5); // 4 annots plus
      // documentAnnotation

      // Test that the annotations are of the correct types
      FSIterator<AnnotationFS> engIt = engIndex.iterator();
      FSIterator<AnnotationFS> gerIt = gerIndex.iterator();
      FSIterator<AnnotationFS> frIt = frIndex.iterator();
      AnnotationFS engAnnot = engIt.get();
      AnnotationFS gerAnnot = gerIt.get();
      AnnotationFS frAnnot = frIt.get();
      assertTrue(docAnnotationType.getName().equals(engAnnot.getType().getName()));
      assertTrue(docAnnotationType.getName().equals(gerAnnot.getType().getName()));
      assertTrue(docAnnotationType.getName().equals(frAnnot.getType().getName()));

      engIt.moveToNext();
      gerIt.moveToNext();
      frIt.moveToNext();
      engAnnot = engIt.get();
      gerAnnot = gerIt.get();
      frAnnot = frIt.get();
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
     
      //Test equals method for same annotation obtained through different views
      assertEquals(engAnnot, crossAnnot);
      
      // Test that annotations accessed from a reference in the base CAS
      // work correctly
      ArrayFS anArray =this.cas.createArrayFS(3);
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

  /*
   * Test stream access to Sofa Data.
   */
  public void testSofaDataStream() throws Exception {
    try {

      // Create Sofas
      // Create a local Sofa and set string feature
      // SofaID_impl id = new SofaID_impl();
      // id.setSofaID("StringSofaData");
      // SofaFS strSofa = cas.createSofa(id, "text");
      // strSofa.setLocalSofaData("this beer is good");
      CAS stringView =this.cas.createView("StringSofaData");
      stringView.setDocumentText("this beer is good");

      // create a int array fs
      IntArrayFS intArrayFS =this.cas.createIntArrayFS(5);
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
      CAS intArrayView =this.cas.createView("intArraySofaData");
      intArrayView.setSofaDataArray(intArrayFS, "integers");

      // create a string array fs
      StringArrayFS stringArrayFS =this.cas.createStringArrayFS(5);
      stringArrayFS.set(0, "This");
      stringArrayFS.set(1, "beer");
      stringArrayFS.set(2, "is");
      stringArrayFS.set(3, "really");
      stringArrayFS.set(4, "good");
      CAS stringArrayView =this.cas.createView("stringArraySofaData");
      stringArrayView.setSofaDataArray(stringArrayFS, "strings");

      // create a float array fs
      FloatArrayFS floatArrayFS =this.cas.createFloatArrayFS(5);
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
      CAS floatArrayView =this.cas.createView("floatArraySofaData");
      floatArrayView.setSofaDataArray(floatArrayFS, "floats");

      // create a short array fs
      ShortArrayFS shortArrayFS =this.cas.createShortArrayFS(5);
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
      CAS shortArrayView =this.cas.createView("shortArraySofaData");
      shortArrayView.setSofaDataArray(shortArrayFS, "shorts");

      // create a byte array fs
      ByteArrayFS byteArrayFS =this.cas.createByteArrayFS(5);
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
      CAS byteArrayView =this.cas.createView("byteArraySofaData");
      byteArrayView.setSofaDataArray(byteArrayFS, "bytes");

      // create a long array fs
      LongArrayFS longArrayFS =this.cas.createLongArrayFS(5);
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
      CAS longArrayView =this.cas.createView("longArraySofaData");
      longArrayView.setSofaDataArray(longArrayFS, "longs");

      DoubleArrayFS doubleArrayFS =this.cas.createDoubleArrayFS(5);
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
      CAS doubleArrayView =this.cas.createView("doubleArraySofaData");
      doubleArrayView.setSofaDataArray(doubleArrayFS, "doubles");

      // create remote sofa and set the SofaURI feature
      // id = new SofaID_impl();
      // id.setSofaID("remoteSofaData");
      // SofaFS remoteSofa = cas.createSofa(id, "text");
      // remoteSofa.setRemoteSofaURI("file:.\\Sofa.xcas");
      CAS remoteView =this.cas.createView("remoteSofaData");
      String sofaFileName = "./Sofa.xcas";
      remoteView.setSofaDataURI("file:" + sofaFileName, "text");
      PrintWriter out = new PrintWriter(sofaFileName);
      out.print("this beer is good");
      out.close();
      
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
      }

      is.close();
      is = stringArrayView.getSofaDataStream();
      assertTrue(is != null);
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      i = 0;
      while (br.ready()) {
        assertTrue(stringArrayFS.get(i++).equals(br.readLine()));
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
      is.close();
      // is = remoteSofa.getSofaDataStream();
      is = remoteView.getSofaDataStream();
      assertTrue(is != null);
      buf = new StringBuffer();
      while (is.read(dest) != -1) {
        buf.append((char) dest[0]);
      }
      assertTrue(buf.toString().equals("this beer is good"));
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
  
  public void testSetSofaDataString() {
    final String TEST_TEXT = "this is a test";
    final String TEST_MIME = "text/plain";
    CAS testView = this.cas.createView("TestView");
    testView.setSofaDataString(TEST_TEXT, TEST_MIME);
    assertEquals(TEST_TEXT, testView.getSofa().getLocalStringData());
    assertEquals(TEST_MIME, testView.getSofa().getSofaMime());
    assertEquals(TEST_TEXT, testView.getSofaDataString());
  }
  
  public void testSetSofaDataStringOnInitialView() {
    final String TEST_TEXT = "this is a test";
    final String TEST_MIME = "text/plain";
    this.cas.setSofaDataString(TEST_TEXT, TEST_MIME);
    assertEquals(TEST_TEXT, this.cas.getSofa().getLocalStringData());
    assertEquals(TEST_MIME, this.cas.getSofa().getSofaMime());
    assertEquals(TEST_TEXT, this.cas.getSofaDataString());
  }

  public void testSetSofaDataURI() {
    final String TEST_URI = "file:/test";
    final String TEST_MIME = "text/plain";
    CAS testView = this.cas.createView("TestView");
    testView.setSofaDataURI(TEST_URI, TEST_MIME);
    assertEquals(TEST_URI, testView.getSofa().getSofaURI());
    assertEquals(TEST_MIME, testView.getSofa().getSofaMime());
  }
  
  public void testSetSofaDataURIonInitialView() throws Exception {
    // This test uses platform encoding both for reading and writing.  
    String someText="remote text.";
    String someTextFile="./someUriText.txt";
    FileWriter output = new FileWriter(someTextFile);
    output.write(someText);
    output.close();

    final String TEST_URI = "file:" + someTextFile;
    final String TEST_MIME = "text/plain";
    this.cas.setSofaDataURI(TEST_URI, TEST_MIME);
    assertEquals(TEST_URI, this.cas.getSofa().getSofaURI());
    assertEquals(TEST_MIME, this.cas.getSofa().getSofaMime());
    
    InputStream is = this.cas.getSofaDataStream();
    assertTrue(is != null);

    // This obviously can't work on all platforms
//    byte[] dest = new byte[1];
//    StringBuffer buf = new StringBuffer();
//    while (is.read(dest) != -1) {
//      buf.append((char) dest[0]);
//    }
//    is.close();
//    assertTrue(buf.toString().equals(someText));
    
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String textFromFile = reader.readLine();
    is.close();
    assertTrue(textFromFile.equals(someText));
    File testFile = new File(someTextFile);
    assertTrue(testFile.delete());
  }
  
  public void testSetSofaDataArray() {
    final String TEST_MIME = "text/plain";    
    CAS testView = this.cas.createView("TestView");
    ByteArrayFS sofaDataArray = testView.createByteArrayFS(2);
    sofaDataArray.set(0, (byte)0);
    sofaDataArray.set(1, (byte)42);
    testView.setSofaDataArray(sofaDataArray, TEST_MIME);
    assertEquals(sofaDataArray, testView.getSofa().getLocalFSData());
    assertEquals(TEST_MIME, testView.getSofa().getSofaMime());
  }
  
  public void testSetSofaDataArrayOnInitialView() {
    final String TEST_MIME = "text/plain";    
    ByteArrayFS sofaDataArray = this.cas.createByteArrayFS(2);
    sofaDataArray.set(0, (byte)0);
    sofaDataArray.set(1, (byte)42);
    this.cas.setSofaDataArray(sofaDataArray, TEST_MIME);
    assertEquals(sofaDataArray, this.cas.getSofa().getLocalFSData());
    assertEquals(TEST_MIME, this.cas.getSofa().getSofaMime());
  }
  
  public void testReset() {
    this.cas.reset();
    this.cas.setDocumentText("setDocumentText creates the _InitialView Sofa");
    CAS testView = this.cas.createView("TestView");
    testView.setDocumentText("create a 2nd Sofa");
    assertTrue( this.cas.getViewName().equals("_InitialView"));
    assertTrue( testView.getViewName().equals("TestView"));
    
    this.cas.reset();
    SofaID_impl id = new SofaID_impl();
    id.setSofaID("TestView");
    SofaFS testSofa = this.cas.createSofa(id, "text");
    CAS newView = this.cas.getView(testSofa);
    assertTrue( newView.getViewName().equals("TestView"));
  }
  
  public void testGetViewIterator() throws Exception {
    this.cas.reset();
    CAS view1 = this.cas.createView("View1");
    CAS view2 = this.cas.createView("View2");
    Iterator<CAS> iter = this.cas.getViewIterator();
    assertEquals(this.cas, iter.next());
    assertEquals(view1, iter.next());
    assertEquals(view2, iter.next());
    assertFalse(iter.hasNext());
    
    CAS viewE1 = this.cas.createView("EnglishDocument");
    CAS viewE2 = this.cas.createView("EnglishDocument.2");
    iter = this.cas.getViewIterator("EnglishDocument");
    assertEquals(viewE1, iter.next());
    assertEquals(viewE2, iter.next());
    assertFalse(iter.hasNext());
    
    //try with Sofa mappings
    UimaContextAdmin rootCtxt = UIMAFramework.newUimaContext(
            UIMAFramework.getLogger(), UIMAFramework.newDefaultResourceManager(),
            UIMAFramework.newConfigurationManager());
    Map<String, String> sofamap = new HashMap<String, String>();
    sofamap.put("SourceDocument","EnglishDocument");
    UimaContextAdmin childCtxt = rootCtxt.createChild("test", sofamap);
    cas.setCurrentComponentInfo(childCtxt.getComponentInfo());
    iter = this.cas.getViewIterator("SourceDocument");
    assertEquals(viewE1, iter.next());
    assertEquals(viewE2, iter.next());
    assertFalse(iter.hasNext());  
    this.cas.setCurrentComponentInfo(null);
    
    //repeat with JCas
    this.cas.reset();
    JCas jcas = this.cas.getJCas();
    JCas jview1 = jcas.createView("View1");
    JCas jview2 = jcas.createView("View2");
    Iterator<JCas> jCasIter = jcas.getViewIterator();
    assertEquals(jcas, jCasIter.next());
    assertEquals(jview1, jCasIter.next());
    assertEquals(jview2, jCasIter.next());
    assertFalse(jCasIter.hasNext());
    
    JCas jviewE1 = jcas.createView("EnglishDocument");
    JCas jviewE2 = jcas.createView("EnglishDocument.2");
    jCasIter = jcas.getViewIterator("EnglishDocument");
    assertEquals(jviewE1, jCasIter.next());
    assertEquals(jviewE2, jCasIter.next());
    assertFalse(jCasIter.hasNext());
    
    //try with Sofa mappings
    cas.setCurrentComponentInfo(childCtxt.getComponentInfo());
    jCasIter = jcas.getViewIterator("SourceDocument");
    assertEquals(jviewE1, jCasIter.next());
    assertEquals(jviewE2, jCasIter.next());
    assertFalse(jCasIter.hasNext());  
    this.cas.setCurrentComponentInfo(null);
  }
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(SofaTest.class);
  }

}
