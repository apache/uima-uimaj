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

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.analysis_engine.metadata.FlowConstraints;
import org.apache.uima.cas.CAS;
import org.apache.uima.flow.CasFlowController_ImplBase;
import org.apache.uima.flow.CasFlow_ImplBase;
import org.apache.uima.flow.FinalStep;
import org.apache.uima.flow.Flow;
import org.apache.uima.flow.FlowControllerContext;
import org.apache.uima.flow.SimpleStep;
import org.apache.uima.flow.Step;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * FlowController for testing proper notification to the Flow Controller
 * when an error occurs.
 */
public class FlowControllerForErrorTest extends CasFlowController_ImplBase {
  public static final String PARAM_CONTINUE_ON_FAILURE = "ContinueOnFailure";
  
  private String[] mSequence;
  private boolean mContinueOnFailure;
  
  public static List<String> abortedDocuments = new ArrayList<String>();
  public static List<String> failedAEs = new ArrayList<String>();

  public void initialize(FlowControllerContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    FlowConstraints flowConstraints = aContext.getAggregateMetadata().getFlowConstraints();
    mSequence = ((FixedFlow) flowConstraints).getFixedFlow();
    Boolean paramVal = (Boolean)aContext.getConfigParameterValue(PARAM_CONTINUE_ON_FAILURE);
    mContinueOnFailure = paramVal != null && paramVal.booleanValue(); 
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.flow.CasFlowController_ImplBase#computeFlow(org.apache.uima.cas.CAS)
   */
  public Flow computeFlow(CAS aCAS) throws AnalysisEngineProcessException {
    FixedFlowObject ffo = new FixedFlowObject(aCAS, 0, false);
    ffo.setCas(aCAS);
    return ffo;
  }

  class FixedFlowObject extends CasFlow_ImplBase {
    private int currentStep;

    private boolean wasSegmented = false;
    
    private boolean internallyCreatedCas;

    /**
     * Create a new fixed flow starting at step <code>startStep</code> of the fixed sequence.
     * 
     * @param startStep
     *          index of mSequence to start at
     */
    public FixedFlowObject(CAS cas, int startStep, boolean internallyCreatedCas) {
      setCas(cas);
      currentStep = startStep;
      this.internallyCreatedCas = internallyCreatedCas;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.flow.Flow#next()
     */
    public Step next() throws AnalysisEngineProcessException {
      if (currentStep >= mSequence.length) {
        return new FinalStep(); // this CAS has finished the sequence
      }
      // If CAS was segmented, do not continue with flow. The individual segments
      // are processed further but the original CAS is not.
      if (wasSegmented) {
        return new FinalStep(internallyCreatedCas);
      }

      // otherwise, we just send the CAS to the next AE in sequence.
      return new SimpleStep(mSequence[currentStep++]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.flow.CasFlow_ImplBase#newCasProduced(CAS, String)
     */
    public Flow newCasProduced(CAS newCas, String producedBy) throws AnalysisEngineProcessException {
      // record that the input CAS has been segmented (affects its subsequent flow)
      wasSegmented = true;
      // start the new segment CAS from the next node after the Segmenter that produced it
      int i = 0;
      while (!mSequence[i].equals(producedBy))
        i++;
      return new FixedFlowObject(newCas, i + 1, true);
    }
    
    

    /* (non-Javadoc)
     * @see org.apache.uima.flow.CasFlow_ImplBase#continueOnFailure(java.lang.String, java.lang.Exception)
     */
    public boolean continueOnFailure(String failedAeKey, Exception failure) {
      failedAEs.add(failedAeKey);
      return mContinueOnFailure;
    }

    /* (non-Javadoc)
     * @see org.apache.uima.flow.CasFlow_ImplBase#aborted()
     */
    public void aborted() {
      abortedDocuments.add(getCas().getDocumentText());
    }
    
    
  }

  
  public static void reset() {
    abortedDocuments.clear();
    failedAEs.clear();    
  }
}
