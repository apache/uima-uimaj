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
import java.net.URL;


/**
 * TestPropertyReader reads the parameters for the JUnit tests.
 * Source is the JUnitTestConfig.properties file.
 * 
 * @author Michael Baessler 
 */
public class TestPropertyReader
{

	/**
	 * Return the current user directory
	 * (user.dir)
	 *  
	 * @return String JUnitTestBasePath
	 */
	public static String getJUnitTestBasePath()
	{
    //Changed this to classpath lookup so we can run in Maven
    //String basePath = System.getProperty("user.dir");
    URL url = TestPropertyReader.class.getClassLoader().getResource("testResources");
    File file = new File(url.getFile());
    String basePath = file.getParentFile().getAbsolutePath();

    if(!(basePath.endsWith("\\") || basePath.endsWith("/")))
    {
      basePath = basePath + System.getProperty("file.separator");
    }
    
    return basePath;

	}

}
