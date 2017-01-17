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

package org.apache.uima.resource.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ExternalResourceDependency;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.FileLanguageResourceSpecifier;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.ExternalResourceBinding;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.resource.metadata.impl.ExternalResourceBinding_impl;
import org.apache.uima.resource.metadata.impl.ResourceManagerConfiguration_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;


public class ResourceManager_implTest extends TestCase {
  private final File TEST_DATA_FILE = JUnitExtension
          .getFile("ResourceTest/ResourceManager_implTest_tempDataFile.dat");

  private final String TEST_DATAPATH_WITH_SPACES = JUnitExtension.getFile(
          "ResourceTest/spaces in dir name").toString();

  private final String TEST_FILE_IN_DATAPATH = "file:Test.dat";

  private final String TEST_STRING = "This is a test.  This is only a test."; // contents of test

  // data file

  private final String TEST_CONTEXT_NAME = "/testContext1/testContext2/";

  private ResourceManager_impl mManager;

  /**
   * Constructor for ResourceManager_implTest.
   * 
   * @param arg0
   */
  public ResourceManager_implTest(String arg0) throws IOException {
    super(arg0);
  }

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    try {
      super.setUp();
      mManager = new ResourceManager_impl();
      mManager.setDataPath(TEST_DATAPATH_WITH_SPACES);

      // initialize sample resources
      ResourceManagerConfiguration cfg = new ResourceManagerConfiguration_impl();
      // simple data resource
      FileResourceSpecifier_impl spec = new FileResourceSpecifier_impl();
      spec.setFileUrl(TEST_DATA_FILE.toURL().toString());
      ExternalResourceDescription desc = new ExternalResourceDescription_impl();
      desc.setName("myData");
      desc.setResourceSpecifier(spec);

      // data resource with custom interface and implementation class
      ExternalResourceDescription desc2 = new ExternalResourceDescription_impl();
      desc2.setName("myCustomObject");
      desc2.setResourceSpecifier(spec);
      desc2.setImplementationName(TestResourceInterface_impl.class.getName());

      // parameterized (language-based) resource
      FileLanguageResourceSpecifier langSpec = new FileLanguageResourceSpecifier_impl();

      File baseDir = JUnitExtension.getFile("ResourceTest");
      langSpec.setFileUrlPrefix(new File(baseDir, "FileLanguageResource_implTest_data_").toURL()
              .toString());
      langSpec.setFileUrlSuffix(".dat");
      ExternalResourceDescription desc3 = new ExternalResourceDescription_impl();
      desc3.setName("myLanguageResource");
      desc3.setResourceSpecifier(langSpec);

      // parameterized resource with custom impl class
      ExternalResourceDescription desc4 = new ExternalResourceDescription_impl();
      desc4.setName("myLanguageResourceObject");
      desc4.setResourceSpecifier(langSpec);
      desc4.setImplementationName(TestResourceInterface_impl.class.getName());

      // resource path with space in it (tests proper URL encoding of spaces)
      FileResourceSpecifier_impl spec2 = new FileResourceSpecifier_impl();
      spec2.setFileUrl(TEST_FILE_IN_DATAPATH);
      ExternalResourceDescription desc5 = new ExternalResourceDescription_impl();
      desc5.setName("myResourceWithSpaceInPath");
      desc5.setResourceSpecifier(spec2);

      // resource path as filename instead of URL
      ExternalResourceDescription desc6 = new ExternalResourceDescription_impl();
      FileResourceSpecifier_impl fileSpec = new FileResourceSpecifier_impl();
      fileSpec.setFileUrl(TEST_DATA_FILE.getAbsolutePath());
      desc6.setResourceSpecifier(fileSpec);
      desc6.setName("myResourceWithFilePathNotUrl");

      cfg.setExternalResources(new ExternalResourceDescription[] { desc, desc2, desc3, desc4,
          desc5, desc6 });

      // define bindings
      ExternalResourceBinding binding1 = new ExternalResourceBinding_impl();
      binding1.setKey("myDataKey");
      binding1.setResourceName("myData");
      ExternalResourceBinding binding2 = new ExternalResourceBinding_impl();
      binding2.setKey("myCustomObjectKey");
      binding2.setResourceName("myCustomObject");
      ExternalResourceBinding binding3 = new ExternalResourceBinding_impl();
      binding3.setKey("myLanguageResourceKey");
      binding3.setResourceName("myLanguageResource");
      ExternalResourceBinding binding4 = new ExternalResourceBinding_impl();
      binding4.setKey("myLanguageResourceObjectKey");
      binding4.setResourceName("myLanguageResourceObject");
      ExternalResourceBinding binding5 = new ExternalResourceBinding_impl();
      binding5.setKey("myResourceWithSpaceInPathKey");
      binding5.setResourceName("myResourceWithSpaceInPath");
      ExternalResourceBinding binding6 = new ExternalResourceBinding_impl();
      binding6.setKey("myResourceWithFilePathNotUrl");
      binding6.setResourceName("myResourceWithFilePathNotUrl");
      cfg.setExternalResourceBindings(new ExternalResourceBinding[] { binding1, binding2, binding3,
          binding4, binding5, binding6 });

      mManager.initializeExternalResources(cfg, TEST_CONTEXT_NAME, null);

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void tearDown() {
    mManager = null;
  }
  
  public void testSetDataPath() throws Exception {
    try {
      String path = "c:\\this\\path\\is;for\\windows";
      mManager.setDataPath(path);
      Assert.assertEquals(path, mManager.getDataPath());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetResource() throws Exception {
    try {
      // test retrieval
      DataResource r1 = (DataResource) mManager.getResource(TEST_CONTEXT_NAME + "myDataKey");
      Assert.assertEquals(TEST_DATA_FILE.toURL(), r1.getUrl());

      TestResourceInterface r2 = (TestResourceInterface) mManager.getResource(TEST_CONTEXT_NAME
              + "myCustomObjectKey");
      Assert.assertEquals(TEST_STRING, r2.readString());

      DataResource en_r = (DataResource) mManager.getResource(TEST_CONTEXT_NAME
              + "myLanguageResourceKey", new String[] { "en" });
      Assert.assertTrue(en_r.getUrl().toString().endsWith(
              "FileLanguageResource_implTest_data_en.dat"));

      DataResource de_r = (DataResource) mManager.getResource(TEST_CONTEXT_NAME
              + "myLanguageResourceKey", new String[] { "de" });
      Assert.assertTrue(de_r.getUrl().toString().endsWith(
              "FileLanguageResource_implTest_data_de.dat"));

      // this should get the exact same DataResource object as for the "en" param
      DataResource enus_r = (DataResource) mManager.getResource(TEST_CONTEXT_NAME
              + "myLanguageResourceKey", new String[] { "en-US" });
      Assert.assertTrue(en_r == enus_r);

      TestResourceInterface en_obj = (TestResourceInterface) mManager.getResource(TEST_CONTEXT_NAME
              + "myLanguageResourceObjectKey", new String[] { "en" });
      Assert.assertEquals("English", en_obj.readString());

      // test spaces in datapath
      DataResource r3 = (DataResource) mManager.getResource(TEST_CONTEXT_NAME
              + "myResourceWithSpaceInPathKey");
      URL expectedBaseUrl = new File(TEST_DATAPATH_WITH_SPACES).toURL();
      URL expectedUrl = new URL(expectedBaseUrl, TEST_FILE_IN_DATAPATH);
      Assert.assertEquals(expectedUrl, r3.getUrl());
      URI expectedBaseUri = new File(TEST_DATAPATH_WITH_SPACES).toURI();
      URI expectedUri = expectedBaseUri.resolve("Test.dat");
      Assert.assertEquals(expectedUri, r3.getUri());

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testResolveAndValidateDependencies() throws Exception {
    try {
      // dependencies 1-4 are for the resource bindings created in setUp()
      ExternalResourceDependency dep1 = new ExternalResourceDependency_impl();
      dep1.setKey("myDataKey");
      ExternalResourceDependency dep2 = new ExternalResourceDependency_impl();
      dep2.setKey("myLanguageResourceKey");
      ExternalResourceDependency dep3 = new ExternalResourceDependency_impl();
      dep3.setKey("myCustomObjectKey");
      dep3.setInterfaceName(TestResourceInterface.class.getName());
      ExternalResourceDependency dep4 = new ExternalResourceDependency_impl();
      dep4.setKey("myLanguageResourceObjectKey");
      dep4.setInterfaceName(TestResourceInterface.class.getName());
      // dependency 5 is an unbound but optional resource
      ExternalResourceDependency dep5 = new ExternalResourceDependency_impl();
      dep5.setKey("nonExsitentResource");
      dep5.setOptional(true);
      // dependency 6 is resolvable in the classpath
      ExternalResourceDependency dep6 = new ExternalResourceDependency_impl();
      dep6.setKey("org/apache/uima/resource/impl/ResourceInClasspath.txt");

      mManager.resolveAndValidateResourceDependencies(new ExternalResourceDependency[] { dep1,
          dep2, dep3, dep4, dep5, dep6 }, TEST_CONTEXT_NAME);

      // at this point we should be able to look up dep6
      Object r = mManager.getResource(TEST_CONTEXT_NAME + dep6.getKey());
      assertTrue(r instanceof DataResource);
      assertTrue(((DataResource) r).getUrl().toString().endsWith(dep6.getKey()));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testOverrides() throws Exception {
    try {
      final String TEST_DATAPATH = JUnitExtension.getFile("AnnotatorContextTest").getPath();

      File descFile = JUnitExtension.getFile("ResourceManagerImplTest/ResourceTestAggregate.xml");
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
              new XMLInputSource(descFile));
      ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
      resMgr.setDataPath(TEST_DATAPATH);
      UIMAFramework.produceAnalysisEngine(desc, resMgr, null);

      URL url = resMgr.getResourceURL("/Annotator1/TestFileResource");
      assertTrue(url.toString().endsWith("testDataFile2.dat"));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
}
