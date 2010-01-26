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

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;


public class FsIndexCollection_implTest extends TestCase {

  /**
   * Constructor for TypeSystemDescription_implTest.
   * 
   * @param arg0
   */
  public FsIndexCollection_implTest(String arg0) {
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
      File descriptor = JUnitExtension
              .getFile("FsIndexCollectionImplTest/TestFsIndexCollection.xml");
      FsIndexCollection indexColl = UIMAFramework.getXMLParser().parseFsIndexCollection(
              new XMLInputSource(descriptor));

      assertEquals("TestFsIndexCollection", indexColl.getName());
      assertEquals("This is a test.", indexColl.getDescription());
      assertEquals("The Apache Software Foundation", indexColl.getVendor());
      assertEquals("0.1", indexColl.getVersion());
      Import[] imports = indexColl.getImports();
      assertEquals(2, imports.length);
      assertEquals("FsIndexCollectionImportedFromDataPath", imports[0].getName());
      assertNull(imports[0].getLocation());
      assertNull(imports[1].getName());
      assertEquals("FsIndexCollectionImportedByLocation.xml", imports[1].getLocation());

      FsIndexDescription[] indexes = indexColl.getFsIndexes();
      assertEquals(2, indexes.length);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testResolveImports() throws Exception {
    try {
      File descriptor = JUnitExtension
              .getFile("FsIndexCollectionImplTest/TestFsIndexCollection.xml");
      FsIndexCollection ic = UIMAFramework.getXMLParser().parseFsIndexCollection(
              new XMLInputSource(descriptor));

      FsIndexDescription[] indexes = ic.getFsIndexes();
      assertEquals(2, indexes.length);

      // resolving imports without setting data path should fail
      InvalidXMLException ex = null;
      try {
        ic.resolveImports();
      } catch (InvalidXMLException e) {
        ex = e;
      }
      assertNotNull(ex);
      assertEquals(2, ic.getFsIndexes().length); // should be no side effects when exception is
      // thrown

      // set data path correctly and it should work
      ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
      resMgr.setDataPath(JUnitExtension.getFile("FsIndexCollectionImplTest/dataPathDir")
              .getAbsolutePath());
      ic.resolveImports(resMgr);

      indexes = ic.getFsIndexes();
      assertEquals(4, indexes.length);

      // test that circular imports don't crash
      descriptor = JUnitExtension.getFile("FsIndexCollectionImplTest/Circular1.xml");
      ic = UIMAFramework.getXMLParser().parseFsIndexCollection(new XMLInputSource(descriptor));
      ic.resolveImports();
      assertEquals(2, ic.getFsIndexes().length);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

}
