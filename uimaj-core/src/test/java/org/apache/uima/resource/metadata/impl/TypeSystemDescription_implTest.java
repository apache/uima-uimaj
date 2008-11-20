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

package org.apache.uima.resource.metadata.impl;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;


public class TypeSystemDescription_implTest extends TestCase {

  /**
   * Constructor for TypeSystemDescription_implTest.
   * 
   * @param arg0
   */
  public TypeSystemDescription_implTest(String arg0) {
    super(arg0);
  }

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    UIMAFramework.getXMLParser().enableSchemaValidation(true);
  }

  /*
   * @see TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testBuildFromXmlElement() throws Exception {
    try {
      File descriptor = JUnitExtension.getFile("TypeSystemDescriptionImplTest/TestTypeSystem.xml");
      TypeSystemDescription ts = UIMAFramework.getXMLParser().parseTypeSystemDescription(
              new XMLInputSource(descriptor));

      assertEquals("TestTypeSystem", ts.getName());
      assertEquals("This is a test.", ts.getDescription());
      assertEquals("The Apache Software Foundation", ts.getVendor());
      assertEquals("0.1", ts.getVersion());
      Import[] imports = ts.getImports();
      assertEquals(3, imports.length);
      assertEquals("org.apache.uima.resource.metadata.impl.TypeSystemImportedByName", imports[0]
              .getName());
      assertNull(imports[0].getLocation());
      assertNull(imports[1].getName());
      assertEquals("TypeSystemImportedByLocation.xml", imports[1].getLocation());

      TypeDescription[] types = ts.getTypes();
      assertEquals(6, types.length);
      TypeDescription paragraphType = types[4];
      assertEquals("Paragraph", paragraphType.getName());
      assertEquals("A paragraph.", paragraphType.getDescription());
      assertEquals("DocumentStructure", paragraphType.getSupertypeName());
      FeatureDescription[] features = paragraphType.getFeatures();
      assertEquals(2, features.length);
      assertEquals("sentences", features[0].getName());
      assertEquals("Direct references to sentences in this paragraph", features[0].getDescription());
      assertEquals("uima.cas.FSArray", features[0].getRangeTypeName());
      assertEquals("Sentence", features[0].getElementType());
      assertFalse(features[0].getMultipleReferencesAllowed().booleanValue());

      // ts.toXML(System.out);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testResolveImports() throws Exception {
    try {
      File descriptor = JUnitExtension.getFile("TypeSystemDescriptionImplTest/TestTypeSystem.xml");
      TypeSystemDescription ts = UIMAFramework.getXMLParser().parseTypeSystemDescription(
              new XMLInputSource(descriptor));

      TypeDescription[] types = ts.getTypes();
      assertEquals(6, types.length);

      // resolving imports without setting data path should fail
      InvalidXMLException ex = null;
      try {
        ts.resolveImports();
      } catch (InvalidXMLException e) {
        ex = e;
      }
      assertNotNull(ex);
      assertEquals(6, ts.getTypes().length); // should be no side effects when exception is thrown

      // set data path correctly and it should work
      ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
      resMgr.setDataPath(JUnitExtension.getFile("TypeSystemDescriptionImplTest/dataPathDir")
              .getAbsolutePath());
      ts.resolveImports(resMgr);

      types = ts.getTypes();
      assertEquals(13, types.length);
      
      // test that circular imports don't crash
      descriptor = JUnitExtension.getFile("TypeSystemDescriptionImplTest/Circular1.xml");
      ts = UIMAFramework.getXMLParser().parseTypeSystemDescription(new XMLInputSource(descriptor));
      ts.resolveImports();
      assertEquals(2, ts.getTypes().length);

      // calling resolveImports when there are none should do nothing
      descriptor = JUnitExtension
              .getFile("TypeSystemDescriptionImplTest/TypeSystemImportedByLocation.xml");
      ts = UIMAFramework.getXMLParser().parseTypeSystemDescription(new XMLInputSource(descriptor));
      assertEquals(2, ts.getTypes().length);
      ts.resolveImports();
      assertEquals(2, ts.getTypes().length);

      // test import from programatically created TypeSystemDescription
      TypeSystemDescription typeSystemDescription = UIMAFramework.getResourceSpecifierFactory()
              .createTypeSystemDescription();
      Import[] imports = new Import[1];
      imports[0] = new Import_impl();
      URL url = JUnitExtension.getFile("TypeSystemDescriptionImplTest").toURL();
      ((Import_impl) imports[0]).setSourceUrl(url);
      imports[0].setLocation("TypeSystemImportedByLocation.xml");
      typeSystemDescription.setImports(imports);
      TypeSystemDescription typeSystemWithResolvedImports = (TypeSystemDescription) typeSystemDescription
              .clone();
      typeSystemWithResolvedImports.resolveImports(resMgr);
      assertTrue(typeSystemWithResolvedImports.getTypes().length > 0);      
    
      //test that importing the same descriptor twice (using the same ResourceManager) caches
      //the result of the first import and does not create new objects
      TypeSystemDescription typeSystemDescription2 = UIMAFramework.getResourceSpecifierFactory()
      .createTypeSystemDescription();
        Import[] imports2 = new Import[1];
      imports2[0] = new Import_impl();
      ((Import_impl) imports2[0]).setSourceUrl(url);
      imports2[0].setLocation("TypeSystemImportedByLocation.xml");
      typeSystemDescription2.setImports(imports2);
      TypeSystemDescription typeSystemWithResolvedImports2 = (TypeSystemDescription) typeSystemDescription2
              .clone();
      typeSystemWithResolvedImports2.resolveImports(resMgr);
      for (int i = 0 ; i < typeSystemWithResolvedImports.getTypes().length; i++) {
        assertTrue(typeSystemWithResolvedImports.getTypes()[i] == typeSystemWithResolvedImports2.getTypes()[i]);
      }

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }        
  }

  public void testInvalidTypeSystem() throws Exception {
    File file = JUnitExtension.getFile("TypeSystemDescriptionImplTest/InvalidTypeSystem1.xml");
    TypeSystemDescription tsDesc = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            new XMLInputSource(file));
    try {
      CasCreationUtils.createCas(tsDesc, null, null);
      // the above line should throw an exception
      fail();
    } catch (ResourceInitializationException e) {
      assertNotNull(e.getMessage());
      assertFalse(e.getMessage().startsWith("EXCEPTION MESSAGE LOCALIZATION FAILED"));
    }
  }
}
