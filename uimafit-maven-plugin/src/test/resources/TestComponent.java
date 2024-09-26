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
package some.test.mypackage;

import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.factory.ExternalResourceFactoryTest.DummyResource;
import org.apache.uima.fit.maven.javadoc.JavadocTextExtractor;
import org.apache.uima.resource.Resource;

/**
 * A test component used to test {@link JavadocTextExtractor}.
 * 
 * @author some author
 */
public class TestComponent {
  /**
   * Documentation for value 1
   */
  public static final String PARAM_VALUE_1 = "value1";
  @ConfigurationParameter(name = PARAM_VALUE_1)
  private String value1;

  public static final String PARAM_VALUE_2 = "value2";
  /**
   * Documentation for value 2
   */
  @ConfigurationParameter(name = PARAM_VALUE_2)
  private String value2;

  /**
   * Documentation for value 3
   */
  @ConfigurationParameter
  private String value3;

  @ConfigurationParameter
  private String value4;

  public static final String PARAM_DEFAULT_NAME_5 = "value5";
  /**
   * Documentation for value 5
   */
  public static final String PARAM_VALUE_5 = PARAM_DEFAULT_NAME_5;
  @ConfigurationParameter(name = PARAM_VALUE_5)
  private String value5;
  
  /**
   * Documentation for resource
   */
  public static final String RES_KEY = "res";
  @ExternalResource(key = RES_KEY)
  private Resource res;

  public TestComponent() {
  }
}
