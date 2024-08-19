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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.impl.AnalysisEngineDescription_impl;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.impl.ConfigurationParameter_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the ResourceService_impl class.
 * 
 */
public class ResourceService_implTest {
  @BeforeEach
  public void setUp() throws Exception {
    try {
      // create resource specifier and a pool containing 2 instances
      AnalysisEngineDescription primitiveDesc = new AnalysisEngineDescription_impl();
      primitiveDesc.setPrimitive(true);
      primitiveDesc
              .setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      primitiveDesc.getMetaData().setName("Test Annotator");
      ConfigurationParameter p1 = new ConfigurationParameter_impl();
      p1.setName("StringParam");
      p1.setDescription("parameter with String data type");
      p1.setType(ConfigurationParameter.TYPE_STRING);
      primitiveDesc.getMetaData().getConfigurationParameterDeclarations()
              .setConfigurationParameters(p1);
      // create a ResourceService_impl
      service = new ResourceService_impl();
      service.initialize(primitiveDesc, null);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testGetMetaData() throws Exception {
    try {
      ResourceMetaData md = service.getMetaData();
      assertThat(md).isNotNull();
      assertThat(md.getName()).isEqualTo("Test Annotator");
      assertThat(md.getUUID()).isNotNull();
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  private ResourceService_impl service;
}
