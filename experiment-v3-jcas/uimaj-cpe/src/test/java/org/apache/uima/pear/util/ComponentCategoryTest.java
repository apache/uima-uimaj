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
  private static String CPE_DESC_NAME = "cpe.xml";

  /**
   * Runs test case for CPE descriptor.
   */
  public void testCpeDescriptor() throws Exception {
    File cpeDescFile = JUnitExtension.getFile(TEST_FOLDER + "/" + CPE_DESC_NAME);
    if (!cpeDescFile.isFile())
      throw new FileNotFoundException("CPE descriptor not found");
    Assert.assertTrue(UIMAUtil.CPE_CONFIGURATION_CTG.equals(UIMAUtil
            .identifyUimaComponentCategory(cpeDescFile)));
  }
}
