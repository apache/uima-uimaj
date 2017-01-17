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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.test.junit_extension.JUnitExtension;

/**
 * Tests the DataResource_impl class.
 * 
 */
public class DataResource_implTest extends TestCase {

  /**
   * Constructor for DataResource_implTest.
   * 
   * @param arg0
   */
  public DataResource_implTest(String arg0) {
    super(arg0);
  }

  /**
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testInitialize() throws Exception {
    try {
      // create a FileResourceSpecifier
      FileResourceSpecifier_impl spec = new FileResourceSpecifier_impl();
      File tempDataFile = JUnitExtension
              .getFile("ResourceTest/DataResource_implTest_tempDataFile.dat");
      String fileUrl = tempDataFile.toURL().toString();
      String localCacheFile = "c:\\temp\\cache";
      spec.setFileUrl(fileUrl);
      spec.setLocalCache(localCacheFile);

      // initialize a DataResource
      DataResource_impl dr = new DataResource_impl();
      dr.initialize(spec, Collections.EMPTY_MAP);

      // test
      Assert.assertEquals(new URL(fileUrl), dr.getUrl());
      Assert.assertEquals(new File(localCacheFile), dr.getLocalCache());

      // test failure for nonexistent data
      ResourceInitializationException ex = null;
      try {
        FileResourceSpecifier_impl invalidSpec = new FileResourceSpecifier_impl();
        invalidSpec.setFileUrl("file:/this/file/does/not/exist");
        DataResource_impl dr2 = new DataResource_impl();
        dr2.initialize(invalidSpec, Collections.EMPTY_MAP);
      } catch (ResourceInitializationException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetInputStream() throws Exception {
    try {
      // write a File (APL: changed to use preexisting file - 6/28/04)
      File tempDataFile = JUnitExtension
              .getFile("ResourceTest/DataResource_implTest_tempDataFile.dat");
      // FileWriter writer = new FileWriter(tempDataFile);
      String testString = "This is a test.  This is only a test.";
      // writer.write(testString);
      // writer.close();

      // initialize a DataResource for this file
      FileResourceSpecifier_impl spec = new FileResourceSpecifier_impl();
      spec.setFileUrl(tempDataFile.toURL().toString());
      DataResource_impl dr = new DataResource_impl();
      dr.initialize(spec, Collections.EMPTY_MAP);

      // try to get an input stream and read from the file
      InputStream inStr = dr.getInputStream();
      BufferedReader bufRdr = new BufferedReader(new InputStreamReader(inStr, "utf-8"));
      String result = bufRdr.readLine();
      inStr.close();

      Assert.assertEquals(testString, result);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

}
