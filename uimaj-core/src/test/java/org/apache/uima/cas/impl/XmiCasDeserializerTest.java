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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.Marker;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.XmiSerializationSharedData.OotsElementData;
import org.apache.uima.cas.impl.XmiSerializationSharedData.XmiArrayElement;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas_data.impl.CasComparer;
import org.apache.uima.internal.util.MultiThreadUtils;
import org.apache.uima.internal.util.MultiThreadUtils.ThreadM;
import org.apache.uima.internal.util.XmlAttribute;
import org.apache.uima.internal.util.XmlElementNameAndContents;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.EmptyIntegerList;
import org.apache.uima.jcas.cas.EmptyStringList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.IntegerList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCopier;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;


public class XmiCasDeserializerTest extends TestCase {

  private FsIndexDescription[] indexes;

  private TypeSystemDescription typeSystem;

  /**
   * Constructor for XmiCasDeserializerTest.
   * 
   * @param arg0
   */
  public XmiCasDeserializerTest(String arg0) throws IOException {
    super(arg0);
  }

  protected void setUp() throws Exception {
    File typeSystemFile = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
    File indexesFile = JUnitExtension.getFile("ExampleCas/testIndexes.xml");

    // large type system
    typeSystem = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            new XMLInputSource(typeSystemFile));
    
    // bag index for Entities and Relations
    indexes = UIMAFramework.getXMLParser().parseFsIndexCollection(new XMLInputSource(indexesFile))
            .getFsIndexes();
  }

  /**
   * test case for https://issues.apache.org/jira/projects/UIMA/issues/UIMA-5558
   * @throws Exception
   */
  public void testSerialize_with_0_length_array() throws Exception {
    
    TypeSystemDescription typeSystemDescription = UIMAFramework.getXMLParser().parseTypeSystemDescription(
        new XMLInputSource(JUnitExtension.getFile("ExampleCas/testTypeSystem_small_withoutMultiRefs.xml")));
    CAS cas = CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(), null);
    
    TypeSystem ts = cas.getTypeSystem();
    Type refType = ts.getType("RefType");  // super is Annotation
    Feature ref = refType.getFeatureByBaseName("ref");
    Feature ref_StringArray = refType.getFeatureByBaseName("ref_StringArray");
    Feature ref_StringList = refType.getFeatureByBaseName("ref_StringList");
    JCas jcas = cas.getJCas();
    
    String xml;
    
 // deserialize into another CAS
    SAXParserFactory fact = SAXParserFactory.newInstance();
    SAXParser parser = fact.newSAXParser();
    XMLReader xmlReader = parser.getXMLReader();
    
    CAS cas2 = CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(), null);
    XmiCasDeserializer deser2;
    ContentHandler deserHandler2;
    
    {  
      StringArray stringArray = new StringArray(jcas, 0);
      
      
      AnnotationFS fsRef = cas.createAnnotation(refType, 0, 0);
      fsRef.setFeatureValue(ref_StringArray, stringArray);
      cas.addFsToIndexes(fsRef);                    // gets serialized in=place
            
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
      AnnotationFS fs2 = cas2.getAnnotationIndex(refType).iterator().get();
      StringArrayFS fsa2 = (StringArrayFS) fs2.getFeatureValue(ref_StringArray);
      assertEquals(0, fsa2.size());     
     }
    
    // ------- repeat with lists in place of arrays --------------
    
    cas.reset();

    StringList stringlist0 = new EmptyStringList(jcas);
    
//    fsarray.addToIndexes(); // if added to indexes, forces serialization of FSArray as an element
    
    AnnotationFS fsRef = cas.createAnnotation(refType, 0, 0);
    fsRef.setFeatureValue(ref_StringList, stringlist0);
    cas.addFsToIndexes(fsRef);                    // gets serialized in=place
        
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
    AnnotationFS fs2 = cas2.getAnnotationIndex(refType).iterator().get();
    FeatureStructure fsl2 = fs2.getFeatureValue(ref_StringList);
    assertTrue(fsl2.getType().getShortName().equals("EmptyStringList"));
    
  }
  
  /**
   * test case for https://issues.apache.org/jira/browse/UIMA-5532
   * @throws Exception
   */
  public void testSerialize_withPartialMultiRefs() throws Exception {
   ;
    TypeSystemDescription typeSystemDescription = UIMAFramework.getXMLParser().parseTypeSystemDescription(
        new XMLInputSource(JUnitExtension.getFile("ExampleCas/testTypeSystem_small_withMultiRefs.xml")));
    CAS cas = CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(), null);
    
    TypeSystem ts = cas.getTypeSystem();
    Type refType = ts.getType("RefType");
    Feature ref = refType.getFeatureByBaseName("ref");
    Feature ref_FSArray = refType.getFeatureByBaseName("ref_FSArray");
    Feature ref_FSList = refType.getFeatureByBaseName("ref_FSList");
    
    JCas jcas = cas.getJCas();
    FSArray fsarray = new FSArray(jcas, 2);
    TOP fs1 = new TOP(jcas);
    TOP fs2 = new TOP(jcas);
    fsarray.set(0, fs1);
    fsarray.set(1, fs2);
    
//    fsarray.addToIndexes(); // if added to indexes, forces serialization of FSArray as an element
    
    AnnotationFS fsRef = cas.createAnnotation(refType, 0, 0);
    fsRef.setFeatureValue(ref, fsarray);
    cas.addFsToIndexes(fsRef);                    // gets serialized in=place
    
    fsRef = cas.createAnnotation(refType, 0, 0);
    fsRef.setFeatureValue(ref_FSArray, fsarray);
    cas.addFsToIndexes(fsRef);                    // gets serialized as ref
    
    String xml = serialize(cas, null);
    
 // deserialize into another CAS
    SAXParserFactory fact = SAXParserFactory.newInstance();
    SAXParser parser = fact.newSAXParser();
    XMLReader xmlReader = parser.getXMLReader();
    
    CAS cas2 = CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(), null);
    XmiCasDeserializer deser2 = new XmiCasDeserializer(cas2.getTypeSystem());
    ContentHandler deserHandler2 = deser2.getXmiCasHandler(cas2);
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(new StringReader(xml)));
    
    CasComparer.assertEquals(cas, cas2);
    
    // ------- repeat with lists in place of arrays --------------

    cas.reset();
    fs1 = new TOP(jcas); //https://issues.apache.org/jira/browse/UIMA-5544
    fs2 = new TOP(jcas);
    
    FSList fslist2 = new EmptyFSList(jcas);
    FSList fslist1 = new NonEmptyFSList(jcas, fs2, fslist2);
    FSList fslist0 = new NonEmptyFSList(jcas, fs1, fslist1);
    
