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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FsIndexCollection_implTest {
  @BeforeEach
  void setUp() throws Exception {
    UIMAFramework.getXMLParser().enableSchemaValidation(true);
  }

  @AfterEach
  void tearDown() throws Exception {
    UIMAFramework.getXMLParser().enableSchemaValidation(false);
  }

  @Test
  void testBuildFromXmlElement() throws Exception {
    try {
      File descriptor = JUnitExtension
              .getFile("FsIndexCollectionImplTest/TestFsIndexCollection.xml");
      FsIndexCollection indexColl = UIMAFramework.getXMLParser()
              .parseFsIndexCollection(new XMLInputSource(descriptor));

      assertThat(indexColl.getName()).isEqualTo("TestFsIndexCollection");
      assertThat(indexColl.getDescription()).isEqualTo("This is a test.");
      assertThat(indexColl.getVendor()).isEqualTo("The Apache Software Foundation");
      assertThat(indexColl.getVersion()).isEqualTo("0.1");
      Import[] imports = indexColl.getImports();
      assertThat(imports).hasSize(2);
      assertThat(imports[0].getName()).isEqualTo("FsIndexCollectionImportedFromDataPath");
      assertThat(imports[0].getLocation()).isNull();
      assertThat(imports[1].getName()).isNull();
      assertThat(imports[1].getLocation()).isEqualTo("FsIndexCollectionImportedByLocation.xml");

      FsIndexDescription[] indexes = indexColl.getFsIndexes();
      assertThat(indexes).hasSize(2);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  void testResolveImports() throws Exception {
    File descriptor = JUnitExtension.getFile("FsIndexCollectionImplTest/TestFsIndexCollection.xml");
    FsIndexCollection ic = UIMAFramework.getXMLParser()
            .parseFsIndexCollection(new XMLInputSource(descriptor));

    FsIndexDescription[] indexes = ic.getFsIndexes();
    assertThat(indexes).hasSize(2);

    // resolving imports without setting data path should fail
    InvalidXMLException ex = null;
    try {
      ic.resolveImports();
    } catch (InvalidXMLException e) {
      ex = e;
    }
    assertThat(ex).isNotNull();
    assertThat(ic.getFsIndexes()).hasSize(2); // should be no side effects when exception is
    // thrown

    // set data path correctly and it should work
    ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
    resMgr.setDataPathElements(
            JUnitExtension.getFile("FsIndexCollectionImplTest/dataPathDir").getAbsoluteFile());
    ic.resolveImports(resMgr);

    indexes = ic.getFsIndexes();
    assertThat(indexes).hasSize(4);

    // test that circular imports don't crash
    descriptor = JUnitExtension.getFile("FsIndexCollectionImplTest/Circular1.xml");
    ic = UIMAFramework.getXMLParser().parseFsIndexCollection(new XMLInputSource(descriptor));
    ic.resolveImports();
    assertThat(ic.getFsIndexes()).hasSize(2);
  }
}
