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
import java.util.Collections;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.FileLanguageResourceSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.test.junit_extension.JUnitExtension;

/**
 * Tests the FileLanguageResource_impl class.
 * 
 */
public class FileLanguageResource_implTest extends TestCase {

  /**
   * Constructor for FileLanguageResource_implTest.
   * 
   * @param arg0
   */
  public FileLanguageResource_implTest(String arg0) throws IOException {
    super(arg0);
  }

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    try {
      super.setUp();

      FileLanguageResourceSpecifier spec = new FileLanguageResourceSpecifier_impl();
      File baseDir = JUnitExtension.getFile("ResourceTest");     
      spec.setFileUrlPrefix(new File(baseDir, "FileLanguageResource_implTest_data_").toURL().toString());
      spec.setFileUrlSuffix(".dat");
      mResource = new FileLanguageResource_impl();
      mResource.initialize(spec, Collections.EMPTY_MAP);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetDataResource() throws Exception {
    try {
      DataResource enResource = mResource.getDataResource(new String[] { "en" });
      DataResource deResource = mResource.getDataResource(new String[] { "de" });
      DataResource enusResource = mResource.getDataResource(new String[] { "en-US" });

      Assert.assertNotNull(enResource);
      Assert.assertNotNull(deResource);
      Assert.assertNotNull(enusResource);
      Assert.assertFalse(enResource.equals(deResource));
      Assert.assertTrue(enResource.equals(enusResource));
      Assert.assertEquals(enResource.hashCode(), enusResource.hashCode());

      ResourceInitializationException ex = null;
      try {
        DataResource zhResource = mResource.getDataResource(new String[] { "zh" });
      } catch (ResourceInitializationException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  FileLanguageResource_impl mResource;

}
