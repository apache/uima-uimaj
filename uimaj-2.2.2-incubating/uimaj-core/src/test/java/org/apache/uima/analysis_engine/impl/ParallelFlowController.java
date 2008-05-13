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

package org.apache.uima.analysis_engine.impl;

import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.flow.CasFlowController_ImplBase;
import org.apache.uima.flow.CasFlow_ImplBase;
import org.apache.uima.flow.FinalStep;
import org.apache.uima.flow.Flow;
import org.apache.uima.flow.FlowControllerContext;
import org.apache.uima.flow.ParallelStep;
import org.apache.uima.flow.Step;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * FlowController for testing ParallelStep.
 */
public class ParallelFlowController extends CasFlowController_ImplBase {
  
  public void initialize(FlowControllerContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.flow.CasFlowController_ImplBase#computeFlow(org.apache.uima.cas.CAS)
   */
  public Flow computeFlow(CAS aCAS) throws AnalysisEngineProcessException {
    ParallelFlowObject ffo = new ParallelFlowObject();
    ffo.setCas(aCAS);
    return ffo;
  }

  class ParallelFlowObject extends CasFlow_ImplBase {
    private boolean done = false;
    
    /**
     * Create a new fixed flow starting at step <code>startStep</code> of the fixed sequence.
     * 
     */
    public ParallelFlowObject() {
      //do nothing
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.flow.Flow#next()
     */
    public Step next() throws AnalysisEngineProcessException {
      if (!done) {
        done = true;
        Set keys = getContext().getAnalysisEngineMetaDataMap().keySet();
        return new ParallelStep(keys);
      }
      else {
        return new FinalStep();
      }
        
    }

    /* (non-Javadoc)
     * @see org.apache.uima.flow.CasFlow_ImplBase#newCasProduced(org.apache.uima.cas.CAS, java.lang.String)
     */
    protected Flow newCasProduced(CAS newCas, String producedBy) throws AnalysisEngineProcessException {
      //for this test, new segments don't continue in the flow
      return new EmptyFlow();
    }     
  }
  
  class EmptyFlow extends CasFlow_ImplBase {
    public Step next() {
      return new FinalStep();
    }
  }
}
