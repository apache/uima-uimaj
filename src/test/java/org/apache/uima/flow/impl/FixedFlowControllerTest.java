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
package org.apache.uima.flow.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.analysis_engine.metadata.impl.AnalysisEngineMetaData_impl;
import org.apache.uima.analysis_engine.metadata.impl.FixedFlow_impl;
import org.apache.uima.cas.CAS;
import org.apache.uima.flow.FinalStep;
import org.apache.uima.flow.Flow;
import org.apache.uima.flow.FlowControllerContext;
import org.apache.uima.flow.SimpleStep;
import org.apache.uima.flow.Step;
import org.apache.uima.resource.metadata.OperationalProperties;
import org.apache.uima.resource.metadata.impl.OperationalProperties_impl;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.util.CasCreationUtils;


public class FixedFlowControllerTest extends TestCase {

  private Map<String, AnalysisEngineMetaData> analysisEngineMetaDataMap;
  private FixedFlowController fixedFlowController;
  
  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    analysisEngineMetaDataMap = new HashMap<String, AnalysisEngineMetaData>();
    AnalysisEngineMetaData delegateMd = new AnalysisEngineMetaData_impl();
    delegateMd.setOperationalProperties(new OperationalProperties_impl());
    analysisEngineMetaDataMap.put("key1", delegateMd);
    analysisEngineMetaDataMap.put("key2", delegateMd);
    analysisEngineMetaDataMap.put("key3", delegateMd);
    
    AnalysisEngineMetaData aggregateMd = new AnalysisEngineMetaData_impl();
    FixedFlow fixedFlow = new FixedFlow_impl();
    fixedFlow.setFixedFlow(new String[]{"key1", "key2", "key3"});
    aggregateMd.setFlowConstraints(fixedFlow);
    OperationalProperties opProps = new OperationalProperties_impl();
    aggregateMd.setOperationalProperties(opProps);
    
    UimaContextAdmin rootContext = UIMAFramework.newUimaContext(
            UIMAFramework.getLogger(), UIMAFramework.newDefaultResourceManager(),
            UIMAFramework.newConfigurationManager());
    Map<String, String> aSofaMappings = Collections.emptyMap();
    FlowControllerContext fcContext = new FlowControllerContext_impl(
            rootContext, "_FlowController", aSofaMappings,
            analysisEngineMetaDataMap, aggregateMd);
    fixedFlowController = new FixedFlowController();
    fixedFlowController.initialize(fcContext);    
  }

  public void testComputeFlow() throws Exception {
    CAS cas1 = CasCreationUtils.createCas(new TypeSystemDescription_impl(), null, null);
    CAS cas2 = CasCreationUtils.createCas(new TypeSystemDescription_impl(), null, null);
    Flow flow1 = fixedFlowController.computeFlow(cas1);
    Flow flow2 = fixedFlowController.computeFlow(cas2);
    //two steps in flow 1
    Step step = flow1.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key1", ((SimpleStep)step).getAnalysisEngineKey());
    step = flow1.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key2", ((SimpleStep)step).getAnalysisEngineKey());
    
    //one step in flow 2
    step = flow2.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key1", ((SimpleStep)step).getAnalysisEngineKey());

    //third step in flow 1
    step = flow1.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key3", ((SimpleStep)step).getAnalysisEngineKey());

    //one step in flow 2
    step = flow2.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key2", ((SimpleStep)step).getAnalysisEngineKey());

    //finish flow 1
    step = flow1.next();
    assertTrue(step instanceof FinalStep);
    
    //finish flow 2
    step = flow2.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key3", ((SimpleStep)step).getAnalysisEngineKey());
    step = flow2.next();
    assertTrue(step instanceof FinalStep);
  }
  
  public void testAddAnalysisEngines() throws Exception {
    CAS cas = CasCreationUtils.createCas(new TypeSystemDescription_impl(), null, null);
    Flow flow = fixedFlowController.computeFlow(cas);
    //two steps in flow
    Step step = flow.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key1", ((SimpleStep)step).getAnalysisEngineKey());
    step = flow.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key2", ((SimpleStep)step).getAnalysisEngineKey());
    
    //now add two new AEs
    //first update AE metadata map
    AnalysisEngineMetaData delegateMd = new AnalysisEngineMetaData_impl();
    delegateMd.setOperationalProperties(new OperationalProperties_impl());
    analysisEngineMetaDataMap.put("key4", delegateMd);    
    analysisEngineMetaDataMap.put("key5", delegateMd);    
    //then notify FC
    List<String> newAeKeys = new ArrayList<String>();
    newAeKeys.add("key4");
    newAeKeys.add("key5");
    fixedFlowController.addAnalysisEngines(newAeKeys);
    
    //finish flow
    step = flow.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key3", ((SimpleStep)step).getAnalysisEngineKey());
    step = flow.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key4", ((SimpleStep)step).getAnalysisEngineKey());
    step = flow.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key5", ((SimpleStep)step).getAnalysisEngineKey());
    step = flow.next();
    assertTrue(step instanceof FinalStep);
    
    //test new flow
    flow = fixedFlowController.computeFlow(cas);
    step = flow.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key1", ((SimpleStep)step).getAnalysisEngineKey());
    step = flow.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key2", ((SimpleStep)step).getAnalysisEngineKey());
    step = flow.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key3", ((SimpleStep)step).getAnalysisEngineKey());
    step = flow.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key4", ((SimpleStep)step).getAnalysisEngineKey());
    step = flow.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key5", ((SimpleStep)step).getAnalysisEngineKey());
    step = flow.next();
    assertTrue(step instanceof FinalStep);
  }
  
  public void testRemoveAnalysisEngines() throws Exception {
    CAS cas = CasCreationUtils.createCas(new TypeSystemDescription_impl(), null, null);
    Flow flow = fixedFlowController.computeFlow(cas);
    //one step in flow
    Step step = flow.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key1", ((SimpleStep)step).getAnalysisEngineKey());
    
    //remove "key2"
    analysisEngineMetaDataMap.remove("key2");
    List<String> removedKeys = new ArrayList<String>();
    removedKeys.add("key2");
    fixedFlowController.removeAnalysisEngines(removedKeys);
    
    //finish flow
    step = flow.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key3", ((SimpleStep)step).getAnalysisEngineKey());    
    step = flow.next();
    assertTrue(step instanceof FinalStep);
    
    //test new flow
    flow = fixedFlowController.computeFlow(cas);
    step = flow.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key1", ((SimpleStep)step).getAnalysisEngineKey());
    step = flow.next();
    assertTrue(step instanceof SimpleStep);
    assertEquals("key3", ((SimpleStep)step).getAnalysisEngineKey());
    step = flow.next();
    assertTrue(step instanceof FinalStep);
  }
}