//    fsarray.addToIndexes(); // if added to indexes, forces serialization of FSArray as an element
    
    fsRef = cas.createAnnotation(refType, 0, 0);
    fsRef.setFeatureValue(ref, fslist0);
    cas.addFsToIndexes(fsRef);                    // gets serialized in=place
    
    fsRef = cas.createAnnotation(refType, 0, 0);
    fsRef.setFeatureValue(ref_FSList, fslist0);
    cas.addFsToIndexes(fsRef);                    // gets serialized as ref
    
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

  }
  
  public void testDeserializeAndReserialize() throws Exception {
    try {
      File tsWithNoMultiRefs = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
      File tsWithMultiRefs = JUnitExtension.getFile("ExampleCas/testTypeSystem_withMultiRefs.xml");
      doTestDeserializeAndReserialize(tsWithMultiRefs,false);
      doTestDeserializeAndReserialize(tsWithNoMultiRefs,false);
      //also test with JCas initialized
      doTestDeserializeAndReserialize(tsWithNoMultiRefs,true);
      doTestDeserializeAndReserialize(tsWithMultiRefs,true);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  private void doTestDeserializeAndReserialize(File typeSystemDescriptorFile, boolean useJCas) throws Exception {
    // deserialize a complex CAS from XCAS
    TypeSystemDescription typeSystemDescription = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            new XMLInputSource(typeSystemDescriptorFile));
    CAS cas = CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(), indexes);
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
    
    // tests serialization of types which are arrays of other FS types, eg. Annotation[]
    TypeSystemImpl tsi = (TypeSystemImpl) cas.getTypeSystem();
    TypeImpl arrayOfAnnotType = (TypeImpl) tsi.getArrayType(tsi.annotType);
    
    Type annotArrayTestType0 = tsi.getType("org.apache.uima.testTypeSystem.AnnotationArrayTest");
    FeatureImpl annotArrayFeat = (FeatureImpl) annotArrayTestType0.getFeatureByBaseName("arrayOfAnnotations");
    Iterator<AnnotationFS> iter3 = cas.getAnnotationIndex(annotArrayTestType0).iterator();
    assertTrue(iter3.hasNext());
    AnnotationFS aa = iter3.next();

    // This currently fails to serialize (May 2016) so is commented out for now
    // see https://issues.apache.org/jira/browse/UIMA-4917
//    int a2 = cas.getLowLevelCAS().ll_createArray(arrayOfAnnotType.getCode(), 2);
//    cas.getLowLevelCAS().ll_setIntValue(((FeatureStructureImpl)aa).getAddress(), annotArrayFeat.getCode(), a2);

    // reserialize as XMI
    String xml = serialize(cas, null);
//    dumpStr2File(xml, "145");

    
    // deserialize into another CAS
    CAS cas2 = CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(), indexes);
    if (useJCas) {
      cas2.getJCas();
    }
    XmiCasDeserializer deser2 = new XmiCasDeserializer(cas2.getTypeSystem());
    ContentHandler deserHandler2 = deser2.getXmiCasHandler(cas2);
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(new StringReader(xml)));
    
    // compare
    assertEquals(cas.getAnnotationIndex().size(), cas2.getAnnotationIndex().size());
    assertEquals(cas.getDocumentText(), cas2.getDocumentText());
    CasComparer.assertEquals(cas,cas2);

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
    Type annotArrayTestType = cas2.getTypeSystem().getType("org.apache.uima.testTypeSystem.AnnotationArrayTest");
    Iterator<AnnotationFS> iter2 = cas2.getAnnotationIndex(annotArrayTestType).iterator();
    assertTrue(iter2.hasNext());
    while (iter2.hasNext()) {
      FeatureStructure fs = iter2.next();
      ArrayFS arrayFS = (ArrayFS) fs.getFeatureValue(annotArrayFeat);
      assertNotNull(arrayFS);
      for (int i = 0; i < arrayFS.size(); i++) {
        assertNotNull(arrayFS.get(i));
      }
    }
    
    // test that lenient mode does not report errors
    CAS cas3 = CasCreationUtils.createCas(new TypeSystemDescription_impl(),
            new TypePriorities_impl(), new FsIndexDescription[0]);
    if (useJCas) {
      cas3.getJCas();
    }
    XmiCasDeserializer deser3 = new XmiCasDeserializer(cas3.getTypeSystem());
    ContentHandler deserHandler3 = deser3.getXmiCasHandler(cas3, true);
    xmlReader.setContentHandler(deserHandler3);
    xmlReader.parse(new InputSource(new StringReader(xml)));
  }
  
  /*
   * https://issues.apache.org/jira/browse/UIMA-3396
   */
  public void testDeltaCasIndexing() throws Exception {
    try {
      CAS cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
              indexes);
      CAS cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
              indexes);
      cas1.setDocumentText("This is a test document in the initial view");
      FSIndexRepositoryImpl ir1 = (FSIndexRepositoryImpl) cas1.getIndexRepository();
      
      AnnotationFS anAnnotBefore = cas1.createAnnotation(cas1.getAnnotationType(), 0, 2);
      ir1.addFS(anAnnotBefore);
      
      cas1.createMarker();  // will start journaling index updates
      
      AnnotationFS anAnnot1 = cas1.createAnnotation(cas1.getAnnotationType(), 0, 4);
      ir1.addFS(anAnnot1);
      ir1.removeFS(anAnnot1);
      ir1.addFS(anAnnot1);
      
      assertTrue(ir1.getAddedFSs().length == 1);
      assertTrue(ir1.getDeletedFSs().length == 0);
      assertTrue(ir1.getReindexedFSs().length == 0);
      
      ir1.removeFS(anAnnotBefore);
      ir1.addFS(anAnnotBefore);
      
      assertTrue(ir1.getAddedFSs().length == 1);
      assertTrue(ir1.getDeletedFSs().length == 0);
      assertTrue(ir1.getReindexedFSs().length == 1);      
      
      ir1.removeFS(anAnnotBefore);
      assertTrue(ir1.getAddedFSs().length == 1);
      assertTrue(ir1.getDeletedFSs().length == 1);
      assertTrue(ir1.getReindexedFSs().length == 0);      

      
    } catch (Exception e) {
    JUnitExtension.handleException(e);
    }
  }
  
  public void testMultiThreadedSerialize() throws Exception {
    try {
      File tsWithNoMultiRefs = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
      doTestMultiThreadedSerialize(tsWithNoMultiRefs);
      File tsWithMultiRefs = JUnitExtension.getFile("ExampleCas/testTypeSystem_withMultiRefs.xml");
      doTestMultiThreadedSerialize(tsWithMultiRefs);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  private static class DoSerialize implements Runnable{
  	private CAS cas;
  	
  	DoSerialize(CAS aCas) {
  		cas = aCas;
  	}
  	
		public void run() {
			try {
			  while (true) {
  			  if (!MultiThreadUtils.wait4go((ThreadM) Thread.currentThread())) {
  			    break;
  			  }
  
  			  serialize(cas, null);
			  }
//				serialize(cas, null);
//				serialize(cas, null);
//				serialize(cas, null);
			} catch (IOException e) {
			  throw new RuntimeException(e);
			} catch (SAXException e) {
        throw new RuntimeException(e);				
			}
		}
  }
  
  private static int MAX_THREADS = 16;
  // do as sequence 1, 2, 4, 8, 16 and measure elapsed time
  private static int [] threadsToUse = new int[] {1, 2, 4, 8, 16/*, 32, 64*/};

  private void doTestMultiThreadedSerialize(File typeSystemDescriptor) throws Exception {
    // deserialize a complex CAS from XCAS
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

    // make n copies of the cas, so they all share
    // the same type system
    
    final CAS [] cases = new CAS[MAX_THREADS];
    
    for (int i = 0; i < MAX_THREADS; i++) {
    	cases[i] = CasCreationUtils.createCas(cas.getTypeSystem(), new TypePriorities_impl(),	indexes, null);
    	CasCopier.copyCas(cas, cases[i], true);
    }
    
    // start n threads, serializing as XMI
    MultiThreadUtils.ThreadM [] threads = new MultiThreadUtils.ThreadM[MAX_THREADS];
    for (int i = 0; i < MAX_THREADS; i++) {
      threads[i] = new MultiThreadUtils.ThreadM(new DoSerialize(cases[i]));
      threads[i].start();
    }
    MultiThreadUtils.waitForAllReady(threads);
    
    for (int i = 0; i < threadsToUse.length; i++) {
      MultiThreadUtils.ThreadM[] sliceOfThreads = new MultiThreadUtils.ThreadM[threadsToUse[i]];
      System.arraycopy(threads, 0, sliceOfThreads, 0, threadsToUse[i]);
      
    	long startTime = System.currentTimeMillis();
    	
    	MultiThreadUtils.kickOffThreads(sliceOfThreads);
    	
    	MultiThreadUtils.waitForAllReady(sliceOfThreads);
    	
    	System.out.println("\nNumber of threads serializing: " + threadsToUse[i] + 
    			               "  Normalized millisecs (should be close to the same): " + (System.currentTimeMillis() - startTime) / threadsToUse[i]);
    }
    
    MultiThreadUtils.terminateThreads(threads);
  }

  public void testDeltaCasIndexExistingFsInView() throws Exception {
    CAS cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
            indexes);
    CAS cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
            indexes);
    cas1.setDocumentText("This is a test document in the initial view");
    Type referentType = cas1.getTypeSystem().getType("org.apache.uima.testTypeSystem.Referent");
    FeatureStructure fs1 = cas1.createFS(referentType);
    cas1.getIndexRepository().addFS(fs1);

    //serialize complete
    XmiSerializationSharedData sharedData = new XmiSerializationSharedData();
    String xml = serialize(cas1, sharedData);
//    System.out.println(xml);
    int maxOutgoingXmiId = sharedData.getMaxXmiId();

    //deserialize into cas2
    XmiSerializationSharedData sharedData2 = new XmiSerializationSharedData();
    this.deserialize(xml, cas2, sharedData2, true, -1);
    CasComparer.assertEquals(cas1, cas2);

    //create Marker, add/modify fs and serialize in delta xmi format.
    Marker marker = cas2.createMarker();

    //create View
    CAS view = cas2.createView("NewView");
    //add FS to index
    Type referentType2 = cas2.getTypeSystem().getType("org.apache.uima.testTypeSystem.Referent");
    Iterator<FeatureStructure> fsIter = cas2.getIndexRepository().getAllIndexedFS(referentType2);
    while (fsIter.hasNext()) {
      FeatureStructure fs = fsIter.next();
      view.getIndexRepository().addFS(fs);
    }
    AnnotationFS cas2newAnnot = view.createAnnotation(cas2.getAnnotationType(), 6, 8);
    view.getIndexRepository().addFS(cas2newAnnot);

    // serialize cas2 in delta format
    String deltaxml1 = serialize(cas2, sharedData2, marker);
//    System.out.println(deltaxml1);

    //deserialize delta xmi into cas1
    this.deserialize(deltaxml1, cas1, sharedData, true, maxOutgoingXmiId, AllowPreexistingFS.allow);

    //check that new View contains the FS
    CAS deserView = cas1.getView("NewView");
    Iterator<FeatureStructure> deserFsIter = deserView.getIndexRepository().getAllIndexedFS(referentType);
    assertTrue(deserFsIter.hasNext());
  }

  // test - initial view, no Sofa, 
  public void testDeltaCasIndexExistingFsInInitialView() throws Exception {
    CAS cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
            indexes);
    CAS cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
            indexes);
    // no sofa
//    cas1.setDocumentText("This is a test document in the initial view");
    Type referentType = cas1.getTypeSystem().getType("org.apache.uima.testTypeSystem.Referent");
    FeatureStructure fs1 = cas1.createFS(referentType);
    cas1.getIndexRepository().addFS(fs1);  // index in initial view

    //serialize complete
    XmiSerializationSharedData sharedData = new XmiSerializationSharedData();
    String xml = serialize(cas1, sharedData);
//    System.out.println(xml);
    int maxOutgoingXmiId = sharedData.getMaxXmiId();

    //deserialize into cas2
    XmiSerializationSharedData sharedData2 = new XmiSerializationSharedData();
    this.deserialize(xml, cas2, sharedData2, true, -1);
    CasComparer.assertEquals(cas1, cas2);

    //create Marker, add/modify fs and serialize in delta xmi format.
    Marker marker = cas2.createMarker();

    //create View
    CAS view = cas2.createView("NewView");
    //add FS to index
    Type referentType2 = cas2.getTypeSystem().getType("org.apache.uima.testTypeSystem.Referent");
    Iterator<FeatureStructure> fsIter = cas2.getIndexRepository().getAllIndexedFS(referentType2);
    while (fsIter.hasNext()) {
      FeatureStructure fs = fsIter.next();
      view.getIndexRepository().addFS(fs);
    }
    AnnotationFS cas2newAnnot = view.createAnnotation(cas2.getAnnotationType(), 6, 8);
    view.getIndexRepository().addFS(cas2newAnnot);

    // add fs to initial view index repo.
    
    fs1 = cas2.createFS(referentType);
    cas2.getIndexRepository().addFS(fs1);
    
    // serialize cas2 in delta format
    String deltaxml1 = serialize(cas2, sharedData2, marker);
//    System.out.println(deltaxml1);

    //deserialize delta xmi into cas1
    this.deserialize(deltaxml1, cas1, sharedData, true, maxOutgoingXmiId, AllowPreexistingFS.allow);

    //check that new View contains the FS
    CAS deserView = cas1.getView("NewView");
    Iterator<FeatureStructure> deserFsIter = deserView.getIndexRepository().getAllIndexedFS(referentType);
    assertTrue(deserFsIter.hasNext());
  }
  
  public void testDeltaCasIndexExistingFsInNewView() throws Exception {
    CAS cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
            indexes);
    CAS cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
            indexes);
    cas1.setDocumentText("This is a test document in the initial view");
    Type referentType = cas1.getTypeSystem().getType("org.apache.uima.testTypeSystem.Referent");
    FeatureStructure fs1 = cas1.createFS(referentType);
    cas1.getIndexRepository().addFS(fs1);

    //serialize complete
    XmiSerializationSharedData sharedData = new XmiSerializationSharedData();
    String xml = serialize(cas1, sharedData);
//    System.out.println(xml);
    int maxOutgoingXmiId = sharedData.getMaxXmiId();

    //deserialize into cas2
    XmiSerializationSharedData sharedData2 = new XmiSerializationSharedData();
    this.deserialize(xml, cas2, sharedData2, true, -1);
    CasComparer.assertEquals(cas1, cas2);

    //create Marker, add/modify fs and serialize in delta xmi format.
    Marker marker = cas2.createMarker();

    //create View
    CAS view = cas2.createView("NewView");
    //add FS to index
    Type referentType2 = cas2.getTypeSystem().getType("org.apache.uima.testTypeSystem.Referent");
    Iterator<FeatureStructure> fsIter = cas2.getIndexRepository().getAllIndexedFS(referentType2);
    while (fsIter.hasNext()) {
      FeatureStructure fs = fsIter.next();
      view.getIndexRepository().addFS(fs);
    }
    AnnotationFS cas2newAnnot = view.createAnnotation(cas2.getAnnotationType(), 6, 8);
    view.getIndexRepository().addFS(cas2newAnnot);

    // serialize cas2 in delta format
    String deltaxml1 = serialize(cas2, sharedData2, marker);
