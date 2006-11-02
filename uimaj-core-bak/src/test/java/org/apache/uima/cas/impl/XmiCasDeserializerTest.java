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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.SofaID;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.TCAS;
import org.apache.uima.impl.SofaID_impl;
import org.apache.uima.impl.resource.metadata.TypePriorities_impl;
import org.apache.uima.impl.resource.metadata.TypeSystemDescription_impl;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLSerializer;

/**
 * 
 * @author Adam Lally 
 */
public class XmiCasDeserializerTest extends TestCase
{

  private FsIndexDescription[] indexes;
  private TypeSystemDescription typeSystem;

  /**
   * Constructor for XCASDeserializerTest.
   * @param arg0
   */
  public XmiCasDeserializerTest(String arg0) throws IOException
  {
    super(arg0);
  }

  protected void setUp() throws Exception
  {
    File typeSystemFile = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
    File indexesFile = JUnitExtension.getFile("ExampleCas/kltIndexes.xml");

    typeSystem = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            new XMLInputSource(typeSystemFile));
    indexes = UIMAFramework.getXMLParser().parseFsIndexCollection(
            new XMLInputSource(indexesFile)).getFsIndexes();
  }
  public void testDeserializeAndReserialize() throws Exception
  {
    try
    {
      File tsWithNoMultiRefs = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
      doTestDeserializeAndReserialize(tsWithNoMultiRefs);
      File tsWithMultiRefs = JUnitExtension.getFile("ExampleCas/testTypeSystem_withMultiRefs.xml");
      doTestDeserializeAndReserialize(tsWithMultiRefs);
    }
    catch(Exception e)
    {
      JUnitExtension.handleException(e);
    }
  }
  
  private void doTestDeserializeAndReserialize(File typeSystemDescriptor) throws Exception
  {
    //deserialize a complex CAS from XCAS
    CAS cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);

    InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
    XCASDeserializer deser = new XCASDeserializer(cas.getTypeSystem());
    ContentHandler deserHandler = deser.getXCASHandler(cas);
    SAXParserFactory fact = SAXParserFactory.newInstance();
    SAXParser parser = fact.newSAXParser();
    XMLReader xmlReader = parser.getXMLReader();
    xmlReader.setContentHandler(deserHandler);
    xmlReader.parse(new InputSource(serCasStream));
    serCasStream.close();
    
    //reserialize as XMI
    StringWriter sw = new StringWriter();
    XMLSerializer xmlSer = new XMLSerializer(sw, false);
    XmiCasSerializer xmiSer = new XmiCasSerializer(cas.getTypeSystem());
    xmiSer.serialize(cas, xmlSer.getContentHandler());
    String xml = sw.getBuffer().toString();

    //deserialize into another CAS 
    CAS cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    XmiCasDeserializer deser2 = new XmiCasDeserializer(cas2.getTypeSystem());
    ContentHandler deserHandler2 = deser2.getXmiCasHandler(cas2);
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(new StringReader(xml)));
    
    //compare
    assertEquals(cas.getAnnotationIndex().size(), cas2.getAnnotationIndex().size());
    //CasComparer.assertEquals(tcas,tcas2);

    //check that array refs are not null
    Type entityType = cas2.getTypeSystem().getType("org.apache.uima.klt.Entity");
    Feature classesFeat = entityType.getFeatureByBaseName("classes");
    Iterator iter = cas2.getIndexRepository().getIndex("kltEntityIndex").iterator();
    assertTrue(iter.hasNext());
    while (iter.hasNext())
    {
      FeatureStructure fs = (FeatureStructure)iter.next();
      StringArrayFS arrayFS = (StringArrayFS) fs.getFeatureValue(classesFeat);
      assertNotNull(arrayFS);
      for (int i = 0; i < arrayFS.size(); i++)
      {
        assertNotNull(arrayFS.get(i));
      }
    }
    
    //test that lenient mode does not report errors 
    CAS cas3 = CasCreationUtils.createCas(new TypeSystemDescription_impl(), new TypePriorities_impl(),
        new FsIndexDescription[0]);
    XmiCasDeserializer deser3 = new XmiCasDeserializer(cas3.getTypeSystem());
    ContentHandler deserHandler3 = deser3.getXmiCasHandler(cas3,true);
    xmlReader.setContentHandler(deserHandler3);
    xmlReader.parse(new InputSource(new StringReader(xml)));        
  }
 
  public void testMultipleSofas() throws Exception
  {
    try
    {
      CAS cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
          new FsIndexDescription[0]);
      //set document text for the initial view
      cas.setDocumentText("This is a test");
      //create a new view and set its document text
      CAS cas2 = cas.createView("OtherSofa");
      cas2.setDocumentText("This is only a test");
      
      //create an annotation and add to index of both views
      AnnotationFS anAnnot = cas.createAnnotation(cas.getAnnotationType(), 0, 5);
      cas.getIndexRepository().addFS(anAnnot);
      cas2.getIndexRepository().addFS(anAnnot);
      FSIndex tIndex = cas.getAnnotationIndex();
      FSIndex t2Index = cas2.getAnnotationIndex();
      assertTrue(tIndex.size() == 2); // document annot and this one
      assertTrue(t2Index.size() == 2);  // ditto
          
      //serialize
      StringWriter sw = new StringWriter();
      XMLSerializer xmlSer = new XMLSerializer(sw, false);
      XmiCasSerializer xmiSer = new XmiCasSerializer(cas.getTypeSystem());
      xmiSer.serialize(cas, xmlSer.getContentHandler());
      String xml = sw.getBuffer().toString();
  
      //deserialize into another CAS (repeat twice to check it still works after reset)
      CAS newCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
          new FsIndexDescription[0]);
      for (int i = 0; i < 2; i++)
      {
        XmiCasDeserializer newDeser = new XmiCasDeserializer(newCas.getTypeSystem());
        ContentHandler newDeserHandler = newDeser.getXmiCasHandler(newCas);
        SAXParserFactory fact = SAXParserFactory.newInstance();
        SAXParser parser = fact.newSAXParser();
        XMLReader xmlReader = parser.getXMLReader();
        xmlReader.setContentHandler(newDeserHandler);
        xmlReader.parse(new InputSource(new StringReader(xml)));
    
        //check sofas
        assertEquals("This is a test", newCas.getDocumentText());
        CAS newCas2 = newCas.getView("OtherSofa");
        assertEquals("This is only a test", newCas2.getDocumentText());

        // check that annotation is still indexed in both views
        assertTrue(tIndex.size() == 2); // document annot and this one
        assertTrue(t2Index.size() == 2);  // ditto
        
        newCas.reset(); 
      }    
      
      //test same thing but with a TCAS
      TCAS newTcas = CasCreationUtils.createTCas(typeSystem, new TypePriorities_impl(),
          new FsIndexDescription[0]);
      for (int i = 0; i < 2; i++)
      {
        XmiCasDeserializer newDeser = new XmiCasDeserializer(newTcas.getTypeSystem());
        ContentHandler newDeserHandler = newDeser.getXmiCasHandler(newTcas);
        SAXParserFactory fact = SAXParserFactory.newInstance();
        SAXParser parser = fact.newSAXParser();
        XMLReader xmlReader = parser.getXMLReader();
        xmlReader.setContentHandler(newDeserHandler);
        xmlReader.parse(new InputSource(new StringReader(xml)));
    
        //check sofas
        assertEquals("This is a test", newTcas.getDocumentText());
        CAS newTcas2 = newTcas.getTCAS(newTcas.getSofa(new SofaID_impl("OtherSofa")));
        assertEquals("This is only a test", newTcas2.getDocumentText());
        newTcas.reset(); 
      }
    }
    catch(Exception e)
    {
      JUnitExtension.handleException(e);
    }
  }
  
  public void testTypeSystemFiltering() throws Exception
  {
    try
    {
      //deserialize a complex CAS from XCAS
      CAS cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);

      InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
      XCASDeserializer deser = new XCASDeserializer(cas.getTypeSystem());
      ContentHandler deserHandler = deser.getXCASHandler(cas);
      SAXParserFactory fact = SAXParserFactory.newInstance();
      SAXParser parser = fact.newSAXParser();
      XMLReader xmlReader = parser.getXMLReader();
      xmlReader.setContentHandler(deserHandler);
      xmlReader.parse(new InputSource(serCasStream));
      serCasStream.close();
      
      //now read in a TypeSystem that's a subset of those types
      TypeSystemDescription partialTypeSystemDesc = UIMAFramework.getXMLParser().
        parseTypeSystemDescription(new XMLInputSource(JUnitExtension.getFile("ExampleCas/partialTestTypeSystem.xml")));
      TypeSystem partialTypeSystem =
        CasCreationUtils.createCas(partialTypeSystemDesc,null,null).getTypeSystem();
      
      //reserialize as XMI, filtering out anything that doesn't fit in the
      //partialTypeSystem
      StringWriter sw = new StringWriter();
      XMLSerializer xmlSer = new XMLSerializer(sw, false);
      XmiCasSerializer xmiSer = new XmiCasSerializer(partialTypeSystem);
      xmiSer.serialize(cas, xmlSer.getContentHandler());
      String xml = sw.getBuffer().toString();
      //System.out.println(xml);

      //deserialize into another CAS (which has the whole type system)
      CAS cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
      XmiCasDeserializer deser2 = new XmiCasDeserializer(cas2.getTypeSystem());
      ContentHandler deserHandler2 = deser2.getXmiCasHandler(cas2);
      xmlReader.setContentHandler(deserHandler2);
      xmlReader.parse(new InputSource(new StringReader(xml)));
      
      //check that types have been filtered out
      Type orgType = cas2.getTypeSystem().getType("org.apache.hutt.Organization");
      assertNotNull(orgType);
      assertTrue(cas2.getAnnotationIndex(orgType).size() == 0);
      assertTrue(cas.getAnnotationIndex(orgType).size() > 0);
      
      //but that some types are still there
      Type personType = cas2.getTypeSystem().getType("org.apache.hutt.Person");
      FSIndex personIndex = cas2.getAnnotationIndex(personType);
      assertTrue(personIndex.size() > 0);
      
      //check that mentionType has been filtered out (set to null)
      FeatureStructure somePlace = personIndex.iterator().get();
      Feature mentionTypeFeat = personType.getFeatureByBaseName("mentionType");
      assertNotNull(mentionTypeFeat);
      assertNull(somePlace.getStringValue(mentionTypeFeat));     
    }
    catch (Exception e)
    {
      JUnitExtension.handleException(e);
    }
  }

  public void testNoInitialSofa() throws Exception
  {
    CAS cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), new FsIndexDescription[0]);
    // create non-annotation type so as not to create the _InitialView Sofa
    IntArrayFS intArrayFS = cas.createIntArrayFS(5);
    intArrayFS.set(0,1);
    intArrayFS.set(1,2);
    intArrayFS.set(2,3);  
    intArrayFS.set(3,4);  
    intArrayFS.set(4,5);  
    cas.getIndexRepository().addFS(intArrayFS);
  
    // serialize the CAS
    StringWriter sw = new StringWriter();
    XMLSerializer xmlSer = new XMLSerializer(sw, false);
    XmiCasSerializer xmiSer = new XmiCasSerializer(cas.getTypeSystem());
    xmiSer.serialize(cas, xmlSer.getContentHandler());
    String xml = sw.getBuffer().toString();

    //deserialize into another CAS 
    CAS cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), new FsIndexDescription[0]);

    XmiCasDeserializer deser2 = new XmiCasDeserializer(cas2.getTypeSystem());
    ContentHandler deserHandler2 = deser2.getXmiCasHandler(cas2);
    SAXParserFactory fact = SAXParserFactory.newInstance();
    SAXParser parser = fact.newSAXParser();
    XMLReader xmlReader = parser.getXMLReader();
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(new StringReader(xml)));
    
    // serialize the new CAS
    sw = new StringWriter();
    xmlSer = new XMLSerializer(sw, false);
    xmiSer = new XmiCasSerializer(cas2.getTypeSystem());
    xmiSer.serialize(cas2, xmlSer.getContentHandler());
    String xml2 = sw.getBuffer().toString();
    
    //simple compare for now
