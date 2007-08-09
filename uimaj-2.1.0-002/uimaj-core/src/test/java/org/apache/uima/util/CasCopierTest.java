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
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas_data.impl.CasComparer;
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
    InputStream serCasStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/multiSofaCas.xml"));
    XCASDeserializer.deserialize(serCasStream, srcCas);
    serCasStream.close();

    // create a destination CAS
    CAS destCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);

    // do the copy
    CasCopier.copyCas(srcCas, destCas, true);
    //XCASSerializer.serialize(destCas, System.out);

    // verify copy
    CasComparer.assertEquals(srcCas, destCas);    
    
    //try with type systems are not identical (dest. a superset of src.)
    TypeSystemDescription additionalTypes = new TypeSystemDescription_impl();
    TypeDescription fooType = additionalTypes.addType("test.Foo", "Test Type", "uima.tcas.Annotation");
    fooType.addFeature("bar", "Test Feature", "uima.cas.String");    
    ArrayList destTypeSystems = new ArrayList();
    destTypeSystems.add(additionalTypes);
    destTypeSystems.add(typeSystem);
    CAS destCas2 = CasCreationUtils.createCas(destTypeSystems);
    CasCopier.copyCas(srcCas, destCas2, true);
    CasComparer.assertEquals(srcCas, destCas);
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
    Iterator annotIter = srcCas.getAnnotationIndex().iterator();
    FeatureStructure annot = (FeatureStructure) annotIter.next();
    FeatureStructure copy = copier.copyFs(annot);
    // verify copy
    CasComparer.assertEquals(annot, copy);

    // copy a Relation (which will have references)
    Iterator relationIter = srcCas.getIndexRepository().getIndex("testRelationIndex").iterator();
    FeatureStructure relFS = (FeatureStructure) relationIter.next();
    FeatureStructure relCopy = copier.copyFs(relFS);
    // verify copy
    CasComparer.assertEquals(relFS, relCopy);
  }
}