//    System.out.println(deltaxml1);

    //deserialize delta xmi into cas1
    this.deserialize(deltaxml1, cas1, sharedData, true, maxOutgoingXmiId, AllowPreexistingFS.allow);

    //check that new View contains the FS
    CAS deserView = cas1.getView("NewView");
    Iterator<FeatureStructure> deserFsIter = deserView.getIndexRepository().getAllIndexedFS(referentType);
    assertTrue(deserFsIter.hasNext());
  }


  public void testMultipleSofas() throws Exception {
    try {
      CAS cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
              new FsIndexDescription[0]);
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
      XmiCasSerializer xmiSer = new XmiCasSerializer(cas.getTypeSystem());
      xmiSer.serialize(cas, xmlSer.getContentHandler());
      String xml = sw.getBuffer().toString();

      // deserialize into another CAS (repeat twice to check it still works after reset)
      CAS newCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
              new FsIndexDescription[0]);
      for (int i = 0; i < 2; i++) {
        XmiCasDeserializer newDeser = new XmiCasDeserializer(newCas.getTypeSystem());
        ContentHandler newDeserHandler = newDeser.getXmiCasHandler(newCas);
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
        // check that annotation is still indexed in both views
        it = newCas.getIndexRepository().getAllIndexedFS(topType);
        it2 = newCas2.getIndexRepository().getAllIndexedFS(topType);
        it.next(); it.next();
        it2.next(); it2.next(); 
        assertFalse(it.hasNext());
//        assertFalse(it2.hasNext());        assertTrue(tIndex.size() == 2); // document annot and this one
//        assertTrue(t2Index.size() == 2); // ditto

        newCas.reset();
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testTypeSystemFiltering() throws Exception {
    try {
      // deserialize a complex CAS from XCAS
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

      // now read in a TypeSystem that's a subset of those types
      TypeSystemDescription partialTypeSystemDesc = UIMAFramework.getXMLParser()
              .parseTypeSystemDescription(
                      new XMLInputSource(JUnitExtension
                              .getFile("ExampleCas/partialTestTypeSystem.xml")));
      TypeSystem partialTypeSystem = CasCreationUtils.createCas(partialTypeSystemDesc, null, null)
              .getTypeSystem();

      // reserialize as XMI, filtering out anything that doesn't fit in the
      // partialTypeSystem
      StringWriter sw = new StringWriter();
      XMLSerializer xmlSer = new XMLSerializer(sw, false);
      XmiCasSerializer xmiSer = new XmiCasSerializer(partialTypeSystem);
      xmiSer.serialize(cas, xmlSer.getContentHandler());
      String xml = sw.getBuffer().toString();
      // System.out.println(xml);

      // deserialize into another CAS (which has the whole type system)
      CAS cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
      XmiCasDeserializer deser2 = new XmiCasDeserializer(cas2.getTypeSystem());
      ContentHandler deserHandler2 = deser2.getXmiCasHandler(cas2);
      xmlReader.setContentHandler(deserHandler2);
      xmlReader.parse(new InputSource(new StringReader(xml)));

      // check that types have been filtered out
      Type orgType = cas2.getTypeSystem().getType("org.apache.uima.testTypeSystem.Organization");
      assertNotNull(orgType);
      assertTrue(cas2.getAnnotationIndex(orgType).size() == 0);
      assertTrue(cas.getAnnotationIndex(orgType).size() > 0);

      // but that some types are still there
      Type personType = cas2.getTypeSystem().getType("org.apache.uima.testTypeSystem.Person");
      FSIndex personIndex = cas2.getAnnotationIndex(personType);
      assertTrue(personIndex.size() > 0);

      // check that mentionType has been filtered out (set to null)
      FeatureStructure somePlace = personIndex.iterator().get();
      Feature mentionTypeFeat = personType.getFeatureByBaseName("mentionType");
      assertNotNull(mentionTypeFeat);
      assertNull(somePlace.getStringValue(mentionTypeFeat));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
 
  public void testNoInitialSofa() throws Exception {
    CAS cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
            new FsIndexDescription[0]);
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
    XmiCasSerializer xmiSer = new XmiCasSerializer(cas.getTypeSystem());
    xmiSer.serialize(cas, xmlSer.getContentHandler());
    String xml = sw.getBuffer().toString();

    // deserialize into another CAS
    CAS cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
            new FsIndexDescription[0]);

    XmiCasDeserializer deser2 = new XmiCasDeserializer(cas2.getTypeSystem());
    ContentHandler deserHandler2 = deser2.getXmiCasHandler(cas2);
    SAXParserFactory fact = SAXParserFactory.newInstance();
    SAXParser parser = fact.newSAXParser();
    XMLReader xmlReader = parser.getXMLReader();
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(new StringReader(xml)));

    //test that index is correctly populated
    Type intArrayType = cas2.getTypeSystem().getType(CAS.TYPE_NAME_INTEGER_ARRAY);
    Iterator<FeatureStructure> iter = cas2.getIndexRepository().getAllIndexedFS(intArrayType);
    assertTrue(iter.hasNext());
    IntArrayFS intArrayFS2 = (IntArrayFS)iter.next();
    assertFalse(iter.hasNext());
    assertEquals(5, intArrayFS2.size());
    assertEquals(1, intArrayFS2.get(0));
    assertEquals(2, intArrayFS2.get(1));
    assertEquals(3, intArrayFS2.get(2));
    assertEquals(4, intArrayFS2.get(3));
    assertEquals(5, intArrayFS2.get(4));

    // test that serializing the new CAS produces the same XML
    sw = new StringWriter();
    xmlSer = new XMLSerializer(sw, false);
    xmiSer = new XmiCasSerializer(cas2.getTypeSystem());
    xmiSer.serialize(cas2, xmlSer.getContentHandler());
    String xml2 = sw.getBuffer().toString();    
    assertTrue(xml2.equals(xml));
  }

  public void testv1FormatXcas() throws Exception {
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

    // test it
    assertTrue(CAS.NAME_DEFAULT_SOFA.equals(cas.getSofa().getSofaID()));

    // get a v1 XMI version of the same CAS
    serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/v1xmiCas.xml"));
    XmiCasDeserializer deser2 = new XmiCasDeserializer(v1cas.getTypeSystem());
    ContentHandler deserHandler2 = deser2.getXmiCasHandler(v1cas);
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(serCasStream));
    serCasStream.close();

    // compare
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
    assertTrue(engView.getAnnotationIndex().size() == 5); // 4 annots plus documentAnnotation
    CAS gerView = v1cas.getView("GermanDocument");
    assertTrue(gerView.getDocumentText().equals("das bier ist gut"));
    assertTrue(gerView.getAnnotationIndex().size() == 5); // 4 annots plus documentAnnotation
    assertTrue(CAS.NAME_DEFAULT_SOFA.equals(v1cas.getSofa().getSofaID()));
    assertTrue(v1cas.getDocumentText().equals("some text for the default text sofa."));

    // reserialize as XMI
    StringWriter sw = new StringWriter();
    XMLSerializer xmlSer = new XMLSerializer(sw, false);
    XmiCasSerializer xmiSer = new XmiCasSerializer(v1cas.getTypeSystem());
    xmiSer.serialize(v1cas, xmlSer.getContentHandler());
    String xml = sw.getBuffer().toString();

    cas.reset();

    // deserialize into another CAS
    deser2 = new XmiCasDeserializer(cas.getTypeSystem());
    deserHandler2 = deser2.getXmiCasHandler(cas);
    xmlReader.setContentHandler(deserHandler2);
    xmlReader.parse(new InputSource(new StringReader(xml)));

    // test it
    engView = cas.getView("EnglishDocument");
    assertTrue(engView.getDocumentText().equals("this beer is good"));
    assertTrue(engView.getAnnotationIndex().size() == 5); // 4 annots plus documentAnnotation
    gerView = cas.getView("GermanDocument");
    assertTrue(gerView.getDocumentText().equals("das bier ist gut"));
    assertTrue(gerView.getAnnotationIndex().size() == 5); // 4 annots plus documentAnnotation
    assertTrue(CAS.NAME_DEFAULT_SOFA.equals(v1cas.getSofa().getSofaID()));
    assertTrue(v1cas.getDocumentText().equals("some text for the default text sofa."));
  }
  
  public void testDuplicateNsPrefixes() throws Exception {
    TypeSystemDescription ts = new TypeSystemDescription_impl();
    ts.addType("org.bar.foo.Foo", "", "uima.tcas.Annotation");
    ts.addType("org.baz.foo.Foo", "", "uima.tcas.Annotation");
    CAS cas = CasCreationUtils.createCas(ts, null, null);
    cas.setDocumentText("Foo");
    Type t1 = cas.getTypeSystem().getType("org.bar.foo.Foo");
    Type t2 = cas.getTypeSystem().getType("org.baz.foo.Foo");
    AnnotationFS a1 = cas.createAnnotation(t1,0,3);
    cas.addFsToIndexes(a1);
    AnnotationFS a2 = cas.createAnnotation(t2,0,3);
    cas.addFsToIndexes(a2);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    XmiCasSerializer.serialize(cas, baos);
    baos.close();
    byte[] bytes = baos.toByteArray();
    
    CAS cas2 = CasCreationUtils.createCas(ts, null, null);
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    XmiCasDeserializer.deserialize(bais, cas2);
    bais.close();
    
    CasComparer.assertEquals(cas, cas2);
  }

  public void testMerging() throws Exception {
    testMerging(false);
  }

  public void testDeltaCasMerging() throws Exception {
    testMerging(true);
  }
  
  // Test merging with or without using delta CASes
  private void testMerging(boolean useDeltas) throws Exception {
    // deserialize a complex CAS from XCAS
    CAS cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
    XCASDeserializer.deserialize(serCasStream, cas);
    serCasStream.close();
    int numAnnotations = cas.getAnnotationIndex().size(); //for comparison later
    String docText = cas.getDocumentText(); //for comparison later
    //add a new Sofa to test that multiple Sofas in original CAS work
    CAS preexistingView = cas.createView("preexistingView");
    String preexistingViewText = "John Smith blah blah blah";
    preexistingView.setDocumentText(preexistingViewText);
    createPersonAnnot(preexistingView, 0, 10);
    
    // do XMI serialization to a string, using XmiSerializationSharedData
    // to keep track of maximum ID generated
    XmiSerializationSharedData serSharedData = new XmiSerializationSharedData();
    String xmiStr = serialize(cas, serSharedData);
    int maxOutgoingXmiId = serSharedData.getMaxXmiId();
        
    // deserialize into two new CASes, each with its own instance of XmiSerializationSharedData 
    // so we can get consistent IDs later when serializing back.
    CAS newCas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    XmiSerializationSharedData deserSharedData1 = new XmiSerializationSharedData();
    deserialize(xmiStr, newCas1, deserSharedData1, false, -1);
    
    CAS newCas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    XmiSerializationSharedData deserSharedData2 = new XmiSerializationSharedData();
    deserialize(xmiStr, newCas2, deserSharedData2, false, -1);

    Marker marker1 = null;
    Marker marker2 = null;
    if (useDeltas) {
      // create Marker before adding new FSs
      marker1 = newCas1.createMarker();
      marker2 = newCas2.createMarker();
    }
    
    //add new FS to each new CAS
    createPersonAnnot(newCas1, 0, 10);
    createPersonAnnot(newCas1, 20, 30);
    createPersonAnnot(newCas2, 40, 50);
    AnnotationFS person = createPersonAnnot(newCas2, 60, 70);
    
    //add an Owner relation that points to an organization in the original CAS,
    //to test links across merge boundary
    Type orgType = newCas2.getTypeSystem().getType(
            "org.apache.uima.testTypeSystem.Organization");
    AnnotationFS org = newCas2.getAnnotationIndex(orgType).iterator().next();
    Type ownerType = newCas2.getTypeSystem().getType(
            "org.apache.uima.testTypeSystem.Owner");
    Feature argsFeat = ownerType.getFeatureByBaseName("relationArgs");
    Feature componentIdFeat = ownerType.getFeatureByBaseName("componentId");
    Type relArgsType = newCas2.getTypeSystem().getType(
            "org.apache.uima.testTypeSystem.BinaryRelationArgs");
    Feature domainFeat = relArgsType.getFeatureByBaseName("domainValue");
    Feature rangeFeat = relArgsType.getFeatureByBaseName("rangeValue");
    AnnotationFS ownerAnnot = newCas2.createAnnotation(ownerType, 0, 70);
    FeatureStructure relArgs = newCas2.createFS(relArgsType);
    relArgs.setFeatureValue(domainFeat, person);
    relArgs.setFeatureValue(rangeFeat, org);
    ownerAnnot.setFeatureValue(argsFeat, relArgs);
    ownerAnnot.setStringValue(componentIdFeat, "XCasDeserializerTest");
    newCas2.addFsToIndexes(ownerAnnot);
    int orgBegin = org.getBegin();
    int orgEnd = org.getEnd();
    
    //add Sofas
    CAS newView1 = newCas1.createView("newSofa1");
    final String sofaText1 = "This is a new Sofa, created in CAS 1.";
    newView1.setDocumentText(sofaText1);
    final String annotText = "Sofa";
    int annotStart1 = sofaText1.indexOf(annotText);
    AnnotationFS annot1 = newView1.createAnnotation(orgType, annotStart1, annotStart1 + annotText.length());
    newView1.addFsToIndexes(annot1);
    CAS newView2 = newCas2.createView("newSofa2");
    final String sofaText2 = "This is another new Sofa, created in CAS 2.";
    newView2.setDocumentText(sofaText2);
    int annotStart2 = sofaText2.indexOf(annotText);
    AnnotationFS annot2 = newView2.createAnnotation(orgType, annotStart2, annotStart2 + annotText.length());
    newView2.addFsToIndexes(annot2);

    // Add an FS with an array of existing annotations in another view
    int nToks = 3;
    ArrayFS array = newView2.createArrayFS(nToks);
    Type thingType = newCas2.getTypeSystem().getType("org.apache.uima.testTypeSystem.Thing");
    FSIterator thingIter = newCas2.getAnnotationIndex(thingType).iterator();
    for (int i = 0; i < nToks; ++i)
      array.set(i, (FeatureStructure)thingIter.next());  
    Type annotArrayTestType = newView2.getTypeSystem().getType("org.apache.uima.testTypeSystem.AnnotationArrayTest");
    Feature annotArrayFeat = annotArrayTestType.getFeatureByBaseName("arrayOfAnnotations");
    AnnotationFS fsArrayTestAnno = newView2.createAnnotation(annotArrayTestType, 13, 27);
    fsArrayTestAnno.setFeatureValue(annotArrayFeat,array);
    newView2.addFsToIndexes(fsArrayTestAnno);
    
    // re-serialize each new CAS back to XMI, keeping consistent ids
    String newSerCas1 = serialize(newCas1, deserSharedData1, marker1);
    String newSerCas2 = serialize(newCas2, deserSharedData2, marker2);
    
    // merge the two XMI CASes back into the original CAS
    // the shared data will be reset and recreated if not using deltaCas
    if (useDeltas) {
      deserialize(newSerCas1, cas, serSharedData, false, maxOutgoingXmiId);
    } else {
      deserialize(newSerCas1, cas, serSharedData, false, -1);
    }
    assertEquals(numAnnotations + 2, cas.getAnnotationIndex().size());

    deserialize(newSerCas2, cas, serSharedData, false, maxOutgoingXmiId);
    assertEquals(numAnnotations + 5, cas.getAnnotationIndex().size());
    assertEquals(docText, cas.getDocumentText());

    // Serialize/deserialize again in case merge created duplicate ids
    String newSerCasMerged = serialize(cas, serSharedData);
    
    deserialize(newSerCasMerged, cas, serSharedData, false, -1);
    
    //check covered text of annotations
    FSIterator<AnnotationFS> iter = cas.getAnnotationIndex().iterator();
    while (iter.hasNext()) {
      AnnotationFS annot = (AnnotationFS)iter.next();
      assertEquals(cas.getDocumentText().substring(
              annot.getBegin(), annot.getEnd()), annot.getCoveredText());
    }
    //check Owner annotation we created to test link across merge boundary
    iter = cas.getAnnotationIndex(ownerType).iterator();
    while (iter.hasNext()) {
      AnnotationFS
      annot = iter.next();
      String componentId = annot.getStringValue(componentIdFeat);
      if ("XCasDeserializerTest".equals(componentId)) {
        FeatureStructure targetRelArgs = annot.getFeatureValue(argsFeat);
        AnnotationFS targetDomain = (AnnotationFS)targetRelArgs.getFeatureValue(domainFeat);
        assertEquals(60, targetDomain.getBegin());
        assertEquals(70, targetDomain.getEnd());
        AnnotationFS targetRange = (AnnotationFS)targetRelArgs.getFeatureValue(rangeFeat);
        assertEquals(orgBegin, targetRange.getBegin());
        assertEquals(orgEnd, targetRange.getEnd());
      }     
    } 
    //check Sofas
    CAS targetView1 = cas.getView("newSofa1");
    assertEquals(sofaText1, targetView1.getDocumentText());
    CAS targetView2 = cas.getView("newSofa2");
    assertEquals(sofaText2, targetView2.getDocumentText());
    AnnotationFS targetAnnot1 = targetView1.getAnnotationIndex(orgType).iterator().get();
    assertEquals(annotText, targetAnnot1.getCoveredText());
    AnnotationFS targetAnnot2 = targetView2.getAnnotationIndex(orgType).iterator().get();
    assertEquals(annotText, targetAnnot2.getCoveredText());
    assertTrue(targetView1.getSofa().getSofaRef() != 
            targetView2.getSofa().getSofaRef());
    
    CAS checkPreexistingView = cas.getView("preexistingView");
    assertEquals(preexistingViewText, checkPreexistingView.getDocumentText());
    Type personType = cas.getTypeSystem().getType("org.apache.uima.testTypeSystem.Person");    
    AnnotationFS targetAnnot3 = checkPreexistingView.getAnnotationIndex(personType).iterator().get();
    assertEquals("John Smith", targetAnnot3.getCoveredText());

    // Check the FS with an array of pre-existing FSs
    iter = targetView2.getAnnotationIndex(annotArrayTestType).iterator();
    componentIdFeat = thingType.getFeatureByBaseName("componentId");
    while (iter.hasNext()) {
      AnnotationFS annot = (AnnotationFS)iter.next();
      ArrayFS fsArray = (ArrayFS)annot.getFeatureValue(annotArrayFeat);
      assertTrue(fsArray.size() == 3);
      for (int i = 0; i < fsArray.size(); ++i) {
        AnnotationFS refAnno = (AnnotationFS)fsArray.get(i);
        assertEquals(thingType.getName(), refAnno.getType().getName());
        assertEquals("JResporator", refAnno.getStringValue(componentIdFeat));
        assertTrue(cas == refAnno.getView());
      }
    }
    
    //try an initial CAS that contains multiple Sofas
    
  }
  
  public void testDeltaCasIgnorePreexistingFS() throws Exception {
   try {
	  CAS cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
	          indexes);
	  CAS cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
	          indexes);
	  cas1.setDocumentText("This is a test document in the initial view");
	  AnnotationFS anAnnot1 = cas1.createAnnotation(cas1.getAnnotationType(), 0, 4);
	  cas1.getIndexRepository().addFS(anAnnot1);
	  AnnotationFS anAnnot2 = cas1.createAnnotation(cas1.getAnnotationType(), 5, 10);
	  cas1.getIndexRepository().addFS(anAnnot2);
	  FSIndex tIndex = cas1.getAnnotationIndex();
	  assertTrue(tIndex.size() == 3); //doc annot plus  annots
	  
	  //serialize complete  
	  XmiSerializationSharedData sharedData = new XmiSerializationSharedData();
	  String xml = this.serialize(cas1, sharedData);
	  int maxOutgoingXmiId = sharedData.getMaxXmiId();
	  //deserialize into cas2
	  XmiSerializationSharedData sharedData2 = new XmiSerializationSharedData();      
	  //XmiCasDeserializer.deserialize(new StringBufferInputStream(xml), cas2, true, sharedData2);
	  this.deserialize(xml, cas2, sharedData2, true, -1);
	  CasComparer.assertEquals(cas1, cas2);
	  
	  //create Marker, add/modify fs and serialize in delta xmi format.
	  Marker marker = cas2.createMarker();
	  FSIndex<AnnotationFS> cas2tIndex = cas2.getAnnotationIndex();
	  
	  //create an annotation and add to index
	  AnnotationFS cas2newAnnot = cas2.createAnnotation(cas2.getAnnotationType(), 6, 8);
	  cas2.getIndexRepository().addFS(cas2newAnnot);
	  assertTrue(cas2tIndex.size() == 4); // prev annots and this new one
	  
	  //modify an existing annotation
	  Iterator<AnnotationFS> tIndexIter = cas2tIndex.iterator();
	  AnnotationFS docAnnot = (AnnotationFS) tIndexIter.next(); //doc annot
	  //delete from index
	  AnnotationFS delAnnot = (AnnotationFS) tIndexIter.next(); //annot
	  cas2.getIndexRepository().removeFS(delAnnot);
	  assertTrue(cas2.getAnnotationIndex().size() == 3);
	  
	  //modify language feature
	  Feature languageF = cas2.getDocumentAnnotation().getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_LANGUAGE);
	  docAnnot.setStringValue(languageF, "en");
	  // serialize cas2 in delta format 
	  String deltaxml1 = serialize(cas2, sharedData2, marker);
	  //System.out.println("delta cas");
	  //System.out.println(deltaxml1);
	  
	  //deserialize delta xmi into cas1
	  this.deserialize(deltaxml1, cas1, sharedData, true, maxOutgoingXmiId, AllowPreexistingFS.ignore);
	  
	  //check language feature of doc annot is not changed.
	  //System.out.println(cas1.getDocumentAnnotation().getStringValue(languageF));
	  assertTrue( cas1.getAnnotationIndex().iterator().next().getStringValue(languageF).equals("x-unspecified"));
	  //check new annotation exists and preexisting is not deleted
	  assertTrue(cas1.getAnnotationIndex().size()==4);
   } catch (Exception e) {
	  JUnitExtension.handleException(e);
   }
  }
  
  public void testDeltaCasDisallowPreexistingFSMod() throws Exception {
    try {
      CAS cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
              indexes);
      CAS cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
              indexes);
      cas1.setDocumentText("This is a test document in the initial view");
      AnnotationFS anAnnot1 = cas1.createAnnotation(cas1.getAnnotationType(), 0, 4);
      cas1.getIndexRepository().addFS(anAnnot1);
      AnnotationFS anAnnot2 = cas1.createAnnotation(cas1.getAnnotationType(), 5, 10);
      cas1.getIndexRepository().addFS(anAnnot2);
      FSIndex<AnnotationFS> tIndex = cas1.getAnnotationIndex();
      assertTrue(tIndex.size() == 3); //doc annot plus 2 annots
      
      //serialize complete  
      XmiSerializationSharedData sharedData = new XmiSerializationSharedData();
      String xml = serialize(cas1, sharedData);
      int maxOutgoingXmiId = sharedData.getMaxXmiId();
      
      //deserialize into cas2
      XmiSerializationSharedData sharedData2 = new XmiSerializationSharedData();      
      this.deserialize(xml, cas2, sharedData2, true, -1);
      CasComparer.assertEquals(cas1, cas2);
      
      //create Marker, add/modify fs and serialize in delta xmi format.
      Marker marker = cas2.createMarker();
      FSIndex<AnnotationFS> cas2tIndex = cas2.getAnnotationIndex();
      
      //create an annotation and add to index
      AnnotationFS cas2newAnnot = cas2.createAnnotation(cas2.getAnnotationType(), 6, 8);
      cas2.getIndexRepository().addFS(cas2newAnnot);
      assertTrue(cas2tIndex.size() == 4); // prev annots and this new one
      
      //modify language feature
      Iterator<AnnotationFS> tIndexIter = cas2tIndex.iterator();
      AnnotationFS docAnnot = (AnnotationFS) tIndexIter.next();
      Feature languageF = cas2.getDocumentAnnotation().getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_LANGUAGE);
      docAnnot.setStringValue(languageF, "en");
     
      // serialize cas2 in delta format 
      String deltaxml1 = serialize(cas2, sharedData2, marker);
      //System.out.println(deltaxml1);
      
      //deserialize delta xmi into cas1
      try {
        this.deserialize(deltaxml1, cas1, sharedData, true, maxOutgoingXmiId, AllowPreexistingFS.disallow);
      } catch (CASRuntimeException e) {
    	assertTrue(e.getMessageKey() == CASRuntimeException.DELTA_CAS_PREEXISTING_FS_DISALLOWED);
      }
    	 
      //check language feature of doc annot is not changed.
      //System.out.println(cas1.getDocumentAnnotation().getStringValue(languageF));
      assertTrue( cas1.getAnnotationIndex().iterator().next().getStringValue(languageF).equals("x-unspecified"));
      //check new annotation exists
      assertTrue(cas1.getAnnotationIndex().size() == 3); // cas2 should be unchanged. 
	} catch (Exception e) {
		  JUnitExtension.handleException(e);
	}
  }
  
  
  public void testDeltaCasInvalidMarker() throws Exception {
	  try {
      CAS cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
              indexes);
      CAS cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
              indexes);

      //serialize complete  
      XmiSerializationSharedData sharedData = new XmiSerializationSharedData();
      String xml = serialize(cas1, sharedData);
      int maxOutgoingXmiId = sharedData.getMaxXmiId();
      
      //deserialize into cas2
      XmiSerializationSharedData sharedData2 = new XmiSerializationSharedData();      
      this.deserialize(xml, cas2, sharedData2, true, -1);
      CasComparer.assertEquals(cas1, cas2);
      
      //create Marker, add/modify fs and serialize in delta xmi format.
      Marker marker = cas2.createMarker();
      boolean caughtMutlipleMarker = false;
      try {
        Marker marker2 = cas2.createMarker();
      } catch (UIMARuntimeException e) {
        caughtMutlipleMarker = true;
        System.out.format("Should catch MultipleCreateMarker message: %s%n", e.getMessage());
      }
      assertTrue(caughtMutlipleMarker);
      
      //reset cas
      cas2.reset();
      boolean serfailed = false;
      try {
      	serialize(cas2, sharedData2, marker);
      } catch (CASRuntimeException e) {
      	serfailed = true;
      }
      assertTrue(serfailed);
      
