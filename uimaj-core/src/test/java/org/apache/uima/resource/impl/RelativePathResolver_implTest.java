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
import java.net.URL;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.test.junit_extension.JUnitExtension;


public class RelativePathResolver_implTest extends TestCase {
  /**
   * Constructor for RelativePathResolver_implTest.
   * 
   * @param arg0
   */
  public RelativePathResolver_implTest(String arg0) {
    super(arg0);
  }

  public void testSetDataPath() throws Exception {
    try {
      RelativePathResolver_impl resolver = new RelativePathResolver_impl();
      // specify path by file names
      String path = "/this/is/a/test" + System.getProperty("path.separator") + "/another/test";
      resolver.setDataPath(path);
      Assert.assertEquals(path, resolver.getDataPath());
      URL[] urls = resolver.getBaseUrls();
      Assert.assertEquals(2, urls.length);
      Assert.assertEquals(new File("/this/is/a/test").toURL(), urls[0]);
      Assert.assertEquals(new File("/another/test").toURL(), urls[1]);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testResolveRelativePath() throws Exception {
    try {
      RelativePathResolver_impl resolver = new RelativePathResolver_impl();
      // file should not be found
      URL absUrl = resolver.resolveRelativePath(new URL("file:test/relativePathTest.dat"));
      Assert.assertNull(absUrl);

      // specify path
      String path = JUnitExtension.getFile("ResourceTest/subdir").getAbsolutePath();
      resolver.setDataPath(path);

      // now file should be found
      absUrl = resolver.resolveRelativePath(new URL("file:test/relativePathTest.dat"));
      Assert.assertNotNull(absUrl);

      // try resolving an absolute path even with no data path
      resolver.setDataPath("");
      URL newUrl = resolver.resolveRelativePath(absUrl);
      assertEquals(absUrl, newUrl);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

}
