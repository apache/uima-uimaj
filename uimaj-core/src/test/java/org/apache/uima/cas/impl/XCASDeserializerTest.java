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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;


public class XCASDeserializerTest extends TestCase {

  private TypeSystemDescription typeSystem;

  private FsIndexDescription[] indexes;

  /**
   * Constructor for XCASDeserializerTest.
   * 
   * @param arg0
   */
  public XCASDeserializerTest(String arg0) {
    super(arg0);
  }

  protected void setUp() throws Exception {
    File typeSystemFile = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
    File indexesFile = JUnitExtension.getFile("ExampleCas/testIndexes.xml");

    typeSystem = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            new XMLInputSource(typeSystemFile));
    indexes = UIMAFramework.getXMLParser().parseFsIndexCollection(new XMLInputSource(indexesFile))
            .getFsIndexes();
  }

  public void testNoInitialSofa() throws Exception {

    CAS cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    // create non-annotation type so as not to create the _InitialView Sofa
    IntArrayFS intArrayFS = cas.createIntArrayFS(5);
    intArrayFS.set(0, 1);
    intArrayFS.set(1, 2);
    intArrayFS.set(2, 3);
    intArrayFS.set(3, 4);
    intArrayFS.set(4, 5);
    cas.getIndexRepository().addFS(intArrayFS);

    // serialize the CAS
    StringWriter sw = new StringWriter();
    XMLSerializer xmlSer = new XMLSerializer(sw, false);
    XCASSerializer xcasSer = new XCASSerializer(cas.getTypeSystem());
    xcasSer.serialize(cas, xmlSer.getContentHandler(), true);
    String xml = sw.getBuffer().toString();

    // deserialize into another CAS
    CAS cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    XCASDeserializer deser = new XCASDeserializer(cas2.getTypeSystem());
    ContentHandler deserHandler = deser.getXCASHandler(cas2);
    SAXParserFactory fact = SAXParserFactory.newInstance();
    SAXParser parser = fact.newSAXParser();
    XMLReader xmlReader = parser.getXMLReader();
    xmlReader.setContentHandler(deserHandler);
    xmlReader.parse(new InputSource(new StringReader(xml)));

    // serialize the new CAS
    sw = new StringWriter();
    xmlSer = new XMLSerializer(sw, false);
    xcasSer = new XCASSerializer(cas.getTypeSystem());
    xcasSer.serialize(cas2, xmlSer.getContentHandler(), true);
    String xml2 = sw.getBuffer().toString();

    // compare
    assertTrue(xml2.equals(xml));
  }

  public void testDeserializeAndReserialize() throws Exception {
    doTestDeserializeAndReserialize(false);
    doTestDeserializeAndReserialize(true);
  }

  private void doTestDeserializeAndReserialize(boolean useJCas) throws Exception {
    // deserialize a complex CAS
    CAS cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    if (useJCas) {
      cas.getJCas();
    }
    InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
    XCASDeserializer deser = new XCASDeserializer(cas.getTypeSystem());
    ContentHandler deserHandler = deser.getXCASHandler(cas);
    SAXParserFactory fact = SAXParserFactory.newInstance();
    SAXParser parser = fact.newSAXParser();
    XMLReader xmlReader = parser.getXMLReader();
    xmlReader.setContentHandler(deserHandler);
    xmlReader.parse(new InputSource(serCasStream));
    serCasStream.close();

    // check that array refs are not null
    Type entityType = cas.getTypeSystem().getType("org.apache.uima.testTypeSystem.Entity");
    Feature classesFeat = entityType.getFeatureByBaseName("classes");
    Iterator<FeatureStructure> iter = cas.getIndexRepository().getIndex("testEntityIndex").iterator();
    assertTrue(iter.hasNext());
    while (iter.hasNext()) {
      FeatureStructure fs = iter.next();
      StringArrayFS arrayFS = (StringArrayFS) fs.getFeatureValue(classesFeat);
      assertNotNull(arrayFS);
      for (int i = 0; i < arrayFS.size(); i++) {
        assertNotNull(arrayFS.get(i));
      }
    }

    // reserialize
    StringWriter sw = new StringWriter();
    XMLSerializer xmlSer = new XMLSerializer(sw, false);
    XCASSerializer xcasSer = new XCASSerializer(cas.getTypeSystem());
    xcasSer.serialize(cas, xmlSer.getContentHandler(), true);
    String xml = sw.getBuffer().toString();

    CAS cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    if (useJCas) {
      cas2.getJCas();
    }

    // deserialize into another CAS
    XCASDeserializer deser2 = new XCASDeserializer(cas2.getTypeSystem());
    ContentHandler deserHandler2 = deser2.getXCASHandler(cas2);
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(new StringReader(xml)));

    // compare
    assertEquals(cas.getAnnotationIndex().size(), cas2.getAnnotationIndex().size());
    // CasComparer.assertEquals(cas,cas2);
  }

  public void testOutOfTypeSystem2() throws Exception {
    // deserialize a complex CAS into one with no TypeSystem
    CAS cas = CasCreationUtils.createCas(new TypeSystemDescription_impl(),
            new TypePriorities_impl(), new FsIndexDescription[0]);
    OutOfTypeSystemData ootsd = new OutOfTypeSystemData();
    InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
    XCASDeserializer deser = new XCASDeserializer(cas.getTypeSystem());
    ContentHandler deserHandler = deser.getXCASHandler(cas, ootsd);
    SAXParserFactory fact = SAXParserFactory.newInstance();
    SAXParser parser = fact.newSAXParser();
    XMLReader xmlReader = parser.getXMLReader();
    xmlReader.setContentHandler(deserHandler);
    xmlReader.parse(new InputSource(serCasStream));
    serCasStream.close();

    // now reserialize including OutOfTypeSystem data
    XCASSerializer xcasSer = new XCASSerializer(cas.getTypeSystem());
    StringWriter sw = new StringWriter();
    XMLSerializer xmlSer = new XMLSerializer(sw, false);
    xcasSer.serialize(cas, xmlSer.getContentHandler(), true, ootsd);
    String xml = sw.getBuffer().toString();
//    FileUtils.saveString2File(xml, new File("c:/temp/xmlv2.xml"));
//    System.out.println(xml);

    // deserialize into a CAS that accepts the full typesystem
    CAS cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    XCASDeserializer deser2 = new XCASDeserializer(cas2.getTypeSystem());
    ContentHandler deserHandler2 = deser2.getXCASHandler(cas2);
    xmlReader = parser.getXMLReader();
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(new StringReader(xml)));

    // check that array refs are not null
    Type entityType = cas2.getTypeSystem().getType("org.apache.uima.testTypeSystem.Entity");
    Feature classesFeat = entityType.getFeatureByBaseName("classes");
    Iterator<FeatureStructure> iter = cas2.getIndexRepository().getIndex("testEntityIndex").iterator();
    assertTrue(iter.hasNext());
    while (iter.hasNext()) {
      FeatureStructure fs = iter.next();
      StringArrayFS arrayFS = (StringArrayFS) fs.getFeatureValue(classesFeat);
      assertNotNull(arrayFS);
      for (int i = 0; i < arrayFS.size(); i++) {
        assertNotNull(arrayFS.get(i));
      }
    }
  }

  public void testOutOfTypeSystem3() throws Exception {
    // deserialize an XCAS using the implicit value feature into a CAS with no TypeSystem
    CAS cas = CasCreationUtils.createCas(new TypeSystemDescription_impl(),
            new TypePriorities_impl(), new FsIndexDescription[0]);
    String xcas = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CAS>"
            + "<uima.tcas.Document _content=\"text\">Test Document</uima.tcas.Document>"
            + "<uima.tcas.DocumentAnnotation _indexed=\"1\" _id=\"8\" sofa=\"1\" begin=\"0\" end=\"13\" language=\"en\"/>"
            + "<foo.Bar _indexed=\"1\" _id=\"2\" sofa=\"1\" begin=\"0\" end=\"0\" baz=\"blah\">this is the value feature</foo.Bar></CAS>";
    OutOfTypeSystemData ootsd = new OutOfTypeSystemData();
    XMLReader xmlReader = XMLUtils.createXMLReader();
    XCASDeserializer deser = new XCASDeserializer(cas.getTypeSystem());
    ContentHandler handler = deser.getXCASHandler(cas, ootsd);
    xmlReader.setContentHandler(handler);
    xmlReader.parse(new InputSource(new StringReader(xcas)));

    // now reserialize including OutOfTypeSystem data
    XCASSerializer xcasSer = new XCASSerializer(cas.getTypeSystem());
    StringWriter sw = new StringWriter();
    XMLSerializer xmlSer = new XMLSerializer(sw, false);
    xcasSer.serialize(cas, xmlSer.getContentHandler(), true, ootsd);
    String xml = sw.getBuffer().toString();
    // System.out.println(xml);

    // make sure the value feature was not lost (it will be serialized as an attribute however)
    assertTrue(xml.indexOf("value=\"this is the value feature\"") != -1);
  }

  public void testMultipleSofas() throws Exception {
    CAS cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    // set document text for the initial view
    cas.setDocumentText("This is a test");
    // create a new view and set its document text
    CAS cas2 = cas.createView("OtherSofa");
    cas2.setDocumentText("This is only a test");

    // Change this test to create an instance of TOP because you cannot add an annotation to other than 
    //   the view it is created in. https://issues.apache.org/jira/browse/UIMA-4099
    // create a TOP and add to index of both views
    Type topType = cas.getTypeSystem().getTopType();
    FeatureStructure aTOP = cas.createFS(topType);
    cas.getIndexRepository().addFS(aTOP);
    cas2.getIndexRepository().addFS(aTOP); 
    FSIterator<FeatureStructure> it = cas.getIndexRepository().getAllIndexedFS(topType);
    FSIterator<FeatureStructure> it2 = cas2.getIndexRepository().getAllIndexedFS(topType);
    it.next(); it.next();
    it2.next(); it2.next(); 
    assertFalse(it.hasNext());
    assertFalse(it2.hasNext());
     
    // serialize
    StringWriter sw = new StringWriter();
    XMLSerializer xmlSer = new XMLSerializer(sw, false);
    XCASSerializer xcasSer = new XCASSerializer(cas.getTypeSystem());
    xcasSer.serialize(cas, xmlSer.getContentHandler(), true);
    String xml = sw.getBuffer().toString();

    // deserialize into another CAS (repeat twice to check it still works after reset)
    CAS newCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    for (int i = 0; i < 2; i++) {
      XCASDeserializer newDeser = new XCASDeserializer(newCas.getTypeSystem());
      ContentHandler newDeserHandler = newDeser.getXCASHandler(newCas);
      SAXParserFactory fact = SAXParserFactory.newInstance();
      SAXParser parser = fact.newSAXParser();
      XMLReader xmlReader = parser.getXMLReader();
      xmlReader.setContentHandler(newDeserHandler);
      xmlReader.parse(new InputSource(new StringReader(xml)));

      // check sofas
      assertEquals("This is a test", newCas.getDocumentText());
      CAS newCas2 = newCas.getView("OtherSofa");
      assertEquals("This is only a test", newCas2.getDocumentText());

      // check that annotation is still indexed in both views
      it = newCas.getIndexRepository().getAllIndexedFS(topType);
      it2 = newCas2.getIndexRepository().getAllIndexedFS(topType);
      it.next(); it.next();
      it2.next(); it2.next(); 
      assertFalse(it.hasNext());
      assertFalse(it2.hasNext());
//      assertTrue(tIndex.size() == 2); // document annot and this one
//      assertTrue(t2Index.size() == 2); // ditto
      newCas.reset();  // testing if works after cas reset, go around loop 2nd time
    }
  }

  public void testv1FormatXcas() throws Exception {
    CAS cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    CAS v1cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);

    // get the CAS used above that is in v2.0 format
    InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
    XCASDeserializer deser = new XCASDeserializer(cas.getTypeSystem());
    ContentHandler deserHandler = deser.getXCASHandler(cas);
    SAXParserFactory fact = SAXParserFactory.newInstance();
    SAXParser parser = fact.newSAXParser();
    XMLReader xmlReader = parser.getXMLReader();
    xmlReader.setContentHandler(deserHandler);
    xmlReader.parse(new InputSource(serCasStream));
    serCasStream.close();

    // get a v1.x version of the same CAS
    serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/v1cas.xml"));
    deser = new XCASDeserializer(v1cas.getTypeSystem());
    deserHandler = deser.getXCASHandler(v1cas);
    fact = SAXParserFactory.newInstance();
    parser = fact.newSAXParser();
    xmlReader = parser.getXMLReader();
    xmlReader.setContentHandler(deserHandler);
    xmlReader.parse(new InputSource(serCasStream));
    serCasStream.close();

    // compare
    assertEquals(cas.getAnnotationIndex().size(), v1cas.getAnnotationIndex().size());

    // now a v1.x version of a multiple Sofa CAS
    v1cas.reset();
    serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/v1MultiSofaCas.xml"));
    deser = new XCASDeserializer(v1cas.getTypeSystem());
    deserHandler = deser.getXCASHandler(v1cas);
    fact = SAXParserFactory.newInstance();
    parser = fact.newSAXParser();
    xmlReader = parser.getXMLReader();
    xmlReader.setContentHandler(deserHandler);
    xmlReader.parse(new InputSource(serCasStream));
    serCasStream.close();

    // test it
    assertTrue(v1cas.getDocumentText().equals("some text for the default text sofa."));
    CAS engView = v1cas.getView("EnglishDocument");
    assertTrue(engView.getDocumentText().equals("this beer is good"));
    assertTrue(engView.getAnnotationIndex().size() == 5); // 4 annots plus documentAnnotation
    CAS gerView = v1cas.getView("GermanDocument");
    assertTrue(gerView.getDocumentText().equals("das bier ist gut"));
    assertTrue(gerView.getAnnotationIndex().size() == 5); // 4 annots plus documentAnnotation

    // reserialize
    StringWriter sw = new StringWriter();
    XMLSerializer xmlSer = new XMLSerializer(sw, false);
    XCASSerializer xcasSer = new XCASSerializer(v1cas.getTypeSystem());
    xcasSer.serialize(v1cas, xmlSer.getContentHandler(), true);
    String xml = sw.getBuffer().toString();

    // deserialize into another CAS
    cas.reset();
    XCASDeserializer deser2 = new XCASDeserializer(cas.getTypeSystem());
    ContentHandler deserHandler2 = deser2.getXCASHandler(cas);
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(new StringReader(xml)));

    // test it
    assertTrue(v1cas.getDocumentText().equals("some text for the default text sofa."));
    engView = cas.getView("EnglishDocument");
    assertTrue(engView.getDocumentText().equals("this beer is good"));
    assertTrue(engView.getAnnotationIndex().size() == 5); // 4 annots plus documentAnnotation
    gerView = cas.getView("GermanDocument");
    assertTrue(gerView.getDocumentText().equals("das bier ist gut"));
    assertTrue(gerView.getAnnotationIndex().size() == 5); // 4 annots plus documentAnnotation
  }
  
  public void testStringArrayWithNullValues() throws Exception {
    CAS cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    StringArrayFS strArray = cas.createStringArrayFS(3);
    strArray.set(1, "value");
    cas.getIndexRepository().addFS(strArray);
    
    assertEquals(null, strArray.get(0));
    assertEquals("value", strArray.get(1));
    assertEquals(null, strArray.get(2));
    
    //serialize to XCAS and back
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    XCASSerializer.serialize(cas,baos);
    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    XCASDeserializer.deserialize(bais, cas);

    //check
    Iterator<StringArray> iter = cas.getIndexRepository().getAllIndexedFS(cas.getTypeSystem().getType("uima.cas.StringArray"));
    StringArrayFS strArrayOut = (StringArrayFS)iter.next();
    assertEquals(null, strArrayOut.get(0));
    assertEquals("value", strArrayOut.get(1));
    assertEquals(null, strArrayOut.get(2));
  }
}