//      serfailed = false;
//      try {
//      	serialize(cas2, sharedData2, marker2);
//      } catch (CASRuntimeException e) {
//      	serfailed = true;
//      }
//      assertTrue(serfailed);
	      
		} catch (Exception e) {
			  JUnitExtension.handleException(e);
		}
  }
  
  
  public void testDeltaCasNoChanges() throws Exception {
	    try {
	      CAS cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
	              indexes);
	      CAS cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
	              indexes);

	      //serialize complete  
	      XmiSerializationSharedData sharedData = new XmiSerializationSharedData();
	      String xml = serialize(cas1, sharedData);
	      int maxOutgoingXmiId = sharedData.getMaxXmiId();
	      
	      //deserialize into cas2
	      XmiSerializationSharedData sharedData2 = new XmiSerializationSharedData();      
	      this.deserialize(xml, cas2, sharedData2, true, -1);
	      CasComparer.assertEquals(cas1, cas2);
	      
	      //create Marker, add/modify fs and serialize in delta xmi format.
	      Marker marker = cas2.createMarker();
	      FSIndex<AnnotationFS> cas2tIndex = cas2.getAnnotationIndex();
	      
	      // serialize cas2 in delta format 
	      String deltaxml1 = serialize(cas2, sharedData2, marker);
	      //System.out.println(deltaxml1);
	      
	      //deserialize delta xmi into cas1
	      try {
	        this.deserialize(deltaxml1, cas1, sharedData, true, maxOutgoingXmiId, AllowPreexistingFS.disallow);
	      } catch (CASRuntimeException e) {
	    	assertTrue(e.getMessageKey() == CASRuntimeException.DELTA_CAS_PREEXISTING_FS_DISALLOWED);
	      }
	    	 	     
	      //check new annotation index
	      assertTrue(cas1.getAnnotationIndex().size() == 0); // cas2 should be unchanged. 
		} catch (Exception e) {
			  JUnitExtension.handleException(e);
		}
	  }
  
  public void testDeltaCasDisallowPreexistingFSViewMod() throws Exception {
    try {
      CAS cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
              indexes);
      CAS cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
              indexes);
      cas1.setDocumentText("This is a test document in the initial view");
      AnnotationFS anAnnot1 = cas1.createAnnotation(cas1.getAnnotationType(), 0, 4);
      cas1.getIndexRepository().addFS(anAnnot1);
      AnnotationFS anAnnot2 = cas1.createAnnotation(cas1.getAnnotationType(), 5, 10);
      cas1.getIndexRepository().addFS(anAnnot2);
      FSIndex<AnnotationFS> tIndex = cas1.getAnnotationIndex();
      assertTrue(tIndex.size() == 3); //doc annot plus 2 annots
      
      //serialize complete  
      XmiSerializationSharedData sharedData = new XmiSerializationSharedData();
      String xml = serialize(cas1, sharedData);
      int maxOutgoingXmiId = sharedData.getMaxXmiId();
      
      //deserialize into cas2
      XmiSerializationSharedData sharedData2 = new XmiSerializationSharedData();      
      this.deserialize(xml, cas2, sharedData2, true, -1);
      CasComparer.assertEquals(cas1, cas2);
      
      //create Marker, add/modify fs and serialize in delta xmi format.
      Marker marker = cas2.createMarker();
      FSIndex<AnnotationFS> cas2tIndex = cas2.getAnnotationIndex();
      
      //create an annotation and add to index
      AnnotationFS cas2newAnnot = cas2.createAnnotation(cas2.getAnnotationType(), 6, 8);
      cas2.getIndexRepository().addFS(cas2newAnnot);
      assertTrue(cas2tIndex.size() == 4); // prev annots and this new one
      
      //modify language feature
      Iterator<AnnotationFS> tIndexIter = cas2tIndex.iterator();
      AnnotationFS docAnnot = (AnnotationFS) tIndexIter.next();
      
      //delete annotation from index    
      AnnotationFS delAnnot = (AnnotationFS) tIndexIter.next(); //annot
      cas2.getIndexRepository().removeFS(delAnnot);
      assertTrue(cas2.getAnnotationIndex().size() == 3);
      
      // serialize cas2 in delta format 
      String deltaxml1 = serialize(cas2, sharedData2, marker);
      //System.out.println(deltaxml1);
      
      //deserialize delta xmi into cas1
      try {
        this.deserialize(deltaxml1, cas1, sharedData, true, maxOutgoingXmiId, AllowPreexistingFS.disallow);
      } catch (CASRuntimeException e) {
    	assertTrue(e.getMessageKey() == CASRuntimeException.DELTA_CAS_PREEXISTING_FS_DISALLOWED);
      }
    	 
      //check new annotation added and preexisitng FS not removed from index
      assertTrue(cas1.getAnnotationIndex().size() == 4);  
    } catch (Exception e) {
	  JUnitExtension.handleException(e);
    }
  }
  
  public void testDeltaCasAllowPreexistingFS() throws Exception {
   try {
      CAS cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
              indexes);
      CAS cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
              indexes);
      CAS cas3 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
              indexes);
      
      Type personType = cas1.getTypeSystem().getType(
      		"org.apache.uima.testTypeSystem.Person");
      Feature componentIdFeat = personType.getFeatureByBaseName("componentId");
      Feature confidenceFeat = personType.getFeatureByBaseName("confidence");
      Type orgType = cas1.getTypeSystem().getType(
			"org.apache.uima.testTypeSystem.Organization");
      Type ownerType = cas1.getTypeSystem().getType(
      						"org.apache.uima.testTypeSystem.Owner");
      Type entityAnnotType = cas1.getTypeSystem().getType(
		"org.apache.uima.testTypeSystem.EntityAnnotation");
      Feature mentionTypeFeat = entityAnnotType.getFeatureByBaseName("mentionType");
      Feature argsFeat = ownerType.getFeatureByBaseName("relationArgs");
      Type relArgsType = cas1.getTypeSystem().getType(
      						"org.apache.uima.testTypeSystem.BinaryRelationArgs");
      Feature domainFeat = relArgsType.getFeatureByBaseName("domainValue");
      Feature rangeFeat = relArgsType.getFeatureByBaseName("rangeValue");
      
      Type entityType = cas1.getTypeSystem().getType("org.apache.uima.testTypeSystem.Entity");
      Feature classesFeat = entityType.getFeatureByBaseName("classes");
      Feature linksFeat = entityType.getFeatureByBaseName("links");
      Feature canonicalFormFeat = entityType.getFeatureByBaseName("canonicalForm");
      
      Type nonEmptyFsListType = cas1.getTypeSystem().getType(CAS.TYPE_NAME_NON_EMPTY_FS_LIST);
      Type emptyFsListType = cas1.getTypeSystem().getType(CAS.TYPE_NAME_EMPTY_FS_LIST);
      Feature headFeat = nonEmptyFsListType.getFeatureByBaseName("head");
      Feature tailFeat = nonEmptyFsListType.getFeatureByBaseName("tail");
      
      //cas1
      //initial set of feature structures 
      // set document text for the initial view and create Annotations
      cas1.setDocumentText("This is a test document in the initial view");
      AnnotationFS anAnnot1 = cas1.createAnnotation(cas1.getAnnotationType(), 0, 4);
      cas1.getIndexRepository().addFS(anAnnot1);
      AnnotationFS anAnnot2 = cas1.createAnnotation(cas1.getAnnotationType(), 5, 6);
      cas1.getIndexRepository().addFS(anAnnot2);
      AnnotationFS anAnnot3 = cas1.createAnnotation(cas1.getAnnotationType(), 8, 13);
      cas1.getIndexRepository().addFS(anAnnot3);
      AnnotationFS anAnnot4 = cas1.createAnnotation(cas1.getAnnotationType(), 15, 30);
      cas1.getIndexRepository().addFS(anAnnot4);
      FSIndex<AnnotationFS> tIndex = cas1.getAnnotationIndex();
      assertTrue(tIndex.size() == 5); //doc annot plus 4 annots
      
      FeatureStructure entityFS = cas1.createFS(entityType);
      cas1.getIndexRepository().addFS(entityFS);
      
      StringArrayFS strArrayFS = cas1.createStringArrayFS(5);
      strArrayFS.set(0, "class1");
      entityFS.setFeatureValue(classesFeat, strArrayFS);
      
      //create listFS and set the link feature
      FeatureStructure emptyNode = cas1.createFS(emptyFsListType);
      FeatureStructure secondNode = cas1.createFS(nonEmptyFsListType);
      secondNode.setFeatureValue(headFeat, anAnnot2);
      secondNode.setFeatureValue(tailFeat, emptyNode);
      FeatureStructure firstNode = cas1.createFS(nonEmptyFsListType);
      firstNode.setFeatureValue(headFeat, anAnnot1);
      firstNode.setFeatureValue(tailFeat, secondNode);
      entityFS.setFeatureValue(linksFeat, firstNode);
      
      // create a view w/o setting document text
      CAS view1 = cas1.createView("View1");
      
      // create another view 
      CAS preexistingView = cas1.createView("preexistingView");
      String preexistingViewText = "John Smith blah blah blah";
      preexistingView.setDocumentText(preexistingViewText);
      AnnotationFS person1Annot = createPersonAnnot(preexistingView, 0, 10);
      person1Annot.setStringValue(componentIdFeat, "deltacas1");
      AnnotationFS person2Annot = createPersonAnnot(preexistingView, 0, 5);
      AnnotationFS orgAnnot = preexistingView.createAnnotation(orgType, 16, 24);
      preexistingView.addFsToIndexes(orgAnnot);
      
      AnnotationFS ownerAnnot = preexistingView.createAnnotation(ownerType, 0, 24);
      preexistingView.addFsToIndexes(ownerAnnot);
      FeatureStructure relArgs = cas1.createFS(relArgsType);
      relArgs.setFeatureValue(domainFeat, person1Annot);
      ownerAnnot.setFeatureValue(argsFeat, relArgs);
      
      //serialize complete  
      XmiSerializationSharedData sharedData = new XmiSerializationSharedData();
      String xml = serialize(cas1, sharedData);
      int maxOutgoingXmiId = sharedData.getMaxXmiId();
      //System.out.println("CAS1 " + xml);
      //System.out.println("MaxOutgoingXmiId " + maxOutgoingXmiId);
   
      //deserialize into cas2
      XmiSerializationSharedData sharedData2 = new XmiSerializationSharedData();      
      this.deserialize(xml, cas2, sharedData2, true, -1);
      CasComparer.assertEquals(cas1, cas2);
 
      //=======================================================================
      //create Marker, add/modify fs and serialize in delta xmi format.
      Marker marker = cas2.createMarker();
      FSIndex<AnnotationFS> cas2tIndex = cas2.getAnnotationIndex();
      CAS cas2preexistingView = cas2.getView("preexistingView");
      FSIndex cas2personIndex = cas2preexistingView.getAnnotationIndex(personType);
      FSIndex cas2orgIndex = cas2preexistingView.getAnnotationIndex(orgType);
      FSIndex cas2ownerIndex = cas2preexistingView.getAnnotationIndex(ownerType);
      
      // create an annotation and add to index
      AnnotationFS cas2anAnnot5 = cas2.createAnnotation(cas2.getAnnotationType(), 6, 8);
      cas2.getIndexRepository().addFS(cas2anAnnot5);
      assertTrue(cas2tIndex.size() == 6); // prev annots and this new one
     
      // set document text of View1
      CAS cas2view1 = cas2.getView("View1");
      cas2view1.setDocumentText("This is the View1 document.");
      //create an annotation in View1
      AnnotationFS cas2view1Annot = cas2view1.createAnnotation(cas2.getAnnotationType(), 1, 5);
      cas2view1.getIndexRepository().addFS(cas2view1Annot);
      FSIndex cas2view1Index = cas2view1.getAnnotationIndex();
      assertTrue(cas2view1Index.size() == 2); //document annot and this annot
      
      //modify an existing annotation
      Iterator<AnnotationFS> tIndexIter = cas2tIndex.iterator();
      AnnotationFS docAnnot = (AnnotationFS) tIndexIter.next(); //doc annot
      AnnotationFS modAnnot1 = (AnnotationFS) tIndexIter.next();
      AnnotationFS delAnnot = (AnnotationFS)  tIndexIter.next();
      
      //modify language feature
      Feature languageF = cas2.getDocumentAnnotation().getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_LANGUAGE);
      docAnnot.setStringValue(languageF, "en");
      
      //index update - reindex
      cas2.getIndexRepository().removeFS(modAnnot1);
      Feature endF = cas2.getAnnotationType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_END);
      modAnnot1.setIntValue(endF, 4);
      cas2.getIndexRepository().addFS(modAnnot1);
      //index update - remove annotation from index 
      cas2.getIndexRepository().removeFS(delAnnot);
      
      //modify FS - string feature and FS feature.
      Iterator<FeatureStructure> personIter = cas2personIndex.iterator();     
      AnnotationFS cas2person1 = (AnnotationFS) personIter.next();
      AnnotationFS cas2person2 = (AnnotationFS) personIter.next();
      
      cas2person1.setFloatValue(confidenceFeat, (float) 99.99);
      cas2person1.setStringValue(mentionTypeFeat, "FULLNAME");
      
      cas2person2.setStringValue(componentIdFeat, "delataCas2");
      cas2person2.setStringValue(mentionTypeFeat, "FIRSTNAME");
      
      Iterator<FeatureStructure> orgIter = cas2orgIndex.iterator();
      AnnotationFS cas2orgAnnot = (AnnotationFS) orgIter.next();
      cas2orgAnnot.setStringValue(mentionTypeFeat, "ORGNAME");
      
      //modify FS feature
      Iterator<FeatureStructure> ownerIter = cas2ownerIndex.iterator();
      AnnotationFS cas2ownerAnnot = (AnnotationFS) ownerIter.next();
      FeatureStructure cas2relArgs = cas2ownerAnnot.getFeatureValue(argsFeat);
      cas2relArgs.setFeatureValue(rangeFeat, cas2orgAnnot);
      
    //Test modification of a nonshared multivalued feature.
      //This should serialize the encompassing FS.
      Iterator<FeatureStructure> iter = cas2.getIndexRepository().getIndex("testEntityIndex").iterator();
      FeatureStructure cas2EntityFS = iter.next();
      StringArrayFS cas2strarrayFS = (StringArrayFS) cas2EntityFS.getFeatureValue(classesFeat);
      cas2strarrayFS.set(1, "class2");
      cas2strarrayFS.set(2, "class3");
      cas2strarrayFS.set(3, "class4");
      cas2strarrayFS.set(4, "class5");
      
      //add to FSList 
      FeatureStructure cas2linksFS = cas2EntityFS.getFeatureValue(linksFeat);
      FeatureStructure cas2secondNode = cas2linksFS.getFeatureValue(tailFeat);
      FeatureStructure cas2emptyNode = cas2secondNode.getFeatureValue(tailFeat);
      FeatureStructure cas2thirdNode = cas2.createFS(nonEmptyFsListType);
      cas2thirdNode.setFeatureValue(headFeat, cas2anAnnot5);
      cas2thirdNode.setFeatureValue(tailFeat, cas2emptyNode);
      cas2secondNode.setFeatureValue(tailFeat, cas2thirdNode);
      
