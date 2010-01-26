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
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.metadata.ExternalResourceBinding;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;


public class ResourceManagerConfiguration_implTest extends TestCase {

  /**
   * Constructor for TypeSystemDescription_implTest.
   * 
   * @param arg0
   */
  public ResourceManagerConfiguration_implTest(String arg0) {
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
              .getFile("ResourceManagerConfigurationImplTest/TestResourceManagerConfiguration.xml");
      ResourceManagerConfiguration rmc = UIMAFramework.getXMLParser()
              .parseResourceManagerConfiguration(new XMLInputSource(descriptor));
      ExternalResourceDescription[] resources = rmc.getExternalResources();
      ExternalResourceBinding[] bindings = rmc.getExternalResourceBindings();
      assertEquals(4, resources.length);
      assertEquals(4, bindings.length);
      assertEquals("Test Resource Manager Configuration", rmc.getName());
      assertEquals("This is a test.  This is only a test.", rmc.getDescription());
      assertEquals("0.1", rmc.getVersion());
      assertEquals("The Apache Software Foundation", rmc.getVendor());

      descriptor = JUnitExtension
              .getFile("ResourceManagerConfigurationImplTest/ResourceManagerConfigurationWithImports.xml");
      rmc = UIMAFramework.getXMLParser().parseResourceManagerConfiguration(
              new XMLInputSource(descriptor));
      Import[] imports = rmc.getImports();
      resources = rmc.getExternalResources();
      bindings = rmc.getExternalResourceBindings();
      assertEquals(1, imports.length);
      assertEquals(0, resources.length);
      assertEquals(1, bindings.length);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testResolveImports() throws Exception {
    try {
      File descriptor = JUnitExtension
              .getFile("ResourceManagerConfigurationImplTest/TaeImportingResourceManagerConfiguration.xml");
      AnalysisEngineDescription aeDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(descriptor));
      ResourceManagerConfiguration rmc = aeDesc.getResourceManagerConfiguration();
      assertEquals(0, rmc.getExternalResources().length);
      assertEquals(0, rmc.getExternalResourceBindings().length);

      rmc.resolveImports();

      assertEquals(4, rmc.getExternalResources().length);
      assertEquals(4, rmc.getExternalResourceBindings().length);

      // test old single-import style
      descriptor = JUnitExtension
              .getFile("ResourceManagerConfigurationImplTest/TaeImportingResourceManagerConfiguration.xml");
      aeDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
              new XMLInputSource(descriptor));
      rmc = aeDesc.getResourceManagerConfiguration();
      assertEquals(0, rmc.getExternalResources().length);
      assertEquals(0, rmc.getExternalResourceBindings().length);

      rmc.resolveImports();

      assertEquals(4, rmc.getExternalResources().length);
      assertEquals(4, rmc.getExternalResourceBindings().length);

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testClone() throws Exception {
    try {
      File descriptor = JUnitExtension
              .getFile("ResourceManagerConfigurationImplTest/TaeImportingResourceManagerConfiguration.xml");
      AnalysisEngineDescription aeDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(descriptor));
      ResourceManagerConfiguration rmc = aeDesc.getResourceManagerConfiguration();
      ResourceManagerConfiguration rmcClone = (ResourceManagerConfiguration) rmc.clone();
      assertEquals(0, rmcClone.getExternalResources().length);
      assertEquals(0, rmcClone.getExternalResourceBindings().length);
      assertEquals(1, rmcClone.getImports().length);

      rmc.resolveImports();

      assertEquals(4, rmc.getExternalResources().length);
      assertEquals(4, rmc.getExternalResourceBindings().length);
      assertEquals(0, rmc.getImports().length);

      assertEquals(0, rmcClone.getExternalResources().length);
      assertEquals(0, rmcClone.getExternalResourceBindings().length);
      assertEquals(1, rmcClone.getImports().length);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

}
