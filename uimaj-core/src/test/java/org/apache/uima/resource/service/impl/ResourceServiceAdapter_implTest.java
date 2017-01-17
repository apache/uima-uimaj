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

package org.apache.uima.resource.service.impl;

import java.util.Map;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.impl.ConfigurationParameter_impl;
import org.apache.uima.resource.metadata.impl.ResourceMetaData_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;

/**
 * Tests the ResourceServiceAdapter_impl class.
 * 
 */
public class ResourceServiceAdapter_implTest extends TestCase {
  /**
   * Constructor for ResourceServiceAdapter_implTest.
   * 
   * @param arg0
   */
  public ResourceServiceAdapter_implTest(String arg0) {
    super(arg0);
  }

  /**
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    try {
      super.setUp();
      mServiceStub = new TestResourceServiceStub();
      mAdapter = new ResourceServiceAdapter() {
        public boolean initialize(ResourceSpecifier p1, Map p2) {
          return false;
        }
      };
      mAdapter.setStub(mServiceStub);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testGetMetaData() throws Exception {
    try {
      ResourceMetaData md = new ResourceMetaData_impl();
      md.setName("Test");
      md.setDescription("This is a test");
      ConfigurationParameter p1 = new ConfigurationParameter_impl();
      p1.setName("IntegerArrayParam");
      p1.setDescription("multi-valued parameter with Integer data type");
      p1.setType(ConfigurationParameter.TYPE_INTEGER);
      p1.setMultiValued(true);
      md.getConfigurationParameterDeclarations().setConfigurationParameters(
              new ConfigurationParameter[] { p1 });

      mServiceStub.getMetaDataReturnValue = md;
      ResourceMetaData result = mAdapter.getMetaData();
      Assert.assertEquals("callGetMetaData", mServiceStub.lastMethodName);
      Assert.assertEquals(md, result);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  private TestResourceServiceStub mServiceStub;

  private ResourceServiceAdapter mAdapter;

}
