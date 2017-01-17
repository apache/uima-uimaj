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

package org.apache.uima.cas.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.TaeDescription;
import org.apache.uima.analysis_engine.TextAnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.Resource;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;


public class CasPerformanceTuningSettingsTest extends TestCase {

  /**
   * Constructor for CasPerformanceTuningSettingsTest.
   * 
   * @param arg0
   */
  public CasPerformanceTuningSettingsTest(String arg0) {
    super(arg0);
  }

  public void testInitialHeapSize() throws Exception {
    try {
      Properties defaultProps = UIMAFramework.getDefaultPerformanceTuningProperties();
      int expectedHeapSizeDefault = Integer.parseInt(defaultProps
              .getProperty(UIMAFramework.CAS_INITIAL_HEAP_SIZE));
      Properties newProps = UIMAFramework.getDefaultPerformanceTuningProperties();
      newProps.setProperty(UIMAFramework.CAS_INITIAL_HEAP_SIZE, "100000");

      TaeDescription testDescriptor = UIMAFramework.getXMLParser().parseTaeDescription(
              new XMLInputSource(JUnitExtension
                      .getFile("TextAnalysisEngineImplTest/TestPrimitiveTae1.xml")));

      // check default setting
      TextAnalysisEngine taeDefault = UIMAFramework.produceTAE(testDescriptor);
      CAS tcasDefault = taeDefault.newCAS();
      int heapSize = ((CASImpl) tcasDefault).getHeap().heap.length;
      Assert.assertEquals(expectedHeapSizeDefault, heapSize);

      // check override
      Map<String, Object> params = new HashMap<String, Object>();
      params.put(Resource.PARAM_PERFORMANCE_TUNING_SETTINGS, newProps);
      TextAnalysisEngine taeOverride = UIMAFramework.produceTAE(testDescriptor, params);
      CAS tcasOverride = taeOverride.newCAS();
      heapSize = ((CASImpl) tcasOverride).getHeap().heap.length;
      Assert.assertEquals(100000, heapSize);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

}
