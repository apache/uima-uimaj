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

package org.apache.uima.internal.util;

import java.io.File;
import java.net.URL;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.test.junit_extension.JUnitExtension;

/*
 * UIMA ClassLoader test
 * 
 */
public class UIMAClassLoaderTest extends TestCase {

  private String testClassPath;

  /**
   * Constructor for UIMAClassLoaderTest
   * 
   * @param arg0
   */
  public UIMAClassLoaderTest(String arg0) {
    super(arg0);
  }

  /**
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    
    this.testClassPath = JUnitExtension.getFile("ClassLoaderTest/classLoadingTest.jar").getAbsolutePath();
  }

  public void testSimpleRsrcMgrCLassLoaderCreation() throws Exception {
    ResourceManager rsrcMgr = UIMAFramework.newDefaultResourceManager();

    Assert.assertNull(rsrcMgr.getExtensionClassLoader());

    rsrcMgr.setExtensionClassPath("../this/is/a/simple/test.jar", false);

    Assert.assertNotNull(rsrcMgr.getExtensionClassLoader());
  }

  public void testAdvancedRsrcMgrCLassLoaderCreation() throws Exception {
    ResourceManager rsrcMgr = UIMAFramework.newDefaultResourceManager();

    Assert.assertNull(rsrcMgr.getExtensionClassLoader());

    rsrcMgr.setExtensionClassPath("../this/is/a/simple/test.jar", true);

    Assert.assertNotNull(rsrcMgr.getExtensionClassLoader());

  }

  public void testSimpleClassloadingSampleString() throws Exception {
    UIMAClassLoader cl = new UIMAClassLoader(this.testClassPath, this.getClass().getClassLoader());
    Class testClass = null;

    testClass = cl.loadClass("org.apache.uima.internal.util.ClassloadingTestClass");

    Assert.assertNotNull(testClass);
    Assert.assertEquals(cl, testClass.getClassLoader());

    testClass = cl.loadClass("org.apache.uima.flow.impl.AnalysisSequenceCapabilityNode");
    
    Assert.assertNotNull(testClass);
    Assert.assertEquals(this.getClass().getClassLoader(),testClass.getClassLoader());
  }

  public void testSimpleClassloadingSampleURL() throws Exception {
    URL[] urlClasspath = new URL[] { new File(this.testClassPath).toURL() };
    UIMAClassLoader cl = new UIMAClassLoader(urlClasspath, this.getClass().getClassLoader());
    Class testClass = null;

    testClass = cl.loadClass("org.apache.uima.internal.util.ClassloadingTestClass");

    Assert.assertNotNull(testClass);
    Assert.assertEquals(cl, testClass.getClassLoader());

   
    testClass = cl.loadClass("org.apache.uima.flow.impl.AnalysisSequenceCapabilityNode");
    			
    Assert.assertNotNull(testClass);
    Assert.assertEquals(this.getClass().getClassLoader(),testClass.getClassLoader());
  }

  public void testAdvancedClassloadingSampleString() throws Exception {
    UIMAClassLoader cl = new UIMAClassLoader(this.testClassPath, this.getClass().getClassLoader());
    Class testClass = null;

    testClass = cl.loadClass("org.apache.uima.internal.util.ClassloadingTestClass");

    Assert.assertNotNull(testClass);
    Assert.assertEquals(cl, testClass.getClassLoader());

    testClass = cl.loadClass("org.apache.uima.flow.impl.AnalysisSequenceCapabilityNode");

    Assert.assertNotNull(testClass);
    Assert.assertEquals(this.getClass().getClassLoader(), testClass.getClassLoader());
  }

  public void testAdvancedClassloadingSampleURL() throws Exception {
    URL[] urlClasspath = new URL[] { new File(this.testClassPath).toURL() };
    UIMAClassLoader cl = new UIMAClassLoader(urlClasspath, this.getClass().getClassLoader());
    Class testClass = null;

    testClass = cl.loadClass("org.apache.uima.internal.util.ClassloadingTestClass");

    Assert.assertNotNull(testClass);
    Assert.assertEquals(cl, testClass.getClassLoader());

    testClass = cl.loadClass("org.apache.uima.flow.impl.AnalysisSequenceCapabilityNode");

    Assert.assertNotNull(testClass);
    Assert.assertEquals(this.getClass().getClassLoader(), testClass.getClassLoader());

  }
}