//TODO when default indexing is available, validate the integer array explicitly 
    assertTrue(xml2.equals(xml));
  }
  
  public void testv1FormatXcas() throws Exception
  {
    CAS cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
        new FsIndexDescription[0]);
    CAS v1cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
        new FsIndexDescription[0]);

    // get a complex CAS 
    InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
    XCASDeserializer deser = new XCASDeserializer(cas.getTypeSystem());
    ContentHandler deserHandler = deser.getXCASHandler(cas);
    SAXParserFactory fact = SAXParserFactory.newInstance();
    SAXParser parser = fact.newSAXParser();
    XMLReader xmlReader = parser.getXMLReader();
    xmlReader.setContentHandler(deserHandler);
    xmlReader.parse(new InputSource(serCasStream));
    serCasStream.close();

    //test it
    assertTrue(CAS.NAME_DEFAULT_SOFA.equals(cas.getSofa().getSofaID()));
    
    // get a v1 XMI version of the same CAS
    serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/v1xmiCas.xml"));
    XmiCasDeserializer deser2 = new XmiCasDeserializer(v1cas.getTypeSystem());
    ContentHandler deserHandler2 = deser2.getXmiCasHandler(v1cas);
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(serCasStream));
    serCasStream.close();
    
    //compare
    assertEquals(cas.getAnnotationIndex().size(), v1cas.getAnnotationIndex().size());
    assertTrue(CAS.NAME_DEFAULT_SOFA.equals(v1cas.getSofa().getSofaID()));

    // now a v1 XMI version of a multiple Sofa CAS
    v1cas.reset();
    serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/xmiMsCasV1.xml"));
    deser2 = new XmiCasDeserializer(v1cas.getTypeSystem());
    deserHandler2 = deser2.getXmiCasHandler(v1cas);
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(serCasStream));
    serCasStream.close();
    
    // test it
    CAS engView = v1cas.getView("EnglishDocument");
    assertTrue(engView.getDocumentText().equals("this beer is good"));
    assertTrue(engView.getAnnotationIndex().size() == 5);  // 4 annots plus documentAnnotation
    CAS gerView = v1cas.getView("GermanDocument");
    assertTrue(gerView.getDocumentText().equals("das bier ist gut"));
    assertTrue(gerView.getAnnotationIndex().size() == 5);  // 4 annots plus documentAnnotation
    assertTrue(CAS.NAME_DEFAULT_SOFA.equals(v1cas.getSofa().getSofaID()));
    assertTrue(v1cas.getDocumentText().equals("some text for the default text sofa."));

    //reserialize as XMI
    StringWriter sw = new StringWriter();
    XMLSerializer xmlSer = new XMLSerializer(sw, false);
    XmiCasSerializer xmiSer = new XmiCasSerializer(v1cas.getTypeSystem());
    xmiSer.serialize(v1cas, xmlSer.getContentHandler());
    String xml = sw.getBuffer().toString();

    cas.reset();
    
    //deserialize into another CAS 
    deser2 = new XmiCasDeserializer(cas.getTypeSystem());
    deserHandler2 = deser2.getXmiCasHandler(cas);
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(new StringReader(xml)));

    // test it
    engView = cas.getView("EnglishDocument");
    assertTrue(engView.getDocumentText().equals("this beer is good"));
    assertTrue(engView.getAnnotationIndex().size() == 5);  // 4 annots plus documentAnnotation
    gerView = cas.getView("GermanDocument");
    assertTrue(gerView.getDocumentText().equals("das bier ist gut"));
    assertTrue(gerView.getAnnotationIndex().size() == 5);  // 4 annots plus documentAnnotation
    assertTrue(CAS.NAME_DEFAULT_SOFA.equals(v1cas.getSofa().getSofaID()));
    assertTrue(v1cas.getDocumentText().equals("some text for the default text sofa."));
  }
}
