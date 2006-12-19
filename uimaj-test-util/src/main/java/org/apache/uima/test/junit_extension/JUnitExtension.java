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

package org.apache.uima.test.junit_extension;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;

import junit.framework.Assert;

/**
 * Contains static convenience methods for using the UIMA JUnit extensions.
 * 
 * @author Adam Lally
 */
public class JUnitExtension {
  //private static String junitTestBasePath = TestPropertyReader.getJUnitTestBasePath();

  
//  public static File getFile(String aRelativeFilePath) {
//    return new File(junitTestBasePath, aRelativeFilePath);
//  }
  
  public static File getFile(String aRelativeFilePath) {
    URL url = JUnitExtension.class.getClassLoader().getResource(aRelativeFilePath);
    File file = null;
    if(url != null) {
      String fileURL = null;
      try {
        //TODO: use Java 1.5 decoding 
        fileURL = URLDecoder.decode(url.getFile(), "UTF-8");
      } catch (UnsupportedEncodingException ex) {
        // TODO Auto-generated catch block
        ex.printStackTrace();
      }
      file = new File(fileURL);
    }
    return file;
  } 


  public static void handleException(Exception e) throws Exception {
    // check command line setting
    if (System.getProperty("isCommandLine", "false").equals("true")) {
      // print exception
      ExceptionPrinter.printException(e);
      Assert.fail(e.getMessage());
    } else {
      // thow exception to the JUnit framework
      throw e;
    }
  }
}