//      // Test that the new access method returns an array containing just the right marker
      // removed per https://issues.apache.org/jira/browse/UIMA-2478
//      List<Marker> mkrs = cas2.getMarkers();
//      assertNotNull(mkrs);
//      assertEquals(1, mkrs.size());
//      assertEquals(marker, mkrs.get(0));
      
      // serialize cas2 in delta format 
      String deltaxml1 = serialize(cas2, sharedData2, marker);
      //System.out.println("delta cas");
      //System.out.println(deltaxml1);
      
      //======================================================================
      //deserialize delta xmi into cas1
      this.deserialize(deltaxml1, cas1, sharedData, true, maxOutgoingXmiId, AllowPreexistingFS.allow);
      
      //======================================================================
      //serialize complete cas and deserialize into cas3 and compare with cas1.
      String fullxml = serialize(cas2, sharedData2);
      XmiSerializationSharedData sharedData3 = new XmiSerializationSharedData();
      this.deserialize(fullxml, cas3, sharedData3, true, -1);
      CasComparer.assertEquals(cas1, cas3); 
      
      //System.out.println("CAS1 " + serialize(cas1, new XmiSerializationSharedData()));
      //System.out.println("CAS2 " + serialize(cas2, new XmiSerializationSharedData()));
      
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
  
  public void testDeltaCasListFS() throws Exception {
	   try {
	      CAS cas1 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
	              indexes);
	      CAS cas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
	              indexes);
	      CAS cas3 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(),
	              indexes);
	      
	      Type entityType = cas1.getTypeSystem().getType("org.apache.uima.testTypeSystem.Entity");
	      Feature classesFeat = entityType.getFeatureByBaseName("classes");
	      Feature linksFeat = entityType.getFeatureByBaseName("links");
	      Feature canonicalFormFeat = entityType.getFeatureByBaseName("canonicalForm");
	      
	      Type nonEmptyFsListType = cas1.getTypeSystem().getType(CAS.TYPE_NAME_NON_EMPTY_FS_LIST);
	      Type emptyFsListType = cas1.getTypeSystem().getType(CAS.TYPE_NAME_EMPTY_FS_LIST);
	      Feature headFeat = nonEmptyFsListType.getFeatureByBaseName("head");
	      Feature tailFeat = nonEmptyFsListType.getFeatureByBaseName("tail");
	      
	      //cas1
	      //initial set of feature structures 
	      // set document text for the initial view and create Annotations
	      cas1.setDocumentText("This is a test document in the initial view");
	      AnnotationFS anAnnot1 = cas1.createAnnotation(cas1.getAnnotationType(), 0, 4);
	      cas1.getIndexRepository().addFS(anAnnot1);
	      AnnotationFS anAnnot2 = cas1.createAnnotation(cas1.getAnnotationType(), 5, 6);
	      cas1.getIndexRepository().addFS(anAnnot2);
	      AnnotationFS anAnnot3 = cas1.createAnnotation(cas1.getAnnotationType(), 8, 13);
	      cas1.getIndexRepository().addFS(anAnnot3);
	      AnnotationFS anAnnot4 = cas1.createAnnotation(cas1.getAnnotationType(), 15, 30);
	      cas1.getIndexRepository().addFS(anAnnot4);
	      FSIndex tIndex = cas1.getAnnotationIndex();
	      assertTrue(tIndex.size() == 5); //doc annot plus 4 annots
	      
	      FeatureStructure entityFS = cas1.createFS(entityType);
	      cas1.getIndexRepository().addFS(entityFS);
	      
	      StringArrayFS strArrayFS = cas1.createStringArrayFS(5);
	      strArrayFS.set(0, "class1");
	      entityFS.setFeatureValue(classesFeat, strArrayFS);
	      
	      //create listFS and set the link feature
	      FeatureStructure emptyNode = cas1.createFS(emptyFsListType);
	      FeatureStructure secondNode = cas1.createFS(nonEmptyFsListType);
	      secondNode.setFeatureValue(headFeat, anAnnot2);
	      secondNode.setFeatureValue(tailFeat, emptyNode);
	      FeatureStructure firstNode = cas1.createFS(nonEmptyFsListType);
	      firstNode.setFeatureValue(headFeat, anAnnot1);
	      firstNode.setFeatureValue(tailFeat, secondNode);
	      entityFS.setFeatureValue(linksFeat, firstNode);
	      
	      // create a view w/o setting document text
	      CAS view1 = cas1.createView("View1");
	       
	      //serialize complete  
	      XmiSerializationSharedData sharedData = new XmiSerializationSharedData();
	      String xml = serialize(cas1, sharedData);
	      int maxOutgoingXmiId = sharedData.getMaxXmiId();
	      //System.out.println("CAS1 " + xml);
	      //System.out.println("MaxOutgoingXmiId " + maxOutgoingXmiId);
	   
	      //deserialize into cas2
	      XmiSerializationSharedData sharedData2 = new XmiSerializationSharedData();      
	      this.deserialize(xml, cas2, sharedData2, true, -1);
	      CasComparer.assertEquals(cas1, cas2);
	 
	      //=======================================================================
	      //create Marker, add/modify fs and serialize in delta xmi format.
	      Marker marker = cas2.createMarker();
	      FSIndex cas2tIndex = cas2.getAnnotationIndex();
	      
	      // create an annotation and add to index
	      AnnotationFS cas2anAnnot5 = cas2.createAnnotation(cas2.getAnnotationType(), 6, 8);
	      cas2.getIndexRepository().addFS(cas2anAnnot5);
	      assertTrue(cas2tIndex.size() == 6); // prev annots and this new one
	      // create an annotation and add to index
	      AnnotationFS cas2anAnnot6 = cas2.createAnnotation(cas2.getAnnotationType(), 6, 8);
	      cas2.getIndexRepository().addFS(cas2anAnnot6);
	      assertTrue(cas2tIndex.size() == 7); // prev annots and twonew one
	      
	      //add to FSList 
	      Iterator<FeatureStructure> iter = cas2.getIndexRepository().getIndex("testEntityIndex").iterator();
	      FeatureStructure cas2EntityFS = iter.next();
	      FeatureStructure cas2linksFS = cas2EntityFS.getFeatureValue(linksFeat);
	      FeatureStructure cas2secondNode = cas2linksFS.getFeatureValue(tailFeat);
	      FeatureStructure cas2emptyNode = cas2secondNode.getFeatureValue(tailFeat);
	      FeatureStructure cas2thirdNode = cas2.createFS(nonEmptyFsListType);
	      FeatureStructure cas2fourthNode = cas2.createFS(nonEmptyFsListType);
	      
	      cas2secondNode.setFeatureValue(tailFeat, cas2thirdNode);
	      cas2thirdNode.setFeatureValue(headFeat, cas2anAnnot5);
	      cas2thirdNode.setFeatureValue(tailFeat, cas2fourthNode);
	      cas2fourthNode.setFeatureValue(headFeat, cas2anAnnot6);
	      cas2fourthNode.setFeatureValue(tailFeat, cas2emptyNode);
	      
	      // serialize cas2 in delta format 
	      String deltaxml1 = serialize(cas2, sharedData2, marker);
	      //System.out.println("delta cas");
	      //System.out.println(deltaxml1);
	      
	      //======================================================================
	      //deserialize delta xmi into cas1
	      this.deserialize(deltaxml1, cas1, sharedData, true, maxOutgoingXmiId, AllowPreexistingFS.allow);
	      CasComparer.assertEquals(cas2linksFS, entityFS.getFeatureValue(linksFeat));
	      //======================================================================
	      //serialize complete cas and deserialize into cas3 and compare with cas1.
	      String fullxml = serialize(cas2, sharedData2);
	      XmiSerializationSharedData sharedData3 = new XmiSerializationSharedData();
	      this.deserialize(fullxml, cas3, sharedData3, true,-1);
	      CasComparer.assertEquals(cas1, cas3); 
	      
	      //System.out.println("CAS1 " + serialize(cas1, new XmiSerializationSharedData()));
	      //System.out.println("CAS2 " + serialize(cas2, new XmiSerializationSharedData()));
	      
	    } catch (Exception e) {
	      JUnitExtension.handleException(e);
	    }
	  }
  
  
  
  public void testOutOfTypeSystemData() throws Exception {
    // deserialize a simple XMI into a CAS with no TypeSystem    
    CAS cas = CasCreationUtils.createCas(new TypeSystemDescription_impl(),
            new TypePriorities_impl(), new FsIndexDescription[0]);
    File xmiFile = JUnitExtension.getFile("ExampleCas/simpleCas.xmi");
    String xmiStr = FileUtils.file2String(xmiFile, "UTF-8");
    
    XmiSerializationSharedData sharedData = new XmiSerializationSharedData();
    deserialize(xmiStr, cas, sharedData, true, -1);
    
    //do some checks on the out-of-type system data
    List ootsElems = sharedData.getOutOfTypeSystemElements();
    assertEquals(9, ootsElems.size());
    List ootsViewMembers = sharedData.getOutOfTypeSystemViewMembers("1");
    assertEquals(7, ootsViewMembers.size());
    
    // now reserialize including OutOfTypeSystem data
    String xmiStr2 = serialize(cas, sharedData);
    
    //deserialize both original and new XMI into CASes that do have the full typesystem
    CAS newCas1 = CasCreationUtils.createCas(typeSystem, null, indexes);
    deserialize(xmiStr, newCas1, null, false, -1);
    CAS newCas2 = CasCreationUtils.createCas(typeSystem, null, indexes);
    deserialize(xmiStr2, newCas2, null, false, -1);
    CasComparer.assertEquals(newCas1, newCas2);  
    
    //Test a partial type system with a missing some missing features and
    //missing "Organization" type
    File partialTypeSystemFile = JUnitExtension.getFile("ExampleCas/partialTestTypeSystem.xml");
    TypeSystemDescription partialTypeSystem = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            new XMLInputSource(partialTypeSystemFile));
    CAS partialTsCas = CasCreationUtils.createCas(partialTypeSystem, null, indexes);
    XmiSerializationSharedData sharedData2 = new XmiSerializationSharedData();
    deserialize(xmiStr, partialTsCas, sharedData2, true, -1);
    
    assertEquals(1,sharedData2.getOutOfTypeSystemElements().size());
    OotsElementData ootsFeats3 = sharedData2.getOutOfTypeSystemFeatures(sharedData2.getFsAddrForXmiId(3));
    assertEquals(1, ootsFeats3.attributes.size());
    XmlAttribute ootsAttr = ootsFeats3.attributes.get(0);
    assertEquals("mentionType", ootsAttr.name);
    assertEquals("NAME", ootsAttr.value);
    OotsElementData ootsFeats5 = sharedData2.getOutOfTypeSystemFeatures(sharedData2.getFsAddrForXmiId(5));
    assertEquals(0, ootsFeats5.attributes.size());
    assertEquals(1, ootsFeats5.childElements.size());
    XmlElementNameAndContents ootsChildElem = ootsFeats5.childElements.get(0);
    assertEquals("mentionType", ootsChildElem.name.qName);
    assertEquals("NAME", ootsChildElem.contents);
    
    OotsElementData ootsFeats8 = sharedData2.getOutOfTypeSystemFeatures(sharedData2.getFsAddrForXmiId(8));
    assertEquals(1, ootsFeats8.attributes.size());
    OotsElementData ootsFeats10 = sharedData2.getOutOfTypeSystemFeatures(sharedData2.getFsAddrForXmiId(10));
    assertEquals(1, ootsFeats10.attributes.size());
    OotsElementData ootsFeats11 = sharedData2.getOutOfTypeSystemFeatures(sharedData2.getFsAddrForXmiId(11));
    assertEquals(4, ootsFeats11.childElements.size());
    
    String xmiStr3 = serialize(partialTsCas, sharedData2);
    newCas2.reset();
    deserialize(xmiStr3, newCas2, null, false, -1);
    CasComparer.assertEquals(newCas1, newCas2);    
 }
  
  public void testOutOfTypeSystemArrayElement() throws Exception {
    //add to type system an annotation type that has an FSArray feature
    TypeDescription testAnnotTypeDesc = typeSystem.addType("org.apache.uima.testTypeSystem.TestAnnotation", "", "uima.tcas.Annotation");
    testAnnotTypeDesc.addFeature("arrayFeat", "", "uima.cas.FSArray");
    //populate a CAS with such an array
    CAS cas = CasCreationUtils.createCas(typeSystem, null, null);
    Type testAnnotType = cas.getTypeSystem().getType("org.apache.uima.testTypeSystem.TestAnnotation");
    Type orgType = cas.getTypeSystem().getType(
      "org.apache.uima.testTypeSystem.Organization");
    AnnotationFS orgAnnot1 = cas.createAnnotation(orgType, 0, 10);
    cas.addFsToIndexes(orgAnnot1);
    AnnotationFS orgAnnot2 = cas.createAnnotation(orgType, 10, 20);
    cas.addFsToIndexes(orgAnnot2);
    AnnotationFS testAnnot = cas.createAnnotation(testAnnotType, 0, 20);
    cas.addFsToIndexes(testAnnot);
    ArrayFS arrayFs = cas.createArrayFS(2);
    arrayFs.set(0, orgAnnot1);
    arrayFs.set(1, orgAnnot2);
    Feature arrayFeat = testAnnotType.getFeatureByBaseName("arrayFeat");
    testAnnot.setFeatureValue(arrayFeat, arrayFs);
    
    //serialize to XMI
    String xmiStr = serialize(cas, null);
    
    //deserialize into a CAS that's missing the Organization type
    File partialTypeSystemFile = JUnitExtension.getFile("ExampleCas/partialTestTypeSystem.xml");
    TypeSystemDescription partialTypeSystem = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            new XMLInputSource(partialTypeSystemFile));
    testAnnotTypeDesc = partialTypeSystem.addType("org.apache.uima.testTypeSystem.TestAnnotation", "", "uima.tcas.Annotation");
    testAnnotTypeDesc.addFeature("arrayFeat", "", "uima.cas.FSArray");
    CAS partialTsCas = CasCreationUtils.createCas(partialTypeSystem, null, null);
    XmiSerializationSharedData sharedData = new XmiSerializationSharedData();
    deserialize(xmiStr, partialTsCas, sharedData, true, -1);
    
    //check out of type system data
    Type testAnnotType2 = partialTsCas.getTypeSystem().getType("org.apache.uima.testTypeSystem.TestAnnotation");
    FeatureStructure testAnnot2 = partialTsCas.getAnnotationIndex(testAnnotType2).iterator().get(); 
    Feature arrayFeat2 = testAnnotType2.getFeatureByBaseName("arrayFeat");
    FeatureStructure arrayFs2 = testAnnot2.getFeatureValue(arrayFeat2);
    List ootsElems = sharedData.getOutOfTypeSystemElements();
    assertEquals(2, ootsElems.size());
    List ootsArrayElems = sharedData.getOutOfTypeSystemArrayElements(arrayFs2.hashCode());
    assertEquals(2, ootsArrayElems.size());
    for (int i = 0; i < 2; i++) {
      OotsElementData oed = (OotsElementData)ootsElems.get(i);
      XmiArrayElement arel = (XmiArrayElement)ootsArrayElems.get(i);
      assertEquals(oed.xmiId, arel.xmiId);      
    }
    
    //reserialize along with out of type system data
    String xmiStr2 = serialize(partialTsCas, sharedData);
    
    //deserialize into a new CAS and compare
    CAS cas2 = CasCreationUtils.createCas(typeSystem, null, null);
    deserialize(xmiStr2, cas2, null, false, -1);
    
    CasComparer.assertEquals(cas, cas2);    
  }
  
  public void testOutOfTypeSystemListElement() throws Exception {
    //add to type system an annotation type that has an FSList feature
    TypeDescription testAnnotTypeDesc = typeSystem.addType("org.apache.uima.testTypeSystem.TestAnnotation", "", "uima.tcas.Annotation");
    testAnnotTypeDesc.addFeature("listFeat", "", "uima.cas.FSList");
    //populate a CAS with such an list
    CAS cas = CasCreationUtils.createCas(typeSystem, null, null);
    Type testAnnotType = cas.getTypeSystem().getType("org.apache.uima.testTypeSystem.TestAnnotation");
    Type orgType = cas.getTypeSystem().getType(
      "org.apache.uima.testTypeSystem.Organization");
    AnnotationFS orgAnnot1 = cas.createAnnotation(orgType, 0, 10);
    cas.addFsToIndexes(orgAnnot1);
    AnnotationFS orgAnnot2 = cas.createAnnotation(orgType, 10, 20);
    cas.addFsToIndexes(orgAnnot2);
    AnnotationFS testAnnot = cas.createAnnotation(testAnnotType, 0, 20);
    cas.addFsToIndexes(testAnnot);
    Type nonEmptyFsListType = cas.getTypeSystem().getType(CAS.TYPE_NAME_NON_EMPTY_FS_LIST);
    Type emptyFsListType = cas.getTypeSystem().getType(CAS.TYPE_NAME_EMPTY_FS_LIST);
    Feature headFeat = nonEmptyFsListType.getFeatureByBaseName("head");
    Feature tailFeat = nonEmptyFsListType.getFeatureByBaseName("tail");
    FeatureStructure emptyNode = cas.createFS(emptyFsListType);
    FeatureStructure secondNode = cas.createFS(nonEmptyFsListType);
    secondNode.setFeatureValue(headFeat, orgAnnot2);
    secondNode.setFeatureValue(tailFeat, emptyNode);
    FeatureStructure firstNode = cas.createFS(nonEmptyFsListType);
    firstNode.setFeatureValue(headFeat, orgAnnot1);
    firstNode.setFeatureValue(tailFeat, secondNode);
    
    Feature listFeat = testAnnotType.getFeatureByBaseName("listFeat");
    testAnnot.setFeatureValue(listFeat, firstNode);
    
    //serialize to XMI
    String xmiStr = serialize(cas, null);
//    System.out.println(xmiStr);
    
    //deserialize into a CAS that's missing the Organization type
    File partialTypeSystemFile = JUnitExtension.getFile("ExampleCas/partialTestTypeSystem.xml");
    TypeSystemDescription partialTypeSystem = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            new XMLInputSource(partialTypeSystemFile));
    testAnnotTypeDesc = partialTypeSystem.addType("org.apache.uima.testTypeSystem.TestAnnotation", "", "uima.tcas.Annotation");
    testAnnotTypeDesc.addFeature("listFeat", "", "uima.cas.FSList");
    CAS partialTsCas = CasCreationUtils.createCas(partialTypeSystem, null, null);
    XmiSerializationSharedData sharedData = new XmiSerializationSharedData();
    deserialize(xmiStr, partialTsCas, sharedData, true, -1);
    
    //check out of type system data
    Type testAnnotType2 = partialTsCas.getTypeSystem().getType("org.apache.uima.testTypeSystem.TestAnnotation");
    FeatureStructure testAnnot2 = partialTsCas.getAnnotationIndex(testAnnotType2).iterator().get(); 
    Feature listFeat2 = testAnnotType2.getFeatureByBaseName("listFeat");
    FeatureStructure listFs = testAnnot2.getFeatureValue(listFeat2);
    List ootsElems = sharedData.getOutOfTypeSystemElements();
    assertEquals(2, ootsElems.size());
    OotsElementData oed = sharedData.getOutOfTypeSystemFeatures(listFs.hashCode());
    XmlAttribute attr = oed.attributes.get(0);
    assertNotNull(attr);
    assertEquals(CAS.FEATURE_BASE_NAME_HEAD, attr.name);
    assertEquals(attr.value, ((OotsElementData)ootsElems.get(0)).xmiId);
    
    //reserialize along with out of type system data
    String xmiStr2 = serialize(partialTsCas, sharedData);
