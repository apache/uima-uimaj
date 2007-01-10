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

import java.net.URI;
import java.util.Collections;

import junit.framework.TestCase;

import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.impl.ConfigurationParameterDeclarations_impl;
import org.apache.uima.resource.metadata.impl.ConfigurationParameter_impl;
import org.apache.uima.resource.metadata.impl.ResourceMetaData_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;

/**
 * Tests the DataResource_impl class.
 * 
 */
public class ConfigurableDataResource_implTest extends TestCase {

  /**
   * Constructor for DataResource_implTest.
   * 
   * @param arg0
   */
  public ConfigurableDataResource_implTest(String arg0) {
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
      // create a ConfigurableDataResourceSpecifier
      ConfigurableDataResourceSpecifier_impl cspec = new ConfigurableDataResourceSpecifier_impl();
      cspec.setUrl("jdbc:db2:MyDatabase");
      ResourceMetaData md = new ResourceMetaData_impl();
      cspec.setMetaData(md);
      md.setName("foo");
      ConfigurationParameterDeclarations decls = new ConfigurationParameterDeclarations_impl();
      ConfigurationParameter param = new ConfigurationParameter_impl();
      param.setName("param");
      param.setType("String");
      decls.addConfigurationParameter(param);
      md.setConfigurationParameterDeclarations(decls);

      // initialize a DataResource
      ConfigurableDataResource_impl cdr = new ConfigurableDataResource_impl();
      cdr.initialize(cspec, Collections.EMPTY_MAP);
      assertEquals(new URI("jdbc:db2:MyDatabase"), cdr.getUri());
      assertEquals("foo", cdr.getMetaData().getName());
      ConfigurationParameter param0 = cdr.getMetaData().getConfigurationParameterDeclarations()
              .getConfigurationParameters()[0];
      assertEquals("param", param0.getName());
      assertEquals("String", param0.getType());

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
}
