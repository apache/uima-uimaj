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
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;


public class TypePriorities_implTest extends TestCase {

  /**
   * Constructor for TypeSystemDescription_implTest.
   * 
   * @param arg0
   */
  public TypePriorities_implTest(String arg0) {
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
      // simple type priorties (backwards compatibility check)
      File descriptor = JUnitExtension.getFile("TypePrioritiesImplTest/SimpleTypePriorities.xml");
      TypePriorities pri = UIMAFramework.getXMLParser().parseTypePriorities(
              new XMLInputSource(descriptor));
      assertEquals(null, pri.getName());
      assertEquals(null, pri.getDescription());
      assertEquals(null, pri.getVendor());
      assertEquals(null, pri.getVersion());
      assertEquals(0, pri.getImports().length);
      assertEquals(2, pri.getPriorityLists().length);

      // try one with imports
      descriptor = JUnitExtension.getFile("TypePrioritiesImplTest/TestTypePriorities.xml");

      pri = UIMAFramework.getXMLParser().parseTypePriorities(new XMLInputSource(descriptor));

      assertEquals("TestTypePriorities", pri.getName());
      assertEquals("This is a test.", pri.getDescription());
      assertEquals("The Apache Software Foundation", pri.getVendor());
      assertEquals("0.1", pri.getVersion());
      Import[] imports = pri.getImports();
      assertEquals(2, imports.length);
      assertEquals("TypePrioritiesImportedByLocation.xml", imports[0].getLocation());
      assertNull(imports[0].getName());
      assertNull(imports[1].getLocation());
      assertEquals("TypePrioritiesImportedFromDataPath", imports[1].getName());

      TypePriorityList[] priLists = pri.getPriorityLists();
      assertEquals(1, priLists.length);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testResolveImports() throws Exception {
    try {
      File descriptor = JUnitExtension.getFile("TypePrioritiesImplTest/TestTypePriorities.xml");
      TypePriorities pri = UIMAFramework.getXMLParser().parseTypePriorities(
              new XMLInputSource(descriptor));

      TypePriorityList[] priLists = pri.getPriorityLists();
      assertEquals(1, priLists.length);

      // resolving imports without setting data path should fail
      InvalidXMLException ex = null;
      try {
        pri.resolveImports();
      } catch (InvalidXMLException e) {
        ex = e;
      }
      assertNotNull(ex);
      assertEquals(1, pri.getPriorityLists().length); // should be no side effects when exception is
      // thrown

      // set data path correctly and it should work
      ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
      resMgr.setDataPath(JUnitExtension.getFile("TypePrioritiesImplTest/dataPathDir")
              .getAbsolutePath());
      pri.resolveImports(resMgr);

      priLists = pri.getPriorityLists();
      assertEquals(3, priLists.length);

      // test that circular imports don't crash
      descriptor = JUnitExtension.getFile("TypePrioritiesImplTest/Circular1.xml");
      pri = UIMAFramework.getXMLParser().parseTypePriorities(new XMLInputSource(descriptor));
      pri.resolveImports();
      assertEquals(2, pri.getPriorityLists().length);

      // calling resolveImports when there are none should do nothing
      descriptor = JUnitExtension.getFile("TypePrioritiesImplTest/SimpleTypePriorities.xml");
      pri = UIMAFramework.getXMLParser().parseTypePriorities(new XMLInputSource(descriptor));
      assertEquals(2, pri.getPriorityLists().length);
      pri.resolveImports();
      assertEquals(2, pri.getPriorityLists().length);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testClone() throws Exception {
    try {
      File descriptor = JUnitExtension.getFile("TypePrioritiesImplTest/TestTypePriorities.xml");
      TypePriorities pri = UIMAFramework.getXMLParser().parseTypePriorities(
              new XMLInputSource(descriptor));

      TypePriorities clone = (TypePriorities) pri.clone();

      assertEquals(clone, pri);
      assertFalse(clone.getPriorityLists()[0] == pri.getPriorityLists()[0]);

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }

  }

}
