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
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas_data.impl.CasComparer;
import org.apache.uima.cas_data.impl.CasComparerViewChange;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;

/**
 * 
 */
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
    CasComparer.assertEquals(srcCas, destCas);

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

    // create a destination CAS
    CAS destCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);

    // do the copy
    CasCopier copier = new CasCopier(srcCas, destCas);
    copier.copyCasView(srcCas, true);

    // verify copy
    CasComparer.assertEquals(srcCas, destCas);
    
    // do the copy to a different view
    // create a destination CAS
    destCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);

    // do the copy
    copier = new CasCopier(srcCas, destCas);
    copier.copyCasView(srcCas, "aNewView", true);

    // verify copy
    (new CasComparerViewChange(srcCas, destCas.getView("aNewView"))).assertEqualViews();
    
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

    // set sofa data in destination CAS (this is not copied automtically)
    destCas.setDocumentText(srcCas.getDocumentText());

    // copy an Annotation
    Iterator<AnnotationFS> annotIter = srcCas.getAnnotationIndex().iterator();
    FeatureStructure annot = (FeatureStructure) annotIter.next();
    FeatureStructure copy = copier.copyFs(annot);
    // verify copy
    CasComparer.assertEquals(annot, copy);

    // copy a Relation (which will have references)
    Iterator<FeatureStructure> relationIter = srcCas.getIndexRepository().getIndex("testRelationIndex").iterator();
    FeatureStructure relFS = (FeatureStructure) relationIter.next();
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
  }

  public void testAnnotationWithNullSofaRef() throws Exception {
    CAS srcCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    CAS srcCasView = srcCas.createView("TestView");
    srcCasView.setDocumentText("This is a test");
    CAS destCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    LowLevelCAS lowLevelSrcCasView = srcCasView.getLowLevelCAS();
    int typeCode = lowLevelSrcCasView.ll_getTypeSystem().ll_getCodeForType(
            srcCas.getAnnotationType());
    int destFsAddr = lowLevelSrcCasView.ll_createFS(typeCode);
    AnnotationFS fs = (AnnotationFS) lowLevelSrcCasView.ll_getFSForRef(destFsAddr);
    fs.setIntValue(srcCas.getBeginFeature(), 0);
    fs.setIntValue(srcCas.getEndFeature(), 4);
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