//    System.out.println(xmiStr2);
    
    //deserialize into a new CAS and compare
    CAS cas2 = CasCreationUtils.createCas(typeSystem, null, null);
    deserialize(xmiStr2, cas2, null, false, -1);
    
    CasComparer.assertEquals(cas, cas2);    
  }
  
  public void testOutOfTypeSystemDataComplexCas() throws Exception {
    // deserialize a complex XCAS
    CAS originalCas = CasCreationUtils.createCas(typeSystem, null, indexes);
    InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
    XCASDeserializer.deserialize(serCasStream, originalCas);
    serCasStream.close();
    
    //serialize to XMI
    String xmiStr = serialize(originalCas, null);
    
    //deserialize into a CAS with no type system
    CAS casWithNoTs = CasCreationUtils.createCas(new TypeSystemDescription_impl(),
            new TypePriorities_impl(), new FsIndexDescription[0]);
    XmiSerializationSharedData sharedData = new XmiSerializationSharedData();
    deserialize(xmiStr, casWithNoTs, sharedData, true, -1);
        
    // now reserialize including OutOfTypeSystem data
    String xmiStr2 = serialize(casWithNoTs, sharedData);
    
    //deserialize into a new CAS that has the full type system
    CAS newCas = CasCreationUtils.createCas(typeSystem, null, indexes);
    deserialize(xmiStr2, newCas, null, false, -1);
    
    //compare
    CasComparer.assertEquals(originalCas, newCas);
    
    //Test a partial type system with a missing some missing features and
    //missing "Organization" type
    File partialTypeSystemFile = JUnitExtension.getFile("ExampleCas/partialTestTypeSystem.xml");
    TypeSystemDescription partialTypeSystem = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            new XMLInputSource(partialTypeSystemFile));
    CAS partialTsCas = CasCreationUtils.createCas(partialTypeSystem, null, indexes);
    XmiSerializationSharedData sharedData2 = new XmiSerializationSharedData();
    deserialize(xmiStr, partialTsCas, sharedData2, true, -1);
        
    String xmiStr3 = serialize(partialTsCas, sharedData2);
    newCas.reset();
    deserialize(xmiStr3, newCas, null, false, -1);
    CasComparer.assertEquals(originalCas, newCas);    
 }
  
