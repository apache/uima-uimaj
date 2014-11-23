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

import static org.apache.uima.analysis_engine.impl.AnalysisEngineDescription_implTest.encoding;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.impl.AnalysisEngineDescription_impl;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.w3c.dom.Document;

public class Import_implTest extends TestCase {

  /**
   * Constructor for Import_implTest.
   * 
   * @param arg0
   */
  public Import_implTest(String arg0) {
    super(arg0);
  }

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
  }

  /*
   * @see TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testBuildFromXmlElement() throws Exception {
    try {
      DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

      // name import
      String importXml = "<import name=\"this.is.a.test\"/>";
      Document importDoc = docBuilder.parse(new ByteArrayInputStream(importXml.getBytes(encoding)));
      Import_impl importObj = new Import_impl();
      importObj.buildFromXMLElement(importDoc.getDocumentElement(), null);
      assertEquals("this.is.a.test", importObj.getName());
      assertNull(importObj.getLocation());

      // location import
      importXml = "<import location=\"foo/bar/MyFile.xml\"/>";
      importDoc = docBuilder.parse(new ByteArrayInputStream(importXml.getBytes(encoding)));
      importObj = new Import_impl();
      importObj.buildFromXMLElement(importDoc.getDocumentElement(), null);
      assertEquals("foo/bar/MyFile.xml", importObj.getLocation());
      assertNull(importObj.getName());

      // invalid - both location and name
      importXml = "<import name=\"this.is.a.test\" location=\"foo/bar/MyFile.xml\"/>";
      importDoc = docBuilder.parse(new ByteArrayInputStream(importXml.getBytes(encoding)));
      importObj = new Import_impl();
      InvalidXMLException ex = null;
      try {
        importObj.buildFromXMLElement(importDoc.getDocumentElement(), null);
      } catch (InvalidXMLException e) {
        ex = e;
      }
      assertNotNull(ex);

      // invalid - empty import
      importXml = "<import/>";
      importDoc = docBuilder.parse(new ByteArrayInputStream(importXml.getBytes(encoding)));
      importObj = new Import_impl();
      ex = null;
      try {
        importObj.buildFromXMLElement(importDoc.getDocumentElement(), null);
      } catch (InvalidXMLException e) {
        ex = e;
      }
      assertNotNull(ex);
      assertNotNull(ex.getMessage());

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testFindAbsoluteUrl() throws Exception {
    try {
      // location import
      Import_impl importObj = new Import_impl();
      importObj.setLocation("foo/bar/MyFile.xml");
      URL absUrl = importObj.findAbsoluteUrl(UIMAFramework.newDefaultResourceManager());
      URL expectedUrl = new File(System.getProperty("user.dir"), "foo/bar/MyFile.xml").getAbsoluteFile().toURL();
      assertEquals(expectedUrl, absUrl);

      // name import
      importObj = new Import_impl();
      importObj.setName("TypeSystemDescriptionImplTest.TestTypeSystem");
      String workingDir = JUnitExtension.getFile("TypeSystemDescriptionImplTest").getParentFile()
      		.getAbsolutePath();
      ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
      resMgr.setDataPath(workingDir);
      absUrl = importObj.findAbsoluteUrl(resMgr);
      expectedUrl = new File(workingDir, "TypeSystemDescriptionImplTest/TestTypeSystem.xml")
      .toURL();
      assertEquals(expectedUrl, absUrl);

      // name not found
      importObj = new Import_impl();
      importObj.setName("this.should.not.be.found.at.least.i.hope.not");
      InvalidXMLException ex = null;
      try {
        importObj.findAbsoluteUrl(UIMAFramework.newDefaultResourceManager());
      } catch (InvalidXMLException e) {
        ex = e;
      }
      assertNotNull(ex);
      assertNotNull(ex.getMessage());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testNestedImports() throws Exception {
    try {
      File baseDescriptorFile = JUnitExtension
              .getFile("ImportImplTest/subdir/subdir2/AggregateTaeForNestedImportTest.xml");
      File importedFile = JUnitExtension
              .getFile("ImportImplTest/subdir/PrimitiveTaeForNestedImportTest.xml");
      AnalysisEngineDescription_impl agg = (AnalysisEngineDescription_impl) UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(baseDescriptorFile));
      assertEquals(baseDescriptorFile.toURL(), agg.getSourceUrl());

      AnalysisEngineDescription_impl prim = (AnalysisEngineDescription_impl) agg.getDelegateAnalysisEngineSpecifiers()
              .get("Annotator1");
      assertEquals(importedFile.toURL(), prim.getSourceUrl());

      prim.getAnalysisEngineMetaData().getTypeSystem().resolveImports();
      assertEquals(1, prim.getAnalysisEngineMetaData().getTypeSystem().getTypes().length);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

}
