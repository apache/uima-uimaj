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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.uima.UIMAFramework.getXMLParser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.impl.AnalysisEngineDescription_impl;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.Test;

public class Import_implTest {
  @Test
  public void testBuildFromXmlElement() throws Exception {
    var docBuilderFactory = DocumentBuilderFactory.newInstance();
    var docBuilder = docBuilderFactory.newDocumentBuilder();

    // name import
    var importXml = "<import name=\"this.is.a.test\"/>";
    var importDoc = docBuilder.parse(new ByteArrayInputStream(importXml.getBytes(UTF_8)));
    var importObj = new Import_impl();
    importObj.buildFromXMLElement(importDoc.getDocumentElement(), null);
    assertEquals("this.is.a.test", importObj.getName());
    assertNull(importObj.getLocation());

    // location import
    importXml = "<import location=\"foo/bar/MyFile.xml\"/>";
    importDoc = docBuilder.parse(new ByteArrayInputStream(importXml.getBytes(UTF_8)));
    importObj = new Import_impl();
    importObj.buildFromXMLElement(importDoc.getDocumentElement(), null);
    assertEquals("foo/bar/MyFile.xml", importObj.getLocation());
    assertNull(importObj.getName());

    // invalid - both location and name
    importXml = "<import name=\"this.is.a.test\" location=\"foo/bar/MyFile.xml\"/>";
    importDoc = docBuilder.parse(new ByteArrayInputStream(importXml.getBytes(UTF_8)));
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
    importDoc = docBuilder.parse(new ByteArrayInputStream(importXml.getBytes(UTF_8)));
    importObj = new Import_impl();
    ex = null;
    try {
      importObj.buildFromXMLElement(importDoc.getDocumentElement(), null);
    } catch (InvalidXMLException e) {
      ex = e;
    }
    assertNotNull(ex);
    assertNotNull(ex.getMessage());
  }

  @Test
  void testFindAbsoluteUrl() throws Exception {
    var importObj = new Import_impl();
    importObj.setLocation("foo/bar/MyFile.xml");

    var absUrl = importObj.findAbsoluteUrl(UIMAFramework.newDefaultResourceManager());
    var expectedUrl = new File(System.getProperty("user.dir"), "foo/bar/MyFile.xml")
            .getAbsoluteFile().toURL();

    assertEquals(expectedUrl, absUrl);
  }

  @Test
  void testImportByName() throws Exception {
    var importObj = new Import_impl();
    importObj.setName("TypeSystemDescriptionImplTest.TestTypeSystem");

    var workingDir = JUnitExtension.getFile("TypeSystemDescriptionImplTest").getParentFile()
            .getAbsolutePath();

    var resMgr = UIMAFramework.newDefaultResourceManager();
    resMgr.setDataPathElements(workingDir);

    var absUrl = importObj.findAbsoluteUrl(resMgr);
    var expectedUrl = new File(workingDir, "TypeSystemDescriptionImplTest/TestTypeSystem.xml")
            .toURL();

    assertEquals(expectedUrl, absUrl);
  }

  @Test
  void testImportByNameFailed() throws Exception {
    var importObj = new Import_impl();
    importObj.setName("this.should.not.be.found.at.least.i.hope.not");

    assertThatExceptionOfType(InvalidXMLException.class).isThrownBy(() -> {
      importObj.findAbsoluteUrl(UIMAFramework.newDefaultResourceManager());
    });
  }

  @Test
  void testNestedImports() throws Exception {
    var baseDescriptorFile = getClass()
            .getResource("/ImportImplTest/subdir/subdir2/AggregateTaeForNestedImportTest.xml");
    var agg = (AnalysisEngineDescription_impl) getXMLParser()
            .parseAnalysisEngineDescription(new XMLInputSource(baseDescriptorFile));
    assertThat(agg.getSourceUrl()).isEqualTo(baseDescriptorFile);

    var importedFile = getClass()
            .getResource("/ImportImplTest/subdir/PrimitiveTaeForNestedImportTest.xml");
    var prim = (AnalysisEngineDescription_impl) agg.getDelegateAnalysisEngineSpecifiers()
            .get("Annotator1");
    assertThat(prim.getSourceUrl()).isEqualTo(importedFile);

    prim.getAnalysisEngineMetaData().getTypeSystem().resolveImports();
    assertThat(prim.getAnalysisEngineMetaData().getTypeSystem().getTypes())
            .extracting(TypeDescription::getName) //
            .containsExactly("TestType4");
  }
}