//  public void testdebugnc() throws Exception {
//    CAS cas = CasCreationUtils.createCas(typeSystem, null, indexes);
//    InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/simpleCas.xmi"));
//    XmiCasDeserializer.deserialize(serCasStream, cas);
//    serCasStream.close();
//    
//    XmiSerializationSharedData sharedData = new XmiSerializationSharedData();
//    String r = serialize(cas, sharedData);
//    
//    {  File f = new File("C:/au/wksp431/apache/json-work/tempdebug.xml");
//  PrintWriter pw = new PrintWriter(f);
//  pw.println(r);
//  pw.close();
//    }
//
//  }
//  public void testGetNumChildren() throws Exception {
//    // deserialize a complex XCAS
//    CAS cas = CasCreationUtils.createCas(typeSystem, null, indexes);
////    InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
////    XCASDeserializer.deserialize(serCasStream, cas);
////    serCasStream.close();
//  InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/simpleCas.xmi"));
//  XmiCasDeserializer.deserialize(serCasStream, cas);
//  serCasStream.close();
//    
//    // call serializer with a ContentHandler that checks numChildren
//    XmiCasSerializer xmiSer = new XmiCasSerializer(cas.getTypeSystem());
//    GetNumChildrenTestHandler handler = new GetNumChildrenTestHandler(xmiSer, (CASImpl)cas);
//    xmiSer.serialize(cas, handler, handler.tcds);
//  }

  /** Utility method for serializing a CAS to an XMI String 
   * */
  private static String serialize(CAS cas, XmiSerializationSharedData serSharedData) throws IOException, SAXException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();    
    XmiCasSerializer.serialize(cas, null, baos, false, serSharedData);
    baos.close();
    String xmiStr = new String(baos.toByteArray(), "UTF-8");   //note by default XmiCasSerializer generates UTF-8
    
    //workaround for newline serialization problem in Sun Java 1.4.2
    //this test file should contain CRLF line endings, but Sun Java loses them
    //when it serializes XML.
    if(!builtInXmlSerializationSupportsCRs()) {
      xmiStr = xmiStr.replaceAll("&#10;", "&#13;&#10;");
    }        
    return xmiStr;
  }
  
  /** Utility method for serializing a Delta CAS to XMI String 
   * */
  private static String serialize(CAS cas, XmiSerializationSharedData serSharedData, Marker marker) throws IOException, SAXException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();    
    XmiCasSerializer.serialize(cas, null, baos, false, serSharedData, marker);
    baos.close();
    String xmiStr = new String(baos.toByteArray(), "UTF-8");   //note by default XmiCasSerializer generates UTF-8
    
    //workaround for newline serialization problem in Sun Java 1.4.2
    //this test file should contain CRLF line endings, but Sun Java loses them
    //when it serializes XML.
    if(!builtInXmlSerializationSupportsCRs()) {
      xmiStr = xmiStr.replaceAll("&#10;", "&#13;&#10;");
    }        
    return xmiStr;
  }
  
  /** Utility method for deserializing a CAS from an XMI String */
  private void deserialize(String xmlStr, CAS cas, XmiSerializationSharedData sharedData, boolean lenient, int mergePoint) throws FactoryConfigurationError, ParserConfigurationException, SAXException, IOException {
    byte[] bytes = xmlStr.getBytes("UTF-8"); //this assumes the encoding is UTF-8, which is the default output encoding of the XmiCasSerializer
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    XmiCasDeserializer.deserialize(bais, cas, lenient, sharedData, mergePoint);
    bais.close();
  }
  
  private void deserialize(String xmlStr, CAS cas, XmiSerializationSharedData sharedData, boolean lenient, int mergePoint, AllowPreexistingFS allow) throws FactoryConfigurationError, ParserConfigurationException, SAXException, IOException {
    byte[] bytes = xmlStr.getBytes("UTF-8"); //this assumes the encoding is UTF-8, which is the default output encoding of the XmiCasSerializer
    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    XmiCasDeserializer.deserialize(bais, cas, lenient, sharedData, mergePoint, allow);
    bais.close();
  }  
  
  private AnnotationFS createPersonAnnot(CAS cas, int begin, int end) {
    Type personType = cas.getTypeSystem().getType("org.apache.uima.testTypeSystem.Person");
    AnnotationFS person = cas.createAnnotation(personType, begin, end);
    cas.addFsToIndexes(person);
    return person;
  }
  
  /**
   * Checks the Java vendor and version and returns true if running a version
   * of Java whose built-in XSLT support can properly serialize carriage return
   * characters, and false if not.  It seems to be the case that Sun JVMs prior
   * to 1.5 do not properly serialize carriage return characters.  We have to
   * modify our test case to account for this.
   * @return true if XML serialization of CRs behave properly in the current JRE
   */
  private static boolean builtInXmlSerializationSupportsCRs() {
    String javaVendor = System.getProperty("java.vendor");
    if( javaVendor.startsWith("Sun") ) {
        String javaVersion = System.getProperty("java.version");
        if( javaVersion.startsWith("1.3") || javaVersion.startsWith("1.4") )
            return false;
    }
    return true;
  }  

  /** for debug
   * 
   * @param s - string to dump
   * @param name - file name part
   */
  private static void dumpStr2File(String s, String namepart) throws FileNotFoundException {
 
    File f = new File("C:/au/wksp431/apache/json-work/temp" + namepart + ".xml");
    PrintWriter pw = new PrintWriter(f);
    pw.println(s);
    pw.close();

  }
