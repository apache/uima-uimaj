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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas_data.impl.CasComparer;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;


public class CasCopierTest extends TestCase {
  private TypeSystemDescription typeSystem;

  private FsIndexDescription[] indexes;

  protected void setUp() throws Exception {
    File typeSystemFile1 = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
    File indexesFile = JUnitExtension.getFile("ExampleCas/testIndexes.xml");

    typeSystem = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            new XMLInputSource(typeSystemFile1));
    indexes = UIMAFramework.getXMLParser().parseFsIndexCollection(new XMLInputSource(indexesFile))
            .getFsIndexes();
  }

  public void testCopyCas() throws Exception {
    // create a source CAS by deserializing from XCAS
    CAS srcCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    InputStream serCasStream = new FileInputStream(JUnitExtension
            .getFile("ExampleCas/multiSofaCas.xml"));
    XCASDeserializer.deserialize(serCasStream, srcCas);
    serCasStream.close();

    // create a destination CAS
    CAS destCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);

    // do the copy
    CasCopier.copyCas(srcCas, destCas, true);
    // XCASSerializer.serialize(destCas, System.out);

    // verify copy
    CasComparer.assertEquals(srcCas, destCas);

    // try with type systems are not identical (dest. a superset of src.)
    TypeSystemDescription additionalTypes = new TypeSystemDescription_impl();
    TypeDescription fooType = additionalTypes.addType("test.Foo", "Test Type",
            "uima.tcas.Annotation");
    fooType.addFeature("bar", "Test Feature", "uima.cas.String");
    ArrayList<TypeSystemDescription> destTypeSystems = new ArrayList<TypeSystemDescription>();
    destTypeSystems.add(additionalTypes);
    destTypeSystems.add(typeSystem);
    CAS destCas2 = CasCreationUtils.createCas(destTypeSystems);
    CasCopier.copyCas(srcCas, destCas2, true);
    CasComparer.assertEquals(srcCas, destCas);

    // try with base CAS rather than initial view
    CAS srcCasBase = ((CASImpl) srcCas).getBaseCAS();
    destCas.reset();
    CAS destCasBase = ((CASImpl) destCas).getBaseCAS();
    CasCopier.copyCas(srcCasBase, destCasBase, true);
    CasComparer.assertEquals(srcCasBase, destCasBase);
    
    //try with source and dest cas the same
    // 
    Exception ee = null;
    try {
      CasCopier.copyCas(srcCasBase,  srcCasBase, false);
    } catch (Exception e) {
      ee = e;
    }
    assertTrue(ee instanceof UIMARuntimeException);
    
    ee = null;
    CAS v2 = srcCas.createView("v2");
    CasCopier cc = new CasCopier(srcCas, v2);
    try {
      cc.copyCasView(srcCas, v2, false);
    } catch (Exception e) {
      e.printStackTrace();
      ee = e;
    }
    assertEquals(ee, null);
  }
  
  public void testCopyCasWithDifferentTypeSystemObject() throws Exception {
    // create a source CAS by deserializing from XCAS
    CAS srcCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    InputStream serCasStream = new FileInputStream(JUnitExtension
            .getFile("ExampleCas/multiSofaCas.xml"));
    XCASDeserializer.deserialize(serCasStream, srcCas);
    serCasStream.close();

    // create a destination CAS (do not share the same type system object)
    File typeSystemFile = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
    File indexesFile = JUnitExtension.getFile("ExampleCas/testIndexes.xml");

    TypeSystemDescription newTsDesc = typeSystem = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            new XMLInputSource(typeSystemFile));
    FsIndexDescription[] newFsIndexes = indexes = UIMAFramework.getXMLParser().parseFsIndexCollection(new XMLInputSource(indexesFile))
            .getFsIndexes();
    CAS destCas = CasCreationUtils.createCas(newTsDesc, new TypePriorities_impl(), newFsIndexes);

    // do the copy
    CasCopier.copyCas(srcCas, destCas, true);
    // XCASSerializer.serialize(destCas, System.out);

    // verify copy
    CasComparer.assertEquals(srcCas, destCas);

    // try with type systems are not identical (dest. a superset of src.)
    TypeSystemDescription additionalTypes = new TypeSystemDescription_impl();
    TypeDescription fooType = additionalTypes.addType("test.Foo", "Test Type",
            "uima.tcas.Annotation");
    fooType.addFeature("bar", "Test Feature", "uima.cas.String");
    ArrayList<TypeSystemDescription> destTypeSystems = new ArrayList<TypeSystemDescription>();
    destTypeSystems.add(additionalTypes);
    destTypeSystems.add(typeSystem);
    CAS destCas2 = CasCreationUtils.createCas(destTypeSystems);
    CasCopier.copyCas(srcCas, destCas2, true);
    CasComparer.assertEquals(srcCas, destCas2);
    
    // try with src type system having extra type (no instances) with
    //   incompatible ranges
    additionalTypes = new TypeSystemDescription_impl();
    fooType = additionalTypes.addType("test.Foo", "Test Type",
            "uima.tcas.Annotation");
    fooType.addFeature("bar", "Test Feature", "uima.cas.Float");
    
    ArrayList<TypeSystemDescription> srcTypeSystems = new ArrayList<TypeSystemDescription>();
    srcTypeSystems.add(additionalTypes);
    srcTypeSystems.add(typeSystem);
    
    srcCas = CasCreationUtils.createCas(srcTypeSystems);
    serCasStream = new FileInputStream(JUnitExtension
            .getFile("ExampleCas/multiSofaCas.xml"));
    XCASDeserializer.deserialize(serCasStream, srcCas);
    serCasStream.close();
    
    destCas2.reset();
    
    CasCopier.copyCas(srcCas, destCas2, true);
    CasComparer.assertEquals(srcCas, destCas2);
    
    // try with base CAS rather than initial view
    CAS srcCasBase = ((CASImpl) srcCas).getBaseCAS();
    destCas.reset();
    CAS destCasBase = ((CASImpl) destCas).getBaseCAS();
    CasCopier.copyCas(srcCasBase, destCasBase, true);
    CasComparer.assertEquals(srcCasBase, destCasBase);
  }  

  public void testCopyCasView() throws Exception {
    // create a source CAS by deserializing from XCAS
    CAS srcCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
    XCASDeserializer.deserialize(serCasStream, srcCas);
    serCasStream.close();

    // create a new view
    CAS tgtCas = srcCas.createView("view2");
    CasCopier copierv = new CasCopier(srcCas, tgtCas);
    
    // copy a fs while in an iteration
    FSIterator<FeatureStructure> itv = srcCas.getJCas().getFSIndexRepository().getIndex("testEntityIndex").iterator();
    FSIterator<Annotation> ita = srcCas.getJCas().getAnnotationIndex().iterator();

    while (ita.hasNext()) {
      Annotation fs = (Annotation) ita.next();
      Annotation fsv2 = (Annotation) copierv.copyFs(fs);
      fsv2.addToIndexes();
    }

    while (itv.hasNext()) {
      FeatureStructure fs =  itv.next();
      TOP fsv2 = (TOP) copierv.copyFs(fs);
      fsv2.addToIndexes();      
    }
    
    serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
    XCASDeserializer.deserialize(serCasStream, srcCas);
    serCasStream.close();    
    
    // create a destination CAS
    CAS destCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);

    CasCopier copier;
    // do the copy
    long shortest = Long.MAX_VALUE;
    int i = 0;
