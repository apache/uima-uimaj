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

package org.apache.uima.pear.util;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.test.junit_extension.JUnitExtension;

/**
 * The <code>ComponentCategoryTest</code> class provides JUnit test cases for the
 * org.apache.uima.pear.util.UIMAUtil.identifyUimaComponentCategory() method. The test cases are
 * based on the sample XML descriptors located in the 'pearTests/componentCategoryTests' folder.
 * 
 */
public class ComponentCategoryTest extends TestCase {
  // relative location of test descriptors
  private static String TEST_FOLDER = "pearTests/componentCategoryTests";

  // test descriptor file names
  private static String AE_DESC_NAME = "ae.xml";

  private static String CC_DESC_NAME = "cc.xml";

  private static String CI_DESC_NAME = "ci.xml";

  private static String CR_DESC_NAME = "cr.xml";

  private static String TS_DESC_NAME = "ts.xml";

  
  // NOTE: the testcase to identify a CPE descriptor has meen moved to the CPE project
  
  /**
   * Runs test case for Analysis Engine descriptor.
   */
  public void testAeDescriptor() throws Exception {
    File aeDescFile = JUnitExtension.getFile(TEST_FOLDER + "/" + AE_DESC_NAME);
    if (!aeDescFile.isFile())
      throw new FileNotFoundException("AE descriptor not found");
    Assert.assertTrue(UIMAUtil.ANALYSIS_ENGINE_CTG.equals(UIMAUtil
            .identifyUimaComponentCategory(aeDescFile)));
  }

  /**
   * Runs test case for CAS Consumer descriptor.
   */
  public void testCcDescriptor() throws Exception {
    File ccDescFile = JUnitExtension.getFile(TEST_FOLDER + "/" + CC_DESC_NAME);
    if (!ccDescFile.isFile())
      throw new FileNotFoundException("CC descriptor not found");
    Assert.assertTrue(UIMAUtil.CAS_CONSUMER_CTG.equals(UIMAUtil
            .identifyUimaComponentCategory(ccDescFile)));
  }

  /**
   * Runs test case for CAS Initializer descriptor.
   */
  public void testCiDescriptor() throws Exception {
    File ciDescFile = JUnitExtension.getFile(TEST_FOLDER + "/" + CI_DESC_NAME);
    if (!ciDescFile.isFile())
      throw new FileNotFoundException("CI descriptor not found");
    Assert.assertTrue(UIMAUtil.CAS_INITIALIZER_CTG.equals(UIMAUtil
            .identifyUimaComponentCategory(ciDescFile)));
  }

  /**
   * Runs test case for Collection Reader descriptor.
   */
  public void testCrDescriptor() throws Exception {
    File crDescFile = JUnitExtension.getFile(TEST_FOLDER + "/" + CR_DESC_NAME);
    if (!crDescFile.isFile())
      throw new FileNotFoundException("CR descriptor not found");
    Assert.assertTrue(UIMAUtil.COLLECTION_READER_CTG.equals(UIMAUtil
            .identifyUimaComponentCategory(crDescFile)));
  }

  /**
   * Runs test case for Type System descriptor.
   */
  public void testTsDescriptor() throws Exception {
    File tsDescFile = JUnitExtension.getFile(TEST_FOLDER + "/" + TS_DESC_NAME);
    if (!tsDescFile.isFile())
      throw new FileNotFoundException("TS descriptor not found");
    Assert.assertTrue(UIMAUtil.TYPE_SYSTEM_CTG.equals(UIMAUtil
            .identifyUimaComponentCategory(tsDescFile)));
  }
}
