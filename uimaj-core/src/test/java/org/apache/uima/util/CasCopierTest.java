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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.XCASDeserializer;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CasCopierTest {
  private TypeSystemDescription typeSystem;

  private FsIndexDescription[] indexes;

  @BeforeEach
  void setUp() throws Exception {
    File typeSystemFile1 = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
    File indexesFile = JUnitExtension.getFile("ExampleCas/testIndexes.xml");

    typeSystem = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(typeSystemFile1));
    indexes = UIMAFramework.getXMLParser().parseFsIndexCollection(new XMLInputSource(indexesFile))
            .getFsIndexes();
  }

  @Test
  void testCopyCas() throws Exception {
    // create a source CAS by deserializing from XCAS
    CAS srcCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    InputStream serCasStream = new FileInputStream(
            JUnitExtension.getFile("ExampleCas/multiSofaCas.xml"));
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
    ArrayList<TypeSystemDescription> destTypeSystems = new ArrayList<>();
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

    // try with source and dest cas the same
    //
    Exception ee = null;
    try {
      CasCopier.copyCas(srcCasBase, srcCasBase, false);
    } catch (Exception e) {
      ee = e;
    }
    assertThat(ee instanceof UIMARuntimeException).isTrue();

    ee = null;
    CAS v2 = srcCas.createView("v2");
    CasCopier cc = new CasCopier(srcCas, v2);
    try {
      cc.copyCasView(srcCas, v2, false);
    } catch (Exception e) {
      e.printStackTrace();
      ee = e;
    }
    assertThat(ee).isNull();
  }

  @Test
  void testCopyCasWithDifferentTypeSystemObject() throws Exception {
    // create a source CAS by deserializing from XCAS
    CAS srcCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    InputStream serCasStream = new FileInputStream(
            JUnitExtension.getFile("ExampleCas/multiSofaCas.xml"));
    XCASDeserializer.deserialize(serCasStream, srcCas);
    serCasStream.close();

    // create a destination CAS (do not share the same type system object)
    File typeSystemFile = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
    File indexesFile = JUnitExtension.getFile("ExampleCas/testIndexes.xml");

    TypeSystemDescription newTsDesc = typeSystem = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(typeSystemFile));
    FsIndexDescription[] newFsIndexes = indexes = UIMAFramework.getXMLParser()
            .parseFsIndexCollection(new XMLInputSource(indexesFile)).getFsIndexes();
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
    ArrayList<TypeSystemDescription> destTypeSystems = new ArrayList<>();
    destTypeSystems.add(additionalTypes);
    destTypeSystems.add(typeSystem);
    CAS destCas2 = CasCreationUtils.createCas(destTypeSystems);
    CasCopier.copyCas(srcCas, destCas2, true);
    CasComparer.assertEquals(srcCas, destCas2);

    // try with src type system having extra type (no instances) with
    // incompatible ranges
    additionalTypes = new TypeSystemDescription_impl();
    fooType = additionalTypes.addType("test.Foo", "Test Type", "uima.tcas.Annotation");
    fooType.addFeature("bar", "Test Feature", "uima.cas.Float");

    ArrayList<TypeSystemDescription> srcTypeSystems = new ArrayList<>();
    srcTypeSystems.add(additionalTypes);
    srcTypeSystems.add(typeSystem);

    srcCas = CasCreationUtils.createCas(srcTypeSystems);
    serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/multiSofaCas.xml"));
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

  @Test
  void testCopyCasView() throws Exception {
    // create a source CAS by deserializing from XCAS
    CAS srcCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/cas.xml"));
    XCASDeserializer.deserialize(serCasStream, srcCas);
    serCasStream.close();

    // create a new view
    CAS tgtCas = srcCas.createView("view2");
    CasCopier copierv = new CasCopier(srcCas, tgtCas);

    // copy a fs while in an iteration
    FSIterator<TOP> itv = srcCas.getJCas().getFSIndexRepository().<TOP> getIndex("testEntityIndex")
            .iterator();
    FSIterator<Annotation> ita = srcCas.getJCas().getAnnotationIndex().iterator();

    while (ita.hasNext()) {
      Annotation fs = ita.next();
      Annotation fsv2 = copierv.copyFs(fs);
      fsv2.addToIndexes();
    }

    while (itv.hasNext()) {
      TOP fs = itv.next();
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
    // for (; i < 60000; i++) { // uncomment for perf test. was more than 5x faster than version
    // 2.6.0
    destCas.reset();
    long startTime = System.nanoTime();
    copier = new CasCopier(srcCas, destCas);
    copier.copyCasView(srcCas, true); // true == copy the sofa too
    long time = (System.nanoTime() - startTime) / 1000;
    if (time < shortest) {
      shortest = time;
      System.out.format("CasCopier speed for 400KB xcas is %,d microseconds on iteration %,d%n",
              shortest, i);
    }
    // }

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

  @Test
  void testCopyCasViewsWithWrapper() throws Exception {
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

  @Test
  void testCopyFs() throws Exception {
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
    Iterator<TOP> it = srcCas.getIndexRepository()
            .getIndexedFSs(srcCas.getTypeSystem().getType("org.apache.uima.testTypeSystem.Entity"))
            .iterator();
    // Iterator<TOP> it =
    // srcCas.getIndexRepository().getAllIndexedFS(srcCas.getTypeSystem().getType("org.apache.uima.testTypeSystem.Entity"));
    // while(it.hasNext()) {
    TOP fs = it.next();
    TOP fsc = (TOP) copier.copyFs(fs);
    // destCas.addFsToIndexes(fsc);
    CasComparer.assertEquals(fs, fsc);
    // }
    // copy an Annotation
    Iterator<Annotation> annotIter = srcCas.<Annotation> getAnnotationIndex().iterator();
    TOP annot = annotIter.next();
    TOP copy = (TOP) copier.copyFs(annot);
    // verify copy
    CasComparer.assertEquals(annot, copy);

    // copy a Relation (which will have references)
    Iterator<TOP> relationIter = srcCas.getIndexRepository().<TOP> getIndex("testRelationIndex")
            .iterator();
    TOP relFS = relationIter.next();
    TOP relCopy = (TOP) copier.copyFs(relFS);
    // verify copy
    CasComparer.assertEquals(relFS, relCopy);

    // test null array element
    ArrayFS arrFS = srcCas.createArrayFS(3);
    arrFS.set(0, annot);
    arrFS.set(1, null);
    arrFS.set(2, relFS);
    TOP copyArrFS = (TOP) copier.copyFs(arrFS);
    CasComparer.assertEquals((TOP) arrFS, copyArrFS);

    // test with using base cas

    destCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    destCas.setDocumentText(srcCas.getDocumentText());
    copier = new CasCopier(((CASImpl) srcCas).getBaseCAS(), ((CASImpl) destCas).getBaseCAS());

    annotIter = srcCas.<Annotation> getAnnotationIndex().iterator();
    annot = annotIter.next();
    boolean wascaught = false;
    try {
      copy = copier.copyFs(annot);
    } catch (CASRuntimeException e) {
      wascaught = true;
    }
    assertThat(wascaught).isFalse();
    // verify copy
    CasComparer.assertEquals(annot, copy);

    // test copyFS with two CASs, different views, annotations
    // create a destination CAS and the CasCopier instance
    CAS destCas2 = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    CAS destCas2v = destCas2.createView("secondView");
    CasCopier copier2 = new CasCopier(srcCas, destCas2v);

    // copy an Annotation
    annotIter = srcCas.<Annotation> getAnnotationIndex().iterator();
    annot = annotIter.next();
    copy = copier2.copyFs(annot);
    destCas2v.addFsToIndexes(copy);
    // verify copy
    CasComparer.assertEquals(annot, copy);

  }

  @Test
  void testAnnotationWithNullSofaRef() throws Exception {
    CAS srcCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    CAS srcCasView = srcCas.createView("TestView");
    srcCasView.setDocumentText("This is a test");
    CAS destCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    // LowLevelCAS lowLevelSrcCasView = srcCasView.getLowLevelCAS();
    // int typeCode = lowLevelSrcCasView.ll_getTypeSystem().ll_getCodeForType(
    // srcCas.getAnnotationType());
    // switch method of creating Annotation to create one with valid sofa ref
    // https://issues.apache.org/jira/browse/UIMA-4099
    // int destFsAddr = lowLevelSrcCasView.ll_createFS(typeCode);
    // Annotation fs = (Annotation) lowLevelSrcCasView.ll_getFSForRef(destFsAddr);
    // the above creates an invalid Annotation, because it doesn't set the sofa ref for the view
    // replace with below that includes the proper sofa ref
    JCas srcJCas = srcCasView.getJCas();
    Annotation fs = new Annotation(srcJCas, 0, 4);
    // fs.setIntValue(srcCas.getBeginFeature(), 0);
    // fs.setIntValue(srcCas.getEndFeature(), 4);
    assertThat(fs.getCoveredText()).isEqualTo("This");
    srcCasView.addFsToIndexes(fs);
    CasCopier.copyCas(srcCas, destCas, true);
    CAS destCasView = destCas.getView("TestView");
    Iterator<Annotation> annotIter = destCasView.<Annotation> getAnnotationIndex().iterator();
    annotIter.next(); // document annotation
    Annotation copiedFs = annotIter.next();
    assertThat(copiedFs.getCoveredText()).isEqualTo("This");
  }
}