//    for (; i < 60000; i++) {  // uncomment for perf test.  was more than 5x faster than version 2.6.0
      destCas.reset();
      long startTime = System.nanoTime();
      copier = new CasCopier(srcCas, destCas);
      copier.copyCasView(srcCas, true);
      long time = (System.nanoTime() - startTime)/ 1000;
      if (time < shortest) {
        shortest = time;
        System.out.format("CasCopier speed for 400KB xcas is %,d microseconds on iteration %,d%n", shortest, i);
      }
//    }
    
    // verify copy
    CasComparer.assertEquals(srcCas, destCas);
    
    // do the copy to a different view
    // create a destination CAS
    destCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);

    // do the copy
    copier = new CasCopier(srcCas, destCas);
    copier.copyCasView(srcCas, "aNewView", true);

    // verify copy
    CasComparer.assertEqualViews(srcCas, destCas.getView("aNewView"));
    
  }
  
  public void testCopyCasViewsWithWrapper() throws Exception {
    // create a source CAS by deserializing from XCAS
    CAS srcCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
    XCASDeserializer.deserialize(serCasStream, srcCas);
    serCasStream.close();

    // create a destination CAS
    CAS tgtCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);

    // make wrappers
    CAS wrappedSrcCas = new CasWrapperForTstng(srcCas);
    CAS wrappedTgtCas = new CasWrapperForTstng(tgtCas);
    
    // do the copy
    CasCopier copier = new CasCopier(wrappedSrcCas, wrappedTgtCas);
    copier.copyCasView(wrappedSrcCas, true);

    // verify copy
    CasComparer.assertEquals(wrappedSrcCas, wrappedTgtCas);
    
    // do the copy to a different view
    // create a destination CAS
    tgtCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);

    wrappedTgtCas = new CasWrapperForTstng(tgtCas);
    // do the copy
    copier = new CasCopier(wrappedSrcCas, wrappedTgtCas);
    copier.copyCasView(wrappedSrcCas, "aNewView", true);

    // verify copy
    CasComparer.assertEqualViews(wrappedSrcCas, wrappedTgtCas.getView("aNewView"));
    
  }

  public void testCopyFs() throws Exception {
    // create a source CAS by deserializing from XCAS
    CAS srcCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
    XCASDeserializer.deserialize(serCasStream, srcCas);
    serCasStream.close();

    // create a destination CAS and the CasCopier instance
    CAS destCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    CasCopier copier = new CasCopier(srcCas, destCas);

    // set sofa data in destination CAS (this is not copied automatically)
    destCas.setDocumentText(srcCas.getDocumentText());

    CasComparer cci = new CasComparer();
    // copy all entities
    Iterator<FeatureStructure> it = srcCas.getIndexRepository().getAllIndexedFS(srcCas.getTypeSystem().getType("org.apache.uima.testTypeSystem.Entity"));
//    while(it.hasNext()) {
      FeatureStructure fs = it.next();
      FeatureStructure fsc = copier.copyFs(fs);
//      destCas.addFsToIndexes(fsc);
      CasComparer.assertEquals(fs, fsc);
//    }
    // copy an Annotation
    Iterator<AnnotationFS> annotIter = srcCas.getAnnotationIndex().iterator();
    FeatureStructure annot = annotIter.next();
    FeatureStructure copy = copier.copyFs(annot);
    // verify copy
    CasComparer.assertEquals(annot, copy);

    // copy a Relation (which will have references)
    Iterator<FeatureStructure> relationIter = srcCas.getIndexRepository().getIndex("testRelationIndex").iterator();
    FeatureStructure relFS = relationIter.next();
    FeatureStructure relCopy = copier.copyFs(relFS);
    // verify copy
    CasComparer.assertEquals(relFS, relCopy);

    // test null array element
    ArrayFS arrFS = srcCas.createArrayFS(3);
    arrFS.set(0, annot);
    arrFS.set(1, null);
    arrFS.set(2, relFS);
    FeatureStructure copyArrFS = copier.copyFs(arrFS);
    CasComparer.assertEquals(arrFS, copyArrFS);
    
    // test with using base cas
    destCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    destCas.setDocumentText(srcCas.getDocumentText());
    copier = new CasCopier(((CASImpl)srcCas).getBaseCAS(), ((CASImpl)destCas).getBaseCAS());

    annotIter = srcCas.getAnnotationIndex().iterator();
    annot = annotIter.next();
    copy = copier.copyFs(annot);
    // verify copy
    CasComparer.assertEquals(annot, copy);
    
    // test copyFS with two CASs, different views, annotations
    // create a destination CAS and the CasCopier instance
    CAS destCas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    CAS destCas2v = destCas2.createView("secondView");
    CasCopier copier2 = new CasCopier(srcCas, destCas2v);
    
    // copy an Annotation
    annotIter = srcCas.getAnnotationIndex().iterator();
    annot = annotIter.next();
    copy = copier2.copyFs(annot);
    destCas2v.addFsToIndexes(copy);
    // verify copy
    CasComparer.assertEquals(annot, copy);

  }

  public void testAnnotationWithNullSofaRef() throws Exception {
    CAS srcCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    CAS srcCasView = srcCas.createView("TestView");
    srcCasView.setDocumentText("This is a test");
    CAS destCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    LowLevelCAS lowLevelSrcCasView = srcCasView.getLowLevelCAS();
    int typeCode = lowLevelSrcCasView.ll_getTypeSystem().ll_getCodeForType(
            srcCas.getAnnotationType());
    // switch method of creating Annotation to create one with valid sofa ref https://issues.apache.org/jira/browse/UIMA-4099
//    int destFsAddr = lowLevelSrcCasView.ll_createFS(typeCode);
//    AnnotationFS fs = (AnnotationFS) lowLevelSrcCasView.ll_getFSForRef(destFsAddr);
    // the above creates an invalid Annotation, because it doesn't set the sofa ref for the view
    // replace with below that includes the proper sofa ref
    JCas srcJCas = srcCasView.getJCas();
    AnnotationFS fs = new Annotation(srcJCas, 0, 4);
//    fs.setIntValue(srcCas.getBeginFeature(), 0);
//    fs.setIntValue(srcCas.getEndFeature(), 4);
    assertEquals("This", fs.getCoveredText());
    srcCasView.addFsToIndexes(fs);
    CasCopier.copyCas(srcCas, destCas, true);
    CAS destCasView = destCas.getView("TestView");
    Iterator<AnnotationFS> annotIter = destCasView.getAnnotationIndex().iterator();
    annotIter.next(); // document annotation
    AnnotationFS copiedFs = annotIter.next();
    assertEquals("This", copiedFs.getCoveredText());
  }
}
