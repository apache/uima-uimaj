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
package org.apache.uima.fit.factory;

import static org.junit.Assert.assertEquals;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.flow.Flow;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

public class FlowControllerFactoryTest {

  @Test
  public void testResourceMetaData() throws Exception
  {
    FlowControllerDescription desc = FlowControllerFactory
            .createFlowControllerDescription(TestFlowController.class);
    
    org.apache.uima.resource.metadata.ResourceMetaData meta = desc.getMetaData();
    
    assertEquals("dummy", meta.getName());
    assertEquals("1.0", meta.getVersion());
    assertEquals("Just a dummy", meta.getDescription());
    assertEquals("ASL 2.0", meta.getCopyright());
    assertEquals("uimaFIT", meta.getVendor());
  }

  @ResourceMetaData(name = "dummy", version = "1.0", description = "Just a dummy", copyright = "ASL 2.0", vendor = "uimaFIT")
  public class TestFlowController extends
  org.apache.uima.fit.component.JCasFlowController_ImplBase {

    @Override
    public Flow computeFlow(JCas aJCas) throws AnalysisEngineProcessException {
      // Not require for test
      return null;
    }
  }
}
