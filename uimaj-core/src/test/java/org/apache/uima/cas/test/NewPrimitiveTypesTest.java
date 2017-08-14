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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.apache.uima.cas.BooleanArrayFS;
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
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.cas.impl.FeatureStructureImplC;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;


public class NewPrimitiveTypesTest extends TestCase {

  private CASMgr casMgr;

  private CAS cas;

  private Type annotationType;

  private Type exampleType;

  private Feature beginFeature;
  
  private Feature floatFeature;

  private Feature stringFeature;

  private Feature byteFeature;

  private Feature booleanFeature;

  private Feature shortFeature;

  private Feature longFeature;

  private Feature doubleFeature;

  private Feature intArrayFeature;

  private Feature floatArrayFeature;

  private Feature stringArrayFeature;

  private Feature byteArrayFeature;

  private Feature booleanArrayFeature;

  private Feature shortArrayFeature;

  private Feature longArrayFeature;

  private Feature doubleArrayFeature;

  public NewPrimitiveTypesTest(String arg) {
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
      annotationType = tsa.getType(CAS.TYPE_NAME_ANNOTATION);
      assertTrue(annotationType != null);

      // new primitive types
      exampleType = tsa.addType("test.primitives.Example", annotationType);
      
      beginFeature = exampleType.getFeatureByBaseName("begin");

      floatFeature = tsa.addFeature("floatFeature", exampleType, tsa.getType(CAS.TYPE_NAME_FLOAT));
      stringFeature = tsa.addFeature("stringFeature", exampleType, tsa
              .getType(CAS.TYPE_NAME_STRING));
      booleanFeature = tsa.addFeature("boolFeature", exampleType, tsa
              .getType(CAS.TYPE_NAME_BOOLEAN));
      byteFeature = tsa.addFeature("byteFeature", exampleType, tsa.getType(CAS.TYPE_NAME_BYTE));
      shortFeature = tsa.addFeature("shortFeature", exampleType, tsa.getType(CAS.TYPE_NAME_SHORT));
      longFeature = tsa.addFeature("longFeature", exampleType, tsa.getType(CAS.TYPE_NAME_LONG));
      doubleFeature = tsa.addFeature("doubleFeature", exampleType, tsa
              .getType(CAS.TYPE_NAME_DOUBLE));

      intArrayFeature = tsa.addFeature("intArrayFeature", exampleType, tsa
              .getType(CAS.TYPE_NAME_INTEGER_ARRAY));
      floatArrayFeature = tsa.addFeature("floatArrayFeature", exampleType, tsa
              .getType(CAS.TYPE_NAME_FLOAT_ARRAY), false);
      stringArrayFeature = tsa.addFeature("stringArrayFeature", exampleType, tsa
              .getType(CAS.TYPE_NAME_STRING_ARRAY), false);
      booleanArrayFeature = tsa.addFeature("boolArrayFeature", exampleType, tsa
              .getType(CAS.TYPE_NAME_BOOLEAN_ARRAY));
      byteArrayFeature = tsa.addFeature("byteArrayFeature", exampleType, tsa
              .getType(CAS.TYPE_NAME_BYTE_ARRAY), false);
      shortArrayFeature = tsa.addFeature("shortArrayFeature", exampleType, tsa
              .getType(CAS.TYPE_NAME_SHORT_ARRAY));
      longArrayFeature = tsa.addFeature("longArrayFeature", exampleType, tsa
              .getType(CAS.TYPE_NAME_LONG_ARRAY));
      doubleArrayFeature = tsa.addFeature("doubleArrayFeature", exampleType, tsa
              .getType(CAS.TYPE_NAME_DOUBLE_ARRAY), false);

      // Commit the type system.
      ((CASImpl) casMgr).commitTypeSystem();

      // Create the Base indexes.
      casMgr.initCASIndexes();

      // create custom indexes to test new primitive keys
      FSIndexRepositoryMgr irm = casMgr.getIndexRepositoryMgr();
      FSIndexComparator comp = irm.createComparator();
      comp.setType(tsa.getType("test.primitives.Example"));
      comp.addKey(tsa.getFeatureByFullName("test.primitives.Example:doubleFeature"),
          FSIndexComparator.STANDARD_COMPARE);
      irm.createIndex(comp, "doubleIndex");

      comp = irm.createComparator();
      comp.setType(tsa.getType("test.primitives.Example"));
      comp.addKey(tsa.getFeatureByFullName("test.primitives.Example:longFeature"),
          FSIndexComparator.REVERSE_STANDARD_COMPARE);
      irm.createIndex(comp, "longIndex");

      comp = irm.createComparator();
      comp.setType(tsa.getType("test.primitives.Example"));
      comp.addKey(tsa.getFeatureByFullName("test.primitives.Example:shortFeature"),
          FSIndexComparator.STANDARD_COMPARE);
      irm.createIndex(comp, "shortIndex");

      irm.commit();

      cas = casMgr.getCAS().getView(CAS.NAME_DEFAULT_SOFA);

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }

  }

  public void tearDown() {
    casMgr = null;
    cas = null;
    annotationType = null;
    exampleType = null;
    floatFeature = null;
    stringFeature = null;
    byteFeature = null;
    booleanFeature = null;
    shortFeature = null;
    longFeature = null;
    doubleFeature = null;
    intArrayFeature = null;
    floatArrayFeature = null;
    stringArrayFeature = null;
    byteArrayFeature = null;
    booleanArrayFeature = null;
    shortArrayFeature = null;
    longArrayFeature = null;
    doubleArrayFeature = null;
  }
  
  public void testCreateFS() throws Exception {

    // create FS
    createExampleFS(cas);
    // check values
    validateFSData(cas);
  }

  public void testBlobSerialization() throws Exception {

    // create FS
    createExampleFS(cas);

    // serialize
    ByteArrayOutputStream fos = new ByteArrayOutputStream();
    Serialization.serializeCAS(cas, fos);

    // reset
    cas.reset();

    // deserialize
    ByteArrayInputStream fis = new ByteArrayInputStream(fos.toByteArray());
    Serialization.deserializeCAS(cas, fis);

    // check values
    validateFSData(cas);
  }

  public void testJavaSerialization() throws Exception {

    // create FS
    createExampleFS(cas);

    // serialize
    CASSerializer cs = Serialization.serializeNoMetaData(cas);

    // reset
    cas.reset();

    // deserialize
    cas = Serialization.createCAS(casMgr, cs);

    // check values
    validateFSData(cas);
  }

  public void testXCASSerialization() throws Exception {

    // create FS
    createExampleFS(cas);

    // serialize
    XCASSerializer ser = new XCASSerializer(cas.getTypeSystem());
    OutputStream outputXCAS = new FileOutputStream(JUnitExtension
            .getFile("ExampleCas/newprimitives.xcas"));
    XMLSerializer xmlSer = new XMLSerializer(outputXCAS);
    ser.serialize(cas, xmlSer.getContentHandler());

    // reset
    cas.reset();

    // deserialize
    InputStream inputXCAS = new FileInputStream(JUnitExtension
            .getFile("ExampleCas/newprimitives.xcas"));
    XCASDeserializer.deserialize(inputXCAS, cas, false);

    // check values
    validateFSData(cas);

  }

  public void testXmiSerialization() throws Exception {

    // create FS
    createExampleFS(cas);

    // serialize

    StringWriter sw = new StringWriter();
    XMLSerializer xmlSer = new XMLSerializer(sw, false);
    XmiCasSerializer xmiSer = new XmiCasSerializer(cas.getTypeSystem());
    xmiSer.serialize(cas, xmlSer.getContentHandler());
    String xml = sw.getBuffer().toString();
    // System.out.println(xml);
    // reset
    cas.reset();

    // deserialize
    XmiCasDeserializer deser = new XmiCasDeserializer(cas.getTypeSystem());
    ContentHandler deserHandler = deser.getXmiCasHandler(cas);
    SAXParserFactory fact = SAXParserFactory.newInstance();
    SAXParser parser = fact.newSAXParser();
    XMLReader xmlReader = parser.getXMLReader();
    xmlReader.setContentHandler(deserHandler);
    xmlReader.parse(new InputSource(new StringReader(xml)));

    // check values
    validateFSData(cas);

  }

  public void testFSPrettyPrint() throws Exception {

    // create FS
    createExampleFS(cas);

    // prettyPrint
    CAS englishView = cas.getView("EnglishDocument");
    assertNotNull(englishView);
    assertNotNull(englishView.getSofa());
    FSIndex index = englishView.getAnnotationIndex();
    FSIterator iter = index.iterator();
    // skip document annotation
    AnnotationFS fs = (AnnotationFS) iter.get();
    iter.moveToNext();

    // the exampleType fs
    fs = (AnnotationFS) iter.get();
    FeatureStructureImplC fsImpl = (FeatureStructureImplC) fs;
    StringBuffer sb = new StringBuffer(1024);
    fsImpl.prettyPrint(2, 1, sb, true);
    // System.out.println(sb.toString());
  }

  public void testClone() throws Exception {
    createExampleFS(cas);
    // get the example FS
    CAS englishView = cas.getView("EnglishDocument");
    FSIterator iter = englishView.getAnnotationIndex().iterator();
    // skip document annotation
    iter.moveToNext();
    // the exampleType fs
    AnnotationFS fs = (AnnotationFS) iter.get();

    // clone it
    AnnotationFS clone = (AnnotationFS) fs.clone();

    // subsitute the clone for the original in the index,
    // and validate that it was correctly copied
    englishView.removeFsFromIndexes(fs);
    englishView.addFsToIndexes(clone);
    validateFSData(cas);

    // editing the original FS should not change the clone
    englishView.removeFsFromIndexes(fs);    // does nothing! because fs already removed 3 statements above
                                            // and remove requires finding == item in index
    fs.setStringValue(stringFeature, "foo");
    fs.setFloatValue(floatFeature, -1f);
    fs.setByteValue(byteFeature, (byte) -1);
    fs.setBooleanValue(booleanFeature, false);
    fs.setShortValue(shortFeature, (short) -1);
    fs.setLongValue(longFeature, -1);
    fs.setDoubleValue(doubleFeature, -1);
    fs.setIntValue(beginFeature, fs.getIntValue(beginFeature) + 1);  // otherwise, gets indexed in front 
    englishView.addFsToIndexes(fs);
    validateFSData(cas);
  }

  private void validateFSData(CAS parmCas) throws Exception {
    CAS englishView = parmCas.getView("EnglishDocument");
    assertNotNull(englishView);
    assertNotNull(englishView.getSofa());
    FSIndex index = englishView.getAnnotationIndex();
    FSIterator iter = index.iterator();
    // skip document annotation
    AnnotationFS fs = (AnnotationFS) iter.get();
    iter.moveToNext();

    // the exampleType fs
    fs = (AnnotationFS) iter.get();
    // System.out.print(fs.toString());

    // check the scalar values
    assertTrue(1 == fs.getBegin());
    assertTrue(5 == fs.getEnd());
    assertTrue(fs.getStringValue(stringFeature).equals("aaaaaaa"));
    assertTrue(fs.getFloatValue(floatFeature) == (float) 99.99);
    assertTrue(fs.getByteValue(byteFeature) == (byte) 'z');
    assertTrue(fs.getBooleanValue(booleanFeature));
    assertTrue(fs.getShortValue(shortFeature) == Short.MIN_VALUE);
    assertTrue(fs.getLongValue(longFeature) == Long.MIN_VALUE);
    assertTrue(fs.getDoubleValue(doubleFeature) == Double.MAX_VALUE);

    // check the array values
    StringArrayFS strArrayFS = (StringArrayFS) fs.getFeatureValue(stringArrayFeature);
    assertTrue(strArrayFS.get(0).equals("zzzzzz"));
    assertTrue(strArrayFS.get(1).equals("yyyyyy"));
    assertTrue(strArrayFS.get(2).equals("xxxxxx"));
    assertTrue(strArrayFS.get(3).equals("wwwwww"));
    assertTrue(strArrayFS.get(4).equals("vvvvvv"));

    IntArrayFS intArrayFS = (IntArrayFS) fs.getFeatureValue(intArrayFeature);
    assertTrue(intArrayFS.get(0) == Integer.MAX_VALUE);
    assertTrue(intArrayFS.get(1) == Integer.MAX_VALUE - 1);
    assertTrue(intArrayFS.get(2) == 42);
    assertTrue(intArrayFS.get(3) == Integer.MIN_VALUE + 1);
    assertTrue(intArrayFS.get(4) == Integer.MIN_VALUE);

    FloatArrayFS floatArrayFS = (FloatArrayFS) fs.getFeatureValue(floatArrayFeature);
    assertTrue(floatArrayFS.get(0) == Float.MAX_VALUE);
    assertTrue(floatArrayFS.get(1) == (float) (Float.MAX_VALUE / 1000.0));
    assertTrue(floatArrayFS.get(2) == 42);
    assertTrue(floatArrayFS.get(3) == (float) (Float.MIN_VALUE * 1000.0));
    assertTrue(floatArrayFS.get(4) == Float.MIN_VALUE);

    ByteArrayFS byteArrayFS = (ByteArrayFS) fs.getFeatureValue(byteArrayFeature);
    assertTrue(byteArrayFS.get(0) == (byte) 8);
    assertTrue(byteArrayFS.get(1) == (byte) 16);
    assertTrue(byteArrayFS.get(2) == (byte) 64);
    assertTrue(byteArrayFS.get(3) == (byte) 128);
    assertTrue(byteArrayFS.get(4) == (byte) 255);

    BooleanArrayFS boolArrayFS = (BooleanArrayFS) fs.getFeatureValue(booleanArrayFeature);
    assertTrue(boolArrayFS.get(0));
    assertTrue(!boolArrayFS.get(1));
    assertTrue(boolArrayFS.get(2));
    assertTrue(!boolArrayFS.get(3));
    assertTrue(boolArrayFS.get(4));

    ShortArrayFS shortArrayFS = (ShortArrayFS) fs.getFeatureValue(shortArrayFeature);
    assertTrue(shortArrayFS.get(0) == Short.MAX_VALUE);
    assertTrue(shortArrayFS.get(1) == Short.MAX_VALUE - 1);
    assertTrue(shortArrayFS.get(2) == Short.MAX_VALUE - 2);
    assertTrue(shortArrayFS.get(3) == Short.MAX_VALUE - 3);
    assertTrue(shortArrayFS.get(4) == Short.MAX_VALUE - 4);

    LongArrayFS longArrayFS = (LongArrayFS) fs.getFeatureValue(longArrayFeature);
    assertTrue(longArrayFS.get(0) == Long.MAX_VALUE);
    assertTrue(longArrayFS.get(1) == Long.MAX_VALUE - 1);
    assertTrue(longArrayFS.get(2) == Long.MAX_VALUE - 2);
    assertTrue(longArrayFS.get(3) == Long.MAX_VALUE - 3);
    assertTrue(longArrayFS.get(4) == Long.MAX_VALUE - 4);

    DoubleArrayFS doubleArrayFS = (DoubleArrayFS) fs.getFeatureValue(doubleArrayFeature);
    assertTrue(doubleArrayFS.get(0) == Double.MAX_VALUE);
    assertTrue(doubleArrayFS.get(1) == Double.MIN_VALUE);
    assertTrue(doubleArrayFS.get(2) == Double.parseDouble("1.5555"));
    assertTrue(doubleArrayFS.get(3) == Double.parseDouble("99.000000005"));
    assertTrue(doubleArrayFS.get(4) == Double.parseDouble("4.44444444444444444"));

  }

  private void createExampleFS(CAS parmCas) throws Exception {
    // Create a view
    CAS englishView = parmCas.createView("EnglishDocument");
    // Set the document text
    englishView.setDocumentText("this beer is good");

    // create an FS of exampleType and index it
    AnnotationFS fs = englishView.createAnnotation(exampleType, 1, 5);

    // create Array FSs
    StringArrayFS strArrayFS = parmCas.createStringArrayFS(5);
    strArrayFS.set(0, "zzzzzz");
    strArrayFS.set(1, "yyyyyy");
    strArrayFS.set(2, "xxxxxx");
    strArrayFS.set(3, "wwwwww");
    strArrayFS.set(4, "vvvvvv");

    IntArrayFS intArrayFS = parmCas.createIntArrayFS(5);
    intArrayFS.set(0, Integer.MAX_VALUE);
    intArrayFS.set(1, Integer.MAX_VALUE - 1);
    intArrayFS.set(2, 42);
    intArrayFS.set(3, Integer.MIN_VALUE + 1);
    intArrayFS.set(4, Integer.MIN_VALUE);

    FloatArrayFS floatArrayFS = parmCas.createFloatArrayFS(5);
    floatArrayFS.set(0, Float.MAX_VALUE);
    floatArrayFS.set(1, (float) (Float.MAX_VALUE / 1000.0));
    floatArrayFS.set(2, 42);
    floatArrayFS.set(3, (float) (Float.MIN_VALUE * 1000.0));
    floatArrayFS.set(4, Float.MIN_VALUE);

    ByteArrayFS byteArrayFS = parmCas.createByteArrayFS(5);
    byteArrayFS.set(0, (byte) 8);
    byteArrayFS.set(1, (byte) 16);
    byteArrayFS.set(2, (byte) 64);
    byteArrayFS.set(3, (byte) 128);
    byteArrayFS.set(4, (byte) 255);

    BooleanArrayFS boolArrayFS = parmCas.createBooleanArrayFS(20);
    boolean val = false;
    for (int i = 0; i < 20; i++) {
      boolArrayFS.set(i, val = !val);
    }

    ShortArrayFS shortArrayFS = parmCas.createShortArrayFS(5);
    shortArrayFS.set(0, Short.MAX_VALUE);
    shortArrayFS.set(1, (short) (Short.MAX_VALUE - 1));
    shortArrayFS.set(2, (short) (Short.MAX_VALUE - 2));
    shortArrayFS.set(3, (short) (Short.MAX_VALUE - 3));
    shortArrayFS.set(4, (short) (Short.MAX_VALUE - 4));

    LongArrayFS longArrayFS = parmCas.createLongArrayFS(5);
    longArrayFS.set(0, Long.MAX_VALUE);
    longArrayFS.set(1, Long.MAX_VALUE - 1);
    longArrayFS.set(2, Long.MAX_VALUE - 2);
    longArrayFS.set(3, Long.MAX_VALUE - 3);
    longArrayFS.set(4, Long.MAX_VALUE - 4);

    DoubleArrayFS doubleArrayFS = parmCas.createDoubleArrayFS(5);
    doubleArrayFS.set(0, Double.MAX_VALUE);
    doubleArrayFS.set(1, Double.MIN_VALUE);
    doubleArrayFS.set(2, Double.parseDouble("1.5555"));
    doubleArrayFS.set(3, Double.parseDouble("99.000000005"));
    doubleArrayFS.set(4, Double.parseDouble("4.44444444444444444"));

    // set features of fs
    fs.setStringValue(stringFeature, "aaaaaaa");
    fs.setFloatValue(floatFeature, (float) 99.99);

    fs.setFeatureValue(intArrayFeature, intArrayFS);
    fs.setFeatureValue(floatArrayFeature, floatArrayFS);
    fs.setFeatureValue(stringArrayFeature, strArrayFS);

    // fs.setByteValue(byteFeature, Byte.MAX_VALUE);
    fs.setByteValue(byteFeature, (byte) 'z');
    fs.setFeatureValue(byteArrayFeature, byteArrayFS);
    fs.setBooleanValue(booleanFeature, true);
    fs.setFeatureValue(booleanArrayFeature, boolArrayFS);
    fs.setShortValue(shortFeature, Short.MIN_VALUE);
    fs.setFeatureValue(shortArrayFeature, shortArrayFS);
    fs.setLongValue(longFeature, Long.MIN_VALUE);
    fs.setFeatureValue(longArrayFeature, longArrayFS);
    fs.setDoubleValue(doubleFeature, Double.MAX_VALUE);
    fs.setFeatureValue(doubleArrayFeature, doubleArrayFS);
    
    englishView.getIndexRepository().addFS(fs);
  }

  public void testNewPrimitiveTypeKeys() throws Exception {
    // Create FS with features set in reverse order
    for (int i=0; i<5; i++) {
      AnnotationFS fs = cas.createAnnotation(exampleType, 0, 0);
      fs.setDoubleValue(doubleFeature, 5.0 - i );
      fs.setLongValue(longFeature, (long)1+i );
      fs.setShortValue(shortFeature, (short)(5 - i) );
      cas.getIndexRepository().addFS(fs);
    }

    // test double as key
    FSIterator iter = cas.getIndexRepository().getIndex("doubleIndex", exampleType).iterator();
//    System.out.println("\nDouble");
    for (int i=0; i<5; i++) {
      AnnotationFS testfs = (AnnotationFS)iter.get();
//      System.out.println("exampleType has double=" + testfs.getDoubleValue(doubleFeature)
//              + " long=" + testfs.getLongValue(longFeature)
//              + " short=" + testfs.getShortValue(shortFeature));
      assertTrue(1+i == testfs.getDoubleValue(doubleFeature));
      assertTrue(5-i == testfs.getLongValue(longFeature));
      assertTrue(1+i == testfs.getShortValue(shortFeature));
      iter.moveToNext();
    }

    // test long as key
    iter = cas.getIndexRepository().getIndex("longIndex", exampleType).iterator();
//    System.out.println("\nLong");
    for (int i=0; i<5; i++) {
      AnnotationFS testfs = (AnnotationFS)iter.get();
//      System.out.println("exampleType has double=" + testfs.getDoubleValue(doubleFeature)
//              + " long=" + testfs.getLongValue(longFeature)
//              + " short=" + testfs.getShortValue(shortFeature));
      assertTrue(1+i == testfs.getDoubleValue(doubleFeature));
      assertTrue(5-i == testfs.getLongValue(longFeature));
      assertTrue(1+i == testfs.getShortValue(shortFeature));
      iter.moveToNext();
    }

    // test short as key
    iter = cas.getIndexRepository().getIndex("shortIndex", exampleType).iterator();
//    System.out.println("\nShort");
    for (int i=0; i<5; i++) {
      AnnotationFS testfs = (AnnotationFS)iter.get();
//      System.out.println("exampleType has double=" + testfs.getDoubleValue(doubleFeature)
//              + " long=" + testfs.getLongValue(longFeature)
//              + " short=" + testfs.getShortValue(shortFeature));
      assertTrue(1+i == testfs.getDoubleValue(doubleFeature));
      assertTrue(5-i == testfs.getLongValue(longFeature));
      assertTrue(1+i == testfs.getShortValue(shortFeature));
      iter.moveToNext();
    }

  }

  // public void testUimaTypeSystem2Ecore() throws Exception
  // {
  // //register default resource factory
  // Resource.Factory.Registry.INSTANCE.
  // getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
  // //create Ecore Resource
  // ResourceSet resourceSet = new ResourceSetImpl();
  // URI outputURI = URI.createFileURI("test.ecore");
  // Resource outputResource = resourceSet.createResource(outputURI);
  //    
  // //Convert UIMA Type System to Ecore
  // TypeSystemDescription ts =
  // TypeSystemUtil.typeSystem2TypeSystemDescription(cas.getTypeSystem());
  // UimaTypeSystem2Ecore.uimaTypeSystem2Ecore(ts, outputResource, null);
  // //outputResource.save(null); //for debugging only
  // //Convert back
  // TypeSystemDescription result = Ecore2UimaTypeSystem.ecore2UimaTypeSystem(outputResource, null);
  // ts.toXML(System.out);
  // result.toXML(System.out); //for debugging only
  // //Should be the same
  // UimaTypeSystem2EcoreTest.assertTypeSystemsEqual(ts, result);
  //
  // //we need to do this to reset the the EMF package registry to its default state,
  // //otherwise subsequent tests won't be starting from scratch
  // EPackage.Registry.INSTANCE.clear();
  // EcorePackage ecore = EcorePackage.eINSTANCE;
  // EPackage.Registry.INSTANCE.put(ecore.getNsURI(), ecore);
  // }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(NewPrimitiveTypesTest.class);
  }

}