//  /**
//   * Extends the SAX Normal default handler used to output xml
//   * Keeps a stack of counts:
//   *    For every startElement call, pushes the number of children of that element onto the stack
//   *    For every endElement call, test if at number has been decremented to 0
//   *       Inner endElement calls decrement the count (of their parentt,
//   *       Inner "text" elements also do this (for their parent)
//   */
//  static class GetNumChildrenTestHandler extends DefaultHandler {
//    XmiCasSerializer xmiSer;
//    Stack<Integer> childCountStack = new Stack<Integer>();
//    private XmiDocSerializer tcds; 
//    
//    GetNumChildrenTestHandler(XmiCasSerializer xmiSer, CASImpl cas) {
//      this.xmiSer = xmiSer;
//      tcds = xmiSer.getTestXmiDocSerializer(this, cas);
//      childCountStack.push(Integer.valueOf(1));
//    }
//
//    /* (non-Javadoc)
//     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
//     */
//    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
//      // TODO Auto-generated method stub
//      super.startElement(uri, localName, qName, attributes);
//      childCountStack.push(Integer.valueOf(tcds.getNumChildren()));
//      System.out.format("Debug: NumberOfChildren: Starting element %s,  setting count to %d%n", qName, tcds.getNumChildren());
//    }
//
//    /* (non-Javadoc)
//     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
//     */
//    public void endElement(String uri, String localName, String qName) throws SAXException {
//      // TODO Auto-generated method stub
//      super.endElement(uri, localName, qName);
//      //check that we've seen the expected number of child elements
//      //(count on top of stack should be 0)
//      Integer count = (Integer)childCountStack.pop();
//      assertEquals(0, count.intValue());
//      
//      //decremenet child count of our parent
//      count = (Integer)childCountStack.pop();
//      childCountStack.push(Integer.valueOf(count.intValue() - 1));
//      System.out.format("Debug: NumberOfChildren: ending element %s,  decr parent count to %d%n", qName, count - 1);
//
//    }
//
//    /* (non-Javadoc)
//     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
//     */
//    public void characters(char[] ch, int start, int length) throws SAXException {
//      // text node is considered a child
//      if (length > 0) {
//        Integer count = (Integer)childCountStack.pop();
//        childCountStack.push(Integer.valueOf(count.intValue() - 1));
//        System.out.format("Debug: NumberOfChildren: ending text element,  decr parent count to %d%n", count - 1);        
//      } else {
//        System.out.format("Debug: NumberOfChildren: ending empty text element, not decr count%n");
//      }
//    }
//
//    
//  }
}
